# Spring Boot 集成 Jasypt 配置加密

本文介绍在 Spring Boot 3 项目中集成 Jasypt，对配置文件中的敏感配置进行加密存储，并在应用启动时自动解密读取。该方案适用于数据库密码、Redis 密码、第三方接口密钥、Token 密钥等敏感配置的保护。

## 功能概述

本章说明 Jasypt 在 Spring Boot 项目中的作用、适用场景以及可加密的配置范围。Jasypt 的目标不是替代密钥管理平台，而是在不明显侵入业务代码的前提下，降低配置文件中敏感信息明文暴露的风险。

### Jasypt 的作用

Jasypt 是 Java 生态中常用的加密解密工具。在 Spring Boot 项目中，通常通过 `jasypt-spring-boot-starter` 集成，用于对配置文件中的敏感属性进行透明解密。

集成 Jasypt 后，可以将原本明文的配置值加密后写入 `application.yml` 或 `application.properties`。应用启动时，Jasypt 会识别 `ENC(...)` 格式的密文，并根据配置的密钥、算法等参数完成解密。

例如，数据库密码不再直接写成明文：

```yaml
spring:
  datasource:
    username: root
    password: ENC(数据库密码密文)
```

应用实际读取配置时，`spring.datasource.password` 会被自动解析为解密后的真实密码。业务代码、数据源组件、Redis 组件等仍然按照普通 Spring Boot 配置方式使用，不需要在业务代码中手动调用解密方法。

Jasypt 主要用于解决以下问题：

| 问题             | 说明                                                    |
| ---------------- | ------------------------------------------------------- |
| 配置文件泄密风险 | 避免数据库密码、接口密钥等敏感信息直接暴露在配置文件中  |
| 代码仓库明文风险 | 即使配置文件提交到 Git，也不会直接泄露明文密码          |
| 配置读取侵入性   | 对 Spring Boot 配置读取方式侵入较低，业务代码基本无感知 |
| 多环境隔离       | 开发、测试、生产环境可以使用不同密文和不同解密密钥      |
| 局部加密         | 只加密敏感字段，不需要加密整个配置文件                  |

需要注意的是，Jasypt 只负责“密文配置值的解密”。如果解密密钥也直接写在同一个配置文件中，加密意义会明显降低。因此，生产环境应通过环境变量、启动参数、容器 Secret 或 CI/CD 密钥变量传递解密密钥。

### 适用场景

Jasypt 适合用于 Spring Boot 项目中的配置级敏感信息保护，尤其适合传统单体应用、内部管理系统、私有化部署项目、测试环境配置保护以及暂未接入 KMS 的中小型系统。

常见适用场景如下：

| 场景                 | 示例配置                             | 说明                                            |
| -------------------- | ------------------------------------ | ----------------------------------------------- |
| 数据库密码加密       | `spring.datasource.password`         | 避免 MySQL、PostgreSQL 等数据库密码明文存储     |
| Redis 密码加密       | `spring.data.redis.password`         | 保护 Redis 单机、哨兵、集群认证密码             |
| 第三方接口密钥加密   | `third-party.sms.secret-key`         | 保护短信、支付、地图、物流等平台密钥            |
| 对象存储密钥加密     | `minio.secret-key`、`oss.secret-key` | 保护 MinIO、阿里云 OSS、腾讯云 COS 等访问密钥   |
| MQ 认证信息加密      | `spring.rabbitmq.password`           | 保护 RabbitMQ、Kafka、RocketMQ 等中间件认证信息 |
| JWT 密钥加密         | `security.jwt.secret`                | 保护 Token 签名密钥                             |
| 内部接口签名密钥加密 | `system.sign.secret`                 | 保护内部系统间调用的签名盐值或密钥              |

Jasypt 不适合用于以下场景：

| 场景                 | 原因                                                         |
| -------------------- | ------------------------------------------------------------ |
| 大规模密钥集中管理   | 应优先使用 Vault、KMS、云厂商 Secret Manager 等专业方案      |
| 数据库字段加密       | Jasypt 配置加密主要面向配置属性，不适合直接承担业务数据字段加密 |
| 文件整体加密         | Jasypt 更适合加密单个配置值，不适合加密整个配置文件或附件    |
| 密钥和密文放在同一处 | 如果解密密钥与密文一起提交，安全收益有限                     |
| 需要密钥轮换审计     | 高安全等级系统应使用具备审计、轮换、权限控制能力的密钥平台   |

实际项目中，可以将 Jasypt 作为配置加密的轻量方案；如果系统安全等级较高，应进一步接入专业密钥管理系统。

### 加密配置范围

Jasypt 的加密对象通常是配置属性值，而不是整个配置文件。只要配置值会进入 Spring Environment，并且使用 `ENC(...)` 格式声明，就可以由 Jasypt 在应用启动时尝试解密。

常见可加密配置范围如下：

| 配置来源                 | 是否适合加密 | 示例                                    |
| ------------------------ | ------------ | --------------------------------------- |
| `application.yml`        | 适合         | `password: ENC(xxx)`                    |
| `application.properties` | 适合         | `spring.datasource.password=ENC(xxx)`   |
| Profile 配置文件         | 适合         | `application-prod.yml`                  |
| 启动参数                 | 适合         | `--third-party.sms.secret-key=ENC(xxx)` |
| 环境变量                 | 适合         | `THIRD_PARTY_SMS_SECRET_KEY=ENC(xxx)`   |
| 自定义配置属性           | 适合         | `system.sign.secret: ENC(xxx)`          |
| 业务数据库字段           | 不建议       | 应使用业务加密方案                      |
| 日志内容                 | 不适合       | 应通过日志脱敏处理                      |

典型配置示例如下：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/jasypt_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    # 数据库密码使用 Jasypt 密文
    password: ENC(your-database-password-cipher)

  data:
    redis:
      host: 127.0.0.1
      port: 6379
      # Redis 密码使用 Jasypt 密文
      password: ENC(your-redis-password-cipher)

third-party:
  sms:
    # 第三方平台访问密钥使用 Jasypt 密文
    access-key: ENC(your-sms-access-key-cipher)
    secret-key: ENC(your-sms-secret-key-cipher)

system:
  sign:
    # 内部接口签名密钥使用 Jasypt 密文
    secret: ENC(your-sign-secret-cipher)
```

只有使用 `ENC(...)` 包裹的配置值才会被 Jasypt 当作密文处理。普通配置值仍然按照 Spring Boot 默认方式读取。

## 环境准备

本章说明 Spring Boot 3 集成 Jasypt 前需要准备的基础环境、依赖配置和推荐项目结构。Spring Boot 3.x 项目需要使用 Java 17 或更高版本；较新的 Spring Boot 3.x 文档也要求 Maven 3.6.3 或更高版本。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/installing.html?utm_source=chatgpt.com))

### Spring Boot 版本要求

Spring Boot 3 集成 Jasypt 时，需要重点确认 JDK、Spring Boot、构建工具和 Jasypt Starter 的版本兼容性。

推荐环境如下：

| 组件           | 推荐版本           | 说明                                                         |
| -------------- | ------------------ | ------------------------------------------------------------ |
| JDK            | 17 或以上          | Spring Boot 3.x 的基础运行要求                               |
| Spring Boot    | 3.x                | 建议使用项目当前稳定小版本                                   |
| Maven          | 3.6.3 或以上       | Spring Boot 3.x 新版本推荐使用                               |
| Gradle         | 7.5 或以上 / 8.x   | 使用 Gradle 项目时参考 Spring Boot 对应版本要求              |
| Jasypt Starter | 4.0.4              | Maven Central 当前可查到 `jasypt-spring-boot-starter` 4.0.4 版本 |
| 配置文件格式   | YAML 或 Properties | 推荐使用 `application.yml`，层级更清晰                       |

`com.github.ulisesbocchio:jasypt-spring-boot-starter` 在 Maven Central 中提供了 `4.0.4` 版本，可作为 Spring Boot 3 项目的集成版本参考。([Maven Central](https://central.sonatype.com/artifact/com.github.ulisesbocchio/jasypt-spring-boot-starter?utm_source=chatgpt.com))

版本选择建议如下：

| 项目情况                | 建议                                               |
| ----------------------- | -------------------------------------------------- |
| 新建 Spring Boot 3 项目 | 优先使用 `jasypt-spring-boot-starter:4.0.4`        |
| 已有 Spring Boot 3 项目 | 先确认当前 Jasypt 版本是否正常，再决定是否升级     |
| Spring Boot 2 升级到 3  | 同时验证 JDK、Spring Boot、Jasypt Starter 的兼容性 |
| 生产环境升级 Jasypt     | 必须验证数据库、Redis、自定义配置是否能正常解密    |

不建议在 Spring Boot 3 项目中继续使用过旧的 Jasypt Starter 版本。升级后应重点验证启动过程、配置解密、连接池初始化和第三方配置读取是否正常。

### Jasypt 依赖配置

Spring Boot 项目推荐通过 `jasypt-spring-boot-starter` 接入 Jasypt。Starter 会自动参与 Spring Boot 配置属性解析流程，使 `ENC(...)` 格式的配置值可以在应用启动时被自动解密。

Maven 项目在 `pom.xml` 中添加以下依赖：

```xml
<dependencies>
    <!-- Spring Boot Web 基础依赖，用于提供 Web 应用能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Jasypt 配置加密依赖，用于支持 ENC(...) 密文自动解密 -->
    <dependency>
        <groupId>com.github.ulisesbocchio</groupId>
        <artifactId>jasypt-spring-boot-starter</artifactId>
        <version>4.0.4</version>
    </dependency>

    <!-- 配置元数据生成器，用于增强 IDE 对 application.yml 的配置提示 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Hutool 工具包，可用于后续编写本地密文生成工具或参数校验逻辑 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.35</version>
    </dependency>
