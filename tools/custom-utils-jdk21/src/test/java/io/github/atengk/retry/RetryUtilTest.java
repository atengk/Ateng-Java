package io.github.atengk.retry;

import io.github.atengk.utils.RetryUtil;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * RetryUtil 测试类
 *
 * @author Ateng
 * @since 2026-08-13
 */
class RetryUtilTest {

    /**
     * 测试固定延迟重试最终成功。
     *
     * <p>
     * 前两次失败，第三次成功。
     * retryCount=2，所以最多执行 3 次。
     * </p>
     */
    @Test
    void fixedDelayRetry_success() {
        AtomicInteger counter = new AtomicInteger();

        long startTime = System.currentTimeMillis();

        String result = RetryUtil.fixedDelayRetry(
                () -> {
                    int attempt = counter.incrementAndGet();

                    System.out.println("【fixedDelayRetry】执行第 " + attempt + " 次");

                    if (attempt < 3) {
                        throw new RuntimeException("模拟执行失败");
                    }

                    return "success";
                },
                2,
                Duration.ofMillis(500)
        );

        long elapsed = System.currentTimeMillis() - startTime;

        System.out.println("【fixedDelayRetry】执行成功");
        System.out.println("【fixedDelayRetry】最终结果：" + result);
        System.out.println("【fixedDelayRetry】执行次数：" + counter.get());
        System.out.println("【fixedDelayRetry】总耗时：" + elapsed + "ms");

        assertEquals("success", result);
        assertEquals(3, counter.get());
    }

    /**
     * 测试固定延迟重试最终失败。
     *
     * <p>
     * retryCount=3，所以总共执行 4 次。
     * </p>
     */
    @Test
    void fixedDelayRetry_failed() {
        AtomicInteger counter = new AtomicInteger();

        long startTime = System.currentTimeMillis();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> RetryUtil.fixedDelayRetry(
                        () -> {
                            int attempt = counter.incrementAndGet();

                            System.out.println(
                                    "【fixedDelayRetry】执行第 " + attempt + " 次，模拟失败"
                            );

                            throw new RuntimeException("模拟执行失败");
                        },
                        3,
                        Duration.ofMillis(300)
                )
        );

        long elapsed = System.currentTimeMillis() - startTime;

        System.out.println("【fixedDelayRetry】执行最终失败");
        System.out.println("【fixedDelayRetry】执行次数：" + counter.get());
        System.out.println("【fixedDelayRetry】总耗时：" + elapsed + "ms");
        System.out.println("【fixedDelayRetry】异常：" + exception.getMessage());
        System.out.println("【fixedDelayRetry】根异常：" + exception.getCause().getMessage());

