package local.ateng.java.redis.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import local.ateng.java.redis.service.RedissonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 分布式能力测试控制器
 * 用于演示 RedissonService 的分布式锁、读写锁、闭锁、信号量、限流器、布隆过滤器等能力。
 *
 * @author Ateng
 * @since 2026-04-26
 */
@Slf4j
@RestController
@RequestMapping("/redisson/distributed")
@RequiredArgsConstructor
public class RedissonDistributedController {

    private final RedissonService redissonService;

    // -------------------------------------------------------------------------
    // Lock / FairLock / SpinLock
    // -------------------------------------------------------------------------

    /**
     * 执行带可重入锁的业务。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/lock/execute?lockKey=lock:demo&businessMillis=1000"
     *
     * @param lockKey        锁 Key
     * @param businessMillis 模拟业务耗时，单位毫秒
     * @return 执行结果
     */
    @PostMapping("/lock/execute")
    public Map<String, Object> executeWithLock(@RequestParam String lockKey,
                                               @RequestParam(defaultValue = "1000") Long businessMillis) {
        checkKey(lockKey);
        checkBusinessMillis(businessMillis);

        redissonService.executeWithLock(lockKey, () -> {
            log.info("执行可重入锁保护的业务，lockKey={}，businessMillis={}", lockKey, businessMillis);
            ThreadUtil.sleep(businessMillis);
        });

        return ok("可重入锁业务执行完成");
    }

    /**
     * 尝试执行带可重入锁的业务。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/lock/try-execute?lockKey=lock:demo&waitSeconds=3&leaseSeconds=10&businessMillis=1000"
     *
     * @param lockKey        锁 Key
     * @param waitSeconds    最大等待秒数
     * @param leaseSeconds   锁持有秒数，传 0 或负数表示使用 watchdog
     * @param businessMillis 模拟业务耗时，单位毫秒
     * @return 是否执行成功
     */
    @PostMapping("/lock/try-execute")
    public Map<String, Object> tryExecuteWithLock(@RequestParam String lockKey,
                                                  @RequestParam(defaultValue = "3") Long waitSeconds,
                                                  @RequestParam(defaultValue = "10") Long leaseSeconds,
                                                  @RequestParam(defaultValue = "1000") Long businessMillis) {
        checkKey(lockKey);
        Assert.isTrue(ObjectUtil.isNotNull(waitSeconds) && waitSeconds >= 0, "waitSeconds不能小于0");
        Assert.notNull(leaseSeconds, "leaseSeconds不能为空");
        checkBusinessMillis(businessMillis);

        boolean success = redissonService.tryExecuteWithLock(
                lockKey,
                waitSeconds,
                leaseSeconds,
                TimeUnit.SECONDS,
                () -> {
                    log.info("执行可重入锁保护的业务，lockKey={}，businessMillis={}", lockKey, businessMillis);
                    ThreadUtil.sleep(businessMillis);
                }
        );

        return ok(success);
    }

    /**
     * 执行带公平锁的业务。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/fair-lock/try-execute?lockKey=lock:fair:demo&waitSeconds=3&leaseSeconds=10&businessMillis=1000"
     *
     * @param lockKey        锁 Key
     * @param waitSeconds    最大等待秒数
     * @param leaseSeconds   锁持有秒数
     * @param businessMillis 模拟业务耗时，单位毫秒
     * @return 是否执行成功
     */
    @PostMapping("/fair-lock/try-execute")
    public Map<String, Object> tryExecuteWithFairLock(@RequestParam String lockKey,
                                                      @RequestParam(defaultValue = "3") Long waitSeconds,
                                                      @RequestParam(defaultValue = "10") Long leaseSeconds,
                                                      @RequestParam(defaultValue = "1000") Long businessMillis) {
        checkKey(lockKey);
        checkLockTime(waitSeconds, leaseSeconds);
        checkBusinessMillis(businessMillis);

        boolean success = executeWithRLock(
                lockKey,
                redissonService.getFairLock(lockKey),
                waitSeconds,
                leaseSeconds,
                businessMillis
        );

        return ok(success);
    }