</dependencies>
```

如果项目使用 Gradle，可以添加以下配置：

```groovy
dependencies {
    // Spring Boot Web 基础依赖
    implementation 'org.springframework.boot:spring-boot-starter-web'

    // Jasypt 配置加密依赖
    implementation 'com.github.ulisesbocchio:jasypt-spring-boot-starter:4.0.4'

    // 配置元数据生成器
    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'

    // Hutool 工具包
    implementation 'cn.hutool:hutool-all:5.8.35'
}
```

添加依赖后，还需要在配置文件中声明 Jasypt 的基础加密参数。实际项目中，`password` 不建议直接写死在配置文件中，应优先从环境变量读取。

```yaml
jasypt:
  encryptor:
    # 解密密钥，从环境变量 JASYPT_ENCRYPTOR_PASSWORD 读取
    password: ${JASYPT_ENCRYPTOR_PASSWORD:}
    # 加密算法，需要与生成密文时保持一致
    algorithm: PBEWITHHMACSHA512ANDAES_256
    # 密钥派生迭代次数
    key-obtention-iterations: 1000
    # 盐值生成器
    salt-generator-classname: org.jasypt.salt.RandomSaltGenerator
    # IV 生成器
    iv-generator-classname: org.jasypt.iv.RandomIvGenerator
    # 密文输出格式
    string-output-type: base64
```

这里的 `password` 是 Jasypt 解密密钥，不是业务系统密码。生产环境应通过环境变量、启动参数、Kubernetes Secret、Docker Secret 或 CI/CD 密钥变量注入。

### 项目配置结构

为了区分不同环境的密文配置，建议按照 Spring Boot Profile 管理配置文件。公共配置放在 `application.yml`，开发、测试、生产环境配置分别放在对应的 Profile 文件中。

推荐项目结构如下：

```text
springboot3-jasypt-demo
├── pom.xml
├── README.md
└── src
    ├── main
    │   ├── java
    │   │   └── io
    │   │       └── github
    │   │           └── atengk
    │   │               └── jasypt
    │   │                   ├── JasyptDemoApplication.java
    │   │                   ├── config
    │   │                   │   └── ThirdPartyProperties.java
    │   │                   └── controller
    │   │                       └── ConfigCheckController.java
    │   └── resources
    │       ├── application.yml
    │       ├── application-dev.yml
    │       ├── application-test.yml
    │       └── application-prod.yml
    └── test
        └── java
            └── io
                └── github
                    └── atengk
                        └── jasypt
                            └── JasyptEncryptTest.java
```

各文件职责如下：

| 文件                         | 说明                                            |
| ---------------------------- | ----------------------------------------------- |
| `pom.xml`                    | 管理 Spring Boot、Jasypt、Hutool 等依赖         |
| `application.yml`            | 存放公共配置、Profile 激活配置、Jasypt 基础配置 |
| `application-dev.yml`        | 开发环境配置，使用开发环境密文                  |
| `application-test.yml`       | 测试环境配置，使用测试环境密文                  |
| `application-prod.yml`       | 生产环境配置，使用生产环境密文                  |
| `JasyptDemoApplication.java` | Spring Boot 启动类                              |
| `ThirdPartyProperties.java`  | 自定义配置属性绑定类，用于验证自定义密文配置    |
| `ConfigCheckController.java` | 本地验证接口，用于确认配置是否解密成功          |
| `JasyptEncryptTest.java`     | 本地生成密文和验证解密结果的测试类              |

公共配置文件示例：

```yaml
server:
  port: 8080

spring:
  application:
    name: springboot3-jasypt-demo
  profiles:
    # 默认使用开发环境，部署时通过启动参数覆盖
    active: dev

jasypt:
  encryptor:
    # 生产环境不要把密钥写死在配置文件中
    password: ${JASYPT_ENCRYPTOR_PASSWORD:}
    algorithm: PBEWITHHMACSHA512ANDAES_256
    key-obtention-iterations: 1000
    salt-generator-classname: org.jasypt.salt.RandomSaltGenerator
    iv-generator-classname: org.jasypt.iv.RandomIvGenerator
    string-output-type: base64
```

开发环境配置文件示例：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/jasypt_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    # 开发环境数据库密码密文
    password: ENC(dev-database-password-cipher)

  data:
    redis:
      host: 127.0.0.1
      port: 6379
      # 开发环境 Redis 密码密文
      password: ENC(dev-redis-password-cipher)

third-party:
  sms:
    # 开发环境短信平台密钥密文
    access-key: ENC(dev-sms-access-key-cipher)
    secret-key: ENC(dev-sms-secret-key-cipher)
```

生产环境配置文件示例：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://mysql.prod:3306/jasypt_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: app_user
    # 生产环境数据库密码密文
    password: ENC(prod-database-password-cipher)

  data:
    redis:
      host: redis.prod
      port: 6379
      # 生产环境 Redis 密码密文
      password: ENC(prod-redis-password-cipher)

third-party:
  sms:
    # 生产环境短信平台密钥密文
    access-key: ENC(prod-sms-access-key-cipher)
    secret-key: ENC(prod-sms-secret-key-cipher)
```

项目配置结构的核心原则是：密文可以进入配置文件，解密密钥不要进入代码仓库。开发环境可以临时通过本机环境变量配置密钥，测试和生产环境应由部署平台统一注入密钥。



## 核心配置

本章说明 Jasypt 在 Spring Boot 3 项目中的核心配置项，包括加密密钥、加密算法以及配置文件中的密文格式。使用 `jasypt-spring-boot-starter` 时，如果项目使用 `@SpringBootApplication` 或 `@EnableAutoConfiguration`，加密属性能力会自动应用到整个 Spring Environment，包括系统属性、环境变量、命令行参数、`application.properties`、`application.yml` 等配置源。([GitHub](https://github.com/ulisesbocchio/jasypt-spring-boot?utm_source=chatgpt.com))

### 加密密钥配置

加密密钥是 Jasypt 加密和解密的核心参数。生成密文时使用的密钥，必须与应用启动时解密使用的密钥保持一致，否则应用启动后无法正确解析 `ENC(...)` 配置值。

Jasypt 默认使用 `jasypt.encryptor.password` 作为加密密钥配置项。官方配置中只有 `jasypt.encryptor.password` 是必填项，其余算法、迭代次数、盐值生成器、IV 生成器等参数都有默认值；但生产项目中建议显式配置算法参数，避免版本升级后默认值变化导致旧密文无法解密。([GitHub](https://github.com/ulisesbocchio/jasypt-spring-boot?utm_source=chatgpt.com))

推荐配置方式如下：

```yaml
jasypt:
  encryptor:
    # 解密密钥，从环境变量读取，不建议直接写死在配置文件中
    password: ${JASYPT_ENCRYPTOR_PASSWORD:}
```

本地开发环境可以临时通过环境变量配置密钥：

```bash
export JASYPT_ENCRYPTOR_PASSWORD='ateng-jasypt-secret'
```

Windows PowerShell 可以使用以下方式：

```powershell
$env:JASYPT_ENCRYPTOR_PASSWORD = "ateng-jasypt-secret"
```

也可以在启动应用时通过 JVM 参数传递：

```bash
java -jar springboot3-jasypt-demo.jar \
  --spring.profiles.active=prod \
  --jasypt.encryptor.password='ateng-jasypt-secret'
```

如果使用 Maven 本地启动，可以通过以下方式传递：

```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--jasypt.encryptor.password=ateng-jasypt-secret"
```

以上几种方式中，推荐优先级如下：

| 方式           | 适用环境         | 推荐程度 | 说明                                   |
| -------------- | ---------------- | -------- | -------------------------------------- |
| 环境变量       | 本地、测试、生产 | 推荐     | 通用性好，适合容器和传统部署           |
| 启动参数       | 本地、测试       | 推荐     | 便于临时调试，但命令历史中可能留下密钥 |
| 容器 Secret    | 测试、生产       | 推荐     | 适合 Docker、Kubernetes 等环境         |
| CI/CD 密钥变量 | 测试、生产       | 推荐     | 适合流水线注入                         |
| 写入配置文件   | 仅本地临时验证   | 不推荐   | 密钥容易随代码提交泄露                 |
| 写入代码常量   | 禁止             | 禁止     | 安全性差，维护困难                     |

生产环境应遵循一个基本原则：配置文件中可以保存密文，但解密密钥不应与密文保存在同一个配置源中。

### 加密算法配置

加密算法决定密文生成和解密的具体方式。Jasypt Spring Boot 默认配置中，`jasypt.encryptor.algorithm` 的默认值为 `PBEWITHHMACSHA512ANDAES_256`，同时默认使用 `RandomSaltGenerator`、`RandomIvGenerator` 和 `base64` 输出格式。([GitHub](https://github.com/ulisesbocchio/jasypt-spring-boot?utm_source=chatgpt.com))

推荐在 Spring Boot 3 项目中显式声明以下配置：

```yaml
jasypt:
  encryptor:
    # 解密密钥，从环境变量读取
    password: ${JASYPT_ENCRYPTOR_PASSWORD:}

    # 推荐算法，需与生成密文时保持一致
    algorithm: PBEWITHHMACSHA512ANDAES_256

    # 密钥派生迭代次数
    key-obtention-iterations: 1000

    # 加密器实例池大小，一般保持默认值即可
    pool-size: 1

    # JDK 默认安全提供方
    provider-name: SunJCE

    # 随机盐值生成器
    salt-generator-classname: org.jasypt.salt.RandomSaltGenerator

    # 随机 IV 生成器，AES 类算法通常需要 IV
    iv-generator-classname: org.jasypt.iv.RandomIvGenerator

    # 密文输出格式
    string-output-type: base64
