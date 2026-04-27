package local.ateng.java.redis.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import local.ateng.java.redis.service.RedissonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 基础能力测试控制器
 * 用于演示 RedissonService 的 Key 管理、Bucket、计数器、分布式 ID 等基础能力。
 *
 * @author Ateng
 * @since 2026-04-26
 */
@Slf4j
@RestController
@RequestMapping("/redisson/basic")
@RequiredArgsConstructor
public class RedissonBasicController {

    private final RedissonService redissonService;

    /**
     * 获取 RedissonClient 基础信息。
     * <p>
     * curl "http://localhost:8080/redisson/basic/client"
     *
     * @return RedissonClient 信息
     */
    @GetMapping("/client")
    public Map<String, Object> client() {
        RedissonClient client = redissonService.getClient();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("className", client.getClass().getName());
        data.put("shutdown", client.isShutdown());
        data.put("shuttingDown", client.isShuttingDown());

        return ok(data);
    }

    /**
     * 判断 Key 是否存在。
     * <p>
     * curl "http://localhost:8080/redisson/basic/key/exists?key=user:1"
     *
     * @param key Redis 键
     * @return 是否存在
     */
    @GetMapping("/key/exists")
    public Map<String, Object> hasKey(@RequestParam String key) {
        checkKey(key);

        boolean exists = redissonService.hasKey(key);
        return ok(exists);
    }

    /**
     * 统计多个 Key 中存在的数量。
     * <p>
     * curl "http://localhost:8080/redisson/basic/key/count-exists?keys=user:1&keys=user:2"
     *
     * @param keys Redis 键集合
     * @return 存在数量
     */
    @GetMapping("/key/count-exists")
    public Map<String, Object> countExists(@RequestParam List<String> keys) {
        Assert.isTrue(CollUtil.isNotEmpty(keys), "keys不能为空");

        long count = redissonService.countExists(keys.toArray(new String[0]));
        return ok(count);
    }

    /**
     * 删除指定 Key。
     * <p>
     * curl -X DELETE "http://localhost:8080/redisson/basic/key?key=user:1"
     *
     * @param key Redis 键
     * @return 是否删除成功
     */
    @DeleteMapping("/key")
    public Map<String, Object> deleteKey(@RequestParam String key) {
        checkKey(key);

        boolean deleted = redissonService.deleteKey(key);
        log.info("删除 Redis Key，key={}，deleted={}", key, deleted);
        return ok(deleted);
    }

    /**
     * 批量删除 Key。
     * <p>
     * curl -X DELETE "http://localhost:8080/redisson/basic/keys?keys=user:1&keys=user:2"
     *
     * @param keys Redis 键集合
     * @return 删除数量
     */
    @DeleteMapping("/keys")
    public Map<String, Object> deleteKeys(@RequestParam List<String> keys) {
        Assert.isTrue(CollUtil.isNotEmpty(keys), "keys不能为空");

        long count = redissonService.deleteKeys(keys);
        log.info("批量删除 Redis Key，keys={}，count={}", keys, count);
        return ok(count);
    }

    /**
     * 根据 pattern 删除 Key。
     * <p>
     * curl -X DELETE "http://localhost:8080/redisson/basic/keys/pattern?pattern=test:*"
     *
     * @param pattern 通配符表达式
     * @return 删除数量
     */
    @DeleteMapping("/keys/pattern")
    public Map<String, Object> deleteByPattern(@RequestParam String pattern) {
        checkKey(pattern);

        long count = redissonService.deleteByPattern(pattern);
        log.info("根据 pattern 删除 Redis Key，pattern={}，count={}", pattern, count);
        return ok(count);
    }

