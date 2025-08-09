package local.ateng.java.redisjdk8.service;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;

/**
 * Redis 分布式锁服务
 *
 * <p>职责：
 * - 提供 getLock(name) 返回 RLock（与 Redisson 风格相似）
 * - 维护全局的 watchdog（续租调度器）、线程级 id 与本地重入计数
 *
 * <p>注意：
 * - 依赖注入的 RedisTemplate 必须以字符串序列化 key/value（建议使用 StringRedisTemplate）。
 * - watchDog 采用每个 (key, lockValue) 单独的定时任务，周期为 lease/3（至少 1 秒）。
 *
 * @author 孔余
 * @since 2025-07-31
 */
@Service
public class RedisLockService implements DisposableBean {

    /**
     * Lua：只有当 value 与期望相同时才删除 key（原子）。
     */
    private static final String UNLOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    /**
     * Lua：只有当 value 与期望相同时才设置 key 的 pexpire（原子）。
     * ARGV[2] 为毫秒数
     */
    private static final String RENEW_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('pexpire', KEYS[1], ARGV[2]) else return 0 end";

    /**
     * 默认锁租期（秒），当调用无参 lock() 时使用。
     */
    private static final long DEFAULT_LEASE_SECONDS = 30L;

    private final StringRedisTemplate redisTemplate;

    private DefaultRedisScript<Long> unlockScript;
    private DefaultRedisScript<Long> renewScript;

    /**
     * JVM 唯一标识，用于构建全局唯一的锁持有标志（配合线程 id 保证唯一性）。
     */
    private static final String JVM_ID = UUID.randomUUID().toString();

    /**
     * 线程级唯一 id（保证同一线程无论 getLock 创建多少实例，锁持有值相同，支持可重入）。
     */
    private final ThreadLocal<String> threadId = ThreadLocal.withInitial(() -> UUID.randomUUID().toString());

    /**
     * 线程本地的锁计数：key -> 重入计数。用于实现可重入。
     */
    private final ThreadLocal<Map<String, Integer>> localLockCount = ThreadLocal.withInitial(HashMap::new);

    /**
     * 续命任务（key:lockValue -> ScheduledFuture）。
     */
    private final ConcurrentMap<String, ScheduledFuture<?>> renewFutures = new ConcurrentHashMap<>();

    /**
     * 存储每个 compositeKey 的 leaseMillis（用于续命时使用）。
     */
    private final ConcurrentMap<String, Long> leaseMillisMap = new ConcurrentHashMap<>();

    /**
     * Watchdog 调度器（守护线程），用于周期性续命。
     */
    private final ScheduledExecutorService renewScheduler;

    /**
     * 异步 tryLock 的线程池（可根据需要扩展为可配置）。
     */
    private final ExecutorService asyncExecutor;

    public RedisLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate 不能为空");