```

核心配置项说明如下：

| 配置项                                      | 推荐值                                | 说明                 |
| ------------------------------------------- | ------------------------------------- | -------------------- |
| `jasypt.encryptor.password`                 | 通过环境变量传入                      | 加密和解密使用的密钥 |
| `jasypt.encryptor.algorithm`                | `PBEWITHHMACSHA512ANDAES_256`         | 加密算法             |
| `jasypt.encryptor.key-obtention-iterations` | `1000`                                | 密钥派生迭代次数     |
| `jasypt.encryptor.pool-size`                | `1`                                   | 加密器实例池大小     |
| `jasypt.encryptor.provider-name`            | `SunJCE`                              | JDK 安全提供方       |
| `jasypt.encryptor.salt-generator-classname` | `org.jasypt.salt.RandomSaltGenerator` | 盐值生成器           |
| `jasypt.encryptor.iv-generator-classname`   | `org.jasypt.iv.RandomIvGenerator`     | IV 生成器            |
| `jasypt.encryptor.string-output-type`       | `base64`                              | 密文输出格式         |

实际项目中，最重要的是保证“生成密文时的配置”和“应用启动解密时的配置”完全一致。以下配置如果任意一项不一致，都可能导致解密失败：

| 必须保持一致的配置 | 说明                                        |
| ------------------ | ------------------------------------------- |
| 加密密钥           | `jasypt.encryptor.password`                 |
| 加密算法           | `jasypt.encryptor.algorithm`                |
| 迭代次数           | `jasypt.encryptor.key-obtention-iterations` |
| 盐值生成器         | `jasypt.encryptor.salt-generator-classname` |
| IV 生成器          | `jasypt.encryptor.iv-generator-classname`   |
| 输出格式           | `jasypt.encryptor.string-output-type`       |

如果项目从旧版本 Jasypt 升级到新版本，尤其需要确认旧密文使用的算法。如果旧密文使用 `PBEWithMD5AndDES` 生成，而新配置改为 `PBEWITHHMACSHA512ANDAES_256`，应用启动时会出现解密失败。

### 配置文件加密格式

Jasypt 默认使用 `ENC(...)` 作为密文格式。只要配置值符合该格式，Jasypt 就会将括号中的内容识别为密文，并在属性读取时进行解密。官方示例中，`secret.property=ENC(...)` 这类配置可以通过 `environment.getProperty(...)` 或 `@Value(...)` 获取到解密后的明文值。([GitHub](https://github.com/ulisesbocchio/jasypt-spring-boot?utm_source=chatgpt.com))

YAML 配置示例：

```yaml
spring:
  datasource:
    username: root
    password: ENC(database-password-cipher)

third-party:
  sms:
    access-key: ENC(sms-access-key-cipher)
    secret-key: ENC(sms-secret-key-cipher)
```

Properties 配置示例：

```properties
spring.datasource.username=root
spring.datasource.password=ENC(database-password-cipher)

third-party.sms.access-key=ENC(sms-access-key-cipher)
third-party.sms.secret-key=ENC(sms-secret-key-cipher)
```

默认格式规则如下：

| 部分       | 说明                  |
| ---------- | --------------------- |
| `ENC`      | 默认密文前缀          |
| `(`        | 默认密文开始符        |
| `密文内容` | Jasypt 加密后的字符串 |
| `)`        | 默认密文结束符        |

如果需要自定义密文前缀和后缀，可以配置 `jasypt.encryptor.property.prefix` 和 `jasypt.encryptor.property.suffix`。官方文档说明，如果只是想调整密文前缀和后缀，可以继续使用默认解析器，只覆盖这两个配置项。([GitHub](https://github.com/ulisesbocchio/jasypt-spring-boot?utm_source=chatgpt.com))

自定义格式示例：

```yaml
jasypt:
  encryptor:
    property:
      # 自定义密文前缀
      prefix: "ENC@["
      # 自定义密文后缀
      suffix: "]"
```

使用自定义格式后，业务配置需要同步调整：

```yaml
third-party:
  sms:
    access-key: ENC@[sms-access-key-cipher]
    secret-key: ENC@[sms-secret-key-cipher]
```

一般项目不建议自定义密文格式，保持默认 `ENC(...)` 更容易被团队成员识别，也更方便排查问题。

## 加密与解密使用

本章说明如何将明文配置生成密文、如何在配置文件中编写 `ENC(...)` 密文，以及应用启动时 Jasypt 的解密流程。实际项目中，推荐先在本地使用统一密钥生成密文，再将密文提交到配置文件，将密钥交由部署环境注入。

### 明文配置加密

明文配置加密是指将数据库密码、Redis 密码、第三方接口密钥等明文字符串转换为 Jasypt 密文。Jasypt Maven Plugin 支持加密单个值，也支持对配置文件中的 `DEC(...)` 占位值进行批量加密；官方文档中提供了 `jasypt:encrypt-value`、`jasypt:encrypt`、`jasypt:decrypt-value`、`jasypt:decrypt` 等 Maven 目标。([GitHub](https://github.com/ulisesbocchio/jasypt-spring-boot?utm_source=chatgpt.com))

如果项目需要使用 Maven 命令生成密文，可以先在 `pom.xml` 中加入插件配置。

文件位置：`pom.xml`

```xml
<build>
    <plugins>
        <!-- Jasypt Maven 插件，用于本地生成密文、解密校验和配置文件批量加密 -->
        <plugin>
            <groupId>com.github.ulisesbocchio</groupId>
            <artifactId>jasypt-maven-plugin</artifactId>
            <version>4.0.4</version>
        </plugin>
    </plugins>
</build>
```

`jasypt-maven-plugin` 的 `4.0.4` 版本已发布到 Maven Central，可与前文使用的 `jasypt-spring-boot-starter:4.0.4` 保持一致。([Maven Central](https://central.sonatype.com/artifact/com.github.ulisesbocchio/jasypt-maven-plugin?utm_source=chatgpt.com))

加密单个明文值：

```bash
mvn jasypt:encrypt-value \
  -Djasypt.encryptor.password="ateng-jasypt-secret" \
  -Djasypt.encryptor.algorithm="PBEWITHHMACSHA512ANDAES_256" \
  -Djasypt.plugin.value="root123456"
```

命令说明如下：

| 参数                           | 说明                                       |
| ------------------------------ | ------------------------------------------ |
| `jasypt:encrypt-value`         | 对单个明文值进行加密                       |
| `-Djasypt.encryptor.password`  | 加密密钥，应用启动解密时必须使用同一个密钥 |
| `-Djasypt.encryptor.algorithm` | 加密算法，建议与项目配置保持一致           |
| `-Djasypt.plugin.value`        | 需要加密的明文值                           |

执行后，控制台会输出加密后的密文。将输出结果放入配置文件时，需要使用 `ENC(...)` 包裹。

如果需要对配置文件中的占位明文进行批量加密，可以先将配置写成 `DEC(...)` 格式：

```properties
spring.datasource.password=DEC(root123456)
third-party.sms.secret-key=DEC(sms-secret-value)
```

然后执行：

```bash
mvn jasypt:encrypt \
  -Djasypt.encryptor.password="ateng-jasypt-secret"
```

执行后，插件会将配置文件中的 `DEC(...)` 替换为 `ENC(...)`。默认处理路径是 `src/main/resources/application.properties`，如果项目使用 YAML 文件，可以通过 `jasypt.plugin.path` 指定文件路径。官方文档说明，插件路径可以通过 `-Djasypt.plugin.path` 调整，并支持 YAML 等普通文本文件。([GitHub](https://github.com/ulisesbocchio/jasypt-spring-boot?utm_source=chatgpt.com))

```bash
mvn jasypt:encrypt \
  -Djasypt.plugin.path="file:src/main/resources/application-dev.yml" \
  -Djasypt.encryptor.password="ateng-jasypt-secret"
```

如果不希望插件直接修改配置文件，可以使用测试类生成密文。下面的测试类使用与项目配置一致的算法参数生成密文，并用 Hutool 做参数校验。

文件位置：`src/test/java/io/github/atengk/jasypt/JasyptEncryptTest.java`

```java
package io.github.atengk.jasypt;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Jasypt 密文生成与解密验证测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
class JasyptEncryptTest {

    private static final String PASSWORD = "ateng-jasypt-secret";
    private static final String ALGORITHM = "PBEWITHHMACSHA512ANDAES_256";

    /**
     * 加密明文配置值
     */
    @Test
    void encryptValue() {
        String plainText = "root123456";
        Assertions.assertTrue(StrUtil.isNotBlank(plainText), "明文不能为空");

        String cipherText = stringEncryptor().encrypt(plainText);
        log.info("配置加密完成，密文格式：ENC({})", cipherText);

        Assertions.assertTrue(StrUtil.isNotBlank(cipherText), "密文不能为空");
    }

    /**
     * 解密密文配置值
     */
    @Test
    void decryptValue() {
        String cipherText = "将这里替换为实际密文";
        Assertions.assertTrue(StrUtil.isNotBlank(cipherText), "密文不能为空");

        String plainText = stringEncryptor().decrypt(cipherText);
        log.info("配置解密完成，明文：{}", plainText);

        Assertions.assertTrue(StrUtil.isNotBlank(plainText), "明文不能为空");
    }