    /**
     * 设置 Key 过期时间。
     * <p>
     * curl -X PUT "http://localhost:8080/redisson/basic/key/expire?key=user:1&timeout=60"
     *
     * @param key     Redis 键
     * @param timeout 过期时间，单位秒
     * @return 是否设置成功
     */
    @PutMapping("/key/expire")
    public Map<String, Object> expire(@RequestParam String key,
                                      @RequestParam Long timeout) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(timeout) && timeout > 0, "timeout必须大于0");

        boolean success = redissonService.expire(key, timeout, TimeUnit.SECONDS);
        return ok(success);
    }

    /**
     * 获取 Key 剩余过期时间。
     * <p>
     * curl "http://localhost:8080/redisson/basic/key/ttl?key=user:1"
     *
     * @param key Redis 键
     * @return 剩余秒数，-1 表示永久，-2 表示不存在
     */
    @GetMapping("/key/ttl")
    public Map<String, Object> ttl(@RequestParam String key) {
        checkKey(key);

        long ttl = redissonService.getTtl(key, TimeUnit.SECONDS);
        return ok(ttl);
    }

    /**
     * 移除 Key 过期时间。
     * <p>
     * curl -X PUT "http://localhost:8080/redisson/basic/key/persist?key=user:1"
     *
     * @param key Redis 键
     * @return 是否成功
     */
    @PutMapping("/key/persist")
    public Map<String, Object> persist(@RequestParam String key) {
        checkKey(key);

        boolean success = redissonService.persist(key);
        return ok(success);
    }

    /**
     * 重命名 Key。
     * <p>
     * curl -X PUT "http://localhost:8080/redisson/basic/key/rename?oldKey=user:1&newKey=user:100"
     *
     * @param oldKey 旧 Key
     * @param newKey 新 Key
     * @return 是否成功
     */
    @PutMapping("/key/rename")
    public Map<String, Object> rename(@RequestParam String oldKey,
                                      @RequestParam String newKey) {
        checkKey(oldKey);
        checkKey(newKey);

        boolean success = redissonService.renameKey(oldKey, newKey);
        return ok(success);
    }

    /**
     * 新 Key 不存在时重命名。
     * <p>
     * curl -X PUT "http://localhost:8080/redisson/basic/key/rename-if-absent?oldKey=user:1&newKey=user:100"
     *
     * @param oldKey 旧 Key
     * @param newKey 新 Key
     * @return 是否成功
     */
    @PutMapping("/key/rename-if-absent")
    public Map<String, Object> renameIfAbsent(@RequestParam String oldKey,
                                              @RequestParam String newKey) {
        checkKey(oldKey);
        checkKey(newKey);

        boolean success = redissonService.renameKeyIfAbsent(oldKey, newKey);
        return ok(success);
    }

    /**
     * 查询匹配 pattern 的 Key。
     * <p>
     * curl "http://localhost:8080/redisson/basic/keys/scan?pattern=user:*&count=20"
     *
     * @param pattern 通配符表达式
     * @param count   最大返回数量
     * @return Key 集合
     */
    @GetMapping("/keys/scan")
    public Map<String, Object> scanKeys(@RequestParam String pattern,
                                        @RequestParam(defaultValue = "20") Integer count) {
        checkKey(pattern);
        Assert.isTrue(ObjectUtil.isNotNull(count) && count > 0, "count必须大于0");

        return ok(redissonService.scanKeys(pattern, count));
    }

    /**
     * 获取 Key 类型。
     * <p>
     * curl "http://localhost:8080/redisson/basic/key/type?key=user:1"
     *
     * @param key Redis 键
     * @return 类型
     */
    @GetMapping("/key/type")
    public Map<String, Object> keyType(@RequestParam String key) {
        checkKey(key);

        String type = redissonService.getKeyType(key);
        return ok(type);
    }

    /**
     * 设置 Bucket 值。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/basic/bucket/set?key=user:1&ttlSeconds=300" \\
     * -H "Content-Type: application/json" \\
     * -d '{"id":1,"name":"Ateng"}'
     *
     * @param key        Redis 键
     * @param ttlSeconds 过期秒数，不传则永久
     * @param value      值
     * @return 执行结果
     */
    @PostMapping("/bucket/set")
    public Map<String, Object> set(@RequestParam String key,
                                   @RequestParam(required = false) Long ttlSeconds,
                                   @RequestBody Object value) {
        checkKey(key);

        if (ObjectUtil.isNotNull(ttlSeconds) && ttlSeconds > 0) {
            redissonService.set(key, value, Duration.ofSeconds(ttlSeconds));
        } else {
            redissonService.set(key, value);
        }

        log.info("设置 Redis Bucket 成功，key={}，ttlSeconds={}", key, ttlSeconds);
        return ok(true);
    }

    /**
     * 获取 Bucket 值。
     * <p>
     * curl "http://localhost:8080/redisson/basic/bucket/get?key=user:1"
     *
     * @param key Redis 键
     * @return 值
     */
    @GetMapping("/bucket/get")
    public Map<String, Object> get(@RequestParam String key) {
        checkKey(key);

        Object value = redissonService.get(key, Object.class);
        return ok(value);
    }

    /**
     * Key 不存在时设置 Bucket 值。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/basic/bucket/set-if-absent?key=lock:test&ttlSeconds=60" \\
     * -H "Content-Type: application/json" \\
     * -d '"value"'
     *
     * @param key        Redis 键
     * @param ttlSeconds 过期秒数
     * @param value      值
     * @return 是否设置成功
     */
    @PostMapping("/bucket/set-if-absent")
    public Map<String, Object> setIfAbsent(@RequestParam String key,
                                           @RequestParam Long ttlSeconds,
                                           @RequestBody Object value) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(ttlSeconds) && ttlSeconds > 0, "ttlSeconds必须大于0");

        boolean success = redissonService.setIfAbsent(key, value, Duration.ofSeconds(ttlSeconds));
        return ok(success);
    }

    /**
     * Key 存在时设置 Bucket 值。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/basic/bucket/set-if-exists?key=user:1&ttlSeconds=300" \\
     * -H "Content-Type: application/json" \\
     * -d '{"id":1,"name":"Ateng Updated"}'
     *
     * @param key        Redis 键
     * @param ttlSeconds 过期秒数，不传则不改过期时间
     * @param value      值
     * @return 是否设置成功
     */
    @PostMapping("/bucket/set-if-exists")
    public Map<String, Object> setIfExists(@RequestParam String key,
                                           @RequestParam(required = false) Long ttlSeconds,
                                           @RequestBody Object value) {
        checkKey(key);

        boolean success;
        if (ObjectUtil.isNotNull(ttlSeconds) && ttlSeconds > 0) {
            success = redissonService.setIfExists(key, value, Duration.ofSeconds(ttlSeconds));
        } else {
            success = redissonService.setIfExists(key, value);
        }

        return ok(success);
    }

    /**
     * 原子替换 Bucket 值并返回旧值。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/basic/bucket/get-and-set?key=user:1" \\
     * -H "Content-Type: application/json" \\
     * -d '{"id":1,"name":"new"}'
     *
     * @param key   Redis 键
     * @param value 新值
     * @return 旧值
     */
    @PostMapping("/bucket/get-and-set")
    public Map<String, Object> getAndSet(@RequestParam String key,
                                         @RequestBody Object value) {
        checkKey(key);

        Object oldValue = redissonService.getAndSet(key, value, Object.class);
        return ok(oldValue);
    }

    /**
     * 获取并删除 Bucket 值。
     * <p>
     * curl -X DELETE "http://localhost:8080/redisson/basic/bucket/get-and-delete?key=user:1"
     *
     * @param key Redis 键
     * @return 删除前的值
     */
    @DeleteMapping("/bucket/get-and-delete")
    public Map<String, Object> getAndDelete(@RequestParam String key) {
        checkKey(key);

        Object oldValue = redissonService.getAndDelete(key, Object.class);
        return ok(oldValue);
    }

    /**
     * 批量获取 Bucket 值。
     * <p>
     * curl "http://localhost:8080/redisson/basic/bucket/entries?keys=user:1&keys=user:2"
     *
     * @param keys Redis 键集合
     * @return Key-Value Map
     */
    @GetMapping("/bucket/entries")
    public Map<String, Object> entries(@RequestParam List<String> keys) {
        Assert.isTrue(CollUtil.isNotEmpty(keys), "keys不能为空");

        Map<String, Object> data = redissonService.entries(keys);
        return ok(data);
    }

    /**
     * 获取 Bucket 值大小。
     * <p>
     * curl "http://localhost:8080/redisson/basic/bucket/size?key=user:1"
     *
     * @param key Redis 键
     * @return 字节大小
     */
    @GetMapping("/bucket/size")
    public Map<String, Object> size(@RequestParam String key) {
        checkKey(key);

        long size = redissonService.size(key);
        return ok(size);
    }

    /**
     * AtomicLong 自增。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/basic/atomic-long/increment?key=count:like:1&delta=1"
     *
     * @param key   Redis 键
     * @param delta 增量
     * @return 最新值
     */
    @PostMapping("/atomic-long/increment")
    public Map<String, Object> increment(@RequestParam String key,
                                         @RequestParam(defaultValue = "1") Long delta) {
        checkKey(key);

        long value = redissonService.increment(key, delta);
        return ok(value);
    }

    /**
     * AtomicLong 自减。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/basic/atomic-long/decrement?key=count:like:1&delta=1"
     *
     * @param key   Redis 键
     * @param delta 减量
     * @return 最新值
     */
    @PostMapping("/atomic-long/decrement")
    public Map<String, Object> decrement(@RequestParam String key,
                                         @RequestParam(defaultValue = "1") Long delta) {
        checkKey(key);

        long value = redissonService.decrement(key, delta);
        return ok(value);
    }

    /**
     * 设置 AtomicLong 值。
     * <p>
     * curl -X PUT "http://localhost:8080/redisson/basic/atomic-long/set?key=count:like:1&value=100"
     *
     * @param key   Redis 键
     * @param value 值
     * @return 执行结果
     */
    @PutMapping("/atomic-long/set")
    public Map<String, Object> setAtomicLong(@RequestParam String key,
                                             @RequestParam Long value) {
        checkKey(key);
        Assert.notNull(value, "value不能为空");

        redissonService.setAtomicLong(key, value);
        return ok(true);
    }

    /**
     * 获取 AtomicLong 值。
     * <p>
     * curl "http://localhost:8080/redisson/basic/atomic-long/get?key=count:like:1"
     *
     * @param key Redis 键
     * @return 当前值
     */
    @GetMapping("/atomic-long/get")
    public Map<String, Object> getAtomicLong(@RequestParam String key) {
        checkKey(key);

        long value = redissonService.getAtomicLongValue(key);
        return ok(value);
    }

    /**
     * 重置 AtomicLong 值。
     * <p>
     * curl -X PUT "http://localhost:8080/redisson/basic/atomic-long/reset?key=count:like:1"
     *
     * @param key Redis 键
     * @return 执行结果
     */
    @PutMapping("/atomic-long/reset")
    public Map<String, Object> resetAtomicLong(@RequestParam String key) {
        checkKey(key);

        redissonService.resetAtomicLong(key);
        return ok(true);
    }

    /**
     * AtomicDouble 自增。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/basic/atomic-double/increment?key=score:user:1&delta=1.5"
     *
     * @param key   Redis 键
     * @param delta 增量
     * @return 最新值
     */
    @PostMapping("/atomic-double/increment")
    public Map<String, Object> incrementDouble(@RequestParam String key,
                                               @RequestParam(defaultValue = "1") Double delta) {
        checkKey(key);

        double value = redissonService.incrementDouble(key, delta);
        return ok(value);
    }

    /**
     * AtomicDouble 自减。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/basic/atomic-double/decrement?key=score:user:1&delta=1.5"
     *
     * @param key   Redis 键
     * @param delta 减量
     * @return 最新值
     */
    @PostMapping("/atomic-double/decrement")
    public Map<String, Object> decrementDouble(@RequestParam String key,
                                               @RequestParam(defaultValue = "1") Double delta) {
        checkKey(key);

        double value = redissonService.decrementDouble(key, delta);
        return ok(value);
    }

    /**
     * 初始化分布式 ID 生成器。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/basic/id-generator/init?key=id:order&initialValue=1000&allocationSize=100"
     *
     * @param key            Redis 键
     * @param initialValue   初始值
     * @param allocationSize 分配步长
     * @return 是否初始化成功
     */
    @PostMapping("/id-generator/init")
    public Map<String, Object> idGeneratorInit(@RequestParam String key,
                                               @RequestParam(defaultValue = "1") Long initialValue,
                                               @RequestParam(defaultValue = "100") Long allocationSize) {
        checkKey(key);

        boolean success = redissonService.idGeneratorInit(key, initialValue, allocationSize);
        return ok(success);
    }

    /**
     * 获取下一个分布式 ID。
     * <p>
     * curl "http://localhost:8080/redisson/basic/id-generator/next?key=id:order"
     *
     * @param key Redis 键
     * @return ID
     */
    @GetMapping("/id-generator/next")
    public Map<String, Object> nextId(@RequestParam String key) {
        checkKey(key);

        long id = redissonService.nextId(key);
        return ok(id);
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