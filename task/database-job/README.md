# 数据库驱动的任务执行模型

本模型使用SpringBoot3基于数据库存储任务定义，通过反射方式动态执行指定 Bean 方法，支持任意参数类型（含复杂对象），内置重试机制与乐观锁并发控制。
 任务执行成功后自动删除，失败则保留并记录日志，可人工干预后重新执行。
 适用于一次性任务、异步补偿任务及轻量级后台任务执行场景。



## 创建表

### 任务表

```sql
DROP TABLE IF EXISTS task_job;
CREATE TABLE task_job
(
    id                     BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    job_code               VARCHAR(64)  NOT NULL COMMENT '任务唯一编码',
    job_name               VARCHAR(128) NOT NULL COMMENT '任务名称',
    job_desc               TEXT         NULL COMMENT '任务描述',

    biz_type               VARCHAR(64)  NOT NULL COMMENT '业务类型',
    biz_id                 VARCHAR(128) NULL COMMENT '业务ID',

    bean_name              VARCHAR(128) NOT NULL COMMENT 'Spring Bean名称',
    method_name            VARCHAR(128) NOT NULL COMMENT '方法名',

    method_param_types     TEXT         NULL COMMENT '方法参数类型(JSON数组)',
    method_params          TEXT         NULL COMMENT '方法参数值(JSON数组)',

    execute_status         TINYINT      NOT NULL DEFAULT 0
        COMMENT '执行状态 0=待执行 1=执行中 2=失败 3=成功',

    retry_count            INT          NOT NULL DEFAULT 0 COMMENT '已重试次数',
    max_retry_count        INT          NOT NULL DEFAULT 3 COMMENT '最大重试次数',

    retry_interval_seconds INT          NOT NULL DEFAULT 60 COMMENT '重试间隔(秒)',

    next_execute_time      DATETIME     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下次执行时间',

    execute_start_time     DATETIME     NULL COMMENT '执行开始时间',
    lock_time              DATETIME     NULL COMMENT '锁定时间',

    fail_reason            TEXT         NULL COMMENT '最终失败原因',

    version                INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',

    create_time            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_job_code (job_code),
    KEY idx_status_time (execute_status, next_execute_time),
    KEY idx_next_execute_time (next_execute_time)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='一次性/补偿任务表';

```

### 任务执行日志表

```sql
DROP TABLE IF EXISTS task_job_log;
CREATE TABLE task_job_log
(
    id               BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    job_id           BIGINT      NOT NULL COMMENT '任务ID',
    job_code         VARCHAR(64) NOT NULL COMMENT '任务编码',
    biz_type         VARCHAR(64) NOT NULL COMMENT '业务类型',

    execute_time     DATETIME    NOT NULL COMMENT '执行时间',
    execute_status   TINYINT     NOT NULL COMMENT '执行状态 2=失败 3=成功',

    retry_count      INT         NOT NULL COMMENT '本次执行前的重试次数',

    execute_duration BIGINT      NULL COMMENT '耗时(ms)',

    error_message    TEXT        NULL COMMENT '错误信息',

    create_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    KEY idx_job_execute_time (job_id, execute_time)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='任务执行日志表';

```

### 创建实体类

**任务表**

