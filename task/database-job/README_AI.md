# 数据库驱动的一次性 / 补偿任务执行方案

本方案用于在 Spring Boot 3 项目中落地数据库驱动的轻量级任务执行模型。任务定义存储在 MySQL 中，执行器定时扫描待执行任务，通过 Spring Bean 名称和方法名调用业务方法，并提供失败重试、乐观锁抢占、锁超时恢复、执行日志、调用白名单和人工干预能力。

该方案适合一次性任务、异步补偿任务、轻量级后台任务、外部接口失败后的补偿执行。不建议替代 XXL-JOB、Quartz、PowerJob 等完整调度平台；如果任务需要复杂 Cron、分片、可视化调度、分布式工作流或大量任务吞吐，应优先使用专业调度框架。

## 方案边界

本方案解决的是“业务任务已经产生，后续需要可靠执行”的问题。任务执行成功后默认标记为成功并保留记录，便于追踪和审计；如需减少表数据量，可通过清理任务归档或删除成功任务。

适用场景：

| 场景 | 是否适合 | 说明 |
| --- | --- | --- |
| 订单创建后异步同步第三方系统 | 适合 | 失败后可自动重试，最终失败可人工处理 |
| 支付回调后补偿业务状态 | 适合 | 要求业务方法具备幂等性 |
| 短耗时后台任务 | 适合 | 单次执行建议控制在秒级到分钟级 |
| 大批量数据处理 | 谨慎 | 需要控制分页、限流、并发和执行时长 |
| 精确 Cron 调度 | 不适合 | 建议使用 XXL-JOB、Quartz 等 |
| 复杂任务编排 / DAG | 不适合 | 建议使用工作流或调度平台 |

核心约束：

1. 被调用的业务方法必须幂等，因为失败重试、锁超时恢复、服务重启都可能导致重复调用。
2. 不允许把通用创建任务接口直接暴露给外部调用方，避免任意 Bean 方法被调用。
3. 生产环境必须配置调用白名单，只允许执行明确登记过的 Bean 和方法。
4. 执行器不在长事务中调用业务方法，锁定、执行、记录日志、更新状态拆分为短事务。
5. 多实例部署时依赖数据库条件更新抢占任务，避免同一任务被多个实例同时执行。

## 基础配置

本节给出项目依赖、配置项和目录结构。后续代码均基于这些基础配置展开，包名统一使用 `io.github.atengk.task`。

### Maven 依赖

以下依赖放在业务模块的 `pom.xml` 中。版本建议由父工程统一管理，避免多个模块重复固定版本。

```xml
<dependencies>
    <!-- Spring Web：用于演示接口；如果项目不需要测试接口，可以移除 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验：用于任务创建参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- AOP：用于获取被代理 Bean 的真实目标类型 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>

    <!-- MyBatis-Plus：数据库 CRUD、分页和条件更新 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    </dependency>

    <!-- MySQL 驱动：根据项目实际数据库替换 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Hutool：JSON、集合、字符串、ID、异常工具 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
    </dependency>

    <!-- Lombok：减少实体、DTO、构造器样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

### application.yml

这里配置数据源、MyBatis-Plus、任务扫描间隔、锁超时时间和调用白名单。生产环境建议把白名单配置纳入代码评审范围。

```yaml
spring:
  datasource:
    # 数据库连接地址，按项目实际环境替换
    url: jdbc:mysql://127.0.0.1:3306/demo_task?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis-plus:
  configuration:
    # 开发环境可开启 SQL 日志，生产环境建议关闭或接入统一日志平台
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      # 默认逻辑可按项目规范调整
      id-type: auto

task-job:
  executor:
    # 是否启用数据库任务扫描器
    enabled: true
    # 每次扫描最多取出的任务数量
    scan-page-size: 100
    # 执行中任务超过该时间未释放锁时，允许被恢复为待执行
    lock-timeout-minutes: 5
    # 扫描间隔，单位毫秒
    scan-fixed-delay-ms: 5000
  invoke-allow-list:
    # 生产环境只允许执行明确登记的方法，不建议使用 *
    orderTaskService:
      - syncOrder
      - createOrder
      - noParamTask
```

### 代码结构

下面是建议的最小可落地目录结构。Mapper XML 不是必须的，本方案使用 MyBatis-Plus 通用 Mapper 即可运行。

```text
src/main/java/io/github/atengk/task
├── config
│   ├── MyBatisPlusConfiguration.java
│   └── TaskJobProperties.java
├── controller
│   └── TaskTestController.java
├── dto
│   ├── OrderDTO.java
│   └── TaskJobCreateParam.java
├── entity
│   ├── TaskJob.java
│   └── TaskJobLog.java
├── enums
│   └── TaskExecuteStatusEnum.java
├── executor
│   ├── TaskExecutor.java
│   └── TaskJobScheduleExecutor.java
├── guard
│   └── TaskJobInvokeGuard.java
├── mapper
│   ├── TaskJobLogMapper.java
│   └── TaskJobMapper.java
├── service
│   ├── ITaskJobLogService.java
│   ├── ITaskJobService.java
│   ├── OrderTaskService.java
│   ├── TaskJobSubmitService.java
│   └── TaskJobTxService.java
├── service/impl
│   ├── TaskJobLogServiceImpl.java
│   └── TaskJobServiceImpl.java
└── util
    └── ReflectInvokeUtil.java
```

## 数据库设计

数据库设计需要同时满足任务抢占、失败重试、人工排查和后续清理。任务表保存当前任务状态，日志表保存每一次执行结果。

### 任务表

任务表记录任务的执行目标、参数、状态、重试策略和锁信息。`lock_owner` 用于避免锁超时后旧执行器覆盖新执行器的执行结果。

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

    method_param_types     TEXT         NULL COMMENT '方法参数类型JSON数组',
    method_params          TEXT         NULL COMMENT '方法参数值JSON数组',

    execute_status         TINYINT      NOT NULL DEFAULT 0 COMMENT '执行状态 0=待执行 1=执行中 2=失败 3=成功',

    retry_count            INT          NOT NULL DEFAULT 0 COMMENT '已失败次数',
    max_retry_count        INT          NOT NULL DEFAULT 3 COMMENT '最大失败次数',
    retry_interval_seconds INT          NOT NULL DEFAULT 60 COMMENT '重试间隔秒数',

    next_execute_time      DATETIME     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下次执行时间',

    execute_start_time     DATETIME     NULL COMMENT '本次执行开始时间',
    lock_time              DATETIME     NULL COMMENT '锁定时间',
    lock_owner             VARCHAR(64)  NULL COMMENT '锁持有者',

    fail_reason            TEXT         NULL COMMENT '最后一次失败原因',

    version                INT          NOT NULL DEFAULT 0 COMMENT '版本号',

    create_time            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_job_code (job_code),
    KEY idx_status_time (execute_status, next_execute_time),
    KEY idx_biz_type_status_time (biz_type, execute_status, next_execute_time),
    KEY idx_lock_timeout (execute_status, lock_time),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '数据库驱动任务表';
```

### 任务执行日志表

日志表记录每次执行的状态、耗时、失败信息和锁持有者。后续排查问题时优先查日志表，再回看业务日志。

