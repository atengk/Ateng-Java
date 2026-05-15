# 数据权限与组织隔离

本文档基于 Java 后端经典业务场景中的“数据权限与组织隔离”展开，实现一个适用于 OA、CRM、ERP、后台管理、SaaS、政企系统的数据权限控制方案。核心目标是：用户登录后，系统根据用户角色、组织架构、租户信息自动计算数据可见范围，并在查询业务数据时自动追加权限条件，避免在每个业务接口中重复编写权限过滤逻辑。

## 场景目标

数据权限与组织隔离解决的不是“用户能不能访问接口”，而是“用户访问接口后能看到哪些数据”。

在后台管理系统中，同一个列表接口通常会被不同角色复用。例如客户列表接口，普通员工只能查看自己负责的客户，部门主管可以查看本部门客户，区域负责人可以查看本部门及子部门客户，超级管理员可以查看全部数据。对于 SaaS 系统，还需要在组织权限之外叠加租户隔离，确保不同租户之间的数据互相不可见。

本案例实现的核心链路如下：

```text
用户登录
-> 获取当前用户 ID、租户 ID、部门 ID
-> 查询用户角色
-> 查询角色绑定的数据范围
-> 计算最终可见部门范围
-> 查询业务数据时自动追加权限条件
-> 返回当前用户有权限的数据
```

最终希望业务代码保持简洁：

```java
customerMapper.selectPage(page, queryWrapper);
```

实际执行 SQL 时，系统自动追加租户和组织权限条件，效果类似：

```sql
SELECT *
FROM biz_customer
WHERE tenant_id = 1001
  AND dept_id IN (10, 11, 12)
  AND deleted = 0;
```

### 业务背景

很多后台系统早期只做菜单权限和接口权限，例如：

```text
用户是否能进入客户管理菜单
用户是否能调用客户列表接口
用户是否能新增、编辑、删除客户
```

但实际业务中，仅有接口权限是不够的。典型问题包括：

| 用户类型     | 数据可见范围                       |
| ------------ | ---------------------------------- |
| 普通员工     | 只能查看本人负责的数据             |
| 部门主管     | 可以查看本部门数据                 |
| 区域负责人   | 可以查看本部门及子部门数据         |
| 指定角色用户 | 可以查看配置好的自定义部门数据     |
| 租户管理员   | 可以查看当前租户下的全部数据       |
| 平台管理员   | 可以跨租户管理平台数据，需谨慎开放 |

如果每个业务接口都手动拼接数据权限条件，会带来以下问题：

```text
权限逻辑分散，维护成本高
不同接口的数据权限口径容易不一致
开发人员容易漏加 tenant_id 或 dept_id 条件
复杂 SQL 场景下容易出现越权数据
字符串拼接 SQL 存在注入风险
```

因此，本案例采用“登录上下文 + 数据权限注解 + MyBatis 拦截器 + JSqlParser SQL 改写”的方式，将数据权限能力沉淀为通用基础组件。

本案例以客户数据为业务对象，简化后的业务表字段如下：

```text
biz_customer
- id：客户 ID
- tenant_id：租户 ID
- dept_id：所属部门 ID
- owner_user_id：负责人用户 ID
- customer_name：客户名称
- deleted：逻辑删除标识
```

客户列表查询时需要同时满足以下规则：

```text
租户隔离：tenant_id = 当前租户 ID
组织隔离：dept_id 在当前用户可见部门范围内
本人数据：owner_user_id = 当前用户 ID
管理员绕过：管理员可跳过组织权限条件
```

### 核心功能

本案例只实现数据权限与组织隔离的核心能力，不扩展完整 RBAC 权限系统。菜单权限、按钮权限、接口权限可以由 Sa-Token 或 Spring Security 单独处理。

核心功能如下：

| 功能           | 说明                                                   |
| -------------- | ------------------------------------------------------ |
| 登录用户上下文 | 保存当前 userId、tenantId、deptId、roleIds             |
| 数据范围枚举   | 支持本人、本部门、本部门及子部门、自定义部门、全部数据 |
| 多角色权限合并 | 一个用户拥有多个角色时，合并角色的数据范围             |
| 租户隔离       | 查询业务数据时强制追加 tenant_id 条件                  |
| 组织隔离       | 根据角色数据范围自动追加 dept_id 或 owner_user_id 条件 |
| 管理员绕过     | 超级管理员可跳过组织权限，但一般不跳过租户隔离         |
| 注解控制       | 通过注解标记需要进行数据权限处理的 Mapper 方法         |
| SQL 自动改写   | 使用 MyBatis 拦截器统一追加权限 SQL                    |
| 权限缓存       | 将用户可见部门集合缓存到 Redis，减少重复计算           |

本案例支持的数据范围类型如下：

| 数据范围           | 编码 | 权限含义                     |
| ------------------ | ---- | ---------------------------- |
| 全部数据           | 1    | 当前租户下全部业务数据       |
| 本部门及子部门数据 | 2    | 当前用户部门及所有子部门数据 |
| 本部门数据         | 3    | 当前用户所在部门数据         |
| 本人数据           | 4    | 当前用户负责的数据           |
| 自定义部门数据     | 5    | 角色配置的指定部门数据       |

多角色合并规则如下：

```text
如果任意角色拥有“全部数据”权限：
    当前租户内不追加组织权限条件

否则：
    合并所有角色的数据范围
    本人数据转为 owner_user_id = 当前用户 ID
    部门数据转为 dept_id IN 可见部门 ID 集合
    最终使用 OR 组合本人数据和部门数据
```

示例：

```text
当前用户：
userId = 2001
tenantId = 1001
deptId = 10

角色 A：本人数据
角色 B：本部门及子部门数据
角色 C：自定义部门 30、31

最终权限：
tenant_id = 1001
AND (
    owner_user_id = 2001
    OR dept_id IN (10, 11, 12, 30, 31)
)
```

### 技术选型

本案例使用 Spring Boot 3 体系，结合 Sa-Token、MyBatis-Plus、Redis、JSqlParser 实现数据权限控制。

| 技术                | 用途                                 |
| ------------------- | ------------------------------------ |
| Spring Boot 3       | 基础 Web 框架                        |
| Sa-Token            | 登录认证、会话管理、获取当前登录用户 |
| MyBatis-Plus        | ORM、分页查询、逻辑删除              |
| MyBatis Interceptor | 拦截查询 SQL，自动追加权限条件       |
| JSqlParser          | 解析并改写 SQL，避免字符串硬拼接     |
| Redis               | 缓存用户可见部门范围                 |
| MySQL               | 存储用户、角色、部门、业务数据       |
| Hutool              | 集合、字符串、JSON、Bean 工具处理    |
| Lombok              | 简化实体、DTO、VO 代码               |

核心技术职责如下：

```text
Sa-Token：
负责获取当前登录用户

MyBatis-Plus：
负责业务数据查询和分页

MyBatis Interceptor：
负责拦截 Executor 查询方法

JSqlParser：
负责安全解析和改写 SQL

Redis：
负责缓存用户权限计算结果

Hutool：
负责集合判空、去重、类型转换、JSON 处理
```

不推荐直接字符串拼接 SQL：

```text
不推荐：
sql + " AND dept_id IN (" + deptIds + ")"

推荐：
使用 JSqlParser 解析 SQL AST，再追加 EqualsTo、InExpression、AndExpression、OrExpression
```

后续实现会采用以下工程分层：

```text
controller
-> 提供客户列表查询接口

service
-> 处理客户业务逻辑

mapper
-> 执行业务数据查询

context
-> 维护当前登录用户上下文

permission
-> 计算数据权限范围

interceptor
-> 拦截并改写 SQL

entity / dto / vo
-> 存放实体、请求参数、响应对象
```

## 权限范围模型

权限范围模型用于定义“当前用户能看到哪些业务数据”。本案例围绕用户、角色、部门、租户四类核心信息计算数据范围，最终生成可追加到业务 SQL 中的权限条件。该模块对应原始 README 中“数据权限与组织隔离”的核心难点：本人数据、本部门数据、本部门及子部门数据、自定义部门数据、多角色权限合并、租户隔离叠加、管理员绕过和 SQL 注入风险控制。

### 数据范围类型

本案例将数据范围抽象成固定枚举，角色只需要绑定某一种数据范围即可。查询业务数据时，系统根据当前用户的所有角色计算最终可见范围。

| 数据范围           | 编码 | 说明                         | SQL 条件                     |
| ------------------ | ---- | ---------------------------- | ---------------------------- |
| 全部数据           | 1    | 当前租户下全部数据           | 不追加组织权限条件           |
| 本部门及子部门数据 | 2    | 当前用户部门及所有子部门数据 | `dept_id IN (...)`           |
| 本部门数据         | 3    | 当前用户所在部门数据         | `dept_id = 当前部门ID`       |
| 本人数据           | 4    | 当前用户负责的数据           | `owner_user_id = 当前用户ID` |
| 自定义部门数据     | 5    | 角色配置的指定部门数据       | `dept_id IN (...)`           |

数据范围枚举后续会用于权限计算、数据库存储和 SQL 改写。

文件位置：`src/main/java/io/github/atengk/permission/enums/DataScopeType.java`

```java
package io.github.atengk.permission.enums;

import cn.hutool.core.util.NumberUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据权限范围类型
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum DataScopeType {

    /**
     * 全部数据
     */
    ALL(1, "全部数据"),

    /**
     * 本部门及子部门数据
     */
    DEPT_AND_CHILD(2, "本部门及子部门数据"),

    /**
     * 本部门数据
     */
    DEPT_ONLY(3, "本部门数据"),

    /**
     * 本人数据
     */
    SELF_ONLY(4, "本人数据"),

    /**
     * 自定义部门数据
     */
    CUSTOM_DEPT(5, "自定义部门数据");

    private final Integer code;

    private final String desc;

    /**
     * 根据编码获取数据范围类型
     *
     * @param code 编码
     * @return 数据范围类型
     */
    public static DataScopeType of(Integer code) {
        for (DataScopeType item : values()) {
            if (NumberUtil.equals(item.getCode(), code)) {
                return item;
            }
        }
        throw new IllegalArgumentException("不支持的数据权限范围类型：" + code);
    }
}
```

### 多角色权限合并

一个用户可能同时拥有多个角色，例如“销售人员 + 部门主管 + 项目成员”。数据权限不能简单取第一个角色，而应该按照并集规则合并。

推荐合并规则如下：

```text
1. 如果任意角色拥有“全部数据”权限：
   当前租户内不追加组织权限条件。

2. 如果没有“全部数据”权限：
   合并所有角色产生的部门范围。

3. 如果存在“本人数据”权限：
   追加 owner_user_id = 当前用户 ID。

4. 如果同时存在本人数据和部门数据：
   使用 OR 组合。
```

示例：

```text
当前用户：
user_id = 2001
tenant_id = 1001
dept_id = 10

角色 A：本人数据
角色 B：本部门及子部门数据
角色 C：自定义部门数据，部门范围为 30、31

最终权限：
tenant_id = 1001
AND (
    owner_user_id = 2001
    OR dept_id IN (10, 11, 12, 30, 31)
)
```

为了方便后续 SQL 改写，可以将最终结果封装成统一的权限上下文。

文件位置：`src/main/java/io/github/atengk/permission/model/DataPermissionScope.java`

```java
package io.github.atengk.permission.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * 数据权限计算结果
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class DataPermissionScope implements Serializable {

    /**
     * 是否拥有全部数据权限
     */
    private Boolean allData = false;

    /**
     * 是否包含本人数据权限
     */
    private Boolean selfData = false;

    /**
     * 当前用户 ID
     */
    private Long userId;

    /**
     * 当前租户 ID
     */
    private Long tenantId;

    /**
     * 可见部门 ID 集合
     */
    private Set<Long> deptIds;
}
```

### 租户隔离叠加

组织权限解决的是同一租户内部的数据可见范围，租户隔离解决的是不同租户之间的数据边界。

本案例默认所有业务数据表都包含 `tenant_id` 字段。只要当前用户不是平台级超级管理员，查询业务数据时都必须追加租户条件。

```sql
tenant_id = 当前租户 ID
```

租户隔离和组织权限的组合方式如下：

```text
普通员工：
tenant_id = 当前租户 ID
AND owner_user_id = 当前用户 ID

部门主管：
tenant_id = 当前租户 ID
AND dept_id IN (当前部门及子部门)

租户管理员：
tenant_id = 当前租户 ID

平台管理员：
可根据接口能力决定是否允许跨租户查询
```