    /**
     * 执行带自旋锁的业务。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/spin-lock/try-execute?lockKey=lock:spin:demo&waitSeconds=3&leaseSeconds=10&businessMillis=1000"
     *
     * @param lockKey        锁 Key
     * @param waitSeconds    最大等待秒数
     * @param leaseSeconds   锁持有秒数
     * @param businessMillis 模拟业务耗时，单位毫秒
     * @return 是否执行成功
     */
    @PostMapping("/spin-lock/try-execute")
    public Map<String, Object> tryExecuteWithSpinLock(@RequestParam String lockKey,
                                                      @RequestParam(defaultValue = "3") Long waitSeconds,
                                                      @RequestParam(defaultValue = "10") Long leaseSeconds,
                                                      @RequestParam(defaultValue = "1000") Long businessMillis) {
        checkKey(lockKey);
        checkLockTime(waitSeconds, leaseSeconds);
        checkBusinessMillis(businessMillis);

        boolean success = executeWithRLock(
                lockKey,
                redissonService.getSpinLock(lockKey),
                waitSeconds,
                leaseSeconds,
                businessMillis
        );

        return ok(success);
    }

    /**
     * 查询锁状态。
     * <p>
     * curl "http://localhost:8080/redisson/distributed/lock/status?lockKey=lock:demo"
     *
     * @param lockKey 锁 Key
     * @return 锁状态
     */
    @GetMapping("/lock/status")
    public Map<String, Object> lockStatus(@RequestParam String lockKey) {
        checkKey(lockKey);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("locked", redissonService.isLocked(lockKey));
        data.put("heldByCurrentThread", redissonService.isHeldByCurrentThread(lockKey));
        return ok(data);
    }

    // -------------------------------------------------------------------------
    // ReadWriteLock
    // -------------------------------------------------------------------------

    /**
     * 执行读锁保护的业务。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/read-write-lock/read?lockKey=rw:demo&waitSeconds=3&leaseSeconds=10&businessMillis=1000"
     *
     * @param lockKey        锁 Key
     * @param waitSeconds    最大等待秒数
     * @param leaseSeconds   锁持有秒数
     * @param businessMillis 模拟业务耗时，单位毫秒
     * @return 是否执行成功
     */
    @PostMapping("/read-write-lock/read")
    public Map<String, Object> executeWithReadLock(@RequestParam String lockKey,
                                                   @RequestParam(defaultValue = "3") Long waitSeconds,
                                                   @RequestParam(defaultValue = "10") Long leaseSeconds,
                                                   @RequestParam(defaultValue = "1000") Long businessMillis) {
        checkKey(lockKey);
        checkLockTime(waitSeconds, leaseSeconds);
        checkBusinessMillis(businessMillis);

        boolean success = executeWithRLock(
                lockKey + ":read",
                redissonService.getReadWriteLock(lockKey).readLock(),
                waitSeconds,
                leaseSeconds,
                businessMillis
        );

        return ok(success);
    }

    /**
     * 执行写锁保护的业务。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/read-write-lock/write?lockKey=rw:demo&waitSeconds=3&leaseSeconds=10&businessMillis=1000"
     *
     * @param lockKey        锁 Key
     * @param waitSeconds    最大等待秒数
     * @param leaseSeconds   锁持有秒数
     * @param businessMillis 模拟业务耗时，单位毫秒
     * @return 是否执行成功
     */
    @PostMapping("/read-write-lock/write")
    public Map<String, Object> executeWithWriteLock(@RequestParam String lockKey,
                                                    @RequestParam(defaultValue = "3") Long waitSeconds,
                                                    @RequestParam(defaultValue = "10") Long leaseSeconds,
                                                    @RequestParam(defaultValue = "1000") Long businessMillis) {
        checkKey(lockKey);
        checkLockTime(waitSeconds, leaseSeconds);
        checkBusinessMillis(businessMillis);

        boolean success = executeWithRLock(
                lockKey + ":write",
                redissonService.getReadWriteLock(lockKey).writeLock(),
                waitSeconds,
                leaseSeconds,
                businessMillis
        );

        return ok(success);
    }

