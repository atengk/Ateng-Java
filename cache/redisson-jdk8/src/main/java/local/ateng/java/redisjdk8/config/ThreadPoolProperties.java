package local.ateng.java.redisjdk8.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局线程池配置属性类
 *
 * <p>
 * 通过 thread-pool 前缀绑定线程池配置，用于统一管理多个自定义线程池。
 * 所有线程池以 Map 形式存储，key 作为线程池名称，value 为线程池的参数配置。
 * 应用启动时由 ThreadPoolManager 动态注册对应的线程池 Bean。
 * </p>
 *
 * @author 孔余
 * @since 2025-11-18
 */
@Data
@Component
@ConfigurationProperties(prefix = "thread-pool")
public class ThreadPoolProperties {

    /**
     * 是否启用线程池功能，false 时不创建任何线程池
     */
    private boolean enabled = true;

    /**
     * 是否使用第一个线程池作为默认 @Async 线程池
     */
    private boolean useFirstAsDefault = false;

    /**
     * 线程池配置集合
     *
     * <p>
     * key 表示线程池名称，value 为对应的线程池参数配置。
     * 配置文件中按顺序定义的第一个线程池视为默认线程池。
     * </p>
     */
    private Map<String, PoolConfig> pools = new LinkedHashMap<>();

    /**
     * 单个线程池的详细参数配置
     *
     * <p>用于构建 ThreadPoolTaskExecutor 实例。</p>
     */
    @Data
    public static class PoolConfig {

        /**
         * 核心线程数
         *
         * <p>
         * 线程池维护的最小常驻线程数，即使空闲也不会被销毁。
         * </p>
         */
        private int coreSize;

        /**
         * 最大线程数
         *
         * <p>
         * 当任务量超过核心线程时允许扩容到的最大线程数。
         * </p>
         */
        private int maxSize;

        /**
         * 队列容量
         *
         * <p>
         * 任务队列的最大长度，用于存放待执行任务。
         * 队列已满时会触发拒绝策略。
         * </p>
         */
        private int queueCapacity;

        /**
         * 空闲线程最大存活时间（秒）
         *
         * <p>
         * 超出核心线程数的多余线程在空闲超过此时间后被回收。
         * </p>
         */
        private int keepAliveSeconds;

        /**
         * 线程名称前缀
         *
         * <p>
         * 用于区分线程池来源，便于排查日志。
         * 如：async-default-、io-pool-。
         * </p>
         */
        private String threadNamePrefix;

        /**
         * 是否允许核心线程超时回收
         *
         * <p>
         * 默认核心线程不会被回收，开启后可减少资源占用。
         * </p>
         */
        private boolean allowCoreThreadTimeout;

        /**
         * 线程池任务拒绝策略
         *
         * <p>当线程池无法接收新任务（队列已满且线程数达到最大值）时采用的处理策略。
         * 支持以下几种值：</p>
         *
         * <ul>
         *     <li><b>callerRuns</b> - 由调用线程执行该任务，既不会抛弃也不会抛异常，适合控制系统负载。</li>
         *     <li><b>abort</b> - 默认策略，抛出 {@link java.util.concurrent.RejectedExecutionException} 异常，表示任务提交失败。</li>
         *     <li><b>discard</b> - 丢弃当前任务，不抛异常。</li>
         *     <li><b>discardOldest</b> - 丢弃队列中最旧的任务，然后尝试提交当前任务。</li>
         * </ul>
         *
         * <p>注意：配置不识别的值将默认使用 <b>abort</b> 策略。</p>
         */
        private String rejectedPolicy;

        /**
         * 服务关闭前最大等待时间（秒）
         *
         * <p>
         * Spring 容器关闭时线程池等待任务完成的最长时间。
         * </p>
         */
        private int awaitTerminationSeconds;
    }
}