本案例推荐默认策略：

```text
超级管理员可以绕过组织权限
但不建议默认绕过租户权限

如确实需要跨租户管理：
必须通过单独的后台管理接口处理
不要让普通业务接口自动跨租户
```

### 管理员绕过规则

管理员绕过需要区分“平台超级管理员”和“租户管理员”。

| 管理员类型     | 是否绕过组织权限 | 是否绕过租户隔离 | 说明                       |
| -------------- | ---------------- | ---------------- | -------------------------- |
| 平台超级管理员 | 是               | 可选             | 适合平台运维后台，风险较高 |
| 租户管理员     | 是               | 否               | 只能查看当前租户全部数据   |
| 普通管理员     | 按角色规则       | 否               | 按配置的数据范围计算       |

推荐做法是：

```text
1. 平台超级管理员：
   通过特殊角色编码识别，例如 platform_admin。

2. 租户管理员：
   拥有当前租户下的“全部数据”权限。

3. 普通用户：
   通过角色数据范围计算最终权限。

4. 业务接口默认不开放跨租户查询。
```

后续在权限计算服务中，可以通过角色编码判断是否管理员：

```text
platform_admin：平台超级管理员
tenant_admin：租户管理员
sales_manager：销售主管
sales_user：普通销售
```

## 数据库表设计

本案例使用 MySQL 存储用户、角色、部门、角色数据权限和客户业务数据。为了聚焦核心实现，表结构只保留必要字段。

### 用户表

用户表保存用户的租户、部门、账号状态等基础信息。一个用户归属于一个租户和一个主部门。

```sql
CREATE TABLE sys_user (
    id BIGINT NOT NULL COMMENT '用户ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    dept_id BIGINT NOT NULL COMMENT '所属部门ID',
    username VARCHAR(64) NOT NULL COMMENT '登录账号',
    nickname VARCHAR(64) NOT NULL COMMENT '用户昵称',
    password VARCHAR(255) NOT NULL COMMENT '登录密码',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_username (tenant_id, username),
    KEY idx_tenant_dept (tenant_id, dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';
```

### 角色表

角色表保存角色编码、角色名称和数据范围类型。这里将常用数据范围直接放在角色表中，适合中小型后台系统。

```sql
CREATE TABLE sys_role (
    id BIGINT NOT NULL COMMENT '角色ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    data_scope TINYINT NOT NULL COMMENT '数据范围：1全部，2本部门及子部门，3本部门，4本人，5自定义部门',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_role_code (tenant_id, role_code),
    KEY idx_tenant_data_scope (tenant_id, data_scope)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';
```

### 用户角色表

用户角色表用于维护用户和角色的多对多关系。一个用户可以拥有多个角色。

```sql
CREATE TABLE sys_user_role (
    id BIGINT NOT NULL COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (tenant_id, user_id, role_id),
    KEY idx_tenant_user (tenant_id, user_id),
    KEY idx_tenant_role (tenant_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';
```

### 部门表

部门表使用 `parent_id` 维护组织树。查询“本部门及子部门数据”时，需要递归获取当前部门下的所有子部门 ID。

```sql
CREATE TABLE sys_dept (
    id BIGINT NOT NULL COMMENT '部门ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父部门ID，0表示根部门',
    dept_name VARCHAR(100) NOT NULL COMMENT '部门名称',
    ancestors VARCHAR(500) DEFAULT NULL COMMENT '祖级列表，例如：0,1,10',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_tenant_parent (tenant_id, parent_id),
    KEY idx_tenant_ancestors (tenant_id, ancestors)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';
```

### 角色数据权限表

角色数据权限表用于保存自定义部门数据范围。只有当角色的 `data_scope = 5` 时才需要读取该表。

```sql
CREATE TABLE sys_role_data_scope (
    id BIGINT NOT NULL COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    dept_id BIGINT NOT NULL COMMENT '可见部门ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_dept (tenant_id, role_id, dept_id),
    KEY idx_tenant_role (tenant_id, role_id),
    KEY idx_tenant_dept (tenant_id, dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色自定义数据权限表';
```

### 业务数据表

业务数据表以客户表为例。关键字段是 `tenant_id`、`dept_id`、`owner_user_id`，后续 SQL 拦截器会基于这些字段追加权限条件。

```sql
CREATE TABLE biz_customer (
    id BIGINT NOT NULL COMMENT '客户ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    dept_id BIGINT NOT NULL COMMENT '所属部门ID',
    owner_user_id BIGINT NOT NULL COMMENT '负责人用户ID',
    customer_name VARCHAR(100) NOT NULL COMMENT '客户名称',
    customer_level VARCHAR(32) DEFAULT NULL COMMENT '客户等级',
    contact_phone VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_tenant_dept (tenant_id, dept_id),
    KEY idx_tenant_owner (tenant_id, owner_user_id),
    KEY idx_tenant_name (tenant_id, customer_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户业务表';
```

建议所有需要数据权限控制的业务表统一保留以下字段：

| 字段            | 说明         |
| --------------- | ------------ |
| `tenant_id`     | 租户隔离字段 |
| `dept_id`       | 组织隔离字段 |
| `owner_user_id` | 本人数据字段 |
| `deleted`       | 逻辑删除字段 |
| `create_time`   | 创建时间     |
| `update_time`   | 更新时间     |

## 项目依赖与配置

本案例基于 Spring Boot 3、MyBatis-Plus、Sa-Token、Redis、Hutool 和 JSqlParser 实现。下面先给出最小可运行依赖和核心配置，后续代码都基于这些配置展开。

### Maven 依赖

在 `pom.xml` 中添加以下依赖。版本号可以按项目实际 Spring Boot 版本统一管理。

```xml
<dependencies>
    <!-- Spring Boot Web：提供 REST 接口能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot AOP：后续可用于注解增强和上下文处理 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>

    <!-- MyBatis-Plus：提供 ORM、分页、逻辑删除等能力 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.7</version>
    </dependency>

    <!-- MySQL 驱动：连接 MySQL 数据库 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Sa-Token：登录认证和会话管理 -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-spring-boot3-starter</artifactId>
        <version>1.39.0</version>
    </dependency>

    <!-- Redis：缓存用户数据权限计算结果 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- Hutool：集合、字符串、JSON、Bean 等工具类 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.27</version>
    </dependency>

    <!-- JSqlParser：安全解析和改写 SQL -->
    <dependency>
        <groupId>com.github.jsqlparser</groupId>
        <artifactId>jsqlparser</artifactId>
        <version>4.9</version>
    </dependency>

    <!-- Lombok：简化 Getter、Setter、构造方法等代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot Test：接口和服务测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### MyBatis-Plus 配置

MyBatis-Plus 配置主要包含 Mapper 扫描、分页插件、逻辑删除字段配置。数据权限拦截器会在后续章节单独实现并注册。

文件位置：`src/main/java/io/github/atengk/config/MybatisPlusConfig.java`

```java
package io.github.atengk.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Configuration
@MapperScan("io.github.atengk.**.mapper")
public class MybatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 插件
     *
     * @return MyBatis-Plus 拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件：用于分页查询客户列表等接口
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;
    }
}
```

文件位置：`src/main/resources/application.yml`

```yaml
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: io.github.atengk.**.entity
  global-config:
    db-config:
      # 主键策略：使用雪花 ID
      id-type: assign_id
      # 逻辑删除字段
      logic-delete-field: deleted
      # 已删除值
      logic-delete-value: 1
      # 未删除值
      logic-not-delete-value: 0
  configuration:
    # 开发环境可开启 SQL 日志，生产环境建议关闭或改为日志框架控制
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    # 下划线字段自动映射驼峰属性
    map-underscore-to-camel-case: true
```

### Sa-Token 登录配置

Sa-Token 用于维护登录态。本案例中，登录成功后需要将用户 ID 作为登录 ID，并将租户 ID、部门 ID、角色信息放入会话中，方便后续权限计算。

文件位置：`src/main/resources/application.yml`

```yaml
sa-token:
  # Token 名称
  token-name: Authorization
  # Token 有效期，单位秒，默认 2 小时
  timeout: 7200
  # 临时有效期，超过该时间无操作会自动过期
  active-timeout: 1800
  # 是否允许同一账号多地同时登录
  is-concurrent: true
  # 多人登录同一账号时是否共用一个 Token
  is-share: false
  # Token 风格
  token-style: uuid
  # 是否输出操作日志
  is-log: true
```

登录成功后建议写入的会话信息：

```text
userId：当前用户 ID
tenantId：当前租户 ID
deptId：当前部门 ID
roleIds：当前用户角色 ID 集合
roleCodes：当前用户角色编码集合
```

后续可以通过 Sa-Token 获取：

```java
Long userId = StpUtil.getLoginIdAsLong();
Long tenantId = StpUtil.getSession().getLong("tenantId");
Long deptId = StpUtil.getSession().getLong("deptId");
```

### Redis 缓存配置

Redis 用于缓存用户最终计算出来的数据权限范围，避免每次查询都重复读取角色、部门树和自定义权限表。

推荐缓存 Key 设计：

```text
data-permission:scope:{tenantId}:{userId}
```

示例：

```text
data-permission:scope:1001:2001
```

缓存内容可以是 `DataPermissionScope` 的 JSON 字符串，缓存时间建议 10 到 30 分钟。角色、部门、用户关系发生变更时，需要主动删除相关用户缓存。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  data:
    redis:
      # Redis 地址
      host: 127.0.0.1
      # Redis 端口
      port: 6379
      # Redis 密码，没有密码可不配置
      password:
      # Redis 数据库索引
      database: 0
      # 连接超时时间
      timeout: 3000ms
      lettuce:
        pool:
          # 最大连接数
          max-active: 16
          # 最大空闲连接数
          max-idle: 8
          # 最小空闲连接数
          min-idle: 2
          # 最大等待时间
          max-wait: 3000ms
```

Redis 序列化配置如下，用于避免默认 JDK 序列化结果不可读的问题。

文件位置：`src/main/java/io/github/atengk/config/RedisConfig.java`

```java
package io.github.atengk.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;

/**
 * Redis 配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Configuration
public class RedisConfig {

    /**
     * 配置 RedisTemplate 序列化方式
     *
     * @param redisConnectionFactory Redis 连接工厂
     * @return RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = buildJsonSerializer();

        // Key 使用字符串序列化，便于排查缓存数据
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value 使用 JSON 序列化，便于存储权限对象
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 构建 JSON 序列化器
     *
     * @return JSON 序列化器
     */
    private GenericJackson2JsonRedisSerializer buildJsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }
}
```

## 登录用户上下文

登录用户上下文用于保存当前请求中的用户身份信息。数据权限计算不能直接依赖前端传参，而应该从服务端登录态中获取 `userId`、`tenantId`、`deptId`、`roleIds` 等信息，避免用户伪造请求参数造成越权访问。

### 登录成功写入用户信息

用户登录成功后，使用 Sa-Token 写入登录态，并将数据权限需要的关键信息写入 Session。

文件位置：`src/main/java/io/github/atengk/auth/model/LoginUserInfo.java`

```java
package io.github.atengk.auth.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * 登录用户信息
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class LoginUserInfo implements Serializable {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 角色ID集合
     */
    private Set<Long> roleIds;

    /**
     * 角色编码集合
     */
    private Set<String> roleCodes;
}
```

文件位置：`src/main/java/io/github/atengk/auth/constant/AuthSessionKey.java`

```java
package io.github.atengk.auth.constant;

/**
 * 登录会话 Key
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface AuthSessionKey {

    /**
     * 登录用户信息
     */
    String LOGIN_USER = "loginUser";
}
```

下面的代码演示登录成功后如何写入 Sa-Token 会话信息。

文件位置：`src/main/java/io/github/atengk/auth/service/AuthService.java`

```java
package io.github.atengk.auth.service;

import io.github.atengk.auth.model.LoginUserInfo;

/**
 * 认证服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface AuthService {

    /**
     * 登录并写入用户上下文
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录用户信息
     */
    LoginUserInfo login(String username, String password);
}
```

文件位置：`src/main/java/io/github/atengk/auth/service/impl/AuthServiceImpl.java`

