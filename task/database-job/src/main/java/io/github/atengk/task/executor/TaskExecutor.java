package io.github.atengk.task.executor;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.task.entity.TaskJob;
import io.github.atengk.task.entity.TaskJobLog;
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
 * 数据库驱动任务执行服务
 * <p>
 * 特性：
 * 1. MyBatis-Plus 乐观锁抢占
 * 2. 防死锁恢复（lock_time）
 * 3. 自动重试
 * 4. 无长事务
 * 5. 成功标记成功，不删除
 * <p>
 * 适用于一次性任务 / 异步补偿任务
 *
 * @author Ateng
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TaskExecutor {

    private static final int LOCK_TIMEOUT_MINUTES = 5;

    private final ITaskJobLogService taskJobLogService;
    private final ITaskJobService taskJobService;

    /**
     * 根据任务编码执行任务
     *
     * @param jobCode 任务编码
     */
    public void executeByCode(String jobCode) {

        if (ObjectUtil.isEmpty(jobCode)) {
            return;
        }

        TaskJob job = taskJobService.lambdaQuery()
                .eq(TaskJob::getJobCode, jobCode)
                .one();

        if (ObjectUtil.isEmpty(job)) {
            log.warn("未找到任务 jobCode={}", jobCode);
            return;
        }

        execute(job);
    }

    /**
     * 根据业务类型批量执行任务
     *
     * @param bizType 业务类型
     */
    public void executeByBizType(String bizType) {

        if (ObjectUtil.isEmpty(bizType)) {
            return;
        }

        final int pageSize = 100;

        int pageNo = 1;

        while (true) {

            Page<TaskJob> page = new Page<>(pageNo, pageSize);

            Page<TaskJob> result = taskJobService.lambdaQuery()
                    .eq(TaskJob::getBizType, bizType)
                    .eq(TaskJob::getExecuteStatus, 0)
                    .le(TaskJob::getNextExecuteTime, LocalDateTime.now())
                    .page(page);

            List<TaskJob> records = result.getRecords();

            if (CollectionUtil.isEmpty(records)) {
                break;
            }

            for (TaskJob job : records) {
                try {
                    execute(job);
                } catch (Exception ex) {
                    log.error("执行任务异常 jobCode={}", job.getJobCode(), ex);
                }
            }

            if (records.size() < pageSize) {
                break;
            }

            pageNo++;
        }
    }

    /**
     * 批量执行任务
     *
     * @param jobs 任务列表
     */
    public void executeBatch(List<TaskJob> jobs) {
        if (CollectionUtil.isEmpty(jobs)) {
            return;
        }

        for (TaskJob job : jobs) {
            try {
                execute(job);
            } catch (Exception ex) {
                log.error("批量执行任务异常 jobCode={}", job.getJobCode(), ex);
            }
        }
    }

    /**
     * 执行任务（供调度调用）
     */
    public void execute(TaskJob job) {

        if (job == null) {
            return;
        }

        // 状态检查
        if (!canExecute(job)) {
            return;
        }

        // 乐观锁抢占
        if (!lockJob(job)) {
            return;
        }

        // 真正执行
        doExecute(job);
    }

    /**
     * 判断是否可执行
     */
    private boolean canExecute(TaskJob job) {

        if (job.getExecuteStatus() == 3) {
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

        // 防止死锁
        if (job.getExecuteStatus() == 1
                && job.getLockTime() != null
                && job.getLockTime().isAfter(
                LocalDateTime.now().minusMinutes(LOCK_TIMEOUT_MINUTES))) {
            return false;
        }

        TaskJob update = new TaskJob();
        update.setId(job.getId());
        update.setExecuteStatus(1);
        update.setLockTime(LocalDateTime.now());
        update.setExecuteStartTime(LocalDateTime.now());
        update.setVersion(job.getVersion());

        return taskJobService.updateById(update);
    }

    /**
     * 真正执行任务
     */
    private void doExecute(TaskJob job) {

        boolean success = false;
        String errorMsg = null;
        long startTime = System.currentTimeMillis();

        int retryCount = job.getRetryCount() == null ? 0 : job.getRetryCount();
        int maxRetry = job.getMaxRetryCount();
        int retryInterval = job.getRetryIntervalSeconds();

        try {

            Object bean = SpringUtil.getBean(job.getBeanName());

            ReflectInvokeUtil.invoke(
                    bean,
                    job.getMethodName(),
                    job.getMethodParamTypes(),
                    job.getMethodParams()
            );

            success = true;

        } catch (Exception e) {

            errorMsg = ExceptionUtil.stacktraceToString(e);

            log.error("任务执行异常，jobCode={}", job.getJobCode(), e);

        }

        long duration = System.currentTimeMillis() - startTime;

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
                .set(TaskJob::getExecuteStatus, 3)
                .set(TaskJob::getFailReason, null)
                .set(TaskJob::getLockTime, null)
                .update();

        log.info("任务执行成功，jobCode={}", job.getJobCode());
    }

    /**
     * 失败处理
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleFail(TaskJob job,
                           String errorMsg,
                           int retryCount,
                           int maxRetry,
                           int retryInterval) {

        int nextRetry = retryCount + 1;

        boolean finalFail = nextRetry >= maxRetry;

        taskJobService.lambdaUpdate()
                .eq(TaskJob::getId, job.getId())
                .set(TaskJob::getRetryCount, nextRetry)
                .set(TaskJob::getExecuteStatus, finalFail ? 2 : 0)
                .set(TaskJob::getFailReason,
                        StrUtil.sub(errorMsg, 0, 2000))
                .set(TaskJob::getNextExecuteTime,
                        finalFail ? null :
                                LocalDateTime.now().plusSeconds(retryInterval))
                .set(TaskJob::getLockTime, null)
                .update();

        log.warn("任务执行失败，jobCode={}，retry={}/{}",
                job.getJobCode(), nextRetry, maxRetry);
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
        logEntity.setExecuteStatus(success ? 2 : 3);
        logEntity.setRetryCount(retryCount);
        logEntity.setExecuteDuration(duration);
        logEntity.setErrorMessage(
                StrUtil.sub(errorMsg, 0, 2000)
        );

        taskJobLogService.save(logEntity);
    }
}
