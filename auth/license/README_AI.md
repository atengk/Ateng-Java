# Spring Boot License 系统

## 系统概述

本系统用于为 Spring Boot 业务系统提供统一的 License 授权管理能力，主要解决软件私有化交付、离线部署、试用授权、正式授权、模块授权、机器码绑定和授权到期控制等问题。

系统整体分为两部分：服务端 License 管理系统和客户端 License 校验组件。服务端负责客户、产品、授权记录、License 文件生成和审计日志管理；客户端负责在业务系统启动或运行过程中读取 License 文件，并完成签名、有效期、机器码、产品编码和授权范围校验。

### 建设目标

License 系统的建设目标是建立一套统一、安全、可追溯、可扩展的软件授权机制，避免不同项目各自实现 License 逻辑，降低授权管理和后期维护成本。

主要建设目标如下：

| 目标         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 统一授权管理 | 统一管理客户、产品、版本、授权期限、授权模块和 License 文件  |
| 支持离线校验 | 客户端无需连接授权中心，也可以通过本地 License 文件完成授权校验 |
| 防止文件篡改 | 通过非对称签名保证 License 内容被修改后无法通过校验          |
| 支持机器绑定 | License 可绑定服务器机器码，避免授权文件被复制到其他环境使用 |
| 支持授权扩展 | 支持功能模块、用户数量、租户数量、节点数量等扩展授权字段     |
| 支持过期控制 | License 内置生效时间和过期时间，客户端可自动识别授权状态     |
| 支持审计追踪 | 记录 License 生成、下载、续期、吊销等关键操作                |
| 降低接入成本 | 提供客户端校验组件，业务系统只需引入依赖并完成基础配置即可接入 |

系统建设完成后，应具备以下能力：

1. 服务端可以根据客户、产品、机器码、有效期和授权范围生成 License 文件。
2. License 文件具备防篡改能力，任何非法修改都会导致验签失败。
3. 客户端可以在启动阶段完成 License 校验，校验失败时阻止系统继续运行。
4. 客户端可以在运行阶段校验具体功能模块是否被授权。
5. 管理端可以查询 License 状态、生成记录、下载记录和操作日志。
6. 系统后续可以扩展在线激活、在线吊销、授权心跳和使用量统计能力。

### 应用场景

License 系统主要适用于企业级软件交付场景，尤其适合需要控制客户使用期限、部署环境、功能范围和授权规模的系统。

典型应用场景如下：

| 应用场景       | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| 私有化部署授权 | 产品部署到客户内网环境，通过 License 控制客户是否具备合法使用权 |
| 离线环境授权   | 客户环境无法访问互联网，客户端仅依赖本地 License 文件完成校验 |
| 试用版授权     | 为客户生成短期 License，例如 30 天、60 天、90 天试用授权     |
| 正式版授权     | 为正式购买客户生成固定期限或长期 License                     |
| 模块化授权     | 根据客户购买情况控制功能模块，例如报表、工作流、数据同步等   |
| 节点数授权     | 限制服务可部署的节点数量，适用于集群部署场景                 |
| 用户数授权     | 限制系统允许创建或启用的用户数量                             |
| 租户数授权     | 限制多租户系统中可创建的租户数量                             |
| 项目交付授权   | 不同客户、不同项目、不同产品版本使用独立 License             |
| 授权续期       | License 到期前重新生成新授权文件，客户替换后继续使用         |
| 授权变更       | 客户增购模块、扩容节点、延长有效期后重新签发 License         |

客户端系统通常需要在两个时机进行 License 校验：

1. 启动时校验：校验 License 是否存在、签名是否有效、是否过期、机器码是否匹配。
2. 运行时校验：在访问受控功能前，校验当前功能模块、用户数、租户数或节点数是否在授权范围内。

### 功能边界

License 系统只负责产品授权控制，不负责业务系统内部的用户权限、菜单权限和数据权限。

功能边界如下：

| 功能类型     | 功能说明                                              | 是否包含         |
| ------------ | ----------------------------------------------------- | ---------------- |
| 客户管理     | 维护客户名称、客户编码、联系人、客户状态等信息        | 包含             |
| 产品管理     | 维护产品编码、产品名称、产品版本、授权模块等信息      | 包含             |
| License 申请 | 根据客户、产品、机器码和授权范围创建 License 申请记录 | 包含             |
| License 生成 | 使用服务端私钥生成带签名的 License 文件               | 包含             |
| License 下载 | 提供 License 文件下载能力                             | 包含             |
| License 校验 | 校验签名、有效期、机器码、产品编码和授权范围          | 包含             |
| 操作审计     | 记录 License 生成、下载、续期、吊销等操作             | 包含             |
| 客户端组件   | 提供 License 读取、解析和校验能力                     | 包含             |
| 在线激活     | 客户端连接授权中心完成在线激活                        | 暂不包含，可扩展 |
| 在线吊销     | 服务端远程吊销客户端授权                              | 暂不包含，可扩展 |
| 授权心跳     | 客户端定期上报运行状态和授权使用情况                  | 暂不包含，可扩展 |
| 用户认证     | 登录、Token、角色、菜单权限                           | 不包含           |
| 数据权限     | 业务系统内部的数据范围控制                            | 不包含           |
| 支付订单     | 合同、订单、支付、发票等商业流程                      | 不包含           |

License 系统与业务系统的职责关系如下：

1. License 系统判断“当前客户是否有权使用该产品或功能”。
2. 业务系统判断“当前用户是否有权访问某个菜单、接口或数据”。
3. License 授权属于客户级、产品级、部署级控制。
4. 用户权限属于账号级、角色级、数据级控制。
5. 两者可以结合使用，但不应混淆职责。

## 技术选型

本系统基于 Spring Boot 3 构建服务端授权管理能力，使用非对称签名保证 License 文件不可篡改，使用关系型数据库存储客户、产品、授权记录和操作日志。客户端通过引入 License 校验组件，在启动阶段和运行阶段完成授权校验。

### Spring Boot 3 基础框架

服务端采用 Spring Boot 3.x 作为基础框架，运行环境要求 JDK 17 及以上版本。Spring Boot 3 使用 Jakarta EE 规范，适合新项目建设，也便于后续集成接口文档、安全认证、参数校验、监控探活和数据库访问能力。

推荐技术栈如下：

| 技术                        | 版本建议                  | 用途                                     |
| --------------------------- | ------------------------- | ---------------------------------------- |
| JDK                         | 17+                       | Spring Boot 3 运行环境                   |
| Spring Boot                 | 3.x                       | 后端基础框架                             |
| Spring Web                  | 3.x                       | REST API 接口开发                        |
| Spring Validation           | 3.x                       | 请求参数校验                             |
| Spring Security / Sa-Token  | 最新稳定版                | 管理端接口认证和权限控制                 |
| MyBatis-Plus                | 3.5.x                     | 数据库 CRUD、分页和条件查询              |
| Lombok                      | 最新稳定版                | 简化实体类、DTO、VO 样板代码             |
| Hutool                      | 5.8.x                     | 日期、JSON、Base64、摘要、文件和加密工具 |
| Knife4j / springdoc-openapi | 适配 Spring Boot 3 的版本 | 接口文档                                 |
| MySQL / PostgreSQL          | MySQL 8+ / PostgreSQL 15+ | 核心业务数据存储                         |
| Redis                       | 6+ / 7+                   | 可选，用于缓存、限流、验证码、登录态     |
| Maven                       | 3.8+                      | 项目构建                                 |

推荐 Maven 依赖如下：

```xml
<dependencies>
    <!-- Web 接口开发，提供 REST API、JSON 序列化和内嵌容器能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验，用于校验 DTO 中的 NotBlank、NotNull、Size 等注解 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- MyBatis-Plus，简化单表 CRUD、分页和条件构造器操作 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.7</version>
    </dependency>

    <!-- Hutool，提供 JSON、日期、Base64、摘要、文件和加密相关工具类 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.32</version>
    </dependency>

    <!-- Lombok，减少 Getter、Setter、Builder、构造方法等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Actuator，提供健康检查、运行指标和服务状态监控能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- 安全认证，用于保护 License 管理端接口 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
</dependencies>
```

推荐后端基础分层如下：

```text
src/main/java/io/github/atengk/license
├── LicenseApplication.java
├── common
│   ├── exception
│   ├── result
│   └── util
├── config
├── customer
│   ├── controller
│   ├── service
│   ├── mapper
│   ├── entity
│   ├── dto
│   └── vo
├── product
│   ├── controller
│   ├── service
│   ├── mapper
│   ├── entity
│   ├── dto
│   └── vo
├── license
│   ├── controller
│   ├── service
│   ├── generator
│   ├── verifier
│   ├── model
│   ├── entity
│   ├── dto
│   └── vo
└── audit
    ├── service
    ├── mapper
    └── entity
```

基础框架设计要求如下：

1. 使用 RESTful API 对外提供客户管理、产品管理、License 申请、License 生成和 License 下载接口。
2. 使用 DTO 接收请求参数，使用 VO 返回接口数据，避免直接暴露数据库实体。
3. 使用统一响应结构封装接口返回值，例如 `code`、`message`、`data`。
4. 使用统一异常处理封装参数异常、业务异常和系统异常。
5. 使用 MyBatis-Plus 完成基础 CRUD，复杂查询可通过自定义 Mapper 实现。
6. 使用 Spring Security 或 Sa-Token 保护管理端接口，避免未授权人员生成 License。
7. 使用 Actuator 提供健康检查接口，便于部署平台和运维系统探活。
8. 所有 License 生成、下载、续期、吊销等操作必须记录审计日志。

### License 加密与签名方案

License 文件的核心安全目标是防篡改、可验证、可离线校验。推荐采用“授权内容 + 数字签名”的方式实现。授权内容可以使用 JSON 格式保存，签名用于证明 License 文件由服务端私钥签发，并且内容未被篡改。

需要区分加密和签名：

| 机制        | 作用                                          | 是否必须 |
| ----------- | --------------------------------------------- | -------- |
| 数字签名    | 防止 License 内容被篡改，证明文件由服务端签发 | 必须     |
| 加密        | 防止 License 内容被直接读取                   | 可选     |
| 摘要        | 对 License 内容生成哈希，作为签名前的标准输入 | 建议     |
| Base64 编码 | 便于签名结果存储和传输                        | 建议     |

推荐采用非对称签名方案：

1. 服务端保存私钥，私钥只允许 License 服务端使用。
2. 客户端内置公钥，公钥只用于校验 License 签名。
3. 服务端将授权信息组装为 License Payload。
4. 服务端对 Payload 进行稳定序列化，保证签名前后的字段顺序一致。
5. 服务端使用 `SHA256withRSA` 算法对 Payload 签名。
6. 客户端读取 License 文件后，使用公钥校验签名。
7. 签名校验通过后，再校验有效期、机器码、产品编码和授权范围。

推荐签名算法如下：

| 算法            | 说明                                  | 建议                     |
| --------------- | ------------------------------------- | ------------------------ |
| SHA256withRSA   | 成熟稳定，兼容性好，Java 原生支持完善 | 推荐                     |
| SHA256withECDSA | 密钥更短，签名体积较小                | 可选                     |
| HmacSHA256      | 对称密钥签名，客户端和服务端共享密钥  | 不推荐用于离线客户端场景 |

不建议在离线 License 场景中使用 HMAC 作为核心签名方案。因为客户端必须持有同一份密钥，一旦客户端被反编译，密钥泄露后攻击者就可能伪造 License。非对称签名更适合离线授权场景，客户端只保存公钥，即使公钥泄露，也无法生成合法 License。

License 文件结构建议如下：

```json
{
  "payload": {
    "licenseNo": "LIC-20260506-000001",
    "customerCode": "CUST-001",
    "customerName": "某某科技有限公司",
    "productCode": "PRODUCT-ERP",
    "productVersion": "3.0.0",
    "issuedAt": "2026-05-06 10:00:00",
    "notBefore": "2026-05-06 00:00:00",
    "notAfter": "2027-05-05 23:59:59",
    "machineCode": "b3b6d7f2e8c1a9d0",
    "modules": [
      "system",
      "report",
      "workflow"
    ],
    "limits": {
      "maxUsers": 200,
      "maxTenants": 5,
      "maxNodes": 3
    }
  },
  "signature": "Base64UrlEncodedSignature"
}
```

字段说明如下：

| 字段           | 说明                                 |
| -------------- | ------------------------------------ |
| licenseNo      | License 编号，服务端全局唯一         |
| customerCode   | 客户编码                             |
| customerName   | 客户名称                             |
| productCode    | 产品编码                             |
| productVersion | 产品版本                             |
| issuedAt       | License 签发时间                     |
| notBefore      | License 生效时间                     |
| notAfter       | License 过期时间                     |
| machineCode    | 绑定机器码                           |
| modules        | 已授权功能模块                       |
| limits         | 授权限制，例如用户数、租户数、节点数 |
| signature      | 服务端私钥生成的数字签名             |

服务端生成流程如下：

```text
1. 查询客户、产品和授权申请信息
2. 组装 License Payload
3. 对 Payload 字段进行稳定排序和标准化序列化
4. 使用服务端私钥对 Payload 生成签名
5. 将 Payload 和 Signature 写入 License 文件
6. 保存 License 记录、文件路径、文件摘要和操作日志
```

客户端校验流程如下：

```text
1. 读取本地 License 文件
2. 解析 Payload 和 Signature
3. 使用与服务端一致的规则序列化 Payload
4. 使用公钥校验 Signature
5. 校验产品编码是否匹配当前系统
6. 校验机器码是否匹配当前服务器
7. 校验当前时间是否在有效期范围内
8. 校验功能模块和授权数量是否满足业务要求
9. 校验通过后允许系统启动或允许访问受控功能
```

安全设计注意事项如下：

1. 私钥只能存放在 License 服务端，不能下发到客户端。
2. 客户端只允许保存公钥，不能包含任何可以生成合法 License 的密钥。
3. Payload 签名前必须进行稳定序列化，避免字段顺序不同导致验签失败。
4. 不应使用“私钥加密、公钥解密”替代标准签名流程。
5. 如果 License 内容包含敏感商业信息，可以先对 Payload 加密，再对密文进行签名。
6. 客户端校验失败时应输出明确日志，例如 License 不存在、签名无效、机器码不匹配、License 已过期。
7. 客户端不应只在启动时校验 License，关键功能也应支持运行时授权校验。

### 数据存储方案

License 服务端需要持久化客户信息、产品信息、License 授权记录和操作审计日志。数据存储应以关系型数据库为主，缓存和文件存储按实际部署规模选配。

推荐存储方案如下：

| 存储类型     | 推荐技术                              | 用途                                         |
| ------------ | ------------------------------------- | -------------------------------------------- |
| 关系型数据库 | MySQL 8+ / PostgreSQL 15+             | 存储客户、产品、License、审计日志等核心数据  |
| 缓存         | Redis                                 | 可选，用于登录态、验证码、接口限流和短期缓存 |
| 文件存储     | 本地磁盘 / MinIO / 对象存储           | 存储生成后的 License 文件                    |
| 配置存储     | application.yml / 环境变量 / 配置中心 | 存储数据库连接、公私钥路径、文件目录等配置   |
| 日志存储     | 本地日志 / ELK / Loki                 | 存储系统运行日志和排障日志                   |

核心数据表建议如下：

| 表名                  | 说明                                                         |
| --------------------- | ------------------------------------------------------------ |
| license_customer      | 客户信息表，维护客户主体信息                                 |
| license_product       | 产品信息表，维护产品编码、产品名称和产品版本                 |
| license_authorization | License 授权表，维护授权范围、机器码、有效期、状态和文件信息 |
| license_operation_log | License 操作日志表，记录生成、下载、续期、吊销等操作         |

License 文件存储方式建议如下：

| 存储方式     | 说明                                                         | 适用场景                                 |
| ------------ | ------------------------------------------------------------ | ---------------------------------------- |
| 数据库存储   | 将 License 文件内容或 Base64 内容直接存入数据库字段          | 系统简单、文件较小、下载频率较低         |
| 文件系统存储 | 数据库保存文件路径、文件名和摘要，License 文件存储在磁盘或对象存储中 | 文件需要下载、归档、备份、迁移或统一管理 |

推荐采用“数据库保存元数据 + 文件系统保存 License 文件”的方式。数据库负责查询、状态管理和审计追踪，文件系统或对象存储负责 License 文件下载和归档。

License 授权状态建议如下：

| 状态       | 说明                                      |
| ---------- | ----------------------------------------- |
| DRAFT      | 草稿，授权申请已创建但未生成 License 文件 |
| ISSUED     | 已签发，License 文件已生成                |
| DOWNLOADED | 已下载，客户或交付人员已获取 License 文件 |
| REVOKED    | 已吊销，该 License 不应继续使用           |
| EXPIRED    | 已过期，超过授权结束时间                  |
| RENEWED    | 已续期，已有新的 License 替代当前授权     |

基础配置示例如下：

```yaml
server:
  port: 8080 # License 服务端口

spring:
  application:
    name: springboot-license-server # 服务名称

  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver # MySQL 8 驱动
    url: jdbc:mysql://127.0.0.1:3306/license_server?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: license_user # 数据库账号，生产环境建议通过环境变量注入
    password: license_password # 数据库密码，生产环境禁止提交到代码仓库

  data:
    redis:
      host: 127.0.0.1 # Redis 地址，可选
      port: 6379 # Redis 端口
      database: 0 # Redis 逻辑库

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true # 数据库下划线字段自动映射为 Java 驼峰字段
  global-config:
    db-config:
      id-type: assign_id # 使用雪花算法生成主键

license:
  crypto:
    algorithm: SHA256withRSA # License 签名算法
    private-key-path: /data/license/keys/private_key.pem # 服务端私钥路径，仅服务端保存
    public-key-path: /data/license/keys/public_key.pem # 公钥路径，用于客户端验签
  storage:
    type: local # 文件存储类型，可扩展为 local、minio、oss
    local-path: /data/license/files # License 文件本地存储目录
  verify:
    allow-clock-skew-seconds: 300 # 允许客户端时间误差，避免轻微时间偏差导致误判
```

数据存储设计要求如下：

1. License 编号必须唯一，建议使用业务前缀、日期和序列号组合生成。
2. 客户编码和产品编码应保持唯一，避免客户名称或产品名称变更影响历史授权记录。
3. License 授权表应保存机器码、授权模块、授权限制、有效期、签名摘要和文件路径。
4. 授权模块和授权限制建议使用 JSON 字段存储，便于后续扩展。
5. 操作日志应记录操作人、操作类型、操作时间、请求 IP 和关键业务数据。
6. License 文件应保存 SHA-256 摘要，便于下载后进行完整性校验。
7. 应对 License 编号、客户编码、产品编码、机器码、状态和过期时间建立索引。
8. 私钥路径、数据库密码、文件存储密钥等敏感配置应通过环境变量或部署配置注入，不应硬编码在代码中。



## 核心业务设计

核心业务设计用于描述 License 从申请、生成、发放、校验、续期到失效的完整业务过程。该部分是后续数据库设计、接口设计、客户端集成和异常处理的基础。

### License 生命周期

License 生命周期用于描述一个授权文件从创建到最终失效的完整状态流转。系统应通过明确的状态控制 License 的业务行为，避免已吊销、已过期或被替代的 License 被继续使用。

License 生命周期主要包括以下阶段：

