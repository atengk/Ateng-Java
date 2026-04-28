package local.ateng.java.redis.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import local.ateng.java.redis.service.RedisTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * Redis Set 操作控制器。
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/redis/set")
public class RedisSetController {

    private final RedisTemplateService redisTemplateService;

    /**
     * 向 Redis Set 中添加一个或多个元素。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/set/add" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"user:tags:1","values":["java","redis","springboot"]}'
     * }</pre>
     *
     * 添加对象元素示例：
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/set/add" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"user:favorites:1","values":[{"id":1,"name":"Redis"},{"id":2,"name":"SpringBoot"}]}'
     * }</pre>
     *
     * @param request Set 批量元素请求参数
     * @return 成功添加的元素数量
     */
    @PostMapping("/add")
    public ApiResult<Long> sAdd(@RequestBody SetValuesRequest request) {
        ApiResult<Long> validateResult = validateSetValuesRequest(request);
        if (ObjectUtil.isNotNull(validateResult)) {
            return validateResult;
        }

        long count = redisTemplateService.sAdd(request.key(), request.values().toArray());

        log.info("Redis Set 添加元素，key={}，valueCount={}，addCount={}",
                request.key(), request.values().size(), count);

        return ApiResult.ok(count);
    }

    /**
     * 从 Redis Set 中删除一个或多个元素。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/set/remove" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"user:tags:1","values":["redis","springboot"]}'
     * }</pre>
     *
     * @param request Set 批量元素请求参数
     * @return 成功删除的元素数量
     */
    @PostMapping("/remove")
    public ApiResult<Long> sRemove(@RequestBody SetValuesRequest request) {
        ApiResult<Long> validateResult = validateSetValuesRequest(request);
        if (ObjectUtil.isNotNull(validateResult)) {
            return validateResult;
        }

        long count = redisTemplateService.sRemove(request.key(), request.values().toArray());

        log.info("Redis Set 删除元素，key={}，valueCount={}，removeCount={}",
                request.key(), request.values().size(), count);

        return ApiResult.ok(count);
    }

    /**
     * 判断指定元素是否为 Redis Set 的成员。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/set/is-member" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"user:tags:1","value":"java"}'
     * }</pre>
     *
     * @param request Set 单元素请求参数
     * @return true 表示元素存在，false 表示元素不存在
     */
    @PostMapping("/is-member")
    public ApiResult<Boolean> sIsMember(@RequestBody SetValueRequest request) {
        if (ObjectUtil.isNull(request)) {
            return ApiResult.fail("请求参数不能为空");
        }
        if (StrUtil.isBlank(request.key())) {
            return ApiResult.fail("key 不能为空");
        }
        if (ObjectUtil.isNull(request.value())) {
            return ApiResult.fail("value 不能为空");
        }

        boolean result = redisTemplateService.sIsMember(request.key(), request.value());
        return ApiResult.ok(result);
    }

    /**
     * 获取 Redis Set 中的所有元素。
     *
     * <pre>{@code
     * curl -X GET "http://localhost:8080/api/redis/set/members?key=user:tags:1"
     * }</pre>
     *
     * @param key Redis Key
     * @return Set 元素集合；Key 不存在时返回空集合
     */
    @GetMapping("/members")
    public ApiResult<Set<Object>> sMembers(@RequestParam String key) {
        if (StrUtil.isBlank(key)) {
            return ApiResult.fail("key 不能为空");
        }

        Set<Object> members = redisTemplateService.sMembers(key);
        return ApiResult.ok(members);
    }

    /**
     * 从 Redis Set 中随机弹出一个元素。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/set/pop" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"lottery:pool"}'
     * }</pre>
     *
     * @param request Set 随机弹出请求参数
     * @return 随机弹出的元素；Set 为空时返回 null
     */
    @PostMapping("/pop")
    public ApiResult<Object> sPop(@RequestBody SetPopRequest request) {
        if (ObjectUtil.isNull(request)) {
            return ApiResult.fail("请求参数不能为空");
        }
        if (StrUtil.isBlank(request.key())) {
            return ApiResult.fail("key 不能为空");
        }

        Object value = redisTemplateService.sPop(request.key());

        log.info("Redis Set 随机弹出单个元素，key={}", request.key());
        return ApiResult.ok(value);
    }

