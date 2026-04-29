# Apache MINA SSHD 基础配置

后续内容可以统一基于这个底座展开：命令执行、SFTP 上传下载、SCP、批量主机执行、执行结果封装、审计日志等都复用同一个 `SshClient` 和 `SshSessionFactory`。

Apache MINA SSHD 是纯 Java SSH 协议库，覆盖 client/server 侧能力，底层默认可使用 Java `AsynchronousSocketChannel`。这里使用它作为 Spring Boot 3 + JDK 21 的 SSH 基础组件。当前 Maven Central 上 `2.17.1` 是 2.x 稳定版本，`3.0.0-M3` 属于里程碑版本，因此基础配置先使用 `2.17.1`。([Apache MINA](https://mina.apache.org/sshd-project/?utm_source=chatgpt.com))

## 文件结构

本基础配置先放在 `ssh` 模块包下，后续所有 SSH 能力都基于这些类扩展。

```text
src/main/java/io/github/atengk/ssh/config/SshProperties.java
src/main/java/io/github/atengk/ssh/config/SshClientConfiguration.java
src/main/java/io/github/atengk/ssh/core/SshSessionFactory.java
src/main/java/io/github/atengk/ssh/exception/SshOperationException.java
src/main/resources/application.yml
```

## Maven 依赖

这里先引入 `sshd-core`、`sshd-sftp`、`sshd-scp`。虽然基础配置阶段主要使用 `sshd-core`，但后续文件上传下载和 SCP 传输会直接用到另外两个模块。

文件位置：`pom.xml`

```xml
<properties>
    <!-- JDK 21 -->
    <java.version>21</java.version>

    <!-- Apache MINA SSHD 2.x 稳定版本 -->
    <apache-mina-sshd.version>2.17.1</apache-mina-sshd.version>

    <!-- Hutool 工具类版本，可按项目统一版本管理 -->
    <hutool.version>5.8.38</hutool.version>
</properties>

<dependencies>
    <!-- Spring Boot Web，后续如果要提供 SSH 操作接口可以直接复用 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot 参数校验，用于校验 SSH 配置项 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Apache MINA SSHD 核心模块：SSH 客户端、会话、认证、Channel 等 -->
    <dependency>
        <groupId>org.apache.sshd</groupId>
        <artifactId>sshd-core</artifactId>
        <version>${apache-mina-sshd.version}</version>
    </dependency>

    <!-- Apache MINA SSHD SFTP 模块：后续文件上传、下载、目录操作使用 -->
    <dependency>
        <groupId>org.apache.sshd</groupId>
        <artifactId>sshd-sftp</artifactId>
        <version>${apache-mina-sshd.version}</version>
    </dependency>

    <!-- Apache MINA SSHD SCP 模块：后续 SCP 传输使用 -->
    <dependency>
        <groupId>org.apache.sshd</groupId>
        <artifactId>sshd-scp</artifactId>
        <version>${apache-mina-sshd.version}</version>
    </dependency>

    <!-- Hutool 工具类：字符串、断言、集合、IO 等辅助处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok：减少配置类和构造器样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

## application.yml 配置

这里先配置一个默认 SSH 主机，后续如果要支持多主机，可以在这个结构上扩展 `hosts` 列表或接入数据库表。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

ssh:
  # 是否启用严格主机密钥校验
  # 生产环境建议 true，并提前维护 known_hosts 文件
  strict-host-key-checking: false

  # known_hosts 文件路径
  # strict-host-key-checking=true 时会使用该文件校验服务器公钥
  known-hosts-path: ${user.home}/.ssh/known_hosts

  # 建立 TCP/SSH 连接超时时间
  connect-timeout: 10s

  # SSH 认证超时时间
  auth-timeout: 10s

  # 会话关闭等待时间
  session-close-timeout: 3s

  # 默认连接主机，后续命令执行、SFTP、SCP 默认使用它
  default-host:
    host: 192.168.1.100
    port: 22
    username: root

    # 密码认证，和 private-key-path 二选一或同时配置
    password: "your_password"

    # 私钥认证，不使用可以留空
    private-key-path:

    # 私钥密码，无密码私钥可以留空
    private-key-passphrase:
```

## 配置属性类

这个类负责把 `application.yml` 中的 `ssh` 配置绑定成 Java 对象。后续如果改成多主机配置，可以继续在这里加 `Map<String, Host>` 或 `List<Host>`。

文件位置：`src/main/java/io/github/atengk/ssh/config/SshProperties.java`

```java
package io.github.atengk.ssh.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * SSH 配置属性
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Data
@Validated
@ConfigurationProperties(prefix = "ssh")
public class SshProperties {

    /**
     * 是否启用严格主机密钥校验
     */
    private Boolean strictHostKeyChecking = Boolean.TRUE;

    /**
     * known_hosts 文件路径
     */
    private String knownHostsPath = System.getProperty("user.home") + "/.ssh/known_hosts";

    /**
     * SSH 连接超时时间
     */
    private Duration connectTimeout = Duration.ofSeconds(10);

    /**
     * SSH 认证超时时间
     */
    private Duration authTimeout = Duration.ofSeconds(10);

    /**
     * SSH 会话关闭等待时间
     */
    private Duration sessionCloseTimeout = Duration.ofSeconds(3);

    /**
     * 默认 SSH 主机
     */
    private Host defaultHost = new Host();

    /**
     * SSH 主机配置
     *
     * @author Ateng
     * @since 2026-04-29
     */
    @Data
    public static class Host {

        /**
         * 主机地址
         */
        private String host;

        /**
         * SSH 端口
         */
        private Integer port = 22;

        /**
         * 登录用户名
         */
        private String username;

        /**
         * 登录密码
         */
        private String password;

        /**
         * 私钥文件路径
         */
        private String privateKeyPath;

        /**
         * 私钥密码
         */
        private String privateKeyPassphrase;
    }

}
```

## SshClient 生命周期配置

`SshClient` 应该作为 Spring Bean 管理生命周期，应用启动时初始化，应用关闭时释放资源。Apache MINA SSHD 的客户端会话认证流程是：先通过 `SshClient` 建立 `ClientSession`，再添加密码或公钥身份，最后调用 `auth()` 认证；后续命令执行、SFTP、SCP 都基于认证后的 `ClientSession`。([JavaDoc](https://javadoc.io/static/org.apache.sshd/sshd-core/2.1.0/org/apache/sshd/client/session/ClientSession.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/ssh/config/SshClientConfiguration.java`

```java
package io.github.atengk.ssh.config;

import cn.hutool.core.lang.BooleanUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.keyverifier.DefaultKnownHostsServerKeyVerifier;
import org.apache.sshd.client.keyverifier.RejectAllServerKeyVerifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * SSH 客户端配置
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(SshProperties.class)
public class SshClientConfiguration {

    /**
     * 创建 Apache MINA SSHD 客户端
     *
     * @param properties SSH 配置属性
     * @return SSH 客户端
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public SshClient sshClient(SshProperties properties) {
        SshClient client = SshClient.setUpDefaultClient();

        if (BooleanUtil.isFalse(properties.getStrictHostKeyChecking())) {
            client.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
            log.warn("SSH主机密钥校验已关闭，仅建议在开发或内网可信环境使用");
            return client;
        }

        Path knownHostsPath = Paths.get(properties.getKnownHostsPath());
        client.setServerKeyVerifier(
                new DefaultKnownHostsServerKeyVerifier(
                        RejectAllServerKeyVerifier.INSTANCE,
                        true,
                        knownHostsPath
                )
        );
        log.info("SSH主机密钥校验已开启，known_hosts={}", knownHostsPath);

        return client;
    }

}
```

这里有一个安全取舍：

`strict-host-key-checking=false` 会接受所有服务器主机密钥，开发环境方便，但生产环境不建议长期使用。生产建议设置为 `true`，并提前维护 `${user.home}/.ssh/known_hosts`。

## SSH 会话工厂

这个类是后续所有操作的统一入口。命令执行、SFTP、SCP 都不要自己重复连接逻辑，而是从这里获取已认证的 `ClientSession`。

Apache MINA SSHD 支持密码认证和公钥认证，私钥加载可以通过 `SecurityUtils.getKeyPairResourceParser()` 获取 `KeyPairResourceLoader`，加载后通过 `addPublicKeyIdentity` 加到 session。([apache.googlesource.com](https://apache.googlesource.com/mina-sshd/%2Bshow/refs/heads/releases/v2.13.2/docs/client-setup.md?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/ssh/core/SshSessionFactory.java`

```java
package io.github.atengk.ssh.core;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.ssh.config.SshProperties;
import io.github.atengk.ssh.exception.SshOperationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.config.keys.loader.KeyPairResourceLoader;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.util.Collection;

/**
 * SSH 会话工厂
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SshSessionFactory {

    private final SshClient sshClient;

    private final SshProperties sshProperties;

    /**
     * 创建默认 SSH 会话
     *
     * @return 已认证的 SSH 会话
     */
    public ClientSession createDefaultSession() {
        return createSession(sshProperties.getDefaultHost());
    }

    /**
     * 创建指定主机的 SSH 会话
     *
     * @param host SSH 主机配置
     * @return 已认证的 SSH 会话
     */
    public ClientSession createSession(SshProperties.Host host) {
        validateHost(host);

        try {
            ClientSession session = sshClient
                    .connect(host.getUsername(), host.getHost(), host.getPort())
                    .verify(sshProperties.getConnectTimeout())
                    .getSession();

            applyIdentity(session, host);
            session.auth().verify(sshProperties.getAuthTimeout());

            log.info("SSH连接认证成功，host={}，port={}，username={}",
                    host.getHost(), host.getPort(), host.getUsername());
            return session;
        } catch (Exception e) {
            log.error("SSH连接认证失败，host={}，port={}，username={}",
                    host.getHost(), host.getPort(), host.getUsername(), e);
            throw new SshOperationException("SSH连接认证失败：" + e.getMessage(), e);
        }
    }

    /**
     * 添加 SSH 认证身份
     *
     * @param session SSH 会话
     * @param host    SSH 主机配置
     */
    private void applyIdentity(ClientSession session, SshProperties.Host host) throws Exception {
        boolean hasPassword = StrUtil.isNotBlank(host.getPassword());
        boolean hasPrivateKey = StrUtil.isNotBlank(host.getPrivateKeyPath());

        Assert.isTrue(hasPassword || hasPrivateKey, "SSH认证信息不能为空，请配置 password 或 private-key-path");

        if (hasPrivateKey) {
            loadPrivateKeyIdentities(session, host);
        }

        if (hasPassword) {
            session.addPasswordIdentity(host.getPassword());
        }
    }

    /**
     * 加载私钥认证身份
     *
     * @param session SSH 会话
     * @param host    SSH 主机配置
     */
    private void loadPrivateKeyIdentities(ClientSession session, SshProperties.Host host) throws Exception {
        Path privateKeyPath = Paths.get(host.getPrivateKeyPath());
        Assert.isTrue(Files.isRegularFile(privateKeyPath), "SSH私钥文件不存在：{}", privateKeyPath);

        FilePasswordProvider passwordProvider = StrUtil.isBlank(host.getPrivateKeyPassphrase())
                ? FilePasswordProvider.EMPTY
                : resourceKey -> host.getPrivateKeyPassphrase();

        KeyPairResourceLoader loader = SecurityUtils.getKeyPairResourceParser();
        Collection<KeyPair> keyPairs = loader.loadKeyPairs(null, privateKeyPath, passwordProvider);

        Assert.notEmpty(keyPairs, "SSH私钥文件未加载到有效密钥：{}", privateKeyPath);

        for (KeyPair keyPair : keyPairs) {
            session.addPublicKeyIdentity(keyPair);
        }

        log.info("SSH私钥认证身份加载成功，privateKeyPath={}，keyCount={}", privateKeyPath, keyPairs.size());
    }

    /**
     * 校验 SSH 主机配置
     *
     * @param host SSH 主机配置
     */
    private void validateHost(SshProperties.Host host) {
        Assert.notNull(host, "SSH主机配置不能为空");
        Assert.notBlank(host.getHost(), "SSH主机地址不能为空");
        Assert.notBlank(host.getUsername(), "SSH用户名不能为空");
        Assert.isTrue(host.getPort() != null && host.getPort() > 0 && host.getPort() <= 65535,
                "SSH端口不合法：{}", host.getPort());
    }

}
```

## 统一异常类

后续命令执行、SFTP、SCP 都可以统一抛这个异常，避免业务层直接感知 Apache MINA SSHD 的底层异常。

文件位置：`src/main/java/io/github/atengk/ssh/exception/SshOperationException.java`

```java
package io.github.atengk.ssh.exception;

/**
 * SSH 操作异常
 *
 * @author Ateng
 * @since 2026-04-29
 */
public class SshOperationException extends RuntimeException {

    public SshOperationException(String message) {
        super(message);
    }

    public SshOperationException(String message, Throwable cause) {
        super(message, cause);
    }

}
```

## 基础使用方式

后续业务代码只需要注入 `SshSessionFactory`，通过 `createDefaultSession()` 获取连接即可。注意 `ClientSession` 使用后必须关闭，建议使用 `try-with-resources`。

```java
try (ClientSession session = sshSessionFactory.createDefaultSession()) {
    // 后续在这里创建 exec channel、sftp client 或 scp client
}
```

## 验证方式

先确认目标 Linux 服务器可以从应用所在机器正常 SSH 登录。

```bash
ssh root@192.168.1.100
```

如果启用了 `strict-host-key-checking=true`，需要先生成或确认 `known_hosts`。

```bash
ssh-keyscan -H 192.168.1.100 >> ~/.ssh/known_hosts
```

`ssh-keyscan` 用于拉取目标服务器主机公钥并写入 `known_hosts`。生产环境建议人工核验主机指纹后再写入，避免把错误主机的公钥加入信任列表。

## 后续扩展约定

后面的内容建议全部基于这几个类继续展开：

```text
SshSessionFactory
    ├── SshCommandService        # 执行 Linux 命令
    ├── SftpFileService          # SFTP 上传、下载、删除、目录操作
    ├── ScpFileService           # SCP 上传、下载
    ├── SshBatchService          # 多主机批量执行
    └── SshAuditService          # 操作审计、执行日志、耗时统计
```

下一步可以直接展开 **SSH 命令执行模块**，包括 `exec` 通道封装、stdout/stderr 捕获、退出码、超时控制和接口调用示例。

# SSH 命令执行模块

这一节在上一节基础配置上继续扩展，目标是封装一个可复用的 SSH 命令执行能力。后续 Controller、定时任务、批量运维任务都只调用 `SshCommandService`，不直接操作 Apache MINA SSHD 的底层对象。

Apache MINA SSHD 的 `ClientSession` 支持通过 `createExecChannel(String command)` 创建远程命令执行通道，`ClientChannel` 可以设置标准输出、错误输出，并通过 `waitFor(...)` 等待 `CLOSED`、`EXIT_STATUS`、`TIMEOUT` 等事件；`ClientSession` 使用完后需要关闭。这里采用这种方式封装，而不是直接使用 `executeRemoteCommand(...)`，原因是我们需要同时拿到 `stdout`、`stderr`、退出码、耗时和超时状态。([JavaDoc](https://javadoc.io/static/org.apache.sshd/sshd-core/2.1.0/org/apache/sshd/client/session/ClientSession.html?utm_source=chatgpt.com))

## 文件结构

本节新增命令执行相关代码，继续复用上一节的 `SshSessionFactory` 和 `SshOperationException`。

```text
src/main/java/io/github/atengk/ssh/config/SshProperties.java
src/main/java/io/github/atengk/ssh/dto/SshCommandRequest.java
src/main/java/io/github/atengk/ssh/dto/SshCommandResult.java
src/main/java/io/github/atengk/ssh/service/SshCommandService.java
src/main/java/io/github/atengk/ssh/service/impl/SshCommandServiceImpl.java
src/main/java/io/github/atengk/ssh/controller/SshCommandController.java
src/main/resources/application.yml
```

## 配置补充

这里给 SSH 命令执行增加默认超时时间和最大允许超时时间。默认超时用于普通命令，最大超时用于限制前端或接口调用方传入过大的超时时间。

文件位置：`src/main/resources/application.yml`

```yaml
ssh:
  strict-host-key-checking: false
  known-hosts-path: ${user.home}/.ssh/known_hosts
  connect-timeout: 10s
  auth-timeout: 10s
  session-close-timeout: 3s

  # 默认命令执行超时时间
  command-timeout: 30s

  # 最大允许命令执行超时时间，避免接口调用方传入过大的超时时间
  max-command-timeout: 300s

  default-host:
    host: 192.168.1.100
    port: 22
    username: root
    password: "your_password"
    private-key-path:
    private-key-passphrase:
```

在上一节的 `SshProperties` 中补充这两个字段。

文件位置：`src/main/java/io/github/atengk/ssh/config/SshProperties.java`

```java
/**
 * 默认命令执行超时时间
 */
private Duration commandTimeout = Duration.ofSeconds(30);

/**
 * 最大允许命令执行超时时间
 */
private Duration maxCommandTimeout = Duration.ofSeconds(300);
```

放入 `SshProperties` 主类字段区域即可，不需要放到 `Host` 内部类中。

## 请求对象

请求对象用于接收要执行的 Linux 命令、超时时间以及是否在退出码非 0 时抛出异常。这里默认 `failOnNonZeroExit=false`，也就是命令失败时正常返回退出码和错误输出，由调用方判断。

文件位置：`src/main/java/io/github/atengk/ssh/dto/SshCommandRequest.java`

```java
package io.github.atengk.ssh.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * SSH 命令执行请求
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Data
public class SshCommandRequest {

    /**
     * 要执行的 Linux 命令
     */
    @NotBlank(message = "命令不能为空")
    private String command;

    /**
     * 命令执行超时时间，单位：秒
     */
    @Min(value = 1, message = "命令超时时间不能小于1秒")
    @Max(value = 300, message = "命令超时时间不能大于300秒")
    private Long timeoutSeconds;

    /**
     * 退出码非 0 时是否抛出异常
     */
    private Boolean failOnNonZeroExit = Boolean.FALSE;

}
```

## 响应对象

响应对象统一承载命令执行结果。后续做批量执行时，也可以直接复用这个对象作为单台主机的执行结果。

文件位置：`src/main/java/io/github/atengk/ssh/dto/SshCommandResult.java`

```java
package io.github.atengk.ssh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSH 命令执行结果
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SshCommandResult {

    /**
     * 执行命令
     */
    private String command;

    /**
     * 是否执行成功
     */
    private Boolean success;

    /**
     * 是否执行超时
     */
    private Boolean timeout;

    /**
     * 退出码，正常一般为 0，超时或未返回时可能为 null
     */
    private Integer exitCode;

    /**
     * 标准输出
     */
    private String stdout;

    /**
     * 错误输出
     */
    private String stderr;

    /**
     * 执行耗时，单位：毫秒
     */
    private Long durationMillis;

}
```

## Service 接口

接口层只暴露默认主机命令执行方法。后续如果要支持指定主机，可以增加 `execute(SshProperties.Host host, SshCommandRequest request)` 或引入主机 ID。

文件位置：`src/main/java/io/github/atengk/ssh/service/SshCommandService.java`

```java
package io.github.atengk.ssh.service;

import io.github.atengk.ssh.dto.SshCommandRequest;
import io.github.atengk.ssh.dto.SshCommandResult;

/**
 * SSH 命令服务
 *
 * @author Ateng
 * @since 2026-04-29
 */
public interface SshCommandService {

    /**
     * 在默认主机上执行命令
     *
     * @param request 命令执行请求
     * @return 命令执行结果
     */
    SshCommandResult executeDefault(SshCommandRequest request);

}
```

## Service 实现

这里是核心实现。流程是：创建默认 SSH 会话，创建 `exec` 通道，绑定标准输出和错误输出，打开通道，等待通道关闭，读取退出码，组装统一结果。

文件位置：`src/main/java/io/github/atengk/ssh/service/impl/SshCommandServiceImpl.java`

```java
package io.github.atengk.ssh.service.impl;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.core.util.CharsetUtil;
import io.github.atengk.ssh.config.SshProperties;
import io.github.atengk.ssh.core.SshSessionFactory;
import io.github.atengk.ssh.dto.SshCommandRequest;
import io.github.atengk.ssh.dto.SshCommandResult;
import io.github.atengk.ssh.exception.SshOperationException;
import io.github.atengk.ssh.service.SshCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;

/**
 * SSH 命令服务实现
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SshCommandServiceImpl implements SshCommandService {

    private final SshSessionFactory sshSessionFactory;

    private final SshProperties sshProperties;

    /**
     * 在默认主机上执行命令
     *
     * @param request 命令执行请求
     * @return 命令执行结果
     */
    @Override
    public SshCommandResult executeDefault(SshCommandRequest request) {
        validateRequest(request);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        String command = request.getCommand();
        Duration timeout = resolveTimeout(request.getTimeoutSeconds());

        try (ClientSession session = sshSessionFactory.createDefaultSession();
             ChannelExec channel = session.createExecChannel(command);
             ByteArrayOutputStream stdout = new ByteArrayOutputStream();
             ByteArrayOutputStream stderr = new ByteArrayOutputStream()) {

            channel.setOut(stdout);
            channel.setErr(stderr);

            log.info("开始执行SSH命令，command={}，timeout={}ms", command, timeout.toMillis());

            channel.open().verify(timeout);
            Set<ClientChannelEvent> events = channel.waitFor(
                    EnumSet.of(ClientChannelEvent.CLOSED, ClientChannelEvent.EXIT_STATUS),
                    timeout.toMillis()
            );

            stopWatch.stop();

            boolean timeoutFlag = events.contains(ClientChannelEvent.TIMEOUT);
            Integer exitCode = channel.getExitStatus();

            String stdoutText = stdout.toString(CharsetUtil.CHARSET_UTF_8);
            String stderrText = stderr.toString(CharsetUtil.CHARSET_UTF_8);

            SshCommandResult result = SshCommandResult.builder()
                    .command(command)
                    .success(!timeoutFlag && Integer.valueOf(0).equals(exitCode))
                    .timeout(timeoutFlag)
                    .exitCode(exitCode)
                    .stdout(stdoutText)
                    .stderr(stderrText)
                    .durationMillis(stopWatch.getTotalTimeMillis())
                    .build();

            if (timeoutFlag) {
                log.warn("SSH命令执行超时，command={}，duration={}ms", command, result.getDurationMillis());
                return result;
            }

            if (!Integer.valueOf(0).equals(exitCode)) {
                log.warn("SSH命令执行返回非零退出码，command={}，exitCode={}，stderr={}",
                        command, exitCode, abbreviate(stderrText));

                if (BooleanUtil.isTrue(request.getFailOnNonZeroExit())) {
                    throw new SshOperationException(
                            StrUtil.format("SSH命令执行失败，exitCode={}，stderr={}", exitCode, abbreviate(stderrText))
                    );
                }
            } else {
                log.info("SSH命令执行成功，command={}，duration={}ms", command, result.getDurationMillis());
            }

            return result;
        } catch (SshOperationException e) {
            throw e;
        } catch (Exception e) {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
            log.error("SSH命令执行异常，command={}，duration={}ms", command, stopWatch.getTotalTimeMillis(), e);
            throw new SshOperationException("SSH命令执行异常：" + e.getMessage(), e);
        }
    }

    /**
     * 校验请求参数
     *
     * @param request 命令执行请求
     */
    private void validateRequest(SshCommandRequest request) {
        Assert.notNull(request, "SSH命令执行请求不能为空");
        Assert.notBlank(request.getCommand(), "SSH命令不能为空");

        String command = request.getCommand();

        Assert.isFalse(StrUtil.containsAnyIgnoreCase(command, "reboot", "shutdown", "poweroff"),
                "当前命令包含高风险操作，请通过专门的运维审批流程执行：{}", command);
    }

    /**
     * 解析命令执行超时时间
     *
     * @param timeoutSeconds 请求超时时间，单位：秒
     * @return 命令执行超时时间
     */
    private Duration resolveTimeout(Long timeoutSeconds) {
        Duration timeout = timeoutSeconds == null
                ? sshProperties.getCommandTimeout()
                : Duration.ofSeconds(timeoutSeconds);

        if (timeout.compareTo(sshProperties.getMaxCommandTimeout()) > 0) {
            return sshProperties.getMaxCommandTimeout();
        }

        return timeout;
    }

    /**
     * 缩短日志输出内容
     *
     * @param text 原始文本
     * @return 缩短后的文本
     */
    private String abbreviate(String text) {
        return StrUtil.abbreviate(StrUtil.blankToDefault(text, ""), 500);
    }

}
```

上面代码中有一个多余导入，需要删除，否则会触发 IDE 警告：

```java
import cn.hutool.core.util.URLUtil;
```

最终保留的 Hutool 导入应为：

```java
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
```

## Controller 接口

这里提供一个简单 REST 接口，方便先验证 SSH 命令执行能力。生产环境不要直接裸露这种接口，至少要加登录认证、权限控制、命令白名单、审计日志和操作审批。

文件位置：`src/main/java/io/github/atengk/ssh/controller/SshCommandController.java`

```java
package io.github.atengk.ssh.controller;

import io.github.atengk.ssh.dto.SshCommandRequest;
import io.github.atengk.ssh.dto.SshCommandResult;
import io.github.atengk.ssh.service.SshCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * SSH 命令控制器
 *
 * @author Ateng
 * @since 2026-04-29
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ssh/commands")
public class SshCommandController {

    private final SshCommandService sshCommandService;

    /**
     * 在默认主机上执行 SSH 命令
     *
     * @param request 命令执行请求
     * @return 命令执行结果
     */
    @PostMapping("/execute-default")
    public SshCommandResult executeDefault(@Valid @RequestBody SshCommandRequest request) {
        return sshCommandService.executeDefault(request);
    }

}
```

## 接口调用示例

启动 Spring Boot 项目后，可以先执行无破坏性的查询命令。

```bash
curl -X POST 'http://localhost:8080/api/ssh/commands/execute-default' \
  -H 'Content-Type: application/json' \
  -d '{
    "command": "hostname && whoami && pwd",
    "timeoutSeconds": 10,
    "failOnNonZeroExit": false
  }'
```

返回示例：

```json
{
  "command": "hostname && whoami && pwd",
  "success": true,
  "timeout": false,
  "exitCode": 0,
  "stdout": "server01\nroot\n/root\n",
  "stderr": "",
  "durationMillis": 218
}
```

再验证一个失败命令，观察退出码和错误输出。

```bash
curl -X POST 'http://localhost:8080/api/ssh/commands/execute-default' \
  -H 'Content-Type: application/json' \
  -d '{
    "command": "ls /not-exists",
    "timeoutSeconds": 10,
    "failOnNonZeroExit": false
  }'
```

可能返回：

```json
{
  "command": "ls /not-exists",
  "success": false,
  "timeout": false,
  "exitCode": 2,
  "stdout": "",
  "stderr": "ls: cannot access '/not-exists': No such file or directory\n",
  "durationMillis": 164
}
```

## 常用命令测试

这些命令适合作为基础验证，风险较低。

```bash
# 查看主机名
hostname

# 查看当前登录用户
whoami

# 查看系统版本
cat /etc/os-release

# 查看磁盘空间
df -h

# 查看内存
free -h

# 查看当前目录
pwd

# 查看 Java 版本
java -version
```

这些命令只读查询系统状态，适合验证连接、认证、stdout、stderr 和退出码处理是否正常。

## 注意事项

当前实现是“每次命令执行创建一个 SSH 会话和一个 exec channel”。这样最简单、隔离性最好，适合先把功能跑通。后续如果有高频调用或批量主机执行，再考虑连接池、会话复用、任务队列和并发限流。

不要直接把命令执行接口暴露给普通用户。生产环境建议至少增加这些控制：

```text
1. 登录认证：例如 Sa-Token、Spring Security
2. 权限控制：只有运维或管理员角色可以执行
3. 命令白名单：只允许执行固定模板命令
4. 高危命令拦截：rm、reboot、shutdown、mkfs、dd 等需要审批
5. 审计日志：记录执行人、主机、命令、退出码、stdout/stderr 摘要、耗时
6. 超时控制：避免长时间占用 SSH 连接
7. 并发限制：避免同时对大量主机执行命令
```

另外，`exec` 通道适合执行非交互命令，例如 `df -h`、`systemctl status nginx`、`docker ps`。如果要处理交互式命令，例如 `top`、`vim`、`passwd`、需要输入确认的脚本，后面应单独封装 Shell Channel 或 PTY 模式，不建议混在这个基础命令执行模块里。

# SFTP 上传、下载、删除、目录操作

这一节继续基于前面的 `SshSessionFactory` 展开，不重复创建 SSH 连接配置。核心流程是：先通过 `SshSessionFactory` 创建已认证的 `ClientSession`，再使用 `SftpClientFactory.instance().createSftpClient(session)` 创建 SFTP 客户端，最后调用 `read`、`write`、`remove`、`mkdir`、`rmdir`、`readDir`、`stat` 等方法完成文件操作。`SftpClientFactory` 支持从 `ClientSession` 创建 `SftpClient`，`SftpClient` 本身提供文件读写、目录读取、创建目录、删除文件、删除目录、读取文件属性等能力。([Apache Git Repositories](https://apache.googlesource.com/mina-sshd/%2B/refs/heads/master/sshd-sftp/src/main/java/org/apache/sshd/sftp/client/SftpClientFactory.java?utm_source=chatgpt.com))

## 文件结构

本节新增 SFTP 文件服务、结果对象、目录条目对象和 REST 接口。上一节已有的 `SshSessionFactory`、`SshProperties`、`SshOperationException` 继续复用。

```text
src/main/java/io/github/atengk/ssh/config/SshProperties.java
src/main/java/io/github/atengk/ssh/dto/SftpEntry.java
src/main/java/io/github/atengk/ssh/dto/SftpOperationResult.java
src/main/java/io/github/atengk/ssh/service/SftpFileService.java
src/main/java/io/github/atengk/ssh/service/impl/SftpFileServiceImpl.java
src/main/java/io/github/atengk/ssh/controller/SftpFileController.java
src/main/resources/application.yml
```

## 配置补充

这里给 SFTP 文件读写增加缓冲区大小。默认 `32768` 字节即可，后续如果传输大文件，可以结合网络情况调大。

文件位置：`src/main/resources/application.yml`

```yaml
ssh:
  strict-host-key-checking: false
  known-hosts-path: ${user.home}/.ssh/known_hosts
  connect-timeout: 10s
  auth-timeout: 10s
  session-close-timeout: 3s
  command-timeout: 30s
  max-command-timeout: 300s

  # SFTP 文件读写缓冲区大小，单位：字节
  sftp-buffer-size: 32768

  default-host:
    host: 192.168.1.100
    port: 22
    username: root
    password: "your_password"
    private-key-path:
    private-key-passphrase:
```

在上一节的 `SshProperties` 中补充字段。

文件位置：`src/main/java/io/github/atengk/ssh/config/SshProperties.java`

```java
/**
 * SFTP 文件读写缓冲区大小
 */
private Integer sftpBufferSize = 32768;
```

## SFTP 目录条目对象

这个对象用于返回远程目录列表。文件类型、大小、权限、修改时间等信息来自 `SftpClient.DirEntry` 和 `SftpClient.Attributes`。

文件位置：`src/main/java/io/github/atengk/ssh/dto/SftpEntry.java`

```java
package io.github.atengk.ssh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SFTP 目录条目
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SftpEntry {

    /**
     * 文件名
     */
    private String filename;

    /**
     * 远程完整路径
     */
    private String path;

    /**
     * 是否目录
     */
    private Boolean directory;

    /**
     * 是否普通文件
     */
    private Boolean regularFile;

    /**
     * 是否符号链接
     */
    private Boolean symbolicLink;

    /**
     * 文件大小，单位：字节
     */
    private Long size;

    /**
     * 权限，八进制字符串，例如 755
     */
    private String permissions;

    /**
     * 修改时间，时间戳毫秒
     */
    private Long modifyTimeMillis;

}
```

## SFTP 操作结果对象

上传、删除、创建目录等操作返回统一结果。下载接口直接写入响应流，不通过这个对象返回。

文件位置：`src/main/java/io/github/atengk/ssh/dto/SftpOperationResult.java`

```java
package io.github.atengk.ssh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SFTP 操作结果
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SftpOperationResult {

    /**
     * 操作类型
     */
    private String operation;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 远程路径
     */
    private String remotePath;

    /**
     * 文件大小，单位：字节
     */
    private Long size;

    /**
     * 结果消息
     */
    private String message;

}
```

## Service 接口

接口层提供上传、下载、删除文件、删除目录、创建目录、查询目录列表、判断路径是否存在这些基础能力。后续批量上传、断点续传、目录同步可以在这个接口基础上继续扩展。

文件位置：`src/main/java/io/github/atengk/ssh/service/SftpFileService.java`

```java
package io.github.atengk.ssh.service;

import io.github.atengk.ssh.dto.SftpEntry;
import io.github.atengk.ssh.dto.SftpOperationResult;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * SFTP 文件服务
 *
 * @author Ateng
 * @since 2026-04-29
 */
public interface SftpFileService {

    /**
     * 上传文件到默认主机
     *
     * @param remotePath       远程文件路径
     * @param inputStream      文件输入流
     * @param overwrite        是否覆盖已有文件
     * @param createParentDirs 是否自动创建父目录
     * @return SFTP 操作结果
     */
    SftpOperationResult uploadFile(String remotePath, InputStream inputStream, boolean overwrite, boolean createParentDirs);

    /**
     * 从默认主机下载文件
     *
     * @param remotePath   远程文件路径
     * @param outputStream 输出流
     */
    void downloadFile(String remotePath, OutputStream outputStream);

    /**
     * 删除默认主机上的文件
     *
     * @param remotePath 远程文件路径
     * @return SFTP 操作结果
     */
    SftpOperationResult deleteFile(String remotePath);

    /**
     * 删除默认主机上的目录
     *
     * @param remoteDir 远程目录路径
     * @param recursive 是否递归删除
     * @return SFTP 操作结果
     */
    SftpOperationResult deleteDirectory(String remoteDir, boolean recursive);

    /**
     * 在默认主机上创建目录
     *
     * @param remoteDir 远程目录路径
     * @return SFTP 操作结果
     */
    SftpOperationResult createDirectory(String remoteDir);

    /**
     * 查询默认主机上的目录列表
     *
     * @param remoteDir 远程目录路径
     * @return 目录条目列表
     */
    List<SftpEntry> listDirectory(String remoteDir);

    /**
     * 判断默认主机上的路径是否存在
     *
     * @param remotePath 远程路径
     * @return 是否存在
     */
    boolean exists(String remotePath);

}
```

## Service 实现

这里是 SFTP 操作的核心实现。上传使用 `SftpClient.write(...)` 写远程文件，下载使用 `SftpClient.read(...)` 读取远程文件，目录读取使用 `readDir(...)`，删除文件使用 `remove(...)`，删除目录使用 `rmdir(...)`。`OpenMode` 支持 `Read`、`Write`、`Create`、`Truncate`、`Append`、`Exclusive` 等模式，上传覆盖文件时使用 `Write + Create + Truncate`。([JavaDoc](https://javadoc.io/static/org.apache.sshd/sshd-sftp/2.0.0/org/apache/sshd/client/subsystem/sftp/SftpClient.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/ssh/service/impl/SftpFileServiceImpl.java`

```java
package io.github.atengk.ssh.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.ssh.config.SshProperties;
import io.github.atengk.ssh.core.SshSessionFactory;
import io.github.atengk.ssh.dto.SftpEntry;
import io.github.atengk.ssh.dto.SftpOperationResult;
import io.github.atengk.ssh.exception.SshOperationException;
import io.github.atengk.ssh.service.SftpFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * SFTP 文件服务实现
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SftpFileServiceImpl implements SftpFileService {

    private final SshSessionFactory sshSessionFactory;

    private final SshProperties sshProperties;

    /**
     * 上传文件到默认主机
     *
     * @param remotePath       远程文件路径
     * @param inputStream      文件输入流
     * @param overwrite        是否覆盖已有文件
     * @param createParentDirs 是否自动创建父目录
     * @return SFTP 操作结果
     */
    @Override
    public SftpOperationResult uploadFile(String remotePath, InputStream inputStream, boolean overwrite, boolean createParentDirs) {
        Assert.notNull(inputStream, "上传文件输入流不能为空");

        String path = normalizeAndValidatePath(remotePath);

        try (ClientSession session = sshSessionFactory.createDefaultSession();
             SftpClient sftpClient = SftpClientFactory.instance().createSftpClient(session)) {

            if (createParentDirs) {
                createDirectoryRecursive(sftpClient, getParentPath(path));
            }

            if (!overwrite && exists(sftpClient, path)) {
                throw new SshOperationException("远程文件已存在，不允许覆盖：" + path);
            }

            SftpClient.OpenMode[] openModes = overwrite
                    ? new SftpClient.OpenMode[]{
                    SftpClient.OpenMode.Write,
                    SftpClient.OpenMode.Create,
                    SftpClient.OpenMode.Truncate
            }
                    : new SftpClient.OpenMode[]{
                    SftpClient.OpenMode.Write,
                    SftpClient.OpenMode.Create,
                    SftpClient.OpenMode.Exclusive
            };

            long size;
            try (OutputStream remoteOutputStream = sftpClient.write(path, sshProperties.getSftpBufferSize(), openModes)) {
                size = IoUtil.copy(inputStream, remoteOutputStream, sshProperties.getSftpBufferSize());
            }

            long remoteSize = sftpClient.stat(path).getSize();
            log.info("SFTP文件上传成功，remotePath={}，uploadSize={}，remoteSize={}", path, size, remoteSize);

            return SftpOperationResult.builder()
                    .operation("upload")
                    .success(Boolean.TRUE)
                    .remotePath(path)
                    .size(remoteSize)
                    .message("文件上传成功")
                    .build();
        } catch (SshOperationException e) {
            throw e;
        } catch (Exception e) {
            log.error("SFTP文件上传失败，remotePath={}", path, e);
            throw new SshOperationException("SFTP文件上传失败：" + e.getMessage(), e);
        }
    }

    /**
     * 从默认主机下载文件
     *
     * @param remotePath   远程文件路径
     * @param outputStream 输出流
     */
    @Override
    public void downloadFile(String remotePath, OutputStream outputStream) {
        Assert.notNull(outputStream, "下载文件输出流不能为空");

        String path = normalizeAndValidatePath(remotePath);

        try (ClientSession session = sshSessionFactory.createDefaultSession();
             SftpClient sftpClient = SftpClientFactory.instance().createSftpClient(session)) {

            SftpClient.Attributes attributes = sftpClient.stat(path);
            Assert.isTrue(attributes.isRegularFile(), "远程路径不是普通文件，不能下载：{}", path);

            try (InputStream remoteInputStream = sftpClient.read(path, sshProperties.getSftpBufferSize())) {
                IoUtil.copy(remoteInputStream, outputStream, sshProperties.getSftpBufferSize());
            }

            log.info("SFTP文件下载成功，remotePath={}，size={}", path, attributes.getSize());
        } catch (Exception e) {
            log.error("SFTP文件下载失败，remotePath={}", path, e);
            throw new SshOperationException("SFTP文件下载失败：" + e.getMessage(), e);
        }
    }

    /**
     * 删除默认主机上的文件
     *
     * @param remotePath 远程文件路径
     * @return SFTP 操作结果
     */
    @Override
    public SftpOperationResult deleteFile(String remotePath) {
        String path = normalizeAndValidatePath(remotePath);

        try (ClientSession session = sshSessionFactory.createDefaultSession();
             SftpClient sftpClient = SftpClientFactory.instance().createSftpClient(session)) {

            SftpClient.Attributes attributes = sftpClient.stat(path);
            Assert.isTrue(attributes.isRegularFile() || attributes.isSymbolicLink(),
                    "远程路径不是文件或符号链接，请使用目录删除接口：{}", path);

            sftpClient.remove(path);
            log.info("SFTP文件删除成功，remotePath={}", path);

            return SftpOperationResult.builder()
                    .operation("deleteFile")
                    .success(Boolean.TRUE)
                    .remotePath(path)
                    .size(attributes.getSize())
                    .message("文件删除成功")
                    .build();
        } catch (Exception e) {
            log.error("SFTP文件删除失败，remotePath={}", path, e);
            throw new SshOperationException("SFTP文件删除失败：" + e.getMessage(), e);
        }
    }

    /**
     * 删除默认主机上的目录
     *
     * @param remoteDir 远程目录路径
     * @param recursive 是否递归删除
     * @return SFTP 操作结果
     */
    @Override
    public SftpOperationResult deleteDirectory(String remoteDir, boolean recursive) {
        String dir = normalizeAndValidatePath(remoteDir);
        Assert.isFalse(StrUtil.equals(dir, "/"), "不允许删除根目录");

        try (ClientSession session = sshSessionFactory.createDefaultSession();
             SftpClient sftpClient = SftpClientFactory.instance().createSftpClient(session)) {

            SftpClient.Attributes attributes = sftpClient.stat(dir);
            Assert.isTrue(attributes.isDirectory(), "远程路径不是目录：{}", dir);

            deleteDirectoryInternal(sftpClient, dir, recursive);
            log.info("SFTP目录删除成功，remoteDir={}，recursive={}", dir, recursive);

            return SftpOperationResult.builder()
                    .operation("deleteDirectory")
                    .success(Boolean.TRUE)
                    .remotePath(dir)
                    .message("目录删除成功")
                    .build();
        } catch (Exception e) {
            log.error("SFTP目录删除失败，remoteDir={}，recursive={}", dir, recursive, e);
            throw new SshOperationException("SFTP目录删除失败：" + e.getMessage(), e);
        }
    }

    /**
     * 在默认主机上创建目录
     *
     * @param remoteDir 远程目录路径
     * @return SFTP 操作结果
     */
    @Override
    public SftpOperationResult createDirectory(String remoteDir) {
        String dir = normalizeAndValidatePath(remoteDir);

        try (ClientSession session = sshSessionFactory.createDefaultSession();
             SftpClient sftpClient = SftpClientFactory.instance().createSftpClient(session)) {

            createDirectoryRecursive(sftpClient, dir);
            log.info("SFTP目录创建成功，remoteDir={}", dir);

            return SftpOperationResult.builder()
                    .operation("createDirectory")
                    .success(Boolean.TRUE)
                    .remotePath(dir)
                    .message("目录创建成功")
                    .build();
        } catch (Exception e) {
            log.error("SFTP目录创建失败，remoteDir={}", dir, e);
            throw new SshOperationException("SFTP目录创建失败：" + e.getMessage(), e);
        }
    }

    /**
     * 查询默认主机上的目录列表
     *
     * @param remoteDir 远程目录路径
     * @return 目录条目列表
     */
    @Override
    public List<SftpEntry> listDirectory(String remoteDir) {
        String dir = normalizeAndValidatePath(remoteDir);

        try (ClientSession session = sshSessionFactory.createDefaultSession();
             SftpClient sftpClient = SftpClientFactory.instance().createSftpClient(session)) {

            SftpClient.Attributes attributes = sftpClient.stat(dir);
            Assert.isTrue(attributes.isDirectory(), "远程路径不是目录：{}", dir);

            List<SftpEntry> entries = new ArrayList<>();
            for (SftpClient.DirEntry entry : sftpClient.readDir(dir)) {
                if (isPseudoEntry(entry.getFilename())) {
                    continue;
                }
                entries.add(toSftpEntry(dir, entry));
            }

            entries.sort(Comparator
                    .comparing(SftpEntry::getDirectory, Comparator.reverseOrder())
                    .thenComparing(SftpEntry::getFilename));

            log.info("SFTP目录查询成功，remoteDir={}，count={}", dir, entries.size());
            return entries;
        } catch (Exception e) {
            log.error("SFTP目录查询失败，remoteDir={}", dir, e);
            throw new SshOperationException("SFTP目录查询失败：" + e.getMessage(), e);
        }
    }

    /**
     * 判断默认主机上的路径是否存在
     *
     * @param remotePath 远程路径
     * @return 是否存在
     */
    @Override
    public boolean exists(String remotePath) {
        String path = normalizeAndValidatePath(remotePath);

        try (ClientSession session = sshSessionFactory.createDefaultSession();
             SftpClient sftpClient = SftpClientFactory.instance().createSftpClient(session)) {
            return exists(sftpClient, path);
        } catch (Exception e) {
            log.error("SFTP路径存在性检查失败，remotePath={}", path, e);
            throw new SshOperationException("SFTP路径存在性检查失败：" + e.getMessage(), e);
        }
    }

    /**
     * 递归创建目录
     *
     * @param sftpClient SFTP 客户端
     * @param remoteDir  远程目录
     */
    private void createDirectoryRecursive(SftpClient sftpClient, String remoteDir) throws IOException {
        String dir = normalizeAndValidatePath(remoteDir);
        if (StrUtil.equals(dir, "/")) {
            return;
        }

        String current = "";
        List<String> parts = StrUtil.split(dir, '/');
        for (String part : parts) {
            if (StrUtil.isBlank(part)) {
                continue;
            }

            current = current + "/" + part;
            if (exists(sftpClient, current)) {
                SftpClient.Attributes attributes = sftpClient.stat(current);
                Assert.isTrue(attributes.isDirectory(), "远程路径已存在但不是目录：{}", current);
                continue;
            }

            sftpClient.mkdir(current);
            log.info("SFTP目录创建完成，remoteDir={}", current);
        }
    }

    /**
     * 递归或非递归删除目录
     *
     * @param sftpClient SFTP 客户端
     * @param remoteDir  远程目录
     * @param recursive  是否递归删除
     */
    private void deleteDirectoryInternal(SftpClient sftpClient, String remoteDir, boolean recursive) throws IOException {
        if (!recursive) {
            sftpClient.rmdir(remoteDir);
            return;
        }

        for (SftpClient.DirEntry entry : sftpClient.readDir(remoteDir)) {
            String filename = entry.getFilename();
            if (isPseudoEntry(filename)) {
                continue;
            }

            String childPath = joinRemotePath(remoteDir, filename);
            SftpClient.Attributes attributes = entry.getAttributes();

            if (attributes.isDirectory()) {
                deleteDirectoryInternal(sftpClient, childPath, true);
            } else {
                sftpClient.remove(childPath);
                log.info("SFTP递归删除文件完成，remotePath={}", childPath);
            }
        }

        sftpClient.rmdir(remoteDir);
        log.info("SFTP递归删除目录完成，remoteDir={}", remoteDir);
    }

    /**
     * 判断路径是否存在
     *
     * @param sftpClient SFTP 客户端
     * @param remotePath 远程路径
     * @return 是否存在
     */
    private boolean exists(SftpClient sftpClient, String remotePath) {
        try {
            sftpClient.stat(remotePath);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 转换目录条目对象
     *
     * @param parentDir 父目录
     * @param entry     SFTP 原始目录条目
     * @return SFTP 目录条目
     */
    private SftpEntry toSftpEntry(String parentDir, SftpClient.DirEntry entry) {
        SftpClient.Attributes attributes = entry.getAttributes();
        String filename = entry.getFilename();
        String path = joinRemotePath(parentDir, filename);

        return SftpEntry.builder()
                .filename(filename)
                .path(path)
                .directory(attributes.isDirectory())
                .regularFile(attributes.isRegularFile())
                .symbolicLink(attributes.isSymbolicLink())
                .size(attributes.getSize())
                .permissions(formatPermissions(attributes.getPermissions()))
                .modifyTimeMillis(toMillis(attributes.getModifyTime()))
                .build();
    }

    /**
     * 规范化并校验远程路径
     *
     * @param remotePath 远程路径
     * @return 规范化后的远程路径
     */
    private String normalizeAndValidatePath(String remotePath) {
        Assert.notBlank(remotePath, "远程路径不能为空");

        String path = StrUtil.trim(remotePath).replace('\\', '/');
        while (path.contains("//")) {
            path = path.replace("//", "/");
        }

        if (path.length() > 1 && path.endsWith("/")) {
            path = StrUtil.removeSuffix(path, "/");
        }

        Assert.isTrue(StrUtil.startWith(path, "/"), "远程路径必须使用绝对路径：{}", path);
        Assert.isFalse(StrUtil.contains(path, "\0"), "远程路径不能包含空字符：{}", path);

        List<String> parts = StrUtil.split(path, '/');
        boolean hasParentJump = CollUtil.contains(parts, "..");
        Assert.isFalse(hasParentJump, "远程路径不允许包含上级目录跳转：{}", path);

        return path;
    }

    /**
     * 获取父目录
     *
     * @param remotePath 远程路径
     * @return 父目录
     */
    private String getParentPath(String remotePath) {
        int index = remotePath.lastIndexOf("/");
        if (index <= 0) {
            return "/";
        }
        return remotePath.substring(0, index);
    }

    /**
     * 拼接远程路径
     *
     * @param parentDir 父目录
     * @param filename  文件名
     * @return 拼接后的路径
     */
    private String joinRemotePath(String parentDir, String filename) {
        if (StrUtil.equals(parentDir, "/")) {
            return "/" + filename;
        }
        return StrUtil.removeSuffix(parentDir, "/") + "/" + filename;
    }

    /**
     * 判断是否为伪目录项
     *
     * @param filename 文件名
     * @return 是否为伪目录项
     */
    private boolean isPseudoEntry(String filename) {
        return StrUtil.equals(filename, ".") || StrUtil.equals(filename, "..");
    }

    /**
     * 格式化权限
     *
     * @param permissions 权限整数
     * @return 八进制权限字符串
     */
    private String formatPermissions(int permissions) {
        return Integer.toOctalString(permissions & 07777);
    }

    /**
     * 转换文件时间
     *
     * @param fileTime 文件时间
     * @return 时间戳毫秒
     */
    private Long toMillis(FileTime fileTime) {
        return fileTime == null ? null : fileTime.toMillis();
    }

}
```

## Controller 接口

这里提供基础 REST 接口，方便验证上传、下载、删除和目录操作。生产环境建议对这些接口增加权限控制、路径白名单、审计日志和操作审批。

文件位置：`src/main/java/io/github/atengk/ssh/controller/SftpFileController.java`

```java
package io.github.atengk.ssh.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.ssh.dto.SftpEntry;
import io.github.atengk.ssh.dto.SftpOperationResult;
import io.github.atengk.ssh.service.SftpFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * SFTP 文件控制器
 *
 * @author Ateng
 * @since 2026-04-29
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ssh/sftp")
public class SftpFileController {

    private final SftpFileService sftpFileService;

    /**
     * 上传文件到默认主机
     *
     * @param file             上传文件
     * @param remotePath       远程文件路径
     * @param overwrite        是否覆盖
     * @param createParentDirs 是否创建父目录
     * @return SFTP 操作结果
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SftpOperationResult upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam("remotePath") String remotePath,
                                      @RequestParam(value = "overwrite", defaultValue = "true") Boolean overwrite,
                                      @RequestParam(value = "createParentDirs", defaultValue = "true") Boolean createParentDirs) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            return sftpFileService.uploadFile(
                    remotePath,
                    inputStream,
                    BooleanUtil.isTrue(overwrite),
                    BooleanUtil.isTrue(createParentDirs)
            );
        }
    }

    /**
     * 从默认主机下载文件
     *
     * @param remotePath 远程文件路径
     * @return 文件流响应
     */
    @GetMapping("/download")
    public ResponseEntity<StreamingResponseBody> download(@RequestParam("remotePath") String remotePath) {
        String filename = StrUtil.blankToDefault(FileUtil.getName(remotePath), "download.bin");

        StreamingResponseBody responseBody = outputStream -> sftpFileService.downloadFile(remotePath, outputStream);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(responseBody);
    }

    /**
     * 删除默认主机上的文件
     *
     * @param remotePath 远程文件路径
     * @return SFTP 操作结果
     */
    @DeleteMapping("/file")
    public SftpOperationResult deleteFile(@RequestParam("remotePath") String remotePath) {
        return sftpFileService.deleteFile(remotePath);
    }

    /**
     * 删除默认主机上的目录
     *
     * @param remoteDir 远程目录路径
     * @param recursive 是否递归删除
     * @return SFTP 操作结果
     */
    @DeleteMapping("/directory")
    public SftpOperationResult deleteDirectory(@RequestParam("remoteDir") String remoteDir,
                                               @RequestParam(value = "recursive", defaultValue = "false") Boolean recursive) {
        return sftpFileService.deleteDirectory(remoteDir, BooleanUtil.isTrue(recursive));
    }

    /**
     * 在默认主机上创建目录
     *
     * @param remoteDir 远程目录路径
     * @return SFTP 操作结果
     */
    @PostMapping("/directory")
    public SftpOperationResult createDirectory(@RequestParam("remoteDir") String remoteDir) {
        return sftpFileService.createDirectory(remoteDir);
    }

    /**
     * 查询默认主机上的目录列表
     *
     * @param remoteDir 远程目录路径
     * @return 目录条目列表
     */
    @GetMapping("/directory/list")
    public List<SftpEntry> listDirectory(@RequestParam("remoteDir") String remoteDir) {
        return sftpFileService.listDirectory(remoteDir);
    }

    /**
     * 判断默认主机上的路径是否存在
     *
     * @param remotePath 远程路径
     * @return 是否存在
     */
    @GetMapping("/exists")
    public Map<String, Object> exists(@RequestParam("remotePath") String remotePath) {
        return Map.of(
                "remotePath", remotePath,
                "exists", sftpFileService.exists(remotePath)
        );
    }

}
```

## 接口调用示例

先在远程服务器创建测试目录。

```bash
curl -X POST 'http://localhost:8080/api/ssh/sftp/directory?remoteDir=/tmp/ssh-demo/upload'
```

上传本地文件到远程服务器。`remotePath` 必须是完整文件路径，不是目录路径。

```bash
curl -X POST 'http://localhost:8080/api/ssh/sftp/upload' \
  -F 'file=@/tmp/test.txt' \
  -F 'remotePath=/tmp/ssh-demo/upload/test.txt' \
  -F 'overwrite=true' \
  -F 'createParentDirs=true'
```

查询远程目录列表。

```bash
curl -X GET 'http://localhost:8080/api/ssh/sftp/directory/list?remoteDir=/tmp/ssh-demo/upload'
```

下载远程文件。

```bash
curl -L -o /tmp/test-download.txt \
  'http://localhost:8080/api/ssh/sftp/download?remotePath=/tmp/ssh-demo/upload/test.txt'
```

判断路径是否存在。

```bash
curl -X GET 'http://localhost:8080/api/ssh/sftp/exists?remotePath=/tmp/ssh-demo/upload/test.txt'
```

删除远程文件。

```bash
curl -X DELETE 'http://localhost:8080/api/ssh/sftp/file?remotePath=/tmp/ssh-demo/upload/test.txt'
```

递归删除远程目录。

```bash
curl -X DELETE 'http://localhost:8080/api/ssh/sftp/directory?remoteDir=/tmp/ssh-demo&recursive=true'
```

## 注意事项

当前实现仍然是“每次操作创建一个 SSH 会话和一个 SFTP 客户端”。它的优点是隔离性好、容易排查问题；缺点是高频文件操作时连接成本较高。后续如果要做批量上传、目录同步、大文件传输，可以继续扩展连接复用、任务队列、并发限流和传输进度回调。

路径校验目前要求远程路径必须是绝对路径，并禁止 `..` 上级目录跳转。这是必要的基础保护。生产环境建议继续增加路径白名单，例如只允许操作 `/data/app/upload`、`/opt/app/packages`、`/tmp/ssh-demo` 这类固定目录。

下载接口使用 `StreamingResponseBody`，不会一次性把远程文件全部读入内存，更适合中大型文件下载。上传接口直接使用 `MultipartFile` 的输入流写入远程文件，也避免了中间落盘。

目录删除分为非递归和递归两种。非递归删除只能删除空目录；递归删除会删除目录下所有文件和子目录，生产环境必须加权限控制和审计。

# SCP 上传、下载

这一节继续基于前面的 `SshSessionFactory`。SCP 只负责文件复制，不适合做目录浏览、删除、重命名等文件管理操作；这些仍然放在上一节的 SFTP 模块里。Apache MINA SSHD 的 `ScpClient` 提供 `upload(...)`、`download(...)`、`downloadBytes(...)` 等方法，并支持 `Recursive`、`TargetIsDirectory`、`PreserveAttributes` 等选项。这里先封装最常用的“上传单文件”和“下载单文件”。([svn.apache.org](https://svn.apache.org/repos/infra/websites/production/mina/content/sshd-project/apidocs/org/apache/sshd/client/scp/ScpClient.html?utm_source=chatgpt.com))

## 文件结构

本节新增 SCP 文件服务、结果对象和 REST 接口。继续复用前面已有的 `SshSessionFactory`、`SshProperties`、`SshOperationException` 和 `SftpFileService`。

```text
src/main/java/io/github/atengk/ssh/config/SshProperties.java
src/main/java/io/github/atengk/ssh/dto/ScpOperationResult.java
src/main/java/io/github/atengk/ssh/service/ScpFileService.java
src/main/java/io/github/atengk/ssh/service/impl/ScpFileServiceImpl.java
src/main/java/io/github/atengk/ssh/controller/ScpFileController.java
src/main/resources/application.yml
```

## 配置补充

SCP 上传时需要给远程文件设置基础权限。这里使用 `rw-r--r--`，对应常见的 `0644` 权限。

文件位置：`src/main/resources/application.yml`

```yaml
ssh:
  strict-host-key-checking: false
  known-hosts-path: ${user.home}/.ssh/known_hosts
  connect-timeout: 10s
  auth-timeout: 10s
  session-close-timeout: 3s
  command-timeout: 30s
  max-command-timeout: 300s
  sftp-buffer-size: 32768

  # SCP 上传文件默认权限，等价于 0644
  scp-file-permissions: "rw-r--r--"

  default-host:
    host: 192.168.1.100
    port: 22
    username: root
    password: "your_password"
    private-key-path:
    private-key-passphrase:
```

在 `SshProperties` 主类字段区域补充这个配置。

文件位置：`src/main/java/io/github/atengk/ssh/config/SshProperties.java`

```java
/**
 * SCP 上传文件默认权限
 */
private String scpFilePermissions = "rw-r--r--";
```

## SCP 操作结果对象

这个对象用于返回上传结果。下载接口采用流式响应，避免把远程文件一次性读入内存。

文件位置：`src/main/java/io/github/atengk/ssh/dto/ScpOperationResult.java`

```java
package io.github.atengk.ssh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SCP 操作结果
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScpOperationResult {

    /**
     * 操作类型
     */
    private String operation;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 远程路径
     */
    private String remotePath;

    /**
     * 文件大小，单位：字节
     */
    private Long size;

    /**
     * 结果消息
     */
    private String message;

}
```

## Service 接口

SCP 模块先只提供单文件上传和单文件下载。远程目录创建复用上一节的 `SftpFileService`，因为 SCP 本身不适合作为目录管理工具。

文件位置：`src/main/java/io/github/atengk/ssh/service/ScpFileService.java`

```java
package io.github.atengk.ssh.service;

import io.github.atengk.ssh.dto.ScpOperationResult;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * SCP 文件服务
 *
 * @author Ateng
 * @since 2026-04-29
 */
public interface ScpFileService {

    /**
     * 上传文件到默认主机
     *
     * @param remotePath       远程文件路径
     * @param inputStream      文件输入流
     * @param size             文件大小，单位：字节
     * @param createParentDirs 是否自动创建父目录
     * @return SCP 操作结果
     */
    ScpOperationResult uploadFile(String remotePath, InputStream inputStream, long size, boolean createParentDirs);

    /**
     * 从默认主机下载文件
     *
     * @param remotePath   远程文件路径
     * @param outputStream 输出流
     */
    void downloadFile(String remotePath, OutputStream outputStream);

}
```

## Service 实现

这里是 SCP 的核心封装。上传时使用 `ScpClient.upload(InputStream, remote, size, permissions, timestamp)`；下载时使用 `ScpClient.download(remote, OutputStream)`。`ScpClient` 由 `ScpClientCreator.instance().createScpClient(session)` 基于已认证的 `ClientSession` 创建。([JavaDoc](https://javadoc.io/doc/org.apache.sshd/sshd-scp/2.7.0/org/apache/sshd/scp/client/ScpClientCreator.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/ssh/service/impl/ScpFileServiceImpl.java`

```java
package io.github.atengk.ssh.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.ssh.config.SshProperties;
import io.github.atengk.ssh.core.SshSessionFactory;
import io.github.atengk.ssh.dto.ScpOperationResult;
import io.github.atengk.ssh.exception.SshOperationException;
import io.github.atengk.ssh.service.ScpFileService;
import io.github.atengk.ssh.service.SftpFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.scp.client.ScpClient;
import org.apache.sshd.scp.client.ScpClientCreator;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;

/**
 * SCP 文件服务实现
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScpFileServiceImpl implements ScpFileService {

    private final SshSessionFactory sshSessionFactory;

    private final SshProperties sshProperties;

    private final SftpFileService sftpFileService;

    /**
     * 上传文件到默认主机
     *
     * @param remotePath       远程文件路径
     * @param inputStream      文件输入流
     * @param size             文件大小，单位：字节
     * @param createParentDirs 是否自动创建父目录
     * @return SCP 操作结果
     */
    @Override
    public ScpOperationResult uploadFile(String remotePath, InputStream inputStream, long size, boolean createParentDirs) {
        Assert.notNull(inputStream, "上传文件输入流不能为空");
        Assert.isTrue(size >= 0, "上传文件大小不能小于0");

        String path = normalizeAndValidatePath(remotePath);

        try {
            if (createParentDirs) {
                createParentDirectory(path);
            }

            try (ClientSession session = sshSessionFactory.createDefaultSession()) {
                ScpClient scpClient = ScpClientCreator.instance().createScpClient(session);
                scpClient.upload(inputStream, path, size, resolveFilePermissions(), null);
            }

            log.info("SCP文件上传成功，remotePath={}，size={}", path, size);

            return ScpOperationResult.builder()
                    .operation("upload")
                    .success(Boolean.TRUE)
                    .remotePath(path)
                    .size(size)
                    .message("文件上传成功")
                    .build();
        } catch (Exception e) {
            log.error("SCP文件上传失败，remotePath={}，size={}", path, size, e);
            throw new SshOperationException("SCP文件上传失败：" + e.getMessage(), e);
        }
    }

    /**
     * 从默认主机下载文件
     *
     * @param remotePath   远程文件路径
     * @param outputStream 输出流
     */
    @Override
    public void downloadFile(String remotePath, OutputStream outputStream) {
        Assert.notNull(outputStream, "下载文件输出流不能为空");

        String path = normalizeAndValidatePath(remotePath);

        try (ClientSession session = sshSessionFactory.createDefaultSession()) {
            ScpClient scpClient = ScpClientCreator.instance().createScpClient(session);
            scpClient.download(path, outputStream);

            log.info("SCP文件下载成功，remotePath={}", path);
        } catch (Exception e) {
            log.error("SCP文件下载失败，remotePath={}", path, e);
            throw new SshOperationException("SCP文件下载失败：" + e.getMessage(), e);
        }
    }

    /**
     * 创建远程父目录
     *
     * @param remotePath 远程文件路径
     */
    private void createParentDirectory(String remotePath) {
        String parentPath = getParentPath(remotePath);
        if (StrUtil.equals(parentPath, "/")) {
            return;
        }

        sftpFileService.createDirectory(parentPath);
        log.info("SCP上传前置父目录检查完成，parentPath={}", parentPath);
    }

    /**
     * 解析上传文件权限
     *
     * @return POSIX 文件权限
     */
    private Set<PosixFilePermission> resolveFilePermissions() {
        String permissions = StrUtil.blankToDefault(sshProperties.getScpFilePermissions(), "rw-r--r--");

        try {
            return PosixFilePermissions.fromString(permissions);
        } catch (Exception e) {
            log.warn("SCP文件权限配置不合法，使用默认权限rw-r--r--，configured={}", permissions);
            return PosixFilePermissions.fromString("rw-r--r--");
        }
    }

    /**
     * 规范化并校验远程路径
     *
     * @param remotePath 远程路径
     * @return 规范化后的远程路径
     */
    private String normalizeAndValidatePath(String remotePath) {
        Assert.notBlank(remotePath, "远程路径不能为空");

        String path = StrUtil.trim(remotePath).replace('\\', '/');
        while (path.contains("//")) {
            path = path.replace("//", "/");
        }

        if (path.length() > 1 && path.endsWith("/")) {
            path = StrUtil.removeSuffix(path, "/");
        }

        Assert.isTrue(StrUtil.startWith(path, "/"), "远程路径必须使用绝对路径：{}", path);
        Assert.isFalse(StrUtil.contains(path, "\0"), "远程路径不能包含空字符：{}", path);

        List<String> parts = StrUtil.split(path, '/');
        Assert.isFalse(CollUtil.contains(parts, ".."), "远程路径不允许包含上级目录跳转：{}", path);

        return path;
    }

    /**
     * 获取父目录
     *
     * @param remotePath 远程文件路径
     * @return 父目录路径
     */
    private String getParentPath(String remotePath) {
        int index = remotePath.lastIndexOf("/");
        if (index <= 0) {
            return "/";
        }
        return remotePath.substring(0, index);
    }

}
```

## Controller 接口

这里提供 SCP 上传和下载接口。上传使用 `MultipartFile`，下载使用 `StreamingResponseBody`，避免大文件下载时占用过多 JVM 内存。

文件位置：`src/main/java/io/github/atengk/ssh/controller/ScpFileController.java`

```java
package io.github.atengk.ssh.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.ssh.dto.ScpOperationResult;
import io.github.atengk.ssh.service.ScpFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * SCP 文件控制器
 *
 * @author Ateng
 * @since 2026-04-29
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ssh/scp")
public class ScpFileController {

    private final ScpFileService scpFileService;

    /**
     * 上传文件到默认主机
     *
     * @param file             上传文件
     * @param remotePath       远程文件路径
     * @param createParentDirs 是否创建父目录
     * @return SCP 操作结果
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ScpOperationResult upload(@RequestParam("file") MultipartFile file,
                                     @RequestParam("remotePath") String remotePath,
                                     @RequestParam(value = "createParentDirs", defaultValue = "true") Boolean createParentDirs) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            return scpFileService.uploadFile(
                    remotePath,
                    inputStream,
                    file.getSize(),
                    BooleanUtil.isTrue(createParentDirs)
            );
        }
    }

    /**
     * 从默认主机下载文件
     *
     * @param remotePath 远程文件路径
     * @return 文件流响应
     */
    @GetMapping("/download")
    public ResponseEntity<StreamingResponseBody> download(@RequestParam("remotePath") String remotePath) {
        String filename = StrUtil.blankToDefault(FileUtil.getName(remotePath), "download.bin");

        StreamingResponseBody responseBody = outputStream -> scpFileService.downloadFile(remotePath, outputStream);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(responseBody);
    }

}
```

## 接口调用示例

先上传本地文件到远程服务器。`remotePath` 必须是远程完整文件路径。

```bash
curl -X POST 'http://localhost:8080/api/ssh/scp/upload' \
  -F 'file=@/tmp/test.txt' \
  -F 'remotePath=/tmp/ssh-demo/scp/test.txt' \
  -F 'createParentDirs=true'
```

返回示例：

```json
{
  "operation": "upload",
  "success": true,
  "remotePath": "/tmp/ssh-demo/scp/test.txt",
  "size": 128,
  "message": "文件上传成功"
}
```

下载远程文件到本地。

```bash
curl -L -o /tmp/test-scp-download.txt \
  'http://localhost:8080/api/ssh/scp/download?remotePath=/tmp/ssh-demo/scp/test.txt'
```

对比本地文件内容。

```bash
cat /tmp/test.txt
cat /tmp/test-scp-download.txt
```

这组命令用于确认上传和下载链路是否完整。如果两个文件内容一致，说明 SCP 上传、下载都已经正常。

## SCP 和 SFTP 的使用边界

SCP 适合做简单、直接的文件复制，尤其是“把一个本地文件传到远程路径”或“把一个远程文件拉回本地”。它不适合做复杂文件管理。

| 能力         | SCP                | SFTP   |
| ------------ | ------------------ | ------ |
| 上传单文件   | 适合               | 适合   |
| 下载单文件   | 适合               | 适合   |
| 查询目录     | 不适合             | 适合   |
| 删除文件     | 不适合             | 适合   |
| 创建目录     | 不适合             | 适合   |
| 重命名文件   | 不适合             | 适合   |
| 递归目录同步 | 可以扩展，但不优先 | 更适合 |

当前实现中，SCP 上传前如果需要创建父目录，会调用上一节的 `SftpFileService.createDirectory(...)`。这会额外创建一次 SFTP 会话，优点是职责清晰，缺点是上传前置目录创建和 SCP 上传不是同一个底层通道。后续如果要减少连接次数，可以把“创建目录”改成基于同一个 `ClientSession` 的内部工具方法。

## 注意事项

SCP 下载接口没有提前查询远程文件大小，因为 SCP 本身不是目录管理协议。如果你需要在下载前校验文件是否存在、是否为普通文件、文件大小是多少，可以先调用上一节的 SFTP `stat` 能力，再执行 SCP 下载。

SCP 上传会覆盖远程同名文件。Apache MINA SSHD 的 SCP API 更接近复制语义，不像我们上一节 SFTP 上传那样显式传入 `overwrite` 参数。如果你需要“不允许覆盖已有文件”，建议上传前通过 SFTP `exists(remotePath)` 检查一次，存在则拒绝上传。

生产环境仍然建议加这些控制：

```text
1. 限制远程路径白名单，例如只允许 /data/upload、/opt/app/packages
2. 限制上传文件大小，避免大文件占满带宽和磁盘
3. 限制上传文件类型，避免上传脚本、二进制危险文件
4. 增加操作审计，记录操作人、远程路径、文件大小、耗时
5. 增加并发限制，避免同时发起大量 SCP 传输
6. 下载前通过 SFTP 查询文件属性，避免下载目录或异常路径
```

下一步可以继续扩展 **多主机配置与动态主机选择**，把现在的 `default-host` 改造成按 `hostId` 选择目标服务器。

# 多主机配置与动态主机选择

这一节把前面固定的 `default-host` 改造成主机池配置。后续 SSH 命令执行、SFTP、SCP 都可以通过 `hostId` 选择目标 Linux 服务器；如果没有传 `hostId`，则使用 `default-host-id` 指定的默认主机。

## 文件结构

本节主要改造配置类和会话工厂，并补充一个主机解析器。

```text
src/main/java/io/github/atengk/ssh/config/SshProperties.java
src/main/java/io/github/atengk/ssh/core/SshHostResolver.java
src/main/java/io/github/atengk/ssh/core/SshSessionFactory.java
src/main/java/io/github/atengk/ssh/dto/SshCommandRequest.java
src/main/java/io/github/atengk/ssh/dto/SshCommandResult.java
src/main/resources/application.yml
```

## 多主机 application.yml

这里用 `hosts` 管理多台服务器，`default-host-id` 指定默认服务器。后续接口传入 `hostId` 时使用指定主机，不传时使用默认主机。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

ssh:
  # 是否启用严格主机密钥校验
  strict-host-key-checking: false

  # known_hosts 文件路径
  known-hosts-path: ${user.home}/.ssh/known_hosts

  # SSH 连接超时时间
  connect-timeout: 10s

  # SSH 认证超时时间
  auth-timeout: 10s

  # SSH 会话关闭等待时间
  session-close-timeout: 3s

  # 默认命令执行超时时间
  command-timeout: 30s

  # 最大命令执行超时时间
  max-command-timeout: 300s

  # SFTP 文件读写缓冲区大小
  sftp-buffer-size: 32768

  # SCP 上传文件默认权限
  scp-file-permissions: "rw-r--r--"

  # 默认主机 ID，不传 hostId 时使用该主机
  default-host-id: dev-01

  # 多主机配置
  hosts:
    dev-01:
      host: 192.168.1.100
      port: 22
      username: root
      password: "your_password"
      private-key-path:
      private-key-passphrase:

    test-01:
      host: 192.168.1.101
      port: 22
      username: deploy
      password:
      private-key-path: /home/app/.ssh/id_rsa
      private-key-passphrase:

    prod-01:
      host: 10.0.0.10
      port: 22
      username: deploy
      password:
      private-key-path: /home/app/.ssh/prod_rsa
      private-key-passphrase: "your_key_passphrase"
```

建议从这一节开始废弃旧的 `default-host` 配置，统一使用 `hosts + default-host-id`，避免后续代码同时兼容两套结构导致维护复杂。

## 多主机配置属性类

这里完整替换前面的 `SshProperties`。核心变化是新增 `defaultHostId` 和 `Map<String, Host> hosts`。

文件位置：`src/main/java/io/github/atengk/ssh/config/SshProperties.java`

```java
package io.github.atengk.ssh.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SSH 配置属性
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Data
@Validated
@ConfigurationProperties(prefix = "ssh")
public class SshProperties {

    /**
     * 是否启用严格主机密钥校验
     */
    private Boolean strictHostKeyChecking = Boolean.TRUE;

    /**
     * known_hosts 文件路径
     */
    private String knownHostsPath = System.getProperty("user.home") + "/.ssh/known_hosts";

    /**
     * SSH 连接超时时间
     */
    private Duration connectTimeout = Duration.ofSeconds(10);

    /**
     * SSH 认证超时时间
     */
    private Duration authTimeout = Duration.ofSeconds(10);

    /**
     * SSH 会话关闭等待时间
     */
    private Duration sessionCloseTimeout = Duration.ofSeconds(3);

    /**
     * 默认命令执行超时时间
     */
    private Duration commandTimeout = Duration.ofSeconds(30);

    /**
     * 最大允许命令执行超时时间
     */
    private Duration maxCommandTimeout = Duration.ofSeconds(300);

    /**
     * SFTP 文件读写缓冲区大小
     */
    private Integer sftpBufferSize = 32768;

    /**
     * SCP 上传文件默认权限
     */
    private String scpFilePermissions = "rw-r--r--";

    /**
     * 默认主机 ID
     */
    private String defaultHostId;

    /**
     * SSH 主机池
     */
    private Map<String, Host> hosts = new LinkedHashMap<>();

    /**
     * SSH 主机配置
     *
     * @author Ateng
     * @since 2026-04-29
     */
    @Data
    public static class Host {

        /**
         * 主机地址
         */
        private String host;

        /**
         * SSH 端口
         */
        private Integer port = 22;

        /**
         * 登录用户名
         */
        private String username;

        /**
         * 登录密码
         */
        private String password;

        /**
         * 私钥文件路径
         */
        private String privateKeyPath;

        /**
         * 私钥密码
         */
        private String privateKeyPassphrase;
    }

}
```

## 主机解析器

`SshHostResolver` 负责统一解析 `hostId`。业务层不要自己从 `SshProperties` 里取主机，统一通过这个类，避免默认主机、空值、未知主机的处理逻辑分散到各个 Service。

文件位置：`src/main/java/io/github/atengk/ssh/core/SshHostResolver.java`

```java
package io.github.atengk.ssh.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.ssh.config.SshProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * SSH 主机解析器
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Component
@RequiredArgsConstructor
public class SshHostResolver {

    private final SshProperties sshProperties;

    /**
     * 解析主机 ID，未传时返回默认主机 ID
     *
     * @param hostId 请求主机 ID
     * @return 实际主机 ID
     */
    public String resolveHostId(String hostId) {
        String resolvedHostId = StrUtil.blankToDefault(hostId, sshProperties.getDefaultHostId());
        Assert.notBlank(resolvedHostId, "SSH默认主机ID不能为空，请配置 ssh.default-host-id");
        Assert.isTrue(CollUtil.isNotEmpty(sshProperties.getHosts()), "SSH主机池不能为空，请配置 ssh.hosts");
        Assert.isTrue(sshProperties.getHosts().containsKey(resolvedHostId), "SSH主机不存在：{}", resolvedHostId);
        return resolvedHostId;
    }

    /**
     * 获取主机配置，未传主机 ID 时返回默认主机
     *
     * @param hostId 请求主机 ID
     * @return SSH 主机配置
     */
    public SshProperties.Host getHostOrDefault(String hostId) {
        String resolvedHostId = resolveHostId(hostId);
        return sshProperties.getHosts().get(resolvedHostId);
    }

    /**
     * 获取默认主机配置
     *
     * @return SSH 主机配置
     */
    public SshProperties.Host getDefaultHost() {
        return getHostOrDefault(sshProperties.getDefaultHostId());
    }

    /**
     * 查询所有主机 ID
     *
     * @return 主机 ID 集合
     */
    public Set<String> listHostIds() {
        Assert.isTrue(CollUtil.isNotEmpty(sshProperties.getHosts()), "SSH主机池不能为空，请配置 ssh.hosts");
        return sshProperties.getHosts().keySet();
    }

}
```

## 会话工厂改造

`SshSessionFactory` 增加 `createSessionByHostId(String hostId)`。后续命令执行、SFTP、SCP 都通过这个方法连接指定主机。

文件位置：`src/main/java/io/github/atengk/ssh/core/SshSessionFactory.java`

```java
package io.github.atengk.ssh.core;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.ssh.config.SshProperties;
import io.github.atengk.ssh.exception.SshOperationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.config.keys.loader.KeyPairResourceLoader;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.util.Collection;

/**
 * SSH 会话工厂
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SshSessionFactory {

    private final SshClient sshClient;

    private final SshProperties sshProperties;

    private final SshHostResolver sshHostResolver;

    /**
     * 创建默认 SSH 会话
     *
     * @return 已认证的 SSH 会话
     */
    public ClientSession createDefaultSession() {
        return createSessionByHostId(null);
    }

    /**
     * 根据主机 ID 创建 SSH 会话
     *
     * @param hostId 主机 ID，为空时使用默认主机
     * @return 已认证的 SSH 会话
     */
    public ClientSession createSessionByHostId(String hostId) {
        String resolvedHostId = sshHostResolver.resolveHostId(hostId);
        SshProperties.Host host = sshHostResolver.getHostOrDefault(resolvedHostId);
        return createSession(resolvedHostId, host);
    }

    /**
     * 创建指定主机的 SSH 会话
     *
     * @param hostId 主机 ID
     * @param host   SSH 主机配置
     * @return 已认证的 SSH 会话
     */
    public ClientSession createSession(String hostId, SshProperties.Host host) {
        validateHost(hostId, host);

        try {
            ClientSession session = sshClient
                    .connect(host.getUsername(), host.getHost(), host.getPort())
                    .verify(sshProperties.getConnectTimeout())
                    .getSession();

            applyIdentity(session, host);
            session.auth().verify(sshProperties.getAuthTimeout());

            log.info("SSH连接认证成功，hostId={}，host={}，port={}，username={}",
                    hostId, host.getHost(), host.getPort(), host.getUsername());
            return session;
        } catch (Exception e) {
            log.error("SSH连接认证失败，hostId={}，host={}，port={}，username={}",
                    hostId, host.getHost(), host.getPort(), host.getUsername(), e);
            throw new SshOperationException("SSH连接认证失败：" + e.getMessage(), e);
        }
    }

    /**
     * 添加 SSH 认证身份
     *
     * @param session SSH 会话
     * @param host    SSH 主机配置
     */
    private void applyIdentity(ClientSession session, SshProperties.Host host) throws Exception {
        boolean hasPassword = StrUtil.isNotBlank(host.getPassword());
        boolean hasPrivateKey = StrUtil.isNotBlank(host.getPrivateKeyPath());

        Assert.isTrue(hasPassword || hasPrivateKey, "SSH认证信息不能为空，请配置 password 或 private-key-path");

        if (hasPrivateKey) {
            loadPrivateKeyIdentities(session, host);
        }

        if (hasPassword) {
            session.addPasswordIdentity(host.getPassword());
        }
    }

    /**
     * 加载私钥认证身份
     *
     * @param session SSH 会话
     * @param host    SSH 主机配置
     */
    private void loadPrivateKeyIdentities(ClientSession session, SshProperties.Host host) throws Exception {
        Path privateKeyPath = Paths.get(host.getPrivateKeyPath());
        Assert.isTrue(Files.isRegularFile(privateKeyPath), "SSH私钥文件不存在：{}", privateKeyPath);

        FilePasswordProvider passwordProvider = StrUtil.isBlank(host.getPrivateKeyPassphrase())
                ? FilePasswordProvider.EMPTY
                : resourceKey -> host.getPrivateKeyPassphrase();

        KeyPairResourceLoader loader = SecurityUtils.getKeyPairResourceParser();
        Collection<KeyPair> keyPairs = loader.loadKeyPairs(null, privateKeyPath, passwordProvider);

        Assert.notEmpty(keyPairs, "SSH私钥文件未加载到有效密钥：{}", privateKeyPath);

        for (KeyPair keyPair : keyPairs) {
            session.addPublicKeyIdentity(keyPair);
        }

        log.info("SSH私钥认证身份加载成功，privateKeyPath={}，keyCount={}", privateKeyPath, keyPairs.size());
    }

    /**
     * 校验 SSH 主机配置
     *
     * @param hostId 主机 ID
     * @param host   SSH 主机配置
     */
    private void validateHost(String hostId, SshProperties.Host host) {
        Assert.notBlank(hostId, "SSH主机ID不能为空");
        Assert.notNull(host, "SSH主机配置不能为空");
        Assert.notBlank(host.getHost(), "SSH主机地址不能为空");
        Assert.notBlank(host.getUsername(), "SSH用户名不能为空");
        Assert.isTrue(host.getPort() != null && host.getPort() > 0 && host.getPort() <= 65535,
                "SSH端口不合法：{}", host.getPort());
    }

}
```

## 命令执行 DTO 改造

命令执行请求增加 `hostId`。如果为空，则使用默认主机。

文件位置：`src/main/java/io/github/atengk/ssh/dto/SshCommandRequest.java`

```java
package io.github.atengk.ssh.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * SSH 命令执行请求
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Data
public class SshCommandRequest {

    /**
     * 主机 ID，为空时使用默认主机
     */
    private String hostId;

    /**
     * 要执行的 Linux 命令
     */
    @NotBlank(message = "命令不能为空")
    private String command;

    /**
     * 命令执行超时时间，单位：秒
     */
    @Min(value = 1, message = "命令超时时间不能小于1秒")
    @Max(value = 300, message = "命令超时时间不能大于300秒")
    private Long timeoutSeconds;

    /**
     * 退出码非 0 时是否抛出异常
     */
    private Boolean failOnNonZeroExit = Boolean.FALSE;

}
```

命令执行结果也建议增加 `hostId`，方便前端、日志和批量任务区分结果来源。

文件位置：`src/main/java/io/github/atengk/ssh/dto/SshCommandResult.java`

```java
package io.github.atengk.ssh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSH 命令执行结果
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SshCommandResult {

    /**
     * 主机 ID
     */
    private String hostId;

    /**
     * 执行命令
     */
    private String command;

    /**
     * 是否执行成功
     */
    private Boolean success;

    /**
     * 是否执行超时
     */
    private Boolean timeout;

    /**
     * 退出码
     */
    private Integer exitCode;

    /**
     * 标准输出
     */
    private String stdout;

    /**
     * 错误输出
     */
    private String stderr;

    /**
     * 执行耗时，单位：毫秒
     */
    private Long durationMillis;

}
```

## 命令执行 Service 关键改造

`SshCommandServiceImpl` 中原来使用：

```java
ClientSession session = sshSessionFactory.createDefaultSession()
```

现在改成根据 `hostId` 创建会话。

文件位置：`src/main/java/io/github/atengk/ssh/service/impl/SshCommandServiceImpl.java`

```java
String hostId = sshHostResolver.resolveHostId(request.getHostId());

try (ClientSession session = sshSessionFactory.createSessionByHostId(hostId);
     ChannelExec channel = session.createExecChannel(command);
     ByteArrayOutputStream stdout = new ByteArrayOutputStream();
     ByteArrayOutputStream stderr = new ByteArrayOutputStream()) {

    // 原有执行逻辑保持不变
}
```

同时需要在 `SshCommandServiceImpl` 中注入 `SshHostResolver`。

```java
private final SshHostResolver sshHostResolver;
```

组装结果时补充 `hostId`。

```java
SshCommandResult result = SshCommandResult.builder()
        .hostId(hostId)
        .command(command)
        .success(!timeoutFlag && Integer.valueOf(0).equals(exitCode))
        .timeout(timeoutFlag)
        .exitCode(exitCode)
        .stdout(stdoutText)
        .stderr(stderrText)
        .durationMillis(stopWatch.getTotalTimeMillis())
        .build();
```

## SFTP Service 改造方向

SFTP 接口建议保留默认主机方法，同时增加带 `hostId` 的重载方法。这样之前的接口不受影响，新接口可以支持动态主机。

文件位置：`src/main/java/io/github/atengk/ssh/service/SftpFileService.java`

```java
package io.github.atengk.ssh.service;

import io.github.atengk.ssh.dto.SftpEntry;
import io.github.atengk.ssh.dto.SftpOperationResult;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * SFTP 文件服务
 *
 * @author Ateng
 * @since 2026-04-29
 */
public interface SftpFileService {

    /**
     * 上传文件到默认主机
     *
     * @param remotePath       远程文件路径
     * @param inputStream      文件输入流
     * @param overwrite        是否覆盖已有文件
     * @param createParentDirs 是否自动创建父目录
     * @return SFTP 操作结果
     */
    SftpOperationResult uploadFile(String remotePath, InputStream inputStream, boolean overwrite, boolean createParentDirs);

    /**
     * 上传文件到指定主机
     *
     * @param hostId           主机 ID
     * @param remotePath       远程文件路径
     * @param inputStream      文件输入流
     * @param overwrite        是否覆盖已有文件
     * @param createParentDirs 是否自动创建父目录
     * @return SFTP 操作结果
     */
    SftpOperationResult uploadFile(String hostId, String remotePath, InputStream inputStream, boolean overwrite, boolean createParentDirs);

    /**
     * 从默认主机下载文件
     *
     * @param remotePath   远程文件路径
     * @param outputStream 输出流
     */
    void downloadFile(String remotePath, OutputStream outputStream);

    /**
     * 从指定主机下载文件
     *
     * @param hostId       主机 ID
     * @param remotePath   远程文件路径
     * @param outputStream 输出流
     */
    void downloadFile(String hostId, String remotePath, OutputStream outputStream);

    /**
     * 删除默认主机上的文件
     *
     * @param remotePath 远程文件路径
     * @return SFTP 操作结果
     */
    SftpOperationResult deleteFile(String remotePath);

    /**
     * 删除指定主机上的文件
     *
     * @param hostId     主机 ID
     * @param remotePath 远程文件路径
     * @return SFTP 操作结果
     */
    SftpOperationResult deleteFile(String hostId, String remotePath);

    /**
     * 删除默认主机上的目录
     *
     * @param remoteDir 远程目录路径
     * @param recursive 是否递归删除
     * @return SFTP 操作结果
     */
    SftpOperationResult deleteDirectory(String remoteDir, boolean recursive);

    /**
     * 删除指定主机上的目录
     *
     * @param hostId    主机 ID
     * @param remoteDir 远程目录路径
     * @param recursive 是否递归删除
     * @return SFTP 操作结果
     */
    SftpOperationResult deleteDirectory(String hostId, String remoteDir, boolean recursive);

    /**
     * 在默认主机上创建目录
     *
     * @param remoteDir 远程目录路径
     * @return SFTP 操作结果
     */
    SftpOperationResult createDirectory(String remoteDir);

    /**
     * 在指定主机上创建目录
     *
     * @param hostId    主机 ID
     * @param remoteDir 远程目录路径
     * @return SFTP 操作结果
     */
    SftpOperationResult createDirectory(String hostId, String remoteDir);

    /**
     * 查询默认主机上的目录列表
     *
     * @param remoteDir 远程目录路径
     * @return 目录条目列表
     */
    List<SftpEntry> listDirectory(String remoteDir);

    /**
     * 查询指定主机上的目录列表
     *
     * @param hostId    主机 ID
     * @param remoteDir 远程目录路径
     * @return 目录条目列表
     */
    List<SftpEntry> listDirectory(String hostId, String remoteDir);

    /**
     * 判断默认主机上的路径是否存在
     *
     * @param remotePath 远程路径
     * @return 是否存在
     */
    boolean exists(String remotePath);

    /**
     * 判断指定主机上的路径是否存在
     *
     * @param hostId     主机 ID
     * @param remotePath 远程路径
     * @return 是否存在
     */
    boolean exists(String hostId, String remotePath);

}
```

在 `SftpFileServiceImpl` 中，默认主机方法直接委托给带 `hostId` 的方法。

```java
@Override
public SftpOperationResult uploadFile(String remotePath, InputStream inputStream, boolean overwrite, boolean createParentDirs) {
    return uploadFile(null, remotePath, inputStream, overwrite, createParentDirs);
}

@Override
public void downloadFile(String remotePath, OutputStream outputStream) {
    downloadFile(null, remotePath, outputStream);
}

@Override
public SftpOperationResult deleteFile(String remotePath) {
    return deleteFile(null, remotePath);
}

@Override
public SftpOperationResult deleteDirectory(String remoteDir, boolean recursive) {
    return deleteDirectory(null, remoteDir, recursive);
}

@Override
public SftpOperationResult createDirectory(String remoteDir) {
    return createDirectory(null, remoteDir);
}

@Override
public List<SftpEntry> listDirectory(String remoteDir) {
    return listDirectory(null, remoteDir);
}

@Override
public boolean exists(String remotePath) {
    return exists(null, remotePath);
}
```

然后把原来所有的：

```java
sshSessionFactory.createDefaultSession()
```

改成：

```java
String resolvedHostId = sshHostResolver.resolveHostId(hostId);

try (ClientSession session = sshSessionFactory.createSessionByHostId(resolvedHostId);
     SftpClient sftpClient = SftpClientFactory.instance().createSftpClient(session)) {

    // 原有 SFTP 逻辑保持不变
}
```

同时建议在 `SftpOperationResult` 中增加 `hostId` 字段，方便返回给前端。

```java
/**
 * 主机 ID
 */
private String hostId;
```

组装结果时补充：

```java
.hostId(resolvedHostId)
```

## SCP Service 改造方向

SCP 和 SFTP 一样，保留默认主机方法，增加带 `hostId` 的重载方法。

文件位置：`src/main/java/io/github/atengk/ssh/service/ScpFileService.java`

```java
package io.github.atengk.ssh.service;

import io.github.atengk.ssh.dto.ScpOperationResult;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * SCP 文件服务
 *
 * @author Ateng
 * @since 2026-04-29
 */
public interface ScpFileService {

    /**
     * 上传文件到默认主机
     *
     * @param remotePath       远程文件路径
     * @param inputStream      文件输入流
     * @param size             文件大小，单位：字节
     * @param createParentDirs 是否自动创建父目录
     * @return SCP 操作结果
     */
    ScpOperationResult uploadFile(String remotePath, InputStream inputStream, long size, boolean createParentDirs);

    /**
     * 上传文件到指定主机
     *
     * @param hostId           主机 ID
     * @param remotePath       远程文件路径
     * @param inputStream      文件输入流
     * @param size             文件大小，单位：字节
     * @param createParentDirs 是否自动创建父目录
     * @return SCP 操作结果
     */
    ScpOperationResult uploadFile(String hostId, String remotePath, InputStream inputStream, long size, boolean createParentDirs);

    /**
     * 从默认主机下载文件
     *
     * @param remotePath   远程文件路径
     * @param outputStream 输出流
     */
    void downloadFile(String remotePath, OutputStream outputStream);

    /**
     * 从指定主机下载文件
     *
     * @param hostId       主机 ID
     * @param remotePath   远程文件路径
     * @param outputStream 输出流
     */
    void downloadFile(String hostId, String remotePath, OutputStream outputStream);

}
```

在 `ScpFileServiceImpl` 中，默认主机方法同样委托给带 `hostId` 的方法。

```java
@Override
public ScpOperationResult uploadFile(String remotePath, InputStream inputStream, long size, boolean createParentDirs) {
    return uploadFile(null, remotePath, inputStream, size, createParentDirs);
}

@Override
public void downloadFile(String remotePath, OutputStream outputStream) {
    downloadFile(null, remotePath, outputStream);
}
```

带 `hostId` 的 SCP 上传方法中，把原来的默认会话改成动态主机会话。

```java
String resolvedHostId = sshHostResolver.resolveHostId(hostId);

try (ClientSession session = sshSessionFactory.createSessionByHostId(resolvedHostId)) {
    ScpClient scpClient = ScpClientCreator.instance().createScpClient(session);
    scpClient.upload(inputStream, path, size, resolveFilePermissions(), null);
}
```

如果 SCP 上传前需要创建父目录，也要把 `hostId` 传给 SFTP。

```java
sftpFileService.createDirectory(resolvedHostId, parentPath);
```

同时建议在 `ScpOperationResult` 中增加 `hostId` 字段。

```java
/**
 * 主机 ID
 */
private String hostId;
```

## Controller 改造方式

命令执行接口已经可以直接通过请求体传 `hostId`。

```json
{
  "hostId": "test-01",
  "command": "hostname && whoami && df -h",
  "timeoutSeconds": 10,
  "failOnNonZeroExit": false
}
```

SFTP 和 SCP 这类文件接口建议把 `hostId` 作为可选请求参数。

例如 SFTP 上传接口：

```java
@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public SftpOperationResult upload(@RequestParam(value = "hostId", required = false) String hostId,
                                  @RequestParam("file") MultipartFile file,
                                  @RequestParam("remotePath") String remotePath,
                                  @RequestParam(value = "overwrite", defaultValue = "true") Boolean overwrite,
                                  @RequestParam(value = "createParentDirs", defaultValue = "true") Boolean createParentDirs) throws Exception {
    try (InputStream inputStream = file.getInputStream()) {
        return sftpFileService.uploadFile(
                hostId,
                remotePath,
                inputStream,
                BooleanUtil.isTrue(overwrite),
                BooleanUtil.isTrue(createParentDirs)
        );
    }
}
```

SCP 上传接口同理：

```java
@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ScpOperationResult upload(@RequestParam(value = "hostId", required = false) String hostId,
                                 @RequestParam("file") MultipartFile file,
                                 @RequestParam("remotePath") String remotePath,
                                 @RequestParam(value = "createParentDirs", defaultValue = "true") Boolean createParentDirs) throws Exception {
    try (InputStream inputStream = file.getInputStream()) {
        return scpFileService.uploadFile(
                hostId,
                remotePath,
                inputStream,
                file.getSize(),
                BooleanUtil.isTrue(createParentDirs)
        );
    }
}
```

## 调用示例

不传 `hostId`，使用默认主机 `dev-01`。

```bash
curl -X POST 'http://localhost:8080/api/ssh/commands/execute-default' \
  -H 'Content-Type: application/json' \
  -d '{
    "command": "hostname && whoami",
    "timeoutSeconds": 10
  }'
```

指定 `test-01` 执行命令。

```bash
curl -X POST 'http://localhost:8080/api/ssh/commands/execute-default' \
  -H 'Content-Type: application/json' \
  -d '{
    "hostId": "test-01",
    "command": "hostname && whoami && pwd",
    "timeoutSeconds": 10
  }'
```

指定 `test-01` 上传文件。

```bash
curl -X POST 'http://localhost:8080/api/ssh/sftp/upload' \
  -F 'hostId=test-01' \
  -F 'file=@/tmp/test.txt' \
  -F 'remotePath=/tmp/ssh-demo/upload/test.txt' \
  -F 'overwrite=true' \
  -F 'createParentDirs=true'
```

指定 `prod-01` 查询目录。

```bash
curl -X GET 'http://localhost:8080/api/ssh/sftp/directory/list?hostId=prod-01&remoteDir=/tmp'
```

指定 `test-01` 使用 SCP 下载文件。

```bash
curl -L -o /tmp/test-scp-download.txt \
  'http://localhost:8080/api/ssh/scp/download?hostId=test-01&remotePath=/tmp/ssh-demo/scp/test.txt'
```

## 注意事项

多主机配置不要把生产服务器密码直接写进配置文件。更合理的做法是把密码、私钥密码放到环境变量、Kubernetes Secret、配置中心或密钥管理系统中，例如：

```yaml
ssh:
  hosts:
    prod-01:
      host: 10.0.0.10
      port: 22
      username: deploy
      password: ${SSH_PROD_01_PASSWORD:}
      private-key-path: /home/app/.ssh/prod_rsa
      private-key-passphrase: ${SSH_PROD_01_KEY_PASSPHRASE:}
```

如果主机数量较多，不建议继续写在 `application.yml`。后续可以把主机信息放到数据库，例如 `ssh_host` 表，通过 `hostId` 查询主机、认证方式、可操作目录、环境类型、状态、标签等。

这一节完成后，当前 SSH 模块已经具备这些基础能力：

```text
1. 多主机配置
2. 动态 hostId 选择
3. SSH 命令执行
4. SFTP 上传、下载、删除、目录操作
5. SCP 上传、下载
```

下一步适合继续做 **主机管理表设计 + MyBatis-Plus 动态读取主机配置**，把 `application.yml` 里的 `hosts` 改造成数据库管理。

# 主机管理表设计与数据库动态读取

这一节把上一节的 `ssh.hosts` 静态配置改造成数据库管理。这样可以在后台页面或接口中维护 Linux 主机信息，后续批量执行、主机分组、环境区分、权限控制、审计日志都会更自然。

本节保留 `default-host-id` 作为默认主机标识，但主机详情从数据库 `ssh_host` 表读取。

## 文件结构

本节新增 MyBatis-Plus、数据库表、实体类、Mapper、Service，并改造 `SshHostResolver`。

```text
src/main/java/io/github/atengk/ssh/entity/SshHost.java
src/main/java/io/github/atengk/ssh/mapper/SshHostMapper.java
src/main/java/io/github/atengk/ssh/service/SshHostService.java
src/main/java/io/github/atengk/ssh/service/impl/SshHostServiceImpl.java
src/main/java/io/github/atengk/ssh/core/SshHostResolver.java
src/main/resources/mapper/SshHostMapper.xml
src/main/resources/application.yml
```

## Maven 依赖

这里增加 MyBatis-Plus 和 MySQL 驱动。如果你的项目使用 PostgreSQL，只需要替换数据库驱动和 JDBC URL。

文件位置：`pom.xml`

```xml
<!-- MyBatis-Plus：主机信息 CRUD 与动态查询 -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.12</version>
</dependency>

<!-- MySQL 驱动：连接 MySQL 数据库 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

## 数据库表设计

`ssh_host` 表用于保存 SSH 主机连接信息。这里先把密码、私钥密码直接作为字段演示；生产环境建议加密存储，或者只保存密钥引用，不直接保存明文密钥。

文件位置：`sql/ssh_host.sql`

```sql
CREATE TABLE ssh_host (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    host_id VARCHAR(64) NOT NULL COMMENT '主机唯一标识',
    host_name VARCHAR(128) NOT NULL COMMENT '主机名称',
    host VARCHAR(128) NOT NULL COMMENT '主机地址',
    port INT NOT NULL DEFAULT 22 COMMENT 'SSH端口',
    username VARCHAR(128) NOT NULL COMMENT '登录用户名',

    auth_type VARCHAR(32) NOT NULL DEFAULT 'PASSWORD' COMMENT '认证方式：PASSWORD、PRIVATE_KEY、PASSWORD_AND_PRIVATE_KEY',
    password VARCHAR(512) DEFAULT NULL COMMENT '登录密码，生产建议加密存储',
    private_key_path VARCHAR(512) DEFAULT NULL COMMENT '私钥文件路径',
    private_key_passphrase VARCHAR(512) DEFAULT NULL COMMENT '私钥密码，生产建议加密存储',

    environment VARCHAR(32) DEFAULT NULL COMMENT '环境标识：dev、test、prod',
    tags VARCHAR(512) DEFAULT NULL COMMENT '标签，多个标签用英文逗号分隔',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0否，1是',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',

    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_host_id (host_id),
    KEY idx_environment (environment),
    KEY idx_enabled (enabled)
) COMMENT='SSH主机配置表';
```

初始化几台测试主机。

```sql
INSERT INTO ssh_host (
    host_id, host_name, host, port, username, auth_type, password,
    environment, tags, enabled, remark
) VALUES (
    'dev-01', '开发服务器01', '192.168.1.100', 22, 'root', 'PASSWORD', 'your_password',
    'dev', 'java,springboot,dev', 1, '开发环境测试服务器'
);

INSERT INTO ssh_host (
    host_id, host_name, host, port, username, auth_type, private_key_path, private_key_passphrase,
    environment, tags, enabled, remark
) VALUES (
    'test-01', '测试服务器01', '192.168.1.101', 22, 'deploy', 'PRIVATE_KEY', '/home/app/.ssh/id_rsa', NULL,
    'test', 'java,springboot,test', 1, '测试环境部署服务器'
);
```

## application.yml 配置

从这一节开始，`ssh.hosts` 可以删除，只保留通用配置和默认主机 ID。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  datasource:
    # MySQL 数据源配置
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/ssh_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root

mybatis-plus:
  # Mapper XML 文件位置
  mapper-locations: classpath*:/mapper/**/*.xml

  configuration:
    # SQL 日志，开发环境可开启，生产环境建议关闭或按需配置
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

  global-config:
    db-config:
      # 使用数据库自增主键
      id-type: auto

ssh:
  strict-host-key-checking: false
  known-hosts-path: ${user.home}/.ssh/known_hosts
  connect-timeout: 10s
  auth-timeout: 10s
  session-close-timeout: 3s
  command-timeout: 30s
  max-command-timeout: 300s
  sftp-buffer-size: 32768
  scp-file-permissions: "rw-r--r--"

  # 默认主机 ID，主机详情从 ssh_host 表读取
  default-host-id: dev-01
```

## 实体类

实体类映射 `ssh_host` 表。这里使用 MyBatis-Plus 注解，并保留和 `SshProperties.Host` 相同的核心连接字段，方便后续转换。

文件位置：`src/main/java/io/github/atengk/ssh/entity/SshHost.java`

```java
package io.github.atengk.ssh.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SSH 主机实体
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Data
@TableName("ssh_host")
public class SshHost {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 主机唯一标识
     */
    private String hostId;

    /**
     * 主机名称
     */
    private String hostName;

    /**
     * 主机地址
     */
    private String host;

    /**
     * SSH 端口
     */
    private Integer port;

    /**
     * 登录用户名
     */
    private String username;

    /**
     * 认证方式：PASSWORD、PRIVATE_KEY、PASSWORD_AND_PRIVATE_KEY
     */
    private String authType;

    /**
     * 登录密码
     */
    private String password;

    /**
     * 私钥文件路径
     */
    private String privateKeyPath;

    /**
     * 私钥密码
     */
    private String privateKeyPassphrase;

    /**
     * 环境标识
     */
    private String environment;

    /**
     * 标签
     */
    private String tags;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 备注
     */
    private String remark;

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

## Mapper

Mapper 先使用 MyBatis-Plus 基础 CRUD，复杂查询后续再扩展。

文件位置：`src/main/java/io/github/atengk/ssh/mapper/SshHostMapper.java`

```java
package io.github.atengk.ssh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.ssh.entity.SshHost;
import org.apache.ibatis.annotations.Mapper;

/**
 * SSH 主机 Mapper
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Mapper
public interface SshHostMapper extends BaseMapper<SshHost> {
}
```

如果暂时不写自定义 SQL，XML 可以先保留为空结构。

文件位置：`src/main/resources/mapper/SshHostMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="io.github.atengk.ssh.mapper.SshHostMapper">

</mapper>
```

## Service 接口

主机服务负责从数据库读取启用的主机，并转换成 SSH 连接配置。这样 `SshHostResolver` 不直接依赖 Mapper，职责更清晰。

文件位置：`src/main/java/io/github/atengk/ssh/service/SshHostService.java`

```java
package io.github.atengk.ssh.service;

import io.github.atengk.ssh.config.SshProperties;
import io.github.atengk.ssh.entity.SshHost;

import java.util.List;

/**
 * SSH 主机服务
 *
 * @author Ateng
 * @since 2026-04-29
 */
public interface SshHostService {

    /**
     * 根据主机 ID 查询启用的主机
     *
     * @param hostId 主机 ID
     * @return SSH 主机实体
     */
    SshHost getEnabledByHostId(String hostId);

    /**
     * 根据主机 ID 查询并转换为连接配置
     *
     * @param hostId 主机 ID
     * @return SSH 主机连接配置
     */
    SshProperties.Host getHostConfigByHostId(String hostId);

    /**
     * 查询所有启用主机
     *
     * @return 主机列表
     */
    List<SshHost> listEnabledHosts();

}
```

## Service 实现

这里使用 MyBatis-Plus 的 `lambdaQuery()` 查询启用主机，并转换为 `SshProperties.Host`。转换时只把 SSH 连接必要字段传给底层连接工厂。

文件位置：`src/main/java/io/github/atengk/ssh/service/impl/SshHostServiceImpl.java`

```java
package io.github.atengk.ssh.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.ssh.config.SshProperties;
import io.github.atengk.ssh.entity.SshHost;
import io.github.atengk.ssh.mapper.SshHostMapper;
import io.github.atengk.ssh.service.SshHostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * SSH 主机服务实现
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Service
public class SshHostServiceImpl extends ServiceImpl<SshHostMapper, SshHost> implements SshHostService {

    /**
     * 根据主机 ID 查询启用的主机
     *
     * @param hostId 主机 ID
     * @return SSH 主机实体
     */
    @Override
    public SshHost getEnabledByHostId(String hostId) {
        Assert.notBlank(hostId, "SSH主机ID不能为空");

        SshHost sshHost = lambdaQuery()
                .eq(SshHost::getHostId, hostId)
                .eq(SshHost::getEnabled, Boolean.TRUE)
                .one();

        Assert.notNull(sshHost, "SSH主机不存在或未启用：{}", hostId);
        return sshHost;
    }

    /**
     * 根据主机 ID 查询并转换为连接配置
     *
     * @param hostId 主机 ID
     * @return SSH 主机连接配置
     */
    @Override
    public SshProperties.Host getHostConfigByHostId(String hostId) {
        SshHost sshHost = getEnabledByHostId(hostId);

        SshProperties.Host host = new SshProperties.Host();
        host.setHost(sshHost.getHost());
        host.setPort(sshHost.getPort());
        host.setUsername(sshHost.getUsername());
        host.setPassword(sshHost.getPassword());
        host.setPrivateKeyPath(sshHost.getPrivateKeyPath());
        host.setPrivateKeyPassphrase(sshHost.getPrivateKeyPassphrase());

        log.info("SSH主机配置读取成功，hostId={}，host={}，port={}，username={}",
                sshHost.getHostId(), sshHost.getHost(), sshHost.getPort(), sshHost.getUsername());

        return host;
    }

    /**
     * 查询所有启用主机
     *
     * @return 主机列表
     */
    @Override
    public List<SshHost> listEnabledHosts() {
        return lambdaQuery()
                .eq(SshHost::getEnabled, Boolean.TRUE)
                .orderByAsc(SshHost::getEnvironment)
                .orderByAsc(SshHost::getHostId)
                .list();
    }

    /**
     * 校验认证配置
     *
     * @param sshHost SSH 主机实体
     */
    private void validateAuthConfig(SshHost sshHost) {
        boolean hasPassword = StrUtil.isNotBlank(sshHost.getPassword());
        boolean hasPrivateKey = StrUtil.isNotBlank(sshHost.getPrivateKeyPath());

        Assert.isTrue(hasPassword || hasPrivateKey,
                "SSH主机认证信息不能为空，hostId={}", sshHost.getHostId());
    }

}
```

上面 `validateAuthConfig` 方法已经准备好，但还没有调用。建议在 `getHostConfigByHostId` 中读取到主机后立即校验。

把这一行：

```java
SshHost sshHost = getEnabledByHostId(hostId);
```

改成下面这样：

```java
SshHost sshHost = getEnabledByHostId(hostId);
validateAuthConfig(sshHost);
```

## SshHostResolver 改造

这里把上一节基于 `sshProperties.getHosts()` 的解析逻辑改成查数据库。`resolveHostId` 仍然负责处理默认主机 ID，`getHostOrDefault` 负责返回连接配置。

文件位置：`src/main/java/io/github/atengk/ssh/core/SshHostResolver.java`

```java
package io.github.atengk.ssh.core;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.ssh.config.SshProperties;
import io.github.atengk.ssh.entity.SshHost;
import io.github.atengk.ssh.service.SshHostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SSH 主机解析器
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Component
@RequiredArgsConstructor
public class SshHostResolver {

    private final SshProperties sshProperties;

    private final SshHostService sshHostService;

    /**
     * 解析主机 ID，未传时返回默认主机 ID
     *
     * @param hostId 请求主机 ID
     * @return 实际主机 ID
     */
    public String resolveHostId(String hostId) {
        String resolvedHostId = StrUtil.blankToDefault(hostId, sshProperties.getDefaultHostId());
        Assert.notBlank(resolvedHostId, "SSH默认主机ID不能为空，请配置 ssh.default-host-id");
        return resolvedHostId;
    }

    /**
     * 获取主机配置，未传主机 ID 时返回默认主机
     *
     * @param hostId 请求主机 ID
     * @return SSH 主机配置
     */
    public SshProperties.Host getHostOrDefault(String hostId) {
        String resolvedHostId = resolveHostId(hostId);
        return sshHostService.getHostConfigByHostId(resolvedHostId);
    }

    /**
     * 获取默认主机配置
     *
     * @return SSH 主机配置
     */
    public SshProperties.Host getDefaultHost() {
        return getHostOrDefault(sshProperties.getDefaultHostId());
    }

    /**
     * 查询所有启用主机 ID
     *
     * @return 主机 ID 集合
     */
    public Set<String> listHostIds() {
        List<SshHost> hosts = sshHostService.listEnabledHosts();
        return hosts.stream()
                .map(SshHost::getHostId)
                .collect(Collectors.toSet());
    }

}
```

## SshSessionFactory 无需大改

上一节的 `SshSessionFactory` 如果已经通过 `SshHostResolver` 获取主机，那么这里只需要确认它调用的是数据库版 `SshHostResolver`。

文件位置：`src/main/java/io/github/atengk/ssh/core/SshSessionFactory.java`

```java
/**
 * 根据主机 ID 创建 SSH 会话
 *
 * @param hostId 主机 ID，为空时使用默认主机
 * @return 已认证的 SSH 会话
 */
public ClientSession createSessionByHostId(String hostId) {
    String resolvedHostId = sshHostResolver.resolveHostId(hostId);
    SshProperties.Host host = sshHostResolver.getHostOrDefault(resolvedHostId);
    return createSession(resolvedHostId, host);
}
```

这段逻辑可以继续保留。`getHostOrDefault` 内部已经从数据库查询主机。

## 主机查询接口

为了方便调试和前端选择主机，可以先提供一个只读查询接口。不要在这个接口返回密码、私钥密码等敏感字段。

文件位置：`src/main/java/io/github/atengk/ssh/dto/SshHostVO.java`

```java
package io.github.atengk.ssh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSH 主机视图对象
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SshHostVO {

    /**
     * 主机 ID
     */
    private String hostId;

    /**
     * 主机名称
     */
    private String hostName;

    /**
     * 主机地址
     */
    private String host;

    /**
     * SSH 端口
     */
    private Integer port;

    /**
     * 登录用户名
     */
    private String username;

    /**
     * 认证方式
     */
    private String authType;

    /**
     * 环境标识
     */
    private String environment;

    /**
     * 标签
     */
    private String tags;

    /**
     * 备注
     */
    private String remark;

}
```

文件位置：`src/main/java/io/github/atengk/ssh/controller/SshHostController.java`

```java
package io.github.atengk.ssh.controller;

import io.github.atengk.ssh.dto.SshHostVO;
import io.github.atengk.ssh.entity.SshHost;
import io.github.atengk.ssh.service.SshHostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SSH 主机控制器
 *
 * @author Ateng
 * @since 2026-04-29
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ssh/hosts")
public class SshHostController {

    private final SshHostService sshHostService;

    /**
     * 查询启用主机列表
     *
     * @return 主机列表
     */
    @GetMapping
    public List<SshHostVO> listEnabledHosts() {
        return sshHostService.listEnabledHosts()
                .stream()
                .map(this::toVO)
                .toList();
    }

    /**
     * 转换为视图对象
     *
     * @param sshHost SSH 主机实体
     * @return 主机视图对象
     */
    private SshHostVO toVO(SshHost sshHost) {
        return SshHostVO.builder()
                .hostId(sshHost.getHostId())
                .hostName(sshHost.getHostName())
                .host(sshHost.getHost())
                .port(sshHost.getPort())
                .username(sshHost.getUsername())
                .authType(sshHost.getAuthType())
                .environment(sshHost.getEnvironment())
                .tags(sshHost.getTags())
                .remark(sshHost.getRemark())
                .build();
    }

}
```

## 调用验证

先查询主机列表。

```bash
curl -X GET 'http://localhost:8080/api/ssh/hosts'
```

返回示例：

```json
[
  {
    "hostId": "dev-01",
    "hostName": "开发服务器01",
    "host": "192.168.1.100",
    "port": 22,
    "username": "root",
    "authType": "PASSWORD",
    "environment": "dev",
    "tags": "java,springboot,dev",
    "remark": "开发环境测试服务器"
  }
]
```

然后指定 `hostId` 执行命令。

```bash
curl -X POST 'http://localhost:8080/api/ssh/commands/execute-default' \
  -H 'Content-Type: application/json' \
  -d '{
    "hostId": "dev-01",
    "command": "hostname && whoami && pwd",
    "timeoutSeconds": 10,
    "failOnNonZeroExit": false
  }'
```

再指定 `hostId` 上传文件。

```bash
curl -X POST 'http://localhost:8080/api/ssh/sftp/upload' \
  -F 'hostId=dev-01' \
  -F 'file=@/tmp/test.txt' \
  -F 'remotePath=/tmp/ssh-demo/upload/test.txt' \
  -F 'overwrite=true' \
  -F 'createParentDirs=true'
```

## 安全注意事项

当前表结构能跑通功能，但生产环境不建议明文保存 `password` 和 `private_key_passphrase`。至少需要做以下改造：

```text
1. 使用 AES 或 KMS 加密保存密码字段
2. 接口返回对象永远不返回 password、privateKeyPassphrase
3. 操作生产主机必须加权限控制和审计日志
4. prod 环境主机建议只允许私钥认证，不建议密码认证
5. 数据库中的 private_key_path 应指向应用服务器本地安全目录
6. 私钥文件权限建议设置为 600
```

私钥权限可以这样处理：

```bash
chmod 600 /home/app/.ssh/prod_rsa
chown app:app /home/app/.ssh/prod_rsa
```

`chmod 600` 表示只有文件所有者可读写，其他用户没有权限。SSH 私钥文件权限过宽时，很多 SSH 客户端会拒绝使用该私钥。

这一节完成后，主机来源已经从静态配置升级为数据库动态读取。下一步适合继续做 **主机 CRUD 管理接口**，包括新增主机、修改主机、禁用主机、测试连接，以及密码脱敏和敏感字段加密。

# 主机 CRUD 管理接口

这一节继续在数据库主机管理基础上扩展：新增主机、修改主机、禁用主机、启用主机、测试连接、查询详情。重点是避免接口返回敏感字段，同时把密码、私钥密码做加密存储。

## 文件结构

本节新增 DTO、敏感字段加解密工具、主机管理接口，并改造 `SshHostService` 和 `SshHostServiceImpl`。

```text
src/main/java/io/github/atengk/ssh/enums/SshAuthType.java
src/main/java/io/github/atengk/ssh/dto/SshHostCreateRequest.java
src/main/java/io/github/atengk/ssh/dto/SshHostUpdateRequest.java
src/main/java/io/github/atengk/ssh/dto/SshHostVO.java
src/main/java/io/github/atengk/ssh/dto/SshHostTestResult.java
src/main/java/io/github/atengk/ssh/core/SshSecretCodec.java
src/main/java/io/github/atengk/ssh/service/SshHostService.java
src/main/java/io/github/atengk/ssh/service/impl/SshHostServiceImpl.java
src/main/java/io/github/atengk/ssh/controller/SshHostController.java
src/main/resources/application.yml
```

## 配置补充

这里增加敏感字段加密配置。`secret-key` 必须是 16、24 或 32 字节长度，用于 AES 加密。生产环境不要把密钥直接写死在配置文件里，建议从环境变量、Kubernetes Secret 或配置中心读取。

文件位置：`src/main/resources/application.yml`

```yaml
ssh:
  strict-host-key-checking: false
  known-hosts-path: ${user.home}/.ssh/known_hosts
  connect-timeout: 10s
  auth-timeout: 10s
  session-close-timeout: 3s
  command-timeout: 30s
  max-command-timeout: 300s
  sftp-buffer-size: 32768
  scp-file-permissions: "rw-r--r--"
  default-host-id: dev-01

  # 是否加密保存 password、private_key_passphrase
  encrypt-sensitive: true

  # AES 密钥，长度必须为 16、24 或 32 字节
  # 生产环境建议使用环境变量注入
  secret-key: ${SSH_SECRET_KEY:1234567890abcdef}
```

在 `SshProperties` 中补充字段。

文件位置：`src/main/java/io/github/atengk/ssh/config/SshProperties.java`

```java
/**
 * 是否加密保存敏感字段
 */
private Boolean encryptSensitive = Boolean.TRUE;

/**
 * 敏感字段加密密钥
 */
private String secretKey = "1234567890abcdef";
```

## 认证方式枚举

认证方式统一用枚举约束，避免接口传入任意字符串。

文件位置：`src/main/java/io/github/atengk/ssh/enums/SshAuthType.java`

```java
package io.github.atengk.ssh.enums;

/**
 * SSH 认证方式
 *
 * @author Ateng
 * @since 2026-04-29
 */
public enum SshAuthType {

    /**
     * 密码认证
     */
    PASSWORD,

    /**
     * 私钥认证
     */
    PRIVATE_KEY,

    /**
     * 密码和私钥认证
     */
    PASSWORD_AND_PRIVATE_KEY

}
```

## 敏感字段加解密工具

这个类专门处理密码、私钥密码的加密和解密。为了兼容历史明文数据，加密后的内容统一加 `{AES}` 前缀；解密时只有带 `{AES}` 前缀的内容才会解密，普通明文会原样返回。

文件位置：`src/main/java/io/github/atengk/ssh/core/SshSecretCodec.java`

```java
package io.github.atengk.ssh.core;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import io.github.atengk.ssh.config.SshProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SSH 敏感字段加解密工具
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SshSecretCodec {

    private static final String AES_PREFIX = "{AES}";

    private final SshProperties sshProperties;

    /**
     * 加密敏感字段
     *
     * @param plainText 明文
     * @return 密文
     */
    public String encode(String plainText) {
        if (StrUtil.isBlank(plainText)) {
            return plainText;
        }

        if (BooleanUtil.isFalse(sshProperties.getEncryptSensitive())) {
            return plainText;
        }

        if (StrUtil.startWith(plainText, AES_PREFIX)) {
            return plainText;
        }

        return AES_PREFIX + buildAes().encryptBase64(plainText, CharsetUtil.CHARSET_UTF_8);
    }

    /**
     * 解密敏感字段
     *
     * @param cipherText 密文
     * @return 明文
     */
    public String decode(String cipherText) {
        if (StrUtil.isBlank(cipherText)) {
            return cipherText;
        }

        if (!StrUtil.startWith(cipherText, AES_PREFIX)) {
            return cipherText;
        }

        String encryptedText = StrUtil.removePrefix(cipherText, AES_PREFIX);
        return buildAes().decryptStr(encryptedText, CharsetUtil.CHARSET_UTF_8);
    }

    /**
     * 构建 AES 工具
     *
     * @return AES 工具
     */
    private AES buildAes() {
        String secretKey = sshProperties.getSecretKey();
        Assert.notBlank(secretKey, "SSH敏感字段加密密钥不能为空");

        byte[] keyBytes = secretKey.getBytes(CharsetUtil.CHARSET_UTF_8);
        int keyLength = keyBytes.length;
        Assert.isTrue(keyLength == 16 || keyLength == 24 || keyLength == 32,
                "SSH敏感字段加密密钥长度必须为16、24或32字节，当前长度={}", keyLength);

        return SecureUtil.aes(keyBytes);
    }

}
```

## 新增主机请求对象

新增主机时要求传入主机 ID、主机名称、地址、端口、用户名和认证方式。密码或私钥字段会根据认证方式校验。

文件位置：`src/main/java/io/github/atengk/ssh/dto/SshHostCreateRequest.java`

```java
package io.github.atengk.ssh.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * SSH 主机新增请求
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Data
public class SshHostCreateRequest {

    /**
     * 主机 ID
     */
    @NotBlank(message = "主机ID不能为空")
    private String hostId;

    /**
     * 主机名称
     */
    @NotBlank(message = "主机名称不能为空")
    private String hostName;

    /**
     * 主机地址
     */
    @NotBlank(message = "主机地址不能为空")
    private String host;

    /**
     * SSH 端口
     */
    @Min(value = 1, message = "端口不能小于1")
    @Max(value = 65535, message = "端口不能大于65535")
    private Integer port = 22;

    /**
     * 登录用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 认证方式：PASSWORD、PRIVATE_KEY、PASSWORD_AND_PRIVATE_KEY
     */
    @NotBlank(message = "认证方式不能为空")
    private String authType;

    /**
     * 登录密码
     */
    private String password;

    /**
     * 私钥文件路径
     */
    private String privateKeyPath;

    /**
     * 私钥密码
     */
    private String privateKeyPassphrase;

    /**
     * 环境标识
     */
    private String environment;

    /**
     * 标签
     */
    private String tags;

    /**
     * 备注
     */
    private String remark;

}
```

## 修改主机请求对象

修改时 `null` 表示不修改；敏感字段如果传空字符串，则表示清空该字段。

文件位置：`src/main/java/io/github/atengk/ssh/dto/SshHostUpdateRequest.java`

```java
package io.github.atengk.ssh.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * SSH 主机修改请求
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Data
public class SshHostUpdateRequest {

    /**
     * 主机名称
     */
    private String hostName;

    /**
     * 主机地址
     */
    private String host;

    /**
     * SSH 端口
     */
    @Min(value = 1, message = "端口不能小于1")
    @Max(value = 65535, message = "端口不能大于65535")
    private Integer port;

    /**
     * 登录用户名
     */
    private String username;

    /**
     * 认证方式：PASSWORD、PRIVATE_KEY、PASSWORD_AND_PRIVATE_KEY
     */
    private String authType;

    /**
     * 登录密码，null表示不修改，空字符串表示清空
     */
    private String password;

    /**
     * 私钥文件路径，null表示不修改，空字符串表示清空
     */
    private String privateKeyPath;

    /**
     * 私钥密码，null表示不修改，空字符串表示清空
     */
    private String privateKeyPassphrase;

    /**
     * 环境标识
     */
    private String environment;

    /**
     * 标签
     */
    private String tags;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 备注
     */
    private String remark;

}
```

## 测试连接结果对象

测试连接不执行具体业务命令，只验证数据库中的主机配置能否成功建立 SSH 会话。

文件位置：`src/main/java/io/github/atengk/ssh/dto/SshHostTestResult.java`

```java
package io.github.atengk.ssh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSH 主机连接测试结果
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SshHostTestResult {

    /**
     * 主机 ID
     */
    private String hostId;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 耗时，单位：毫秒
     */
    private Long durationMillis;

    /**
     * 结果消息
     */
    private String message;

}
```

## VO 增加启用状态

主机查询接口不要返回 `password`、`privateKeyPassphrase`。可以返回是否配置了密码、是否配置了私钥，方便前端展示。

文件位置：`src/main/java/io/github/atengk/ssh/dto/SshHostVO.java`

```java
package io.github.atengk.ssh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSH 主机视图对象
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SshHostVO {

    /**
     * 主机 ID
     */
    private String hostId;

    /**
     * 主机名称
     */
    private String hostName;

    /**
     * 主机地址
     */
    private String host;

    /**
     * SSH 端口
     */
    private Integer port;

    /**
     * 登录用户名
     */
    private String username;

    /**
     * 认证方式
     */
    private String authType;

    /**
     * 是否已配置密码
     */
    private Boolean passwordConfigured;

    /**
     * 是否已配置私钥
     */
    private Boolean privateKeyConfigured;

    /**
     * 环境标识
     */
    private String environment;

    /**
     * 标签
     */
    private String tags;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 备注
     */
    private String remark;

}
```

## Service 接口扩展

这里在上一节的查询能力上增加 CRUD、启用禁用和测试连接。

文件位置：`src/main/java/io/github/atengk/ssh/service/SshHostService.java`

```java
package io.github.atengk.ssh.service;

import io.github.atengk.ssh.config.SshProperties;
import io.github.atengk.ssh.dto.SshHostCreateRequest;
import io.github.atengk.ssh.dto.SshHostTestResult;
import io.github.atengk.ssh.dto.SshHostUpdateRequest;
import io.github.atengk.ssh.dto.SshHostVO;
import io.github.atengk.ssh.entity.SshHost;

import java.util.List;

/**
 * SSH 主机服务
 *
 * @author Ateng
 * @since 2026-04-29
 */
public interface SshHostService {

    /**
     * 新增主机
     *
     * @param request 新增请求
     * @return 主机视图对象
     */
    SshHostVO createHost(SshHostCreateRequest request);

    /**
     * 修改主机
     *
     * @param hostId  主机 ID
     * @param request 修改请求
     * @return 主机视图对象
     */
    SshHostVO updateHost(String hostId, SshHostUpdateRequest request);

    /**
     * 禁用主机
     *
     * @param hostId 主机 ID
     */
    void disableHost(String hostId);

    /**
     * 启用主机
     *
     * @param hostId 主机 ID
     */
    void enableHost(String hostId);

    /**
     * 测试主机连接
     *
     * @param hostId 主机 ID
     * @return 测试结果
     */
    SshHostTestResult testConnection(String hostId);

    /**
     * 根据主机 ID 查询启用的主机
     *
     * @param hostId 主机 ID
     * @return SSH 主机实体
     */
    SshHost getEnabledByHostId(String hostId);

    /**
     * 根据主机 ID 查询主机
     *
     * @param hostId 主机 ID
     * @return SSH 主机实体
     */
    SshHost getByHostId(String hostId);

    /**
     * 根据主机 ID 查询并转换为连接配置
     *
     * @param hostId 主机 ID
     * @return SSH 主机连接配置
     */
    SshProperties.Host getHostConfigByHostId(String hostId);

    /**
     * 查询所有启用主机
     *
     * @return 主机列表
     */
    List<SshHost> listEnabledHosts();

    /**
     * 查询全部主机
     *
     * @return 主机视图列表
     */
    List<SshHostVO> listHosts();

}
```

## Service 实现

这里完成新增、修改、启用禁用、连接测试、敏感字段加密保存和解密读取。`getHostConfigByHostId` 返回给 SSH 底层连接使用，因此会把密码和私钥密码解密。

文件位置：`src/main/java/io/github/atengk/ssh/service/impl/SshHostServiceImpl.java`

```java
package io.github.atengk.ssh.service.impl;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.ssh.config.SshProperties;
import io.github.atengk.ssh.core.SshSecretCodec;
import io.github.atengk.ssh.core.SshSessionFactory;
import io.github.atengk.ssh.dto.SshHostCreateRequest;
import io.github.atengk.ssh.dto.SshHostTestResult;
import io.github.atengk.ssh.dto.SshHostUpdateRequest;
import io.github.atengk.ssh.dto.SshHostVO;
import io.github.atengk.ssh.entity.SshHost;
import io.github.atengk.ssh.enums.SshAuthType;
import io.github.atengk.ssh.mapper.SshHostMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.session.ClientSession;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SSH 主机服务实现
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Service
public class SshHostServiceImpl extends ServiceImpl<SshHostMapper, SshHost> implements io.github.atengk.ssh.service.SshHostService {

    private final SshSecretCodec sshSecretCodec;

    private final SshSessionFactory sshSessionFactory;

    public SshHostServiceImpl(SshSecretCodec sshSecretCodec,
                              @Lazy SshSessionFactory sshSessionFactory) {
        this.sshSecretCodec = sshSecretCodec;
        this.sshSessionFactory = sshSessionFactory;
    }

    /**
     * 新增主机
     *
     * @param request 新增请求
     * @return 主机视图对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SshHostVO createHost(SshHostCreateRequest request) {
        Assert.notNull(request, "新增主机请求不能为空");
        validateHostIdFormat(request.getHostId());
        validateAuthType(request.getAuthType());
        validateCreateAuthConfig(request);

        long count = lambdaQuery()
                .eq(SshHost::getHostId, request.getHostId())
                .count();
        Assert.isTrue(count == 0, "SSH主机ID已存在：{}", request.getHostId());

        SshHost sshHost = new SshHost();
        sshHost.setHostId(StrUtil.trim(request.getHostId()));
        sshHost.setHostName(StrUtil.trim(request.getHostName()));
        sshHost.setHost(StrUtil.trim(request.getHost()));
        sshHost.setPort(ObjectUtil.defaultIfNull(request.getPort(), 22));
        sshHost.setUsername(StrUtil.trim(request.getUsername()));
        sshHost.setAuthType(StrUtil.trim(request.getAuthType()).toUpperCase());
        sshHost.setPassword(sshSecretCodec.encode(request.getPassword()));
        sshHost.setPrivateKeyPath(StrUtil.trimToNull(request.getPrivateKeyPath()));
        sshHost.setPrivateKeyPassphrase(sshSecretCodec.encode(request.getPrivateKeyPassphrase()));
        sshHost.setEnvironment(StrUtil.trimToNull(request.getEnvironment()));
        sshHost.setTags(StrUtil.trimToNull(request.getTags()));
        sshHost.setEnabled(Boolean.TRUE);
        sshHost.setRemark(StrUtil.trimToNull(request.getRemark()));

        save(sshHost);
        log.info("SSH主机新增成功，hostId={}，host={}，port={}，username={}",
                sshHost.getHostId(), sshHost.getHost(), sshHost.getPort(), sshHost.getUsername());

        return toVO(sshHost);
    }

    /**
     * 修改主机
     *
     * @param hostId  主机 ID
     * @param request 修改请求
     * @return 主机视图对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SshHostVO updateHost(String hostId, SshHostUpdateRequest request) {
        Assert.notNull(request, "修改主机请求不能为空");

        SshHost sshHost = getByHostId(hostId);

        if (request.getHostName() != null) {
            sshHost.setHostName(StrUtil.trim(request.getHostName()));
        }
        if (request.getHost() != null) {
            sshHost.setHost(StrUtil.trim(request.getHost()));
        }
        if (request.getPort() != null) {
            sshHost.setPort(request.getPort());
        }
        if (request.getUsername() != null) {
            sshHost.setUsername(StrUtil.trim(request.getUsername()));
        }
        if (request.getAuthType() != null) {
            validateAuthType(request.getAuthType());
            sshHost.setAuthType(StrUtil.trim(request.getAuthType()).toUpperCase());
        }
        if (request.getPassword() != null) {
            sshHost.setPassword(sshSecretCodec.encode(StrUtil.emptyToNull(request.getPassword())));
        }
        if (request.getPrivateKeyPath() != null) {
            sshHost.setPrivateKeyPath(StrUtil.emptyToNull(StrUtil.trim(request.getPrivateKeyPath())));
        }
        if (request.getPrivateKeyPassphrase() != null) {
            sshHost.setPrivateKeyPassphrase(sshSecretCodec.encode(StrUtil.emptyToNull(request.getPrivateKeyPassphrase())));
        }
        if (request.getEnvironment() != null) {
            sshHost.setEnvironment(StrUtil.emptyToNull(StrUtil.trim(request.getEnvironment())));
        }
        if (request.getTags() != null) {
            sshHost.setTags(StrUtil.emptyToNull(StrUtil.trim(request.getTags())));
        }
        if (request.getEnabled() != null) {
            sshHost.setEnabled(request.getEnabled());
        }
        if (request.getRemark() != null) {
            sshHost.setRemark(StrUtil.emptyToNull(StrUtil.trim(request.getRemark())));
        }

        validateEntityAuthConfig(sshHost);

        updateById(sshHost);
        log.info("SSH主机修改成功，hostId={}，host={}，port={}，username={}",
                sshHost.getHostId(), sshHost.getHost(), sshHost.getPort(), sshHost.getUsername());

        return toVO(sshHost);
    }

    /**
     * 禁用主机
     *
     * @param hostId 主机 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableHost(String hostId) {
        SshHost sshHost = getByHostId(hostId);
        sshHost.setEnabled(Boolean.FALSE);
        updateById(sshHost);
        log.info("SSH主机已禁用，hostId={}", hostId);
    }

    /**
     * 启用主机
     *
     * @param hostId 主机 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableHost(String hostId) {
        SshHost sshHost = getByHostId(hostId);
        validateEntityAuthConfig(sshHost);
        sshHost.setEnabled(Boolean.TRUE);
        updateById(sshHost);
        log.info("SSH主机已启用，hostId={}", hostId);
    }

    /**
     * 测试主机连接
     *
     * @param hostId 主机 ID
     * @return 测试结果
     */
    @Override
    public SshHostTestResult testConnection(String hostId) {
        Assert.notBlank(hostId, "SSH主机ID不能为空");

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try (ClientSession ignored = sshSessionFactory.createSessionByHostId(hostId)) {
            stopWatch.stop();

            log.info("SSH主机连接测试成功，hostId={}，duration={}ms", hostId, stopWatch.getTotalTimeMillis());
            return SshHostTestResult.builder()
                    .hostId(hostId)
                    .success(Boolean.TRUE)
                    .durationMillis(stopWatch.getTotalTimeMillis())
                    .message("连接成功")
                    .build();
        } catch (Exception e) {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }

            log.warn("SSH主机连接测试失败，hostId={}，duration={}ms，reason={}",
                    hostId, stopWatch.getTotalTimeMillis(), e.getMessage());

            return SshHostTestResult.builder()
                    .hostId(hostId)
                    .success(Boolean.FALSE)
                    .durationMillis(stopWatch.getTotalTimeMillis())
                    .message("连接失败：" + e.getMessage())
                    .build();
        }
    }

    /**
     * 根据主机 ID 查询启用的主机
     *
     * @param hostId 主机 ID
     * @return SSH 主机实体
     */
    @Override
    public SshHost getEnabledByHostId(String hostId) {
        Assert.notBlank(hostId, "SSH主机ID不能为空");

        SshHost sshHost = lambdaQuery()
                .eq(SshHost::getHostId, hostId)
                .eq(SshHost::getEnabled, Boolean.TRUE)
                .one();

        Assert.notNull(sshHost, "SSH主机不存在或未启用：{}", hostId);
        return sshHost;
    }

    /**
     * 根据主机 ID 查询主机
     *
     * @param hostId 主机 ID
     * @return SSH 主机实体
     */
    @Override
    public SshHost getByHostId(String hostId) {
        Assert.notBlank(hostId, "SSH主机ID不能为空");

        SshHost sshHost = lambdaQuery()
                .eq(SshHost::getHostId, hostId)
                .one();

        Assert.notNull(sshHost, "SSH主机不存在：{}", hostId);
        return sshHost;
    }

    /**
     * 根据主机 ID 查询并转换为连接配置
     *
     * @param hostId 主机 ID
     * @return SSH 主机连接配置
     */
    @Override
    public SshProperties.Host getHostConfigByHostId(String hostId) {
        SshHost sshHost = getEnabledByHostId(hostId);
        validateEntityAuthConfig(sshHost);

        SshProperties.Host host = new SshProperties.Host();
        host.setHost(sshHost.getHost());
        host.setPort(sshHost.getPort());
        host.setUsername(sshHost.getUsername());
        host.setPassword(sshSecretCodec.decode(sshHost.getPassword()));
        host.setPrivateKeyPath(sshHost.getPrivateKeyPath());
        host.setPrivateKeyPassphrase(sshSecretCodec.decode(sshHost.getPrivateKeyPassphrase()));

        log.info("SSH主机配置读取成功，hostId={}，host={}，port={}，username={}",
                sshHost.getHostId(), sshHost.getHost(), sshHost.getPort(), sshHost.getUsername());

        return host;
    }

    /**
     * 查询所有启用主机
     *
     * @return 主机列表
     */
    @Override
    public List<SshHost> listEnabledHosts() {
        return lambdaQuery()
                .eq(SshHost::getEnabled, Boolean.TRUE)
                .orderByAsc(SshHost::getEnvironment)
                .orderByAsc(SshHost::getHostId)
                .list();
    }

    /**
     * 查询全部主机
     *
     * @return 主机视图列表
     */
    @Override
    public List<SshHostVO> listHosts() {
        return lambdaQuery()
                .orderByAsc(SshHost::getEnvironment)
                .orderByAsc(SshHost::getHostId)
                .list()
                .stream()
                .map(this::toVO)
                .toList();
    }

    /**
     * 转换为视图对象
     *
     * @param sshHost SSH 主机实体
     * @return 主机视图对象
     */
    private SshHostVO toVO(SshHost sshHost) {
        return SshHostVO.builder()
                .hostId(sshHost.getHostId())
                .hostName(sshHost.getHostName())
                .host(sshHost.getHost())
                .port(sshHost.getPort())
                .username(sshHost.getUsername())
                .authType(sshHost.getAuthType())
                .passwordConfigured(StrUtil.isNotBlank(sshHost.getPassword()))
                .privateKeyConfigured(StrUtil.isNotBlank(sshHost.getPrivateKeyPath()))
                .environment(sshHost.getEnvironment())
                .tags(sshHost.getTags())
                .enabled(sshHost.getEnabled())
                .remark(sshHost.getRemark())
                .build();
    }

    /**
     * 校验主机 ID 格式
     *
     * @param hostId 主机 ID
     */
    private void validateHostIdFormat(String hostId) {
        Assert.isTrue(StrUtil.isNotBlank(hostId), "SSH主机ID不能为空");
        Assert.isTrue(hostId.matches("^[a-zA-Z0-9][a-zA-Z0-9_-]{1,63}$"),
                "SSH主机ID格式不合法，仅允许字母、数字、下划线、中划线，长度2到64位：{}", hostId);
    }

    /**
     * 校验认证方式
     *
     * @param authType 认证方式
     */
    private void validateAuthType(String authType) {
        Assert.isTrue(StrUtil.isNotBlank(authType), "SSH认证方式不能为空");
        Assert.isTrue(EnumUtil.contains(SshAuthType.class, StrUtil.trim(authType).toUpperCase()),
                "SSH认证方式不合法：{}", authType);
    }

    /**
     * 校验新增请求认证配置
     *
     * @param request 新增请求
     */
    private void validateCreateAuthConfig(SshHostCreateRequest request) {
        String authType = StrUtil.trim(request.getAuthType()).toUpperCase();

        if (StrUtil.equals(authType, SshAuthType.PASSWORD.name())) {
            Assert.notBlank(request.getPassword(), "密码认证时 password 不能为空");
            return;
        }

        if (StrUtil.equals(authType, SshAuthType.PRIVATE_KEY.name())) {
            Assert.notBlank(request.getPrivateKeyPath(), "私钥认证时 privateKeyPath 不能为空");
            return;
        }

        if (StrUtil.equals(authType, SshAuthType.PASSWORD_AND_PRIVATE_KEY.name())) {
            Assert.notBlank(request.getPassword(), "密码和私钥认证时 password 不能为空");
            Assert.notBlank(request.getPrivateKeyPath(), "密码和私钥认证时 privateKeyPath 不能为空");
        }
    }

    /**
     * 校验实体认证配置
     *
     * @param sshHost SSH 主机实体
     */
    private void validateEntityAuthConfig(SshHost sshHost) {
        validateAuthType(sshHost.getAuthType());

        String authType = StrUtil.trim(sshHost.getAuthType()).toUpperCase();

        if (StrUtil.equals(authType, SshAuthType.PASSWORD.name())) {
            Assert.notBlank(sshHost.getPassword(), "密码认证时 password 不能为空，hostId={}", sshHost.getHostId());
            return;
        }

        if (StrUtil.equals(authType, SshAuthType.PRIVATE_KEY.name())) {
            Assert.notBlank(sshHost.getPrivateKeyPath(), "私钥认证时 privateKeyPath 不能为空，hostId={}", sshHost.getHostId());
            return;
        }

        if (StrUtil.equals(authType, SshAuthType.PASSWORD_AND_PRIVATE_KEY.name())) {
            Assert.notBlank(sshHost.getPassword(), "密码和私钥认证时 password 不能为空，hostId={}", sshHost.getHostId());
            Assert.notBlank(sshHost.getPrivateKeyPath(), "密码和私钥认证时 privateKeyPath 不能为空，hostId={}", sshHost.getHostId());
        }
    }

}
```

## Controller 接口

这里把主机管理接口补全。查询接口返回 VO，不泄露密码；新增和修改接口接收敏感字段，但保存前会加密。

文件位置：`src/main/java/io/github/atengk/ssh/controller/SshHostController.java`

```java
package io.github.atengk.ssh.controller;

import io.github.atengk.ssh.dto.SshHostCreateRequest;
import io.github.atengk.ssh.dto.SshHostTestResult;
import io.github.atengk.ssh.dto.SshHostUpdateRequest;
import io.github.atengk.ssh.dto.SshHostVO;
import io.github.atengk.ssh.service.SshHostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * SSH 主机控制器
 *
 * @author Ateng
 * @since 2026-04-29
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ssh/hosts")
public class SshHostController {

    private final SshHostService sshHostService;

    /**
     * 查询主机列表
     *
     * @return 主机列表
     */
    @GetMapping
    public List<SshHostVO> listHosts() {
        return sshHostService.listHosts();
    }

    /**
     * 新增主机
     *
     * @param request 新增请求
     * @return 主机视图对象
     */
    @PostMapping
    public SshHostVO createHost(@Valid @RequestBody SshHostCreateRequest request) {
        return sshHostService.createHost(request);
    }

    /**
     * 修改主机
     *
     * @param hostId  主机 ID
     * @param request 修改请求
     * @return 主机视图对象
     */
    @PutMapping("/{hostId}")
    public SshHostVO updateHost(@PathVariable String hostId,
                                @Valid @RequestBody SshHostUpdateRequest request) {
        return sshHostService.updateHost(hostId, request);
    }

    /**
     * 禁用主机
     *
     * @param hostId 主机 ID
     * @return 操作结果
     */
    @PatchMapping("/{hostId}/disable")
    public Map<String, Object> disableHost(@PathVariable String hostId) {
        sshHostService.disableHost(hostId);
        return Map.of(
                "hostId", hostId,
                "success", true,
                "message", "主机已禁用"
        );
    }

    /**
     * 启用主机
     *
     * @param hostId 主机 ID
     * @return 操作结果
     */
    @PatchMapping("/{hostId}/enable")
    public Map<String, Object> enableHost(@PathVariable String hostId) {
        sshHostService.enableHost(hostId);
        return Map.of(
                "hostId", hostId,
                "success", true,
                "message", "主机已启用"
        );
    }

    /**
     * 测试主机连接
     *
     * @param hostId 主机 ID
     * @return 测试结果
     */
    @PostMapping("/{hostId}/test")
    public SshHostTestResult testConnection(@PathVariable String hostId) {
        return sshHostService.testConnection(hostId);
    }

}
```

## 调用示例

新增密码认证主机。

```bash
curl -X POST 'http://localhost:8080/api/ssh/hosts' \
  -H 'Content-Type: application/json' \
  -d '{
    "hostId": "dev-02",
    "hostName": "开发服务器02",
    "host": "192.168.1.102",
    "port": 22,
    "username": "root",
    "authType": "PASSWORD",
    "password": "your_password",
    "environment": "dev",
    "tags": "java,springboot",
    "remark": "开发测试服务器"
  }'
```

新增私钥认证主机。

```bash
curl -X POST 'http://localhost:8080/api/ssh/hosts' \
  -H 'Content-Type: application/json' \
  -d '{
    "hostId": "test-02",
    "hostName": "测试服务器02",
    "host": "192.168.1.103",
    "port": 22,
    "username": "deploy",
    "authType": "PRIVATE_KEY",
    "privateKeyPath": "/home/app/.ssh/id_rsa",
    "privateKeyPassphrase": "",
    "environment": "test",
    "tags": "deploy,test",
    "remark": "测试部署服务器"
  }'
```

查询主机列表。

```bash
curl -X GET 'http://localhost:8080/api/ssh/hosts'
```

测试主机连接。

```bash
curl -X POST 'http://localhost:8080/api/ssh/hosts/dev-02/test'
```

修改主机信息。

```bash
curl -X PUT 'http://localhost:8080/api/ssh/hosts/dev-02' \
  -H 'Content-Type: application/json' \
  -d '{
    "hostName": "开发服务器02-已更新",
    "host": "192.168.1.102",
    "port": 22,
    "username": "root",
    "authType": "PASSWORD",
    "password": "new_password",
    "environment": "dev",
    "tags": "java,springboot,updated",
    "enabled": true,
    "remark": "已更新连接信息"
  }'
```

禁用主机。

```bash
curl -X PATCH 'http://localhost:8080/api/ssh/hosts/dev-02/disable'
```

启用主机。

```bash
curl -X PATCH 'http://localhost:8080/api/ssh/hosts/dev-02/enable'
```

## 数据库存储效果

新增主机后，`password` 和 `private_key_passphrase` 字段会以 `{AES}` 前缀保存。

```text
{AES}xxxxxxxxxxxxxxxxxxxxxxxx
```

接口返回时不会返回密文或明文，只返回：

```json
{
  "hostId": "dev-02",
  "hostName": "开发服务器02",
  "host": "192.168.1.102",
  "port": 22,
  "username": "root",
  "authType": "PASSWORD",
  "passwordConfigured": true,
  "privateKeyConfigured": false,
  "environment": "dev",
  "tags": "java,springboot",
  "enabled": true,
  "remark": "开发测试服务器"
}
```

## 注意事项

当前 CRUD 接口已经具备主机管理能力，但生产环境还需要继续补强以下几点：

```text
1. 主机管理接口必须加登录认证和角色权限控制
2. 测试连接接口应限制调用频率，避免被用于探测内网
3. 生产主机建议禁止密码认证，只允许私钥认证
4. AES secret-key 一旦变更，历史密文将无法解密，需做密钥轮换方案
5. privateKeyPath 指向的是应用服务器本地路径，不是用户电脑路径
6. 私钥文件必须限制权限，例如 chmod 600
7. 所有新增、修改、启用、禁用、测试连接都应写操作审计日志
```

这一节完成后，SSH 模块已经从静态配置升级为数据库动态管理，并具备主机维护接口。下一步适合继续做 **统一异常处理与接口响应包装**，避免现在 `Assert` 或底层异常直接返回给前端。