package local.ateng.java.redis.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import local.ateng.java.redis.service.RedissonService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.*;

/**
 * Redisson 数据结构测试控制器
 * 用于演示 RedissonService 的 Hash、MapCache、List、Deque、Set、SetCache、ZSet 等能力。
 *
 * @author Ateng
 * @since 2026-04-26
 */
@Slf4j
@RestController
@RequestMapping("/redisson/data")
@RequiredArgsConstructor
public class RedissonDataStructureController {

    private final RedissonService redissonService;

    // -------------------------------------------------------------------------
    // Hash / Map
    // -------------------------------------------------------------------------

    /**
     * 设置 Hash 字段值。
     *
     * curl -X POST "http://localhost:8080/redisson/data/hash/put?key=user:hash:1&field=name" \
     *   -H "Content-Type: application/json" \
     *   -d '"Ateng"'
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param value 字段值
     * @return 执行结果
     */
    @PostMapping("/hash/put")
    public Map<String, Object> hPut(@RequestParam String key,
                                    @RequestParam String field,
                                    @RequestBody Object value) {
        checkKey(key);
        checkField(field);

        redissonService.hPut(key, field, value);
        log.info("Hash 字段写入成功，key={}，field={}", key, field);
        return ok(true);
    }

    /**
     * 批量设置 Hash 字段值。
     *
     * curl -X POST "http://localhost:8080/redisson/data/hash/put-all?key=user:hash:1" \
     *   -H "Content-Type: application/json" \
     *   -d '{"name":"Ateng","age":18,"city":"Chongqing"}'
     *
     * @param key Redis 键
     * @param map 字段 Map
     * @return 执行结果
     */
    @PostMapping("/hash/put-all")
    public Map<String, Object> hPutAll(@RequestParam String key,
                                       @RequestBody Map<String, Object> map) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(map), "字段Map不能为空");

        redissonService.hPutAll(key, map);
        log.info("Hash 字段批量写入成功，key={}，size={}", key, map.size());
        return ok(true);
    }

    /**
     * 字段不存在时设置 Hash 字段值。
     *
     * curl -X POST "http://localhost:8080/redisson/data/hash/put-if-absent?key=user:hash:1&field=email" \
     *   -H "Content-Type: application/json" \
     *   -d '"ateng@example.com"'
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param value 字段值
     * @return 是否设置成功
     */
    @PostMapping("/hash/put-if-absent")
    public Map<String, Object> hPutIfAbsent(@RequestParam String key,
                                            @RequestParam String field,
                                            @RequestBody Object value) {
        checkKey(key);
        checkField(field);

        boolean success = redissonService.hPutIfAbsent(key, field, value);
        return ok(success);
    }

    /**
     * 获取 Hash 字段值。
     *
     * curl "http://localhost:8080/redisson/data/hash/get?key=user:hash:1&field=name"
     *
     * @param key   Redis 键
     * @param field 字段名
     * @return 字段值
     */
    @GetMapping("/hash/get")
    public Map<String, Object> hGet(@RequestParam String key,
                                    @RequestParam String field) {
        checkKey(key);
        checkField(field);

        Object value = redissonService.hGet(key, field, Object.class);
        return ok(value);
    }

    /**
     * 批量获取 Hash 字段值。
     *
     * curl "http://localhost:8080/redisson/data/hash/multi-get?key=user:hash:1&fields=name&fields=age"
     *
     * @param key    Redis 键
     * @param fields 字段集合
     * @return 字段值 Map
     */
    @GetMapping("/hash/multi-get")
    public Map<String, Object> hMultiGet(@RequestParam String key,
                                         @RequestParam List<String> fields) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(fields), "fields不能为空");

        Map<String, Object> data = redissonService.hMultiGet(key, fields);
        return ok(data);
    }

    /**
     * 删除 Hash 字段。
     *
     * curl -X DELETE "http://localhost:8080/redisson/data/hash/delete?key=user:hash:1&fields=name&fields=age"
     *
     * @param key    Redis 键
     * @param fields 字段集合
     * @return 删除数量
     */
    @DeleteMapping("/hash/delete")
    public Map<String, Object> hDelete(@RequestParam String key,
                                       @RequestParam List<String> fields) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(fields), "fields不能为空");

        long count = redissonService.hDelete(key, fields.toArray(new String[0]));
        log.info("Hash 字段删除成功，key={}，fields={}，count={}", key, fields, count);
        return ok(count);
    }

    /**
     * 判断 Hash 字段是否存在。
     *
     * curl "http://localhost:8080/redisson/data/hash/has-key?key=user:hash:1&field=name"
     *
     * @param key   Redis 键
     * @param field 字段名
     * @return 是否存在
     */
    @GetMapping("/hash/has-key")
    public Map<String, Object> hHasKey(@RequestParam String key,
                                       @RequestParam String field) {
        checkKey(key);
        checkField(field);

        boolean exists = redissonService.hHasKey(key, field);
        return ok(exists);
    }

    /**
     * 获取 Hash 全部字段和值。
     *
     * curl "http://localhost:8080/redisson/data/hash/entries?key=user:hash:1"
     *
     * @param key Redis 键
     * @return 字段值 Map
     */
    @GetMapping("/hash/entries")
    public Map<String, Object> hEntries(@RequestParam String key) {
        checkKey(key);

        Map<String, Object> data = redissonService.hEntries(key);
        return ok(data);
    }

    /**
     * 获取 Hash 全部字段名。
     *
     * curl "http://localhost:8080/redisson/data/hash/keys?key=user:hash:1"
     *
     * @param key Redis 键
     * @return 字段名集合
     */
    @GetMapping("/hash/keys")
    public Map<String, Object> hKeys(@RequestParam String key) {
        checkKey(key);

        Set<String> data = redissonService.hKeys(key);
        return ok(data);
    }

    /**
     * 获取 Hash 全部字段值。
     *
     * curl "http://localhost:8080/redisson/data/hash/values?key=user:hash:1"
     *
     * @param key Redis 键
     * @return 字段值集合
     */
    @GetMapping("/hash/values")
    public Map<String, Object> hValues(@RequestParam String key) {
        checkKey(key);

        Collection<Object> data = redissonService.hValues(key);
        return ok(data);
    }

    /**
     * 获取 Hash 字段数量。
     *
     * curl "http://localhost:8080/redisson/data/hash/size?key=user:hash:1"
     *
     * @param key Redis 键
     * @return 字段数量
     */
    @GetMapping("/hash/size")
    public Map<String, Object> hSize(@RequestParam String key) {
        checkKey(key);

        int size = redissonService.hSize(key);
        return ok(size);
    }

    /**
     * Hash 字段整数自增。
     *
     * curl -X POST "http://localhost:8080/redisson/data/hash/increment?key=user:hash:1&field=count&delta=1"
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param delta 增量
     * @return 最新值
     */
    @PostMapping("/hash/increment")
    public Map<String, Object> hIncrement(@RequestParam String key,
                                          @RequestParam String field,
                                          @RequestParam(defaultValue = "1") Long delta) {
        checkKey(key);
        checkField(field);

        long value = redissonService.hIncrement(key, field, delta);
        return ok(value);
    }

    /**
     * Hash 字段浮点数自增。
     *
     * curl -X POST "http://localhost:8080/redisson/data/hash/increment-double?key=user:hash:1&field=score&delta=1.5"
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param delta 增量
     * @return 最新值
     */
    @PostMapping("/hash/increment-double")
    public Map<String, Object> hIncrementDouble(@RequestParam String key,
                                                @RequestParam String field,
                                                @RequestParam(defaultValue = "1") Double delta) {
        checkKey(key);
        checkField(field);

        double value = redissonService.hIncrementDouble(key, field, delta);
        return ok(value);
    }

    /**
     * 清空 Hash。
     *
     * curl -X DELETE "http://localhost:8080/redisson/data/hash/clear?key=user:hash:1"
     *
     * @param key Redis 键
     * @return 执行结果
     */
    @DeleteMapping("/hash/clear")
    public Map<String, Object> hClear(@RequestParam String key) {
        checkKey(key);

        redissonService.hClear(key);
        log.info("Hash 清空成功，key={}", key);
        return ok(true);
    }

    // -------------------------------------------------------------------------
    // MapCache
    // -------------------------------------------------------------------------

    /**
     * 设置 MapCache 字段值。
     *
     * curl -X POST "http://localhost:8080/redisson/data/map-cache/put?key=user:cache:1&field=profile&ttlSeconds=300" \
     *   -H "Content-Type: application/json" \
     *   -d '{"name":"Ateng","age":18}'
     *
     * @param key            Redis 键
     * @param field          字段名
     * @param ttlSeconds     字段 TTL 秒数
     * @param maxIdleSeconds 最大空闲秒数，可选
     * @param value          字段值
     * @return 执行结果
     */
    @PostMapping("/map-cache/put")
    public Map<String, Object> hcPut(@RequestParam String key,
                                     @RequestParam String field,
                                     @RequestParam Long ttlSeconds,
                                     @RequestParam(required = false) Long maxIdleSeconds,
                                     @RequestBody Object value) {
        checkKey(key);
        checkField(field);
        Assert.isTrue(ObjectUtil.isNotNull(ttlSeconds) && ttlSeconds > 0, "ttlSeconds必须大于0");

        if (ObjectUtil.isNotNull(maxIdleSeconds) && maxIdleSeconds > 0) {
            redissonService.hcPut(key, field, value, Duration.ofSeconds(ttlSeconds), Duration.ofSeconds(maxIdleSeconds));
        } else {
            redissonService.hcPut(key, field, value, Duration.ofSeconds(ttlSeconds));
        }

        log.info("MapCache 字段写入成功，key={}，field={}，ttlSeconds={}，maxIdleSeconds={}",
                key, field, ttlSeconds, maxIdleSeconds);
        return ok(true);
    }

    /**
     * 获取 MapCache 字段值。
     *
     * curl "http://localhost:8080/redisson/data/map-cache/get?key=user:cache:1&field=profile"
     *
     * @param key   Redis 键
     * @param field 字段名
     * @return 字段值
     */
    @GetMapping("/map-cache/get")
    public Map<String, Object> hcGet(@RequestParam String key,
                                     @RequestParam String field) {
        checkKey(key);
        checkField(field);

        Object value = redissonService.hcGet(key, field, Object.class);
        return ok(value);
    }

    /**
     * 删除 MapCache 字段。
     *
     * curl -X DELETE "http://localhost:8080/redisson/data/map-cache/delete?key=user:cache:1&fields=profile"
     *
     * @param key    Redis 键
     * @param fields 字段集合
     * @return 删除数量
     */
    @DeleteMapping("/map-cache/delete")
    public Map<String, Object> hcDelete(@RequestParam String key,
                                        @RequestParam List<String> fields) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(fields), "fields不能为空");

        long count = redissonService.hcDelete(key, fields.toArray(new String[0]));
        return ok(count);
    }

    // -------------------------------------------------------------------------
    // List / Deque
    // -------------------------------------------------------------------------

    /**
     * 从左侧压入列表。
     *
     * curl -X POST "http://localhost:8080/redisson/data/list/left-push?key=list:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"A"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 执行结果
     */
    @PostMapping("/list/left-push")
    public Map<String, Object> lLeftPush(@RequestParam String key,
                                         @RequestBody Object value) {
        checkKey(key);

        redissonService.lLeftPush(key, value);
        return ok(true);
    }

    /**
     * 从右侧压入列表。
     *
     * curl -X POST "http://localhost:8080/redisson/data/list/right-push?key=list:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"B"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 执行结果
     */
    @PostMapping("/list/right-push")
    public Map<String, Object> lRightPush(@RequestParam String key,
                                          @RequestBody Object value) {
        checkKey(key);

        redissonService.lRightPush(key, value);
        return ok(true);
    }

    /**
     * 批量从右侧压入列表。
     *
     * curl -X POST "http://localhost:8080/redisson/data/list/right-push-all?key=list:test" \
     *   -H "Content-Type: application/json" \
     *   -d '["A","B","C"]'
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 执行结果
     */
    @PostMapping("/list/right-push-all")
    public Map<String, Object> lRightPushAll(@RequestParam String key,
                                             @RequestBody List<Object> values) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(values), "values不能为空");

        redissonService.lRightPushAll(key, values);
        return ok(true);
    }

    /**
     * 从左侧弹出列表元素。
     *
     * curl -X POST "http://localhost:8080/redisson/data/list/left-pop?key=list:test"
     *
     * @param key Redis 键
     * @return 弹出的元素
     */
    @PostMapping("/list/left-pop")
    public Map<String, Object> lLeftPop(@RequestParam String key) {
        checkKey(key);

        Object value = redissonService.lLeftPop(key);
        return ok(value);
    }

    /**
     * 从右侧弹出列表元素。
     *
     * curl -X POST "http://localhost:8080/redisson/data/list/right-pop?key=list:test"
     *
     * @param key Redis 键
     * @return 弹出的元素
     */
    @PostMapping("/list/right-pop")
    public Map<String, Object> lRightPop(@RequestParam String key) {
        checkKey(key);

        Object value = redissonService.lRightPop(key);
        return ok(value);
    }

    /**
     * 获取列表范围。
     *
     * curl "http://localhost:8080/redisson/data/list/range?key=list:test&start=0&end=-1"
     *
     * @param key   Redis 键
     * @param start 开始索引
     * @param end   结束索引
     * @return 元素列表
     */
    @GetMapping("/list/range")
    public Map<String, Object> lRange(@RequestParam String key,
                                      @RequestParam(defaultValue = "0") Long start,
                                      @RequestParam(defaultValue = "-1") Long end) {
        checkKey(key);

        List<Object> data = redissonService.lRange(key, start, end);
        return ok(data);
    }

    /**
     * 获取列表长度。
     *
     * curl "http://localhost:8080/redisson/data/list/size?key=list:test"
     *
     * @param key Redis 键
     * @return 长度
     */
    @GetMapping("/list/size")
    public Map<String, Object> lSize(@RequestParam String key) {
        checkKey(key);

        long size = redissonService.lSize(key);
        return ok(size);
    }

    /**
     * 删除列表元素。
     *
     * curl -X POST "http://localhost:8080/redisson/data/list/remove?key=list:test&count=0" \
     *   -H "Content-Type: application/json" \
     *   -d '"A"'
     *
     * @param key   Redis 键
     * @param count 删除数量规则
     * @param value 元素
     * @return 删除数量
     */
    @PostMapping("/list/remove")
    public Map<String, Object> lRemove(@RequestParam String key,
                                       @RequestParam(defaultValue = "0") Long count,
                                       @RequestBody Object value) {
        checkKey(key);

        long removed = redissonService.lRemove(key, count, value);
        return ok(removed);
    }

    /**
     * 获取列表指定索引元素。
     *
     * curl "http://localhost:8080/redisson/data/list/index?key=list:test&index=0"
     *
     * @param key   Redis 键
     * @param index 索引
     * @return 元素
     */
    @GetMapping("/list/index")
    public Map<String, Object> lIndex(@RequestParam String key,
                                      @RequestParam Long index) {
        checkKey(key);
        Assert.notNull(index, "index不能为空");

        Object value = redissonService.lIndex(key, index);
        return ok(value);
    }

    /**
     * 设置列表指定索引元素。
     *
     * curl -X PUT "http://localhost:8080/redisson/data/list/set?key=list:test&index=0" \
     *   -H "Content-Type: application/json" \
     *   -d '"NEW"'
     *
     * @param key   Redis 键
     * @param index 索引
     * @param value 元素
     * @return 执行结果
     */
    @PutMapping("/list/set")
    public Map<String, Object> lSet(@RequestParam String key,
                                    @RequestParam Long index,
                                    @RequestBody Object value) {
        checkKey(key);
        Assert.notNull(index, "index不能为空");

        redissonService.lSet(key, index, value);
        return ok(true);
    }

    /**
     * 裁剪列表范围。
     *
     * curl -X PUT "http://localhost:8080/redisson/data/list/trim?key=list:test&start=0&end=9"
     *
     * @param key   Redis 键
     * @param start 开始索引
     * @param end   结束索引
     * @return 执行结果
     */
    @PutMapping("/list/trim")
    public Map<String, Object> lTrim(@RequestParam String key,
                                     @RequestParam Integer start,
                                     @RequestParam Integer end) {
        checkKey(key);
        Assert.notNull(start, "start不能为空");
        Assert.notNull(end, "end不能为空");

        redissonService.lTrim(key, start, end);
        return ok(true);
    }

    /**
     * 清空列表。
     *
     * curl -X DELETE "http://localhost:8080/redisson/data/list/clear?key=list:test"
     *
     * @param key Redis 键
     * @return 执行结果
     */
    @DeleteMapping("/list/clear")
    public Map<String, Object> lClear(@RequestParam String key) {
        checkKey(key);

        redissonService.lClear(key);
        return ok(true);
    }

    // -------------------------------------------------------------------------
    // Set / SetCache
    // -------------------------------------------------------------------------

    /**
     * 添加集合元素。
     *
     * curl -X POST "http://localhost:8080/redisson/data/set/add?key=set:test" \
     *   -H "Content-Type: application/json" \
     *   -d '["A","B","C"]'
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 是否有新增
     */
    @PostMapping("/set/add")
    public Map<String, Object> sAdd(@RequestParam String key,
                                    @RequestBody List<Object> values) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(values), "values不能为空");

        boolean success = redissonService.sAdd(key, values);
        return ok(success);
    }

    /**
     * 添加 SetCache 元素。
     *
     * curl -X POST "http://localhost:8080/redisson/data/set-cache/add?key=set:cache:test&ttlSeconds=60" \
     *   -H "Content-Type: application/json" \
     *   -d '"A"'
     *
     * @param key        Redis 键
     * @param ttlSeconds 元素 TTL 秒数
     * @param value      元素
     * @return 是否添加成功
     */
    @PostMapping("/set-cache/add")
    public Map<String, Object> scAdd(@RequestParam String key,
                                     @RequestParam Long ttlSeconds,
                                     @RequestBody Object value) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(ttlSeconds) && ttlSeconds > 0, "ttlSeconds必须大于0");

        boolean success = redissonService.scAdd(key, value, Duration.ofSeconds(ttlSeconds));
        return ok(success);
    }

    /**
     * 判断集合是否包含元素。
     *
     * curl -X POST "http://localhost:8080/redisson/data/set/is-member?key=set:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"A"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否包含
     */
    @PostMapping("/set/is-member")
    public Map<String, Object> sIsMember(@RequestParam String key,
                                         @RequestBody Object value) {
        checkKey(key);

        boolean exists = redissonService.sIsMember(key, value);
        return ok(exists);
    }

    /**
     * 获取集合全部元素。
     *
     * curl "http://localhost:8080/redisson/data/set/members?key=set:test"
     *
     * @param key Redis 键
     * @return 元素集合
     */
    @GetMapping("/set/members")
    public Map<String, Object> sMembers(@RequestParam String key) {
        checkKey(key);

        Set<Object> data = redissonService.sMembers(key);
        return ok(data);
    }

    /**
     * 获取集合大小。
     *
     * curl "http://localhost:8080/redisson/data/set/size?key=set:test"
     *
     * @param key Redis 键
     * @return 集合大小
     */
    @GetMapping("/set/size")
    public Map<String, Object> sSize(@RequestParam String key) {
        checkKey(key);

        long size = redissonService.sSize(key);
        return ok(size);
    }

    /**
     * 随机弹出集合元素。
     *
     * curl -X POST "http://localhost:8080/redisson/data/set/pop?key=set:test"
     *
     * @param key Redis 键
     * @return 元素
     */
    @PostMapping("/set/pop")
    public Map<String, Object> sPop(@RequestParam String key) {
        checkKey(key);

        Object value = redissonService.sPop(key);
        return ok(value);
    }

    /**
     * 删除集合元素。
     *
     * curl -X POST "http://localhost:8080/redisson/data/set/remove?key=set:test" \
     *   -H "Content-Type: application/json" \
     *   -d '["A","B"]'
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 是否删除成功
     */
    @PostMapping("/set/remove")
    public Map<String, Object> sRemove(@RequestParam String key,
                                       @RequestBody List<Object> values) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(values), "values不能为空");

        boolean success = redissonService.sRemove(key, values.toArray());
        return ok(success);
    }

    /**
     * 随机获取集合元素但不删除。
     *
     * curl "http://localhost:8080/redisson/data/set/random-member?key=set:test"
     *
     * @param key Redis 键
     * @return 元素
     */
    @GetMapping("/set/random-member")
    public Map<String, Object> sRandomMember(@RequestParam String key) {
        checkKey(key);

        Object value = redissonService.sRandomMember(key);
        return ok(value);
    }

    /**
     * 随机获取多个集合元素。
     *
     * curl "http://localhost:8080/redisson/data/set/random-members?key=set:test&count=2"
     *
     * @param key   Redis 键
     * @param count 数量
     * @return 元素集合
     */
    @GetMapping("/set/random-members")
    public Map<String, Object> sRandomMembers(@RequestParam String key,
                                              @RequestParam Integer count) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(count) && count > 0, "count必须大于0");

        Set<Object> data = redissonService.sRandomMembers(key, count);
        return ok(data);
    }

    /**
     * 获取两个集合的并集。
     *
     * curl "http://localhost:8080/redisson/data/set/union?key1=set:a&key2=set:b"
     *
     * @param key1 第一个 Key
     * @param key2 第二个 Key
     * @return 并集
     */
    @GetMapping("/set/union")
    public Map<String, Object> sUnion(@RequestParam String key1,
                                      @RequestParam String key2) {
        checkKey(key1);
        checkKey(key2);

        Set<Object> data = redissonService.sUnion(key1, key2);
        return ok(data);
    }

    /**
     * 获取两个集合的交集。
     *
     * curl "http://localhost:8080/redisson/data/set/intersect?key1=set:a&key2=set:b"
     *
     * @param key1 第一个 Key
     * @param key2 第二个 Key
     * @return 交集
     */
    @GetMapping("/set/intersect")
    public Map<String, Object> sIntersect(@RequestParam String key1,
                                          @RequestParam String key2) {
        checkKey(key1);
        checkKey(key2);

        Set<Object> data = redissonService.sIntersect(key1, key2);
        return ok(data);
    }

    /**
     * 获取两个集合的差集。
     *
     * curl "http://localhost:8080/redisson/data/set/difference?key1=set:a&key2=set:b"
     *
     * @param key1 第一个 Key
     * @param key2 第二个 Key
     * @return 差集
     */
    @GetMapping("/set/difference")
    public Map<String, Object> sDifference(@RequestParam String key1,
                                           @RequestParam String key2) {
        checkKey(key1);
        checkKey(key2);

        Set<Object> data = redissonService.sDifference(key1, key2);
        return ok(data);
    }

    /**
     * 并集存储。
     *
     * curl -X POST "http://localhost:8080/redisson/data/set/union-store?destKey=set:dest&keys=set:a&keys=set:b"
     *
     * @param destKey 目标 Key
     * @param keys    源 Key 集合
     * @return 存储数量
     */
    @PostMapping("/set/union-store")
    public Map<String, Object> sUnionStore(@RequestParam String destKey,
                                           @RequestParam List<String> keys) {
        checkKey(destKey);
        Assert.isTrue(CollUtil.isNotEmpty(keys), "keys不能为空");

        long count = redissonService.sUnionStore(destKey, keys.toArray(new String[0]));
        return ok(count);
    }

    /**
     * 交集存储。
     *
     * curl -X POST "http://localhost:8080/redisson/data/set/intersect-store?destKey=set:dest&keys=set:a&keys=set:b"
     *
     * @param destKey 目标 Key
     * @param keys    源 Key 集合
     * @return 存储数量
     */
    @PostMapping("/set/intersect-store")
    public Map<String, Object> sIntersectStore(@RequestParam String destKey,
                                               @RequestParam List<String> keys) {
        checkKey(destKey);
        Assert.isTrue(CollUtil.isNotEmpty(keys), "keys不能为空");

        long count = redissonService.sIntersectStore(destKey, keys.toArray(new String[0]));
        return ok(count);
    }

    /**
     * 差集存储。
     *
     * curl -X POST "http://localhost:8080/redisson/data/set/difference-store?destKey=set:dest&keys=set:a&keys=set:b"
     *
     * @param destKey 目标 Key
     * @param keys    源 Key 集合
     * @return 存储数量
     */
    @PostMapping("/set/difference-store")
    public Map<String, Object> sDifferenceStore(@RequestParam String destKey,
                                                @RequestParam List<String> keys) {
        checkKey(destKey);
        Assert.isTrue(CollUtil.isNotEmpty(keys), "keys不能为空");

        long count = redissonService.sDifferenceStore(destKey, keys.toArray(new String[0]));
        return ok(count);
    }

    // -------------------------------------------------------------------------
    // ZSet / 排行榜
    // -------------------------------------------------------------------------

    /**
     * 添加 ZSet 元素。
     *
     * curl -X POST "http://localhost:8080/redisson/data/zset/add?key=rank:score" \
     *   -H "Content-Type: application/json" \
     *   -d '{"value":"user:1","score":100}'
     *
     * @param key     Redis 键
     * @param request 请求体
     * @return 是否新增
     */
    @PostMapping("/zset/add")
    public Map<String, Object> zAdd(@RequestParam String key,
                                    @RequestBody ZSetValueScoreRequest request) {
        checkKey(key);
        Assert.notNull(request, "请求体不能为空");
        Assert.notNull(request.getValue(), "value不能为空");
        Assert.notNull(request.getScore(), "score不能为空");

        boolean success = redissonService.zAdd(key, request.getValue(), request.getScore());
        return ok(success);
    }

    /**
     * 批量添加 ZSet 元素。
     *
     * curl -X POST "http://localhost:8080/redisson/data/zset/add-all?key=rank:score" \
     *   -H "Content-Type: application/json" \
     *   -d '{"scoreMap":{"user:1":100,"user:2":90}}'
     *
     * @param key     Redis 键
     * @param request 请求体
     * @return 新增数量
     */
    @PostMapping("/zset/add-all")
    public Map<String, Object> zAddAll(@RequestParam String key,
                                       @RequestBody ZSetAddAllRequest request) {
        checkKey(key);
        Assert.notNull(request, "请求体不能为空");
        Assert.isTrue(CollUtil.isNotEmpty(request.getScoreMap()), "scoreMap不能为空");

        int count = redissonService.zAddAll(key, request.getScoreMap());
        return ok(count);
    }

    /**
     * 删除 ZSet 元素。
     *
     * curl -X POST "http://localhost:8080/redisson/data/zset/remove?key=rank:score" \
     *   -H "Content-Type: application/json" \
     *   -d '["user:1","user:2"]'
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 是否删除成功
     */
    @PostMapping("/zset/remove")
    public Map<String, Object> zRemove(@RequestParam String key,
                                       @RequestBody List<Object> values) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(values), "values不能为空");

        boolean success = redissonService.zRemove(key, values.toArray());
        return ok(success);
    }

    /**
     * 获取 ZSet 元素分数。
     *
     * curl -X POST "http://localhost:8080/redisson/data/zset/score?key=rank:score" \
     *   -H "Content-Type: application/json" \
     *   -d '"user:1"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 分数
     */
    @PostMapping("/zset/score")
    public Map<String, Object> zScore(@RequestParam String key,
                                      @RequestBody Object value) {
        checkKey(key);

        Double score = redissonService.zScore(key, value);
        return ok(score);
    }

    /**
     * 获取 ZSet 升序排名。
     *
     * curl -X POST "http://localhost:8080/redisson/data/zset/rank?key=rank:score" \
     *   -H "Content-Type: application/json" \
     *   -d '"user:1"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 排名
     */
    @PostMapping("/zset/rank")
    public Map<String, Object> zRank(@RequestParam String key,
                                     @RequestBody Object value) {
        checkKey(key);

        Integer rank = redissonService.zRank(key, value);
        return ok(rank);
    }

    /**
     * 获取 ZSet 降序排名。
     *
     * curl -X POST "http://localhost:8080/redisson/data/zset/rev-rank?key=rank:score" \
     *   -H "Content-Type: application/json" \
     *   -d '"user:1"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 排名
     */
    @PostMapping("/zset/rev-rank")
    public Map<String, Object> zRevRank(@RequestParam String key,
                                        @RequestBody Object value) {
        checkKey(key);

        Integer rank = redissonService.zRevRank(key, value);
        return ok(rank);
    }

    /**
     * 获取 ZSet 分数区间元素。
     *
     * curl "http://localhost:8080/redisson/data/zset/range-by-score?key=rank:score&min=0&max=100"
     *
     * @param key Redis 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 元素集合
     */
    @GetMapping("/zset/range-by-score")
    public Map<String, Object> zRangeByScore(@RequestParam String key,
                                             @RequestParam Double min,
                                             @RequestParam Double max) {
        checkKey(key);
        Assert.notNull(min, "min不能为空");
        Assert.notNull(max, "max不能为空");

        Set<Object> data = redissonService.zRangeByScore(key, min, max);
        return ok(data);
    }

    /**
     * 获取 ZSet 分数区间元素并分页。
     *
     * curl "http://localhost:8080/redisson/data/zset/range-by-score-page?key=rank:score&min=0&max=100&offset=0&count=10"
     *
     * @param key    Redis 键
     * @param min    最小分数
     * @param max    最大分数
     * @param offset 偏移量
     * @param count  数量
     * @return 元素集合
     */
    @GetMapping("/zset/range-by-score-page")
    public Map<String, Object> zRangeByScorePage(@RequestParam String key,
                                                 @RequestParam Double min,
                                                 @RequestParam Double max,
                                                 @RequestParam(defaultValue = "0") Integer offset,
                                                 @RequestParam(defaultValue = "10") Integer count) {
        checkKey(key);

        Collection<Object> data = redissonService.zRangeByScore(key, min, max, offset, count);
        return ok(data);
    }

    /**
     * 获取 ZSet 分数区间元素及分数。
     *
     * curl "http://localhost:8080/redisson/data/zset/range-by-score-with-scores?key=rank:score&min=0&max=100"
     *
     * @param key Redis 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 元素分数 Map
     */
    @GetMapping("/zset/range-by-score-with-scores")
    public Map<String, Object> zRangeByScoreWithScores(@RequestParam String key,
                                                       @RequestParam Double min,
                                                       @RequestParam Double max) {
        checkKey(key);

        Map<Object, Double> data = redissonService.zRangeByScoreWithScores(key, min, max);
        return ok(data);
    }

    /**
     * 获取 ZSet 降序分数区间元素及分数。
     *
     * curl "http://localhost:8080/redisson/data/zset/rev-range-by-score-with-scores?key=rank:score&min=0&max=100"
     *
     * @param key Redis 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 元素分数 Map
     */
    @GetMapping("/zset/rev-range-by-score-with-scores")
    public Map<String, Object> zRevRangeByScoreWithScores(@RequestParam String key,
                                                          @RequestParam Double min,
                                                          @RequestParam Double max) {
        checkKey(key);

        Map<Object, Double> data = redissonService.zRevRangeByScoreWithScores(key, min, max);
        return ok(data);
    }

    /**
     * 获取 ZSet 排名区间元素。
     *
     * curl "http://localhost:8080/redisson/data/zset/range?key=rank:score&start=0&end=9"
     *
     * @param key   Redis 键
     * @param start 开始排名
     * @param end   结束排名
     * @return 元素集合
     */
    @GetMapping("/zset/range")
    public Map<String, Object> zRange(@RequestParam String key,
                                      @RequestParam(defaultValue = "0") Integer start,
                                      @RequestParam(defaultValue = "9") Integer end) {
        checkKey(key);

        Set<Object> data = redissonService.zRange(key, start, end);
        return ok(data);
    }

    /**
     * 获取 ZSet 排名区间元素及分数。
     *
     * curl "http://localhost:8080/redisson/data/zset/range-with-scores?key=rank:score&start=0&end=9"
     *
     * @param key   Redis 键
     * @param start 开始排名
     * @param end   结束排名
     * @return 元素分数 Map
     */
    @GetMapping("/zset/range-with-scores")
    public Map<String, Object> zRangeWithScores(@RequestParam String key,
                                                @RequestParam(defaultValue = "0") Integer start,
                                                @RequestParam(defaultValue = "9") Integer end) {
        checkKey(key);

        Map<Object, Double> data = redissonService.zRangeWithScores(key, start, end);
        return ok(data);
    }

    /**
     * 获取 ZSet 降序排名区间元素。
     *
     * curl "http://localhost:8080/redisson/data/zset/rev-range?key=rank:score&start=0&end=9"
     *
     * @param key   Redis 键
     * @param start 开始排名
     * @param end   结束排名
     * @return 元素集合
     */
    @GetMapping("/zset/rev-range")
    public Map<String, Object> zRevRange(@RequestParam String key,
                                         @RequestParam(defaultValue = "0") Integer start,
                                         @RequestParam(defaultValue = "9") Integer end) {
        checkKey(key);

        Set<Object> data = redissonService.zRevRange(key, start, end);
        return ok(data);
    }

    /**
     * 获取 ZSet 降序排名区间元素及分数。
     *
     * curl "http://localhost:8080/redisson/data/zset/rev-range-with-scores?key=rank:score&start=0&end=9"
     *
     * @param key   Redis 键
     * @param start 开始排名
     * @param end   结束排名
     * @return 元素分数 Map
     */
    @GetMapping("/zset/rev-range-with-scores")
    public Map<String, Object> zRevRangeWithScores(@RequestParam String key,
                                                   @RequestParam(defaultValue = "0") Integer start,
                                                   @RequestParam(defaultValue = "9") Integer end) {
        checkKey(key);

        Map<Object, Double> data = redissonService.zRevRangeWithScores(key, start, end);
        return ok(data);
    }

    /**
     * ZSet 元素分数自增。
     *
     * curl -X POST "http://localhost:8080/redisson/data/zset/increment-score?key=rank:score" \
     *   -H "Content-Type: application/json" \
     *   -d '{"value":"user:1","score":10}'
     *
     * @param key     Redis 键
     * @param request 请求体
     * @return 最新分数
     */
    @PostMapping("/zset/increment-score")
    public Map<String, Object> zIncrBy(@RequestParam String key,
                                       @RequestBody ZSetValueScoreRequest request) {
        checkKey(key);
        Assert.notNull(request, "请求体不能为空");
        Assert.notNull(request.getValue(), "value不能为空");
        Assert.notNull(request.getScore(), "score不能为空");

        Double score = redissonService.zIncrBy(key, request.getValue(), request.getScore());
        return ok(score);
    }

    /**
     * 获取 ZSet 元素数量。
     *
     * curl "http://localhost:8080/redisson/data/zset/card?key=rank:score"
     *
     * @param key Redis 键
     * @return 数量
     */
    @GetMapping("/zset/card")
    public Map<String, Object> zCard(@RequestParam String key) {
        checkKey(key);

        int count = redissonService.zCard(key);
        return ok(count);
    }

    /**
     * 获取 ZSet 指定分数区间元素数量。
     *
     * curl "http://localhost:8080/redisson/data/zset/count?key=rank:score&min=0&max=100"
     *
     * @param key Redis 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 数量
     */
    @GetMapping("/zset/count")
    public Map<String, Object> zCount(@RequestParam String key,
                                      @RequestParam Double min,
                                      @RequestParam Double max) {
        checkKey(key);

        long count = redissonService.zCount(key, min, max);
        return ok(count);
    }

    /**
     * 删除 ZSet 指定分数区间元素。
     *
     * curl -X DELETE "http://localhost:8080/redisson/data/zset/remove-range-by-score?key=rank:score&min=0&max=10"
     *
     * @param key Redis 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 删除数量
     */
    @DeleteMapping("/zset/remove-range-by-score")
    public Map<String, Object> zRemoveRangeByScore(@RequestParam String key,
                                                   @RequestParam Double min,
                                                   @RequestParam Double max) {
        checkKey(key);

        long count = redissonService.zRemoveRangeByScore(key, min, max);
        return ok(count);
    }

    /**
     * 删除 ZSet 指定排名区间元素。
     *
     * curl -X DELETE "http://localhost:8080/redisson/data/zset/remove-range-by-rank?key=rank:score&start=0&end=9"
     *
     * @param key   Redis 键
     * @param start 开始排名
     * @param end   结束排名
     * @return 删除数量
     */
    @DeleteMapping("/zset/remove-range-by-rank")
    public Map<String, Object> zRemoveRangeByRank(@RequestParam String key,
                                                  @RequestParam Integer start,
                                                  @RequestParam Integer end) {
        checkKey(key);

        long count = redissonService.zRemoveRangeByRank(key, start, end);
        return ok(count);
    }

    /**
     * 弹出 ZSet 分数最小元素。
     *
     * curl -X POST "http://localhost:8080/redisson/data/zset/pop-first?key=rank:score"
     *
     * @param key Redis 键
     * @return 元素和分数
     */
    @PostMapping("/zset/pop-first")
    public Map<String, Object> zPopFirst(@RequestParam String key) {
        checkKey(key);

        ScoredEntry<Object> entry = redissonService.zPopFirst(key);
        return ok(scoredEntryToMap(entry));
    }

    /**
     * 弹出 ZSet 分数最大元素。
     *
     * curl -X POST "http://localhost:8080/redisson/data/zset/pop-last?key=rank:score"
     *
     * @param key Redis 键
     * @return 元素和分数
     */
    @PostMapping("/zset/pop-last")
    public Map<String, Object> zPopLast(@RequestParam String key) {
        checkKey(key);

        ScoredEntry<Object> entry = redissonService.zPopLast(key);
        return ok(scoredEntryToMap(entry));
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
     * 校验字段名。
     *
     * @param field 字段名
     */
    private void checkField(String field) {
        Assert.isTrue(StrUtil.isNotBlank(field), "字段名不能为空");
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
     * 转换 ScoredEntry。
     *
     * @param entry ScoredEntry
     * @return Map
     */
    private Map<String, Object> scoredEntryToMap(ScoredEntry<Object> entry) {
        if (ObjectUtil.isNull(entry)) {
            return null;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("value", entry.getValue());
        result.put("score", entry.getScore());
        return result;
    }

    /**
     * ZSet 元素分数请求体。
     *
     * @author Ateng
     * @since 2026-04-26
     */
    @Data
    public static class ZSetValueScoreRequest {

        /**
         * 元素值。
         */
        private Object value;

        /**
         * 分数。
         */
        private Double score;

    }

    /**
     * ZSet 批量添加请求体。
     *
     * @author Ateng
     * @since 2026-04-26
     */
    @Data
    public static class ZSetAddAllRequest {

        /**
         * 元素分数 Map。
         */
        private Map<Object, Double> scoreMap;

    }

}