    // -------------------------------------------------------------------------
    // CountDownLatch
    // -------------------------------------------------------------------------

    /**
     * 设置闭锁计数。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/latch/set-count?latchKey=latch:demo&count=3"
     *
     * @param latchKey 闭锁 Key
     * @param count    计数
     * @return 执行结果
     */
    @PostMapping("/latch/set-count")
    public Map<String, Object> setCount(@RequestParam String latchKey,
                                        @RequestParam Integer count) {
        checkKey(latchKey);
        Assert.isTrue(ObjectUtil.isNotNull(count) && count > 0, "count必须大于0");

        redissonService.setCount(latchKey, count);
        log.info("设置闭锁计数，latchKey={}，count={}", latchKey, count);
        return ok(true);
    }

    /**
     * 闭锁计数减一。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/latch/count-down?latchKey=latch:demo"
     *
     * @param latchKey 闭锁 Key
     * @return 执行结果
     */
    @PostMapping("/latch/count-down")
    public Map<String, Object> countDown(@RequestParam String latchKey) {
        checkKey(latchKey);

        redissonService.countDown(latchKey);
        log.info("闭锁计数减一，latchKey={}", latchKey);
        return ok(true);
    }

    /**
     * 等待闭锁完成。
     * <p>
     * curl "http://localhost:8080/redisson/distributed/latch/await?latchKey=latch:demo&timeoutSeconds=10"
     *
     * @param latchKey       闭锁 Key
     * @param timeoutSeconds 超时秒数
     * @return 是否完成
     * @throws InterruptedException 线程中断时抛出
     */
    @GetMapping("/latch/await")
    public Map<String, Object> await(@RequestParam String latchKey,
                                     @RequestParam(defaultValue = "10") Long timeoutSeconds) throws InterruptedException {
        checkKey(latchKey);
        Assert.isTrue(ObjectUtil.isNotNull(timeoutSeconds) && timeoutSeconds >= 0, "timeoutSeconds不能小于0");

        boolean success = redissonService.await(latchKey, timeoutSeconds, TimeUnit.SECONDS);
        return ok(success);
    }

    // -------------------------------------------------------------------------
    // Semaphore
    // -------------------------------------------------------------------------

    /**
     * 初始化信号量许可。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/semaphore/init?semaphoreKey=semaphore:demo&permits=5"
     *
     * @param semaphoreKey 信号量 Key
     * @param permits      许可数量
     * @return 执行结果
     */
    @PostMapping("/semaphore/init")
    public Map<String, Object> trySetPermits(@RequestParam String semaphoreKey,
                                             @RequestParam Integer permits) {
        checkKey(semaphoreKey);
        Assert.isTrue(ObjectUtil.isNotNull(permits) && permits > 0, "permits必须大于0");

        redissonService.trySetPermits(semaphoreKey, permits);
        return ok(true);
    }

    /**
     * 尝试获取信号量许可。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/semaphore/try-acquire?semaphoreKey=semaphore:demo&permits=1&waitSeconds=3"
     *
     * @param semaphoreKey 信号量 Key
     * @param permits      许可数量
     * @param waitSeconds  等待秒数
     * @return 是否获取成功
     * @throws InterruptedException 线程中断时抛出
     */
    @PostMapping("/semaphore/try-acquire")
    public Map<String, Object> tryAcquire(@RequestParam String semaphoreKey,
                                          @RequestParam(defaultValue = "1") Integer permits,
                                          @RequestParam(defaultValue = "3") Long waitSeconds) throws InterruptedException {
        checkKey(semaphoreKey);
        Assert.isTrue(ObjectUtil.isNotNull(permits) && permits > 0, "permits必须大于0");
        Assert.isTrue(ObjectUtil.isNotNull(waitSeconds) && waitSeconds >= 0, "waitSeconds不能小于0");

        boolean success = redissonService.tryAcquire(semaphoreKey, permits, waitSeconds, TimeUnit.SECONDS);
        return ok(success);
    }

