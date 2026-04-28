package local.ateng.java.redis.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import local.ateng.java.redis.service.RedisTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Redis Key 通用操作控制器。
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/redis/key")
public class RedisKeyController {

    private final RedisTemplateService redisTemplateService;

    /**
     * 判断指定 Redis Key 是否存在。
     *
     * <pre>{@code
     * curl -X GET "http://localhost:8080/api/redis/key/has-key?key=user:1"
     * }</pre>
     *
     * @param key Redis Key
     * @return true 表示存在，false 表示不存在
     */
    @GetMapping("/has-key")
    public ApiResult<Boolean> hasKey(@RequestParam String key) {
        if (StrUtil.isBlank(key)) {
            return ApiResult.fail("key 不能为空");
        }

        boolean exists = redisTemplateService.hasKey(key);
        return ApiResult.ok(exists);
    }

    /**
     * 删除指定 Redis Key。
     *
     * <pre>{@code
     * curl -X DELETE "http://localhost:8080/api/redis/key?key=user:1"
     * }</pre>
     *
     * @param key Redis Key
     * @return true 表示删除成功，false 表示 Key 不存在或删除失败
     */
    @DeleteMapping
    public ApiResult<Boolean> delete(@RequestParam String key) {
        if (StrUtil.isBlank(key)) {
            return ApiResult.fail("key 不能为空");
        }

        boolean deleted = redisTemplateService.delete(key);
        log.info("删除 Redis Key，key={}，deleted={}", key, deleted);

        return ApiResult.ok(deleted);
    }

    /**
     * 批量删除多个 Redis Key。
     *
     * <pre>{@code
     * curl -X DELETE "http://localhost:8080/api/redis/key/batch" \
     *   -H "Content-Type: application/json" \
     *   -d '{"keys":["user:1","user:2","order:1"]}'
     * }</pre>
     *
     * @param request 批量删除请求参数
     * @return 成功删除的 Key 数量
     */
    @DeleteMapping("/batch")
    public ApiResult<Long> deleteBatch(@RequestBody BatchDeleteRequest request) {
        if (ObjectUtil.isNull(request) || CollUtil.isEmpty(request.keys())) {
            return ApiResult.fail("keys 不能为空");
        }

        long count = redisTemplateService.delete(request.keys());
        log.info("批量删除 Redis Key，keyCount={}，deleteCount={}", request.keys().size(), count);

        return ApiResult.ok(count);
    }

    /**
     * 设置指定 Redis Key 的过期时间。
     *
     * <pre>{@code
     * curl -X PUT "http://localhost:8080/api/redis/key/expire?key=user:1&timeoutSeconds=3600"
     * }</pre>
     *
     * @param key            Redis Key
     * @param timeoutSeconds 过期时间，单位：秒
     * @return true 表示设置成功，false 表示设置失败
     */
    @PutMapping("/expire")
    public ApiResult<Boolean> expire(@RequestParam String key, @RequestParam Long timeoutSeconds) {
        if (StrUtil.isBlank(key)) {
            return ApiResult.fail("key 不能为空");
        }
        if (ObjectUtil.isNull(timeoutSeconds) || timeoutSeconds <= 0) {
            return ApiResult.fail("timeoutSeconds 必须大于 0");
        }

        boolean result = redisTemplateService.expire(key, Duration.ofSeconds(timeoutSeconds));
        log.info("设置 Redis Key 过期时间，key={}，timeoutSeconds={}，result={}", key, timeoutSeconds, result);

        return ApiResult.ok(result);
    }