```sql
DROP TABLE IF EXISTS task_job_log;

CREATE TABLE task_job_log
(
    id               BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    job_id           BIGINT       NOT NULL COMMENT '任务ID',
    job_code         VARCHAR(64)  NOT NULL COMMENT '任务编码',
    biz_type         VARCHAR(64)  NOT NULL COMMENT '业务类型',
    lock_owner       VARCHAR(64)  NULL COMMENT '本次执行锁持有者',

    execute_time     DATETIME     NOT NULL COMMENT '执行时间',
    execute_status   TINYINT      NOT NULL COMMENT '执行状态 2=失败 3=成功',

    retry_count      INT          NOT NULL COMMENT '本次执行前失败次数',
    execute_duration BIGINT       NULL COMMENT '耗时毫秒',

    error_message    TEXT         NULL COMMENT '错误信息',

    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    KEY idx_job_execute_time (job_id, execute_time),
    KEY idx_job_code_execute_time (job_code, execute_time),
    KEY idx_biz_type_execute_time (biz_type, execute_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '数据库驱动任务执行日志表';
```

### 状态流转

任务状态只通过执行器或人工干预脚本流转，不建议业务代码直接修改状态。

| 当前状态 | 目标状态 | 触发条件 |
| --- | --- | --- |
| 待执行 | 执行中 | 扫描器抢占成功 |
| 执行中 | 成功 | 业务方法执行成功 |
| 执行中 | 待执行 | 执行失败，且未达到最大失败次数 |
| 执行中 | 失败 | 执行失败，且达到最大失败次数 |
| 执行中 | 待执行 | 锁超时恢复 |
| 失败 | 待执行 | 人工确认后重新投递 |
| 成功 | 保留 / 清理 | 默认保留，可定期归档或删除 |

## 核心代码

本节给出可以直接放入项目的关键代码。为了避免事务自调用失效，状态更新和日志写入拆分到 `TaskJobTxService` 中，由执行器调用该服务完成短事务操作。

### 配置类

`TaskJobProperties` 用于读取任务执行器配置和调用白名单。

文件位置：`src/main/java/io/github/atengk/task/config/TaskJobProperties.java`

```java
package io.github.atengk.task.config;

import cn.hutool.core.map.MapUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * 数据库任务配置属性
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Data
@ConfigurationProperties(prefix = "task-job")
public class TaskJobProperties {

    /**
     * 执行器配置
     */
    private Executor executor = new Executor();

    /**
     * 调用白名单
     * key：Spring Bean 名称
     * value：允许调用的方法名列表
     */
    private Map<String, List<String>> invokeAllowList = MapUtil.newHashMap();

    /**
     * 执行器配置
     *
     * @author Ateng
     * @since 2026-05-12
     */
    @Data
    public static class Executor {

        /**
         * 是否启用扫描器
         */
        private Boolean enabled = true;

        /**
         * 每次扫描任务数量
         */
        private Integer scanPageSize = 100;

        /**
         * 锁超时时间，单位分钟
         */
        private Integer lockTimeoutMinutes = 5;

        /**
         * 扫描间隔，单位毫秒
         */
        private Long scanFixedDelayMs = 5000L;
    }
}
```

`MyBatisPlusConfiguration` 开启 Mapper 扫描、分页插件和配置属性绑定。

文件位置：`src/main/java/io/github/atengk/task/config/MyBatisPlusConfiguration.java`

```java
package io.github.atengk.task.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * MyBatis-Plus 和任务扫描配置
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(TaskJobProperties.class)
@MapperScan("io.github.atengk.**.mapper")
public class MyBatisPlusConfiguration {

    /**
     * 配置 MyBatis-Plus 拦截器
     *
     * @return MyBatis-Plus 拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件，用于任务扫描分页
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

### 枚举

任务状态枚举统一维护状态码，避免业务代码中散落魔法值。

文件位置：`src/main/java/io/github/atengk/task/enums/TaskExecuteStatusEnum.java`

```java
package io.github.atengk.task.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 任务执行状态枚举
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Getter
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

    /**
     * 根据状态码获取枚举
     *
     * @param code 状态码
     * @return 状态枚举
     */
    public static TaskExecuteStatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知任务执行状态：" + code));
    }
}
```

### 实体类

实体类与表字段保持一致。`method_param_types` 和 `method_params` 使用 JSON 字符串存储，创建任务时由服务统一序列化。

文件位置：`src/main/java/io/github/atengk/task/entity/TaskJob.java`

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
 * 数据库驱动任务表
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Getter
@Setter
@ToString
@TableName("task_job")
public class TaskJob implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("job_code")
    private String jobCode;

    @TableField("job_name")
    private String jobName;

    @TableField("job_desc")
    private String jobDesc;

    @TableField("biz_type")
    private String bizType;

    @TableField("biz_id")
    private String bizId;

    @TableField("bean_name")
    private String beanName;

    @TableField("method_name")
    private String methodName;

    @TableField("method_param_types")
    private String methodParamTypes;

    @TableField("method_params")
    private String methodParams;

    @TableField("execute_status")
    private Integer executeStatus;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("max_retry_count")
    private Integer maxRetryCount;

    @TableField("retry_interval_seconds")
    private Integer retryIntervalSeconds;

    @TableField("next_execute_time")
    private LocalDateTime nextExecuteTime;

    @TableField("execute_start_time")
    private LocalDateTime executeStartTime;

    @TableField("lock_time")
    private LocalDateTime lockTime;

    @TableField("lock_owner")
    private String lockOwner;

    @TableField("fail_reason")
    private String failReason;

    @TableField("version")
    private Integer version;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
```

文件位置：`src/main/java/io/github/atengk/task/entity/TaskJobLog.java`

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
 * 数据库驱动任务执行日志表
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Getter
@Setter
@ToString
@TableName("task_job_log")
public class TaskJobLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("job_id")
    private Long jobId;

    @TableField("job_code")
    private String jobCode;

    @TableField("biz_type")
    private String bizType;

    @TableField("lock_owner")
    private String lockOwner;

    @TableField("execute_time")
    private LocalDateTime executeTime;

    @TableField("execute_status")
    private Integer executeStatus;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("execute_duration")
    private Long executeDuration;

    @TableField("error_message")
    private String errorMessage;

    @TableField("create_time")
    private LocalDateTime createTime;
}
```

### Mapper 与 Service

Mapper 和 Service 使用 MyBatis-Plus 通用能力即可满足本方案。

文件位置：`src/main/java/io/github/atengk/task/mapper/TaskJobMapper.java`

```java
package io.github.atengk.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.task.entity.TaskJob;

/**
 * 数据库驱动任务 Mapper
 *
 * @author Ateng
 * @since 2026-05-12
 */
public interface TaskJobMapper extends BaseMapper<TaskJob> {
}
```

文件位置：`src/main/java/io/github/atengk/task/mapper/TaskJobLogMapper.java`

```java
package io.github.atengk.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.task.entity.TaskJobLog;

/**
 * 数据库驱动任务执行日志 Mapper
 *
 * @author Ateng
 * @since 2026-05-12
 */
public interface TaskJobLogMapper extends BaseMapper<TaskJobLog> {
}
```

文件位置：`src/main/java/io/github/atengk/task/service/ITaskJobService.java`

```java
package io.github.atengk.task.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.atengk.task.entity.TaskJob;

