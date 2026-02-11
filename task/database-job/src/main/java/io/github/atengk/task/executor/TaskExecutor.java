package io.github.atengk.task.executor;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
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
import java.util.concurrent.TimeUnit;

/**
 * 数据库驱动任务执行服务
 *
 * 特性：
 * 1. 乐观锁抢占
 * 2. 无长事务
 * 3. 支持多实例部署
 * 4. 成功删除，失败保留
 * 5. 支持人工重置后再次执行
 *
 * @author Ateng
 * @since 2026-02-11
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TaskExecutor {

    private final ITaskJobLogService taskJobLogService;
    private final ITaskJobService taskJobService;

    /**
     * 执行任务（供调度框架调用）
     */
    public void execute(TaskJob job) {

        if (job == null) {
            return;
        }

        // 抢占任务（乐观锁 + 状态 + 时间控制）
        if (!lockJob(job)) {
            return;
        }

        // 真正执行（无事务）
        doExecute(job);
    }

    /**
     * 抢占任务
     */
    private boolean lockJob(TaskJob job) {

        job.setExecuteStatus(1);

        return taskJobService.updateById(job);
    }


    /**
     * 真正执行任务
     */
    private void doExecute(TaskJob job) {

        int attempt = 0;
        boolean success = false;
        String errorMsg = null;
        long startTime = System.currentTimeMillis();

        int maxRetry = job.getMaxRetryCount() == null ? 0 : job.getMaxRetryCount();
        int retryInterval = job.getRetryInterval() == null ? 0 : job.getRetryInterval();

        while (attempt <= maxRetry) {

            try {

                attempt++;

                Object bean = SpringUtil.getBean(job.getBeanName());

                ReflectInvokeUtil.invoke(
                        bean,
                        job.getMethodName(),
                        job.getMethodParamTypes(),
                        job.getMethodParams()
                );

                success = true;
                break;

            } catch (Exception e) {

                errorMsg = ExceptionUtil.stacktraceToString(e);
                log.error("任务执行失败，jobCode={}, 第{}次尝试", job.getJobCode(), attempt, e);

                if (attempt > maxRetry) {
                    break;
                }

                ThreadUtil.sleep(retryInterval, TimeUnit.SECONDS);
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        // 写执行日志（短事务）
        saveLog(job, attempt - 1, success, duration, errorMsg);

        // 更新任务状态
        if (success) {
            deleteSuccessJob(job.getId());
        } else {
            markFail(job.getId(), errorMsg, retryInterval);
        }
    }

    /**
     * 写执行日志（独立事务）
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
        logEntity.setExecuteTime(LocalDateTime.now());
        logEntity.setExecuteStatus(success ? 1 : 2);
        logEntity.setRetryCount(retryCount);
        logEntity.setExecuteDuration(duration);
        logEntity.setErrorMessage(
                StrUtil.sub(errorMsg, 0, 5000) // 防止日志过大
        );
        logEntity.setCreateTime(LocalDateTime.now());

        taskJobLogService.save(logEntity);
    }

    /**
     * 成功后删除任务（独立事务）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSuccessJob(Long jobId) {
        taskJobService.removeById(jobId);
    }

    /**
     * 标记失败（独立事务）
     * 同时设置 next_execute_time，防止立即被再次拉取
     */
    @Transactional(rollbackFor = Exception.class)
    public void markFail(Long jobId,
                         String errorMsg,
                         int retryInterval) {

        taskJobService.lambdaUpdate()
                .eq(TaskJob::getId, jobId)
                .set(TaskJob::getExecuteStatus, 2)
                .set(TaskJob::getFailReason,
                        StrUtil.sub(errorMsg, 0, 5000))
                .set(TaskJob::getNextExecuteTime,
                        LocalDateTime.now().plusSeconds(retryInterval))
                .update();
    }
}
