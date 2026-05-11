# Spring Boot 集成 SSHJ 开发

## 文档概述

本文档用于说明如何在 Spring Boot 3 项目中集成 SSHJ，实现远程服务器的 SSH 连接、命令执行、文件上传和文件下载能力。SSHJ 是一个 Java SSHv2 客户端库，可用于在 Java 服务端程序中访问 Linux 服务器、执行 Shell 命令、操作 SFTP 文件和完成自动化运维流程。

### 功能定位

本功能定位为 Spring Boot 后端服务中的远程服务器操作组件，主要封装 SSH 连接管理、认证处理、命令执行和 SFTP 文件传输能力。业务系统不直接操作 SSHJ 原生 API，而是通过统一的 Service 方法完成远程操作，降低调用方对 SSH 协议、连接生命周期、异常处理和资源关闭细节的感知。

在实际项目中，该模块通常作为基础设施能力存在，可被部署系统、运维平台、文件分发服务、服务器巡检任务、日志采集任务等模块复用。核心目标不是替代专业运维工具，而是在 Java 应用内部提供可控、可审计、可封装的远程服务器访问能力。

SSHJ 官方说明其定位是 Java 的 SSHv2 库，支持 SSH、SCP、SFTP 等能力；官方 README 也提示 0.37.0 及以下版本存在 Terrapin 相关风险，建议使用 0.38.0 或更高版本。当前 Maven Central 页面显示 `com.hierynomus:sshj` 可使用 `0.40.0`。([GitHub](https://github.com/hierynomus/sshj))

### 适用场景

该集成方案适用于需要由 Spring Boot 服务主动连接远程 Linux 服务器的场景。例如，在自动化部署系统中，可以通过 SSH 执行启动、停止、重启、查看进程等命令；在文件分发系统中，可以通过 SFTP 上传安装包、配置文件或脚本；在运维巡检系统中，可以远程执行磁盘、内存、端口、服务状态等检查命令。

常见适用场景包括：

| 场景         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 远程命令执行 | 执行 `systemctl status`、`docker ps`、`df -h`、`tail` 等命令 |
| 文件上传     | 上传 JAR 包、配置文件、Shell 脚本、证书文件等                |
| 文件下载     | 下载日志文件、备份文件、导出文件、服务器生成文件等           |
| 自动化部署   | 上传应用包后执行部署脚本或重启服务                           |
| 服务器巡检   | 定时采集 CPU、内存、磁盘、端口、进程等状态                   |
| 运维平台集成 | 为后台管理系统提供远程主机操作能力                           |

不建议将该模块用于高频、长时间、大规模并发的远程交互式任务。如果需要大规模主机编排、复杂任务调度、失败重试、灰度发布和审计闭环，应优先评估 Ansible、SaltStack、Jenkins、Kubernetes Operator 或专业运维平台。

## 环境准备

本节说明开发和运行 Spring Boot 3 + SSHJ 项目前需要准备的基础环境，包括 JDK、Spring Boot 版本、Maven 依赖和远程服务器的 SSH 服务配置。

### JDK 与 Spring Boot 版本

Spring Boot 3.x 要求使用 Java 17 或更高版本。以 Spring Boot 3.5.x 官方文档为例，Spring Boot 3.5.14 至少需要 Java 17，并支持 Maven 3.6.3 或更高版本。([Home](https://docs.spring.io/spring-boot/3.5/system-requirements.html))

建议开发环境如下：

| 组件        | 推荐版本                | 说明                                                      |
| ----------- | ----------------------- | --------------------------------------------------------- |
| JDK         | 17 或 21                | Spring Boot 3 最低要求 Java 17，生产环境建议使用 LTS 版本 |
| Spring Boot | 3.3.x / 3.4.x / 3.5.x   | 根据公司基础框架版本统一选择                              |
| Maven       | 3.8.x 或更高            | 满足 Spring Boot 3 构建要求                               |
| SSHJ        | 0.40.0                  | 示例使用 Maven Central 当前展示版本                       |
| 操作系统    | Linux / macOS / Windows | Windows 开发时注意私钥路径和文件权限差异                  |
| 远程服务器  | Linux                   | 需要开启 SSH 服务，默认端口通常为 22                      |

开发环境检查命令如下：

```bash
# 查看 JDK 版本，建议为 17 或 21
java -version

# 查看 Maven 版本，建议为 3.8.x 或更高
mvn -version

# 查看 Git 版本，便于拉取项目和管理配置
git --version
```

`java -version` 用于确认当前运行环境是否满足 Spring Boot 3 要求；`mvn -version` 用于确认 Maven 是否能正常构建项目；`git --version` 不是 SSHJ 必需项，但在实际开发环境中通常需要保留。

### SSHJ 依赖配置

在 Spring Boot 3 项目的 `pom.xml` 中添加 SSHJ 依赖。SSHJ 本身不是 Spring Boot Starter，需要手动声明依赖版本。Maven Central 当前展示 `com.hierynomus:sshj` 的版本为 `0.40.0`，可以作为新项目的依赖版本选择。([Maven Central](https://central.sonatype.com/artifact/com.hierynomus/sshj))

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Web：用于后续提供 REST 接口调用 SSH 功能 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Validation：用于后续校验 SSH 连接参数、命令参数、文件路径参数 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- SSHJ：Java SSHv2 客户端库，用于 SSH 命令执行和 SFTP 文件传输 -->
    <dependency>
        <groupId>com.hierynomus</groupId>
        <artifactId>sshj</artifactId>
        <version>0.40.0</version>
    </dependency>

    <!-- Hutool：用于字符串、文件、集合等常用工具处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- Lombok：减少配置类、DTO、VO 的样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot Test：用于后续编写连接、命令执行、文件传输测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目统一使用父级依赖管理，可以将 SSHJ 版本抽取到 `<properties>` 中，便于后续统一升级。

文件位置：`pom.xml`

```xml
<properties>
    <!-- Java 编译版本：Spring Boot 3 建议使用 Java 17 或更高 LTS 版本 -->
    <java.version>17</java.version>

    <!-- SSHJ 版本：建议使用 0.38.0 及以上版本 -->
    <sshj.version>0.40.0</sshj.version>

    <!-- Hutool 工具包版本 -->
    <hutool.version>5.8.36</hutool.version>
</properties>

<dependencies>
    <!-- SSHJ：远程 SSH 命令执行与 SFTP 文件传输 -->
    <dependency>
        <groupId>com.hierynomus</groupId>
        <artifactId>sshj</artifactId>
        <version>${sshj.version}</version>
    </dependency>

    <!-- Hutool：常用工具类封装 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>
</dependencies>
```

依赖添加完成后，执行以下命令确认项目可以正常解析依赖：

```bash
# 清理并编译项目，验证 Maven 依赖是否可以正常下载
mvn clean compile

# 查看 SSHJ 依赖树，确认版本没有被其他依赖覆盖
mvn dependency:tree | grep sshj
```

`mvn clean compile` 用于验证项目是否可以正常构建；`mvn dependency:tree` 用于检查最终引入的 SSHJ 版本，避免因为依赖传递或父工程管理导致版本不符合预期。

### 远程服务器准备

远程服务器需要开启 SSH 服务，并准备可用于测试的账号、认证方式和文件目录。开发阶段建议优先使用测试服务器，不要直接连接生产服务器验证命令执行和文件传输能力。

以 Ubuntu / Debian 系统为例，安装并启动 OpenSSH Server：

```bash
# 安装 OpenSSH 服务端
sudo apt update
sudo apt install -y openssh-server

# 启动 SSH 服务
sudo systemctl start ssh

# 设置 SSH 服务开机自启
sudo systemctl enable ssh

# 查看 SSH 服务状态
sudo systemctl status ssh
```

以 CentOS / Rocky Linux / AlmaLinux 系统为例，安装并启动 OpenSSH Server：

```bash
# 安装 OpenSSH 服务端
sudo yum install -y openssh-server

# 启动 SSH 服务
sudo systemctl start sshd

# 设置 SSH 服务开机自启
sudo systemctl enable sshd

# 查看 SSH 服务状态
sudo systemctl status sshd
```

如果服务器启用了防火墙，需要放行 SSH 端口。默认 SSH 端口为 `22`，如果服务器使用了自定义端口，应以实际配置为准。

```bash
# firewalld 放行 22 端口
sudo firewall-cmd --permanent --add-port=22/tcp
sudo firewall-cmd --reload

# 查看端口监听情况
ss -tnlp | grep :22
```

建议创建专门用于应用连接的低权限用户，不建议直接使用 `root` 账号进行开发测试。

```bash
# 创建测试用户
sudo useradd -m appuser

# 设置测试用户密码
sudo passwd appuser

# 创建应用目录
sudo mkdir -p /opt/app/sshj-demo

# 授权目录给测试用户
sudo chown -R appuser:appuser /opt/app/sshj-demo
```

如果使用私钥认证，需要在开发机器生成密钥，并将公钥写入远程服务器目标用户的 `authorized_keys`。

```bash
# 在开发机器生成 SSH 密钥，按提示确认保存路径
ssh-keygen -t ed25519 -C "sshj-demo"

# 将公钥复制到远程服务器
ssh-copy-id -i ~/.ssh/id_ed25519.pub appuser@192.168.1.100

# 手动测试私钥登录
ssh -i ~/.ssh/id_ed25519 appuser@192.168.1.100
```

远程服务器准备完成后，至少需要确认以下事项：

| 检查项   | 要求                                                   |
| -------- | ------------------------------------------------------ |
| SSH 服务 | 已安装并正常运行                                       |
| 网络连通 | Spring Boot 服务所在机器可以访问远程服务器 SSH 端口    |
| 登录账号 | 已创建专用用户，具备必要命令执行权限                   |
| 密码认证 | 如果使用密码认证，需要确认账号密码可登录               |
| 私钥认证 | 如果使用私钥认证，需要确认公钥已写入 `authorized_keys` |
| 文件目录 | 上传和下载目录已创建，并对连接用户开放读写权限         |
| 权限边界 | 不建议默认授予 root 权限，必要时使用受控 sudo 策略     |

开发阶段可以先使用原生命令验证连通性：

```bash
# 使用密码或系统默认认证方式验证连接
ssh appuser@192.168.1.100

# 使用指定私钥验证连接
ssh -i ~/.ssh/id_ed25519 appuser@192.168.1.100

# 验证远程命令执行
ssh appuser@192.168.1.100 "whoami && hostname && pwd"

# 验证远程目录写入权限
ssh appuser@192.168.1.100 "echo 'sshj test' > /opt/app/sshj-demo/test.txt && cat /opt/app/sshj-demo/test.txt"
```

这些命令用于在接入 Java 代码前排除服务器、账号、网络、防火墙和目录权限问题。如果原生命令无法登录或无法写入目录，Spring Boot 集成 SSHJ 后也会失败，应先修复服务器侧配置。



## 配置设计

本节用于定义 SSH 连接、认证方式和超时控制的配置方案。配置设计的目标是让连接参数从代码中解耦，后续可以通过 `application.yml`、环境变量、配置中心或数据库动态维护服务器连接信息。

### SSH 连接配置

SSH 连接配置用于描述远程服务器的基础连接信息，包括主机地址、端口、用户名、主机密钥校验策略等。开发阶段可以先在 `application.yml` 中配置单个默认服务器，后续如果需要支持多服务器，可以扩展为服务器列表或数据库配置。

文件位置：`src/main/resources/application.yml`

```yaml
sshj:
  # 默认服务器连接配置，适合单服务器或开发验证场景
  host: 192.168.1.100
  port: 22
  username: appuser

  # 是否开启严格主机密钥校验
  # 开发环境可以为 false；生产环境建议为 true，并维护 known_hosts
  strict-host-key-checking: false

  # 连接超时时间，单位：秒
  connect-timeout-seconds: 10

  # Socket 读取超时时间，单位：秒
  socket-timeout-seconds: 30

  # 命令执行超时时间，单位：秒
  command-timeout-seconds: 60
```

核心配置项说明如下：

| 配置项                     | 说明                     | 建议值                     |
| -------------------------- | ------------------------ | -------------------------- |
| `host`                     | 远程服务器 IP 或域名     | 测试服务器或目标服务器地址 |
| `port`                     | SSH 端口                 | 默认 `22`                  |
| `username`                 | SSH 登录用户             | 建议使用低权限专用用户     |
| `strict-host-key-checking` | 是否校验服务端主机密钥   | 生产建议 `true`            |
| `connect-timeout-seconds`  | 建立 SSH 连接的超时时间  | `5` 到 `15` 秒             |
| `socket-timeout-seconds`   | Socket 读写超时时间      | `30` 到 `60` 秒            |
| `command-timeout-seconds`  | 单条远程命令执行超时时间 | 根据业务命令调整           |

生产环境中不建议长期使用 `strict-host-key-checking: false`。关闭主机密钥校验会降低接入门槛，但也会弱化对中间人攻击的防护。正式环境应将目标服务器公钥加入运行机器的 `known_hosts` 文件，并开启严格校验。

### 认证方式配置

SSHJ 常用的认证方式包括密码认证和私钥认证。密码认证配置简单，适合本地开发和测试环境；私钥认证安全性更高，适合生产环境和自动化部署场景。

密码认证配置示例：

文件位置：`src/main/resources/application.yml`

```yaml
sshj:
  host: 192.168.1.100
  port: 22
  username: appuser

  # 认证方式：password 表示密码认证
  auth-type: password

  # 密码认证使用，生产环境不建议明文写入配置文件
  password: "your_password"

  strict-host-key-checking: false
  connect-timeout-seconds: 10
  socket-timeout-seconds: 30
  command-timeout-seconds: 60
```

私钥认证配置示例：

文件位置：`src/main/resources/application.yml`

```yaml
sshj:
  host: 192.168.1.100
  port: 22
  username: appuser

  # 认证方式：private_key 表示私钥认证
  auth-type: private_key

  # 私钥路径是 Spring Boot 服务运行机器上的本地路径
  private-key-path: /home/app/.ssh/id_ed25519

  # 如果私钥没有口令，可以不配置该项
  private-key-passphrase: ""

  strict-host-key-checking: true
  connect-timeout-seconds: 10
  socket-timeout-seconds: 30
  command-timeout-seconds: 60
```

认证方式建议如下：

| 认证方式   | 适用场景               | 说明                               |
| ---------- | ---------------------- | ---------------------------------- |
| 密码认证   | 本地开发、临时测试     | 配置简单，但不建议生产环境长期使用 |
| 私钥认证   | 生产环境、自动化部署   | 推荐方式，需要妥善管理私钥文件权限 |
| 带口令私钥 | 安全要求较高的生产环境 | 私钥泄露后仍需要口令才能使用       |

敏感信息不建议直接写死在配置文件中。生产环境可以通过环境变量、配置中心、KMS、Vault 或容器 Secret 注入。例如：

```yaml
sshj:
  host: ${SSHJ_HOST}
  port: ${SSHJ_PORT:22}
  username: ${SSHJ_USERNAME}
  auth-type: ${SSHJ_AUTH_TYPE:private_key}
  password: ${SSHJ_PASSWORD:}
  private-key-path: ${SSHJ_PRIVATE_KEY_PATH:/home/app/.ssh/id_ed25519}
  private-key-passphrase: ${SSHJ_PRIVATE_KEY_PASSPHRASE:}
```

### 连接超时与执行超时配置

SSH 操作涉及网络连接、认证、命令执行和文件传输，必须设置明确的超时时间，避免远程服务器无响应时导致业务线程长期阻塞。

推荐将超时拆分为三类：

| 超时类型     | 作用范围                 | 示例                       |
| ------------ | ------------------------ | -------------------------- |
| 连接超时     | 建立 TCP/SSH 连接阶段    | 服务器不可达、端口不通     |
| Socket 超时  | SSH 连接建立后的读写阶段 | 网络抖动、远端无响应       |
| 命令执行超时 | 单条命令执行阶段         | 命令卡住、脚本长时间不退出 |

推荐配置如下：

```yaml
sshj:
  # 建立 SSH 连接的最大等待时间
  connect-timeout-seconds: 10

  # SSH Socket 读写最大等待时间
  socket-timeout-seconds: 30

  # 远程命令执行最大等待时间
  command-timeout-seconds: 60
```

配置建议如下：

| 场景         | 连接超时 | Socket 超时 | 命令超时           |
| ------------ | -------- | ----------- | ------------------ |
| 普通命令执行 | 5-10 秒  | 30 秒       | 30-60 秒           |
| 文件上传下载 | 10 秒    | 60-300 秒   | 不直接使用命令超时 |
| 部署脚本执行 | 10 秒    | 60 秒       | 300-900 秒         |
| 巡检命令     | 5 秒     | 15-30 秒    | 10-30 秒           |

对于可能长时间执行的命令，不建议无限放大超时时间。更合理的方式是将长任务改造为远程后台脚本，由 Spring Boot 服务触发任务、查询状态和拉取日志，而不是一直阻塞等待命令返回。

## 核心实现

本节给出 SSHJ 的核心封装代码，包括 SSH 客户端初始化、密码认证、私钥认证、远程命令执行、文件上传和文件下载。这里先实现可复用的基础能力，后续章节可以在此基础上继续封装 Spring Boot 配置属性类、Service 和 REST 接口。

核心文件结构如下：

```text
src/main/java/io/github/atengk/sshj/core
├── SshAuthType.java
├── SshConnectConfig.java
├── SshCommandResult.java
├── SshOperationException.java
├── SshClientFactory.java
└── SshOperationTemplate.java
```

### SSH 客户端初始化

SSH 客户端初始化负责创建 `SSHClient`、设置超时时间、配置主机密钥校验策略，并完成认证前的连接动作。这里将连接参数封装为 `SshConnectConfig`，方便后续从配置文件、数据库或接口参数中构建连接对象。

文件位置：`src/main/java/io/github/atengk/sshj/core/SshAuthType.java`

```java
package io.github.atengk.sshj.core;

/**
 * SSH 认证方式枚举。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public enum SshAuthType {

    /**
     * 密码认证。
     */
    PASSWORD,

    /**
     * 私钥认证。
     */
    PRIVATE_KEY

}
```

文件位置：`src/main/java/io/github/atengk/sshj/core/SshConnectConfig.java`

```java
package io.github.atengk.sshj.core;

import lombok.Data;

import java.time.Duration;

/**
 * SSH 连接配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class SshConnectConfig {

    /**
     * 远程服务器地址。
     */
    private String host;

    /**
     * SSH 端口。
     */
    private Integer port = 22;

    /**
     * 登录用户名。
     */
    private String username;

    /**
     * 认证方式。
     */
    private SshAuthType authType = SshAuthType.PASSWORD;

    /**
     * 登录密码，密码认证时使用。
     */
    private String password;

    /**
     * 私钥路径，私钥认证时使用。
     */
    private String privateKeyPath;

    /**
     * 私钥口令，私钥未设置口令时可以为空。
     */
    private String privateKeyPassphrase;

    /**
     * 是否开启严格主机密钥校验。
     */
    private Boolean strictHostKeyChecking = false;

    /**
     * SSH 连接超时时间。
     */
    private Duration connectTimeout = Duration.ofSeconds(10);

    /**
     * SSH Socket 读写超时时间。
     */
    private Duration socketTimeout = Duration.ofSeconds(30);

    /**
     * 远程命令执行超时时间。
     */
    private Duration commandTimeout = Duration.ofSeconds(60);

}
```

文件位置：`src/main/java/io/github/atengk/sshj/core/SshOperationException.java`

```java
package io.github.atengk.sshj.core;

/**
 * SSH 操作异常。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public class SshOperationException extends RuntimeException {

    /**
     * 构建 SSH 操作异常。
     *
     * @param message 异常信息
     */
    public SshOperationException(String message) {
        super(message);
    }

    /**
     * 构建 SSH 操作异常。
     *
     * @param message 异常信息
     * @param cause 原始异常
     */
    public SshOperationException(String message, Throwable cause) {
        super(message, cause);
    }

}
```

下面的工厂类负责创建已连接、已认证的 `SSHClient`。调用方拿到客户端后需要自行关闭，因此后续统一通过模板类使用 `try-with-resources` 管理连接生命周期。

文件位置：`src/main/java/io/github/atengk/sshj/core/SshClientFactory.java`

```java
package io.github.atengk.sshj.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * SSH 客户端工厂。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
public class SshClientFactory {

    /**
     * 创建已连接并完成认证的 SSH 客户端。
     *
     * @param config SSH 连接配置
     * @return SSH 客户端
     */
    public SSHClient createConnectedClient(SshConnectConfig config) {
        validateBaseConfig(config);

        SSHClient sshClient = new SSHClient();
        try {
            configureTimeout(sshClient, config);
            configureHostKeyVerifier(sshClient, config);

            log.info("开始连接远程服务器，host={}，port={}，username={}",
                    config.getHost(), config.getPort(), config.getUsername());

            sshClient.connect(config.getHost(), config.getPort());
            authenticate(sshClient, config);

            log.info("远程服务器连接成功，host={}，username={}",
                    config.getHost(), config.getUsername());

            return sshClient;
        } catch (Exception e) {
            closeQuietly(sshClient);
            log.error("远程服务器连接失败，host={}，username={}，原因={}",
                    config.getHost(), config.getUsername(), e.getMessage(), e);
            throw new SshOperationException("远程服务器连接失败：" + config.getHost(), e);
        }
    }

    /**
     * 校验基础连接配置。
     *
     * @param config SSH 连接配置
     */
    private void validateBaseConfig(SshConnectConfig config) {
        Assert.notNull(config, "SSH 连接配置不能为空");
        Assert.notBlank(config.getHost(), "SSH 主机地址不能为空");
        Assert.notBlank(config.getUsername(), "SSH 用户名不能为空");
        Assert.notNull(config.getPort(), "SSH 端口不能为空");
        Assert.isTrue(config.getPort() > 0 && config.getPort() <= 65535,
                "SSH 端口范围非法：{}", config.getPort());
        Assert.notNull(config.getAuthType(), "SSH 认证方式不能为空");
    }

    /**
     * 配置 SSH 超时时间。
     *
     * @param sshClient SSH 客户端
     * @param config SSH 连接配置
     */
    private void configureTimeout(SSHClient sshClient, SshConnectConfig config) {
        int connectTimeout = Math.toIntExact(config.getConnectTimeout().toMillis());
        int socketTimeout = Math.toIntExact(config.getSocketTimeout().toMillis());

        sshClient.setConnectTimeout(connectTimeout);
        sshClient.setTimeout(socketTimeout);
    }

    /**
     * 配置主机密钥校验策略。
     *
     * @param sshClient SSH 客户端
     * @param config SSH 连接配置
     * @throws IOException known_hosts 加载失败时抛出
     */
    private void configureHostKeyVerifier(SSHClient sshClient, SshConnectConfig config) throws IOException {
        if (Boolean.TRUE.equals(config.getStrictHostKeyChecking())) {
            // 生产环境建议开启，默认加载当前用户 ~/.ssh/known_hosts
            sshClient.loadKnownHosts();
            log.info("已开启 SSH 严格主机密钥校验");
            return;
        }

        // 开发环境便捷配置，不建议在生产环境长期使用
        sshClient.addHostKeyVerifier(new PromiscuousVerifier());
        log.warn("当前未开启 SSH 严格主机密钥校验，仅建议开发或测试环境使用");
    }

    /**
     * 根据认证方式完成登录认证。
     *
     * @param sshClient SSH 客户端
     * @param config SSH 连接配置
     * @throws IOException 私钥加载或认证失败时抛出
     */
    private void authenticate(SSHClient sshClient, SshConnectConfig config) throws IOException {
        if (SshAuthType.PASSWORD.equals(config.getAuthType())) {
            authenticateByPassword(sshClient, config);
            return;
        }

        if (SshAuthType.PRIVATE_KEY.equals(config.getAuthType())) {
            authenticateByPrivateKey(sshClient, config);
            return;
        }

        throw new SshOperationException("不支持的 SSH 认证方式：" + config.getAuthType());
    }

    /**
     * 使用密码认证。
     *
     * @param sshClient SSH 客户端
     * @param config SSH 连接配置
     */
    private void authenticateByPassword(SSHClient sshClient, SshConnectConfig config) {
        Assert.notBlank(config.getPassword(), "SSH 密码不能为空");
        sshClient.authPassword(config.getUsername(), config.getPassword());
        log.info("SSH 密码认证成功，username={}", config.getUsername());
    }

    /**
     * 使用私钥认证。
     *
     * @param sshClient SSH 客户端
     * @param config SSH 连接配置
     * @throws IOException 私钥加载失败时抛出
     */
    private void authenticateByPrivateKey(SSHClient sshClient, SshConnectConfig config) throws IOException {
        Assert.notBlank(config.getPrivateKeyPath(), "SSH 私钥路径不能为空");
        Assert.isTrue(FileUtil.exist(config.getPrivateKeyPath()),
                "SSH 私钥文件不存在：{}", config.getPrivateKeyPath());

        KeyProvider keyProvider;
        if (StrUtil.isBlank(config.getPrivateKeyPassphrase())) {
            keyProvider = sshClient.loadKeys(config.getPrivateKeyPath());
        } else {
            keyProvider = sshClient.loadKeys(config.getPrivateKeyPath(), config.getPrivateKeyPassphrase());
        }

        sshClient.authPublickey(config.getUsername(), keyProvider);
        log.info("SSH 私钥认证成功，username={}，privateKeyPath={}",
                config.getUsername(), config.getPrivateKeyPath());
    }

    /**
     * 安静关闭 SSH 客户端。
     *
     * @param sshClient SSH 客户端
     */
    private void closeQuietly(SSHClient sshClient) {
        try {
            sshClient.close();
        } catch (Exception ignored) {
            log.warn("SSH 客户端关闭失败，已忽略");
        }
    }

}
```

### 密码认证连接

密码认证适合开发环境、测试环境或临时验证场景。调用方只需要构建 `SshConnectConfig`，设置认证方式为 `PASSWORD`，并传入用户名和密码即可。

密码认证调用示例：

```java
SshConnectConfig config = new SshConnectConfig();
config.setHost("192.168.1.100");
config.setPort(22);
config.setUsername("appuser");
config.setAuthType(SshAuthType.PASSWORD);
config.setPassword("your_password");
config.setStrictHostKeyChecking(false);
```

密码认证的核心逻辑已经封装在 `SshClientFactory#authenticateByPassword` 方法中：

```java
private void authenticateByPassword(SSHClient sshClient, SshConnectConfig config) {
    Assert.notBlank(config.getPassword(), "SSH 密码不能为空");
    sshClient.authPassword(config.getUsername(), config.getPassword());
    log.info("SSH 密码认证成功，username={}", config.getUsername());
}
```

使用密码认证时需要注意，日志中不能打印密码，接口返回值中不能透出密码，配置文件也不建议提交到 Git 仓库。生产环境如果必须使用密码认证，应通过环境变量、配置中心密文或 Secret 管理工具注入。

### 私钥认证连接

私钥认证适合生产环境和自动化运维场景。Spring Boot 服务所在机器需要保存私钥文件，远程服务器目标用户的 `~/.ssh/authorized_keys` 中需要提前写入对应公钥。

私钥认证调用示例：

```java
SshConnectConfig config = new SshConnectConfig();
config.setHost("192.168.1.100");
config.setPort(22);
config.setUsername("appuser");
config.setAuthType(SshAuthType.PRIVATE_KEY);
config.setPrivateKeyPath("/home/app/.ssh/id_ed25519");
config.setPrivateKeyPassphrase("");
config.setStrictHostKeyChecking(true);
```

私钥认证的核心逻辑已经封装在 `SshClientFactory#authenticateByPrivateKey` 方法中：

```java
private void authenticateByPrivateKey(SSHClient sshClient, SshConnectConfig config) throws IOException {
    Assert.notBlank(config.getPrivateKeyPath(), "SSH 私钥路径不能为空");
    Assert.isTrue(FileUtil.exist(config.getPrivateKeyPath()),
            "SSH 私钥文件不存在：{}", config.getPrivateKeyPath());

    KeyProvider keyProvider;
    if (StrUtil.isBlank(config.getPrivateKeyPassphrase())) {
        keyProvider = sshClient.loadKeys(config.getPrivateKeyPath());
    } else {
        keyProvider = sshClient.loadKeys(config.getPrivateKeyPath(), config.getPrivateKeyPassphrase());
    }

    sshClient.authPublickey(config.getUsername(), keyProvider);
    log.info("SSH 私钥认证成功，username={}，privateKeyPath={}",
            config.getUsername(), config.getPrivateKeyPath());
}
```

私钥文件权限建议设置为仅当前运行用户可读：

```bash
# 设置私钥文件权限，避免权限过宽导致 SSH 拒绝使用
chmod 600 /home/app/.ssh/id_ed25519

# 设置 .ssh 目录权限
chmod 700 /home/app/.ssh

# 确认私钥文件归属 Spring Boot 运行用户
chown app:app /home/app/.ssh/id_ed25519
```

`chmod 600` 表示仅文件所有者可读写；`chmod 700` 表示仅目录所有者可进入和操作；`chown` 用于确保 Spring Boot 进程用户可以读取私钥文件。

### 命令执行封装

命令执行封装用于屏蔽 SSHJ 原生 `Session` 和 `Command` 的使用细节。业务侧只需要传入连接配置和命令字符串，即可获得退出码、标准输出和错误输出。

文件位置：`src/main/java/io/github/atengk/sshj/core/SshCommandResult.java`

```java
package io.github.atengk.sshj.core;

/**
 * SSH 命令执行结果。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public record SshCommandResult(
        String command,
        int exitStatus,
        String stdout,
        String stderr
) {

    /**
     * 判断命令是否执行成功。
     *
     * @return true 表示退出码为 0
     */
    public boolean isSuccess() {
        return exitStatus == 0;
    }

}
```

下面的模板类封装命令执行、文件上传和文件下载。每次操作都会创建独立 SSH 连接，并在操作完成后自动关闭，适合低频管理操作和后台任务。如果业务需要高频操作，可以在后续章节扩展连接池或会话复用机制。

文件位置：`src/main/java/io/github/atengk/sshj/core/SshOperationTemplate.java`

```java
package io.github.atengk.sshj.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.SFTPClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * SSH 操作模板。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SshOperationTemplate {

    private final SshClientFactory sshClientFactory;

    /**
     * 执行远程命令。
     *
     * @param config SSH 连接配置
     * @param commandText 远程命令
     * @return 命令执行结果
     */
    public SshCommandResult executeCommand(SshConnectConfig config, String commandText) {
        Assert.notNull(config, "SSH 连接配置不能为空");
        Assert.notBlank(commandText, "远程命令不能为空");

        long timeoutSeconds = Math.max(1, config.getCommandTimeout().toSeconds());

        log.info("开始执行远程命令，host={}，username={}，command={}",
                config.getHost(), config.getUsername(), commandText);

        try (SSHClient sshClient = sshClientFactory.createConnectedClient(config);
             Session session = sshClient.startSession();
             Session.Command command = session.exec(commandText)) {

            command.join(timeoutSeconds, TimeUnit.SECONDS);

            Integer exitStatus = command.getExitStatus();
            if (exitStatus == null) {
                log.warn("远程命令执行超时，host={}，timeoutSeconds={}，command={}",
                        config.getHost(), timeoutSeconds, commandText);
                throw new SshOperationException("远程命令执行超时：" + commandText);
            }

            String stdout = IoUtil.read(command.getInputStream(), CharsetUtil.CHARSET_UTF_8);
            String stderr = IoUtil.read(command.getErrorStream(), CharsetUtil.CHARSET_UTF_8);

            log.info("远程命令执行完成，host={}，exitStatus={}，command={}",
                    config.getHost(), exitStatus, commandText);

            return new SshCommandResult(
                    commandText,
                    exitStatus,
                    StrUtil.nullToEmpty(stdout),
                    StrUtil.nullToEmpty(stderr)
            );
        } catch (SshOperationException e) {
            throw e;
        } catch (Exception e) {
            log.error("远程命令执行失败，host={}，command={}，原因={}",
                    config.getHost(), commandText, e.getMessage(), e);
            throw new SshOperationException("远程命令执行失败：" + commandText, e);
        }
    }

    /**
     * 上传本地文件到远程服务器。
     *
     * @param config SSH 连接配置
     * @param localFilePath 本地文件路径
     * @param remoteFilePath 远程文件路径
     */
    public void uploadFile(SshConnectConfig config, String localFilePath, String remoteFilePath) {
        Assert.notNull(config, "SSH 连接配置不能为空");
        Assert.notBlank(localFilePath, "本地文件路径不能为空");
        Assert.notBlank(remoteFilePath, "远程文件路径不能为空");
        Assert.isTrue(FileUtil.exist(localFilePath), "本地文件不存在：{}", localFilePath);
        Assert.isTrue(FileUtil.isFile(localFilePath), "本地路径不是文件：{}", localFilePath);

        log.info("开始上传文件到远程服务器，host={}，localFilePath={}，remoteFilePath={}",
                config.getHost(), localFilePath, remoteFilePath);

        try (SSHClient sshClient = sshClientFactory.createConnectedClient(config);
             SFTPClient sftpClient = sshClient.newSFTPClient()) {

            sftpClient.put(localFilePath, remoteFilePath);

            log.info("文件上传完成，host={}，remoteFilePath={}",
                    config.getHost(), remoteFilePath);
        } catch (Exception e) {
            log.error("文件上传失败，host={}，localFilePath={}，remoteFilePath={}，原因={}",
                    config.getHost(), localFilePath, remoteFilePath, e.getMessage(), e);
            throw new SshOperationException("文件上传失败：" + remoteFilePath, e);
        }
    }

    /**
     * 从远程服务器下载文件到本地。
     *
     * @param config SSH 连接配置
     * @param remoteFilePath 远程文件路径
     * @param localFilePath 本地文件路径
     */
    public void downloadFile(SshConnectConfig config, String remoteFilePath, String localFilePath) {
        Assert.notNull(config, "SSH 连接配置不能为空");
        Assert.notBlank(remoteFilePath, "远程文件路径不能为空");
        Assert.notBlank(localFilePath, "本地文件路径不能为空");

        String localParentPath = FileUtil.getParent(localFilePath, 1);
        if (StrUtil.isNotBlank(localParentPath)) {
            FileUtil.mkdir(localParentPath);
        }

        log.info("开始从远程服务器下载文件，host={}，remoteFilePath={}，localFilePath={}",
                config.getHost(), remoteFilePath, localFilePath);

        try (SSHClient sshClient = sshClientFactory.createConnectedClient(config);
             SFTPClient sftpClient = sshClient.newSFTPClient()) {

            sftpClient.get(remoteFilePath, localFilePath);

            log.info("文件下载完成，host={}，localFilePath={}",
                    config.getHost(), localFilePath);
        } catch (Exception e) {
            log.error("文件下载失败，host={}，remoteFilePath={}，localFilePath={}，原因={}",
                    config.getHost(), remoteFilePath, localFilePath, e.getMessage(), e);
            throw new SshOperationException("文件下载失败：" + remoteFilePath, e);
        }
    }

}
```

命令执行调用示例：

```java
SshCommandResult result = sshOperationTemplate.executeCommand(config, "whoami && hostname && pwd");

if (result.isSuccess()) {
    log.info("命令执行成功，输出={}", result.stdout());
} else {
    log.warn("命令执行失败，退出码={}，错误输出={}", result.exitStatus(), result.stderr());
}
```

命令执行封装需要注意以下几点：

| 注意项                     | 说明                                     |
| -------------------------- | ---------------------------------------- |
| 不要直接拼接用户输入       | 避免命令注入风险                         |
| 必须设置命令超时           | 避免远程命令长时间不退出                 |
| 标准输出和错误输出都要读取 | 便于定位执行失败原因                     |
| 根据退出码判断结果         | `exitStatus = 0` 通常表示成功            |
| 敏感命令需要审计           | 删除、重启、授权等命令应记录操作人和参数 |

如果命令参数来自接口请求，建议使用白名单命令或固定脚本名称，不建议让前端直接传入完整 Shell 命令。

### 文件上传与下载封装

文件上传和下载通过 SSHJ 的 `SFTPClient` 实现。上传时需要确认本地文件存在，下载时需要提前创建本地父目录。远程目录是否存在通常由调用方或远程部署脚本保证，也可以在后续封装中增加远程目录创建逻辑。

上传调用示例：

```java
sshOperationTemplate.uploadFile(
        config,
        "/Users/ateng/files/demo.jar",
        "/opt/app/sshj-demo/demo.jar"
);
```

下载调用示例：

```java
sshOperationTemplate.downloadFile(
        config,
        "/opt/app/sshj-demo/logs/app.log",
        "/Users/ateng/downloads/app.log"
);
```

上传前可以先通过远程命令创建目录：

```java
sshOperationTemplate.executeCommand(config, "mkdir -p /opt/app/sshj-demo");
sshOperationTemplate.uploadFile(config, "/Users/ateng/files/demo.jar", "/opt/app/sshj-demo/demo.jar");
```

下载前可以先验证远程文件是否存在：

```java
SshCommandResult result = sshOperationTemplate.executeCommand(
        config,
        "test -f /opt/app/sshj-demo/logs/app.log && echo exists || echo missing"
);

if (StrUtil.contains(result.stdout(), "exists")) {
    sshOperationTemplate.downloadFile(
            config,
            "/opt/app/sshj-demo/logs/app.log",
            "/Users/ateng/downloads/app.log"
    );
} else {
    log.warn("远程文件不存在，跳过下载");
}
```

文件传输封装需要注意以下几点：

| 注意项       | 说明                                                    |
| ------------ | ------------------------------------------------------- |
| 本地路径校验 | 上传前确认本地文件存在，下载前创建本地目录              |
| 远程路径校验 | 上传前确认远程目录存在或提前创建                        |
| 大文件传输   | 适当增大 Socket 超时时间                                |
| 权限控制     | 远程用户必须具备目标目录读写权限                        |
| 文件覆盖     | `put` 和 `get` 可能覆盖目标文件，业务侧需要提前确认策略 |
| 路径安全     | 接口传入路径时需要限制根目录，避免任意文件读写          |

当前实现采用“每次操作创建连接，用完即关闭”的方式，代码简单且资源边界清晰。对于低频运维操作、后台任务和管理接口，这种方式更容易维护。对于批量服务器巡检、大量文件传输或高频命令执行，可以在此基础上继续扩展批量执行、连接复用、异步任务、重试机制和操作审计。



## Spring Boot 集成

本节将前面封装好的 SSH 核心能力接入 Spring Boot，包括配置属性绑定、业务 Service 封装和 REST 接口暴露。这里的设计目标是让 Controller 不直接感知 SSHJ 原生 API，也不直接操作 `SSHClient`，而是通过 Service 层完成远程命令执行和文件传输。

本节示例基于前文已创建的核心类：

```text
src/main/java/io/github/atengk/sshj/core
├── SshAuthType.java
├── SshConnectConfig.java
├── SshCommandResult.java
├── SshOperationException.java
├── SshClientFactory.java
└── SshOperationTemplate.java
```

继续新增以下 Spring Boot 集成文件：

```text
src/main/java/io/github/atengk/sshj
├── common
│   └── ApiResult.java
├── config
│   └── SshjProperties.java
├── controller
│   └── SshController.java
├── dto
│   ├── SshCommandRequest.java
│   ├── SshFileDownloadRequest.java
│   └── SshFileUploadRequest.java
├── exception
│   └── GlobalExceptionHandler.java
├── service
│   ├── SshService.java
│   └── impl
│       └── SshServiceImpl.java
└── vo
    ├── SshCommandVO.java
    └── SshOperationVO.java
```

### 配置属性类设计

配置属性类用于将 `application.yml` 中的 `sshj` 配置绑定为 Java 对象，并转换成核心层需要的 `SshConnectConfig`。这样可以避免业务代码散落读取配置，也方便后续切换为数据库配置或配置中心配置。

文件位置：`src/main/resources/application.yml`

```yaml
sshj:
  # 远程服务器地址
  host: 192.168.1.100

  # SSH 端口
  port: 22

  # 登录用户名
  username: appuser

  # 认证方式：PASSWORD 或 PRIVATE_KEY
  auth-type: PASSWORD

  # 密码认证使用，生产环境建议通过环境变量或配置中心密文注入
  password: ${SSHJ_PASSWORD:your_password}

  # 私钥认证使用，auth-type 为 PRIVATE_KEY 时启用
  private-key-path: ${SSHJ_PRIVATE_KEY_PATH:}

  # 私钥口令，没有口令可以留空
  private-key-passphrase: ${SSHJ_PRIVATE_KEY_PASSPHRASE:}

  # 是否开启严格主机密钥校验，生产环境建议 true
  strict-host-key-checking: false

  # 建立 SSH 连接超时时间，单位：秒
  connect-timeout-seconds: 10

  # Socket 读写超时时间，单位：秒
  socket-timeout-seconds: 30

  # 远程命令执行超时时间，单位：秒
  command-timeout-seconds: 60
```

下面的配置属性类负责读取 `sshj` 配置，并根据认证方式校验必要参数。

文件位置：`src/main/java/io/github/atengk/sshj/config/SshjProperties.java`

```java
package io.github.atengk.sshj.config;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.sshj.core.SshAuthType;
import io.github.atengk.sshj.core.SshConnectConfig;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * SSHJ 配置属性。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@Validated
@Component
@ConfigurationProperties(prefix = "sshj")
public class SshjProperties {

    /**
     * 远程服务器地址。
     */
    @NotBlank(message = "SSH 主机地址不能为空")
    private String host;

    /**
     * SSH 端口。
     */
    @NotNull(message = "SSH 端口不能为空")
    @Min(value = 1, message = "SSH 端口不能小于 1")
    @Max(value = 65535, message = "SSH 端口不能大于 65535")
    private Integer port = 22;

    /**
     * 登录用户名。
     */
    @NotBlank(message = "SSH 用户名不能为空")
    private String username;

    /**
     * 认证方式。
     */
    @NotNull(message = "SSH 认证方式不能为空")
    private SshAuthType authType = SshAuthType.PASSWORD;

    /**
     * 登录密码，密码认证时使用。
     */
    private String password;

    /**
     * 私钥路径，私钥认证时使用。
     */
    private String privateKeyPath;

    /**
     * 私钥口令。
     */
    private String privateKeyPassphrase;

    /**
     * 是否开启严格主机密钥校验。
     */
    private Boolean strictHostKeyChecking = false;

    /**
     * 连接超时时间，单位：秒。
     */
    @NotNull(message = "SSH 连接超时时间不能为空")
    @Min(value = 1, message = "SSH 连接超时时间不能小于 1 秒")
    private Integer connectTimeoutSeconds = 10;

    /**
     * Socket 超时时间，单位：秒。
     */
    @NotNull(message = "SSH Socket 超时时间不能为空")
    @Min(value = 1, message = "SSH Socket 超时时间不能小于 1 秒")
    private Integer socketTimeoutSeconds = 30;

    /**
     * 命令执行超时时间，单位：秒。
     */
    @NotNull(message = "SSH 命令执行超时时间不能为空")
    @Min(value = 1, message = "SSH 命令执行超时时间不能小于 1 秒")
    private Integer commandTimeoutSeconds = 60;

    /**
     * 校验认证参数是否完整。
     *
     * @return true 表示认证配置合法
     */
    @AssertTrue(message = "SSH 认证配置不完整")
    public boolean isAuthConfigValid() {
        if (SshAuthType.PASSWORD.equals(authType)) {
            return StrUtil.isNotBlank(password);
        }
        if (SshAuthType.PRIVATE_KEY.equals(authType)) {
            return StrUtil.isNotBlank(privateKeyPath);
        }
        return false;
    }

    /**
     * 转换为 SSH 连接配置。
     *
     * @return SSH 连接配置
     */
    public SshConnectConfig toConnectConfig() {
        SshConnectConfig config = new SshConnectConfig();
        config.setHost(host);
        config.setPort(port);
        config.setUsername(username);
        config.setAuthType(authType);
        config.setPassword(password);
        config.setPrivateKeyPath(privateKeyPath);
        config.setPrivateKeyPassphrase(privateKeyPassphrase);
        config.setStrictHostKeyChecking(strictHostKeyChecking);
        config.setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds));
        config.setSocketTimeout(Duration.ofSeconds(socketTimeoutSeconds));
        config.setCommandTimeout(Duration.ofSeconds(commandTimeoutSeconds));
        return config;
    }

}
```

如果项目启动时报 `Failed to bind properties under 'sshj'`，通常说明配置项缺失、枚举值错误或类型不匹配。例如 `auth-type` 应配置为 `PASSWORD` 或 `PRIVATE_KEY`，不要写成任意字符串。

### SSH 服务类设计

Service 层用于承接业务调用，屏蔽底层 SSH 连接、命令执行和 SFTP 操作细节。Controller 只负责参数校验和 HTTP 输入输出，真正的 SSH 业务逻辑由 `SshService` 完成。

文件位置：`src/main/java/io/github/atengk/sshj/service/SshService.java`

```java
package io.github.atengk.sshj.service;

import io.github.atengk.sshj.vo.SshCommandVO;
import io.github.atengk.sshj.vo.SshOperationVO;

/**
 * SSH 服务接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public interface SshService {

    /**
     * 执行远程命令。
     *
     * @param command 远程命令
     * @return 命令执行结果
     */
    SshCommandVO executeCommand(String command);

    /**
     * 上传本地文件到远程服务器。
     *
     * @param localFilePath 本地文件路径
     * @param remoteFilePath 远程文件路径
     * @return 操作结果
     */
    SshOperationVO uploadFile(String localFilePath, String remoteFilePath);

    /**
     * 从远程服务器下载文件到本地。
     *
     * @param remoteFilePath 远程文件路径
     * @param localFilePath 本地文件路径
     * @return 操作结果
     */
    SshOperationVO downloadFile(String remoteFilePath, String localFilePath);

}
```

命令执行结果 VO 用于返回退出码、标准输出、错误输出和成功标识。

文件位置：`src/main/java/io/github/atengk/sshj/vo/SshCommandVO.java`

```java
package io.github.atengk.sshj.vo;

import io.github.atengk.sshj.core.SshCommandResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSH 命令执行结果 VO。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SshCommandVO {

    /**
     * 执行命令。
     */
    private String command;

    /**
     * 退出码，0 通常表示成功。
     */
    private Integer exitStatus;

    /**
     * 是否执行成功。
     */
    private Boolean success;

    /**
     * 标准输出。
     */
    private String stdout;

    /**
     * 错误输出。
     */
    private String stderr;

    /**
     * 从核心命令结果转换为 VO。
     *
     * @param result 核心命令执行结果
     * @return 命令结果 VO
     */
    public static SshCommandVO of(SshCommandResult result) {
        return new SshCommandVO(
                result.command(),
                result.exitStatus(),
                result.isSuccess(),
                result.stdout(),
                result.stderr()
        );
    }

}
```

普通操作结果 VO 用于返回上传和下载结果。

文件位置：`src/main/java/io/github/atengk/sshj/vo/SshOperationVO.java`

```java
package io.github.atengk.sshj.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSH 普通操作结果 VO。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SshOperationVO {

    /**
     * 是否成功。
     */
    private Boolean success;

    /**
     * 结果消息。
     */
    private String message;

    /**
     * 关联路径。
     */
    private String path;

    /**
     * 构建成功结果。
     *
     * @param message 结果消息
     * @param path 关联路径
     * @return 操作结果
     */
    public static SshOperationVO success(String message, String path) {
        return new SshOperationVO(true, message, path);
    }

}
```

Service 实现类负责读取默认 SSH 配置，并调用前文封装的 `SshOperationTemplate`。

文件位置：`src/main/java/io/github/atengk/sshj/service/impl/SshServiceImpl.java`

```java
package io.github.atengk.sshj.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.sshj.config.SshjProperties;
import io.github.atengk.sshj.core.SshCommandResult;
import io.github.atengk.sshj.core.SshConnectConfig;
import io.github.atengk.sshj.core.SshOperationTemplate;
import io.github.atengk.sshj.service.SshService;
import io.github.atengk.sshj.vo.SshCommandVO;
import io.github.atengk.sshj.vo.SshOperationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * SSH 服务实现类。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SshServiceImpl implements SshService {

    private final SshjProperties sshjProperties;

    private final SshOperationTemplate sshOperationTemplate;

    /**
     * 执行远程命令。
     *
     * @param command 远程命令
     * @return 命令执行结果
     */
    @Override
    public SshCommandVO executeCommand(String command) {
        Assert.notBlank(command, "远程命令不能为空");

        log.info("接收到远程命令执行请求，command={}", command);

        SshConnectConfig connectConfig = sshjProperties.toConnectConfig();
        SshCommandResult result = sshOperationTemplate.executeCommand(connectConfig, command);

        if (result.isSuccess()) {
            log.info("远程命令执行成功，command={}", command);
        } else {
            log.warn("远程命令执行失败，exitStatus={}，command={}",
                    result.exitStatus(), command);
        }

        return SshCommandVO.of(result);
    }

    /**
     * 上传本地文件到远程服务器。
     *
     * @param localFilePath 本地文件路径
     * @param remoteFilePath 远程文件路径
     * @return 操作结果
     */
    @Override
    public SshOperationVO uploadFile(String localFilePath, String remoteFilePath) {
        Assert.notBlank(localFilePath, "本地文件路径不能为空");
        Assert.notBlank(remoteFilePath, "远程文件路径不能为空");

        log.info("接收到文件上传请求，localFilePath={}，remoteFilePath={}",
                localFilePath, remoteFilePath);

        SshConnectConfig connectConfig = sshjProperties.toConnectConfig();
        sshOperationTemplate.uploadFile(connectConfig, localFilePath, remoteFilePath);

        return SshOperationVO.success(
                StrUtil.format("文件上传成功：{}", remoteFilePath),
                remoteFilePath
        );
    }

    /**
     * 从远程服务器下载文件到本地。
     *
     * @param remoteFilePath 远程文件路径
     * @param localFilePath 本地文件路径
     * @return 操作结果
     */
    @Override
    public SshOperationVO downloadFile(String remoteFilePath, String localFilePath) {
        Assert.notBlank(remoteFilePath, "远程文件路径不能为空");
        Assert.notBlank(localFilePath, "本地文件路径不能为空");

        log.info("接收到文件下载请求，remoteFilePath={}，localFilePath={}",
                remoteFilePath, localFilePath);

        SshConnectConfig connectConfig = sshjProperties.toConnectConfig();
        sshOperationTemplate.downloadFile(connectConfig, remoteFilePath, localFilePath);

        return SshOperationVO.success(
                StrUtil.format("文件下载成功：{}", localFilePath),
                localFilePath
        );
    }

}
```

当前 Service 设计使用配置文件中的默认服务器。若需要支持多台服务器，可以将 `SshConnectConfig` 改为从数据库读取，或者在请求参数中传入服务器 ID，再由 Service 根据服务器 ID 查询连接配置。

### REST 接口设计

REST 接口用于对外暴露远程命令执行、文件上传和文件下载能力。这里需要注意：示例中的“上传文件”是指从 Spring Boot 服务所在机器上传本地文件到远程服务器，不是浏览器通过 `multipart/form-data` 上传文件。

统一响应对象如下。

文件位置：`src/main/java/io/github/atengk/sshj/common/ApiResult.java`

```java
package io.github.atengk.sshj.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口统一响应对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /**
     * 状态码，0 表示成功。
     */
    private Integer code;

    /**
     * 响应消息。
     */
    private String message;

    /**
     * 响应数据。
     */
    private T data;

    /**
     * 成功响应。
     *
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 统一响应对象
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(0, "操作成功", data);
    }

    /**
     * 失败响应。
     *
     * @param code 状态码
     * @param message 响应消息
     * @param <T> 数据类型
     * @return 统一响应对象
     */
    public static <T> ApiResult<T> fail(Integer code, String message) {
        return new ApiResult<>(code, message, null);
    }

}
```

命令执行请求 DTO。

文件位置：`src/main/java/io/github/atengk/sshj/dto/SshCommandRequest.java`

```java
package io.github.atengk.sshj.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * SSH 命令执行请求。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class SshCommandRequest {

    /**
     * 远程命令。
     */
    @NotBlank(message = "远程命令不能为空")
    private String command;

}
```

文件上传请求 DTO。

文件位置：`src/main/java/io/github/atengk/sshj/dto/SshFileUploadRequest.java`

```java
package io.github.atengk.sshj.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * SSH 文件上传请求。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class SshFileUploadRequest {

    /**
     * Spring Boot 服务所在机器的本地文件路径。
     */
    @NotBlank(message = "本地文件路径不能为空")
    private String localFilePath;

    /**
     * 远程服务器目标文件路径。
     */
    @NotBlank(message = "远程文件路径不能为空")
    private String remoteFilePath;

}
```

文件下载请求 DTO。

文件位置：`src/main/java/io/github/atengk/sshj/dto/SshFileDownloadRequest.java`

```java
package io.github.atengk.sshj.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * SSH 文件下载请求。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class SshFileDownloadRequest {

    /**
     * 远程服务器文件路径。
     */
    @NotBlank(message = "远程文件路径不能为空")
    private String remoteFilePath;

    /**
     * Spring Boot 服务所在机器的本地保存路径。
     */
    @NotBlank(message = "本地文件路径不能为空")
    private String localFilePath;

}
```

Controller 对外提供三个接口：执行命令、上传文件、下载文件。

文件位置：`src/main/java/io/github/atengk/sshj/controller/SshController.java`

```java
package io.github.atengk.sshj.controller;

import io.github.atengk.sshj.common.ApiResult;
import io.github.atengk.sshj.dto.SshCommandRequest;
import io.github.atengk.sshj.dto.SshFileDownloadRequest;
import io.github.atengk.sshj.dto.SshFileUploadRequest;
import io.github.atengk.sshj.service.SshService;
import io.github.atengk.sshj.vo.SshCommandVO;
import io.github.atengk.sshj.vo.SshOperationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * SSH 操作接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@RestController
@RequestMapping("/api/ssh")
@RequiredArgsConstructor
public class SshController {

    private final SshService sshService;

    /**
     * 执行远程命令。
     *
     * @param request 命令执行请求
     * @return 命令执行结果
     */
    @PostMapping("/commands")
    public ApiResult<SshCommandVO> executeCommand(@Valid @RequestBody SshCommandRequest request) {
        return ApiResult.success(sshService.executeCommand(request.getCommand()));
    }

    /**
     * 上传文件到远程服务器。
     *
     * @param request 文件上传请求
     * @return 操作结果
     */
    @PostMapping("/files/upload")
    public ApiResult<SshOperationVO> uploadFile(@Valid @RequestBody SshFileUploadRequest request) {
        return ApiResult.success(sshService.uploadFile(
                request.getLocalFilePath(),
                request.getRemoteFilePath()
        ));
    }

    /**
     * 从远程服务器下载文件。
     *
     * @param request 文件下载请求
     * @return 操作结果
     */
    @PostMapping("/files/download")
    public ApiResult<SshOperationVO> downloadFile(@Valid @RequestBody SshFileDownloadRequest request) {
        return ApiResult.success(sshService.downloadFile(
                request.getRemoteFilePath(),
                request.getLocalFilePath()
        ));
    }

}
```

接口清单如下：

| 接口                      | 方法   | 说明                       |
| ------------------------- | ------ | -------------------------- |
| `/api/ssh/commands`       | `POST` | 执行远程命令               |
| `/api/ssh/files/upload`   | `POST` | 从本地上传文件到远程服务器 |
| `/api/ssh/files/download` | `POST` | 从远程服务器下载文件到本地 |

## 使用示例

本节给出 REST 接口调用示例。调用前需要确保 Spring Boot 服务已经启动，并且 `application.yml` 中的 SSH 服务器配置可以正常连接。

### 执行远程命令

执行远程命令接口用于验证远程服务器连通性、查看基础环境信息或执行受控脚本。

请求示例：

```bash
curl -X POST "http://localhost:8080/api/ssh/commands" \
  -H "Content-Type: application/json" \
  -d '{
    "command": "whoami && hostname && pwd"
  }'
```

成功响应示例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "command": "whoami && hostname && pwd",
    "exitStatus": 0,
    "success": true,
    "stdout": "appuser\nserver-01\n/home/appuser\n",
    "stderr": ""
  }
}
```

失败响应示例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "command": "cat /not-exist-file",
    "exitStatus": 1,
    "success": false,
    "stdout": "",
    "stderr": "cat: /not-exist-file: No such file or directory\n"
  }
}
```

这里需要区分两类失败：接口调用失败和远程命令失败。接口调用失败通常会返回非成功状态码；远程命令失败但 SSH 调用流程正常时，接口仍然可以正常返回，只是 `success` 为 `false`，并通过 `exitStatus` 和 `stderr` 表示远程命令执行结果。

### 上传文件到远程服务器

上传文件接口用于将 Spring Boot 服务所在机器上的本地文件上传到远程服务器指定路径。调用前需要确认本地文件存在，远程目标目录已创建，并且 SSH 用户对远程目录有写入权限。

先在 Spring Boot 服务所在机器创建测试文件：

```bash
mkdir -p /tmp/sshj-demo
echo "hello sshj" > /tmp/sshj-demo/hello.txt
```

调用上传接口：

```bash
curl -X POST "http://localhost:8080/api/ssh/files/upload" \
  -H "Content-Type: application/json" \
  -d '{
    "localFilePath": "/tmp/sshj-demo/hello.txt",
    "remoteFilePath": "/opt/app/sshj-demo/hello.txt"
  }'
```

成功响应示例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "success": true,
    "message": "文件上传成功：/opt/app/sshj-demo/hello.txt",
    "path": "/opt/app/sshj-demo/hello.txt"
  }
}
```

上传后可以通过远程命令验证文件是否存在：

```bash
curl -X POST "http://localhost:8080/api/ssh/commands" \
  -H "Content-Type: application/json" \
  -d '{
    "command": "ls -l /opt/app/sshj-demo/hello.txt && cat /opt/app/sshj-demo/hello.txt"
  }'
```

### 下载远程服务器文件

下载文件接口用于将远程服务器文件下载到 Spring Boot 服务所在机器。调用前需要确认远程文件存在，Spring Boot 服务进程对本地目标目录有写入权限。

调用下载接口：

```bash
curl -X POST "http://localhost:8080/api/ssh/files/download" \
  -H "Content-Type: application/json" \
  -d '{
    "remoteFilePath": "/opt/app/sshj-demo/hello.txt",
    "localFilePath": "/tmp/sshj-demo/download/hello.txt"
  }'