    /**
     * 构建与项目配置一致的字符串加密器
     *
     * @return 字符串加密器
     */
    private StringEncryptor stringEncryptor() {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();

        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(PASSWORD);
        config.setAlgorithm(ALGORITHM);
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        config.setStringOutputType("base64");

        encryptor.setConfig(config);
        return encryptor;
    }
}
```

使用测试类时，需要保证测试类中的 `PASSWORD`、`ALGORITHM`、`keyObtentionIterations`、`saltGeneratorClassName`、`ivGeneratorClassName`、`stringOutputType` 与 `application.yml` 中的 Jasypt 配置一致。

### ENC 密文配置写法

密文生成后，需要将密文写入配置文件。Jasypt 默认识别的写法是 `ENC(密文内容)`，其中括号中的内容是实际密文，不包含明文、不包含引号标记、不包含额外空格。

推荐写法如下：

```yaml
spring:
  datasource:
    username: root
    password: ENC(nrmZtkF7T0kjG/VodDvBw93Ct8EgjCA+)

  data:
    redis:
      password: ENC(G6gXjJvF5p8f9K1kL0pQ2g==)

third-party:
  sms:
    access-key: ENC(EhJYbaEIKCcEO5C85JEqGAhfcjFMGnoRFf)
    secret-key: ENC(DbG1GppXOsFa2G69PnmADvQFI3esceEhJYbaEIKCcEO5C85J)
```

不推荐写法如下：

```yaml
spring:
  datasource:
    # 错误：ENC 与括号之间不应有空格
    password: ENC (nrmZtkF7T0kjG)

third-party:
  sms:
    # 错误：只写密文但未使用 ENC(...) 包裹，不会被识别为 Jasypt 密文
    secret-key: DbG1GppXOsFa2G69PnmADvQ

system:
  sign:
    # 错误：明文不应直接提交到配置文件
    secret: ateng-sign-secret
```

在 YAML 中，密文值通常可以不加引号。但如果密文中包含 YAML 特殊字符，建议使用双引号包裹整个 `ENC(...)`：

```yaml
system:
  sign:
    # 密文包含特殊字符时，可以使用双引号包裹完整配置值
    secret: "ENC(nrmZtkF7T0kjG/VodDvBw93Ct8EgjCA+)"
```

在 Properties 文件中，写法如下：

```properties
spring.datasource.password=ENC(nrmZtkF7T0kjG/VodDvBw93Ct8EgjCA+)
spring.data.redis.password=ENC(G6gXjJvF5p8f9K1kL0pQ2g==)
third-party.sms.secret-key=ENC(DbG1GppXOsFa2G69PnmADvQ)
```

配置密文时建议遵循以下规范：

| 规范              | 说明                                   |
| ----------------- | -------------------------------------- |
| 只加密敏感字段    | 普通配置不需要加密，避免增加排查成本   |
| 保留配置 key 原名 | 只替换 value，不修改原有配置 key       |
| 使用统一算法      | 同一项目内尽量使用同一套 Jasypt 参数   |
| 按环境生成密文    | 开发、测试、生产环境使用不同密钥和密文 |
| 密文不要手工拆分  | 避免换行、空格、复制缺失导致解密失败   |
| 密钥不要入库      | 解密密钥不能提交到 Git 仓库            |

同一个明文在使用随机盐值和随机 IV 的情况下，多次加密得到的密文通常不同，这是正常现象。只要使用相同密钥和相同算法参数，都可以正确解密。

### 应用启动解密流程

应用启动时，Jasypt 会参与 Spring Boot 配置属性解析流程。官方文档说明，Jasypt Spring Boot 会注册后置处理器，对 Spring Environment 中的 PropertySource 进行包装或代理，使属性源具备加密感知能力；当属性值符合加密约定时，会通过 StringEncryptor 进行解密。([GitHub](https://github.com/ulisesbocchio/jasypt-spring-boot?utm_source=chatgpt.com))

典型启动解密流程如下：

| 步骤 | 说明                                                         |
| ---- | ------------------------------------------------------------ |
| 1    | Spring Boot 启动并加载配置源                                 |
| 2    | Jasypt 自动配置生效，创建或加载 `StringEncryptor`            |
| 3    | Jasypt 读取 `jasypt.encryptor.password` 等加密参数           |
| 4    | Jasypt 包装 Spring Environment 中的 PropertySource           |
| 5    | Spring 读取配置属性时，Jasypt 判断配置值是否符合 `ENC(...)` 格式 |
| 6    | 如果是密文，则调用加密器进行解密                             |
| 7    | Spring 组件获得解密后的真实配置值                            |
| 8    | 数据源、Redis、第三方客户端等组件使用解密后的值完成初始化    |

以数据库密码为例，配置文件中保存的是密文：

```yaml
spring:
  datasource:
    username: root
    password: ENC(database-password-cipher)
```

应用启动时，数据源组件实际拿到的是解密后的明文密码：

```text
spring.datasource.password -> root123456
```

业务代码中无需手动解密，可以正常通过 `@Value`、`Environment` 或 `@ConfigurationProperties` 读取配置。

示例一：使用 `@Value` 读取解密后的配置值。

```java
@Value("${third-party.sms.access-key}")
private String accessKey;
```

示例二：使用 `@ConfigurationProperties` 绑定解密后的配置值。

文件位置：`src/main/java/io/github/atengk/jasypt/config/ThirdPartyProperties.java`

```java
package io.github.atengk.jasypt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 第三方平台配置属性
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Component
@ConfigurationProperties(prefix = "third-party.sms")
public class ThirdPartyProperties {

    /**
     * 访问密钥
     */
    private String accessKey;

    /**
     * 安全密钥
     */
    private String secretKey;
}
```

示例三：提供一个本地验证接口，确认配置是否已完成解密。

文件位置：`src/main/java/io/github/atengk/jasypt/controller/ConfigCheckController.java`

```java
package io.github.atengk.jasypt.controller;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.jasypt.config.ThirdPartyProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 配置解密验证接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ConfigCheckController {

    private final Environment environment;
    private final ThirdPartyProperties thirdPartyProperties;

    /**
     * 检查配置是否完成解密
     *
     * @return 配置检查结果
     */
    @GetMapping("/config/check")
    public Map<String, Object> checkConfig() {
        String datasourcePassword = environment.getProperty("spring.datasource.password");
        String accessKey = thirdPartyProperties.getAccessKey();

        boolean datasourcePasswordDecrypted = StrUtil.isNotBlank(datasourcePassword)
                && !StrUtil.startWith(datasourcePassword, "ENC(");
        boolean accessKeyDecrypted = StrUtil.isNotBlank(accessKey)
                && !StrUtil.startWith(accessKey, "ENC(");

        log.info("配置解密检查完成，数据库密码解密状态：{}，第三方密钥解密状态：{}",
                datasourcePasswordDecrypted, accessKeyDecrypted);

        return Map.of(
                "datasourcePasswordDecrypted", datasourcePasswordDecrypted,
                "thirdPartyAccessKeyDecrypted", accessKeyDecrypted
        );
    }
}
```

启动应用时传入密钥：

```bash
export JASYPT_ENCRYPTOR_PASSWORD='ateng-jasypt-secret'

java -jar springboot3-jasypt-demo.jar \
  --spring.profiles.active=dev
```

调用验证接口：

```bash
curl http://127.0.0.1:8080/config/check
```

响应示例：

```json
{
  "datasourcePasswordDecrypted": true,
  "thirdPartyAccessKeyDecrypted": true
}
```

如果解密失败，常见原因通常是密钥不一致、算法不一致、密文格式错误、密文复制不完整、启动时未传入 `jasypt.encryptor.password`，或者 Profile 配置文件未正确加载。



## Spring Boot 3 集成实现

本章给出 Spring Boot 3 项目中集成 Jasypt 的完整基础实现，包括 Maven 依赖、`application.yml` 配置、启动参数传递密钥以及环境变量传递密钥。`jasypt-spring-boot-starter` 加入项目后，在使用 `@SpringBootApplication` 或 `@EnableAutoConfiguration` 的 Spring Boot 应用中，可以对 Spring Environment 中的属性源启用加密属性支持。([GitHub](https://github.com/ulisesbocchio/jasypt-spring-boot?utm_source=chatgpt.com))

### Maven 依赖引入

Spring Boot 3 项目推荐直接引入 `jasypt-spring-boot-starter`。截至当前 Maven Central 可查信息，`com.github.ulisesbocchio:jasypt-spring-boot-starter` 已发布 `4.0.4` 版本，可作为 Spring Boot 3 项目的集成版本参考。([Maven Central](https://central.sonatype.com/artifact/com.github.ulisesbocchio/jasypt-spring-boot-starter/4.0.4?utm_source=chatgpt.com))

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web 基础依赖，用于提供接口验证能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot JDBC 依赖，用于数据源配置验证 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>

    <!-- Spring Boot Redis 依赖，用于 Redis 密码加密配置验证 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- MySQL 驱动，运行时由数据源自动加载 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Jasypt 配置加密依赖，用于支持 ENC(...) 密文自动解密 -->
    <dependency>
        <groupId>com.github.ulisesbocchio</groupId>
        <artifactId>jasypt-spring-boot-starter</artifactId>
        <version>4.0.4</version>
    </dependency>

    <!-- Hutool 工具包，用于字符串校验、脱敏处理等辅助逻辑 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.35</version>
    </dependency>

    <!-- Lombok 工具，用于减少 Java Bean 和日志对象样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 配置元数据生成器，用于增强 IDE 对 application.yml 的配置提示 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

如果项目只需要加密普通业务配置，不需要验证数据库和 Redis 连接，可以移除 `spring-boot-starter-jdbc`、`spring-boot-starter-data-redis` 和 `mysql-connector-j`。如果文档示例需要覆盖数据库密码、Redis 密码和第三方接口密钥，建议保留这些依赖，便于后续完整验证。

### application 配置示例

`application.yml` 用于配置项目公共参数和 Jasypt 加密器参数。Jasypt 官方配置中，`jasypt.encryptor.password` 是必填项，其余配置项有默认值；但实际项目建议显式声明算法、盐值生成器、IV 生成器和输出格式，保证本地生成密文与应用启动解密的参数一致。([GitHub](https://github.com/ulisesbocchio/jasypt-spring-boot?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: springboot3-jasypt-demo

  profiles:
    # 默认启用开发环境，测试和生产环境通过启动参数覆盖
    active: dev

jasypt:
  encryptor:
    # 解密密钥从环境变量读取，避免写入配置文件或代码仓库
    password: ${JASYPT_ENCRYPTOR_PASSWORD:}

    # 加密算法，生成密文和应用解密必须保持一致
    algorithm: PBEWITHHMACSHA512ANDAES_256

    # 密钥派生迭代次数
    key-obtention-iterations: 1000

    # 加密器实例池大小
    pool-size: 1

    # JDK 默认安全提供方
    provider-name: SunJCE

    # 随机盐值生成器
    salt-generator-classname: org.jasypt.salt.RandomSaltGenerator

    # 随机 IV 生成器
    iv-generator-classname: org.jasypt.iv.RandomIvGenerator

    # 密文输出格式
    string-output-type: base64
```

