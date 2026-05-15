# 统一认证授权与 Token 会话管理

本文基于「Java 后端经典高含金量业务场景功能 30 个」中第 21 个业务场景展开，原始场景要求覆盖登录认证、Token 生成、会话保存、接口鉴权、权限校验、Token 续期和退出登录等核心能力。

## 案例目标

本案例实现一个可直接迁移到后台系统、SaaS 系统或微服务平台中的统一认证授权模块。重点不放在复杂账号体系，而是把登录、Token、权限、会话、网关鉴权、用户上下文透传这些后端核心能力打通。

核心流程如下：

```text
用户登录
-> 校验账号密码
-> 生成登录 Token
-> 将用户会话写入 Redis
-> 前端携带 Token 请求接口
-> 网关统一校验 Token
-> 业务服务解析用户上下文
-> 接口执行权限校验
-> Token 自动续期
-> 用户退出登录或管理员踢人下线
-> 清理 Token 会话
```

这个案例最终实现的是一个「小而完整」的认证授权闭环，而不是完整 IAM 平台。

### 实现功能边界

本案例实现以下核心功能：

| 功能             | 是否实现 | 说明                                     |
| ---------------- | -------- | ---------------------------------------- |
| 用户登录         | 实现     | 使用账号密码登录                         |
| 密码加密校验     | 实现     | 使用 Hutool `SecureUtil` 做摘要示例      |
| Token 生成       | 实现     | 使用 Sa-Token 管理登录态                 |
| Redis 会话存储   | 实现     | Sa-Token 会话数据落 Redis                |
| 当前用户信息获取 | 实现     | 提供 `/auth/profile` 接口                |
| 权限码加载       | 实现     | 从数据库或模拟权限集合加载               |
| 接口权限校验     | 实现     | 使用 `@SaCheckPermission`                |
| 角色校验         | 实现     | 使用 `@SaCheckRole`                      |
| Token 续期       | 实现     | 登录有效期 + 活跃续期                    |
| 退出登录         | 实现     | 清理当前 Token                           |
| 踢人下线         | 实现     | 管理员按用户 ID 强制下线                 |
| 网关统一鉴权     | 实现     | Gateway Filter 校验 Token                |
| 用户上下文透传   | 实现     | 网关向下游服务传递用户 ID、账号、租户 ID |
| 多端登录控制     | 实现     | 通过 Sa-Token 配置控制是否允许并发登录   |

本案例暂不实现以下扩展功能，避免文档过重：

| 功能                        | 不实现原因                             |
| --------------------------- | -------------------------------------- |
| 用户注册                    | 和 Token 会话管理不是同一重点          |
| 图片验证码 / 短信验证码     | 属于登录安全增强，不影响核心链路       |
| OAuth2 / 第三方登录         | 场景复杂度较高，后续可单独扩展         |
| 菜单权限管理页面            | 偏前端和后台配置，不影响后端鉴权主流程 |
| 复杂组织数据权限            | 已属于「数据权限与组织隔离」专项       |
| refresh_token 双 Token 模型 | 本案例优先使用 Sa-Token 活跃续期方案   |

### 技术栈选型

本案例采用 Spring Boot 3 + Sa-Token + Redis + MyBatis-Plus 的组合，原因是实现成本低、代码侵入小、会话控制能力完整，适合后台系统和微服务平台快速落地。

| 技术                 | 用途               | 说明                                      |
| -------------------- | ------------------ | ----------------------------------------- |
| Spring Boot 3        | 基础开发框架       | 提供 Web、配置、依赖管理能力              |
| Sa-Token             | 登录认证与权限校验 | 负责 Token、登录态、权限注解、踢人下线    |
| Redis                | 会话与权限缓存     | 保存 Token 会话、权限缓存、用户上下文     |
| MyBatis-Plus         | 数据访问           | 查询用户、角色、权限数据                  |
| MySQL                | 业务数据库         | 存储用户、角色、权限、关联关系            |
| Spring Cloud Gateway | 网关鉴权           | 在微服务入口统一校验 Token                |
| OpenFeign            | 服务间调用         | 透传用户上下文请求头                      |
| Hutool               | 工具类             | 密码摘要、字符串判断、集合处理、JSON 处理 |
| Lombok               | 简化实体代码       | 减少 Getter、Setter、构造方法样板代码     |

最终选型如下：

```text
认证授权核心：Sa-Token
Token 存储方式：Redis
权限模型：RBAC，用户 -> 角色 -> 权限
接口鉴权方式：注解鉴权 + 网关前置鉴权
上下文传递方式：HTTP Header 透传 userId、username、tenantId
密码处理方式：Hutool SecureUtil 示例，生产建议替换为 BCrypt
```

本案例推荐的模块拆分如下：

```text
auth-service
  负责登录、退出、踢人、用户信息、权限加载

gateway-service
  负责统一 Token 校验、白名单放行、用户上下文透传

business-service
  负责业务接口，使用注解进行接口权限控制
```

单体项目可以只保留 `auth-service` 内的认证代码；微服务项目建议增加 `gateway-service`，把 Token 校验前移到网关层。

## 项目结构

本案例按微服务项目拆分，核心包含认证服务、网关鉴权服务和公共上下文模块。单体项目可以只保留 `auth-service` 和 `auth-common`，把网关相关代码省略。

```text
auth-token-demo
├── pom.xml
├── auth-common
│   ├── pom.xml
│   └── src/main/java/io/github/atengk/auth/common
│       ├── context
│       │   ├── LoginUser.java
│       │   └── UserContextHolder.java
│       └── constant
│           └── AuthHeaderConstant.java
├── auth-service
│   ├── pom.xml
│   └── src/main/java/io/github/atengk/auth
│       ├── AuthApplication.java
│       ├── controller
│       │   └── AuthController.java
│       ├── service
│       │   ├── AuthService.java
│       │   └── impl/AuthServiceImpl.java
│       ├── satoken
│       │   └── StpPermissionImpl.java
│       ├── entity
│       │   ├── SysUser.java
│       │   ├── SysRole.java
│       │   ├── SysPermission.java
│       │   ├── SysUserRole.java
│       │   └── SysRolePermission.java
│       ├── mapper
│       │   ├── SysUserMapper.java
│       │   ├── SysRoleMapper.java
│       │   └── SysPermissionMapper.java
│       └── model
│           ├── dto/LoginRequest.java
│           └── vo/LoginResponse.java
└── gateway-service
    ├── pom.xml
    └── src/main/java/io/github/atengk/gateway
        ├── GatewayApplication.java
        ├── config
        │   └── GatewayWhiteListProperties.java
        └── filter
            └── TokenAuthGlobalFilter.java
```

### 认证服务模块

认证服务负责登录、退出、踢人下线、当前用户信息查询、权限码加载。业务接口不直接生成 Token，只依赖认证服务颁发和维护登录态。

核心职责如下：

```text
auth-service
-> 校验账号密码
-> 调用 Sa-Token 完成登录
-> 写入 Token 会话信息
-> 查询用户角色和权限
-> 提供当前登录用户接口
-> 支持退出登录和管理员踢人
```

本模块会直接连接 MySQL 和 Redis。MySQL 存用户、角色、权限基础数据，Redis 存 Token 会话、权限缓存和登录状态。

### 网关鉴权模块

网关鉴权模块负责在请求进入业务服务前统一校验 Token，避免每个业务服务重复写 Token 解析逻辑。它只做入口层面的认证判断，不替代业务服务内部的精细权限注解。

核心职责如下：

```text
gateway-service
-> 放行登录、健康检查等白名单接口
-> 从请求头读取 Token
-> 校验 Token 是否有效
-> 查询 Token 对应的登录用户
-> 将用户信息写入请求头
-> 转发到下游业务服务
```

建议网关只做「是否登录」校验，具体接口是否拥有权限仍由业务服务通过 `@SaCheckPermission` 或 `@SaCheckRole` 判断。这样可以避免网关维护过多业务权限规则。

### 用户上下文模块

用户上下文模块用于在多个服务之间统一传递当前用户信息。网关解析 Token 后，把用户 ID、账号、租户 ID 等信息写入请求头，下游服务再通过拦截器读取并放入 `ThreadLocal`。

核心字段建议如下：

```text
userId      当前登录用户 ID
username    当前登录账号
tenantId    当前租户 ID
token       当前请求 Token
```

公共请求头统一定义如下：

```text
X-User-Id       用户 ID
X-Username      用户账号
X-Tenant-Id     租户 ID
Authorization   登录 Token
```

后续业务服务中不要重复解析 Token，优先从用户上下文中获取当前用户信息。

## 环境准备

本案例基于原始 README 中推荐的 Spring Boot、Spring Cloud Gateway、Redis、Sa-Token、JWT、OpenFeign、Nacos、MyBatis-Plus 等技术栈展开，聚焦统一认证授权与 Token 会话管理的核心落地。

基础环境建议如下：

| 组件         | 建议版本         | 用途                   |
| ------------ | ---------------- | ---------------------- |
| JDK          | 17+              | Spring Boot 3 基础要求 |
| Spring Boot  | 3.x              | 后端基础框架           |
| Spring Cloud | 2023.x / 2024.x  | 微服务与 Gateway       |
| MySQL        | 8.x              | 存储用户、角色、权限   |
| Redis        | 6.x / 7.x        | 存储 Token 会话        |
| Sa-Token     | 项目统一锁定版本 | 登录认证、权限校验     |
| MyBatis-Plus | 3.5.x            | 数据访问               |
| Hutool       | 5.8.x            | 工具类                 |

### Maven 依赖配置

