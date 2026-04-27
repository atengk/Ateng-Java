package local.ateng.java.redis.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import local.ateng.java.redis.service.RedissonService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.BatchResult;
import org.redisson.api.RBatch;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RTransaction;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 高级能力测试控制器
 * 用于演示 RedissonService 的 Lua 脚本、脚本缓存、批处理、事务等高级能力。
 *
 * @author Ateng
 * @since 2026-04-26
 */
@Slf4j
@RestController
@RequestMapping("/redisson/advanced")
@RequiredArgsConstructor
public class RedissonAdvancedController {

    private final RedissonService redissonService;

    // -------------------------------------------------------------------------
    // Lua / Script
    // -------------------------------------------------------------------------

    /**
     * 执行 Lua 脚本。
     *
     * curl -X POST "http://localhost:8080/redisson/advanced/lua/eval" \
     *   -H "Content-Type: application/json" \
     *   -d '{"script":"return redis.call(\"get\", KEYS[1])","mode":"READ_ONLY","returnType":"VALUE","keys":["lua:test"],"args":[]}'
     *
     * @param request Lua 执行请求
     * @return 执行结果
     */
    @PostMapping("/lua/eval")
    public Map<String, Object> eval(@RequestBody LuaEvalRequest request) {
        checkLuaEvalRequest(request);

        Object result = redissonService.eval(
                request.getScript(),
                request.getMode(),
                request.getReturnType(),
                safeKeys(request.getKeys()),
                safeArgs(request.getArgs())
        );

        return ok(result);
    }

    /**
     * 执行 Lua 脚本并按 Object 返回。
     *
     * curl -X POST "http://localhost:8080/redisson/advanced/lua/eval-value" \
     *   -H "Content-Type: application/json" \
     *   -d '{"script":"redis.call(\"set\", KEYS[1], ARGV[1]); return redis.call(\"get\", KEYS[1])","keys":["lua:test"],"args":["hello"]}'
     *
     * @param request Lua 执行请求
     * @return 执行结果
     */
    @PostMapping("/lua/eval-value")
    public Map<String, Object> evalValue(@RequestBody LuaSimpleEvalRequest request) {
        checkLuaSimpleEvalRequest(request);

        Object result = redissonService.eval(
                request.getScript(),
                Object.class,
                safeKeys(request.getKeys()),
                safeArgs(request.getArgs())
        );

        return ok(result);
    }

    /**
     * 执行 Lua 脚本但不关心返回值。
     *
     * curl -X POST "http://localhost:8080/redisson/advanced/lua/eval-no-result" \
     *   -H "Content-Type: application/json" \
     *   -d '{"script":"redis.call(\"set\", KEYS[1], ARGV[1])","keys":["lua:test"],"args":["hello"]}'
     *
     * @param request Lua 执行请求
     * @return 执行结果
     */
    @PostMapping("/lua/eval-no-result")
    public Map<String, Object> evalNoResult(@RequestBody LuaSimpleEvalRequest request) {
        checkLuaSimpleEvalRequest(request);

        redissonService.evalNoResult(
                request.getScript(),
                safeKeys(request.getKeys()),
                safeArgs(request.getArgs())
        );

        return ok(true);
    }

    /**
     * 加载 Lua 脚本。
     *
     * curl -X POST "http://localhost:8080/redisson/advanced/lua/load" \
     *   -H "Content-Type: text/plain" \
     *   -d 'return redis.call("get", KEYS[1])'
     *
     * @param script Lua 脚本
     * @return SHA1
     */
    @PostMapping("/lua/load")
    public Map<String, Object> loadScript(@RequestBody String script) {
        Assert.isTrue(StrUtil.isNotBlank(script), "Lua脚本不能为空");

        String sha1 = redissonService.loadScript(script);
        log.info("Lua 脚本加载成功，sha1={}", sha1);
        return ok(sha1);
    }

    /**
     * 根据 SHA1 执行 Lua 脚本。
     *
     * curl -X POST "http://localhost:8080/redisson/advanced/lua/eval-sha" \
     *   -H "Content-Type: application/json" \
     *   -d '{"sha1":"xxxx","keys":["lua:test"],"args":[]}'
     *
     * @param request Lua SHA 执行请求
     * @return 执行结果
     */
    @PostMapping("/lua/eval-sha")
    public Map<String, Object> evalBySha(@RequestBody LuaEvalShaRequest request) {
        Assert.notNull(request, "请求体不能为空");
        Assert.isTrue(StrUtil.isNotBlank(request.getSha1()), "sha1不能为空");

        Object result = redissonService.evalBySha(
                request.getSha1(),
                Object.class,
                safeKeys(request.getKeys()),
                safeArgs(request.getArgs())
        );

        return ok(result);
    }