开发环境配置示例：

文件位置：`src/main/resources/application-dev.yml`

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/jasypt_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    # 数据库密码密文
    password: ENC(dev-database-password-cipher)

  data:
    redis:
      host: 127.0.0.1
      port: 6379
      database: 0
      # Redis 密码密文
      password: ENC(dev-redis-password-cipher)

third-party:
  sms:
    # 短信平台 AccessKey 密文
    access-key: ENC(dev-sms-access-key-cipher)
    # 短信平台 SecretKey 密文
    secret-key: ENC(dev-sms-secret-key-cipher)

  oss:
    # 对象存储访问密钥密文
    access-key: ENC(dev-oss-access-key-cipher)
    secret-key: ENC(dev-oss-secret-key-cipher)
    bucket-name: ateng-demo
    endpoint: https://oss-cn-hangzhou.aliyuncs.com
```

生产环境配置示例：

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://mysql.prod:3306/jasypt_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: app_user
    # 生产环境数据库密码密文
    password: ENC(prod-database-password-cipher)

  data:
    redis:
      host: redis.prod
      port: 6379
      database: 0
      # 生产环境 Redis 密码密文
      password: ENC(prod-redis-password-cipher)

third-party:
  sms:
    # 生产环境短信平台密钥密文
    access-key: ENC(prod-sms-access-key-cipher)
    secret-key: ENC(prod-sms-secret-key-cipher)

  oss:
    # 生产环境对象存储密钥密文
    access-key: ENC(prod-oss-access-key-cipher)
    secret-key: ENC(prod-oss-secret-key-cipher)
    bucket-name: ateng-prod
    endpoint: https://oss-cn-hangzhou.aliyuncs.com
```

配置文件中只保存 `ENC(...)` 密文，不保存解密密钥。开发、测试、生产环境应分别使用不同密钥生成不同密文，避免一个环境的密钥泄露后影响其他环境。

### 启动参数传递密钥