父工程用于统一管理 Spring Boot、Spring Cloud、Sa-Token、MyBatis-Plus、Hutool 等版本。实际项目中建议版本号集中放在父工程，避免各模块版本不一致。

文件位置：`auth-token-demo/pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <groupId>io.github.atengk</groupId>
    <artifactId>auth-token-demo</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <modules>
        <module>auth-common</module>
        <module>auth-service</module>
        <module>gateway-service</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <spring-boot.version>3.3.5</spring-boot.version>
        <spring-cloud.version>2023.0.3</spring-cloud.version>
        <sa-token.version>1.39.0</sa-token.version>
        <mybatis-plus.version>3.5.9</mybatis-plus.version>
        <hutool.version>5.8.33</hutool.version>
        <mysql.version>8.4.0</mysql.version>
        <lombok.version>1.18.34</lombok.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Spring Boot 依赖版本管理 -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- Spring Cloud 依赖版本管理，用于 Gateway、OpenFeign 等组件 -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- Sa-Token 登录认证和权限校验 -->
            <dependency>
                <groupId>cn.dev33</groupId>
                <artifactId>sa-token-spring-boot3-starter</artifactId>
                <version>${sa-token.version}</version>
            </dependency>

            <!-- Sa-Token Redis 集成，Token 会话落 Redis -->
            <dependency>
                <groupId>cn.dev33</groupId>
                <artifactId>sa-token-redis-jackson</artifactId>
                <version>${sa-token.version}</version>
            </dependency>

            <!-- MyBatis-Plus 数据访问增强 -->
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>

            <!-- Hutool 工具类，处理字符串、集合、加密摘要等常用逻辑 -->
            <dependency>
                <groupId>cn.hutool</groupId>
                <artifactId>hutool-all</artifactId>
                <version>${hutool.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

认证服务依赖如下，主要包含 Web、Sa-Token、Redis、MyBatis-Plus、MySQL、Hutool。

文件位置：`auth-service/pom.xml`

```xml
<dependencies>
    <!-- Web 接口支持 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验，用于登录参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Sa-Token Spring Boot 3 集成 -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-spring-boot3-starter</artifactId>
    </dependency>

    <!-- Sa-Token Redis 会话存储 -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-redis-jackson</artifactId>
    </dependency>

    <!-- Redis 客户端，用于会话和权限缓存 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- MyBatis-Plus 数据访问 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    </dependency>

    <!-- MySQL 驱动 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <version>${mysql.version}</version>
    </dependency>

    <!-- Hutool 工具类 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
    </dependency>

    <!-- Lombok 简化实体和 DTO 代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
        <optional>true</optional>
    </dependency>
</dependencies>
```

网关服务依赖如下，主要包含 Gateway、Sa-Token Reactor 集成、Redis 和公共模块。

文件位置：`gateway-service/pom.xml`

```xml
<dependencies>
    <!-- Spring Cloud Gateway 网关 -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>

    <!-- Sa-Token Reactor 集成，适配 Gateway 响应式环境 -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-reactor-spring-boot3-starter</artifactId>
        <version>${sa-token.version}</version>
    </dependency>

    <!-- Sa-Token Redis 会话存储 -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-redis-jackson</artifactId>
    </dependency>

    <!-- Redis 客户端，用于读取登录会话 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
    </dependency>

    <!-- 公共用户上下文对象和请求头常量 -->
    <dependency>
        <groupId>io.github.atengk</groupId>
        <artifactId>auth-common</artifactId>
        <version>1.0.0</version>
    </dependency>

    <!-- Hutool 工具类 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
    </dependency>

    <!-- Lombok 简化配置类代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
        <optional>true</optional>
    </dependency>
</dependencies>
```

### Redis 配置

Redis 用于保存 Sa-Token 登录会话。认证服务和网关服务必须连接同一个 Redis，否则网关无法识别认证服务生成的 Token。

文件位置：`auth-service/src/main/resources/application.yml`

```yaml
server:
  port: 8081

spring:
  application:
    name: auth-service

  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/auth_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: root

  data:
    redis:
      # Redis 地址，认证服务和网关服务必须保持一致
      host: localhost
      port: 6379
      database: 0
      timeout: 3s
      lettuce:
        pool:
          # 最大连接数
          max-active: 16
          # 最大空闲连接
          max-idle: 8
          # 最小空闲连接
          min-idle: 2

mybatis-plus:
  configuration:
    # 开发阶段打开 SQL 日志，生产环境建议关闭
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      # 使用数据库自增 ID
      id-type: auto
```

文件位置：`gateway-service/src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: gateway-service

  data:
    redis:
      # 与 auth-service 使用同一个 Redis
      host: localhost
      port: 6379
      database: 0
      timeout: 3s

  cloud:
    gateway:
      routes:
        # 认证服务路由
        - id: auth-service
          uri: http://localhost:8081
          predicates:
            - Path=/auth/**

        # 示例业务服务路由
        - id: business-service
          uri: http://localhost:8082
          predicates:
            - Path=/business/**
```

### Sa-Token 配置

Sa-Token 负责 Token 生成、登录态管理、权限校验、踢人下线和自动续期。本案例使用 Header 传 Token，字段名为 `Authorization`。

文件位置：`auth-service/src/main/resources/application.yml`

```yaml
sa-token:
  # Token 名称，前端请求时放在 Header: Authorization
  token-name: Authorization

  # Token 有效期，单位秒，示例为 2 小时
  timeout: 7200

  # Token 临时有效期，单位秒；超过 30 分钟无操作则需要重新登录
  active-timeout: 1800

  # 是否允许同一账号多地同时登录
  is-concurrent: true

  # 多端登录时，是否共用一个 Token
  is-share: false

  # 是否从 Cookie 读取 Token，前后端分离项目建议关闭
  is-read-cookie: false

  # 是否从 Header 读取 Token
  is-read-header: true

  # 是否从请求参数读取 Token，生产环境建议关闭
  is-read-body: false

  # Token 风格，可选 uuid、simple-uuid、random-32、tik 等
  token-style: random-32

  # 是否输出操作日志
  is-log: true

  # Token 前缀；如果开启，前端需要传：Authorization: Bearer xxx
  token-prefix: Bearer
```

文件位置：`gateway-service/src/main/resources/application.yml`

```yaml
sa-token:
  # 网关必须和认证服务保持一致
  token-name: Authorization
  timeout: 7200
  active-timeout: 1800
  is-concurrent: true
  is-share: false
  is-read-cookie: false
  is-read-header: true
  is-read-body: false
  token-style: random-32
  is-log: true
  token-prefix: Bearer

auth:
  gateway:
    white-list:
      # 登录接口放行
      - /auth/login
      # 健康检查放行
      - /actuator/**
      # Swagger 文档放行，按实际项目决定是否开放
      - /v3/api-docs/**
      - /swagger-ui/**
```

前端请求 Header 示例：

```http
Authorization: Bearer 1f2e3d4c5b6a7890abcdef1234567890
```

## 数据库设计

本案例使用标准 RBAC 模型：用户绑定角色，角色绑定权限。接口鉴权时根据当前登录用户 ID 查询权限码集合，再交给 Sa-Token 判断。

```text
sys_user
  -> sys_user_role
    -> sys_role
      -> sys_role_permission
        -> sys_permission
```

### 用户表

用户表存储账号、密码、状态、租户 ID 等基础登录信息。密码字段这里使用摘要值示例，生产环境建议使用 BCrypt 或 Argon2。

```sql
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID，单租户系统可固定为0',
    username VARCHAR(64) NOT NULL COMMENT '登录账号',
    password VARCHAR(128) NOT NULL COMMENT '登录密码摘要',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    UNIQUE KEY uk_username (username),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';
```

示例初始化用户：

```sql
-- 密码摘要示例：后续代码中会使用 SecureUtil.md5("123456" + salt) 的方式演示
INSERT INTO sys_user
(id, tenant_id, username, password, nickname, status)
VALUES
(1, 1001, 'admin', 'e10adc3949ba59abbe56e057f20f883e', '系统管理员', 1),
(2, 1001, 'ateng', 'e10adc3949ba59abbe56e057f20f883e', '普通用户', 1);
```

### 角色表

角色表存储角色编码和角色名称。角色编码用于 `@SaCheckRole("admin")` 这类注解判断。

```sql
CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';
```

示例初始化角色：

```sql
INSERT INTO sys_role
(id, role_code, role_name, status)
VALUES
(1, 'admin', '系统管理员', 1),
(2, 'user', '普通用户', 1);
```

### 权限表

权限表存储接口权限码。权限码建议按业务模块分层命名，例如 `user:list`、`user:add`、`order:pay`。

```sql
CREATE TABLE sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限ID',
    permission_code VARCHAR(128) NOT NULL COMMENT '权限编码',
    permission_name VARCHAR(128) NOT NULL COMMENT '权限名称',
    permission_type TINYINT NOT NULL DEFAULT 2 COMMENT '权限类型：1菜单，2按钮，3接口',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    UNIQUE KEY uk_permission_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表';
```

示例初始化权限：

```sql
INSERT INTO sys_permission
(id, permission_code, permission_name, permission_type, status)
VALUES
(1, 'user:list', '用户列表查询', 3, 1),
(2, 'user:add', '用户新增', 3, 1),
(3, 'user:delete', '用户删除', 3, 1),
(4, 'order:list', '订单列表查询', 3, 1);
```

### 用户角色关联表

用户角色关联表用于维护一个用户拥有多个角色的关系。

```sql
CREATE TABLE sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';
```

示例初始化用户角色：

```sql
INSERT INTO sys_user_role
(user_id, role_id)
VALUES
(1, 1),
(2, 2);
```

### 角色权限关联表

角色权限关联表用于维护一个角色拥有多个权限的关系。

```sql
CREATE TABLE sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    KEY idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';
```

示例初始化角色权限：

```sql
-- 管理员拥有所有示例权限
INSERT INTO sys_role_permission
(role_id, permission_id)
VALUES
(1, 1),
(1, 2),
(1, 3),
(1, 4);

-- 普通用户只拥有查询类权限
INSERT INTO sys_role_permission
(role_id, permission_id)
VALUES
(2, 1),
(2, 4);
```

完整初始化顺序建议如下：

```sql
-- 1. 创建 sys_user
-- 2. 创建 sys_role
-- 3. 创建 sys_permission
-- 4. 创建 sys_user_role
-- 5. 创建 sys_role_permission
-- 6. 初始化用户、角色、权限和关联数据
```

这套表结构已经能支撑后续登录认证、权限加载、角色校验、接口鉴权和管理员踢人下线。下一步可以继续实现登录 DTO、返回对象、实体类、Mapper 和认证 Service。

## 登录认证实现

登录认证负责完成账号密码校验、Token 生成、会话写入和登录结果返回。这里使用 Sa-Token 维护登录态，Redis 保存 Token 会话，MyBatis-Plus 查询用户基础信息。该实现对应原文档中「用户登录 -> 校验账号密码 -> 生成 Token -> 保存会话信息」这条核心链路。

### 登录参数与返回对象

登录参数只保留认证必要字段：账号、密码、登录设备。`device` 用于区分 PC、APP、小程序等客户端，后续可以配合 Sa-Token 做多端登录控制。

文件位置：`auth-service/src/main/java/io/github/atengk/auth/model/dto/LoginRequest.java`

```java
package io.github.atengk.auth.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class LoginRequest {

    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 登录设备：PC、APP、MINI_PROGRAM
     */
    private String device;
}
```

登录返回对象中直接返回前端最常用的 Token 信息，同时返回用户基础信息、角色编码和权限编码，方便前端初始化用户状态。

文件位置：`auth-service/src/main/java/io/github/atengk/auth/model/vo/LoginResponse.java`

```java
package io.github.atengk.auth.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 登录响应结果
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
public class LoginResponse {

