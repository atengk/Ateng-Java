# 多租户业务隔离

本文围绕 Java 后端经典业务场景中的「多租户业务隔离」展开，目标是实现 SaaS 系统中最核心的数据、缓存、文件和任务隔离能力。原始 README 中对该场景的定义包含租户开通、租户数据初始化、SQL 自动隔离、Redis Key 隔离、文件路径隔离和定时任务按租户执行等内容，本案例会围绕这些核心点落地实现。

## 场景目标与实现范围

本案例以「SaaS CRM 客户管理」作为演示业务。系统中存在多个企业租户，每个租户都有自己的用户、客户、缓存和文件数据。用户登录后进入某一个租户空间，后续所有业务操作都必须在当前租户范围内执行。

本案例重点实现共享数据库、共享业务表、通过 `tenant_id` 字段隔离数据的多租户方案。该方案适合大多数中小型 SaaS、企业服务平台、多商户后台系统，落地成本低，也便于和 MyBatis-Plus、Redis、MinIO、XXL-JOB 等常见技术栈集成。

实现范围如下：

| 能力           | 实现说明                                              |
| -------------- | ----------------------------------------------------- |
| 租户上下文     | 使用 `TransmittableThreadLocal` 保存当前请求的租户 ID |
| 请求租户解析   | 从请求头 `X-Tenant-Id` 获取当前租户                   |
| 租户状态校验   | 拦截不存在、停用、过期的租户                          |
| 用户租户校验   | 校验当前登录用户是否属于该租户                        |
| SQL 自动隔离   | 使用 MyBatis-Plus 多租户插件自动追加 `tenant_id`      |
| 新增自动填充   | 新增业务数据时自动填充当前租户 ID                     |
| Redis Key 隔离 | 统一封装租户级 Redis Key                              |
| 文件路径隔离   | MinIO 文件路径按租户目录存储                          |
| 定时任务隔离   | XXL-JOB 按租户循环执行业务任务                        |

本案例不展开以下复杂内容：

```text
租户独立数据库模式
租户独立 Schema 模式
复杂套餐计费系统
完整 RBAC 权限系统
完整 Spring Cloud Gateway 租户透传
完整前端租户切换页面
```

### 业务场景

假设当前系统是一个 SaaS CRM 平台，平台为多个企业提供客户管理能力。每个企业就是一个租户，不同租户之间的数据必须严格隔离。

示例租户如下：

| 租户 ID | 租户名称             | 状态 |
| ------- | -------------------- | ---- |
| 10001   | 北京演示科技有限公司 | 正常 |
| 10002   | 上海测试信息有限公司 | 正常 |
| 10003   | 广州停用租户有限公司 | 停用 |

用户进入系统后的核心流程如下：

```text
用户登录
-> 查询用户可访问的租户列表
-> 用户选择当前租户
-> 前端请求头携带 X-Tenant-Id
-> 后端解析租户 ID
-> 校验租户状态和用户租户关系
-> 写入租户上下文
-> 执行业务逻辑
-> MyBatis-Plus 自动拼接 tenant_id
-> 请求结束清理租户上下文
```

以客户管理为例，不同租户可以创建相同名称的客户，但查询时只能看到自己租户下的数据。

| 当前租户   | 查询条件     | 查询结果                          |
| ---------- | ------------ | --------------------------------- |
| 10001      | 查询客户列表 | 只返回 `tenant_id = 10001` 的客户 |
| 10002      | 查询客户列表 | 只返回 `tenant_id = 10002` 的客户 |
| 未携带租户 | 查询客户列表 | 拒绝访问                          |
| 停用租户   | 查询客户列表 | 拒绝访问                          |

核心业务规则如下：

```text
业务表必须包含 tenant_id 字段
普通业务接口必须携带租户 ID
新增业务数据自动写入当前租户 ID
查询业务数据自动追加当前租户条件
用户不能访问未加入的租户
停用租户不能继续访问业务接口
Redis 缓存必须按租户生成 Key
文件上传路径必须按租户分目录
定时任务必须按租户逐个执行
```

### 核心能力清单

本案例的核心不是把每个业务接口都手动加上 `tenant_id`，而是通过公共组件统一完成租户识别、租户校验、SQL 隔离和资源隔离。

核心能力如下：

| 核心能力       | 落地方式                                             |
| -------------- | ---------------------------------------------------- |
| 租户上下文传递 | `TenantContext` + `TransmittableThreadLocal`         |
| Web 请求拦截   | `TenantInterceptor` 解析请求头                       |
| 租户合法性校验 | 校验租户状态、过期时间、用户租户关系                 |
| SQL 自动隔离   | MyBatis-Plus `TenantLineInnerInterceptor`            |
| 新增字段填充   | MyBatis-Plus `MetaObjectHandler`                     |
| 忽略系统表     | 租户表、用户表、用户租户关系表不拼接租户条件         |
| 缓存隔离       | `TenantRedisKey` 统一构建缓存 Key                    |
| 文件隔离       | 文件路径使用 `tenant/{tenantId}/...` 格式            |
| 定时任务隔离   | 查询有效租户列表，循环设置租户上下文执行             |
| 防止上下文污染 | 请求结束、任务结束后必须执行 `TenantContext.clear()` |

核心设计原则如下：

```text
能由框架自动隔离的，不在业务代码里手写 tenant_id
能由拦截器统一校验的，不在每个 Controller 中重复判断
能由工具类统一生成的，不在业务代码里手动拼接 Redis Key 和文件路径
涉及 ThreadLocal 的地方必须及时清理，避免线程复用导致串租
```

### 技术栈选型

本案例使用 Spring Boot 3 作为基础框架，配合 MyBatis-Plus 实现 SQL 级租户隔离，使用 Redis 和 MinIO 演示缓存与文件隔离，使用 XXL-JOB 演示定时任务按租户执行。

| 技术                     | 用途                                |
| ------------------------ | ----------------------------------- |
| Spring Boot 3            | 后端基础框架                        |
| MyBatis-Plus             | ORM、字段填充、多租户插件           |
| MySQL                    | 存储租户、用户、客户等业务数据      |
| Redis                    | 缓存租户信息和业务数据              |
| Sa-Token                 | 登录认证，获取当前用户 ID           |
| TransmittableThreadLocal | 保存和传递租户上下文                |
| MinIO                    | 对象存储，演示文件路径隔离          |
| XXL-JOB                  | 定时任务，演示按租户执行任务        |
| Hutool                   | 字符串、集合、日期、JSON 等工具处理 |
| Lombok                   | 简化实体类、日志和构造器代码        |

本案例采用的多租户模型是：

```text
共享数据库
共享数据表
通过 tenant_id 字段隔离租户数据
```

示例数据表包括：

```text
sys_tenant        租户表
sys_user          用户表
sys_tenant_user   用户租户关系表
crm_customer      客户业务表
```

其中 `crm_customer` 是典型业务表，必须包含 `tenant_id` 字段；`sys_tenant`、`sys_user`、`sys_tenant_user` 属于系统基础表，通常需要在 MyBatis-Plus 多租户插件中配置为忽略租户隔离。

## 项目结构

本案例按单体 Spring Boot 项目组织代码，重点演示多租户隔离的核心能力：租户上下文、接口拦截、SQL 自动拼接、字段自动填充、Redis Key 隔离、MinIO 路径隔离和 XXL-JOB 租户任务隔离。该结构对应原始场景中提到的租户上下文传递、SQL 自动拼接 `tenant_id`、缓存隔离、文件隔离和定时任务隔离能力。

### 模块目录规划

项目基础包名使用 `io.github.atengk`，后续代码都放在该包路径下。

```text
tenant-isolation-demo
├── pom.xml
└── src
    └── main
        ├── java
        │   └── io
        │       └── github
        │           └── atengk
        │               ├── TenantIsolationApplication.java
        │               ├── common
        │               │   ├── context
        │               │   │   └── TenantContext.java
        │               │   ├── exception
        │               │   │   ├── BizException.java
        │               │   │   └── GlobalExceptionHandler.java
        │               │   ├── result
        │               │   │   └── R.java
        │               │   └── util
        │               │       ├── TenantRedisKey.java
        │               │       └── TenantFilePath.java
        │               ├── config
        │               │   ├── MybatisPlusConfig.java
        │               │   ├── MyMetaObjectHandler.java
        │               │   ├── TenantWebConfig.java
        │               │   └── RedisConfig.java
        │               ├── interceptor
        │               │   └── TenantInterceptor.java
        │               ├── module
        │               │   ├── tenant
        │               │   │   ├── controller
        │               │   │   │   └── TenantController.java
        │               │   │   ├── entity
        │               │   │   │   ├── SysTenant.java
        │               │   │   │   └── SysTenantUser.java
        │               │   │   ├── mapper
        │               │   │   │   ├── SysTenantMapper.java
        │               │   │   │   └── SysTenantUserMapper.java
        │               │   │   ├── service
        │               │   │   │   ├── SysTenantService.java
        │               │   │   │   └── SysTenantUserService.java
        │               │   │   └── service
        │               │   │       └── impl
        │               │   │           ├── SysTenantServiceImpl.java
        │               │   │           └── SysTenantUserServiceImpl.java
        │               │   └── customer
        │               │       ├── controller
        │               │       │   └── CustomerController.java
        │               │       ├── dto
        │               │       │   └── CustomerCreateDTO.java
        │               │       ├── entity
        │               │       │   └── CrmCustomer.java
        │               │       ├── mapper
        │               │       │   └── CrmCustomerMapper.java
        │               │       ├── service
        │               │       │   └── CrmCustomerService.java
        │               │       └── service
        │               │           └── impl
        │               │               └── CrmCustomerServiceImpl.java
        │               └── job
        │                   └── TenantCustomerStatJob.java
        └── resources
            ├── application.yml
            └── mapper
                └── customer
                    └── CrmCustomerMapper.xml
```

目录设计重点如下：

| 目录               | 作用                                                  |
| ------------------ | ----------------------------------------------------- |
| `common/context`   | 存放租户上下文工具类，负责保存、获取、清理当前租户 ID |
| `common/util`      | 存放租户级 Redis Key、文件路径等公共工具              |
| `config`           | 存放 MyBatis-Plus、字段填充、Web 拦截器、Redis 等配置 |
| `interceptor`      | 存放租户请求拦截器                                    |
| `module/tenant`    | 租户、用户租户关系相关代码                            |
| `module/customer`  | 客户管理业务代码，用于验证租户数据隔离                |
| `job`              | 定时任务代码，用于演示按租户循环执行任务              |
| `resources/mapper` | 存放 MyBatis XML，自定义复杂 SQL 时使用               |

### 核心类职责

下面这些类是本案例的核心类，后续章节会逐步实现。

| 类名                    | 文件位置                                             | 核心职责                                               |
| ----------------------- | ---------------------------------------------------- | ------------------------------------------------------ |
| `TenantContext`         | `common/context/TenantContext.java`                  | 使用 `TransmittableThreadLocal` 保存当前租户 ID        |
| `TenantInterceptor`     | `interceptor/TenantInterceptor.java`                 | 从请求头 `X-Tenant-Id` 解析租户 ID，并校验租户状态     |
| `TenantWebConfig`       | `config/TenantWebConfig.java`                        | 注册租户拦截器，配置需要拦截和放行的接口               |
| `MybatisPlusConfig`     | `config/MybatisPlusConfig.java`                      | 配置 MyBatis-Plus 多租户插件，自动追加 `tenant_id`     |
| `MyMetaObjectHandler`   | `config/MyMetaObjectHandler.java`                    | 新增业务数据时自动填充 `tenant_id`、创建时间、更新时间 |
| `TenantRedisKey`        | `common/util/TenantRedisKey.java`                    | 统一生成租户隔离的 Redis Key                           |
| `TenantFilePath`        | `common/util/TenantFilePath.java`                    | 统一生成租户隔离的文件存储路径                         |
| `SysTenant`             | `module/tenant/entity/SysTenant.java`                | 租户实体，维护租户状态、到期时间等信息                 |
| `SysTenantUser`         | `module/tenant/entity/SysTenantUser.java`            | 用户与租户的关系实体                                   |
| `CrmCustomer`           | `module/customer/entity/CrmCustomer.java`            | 客户业务实体，包含 `tenant_id` 字段                    |
| `CustomerController`    | `module/customer/controller/CustomerController.java` | 提供客户新增、查询接口，用于验证租户隔离               |
| `TenantCustomerStatJob` | `job/TenantCustomerStatJob.java`                     | XXL-JOB 任务示例，按有效租户逐个执行统计逻辑           |

核心调用链如下：

```text
HTTP 请求
-> TenantInterceptor
-> TenantContext.setTenantId()
-> Controller
-> Service
-> Mapper
-> MyBatis-Plus 多租户插件自动拼接 tenant_id
-> 返回响应
-> TenantContext.clear()
```

新增客户数据时的处理链路如下：

