package local.ateng.java.redis.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import local.ateng.java.redis.service.RedisTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Redis Value 操作控制器。
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/redis/value")
public class RedisValueController {

    private final RedisTemplateService redisTemplateService;

    /**
     * 设置 Redis String 类型缓存值。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/value/set" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"user:1","value":{"id":1,"name":"Ateng"},"timeoutSeconds":3600}'
     * }</pre>
     *
     * @param request 设置缓存请求参数
     * @return true 表示设置成功
     */
    @PostMapping("/set")
    public ApiResult<Boolean> set(@RequestBody SetValueRequest request) {
        ApiResult<Boolean> validateResult = validateSetValueRequest(request);
        if (ObjectUtil.isNotNull(validateResult)) {
            return validateResult;
        }

        if (ObjectUtil.isNull(request.timeoutSeconds())) {
            redisTemplateService.set(request.key(), request.value());
        } else {
            redisTemplateService.set(request.key(), request.value(), Duration.ofSeconds(request.timeoutSeconds()));
        }

        log.info("设置 Redis Value，key={}，hasTimeout={}", request.key(), ObjectUtil.isNotNull(request.timeoutSeconds()));
        return ApiResult.ok(true);
    }

    /**
     * 当 Redis Key 不存在时设置缓存值。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/value/set-if-absent" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"lock:user:1","value":"request-001","timeoutSeconds":30}'
     * }</pre>
     *
     * @param request 设置缓存请求参数
     * @return true 表示设置成功，false 表示 Key 已存在
     */
    @PostMapping("/set-if-absent")
    public ApiResult<Boolean> setIfAbsent(@RequestBody SetValueRequest request) {
        ApiResult<Boolean> validateResult = validateSetValueRequest(request);
        if (ObjectUtil.isNotNull(validateResult)) {
            return validateResult;
        }

        boolean result;
        if (ObjectUtil.isNull(request.timeoutSeconds())) {
            result = redisTemplateService.setIfAbsent(request.key(), request.value());
        } else {
            result = redisTemplateService.setIfAbsent(request.key(), request.value(), Duration.ofSeconds(request.timeoutSeconds()));
        }

        log.info("仅不存在时设置 Redis Value，key={}，result={}", request.key(), result);
        return ApiResult.ok(result);
    }

    /**
     * 当 Redis Key 已存在时设置缓存值。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/value/set-if-present" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"user:1","value":{"id":1,"name":"Ateng-Updated"},"timeoutSeconds":3600}'
     * }</pre>
     *
     * @param request 设置缓存请求参数
     * @return true 表示设置成功，false 表示 Key 不存在
     */
    @PostMapping("/set-if-present")
    public ApiResult<Boolean> setIfPresent(@RequestBody SetValueRequest request) {
        ApiResult<Boolean> validateResult = validateSetValueRequest(request);
        if (ObjectUtil.isNotNull(validateResult)) {
            return validateResult;
        }

        boolean result;
        if (ObjectUtil.isNull(request.timeoutSeconds())) {
            result = redisTemplateService.setIfPresent(request.key(), request.value());
        } else {
            result = redisTemplateService.setIfPresent(request.key(), request.value(), Duration.ofSeconds(request.timeoutSeconds()));
        }

        log.info("仅存在时设置 Redis Value，key={}，result={}", request.key(), result);
        return ApiResult.ok(result);
    }

    /**
     * 获取指定 Redis Key 的缓存值。
     *
     * <pre>{@code
     * curl -X GET "http://localhost:8080/api/redis/value/get?key=user:1"
     * }</pre>
     *
     * @param key Redis Key
     * @return 缓存值；Key 不存在时返回 null
     */
    @GetMapping("/get")
    public ApiResult<Object> get(@RequestParam String key) {
        if (StrUtil.isBlank(key)) {
            return ApiResult.fail("key 不能为空");
        }

        Object value = redisTemplateService.get(key);
        return ApiResult.ok(value);
    }

    /**
     * 获取指定 Redis Key 的旧值，并设置新值。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/value/get-and-set" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"user:1","value":{"id":1,"name":"Ateng-New"}}'
     * }</pre>
     *
     * @param request 获取并替换缓存请求参数
     * @return 替换前的旧值；Key 不存在时返回 null
     */
    @PostMapping("/get-and-set")
    public ApiResult<Object> getAndSet(@RequestBody GetAndSetRequest request) {
        if (ObjectUtil.isNull(request)) {
            return ApiResult.fail("请求参数不能为空");
        }
        if (StrUtil.isBlank(request.key())) {
            return ApiResult.fail("key 不能为空");
        }
        if (ObjectUtil.isNull(request.value())) {
            return ApiResult.fail("value 不能为空");
        }

        Object oldValue = redisTemplateService.getAndSet(request.key(), request.value());

        log.info("获取并替换 Redis Value，key={}", request.key());
        return ApiResult.ok(oldValue);
    }

    /**
     * 批量获取多个 Redis Key 的缓存值。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/value/multi-get" \
     *   -H "Content-Type: application/json" \
     *   -d '{"keys":["user:1","user:2","user:3"]}'
     * }</pre>
     *
     * @param request 多 Key 查询请求参数
     * @return 缓存值列表，返回顺序与请求 Key 顺序一致
     */
    @PostMapping("/multi-get")
    public ApiResult<List<Object>> multiGet(@RequestBody MultiKeyRequest request) {
        if (ObjectUtil.isNull(request) || CollUtil.isEmpty(request.keys())) {
            return ApiResult.fail("keys 不能为空");
        }

        List<Object> values = redisTemplateService.multiGet(request.keys());
        return ApiResult.ok(values);
    }

