package io.github.atengk.utils;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * 重试工具类
 *
 * @author Ateng
 * @since 2026-08-13
 */
public final class RetryUtil {

    private static final Logger log = LoggerFactory.getLogger(RetryUtil.class);

    private RetryUtil() {
        throw new UnsupportedOperationException("RetryUtil 不允许实例化");
    }

    /**
     * 固定等待时间重试。
     *
     * <p>
     * 每次执行失败后等待固定时间再进行下一次重试。
     * 例如：1s -> 1s -> 1s。
     * </p>
     *
     * @param action     执行方法
     * @param retryCount 失败后的重试次数
     * @param retryDelay 每次重试等待时间
     * @param <T>        返回值类型
     * @return 执行结果
     */
    public static <T> T fixedDelayRetry(
            Callable<T> action,
            int retryCount,
            Duration retryDelay
    ) {
        return fixedDelayRetry(
                action,
                retryCount,
                retryDelay,
                exception -> true
        );
    }

    /**
     * 固定等待时间重试。
     *
     * @param action         执行方法
     * @param retryCount     失败后的重试次数
     * @param retryDelay     每次重试等待时间
     * @param retryPredicate 是否允许当前异常进行重试
     * @param <T>            返回值类型
     * @return 执行结果
     */
    public static <T> T fixedDelayRetry(
            Callable<T> action,
            int retryCount,
            Duration retryDelay,
            Predicate<Exception> retryPredicate
    ) {
        validate(action, retryCount, retryDelay);
        Objects.requireNonNull(retryPredicate, "重试判断条件不能为空");

        return doRetry(
                action,
                retryCount,
                attempt -> retryDelay,
                retryPredicate
        );
    }

    /**
     * 指数退避 + Full Jitter 重试。
     *
     * <p>
     * 基础等待时间按照指数增长：
     * initialDelay -> 2 * initialDelay -> 4 * initialDelay ...
     * </p>
     *
     * <p>
     * 使用 Full Jitter 后，实际等待时间为：
     * 0 ~ baseDelay。
     * </p>
     *
     * @param action       执行方法
     * @param retryCount   失败后的重试次数
     * @param initialDelay 初始等待时间
     * @param maxDelay     最大等待时间
     * @param <T>          返回值类型
     * @return 执行结果
     */
    public static <T> T exponentialBackoffRetry(
            Callable<T> action,
            int retryCount,
            Duration initialDelay,
            Duration maxDelay
    ) {
        return exponentialBackoffRetry(
                action,
                retryCount,
                initialDelay,
                maxDelay,
                exception -> true
        );
    }

    /**
     * 指数退避 + Full Jitter 重试。
     *
     * @param action         执行方法
     * @param retryCount     失败后的重试次数
     * @param initialDelay   初始等待时间
     * @param maxDelay       最大等待时间
     * @param retryPredicate 是否允许当前异常进行重试
     * @param <T>            返回值类型
     * @return 执行结果
     */
    public static <T> T exponentialBackoffRetry(
            Callable<T> action,
            int retryCount,
            Duration initialDelay,
            Duration maxDelay,
            Predicate<Exception> retryPredicate
    ) {
        validate(action, retryCount, initialDelay);
        validateMaxDelay(initialDelay, maxDelay);
        Objects.requireNonNull(retryPredicate, "重试判断条件不能为空");

        return doRetry(
                action,
                retryCount,
                attempt -> calculateExponentialJitter(
                        initialDelay,
                        maxDelay,
                        attempt
                ),
                retryPredicate
        );
    }

    /**
     * Decorrelated Jitter 重试。
     *
     * <p>
     * 下一次等待时间同时参考上一次等待时间和随机值，
     * 相比严格的指数退避具有更强的随机性。
     * </p>
     *
     * <p>
     * 常见计算方式：
     * random(initialDelay, previousDelay * 3)
     * </p>
     *
     * @param action       执行方法
     * @param retryCount   失败后的重试次数
     * @param initialDelay 初始等待时间
     * @param maxDelay     最大等待时间
     * @param <T>          返回值类型
     * @return 执行结果
     */
    public static <T> T decorrelatedJitterRetry(
            Callable<T> action,
            int retryCount,
            Duration initialDelay,
            Duration maxDelay
    ) {
        return decorrelatedJitterRetry(
                action,
                retryCount,
                initialDelay,
                maxDelay,
                exception -> true
        );
    }

    /**
     * Decorrelated Jitter 重试。
     *
     * @param action         执行方法
     * @param retryCount     失败后的重试次数
     * @param initialDelay   初始等待时间
     * @param maxDelay       最大等待时间
     * @param retryPredicate 是否允许当前异常进行重试
     * @param <T>            返回值类型
     * @return 执行结果
     */
    public static <T> T decorrelatedJitterRetry(
            Callable<T> action,
            int retryCount,
            Duration initialDelay,
            Duration maxDelay,
            Predicate<Exception> retryPredicate
    ) {
        validate(action, retryCount, initialDelay);
        validateMaxDelay(initialDelay, maxDelay);
        Objects.requireNonNull(retryPredicate, "重试判断条件不能为空");

        long initialMillis = initialDelay.toMillis();
        long maxMillis = maxDelay.toMillis();

        return doRetry(
                action,
                retryCount,
                new RetryDelayHandler() {

                    private long previousDelay = initialMillis;

                    @Override
                    public Duration calculate(int attempt) {
                        long upperBound = safeMultiply(previousDelay, 3, maxMillis);

                        long delay = randomLong(
                                initialMillis,
                                Math.max(initialMillis, upperBound)
                        );

                        previousDelay = delay;

                        return Duration.ofMillis(Math.min(delay, maxMillis));
                    }
                },
                retryPredicate
        );
    }