```

成功响应示例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "success": true,
    "message": "文件下载成功：/tmp/sshj-demo/download/hello.txt",
    "path": "/tmp/sshj-demo/download/hello.txt"
  }
}
```

下载后在 Spring Boot 服务所在机器验证：

```bash
cat /tmp/sshj-demo/download/hello.txt
```

如果输出 `hello sshj`，说明远程文件下载成功。

## 异常处理

SSH 操作涉及网络、认证、权限、命令执行和文件系统，异常来源较多。建议统一在核心层抛出 `SshOperationException`，再通过全局异常处理器转换为接口响应。

### 连接异常处理

连接异常通常发生在 TCP 连接或 SSH 握手阶段，常见原因包括服务器地址错误、端口未开放、防火墙阻断、SSH 服务未启动、网络不通等。

常见表现如下：

| 原因                 | 表现                          | 处理方式                            |
| -------------------- | ----------------------------- | ----------------------------------- |
| IP 或域名错误        | 连接超时或无法解析            | 检查 `sshj.host`                    |
| 端口错误             | Connection refused 或 timeout | 检查 `sshj.port` 和防火墙           |
| SSH 服务未启动       | Connection refused            | 启动 `sshd` 或 `ssh` 服务           |
| 网络策略阻断         | 连接超时                      | 检查安全组、防火墙、网络 ACL        |
| known_hosts 校验失败 | 主机密钥校验异常              | 更新 `known_hosts` 或确认服务器指纹 |