| 阶段       | 状态       | 说明                                                        |
| ---------- | ---------- | ----------------------------------------------------------- |
| 创建申请   | DRAFT      | 管理端创建 License 授权申请，但尚未生成 License 文件        |
| 签发授权   | ISSUED     | 服务端使用私钥生成 License 文件，并保存授权记录             |
| 文件下载   | DOWNLOADED | 授权文件已被交付人员或客户下载                              |
| 客户端使用 | ACTIVE     | 客户端校验通过，License 处于可用状态                        |
| 授权续期   | RENEWED    | 原 License 已被新的 License 替代                            |
| 授权吊销   | REVOKED    | 管理端主动吊销 License，在线模式下客户端应拒绝继续使用      |
| 授权过期   | EXPIRED    | 当前时间超过 License 有效期，客户端应拒绝启动或限制受控功能 |

推荐状态流转如下：

```text
DRAFT -> ISSUED -> DOWNLOADED -> ACTIVE
                 -> REVOKED
                 -> EXPIRED
                 -> RENEWED
```

状态流转规则如下：

| 当前状态                     | 允许操作       | 目标状态   | 说明                                             |
| ---------------------------- | -------------- | ---------- | ------------------------------------------------ |
| DRAFT                        | 生成 License   | ISSUED     | 根据授权申请生成 License 文件                    |
| ISSUED                       | 下载 License   | DOWNLOADED | 下载后记录下载时间和下载人                       |
| ISSUED / DOWNLOADED          | 客户端校验通过 | ACTIVE     | 客户端实际使用时的逻辑状态，通常不一定回写服务端 |
| ISSUED / DOWNLOADED / ACTIVE | 吊销 License   | REVOKED    | 管理端主动作废授权                               |
| ISSUED / DOWNLOADED / ACTIVE | 授权到期       | EXPIRED    | 可通过定时任务或查询时动态判断                   |
| ISSUED / DOWNLOADED / ACTIVE | 续期 License   | RENEWED    | 生成新 License 后，将旧 License 标记为已续期     |

生命周期设计要求如下：

1. `DRAFT` 状态只表示申请记录，不应允许下载 License 文件。
2. `ISSUED` 状态表示 License 文件已经生成，可以下载和交付。
3. `DOWNLOADED` 状态表示 License 文件已被获取，用于审计交付行为。
4. `ACTIVE` 更多是客户端运行时状态，离线模式下一般不依赖服务端实时维护。
5. `REVOKED` 状态表示服务端已吊销授权，在线校验模式下客户端应拒绝继续使用。
6. `EXPIRED` 可由服务端定时任务更新，也可在查询时根据 `expire_time` 动态判断。
7. `RENEWED` 表示当前 License 已有替代授权，历史记录仍需保留，不应物理删除。

### 授权信息模型

授权信息模型用于定义 License 文件中包含的核心业务数据。该模型应同时满足服务端生成、客户端校验和后续扩展需求。

License 授权信息建议分为基础信息、客户信息、产品信息、机器绑定信息、有效期信息、功能授权信息和限制信息。

授权信息模型如下：

| 分类     | 字段           | 说明                                          |
| -------- | -------------- | --------------------------------------------- |
| 基础信息 | licenseNo      | License 编号，全局唯一                        |
| 基础信息 | licenseType    | License 类型，例如 TRIAL、OFFICIAL、TEMPORARY |
| 基础信息 | issuedAt       | 签发时间                                      |
| 客户信息 | customerCode   | 客户编码                                      |
| 客户信息 | customerName   | 客户名称                                      |
| 产品信息 | productCode    | 产品编码                                      |
| 产品信息 | productName    | 产品名称                                      |
| 产品信息 | productVersion | 产品版本                                      |
| 机器绑定 | machineCode    | 绑定机器码                                    |
| 机器绑定 | machineInfo    | 机器摘要信息，可选                            |
| 有效期   | notBefore      | 生效时间                                      |
| 有效期   | notAfter       | 过期时间                                      |
| 授权模块 | modules        | 已授权模块列表                                |
| 授权限制 | limits         | 授权数量限制，例如用户数、租户数、节点数      |
| 扩展字段 | extra          | 扩展参数，便于后续业务增强                    |

推荐 License Payload 结构如下：

```json
{
  "licenseNo": "LIC-20260506-000001",
  "licenseType": "OFFICIAL",
  "customer": {
    "customerCode": "CUST-001",
    "customerName": "某某科技有限公司"
  },
  "product": {
    "productCode": "PRODUCT-ERP",
    "productName": "企业管理平台",
    "productVersion": "3.0.0"
  },
  "machine": {
    "machineCode": "8f2a7d0b6e9c4a11",
    "machineInfo": {
      "osName": "Linux",
      "cpuSerial": "CPU-XXX",
      "mainBoardSerial": "BOARD-XXX",
      "macAddress": "00-16-3E-XX-XX-XX"
    }
  },
  "validity": {
    "issuedAt": "2026-05-06 10:00:00",
    "notBefore": "2026-05-06 00:00:00",
    "notAfter": "2027-05-05 23:59:59"
  },
  "authorization": {
    "modules": [
      "system",
      "report",
      "workflow"
    ],
    "limits": {
      "maxUsers": 200,
      "maxTenants": 5,
      "maxNodes": 3
    }
  },
  "extra": {
    "remark": "正式授权"
  }
}
```

授权信息模型设计要求如下：

1. `licenseNo` 必须全局唯一，建议由服务端统一生成。
2. `customerCode` 和 `productCode` 应使用稳定编码，不应依赖可变名称。
3. `machineCode` 应由客户端工具生成，再提交给服务端用于签发 License。
4. `notBefore` 和 `notAfter` 必须写入 License 文件，客户端本地即可完成有效期校验。
5. `modules` 用于控制功能模块是否可用。
6. `limits` 用于控制用户数、租户数、节点数、并发数等授权规模。
7. `extra` 仅用于扩展，不应承载关键校验逻辑。
8. 服务端数据库中的授权信息应与 License 文件中的 Payload 保持一致，便于后续追溯。

### 机器码绑定机制

机器码绑定机制用于限制 License 文件只能在指定服务器或指定部署环境中使用，避免授权文件被复制到其他机器后继续生效。

机器码通常由客户端采集本机硬件和系统信息后生成。为了避免暴露原始硬件信息，客户端不应直接上传完整硬件明文，而应对关键字段进行标准化处理后生成摘要。

推荐采集信息如下：

| 信息项       | 说明                                     | 是否建议 |
| ------------ | ---------------------------------------- | -------- |
| CPU 序列号   | 服务器 CPU 标识，不同环境可用性不同      | 可选     |
| 主板序列号   | 物理机绑定稳定性较好，虚拟机中可能不稳定 | 建议     |
| 磁盘序列号   | 可用于增强唯一性，云服务器中可能变化     | 可选     |
| MAC 地址     | 获取简单，但容器、虚拟机环境可能变化     | 建议     |
| 操作系统名称 | 辅助信息，不应单独作为机器码依据         | 建议     |
| 主机名       | 容易修改，只适合作为辅助信息             | 可选     |
| 容器 ID      | 容器化部署时可选，但重建容器后可能变化   | 可选     |

机器码生成流程如下：

```text
1. 客户端采集 CPU、主板、磁盘、MAC 地址、操作系统等信息
2. 过滤空值、无效值和明显不稳定的字段
3. 对字段按固定顺序排序
4. 拼接为标准字符串
5. 使用 SHA-256 生成摘要
6. 截取摘要前 16 位或 32 位作为 machineCode
7. 客户将 machineCode 提交给授权服务端
8. 服务端将 machineCode 写入 License Payload 并签名
```

机器码绑定校验流程如下：

```text
1. 客户端启动时重新采集当前机器信息
2. 使用相同规则生成当前机器 machineCode
3. 读取 License 文件中的 machineCode
4. 比较当前 machineCode 与 License machineCode
5. 一致则通过机器绑定校验
6. 不一致则抛出机器码不匹配异常
```

机器码绑定设计要求如下：

1. 机器码生成规则必须稳定，客户端和机器码采集工具应使用同一套实现。
2. 应避免只使用 MAC 地址作为唯一依据，因为 MAC 地址容易变化或被修改。
3. 云服务器、容器、虚拟机环境中的硬件信息可能不稳定，应支持配置采集策略。
4. 对于集群部署场景，可以支持多个机器码绑定一个 License。
5. 对于容器化部署场景，可以选择绑定宿主机机器码、节点标识或部署环境标识。
6. License 文件中不建议保存完整硬件明文，可只保存机器码摘要。
7. 机器码不匹配时应输出明确错误日志，方便交付人员定位问题。

集群授权场景下，License 可以支持多个机器码：

```json
{
  "machine": {
    "bindType": "MULTI_NODE",
    "machineCodes": [
      "8f2a7d0b6e9c4a11",
      "19b7c2f0a6d4e931",
      "43c9a72f0e6a812b"
    ],
    "maxNodes": 3
  }
}
```

### License 生成机制

License 生成机制用于描述服务端如何根据客户、产品、机器码和授权范围生成不可篡改的 License 文件。

服务端生成 License 时，必须使用私钥进行签名。私钥只能存在于 License 服务端，不能出现在客户端、前端代码、测试包或交付包中。

License 生成流程如下：

```text
1. 管理员创建 License 申请
2. 选择客户、产品、产品版本、License 类型和授权有效期
3. 填写机器码、授权模块和授权限制
4. 服务端校验客户、产品、授权范围和机器码是否合法
5. 服务端生成 License 编号
6. 服务端组装 License Payload
7. 服务端对 Payload 进行稳定序列化
8. 服务端使用私钥对 Payload 签名
9. 服务端生成 License 文件
10. 服务端保存授权记录、文件路径、文件摘要和操作日志
11. 管理员下载 License 文件并交付客户
```

License 生成规则如下：

| 规则       | 说明                                   |
| ---------- | -------------------------------------- |
| 编号唯一   | `licenseNo` 必须全局唯一，不能重复生成 |
| 时间合法   | `notBefore` 必须小于 `notAfter`        |
| 客户有效   | 客户状态必须为启用状态                 |
| 产品有效   | 产品状态必须为启用状态                 |
| 机器码有效 | 机器码不能为空，格式必须符合规则       |
| 模块合法   | 授权模块必须属于当前产品支持的模块     |
| 限制合法   | 用户数、租户数、节点数等限制不能为负数 |
| 签名可靠   | 必须使用服务端私钥签名                 |
| 文件可追溯 | 必须记录文件路径、文件摘要和操作日志   |

License 文件建议使用 `.lic` 后缀，例如：

```text
LIC-20260506-000001.lic
```

License 文件内容建议采用如下结构：

```json
{
  "payload": {
    "licenseNo": "LIC-20260506-000001",
    "licenseType": "OFFICIAL",
    "customer": {
      "customerCode": "CUST-001",
      "customerName": "某某科技有限公司"
    },
    "product": {
      "productCode": "PRODUCT-ERP",
      "productName": "企业管理平台",
      "productVersion": "3.0.0"
    },
    "machine": {
      "machineCode": "8f2a7d0b6e9c4a11"
    },
    "validity": {
      "issuedAt": "2026-05-06 10:00:00",
      "notBefore": "2026-05-06 00:00:00",
      "notAfter": "2027-05-05 23:59:59"
    },
    "authorization": {
      "modules": [
        "system",
        "report",
        "workflow"
      ],
      "limits": {
        "maxUsers": 200,
        "maxTenants": 5,
        "maxNodes": 3
      }
    }
  },
  "signature": "Base64UrlEncodedSignature",
  "algorithm": "SHA256withRSA"
}
```

生成机制设计要求如下：

1. License 文件生成后不建议修改原文件，授权变更应重新生成新的 License。
2. 每次生成 License 都应记录操作日志，包含操作人、操作时间、客户、产品、授权范围和请求 IP。
3. 文件摘要建议使用 SHA-256 生成，并保存到数据库中。
4. 下载 License 文件时可重新计算文件摘要，与数据库摘要比对，确保文件未损坏。
5. License 文件内容应使用 UTF-8 编码。
6. Payload 序列化必须保持稳定，推荐统一使用字段排序后的 JSON 字符串。
7. 如果启用 Payload 加密，应采用“先加密 Payload，再对密文签名”的方式。

### License 校验机制

License 校验机制用于描述客户端如何判断 License 文件是否有效。客户端校验应覆盖文件存在性、文件格式、签名、产品、机器码、有效期和授权范围。

客户端启动时应执行完整校验，校验失败时应阻止业务系统继续启动，或根据项目要求进入受限模式。

License 校验流程如下：

```text
1. 读取 License 配置，获取 License 文件路径
2. 判断 License 文件是否存在
3. 解析 License 文件内容
4. 校验文件结构是否完整
5. 提取 payload、signature 和 algorithm
6. 使用公钥校验 signature
7. 校验产品编码是否匹配当前应用
8. 生成当前机器 machineCode
9. 校验 machineCode 是否匹配
10. 校验当前时间是否在 notBefore 和 notAfter 范围内
11. 校验授权模块是否满足当前系统要求
12. 校验用户数、租户数、节点数等授权限制
13. 校验通过后缓存 License 上下文
```

校验项如下：

| 校验项     | 说明                               | 失败处理                    |
| ---------- | ---------------------------------- | --------------------------- |
| 文件存在性 | 判断 License 文件是否存在          | 抛出 License 文件不存在异常 |
| 文件格式   | 判断 JSON 结构是否正确             | 抛出 License 文件格式异常   |
| 签名有效性 | 使用公钥校验签名                   | 抛出 License 签名无效异常   |
| 产品编码   | 校验 License 是否属于当前产品      | 抛出产品不匹配异常          |
| 机器码     | 校验 License 是否绑定当前机器      | 抛出机器码不匹配异常        |
| 生效时间   | 当前时间不能早于 `notBefore`       | 抛出 License 未生效异常     |
| 过期时间   | 当前时间不能晚于 `notAfter`        | 抛出 License 已过期异常     |
| 授权模块   | 当前访问模块必须在授权列表内       | 拒绝访问受控功能            |
| 授权限制   | 用户数、租户数、节点数不能超过限制 | 拒绝新增或拒绝启动          |

运行时授权校验可以分为以下几类：

| 校验类型 | 校验时机       | 示例                               |
| -------- | -------------- | ---------------------------------- |
| 启动校验 | 应用启动时     | License 文件、签名、机器码、有效期 |
| 模块校验 | 访问功能前     | 判断 `report` 模块是否授权         |
| 数量校验 | 新增资源前     | 判断用户数量是否超过 `maxUsers`    |
| 节点校验 | 集群节点启动时 | 判断当前节点数是否超过 `maxNodes`  |
| 定时校验 | 后台定时任务   | 定期检查 License 是否过期          |

客户端校验通过后，建议将 License 信息缓存为运行时上下文，例如：

| 上下文字段   | 说明                  |
| ------------ | --------------------- |
| licenseNo    | 当前 License 编号     |
| customerCode | 当前客户编码          |
| productCode  | 当前产品编码          |
| modules      | 当前已授权模块        |
| limits       | 当前授权限制          |
| notAfter     | 当前 License 过期时间 |
| machineCode  | 当前机器码            |
| verifiedAt   | 最近一次校验时间      |

校验机制设计要求如下：

1. 客户端必须使用公钥验签，不能绕过签名校验直接读取 Payload。
2. 客户端应在启动阶段执行完整校验，避免系统在无授权状态下运行。
3. 客户端应提供运行时校验方法，支持业务功能按模块或数量进行授权判断。
4. 客户端应输出明确日志，便于区分签名无效、过期、机器码不匹配等问题。
5. 客户端可对 License 上下文进行缓存，但不能因为缓存而永久跳过文件校验。
6. 对于长时间运行的服务，应增加定时校验，避免 License 过期后继续运行。
7. 校验异常应使用明确的异常类型，便于业务系统统一处理。

### License 过期与失效处理

License 过期与失效处理用于描述授权不可继续使用时系统应如何响应。失效原因包括过期、吊销、机器码不匹配、签名无效、产品不匹配和授权范围不足。

License 失效类型如下：

| 失效类型     | 说明                             | 处理方式                      |
| ------------ | -------------------------------- | ----------------------------- |
| 文件不存在   | 未配置或未上传 License 文件      | 阻止启动，提示上传 License    |
| 签名无效     | License 内容被篡改或签名不匹配   | 阻止启动，提示签名无效        |
| 产品不匹配   | License 产品编码与当前系统不一致 | 阻止启动，提示产品不匹配      |
| 机器码不匹配 | License 绑定机器与当前机器不一致 | 阻止启动，提示机器码不匹配    |
| 未到生效时间 | 当前时间早于 License 生效时间    | 阻止启动，提示 License 未生效 |
| 已过期       | 当前时间晚于 License 过期时间    | 阻止启动或进入受限模式        |
| 已吊销       | 服务端标记 License 已吊销        | 在线校验模式下拒绝使用        |
| 授权不足     | 当前功能或数量超过授权范围       | 拒绝访问对应功能              |

过期处理策略如下：

| 策略     | 说明                                           | 适用场景               |
| -------- | ---------------------------------------------- | ---------------------- |
| 阻止启动 | License 已过期时应用直接启动失败               | 强授权控制系统         |
| 受限模式 | 系统允许启动，但禁用新增、导出、核心业务等功能 | 对连续性要求较高的系统 |
| 宽限期   | License 到期后允许继续使用固定天数             | 商业上允许缓冲的项目   |
| 只读模式 | 系统允许查询历史数据，但禁止新增和修改         | 数据平台、管理系统     |
| 在线复核 | 客户端连接授权中心确认是否续期或吊销           | 支持联网的客户环境     |

推荐默认策略如下：

1. License 文件不存在、签名无效、产品不匹配、机器码不匹配时，直接阻止系统启动。
2. License 已过期时，默认阻止启动；如业务要求高可配置宽限期或只读模式。
3. 授权模块不足时，不影响系统启动，但访问对应功能时应拒绝。
4. 授权数量超限时，应禁止继续新增资源，并提示当前授权限制。
5. License 已吊销只在在线校验或授权中心可达时生效，纯离线场景无法实时感知吊销状态。

过期提醒机制如下：

| 提醒时间     | 处理方式                               |
| ------------ | -------------------------------------- |
| 到期前 30 天 | 管理端或客户端输出提醒日志             |
| 到期前 15 天 | 首页或系统通知提示 License 即将过期    |
| 到期前 7 天  | 提高提醒频率，提示联系管理员续期       |
| 到期当天     | 提示 License 今日到期                  |
| 到期后       | 按配置进入阻止启动、受限模式或只读模式 |

失效处理设计要求如下：

1. 客户端应提供统一的 License 异常码和异常信息。
2. 日志中应输出 License 编号、产品编码、过期时间和失败原因。
3. 不应在日志中输出私钥、完整签名、敏感客户信息等内容。
4. 管理端应支持查询即将过期和已过期 License。
5. 服务端可通过定时任务将已过期授权标记为 `EXPIRED`。
6. License 续期应生成新文件，不应直接修改旧 License 文件。
7. 已吊销、已过期、已续期的 License 记录应保留，便于审计追踪。

## 数据库设计

数据库设计用于支撑客户管理、产品管理、License 授权管理和操作审计。以下表结构以 MySQL 8 为示例，主键采用雪花 ID，时间字段统一使用 `datetime`，扩展授权字段使用 `json` 类型保存。

数据库命名约定如下：

| 约定     | 说明                                                  |
| -------- | ----------------------------------------------------- |
| 表名前缀 | 使用 `license_` 前缀区分授权系统表                    |
| 主键字段 | 统一使用 `id`                                         |
| 编码字段 | 客户编码、产品编码、License 编号均使用唯一索引        |
| 时间字段 | 创建时间、更新时间、签发时间、过期时间使用 `datetime` |
| 删除字段 | 使用 `deleted` 表示逻辑删除                           |
| 状态字段 | 使用 `status` 表示业务状态                            |
| 扩展字段 | 使用 `json` 保存模块、限制、扩展参数等半结构化数据    |