```java
package io.github.atengk.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 一次性任务 / 异步补偿任务表
 * </p>
 *
 * <p>
 * 用于存储需要执行的任务信息，
 * 支持延迟执行、失败重试、乐观锁控制。
 * </p>
 *
 * @author Ateng
 * @since 2026-02-12
 */
@Getter
@Setter
@ToString
@TableName("task_job")
public class TaskJob implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务唯一编码
     */
    @TableField("job_code")
    private String jobCode;

    /**
     * 任务名称
     */
    @TableField("job_name")
    private String jobName;

    /**
     * 任务描述
     */
    @TableField("job_desc")
    private String jobDesc;

    /**
     * 业务类型
     */
    @TableField("biz_type")
    private String bizType;

    /**
     * 业务ID
     */
    @TableField("biz_id")
    private String bizId;

    /**
     * Spring Bean名称
     */
    @TableField("bean_name")
    private String beanName;

    /**
     * 方法名
     */
    @TableField("method_name")
    private String methodName;

    /**
     * 方法参数类型(JSON数组)
     */
    @TableField("method_param_types")
    private String methodParamTypes;

    /**
     * 方法参数值(JSON数组)
     */
    @TableField("method_params")
    private String methodParams;

    /**
     * 执行状态
     * 0=待执行 1=执行中 2=失败 3=成功
     */
    @TableField("execute_status")
    private Integer executeStatus;

    /**
     * 已重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    @TableField("max_retry_count")
    private Integer maxRetryCount;

    /**
     * 重试间隔(秒)
     */
    @TableField("retry_interval_seconds")
    private Integer retryIntervalSeconds;

    /**
     * 下次执行时间
     */
    @TableField("next_execute_time")
    private LocalDateTime nextExecuteTime;

    /**
     * 执行开始时间
     */
    @TableField("execute_start_time")
    private LocalDateTime executeStartTime;

    /**
     * 锁定时间
     */
    @TableField("lock_time")
    private LocalDateTime lockTime;

    /**
     * 最终失败原因
     */
    @TableField("fail_reason")
    private String failReason;

    /**
     * 乐观锁版本号
     */
    @Version
    @TableField("version")
    private Integer version;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}

```

**任务执行日志表**

```java
package io.github.atengk.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 任务执行日志表
 * </p>
 *
 * <p>
 * 用于记录任务每次执行情况，
 * 包括执行结果、耗时、错误信息等。
 * </p>
 *
 * @author Ateng
 * @since 2026-02-12
 */
@Getter
@Setter
@ToString
@TableName("task_job_log")
public class TaskJobLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID
     */
    @TableField("job_id")
    private Long jobId;

    /**
     * 任务编码
     */
    @TableField("job_code")
    private String jobCode;

    /**
     * 业务类型
     */
    @TableField("biz_type")
    private String bizType;

    /**
     * 执行时间
     */
    @TableField("execute_time")
    private LocalDateTime executeTime;

    /**
     * 执行状态
     * 2=失败 3=成功
     */
    @TableField("execute_status")
    private Integer executeStatus;

    /**
     * 本次执行前的重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 执行耗时(ms)
     */
    @TableField("execute_duration")
    private Long executeDuration;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;
}

```

### 创建配置类

```java
package io.github.atengk.task.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("io.github.atengk.**.mapper")
public class MyBatisPlusConfiguration {

    /**
     * 拦截器配置
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        return interceptor;
    }

}

```



## 创建 execute

### 创建反射工具类