```text
POST /customers
-> TenantInterceptor 写入租户上下文
-> CustomerController 接收请求
-> CustomerService 保存客户
-> MyMetaObjectHandler 自动填充 tenant_id
-> MyBatis-Plus 执行 INSERT
```

查询客户数据时的处理链路如下：

```text
GET /customers
-> TenantInterceptor 写入租户上下文
-> CustomerController 查询客户列表
-> MyBatis-Plus 多租户插件自动追加 WHERE tenant_id = 当前租户ID
-> 只返回当前租户客户数据
```

## 数据库设计

本案例采用「共享数据库 + 共享数据表 + `tenant_id` 字段隔离」模式。系统表负责维护租户和用户租户关系，业务表通过 `tenant_id` 归属到具体租户。

### 租户表设计

`sys_tenant` 用于保存租户基础信息。该表是系统基础表，不需要被 MyBatis-Plus 多租户插件追加 `tenant_id` 条件。

```sql
-- 租户表：保存 SaaS 平台中的企业租户信息
CREATE TABLE sys_tenant (
    id BIGINT NOT NULL COMMENT '租户ID',
    tenant_code VARCHAR(64) NOT NULL COMMENT '租户编码',
    tenant_name VARCHAR(128) NOT NULL COMMENT '租户名称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常，2停用',
    expire_time DATETIME DEFAULT NULL COMMENT '租户到期时间，NULL表示不过期',
    contact_name VARCHAR(64) DEFAULT NULL COMMENT '联系人姓名',
    contact_phone VARCHAR(32) DEFAULT NULL COMMENT '联系人手机号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_code (tenant_code),
    KEY idx_status_expire_time (status, expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户表';
```

字段说明如下：

| 字段          | 说明                                 |
| ------------- | ------------------------------------ |
| `id`          | 租户主键，建议使用雪花 ID            |
| `tenant_code` | 租户编码，适合展示、搜索和外部对接   |
| `tenant_name` | 租户名称                             |
| `status`      | 租户状态，停用后禁止访问业务接口     |
| `expire_time` | 租户到期时间，到期后可以拦截业务请求 |
| `deleted`     | 逻辑删除字段                         |

租户状态建议使用简单枚举：

```text
1 正常
2 停用
```

到期判断规则：

```text
expire_time IS NULL              -> 永不过期
expire_time >= 当前时间           -> 未过期
expire_time < 当前时间            -> 已过期
```

### 用户租户关系表设计

`sys_tenant_user` 用于保存用户和租户的绑定关系。一个用户可以加入多个租户，一个租户也可以拥有多个用户。

```sql
-- 用户租户关系表：用于判断当前登录用户是否有权限进入指定租户
CREATE TABLE sys_tenant_user (
    id BIGINT NOT NULL COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_code VARCHAR(64) DEFAULT NULL COMMENT '租户内角色编码，例如 owner、admin、member',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常，2禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_user (tenant_id, user_id),
    KEY idx_user_id (user_id),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户租户关系表';
```

字段说明如下：

| 字段             | 说明                             |
| ---------------- | -------------------------------- |
| `tenant_id`      | 用户所属租户                     |
| `user_id`        | 平台用户 ID                      |
| `role_code`      | 用户在当前租户下的角色           |
| `status`         | 用户在该租户下是否可用           |
| `uk_tenant_user` | 防止同一个用户重复加入同一个租户 |

该表主要用于两个场景：

```text
登录后查询用户可访问的租户列表
请求业务接口时校验用户是否属于当前租户
```

典型校验 SQL 如下：

```sql
-- 校验用户是否属于当前租户
SELECT COUNT(1)
FROM sys_tenant_user
WHERE tenant_id = #{tenantId}
  AND user_id = #{userId}
  AND status = 1
  AND deleted = 0;
```

### 业务表 tenant_id 字段设计

所有需要按租户隔离的业务表都必须包含 `tenant_id` 字段。例如客户表 `crm_customer`。

```sql
-- 客户表：典型租户业务表，必须包含 tenant_id 字段
CREATE TABLE crm_customer (
    id BIGINT NOT NULL COMMENT '客户ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    customer_name VARCHAR(128) NOT NULL COMMENT '客户名称',
    customer_phone VARCHAR(32) DEFAULT NULL COMMENT '客户手机号',
    customer_level TINYINT NOT NULL DEFAULT 1 COMMENT '客户等级：1普通，2重要，3核心',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    updated_by BIGINT DEFAULT NULL COMMENT '更新人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    KEY idx_tenant_customer_name (tenant_id, customer_name),
    KEY idx_tenant_created_at (tenant_id, created_at),
    KEY idx_tenant_phone (tenant_id, customer_phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户表';
```

业务表设计规则如下：

| 规则                       | 说明                                             |
| -------------------------- | ------------------------------------------------ |
| 必须包含 `tenant_id`       | 所有租户业务表都需要该字段                       |
| `tenant_id` 不允许为空     | 防止产生无法归属的脏数据                         |
| 常用索引带上 `tenant_id`   | 提升租户内查询性能                               |
| 不建议全局唯一业务字段     | 例如客户手机号通常应允许不同租户重复             |
| 唯一索引应带上 `tenant_id` | 例如 `(tenant_id, customer_phone)`               |
| 系统基础表可忽略租户插件   | 例如 `sys_tenant`、`sys_user`、`sys_tenant_user` |

如果客户手机号在同一租户内必须唯一，可以增加如下唯一索引：

```sql
-- 同一租户内客户手机号唯一，不同租户之间允许重复
ALTER TABLE crm_customer
ADD UNIQUE KEY uk_tenant_customer_phone (tenant_id, customer_phone);
```

不建议这样设计：

```sql
-- 不推荐：这会导致不同租户之间也不能存在相同手机号
ALTER TABLE crm_customer
ADD UNIQUE KEY uk_customer_phone (customer_phone);
```

多租户业务表的查询索引建议优先考虑以下结构：

```text
(tenant_id, 业务查询字段)
(tenant_id, created_at)
(tenant_id, status)
(tenant_id, deleted)
```

原因是绝大多数业务查询都会被自动追加 `tenant_id = 当前租户ID` 条件，复合索引把 `tenant_id` 放在前面更容易命中索引。

### 初始化 SQL 脚本

下面的 SQL 可以直接作为本案例的初始化脚本使用。

文件位置：`src/main/resources/sql/init.sql`

```sql
-- 初始化数据库
CREATE DATABASE IF NOT EXISTS tenant_demo
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_general_ci;

USE tenant_demo;

-- 租户表
DROP TABLE IF EXISTS sys_tenant;
CREATE TABLE sys_tenant (
    id BIGINT NOT NULL COMMENT '租户ID',
    tenant_code VARCHAR(64) NOT NULL COMMENT '租户编码',
    tenant_name VARCHAR(128) NOT NULL COMMENT '租户名称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常，2停用',
    expire_time DATETIME DEFAULT NULL COMMENT '租户到期时间，NULL表示不过期',
    contact_name VARCHAR(64) DEFAULT NULL COMMENT '联系人姓名',
    contact_phone VARCHAR(32) DEFAULT NULL COMMENT '联系人手机号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_code (tenant_code),
    KEY idx_status_expire_time (status, expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户表';

-- 用户表：本案例只保留最小字段，用于配合 Sa-Token 登录用户ID
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT NOT NULL COMMENT '用户ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    password VARCHAR(128) DEFAULT NULL COMMENT '密码，案例中不展开登录逻辑',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常，2禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 用户租户关系表
DROP TABLE IF EXISTS sys_tenant_user;
CREATE TABLE sys_tenant_user (
    id BIGINT NOT NULL COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_code VARCHAR(64) DEFAULT NULL COMMENT '租户内角色编码，例如 owner、admin、member',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常，2禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_user (tenant_id, user_id),
    KEY idx_user_id (user_id),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户租户关系表';

-- 客户表：租户业务表
DROP TABLE IF EXISTS crm_customer;
CREATE TABLE crm_customer (
    id BIGINT NOT NULL COMMENT '客户ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    customer_name VARCHAR(128) NOT NULL COMMENT '客户名称',
    customer_phone VARCHAR(32) DEFAULT NULL COMMENT '客户手机号',
    customer_level TINYINT NOT NULL DEFAULT 1 COMMENT '客户等级：1普通，2重要，3核心',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    updated_by BIGINT DEFAULT NULL COMMENT '更新人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    KEY idx_tenant_customer_name (tenant_id, customer_name),
    KEY idx_tenant_created_at (tenant_id, created_at),
    KEY idx_tenant_phone (tenant_id, customer_phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户表';

-- 初始化租户数据
INSERT INTO sys_tenant (
    id, tenant_code, tenant_name, status, expire_time, contact_name, contact_phone
) VALUES
    (10001, 'tenant_beijing_demo', '北京演示科技有限公司', 1, NULL, '张三', '13800000001'),
    (10002, 'tenant_shanghai_test', '上海测试信息有限公司', 1, NULL, '李四', '13800000002'),
    (10003, 'tenant_guangzhou_disabled', '广州停用租户有限公司', 2, NULL, '王五', '13800000003');

-- 初始化用户数据
INSERT INTO sys_user (
    id, username, nickname, password, status
) VALUES
    (1, 'admin', '平台管理员', NULL, 1),
    (2, 'tenant_user', '租户用户', NULL, 1);

-- 初始化用户租户关系
INSERT INTO sys_tenant_user (
    id, tenant_id, user_id, role_code, status
) VALUES
    (100001, 10001, 1, 'owner', 1),
    (100002, 10002, 1, 'admin', 1),
    (100003, 10001, 2, 'member', 1),
    (100004, 10003, 1, 'owner', 1);

-- 初始化客户数据：两个租户可以存在相同客户名称，用于验证租户隔离
INSERT INTO crm_customer (
    id, tenant_id, customer_name, customer_phone, customer_level, remark, created_by, updated_by
) VALUES
    (200001, 10001, '华北测试客户', '13900000001', 2, '北京租户客户数据', 1, 1),
    (200002, 10001, '公共客户A', '13900000002', 1, '北京租户下的公共客户A', 1, 1),
    (200003, 10002, '华东测试客户', '13900000003', 2, '上海租户客户数据', 1, 1),
    (200004, 10002, '公共客户A', '13900000004', 1, '上海租户下的公共客户A', 1, 1);
```

执行初始化脚本：

```bash
mysql -uroot -p < src/main/resources/sql/init.sql
```

执行后可以先用 SQL 验证基础数据：

```sql
-- 查看租户
SELECT id, tenant_code, tenant_name, status
FROM sys_tenant
ORDER BY id;

-- 查看用户可访问租户
SELECT tenant_id, user_id, role_code, status
FROM sys_tenant_user
WHERE user_id = 1
ORDER BY tenant_id;

-- 查看客户数据是否已经按 tenant_id 分布
SELECT tenant_id, customer_name, customer_phone
FROM crm_customer
ORDER BY tenant_id, id;
```

预期客户数据分布如下：

| tenant_id | 客户数量 | 说明                     |
| --------- | -------- | ------------------------ |
| `10001`   | 2        | 北京演示科技有限公司客户 |
| `10002`   | 2        | 上海测试信息有限公司客户 |
| `10003`   | 0        | 停用租户暂无客户数据     |

## 租户上下文实现

租户上下文负责在一次请求、一次异步任务或一次定时任务执行期间保存当前租户 ID。原始场景中明确要求处理「租户上下文传递」「SQL 自动拼接 tenant_id」「缓存隔离」「定时任务隔离」等能力，本节先实现最基础的租户上下文和 Web 请求注入逻辑。

### TenantContext 上下文工具类

`TenantContext` 使用 `TransmittableThreadLocal` 保存当前租户 ID。相比普通 `ThreadLocal`，它在异步线程池、任务线程中更容易传递上下文，适合多租户场景。

文件位置：`src/main/java/io/github/atengk/common/context/TenantContext.java`

该类用于设置、获取、校验和清理当前线程中的租户 ID。

```java
package io.github.atengk.common.context;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * 租户上下文
 *
 * @author Ateng
 * @since 2026-05-15
 */
public final class TenantContext {

    private static final TransmittableThreadLocal<Long> TENANT_ID_HOLDER = new TransmittableThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(Long tenantId) {
        TENANT_ID_HOLDER.set(tenantId);
    }

    public static Long getTenantId() {
        return TENANT_ID_HOLDER.get();
    }

    public static Long requireTenantId() {
        Long tenantId = getTenantId();
        if (ObjectUtil.isNull(tenantId)) {
            throw new IllegalStateException("当前请求缺少租户上下文");
        }
        return tenantId;
    }

    public static boolean hasTenantId() {
        return ObjectUtil.isNotNull(getTenantId());
    }

    public static void clear() {
        TENANT_ID_HOLDER.remove();
    }
}
```

这里必须提供 `clear()` 方法。Web 容器线程会被复用，如果请求结束后不清理上下文，后续请求可能复用上一次请求的租户 ID，造成严重串租问题。