连接异常应在日志中记录目标主机、端口、用户名和失败原因，但不能记录密码、私钥内容或私钥口令。

### 认证失败处理

认证失败通常发生在 SSH 连接成功之后，常见原因包括用户名错误、密码错误、私钥错误、私钥权限不正确、公钥未加入远程服务器等。

常见排查命令如下：

```bash
# 使用密码方式手动验证
ssh appuser@192.168.1.100

# 使用私钥方式手动验证
ssh -i /home/app/.ssh/id_ed25519 appuser@192.168.1.100

# 检查私钥文件权限
ls -l /home/app/.ssh/id_ed25519

# 检查远程服务器 authorized_keys
cat /home/appuser/.ssh/authorized_keys
```

私钥认证失败时，重点检查以下内容：

| 检查项          | 要求                                          |
| --------------- | --------------------------------------------- |
| 私钥路径        | Spring Boot 服务运行机器上必须存在            |
| 私钥权限        | 建议 `chmod 600`                              |
| `.ssh` 目录权限 | 建议 `chmod 700`                              |
| 公钥配置        | 远程用户 `authorized_keys` 中必须包含对应公钥 |
| 私钥口令        | 如果私钥设置了口令，配置中必须填写正确口令    |
| 登录用户        | 公钥需要配置在对应登录用户目录下              |