### 客户信息表

客户信息表用于维护被授权客户的基础资料。License 生成时需要选择客户，并将客户编码和客户名称写入授权记录和 License 文件。

表名：`license_customer`

字段设计如下：

| 字段名        | 类型         | 是否必填 | 说明                        |
| ------------- | ------------ | -------- | --------------------------- |
| id            | bigint       | 是       | 主键 ID                     |
| customer_code | varchar(64)  | 是       | 客户编码，全局唯一          |
| customer_name | varchar(128) | 是       | 客户名称                    |
| contact_name  | varchar(64)  | 否       | 联系人姓名                  |
| contact_phone | varchar(32)  | 否       | 联系电话                    |
| contact_email | varchar(128) | 否       | 联系邮箱                    |
| industry      | varchar(64)  | 否       | 所属行业                    |
| region        | varchar(128) | 否       | 所属地区                    |
| status        | varchar(32)  | 是       | 客户状态：ENABLED、DISABLED |
| remark        | varchar(512) | 否       | 备注                        |
| created_by    | varchar(64)  | 否       | 创建人                      |
| created_time  | datetime     | 是       | 创建时间                    |
| updated_by    | varchar(64)  | 否       | 更新人                      |
| updated_time  | datetime     | 是       | 更新时间                    |
| deleted       | tinyint      | 是       | 是否删除：0 否，1 是        |

建表语句如下：

```sql
CREATE TABLE license_customer (
    id BIGINT NOT NULL COMMENT '主键ID',
    customer_code VARCHAR(64) NOT NULL COMMENT '客户编码',
    customer_name VARCHAR(128) NOT NULL COMMENT '客户名称',
    contact_name VARCHAR(64) DEFAULT NULL COMMENT '联系人姓名',
    contact_phone VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
    contact_email VARCHAR(128) DEFAULT NULL COMMENT '联系邮箱',
    industry VARCHAR(64) DEFAULT NULL COMMENT '所属行业',
    region VARCHAR(128) DEFAULT NULL COMMENT '所属地区',
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '客户状态：ENABLED启用，DISABLED禁用',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    created_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0否，1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_code (customer_code),
    KEY idx_customer_name (customer_name),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户信息表';
```

设计说明如下：

1. `customer_code` 是客户稳定标识，不应因客户名称变更而变化。
2. `customer_name` 用于展示和写入 License 文件。
3. `status` 为 `DISABLED` 时，不允许继续生成新的 License。
4. 客户删除建议使用逻辑删除，避免历史 License 记录失去关联依据。

### 产品信息表

产品信息表用于维护可授权的软件产品和版本信息。License 生成时需要选择产品，并根据产品配置限制可授权模块。

表名：`license_product`

字段设计如下：

| 字段名          | 类型         | 是否必填 | 说明                        |
| --------------- | ------------ | -------- | --------------------------- |
| id              | bigint       | 是       | 主键 ID                     |
| product_code    | varchar(64)  | 是       | 产品编码，全局唯一          |
| product_name    | varchar(128) | 是       | 产品名称                    |
| product_version | varchar(64)  | 是       | 产品版本                    |
| product_type    | varchar(64)  | 否       | 产品类型                    |
| module_config   | json         | 否       | 产品支持的模块配置          |
| default_limits  | json         | 否       | 默认授权限制                |
| public_key      | text         | 否       | 产品公钥，可选              |
| status          | varchar(32)  | 是       | 产品状态：ENABLED、DISABLED |
| remark          | varchar(512) | 否       | 备注                        |
| created_by      | varchar(64)  | 否       | 创建人                      |
| created_time    | datetime     | 是       | 创建时间                    |
| updated_by      | varchar(64)  | 否       | 更新人                      |
| updated_time    | datetime     | 是       | 更新时间                    |
| deleted         | tinyint      | 是       | 是否删除：0 否，1 是        |

建表语句如下：

```sql
CREATE TABLE license_product (
    id BIGINT NOT NULL COMMENT '主键ID',
    product_code VARCHAR(64) NOT NULL COMMENT '产品编码',
    product_name VARCHAR(128) NOT NULL COMMENT '产品名称',
    product_version VARCHAR(64) NOT NULL COMMENT '产品版本',
    product_type VARCHAR(64) DEFAULT NULL COMMENT '产品类型',
    module_config JSON DEFAULT NULL COMMENT '产品支持的模块配置',
    default_limits JSON DEFAULT NULL COMMENT '默认授权限制',
    public_key TEXT DEFAULT NULL COMMENT '产品公钥，可选',
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '产品状态：ENABLED启用，DISABLED禁用',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    created_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0否，1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_code_version (product_code, product_version),
    KEY idx_product_code (product_code),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品信息表';
```

`module_config` 示例：

```json
[
  {
    "moduleCode": "system",
    "moduleName": "系统管理"
  },
  {
    "moduleCode": "report",
    "moduleName": "报表中心"
  },
  {
    "moduleCode": "workflow",
    "moduleName": "流程管理"
  }
]
```

`default_limits` 示例：

```json
{
  "maxUsers": 100,
  "maxTenants": 1,
  "maxNodes": 1
}
```

设计说明如下：

1. `product_code` 和 `product_version` 组合唯一，用于区分不同产品版本。
2. `module_config` 用于限制 License 生成时可选择的授权模块。
3. `default_limits` 用于生成 License 时自动填充默认授权限制。
4. 如果不同产品使用不同密钥对，可以在产品维度保存公钥标识或公钥内容。
5. 产品禁用后，不允许继续生成新的 License，但不影响历史 License 查询。

### License 授权表

License 授权表用于保存每一次 License 申请、生成、下载、续期、吊销和过期状态，是系统的核心业务表。

表名：`license_authorization`

字段设计如下：

| 字段名              | 类型         | 是否必填 | 说明                                                       |
| ------------------- | ------------ | -------- | ---------------------------------------------------------- |
| id                  | bigint       | 是       | 主键 ID                                                    |
| license_no          | varchar(64)  | 是       | License 编号，全局唯一                                     |
| license_type        | varchar(32)  | 是       | License 类型：TRIAL、OFFICIAL、TEMPORARY                   |
| customer_id         | bigint       | 是       | 客户 ID                                                    |
| customer_code       | varchar(64)  | 是       | 客户编码                                                   |
| customer_name       | varchar(128) | 是       | 客户名称                                                   |
| product_id          | bigint       | 是       | 产品 ID                                                    |
| product_code        | varchar(64)  | 是       | 产品编码                                                   |
| product_name        | varchar(128) | 是       | 产品名称                                                   |
| product_version     | varchar(64)  | 是       | 产品版本                                                   |
| machine_code        | varchar(128) | 是       | 机器码                                                     |
| machine_info        | json         | 否       | 机器信息摘要                                               |
| authorized_modules  | json         | 否       | 授权模块                                                   |
| authorized_limits   | json         | 否       | 授权限制                                                   |
| issued_time         | datetime     | 否       | 签发时间                                                   |
| effective_time      | datetime     | 是       | 生效时间                                                   |
| expire_time         | datetime     | 是       | 过期时间                                                   |
| file_name           | varchar(255) | 否       | License 文件名                                             |
| file_path           | varchar(512) | 否       | License 文件路径                                           |
| file_sha256         | varchar(128) | 否       | License 文件 SHA-256 摘要                                  |
| signature_algorithm | varchar(64)  | 否       | 签名算法                                                   |
| signature_value     | text         | 否       | 签名值                                                     |
| parent_license_no   | varchar(64)  | 否       | 原 License 编号，用于续期或变更                            |
| status              | varchar(32)  | 是       | 状态：DRAFT、ISSUED、DOWNLOADED、REVOKED、EXPIRED、RENEWED |
| revoke_reason       | varchar(512) | 否       | 吊销原因                                                   |
| downloaded_time     | datetime     | 否       | 最近下载时间                                               |
| downloaded_by       | varchar(64)  | 否       | 最近下载人                                                 |
| remark              | varchar(512) | 否       | 备注                                                       |
| created_by          | varchar(64)  | 否       | 创建人                                                     |
| created_time        | datetime     | 是       | 创建时间                                                   |
| updated_by          | varchar(64)  | 否       | 更新人                                                     |
| updated_time        | datetime     | 是       | 更新时间                                                   |
| deleted             | tinyint      | 是       | 是否删除：0 否，1 是                                       |

建表语句如下：

```sql
CREATE TABLE license_authorization (
    id BIGINT NOT NULL COMMENT '主键ID',
    license_no VARCHAR(64) NOT NULL COMMENT 'License编号',
    license_type VARCHAR(32) NOT NULL COMMENT 'License类型：TRIAL试用，OFFICIAL正式，TEMPORARY临时',
    customer_id BIGINT NOT NULL COMMENT '客户ID',
    customer_code VARCHAR(64) NOT NULL COMMENT '客户编码',
    customer_name VARCHAR(128) NOT NULL COMMENT '客户名称',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    product_code VARCHAR(64) NOT NULL COMMENT '产品编码',
    product_name VARCHAR(128) NOT NULL COMMENT '产品名称',
    product_version VARCHAR(64) NOT NULL COMMENT '产品版本',
    machine_code VARCHAR(128) NOT NULL COMMENT '机器码',
    machine_info JSON DEFAULT NULL COMMENT '机器信息摘要',
    authorized_modules JSON DEFAULT NULL COMMENT '授权模块',
    authorized_limits JSON DEFAULT NULL COMMENT '授权限制',
    issued_time DATETIME DEFAULT NULL COMMENT '签发时间',
    effective_time DATETIME NOT NULL COMMENT '生效时间',
    expire_time DATETIME NOT NULL COMMENT '过期时间',
    file_name VARCHAR(255) DEFAULT NULL COMMENT 'License文件名',
    file_path VARCHAR(512) DEFAULT NULL COMMENT 'License文件路径',
    file_sha256 VARCHAR(128) DEFAULT NULL COMMENT 'License文件SHA-256摘要',
    signature_algorithm VARCHAR(64) DEFAULT NULL COMMENT '签名算法',
    signature_value TEXT DEFAULT NULL COMMENT '签名值',
    parent_license_no VARCHAR(64) DEFAULT NULL COMMENT '原License编号，用于续期或变更',
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT草稿，ISSUED已签发，DOWNLOADED已下载，REVOKED已吊销，EXPIRED已过期，RENEWED已续期',
    revoke_reason VARCHAR(512) DEFAULT NULL COMMENT '吊销原因',
    downloaded_time DATETIME DEFAULT NULL COMMENT '最近下载时间',
    downloaded_by VARCHAR(64) DEFAULT NULL COMMENT '最近下载人',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    created_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0否，1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_license_no (license_no),
    KEY idx_customer_code (customer_code),
    KEY idx_product_code_version (product_code, product_version),
    KEY idx_machine_code (machine_code),
    KEY idx_status (status),
    KEY idx_expire_time (expire_time),
    KEY idx_parent_license_no (parent_license_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='License授权表';
```

`authorized_modules` 示例：

```json
[
  "system",
  "report",
  "workflow"
]
```

`authorized_limits` 示例：

```json
{
  "maxUsers": 200,
  "maxTenants": 5,
  "maxNodes": 3
}
```

设计说明如下：

1. `license_no` 是 License 的唯一业务编号。
2. `customer_code`、`customer_name`、`product_code`、`product_name` 等字段冗余保存，便于历史追溯。
3. `machine_code` 保存机器码摘要，不建议保存完整硬件明文。
4. `authorized_modules` 和 `authorized_limits` 使用 JSON 存储，便于授权模型扩展。
5. `file_sha256` 用于校验 License 文件完整性。
6. `signature_value` 可选保存，便于排查签名问题；如安全要求较高，也可以只保存在 License 文件中。
7. `parent_license_no` 用于关联续期、变更前的旧 License。
8. `status` 控制 License 当前业务状态。
9. `expire_time` 应建立索引，便于查询即将过期和已过期授权。

### License 操作日志表

License 操作日志表用于记录 License 相关关键操作，包括创建申请、生成文件、下载文件、续期、吊销、修改授权范围等行为。审计日志不应物理删除。

表名：`license_operation_log`

字段设计如下：

| 字段名           | 类型          | 是否必填 | 说明                      |
| ---------------- | ------------- | -------- | ------------------------- |
| id               | bigint        | 是       | 主键 ID                   |
| license_no       | varchar(64)   | 否       | License 编号              |
| operation_type   | varchar(64)   | 是       | 操作类型                  |
| operation_name   | varchar(128)  | 是       | 操作名称                  |
| operator_id      | varchar(64)   | 否       | 操作人 ID                 |
| operator_name    | varchar(64)   | 否       | 操作人名称                |
| request_ip       | varchar(64)   | 否       | 请求 IP                   |
| request_uri      | varchar(255)  | 否       | 请求地址                  |
| request_method   | varchar(16)   | 否       | 请求方法                  |
| before_data      | json          | 否       | 操作前数据                |
| after_data       | json          | 否       | 操作后数据                |
| operation_result | varchar(32)   | 是       | 操作结果：SUCCESS、FAILED |
| error_message    | varchar(1024) | 否       | 错误信息                  |
| operation_time   | datetime      | 是       | 操作时间                  |
| trace_id         | varchar(128)  | 否       | 链路追踪 ID               |
| remark           | varchar(512)  | 否       | 备注                      |

建表语句如下：

```sql
CREATE TABLE license_operation_log (
    id BIGINT NOT NULL COMMENT '主键ID',
    license_no VARCHAR(64) DEFAULT NULL COMMENT 'License编号',
    operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
    operation_name VARCHAR(128) NOT NULL COMMENT '操作名称',
    operator_id VARCHAR(64) DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(64) DEFAULT NULL COMMENT '操作人名称',
    request_ip VARCHAR(64) DEFAULT NULL COMMENT '请求IP',
    request_uri VARCHAR(255) DEFAULT NULL COMMENT '请求地址',
    request_method VARCHAR(16) DEFAULT NULL COMMENT '请求方法',
    before_data JSON DEFAULT NULL COMMENT '操作前数据',
    after_data JSON DEFAULT NULL COMMENT '操作后数据',
    operation_result VARCHAR(32) NOT NULL COMMENT '操作结果：SUCCESS成功，FAILED失败',
    error_message VARCHAR(1024) DEFAULT NULL COMMENT '错误信息',
    operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    trace_id VARCHAR(128) DEFAULT NULL COMMENT '链路追踪ID',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (id),
    KEY idx_license_no (license_no),
    KEY idx_operation_type (operation_type),
    KEY idx_operator_id (operator_id),
    KEY idx_operation_time (operation_time),
    KEY idx_trace_id (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='License操作日志表';
```

操作类型建议如下：

| operation_type    | operation_name | 说明                            |
| ----------------- | -------------- | ------------------------------- |
| CREATE_DRAFT      | 创建授权申请   | 创建 License 草稿               |
| GENERATE_LICENSE  | 生成 License   | 使用私钥签发 License 文件       |
| DOWNLOAD_LICENSE  | 下载 License   | 下载 License 文件               |
| RENEW_LICENSE     | 续期 License   | 生成新的 License 替代旧授权     |
| REVOKE_LICENSE    | 吊销 License   | 主动作废 License                |
| UPDATE_AUTH_SCOPE | 修改授权范围   | 修改授权模块或授权限制          |
| MARK_EXPIRED      | 标记过期       | 定时任务或人工标记 License 过期 |
| VERIFY_LICENSE    | 校验 License   | 在线校验模式下记录校验行为      |

设计说明如下：

1. License 生成、下载、续期、吊销等关键操作必须记录日志。
2. `before_data` 和 `after_data` 用于保存关键字段变化，不建议保存过多无关数据。
3. 操作失败时应记录 `error_message`，便于问题排查。
4. `trace_id` 用于关联接口日志、业务日志和链路追踪日志。
5. 审计日志原则上不做物理删除，后续可按时间归档。
6. 对 `license_no`、`operation_type`、`operator_id`、`operation_time` 建立索引，便于审计查询。



## 后端模块设计

后端模块设计用于说明 License 服务端的核心功能分层、模块职责和调用关系。系统整体按照客户产品管理、授权管理、License 生成、License 校验和审计日志进行拆分，保证业务边界清晰，便于后续扩展在线激活、在线吊销、授权心跳等能力。

推荐后端模块划分如下：

| 模块               | 主要职责                                                    |
| ------------------ | ----------------------------------------------------------- |
| 授权管理模块       | 管理 License 申请、授权范围、授权状态、续期、吊销等业务     |
| License 生成模块   | 负责组装 License Payload、签名、生成 License 文件和文件摘要 |
| License 校验模块   | 负责服务端校验 License 文件、签名、有效期、机器码和产品编码 |
| 客户与产品管理模块 | 管理客户、产品、版本、模块配置和默认授权限制                |
| 审计日志模块       | 记录 License 申请、生成、下载、续期、吊销、校验等操作日志   |

推荐模块包结构如下：

```text
src/main/java/io/github/atengk/license
├── authorization
│   ├── controller
│   ├── service
│   ├── service/impl
│   ├── mapper
│   ├── entity
│   ├── dto
│   └── vo
├── generator
│   ├── service
│   ├── signer
│   ├── storage
│   └── model
├── verifier
│   ├── service
│   ├── checker
│   └── model
├── customer
│   ├── controller
│   ├── service
│   ├── mapper
│   ├── entity
│   ├── dto
│   └── vo
├── product
│   ├── controller
│   ├── service
│   ├── mapper
│   ├── entity
│   ├── dto
│   └── vo
└── audit
    ├── service
    ├── mapper
    ├── entity
    └── event
```

### 授权管理模块

授权管理模块是 License 服务端的核心业务模块，主要负责 License 申请、授权范围维护、状态流转、续期、吊销和查询统计。该模块不直接负责签名算法细节，而是调用 License 生成模块完成文件生成。

模块职责如下：

| 功能         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 创建授权申请 | 根据客户、产品、机器码、有效期和授权范围创建 License 草稿    |
| 查询授权详情 | 查询 License 基础信息、授权范围、文件信息和状态              |
| 修改授权草稿 | 在 License 未签发前允许调整机器码、有效期、模块和限制        |
| 生成 License | 调用 License 生成模块完成签名和文件生成                      |
| 续期 License | 根据原 License 生成新的授权记录，并将旧 License 标记为已续期 |
| 吊销 License | 将 License 状态标记为 `REVOKED`，记录吊销原因                |
| 标记过期     | 定时扫描过期 License，将状态更新为 `EXPIRED`                 |
| 授权统计     | 统计即将过期、已过期、已签发、已吊销的 License 数量          |

授权管理模块核心业务流程如下：

```text
1. 管理员提交 License 申请
2. 授权管理模块校验客户状态、产品状态、机器码和授权范围
3. 创建 license_authorization 草稿记录，状态为 DRAFT
4. 管理员确认生成 License
5. 授权管理模块调用 License 生成模块
6. License 生成模块返回文件名、文件路径、文件摘要和签名信息
7. 授权管理模块更新授权记录，状态变更为 ISSUED
8. 审计日志模块记录生成操作
```

授权管理模块状态控制要求如下：

| 状态       | 允许操作                                       |
| ---------- | ---------------------------------------------- |
| DRAFT      | 修改申请、生成 License、删除草稿               |
| ISSUED     | 下载 License、续期、吊销                       |
| DOWNLOADED | 再次下载、续期、吊销                           |
| REVOKED    | 查询详情、查看日志，不允许下载和续期           |
| EXPIRED    | 查询详情、续期，不允许下载旧文件               |
| RENEWED    | 查询详情、查看新 License，不允许继续下载旧文件 |