    /**
     * 使用 Lua 实现原子设置并返回旧值。
     *
     * curl -X POST "http://localhost:8080/redisson/advanced/lua/get-and-set?key=lua:atomic&value=newValue"
     *
     * @param key   Redis 键
     * @param value 新值
     * @return 旧值
     */
    @PostMapping("/lua/get-and-set")
    public Map<String, Object> luaGetAndSet(@RequestParam String key,
                                            @RequestParam String value) {
        checkKey(key);
        Assert.notNull(value, "value不能为空");

        String script = """
                local oldValue = redis.call('get', KEYS[1])
                redis.call('set', KEYS[1], ARGV[1])
                return oldValue
                """;

        Object oldValue = redissonService.eval(
                script,
                RScript.Mode.READ_WRITE,
                RScript.ReturnType.VALUE,
                List.of(key),
                value
        );

        return ok(oldValue);
    }

    /**
     * 使用 Lua 实现存在则递增，不存在则初始化。
     *
     * curl -X POST "http://localhost:8080/redisson/advanced/lua/increment-or-init?key=lua:counter&delta=2&initialValue=100"
     *
     * @param key          Redis 键
     * @param delta        增量
     * @param initialValue 初始值
     * @return 最新值
     */
    @PostMapping("/lua/increment-or-init")
    public Map<String, Object> luaIncrementOrInit(@RequestParam String key,
                                                  @RequestParam(defaultValue = "1") Long delta,
                                                  @RequestParam(defaultValue = "0") Long initialValue) {
        checkKey(key);
        Assert.notNull(delta, "delta不能为空");
        Assert.notNull(initialValue, "initialValue不能为空");

        String script = """
                if redis.call('exists', KEYS[1]) == 1 then
                    return redis.call('incrby', KEYS[1], ARGV[1])
                else
                    redis.call('set', KEYS[1], ARGV[2])
                    return tonumber(ARGV[2])
                end
                """;

        Object value = redissonService.eval(
                script,
                RScript.Mode.READ_WRITE,
                RScript.ReturnType.INTEGER,
                List.of(key),
                delta,
                initialValue
        );

        return ok(value);
    }

    // -------------------------------------------------------------------------
    // Batch
    // -------------------------------------------------------------------------

    /**
     * 批量设置 Bucket 值。
     *
     * curl -X POST "http://localhost:8080/redisson/advanced/batch/bucket-set" \
     *   -H "Content-Type: application/json" \
     *   -d '{"ttlSeconds":300,"items":[{"key":"batch:user:1","value":{"id":1,"name":"Ateng"}},{"key":"batch:user:2","value":{"id":2,"name":"Tom"}}]}'
     *
     * @param request 批量设置请求
     * @return 批处理结果
     */
    @PostMapping("/batch/bucket-set")
    public Map<String, Object> batchBucketSet(@RequestBody BatchBucketSetRequest request) {
        Assert.notNull(request, "请求体不能为空");
        Assert.isTrue(CollUtil.isNotEmpty(request.getItems()), "items不能为空");

        RBatch batch = redissonService.createBatch();

        for (BucketItem item : request.getItems()) {
            checkBucketItem(item);

            if (ObjectUtil.isNotNull(request.getTtlSeconds()) && request.getTtlSeconds() > 0) {
                batch.getBucket(item.getKey()).setAsync(item.getValue(), request.getTtlSeconds(), TimeUnit.SECONDS);
            } else {
                batch.getBucket(item.getKey()).setAsync(item.getValue());
            }
        }

        BatchResult<?> result = batch.execute();
        log.info("批量设置 Bucket 完成，size={}", request.getItems().size());

        return ok(batchResultToMap(result));
    }