    /**
     * 统一执行重试。
     *
     * @param action         执行方法
     * @param retryCount     重试次数
     * @param delayHandler   等待时间计算器
     * @param retryPredicate 异常重试判断
     * @param <T>            返回值类型
     * @return 执行结果
     */
    private static <T> T doRetry(
            Callable<T> action,
            int retryCount,
            RetryDelayHandler delayHandler,
            Predicate<Exception> retryPredicate
    ) {
        Exception lastException = null;

        for (int attempt = 0; attempt <= retryCount; attempt++) {
            try {
                return action.call();
            } catch (Exception e) {
                lastException = e;

                log.warn(
                        "执行失败，第 {}/{} 次，error={}",
                        attempt + 1,
                        retryCount + 1,
                        e.getMessage()
                );

                // 当前异常不允许重试
                if (!retryPredicate.test(e)) {
                    throw new IllegalStateException(
                            StrUtil.format(
                                    "当前异常不允许重试：{}",
                                    e.getClass().getSimpleName()
                            ),
                            e
                    );
                }

                // 已经是最后一次执行
                if (attempt >= retryCount) {
                    break;
                }

                Duration delay = delayHandler.calculate(attempt);

                log.info(
                        "将在 {} 后进行第 {} 次重试",
                        formatDuration(delay),
                        attempt + 1
                );

                sleep(delay);
            }
        }

        throw new IllegalStateException(
                StrUtil.format(
                        "执行失败，重试 {} 次后仍未成功",
                        retryCount
                ),
                lastException
        );
    }

    /**
     * 计算指数退避 + Full Jitter。
     *
     * <p>
     * 基础值：
     * 1s -> 2s -> 4s -> 8s
     *
     * <p>
     * Full Jitter：
     * 0 ~ baseDelay
     *
     * @param initialDelay 初始等待时间
     * @param maxDelay     最大等待时间
     * @param attempt      当前失败次数，从 0 开始
     * @return 实际等待时间
     */
    private static Duration calculateExponentialJitter(
            Duration initialDelay,
            Duration maxDelay,
            int attempt
    ) {
        long initialMillis = initialDelay.toMillis();
        long maxMillis = maxDelay.toMillis();

        long baseDelay = initialMillis;

        for (int i = 0; i < attempt; i++) {
            if (baseDelay >= maxMillis || baseDelay > maxMillis / 2) {
                baseDelay = maxMillis;
                break;
            }

            baseDelay *= 2;
        }

        baseDelay = Math.min(baseDelay, maxMillis);

        if (baseDelay <= 0) {
            return Duration.ZERO;
        }

        long jitterDelay = ThreadLocalRandom.current()
                .nextLong(baseDelay + 1);

        return Duration.ofMillis(jitterDelay);
    }

    /**
     * 安全计算最大等待范围。
     *
     * @param value      当前值
     * @param multiplier 倍数
     * @param maxValue   最大值
     * @return 计算后的值
     */
    private static long safeMultiply(
            long value,
            int multiplier,
            long maxValue
    ) {
        if (value >= maxValue) {
            return maxValue;
        }

        if (value > maxValue / multiplier) {
            return maxValue;
        }

        return value * multiplier;
    }

    /**
     * 生成指定范围内的随机数。
     *
     * @param min 最小值，包含
     * @param max 最大值，包含
     * @return 随机值
     */
    private static long randomLong(long min, long max) {
        if (min >= max) {
            return min;
        }

        return ThreadLocalRandom.current().nextLong(min, max + 1);
    }

    /**
     * 参数校验。
     *
     * @param action     执行方法
     * @param retryCount 重试次数
     * @param delay      等待时间
     */
    private static void validate(
            Callable<?> action,
            int retryCount,
            Duration delay
    ) {
        Objects.requireNonNull(action, "执行方法不能为空");

        if (retryCount < 0) {
            throw new IllegalArgumentException("重试次数不能小于 0");
        }

        Objects.requireNonNull(delay, "等待时间不能为空");

        if (delay.isNegative()) {
            throw new IllegalArgumentException("等待时间不能小于 0");
        }
    }

    /**
     * 最大等待时间校验。
     *
     * @param initialDelay 初始等待时间
     * @param maxDelay     最大等待时间
     */
    private static void validateMaxDelay(
            Duration initialDelay,
            Duration maxDelay
    ) {
        Objects.requireNonNull(maxDelay, "最大等待时间不能为空");

        if (maxDelay.isNegative()) {
            throw new IllegalArgumentException("最大等待时间不能小于 0");
        }

        if (maxDelay.compareTo(initialDelay) < 0) {
            throw new IllegalArgumentException(
                    "最大等待时间不能小于初始等待时间"
            );
        }
    }

    /**
     * 线程等待。
     *
     * @param duration 等待时间
     */
    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "重试等待过程中线程被中断",
                    e
            );
        }
    }

    /**
     * 格式化等待时间。
     *
     * @param duration 时间
     * @return 格式化后的时间
     */
    private static String formatDuration(Duration duration) {
        long millis = duration.toMillis();

        if (millis >= 1000) {
            return String.format("%.3fs", millis / 1000.0);
        }

        return millis + "ms";
    }

    /**
     * 重试等待时间计算器。
     *
     * @author Ateng
     * @since 2026-08-13
     */
    @FunctionalInterface
    private interface RetryDelayHandler {

        /**
         * 计算下一次重试等待时间。
         *
         * @param attempt 当前失败次数，从 0 开始
         * @return 等待时间
         */
        Duration calculate(int attempt);
    }
}