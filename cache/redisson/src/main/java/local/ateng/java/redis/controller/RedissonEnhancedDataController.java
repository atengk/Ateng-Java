package local.ateng.java.redis.controller;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import local.ateng.java.redis.service.RedissonService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.LocalCachedMapOptions;
import org.redisson.codec.JsonCodec;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 增强数据结构测试控制器
 * 用于演示 RedissonService 的 LocalCachedMap、JsonBucket、BinaryStream、Multimap、SortedSet、LexSortedSet、Adder、有界队列和优先级队列能力。
 *
 * @author Ateng
 * @since 2026-04-27
 */
@Slf4j
@RestController
@RequestMapping("/redisson/enhanced")
@RequiredArgsConstructor
public class RedissonEnhancedDataController {

    private final RedissonService redissonService;

    /**
     * JSON 编解码器。
     * 建议在配置类中注入 JsonCodec Bean，例如 JsonJacksonCodec。
     */
    private final JsonCodec jsonCodec;

    // -------------------------------------------------------------------------
    // LocalCachedMap
    // -------------------------------------------------------------------------

    /**
     * 设置 LocalCachedMap 字段值。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/local-cache/put?key=lc:user&field=user:1" \
     *   -H "Content-Type: application/json" \
     *   -d '{"id":1,"name":"Ateng"}'
     *
     * @param key   Redis 键
     * @param field 字段
     * @param value 字段值
     * @return 执行结果
     */
    @PostMapping("/local-cache/put")
    public Map<String, Object> lcPut(@RequestParam String key,
                                     @RequestParam String field,
                                     @RequestBody Object value) {
        checkKey(key);
        checkKey(field);

        redissonService.lcPut(key, field, value, localCachedMapOptions());
        log.info("LocalCachedMap 写入成功，key={}，field={}", key, field);
        return ok(true);
    }

    /**
     * 字段不存在时设置 LocalCachedMap 字段值。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/local-cache/put-if-absent?key=lc:user&field=user:1" \
     *   -H "Content-Type: application/json" \
     *   -d '{"id":1,"name":"Ateng"}'
     *
     * @param key   Redis 键
     * @param field 字段
     * @param value 字段值
     * @return 是否设置成功
     */
    @PostMapping("/local-cache/put-if-absent")
    public Map<String, Object> lcPutIfAbsent(@RequestParam String key,
                                             @RequestParam String field,
                                             @RequestBody Object value) {
        checkKey(key);
        checkKey(field);

        boolean success = redissonService.lcPutIfAbsent(key, field, value, localCachedMapOptions());
        return ok(success);
    }

    /**
     * 获取 LocalCachedMap 字段值。
     *
     * curl "http://localhost:8080/redisson/enhanced/local-cache/get?key=lc:user&field=user:1"
     *
     * @param key   Redis 键
     * @param field 字段
     * @return 字段值
     */
    @GetMapping("/local-cache/get")
    public Map<String, Object> lcGet(@RequestParam String key,
                                     @RequestParam String field) {
        checkKey(key);
        checkKey(field);

        Object value = redissonService.lcGet(key, field, Object.class, localCachedMapOptions());
        return ok(value);
    }

    /**
     * 删除 LocalCachedMap 字段。
     *
     * curl -X DELETE "http://localhost:8080/redisson/enhanced/local-cache/remove?key=lc:user&field=user:1"
     *
     * @param key   Redis 键
     * @param field 字段
     * @return 删除前的值
     */
    @DeleteMapping("/local-cache/remove")
    public Map<String, Object> lcRemove(@RequestParam String key,
                                        @RequestParam String field) {
        checkKey(key);
        checkKey(field);

        Object oldValue = redissonService.lcRemove(key, field, localCachedMapOptions());
        return ok(oldValue);
    }

    /**
     * 判断 LocalCachedMap 字段是否存在。
     *
     * curl "http://localhost:8080/redisson/enhanced/local-cache/contains?key=lc:user&field=user:1"
     *
     * @param key   Redis 键
     * @param field 字段
     * @return 是否存在
     */
    @GetMapping("/local-cache/contains")
    public Map<String, Object> lcContainsKey(@RequestParam String key,
                                             @RequestParam String field) {
        checkKey(key);
        checkKey(field);

        boolean exists = redissonService.lcContainsKey(key, field, localCachedMapOptions());
        return ok(exists);
    }

    /**
     * 获取 LocalCachedMap 大小。
     *
     * curl "http://localhost:8080/redisson/enhanced/local-cache/size?key=lc:user"
     *
     * @param key Redis 键
     * @return 大小
     */
    @GetMapping("/local-cache/size")
    public Map<String, Object> lcSize(@RequestParam String key) {
        checkKey(key);

        int size = redissonService.lcSize(key, localCachedMapOptions());
        return ok(size);
    }

    /**
     * 清空 LocalCachedMap 远端和本地数据。
     *
     * curl -X DELETE "http://localhost:8080/redisson/enhanced/local-cache/clear?key=lc:user"
     *
     * @param key Redis 键
     * @return 执行结果
     */
    @DeleteMapping("/local-cache/clear")
    public Map<String, Object> lcClear(@RequestParam String key) {
        checkKey(key);

        redissonService.lcClear(key, localCachedMapOptions());
        log.info("LocalCachedMap 清空成功，key={}", key);
        return ok(true);
    }

