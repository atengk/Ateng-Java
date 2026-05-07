# Apache MINA SSHD

本文档用于说明在 Spring Boot 3 项目中集成 Apache MINA SSHD 的基础设计方式，重点覆盖模块定位、典型场景、技术选型、依赖配置和项目结构。后续章节可继续基于该基础展开 SSH 服务端启动、认证、命令处理、SFTP 文件服务和运行部署等实现内容。当前内容基于你提供的开发大纲补齐。

## 模块概述

本模块基于 Apache MINA SSHD 在 Spring Boot 3 应用内嵌一个 SSH/SFTP 服务端，使业务系统可以通过标准 SSH 协议提供命令通道、文件传输通道和受控远程运维能力。模块不依赖系统级 `sshd` 服务，而是由 Java 应用进程自行启动、监听、认证和管理会话。

### 功能定位

Apache MINA SSHD 模块的核心定位是“应用内嵌式 SSH 服务能力”。它适合在业务系统内部暴露受控的 SSH 命令入口、SFTP 文件入口或自动化运维入口，而不是直接替代 Linux 操作系统自带的 OpenSSH Server。

在 Spring Boot 3 项目中，该模块通常承担以下职责：

| 功能          | 说明                                                         |
| ------------- | ------------------------------------------------------------ |
| SSH 服务监听  | 在应用启动后监听指定端口，例如 `2222`，接收 SSH 客户端连接   |
| 用户认证      | 支持用户名密码认证、公钥认证，后续可扩展到数据库、Redis 或外部 IAM |
| 命令处理      | 将 SSH 客户端输入的命令解析为应用内指令，例如状态查询、任务触发、文件操作 |
| SFTP 文件服务 | 提供标准 SFTP 上传、下载、删除、目录浏览能力                 |
| 会话管理      | 记录在线连接、登录用户、客户端地址、连接时间和关闭原因       |
| 权限控制      | 根据用户、角色、命令、目录范围控制可访问能力                 |
| 审计日志      | 记录登录、认证失败、命令执行、文件上传下载等关键行为         |

该模块的边界需要明确：它负责提供协议接入、认证授权、命令分发和文件服务能力；具体业务逻辑仍应下沉到 Spring Service 层，不建议直接写在 SSH 命令处理类中。

### 使用场景

Apache MINA SSHD 适用于需要“通过 SSH 协议接入应用能力”的场景。由于 SSH/SFTP 是标准协议，客户端可以直接使用 `ssh`、`scp`、`sftp`、WinSCP、FileZilla、JSch、Apache SSHD Client 等工具或 SDK 接入。

常见使用场景如下：

| 场景               | 示例                                                         |
| ------------------ | ------------------------------------------------------------ |
| 应用内运维命令     | 通过 SSH 执行 `status`、`reload`、`metrics`、`job start` 等受控命令 |
| 自动化任务入口     | CI/CD 或运维脚本通过 SSH 登录系统并触发内部任务              |
| 文件上传下载       | 使用 SFTP 上传数据文件、下载结果文件、交换报表或日志         |
| 租户文件隔离       | 不同用户登录后只能访问自己的根目录                           |
| 设备或边缘节点接入 | 设备通过 SSH/SFTP 上传采集文件或接收配置文件                 |
| 安全审计场景       | 所有 SSH 登录、认证失败、命令执行、文件操作均进入业务审计日志 |

不建议将该模块用于完全开放的公网远程 Shell 场景。若需要公网暴露，应增加网络白名单、限流、防暴力破解、强密码策略、公钥认证、审计告警和最小权限控制。

### 技术选型