```java
package io.github.atengk.task.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.core.util.ReflectUtil;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 反射调用工具类
 * <p>
 * 功能说明：
 * 1. 支持通过 Spring Bean + 方法名动态调用方法
 * 2. 支持基础类型参数
 * 3. 支持复杂对象参数（JSON转Bean）
 * 4. 支持无参方法
 * <p>
 * 参数格式说明：
 * method_param_types  : ["java.lang.Long","java.lang.String"]
 * method_params       : [10001,"admin"]
 * <p>
 * 注意：
 * 1. 参数类型数组顺序必须与参数值数组顺序一致
 * 2. 参数数量必须匹配
 *
 * @author Ateng
 * @since 2026-02-12
 */
public class ReflectInvokeUtil {

    /**
     * 反射调用指定方法
     *
     * @param bean           Spring 容器中的 Bean 实例
     * @param methodName     方法名
     * @param paramTypesJson 方法参数类型(JSON数组)
     * @param paramsJson     方法参数值(JSON数组)
     * @return 方法返回值
     */
    public static Object invoke(Object bean,
                                String methodName,
                                String paramTypesJson,
                                String paramsJson) {

        if (bean == null) {
            throw new IllegalArgumentException("反射调用失败：Bean 不能为空");
        }

        if (StrUtil.isBlank(methodName)) {
            throw new IllegalArgumentException("反射调用失败：方法名不能为空");
        }

        // 构建参数类型数组
        Class<?>[] paramTypes = buildParamTypes(paramTypesJson);

        // 构建参数值数组
        Object[] args = buildArgs(paramTypes, paramsJson);

        // 查找方法
        Method method = ReflectUtil.getMethod(bean.getClass(), methodName, paramTypes);

        if (method == null) {
            throw new IllegalArgumentException(
                    StrUtil.format("反射调用失败：未找到方法 {}，参数类型={}",
                            methodName,
                            paramTypesJson)
            );
        }

        if (paramTypes.length != args.length) {
            throw new IllegalArgumentException(
                    StrUtil.format("反射调用失败：参数数量不匹配，期望 {} 个，实际 {} 个",
                            paramTypes.length,
                            args.length)
            );
        }

        try {
            return ReflectUtil.invoke(bean, method, args);
        } catch (Exception e) {
            throw new RuntimeException(
                    StrUtil.format("反射调用方法失败：{}#{}",
                            bean.getClass().getName(),
                            methodName),
                    e
            );
        }
    }

    /**
     * 构建参数类型数组
     *
     * @param paramTypesJson 参数类型JSON
     * @return Class数组
     */
    private static Class<?>[] buildParamTypes(String paramTypesJson) {

        if (StrUtil.isBlank(paramTypesJson) || "[]" .equals(paramTypesJson)) {
            return new Class<?>[0];
        }

        List<String> typeList = JSONUtil.toList(paramTypesJson, String.class);

        return typeList.stream()
                .map(ReflectInvokeUtil::loadClass)
                .toArray(Class<?>[]::new);
    }

    /**
     * 构建参数值数组
     *
     * @param paramTypes 参数类型数组
     * @param paramsJson 参数值JSON
     * @return 参数对象数组
     */
    private static Object[] buildArgs(Class<?>[] paramTypes, String paramsJson) {

        if (StrUtil.isBlank(paramsJson) || "[]" .equals(paramsJson)) {
            return new Object[0];
        }

        List<Object> paramList = JSONUtil.toList(paramsJson, Object.class);

        if (paramList.size() != paramTypes.length) {
            throw new IllegalArgumentException(
                    StrUtil.format("参数数量不匹配：期望 {} 个，实际 {} 个",
                            paramTypes.length,
                            paramList.size())
            );
        }

        Object[] args = new Object[paramTypes.length];

        for (int i = 0; i < paramTypes.length; i++) {

            Class<?> targetType = paramTypes[i];
            Object value = paramList.get(i);

            if (value == null) {
                args[i] = null;
                continue;
            }

            // 如果是JSON对象，则转为复杂类型
            if (JSONUtil.isTypeJSON(String.valueOf(value))) {
                args[i] = JSONUtil.toBean(
                        JSONUtil.parseObj(value),
                        targetType
                );
            } else {
                args[i] = Convert.convert(targetType, value);
            }
        }

        return args;
    }

    /**
     * 加载Class
     *
     * @param className 类全限定名
     * @return Class对象
     */
    private static Class<?> loadClass(String className) {

        if (StrUtil.isBlank(className)) {
            throw new IllegalArgumentException("参数类型不能为空");
        }

        try {
            return Class.forName(className);
        } catch (Exception e) {
            throw new RuntimeException(
                    StrUtil.format("加载参数类型失败：{}", className),
                    e
            );
        }
    }
}

```

### 创建枚举

```java
package io.github.atengk.task.enums;

import java.util.Arrays;

/**
 * 任务执行状态枚举
 *
 * @author Ateng
 * @since 2026-02-12
 */
public enum TaskExecuteStatusEnum {

    /**
     * 待执行
     */
    PENDING(0, "待执行"),

    /**
     * 执行中
     */
    RUNNING(1, "执行中"),

    /**
     * 执行失败
     */
    FAILED(2, "失败"),

    /**
     * 执行成功
     */
    SUCCESS(3, "成功");


    private final int code;

    private final String name;

    TaskExecuteStatusEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static TaskExecuteStatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("未知的任务执行状态 code: " + code));
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}

```