    /**
     * 仅清空当前 JVM 的 LocalCachedMap 本地缓存。
     *
     * curl -X DELETE "http://localhost:8080/redisson/enhanced/local-cache/clear-local?key=lc:user"
     *
     * @param key Redis 键
     * @return 执行结果
     */
    @DeleteMapping("/local-cache/clear-local")
    public Map<String, Object> lcClearLocalCache(@RequestParam String key) {
        checkKey(key);

        redissonService.lcClearLocalCache(key, localCachedMapOptions());
        log.info("LocalCachedMap 本地缓存清空成功，key={}", key);
        return ok(true);
    }

    // -------------------------------------------------------------------------
    // JsonBucket / RedisJSON
    // -------------------------------------------------------------------------

    /**
     * 设置完整 JSON 文档。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/json/set?key=json:user:1&ttlSeconds=300" \
     *   -H "Content-Type: application/json" \
     *   -d '{"id":1,"name":"Ateng","tags":["java","redis"]}'
     *
     * @param key        Redis 键
     * @param ttlSeconds 过期秒数，可选
     * @param value      JSON 对象
     * @return 执行结果
     */
    @PostMapping("/json/set")
    public Map<String, Object> jsonSet(@RequestParam String key,
                                       @RequestParam(required = false) Long ttlSeconds,
                                       @RequestBody Object value) {
        checkKey(key);

        if (ObjectUtil.isNotNull(ttlSeconds) && ttlSeconds > 0) {
            redissonService.jsonSet(key, value, Duration.ofSeconds(ttlSeconds), jsonCodec);
        } else {
            redissonService.jsonSet(key, value, jsonCodec);
        }

        log.info("JSON 文档写入成功，key={}，ttlSeconds={}", key, ttlSeconds);
        return ok(true);
    }

    /**
     * 获取完整 JSON 文档。
     *
     * curl "http://localhost:8080/redisson/enhanced/json/get?key=json:user:1"
     *
     * @param key Redis 键
     * @return JSON 对象
     */
    @GetMapping("/json/get")
    public Map<String, Object> jsonGet(@RequestParam String key) {
        checkKey(key);

        Object value = redissonService.jsonGet(key, jsonCodec);
        return ok(value);
    }

    /**
     * 根据 JSONPath 获取局部内容。
     *
     * curl "http://localhost:8080/redisson/enhanced/json/path-get?key=json:user:1&path=$.name"
     *
     * @param key  Redis 键
     * @param path JSONPath
     * @return JSONPath 对应值
     */
    @GetMapping("/json/path-get")
    public Map<String, Object> jsonPathGet(@RequestParam String key,
                                           @RequestParam String path) {
        checkKey(key);
        checkKey(path);

        Object value = redissonService.jsonGet(key, path, jsonCodec);
        return ok(value);
    }

    /**
     * 根据 JSONPath 设置局部内容。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/json/path-set?key=json:user:1&path=$.name" \
     *   -H "Content-Type: application/json" \
     *   -d '"Ateng Updated"'
     *
     * @param key   Redis 键
     * @param path  JSONPath
     * @param value 值
     * @return 执行结果
     */
    @PostMapping("/json/path-set")
    public Map<String, Object> jsonPathSet(@RequestParam String key,
                                           @RequestParam String path,
                                           @RequestBody Object value) {
        checkKey(key);
        checkKey(path);

        redissonService.jsonSet(key, path, value, jsonCodec);
        return ok(true);
    }

    /**
     * JSONPath 不存在时设置局部内容。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/json/path-set-if-absent?key=json:user:1&path=$.email" \
     *   -H "Content-Type: application/json" \
     *   -d '"ateng@example.com"'
     *
     * @param key   Redis 键
     * @param path  JSONPath
     * @param value 值
     * @return 是否设置成功
     */
    @PostMapping("/json/path-set-if-absent")
    public Map<String, Object> jsonSetIfAbsent(@RequestParam String key,
                                               @RequestParam String path,
                                               @RequestBody Object value) {
        checkKey(key);
        checkKey(path);

        boolean success = redissonService.jsonSetIfAbsent(key, path, value, jsonCodec);
        return ok(success);
    }

    /**
     * JSONPath 存在时设置局部内容。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/json/path-set-if-exists?key=json:user:1&path=$.name" \
     *   -H "Content-Type: application/json" \
     *   -d '"Ateng Updated"'
     *
     * @param key   Redis 键
     * @param path  JSONPath
     * @param value 值
     * @return 是否设置成功
     */
    @PostMapping("/json/path-set-if-exists")
    public Map<String, Object> jsonSetIfExists(@RequestParam String key,
                                               @RequestParam String path,
                                               @RequestBody Object value) {
        checkKey(key);
        checkKey(path);

        boolean success = redissonService.jsonSetIfExists(key, path, value, jsonCodec);
        return ok(success);
    }

    /**
     * 删除 JSONPath 对应内容。
     *
     * curl -X DELETE "http://localhost:8080/redisson/enhanced/json/path-delete?key=json:user:1&path=$.email"
     *
     * @param key  Redis 键
     * @param path JSONPath
     * @return 删除数量
     */
    @DeleteMapping("/json/path-delete")
    public Map<String, Object> jsonDelete(@RequestParam String key,
                                          @RequestParam String path) {
        checkKey(key);
        checkKey(path);

        long count = redissonService.jsonDelete(key, path, jsonCodec);
        return ok(count);
    }