### 命令执行失败处理

命令执行失败不一定代表 SSH 调用失败。只要 SSH 连接、认证、会话创建和命令发送都正常，远程命令即使返回非 0 退出码，也可以作为正常响应返回给调用方。

例如执行不存在的文件：

```bash
curl -X POST "http://localhost:8080/api/ssh/commands" \
  -H "Content-Type: application/json" \
  -d '{
    "command": "cat /not-exist-file"
  }'
```

此时接口可能正常返回，但 `exitStatus` 为 `1`，`success` 为 `false`，`stderr` 中包含错误原因。

为了统一接口异常响应，可以增加全局异常处理器。

文件位置：`src/main/java/io/github/atengk/sshj/exception/GlobalExceptionHandler.java`

```java
package io.github.atengk.sshj.exception;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.sshj.common.ApiResult;
import io.github.atengk.sshj.core.SshOperationException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 SSH 操作异常。
     *
     * @param exception SSH 操作异常
     * @return 统一响应
     */
    @ExceptionHandler(SshOperationException.class)
    public ApiResult<Void> handleSshOperationException(SshOperationException exception) {
        log.error("SSH 操作失败，原因={}", exception.getMessage(), exception);
        return ApiResult.fail(5001, exception.getMessage());
    }

    /**
     * 处理请求体参数校验异常。
     *
     * @param exception 参数校验异常
     * @return 统一响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> StrUtil.format("{} {}", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining("；"));

        log.warn("请求参数校验失败，原因={}", message);
        return ApiResult.fail(4001, StrUtil.blankToDefault(message, "请求参数校验失败"));
    }

    /**
     * 处理普通参数校验异常。
     *
     * @param exception 参数校验异常
     * @return 统一响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResult<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .collect(Collectors.joining("；"));

        log.warn("请求参数校验失败，原因={}", message);
        return ApiResult.fail(4002, StrUtil.blankToDefault(message, "请求参数校验失败"));
    }

    /**
     * 处理 Spring 绑定异常。
     *
     * @param exception 绑定异常
     * @return 统一响应
     */
    @ExceptionHandler(BindException.class)
    public ApiResult<Void> handleBindException(BindException exception) {
        log.warn("配置或参数绑定失败，原因={}", exception.getMessage());
        return ApiResult.fail(4003, "配置或参数绑定失败");
    }

    /**
     * 处理非法参数异常。
     *
     * @param exception 非法参数异常
     * @return 统一响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResult<Void> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.warn("参数不合法，原因={}", exception.getMessage());
        return ApiResult.fail(4004, exception.getMessage());
    }

    /**
     * 处理未知异常。
     *
     * @param exception 未知异常
     * @return 统一响应
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception exception) {
        log.error("系统异常，原因={}", exception.getMessage(), exception);
        return ApiResult.fail(5000, "系统异常，请联系管理员");
    }

}
```