### 创建执行器

```java
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

```



## 执行任务

### 创建测试服务

**创建实体类**

```java
package io.github.atengk.task.dto;

import lombok.Data;

@Data
public class OrderDTO {

    private Long orderId;
    private String userName;
    private Double amount;
}

```

**创建服务类**

```java
package io.github.atengk.task.service;

import cn.hutool.json.JSONUtil;
import io.github.atengk.task.dto.OrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("orderTaskService")
public class OrderTaskService {

    /**
     * 无参方法
     */
    public void noParamTask() {
        log.info("执行无参任务成功");
    }

    /**
     * 基础类型参数
     */
    public void syncOrder(Long orderId, String operator) {
        log.info("同步订单，orderId={}, operator={}", orderId, operator);
    }

    /**
     * 复杂对象参数
     */
    public void createOrder(OrderDTO dto) {
        log.info("创建订单：{}", JSONUtil.toJsonStr(dto));
    }
}

```

### 创建测试接口

```java
package io.github.atengk.task.controller;

import io.github.atengk.task.entity.TaskJob;
import io.github.atengk.task.executor.TaskExecutor;
import io.github.atengk.task.service.ITaskJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class TaskTestController {

    private final TaskExecutor taskExecutor;
    private final ITaskJobService taskJobService;

    @GetMapping("/executeByCode")
    public String executeByCode(@RequestParam String code) {
        taskExecutor.executeByCode(code);
        return "执行完成";
    }

    @GetMapping("/executeByBizType")
    public String executeByBizType() {
        taskExecutor.executeByBizType("TEST_BIZ_ORDER");
        return "执行完成";
    }

    @PostMapping("/createJob")
    public String createJob() {

        TaskJob taskJob = new TaskJob();

        // 生成唯一任务编码（无-的UUID）
        taskJob.setJobCode(cn.hutool.core.util.IdUtil.fastSimpleUUID());

        // 任务信息
        taskJob.setJobName("创建订单任务");
        taskJob.setJobDesc("创建订单任务（详情）");

        // 业务类型
        taskJob.setBizType("TEST_BIZ_ORDER");
        // 业务ID（可选）
        taskJob.setBizId("TEST_BIZ_ORDER");

        // Spring 容器中的 Bean 名称
        taskJob.setBeanName("orderTaskService");

        // 要执行的方法
        taskJob.setMethodName("syncOrder");

        // 参数类型
        java.util.List<String> paramTypes =
                cn.hutool.core.collection.CollUtil.newArrayList(
                        "java.lang.Long",
                        "java.lang.String"
                );

        taskJob.setMethodParamTypes(
                cn.hutool.json.JSONUtil.toJsonStr(paramTypes)
        );

        // 参数值
        java.util.List<Object> params =
                cn.hutool.core.collection.CollUtil.newArrayList(
                        123,
                        "hello"
                );

        taskJob.setMethodParams(
                cn.hutool.json.JSONUtil.toJsonStr(params)
        );

        // 执行时间
        taskJob.setNextExecuteTime(java.time.LocalDateTime.now());

        // 重试相关
        taskJob.setRetryCount(0);
        taskJob.setMaxRetryCount(3);
        taskJob.setRetryIntervalSeconds(60);

        taskJobService.save(taskJob);

        return "创建成功，jobCode=" + taskJob.getJobCode();
    }


}

```

### 调用接口

**创建任务**

```
POST /task/createJob
```

**执行指定编码的任务**

```
GET /task/executeByCode?code=ee8c53945688403681844fc3b9910a2e
```

**批量执行指定业务类型的任务**

后续可以通过外部定时任务框架走定时任务来执行 executeByBizType ，补偿失败的任务。

```
GET /task/executeByBizType
```

