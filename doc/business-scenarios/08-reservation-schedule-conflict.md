# 预约排班与时间冲突检测

本文档围绕“预约排班与时间冲突检测”场景进行实现，适用于会议室预约、医生挂号、场馆预约、工位预约、设备预约等需要按资源和时间段占用的业务。该场景的核心价值在于：准确判断时间区间冲突，并在并发预约时避免同一资源被重复占用。原始 README 中也将该场景定位为 Java 后端高含金量业务场景之一，核心难点包括时间段冲突判断、跨天时间处理、资源排班、并发预约、名额限制、取消释放、爽约处理和签到核销。

## 业务场景说明

预约排班系统本质上是在“资源、可预约时间、预约单”三类数据之间建立约束关系。用户只能在资源开放的时间段内预约，并且同一资源在同一时间段内不能被重复占用。

本案例以“会议室预约”为示例展开，也可以平滑迁移到医生挂号、设备预约、场馆预约等场景。核心实现重点放在资源排班、预约创建、时间冲突检测、并发控制、取消预约和签到核销。

### 功能目标

本案例实现一个可直接落地的预约排班核心功能，目标包括：

```text
1. 支持配置可预约资源，例如会议室、医生、设备、场馆。
2. 支持配置资源可用时间段，例如每天 09:00 - 18:00 可预约。
3. 用户提交预约开始时间和结束时间。
4. 系统校验预约时间是否在资源排班范围内。
5. 系统检测同一资源下是否存在时间段冲突。
6. 使用 Redisson 分布式锁控制同一资源的并发预约。
7. 创建预约单并记录预约状态。
8. 用户取消预约后释放资源占用。
9. 用户到场后进行签到核销。
```

本案例重点解决下面几个后端开发中常见但容易出错的问题：

```text
时间段冲突怎么判断？
预约时间是否在可用排班内怎么校验？
高并发下如何避免重复预约同一资源？
预约单状态如何流转？
取消预约和签到核销如何保证状态正确？
```

### 核心业务流程

用户创建预约时，系统需要先校验资源是否存在，再校验预约时间是否合法，然后进入冲突检测和并发控制流程。核心流程如下：

```text
用户选择资源和预约时间
-> 校验资源是否存在且启用
-> 校验预约开始时间小于结束时间
-> 校验预约时间是否在资源可用排班内
-> 按资源 ID 加 Redisson 分布式锁
-> 查询同一资源下已生效预约单
-> 判断是否存在时间段冲突
-> 无冲突则创建预约单
-> 返回预约成功
```

取消预约流程如下：

```text
用户发起取消预约
-> 查询预约单
-> 校验预约单是否属于当前用户
-> 校验预约状态是否允许取消
-> 更新预约状态为已取消
-> 记录取消时间
```

签到核销流程如下：

```text
用户到场签到
-> 查询预约单
-> 校验预约单状态是否为已预约
-> 校验当前时间是否在允许签到范围内
-> 更新预约状态为已签到
-> 记录签到时间
```

其中最关键的是时间段冲突判断。两个时间段只要满足下面条件，就说明存在重叠：

```text
已有预约开始时间 < 新预约结束时间
并且
已有预约结束时间 > 新预约开始时间
```

也就是：

```text
exist_start < request_end AND exist_end > request_start
```

这个判断可以覆盖大部分常见冲突场景，包括：

```text
新预约完全覆盖已有预约
新预约被已有预约完全覆盖
新预约开始时间落在已有预约中
新预约结束时间落在已有预约中
新预约与已有预约边界不重叠
```

边界不重叠的情况，例如：

```text
已有预约：10:00 - 11:00
新预约：11:00 - 12:00
```

这种情况不算冲突，因为已有预约结束时间等于新预约开始时间，可以连续预约。

### 本案例实现边界

为了突出核心实现，本案例只实现预约排班与冲突检测的主链路，不扩展成完整商业系统。

本案例会实现：

```text
资源管理：维护可预约资源。
排班管理：维护资源可用时间段。
预约创建：创建预约单。
冲突检测：判断同一资源同一时间段是否冲突。
并发控制：使用 Redisson 按资源维度加锁。
取消预约：释放预约占用。
签到核销：完成预约到场确认。
```

本案例暂不展开：

```text
复杂周期排班，例如每周一、三、五自动生成排班。
节假日规则。
预约费用和支付。
退款流程。
消息通知。
WebSocket 实时推送。
爽约自动处理定时任务。
多租户隔离。
复杂权限控制。
```

后续如果要增强为生产级系统，可以继续补充：

```text
XXL-JOB 自动处理爽约。
RabbitMQ 异步发送预约通知。
Redis 缓存资源可预约时间。
WebSocket 推送预约状态变化。
Sa-Token 控制用户登录和接口权限。
操作日志记录关键状态变更。
```

本案例默认技术栈为：

```text
Spring Boot 3
MyBatis-Plus
MySQL
Redis
Redisson
Hutool DateUtil
Lombok
```

## 技术栈与项目结构

本案例基于 Spring Boot 3 实现预约排班与时间冲突检测的核心链路，重点覆盖资源配置、资源排班、预约创建、冲突检测、并发控制、取消预约和签到核销。技术栈延续原 README 中推荐的 Spring Boot、MyBatis-Plus、Redis、Redisson、XXL-JOB、MySQL、Hutool DateUtil 等方向。

### 技术栈选型

| 技术                        | 用途                                   |
| --------------------------- | -------------------------------------- |
| Spring Boot 3               | 后端基础框架                           |
| MyBatis-Plus                | 数据库 CRUD、条件构造器、分页查询      |
| MySQL 8                     | 存储资源、排班、预约单                 |
| Redis                       | 缓存和分布式锁基础设施                 |
| Redisson                    | 实现资源维度分布式锁，防止并发重复预约 |
| Hutool DateUtil             | 时间处理、日期格式化、时间比较         |
| Lombok                      | 简化实体类、DTO、VO 代码               |
| Knife4j / springdoc-openapi | 接口文档，可选                         |
| XXL-JOB                     | 后续扩展爽约处理、预约过期处理，可选   |

本案例核心逻辑不依赖复杂中间件，主要使用 MySQL 负责最终数据一致性，Redisson 负责并发预约时的临界区控制。

### Maven 依赖

在 `pom.xml` 中加入以下核心依赖。

```xml
<dependencies>
    <!-- Spring Boot Web：提供 REST 接口能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- MyBatis-Plus：简化 CRUD 和条件查询 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.9</version>
    </dependency>

    <!-- MySQL 驱动：连接 MySQL 8 数据库 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Redisson：基于 Redis 实现分布式锁 -->
    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson-spring-boot-starter</artifactId>
        <version>3.36.0</version>
    </dependency>

    <!-- Hutool：日期、字符串、集合等常用工具类 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.32</version>
    </dependency>

    <!-- Lombok：减少 Getter、Setter、构造器等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 参数校验：用于校验接口入参 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Knife4j：接口文档，可选 -->
    <dependency>
        <groupId>com.github.xiaoymin</groupId>
        <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
        <version>4.5.0</version>
    </dependency>
</dependencies>
```

### 目录结构

本案例采用常规 Spring Boot 分层结构，核心代码集中在 `appointment` 业务模块下。

```text
src/main/java/io/github/atengk/appointment
├── AppointmentApplication.java
├── config
│   └── MybatisPlusConfig.java
├── controller
│   ├── ResourceController.java
│   ├── ScheduleController.java
│   └── AppointmentController.java
├── domain
│   ├── entity
│   │   ├── AppointmentResource.java
│   │   ├── ResourceSchedule.java
│   │   └── AppointmentOrder.java
│   ├── dto
│   │   ├── AppointmentCreateDTO.java
│   │   ├── AppointmentCancelDTO.java
│   │   └── AppointmentCheckInDTO.java
│   ├── vo
│   │   ├── ResourceAvailableTimeVO.java
│   │   └── AppointmentOrderVO.java
│   └── enums
│       ├── ResourceStatusEnum.java
│       └── AppointmentStatusEnum.java
├── mapper
│   ├── AppointmentResourceMapper.java
│   ├── ResourceScheduleMapper.java
│   └── AppointmentOrderMapper.java
├── service
│   ├── AppointmentResourceService.java
│   ├── ResourceScheduleService.java
│   └── AppointmentOrderService.java
└── service/impl
    ├── AppointmentResourceServiceImpl.java
    ├── ResourceScheduleServiceImpl.java
    └── AppointmentOrderServiceImpl.java
```

## 数据库设计

数据库设计围绕三张核心表展开：资源表、资源排班表、预约单表。资源表定义“可以被预约的对象”，排班表定义“什么时候可以预约”，预约单表定义“谁在什么时间占用了什么资源”。

### 资源表设计