本文以 Spring Boot 3.x 和 Apache MINA SSHD 2.x 稳定版本线为基础。Spring Boot 3 要求 Java 17 起步；Spring 官方的 Spring Boot 3.3 系统要求文档明确要求至少 Java 17，并基于 Spring Framework 6.x 运行。([Spring Enterprise Docs](https://docs.enterprise.spring.io/spring-boot/system-requirements.html?utm_source=chatgpt.com))

Apache MINA SSHD 选择 2.17.1 作为示例稳定版本。Maven Central 中 `sshd-mina` 和 `sshd-sftp` 均已发布 2.17.1；同时 3.0.0 系列仍存在 milestone 版本，且 Apache 发布说明中明确提示 3.x 与 2.x API 不兼容，因此生产文档示例优先采用 2.x 稳定线。([Maven Repository](https://repo.maven.apache.org/maven2/org/apache/sshd/sshd-mina/?utm_source=chatgpt.com))

| 技术                 | 推荐版本   | 说明                                                |
| -------------------- | ---------- | --------------------------------------------------- |
| JDK                  | 17+        | Spring Boot 3 基线要求，建议生产使用 JDK 17 或 21   |
| Spring Boot          | 3.x        | 本文档限定 Spring Boot 3，不使用 Spring Boot 4 示例 |
| Apache MINA SSHD     | 2.17.1     | 稳定版本线，避免 3.x milestone API 变更风险         |
| Maven                | 3.8+       | 用于依赖管理和构建打包                              |
| Lombok               | 可选       | 简化配置类、DTO、日志类代码                         |
| Hutool               | 可选但推荐 | 用于字符串、集合、文件路径等通用处理                |
| Spring Boot Actuator | 可选       | 用于暴露 SSH 服务状态、健康检查和运行指标           |

## 环境与依赖

本节定义项目的基础运行环境、Maven 依赖和目录结构。后续 SSH 服务启动、认证、命令处理、SFTP 子系统、会话管理和接口管理章节均基于这里的配置展开。

### Spring Boot 版本说明

本模块面向 Spring Boot 3.x 开发，建议使用 JDK 17 或 JDK 21。JDK 17 是 Spring Boot 3 的最低运行要求，JDK 21 更适合长期维护版本项目。

推荐基础环境如下：

| 项目             | 建议值                | 说明                                           |
| ---------------- | --------------------- | ---------------------------------------------- |
| Java             | 17 或 21              | Spring Boot 3 至少要求 Java 17                 |
| Spring Boot      | 3.3.x / 3.4.x / 3.5.x | 根据项目统一版本选择，不建议混用 Spring Boot 4 |
| 构建工具         | Maven 3.8+            | 与企业常用 Spring Boot 项目兼容                |
| 打包方式         | Jar                   | SSH 服务随 Spring Boot 应用进程启动            |
| 默认 SSH 端口    | 2222                  | 避免与系统 OpenSSH 默认 `22` 端口冲突          |
| 默认 SFTP 根目录 | `/data/app/sshd`      | 生产环境应使用独立数据目录                     |

版本选择建议：

1. 新项目优先选择公司统一的 Spring Boot 3.x 稳定版本，不建议单独为 SSH 模块升级 Spring Boot 主版本。
2. Apache MINA SSHD 的版本建议在父工程或 `dependencyManagement` 中统一管理。
3. 若后续升级到 Apache MINA SSHD 3.x，需要单独验证 API 兼容性、认证接口、SFTP 子系统配置和会话生命周期处理逻辑。
4. SSH 服务端口应与应用 HTTP 端口分离，例如 HTTP 使用 `8080`，SSH 使用 `2222`。
5. 生产环境应优先启用公钥认证，并限制用户名密码认证的使用范围。

### Apache MINA SSHD 依赖配置

Maven 依赖建议集中放在业务服务的 `pom.xml` 中。如果项目是多模块结构，可以将版本号放到父工程的 `dependencyManagement`，具体业务模块只声明依赖。

文件位置：`pom.xml`

```xml
<properties>
    <!-- Spring Boot 3 项目建议使用 Java 17 或 21 -->
    <java.version>17</java.version>

    <!-- Apache MINA SSHD 稳定版本线，避免直接使用 3.x milestone -->
    <apache.sshd.version>2.17.1</apache.sshd.version>

    <!-- Hutool 用于字符串、集合、文件路径等通用处理 -->
    <hutool.version>5.8.35</hutool.version>
</properties>

<dependencies>
    <!-- Spring Boot Web：用于提供 SSH 服务管理接口，例如状态查询、会话查询、用户配置管理 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot Validation：用于配置属性、接口参数和用户配置校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Spring Boot Actuator：用于暴露应用健康检查和运行指标，生产环境可按需启用 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Apache MINA SSHD 核心服务端能力 -->
    <dependency>
        <groupId>org.apache.sshd</groupId>
        <artifactId>sshd-core</artifactId>
        <version>${apache.sshd.version}</version>
    </dependency>

    <!-- Apache MINA SSHD 基于 MINA 的传输实现 -->
    <dependency>
        <groupId>org.apache.sshd</groupId>
        <artifactId>sshd-mina</artifactId>
        <version>${apache.sshd.version}</version>
    </dependency>

    <!-- Apache MINA SSHD SFTP 子系统，用于文件上传、下载和目录操作 -->
    <dependency>
        <groupId>org.apache.sshd</groupId>
        <artifactId>sshd-sftp</artifactId>
        <version>${apache.sshd.version}</version>
    </dependency>

    <!-- Hutool 工具包：用于路径、字符串、集合、文件等通用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok：简化配置类、DTO、日志类代码，编译期生效 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot Test：用于服务启动、认证、命令执行和 SFTP 功能测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目使用父工程统一管理依赖版本，可以将 Apache MINA SSHD 版本放入父工程：

文件位置：`pom.xml`

```xml
<dependencyManagement>
    <dependencies>
        <!-- 统一管理 Apache MINA SSHD 版本，避免多模块版本不一致 -->
        <dependency>
            <groupId>org.apache.sshd</groupId>
            <artifactId>sshd-core</artifactId>
            <version>${apache.sshd.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.sshd</groupId>
            <artifactId>sshd-mina</artifactId>
            <version>${apache.sshd.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.sshd</groupId>
            <artifactId>sshd-sftp</artifactId>
            <version>${apache.sshd.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

对应的基础配置建议放在 `application.yml` 中，后续通过 `@ConfigurationProperties` 绑定为配置类。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080 # Spring Boot HTTP 管理接口端口

app:
  sshd:
    enabled: true # 是否启用内嵌 SSH 服务
    host: 0.0.0.0 # SSH 服务监听地址，生产环境可按需绑定内网 IP
    port: 2222 # SSH 服务端口，避免与系统 22 端口冲突
    host-key-path: ./data/sshd/hostkey.ser # 服务端主机密钥保存路径，首次启动可自动生成
    sftp-root-path: ./data/sshd/sftp # SFTP 文件根目录
    idle-timeout: 600s # 会话空闲超时时间
    auth:
      password-enabled: true # 是否启用用户名密码认证
      public-key-enabled: true # 是否启用公钥认证
      max-auth-failures: 5 # 单个连接最大认证失败次数
    audit:
      enabled: true # 是否记录 SSH 登录、命令、SFTP 操作审计日志
```

配置项说明如下：

| 配置项                             | 说明                        |
| ---------------------------------- | --------------------------- |
| `app.sshd.enabled`                 | 控制 SSH 服务是否随应用启动 |
| `app.sshd.host`                    | SSH 服务监听地址            |
| `app.sshd.port`                    | SSH 服务监听端口            |
| `app.sshd.host-key-path`           | SSH 服务端主机密钥保存路径  |
| `app.sshd.sftp-root-path`          | SFTP 根目录                 |
| `app.sshd.idle-timeout`            | SSH 会话空闲超时时间        |
| `app.sshd.auth.password-enabled`   | 是否开启用户名密码认证      |
| `app.sshd.auth.public-key-enabled` | 是否开启公钥认证            |
| `app.sshd.auth.max-auth-failures`  | 最大认证失败次数            |
| `app.sshd.audit.enabled`           | 是否开启审计日志            |

### 项目目录结构

项目目录建议按“配置、服务、认证、命令、SFTP、会话、管理接口、审计日志”进行拆分。SSH 协议接入代码应与业务 Service 解耦，避免命令处理类直接堆积业务逻辑。

推荐目录结构如下：

```text
springboot-mina-sshd/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── io/
│   │   │       └── github/
│   │   │           └── atengk/
│   │   │               └── sshd/
│   │   │                   ├── MinaSshdApplication.java
│   │   │                   ├── config/
│   │   │                   │   ├── SshdAutoConfiguration.java
│   │   │                   │   └── SshdProperties.java
│   │   │                   ├── server/
│   │   │                   │   ├── SshServerLifecycle.java
│   │   │                   │   └── SshServerFactory.java
│   │   │                   ├── auth/
│   │   │                   │   ├── PasswordAuthenticatorImpl.java
│   │   │                   │   ├── PublicKeyAuthenticatorImpl.java
│   │   │                   │   └── SshUserAccessService.java
│   │   │                   ├── command/
│   │   │                   │   ├── AppCommandFactory.java
│   │   │                   │   ├── AppCommand.java
│   │   │                   │   ├── CommandContext.java
│   │   │                   │   └── handler/
│   │   │                   │       ├── StatusCommandHandler.java
│   │   │                   │       └── HelpCommandHandler.java
│   │   │                   ├── sftp/
│   │   │                   │   ├── SftpSubsystemConfiguration.java
│   │   │                   │   └── UserRootFileSystemFactory.java
│   │   │                   ├── session/
│   │   │                   │   ├── SshSessionRegistry.java
│   │   │                   │   └── SshSessionInfo.java
│   │   │                   ├── audit/
│   │   │                   │   ├── SshAuditService.java
│   │   │                   │   └── SshAuditEvent.java
│   │   │                   └── controller/
│   │   │                       ├── SshServerController.java
│   │   │                       └── SshSessionController.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── logback-spring.xml
│   └── test/
│       └── java/
│           └── io/
│               └── github/
│                   └── atengk/
│                       └── sshd/
│                           ├── SshServerLifecycleTest.java
│                           └── SftpFunctionTest.java
└── data/
    └── sshd/
        ├── hostkey.ser
        └── sftp/
```

核心目录说明如下：

| 目录         | 说明                                                     |
| ------------ | -------------------------------------------------------- |
| `config`     | 保存 SSH 模块配置类、自动装配类、属性绑定类              |
| `server`     | 负责 SSH 服务端创建、启动、停止和生命周期控制            |
| `auth`       | 负责用户名密码认证、公钥认证和用户权限查询               |
| `command`    | 负责 SSH 命令解析、命令上下文封装和命令处理器分发        |
| `sftp`       | 负责 SFTP 子系统配置、用户根目录隔离和文件系统工厂       |
| `session`    | 负责在线会话登记、查询、关闭和会话状态维护               |
| `audit`      | 负责登录、认证、命令、文件操作等审计事件记录             |
| `controller` | 提供 HTTP 管理接口，例如服务状态、在线会话和用户访问配置 |
| `resources`  | 保存应用配置和日志配置                                   |
| `data/sshd`  | 本地开发环境的 SSH 主机密钥和 SFTP 文件根目录            |

该目录结构的设计原则是：协议层只处理 SSH/SFTP 连接和输入输出，认证层只处理身份与权限，命令层只做解析和分发，业务逻辑放入独立 Service。这样后续无论是增加新命令、接入数据库用户体系，还是扩展 SFTP 权限模型，都不会破坏 SSH 服务端基础结构。

## SSH 服务端设计

SSH 服务端设计负责完成内嵌 SSH Server 的创建、配置、启动、停止和会话监听。该部分建议独立放在 `server` 包中，不要将认证、命令处理和 SFTP 逻辑直接写入启动类，避免后续扩展困难。本节继续基于前面的开发大纲展开。

### 服务端启动流程

服务端启动流程建议交给 Spring 生命周期托管。应用启动后，由 `SmartLifecycle` 触发 SSH Server 初始化；应用关闭时，统一停止 SSH Server 并释放端口、会话和底层 I/O 资源。

推荐启动流程如下：

```text
Spring Boot 启动
  ↓
加载 app.sshd 配置
  ↓
初始化 SshServer
  ↓
配置监听地址、端口、主机密钥
  ↓
配置认证器：密码认证、公钥认证
  ↓
配置命令工厂：CommandFactory
  ↓
注册会话监听器：SessionListener
  ↓
启动 SSH Server
  ↓
接收 SSH 客户端连接
```

文件位置：`src/main/java/io/github/atengk/sshd/config/SshdProperties.java`

下面的配置类用于绑定 `application.yml` 中的 `app.sshd` 配置。

```java
package io.github.atengk.sshd.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * SSH 服务配置属性
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Validated
@ConfigurationProperties(prefix = "app.sshd")
public class SshdProperties {

    /**
     * 是否启用 SSH 服务
     */
    private boolean enabled = true;

    /**
     * 监听地址
     */
    @NotBlank(message = "SSH监听地址不能为空")
    private String host = "0.0.0.0";

    /**
     * 监听端口
     */
    @Min(value = 1, message = "SSH端口必须大于0")
    private int port = 2222;

    /**
     * 主机密钥路径
     */
    @NotBlank(message = "SSH主机密钥路径不能为空")
    private String hostKeyPath = "./data/sshd/hostkey.ser";

    /**
     * SFTP 根目录
     */
    @NotBlank(message = "SFTP根目录不能为空")
    private String sftpRootPath = "./data/sshd/sftp";

    /**
     * 会话空闲超时时间
     */
    @NotNull(message = "SSH空闲超时时间不能为空")
    private Duration idleTimeout = Duration.ofMinutes(10);

    /**
     * 认证配置
     */
    @Valid
    private Auth auth = new Auth();

    /**
     * SSH 认证配置
     *
     * @author Ateng
     * @since 2026-05-07
     */
    @Data
    public static class Auth {

        /**
         * 是否启用用户名密码认证
         */
        private boolean passwordEnabled = true;

        /**
         * 是否启用公钥认证
         */
        private boolean publicKeyEnabled = true;

        /**
         * 最大认证失败次数
         */
        @Min(value = 1, message = "最大认证失败次数必须大于0")
        private int maxAuthFailures = 5;
    }
}
```

文件位置：`src/main/java/io/github/atengk/sshd/config/SshdAutoConfiguration.java`

下面的自动配置类用于启用 SSH 配置属性，并将 SSH 服务相关 Bean 纳入 Spring 容器。

```java
package io.github.atengk.sshd.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * SSH 自动配置类
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Configuration
@EnableConfigurationProperties(SshdProperties.class)
public class SshdAutoConfiguration {
}
```

文件位置：`src/main/java/io/github/atengk/sshd/server/SshServerFactory.java`

下面的工厂类用于创建并配置 Apache MINA SSHD 服务端实例。

```java
package io.github.atengk.sshd.server;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.sshd.auth.PasswordAuthenticatorImpl;
import io.github.atengk.sshd.auth.PublicKeyAuthenticatorImpl;
import io.github.atengk.sshd.command.AppCommandFactory;
import io.github.atengk.sshd.config.SshdProperties;
import io.github.atengk.sshd.session.SshSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.core.CoreModuleProperties;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * SSH 服务端工厂
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SshServerFactory {

    private final SshdProperties sshdProperties;
    private final PasswordAuthenticatorImpl passwordAuthenticator;
    private final PublicKeyAuthenticatorImpl publicKeyAuthenticator;
    private final AppCommandFactory appCommandFactory;
    private final SshSessionRegistry sshSessionRegistry;

    /**
     * 创建 SSH 服务端实例
     *
     * @return SSH 服务端
     */
    public SshServer create() {
        prepareDirectory();

        SshServer sshServer = SshServer.setUpDefaultServer();
        sshServer.setHost(sshdProperties.getHost());
        sshServer.setPort(sshdProperties.getPort());
        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(FileUtil.file(sshdProperties.getHostKeyPath()).toPath()));
        sshServer.setPasswordAuthenticator(passwordAuthenticator);
        sshServer.setPublickeyAuthenticator(publicKeyAuthenticator);
        sshServer.setCommandFactory(appCommandFactory);
        sshServer.addSessionListener(sshSessionRegistry);

        CoreModuleProperties.IDLE_TIMEOUT.set(sshServer, sshdProperties.getIdleTimeout());

        log.info("SSH服务端初始化完成，监听地址：{}，监听端口：{}", sshdProperties.getHost(), sshdProperties.getPort());
        return sshServer;
    }

    /**
     * 准备 SSH 服务运行目录
     */
    private void prepareDirectory() {
        File hostKeyFile = FileUtil.file(sshdProperties.getHostKeyPath());
        File hostKeyParent = hostKeyFile.getParentFile();
        if (ObjectUtil.isNotNull(hostKeyParent)) {
            FileUtil.mkdir(hostKeyParent);
        }

        FileUtil.mkdir(sshdProperties.getSftpRootPath());
        log.info("SSH运行目录准备完成，主机密钥路径：{}，SFTP根目录：{}",
                sshdProperties.getHostKeyPath(), sshdProperties.getSftpRootPath());
    }
}
```

文件位置：`src/main/java/io/github/atengk/sshd/server/SshServerLifecycle.java`

下面的生命周期类用于跟随 Spring Boot 应用启动和关闭 SSH 服务。

```java
package io.github.atengk.sshd.server;

import io.github.atengk.sshd.config.SshdProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.server.SshServer;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SSH 服务生命周期管理器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SshServerLifecycle implements SmartLifecycle {

    private final SshdProperties sshdProperties;
    private final SshServerFactory sshServerFactory;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Getter
    private SshServer sshServer;

    /**
     * 启动 SSH 服务
     */
    @Override
    public void start() {
        if (!sshdProperties.isEnabled()) {
            log.info("SSH服务未启用，跳过启动");
            return;
        }

        if (!running.compareAndSet(false, true)) {
            log.info("SSH服务已经处于运行状态，跳过重复启动");
            return;
        }

        try {
            this.sshServer = sshServerFactory.create();
            this.sshServer.start();
            log.info("SSH服务启动成功，监听地址：{}，监听端口：{}", sshdProperties.getHost(), sshdProperties.getPort());
        } catch (IOException e) {
            running.set(false);
            log.error("SSH服务启动失败，监听端口：{}", sshdProperties.getPort(), e);
            throw new IllegalStateException("SSH服务启动失败", e);
        }
    }

    /**
     * 停止 SSH 服务
     */
    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        if (this.sshServer == null) {
            return;
        }

        try {
            this.sshServer.stop(true);
            log.info("SSH服务停止成功");
        } catch (IOException e) {
            log.error("SSH服务停止异常", e);
            throw new IllegalStateException("SSH服务停止异常", e);
        }
    }

    /**
     * 停止 SSH 服务并执行回调
     *
     * @param callback 停止完成回调
     */
    @Override
    public void stop(Runnable callback) {
        try {
            stop();
        } finally {
            callback.run();
        }
    }

    /**
     * 判断 SSH 服务是否运行
     *
     * @return 是否运行
     */
    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 是否随 Spring 容器自动启动
     *
     * @return 是否自动启动
     */
    @Override
    public boolean isAutoStartup() {
        return true;
    }

    /**
     * 启动阶段，值越小越早启动
     *
     * @return 生命周期阶段
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
```

### 端口与主机密钥配置

SSH 服务端口应与 Spring Boot HTTP 端口分离。开发环境推荐使用 `2222`，生产环境建议根据安全策略指定固定端口，并通过防火墙或安全组限制访问来源。

主机密钥用于标识 SSH 服务端身份。首次启动时，`SimpleGeneratorHostKeyProvider` 会根据指定路径生成服务端密钥文件；后续启动时复用该文件。生产环境不建议频繁删除或重新生成主机密钥，否则客户端会出现主机指纹变化警告。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080 # HTTP管理接口端口

app:
  sshd:
    enabled: true # 是否启用内嵌SSH服务
    host: 0.0.0.0 # 监听所有网卡，生产环境可改为内网IP
    port: 2222 # SSH服务端口，避免占用系统22端口
    host-key-path: /data/app/sshd/hostkey.ser # 服务端主机密钥文件
    sftp-root-path: /data/app/sshd/sftp # SFTP文件根目录
    idle-timeout: 600s # 会话空闲超时时间
    auth:
      password-enabled: true # 是否启用密码认证
      public-key-enabled: true # 是否启用公钥认证
      max-auth-failures: 5 # 最大认证失败次数
```

生产配置建议：

| 配置项                   | 建议                                        |
| ------------------------ | ------------------------------------------- |
| `app.sshd.port`          | 不建议使用 `22`，避免与系统 OpenSSH 冲突    |
| `app.sshd.host`          | 内部系统优先绑定内网 IP                     |
| `app.sshd.host-key-path` | 使用持久化磁盘路径，容器环境需要挂载 Volume |
| `app.sshd.idle-timeout`  | 根据业务场景设置，例如 `300s` 或 `600s`     |
| `password-enabled`       | 生产环境建议谨慎开启                        |
| `public-key-enabled`     | 生产环境推荐开启                            |

### SSH 会话生命周期

SSH 会话生命周期包括连接创建、认证成功、命令执行、空闲超时、客户端断开和服务端关闭。建议通过 `SessionListener` 统一登记在线会话，并在认证器中标记认证成功状态。

推荐生命周期如下：

```text
客户端建立 TCP 连接
  ↓
创建 SSH Session
  ↓
执行用户名密码认证或公钥认证
  ↓
认证成功后登记用户身份
  ↓
执行命令或 SFTP 操作
  ↓
客户端断开 / 空闲超时 / 服务端关闭
  ↓
移除在线会话并记录日志
```

文件位置：`src/main/java/io/github/atengk/sshd/session/SshSessionInfo.java`

下面的会话信息类用于保存在线 SSH 会话状态。

```java
package io.github.atengk.sshd.session;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SSH 会话信息
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
public class SshSessionInfo {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 登录用户名
     */
    private String username;

    /**
     * 客户端地址
     */
    private String clientAddress;

    /**
     * 是否已认证
     */
    private boolean authenticated;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 关闭时间
     */
    private LocalDateTime closedTime;
}
```

文件位置：`src/main/java/io/github/atengk/sshd/session/SshSessionRegistry.java`

下面的会话登记器用于记录会话创建、认证成功、异常和关闭事件。

```java
package io.github.atengk.sshd.session;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.session.SessionListener;
import org.apache.sshd.server.session.ServerSession;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SSH 会话登记器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class SshSessionRegistry implements SessionListener {

    private final ConcurrentMap<String, SshSessionInfo> sessionMap = new ConcurrentHashMap<>();

    /**
     * 会话创建事件
     *
     * @param session SSH会话
     */
    @Override
    public void sessionCreated(Session session) {
        String sessionId = getSessionId(session);
        String clientAddress = getClientAddress(session);

        SshSessionInfo sessionInfo = SshSessionInfo.builder()
                .sessionId(sessionId)
                .clientAddress(clientAddress)
                .authenticated(false)
                .createdTime(LocalDateTime.now())
                .build();

        sessionMap.put(sessionId, sessionInfo);
        log.info("SSH会话已创建，会话ID：{}，客户端：{}", sessionId, clientAddress);
    }

    /**
     * 会话事件
     *
     * @param session SSH会话
     * @param event   会话事件
     */
    @Override
    public void sessionEvent(Session session, Event event) {
        log.debug("SSH会话事件，会话ID：{}，事件：{}", getSessionId(session), event);
    }

    /**
     * 会话异常事件
     *
     * @param session SSH会话
     * @param t       异常信息
     */
    @Override
    public void sessionException(Session session, Throwable t) {
        log.warn("SSH会话发生异常，会话ID：{}，客户端：{}", getSessionId(session), getClientAddress(session), t);
    }

    /**
     * 会话关闭事件
     *
     * @param session SSH会话
     */
    @Override
    public void sessionClosed(Session session) {
        String sessionId = getSessionId(session);
        SshSessionInfo sessionInfo = sessionMap.remove(sessionId);

        if (sessionInfo != null) {
            sessionInfo.setClosedTime(LocalDateTime.now());
            log.info("SSH会话已关闭，会话ID：{}，用户：{}，客户端：{}",
                    sessionId, sessionInfo.getUsername(), sessionInfo.getClientAddress());
        }
    }

    /**
     * 标记会话认证成功
     *
     * @param session  SSH服务端会话
     * @param username 用户名
     */
    public void markAuthenticated(ServerSession session, String username) {
        String sessionId = getSessionId(session);
        SshSessionInfo sessionInfo = sessionMap.get(sessionId);
        if (sessionInfo == null) {
            return;
        }

        sessionInfo.setUsername(username);
        sessionInfo.setAuthenticated(true);
        log.info("SSH会话认证成功，会话ID：{}，用户：{}，客户端：{}", sessionId, username, sessionInfo.getClientAddress());
    }

    /**
     * 查询在线会话
     *
     * @return 在线会话列表
     */
    public List<SshSessionInfo> listOnlineSessions() {
        return CollUtil.newArrayList(sessionMap.values());
    }

    /**
     * 获取会话ID
     *
     * @param session SSH会话
     * @return 会话ID
     */
    private String getSessionId(Session session) {
        return String.valueOf(session.getSessionId());
    }

    /**
     * 获取客户端地址
     *
     * @param session SSH会话
     * @return 客户端地址
     */
    private String getClientAddress(Session session) {
        if (session.getIoSession() == null || session.getIoSession().getRemoteAddress() == null) {
            return StrUtil.EMPTY;
        }
        return String.valueOf(session.getIoSession().getRemoteAddress());
    }
}
```

## 认证与权限控制

认证与权限控制负责确认“谁可以登录”和“登录后可以做什么”。建议将身份认证、权限模型、命令权限、SFTP 根目录隔离分开设计，避免只做登录校验而忽略登录后的操作边界。

### 用户名密码认证

用户名密码认证适合本地开发、内网系统、临时运维入口或兼容旧系统。生产环境如果启用密码认证，应配合强密码、失败次数限制、IP 白名单和审计日志使用。

文件位置：`src/main/java/io/github/atengk/sshd/auth/SshUserAccess.java`

下面的用户访问模型用于保存 SSH 用户、密码摘要、公钥指纹和命令权限。

```java
package io.github.atengk.sshd.auth;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * SSH 用户访问配置
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
public class SshUserAccess {

    /**
     * 用户名
     */
    private String username;

    /**
     * BCrypt密码摘要
     */
    private String passwordHash;

    /**
     * 公钥指纹集合
     */
    private Set<String> publicKeyFingerprints;

    /**
     * 允许执行的命令集合
     */
    private Set<String> allowedCommands;

    /**
     * SFTP用户根目录
     */
    private String sftpRootPath;

    /**
     * 是否启用
     */
    private boolean enabled;
}
```

文件位置：`src/main/java/io/github/atengk/sshd/auth/SshUserAccessService.java`

下面的访问服务提供用户查询、密码校验、公钥校验和命令权限判断。示例使用内存数据，生产环境建议替换为数据库、Redis 或统一 IAM 服务。

```java
package io.github.atengk.sshd.auth;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SSH 用户访问服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
public class SshUserAccessService {

    private final ConcurrentMap<String, SshUserAccess> userStore = new ConcurrentHashMap<>();

    /**
     * 初始化示例用户
     */
    @PostConstruct
    public void init() {
        userStore.put("admin", SshUserAccess.builder()
                .username("admin")
                .passwordHash(BCrypt.hashpw("123456", BCrypt.gensalt()))
                .publicKeyFingerprints(Set.of())
                .allowedCommands(Set.of("help", "status"))
                .sftpRootPath("./data/sshd/sftp/admin")
                .enabled(true)
                .build());

        log.info("SSH示例用户初始化完成，用户数量：{}", userStore.size());
    }

    /**
     * 查询可用用户
     *
     * @param username 用户名
     * @return 用户访问配置
     */
    public Optional<SshUserAccess> findEnabledUser(String username) {
        if (StrUtil.isBlank(username)) {
            return Optional.empty();
        }

        SshUserAccess access = userStore.get(username);
        if (access == null || !access.isEnabled()) {
            return Optional.empty();
        }

        return Optional.of(access);
    }

    /**
     * 校验用户密码
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 是否通过
     */
    public boolean verifyPassword(String username, String password) {
        Optional<SshUserAccess> optionalAccess = findEnabledUser(username);
        if (optionalAccess.isEmpty()) {
            log.warn("SSH密码认证失败，用户不存在或已禁用，用户：{}", username);
            return false;
        }

        SshUserAccess access = optionalAccess.get();
        boolean matched = BCrypt.checkpw(password, access.getPasswordHash());
        if (!matched) {
            log.warn("SSH密码认证失败，密码不匹配，用户：{}", username);
        }

        return matched;
    }

    /**
     * 校验公钥指纹
     *
     * @param username    用户名
     * @param fingerprint 公钥指纹
     * @return 是否通过
     */
    public boolean verifyPublicKeyFingerprint(String username, String fingerprint) {
        Optional<SshUserAccess> optionalAccess = findEnabledUser(username);
        if (optionalAccess.isEmpty()) {
            log.warn("SSH公钥认证失败，用户不存在或已禁用，用户：{}", username);
            return false;
        }

        SshUserAccess access = optionalAccess.get();
        boolean matched = CollUtil.isNotEmpty(access.getPublicKeyFingerprints())
                && access.getPublicKeyFingerprints().contains(fingerprint);

        if (!matched) {
            log.warn("SSH公钥认证失败，指纹不匹配，用户：{}，指纹：{}", username, fingerprint);
        }

        return matched;
    }

    /**
     * 判断用户是否可以执行命令
     *
     * @param username 用户名
     * @param command  命令名称
     * @return 是否允许
     */
    public boolean canExecuteCommand(String username, String command) {
        Optional<SshUserAccess> optionalAccess = findEnabledUser(username);
        if (optionalAccess.isEmpty()) {
            return false;
        }

        SshUserAccess access = optionalAccess.get();
        return CollUtil.isNotEmpty(access.getAllowedCommands())
                && access.getAllowedCommands().contains(command);
    }
}
```

文件位置：`src/main/java/io/github/atengk/sshd/auth/PasswordAuthenticatorImpl.java`

下面的密码认证器用于对接 Apache MINA SSHD 的用户名密码认证流程。

```java
package io.github.atengk.sshd.auth;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.sshd.config.SshdProperties;
import io.github.atengk.sshd.session.SshSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.springframework.stereotype.Component;

/**
 * SSH 用户名密码认证器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordAuthenticatorImpl implements PasswordAuthenticator {

    private final SshdProperties sshdProperties;
    private final SshUserAccessService sshUserAccessService;
    private final SshSessionRegistry sshSessionRegistry;

    /**
     * 执行用户名密码认证
     *
     * @param username 用户名
     * @param password 密码
     * @param session  SSH会话
     * @return 是否认证成功
     */
    @Override
    public boolean authenticate(String username, String password, ServerSession session) {
        if (!sshdProperties.getAuth().isPasswordEnabled()) {
            log.warn("SSH密码认证未启用，用户：{}，客户端：{}", username, getClientAddress(session));
            return false;
        }

        if (StrUtil.hasBlank(username, password)) {
            log.warn("SSH密码认证失败，用户名或密码为空，客户端：{}", getClientAddress(session));
            return false;
        }

        boolean authenticated = sshUserAccessService.verifyPassword(username, password);
        if (authenticated) {
            sshSessionRegistry.markAuthenticated(session, username);
            log.info("SSH密码认证成功，用户：{}，客户端：{}", username, getClientAddress(session));
        }

        return authenticated;
    }

    /**
     * 获取客户端地址
     *
     * @param session SSH会话
     * @return 客户端地址
     */
    private String getClientAddress(ServerSession session) {
        if (session.getIoSession() == null || session.getIoSession().getRemoteAddress() == null) {
            return StrUtil.EMPTY;
        }
        return String.valueOf(session.getIoSession().getRemoteAddress());
    }
}
```

### 公钥认证

公钥认证更适合生产环境。客户端持有私钥，服务端保存公钥指纹或公钥内容。本文示例使用公钥指纹进行匹配，便于将公钥授权信息保存到数据库中。

文件位置：`src/main/java/io/github/atengk/sshd/auth/PublicKeyAuthenticatorImpl.java`

下面的公钥认证器使用 Apache MINA SSHD 的 `KeyUtils.getFingerPrint` 计算客户端公钥指纹，并与用户授权配置进行匹配。

```java
package io.github.atengk.sshd.auth;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.sshd.config.SshdProperties;
import io.github.atengk.sshd.session.SshSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.springframework.stereotype.Component;

import java.security.PublicKey;

/**
 * SSH 公钥认证器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PublicKeyAuthenticatorImpl implements PublickeyAuthenticator {

    private final SshdProperties sshdProperties;
    private final SshUserAccessService sshUserAccessService;
    private final SshSessionRegistry sshSessionRegistry;

    /**
     * 执行公钥认证
     *
     * @param username 用户名
     * @param key      客户端公钥
     * @param session  SSH会话
     * @return 是否认证成功
     */
    @Override
    public boolean authenticate(String username, PublicKey key, ServerSession session) {
        if (!sshdProperties.getAuth().isPublicKeyEnabled()) {
            log.warn("SSH公钥认证未启用，用户：{}，客户端：{}", username, getClientAddress(session));
            return false;
        }

        if (StrUtil.isBlank(username) || key == null) {
            log.warn("SSH公钥认证失败，用户名或公钥为空，客户端：{}", getClientAddress(session));
            return false;
        }

        String fingerprint = KeyUtils.getFingerPrint(key);
        boolean authenticated = sshUserAccessService.verifyPublicKeyFingerprint(username, fingerprint);
        if (authenticated) {
            sshSessionRegistry.markAuthenticated(session, username);
            log.info("SSH公钥认证成功，用户：{}，指纹：{}，客户端：{}", username, fingerprint, getClientAddress(session));
        }

        return authenticated;
    }

    /**
     * 获取客户端地址
     *
     * @param session SSH会话
     * @return 客户端地址
     */
    private String getClientAddress(ServerSession session) {
        if (session.getIoSession() == null || session.getIoSession().getRemoteAddress() == null) {
            return StrUtil.EMPTY;
        }
        return String.valueOf(session.getIoSession().getRemoteAddress());
    }
}
```

客户端可以通过下面的命令查看公钥指纹，然后将指纹配置到用户授权表或配置中心中。

```bash
ssh-keygen -lf ~/.ssh/id_rsa.pub
```

该命令用于读取本地公钥文件的指纹信息。生产环境中应保存完整授权记录，包括用户名、公钥指纹、公钥类型、创建时间、过期时间、状态和备注。

### 用户权限模型

用户权限模型不应只判断“是否能登录”，还要判断“能执行哪些命令”“能访问哪个 SFTP 根目录”“是否允许上传、下载、删除文件”。建议将权限拆分为认证权限、命令权限和文件权限。

推荐模型如下：

| 权限类型 | 说明                                                    |
| -------- | ------------------------------------------------------- |
| 登录权限 | 用户是否启用，是否允许密码登录或公钥登录                |
| 命令权限 | 用户允许执行的命令列表，例如 `help`、`status`、`reload` |
| 文件权限 | 用户的 SFTP 根目录、读权限、写权限、删除权限            |
| 审计权限 | 是否记录该用户的命令和文件操作                          |
| 管理权限 | 是否允许查看在线会话、关闭会话、修改用户配置            |

权限判断建议集中放在 `SshUserAccessService` 中，不建议散落在每个命令处理器里。命令层只调用 `canExecuteCommand` 判断是否允许执行。

```text
用户登录成功
  ↓
CommandFactory 接收命令
  ↓
解析命令名称
  ↓
调用 SshUserAccessService.canExecuteCommand
  ↓
允许：执行命令处理器
拒绝：返回权限不足
```

对于生产环境，可以将示例中的内存模型替换为数据库表：

```sql
-- SSH用户表
CREATE TABLE ssh_user_access (
    id BIGINT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sftp_root_path VARCHAR(255),
    created_time TIMESTAMP,
    updated_time TIMESTAMP
);

-- SSH用户公钥表
CREATE TABLE ssh_user_public_key (
    id BIGINT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    fingerprint VARCHAR(255) NOT NULL,
    public_key_text TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    expired_time TIMESTAMP,
    created_time TIMESTAMP
);

-- SSH用户命令权限表
CREATE TABLE ssh_user_command_permission (
    id BIGINT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    command_name VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_time TIMESTAMP
);
```

以上 SQL 只是权限模型示例。正式项目中建议补充租户 ID、创建人、更新人、状态枚举、逻辑删除字段和唯一索引。

## 命令处理实现

命令处理实现负责将 SSH 客户端输入的命令转换为应用内部可执行逻辑。推荐使用 `CommandFactory + Command + Handler` 的结构：`CommandFactory` 创建命令对象，`Command` 负责解析和执行流程，具体业务命令由多个 Handler 实现。

### CommandFactory 设计

`CommandFactory` 是 Apache MINA SSHD 命令入口。客户端执行 `ssh -p 2222 admin@127.0.0.1 status` 时，`status` 会作为原始命令字符串进入 `CommandFactory`。

推荐处理流程如下：

```text
SSH客户端输入命令
  ↓
AppCommandFactory.createCommand
  ↓
创建 AppCommand
  ↓
AppCommand 解析命令名称和参数
  ↓
校验用户命令权限
  ↓
分发给 AppCommandHandler
  ↓
写入 stdout / stderr
  ↓
返回 exit code
```

文件位置：`src/main/java/io/github/atengk/sshd/command/AppCommandFactory.java`

下面的命令工厂用于创建每一次 SSH 命令请求对应的 `AppCommand`。

```java
package io.github.atengk.sshd.command;

import io.github.atengk.sshd.auth.SshUserAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.command.CommandFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * SSH命令工厂
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppCommandFactory implements CommandFactory {

    private final List<AppCommandHandler> commandHandlers;
    private final SshUserAccessService sshUserAccessService;

    /**
     * 创建 SSH 命令
     *
     * @param channel SSH通道
     * @param command 原始命令
     * @return 命令对象
     * @throws IOException IO异常
     */
    @Override
    public Command createCommand(ChannelSession channel, String command) throws IOException {
        log.info("接收到SSH命令，命令：{}", command);
        return new AppCommand(command, commandHandlers, sshUserAccessService);
    }
}
```

文件位置：`src/main/java/io/github/atengk/sshd/command/AppCommand.java`

下面的命令对象负责解析命令、校验权限、调用处理器并写回执行结果。

```java
package io.github.atengk.sshd.command;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.sshd.auth.SshUserAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.session.ServerSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * SSH应用命令
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@RequiredArgsConstructor
public class AppCommand implements Command {

    private final String rawCommand;
    private final List<AppCommandHandler> commandHandlers;
    private final SshUserAccessService sshUserAccessService;

    private InputStream inputStream;
    private OutputStream outputStream;
    private OutputStream errorStream;
    private ExitCallback exitCallback;

    /**
     * 设置输入流
     *
     * @param inputStream 输入流
     */
    @Override
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * 设置输出流
     *
     * @param outputStream 输出流
     */
    @Override
    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    /**
     * 设置错误流
     *
     * @param errorStream 错误流
     */
    @Override
    public void setErrorStream(OutputStream errorStream) {
        this.errorStream = errorStream;
    }

    /**
     * 设置退出回调
     *
     * @param exitCallback 退出回调
     */
    @Override
    public void setExitCallback(ExitCallback exitCallback) {
        this.exitCallback = exitCallback;
    }

    /**
     * 启动命令执行
     *
     * @param channel     SSH通道
     * @param environment SSH环境变量
     */
    @Override
    public void start(ChannelSession channel, Environment environment) {
        Thread commandThread = new Thread(() -> execute(channel), "ssh-command-worker");
        commandThread.start();
    }

    /**
     * 销毁命令
     *
     * @param channel SSH通道
     */
    @Override
    public void destroy(ChannelSession channel) {
        log.debug("SSH命令销毁，命令：{}", rawCommand);
    }

    /**
     * 执行命令
     *
     * @param channel SSH通道
     */
    private void execute(ChannelSession channel) {
        int exitCode = 0;

        try {
            ServerSession serverSession = channel.getSession();
            String username = serverSession.getUsername();

            CommandContext context = CommandContext.parse(username, rawCommand, serverSession);
            if (StrUtil.isBlank(context.getCommandName())) {
                writeError("命令不能为空\n");
                exitCode = 1;
                return;
            }

            if (!sshUserAccessService.canExecuteCommand(username, context.getCommandName())) {
                writeError(StrUtil.format("权限不足，无法执行命令：{}\n", context.getCommandName()));
                log.warn("SSH命令被拒绝，用户：{}，命令：{}", username, context.getCommandName());
                exitCode = 126;
                return;
            }

            Map<String, AppCommandHandler> handlerMap = commandHandlers.stream()
                    .collect(Collectors.toMap(AppCommandHandler::commandName, Function.identity(), (left, right) -> left));

            AppCommandHandler handler = handlerMap.get(context.getCommandName());
            if (handler == null) {
                writeError(StrUtil.format("未知命令：{}\n", context.getCommandName()));
                exitCode = 127;
                return;
            }

            CommandResult result = handler.handle(context);
            writeOutput(result.getStdout());
            writeError(result.getStderr());
            exitCode = result.getExitCode();

            log.info("SSH命令执行完成，用户：{}，命令：{}，退出码：{}", username, rawCommand, exitCode);
        } catch (Exception e) {
            exitCode = 1;
            writeError("命令执行异常，请联系管理员\n");
            log.error("SSH命令执行异常，命令：{}", rawCommand, e);
        } finally {
            if (exitCallback != null) {
                exitCallback.onExit(exitCode);
            }
        }
    }

    /**
     * 写入标准输出
     *
     * @param content 输出内容
     */
    private void writeOutput(String content) {
        write(outputStream, content);
    }

    /**
     * 写入错误输出
     *
     * @param content 错误内容
     */
    private void writeError(String content) {
        write(errorStream, content);
    }

    /**
     * 写入流
     *
     * @param outputStream 输出流
     * @param content      内容
     */
    private void write(OutputStream outputStream, String content) {
        if (outputStream == null || StrUtil.isBlank(content)) {
            return;
        }

        try {
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (IOException e) {
            log.error("SSH命令结果写入失败", e);
        }
    }
}
```

### 自定义命令解析

自定义命令解析建议先拆分命令名称和参数，再将具体逻辑交给命令处理器。这样新增命令时只需要增加一个 `AppCommandHandler` 实现类，不需要修改主流程。

文件位置：`src/main/java/io/github/atengk/sshd/command/CommandContext.java`

下面的命令上下文用于封装当前用户、原始命令、命令名称、参数和 SSH 会话。

```java
package io.github.atengk.sshd.command;

import cn.hutool.core.util.StrUtil;
import lombok.Builder;
import lombok.Data;
import org.apache.sshd.server.session.ServerSession;

import java.util.List;

/**
 * SSH命令上下文
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
public class CommandContext {

    /**
     * 用户名
     */
    private String username;

    /**
     * 原始命令
     */
    private String rawCommand;

    /**
     * 命令名称
     */
    private String commandName;

    /**
     * 命令参数
     */
    private List<String> args;

    /**
     * SSH服务端会话
     */
    private ServerSession serverSession;

    /**
     * 解析命令上下文
     *
     * @param username      用户名
     * @param rawCommand    原始命令
     * @param serverSession SSH会话
     * @return 命令上下文
     */
    public static CommandContext parse(String username, String rawCommand, ServerSession serverSession) {
        List<String> parts = StrUtil.splitTrim(StrUtil.blankToDefault(rawCommand, StrUtil.EMPTY), ' ');
        String commandName = parts.isEmpty() ? StrUtil.EMPTY : parts.get(0);
        List<String> args = parts.size() <= 1 ? List.of() : parts.subList(1, parts.size());

        return CommandContext.builder()
                .username(username)
                .rawCommand(rawCommand)
                .commandName(commandName)
                .args(args)
                .serverSession(serverSession)
                .build();
    }
}
```

文件位置：`src/main/java/io/github/atengk/sshd/command/AppCommandHandler.java`

下面的处理器接口用于定义具体命令的扩展点。

```java
package io.github.atengk.sshd.command;

/**
 * SSH应用命令处理器
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface AppCommandHandler {

    /**
     * 命令名称
     *
     * @return 命令名称
     */
    String commandName();

    /**
     * 执行命令
     *
     * @param context 命令上下文
     * @return 命令结果
     */
    CommandResult handle(CommandContext context);
}
```

文件位置：`src/main/java/io/github/atengk/sshd/command/handler/HelpCommandHandler.java`

下面的 `help` 命令用于输出当前支持的命令说明。

```java
package io.github.atengk.sshd.command.handler;

import io.github.atengk.sshd.command.AppCommandHandler;
import io.github.atengk.sshd.command.CommandContext;
import io.github.atengk.sshd.command.CommandResult;
import org.springframework.stereotype.Component;

/**
 * help命令处理器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Component
public class HelpCommandHandler implements AppCommandHandler {

    /**
     * 命令名称
     *
     * @return 命令名称
     */
    @Override
    public String commandName() {
        return "help";
    }

    /**
     * 执行help命令
     *
     * @param context 命令上下文
     * @return 命令结果
     */
    @Override
    public CommandResult handle(CommandContext context) {
        String content = """
                支持的命令：
                  help    查看命令帮助
                  status  查看SSH服务状态
                """;
        return CommandResult.success(content);
    }
}
```

文件位置：`src/main/java/io/github/atengk/sshd/command/handler/StatusCommandHandler.java`

下面的 `status` 命令用于查看 SSH 服务的基础运行状态和在线会话数量。

```java
package io.github.atengk.sshd.command.handler;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.sshd.command.AppCommandHandler;
import io.github.atengk.sshd.command.CommandContext;
import io.github.atengk.sshd.command.CommandResult;
import io.github.atengk.sshd.config.SshdProperties;
import io.github.atengk.sshd.session.SshSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * status命令处理器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Component
@RequiredArgsConstructor
public class StatusCommandHandler implements AppCommandHandler {

    private final SshdProperties sshdProperties;
    private final SshSessionRegistry sshSessionRegistry;

    /**
     * 命令名称
     *
     * @return 命令名称
     */
    @Override
    public String commandName() {
        return "status";
    }

    /**
     * 执行status命令
     *
     * @param context 命令上下文
     * @return 命令结果
     */
    @Override
    public CommandResult handle(CommandContext context) {
        String content = StrUtil.format("""
                        SSH服务状态：
                          启用状态：{}
                          监听地址：{}
                          监听端口：{}
                          当前用户：{}
                          在线会话：{}
                        """,
                sshdProperties.isEnabled() ? "已启用" : "未启用",
                sshdProperties.getHost(),
                sshdProperties.getPort(),
                context.getUsername(),
                sshSessionRegistry.listOnlineSessions().size());

        return CommandResult.success(content);
    }
}
```

### 命令执行结果返回

命令执行结果建议统一封装为 `stdout`、`stderr` 和 `exitCode`。客户端可以根据退出码判断执行结果，自动化脚本也能直接识别成功或失败。

推荐退出码规则如下：

| 退出码 | 含义           |
| ------ | -------------- |
| `0`    | 执行成功       |
| `1`    | 通用执行失败   |
| `126`  | 无权限执行     |
| `127`  | 未知命令       |
| `130`  | 客户端中断执行 |

文件位置：`src/main/java/io/github/atengk/sshd/command/CommandResult.java`

下面的结果对象用于统一返回命令执行结果。

```java
package io.github.atengk.sshd.command;

import cn.hutool.core.util.StrUtil;
import lombok.Builder;
import lombok.Data;

/**
 * SSH命令执行结果
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
public class CommandResult {

    /**
     * 标准输出
     */
    private String stdout;

    /**
     * 错误输出
     */
    private String stderr;

    /**
     * 退出码
     */
    private int exitCode;

    /**
     * 成功结果
     *
     * @param stdout 标准输出
     * @return 命令结果
     */
    public static CommandResult success(String stdout) {
        return CommandResult.builder()
                .stdout(StrUtil.blankToDefault(stdout, StrUtil.EMPTY))
                .stderr(StrUtil.EMPTY)
                .exitCode(0)
                .build();
    }

    /**
     * 失败结果
     *
     * @param stderr   错误输出
     * @param exitCode 退出码
     * @return 命令结果
     */
    public static CommandResult failure(String stderr, int exitCode) {
        return CommandResult.builder()
                .stdout(StrUtil.EMPTY)
                .stderr(StrUtil.blankToDefault(stderr, StrUtil.EMPTY))
                .exitCode(exitCode)
                .build();
    }
}
```

本地验证命令如下：

```bash
# 查看帮助命令
ssh -p 2222 admin@127.0.0.1 help