### 请求头租户标识解析

本案例约定前端在访问租户业务接口时，通过请求头传递当前租户 ID。

```text
X-Tenant-Id: 10001
```

推荐约定如下：

| 请求头          | 说明                                  |
| --------------- | ------------------------------------- |
| `X-Tenant-Id`   | 当前访问的租户 ID                     |
| `Authorization` | 登录 Token，由 Sa-Token 解析          |
| `Content-Type`  | 请求数据格式，例如 `application/json` |

典型请求示例：

```bash
curl -X GET "http://localhost:8080/customers" \
  -H "Authorization: Bearer your-token" \
  -H "X-Tenant-Id: 10001"
```

后端解析租户 ID 时需要做三个校验：

```text
请求头不能为空
租户 ID 必须是数字
租户 ID 必须大于 0
```

### Web 拦截器注入租户上下文

`TenantInterceptor` 负责从请求头解析 `X-Tenant-Id`，校验租户状态，校验当前登录用户是否属于该租户，然后写入 `TenantContext`。

这里依赖两个 Service：

```text
SysTenantService       校验租户是否存在、是否停用、是否过期
SysTenantUserService   校验当前用户是否属于该租户
```

文件位置：`src/main/java/io/github/atengk/interceptor/TenantInterceptor.java`

该拦截器用于在 Controller 执行前注入租户上下文，并在请求结束后清理上下文。

```java
package io.github.atengk.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.context.TenantContext;
import io.github.atengk.common.exception.BizException;
import io.github.atengk.module.tenant.service.SysTenantService;
import io.github.atengk.module.tenant.service.SysTenantUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 租户请求拦截器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    private final SysTenantService sysTenantService;
    private final SysTenantUserService sysTenantUserService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantIdText = request.getHeader(TENANT_HEADER);
        if (StrUtil.isBlank(tenantIdText)) {
            throw new BizException("请求头缺少租户标识：" + TENANT_HEADER);
        }
        if (!NumberUtil.isLong(tenantIdText)) {
            throw new BizException("租户标识格式错误");
        }

        Long tenantId = Long.valueOf(tenantIdText);
        if (tenantId <= 0) {
            throw new BizException("租户标识不合法");
        }

        sysTenantService.checkTenantAvailable(tenantId);

        Long userId = StpUtil.getLoginIdAsLong();
        boolean joined = sysTenantUserService.existsNormalRelation(tenantId, userId);
        if (!joined) {
            log.warn("用户访问未授权租户，userId={}，tenantId={}", userId, tenantId);
            throw new BizException("当前用户无权访问该租户");
        }

        TenantContext.setTenantId(tenantId);
        log.debug("租户上下文注入完成，tenantId={}，userId={}", tenantId, userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}
```

如果当前项目暂时没有接入 Sa-Token，可以先把 `StpUtil.getLoginIdAsLong()` 替换成固定用户 ID 进行本地验证：

```java
Long userId = 1L;
```

但正式项目不建议这样写，用户租户关系必须基于真实登录用户校验。

注册拦截器：

文件位置：`src/main/java/io/github/atengk/config/TenantWebConfig.java`

该配置类用于把 `TenantInterceptor` 注册到 Spring MVC，并排除登录、健康检查、公开接口等不需要租户上下文的路径。

```java
package io.github.atengk.config;

import io.github.atengk.interceptor.TenantInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 租户 Web 配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Configuration
@RequiredArgsConstructor
public class TenantWebConfig implements WebMvcConfigurer {

    private final TenantInterceptor tenantInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/**",
                        "/tenant/list",
                        "/actuator/**",
                        "/error",
                        "/doc.html",
                        "/v3/api-docs/**",
                        "/swagger-ui/**"
                );
    }
}
```

这里的排除路径需要根据实际项目调整。一般来说，登录接口、验证码接口、租户列表接口、健康检查接口不应该强制携带 `X-Tenant-Id`。

### 请求结束清理上下文

租户上下文必须在请求结束时清理。本案例在 `TenantInterceptor.afterCompletion()` 中执行：

```java
TenantContext.clear();
```

这一步不能省略。原因如下：

```text
Tomcat 工作线程会复用
ThreadLocal 数据不会自动随着请求结束销毁
如果不 clear，后续请求可能拿到上一个请求的 tenantId
一旦出现串租，可能导致跨租户数据泄露
```

除了 Web 请求，定时任务和异步任务也必须遵循相同规则：

```java
try {
    TenantContext.setTenantId(tenantId);
    // 执行业务逻辑
} finally {
    TenantContext.clear();
}
```

## MyBatis-Plus 多租户隔离

MyBatis-Plus 多租户插件是本案例的数据隔离核心。它会在执行 SQL 时自动追加 `tenant_id` 条件，避免业务代码在每个查询里手写租户条件。

### 多租户插件配置

MyBatis-Plus 通过 `TenantLineInnerInterceptor` 实现多租户隔离。对于普通查询、更新、删除 SQL，会自动追加当前租户条件。

文件位置：`src/main/java/io/github/atengk/config/MybatisPlusConfig.java`

该配置类用于启用 MyBatis-Plus 多租户插件，并声明哪些表需要忽略租户隔离。

```java
package io.github.atengk.config;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import io.github.atengk.common.context.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * MyBatis-Plus 配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Configuration
public class MybatisPlusConfig {

    private static final String TENANT_ID_COLUMN = "tenant_id";

    private static final Set<String> IGNORE_TENANT_TABLES = CollUtil.newHashSet(
            "sys_tenant",
            "sys_user",
            "sys_tenant_user"
    );

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(tenantLineHandler()));
        return interceptor;
    }

    @Bean
    public TenantLineHandler tenantLineHandler() {
        return new TenantLineHandler() {

            @Override
            public Expression getTenantId() {
                return new LongValue(TenantContext.requireTenantId());
            }

            @Override
            public String getTenantIdColumn() {
                return TENANT_ID_COLUMN;
            }

            @Override
            public boolean ignoreTable(String tableName) {
                return IGNORE_TENANT_TABLES.contains(tableName);
            }
        };
    }
}
```

核心点是 `getTenantId()`：

```java
return new LongValue(TenantContext.requireTenantId());
```

它会从当前线程租户上下文中获取租户 ID。如果当前业务表需要租户隔离，但请求中没有租户上下文，会直接抛出异常，避免执行无租户条件的 SQL。

### tenant_id 自动拼接规则

假设业务代码中写的是普通查询：

```java
lambdaQuery()
        .like(StrUtil.isNotBlank(keyword), CrmCustomer::getCustomerName, keyword)
        .orderByDesc(CrmCustomer::getCreatedAt)
        .list();
```

实际执行时，MyBatis-Plus 会自动把它改造成带租户条件的 SQL。

业务代码逻辑：

```sql
SELECT id, tenant_id, customer_name, customer_phone, created_at
FROM crm_customer
WHERE deleted = 0
ORDER BY created_at DESC;
```

插件处理后的逻辑效果：

```sql
SELECT id, tenant_id, customer_name, customer_phone, created_at
FROM crm_customer
WHERE tenant_id = 10001
  AND deleted = 0
ORDER BY created_at DESC;
```

更新操作同样会自动追加租户条件。

业务代码逻辑：

```sql
UPDATE crm_customer
SET customer_name = '新客户名称'
WHERE id = 200001;
```

插件处理后的逻辑效果：

```sql
UPDATE crm_customer
SET customer_name = '新客户名称'
WHERE id = 200001
  AND tenant_id = 10001;
```

删除操作也会自动追加租户条件。

业务代码逻辑：

```sql
DELETE FROM crm_customer
WHERE id = 200001;
```

插件处理后的逻辑效果：

```sql
DELETE FROM crm_customer
WHERE id = 200001
  AND tenant_id = 10001;
```

因此，业务层不需要重复拼接：

```java
.eq(CrmCustomer::getTenantId, TenantContext.getTenantId())
```

一般业务查询不建议手动写租户条件，避免出现重复条件、遗漏条件或维护不一致。

### 忽略租户隔离的表配置

不是所有表都应该追加 `tenant_id`。例如租户表本身、用户表、用户租户关系表属于系统基础表，如果强制追加租户条件，反而会导致登录、租户切换、租户校验无法正常执行。

本案例忽略以下表：

```java
private static final Set<String> IGNORE_TENANT_TABLES = CollUtil.newHashSet(
        "sys_tenant",
        "sys_user",
        "sys_tenant_user"
);
```

建议忽略租户隔离的表包括：

| 表名              | 是否忽略 | 原因                                   |
| ----------------- | -------- | -------------------------------------- |
| `sys_tenant`      | 是       | 租户基础表，本身不属于某个租户         |
| `sys_user`        | 是       | 用户可加入多个租户，通常属于平台级账号 |
| `sys_tenant_user` | 是       | 用于校验用户和租户关系                 |
| `sys_dict`        | 视情况   | 平台字典可忽略，租户自定义字典不忽略   |
| `sys_config`      | 视情况   | 平台配置可忽略，租户配置不忽略         |
| `crm_customer`    | 否       | 典型租户业务表，必须隔离               |

如果一个表既有平台级数据，又有租户级数据，不建议混在同一张表里。更稳妥的做法是拆成两张表：

```text
sys_dict_type       平台字典类型
tenant_dict_item    租户字典项
```

这样可以减少多租户插件的特殊判断，避免业务规则变复杂。

### 新增数据自动填充 tenant_id

查询、更新、删除可以通过多租户插件自动追加 `tenant_id`，但新增数据时，还需要自动把当前租户 ID 写入业务表。

MyBatis-Plus 可以通过 `MetaObjectHandler` 实现字段自动填充。

业务实体中的 `tenant_id` 字段需要配置自动填充。

文件位置：`src/main/java/io/github/atengk/module/customer/entity/CrmCustomer.java`

该实体类表示客户业务表，其中 `tenantId` 字段在新增时由当前租户上下文自动填充。

```java
package io.github.atengk.module.customer.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("crm_customer")
public class CrmCustomer {

    private Long id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    private String customerName;

    private String customerPhone;

    private Integer customerLevel;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Integer deleted;
}
```

然后配置字段填充处理器。

文件位置：`src/main/java/io/github/atengk/config/MyMetaObjectHandler.java`

该处理器用于在新增和更新数据时自动填充租户 ID、操作人和时间字段。

```java
package io.github.atengk.config;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import io.github.atengk.common.context.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 字段自动填充处理器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();

        if (hasField(metaObject, "tenantId") && TenantContext.hasTenantId()) {
            this.strictInsertFill(metaObject, "tenantId", Long.class, TenantContext.requireTenantId());
        }

        Long userId = getCurrentUserId();
        if (hasField(metaObject, "createdBy") && ObjectUtil.isNotNull(userId)) {
            this.strictInsertFill(metaObject, "createdBy", Long.class, userId);
        }
        if (hasField(metaObject, "updatedBy") && ObjectUtil.isNotNull(userId)) {
            this.strictInsertFill(metaObject, "updatedBy", Long.class, userId);
        }

        if (hasField(metaObject, "createdAt")) {
            this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        }
        if (hasField(metaObject, "updatedAt")) {
            this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Long userId = getCurrentUserId();
        if (hasField(metaObject, "updatedBy") && ObjectUtil.isNotNull(userId)) {
            this.strictUpdateFill(metaObject, "updatedBy", Long.class, userId);
        }
        if (hasField(metaObject, "updatedAt")) {
            this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        }
    }

    private boolean hasField(MetaObject metaObject, String fieldName) {
        return metaObject.hasGetter(fieldName) || metaObject.hasSetter(fieldName);
    }

    private Long getCurrentUserId() {
        try {
            return StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        } catch (Exception ex) {
            log.debug("获取当前登录用户失败，跳过用户字段填充");
            return null;
        }
    }
}
```

新增客户时，业务代码只需要设置客户自己的业务字段：

```java
CrmCustomer customer = new CrmCustomer();
customer.setCustomerName("测试客户");
customer.setCustomerPhone("13900000001");
customer.setCustomerLevel(1);
customer.setRemark("租户隔离测试数据");

crmCustomerMapper.insert(customer);
```

最终插入数据库时，`tenant_id` 会被自动填充为当前请求头中的租户 ID。

实际入库效果：

```text
id = 业务生成ID
tenant_id = TenantContext 当前租户ID
customer_name = 测试客户
customer_phone = 13900000001
customer_level = 1
created_by = 当前登录用户ID
updated_by = 当前登录用户ID
created_at = 当前时间
updated_at = 当前时间
```

注意：只有实体字段配置了 `fill = FieldFill.INSERT`，`MetaObjectHandler` 的自动填充才会生效。业务表如果需要租户隔离，实体类必须显式配置：

```java
@TableField(value = "tenant_id", fill = FieldFill.INSERT)
private Long tenantId;
```

## 登录用户与租户空间切换

