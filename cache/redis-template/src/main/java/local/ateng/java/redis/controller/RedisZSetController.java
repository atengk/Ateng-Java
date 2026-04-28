package local.ateng.java.redis.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import local.ateng.java.redis.service.RedisTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * Redis ZSet 操作控制器。
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/redis/zset")
public class RedisZSetController {

    private final RedisTemplateService redisTemplateService;

    /**
     * 向 Redis ZSet 中添加一个元素及其分数。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/zset/add" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"rank:user:score","value":{"userId":1,"name":"Ateng"},"score":98.5}'
     * }</pre>
     *
     * 添加字符串元素示例：
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/zset/add" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"rank:user:score","value":"user:1","score":98.5}'
     * }</pre>
     *
     * @param request ZSet 添加元素请求参数
     * @return true 表示添加成功，false 表示添加失败
     */
    @PostMapping("/add")
    public ApiResult<Boolean> zAdd(@RequestBody ZSetAddRequest request) {
        String errorMessage = validateZSetAddRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        boolean result = redisTemplateService.zAdd(request.key(), request.value(), request.score());

        log.info("Redis ZSet 添加元素，key={}，score={}，result={}", request.key(), request.score(), result);
        return ApiResult.ok(result);
    }

    /**
     * 从 Redis ZSet 中删除一个或多个元素。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/zset/remove" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"rank:user:score","values":["user:1","user:2"]}'
     * }</pre>
     *
     * @param request ZSet 批量元素请求参数
     * @return 成功删除的元素数量
     */
    @PostMapping("/remove")
    public ApiResult<Long> zRemove(@RequestBody ZSetValuesRequest request) {
        String errorMessage = validateZSetValuesRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        long count = redisTemplateService.zRemove(request.key(), request.values().toArray());

        log.info("Redis ZSet 删除元素，key={}，valueCount={}，removeCount={}",
                request.key(), request.values().size(), count);

        return ApiResult.ok(count);
    }

    /**
     * 获取 Redis ZSet 中指定元素的分数。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/zset/score" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"rank:user:score","value":"user:1"}'
     * }</pre>
     *
     * @param request ZSet 单元素请求参数
     * @return 元素分数；元素不存在时返回 null
     */
    @PostMapping("/score")
    public ApiResult<Double> zScore(@RequestBody ZSetValueRequest request) {
        String errorMessage = validateZSetValueRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        Double score = redisTemplateService.zScore(request.key(), request.value());
        return ApiResult.ok(score);
    }

    /**
     * 获取 Redis ZSet 中指定元素的正序排名。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/zset/rank" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"rank:user:score","value":"user:1"}'
     * }</pre>
     *
     * @param request ZSet 单元素请求参数
     * @return 元素正序排名，排名从 0 开始；元素不存在时返回 null
     */
    @PostMapping("/rank")
    public ApiResult<Long> zRank(@RequestBody ZSetValueRequest request) {
        String errorMessage = validateZSetValueRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        Long rank = redisTemplateService.zRank(request.key(), request.value());
        return ApiResult.ok(rank);
    }

    /**
     * 获取 Redis ZSet 中指定元素的倒序排名。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/zset/reverse-rank" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"rank:user:score","value":"user:1"}'
     * }</pre>
     *
     * @param request ZSet 单元素请求参数
     * @return 元素倒序排名，排名从 0 开始；元素不存在时返回 null
     */
    @PostMapping("/reverse-rank")
    public ApiResult<Long> zReverseRank(@RequestBody ZSetValueRequest request) {
        String errorMessage = validateZSetValueRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        Long rank = redisTemplateService.zReverseRank(request.key(), request.value());
        return ApiResult.ok(rank);
    }

    /**
     * 获取 Redis ZSet 的元素数量。
     *
     * <pre>{@code
     * curl -X GET "http://localhost:8080/api/redis/zset/size?key=rank:user:score"
     * }</pre>
     *
     * @param key Redis Key
     * @return ZSet 元素数量；Key 不存在时返回 0
     */
    @GetMapping("/size")
    public ApiResult<Long> zSize(@RequestParam String key) {
        if (StrUtil.isBlank(key)) {
            return ApiResult.fail("key 不能为空");
        }

        long size = redisTemplateService.zSize(key);
        return ApiResult.ok(size);
    }

    /**
     * 获取 Redis ZSet 中指定分数区间内的元素数量。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/zset/count" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"rank:user:score","min":60,"max":100}'
     * }</pre>
     *
     * @param request ZSet 分数区间请求参数
     * @return 指定分数区间内的元素数量
     */
    @PostMapping("/count")
    public ApiResult<Long> zCount(@RequestBody ZSetScoreRangeRequest request) {
        String errorMessage = validateScoreRangeRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        long count = redisTemplateService.zCount(request.key(), request.min(), request.max());
        return ApiResult.ok(count);
    }