资源表用于存储会议室、医生、设备、场馆等可预约对象。

```sql
CREATE TABLE appointment_resource (
    id BIGINT NOT NULL COMMENT '主键 ID',
    resource_name VARCHAR(100) NOT NULL COMMENT '资源名称，例如会议室 A、医生张三、篮球场 1 号场',
    resource_type VARCHAR(50) NOT NULL COMMENT '资源类型，例如 MEETING_ROOM、DOCTOR、VENUE、DEVICE',
    capacity INT NOT NULL DEFAULT 1 COMMENT '资源容量，单人资源一般为 1，场馆或会议室可大于 1',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
    PRIMARY KEY (id),
    KEY idx_resource_type_status (resource_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约资源表';
```

字段说明：

| 字段          | 说明                                                       |
| ------------- | ---------------------------------------------------------- |
| resource_name | 资源名称，用于前端展示                                     |
| resource_type | 资源类型，用于区分会议室、医生、设备等                     |
| capacity      | 容量，本案例默认按单资源互斥预约处理，后续可扩展为名额预约 |
| status        | 禁用后不能继续创建预约                                     |
| deleted       | 配合 MyBatis-Plus 逻辑删除                                 |

### 资源排班表设计

资源排班表用于定义某个资源在哪些时间段允许预约。

```sql
CREATE TABLE resource_schedule (
    id BIGINT NOT NULL COMMENT '主键 ID',
    resource_id BIGINT NOT NULL COMMENT '资源 ID',
    schedule_date DATE NOT NULL COMMENT '排班日期',
    start_time DATETIME NOT NULL COMMENT '可预约开始时间',
    end_time DATETIME NOT NULL COMMENT '可预约结束时间',
    total_quota INT NOT NULL DEFAULT 1 COMMENT '总名额，互斥资源一般为 1',
    used_quota INT NOT NULL DEFAULT 0 COMMENT '已使用名额',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
    PRIMARY KEY (id),
    KEY idx_resource_date_status (resource_id, schedule_date, status),
    KEY idx_resource_time_range (resource_id, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源排班表';
```

排班示例：

```text
会议室 A：2026-05-15 09:00:00 - 2026-05-15 18:00:00
医生张三：2026-05-15 08:30:00 - 2026-05-15 11:30:00
篮球场 1 号场：2026-05-15 10:00:00 - 2026-05-15 22:00:00
```

本案例使用 `DATETIME` 存储完整开始时间和结束时间，而不是只存 `TIME`。这样可以更好地支持跨天排班，例如：

```text
设备 A：2026-05-15 22:00:00 - 2026-05-16 02:00:00
```

### 预约单表设计

预约单表记录用户对某个资源在某个时间段的占用。

```sql
CREATE TABLE appointment_order (
    id BIGINT NOT NULL COMMENT '主键 ID',
    appointment_no VARCHAR(64) NOT NULL COMMENT '预约单号',
    resource_id BIGINT NOT NULL COMMENT '资源 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    start_time DATETIME NOT NULL COMMENT '预约开始时间',
    end_time DATETIME NOT NULL COMMENT '预约结束时间',
    status TINYINT NOT NULL COMMENT '预约状态：1 已预约，2 已取消，3 已签到，4 已爽约',
    cancel_time DATETIME DEFAULT NULL COMMENT '取消时间',
    check_in_time DATETIME DEFAULT NULL COMMENT '签到时间',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_appointment_no (appointment_no),
    KEY idx_resource_time_status (resource_id, start_time, end_time, status),
    KEY idx_user_time_status (user_id, start_time, end_time, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约单表';
```

预约状态建议设计为：

| 状态值 | 状态名称 | 说明                       |
| ------ | -------- | -------------------------- |
| 1      | 已预约   | 预约成功，占用资源         |
| 2      | 已取消   | 用户主动取消，不再占用资源 |
| 3      | 已签到   | 用户已到场核销             |
| 4      | 已爽约   | 超过可签到时间未签到       |

在冲突检测中，只需要将“仍然占用资源”的状态纳入判断，例如：

```text
已预约
已签到
```

已取消和已爽约通常不再占用资源，不参与冲突判断。

### 表索引与唯一约束

本案例的核心查询是“查询某个资源在指定时间段内是否已有有效预约”，因此预约单表必须建立资源和时间范围相关索引。

推荐索引：

```sql
-- 预约单号唯一，防止业务单号重复
UNIQUE KEY uk_appointment_no (appointment_no)

-- 按资源和时间范围查询预约冲突
KEY idx_resource_time_status (resource_id, start_time, end_time, status)

-- 查询用户自己的预约列表
KEY idx_user_time_status (user_id, start_time, end_time, status)

-- 查询资源某天是否存在可用排班
KEY idx_resource_date_status (resource_id, schedule_date, status)

-- 查询资源排班时间范围
KEY idx_resource_time_range (resource_id, start_time, end_time)
```

冲突检测 SQL 会使用如下条件：

```sql
SELECT COUNT(1)
FROM appointment_order
WHERE resource_id = #{resourceId}
  AND status IN (1, 3)
  AND deleted = 0
  AND start_time < #{requestEndTime}
  AND end_time > #{requestStartTime};
```

这条 SQL 的含义是：只要已有预约的开始时间早于新预约结束时间，并且已有预约的结束时间晚于新预约开始时间，就说明两个时间段发生了重叠。

## 核心业务规则

核心业务规则需要同时考虑业务正确性和并发安全。时间校验解决“能不能约”，冲突检测解决“有没有被别人约”，分布式锁解决“多人同时抢同一个资源时谁先写入”。

### 可预约时间校验

用户提交预约时，必须先判断请求时间是否合法。

基础校验规则：

```text
1. resourceId 不能为空。
2. userId 不能为空。
3. startTime 不能为空。
4. endTime 不能为空。
5. startTime 必须早于 endTime。
6. startTime 不能早于当前时间。
7. 预约时间必须落在资源启用的排班时间段内。
8. 资源状态必须为启用。
```

排班校验 SQL：

```sql
SELECT COUNT(1)
FROM resource_schedule
WHERE resource_id = #{resourceId}
  AND status = 1
  AND deleted = 0
  AND start_time <= #{requestStartTime}
  AND end_time >= #{requestEndTime};
```

这条 SQL 表示：用户请求的预约开始时间和结束时间，必须完整落在某一条有效排班范围内。

正确示例：

```text
资源排班：09:00 - 18:00
用户预约：10:00 - 11:00
结果：允许预约
```

错误示例：

```text
资源排班：09:00 - 18:00
用户预约：08:30 - 09:30
结果：不允许预约，因为开始时间早于排班开始时间
```

跨天示例：

```text
资源排班：2026-05-15 22:00:00 - 2026-05-16 02:00:00
用户预约：2026-05-15 23:00:00 - 2026-05-16 01:00:00
结果：允许预约
```

### 时间段冲突判断

时间段冲突判断是本案例最核心的规则。不要只判断开始时间或结束时间是否相等，而要判断两个时间区间是否存在交集。

推荐判断公式：

```text
已有预约开始时间 < 新预约结束时间
并且
已有预约结束时间 > 新预约开始时间
```

对应 SQL：

```sql
SELECT COUNT(1)
FROM appointment_order
WHERE resource_id = #{resourceId}
  AND status IN (1, 3)
  AND deleted = 0
  AND start_time < #{requestEndTime}
  AND end_time > #{requestStartTime};
```

冲突示例：

```text
已有预约：10:00 - 11:00
新预约：10:30 - 11:30
结果：冲突
已有预约：10:00 - 12:00
新预约：10:30 - 11:00
结果：冲突
已有预约：10:00 - 11:00
新预约：09:00 - 12:00
结果：冲突
```

不冲突示例：

```text
已有预约：10:00 - 11:00
新预约：11:00 - 12:00
结果：不冲突
已有预约：10:00 - 11:00
新预约：09:00 - 10:00
结果：不冲突
```

这里采用左闭右开区间思路：

```text
[startTime, endTime)
```

也就是说，预约开始时间包含在占用区间内，预约结束时间不再占用资源。这样可以允许连续预约，避免 10:00 - 11:00 和 11:00 - 12:00 被误判为冲突。

### 并发预约控制

如果两个用户同时预约同一资源、同一时间段，单靠先查询再插入是不安全的。因为两个请求可能同时查询到“无冲突”，然后都插入成功。

错误流程：

```text
用户 A 查询无冲突
用户 B 查询无冲突
用户 A 创建预约单
用户 B 创建预约单
结果：同一资源同一时间段被重复预约
```

本案例使用 Redisson 按资源维度加锁：

```text
lock:appointment:resource:{resourceId}
```

正确流程：

