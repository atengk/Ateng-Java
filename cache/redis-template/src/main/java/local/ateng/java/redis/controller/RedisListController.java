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

/**
 * Redis List 操作控制器。
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/redis/list")
public class RedisListController {

    private final RedisTemplateService redisTemplateService;

    /**
     * 从 Redis List 左侧插入一个元素。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/list/left-push" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"queue:message","value":{"id":1,"content":"hello"}}'
     * }</pre>
     *
     * @param request List 插入请求参数
     * @return 插入后 List 的长度
     */
    @PostMapping("/left-push")
    public ApiResult<Long> lLeftPush(@RequestBody ListPushRequest request) {
        ApiResult<Long> validateResult = validateListPushRequest(request);
        if (ObjectUtil.isNotNull(validateResult)) {
            return validateResult;
        }

        long size = redisTemplateService.lLeftPush(request.key(), request.value());

        log.info("Redis List 左侧插入元素，key={}，size={}", request.key(), size);
        return ApiResult.ok(size);
    }

    /**
     * 从 Redis List 左侧批量插入元素。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/list/left-push-all" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"queue:message","values":[{"id":1,"content":"a"},{"id":2,"content":"b"}]}'
     * }</pre>
     *
     * @param request List 批量插入请求参数
     * @return 插入后 List 的长度
     */
    @PostMapping("/left-push-all")
    public ApiResult<Long> lLeftPushAll(@RequestBody ListPushAllRequest request) {
        ApiResult<Long> validateResult = validateListPushAllRequest(request);
        if (ObjectUtil.isNotNull(validateResult)) {
            return validateResult;
        }

        long size = redisTemplateService.lLeftPushAll(request.key(), request.values());

        log.info("Redis List 左侧批量插入元素，key={}，valueCount={}，size={}",
                request.key(), request.values().size(), size);

        return ApiResult.ok(size);
    }

    /**
     * 从 Redis List 右侧插入一个元素。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/list/right-push" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"queue:message","value":{"id":3,"content":"hello"}}'
     * }</pre>
     *
     * @param request List 插入请求参数
     * @return 插入后 List 的长度
     */
    @PostMapping("/right-push")
    public ApiResult<Long> lRightPush(@RequestBody ListPushRequest request) {
        ApiResult<Long> validateResult = validateListPushRequest(request);
        if (ObjectUtil.isNotNull(validateResult)) {
            return validateResult;
        }

        long size = redisTemplateService.lRightPush(request.key(), request.value());

        log.info("Redis List 右侧插入元素，key={}，size={}", request.key(), size);
        return ApiResult.ok(size);
    }

    /**
     * 从 Redis List 右侧批量插入元素。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/list/right-push-all" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"queue:message","values":[{"id":3,"content":"c"},{"id":4,"content":"d"}]}'
     * }</pre>
     *
     * @param request List 批量插入请求参数
     * @return 插入后 List 的长度
     */
    @PostMapping("/right-push-all")
    public ApiResult<Long> lRightPushAll(@RequestBody ListPushAllRequest request) {
        ApiResult<Long> validateResult = validateListPushAllRequest(request);
        if (ObjectUtil.isNotNull(validateResult)) {
            return validateResult;
        }

        long size = redisTemplateService.lRightPushAll(request.key(), request.values());

        log.info("Redis List 右侧批量插入元素，key={}，valueCount={}，size={}",
                request.key(), request.values().size(), size);

        return ApiResult.ok(size);
    }

    /**
     * 从 Redis List 左侧弹出一个元素。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/list/left-pop" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"queue:message"}'
     * }</pre>
     *
     * 阻塞弹出示例：
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/list/left-pop" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"queue:message","timeoutSeconds":5}'
     * }</pre>
     *
     * @param request List 弹出请求参数
     * @return 弹出的元素；List 为空或超时时返回 null
     */
    @PostMapping("/left-pop")
    public ApiResult<Object> lLeftPop(@RequestBody ListPopRequest request) {
        ApiResult<Object> validateResult = validateListPopRequest(request);
        if (ObjectUtil.isNotNull(validateResult)) {
            return validateResult;
        }

        Object value;
        if (ObjectUtil.isNull(request.timeoutSeconds())) {
            value = redisTemplateService.lLeftPop(request.key());
        } else {
            // timeoutSeconds 存在时使用阻塞弹出，适合简单消费队列场景
            value = redisTemplateService.lLeftPop(request.key(), Duration.ofSeconds(request.timeoutSeconds()));
        }

        log.info("Redis List 左侧弹出元素，key={}，hasTimeout={}",
                request.key(), ObjectUtil.isNotNull(request.timeoutSeconds()));

        return ApiResult.ok(value);
    }

    /**
     * 从 Redis List 右侧弹出一个元素。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/list/right-pop" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"queue:message"}'
     * }</pre>
     *
     * 阻塞弹出示例：
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/list/right-pop" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"queue:message","timeoutSeconds":5}'
     * }</pre>
     *
     * @param request List 弹出请求参数
     * @return 弹出的元素；List 为空或超时时返回 null
     */
    @PostMapping("/right-pop")
    public ApiResult<Object> lRightPop(@RequestBody ListPopRequest request) {
        ApiResult<Object> validateResult = validateListPopRequest(request);
        if (ObjectUtil.isNotNull(validateResult)) {
            return validateResult;
        }

        Object value;
        if (ObjectUtil.isNull(request.timeoutSeconds())) {
            value = redisTemplateService.lRightPop(request.key());
        } else {
            // timeoutSeconds 存在时使用阻塞弹出，避免调用方频繁轮询 Redis
            value = redisTemplateService.lRightPop(request.key(), Duration.ofSeconds(request.timeoutSeconds()));
        }

        log.info("Redis List 右侧弹出元素，key={}，hasTimeout={}",
                request.key(), ObjectUtil.isNotNull(request.timeoutSeconds()));

        return ApiResult.ok(value);
    }

    /**
     * 根据索引获取 Redis List 中的元素。
     *
     * <pre>{@code
     * curl -X GET "http://localhost:8080/api/redis/list/index?key=queue:message&index=0"
     * }</pre>
     *
     * 获取最后一个元素示例：
     *
     * <pre>{@code
     * curl -X GET "http://localhost:8080/api/redis/list/index?key=queue:message&index=-1"
     * }</pre>
     *
     * @param key   Redis Key
     * @param index 元素索引，支持负数索引
     * @return 指定索引位置的元素；索引不存在时返回 null
     */
    @GetMapping("/index")
    public ApiResult<Object> lIndex(@RequestParam String key, @RequestParam Long index) {
        if (StrUtil.isBlank(key)) {
            return ApiResult.fail("key 不能为空");
        }
        if (ObjectUtil.isNull(index)) {
            return ApiResult.fail("index 不能为空");
        }

        Object value = redisTemplateService.lIndex(key, index);
        return ApiResult.ok(value);
    }

    /**
     * 获取 Redis List 指定范围内的元素。
     *
     * <pre>{@code
     * curl -X GET "http://localhost:8080/api/redis/list/range?key=queue:message&start=0&end=9"
     * }</pre>
     *
     * 获取全部元素示例：
     *
     * <pre>{@code
     * curl -X GET "http://localhost:8080/api/redis/list/range?key=queue:message&start=0&end=-1"
     * }</pre>
     *
     * @param key   Redis Key
     * @param start 开始索引，支持负数索引
     * @param end   结束索引，支持负数索引
     * @return 指定范围内的元素列表
     */
    @GetMapping("/range")
    public ApiResult<List<Object>> lRange(@RequestParam String key,
                                          @RequestParam Long start,
                                          @RequestParam Long end) {
        if (StrUtil.isBlank(key)) {
            return ApiResult.fail("key 不能为空");
        }
        if (ObjectUtil.isNull(start)) {
            return ApiResult.fail("start 不能为空");
        }
        if (ObjectUtil.isNull(end)) {
            return ApiResult.fail("end 不能为空");
        }

        List<Object> values = redisTemplateService.lRange(key, start, end);
        return ApiResult.ok(values);
    }

    /**
     * 根据索引设置 Redis List 中的元素值。
     *
     * <pre>{@code
     * curl -X PUT "http://localhost:8080/api/redis/list/set" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"queue:message","index":0,"value":{"id":1,"content":"updated"}}'
     * }</pre>
     *
     * @param request List 索引设置请求参数
     * @return true 表示设置成功
     */
    @PutMapping("/set")
    public ApiResult<Boolean> lSet(@RequestBody ListSetRequest request) {
        if (ObjectUtil.isNull(request)) {
            return ApiResult.fail("请求参数不能为空");
        }
        if (StrUtil.isBlank(request.key())) {
            return ApiResult.fail("key 不能为空");
        }
        if (ObjectUtil.isNull(request.index())) {
            return ApiResult.fail("index 不能为空");
        }
        if (ObjectUtil.isNull(request.value())) {
            return ApiResult.fail("value 不能为空");
        }

        redisTemplateService.lSet(request.key(), request.index(), request.value());

        log.info("Redis List 按索引设置元素，key={}，index={}", request.key(), request.index());
        return ApiResult.ok(true);
    }

    /**
     * 裁剪 Redis List，只保留指定范围内的元素。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/list/trim" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"queue:message","start":0,"end":99}'
     * }</pre>
     *
     * @param request List 范围请求参数
     * @return true 表示裁剪成功
     */
    @PostMapping("/trim")
    public ApiResult<Boolean> lTrim(@RequestBody ListRangeRequest request) {
        if (ObjectUtil.isNull(request)) {
            return ApiResult.fail("请求参数不能为空");
        }
        if (StrUtil.isBlank(request.key())) {
            return ApiResult.fail("key 不能为空");
        }
        if (ObjectUtil.isNull(request.start())) {
            return ApiResult.fail("start 不能为空");
        }
        if (ObjectUtil.isNull(request.end())) {
            return ApiResult.fail("end 不能为空");
        }

        // LTRIM 会保留指定区间元素，常用于固定长度队列或时间线裁剪
        redisTemplateService.lTrim(request.key(), request.start(), request.end());

        log.info("Redis List 裁剪元素，key={}，start={}，end={}",
                request.key(), request.start(), request.end());

        return ApiResult.ok(true);
    }

    /**
     * 删除 Redis List 中指定数量的匹配元素。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/list/remove" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"queue:message","count":1,"value":{"id":1,"content":"hello"}}'
     * }</pre>
     *
     * 删除全部匹配元素示例：
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/list/remove" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"queue:message","count":0,"value":"hello"}'
     * }</pre>
     *
     * @param request List 删除元素请求参数；count 大于 0 从左到右删除，小于 0 从右到左删除，等于 0 删除全部匹配元素
     * @return 成功删除的元素数量
     */
    @PostMapping("/remove")
    public ApiResult<Long> lRemove(@RequestBody ListRemoveRequest request) {
        if (ObjectUtil.isNull(request)) {
            return ApiResult.fail("请求参数不能为空");
        }
        if (StrUtil.isBlank(request.key())) {
            return ApiResult.fail("key 不能为空");
        }
        if (ObjectUtil.isNull(request.count())) {
            return ApiResult.fail("count 不能为空");
        }
        if (ObjectUtil.isNull(request.value())) {
            return ApiResult.fail("value 不能为空");
        }

        long removeCount = redisTemplateService.lRemove(request.key(), request.count(), request.value());

        log.info("Redis List 删除指定元素，key={}，count={}，removeCount={}",
                request.key(), request.count(), removeCount);

        return ApiResult.ok(removeCount);
    }

    /**
     * 获取 Redis List 的长度。
     *
     * <pre>{@code
     * curl -X GET "http://localhost:8080/api/redis/list/size?key=queue:message"
     * }</pre>
     *
     * @param key Redis Key
     * @return List 长度；Key 不存在时返回 0
     */
    @GetMapping("/size")
    public ApiResult<Long> lSize(@RequestParam String key) {
        if (StrUtil.isBlank(key)) {
            return ApiResult.fail("key 不能为空");
        }

        long size = redisTemplateService.lSize(key);
        return ApiResult.ok(size);
    }

    private ApiResult<Long> validateListPushRequest(ListPushRequest request) {
        if (ObjectUtil.isNull(request)) {
            return ApiResult.fail("请求参数不能为空");
        }
        if (StrUtil.isBlank(request.key())) {
            return ApiResult.fail("key 不能为空");
        }
        if (ObjectUtil.isNull(request.value())) {
            return ApiResult.fail("value 不能为空");
        }
        return null;
    }

    private ApiResult<Long> validateListPushAllRequest(ListPushAllRequest request) {
        if (ObjectUtil.isNull(request)) {
            return ApiResult.fail("请求参数不能为空");
        }
        if (StrUtil.isBlank(request.key())) {
            return ApiResult.fail("key 不能为空");
        }
        if (CollUtil.isEmpty(request.values())) {
            return ApiResult.fail("values 不能为空");
        }
        return null;
    }

    private ApiResult<Object> validateListPopRequest(ListPopRequest request) {
        if (ObjectUtil.isNull(request)) {
            return ApiResult.fail("请求参数不能为空");
        }
        if (StrUtil.isBlank(request.key())) {
            return ApiResult.fail("key 不能为空");
        }
        if (ObjectUtil.isNotNull(request.timeoutSeconds()) && request.timeoutSeconds() <= 0) {
            return ApiResult.fail("timeoutSeconds 必须大于 0");
        }
        return null;
    }

    public record ListPushRequest(String key, Object value) {
    }

    public record ListPushAllRequest(String key, List<Object> values) {
    }

    public record ListPopRequest(String key, Long timeoutSeconds) {
    }

    public record ListSetRequest(String key, Long index, Object value) {
    }

    public record ListRangeRequest(String key, Long start, Long end) {
    }

    public record ListRemoveRequest(String key, Long count, Object value) {
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