    /**
     * 按正序排名范围获取 Redis ZSet 元素。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/zset/range" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"rank:user:score","start":0,"end":9}'
     * }</pre>
     *
     * 获取全部元素示例：
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/zset/range" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"rank:user:score","start":0,"end":-1}'
     * }</pre>
     *
     * @param request ZSet 排名范围请求参数
     * @return 指定排名范围内的元素集合
     */
    @PostMapping("/range")
    public ApiResult<Set<Object>> zRange(@RequestBody ZSetRangeRequest request) {
        String errorMessage = validateRangeRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        Set<Object> values = redisTemplateService.zRange(request.key(), request.start(), request.end());
        return ApiResult.ok(values);
    }

    /**
     * 按倒序排名范围获取 Redis ZSet 元素。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/zset/reverse-range" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"rank:user:score","start":0,"end":9}'
     * }</pre>
     *
     * @param request ZSet 排名范围请求参数
     * @return 指定倒序排名范围内的元素集合
     */
    @PostMapping("/reverse-range")
    public ApiResult<Set<Object>> zReverseRange(@RequestBody ZSetRangeRequest request) {
        String errorMessage = validateRangeRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        Set<Object> values = redisTemplateService.zReverseRange(request.key(), request.start(), request.end());
        return ApiResult.ok(values);
    }

    /**
     * 按分数区间获取 Redis ZSet 元素。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/zset/range-by-score" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"rank:user:score","min":60,"max":100}'
     * }</pre>
     *
     * @param request ZSet 分数区间请求参数
     * @return 指定分数区间内的元素集合
     */
    @PostMapping("/range-by-score")
    public ApiResult<Set<Object>> zRangeByScore(@RequestBody ZSetScoreRangeRequest request) {
        String errorMessage = validateScoreRangeRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        Set<Object> values = redisTemplateService.zRangeByScore(request.key(), request.min(), request.max());
        return ApiResult.ok(values);
    }

    /**
     * 按分数区间分页获取 Redis ZSet 元素。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/zset/range-by-score-page" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"rank:user:score","min":60,"max":100,"offset":0,"count":10}'
     * }</pre>
     *
     * @param request ZSet 分数区间分页请求参数
     * @return 指定分数区间内分页后的元素集合
     */
    @PostMapping("/range-by-score-page")
    public ApiResult<Set<Object>> zRangeByScorePage(@RequestBody ZSetScorePageRequest request) {
        String errorMessage = validateScorePageRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        // 按 score 区间分页查询，适合排行榜、时间线、延迟队列分页查看等场景
        Set<Object> values = redisTemplateService.zRangeByScore(
                request.key(),
                request.min(),
                request.max(),
                request.offset(),
                request.count()
        );

        return ApiResult.ok(values);
    }

    /**
     * 按正序排名范围获取 Redis ZSet 元素及其分数。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/zset/range-with-scores" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"rank:user:score","start":0,"end":9}'
     * }</pre>
     *
     * @param request ZSet 排名范围请求参数
     * @return 元素和分数列表
     */
    @PostMapping("/range-with-scores")
    public ApiResult<List<ZSetTupleResponse>> zRangeWithScores(@RequestBody ZSetRangeRequest request) {
        String errorMessage = validateRangeRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        Set<ZSetOperations.TypedTuple<Object>> tuples = redisTemplateService.zRangeWithScores(
                request.key(),
                request.start(),
                request.end()
        );

        // TypedTuple 直接返回给前端可读性较差，这里转换为 value + score 的稳定结构
        List<ZSetTupleResponse> values = tuples.stream()
                .filter(ObjectUtil::isNotNull)
                .map(tuple -> new ZSetTupleResponse(tuple.getValue(), tuple.getScore()))
                .toList();

        return ApiResult.ok(values);
    }

    /**
     * 按排名范围删除 Redis ZSet 元素。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/zset/remove-range" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"rank:user:score","start":100,"end":-1}'
     * }</pre>
     *
     * 删除前 10 名示例：
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/zset/remove-range" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"rank:user:score","start":0,"end":9}'
     * }</pre>
     *
     * @param request ZSet 排名范围请求参数
     * @return 成功删除的元素数量
     */
    @PostMapping("/remove-range")
    public ApiResult<Long> zRemoveRange(@RequestBody ZSetRangeRequest request) {
        String errorMessage = validateRangeRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        long count = redisTemplateService.zRemoveRange(request.key(), request.start(), request.end());

        log.info("Redis ZSet 按排名区间删除元素，key={}，start={}，end={}，removeCount={}",
                request.key(), request.start(), request.end(), count);

        return ApiResult.ok(count);
    }

    /**
     * 按分数区间删除 Redis ZSet 元素。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/zset/remove-range-by-score" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"rank:user:score","min":0,"max":59}'
     * }</pre>
     *
     * @param request ZSet 分数区间请求参数
     * @return 成功删除的元素数量
     */
    @PostMapping("/remove-range-by-score")
    public ApiResult<Long> zRemoveRangeByScore(@RequestBody ZSetScoreRangeRequest request) {
        String errorMessage = validateScoreRangeRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        long count = redisTemplateService.zRemoveRangeByScore(request.key(), request.min(), request.max());

        log.info("Redis ZSet 按分数区间删除元素，key={}，min={}，max={}，removeCount={}",
                request.key(), request.min(), request.max(), count);

        return ApiResult.ok(count);
    }

