package local.ateng.java.redis.controller;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import local.ateng.java.redis.service.RedisTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis Hash 操作控制器。
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/redis/hash")
public class RedisHashController {

    private final RedisTemplateService redisTemplateService;

    @PostMapping("/put")
    public ApiResult<Boolean> hPut(@RequestBody HashPutRequest request) {
        ApiResult<Boolean> validateResult = validateHashPutRequest(request);
        if (ObjectUtil.isNotNull(validateResult)) {
            return validateResult;
        }

        redisTemplateService.hPut(request.key(), request.hashKey(), request.value());

        log.info("设置 Redis Hash，key={}，hashKey={}", request.key(), request.hashKey());
        return ApiResult.ok(true);
    }

    @PostMapping("/put-all")
    public ApiResult<Boolean> hPutAll(@RequestBody HashPutAllRequest request) {
        if (ObjectUtil.isNull(request)) {
            return ApiResult.fail("请求参数不能为空");
        }
        if (StrUtil.isBlank(request.key())) {
            return ApiResult.fail("key 不能为空");
        }
        if (CollUtil.isEmpty(request.map())) {
            return ApiResult.fail("map 不能为空");
        }

        redisTemplateService.hPutAll(request.key(), request.map());

        log.info("批量设置 Redis Hash，key={}，fieldCount={}", request.key(), request.map().size());
        return ApiResult.ok(true);
    }

    @PostMapping("/put-if-absent")
    public ApiResult<Boolean> hPutIfAbsent(@RequestBody HashPutRequest request) {
        ApiResult<Boolean> validateResult = validateHashPutRequest(request);
        if (ObjectUtil.isNotNull(validateResult)) {
            return validateResult;
        }

        boolean result = redisTemplateService.hPutIfAbsent(request.key(), request.hashKey(), request.value());

        log.info("仅不存在时设置 Redis Hash，key={}，hashKey={}，result={}", request.key(), request.hashKey(), result);
        return ApiResult.ok(result);
    }

    @GetMapping("/get")
    public ApiResult<Object> hGet(@RequestParam String key, @RequestParam String hashKey) {
        if (StrUtil.isBlank(key)) {
            return ApiResult.fail("key 不能为空");
        }
        if (StrUtil.isBlank(hashKey)) {
            return ApiResult.fail("hashKey 不能为空");
        }

        Object value = redisTemplateService.hGet(key, hashKey);
        return ApiResult.ok(value);
    }

    @PostMapping("/multi-get")
    public ApiResult<List<Object>> hMultiGet(@RequestBody HashMultiGetRequest request) {
        if (ObjectUtil.isNull(request)) {
            return ApiResult.fail("请求参数不能为空");
        }
        if (StrUtil.isBlank(request.key())) {
            return ApiResult.fail("key 不能为空");
        }
        if (CollUtil.isEmpty(request.hashKeys())) {
            return ApiResult.fail("hashKeys 不能为空");
        }

        List<Object> values = redisTemplateService.hMultiGet(request.key(), request.hashKeys());
        return ApiResult.ok(values);
    }

    @GetMapping("/get-all")
    public ApiResult<Map<Object, Object>> hGetAll(@RequestParam String key) {
        if (StrUtil.isBlank(key)) {
            return ApiResult.fail("key 不能为空");
        }

        Map<Object, Object> values = redisTemplateService.hGetAll(key);
        return ApiResult.ok(values);
    }

    @GetMapping("/has-key")
    public ApiResult<Boolean> hHasKey(@RequestParam String key, @RequestParam String hashKey) {
        if (StrUtil.isBlank(key)) {
            return ApiResult.fail("key 不能为空");
        }
        if (StrUtil.isBlank(hashKey)) {
            return ApiResult.fail("hashKey 不能为空");
        }

        boolean exists = redisTemplateService.hHasKey(key, hashKey);
        return ApiResult.ok(exists);
    }