    private Long userId;

    private Long tenantId;

    private String username;

    private String nickname;

    private String tokenName;

    private String tokenValue;

    /**
     * 前端可直接复制到请求头中的完整 Token 值，例如：Bearer xxx
     */
    private String authorization;

    /**
     * Token 剩余有效期，单位秒
     */
    private Long tokenTimeout;

    /**
     * Token 活跃有效期，单位秒
     */
    private Long activeTimeout;

    private List<String> roleCodes;

    private List<String> permissionCodes;
}
```

接口统一返回结构如下。实际项目中通常会放到 `common-core` 模块，本案例先放在认证服务中，保证代码完整。

文件位置：`auth-service/src/main/java/io/github/atengk/auth/model/Result.java`

```java
package io.github.atengk.auth.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口返回对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;

    private String message;

    private T data;

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "操作成功", data);
    }

    public static <T> Result<T> ok() {
        return new Result<>(200, "操作成功", null);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }
}
```

### 账号密码校验

账号密码校验分为三步：按账号查询用户、判断用户状态、比对密码摘要。为了和前面初始化 SQL 中的 `e10adc3949ba59abbe56e057f20f883e` 对齐，这里使用 `SecureUtil.md5("123456")` 做示例。生产环境建议替换为 `BCryptPasswordEncoder` 或 Argon2。

先补齐用户实体和 Mapper。

文件位置：`auth-service/src/main/java/io/github/atengk/auth/entity/SysUser.java`

```java
package io.github.atengk.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String username;

    private String password;

    private String nickname;

    /**
     * 状态：1启用，0禁用
     */
    private Integer status;

    private LocalDateTime lastLoginTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
```

这个 Mapper 同时提供登录查询、角色编码查询和权限编码查询。权限查询后续也会被 Sa-Token 权限实现类复用。

文件位置：`auth-service/src/main/java/io/github/atengk/auth/mapper/SysUserMapper.java`

```java
package io.github.atengk.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.auth.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统用户 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("""
            SELECT r.role_code
            FROM sys_role r
            INNER JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
              AND r.status = 1
              AND r.deleted = 0
            """)
    List<String> selectRoleCodesByUserId(Long userId);

    @Select("""
            SELECT DISTINCT p.permission_code
            FROM sys_permission p
            INNER JOIN sys_role_permission rp ON rp.permission_id = p.id
            INNER JOIN sys_user_role ur ON ur.role_id = rp.role_id
            INNER JOIN sys_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND r.status = 1
              AND r.deleted = 0
              AND p.status = 1
              AND p.deleted = 0
            """)
    List<String> selectPermissionCodesByUserId(Long userId);
}
```

### Token 生成与会话写入

登录成功后调用 `StpUtil.login()` 生成 Token，然后把当前用户、角色、权限写入 Sa-Token 会话。这里同时使用 `Session` 和 `TokenSession`：

| 会话类型       | 作用                                                |
| -------------- | --------------------------------------------------- |
| `Session`      | 按用户 ID 维度保存信息，同一个用户多个 Token 可共享 |
| `TokenSession` | 按当前 Token 维度保存信息，适合保存本次登录上下文   |

用户上下文对象放在公共模块中，后续网关和业务服务都会复用。

文件位置：`auth-common/src/main/java/io/github/atengk/auth/common/context/LoginUser.java`

```java
package io.github.atengk.auth.common.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 当前登录用户上下文
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {

    private Long userId;

    private Long tenantId;

    private String username;

    private String nickname;

    private List<String> roleCodes;

    private List<String> permissionCodes;
}
```

认证 Service 接口定义登录、当前用户、续期、退出和踢人下线这些核心动作。

文件位置：`auth-service/src/main/java/io/github/atengk/auth/service/AuthService.java`

```java
package io.github.atengk.auth.service;

import io.github.atengk.auth.common.context.LoginUser;
import io.github.atengk.auth.model.dto.LoginRequest;
import io.github.atengk.auth.model.vo.LoginResponse;