/**
 * 数据库驱动任务 Service
 *
 * @author Ateng
 * @since 2026-05-12
 */
public interface ITaskJobService extends IService<TaskJob> {
}
```

文件位置：`src/main/java/io/github/atengk/task/service/ITaskJobLogService.java`

```java
package io.github.atengk.task.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.atengk.task.entity.TaskJobLog;

/**
 * 数据库驱动任务执行日志 Service
 *
 * @author Ateng
 * @since 2026-05-12
 */
public interface ITaskJobLogService extends IService<TaskJobLog> {
}
```

文件位置：`src/main/java/io/github/atengk/task/service/impl/TaskJobServiceImpl.java`

```java
package io.github.atengk.task.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.task.entity.TaskJob;
import io.github.atengk.task.mapper.TaskJobMapper;
import io.github.atengk.task.service.ITaskJobService;
import org.springframework.stereotype.Service;

/**
 * 数据库驱动任务 Service 实现
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Service
public class TaskJobServiceImpl extends ServiceImpl<TaskJobMapper, TaskJob> implements ITaskJobService {
}
```

文件位置：`src/main/java/io/github/atengk/task/service/impl/TaskJobLogServiceImpl.java`

```java
package io.github.atengk.task.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.task.entity.TaskJobLog;
import io.github.atengk.task.mapper.TaskJobLogMapper;
import io.github.atengk.task.service.ITaskJobLogService;
import org.springframework.stereotype.Service;

/**
 * 数据库驱动任务执行日志 Service 实现
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Service
public class TaskJobLogServiceImpl extends ServiceImpl<TaskJobLogMapper, TaskJobLog> implements ITaskJobLogService {
}
```

### 反射调用工具

反射调用工具负责把数据库中的 JSON 参数转换为目标方法需要的 Java 类型。这里额外处理了基础类型、包装类型、字符串、复杂对象以及 Spring AOP 代理类。

文件位置：`src/main/java/io/github/atengk/task/util/ReflectInvokeUtil.java`

```java
package io.github.atengk.task.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import org.springframework.aop.framework.AopProxyUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * 反射调用工具类
 *
 * @author Ateng
 * @since 2026-05-12
 */
public class ReflectInvokeUtil {

    private static final Map<String, Class<?>> PRIMITIVE_TYPE_MAP = MapUtil.<String, Class<?>>builder()
            .put("boolean", boolean.class)
            .put("byte", byte.class)
            .put("short", short.class)
            .put("int", int.class)
            .put("long", long.class)
            .put("float", float.class)
            .put("double", double.class)
            .put("char", char.class)
            .build();

    private ReflectInvokeUtil() {
    }

    /**
     * 反射调用指定 Bean 方法
     *
     * @param bean           Spring Bean 实例
     * @param methodName     方法名
     * @param paramTypesJson 参数类型 JSON 数组
     * @param paramsJson     参数值 JSON 数组
     * @return 方法返回值
     */
    public static Object invoke(Object bean, String methodName, String paramTypesJson, String paramsJson) {
        if (bean == null) {
            throw new IllegalArgumentException("反射调用失败：Bean 不能为空");
        }
        if (StrUtil.isBlank(methodName)) {
            throw new IllegalArgumentException("反射调用失败：方法名不能为空");
        }

        Class<?>[] paramTypes = buildParamTypes(paramTypesJson);
        Object[] args = buildArgs(paramTypes, paramsJson);

        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
        Method method = ReflectUtil.getMethod(targetClass, methodName, paramTypes);
        if (method == null) {
            throw new IllegalArgumentException(StrUtil.format(
                    "反射调用失败：未找到方法，class={}, method={}, paramTypes={}",
                    targetClass.getName(), methodName, paramTypesJson
            ));
        }

        try {
            return ReflectUtil.invoke(bean, method, args);
        } catch (Exception ex) {
            throw new RuntimeException(StrUtil.format(
                    "反射调用失败：class={}, method={}",
                    targetClass.getName(), methodName
            ), ex);
        }
    }

    /**
     * 构建参数类型数组
     *
     * @param paramTypesJson 参数类型 JSON
     * @return 参数类型数组
     */
    private static Class<?>[] buildParamTypes(String paramTypesJson) {
        if (StrUtil.isBlank(paramTypesJson) || "[]".equals(StrUtil.trim(paramTypesJson))) {
            return new Class<?>[0];
        }

        List<String> typeList = JSONUtil.toBean(paramTypesJson, new TypeReference<List<String>>() {
        }, false);

        return typeList.stream()
                .map(ReflectInvokeUtil::loadClass)
                .toArray(Class<?>[]::new);
    }

    /**
     * 构建参数值数组
     *
     * @param paramTypes 参数类型数组
     * @param paramsJson 参数值 JSON
     * @return 参数值数组
     */
    private static Object[] buildArgs(Class<?>[] paramTypes, String paramsJson) {
        if (StrUtil.isBlank(paramsJson) || "[]".equals(StrUtil.trim(paramsJson))) {
            if (paramTypes.length > 0) {
                throw new IllegalArgumentException(StrUtil.format("参数数量不匹配：期望 {} 个，实际 0 个", paramTypes.length));
            }
            return new Object[0];
        }

        List<Object> paramList = JSONUtil.toBean(paramsJson, new TypeReference<List<Object>>() {
        }, false);

        if (paramList.size() != paramTypes.length) {
            throw new IllegalArgumentException(StrUtil.format(
                    "参数数量不匹配：期望 {} 个，实际 {} 个",
                    paramTypes.length, paramList.size()
            ));
        }

        Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            args[i] = convertArg(paramTypes[i], paramList.get(i));
        }

        return args;
    }

    /**
     * 转换单个参数
     *
     * @param targetType 目标类型
     * @param value      参数值
     * @return 转换后的参数
     */
    private static Object convertArg(Class<?> targetType, Object value) {
        if (value == null) {
            return null;
        }

        Class<?> wrapperType = targetType.isPrimitive()
                ? ClassUtil.wrap(targetType)
                : targetType;

        if (ClassUtil.isBasicType(wrapperType) || CharSequence.class.isAssignableFrom(wrapperType)) {
            return Convert.convert(wrapperType, value);
        }

        return JSONUtil.toBean(JSONUtil.parseObj(value), wrapperType);
    }

    /**
     * 加载参数类型
     *
     * @param className 类全限定名或基础类型名
     * @return Class 对象
     */
    private static Class<?> loadClass(String className) {
        if (StrUtil.isBlank(className)) {
            throw new IllegalArgumentException("参数类型不能为空");
        }

        String trimmedClassName = StrUtil.trim(className);
        Class<?> primitiveType = PRIMITIVE_TYPE_MAP.get(trimmedClassName);
        if (primitiveType != null) {
            return primitiveType;
        }

        try {
            return Class.forName(trimmedClassName);
        } catch (Exception ex) {
            throw new RuntimeException(StrUtil.format("加载参数类型失败：{}", trimmedClassName), ex);
        }
    }
}
```

### 调用白名单

调用白名单用于限制数据库任务只能调用配置允许的 Bean 方法。生产环境不建议允许任意 Bean 或任意方法。

文件位置：`src/main/java/io/github/atengk/task/guard/TaskJobInvokeGuard.java`

```java
package io.github.atengk.task.guard;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.task.config.TaskJobProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 任务调用白名单校验器
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Component
@RequiredArgsConstructor
public class TaskJobInvokeGuard {