        assertEquals(4, counter.get());
        assertEquals("执行失败，重试 3 次后仍未成功", exception.getMessage());
        assertEquals("模拟执行失败", exception.getCause().getMessage());
    }

    /**
     * 测试指数退避 + Full Jitter 最终成功。
     *
     * <p>
     * 由于 Jitter 是随机的，这里打印每次执行时间，
     * 方便观察实际重试间隔。
     * </p>
     */
    @Test
    void exponentialBackoffRetry_success() {
        AtomicInteger counter = new AtomicInteger();

        long startTime = System.currentTimeMillis();
        AtomicInteger lastAttemptTime = new AtomicInteger((int) startTime);

        String result = RetryUtil.exponentialBackoffRetry(
                () -> {
                    int attempt = counter.incrementAndGet();

                    long currentTime = System.currentTimeMillis();

                    if (attempt > 1) {
                        long previousTime = lastAttemptTime.getAndSet((int) currentTime);
                        System.out.println(
                                "【exponentialBackoffRetry】第 "
                                        + attempt
                                        + " 次执行，距离上次执行约 "
                                        + (currentTime - previousTime)
                                        + "ms"
                        );
                    } else {
                        lastAttemptTime.set((int) currentTime);

                        System.out.println(
                                "【exponentialBackoffRetry】第 1 次执行"
                        );
                    }

                    if (attempt < 4) {
                        throw new RuntimeException("模拟执行失败");
                    }

                    return "success";
                },
                4,
                Duration.ofMillis(500),
                Duration.ofSeconds(5)
        );

        long elapsed = System.currentTimeMillis() - startTime;

        System.out.println("【exponentialBackoffRetry】执行成功");
        System.out.println("【exponentialBackoffRetry】最终结果：" + result);
        System.out.println("【exponentialBackoffRetry】执行次数：" + counter.get());
        System.out.println("【exponentialBackoffRetry】总耗时：" + elapsed + "ms");

        assertEquals("success", result);
        assertEquals(4, counter.get());
    }

    /**
     * 测试指数退避 + Full Jitter 最终失败。
     */
    @Test
    void exponentialBackoffRetry_failed() {
        AtomicInteger counter = new AtomicInteger();

        long startTime = System.currentTimeMillis();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> RetryUtil.exponentialBackoffRetry(
                        () -> {
                            int attempt = counter.incrementAndGet();

                            System.out.println(
                                    "【exponentialBackoffRetry】执行第 "
                                            + attempt
                                            + " 次，模拟失败"
                            );

                            throw new RuntimeException("模拟执行失败");
                        },
                        4,
                        Duration.ofMillis(300),
                        Duration.ofSeconds(3)
                )
        );

        long elapsed = System.currentTimeMillis() - startTime;

        System.out.println("【exponentialBackoffRetry】执行最终失败");
        System.out.println("【exponentialBackoffRetry】执行次数：" + counter.get());
        System.out.println("【exponentialBackoffRetry】总耗时：" + elapsed + "ms");
        System.out.println("【exponentialBackoffRetry】异常：" + exception.getMessage());

        assertEquals(5, counter.get());
    }

    /**
     * 测试 Decorrelated Jitter 最终成功。
     */
    @Test
    void decorrelatedJitterRetry_success() {
        AtomicInteger counter = new AtomicInteger();

        long startTime = System.currentTimeMillis();

        String result = RetryUtil.decorrelatedJitterRetry(
                () -> {
                    int attempt = counter.incrementAndGet();

                    System.out.println(
                            "【decorrelatedJitterRetry】执行第 "
                                    + attempt
                                    + " 次"
                    );

                    if (attempt < 4) {
                        throw new RuntimeException("模拟执行失败");
                    }

                    return "success";
                },
                5,
                Duration.ofMillis(300),
                Duration.ofSeconds(3)
        );

        long elapsed = System.currentTimeMillis() - startTime;

        System.out.println("【decorrelatedJitterRetry】执行成功");
        System.out.println("【decorrelatedJitterRetry】最终结果：" + result);
        System.out.println("【decorrelatedJitterRetry】执行次数：" + counter.get());
        System.out.println("【decorrelatedJitterRetry】总耗时：" + elapsed + "ms");

        assertEquals("success", result);
        assertEquals(4, counter.get());
    }

    /**
     * 测试 Decorrelated Jitter 最终失败。
     */
    @Test
    void decorrelatedJitterRetry_failed() {
        AtomicInteger counter = new AtomicInteger();

        long startTime = System.currentTimeMillis();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> RetryUtil.decorrelatedJitterRetry(
                        () -> {
                            int attempt = counter.incrementAndGet();

                            System.out.println(
                                    "【decorrelatedJitterRetry】执行第 "
                                            + attempt
                                            + " 次，模拟失败"
                            );

                            throw new RuntimeException("模拟执行失败");
                        },
                        3,
                        Duration.ofMillis(300),
                        Duration.ofSeconds(3)
                )
        );

        long elapsed = System.currentTimeMillis() - startTime;

        System.out.println("【decorrelatedJitterRetry】执行最终失败");
        System.out.println("【decorrelatedJitterRetry】执行次数：" + counter.get());
        System.out.println("【decorrelatedJitterRetry】总耗时：" + elapsed + "ms");
        System.out.println("【decorrelatedJitterRetry】异常：" + exception.getMessage());

        assertEquals(4, counter.get());
    }

    /**
     * 测试异常过滤。
     *
     * <p>
     * 当异常不满足重试条件时，应立即结束，不再继续重试。
     * </p>
     */
    @Test
    void retryPredicate_notRetry() {
        AtomicInteger counter = new AtomicInteger();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> RetryUtil.exponentialBackoffRetry(
                        () -> {
                            int attempt = counter.incrementAndGet();

                            System.out.println(
                                    "【retryPredicate】执行第 "
                                            + attempt
                                            + " 次"
                            );

                            throw new IllegalArgumentException("参数错误，不允许重试");
                        },
                        5,
                        Duration.ofMillis(500),
                        Duration.ofSeconds(5),
                        e -> !(e instanceof IllegalArgumentException)
                )
        );

        System.out.println("【retryPredicate】未进行重试");
        System.out.println("【retryPredicate】实际执行次数：" + counter.get());
        System.out.println("【retryPredicate】异常：" + exception.getMessage());

        assertEquals(1, counter.get());
        assertEquals(
                "当前异常不允许重试：IllegalArgumentException",
                exception.getMessage()
        );
        assertEquals(
                "参数错误，不允许重试",
                exception.getCause().getMessage()
        );
    }

    /**
     * 测试 retryCount=0 时只执行一次。
     */
    @Test
    void retryCount_zero() {
        AtomicInteger counter = new AtomicInteger();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> RetryUtil.fixedDelayRetry(
                        () -> {
                            int attempt = counter.incrementAndGet();

                            System.out.println(
                                    "【retryCount=0】执行第 "
                                            + attempt
                                            + " 次"
                            );

                            throw new RuntimeException("模拟失败");
                        },
                        0,
                        Duration.ZERO
                )
        );

        System.out.println("【retryCount=0】不会发生重试");
        System.out.println("【retryCount=0】实际执行次数：" + counter.get());

        assertEquals(1, counter.get());
        assertEquals(
                "执行失败，重试 0 次后仍未成功",
                exception.getMessage()
        );
    }

    /**
     * 测试参数校验。
     */
    @Test
    void invalidArgument() {
        System.out.println("【invalidArgument】开始参数校验测试");

        assertThrows(
                NullPointerException.class,
                () -> RetryUtil.fixedDelayRetry(
                        null,
                        1,
                        Duration.ofSeconds(1)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> RetryUtil.fixedDelayRetry(
                        () -> "success",
                        -1,
                        Duration.ofSeconds(1)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> RetryUtil.fixedDelayRetry(
                        () -> "success",
                        1,
                        Duration.ofSeconds(-1)
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> RetryUtil.fixedDelayRetry(
                        () -> "success",
                        1,
                        null
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> RetryUtil.exponentialBackoffRetry(
                        () -> "success",
                        1,
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(1)
                )
        );

        System.out.println("【invalidArgument】参数校验测试通过");
    }
}