/**
 * 认证服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface AuthService {

    LoginResponse login(LoginRequest request);

    LoginUser currentUser();

    LoginResponse renew();

    void logout();

    void kickout(Long userId);
}
```

认证 Service 实现类是登录认证的核心代码，包含账号密码校验、Token 生成、会话写入、Token 续期、退出登录和踢人下线。

文件位置：`auth-service/src/main/java/io/github/atengk/auth/service/impl/AuthServiceImpl.java`

```java
package io.github.atengk.auth.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.atengk.auth.common.context.LoginUser;
import io.github.atengk.auth.entity.SysUser;
import io.github.atengk.auth.mapper.SysUserMapper;
import io.github.atengk.auth.model.dto.LoginRequest;
import io.github.atengk.auth.model.vo.LoginResponse;
import io.github.atengk.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

    private static final Integer USER_STATUS_ENABLE = 1;

    private static final String SESSION_LOGIN_USER = "loginUser";

    private final SysUserMapper sysUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(LoginRequest request) {
        String username = StrUtil.trim(request.getUsername());
        String device = StrUtil.blankToDefault(request.getDevice(), "PC");

        SysUser user = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username)
                .last("LIMIT 1"));

        if (ObjectUtil.isNull(user)) {
            log.warn("登录失败，账号不存在，username={}", username);
            throw new IllegalArgumentException("账号或密码错误");
        }

        if (!USER_STATUS_ENABLE.equals(user.getStatus())) {
            log.warn("登录失败，账号已禁用，userId={}, username={}", user.getId(), username);
            throw new IllegalArgumentException("账号已被禁用");
        }

        String passwordDigest = SecureUtil.md5(request.getPassword());
        if (!StrUtil.equals(passwordDigest, user.getPassword())) {
            log.warn("登录失败，密码错误，userId={}, username={}", user.getId(), username);
            throw new IllegalArgumentException("账号或密码错误");
        }

        List<String> roleCodes = sysUserMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissionCodes = sysUserMapper.selectPermissionCodesByUserId(user.getId());

        StpUtil.login(user.getId(), new SaLoginModel()
                .setDevice(device)
                .setExtra("tenantId", user.getTenantId())
                .setExtra("username", user.getUsername()));

        LoginUser loginUser = LoginUser.builder()
                .userId(user.getId())
                .tenantId(user.getTenantId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .roleCodes(CollUtil.isEmpty(roleCodes) ? List.of() : roleCodes)
                .permissionCodes(CollUtil.isEmpty(permissionCodes) ? List.of() : permissionCodes)
                .build();

        StpUtil.getSession().set(SESSION_LOGIN_USER, loginUser);
        StpUtil.getTokenSession().set(SESSION_LOGIN_USER, loginUser);

        SysUser updateUser = new SysUser();
        updateUser.setId(user.getId());
        updateUser.setLastLoginTime(LocalDateTime.now());
        sysUserMapper.updateById(updateUser);

        log.info("用户登录成功，userId={}, username={}, device={}", user.getId(), username, device);
        return buildLoginResponse(loginUser);
    }

    @Override
    public LoginUser currentUser() {
        StpUtil.checkLogin();

        Object sessionValue = StpUtil.getTokenSession().get(SESSION_LOGIN_USER);
        if (ObjectUtil.isNotNull(sessionValue)) {
            return BeanUtil.toBean(sessionValue, LoginUser.class);
        }

        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserMapper.selectById(userId);
        if (ObjectUtil.isNull(user)) {
            throw new NotLoginException("登录用户不存在", StpUtil.getLoginType(), StpUtil.getTokenValue());
        }

        List<String> roleCodes = sysUserMapper.selectRoleCodesByUserId(userId);
        List<String> permissionCodes = sysUserMapper.selectPermissionCodesByUserId(userId);

        LoginUser loginUser = LoginUser.builder()
                .userId(user.getId())
                .tenantId(user.getTenantId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .roleCodes(CollUtil.isEmpty(roleCodes) ? List.of() : roleCodes)
                .permissionCodes(CollUtil.isEmpty(permissionCodes) ? List.of() : permissionCodes)
                .build();

        StpUtil.getTokenSession().set(SESSION_LOGIN_USER, loginUser);
        return loginUser;
    }

    @Override
    public LoginResponse renew() {
        StpUtil.checkLogin();
        StpUtil.updateLastActiveToNow();

        LoginUser loginUser = currentUser();
        log.info("Token续期成功，userId={}, username={}", loginUser.getUserId(), loginUser.getUsername());
        return buildLoginResponse(loginUser);
    }

    @Override
    public void logout() {
        StpUtil.checkLogin();

        Long userId = StpUtil.getLoginIdAsLong();
        StpUtil.logout();

        log.info("用户退出登录成功，userId={}", userId);
    }

    @Override
    public void kickout(Long userId) {
        if (ObjectUtil.isNull(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        StpUtil.kickout(userId);
        log.info("管理员踢人下线成功，userId={}", userId);
    }

    private LoginResponse buildLoginResponse(LoginUser loginUser) {
        String tokenName = SaManager.getConfig().getTokenName();
        String tokenPrefix = StrUtil.blankToDefault(SaManager.getConfig().getTokenPrefix(), "");
        String tokenValue = StpUtil.getTokenValue();
        String authorization = StrUtil.isBlank(tokenPrefix) ? tokenValue : tokenPrefix + " " + tokenValue;

        return LoginResponse.builder()
                .userId(loginUser.getUserId())
                .tenantId(loginUser.getTenantId())
                .username(loginUser.getUsername())
                .nickname(loginUser.getNickname())
                .tokenName(tokenName)
                .tokenValue(tokenValue)
                .authorization(authorization)
                .tokenTimeout(StpUtil.getTokenTimeout())
                .activeTimeout(StpUtil.getTokenActivityTimeout())
                .roleCodes(loginUser.getRoleCodes())
                .permissionCodes(loginUser.getPermissionCodes())
                .build();
    }
}
```

### 登录接口实现

登录接口只负责参数接收和返回，不在 Controller 中写业务逻辑。登录接口必须放在网关白名单中，否则用户还没登录就会被网关拦截。

文件位置：`auth-service/src/main/java/io/github/atengk/auth/controller/AuthController.java`

```java
package io.github.atengk.auth.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import io.github.atengk.auth.common.context.LoginUser;
import io.github.atengk.auth.model.Result;
import io.github.atengk.auth.model.dto.LoginRequest;
import io.github.atengk.auth.model.vo.LoginResponse;
import io.github.atengk.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @GetMapping("/profile")
    public Result<LoginUser> profile() {
        return Result.ok(authService.currentUser());
    }

    @PostMapping("/renew")
    public Result<LoginResponse> renew() {
        return Result.ok(authService.renew());
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    @SaCheckRole("admin")
    @PostMapping("/kickout/{userId}")
    public Result<Void> kickout(@PathVariable Long userId) {
        authService.kickout(userId);
        return Result.ok();
    }
}
```

接口调用示例：

```bash
# 登录获取 Token
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "123456",
    "device": "PC"
  }'

# 携带 Token 查询当前用户
curl -X GET http://localhost:8081/auth/profile \
  -H "Authorization: Bearer 替换为登录返回的tokenValue"

# 主动续期
curl -X POST http://localhost:8081/auth/renew \
  -H "Authorization: Bearer 替换为登录返回的tokenValue"

# 退出登录
curl -X POST http://localhost:8081/auth/logout \
  -H "Authorization: Bearer 替换为登录返回的tokenValue"

# 管理员踢人下线
curl -X POST http://localhost:8081/auth/kickout/2 \
  -H "Authorization: Bearer 替换为管理员tokenValue"
```

## Token 会话管理

Token 会话管理主要解决四件事：保存登录状态、维持活跃状态、退出清理会话、管理员强制用户下线。Sa-Token 已经封装了大部分底层能力，本案例重点在业务侧如何正确使用这些能力。

### 会话信息存储

登录成功后，核心会话数据会写入 Redis。业务代码不需要直接操作 Redis Key，而是通过 Sa-Token API 读取。

```java
StpUtil.getSession().set("loginUser", loginUser);
StpUtil.getTokenSession().set("loginUser", loginUser);
```

建议存储的会话内容如下：

| 字段              | 说明             |
| ----------------- | ---------------- |
| `userId`          | 当前登录用户 ID  |
| `tenantId`        | 当前租户 ID      |
| `username`        | 登录账号         |
| `nickname`        | 用户昵称         |
| `roleCodes`       | 当前用户角色编码 |
| `permissionCodes` | 当前用户权限编码 |

推荐只在会话中保存认证授权高频使用的小字段，不要把完整用户表、菜单树、大对象配置全部塞入 Token 会话。权限很多时可以只存用户基础信息，权限码走 Redis 缓存或数据库加载。

### Token 续期处理

本案例使用 Sa-Token 的活跃续期机制：

```yaml
sa-token:
  timeout: 7200
  active-timeout: 1800
```

含义如下：

| 配置                              | 说明                                |
| --------------------------------- | ----------------------------------- |
| `timeout`                         | Token 总有效期，这里是 2 小时       |
| `active-timeout`                  | 活跃有效期，这里是 30 分钟          |
| `StpUtil.updateLastActiveToNow()` | 将当前 Token 活跃时间刷新到当前时间 |

主动续期接口如下：

```java
@PostMapping("/renew")
public Result<LoginResponse> renew() {
    return Result.ok(authService.renew());
}
```

前端可以在以下场景调用续期接口：

```text
1. 用户打开系统首页后，先调用 /auth/profile 确认登录态
2. 用户持续操作系统时，每隔 10 到 15 分钟调用一次 /auth/renew
3. 续期接口返回未登录时，前端跳转登录页
```

更常见的后台系统做法是：每次接口请求都携带 Token，后端在关键接口中自动刷新活跃时间；前端只在页面初始化和定时探活时调用 `/auth/renew`。

### 退出登录处理

退出登录只清理当前 Token，不影响同一账号在其他设备上的登录态。适用于用户主动点击「退出登录」。

核心代码如下：

```java
@Override
public void logout() {
    StpUtil.checkLogin();

    Long userId = StpUtil.getLoginIdAsLong();
    StpUtil.logout();

    log.info("用户退出登录成功，userId={}", userId);
}
```

退出后的效果：

```text
当前 Token 立即失效
当前 TokenSession 被清理
再次访问受保护接口会返回未登录
同账号其他设备 Token 不受影响
```

验证命令：

```bash
# 1. 先登录获取 Token
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456","device":"PC"}'

# 2. 退出登录
curl -X POST http://localhost:8081/auth/logout \
  -H "Authorization: Bearer 替换为tokenValue"

# 3. 再次访问当前用户接口，应返回未登录
curl -X GET http://localhost:8081/auth/profile \
  -H "Authorization: Bearer 替换为tokenValue"
```

### 踢人下线处理

踢人下线用于管理员强制某个用户重新登录，例如账号异常、权限变更、安全策略触发等场景。

核心代码如下：

```java
@Override
public void kickout(Long userId) {
    if (ObjectUtil.isNull(userId)) {
        throw new IllegalArgumentException("用户ID不能为空");
    }

    StpUtil.kickout(userId);
    log.info("管理员踢人下线成功，userId={}", userId);
}
```

踢人和退出登录的区别如下：

| 操作                              | 影响范围                          | 使用场景                     |
| --------------------------------- | --------------------------------- | ---------------------------- |
| `StpUtil.logout()`                | 当前 Token                        | 用户主动退出                 |
| `StpUtil.logout(userId)`          | 指定用户所有 Token 直接注销       | 后台清理登录态               |
| `StpUtil.kickout(userId)`         | 指定用户所有 Token 标记为被踢下线 | 管理员强制下线，并可区分提示 |
| `StpUtil.kickout(userId, device)` | 指定用户某个设备端下线            | 只踢 APP 或只踢 PC           |

接口已经加了角色校验：

```java
@SaCheckRole("admin")
@PostMapping("/kickout/{userId}")
public Result<Void> kickout(@PathVariable Long userId) {
    authService.kickout(userId);
    return Result.ok();
}
```

验证命令：

```bash
# 管理员踢用户 2 下线
curl -X POST http://localhost:8081/auth/kickout/2 \
  -H "Authorization: Bearer 替换为管理员tokenValue"

# 用户 2 再访问接口，应返回被踢下线或未登录
curl -X GET http://localhost:8081/auth/profile \
  -H "Authorization: Bearer 替换为用户2的tokenValue"
```

到这里，登录认证与 Token 会话管理的核心闭环已经完成：登录生成 Token、Redis 保存会话、接口读取当前用户、Token 续期、退出登录、管理员踢人下线。下一段可以继续实现「权限授权实现」，把 `@SaCheckRole`、`@SaCheckPermission` 和权限缓存补齐。

## 权限授权实现

权限授权实现基于 RBAC 模型：用户拥有角色，角色拥有权限。登录后系统会加载当前用户的角色编码和权限编码，接口访问时通过 Sa-Token 的 `@SaCheckPermission`、`@SaCheckRole` 进行校验。这部分对应原文档中「权限校验、权限缓存、接口鉴权、网关鉴权」等核心难点。

### 用户权限加载

Sa-Token 的注解鉴权依赖 `StpInterface`。只要实现该接口，Sa-Token 在执行 `@SaCheckPermission` 和 `@SaCheckRole` 时就会自动调用对应方法获取当前用户权限。

先定义权限缓存 Key，后续权限加载、刷新、清理都统一使用这里的常量。

文件位置：`auth-service/src/main/java/io/github/atengk/auth/constant/AuthCacheConstant.java`

```java
package io.github.atengk.auth.constant;

/**
 * 认证缓存常量
 *
 * @author Ateng
 * @since 2026-05-15
 */