```text
用户 A 获取资源锁
用户 B 等待资源锁

用户 A 校验排班
用户 A 检测冲突
用户 A 创建预约单
用户 A 释放资源锁

用户 B 获取资源锁
用户 B 再次检测冲突
用户 B 发现已被占用
用户 B 创建失败
```

锁粒度建议：

| 锁粒度            | 示例                                          | 说明                   |
| ----------------- | --------------------------------------------- | ---------------------- |
| 资源维度          | `lock:appointment:resource:1001`              | 推荐，简单可靠         |
| 资源 + 日期维度   | `lock:appointment:resource:1001:2026-05-15`   | 并发更高，适合高频预约 |
| 资源 + 时间片维度 | `lock:appointment:resource:1001:202605151000` | 更细，但实现复杂       |

本案例优先使用资源维度锁，保证实现清晰：

```text
lock:appointment:resource:{resourceId}
```

注意事项：

```text
1. 加锁后必须再次查询冲突，不能复用加锁前的查询结果。
2. 创建预约单和冲突检测必须放在同一个事务中。
3. 分布式锁只控制并发入口，数据库仍然是最终一致性的落点。
4. finally 中必须释放锁。
5. 释放锁前要判断当前线程是否持有锁。
```

### 预约状态流转

预约单状态流转要保持简单明确，避免状态随意修改。

推荐状态流转如下：

```text
已预约
  -> 已取消
  -> 已签到
  -> 已爽约
```

更完整的状态规则：

| 当前状态 | 可流转状态 | 触发动作                       |
| -------- | ---------- | ------------------------------ |
| 已预约   | 已取消     | 用户取消预约                   |
| 已预约   | 已签到     | 用户到场签到                   |
| 已预约   | 已爽约     | 定时任务处理超过签到时间的预约 |
| 已取消   | 不允许流转 | 终态                           |
| 已签到   | 不允许流转 | 终态                           |
| 已爽约   | 不允许流转 | 终态                           |

状态流转限制：

```text
1. 已取消的预约不能再次签到。
2. 已签到的预约不能取消。
3. 已爽约的预约不能签到。
4. 只有已预约状态才占用资源。
5. 是否让已签到继续占用资源，取决于业务口径。本案例中已签到仍视为有效占用记录。
```

取消预约规则：

```text
只有已预约状态允许取消。
取消时更新 status = 2。
取消后不再参与时间冲突检测。
```

签到核销规则：

```text
只有已预约状态允许签到。
当前时间应在预约开始前后允许范围内。
签到成功后更新 status = 3。
```

签到时间窗口可以按业务配置，例如：

```text
允许提前 15 分钟签到
允许延后 30 分钟签到
```

示例：

```text
预约时间：10:00 - 11:00
允许签到时间：09:45 - 10:30
当前时间：09:50
结果：允许签到
预约时间：10:00 - 11:00
允许签到时间：09:45 - 10:30
当前时间：10:45
结果：不允许签到，可由定时任务后续标记为爽约
```

## 核心代码实现

本节给出预约排班与时间冲突检测的核心代码，主要覆盖实体类、枚举、Mapper 查询、创建预约、冲突检测、Redisson 分布式锁、取消预约和签到核销。该实现聚焦原场景中的“时间段冲突判断、资源排班、并发预约、取消释放、签到核销”等核心难点。

### 实体类与枚举

先定义资源状态和预约状态枚举。预约冲突检测时，只把仍然占用资源的状态纳入判断，例如“已预约”和“已签到”。

文件位置：`src/main/java/io/github/atengk/appointment/domain/enums/ResourceStatusEnum.java`

```java
package io.github.atengk.appointment.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 预约资源状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum ResourceStatusEnum {

    DISABLED(0, "禁用"),
    ENABLED(1, "启用");

    private final Integer code;
    private final String desc;
}
```

文件位置：`src/main/java/io/github/atengk/appointment/domain/enums/AppointmentStatusEnum.java`

```java
package io.github.atengk.appointment.domain.enums;

import cn.hutool.core.collection.CollUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 预约单状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum AppointmentStatusEnum {

    BOOKED(1, "已预约"),
    CANCELED(2, "已取消"),
    CHECKED_IN(3, "已签到"),
    NO_SHOW(4, "已爽约");

    private final Integer code;
    private final String desc;

    /**
     * 获取仍然占用资源的预约状态
     *
     * @return 状态编码集合
     */
    public static List<Integer> occupiedStatusCodes() {
        return CollUtil.newArrayList(BOOKED.code, CHECKED_IN.code);
    }
}
```

资源实体用于表示可被预约的对象，例如会议室、医生、设备或场馆。

文件位置：`src/main/java/io/github/atengk/appointment/domain/entity/AppointmentResource.java`

```java
package io.github.atengk.appointment.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预约资源实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("appointment_resource")
public class AppointmentResource {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String resourceName;

    private String resourceType;

    private Integer capacity;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
```

资源排班实体用于表示某个资源在哪一天、哪个时间段可以被预约。

文件位置：`src/main/java/io/github/atengk/appointment/domain/entity/ResourceSchedule.java`

```java
package io.github.atengk.appointment.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资源排班实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("resource_schedule")
public class ResourceSchedule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long resourceId;

    private LocalDate scheduleDate;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer totalQuota;

    private Integer usedQuota;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
```

预约单实体用于记录用户对某个资源在某个时间段的占用。

文件位置：`src/main/java/io/github/atengk/appointment/domain/entity/AppointmentOrder.java`

```java
package io.github.atengk.appointment.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预约单实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("appointment_order")
public class AppointmentOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String appointmentNo;

    private Long resourceId;

    private Long userId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;

    private LocalDateTime cancelTime;

    private LocalDateTime checkInTime;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
```

创建预约、取消预约和签到核销使用独立 DTO，避免直接暴露实体类给接口层。

文件位置：`src/main/java/io/github/atengk/appointment/domain/dto/AppointmentCreateDTO.java`

```java
package io.github.atengk.appointment.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建预约请求参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class AppointmentCreateDTO {

    @NotNull(message = "资源ID不能为空")
    private Long resourceId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "预约开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @NotNull(message = "预约结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private String remark;
}
```

文件位置：`src/main/java/io/github/atengk/appointment/domain/dto/AppointmentCancelDTO.java`

```java
package io.github.atengk.appointment.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 取消预约请求参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class AppointmentCancelDTO {

    @NotNull(message = "预约单ID不能为空")
    private Long appointmentId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
```

文件位置：`src/main/java/io/github/atengk/appointment/domain/dto/AppointmentCheckInDTO.java`

```java
package io.github.atengk.appointment.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 预约签到请求参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class AppointmentCheckInDTO {

    @NotNull(message = "预约单ID不能为空")
    private Long appointmentId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
```

### Mapper 与查询方法

Mapper 主要提供三类能力：基础 CRUD、排班可用性校验、预约时间冲突检测。

文件位置：`src/main/java/io/github/atengk/appointment/mapper/AppointmentResourceMapper.java`

```java
package io.github.atengk.appointment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.appointment.domain.entity.AppointmentResource;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预约资源 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface AppointmentResourceMapper extends BaseMapper<AppointmentResource> {
}
```

文件位置：`src/main/java/io/github/atengk/appointment/mapper/ResourceScheduleMapper.java`

```java
package io.github.atengk.appointment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.appointment.domain.entity.ResourceSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 资源排班 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface ResourceScheduleMapper extends BaseMapper<ResourceSchedule> {

    /**
     * 判断预约时间是否完整落在可用排班内
     *
     * @param resourceId 资源ID
     * @param startTime  预约开始时间
     * @param endTime    预约结束时间
     * @return 可用排班数量
     */
    @Select("""
            SELECT COUNT(1)
            FROM resource_schedule
            WHERE resource_id = #{resourceId}
              AND status = 1
              AND deleted = 0
              AND start_time <= #{startTime}
              AND end_time >= #{endTime}
            """)
    Long countAvailableSchedule(@Param("resourceId") Long resourceId,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);
}
```

文件位置：`src/main/java/io/github/atengk/appointment/mapper/AppointmentOrderMapper.java`

```java
package io.github.atengk.appointment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.appointment.domain.entity.AppointmentOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 预约单 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface AppointmentOrderMapper extends BaseMapper<AppointmentOrder> {

    /**
     * 查询指定资源在指定时间段内是否存在冲突预约
     *
     * @param resourceId    资源ID
     * @param startTime     新预约开始时间
     * @param endTime       新预约结束时间
     * @param statusList    占用资源的预约状态
     * @return 冲突预约数量
     */
    @Select("""
            <script>
            SELECT COUNT(1)
            FROM appointment_order
            WHERE resource_id = #{resourceId}
              AND deleted = 0
              AND status IN
              <foreach collection="statusList" item="status" open="(" separator="," close=")">
                  #{status}
              </foreach>
              AND start_time &lt; #{endTime}
              AND end_time &gt; #{startTime}
            </script>
            """)
    Long countConflictAppointment(@Param("resourceId") Long resourceId,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime,
                                  @Param("statusList") List<Integer> statusList);
}
```