# 查看SSH服务状态
ssh -p 2222 admin@127.0.0.1 status

# 测试未知命令
ssh -p 2222 admin@127.0.0.1 version
```

以上命令中的 `-p 2222` 表示连接内嵌 SSH 服务端口，`admin@127.0.0.1` 表示使用 `admin` 用户登录本机 SSH 服务。示例用户密码为 `123456`，仅用于本地开发验证；生产环境应替换为数据库用户、强密码或公钥认证。

## SFTP 文件服务

SFTP 文件服务用于在内嵌 SSH Server 中提供标准文件传输能力，客户端可以使用 `sftp`、WinSCP、FileZilla、JSch 或 Apache SSHD Client 访问。Apache MINA SSHD 从 2.0 开始将 SFTP 相关能力放在 `sshd-sftp` 依赖中，服务端需要通过 `SftpSubsystemFactory` 注册 SFTP 子系统。([apache.googlesource.com](https://apache.googlesource.com/mina-sshd/%2B/HEAD/docs/sftp.md?utm_source=chatgpt.com)) 本节继续基于原开发大纲展开。

### SFTP 子系统配置

SFTP 子系统配置负责将 SFTP 能力挂载到 SSH Server 上。SSH 命令处理走 `CommandFactory`，SFTP 文件传输走 `SubsystemFactory`，两者互不冲突。Apache MINA SSHD 官方文档中的服务端 SFTP 配置方式就是创建 `SftpSubsystemFactory`，然后通过 `server.setSubsystemFactories(...)` 注册到服务端。([apache.googlesource.com](https://apache.googlesource.com/mina-sshd/%2B/HEAD/docs/sftp.md?utm_source=chatgpt.com))

推荐新增以下文件：

```text
src/main/java/io/github/atengk/sshd/sftp/SftpSubsystemConfiguration.java
src/main/java/io/github/atengk/sshd/sftp/UserRootFileSystemFactory.java
```

文件位置：`src/main/java/io/github/atengk/sshd/sftp/SftpSubsystemConfiguration.java`

下面的配置类用于创建 SFTP 子系统工厂，后续由 `SshServerFactory` 注入并注册到 SSH Server。

```java
package io.github.atengk.sshd.sftp;