    /**
     * 批量获取多个 Redis Key 的缓存值，并按 Key 组装为 Map。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/value/multi-get-map" \
     *   -H "Content-Type: application/json" \
     *   -d '{"keys":["user:1","user:2","user:3"]}'
     * }</pre>
     *
     * @param request 多 Key 查询请求参数
     * @return Key 与缓存值的映射关系
     */
    @PostMapping("/multi-get-map")
    public ApiResult<Map<String, Object>> multiGetAsMap(@RequestBody MultiKeyRequest request) {
        if (ObjectUtil.isNull(request) || CollUtil.isEmpty(request.keys())) {
            return ApiResult.fail("keys 不能为空");
        }

        // 按请求 Key 顺序组装 Map，便于调用方定位每个 Key 对应的缓存值
        Map<String, Object> valueMap = redisTemplateService.multiGetAsMap(request.keys());
        return ApiResult.ok(valueMap);
    }

    /**
     * 对指定 Redis Key 执行整数自增。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/value/increment" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"counter:user:1","delta":1}'
     * }</pre>
     *
     * @param request 计数器请求参数；delta 为空时默认自增 1
     * @return 自增后的值
     */
    @PostMapping("/increment")
    public ApiResult<Long> increment(@RequestBody CounterRequest request) {
        if (ObjectUtil.isNull(request)) {
            return ApiResult.fail("请求参数不能为空");
        }
        if (StrUtil.isBlank(request.key())) {
            return ApiResult.fail("key 不能为空");
        }
        if (ObjectUtil.isNotNull(request.delta()) && request.delta() <= 0) {
            return ApiResult.fail("delta 必须大于 0");
        }

        long value = ObjectUtil.isNull(request.delta())
                ? redisTemplateService.increment(request.key())
                : redisTemplateService.increment(request.key(), request.delta());

        log.info("Redis Value 自增，key={}，delta={}，value={}", request.key(), request.delta(), value);
        return ApiResult.ok(value);
    }

    /**
     * 对指定 Redis Key 执行浮点数自增。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/value/increment-double" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"score:user:1","delta":1.5}'
     * }</pre>
     *
     * @param request 浮点计数器请求参数
     * @return 自增后的值
     */
    @PostMapping("/increment-double")
    public ApiResult<Double> incrementDouble(@RequestBody DoubleCounterRequest request) {
        if (ObjectUtil.isNull(request)) {
            return ApiResult.fail("请求参数不能为空");
        }
        if (StrUtil.isBlank(request.key())) {
            return ApiResult.fail("key 不能为空");
        }
        if (ObjectUtil.isNull(request.delta()) || request.delta() <= 0) {
            return ApiResult.fail("delta 必须大于 0");
        }

        double value = redisTemplateService.increment(request.key(), request.delta());

        log.info("Redis Value 浮点自增，key={}，delta={}，value={}", request.key(), request.delta(), value);
        return ApiResult.ok(value);
    }

    /**
     * 对指定 Redis Key 执行整数自减。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/value/decrement" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"counter:user:1","delta":1}'
     * }</pre>
     *
     * @param request 计数器请求参数；delta 为空时默认自减 1
     * @return 自减后的值
     */
    @PostMapping("/decrement")
    public ApiResult<Long> decrement(@RequestBody CounterRequest request) {
        if (ObjectUtil.isNull(request)) {
            return ApiResult.fail("请求参数不能为空");
        }
        if (StrUtil.isBlank(request.key())) {
            return ApiResult.fail("key 不能为空");
        }
        if (ObjectUtil.isNotNull(request.delta()) && request.delta() <= 0) {
            return ApiResult.fail("delta 必须大于 0");
        }

        long value = ObjectUtil.isNull(request.delta())
                ? redisTemplateService.decrement(request.key())
                : redisTemplateService.decrement(request.key(), request.delta());

        log.info("Redis Value 自减，key={}，delta={}，value={}", request.key(), request.delta(), value);
        return ApiResult.ok(value);
    }

    private ApiResult<Boolean> validateSetValueRequest(SetValueRequest request) {
        if (ObjectUtil.isNull(request)) {
            return ApiResult.fail("请求参数不能为空");
        }
        if (StrUtil.isBlank(request.key())) {
            return ApiResult.fail("key 不能为空");
        }
        if (ObjectUtil.isNull(request.value())) {
            return ApiResult.fail("value 不能为空");
        }
        if (ObjectUtil.isNotNull(request.timeoutSeconds()) && request.timeoutSeconds() <= 0) {
            return ApiResult.fail("timeoutSeconds 必须大于 0");
        }
        return null;
    }

    public record SetValueRequest(String key, Object value, Long timeoutSeconds) {
    }

    public record GetAndSetRequest(String key, Object value) {
    }

    public record MultiKeyRequest(List<String> keys) {
    }

    public record CounterRequest(String key, Long delta) {
    }

    public record DoubleCounterRequest(String key, Double delta) {
    }

    public record ApiResult<T>(Integer code, String message, T data) {

        public static <T> ApiResult<T> ok(T data) {
            return new ApiResult<>(200, "操作成功", data);
        }

        public static <T> ApiResult<T> fail(String message) {
            return new ApiResult<>(500, message, null);
        }

    }

}