# 数据库驱动的任务执行模型

本模型基于数据库存储任务定义，通过反射方式动态执行指定 Bean 方法，支持任意参数类型（含复杂对象），内置重试机制与乐观锁并发控制。
 任务执行成功后自动删除，失败则保留并记录日志，可人工干预后重新执行。
 适用于一次性任务、异步补偿任务及轻量级后台任务执行场景。



## 创建表

### 任务定义表

```sql
DROP TABLE IF EXISTS task_job;
CREATE TABLE task_job
(
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    job_code           VARCHAR(64)  NOT NULL COMMENT '任务唯一编码',
    job_name           VARCHAR(128) NOT NULL COMMENT '任务名称',
    job_desc           TEXT         NULL COMMENT '任务描述',

    biz_type           VARCHAR(64)  NOT NULL COMMENT '业务类型',

    bean_name          VARCHAR(128) NOT NULL COMMENT 'Spring Bean名称',
    method_name        VARCHAR(128) NOT NULL COMMENT '方法名',

    method_param_types TEXT                  DEFAULT NULL COMMENT '方法参数类型(JSON数组)',
    method_params      TEXT                  DEFAULT NULL COMMENT '方法参数值(JSON数组)',

    max_retry_count    INT                   DEFAULT 3 COMMENT '最大重试次数',
    retry_interval     INT                   DEFAULT 60 COMMENT '重试间隔(秒)',

    execute_status     TINYINT      NOT NULL DEFAULT 0
        COMMENT '执行状态 0=待执行 1=执行中 2=执行失败',

    next_execute_time  DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '下次执行时间',
    fail_reason        TEXT                  DEFAULT NULL COMMENT '最终失败原因',

    version            INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',

    create_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_job_code (job_code),
    INDEX idx_execute_status (execute_status),
    INDEX idx_status_time (execute_status, next_execute_time)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='任务定义表';
```

### 任务执行日志表

```sql
DROP TABLE IF EXISTS task_job_log;
CREATE TABLE task_job_log
(
    id               BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    job_id           BIGINT      NOT NULL COMMENT '任务ID',
    job_code         VARCHAR(64) NOT NULL COMMENT '任务编码',

    execute_time     DATETIME    NOT NULL COMMENT '执行时间',
    execute_status   TINYINT     NOT NULL COMMENT '1=成功 2=失败',

    retry_count      INT                  DEFAULT 0 COMMENT '重试次数',
    execute_duration BIGINT               DEFAULT NULL COMMENT '耗时(ms)',

    error_message    TEXT                 DEFAULT NULL COMMENT '错误信息',

    create_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_job_id (job_id),
    INDEX idx_execute_time (execute_time)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='任务执行日志表';
```

### 插入测试数据

```sql
-- 无参任务
INSERT INTO task_job
(job_code, job_name, biz_type, bean_name, method_name,
 method_param_types, method_params,
 max_retry_count, retry_interval,
 execute_status, next_execute_time, version)
VALUES
('no_param_job',
 '无参测试任务',
 'order',
 'orderTaskService',
 'noParamTask',
 '[]',
 '[]',
 2,
 3,
 0,
 NOW(),
 0);

-- 基础参数任务
INSERT INTO task_job
(job_code, job_name, biz_type, bean_name, method_name,
 method_param_types, method_params,
 max_retry_count, retry_interval,
 execute_status, next_execute_time, version)
VALUES
('sync_order_job',
 '同步订单任务',
 'order',
 'orderTaskService',
 'syncOrder',
 '["java.lang.Long","java.lang.String"]',
 '[10001,"admin"]',
 2,
 3,
 0,
 NOW(),
 0);

-- 复杂对象任务（修正包名）
INSERT INTO task_job
(job_code, job_name, biz_type, bean_name, method_name,
 method_param_types, method_params,
 max_retry_count, retry_interval,
 execute_status, next_execute_time, version)
VALUES
('create_order_job',
 '创建订单任务',
 'order',
 'orderTaskService',
 'createOrder',
 '["io.github.atengk.task.dto.OrderDTO"]',
 '[{"orderId":20001,"userName":"Tom","amount":199.99}]',
 2,
 3,
 0,
 NOW(),
 0);

```

### 创建实体类

```java
package io.github.atengk.task.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 任务定义表
 * </p>
 *
 * @author Ateng
 * @since 2026-02-11
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
     * 业务类型，如 order/report/user 等
     */
    @TableField("biz_type")
    private String bizType;

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
     * 最大重试次数
     */
    @TableField("max_retry_count")
    private Integer maxRetryCount;

    /**
     * 重试间隔(秒)
     */
    @TableField("retry_interval")
    private Integer retryInterval;

    /**
     * 执行状态 0=待执行 1=执行中 2=执行失败
     */
    @TableField("execute_status")
    private Integer executeStatus;

    /**
     * 最终失败原因
     */
    @TableField("next_execute_time")
    private LocalDateTime nextExecuteTime;

    /**
     * 最终失败原因
     */
    @TableField("fail_reason")
    private String failReason;

    /**
     * 乐观锁版本号
     */
    @TableField("version")
    @Version
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

### 创建执行服务

```java
package io.github.atengk.task.service.impl;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.task.entity.TaskJob;
import io.github.atengk.task.entity.TaskJobLog;
import io.github.atengk.task.mapper.TaskJobMapper;
import io.github.atengk.task.service.ITaskJobLogService;
import io.github.atengk.task.service.ITaskJobService;
import io.github.atengk.task.util.ReflectInvokeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 数据库驱动任务执行服务（生产级实现）
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
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskJobServiceImpl
        extends ServiceImpl<TaskJobMapper, TaskJob>
        implements ITaskJobService {

    private final ITaskJobLogService taskJobLogService;

    /**
     * 执行任务（供调度框架调用）
     */
    @Override
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

        return updateById(job);
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
        removeById(jobId);
    }

    /**
     * 标记失败（独立事务）
     * 同时设置 next_execute_time，防止立即被再次拉取
     */
    @Transactional(rollbackFor = Exception.class)
    public void markFail(Long jobId,
                         String errorMsg,
                         int retryInterval) {

        lambdaUpdate()
                .eq(TaskJob::getId, jobId)
                .set(TaskJob::getExecuteStatus, 2)
                .set(TaskJob::getFailReason,
                        StrUtil.sub(errorMsg, 0, 5000))
                .set(TaskJob::getNextExecuteTime,
                        LocalDateTime.now().plusSeconds(retryInterval))
                .update();
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
import io.github.atengk.task.service.ITaskJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class TaskTestController {

    private final ITaskJobService taskJobService;

    @GetMapping("/execute")
    public String execute(@RequestParam String code) {
        TaskJob taskJob = taskJobService
                .lambdaQuery()
                .eq(TaskJob::getJobCode, code)
                .one();
        taskJobService.execute(taskJob);
        return "执行完成";
    }

}

```

### 调用接口

```
GET /task/execute?code=create_order_job
```