import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SFTP子系统配置
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Configuration
public class SftpSubsystemConfiguration {

    /**
     * 创建SFTP子系统工厂
     *
     * @return SFTP子系统工厂
     */
    @Bean
    public SftpSubsystemFactory sftpSubsystemFactory() {
        SftpSubsystemFactory factory = new SftpSubsystemFactory.Builder()
                .build();

        log.info("SFTP子系统工厂初始化完成");
        return factory;
    }
}
```

需要在前面已经定义的 `SshServerFactory` 中注册 SFTP 子系统和文件系统工厂。

文件位置：`src/main/java/io/github/atengk/sshd/server/SshServerFactory.java`

下面只展示需要调整的完整版本，用于替换前面章节中的同名类。

```java
package io.github.atengk.sshd.server;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.sshd.auth.PasswordAuthenticatorImpl;
import io.github.atengk.sshd.auth.PublicKeyAuthenticatorImpl;
import io.github.atengk.sshd.command.AppCommandFactory;
import io.github.atengk.sshd.config.SshdProperties;
import io.github.atengk.sshd.session.SshSessionRegistry;
import io.github.atengk.sshd.sftp.UserRootFileSystemFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.core.CoreModuleProperties;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

/**
 * SSH服务端工厂
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SshServerFactory {

    private final SshdProperties sshdProperties;
    private final PasswordAuthenticatorImpl passwordAuthenticator;
    private final PublicKeyAuthenticatorImpl publicKeyAuthenticator;
    private final AppCommandFactory appCommandFactory;
    private final SshSessionRegistry sshSessionRegistry;
    private final SftpSubsystemFactory sftpSubsystemFactory;
    private final UserRootFileSystemFactory userRootFileSystemFactory;

    /**
     * 创建SSH服务端实例
     *
     * @return SSH服务端
     */
    public SshServer create() {
        prepareDirectory();

        SshServer sshServer = SshServer.setUpDefaultServer();
        sshServer.setHost(sshdProperties.getHost());
        sshServer.setPort(sshdProperties.getPort());
        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(FileUtil.file(sshdProperties.getHostKeyPath()).toPath()));

        // 认证能力
        sshServer.setPasswordAuthenticator(passwordAuthenticator);
        sshServer.setPublickeyAuthenticator(publicKeyAuthenticator);

        // 命令处理能力
        sshServer.setCommandFactory(appCommandFactory);

        // SFTP能力
        sshServer.setSubsystemFactories(List.of(sftpSubsystemFactory));
        sshServer.setFileSystemFactory(userRootFileSystemFactory);

        // 会话监听
        sshServer.addSessionListener(sshSessionRegistry);

        // 空闲超时
        CoreModuleProperties.IDLE_TIMEOUT.set(sshServer, sshdProperties.getIdleTimeout());

        log.info("SSH服务端初始化完成，监听地址：{}，监听端口：{}，SFTP根目录：{}",
                sshdProperties.getHost(), sshdProperties.getPort(), sshdProperties.getSftpRootPath());
        return sshServer;
    }

    /**
     * 准备SSH服务运行目录
     */
    private void prepareDirectory() {
        File hostKeyFile = FileUtil.file(sshdProperties.getHostKeyPath());
        File hostKeyParent = hostKeyFile.getParentFile();
        if (ObjectUtil.isNotNull(hostKeyParent)) {
            FileUtil.mkdir(hostKeyParent);
        }

        FileUtil.mkdir(sshdProperties.getSftpRootPath());
        log.info("SSH运行目录准备完成，主机密钥路径：{}，SFTP根目录：{}",
                sshdProperties.getHostKeyPath(), sshdProperties.getSftpRootPath());
    }
}
```

该配置完成后，SSH Server 同时具备两类能力：执行 `ssh -p 2222 admin@127.0.0.1 status` 时进入命令处理流程；执行 `sftp -P 2222 admin@127.0.0.1` 时进入 SFTP 子系统。

### 文件根目录隔离

文件根目录隔离用于限制用户只能访问自己的 SFTP 根目录，避免通过 `../`、绝对路径或软链接等方式越权访问服务器文件。Apache MINA SSHD 提供的 `VirtualFileSystemFactory` 用于将用户可见范围限制在一个物理目录内；其说明明确指出该工厂用于降低文件系统可见范围到指定物理文件夹。([Javadoc](https://javadoc.io/static/org.apache.sshd/sshd-core/1.7.0/org/apache/sshd/common/file/virtualfs/VirtualFileSystemFactory.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/sshd/sftp/UserRootFileSystemFactory.java`

下面的文件系统工厂按用户名解析 SFTP 根目录。示例优先读取用户配置中的 `sftpRootPath`，如果没有配置则回退到全局根目录。

```java
package io.github.atengk.sshd.sftp;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.sshd.auth.SshUserAccessService;
import io.github.atengk.sshd.config.SshdProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.session.SessionContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 用户根目录文件系统工厂
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRootFileSystemFactory extends VirtualFileSystemFactory {

    private final SshdProperties sshdProperties;
    private final SshUserAccessService sshUserAccessService;

    /**
     * 获取用户SFTP根目录
     *
     * @param session SSH会话上下文
     * @return 用户根目录
     * @throws IOException IO异常
     */
    @Override
    public Path getUserHomeDir(SessionContext session) throws IOException {
        String username = session == null ? StrUtil.EMPTY : session.getUsername();

        String rootPath = sshUserAccessService.resolveSftpRootPath(username)
                .filter(StrUtil::isNotBlank)
                .orElse(sshdProperties.getSftpRootPath());

        Path root = Paths.get(rootPath).toAbsolutePath().normalize();
        FileUtil.mkdir(root.toFile());

        log.info("解析SFTP用户根目录，用户：{}，根目录：{}", username, root);
        return root;
    }
}
```

需要在 `SshUserAccessService` 中补充一个根目录解析方法。

文件位置：`src/main/java/io/github/atengk/sshd/auth/SshUserAccessService.java`

下面只展示需要追加到 `SshUserAccessService` 中的方法。

```java
/**
 * 解析用户SFTP根目录
 *
 * @param username 用户名
 * @return SFTP根目录
 */
public Optional<String> resolveSftpRootPath(String username) {
    return findEnabledUser(username)
            .map(SshUserAccess::getSftpRootPath)
            .filter(StrUtil::isNotBlank);
}
```

用户根目录建议按用户或租户隔离，例如：

```text
/data/app/sshd/sftp/
├── admin/
│   ├── inbound/
│   ├── outbound/
│   └── archive/
├── ops/
│   ├── inbound/
│   ├── outbound/
│   └── archive/
└── tenant-a/
    ├── inbound/
    ├── outbound/
    └── archive/
```

隔离规则建议如下：

| 规则               | 说明                                                        |
| ------------------ | ----------------------------------------------------------- |
| 每个用户独立根目录 | 用户登录后只能看到自己的虚拟根目录                          |
| 不暴露系统目录     | 不允许将 `/`、`/etc`、`/root`、应用安装目录作为 SFTP 根目录 |
| 路径统一规范化     | 使用 `toAbsolutePath().normalize()` 处理路径                |
| 目录自动创建       | 用户首次登录时自动创建根目录                                |
| 权限与审计分离     | 目录隔离负责文件范围，审计日志负责操作记录                  |

### 文件上传与下载

文件上传与下载使用标准 SFTP 客户端即可完成。服务端只需要保证 SFTP 子系统已注册、用户认证通过、文件系统根目录已正确解析。

本地验证前先准备测试文件：

```bash
# 创建本地测试文件
echo "hello apache mina sshd" > /tmp/sshd-demo.txt

# 查看测试文件
cat /tmp/sshd-demo.txt
```

上述命令会在本机 `/tmp` 下创建一个测试文件，用于后续上传到内嵌 SFTP 服务。

使用命令行客户端连接 SFTP：

```bash
# 连接内嵌SFTP服务
sftp -P 2222 admin@127.0.0.1
```

进入 SFTP 交互终端后执行：

```bash
# 查看服务端当前目录
pwd

# 创建上传目录
mkdir inbound

# 上传文件到服务端
put /tmp/sshd-demo.txt inbound/sshd-demo.txt

# 查看服务端文件列表
ls inbound

# 下载文件到本地
get inbound/sshd-demo.txt /tmp/sshd-demo-download.txt

# 删除服务端测试文件
rm inbound/sshd-demo.txt

# 退出SFTP
bye
```

这些命令分别用于连接 SFTP、创建目录、上传文件、查看文件、下载文件、删除文件和退出会话。`-P 2222` 是 SFTP 客户端指定端口的参数，注意是大写 `P`，不同于 `ssh` 命令中的小写 `-p`。

上传下载完成后，可以在服务端检查用户目录：

```bash
# 查看admin用户SFTP目录
find ./data/sshd/sftp/admin -maxdepth 3 -type f -print
```

生产环境建议对 SFTP 文件操作增加审计事件，例如上传、下载、删除、重命名、创建目录、删除目录。可以在后续“日志与异常处理”章节中通过 `SftpEventListener` 或文件系统访问器进行扩展。

## Spring Boot 集成方式

Spring Boot 集成方式负责将 SSH Server 的配置、Bean 初始化、生命周期和管理接口纳入 Spring 容器。推荐采用 `@ConfigurationProperties + SmartLifecycle + Controller` 的组合方式：配置由 Spring 绑定，服务由生命周期组件启动和停止，管理接口用于查看状态和手动控制服务。

### 配置属性绑定

配置属性绑定用于将 `application.yml` 中的 `app.sshd` 配置映射为 Java 对象。前面已经定义过 `SshdProperties`，这里补充主启动类和配置文件示例，确保配置类能够被扫描和校验。

文件位置：`src/main/java/io/github/atengk/sshd/MinaSshdApplication.java`

下面的启动类是标准 Spring Boot 3 应用入口。

```java
package io.github.atengk.sshd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Apache MINA SSHD示例应用
 *
 * @author Ateng
 * @since 2026-05-07
 */
@SpringBootApplication
public class MinaSshdApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MinaSshdApplication.class, args);
    }
}
```

文件位置：`src/main/resources/application.yml`

下面是完整基础配置示例，包含 HTTP 管理端口、SSH 服务配置和日志配置。

```yaml
server:
  port: 8080 # HTTP管理接口端口

spring:
  application:
    name: springboot-mina-sshd # 应用名称

app:
  sshd:
    enabled: true # 是否启用内嵌SSH服务
    host: 0.0.0.0 # SSH监听地址
    port: 2222 # SSH监听端口
    host-key-path: ./data/sshd/hostkey.ser # SSH服务端主机密钥
    sftp-root-path: ./data/sshd/sftp # SFTP默认根目录
    idle-timeout: 600s # SSH会话空闲超时时间
    auth:
      password-enabled: true # 是否启用密码认证
      public-key-enabled: true # 是否启用公钥认证
      max-auth-failures: 5 # 最大认证失败次数

logging:
  level:
    io.github.atengk.sshd: info # 当前模块日志级别
    org.apache.sshd: info # Apache MINA SSHD日志级别
```

配置绑定注意事项：

| 配置项           | 注意事项                                                  |
| ---------------- | --------------------------------------------------------- |
| `host-key-path`  | 容器部署时应挂载持久化目录，避免每次重启生成新主机密钥    |
| `sftp-root-path` | 不建议放在应用 jar 所在目录，可放到 `/data/app/sshd/sftp` |
| `idle-timeout`   | 支持 Spring Boot Duration 格式，例如 `600s`、`10m`        |
| `enabled`        | 本地开发可开启，部分环境可通过配置关闭 SSH 服务           |

### Bean 初始化与销毁

Bean 初始化与销毁建议由 Spring 容器统一管理。SSH Server 本身是运行时对象，不建议在 `main` 方法中手动启动；应通过 `SmartLifecycle` 在应用启动完成后启动，在应用关闭时停止。这样可以避免资源泄漏、端口未释放和重复启动。

前面已经定义过 `SshServerLifecycle`，这里给出一个更适合管理接口调用的完整版本，支持启动、停止、重启和状态查询。

文件位置：`src/main/java/io/github/atengk/sshd/server/SshServerLifecycle.java`

下面的生命周期类用于托管 SSH Server 的初始化与销毁。

