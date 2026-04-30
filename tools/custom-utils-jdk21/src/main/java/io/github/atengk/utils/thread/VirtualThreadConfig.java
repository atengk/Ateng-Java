package io.github.atengk.utils.thread;

import java.time.Duration;
import java.util.Objects;

/**
 * 虚拟线程工具默认配置。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class VirtualThreadConfig {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final int DEFAULT_CONCURRENCY = Math.max(1, Runtime.getRuntime().availableProcessors());
    private static final String DEFAULT_PREFIX = "vt-task";

    private final Duration timeout;
    private final int concurrency;
    private final String threadNamePrefix;

    private VirtualThreadConfig(Builder builder) {
        this.timeout = builder.timeout;
        this.concurrency = builder.concurrency;
        this.threadNamePrefix = builder.threadNamePrefix;
    }

    /**
     * 创建默认配置。
     *
     * @return 默认配置
     */
    public static VirtualThreadConfig defaults() {
        return builder().build();
    }

    /**
     * 创建配置构建器。
     *
     * @return 配置构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取默认超时时间。
     *
     * @return 默认超时时间
     */
    public Duration getTimeout() {
        return timeout;
    }

    /**
     * 获取默认并发数。
     *
     * @return 默认并发数
     */
    public int getConcurrency() {
        return concurrency;
    }

    /**
     * 获取默认线程名前缀。
     *
     * @return 默认线程名前缀
     */
    public String getThreadNamePrefix() {
        return threadNamePrefix;
    }

    /**
     * 虚拟线程配置构建器。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    public static final class Builder {

        private Duration timeout = DEFAULT_TIMEOUT;
        private int concurrency = DEFAULT_CONCURRENCY;
        private String threadNamePrefix = DEFAULT_PREFIX;

        private Builder() {
        }

        /**
         * 设置默认超时时间。
         *
         * @param timeout 默认超时时间
         * @return 当前构建器
         */
        public Builder timeout(Duration timeout) {
            if (timeout == null || timeout.isZero() || timeout.isNegative()) {
                throw new IllegalArgumentException("timeout must be positive");
            }
            this.timeout = timeout;
            return this;
        }

        /**
         * 设置默认并发数。
         *
         * @param concurrency 默认并发数
         * @return 当前构建器
         */
        public Builder concurrency(int concurrency) {
            if (concurrency <= 0) {
                throw new IllegalArgumentException("concurrency must be positive");
            }
            this.concurrency = concurrency;
            return this;
        }

        /**
         * 设置默认线程名前缀。
         *
         * @param threadNamePrefix 默认线程名前缀
         * @return 当前构建器
         */
        public Builder threadNamePrefix(String threadNamePrefix) {
            String normalized = normalizePrefix(threadNamePrefix);
            if (normalized.isBlank()) {
                throw new IllegalArgumentException("threadNamePrefix must not be blank");
            }
            this.threadNamePrefix = normalized;
            return this;
        }

        /**
         * 构建配置对象。
         *
         * @return 配置对象
         */
        public VirtualThreadConfig build() {
            return new VirtualThreadConfig(this);
        }

        private static String normalizePrefix(String value) {
            return Objects.requireNonNull(value, "threadNamePrefix must not be null").trim();
        }
    }
}