    /**
     * 释放一个信号量许可。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/semaphore/release?semaphoreKey=semaphore:demo"
     *
     * @param semaphoreKey 信号量 Key
     * @return 执行结果
     */
    @PostMapping("/semaphore/release")
    public Map<String, Object> release(@RequestParam String semaphoreKey) {
        checkKey(semaphoreKey);

        redissonService.release(semaphoreKey);
        return ok(true);
    }

    /**
     * 获取信号量可用许可数量。
     * <p>
     * curl "http://localhost:8080/redisson/distributed/semaphore/available?semaphoreKey=semaphore:demo"
     *
     * @param semaphoreKey 信号量 Key
     * @return 可用许可数量
     */
    @GetMapping("/semaphore/available")
    public Map<String, Object> availablePermits(@RequestParam String semaphoreKey) {
        checkKey(semaphoreKey);

        int permits = redissonService.availablePermits(semaphoreKey);
        return ok(permits);
    }

    // -------------------------------------------------------------------------
    // PermitExpirableSemaphore
    // -------------------------------------------------------------------------

    /**
     * 初始化可过期信号量许可。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/permit-expirable-semaphore/init?semaphoreKey=permit:demo&permits=5"
     *
     * @param semaphoreKey 信号量 Key
     * @param permits      许可数量
     * @return 执行结果
     */
    @PostMapping("/permit-expirable-semaphore/init")
    public Map<String, Object> permitTrySetPermits(@RequestParam String semaphoreKey,
                                                   @RequestParam Integer permits) {
        checkKey(semaphoreKey);
        Assert.isTrue(ObjectUtil.isNotNull(permits) && permits > 0, "permits必须大于0");

        boolean success = redissonService.getPermitExpirableSemaphore(semaphoreKey).trySetPermits(permits);
        return ok(success);
    }

    /**
     * 尝试获取可过期信号量许可。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/permit-expirable-semaphore/try-acquire?semaphoreKey=permit:demo&waitSeconds=3&leaseSeconds=30"
     *
     * @param semaphoreKey 信号量 Key
     * @param waitSeconds  等待秒数
     * @param leaseSeconds 许可租约秒数
     * @return permitId，返回 null 表示未获取到
     * @throws InterruptedException 线程中断时抛出
     */
    @PostMapping("/permit-expirable-semaphore/try-acquire")
    public Map<String, Object> permitTryAcquire(@RequestParam String semaphoreKey,
                                                @RequestParam(defaultValue = "3") Long waitSeconds,
                                                @RequestParam(defaultValue = "30") Long leaseSeconds) throws InterruptedException {
        checkKey(semaphoreKey);
        Assert.isTrue(ObjectUtil.isNotNull(waitSeconds) && waitSeconds >= 0, "waitSeconds不能小于0");
        Assert.isTrue(ObjectUtil.isNotNull(leaseSeconds) && leaseSeconds > 0, "leaseSeconds必须大于0");

        String permitId = redissonService.getPermitExpirableSemaphore(semaphoreKey)
                .tryAcquire(waitSeconds, leaseSeconds, TimeUnit.SECONDS);
        return ok(permitId);
    }

    /**
     * 释放可过期信号量许可。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/permit-expirable-semaphore/release?semaphoreKey=permit:demo&permitId=xxxx"
     *
     * @param semaphoreKey 信号量 Key
     * @param permitId     许可 ID
     * @return 执行结果
     */
    @PostMapping("/permit-expirable-semaphore/release")
    public Map<String, Object> permitRelease(@RequestParam String semaphoreKey,
                                             @RequestParam String permitId) {
        checkKey(semaphoreKey);
        Assert.isTrue(StrUtil.isNotBlank(permitId), "permitId不能为空");

        redissonService.getPermitExpirableSemaphore(semaphoreKey).release(permitId);
        return ok(true);
    }