```java
package io.github.atengk.sshd.server;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.sshd.config.SshdProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.server.SshServer;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SSH服务生命周期管理器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SshServerLifecycle implements SmartLifecycle {

    private final SshdProperties sshdProperties;
    private final SshServerFactory sshServerFactory;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Getter
    private SshServer sshServer;

    /**
     * 启动SSH服务
     */
    @Override
    public synchronized void start() {
        if (!sshdProperties.isEnabled()) {
            log.info("SSH服务未启用，跳过启动");
            return;
        }

        if (running.get()) {
            log.info("SSH服务已经处于运行状态，跳过重复启动");
            return;
        }

        try {
            this.sshServer = sshServerFactory.create();
            this.sshServer.start();
            running.set(true);
            log.info("SSH服务启动成功，监听地址：{}，监听端口：{}", sshdProperties.getHost(), sshdProperties.getPort());
        } catch (IOException e) {
            running.set(false);
            log.error("SSH服务启动失败，监听端口：{}", sshdProperties.getPort(), e);
            throw new IllegalStateException("SSH服务启动失败", e);
        }
    }

    /**
     * 停止SSH服务
     */
    @Override
    public synchronized void stop() {
        if (!running.get()) {
            log.info("SSH服务未运行，跳过停止");
            return;
        }

        if (ObjectUtil.isNull(this.sshServer)) {
            running.set(false);
            return;
        }

        try {
            this.sshServer.stop(true);
            running.set(false);
            log.info("SSH服务停止成功");
        } catch (IOException e) {
            log.error("SSH服务停止异常", e);
            throw new IllegalStateException("SSH服务停止异常", e);
        }
    }

    /**
     * 停止SSH服务并执行回调
     *
     * @param callback 停止完成回调
     */
    @Override
    public void stop(Runnable callback) {
        try {
            stop();
        } finally {
            callback.run();
        }
    }

    /**
     * 重启SSH服务
     */
    public synchronized void restart() {
        log.info("开始重启SSH服务");
        stop();
        start();
        log.info("SSH服务重启完成");
    }

    /**
     * 判断SSH服务是否运行
     *
     * @return 是否运行
     */
    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 是否自动启动
     *
     * @return 是否自动启动
     */
    @Override
    public boolean isAutoStartup() {
        return true;
    }

    /**
     * 生命周期阶段
     *
     * @return 生命周期阶段
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
```

该类的关键点是 `start()`、`stop()` 和 `restart()` 都使用 `synchronized` 控制并发，避免管理接口同时触发启动和停止导致状态错乱。`stop(true)` 表示停止服务时同时关闭相关资源。

### 服务启动与停止控制

服务启动与停止控制用于通过 HTTP 管理接口查看 SSH 服务状态，或在不重启 Spring Boot 应用的情况下手动启动、停止、重启 SSH Server。生产环境中这些接口应接入 Spring Security、Sa-Token 或网关鉴权，避免未授权用户关闭 SSH 服务。

推荐新增以下文件：

```text
src/main/java/io/github/atengk/sshd/controller/SshServerController.java
src/main/java/io/github/atengk/sshd/controller/vo/SshServerStatusVO.java
```

文件位置：`src/main/java/io/github/atengk/sshd/controller/vo/SshServerStatusVO.java`

下面的 VO 用于返回 SSH 服务状态。

```java
package io.github.atengk.sshd.controller.vo;

import lombok.Builder;
import lombok.Data;

/**
 * SSH服务状态响应
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
public class SshServerStatusVO {

    /**
     * 是否启用
     */
    private boolean enabled;

    /**
     * 是否运行中
     */
    private boolean running;

    /**
     * 监听地址
     */
    private String host;

    /**
     * 监听端口
     */
    private int port;

    /**
     * SFTP根目录
     */
    private String sftpRootPath;

    /**
     * 在线会话数量
     */
    private int onlineSessionCount;
}
```

文件位置：`src/main/java/io/github/atengk/sshd/controller/SshServerController.java`

下面的 Controller 提供 SSH 服务状态查询、启动、停止和重启接口。

```java
package io.github.atengk.sshd.controller;

import io.github.atengk.sshd.config.SshdProperties;
import io.github.atengk.sshd.controller.vo.SshServerStatusVO;
import io.github.atengk.sshd.server.SshServerLifecycle;
import io.github.atengk.sshd.session.SshSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * SSH服务管理接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@RestController
@RequestMapping("/api/ssh/server")
@RequiredArgsConstructor
public class SshServerController {

    private final SshdProperties sshdProperties;
    private final SshServerLifecycle sshServerLifecycle;
    private final SshSessionRegistry sshSessionRegistry;

    /**
     * 查询SSH服务状态
     *
     * @return SSH服务状态
     */
    @GetMapping("/status")
    public SshServerStatusVO status() {
        return SshServerStatusVO.builder()
                .enabled(sshdProperties.isEnabled())
                .running(sshServerLifecycle.isRunning())
                .host(sshdProperties.getHost())
                .port(sshdProperties.getPort())
                .sftpRootPath(sshdProperties.getSftpRootPath())
                .onlineSessionCount(sshSessionRegistry.listOnlineSessions().size())
                .build();
    }

    /**
     * 启动SSH服务
     *
     * @return SSH服务状态
     */
    @PostMapping("/start")
    public SshServerStatusVO start() {
        sshServerLifecycle.start();
        log.info("通过管理接口启动SSH服务");
        return status();
    }

    /**
     * 停止SSH服务
     *
     * @return SSH服务状态
     */
    @PostMapping("/stop")
    public SshServerStatusVO stop() {
        sshServerLifecycle.stop();
        log.info("通过管理接口停止SSH服务");
        return status();
    }

    /**
     * 重启SSH服务
     *
     * @return SSH服务状态
     */
    @PostMapping("/restart")
    public SshServerStatusVO restart() {
        sshServerLifecycle.restart();
        log.info("通过管理接口重启SSH服务");
        return status();
    }
}
```

接口验证命令如下：

```bash
# 查询SSH服务状态
curl -X GET http://127.0.0.1:8080/api/ssh/server/status

# 启动SSH服务
curl -X POST http://127.0.0.1:8080/api/ssh/server/start

# 停止SSH服务
curl -X POST http://127.0.0.1:8080/api/ssh/server/stop

# 重启SSH服务
curl -X POST http://127.0.0.1:8080/api/ssh/server/restart
```

这些接口用于本地调试和内部管理。生产环境不建议裸露在公网，应至少增加鉴权、访问日志、IP 白名单和操作审计。

状态接口响应示例：

```json
{
  "enabled": true,
  "running": true,
  "host": "0.0.0.0",
  "port": 2222,
  "sftpRootPath": "./data/sshd/sftp",
  "onlineSessionCount": 1
}
```

完成以上配置后，Spring Boot 与 Apache MINA SSHD 的集成关系如下：

```text
Spring Boot容器
  ├── SshdProperties             绑定 app.sshd 配置
  ├── SftpSubsystemFactory       提供 SFTP 子系统
  ├── UserRootFileSystemFactory  提供用户文件根目录隔离
  ├── SshServerFactory           创建并组装 SSH Server
  ├── SshServerLifecycle         托管启动、停止、重启
  └── SshServerController        提供 HTTP 管理接口
```

至此，SSH 命令能力、SFTP 文件能力和 Spring Boot 生命周期管理已经形成完整闭环。后续可以继续在“日志与异常处理”章节中补充连接日志、命令日志、SFTP 操作审计和异常分类处理。

## 日志与异常处理

日志与异常处理用于记录 SSH 连接、认证、命令执行、SFTP 操作和管理接口操作，并将异常按认证失败、权限不足、命令不存在、服务状态异常等类型进行归类。该部分应和业务日志、审计日志区分：业务日志用于排查问题，审计日志用于追踪用户行为。以下内容继续基于原开发大纲展开。

### 连接日志记录

连接日志记录 SSH 客户端从建立连接到关闭连接的全过程。建议在 `SessionListener` 中统一记录会话创建、认证成功、异常和关闭事件，并将关键字段写入审计服务。

推荐新增以下文件：

```text
src/main/java/io/github/atengk/sshd/audit/SshAuditEventType.java
src/main/java/io/github/atengk/sshd/audit/SshAuditEvent.java
src/main/java/io/github/atengk/sshd/audit/SshAuditService.java
```

文件位置：`src/main/java/io/github/atengk/sshd/audit/SshAuditEventType.java`

下面的枚举用于定义 SSH 审计事件类型。

```java
package io.github.atengk.sshd.audit;

/**
 * SSH审计事件类型
 *
 * @author Ateng
 * @since 2026-05-07
 */
public enum SshAuditEventType {

    /**
     * 连接创建
     */
    CONNECTION_CREATED,

    /**
     * 连接关闭
     */
    CONNECTION_CLOSED,

    /**
     * 认证成功
     */
    AUTH_SUCCESS,

    /**
     * 认证失败
     */
    AUTH_FAILURE,

    /**
     * 命令执行
     */
    COMMAND_EXECUTE,

    /**
     * 命令拒绝
     */
    COMMAND_REJECTED,

    /**
     * 命令失败
     */
    COMMAND_FAILED,

    /**
     * 用户访问配置变更
     */
    USER_ACCESS_CHANGE
}
```

文件位置：`src/main/java/io/github/atengk/sshd/audit/SshAuditEvent.java`

下面的事件对象用于封装审计日志字段，后续可以落库、写入 MQ 或接入日志平台。

```java
package io.github.atengk.sshd.audit;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SSH审计事件
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
public class SshAuditEvent {

    /**
     * 事件类型
     */
    private SshAuditEventType eventType;

    /**
     * 用户名
     */
    private String username;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 客户端地址
     */
    private String clientAddress;

    /**
     * 命令内容
     */
    private String command;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 事件消息
     */
    private String message;

    /**
     * 发生时间
     */
    private LocalDateTime occurredTime;
}
```

文件位置：`src/main/java/io/github/atengk/sshd/audit/SshAuditService.java`

下面的审计服务当前使用日志输出，生产环境可替换为数据库、Kafka、RabbitMQ 或日志采集系统。

```java
package io.github.atengk.sshd.audit;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * SSH审计服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
public class SshAuditService {

    /**
     * 记录审计事件
     *
     * @param event 审计事件
     */
    public void record(SshAuditEvent event) {
        if (ObjectUtil.isNull(event)) {
            return;
        }

        if (event.getOccurredTime() == null) {
            event.setOccurredTime(LocalDateTime.now());
        }

        log.info("SSH审计事件：{}", JSONUtil.toJsonStr(event));
    }

    /**
     * 记录连接事件
     *
     * @param eventType     事件类型
     * @param sessionId     会话ID
     * @param username      用户名
     * @param clientAddress 客户端地址
     * @param success       是否成功
     * @param message       事件消息
     */
    public void recordConnection(SshAuditEventType eventType, String sessionId, String username,
                                 String clientAddress, boolean success, String message) {
        record(SshAuditEvent.builder()
                .eventType(eventType)
                .sessionId(StrUtil.blankToDefault(sessionId, StrUtil.EMPTY))
                .username(StrUtil.blankToDefault(username, StrUtil.EMPTY))
                .clientAddress(StrUtil.blankToDefault(clientAddress, StrUtil.EMPTY))
                .success(success)
                .message(StrUtil.blankToDefault(message, StrUtil.EMPTY))
                .occurredTime(LocalDateTime.now())
                .build());
    }

    /**
     * 记录命令事件
     *
     * @param eventType     事件类型
     * @param sessionId     会话ID
     * @param username      用户名
     * @param clientAddress 客户端地址
     * @param command       命令内容
     * @param success       是否成功
     * @param message       事件消息
     */
    public void recordCommand(SshAuditEventType eventType, String sessionId, String username,
                              String clientAddress, String command, boolean success, String message) {
        record(SshAuditEvent.builder()
                .eventType(eventType)
                .sessionId(StrUtil.blankToDefault(sessionId, StrUtil.EMPTY))
                .username(StrUtil.blankToDefault(username, StrUtil.EMPTY))
                .clientAddress(StrUtil.blankToDefault(clientAddress, StrUtil.EMPTY))
                .command(StrUtil.blankToDefault(command, StrUtil.EMPTY))
                .success(success)
                .message(StrUtil.blankToDefault(message, StrUtil.EMPTY))
                .occurredTime(LocalDateTime.now())
                .build());
    }
}
```

需要将前面章节中的 `SshSessionRegistry` 调整为支持连接日志和会话关闭控制。

文件位置：`src/main/java/io/github/atengk/sshd/session/SshSessionRegistry.java`

下面的会话登记器负责记录连接创建、连接异常、连接关闭和认证成功事件。

```java
package io.github.atengk.sshd.session;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.sshd.audit.SshAuditEventType;
import io.github.atengk.sshd.audit.SshAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.session.SessionListener;
import org.apache.sshd.server.session.ServerSession;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SSH会话登记器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SshSessionRegistry implements SessionListener {

    private final SshAuditService sshAuditService;

    private final ConcurrentMap<String, SshSessionInfo> sessionInfoMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Session> sessionMap = new ConcurrentHashMap<>();

    /**
     * 会话创建事件
     *
     * @param session SSH会话
     */
    @Override
    public void sessionCreated(Session session) {
        String sessionId = getSessionId(session);
        String clientAddress = getClientAddress(session);

        SshSessionInfo sessionInfo = SshSessionInfo.builder()
                .sessionId(sessionId)
                .clientAddress(clientAddress)
                .authenticated(false)
                .createdTime(LocalDateTime.now())
                .build();

        sessionMap.put(sessionId, session);
        sessionInfoMap.put(sessionId, sessionInfo);

        log.info("SSH连接已创建，会话ID：{}，客户端：{}", sessionId, clientAddress);
        sshAuditService.recordConnection(SshAuditEventType.CONNECTION_CREATED, sessionId, StrUtil.EMPTY,
                clientAddress, true, "SSH连接已创建");
    }

    /**
     * 会话异常事件
     *
     * @param session SSH会话
     * @param t       异常信息
     */
    @Override
    public void sessionException(Session session, Throwable t) {
        String sessionId = getSessionId(session);
        String clientAddress = getClientAddress(session);

        log.warn("SSH连接发生异常，会话ID：{}，客户端：{}", sessionId, clientAddress, t);
        sshAuditService.recordConnection(SshAuditEventType.CONNECTION_CLOSED, sessionId, getUsername(session),
                clientAddress, false, "SSH连接发生异常：" + t.getMessage());
    }

    /**
     * 会话关闭事件
     *
     * @param session SSH会话
     */
    @Override
    public void sessionClosed(Session session) {
        String sessionId = getSessionId(session);
        SshSessionInfo sessionInfo = sessionInfoMap.remove(sessionId);
        sessionMap.remove(sessionId);

        String username = sessionInfo == null ? getUsername(session) : sessionInfo.getUsername();
        String clientAddress = sessionInfo == null ? getClientAddress(session) : sessionInfo.getClientAddress();

        if (sessionInfo != null) {
            sessionInfo.setClosedTime(LocalDateTime.now());
        }

        log.info("SSH连接已关闭，会话ID：{}，用户：{}，客户端：{}", sessionId, username, clientAddress);
        sshAuditService.recordConnection(SshAuditEventType.CONNECTION_CLOSED, sessionId, username,
                clientAddress, true, "SSH连接已关闭");
    }

    /**
     * 标记认证成功
     *
     * @param session  SSH服务端会话
     * @param username 用户名
     */
    public void markAuthenticated(ServerSession session, String username) {
        String sessionId = getSessionId(session);
        String clientAddress = getClientAddress(session);
        SshSessionInfo sessionInfo = sessionInfoMap.get(sessionId);

        if (sessionInfo != null) {
            sessionInfo.setUsername(username);
            sessionInfo.setAuthenticated(true);
        }

        log.info("SSH认证成功，会话ID：{}，用户：{}，客户端：{}", sessionId, username, clientAddress);
        sshAuditService.recordConnection(SshAuditEventType.AUTH_SUCCESS, sessionId, username,
                clientAddress, true, "SSH认证成功");
    }

    /**
     * 查询在线会话
     *
     * @return 在线会话列表
     */
    public List<SshSessionInfo> listOnlineSessions() {
        return CollUtil.newArrayList(sessionInfoMap.values());
    }

    /**
     * 关闭指定会话
     *
     * @param sessionId 会话ID
     * @return 是否关闭成功
     */
    public boolean closeSession(String sessionId) {
        Session session = sessionMap.get(sessionId);
        if (session == null) {
            log.warn("SSH会话不存在，无法关闭，会话ID：{}", sessionId);
            return false;
        }

        session.close(true);
        log.info("SSH会话关闭指令已发送，会话ID：{}", sessionId);
        return true;
    }

    /**
     * 获取会话ID
     *
     * @param session SSH会话
     * @return 会话ID
     */
    private String getSessionId(Session session) {
        return String.valueOf(session.getSessionId());
    }

    /**
     * 获取用户名
     *
     * @param session SSH会话
     * @return 用户名
     */
    private String getUsername(Session session) {
        return session instanceof ServerSession serverSession
                ? StrUtil.blankToDefault(serverSession.getUsername(), StrUtil.EMPTY)
                : StrUtil.EMPTY;
    }

    /**
     * 获取客户端地址
     *
     * @param session SSH会话
     * @return 客户端地址
     */
    private String getClientAddress(Session session) {
        if (session == null || session.getIoSession() == null || session.getIoSession().getRemoteAddress() == null) {
            return StrUtil.EMPTY;
        }
        return String.valueOf(session.getIoSession().getRemoteAddress());
    }
}
```

### 命令执行日志

命令执行日志用于记录用户执行了什么命令、执行结果是什么、是否被权限控制拒绝、是否发生异常。建议在命令执行入口统一记录，而不是在每个命令处理器中重复记录。

需要调整前面章节中的 `AppCommandFactory` 和 `AppCommand`，将 `SshAuditService` 注入命令对象。

文件位置：`src/main/java/io/github/atengk/sshd/command/AppCommandFactory.java`

下面的命令工厂增加了审计服务注入。