如果项目已经有统一异常处理器，不需要重复创建 `GlobalExceptionHandler`，只需要将 `SshOperationException` 的处理分支合并到现有异常处理体系中。

## 测试与验证

本节用于验证 SSH 配置、远程命令执行和文件传输是否正常。建议先用系统原生命令验证服务器连通性，再通过 JUnit 或 REST 接口验证 Spring Boot 集成结果。

### 本地功能验证

本地功能验证主要确认项目能正常启动、配置能正常绑定、核心 Bean 能正常注入。

建议先创建测试环境配置。

文件位置：`src/test/resources/application-test.yml`

```yaml
sshj:
  # 测试服务器地址
  host: 192.168.1.100

  # SSH 端口
  port: 22

  # 测试用户
  username: appuser

  # 测试阶段可以使用 PASSWORD，生产建议使用 PRIVATE_KEY
  auth-type: PASSWORD

  # 测试密码，建议通过环境变量注入
  password: ${SSHJ_PASSWORD:your_password}

  # 私钥认证时填写
  private-key-path: ${SSHJ_PRIVATE_KEY_PATH:}
  private-key-passphrase: ${SSHJ_PRIVATE_KEY_PASSPHRASE:}

  # 测试环境可以关闭，生产建议开启
  strict-host-key-checking: false

  # 超时配置
  connect-timeout-seconds: 10
  socket-timeout-seconds: 30
  command-timeout-seconds: 60
```