    private final TaskJobProperties taskJobProperties;

    /**
     * 校验 Bean 方法是否允许被任务执行器调用
     *
     * @param beanName   Spring Bean 名称
     * @param methodName 方法名
     */
    public void checkAllowed(String beanName, String methodName) {
        if (StrUtil.isBlank(beanName) || StrUtil.isBlank(methodName)) {
            throw new IllegalArgumentException("任务调用目标不能为空");
        }

        Map<String, List<String>> allowList = taskJobProperties.getInvokeAllowList();
        if (CollUtil.isEmpty(allowList)) {
            throw new IllegalStateException("未配置任务调用白名单，拒绝执行任务");
        }

        List<String> methods = allowList.get(beanName);
        if (CollUtil.isEmpty(methods)) {
            throw new IllegalStateException(StrUtil.format("Bean 未加入任务调用白名单：{}", beanName));
        }

        if (!methods.contains("*") && !methods.contains(methodName)) {
            throw new IllegalStateException(StrUtil.format("方法未加入任务调用白名单：{}#{}", beanName, methodName));
        }
    }
}
```

### 任务创建参数

任务创建参数由业务代码构造，不建议直接从前端传入 `beanName` 和 `methodName`。对外接口应封装成具体业务动作，例如“创建订单同步任务”。

文件位置：`src/main/java/io/github/atengk/task/dto/TaskJobCreateParam.java`

```java
package io.github.atengk.task.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务创建参数
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Data
public class TaskJobCreateParam {

    /**
     * 任务编码，为空时自动生成
     */
    private String jobCode;

    @NotBlank(message = "任务名称不能为空")
    private String jobName;

    /**
     * 任务描述
     */
    private String jobDesc;

    @NotBlank(message = "业务类型不能为空")
    private String bizType;

    /**
     * 业务ID
     */
    private String bizId;

    @NotBlank(message = "Bean 名称不能为空")
    private String beanName;

    @NotBlank(message = "方法名不能为空")
    private String methodName;

    /**
     * 方法参数类型
     */
    private List<String> methodParamTypes;

    /**
     * 方法参数值
     */
    private List<Object> methodParams;

    /**
     * 下次执行时间，为空时立即执行
     */
    private LocalDateTime nextExecuteTime;

    @Min(value = 1, message = "最大失败次数必须大于 0")
    private Integer maxRetryCount = 3;