```java
package io.github.atengk.sshd.command;

import io.github.atengk.sshd.audit.SshAuditService;
import io.github.atengk.sshd.auth.SshUserAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.command.CommandFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * SSH命令工厂
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppCommandFactory implements CommandFactory {

    private final List<AppCommandHandler> commandHandlers;
    private final SshUserAccessService sshUserAccessService;
    private final SshAuditService sshAuditService;

    /**
     * 创建SSH命令
     *
     * @param channel SSH通道
     * @param command 原始命令
     * @return 命令对象
     * @throws IOException IO异常
     */
    @Override
    public Command createCommand(ChannelSession channel, String command) throws IOException {
        log.info("接收到SSH命令，命令：{}", command);
        return new AppCommand(command, commandHandlers, sshUserAccessService, sshAuditService);
    }
}
```

文件位置：`src/main/java/io/github/atengk/sshd/command/AppCommand.java`

下面的命令实现增加了命令成功、拒绝、失败的统一审计记录。

```java
package io.github.atengk.sshd.command;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.sshd.audit.SshAuditEventType;
import io.github.atengk.sshd.audit.SshAuditService;
import io.github.atengk.sshd.auth.SshUserAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.session.ServerSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * SSH应用命令
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@RequiredArgsConstructor
public class AppCommand implements Command {

    private final String rawCommand;
    private final List<AppCommandHandler> commandHandlers;
    private final SshUserAccessService sshUserAccessService;
    private final SshAuditService sshAuditService;

    private InputStream inputStream;
    private OutputStream outputStream;
    private OutputStream errorStream;
    private ExitCallback exitCallback;

    /**
     * 设置输入流
     *
     * @param inputStream 输入流
     */
    @Override
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * 设置输出流
     *
     * @param outputStream 输出流
     */
    @Override
    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    /**
     * 设置错误流
     *
     * @param errorStream 错误流
     */
    @Override
    public void setErrorStream(OutputStream errorStream) {
        this.errorStream = errorStream;
    }

    /**
     * 设置退出回调
     *
     * @param exitCallback 退出回调
     */
    @Override
    public void setExitCallback(ExitCallback exitCallback) {
        this.exitCallback = exitCallback;
    }

    /**
     * 启动命令执行
     *
     * @param channel     SSH通道
     * @param environment SSH环境变量
     */
    @Override
    public void start(ChannelSession channel, Environment environment) {
        Thread commandThread = new Thread(() -> execute(channel), "ssh-command-worker");
        commandThread.start();
    }

    /**
     * 销毁命令
     *
     * @param channel SSH通道
     */
    @Override
    public void destroy(ChannelSession channel) {
        log.debug("SSH命令销毁，命令：{}", rawCommand);
    }

    /**
     * 执行命令
     *
     * @param channel SSH通道
     */
    private void execute(ChannelSession channel) {
        int exitCode = 0;
        ServerSession serverSession = channel.getSession();
        String sessionId = String.valueOf(serverSession.getSessionId());
        String username = StrUtil.blankToDefault(serverSession.getUsername(), StrUtil.EMPTY);
        String clientAddress = getClientAddress(serverSession);

        try {
            CommandContext context = CommandContext.parse(username, rawCommand, serverSession);
            if (StrUtil.isBlank(context.getCommandName())) {
                writeError("命令不能为空\n");
                exitCode = 1;
                sshAuditService.recordCommand(SshAuditEventType.COMMAND_FAILED, sessionId, username,
                        clientAddress, rawCommand, false, "命令为空");
                return;
            }

            if (!sshUserAccessService.canExecuteCommand(username, context.getCommandName())) {
                writeError(StrUtil.format("权限不足，无法执行命令：{}\n", context.getCommandName()));
                exitCode = 126;
                log.warn("SSH命令被拒绝，用户：{}，命令：{}", username, context.getCommandName());
                sshAuditService.recordCommand(SshAuditEventType.COMMAND_REJECTED, sessionId, username,
                        clientAddress, rawCommand, false, "权限不足");
                return;
            }

            Map<String, AppCommandHandler> handlerMap = commandHandlers.stream()
                    .collect(Collectors.toMap(AppCommandHandler::commandName, Function.identity(), (left, right) -> left));

            AppCommandHandler handler = handlerMap.get(context.getCommandName());
            if (handler == null) {
                writeError(StrUtil.format("未知命令：{}\n", context.getCommandName()));
                exitCode = 127;
                sshAuditService.recordCommand(SshAuditEventType.COMMAND_FAILED, sessionId, username,
                        clientAddress, rawCommand, false, "未知命令");
                return;
            }

            CommandResult result = handler.handle(context);
            writeOutput(result.getStdout());
            writeError(result.getStderr());
            exitCode = result.getExitCode();

            boolean success = exitCode == 0;
            sshAuditService.recordCommand(success ? SshAuditEventType.COMMAND_EXECUTE : SshAuditEventType.COMMAND_FAILED,
                    sessionId, username, clientAddress, rawCommand, success, "命令执行完成");

            log.info("SSH命令执行完成，用户：{}，命令：{}，退出码：{}", username, rawCommand, exitCode);
        } catch (Exception e) {
            exitCode = 1;
            writeError("命令执行异常，请联系管理员\n");
            log.error("SSH命令执行异常，用户：{}，命令：{}", username, rawCommand, e);
            sshAuditService.recordCommand(SshAuditEventType.COMMAND_FAILED, sessionId, username,
                    clientAddress, rawCommand, false, "命令执行异常：" + e.getMessage());
        } finally {
            if (exitCallback != null) {
                exitCallback.onExit(exitCode);
            }
        }
    }

    /**
     * 写入标准输出
     *
     * @param content 输出内容
     */
    private void writeOutput(String content) {
        write(outputStream, content);
    }

    /**
     * 写入错误输出
     *
     * @param content 错误内容
     */
    private void writeError(String content) {
        write(errorStream, content);
    }

    /**
     * 写入流
     *
     * @param outputStream 输出流
     * @param content      内容
     */
    private void write(OutputStream outputStream, String content) {
        if (outputStream == null || StrUtil.isBlank(content)) {
            return;
        }

        try {
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (IOException e) {
            log.error("SSH命令结果写入失败", e);
        }
    }

    /**
     * 获取客户端地址
     *
     * @param serverSession SSH服务端会话
     * @return 客户端地址
     */
    private String getClientAddress(ServerSession serverSession) {
        if (serverSession.getIoSession() == null || serverSession.getIoSession().getRemoteAddress() == null) {
            return StrUtil.EMPTY;
        }
        return String.valueOf(serverSession.getIoSession().getRemoteAddress());
    }
}
```

命令执行日志建议至少包含以下字段：

| 字段            | 说明               |
| --------------- | ------------------ |
| `sessionId`     | SSH 会话 ID        |
| `username`      | 登录用户           |
| `clientAddress` | 客户端地址         |
| `command`       | 原始命令           |
| `exitCode`      | 命令退出码         |
| `success`       | 是否执行成功       |
| `message`       | 失败原因或补充说明 |
| `occurredTime`  | 发生时间           |

### 异常分类处理

异常分类处理用于统一管理 SSH 模块内部错误。命令通道中的异常应写入 SSH 错误流；HTTP 管理接口中的异常应返回标准 JSON。两类异常处理目标不同，但底层错误码可以统一。

推荐新增以下文件：

```text
src/main/java/io/github/atengk/sshd/common/ApiResult.java
src/main/java/io/github/atengk/sshd/common/exception/SshErrorCode.java
src/main/java/io/github/atengk/sshd/common/exception/SshBizException.java
src/main/java/io/github/atengk/sshd/common/exception/GlobalExceptionHandler.java
```

文件位置：`src/main/java/io/github/atengk/sshd/common/ApiResult.java`

下面的响应对象用于 HTTP 管理接口返回统一结构。

```java
package io.github.atengk.sshd.common;

import lombok.Builder;
import lombok.Data;

/**
 * 接口统一响应
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
public class ApiResult<T> {

    /**
     * 响应码
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
     * @return 响应结果
     */
    public static <T> ApiResult<T> success(T data) {
        return ApiResult.<T>builder()
                .code(0)
                .message("操作成功")
                .data(data)
                .build();
    }

    /**
     * 失败响应
     *
     * @param code    响应码
     * @param message 响应消息
     * @param <T>     数据类型
     * @return 响应结果
     */
    public static <T> ApiResult<T> failure(Integer code, String message) {
        return ApiResult.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .build();
    }
}
```

文件位置：`src/main/java/io/github/atengk/sshd/common/exception/SshErrorCode.java`

下面的错误码枚举用于分类 SSH 模块异常。

```java
package io.github.atengk.sshd.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * SSH错误码
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Getter
@RequiredArgsConstructor
public enum SshErrorCode {

    /**
     * 认证失败
     */
    AUTH_FAILED(1001, "SSH认证失败"),

    /**
     * 权限不足
     */
    PERMISSION_DENIED(1002, "SSH权限不足"),

    /**
     * 未知命令
     */
    UNKNOWN_COMMAND(1003, "SSH命令不存在"),

    /**
     * 命令执行失败
     */
    COMMAND_EXECUTE_ERROR(1004, "SSH命令执行失败"),

    /**
     * 会话不存在
     */
    SESSION_NOT_FOUND(1005, "SSH会话不存在"),

    /**
     * 服务状态异常
     */
    SERVER_STATE_ERROR(1006, "SSH服务状态异常"),

    /**
     * 用户访问配置不存在
     */
    USER_ACCESS_NOT_FOUND(1007, "SSH用户访问配置不存在");

    private final Integer code;
    private final String message;
}
```

文件位置：`src/main/java/io/github/atengk/sshd/common/exception/SshBizException.java`

下面的业务异常用于抛出可识别的 SSH 模块错误。

```java
package io.github.atengk.sshd.common.exception;

import lombok.Getter;

/**
 * SSH业务异常
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Getter
public class SshBizException extends RuntimeException {

    private final SshErrorCode errorCode;

    /**
     * 创建SSH业务异常
     *
     * @param errorCode 错误码
     */
    public SshBizException(SshErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 创建SSH业务异常
     *
     * @param errorCode 错误码
     * @param message   错误消息
     */
    public SshBizException(SshErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
```

文件位置：`src/main/java/io/github/atengk/sshd/common/exception/GlobalExceptionHandler.java`

下面的全局异常处理器用于管理接口异常响应。

```java
package io.github.atengk.sshd.common.exception;

import io.github.atengk.sshd.common.ApiResult;
import lombok.extern.slf4j.Slf4j;
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
     * 处理SSH业务异常
     *
     * @param exception SSH业务异常
     * @return 响应结果
     */
    @ExceptionHandler(SshBizException.class)
    public ApiResult<Void> handleSshBizException(SshBizException exception) {
        log.warn("SSH业务异常，错误码：{}，错误信息：{}",
                exception.getErrorCode().getCode(), exception.getMessage());
        return ApiResult.failure(exception.getErrorCode().getCode(), exception.getMessage());
    }

    /**
     * 处理未知异常
     *
     * @param exception 异常
     * @return 响应结果
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception exception) {
        log.error("系统异常", exception);
        return ApiResult.failure(500, "系统异常，请联系管理员");
    }
}
```

异常分类建议如下：

| 类型         | 处理方式                                         |
| ------------ | ------------------------------------------------ |
| 认证失败     | 认证器返回 `false`，记录 `AUTH_FAILURE` 审计事件 |
| 权限不足     | 命令返回退出码 `126`，管理接口返回 `1002`        |
| 命令不存在   | 命令返回退出码 `127`                             |
| 命令执行异常 | 命令返回退出码 `1`，错误详情写入服务端日志       |
| 会话不存在   | 管理接口返回 `1005`                              |
| 服务状态异常 | 管理接口返回 `1006`                              |

## 接口与管理能力

接口与管理能力用于通过 HTTP 管理内嵌 SSH 服务，包括服务状态查询、在线会话管理和用户访问配置管理。生产环境必须给这些接口增加鉴权、操作审计和访问来源限制，避免未授权用户关闭 SSH 服务或修改 SSH 用户权限。

### SSH 服务状态查询

SSH 服务状态查询用于查看 SSH 服务是否启用、是否运行、监听地址、监听端口、SFTP 根目录和在线会话数量。该接口可用于管理后台、健康检查页面或运维脚本。

文件位置：`src/main/java/io/github/atengk/sshd/controller/SshServerController.java`

下面的 Controller 提供 SSH 服务状态查询、启动、停止和重启接口。

```java
package io.github.atengk.sshd.controller;

import io.github.atengk.sshd.common.ApiResult;
import io.github.atengk.sshd.config.SshdProperties;
import io.github.atengk.sshd.controller.vo.SshServerStatusVO;
import io.github.atengk.sshd.server.SshServerLifecycle;
import io.github.atengk.sshd.session.SshSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * SSH服务管理接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@RestController
@RequestMapping("/api/ssh/server")
@RequiredArgsConstructor
public class SshServerController {

    private final SshdProperties sshdProperties;
    private final SshServerLifecycle sshServerLifecycle;
    private final SshSessionRegistry sshSessionRegistry;

    /**
     * 查询SSH服务状态
     *
     * @return SSH服务状态
     */
    @GetMapping("/status")
    public ApiResult<SshServerStatusVO> status() {
        SshServerStatusVO status = SshServerStatusVO.builder()
                .enabled(sshdProperties.isEnabled())
                .running(sshServerLifecycle.isRunning())
                .host(sshdProperties.getHost())
                .port(sshdProperties.getPort())
                .sftpRootPath(sshdProperties.getSftpRootPath())
                .onlineSessionCount(sshSessionRegistry.listOnlineSessions().size())
                .build();

        return ApiResult.success(status);
    }

    /**
     * 启动SSH服务
     *
     * @return SSH服务状态
     */
    @PostMapping("/start")
    public ApiResult<SshServerStatusVO> start() {
        sshServerLifecycle.start();
        log.info("通过管理接口启动SSH服务");
        return status();
    }

    /**
     * 停止SSH服务
     *
     * @return SSH服务状态
     */
    @PostMapping("/stop")
    public ApiResult<SshServerStatusVO> stop() {
        sshServerLifecycle.stop();
        log.info("通过管理接口停止SSH服务");
        return status();
    }

    /**
     * 重启SSH服务
     *
     * @return SSH服务状态
     */
    @PostMapping("/restart")
    public ApiResult<SshServerStatusVO> restart() {
        sshServerLifecycle.restart();
        log.info("通过管理接口重启SSH服务");
        return status();
    }
}
```

接口调用示例：

```bash
# 查询SSH服务状态
curl -X GET http://127.0.0.1:8080/api/ssh/server/status

# 启动SSH服务
curl -X POST http://127.0.0.1:8080/api/ssh/server/start

# 停止SSH服务
curl -X POST http://127.0.0.1:8080/api/ssh/server/stop

# 重启SSH服务
curl -X POST http://127.0.0.1:8080/api/ssh/server/restart
```

响应示例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "enabled": true,
    "running": true,
    "host": "0.0.0.0",
    "port": 2222,
    "sftpRootPath": "./data/sshd/sftp",
    "onlineSessionCount": 1
  }
}
```

### 在线会话管理

在线会话管理用于查看当前 SSH 连接，并支持按会话 ID 关闭指定连接。该能力适合管理后台使用，例如发现异常客户端、长时间空闲连接或违规用户时，可以主动断开会话。

文件位置：`src/main/java/io/github/atengk/sshd/controller/SshSessionController.java`

下面的 Controller 提供在线会话列表和关闭会话接口。

```java
package io.github.atengk.sshd.controller;

import io.github.atengk.sshd.common.ApiResult;
import io.github.atengk.sshd.common.exception.SshBizException;
import io.github.atengk.sshd.common.exception.SshErrorCode;
import io.github.atengk.sshd.session.SshSessionInfo;
import io.github.atengk.sshd.session.SshSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SSH在线会话管理接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@RestController
@RequestMapping("/api/ssh/sessions")
@RequiredArgsConstructor
public class SshSessionController {

    private final SshSessionRegistry sshSessionRegistry;

    /**
     * 查询在线会话列表
     *
     * @return 在线会话列表
     */
    @GetMapping
    public ApiResult<List<SshSessionInfo>> listOnlineSessions() {
        return ApiResult.success(sshSessionRegistry.listOnlineSessions());
    }

    /**
     * 关闭指定会话
     *
     * @param sessionId 会话ID
     * @return 操作结果
     */
    @DeleteMapping("/{sessionId}")
    public ApiResult<Void> closeSession(@PathVariable String sessionId) {
        boolean closed = sshSessionRegistry.closeSession(sessionId);
        if (!closed) {
            throw new SshBizException(SshErrorCode.SESSION_NOT_FOUND);
        }

        log.info("通过管理接口关闭SSH会话，会话ID：{}", sessionId);
        return ApiResult.success(null);
    }
}
```

接口调用示例：

```bash
# 查询在线SSH会话
curl -X GET http://127.0.0.1:8080/api/ssh/sessions