```java
package io.github.atengk.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.crypto.digest.BCrypt;
import io.github.atengk.auth.constant.AuthSessionKey;
import io.github.atengk.auth.model.LoginUserInfo;
import io.github.atengk.auth.service.AuthService;
import io.github.atengk.system.entity.SysUser;
import io.github.atengk.system.mapper.SysRoleMapper;
import io.github.atengk.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 认证服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;

    private final SysRoleMapper sysRoleMapper;

    /**
     * 登录并写入用户上下文
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录用户信息
     */
    @Override
    public LoginUserInfo login(String username, String password) {
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            log.warn("登录失败，用户不存在：{}", username);
            throw new IllegalArgumentException("用户名或密码错误");
        }

        if (!BCrypt.checkpw(password, user.getPassword())) {
            log.warn("登录失败，密码错误：{}", username);
            throw new IllegalArgumentException("用户名或密码错误");
        }

        Set<Long> roleIds = sysRoleMapper.selectRoleIdsByUserId(user.getTenantId(), user.getId());
        Set<String> roleCodes = sysRoleMapper.selectRoleCodesByUserId(user.getTenantId(), user.getId());

        if (CollUtil.isEmpty(roleIds)) {
            log.warn("登录失败，用户未绑定角色：userId={}", user.getId());
            throw new IllegalStateException("当前用户未绑定角色");
        }

        LoginUserInfo loginUser = new LoginUserInfo();
        loginUser.setUserId(user.getId());
        loginUser.setTenantId(user.getTenantId());
        loginUser.setDeptId(user.getDeptId());
        loginUser.setRoleIds(roleIds);
        loginUser.setRoleCodes(roleCodes);

        StpUtil.login(user.getId());
        StpUtil.getSession().set(AuthSessionKey.LOGIN_USER, loginUser);

        log.info("用户登录成功：userId={}, tenantId={}, deptId={}", user.getId(), user.getTenantId(), user.getDeptId());
        return loginUser;
    }
}
```

### 当前用户上下文获取

业务代码和数据权限组件不应该到处直接操作 `StpUtil.getSession()`，建议封装一个统一的上下文工具类。

文件位置：`src/main/java/io/github/atengk/auth/context/LoginUserContext.java`

```java
package io.github.atengk.auth.context;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.auth.constant.AuthSessionKey;
import io.github.atengk.auth.model.LoginUserInfo;

import java.util.Set;

/**
 * 登录用户上下文
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LoginUserContext {

    /**
     * 获取当前登录用户
     *
     * @return 登录用户信息
     */
    public static LoginUserInfo getLoginUser() {
        Object value = StpUtil.getSession().get(AuthSessionKey.LOGIN_USER);
        if (ObjectUtil.isNull(value)) {
            throw new IllegalStateException("当前登录用户上下文不存在");
        }
        return (LoginUserInfo) value;
    }

    /**
     * 获取当前用户ID
     *
     * @return 用户ID
     */
    public static Long getUserId() {
        return getLoginUser().getUserId();
    }

    /**
     * 获取当前租户ID
     *
     * @return 租户ID
     */
    public static Long getTenantId() {
        return getLoginUser().getTenantId();
    }

    /**
     * 获取当前部门ID
     *
     * @return 部门ID
     */
    public static Long getDeptId() {
        return getLoginUser().getDeptId();
    }

    /**
     * 获取当前用户角色ID集合
     *
     * @return 角色ID集合
     */
    public static Set<Long> getRoleIds() {
        Set<Long> roleIds = getLoginUser().getRoleIds();
        if (CollUtil.isEmpty(roleIds)) {
            throw new IllegalStateException("当前用户角色为空");
        }
        return roleIds;
    }

    /**
     * 判断当前用户是否拥有指定角色
     *
     * @param roleCode 角色编码
     * @return 是否拥有
     */
    public static boolean hasRole(String roleCode) {
        Set<String> roleCodes = getLoginUser().getRoleCodes();
        return CollUtil.isNotEmpty(roleCodes) && roleCodes.contains(roleCode);
    }
}
```

### 租户 ID 获取

租户 ID 必须从服务端登录态获取，不能从前端请求参数中直接信任。

在业务代码中获取当前租户：

```java
Long tenantId = LoginUserContext.getTenantId();
```

在 SQL 权限拦截器中获取当前租户：

```java
Long tenantId = LoginUserContext.getTenantId();
```

租户隔离默认规则如下：

```text
普通业务接口：
必须追加 tenant_id = 当前租户ID

租户管理员：
可以查看当前租户下全部数据，但不能跨租户

平台管理员：
如需跨租户，应通过专门接口控制，不建议默认放开
```

### 部门 ID 获取

部门 ID 用于计算本部门、本部门及子部门、自定义部门等组织权限范围。

在业务代码中获取当前部门：

```java
Long deptId = LoginUserContext.getDeptId();
```

部门权限计算中会使用：

```text
本部门数据：
dept_id = 当前部门ID

本部门及子部门数据：
dept_id IN 当前部门及所有子部门ID

自定义部门数据：
dept_id IN 角色绑定的部门ID集合
```

## 数据权限计算

数据权限计算用于把用户拥有的多个角色转换成统一的权限结果。最终结果包含：是否拥有全部数据权限、是否包含本人数据、可见部门 ID 集合、当前用户 ID、当前租户 ID。

### 用户角色查询

用户角色查询需要返回当前用户在当前租户下的有效角色。

文件位置：`src/main/java/io/github/atengk/system/entity/SysRole.java`

```java
package io.github.atengk.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 系统角色
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("sys_role")
public class SysRole implements Serializable {

    /**
     * 角色ID
     */
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 数据范围：1全部，2本部门及子部门，3本部门，4本人，5自定义部门
     */
    private Integer dataScope;

    /**
     * 状态：1启用，0禁用
     */
    private Integer status;

    /**
     * 逻辑删除：0否，1是
     */
    private Integer deleted;
}
```

文件位置：`src/main/java/io/github/atengk/system/mapper/SysRoleMapper.java`

```java
package io.github.atengk.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.system.entity.SysRole;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * 系统角色 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 查询用户角色列表
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 角色列表
     */
    List<SysRole> selectRolesByUserId(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    /**
     * 查询用户角色ID集合
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 角色ID集合
     */
    Set<Long> selectRoleIdsByUserId(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    /**
     * 查询用户角色编码集合
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 角色编码集合
     */
    Set<String> selectRoleCodesByUserId(@Param("tenantId") Long tenantId, @Param("userId") Long userId);
}
```

文件位置：`src/main/resources/mapper/system/SysRoleMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="io.github.atengk.system.mapper.SysRoleMapper">

    <!-- 查询用户有效角色列表 -->
    <select id="selectRolesByUserId" resultType="io.github.atengk.system.entity.SysRole">
        SELECT
            r.id,
            r.tenant_id,
            r.role_code,
            r.role_name,
            r.data_scope,
            r.status,
            r.deleted
        FROM sys_user_role ur
        INNER JOIN sys_role r ON r.id = ur.role_id
            AND r.tenant_id = ur.tenant_id
        WHERE ur.tenant_id = #{tenantId}
          AND ur.user_id = #{userId}
          AND r.status = 1
          AND r.deleted = 0
    </select>

    <!-- 查询用户角色ID集合 -->
    <select id="selectRoleIdsByUserId" resultType="java.lang.Long">
        SELECT
            r.id
        FROM sys_user_role ur
        INNER JOIN sys_role r ON r.id = ur.role_id
            AND r.tenant_id = ur.tenant_id
        WHERE ur.tenant_id = #{tenantId}
          AND ur.user_id = #{userId}
          AND r.status = 1
          AND r.deleted = 0
    </select>

    <!-- 查询用户角色编码集合 -->
    <select id="selectRoleCodesByUserId" resultType="java.lang.String">
        SELECT
            r.role_code
        FROM sys_user_role ur
        INNER JOIN sys_role r ON r.id = ur.role_id
            AND r.tenant_id = ur.tenant_id
        WHERE ur.tenant_id = #{tenantId}
          AND ur.user_id = #{userId}
          AND r.status = 1
          AND r.deleted = 0
    </select>

</mapper>
```

### 组织树查询

组织树查询用于获取当前部门的所有子部门。这里基于 `ancestors` 字段实现，避免在 Java 中递归多次查询数据库。

文件位置：`src/main/java/io/github/atengk/system/mapper/SysDeptMapper.java`

```java
package io.github.atengk.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.system.entity.SysDept;
import org.apache.ibatis.annotations.Param;

import java.util.Set;

/**
 * 部门 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /**
     * 查询当前部门及所有子部门ID
     *
     * @param tenantId 租户ID
     * @param deptId 部门ID
     * @return 部门ID集合
     */
    Set<Long> selectSelfAndChildDeptIds(@Param("tenantId") Long tenantId, @Param("deptId") Long deptId);
}
```

文件位置：`src/main/resources/mapper/system/SysDeptMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="io.github.atengk.system.mapper.SysDeptMapper">

    <!--
        查询本部门及子部门：
        1. id = 当前部门ID，表示本部门
        2. FIND_IN_SET 当前部门ID，表示 ancestors 中包含当前部门
    -->
    <select id="selectSelfAndChildDeptIds" resultType="java.lang.Long">
        SELECT
            id
        FROM sys_dept
        WHERE tenant_id = #{tenantId}
          AND deleted = 0
          AND status = 1
          AND (
              id = #{deptId}
              OR FIND_IN_SET(#{deptId}, ancestors)
          )
    </select>

</mapper>
```

自定义部门权限需要读取角色绑定的部门 ID。

文件位置：`src/main/java/io/github/atengk/system/mapper/SysRoleDataScopeMapper.java`

```java
package io.github.atengk.system.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.Set;

/**
 * 角色自定义数据权限 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface SysRoleDataScopeMapper {

    /**
     * 根据角色ID集合查询自定义部门ID集合
     *
     * @param tenantId 租户ID
     * @param roleIds 角色ID集合
     * @return 部门ID集合
     */
    Set<Long> selectDeptIdsByRoleIds(@Param("tenantId") Long tenantId, @Param("roleIds") Set<Long> roleIds);
}
```

文件位置：`src/main/resources/mapper/system/SysRoleDataScopeMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="io.github.atengk.system.mapper.SysRoleDataScopeMapper">

    <!-- 查询角色绑定的自定义部门 -->
    <select id="selectDeptIdsByRoleIds" resultType="java.lang.Long">
        SELECT
            dept_id
        FROM sys_role_data_scope
        WHERE tenant_id = #{tenantId}
          AND role_id IN
          <foreach collection="roleIds" item="roleId" open="(" separator="," close=")">
              #{roleId}
          </foreach>
    </select>

</mapper>
```

### 可见部门范围计算

可见部门范围计算是数据权限的核心逻辑。它会把多个角色的数据范围合并成最终的 `DataPermissionScope`。

文件位置：`src/main/java/io/github/atengk/permission/service/DataPermissionService.java`

```java
package io.github.atengk.permission.service;

import io.github.atengk.permission.model.DataPermissionScope;

/**
 * 数据权限服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface DataPermissionService {

    /**
     * 获取当前用户数据权限范围
     *
     * @return 数据权限范围
     */
    DataPermissionScope getCurrentUserScope();

    /**
     * 清理指定用户数据权限缓存
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     */
    void clearUserScopeCache(Long tenantId, Long userId);
}
```

下面的实现会优先读取 Redis 缓存；缓存不存在时，重新查询角色、部门树和自定义部门权限并计算结果。

文件位置：`src/main/java/io/github/atengk/permission/service/impl/DataPermissionServiceImpl.java`