    @Min(value = 1, message = "重试间隔必须大于 0")
    private Integer retryIntervalSeconds = 60;
}
```

### 任务提交服务

任务提交服务负责统一生成任务编码、序列化参数、设置默认状态，并在创建前校验白名单和参数数量。

文件位置：`src/main/java/io/github/atengk/task/service/TaskJobSubmitService.java`

```java
package io.github.atengk.task.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.task.dto.TaskJobCreateParam;
import io.github.atengk.task.entity.TaskJob;
import io.github.atengk.task.enums.TaskExecuteStatusEnum;
import io.github.atengk.task.guard.TaskJobInvokeGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务提交服务
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class TaskJobSubmitService {

    private final ITaskJobService taskJobService;
    private final TaskJobInvokeGuard taskJobInvokeGuard;

    /**
     * 创建任务
     *
     * @param param 创建参数
     * @return 任务编码
     */
    public String submit(@Valid TaskJobCreateParam param) {
        taskJobInvokeGuard.checkAllowed(param.getBeanName(), param.getMethodName());
        checkParamCount(param.getMethodParamTypes(), param.getMethodParams());

        TaskJob taskJob = new TaskJob();
        taskJob.setJobCode(StrUtil.blankToDefault(param.getJobCode(), IdUtil.fastSimpleUUID()));
        taskJob.setJobName(param.getJobName());
        taskJob.setJobDesc(param.getJobDesc());
        taskJob.setBizType(param.getBizType());
        taskJob.setBizId(param.getBizId());
        taskJob.setBeanName(param.getBeanName());
        taskJob.setMethodName(param.getMethodName());
        taskJob.setMethodParamTypes(JSONUtil.toJsonStr(ObjectUtil.defaultIfNull(param.getMethodParamTypes(), CollUtil.newArrayList())));
        taskJob.setMethodParams(JSONUtil.toJsonStr(ObjectUtil.defaultIfNull(param.getMethodParams(), CollUtil.newArrayList())));
        taskJob.setExecuteStatus(TaskExecuteStatusEnum.PENDING.getCode());
        taskJob.setRetryCount(0);
        taskJob.setMaxRetryCount(ObjectUtil.defaultIfNull(param.getMaxRetryCount(), 3));
        taskJob.setRetryIntervalSeconds(ObjectUtil.defaultIfNull(param.getRetryIntervalSeconds(), 60));
        taskJob.setNextExecuteTime(ObjectUtil.defaultIfNull(param.getNextExecuteTime(), LocalDateTime.now()));
        taskJob.setVersion(0);

        taskJobService.save(taskJob);

        log.info("任务创建成功，jobCode={}，bizType={}，bizId={}",
                taskJob.getJobCode(), taskJob.getBizType(), taskJob.getBizId());

        return taskJob.getJobCode();
    }

    /**
     * 校验参数数量
     *
     * @param methodParamTypes 参数类型
     * @param methodParams     参数值
     */
    private void checkParamCount(List<String> methodParamTypes, List<Object> methodParams) {
        int typeSize = CollUtil.size(methodParamTypes);
        int paramSize = CollUtil.size(methodParams);
        if (typeSize != paramSize) {
            throw new IllegalArgumentException(StrUtil.format("任务参数数量不匹配：参数类型 {} 个，参数值 {} 个", typeSize, paramSize));
        }
    }
}
```

### 事务服务

事务服务负责抢占任务、恢复超时任务、记录执行日志、标记成功、处理失败。这里使用条件更新和 `lock_owner` 防止并发覆盖。

文件位置：`src/main/java/io/github/atengk/task/service/TaskJobTxService.java`

```java
package io.github.atengk.task.service;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.task.entity.TaskJob;
import io.github.atengk.task.entity.TaskJobLog;
import io.github.atengk.task.enums.TaskExecuteStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 任务短事务服务
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskJobTxService {

    private final ITaskJobService taskJobService;
    private final ITaskJobLogService taskJobLogService;

    /**
     * 抢占任务
     *
     * @param job                任务
     * @param lockOwner          锁持有者
     * @param lockTimeoutMinutes 锁超时分钟数
     * @return 是否抢占成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean lockJob(TaskJob job, String lockOwner, int lockTimeoutMinutes) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timeoutTime = now.minusMinutes(lockTimeoutMinutes);

        boolean updated = taskJobService.lambdaUpdate()
                .set(TaskJob::getExecuteStatus, TaskExecuteStatusEnum.RUNNING.getCode())
                .set(TaskJob::getExecuteStartTime, now)
                .set(TaskJob::getLockTime, now)
                .set(TaskJob::getLockOwner, lockOwner)
                .set(TaskJob::getVersion, ObjectUtil.defaultIfNull(job.getVersion(), 0) + 1)
                .eq(TaskJob::getId, job.getId())
                .eq(TaskJob::getVersion, ObjectUtil.defaultIfNull(job.getVersion(), 0))
                .and(wrapper -> wrapper
                        .eq(TaskJob::getExecuteStatus, TaskExecuteStatusEnum.PENDING.getCode())
                        .le(TaskJob::getNextExecuteTime, now)
                        .or()
                        .eq(TaskJob::getExecuteStatus, TaskExecuteStatusEnum.RUNNING.getCode())
                        .le(TaskJob::getLockTime, timeoutTime))
                .update();

        if (updated) {
            log.info("任务抢占成功，jobCode={}，lockOwner={}", job.getJobCode(), lockOwner);
        } else {
            log.warn("任务抢占失败，jobCode={}，version={}", job.getJobCode(), job.getVersion());
        }

        return updated;
    }

    /**
     * 恢复超时的执行中任务
     *
     * @param lockTimeoutMinutes 锁超时分钟数
     * @return 恢复数量
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean recoverTimeoutRunningJob(int lockTimeoutMinutes) {
        LocalDateTime timeoutTime = LocalDateTime.now().minusMinutes(lockTimeoutMinutes);

        boolean updated = taskJobService.lambdaUpdate()
                .set(TaskJob::getExecuteStatus, TaskExecuteStatusEnum.PENDING.getCode())
                .set(TaskJob::getLockTime, null)
                .set(TaskJob::getLockOwner, null)
                .set(TaskJob::getNextExecuteTime, LocalDateTime.now())
                .eq(TaskJob::getExecuteStatus, TaskExecuteStatusEnum.RUNNING.getCode())
                .le(TaskJob::getLockTime, timeoutTime)
                .update();

        if (updated) {
            log.warn("已恢复超时执行中的任务，timeoutTime={}", timeoutTime);
        }

        return updated;
    }

    /**
     * 标记任务成功
     *
     * @param job       任务
     * @param lockOwner 锁持有者
     */
    @Transactional(rollbackFor = Exception.class)
    public void markSuccess(TaskJob job, String lockOwner) {
        boolean updated = taskJobService.lambdaUpdate()
                .set(TaskJob::getExecuteStatus, TaskExecuteStatusEnum.SUCCESS.getCode())
                .set(TaskJob::getFailReason, null)
                .set(TaskJob::getLockTime, null)
                .set(TaskJob::getLockOwner, null)
                .eq(TaskJob::getId, job.getId())
                .eq(TaskJob::getExecuteStatus, TaskExecuteStatusEnum.RUNNING.getCode())
                .eq(TaskJob::getLockOwner, lockOwner)
                .update();

        if (updated) {
            log.info("任务标记成功，jobCode={}", job.getJobCode());
        } else {
            log.warn("任务成功状态更新失败，可能锁已变化，jobCode={}，lockOwner={}", job.getJobCode(), lockOwner);
        }
    }

    /**
     * 处理任务失败
     *
     * @param job           任务
     * @param lockOwner     锁持有者
     * @param error         异常
     * @param retryCount    当前失败次数
     * @param maxRetryCount 最大失败次数
     * @param retryInterval 重试间隔秒数
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleFail(TaskJob job,
                           String lockOwner,
                           Throwable error,
                           int retryCount,
                           int maxRetryCount,
                           int retryInterval) {
        int nextRetryCount = retryCount + 1;
        boolean finalFail = nextRetryCount >= maxRetryCount;
        LocalDateTime nextExecuteTime = finalFail ? null : LocalDateTime.now().plusSeconds(retryInterval);
        String errorMessage = ExceptionUtil.stacktraceToString(error, -1);

        boolean updated = taskJobService.lambdaUpdate()
                .set(TaskJob::getRetryCount, nextRetryCount)
                .set(TaskJob::getExecuteStatus, finalFail
                        ? TaskExecuteStatusEnum.FAILED.getCode()
                        : TaskExecuteStatusEnum.PENDING.getCode())
                .set(TaskJob::getFailReason, errorMessage)
                .set(TaskJob::getNextExecuteTime, nextExecuteTime)
                .set(TaskJob::getLockTime, null)
                .set(TaskJob::getLockOwner, null)
                .eq(TaskJob::getId, job.getId())
                .eq(TaskJob::getExecuteStatus, TaskExecuteStatusEnum.RUNNING.getCode())
                .eq(TaskJob::getLockOwner, lockOwner)
                .update();

        if (!updated) {
            log.warn("任务失败状态更新失败，可能锁已变化，jobCode={}，lockOwner={}", job.getJobCode(), lockOwner);
            return;
        }

        if (finalFail) {
            log.error("任务达到最大失败次数，jobCode={}，retry={}/{}",
                    job.getJobCode(), nextRetryCount, maxRetryCount);
        } else {
            log.warn("任务执行失败，等待重试，jobCode={}，retry={}/{}，nextExecuteTime={}",
                    job.getJobCode(), nextRetryCount, maxRetryCount, nextExecuteTime);
        }
    }

    /**
     * 记录执行日志
     *
     * @param job       任务
     * @param lockOwner 锁持有者
     * @param success   是否成功
     * @param duration  执行耗时
     * @param error     异常
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveExecuteLog(TaskJob job, String lockOwner, boolean success, long duration, Throwable error) {
        TaskJobLog taskJobLog = new TaskJobLog();
        taskJobLog.setJobId(job.getId());
        taskJobLog.setJobCode(job.getJobCode());
        taskJobLog.setBizType(job.getBizType());
        taskJobLog.setLockOwner(lockOwner);
        taskJobLog.setExecuteTime(LocalDateTime.now());
        taskJobLog.setExecuteStatus(success ? TaskExecuteStatusEnum.SUCCESS.getCode() : TaskExecuteStatusEnum.FAILED.getCode());
        taskJobLog.setRetryCount(ObjectUtil.defaultIfNull(job.getRetryCount(), 0));
        taskJobLog.setExecuteDuration(duration);
        taskJobLog.setErrorMessage(error == null ? null : StrUtil.maxLength(ExceptionUtil.stacktraceToString(error, -1), 60000));

        taskJobLogService.save(taskJobLog);

        log.info("任务执行日志已写入，jobCode={}，success={}，duration={}ms",
                job.getJobCode(), success, duration);
    }
}
```

### 执行器

执行器负责扫描、抢占和调用业务方法。注意业务方法执行不放在数据库事务中，避免长事务占用连接和锁。

文件位置：`src/main/java/io/github/atengk/task/executor/TaskExecutor.java`

```java
package io.github.atengk.task.executor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.task.config.TaskJobProperties;
import io.github.atengk.task.entity.TaskJob;
import io.github.atengk.task.enums.TaskExecuteStatusEnum;
import io.github.atengk.task.guard.TaskJobInvokeGuard;
import io.github.atengk.task.service.ITaskJobService;
import io.github.atengk.task.service.TaskJobTxService;
import io.github.atengk.task.util.ReflectInvokeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据库驱动任务执行器
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskExecutor {

    private final ITaskJobService taskJobService;
    private final TaskJobTxService taskJobTxService;
    private final TaskJobInvokeGuard taskJobInvokeGuard;
    private final TaskJobProperties taskJobProperties;

    /**
     * 执行指定任务编码
     *
     * @param jobCode 任务编码
     */
    public void executeByCode(String jobCode) {
        TaskJob job = taskJobService.lambdaQuery()
                .eq(TaskJob::getJobCode, jobCode)
                .one();

        if (job == null) {
            log.warn("任务不存在，jobCode={}", jobCode);
            return;
        }

        execute(job);
    }

    /**
     * 扫描并执行到期任务
     */
    public void scanAndExecute() {
        int pageSize = ObjectUtil.defaultIfNull(taskJobProperties.getExecutor().getScanPageSize(), 100);
        LocalDateTime now = LocalDateTime.now();

        Page<TaskJob> page = new Page<>(1, pageSize);
        Page<TaskJob> result = taskJobService.lambdaQuery()
                .eq(TaskJob::getExecuteStatus, TaskExecuteStatusEnum.PENDING.getCode())
                .le(TaskJob::getNextExecuteTime, now)
                .orderByAsc(TaskJob::getNextExecuteTime)
                .page(page);

        List<TaskJob> records = result.getRecords();
        if (CollUtil.isEmpty(records)) {
            return;
        }

        log.info("扫描到待执行任务，数量={}", records.size());

        for (TaskJob job : records) {
            try {
                execute(job);
            } catch (Exception ex) {
                log.error("任务扫描执行异常，jobCode={}", job.getJobCode(), ex);
            }
        }
    }

    /**
     * 根据业务类型扫描并执行任务
     *
     * @param bizType 业务类型
     */
    public void scanAndExecuteByBizType(String bizType) {
        int pageSize = ObjectUtil.defaultIfNull(taskJobProperties.getExecutor().getScanPageSize(), 100);
        LocalDateTime now = LocalDateTime.now();

        Page<TaskJob> page = new Page<>(1, pageSize);
        Page<TaskJob> result = taskJobService.lambdaQuery()
                .eq(TaskJob::getBizType, bizType)
                .eq(TaskJob::getExecuteStatus, TaskExecuteStatusEnum.PENDING.getCode())
                .le(TaskJob::getNextExecuteTime, now)
                .orderByAsc(TaskJob::getNextExecuteTime)
                .page(page);

        List<TaskJob> records = result.getRecords();
        if (CollUtil.isEmpty(records)) {
            return;
        }

        log.info("按业务类型扫描到待执行任务，bizType={}，数量={}", bizType, records.size());

        for (TaskJob job : records) {
            try {
                execute(job);
            } catch (Exception ex) {
                log.error("按业务类型执行任务异常，bizType={}，jobCode={}", bizType, job.getJobCode(), ex);
            }
        }
    }

    /**
     * 执行任务
     *
     * @param job 任务
     */
    public void execute(TaskJob job) {
        if (job == null) {
            log.warn("任务为空，跳过执行");
            return;
        }
        if (!canExecute(job)) {
            log.info("任务当前不可执行，jobCode={}，status={}", job.getJobCode(), job.getExecuteStatus());
            return;
        }

        String lockOwner = IdUtil.fastSimpleUUID();
        int lockTimeoutMinutes = ObjectUtil.defaultIfNull(taskJobProperties.getExecutor().getLockTimeoutMinutes(), 5);

        boolean locked = taskJobTxService.lockJob(job, lockOwner, lockTimeoutMinutes);
        if (!locked) {
            return;
        }

        doExecute(job, lockOwner);
    }

    /**
     * 判断任务是否可执行
     *
     * @param job 任务
     * @return 是否可执行
     */
    private boolean canExecute(TaskJob job) {
        if (TaskExecuteStatusEnum.SUCCESS.getCode() == job.getExecuteStatus()) {
            return false;
        }
        if (TaskExecuteStatusEnum.FAILED.getCode() == job.getExecuteStatus()) {
            return false;
        }
        if (job.getNextExecuteTime() != null && job.getNextExecuteTime().isAfter(LocalDateTime.now())) {
            return false;
        }
        return TaskExecuteStatusEnum.PENDING.getCode() == job.getExecuteStatus()
                || TaskExecuteStatusEnum.RUNNING.getCode() == job.getExecuteStatus();
    }

    /**
     * 真正执行任务
     *
     * @param job       任务
     * @param lockOwner 锁持有者
     */
    private void doExecute(TaskJob job, String lockOwner) {
        boolean success = false;
        Throwable error = null;
        long startTime = System.currentTimeMillis();

        try {
            taskJobInvokeGuard.checkAllowed(job.getBeanName(), job.getMethodName());

            log.info("开始执行任务，jobCode={}，bean={}，method={}，lockOwner={}",
                    job.getJobCode(), job.getBeanName(), job.getMethodName(), lockOwner);

            Object bean = SpringUtil.getBean(job.getBeanName());
            ReflectInvokeUtil.invoke(bean, job.getMethodName(), job.getMethodParamTypes(), job.getMethodParams());

            success = true;
        } catch (Throwable ex) {
            error = ex;
            log.error("任务执行异常，jobCode={}，lockOwner={}", job.getJobCode(), lockOwner, ex);
        }

        long duration = System.currentTimeMillis() - startTime;

        try {
            taskJobTxService.saveExecuteLog(job, lockOwner, success, duration, error);
        } catch (Exception logEx) {
            log.error("任务执行日志写入失败，jobCode={}，lockOwner={}", job.getJobCode(), lockOwner, logEx);
        }

        if (success) {
            taskJobTxService.markSuccess(job, lockOwner);
            return;
        }

        int retryCount = ObjectUtil.defaultIfNull(job.getRetryCount(), 0);
        int maxRetryCount = ObjectUtil.defaultIfNull(job.getMaxRetryCount(), 3);
        int retryInterval = ObjectUtil.defaultIfNull(job.getRetryIntervalSeconds(), 60);
        taskJobTxService.handleFail(job, lockOwner, error, retryCount, maxRetryCount, retryInterval);
    }
}
```

### 定时扫描器

定时扫描器负责周期性恢复超时锁并扫描待执行任务。如果项目已经接入 XXL-JOB，也可以不使用 `@Scheduled`，改为由 XXL-JOB 调用 `taskExecutor.scanAndExecute()`。

文件位置：`src/main/java/io/github/atengk/task/executor/TaskJobScheduleExecutor.java`

```java
package io.github.atengk.task.executor;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.task.config.TaskJobProperties;
import io.github.atengk.task.service.TaskJobTxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 数据库任务定时扫描器
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskJobScheduleExecutor {

    private final TaskExecutor taskExecutor;
    private final TaskJobTxService taskJobTxService;
    private final TaskJobProperties taskJobProperties;

    /**
     * 定时扫描待执行任务
     */
    @Scheduled(fixedDelayString = "${task-job.executor.scan-fixed-delay-ms:5000}")
    public void scan() {
        if (BooleanUtil.isFalse(taskJobProperties.getExecutor().getEnabled())) {
            return;
        }

        int lockTimeoutMinutes = ObjectUtil.defaultIfNull(taskJobProperties.getExecutor().getLockTimeoutMinutes(), 5);

        try {
            taskJobTxService.recoverTimeoutRunningJob(lockTimeoutMinutes);
            taskExecutor.scanAndExecute();
        } catch (Exception ex) {
            log.error("数据库任务扫描异常", ex);
        }
    }
}
```

## 业务接入示例

本节给出订单同步任务示例。真实项目中建议每类业务封装一个明确的提交方法，不要让调用方直接传入 Bean 名称和方法名。

### 订单参数对象

`OrderDTO` 用于演示复杂对象参数。执行器会从 JSON 自动反序列化为该类型。

文件位置：`src/main/java/io/github/atengk/task/dto/OrderDTO.java`

```java
package io.github.atengk.task.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单参数对象
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Data
public class OrderDTO {