登录后绑定当前租户的核心目标是：用户不是直接进入系统全局空间，而是进入某一个租户空间。后续请求可以从 Sa-Token 会话中读取当前租户，也可以通过请求头 `X-Tenant-Id` 显式传递租户 ID。原始场景中强调「用户进入租户空间」「查询自动隔离租户数据」「租户上下文传递」，本节重点实现租户空间切换和会话保存。

### 登录后绑定当前租户

用户登录成功后，系统应查询当前用户可访问的租户列表。如果用户只有一个租户，可以自动绑定为当前租户；如果用户有多个租户，返回租户列表给前端，让用户选择后再进入系统。

本案例约定 Sa-Token 会话中保存两个租户相关字段：

| Session Key           | 说明         |
| --------------------- | ------------ |
| `CURRENT_TENANT_ID`   | 当前租户 ID  |
| `CURRENT_TENANT_NAME` | 当前租户名称 |

登录后的租户处理流程如下：

```text
用户登录成功
-> 获取 userId
-> 查询用户可访问租户列表
-> 如果没有可访问租户，拒绝进入系统
-> 如果只有一个租户，自动写入 Sa-Token 会话
-> 如果有多个租户，返回租户列表，由用户选择
```

先定义租户会话 Key。

文件位置：`src/main/java/io/github/atengk/common/constant/TenantSessionKey.java`

该类用于统一管理 Sa-Token 会话中的租户字段名。

```java
package io.github.atengk.common.constant;

/**
 * 租户会话常量
 *
 * @author Ateng
 * @since 2026-05-15
 */
public final class TenantSessionKey {

    public static final String CURRENT_TENANT_ID = "CURRENT_TENANT_ID";

    public static final String CURRENT_TENANT_NAME = "CURRENT_TENANT_NAME";

    private TenantSessionKey() {
    }
}
```

定义返回给前端的租户信息。

文件位置：`src/main/java/io/github/atengk/module/tenant/vo/TenantVO.java`

该类用于返回用户可访问的租户基础信息。

```java
package io.github.atengk.module.tenant.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户视图对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class TenantVO {

    private Long tenantId;

    private String tenantCode;

    private String tenantName;

    private Integer status;

    private LocalDateTime expireTime;

    private Boolean current;
}
```

登录成功后可以调用如下方法绑定默认租户。

文件位置：`src/main/java/io/github/atengk/module/tenant/service/SysTenantService.java`

该接口提供租户校验、租户列表查询和当前租户绑定能力。

```java
package io.github.atengk.module.tenant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.atengk.module.tenant.entity.SysTenant;
import io.github.atengk.module.tenant.vo.TenantVO;

import java.util.List;

/**
 * 租户服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface SysTenantService extends IService<SysTenant> {

    void checkTenantAvailable(Long tenantId);

    List<TenantVO> listAvailableTenantsByUserId(Long userId);

    TenantVO bindDefaultTenantAfterLogin(Long userId);

    TenantVO switchTenant(Long userId, Long tenantId);

    Long getCurrentTenantIdFromSession();
}
```

下面是服务实现的核心代码。

文件位置：`src/main/java/io/github/atengk/module/tenant/service/impl/SysTenantServiceImpl.java`

该实现类用于查询用户可访问租户、绑定默认租户、切换租户，并把当前租户写入 Sa-Token 会话。

```java
package io.github.atengk.module.tenant.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.common.constant.TenantSessionKey;
import io.github.atengk.common.exception.BizException;
import io.github.atengk.module.tenant.entity.SysTenant;
import io.github.atengk.module.tenant.mapper.SysTenantMapper;
import io.github.atengk.module.tenant.service.SysTenantService;
import io.github.atengk.module.tenant.service.SysTenantUserService;
import io.github.atengk.module.tenant.vo.TenantVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 租户服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysTenantServiceImpl extends ServiceImpl<SysTenantMapper, SysTenant> implements SysTenantService {

    private static final int TENANT_STATUS_NORMAL = 1;

    private final SysTenantUserService sysTenantUserService;

    @Override
    public void checkTenantAvailable(Long tenantId) {
        SysTenant tenant = this.getById(tenantId);
        if (ObjectUtil.isNull(tenant) || ObjectUtil.equal(tenant.getDeleted(), 1)) {
            throw new BizException("租户不存在");
        }
        if (!ObjectUtil.equal(tenant.getStatus(), TENANT_STATUS_NORMAL)) {
            throw new BizException("租户已停用");
        }
        if (ObjectUtil.isNotNull(tenant.getExpireTime())
                && LocalDateTimeUtil.compare(tenant.getExpireTime(), LocalDateTime.now()) < 0) {
            throw new BizException("租户已到期");
        }
    }

    @Override
    public List<TenantVO> listAvailableTenantsByUserId(Long userId) {
        List<TenantVO> tenants = baseMapper.selectAvailableTenantsByUserId(userId);
        Long currentTenantId = getCurrentTenantIdFromSession();
        tenants.forEach(item -> item.setCurrent(ObjectUtil.equal(item.getTenantId(), currentTenantId)));
        return tenants;
    }

    @Override
    public TenantVO bindDefaultTenantAfterLogin(Long userId) {
        List<TenantVO> tenants = listAvailableTenantsByUserId(userId);
        if (CollUtil.isEmpty(tenants)) {
            throw new BizException("当前用户没有可访问的租户");
        }

        TenantVO firstTenant = tenants.get(0);
        setTenantSession(firstTenant);
        log.info("登录后绑定默认租户，userId={}，tenantId={}", userId, firstTenant.getTenantId());
        return firstTenant;
    }

    @Override
    public TenantVO switchTenant(Long userId, Long tenantId) {
        checkTenantAvailable(tenantId);

        boolean joined = sysTenantUserService.existsNormalRelation(tenantId, userId);
        if (!joined) {
            log.warn("租户切换失败，用户不属于该租户，userId={}，tenantId={}", userId, tenantId);
            throw new BizException("当前用户无权切换到该租户");
        }

        SysTenant tenant = this.getById(tenantId);
        TenantVO tenantVO = new TenantVO();
        tenantVO.setTenantId(tenant.getId());
        tenantVO.setTenantCode(tenant.getTenantCode());
        tenantVO.setTenantName(tenant.getTenantName());
        tenantVO.setStatus(tenant.getStatus());
        tenantVO.setExpireTime(tenant.getExpireTime());
        tenantVO.setCurrent(Boolean.TRUE);

        setTenantSession(tenantVO);
        log.info("租户切换成功，userId={}，tenantId={}", userId, tenantId);
        return tenantVO;
    }

    @Override
    public Long getCurrentTenantIdFromSession() {
        Object value = StpUtil.getSession().get(TenantSessionKey.CURRENT_TENANT_ID);
        return ObjectUtil.isNull(value) ? null : Long.valueOf(String.valueOf(value));
    }

    private void setTenantSession(TenantVO tenantVO) {
        StpUtil.getSession().set(TenantSessionKey.CURRENT_TENANT_ID, tenantVO.getTenantId());
        StpUtil.getSession().set(TenantSessionKey.CURRENT_TENANT_NAME, tenantVO.getTenantName());
    }
}
```

### 用户可访问租户校验

用户是否可以进入某个租户，不能只依赖前端传入的 `tenantId`。后端必须基于 `sys_tenant_user` 表校验用户和租户关系。

文件位置：`src/main/java/io/github/atengk/module/tenant/service/SysTenantUserService.java`

该接口用于判断用户是否属于指定租户。

```java
package io.github.atengk.module.tenant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.atengk.module.tenant.entity.SysTenantUser;

/**
 * 用户租户关系服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface SysTenantUserService extends IService<SysTenantUser> {

    boolean existsNormalRelation(Long tenantId, Long userId);
}
```

文件位置：`src/main/java/io/github/atengk/module/tenant/service/impl/SysTenantUserServiceImpl.java`

该实现类用于校验用户和租户之间是否存在正常绑定关系。

```java
package io.github.atengk.module.tenant.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.module.tenant.entity.SysTenantUser;
import io.github.atengk.module.tenant.mapper.SysTenantUserMapper;
import io.github.atengk.module.tenant.service.SysTenantUserService;
import org.springframework.stereotype.Service;

/**
 * 用户租户关系服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Service
public class SysTenantUserServiceImpl extends ServiceImpl<SysTenantUserMapper, SysTenantUser>
        implements SysTenantUserService {

    private static final int STATUS_NORMAL = 1;
    private static final int NOT_DELETED = 0;

    @Override
    public boolean existsNormalRelation(Long tenantId, Long userId) {
        return this.lambdaQuery()
                .eq(SysTenantUser::getTenantId, tenantId)
                .eq(SysTenantUser::getUserId, userId)
                .eq(SysTenantUser::getStatus, STATUS_NORMAL)
                .eq(SysTenantUser::getDeleted, NOT_DELETED)
                .exists();
    }
}
```

查询用户可访问租户列表使用自定义 SQL 更直观。

文件位置：`src/main/java/io/github/atengk/module/tenant/mapper/SysTenantMapper.java`

该 Mapper 提供用户租户列表查询方法。

```java
package io.github.atengk.module.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.module.tenant.entity.SysTenant;
import io.github.atengk.module.tenant.vo.TenantVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 租户 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface SysTenantMapper extends BaseMapper<SysTenant> {

    List<TenantVO> selectAvailableTenantsByUserId(@Param("userId") Long userId);
}
```

文件位置：`src/main/resources/mapper/tenant/SysTenantMapper.xml`

该 XML 用于查询当前用户可访问且状态正常的租户列表。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="io.github.atengk.module.tenant.mapper.SysTenantMapper">

    <!-- 查询用户可访问的正常租户列表 -->
    <select id="selectAvailableTenantsByUserId" resultType="io.github.atengk.module.tenant.vo.TenantVO">
        SELECT
            t.id AS tenantId,
            t.tenant_code AS tenantCode,
            t.tenant_name AS tenantName,
            t.status AS status,
            t.expire_time AS expireTime
        FROM sys_tenant_user tu
        INNER JOIN sys_tenant t ON t.id = tu.tenant_id
        WHERE tu.user_id = #{userId}
          AND tu.status = 1
          AND tu.deleted = 0
          AND t.status = 1
          AND t.deleted = 0
          AND (t.expire_time IS NULL OR t.expire_time >= NOW())
        ORDER BY t.id ASC
    </select>

</mapper>
```

因为 `sys_tenant` 和 `sys_tenant_user` 已经配置为忽略租户隔离，所以这个 SQL 不会被自动追加 `tenant_id` 条件。

### 切换租户接口实现

切换租户接口用于让用户在多个租户空间之间切换。切换成功后，后端把当前租户写入 Sa-Token 会话，前端也应把当前租户 ID 放入后续请求头 `X-Tenant-Id`。

文件位置：`src/main/java/io/github/atengk/module/tenant/dto/TenantSwitchDTO.java`

该 DTO 用于接收租户切换请求。

```java
package io.github.atengk.module.tenant.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 租户切换请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class TenantSwitchDTO {

    @NotNull(message = "租户ID不能为空")
    private Long tenantId;
}
```

文件位置：`src/main/java/io/github/atengk/module/tenant/controller/TenantController.java`

该控制器提供查询可访问租户、切换租户和查询当前租户接口。

```java
package io.github.atengk.module.tenant.controller;

import cn.dev33.satoken.stp.StpUtil;
import io.github.atengk.common.result.R;
import io.github.atengk.module.tenant.dto.TenantSwitchDTO;
import io.github.atengk.module.tenant.service.SysTenantService;
import io.github.atengk.module.tenant.vo.TenantVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租户控制器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequestMapping("/tenant")
@RequiredArgsConstructor
public class TenantController {

    private final SysTenantService sysTenantService;

    @GetMapping("/list")
    public R<List<TenantVO>> listMyTenants() {
        Long userId = StpUtil.getLoginIdAsLong();
        return R.ok(sysTenantService.listAvailableTenantsByUserId(userId));
    }

    @PostMapping("/switch")
    public R<TenantVO> switchTenant(@Valid @RequestBody TenantSwitchDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        return R.ok(sysTenantService.switchTenant(userId, dto.getTenantId()));
    }

    @GetMapping("/current")
    public R<Long> currentTenant() {
        return R.ok(sysTenantService.getCurrentTenantIdFromSession());
    }
}
```

接口调用示例：

```bash
# 查询当前用户可访问租户
curl -X GET "http://localhost:8080/tenant/list" \
  -H "Authorization: Bearer your-token"

# 切换当前租户
curl -X POST "http://localhost:8080/tenant/switch" \
  -H "Authorization: Bearer your-token" \
  -H "Content-Type: application/json" \
  -d '{"tenantId":10001}'

# 查询当前会话中的租户
curl -X GET "http://localhost:8080/tenant/current" \
  -H "Authorization: Bearer your-token"
```

前端切换成功后，后续业务请求应携带当前租户 ID：

```bash
curl -X GET "http://localhost:8080/customers" \
  -H "Authorization: Bearer your-token" \
  -H "X-Tenant-Id: 10001"