启动参数传递密钥适合本地调试、临时验证和测试环境启动。Jasypt 官方说明中，加密密码可以通过系统属性、命令行参数、环境变量等方式传入，只要属性名是 `jasypt.encryptor.password` 即可被识别。([GitHub](https://github.com/ulisesbocchio/jasypt-spring-boot?utm_source=chatgpt.com))

通过 Jar 包启动时传递密钥：

```bash
java -jar springboot3-jasypt-demo.jar \
  --spring.profiles.active=prod \
  --jasypt.encryptor.password="ateng-jasypt-secret"
```

通过 Maven 启动时传递密钥：

```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--spring.profiles.active=dev --jasypt.encryptor.password=ateng-jasypt-secret"
```

通过 JVM 系统参数传递密钥：

```bash
java \
  -Djasypt.encryptor.password="ateng-jasypt-secret" \
  -jar springboot3-jasypt-demo.jar \
  --spring.profiles.active=prod
```

Docker 容器启动时也可以通过命令行参数传递：

```bash
docker run -d \
  --name springboot3-jasypt-demo \
  -p 8080:8080 \
  springboot3-jasypt-demo:1.0.0 \
  --spring.profiles.active=prod \
  --jasypt.encryptor.password="ateng-jasypt-secret"
```

启动参数方式简单直接，但需要注意命令历史、进程参数和运维平台日志中可能出现明文密钥。生产环境更推荐使用环境变量或 Secret 注入。

### 环境变量传递密钥

环境变量传递密钥是更常用的方式。配置文件中通过 `${JASYPT_ENCRYPTOR_PASSWORD:}` 引用环境变量，应用启动时由操作系统、容器平台或 CI/CD 系统注入真实密钥。

Linux 或 macOS 设置环境变量：

```bash
export JASYPT_ENCRYPTOR_PASSWORD="ateng-jasypt-secret"

java -jar springboot3-jasypt-demo.jar \
  --spring.profiles.active=prod
```

Windows PowerShell 设置环境变量：

```powershell
$env:JASYPT_ENCRYPTOR_PASSWORD = "ateng-jasypt-secret"

java -jar springboot3-jasypt-demo.jar --spring.profiles.active=prod
```

Docker 使用环境变量启动：

```bash
docker run -d \
  --name springboot3-jasypt-demo \
  -p 8080:8080 \
  -e JASYPT_ENCRYPTOR_PASSWORD="ateng-jasypt-secret" \
  springboot3-jasypt-demo:1.0.0 \
  --spring.profiles.active=prod
```

Docker Compose 配置示例：

文件位置：`docker-compose.yml`

```yaml
services:
  springboot3-jasypt-demo:
    image: springboot3-jasypt-demo:1.0.0
    container_name: springboot3-jasypt-demo
    ports:
      - "8080:8080"
    environment:
      # Jasypt 解密密钥，生产环境建议改为从 .env 或 Secret 管理平台注入
      JASYPT_ENCRYPTOR_PASSWORD: "ateng-jasypt-secret"
    command:
      - "--spring.profiles.active=prod"
```

Kubernetes Secret 配置示例：

文件位置：`k8s/secret.yaml`

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: springboot3-jasypt-secret
type: Opaque
stringData:
  # Jasypt 解密密钥
  JASYPT_ENCRYPTOR_PASSWORD: "ateng-jasypt-secret"
```

Kubernetes Deployment 配置示例：

文件位置：`k8s/deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: springboot3-jasypt-demo
spec:
  replicas: 1
  selector:
    matchLabels:
      app: springboot3-jasypt-demo
  template:
    metadata:
      labels:
        app: springboot3-jasypt-demo
    spec:
      containers:
        - name: springboot3-jasypt-demo
          image: springboot3-jasypt-demo:1.0.0
          ports:
            - containerPort: 8080
          env:
            # 从 Kubernetes Secret 中读取 Jasypt 解密密钥
            - name: JASYPT_ENCRYPTOR_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: springboot3-jasypt-secret
                  key: JASYPT_ENCRYPTOR_PASSWORD
          args:
            - "--spring.profiles.active=prod"
```

环境变量方式的优势是配置文件不需要包含密钥，部署环境可以按环境隔离管理密钥。生产环境应限制密钥查看权限，并避免在日志中打印完整密钥。

## 常见业务配置加密

本章给出项目中最常见的三类敏感配置加密示例：数据库密码、Redis 密码和第三方接口密钥。示例中的密文仅用于说明写法，实际使用时需要使用当前项目的 Jasypt 密钥和算法重新生成。

### 数据库密码加密

数据库密码是最常见的加密配置项。Spring Boot 数据源初始化时读取的是 `spring.datasource.password`，如果该值使用 `ENC(...)` 包裹，Jasypt 会在数据源组件获取属性前完成解密。

配置示例：

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://mysql.prod:3306/jasypt_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: app_user
    # 数据库密码密文，启动时由 Jasypt 自动解密
    password: ENC(prod-database-password-cipher)
```

生成数据库密码密文：

```bash
mvn jasypt:encrypt-value \
  -Djasypt.encryptor.password="ateng-jasypt-secret" \
  -Djasypt.encryptor.algorithm="PBEWITHHMACSHA512ANDAES_256" \
  -Djasypt.plugin.value="数据库明文密码"
```

启动应用：

```bash
export JASYPT_ENCRYPTOR_PASSWORD="ateng-jasypt-secret"

java -jar springboot3-jasypt-demo.jar \
  --spring.profiles.active=prod
```

可以使用一个简单接口验证数据源是否可用。

文件位置：`src/main/java/io/github/atengk/jasypt/controller/DatabaseCheckController.java`

```java
package io.github.atengk.jasypt.controller;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 数据库连接验证接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class DatabaseCheckController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 验证数据库连接是否正常
     *
     * @return 数据库连接检查结果
     */
    @GetMapping("/check/database")
    public Map<String, Object> checkDatabase() {
        String result = jdbcTemplate.queryForObject("select 'ok'", String.class);
        boolean success = StrUtil.equals("ok", result);

        log.info("数据库连接检查完成，连接状态：{}", success);

        return Map.of(
                "success", success,
                "message", success ? "数据库连接正常" : "数据库连接异常"
        );
    }
}
```

验证命令：

```bash
curl http://127.0.0.1:8080/check/database
```

响应示例：

```json
{
  "success": true,
  "message": "数据库连接正常"
}
```

如果数据库连接失败，需要优先检查 Jasypt 密钥是否传入、数据库密文是否由同一密钥生成、数据库地址是否正确以及数据库账号是否具备连接权限。

### Redis 密码加密

Redis 密码可以通过 `spring.data.redis.password` 配置。Spring Boot 3 使用 `spring.data.redis.*` 作为 Redis 自动配置属性前缀，配置值可以直接写成 `ENC(...)` 格式。

配置示例：

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  data:
    redis:
      host: redis.prod
      port: 6379
      database: 0
      timeout: 3s
      # Redis 认证密码密文
      password: ENC(prod-redis-password-cipher)
```

生成 Redis 密码密文：

```bash
mvn jasypt:encrypt-value \
  -Djasypt.encryptor.password="ateng-jasypt-secret" \
  -Djasypt.encryptor.algorithm="PBEWITHHMACSHA512ANDAES_256" \
  -Djasypt.plugin.value="Redis明文密码"
```

可以使用 `StringRedisTemplate` 验证 Redis 是否能正常写入和读取。

文件位置：`src/main/java/io/github/atengk/jasypt/controller/RedisCheckController.java`

```java
package io.github.atengk.jasypt.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

/**
 * Redis 连接验证接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class RedisCheckController {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 验证 Redis 连接是否正常
     *
     * @return Redis 连接检查结果
     */
    @GetMapping("/check/redis")
    public Map<String, Object> checkRedis() {
        String key = "jasypt:check:redis";
        String value = DateUtil.now();

        stringRedisTemplate.opsForValue().set(key, value, Duration.ofMinutes(5));
        String redisValue = stringRedisTemplate.opsForValue().get(key);

        boolean success = StrUtil.equals(value, redisValue);
        log.info("Redis 连接检查完成，写入状态：{}", success);

        return Map.of(
                "success", success,
                "key", key,
                "message", success ? "Redis 连接正常" : "Redis 连接异常"
        );
    }
}
```

验证命令：

```bash
curl http://127.0.0.1:8080/check/redis
```

响应示例：

```json
{
  "success": true,
  "key": "jasypt:check:redis",
  "message": "Redis 连接正常"
}
```

如果 Redis 验证失败，需要重点检查 Redis 密码密文、Redis 服务地址、端口、防火墙、数据库编号以及当前 Redis 实例是否启用了认证。

### 第三方接口密钥加密

第三方接口密钥通常包括 `access-key`、`secret-key`、`app-id`、`app-secret`、签名盐值等信息。这类配置一般不会被 Spring Boot 自动消费，需要通过 `@ConfigurationProperties` 绑定到自定义配置类中。

配置示例：

文件位置：`src/main/resources/application-prod.yml`

```yaml
third-party:
  sms:
    # 短信平台访问密钥密文
    access-key: ENC(prod-sms-access-key-cipher)
    # 短信平台安全密钥密文
    secret-key: ENC(prod-sms-secret-key-cipher)
    sign-name: Ateng系统
    template-code: SMS_10000001

  oss:
    # 对象存储访问密钥密文
    access-key: ENC(prod-oss-access-key-cipher)
    secret-key: ENC(prod-oss-secret-key-cipher)
    bucket-name: ateng-prod
    endpoint: https://oss-cn-hangzhou.aliyuncs.com
```

生成第三方接口密钥密文：

```bash
mvn jasypt:encrypt-value \
  -Djasypt.encryptor.password="ateng-jasypt-secret" \
  -Djasypt.encryptor.algorithm="PBEWITHHMACSHA512ANDAES_256" \
  -Djasypt.plugin.value="第三方接口明文密钥"
```

定义短信平台配置类：

文件位置：`src/main/java/io/github/atengk/jasypt/config/SmsProperties.java`

```java
package io.github.atengk.jasypt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 短信平台配置属性
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Component
@ConfigurationProperties(prefix = "third-party.sms")
public class SmsProperties {

    /**
     * 访问密钥
     */
    private String accessKey;

    /**
     * 安全密钥
     */
    private String secretKey;

    /**
     * 短信签名
     */
    private String signName;

    /**
     * 模板编码
     */
    private String templateCode;
}
```

定义对象存储配置类：

文件位置：`src/main/java/io/github/atengk/jasypt/config/OssProperties.java`

```java
package io.github.atengk.jasypt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 对象存储配置属性
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Component
@ConfigurationProperties(prefix = "third-party.oss")
public class OssProperties {

    /**
     * 访问密钥
     */
    private String accessKey;

    /**
     * 安全密钥
     */
    private String secretKey;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 访问端点
     */
    private String endpoint;
}
```

提供第三方配置解密验证接口。接口只返回脱敏后的配置，不返回完整明文密钥，避免通过验证接口造成二次泄露。

文件位置：`src/main/java/io/github/atengk/jasypt/controller/ThirdPartyConfigCheckController.java`

```java
package io.github.atengk.jasypt.controller;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.jasypt.config.OssProperties;
import io.github.atengk.jasypt.config.SmsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 第三方配置解密验证接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ThirdPartyConfigCheckController {

    private final SmsProperties smsProperties;
    private final OssProperties ossProperties;

    /**
     * 检查第三方配置是否完成解密
     *
     * @return 第三方配置检查结果
     */
    @GetMapping("/check/third-party")
    public Map<String, Object> checkThirdPartyConfig() {
        boolean smsSecretDecrypted = isDecrypted(smsProperties.getSecretKey());
        boolean ossSecretDecrypted = isDecrypted(ossProperties.getSecretKey());

        log.info("第三方配置解密检查完成，短信密钥状态：{}，对象存储密钥状态：{}",
                smsSecretDecrypted, ossSecretDecrypted);

        return Map.of(
                "sms", Map.of(
                        "accessKey", maskSecret(smsProperties.getAccessKey()),
                        "secretKeyDecrypted", smsSecretDecrypted,
                        "signName", smsProperties.getSignName(),
                        "templateCode", smsProperties.getTemplateCode()
                ),
                "oss", Map.of(
                        "accessKey", maskSecret(ossProperties.getAccessKey()),
                        "secretKeyDecrypted", ossSecretDecrypted,
                        "bucketName", ossProperties.getBucketName(),
                        "endpoint", ossProperties.getEndpoint()
                )
        );
    }

    /**
     * 判断配置值是否已完成解密
     *
     * @param value 配置值
     * @return 是否已解密
     */
    private boolean isDecrypted(String value) {
        return StrUtil.isNotBlank(value) && !StrUtil.startWith(value, "ENC(");
    }

    /**
     * 对密钥进行脱敏
     *
     * @param value 原始密钥
     * @return 脱敏后的密钥
     */
    private String maskSecret(String value) {
        if (StrUtil.isBlank(value)) {
            return "";
        }
        return DesensitizedUtil.idCardNum(value, 4, 4);
    }
}
```

验证命令：

```bash
curl http://127.0.0.1:8080/check/third-party
```

响应示例：

```json
{
  "sms": {
    "accessKey": "prod************pher",
    "secretKeyDecrypted": true,
    "signName": "Ateng系统",
    "templateCode": "SMS_10000001"
  },
  "oss": {
    "accessKey": "prod************pher",
    "secretKeyDecrypted": true,
    "bucketName": "ateng-prod",
    "endpoint": "https://oss-cn-hangzhou.aliyuncs.com"
  }
}
```

第三方接口密钥解密成功后，后续短信客户端、对象存储客户端或支付客户端可以直接注入对应的 `Properties` 配置类使用。需要避免在日志、接口响应和异常堆栈中输出完整 `access-key`、`secret-key`、`token` 等敏感信息。



## 本地开发与部署

本章说明 Jasypt 在本地开发、测试环境和生产环境中的配置方式。不同环境的核心差异在于：密文配置可以放在配置文件中，但解密密钥应由运行环境注入，不能和密文一起提交到代码仓库。

### 本地运行方式

本地开发环境主要用于验证 Jasypt 是否能正常加密、解密配置。开发人员可以在本机设置 `JASYPT_ENCRYPTOR_PASSWORD` 环境变量，然后通过 Maven 或 Jar 包方式启动应用。

本地配置文件示例：

文件位置：`src/main/resources/application-dev.yml`

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/jasypt_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    # 本地数据库密码密文
    password: ENC(dev-database-password-cipher)

  data:
    redis:
      host: 127.0.0.1
      port: 6379
      database: 0
      # 本地 Redis 密码密文
      password: ENC(dev-redis-password-cipher)

third-party:
  sms:
    # 本地短信平台密钥密文
    access-key: ENC(dev-sms-access-key-cipher)
    secret-key: ENC(dev-sms-secret-key-cipher)
```

Linux 或 macOS 本地运行：

```bash
export JASYPT_ENCRYPTOR_PASSWORD="ateng-jasypt-secret"

mvn spring-boot:run \
  -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

Windows PowerShell 本地运行：

```powershell
$env:JASYPT_ENCRYPTOR_PASSWORD = "ateng-jasypt-secret"

mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=dev"
```

如果项目已经打包为 Jar，可以使用以下方式运行：

```bash
export JASYPT_ENCRYPTOR_PASSWORD="ateng-jasypt-secret"

java -jar target/springboot3-jasypt-demo.jar \
  --spring.profiles.active=dev
```

本地运行时建议使用开发环境专用密钥，不要使用测试环境或生产环境密钥。开发环境密文也应使用开发环境密钥单独生成，避免不同环境之间共用同一套加密材料。

### 测试环境配置方式

测试环境通常由 Jenkins、GitLab CI、Docker、Docker Compose 或 Kubernetes 统一部署。测试环境配置的目标是验证应用在接近生产的部署方式下，是否能正确读取密钥并解密配置。

测试环境配置文件示例：

文件位置：`src/main/resources/application-test.yml`

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://mysql.test:3306/jasypt_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: test_user
    # 测试环境数据库密码密文
    password: ENC(test-database-password-cipher)

  data:
    redis:
      host: redis.test
      port: 6379
      database: 0
      timeout: 3s
      # 测试环境 Redis 密码密文
      password: ENC(test-redis-password-cipher)

third-party:
  sms:
    # 测试环境第三方平台密钥密文
    access-key: ENC(test-sms-access-key-cipher)
    secret-key: ENC(test-sms-secret-key-cipher)
```

测试环境通过环境变量启动：

```bash
export JASYPT_ENCRYPTOR_PASSWORD="test-jasypt-secret"

java -jar springboot3-jasypt-demo.jar \
  --spring.profiles.active=test
```

Docker 测试环境启动示例：

```bash
docker run -d \
  --name springboot3-jasypt-demo-test \
  -p 8080:8080 \
  -e JASYPT_ENCRYPTOR_PASSWORD="test-jasypt-secret" \
  springboot3-jasypt-demo:1.0.0 \
  --spring.profiles.active=test
```

Docker Compose 测试环境配置示例：

文件位置：`docker-compose-test.yml`

```yaml
services:
  springboot3-jasypt-demo:
    image: springboot3-jasypt-demo:1.0.0
    container_name: springboot3-jasypt-demo-test
    ports:
      - "8080:8080"
    environment:
      # 测试环境 Jasypt 解密密钥
      JASYPT_ENCRYPTOR_PASSWORD: "test-jasypt-secret"
    command:
      - "--spring.profiles.active=test"
```

启动测试环境：

```bash
docker compose -f docker-compose-test.yml up -d
```

查看测试环境日志：

```bash
docker logs -f springboot3-jasypt-demo-test
```

测试环境应重点验证以下内容：

| 验证项                 | 说明                                    |
| ---------------------- | --------------------------------------- |
| Profile 是否正确       | 确认启用的是 `test` 环境                |
| 密钥是否注入           | 确认 `JASYPT_ENCRYPTOR_PASSWORD` 已配置 |
| 数据库是否连接成功     | 验证数据源密码是否成功解密              |
| Redis 是否连接成功     | 验证 Redis 密码是否成功解密             |
| 第三方配置是否绑定成功 | 验证自定义配置类能读取解密后的密钥      |
| 日志是否泄露敏感信息   | 日志中不能打印完整密码、密钥、Token     |

### 生产环境配置方式

生产环境配置的重点是安全性和可维护性。生产环境应使用独立的生产密钥和生产密文，密钥由部署平台、运维系统或 Secret 管理系统注入，不能写入代码仓库、镜像文件或普通配置文件。

生产环境配置文件示例：

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://mysql.prod:3306/jasypt_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: app_user
    # 生产环境数据库密码密文
    password: ENC(prod-database-password-cipher)

  data:
    redis:
      host: redis.prod
      port: 6379
      database: 0
      timeout: 3s
      # 生产环境 Redis 密码密文
      password: ENC(prod-redis-password-cipher)

third-party:
  sms:
    # 生产环境第三方平台密钥密文
    access-key: ENC(prod-sms-access-key-cipher)
    secret-key: ENC(prod-sms-secret-key-cipher)
```

生产环境 Jar 包启动示例：

```bash
export JASYPT_ENCRYPTOR_PASSWORD="prod-jasypt-secret"

java -jar springboot3-jasypt-demo.jar \
  --spring.profiles.active=prod
```

生产环境 systemd 配置示例：

文件位置：`/etc/systemd/system/springboot3-jasypt-demo.service`

```ini
[Unit]
Description=Spring Boot 3 Jasypt Demo
After=network.target

[Service]
Type=simple
User=app
WorkingDirectory=/opt/springboot3-jasypt-demo

# 生产环境 Jasypt 解密密钥，建议改为 EnvironmentFile 管理
Environment="JASYPT_ENCRYPTOR_PASSWORD=prod-jasypt-secret"

ExecStart=/usr/bin/java -jar /opt/springboot3-jasypt-demo/springboot3-jasypt-demo.jar --spring.profiles.active=prod
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

生产环境使用独立环境变量文件更便于权限控制：

文件位置：`/etc/springboot3-jasypt-demo/app.env`

```properties
# Jasypt 生产环境解密密钥
JASYPT_ENCRYPTOR_PASSWORD=prod-jasypt-secret
```

对应的 systemd 配置可以调整为：

```ini
[Service]
Type=simple
User=app
WorkingDirectory=/opt/springboot3-jasypt-demo
EnvironmentFile=/etc/springboot3-jasypt-demo/app.env
ExecStart=/usr/bin/java -jar /opt/springboot3-jasypt-demo/springboot3-jasypt-demo.jar --spring.profiles.active=prod
Restart=always
RestartSec=10
```

修改权限，避免普通用户读取密钥文件：

```bash
sudo chown root:app /etc/springboot3-jasypt-demo/app.env
sudo chmod 640 /etc/springboot3-jasypt-demo/app.env
```

重新加载并启动服务：

```bash
sudo systemctl daemon-reload
sudo systemctl enable springboot3-jasypt-demo
sudo systemctl start springboot3-jasypt-demo
sudo systemctl status springboot3-jasypt-demo
```

生产环境建议遵循以下规则：

| 规则             | 说明                                       |
| ---------------- | ------------------------------------------ |
| 生产密钥独立     | 不与开发、测试环境共用密钥                 |
| 密钥不进仓库     | 不写入 Git、镜像、配置文件模板             |
| 密文可进配置     | `ENC(...)` 可以放入环境配置文件            |
| 日志不打印密钥   | 禁止输出完整密码、Token、SecretKey         |
| 定期轮换密钥     | 轮换时需要重新生成所有相关密文             |
| 控制读取权限     | 只有部署账号或运维系统可以读取密钥         |
| 保留验证接口权限 | 验证接口不能对公网开放，且不能返回完整明文 |

## 验证与测试

本章说明 Jasypt 配置加密完成后的验证方式，包括配置解密验证、启动日志检查和常见异常排查。验证的核心目标是确认应用实际读取到的是解密后的明文，而配置文件中保存的是不可直接使用的密文。

### 配置解密验证

配置解密验证可以通过单元测试、接口验证和组件连接验证三种方式完成。建议本地开发优先使用单元测试生成密文和验证解密，测试环境和生产环境通过健康检查接口或组件连接状态验证。

可以先定义一个配置绑定类，用于读取第三方配置。

文件位置：`src/main/java/io/github/atengk/jasypt/config/SmsProperties.java`

```java
package io.github.atengk.jasypt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 短信平台配置属性
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Component
@ConfigurationProperties(prefix = "third-party.sms")
public class SmsProperties {

    /**
     * 访问密钥
     */
    private String accessKey;

    /**
     * 安全密钥
     */
    private String secretKey;
}
```

下面的验证接口用于检查配置是否已经被解密。接口只返回状态和脱敏信息，不返回完整明文密钥。

文件位置：`src/main/java/io/github/atengk/jasypt/controller/JasyptCheckController.java`

```java
package io.github.atengk.jasypt.controller;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.jasypt.config.SmsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Jasypt 配置解密验证接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class JasyptCheckController {

    private final Environment environment;
    private final SmsProperties smsProperties;

    /**
     * 检查配置是否完成解密
     *
     * @return 解密检查结果
     */
    @GetMapping("/check/jasypt")
    public Map<String, Object> checkJasypt() {
        String datasourcePassword = environment.getProperty("spring.datasource.password");
        String redisPassword = environment.getProperty("spring.data.redis.password");
        String smsSecretKey = smsProperties.getSecretKey();

        boolean datasourceDecrypted = isDecrypted(datasourcePassword);
        boolean redisDecrypted = isDecrypted(redisPassword);
        boolean smsSecretDecrypted = isDecrypted(smsSecretKey);

        log.info("Jasypt 配置解密检查完成，数据库：{}，Redis：{}，短信密钥：{}",
                datasourceDecrypted, redisDecrypted, smsSecretDecrypted);

        return Map.of(
                "datasourcePasswordDecrypted", datasourceDecrypted,
                "redisPasswordDecrypted", redisDecrypted,
                "smsSecretKeyDecrypted", smsSecretDecrypted,
                "smsAccessKey", maskSecret(smsProperties.getAccessKey())
        );
    }

    /**
     * 判断配置值是否已解密
     *
     * @param value 配置值
     * @return 是否已解密
     */
    private boolean isDecrypted(String value) {
        return StrUtil.isNotBlank(value) && !StrUtil.startWith(value, "ENC(");
    }

    /**
     * 对敏感配置进行脱敏
     *
     * @param value 原始值
     * @return 脱敏值
     */
    private String maskSecret(String value) {
        if (StrUtil.isBlank(value)) {
            return "";
        }
        return DesensitizedUtil.idCardNum(value, 4, 4);
    }
}
```

启动应用后调用验证接口：

```bash
curl http://127.0.0.1:8080/check/jasypt
```

响应示例：

```json
{
  "datasourcePasswordDecrypted": true,
  "redisPasswordDecrypted": true,
  "smsSecretKeyDecrypted": true,
  "smsAccessKey": "prod************pher"
}
```

也可以通过 Spring Boot 测试验证配置是否已解密。

文件位置：`src/test/java/io/github/atengk/jasypt/JasyptConfigDecryptTest.java`

```java
package io.github.atengk.jasypt;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

/**
 * Jasypt 配置解密测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@SpringBootTest
class JasyptConfigDecryptTest {

    @Autowired
    private Environment environment;

    /**
     * 验证数据库密码是否完成解密
     */
    @Test
    void checkDatasourcePasswordDecrypted() {
        String password = environment.getProperty("spring.datasource.password");

        Assertions.assertTrue(StrUtil.isNotBlank(password), "数据库密码不能为空");
        Assertions.assertFalse(StrUtil.startWith(password, "ENC("), "数据库密码未完成解密");

        log.info("数据库密码解密验证通过");
    }

    /**
     * 验证 Redis 密码是否完成解密
     */
    @Test
    void checkRedisPasswordDecrypted() {
        String password = environment.getProperty("spring.data.redis.password");

        Assertions.assertTrue(StrUtil.isNotBlank(password), "Redis 密码不能为空");
        Assertions.assertFalse(StrUtil.startWith(password, "ENC("), "Redis 密码未完成解密");

        log.info("Redis 密码解密验证通过");
    }
}
```

运行测试前需要传入密钥：

```bash
export JASYPT_ENCRYPTOR_PASSWORD="ateng-jasypt-secret"

mvn test \
  -Dspring.profiles.active=dev \
  -Dtest=JasyptConfigDecryptTest
```

测试通过说明配置已经能被 Spring Environment 正常读取并完成解密。如果测试失败，应优先检查密钥、算法、Profile 和密文格式。

### 启动日志检查

启动日志检查用于快速判断 Jasypt 是否已生效，以及应用是否因为配置解密失败导致启动异常。日志检查不要求打印明文密钥，只需要确认 Profile、配置加载、数据源初始化和 Redis 初始化等关键步骤是否正常。

启动时建议关注以下日志：

| 检查项      | 正常表现                                              | 异常表现                                |
| ----------- | ----------------------------------------------------- | --------------------------------------- |
| Profile     | 日志中出现 `The following 1 profile is active: "dev"` | 启用了错误环境或未启用目标 Profile      |
| Jasypt 密钥 | 应用正常启动，无缺失密钥异常                          | 提示 `jasypt.encryptor.password` 未配置 |
| 数据源      | Hikari 或数据源初始化成功                             | 数据库认证失败、连接池初始化失败        |
| Redis       | Redis 连接验证成功                                    | Redis 认证失败或连接超时                |
| 自定义配置  | `@ConfigurationProperties` 正常绑定                   | 配置为空或仍为 `ENC(...)`               |
| 敏感日志    | 不出现完整密码、SecretKey                             | 日志中输出完整敏感值                    |

查看本地控制台日志：

```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

查看 Jar 包运行日志：

```bash
java -jar springboot3-jasypt-demo.jar \
  --spring.profiles.active=prod
```

查看 systemd 服务日志：

```bash
sudo journalctl -u springboot3-jasypt-demo -f
```

查看 Docker 容器日志：

```bash
docker logs -f springboot3-jasypt-demo
```

日志中如果出现类似数据库认证失败的信息，不一定代表 Jasypt 没有生效，也可能是密文解密成功但明文密码本身不正确。排查时应同时确认密文生成使用的明文、当前环境密钥、数据库账号密码和 Profile 是否一致。

建议在应用启动后输出一条脱敏后的配置检查日志，例如：

```text
Jasypt 配置解密检查完成，数据库：true，Redis：true，短信密钥：true
```

不要输出以下日志：

```text
数据库密码：root123456
短信 SecretKey：abcdefg123456
Jasypt 密钥：prod-jasypt-secret
```

生产环境日志必须避免出现完整敏感信息。

### 常见异常排查

Jasypt 配置加密相关问题通常集中在密钥、算法、密文格式、Profile、配置路径和依赖版本。排查时建议先确认应用是否读取到了正确环境，再确认密钥是否传入，最后确认密文是否由同一套参数生成。

常见异常及处理方式如下：

| 异常现象                     | 可能原因                                | 处理方式                                       |
| ---------------------------- | --------------------------------------- | ---------------------------------------------- |
| 应用启动失败，提示解密失败   | 密钥不正确                              | 使用生成密文时的同一个密钥启动应用             |
| 应用启动失败，提示算法不支持 | 算法配置错误或 JDK 环境不支持           | 检查 `jasypt.encryptor.algorithm` 是否拼写正确 |
| 数据库认证失败               | 数据库密码密文解密后不是正确密码        | 重新用正确明文生成数据库密码密文               |
| Redis 认证失败               | Redis 密码密文错误或 Redis 未启用该密码 | 检查 Redis 实例认证配置                        |
| 配置读取到 `ENC(...)` 原文   | Jasypt 未生效或格式不匹配               | 检查依赖是否引入、密文格式是否正确             |
| 测试环境正常，生产环境失败   | 生产密钥或 Profile 不一致               | 检查生产环境变量和 `spring.profiles.active`    |
| 本地能启动，Docker 不能启动  | 容器环境变量未传入                      | 检查 `docker run -e` 或 Compose `environment`  |
| Maven 插件生成的密文无法解密 | 插件参数与应用参数不一致                | 保证算法、密钥、IV、盐值、输出格式一致         |
| YAML 解析异常                | 密文包含特殊字符未加引号                | 使用双引号包裹完整 `ENC(...)`                  |
| 日志泄露敏感信息             | 验证接口或日志打印明文                  | 改为返回布尔状态和脱敏值                       |

密钥未传入时，通常会出现解密失败或加密器初始化失败。处理方式是确认环境变量是否存在：

```bash
echo "$JASYPT_ENCRYPTOR_PASSWORD"
```

如果该命令没有输出内容，说明当前 Shell 中没有配置密钥。需要重新设置：

```bash
export JASYPT_ENCRYPTOR_PASSWORD="ateng-jasypt-secret"
```

Docker 环境中检查环境变量：

```bash
docker exec -it springboot3-jasypt-demo printenv JASYPT_ENCRYPTOR_PASSWORD
```

如果容器内没有该变量，需要检查启动命令是否包含 `-e JASYPT_ENCRYPTOR_PASSWORD=xxx`，或者 Docker Compose 中是否正确声明 `environment`。

Profile 错误时，应用可能读取了错误环境的密文。例如使用生产密钥启动，却加载了 `application-dev.yml` 中的开发密文。可以通过启动日志确认当前环境：

```bash
java -jar springboot3-jasypt-demo.jar \
  --spring.profiles.active=prod
```

也可以通过 Actuator 或自定义接口输出当前 Profile。下面的接口只返回环境信息，不返回敏感配置。

文件位置：`src/main/java/io/github/atengk/jasypt/controller/ProfileCheckController.java`

```java
package io.github.atengk.jasypt.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Map;

/**
 * Profile 环境验证接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ProfileCheckController {

    private final Environment environment;

    /**
     * 获取当前激活的 Profile
     *
     * @return 当前环境信息
     */
    @GetMapping("/check/profile")
    public Map<String, Object> checkProfile() {
        String[] activeProfiles = environment.getActiveProfiles();

        log.info("当前激活的 Profile：{}", Arrays.toString(activeProfiles));

        return Map.of(
                "activeProfiles", activeProfiles
        );
    }
}
```

验证命令：

```bash
curl http://127.0.0.1:8080/check/profile
```

密文格式错误时，需要检查配置是否符合默认格式：

```yaml
# 正确写法
password: ENC(cipher-text)