设计要求如下：

1. 已签发的 License 不建议直接修改授权内容，授权变更应通过重新生成新 License 完成。
2. License 吊销必须填写吊销原因，并记录审计日志。
3. License 续期应保留旧 License 记录，通过 `parent_license_no` 建立关联。
4. 授权申请生成前必须校验客户、产品、模块和有效期是否合法。
5. 授权查询接口应支持按客户、产品、状态、过期时间、License 编号进行筛选。
6. 定时任务只负责标记服务端状态，客户端离线环境下仍以本地 License 文件为准。

### License 生成模块

License 生成模块负责将授权记录转换为标准 License 文件。该模块应独立封装 Payload 组装、稳定序列化、签名、文件写入和摘要计算逻辑，避免散落在 Controller 或业务 Service 中。

模块职责如下：

| 功能         | 说明                                             |
| ------------ | ------------------------------------------------ |
| 组装 Payload | 根据授权记录生成 License Payload                 |
| 稳定序列化   | 对 Payload 字段排序后生成标准 JSON 字符串        |
| 生成签名     | 使用服务端私钥对 Payload 进行数字签名            |
| 生成文件     | 将 Payload、签名和算法写入 `.lic` 文件           |
| 计算摘要     | 对生成的 License 文件计算 SHA-256 摘要           |
| 文件存储     | 支持本地磁盘、MinIO 或对象存储                   |
| 返回结果     | 返回文件名、文件路径、文件大小、文件摘要和签名值 |

License 生成模块内部组件建议如下：

| 组件                     | 职责                      |
| ------------------------ | ------------------------- |
| `LicensePayloadBuilder`  | 负责组装 License Payload  |
| `LicenseCanonicalizer`   | 负责将 Payload 稳定序列化 |
| `LicenseSigner`          | 负责使用私钥生成签名      |
| `LicenseFileStorage`     | 负责保存 License 文件     |
| `LicenseDigestService`   | 负责计算文件摘要          |
| `LicenseGenerateService` | 编排完整 License 生成流程 |

License 生成流程如下：

```text
1. 接收 License 授权记录
2. 根据授权记录组装 License Payload
3. 将 Payload 按固定规则序列化为 JSON
4. 使用 SHA256withRSA 算法和服务端私钥生成签名
5. 组装最终 License 文件结构
6. 将 License 文件写入指定存储路径
7. 计算 License 文件 SHA-256 摘要
8. 返回文件信息和签名信息
```

License 文件生成结果建议包含以下字段：

| 字段               | 说明                      |
| ------------------ | ------------------------- |
| licenseNo          | License 编号              |
| fileName           | License 文件名            |
| filePath           | License 文件存储路径      |
| fileSize           | License 文件大小          |
| fileSha256         | License 文件 SHA-256 摘要 |
| signatureAlgorithm | 签名算法                  |
| signatureValue     | 签名值                    |
| generatedTime      | 生成时间                  |

设计要求如下：

1. 私钥读取应统一封装，业务代码不能直接操作私钥文件。
2. 签名算法应从配置读取，默认使用 `SHA256withRSA`。
3. Payload 序列化规则必须与客户端验签规则保持一致。
4. License 文件生成后不应被覆盖，重复生成应创建新的文件或阻止重复生成。
5. 文件名建议使用 `licenseNo`，例如 `LIC-20260506-000001.lic`。
6. 文件摘要必须在文件写入后计算，避免摘要与最终文件内容不一致。
7. 生成失败时应记录失败日志和失败原因，不应生成半成品文件。
8. License 文件存储路径不应直接暴露给前端，应通过下载接口获取文件。

### License 校验模块

License 校验模块用于服务端对 License 文件进行解析和验证，主要用于管理端上传检查、在线校验接口、交付人员验证 License 文件是否正确等场景。客户端也可以复用同一套校验模型，但客户端应只持有公钥，不应包含私钥。

模块职责如下：

| 功能         | 说明                                     |
| ------------ | ---------------------------------------- |
| 文件解析     | 解析 `.lic` 文件中的 Payload、签名和算法 |
| 签名校验     | 使用公钥校验 License 文件是否被篡改      |
| 有效期校验   | 校验当前时间是否在授权有效期范围内       |
| 产品校验     | 校验 License 产品编码是否匹配指定产品    |
| 机器码校验   | 校验 License 机器码是否匹配目标机器      |
| 模块校验     | 校验指定模块是否在授权范围内             |
| 限制校验     | 校验用户数、租户数、节点数等限制是否超限 |
| 校验结果返回 | 返回是否通过、失败原因、License 摘要信息 |

服务端校验流程如下：

```text
1. 接收 License 文件或 License 编号
2. 读取 License 文件内容
3. 解析 Payload、Signature 和 Algorithm
4. 使用公钥校验签名
5. 校验 License 有效期
6. 校验产品编码和版本
7. 按需校验机器码
8. 按需校验模块和授权限制
9. 返回校验结果
10. 记录校验日志
```

校验结果模型建议如下：

| 字段         | 说明         |
| ------------ | ------------ |
| valid        | 是否校验通过 |
| licenseNo    | License 编号 |
| customerCode | 客户编码     |
| productCode  | 产品编码     |
| machineCode  | 机器码       |
| notBefore    | 生效时间     |
| notAfter     | 过期时间     |
| modules      | 授权模块     |
| limits       | 授权限制     |
| errorCode    | 失败错误码   |
| errorMessage | 失败原因     |
| verifiedTime | 校验时间     |

常见错误码如下：

| 错误码                    | 说明                 |
| ------------------------- | -------------------- |
| LICENSE_FILE_NOT_FOUND    | License 文件不存在   |
| LICENSE_FORMAT_INVALID    | License 文件格式错误 |
| LICENSE_SIGNATURE_INVALID | License 签名无效     |
| LICENSE_PRODUCT_MISMATCH  | 产品编码不匹配       |
| LICENSE_MACHINE_MISMATCH  | 机器码不匹配         |
| LICENSE_NOT_EFFECTIVE     | License 尚未生效     |
| LICENSE_EXPIRED           | License 已过期       |
| LICENSE_MODULE_DENIED     | 功能模块未授权       |
| LICENSE_LIMIT_EXCEEDED    | 授权数量超限         |
| LICENSE_REVOKED           | License 已吊销       |

设计要求如下：

1. 校验模块应返回结构化结果，不应只返回 `true` 或 `false`。
2. 签名失败、过期、机器码不匹配等原因应明确区分。
3. 服务端在线校验时，可结合数据库状态判断 License 是否已吊销。
4. 离线客户端校验时，不能依赖服务端状态，只能依据 License 文件内容判断。
5. 校验失败日志中不应输出完整签名、私钥路径或敏感密钥信息。
6. 校验方法应支持单独校验文件，也应支持按 License 编号读取服务端文件后校验。

### 客户与产品管理模块

客户与产品管理模块用于维护 License 签发所需的基础资料。客户信息决定授权主体，产品信息决定授权对象、产品版本、可授权模块和默认授权限制。

客户管理职责如下：

| 功能     | 说明                                     |
| -------- | ---------------------------------------- |
| 新增客户 | 创建客户编码、客户名称、联系人和联系方式 |
| 修改客户 | 更新客户名称、联系人、行业、地区等信息   |
| 启停客户 | 启用或禁用客户                           |
| 查询客户 | 按客户编码、名称、状态分页查询           |
| 客户详情 | 查询客户基础信息和关联 License 统计      |
| 删除客户 | 逻辑删除未使用或不再维护的客户           |

产品管理职责如下：

| 功能     | 说明                                     |
| -------- | ---------------------------------------- |
| 新增产品 | 创建产品编码、产品名称、版本和模块配置   |
| 修改产品 | 更新产品名称、版本、模块配置和默认限制   |
| 启停产品 | 启用或禁用产品                           |
| 查询产品 | 按产品编码、名称、版本、状态分页查询     |
| 产品详情 | 查询产品基础信息、模块配置和默认授权限制 |
| 删除产品 | 逻辑删除未使用的产品配置                 |

产品模块配置建议如下：

```json
[
  {
    "moduleCode": "system",
    "moduleName": "系统管理",
    "description": "基础系统管理能力"
  },
  {
    "moduleCode": "report",
    "moduleName": "报表中心",
    "description": "报表查询、导出和统计能力"
  },
  {
    "moduleCode": "workflow",
    "moduleName": "流程管理",
    "description": "审批流程和流程配置能力"
  }
]
```

默认授权限制建议如下：

```json
{
  "maxUsers": 100,
  "maxTenants": 1,
  "maxNodes": 1,
  "maxProjects": 10
}
```

设计要求如下：

1. 客户编码和产品编码必须由系统统一生成或按规则录入，不能重复。
2. License 生成时应校验客户和产品是否为启用状态。
3. 产品模块配置是 License 授权模块的来源，不能授权产品未配置的模块。
4. 默认授权限制用于创建 License 申请时自动填充，管理员可按实际合同调整。
5. 客户和产品如果已被 License 使用，不建议物理删除。
6. 客户名称和产品名称可以变更，但历史 License 中应保留签发时的冗余快照。

### 审计日志模块

审计日志模块用于记录 License 系统中的关键操作，保证授权过程可追溯。审计日志应覆盖成功和失败场景，尤其是 License 生成、下载、续期、吊销、授权范围变更等敏感操作。

模块职责如下：

| 功能         | 说明                                            |
| ------------ | ----------------------------------------------- |
| 记录操作日志 | 保存操作人、操作类型、请求信息、结果和时间      |
| 记录数据快照 | 保存操作前和操作后的关键数据                    |
| 查询操作日志 | 按 License 编号、操作人、操作类型、时间范围查询 |
| 失败日志记录 | 记录业务异常、签名失败、文件生成失败等错误信息  |
| 审计追踪     | 支持从 License 详情追踪完整操作链路             |

需要记录审计日志的操作如下：

| 操作               | 是否必须记录     |
| ------------------ | ---------------- |
| 创建 License 申请  | 是               |
| 修改 License 草稿  | 是               |
| 生成 License 文件  | 是               |
| 下载 License 文件  | 是               |
| 续期 License       | 是               |
| 吊销 License       | 是               |
| 标记 License 过期  | 是               |
| 修改授权模块或限制 | 是               |
| 客户和产品启停     | 建议             |
| License 在线校验   | 可选，按规模决定 |

审计日志内容建议如下：

| 字段            | 说明           |
| --------------- | -------------- |
| licenseNo       | License 编号   |
| operationType   | 操作类型       |
| operationName   | 操作名称       |
| operatorId      | 操作人 ID      |
| operatorName    | 操作人名称     |
| requestIp       | 请求 IP        |
| requestUri      | 请求地址       |
| beforeData      | 操作前关键数据 |
| afterData       | 操作后关键数据 |
| operationResult | 操作结果       |
| errorMessage    | 失败原因       |
| traceId         | 链路追踪 ID    |
| operationTime   | 操作时间       |

设计要求如下：

1. 审计日志不应依赖业务事务完全成功后才记录，失败场景也应记录。
2. `beforeData` 和 `afterData` 只保存关键字段，不建议保存完整请求体中的敏感信息。
3. License 文件下载日志应记录下载人、下载时间、请求 IP 和 License 编号。
4. 私钥、公钥原文、完整签名值、数据库密码等敏感信息不得写入审计日志。
5. 审计日志原则上不允许物理删除，可按月份或年份归档。
6. 审计查询接口应限制权限，仅管理员或审计角色可访问。

## 接口设计

接口设计用于描述 License 服务端对外提供的 REST API。接口主要面向管理端前端、交付人员工具和客户端在线校验场景。所有管理类接口都应进行身份认证和权限控制，避免未授权人员生成、下载或吊销 License。

接口统一前缀建议如下：

```text
/api/license-server
```

统一响应结构建议如下：

```json
{
  "code": "200",
  "message": "操作成功",
  "data": {}
}
```

统一分页响应结构建议如下：

```json
{
  "code": "200",
  "message": "操作成功",
  "data": {
    "records": [],
    "total": 100,
    "current": 1,
    "size": 10
  }
}
```

常见响应码建议如下：

| 响应码 | 说明               |
| ------ | ------------------ |
| 200    | 操作成功           |
| 400    | 请求参数错误       |
| 401    | 未登录或登录已失效 |
| 403    | 无接口权限         |
| 404    | 资源不存在         |
| 409    | 业务状态冲突       |
| 500    | 系统内部异常       |

### 客户管理接口

客户管理接口用于维护 License 签发所需的客户基础信息。客户被禁用后，不允许继续签发新的 License，但历史授权记录仍可查询。

接口列表如下：

| 接口     | 方法   | 地址                                         | 说明         |
| -------- | ------ | -------------------------------------------- | ------------ |
| 新增客户 | POST   | `/api/license-server/customers`              | 创建客户     |
| 修改客户 | PUT    | `/api/license-server/customers/{id}`         | 修改客户信息 |
| 删除客户 | DELETE | `/api/license-server/customers/{id}`         | 逻辑删除客户 |
| 客户详情 | GET    | `/api/license-server/customers/{id}`         | 查询客户详情 |
| 客户分页 | GET    | `/api/license-server/customers/page`         | 分页查询客户 |
| 启用客户 | PATCH  | `/api/license-server/customers/{id}/enable`  | 启用客户     |
| 禁用客户 | PATCH  | `/api/license-server/customers/{id}/disable` | 禁用客户     |

新增客户请求示例：

```http
POST /api/license-server/customers
Content-Type: application/json

{
  "customerCode": "CUST-001",
  "customerName": "某某科技有限公司",
  "contactName": "张三",
  "contactPhone": "13800000000",
  "contactEmail": "zhangsan@example.com",
  "industry": "制造业",
  "region": "上海",
  "remark": "正式客户"
}
```

新增客户响应示例：

```json
{
  "code": "200",
  "message": "操作成功",
  "data": {
    "id": 1900000000000000001,
    "customerCode": "CUST-001",
    "customerName": "某某科技有限公司",
    "status": "ENABLED"
  }
}
```

客户分页查询参数如下：

| 参数         | 类型    | 是否必填 | 说明                        |
| ------------ | ------- | -------- | --------------------------- |
| current      | integer | 否       | 当前页，默认 1              |
| size         | integer | 否       | 每页条数，默认 10           |
| customerCode | string  | 否       | 客户编码                    |
| customerName | string  | 否       | 客户名称，支持模糊查询      |
| status       | string  | 否       | 客户状态：ENABLED、DISABLED |

客户分页请求示例：

```http
GET /api/license-server/customers/page?current=1&size=10&customerName=科技&status=ENABLED
```

设计要求如下：

1. `customerCode` 必须唯一。
2. 禁用客户后，不允许创建新的 License 申请。
3. 删除客户前应检查是否存在 License 授权记录。
4. 客户详情接口可返回该客户的 License 数量、即将过期数量和已过期数量。

### 产品管理接口

产品管理接口用于维护可授权产品、产品版本、模块配置和默认授权限制。License 申请时只能选择启用状态的产品。

接口列表如下：

| 接口     | 方法   | 地址                                        | 说明         |
| -------- | ------ | ------------------------------------------- | ------------ |
| 新增产品 | POST   | `/api/license-server/products`              | 创建产品     |
| 修改产品 | PUT    | `/api/license-server/products/{id}`         | 修改产品信息 |
| 删除产品 | DELETE | `/api/license-server/products/{id}`         | 逻辑删除产品 |
| 产品详情 | GET    | `/api/license-server/products/{id}`         | 查询产品详情 |
| 产品分页 | GET    | `/api/license-server/products/page`         | 分页查询产品 |
| 启用产品 | PATCH  | `/api/license-server/products/{id}/enable`  | 启用产品     |
| 禁用产品 | PATCH  | `/api/license-server/products/{id}/disable` | 禁用产品     |

新增产品请求示例：

```http
POST /api/license-server/products
Content-Type: application/json

{
  "productCode": "PRODUCT-ERP",
  "productName": "企业管理平台",
  "productVersion": "3.0.0",
  "productType": "WEB",
  "moduleConfig": [
    {
      "moduleCode": "system",
      "moduleName": "系统管理"
    },
    {
      "moduleCode": "report",
      "moduleName": "报表中心"
    },
    {
      "moduleCode": "workflow",
      "moduleName": "流程管理"
    }
  ],
  "defaultLimits": {
    "maxUsers": 100,
    "maxTenants": 1,
    "maxNodes": 1
  },
  "remark": "Spring Boot 3 产品线"
}
```

新增产品响应示例：

```json
{
  "code": "200",
  "message": "操作成功",
  "data": {
    "id": 1900000000000000002,
    "productCode": "PRODUCT-ERP",
    "productName": "企业管理平台",
    "productVersion": "3.0.0",
    "status": "ENABLED"
  }
}
```

产品分页查询参数如下：

| 参数           | 类型    | 是否必填 | 说明                        |
| -------------- | ------- | -------- | --------------------------- |
| current        | integer | 否       | 当前页，默认 1              |
| size           | integer | 否       | 每页条数，默认 10           |
| productCode    | string  | 否       | 产品编码                    |
| productName    | string  | 否       | 产品名称，支持模糊查询      |
| productVersion | string  | 否       | 产品版本                    |
| status         | string  | 否       | 产品状态：ENABLED、DISABLED |

设计要求如下：

1. `productCode` 和 `productVersion` 组合唯一。
2. `moduleConfig` 用于限制 License 申请时可选模块。
3. `defaultLimits` 用于创建 License 申请时填充默认授权限制。
4. 产品禁用后，不允许签发新的 License。
5. 产品模块变更不应影响已签发 License 的历史内容。

### License 申请接口

License 申请接口用于创建和管理授权申请。申请记录在生成 License 文件前处于 `DRAFT` 状态，允许修改；生成后不建议直接修改。

接口列表如下：

| 接口     | 方法   | 地址                                             | 说明                        |
| -------- | ------ | ------------------------------------------------ | --------------------------- |
| 创建申请 | POST   | `/api/license-server/authorizations`             | 创建 License 草稿           |
| 修改申请 | PUT    | `/api/license-server/authorizations/{id}`        | 修改 License 草稿           |
| 删除草稿 | DELETE | `/api/license-server/authorizations/{id}`        | 删除未签发草稿              |
| 申请详情 | GET    | `/api/license-server/authorizations/{id}`        | 查询授权详情                |
| 申请分页 | GET    | `/api/license-server/authorizations/page`        | 分页查询授权                |
| 续期申请 | POST   | `/api/license-server/authorizations/{id}/renew`  | 基于旧 License 创建续期申请 |
| 吊销授权 | PATCH  | `/api/license-server/authorizations/{id}/revoke` | 吊销 License                |

创建 License 申请请求示例：

```http
POST /api/license-server/authorizations
Content-Type: application/json

{
  "licenseType": "OFFICIAL",
  "customerId": 1900000000000000001,
  "productId": 1900000000000000002,
  "machineCode": "8f2a7d0b6e9c4a11",
  "effectiveTime": "2026-05-06 00:00:00",
  "expireTime": "2027-05-05 23:59:59",
  "authorizedModules": [
    "system",
    "report",
    "workflow"
  ],
  "authorizedLimits": {
    "maxUsers": 200,
    "maxTenants": 5,
    "maxNodes": 3
  },
  "remark": "正式授权"
}
```

创建 License 申请响应示例：

```json
{
  "code": "200",
  "message": "操作成功",
  "data": {
    "id": 1900000000000000003,
    "licenseNo": "LIC-20260506-000001",
    "licenseType": "OFFICIAL",
    "customerCode": "CUST-001",
    "productCode": "PRODUCT-ERP",
    "status": "DRAFT"
  }
}
```