    /**
     * 设置指定 Redis Key 在指定时间点过期。
     *
     * <pre>{@code
     * curl -X PUT "http://localhost:8080/api/redis/key/expire-at?key=user:1&expireAt=2026-04-28T10:00:00Z"
     * }</pre>
     *
     * @param key      Redis Key
     * @param expireAt 过期时间点，ISO-8601 格式，例如：2026-04-28T10:00:00Z
     * @return true 表示设置成功，false 表示设置失败
     */
    @PutMapping("/expire-at")
    public ApiResult<Boolean> expireAt(@RequestParam String key, @RequestParam String expireAt) {
        if (StrUtil.isBlank(key)) {
            return ApiResult.fail("key 不能为空");
        }
        if (StrUtil.isBlank(expireAt)) {
            return ApiResult.fail("expireAt 不能为空");
        }

        Instant expireInstant;
        try {
            expireInstant = Instant.parse(expireAt);
        } catch (Exception e) {
            log.warn("Redis Key 过期时间格式错误，key={}，expireAt={}", key, expireAt, e);
            return ApiResult.fail("expireAt 格式错误，请使用 ISO-8601 格式，例如：2026-04-28T10:00:00Z");
        }

        boolean result = redisTemplateService.expireAt(key, expireInstant);
        log.info("设置 Redis Key 指定时间过期，key={}，expireAt={}，result={}", key, expireAt, result);

        return ApiResult.ok(result);
    }

    /**
     * 移除指定 Redis Key 的过期时间，使其永久有效。
     *
     * <pre>{@code
     * curl -X PUT "http://localhost:8080/api/redis/key/persist?key=user:1"
     * }</pre>
     *
     * @param key Redis Key
     * @return true 表示移除成功，false 表示 Key 不存在、无过期时间或移除失败
     */
    @PutMapping("/persist")
    public ApiResult<Boolean> persist(@RequestParam String key) {
        if (StrUtil.isBlank(key)) {
            return ApiResult.fail("key 不能为空");
        }

        boolean result = redisTemplateService.persist(key);
        log.info("移除 Redis Key 过期时间，key={}，result={}", key, result);

        return ApiResult.ok(result);
    }

    /**
     * 获取指定 Redis Key 的剩余过期时间。
     *
     * <pre>{@code
     * curl -X GET "http://localhost:8080/api/redis/key/expire?key=user:1"
     * }</pre>
     *
     * @param key Redis Key
     * @return Key 剩余过期时间信息；-1 表示永久有效，-2 表示 Key 不存在
     */
    @GetMapping("/expire")
    public ApiResult<ExpireResponse> getExpire(@RequestParam String key) {
        if (StrUtil.isBlank(key)) {
            return ApiResult.fail("key 不能为空");
        }

        Duration expire = redisTemplateService.getExpire(key);
        ExpireResponse response = buildExpireResponse(expire);

        return ApiResult.ok(response);
    }

    /**
     * 使用 SCAN 按匹配规则扫描 Redis Key。
     *
     * <pre>{@code
     * curl -G "http://localhost:8080/api/redis/key/scan" \
     *   --data-urlencode "pattern=user:*" \
     *   --data-urlencode "count=1000"
     * }</pre>
     *
     * @param pattern Key 匹配表达式，例如 user:*、order:*
     * @param count   每批扫描数量
     * @return 匹配到的 Redis Key 集合
     */
    @GetMapping("/scan")
    public ApiResult<Set<String>> scanKeys(@RequestParam String pattern,
                                           @RequestParam(defaultValue = "1000") Long count) {
        if (StrUtil.isBlank(pattern)) {
            return ApiResult.fail("pattern 不能为空");
        }

        long scanCount = ObjectUtil.defaultIfNull(count, 1000L);
        if (scanCount <= 0) {
            return ApiResult.fail("count 必须大于 0");
        }

        // 使用 service 内部的 SCAN 实现，避免直接调用 Redis KEYS 命令造成阻塞
        Set<String> keys = redisTemplateService.scanKeys(pattern, scanCount);

        log.info("扫描 Redis Key，pattern={}，count={}，resultSize={}", pattern, scanCount, keys.size());

        return ApiResult.ok(keys);
    }

    private ExpireResponse buildExpireResponse(Duration expire) {
        if (ObjectUtil.isNull(expire)) {
            return new ExpireResponse(-2L, -2L, "Key 不存在");
        }

        long millis = expire.toMillis();
        long seconds = expire.toSeconds();

        if (millis == -1L) {
            return new ExpireResponse(-1L, -1L, "Key 永久有效");
        }
        if (millis == -2L) {
            return new ExpireResponse(-2L, -2L, "Key 不存在");
        }

        return new ExpireResponse(seconds, millis, "Key 存在且已设置过期时间");
    }

    public record BatchDeleteRequest(List<String> keys) {
    }

    public record ExpireResponse(Long seconds, Long millis, String description) {
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