    private Long orderId;

    private String userName;

    private BigDecimal amount;
}
```

### 订单任务服务

被任务执行器调用的方法必须是幂等的。下面示例只打印日志，真实项目中应根据业务唯一键判断是否已处理。

文件位置：`src/main/java/io/github/atengk/task/service/OrderTaskService.java`

```java
package io.github.atengk.task.service;

import cn.hutool.json.JSONUtil;
import io.github.atengk.task.dto.OrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单任务服务
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Slf4j
@Service("orderTaskService")
public class OrderTaskService {

    /**
     * 无参任务
     */
    public void noParamTask() {
        log.info("执行无参任务成功");
    }

    /**
     * 同步订单
     *
     * @param orderId  订单ID
     * @param operator 操作人
     */
    public void syncOrder(Long orderId, String operator) {
        log.info("开始同步订单，orderId={}，operator={}", orderId, operator);

        // 真实项目中应先根据 orderId 查询业务状态，保证重复执行不会产生副作用
        log.info("订单同步完成，orderId={}", orderId);
    }

    /**
     * 创建订单
     *
     * @param orderDTO 订单参数
     */
    public void createOrder(OrderDTO orderDTO) {
        log.info("开始创建订单，orderDTO={}", JSONUtil.toJsonStr(orderDTO));

        // 真实项目中应根据业务唯一键做幂等校验
        log.info("订单创建完成，orderId={}", orderDTO.getOrderId());
    }
}
```

### 测试接口

测试接口只用于本地验证，不建议直接暴露到生产环境。如果生产需要人工补偿入口，应做权限控制、审计日志和固定业务动作封装。

文件位置：`src/main/java/io/github/atengk/task/controller/TaskTestController.java`

```java
package io.github.atengk.task.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import io.github.atengk.task.dto.OrderDTO;
import io.github.atengk.task.dto.TaskJobCreateParam;
import io.github.atengk.task.executor.TaskExecutor;
import io.github.atengk.task.service.TaskJobSubmitService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 数据库任务测试接口
 *
 * @author Ateng
 * @since 2026-05-12
 */