这里的冲突判断条件是核心：

```sql
start_time < request_end_time
AND end_time > request_start_time
```

它表示已有预约和新预约存在时间交集。

### 创建预约接口

创建预约接口只负责接收请求和返回结果，核心业务逻辑放在 Service 层。实际项目中，`userId` 通常从登录态获取，这里为了方便接口测试，放在请求参数中。

先定义一个简单通用返回体，便于 Controller 直接返回统一结构。

文件位置：`src/main/java/io/github/atengk/appointment/common/ApiResult.java`

```java
package io.github.atengk.appointment.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口统一返回结果
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    private Integer code;

    private String message;

    private T data;

    /**
     * 返回成功结果
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 统一返回结果
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "操作成功", data);
    }

    /**
     * 返回成功结果
     *
     * @return 统一返回结果
     */
    public static ApiResult<Void> success() {
        return new ApiResult<>(200, "操作成功", null);
    }
}
```

业务异常用于在业务校验失败时中断流程。

文件位置：`src/main/java/io/github/atengk/appointment/common/BizException.java`

```java
package io.github.atengk.appointment.common;

/**
 * 业务异常
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class BizException extends RuntimeException {

    /**
     * 创建业务异常
     *
     * @param message 异常信息
     */
    public BizException(String message) {
        super(message);
    }
}
```

Controller 提供创建预约、取消预约、签到核销三个核心接口。

文件位置：`src/main/java/io/github/atengk/appointment/controller/AppointmentController.java`

```java
package io.github.atengk.appointment.controller;

import io.github.atengk.appointment.common.ApiResult;
import io.github.atengk.appointment.domain.dto.AppointmentCancelDTO;
import io.github.atengk.appointment.domain.dto.AppointmentCheckInDTO;
import io.github.atengk.appointment.domain.dto.AppointmentCreateDTO;
import io.github.atengk.appointment.service.AppointmentOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 预约单接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentOrderService appointmentOrderService;

    /**
     * 创建预约
     *
     * @param dto 创建预约请求
     * @return 预约单ID
     */
    @PostMapping
    public ApiResult<Long> createAppointment(@Valid @RequestBody AppointmentCreateDTO dto) {
        return ApiResult.success(appointmentOrderService.createAppointment(dto));
    }

    /**
     * 取消预约
     *
     * @param dto 取消预约请求
     * @return 操作结果
     */
    @PostMapping("/cancel")
    public ApiResult<Void> cancelAppointment(@Valid @RequestBody AppointmentCancelDTO dto) {
        appointmentOrderService.cancelAppointment(dto);
        return ApiResult.success();
    }

    /**
     * 签到核销
     *
     * @param dto 签到请求
     * @return 操作结果
     */
    @PostMapping("/check-in")
    public ApiResult<Void> checkIn(@Valid @RequestBody AppointmentCheckInDTO dto) {
        appointmentOrderService.checkIn(dto);
        return ApiResult.success();
    }
}
```

Service 接口定义预约主链路方法。

文件位置：`src/main/java/io/github/atengk/appointment/service/AppointmentOrderService.java`

```java
package io.github.atengk.appointment.service;

import io.github.atengk.appointment.domain.dto.AppointmentCancelDTO;
import io.github.atengk.appointment.domain.dto.AppointmentCheckInDTO;
import io.github.atengk.appointment.domain.dto.AppointmentCreateDTO;

/**
 * 预约单服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface AppointmentOrderService {

    /**
     * 创建预约
     *
     * @param dto 创建预约请求
     * @return 预约单ID
     */
    Long createAppointment(AppointmentCreateDTO dto);

    /**
     * 取消预约
     *
     * @param dto 取消预约请求
     */
    void cancelAppointment(AppointmentCancelDTO dto);

    /**
     * 签到核销
     *
     * @param dto 签到请求
     */
    void checkIn(AppointmentCheckInDTO dto);
}
```

### 时间冲突检测逻辑

时间冲突检测不要写成“开始时间相等”或“结束时间相等”，而是判断两个时间区间是否有交集。该逻辑在 Service 中封装成独立方法，创建预约时必须调用。

下面的 Service 实现包含预约创建、排班校验、冲突检测、分布式锁、取消预约和签到核销。

文件位置：`src/main/java/io/github/atengk/appointment/service/impl/AppointmentOrderServiceImpl.java`