```java
package io.github.atengk.permission.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.auth.context.LoginUserContext;
import io.github.atengk.permission.enums.DataScopeType;
import io.github.atengk.permission.model.DataPermissionScope;
import io.github.atengk.permission.service.DataPermissionService;
import io.github.atengk.system.entity.SysRole;
import io.github.atengk.system.mapper.SysDeptMapper;
import io.github.atengk.system.mapper.SysRoleDataScopeMapper;
import io.github.atengk.system.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据权限服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataPermissionServiceImpl implements DataPermissionService {

    private static final String CACHE_KEY_PREFIX = "data-permission:scope:";

    private static final Duration CACHE_TTL = Duration.ofMinutes(20);

    private static final String PLATFORM_ADMIN = "platform_admin";

    private final SysRoleMapper sysRoleMapper;

    private final SysDeptMapper sysDeptMapper;

    private final SysRoleDataScopeMapper sysRoleDataScopeMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取当前用户数据权限范围
     *
     * @return 数据权限范围
     */
    @Override
    public DataPermissionScope getCurrentUserScope() {
        Long tenantId = LoginUserContext.getTenantId();
        Long userId = LoginUserContext.getUserId();
        String cacheKey = buildCacheKey(tenantId, userId);

        Object cacheValue = redisTemplate.opsForValue().get(cacheKey);
        if (cacheValue instanceof DataPermissionScope scope) {
            return scope;
        }

        DataPermissionScope scope = calculateScope(tenantId, userId, LoginUserContext.getDeptId());
        redisTemplate.opsForValue().set(cacheKey, scope, CACHE_TTL);

        log.info("数据权限范围计算完成：userId={}, scope={}", userId, JSONUtil.toJsonStr(scope));
        return scope;
    }

    /**
     * 清理指定用户数据权限缓存
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     */
    @Override
    public void clearUserScopeCache(Long tenantId, Long userId) {
        String cacheKey = buildCacheKey(tenantId, userId);
        redisTemplate.delete(cacheKey);
        log.info("已清理用户数据权限缓存：tenantId={}, userId={}", tenantId, userId);
    }

    /**
     * 计算用户数据权限范围
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param deptId 部门ID
     * @return 数据权限范围
     */
    private DataPermissionScope calculateScope(Long tenantId, Long userId, Long deptId) {
        DataPermissionScope scope = new DataPermissionScope();
        scope.setTenantId(tenantId);
        scope.setUserId(userId);
        scope.setDeptIds(new HashSet<>());

        if (LoginUserContext.hasRole(PLATFORM_ADMIN)) {
            scope.setAllData(true);
            log.info("平台管理员跳过组织权限：userId={}", userId);
            return scope;
        }

        List<SysRole> roles = sysRoleMapper.selectRolesByUserId(tenantId, userId);
        if (CollUtil.isEmpty(roles)) {
            log.warn("用户未绑定有效角色，默认无业务数据权限：tenantId={}, userId={}", tenantId, userId);
            return scope;
        }

        Set<Long> customRoleIds = new HashSet<>();

        for (SysRole role : roles) {
            DataScopeType scopeType = DataScopeType.of(role.getDataScope());

            if (scopeType == DataScopeType.ALL) {
                scope.setAllData(true);
                return scope;
            }

            if (scopeType == DataScopeType.SELF_ONLY) {
                scope.setSelfData(true);
                continue;
            }

            if (scopeType == DataScopeType.DEPT_ONLY) {
                scope.getDeptIds().add(deptId);
                continue;
            }

            if (scopeType == DataScopeType.DEPT_AND_CHILD) {
                Set<Long> deptIds = sysDeptMapper.selectSelfAndChildDeptIds(tenantId, deptId);
                if (CollUtil.isNotEmpty(deptIds)) {
                    scope.getDeptIds().addAll(deptIds);
                }
                continue;
            }

            if (scopeType == DataScopeType.CUSTOM_DEPT) {
                customRoleIds.add(role.getId());
            }
        }

        if (CollUtil.isNotEmpty(customRoleIds)) {
            Set<Long> customDeptIds = sysRoleDataScopeMapper.selectDeptIdsByRoleIds(tenantId, customRoleIds);
            if (CollUtil.isNotEmpty(customDeptIds)) {
                scope.getDeptIds().addAll(customDeptIds);
            }
        }

        if (BooleanUtil.isFalse(scope.getSelfData()) && CollUtil.isEmpty(scope.getDeptIds())) {
            log.warn("用户无可见部门且无本人数据权限：tenantId={}, userId={}", tenantId, userId);
        }

        return scope;
    }

    /**
     * 构建缓存 Key
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 缓存 Key
     */
    private String buildCacheKey(Long tenantId, Long userId) {
        return CACHE_KEY_PREFIX + tenantId + ":" + userId;
    }
}
```

### 数据权限缓存

数据权限缓存建议在以下场景主动清理：

```text
用户部门变更
用户角色变更
角色数据范围变更
角色绑定的自定义部门变更
部门组织树变更
```

单用户清理：

```java
dataPermissionService.clearUserScopeCache(1001L, 2001L);
```

如果角色权限变更影响多个用户，建议查询角色下所有用户后批量清理：

```text
角色权限变更
-> 查询绑定该角色的用户
-> 逐个删除 data-permission:scope:{tenantId}:{userId}
```

缓存策略建议：

| 场景         | 建议                   |
| ------------ | ---------------------- |
| 普通查询     | 优先读 Redis           |
| 缓存不存在   | 实时计算并写入 Redis   |
| 权限变更     | 主动删除缓存           |
| 缓存时长     | 10 到 30 分钟          |
| 强一致要求高 | 权限变更后必须删除缓存 |

## SQL 自动拼接权限条件

SQL 自动拼接权限条件的目标是让业务层无需手写 `tenant_id`、`dept_id`、`owner_user_id` 条件。业务 Mapper 方法只要标记数据权限注解，MyBatis 拦截器就会自动改写查询 SQL。

### 数据权限注解

数据权限注解用于声明当前 Mapper 或 Mapper 方法是否启用数据权限，并指定业务表中的权限字段。

文件位置：`src/main/java/io/github/atengk/permission/annotation/DataPermission.java`

```java
package io.github.atengk.permission.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {

    /**
     * 表别名，例如 c
     *
     * @return 表别名
     */
    String tableAlias() default "";

    /**
     * 租户字段
     *
     * @return 租户字段
     */
    String tenantColumn() default "tenant_id";

    /**
     * 部门字段
     *
     * @return 部门字段
     */
    String deptColumn() default "dept_id";

    /**
     * 负责人字段
     *
     * @return 负责人字段
     */
    String ownerColumn() default "owner_user_id";

    /**
     * 是否忽略租户隔离
     *
     * @return 是否忽略
     */
    boolean ignoreTenant() default false;

    /**
     * 是否忽略组织权限
     *
     * @return 是否忽略
     */
    boolean ignoreOrg() default false;
}
```

业务 Mapper 使用示例：

```java
package io.github.atengk.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.customer.entity.BizCustomer;
import io.github.atengk.permission.annotation.DataPermission;

/**
 * 客户 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@DataPermission(tableAlias = "c")
public interface BizCustomerMapper extends BaseMapper<BizCustomer> {
}
```

如果某个方法需要特殊控制，也可以标记到方法上：

```java
@DataPermission(tableAlias = "c", ignoreOrg = true)
List<BizCustomer> selectTenantCustomerList();
```

### MyBatis 拦截器实现

拦截器在 SQL 执行前获取 `MappedStatement` 和 `BoundSql`，判断当前 Mapper 方法是否存在 `@DataPermission` 注解。如果存在，就调用 SQL 构建器追加权限条件。

文件位置：`src/main/java/io/github/atengk/permission/interceptor/DataPermissionInterceptor.java`

```java
package io.github.atengk.permission.interceptor;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.permission.annotation.DataPermission;
import io.github.atengk.permission.sql.DataPermissionSqlParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Properties;

/**
 * 数据权限 MyBatis 拦截器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class DataPermissionInterceptor implements Interceptor {

    private final DataPermissionSqlParser dataPermissionSqlParser;

    /**
     * 拦截 SQL 执行前的 prepare 阶段
     *
     * @param invocation 调用信息
     * @return 执行结果
     * @throws Throwable 异常
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MetaObject metaObject = getRealMetaObject(invocation.getTarget());

        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        if (mappedStatement.getSqlCommandType() != SqlCommandType.SELECT) {
            return invocation.proceed();
        }

        DataPermission dataPermission = findDataPermission(mappedStatement);
        if (dataPermission == null) {
            return invocation.proceed();
        }

        BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
        String originalSql = boundSql.getSql();

        String newSql = dataPermissionSqlParser.parse(originalSql, dataPermission);
        if (StrUtil.isNotBlank(newSql) && !StrUtil.equals(originalSql, newSql)) {
            metaObject.setValue("delegate.boundSql.sql", newSql);
            log.debug("数据权限 SQL 已改写：{}", newSql);
        }

        return invocation.proceed();
    }

    /**
     * 包装插件
     *
     * @param target 目标对象
     * @return 代理对象
     */
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    /**
     * 设置属性
     *
     * @param properties 属性
     */
    @Override
    public void setProperties(Properties properties) {
        // 当前拦截器暂无额外属性
    }

    /**
     * 获取真实 MetaObject
     *
     * @param target 目标对象
     * @return MetaObject
     */
    private MetaObject getRealMetaObject(Object target) {
        MetaObject metaObject = SystemMetaObject.forObject(target);

        while (metaObject.hasGetter("h")) {
            Object object = metaObject.getValue("h");
            metaObject = SystemMetaObject.forObject(object);
        }

        while (metaObject.hasGetter("target")) {
            Object object = metaObject.getValue("target");
            metaObject = SystemMetaObject.forObject(object);
        }

        return metaObject;
    }

    /**
     * 查找数据权限注解
     *
     * @param mappedStatement MappedStatement
     * @return 数据权限注解
     */
    private DataPermission findDataPermission(MappedStatement mappedStatement) {
        try {
            String statementId = mappedStatement.getId();
            int lastDotIndex = statementId.lastIndexOf(".");
            String className = statementId.substring(0, lastDotIndex);
            String methodName = statementId.substring(lastDotIndex + 1);

            Class<?> mapperClass = Class.forName(className);

            for (Method method : mapperClass.getMethods()) {
                if (StrUtil.equals(method.getName(), methodName) && method.isAnnotationPresent(DataPermission.class)) {
                    return method.getAnnotation(DataPermission.class);
                }
            }

            if (mapperClass.isAnnotationPresent(DataPermission.class)) {
                return mapperClass.getAnnotation(DataPermission.class);
            }

            return null;
        } catch (Exception e) {
            log.warn("解析数据权限注解失败：statementId={}", mappedStatement.getId(), e);
            return null;
        }
    }
}
```

### SQL 条件构建

SQL 条件构建分为两步：

```text
1. 根据当前用户权限范围构建权限表达式
2. 使用 JSqlParser 将表达式追加到原 SQL 的 WHERE 条件中
```

这里不直接拼接用户输入参数，所有 ID 都来自服务端登录态、角色表、部门表，降低 SQL 注入风险。

文件位置：`src/main/java/io/github/atengk/permission/sql/DataPermissionSqlParser.java`

```java
package io.github.atengk.permission.sql;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.permission.annotation.DataPermission;
import io.github.atengk.permission.model.DataPermissionScope;
import io.github.atengk.permission.service.DataPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 数据权限 SQL 解析器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataPermissionSqlParser {

    private final DataPermissionService dataPermissionService;

    /**
     * 改写 SQL
     *
     * @param sql 原始 SQL
     * @param annotation 数据权限注解
     * @return 改写后的 SQL
     */
    public String parse(String sql, DataPermission annotation) {
        try {
            DataPermissionScope scope = dataPermissionService.getCurrentUserScope();
            String conditionSql = buildPermissionCondition(annotation, scope);
            if (StrUtil.isBlank(conditionSql)) {
                return sql;
            }

            Statement statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select select)) {
                return sql;
            }

            if (!(select.getSelectBody() instanceof PlainSelect plainSelect)) {
                log.warn("当前仅处理普通 SELECT，复杂 SQL 已跳过数据权限改写");
                return sql;
            }

            Expression permissionExpression = CCJSqlParserUtil.parseCondExpression(conditionSql);
            Expression where = plainSelect.getWhere();

            if (where == null) {
                plainSelect.setWhere(permissionExpression);
            } else {
                plainSelect.setWhere(new AndExpression(where, permissionExpression));
            }

            return statement.toString();
        } catch (Exception e) {
            log.error("数据权限 SQL 改写失败，已阻断查询：sql={}", sql, e);
            throw new IllegalStateException("数据权限 SQL 改写失败");
        }
    }

    /**
     * 构建权限条件
     *
     * @param annotation 数据权限注解
     * @param scope 数据权限范围
     * @return 权限条件 SQL
     */
    private String buildPermissionCondition(DataPermission annotation, DataPermissionScope scope) {
        List<String> conditions = new ArrayList<>();

        if (!annotation.ignoreTenant()) {
            conditions.add(column(annotation.tableAlias(), annotation.tenantColumn()) + " = " + scope.getTenantId());
        }

        if (!annotation.ignoreOrg() && !Boolean.TRUE.equals(scope.getAllData())) {
            String orgCondition = buildOrgCondition(annotation, scope);
            conditions.add(orgCondition);
        }

        return CollUtil.join(conditions, " AND ");
    }

    /**
     * 构建组织权限条件
     *
     * @param annotation 数据权限注解
     * @param scope 数据权限范围
     * @return 组织权限 SQL
     */
    private String buildOrgCondition(DataPermission annotation, DataPermissionScope scope) {
        List<String> orgConditions = new ArrayList<>();

        if (Boolean.TRUE.equals(scope.getSelfData())) {
            orgConditions.add(column(annotation.tableAlias(), annotation.ownerColumn()) + " = " + scope.getUserId());
        }

        Set<Long> deptIds = scope.getDeptIds();
        if (CollUtil.isNotEmpty(deptIds)) {
            String deptIdText = CollUtil.join(deptIds, ",");
            orgConditions.add(column(annotation.tableAlias(), annotation.deptColumn()) + " IN (" + deptIdText + ")");
        }

        if (CollUtil.isEmpty(orgConditions)) {
            return "1 = 0";
        }

        return "(" + CollUtil.join(orgConditions, " OR ") + ")";
    }

    /**
     * 构建字段名
     *
     * @param tableAlias 表别名
     * @param column 字段名
     * @return 字段名
     */
    private String column(String tableAlias, String column) {
        if (StrUtil.isBlank(tableAlias)) {
            return column;
        }
        return tableAlias + "." + column;
    }
}
```

