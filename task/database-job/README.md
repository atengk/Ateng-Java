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

    next_execute_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下次执行时间',

    execute_start_time     DATETIME     NULL COMMENT '执行开始时间',
    lock_time              DATETIME     NULL COMMENT '锁定时间',

    fail_reason            VARCHAR(2000) NULL COMMENT '最终失败原因',

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

    error_message    VARCHAR(2000) NULL COMMENT '错误信息',

    create_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    KEY idx_job_execute_time (job_id, execute_time)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='任务执行日志表';

```

### 插入测试数据

```sql
-- 无参任务
INSERT INTO task_job
(
    job_code,
    job_name,
    biz_type,
    biz_id,
    bean_name,
    method_name,
    method_param_types,
    method_params,
    max_retry_count,
    retry_interval_seconds,
    execute_status,
    next_execute_time,
    version
)
VALUES
(
    'no_param_job',
    '无参测试任务',
    'order',
    'TEST-001',
    'orderTaskService',
    'noParamTask',
    '[]',
    '[]',
    2,
    3,
    0,
    NOW(),
    0
);
-- 基础参数任务
INSERT INTO task_job
(
    job_code,
    job_name,
    biz_type,
    biz_id,
    bean_name,
    method_name,
    method_param_types,
    method_params,
    max_retry_count,
    retry_interval_seconds,
    execute_status,
    next_execute_time,
    version
)
VALUES
(
    'sync_order_job',
    '同步订单任务',
    'order',
    'ORDER-10001',
    'orderTaskService',
    'syncOrder',
    '["java.lang.Long","java.lang.String"]',
    '[10001,"admin"]',
    2,
    3,
    0,
    NOW(),
    0
);
-- 复杂对象任务
INSERT INTO task_job
(
    job_code,
    job_name,
    biz_type,
    biz_id,
    bean_name,
    method_name,
    method_param_types,
    method_params,
    max_retry_count,
    retry_interval_seconds,
    execute_status,
    next_execute_time,
    version
)
VALUES
(
    'create_order_job',
    '创建订单任务',
    'order',
    'ORDER-20001',
    'orderTaskService',
    'createOrder',
    '["io.github.atengk.task.dto.OrderDTO"]',
    '[{"orderId":20001,"userName":"Tom","amount":199.99}]',
    2,
    3,
    0,
    NOW(),
    0
);
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

public class ReflectInvokeUtil {

    /**
     * 反射调用方法
     */
    public static Object invoke(Object bean,
                                String methodName,
                                String paramTypesJson,
                                String paramsJson) {

        Class<?>[] paramTypes = buildParamTypes(paramTypesJson);
        Object[] args = buildArgs(paramTypes, paramsJson);

        Method method = ReflectUtil.getMethod(bean.getClass(), methodName, paramTypes);

        if (method == null) {
            throw new IllegalArgumentException("Method not found: " + methodName);
        }

        if (paramTypes.length != args.length) {
            throw new IllegalArgumentException("Parameter size mismatch");
        }

        return ReflectUtil.invoke(bean, method, args);
    }

    private static Class<?>[] buildParamTypes(String paramTypesJson) {

        if (StrUtil.isBlank(paramTypesJson) || "[]".equals(paramTypesJson)) {
            return new Class<?>[0];
        }

        List<String> typeList = JSONUtil.toList(paramTypesJson, String.class);

        return typeList.stream()
                .map(ReflectInvokeUtil::loadClass)
                .toArray(Class<?>[]::new);
    }

    private static Object[] buildArgs(Class<?>[] paramTypes, String paramsJson) {

        if (StrUtil.isBlank(paramsJson) || "[]".equals(paramsJson)) {
            return new Object[0];
        }

        List<Object> paramList = JSONUtil.toList(paramsJson, Object.class);

        Object[] args = new Object[paramTypes.length];

        for (int i = 0; i < paramTypes.length; i++) {

            Class<?> targetType = paramTypes[i];
            Object value = paramList.get(i);

            if (JSONUtil.isTypeJSON(String.valueOf(value))) {
                args[i] = JSONUtil.toBean(JSONUtil.parseObj(value), targetType);
            } else {
                args[i] = Convert.convert(targetType, value);
            }
        }

        return args;
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (Exception e) {
            throw new RuntimeException("Load class error: " + className, e);
        }
    }
}

```

### 创建执行器

```java
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

import io.github.atengk.task.executor.TaskExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class TaskTestController {

    private final TaskExecutor taskExecutor;

    @GetMapping("/execute")
    public String execute(@RequestParam String code) {
        taskExecutor.executeByCode(code);
        return "执行完成";
    }

}

```

### 调用接口

```
GET /task/execute?code=create_order_job
```

