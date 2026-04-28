package local.ateng.java.redis.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import local.ateng.java.redis.service.RedisTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

/**
 * Redis Lua 脚本操作控制器。
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/redis/lua")
public class RedisLuaController {

    private final RedisTemplateService redisTemplateService;

    /**
     * 执行 Redis Lua 脚本并返回原始结果。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/lua/execute" \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "script":"return redis.call('get', KEYS[1])",
     *     "keys":["user:1"],
     *     "args":[]
     *   }'
     * }</pre>
     *
     * @param request Lua 脚本执行请求参数
     * @return Lua 脚本执行结果
     */
    @PostMapping("/execute")
    public ApiResult<Object> executeLua(@RequestBody LuaExecuteRequest request) {
        String errorMessage = validateLuaExecuteRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        Object result = redisTemplateService.executeLua(
                request.script(),
                buildKeys(request.keys()),
                buildArgs(request.args())
        );

        log.info("执行 Redis Lua 脚本，keyCount={}，argCount={}",
                getSize(request.keys()), getSize(request.args()));

        return ApiResult.ok(result);
    }

    /**
     * 执行 Redis Lua 脚本并按 Boolean 结果返回。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/lua/execute-boolean" \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "script":"return redis.call('exists', KEYS[1]) == 1",
     *     "keys":["user:1"],
     *     "args":[]
     *   }'
     * }</pre>
     *
     * @param request Lua 脚本执行请求参数
     * @return true 表示脚本执行结果为 true
     */
    @PostMapping("/execute-boolean")
    public ApiResult<Boolean> executeLuaAsBoolean(@RequestBody LuaExecuteRequest request) {
        String errorMessage = validateLuaExecuteRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        boolean result = redisTemplateService.executeLuaAsBoolean(
                request.script(),
                buildKeys(request.keys()),
                buildArgs(request.args())
        );

        log.info("执行 Redis Lua 脚本并返回 Boolean，keyCount={}，argCount={}",
                getSize(request.keys()), getSize(request.args()));

        return ApiResult.ok(result);
    }

    /**
     * 执行 Redis Lua 脚本并按 Long 结果返回。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/lua/execute-long" \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "script":"return redis.call('incrby', KEYS[1], ARGV[1])",
     *     "keys":["counter:order"],
     *     "args":[1]
     *   }'
     * }</pre>
     *
     * @param request Lua 脚本执行请求参数
     * @return Lua 脚本返回的 Long 值
     */
    @PostMapping("/execute-long")
    public ApiResult<Long> executeLuaAsLong(@RequestBody LuaExecuteRequest request) {
        String errorMessage = validateLuaExecuteRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        long result = redisTemplateService.executeLuaAsLong(
                request.script(),
                buildKeys(request.keys()),
                buildArgs(request.args())
        );

        log.info("执行 Redis Lua 脚本并返回 Long，keyCount={}，argCount={}",
                getSize(request.keys()), getSize(request.args()));

        return ApiResult.ok(result);
    }

    /**
     * 执行 Redis Lua 脚本并按 String 结果返回。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/lua/execute-string" \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "script":"return redis.call('get', KEYS[1])",
     *     "keys":["user:name:1"],
     *     "args":[]
     *   }'
     * }</pre>
     *
     * @param request Lua 脚本执行请求参数
     * @return Lua 脚本返回的字符串
     */
    @PostMapping("/execute-string")
    public ApiResult<String> executeLuaAsString(@RequestBody LuaExecuteRequest request) {
        String errorMessage = validateLuaExecuteRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        String result = redisTemplateService.executeLuaAsString(
                request.script(),
                buildKeys(request.keys()),
                buildArgs(request.args())
        );

        log.info("执行 Redis Lua 脚本并返回 String，keyCount={}，argCount={}",
                getSize(request.keys()), getSize(request.args()));

        return ApiResult.ok(result);
    }

    /**
     * 执行 Redis Lua 脚本并按 List 结果返回。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/lua/execute-list" \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "script":"return redis.call('mget', KEYS[1], KEYS[2], KEYS[3])",
     *     "keys":["user:1","user:2","user:3"],
     *     "args":[]
     *   }'
     * }</pre>
     *
     * @param request Lua 脚本执行请求参数
     * @return Lua 脚本返回的列表
     */
    @PostMapping("/execute-list")
    public ApiResult<List<Object>> executeLuaAsList(@RequestBody LuaExecuteRequest request) {
        String errorMessage = validateLuaExecuteRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        List<Object> result = redisTemplateService.executeLuaAsList(
                request.script(),
                buildKeys(request.keys()),
                buildArgs(request.args())
        );

        log.info("执行 Redis Lua 脚本并返回 List，keyCount={}，argCount={}，resultSize={}",
                getSize(request.keys()), getSize(request.args()), result.size());

        return ApiResult.ok(result);
    }

    /**
     * 从 classpath 资源文件读取 Redis Lua 脚本并执行，返回原始结果。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/lua/execute-resource" \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "resourceLocation":"lua/stock_decrease.lua",
     *     "keys":["stock:product:1001"],
     *     "args":[1]
     *   }'
     * }</pre>
     *
     * @param request Lua 资源脚本执行请求参数
     * @return Lua 脚本执行结果
     */
    @PostMapping("/execute-resource")
    public ApiResult<Object> executeLuaFromResource(@RequestBody LuaResourceExecuteRequest request) {
        String errorMessage = validateLuaResourceExecuteRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        Object result = redisTemplateService.executeLuaFromResource(
                request.resourceLocation(),
                buildKeys(request.keys()),
                buildArgs(request.args())
        );

        log.info("执行 Redis Lua 资源脚本，resourceLocation={}，keyCount={}，argCount={}",
                request.resourceLocation(), getSize(request.keys()), getSize(request.args()));

        return ApiResult.ok(result);
    }

    /**
     * 从 classpath 资源文件读取 Redis Lua 脚本并执行，按 Boolean 结果返回。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/lua/execute-resource-boolean" \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "resourceLocation":"lua/check_exists.lua",
     *     "keys":["user:1"],
     *     "args":[]
     *   }'
     * }</pre>
     *
     * @param request Lua 资源脚本执行请求参数
     * @return true 表示脚本执行结果为 true
     */
    @PostMapping("/execute-resource-boolean")
    public ApiResult<Boolean> executeLuaFromResourceAsBoolean(@RequestBody LuaResourceExecuteRequest request) {
        String errorMessage = validateLuaResourceExecuteRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        Boolean result = redisTemplateService.executeLuaFromResource(
                request.resourceLocation(),
                Boolean.class,
                buildKeys(request.keys()),
                buildArgs(request.args())
        );

        log.info("执行 Redis Lua 资源脚本并返回 Boolean，resourceLocation={}，keyCount={}，argCount={}",
                request.resourceLocation(), getSize(request.keys()), getSize(request.args()));

        return ApiResult.ok(Boolean.TRUE.equals(result));
    }

    /**
     * 从 classpath 资源文件读取 Redis Lua 脚本并执行，按 Long 结果返回。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/lua/execute-resource-long" \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "resourceLocation":"lua/increment_and_expire.lua",
     *     "keys":["counter:sms:18800000000"],
     *     "args":[1,60]
     *   }'
     * }</pre>
     *
     * @param request Lua 资源脚本执行请求参数
     * @return Lua 脚本返回的 Long 值
     */
    @PostMapping("/execute-resource-long")
    public ApiResult<Long> executeLuaFromResourceAsLong(@RequestBody LuaResourceExecuteRequest request) {
        String errorMessage = validateLuaResourceExecuteRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        Long result = redisTemplateService.executeLuaFromResource(
                request.resourceLocation(),
                Long.class,
                buildKeys(request.keys()),
                buildArgs(request.args())
        );

        log.info("执行 Redis Lua 资源脚本并返回 Long，resourceLocation={}，keyCount={}，argCount={}",
                request.resourceLocation(), getSize(request.keys()), getSize(request.args()));

        return ApiResult.ok(ObjectUtil.defaultIfNull(result, 0L));
    }

    /**
     * 从 classpath 资源文件读取 Redis Lua 脚本并执行，按 String 结果返回。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/lua/execute-resource-string" \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "resourceLocation":"lua/get_value.lua",
     *     "keys":["user:name:1"],
     *     "args":[]
     *   }'
     * }</pre>
     *
     * @param request Lua 资源脚本执行请求参数
     * @return Lua 脚本返回的字符串
     */
    @PostMapping("/execute-resource-string")
    public ApiResult<String> executeLuaFromResourceAsString(@RequestBody LuaResourceExecuteRequest request) {
        String errorMessage = validateLuaResourceExecuteRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        String result = redisTemplateService.executeLuaFromResource(
                request.resourceLocation(),
                String.class,
                buildKeys(request.keys()),
                buildArgs(request.args())
        );

        log.info("执行 Redis Lua 资源脚本并返回 String，resourceLocation={}，keyCount={}，argCount={}",
                request.resourceLocation(), getSize(request.keys()), getSize(request.args()));

        return ApiResult.ok(result);
    }

    /**
     * 使用 Lua 脚本比较并删除指定 Redis Key。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/lua/compare-delete" \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "key":"lock:order:1001",
     *     "expectedValue":"request-001"
     *   }'
     * }</pre>
     *
     * @param request Lua 比较删除请求参数
     * @return true 表示当前值与期望值一致并删除成功
     */
    @PostMapping("/compare-delete")
    public ApiResult<Boolean> compareAndDeleteByLua(@RequestBody LuaCompareDeleteRequest request) {
        String errorMessage = validateCompareDeleteRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        boolean result = redisTemplateService.compareAndDeleteByLua(request.key(), request.expectedValue());

        log.info("执行 Redis Lua 比较删除，key={}，result={}", request.key(), result);
        return ApiResult.ok(result);
    }

    /**
     * 使用 Lua 脚本比较并设置指定 Redis Key 的新值。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/lua/compare-set" \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "key":"config:version",
     *     "expectedValue":"v1",
     *     "newValue":"v2",
     *     "timeoutSeconds":3600
     *   }'
     * }</pre>
     *
     * 不设置过期时间示例：
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/lua/compare-set" \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "key":"config:version",
     *     "expectedValue":"v1",
     *     "newValue":"v2"
     *   }'
     * }</pre>
     *
     * @param request Lua 比较设置请求参数
     * @return true 表示当前值与期望值一致并设置成功
     */
    @PostMapping("/compare-set")
    public ApiResult<Boolean> compareAndSetByLua(@RequestBody LuaCompareSetRequest request) {
        String errorMessage = validateCompareSetRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        boolean result;
        if (ObjectUtil.isNull(request.timeoutSeconds())) {
            result = redisTemplateService.compareAndSetByLua(
                    request.key(),
                    request.expectedValue(),
                    request.newValue()
            );
        } else {
            result = redisTemplateService.compareAndSetByLua(
                    request.key(),
                    request.expectedValue(),
                    request.newValue(),
                    Duration.ofSeconds(request.timeoutSeconds())
            );
        }

        log.info("执行 Redis Lua 比较设置，key={}，hasTimeout={}，result={}",
                request.key(), ObjectUtil.isNotNull(request.timeoutSeconds()), result);

        return ApiResult.ok(result);
    }

    /**
     * 使用 Lua 脚本对指定 Redis Key 自增并设置过期时间。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/lua/increment-expire" \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "key":"limit:sms:18800000000",
     *     "delta":1,
     *     "timeoutSeconds":60
     *   }'
     * }</pre>
     *
     * @param request Lua 自增并设置过期时间请求参数
     * @return 自增后的值
     */
    @PostMapping("/increment-expire")
    public ApiResult<Long> incrementAndExpireByLua(@RequestBody LuaIncrementExpireRequest request) {
        String errorMessage = validateIncrementExpireRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        long result = redisTemplateService.incrementAndExpireByLua(
                request.key(),
                request.delta(),
                Duration.ofSeconds(request.timeoutSeconds())
        );

        log.info("执行 Redis Lua 自增并设置过期时间，key={}，delta={}，timeoutSeconds={}，result={}",
                request.key(), request.delta(), request.timeoutSeconds(), result);

        return ApiResult.ok(result);
    }

    private String validateLuaExecuteRequest(LuaExecuteRequest request) {
        if (ObjectUtil.isNull(request)) {
            return "请求参数不能为空";
        }
        if (StrUtil.isBlank(request.script())) {
            return "script 不能为空";
        }
        return null;
    }

    private String validateLuaResourceExecuteRequest(LuaResourceExecuteRequest request) {
        if (ObjectUtil.isNull(request)) {
            return "请求参数不能为空";
        }
        if (StrUtil.isBlank(request.resourceLocation())) {
            return "resourceLocation 不能为空";
        }
        return null;
    }

    private String validateCompareDeleteRequest(LuaCompareDeleteRequest request) {
        if (ObjectUtil.isNull(request)) {
            return "请求参数不能为空";
        }
        if (StrUtil.isBlank(request.key())) {
            return "key 不能为空";
        }
        if (ObjectUtil.isNull(request.expectedValue())) {
            return "expectedValue 不能为空";
        }
        return null;
    }

    private String validateCompareSetRequest(LuaCompareSetRequest request) {
        if (ObjectUtil.isNull(request)) {
            return "请求参数不能为空";
        }
        if (StrUtil.isBlank(request.key())) {
            return "key 不能为空";
        }
        if (ObjectUtil.isNull(request.expectedValue())) {
            return "expectedValue 不能为空";
        }
        if (ObjectUtil.isNull(request.newValue())) {
            return "newValue 不能为空";
        }
        if (ObjectUtil.isNotNull(request.timeoutSeconds()) && request.timeoutSeconds() <= 0) {
            return "timeoutSeconds 必须大于 0";
        }
        return null;
    }

    private String validateIncrementExpireRequest(LuaIncrementExpireRequest request) {
        if (ObjectUtil.isNull(request)) {
            return "请求参数不能为空";
        }
        if (StrUtil.isBlank(request.key())) {
            return "key 不能为空";
        }
        if (ObjectUtil.isNull(request.delta()) || request.delta() == 0L) {
            return "delta 不能为 0";
        }
        if (ObjectUtil.isNull(request.timeoutSeconds()) || request.timeoutSeconds() <= 0) {
            return "timeoutSeconds 必须大于 0";
        }
        return null;
    }

    private List<String> buildKeys(List<String> keys) {
        if (CollUtil.isEmpty(keys)) {
            return List.of();
        }

        // 过滤空 Key，避免 Lua 执行时 KEYS 下标和调用方预期不一致
        return keys.stream()
                .filter(StrUtil::isNotBlank)
                .toList();
    }

    private Object[] buildArgs(List<Object> args) {
        if (CollUtil.isEmpty(args)) {
            return new Object[0];
        }

        // ARGV 支持字符串、数字等 Redis 可序列化对象，复杂对象依赖 RedisTemplate 的 valueSerializer
        return args.stream()
                .filter(ObjectUtil::isNotNull)
                .toArray();
    }

    private int getSize(List<?> list) {
        return ObjectUtil.isNull(list) ? 0 : list.size();
    }

    public record LuaExecuteRequest(String script, List<String> keys, List<Object> args) {
    }

    public record LuaResourceExecuteRequest(String resourceLocation, List<String> keys, List<Object> args) {
    }

    public record LuaCompareDeleteRequest(String key, Object expectedValue) {
    }

    public record LuaCompareSetRequest(String key, Object expectedValue, Object newValue, Long timeoutSeconds) {
    }

    public record LuaIncrementExpireRequest(String key, Long delta, Long timeoutSeconds) {
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