@RestController
@RequestMapping("/task/test")
@RequiredArgsConstructor
public class TaskTestController {

    private final TaskExecutor taskExecutor;
    private final TaskJobSubmitService taskJobSubmitService;

    /**
     * 创建基础类型参数任务
     *
     * @return 任务编码
     */
    @PostMapping("/createSyncOrderJob")
    public String createSyncOrderJob() {
        TaskJobCreateParam param = new TaskJobCreateParam();
        param.setJobCode("SYNC_ORDER_" + IdUtil.fastSimpleUUID());
        param.setJobName("同步订单任务");
        param.setJobDesc("演示基础类型参数任务");
        param.setBizType("ORDER_SYNC");
        param.setBizId("10001");
        param.setBeanName("orderTaskService");
        param.setMethodName("syncOrder");
        param.setMethodParamTypes(CollUtil.newArrayList(
                "java.lang.Long",
                "java.lang.String"
        ));
        param.setMethodParams(CollUtil.newArrayList(
                10001L,
                "system"
        ));
        param.setNextExecuteTime(LocalDateTime.now());
        param.setMaxRetryCount(3);
        param.setRetryIntervalSeconds(60);

        return taskJobSubmitService.submit(param);
    }

    /**
     * 创建复杂对象参数任务
     *
     * @return 任务编码
     */
    @PostMapping("/createOrderJob")
    public String createOrderJob() {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderId(10002L);
        orderDTO.setUserName("Ateng");
        orderDTO.setAmount(new BigDecimal("99.99"));

        TaskJobCreateParam param = new TaskJobCreateParam();
        param.setJobCode("CREATE_ORDER_" + IdUtil.fastSimpleUUID());
        param.setJobName("创建订单任务");
        param.setJobDesc("演示复杂对象参数任务");
        param.setBizType("ORDER_CREATE");
        param.setBizId(String.valueOf(orderDTO.getOrderId()));
        param.setBeanName("orderTaskService");
        param.setMethodName("createOrder");
        param.setMethodParamTypes(CollUtil.newArrayList(
                "io.github.atengk.task.dto.OrderDTO"
        ));
        param.setMethodParams(CollUtil.newArrayList(orderDTO));
        param.setNextExecuteTime(LocalDateTime.now());
        param.setMaxRetryCount(3);
        param.setRetryIntervalSeconds(60);

        return taskJobSubmitService.submit(param);
    }

    /**
     * 手动执行指定任务
     *
     * @param jobCode 任务编码
     * @return 执行结果
     */
    @GetMapping("/executeByCode")
    public String executeByCode(@RequestParam String jobCode) {
        taskExecutor.executeByCode(jobCode);
        return "执行完成";
    }

    /**
     * 手动扫描所有到期任务
     *
     * @return 扫描结果
     */
    @PostMapping("/scan")
    public String scan() {
        taskExecutor.scanAndExecute();
        return "扫描完成";
    }
}
```

## 使用方式

业务接入时优先调用 `TaskJobSubmitService` 创建任务，执行由定时扫描器自动完成。对于已有调度平台的项目，可以关闭本方案自带扫描器，由外部调度平台定时触发扫描方法。

### 创建任务

启动项目后调用下面接口创建基础类型参数任务。

```bash
curl -X POST "http://127.0.0.1:8080/task/test/createSyncOrderJob"
```

返回示例：

```text
SYNC_ORDER_2f6a9c73f0dd4d7b8a0f4e2b2d0f7f44
```

创建复杂对象参数任务。

```bash
curl -X POST "http://127.0.0.1:8080/task/test/createOrderJob"
```

返回示例：

```text
CREATE_ORDER_a13f3934e92c4d4e8db19e10c4a3958a
```

### 手动执行指定任务

如果需要立即验证某个任务，可以根据任务编码手动触发。

```bash
curl -X GET "http://127.0.0.1:8080/task/test/executeByCode?jobCode=SYNC_ORDER_2f6a9c73f0dd4d7b8a0f4e2b2d0f7f44"
```

### 手动扫描任务

如果关闭了定时扫描器，可以由外部调度平台定时调用扫描方法。

```bash
curl -X POST "http://127.0.0.1:8080/task/test/scan"
```

### 查询任务状态

通过任务表查看当前任务状态。

```sql
SELECT
    id,
    job_code,
    job_name,
    biz_type,
    biz_id,
    execute_status,
    retry_count,
    max_retry_count,
    next_execute_time,
    lock_time,
    lock_owner,
    fail_reason,
    create_time,
    update_time