    @DeleteMapping
    public ApiResult<Long> hDelete(@RequestBody HashDeleteRequest request) {
        if (ObjectUtil.isNull(request)) {
            return ApiResult.fail("请求参数不能为空");
        }
        if (StrUtil.isBlank(request.key())) {
            return ApiResult.fail("key 不能为空");
        }
        if (CollUtil.isEmpty(request.hashKeys())) {
            return ApiResult.fail("hashKeys 不能为空");
        }

        // RedisTemplate Hash delete 接收 Object...，这里将请求集合转换为可变参数数组
        long count = redisTemplateService.hDelete(request.key(), request.hashKeys().toArray());

        log.info("删除 Redis Hash 字段，key={}，fieldCount={}，deleteCount={}",
                request.key(), request.hashKeys().size(), count);

        return ApiResult.ok(count);
    }

    @GetMapping("/size")
    public ApiResult<Long> hSize(@RequestParam String key) {
        if (StrUtil.isBlank(key)) {
            return ApiResult.fail("key 不能为空");
        }

        long size = redisTemplateService.hSize(key);
        return ApiResult.ok(size);
    }

    @GetMapping("/keys")
    public ApiResult<Set<Object>> hKeys(@RequestParam String key) {
        if (StrUtil.isBlank(key)) {
            return ApiResult.fail("key 不能为空");
        }

        Set<Object> hashKeys = redisTemplateService.hKeys(key);
        return ApiResult.ok(hashKeys);
    }

    @GetMapping("/values")
    public ApiResult<List<Object>> hValues(@RequestParam String key) {
        if (StrUtil.isBlank(key)) {
            return ApiResult.fail("key 不能为空");
        }

        List<Object> values = redisTemplateService.hValues(key);
        return ApiResult.ok(values);
    }

    @PostMapping("/increment")
    public ApiResult<Long> hIncrement(@RequestBody HashCounterRequest request) {
        if (ObjectUtil.isNull(request)) {
            return ApiResult.fail("请求参数不能为空");
        }
        if (StrUtil.isBlank(request.key())) {
            return ApiResult.fail("key 不能为空");
        }
        if (ObjectUtil.isNull(request.hashKey())) {
            return ApiResult.fail("hashKey 不能为空");
        }
        if (ObjectUtil.isNull(request.delta()) || request.delta() == 0) {
            return ApiResult.fail("delta 不能为 0");
        }

        long value = redisTemplateService.hIncrement(request.key(), request.hashKey(), request.delta());

        log.info("Redis Hash 整数递增，key={}，hashKey={}，delta={}，value={}",
                request.key(), request.hashKey(), request.delta(), value);

        return ApiResult.ok(value);
    }

    @PostMapping("/increment-double")
    public ApiResult<Double> hIncrementDouble(@RequestBody HashDoubleCounterRequest request) {
        if (ObjectUtil.isNull(request)) {
            return ApiResult.fail("请求参数不能为空");
        }
        if (StrUtil.isBlank(request.key())) {
            return ApiResult.fail("key 不能为空");
        }
        if (ObjectUtil.isNull(request.hashKey())) {
            return ApiResult.fail("hashKey 不能为空");
        }
        if (ObjectUtil.isNull(request.delta()) || request.delta() == 0D) {
            return ApiResult.fail("delta 不能为 0");
        }

        double value = redisTemplateService.hIncrement(request.key(), request.hashKey(), request.delta());

        log.info("Redis Hash 浮点递增，key={}，hashKey={}，delta={}，value={}",
                request.key(), request.hashKey(), request.delta(), value);

        return ApiResult.ok(value);
    }

    private ApiResult<Boolean> validateHashPutRequest(HashPutRequest request) {
        if (ObjectUtil.isNull(request)) {
            return ApiResult.fail("请求参数不能为空");
        }
        if (StrUtil.isBlank(request.key())) {
            return ApiResult.fail("key 不能为空");
        }
        if (ObjectUtil.isNull(request.hashKey())) {
            return ApiResult.fail("hashKey 不能为空");
        }
        if (ObjectUtil.isNull(request.value())) {
            return ApiResult.fail("value 不能为空");
        }
        return null;
    }

    public record HashPutRequest(String key, Object hashKey, Object value) {
    }

    public record HashPutAllRequest(String key, Map<Object, Object> map) {
    }

    public record HashMultiGetRequest(String key, List<Object> hashKeys) {
    }

    public record HashDeleteRequest(String key, List<Object> hashKeys) {
    }

    public record HashCounterRequest(String key, Object hashKey, Long delta) {
    }

    public record HashDoubleCounterRequest(String key, Object hashKey, Double delta) {
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