public final class AuthCacheConstant {

    public static final String USER_PERMISSION_KEY = "auth:permission:user:";

    public static final String USER_ROLE_KEY = "auth:role:user:";

    private AuthCacheConstant() {
    }
}
```

权限加载服务负责从 Redis 读取权限缓存；缓存不存在时再查数据库，并写回 Redis。

文件位置：`auth-service/src/main/java/io/github/atengk/auth/service/UserPermissionService.java`

```java
package io.github.atengk.auth.service;

import java.util.List;

/**
 * 用户权限服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface UserPermissionService {

    /**
     * 查询用户权限编码集合
     *
     * @param userId 用户ID
     * @return 权限编码集合
     */
    List<String> getPermissionCodes(Long userId);

    /**
     * 查询用户角色编码集合
     *
     * @param userId 用户ID
     * @return 角色编码集合
     */
    List<String> getRoleCodes(Long userId);

    /**
     * 刷新用户权限缓存
     *
     * @param userId 用户ID
     */
    void refreshUserAuthCache(Long userId);

    /**
     * 清理用户权限缓存
     *
     * @param userId 用户ID
     */
    void clearUserAuthCache(Long userId);
}
```

该实现优先读 Redis，未命中时查询 `sys_user_role`、`sys_role_permission`、`sys_permission` 等表。

文件位置：`auth-service/src/main/java/io/github/atengk/auth/service/impl/UserPermissionServiceImpl.java`

```java
package io.github.atengk.auth.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.auth.constant.AuthCacheConstant;
import io.github.atengk.auth.mapper.SysUserMapper;
import io.github.atengk.auth.service.UserPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 用户权限服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserPermissionServiceImpl implements UserPermissionService {

    private static final Duration AUTH_CACHE_TTL = Duration.ofMinutes(30);

    private final SysUserMapper sysUserMapper;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public List<String> getPermissionCodes(Long userId) {
        if (ObjectUtil.isNull(userId)) {
            return List.of();
        }

        String cacheKey = AuthCacheConstant.USER_PERMISSION_KEY + userId;
        String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cacheValue)) {
            return JSONUtil.toList(JSONUtil.parseArray(cacheValue), String.class);
        }

        List<String> permissionCodes = sysUserMapper.selectPermissionCodesByUserId(userId);
        if (CollUtil.isEmpty(permissionCodes)) {
            permissionCodes = List.of();
        }

        stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(permissionCodes), AUTH_CACHE_TTL);
        log.info("加载用户权限缓存，userId={}, permissionCount={}", userId, permissionCodes.size());
        return permissionCodes;
    }

    @Override
    public List<String> getRoleCodes(Long userId) {
        if (ObjectUtil.isNull(userId)) {
            return List.of();
        }

        String cacheKey = AuthCacheConstant.USER_ROLE_KEY + userId;
        String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cacheValue)) {
            return JSONUtil.toList(JSONUtil.parseArray(cacheValue), String.class);
        }

        List<String> roleCodes = sysUserMapper.selectRoleCodesByUserId(userId);
        if (CollUtil.isEmpty(roleCodes)) {
            roleCodes = List.of();
        }

        stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(roleCodes), AUTH_CACHE_TTL);
        log.info("加载用户角色缓存，userId={}, roleCount={}", userId, roleCodes.size());
        return roleCodes;
    }

    @Override
    public void refreshUserAuthCache(Long userId) {
        clearUserAuthCache(userId);
        getRoleCodes(userId);
        getPermissionCodes(userId);
        log.info("刷新用户认证授权缓存，userId={}", userId);
    }

    @Override
    public void clearUserAuthCache(Long userId) {
        if (ObjectUtil.isNull(userId)) {
            return;
        }

        stringRedisTemplate.delete(List.of(
                AuthCacheConstant.USER_ROLE_KEY + userId,
                AuthCacheConstant.USER_PERMISSION_KEY + userId
        ));
        log.info("清理用户认证授权缓存，userId={}", userId);
    }
}
```

然后实现 Sa-Token 的权限接口。

文件位置：`auth-service/src/main/java/io/github/atengk/auth/satoken/StpPermissionImpl.java`

```java
package io.github.atengk.auth.satoken;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.convert.Convert;
import io.github.atengk.auth.service.UserPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sa-Token 权限加载实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Component
@RequiredArgsConstructor
public class StpPermissionImpl implements StpInterface {

    private final UserPermissionService userPermissionService;

    /**
     * 返回当前用户权限编码集合
     *
     * @param loginId   登录ID
     * @param loginType 登录类型
     * @return 权限编码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Convert.toLong(loginId);
        return userPermissionService.getPermissionCodes(userId);
    }

    /**
     * 返回当前用户角色编码集合
     *
     * @param loginId   登录ID
     * @param loginType 登录类型
     * @return 角色编码集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Convert.toLong(loginId);
        return userPermissionService.getRoleCodes(userId);
    }
}
```

登录成功时，也建议改为从 `UserPermissionService` 获取角色和权限，避免登录逻辑和权限逻辑重复。

在 `AuthServiceImpl` 中注入：

```java
private final UserPermissionService userPermissionService;
```

然后替换原来的角色、权限查询代码：

```java
List<String> roleCodes = userPermissionService.getRoleCodes(user.getId());
List<String> permissionCodes = userPermissionService.getPermissionCodes(user.getId());
```

### 权限缓存设计

权限缓存建议按用户维度存储，不建议直接按 Token 存储。原因是一个用户可能有多个登录端，只要用户角色权限发生变化，清理用户维度缓存即可影响所有端。

| 缓存 Key                        | 缓存内容             | 过期时间 |
| ------------------------------- | -------------------- | -------- |
| `auth:role:user:{userId}`       | 当前用户角色编码集合 | 30 分钟  |
| `auth:permission:user:{userId}` | 当前用户权限编码集合 | 30 分钟  |

权限变更后必须清理缓存。比如后台给用户分配新角色、修改角色权限、禁用角色时，应调用：

```java
userPermissionService.clearUserAuthCache(userId);
```

如果希望权限变更后用户立即重新登录，可以同时踢人下线：

```java
userPermissionService.clearUserAuthCache(userId);
StpUtil.kickout(userId);
```

权限缓存刷新接口可以给后台管理使用。这里限制为管理员角色。

文件位置：`auth-service/src/main/java/io/github/atengk/auth/controller/AuthPermissionController.java`

```java
package io.github.atengk.auth.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import io.github.atengk.auth.model.Result;
import io.github.atengk.auth.service.UserPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证授权缓存接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/permission")
public class AuthPermissionController {

    private final UserPermissionService userPermissionService;

    /**
     * 刷新指定用户权限缓存
     *
     * @param userId 用户ID
     * @return 操作结果
     */
    @SaCheckRole("admin")
    @PostMapping("/refresh/{userId}")
    public Result<Void> refresh(@PathVariable Long userId) {
        userPermissionService.refreshUserAuthCache(userId);
        return Result.ok();
    }

    /**
     * 清理指定用户权限缓存
     *
     * @param userId 用户ID
     * @return 操作结果
     */
    @SaCheckRole("admin")
    @DeleteMapping("/cache/{userId}")
    public Result<Void> clear(@PathVariable Long userId) {
        userPermissionService.clearUserAuthCache(userId);
        return Result.ok();
    }
}
```

### 接口权限校验

接口权限校验适合按钮级、接口级权限。例如用户查询、用户新增、订单查询、订单导出等。

下面给一个业务接口示例，模拟放在业务服务中。只要业务服务也引入 Sa-Token，并实现或能访问同一套权限加载逻辑，就可以直接使用 `@SaCheckPermission`。

文件位置：`business-service/src/main/java/io/github/atengk/business/controller/BusinessUserController.java`

```java
package io.github.atengk.business.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 业务用户接口示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@RestController
@RequestMapping("/business/users")
public class BusinessUserController {

    /**
     * 查询用户列表
     *
     * @return 用户列表
     */
    @SaCheckPermission("user:list")
    @GetMapping
    public List<String> listUsers() {
        log.info("查询业务用户列表");
        return List.of("admin", "ateng");
    }

    /**
     * 新增用户
     *
     * @param username 用户名
     * @return 操作结果
     */
    @SaCheckPermission("user:add")
    @PostMapping
    public String addUser(@RequestParam String username) {
        log.info("新增业务用户，username={}", username);
        return "新增成功：" + username;
    }

    /**
     * 删除用户
     *
     * @param userId 用户ID
     * @return 操作结果
     */
    @SaCheckPermission("user:delete")
    @DeleteMapping("/{userId}")
    public String deleteUser(@PathVariable Long userId) {
        log.info("删除业务用户，userId={}", userId);
        return "删除成功：" + userId;
    }
}
```

验证效果如下：

```bash
# admin 拥有 user:list 权限，可以访问
curl -X GET http://localhost:8082/business/users \
  -H "Authorization: Bearer 替换为管理员tokenValue"

