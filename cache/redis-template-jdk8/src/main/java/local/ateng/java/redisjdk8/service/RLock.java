package local.ateng.java.redisjdk8.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 分布式可重入锁接口（兼容 Redisson RLock 常用方法）
 * <p>
 * 说明：
 * - lock(...) 系列方法会在当前线程持有锁时进行本地计数（可重入）。
 * - tryLockAsync 提供异步非阻塞获取锁的能力。
 *
 * @author 孔余
 * @since 2025-07-31
 */
public interface RLock {

    /**
     * 阻塞式加锁（使用默认租期）。
     * <p>方法返回前保证已经获取到锁（或线程被中断）。</p>
     */
    void lock();

    /**
     * 阻塞式加锁（指定租期）。
     *
     * @param leaseTime 锁租期
     * @param unit      时间单位
     */
    void lock(long leaseTime, TimeUnit unit);

    /**
     * 带超时等待的尝试加锁。
     *
     * @param waitTime  最长等待时间
     * @param leaseTime 锁租期
     * @param unit      时间单位
     * @return 成功返回 true，超时或被中断返回 false
     */
    boolean tryLock(long waitTime, long leaseTime, TimeUnit unit);

    /**
     * 立即尝试加锁（不等待）。
     *
     * @param leaseTime 锁租期
     * @param unit      时间单位
     * @return 成功返回 true，失败返回 false
     */
    boolean tryLock(long leaseTime, TimeUnit unit);

    /**
     * 异步尝试加锁（在后台线程执行 tryLock(waitTime, leaseTime, unit)）。
     *
     * @param waitTime  最长等待时间
     * @param leaseTime 锁租期
     * @param unit      时间单位
     * @return CompletableFuture<Boolean>，完成时表示是否获取到锁
     */
    CompletableFuture<Boolean> tryLockAsync(long waitTime, long leaseTime, TimeUnit unit);

    /**
     * 释放锁（支持可重入计数）。
     *
     * @return 若成功释放或减少重入计数返回 true，否则返回 false（可能不是持有者或锁已过期）
     */
    boolean unlock();

    /**
     * 强制删除 Redis 上的锁（不校验持有者，谨慎使用）。
     */
    void forceUnlock();
}