    /**
     * 向 JSON 数组追加元素。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/json/array-append?key=json:user:1&path=$.tags" \
     *   -H "Content-Type: application/json" \
     *   -d '["springboot","redisson"]'
     *
     * @param key    Redis 键
     * @param path   JSONPath
     * @param values 追加元素集合
     * @return 追加后数组长度
     */
    @PostMapping("/json/array-append")
    public Map<String, Object> jsonArrayAppend(@RequestParam String key,
                                               @RequestParam String path,
                                               @RequestBody List<Object> values) {
        checkKey(key);
        checkKey(path);
        Assert.isTrue(CollUtil.isNotEmpty(values), "values不能为空");

        long length = redissonService.jsonArrayAppend(key, path, jsonCodec, values.toArray());
        return ok(length);
    }

    /**
     * 获取 JSON 对象字段名。
     *
     * curl "http://localhost:8080/redisson/enhanced/json/keys?key=json:user:1"
     *
     * @param key Redis 键
     * @return 字段名集合
     */
    @GetMapping("/json/keys")
    public Map<String, Object> jsonKeys(@RequestParam String key) {
        checkKey(key);

        List<String> keys = redissonService.jsonKeys(key, jsonCodec);
        return ok(keys);
    }

    /**
     * 清空 JSON 文档。
     *
     * curl -X DELETE "http://localhost:8080/redisson/enhanced/json/clear?key=json:user:1"
     *
     * @param key Redis 键
     * @return 执行结果
     */
    @DeleteMapping("/json/clear")
    public Map<String, Object> jsonClear(@RequestParam String key) {
        checkKey(key);

        redissonService.jsonClear(key, jsonCodec);
        return ok(true);
    }

    // -------------------------------------------------------------------------
    // BinaryStream
    // -------------------------------------------------------------------------

    /**
     * 写入文本到二进制流。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/binary/write-text?key=binary:test" \
     *   -H "Content-Type: text/plain" \
     *   -d 'hello redisson'
     *
     * @param key  Redis 键
     * @param text 文本内容
     * @return 执行结果
     */
    @PostMapping("/binary/write-text")
    public Map<String, Object> binaryWriteText(@RequestParam String key,
                                               @RequestBody String text) {
        checkKey(key);
        Assert.notNull(text, "text不能为空");

        redissonService.binaryWrite(key, text.getBytes(StandardCharsets.UTF_8));
        log.info("二进制文本写入成功，key={}，length={}", key, text.length());
        return ok(true);
    }

    /**
     * 写入 Base64 二进制数据。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/binary/write-base64?key=binary:test" \
     *   -H "Content-Type: text/plain" \
     *   -d 'aGVsbG8='
     *
     * @param key        Redis 键
     * @param base64Text Base64 文本
     * @return 执行结果
     */
    @PostMapping("/binary/write-base64")
    public Map<String, Object> binaryWriteBase64(@RequestParam String key,
                                                 @RequestBody String base64Text) {
        checkKey(key);
        Assert.isTrue(StrUtil.isNotBlank(base64Text), "base64Text不能为空");

        redissonService.binaryWrite(key, Base64.decode(base64Text));
        return ok(true);
    }

    /**
     * 读取二进制数据并按 UTF-8 文本返回。
     *
     * curl "http://localhost:8080/redisson/enhanced/binary/read-text?key=binary:test"
     *
     * @param key Redis 键
     * @return 文本内容
     */
    @GetMapping("/binary/read-text")
    public Map<String, Object> binaryReadText(@RequestParam String key) {
        checkKey(key);

        byte[] data = redissonService.binaryReadAll(key);
        return ok(new String(data, StandardCharsets.UTF_8));
    }

    /**
     * 读取二进制数据并按 Base64 返回。
     *
     * curl "http://localhost:8080/redisson/enhanced/binary/read-base64?key=binary:test"
     *
     * @param key Redis 键
     * @return Base64 文本
     */
    @GetMapping("/binary/read-base64")
    public Map<String, Object> binaryReadBase64(@RequestParam String key) {
        checkKey(key);

        byte[] data = redissonService.binaryReadAll(key);
        return ok(Base64.encode(data));
    }

    /**
     * 获取二进制数据大小。
     *
     * curl "http://localhost:8080/redisson/enhanced/binary/size?key=binary:test"
     *
     * @param key Redis 键
     * @return 字节大小
     */
    @GetMapping("/binary/size")
    public Map<String, Object> binarySize(@RequestParam String key) {
        checkKey(key);

        long size = redissonService.binarySize(key);
        return ok(size);
    }

    /**
     * 删除二进制数据。
     *
     * curl -X DELETE "http://localhost:8080/redisson/enhanced/binary/delete?key=binary:test"
     *
     * @param key Redis 键
     * @return 是否删除成功
     */
    @DeleteMapping("/binary/delete")
    public Map<String, Object> binaryDelete(@RequestParam String key) {
        checkKey(key);

        boolean success = redissonService.binaryDelete(key);
        return ok(success);
    }