    /**
     * 从 Redis Set 中随机弹出指定数量的元素。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/set/pop-count" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"lottery:pool","count":3}'
     * }</pre>
     *
     * @param request Set 随机弹出请求参数
     * @return 随机弹出的元素列表；Set 为空时返回空列表
     */
    @PostMapping("/pop-count")
    public ApiResult<List<Object>> sPopCount(@RequestBody SetPopRequest request) {
        if (ObjectUtil.isNull(request)) {
            return ApiResult.fail("请求参数不能为空");
        }
        if (StrUtil.isBlank(request.key())) {
            return ApiResult.fail("key 不能为空");
        }
        if (ObjectUtil.isNull(request.count()) || request.count() <= 0) {
            return ApiResult.fail("count 必须大于 0");
        }

        // SPOP 会从集合中移除并返回随机元素，适合抽奖、任务随机领取等场景
        List<Object> values = redisTemplateService.sPop(request.key(), request.count());

        log.info("Redis Set 随机弹出多个元素，key={}，count={}，resultSize={}",
                request.key(), request.count(), values.size());

        return ApiResult.ok(values);
    }

    /**
     * 获取 Redis Set 的元素数量。
     *
     * <pre>{@code
     * curl -X GET "http://localhost:8080/api/redis/set/size?key=user:tags:1"
     * }</pre>
     *
     * @param key Redis Key
     * @return Set 元素数量；Key 不存在时返回 0
     */
    @GetMapping("/size")
    public ApiResult<Long> sSize(@RequestParam String key) {
        if (StrUtil.isBlank(key)) {
            return ApiResult.fail("key 不能为空");
        }

        long size = redisTemplateService.sSize(key);
        return ApiResult.ok(size);
    }

    /**
     * 获取两个 Redis Set 的交集。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/set/intersect" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"user:tags:1","otherKey":"user:tags:2"}'
     * }</pre>
     *
     * @param request Set 集合运算请求参数
     * @return 两个 Set 的交集元素集合
     */
    @PostMapping("/intersect")
    public ApiResult<Set<Object>> sIntersect(@RequestBody SetOperationRequest request) {
        ApiResult<Set<Object>> validateResult = validateSetOperationRequest(request);
        if (ObjectUtil.isNotNull(validateResult)) {
            return validateResult;
        }

        Set<Object> values = redisTemplateService.sIntersect(request.key(), request.otherKey());
        return ApiResult.ok(values);
    }

    /**
     * 获取两个 Redis Set 的并集。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/set/union" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"user:tags:1","otherKey":"user:tags:2"}'
     * }</pre>
     *
     * @param request Set 集合运算请求参数
     * @return 两个 Set 的并集元素集合
     */
    @PostMapping("/union")
    public ApiResult<Set<Object>> sUnion(@RequestBody SetOperationRequest request) {
        ApiResult<Set<Object>> validateResult = validateSetOperationRequest(request);
        if (ObjectUtil.isNotNull(validateResult)) {
            return validateResult;
        }

        Set<Object> values = redisTemplateService.sUnion(request.key(), request.otherKey());
        return ApiResult.ok(values);
    }

    /**
     * 获取两个 Redis Set 的差集。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/set/difference" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"user:tags:1","otherKey":"user:tags:2"}'
     * }</pre>
     *
     * @param request Set 集合运算请求参数
     * @return 差集元素集合，即 key 中存在但 otherKey 中不存在的元素
     */
    @PostMapping("/difference")
    public ApiResult<Set<Object>> sDifference(@RequestBody SetOperationRequest request) {
        ApiResult<Set<Object>> validateResult = validateSetOperationRequest(request);
        if (ObjectUtil.isNotNull(validateResult)) {
            return validateResult;
        }

        // 差集结果为 key 中存在但 otherKey 中不存在的元素
        Set<Object> values = redisTemplateService.sDifference(request.key(), request.otherKey());
        return ApiResult.ok(values);
    }

    private ApiResult<Long> validateSetValuesRequest(SetValuesRequest request) {
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

    private ApiResult<Set<Object>> validateSetOperationRequest(SetOperationRequest request) {
        if (ObjectUtil.isNull(request)) {
            return ApiResult.fail("请求参数不能为空");
        }
        if (StrUtil.isBlank(request.key())) {
            return ApiResult.fail("key 不能为空");
        }
        if (StrUtil.isBlank(request.otherKey())) {
            return ApiResult.fail("otherKey 不能为空");
        }
        return null;
    }

    public record SetValueRequest(String key, Object value) {
    }

    public record SetValuesRequest(String key, List<Object> values) {
    }

    public record SetPopRequest(String key, Long count) {
    }

    public record SetOperationRequest(String key, String otherKey) {
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