```java
package io.github.atengk.appointment.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.github.atengk.appointment.common.BizException;
import io.github.atengk.appointment.domain.dto.AppointmentCancelDTO;
import io.github.atengk.appointment.domain.dto.AppointmentCheckInDTO;
import io.github.atengk.appointment.domain.dto.AppointmentCreateDTO;
import io.github.atengk.appointment.domain.entity.AppointmentOrder;
import io.github.atengk.appointment.domain.entity.AppointmentResource;
import io.github.atengk.appointment.domain.enums.AppointmentStatusEnum;
import io.github.atengk.appointment.domain.enums.ResourceStatusEnum;
import io.github.atengk.appointment.mapper.AppointmentOrderMapper;
import io.github.atengk.appointment.mapper.AppointmentResourceMapper;
import io.github.atengk.appointment.mapper.ResourceScheduleMapper;
import io.github.atengk.appointment.service.AppointmentOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 预约单服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentOrderServiceImpl implements AppointmentOrderService {

    private static final int CHECK_IN_BEFORE_MINUTES = 15;

    private static final int CHECK_IN_AFTER_MINUTES = 30;

    private final AppointmentResourceMapper appointmentResourceMapper;

    private final ResourceScheduleMapper resourceScheduleMapper;

    private final AppointmentOrderMapper appointmentOrderMapper;

    private final RedissonClient redissonClient;

    /**
     * 创建预约
     *
     * @param dto 创建预约请求
     * @return 预约单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAppointment(AppointmentCreateDTO dto) {
        validateCreateRequest(dto);
        AppointmentResource resource = getEnabledResource(dto.getResourceId());

        String lockKey = StrUtil.format("lock:appointment:resource:{}", dto.getResourceId());
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;

        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("资源预约加锁失败，资源ID：{}，用户ID：{}", dto.getResourceId(), dto.getUserId());
                throw new BizException("当前资源预约人数较多，请稍后重试");
            }

            checkAvailableSchedule(dto);
            checkTimeConflict(dto);

            AppointmentOrder order = new AppointmentOrder();
            order.setAppointmentNo(generateAppointmentNo());
            order.setResourceId(resource.getId());
            order.setUserId(dto.getUserId());
            order.setStartTime(dto.getStartTime());
            order.setEndTime(dto.getEndTime());
            order.setStatus(AppointmentStatusEnum.BOOKED.getCode());
            order.setRemark(dto.getRemark());

            appointmentOrderMapper.insert(order);

            log.info("预约创建成功，预约单号：{}，资源ID：{}，用户ID：{}，预约时间：{} - {}",
                    order.getAppointmentNo(),
                    order.getResourceId(),
                    order.getUserId(),
                    formatTime(order.getStartTime()),
                    formatTime(order.getEndTime()));

            return order.getId();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("预约创建被中断，资源ID：{}，用户ID：{}", dto.getResourceId(), dto.getUserId(), e);
            throw new BizException("预约请求被中断，请重试");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 取消预约
     *
     * @param dto 取消预约请求
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelAppointment(AppointmentCancelDTO dto) {
        AppointmentOrder order = getUserAppointment(dto.getAppointmentId(), dto.getUserId());

        if (!AppointmentStatusEnum.BOOKED.getCode().equals(order.getStatus())) {
            throw new BizException("当前预约状态不允许取消");
        }

        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<AppointmentOrder> updateWrapper = new LambdaUpdateWrapper<AppointmentOrder>()
                .eq(AppointmentOrder::getId, dto.getAppointmentId())
                .eq(AppointmentOrder::getUserId, dto.getUserId())
                .eq(AppointmentOrder::getStatus, AppointmentStatusEnum.BOOKED.getCode())
                .set(AppointmentOrder::getStatus, AppointmentStatusEnum.CANCELED.getCode())
                .set(AppointmentOrder::getCancelTime, now);

        int updated = appointmentOrderMapper.update(null, updateWrapper);
        if (updated <= 0) {
            throw new BizException("取消预约失败，请刷新后重试");
        }

        log.info("预约取消成功，预约单ID：{}，用户ID：{}，取消时间：{}",
                dto.getAppointmentId(), dto.getUserId(), formatTime(now));
    }

    /**
     * 签到核销
     *
     * @param dto 签到请求
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkIn(AppointmentCheckInDTO dto) {
        AppointmentOrder order = getUserAppointment(dto.getAppointmentId(), dto.getUserId());

        if (!AppointmentStatusEnum.BOOKED.getCode().equals(order.getStatus())) {
            throw new BizException("当前预约状态不允许签到");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime allowStartTime = order.getStartTime().minusMinutes(CHECK_IN_BEFORE_MINUTES);
        LocalDateTime allowEndTime = order.getStartTime().plusMinutes(CHECK_IN_AFTER_MINUTES);

        if (now.isBefore(allowStartTime) || now.isAfter(allowEndTime)) {
            log.warn("预约签到时间不合法，预约单ID：{}，当前时间：{}，允许签到时间：{} - {}",
                    order.getId(), formatTime(now), formatTime(allowStartTime), formatTime(allowEndTime));
            throw new BizException("当前不在允许签到时间范围内");
        }

        LambdaUpdateWrapper<AppointmentOrder> updateWrapper = new LambdaUpdateWrapper<AppointmentOrder>()
                .eq(AppointmentOrder::getId, dto.getAppointmentId())
                .eq(AppointmentOrder::getUserId, dto.getUserId())
                .eq(AppointmentOrder::getStatus, AppointmentStatusEnum.BOOKED.getCode())
                .set(AppointmentOrder::getStatus, AppointmentStatusEnum.CHECKED_IN.getCode())
                .set(AppointmentOrder::getCheckInTime, now);

        int updated = appointmentOrderMapper.update(null, updateWrapper);
        if (updated <= 0) {
            throw new BizException("签到失败，请刷新后重试");
        }

        log.info("预约签到成功，预约单ID：{}，用户ID：{}，签到时间：{}",
                dto.getAppointmentId(), dto.getUserId(), formatTime(now));
    }

    /**
     * 校验创建预约请求
     *
     * @param dto 创建预约请求
     */
    private void validateCreateRequest(AppointmentCreateDTO dto) {
        if (ObjectUtil.hasNull(dto.getResourceId(), dto.getUserId(), dto.getStartTime(), dto.getEndTime())) {
            throw new BizException("预约参数不能为空");
        }
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BizException("预约结束时间必须晚于预约开始时间");
        }
        if (dto.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BizException("不能预约过去的时间");
        }
    }

    /**
     * 获取启用状态的预约资源
     *
     * @param resourceId 资源ID
     * @return 预约资源
     */
    private AppointmentResource getEnabledResource(Long resourceId) {
        AppointmentResource resource = appointmentResourceMapper.selectById(resourceId);
        if (resource == null) {
            throw new BizException("预约资源不存在");
        }
        if (!ResourceStatusEnum.ENABLED.getCode().equals(resource.getStatus())) {
            throw new BizException("预约资源已禁用");
        }
        return resource;
    }

    /**
     * 校验预约时间是否在资源排班范围内
     *
     * @param dto 创建预约请求
     */
    private void checkAvailableSchedule(AppointmentCreateDTO dto) {
        Long count = resourceScheduleMapper.countAvailableSchedule(
                dto.getResourceId(),
                dto.getStartTime(),
                dto.getEndTime()
        );

        if (count == null || count <= 0) {
            log.warn("预约时间不在资源排班范围内，资源ID：{}，预约时间：{} - {}",
                    dto.getResourceId(), formatTime(dto.getStartTime()), formatTime(dto.getEndTime()));
            throw new BizException("预约时间不在资源可预约范围内");
        }
    }

    /**
     * 校验预约时间是否冲突
     *
     * @param dto 创建预约请求
     */
    private void checkTimeConflict(AppointmentCreateDTO dto) {
        Long conflictCount = appointmentOrderMapper.countConflictAppointment(
                dto.getResourceId(),
                dto.getStartTime(),
                dto.getEndTime(),
                AppointmentStatusEnum.occupiedStatusCodes()
        );

        if (conflictCount != null && conflictCount > 0) {
            log.warn("预约时间冲突，资源ID：{}，用户ID：{}，预约时间：{} - {}",
                    dto.getResourceId(),
                    dto.getUserId(),
                    formatTime(dto.getStartTime()),
                    formatTime(dto.getEndTime()));
            throw new BizException("当前时间段已被预约，请选择其他时间");
        }
    }

    /**
     * 查询用户预约单
     *
     * @param appointmentId 预约单ID
     * @param userId        用户ID
     * @return 预约单
     */
    private AppointmentOrder getUserAppointment(Long appointmentId, Long userId) {
        Assert.notNull(appointmentId, () -> new BizException("预约单ID不能为空"));
        Assert.notNull(userId, () -> new BizException("用户ID不能为空"));

        AppointmentOrder order = appointmentOrderMapper.selectById(appointmentId);
        if (order == null) {
            throw new BizException("预约单不存在");
        }
        if (!userId.equals(order.getUserId())) {
            throw new BizException("不能操作他人的预约单");
        }
        return order;
    }

    /**
     * 生成预约单号
     *
     * @return 预约单号
     */
    private String generateAppointmentNo() {
        return StrUtil.format("AP{}{}",
                DateUtil.format(DateUtil.date(), DatePattern.PURE_DATETIME_PATTERN),
                IdUtil.getSnowflakeNextId());
    }

    /**
     * 格式化时间
     *
     * @param time 时间
     * @return 格式化后的时间
     */
    private String formatTime(LocalDateTime time) {
        return DateUtil.format(LocalDateTimeUtil.toDate(time), DatePattern.NORM_DATETIME_PATTERN);
    }
}
```

### Redisson 分布式锁防并发

创建预约时必须避免“先查无冲突、再插入”的并发穿透问题。这里使用资源维度锁：

```text
lock:appointment:resource:{resourceId}
```

核心逻辑已经在 `createAppointment` 方法中体现：

```java
String lockKey = StrUtil.format("lock:appointment:resource:{}", dto.getResourceId());
RLock lock = redissonClient.getLock(lockKey);

locked = lock.tryLock(3, 10, TimeUnit.SECONDS);

checkAvailableSchedule(dto);
checkTimeConflict(dto);
appointmentOrderMapper.insert(order);
```

关键点是：排班校验、冲突检测、预约单插入必须放在锁内部执行。不能在加锁前查一次冲突，然后加锁后直接插入。

原因如下：

```text
用户 A 和用户 B 同时预约同一资源 10:00 - 11:00。

如果加锁前检测冲突：
A 查询无冲突
B 查询无冲突
A 插入成功
B 插入成功
结果重复预约。

如果加锁后检测冲突：
A 获取锁，查询无冲突，插入成功，释放锁
B 获取锁，再次查询，发现冲突，创建失败
结果正确。
```

### 取消预约释放资源

本案例中不单独维护库存扣减，而是通过预约单状态控制资源是否被占用。取消预约时把状态从“已预约”改为“已取消”，后续冲突检测只查询“已预约、已签到”的记录，因此已取消记录不会再占用资源。

核心更新条件：

```java
.eq(AppointmentOrder::getStatus, AppointmentStatusEnum.BOOKED.getCode())
.set(AppointmentOrder::getStatus, AppointmentStatusEnum.CANCELED.getCode())
.set(AppointmentOrder::getCancelTime, now);
```

这里必须带上原状态条件，避免重复取消或并发取消导致状态错乱。

取消预约接口示例：

```bash
curl -X POST 'http://localhost:8080/api/appointments/cancel' \
  -H 'Content-Type: application/json' \
  -d '{
    "appointmentId": 1900000000000000001,
    "userId": 10001
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

### 签到核销实现

签到核销用于确认用户到场。只有“已预约”状态允许签到，已取消、已签到、已爽约都不能再次签到。

本案例设置签到窗口：

```text
预约开始前 15 分钟可以签到
预约开始后 30 分钟内可以签到
```

例如预约时间为 `10:00 - 11:00`，允许签到时间为：

```text
09:45 - 10:30
```

签到核心判断：

```java
LocalDateTime allowStartTime = order.getStartTime().minusMinutes(15);
LocalDateTime allowEndTime = order.getStartTime().plusMinutes(30);

if (now.isBefore(allowStartTime) || now.isAfter(allowEndTime)) {
    throw new BizException("当前不在允许签到时间范围内");
}
```

签到成功后状态变更：

```java
.set(AppointmentOrder::getStatus, AppointmentStatusEnum.CHECKED_IN.getCode())
.set(AppointmentOrder::getCheckInTime, now);
```

签到接口示例：

```bash
curl -X POST 'http://localhost:8080/api/appointments/check-in' \
  -H 'Content-Type: application/json' \
  -d '{
    "appointmentId": 1900000000000000001,
    "userId": 10001
  }'
