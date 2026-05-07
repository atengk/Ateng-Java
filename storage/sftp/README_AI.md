# Spring Integration SFTP 开发

本文档用于说明在 Spring Boot 3 项目中基于 Spring Integration SFTP 实现 SFTP 文件传输能力的开发方式。本文先覆盖模块定位、适用场景、技术选型、版本要求、Maven 依赖以及 SFTP 服务准备，后续章节可继续展开连接工厂、上传下载、入站出站流程、异常处理和测试方案。

## 模块概述

本模块面向后端系统中的文件交换场景，提供基于 SFTP 协议的文件上传、下载、查询、删除、重命名和定时拉取能力。Spring Integration 官方文档明确提供 SFTP 文件传输支持，并覆盖 inbound channel adapter、outbound channel adapter、outbound gateway 三类客户端端点，适合将 SFTP 操作集成到 Spring 消息流或业务服务中。([Home](https://docs.spring.io/spring-integration/reference/sftp.html?utm_source=chatgpt.com))

### 功能定位

Spring Integration SFTP 模块的核心定位是为业务系统提供统一、可配置、可扩展的远程文件传输能力。它不只是简单封装 SFTP 客户端连接，而是将远程文件操作纳入 Spring Bean、配置属性、消息通道、定时任务、文件过滤器和异常处理体系中。

在本项目中，SFTP 模块主要承担以下职责：

| 功能         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 文件上传     | 将本地文件、接口上传文件或业务生成文件上传到远程 SFTP 目录   |
| 文件下载     | 从远程 SFTP 目录下载指定文件到本地目录或返回给接口调用方     |
| 文件查询     | 查询远程目录文件列表，判断文件是否存在                       |
| 文件删除     | 删除远程目录中的指定文件                                     |
| 文件重命名   | 对远程文件进行重命名，常用于上传完成后从临时文件名切换为正式文件名 |
| 定时拉取     | 通过 Spring Integration 入站适配器定时从远程目录同步文件     |
| 传输流程编排 | 结合 IntegrationFlow、MessageChannel、Filter、Poller 实现文件传输流程控制 |

该模块建议对外暴露业务服务层方法或 REST API，对内使用 `DefaultSftpSessionFactory`、`SftpRemoteFileTemplate` 和 Spring Integration Flow 完成实际传输。`SftpRemoteFileTemplate` 继承自 `RemoteFileTemplate`，可用于发送、获取、删除、重命名、列出远程文件，并负责在操作结束后可靠关闭会话。([Home](https://docs.spring.io/spring-integration/reference/7.0/sftp/rft.html?utm_source=chatgpt.com))

### 使用场景

SFTP 通常用于系统之间安全、可靠、批量的文件交换。相比 HTTP 接口，SFTP 更适合文件体积较大、传输频率固定、对目录结构和文件落盘状态有明确要求的场景。

常见使用场景如下：

| 场景               | 示例                                                |
| ------------------ | --------------------------------------------------- |
| 银企直连文件交换   | 付款文件上传、回单文件下载、对账文件拉取            |
| 第三方系统批量对接 | 每日订单文件、库存文件、结算文件同步                |
| 数据平台文件传输   | CSV、Excel、JSON、压缩包等离线数据文件交换          |
| 报表文件分发       | 将系统生成的日报、月报上传到客户或内部 SFTP 服务器  |
| 异步文件处理       | 远程目录作为文件队列，本系统定时拉取并处理          |
| 跨网络安全传输     | 在无法开放业务 API 的场景下，通过 SFTP 进行文件中转 |

设计时应优先明确以下业务边界：

| 设计项   | 建议                                                         |
| -------- | ------------------------------------------------------------ |
| 上传方式 | 小文件可直接上传；大文件建议使用临时文件名上传，完成后重命名 |
| 下载方式 | 接口下载适合单文件；批量文件建议先同步到本地目录再处理       |
| 文件幂等 | 使用文件名、业务流水号、远程路径或本地记录表避免重复处理     |
| 文件过滤 | 入站拉取应配置文件名过滤、已处理文件过滤和临时文件过滤       |
| 异常恢复 | 网络中断、远程目录不存在、权限不足、文件不存在应区分处理     |
| 安全配置 | 生产环境建议使用密钥认证，并启用 known hosts 校验            |

### 技术选型

本模块基于 Spring Boot 3、Spring Integration SFTP 和 Apache MINA SSHD 实现。Spring Integration 6.0 起已将旧的 JCraft JSch 客户端替换为 Apache MINA SSHD，`DefaultSftpSessionFactory` 也基于 `org.apache.sshd.client.SshClient` 实现，因此 Spring Boot 3 项目应优先采用 Spring Integration 6.x 版本体系。([Home](https://docs.spring.io/spring-integration/reference/sftp.html?utm_source=chatgpt.com))

推荐技术组合如下：

| 技术                    | 版本建议                            | 用途                                 |
| ----------------------- | ----------------------------------- | ------------------------------------ |
| JDK                     | 17+                                 | Spring Boot 3 最低要求               |
| Spring Boot             | 3.x                                 | 应用基础框架                         |
| Spring Integration SFTP | 6.x                                 | SFTP 文件传输能力                    |
| Apache MINA SSHD        | 由 Spring Integration SFTP 间接引入 | 底层 SSH/SFTP 客户端                 |
| Lombok                  | 可选                                | 简化配置类、DTO、日志对象            |
| Hutool                  | 可选，推荐                          | 文件名、路径、字符串、日期等工具处理 |
| Docker / OpenSSH SFTP   | 开发和测试环境                      | 快速准备本地 SFTP 服务               |

技术选型建议如下：

1. 普通上传、下载、删除、重命名操作，优先使用 `SftpRemoteFileTemplate`。
2. 定时拉取远程文件，优先使用 Spring Integration SFTP 入站适配器。
3. 业务系统主动上传本地文件到远程目录，优先使用 Spring Integration SFTP 出站适配器或 `SftpRemoteFileTemplate`。
4. 对远程目录执行查询、移动、删除等命令式操作时，可使用 outbound gateway 或 `SftpRemoteFileTemplate`。
5. 不建议在 Spring Boot 3 项目中直接使用 JSch 作为主要实现，避免与 Spring Integration 6.x 的底层实现方向不一致。

## 环境与依赖

本节定义项目运行所需的 JDK、Spring Boot、Maven 依赖和本地 SFTP 服务。后续配置设计和核心实现章节均基于本节的依赖与服务参数展开。

### JDK 与 Spring Boot 版本

Spring Boot 3 要求至少 Java 17。以 Spring Boot 3.3.x 官方系统要求为例，Spring Boot 3.3.16 要求 Java 17 及以上，并明确支持 Maven 3.6.3 或更高版本。([Spring Enterprise 文档](https://docs.enterprise.spring.io/spring-boot/system-requirements.html?utm_source=chatgpt.com))

推荐版本如下：

| 环境项             | 推荐值                  | 说明                                                         |
| ------------------ | ----------------------- | ------------------------------------------------------------ |
| JDK                | 17 或 21                | JDK 17 满足 Spring Boot 3 最低要求；JDK 21 适合新项目长期维护 |
| Spring Boot        | 3.3.x / 3.4.x           | 生产项目建议选择仍在维护的 3.x 稳定版本                      |
| Spring Integration | 6.x                     | 与 Spring Boot 3、Spring Framework 6 体系匹配                |
| Maven              | 3.6.3+                  | 满足 Spring Boot 3 构建要求                                  |
| 编码               | UTF-8                   | 避免中文文件名、日志、配置读取乱码                           |
| 操作系统           | Linux / macOS / Windows | 生产环境建议 Linux                                           |

版本选择建议：

| 项目情况                            | 建议                                                         |
| ----------------------------------- | ------------------------------------------------------------ |
| 新建 Spring Boot 3 项目             | 使用 Spring Boot 3.4.x 或企业内部统一版本                    |
| 已有 Spring Boot 3.0/3.1 项目       | 不强制升级，但应保持 Spring Integration 6.x                  |
| 需要长期维护                        | 优先选择公司统一基线版本，避免随意指定 Spring Integration 子版本 |
| 需要使用最新 Spring Integration 7.x | 不建议用于 Spring Boot 3；Spring Integration 7.x 更适合 Spring Boot 4 / Spring Framework 7 体系 |

Spring Boot 会通过 BOM 管理大量依赖版本。例如 Spring Boot 3.4.12 管理的 `spring-integration-sftp` 版本为 `6.4.10`，因此在 Spring Boot 3 项目中通常不需要手动写 Spring Integration SFTP 的版本号。([Home](https://docs.spring.io/spring-boot/3.4.12/appendix/dependency-versions/coordinates.html?utm_source=chatgpt.com))

### Maven 依赖配置

项目建议通过 `spring-boot-starter-parent` 或 Spring Boot BOM 统一管理依赖版本。这样可以避免 Spring Boot、Spring Framework、Spring Integration、Apache MINA SSHD 等依赖版本不一致。

文件位置：`pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- 使用 Spring Boot Parent 统一管理 Spring 生态依赖版本 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.12</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>springboot3-sftp-demo</artifactId>
    <version>1.0.0</version>
    <name>springboot3-sftp-demo</name>
    <description>Spring Boot 3 集成 Spring Integration SFTP 示例项目</description>

    <properties>
        <!-- Spring Boot 3 最低要求 Java 17 -->
        <java.version>17</java.version>
        <!-- Hutool 工具类，用于文件名、路径、字符串等辅助处理 -->
        <hutool.version>5.8.35</hutool.version>
    </properties>

    <dependencies>
        <!-- Web 接口支持，用于暴露上传、下载、查询、删除接口 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Integration SFTP 核心依赖，包含 SFTP SessionFactory、RemoteFileTemplate、入站/出站适配器 -->
        <dependency>
            <groupId>org.springframework.integration</groupId>
            <artifactId>spring-integration-sftp</artifactId>
        </dependency>

        <!-- Spring Integration Java DSL 支持，便于使用 IntegrationFlow 编排入站/出站流程 -->
        <dependency>
            <groupId>org.springframework.integration</groupId>
            <artifactId>spring-integration-core</artifactId>
        </dependency>

        <!-- 配置元数据提示，便于在 application.yml 中获得配置提示 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Hutool 工具包，用于文件、路径、字符串、日期等常用处理 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok 简化实体类、配置类和日志对象，仅编译期使用 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot 测试依赖，用于单元测试和集成测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Spring Integration 测试支持，用于消息流、通道、适配器相关测试 -->
        <dependency>
            <groupId>org.springframework.integration</groupId>
            <artifactId>spring-integration-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot 打包插件，用于生成可执行 Jar -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

依赖配置说明：

| 依赖                      | 是否必需 | 说明                                                         |
| ------------------------- | -------- | ------------------------------------------------------------ |
| `spring-integration-sftp` | 必需     | 提供 SFTP 文件传输能力                                       |
| `spring-integration-core` | 建议     | 使用 IntegrationFlow 时需要核心能力；通常会被间接引入，但显式声明更清晰 |
| `spring-boot-starter-web` | 按需     | 需要提供 REST 文件接口时引入                                 |
| `hutool-all`              | 推荐     | 简化文件名、路径、字符串、日期等处理                         |
| `lombok`                  | 可选     | 简化 POJO、日志对象和构造器                                  |
| `spring-integration-test` | 测试推荐 | 用于 Spring Integration 流程测试                             |

注意：不建议在 Spring Boot 3 项目中为 `spring-integration-sftp` 手动指定版本，除非明确知道需要覆盖 Spring Boot BOM 的管理版本。手动指定版本可能导致 Spring Integration、Spring Framework、Apache MINA SSHD 之间出现兼容性问题。

### SFTP 服务准备

开发环境需要准备一个可连接的 SFTP 服务，用于验证上传、下载、查询、删除、重命名和入站拉取流程。可以使用公司测试环境 SFTP，也可以通过 Docker 本地启动临时 SFTP 服务。

开发测试推荐使用 Docker 启动 `atmoz/sftp`。该镜像基于 OpenSSH，支持通过命令参数、`SFTP_USERS` 环境变量或 `/etc/sftp/users.conf` 定义用户；官方示例中也展示了将容器 22 端口映射到宿主机 2222 端口，并通过 `sftp -P 2222 用户名@主机` 登录。([Docker Hub](https://hub.docker.com/r/atmoz/sftp/?utm_source=chatgpt.com))

本地目录准备：

```bash
# 创建本地挂载目录，用于保存 SFTP 服务器中的上传文件
mkdir -p ./docker/sftp/upload

# 创建本地下载目录，用于应用程序保存从 SFTP 拉取的文件
mkdir -p ./data/sftp/download

# 创建本地临时目录，用于上传前生成临时文件或处理中间文件
mkdir -p ./data/sftp/temp
```

命令说明：

| 命令                            | 说明                                       |
| ------------------------------- | ------------------------------------------ |
| `mkdir -p ./docker/sftp/upload` | 创建挂载到 SFTP 容器的远程上传目录         |
| `mkdir -p ./data/sftp/download` | 创建应用本地下载目录                       |
| `mkdir -p ./data/sftp/temp`     | 创建应用本地临时目录                       |
| `-p`                            | 父目录不存在时自动创建，目录已存在时不报错 |

Docker Compose 配置如下。

文件位置：`docker-compose.yml`

```yaml
services:
  sftp:
    # 开发测试使用的 SFTP 服务镜像，生产环境请使用企业统一 SFTP 服务
    image: atmoz/sftp:latest
    container_name: springboot3-sftp
    ports:
      # 宿主机 2222 端口映射到容器 SSH/SFTP 22 端口
      - "2222:22"
    volumes:
      # 将宿主机目录挂载到 SFTP 用户的 upload 目录
      - ./docker/sftp/upload:/home/sftpuser/upload
    command: sftpuser:123456:1001
    restart: unless-stopped
```

启动 SFTP 服务：

```bash
# 启动本地 SFTP 服务
docker compose up -d

# 查看容器运行状态
docker ps | grep springboot3-sftp

# 查看 SFTP 容器日志
docker logs -f springboot3-sftp
```

使用命令行验证连接：

```bash
# 连接本地 SFTP 服务
sftp -P 2222 sftpuser@127.0.0.1

# 登录后执行以下命令
pwd
ls
cd upload
put README.md
ls
get README.md
rm README.md
bye
```

命令说明：

| 命令                              | 说明                         |
| --------------------------------- | ---------------------------- |
| `sftp -P 2222 sftpuser@127.0.0.1` | 使用 SFTP 客户端连接本地服务 |
| `pwd`                             | 查看当前远程目录             |
| `ls`                              | 查看远程目录文件             |
| `cd upload`                       | 切换到远程上传目录           |
| `put README.md`                   | 上传本地文件到远程目录       |
| `get README.md`                   | 下载远程文件到当前本地目录   |
| `rm README.md`                    | 删除远程文件                 |
| `bye`                             | 退出 SFTP 客户端             |

应用配置示例：

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

sftp:
  # SFTP 服务地址，本地 Docker 环境使用 127.0.0.1
  host: 127.0.0.1
  # Docker Compose 中映射的宿主机端口
  port: 2222
  # SFTP 登录用户名
  username: sftpuser
  # SFTP 登录密码，生产环境建议改为密钥认证或从配置中心读取
  password: 123456
  # 远程基础目录，基于 SFTP 用户根目录
  remote-base-dir: /upload
  # 本地下载目录
  local-download-dir: ./data/sftp/download
  # 本地临时目录
  local-temp-dir: ./data/sftp/temp
  # 连接超时时间，单位毫秒
  connect-timeout: 10000
  # 会话超时时间，单位毫秒
  session-timeout: 30000
```

服务准备完成后，应至少验证以下内容：

| 验证项        | 期望结果                                           |
| ------------- | -------------------------------------------------- |
| SFTP 端口连通 | `sftp -P 2222 sftpuser@127.0.0.1` 可以正常登录     |
| 用户权限      | 可以进入 `/upload` 目录                            |
| 上传权限      | `put` 命令可以上传文件                             |
| 下载权限      | `get` 命令可以下载文件                             |
| 删除权限      | `rm` 命令可以删除测试文件                          |
| 挂载目录      | 宿主机 `./docker/sftp/upload` 可以看到上传后的文件 |

生产环境注意事项：

1. 生产环境不要使用示例密码 `123456`。
2. 优先使用 SSH Key 认证，不建议长期使用明文密码认证。
3. 建议启用 known hosts 校验，避免连接到伪造 SFTP 服务。
4. 远程目录应按业务系统隔离，例如 `/upload/order`、`/upload/bill`、`/download/reconciliation`。
5. 上传大文件时建议先上传为 `.tmp` 文件，上传完成后再重命名为正式文件名。
6. 入站拉取时应过滤 `.tmp`、`.part`、`.uploading` 等未完成文件。
7. SFTP 账号权限应最小化，只授予当前业务目录读写权限。



## 配置设计

本节定义 SFTP 模块所需的连接参数、连接工厂和文件路径命名规则。配置设计应尽量把主机、端口、认证方式、远程目录、本地目录、缓存大小、超时时间等参数外置到 `application.yml`，避免在业务代码中硬编码。

Spring Integration SFTP 在配置适配器或模板前需要先配置 SFTP Session Factory；默认情况下每次获取 Session 都会创建新的 SFTP 会话，如果需要复用会话，应使用 `CachingSessionFactory` 包装原始 SessionFactory。Spring Integration 6.x/7.x 的 SFTP Session Factory 底层依赖 Apache MINA SSHD 提供 SFTP 能力。([Home](https://docs.spring.io/spring-integration/reference/7.0/sftp/session-factory.html?utm_source=chatgpt.com))

### SFTP 连接参数配置

SFTP 连接参数用于描述远程服务器地址、认证方式、目录规则、缓存配置和超时时间。开发环境可以使用密码认证并允许未知主机密钥，生产环境建议使用私钥认证并配置 `known_hosts` 文件，避免连接到非预期服务器。

文件位置：`src/main/resources/application.yml`

```yaml
sftp:
  # SFTP 服务地址
  host: 127.0.0.1

  # SFTP 服务端口，默认 22；本地 Docker 测试环境常用 2222
  port: 2222

  # SFTP 登录用户名
  username: sftpuser

  # SFTP 登录密码；生产环境建议通过配置中心或环境变量注入
  password: 123456

  # 私钥路径，支持 file:、classpath: 等 Spring Resource 路径；配置私钥后可不配置 password
  private-key:

  # 私钥密码；私钥无密码时留空
  private-key-passphrase:

  # known_hosts 文件路径；生产环境建议配置，例如 file:/opt/app/sftp/known_hosts
  known-hosts:

  # 是否允许未知主机密钥；开发环境可为 true，生产环境建议为 false
  allow-unknown-keys: true

  # Socket 连接和默认操作超时时间
  timeout: 30s

  # Session 缓存池大小，控制可复用 SFTP 会话数量
  session-cache-size: 10

  # 获取 Session 的等待超时时间
  session-wait-timeout: 10s

  # 从缓存中取出 Session 时是否检测可用性
  test-session: true

  # 远程基础目录，业务传入相对路径时会拼接到该目录下
  remote-base-dir: /upload

  # 本地下载目录
  local-download-dir: ./data/sftp/download

  # 本地临时目录
  local-temp-dir: ./data/sftp/temp

  # 上传临时文件后缀，上传完成后再重命名为正式文件名
  temporary-suffix: .tmp

  # 上传前是否自动创建远程目录
  auto-create-remote-dir: true
```

配置项说明如下：

| 配置项                        | 必填         | 默认值                 | 说明                  |
| ----------------------------- | ------------ | ---------------------- | --------------------- |
| `sftp.host`                   | 是           | 无                     | SFTP 服务器地址       |
| `sftp.port`                   | 否           | `22`                   | SFTP 服务器端口       |
| `sftp.username`               | 是           | 无                     | 登录用户名            |
| `sftp.password`               | 条件必填     | 无                     | 密码认证时使用        |
| `sftp.private-key`            | 条件必填     | 无                     | 私钥认证时使用        |
| `sftp.private-key-passphrase` | 否           | 无                     | 私钥口令              |
| `sftp.known-hosts`            | 生产建议必填 | 无                     | 主机密钥校验文件      |
| `sftp.allow-unknown-keys`     | 否           | `false` 建议           | 是否允许未知主机密钥  |
| `sftp.timeout`                | 否           | `30s`                  | 连接和操作超时        |
| `sftp.session-cache-size`     | 否           | `10`                   | Session 缓存池大小    |
| `sftp.session-wait-timeout`   | 否           | `10s`                  | 获取 Session 等待时间 |
| `sftp.remote-base-dir`        | 是           | `/upload`              | 远程基础目录          |
| `sftp.local-download-dir`     | 是           | `./data/sftp/download` | 本地下载目录          |
| `sftp.temporary-suffix`       | 否           | `.tmp`                 | 临时文件后缀          |
| `sftp.auto-create-remote-dir` | 否           | `true`                 | 是否自动创建远程目录  |

配置属性类用于接收 `application.yml` 中的 SFTP 参数，后续连接工厂和文件服务都依赖该类。

文件位置：`src/main/java/io/github/atengk/sftp/config/SftpProperties.java`

```java
package io.github.atengk.sftp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * SFTP 配置属性
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@ConfigurationProperties(prefix = "sftp")
public class SftpProperties {

    /**
     * SFTP 服务地址
     */
    private String host;

    /**
     * SFTP 服务端口
     */
    private int port = 22;

    /**
     * 登录用户名
     */
    private String username;

    /**
     * 登录密码
     */
    private String password;

    /**
     * 私钥路径，支持 file:、classpath: 等 Spring Resource 路径
     */
    private String privateKey;

    /**
     * 私钥密码
     */
    private String privateKeyPassphrase;

    /**
     * known_hosts 文件路径
     */
    private String knownHosts;

    /**
     * 是否允许未知主机密钥
     */
    private boolean allowUnknownKeys = false;

    /**
     * 连接和操作超时时间
     */
    private Duration timeout = Duration.ofSeconds(30);

    /**
     * Session 缓存池大小
     */
    private int sessionCacheSize = 10;

    /**
     * 获取 Session 等待超时时间
     */
    private Duration sessionWaitTimeout = Duration.ofSeconds(10);

    /**
     * 是否检测缓存 Session 可用性
     */
    private boolean testSession = true;

    /**
     * 远程基础目录
     */
    private String remoteBaseDir = "/upload";

    /**
     * 本地下载目录
     */
    private String localDownloadDir = "./data/sftp/download";

    /**
     * 本地临时目录
     */
    private String localTempDir = "./data/sftp/temp";

    /**
     * 上传临时文件后缀
     */
    private String temporarySuffix = ".tmp";

    /**
     * 是否自动创建远程目录
     */
    private boolean autoCreateRemoteDir = true;
}
```

### 连接工厂配置

连接工厂负责创建和管理 SFTP Session。实际项目中建议分为两层：第一层使用 `DefaultSftpSessionFactory` 创建真实连接，第二层使用 `CachingSessionFactory` 复用 Session，减少频繁连接和认证带来的开销。`CachingSessionFactory` 支持设置缓存大小、等待超时时间和 Session 检测能力；其作用是包装底层 SessionFactory 并复用 Session。([Spring Enterprise 文档](https://docs.enterprise.spring.io/spring-integration/docs/6.3.12/api/org/springframework/integration/file/remote/session/CachingSessionFactory.html?utm_source=chatgpt.com))

本节涉及的文件结构如下：

```text
src/main/java/io/github/atengk/sftp/config/SftpProperties.java
src/main/java/io/github/atengk/sftp/config/SftpConfig.java
src/main/java/io/github/atengk/sftp/core/SftpPathHelper.java
src/main/java/io/github/atengk/sftp/exception/SftpOperationException.java
src/main/java/io/github/atengk/sftp/service/SftpFileService.java
src/main/java/io/github/atengk/sftp/service/impl/SftpFileServiceImpl.java
```

该配置类初始化原始 SFTP SessionFactory、缓存 SessionFactory 和 SftpRemoteFileTemplate。

文件位置：`src/main/java/io/github/atengk/sftp/config/SftpConfig.java`

```java
package io.github.atengk.sftp.config;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;

import java.util.Objects;

/**
 * SFTP 核心配置
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(SftpProperties.class)
public class SftpConfig implements ResourceLoaderAware {

    private ResourceLoader resourceLoader;

    /**
     * 设置 Spring Resource 加载器
     *
     * @param resourceLoader Resource 加载器
     */
    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 初始化原始 SFTP SessionFactory
     *
     * @param properties SFTP 配置属性
     * @return 原始 SessionFactory
     */
    @Bean
    public SessionFactory<SftpClient.DirEntry> sftpSessionFactory(SftpProperties properties) {
        validateProperties(properties);

        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
        factory.setHost(properties.getHost());
        factory.setPort(properties.getPort());
        factory.setUser(properties.getUsername());
        factory.setTimeout(Math.toIntExact(properties.getTimeout().toMillis()));
        factory.setAllowUnknownKeys(properties.isAllowUnknownKeys());

        if (StrUtil.isNotBlank(properties.getPassword())) {
            factory.setPassword(properties.getPassword());
        }

        if (StrUtil.isNotBlank(properties.getPrivateKey())) {
            factory.setPrivateKey(resourceLoader.getResource(properties.getPrivateKey()));
        }

        if (StrUtil.isNotBlank(properties.getPrivateKeyPassphrase())) {
            factory.setPrivateKeyPassphrase(properties.getPrivateKeyPassphrase());
        }

        if (StrUtil.isNotBlank(properties.getKnownHosts())) {
            factory.setKnownHostsResource(resourceLoader.getResource(properties.getKnownHosts()));
        }

        log.info("SFTP连接工厂初始化完成，host：{}，port：{}，username：{}",
                properties.getHost(), properties.getPort(), properties.getUsername());
        return factory;
    }

    /**
     * 初始化带缓存的 SFTP SessionFactory
     *
     * @param sessionFactory 原始 SessionFactory
     * @param properties     SFTP 配置属性
     * @return 缓存 SessionFactory
     */
    @Bean
    public CachingSessionFactory<SftpClient.DirEntry> cachingSftpSessionFactory(
            @Qualifier("sftpSessionFactory") SessionFactory<SftpClient.DirEntry> sessionFactory,
            SftpProperties properties) {

        CachingSessionFactory<SftpClient.DirEntry> cachingSessionFactory =
                new CachingSessionFactory<>(sessionFactory, properties.getSessionCacheSize());
        cachingSessionFactory.setSessionWaitTimeout(properties.getSessionWaitTimeout().toMillis());
        cachingSessionFactory.setTestSession(properties.isTestSession());

        log.info("SFTP缓存连接工厂初始化完成，缓存大小：{}，等待超时：{}ms",
                properties.getSessionCacheSize(), properties.getSessionWaitTimeout().toMillis());
        return cachingSessionFactory;
    }

    /**
     * 初始化 SFTP 远程文件模板
     *
     * @param sessionFactory 缓存 SessionFactory
     * @param properties     SFTP 配置属性
     * @return SFTP 远程文件模板
     */
    @Bean
    public SftpRemoteFileTemplate sftpRemoteFileTemplate(
            @Qualifier("cachingSftpSessionFactory") SessionFactory<SftpClient.DirEntry> sessionFactory,
            SftpProperties properties) {

        SftpRemoteFileTemplate template = new SftpRemoteFileTemplate(sessionFactory);
        template.setAutoCreateDirectory(properties.isAutoCreateRemoteDir());

        log.info("SFTP远程文件模板初始化完成，自动创建远程目录：{}", properties.isAutoCreateRemoteDir());
        return template;
    }

    /**
     * 校验 SFTP 基础配置
     *
     * @param properties SFTP 配置属性
     */
    private void validateProperties(SftpProperties properties) {
        Objects.requireNonNull(properties, "SFTP配置不能为空");

        if (StrUtil.isBlank(properties.getHost())) {
            throw new IllegalArgumentException("SFTP主机地址不能为空");
        }
        if (properties.getPort() <= 0) {
            throw new IllegalArgumentException("SFTP端口必须大于0");
        }
        if (StrUtil.isBlank(properties.getUsername())) {
            throw new IllegalArgumentException("SFTP用户名不能为空");
        }
        if (StrUtil.isBlank(properties.getPassword()) && StrUtil.isBlank(properties.getPrivateKey())) {
            throw new IllegalArgumentException("SFTP密码和私钥不能同时为空");
        }
        if (!properties.isAllowUnknownKeys() && StrUtil.isBlank(properties.getKnownHosts())) {
            throw new IllegalArgumentException("禁止未知主机密钥时必须配置known_hosts文件");
        }
    }
}
```

### 文件路径与命名配置

文件路径与命名规则用于统一远程路径拼接、本地路径创建、文件名安全校验和临时文件命名。SFTP 远程路径建议统一使用 `/`，避免 Windows 本地路径分隔符 `\` 混入远程路径。

建议采用以下路径规则：

| 类型         | 示例                                   | 说明                     |
| ------------ | -------------------------------------- | ------------------------ |
| 远程基础目录 | `/upload`                              | 系统级远程根目录         |
| 业务远程目录 | `/upload/order/20260507`               | 按业务类型和日期分目录   |
| 上传临时文件 | `/upload/order/20260507/order.csv.tmp` | 上传过程中使用           |
| 上传正式文件 | `/upload/order/20260507/order.csv`     | 上传完成后重命名         |
| 本地下载目录 | `./data/sftp/download`                 | 下载文件保存目录         |
| 本地临时目录 | `./data/sftp/temp`                     | 上传前生成或处理中间文件 |

该工具类统一处理远程路径拼接、文件名校验和本地目录创建。

文件位置：`src/main/java/io/github/atengk/sftp/core/SftpPathHelper.java`

```java
package io.github.atengk.sftp.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * SFTP 路径工具
 *
 * @author Ateng
 * @since 2026-05-07
 */
public final class SftpPathHelper {

    private SftpPathHelper() {
    }

    /**
     * 拼接远程路径
     *
     * @param parts 路径片段
     * @return 标准远程路径
     */
    public static String joinRemotePath(String... parts) {
        String path = Arrays.stream(parts)
                .filter(StrUtil::isNotBlank)
                .map(StrUtil::trim)
                .map(part -> part.replace("\\", "/"))
                .map(part -> StrUtil.removePrefix(part, "/"))
                .map(part -> StrUtil.removeSuffix(part, "/"))
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining("/"));

        return StrUtil.SLASH + path;
    }

    /**
     * 解析远程路径，绝对路径直接返回，相对路径拼接基础目录
     *
     * @param remoteBaseDir 远程基础目录
     * @param remotePath    远程路径
     * @return 标准远程路径
     */
    public static String resolveRemotePath(String remoteBaseDir, String remotePath) {
        if (StrUtil.isBlank(remotePath)) {
            throw new IllegalArgumentException("远程路径不能为空");
        }
        String normalized = remotePath.replace("\\", "/");
        if (normalized.startsWith(StrUtil.SLASH)) {
            return normalized;
        }
        return joinRemotePath(remoteBaseDir, normalized);
    }

    /**
     * 获取远程父目录
     *
     * @param remotePath 远程文件路径
     * @return 远程父目录
     */
    public static String getRemoteParentDir(String remotePath) {
        String normalized = remotePath.replace("\\", "/");
        int index = normalized.lastIndexOf(StrUtil.SLASH);
        if (index <= 0) {
            return StrUtil.SLASH;
        }
        return normalized.substring(0, index);
    }

    /**
     * 校验文件名，禁止通过文件名传入路径
     *
     * @param fileName 文件名
     */
    public static void validateFileName(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        if (fileName.contains("/") || fileName.contains("\\")) {
            throw new IllegalArgumentException("文件名不能包含路径分隔符");
        }
        if (StrUtil.equals(fileName, ".") || StrUtil.equals(fileName, "..")) {
            throw new IllegalArgumentException("文件名不合法");
        }
    }

    /**
     * 创建本地文件父目录
     *
     * @param localFile 本地文件路径
     */
    public static void mkdirParent(Path localFile) {
        Path parent = localFile.toAbsolutePath().getParent();
        if (parent != null) {
            FileUtil.mkdir(parent.toFile());
        }
    }
}
```

## 核心实现

本节给出 SFTP 文件上传、下载、删除和重命名的核心实现。这里不直接在业务代码中操作底层 SSH 客户端，而是通过 `SftpRemoteFileTemplate` 执行远程文件操作。Spring Integration 的 `RemoteFileTemplate` 抽象支持发送、获取、删除、重命名和在同一 Session 中执行多个操作，并负责可靠关闭 Session；SFTP 对应实现类为 `SftpRemoteFileTemplate`。([Home](https://docs.spring.io/spring-integration/reference/7.0/sftp/rft.html?utm_source=chatgpt.com))

### SFTP SessionFactory 初始化

`SessionFactory` 是 SFTP 操作的基础组件，负责创建远程会话。本文实现中已经在 `SftpConfig` 中提供两个 Bean：

| Bean 名称                   | 类型                                         | 作用                    |
| --------------------------- | -------------------------------------------- | ----------------------- |
| `sftpSessionFactory`        | `SessionFactory<SftpClient.DirEntry>`        | 创建真实 SFTP Session   |
| `cachingSftpSessionFactory` | `CachingSessionFactory<SftpClient.DirEntry>` | 缓存并复用 SFTP Session |

使用缓存连接工厂后，业务服务和 `SftpRemoteFileTemplate` 不需要关心连接复用逻辑，只需要注入 `SftpRemoteFileTemplate` 即可。

初始化流程如下：

```text
application.yml
  ↓
SftpProperties
  ↓
DefaultSftpSessionFactory
  ↓
CachingSessionFactory
  ↓
SftpRemoteFileTemplate
  ↓
SftpFileServiceImpl
```

关键设计点：

| 设计点                            | 说明                                           |
| --------------------------------- | ---------------------------------------------- |
| `DefaultSftpSessionFactory(true)` | 使用共享连接模式，配合缓存工厂减少物理连接创建 |
| `CachingSessionFactory`           | 控制 Session 缓存数量和等待超时时间            |
| `setTestSession(true)`            | 从缓存中取出 Session 时检测是否可用            |
| `knownHostsResource`              | 生产环境建议启用主机密钥校验                   |
| `allowUnknownKeys`                | 仅建议开发测试环境开启                         |

### SFTP RemoteFileTemplate 配置

`SftpRemoteFileTemplate` 是业务代码执行远程文件操作的主要入口。它适合封装在 Service 层中，由 Controller、定时任务或消息流调用。

模板配置已经在 `SftpConfig` 中完成：

```java
@Bean
public SftpRemoteFileTemplate sftpRemoteFileTemplate(
        @Qualifier("cachingSftpSessionFactory") SessionFactory<SftpClient.DirEntry> sessionFactory,
        SftpProperties properties) {

    SftpRemoteFileTemplate template = new SftpRemoteFileTemplate(sessionFactory);
    template.setAutoCreateDirectory(properties.isAutoCreateRemoteDir());
    return template;
}
```

建议在业务中优先使用 `execute` 方法执行一组相关操作。例如上传时先写入临时文件，再删除旧正式文件，最后执行重命名。这样可以确保这些操作在同一个远程 Session 上完成，减少多次获取 Session 的开销。

### 文件上传实现

文件上传采用“临时文件 + 重命名”的方式。业务文件先上传为 `.tmp` 文件，上传完成后再重命名为正式文件名。这样可以避免下游系统或入站拉取流程读取到未上传完成的半成品文件。

上传流程如下：

```text
校验本地文件
  ↓
生成远程正式路径
  ↓
生成远程临时路径
  ↓
创建远程目录
  ↓
上传到 .tmp 文件
  ↓
删除已存在的正式文件
  ↓
重命名 .tmp 为正式文件
  ↓
返回远程正式路径
```

先定义统一的业务异常，便于 Controller 或上层业务进行错误转换。

文件位置：`src/main/java/io/github/atengk/sftp/exception/SftpOperationException.java`

```java
package io.github.atengk.sftp.exception;

/**
 * SFTP 操作异常
 *
 * @author Ateng
 * @since 2026-05-07
 */
public class SftpOperationException extends RuntimeException {

    /**
     * 创建 SFTP 操作异常
     *
     * @param message 异常消息
     */
    public SftpOperationException(String message) {
        super(message);
    }

    /**
     * 创建 SFTP 操作异常
     *
     * @param message 异常消息
     * @param cause   原始异常
     */
    public SftpOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

定义文件服务接口，统一暴露上传、下载、删除和重命名能力。

文件位置：`src/main/java/io/github/atengk/sftp/service/SftpFileService.java`

```java
package io.github.atengk.sftp.service;

import java.nio.file.Path;

/**
 * SFTP 文件服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface SftpFileService {

    /**
     * 上传本地文件到远程目录
     *
     * @param localFile      本地文件路径
     * @param remoteDir      远程相对目录
     * @param remoteFileName 远程文件名，为空时使用本地文件名
     * @return 远程正式文件路径
     */
    String upload(Path localFile, String remoteDir, String remoteFileName);

    /**
     * 下载远程文件到本地文件
     *
     * @param remotePath 远程文件路径，支持绝对路径或基于 remoteBaseDir 的相对路径
     * @param localFile  本地目标文件
     * @return 本地目标文件
     */
    Path download(String remotePath, Path localFile);

    /**
     * 下载远程文件为字节数组
     *
     * @param remotePath 远程文件路径，支持绝对路径或基于 remoteBaseDir 的相对路径
     * @return 文件字节数组
     */
    byte[] downloadBytes(String remotePath);

    /**
     * 删除远程文件
     *
     * @param remotePath 远程文件路径，支持绝对路径或基于 remoteBaseDir 的相对路径
     * @return 是否删除成功
     */
    boolean delete(String remotePath);

    /**
     * 重命名远程文件
     *
     * @param fromPath 原远程文件路径
     * @param toPath   新远程文件路径
     */
    void rename(String fromPath, String toPath);
}
```

该实现类通过 `SftpRemoteFileTemplate` 完成上传、下载、删除和重命名，并在上传时自动创建远程目录。

文件位置：`src/main/java/io/github/atengk/sftp/service/impl/SftpFileServiceImpl.java`

```java
package io.github.atengk.sftp.service.impl;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.sftp.config.SftpProperties;
import io.github.atengk.sftp.core.SftpPathHelper;
import io.github.atengk.sftp.exception.SftpOperationException;
import io.github.atengk.sftp.service.SftpFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * SFTP 文件服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SftpFileServiceImpl implements SftpFileService {

    private final SftpRemoteFileTemplate sftpRemoteFileTemplate;

    private final SftpProperties sftpProperties;

    /**
     * 上传本地文件到远程目录
     *
     * @param localFile      本地文件路径
     * @param remoteDir      远程相对目录
     * @param remoteFileName 远程文件名，为空时使用本地文件名
     * @return 远程正式文件路径
     */
    @Override
    public String upload(Path localFile, String remoteDir, String remoteFileName) {
        if (localFile == null || !Files.isRegularFile(localFile)) {
            throw new IllegalArgumentException("本地文件不存在或不是普通文件");
        }

        String fileName = StrUtil.blankToDefault(remoteFileName, localFile.getFileName().toString());
        SftpPathHelper.validateFileName(fileName);

        String remotePath = SftpPathHelper.joinRemotePath(sftpProperties.getRemoteBaseDir(), remoteDir, fileName);
        String temporaryPath = remotePath + sftpProperties.getTemporarySuffix();

        try (InputStream inputStream = Files.newInputStream(localFile)) {
            sftpRemoteFileTemplate.execute(session -> {
                String parentDir = SftpPathHelper.getRemoteParentDir(remotePath);
                ensureRemoteDir(session, parentDir);

                if (session.exists(temporaryPath)) {
                    session.remove(temporaryPath);
                    log.info("删除已存在的SFTP临时文件：{}", temporaryPath);
                }

                session.write(inputStream, temporaryPath);
                log.info("SFTP临时文件上传完成，本地文件：{}，远程临时文件：{}", localFile, temporaryPath);

                if (session.exists(remotePath)) {
                    session.remove(remotePath);
                    log.info("删除已存在的SFTP正式文件：{}", remotePath);
                }

                session.rename(temporaryPath, remotePath);
                return true;
            });

            log.info("SFTP文件上传成功，本地文件：{}，远程文件：{}", localFile, remotePath);
            return remotePath;
        } catch (Exception e) {
            log.error("SFTP文件上传失败，本地文件：{}，远程文件：{}", localFile, remotePath, e);
            throw new SftpOperationException("SFTP文件上传失败：" + remotePath, e);
        }
    }

    /**
     * 下载远程文件到本地文件
     *
     * @param remotePath 远程文件路径，支持绝对路径或基于 remoteBaseDir 的相对路径
     * @param localFile  本地目标文件
     * @return 本地目标文件
     */
    @Override
    public Path download(String remotePath, Path localFile) {
        if (localFile == null) {
            throw new IllegalArgumentException("本地目标文件不能为空");
        }

        String fullRemotePath = SftpPathHelper.resolveRemotePath(sftpProperties.getRemoteBaseDir(), remotePath);
        SftpPathHelper.mkdirParent(localFile);

        try (OutputStream outputStream = Files.newOutputStream(localFile)) {
            Boolean result = sftpRemoteFileTemplate.execute(session -> {
                if (!session.exists(fullRemotePath)) {
                    throw new SftpOperationException("远程文件不存在：" + fullRemotePath);
                }
                session.read(fullRemotePath, outputStream);
                return true;
            });

            if (!Boolean.TRUE.equals(result)) {
                throw new SftpOperationException("SFTP文件下载未成功：" + fullRemotePath);
            }

            log.info("SFTP文件下载成功，远程文件：{}，本地文件：{}", fullRemotePath, localFile);
            return localFile;
        } catch (SftpOperationException e) {
            throw e;
        } catch (Exception e) {
            log.error("SFTP文件下载失败，远程文件：{}，本地文件：{}", fullRemotePath, localFile, e);
            throw new SftpOperationException("SFTP文件下载失败：" + fullRemotePath, e);
        }
    }

    /**
     * 下载远程文件为字节数组
     *
     * @param remotePath 远程文件路径，支持绝对路径或基于 remoteBaseDir 的相对路径
     * @return 文件字节数组
     */
    @Override
    public byte[] downloadBytes(String remotePath) {
        String fullRemotePath = SftpPathHelper.resolveRemotePath(sftpProperties.getRemoteBaseDir(), remotePath);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Boolean result = sftpRemoteFileTemplate.execute(session -> {
                if (!session.exists(fullRemotePath)) {
                    throw new SftpOperationException("远程文件不存在：" + fullRemotePath);
                }
                session.read(fullRemotePath, outputStream);
                return true;
            });

            if (!Boolean.TRUE.equals(result)) {
                throw new SftpOperationException("SFTP文件读取未成功：" + fullRemotePath);
            }

            log.info("SFTP文件读取成功，远程文件：{}，文件大小：{}字节", fullRemotePath, outputStream.size());
            return outputStream.toByteArray();
        } catch (SftpOperationException e) {
            throw e;
        } catch (Exception e) {
            log.error("SFTP文件读取失败，远程文件：{}", fullRemotePath, e);
            throw new SftpOperationException("SFTP文件读取失败：" + fullRemotePath, e);
        }
    }

    /**
     * 删除远程文件
     *
     * @param remotePath 远程文件路径，支持绝对路径或基于 remoteBaseDir 的相对路径
     * @return 是否删除成功
     */
    @Override
    public boolean delete(String remotePath) {
        String fullRemotePath = SftpPathHelper.resolveRemotePath(sftpProperties.getRemoteBaseDir(), remotePath);

        try {
            Boolean result = sftpRemoteFileTemplate.execute(session -> {
                if (!session.exists(fullRemotePath)) {
                    log.info("SFTP远程文件不存在，无需删除：{}", fullRemotePath);
                    return false;
                }
                return session.remove(fullRemotePath);
            });

            boolean deleted = Boolean.TRUE.equals(result);
            log.info("SFTP文件删除完成，远程文件：{}，删除结果：{}", fullRemotePath, deleted);
            return deleted;
        } catch (Exception e) {
            log.error("SFTP文件删除失败，远程文件：{}", fullRemotePath, e);
            throw new SftpOperationException("SFTP文件删除失败：" + fullRemotePath, e);
        }
    }

    /**
     * 重命名远程文件
     *
     * @param fromPath 原远程文件路径
     * @param toPath   新远程文件路径
     */
    @Override
    public void rename(String fromPath, String toPath) {
        String fullFromPath = SftpPathHelper.resolveRemotePath(sftpProperties.getRemoteBaseDir(), fromPath);
        String fullToPath = SftpPathHelper.resolveRemotePath(sftpProperties.getRemoteBaseDir(), toPath);

        try {
            sftpRemoteFileTemplate.execute(session -> {
                if (!session.exists(fullFromPath)) {
                    throw new SftpOperationException("原远程文件不存在：" + fullFromPath);
                }
                if (session.exists(fullToPath)) {
                    throw new SftpOperationException("目标远程文件已存在：" + fullToPath);
                }

                String parentDir = SftpPathHelper.getRemoteParentDir(fullToPath);
                ensureRemoteDir(session, parentDir);
                session.rename(fullFromPath, fullToPath);
                return true;
            });

            log.info("SFTP文件重命名成功，原文件：{}，新文件：{}", fullFromPath, fullToPath);
        } catch (SftpOperationException e) {
            throw e;
        } catch (Exception e) {
            log.error("SFTP文件重命名失败，原文件：{}，新文件：{}", fullFromPath, fullToPath, e);
            throw new SftpOperationException("SFTP文件重命名失败：" + fullFromPath, e);
        }
    }

    /**
     * 递归创建远程目录
     *
     * @param session   SFTP Session
     * @param remoteDir 远程目录
     */
    private void ensureRemoteDir(Session<SftpClient.DirEntry> session, String remoteDir) {
        if (!sftpProperties.isAutoCreateRemoteDir() || StrUtil.isBlank(remoteDir) || StrUtil.equals(remoteDir, StrUtil.SLASH)) {
            return;
        }

        try {
            String normalizedDir = remoteDir.replace("\\", "/");
            String[] parts = normalizedDir.split("/");
            StringBuilder current = new StringBuilder();

            for (String part : parts) {
                if (StrUtil.isBlank(part)) {
                    continue;
                }

                current.append(StrUtil.SLASH).append(part);
                String currentDir = current.toString();

                if (!session.exists(currentDir)) {
                    session.mkdir(currentDir);
                    log.info("创建SFTP远程目录：{}", currentDir);
                }
            }
        } catch (Exception e) {
            log.error("创建SFTP远程目录失败，目录：{}", remoteDir, e);
            throw new SftpOperationException("创建SFTP远程目录失败：" + remoteDir, e);
        }
    }
}
```

### 文件下载实现

文件下载支持两种方式：下载到本地路径，以及直接读取为字节数组。下载到本地路径适合批处理、异步任务、文件落盘后再解析的场景；读取为字节数组适合通过 HTTP 接口直接返回给调用方的场景。

下载到本地文件示例：

```java
Path localFile = Path.of("./data/sftp/download/order-20260507.csv");
sftpFileService.download("/upload/order/20260507/order-20260507.csv", localFile);
```

读取为字节数组示例：

```java
byte[] bytes = sftpFileService.downloadBytes("/upload/order/20260507/order-20260507.csv");
```

如果业务传入的是相对路径，例如：

```java
byte[] bytes = sftpFileService.downloadBytes("order/20260507/order-20260507.csv");
```

服务会自动拼接 `sftp.remote-base-dir`，最终访问：

```text
/upload/order/20260507/order-20260507.csv
```

下载实现中的关键点：

| 处理项       | 说明                                                  |
| ------------ | ----------------------------------------------------- |
| 文件存在校验 | 下载前通过 `session.exists(...)` 判断远程文件是否存在 |
| 本地目录创建 | 下载前自动创建本地目标文件父目录                      |
| 异常封装     | 将底层异常统一封装为 `SftpOperationException`         |
| 日志记录     | 成功和失败均记录远程路径、本地路径等关键字段          |

### 文件删除与重命名实现

文件删除适用于清理远程临时文件、删除业务作废文件或处理完成后归档前清理。删除方法返回布尔值，远程文件不存在时返回 `false`，不直接抛异常。

删除示例：

```java
boolean deleted = sftpFileService.delete("/upload/order/20260507/order-20260507.csv");
```

文件重命名适用于以下场景：

| 场景         | 示例                                     |
| ------------ | ---------------------------------------- |
| 上传完成标记 | `order.csv.tmp` 重命名为 `order.csv`     |
| 处理完成归档 | `order.csv` 重命名为 `archive/order.csv` |
| 错误文件隔离 | `order.csv` 重命名为 `error/order.csv`   |
| 防止重复处理 | 拉取前先将文件重命名为处理中状态         |

重命名示例：

```java
sftpFileService.rename(
        "/upload/order/20260507/order-20260507.csv.tmp",
        "/upload/order/20260507/order-20260507.csv"
);
```

删除与重命名的设计建议：

1. 删除接口建议保持幂等，文件不存在时返回 `false`，不作为系统异常处理。
2. 重命名接口建议在目标文件已存在时抛出异常，避免覆盖历史文件。
3. 归档类重命名建议按日期分目录，例如 `/archive/order/20260507/order.csv`。
4. 上传类重命名建议与临时后缀配合使用，例如 `.tmp`、`.part`、`.uploading`。
5. 下游文件监听或入站拉取流程应过滤临时后缀，避免处理未完成文件。



## Spring Integration 流程设计

本节用于说明如何通过 Spring Integration Java DSL 编排 SFTP 出站上传和入站拉取流程。出站流程适合把业务生成的本地文件推送到远程 SFTP；入站流程适合定时轮询远程目录，将文件同步到本地后交给后续业务处理。Spring Integration SFTP 出站适配器本质上是一个 `MessageHandler`，可以接收 `File`、`byte[]`、`String`、`InputStream`、`Resource` 等 payload 并上传到远程目录；入站适配器会先把远程文件同步到本地目录，再发送 `java.io.File` 类型消息到后续流程。([Spring Enterprise 文档](https://docs.enterprise.spring.io/spring-integration/reference/sftp/outbound.html?utm_source=chatgpt.com))

### 出站上传流程

出站上传流程适用于“业务代码生成文件后发送到 SFTP”的场景。它通过消息通道接收本地文件，并根据消息头中的远程目录和远程文件名完成上传。相比直接调用 `SftpFileService.upload(...)`，出站流程更适合文件传输需要接入消息流、异步处理、统一通道、网关调用或后续扩展链路的场景。

推荐流程如下：

```text
业务服务生成本地文件
  ↓
调用 SftpUploadGateway
  ↓
发送 File 消息到 sftpUploadChannel
  ↓
SFTP 出站适配器上传文件
  ↓
使用临时文件名写入远程目录
  ↓
上传完成后自动重命名为正式文件名
```

Spring Integration SFTP 出站适配器支持 `use-temporary-file-name` 和 `temporary-file-suffix` 机制，默认会先写入临时文件，再在传输完成后重命名，避免远程系统读取到未写完的半成品文件；还支持通过 `FileExistsMode` 控制远程文件已存在时的处理策略。([Spring Enterprise 文档](https://docs.enterprise.spring.io/spring-integration/reference/sftp/outbound.html?utm_source=chatgpt.com))

本节涉及的文件结构如下：

```text
src/main/java/io/github/atengk/sftp/integration/SftpIntegrationFlowConfig.java
src/main/java/io/github/atengk/sftp/integration/SftpUploadGateway.java
```

该配置类定义出站上传通道、入站拉取通道、远程文件过滤器和 IntegrationFlow。

文件位置：`src/main/java/io/github/atengk/sftp/integration/SftpIntegrationFlowConfig.java`

```java
package io.github.atengk.sftp.integration;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.sftp.config.SftpProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.remote.FileExistsMode;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.sftp.dsl.Sftp;
import org.springframework.integration.sftp.filters.SftpPersistentAcceptOnceFileListFilter;
import org.springframework.messaging.MessageChannel;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * SFTP IntegrationFlow 配置
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SftpIntegrationFlowConfig {

    private final SftpProperties sftpProperties;

    private final SessionFactory<SftpClient.DirEntry> cachingSftpSessionFactory;

    /**
     * SFTP 出站上传通道
     *
     * @return 上传消息通道
     */
    @Bean
    public MessageChannel sftpUploadChannel() {
        return new DirectChannel();
    }

    /**
     * SFTP 入站拉取通道
     *
     * @return 入站消息通道
     */
    @Bean
    public MessageChannel sftpInboundChannel() {
        return MessageChannels.direct().getObject();
    }

    /**
     * SFTP 出站上传流程
     *
     * @return IntegrationFlow
     */
    @Bean
    public IntegrationFlow sftpOutboundUploadFlow() {
        return IntegrationFlow
                .from(sftpUploadChannel())
                .handle(Sftp.outboundAdapter(cachingSftpSessionFactory)
                        // 远程目录从消息头中获取，调用网关时传入
                        .remoteDirectoryExpression("headers['remoteDir']")
                        // 远程文件名从消息头中获取；为空时使用本地文件名
                        .fileNameExpression("headers['remoteFileName'] ?: payload.name")
                        // 上传过程中使用临时文件名，完成后自动重命名
                        .useTemporaryFileName(true)
                        .temporaryFileSuffix(sftpProperties.getTemporarySuffix())
                        // 远程目录不存在时自动创建
                        .autoCreateDirectory(sftpProperties.isAutoCreateRemoteDir())
                        // 远程文件已存在时覆盖
                        .fileExistsMode(FileExistsMode.REPLACE))
                .get();
    }

    /**
     * SFTP 入站拉取流程
     *
     * @return IntegrationFlow
     */
    @Bean
    public IntegrationFlow sftpInboundPullFlow() {
        File localDirectory = FileUtil.mkdir(sftpProperties.getLocalDownloadDir());

        return IntegrationFlow
                .from(Sftp.inboundAdapter(cachingSftpSessionFactory)
                                // 保留远程文件修改时间，便于后续按时间判断
                                .preserveTimestamp(true)
                                // 远程拉取目录
                                .remoteDirectory(sftpProperties.getRemoteBaseDir())
                                // 本地同步目录
                                .localDirectory(localDirectory)
                                // 自动创建本地目录
                                .autoCreateLocalDirectory(true)
                                // 拉取后不删除远程文件；是否删除应由业务处理结果决定
                                .deleteRemoteFiles(false)
                                // 远程文件过滤器，避免重复拉取和拉取临时文件
                                .filter(this::filterRemoteFiles),
                        endpoint -> endpoint
                                .id("sftpInboundPullAdapter")
                                .autoStartup(true)
                                .poller(Pollers.fixedDelay(Duration.ofSeconds(10))
                                        .maxMessagesPerPoll(10)))
                .channel(sftpInboundChannel())
                .handle(File.class, (file, headers) -> {
                    log.info("SFTP入站文件同步完成，本地文件：{}，文件大小：{}字节",
                            file.getAbsolutePath(), file.length());
                    return null;
                })
                .get();
    }

    /**
     * 过滤远程文件
     *
     * @param files 远程文件列表
     * @return 允许拉取的文件列表
     */
    private List<SftpClient.DirEntry> filterRemoteFiles(SftpClient.DirEntry[] files) {
        SftpPersistentAcceptOnceFileListFilter persistentFilter =
                new SftpPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "sftp:inbound:");

        SftpClient.DirEntry[] acceptedFiles = persistentFilter.filterFiles(files);

        return Arrays.stream(acceptedFiles)
                // 跳过目录
                .filter(file -> !file.getAttributes().isDirectory())
                // 跳过上传中的临时文件
                .filter(file -> !StrUtil.endWithAny(file.getFilename(), ".tmp", ".writing", ".part", ".uploading"))
                // 跳过隐藏文件
                .filter(file -> !StrUtil.startWith(file.getFilename(), "."))
                .toList();
    }
}
```

该网关用于让业务代码以方法调用形式触发出站上传流程。

文件位置：`src/main/java/io/github/atengk/sftp/integration/SftpUploadGateway.java`

```java
package io.github.atengk.sftp.integration;

import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.handler.annotation.Header;

import java.io.File;

/**
 * SFTP 上传网关
 *
 * @author Ateng
 * @since 2026-05-07
 */
@MessagingGateway
public interface SftpUploadGateway {

    /**
     * 上传文件到 SFTP
     *
     * @param file           本地文件
     * @param remoteDir      远程目录
     * @param remoteFileName 远程文件名
     */
    @Gateway(requestChannel = "sftpUploadChannel")
    void upload(File file,
                @Header("remoteDir") String remoteDir,
                @Header("remoteFileName") String remoteFileName);
}
```

使用出站上传网关时，业务代码只需要传入本地文件、远程目录和远程文件名。

```java
File file = new File("./data/sftp/temp/order-20260507.csv");
sftpUploadGateway.upload(file, "/upload/order/20260507", "order-20260507.csv");
```

### 入站拉取流程

入站拉取流程用于定时从远程目录同步文件到本地目录。Spring Integration SFTP 入站适配器是轮询型消费者，需要配置 Poller；当文件被传输到本地目录后，会生成 payload 类型为 `java.io.File` 的消息并发送到指定通道。([Home](https://docs.spring.io/spring-integration/reference/7.0/sftp/inbound.html?utm_source=chatgpt.com))

推荐流程如下：

```text
定时触发 Poller
  ↓
连接 SFTP 远程目录
  ↓
列出远程文件
  ↓
执行远程文件过滤
  ↓
同步文件到本地目录
  ↓
执行本地文件过滤
  ↓
发送 File 消息到后续处理流程
```

入站拉取设计建议如下：

| 设计项       | 建议                                                         |
| ------------ | ------------------------------------------------------------ |
| 拉取目录     | 按业务类型拆分，例如 `/upload/order`、`/upload/bill`         |
| 轮询间隔     | 根据业务时效配置，例如 10 秒、1 分钟、5 分钟                 |
| 单次拉取数量 | 大文件或集群部署建议限制 `maxMessagesPerPoll`                |
| 远程文件处理 | 默认不删除远程文件，业务处理成功后再归档或删除               |
| 本地文件处理 | 建议处理成功后移动到 `done` 目录，处理失败移动到 `error` 目录 |
| 集群部署     | 建议使用 Redis 等共享 `MetadataStore` 实现跨节点幂等         |

Spring Integration 入站适配器涉及两层过滤：远程过滤器决定哪些远程文件会被下载，本地过滤器决定哪些本地文件会被发送为消息。官方文档说明，`SftpPersistentAcceptOnceFileListFilter` 可以基于 `MetadataStore` 保存已接收文件状态，适合防止重复同步；默认本地过滤也会阻止仍带临时后缀的下载中文件被处理。([Home](https://docs.spring.io/spring-integration/reference/7.0/sftp/inbound.html?utm_source=chatgpt.com))

### 文件过滤策略

文件过滤策略用于避免处理临时文件、重复文件、隐藏文件、目录和不符合业务命名规则的文件。SFTP 文件传输中最常见的问题是远程文件刚出现但还没写完，因此应避免直接拉取 `.tmp`、`.writing`、`.part`、`.uploading` 等后缀文件。Spring Integration 出站适配器本身也支持临时文件后缀机制，默认临时后缀为 `.writing`，可通过 `temporary-file-suffix` 调整。([Spring Enterprise 文档](https://docs.enterprise.spring.io/spring-integration/reference/sftp/outbound.html?utm_source=chatgpt.com))

推荐过滤规则如下：

| 规则         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 后缀过滤     | 排除 `.tmp`、`.writing`、`.part`、`.uploading`               |
| 目录过滤     | 只处理普通文件，不处理目录                                   |
| 隐藏文件过滤 | 排除以 `.` 开头的隐藏文件                                    |
| 文件名过滤   | 按业务只允许 `.csv`、`.xlsx`、`.json`、`.zip` 等格式         |
| 幂等过滤     | 使用 `SftpPersistentAcceptOnceFileListFilter` 或业务数据库记录 |
| 时间过滤     | 大文件场景可只处理最后修改时间早于一定时间的文件             |

如果项目需要只拉取 CSV 和 JSON 文件，可以将过滤条件调整为：

```java
private List<SftpClient.DirEntry> filterRemoteFiles(SftpClient.DirEntry[] files) {
    SftpPersistentAcceptOnceFileListFilter persistentFilter =
            new SftpPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "sftp:inbound:");

    SftpClient.DirEntry[] acceptedFiles = persistentFilter.filterFiles(files);

    return Arrays.stream(acceptedFiles)
            .filter(file -> !file.getAttributes().isDirectory())
            .filter(file -> !StrUtil.startWith(file.getFilename(), "."))
            .filter(file -> !StrUtil.endWithAny(file.getFilename(), ".tmp", ".writing", ".part", ".uploading"))
            .filter(file -> StrUtil.endWithAny(file.getFilename(), ".csv", ".json"))
            .toList();
}
```

生产环境注意事项：

1. 单机部署可以使用内存型 `SimpleMetadataStore`，但应用重启后状态会丢失。
2. 多实例部署应使用 Redis、JDBC 或其他共享 `MetadataStore`，否则多个实例可能重复拉取同一个远程文件。
3. 对账、结算、付款等强一致文件场景，建议同时使用远程过滤器和业务表幂等记录。
4. 对于上游无法保证“写完后重命名”的文件，可以使用最后修改时间过滤策略，避免过早拉取大文件。
5. 不建议仅依赖文件名判断是否处理完成，应结合业务流水号、文件批次号或远程归档目录。

### 本地目录同步策略

本地目录同步策略用于管理远程文件落盘后的处理路径。入站适配器会将远程文件先同步到本地目录，再把本地文件作为消息 payload 交给后续处理流程。因此本地目录既是文件处理入口，也是问题排查和失败恢复的重要依据。

推荐目录结构如下：

```text
./data/sftp
├── download
│   ├── order
│   ├── bill
│   └── reconciliation
├── done
│   ├── order
│   ├── bill
│   └── reconciliation
├── error
│   ├── order
│   ├── bill
│   └── reconciliation
└── temp
```

目录含义如下：

| 目录       | 说明                             |
| ---------- | -------------------------------- |
| `download` | 入站适配器同步下来的待处理文件   |
| `done`     | 业务处理成功后的归档文件         |
| `error`    | 业务处理失败后的问题文件         |
| `temp`     | 上传前临时文件或接口上传临时文件 |

本地文件处理建议如下：

| 场景         | 策略                                     |
| ------------ | ---------------------------------------- |
| 处理成功     | 移动到 `done` 目录，并记录处理结果       |
| 处理失败     | 移动到 `error` 目录，并记录异常原因      |
| 重复文件     | 跳过处理，保留日志或移动到重复文件目录   |
| 文件格式错误 | 移动到 `error` 目录，不删除原始文件      |
| 文件过大     | 分片处理或流式处理，不建议一次性读入内存 |

如果需要在入站流程中处理完成后移动本地文件，可以在 `.handle(...)` 中调用业务服务。示例：

```java
.handle(File.class, (file, headers) -> {
    try {
        log.info("开始处理SFTP入站文件：{}", file.getAbsolutePath());

        // TODO 调用业务解析服务，例如订单导入、对账解析、报表入库等
        // orderImportService.importFile(file.toPath());

        File doneDir = FileUtil.mkdir("./data/sftp/done");
        FileUtil.move(file, new File(doneDir, file.getName()), true);

        log.info("SFTP入站文件处理成功，文件：{}", file.getName());
    } catch (Exception e) {
        File errorDir = FileUtil.mkdir("./data/sftp/error");
        FileUtil.move(file, new File(errorDir, file.getName()), true);

        log.error("SFTP入站文件处理失败，已移动到错误目录，文件：{}", file.getName(), e);
    }
    return null;
})
```

## 接口设计

本节定义 SFTP 文件上传、下载、查询和删除接口。接口层建议只处理 HTTP 参数、文件接收、响应封装和调用结果转换，不直接操作 `SftpRemoteFileTemplate`。实际 SFTP 操作应委托给前面定义的 `SftpFileService`。

接口路径建议统一放在 `/api/sftp/files` 下：

| 功能         | 请求方法 | 接口路径                   | 说明                       |
| ------------ | -------- | -------------------------- | -------------------------- |
| 文件上传     | `POST`   | `/api/sftp/files/upload`   | 上传 multipart 文件到 SFTP |
| 文件下载     | `GET`    | `/api/sftp/files/download` | 下载远程文件               |
| 文件列表查询 | `GET`    | `/api/sftp/files`          | 查询远程目录文件列表       |
| 文件存在判断 | `GET`    | `/api/sftp/files/exists`   | 判断远程文件是否存在       |
| 文件删除     | `DELETE` | `/api/sftp/files`          | 删除远程文件               |

本节涉及的文件结构如下：

```text
src/main/java/io/github/atengk/sftp/common/ApiResult.java
src/main/java/io/github/atengk/sftp/vo/SftpFileInfoVO.java
src/main/java/io/github/atengk/sftp/service/SftpFileService.java
src/main/java/io/github/atengk/sftp/service/impl/SftpFileServiceImpl.java
src/main/java/io/github/atengk/sftp/controller/SftpFileController.java
```

接口统一响应对象如下。

文件位置：`src/main/java/io/github/atengk/sftp/common/ApiResult.java`

```java
package io.github.atengk.sftp.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口统一响应
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 成功响应
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 统一响应
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "操作成功", data);
    }

    /**
     * 失败响应
     *
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 统一响应
     */
    public static <T> ApiResult<T> fail(String message) {
        return new ApiResult<>(500, message, null);
    }
}
```

远程文件信息返回对象如下。

文件位置：`src/main/java/io/github/atengk/sftp/vo/SftpFileInfoVO.java`

```java
package io.github.atengk.sftp.vo;

import lombok.Builder;
import lombok.Data;

/**
 * SFTP 文件信息
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
public class SftpFileInfoVO {

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 远程完整路径
     */
    private String remotePath;

    /**
     * 是否目录
     */
    private Boolean directory;

    /**
     * 文件大小，单位字节
     */
    private Long size;

    /**
     * 最后修改时间戳，单位毫秒
     */
    private Long lastModifiedTime;
}
```

### 文件上传接口

文件上传接口接收 `multipart/form-data` 请求，将上传文件先保存到本地临时目录，再调用 `SftpFileService.upload(...)` 上传到远程 SFTP 目录。上传完成后删除本地临时文件，避免临时目录持续增长。

接口定义如下：

| 项目             | 内容                                             |
| ---------------- | ------------------------------------------------ |
| 请求方法         | `POST`                                           |
| 请求路径         | `/api/sftp/files/upload`                         |
| Content-Type     | `multipart/form-data`                            |
| 参数 `file`      | 上传文件                                         |
| 参数 `remoteDir` | 远程目录，可选，基于 `sftp.remote-base-dir` 拼接 |
| 参数 `fileName`  | 远程文件名，可选，默认使用原始文件名             |
| 返回值           | 上传后的远程文件路径                             |

Controller 完整代码如下。

文件位置：`src/main/java/io/github/atengk/sftp/controller/SftpFileController.java`

```java
package io.github.atengk.sftp.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.sftp.common.ApiResult;
import io.github.atengk.sftp.config.SftpProperties;
import io.github.atengk.sftp.core.SftpPathHelper;
import io.github.atengk.sftp.service.SftpFileService;
import io.github.atengk.sftp.vo.SftpFileInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

/**
 * SFTP 文件接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sftp/files")
public class SftpFileController {

    private final SftpFileService sftpFileService;

    private final SftpProperties sftpProperties;

    /**
     * 上传文件到 SFTP
     *
     * @param file      上传文件
     * @param remoteDir 远程目录
     * @param fileName  远程文件名
     * @return 远程文件路径
     */
    @PostMapping("/upload")
    public ApiResult<String> upload(@RequestPart("file") MultipartFile file,
                                    @RequestParam(defaultValue = "") String remoteDir,
                                    @RequestParam(required = false) String fileName) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String originalFileName = FileNameUtil.getName(file.getOriginalFilename());
        String remoteFileName = StrUtil.blankToDefault(fileName, originalFileName);
        SftpPathHelper.validateFileName(remoteFileName);

        String dateDir = DateUtil.format(new Date(), "yyyyMMdd");
        String temporaryFileName = IdUtil.fastSimpleUUID() + "-" + remoteFileName;
        Path temporaryFile = Path.of(sftpProperties.getLocalTempDir(), dateDir, temporaryFileName);

        try {
            SftpPathHelper.mkdirParent(temporaryFile);
            file.transferTo(temporaryFile);

            String remotePath = sftpFileService.upload(temporaryFile, remoteDir, remoteFileName);
            log.info("接口上传SFTP文件成功，原始文件：{}，远程文件：{}", originalFileName, remotePath);

            return ApiResult.success(remotePath);
        } catch (Exception e) {
            log.error("接口上传SFTP文件失败，原始文件：{}", originalFileName, e);
            throw new RuntimeException("SFTP文件上传失败", e);
        } finally {
            FileUtil.del(temporaryFile);
        }
    }

    /**
     * 下载 SFTP 文件
     *
     * @param remotePath 远程文件路径
     * @return 文件响应
     */
    @GetMapping("/download")
    public ResponseEntity<ByteArrayResource> download(@RequestParam String remotePath) {
        byte[] bytes = sftpFileService.downloadBytes(remotePath);
        String fileName = FileNameUtil.getName(remotePath);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();

        log.info("接口下载SFTP文件成功，远程文件：{}，文件大小：{}字节", remotePath, bytes.length);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(bytes.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(new ByteArrayResource(bytes));
    }

    /**
     * 查询远程目录文件列表
     *
     * @param remoteDir 远程目录
     * @return 文件列表
     */
    @GetMapping
    public ApiResult<List<SftpFileInfoVO>> list(@RequestParam(defaultValue = "") String remoteDir) {
        List<SftpFileInfoVO> files = sftpFileService.list(remoteDir);
        log.info("接口查询SFTP文件列表完成，远程目录：{}，数量：{}", remoteDir, files.size());
        return ApiResult.success(files);
    }

    /**
     * 判断远程文件是否存在
     *
     * @param remotePath 远程文件路径
     * @return 是否存在
     */
    @GetMapping("/exists")
    public ApiResult<Boolean> exists(@RequestParam String remotePath) {
        boolean exists = sftpFileService.exists(remotePath);
        log.info("接口判断SFTP文件是否存在完成，远程文件：{}，结果：{}", remotePath, exists);
        return ApiResult.success(exists);
    }

    /**
     * 删除远程文件
     *
     * @param remotePath 远程文件路径
     * @return 删除结果
     */
    @DeleteMapping
    public ApiResult<Boolean> delete(@RequestParam String remotePath) {
        boolean deleted = sftpFileService.delete(remotePath);
        log.info("接口删除SFTP文件完成，远程文件：{}，结果：{}", remotePath, deleted);
        return ApiResult.success(deleted);
    }
}
```

上传接口调用示例：

```bash
curl -X POST "http://127.0.0.1:8080/api/sftp/files/upload" \
  -F "file=@./data/sftp/temp/order-20260507.csv" \
  -F "remoteDir=order/20260507" \
  -F "fileName=order-20260507.csv"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "/upload/order/20260507/order-20260507.csv"
}
```

### 文件下载接口

文件下载接口通过远程文件路径读取文件内容，并以二进制流返回。该接口适合下载中小型文件；如果文件较大，建议使用流式响应或先落盘后返回下载地址。

接口定义如下：

| 项目              | 内容                                 |
| ----------------- | ------------------------------------ |
| 请求方法          | `GET`                                |
| 请求路径          | `/api/sftp/files/download`           |
| 参数 `remotePath` | 远程文件路径，支持绝对路径或相对路径 |
| 响应类型          | `application/octet-stream`           |
| 返回值            | 文件二进制内容                       |

调用示例：

```bash
curl -L -o order-20260507.csv \
  "http://127.0.0.1:8080/api/sftp/files/download?remotePath=/upload/order/20260507/order-20260507.csv"
```

接口说明：

| 处理步骤     | 说明                                             |
| ------------ | ------------------------------------------------ |
| 读取远程文件 | 调用 `sftpFileService.downloadBytes(remotePath)` |
| 设置文件名   | 从 `remotePath` 中提取文件名                     |
| 设置响应头   | 使用 `Content-Disposition: attachment`           |
| 返回内容     | 使用 `ByteArrayResource` 返回字节数组            |

### 文件查询接口

文件查询接口用于列出远程目录下的文件，并支持判断某个远程文件是否存在。查询能力依赖 `RemoteFileTemplate` 或 `Session` 的远程文件操作能力，`RemoteFileTemplate` 官方 API 提供 `exists`、`remove`、`rename`、`send` 等远程操作方法。([Home](https://docs.spring.io/spring-integration/api/org/springframework/integration/file/remote/RemoteFileTemplate.html?utm_source=chatgpt.com))

需要在前面定义的 `SftpFileService` 中补充查询方法。

文件位置：`src/main/java/io/github/atengk/sftp/service/SftpFileService.java`

```java
package io.github.atengk.sftp.service;

import io.github.atengk.sftp.vo.SftpFileInfoVO;

import java.nio.file.Path;
import java.util.List;

/**
 * SFTP 文件服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface SftpFileService {

    /**
     * 上传本地文件到远程目录
     *
     * @param localFile      本地文件路径
     * @param remoteDir      远程相对目录
     * @param remoteFileName 远程文件名，为空时使用本地文件名
     * @return 远程正式文件路径
     */
    String upload(Path localFile, String remoteDir, String remoteFileName);

    /**
     * 下载远程文件到本地文件
     *
     * @param remotePath 远程文件路径
     * @param localFile  本地目标文件
     * @return 本地目标文件
     */
    Path download(String remotePath, Path localFile);

    /**
     * 下载远程文件为字节数组
     *
     * @param remotePath 远程文件路径
     * @return 文件字节数组
     */
    byte[] downloadBytes(String remotePath);

    /**
     * 查询远程目录文件列表
     *
     * @param remoteDir 远程目录
     * @return 文件列表
     */
    List<SftpFileInfoVO> list(String remoteDir);

    /**
     * 判断远程文件是否存在
     *
     * @param remotePath 远程文件路径
     * @return 是否存在
     */
    boolean exists(String remotePath);

    /**
     * 删除远程文件
     *
     * @param remotePath 远程文件路径
     * @return 是否删除成功
     */
    boolean delete(String remotePath);

    /**
     * 重命名远程文件
     *
     * @param fromPath 原远程文件路径
     * @param toPath   新远程文件路径
     */
    void rename(String fromPath, String toPath);
}
```

在 `SftpFileServiceImpl` 中补充 `list` 和 `exists` 方法。

文件位置：`src/main/java/io/github/atengk/sftp/service/impl/SftpFileServiceImpl.java`

```java
@Override
public List<SftpFileInfoVO> list(String remoteDir) {
    String fullRemoteDir = SftpPathHelper.resolveRemotePath(sftpProperties.getRemoteBaseDir(), remoteDir);

    try {
        SftpClient.DirEntry[] entries = sftpRemoteFileTemplate.execute(session -> {
            if (!session.exists(fullRemoteDir)) {
                throw new SftpOperationException("远程目录不存在：" + fullRemoteDir);
            }
            return session.list(fullRemoteDir);
        });

        if (entries == null) {
            return List.of();
        }

        List<SftpFileInfoVO> files = Arrays.stream(entries)
                .filter(entry -> !StrUtil.equals(entry.getFilename(), "."))
                .filter(entry -> !StrUtil.equals(entry.getFilename(), ".."))
                .map(entry -> SftpFileInfoVO.builder()
                        .fileName(entry.getFilename())
                        .remotePath(SftpPathHelper.joinRemotePath(fullRemoteDir, entry.getFilename()))
                        .directory(entry.getAttributes().isDirectory())
                        .size(entry.getAttributes().getSize())
                        .lastModifiedTime(entry.getAttributes().getModifyTime() == null
                                ? null
                                : entry.getAttributes().getModifyTime().toMillis())
                        .build())
                .toList();

        log.info("SFTP文件列表查询成功，远程目录：{}，数量：{}", fullRemoteDir, files.size());
        return files;
    } catch (SftpOperationException e) {
        throw e;
    } catch (Exception e) {
        log.error("SFTP文件列表查询失败，远程目录：{}", fullRemoteDir, e);
        throw new SftpOperationException("SFTP文件列表查询失败：" + fullRemoteDir, e);
    }
}

@Override
public boolean exists(String remotePath) {
    String fullRemotePath = SftpPathHelper.resolveRemotePath(sftpProperties.getRemoteBaseDir(), remotePath);

    try {
        Boolean exists = sftpRemoteFileTemplate.execute(session -> session.exists(fullRemotePath));
        boolean result = Boolean.TRUE.equals(exists);

        log.info("SFTP文件存在性检查完成，远程路径：{}，结果：{}", fullRemotePath, result);
        return result;
    } catch (Exception e) {
        log.error("SFTP文件存在性检查失败，远程路径：{}", fullRemotePath, e);
        throw new SftpOperationException("SFTP文件存在性检查失败：" + fullRemotePath, e);
    }
}
```

注意：上面是追加方法代码，`SftpFileServiceImpl` 需要额外补充以下导入：

```java
import io.github.atengk.sftp.vo.SftpFileInfoVO;

import java.util.Arrays;
import java.util.List;
```

文件列表查询调用示例：

```bash
curl "http://127.0.0.1:8080/api/sftp/files?remoteDir=order/20260507"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "fileName": "order-20260507.csv",
      "remotePath": "/upload/order/20260507/order-20260507.csv",
      "directory": false,
      "size": 2048,
      "lastModifiedTime": 1778123456000
    }
  ]
}
```

文件存在判断调用示例：

```bash
curl "http://127.0.0.1:8080/api/sftp/files/exists?remotePath=/upload/order/20260507/order-20260507.csv"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": true
}
```

### 文件删除接口

文件删除接口用于删除远程 SFTP 文件。该接口建议设计为幂等接口：远程文件存在时删除并返回 `true`；远程文件不存在时返回 `false`，不直接视为系统异常。`RemoteFileTemplate.remove(path)` 的语义是删除远程文件，并在删除成功时返回 `true`。([Home](https://docs.spring.io/spring-integration/docs/current/api/org/springframework/integration/file/remote/RemoteFileTemplate.html?utm_source=chatgpt.com))

接口定义如下：

| 项目              | 内容              |
| ----------------- | ----------------- |
| 请求方法          | `DELETE`          |
| 请求路径          | `/api/sftp/files` |
| 参数 `remotePath` | 远程文件路径      |
| 返回值            | 是否删除成功      |

调用示例：

```bash
curl -X DELETE \
  "http://127.0.0.1:8080/api/sftp/files?remotePath=/upload/order/20260507/order-20260507.csv"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": true
}
```

删除接口注意事项：

1. 不建议开放目录删除能力，避免误删远程目录。
2. 删除前应校验路径是否在 `sftp.remote-base-dir` 范围内。
3. 对关键业务文件，建议优先移动到归档目录，而不是直接删除。
4. 如果需要审计，应记录操作人、远程路径、删除时间和删除结果。
5. 生产环境建议在 Controller 外层增加鉴权、操作审计和异常统一处理。



## 异常处理

SFTP 模块涉及网络连接、认证、远程目录权限、文件不存在、传输中断、重复文件、业务幂等等问题。异常处理应分为三层：底层异常捕获、模块异常封装、接口响应转换。业务代码不应直接把 Apache MINA SSHD、Spring Integration 或 IO 层异常暴露给前端调用方。

推荐异常处理流程如下：

```text
底层异常
  ↓
SftpOperationException 统一封装
  ↓
GlobalExceptionHandler 转换响应
  ↓
接口返回统一错误结构
  ↓
日志记录完整异常栈和关键参数
```

### 连接异常处理

连接异常主要发生在应用启动、首次获取 Session、远程服务不可达、用户名密码错误、私钥错误、known_hosts 校验失败等阶段。连接异常应区分为“配置错误”和“运行期连接失败”两类。

常见连接异常如下：

| 异常场景         | 常见原因                            | 处理建议                         |
| ---------------- | ----------------------------------- | -------------------------------- |
| 主机不可达       | SFTP 地址错误、网络不通、防火墙拦截 | 返回连接失败，记录 host、port    |
| 认证失败         | 用户名、密码、私钥、私钥口令错误    | 返回认证失败，避免打印密码       |
| 主机密钥校验失败 | `known_hosts` 不匹配或未配置        | 生产环境应中断连接并提示配置错误 |
| Session 获取超时 | 连接池耗尽、远程服务响应慢          | 调整缓存大小、等待超时和并发数   |
| 连接被关闭       | 远程服务断开、网络抖动              | 依赖缓存 Session 检测和重试策略  |

Spring Integration 中的轮询端点可以将异常发送到错误通道；`MessagePublishingErrorHandler` 是 Spring Integration 提供的 `ErrorHandler` 实现，会把异常封装为 `ErrorMessage` 并发送到 `MessageChannel`。因此，入站拉取这类异步流程建议配置专门的错误通道集中记录。([Home](https://www.springframework.org/spring-integration/docs/7.0.1/api/org/springframework/integration/channel/MessagePublishingErrorHandler.html?utm_source=chatgpt.com))

在前面 `SftpIntegrationFlowConfig` 中补充错误通道和错误处理流程。

文件位置：`src/main/java/io/github/atengk/sftp/integration/SftpIntegrationFlowConfig.java`

```java
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ErrorMessage;
/**
 * SFTP 错误通道
 *
 * @return 错误消息通道
 */
@Bean
public MessageChannel sftpErrorChannel() {
    return new DirectChannel();
}

/**
 * SFTP 错误处理流程
 *
 * @return IntegrationFlow
 */
@Bean
public IntegrationFlow sftpErrorFlow() {
    return IntegrationFlow
            .from("sftpErrorChannel")
            .handle(Message.class, (message, headers) -> {
                Object payload = message.getPayload();
                if (payload instanceof ErrorMessage errorMessage) {
                    Throwable throwable = errorMessage.getPayload();
                    log.error("SFTP集成流程执行异常，headers：{}", errorMessage.getHeaders(), throwable);
                } else if (payload instanceof Throwable throwable) {
                    log.error("SFTP集成流程执行异常", throwable);
                } else {
                    log.error("SFTP集成流程收到未知错误消息，payload：{}", payload);
                }
                return null;
            })
            .get();
}
```

入站拉取 Poller 中配置错误通道。

文件位置：`src/main/java/io/github/atengk/sftp/integration/SftpIntegrationFlowConfig.java`

```java
.poller(Pollers.fixedDelay(Duration.ofSeconds(10))
        .maxMessagesPerPoll(10)
        .errorChannel("sftpErrorChannel"))
```

连接异常处理建议：

1. 应用启动时只校验必要配置，不建议强制连接 SFTP，避免远程服务短暂不可用导致应用无法启动。
2. 首次业务调用失败时，应返回明确的连接失败信息。
3. 日志中可以打印 `host`、`port`、`username`，不要打印 `password`、私钥内容、私钥密码。
4. 生产环境建议关闭 `allowUnknownKeys`，并配置 `known_hosts`。
5. 连接池等待超时应小于接口网关超时时间，避免调用方长时间阻塞。

### 文件传输异常处理

文件传输异常主要发生在上传、下载、删除、重命名、列表查询等操作过程中。处理原则是：参数错误直接返回业务错误；远程文件不存在返回明确错误；网络或 IO 异常统一封装为传输失败；上传中断产生的临时文件允许下次覆盖清理。

常见传输异常如下：

| 异常场景       | 处理方式                                         |
| -------------- | ------------------------------------------------ |
| 本地文件不存在 | 参数校验失败，直接抛出业务异常                   |
| 远程目录不存在 | 开启自动创建时创建目录；关闭时返回目录不存在     |
| 远程文件不存在 | 下载、重命名时抛出文件不存在；删除时返回 `false` |
| 远程文件已存在 | 上传可覆盖；重命名默认不覆盖                     |
| 上传中断       | 保留或删除 `.tmp` 文件，下次上传前先清理         |
| 下载中断       | 删除本地未完成文件或使用临时文件下载后重命名     |
| 权限不足       | 返回权限错误，记录远程路径和操作类型             |
| 文件过大       | 避免一次性读取到内存，优先使用流式下载或落盘下载 |

建议将前面定义的 `SftpOperationException` 替换为支持错误码的版本。

文件位置：`src/main/java/io/github/atengk/sftp/exception/SftpErrorCode.java`

```java
package io.github.atengk.sftp.exception;

import lombok.Getter;

/**
 * SFTP 错误码
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Getter
public enum SftpErrorCode {

    /**
     * 配置错误
     */
    CONFIG_INVALID(400, "SFTP配置错误"),

    /**
     * 连接失败
     */
    CONNECT_FAILED(503, "SFTP连接失败"),

    /**
     * 远程路径不存在
     */
    REMOTE_PATH_NOT_FOUND(404, "SFTP远程路径不存在"),

    /**
     * 本地文件不存在
     */
    LOCAL_FILE_NOT_FOUND(400, "本地文件不存在"),

    /**
     * 文件传输失败
     */
    TRANSFER_FAILED(500, "SFTP文件传输失败"),

    /**
     * 文件已存在
     */
    FILE_ALREADY_EXISTS(409, "SFTP文件已存在"),

    /**
     * 权限不足
     */
    PERMISSION_DENIED(403, "SFTP权限不足"),

    /**
     * 未知异常
     */
    UNKNOWN(500, "SFTP未知异常");

    private final Integer code;

    private final String message;

    /**
     * 创建错误码
     *
     * @param code    状态码
     * @param message 错误消息
     */
    SftpErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

文件位置：`src/main/java/io/github/atengk/sftp/exception/SftpOperationException.java`

```java
package io.github.atengk.sftp.exception;

import lombok.Getter;

/**
 * SFTP 操作异常
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Getter
public class SftpOperationException extends RuntimeException {

    /**
     * 错误码
     */
    private final SftpErrorCode errorCode;

    /**
     * 创建 SFTP 操作异常
     *
     * @param errorCode 错误码
     */
    public SftpOperationException(SftpErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 创建 SFTP 操作异常
     *
     * @param errorCode 错误码
     * @param message   异常消息
     */
    public SftpOperationException(SftpErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 创建 SFTP 操作异常
     *
     * @param errorCode 错误码
     * @param message   异常消息
     * @param cause     原始异常
     */
    public SftpOperationException(SftpErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
```

在文件服务中使用错误码封装传输异常。

```java
throw new SftpOperationException(
        SftpErrorCode.TRANSFER_FAILED,
        "SFTP文件上传失败：" + remotePath,
        e
);
```

对于远程文件不存在的场景，建议使用明确错误码。

```java
if (!session.exists(fullRemotePath)) {
    throw new SftpOperationException(
            SftpErrorCode.REMOTE_PATH_NOT_FOUND,
            "远程文件不存在：" + fullRemotePath
    );
}
```

### 业务异常封装

业务异常封装用于将模块内部异常转换为统一 HTTP 响应，避免 Controller 中反复编写 `try-catch`。对于 SFTP 操作异常，返回模块错误码；对于参数错误，返回 `400`；对于未识别异常，返回 `500`。

该全局异常处理器统一转换接口响应。

文件位置：`src/main/java/io/github/atengk/sftp/handler/GlobalExceptionHandler.java`

```java
package io.github.atengk.sftp.handler;

import cn.hutool.core.exceptions.ExceptionUtil;
import io.github.atengk.sftp.common.ApiResult;
import io.github.atengk.sftp.exception.SftpErrorCode;
import io.github.atengk.sftp.exception.SftpOperationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 SFTP 操作异常
     *
     * @param exception SFTP 操作异常
     * @return 统一响应
     */
    @ExceptionHandler(SftpOperationException.class)
    public ResponseEntity<ApiResult<Void>> handleSftpOperationException(SftpOperationException exception) {
        SftpErrorCode errorCode = exception.getErrorCode();

        log.error("SFTP业务异常，错误码：{}，错误信息：{}，根因：{}",
                errorCode.getCode(), exception.getMessage(), ExceptionUtil.getRootCauseMessage(exception), exception);

        return ResponseEntity
                .status(errorCode.getCode())
                .body(new ApiResult<>(errorCode.getCode(), exception.getMessage(), null));
    }

    /**
     * 处理参数异常
     *
     * @param exception 参数异常
     * @return 统一响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<Void>> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.warn("请求参数异常，错误信息：{}", exception.getMessage());

        return ResponseEntity
                .badRequest()
                .body(new ApiResult<>(400, exception.getMessage(), null));
    }

    /**
     * 处理未知异常
     *
     * @param exception 未知异常
     * @return 统一响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleException(Exception exception) {
        log.error("系统未知异常，根因：{}", ExceptionUtil.getRootCauseMessage(exception), exception);

        return ResponseEntity
                .internalServerError()
                .body(new ApiResult<>(500, "系统内部异常", null));
    }
}
```

异常响应示例：

```json
{
  "code": 404,
  "message": "远程文件不存在：/upload/order/20260507/order.csv",
  "data": null
}
```

业务异常封装注意事项：

1. 接口返回给前端的信息应清晰，但不要暴露服务器内部路径、密码、私钥等敏感信息。
2. 日志中应记录完整异常栈，接口响应中只返回必要错误信息。
3. 文件不存在、参数错误、权限不足、连接失败应使用不同错误码，便于监控和排查。
4. 删除接口建议保持幂等，文件不存在不一定视为异常。
5. 关键业务场景可以额外记录审计日志，例如操作人、业务单号、文件批次号、远程路径。

## 日志与验证

SFTP 模块的日志应覆盖连接初始化、上传、下载、删除、重命名、入站拉取、异常处理和接口调用。验证则应分为接口验证和 SFTP 服务端文件验证两类，确保接口返回成功并不等于最终文件一定正确，还需要通过 SFTP 客户端检查远程目录状态。

### 关键操作日志

关键操作日志用于支撑问题排查、审计追踪和生产监控。日志应做到“能定位问题，但不泄露敏感信息”。

建议日志点如下：

| 操作                  | 日志级别 | 关键字段                       |
| --------------------- | -------- | ------------------------------ |
| SessionFactory 初始化 | `INFO`   | host、port、username、缓存大小 |
| 上传开始              | `INFO`   | 本地文件、远程目录、远程文件名 |
| 上传成功              | `INFO`   | 本地文件、远程路径、文件大小   |
| 上传失败              | `ERROR`  | 本地文件、远程路径、异常栈     |
| 下载成功              | `INFO`   | 远程路径、本地路径、文件大小   |
| 删除完成              | `INFO`   | 远程路径、删除结果             |
| 重命名成功            | `INFO`   | 原路径、新路径                 |
| 入站拉取              | `INFO`   | 远程目录、本地文件、文件大小   |
| 过滤跳过              | `DEBUG`  | 文件名、跳过原因               |
| 参数异常              | `WARN`   | 参数名、错误原因               |
| 系统异常              | `ERROR`  | 操作类型、异常栈               |

日志配置建议如下。

文件位置：`src/main/resources/application.yml`

```yaml
logging:
  level:
    # 当前项目包日志级别
    io.github.atengk.sftp: info

    # Spring Integration 运行日志，排查流程问题时可临时改为 debug
    org.springframework.integration: info

    # Apache SSHD 日志，排查连接认证问题时可临时改为 debug
    org.apache.sshd: warn

  pattern:
    # 控制台日志格式，包含时间、级别、线程、类名和消息
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n"
```

生产排查时可以临时提高日志级别：

```yaml
logging:
  level:
    io.github.atengk.sftp: debug
    org.springframework.integration.sftp: debug
    org.apache.sshd: debug
```

注意：`org.apache.sshd` 的 debug 日志可能较多，只建议在测试环境或短时间生产排障时开启。

### 接口调用验证

接口调用验证用于确认 REST API 到 SFTP 服务之间的链路是否正常。建议按上传、查询、存在判断、下载、删除的顺序验证，避免直接下载或删除不存在的文件。

准备测试文件：

```bash
mkdir -p ./data/sftp/temp
echo "orderId,amount,status" > ./data/sftp/temp/order-20260507.csv
echo "1001,99.90,SUCCESS" >> ./data/sftp/temp/order-20260507.csv
```

命令说明：

| 命令                        | 说明             |
| --------------------------- | ---------------- |
| `mkdir -p ./data/sftp/temp` | 创建本地临时目录 |
| `echo ... > file`           | 写入 CSV 表头    |
| `echo ... >> file`          | 追加 CSV 数据行  |

上传文件：

```bash
curl -X POST "http://127.0.0.1:8080/api/sftp/files/upload" \
  -F "file=@./data/sftp/temp/order-20260507.csv" \
  -F "remoteDir=order/20260507" \
  -F "fileName=order-20260507.csv"
```

预期响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "/upload/order/20260507/order-20260507.csv"
}
```

查询远程目录：

```bash
curl "http://127.0.0.1:8080/api/sftp/files?remoteDir=order/20260507"
```

判断文件是否存在：

```bash
curl "http://127.0.0.1:8080/api/sftp/files/exists?remotePath=/upload/order/20260507/order-20260507.csv"
```

下载文件：

```bash
curl -L -o ./data/sftp/download/order-20260507-download.csv \
  "http://127.0.0.1:8080/api/sftp/files/download?remotePath=/upload/order/20260507/order-20260507.csv"
```

对比下载内容：

```bash
diff ./data/sftp/temp/order-20260507.csv ./data/sftp/download/order-20260507-download.csv
```

删除文件：

```bash
curl -X DELETE \
  "http://127.0.0.1:8080/api/sftp/files?remotePath=/upload/order/20260507/order-20260507.csv"
```

接口验证通过标准如下：

| 验证项       | 通过标准                                   |
| ------------ | ------------------------------------------ |
| 上传接口     | 返回远程文件路径，SFTP 目录存在目标文件    |
| 查询接口     | 返回目标文件名、路径、大小、修改时间       |
| 存在判断接口 | 上传后返回 `true`，删除后返回 `false`      |
| 下载接口     | 下载文件内容与上传文件一致                 |
| 删除接口     | 首次删除返回 `true`，重复删除返回 `false`  |
| 异常响应     | 文件不存在、参数错误时返回明确错误码和消息 |

### SFTP 文件验证

SFTP 文件验证用于直接检查远程服务器上的文件状态。接口调用成功后，应使用 SFTP 客户端确认远程目录、文件名、文件大小和文件内容是否符合预期。

连接本地 SFTP 服务：

```bash
sftp -P 2222 sftpuser@127.0.0.1
```

登录后执行以下命令：

```bash
pwd
ls
cd upload
ls
cd order/20260507
ls -l
get order-20260507.csv
rm order-20260507.csv
bye
```

命令说明：

| 命令         | 说明                   |
| ------------ | ---------------------- |
| `pwd`        | 查看当前远程路径       |
| `ls`         | 查看当前目录文件       |
| `cd upload`  | 进入上传基础目录       |
| `ls -l`      | 查看文件大小和修改时间 |
| `get 文件名` | 下载远程文件到本地     |
| `rm 文件名`  | 删除远程文件           |
| `bye`        | 退出 SFTP 客户端       |

如果使用 Docker 启动本地 SFTP 服务，也可以在宿主机挂载目录中验证：

```bash
find ./docker/sftp/upload -type f -maxdepth 5
ls -l ./docker/sftp/upload/order/20260507
cat ./docker/sftp/upload/order/20260507/order-20260507.csv
```

验证重点如下：

| 验证项           | 检查方式                                    |
| ---------------- | ------------------------------------------- |
| 远程目录是否创建 | `ls` 或 `find`                              |
| 临时文件是否清理 | 确认不存在 `.tmp`、`.writing`、`.part` 文件 |
| 文件大小是否正确 | `ls -l` 对比本地文件大小                    |
| 文件内容是否一致 | `get` 后使用 `diff`                         |
| 删除是否生效     | 删除后再次 `ls` 或调用 exists 接口          |
| 入站拉取是否落盘 | 检查 `./data/sftp/download` 目录            |

## 测试方案

SFTP 测试建议分为单元测试、集成测试和异常场景测试。单元测试关注路径拼接、参数校验、业务分支和异常封装；集成测试关注真实 SFTP 服务上的上传、下载、查询、删除；异常场景测试关注连接失败、文件不存在、权限不足、重复文件、临时文件过滤等问题。

Spring Integration 官方提供 `spring-integration-test` 测试支持，`@SpringIntegrationTest` 会自动注册 `MockIntegrationContext`，可用于在测试中替换真实消息源或消息处理器。([Home](https://docs.spring.io/spring-integration/api/org/springframework/integration/test/context/MockIntegrationContext.html?utm_source=chatgpt.com))

### 单元测试

单元测试不依赖真实 SFTP 服务，主要验证工具类、参数校验、异常封装和 Service 分支逻辑。对于 `SftpRemoteFileTemplate`，可以使用 Mockito 模拟 `execute(...)` 回调和 `Session` 行为。

测试依赖建议如下。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot 测试基础依赖，包含 JUnit 5、Mockito、AssertJ 等 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Spring Integration 测试支持 -->
    <dependency>
        <groupId>org.springframework.integration</groupId>
        <artifactId>spring-integration-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

路径工具单元测试如下。

文件位置：`src/test/java/io/github/atengk/sftp/core/SftpPathHelperTest.java`

```java
package io.github.atengk.sftp.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SFTP 路径工具测试
 *
 * @author Ateng
 * @since 2026-05-07
 */
class SftpPathHelperTest {

    /**
     * 测试远程路径拼接
     */
    @Test
    void shouldJoinRemotePath() {
        String path = SftpPathHelper.joinRemotePath("/upload/", "/order/", "20260507", "order.csv");

        assertThat(path).isEqualTo("/upload/order/20260507/order.csv");
    }

    /**
     * 测试相对路径解析
     */
    @Test
    void shouldResolveRelativeRemotePath() {
        String path = SftpPathHelper.resolveRemotePath("/upload", "order/20260507/order.csv");

        assertThat(path).isEqualTo("/upload/order/20260507/order.csv");
    }

    /**
     * 测试绝对路径解析
     */
    @Test
    void shouldResolveAbsoluteRemotePath() {
        String path = SftpPathHelper.resolveRemotePath("/upload", "/archive/order.csv");

        assertThat(path).isEqualTo("/archive/order.csv");
    }

    /**
     * 测试非法文件名
     */
    @Test
    void shouldRejectInvalidFileName() {
        assertThatThrownBy(() -> SftpPathHelper.validateFileName("../order.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("文件名不能包含路径分隔符");
    }
}
```

文件服务上传逻辑单元测试如下。

文件位置：`src/test/java/io/github/atengk/sftp/service/impl/SftpFileServiceImplTest.java`

```java
package io.github.atengk.sftp.service.impl;

import io.github.atengk.sftp.config.SftpProperties;
import io.github.atengk.sftp.service.SftpFileService;
import org.apache.sshd.sftp.client.SftpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.integration.file.remote.SessionCallback;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * SFTP 文件服务单元测试
 *
 * @author Ateng
 * @since 2026-05-07
 */
class SftpFileServiceImplTest {

    private SftpRemoteFileTemplate sftpRemoteFileTemplate;

    private Session<SftpClient.DirEntry> session;

    private SftpFileService sftpFileService;

    /**
     * 初始化测试对象
     */
    @BeforeEach
    void setUp() {
        sftpRemoteFileTemplate = mock(SftpRemoteFileTemplate.class);
        session = mock(Session.class);

        SftpProperties properties = new SftpProperties();
        properties.setRemoteBaseDir("/upload");
        properties.setTemporarySuffix(".tmp");
        properties.setAutoCreateRemoteDir(true);

        sftpFileService = new SftpFileServiceImpl(sftpRemoteFileTemplate, properties);
    }

    /**
     * 测试上传成功
     *
     * @throws Exception 测试异常
     */
    @Test
    void shouldUploadFile() throws Exception {
        Path localFile = Files.createTempFile("sftp-upload-", ".txt");
        Files.writeString(localFile, "hello sftp");

        when(session.exists("/upload")).thenReturn(true);
        when(session.exists("/upload/test")).thenReturn(false);
        when(session.exists("/upload/test/demo.txt.tmp")).thenReturn(false);
        when(session.exists("/upload/test/demo.txt")).thenReturn(false);

        when(sftpRemoteFileTemplate.execute(any())).thenAnswer(invocation -> {
            SessionCallback<SftpClient.DirEntry, Boolean> callback = invocation.getArgument(0);
            return callback.doInSession(session);
        });

        String remotePath = sftpFileService.upload(localFile, "test", "demo.txt");

        assertThat(remotePath).isEqualTo("/upload/test/demo.txt");
        verify(session).mkdir("/upload/test");
        verify(session).write(any(InputStream.class), eq("/upload/test/demo.txt.tmp"));
        verify(session).rename("/upload/test/demo.txt.tmp", "/upload/test/demo.txt");
    }

    /**
     * 测试本地文件不存在
     */
    @Test
    void shouldRejectMissingLocalFile() {
        Path missingFile = Path.of("./not-exists.txt");

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        sftpFileService.upload(missingFile, "test", "demo.txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("本地文件不存在");
    }
}
```

单元测试执行命令：

```bash
mvn test -Dtest=SftpPathHelperTest,SftpFileServiceImplTest
```

命令说明：

| 参数         | 说明                |
| ------------ | ------------------- |
| `mvn test`   | 执行 Maven 测试阶段 |
| `-Dtest=...` | 指定要运行的测试类  |

### 集成测试

集成测试应连接真实 SFTP 服务，验证上传、下载、存在判断和删除的完整链路。可以使用本地 Docker Compose 启动 SFTP，也可以使用 Testcontainers 在测试过程中自动启动临时 SFTP 容器。

如果使用 Testcontainers，增加以下测试依赖。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Testcontainers JUnit 5 支持，用于集成测试自动启动 Docker 容器 -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

集成测试代码如下。

文件位置：`src/test/java/io/github/atengk/sftp/SftpFileServiceIntegrationTest.java`

```java
package io.github.atengk.sftp;

import io.github.atengk.sftp.service.SftpFileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SFTP 文件服务集成测试
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Testcontainers
@SpringBootTest
class SftpFileServiceIntegrationTest {

    @Container
    static final GenericContainer<?> SFTP_CONTAINER = new GenericContainer<>("atmoz/sftp:latest")
            .withExposedPorts(22)
            .withCommand("sftpuser:123456:1001:100:upload");

    @Autowired
    private SftpFileService sftpFileService;

    /**
     * 动态注入 SFTP 测试容器配置
     *
     * @param registry 动态配置注册器
     */
    @DynamicPropertySource
    static void registerSftpProperties(DynamicPropertyRegistry registry) {
        registry.add("sftp.host", SFTP_CONTAINER::getHost);
        registry.add("sftp.port", () -> SFTP_CONTAINER.getMappedPort(22));
        registry.add("sftp.username", () -> "sftpuser");
        registry.add("sftp.password", () -> "123456");
        registry.add("sftp.allow-unknown-keys", () -> true);
        registry.add("sftp.remote-base-dir", () -> "/upload");
        registry.add("sftp.local-download-dir", () -> "./target/sftp/download");
        registry.add("sftp.local-temp-dir", () -> "./target/sftp/temp");
        registry.add("sftp.temporary-suffix", () -> ".tmp");
        registry.add("sftp.auto-create-remote-dir", () -> true);
    }

    /**
     * 测试上传、存在判断、下载和删除完整链路
     *
     * @throws Exception 测试异常
     */
    @Test
    void shouldUploadDownloadAndDeleteFile() throws Exception {
        Path localFile = Files.createTempFile("sftp-it-", ".txt");
        Files.writeString(localFile, "hello sftp integration", StandardCharsets.UTF_8);

        String remotePath = sftpFileService.upload(localFile, "integration", "demo.txt");
        assertThat(remotePath).isEqualTo("/upload/integration/demo.txt");

        boolean exists = sftpFileService.exists(remotePath);
        assertThat(exists).isTrue();

        byte[] bytes = sftpFileService.downloadBytes(remotePath);
        assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo("hello sftp integration");

        boolean deleted = sftpFileService.delete(remotePath);
        assertThat(deleted).isTrue();

        boolean existsAfterDelete = sftpFileService.exists(remotePath);
        assertThat(existsAfterDelete).isFalse();
    }
}
```

集成测试执行命令：

```bash
mvn test -Dtest=SftpFileServiceIntegrationTest
```

集成测试通过标准：

| 验证项     | 通过标准                           |
| ---------- | ---------------------------------- |
| 容器启动   | SFTP 容器正常启动并映射端口        |
| 上传       | 远程路径返回正确，文件存在         |
| 下载       | 下载内容与上传内容一致             |
| 删除       | 删除返回 `true`                    |
| 删除后检查 | 文件存在判断返回 `false`           |
| 日志       | 无连接失败、认证失败、权限不足异常 |

### 异常场景测试

异常场景测试用于确认系统在失败情况下返回可预期的错误，而不是抛出未处理异常或返回模糊的 500 错误。重点覆盖连接失败、远程文件不存在、本地文件不存在、非法文件名、重复删除和目标文件已存在等场景。

建议覆盖的异常用例如下：

| 测试场景       | 输入                 | 预期结果                              |
| -------------- | -------------------- | ------------------------------------- |
| 本地文件不存在 | 上传不存在的本地路径 | 抛出参数异常或 `LOCAL_FILE_NOT_FOUND` |
| 非法文件名     | `../demo.txt`        | 抛出参数异常                          |
| 远程文件不存在 | 下载不存在文件       | 返回 `REMOTE_PATH_NOT_FOUND`          |
| 重复删除       | 删除已删除文件       | 第二次返回 `false`                    |
| 目标文件已存在 | 重命名到已有路径     | 返回 `FILE_ALREADY_EXISTS`            |
| 连接失败       | 错误 host 或 port    | 返回 `CONNECT_FAILED` 或传输失败      |
| 认证失败       | 错误密码             | 返回连接失败，日志记录认证异常        |
| 权限不足       | 访问无权限目录       | 返回 `PERMISSION_DENIED` 或传输失败   |

异常测试示例代码如下。

文件位置：`src/test/java/io/github/atengk/sftp/SftpExceptionScenarioTest.java`

```java
package io.github.atengk.sftp;

import io.github.atengk.sftp.exception.SftpOperationException;
import io.github.atengk.sftp.service.SftpFileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SFTP 异常场景测试
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Testcontainers
@SpringBootTest
class SftpExceptionScenarioTest {

    @Container
    static final GenericContainer<?> SFTP_CONTAINER = new GenericContainer<>("atmoz/sftp:latest")
            .withExposedPorts(22)
            .withCommand("sftpuser:123456:1001:100:upload");

    @Autowired
    private SftpFileService sftpFileService;

    /**
     * 动态注入 SFTP 测试配置
     *
     * @param registry 动态配置注册器
     */
    @DynamicPropertySource
    static void registerSftpProperties(DynamicPropertyRegistry registry) {
        registry.add("sftp.host", SFTP_CONTAINER::getHost);
        registry.add("sftp.port", () -> SFTP_CONTAINER.getMappedPort(22));
        registry.add("sftp.username", () -> "sftpuser");
        registry.add("sftp.password", () -> "123456");
        registry.add("sftp.allow-unknown-keys", () -> true);
        registry.add("sftp.remote-base-dir", () -> "/upload");
        registry.add("sftp.local-download-dir", () -> "./target/sftp/download");
        registry.add("sftp.local-temp-dir", () -> "./target/sftp/temp");
    }

    /**
     * 测试上传不存在的本地文件
     */
    @Test
    void shouldRejectMissingLocalFileWhenUpload() {
        assertThatThrownBy(() ->
                sftpFileService.upload(Path.of("./not-exists.csv"), "exception", "demo.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("本地文件不存在");
    }

    /**
     * 测试下载不存在的远程文件
     */
    @Test
    void shouldThrowExceptionWhenRemoteFileNotFound() {
        assertThatThrownBy(() ->
                sftpFileService.downloadBytes("/upload/not-exists.csv"))
                .isInstanceOf(SftpOperationException.class)
                .hasMessageContaining("远程文件不存在");
    }

    /**
     * 测试重复删除
     */
    @Test
    void shouldReturnFalseWhenDeleteMissingRemoteFile() {
        boolean deleted = sftpFileService.delete("/upload/missing-delete.csv");

        assertThat(deleted).isFalse();
    }

    /**
     * 测试非法文件名
     */
    @Test
    void shouldRejectInvalidRemoteFileName() {
        assertThatThrownBy(() ->
                sftpFileService.upload(Path.of("./pom.xml"), "exception", "../demo.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("文件名不能包含路径分隔符");
    }
}
```

异常场景接口验证示例：

```bash
# 下载不存在的远程文件
curl -i "http://127.0.0.1:8080/api/sftp/files/download?remotePath=/upload/not-exists.csv"

# 删除不存在的远程文件，预期 data=false
curl -i -X DELETE "http://127.0.0.1:8080/api/sftp/files?remotePath=/upload/not-exists.csv"

# 上传时传入非法文件名，预期返回 400
curl -i -X POST "http://127.0.0.1:8080/api/sftp/files/upload" \
  -F "file=@./pom.xml" \
  -F "remoteDir=order" \
  -F "fileName=../demo.csv"
```

测试方案注意事项：

1. 单元测试不应依赖真实 SFTP 服务，应使用 Mockito 模拟远程 Session 行为。
2. 集成测试应使用独立测试目录，避免污染真实业务 SFTP 目录。
3. 集成测试不要连接生产 SFTP 服务。
4. 大文件测试建议单独执行，不要放入普通单元测试流水线。
5. 入站拉取流程是轮询消费者，测试时要控制轮询间隔和最大拉取数量。Spring Integration 官方文档说明，SFTP 入站适配器是 polling consumer，文件同步到本地后会发送 `java.io.File` 类型消息到通道。([Home](https://docs.spring.io/spring-integration/reference/7.0/sftp/inbound.html?utm_source=chatgpt.com))
6. 文件过滤和失败恢复需要单独测试，尤其是临时后缀文件、重复文件和处理失败文件。Spring Integration 文档说明，入站流程涉及远程过滤和本地过滤，持久化过滤器可基于 `MetadataStore` 记录已接收文件状态；传输期间的临时文件默认不会被本地过滤器处理。([Home](https://docs.spring.io/spring-integration/reference/7.0/sftp/inbound.html?utm_source=chatgpt.com))