下面的测试类默认使用 `@Disabled` 禁用，避免没有真实服务器配置时自动执行失败。配置好测试服务器后，可以移除 `@Disabled` 再执行。

文件位置：`src/test/java/io/github/atengk/sshj/SshServiceIntegrationTest.java`

```java
package io.github.atengk.sshj;

import cn.hutool.core.io.FileUtil;
import io.github.atengk.sshj.service.SshService;
import io.github.atengk.sshj.vo.SshCommandVO;
import io.github.atengk.sshj.vo.SshOperationVO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * SSH 服务集成测试。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Disabled("需要配置真实 SSH 服务器后手动启用")
@SpringBootTest
@ActiveProfiles("test")
class SshServiceIntegrationTest {

    @Resource
    private SshService sshService;

    /**
     * 测试远程命令执行。
     */
    @Test
    void executeCommand() {
        SshCommandVO result = sshService.executeCommand("whoami && hostname && pwd");

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getSuccess(), "远程命令应执行成功");
        Assertions.assertNotNull(result.getStdout());
    }

    /**
     * 测试文件上传。
     */
    @Test
    void uploadFile() {
        String localFilePath = "/tmp/sshj-test/upload.txt";
        String remoteFilePath = "/opt/app/sshj-demo/upload.txt";

        FileUtil.writeUtf8String("hello sshj upload", localFilePath);

        SshOperationVO result = sshService.uploadFile(localFilePath, remoteFilePath);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getSuccess(), "文件应上传成功");
    }

    /**
     * 测试文件下载。
     */
    @Test
    void downloadFile() {
        String remoteFilePath = "/opt/app/sshj-demo/upload.txt";
        String localFilePath = "/tmp/sshj-test/download.txt";

        SshOperationVO result = sshService.downloadFile(remoteFilePath, localFilePath);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getSuccess(), "文件应下载成功");
        Assertions.assertTrue(FileUtil.exist(localFilePath), "本地下载文件应存在");
    }

}
```