# 关闭指定SSH会话
curl -X DELETE http://127.0.0.1:8080/api/ssh/sessions/123456789
```

在线会话响应示例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": [
    {
      "sessionId": "123456789",
      "username": "admin",
      "clientAddress": "/127.0.0.1:54621",
      "authenticated": true,
      "createdTime": "2026-05-07T10:20:30",
      "closedTime": null
    }
  ]
}
```

### 用户访问配置管理

用户访问配置管理用于维护允许登录 SSH 服务的用户、密码、公钥指纹、命令权限和 SFTP 根目录。示例使用内存存储，便于开发验证；生产环境建议改造为 MyBatis-Plus 持久化实现，并增加操作审计。

推荐新增以下文件：

```text
src/main/java/io/github/atengk/sshd/controller/dto/SshUserAccessSaveRequest.java
src/main/java/io/github/atengk/sshd/controller/vo/SshUserAccessVO.java
src/main/java/io/github/atengk/sshd/controller/SshUserAccessController.java
```

文件位置：`src/main/java/io/github/atengk/sshd/controller/dto/SshUserAccessSaveRequest.java`

下面的请求对象用于创建或更新 SSH 用户访问配置。

```java
package io.github.atengk.sshd.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Set;

/**
 * SSH用户访问配置保存请求
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class SshUserAccessSaveRequest {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 明文密码，为空时表示不修改密码
     */
    private String password;

    /**
     * 公钥指纹集合
     */
    private Set<String> publicKeyFingerprints;

    /**
     * 允许执行的命令集合
     */
    private Set<String> allowedCommands;

    /**
     * SFTP用户根目录
     */
    private String sftpRootPath;

    /**
     * 是否启用
     */
    private boolean enabled = true;
}
```

文件位置：`src/main/java/io/github/atengk/sshd/controller/vo/SshUserAccessVO.java`

下面的 VO 用于返回用户访问配置，不返回密码摘要。

```java
package io.github.atengk.sshd.controller.vo;

import io.github.atengk.sshd.auth.SshUserAccess;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * SSH用户访问配置响应
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
public class SshUserAccessVO {

    /**
     * 用户名
     */
    private String username;

    /**
     * 公钥指纹集合
     */
    private Set<String> publicKeyFingerprints;

    /**
     * 允许执行的命令集合
     */
    private Set<String> allowedCommands;

    /**
     * SFTP用户根目录
     */
    private String sftpRootPath;

    /**
     * 是否启用
     */
    private boolean enabled;

    /**
     * 转换用户访问配置
     *
     * @param access 用户访问配置
     * @return 响应对象
     */
    public static SshUserAccessVO from(SshUserAccess access) {
        return SshUserAccessVO.builder()
                .username(access.getUsername())
                .publicKeyFingerprints(access.getPublicKeyFingerprints())
                .allowedCommands(access.getAllowedCommands())
                .sftpRootPath(access.getSftpRootPath())
                .enabled(access.isEnabled())
                .build();
    }
}
```

需要将前面章节中的 `SshUserAccessService` 扩展为支持增删改查。

文件位置：`src/main/java/io/github/atengk/sshd/auth/SshUserAccessService.java`

下面是带管理能力的完整内存版用户访问服务。

```java
package io.github.atengk.sshd.auth;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import io.github.atengk.sshd.audit.SshAuditEvent;
import io.github.atengk.sshd.audit.SshAuditEventType;
import io.github.atengk.sshd.audit.SshAuditService;
import io.github.atengk.sshd.common.exception.SshBizException;
import io.github.atengk.sshd.common.exception.SshErrorCode;
import io.github.atengk.sshd.controller.dto.SshUserAccessSaveRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SSH用户访问服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SshUserAccessService {

    private final SshAuditService sshAuditService;

    private final ConcurrentMap<String, SshUserAccess> userStore = new ConcurrentHashMap<>();

    /**
     * 初始化示例用户
     */
    @PostConstruct
    public void init() {
        userStore.put("admin", SshUserAccess.builder()
                .username("admin")
                .passwordHash(BCrypt.hashpw("123456", BCrypt.gensalt()))
                .publicKeyFingerprints(Set.of())
                .allowedCommands(Set.of("help", "status"))
                .sftpRootPath("./data/sshd/sftp/admin")
                .enabled(true)
                .build());

        log.info("SSH示例用户初始化完成，用户数量：{}", userStore.size());
    }

    /**
     * 查询全部用户
     *
     * @return 用户访问配置列表
     */
    public List<SshUserAccess> listUsers() {
        return CollUtil.newArrayList(userStore.values());
    }

    /**
     * 查询用户配置
     *
     * @param username 用户名
     * @return 用户访问配置
     */
    public SshUserAccess getUser(String username) {
        return findEnabledOrDisabledUser(username)
                .orElseThrow(() -> new SshBizException(SshErrorCode.USER_ACCESS_NOT_FOUND));
    }

    /**
     * 保存用户配置
     *
     * @param request 保存请求
     * @return 用户访问配置
     */
    public SshUserAccess saveUser(SshUserAccessSaveRequest request) {
        SshUserAccess oldAccess = userStore.get(request.getUsername());

        String passwordHash = oldAccess == null ? null : oldAccess.getPasswordHash();
        if (StrUtil.isNotBlank(request.getPassword())) {
            passwordHash = BCrypt.hashpw(request.getPassword(), BCrypt.gensalt());
        }

        SshUserAccess access = SshUserAccess.builder()
                .username(request.getUsername())
                .passwordHash(passwordHash)
                .publicKeyFingerprints(toImmutableSet(request.getPublicKeyFingerprints()))
                .allowedCommands(toImmutableSet(request.getAllowedCommands()))
                .sftpRootPath(StrUtil.blankToDefault(request.getSftpRootPath(), "./data/sshd/sftp/" + request.getUsername()))
                .enabled(request.isEnabled())
                .build();

        userStore.put(request.getUsername(), access);
        log.info("SSH用户访问配置已保存，用户：{}", request.getUsername());

        sshAuditService.record(SshAuditEvent.builder()
                .eventType(SshAuditEventType.USER_ACCESS_CHANGE)
                .username(request.getUsername())
                .success(true)
                .message("SSH用户访问配置已保存")
                .occurredTime(LocalDateTime.now())
                .build());

        return access;
    }

    /**
     * 删除用户配置
     *
     * @param username 用户名
     */
    public void deleteUser(String username) {
        SshUserAccess removed = userStore.remove(username);
        if (removed == null) {
            throw new SshBizException(SshErrorCode.USER_ACCESS_NOT_FOUND);
        }

        log.info("SSH用户访问配置已删除，用户：{}", username);
        sshAuditService.record(SshAuditEvent.builder()
                .eventType(SshAuditEventType.USER_ACCESS_CHANGE)
                .username(username)
                .success(true)
                .message("SSH用户访问配置已删除")
                .occurredTime(LocalDateTime.now())
                .build());
    }

    /**
     * 查询可用用户
     *
     * @param username 用户名
     * @return 用户访问配置
     */
    public Optional<SshUserAccess> findEnabledUser(String username) {
        if (StrUtil.isBlank(username)) {
            return Optional.empty();
        }

        SshUserAccess access = userStore.get(username);
        if (access == null || !access.isEnabled()) {
            return Optional.empty();
        }

        return Optional.of(access);
    }

    /**
     * 校验用户密码
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 是否通过
     */
    public boolean verifyPassword(String username, String password) {
        Optional<SshUserAccess> optionalAccess = findEnabledUser(username);
        if (optionalAccess.isEmpty()) {
            log.warn("SSH密码认证失败，用户不存在或已禁用，用户：{}", username);
            return false;
        }

        SshUserAccess access = optionalAccess.get();
        if (StrUtil.isBlank(access.getPasswordHash())) {
            log.warn("SSH密码认证失败，用户未配置密码，用户：{}", username);
            return false;
        }

        boolean matched = BCrypt.checkpw(password, access.getPasswordHash());
        if (!matched) {
            log.warn("SSH密码认证失败，密码不匹配，用户：{}", username);
        }

        return matched;
    }

    /**
     * 校验公钥指纹
     *
     * @param username    用户名
     * @param fingerprint 公钥指纹
     * @return 是否通过
     */
    public boolean verifyPublicKeyFingerprint(String username, String fingerprint) {
        Optional<SshUserAccess> optionalAccess = findEnabledUser(username);
        if (optionalAccess.isEmpty()) {
            log.warn("SSH公钥认证失败，用户不存在或已禁用，用户：{}", username);
            return false;
        }

        SshUserAccess access = optionalAccess.get();
        boolean matched = CollUtil.isNotEmpty(access.getPublicKeyFingerprints())
                && access.getPublicKeyFingerprints().contains(fingerprint);

        if (!matched) {
            log.warn("SSH公钥认证失败，指纹不匹配，用户：{}，指纹：{}", username, fingerprint);
        }

        return matched;
    }

    /**
     * 判断用户是否可以执行命令
     *
     * @param username 用户名
     * @param command  命令名称
     * @return 是否允许
     */
    public boolean canExecuteCommand(String username, String command) {
        Optional<SshUserAccess> optionalAccess = findEnabledUser(username);
        if (optionalAccess.isEmpty()) {
            return false;
        }

        SshUserAccess access = optionalAccess.get();
        return CollUtil.isNotEmpty(access.getAllowedCommands())
                && access.getAllowedCommands().contains(command);
    }

    /**
     * 解析用户SFTP根目录
     *
     * @param username 用户名
     * @return SFTP根目录
     */
    public Optional<String> resolveSftpRootPath(String username) {
        return findEnabledUser(username)
                .map(SshUserAccess::getSftpRootPath)
                .filter(StrUtil::isNotBlank);
    }

    /**
     * 查询用户，包含禁用用户
     *
     * @param username 用户名
     * @return 用户访问配置
     */
    private Optional<SshUserAccess> findEnabledOrDisabledUser(String username) {
        if (StrUtil.isBlank(username)) {
            return Optional.empty();
        }

        return Optional.ofNullable(userStore.get(username));
    }

    /**
     * 转换不可变集合
     *
     * @param values 原始集合
     * @return 不可变集合
     */
    private Set<String> toImmutableSet(Set<String> values) {
        if (CollUtil.isEmpty(values)) {
            return Set.of();
        }
        return Set.copyOf(values);
    }
}
```

文件位置：`src/main/java/io/github/atengk/sshd/controller/SshUserAccessController.java`

下面的 Controller 提供 SSH 用户访问配置的查询、新增、修改和删除接口。

```java
package io.github.atengk.sshd.controller;

import io.github.atengk.sshd.auth.SshUserAccess;
import io.github.atengk.sshd.auth.SshUserAccessService;
import io.github.atengk.sshd.common.ApiResult;
import io.github.atengk.sshd.controller.dto.SshUserAccessSaveRequest;
import io.github.atengk.sshd.controller.vo.SshUserAccessVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SSH用户访问配置管理接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@RestController
@RequestMapping("/api/ssh/users")
@RequiredArgsConstructor
public class SshUserAccessController {

    private final SshUserAccessService sshUserAccessService;

    /**
     * 查询用户访问配置列表
     *
     * @return 用户访问配置列表
     */
    @GetMapping
    public ApiResult<List<SshUserAccessVO>> listUsers() {
        List<SshUserAccessVO> users = sshUserAccessService.listUsers()
                .stream()
                .map(SshUserAccessVO::from)
                .toList();

        return ApiResult.success(users);
    }

    /**
     * 查询用户访问配置
     *
     * @param username 用户名
     * @return 用户访问配置
     */
    @GetMapping("/{username}")
    public ApiResult<SshUserAccessVO> getUser(@PathVariable String username) {
        SshUserAccess access = sshUserAccessService.getUser(username);
        return ApiResult.success(SshUserAccessVO.from(access));
    }

    /**
     * 保存用户访问配置
     *
     * @param request 保存请求
     * @return 用户访问配置
     */
    @PostMapping
    public ApiResult<SshUserAccessVO> saveUser(@Valid @RequestBody SshUserAccessSaveRequest request) {
        SshUserAccess access = sshUserAccessService.saveUser(request);
        log.info("通过管理接口保存SSH用户访问配置，用户：{}", request.getUsername());
        return ApiResult.success(SshUserAccessVO.from(access));
    }

    /**
     * 删除用户访问配置
     *
     * @param username 用户名
     * @return 操作结果
     */
    @DeleteMapping("/{username}")
    public ApiResult<Void> deleteUser(@PathVariable String username) {
        sshUserAccessService.deleteUser(username);
        log.info("通过管理接口删除SSH用户访问配置，用户：{}", username);
        return ApiResult.success(null);
    }
}
```

用户配置接口调用示例：

```bash
# 查询SSH用户列表
curl -X GET http://127.0.0.1:8080/api/ssh/users

# 查询指定SSH用户
curl -X GET http://127.0.0.1:8080/api/ssh/users/admin

# 新增或更新SSH用户
curl -X POST http://127.0.0.1:8080/api/ssh/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ops",
    "password": "123456",
    "publicKeyFingerprints": [],
    "allowedCommands": ["help", "status"],
    "sftpRootPath": "./data/sshd/sftp/ops",
    "enabled": true
  }'

# 删除SSH用户
curl -X DELETE http://127.0.0.1:8080/api/ssh/users/ops
```

新增用户后可以通过 SSH 和 SFTP 验证：

```bash
# 验证命令执行权限
ssh -p 2222 ops@127.0.0.1 status

# 验证SFTP目录隔离
sftp -P 2222 ops@127.0.0.1
```

生产环境改造建议：

| 项目     | 建议                                               |
| -------- | -------------------------------------------------- |
| 用户存储 | 使用 MyBatis-Plus 落库，不使用内存 Map             |
| 密码存储 | 只保存 BCrypt 哈希，不保存明文密码                 |
| 公钥管理 | 保存公钥内容、指纹、启用状态、过期时间             |
| 命令权限 | 命令权限独立表维护，支持角色或租户维度             |
| 管理接口 | 接入 Spring Security、Sa-Token 或网关鉴权          |
| 操作审计 | 所有用户配置变更必须记录操作人、来源 IP 和变更内容 |

## 测试与验证

测试与验证用于确认 SSH 服务端启动、命令执行、SFTP 文件传输和认证失败处理是否符合预期。建议先使用命令行工具完成黑盒验证，再补充 Spring Boot 集成测试。该部分继续基于原开发大纲展开。

### 本地 SSH 连接测试

本地 SSH 连接测试用于验证服务是否正常监听端口、用户是否可以登录、命令是否可以执行、命令权限是否生效。测试前需要确认 Spring Boot 应用已启动，并且 `app.sshd.enabled=true`。

启动应用：

```bash
# 使用 Maven 启动应用
mvn spring-boot:run

# 或先打包再启动
mvn clean package -DskipTests
java -jar target/springboot-mina-sshd.jar
```

上述命令分别适用于开发调试和本地打包验证。`mvn spring-boot:run` 适合开发阶段快速启动，`java -jar` 更接近生产运行方式。

检查端口监听状态：

```bash
# 查看2222端口是否已监听
ss -lntp | grep 2222

# 兼容部分旧系统
netstat -lntp | grep 2222
```

如果端口正常监听，应能看到 `0.0.0.0:2222` 或指定内网 IP 的监听记录。

使用 SSH 客户端验证命令执行：

```bash
# 查看命令帮助
ssh -p 2222 admin@127.0.0.1 help

# 查看SSH服务状态
ssh -p 2222 admin@127.0.0.1 status

# 测试未知命令
ssh -p 2222 admin@127.0.0.1 version
```

`-p 2222` 表示连接内嵌 SSH 服务端口，`admin@127.0.0.1` 表示使用 `admin` 用户连接本机服务。首次连接时，客户端可能提示是否信任服务端主机指纹，输入 `yes` 后继续。

预期结果如下：

| 测试项     | 命令       | 预期结果                                   |
| ---------- | ---------- | ------------------------------------------ |
| 帮助命令   | `help`     | 返回支持的命令列表                         |
| 状态命令   | `status`   | 返回启用状态、监听地址、端口、在线会话数量 |
| 未知命令   | `version`  | 返回未知命令，退出码为 `127`               |
| 无权限命令 | 未授权命令 | 返回权限不足，退出码为 `126`               |

查看命令退出码：

```bash
# 执行未知命令
ssh -p 2222 admin@127.0.0.1 version

# 查看上一条命令退出码
echo $?
```

如果命令不存在，预期退出码为 `127`；如果用户无权限执行，预期退出码为 `126`；如果执行成功，预期退出码为 `0`。

也可以通过 HTTP 管理接口验证 SSH 服务状态：

```bash
# 查询SSH服务状态
curl -X GET http://127.0.0.1:8080/api/ssh/server/status

# 查询在线会话
curl -X GET http://127.0.0.1:8080/api/ssh/sessions
```