申请分页查询参数如下：

| 参数            | 类型    | 是否必填 | 说明         |
| --------------- | ------- | -------- | ------------ |
| current         | integer | 否       | 当前页       |
| size            | integer | 否       | 每页条数     |
| licenseNo       | string  | 否       | License 编号 |
| customerCode    | string  | 否       | 客户编码     |
| productCode     | string  | 否       | 产品编码     |
| machineCode     | string  | 否       | 机器码       |
| status          | string  | 否       | 授权状态     |
| expireStartTime | string  | 否       | 过期开始时间 |
| expireEndTime   | string  | 否       | 过期结束时间 |

吊销 License 请求示例：

```http
PATCH /api/license-server/authorizations/1900000000000000003/revoke
Content-Type: application/json

{
  "revokeReason": "客户合同终止，吊销当前授权"
}
```

吊销响应示例：

```json
{
  "code": "200",
  "message": "操作成功",
  "data": {
    "licenseNo": "LIC-20260506-000001",
    "status": "REVOKED"
  }
}
```

设计要求如下：

1. 创建申请时必须校验客户和产品是否启用。
2. 授权模块必须存在于产品模块配置中。
3. `effectiveTime` 必须早于 `expireTime`。
4. `DRAFT` 状态允许修改，`ISSUED` 后不建议修改。
5. 吊销接口只允许管理员或授权管理员调用。
6. 续期接口应生成新的 License 草稿，并关联旧 License 编号。

### License 生成接口

License 生成接口用于将 `DRAFT` 状态的授权申请签发为正式 License 文件。生成成功后，授权状态应变更为 `ISSUED`。

接口列表如下：

| 接口             | 方法 | 地址                                                      | 说明                          |
| ---------------- | ---- | --------------------------------------------------------- | ----------------------------- |
| 生成 License     | POST | `/api/license-server/authorizations/{id}/generate`        | 根据授权申请生成 License 文件 |
| 重新生成 License | POST | `/api/license-server/authorizations/{id}/regenerate`      | 按策略重新生成 License 文件   |
| 查询生成结果     | GET  | `/api/license-server/authorizations/{id}/generate-result` | 查询文件信息和签名信息        |

生成 License 请求示例：

```http
POST /api/license-server/authorizations/1900000000000000003/generate
Content-Type: application/json

{
  "force": false
}
```

生成 License 响应示例：

```json
{
  "code": "200",
  "message": "操作成功",
  "data": {
    "licenseNo": "LIC-20260506-000001",
    "status": "ISSUED",
    "fileName": "LIC-20260506-000001.lic",
    "filePath": "/data/license/files/LIC-20260506-000001.lic",
    "fileSha256": "8F2A7D0B6E9C4A1193C21D7F5B0E6A88D5F2A6C9A3E1D7B0F8A9C2E6D1A0B3C4",
    "signatureAlgorithm": "SHA256withRSA",
    "issuedTime": "2026-05-06 10:00:00"
  }
}
```

生成接口业务校验如下：

| 校验项           | 说明                           |
| ---------------- | ------------------------------ |
| 授权记录是否存在 | 不存在则返回资源不存在         |
| 状态是否为 DRAFT | 非 DRAFT 状态默认不允许生成    |
| 客户是否启用     | 客户禁用时不允许生成           |
| 产品是否启用     | 产品禁用时不允许生成           |
| 机器码是否为空   | 机器码为空时不允许生成         |
| 有效期是否合法   | 生效时间必须早于过期时间       |
| 授权模块是否合法 | 授权模块必须属于产品模块配置   |
| 私钥是否可用     | 私钥不存在或读取失败时生成失败 |
| 文件是否写入成功 | 文件写入失败时生成失败         |

重新生成策略建议如下：

| 策略       | 说明                                                      |
| ---------- | --------------------------------------------------------- |
| 默认禁止   | 已签发 License 不允许重复生成，避免文件和数据库状态不一致 |
| force=true | 仅管理员可强制重新生成，并覆盖旧文件或生成新文件版本      |
| 生成新版本 | 推荐方式，保留旧文件，创建新的文件版本和日志记录          |

设计要求如下：

1. 生成接口必须记录审计日志。
2. 生成失败时应返回明确错误原因。
3. 生成成功后必须保存文件名、文件路径、文件摘要、签名算法和签发时间。
4. 不应将私钥信息返回给前端。
5. 前端展示文件路径时应谨慎，实际下载必须通过下载接口完成。
6. 已吊销、已过期、已续期的 License 不允许重新生成。

### License 下载接口

License 下载接口用于下载已经生成的 License 文件。该接口需要进行权限控制，并记录下载日志。

接口列表如下：

| 接口         | 方法 | 地址                                                | 说明                      |
| ------------ | ---- | --------------------------------------------------- | ------------------------- |
| 下载 License | GET  | `/api/license-server/authorizations/{id}/download`  | 下载 License 文件         |
| 按编号下载   | GET  | `/api/license-server/licenses/{licenseNo}/download` | 根据 License 编号下载文件 |
| 查询文件信息 | GET  | `/api/license-server/authorizations/{id}/file`      | 查询 License 文件元数据   |

下载请求示例：

```http
GET /api/license-server/authorizations/1900000000000000003/download
```

下载响应头建议如下：

```http
HTTP/1.1 200 OK
Content-Type: application/octet-stream
Content-Disposition: attachment; filename="LIC-20260506-000001.lic"
X-License-No: LIC-20260506-000001
X-File-Sha256: 8F2A7D0B6E9C4A1193C21D7F5B0E6A88D5F2A6C9A3E1D7B0F8A9C2E6D1A0B3C4
```

文件信息响应示例：

```json
{
  "code": "200",
  "message": "操作成功",
  "data": {
    "licenseNo": "LIC-20260506-000001",
    "fileName": "LIC-20260506-000001.lic",
    "fileSize": 2048,
    "fileSha256": "8F2A7D0B6E9C4A1193C21D7F5B0E6A88D5F2A6C9A3E1D7B0F8A9C2E6D1A0B3C4",
    "issuedTime": "2026-05-06 10:00:00",
    "downloadedTime": "2026-05-06 10:30:00",
    "downloadedBy": "admin"
  }
}
```

下载接口业务校验如下：

| 校验项           | 说明                                      |
| ---------------- | ----------------------------------------- |
| 授权记录是否存在 | 不存在则返回 404                          |
| 文件是否已生成   | 状态为 DRAFT 时不允许下载                 |
| 文件是否存在     | 文件丢失时返回文件不存在                  |
| 文件摘要是否匹配 | 下载前可重新计算摘要并比对                |
| 状态是否允许下载 | REVOKED、RENEWED 状态默认不允许下载旧文件 |
| 用户是否有权限   | 只有管理员或交付角色可下载                |

设计要求如下：

1. 每次下载都应记录审计日志。
2. 下载成功后可将状态从 `ISSUED` 更新为 `DOWNLOADED`。
3. 重复下载时应更新最近下载时间和下载人。
4. 文件下载接口不应直接暴露服务器真实目录结构。
5. 下载前建议校验文件 SHA-256 摘要，避免交付损坏文件。
6. 对下载接口建议增加限流和权限控制。

### License 校验接口

License 校验接口用于服务端校验 License 文件是否合法，适用于管理端上传验证、交付前检查、在线校验和问题排查。纯离线客户端可以不调用该接口。

接口列表如下：

| 接口           | 方法 | 地址                                              | 说明                         |
| -------------- | ---- | ------------------------------------------------- | ---------------------------- |
| 上传文件校验   | POST | `/api/license-server/licenses/verify-file`        | 上传 License 文件并校验      |
| 按编号校验     | POST | `/api/license-server/licenses/{licenseNo}/verify` | 校验服务端已生成的 License   |
| 在线客户端校验 | POST | `/api/license-server/licenses/online-verify`      | 客户端携带机器码进行在线校验 |
| 模块授权校验   | POST | `/api/license-server/licenses/module-check`       | 校验指定模块是否授权         |

上传文件校验请求示例：

```http
POST /api/license-server/licenses/verify-file
Content-Type: multipart/form-data

file=@LIC-20260506-000001.lic
productCode=PRODUCT-ERP
machineCode=8f2a7d0b6e9c4a11
```

上传文件校验响应示例：

```json
{
  "code": "200",
  "message": "操作成功",
  "data": {
    "valid": true,
    "licenseNo": "LIC-20260506-000001",
    "customerCode": "CUST-001",
    "customerName": "某某科技有限公司",
    "productCode": "PRODUCT-ERP",
    "productVersion": "3.0.0",
    "machineCode": "8f2a7d0b6e9c4a11",
    "notBefore": "2026-05-06 00:00:00",
    "notAfter": "2027-05-05 23:59:59",
    "modules": [
      "system",
      "report",
      "workflow"
    ],
    "limits": {
      "maxUsers": 200,
      "maxTenants": 5,
      "maxNodes": 3
    },
    "verifiedTime": "2026-05-06 11:00:00"
  }
}
```

校验失败响应示例：

```json
{
  "code": "400",
  "message": "License 校验失败",
  "data": {
    "valid": false,
    "errorCode": "LICENSE_MACHINE_MISMATCH",
    "errorMessage": "License 绑定机器码与当前机器码不匹配",
    "licenseNo": "LIC-20260506-000001",
    "verifiedTime": "2026-05-06 11:00:00"
  }
}
```

在线客户端校验请求示例：

```http
POST /api/license-server/licenses/online-verify
Content-Type: application/json

{
  "licenseNo": "LIC-20260506-000001",
  "productCode": "PRODUCT-ERP",
  "machineCode": "8f2a7d0b6e9c4a11",
  "clientVersion": "3.0.0",
  "instanceId": "node-01"
}
```

在线客户端校验响应示例：

```json
{
  "code": "200",
  "message": "操作成功",
  "data": {
    "valid": true,
    "licenseNo": "LIC-20260506-000001",
    "status": "ISSUED",
    "notAfter": "2027-05-05 23:59:59",
    "remainDays": 364
  }
}
```

模块授权校验请求示例：

```http
POST /api/license-server/licenses/module-check
Content-Type: application/json

{
  "licenseNo": "LIC-20260506-000001",
  "moduleCode": "report"
}
```

模块授权校验响应示例：

```json
{
  "code": "200",
  "message": "操作成功",
  "data": {
    "allowed": true,
    "moduleCode": "report",
    "licenseNo": "LIC-20260506-000001"
  }
}
```

校验接口设计要求如下：

1. 上传文件校验不应保存上传文件，除非业务明确要求留档。
2. 在线校验应同时校验 License 文件内容和数据库状态。
3. 在线校验可以识别服务端 `REVOKED` 状态，离线校验无法实时识别吊销。
4. 校验失败应返回明确错误码，便于客户端区分处理。
5. 校验接口应记录审计日志，但高频在线校验场景可单独落入运行日志或心跳表。
6. 客户端调用在线校验接口时，应避免上传完整硬件明文，只上传机器码摘要。
7. 校验接口不应返回签名私钥、公钥原文或内部文件路径。



## License 客户端集成

License 客户端集成用于说明业务系统如何接入 License 校验能力。客户端主要负责读取本地 License 文件、校验签名、校验机器码、校验有效期、缓存授权上下文，并在业务运行过程中提供模块授权和数量限制校验能力。

客户端集成后，业务系统应具备以下能力：

| 能力       | 说明                                                         |
| ---------- | ------------------------------------------------------------ |
| 启动校验   | 应用启动时自动校验 License 文件是否合法                      |
| 运行时校验 | 业务执行过程中校验模块、用户数、租户数、节点数等授权限制     |
| 授权上下文 | 将 License 信息解析为运行时上下文，供业务代码读取            |
| 文件热更新 | 支持替换 License 文件后重新加载授权信息                      |
| 异常处理   | 对文件不存在、过期、机器码不匹配、签名无效等情况给出明确错误 |

### 客户端依赖引入

客户端业务系统通过引入 License Client Starter 完成集成。该依赖内部应封装 License 文件读取、签名校验、机器码生成、授权上下文缓存、启动校验和运行时校验能力。

推荐将客户端组件拆分为独立 Maven 模块，例如：

```text
license-client-starter
```

业务系统引入依赖如下：

```xml
<dependencies>
    <!-- License 客户端组件：提供 License 文件读取、签名校验、机器码校验和运行时授权校验能力 -->
    <dependency>
        <groupId>io.github.atengk</groupId>
        <artifactId>license-client-starter</artifactId>
        <version>1.0.0</version>
    </dependency>

    <!-- Hutool 工具包：用于 JSON、日期、摘要、文件读取、Base64 等工具能力 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.32</version>
    </dependency>
</dependencies>
```

客户端基础配置如下：

```yaml
license:
  client:
    enabled: true # 是否启用 License 校验
    product-code: PRODUCT-ERP # 当前业务系统产品编码，需要与 License 文件中的 productCode 一致
    product-version: 3.0.0 # 当前业务系统产品版本，可按需校验
    license-path: /data/app/license/PRODUCT-ERP.lic # License 文件路径
    public-key-path: classpath:license/public_key.pem # 公钥路径，只用于验签
    fail-fast: true # 启动校验失败时是否直接终止应用
    allow-clock-skew-seconds: 300 # 允许服务器时间误差，单位秒
    runtime-check:
      enabled: true # 是否开启运行时校验
      interval-seconds: 300 # 定时校验间隔，避免应用长时间运行后 License 过期仍继续使用
```

推荐客户端目录如下：

```text
src/main/resources
└── license
    ├── public_key.pem
    └── PRODUCT-ERP.lic
```

配置项说明如下：

| 配置项                                          | 说明                                            |
| ----------------------------------------------- | ----------------------------------------------- |
| `license.client.enabled`                        | 是否启用 License 校验，生产环境必须启用         |
| `license.client.product-code`                   | 当前业务系统产品编码                            |
| `license.client.product-version`                | 当前业务系统产品版本                            |
| `license.client.license-path`                   | License 文件路径，支持绝对路径或 classpath 路径 |
| `license.client.public-key-path`                | 公钥路径，只能用于验签                          |
| `license.client.fail-fast`                      | 校验失败是否阻止应用启动                        |
| `license.client.allow-clock-skew-seconds`       | 允许客户端服务器时间偏差                        |
| `license.client.runtime-check.enabled`          | 是否启用运行时定时校验                          |
| `license.client.runtime-check.interval-seconds` | 运行时校验间隔                                  |

接入要求如下：

1. 业务系统必须配置 `product-code`，且与 License 文件中的产品编码一致。
2. 公钥可以随客户端一起发布，但私钥绝不能出现在客户端工程中。
3. License 文件建议放在外部目录，便于运维替换和续期。
4. 生产环境不建议将 `license.client.enabled` 设置为 `false`。
5. 如果业务系统支持容器部署，应将 License 文件通过挂载卷方式注入容器。
6. 如果启用启动失败即退出，`fail-fast` 应设置为 `true`。

容器部署时，推荐通过挂载目录提供 License 文件：

```bash
docker run -d \
  --name product-erp \
  -p 8080:8080 \
  -v /data/license/PRODUCT-ERP.lic:/data/app/license/PRODUCT-ERP.lic \
  -e SPRING_PROFILES_ACTIVE=prod \
  product-erp:3.0.0
```

该命令将宿主机 `/data/license/PRODUCT-ERP.lic` 挂载到容器内 `/data/app/license/PRODUCT-ERP.lic`，业务系统通过 `license.client.license-path` 读取该文件。

### 启动时 License 校验

启动时 License 校验用于保证业务系统在运行前已经具备合法授权。客户端组件应在 Spring Boot 应用启动阶段完成完整校验，校验失败时根据配置决定阻止启动或进入受限模式。

启动校验建议在以下时机执行：

| 执行方式                   | 说明                                    | 建议               |
| -------------------------- | --------------------------------------- | ------------------ |
| `ApplicationRunner`        | Spring Boot 启动完成后执行 License 校验 | 推荐               |
| `SmartLifecycle`           | 更细粒度控制启动生命周期                | 可选               |
| `EnvironmentPostProcessor` | 在环境准备阶段处理配置                  | 不建议承载复杂校验 |
| 自定义 Bean 初始化         | 在关键业务 Bean 初始化前校验            | 可选               |

启动校验流程如下：

```text
1. 读取 license.client 配置
2. 判断是否启用 License 校验
3. 读取 License 文件
4. 解析 License 文件结构
5. 读取公钥文件
6. 使用公钥校验 License 签名
7. 校验产品编码和产品版本
8. 生成当前机器码
9. 校验机器码是否匹配
10. 校验当前时间是否在有效期范围内
11. 解析授权模块和授权限制
12. 构建 License 运行时上下文
13. 校验通过后允许应用继续运行
```

启动校验项如下：

| 校验项       | 说明                                         | 失败处理           |
| ------------ | -------------------------------------------- | ------------------ |
| 配置校验     | 检查产品编码、License 路径、公钥路径是否配置 | 启动失败           |
| 文件存在性   | 检查 License 文件是否存在                    | 启动失败           |
| 文件格式     | 检查 License JSON 结构是否合法               | 启动失败           |
| 签名校验     | 使用公钥校验 License 是否被篡改              | 启动失败           |
| 产品校验     | License 产品编码必须与当前系统一致           | 启动失败           |
| 机器码校验   | 当前机器码必须与 License 绑定机器码一致      | 启动失败           |
| 有效期校验   | 当前时间必须在生效时间和过期时间之间         | 启动失败或受限模式 |
| 授权范围解析 | 解析模块和数量限制                           | 启动失败           |

启动校验成功后，应在内存中保存 License 上下文：

| 上下文字段       | 说明              |
| ---------------- | ----------------- |
| `licenseNo`      | 当前 License 编号 |
| `customerCode`   | 当前客户编码      |
| `customerName`   | 当前客户名称      |
| `productCode`    | 当前产品编码      |
| `productVersion` | 当前产品版本      |
| `machineCode`    | 当前机器码        |
| `modules`        | 当前已授权模块    |
| `limits`         | 当前授权限制      |
| `notBefore`      | 生效时间          |
| `notAfter`       | 过期时间          |
| `verifiedAt`     | 最近一次校验时间  |

启动日志建议如下：

```text
License 校验开始，产品编码：PRODUCT-ERP，License路径：/data/app/license/PRODUCT-ERP.lic
License 签名校验通过，License编号：LIC-20260506-000001
License 机器码校验通过，机器码：8f2a7d0b6e9c4a11
License 有效期校验通过，到期时间：2027-05-05 23:59:59
License 校验完成，当前系统允许启动
```

启动校验失败日志示例：

```text
License 校验失败，原因：License 已过期，License编号：LIC-20260506-000001，到期时间：2026-05-05 23:59:59
```

设计要求如下：

1. 启动阶段必须执行签名校验，不能只解析 License 内容。
2. 校验失败时应输出明确失败原因，便于交付和运维定位问题。
3. 日志中可以输出 License 编号、产品编码、过期时间，但不能输出私钥、完整签名等敏感信息。
4. 如果 `fail-fast=true`，校验失败应直接终止应用启动。
5. 如果业务要求高可用，可以支持受限模式，但受限模式必须明确禁用核心写操作。
6. 启动校验通过后，License 上下文应只读缓存，避免业务代码随意修改。

### 运行时 License 校验

运行时 License 校验用于在系统运行过程中控制具体功能模块和授权数量。启动校验只能证明系统具备基础授权，运行时校验用于判断某个模块、操作或资源数量是否在授权范围内。

运行时校验适用场景如下：