执行测试前，需要确认远程目录存在：

```bash
ssh appuser@192.168.1.100 "mkdir -p /opt/app/sshj-demo"
```

执行指定测试：

```bash
mvn test -Dtest=SshServiceIntegrationTest
```

如果测试类保留了 `@Disabled`，JUnit 会跳过测试。需要真实执行时，移除 `@Disabled` 或注释该注解。

### 远程命令执行验证

远程命令执行验证建议从无副作用命令开始，避免一开始执行删除、重启、授权等高风险命令。

推荐验证顺序如下：

```bash
# 验证当前登录用户、主机名和工作目录
curl -X POST "http://localhost:8080/api/ssh/commands" \
  -H "Content-Type: application/json" \
  -d '{
    "command": "whoami && hostname && pwd"
  }'

# 验证磁盘信息
curl -X POST "http://localhost:8080/api/ssh/commands" \
  -H "Content-Type: application/json" \
  -d '{
    "command": "df -h"
  }'

# 验证 Java 环境
curl -X POST "http://localhost:8080/api/ssh/commands" \
  -H "Content-Type: application/json" \
  -d '{
    "command": "java -version"
  }'
```

需要注意，`java -version` 的输出通常会写入标准错误流 `stderr`，这不一定表示命令失败。判断命令是否成功应优先看 `exitStatus`。