响应示例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "enabled": true,
    "running": true,
    "host": "0.0.0.0",
    "port": 2222,
    "sftpRootPath": "./data/sshd/sftp",
    "onlineSessionCount": 0
  }
}
```

### SFTP 功能测试

SFTP 功能测试用于验证文件上传、下载、删除、目录创建和用户根目录隔离是否正常。测试前需要确认项目已引入 `sshd-sftp` 依赖，并且 `SftpSubsystemFactory` 和 `UserRootFileSystemFactory` 已注册到 `SshServer`。

准备本地测试文件：

```bash
# 创建本地测试文件
echo "hello apache mina sshd sftp" > /tmp/sshd-sftp-demo.txt

# 查看文件内容
cat /tmp/sshd-sftp-demo.txt
```

连接 SFTP 服务：

```bash
# 使用admin用户连接SFTP服务
sftp -P 2222 admin@127.0.0.1
```

注意 `sftp` 命令指定端口使用大写 `-P`，而 `ssh` 命令指定端口使用小写 `-p`。

进入 SFTP 交互终端后执行：

```bash
# 查看当前服务端目录
pwd

# 创建上传目录
mkdir inbound

# 上传文件
put /tmp/sshd-sftp-demo.txt inbound/sshd-sftp-demo.txt

# 查看上传结果
ls inbound

# 下载文件
get inbound/sshd-sftp-demo.txt /tmp/sshd-sftp-demo-download.txt

# 删除服务端测试文件
rm inbound/sshd-sftp-demo.txt

# 退出SFTP
bye
```

退出后检查下载文件：

```bash
# 查看下载文件内容
cat /tmp/sshd-sftp-demo-download.txt

# 对比上传前和下载后的文件
diff /tmp/sshd-sftp-demo.txt /tmp/sshd-sftp-demo-download.txt
```

如果 `diff` 没有输出，表示上传和下载后的文件内容一致。

检查服务端用户目录：

```bash
# 查看admin用户SFTP目录结构
find ./data/sshd/sftp/admin -maxdepth 3 -print
```

用户目录隔离测试：

```bash
# 在SFTP交互终端中尝试访问上级目录
cd ..

# 查看当前目录
pwd

# 尝试访问系统目录
cd /etc
```

预期结果是客户端始终被限制在用户虚拟根目录下，不能访问宿主机真实 `/etc`、`/root` 或应用目录之外的路径。

SFTP 功能验证清单：

| 测试项     | 操作                           | 预期结果             |
| ---------- | ------------------------------ | -------------------- |
| 登录 SFTP  | `sftp -P 2222 admin@127.0.0.1` | 登录成功             |
| 创建目录   | `mkdir inbound`                | 目录创建成功         |
| 上传文件   | `put 本地文件 服务端路径`      | 文件上传成功         |
| 下载文件   | `get 服务端路径 本地路径`      | 文件下载成功         |
| 删除文件   | `rm 服务端文件`                | 文件删除成功         |
| 根目录隔离 | `cd ..`、`cd /etc`             | 不能越权访问系统目录 |

### 认证失败场景测试

认证失败场景测试用于验证密码错误、用户不存在、用户禁用、公钥不匹配、空密码和权限不足等异常流程是否符合预期。认证失败不应暴露过多内部细节，服务端日志和审计日志应记录具体原因。

测试密码错误：

```bash
# 使用错误密码连接
ssh -p 2222 admin@127.0.0.1 status
```

输入错误密码后，客户端应提示认证失败；服务端日志应出现类似记录：

```text
SSH密码认证失败，密码不匹配，用户：admin
```

测试不存在的用户：

```bash
# 使用不存在的用户连接
ssh -p 2222 nobody@127.0.0.1 status
```

服务端应拒绝认证，并记录用户不存在或已禁用。

测试禁用用户：

```bash
# 禁用ops用户
curl -X POST http://127.0.0.1:8080/api/ssh/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ops",
    "password": "123456",
    "publicKeyFingerprints": [],
    "allowedCommands": ["help", "status"],
    "sftpRootPath": "./data/sshd/sftp/ops",
    "enabled": false
  }'

# 使用禁用用户连接
ssh -p 2222 ops@127.0.0.1 status
```

预期结果是 `ops` 用户认证失败，不能执行命令，也不能进入 SFTP。

测试权限不足：

```bash
# 创建只允许help命令的用户
curl -X POST http://127.0.0.1:8080/api/ssh/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "limited",
    "password": "123456",
    "publicKeyFingerprints": [],
    "allowedCommands": ["help"],
    "sftpRootPath": "./data/sshd/sftp/limited",
    "enabled": true
  }'

# 执行允许的命令
ssh -p 2222 limited@127.0.0.1 help

# 执行未授权命令
ssh -p 2222 limited@127.0.0.1 status

# 查看退出码
echo $?
```

`help` 应执行成功，`status` 应返回权限不足，退出码应为 `126`。

测试公钥认证失败时，可以先生成测试密钥：

```bash
# 生成测试密钥
ssh-keygen -t rsa -b 4096 -f /tmp/sshd-test-key -N ""

# 查看公钥指纹
ssh-keygen -lf /tmp/sshd-test-key.pub

# 使用未授权私钥连接
ssh -i /tmp/sshd-test-key -p 2222 admin@127.0.0.1 status
```

如果用户未配置该公钥指纹，服务端应拒绝公钥认证。若同时启用了密码认证，客户端可能会继续尝试密码登录；这属于 SSH 客户端认证回退行为。

认证失败验证清单：

| 场景       | 预期结果                   |
| ---------- | -------------------------- |
| 密码错误   | 认证失败，记录失败日志     |
| 用户不存在 | 认证失败，不创建已认证会话 |
| 用户禁用   | 认证失败，不能执行命令     |
| 公钥未授权 | 公钥认证失败               |
| 未授权命令 | 返回权限不足，退出码 `126` |
| 未知命令   | 返回未知命令，退出码 `127` |

## 部署与运行

部署与运行用于说明如何在 Linux 环境中安装、配置、启动、停止和排查 Spring Boot 3 + Apache MINA SSHD 应用。生产环境建议使用独立运行用户、固定数据目录、持久化主机密钥、systemd 托管进程，并限制 SSH 服务端口访问来源。

### 应用配置示例

生产环境建议使用独立的 `application-prod.yml`，不要直接复用本地开发配置。主机密钥和 SFTP 根目录应放到持久化数据目录，日志目录应和应用包目录分离。

文件位置：`src/main/resources/application-prod.yml`

下面的生产配置示例用于 Linux 环境运行。

```yaml
server:
  port: 8080 # HTTP管理接口端口，建议仅内网访问

spring:
  application:
    name: springboot-mina-sshd # 应用名称
  profiles:
    active: prod # 当前运行环境

app:
  sshd:
    enabled: true # 是否启用内嵌SSH服务
    host: 0.0.0.0 # 监听地址，生产环境可改为内网IP
    port: 2222 # SSH服务端口，不建议使用22
    host-key-path: /data/app/springboot-mina-sshd/sshd/hostkey.ser # 主机密钥持久化路径
    sftp-root-path: /data/app/springboot-mina-sshd/sshd/sftp # SFTP文件根目录
    idle-timeout: 600s # 空闲超时时间
    auth:
      password-enabled: false # 生产环境建议关闭密码认证
      public-key-enabled: true # 生产环境建议使用公钥认证
      max-auth-failures: 5 # 最大认证失败次数

logging:
  file:
    name: /data/logs/springboot-mina-sshd/app.log # 应用日志文件
  level:
    root: info # 根日志级别
    io.github.atengk.sshd: info # SSH模块日志级别
    org.apache.sshd: info # Apache MINA SSHD日志级别
```

配置说明：

| 配置项                    | 生产建议                         |
| ------------------------- | -------------------------------- |
| `app.sshd.port`           | 使用固定非 22 端口，例如 `2222`  |
| `app.sshd.host-key-path`  | 持久化，避免服务端指纹变化       |
| `app.sshd.sftp-root-path` | 使用独立数据目录                 |
| `password-enabled`        | 公网或生产环境建议关闭           |
| `public-key-enabled`      | 推荐开启                         |
| `logging.file.name`       | 写入独立日志目录，便于采集和轮转 |

如果使用外部配置文件启动，可以将配置放到服务器目录：

文件位置：`/data/app/springboot-mina-sshd/config/application-prod.yml`

```yaml
server:
  port: 8080 # HTTP管理接口端口

spring:
  application:
    name: springboot-mina-sshd # 应用名称

app:
  sshd:
    enabled: true # 是否启用SSH服务
    host: 0.0.0.0 # SSH监听地址
    port: 2222 # SSH监听端口
    host-key-path: /data/app/springboot-mina-sshd/sshd/hostkey.ser # 主机密钥路径
    sftp-root-path: /data/app/springboot-mina-sshd/sshd/sftp # SFTP根目录
    idle-timeout: 600s # 会话空闲超时
    auth:
      password-enabled: false # 生产环境建议关闭密码认证
      public-key-enabled: true # 启用公钥认证
      max-auth-failures: 5 # 最大认证失败次数

logging:
  file:
    name: /data/logs/springboot-mina-sshd/app.log # 应用日志
  level:
    io.github.atengk.sshd: info # SSH模块日志
    org.apache.sshd: info # SSHD框架日志
```

### Linux 环境运行

Linux 环境运行建议使用专用用户启动应用，避免使用 `root` 用户直接运行。应用目录、日志目录、SFTP 根目录应提前创建并设置权限。

创建运行用户和目录：

```bash
# 创建应用用户
sudo useradd -r -s /sbin/nologin appuser

# 创建应用目录、配置目录、SSH数据目录和日志目录
sudo mkdir -p /data/app/springboot-mina-sshd/config
sudo mkdir -p /data/app/springboot-mina-sshd/sshd/sftp
sudo mkdir -p /data/logs/springboot-mina-sshd

# 设置目录归属
sudo chown -R appuser:appuser /data/app/springboot-mina-sshd
sudo chown -R appuser:appuser /data/logs/springboot-mina-sshd

# 设置基础权限
sudo chmod -R 750 /data/app/springboot-mina-sshd
sudo chmod -R 750 /data/logs/springboot-mina-sshd
```

上述命令会创建专用系统用户和运行目录。`750` 权限表示目录所有者可读写执行，同组用户可读执行，其他用户无权限访问。

上传应用包和配置文件：

```bash
# 上传jar包到应用目录，示例使用scp
scp target/springboot-mina-sshd.jar root@服务器IP:/data/app/springboot-mina-sshd/

# 上传生产配置文件
scp application-prod.yml root@服务器IP:/data/app/springboot-mina-sshd/config/

# 修正文件归属
ssh root@服务器IP "chown -R appuser:appuser /data/app/springboot-mina-sshd"
```

手动启动验证：

```bash
# 切换到应用目录
cd /data/app/springboot-mina-sshd

# 使用prod配置启动应用
sudo -u appuser java \
  -jar /data/app/springboot-mina-sshd/springboot-mina-sshd.jar \
  --spring.profiles.active=prod \
  --spring.config.additional-location=/data/app/springboot-mina-sshd/config/
```

该命令使用 `appuser` 启动应用，并加载外部配置目录。验证无误后，建议使用 systemd 托管。

文件位置：`/etc/systemd/system/springboot-mina-sshd.service`

下面的 systemd 配置用于托管 Spring Boot 应用进程。

```ini
[Unit]
Description=Spring Boot Apache MINA SSHD Service
After=network.target

[Service]
Type=simple
User=appuser
Group=appuser
WorkingDirectory=/data/app/springboot-mina-sshd
ExecStart=/usr/bin/java -jar /data/app/springboot-mina-sshd/springboot-mina-sshd.jar --spring.profiles.active=prod --spring.config.additional-location=/data/app/springboot-mina-sshd/config/
Restart=on-failure
RestartSec=10

# JVM退出码143通常表示收到SIGTERM，属于正常停止
SuccessExitStatus=143

# 限制打开文件数，SFTP并发较高时可适当调大
LimitNOFILE=65535

[Install]
WantedBy=multi-user.target
```

加载并启动服务：

```bash
# 重新加载systemd配置
sudo systemctl daemon-reload

# 设置开机自启
sudo systemctl enable springboot-mina-sshd

# 启动服务
sudo systemctl start springboot-mina-sshd

# 查看服务状态
sudo systemctl status springboot-mina-sshd

# 查看实时日志
sudo journalctl -u springboot-mina-sshd -f
```

开放防火墙端口：

```bash
# firewalld环境开放SSH应用端口
sudo firewall-cmd --permanent --add-port=2222/tcp
sudo firewall-cmd --reload

# 查看端口是否开放
sudo firewall-cmd --list-ports
```

如果使用云服务器，还需要在安全组中放行 `2222/tcp`，并尽量限制来源 IP。

部署后验证：

```bash
# 查看HTTP状态接口
curl -X GET http://服务器IP:8080/api/ssh/server/status

# 验证SSH命令
ssh -p 2222 admin@服务器IP help

# 验证SFTP连接
sftp -P 2222 admin@服务器IP
```

### 常见问题排查

常见问题排查用于定位端口监听失败、认证失败、SFTP 无法上传、主机指纹变化、systemd 启动失败等问题。建议按照“进程状态、端口状态、配置文件、权限、日志、客户端命令”的顺序检查。

| 问题             | 可能原因                                       | 处理方式                                         |
| ---------------- | ---------------------------------------------- | ------------------------------------------------ |
| SSH 端口未监听   | 应用未启动、`app.sshd.enabled=false`、启动异常 | 查看应用日志和 `/api/ssh/server/status`          |
| 端口被占用       | 2222 已被其他进程占用                          | 使用 `ss -lntp                                   |
| 客户端连接超时   | 防火墙或安全组未放行                           | 放行 `2222/tcp` 并限制来源 IP                    |
| 密码认证失败     | 密码错误、用户禁用、密码认证关闭               | 检查用户配置和 `password-enabled`                |
| 公钥认证失败     | 指纹未配置、公钥格式错误、用户禁用             | 使用 `ssh-keygen -lf` 检查指纹                   |
| SFTP 上传失败    | 目录权限不足、根目录不存在、磁盘满             | 检查目录归属、权限和磁盘空间                     |
| 主机指纹变化     | `hostkey.ser` 被删除或重新生成                 | 恢复持久化主机密钥，必要时清理客户端 known_hosts |
| systemd 启动失败 | Java 路径错误、jar 不存在、配置路径错误        | 使用 `journalctl -u 服务名 -xe` 查看原因         |

检查端口占用：

```bash
# 查看2222端口占用
sudo ss -lntp | grep 2222

# 查看8080端口占用
sudo ss -lntp | grep 8080
```

如果端口已被占用，需要修改 `app.sshd.port` 或停止占用端口的进程。

检查应用日志：

```bash
# 查看systemd日志
sudo journalctl -u springboot-mina-sshd -n 200

# 实时查看应用日志文件
sudo tail -f /data/logs/springboot-mina-sshd/app.log
```

如果 SSH 服务启动失败，重点搜索以下关键词：

```bash
# 搜索SSH相关错误
grep -E "SSH|SSHD|SFTP|Exception|ERROR|启动失败|认证失败" /data/logs/springboot-mina-sshd/app.log
```

检查目录权限：

```bash
# 查看应用目录权限
ls -ld /data/app/springboot-mina-sshd
ls -ld /data/app/springboot-mina-sshd/sshd
ls -ld /data/app/springboot-mina-sshd/sshd/sftp

# 修正目录归属
sudo chown -R appuser:appuser /data/app/springboot-mina-sshd/sshd
sudo chmod -R 750 /data/app/springboot-mina-sshd/sshd
```

如果 SFTP 上传失败，通常是运行用户对 `sftp-root-path` 没有写权限，或者磁盘空间不足。

检查磁盘空间：

```bash
# 查看磁盘空间
df -h

# 查看SFTP目录占用
du -sh /data/app/springboot-mina-sshd/sshd/sftp
```

处理客户端主机指纹变化：

```bash
# 删除指定主机和端口的known_hosts记录
ssh-keygen -R "[服务器IP]:2222"

# 重新连接并确认新指纹
ssh -p 2222 admin@服务器IP help
```

如果生产环境频繁出现主机指纹变化，说明 `host-key-path` 没有持久化或部署过程误删了主机密钥文件。应固定 `host-key-path`，并确保发布流程不会清理该文件。

检查 systemd 服务：

```bash
# 查看服务状态
sudo systemctl status springboot-mina-sshd

# 查看详细启动错误
sudo journalctl -u springboot-mina-sshd -xe

# 修改服务文件后重新加载
sudo systemctl daemon-reload

# 重启服务
sudo systemctl restart springboot-mina-sshd
```

排查顺序建议：

```text
确认systemd服务是否运行
  ↓
确认Java进程是否存在
  ↓
确认8080和2222端口是否监听
  ↓
确认配置文件是否加载
  ↓
确认host-key-path和sftp-root-path权限
  ↓
确认用户认证配置
  ↓
确认客户端命令参数是否正确
  ↓
查看应用日志和审计日志
```

完成以上测试与部署配置后，Spring Boot 3 + Apache MINA SSHD 模块已经具备本地验证、SFTP 文件验证、认证失败验证、Linux 生产运行和常见问题排查的完整闭环。