| 场景           | 示例                                            |
| -------------- | ----------------------------------------------- |
| 模块授权校验   | 访问报表中心前校验 `report` 模块是否授权        |
| 用户数量校验   | 创建用户前校验当前用户数量是否超过 `maxUsers`   |
| 租户数量校验   | 创建租户前校验当前租户数量是否超过 `maxTenants` |
| 节点数量校验   | 集群节点启动时校验当前节点数是否超过 `maxNodes` |
| 定时有效期校验 | 后台定时检查 License 是否过期                   |
| 文件变更校验   | License 文件被替换后重新加载并校验              |

运行时校验方式建议如下：

| 方式       | 说明                                        | 适用场景                 |
| ---------- | ------------------------------------------- | ------------------------ |
| 显式调用   | 业务代码主动调用 License 校验服务           | 数量限制、复杂业务规则   |
| 注解拦截   | 在 Controller 或 Service 方法上增加授权注解 | 模块权限控制             |
| Web 拦截器 | 对特定 URL 进行模块授权控制                 | 前后端模块边界清晰的系统 |
| AOP 切面   | 统一拦截带注解的方法                        | 推荐用于模块授权         |
| 定时任务   | 周期性检查 License 是否过期                 | 长时间运行服务           |

运行时模块校验注解示例：

```java
package io.github.atengk.license.client.annotation;

import java.lang.annotation.*;

/**
 * License 模块授权校验注解
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireLicenseModule {

    /**
     * 授权模块编码
     *
     * @return 模块编码
     */
    String value();

}
```

业务接口使用示例：

```java
package io.github.atengk.report.controller;

import io.github.atengk.license.client.annotation.RequireLicenseModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报表接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
public class ReportController {

    /**
     * 导出报表
     *
     * @return 导出结果
     */
    @RequireLicenseModule("report")
    @GetMapping("/api/reports/export")
    public String exportReport() {
        log.info("执行报表导出，已通过 License 模块授权校验");
        return "报表导出成功";
    }

}
```

数量限制校验示例：

```java
package io.github.atengk.user.service;

import io.github.atengk.license.client.checker.LicenseRuntimeChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户业务服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final LicenseRuntimeChecker licenseRuntimeChecker;

    /**
     * 创建用户
     *
     * @param currentUserCount 当前用户数量
     */
    public void createUser(long currentUserCount) {
        licenseRuntimeChecker.checkLimit("maxUsers", currentUserCount + 1);
        log.info("用户数量 License 校验通过，当前创建后用户数：{}", currentUserCount + 1);

        // 执行业务系统创建用户逻辑
    }

}
```

运行时校验设计要求如下：

1. 模块校验失败时，应返回明确提示，例如“当前 License 未授权报表模块”。
2. 数量限制校验失败时，应提示当前数量和授权上限。
3. 运行时校验必须基于启动时解析的 License 上下文，不能每次业务调用都重复读取文件。
4. 对于 License 文件更新，应通过显式刷新或文件监听重新加载上下文。
5. 对于高频接口，运行时校验应避免复杂 IO 和重复签名验签。
6. 关键业务操作应在 Service 层进行数量限制校验，避免绕过 Controller。
7. 定时校验发现 License 过期后，应更新运行时状态，阻止后续受控功能继续执行。

### License 文件更新

License 文件更新用于支持授权续期、模块增购、节点扩容或客户信息变更后的文件替换。客户端应支持安全替换 License 文件，并在替换后重新加载授权上下文。

License 文件更新方式如下：

| 更新方式   | 说明                                    | 适用场景             |
| ---------- | --------------------------------------- | -------------------- |
| 手动替换   | 运维人员将新 License 文件复制到指定目录 | 离线私有化部署       |
| 管理端上传 | 系统提供页面上传新 License 文件         | 有管理后台的业务系统 |
| 接口上传   | 通过内部接口上传 License 文件           | 自动化交付           |
| 在线拉取   | 客户端从授权中心拉取新 License          | 在线授权场景，可扩展 |

手动替换流程如下：

```text
1. 服务端生成新的 License 文件
2. 交付人员将新文件发送给客户
3. 运维人员备份旧 License 文件
4. 将新 License 文件复制到 license.client.license-path 指定路径
5. 重启业务系统或调用 License 刷新接口
6. 客户端重新读取并校验新 License 文件
7. 校验通过后刷新 License 上下文
8. 校验失败则回滚旧 License 文件
```

推荐更新命令如下：

```bash
# 进入 License 文件目录
cd /data/app/license

# 备份旧 License 文件
cp PRODUCT-ERP.lic PRODUCT-ERP.lic.bak.$(date +%Y%m%d%H%M%S)

# 上传或复制新的 License 文件
cp /tmp/PRODUCT-ERP-new.lic PRODUCT-ERP.lic

# 查看文件权限
ls -l PRODUCT-ERP.lic

# 重启业务系统，或调用系统提供的 License 刷新接口
systemctl restart product-erp
```

以上命令先备份旧文件，再替换新文件。替换完成后建议重启应用，或者调用客户端提供的刷新接口重新加载 License 上下文。

如果业务系统提供 License 刷新接口，建议接口如下：

```http
POST /api/system/license/reload
Content-Type: application/json

{}
```

刷新成功响应示例：

```json
{
  "code": "200",
  "message": "License 重新加载成功",
  "data": {
    "licenseNo": "LIC-20260506-000002",
    "notAfter": "2028-05-05 23:59:59",
    "modules": [
      "system",
      "report",
      "workflow"
    ]
  }
}
```

文件更新设计要求如下：

1. 更新前必须备份旧 License 文件。
2. 新文件必须先完整写入临时文件，再原子替换正式文件，避免读取到半写入文件。
3. 替换后必须重新执行完整校验，包括签名、产品编码、机器码和有效期。
4. 校验失败时应保留旧 License 上下文，不应直接清空授权状态。
5. 文件权限应限制为应用运行用户可读，避免被无关用户修改。
6. 支持容器部署时，License 文件应通过挂载卷更新，不建议写入镜像内部。
7. License 文件更新操作应记录业务日志和审计日志。

## 安全设计

安全设计用于保证 License 签发、存储、传输、校验和接口访问过程的安全性。系统安全重点包括私钥保护、公钥分发、License 文件防篡改、管理端接口权限控制和敏感日志脱敏。

### 私钥与公钥管理

私钥与公钥管理是 License 安全设计的核心。服务端使用私钥签发 License，客户端使用公钥校验 License。私钥一旦泄露，攻击者就可能伪造合法 License，因此私钥必须严格保护。

密钥职责如下：

| 密钥     | 使用位置                       | 作用                 | 是否可分发 |
| -------- | ------------------------------ | -------------------- | ---------- |
| 私钥     | License 服务端                 | 生成 License 签名    | 不允许     |
| 公钥     | License 客户端、服务端校验模块 | 校验 License 签名    | 允许       |
| AES 密钥 | 可选，服务端和客户端           | 加密 License Payload | 谨慎使用   |

密钥生成建议如下：

```bash
# 生成 RSA 私钥
openssl genrsa -out private_key.pem 2048

# 从私钥中导出公钥
openssl rsa -in private_key.pem -pubout -out public_key.pem

# 查看私钥文件权限
chmod 600 private_key.pem

# 查看公钥文件权限
chmod 644 public_key.pem
```

该命令使用 OpenSSL 生成 RSA 私钥和公钥。`private_key.pem` 只能部署在 License 服务端，`public_key.pem` 可以分发给客户端用于验签。

服务端私钥管理要求如下：

1. 私钥只能存放在 License 服务端，不允许提交到 Git 仓库。
2. 私钥文件权限应限制为服务运行用户可读。
3. 生产环境建议通过密钥管理系统、KMS、Vault 或加密挂载方式提供私钥。
4. 私钥路径应通过环境变量或部署配置注入，不应硬编码在代码中。
5. 私钥读取失败时应阻止 License 生成。
6. 私钥轮换时应保留旧公钥校验历史 License 的能力。
7. 私钥备份应加密保存，并限制访问人员范围。

客户端公钥管理要求如下：

1. 公钥可以随客户端组件一起发布。
2. 公钥用于验签，不具备生成 License 的能力。
3. 公钥文件不应允许业务系统运行过程中被随意修改。
4. 如果需要轮换密钥，客户端应支持多个公钥版本。
5. License 文件中可增加 `keyId` 字段，用于标识当前 License 使用的密钥版本。

密钥版本示例：

```json
{
  "payload": {
    "licenseNo": "LIC-20260506-000001",
    "productCode": "PRODUCT-ERP"
  },
  "signature": "Base64UrlEncodedSignature",
  "algorithm": "SHA256withRSA",
  "keyId": "rsa-2026-01"
}
```

密钥轮换策略如下：

| 场景              | 处理方式                                                 |
| ----------------- | -------------------------------------------------------- |
| 正常轮换          | 新 License 使用新私钥签发，客户端内置新旧公钥            |
| 私钥疑似泄露      | 停止使用旧私钥，生成新密钥对，重新签发高风险客户 License |
| 客户端升级        | 新版本客户端内置新公钥，同时保留旧公钥兼容历史 License   |
| 历史 License 校验 | 根据 `keyId` 选择对应公钥完成验签                        |

### License 防篡改设计

License 防篡改设计用于保证授权内容被修改后无法通过校验。防篡改的核心不是隐藏 License 内容，而是保证任何字段被修改后签名校验都会失败。

推荐防篡改方案如下：

```text
1. 服务端组装 License Payload
2. 对 Payload 进行稳定序列化
3. 使用 SHA256withRSA 对序列化内容生成签名
4. License 文件同时保存 Payload、Signature、Algorithm、KeyId
5. 客户端读取 Payload 后使用相同规则序列化
6. 客户端使用公钥校验 Signature
7. Payload 任意字段被修改，验签失败
```

防篡改校验覆盖字段如下：

| 字段         | 是否纳入签名     | 说明                         |
| ------------ | ---------------- | ---------------------------- |
| License 编号 | 是               | 防止替换授权编号             |
| 客户信息     | 是               | 防止伪造授权客户             |
| 产品信息     | 是               | 防止跨产品使用               |
| 机器码       | 是               | 防止复制到其他机器           |
| 有效期       | 是               | 防止修改过期时间             |
| 授权模块     | 是               | 防止增加未购买模块           |
| 授权限制     | 是               | 防止修改用户数、节点数等限制 |
| 扩展字段     | 是               | 防止扩展业务字段被篡改       |
| Signature    | 否               | 签名本身不参与签名           |
| Algorithm    | 建议参与外层校验 | 防止算法降级攻击             |
| KeyId        | 建议参与外层校验 | 防止错误公钥选择             |

License 文件防篡改要求如下：

1. 客户端必须先验签，再读取授权内容用于业务判断。
2. Payload 的序列化规则必须固定，不能依赖不稳定的 JSON 字段顺序。
3. 推荐使用字段排序后的 JSON 字符串作为签名原文。
4. License 文件可以 Base64 编码，但 Base64 不是安全加密手段。
5. 如果授权内容涉及敏感商业信息，可对 Payload 加密，但加密不能替代签名。
6. 文件摘要可以用于下载完整性校验，但不能替代数字签名。
7. 客户端不能提供跳过签名校验的参数。
8. 生产环境不能保留测试公钥或测试绕过逻辑。

如果需要隐藏 License 内容，可采用以下结构：

```json
{
  "cipherText": "Base64UrlEncodedEncryptedPayload",
  "signature": "Base64UrlEncodedSignature",
  "algorithm": "SHA256withRSA",
  "encryptAlgorithm": "AES/GCM/NoPadding",
  "keyId": "rsa-2026-01"
}
```

加密模式下建议采用“先加密，再签名”的方式：

```text
1. 服务端组装 Payload
2. 使用 AES-GCM 加密 Payload，得到 cipherText
3. 使用私钥对 cipherText 生成签名
4. 客户端先使用公钥验签
5. 验签通过后再解密 cipherText
6. 解密后执行有效期、机器码、模块和限制校验
```

### 接口权限控制

接口权限控制用于保护 License 服务端管理接口，避免未授权用户创建、生成、下载、吊销 License。License 生成和下载属于高敏感操作，必须纳入权限控制和审计范围。

推荐角色设计如下：

| 角色               | 权限范围                                   |
| ------------------ | ------------------------------------------ |
| `LICENSE_ADMIN`    | License 系统管理员，拥有全部授权管理权限   |
| `LICENSE_OPERATOR` | 授权操作员，可创建申请、生成和下载 License |
| `LICENSE_AUDITOR`  | 审计人员，只能查询授权记录和操作日志       |
| `PRODUCT_ADMIN`    | 产品管理员，可维护产品和模块配置           |
| `CUSTOMER_ADMIN`   | 客户管理员，可维护客户信息                 |
| `READONLY`         | 只读用户，只能查看部分基础信息             |

接口权限建议如下：

| 接口类型             | 需要权限                              |
| -------------------- | ------------------------------------- |
| 客户新增、修改、启停 | `CUSTOMER_ADMIN` 或 `LICENSE_ADMIN`   |
| 产品新增、修改、启停 | `PRODUCT_ADMIN` 或 `LICENSE_ADMIN`    |
| License 申请创建     | `LICENSE_OPERATOR` 或 `LICENSE_ADMIN` |
| License 生成         | `LICENSE_OPERATOR` 或 `LICENSE_ADMIN` |
| License 下载         | `LICENSE_OPERATOR` 或 `LICENSE_ADMIN` |
| License 吊销         | `LICENSE_ADMIN`                       |
| License 续期         | `LICENSE_OPERATOR` 或 `LICENSE_ADMIN` |
| 审计日志查询         | `LICENSE_AUDITOR` 或 `LICENSE_ADMIN`  |
| 在线校验接口         | 客户端专用凭证或内部网络访问          |

接口安全控制要求如下：

1. 所有管理端接口必须登录后访问。
2. License 生成、下载、吊销接口必须进行权限校验。
3. 高敏感接口应记录操作日志，包括操作人、IP、请求路径、操作结果。
4. 下载接口建议增加频率限制，避免 License 文件被批量下载。
5. 在线校验接口应使用客户端凭证、签名请求或内网访问控制。
6. 前端隐藏按钮不能替代后端权限校验。
7. 管理端应开启 HTTPS，避免 License 文件和操作数据在传输过程中被窃取。
8. 响应数据不应返回服务器真实文件路径、私钥路径、完整签名值等敏感信息。

Spring Security 权限控制示例：

```java
package io.github.atengk.license.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * License 接口安全配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
@EnableMethodSecurity
public class LicenseSecurityConfig {

    /**
     * 配置 License 服务端接口权限
     *
     * @param http HttpSecurity
     * @return 安全过滤链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain licenseSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("初始化 License 接口安全配置");

        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/license-server/licenses/online-verify").permitAll()
                .requestMatchers("/api/license-server/authorizations/*/generate").hasAnyRole("LICENSE_ADMIN", "LICENSE_OPERATOR")
                .requestMatchers("/api/license-server/authorizations/*/download").hasAnyRole("LICENSE_ADMIN", "LICENSE_OPERATOR")
                .requestMatchers("/api/license-server/authorizations/*/revoke").hasRole("LICENSE_ADMIN")
                .requestMatchers("/api/license-server/audit/**").hasAnyRole("LICENSE_ADMIN", "LICENSE_AUDITOR")
                .anyRequest().authenticated()
            );

        return http.build();
    }

}
```

该配置仅作为接口权限示例。实际项目中应结合登录认证方式、Token 解析、用户角色体系、白名单和统一异常返回进行完整实现。

## 异常处理

异常处理用于定义 License 客户端和服务端在校验失败时的错误类型、错误码、日志输出和业务响应方式。异常信息应明确、可定位，但不能泄露密钥、签名原文或其他敏感数据。

推荐异常结构如下：

| 字段           | 说明                   |
| -------------- | ---------------------- |
| `errorCode`    | License 错误码         |
| `errorMessage` | 错误描述               |
| `licenseNo`    | License 编号，可为空   |
| `productCode`  | 产品编码               |
| `machineCode`  | 机器码摘要，可按需脱敏 |
| `occurredTime` | 异常发生时间           |
| `suggestion`   | 建议处理方式           |

统一错误响应示例：

```json
{
  "code": "400",
  "message": "License 校验失败",
  "data": {
    "errorCode": "LICENSE_EXPIRED",
    "errorMessage": "License 已过期",
    "licenseNo": "LIC-20260506-000001",
    "productCode": "PRODUCT-ERP",
    "occurredTime": "2026-05-06 10:00:00",
    "suggestion": "请联系管理员重新生成 License 文件"
  }
}
```

License 异常码建议如下：

| 错误码                        | 说明                   |
| ----------------------------- | ---------------------- |
| `LICENSE_FILE_NOT_FOUND`      | License 文件不存在     |
| `LICENSE_FILE_READ_FAILED`    | License 文件读取失败   |
| `LICENSE_FORMAT_INVALID`      | License 文件格式不正确 |
| `LICENSE_SIGNATURE_INVALID`   | License 签名无效       |
| `LICENSE_PRODUCT_MISMATCH`    | 产品编码不匹配         |
| `LICENSE_MACHINE_MISMATCH`    | 机器码不匹配           |
| `LICENSE_NOT_EFFECTIVE`       | License 尚未生效       |
| `LICENSE_EXPIRED`             | License 已过期         |
| `LICENSE_MODULE_DENIED`       | 功能模块未授权         |
| `LICENSE_LIMIT_EXCEEDED`      | 授权数量超限           |
| `LICENSE_REVOKED`             | License 已吊销         |
| `LICENSE_PUBLIC_KEY_INVALID`  | 公钥无效               |
| `LICENSE_PRIVATE_KEY_INVALID` | 私钥无效               |

### License 文件不存在

License 文件不存在表示客户端配置的 License 文件路径下未找到授权文件。该异常通常发生在首次部署、License 文件未挂载、文件路径配置错误或容器挂载失败时。

异常判定条件如下：

| 条件                                 | 说明                                 |
| ------------------------------------ | ------------------------------------ |
| `license.client.license-path` 未配置 | 客户端未指定 License 文件路径        |
| 文件路径不存在                       | 指定目录或文件不存在                 |
| 文件不可读                           | 文件存在，但应用运行用户没有读取权限 |
| 容器挂载失败                         | 容器内路径没有正确挂载 License 文件  |

错误示例：

```text
错误码：LICENSE_FILE_NOT_FOUND
错误信息：License 文件不存在
处理建议：请检查 license.client.license-path 配置，并确认 License 文件已上传到指定目录
```

日志示例：

```text
License 校验失败，原因：License 文件不存在，文件路径：/data/app/license/PRODUCT-ERP.lic
```

处理策略如下：

| 场景         | 处理方式                                      |
| ------------ | --------------------------------------------- |
| 生产环境启动 | 直接阻止应用启动                              |
| 测试环境启动 | 可通过配置关闭 License 校验，但不建议长期关闭 |
| 管理后台上传 | 提示用户上传 License 文件                     |
| 容器部署     | 检查挂载目录和文件权限                        |

排查步骤如下：

```bash
# 查看 License 文件是否存在
ls -l /data/app/license/PRODUCT-ERP.lic

# 查看当前用户是否有读取权限
cat /data/app/license/PRODUCT-ERP.lic

# 查看容器内文件是否存在
docker exec -it product-erp ls -l /data/app/license/PRODUCT-ERP.lic
```

设计要求如下：

1. 文件不存在时不得进入正常授权状态。
2. 错误信息应包含配置路径，但不应暴露服务器敏感目录结构给普通用户。
3. 管理端可以提示“请上传 License 文件”，客户端日志可以输出详细路径。
4. 如果存在备份文件，不应自动使用备份文件，除非系统明确支持回滚策略。

### License 已过期

