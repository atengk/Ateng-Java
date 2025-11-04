package io.github.atengk.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Spring 管理的单例服务。
 *
 * <p>说明：
 * 1. Spring 容器中默认的 Bean 作用域是单例（singleton），因此使用 {@code @Service} 注解的类在容器中是单例的。
 * 2. 本类演示一个线程安全的 ID 生成器，适合作为应用级全局服务（例如订单号、流水号等）。
 * 3. 推荐在业务中优先使用 Spring 管理的单例 Bean，而非手动实现单例，便于集成 AOP、配置、测试等功能。
 * </p>
 */
@Service
public class SpringSingletonService {

    /**
     * 线程安全的自增计数器，用于生成唯一 ID。
     */
    private final AtomicLong idCounter = new AtomicLong(0L);

    /**
     * 生成下一个唯一 ID。
     *
     * @return 下一个唯一 ID（long 类型）
     */
    public long nextId() {
        return idCounter.incrementAndGet();
    }

    /**
     * 获取当前计数值（不会改变计数器）。
     *
     * @return 当前计数值
     */
    public long currentId() {
        return idCounter.get();
    }
}