拦截器注册方式如下。如果已经使用 `@Component` 并且能被 Spring 扫描到，可以不额外配置；如果项目中拦截器注册顺序复杂，建议显式配置。

文件位置：`src/main/java/io/github/atengk/permission/config/DataPermissionConfig.java`

```java
package io.github.atengk.permission.config;

import io.github.atengk.permission.interceptor.DataPermissionInterceptor;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * 数据权限配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Configuration
@RequiredArgsConstructor
public class DataPermissionConfig {

    private final SqlSessionFactory sqlSessionFactory;

    private final DataPermissionInterceptor dataPermissionInterceptor;

    /**
     * 注册数据权限拦截器
     */
    @PostConstruct
    public void addInterceptor() {
        sqlSessionFactory.getConfiguration().addInterceptor(dataPermissionInterceptor);
    }
}
```

### SQL 注入风险控制

数据权限 SQL 改写属于安全敏感逻辑，不能直接信任前端传入的字段名、表别名或 ID 集合。

本案例采用以下控制策略：

| 风险点     | 控制方式                       |
| ---------- | ------------------------------ |
| 租户 ID    | 从 Sa-Token 登录态获取         |
| 用户 ID    | 从 Sa-Token 登录态获取         |
| 部门 ID    | 从数据库组织表查询             |
| 角色范围   | 从数据库角色表查询             |
| 表字段名   | 固定在后端注解中，不从前端传入 |
| SQL 改写   | 使用 JSqlParser 解析后追加条件 |
| 无权限数据 | 默认追加 `1 = 0`，避免放开查询 |

不推荐写法：

```java
String sql = originSql + " AND dept_id IN (" + request.getDeptIds() + ")";
```

推荐写法：

```text
前端不传权限字段
后端从登录态获取 userId、tenantId、deptId
后端查询角色和部门范围
后端构造安全权限条件
JSqlParser 追加到原 SQL
```

对于复杂 SQL，建议逐步增强处理范围：

```text
第一阶段：
只处理普通 SELECT 单表或主表查询

第二阶段：
支持表别名和 JOIN 主表权限

第三阶段：
支持 UNION、子查询、复杂报表 SQL

第四阶段：
对报表类 SQL 单独设计权限视图或权限临时表
```

本案例默认策略是：SQL 改写失败时直接阻断查询，而不是放行原 SQL。

```text
改写成功：
执行追加权限后的 SQL

改写失败：
抛出异常，阻断查询

不建议：
改写失败后执行原 SQL
```

这样可以避免因为解析异常导致越权数据泄露。

## 业务接口实现

业务接口以客户数据为例，演示新增、分页查询、详情查询和管理员查询绕过。客户表包含 `tenant_id`、`dept_id`、`owner_user_id` 三个权限字段，查询时由数据权限拦截器自动追加租户隔离和组织权限条件。该实现对应“查询业务数据时自动拼接权限条件，返回用户有权限的数据”的核心链路。

### 客户数据新增

客户数据新增时，不允许前端传入 `tenantId`、`deptId`、`ownerUserId` 作为最终可信值。后端必须从当前登录用户上下文中获取这些字段，避免用户伪造归属部门或负责人。

文件位置：`src/main/java/io/github/atengk/customer/entity/BizCustomer.java`

```java
package io.github.atengk.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客户业务数据
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("biz_customer")
public class BizCustomer implements Serializable {

    /**
     * 客户ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 所属部门ID
     */
    private Long deptId;

    /**
     * 负责人用户ID
     */
    private Long ownerUserId;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 客户等级
     */
    private String customerLevel;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 备注
     */
    private String remark;

    /**
     * 逻辑删除：0否，1是
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
```

文件位置：`src/main/java/io/github/atengk/customer/dto/CustomerCreateDTO.java`

```java
package io.github.atengk.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 客户新增参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class CustomerCreateDTO implements Serializable {

    /**
     * 客户名称
     */
    @NotBlank(message = "客户名称不能为空")
    private String customerName;

    /**
     * 客户等级
     */
    private String customerLevel;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 备注
     */
    private String remark;
}
```

客户新增的核心点是：归属字段统一由后端写入。

```java
customer.setTenantId(LoginUserContext.getTenantId());
customer.setDeptId(LoginUserContext.getDeptId());
customer.setOwnerUserId(LoginUserContext.getUserId());
```

### 客户数据分页查询

分页查询使用自定义 Mapper 方法，并在 Mapper 方法上添加 `@DataPermission(tableAlias = "c")`。这样拦截器可以基于 `c.tenant_id`、`c.dept_id`、`c.owner_user_id` 自动追加权限条件。

文件位置：`src/main/java/io/github/atengk/customer/dto/CustomerPageQueryDTO.java`

```java
package io.github.atengk.customer.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 客户分页查询参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class CustomerPageQueryDTO implements Serializable {

    /**
     * 当前页
     */
    private Long current = 1L;

    /**
     * 每页数量
     */
    private Long size = 10L;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 客户等级
     */
    private String customerLevel;
}
```

文件位置：`src/main/java/io/github/atengk/customer/vo/CustomerVO.java`

```java
package io.github.atengk.customer.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客户响应对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class CustomerVO implements Serializable {

    /**
     * 客户ID
     */
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 所属部门ID
     */
    private Long deptId;

    /**
     * 负责人用户ID
     */
    private Long ownerUserId;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 客户等级
     */
    private String customerLevel;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
```

文件位置：`src/main/java/io/github/atengk/customer/mapper/BizCustomerMapper.java`

```java
package io.github.atengk.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.customer.dto.CustomerPageQueryDTO;
import io.github.atengk.customer.entity.BizCustomer;
import io.github.atengk.customer.vo.CustomerVO;
import io.github.atengk.permission.annotation.DataPermission;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 客户业务 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface BizCustomerMapper extends BaseMapper<BizCustomer> {

    /**
     * 分页查询客户列表
     *
     * @param page 分页参数
     * @param query 查询参数
     * @return 客户分页数据
     */
    @DataPermission(tableAlias = "c")
    Page<CustomerVO> selectCustomerPage(Page<CustomerVO> page, @Param("query") CustomerPageQueryDTO query);

    /**
     * 查询客户详情
     *
     * @param id 客户ID
     * @return 客户详情
     */
    @DataPermission(tableAlias = "c")
    CustomerVO selectCustomerDetail(@Param("id") Long id);

    /**
     * 管理员查询当前租户客户列表
     *
     * @param query 查询参数
     * @return 客户列表
     */
    @DataPermission(tableAlias = "c", ignoreOrg = true)
    List<CustomerVO> selectTenantCustomerList(@Param("query") CustomerPageQueryDTO query);

    /**
     * 平台管理员跨租户查询客户列表
     *
     * @param query 查询参数
     * @return 客户列表
     */
    @DataPermission(tableAlias = "c", ignoreTenant = true, ignoreOrg = true)
    List<CustomerVO> selectPlatformCustomerList(@Param("query") CustomerPageQueryDTO query);
}
```

文件位置：`src/main/resources/mapper/customer/BizCustomerMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="io.github.atengk.customer.mapper.BizCustomerMapper">

    <!-- 分页查询客户列表，数据权限条件由拦截器自动追加 -->
    <select id="selectCustomerPage" resultType="io.github.atengk.customer.vo.CustomerVO">
        SELECT
            c.id,
            c.tenant_id,
            c.dept_id,
            c.owner_user_id,
            c.customer_name,
            c.customer_level,
            c.contact_phone,
            c.remark,
            c.create_time
        FROM biz_customer c
        WHERE c.deleted = 0
        <if test="query.customerName != null and query.customerName != ''">
            AND c.customer_name LIKE CONCAT('%', #{query.customerName}, '%')
        </if>
        <if test="query.customerLevel != null and query.customerLevel != ''">
            AND c.customer_level = #{query.customerLevel}
        </if>
        ORDER BY c.create_time DESC
    </select>

    <!-- 查询客户详情，数据权限条件由拦截器自动追加 -->
    <select id="selectCustomerDetail" resultType="io.github.atengk.customer.vo.CustomerVO">
        SELECT
            c.id,
            c.tenant_id,
            c.dept_id,
            c.owner_user_id,
            c.customer_name,
            c.customer_level,
            c.contact_phone,
            c.remark,
            c.create_time
        FROM biz_customer c
        WHERE c.deleted = 0
          AND c.id = #{id}
        LIMIT 1
    </select>

    <!-- 租户管理员查询：只保留租户隔离，忽略组织权限 -->
    <select id="selectTenantCustomerList" resultType="io.github.atengk.customer.vo.CustomerVO">
        SELECT
            c.id,
            c.tenant_id,
            c.dept_id,
            c.owner_user_id,
            c.customer_name,
            c.customer_level,
            c.contact_phone,
            c.remark,
            c.create_time
        FROM biz_customer c
        WHERE c.deleted = 0
        <if test="query.customerName != null and query.customerName != ''">
            AND c.customer_name LIKE CONCAT('%', #{query.customerName}, '%')
        </if>
        <if test="query.customerLevel != null and query.customerLevel != ''">
            AND c.customer_level = #{query.customerLevel}
        </if>
        ORDER BY c.create_time DESC
    </select>

    <!-- 平台管理员查询：忽略租户隔离和组织权限，必须在 Service 层校验平台管理员角色 -->
    <select id="selectPlatformCustomerList" resultType="io.github.atengk.customer.vo.CustomerVO">
        SELECT
            c.id,
            c.tenant_id,
            c.dept_id,
            c.owner_user_id,
            c.customer_name,
            c.customer_level,
            c.contact_phone,
            c.remark,
            c.create_time
        FROM biz_customer c
        WHERE c.deleted = 0
        <if test="query.customerName != null and query.customerName != ''">
            AND c.customer_name LIKE CONCAT('%', #{query.customerName}, '%')
        </if>
        <if test="query.customerLevel != null and query.customerLevel != ''">
            AND c.customer_level = #{query.customerLevel}
        </if>
        ORDER BY c.create_time DESC
    </select>

</mapper>
```

### 客户数据详情查询

详情查询同样需要数据权限控制。不能因为用户知道某条数据的 `id`，就允许直接访问。

错误做法：

```java
BizCustomer customer = customerMapper.selectById(id);
```

这种写法只按主键查，不会自动限制当前用户是否可见该数据。

推荐做法是使用带数据权限注解的自定义详情查询：

```java
CustomerVO customer = customerMapper.selectCustomerDetail(id);
```

最终执行效果类似：

```sql
SELECT
    c.id,
    c.tenant_id,
    c.dept_id,
    c.owner_user_id,
    c.customer_name
FROM biz_customer c
WHERE c.deleted = 0
  AND c.id = 10001
  AND c.tenant_id = 1001
  AND (
      c.owner_user_id = 2001
      OR c.dept_id IN (10, 11, 12)
  )
LIMIT 1;
```

如果查询结果为空，表示数据不存在，或者当前用户无权访问。

### 管理员查询绕过

管理员绕过分两类：

| 场景                           | 处理方式                                                     |
| ------------------------------ | ------------------------------------------------------------ |
| 租户管理员查看当前租户全部客户 | `ignoreOrg = true`，保留 `tenant_id`                         |
| 平台管理员跨租户查看客户       | `ignoreTenant = true, ignoreOrg = true`，并且 Service 层强校验平台管理员角色 |