    // -------------------------------------------------------------------------
    // SetMultimap / ListMultimap
    // -------------------------------------------------------------------------

    /**
     * 向 SetMultimap 添加值。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/set-multimap/put?key=smm:user-role&mapKey=user:1" \
     *   -H "Content-Type: application/json" \
     *   -d '"admin"'
     *
     * @param key      Redis 键
     * @param mapKey   Multimap 字段
     * @param mapValue Multimap 值
     * @return 是否新增
     */
    @PostMapping("/set-multimap/put")
    public Map<String, Object> smmPut(@RequestParam String key,
                                      @RequestParam String mapKey,
                                      @RequestBody Object mapValue) {
        checkKey(key);
        checkKey(mapKey);

        boolean success = redissonService.smmPut(key, mapKey, mapValue);
        return ok(success);
    }

    /**
     * 获取 SetMultimap 字段值集合。
     *
     * curl "http://localhost:8080/redisson/enhanced/set-multimap/get?key=smm:user-role&mapKey=user:1"
     *
     * @param key    Redis 键
     * @param mapKey Multimap 字段
     * @return 值集合
     */
    @GetMapping("/set-multimap/get")
    public Map<String, Object> smmGet(@RequestParam String key,
                                      @RequestParam String mapKey) {
        checkKey(key);
        checkKey(mapKey);

        Set<Object> data = redissonService.smmGet(key, mapKey);
        return ok(data);
    }

    /**
     * 删除 SetMultimap 字段的指定值。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/set-multimap/remove?key=smm:user-role&mapKey=user:1" \
     *   -H "Content-Type: application/json" \
     *   -d '"admin"'
     *
     * @param key      Redis 键
     * @param mapKey   Multimap 字段
     * @param mapValue Multimap 值
     * @return 是否删除成功
     */
    @PostMapping("/set-multimap/remove")
    public Map<String, Object> smmRemove(@RequestParam String key,
                                         @RequestParam String mapKey,
                                         @RequestBody Object mapValue) {
        checkKey(key);
        checkKey(mapKey);

        boolean success = redissonService.smmRemove(key, mapKey, mapValue);
        return ok(success);
    }

    /**
     * 删除 SetMultimap 字段的全部值。
     *
     * curl -X DELETE "http://localhost:8080/redisson/enhanced/set-multimap/remove-all?key=smm:user-role&mapKey=user:1"
     *
     * @param key    Redis 键
     * @param mapKey Multimap 字段
     * @return 删除的值集合
     */
    @DeleteMapping("/set-multimap/remove-all")
    public Map<String, Object> smmRemoveAll(@RequestParam String key,
                                            @RequestParam String mapKey) {
        checkKey(key);
        checkKey(mapKey);

        Set<Object> data = redissonService.smmRemoveAll(key, mapKey);
        return ok(data);
    }

    /**
     * 判断 SetMultimap 是否包含字段和值。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/set-multimap/contains-entry?key=smm:user-role&mapKey=user:1" \
     *   -H "Content-Type: application/json" \
     *   -d '"admin"'
     *
     * @param key      Redis 键
     * @param mapKey   Multimap 字段
     * @param mapValue Multimap 值
     * @return 是否包含
     */
    @PostMapping("/set-multimap/contains-entry")
    public Map<String, Object> smmContainsEntry(@RequestParam String key,
                                                @RequestParam String mapKey,
                                                @RequestBody Object mapValue) {
        checkKey(key);
        checkKey(mapKey);

        boolean exists = redissonService.smmContainsEntry(key, mapKey, mapValue);
        return ok(exists);
    }

    /**
     * 获取 SetMultimap 总值数量。
     *
     * curl "http://localhost:8080/redisson/enhanced/set-multimap/size?key=smm:user-role"
     *
     * @param key Redis 键
     * @return 总值数量
     */
    @GetMapping("/set-multimap/size")
    public Map<String, Object> smmSize(@RequestParam String key) {
        checkKey(key);

        int size = redissonService.smmSize(key);
        return ok(size);
    }

    /**
     * 清空 SetMultimap。
     *
     * curl -X DELETE "http://localhost:8080/redisson/enhanced/set-multimap/clear?key=smm:user-role"
     *
     * @param key Redis 键
     * @return 执行结果
     */
    @DeleteMapping("/set-multimap/clear")
    public Map<String, Object> smmClear(@RequestParam String key) {
        checkKey(key);

        redissonService.smmClear(key);
        return ok(true);
    }

    /**
     * 向 ListMultimap 添加值。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/list-multimap/put?key=lmm:user-tag&mapKey=user:1" \
     *   -H "Content-Type: application/json" \
     *   -d '"java"'
     *
     * @param key      Redis 键
     * @param mapKey   Multimap 字段
     * @param mapValue Multimap 值
     * @return 是否新增
     */
    @PostMapping("/list-multimap/put")
    public Map<String, Object> lmmPut(@RequestParam String key,
                                      @RequestParam String mapKey,
                                      @RequestBody Object mapValue) {
        checkKey(key);
        checkKey(mapKey);

        boolean success = redissonService.lmmPut(key, mapKey, mapValue);
        return ok(success);
    }

