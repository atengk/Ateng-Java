package local.ateng.java.redis.controller;

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
import java.util.Optional;

/**
 * Redis 分布式锁操作控制器。
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/redis/lock")
public class RedisLockController {

    private final RedisTemplateService redisTemplateService;

    /**
     * 尝试获取 Redis 分布式锁，不等待锁释放。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/lock/try-lock" \
     *   -H "Content-Type: application/json" \
     *   -d '{"lockKey":"lock:order:1001","lockValue":"request-001","leaseSeconds":30}'
     * }</pre>
     *
     * @param request 加锁请求参数
     * @return true 表示获取锁成功，false 表示锁已存在或获取失败
     */
    @PostMapping("/try-lock")
    public ApiResult<Boolean> tryLock(@RequestBody LockRequest request) {
        String errorMessage = validateLockRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        boolean locked = redisTemplateService.tryLock(
                request.lockKey(),
                request.lockValue(),
                Duration.ofSeconds(request.leaseSeconds())
        );

        log.info("尝试获取 Redis 分布式锁，lockKey={}，locked={}", request.lockKey(), locked);
        return ApiResult.ok(locked);
    }

    /**
     * 在指定等待时间内尝试获取 Redis 分布式锁。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/lock/try-lock-wait" \
     *   -H "Content-Type: application/json" \
     *   -d '{"lockKey":"lock:order:1001","lockValue":"request-002","waitSeconds":5,"leaseSeconds":30}'
     * }</pre>
     *
     * @param request 等待加锁请求参数
     * @return true 表示获取锁成功，false 表示等待超时或获取失败
     */
    @PostMapping("/try-lock-wait")
    public ApiResult<Boolean> tryLockWithWait(@RequestBody WaitLockRequest request) {
        String errorMessage = validateWaitLockRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        boolean locked = redisTemplateService.tryLock(
                request.lockKey(),
                request.lockValue(),
                Duration.ofSeconds(request.waitSeconds()),
                Duration.ofSeconds(request.leaseSeconds())
        );

        log.info("等待获取 Redis 分布式锁，lockKey={}，waitSeconds={}，locked={}",
                request.lockKey(), request.waitSeconds(), locked);

        return ApiResult.ok(locked);
    }

    /**
     * 释放 Redis 分布式锁。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/lock/unlock" \
     *   -H "Content-Type: application/json" \
     *   -d '{"lockKey":"lock:order:1001","lockValue":"request-001"}'
     * }</pre>
     *
     * @param request 解锁请求参数，lockValue 必须与加锁时一致
     * @return true 表示释放成功，false 表示锁不存在、锁值不一致或释放失败
     */
    @PostMapping("/unlock")
    public ApiResult<Boolean> unlock(@RequestBody UnlockRequest request) {
        if (ObjectUtil.isNull(request)) {
            return ApiResult.fail("请求参数不能为空");
        }
        if (StrUtil.isBlank(request.lockKey())) {
            return ApiResult.fail("lockKey 不能为空");
        }
        if (StrUtil.isBlank(request.lockValue())) {
            return ApiResult.fail("lockValue 不能为空");
        }

        boolean unlocked = redisTemplateService.unlock(request.lockKey(), request.lockValue());

        log.info("释放 Redis 分布式锁，lockKey={}，unlocked={}", request.lockKey(), unlocked);
        return ApiResult.ok(unlocked);
    }

    /**
     * 尝试获取 Redis 分布式锁并执行无返回值任务，获取锁失败时不抛出异常。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/lock/try-execute" \
     *   -H "Content-Type: application/json" \
     *   -d '{"lockKey":"lock:task:sync-order","leaseSeconds":30,"taskName":"同步订单任务","sleepMillis":1000}'
     * }</pre>
     *
     * 带等待时间示例：
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/lock/try-execute" \
     *   -H "Content-Type: application/json" \
     *   -d '{"lockKey":"lock:task:sync-order","waitSeconds":5,"leaseSeconds":30,"taskName":"同步订单任务","sleepMillis":1000}'
     * }</pre>
     *
     * @param request 带锁执行任务请求参数
     * @return true 表示获取锁并执行任务成功，false 表示获取锁失败或执行失败
     */
    @PostMapping("/try-execute")
    public ApiResult<Boolean> tryExecuteWithLock(@RequestBody LockTaskRequest request) {
        String errorMessage = validateLockTaskRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        boolean executed;
        if (ObjectUtil.isNull(request.waitSeconds())) {
            executed = redisTemplateService.tryExecuteWithLock(
                    request.lockKey(),
                    Duration.ofSeconds(request.leaseSeconds()),
                    () -> runMockTask(request)
            );
        } else {
            executed = redisTemplateService.tryExecuteWithLock(
                    request.lockKey(),
                    Duration.ofSeconds(request.waitSeconds()),
                    Duration.ofSeconds(request.leaseSeconds()),
                    () -> runMockTask(request)
            );
        }

        log.info("尝试带锁执行 Runnable 任务，lockKey={}，taskName={}，executed={}",
                request.lockKey(), request.taskName(), executed);

        return ApiResult.ok(executed);
    }

    /**
     * 尝试获取 Redis 分布式锁并执行有返回值任务，获取锁失败时返回未执行状态。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/lock/try-execute-supplier" \
     *   -H "Content-Type: application/json" \
     *   -d '{"lockKey":"lock:task:calculate","leaseSeconds":30,"taskName":"计算任务","sleepMillis":1000,"result":{"success":true,"count":10}}'
     * }</pre>
     *
     * 带等待时间示例：
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/lock/try-execute-supplier" \
     *   -H "Content-Type: application/json" \
     *   -d '{"lockKey":"lock:task:calculate","waitSeconds":5,"leaseSeconds":30,"taskName":"计算任务","sleepMillis":1000,"result":"OK"}'
     * }</pre>
     *
     * @param request 带锁执行 Supplier 任务请求参数
     * @return 任务执行状态和任务返回值
     */
    @PostMapping("/try-execute-supplier")
    public ApiResult<LockTaskResponse> tryExecuteSupplierWithLock(@RequestBody LockSupplierTaskRequest request) {
        String errorMessage = validateLockSupplierTaskRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        Optional<Object> optionalResult;
        if (ObjectUtil.isNull(request.waitSeconds())) {
            optionalResult = redisTemplateService.tryExecuteWithLock(
                    request.lockKey(),
                    Duration.ofSeconds(request.leaseSeconds()),
                    () -> runMockSupplierTask(request)
            );
        } else {
            optionalResult = redisTemplateService.tryExecuteWithLock(
                    request.lockKey(),
                    Duration.ofSeconds(request.waitSeconds()),
                    Duration.ofSeconds(request.leaseSeconds()),
                    () -> runMockSupplierTask(request)
            );
        }

        LockTaskResponse response = new LockTaskResponse(optionalResult.isPresent(), optionalResult.orElse(null));

        log.info("尝试带锁执行 Supplier 任务，lockKey={}，taskName={}，executed={}",
                request.lockKey(), request.taskName(), response.executed());

        return ApiResult.ok(response);
    }

    /**
     * 获取 Redis 分布式锁并执行无返回值任务，获取锁失败时返回失败信息。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/lock/execute" \
     *   -H "Content-Type: application/json" \
     *   -d '{"lockKey":"lock:task:stock-deduct","leaseSeconds":30,"taskName":"库存扣减任务","sleepMillis":1000}'
     * }</pre>
     *
     * 带等待时间示例：
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/lock/execute" \
     *   -H "Content-Type: application/json" \
     *   -d '{"lockKey":"lock:task:stock-deduct","waitSeconds":5,"leaseSeconds":30,"taskName":"库存扣减任务","sleepMillis":1000}'
     * }</pre>
     *
     * @param request 带锁执行任务请求参数
     * @return true 表示任务执行成功；获取锁失败时返回失败信息
     */
    @PostMapping("/execute")
    public ApiResult<Boolean> executeWithLock(@RequestBody LockTaskRequest request) {
        String errorMessage = validateLockTaskRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        try {
            if (ObjectUtil.isNull(request.waitSeconds())) {
                redisTemplateService.executeWithLock(
                        request.lockKey(),
                        Duration.ofSeconds(request.leaseSeconds()),
                        () -> runMockTask(request)
                );
            } else {
                redisTemplateService.executeWithLock(
                        request.lockKey(),
                        Duration.ofSeconds(request.waitSeconds()),
                        Duration.ofSeconds(request.leaseSeconds()),
                        () -> runMockTask(request)
                );
            }

            log.info("带锁执行 Runnable 任务成功，lockKey={}，taskName={}", request.lockKey(), request.taskName());
            return ApiResult.ok(true);
        } catch (IllegalStateException e) {
            log.warn("带锁执行 Runnable 任务失败，lockKey={}，taskName={}", request.lockKey(), request.taskName(), e);
            return ApiResult.fail(e.getMessage());
        }
    }

    /**
     * 获取 Redis 分布式锁并执行有返回值任务，获取锁失败时返回失败信息。
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/lock/execute-supplier" \
     *   -H "Content-Type: application/json" \
     *   -d '{"lockKey":"lock:task:query-report","leaseSeconds":30,"taskName":"报表查询任务","sleepMillis":1000,"result":{"total":100,"status":"DONE"}}'
     * }</pre>
     *
     * 带等待时间示例：
     *
     * <pre>{@code
     * curl -X POST "http://localhost:8080/api/redis/lock/execute-supplier" \
     *   -H "Content-Type: application/json" \
     *   -d '{"lockKey":"lock:task:query-report","waitSeconds":5,"leaseSeconds":30,"taskName":"报表查询任务","sleepMillis":1000,"result":"SUCCESS"}'
     * }</pre>
     *
     * @param request 带锁执行 Supplier 任务请求参数
     * @return 任务返回值；获取锁失败时返回失败信息
     */
    @PostMapping("/execute-supplier")
    public ApiResult<Object> executeSupplierWithLock(@RequestBody LockSupplierTaskRequest request) {
        String errorMessage = validateLockSupplierTaskRequest(request);
        if (StrUtil.isNotBlank(errorMessage)) {
            return ApiResult.fail(errorMessage);
        }

        try {
            Object result;
            if (ObjectUtil.isNull(request.waitSeconds())) {
                result = redisTemplateService.executeWithLock(
                        request.lockKey(),
                        Duration.ofSeconds(request.leaseSeconds()),
                        () -> runMockSupplierTask(request)
                );
            } else {
                result = redisTemplateService.executeWithLock(
                        request.lockKey(),
                        Duration.ofSeconds(request.waitSeconds()),
                        Duration.ofSeconds(request.leaseSeconds()),
                        () -> runMockSupplierTask(request)
                );
            }

            log.info("带锁执行 Supplier 任务成功，lockKey={}，taskName={}", request.lockKey(), request.taskName());
            return ApiResult.ok(result);
        } catch (IllegalStateException e) {
            log.warn("带锁执行 Supplier 任务失败，lockKey={}，taskName={}", request.lockKey(), request.taskName(), e);
            return ApiResult.fail(e.getMessage());
        }
    }

    private void runMockTask(LockTaskRequest request) {
        // 这里模拟 Runnable 任务执行，实际业务中可以替换为订单处理、库存扣减、定时补偿等逻辑
        log.info("开始执行 Redis 分布式锁 Runnable 任务，taskName={}，sleepMillis={}",
                request.taskName(), request.sleepMillis());

        sleepQuietly(request.sleepMillis());

        log.info("完成执行 Redis 分布式锁 Runnable 任务，taskName={}", request.taskName());
    }

    private Object runMockSupplierTask(LockSupplierTaskRequest request) {
        // 这里模拟 Supplier 任务执行，实际业务中可以替换为查询、计算、生成结果等逻辑
        log.info("开始执行 Redis 分布式锁 Supplier 任务，taskName={}，sleepMillis={}",
                request.taskName(), request.sleepMillis());

        sleepQuietly(request.sleepMillis());

        log.info("完成执行 Redis 分布式锁 Supplier 任务，taskName={}，result={}",
                request.taskName(), request.result());

        return request.result();
    }

    private void sleepQuietly(Long sleepMillis) {
        if (ObjectUtil.isNull(sleepMillis) || sleepMillis <= 0) {
            return;
        }

        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Redis 分布式锁模拟任务被中断", e);
        }
    }

    private String validateLockRequest(LockRequest request) {
        if (ObjectUtil.isNull(request)) {
            return "请求参数不能为空";
        }
        if (StrUtil.isBlank(request.lockKey())) {
            return "lockKey 不能为空";
        }
        if (StrUtil.isBlank(request.lockValue())) {
            return "lockValue 不能为空";
        }
        if (ObjectUtil.isNull(request.leaseSeconds()) || request.leaseSeconds() <= 0) {
            return "leaseSeconds 必须大于 0";
        }
        return null;
    }

    private String validateWaitLockRequest(WaitLockRequest request) {
        if (ObjectUtil.isNull(request)) {
            return "请求参数不能为空";
        }
        if (StrUtil.isBlank(request.lockKey())) {
            return "lockKey 不能为空";
        }
        if (StrUtil.isBlank(request.lockValue())) {
            return "lockValue 不能为空";
        }
        if (ObjectUtil.isNull(request.waitSeconds()) || request.waitSeconds() <= 0) {
            return "waitSeconds 必须大于 0";
        }
        if (ObjectUtil.isNull(request.leaseSeconds()) || request.leaseSeconds() <= 0) {
            return "leaseSeconds 必须大于 0";
        }
        return null;
    }

    private String validateLockTaskRequest(LockTaskRequest request) {
        if (ObjectUtil.isNull(request)) {
            return "请求参数不能为空";
        }
        if (StrUtil.isBlank(request.lockKey())) {
            return "lockKey 不能为空";
        }
        if (ObjectUtil.isNull(request.leaseSeconds()) || request.leaseSeconds() <= 0) {
            return "leaseSeconds 必须大于 0";
        }
        if (ObjectUtil.isNotNull(request.waitSeconds()) && request.waitSeconds() <= 0) {
            return "waitSeconds 必须大于 0";
        }
        if (ObjectUtil.isNotNull(request.sleepMillis()) && request.sleepMillis() < 0) {
            return "sleepMillis 不能小于 0";
        }
        return null;
    }

    private String validateLockSupplierTaskRequest(LockSupplierTaskRequest request) {
        if (ObjectUtil.isNull(request)) {
            return "请求参数不能为空";
        }
        if (StrUtil.isBlank(request.lockKey())) {
            return "lockKey 不能为空";
        }
        if (ObjectUtil.isNull(request.leaseSeconds()) || request.leaseSeconds() <= 0) {
            return "leaseSeconds 必须大于 0";
        }
        if (ObjectUtil.isNotNull(request.waitSeconds()) && request.waitSeconds() <= 0) {
            return "waitSeconds 必须大于 0";
        }
        if (ObjectUtil.isNotNull(request.sleepMillis()) && request.sleepMillis() < 0) {
            return "sleepMillis 不能小于 0";
        }
        return null;
    }

    public record LockRequest(String lockKey, String lockValue, Long leaseSeconds) {
    }

    public record WaitLockRequest(String lockKey, String lockValue, Long waitSeconds, Long leaseSeconds) {
    }

    public record UnlockRequest(String lockKey, String lockValue) {
    }

    public record LockTaskRequest(String lockKey, Long waitSeconds, Long leaseSeconds, String taskName,
                                  Long sleepMillis) {
    }

    public record LockSupplierTaskRequest(String lockKey, Long waitSeconds, Long leaseSeconds, String taskName,
                                          Long sleepMillis, Object result) {
    }

    public record LockTaskResponse(Boolean executed, Object result) {
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