    /**
     * 批量获取 Bucket 值。
     *
     * curl -X POST "http://localhost:8080/redisson/advanced/batch/bucket-get" \
     *   -H "Content-Type: application/json" \
     *   -d '{"keys":["batch:user:1","batch:user:2"]}'
     *
     * @param request 批量获取请求
     * @return 批处理结果
     */
    @PostMapping("/batch/bucket-get")
    public Map<String, Object> batchBucketGet(@RequestBody BatchBucketGetRequest request) {
        Assert.notNull(request, "请求体不能为空");
        Assert.isTrue(CollUtil.isNotEmpty(request.getKeys()), "keys不能为空");

        RBatch batch = redissonService.createBatch();

        for (String key : request.getKeys()) {
            checkKey(key);
            batch.getBucket(key).getAsync();
        }

        BatchResult<?> result = batch.execute();
        return ok(batchResultToMap(result));
    }

    /**
     * 批量删除 Key。
     *
     * curl -X POST "http://localhost:8080/redisson/advanced/batch/key-delete" \
     *   -H "Content-Type: application/json" \
     *   -d '{"keys":["batch:user:1","batch:user:2"]}'
     *
     * @param request 批量删除请求
     * @return 批处理结果
     */
    @PostMapping("/batch/key-delete")
    public Map<String, Object> batchKeyDelete(@RequestBody BatchBucketGetRequest request) {
        Assert.notNull(request, "请求体不能为空");
        Assert.isTrue(CollUtil.isNotEmpty(request.getKeys()), "keys不能为空");

        RBatch batch = redissonService.createBatch();

        for (String key : request.getKeys()) {
            checkKey(key);
            batch.getKeys().deleteAsync(key);
        }

        BatchResult<?> result = batch.execute();
        log.info("批量删除 Key 完成，keys={}", request.getKeys());

        return ok(batchResultToMap(result));
    }

    /**
     * 批量计数器递增。
     *
     * curl -X POST "http://localhost:8080/redisson/advanced/batch/atomic-long-increment" \
     *   -H "Content-Type: application/json" \
     *   -d '{"items":[{"key":"batch:count:1","delta":1},{"key":"batch:count:2","delta":10}]}'
     *
     * @param request 批量递增请求
     * @return 批处理结果
     */
    @PostMapping("/batch/atomic-long-increment")
    public Map<String, Object> batchAtomicLongIncrement(@RequestBody BatchAtomicLongIncrementRequest request) {
        Assert.notNull(request, "请求体不能为空");
        Assert.isTrue(CollUtil.isNotEmpty(request.getItems()), "items不能为空");

        RBatch batch = redissonService.createBatch();

        for (AtomicLongIncrementItem item : request.getItems()) {
            Assert.notNull(item, "item不能为空");
            checkKey(item.getKey());
            Assert.notNull(item.getDelta(), "delta不能为空");

            batch.getAtomicLong(item.getKey()).addAndGetAsync(item.getDelta());
        }

        BatchResult<?> result = batch.execute();
        return ok(batchResultToMap(result));
    }

    // -------------------------------------------------------------------------
    // Transaction
    // -------------------------------------------------------------------------

    /**
     * 事务设置 Bucket 值。
     *
     * curl -X POST "http://localhost:8080/redisson/advanced/transaction/bucket-set" \
     *   -H "Content-Type: application/json" \
     *   -d '{"items":[{"key":"tx:user:1","value":{"id":1,"name":"Ateng"}},{"key":"tx:user:2","value":{"id":2,"name":"Tom"}}]}'
     *
     * @param request 事务设置请求
     * @return 执行结果
     */
    @PostMapping("/transaction/bucket-set")
    public Map<String, Object> transactionBucketSet(@RequestBody BatchBucketSetRequest request) {
        Assert.notNull(request, "请求体不能为空");
        Assert.isTrue(CollUtil.isNotEmpty(request.getItems()), "items不能为空");

        RTransaction transaction = redissonService.createTransaction();

        try {
            for (BucketItem item : request.getItems()) {
                checkBucketItem(item);

                RBucket<Object> bucket = transaction.getBucket(item.getKey());
                if (ObjectUtil.isNotNull(request.getTtlSeconds()) && request.getTtlSeconds() > 0) {
                    bucket.set(item.getValue(), request.getTtlSeconds(), TimeUnit.SECONDS);
                } else {
                    bucket.set(item.getValue());
                }
            }

            transaction.commit();
            log.info("事务设置 Bucket 成功，size={}", request.getItems().size());
            return ok(true);
        } catch (Exception e) {
            rollbackSafely(transaction);
            log.error("事务设置 Bucket 失败，已回滚", e);
            throw e;
        }
    }