租户管理员查询当前租户全部客户时，仍然需要保留租户隔离：

```java
@DataPermission(tableAlias = "c", ignoreOrg = true)
List<CustomerVO> selectTenantCustomerList(@Param("query") CustomerPageQueryDTO query);
```

平台管理员跨租户查询必须额外校验角色，不建议只依赖注解放开：

```java
if (!LoginUserContext.hasRole("platform_admin")) {
    throw new IllegalStateException("仅平台管理员允许跨租户查询");
}
```

## 核心代码清单

本节汇总本案例最小可运行链路中的核心代码。前面已经给出部分基础类，下面补齐客户业务 Service 和 Controller，并整理所有关键文件的职责。

### 数据权限枚举

数据权限枚举用于统一角色数据范围编码，避免业务中直接散落 `1`、`2`、`3` 等魔法值。

文件位置：`src/main/java/io/github/atengk/permission/enums/DataScopeType.java`

```java
package io.github.atengk.permission.enums;

import cn.hutool.core.util.NumberUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据权限范围类型
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum DataScopeType {

    /**
     * 全部数据
     */
    ALL(1, "全部数据"),

    /**
     * 本部门及子部门数据
     */
    DEPT_AND_CHILD(2, "本部门及子部门数据"),

    /**
     * 本部门数据
     */
    DEPT_ONLY(3, "本部门数据"),

    /**
     * 本人数据
     */
    SELF_ONLY(4, "本人数据"),

    /**
     * 自定义部门数据
     */
    CUSTOM_DEPT(5, "自定义部门数据");

    private final Integer code;

    private final String desc;

    /**
     * 根据编码获取数据范围类型
     *
     * @param code 编码
     * @return 数据范围类型
     */
    public static DataScopeType of(Integer code) {
        for (DataScopeType item : values()) {
            if (NumberUtil.equals(item.getCode(), code)) {
                return item;
            }
        }
        throw new IllegalArgumentException("不支持的数据权限范围类型：" + code);
    }
}
```

### 登录用户模型

登录用户模型用于保存当前用户的数据权限上下文。登录成功后写入 Sa-Token Session，后续权限计算统一从这里读取。

文件位置：`src/main/java/io/github/atengk/auth/model/LoginUserInfo.java`

```java
package io.github.atengk.auth.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * 登录用户信息
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class LoginUserInfo implements Serializable {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 角色ID集合
     */
    private Set<Long> roleIds;

    /**
     * 角色编码集合
     */
    private Set<String> roleCodes;
}
```

### 数据权限注解

数据权限注解用于声明 Mapper 方法是否需要自动追加权限条件，以及业务表中对应的租户、部门、负责人字段。

文件位置：`src/main/java/io/github/atengk/permission/annotation/DataPermission.java`

```java
package io.github.atengk.permission.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {

    /**
     * 表别名
     *
     * @return 表别名
     */
    String tableAlias() default "";

    /**
     * 租户字段
     *
     * @return 租户字段
     */
    String tenantColumn() default "tenant_id";

    /**
     * 部门字段
     *
     * @return 部门字段
     */
    String deptColumn() default "dept_id";

    /**
     * 负责人字段
     *
     * @return 负责人字段
     */
    String ownerColumn() default "owner_user_id";

    /**
     * 是否忽略租户隔离
     *
     * @return 是否忽略
     */
    boolean ignoreTenant() default false;

    /**
     * 是否忽略组织权限
     *
     * @return 是否忽略
     */
    boolean ignoreOrg() default false;
}
```

### 数据权限服务

数据权限服务负责把当前用户的多个角色合并成统一的数据范围。这里给出接口和结果模型，完整实现可沿用前文的 `DataPermissionServiceImpl`。

文件位置：`src/main/java/io/github/atengk/permission/model/DataPermissionScope.java`

```java
package io.github.atengk.permission.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * 数据权限计算结果
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class DataPermissionScope implements Serializable {

    /**
     * 是否拥有全部数据权限
     */
    private Boolean allData = false;

    /**
     * 是否包含本人数据权限
     */
    private Boolean selfData = false;

    /**
     * 当前用户ID
     */
    private Long userId;

    /**
     * 当前租户ID
     */
    private Long tenantId;

    /**
     * 可见部门ID集合
     */
    private Set<Long> deptIds;
}
```

文件位置：`src/main/java/io/github/atengk/permission/service/DataPermissionService.java`

```java
package io.github.atengk.permission.service;

import io.github.atengk.permission.model.DataPermissionScope;

/**
 * 数据权限服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface DataPermissionService {

    /**
     * 获取当前用户数据权限范围
     *
     * @return 数据权限范围
     */
    DataPermissionScope getCurrentUserScope();

    /**
     * 清理指定用户数据权限缓存
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     */
    void clearUserScopeCache(Long tenantId, Long userId);
}
```

### MyBatis 数据权限拦截器

MyBatis 数据权限拦截器负责在 SQL 执行前识别 `@DataPermission` 注解，并调用 SQL 解析器追加权限条件。

文件位置：`src/main/java/io/github/atengk/permission/interceptor/DataPermissionInterceptor.java`

```java
package io.github.atengk.permission.interceptor;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.permission.annotation.DataPermission;
import io.github.atengk.permission.sql.DataPermissionSqlParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Properties;

/**
 * 数据权限 MyBatis 拦截器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class DataPermissionInterceptor implements Interceptor {

    private final DataPermissionSqlParser dataPermissionSqlParser;

    /**
     * 拦截 SQL 执行前的 prepare 阶段
     *
     * @param invocation 调用信息
     * @return 执行结果
     * @throws Throwable 异常
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MetaObject metaObject = getRealMetaObject(invocation.getTarget());

        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        if (mappedStatement.getSqlCommandType() != SqlCommandType.SELECT) {
            return invocation.proceed();
        }

        DataPermission dataPermission = findDataPermission(mappedStatement);
        if (dataPermission == null) {
            return invocation.proceed();
        }

        BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
        String originalSql = boundSql.getSql();
        String newSql = dataPermissionSqlParser.parse(originalSql, dataPermission);

        if (StrUtil.isNotBlank(newSql) && !StrUtil.equals(originalSql, newSql)) {
            metaObject.setValue("delegate.boundSql.sql", newSql);
            log.debug("数据权限 SQL 已改写：{}", newSql);
        }

        return invocation.proceed();
    }

    /**
     * 包装插件
     *
     * @param target 目标对象
     * @return 代理对象
     */
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    /**
     * 设置属性
     *
     * @param properties 属性
     */
    @Override
    public void setProperties(Properties properties) {
        // 当前拦截器暂无额外属性
    }

    /**
     * 获取真实 MetaObject
     *
     * @param target 目标对象
     * @return MetaObject
     */
    private MetaObject getRealMetaObject(Object target) {
        MetaObject metaObject = SystemMetaObject.forObject(target);

        while (metaObject.hasGetter("h")) {
            metaObject = SystemMetaObject.forObject(metaObject.getValue("h"));
        }

        while (metaObject.hasGetter("target")) {
            metaObject = SystemMetaObject.forObject(metaObject.getValue("target"));
        }

        return metaObject;
    }

    /**
     * 查找数据权限注解
     *
     * @param mappedStatement MappedStatement
     * @return 数据权限注解
     */
    private DataPermission findDataPermission(MappedStatement mappedStatement) {
        try {
            String statementId = mappedStatement.getId();
            int lastDotIndex = statementId.lastIndexOf(".");
            String className = statementId.substring(0, lastDotIndex);
            String methodName = statementId.substring(lastDotIndex + 1);

            Class<?> mapperClass = Class.forName(className);

            for (Method method : mapperClass.getMethods()) {
                if (StrUtil.equals(method.getName(), methodName) && method.isAnnotationPresent(DataPermission.class)) {
                    return method.getAnnotation(DataPermission.class);
                }
            }

            if (mapperClass.isAnnotationPresent(DataPermission.class)) {
                return mapperClass.getAnnotation(DataPermission.class);
            }

            return null;
        } catch (Exception e) {
            log.warn("解析数据权限注解失败：statementId={}", mappedStatement.getId(), e);
            return null;
        }
    }
}
```

### 客户业务 Mapper

客户 Mapper 是数据权限落地的入口。分页、详情、管理员查询都通过自定义 SQL 执行，避免直接使用无权限约束的 `selectById`。

文件位置：`src/main/java/io/github/atengk/customer/mapper/BizCustomerMapper.java`

```java
package io.github.atengk.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.customer.dto.CustomerPageQueryDTO;
import io.github.atengk.customer.entity.BizCustomer;
import io.github.atengk.customer.vo.CustomerVO;
import io.github.atengk.permission.annotation.DataPermission;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 客户业务 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface BizCustomerMapper extends BaseMapper<BizCustomer> {

    /**
     * 分页查询客户列表
     *
     * @param page 分页参数
     * @param query 查询参数
     * @return 客户分页数据
     */
    @DataPermission(tableAlias = "c")
    Page<CustomerVO> selectCustomerPage(Page<CustomerVO> page, @Param("query") CustomerPageQueryDTO query);

    /**
     * 查询客户详情
     *
     * @param id 客户ID
     * @return 客户详情
     */
    @DataPermission(tableAlias = "c")
    CustomerVO selectCustomerDetail(@Param("id") Long id);

    /**
     * 管理员查询当前租户客户列表
     *
     * @param query 查询参数
     * @return 客户列表
     */
    @DataPermission(tableAlias = "c", ignoreOrg = true)
    List<CustomerVO> selectTenantCustomerList(@Param("query") CustomerPageQueryDTO query);

    /**
     * 平台管理员跨租户查询客户列表
     *
     * @param query 查询参数
     * @return 客户列表
     */
    @DataPermission(tableAlias = "c", ignoreTenant = true, ignoreOrg = true)
    List<CustomerVO> selectPlatformCustomerList(@Param("query") CustomerPageQueryDTO query);
}
```

### 客户业务 Service

客户业务 Service 负责写入归属字段、调用带数据权限的 Mapper 方法，并在跨租户查询时进行管理员角色校验。

文件位置：`src/main/java/io/github/atengk/customer/service/BizCustomerService.java`

```java
package io.github.atengk.customer.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.customer.dto.CustomerCreateDTO;
import io.github.atengk.customer.dto.CustomerPageQueryDTO;
import io.github.atengk.customer.vo.CustomerVO;

import java.util.List;

/**
 * 客户业务服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface BizCustomerService {

    /**
     * 新增客户
     *
     * @param createDTO 新增参数
     * @return 客户ID
     */
    Long createCustomer(CustomerCreateDTO createDTO);

    /**
     * 分页查询客户
     *
     * @param query 查询参数
     * @return 客户分页数据
     */
    Page<CustomerVO> pageCustomer(CustomerPageQueryDTO query);

    /**
     * 查询客户详情
     *
     * @param id 客户ID
     * @return 客户详情
     */
    CustomerVO getCustomerDetail(Long id);

    /**
     * 租户管理员查询当前租户客户
     *
     * @param query 查询参数
     * @return 客户列表
     */
    List<CustomerVO> listTenantCustomer(CustomerPageQueryDTO query);

    /**
     * 平台管理员跨租户查询客户
     *
     * @param query 查询参数
     * @return 客户列表
     */
    List<CustomerVO> listPlatformCustomer(CustomerPageQueryDTO query);
}
```

文件位置：`src/main/java/io/github/atengk/customer/service/impl/BizCustomerServiceImpl.java`