# 普通用户拥有 user:list 权限，也可以访问
curl -X GET http://localhost:8082/business/users \
  -H "Authorization: Bearer 替换为普通用户tokenValue"

# 普通用户没有 user:delete 权限，访问会被拒绝
curl -X DELETE http://localhost:8082/business/users/2 \
  -H "Authorization: Bearer 替换为普通用户tokenValue"
```

### 角色权限校验

角色校验适合粗粒度管理接口，例如系统配置、管理员操作、踢人下线、权限缓存刷新等。

示例代码如下：

```java
@SaCheckRole("admin")
@PostMapping("/kickout/{userId}")
public Result<Void> kickout(@PathVariable Long userId) {
    authService.kickout(userId);
    return Result.ok();
}
```

如果一个接口允许多个角色访问，可以使用：

```java
@SaCheckRole(value = {"admin", "manager"}, mode = SaMode.OR)
```

完整示例：

文件位置：`auth-service/src/main/java/io/github/atengk/auth/controller/AdminDemoController.java`

```java
package io.github.atengk.auth.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import io.github.atengk.auth.model.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员接口示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@RestController
@RequestMapping("/auth/admin")
public class AdminDemoController {

    /**
     * 仅管理员可访问
     *
     * @return 操作结果
     */
    @SaCheckRole("admin")
    @GetMapping("/only-admin")
    public Result<String> onlyAdmin() {
        log.info("管理员接口访问成功");
        return Result.ok("仅管理员可访问");
    }

    /**
     * 管理员或经理可访问
     *
     * @return 操作结果
     */
    @SaCheckRole(value = {"admin", "manager"}, mode = SaMode.OR)
    @GetMapping("/admin-or-manager")
    public Result<String> adminOrManager() {
        log.info("管理员或经理接口访问成功");
        return Result.ok("管理员或经理可访问");
    }
}
```

这里的角色编码必须和 `sys_role.role_code` 保持一致，例如：

```text
admin
user
manager
```

## 网关统一鉴权

网关统一鉴权用于在请求进入下游服务之前先判断 Token 是否有效。它解决的是「是否登录」的问题，不建议把所有接口权限规则都写在网关层。更合理的分工是：

| 层级             | 职责                                      |
| ---------------- | ----------------------------------------- |
| Gateway          | 校验 Token 是否存在、是否有效、是否已过期 |
| Auth Service     | 登录、退出、续期、踢人、权限加载          |
| Business Service | 使用注解做接口权限和角色权限校验          |

### Gateway 鉴权过滤器

先在公共模块中定义请求头常量，避免网关和业务服务各写一套字符串。

文件位置：`auth-common/src/main/java/io/github/atengk/auth/common/constant/AuthHeaderConstant.java`

```java
package io.github.atengk.auth.common.constant;

/**
 * 认证请求头常量
 *
 * @author Ateng
 * @since 2026-05-15
 */
public final class AuthHeaderConstant {

    public static final String AUTHORIZATION = "Authorization";

    public static final String TOKEN_PREFIX = "Bearer ";

    public static final String USER_ID = "X-User-Id";

    public static final String USERNAME = "X-Username";

    public static final String TENANT_ID = "X-Tenant-Id";

    private AuthHeaderConstant() {
    }
}
```

网关白名单配置类用于读取 `application.yml` 中的放行路径。

文件位置：`gateway-service/src/main/java/io/github/atengk/gateway/config/GatewayWhiteListProperties.java`

```java
package io.github.atengk.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关白名单配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth.gateway")
public class GatewayWhiteListProperties {

    /**
     * 网关鉴权白名单
     */
    private List<String> whiteList = new ArrayList<>();
}
```

网关全局过滤器实现 Token 校验、白名单放行、用户上下文透传。它会先清理前端伪造的用户请求头，再写入可信的用户信息。

文件位置：`gateway-service/src/main/java/io/github/atengk/gateway/filter/TokenAuthGlobalFilter.java`

```java
package io.github.atengk.gateway.filter;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.auth.common.constant.AuthHeaderConstant;
import io.github.atengk.auth.common.context.LoginUser;
import io.github.atengk.gateway.config.GatewayWhiteListProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Token 认证全局过滤器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String SESSION_LOGIN_USER = "loginUser";

    private final GatewayWhiteListProperties whiteListProperties;

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    /**
     * 执行网关鉴权
     *
     * @param exchange 请求交换对象
     * @param chain    过滤器链
     * @return 过滤结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isWhitePath(path)) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest()
                .getHeaders()
                .getFirst(AuthHeaderConstant.AUTHORIZATION);

        String token = parseToken(authorization);
        if (StrUtil.isBlank(token)) {
            return unauthorized(exchange, "请先登录");
        }

        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (ObjectUtil.isNull(loginId)) {
                return unauthorized(exchange, "登录状态已失效");
            }

            LoginUser loginUser = getLoginUserByToken(token, loginId);
            ServerHttpRequest newRequest = buildUserContextRequest(exchange, loginUser);

            log.info("网关鉴权通过，path={}, userId={}, username={}",
                    path, loginUser.getUserId(), loginUser.getUsername());

            return chain.filter(exchange.mutate().request(newRequest).build());
        } catch (Exception ex) {
            log.warn("网关鉴权失败，path={}, message={}", path, ex.getMessage());
            return unauthorized(exchange, "登录状态无效或已过期");
        }
    }

    /**
     * 返回过滤器执行顺序
     *
     * @return 顺序值
     */
    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isWhitePath(String path) {
        if (CollUtil.isEmpty(whiteListProperties.getWhiteList())) {
            return false;
        }
        return whiteListProperties.getWhiteList()
                .stream()
                .anyMatch(pattern -> antPathMatcher.match(pattern, path));
    }

    private String parseToken(String authorization) {
        if (StrUtil.isBlank(authorization)) {
            return null;
        }

        String value = StrUtil.trim(authorization);
        if (StrUtil.startWithIgnoreCase(value, AuthHeaderConstant.TOKEN_PREFIX)) {
            return StrUtil.removePrefixIgnoreCase(value, AuthHeaderConstant.TOKEN_PREFIX).trim();
        }
        return value;
    }

    private LoginUser getLoginUserByToken(String token, Object loginId) {
        SaSession tokenSession = StpUtil.getTokenSessionByToken(token);
        Object sessionValue = tokenSession.get(SESSION_LOGIN_USER);

        if (ObjectUtil.isNotNull(sessionValue)) {
            return BeanUtil.toBean(sessionValue, LoginUser.class);
        }

        return LoginUser.builder()
                .userId(Long.valueOf(String.valueOf(loginId)))
                .username(String.valueOf(loginId))
                .tenantId(0L)
                .build();
    }

    private ServerHttpRequest buildUserContextRequest(ServerWebExchange exchange, LoginUser loginUser) {
        return exchange.getRequest()
                .mutate()
                .headers(headers -> {
                    // 清理外部伪造的用户上下文请求头
                    headers.remove(AuthHeaderConstant.USER_ID);
                    headers.remove(AuthHeaderConstant.USERNAME);
                    headers.remove(AuthHeaderConstant.TENANT_ID);

                    // 写入网关认证后的可信用户上下文
                    headers.set(AuthHeaderConstant.USER_ID, String.valueOf(loginUser.getUserId()));
                    headers.set(AuthHeaderConstant.USERNAME, StrUtil.blankToDefault(loginUser.getUsername(), ""));
                    headers.set(AuthHeaderConstant.TENANT_ID, String.valueOf(loginUser.getTenantId()));
                })
                .build();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = JSONUtil.toJsonStr(Dict.create()
                .set("code", 401)
                .set("message", message)
                .set("data", null));

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
```

### 白名单接口放行

白名单接口包括登录接口、健康检查接口、Swagger 文档接口。生产环境是否开放 Swagger，需要按安全要求控制。

文件位置：`gateway-service/src/main/resources/application.yml`

```yaml
auth:
  gateway:
    white-list:
      # 登录接口必须放行，否则用户无法获取 Token
      - /auth/login

      # 可选：健康检查接口
      - /actuator/**

      # 可选：接口文档，生产环境建议关闭或加内网限制
      - /v3/api-docs/**
      - /swagger-ui/**
```

白名单匹配规则使用 `AntPathMatcher`，支持：

```text
/auth/login
/auth/**
/actuator/**
/swagger-ui/**
```

注意不要把核心业务路径配置成白名单，例如：

```text
/business/**
/order/**
/admin/**
```

否则网关层登录校验会被绕过。

### Token 校验与拦截

网关从请求头读取 Token：

```http
Authorization: Bearer 1f2e3d4c5b6a7890abcdef1234567890
```

过滤器会完成以下校验：

```text
请求路径是否在白名单
-> 是：直接放行
-> 否：读取 Authorization
-> Token 为空：返回 401
-> Token 无效：返回 401
-> Token 过期：返回 401
-> Token 有效：写入用户上下文请求头并转发
```

验证命令如下：

```bash
# 1. 登录获取 Token
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456","device":"PC"}'

# 2. 不带 Token 访问业务接口，应返回 401
curl -X GET http://localhost:8080/business/users

# 3. 携带 Token 访问业务接口，应进入下游服务
curl -X GET http://localhost:8080/business/users \
  -H "Authorization: Bearer 替换为登录返回的tokenValue"

# 4. 使用错误 Token 访问，应返回 401
curl -X GET http://localhost:8080/business/users \
  -H "Authorization: Bearer error-token"
```

### 用户上下文请求头透传

网关校验 Token 成功后，会向下游服务透传以下请求头：

| 请求头        | 含义            |
| ------------- | --------------- |
| `X-User-Id`   | 当前登录用户 ID |
| `X-Username`  | 当前登录账号    |
| `X-Tenant-Id` | 当前租户 ID     |

下游服务可以通过请求头读取用户信息。后续更推荐统一封装成拦截器和 `ThreadLocal`，业务代码不要到处写 `request.getHeader()`。

简单读取示例：

文件位置：`business-service/src/main/java/io/github/atengk/business/controller/UserContextDemoController.java`

```java
package io.github.atengk.business.controller;

import io.github.atengk.auth.common.constant.AuthHeaderConstant;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户上下文示例接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/business/context")
public class UserContextDemoController {

    private final HttpServletRequest request;

    /**
     * 获取网关透传的用户上下文
     *
     * @return 当前用户上下文
     */
    @GetMapping("/current")
    public String current() {
        String userId = request.getHeader(AuthHeaderConstant.USER_ID);
        String username = request.getHeader(AuthHeaderConstant.USERNAME);
        String tenantId = request.getHeader(AuthHeaderConstant.TENANT_ID);

        log.info("读取用户上下文，userId={}, username={}, tenantId={}", userId, username, tenantId);
        return "userId=" + userId + ", username=" + username + ", tenantId=" + tenantId;
    }
}
```

验证命令：

```bash
curl -X GET http://localhost:8080/business/context/current \
  -H "Authorization: Bearer 替换为登录返回的tokenValue"