```

创建预约接口示例：

```bash
curl -X POST 'http://localhost:8080/api/appointments' \
  -H 'Content-Type: application/json' \
  -d '{
    "resourceId": 1001,
    "userId": 10001,
    "startTime": "2026-05-15 10:00:00",
    "endTime": "2026-05-15 11:00:00",
    "remark": "项目评审会议"
  }'
```

预约成功响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 1900000000000000001
}
```

冲突预约请求示例：

```bash
curl -X POST 'http://localhost:8080/api/appointments' \
  -H 'Content-Type: application/json' \
  -d '{
    "resourceId": 1001,
    "userId": 10002,
    "startTime": "2026-05-15 10:30:00",
    "endTime": "2026-05-15 11:30:00",
    "remark": "冲突预约测试"
  }'
```

预期结果：

```json
{
  "code": 500,
  "message": "当前时间段已被预约，请选择其他时间",
  "data": null
}
```

这里的 `500` 只是示例。实际项目建议配合全局异常处理，把 `BizException` 转成业务错误码，例如 `400` 或自定义业务码。

## 接口设计

本节给出预约排班核心接口设计，覆盖资源可预约时间查询、创建预约、取消预约和签到核销。接口围绕 README 原始场景中的“用户选择预约时间、检测时间段冲突、创建预约单、取消预约释放资源、签到核销”主流程展开。

### 查询资源可预约时间

该接口用于查询某个资源在指定日期内的可预约排班时间。前端通常会在用户选择资源后调用该接口，然后展示可预约时间段。

接口信息：

```text
请求方式：GET
接口路径：/api/resources/{resourceId}/available-times
接口说明：查询资源某天可预约时间段
```

请求参数：

| 参数       | 类型   | 是否必填 | 示例       | 说明     |
| ---------- | ------ | -------- | ---------- | -------- |
| resourceId | Long   | 是       | 1001       | 资源 ID  |
| date       | String | 是       | 2026-05-15 | 查询日期 |

请求示例：

```bash
curl -X GET 'http://localhost:8080/api/resources/1001/available-times?date=2026-05-15'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "scheduleId": 1,
      "resourceId": 1001,
      "startTime": "2026-05-15 09:00:00",
      "endTime": "2026-05-15 18:00:00",
      "totalQuota": 1,
      "usedQuota": 0
    }
  ]
}
```

可预约时间 VO 可以这样定义。

文件位置：`src/main/java/io/github/atengk/appointment/domain/vo/ResourceAvailableTimeVO.java`

```java
package io.github.atengk.appointment.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资源可预约时间响应
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class ResourceAvailableTimeVO {

    private Long scheduleId;

    private Long resourceId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private Integer totalQuota;

    private Integer usedQuota;
}
```

如果只需要查询排班，不计算被占用后的剩余时间段，可以直接按日期查询启用排班。

文件位置：`src/main/java/io/github/atengk/appointment/controller/ResourceController.java`

```java
package io.github.atengk.appointment.controller;

import io.github.atengk.appointment.common.ApiResult;
import io.github.atengk.appointment.domain.vo.ResourceAvailableTimeVO;
import io.github.atengk.appointment.service.ResourceScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 预约资源接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceScheduleService resourceScheduleService;

    /**
     * 查询资源可预约时间
     *
     * @param resourceId 资源ID
     * @param date       查询日期
     * @return 可预约时间段
     */
    @GetMapping("/{resourceId}/available-times")
    public ApiResult<List<ResourceAvailableTimeVO>> listAvailableTimes(@PathVariable Long resourceId,
                                                                       @RequestParam LocalDate date) {
        return ApiResult.success(resourceScheduleService.listAvailableTimes(resourceId, date));
    }
}
```

文件位置：`src/main/java/io/github/atengk/appointment/service/ResourceScheduleService.java`

```java
package io.github.atengk.appointment.service;

import io.github.atengk.appointment.domain.vo.ResourceAvailableTimeVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 资源排班服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface ResourceScheduleService {

    /**
     * 查询资源可预约时间
     *
     * @param resourceId 资源ID
     * @param date       查询日期
     * @return 可预约时间段
     */
    List<ResourceAvailableTimeVO> listAvailableTimes(Long resourceId, LocalDate date);
}
```

文件位置：`src/main/java/io/github/atengk/appointment/service/impl/ResourceScheduleServiceImpl.java`

```java
package io.github.atengk.appointment.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.atengk.appointment.domain.entity.ResourceSchedule;
import io.github.atengk.appointment.domain.enums.ResourceStatusEnum;
import io.github.atengk.appointment.domain.vo.ResourceAvailableTimeVO;
import io.github.atengk.appointment.mapper.ResourceScheduleMapper;
import io.github.atengk.appointment.service.ResourceScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 资源排班服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceScheduleServiceImpl implements ResourceScheduleService {

    private final ResourceScheduleMapper resourceScheduleMapper;

    /**
     * 查询资源可预约时间
     *
     * @param resourceId 资源ID
     * @param date       查询日期
     * @return 可预约时间段
     */
    @Override
    public List<ResourceAvailableTimeVO> listAvailableTimes(Long resourceId, LocalDate date) {
        LambdaQueryWrapper<ResourceSchedule> queryWrapper = new LambdaQueryWrapper<ResourceSchedule>()
                .eq(ResourceSchedule::getResourceId, resourceId)
                .eq(ResourceSchedule::getScheduleDate, date)
                .eq(ResourceSchedule::getStatus, ResourceStatusEnum.ENABLED.getCode())
                .orderByAsc(ResourceSchedule::getStartTime);

        List<ResourceSchedule> scheduleList = resourceScheduleMapper.selectList(queryWrapper);
        log.info("查询资源可预约时间，资源ID：{}，日期：{}，排班数量：{}", resourceId, date, scheduleList.size());

        return BeanUtil.copyToList(scheduleList, ResourceAvailableTimeVO.class);
    }
}
```

### 创建预约

该接口用于创建预约单。后端会完成资源状态校验、排班时间校验、时间冲突检测和 Redisson 分布式锁并发控制。

接口信息：

```text
请求方式：POST
接口路径：/api/appointments
接口说明：创建预约单
```

请求参数：

| 参数       | 类型   | 是否必填 | 示例                | 说明                              |
| ---------- | ------ | -------- | ------------------- | --------------------------------- |
| resourceId | Long   | 是       | 1001                | 资源 ID                           |
| userId     | Long   | 是       | 10001               | 用户 ID，实际项目建议从登录态获取 |
| startTime  | String | 是       | 2026-05-15 10:00:00 | 预约开始时间                      |
| endTime    | String | 是       | 2026-05-15 11:00:00 | 预约结束时间                      |
| remark     | String | 否       | 项目评审会议        | 备注                              |

请求示例：

```bash
curl -X POST 'http://localhost:8080/api/appointments' \
  -H 'Content-Type: application/json' \
  -d '{
    "resourceId": 1001,
    "userId": 10001,
    "startTime": "2026-05-15 10:00:00",
    "endTime": "2026-05-15 11:00:00",
    "remark": "项目评审会议"
  }'
```

成功响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 1900000000000000001
}
```

时间冲突响应：

```json
{
  "code": 400,
  "message": "当前时间段已被预约，请选择其他时间",
  "data": null
}
```

排班不可用响应：

```json
{
  "code": 400,
  "message": "预约时间不在资源可预约范围内",
  "data": null
}
```

建议创建预约时只允许使用左闭右开的时间区间：

```text
[startTime, endTime)
```

例如 `10:00 - 11:00` 和 `11:00 - 12:00` 不算冲突，可以连续预约。

### 取消预约

该接口用于取消预约。取消后预约单状态变为“已取消”，后续冲突检测不再把该预约单视为资源占用。

接口信息：

```text
请求方式：POST
接口路径：/api/appointments/cancel
接口说明：取消预约单
```

请求参数：

| 参数          | 类型 | 是否必填 | 示例                | 说明                              |
| ------------- | ---- | -------- | ------------------- | --------------------------------- |
| appointmentId | Long | 是       | 1900000000000000001 | 预约单 ID                         |
| userId        | Long | 是       | 10001               | 用户 ID，实际项目建议从登录态获取 |

请求示例：

```bash
curl -X POST 'http://localhost:8080/api/appointments/cancel' \
  -H 'Content-Type: application/json' \
  -d '{
    "appointmentId": 1900000000000000001,
    "userId": 10001
  }'
```

成功响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

不允许取消响应：

```json
{
  "code": 400,
  "message": "当前预约状态不允许取消",
  "data": null
}
```

取消预约时需要注意两个条件：

```text
1. 只能取消自己的预约单。
2. 只有“已预约”状态允许取消。
```

核心状态更新条件应该同时带上 `id`、`user_id`、`status`：

```sql
UPDATE appointment_order
SET status = 2,
    cancel_time = NOW()