```

### Sa-Token 会话中保存租户信息

Sa-Token 会话中保存当前租户，可以解决两个问题：

```text
前端刷新页面后，可以查询当前租户
后端可以校验请求头租户是否与会话租户一致
```

可以在前面的 `TenantInterceptor` 中增加一段会话一致性校验，避免用户随意改请求头访问其他租户。

文件位置：`src/main/java/io/github/atengk/interceptor/TenantInterceptor.java`

在 `preHandle` 中校验用户关系之后，增加如下逻辑。

```java
Long sessionTenantId = sysTenantService.getCurrentTenantIdFromSession();
if (sessionTenantId != null && !sessionTenantId.equals(tenantId)) {
    log.warn("请求头租户与会话租户不一致，userId={}，headerTenantId={}，sessionTenantId={}",
            userId, tenantId, sessionTenantId);
    throw new BizException("请求租户与当前会话租户不一致，请重新切换租户");
}
```

如果你希望允许前端只通过请求头切换租户，不走 `/tenant/switch` 接口，也可以不做这段一致性校验。但更推荐使用明确的切换租户接口，这样租户切换行为可审计、可记录、可控制。

## Redis Key 租户隔离

Redis Key 隔离的目标是避免不同租户使用相同缓存 Key 导致数据串读。多租户系统中，不能直接使用 `customer:list`、`user:info` 这类全局 Key，而应该把租户 ID 加入 Key 前缀。

### Redis Key 命名规范

推荐 Redis Key 格式如下：

```text
tenant:{tenantId}:{business}:{key}
```

示例：

```text
tenant:10001:customer:detail:200001
tenant:10001:customer:list:keyword:abc
tenant:10002:customer:detail:200001
tenant:10002:customer:list:keyword:abc
```

同样的业务 ID 在不同租户下会生成不同 Key：

| 当前租户 | 原始业务 Key             | 最终 Redis Key                        |
| -------- | ------------------------ | ------------------------------------- |
| `10001`  | `customer:detail:200001` | `tenant:10001:customer:detail:200001` |
| `10002`  | `customer:detail:200001` | `tenant:10002:customer:detail:200001` |

不要这样写：

```java
String key = "customer:detail:" + customerId;
```

推荐这样写：

```java
String key = TenantRedisKey.customerDetail(customerId);
```

这样可以把租户隔离规则收敛到一个工具类里。

### TenantRedisKey 工具类

文件位置：`src/main/java/io/github/atengk/common/util/TenantRedisKey.java`

该工具类用于统一生成带租户前缀的 Redis Key。

```java
package io.github.atengk.common.util;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.context.TenantContext;

/**
 * 租户 Redis Key 工具类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public final class TenantRedisKey {

    private static final String PREFIX = "tenant:{}:{}";

    private TenantRedisKey() {
    }

    public static String of(String business, Object key) {
        return StrUtil.format(PREFIX, TenantContext.requireTenantId(), business) + ":" + key;
    }

    public static String of(Long tenantId, String business, Object key) {
        return StrUtil.format(PREFIX, tenantId, business) + ":" + key;
    }

    public static String customerDetail(Long customerId) {
        return of("customer:detail", customerId);
    }

    public static String customerList(String keyword) {
        return of("customer:list", StrUtil.blankToDefault(keyword, "all"));
    }

    public static String tenantInfo(Long tenantId) {
        return StrUtil.format("tenant:{}:info", tenantId);
    }
}
```

这里提供了两个通用方法：

| 方法                                             | 使用场景                                 |
| ------------------------------------------------ | ---------------------------------------- |
| `of(String business, Object key)`                | 使用当前上下文租户 ID 生成业务 Key       |
| `of(Long tenantId, String business, Object key)` | 定时任务、批处理等显式指定租户 ID 的场景 |

`tenantInfo(Long tenantId)` 没有依赖 `TenantContext`，因为租户基础信息通常属于平台级缓存，可以直接按租户 ID 缓存。

### 业务缓存读写示例

下面以客户详情缓存为例，演示如何在业务代码中读写租户隔离的 Redis Key。

文件位置：`src/main/java/io/github/atengk/module/customer/service/impl/CrmCustomerServiceImpl.java`

该服务实现类通过 `TenantRedisKey` 生成租户级缓存 Key，避免不同租户读取到彼此的客户缓存。

```java
package io.github.atengk.module.customer.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.common.exception.BizException;
import io.github.atengk.common.util.TenantRedisKey;
import io.github.atengk.module.customer.dto.CustomerCreateDTO;
import io.github.atengk.module.customer.entity.CrmCustomer;
import io.github.atengk.module.customer.mapper.CrmCustomerMapper;
import io.github.atengk.module.customer.service.CrmCustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 客户服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrmCustomerServiceImpl extends ServiceImpl<CrmCustomerMapper, CrmCustomer>
        implements CrmCustomerService {

    private final StringRedisTemplate stringRedisTemplate;

    public CrmCustomer createCustomer(CustomerCreateDTO dto) {
        CrmCustomer customer = BeanUtil.copyProperties(dto, CrmCustomer.class);
        boolean saved = this.save(customer);
        if (!saved) {
            throw new BizException("客户创建失败");
        }

        String key = TenantRedisKey.customerDetail(customer.getId());
        stringRedisTemplate.opsForValue().set(key, customer.getCustomerName(), Duration.ofMinutes(10));
        log.info("客户创建成功，customerId={}，cacheKey={}", customer.getId(), key);
        return customer;
    }

    public String getCustomerNameFromCache(Long customerId) {
        String key = TenantRedisKey.customerDetail(customerId);
        String customerName = stringRedisTemplate.opsForValue().get(key);
        if (ObjectUtil.isNotNull(customerName)) {
            log.debug("命中客户缓存，customerId={}，cacheKey={}", customerId, key);
            return customerName;
        }

        CrmCustomer customer = this.getById(customerId);
        if (ObjectUtil.isNull(customer)) {
            throw new BizException("客户不存在");
        }

        stringRedisTemplate.opsForValue().set(key, customer.getCustomerName(), Duration.ofMinutes(10));
        log.info("客户缓存重建完成，customerId={}，cacheKey={}", customerId, key);
        return customer.getCustomerName();
    }

    public void evictCustomerCache(Long customerId) {
        String key = TenantRedisKey.customerDetail(customerId);
        stringRedisTemplate.delete(key);
        log.info("客户缓存已删除，customerId={}，cacheKey={}", customerId, key);
    }
}
```

注意这里的 `this.getById(customerId)` 仍然会经过 MyBatis-Plus 多租户插件处理，实际查询会自动追加当前租户条件。

逻辑效果如下：

```sql
SELECT *
FROM crm_customer
WHERE id = 200001
  AND tenant_id = 10001;
```

接口验证时，可以分别用两个租户访问相同客户 ID：

```bash
# 租户 10001 读取客户缓存
curl -X GET "http://localhost:8080/customers/200001/cache-name" \
  -H "Authorization: Bearer your-token" \
  -H "X-Tenant-Id: 10001"

# 租户 10002 读取客户缓存
curl -X GET "http://localhost:8080/customers/200001/cache-name" \
  -H "Authorization: Bearer your-token" \
  -H "X-Tenant-Id: 10002"
```

最终 Redis 中会形成两个不同 Key：

```text
tenant:10001:customer:detail:200001
tenant:10002:customer:detail:200001
```

这样即使不同租户存在相同业务 ID，也不会发生缓存串读。

## 文件路径租户隔离

文件路径隔离用于保证不同租户上传的文件不会混在同一个对象存储目录下。原始场景中要求实现「文件路径隔离」和「用户进入租户空间后按租户访问资源」，本节使用 MinIO 演示最常见的租户级文件目录方案。

### MinIO 存储路径规范

本案例约定所有租户文件都使用如下路径格式：

```text
tenant/{tenantId}/{bizType}/{yyyyMMdd}/{fileId}.{ext}
```

示例路径：

```text
tenant/10001/customer/20260515/1900123456789017600.png
tenant/10001/contract/20260515/1900123456789017601.pdf
tenant/10002/customer/20260515/1900123456789017602.png
```

路径字段说明如下：

| 片段             | 说明                                            |
| ---------------- | ----------------------------------------------- |
| `tenant`         | 固定目录前缀                                    |
| `{tenantId}`     | 当前租户 ID                                     |
| `{bizType}`      | 业务类型，例如 `customer`、`contract`、`avatar` |
| `{yyyyMMdd}`     | 上传日期                                        |
| `{fileId}.{ext}` | 文件唯一名，避免原始文件名冲突                  |

不要直接使用原始文件名作为对象路径：

```text
upload/合同.pdf
```

推荐使用租户隔离后的路径：

```text
tenant/10001/contract/20260515/1900123456789017601.pdf
```

这样即使两个租户上传同名文件，也不会相互覆盖。

### 上传文件自动追加租户目录

先封装文件路径工具类，所有上传入口都必须通过该工具生成对象存储路径。

文件位置：`src/main/java/io/github/atengk/common/util/TenantFilePath.java`

该工具类用于生成租户隔离的 MinIO 对象路径，并校验下载路径是否属于当前租户。

```java
package io.github.atengk.common.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.context.TenantContext;
import io.github.atengk.common.exception.BizException;

import java.util.Date;

/**
 * 租户文件路径工具类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public final class TenantFilePath {

    private static final String TENANT_PATH_PREFIX = "tenant/{}/{}";

    private TenantFilePath() {
    }

    /**
     * 生成当前租户文件路径
     *
     * @param bizType 业务类型
     * @param originalFilename 原始文件名
     * @return 对象存储路径
     */
    public static String build(String bizType, String originalFilename) {
        if (StrUtil.isBlank(bizType)) {
            throw new BizException("业务类型不能为空");
        }

        Long tenantId = TenantContext.requireTenantId();
        String extName = FileNameUtil.extName(originalFilename);
        String datePath = DateUtil.format(new Date(), "yyyyMMdd");
        String fileName = StrUtil.isBlank(extName)
                ? IdUtil.getSnowflakeNextIdStr()
                : IdUtil.getSnowflakeNextIdStr() + "." + extName;

        return StrUtil.format(TENANT_PATH_PREFIX, tenantId, bizType)
                + "/" + datePath
                + "/" + fileName;
    }

    /**
     * 校验对象路径是否属于当前租户
     *
     * @param objectName 对象路径
     */
    public static void checkCurrentTenantPath(String objectName) {
        Long tenantId = TenantContext.requireTenantId();
        String currentTenantPrefix = StrUtil.format("tenant/{}/", tenantId);
        if (!StrUtil.startWith(objectName, currentTenantPrefix)) {
            throw new BizException("无权访问其他租户文件");
        }
    }
}
```

MinIO 配置示例：

文件位置：`src/main/resources/application.yml`

```yaml
minio:
  # MinIO 服务地址
  endpoint: http://127.0.0.1:9000
  # 访问账号
  access-key: minioadmin
  # 访问密钥
  secret-key: minioadmin
  # 存储桶名称
  bucket-name: tenant-demo
```

MinIO 客户端配置类如下。

文件位置：`src/main/java/io/github/atengk/config/MinioConfig.java`

该配置类用于初始化 MinIO 客户端，并读取对象存储连接参数。

```java
package io.github.atengk.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {

    private String endpoint;

    private String accessKey;

    private String secretKey;

    private String bucketName;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
```

封装文件上传服务。

文件位置：`src/main/java/io/github/atengk/module/file/service/TenantFileService.java`

该接口提供租户文件上传和下载能力。

```java
package io.github.atengk.module.file.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 租户文件服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface TenantFileService {

    /**
     * 上传租户文件
     *
     * @param bizType 业务类型
     * @param file 上传文件
     * @return 对象路径
     */
    String upload(String bizType, MultipartFile file);

    /**
     * 下载租户文件
     *
     * @param objectName 对象路径
     * @return 文件字节
     */
    byte[] download(String objectName);
}
```

文件位置：`src/main/java/io/github/atengk/module/file/service/impl/TenantFileServiceImpl.java`

该实现类在上传时自动追加租户目录，在下载时校验对象路径是否属于当前租户。

```java
package io.github.atengk.module.file.service.impl;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.exception.BizException;
import io.github.atengk.common.util.TenantFilePath;
import io.github.atengk.config.MinioConfig;
import io.github.atengk.module.file.service.TenantFileService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 租户文件服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantFileServiceImpl implements TenantFileService {

    private final MinioClient minioClient;

    private final MinioConfig minioConfig;

    @Override
    public String upload(String bizType, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件不能为空");
        }

        String objectName = TenantFilePath.build(bizType, file.getOriginalFilename());

        try {
            ensureBucketExists();

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .contentType(StrUtil.blankToDefault(file.getContentType(), "application/octet-stream"))
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .build());

            log.info("租户文件上传成功，objectName={}，size={}", objectName, file.getSize());
            return objectName;
        } catch (Exception ex) {
            log.error("租户文件上传失败，objectName={}", objectName, ex);
            throw new BizException("文件上传失败");
        }
    }

    @Override
    public byte[] download(String objectName) {
        if (StrUtil.isBlank(objectName)) {
            throw new BizException("文件路径不能为空");
        }

        TenantFilePath.checkCurrentTenantPath(objectName);

        try {
            return IoUtil.readBytes(minioClient.getObject(GetObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .build()));
        } catch (Exception ex) {
            log.error("租户文件下载失败，objectName={}", objectName, ex);
            throw new BizException("文件下载失败");
        }
    }

    /**
     * 确保存储桶存在
     */
    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(minioConfig.getBucketName())
                .build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .build());
            log.info("MinIO 存储桶创建成功，bucket={}", minioConfig.getBucketName());
        }
    }
}
```

上传接口如下。

文件位置：`src/main/java/io/github/atengk/module/file/controller/TenantFileController.java`

该控制器提供租户文件上传和下载接口，接口本身不手动拼接租户路径，统一交给 `TenantFileService` 处理。

```java
package io.github.atengk.module.file.controller;