```

预期返回：

```text
userId=1, username=admin, tenantId=1001
```

到这里，权限授权和网关统一鉴权已经形成闭环：

```text
用户登录获取 Token
-> Sa-Token 写入 Redis 会话
-> Gateway 校验 Token
-> Gateway 透传用户上下文
-> 业务服务执行 @SaCheckPermission / @SaCheckRole
-> Sa-Token 调用 StpInterface 加载角色和权限
-> 权限缓存命中 Redis，未命中再查数据库
```

## 微服务用户上下文

微服务用户上下文用于解决下游业务服务如何获取当前登录用户的问题。网关已经完成 Token 校验，并把可信用户信息写入请求头；业务服务只需要在入口拦截器中解析这些请求头，再放入 `ThreadLocal`，业务代码即可通过统一工具类获取当前用户。该设计承接原文档中「用户上下文透传」这一核心难点。

### 上下文对象设计

`LoginUser` 已在前文放入 `auth-common` 模块。这里补充 `UserContextHolder`，用于保存当前线程中的登录用户信息。

文件位置：`auth-common/src/main/java/io/github/atengk/auth/common/context/UserContextHolder.java`

```java
package io.github.atengk.auth.common.context;

import cn.hutool.core.util.ObjectUtil;

/**
 * 用户上下文持有器
 *
 * @author Ateng
 * @since 2026-05-15
 */
public final class UserContextHolder {

    private static final ThreadLocal<LoginUser> USER_CONTEXT = new ThreadLocal<>();

    private UserContextHolder() {
    }

    /**
     * 设置当前登录用户
     *
     * @param loginUser 登录用户
     */
    public static void set(LoginUser loginUser) {
        USER_CONTEXT.set(loginUser);
    }

    /**
     * 获取当前登录用户
     *
     * @return 登录用户
     */
    public static LoginUser get() {
        return USER_CONTEXT.get();
    }

    /**
     * 获取当前用户ID
     *
     * @return 用户ID
     */
    public static Long getUserId() {
        LoginUser loginUser = get();
        return ObjectUtil.isNull(loginUser) ? null : loginUser.getUserId();
    }

    /**
     * 获取当前租户ID
     *
     * @return 租户ID
     */
    public static Long getTenantId() {
        LoginUser loginUser = get();
        return ObjectUtil.isNull(loginUser) ? null : loginUser.getTenantId();
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名
     */
    public static String getUsername() {
        LoginUser loginUser = get();
        return ObjectUtil.isNull(loginUser) ? null : loginUser.getUsername();
    }

    /**
     * 清理当前线程用户上下文
     */
    public static void clear() {
        USER_CONTEXT.remove();
    }
}
```

业务代码中可以这样使用：

```java
Long userId = UserContextHolder.getUserId();
Long tenantId = UserContextHolder.getTenantId();
String username = UserContextHolder.getUsername();
```

注意，`ThreadLocal` 必须在请求结束后清理，否则线程池复用时可能出现用户上下文污染。

### 请求拦截器解析用户信息

业务服务通过 Spring MVC 拦截器读取网关透传的请求头，并写入 `UserContextHolder`。这个拦截器建议每个业务服务都接入。

文件位置：`business-service/src/main/java/io/github/atengk/business/interceptor/UserContextInterceptor.java`

```java
package io.github.atengk.business.interceptor;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.auth.common.constant.AuthHeaderConstant;
import io.github.atengk.auth.common.context.LoginUser;
import io.github.atengk.auth.common.context.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户上下文解析拦截器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
public class UserContextInterceptor implements HandlerInterceptor {

    /**
     * 请求进入 Controller 前解析用户上下文
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param handler  处理器
     * @return 是否放行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader(AuthHeaderConstant.USER_ID);
        String username = request.getHeader(AuthHeaderConstant.USERNAME);
        String tenantId = request.getHeader(AuthHeaderConstant.TENANT_ID);
        String authorization = request.getHeader(AuthHeaderConstant.AUTHORIZATION);

        if (StrUtil.isBlank(userId)) {
            return true;
        }

        LoginUser loginUser = LoginUser.builder()
                .userId(Convert.toLong(userId))
                .tenantId(Convert.toLong(tenantId, 0L))
                .username(username)
                .build();

        UserContextHolder.set(loginUser);
        log.info("解析用户上下文成功，userId={}, username={}, tenantId={}", userId, username, tenantId);

        return true;
    }

    /**
     * 请求完成后清理用户上下文
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param handler  处理器
     * @param ex       异常
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContextHolder.clear();
    }
}
```

注册拦截器。

文件位置：`business-service/src/main/java/io/github/atengk/business/config/WebMvcConfig.java`

```java
package io.github.atengk.business.config;

import io.github.atengk.business.interceptor.UserContextInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * Web MVC 配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserContextInterceptor userContextInterceptor;

    /**
     * 注册请求拦截器
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userContextInterceptor)
                .addPathPatterns("/**");
    }
}
```

业务接口中验证上下文是否解析成功。

文件位置：`business-service/src/main/java/io/github/atengk/business/controller/ContextDemoController.java`

```java
package io.github.atengk.business.controller;

import cn.hutool.core.lang.Dict;
import io.github.atengk.auth.common.context.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户上下文测试接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@RestController
public class ContextDemoController {

    /**
     * 获取当前用户上下文
     *
     * @return 用户上下文
     */
    @GetMapping("/business/context/current")
    public Dict current() {
        Long userId = UserContextHolder.getUserId();
        Long tenantId = UserContextHolder.getTenantId();
        String username = UserContextHolder.getUsername();

        log.info("读取当前用户上下文，userId={}, username={}, tenantId={}", userId, username, tenantId);

        return Dict.create()
                .set("userId", userId)
                .set("username", username)
                .set("tenantId", tenantId);
    }
}
```

### OpenFeign 用户信息透传

服务 A 调用服务 B 时，如果不额外处理，服务 B 无法拿到当前用户上下文。可以通过 Feign 拦截器把当前请求中的用户信息继续向下游透传。

业务服务需要引入 OpenFeign：

```xml
<!-- OpenFeign 服务间调用 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

Feign 透传拦截器如下。

文件位置：`business-service/src/main/java/io/github/atengk/business/config/FeignUserContextConfig.java`

```java
package io.github.atengk.business.config;

import cn.hutool.core.util.StrUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.github.atengk.auth.common.constant.AuthHeaderConstant;
import io.github.atengk.auth.common.context.LoginUser;
import io.github.atengk.auth.common.context.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 用户上下文透传配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Configuration
public class FeignUserContextConfig {

    /**
     * 透传当前用户上下文
     *
     * @return Feign 请求拦截器
     */
    @Bean
    public RequestInterceptor userContextRequestInterceptor() {
        return new RequestInterceptor() {

            /**
             * Feign 请求发出前写入用户上下文请求头
             *
             * @param template 请求模板
             */
            @Override
            public void apply(RequestTemplate template) {
                LoginUser loginUser = UserContextHolder.get();
                if (loginUser == null) {
                    return;
                }

                template.header(AuthHeaderConstant.USER_ID, String.valueOf(loginUser.getUserId()));
                template.header(AuthHeaderConstant.TENANT_ID, String.valueOf(loginUser.getTenantId()));

                if (StrUtil.isNotBlank(loginUser.getUsername())) {
                    template.header(AuthHeaderConstant.USERNAME, loginUser.getUsername());
                }

                log.info("Feign透传用户上下文，userId={}, username={}",
                        loginUser.getUserId(), loginUser.getUsername());
            }
        };
    }
}
```

启动类开启 Feign：

文件位置：`business-service/src/main/java/io/github/atengk/business/BusinessApplication.java`

```java
package io.github.atengk.business;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 业务服务启动类
 *
 * @author Ateng
 * @since 2026-05-15
 */
@EnableFeignClients
@SpringBootApplication
public class BusinessApplication {

    /**
     * 启动业务服务
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(BusinessApplication.class, args);
    }
}
```

## 核心接口测试

这里统一通过网关端口 `8080` 测试完整链路，避免绕过 Gateway 鉴权。测试顺序为：登录获取 Token、携带 Token 访问接口、验证无权限访问、退出后再次访问。

### 登录获取 Token

登录接口在网关白名单中，可以不携带 Token 访问。

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "123456",
    "device": "PC"
  }'