```java
package io.github.atengk.customer.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.auth.context.LoginUserContext;
import io.github.atengk.customer.dto.CustomerCreateDTO;
import io.github.atengk.customer.dto.CustomerPageQueryDTO;
import io.github.atengk.customer.entity.BizCustomer;
import io.github.atengk.customer.mapper.BizCustomerMapper;
import io.github.atengk.customer.service.BizCustomerService;
import io.github.atengk.customer.vo.CustomerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 客户业务服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BizCustomerServiceImpl implements BizCustomerService {

    private static final String PLATFORM_ADMIN = "platform_admin";

    private static final String TENANT_ADMIN = "tenant_admin";

    private final BizCustomerMapper bizCustomerMapper;

    /**
     * 新增客户
     *
     * @param createDTO 新增参数
     * @return 客户ID
     */
    @Override
    public Long createCustomer(CustomerCreateDTO createDTO) {
        BizCustomer customer = BeanUtil.copyProperties(createDTO, BizCustomer.class);

        customer.setTenantId(LoginUserContext.getTenantId());
        customer.setDeptId(LoginUserContext.getDeptId());
        customer.setOwnerUserId(LoginUserContext.getUserId());
        customer.setDeleted(0);

        bizCustomerMapper.insert(customer);

        log.info("新增客户成功：customerId={}, tenantId={}, ownerUserId={}",
                customer.getId(), customer.getTenantId(), customer.getOwnerUserId());
        return customer.getId();
    }

    /**
     * 分页查询客户
     *
     * @param query 查询参数
     * @return 客户分页数据
     */
    @Override
    public Page<CustomerVO> pageCustomer(CustomerPageQueryDTO query) {
        Page<CustomerVO> page = Page.of(query.getCurrent(), query.getSize());
        return bizCustomerMapper.selectCustomerPage(page, query);
    }

    /**
     * 查询客户详情
     *
     * @param id 客户ID
     * @return 客户详情
     */
    @Override
    public CustomerVO getCustomerDetail(Long id) {
        CustomerVO customer = bizCustomerMapper.selectCustomerDetail(id);
        if (ObjectUtil.isNull(customer)) {
            log.warn("客户不存在或无权访问：customerId={}, userId={}", id, LoginUserContext.getUserId());
            throw new IllegalArgumentException("客户不存在或无权访问");
        }
        return customer;
    }

    /**
     * 租户管理员查询当前租户客户
     *
     * @param query 查询参数
     * @return 客户列表
     */
    @Override
    public List<CustomerVO> listTenantCustomer(CustomerPageQueryDTO query) {
        if (!LoginUserContext.hasRole(TENANT_ADMIN) && !LoginUserContext.hasRole(PLATFORM_ADMIN)) {
            log.warn("非管理员尝试查询租户客户全量数据：userId={}", LoginUserContext.getUserId());
            throw new IllegalStateException("仅管理员允许查询当前租户全部客户");
        }
        return bizCustomerMapper.selectTenantCustomerList(query);
    }

    /**
     * 平台管理员跨租户查询客户
     *
     * @param query 查询参数
     * @return 客户列表
     */
    @Override
    public List<CustomerVO> listPlatformCustomer(CustomerPageQueryDTO query) {
        if (!LoginUserContext.hasRole(PLATFORM_ADMIN)) {
            log.warn("非平台管理员尝试跨租户查询客户：userId={}", LoginUserContext.getUserId());
            throw new IllegalStateException("仅平台管理员允许跨租户查询客户");
        }
        return bizCustomerMapper.selectPlatformCustomerList(query);
    }
}
```

### 客户业务 Controller

Controller 提供客户新增、分页查询、详情查询和管理员查询接口。普通查询接口不需要显式传入租户和部门信息。

文件位置：`src/main/java/io/github/atengk/customer/controller/BizCustomerController.java`

```java
package io.github.atengk.customer.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.customer.dto.CustomerCreateDTO;
import io.github.atengk.customer.dto.CustomerPageQueryDTO;
import io.github.atengk.customer.service.BizCustomerService;
import io.github.atengk.customer.vo.CustomerVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客户业务接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers")
public class BizCustomerController {

    private final BizCustomerService bizCustomerService;

    /**
     * 新增客户
     *
     * @param createDTO 新增参数
     * @return 客户ID
     */
    @PostMapping
    public Long createCustomer(@Valid @RequestBody CustomerCreateDTO createDTO) {
        return bizCustomerService.createCustomer(createDTO);
    }

    /**
     * 分页查询客户
     *
     * @param query 查询参数
     * @return 客户分页数据
     */
    @GetMapping
    public Page<CustomerVO> pageCustomer(CustomerPageQueryDTO query) {
        return bizCustomerService.pageCustomer(query);
    }

    /**
     * 查询客户详情
     *
     * @param id 客户ID
     * @return 客户详情
     */
    @GetMapping("/{id}")
    public CustomerVO getCustomerDetail(@PathVariable Long id) {
        return bizCustomerService.getCustomerDetail(id);
    }

    /**
     * 租户管理员查询当前租户客户
     *
     * @param query 查询参数
     * @return 客户列表
     */
    @GetMapping("/admin/tenant")
    public List<CustomerVO> listTenantCustomer(CustomerPageQueryDTO query) {
        return bizCustomerService.listTenantCustomer(query);
    }

    /**
     * 平台管理员跨租户查询客户
     *
     * @param query 查询参数
     * @return 客户列表
     */
    @GetMapping("/admin/platform")
    public List<CustomerVO> listPlatformCustomer(CustomerPageQueryDTO query) {
        return bizCustomerService.listPlatformCustomer(query);
    }
}
```

接口调用示例：

```bash
# 新增客户
curl -X POST 'http://localhost:8080/api/customers' \
  -H 'Authorization: your-token' \
  -H 'Content-Type: application/json' \
  -d '{
    "customerName": "杭州示例科技有限公司",
    "customerLevel": "A",
    "contactPhone": "13800000000",
    "remark": "重点客户"
  }'

# 普通分页查询，自动追加租户和组织权限
curl -X GET 'http://localhost:8080/api/customers?current=1&size=10&customerName=示例' \
  -H 'Authorization: your-token'

# 查询详情，自动校验当前用户是否可见该客户
curl -X GET 'http://localhost:8080/api/customers/10001' \
  -H 'Authorization: your-token'

# 租户管理员查询当前租户全部客户
curl -X GET 'http://localhost:8080/api/customers/admin/tenant' \
  -H 'Authorization: tenant-admin-token'

# 平台管理员跨租户查询客户
curl -X GET 'http://localhost:8080/api/customers/admin/platform' \
  -H 'Authorization: platform-admin-token'
```

核心执行链路如下：

```text
Controller 接收请求
-> Service 处理业务
-> Mapper 执行查询
-> DataPermissionInterceptor 拦截 SQL
-> DataPermissionService 计算当前用户权限范围
-> DataPermissionSqlParser 追加 tenant_id / dept_id / owner_user_id 条件
-> 数据库返回当前用户可见数据
```

## 接口测试

接口测试用于验证数据权限是否真正生效。重点不是接口能否返回数据，而是不同用户登录后，同一个客户查询接口返回的数据范围是否正确。测试覆盖本人数据、本部门数据、本部门及子部门数据、自定义部门数据和租户隔离。

### 初始化测试数据

下面初始化一组最小测试数据，方便直接验证不同数据权限范围的查询效果。

测试用户设计如下：

| 用户ID | 用户名            | 租户ID | 部门ID | 角色           | 数据范围           |
| ------ | ----------------- | ------ | ------ | -------------- | ------------------ |
| 2001   | sales_a           | 1001   | 11     | 普通销售       | 本人数据           |
| 2002   | sales_b           | 1001   | 11     | 普通销售       | 本人数据           |
| 2003   | dept_manager      | 1001   | 11     | 部门主管       | 本部门数据         |
| 2004   | area_manager      | 1001   | 10     | 区域负责人     | 本部门及子部门数据 |
| 2005   | custom_user       | 1001   | 20     | 自定义权限用户 | 自定义部门数据     |
| 2006   | tenant_admin      | 1001   | 10     | 租户管理员     | 全部数据           |
| 3001   | other_tenant_user | 1002   | 110    | 其他租户用户   | 全部数据           |

测试部门设计如下：

| 部门ID | 租户ID | 父部门ID | 部门名称     | ancestors |
| ------ | ------ | -------- | ------------ | --------- |
| 10     | 1001   | 0        | 华东大区     | 0         |
| 11     | 1001   | 10       | 杭州销售部   | 0,10      |
| 12     | 1001   | 11       | 杭州一组     | 0,10,11   |
| 20     | 1001   | 0        | 华南大区     | 0         |
| 30     | 1001   | 20       | 深圳销售部   | 0,20      |
| 31     | 1001   | 30       | 深圳一组     | 0,20,30   |
| 110    | 1002   | 0        | 其他租户部门 | 0         |

先清理测试数据：

```sql
DELETE FROM biz_customer;
DELETE FROM sys_role_data_scope;
DELETE FROM sys_user_role;
DELETE FROM sys_role;
DELETE FROM sys_dept;
DELETE FROM sys_user;
```

初始化用户、角色、部门和客户数据：

```sql
-- 用户数据
INSERT INTO sys_user
(id, tenant_id, dept_id, username, nickname, password, status, deleted)
VALUES
(2001, 1001, 11, 'sales_a', '销售A', '$2a$10$test', 1, 0),
(2002, 1001, 11, 'sales_b', '销售B', '$2a$10$test', 1, 0),
(2003, 1001, 11, 'dept_manager', '部门主管', '$2a$10$test', 1, 0),
(2004, 1001, 10, 'area_manager', '区域负责人', '$2a$10$test', 1, 0),
(2005, 1001, 20, 'custom_user', '自定义权限用户', '$2a$10$test', 1, 0),
(2006, 1001, 10, 'tenant_admin', '租户管理员', '$2a$10$test', 1, 0),
(3001, 1002, 110, 'other_tenant_user', '其他租户用户', '$2a$10$test', 1, 0);

-- 角色数据
INSERT INTO sys_role
(id, tenant_id, role_code, role_name, data_scope, status, deleted)
VALUES
(101, 1001, 'sales_user', '普通销售', 4, 1, 0),
(102, 1001, 'dept_manager', '部门主管', 3, 1, 0),
(103, 1001, 'area_manager', '区域负责人', 2, 1, 0),
(104, 1001, 'custom_scope_user', '自定义数据权限用户', 5, 1, 0),
(105, 1001, 'tenant_admin', '租户管理员', 1, 1, 0),
(201, 1002, 'tenant_admin', '其他租户管理员', 1, 1, 0);

-- 用户角色关系
INSERT INTO sys_user_role
(id, tenant_id, user_id, role_id)
VALUES
(10001, 1001, 2001, 101),
(10002, 1001, 2002, 101),
(10003, 1001, 2003, 102),
(10004, 1001, 2004, 103),
(10005, 1001, 2005, 104),
(10006, 1001, 2006, 105),
(20001, 1002, 3001, 201);

-- 部门数据
INSERT INTO sys_dept
(id, tenant_id, parent_id, dept_name, ancestors, sort, status, deleted)
VALUES
(10, 1001, 0, '华东大区', '0', 1, 1, 0),
(11, 1001, 10, '杭州销售部', '0,10', 1, 1, 0),
(12, 1001, 11, '杭州一组', '0,10,11', 1, 1, 0),
(20, 1001, 0, '华南大区', '0', 2, 1, 0),
(30, 1001, 20, '深圳销售部', '0,20', 1, 1, 0),
(31, 1001, 30, '深圳一组', '0,20,30', 1, 1, 0),
(110, 1002, 0, '其他租户部门', '0', 1, 1, 0);

-- 自定义部门权限：角色 104 可以查看深圳销售部和深圳一组
INSERT INTO sys_role_data_scope
(id, tenant_id, role_id, dept_id)
VALUES
(1, 1001, 104, 30),
(2, 1001, 104, 31);

-- 客户业务数据
INSERT INTO biz_customer
(id, tenant_id, dept_id, owner_user_id, customer_name, customer_level, contact_phone, remark, deleted)
VALUES
(50001, 1001, 11, 2001, '杭州客户A', 'A', '13800000001', '销售A负责', 0),
(50002, 1001, 11, 2002, '杭州客户B', 'B', '13800000002', '销售B负责', 0),
(50003, 1001, 12, 2001, '杭州一组客户C', 'A', '13800000003', '杭州一组客户', 0),
(50004, 1001, 30, 2005, '深圳客户D', 'A', '13800000004', '自定义部门客户', 0),
(50005, 1001, 31, 2005, '深圳一组客户E', 'B', '13800000005', '自定义子部门客户', 0),
(50006, 1001, 20, 2005, '华南客户F', 'C', '13800000006', '不在自定义部门范围内', 0),
(60001, 1002, 110, 3001, '其他租户客户X', 'A', '13900000001', '其他租户数据', 0);
```

测试接口统一使用：

```bash
curl -X GET 'http://localhost:8080/api/customers?current=1&size=20' \
  -H 'Authorization: 替换为对应用户Token'
```

### 本人数据验证

本人数据验证使用 `sales_a` 登录。该用户的数据范围是“本人数据”，所以只能看到 `owner_user_id = 2001` 的客户。

登录用户信息：

```text
userId = 2001
tenantId = 1001
deptId = 11
role = sales_user
dataScope = 本人数据
```