# 密文包含特殊字符时推荐加双引号
password: "ENC(cipher-text)"
```

以下写法不推荐：

```yaml
# 错误：ENC 与括号之间存在空格
password: ENC (cipher-text)

# 错误：没有使用 ENC(...) 包裹
password: cipher-text

# 错误：括号不完整
password: ENC(cipher-text
```

算法参数不一致时，需要重新确认生成密文命令和应用配置是否一致。建议生成密文时显式指定算法：

```bash
mvn jasypt:encrypt-value \
  -Djasypt.encryptor.password="ateng-jasypt-secret" \
  -Djasypt.encryptor.algorithm="PBEWITHHMACSHA512ANDAES_256" \
  -Djasypt.plugin.value="root123456"
```

应用配置中也必须使用同一算法：

```yaml
jasypt:
  encryptor:
    password: ${JASYPT_ENCRYPTOR_PASSWORD:}
    algorithm: PBEWITHHMACSHA512ANDAES_256
    key-obtention-iterations: 1000
    salt-generator-classname: org.jasypt.salt.RandomSaltGenerator
    iv-generator-classname: org.jasypt.iv.RandomIvGenerator
    string-output-type: base64
```

如果仍然无法定位问题，可以按以下顺序排查：

| 顺序 | 排查动作                                            |
| ---- | --------------------------------------------------- |
| 1    | 确认当前启动的 `spring.profiles.active` 是否正确    |
| 2    | 确认运行环境中是否存在 `JASYPT_ENCRYPTOR_PASSWORD`  |
| 3    | 确认密文是否由当前环境密钥生成                      |
| 4    | 确认生成密文和应用解密使用同一套算法参数            |
| 5    | 确认配置值是否完整使用 `ENC(...)` 包裹              |
| 6    | 确认 YAML 中密文是否需要加双引号                    |
| 7    | 确认依赖中已引入 `jasypt-spring-boot-starter`       |
| 8    | 确认没有在其他配置源中覆盖同名配置                  |
| 9    | 使用测试类单独验证密文是否能解密                    |
| 10   | 查看数据库、Redis、第三方服务自身的认证配置是否正确 |

Jasypt 问题排查的关键是区分“密文没有被解密”和“密文已解密但明文不正确”。前者通常表现为读取到 `ENC(...)` 原文或解密异常，后者通常表现为数据库、Redis 或第三方服务认证失败。