    /**
     * 对 Redis ZSet 中指定元素的分数执行递增。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/zset/increment-score" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"rank:user:score","value":"user:1","delta":5.5}'
     * }</pre>
     *
     * 递减分数示例：
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/zset/increment-score" \
     *   -H "Content-Type: application/json" \
     *   -d '{"key":"rank:user:score","value":"user:1","delta":-2}'
     * }</pre>
     *
     * @param request ZSet 分数递增请求参数
     * @return 递增后的元素分数
     */
    @PostMapping("/increment-score")
    public ApiResult<Double> zIncrementScore(@RequestBody ZSetIncrementScoreRequest request) {
        if (ObjectUtil.isNull(request)) {
            return ApiResult.fail("请求参数不能为空");
        }
        if (StrUtil.isBlank(request.key())) {
            return ApiResult.fail("key 不能为空");
        }
        if (ObjectUtil.isNull(request.value())) {
            return ApiResult.fail("value 不能为空");
        }
        if (ObjectUtil.isNull(request.delta()) || request.delta() == 0D) {
            return ApiResult.fail("delta 不能为 0");
        }

        Double score = redisTemplateService.zIncrementScore(request.key(), request.value(), request.delta());

        log.info("Redis ZSet 分数递增，key={}，delta={}，score={}", request.key(), request.delta(), score);
        return ApiResult.ok(score);
    }

    private String validateZSetAddRequest(ZSetAddRequest request) {
        if (ObjectUtil.isNull(request)) {
            return "请求参数不能为空";
        }
        if (StrUtil.isBlank(request.key())) {
            return "key 不能为空";
        }
        if (ObjectUtil.isNull(request.value())) {
            return "value 不能为空";
        }
        if (ObjectUtil.isNull(request.score())) {
            return "score 不能为空";
        }
        return null;
    }

    private String validateZSetValueRequest(ZSetValueRequest request) {
        if (ObjectUtil.isNull(request)) {
            return "请求参数不能为空";
        }
        if (StrUtil.isBlank(request.key())) {
            return "key 不能为空";
        }
        if (ObjectUtil.isNull(request.value())) {
            return "value 不能为空";
        }
        return null;
    }

    private String validateZSetValuesRequest(ZSetValuesRequest request) {
        if (ObjectUtil.isNull(request)) {
            return "请求参数不能为空";
        }
        if (StrUtil.isBlank(request.key())) {
            return "key 不能为空";
        }
        if (CollUtil.isEmpty(request.values())) {
            return "values 不能为空";
        }
        return null;
    }

    private String validateRangeRequest(ZSetRangeRequest request) {
        if (ObjectUtil.isNull(request)) {
            return "请求参数不能为空";
        }
        if (StrUtil.isBlank(request.key())) {
            return "key 不能为空";
        }
        if (ObjectUtil.isNull(request.start())) {
            return "start 不能为空";
        }
        if (ObjectUtil.isNull(request.end())) {
            return "end 不能为空";
        }
        return null;
    }

    private String validateScoreRangeRequest(ZSetScoreRangeRequest request) {
        if (ObjectUtil.isNull(request)) {
            return "请求参数不能为空";
        }
        if (StrUtil.isBlank(request.key())) {
            return "key 不能为空";
        }
        if (ObjectUtil.isNull(request.min())) {
            return "min 不能为空";
        }
        if (ObjectUtil.isNull(request.max())) {
            return "max 不能为空";
        }
        if (request.min() > request.max()) {
            return "min 不能大于 max";
        }
        return null;
    }

    private String validateScorePageRequest(ZSetScorePageRequest request) {
        if (ObjectUtil.isNull(request)) {
            return "请求参数不能为空";
        }
        if (StrUtil.isBlank(request.key())) {
            return "key 不能为空";
        }
        if (ObjectUtil.isNull(request.min())) {
            return "min 不能为空";
        }
        if (ObjectUtil.isNull(request.max())) {
            return "max 不能为空";
        }
        if (request.min() > request.max()) {
            return "min 不能大于 max";
        }
        if (ObjectUtil.isNull(request.offset()) || request.offset() < 0) {
            return "offset 必须大于等于 0";
        }
        if (ObjectUtil.isNull(request.count()) || request.count() <= 0) {
            return "count 必须大于 0";
        }
        return null;
    }

    public record ZSetAddRequest(String key, Object value, Double score) {
    }

    public record ZSetValueRequest(String key, Object value) {
    }

    public record ZSetValuesRequest(String key, List<Object> values) {
    }

    public record ZSetRangeRequest(String key, Long start, Long end) {
    }

    public record ZSetScoreRangeRequest(String key, Double min, Double max) {
    }

    public record ZSetScorePageRequest(String key, Double min, Double max, Long offset, Long count) {
    }

    public record ZSetIncrementScoreRequest(String key, Object value, Double delta) {
    }

    public record ZSetTupleResponse(Object value, Double score) {
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