    /**
     * 获取可过期信号量可用许可数量。
     * <p>
     * curl "http://localhost:8080/redisson/distributed/permit-expirable-semaphore/available?semaphoreKey=permit:demo"
     *
     * @param semaphoreKey 信号量 Key
     * @return 可用许可数量
     */
    @GetMapping("/permit-expirable-semaphore/available")
    public Map<String, Object> permitAvailablePermits(@RequestParam String semaphoreKey) {
        checkKey(semaphoreKey);

        int permits = redissonService.getPermitExpirableSemaphore(semaphoreKey).availablePermits();
        return ok(permits);
    }

    // -------------------------------------------------------------------------
    // RateLimiter
    // -------------------------------------------------------------------------

    /**
     * 初始化限流器。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/rate-limiter/init?key=rate:sms&rate=5&interval=1&unit=SECONDS&rateType=OVERALL"
     *
     * @param key      限流器 Key
     * @param rate     令牌数量
     * @param interval 时间间隔
     * @param unit     时间单位
     * @param rateType 限流类型
     * @return 是否初始化成功
     */
    @PostMapping("/rate-limiter/init")
    public Map<String, Object> rateLimiterInit(@RequestParam String key,
                                               @RequestParam Long rate,
                                               @RequestParam(defaultValue = "1") Long interval,
                                               @RequestParam(defaultValue = "SECONDS") RateIntervalUnit unit,
                                               @RequestParam(defaultValue = "OVERALL") RateType rateType) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(rate) && rate > 0, "rate必须大于0");
        Assert.isTrue(ObjectUtil.isNotNull(interval) && interval > 0, "interval必须大于0");

        boolean success = redissonService.rateLimiterInit(key, rateType, rate, interval, unit);
        return ok(success);
    }

    /**
     * 更新限流器速率。
     * <p>
     * curl -X PUT "http://localhost:8080/redisson/distributed/rate-limiter/rate?key=rate:sms&rate=10&interval=1&unit=SECONDS&rateType=OVERALL"
     *
     * @param key      限流器 Key
     * @param rate     令牌数量
     * @param interval 时间间隔
     * @param unit     时间单位
     * @param rateType 限流类型
     * @return 执行结果
     */
    @PutMapping("/rate-limiter/rate")
    public Map<String, Object> rateLimiterSetRate(@RequestParam String key,
                                                  @RequestParam Long rate,
                                                  @RequestParam(defaultValue = "1") Long interval,
                                                  @RequestParam(defaultValue = "SECONDS") RateIntervalUnit unit,
                                                  @RequestParam(defaultValue = "OVERALL") RateType rateType) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(rate) && rate > 0, "rate必须大于0");
        Assert.isTrue(ObjectUtil.isNotNull(interval) && interval > 0, "interval必须大于0");

        redissonService.rateLimiterSetRate(key, rateType, rate, interval, unit);
        return ok(true);
    }

    /**
     * 尝试获取限流令牌。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/rate-limiter/try-acquire?key=rate:sms&permits=1"
     *
     * @param key     限流器 Key
     * @param permits 令牌数量
     * @return 是否获取成功
     */
    @PostMapping("/rate-limiter/try-acquire")
    public Map<String, Object> rateLimiterTryAcquire(@RequestParam String key,
                                                     @RequestParam(defaultValue = "1") Long permits) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(permits) && permits > 0, "permits必须大于0");

        boolean success = redissonService.rateLimiterTryAcquire(key, permits);
        return ok(success);
    }

    /**
     * 等待获取限流令牌。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/rate-limiter/try-acquire-wait?key=rate:sms&timeoutSeconds=3"
     *
     * @param key            限流器 Key
     * @param timeoutSeconds 等待秒数
     * @return 是否获取成功
     */
    @PostMapping("/rate-limiter/try-acquire-wait")
    public Map<String, Object> rateLimiterTryAcquireWait(@RequestParam String key,
                                                         @RequestParam(defaultValue = "3") Long timeoutSeconds) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(timeoutSeconds) && timeoutSeconds >= 0, "timeoutSeconds不能小于0");

        boolean success = redissonService.rateLimiterTryAcquire(key, timeoutSeconds, TimeUnit.SECONDS);
        return ok(success);
    }

    /**
     * 删除限流器。
     * <p>
     * curl -X DELETE "http://localhost:8080/redisson/distributed/rate-limiter?key=rate:sms"
     *
     * @param key 限流器 Key
     * @return 是否删除成功
     */
    @DeleteMapping("/rate-limiter")
    public Map<String, Object> rateLimiterDelete(@RequestParam String key) {
        checkKey(key);

        boolean success = redissonService.rateLimiterDelete(key);
        return ok(success);
    }

    // -------------------------------------------------------------------------
    // BloomFilter
    // -------------------------------------------------------------------------

    /**
     * 初始化布隆过滤器。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/bloom/init?key=bloom:user&expectedInsertions=100000&falseProbability=0.01"
     *
     * @param key                Redis 键
     * @param expectedInsertions 预计插入量
     * @param falseProbability   误判率
     * @return 执行结果
     */
    @PostMapping("/bloom/init")
    public Map<String, Object> bloomInit(@RequestParam String key,
                                         @RequestParam(defaultValue = "100000") Long expectedInsertions,
                                         @RequestParam(defaultValue = "0.01") Double falseProbability) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(expectedInsertions) && expectedInsertions > 0, "expectedInsertions必须大于0");
        Assert.isTrue(ObjectUtil.isNotNull(falseProbability) && falseProbability > 0 && falseProbability < 1,
                "falseProbability必须在0到1之间");

        redissonService.bloomInit(key, expectedInsertions, falseProbability);
        return ok(true);
    }

    /**
     * 添加布隆过滤器元素。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/bloom/add?key=bloom:user" \
     * -H "Content-Type: application/json" \
     * -d '"user:1"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否新增
     */
    @PostMapping("/bloom/add")
    public Map<String, Object> bloomAdd(@RequestParam String key,
                                        @RequestBody Object value) {
        checkKey(key);
        Assert.notNull(value, "value不能为空");

        boolean success = redissonService.bloomAdd(key, value);
        return ok(success);
    }

    /**
     * 批量添加布隆过滤器元素。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/bloom/add-all?key=bloom:user" \
     * -H "Content-Type: application/json" \
     * -d '["user:1","user:2","user:3"]'
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 新增数量
     */
    @PostMapping("/bloom/add-all")
    public Map<String, Object> bloomAddAll(@RequestParam String key,
                                           @RequestBody List<Object> values) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(values), "values不能为空");

        long count = redissonService.bloomAddAll(key, values);
        return ok(count);
    }

    /**
     * 判断布隆过滤器是否可能包含元素。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/bloom/contains?key=bloom:user" \
     * -H "Content-Type: application/json" \
     * -d '"user:1"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否可能包含
     */
    @PostMapping("/bloom/contains")
    public Map<String, Object> bloomContains(@RequestParam String key,
                                             @RequestBody Object value) {
        checkKey(key);
        Assert.notNull(value, "value不能为空");

        boolean exists = redissonService.bloomContains(key, value);
        return ok(exists);
    }

    /**
     * 判断布隆过滤器是否存在。
     * <p>
     * curl "http://localhost:8080/redisson/distributed/bloom/exists?key=bloom:user"
     *
     * @param key Redis 键
     * @return 是否存在
     */
    @GetMapping("/bloom/exists")
    public Map<String, Object> bloomExists(@RequestParam String key) {
        checkKey(key);

        boolean exists = redissonService.bloomExists(key);
        return ok(exists);
    }

    /**
     * 获取布隆过滤器信息。
     * <p>
     * curl "http://localhost:8080/redisson/distributed/bloom/info?key=bloom:user"
     *
     * @param key Redis 键
     * @return 布隆过滤器信息
     */
    @GetMapping("/bloom/info")
    public Map<String, Object> bloomInfo(@RequestParam String key) {
        checkKey(key);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exists", redissonService.bloomExists(key));
        data.put("expectedInsertions", redissonService.bloomGetExpectedInsertions(key));
        data.put("falseProbability", redissonService.bloomGetFalseProbability(key));

        RBloomFilter<Object> bloomFilter = redissonService.getBloomFilter(key);
        if (bloomFilter.isExists()) {
            data.put("count", bloomFilter.count());
            data.put("size", bloomFilter.getSize());
            data.put("hashIterations", bloomFilter.getHashIterations());
        }

        return ok(data);
    }

    /**
     * 删除布隆过滤器。
     * <p>
     * curl -X DELETE "http://localhost:8080/redisson/distributed/bloom?key=bloom:user"
     *
     * @param key Redis 键
     * @return 是否删除成功
     */
    @DeleteMapping("/bloom")
    public Map<String, Object> bloomDelete(@RequestParam String key) {
        checkKey(key);

        boolean success = redissonService.bloomDelete(key);
        return ok(success);
    }

    /**
     * 执行带锁任务。
     *
     * @param lockKey        锁 Key
     * @param lock           锁对象
     * @param waitSeconds    最大等待秒数
     * @param leaseSeconds   锁持有秒数
     * @param businessMillis 模拟业务耗时
     * @return 是否执行成功
     */
    private boolean executeWithRLock(String lockKey,
                                     RLock lock,
                                     long waitSeconds,
                                     long leaseSeconds,
                                     long businessMillis) {
        boolean locked = false;

        try {
            locked = leaseSeconds <= 0
                    ? lock.tryLock(waitSeconds, TimeUnit.SECONDS)
                    : lock.tryLock(waitSeconds, leaseSeconds, TimeUnit.SECONDS);

            if (!locked) {
                log.warn("获取分布式锁失败，lockKey={}", lockKey);
                return false;
            }

            log.info("获取分布式锁成功，lockKey={}，businessMillis={}", lockKey, businessMillis);
            ThreadUtil.sleep(businessMillis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断，lockKey={}", lockKey, e);
            return false;
        } finally {
            unlockSafely(lockKey, lock, locked);
        }
    }

    /**
     * 安全释放锁。
     *
     * @param lockKey 锁 Key
     * @param lock    锁对象
     * @param locked  是否已获取锁
     */
    private void unlockSafely(String lockKey, RLock lock, boolean locked) {
        if (!locked || ObjectUtil.isNull(lock)) {
            return;
        }

        if (!lock.isHeldByCurrentThread()) {
            log.warn("当前线程未持有分布式锁，跳过释放，lockKey={}", lockKey);
            return;
        }

        try {
            lock.unlock();
            log.info("释放分布式锁成功，lockKey={}", lockKey);
        } catch (Exception e) {
            log.error("释放分布式锁异常，lockKey={}", lockKey, e);
        }
    }

    /**
     * 校验 Redis Key。
     *
     * @param key Redis 键
     */
    private void checkKey(String key) {
        Assert.isTrue(StrUtil.isNotBlank(key), "Redis Key不能为空");
    }

    /**
     * 校验锁时间参数。
     *
     * @param waitSeconds  等待秒数
     * @param leaseSeconds 持有秒数
     */
    private void checkLockTime(Long waitSeconds, Long leaseSeconds) {
        Assert.isTrue(ObjectUtil.isNotNull(waitSeconds) && waitSeconds >= 0, "waitSeconds不能小于0");
        Assert.notNull(leaseSeconds, "leaseSeconds不能为空");
    }

    /**
     * 校验模拟业务耗时。
     *
     * @param businessMillis 模拟业务耗时
     */
    private void checkBusinessMillis(Long businessMillis) {
        Assert.isTrue(ObjectUtil.isNotNull(businessMillis) && businessMillis >= 0, "businessMillis不能小于0");
    }

    /**
     * 成功响应。
     *
     * @param data 响应数据
     * @return 响应 Map
     */
    private Map<String, Object> ok(Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("message", "操作成功");
        result.put("data", data);
        return result;
    }

}