WHERE id = ?
  AND user_id = ?
  AND status = 1
  AND deleted = 0;
```

这样可以防止重复取消、并发取消、取消他人预约等问题。

### 签到核销

该接口用于用户到场签到。签到成功后预约单状态变为“已签到”。

接口信息：

```text
请求方式：POST
接口路径：/api/appointments/check-in
接口说明：预约签到核销
```

请求参数：

| 参数          | 类型 | 是否必填 | 示例                | 说明                              |
| ------------- | ---- | -------- | ------------------- | --------------------------------- |
| appointmentId | Long | 是       | 1900000000000000001 | 预约单 ID                         |
| userId        | Long | 是       | 10001               | 用户 ID，实际项目建议从登录态获取 |

请求示例：

```bash
curl -X POST 'http://localhost:8080/api/appointments/check-in' \
  -H 'Content-Type: application/json' \
  -d '{
    "appointmentId": 1900000000000000001,
    "userId": 10001
  }'
```

成功响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

不在签到时间范围内响应：

```json
{
  "code": 400,
  "message": "当前不在允许签到时间范围内",
  "data": null
}
```

签到窗口示例：

```text
预约时间：2026-05-15 10:00:00 - 2026-05-15 11:00:00
允许提前签到：15 分钟
允许延后签到：30 分钟
允许签到区间：2026-05-15 09:45:00 - 2026-05-15 10:30:00
```

核心状态更新条件：

```sql
UPDATE appointment_order
SET status = 3,
    check_in_time = NOW()
WHERE id = ?
  AND user_id = ?
  AND status = 1
  AND deleted = 0;
```

## 功能验证

本节通过测试数据、接口调用和预期结果验证核心功能。验证顺序建议为：初始化资源与排班、正常预约、冲突预约、并发预约、取消预约、再次预约、签到核销。

### 正常预约验证

先准备一条资源数据和一条排班数据。

```sql
-- 清理测试数据
DELETE FROM appointment_order WHERE resource_id = 1001;
DELETE FROM resource_schedule WHERE resource_id = 1001;
DELETE FROM appointment_resource WHERE id = 1001;

-- 新增会议室资源
INSERT INTO appointment_resource (
    id,
    resource_name,
    resource_type,
    capacity,
    status,
    remark,
    deleted
) VALUES (
    1001,
    '会议室 A',
    'MEETING_ROOM',
    1,
    1,
    '预约排班测试资源',
    0
);

-- 新增会议室排班
INSERT INTO resource_schedule (
    id,
    resource_id,
    schedule_date,
    start_time,
    end_time,
    total_quota,
    used_quota,
    status,
    remark,
    deleted
) VALUES (
    2001,
    1001,
    '2026-05-15',
    '2026-05-15 09:00:00',
    '2026-05-15 18:00:00',
    1,
    0,
    1,
    '会议室 A 当日可预约时间',
    0
);
```

查询资源可预约时间：

```bash
curl -X GET 'http://localhost:8080/api/resources/1001/available-times?date=2026-05-15'
```

预期响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "scheduleId": 2001,
      "resourceId": 1001,
      "startTime": "2026-05-15 09:00:00",
      "endTime": "2026-05-15 18:00:00",
      "totalQuota": 1,
      "usedQuota": 0
    }
  ]
}
```

创建正常预约：

```bash
curl -X POST 'http://localhost:8080/api/appointments' \
  -H 'Content-Type: application/json' \
  -d '{
    "resourceId": 1001,
    "userId": 10001,
    "startTime": "2026-05-15 10:00:00",
    "endTime": "2026-05-15 11:00:00",
    "remark": "正常预约验证"
  }'
```

预期响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 1900000000000000001
}
```

数据库验证：

```sql
SELECT id,
       appointment_no,
       resource_id,
       user_id,
       start_time,
       end_time,
       status
FROM appointment_order
WHERE resource_id = 1001
ORDER BY create_time DESC;
```

预期结果：

```text
存在一条 status = 1 的预约单。
预约时间为 2026-05-15 10:00:00 - 2026-05-15 11:00:00。
```

### 冲突预约验证

已有预约为：

```text
2026-05-15 10:00:00 - 2026-05-15 11:00:00
```

发起一个重叠预约：

```bash
curl -X POST 'http://localhost:8080/api/appointments' \
  -H 'Content-Type: application/json' \
  -d '{
    "resourceId": 1001,
    "userId": 10002,
    "startTime": "2026-05-15 10:30:00",
    "endTime": "2026-05-15 11:30:00",
    "remark": "冲突预约验证"
  }'
```

预期响应：

```json
{
  "code": 400,
  "message": "当前时间段已被预约，请选择其他时间",
  "data": null
}
```

再发起一个边界不重叠预约：

```bash
curl -X POST 'http://localhost:8080/api/appointments' \
  -H 'Content-Type: application/json' \
  -d '{
    "resourceId": 1001,
    "userId": 10003,
    "startTime": "2026-05-15 11:00:00",
    "endTime": "2026-05-15 12:00:00",
    "remark": "边界不冲突预约验证"
  }'
```

预期响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 1900000000000000002
}
```

验证重点：

```text
10:30 - 11:30 与 10:00 - 11:00 有交集，应该失败。
11:00 - 12:00 与 10:00 - 11:00 边界相接，应该成功。
```

### 并发预约验证

并发预约主要验证 Redisson 分布式锁是否生效。这里使用 `xargs` 并发发起 10 次相同资源、相同时间段的预约请求。

先清理目标时间段数据：

```sql
DELETE FROM appointment_order
WHERE resource_id = 1001
  AND start_time = '2026-05-15 14:00:00'
  AND end_time = '2026-05-15 15:00:00';
```

使用 Shell 并发请求：

```bash
seq 1 10 | xargs -I {} -P 10 curl -s -X POST 'http://localhost:8080/api/appointments' \
  -H 'Content-Type: application/json' \
  -d '{
    "resourceId": 1001,
    "userId": {},
    "startTime": "2026-05-15 14:00:00",
    "endTime": "2026-05-15 15:00:00",
    "remark": "并发预约验证"
  }'
```

这条命令会并发发起 10 个预约请求，`-P 10` 表示最多 10 个并行进程。因为锁粒度是资源维度，同一时间只有一个请求能进入“排班校验、冲突检测、插入预约单”临界区。

数据库验证：

```sql
SELECT COUNT(1) AS success_count
FROM appointment_order
WHERE resource_id = 1001
  AND start_time = '2026-05-15 14:00:00'
  AND end_time = '2026-05-15 15:00:00'
  AND status IN (1, 3)
  AND deleted = 0;
```

预期结果：

```text
success_count = 1
```

如果结果大于 1，说明并发控制存在问题，需要重点检查：

```text
1. 是否在加锁后再次执行冲突检测。
2. 冲突检测和插入预约单是否在同一个事务内。
3. Redisson 是否连接到同一个 Redis。
4. lockKey 是否按 resourceId 生成。
5. finally 中是否正确释放锁。
```

### 取消与签到验证

先取消一个已预约的预约单。假设预约单 ID 为 `1900000000000000001`。

```bash
curl -X POST 'http://localhost:8080/api/appointments/cancel' \
  -H 'Content-Type: application/json' \
  -d '{
    "appointmentId": 1900000000000000001,
    "userId": 10001
  }'
```

数据库验证：

```sql
SELECT id,
       user_id,
       status,
       cancel_time
FROM appointment_order
WHERE id = 1900000000000000001;
```

预期结果：

```text
status = 2
cancel_time 不为空
```

取消后，再次预约同一资源、同一时间段：

```bash
curl -X POST 'http://localhost:8080/api/appointments' \
  -H 'Content-Type: application/json' \
  -d '{
    "resourceId": 1001,
    "userId": 10004,
    "startTime": "2026-05-15 10:00:00",
    "endTime": "2026-05-15 11:00:00",
    "remark": "取消后重新预约"
  }'
```

预期结果：

```text
如果原预约已经取消，则新预约应该成功。
```

签到验证需要保证当前时间落在允许签到窗口内。为了方便本地测试，可以临时插入一个接近当前时间的预约单，或者把系统中的签到窗口参数调大。

查询待签到预约：

```sql
SELECT id,
       user_id,
       start_time,
       end_time,
       status
FROM appointment_order
WHERE resource_id = 1001
  AND status = 1
ORDER BY create_time DESC
LIMIT 1;
```

发起签到：

```bash
curl -X POST 'http://localhost:8080/api/appointments/check-in' \
  -H 'Content-Type: application/json' \
  -d '{
    "appointmentId": 1900000000000000002,
    "userId": 10004
  }'
```

数据库验证：

```sql
SELECT id,
       user_id,
       status,
       check_in_time
FROM appointment_order
WHERE id = 1900000000000000002;
```