```

预期响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 1,
    "tenantId": 1001,
    "username": "admin",
    "nickname": "系统管理员",
    "tokenName": "Authorization",
    "tokenValue": "4f5c6e7a8b9c1234567890abcdef1234",
    "authorization": "Bearer 4f5c6e7a8b9c1234567890abcdef1234",
    "tokenTimeout": 7200,
    "activeTimeout": 1800,
    "roleCodes": [
      "admin"
    ],
    "permissionCodes": [
      "user:list",
      "user:add",
      "user:delete",
      "order:list"
    ]
  }
}
```

测试时把 `data.authorization` 的值复制出来，后续请求统一放到 `Authorization` 请求头中。

### 携带 Token 访问接口

携带 Token 访问当前用户接口。

```bash
curl -X GET http://localhost:8080/auth/profile \
  -H "Authorization: Bearer 替换为登录返回的tokenValue"
```

携带 Token 访问业务服务上下文接口。

```bash
curl -X GET http://localhost:8080/business/context/current \
  -H "Authorization: Bearer 替换为登录返回的tokenValue"
```

预期响应：

```json
{
  "userId": 1,
  "username": "admin",
  "tenantId": 1001
}
```

这说明链路已经打通：

```text
前端携带 Token
-> Gateway 校验 Token
-> Gateway 写入 X-User-Id / X-Username / X-Tenant-Id
-> business-service 拦截器解析请求头
-> UserContextHolder 获取当前用户
```

### 无权限访问验证

使用普通用户登录：

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ateng",
    "password": "123456",
    "device": "PC"
  }'
```

普通用户访问有权限的查询接口：

```bash
curl -X GET http://localhost:8080/business/users \
  -H "Authorization: Bearer 替换为普通用户tokenValue"
```

普通用户访问无权限的删除接口：

```bash
curl -X DELETE http://localhost:8080/business/users/2 \
  -H "Authorization: Bearer 替换为普通用户tokenValue"
```

预期结果是访问被拒绝。原因是普通用户只有：

```text
user:list
order:list
```

没有：

```text
user:delete
```

管理员访问删除接口应该成功：

```bash
curl -X DELETE http://localhost:8080/business/users/2 \
  -H "Authorization: Bearer 替换为管理员tokenValue"
```

### 退出后访问验证

先退出登录：

```bash
curl -X POST http://localhost:8080/auth/logout \
  -H "Authorization: Bearer 替换为登录返回的tokenValue"
```

再访问当前用户接口：

```bash
curl -X GET http://localhost:8080/auth/profile \
  -H "Authorization: Bearer 替换为刚才已退出的tokenValue"
```

预期返回 `401` 或未登录错误。此时 Token 已失效，Redis 中对应 Token 会话也会被清理。

继续访问业务接口：

```bash
curl -X GET http://localhost:8080/business/context/current \
  -H "Authorization: Bearer 替换为刚才已退出的tokenValue"
```

预期由网关直接拦截，不会进入下游业务服务。

## 本案例完整代码结构

下面是本案例最终涉及的核心代码结构。非核心样板代码，例如完整异常处理、Swagger 配置、Nacos 注册发现、生产级密码加密策略，可以按项目需要继续补充。

### 配置类

```text
auth-token-demo
├── auth-service
│   └── src/main/resources/application.yml
├── gateway-service
│   ├── src/main/resources/application.yml
│   └── src/main/java/io/github/atengk/gateway/config
│       └── GatewayWhiteListProperties.java
└── business-service
    └── src/main/java/io/github/atengk/business/config
        ├── WebMvcConfig.java
        └── FeignUserContextConfig.java
```

配置类职责说明：

| 文件                              | 作用                                       |
| --------------------------------- | ------------------------------------------ |
| `auth-service/application.yml`    | 配置 MySQL、Redis、Sa-Token                |
| `gateway-service/application.yml` | 配置 Gateway 路由、Redis、Sa-Token、白名单 |
| `GatewayWhiteListProperties.java` | 读取网关白名单配置                         |
| `WebMvcConfig.java`               | 注册用户上下文拦截器                       |
| `FeignUserContextConfig.java`     | Feign 服务间调用时透传用户上下文           |

### 实体类与 Mapper

```text
auth-service
└── src/main/java/io/github/atengk/auth
    ├── entity
    │   ├── SysUser.java
    │   ├── SysRole.java
    │   ├── SysPermission.java
    │   ├── SysUserRole.java
    │   └── SysRolePermission.java
    └── mapper
        ├── SysUserMapper.java
        ├── SysRoleMapper.java
        └── SysPermissionMapper.java
```

本案例最核心的是 `SysUserMapper.java`，它负责登录用户查询、角色编码查询、权限编码查询。其他 Mapper 可按 MyBatis-Plus 标准 BaseMapper 创建。

示例：

```java
package io.github.atengk.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.auth.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统角色 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {
}
package io.github.atengk.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.auth.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统权限 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {
}
```

### Service 实现

```text
auth-service
└── src/main/java/io/github/atengk/auth
    ├── service
    │   ├── AuthService.java
    │   ├── UserPermissionService.java
    │   └── impl
    │       ├── AuthServiceImpl.java
    │       └── UserPermissionServiceImpl.java
    └── satoken
        └── StpPermissionImpl.java
```

Service 主要职责如下：

| 类                          | 作用                                     |
| --------------------------- | ---------------------------------------- |
| `AuthService`               | 定义登录、当前用户、续期、退出、踢人下线 |
| `AuthServiceImpl`           | 实现账号密码校验、Token 生成、会话写入   |
| `UserPermissionService`     | 定义角色、权限加载和缓存刷新             |
| `UserPermissionServiceImpl` | 实现 Redis 权限缓存和数据库兜底查询      |
| `StpPermissionImpl`         | 对接 Sa-Token 注解鉴权                   |

### Controller 接口

```text
auth-service
└── src/main/java/io/github/atengk/auth/controller
    ├── AuthController.java
    ├── AuthPermissionController.java
    └── AdminDemoController.java

business-service
└── src/main/java/io/github/atengk/business/controller
    ├── BusinessUserController.java
    └── ContextDemoController.java
```

接口职责如下：

| 接口类                     | 作用                                 |
| -------------------------- | ------------------------------------ |
| `AuthController`           | 登录、当前用户、续期、退出、踢人下线 |
| `AuthPermissionController` | 刷新或清理用户权限缓存               |
| `AdminDemoController`      | 角色校验示例                         |
| `BusinessUserController`   | 权限码校验示例                       |
| `ContextDemoController`    | 用户上下文读取示例                   |

核心接口清单：

| 请求方式 | 路径                                | 说明                         |
| -------- | ----------------------------------- | ---------------------------- |
| `POST`   | `/auth/login`                       | 登录获取 Token               |
| `GET`    | `/auth/profile`                     | 获取当前登录用户             |
| `POST`   | `/auth/renew`                       | Token 活跃续期               |
| `POST`   | `/auth/logout`                      | 退出登录                     |
| `POST`   | `/auth/kickout/{userId}`            | 管理员踢人下线               |
| `POST`   | `/auth/permission/refresh/{userId}` | 刷新用户权限缓存             |
| `DELETE` | `/auth/permission/cache/{userId}`   | 清理用户权限缓存             |
| `GET`    | `/business/users`                   | 用户列表，要求 `user:list`   |
| `POST`   | `/business/users`                   | 新增用户，要求 `user:add`    |
| `DELETE` | `/business/users/{userId}`          | 删除用户，要求 `user:delete` |
| `GET`    | `/business/context/current`         | 获取当前用户上下文           |

### Gateway Filter 实现

```text
gateway-service
└── src/main/java/io/github/atengk/gateway
    ├── GatewayApplication.java
    ├── config
    │   └── GatewayWhiteListProperties.java
    └── filter
        └── TokenAuthGlobalFilter.java
```

Gateway Filter 是微服务统一认证入口，负责：

```text
白名单放行
-> 读取 Authorization
-> 解析 Bearer Token
-> 调用 Sa-Token 校验登录态
-> 从 TokenSession 读取 LoginUser
-> 清理外部伪造的用户请求头
-> 写入可信用户上下文请求头
-> 转发到下游服务
```

网关启动类如下。

文件位置：`gateway-service/src/main/java/io/github/atengk/gateway/GatewayApplication.java`

```java
package io.github.atengk.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 网关服务启动类
 *
 * @author Ateng
 * @since 2026-05-15
 */
@SpringBootApplication
public class GatewayApplication {

    /**
     * 启动网关服务
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

认证服务启动类如下。

文件位置：`auth-service/src/main/java/io/github/atengk/auth/AuthApplication.java`

```java
package io.github.atengk.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 认证服务启动类
 *
 * @author Ateng
 * @since 2026-05-15
 */
@MapperScan("io.github.atengk.auth.mapper")
@SpringBootApplication
public class AuthApplication {

    /**
     * 启动认证服务
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
```

最终完整调用链如下：

```text
前端登录
-> auth-service 校验账号密码
-> Sa-Token 生成 Token
-> Redis 保存 Token 会话
-> 前端携带 Authorization 请求 Gateway
-> gateway-service 校验 Token
-> gateway-service 透传 X-User-Id / X-Username / X-Tenant-Id
-> business-service 解析用户上下文
-> Controller 使用 @SaCheckPermission / @SaCheckRole 校验权限
-> 接口执行业务逻辑
```

该案例已经覆盖统一认证授权与 Token 会话管理的核心功能：登录认证、Token 生成、Redis 会话、Token 续期、退出登录、踢人下线、权限缓存、注解鉴权、网关鉴权和用户上下文透传。