FROM task_job
ORDER BY id DESC
LIMIT 20;
```

通过日志表查看每次执行记录。

```sql
SELECT
    job_code,
    biz_type,
    execute_status,
    retry_count,
    execute_duration,
    LEFT(error_message, 1000) AS error_message,
    execute_time
FROM task_job_log
WHERE job_code = 'SYNC_ORDER_2f6a9c73f0dd4d7b8a0f4e2b2d0f7f44'
ORDER BY id DESC;
```

## 人工干预

任务最终失败后会停留在 `FAILED` 状态。人工确认问题修复后，可以把任务重新改为待执行。

### 重新投递失败任务

重新投递前应先确认业务方法幂等，避免重复扣款、重复发货、重复通知等问题。

```sql
UPDATE task_job
SET execute_status = 0,
    next_execute_time = NOW(),
    lock_time = NULL,
    lock_owner = NULL,
    fail_reason = NULL,
    update_time = NOW()
WHERE job_code = '需要重新执行的任务编码'
  AND execute_status = 2;
```

### 延迟重试任务

如果外部系统正在恢复，可以把任务延迟到指定时间后再执行。

```sql
UPDATE task_job
SET execute_status = 0,
    next_execute_time = DATE_ADD(NOW(), INTERVAL 10 MINUTE),
    lock_time = NULL,
    lock_owner = NULL,
    update_time = NOW()
WHERE job_code = '需要延迟的任务编码'
  AND execute_status IN (0, 2);
```

### 清理成功任务

成功任务默认保留，便于审计。如果业务量较大，可以按时间窗口归档或清理。

```sql
DELETE FROM task_job
WHERE execute_status = 3
  AND update_time < DATE_SUB(NOW(), INTERVAL 30 DAY);
```

执行删除前建议先确认数量。

```sql
SELECT COUNT(1)
FROM task_job
WHERE execute_status = 3
  AND update_time < DATE_SUB(NOW(), INTERVAL 30 DAY);
```

## 验证方式

完成代码接入后，建议按下面顺序验证，避免只验证成功路径。

### 成功路径验证

先创建一个基础类型参数任务，然后确认任务状态从待执行变为成功。

```bash
curl -X POST "http://127.0.0.1:8080/task/test/createSyncOrderJob"
```

等待扫描器执行后查询：

```sql
SELECT job_code, execute_status, retry_count, fail_reason
FROM task_job
ORDER BY id DESC
LIMIT 5;
```

预期结果：

```text
execute_status = 3
retry_count = 0
fail_reason = NULL
```

### 失败重试验证

把白名单中的方法名临时改成不存在的方法，或者创建一个抛出异常的测试方法，然后观察任务状态。

预期过程：

1. 第一次失败后，`retry_count` 增加为 `1`。
2. 未达到最大失败次数时，状态回到 `PENDING`。
3. `next_execute_time` 被设置为下一次重试时间。
4. 达到最大失败次数后，状态变为 `FAILED`。

查询 SQL：

```sql
SELECT
    job_code,
    execute_status,
    retry_count,
    max_retry_count,
    next_execute_time,
    LEFT(fail_reason, 500) AS fail_reason
FROM task_job
ORDER BY id DESC
LIMIT 10;
```

### 多实例并发验证

启动两个项目实例，同时扫描同一张任务表。观察同一个 `job_code` 是否只产生一次成功业务日志。即使多个实例同时扫描到同一条记录，也应该只有一个实例抢占成功。

```sql
SELECT job_code, execute_status, COUNT(1) AS execute_count
FROM task_job_log
GROUP BY job_code, execute_status
ORDER BY execute_count DESC;
```

### 锁超时恢复验证

手动把某条任务改成执行中，并设置一个过期锁时间。

```sql
UPDATE task_job
SET execute_status = 1,
    lock_time = DATE_SUB(NOW(), INTERVAL 10 MINUTE),
    lock_owner = 'manual-timeout-test',
    update_time = NOW()
WHERE job_code = '需要验证的任务编码';
```

等待扫描器执行后，任务应被恢复为待执行并重新抢占。

```sql
SELECT job_code, execute_status, lock_time, lock_owner, next_execute_time
FROM task_job
WHERE job_code = '需要验证的任务编码';
```

## 生产注意事项

上线前建议重点检查以下内容。该方案虽然轻量，但涉及反射调用和失败重试，必须控制好边界。

1. 业务方法必须幂等。建议以 `biz_type + biz_id` 或业务唯一键判断是否已经处理。
2. 白名单必须启用。不要把任意 `beanName`、`methodName` 暴露给前端或外部系统。
3. 不要在任务方法中执行过长逻辑。长任务建议拆分或转交专业任务平台。
4. 扫描间隔和批量大小要结合数据库压力调整。任务量较大时建议按 `biz_type` 拆分调度。
5. 失败原因可能很长。日志表增长较快时，需要定期清理或归档。
6. 如果任务方法调用外部接口，需要设置超时时间，避免线程长期阻塞。
7. 如果使用多实例部署，所有实例必须连接同一任务数据库，并保持系统时间基本一致。
8. 不建议删除失败任务。失败任务是后续排查和人工补偿的重要依据。
9. 若任务执行成功后必须删除记录，应先确认审计要求，并保留日志表。
10. 如果任务有严格顺序要求，需要额外设计队列分组或业务锁，本方案默认不保证同一业务下的强顺序执行。

## 常见问题

### 为什么成功任务不直接删除

成功任务保留可以支持审计、排查和重复提交识别。直接删除虽然表数据更少，但会丢失任务生命周期信息。推荐保留一段时间后定期清理。

### 为什么不把业务方法执行放进事务

业务方法可能调用外部接口、执行耗时逻辑或触发其他事务。如果把整个执行过程放入数据库事务，会造成长事务、连接占用和锁等待。本方案只把状态更新和日志写入放入短事务。

### 为什么需要 lock_owner

锁超时后，旧执行器可能还在执行。如果新执行器已经抢占并开始执行，旧执行器完成后不应该覆盖新执行器的状态。`lock_owner` 可以保证只有当前锁持有者才能更新本次执行结果。

### 为什么扫描时始终取第一页

执行器会把任务从待执行改为执行中、成功或失败。如果使用递增页码，结果集在扫描过程中会变化，可能导致跳过部分任务。因此每次扫描只取第一页到期任务，由下一轮扫描继续处理后续任务。

### 如何接入 XXL-JOB

关闭本方案自带扫描器：

```yaml
task-job:
  executor:
    enabled: false
```

然后在 XXL-JOB Handler 中调用：

```java
taskExecutor.scanAndExecute();
```

如果任务量较大，可以按业务类型拆分多个 Handler：

```java
taskExecutor.scanAndExecuteByBizType("ORDER_SYNC");
taskExecutor.scanAndExecuteByBizType("ORDER_CREATE");
```

## 小结

该方案的核心是把“任务定义”和“任务执行状态”落到数据库，用条件更新实现多实例抢占，用 `lock_owner` 保护执行结果，用短事务避免长事务风险，用白名单控制反射调用范围。只要业务方法具备幂等性，并且生产环境做好白名单、清理和监控，就可以作为中小型业务系统中的一次性任务和异步补偿任务执行方案。