import io.github.atengk.common.result.R;
import io.github.atengk.module.file.service.TenantFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * 租户文件控制器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class TenantFileController {

    private final TenantFileService tenantFileService;

    @PostMapping("/upload")
    public R<String> upload(@RequestParam("bizType") String bizType,
                            @RequestParam("file") MultipartFile file) {
        return R.ok(tenantFileService.upload(bizType, file));
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam("objectName") String objectName) {
        byte[] bytes = tenantFileService.download(objectName);
        String filename = objectName.substring(objectName.lastIndexOf("/") + 1);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(bytes);
    }
}
```

上传验证命令：

```bash
curl -X POST "http://localhost:8080/files/upload" \
  -H "Authorization: Bearer your-token" \
  -H "X-Tenant-Id: 10001" \
  -F "bizType=customer" \
  -F "file=@/tmp/demo.png"
```

返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": "tenant/10001/customer/20260515/1900123456789017600.png"
}
```

### 下载文件租户权限校验

下载时必须校验对象路径是否属于当前租户。不能让租户 `10002` 通过传入完整对象路径下载租户 `10001` 的文件。

校验逻辑已经封装在：

```java
TenantFilePath.checkCurrentTenantPath(objectName);
```

访问示例：

```bash
# 正常：租户 10001 下载自己的文件
curl -X GET "http://localhost:8080/files/download?objectName=tenant/10001/customer/20260515/1900123456789017600.png" \
  -H "Authorization: Bearer your-token" \
  -H "X-Tenant-Id: 10001" \
  --output demo.png

# 异常：租户 10002 尝试下载租户 10001 的文件
curl -X GET "http://localhost:8080/files/download?objectName=tenant/10001/customer/20260515/1900123456789017600.png" \
  -H "Authorization: Bearer your-token" \
  -H "X-Tenant-Id: 10002"
```

异常响应示例：

```json
{
  "code": 500,
  "message": "无权访问其他租户文件",
  "data": null
}
```

如果项目对文件权限要求更高，建议增加一张文件元数据表，例如 `sys_file`，记录 `tenant_id`、`object_name`、`biz_type`、`created_by`。下载时先查数据库确认归属，再读取 MinIO。

## 定时任务按租户执行

定时任务按租户执行的核心是：任务不能在无租户上下文中直接扫描所有业务数据，而是应该查询有效租户列表，然后逐个设置 `TenantContext` 执行业务逻辑。

### 查询有效租户列表

先在租户 Mapper 中增加查询有效租户 ID 的方法。

文件位置：`src/main/java/io/github/atengk/module/tenant/mapper/SysTenantMapper.java`

该方法用于给定时任务查询当前可用租户。

```java
package io.github.atengk.module.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.module.tenant.entity.SysTenant;
import io.github.atengk.module.tenant.vo.TenantVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 租户 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface SysTenantMapper extends BaseMapper<SysTenant> {

    /**
     * 查询用户可访问租户
     *
     * @param userId 用户ID
     * @return 租户列表
     */
    List<TenantVO> selectAvailableTenantsByUserId(@Param("userId") Long userId);

    /**
     * 查询有效租户ID列表
     *
     * @return 租户ID列表
     */
    List<Long> selectValidTenantIds();
}
```

文件位置：`src/main/resources/mapper/tenant/SysTenantMapper.xml`

在原有 XML 中追加 `selectValidTenantIds`。

```xml
<!-- 查询有效租户ID列表，用于定时任务按租户执行 -->
<select id="selectValidTenantIds" resultType="java.lang.Long">
    SELECT id
    FROM sys_tenant
    WHERE status = 1
      AND deleted = 0
      AND (expire_time IS NULL OR expire_time >= NOW())
    ORDER BY id ASC
</select>
```

在 `SysTenantService` 中增加方法。

文件位置：`src/main/java/io/github/atengk/module/tenant/service/SysTenantService.java`

```java
List<Long> listValidTenantIds();
```

文件位置：`src/main/java/io/github/atengk/module/tenant/service/impl/SysTenantServiceImpl.java`

```java
@Override
public List<Long> listValidTenantIds() {
    return baseMapper.selectValidTenantIds();
}
```

### XXL-JOB 遍历租户执行任务

下面以「统计每个租户客户数量」为例。真实项目中可以替换为订单超时关闭、套餐额度刷新、报表汇总、消息补偿等逻辑。

文件位置：`src/main/java/io/github/atengk/job/TenantCustomerStatJob.java`

该任务会查询有效租户列表，然后逐个注入租户上下文执行统计。`crmCustomerService.count()` 会被 MyBatis-Plus 多租户插件自动追加当前租户条件。

```java
package io.github.atengk.job;

import cn.hutool.core.collection.CollUtil;
import com.xxl.job.core.handler.annotation.XxlJob;
import io.github.atengk.common.context.TenantContext;
import io.github.atengk.module.customer.service.CrmCustomerService;
import io.github.atengk.module.tenant.service.SysTenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 租户客户统计任务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantCustomerStatJob {

    private final SysTenantService sysTenantService;

    private final CrmCustomerService crmCustomerService;

    @XxlJob("tenantCustomerStatJob")
    public void tenantCustomerStatJob() {
        List<Long> tenantIds = sysTenantService.listValidTenantIds();
        if (CollUtil.isEmpty(tenantIds)) {
            log.info("没有需要执行任务的有效租户");
            return;
        }

        log.info("开始执行租户客户统计任务，tenantCount={}", tenantIds.size());

        for (Long tenantId : tenantIds) {
            try {
                TenantContext.setTenantId(tenantId);

                long customerCount = crmCustomerService.count();
                log.info("租户客户统计完成，tenantId={}，customerCount={}", tenantId, customerCount);
            } catch (Exception ex) {
                log.error("租户客户统计失败，tenantId={}", tenantId, ex);
            } finally {
                TenantContext.clear();
            }
        }

        log.info("租户客户统计任务执行完成");
    }
}
```

任务执行时，逻辑效果如下：

```text
查询有效租户：10001、10002
-> 设置 TenantContext = 10001
-> 执行 SELECT COUNT(*) FROM crm_customer WHERE tenant_id = 10001
-> 清理 TenantContext
-> 设置 TenantContext = 10002
-> 执行 SELECT COUNT(*) FROM crm_customer WHERE tenant_id = 10002
-> 清理 TenantContext
```

不要这样写：

```java
long count = crmCustomerService.count();
```

如果任务中没有设置 `TenantContext`，MyBatis-Plus 多租户插件会因为缺少租户上下文而抛出异常；如果为了规避异常而绕开插件，又容易变成全租户数据扫描。

### 任务执行时注入租户上下文

定时任务、MQ 消费、异步线程都应该使用同一种上下文注入模板：

```java
try {
    TenantContext.setTenantId(tenantId);
    // 执行业务逻辑
} finally {
    TenantContext.clear();
}
```

可以进一步封装一个执行器，减少重复代码。

文件位置：`src/main/java/io/github/atengk/common/context/TenantExecutor.java`

该执行器用于在指定租户上下文中执行业务逻辑，并保证执行结束后清理上下文。

```java
package io.github.atengk.common.context;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * 租户上下文执行器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public final class TenantExecutor {

    private TenantExecutor() {
    }

    /**
     * 在指定租户下执行无返回值任务
     *
     * @param tenantId 租户ID
     * @param runnable 任务逻辑
     */
    public static void run(Long tenantId, Runnable runnable) {
        try {
            TenantContext.setTenantId(tenantId);
            runnable.run();
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 在指定租户下执行有返回值任务
     *
     * @param tenantId 租户ID
     * @param supplier 任务逻辑
     * @return 执行结果
     * @param <T> 返回类型
     */
    public static <T> T call(Long tenantId, Supplier<T> supplier) {
        try {
            TenantContext.setTenantId(tenantId);
            return supplier.get();
        } finally {
            TenantContext.clear();
        }
    }
}
```

任务代码可以改成：

```java
for (Long tenantId : tenantIds) {
    try {
        TenantExecutor.run(tenantId, () -> {
            long customerCount = crmCustomerService.count();
            log.info("租户客户统计完成，tenantId={}，customerCount={}", tenantId, customerCount);
        });
    } catch (Exception ex) {
        log.error("租户客户统计失败，tenantId={}", tenantId, ex);
    }
}
```

这种写法可以统一约束上下文清理，减少人为遗漏。

## 套餐限制与租户停用控制

套餐限制和租户停用控制用于防止租户在停用、到期、超过额度后继续使用系统资源。本案例只实现核心控制点：租户状态校验、客户数量额度校验、到期租户拦截。

### 租户状态校验

租户状态校验已经在 `SysTenantServiceImpl.checkTenantAvailable()` 中实现，拦截器会在每次业务请求进入时调用。

核心逻辑如下：

```java
@Override
public void checkTenantAvailable(Long tenantId) {
    SysTenant tenant = this.getById(tenantId);
    if (ObjectUtil.isNull(tenant) || ObjectUtil.equal(tenant.getDeleted(), 1)) {
        throw new BizException("租户不存在");
    }
    if (!ObjectUtil.equal(tenant.getStatus(), TENANT_STATUS_NORMAL)) {
        throw new BizException("租户已停用");
    }
    if (ObjectUtil.isNotNull(tenant.getExpireTime())
            && LocalDateTimeUtil.compare(tenant.getExpireTime(), LocalDateTime.now()) < 0) {
        throw new BizException("租户已到期");
    }
}
```

拦截位置：

```text
TenantInterceptor.preHandle()
-> sysTenantService.checkTenantAvailable(tenantId)
-> 校验通过后 TenantContext.setTenantId(tenantId)
```

这样可以保证停用租户、过期租户无法继续访问普通业务接口。

### 套餐额度校验

套餐额度通常包括用户数量、客户数量、文件容量、接口调用次数等。本案例以「客户数量上限」为例，演示创建客户前的额度校验。

新增套餐额度表：

```sql
-- 租户套餐额度表：保存每个租户的资源上限
CREATE TABLE tenant_package_quota (
    id BIGINT NOT NULL COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    max_customer_count INT NOT NULL DEFAULT 100 COMMENT '最大客户数量',
    max_file_size_mb INT NOT NULL DEFAULT 1024 COMMENT '最大文件容量MB',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户套餐额度表';

INSERT INTO tenant_package_quota (
    id, tenant_id, max_customer_count, max_file_size_mb
) VALUES
    (300001, 10001, 100, 1024),
    (300002, 10002, 2, 1024);
```

该表属于租户业务配置表，可以选择让 MyBatis-Plus 自动拼接 `tenant_id`，也可以配置为系统表后显式按 `tenant_id` 查询。为了更直观，本案例保留 `tenant_id` 隔离。

实体类如下。

文件位置：`src/main/java/io/github/atengk/module/tenant/entity/TenantPackageQuota.java`

```java
package io.github.atengk.module.tenant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户套餐额度实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("tenant_package_quota")
public class TenantPackageQuota {

    private Long id;

    private Long tenantId;

    private Integer maxCustomerCount;

    private Integer maxFileSizeMb;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
```

Mapper 如下。

文件位置：`src/main/java/io/github/atengk/module/tenant/mapper/TenantPackageQuotaMapper.java`

```java
package io.github.atengk.module.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.module.tenant.entity.TenantPackageQuota;

/**
 * 租户套餐额度 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface TenantPackageQuotaMapper extends BaseMapper<TenantPackageQuota> {
}
```

套餐服务接口如下。

文件位置：`src/main/java/io/github/atengk/module/tenant/service/TenantPackageQuotaService.java`

```java
package io.github.atengk.module.tenant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.atengk.module.tenant.entity.TenantPackageQuota;

/**
 * 租户套餐额度服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface TenantPackageQuotaService extends IService<TenantPackageQuota> {

    /**
     * 校验客户数量额度
     */
    void checkCustomerQuota();
}
```

服务实现如下。