    /**
     * 获取 ListMultimap 字段值列表。
     *
     * curl "http://localhost:8080/redisson/enhanced/list-multimap/get?key=lmm:user-tag&mapKey=user:1"
     *
     * @param key    Redis 键
     * @param mapKey Multimap 字段
     * @return 值列表
     */
    @GetMapping("/list-multimap/get")
    public Map<String, Object> lmmGet(@RequestParam String key,
                                      @RequestParam String mapKey) {
        checkKey(key);
        checkKey(mapKey);

        List<Object> data = redissonService.lmmGet(key, mapKey);
        return ok(data);
    }

    /**
     * 删除 ListMultimap 字段的指定值。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/list-multimap/remove?key=lmm:user-tag&mapKey=user:1" \
     *   -H "Content-Type: application/json" \
     *   -d '"java"'
     *
     * @param key      Redis 键
     * @param mapKey   Multimap 字段
     * @param mapValue Multimap 值
     * @return 是否删除成功
     */
    @PostMapping("/list-multimap/remove")
    public Map<String, Object> lmmRemove(@RequestParam String key,
                                         @RequestParam String mapKey,
                                         @RequestBody Object mapValue) {
        checkKey(key);
        checkKey(mapKey);

        boolean success = redissonService.lmmRemove(key, mapKey, mapValue);
        return ok(success);
    }

    /**
     * 删除 ListMultimap 字段的全部值。
     *
     * curl -X DELETE "http://localhost:8080/redisson/enhanced/list-multimap/remove-all?key=lmm:user-tag&mapKey=user:1"
     *
     * @param key    Redis 键
     * @param mapKey Multimap 字段
     * @return 删除的值列表
     */
    @DeleteMapping("/list-multimap/remove-all")
    public Map<String, Object> lmmRemoveAll(@RequestParam String key,
                                            @RequestParam String mapKey) {
        checkKey(key);
        checkKey(mapKey);

        List<Object> data = redissonService.lmmRemoveAll(key, mapKey);
        return ok(data);
    }

    /**
     * 判断 ListMultimap 是否包含字段和值。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/list-multimap/contains-entry?key=lmm:user-tag&mapKey=user:1" \
     *   -H "Content-Type: application/json" \
     *   -d '"java"'
     *
     * @param key      Redis 键
     * @param mapKey   Multimap 字段
     * @param mapValue Multimap 值
     * @return 是否包含
     */
    @PostMapping("/list-multimap/contains-entry")
    public Map<String, Object> lmmContainsEntry(@RequestParam String key,
                                                @RequestParam String mapKey,
                                                @RequestBody Object mapValue) {
        checkKey(key);
        checkKey(mapKey);

        boolean exists = redissonService.lmmContainsEntry(key, mapKey, mapValue);
        return ok(exists);
    }

    /**
     * 获取 ListMultimap 总值数量。
     *
     * curl "http://localhost:8080/redisson/enhanced/list-multimap/size?key=lmm:user-tag"
     *
     * @param key Redis 键
     * @return 总值数量
     */
    @GetMapping("/list-multimap/size")
    public Map<String, Object> lmmSize(@RequestParam String key) {
        checkKey(key);

        int size = redissonService.lmmSize(key);
        return ok(size);
    }

    /**
     * 清空 ListMultimap。
     *
     * curl -X DELETE "http://localhost:8080/redisson/enhanced/list-multimap/clear?key=lmm:user-tag"
     *
     * @param key Redis 键
     * @return 执行结果
     */
    @DeleteMapping("/list-multimap/clear")
    public Map<String, Object> lmmClear(@RequestParam String key) {
        checkKey(key);

        redissonService.lmmClear(key);
        return ok(true);
    }

    // -------------------------------------------------------------------------
    // SortedSet / LexSortedSet
    // -------------------------------------------------------------------------

    /**
     * 添加自然排序集合元素。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/sorted-set/add?key=sorted:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"B"'
     *
     * @param key   Redis 键
     * @param value 元素，建议实现 Comparable
     * @return 是否新增
     */
    @PostMapping("/sorted-set/add")
    public Map<String, Object> sortedSetAdd(@RequestParam String key,
                                            @RequestBody Object value) {
        checkKey(key);
        Assert.notNull(value, "value不能为空");

        boolean success = redissonService.sortedSetAdd(key, value);
        return ok(success);
    }