License 已过期表示当前服务器时间已经超过 License 文件中的 `notAfter`。该异常通常发生在授权期限结束、客户未续期、服务器时间异常或 License 文件替换失败时。

异常判定条件如下：

```text
当前时间 > License.notAfter + allowClockSkewSeconds
```

错误示例：

```text
错误码：LICENSE_EXPIRED
错误信息：License 已过期
处理建议：请联系管理员续期并替换新的 License 文件
```

日志示例：

```text
License 校验失败，原因：License 已过期，License编号：LIC-20260506-000001，到期时间：2026-05-05 23:59:59，当前时间：2026-05-06 10:00:00
```

处理策略如下：

| 策略     | 说明                                         |
| -------- | -------------------------------------------- |
| 阻止启动 | 默认策略，License 过期后应用启动失败         |
| 只读模式 | 允许查询历史数据，禁止新增、修改、导出等操作 |
| 受限模式 | 仅允许访问非核心功能                         |
| 宽限期   | 到期后允许继续使用固定天数                   |
| 在线复核 | 联网环境下向授权中心确认是否已续期           |

推荐默认处理如下：

1. 启动阶段发现已过期，直接阻止启动。
2. 运行阶段发现已过期，更新 License 上下文状态，拒绝后续受控功能。
3. 管理端显示剩余天数，到期前 30 天开始提醒。
4. 续期后通过替换 License 文件并重新加载恢复正常使用。

过期提醒规则如下：

| 时间         | 提醒级别   | 说明               |
| ------------ | ---------- | ------------------ |
| 到期前 30 天 | 普通提醒   | 日志或首页提示     |
| 到期前 15 天 | 中度提醒   | 提示管理员准备续期 |
| 到期前 7 天  | 强提醒     | 提高提醒频率       |
| 到期当天     | 严重提醒   | 提示当天到期       |
| 到期后       | 阻断或受限 | 按系统策略处理     |

设计要求如下：

1. License 到期判断应使用服务器当前时间。
2. 应允许少量时间误差，避免服务器时钟轻微偏差导致误判。
3. 不应通过修改服务器时间规避过期校验。
4. 定时校验发现 License 过期后，应立即更新运行时状态。
5. 日志中应输出 License 编号和过期时间，便于定位授权文件。

### 机器码不匹配

机器码不匹配表示 License 文件绑定的机器码与当前服务器生成的机器码不一致。该异常通常发生在更换服务器、迁移虚拟机、容器调度到新节点、网卡变化或使用了错误的 License 文件时。

异常判定条件如下：

```text
当前机器码 != License 文件中的 machineCode
```

如果 License 支持多节点绑定，则判定条件如下：

```text
当前机器码不在 License.machineCodes 列表中
```

错误示例：

```text
错误码：LICENSE_MACHINE_MISMATCH
错误信息：License 绑定机器码与当前机器码不匹配
处理建议：请使用当前服务器机器码重新申请 License
```

日志示例：

```text
License 校验失败，原因：机器码不匹配，License机器码：8f2a7d0b6e9c4a11，当前机器码：19b7c2f0a6d4e931
```

处理策略如下：

| 场景             | 处理方式                                     |
| ---------------- | -------------------------------------------- |
| 更换服务器       | 使用新服务器机器码重新申请 License           |
| 虚拟机迁移       | 检查硬件标识是否变化，必要时重新签发 License |
| 容器调度         | 绑定宿主机节点标识或部署环境标识             |
| 集群扩容         | 使用多机器码 License 或节点数授权模式        |
| License 文件拿错 | 更换为当前机器对应的 License 文件            |

机器码排查步骤如下：

```bash
# 查看业务系统生成的当前机器码，具体命令由客户端工具提供
java -jar license-machine-code-tool.jar

# 查看 License 文件中的机器码，可通过服务端校验接口或管理端页面查看
# 不建议手动修改 License 文件，修改后会导致签名无效
```

设计要求如下：

1. 机器码不匹配时默认阻止应用启动。
2. 日志可以输出机器码摘要，但不应输出完整硬件明文。
3. 机器码生成规则应保持稳定，避免系统升级后机器码算法变化。
4. 容器和云服务器场景应提供可配置的机器码采集策略。
5. 集群部署应支持多机器码或节点数量授权，避免每个节点单独签发授权。
6. 不允许通过配置跳过机器码校验，除非是明确隔离的本地开发环境。

### License 签名无效

License 签名无效表示客户端使用公钥校验 License 文件时失败。该异常通常说明 License 文件被篡改、文件内容损坏、公钥不匹配、签名算法不一致，或者 License 文件不是由当前服务端私钥签发。

异常判定条件如下：

```text
使用 publicKey 校验 signature 失败
```

常见原因如下：

| 原因                   | 说明                                        |
| ---------------------- | ------------------------------------------- |
| 文件被修改             | 手动修改过有效期、模块、机器码等字段        |
| 文件传输损坏           | 邮件、复制、上传过程中内容损坏              |
| 公钥不匹配             | 客户端公钥不是签发该 License 的私钥对应公钥 |
| 签名算法不一致         | 服务端签名算法和客户端验签算法不一致        |
| Payload 序列化不一致   | 客户端验签时生成的签名原文与服务端不一致    |
| License 文件版本不兼容 | 客户端不支持当前 License 文件结构           |

错误示例：

```text
错误码：LICENSE_SIGNATURE_INVALID
错误信息：License 签名无效
处理建议：请确认 License 文件未被修改，并使用正确版本的公钥和客户端组件
```

日志示例：

```text
License 校验失败，原因：签名无效，License编号：LIC-20260506-000001，签名算法：SHA256withRSA
```

处理策略如下：

| 场景           | 处理方式                                   |
| -------------- | ------------------------------------------ |
| 文件被手动修改 | 重新下载或重新签发 License 文件            |
| 公钥不匹配     | 更新客户端公钥或使用正确版本客户端         |
| 文件损坏       | 重新传输 License 文件，并比对 SHA-256 摘要 |
| 算法不一致     | 检查服务端和客户端签名算法配置             |
| 结构不兼容     | 升级客户端组件或重新生成兼容格式 License   |

排查步骤如下：

```bash
# 查看 License 文件摘要
sha256sum /data/app/license/PRODUCT-ERP.lic

# 对比服务端记录的 file_sha256，确认文件是否完整
# 如果摘要不一致，应重新下载 License 文件
```

设计要求如下：

1. 签名无效时必须阻止应用启动。
2. 客户端不得提供忽略签名错误的配置项。
3. 验签必须发生在读取授权内容并应用到业务逻辑之前。
4. 日志中不得输出完整签名、公钥原文或私钥相关信息。
5. 服务端和客户端必须使用同一套 Payload 稳定序列化规则。
6. 如果支持密钥版本，应根据 License 文件中的 `keyId` 选择对应公钥。
7. 如果签名无效，应优先排查文件是否被修改、公钥是否匹配、文件摘要是否一致。



## 测试方案

测试方案用于验证 License 系统在正常业务流程、接口调用、文件生成、签名校验、客户端接入和异常处理场景下是否符合预期。测试范围应覆盖服务端 License 管理能力、客户端 License 校验能力、数据库状态流转和安全控制逻辑。

测试目标如下：

| 测试目标       | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| 验证核心流程   | 覆盖客户创建、产品创建、授权申请、License 生成、下载、校验流程 |
| 验证签名安全   | 确认 License 文件被修改后无法通过验签                        |
| 验证机器绑定   | 确认 License 只能在绑定机器码的环境中使用                    |
| 验证有效期控制 | 确认未生效、已过期、正常有效状态判断正确                     |
| 验证异常处理   | 确认文件不存在、签名无效、机器码不匹配等异常返回明确         |
| 验证权限控制   | 确认未授权用户不能生成、下载、吊销 License                   |
| 验证审计日志   | 确认关键操作均记录审计日志                                   |

### 单元测试

单元测试用于验证 License 系统中的独立业务组件是否正确，包括 License Payload 构建、稳定序列化、签名生成、签名校验、机器码生成、有效期判断和授权范围判断等逻辑。

建议优先覆盖以下类或组件：

| 测试对象                | 测试重点                                           |
| ----------------------- | -------------------------------------------------- |
| `LicensePayloadBuilder` | Payload 字段是否完整，客户、产品、授权范围是否正确 |
| `LicenseCanonicalizer`  | JSON 序列化结果是否稳定，字段顺序是否一致          |
| `LicenseSigner`         | 私钥签名是否成功，签名结果是否可被公钥验证         |
| `LicenseVerifier`       | 签名、产品、机器码、有效期、模块是否校验正确       |
| `MachineCodeGenerator`  | 相同机器信息是否生成相同机器码                     |
| `LicenseRuntimeChecker` | 模块授权和数量限制判断是否正确                     |
| `LicenseFileStorage`    | License 文件是否能正确写入和读取                   |
| `LicenseDigestService`  | SHA-256 摘要是否计算正确                           |

单元测试用例建议如下：

| 用例               | 输入                             | 预期结果             |
| ------------------ | -------------------------------- | -------------------- |
| 生成 Payload 成功  | 客户、产品、机器码、授权范围完整 | 返回完整 Payload     |
| Payload 稳定序列化 | 相同字段不同顺序                 | 序列化结果一致       |
| 签名生成成功       | 合法私钥和 Payload               | 返回非空签名         |
| 签名校验成功       | 原始 Payload、签名、公钥         | 校验通过             |
| 签名校验失败       | 修改 Payload 后再验签            | 校验失败             |
| 机器码一致         | 相同机器信息                     | 生成相同 machineCode |
| 机器码不一致       | 不同机器信息                     | 生成不同 machineCode |
| License 未过期     | 当前时间在有效期内               | 校验通过             |
| License 已过期     | 当前时间晚于过期时间             | 抛出过期异常         |
| 模块已授权         | 检查 `report` 模块               | 校验通过             |
| 模块未授权         | 检查不存在模块                   | 抛出模块未授权异常   |
| 数量未超限         | 当前数量小于授权上限             | 校验通过             |
| 数量超限           | 当前数量大于授权上限             | 抛出数量超限异常     |

测试依赖建议如下：

```xml
<!-- Spring Boot 测试依赖，包含 JUnit 5、Mockito、AssertJ 等测试工具 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Hutool 工具包，用于测试中的 JSON、文件、摘要和日期处理 -->
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.32</version>
    <scope>test</scope>
</dependency>
```

推荐测试文件位置如下：

```text
src/test/java/io/github/atengk/license/generator/LicenseSignerTest.java
src/test/java/io/github/atengk/license/verifier/LicenseVerifierTest.java
src/test/java/io/github/atengk/license/client/MachineCodeGeneratorTest.java
src/test/java/io/github/atengk/license/client/LicenseRuntimeCheckerTest.java
```

License 签名校验单元测试示例：

```java
package io.github.atengk.license.generator;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.CharsetUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.security.*;

/**
 * License 签名单元测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
class LicenseSignerTest {

    /**
     * 测试 RSA 签名和验签
     *
     * @throws Exception 测试异常
     */
    @Test
    void shouldVerifySignatureWhenPayloadNotChanged() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        String payload = "{\"licenseNo\":\"LIC-20260506-000001\",\"productCode\":\"PRODUCT-ERP\"}";
        Charset charset = CharsetUtil.CHARSET_UTF_8;

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(keyPair.getPrivate());
        signer.update(payload.getBytes(charset));
        String signatureText = Base64.encode(signer.sign());

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(keyPair.getPublic());
        verifier.update(payload.getBytes(charset));

        Assertions.assertTrue(verifier.verify(Base64.decode(signatureText)));
    }

    /**
     * 测试 Payload 被修改后验签失败
     *
     * @throws Exception 测试异常
     */
    @Test
    void shouldRejectSignatureWhenPayloadChanged() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        String originalPayload = "{\"licenseNo\":\"LIC-20260506-000001\",\"maxUsers\":100}";
        String changedPayload = "{\"licenseNo\":\"LIC-20260506-000001\",\"maxUsers\":999}";

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(keyPair.getPrivate());
        signer.update(originalPayload.getBytes(CharsetUtil.CHARSET_UTF_8));
        byte[] signatureBytes = signer.sign();

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(keyPair.getPublic());
        verifier.update(changedPayload.getBytes(CharsetUtil.CHARSET_UTF_8));

        Assertions.assertFalse(verifier.verify(signatureBytes));
    }

}
```

设计要求如下：

1. 单元测试不依赖外部数据库、Redis、文件服务等中间件。
2. 密钥测试应使用测试环境临时生成的密钥对，不应使用生产私钥。
3. 签名、验签、序列化、机器码、有效期判断必须具备独立测试。
4. 对异常分支必须有明确断言，不能只测试正常流程。
5. 测试数据应与生产数据隔离，不能包含真实客户信息和真实 License 文件。

### 接口测试

接口测试用于验证 License 服务端 REST API 是否符合设计，包括参数校验、业务状态流转、权限控制、响应结构和审计日志记录。

建议覆盖以下接口：

| 接口类型         | 测试内容                                           |
| ---------------- | -------------------------------------------------- |
| 客户管理接口     | 新增、修改、分页、详情、启用、禁用、删除           |
| 产品管理接口     | 新增、修改、分页、详情、模块配置、默认限制         |
| License 申请接口 | 创建申请、修改草稿、查询详情、分页查询、续期、吊销 |
| License 生成接口 | 草稿生成 License、重复生成控制、生成失败处理       |
| License 下载接口 | 文件下载、文件不存在、状态不允许下载、下载日志     |
| License 校验接口 | 上传文件校验、按编号校验、在线校验、模块校验       |
| 审计日志接口     | 操作日志查询、按 License 编号查询、按操作人查询    |

接口测试工具建议如下：

| 工具             | 用途                                     |
| ---------------- | ---------------------------------------- |
| JUnit 5          | 编写自动化接口测试                       |
| MockMvc          | Spring Boot 内部接口测试                 |
| Testcontainers   | 启动 MySQL、PostgreSQL、Redis 等测试容器 |
| Postman / Apifox | 手动接口调试和接口集合管理               |
| curl             | 部署后接口快速验证                       |

接口测试用例建议如下：

| 用例               | 步骤                             | 预期结果                      |
| ------------------ | -------------------------------- | ----------------------------- |
| 创建客户成功       | 调用新增客户接口                 | 返回客户 ID，状态为 `ENABLED` |
| 重复客户编码       | 使用相同 `customerCode` 创建客户 | 返回业务异常                  |
| 创建产品成功       | 调用新增产品接口                 | 返回产品 ID，状态为 `ENABLED` |
| 创建 License 草稿  | 选择启用客户和产品               | 返回 `DRAFT` 状态             |
| 禁用客户创建授权   | 客户状态为 `DISABLED`            | 创建失败                      |
| 生成 License 成功  | 对 `DRAFT` 授权调用生成接口      | 状态变为 `ISSUED`             |
| 重复生成 License   | 对 `ISSUED` 授权再次生成         | 返回状态冲突                  |
| 下载 License 成功  | 对 `ISSUED` 授权调用下载接口     | 返回 `.lic` 文件              |
| 吊销 License 成功  | 管理员调用吊销接口               | 状态变为 `REVOKED`            |
| 无权限生成 License | 普通用户调用生成接口             | 返回 403                      |
| 查询审计日志       | 按 License 编号查询              | 返回生成、下载、吊销日志      |

接口测试请求示例：

```bash
# 新增客户
curl -X POST http://127.0.0.1:8080/api/license-server/customers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "customerCode": "CUST-001",
    "customerName": "某某科技有限公司",
    "contactName": "张三",
    "contactPhone": "13800000000",
    "status": "ENABLED"
  }'

# 新增产品
curl -X POST http://127.0.0.1:8080/api/license-server/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "productCode": "PRODUCT-ERP",
    "productName": "企业管理平台",
    "productVersion": "3.0.0",
    "moduleConfig": [
      {"moduleCode": "system", "moduleName": "系统管理"},
      {"moduleCode": "report", "moduleName": "报表中心"}
    ],
    "defaultLimits": {
      "maxUsers": 100,
      "maxTenants": 1,
      "maxNodes": 1
    }
  }'
```

接口测试要求如下：

1. 所有接口响应必须符合统一响应结构。
2. 参数缺失、参数格式错误、业务状态冲突应返回明确错误码。
3. License 生成、下载、吊销接口必须验证权限。
4. License 生成后必须校验数据库状态、文件路径、文件摘要和审计日志。
5. 下载接口应验证响应头中的文件名和摘要。
6. 接口自动化测试应清理测试数据，避免影响后续测试。

### License 校验测试

License 校验测试用于验证 License 文件在客户端和服务端是否能够正确解析、验签和执行业务校验。该测试是 License 系统的核心测试内容。

校验测试范围如下：

| 测试项         | 说明                                          |
| -------------- | --------------------------------------------- |
| 文件解析测试   | 校验 `.lic` 文件结构是否能正确解析            |
| 签名校验测试   | 校验原始 License 文件是否能通过验签           |
| 篡改测试       | 修改 License 内容后应验签失败                 |
| 产品校验测试   | 产品编码不一致时应校验失败                    |
| 机器码校验测试 | 当前机器码和 License 机器码不一致时应校验失败 |
| 有效期测试     | 未生效、有效、已过期三种状态判断正确          |
| 模块授权测试   | 已授权模块允许访问，未授权模块拒绝访问        |
| 数量限制测试   | 用户数、租户数、节点数超过授权上限时拒绝操作  |
| 多节点测试     | 多机器码 License 中当前机器码存在时通过校验   |
| 文件更新测试   | 替换 License 文件后重新加载上下文             |

License 校验测试矩阵如下：

| 场景           | License 文件       | 产品编码 | 机器码 | 时间         | 预期结果       |
| -------------- | ------------------ | -------- | ------ | ------------ | -------------- |
| 正常授权       | 原始文件           | 匹配     | 匹配   | 有效期内     | 校验通过       |
| 文件被篡改     | 修改模块或过期时间 | 匹配     | 匹配   | 有效期内     | 签名无效       |
| 产品不匹配     | 原始文件           | 不匹配   | 匹配   | 有效期内     | 产品不匹配     |
| 机器码不匹配   | 原始文件           | 匹配     | 不匹配 | 有效期内     | 机器码不匹配   |
| License 未生效 | 原始文件           | 匹配     | 匹配   | 早于生效时间 | License 未生效 |
| License 已过期 | 原始文件           | 匹配     | 匹配   | 晚于过期时间 | License 已过期 |
| 模块未授权     | 原始文件           | 匹配     | 匹配   | 有效期内     | 模块拒绝       |
| 数量超限       | 原始文件           | 匹配     | 匹配   | 有效期内     | 数量超限       |

License 文件篡改测试示例：

```bash
# 复制一份 License 文件用于测试
cp LIC-20260506-000001.lic LIC-20260506-000001-tampered.lic

# 手动修改 License 中的授权用户数、过期时间或模块列表
vi LIC-20260506-000001-tampered.lic

# 调用服务端校验接口，预期返回 LICENSE_SIGNATURE_INVALID
curl -X POST http://127.0.0.1:8080/api/license-server/licenses/verify-file \
  -H "Authorization: Bearer ${TOKEN}" \
  -F "file=@LIC-20260506-000001-tampered.lic" \
  -F "productCode=PRODUCT-ERP" \
  -F "machineCode=8f2a7d0b6e9c4a11"
```

客户端启动校验测试示例：

```bash
# 使用正确 License 启动，预期启动成功
java -jar product-erp.jar \
  --license.client.license-path=/data/app/license/PRODUCT-ERP.lic \
  --license.client.product-code=PRODUCT-ERP

# 使用错误机器码 License 启动，预期启动失败
java -jar product-erp.jar \
  --license.client.license-path=/data/app/license/PRODUCT-ERP-wrong-machine.lic \
  --license.client.product-code=PRODUCT-ERP
```