文件位置：`src/main/java/io/github/atengk/module/tenant/service/impl/TenantPackageQuotaServiceImpl.java`

该实现类会读取当前租户套餐额度，并统计当前租户已有客户数量，超过额度时拒绝新增客户。

```java
package io.github.atengk.module.tenant.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.common.context.TenantContext;
import io.github.atengk.common.exception.BizException;
import io.github.atengk.module.customer.service.CrmCustomerService;
import io.github.atengk.module.tenant.entity.TenantPackageQuota;
import io.github.atengk.module.tenant.mapper.TenantPackageQuotaMapper;
import io.github.atengk.module.tenant.service.TenantPackageQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 租户套餐额度服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantPackageQuotaServiceImpl extends ServiceImpl<TenantPackageQuotaMapper, TenantPackageQuota>
        implements TenantPackageQuotaService {

    private final CrmCustomerService crmCustomerService;

    @Override
    public void checkCustomerQuota() {
        Long tenantId = TenantContext.requireTenantId();

        TenantPackageQuota quota = this.lambdaQuery()
                .eq(TenantPackageQuota::getTenantId, tenantId)
                .one();

        if (ObjectUtil.isNull(quota)) {
            throw new BizException("租户套餐额度未配置");
        }

        long currentCustomerCount = crmCustomerService.count();
        if (currentCustomerCount >= quota.getMaxCustomerCount()) {
            log.warn("租户客户数量超过套餐限制，tenantId={}，current={}，limit={}",
                    tenantId, currentCustomerCount, quota.getMaxCustomerCount());
            throw new BizException("客户数量已达到套餐上限");
        }
    }
}
```

在创建客户前调用额度校验：

文件位置：`src/main/java/io/github/atengk/module/customer/service/impl/CrmCustomerServiceImpl.java`

```java
private final TenantPackageQuotaService tenantPackageQuotaService;

public CrmCustomer createCustomer(CustomerCreateDTO dto) {
    tenantPackageQuotaService.checkCustomerQuota();

    CrmCustomer customer = BeanUtil.copyProperties(dto, CrmCustomer.class);
    boolean saved = this.save(customer);
    if (!saved) {
        throw new BizException("客户创建失败");
    }

    log.info("客户创建成功，customerId={}", customer.getId());
    return customer;
}
```

验证方式：

```bash
# 租户 10002 的 max_customer_count 初始化为 2
# 如果已有客户数量也是 2，再新增客户会失败
curl -X POST "http://localhost:8080/customers" \
  -H "Authorization: Bearer your-token" \
  -H "X-Tenant-Id: 10002" \
  -H "Content-Type: application/json" \
  -d '{"customerName":"超过额度客户","customerPhone":"13999999999","customerLevel":1}'
```

预期响应：

```json
{
  "code": 500,
  "message": "客户数量已达到套餐上限",
  "data": null
}
```

### 到期租户拦截处理

到期租户拦截建议放在统一入口，也就是 `TenantInterceptor` 中完成。这样所有普通业务接口都会自动生效。

当前拦截链路如下：

```text
请求进入
-> 解析 X-Tenant-Id
-> 校验租户存在
-> 校验租户状态
-> 校验租户到期时间
-> 校验用户是否属于租户
-> 注入 TenantContext
-> 执行业务接口
```

如果需要保留部分到期后仍可访问的接口，例如续费、账单、联系客服，可以在 `TenantWebConfig` 中放行这些路径，或者在拦截器中为特定路径跳过到期校验。

示例配置：

```java
registry.addInterceptor(tenantInterceptor)
        .addPathPatterns("/**")
        .excludePathPatterns(
                "/auth/**",
                "/tenant/list",
                "/tenant/renew/**",
                "/billing/**",
                "/actuator/**",
                "/error"
        );
```

更细粒度的做法是增加注解，例如 `@IgnoreTenantExpireCheck`，用于标记到期后仍允许访问的接口。但本案例为了保持核心实现简洁，不额外展开注解方案。

到期租户验证 SQL：

```sql
-- 将租户 10001 设置为已到期
UPDATE sys_tenant
SET expire_time = DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE id = 10001;
```

请求验证：

```bash
curl -X GET "http://localhost:8080/customers" \
  -H "Authorization: Bearer your-token" \
  -H "X-Tenant-Id: 10001"
```

预期响应：

```json
{
  "code": 500,
  "message": "租户已到期",
  "data": null
}
```

恢复租户：

```sql
-- 恢复为不过期
UPDATE sys_tenant
SET expire_time = NULL
WHERE id = 10001;
```

这一层控制解决的是「租户是否还能使用系统」的问题；套餐额度解决的是「租户可以使用多少资源」的问题。两者都应该放在公共服务或公共拦截逻辑中，避免散落在各个业务接口里。

## 接口实战案例

本节用「客户管理」作为多租户业务隔离的实战接口。客户表 `crm_customer` 是典型租户业务表，请求进入系统后由拦截器注入租户上下文，查询和写入由 MyBatis-Plus 自动处理 `tenant_id`，这对应原始场景中的「查询自动隔离租户数据」和「SQL 自动拼接 tenant_id」能力。

### 创建客户数据

创建客户时，Controller 和 Service 不需要手动接收 `tenantId`。当前租户来自请求头 `X-Tenant-Id`，由 `TenantInterceptor` 写入 `TenantContext`，最终由 `MyMetaObjectHandler` 自动填充到 `crm_customer.tenant_id` 字段。

请求 DTO 如下。

文件位置：`src/main/java/io/github/atengk/module/customer/dto/CustomerCreateDTO.java`

```java
package io.github.atengk.module.customer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建客户请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class CustomerCreateDTO {

    @NotBlank(message = "客户名称不能为空")
    private String customerName;

    private String customerPhone;

    @Min(value = 1, message = "客户等级不合法")
    @Max(value = 3, message = "客户等级不合法")
    private Integer customerLevel;

    private String remark;
}
```

客户返回对象如下。

文件位置：`src/main/java/io/github/atengk/module/customer/vo/CustomerVO.java`

```java
package io.github.atengk.module.customer.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户视图对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class CustomerVO {

    private Long id;

    private Long tenantId;

    private String customerName;

    private String customerPhone;

    private Integer customerLevel;

    private String remark;

    private LocalDateTime createdAt;
}
```

Mapper 使用 MyBatis-Plus 基础 Mapper 即可。

文件位置：`src/main/java/io/github/atengk/module/customer/mapper/CrmCustomerMapper.java`

```java
package io.github.atengk.module.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.module.customer.entity.CrmCustomer;

/**
 * 客户 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface CrmCustomerMapper extends BaseMapper<CrmCustomer> {
}
```

客户 Service 接口如下。

文件位置：`src/main/java/io/github/atengk/module/customer/service/CrmCustomerService.java`

```java
package io.github.atengk.module.customer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.atengk.module.customer.dto.CustomerCreateDTO;
import io.github.atengk.module.customer.entity.CrmCustomer;
import io.github.atengk.module.customer.vo.CustomerVO;

import java.util.List;

/**
 * 客户服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface CrmCustomerService extends IService<CrmCustomer> {

    CrmCustomer createCustomer(CustomerCreateDTO dto);

    List<CustomerVO> listCustomers(String keyword);

    CustomerVO getCustomerDetail(Long customerId);
}
```

客户 Service 实现如下，创建前会校验套餐额度，创建后会写入租户级 Redis 缓存。

文件位置：`src/main/java/io/github/atengk/module/customer/service/impl/CrmCustomerServiceImpl.java`

```java
package io.github.atengk.module.customer.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.common.exception.BizException;
import io.github.atengk.common.util.TenantRedisKey;
import io.github.atengk.module.customer.dto.CustomerCreateDTO;
import io.github.atengk.module.customer.entity.CrmCustomer;
import io.github.atengk.module.customer.mapper.CrmCustomerMapper;
import io.github.atengk.module.customer.service.CrmCustomerService;
import io.github.atengk.module.customer.vo.CustomerVO;
import io.github.atengk.module.tenant.service.TenantPackageQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 客户服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrmCustomerServiceImpl extends ServiceImpl<CrmCustomerMapper, CrmCustomer>
        implements CrmCustomerService {

    private final StringRedisTemplate stringRedisTemplate;

    private final TenantPackageQuotaService tenantPackageQuotaService;

    @Override
    public CrmCustomer createCustomer(CustomerCreateDTO dto) {
        tenantPackageQuotaService.checkCustomerQuota();

        CrmCustomer customer = BeanUtil.copyProperties(dto, CrmCustomer.class);
        if (customer.getCustomerLevel() == null) {
            customer.setCustomerLevel(1);
        }

        boolean saved = this.save(customer);
        if (!saved) {
            throw new BizException("客户创建失败");
        }

        String key = TenantRedisKey.customerDetail(customer.getId());
        stringRedisTemplate.opsForValue().set(key, customer.getCustomerName(), Duration.ofMinutes(10));

        log.info("客户创建成功，customerId={}，cacheKey={}", customer.getId(), key);
        return customer;
    }

    @Override
    public List<CustomerVO> listCustomers(String keyword) {
        List<CrmCustomer> customers = this.lambdaQuery()
                .like(StrUtil.isNotBlank(keyword), CrmCustomer::getCustomerName, keyword)
                .orderByDesc(CrmCustomer::getCreatedAt)
                .list();

        return BeanUtil.copyToList(customers, CustomerVO.class);
    }

    @Override
    public CustomerVO getCustomerDetail(Long customerId) {
        CrmCustomer customer = this.getById(customerId);
        if (customer == null) {
            throw new BizException("客户不存在");
        }
        return BeanUtil.copyProperties(customer, CustomerVO.class);
    }
}
```

客户 Controller 如下。

文件位置：`src/main/java/io/github/atengk/module/customer/controller/CustomerController.java`

```java
package io.github.atengk.module.customer.controller;

import io.github.atengk.common.result.R;
import io.github.atengk.module.customer.dto.CustomerCreateDTO;
import io.github.atengk.module.customer.entity.CrmCustomer;
import io.github.atengk.module.customer.service.CrmCustomerService;
import io.github.atengk.module.customer.vo.CustomerVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客户控制器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CrmCustomerService crmCustomerService;

    @PostMapping
    public R<CrmCustomer> createCustomer(@Valid @RequestBody CustomerCreateDTO dto) {
        return R.ok(crmCustomerService.createCustomer(dto));
    }

    @GetMapping
    public R<List<CustomerVO>> listCustomers(@RequestParam(required = false) String keyword) {
        return R.ok(crmCustomerService.listCustomers(keyword));
    }

    @GetMapping("/{customerId}")
    public R<CustomerVO> getCustomerDetail(@PathVariable Long customerId) {
        return R.ok(crmCustomerService.getCustomerDetail(customerId));
    }
}
```

创建客户请求示例：

```bash
curl -X POST "http://localhost:8080/customers" \
  -H "Authorization: Bearer your-token" \
  -H "X-Tenant-Id: 10001" \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "北京新客户",
    "customerPhone": "13910000001",
    "customerLevel": 2,
    "remark": "租户10001创建的客户"
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1900123456789017600,
    "tenantId": 10001,
    "customerName": "北京新客户",
    "customerPhone": "13910000001",
    "customerLevel": 2,
    "remark": "租户10001创建的客户"
  }
}
```

实际 SQL 效果是插入时自动写入 `tenant_id = 10001`：

```sql
INSERT INTO crm_customer (
    id,
    tenant_id,
    customer_name,
    customer_phone,
    customer_level,
    remark,
    created_by,
    updated_by,
    created_at,
    updated_at
) VALUES (
    1900123456789017600,
    10001,
    '北京新客户',
    '13910000001',
    2,
    '租户10001创建的客户',
    1,
    1,
    NOW(),
    NOW()
);
```

### 查询客户列表

查询客户列表时，业务代码只写普通查询条件，不手动拼接 `tenant_id`。MyBatis-Plus 多租户插件会根据 `TenantContext` 自动追加当前租户条件。

请求示例：

```bash
curl -X GET "http://localhost:8080/customers?keyword=客户" \
  -H "Authorization: Bearer your-token" \
  -H "X-Tenant-Id: 10001"
```

业务代码中的查询逻辑：

```java
List<CrmCustomer> customers = this.lambdaQuery()
        .like(StrUtil.isNotBlank(keyword), CrmCustomer::getCustomerName, keyword)
        .orderByDesc(CrmCustomer::getCreatedAt)
        .list();
```

SQL 逻辑效果：

```sql
SELECT
    id,
    tenant_id,
    customer_name,
    customer_phone,
    customer_level,
    remark,
    created_at
FROM crm_customer
WHERE tenant_id = 10001
  AND customer_name LIKE '%客户%'
ORDER BY created_at DESC;
```

如果换成租户 `10002` 请求：

```bash
curl -X GET "http://localhost:8080/customers?keyword=客户" \
  -H "Authorization: Bearer your-token" \
  -H "X-Tenant-Id: 10002"
```

SQL 逻辑效果会自动变成：