预期结果：

```text
status = 3
check_in_time 不为空
```

再次取消已签到预约：

```bash
curl -X POST 'http://localhost:8080/api/appointments/cancel' \
  -H 'Content-Type: application/json' \
  -d '{
    "appointmentId": 1900000000000000002,
    "userId": 10004
  }'
```

预期响应：

```json
{
  "code": 400,
  "message": "当前预约状态不允许取消",
  "data": null
}
```

## 可扩展优化

本案例实现的是预约排班核心链路。生产环境中可以继续补充跨天预约、爽约自动处理、WebSocket 状态推送、通知消息、缓存优化、名额预约等能力。

### 跨天预约处理

本案例数据库中 `start_time` 和 `end_time` 使用 `DATETIME`，天然支持跨天预约。例如：

```text
资源排班：2026-05-15 22:00:00 - 2026-05-16 02:00:00
用户预约：2026-05-15 23:00:00 - 2026-05-16 01:00:00
```

校验逻辑仍然成立：

```sql
SELECT COUNT(1)
FROM resource_schedule
WHERE resource_id = ?
  AND status = 1
  AND deleted = 0
  AND start_time <= ?
  AND end_time >= ?;
```

冲突检测逻辑也不需要特殊调整：

```sql
SELECT COUNT(1)
FROM appointment_order
WHERE resource_id = ?
  AND status IN (1, 3)
  AND deleted = 0
  AND start_time < ?
  AND end_time > ?;
```

推荐跨天处理规则：

```text
1. 不使用单独的 TIME 字段表达预约时间，避免丢失日期语义。
2. 排班表使用完整 DATETIME 存储 start_time 和 end_time。
3. schedule_date 可以保留为排班归属日期，用于按天查询。
4. 跨天排班的 schedule_date 建议取 start_time 所在日期。
5. 前端展示时根据 start_time 和 end_time 判断是否跨天。
```

跨天测试数据：

```sql
INSERT INTO resource_schedule (
    id,
    resource_id,
    schedule_date,
    start_time,
    end_time,
    total_quota,
    used_quota,
    status,
    remark,
    deleted
) VALUES (
    2002,
    1001,
    '2026-05-15',
    '2026-05-15 22:00:00',
    '2026-05-16 02:00:00',
    1,
    0,
    1,
    '跨天排班测试',
    0
);
```

跨天预约请求：

```bash
curl -X POST 'http://localhost:8080/api/appointments' \
  -H 'Content-Type: application/json' \
  -d '{
    "resourceId": 1001,
    "userId": 10005,
    "startTime": "2026-05-15 23:00:00",
    "endTime": "2026-05-16 01:00:00",
    "remark": "跨天预约测试"
  }'
```

预期结果：

```text
只要预约时间完整落在跨天排班内，并且没有冲突预约，就应该创建成功。
```

### 爽约自动处理

爽约处理适合使用 XXL-JOB 或 Spring 定时任务扫描。规则通常是：预约开始后超过允许签到时间仍未签到，则标记为“已爽约”。

示例规则：

```text
预约开始时间：10:00
允许延后签到：30 分钟
超过 10:30 仍未签到，则标记为已爽约
```

如果项目暂时没有接入 XXL-JOB，可以先用 Spring 定时任务实现。生产环境建议替换为 XXL-JOB，便于任务调度、失败重试和运行日志查看。

文件位置：`src/main/java/io/github/atengk/appointment/job/AppointmentNoShowJob.java`

```java
package io.github.atengk.appointment.job;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.github.atengk.appointment.domain.entity.AppointmentOrder;
import io.github.atengk.appointment.domain.enums.AppointmentStatusEnum;
import io.github.atengk.appointment.mapper.AppointmentOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 预约爽约处理任务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentNoShowJob {

    private static final int CHECK_IN_AFTER_MINUTES = 30;

    private final AppointmentOrderMapper appointmentOrderMapper;

    /**
     * 定时处理爽约预约单
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void markNoShowAppointments() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(CHECK_IN_AFTER_MINUTES);

        LambdaUpdateWrapper<AppointmentOrder> updateWrapper = new LambdaUpdateWrapper<AppointmentOrder>()
                .eq(AppointmentOrder::getStatus, AppointmentStatusEnum.BOOKED.getCode())
                .lt(AppointmentOrder::getStartTime, deadline)
                .set(AppointmentOrder::getStatus, AppointmentStatusEnum.NO_SHOW.getCode());

        int updated = appointmentOrderMapper.update(null, updateWrapper);
        if (updated > 0) {
            log.info("预约爽约处理完成，处理数量：{}，截止时间：{}", updated, deadline);
        }
    }
}
```

启动类需要开启定时任务。

文件位置：`src/main/java/io/github/atengk/appointment/AppointmentApplication.java`

```java
package io.github.atengk.appointment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 预约排班服务启动类
 *
 * @author Ateng
 * @since 2026-05-15
 */
@EnableScheduling
@SpringBootApplication
public class AppointmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppointmentApplication.class, args);
    }
}
```

验证 SQL：

```sql
SELECT id,
       user_id,
       start_time,
       status
FROM appointment_order
WHERE status = 4
ORDER BY update_time DESC;
```

生产环境需要注意：

```text
1. 爽约任务要避免和签到接口并发修改同一预约单。
2. 更新条件必须带 status = 已预约。
3. 如果使用 XXL-JOB，多节点部署时由调度中心控制执行实例。
4. 爽约后是否释放资源，要根据业务决定。本案例中爽约不再参与后续冲突检测。
```

### WebSocket 预约状态推送

WebSocket 适合在预约创建、取消、签到、爽约后推送状态变化。典型场景包括会议室大屏、医生叫号屏、场馆预约状态页、设备占用看板。

推送事件可以统一定义为：

```json
{
  "eventType": "APPOINTMENT_STATUS_CHANGED",
  "resourceId": 1001,
  "appointmentId": 1900000000000000001,
  "status": 3,
  "message": "预约已签到"
}
```

简单实现可以先使用 Spring WebSocket。先添加依赖：

```xml
<!-- Spring WebSocket：用于预约状态实时推送 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

定义推送消息对象。

文件位置：`src/main/java/io/github/atengk/appointment/domain/vo/AppointmentPushMessage.java`

```java
package io.github.atengk.appointment.domain.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 预约状态推送消息
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
public class AppointmentPushMessage {

    private String eventType;

    private Long resourceId;

    private Long appointmentId;

    private Integer status;

    private String message;
}
```

定义 WebSocket 推送服务。这里使用 Hutool `JSONUtil` 做消息序列化。

文件位置：`src/main/java/io/github/atengk/appointment/service/AppointmentPushService.java`

```java
package io.github.atengk.appointment.service;

import io.github.atengk.appointment.domain.vo.AppointmentPushMessage;

/**
 * 预约状态推送服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface AppointmentPushService {

    /**
     * 推送资源预约状态变化
     *
     * @param message 推送消息
     */
    void pushResourceStatus(AppointmentPushMessage message);
}
```

文件位置：`src/main/java/io/github/atengk/appointment/service/impl/AppointmentPushServiceImpl.java`

```java
package io.github.atengk.appointment.service.impl;

import cn.hutool.json.JSONUtil;
import io.github.atengk.appointment.domain.vo.AppointmentPushMessage;
import io.github.atengk.appointment.service.AppointmentPushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 预约状态推送服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
public class AppointmentPushServiceImpl implements AppointmentPushService {

    /**
     * 推送资源预约状态变化
     *
     * @param message 推送消息
     */
    @Override
    public void pushResourceStatus(AppointmentPushMessage message) {
        String payload = JSONUtil.toJsonStr(message);

        // 这里可以替换为实际 WebSocket Session 推送逻辑。
        // 单机版可以维护 resourceId -> Session 集合。
        // 多节点版建议通过 Redis Pub/Sub 或 MQ 广播到各节点。
        log.info("推送预约状态变化：{}", payload);
    }
}
```

在预约创建成功、取消成功、签到成功、爽约处理成功后调用推送服务即可。例如签到成功后：

```java
appointmentPushService.pushResourceStatus(AppointmentPushMessage.builder()
        .eventType("APPOINTMENT_STATUS_CHANGED")
        .resourceId(order.getResourceId())
        .appointmentId(order.getId())
        .status(AppointmentStatusEnum.CHECKED_IN.getCode())
        .message("预约已签到")
        .build());
```

生产环境建议采用下面的推送架构：

```text
业务服务更新预约状态
-> 发送预约状态变更事件到 RabbitMQ / Redis Stream
-> WebSocket 服务消费事件
-> 按 resourceId / userId / tenantId 推送给在线客户端
```

这样可以避免业务服务直接维护大量 WebSocket 连接，也更适合多节点部署。