如果需要执行部署脚本，建议固定脚本路径，不要直接拼接前端传入参数：

```bash
curl -X POST "http://localhost:8080/api/ssh/commands" \
  -H "Content-Type: application/json" \
  -d '{
    "command": "bash /opt/app/sshj-demo/deploy.sh"
  }'
```

生产环境中建议增加命令白名单，例如只允许执行 `/opt/app/scripts` 目录下的脚本，不允许接口调用方直接传入任意 Shell 命令。

### 文件传输验证

文件传输验证需要分别验证上传和下载。上传验证重点是本地文件是否存在、远程目录是否存在、远程目录是否有写入权限。下载验证重点是远程文件是否存在、本地目录是否可写。

先在 Spring Boot 服务所在机器准备本地测试文件：

```bash
mkdir -p /tmp/sshj-demo
echo "hello sshj file transfer" > /tmp/sshj-demo/source.txt
```

在远程服务器准备目标目录：

```bash
ssh appuser@192.168.1.100 "mkdir -p /opt/app/sshj-demo"
```

调用上传接口：

```bash
curl -X POST "http://localhost:8080/api/ssh/files/upload" \
  -H "Content-Type: application/json" \
  -d '{
    "localFilePath": "/tmp/sshj-demo/source.txt",
    "remoteFilePath": "/opt/app/sshj-demo/source.txt"
  }'
```

验证远程文件：

```bash
ssh appuser@192.168.1.100 "cat /opt/app/sshj-demo/source.txt"
```

调用下载接口：

```bash
curl -X POST "http://localhost:8080/api/ssh/files/download" \
  -H "Content-Type: application/json" \
  -d '{
    "remoteFilePath": "/opt/app/sshj-demo/source.txt",
    "localFilePath": "/tmp/sshj-demo/download/source.txt"
  }'
```

验证本地下载文件：

```bash
cat /tmp/sshj-demo/download/source.txt
```

如果上传失败，优先检查本地文件路径和远程目录权限。如果下载失败，优先检查远程文件是否存在、本地父目录是否可创建、Spring Boot 进程是否有本地目录写入权限。

最终建议至少完成以下验证闭环：

| 验证项           | 预期结果                                          |
| ---------------- | ------------------------------------------------- |
| Spring Boot 启动 | 配置绑定成功，无 Bean 创建异常                    |
| SSH 连接         | 可以连接远程服务器并完成认证                      |
| 命令执行         | `whoami && hostname && pwd` 返回 `exitStatus = 0` |
| 命令失败         | 执行错误命令时能返回 `stderr` 和非 0 退出码       |
| 文件上传         | 本地文件成功上传到远程服务器                      |
| 文件下载         | 远程文件成功下载到本地目录                        |
| 异常处理         | 连接失败、参数错误、文件不存在时返回统一错误结构  |