```sql
SELECT
    id,
    tenant_id,
    customer_name,
    customer_phone,
    customer_level,
    remark,
    created_at
FROM crm_customer
WHERE tenant_id = 10002
  AND customer_name LIKE '%客户%'
ORDER BY created_at DESC;
```

这就是多租户隔离最核心的效果：同一套接口、同一套业务代码，根据当前租户上下文自动查询不同租户的数据。

### 切换租户后查询验证

先切换到租户 `10001`：

```bash
curl -X POST "http://localhost:8080/tenant/switch" \
  -H "Authorization: Bearer your-token" \
  -H "Content-Type: application/json" \
  -d '{"tenantId":10001}'
```

查询客户列表：

```bash
curl -X GET "http://localhost:8080/customers" \
  -H "Authorization: Bearer your-token" \
  -H "X-Tenant-Id: 10001"
```

预期只能看到租户 `10001` 的客户：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "tenantId": 10001,
      "customerName": "北京新客户"
    },
    {
      "tenantId": 10001,
      "customerName": "华北测试客户"
    },
    {
      "tenantId": 10001,
      "customerName": "公共客户A"
    }
  ]
}
```

再切换到租户 `10002`：

```bash
curl -X POST "http://localhost:8080/tenant/switch" \
  -H "Authorization: Bearer your-token" \
  -H "Content-Type: application/json" \
  -d '{"tenantId":10002}'
```

查询客户列表：

```bash
curl -X GET "http://localhost:8080/customers" \
  -H "Authorization: Bearer your-token" \
  -H "X-Tenant-Id: 10002"
```

预期只能看到租户 `10002` 的客户：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "tenantId": 10002,
      "customerName": "华东测试客户"
    },
    {
      "tenantId": 10002,
      "customerName": "公共客户A"
    }
  ]
}
```

可以看到，两个租户都存在 `公共客户A`，但数据归属完全不同。查询结果由当前租户上下文决定，不会跨租户返回。

### 跨租户访问拦截验证

跨租户访问主要验证两个点：

```text
用户不能切换到自己未加入的租户
用户不能通过请求头伪造租户访问其他租户数据
```

先把用户 `2` 只绑定到租户 `10001`：

```sql
DELETE FROM sys_tenant_user WHERE user_id = 2;

INSERT INTO sys_tenant_user (
    id, tenant_id, user_id, role_code, status
) VALUES (
    100101, 10001, 2, 'member', 1
);
```

用户 `2` 尝试切换到租户 `10002`：

```bash
curl -X POST "http://localhost:8080/tenant/switch" \
  -H "Authorization: Bearer user2-token" \
  -H "Content-Type: application/json" \
  -d '{"tenantId":10002}'
```

预期响应：

```json
{
  "code": 500,
  "message": "当前用户无权切换到该租户",
  "data": null
}
```

用户 `2` 直接伪造请求头访问租户 `10002`：

```bash
curl -X GET "http://localhost:8080/customers" \
  -H "Authorization: Bearer user2-token" \
  -H "X-Tenant-Id: 10002"
```

预期响应：

```json
{
  "code": 500,
  "message": "当前用户无权访问该租户",
  "data": null
}
```

这一步由 `TenantInterceptor` 中的用户租户关系校验完成：

```java
boolean joined = sysTenantUserService.existsNormalRelation(tenantId, userId);
if (!joined) {
    log.warn("用户访问未授权租户，userId={}，tenantId={}", userId, tenantId);
    throw new BizException("当前用户无权访问该租户");
}
```

## 测试与验证

本节给出完整验证路径，重点验证数据库隔离、缓存隔离、文件隔离和停用租户拦截。

### 接口测试流程

推荐按下面顺序测试：

```text
初始化数据库
-> 启动 MySQL、Redis、MinIO
-> 启动 Spring Boot 应用
-> 登录并获取 Token
-> 查询可访问租户
-> 切换租户
-> 创建客户
-> 查询客户列表
-> 验证数据库 tenant_id
-> 验证 Redis Key
-> 上传文件
-> 下载文件
-> 验证跨租户访问拦截
```

如果本地还没有真实登录接口，可以临时在开发环境用固定用户模拟登录，但正式环境必须使用真实 Sa-Token 登录态。

查询用户可访问租户：

```bash
curl -X GET "http://localhost:8080/tenant/list" \
  -H "Authorization: Bearer your-token"
```

切换到租户 `10001`：

```bash
curl -X POST "http://localhost:8080/tenant/switch" \
  -H "Authorization: Bearer your-token" \
  -H "Content-Type: application/json" \
  -d '{"tenantId":10001}'
```

创建客户：

```bash
curl -X POST "http://localhost:8080/customers" \
  -H "Authorization: Bearer your-token" \
  -H "X-Tenant-Id: 10001" \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "接口测试客户",
    "customerPhone": "13988880001",
    "customerLevel": 1,
    "remark": "接口测试创建"
  }'
```

查询客户：

```bash
curl -X GET "http://localhost:8080/customers?keyword=接口测试" \
  -H "Authorization: Bearer your-token" \
  -H "X-Tenant-Id: 10001"
```

### 数据隔离验证

先查看数据库中的客户分布：

```sql
SELECT tenant_id, COUNT(*) AS customer_count
FROM crm_customer
GROUP BY tenant_id
ORDER BY tenant_id;
```

预期类似：

| tenant_id | customer_count |
| --------- | -------------- |
| 10001     | 3              |
| 10002     | 2              |

验证租户 `10001` 查询结果：

```bash
curl -X GET "http://localhost:8080/customers" \
  -H "Authorization: Bearer your-token" \
  -H "X-Tenant-Id: 10001"
```

结果中不应出现：

```json
{
  "tenantId": 10002
}
```

验证租户 `10002` 查询结果：

```bash
curl -X GET "http://localhost:8080/customers" \
  -H "Authorization: Bearer your-token" \
  -H "X-Tenant-Id: 10002"
```

结果中不应出现：

```json
{
  "tenantId": 10001
}
```

如果希望确认 MyBatis-Plus 是否真正追加了租户条件，可以打开 SQL 日志：

```yaml
mybatis-plus:
  configuration:
    # 开发环境输出 SQL，生产环境不建议开启
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

控制台应该能看到类似 SQL：

```sql
SELECT id, tenant_id, customer_name, customer_phone, customer_level, remark, created_at
FROM crm_customer
WHERE tenant_id = 10001
ORDER BY created_at DESC;
```

### 缓存隔离验证

创建或查询客户后，检查 Redis Key：

```bash
redis-cli keys "tenant:*:customer:*"
```

预期结果：

```text
tenant:10001:customer:detail:200001
tenant:10002:customer:detail:200003
```

分别访问两个租户下的客户详情缓存：

```bash
curl -X GET "http://localhost:8080/customers/200001" \
  -H "Authorization: Bearer your-token" \
  -H "X-Tenant-Id: 10001"

curl -X GET "http://localhost:8080/customers/200001" \
  -H "Authorization: Bearer your-token" \
  -H "X-Tenant-Id: 10002"
```

如果 `200001` 属于租户 `10001`，那么租户 `10002` 查询时应该返回客户不存在。因为 `getById` 会被多租户插件自动追加：

```sql
WHERE id = 200001
  AND tenant_id = 10002
```

缓存隔离验证重点如下：

| 验证项                               | 预期                 |
| ------------------------------------ | -------------------- |
| Redis Key 是否包含租户 ID            | 必须包含             |
| 不同租户相同业务 ID 是否生成不同 Key | 必须不同             |
| 查询客户详情是否受 SQL 租户条件限制  | 必须限制             |
| 删除缓存是否只删除当前租户 Key       | 不能删除其他租户 Key |

### 文件隔离验证

先用租户 `10001` 上传文件：

```bash
curl -X POST "http://localhost:8080/files/upload" \
  -H "Authorization: Bearer your-token" \
  -H "X-Tenant-Id: 10001" \
  -F "bizType=customer" \
  -F "file=@/tmp/demo.png"
```

预期返回：

```json
{
  "code": 200,
  "message": "success",
  "data": "tenant/10001/customer/20260515/1900123456789017600.png"
}
```

租户 `10001` 下载自己的文件：

```bash
curl -X GET "http://localhost:8080/files/download?objectName=tenant/10001/customer/20260515/1900123456789017600.png" \
  -H "Authorization: Bearer your-token" \
  -H "X-Tenant-Id: 10001" \
  --output demo.png
```

租户 `10002` 尝试下载租户 `10001` 的文件：

```bash
curl -X GET "http://localhost:8080/files/download?objectName=tenant/10001/customer/20260515/1900123456789017600.png" \
  -H "Authorization: Bearer your-token" \
  -H "X-Tenant-Id: 10002"
```

预期响应：

```json
{
  "code": 500,
  "message": "无权访问其他租户文件",
  "data": null
}
```

文件隔离验证重点如下：

```text
上传路径必须包含 tenant/{tenantId}
下载时必须校验 objectName 是否属于当前租户
不能仅依赖前端隐藏文件地址
高安全要求场景建议增加文件元数据表做二次鉴权
```

## 本案例总结

本案例完成了一个可直接落地的 Java 后端多租户隔离最小闭环，覆盖接口请求、数据库、缓存、文件、定时任务和套餐控制。

### 核心实现回顾

本案例核心链路如下：

```text
请求进入系统
-> TenantInterceptor 解析 X-Tenant-Id
-> 校验租户状态、到期时间、用户租户关系
-> TenantContext 保存当前租户 ID
-> MyBatis-Plus 自动拼接 tenant_id
-> MetaObjectHandler 新增时自动填充 tenant_id
-> TenantRedisKey 生成租户级缓存 Key
-> TenantFilePath 生成租户级文件路径
-> 请求结束 TenantContext.clear()
```

核心组件职责如下：

| 组件                         | 作用                               |
| ---------------------------- | ---------------------------------- |
| `TenantContext`              | 保存当前租户上下文                 |
| `TenantInterceptor`          | 请求入口统一解析和校验租户         |
| `TenantLineInnerInterceptor` | SQL 自动追加 `tenant_id`           |
| `MyMetaObjectHandler`        | 新增数据自动填充 `tenant_id`       |
| `TenantRedisKey`             | 统一生成租户隔离 Redis Key         |
| `TenantFilePath`             | 统一生成租户隔离文件路径           |
| `TenantExecutor`             | 定时任务、异步任务中注入租户上下文 |
| `SysTenantService`           | 校验租户状态、到期时间、租户切换   |
| `SysTenantUserService`       | 校验用户是否属于当前租户           |
| `TenantPackageQuotaService`  | 校验租户套餐资源额度               |

最关键的工程规则如下：

```text
业务表必须有 tenant_id
业务代码尽量不要手写 tenant_id 条件
新增数据必须自动填充 tenant_id
请求结束必须清理 TenantContext
Redis Key 必须包含 tenantId
文件路径必须包含 tenantId
定时任务必须按租户循环执行
停用、过期租户必须在入口统一拦截
```

这个方案适合大多数中小型 SaaS 项目，尤其适合以下场景：

```text
企业服务平台
多商户后台
CRM / ERP / OA SaaS
多组织业务系统
内部管理平台多租户化
```

### 可扩展方向

当前方案采用「共享数据库 + 共享表 + tenant_id」模式，后续可以根据业务规模继续扩展。

| 扩展方向             | 说明                                                         |
| -------------------- | ------------------------------------------------------------ |
| 网关层租户透传       | 在 Spring Cloud Gateway 中解析域名、请求头或 Token，并向下游服务透传租户 ID |
| 租户独立域名         | 通过 `tenantCode.example.com` 自动识别租户                   |
| 租户套餐体系         | 增加套餐、权益、计费、续费、额度扣减                         |
| 租户级配置中心       | 支持不同租户使用不同业务配置                                 |
| 租户级数据权限       | 在 `tenant_id` 基础上叠加部门、角色、本人数据权限            |
| 租户级审计日志       | 关键操作日志中强制记录 `tenant_id`                           |
| 租户级限流           | Redis 或 Sentinel 按租户限制接口访问频率                     |
| 租户级消息通知       | MQ 消息体中携带 `tenant_id`，消费时注入租户上下文            |
| 租户级报表统计       | 定时按租户生成统计结果，避免跨租户聚合污染                   |
| 独立 Schema / 独立库 | 大客户或强隔离场景可以升级为独立数据库方案                   |

如果系统规模继续扩大，可以按下面路径演进：

```text
共享库共享表 tenant_id 隔离
-> 重点租户拆分独立 Schema
-> 大客户租户拆分独立数据库
-> 网关统一识别租户
-> 链路日志、MQ、任务、缓存全链路携带 tenantId
```

本案例的核心价值不是代码量，而是把多租户系统中最容易出问题的几个点统一收口：入口校验、上下文传递、SQL 隔离、缓存隔离、文件隔离和任务隔离。只要这些底座稳定，后续增加业务模块时就能自然继承租户隔离能力。