        // 单线程守护调度器
        this.renewScheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "redis-lock-renewal");
            t.setDaemon(true);
            return t;
        });

        // 异步尝试锁线程池（可按需替换或外部注入）
        this.asyncExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "redis-lock-async");
            t.setDaemon(true);
            return t;
        });
    }

    @PostConstruct
    private void initScripts() {
        this.unlockScript = new DefaultRedisScript<>();
        this.unlockScript.setScriptText(UNLOCK_LUA);
        this.unlockScript.setResultType(Long.class);

        this.renewScript = new DefaultRedisScript<>();
        this.renewScript.setScriptText(RENEW_LUA);
        this.renewScript.setResultType(Long.class);
    }

    /**
     * 获取分布式锁对象，使用方式与 Redisson 的 getLock(name) 一致。
     *
     * @param name 锁名（Key）
     * @return RLock 实例（轻量对象）
     */
    public RLock getLock(String name) {
        return new LockImpl(name);
    }

    /**
     * 构建当前线程/进程唯一的锁值（用于写入 Redis，作为 ownership 标记）。
     *
     * @return 锁持有者标识，例如 "jvmId:threadId"
     */
    private String buildLockValue() {
        return JVM_ID + ":" + threadId.get();
    }

    /**
     * 内部实现的 RLock（非静态内部类，可以访问外部服务的共享资源与状态）。
     */
    private class LockImpl implements RLock {

        /**
         * Redis key（直接使用传入的 name，不做前缀处理）
         */
        private final String key;

        LockImpl(String key) {
            this.key = key;
        }

        @Override
        public void lock() {
            lock(DEFAULT_LEASE_SECONDS, TimeUnit.SECONDS);
        }

        @Override
        public void lock(final long leaseTime, final TimeUnit unit) {
            // 无限等待，直到获取到锁或线程中断
            while (true) {
                boolean ok = tryLock(leaseTime, unit);
                if (ok) {
                    return;
                }
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("线程在等待锁时被中断", e);
                }
            }
        }

        @Override
        public boolean tryLock(final long waitTime, final long leaseTime, final TimeUnit unit) {
            final long deadline = System.nanoTime() + unit.toNanos(waitTime);
            while (System.nanoTime() < deadline) {
                if (tryLock(leaseTime, unit)) {
                    return true;
                }
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return false;
        }

        @Override
        public boolean tryLock(final long leaseTime, final TimeUnit unit) {
            final String lockValue = buildLockValue();
            final long leaseMillis = unit.toMillis(leaseTime);

            // 1) 尝试第一次获取
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, lockValue, leaseTime, unit);
            if (Boolean.TRUE.equals(acquired)) {
                // 首次获取：本地计数 + 注册续命任务
                markLocalLocked(key);
                scheduleRenewalIfNeeded(key, lockValue, leaseMillis);
                return true;
            }

            // 2) 检查是否为本线程持有（可重入）
            String current = redisTemplate.opsForValue().get(key);
            if (lockValue.equals(current)) {
                // 可重入：更新 ttl（原子 pexpire），本地计数 + 确保续命任务存在
                redisTemplate.execute(renewScript, Collections.singletonList(key), lockValue, String.valueOf(leaseMillis));
                markLocalLocked(key);
                scheduleRenewalIfNeeded(key, lockValue, leaseMillis);
                return true;
            }

            return false;
        }

        @Override
        public CompletableFuture<Boolean> tryLockAsync(final long waitTime, final long leaseTime, final TimeUnit unit) {
            return CompletableFuture.supplyAsync(() -> tryLock(waitTime, leaseTime, unit), asyncExecutor);
        }

        @Override
        public boolean unlock() {
            Map<String, Integer> map = localLockCount.get();
            Integer count = map.get(key);
            if (count != null && count > 1) {
                // 可重入场景，仅减少计数
                map.put(key, count - 1);
                return true;
            }

            // 计数为 1 或 null -> 尝试原子删除 Redis key（只有持有者能删除）
            final String lockValue = buildLockValue();

            Long result = redisTemplate.execute(unlockScript, Collections.singletonList(key), lockValue);

            // 本地清理计数与续命任务
            map.remove(key);
            if (map.isEmpty()) {
                localLockCount.remove();
            }
            cancelRenewalIfNeeded(key, lockValue);

            return result != null && result > 0;
        }

        @Override
        public void forceUnlock() {
            // 直接删除 Redis 锁，不校验 owner
            redisTemplate.delete(key);

            // 清理可能存在的续命任务（所有与 key 前缀匹配的任务）
            renewFutures.keySet().stream()
                    .filter(composite -> composite.startsWith(key + ":"))
                    .forEach(composite -> {
                        ScheduledFuture<?> f = renewFutures.remove(composite);
                        if (f != null) {
                            f.cancel(true);
                        }
                        leaseMillisMap.remove(composite);
                    });

            // 本地计数清理（仅本线程）
            Map<String, Integer> map = localLockCount.get();
            map.remove(key);
            if (map.isEmpty()) {
                localLockCount.remove();
            }
        }

        /**
         * 将当前线程对 key 的本地重入计数 +1
         */
        private void markLocalLocked(final String key) {
            Map<String, Integer> map = localLockCount.get();
            map.put(key, map.getOrDefault(key, 0) + 1);
        }
    } // end LockImpl

    /**
     * 为 (key, lockValue) 创建续命任务（如果尚未创建）。
     *
     * @param key         Redis key
     * @param lockValue   当前线程的锁值
     * @param leaseMillis 租期（毫秒）
     */
    private void scheduleRenewalIfNeeded(final String key, final String lockValue, final long leaseMillis) {
        final String composite = key + ":" + lockValue;

        // putIfAbsent 保证并发下只有一个任务被注册
        if (renewFutures.containsKey(composite)) {
            return;
        }

        // 保存租期（以便任务使用）
        leaseMillisMap.put(composite, leaseMillis);

        final long interval = Math.max(leaseMillis / 3L, 1000L);

        final ScheduledFuture<?> future = renewScheduler.scheduleAtFixedRate(() -> {
            try {
                // 调用 Lua 脚本：只有当 value 匹配时才会 pexpire
                redisTemplate.execute(renewScript, Collections.singletonList(key), lockValue, String.valueOf(leaseMillis));
            } catch (Exception ex) {
                // 续命失败需要监控/记录；为简洁此处不打印，但建议接入日志或监控
            }
        }, interval, interval, TimeUnit.MILLISECONDS);

        final ScheduledFuture<?> prev = renewFutures.putIfAbsent(composite, future);
        if (prev != null) {
            // 并发场景：若已有任务，取消本次任务并恢复 leaseMillisMap（prev 已存在租期）
            future.cancel(true);
            leaseMillisMap.remove(composite);
        }
    }

    /**
     * 取消 (key, lockValue) 的续命任务（锁完全释放时调用）。
     *
     * @param key       Redis key
     * @param lockValue 锁值
     */
    private void cancelRenewalIfNeeded(final String key, final String lockValue) {
        final String composite = key + ":" + lockValue;
        final ScheduledFuture<?> future = renewFutures.remove(composite);
        leaseMillisMap.remove(composite);
        if (future != null) {
            future.cancel(true);
        }
    }

    /**
     * 销毁时关闭线程池（Spring 容器关闭时调用）。
     */
    @Override
    public void destroy() {
        try {
            renewScheduler.shutdownNow();
        } catch (Exception ignored) {
            // ignore
        }
        try {
            asyncExecutor.shutdownNow();
        } catch (Exception ignored) {
            // ignore
        }
    }
}