预期权限条件：

```sql
c.tenant_id = 1001
AND (
    c.owner_user_id = 2001
)
```

预期可见数据：

| 客户ID | 客户名称      | 部门ID | 负责人 |
| ------ | ------------- | ------ | ------ |
| 50001  | 杭州客户A     | 11     | 2001   |
| 50003  | 杭州一组客户C | 12     | 2001   |

不应该看到的数据：

| 客户ID | 客户名称      | 原因               |
| ------ | ------------- | ------------------ |
| 50002  | 杭州客户B     | 负责人不是当前用户 |
| 50004  | 深圳客户D     | 负责人不是当前用户 |
| 60001  | 其他租户客户X | 租户不同           |

调用示例：

```bash
curl -X GET 'http://localhost:8080/api/customers?current=1&size=20' \
  -H 'Authorization: sales-a-token'
```

详情越权验证：

```bash
curl -X GET 'http://localhost:8080/api/customers/50002' \
  -H 'Authorization: sales-a-token'
```

预期结果：

```json
{
  "message": "客户不存在或无权访问"
}
```

### 本部门数据验证

本部门数据验证使用 `dept_manager` 登录。该用户的数据范围是“本部门数据”，所以只能看到 `dept_id = 11` 的客户，不包含子部门 `12`。

登录用户信息：

```text
userId = 2003
tenantId = 1001
deptId = 11
role = dept_manager
dataScope = 本部门数据
```

预期权限条件：

```sql
c.tenant_id = 1001
AND (
    c.dept_id IN (11)
)
```

预期可见数据：

| 客户ID | 客户名称  | 部门ID | 负责人 |
| ------ | --------- | ------ | ------ |
| 50001  | 杭州客户A | 11     | 2001   |
| 50002  | 杭州客户B | 11     | 2002   |

不应该看到的数据：

| 客户ID | 客户名称      | 原因                     |
| ------ | ------------- | ------------------------ |
| 50003  | 杭州一组客户C | 属于子部门，不属于本部门 |
| 50004  | 深圳客户D     | 属于其他部门             |
| 60001  | 其他租户客户X | 租户不同                 |

调用示例：

```bash
curl -X GET 'http://localhost:8080/api/customers?current=1&size=20' \
  -H 'Authorization: dept-manager-token'
```

详情验证：

```bash
# 可以访问本部门客户
curl -X GET 'http://localhost:8080/api/customers/50001' \
  -H 'Authorization: dept-manager-token'

# 不能访问子部门客户
curl -X GET 'http://localhost:8080/api/customers/50003' \
  -H 'Authorization: dept-manager-token'
```

### 本部门及子部门数据验证

本部门及子部门数据验证使用 `area_manager` 登录。该用户所属部门是 `10`，数据范围是“本部门及子部门数据”，所以可以看到部门 `10`、`11`、`12` 下的数据。

登录用户信息：

```text
userId = 2004
tenantId = 1001
deptId = 10
role = area_manager
dataScope = 本部门及子部门数据
```

根据部门树：

```text
10 华东大区
└── 11 杭州销售部
    └── 12 杭州一组
```

预期权限条件：

```sql
c.tenant_id = 1001
AND (
    c.dept_id IN (10, 11, 12)
)
```

预期可见数据：

| 客户ID | 客户名称      | 部门ID | 负责人 |
| ------ | ------------- | ------ | ------ |
| 50001  | 杭州客户A     | 11     | 2001   |
| 50002  | 杭州客户B     | 11     | 2002   |
| 50003  | 杭州一组客户C | 12     | 2001   |

不应该看到的数据：

| 客户ID | 客户名称      | 原因             |
| ------ | ------------- | ---------------- |
| 50004  | 深圳客户D     | 不属于当前部门树 |
| 50005  | 深圳一组客户E | 不属于当前部门树 |
| 60001  | 其他租户客户X | 租户不同         |

调用示例：

```bash
curl -X GET 'http://localhost:8080/api/customers?current=1&size=20' \
  -H 'Authorization: area-manager-token'
```

可以通过 SQL 日志观察拦截器追加后的条件，效果类似：

```sql
SELECT
    c.id,
    c.tenant_id,
    c.dept_id,
    c.owner_user_id,
    c.customer_name,
    c.customer_level,
    c.contact_phone,
    c.remark,
    c.create_time
FROM biz_customer c
WHERE c.deleted = 0
  AND c.tenant_id = 1001
  AND (c.dept_id IN (10, 11, 12))
ORDER BY c.create_time DESC;
```

### 自定义部门数据验证

自定义部门数据验证使用 `custom_user` 登录。该用户虽然所属部门是 `20`，但角色绑定的自定义部门是 `30` 和 `31`，所以只能看到深圳销售部和深圳一组的数据。

登录用户信息：

```text
userId = 2005
tenantId = 1001
deptId = 20
role = custom_scope_user
dataScope = 自定义部门数据
```

角色自定义部门配置：

```text
roleId = 104
deptIds = 30, 31
```

预期权限条件：

```sql
c.tenant_id = 1001
AND (
    c.dept_id IN (30, 31)
)
```

预期可见数据：

| 客户ID | 客户名称      | 部门ID | 负责人 |
| ------ | ------------- | ------ | ------ |
| 50004  | 深圳客户D     | 30     | 2005   |
| 50005  | 深圳一组客户E | 31     | 2005   |

不应该看到的数据：

| 客户ID | 客户名称      | 原因                            |
| ------ | ------------- | ------------------------------- |
| 50006  | 华南客户F     | 部门为 20，不在自定义部门范围内 |
| 50001  | 杭州客户A     | 不在自定义部门范围内            |
| 60001  | 其他租户客户X | 租户不同                        |

调用示例：

```bash
curl -X GET 'http://localhost:8080/api/customers?current=1&size=20' \
  -H 'Authorization: custom-user-token'
```

详情验证：

```bash
# 可以访问自定义部门客户
curl -X GET 'http://localhost:8080/api/customers/50004' \
  -H 'Authorization: custom-user-token'

# 不能访问非自定义部门客户
curl -X GET 'http://localhost:8080/api/customers/50006' \
  -H 'Authorization: custom-user-token'
```

### 租户隔离验证

租户隔离验证的目标是确认不同租户之间的数据不会互相泄露。即使用户拥有“全部数据”权限，也只能查看当前租户下的数据，除非走平台管理员专用接口。

使用 `tenant_admin` 登录：

```text
userId = 2006
tenantId = 1001
deptId = 10
role = tenant_admin
dataScope = 全部数据
```

预期权限条件：

```sql
c.tenant_id = 1001
```

预期可见数据：

| 客户ID | 客户名称      | 租户ID |
| ------ | ------------- | ------ |
| 50001  | 杭州客户A     | 1001   |
| 50002  | 杭州客户B     | 1001   |
| 50003  | 杭州一组客户C | 1001   |
| 50004  | 深圳客户D     | 1001   |
| 50005  | 深圳一组客户E | 1001   |
| 50006  | 华南客户F     | 1001   |

不应该看到的数据：

| 客户ID | 客户名称      | 原因             |
| ------ | ------------- | ---------------- |
| 60001  | 其他租户客户X | tenant_id = 1002 |

调用普通分页接口：

```bash
curl -X GET 'http://localhost:8080/api/customers?current=1&size=20' \
  -H 'Authorization: tenant-admin-token'
```

调用租户管理员接口：

```bash
curl -X GET 'http://localhost:8080/api/customers/admin/tenant' \
  -H 'Authorization: tenant-admin-token'
```

两个接口都不能返回 `tenant_id = 1002` 的客户。

如果使用 `other_tenant_user` 登录：

```text
userId = 3001
tenantId = 1002
deptId = 110
role = tenant_admin
dataScope = 全部数据
```

调用接口：

```bash
curl -X GET 'http://localhost:8080/api/customers?current=1&size=20' \
  -H 'Authorization: other-tenant-user-token'
```

预期只能看到：

| 客户ID | 客户名称      | 租户ID |
| ------ | ------------- | ------ |
| 60001  | 其他租户客户X | 1002   |

平台管理员跨租户接口必须单独验证。只有拥有 `platform_admin` 角色时才允许调用：

```bash
curl -X GET 'http://localhost:8080/api/customers/admin/platform' \
  -H 'Authorization: platform-admin-token'
```

非平台管理员调用时，预期返回：

```json
{
  "message": "仅平台管理员允许跨租户查询客户"
}
```

## 实现小结

本案例完成了一个可落地的数据权限与组织隔离核心实现。核心思路是：业务接口不直接拼权限 SQL，而是由登录上下文、权限计算服务和 MyBatis 拦截器统一处理。

### 核心流程回顾

完整执行链路如下：

```text
用户登录
-> Sa-Token 保存登录态
-> Session 写入 userId、tenantId、deptId、roleIds、roleCodes
-> 调用客户列表接口
-> Mapper 方法命中 @DataPermission
-> MyBatis 拦截器拦截 SELECT SQL
-> DataPermissionService 计算当前用户数据范围
-> 优先读取 Redis 权限缓存
-> 缓存不存在时查询角色、部门树、自定义部门权限
-> 生成 DataPermissionScope
-> JSqlParser 改写 SQL
-> 自动追加 tenant_id、dept_id、owner_user_id 条件
-> 数据库返回当前用户可见数据
```

本案例中的关键控制点如下：

| 控制点       | 实现方式                                     |
| ------------ | -------------------------------------------- |
| 当前用户身份 | Sa-Token 登录态                              |
| 租户隔离     | 自动追加 `tenant_id = 当前租户ID`            |
| 本人数据     | 自动追加 `owner_user_id = 当前用户ID`        |
| 部门数据     | 自动追加 `dept_id IN 可见部门ID集合`         |
| 多角色合并   | 取权限并集                                   |
| 管理员绕过   | 租户管理员绕过组织权限，平台管理员走专用接口 |
| SQL 注入控制 | 权限字段后端固定，ID 来源于服务端和数据库    |
| 权限缓存     | Redis 缓存用户最终权限范围                   |
| 越权详情访问 | 详情查询也使用带权限注解的 Mapper 方法       |

本实现的落地原则是：

```text
新增数据时：
归属字段由后端写入，不信任前端传参。

查询列表时：
业务代码只写业务查询条件，权限条件由拦截器追加。

查询详情时：
不能直接 selectById，必须走带数据权限的查询方法。

管理员查询时：
只在明确接口中绕过权限，并在 Service 层校验管理员角色。

SQL 改写失败时：
直接阻断查询，不放行原 SQL。
```

### 可扩展点

当前实现是核心版本，适合中小型后台系统和 SaaS 业务的基础数据权限控制。后续可以按业务复杂度继续扩展。

| 扩展方向           | 说明                                             |
| ------------------ | ------------------------------------------------ |
| 支持多部门用户     | 用户可同时归属多个部门，计算多个主部门的数据范围 |
| 支持岗位权限       | 在部门维度之外叠加岗位、职级、区域等权限         |
| 支持数据权限表达式 | 将角色权限配置为表达式，例如区域、渠道、业务线   |
| 支持字段级权限     | 不同角色查看不同字段，例如手机号脱敏、金额隐藏   |
| 支持行级数据授权   | 针对单条业务数据授权给指定用户或角色             |
| 支持复杂 SQL       | 增强 JSqlParser，处理 UNION、子查询、JOIN 多主表 |
| 支持权限版本号     | 用户权限变更时更新版本号，缓存 Key 带版本失效    |
| 支持网关租户上下文 | 微服务场景下通过网关透传 tenantId 和 userId      |
| 支持报表权限视图   | 对复杂报表单独设计权限视图或中间表               |
| 支持操作审计       | 记录用户查询、导出、越权访问等审计日志           |

实际项目中，建议按以下优先级扩展：

```text
第一阶段：
完成租户隔离 + 部门数据权限 + 本人数据权限。

第二阶段：
补充 Redis 缓存失效、权限变更刷新、详情接口防越权。

第三阶段：
支持自定义部门、多角色合并、管理员专用接口。

第四阶段：
处理复杂 SQL、报表查询、导出权限、字段脱敏。

第五阶段：
接入操作审计、权限变更留痕、异常访问告警。
```

最终建议保留两条工程约束：

```text
所有需要数据权限的业务表必须包含 tenant_id、dept_id、owner_user_id。

所有需要数据权限的查询必须走带 @DataPermission 的 Mapper 方法，禁止直接使用无权限约束的 selectById 查询业务详情。
```