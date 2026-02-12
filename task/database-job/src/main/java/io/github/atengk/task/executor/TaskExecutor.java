package io.github.atengk.task.executor;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.task.entity.TaskJob;
import io.github.atengk.task.entity.TaskJobLog;
import io.github.atengk.task.enums.TaskExecuteStatusEnum;
import io.github.atengk.task.service.ITaskJobLogService;
import io.github.atengk.task.service.ITaskJobService;
import io.github.atengk.task.util.ReflectInvokeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据库驱动任务执行器
 * <p>
 * 设计目标：
 * 1. 支持一次性任务
 * 2. 支持异步补偿任务
 * 3. MyBatis-Plus 乐观锁控制并发
 * 4. lock_time 防死锁恢复
 * 5. 自动重试
 * 6. 成功仅标记成功，不删除
 * 7. 无长事务
 * <p>
 * 适用于中小型业务系统
 *
 * @author Ateng
 * @since 2026-02-12
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TaskExecutor {

    /**
     * 锁超时时间（分钟）
     */
    private static final int LOCK_TIMEOUT_MINUTES = 5;

    /**
     * 日志前缀
     */
    private static final String LOG_PREFIX = "[TaskExecutor]";

    private final ITaskJobService taskJobService;
    private final ITaskJobLogService taskJobLogService;

    /* ========================================================= */
    /* ======================= 对外方法 ========================= */
    /* ========================================================= */

    /**
     * 根据任务编码执行任务
     *
     * @param jobCode 任务唯一编码
     */
    public void executeByCode(String jobCode) {

        if (ObjectUtil.isEmpty(jobCode)) {
            log.warn("{} jobCode 为空", LOG_PREFIX);
            return;
        }

        TaskJob job = taskJobService.lambdaQuery()
                .eq(TaskJob::getJobCode, jobCode)
                .one();

        if (ObjectUtil.isEmpty(job)) {
            log.warn("{} 未找到任务 jobCode={}", LOG_PREFIX, jobCode);
            return;
        }

        execute(job);
    }

    /**
     * 根据业务类型批量执行任务
     *
     * @param bizType 业务类型，查询出批量执行
     */
    public void executeByBizType(String bizType) {

        if (ObjectUtil.isEmpty(bizType)) {
            log.warn("{} bizType 为空", LOG_PREFIX);
            return;
        }

        log.info("{} 开始扫描 bizType={}", LOG_PREFIX, bizType);

        final int pageSize = 100;
        int pageNo = 1;

        while (true) {

            Page<TaskJob> page = new Page<>(pageNo, pageSize);

            Page<TaskJob> result = taskJobService.lambdaQuery()
                    .eq(TaskJob::getBizType, bizType)
                    .eq(TaskJob::getExecuteStatus, TaskExecuteStatusEnum.PENDING.getCode())
                    .le(TaskJob::getNextExecuteTime, LocalDateTime.now())
                    .page(page);

            List<TaskJob> records = result.getRecords();

            if (CollectionUtil.isEmpty(records)) {
                break;
            }

            log.info("{} 扫描到任务数量={}", LOG_PREFIX, records.size());

            for (TaskJob job : records) {
                try {
                    execute(job);
                } catch (Exception ex) {
                    log.error("{} 批量执行异常 jobCode={}",
                            LOG_PREFIX,
                            job.getJobCode(),
                            ex);
                }
            }

            if (records.size() < pageSize) {
                break;
            }

            pageNo++;
        }

        log.info("{} bizType={} 扫描结束", LOG_PREFIX, bizType);
    }

    /**
     * 批量执行任务
     *
     * @param jobs 任务列表
     */
    public void executeBatch(List<TaskJob> jobs) {

        if (CollectionUtil.isEmpty(jobs)) {
            log.warn("{} 批量执行任务为空", LOG_PREFIX);
            return;
        }

        log.info("{} 开始批量执行任务，数量={}", LOG_PREFIX, jobs.size());

        int successCount = 0;
        int failCount = 0;

        for (TaskJob job : jobs) {

            try {

                execute(job);

                successCount++;

            } catch (Exception ex) {

                failCount++;

                log.error("{} 批量执行异常 jobCode={}",
                        LOG_PREFIX,
                        job.getJobCode(),
                        ex);
            }
        }

        log.info("{} 批量执行结束，总数={}, 成功={}, 异常={}",
                LOG_PREFIX,
                jobs.size(),
                successCount,
                failCount);
    }

    /**
     * 执行任务入口
     *
     * @param job 任务
     */
    public void execute(TaskJob job) {

        if (job == null) {
            log.warn("{} 传入任务为空", LOG_PREFIX);
            return;
        }

        log.info("{} 准备执行 jobCode={}, id={}, status={}, retry={}/{}",
                LOG_PREFIX,
                job.getJobCode(),
                job.getId(),
                job.getExecuteStatus(),
                job.getRetryCount(),
                job.getMaxRetryCount());

        if (!canExecute(job)) {
            log.info("{} 任务不可执行 jobCode={}",
                    LOG_PREFIX,
                    job.getJobCode());
            return;
        }

        if (!lockJob(job)) {
            log.warn("{} 抢占失败 jobCode={}, version={}",
                    LOG_PREFIX,
                    job.getJobCode(),
                    job.getVersion());
            return;
        }

        doExecute(job);
    }

    /* ========================================================= */
    /* ======================= 内部逻辑 ========================= */
    /* ========================================================= */

    /**
     * 判断任务是否可执行
     */
    private boolean canExecute(TaskJob job) {

        if (job.getExecuteStatus() == TaskExecuteStatusEnum.SUCCESS.getCode()) {
            return false;
        }

        if (job.getNextExecuteTime() != null
                && job.getNextExecuteTime().isAfter(LocalDateTime.now())) {
            return false;
        }

        return true;
    }

    /**
     * 乐观锁抢占任务
     */
    private boolean lockJob(TaskJob job) {

        if (job.getExecuteStatus() == TaskExecuteStatusEnum.RUNNING.getCode()
                && job.getLockTime() != null
                && job.getLockTime().isAfter(
                LocalDateTime.now().minusMinutes(LOCK_TIMEOUT_MINUTES))) {

            log.warn("{} 任务仍被锁定 jobCode={}, lockTime={}",
                    LOG_PREFIX,
                    job.getJobCode(),
                    job.getLockTime());

            return false;
        }

        TaskJob update = new TaskJob();
        update.setId(job.getId());
        update.setExecuteStatus(TaskExecuteStatusEnum.RUNNING.getCode());
        update.setLockTime(LocalDateTime.now());
        update.setVersion(job.getVersion());

        boolean success = taskJobService.updateById(update);

        if (success) {
            log.info("{} 抢占成功 jobCode={}", LOG_PREFIX, job.getJobCode());
        }

        return success;
    }

    /**
     * 真正执行任务
     */
    private void doExecute(TaskJob job) {

        boolean success = false;
        String errorMsg = null;

        long startTime = System.currentTimeMillis();

        int retryCount = ObjectUtil.defaultIfNull(job.getRetryCount(), 0);
        int maxRetry = ObjectUtil.defaultIfNull(job.getMaxRetryCount(), 0);
        int retryInterval = ObjectUtil.defaultIfNull(job.getRetryIntervalSeconds(), 0);

        try {

            log.info("{} 调用方法 bean={}, method={}",
                    LOG_PREFIX,
                    job.getBeanName(),
                    job.getMethodName());

            Object bean = SpringUtil.getBean(job.getBeanName());

            ReflectInvokeUtil.invoke(
                    bean,
                    job.getMethodName(),
                    job.getMethodParamTypes(),
                    job.getMethodParams()
            );

            success = true;

        } catch (Exception e) {

            errorMsg = ExceptionUtil.stacktraceToString(e, -1);

            log.error("{} 执行异常 jobCode={}",
                    LOG_PREFIX,
                    job.getJobCode(),
                    e);
        }

        long duration = System.currentTimeMillis() - startTime;

        log.info("{} 执行结束 jobCode={}, success={}, duration={}ms",
                LOG_PREFIX,
                job.getJobCode(),
                success,
                duration);

        saveLog(job, retryCount, success, duration, errorMsg);

        if (success) {
            markSuccess(job);
        } else {
            handleFail(job, errorMsg, retryCount, maxRetry, retryInterval);
        }
    }

    /**
     * 标记成功
     */
    @Transactional(rollbackFor = Exception.class)
    public void markSuccess(TaskJob job) {

        taskJobService.lambdaUpdate()
                .eq(TaskJob::getId, job.getId())
                .set(TaskJob::getExecuteStatus, TaskExecuteStatusEnum.SUCCESS.getCode())
                .set(TaskJob::getFailReason, null)
                .set(TaskJob::getLockTime, null)
                .update();

        log.info("{} 任务标记成功 jobCode={}", LOG_PREFIX, job.getJobCode());
    }

    /**
     * 处理失败及重试逻辑
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleFail(TaskJob job,
                           String errorMsg,
                           int retryCount,
                           int maxRetry,
                           int retryInterval) {

        int nextRetry = retryCount + 1;

        boolean finalFail = nextRetry >= maxRetry;

        LocalDateTime nextTime = finalFail
                ? null
                : LocalDateTime.now().plusSeconds(retryInterval);

        taskJobService.lambdaUpdate()
                .eq(TaskJob::getId, job.getId())
                .set(TaskJob::getRetryCount, nextRetry)
                .set(TaskJob::getExecuteStatus, finalFail ? TaskExecuteStatusEnum.FAILED.getCode() : TaskExecuteStatusEnum.PENDING.getCode())
                .set(TaskJob::getFailReason, errorMsg)
                .set(TaskJob::getNextExecuteTime, nextTime)
                .set(TaskJob::getLockTime, null)
                .update();

        if (finalFail) {

            log.error("{} 任务最终失败 jobCode={}, retry={}/{}",
                    LOG_PREFIX,
                    job.getJobCode(),
                    nextRetry,
                    maxRetry);

        } else {

            log.warn("{} 任务失败等待重试 jobCode={}, retry={}/{}, nextExecuteTime={}",
                    LOG_PREFIX,
                    job.getJobCode(),
                    nextRetry,
                    maxRetry,
                    nextTime);
        }
    }

    /**
     * 写执行日志
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveLog(TaskJob job,
                        int retryCount,
                        boolean success,
                        long duration,
                        String errorMsg) {

        TaskJobLog logEntity = new TaskJobLog();
        logEntity.setJobId(job.getId());
        logEntity.setJobCode(job.getJobCode());
        logEntity.setBizType(job.getBizType());
        logEntity.setExecuteTime(LocalDateTime.now());
        logEntity.setExecuteStatus(success ? TaskExecuteStatusEnum.SUCCESS.getCode() : TaskExecuteStatusEnum.FAILED.getCode());
        logEntity.setRetryCount(retryCount);
        logEntity.setExecuteDuration(duration);
        logEntity.setErrorMessage(errorMsg);

        taskJobLogService.save(logEntity);

        log.info("{} 已记录执行日志 jobCode={}, success={}",
                LOG_PREFIX,
                job.getJobCode(),
                success);
    }
}