校验测试要求如下：

1. License 校验测试必须覆盖正常文件和篡改文件。
2. 必须验证签名校验发生在业务字段读取之前。
3. 必须验证 License 文件内容被修改后无法通过验签。
4. 必须验证客户端和服务端使用相同公钥时验签结果一致。
5. 必须验证机器码、产品编码、有效期和授权模块的失败场景。
6. 必须验证文件更新后 License 上下文是否正确刷新。
7. 如果支持多公钥版本，应验证 `keyId` 选择公钥的逻辑。

### 异常场景测试

异常场景测试用于验证系统在 License 异常、文件异常、配置异常、权限异常和环境异常时是否能够正确处理，并输出明确错误信息。

异常测试用例建议如下：

| 异常场景           | 构造方式                          | 预期结果                          |
| ------------------ | --------------------------------- | --------------------------------- |
| License 文件不存在 | 配置一个不存在的文件路径          | 返回 `LICENSE_FILE_NOT_FOUND`     |
| License 文件不可读 | 移除文件读取权限                  | 返回 `LICENSE_FILE_READ_FAILED`   |
| License 格式错误   | 上传非 JSON 或非法结构文件        | 返回 `LICENSE_FORMAT_INVALID`     |
| License 签名无效   | 修改 License 文件内容             | 返回 `LICENSE_SIGNATURE_INVALID`  |
| 公钥错误           | 使用错误公钥验签                  | 返回 `LICENSE_SIGNATURE_INVALID`  |
| 公钥文件不存在     | 配置错误公钥路径                  | 返回 `LICENSE_PUBLIC_KEY_INVALID` |
| 产品不匹配         | 当前系统产品编码与 License 不一致 | 返回 `LICENSE_PRODUCT_MISMATCH`   |
| 机器码不匹配       | 使用其他机器的 License            | 返回 `LICENSE_MACHINE_MISMATCH`   |
| License 未生效     | 当前时间早于 `notBefore`          | 返回 `LICENSE_NOT_EFFECTIVE`      |
| License 已过期     | 当前时间晚于 `notAfter`           | 返回 `LICENSE_EXPIRED`            |
| 模块未授权         | 访问未授权模块                    | 返回 `LICENSE_MODULE_DENIED`      |
| 数量超限           | 新增资源超过授权限制              | 返回 `LICENSE_LIMIT_EXCEEDED`     |
| 已吊销 License     | 在线校验服务端状态为 `REVOKED`    | 返回 `LICENSE_REVOKED`            |
| 无权限下载         | 普通用户下载 License 文件         | 返回 403                          |
| 私钥不可用         | 服务端私钥路径错误                | License 生成失败                  |

文件权限异常测试命令如下：

```bash
# 移除 License 文件读取权限
chmod 000 /data/app/license/PRODUCT-ERP.lic

# 启动客户端应用，预期返回 LICENSE_FILE_READ_FAILED
java -jar product-erp.jar \
  --license.client.license-path=/data/app/license/PRODUCT-ERP.lic

# 恢复文件权限
chmod 600 /data/app/license/PRODUCT-ERP.lic
```

过期 License 测试建议如下：

```text
1. 生成一份过期时间早于当前时间的 License
2. 将该 License 放入客户端配置路径
3. 启动业务系统
4. 预期系统启动失败，并输出 LICENSE_EXPIRED
5. 替换为有效 License
6. 重新启动或刷新 License
7. 预期系统恢复正常
```

异常场景测试要求如下：

1. 每一种异常都应有明确错误码和错误信息。
2. 客户端启动异常应在日志中输出可定位原因。
3. 服务端接口异常应返回统一响应结构。
4. 敏感信息不得出现在异常响应中，例如私钥路径、完整签名、数据库密码。
5. 异常测试应同时覆盖客户端和服务端。
6. 高敏感接口的异常场景应验证审计日志是否记录。

## 部署与使用

部署与使用用于说明 License 服务端如何部署、License 文件如何生成、客户端业务系统如何接入，以及运维人员在续期、替换、排查异常时的操作流程。

部署环境建议如下：

| 组件        | 建议                       |
| ----------- | -------------------------- |
| JDK         | 17+                        |
| Spring Boot | 3.x                        |
| 数据库      | MySQL 8+ 或 PostgreSQL 15+ |
| Redis       | 可选，6+ 或 7+             |
| 文件存储    | 本地磁盘、MinIO 或对象存储 |
| 操作系统    | Linux 发行版               |
| 容器化      | Docker 或 Kubernetes       |
| 日志        | 本地日志、ELK 或 Loki      |

### 服务端部署

服务端部署用于安装和启动 License 管理系统。服务端需要连接数据库，读取签名私钥，写入 License 文件，并对管理端接口提供访问能力。

服务端部署前置条件如下：

| 条件             | 说明                                 |
| ---------------- | ------------------------------------ |
| JDK 已安装       | 版本不低于 17                        |
| 数据库已创建     | 初始化 License 系统业务表            |
| 私钥已生成       | 服务端需要读取 RSA 私钥              |
| 文件目录已创建   | 用于保存生成后的 `.lic` 文件         |
| 配置文件已准备   | 包含数据库、密钥路径、文件路径等配置 |
| 管理端账号已创建 | 用于登录系统生成 License             |

推荐目录结构如下：

```text
/opt/license-server
├── app
│   └── license-server.jar
├── config
│   └── application-prod.yml
├── keys
│   └── private_key.pem
├── files
│   └──
└── logs
    └──
```

生产配置示例：

```yaml
server:
  port: 8080 # License 服务端口

spring:
  application:
    name: license-server # 服务名称

  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver # MySQL 8 驱动
    url: jdbc:mysql://127.0.0.1:3306/license_server?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: ${LICENSE_DB_USERNAME} # 数据库账号，通过环境变量注入
    password: ${LICENSE_DB_PASSWORD} # 数据库密码，通过环境变量注入

license:
  crypto:
    algorithm: SHA256withRSA # License 签名算法
    private-key-path: /opt/license-server/keys/private_key.pem # 私钥路径，仅服务端保存
  storage:
    type: local # 文件存储类型
    local-path: /opt/license-server/files # License 文件保存目录

logging:
  file:
    name: /opt/license-server/logs/license-server.log # 服务日志路径
```

部署命令如下：

```bash
# 创建部署目录
mkdir -p /opt/license-server/{app,config,keys,files,logs}

# 上传应用包
cp license-server.jar /opt/license-server/app/license-server.jar

# 上传生产配置文件
cp application-prod.yml /opt/license-server/config/application-prod.yml

# 上传私钥文件
cp private_key.pem /opt/license-server/keys/private_key.pem

# 设置目录权限
chmod 700 /opt/license-server/keys
chmod 600 /opt/license-server/keys/private_key.pem
chmod 755 /opt/license-server/files
chmod 755 /opt/license-server/logs

# 设置数据库环境变量
export LICENSE_DB_USERNAME=license_user
export LICENSE_DB_PASSWORD=license_password

# 启动服务
java -jar /opt/license-server/app/license-server.jar \
  --spring.config.location=/opt/license-server/config/application-prod.yml \
  --spring.profiles.active=prod
```

以上命令完成目录创建、应用包上传、配置文件准备、私钥权限设置和服务启动。生产环境建议将数据库账号密码配置到系统环境变量、容器 Secret 或配置中心中。

systemd 服务配置如下：

文件位置：`/etc/systemd/system/license-server.service`

```ini
[Unit]
Description=License Server
After=network.target

[Service]
Type=simple
User=license
Group=license
Environment="LICENSE_DB_USERNAME=license_user"
Environment="LICENSE_DB_PASSWORD=license_password"
ExecStart=/usr/bin/java -jar /opt/license-server/app/license-server.jar --spring.config.location=/opt/license-server/config/application-prod.yml --spring.profiles.active=prod
Restart=always
RestartSec=10
StandardOutput=append:/opt/license-server/logs/license-server.out.log
StandardError=append:/opt/license-server/logs/license-server.err.log

[Install]
WantedBy=multi-user.target
```

启动和查看状态命令如下：

```bash
# 重新加载 systemd 配置
systemctl daemon-reload

# 启动 License 服务
systemctl start license-server

# 设置开机自启
systemctl enable license-server

# 查看服务状态
systemctl status license-server

# 查看实时日志
tail -f /opt/license-server/logs/license-server.log
```

部署验证方式如下：

```bash
# 检查服务端口
curl http://127.0.0.1:8080/actuator/health

# 预期响应
# {"status":"UP"}
```

部署要求如下：

1. 私钥文件必须只允许服务运行用户读取。
2. License 文件目录必须具备写入权限。
3. 数据库账号应使用最小权限，不建议使用 root 账号。
4. 服务端接口应通过 HTTPS 或网关暴露。
5. 生产环境应开启接口认证和操作审计。
6. 日志目录应配置轮转策略，避免磁盘被写满。
7. 私钥、数据库密码、对象存储密钥不得提交到代码仓库。

### License 文件生成

License 文件生成用于说明管理员如何在服务端创建授权并生成可交付给客户的 `.lic` 文件。该流程通常由交付人员或授权管理员完成。

生成前需要准备以下信息：

| 信息         | 说明                         |
| ------------ | ---------------------------- |
| 客户信息     | 客户编码、客户名称、联系人   |
| 产品信息     | 产品编码、产品名称、产品版本 |
| License 类型 | 试用、正式、临时             |
| 机器码       | 客户部署服务器生成的机器码   |
| 有效期       | 生效时间和过期时间           |
| 授权模块     | 客户购买或试用的功能模块     |
| 授权限制     | 用户数、租户数、节点数等     |
| 备注信息     | 合同编号、项目名称或交付说明 |

License 文件生成流程如下：

```text
1. 客户端部署人员在客户服务器上运行机器码采集工具
2. 将生成的 machineCode 提供给授权管理员
3. 授权管理员在 License 服务端创建客户信息
4. 授权管理员在 License 服务端创建产品信息
5. 授权管理员创建 License 授权申请
6. 填写机器码、有效期、授权模块和授权限制
7. 服务端校验客户、产品和授权范围
8. 授权管理员点击生成 License
9. 服务端使用私钥签名并生成 .lic 文件
10. 授权管理员下载 License 文件
11. 将 License 文件交付给客户或部署人员
```

机器码采集命令示例：

```bash
# 在客户服务器上执行机器码采集工具
java -jar license-machine-code-tool.jar

# 示例输出
# machineCode=8f2a7d0b6e9c4a11
```

License 申请接口调用示例：

```bash
curl -X POST http://127.0.0.1:8080/api/license-server/authorizations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "licenseType": "OFFICIAL",
    "customerId": 1900000000000000001,
    "productId": 1900000000000000002,
    "machineCode": "8f2a7d0b6e9c4a11",
    "effectiveTime": "2026-05-06 00:00:00",
    "expireTime": "2027-05-05 23:59:59",
    "authorizedModules": ["system", "report", "workflow"],
    "authorizedLimits": {
      "maxUsers": 200,
      "maxTenants": 5,
      "maxNodes": 3
    },
    "remark": "正式授权"
  }'
```

License 生成接口调用示例：

```bash
curl -X POST http://127.0.0.1:8080/api/license-server/authorizations/1900000000000000003/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "force": false
  }'
```

License 下载接口调用示例：

```bash
curl -X GET http://127.0.0.1:8080/api/license-server/authorizations/1900000000000000003/download \
  -H "Authorization: Bearer ${TOKEN}" \
  -o LIC-20260506-000001.lic
```

文件生成后建议校验摘要：

```bash
# 计算 License 文件摘要
sha256sum LIC-20260506-000001.lic

# 与服务端返回的 fileSha256 比对，确认文件完整
```

生成要求如下：

1. 机器码必须由客户实际部署环境生成。
2. 授权模块必须属于产品模块配置范围。
3. 有效期必须符合合同或试用策略。
4. 生成 License 前应确认客户和产品状态均为启用。
5. 生成后应记录操作日志，包括操作人、客户、产品、机器码和授权范围。
6. License 文件不应通过不安全渠道传输，建议使用加密压缩包、交付平台或内网文件系统。
7. License 文件生成后不建议直接修改；如需变更，应重新生成。

### 客户端接入流程

客户端接入流程用于说明业务系统如何引入 License Client Starter，并在启动和运行过程中完成授权校验。

客户端接入步骤如下：

```text
1. 在业务系统中引入 license-client-starter 依赖
2. 将 public_key.pem 放入业务系统资源目录或外部配置目录
3. 将客户 License 文件放入服务器指定路径
4. 在 application.yml 中配置产品编码、License 路径和公钥路径
5. 启动业务系统
6. 客户端组件自动读取 License 文件并执行完整校验
7. 校验通过后写入 License 运行时上下文
8. 业务代码通过注解或显式调用进行运行时授权校验
```

客户端配置示例：

```yaml
license:
  client:
    enabled: true # 启用 License 校验
    product-code: PRODUCT-ERP # 当前系统产品编码
    product-version: 3.0.0 # 当前系统版本
    license-path: /data/app/license/PRODUCT-ERP.lic # License 文件路径
    public-key-path: classpath:license/public_key.pem # 公钥路径
    fail-fast: true # 校验失败时终止启动
    allow-clock-skew-seconds: 300 # 允许时间误差
    runtime-check:
      enabled: true # 开启运行时校验
      interval-seconds: 300 # 定时校验间隔
```

客户端部署目录建议如下：

```text
/data/app/product-erp
├── app
│   └── product-erp.jar
├── config
│   └── application-prod.yml
├── license
│   └── PRODUCT-ERP.lic
└── logs
    └── product-erp.log
```

客户端启动命令如下：

```bash
java -jar /data/app/product-erp/app/product-erp.jar \
  --spring.config.location=/data/app/product-erp/config/application-prod.yml \
  --license.client.license-path=/data/app/product-erp/license/PRODUCT-ERP.lic
```

客户端启动成功日志应包含以下内容：

```text
License 校验开始，产品编码：PRODUCT-ERP
License 文件读取成功，License编号：LIC-20260506-000001
License 签名校验通过
License 机器码校验通过
License 有效期校验通过，到期时间：2027-05-05 23:59:59
License 校验完成，应用允许启动
```

运行时模块校验示例：

```java
package io.github.atengk.report.controller;

import io.github.atengk.license.client.annotation.RequireLicenseModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报表接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
public class ReportController {

    /**
     * 导出报表
     *
     * @return 导出结果
     */
    @RequireLicenseModule("report")
    @GetMapping("/api/reports/export")
    public String exportReport() {
        log.info("报表导出授权校验通过，开始执行导出");
        return "导出成功";
    }

}
```

客户端接入要求如下：

1. `product-code` 必须与 License 文件中的产品编码一致。
2. 公钥必须与服务端签发 License 使用的私钥匹配。
3. License 文件应放在外部目录，便于续期替换。
4. 启动阶段必须执行完整校验。
5. 关键功能应增加运行时模块授权校验。
6. 用户数、租户数、节点数等限制应在 Service 层校验。
7. 客户端不得包含私钥或 License 生成逻辑。
8. 生产环境不得关闭 License 校验。

### 运维使用流程

运维使用流程用于指导部署人员和运维人员完成 License 文件替换、续期、排查和备份工作。该流程重点保证 License 文件更新过程安全、可回滚、可验证。

常见运维场景如下：

| 场景         | 处理方式                                   |
| ------------ | ------------------------------------------ |
| 首次部署     | 放置 License 文件，配置路径，启动业务系统  |
| License 续期 | 备份旧文件，替换新文件，重启或刷新 License |
| 授权模块变更 | 替换新 License 文件，重新加载授权上下文    |
| 机器迁移     | 在新机器生成机器码，重新申请 License       |
| License 过期 | 联系授权管理员续期，替换新文件             |
| 签名无效     | 重新下载 License 文件，检查公钥和文件摘要  |
| 机器码不匹配 | 检查是否更换服务器或挂载了错误 License     |
| 文件不存在   | 检查配置路径、文件权限和容器挂载           |

首次部署流程如下：

```text
1. 获取业务系统部署包
2. 获取 public_key.pem 公钥文件
3. 获取客户 License 文件
4. 创建应用部署目录和 License 目录
5. 将 License 文件放入指定目录
6. 修改 application-prod.yml 中的 license.client 配置
7. 启动业务系统
8. 查看启动日志，确认 License 校验通过
9. 访问健康检查接口，确认服务正常
```

License 续期替换流程如下：

```bash
# 进入客户端 License 目录
cd /data/app/product-erp/license

# 备份旧 License 文件
cp PRODUCT-ERP.lic PRODUCT-ERP.lic.bak.$(date +%Y%m%d%H%M%S)

# 上传新 License 文件到临时文件
cp /tmp/PRODUCT-ERP-new.lic PRODUCT-ERP.lic.tmp

# 调整文件权限
chmod 600 PRODUCT-ERP.lic.tmp

# 原子替换 License 文件
mv PRODUCT-ERP.lic.tmp PRODUCT-ERP.lic

# 重启业务系统
systemctl restart product-erp

# 查看启动日志
tail -f /data/app/product-erp/logs/product-erp.log
```

该流程先备份旧文件，再通过临时文件替换正式 License 文件，避免应用读取到半写入文件。

如果业务系统支持 License 热加载，可以使用刷新接口：

```bash
curl -X POST http://127.0.0.1:8080/api/system/license/reload \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{}'
```

刷新成功后应返回新的 License 编号和过期时间：

```json
{
  "code": "200",
  "message": "License 重新加载成功",
  "data": {
    "licenseNo": "LIC-20260506-000002",
    "notAfter": "2028-05-05 23:59:59"
  }
}
```

运维排查命令如下：

```bash
# 查看 License 文件是否存在
ls -l /data/app/product-erp/license/PRODUCT-ERP.lic

# 查看 License 文件摘要
sha256sum /data/app/product-erp/license/PRODUCT-ERP.lic

# 查看应用日志中的 License 校验记录
grep "License" /data/app/product-erp/logs/product-erp.log

# 查看服务状态
systemctl status product-erp

# 查看最近 200 行日志
tail -n 200 /data/app/product-erp/logs/product-erp.log
```

运维检查清单如下：

| 检查项   | 说明                                          |
| -------- | --------------------------------------------- |
| 文件路径 | `license.client.license-path` 是否配置正确    |
| 文件权限 | 应用运行用户是否可读取 License 文件           |
| 文件摘要 | 本地文件 SHA-256 是否与服务端记录一致         |
| 产品编码 | 客户端 `product-code` 是否与 License 文件一致 |
| 公钥版本 | 客户端公钥是否匹配服务端签发密钥              |
| 机器码   | 当前服务器机器码是否与 License 绑定机器码一致 |
| 有效期   | License 是否已经过期                          |
| 日志信息 | 启动日志是否存在明确异常原因                  |
| 容器挂载 | 容器内路径是否能看到 License 文件             |
| 系统时间 | 服务器时间是否明显异常                        |

运维使用要求如下：

1. 替换 License 文件前必须备份旧文件。
2. 替换后必须通过启动日志或刷新接口确认校验成功。
3. 不允许手动修改 License 文件内容，修改后会导致签名无效。
4. License 文件应限制访问权限，避免被无关用户复制或覆盖。
5. 机器迁移、节点扩容、授权模块变更都应重新申请 License。
6. License 即将到期时应提前续期，不建议到期后再处理。
7. 生产问题排查时优先查看错误码、License 编号、产品编码、机器码和过期时间。
8. 运维人员不得接触服务端私钥，私钥仅由 License 服务端使用。