    /**
     * Lua 原子转账示例。
     * 使用 Redis 数值 Key 模拟账户余额，from 扣减，to 增加。
     *
     * curl -X POST "http://localhost:8080/redisson/advanced/transaction/transfer?fromKey=account:1001&toKey=account:1002&amount=10"
     *
     * @param fromKey 转出账户 Key
     * @param toKey   转入账户 Key
     * @param amount  金额
     * @return 执行结果
     */
    @PostMapping("/transaction/transfer")
    public Map<String, Object> transactionTransfer(@RequestParam String fromKey,
                                                   @RequestParam String toKey,
                                                   @RequestParam Long amount) {
        checkKey(fromKey);
        checkKey(toKey);
        Assert.isTrue(ObjectUtil.isNotNull(amount) && amount > 0, "amount必须大于0");

        String script = """
                local amount = tonumber(ARGV[1])
                if amount == nil or amount <= 0 then
                    return redis.error_reply('amount invalid')
                end

                local fromBalance = tonumber(redis.call('get', KEYS[1]) or '0')
                if fromBalance < amount then
                    return redis.error_reply('balance not enough')
                end

                local latestFrom = redis.call('decrby', KEYS[1], amount)
                local latestTo = redis.call('incrby', KEYS[2], amount)

                return tostring(latestFrom) .. ':' .. tostring(latestTo)
                """;

        Object result = redissonService.eval(
                script,
                RScript.Mode.READ_WRITE,
                RScript.ReturnType.VALUE,
                List.of(fromKey, toKey),
                amount
        );

        String resultText = String.valueOf(result);
        List<String> parts = StrUtil.split(resultText, ":");
        Assert.isTrue(parts.size() == 2, "Lua转账结果格式异常");

        long latestFrom = Long.parseLong(parts.get(0));
        long latestTo = Long.parseLong(parts.get(1));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("fromKey", fromKey);
        data.put("toKey", toKey);
        data.put("amount", amount);
        data.put("fromBalance", latestFrom);
        data.put("toBalance", latestTo);

        log.info("Lua 原子转账成功，fromKey={}，toKey={}，amount={}", fromKey, toKey, amount);
        return ok(data);
    }

    /**
     * 初始化事务转账账户余额。
     *
     * curl -X POST "http://localhost:8080/redisson/advanced/transaction/account/init?accountKey=account:1001&balance=100"
     *
     * @param accountKey 账户 Key
     * @param balance    余额
     * @return 执行结果
     */
    @PostMapping("/transaction/account/init")
    public Map<String, Object> initAccount(@RequestParam String accountKey,
                                           @RequestParam Long balance) {
        checkKey(accountKey);
        Assert.isTrue(ObjectUtil.isNotNull(balance) && balance >= 0, "balance不能小于0");

        redissonService.setAtomicLong(accountKey, balance);
        log.info("账户余额初始化成功，accountKey={}，balance={}", accountKey, balance);
        return ok(true);
    }

    /**
     * 查询事务转账账户余额。
     *
     * curl "http://localhost:8080/redisson/advanced/transaction/account/balance?accountKey=account:1001"
     *
     * @param accountKey 账户 Key
     * @return 余额
     */
    @GetMapping("/transaction/account/balance")
    public Map<String, Object> accountBalance(@RequestParam String accountKey) {
        checkKey(accountKey);

        long balance = redissonService.getAtomicLongValue(accountKey);
        return ok(balance);
    }

    /**
     * 获取批处理和事务对象信息。
     *
     * curl "http://localhost:8080/redisson/advanced/info"
     *
     * @return 对象信息
     */
    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("scriptClass", redissonService.getScript().getClass().getName());
        data.put("batchClass", redissonService.createBatch().getClass().getName());
        data.put("transactionClass", redissonService.createTransaction().getClass().getName());
        return ok(data);
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
     * 校验 Bucket 项。
     *
     * @param item Bucket 项
     */
    private void checkBucketItem(BucketItem item) {
        Assert.notNull(item, "item不能为空");
        checkKey(item.getKey());
        Assert.notNull(item.getValue(), "value不能为空");
    }

    /**
     * 获取安全 KEYS。
     *
     * @param keys KEYS
     * @return KEYS
     */
    private List<Object> safeKeys(List<Object> keys) {
        if (CollUtil.isEmpty(keys)) {
            return Collections.emptyList();
        }
        return keys;
    }

    /**
     * 获取安全 ARGV。
     *
     * @param args ARGV
     * @return ARGV 数组
     */
    private Object[] safeArgs(List<Object> args) {
        if (CollUtil.isEmpty(args)) {
            return new Object[0];
        }
        return args.toArray();
    }