    /**
     * 批量添加自然排序集合元素。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/sorted-set/add-all?key=sorted:test" \
     *   -H "Content-Type: application/json" \
     *   -d '["A","B","C"]'
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 是否有新增
     */
    @PostMapping("/sorted-set/add-all")
    public Map<String, Object> sortedSetAddAll(@RequestParam String key,
                                               @RequestBody List<Object> values) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(values), "values不能为空");

        boolean success = redissonService.sortedSetAddAll(key, values);
        return ok(success);
    }

    /**
     * 获取自然排序集合全部元素。
     *
     * curl "http://localhost:8080/redisson/enhanced/sorted-set/read-all?key=sorted:test"
     *
     * @param key Redis 键
     * @return 元素集合
     */
    @GetMapping("/sorted-set/read-all")
    public Map<String, Object> sortedSetReadAll(@RequestParam String key) {
        checkKey(key);

        Collection<Object> data = redissonService.sortedSetReadAll(key);
        return ok(data);
    }

    /**
     * 删除自然排序集合元素。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/sorted-set/remove?key=sorted:test" \
     *   -H "Content-Type: application/json" \
     *   -d '["A","B"]'
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 是否删除成功
     */
    @PostMapping("/sorted-set/remove")
    public Map<String, Object> sortedSetRemove(@RequestParam String key,
                                               @RequestBody List<Object> values) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(values), "values不能为空");

        boolean success = redissonService.sortedSetRemove(key, values.toArray());
        return ok(success);
    }

    /**
     * 获取自然排序集合大小。
     *
     * curl "http://localhost:8080/redisson/enhanced/sorted-set/size?key=sorted:test"
     *
     * @param key Redis 键
     * @return 大小
     */
    @GetMapping("/sorted-set/size")
    public Map<String, Object> sortedSetSize(@RequestParam String key) {
        checkKey(key);

        int size = redissonService.sortedSetSize(key);
        return ok(size);
    }

    /**
     * 清空自然排序集合。
     *
     * curl -X DELETE "http://localhost:8080/redisson/enhanced/sorted-set/clear?key=sorted:test"
     *
     * @param key Redis 键
     * @return 执行结果
     */
    @DeleteMapping("/sorted-set/clear")
    public Map<String, Object> sortedSetClear(@RequestParam String key) {
        checkKey(key);

        redissonService.sortedSetClear(key);
        return ok(true);
    }

    /**
     * 添加字典序集合元素。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/lex/add?key=lex:test&value=apple"
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否新增
     */
    @PostMapping("/lex/add")
    public Map<String, Object> lexAdd(@RequestParam String key,
                                      @RequestParam String value) {
        checkKey(key);
        checkKey(value);

        boolean success = redissonService.lexAdd(key, value);
        return ok(success);
    }

    /**
     * 批量添加字典序集合元素。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/lex/add-all?key=lex:test" \
     *   -H "Content-Type: application/json" \
     *   -d '["apple","banana","cat"]'
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 是否有新增
     */
    @PostMapping("/lex/add-all")
    public Map<String, Object> lexAddAll(@RequestParam String key,
                                         @RequestBody List<String> values) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(values), "values不能为空");

        boolean success = redissonService.lexAddAll(key, values);
        return ok(success);
    }

    /**
     * 获取字典序集合全部元素。
     *
     * curl "http://localhost:8080/redisson/enhanced/lex/read-all?key=lex:test"
     *
     * @param key Redis 键
     * @return 元素集合
     */
    @GetMapping("/lex/read-all")
    public Map<String, Object> lexReadAll(@RequestParam String key) {
        checkKey(key);

        Collection<String> data = redissonService.lexReadAll(key);
        return ok(data);
    }

    /**
     * 获取大于等于指定元素的字典序集合。
     *
     * curl "http://localhost:8080/redisson/enhanced/lex/range-tail?key=lex:test&from=banana"
     *
     * @param key  Redis 键
     * @param from 开始元素
     * @return 元素集合
     */
    @GetMapping("/lex/range-tail")
    public Map<String, Object> lexRangeTail(@RequestParam String key,
                                            @RequestParam String from) {
        checkKey(key);
        checkKey(from);

        Collection<String> data = redissonService.lexRangeTail(key, from);
        return ok(data);
    }

    /**
     * 获取小于等于指定元素的字典序集合。
     *
     * curl "http://localhost:8080/redisson/enhanced/lex/range-head?key=lex:test&to=banana"
     *
     * @param key Redis 键
     * @param to  结束元素
     * @return 元素集合
     */
    @GetMapping("/lex/range-head")
    public Map<String, Object> lexRangeHead(@RequestParam String key,
                                            @RequestParam String to) {
        checkKey(key);
        checkKey(to);

        Collection<String> data = redissonService.lexRangeHead(key, to);
        return ok(data);
    }

    /**
     * 删除字典序集合元素。
     *
     * curl -X DELETE "http://localhost:8080/redisson/enhanced/lex/remove?key=lex:test&values=apple&values=banana"
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 是否删除成功
     */
    @DeleteMapping("/lex/remove")
    public Map<String, Object> lexRemove(@RequestParam String key,
                                         @RequestParam List<String> values) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(values), "values不能为空");

        boolean success = redissonService.lexRemove(key, values.toArray(new String[0]));
        return ok(success);
    }

    /**
     * 获取字典序集合大小。
     *
     * curl "http://localhost:8080/redisson/enhanced/lex/size?key=lex:test"
     *
     * @param key Redis 键
     * @return 大小
     */
    @GetMapping("/lex/size")
    public Map<String, Object> lexSize(@RequestParam String key) {
        checkKey(key);

        int size = redissonService.lexSize(key);
        return ok(size);
    }

    /**
     * 清空字典序集合。
     *
     * curl -X DELETE "http://localhost:8080/redisson/enhanced/lex/clear?key=lex:test"
     *
     * @param key Redis 键
     * @return 执行结果
     */
    @DeleteMapping("/lex/clear")
    public Map<String, Object> lexClear(@RequestParam String key) {
        checkKey(key);

        redissonService.lexClear(key);
        return ok(true);
    }

    // -------------------------------------------------------------------------
    // LongAdder / DoubleAdder
    // -------------------------------------------------------------------------

    /**
     * LongAdder 增加。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/long-adder/add?key=adder:pv&delta=1"
     *
     * @param key   Redis 键
     * @param delta 增量
     * @return 执行结果
     */
    @PostMapping("/long-adder/add")
    public Map<String, Object> longAdderAdd(@RequestParam String key,
                                            @RequestParam(defaultValue = "1") Long delta) {
        checkKey(key);
        Assert.notNull(delta, "delta不能为空");

        redissonService.longAdderAdd(key, delta);
        return ok(true);
    }

    /**
     * LongAdder 求和。
     *
     * curl "http://localhost:8080/redisson/enhanced/long-adder/sum?key=adder:pv"
     *
     * @param key Redis 键
     * @return 当前总和
     */
    @GetMapping("/long-adder/sum")
    public Map<String, Object> longAdderSum(@RequestParam String key) {
        checkKey(key);

        long value = redissonService.longAdderSum(key);
        return ok(value);
    }

    /**
     * LongAdder 重置。
     *
     * curl -X PUT "http://localhost:8080/redisson/enhanced/long-adder/reset?key=adder:pv"
     *
     * @param key Redis 键
     * @return 执行结果
     */
    @PutMapping("/long-adder/reset")
    public Map<String, Object> longAdderReset(@RequestParam String key) {
        checkKey(key);

        redissonService.longAdderReset(key);
        return ok(true);
    }

    /**
     * LongAdder 求和后重置。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/long-adder/sum-then-reset?key=adder:pv"
     *
     * @param key Redis 键
     * @return 重置前总和
     */
    @PostMapping("/long-adder/sum-then-reset")
    public Map<String, Object> longAdderSumThenReset(@RequestParam String key) {
        checkKey(key);

        long value = redissonService.longAdderSumThenReset(key);
        return ok(value);
    }

    /**
     * DoubleAdder 增加。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/double-adder/add?key=adder:score&delta=1.5"
     *
     * @param key   Redis 键
     * @param delta 增量
     * @return 执行结果
     */
    @PostMapping("/double-adder/add")
    public Map<String, Object> doubleAdderAdd(@RequestParam String key,
                                              @RequestParam(defaultValue = "1") Double delta) {
        checkKey(key);
        Assert.notNull(delta, "delta不能为空");

        redissonService.doubleAdderAdd(key, delta);
        return ok(true);
    }

    /**
     * DoubleAdder 求和。
     *
     * curl "http://localhost:8080/redisson/enhanced/double-adder/sum?key=adder:score"
     *
     * @param key Redis 键
     * @return 当前总和
     */
    @GetMapping("/double-adder/sum")
    public Map<String, Object> doubleAdderSum(@RequestParam String key) {
        checkKey(key);

        double value = redissonService.doubleAdderSum(key);
        return ok(value);
    }

    /**
     * DoubleAdder 重置。
     *
     * curl -X PUT "http://localhost:8080/redisson/enhanced/double-adder/reset?key=adder:score"
     *
     * @param key Redis 键
     * @return 执行结果
     */
    @PutMapping("/double-adder/reset")
    public Map<String, Object> doubleAdderReset(@RequestParam String key) {
        checkKey(key);

        redissonService.doubleAdderReset(key);
        return ok(true);
    }

    /**
     * DoubleAdder 求和后重置。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/double-adder/sum-then-reset?key=adder:score"
     *
     * @param key Redis 键
     * @return 重置前总和
     */
    @PostMapping("/double-adder/sum-then-reset")
    public Map<String, Object> doubleAdderSumThenReset(@RequestParam String key) {
        checkKey(key);

        double value = redissonService.doubleAdderSumThenReset(key);
        return ok(value);
    }

    // -------------------------------------------------------------------------
    // Bounded / Priority Queue
    // -------------------------------------------------------------------------

    /**
     * 初始化有界阻塞队列容量。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/bounded-queue/init?key=bq:test&capacity=10"
     *
     * @param key      Redis 键
     * @param capacity 容量
     * @return 是否初始化成功
     */
    @PostMapping("/bounded-queue/init")
    public Map<String, Object> boundedQueueTrySetCapacity(@RequestParam String key,
                                                          @RequestParam Integer capacity) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(capacity) && capacity > 0, "capacity必须大于0");

        boolean success = redissonService.boundedQueueTrySetCapacity(key, capacity);
        return ok(success);
    }

    /**
     * 有界阻塞队列入队。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/bounded-queue/offer?key=bq:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"A"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否入队成功
     */
    @PostMapping("/bounded-queue/offer")
    public Map<String, Object> boundedQueueOffer(@RequestParam String key,
                                                 @RequestBody Object value) {
        checkKey(key);

        boolean success = redissonService.boundedQueueOffer(key, value);
        return ok(success);
    }

    /**
     * 有界阻塞队列超时入队。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/bounded-queue/offer-timeout?key=bq:test&timeoutSeconds=3" \
     *   -H "Content-Type: application/json" \
     *   -d '"A"'
     *
     * @param key            Redis 键
     * @param timeoutSeconds 等待秒数
     * @param value          元素
     * @return 是否入队成功
     * @throws InterruptedException 线程中断时抛出
     */
    @PostMapping("/bounded-queue/offer-timeout")
    public Map<String, Object> boundedQueueOfferTimeout(@RequestParam String key,
                                                        @RequestParam(defaultValue = "3") Long timeoutSeconds,
                                                        @RequestBody Object value) throws InterruptedException {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(timeoutSeconds) && timeoutSeconds >= 0, "timeoutSeconds不能小于0");

        boolean success = redissonService.boundedQueueOffer(key, value, timeoutSeconds, TimeUnit.SECONDS);
        return ok(success);
    }

    /**
     * 有界阻塞队列出队。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/bounded-queue/poll?key=bq:test"
     *
     * @param key Redis 键
     * @return 元素
     */
    @PostMapping("/bounded-queue/poll")
    public Map<String, Object> boundedQueuePoll(@RequestParam String key) {
        checkKey(key);

        Object value = redissonService.boundedQueuePoll(key);
        return ok(value);
    }

    /**
     * 有界阻塞队列超时出队。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/bounded-queue/poll-timeout?key=bq:test&timeoutSeconds=3"
     *
     * @param key            Redis 键
     * @param timeoutSeconds 等待秒数
     * @return 元素
     * @throws InterruptedException 线程中断时抛出
     */
    @PostMapping("/bounded-queue/poll-timeout")
    public Map<String, Object> boundedQueuePollTimeout(@RequestParam String key,
                                                       @RequestParam(defaultValue = "3") Long timeoutSeconds) throws InterruptedException {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(timeoutSeconds) && timeoutSeconds >= 0, "timeoutSeconds不能小于0");

        Object value = redissonService.boundedQueuePoll(key, timeoutSeconds, TimeUnit.SECONDS);
        return ok(value);
    }

    /**
     * 获取有界阻塞队列信息。
     *
     * curl "http://localhost:8080/redisson/enhanced/bounded-queue/info?key=bq:test"
     *
     * @param key Redis 键
     * @return 队列信息
     */
    @GetMapping("/bounded-queue/info")
    public Map<String, Object> boundedQueueInfo(@RequestParam String key) {
        checkKey(key);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("size", redissonService.boundedQueueSize(key));
        data.put("remainingCapacity", redissonService.boundedQueueRemainingCapacity(key));
        return ok(data);
    }

    /**
     * 优先级队列入队。
     * 注意：元素建议使用 String、Integer 等可比较类型，或自定义实现 Comparable。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/priority-queue/offer?key=pq:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"A"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否入队成功
     */
    @PostMapping("/priority-queue/offer")
    public Map<String, Object> priorityQueueOffer(@RequestParam String key,
                                                  @RequestBody Object value) {
        checkKey(key);

        boolean success = redissonService.priorityQueueOffer(key, value);
        return ok(success);
    }

    /**
     * 优先级队列出队。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/priority-queue/poll?key=pq:test"
     *
     * @param key Redis 键
     * @return 元素
     */
    @PostMapping("/priority-queue/poll")
    public Map<String, Object> priorityQueuePoll(@RequestParam String key) {
        checkKey(key);

        Object value = redissonService.priorityQueuePoll(key);
        return ok(value);
    }

    /**
     * 优先级双端队列头部入队。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/priority-deque/offer-first?key=pdq:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"A"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否入队成功
     */
    @PostMapping("/priority-deque/offer-first")
    public Map<String, Object> priorityDequeOfferFirst(@RequestParam String key,
                                                       @RequestBody Object value) {
        checkKey(key);

        boolean success = redissonService.priorityDequeOfferFirst(key, value);
        return ok(success);
    }

    /**
     * 优先级双端队列尾部入队。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/priority-deque/offer-last?key=pdq:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"B"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否入队成功
     */
    @PostMapping("/priority-deque/offer-last")
    public Map<String, Object> priorityDequeOfferLast(@RequestParam String key,
                                                      @RequestBody Object value) {
        checkKey(key);

        boolean success = redissonService.priorityDequeOfferLast(key, value);
        return ok(success);
    }

    /**
     * 优先级双端队列头部出队。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/priority-deque/poll-first?key=pdq:test"
     *
     * @param key Redis 键
     * @return 元素
     */
    @PostMapping("/priority-deque/poll-first")
    public Map<String, Object> priorityDequePollFirst(@RequestParam String key) {
        checkKey(key);

        Object value = redissonService.priorityDequePollFirst(key);
        return ok(value);
    }

    /**
     * 优先级双端队列尾部出队。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/priority-deque/poll-last?key=pdq:test"
     *
     * @param key Redis 键
     * @return 元素
     */
    @PostMapping("/priority-deque/poll-last")
    public Map<String, Object> priorityDequePollLast(@RequestParam String key) {
        checkKey(key);

        Object value = redissonService.priorityDequePollLast(key);
        return ok(value);
    }

    /**
     * 构建 LocalCachedMap 默认配置。
     *
     * @return LocalCachedMapOptions
     */
    private LocalCachedMapOptions<Object, Object> localCachedMapOptions() {
        return LocalCachedMapOptions.<Object, Object>defaults();
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

    /**
     * JSONPath 设置请求体。
     *
     * @author Ateng
     * @since 2026-04-27
     */
    @Data
    public static class JsonPathSetRequest {

        /**
         * JSONPath。
         */
        private String path;

        /**
         * 值。
         */
        private Object value;

    }

}