    /**
     * 转换批处理结果。
     *
     * @param result 批处理结果
     * @return 响应数据
     */
    private Map<String, Object> batchResultToMap(BatchResult<?> result) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("responses", ObjectUtil.isNull(result) ? Collections.emptyList() : result.getResponses());
        return data;
    }

    /**
     * 安全回滚事务。
     *
     * @param transaction 事务对象
     */
    private void rollbackSafely(RTransaction transaction) {
        if (ObjectUtil.isNull(transaction)) {
            return;
        }

        try {
            transaction.rollback();
        } catch (Exception e) {
            log.error("Redis 事务回滚异常", e);
        }
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

    /**
     * Lua 完整执行请求。
     *
     * @author Ateng
     * @since 2026-04-26
     */
    @Data
    public static class LuaEvalRequest {

        /**
         * Lua 脚本。
         */
        private String script;

        /**
         * 执行模式。
         */
        private RScript.Mode mode = RScript.Mode.READ_WRITE;

        /**
         * 返回类型。
         */
        private RScript.ReturnType returnType = RScript.ReturnType.VALUE;

        /**
         * KEYS 参数。
         */
        private List<Object> keys;

        /**
         * ARGV 参数。
         */
        private List<Object> args;

    }

    /**
     * Lua 简单执行请求。
     *
     * @author Ateng
     * @since 2026-04-26
     */
    @Data
    public static class LuaSimpleEvalRequest {

        /**
         * Lua 脚本。
         */
        private String script;

        /**
         * KEYS 参数。
         */
        private List<Object> keys;

        /**
         * ARGV 参数。
         */
        private List<Object> args;

    }

    /**
     * Lua SHA 执行请求。
     *
     * @author Ateng
     * @since 2026-04-26
     */
    @Data
    public static class LuaEvalShaRequest {

        /**
         * 脚本 SHA1。
         */
        private String sha1;

        /**
         * KEYS 参数。
         */
        private List<Object> keys;

        /**
         * ARGV 参数。
         */
        private List<Object> args;

    }

    /**
     * 批量 Bucket 设置请求。
     *
     * @author Ateng
     * @since 2026-04-26
     */
    @Data
    public static class BatchBucketSetRequest {

        /**
         * 过期秒数。
         */
        private Long ttlSeconds;

        /**
         * Bucket 项集合。
         */
        private List<BucketItem> items;

    }

    /**
     * Bucket 项。
     *
     * @author Ateng
     * @since 2026-04-26
     */
    @Data
    public static class BucketItem {

        /**
         * Redis 键。
         */
        private String key;

        /**
         * Redis 值。
         */
        private Object value;

    }

    /**
     * 批量 Bucket 获取请求。
     *
     * @author Ateng
     * @since 2026-04-26
     */
    @Data
    public static class BatchBucketGetRequest {

        /**
         * Redis 键集合。
         */
        private List<String> keys;

    }

    /**
     * 批量 AtomicLong 递增请求。
     *
     * @author Ateng
     * @since 2026-04-26
     */
    @Data
    public static class BatchAtomicLongIncrementRequest {

        /**
         * 递增项集合。
         */
        private List<AtomicLongIncrementItem> items;

    }

    /**
     * AtomicLong 递增项。
     *
     * @author Ateng
     * @since 2026-04-26
     */
    @Data
    public static class AtomicLongIncrementItem {

        /**
         * Redis 键。
         */
        private String key;

        /**
         * 增量。
         */
        private Long delta;

    }

    /**
     * 校验 Lua 完整执行请求。
     *
     * @param request Lua 请求
     */
    private void checkLuaEvalRequest(LuaEvalRequest request) {
        Assert.notNull(request, "请求体不能为空");
        Assert.isTrue(StrUtil.isNotBlank(request.getScript()), "Lua脚本不能为空");
        Assert.notNull(request.getMode(), "mode不能为空");
        Assert.notNull(request.getReturnType(), "returnType不能为空");
    }

    /**
     * 校验 Lua 简单执行请求。
     *
     * @param request Lua 请求
     */
    private void checkLuaSimpleEvalRequest(LuaSimpleEvalRequest request) {
        Assert.notNull(request, "请求体不能为空");
        Assert.isTrue(StrUtil.isNotBlank(request.getScript()), "Lua脚本不能为空");
    }

}