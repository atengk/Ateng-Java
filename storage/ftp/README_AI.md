# Spring Integration FTP 开发

Spring Integration FTP 用于在 Spring Boot 3 项目中集成 FTP 文件传输能力，适合处理文件上传、文件下载、目录同步、定时拉取、远程文件管理等业务场景。通过 Spring Integration 提供的 FTP 适配器，可以将 FTP 操作抽象为标准的消息流处理，降低业务代码与底层 FTP 协议之间的耦合。

## 模块概述

本模块主要用于封装 FTP 文件传输相关能力，为业务系统提供统一的上传、下载、查询、删除和定时同步接口。模块内部负责 FTP 连接管理、远程目录处理、文件传输、异常处理和日志记录，业务层只需要关注文件来源、目标路径和处理结果。

### 功能定位

Spring Integration FTP 模块的定位是项目中的文件交换基础能力模块。它不直接承担业务解析逻辑，而是负责完成文件在本地系统与远程 FTP 服务之间的可靠传输。

在系统架构中，该模块通常作为独立的基础组件存在，对上提供业务接口，对下屏蔽 FTP 连接、登录认证、目录切换、文件上传、文件下载、远程删除等底层操作。

模块主要提供以下能力：

| 功能           | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| FTP 连接管理   | 统一维护 FTP 主机、端口、账号、密码、超时时间、被动模式等连接参数 |
| 文件上传       | 将本地文件上传到远程 FTP 指定目录                            |
| 文件下载       | 从远程 FTP 目录下载指定文件或批量下载文件                    |
| 目录同步       | 定时扫描远程目录，将符合规则的文件同步到本地                 |
| 文件过滤       | 按文件名、后缀、时间、是否已处理等条件过滤文件               |
| 远程文件管理   | 支持远程文件列表查询、远程文件删除、远程目录创建等操作       |
| 异常与日志处理 | 统一记录连接异常、传输异常、业务操作日志和失败原因           |

该模块建议只处理“文件传输”本身，不建议在 FTP 组件中直接编写复杂的业务解析逻辑。例如，对账文件下载完成后，可以交给对账服务解析；报表文件生成完成后，再调用 FTP 上传服务推送。

### 使用场景

Spring Integration FTP 适合系统之间通过 FTP 服务器交换文件的场景，尤其适合外部系统不提供 HTTP 接口、消息队列或对象存储接口，只能通过 FTP 目录交互文件的情况。

常见使用场景如下：

| 使用场景     | 场景说明                                                     |
| ------------ | ------------------------------------------------------------ |
| 对账文件拉取 | 定时从银行、支付渠道、供应商 FTP 目录拉取对账文件            |
| 报表文件推送 | 将系统生成的日报、月报、统计报表上传到客户或第三方 FTP 目录  |
| 批量数据交换 | 通过 CSV、TXT、XML、ZIP 等文件与外部系统交换数据             |
| 回执文件接收 | 上传业务文件后，定时拉取第三方系统生成的处理结果文件         |
| 文件归档     | 将本地业务文件定时上传到远程 FTP 目录进行集中归档            |
| 老系统集成   | 对接只能通过 FTP 交换数据的政企平台、金融系统、供应链系统或历史系统 |

在实际开发中，建议根据业务方向拆分不同的远程目录。例如上传目录、下载目录、备份目录、失败目录分别配置，避免所有文件混在同一个 FTP 目录下，降低后续排查和维护成本。

### 技术选型

本模块采用 Spring Boot 3 + Spring Integration FTP 实现。Spring Boot 负责应用启动、配置管理、依赖装配和接口暴露；Spring Integration FTP 负责 FTP 会话、远程文件操作和消息流处理。

推荐技术选型如下：

| 技术                        | 作用           | 说明                                                         |
| --------------------------- | -------------- | ------------------------------------------------------------ |
| Spring Boot 3.x             | 应用基础框架   | 提供配置管理、Bean 管理、Web 接口、任务调度、测试等基础能力  |
| Spring Integration FTP      | FTP 集成组件   | 提供 FTP SessionFactory、RemoteFileTemplate、Inbound Adapter、Outbound Adapter 等能力 |
| Spring Integration Java DSL | 集成流配置     | 使用 Java 配置方式声明文件上传、下载、同步等流程             |
| Apache Commons Net          | FTP 底层客户端 | Spring Integration FTP 底层依赖该组件处理 FTP 协议通信       |
| Hutool                      | 通用工具库     | 用于文件名处理、路径处理、字符串判断、集合判断等辅助逻辑     |
| Lombok                      | 简化代码       | 用于简化配置类、DTO、日志对象等样板代码                      |
| Spring Validation           | 参数校验       | 用于校验上传、下载、删除等接口请求参数                       |
| Spring Boot Actuator        | 运行监控       | 可选，用于健康检查、运行状态查看和服务监控                   |

技术选型建议如下：

| 需求                                   | 推荐方案                                             |
| -------------------------------------- | ---------------------------------------------------- |
| 只需要主动上传、下载文件               | 使用 `RemoteFileTemplate` 封装服务方法               |
| 需要定时扫描远程目录并拉取文件         | 使用 FTP Inbound Channel Adapter                     |
| 需要通过消息流上传本地文件             | 使用 FTP Outbound Channel Adapter                    |
| 需要执行远程文件列表、删除、移动等命令 | 使用 FTP Outbound Gateway                            |
| 项目希望代码简单、便于业务调用         | 优先封装 `FtpService`，内部使用 `RemoteFileTemplate` |

对于普通业务系统，建议优先使用 `DefaultFtpSessionFactory` + `FtpRemoteFileTemplate` 封装基础上传、下载、删除和列表查询能力；当需要定时监听远程目录时，再引入 `IntegrationFlow` 和 FTP Inbound Adapter。

## 环境准备

环境准备阶段需要确认 JDK、Spring Boot、Maven、FTP 服务和项目依赖是否满足开发要求。建议先在本地准备一个可控的 FTP 服务，用于验证连接、上传、下载、目录权限、被动模式和中文文件名等关键行为。

### JDK 与 Spring Boot 版本

Spring Boot 3 要求使用 Java 17 或更高版本，因此本模块建议使用 JDK 17 或 JDK 21。生产环境优先选择企业内部统一的 LTS 版本，避免开发环境和部署环境 JDK 版本不一致导致运行问题。

推荐版本如下：

| 组件        | 推荐版本                | 说明                                                  |
| ----------- | ----------------------- | ----------------------------------------------------- |
| JDK         | 17 或 21                | Spring Boot 3 最低要求 Java 17，建议使用 LTS 版本     |
| Spring Boot | 3.x                     | 根据项目统一版本选择，例如 3.2.x、3.3.x、3.4.x、3.5.x |
| Maven       | 3.8+                    | 建议使用较新的 Maven 版本，避免依赖解析问题           |
| IDE         | IntelliJ IDEA           | 建议启用 Lombok 插件和注解处理                        |
| 操作系统    | Linux / macOS / Windows | 开发环境均可，生产部署建议 Linux                      |

本地开发环境可以通过以下命令检查：

```bash
# 查看 JDK 版本
java -version

# 查看 Maven 版本
mvn -version

# 查看 Git 版本
git --version
```

执行后需要重点确认两点：第一，`java -version` 输出的主版本号需要大于或等于 17；第二，`mvn -version` 中显示的 Java Home 应该指向当前项目使用的 JDK 路径，避免 Maven 编译时使用了错误的 JDK。

### FTP 服务准备

开发阶段建议优先使用 Docker 启动本地 FTP 服务，便于快速验证上传、下载、目录创建和权限控制。生产环境通常由外部系统或运维团队提供 FTP 地址、账号、密码、远程目录和网络白名单。

本地可以使用以下命令启动测试 FTP 服务：

```bash
# 创建 FTP 本地数据目录
mkdir -p ~/docker-data/ftp/data

# 启动 FTP 服务
docker run -d \
  --name local-ftp \
  -p 21:21 \
  -p 21100-21110:21100-21110 \
  -e FTP_USER=ateng \
  -e FTP_PASS=123456 \
  -e PASV_ADDRESS=127.0.0.1 \
  -e PASV_MIN_PORT=21100 \
  -e PASV_MAX_PORT=21110 \
  -v ~/docker-data/ftp/data:/home/vsftpd \
  fauria/vsftpd

# 查看 FTP 容器状态
docker ps | grep local-ftp
```

这组命令会在本地启动一个 FTP 服务。`21` 是 FTP 控制端口，`21100-21110` 是被动模式数据端口范围，`FTP_USER` 和 `FTP_PASS` 是测试账号密码，`~/docker-data/ftp/data` 是宿主机文件挂载目录。

如果在服务器或虚拟机上启动 FTP 服务，需要将 `PASV_ADDRESS` 修改为客户端可以访问到的服务器 IP 或域名，否则可能出现可以登录但无法上传、下载、列目录的问题。

启动完成后，可以使用 FTP 客户端验证连接：

```bash
# 使用 lftp 连接 FTP 服务
lftp -u ateng,123456 127.0.0.1

# 登录后执行以下命令
pwd
ls
mkdir upload
cd upload
put ./test.txt
ls
bye
```

如果 `put ./test.txt` 执行失败，需要检查以下内容：

| 检查项               | 说明                                   |
| -------------------- | -------------------------------------- |
| 本地文件是否存在     | 确认当前目录下存在 `test.txt`          |
| FTP 账号是否有写权限 | 确认账号具备上传和创建目录权限         |
| 被动端口是否开放     | 确认 `21100-21110` 端口已映射并放行    |
| 目录挂载是否正确     | 确认宿主机目录与容器目录挂载正常       |
| 防火墙是否拦截       | Linux 服务器需要检查安全组和防火墙规则 |

接入生产 FTP 服务前，需要提前确认以下信息：

| 配置项       | 示例               | 说明                                     |
| ------------ | ------------------ | ---------------------------------------- |
| FTP 主机     | `ftp.example.com`  | FTP 服务 IP 或域名                       |
| FTP 端口     | `21`               | 默认 FTP 端口为 21，具体以服务方提供为准 |
| 用户名       | `ftp_user`         | FTP 登录账号                             |
| 密码         | `******`           | 建议通过环境变量或配置中心管理           |
| 远程上传目录 | `/upload`          | 本系统上传文件的目标目录                 |
| 远程下载目录 | `/download`        | 本系统拉取文件的来源目录                 |
| 备份目录     | `/backup`          | 下载或处理完成后的备份目录               |
| 失败目录     | `/error`           | 处理失败文件的存放目录                   |
| 被动模式     | `true`             | 跨网络访问 FTP 时通常建议开启            |
| 文件编码     | `UTF-8`            | 涉及中文文件名时必须确认                 |
| 目录权限     | 读、写、删、建目录 | 根据业务功能确认最小权限                 |
| 网络白名单   | 应用服务器出口 IP  | 生产环境通常需要提前开通                 |

### Maven 依赖配置

项目依赖建议使用 Spring Boot Parent 或 Spring Boot BOM 统一管理版本。`spring-integration-ftp` 建议不单独指定版本，交由 Spring Boot 依赖管理统一控制，避免 Spring Boot、Spring Integration 与底层依赖版本不一致。

文件位置：`pom.xml`

```xml
<properties>
    <!-- Spring Boot 3 要求 Java 17 或更高版本 -->
    <java.version>17</java.version>

    <!-- Hutool 版本可根据公司统一依赖版本调整 -->
    <hutool.version>5.8.36</hutool.version>
</properties>

<dependencies>
    <!-- Web 支持：用于提供上传、下载、查询、删除等 HTTP 接口 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Integration 核心能力：提供消息通道、消息端点、IntegrationFlow 等组件 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-integration</artifactId>
    </dependency>

    <!-- Spring Integration FTP：提供 FTP SessionFactory、Adapter、Gateway、RemoteFileTemplate 等能力 -->
    <dependency>
        <groupId>org.springframework.integration</groupId>
        <artifactId>spring-integration-ftp</artifactId>
    </dependency>

    <!-- 参数校验：用于校验接口请求参数 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Actuator：用于健康检查和运行状态监控，可按需保留 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Hutool：用于文件、路径、字符串、集合等通用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok：用于简化配置类、DTO、日志对象等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 单元测试与集成测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Spring Integration 测试支持：用于测试消息通道、IntegrationFlow 等组件 -->
    <dependency>
        <groupId>org.springframework.integration</groupId>
        <artifactId>spring-integration-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目没有继承 `spring-boot-starter-parent`，需要通过 `dependencyManagement` 导入 Spring Boot BOM。

文件位置：`pom.xml`

```xml
<dependencyManagement>
    <dependencies>
        <!-- 使用 Spring Boot BOM 统一管理 Spring Boot 与 Spring Integration 相关依赖版本 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

依赖配置完成后，可以执行以下命令验证项目是否可以正常编译：

```bash
# 清理并编译项目
mvn clean compile

# 查看是否已引入 Spring Integration FTP 依赖
mvn dependency:tree | grep spring-integration-ftp
```

如果 `dependency:tree` 输出中包含 `org.springframework.integration:spring-integration-ftp`，说明 FTP 集成依赖已经正确引入。后续即可继续编写 FTP 连接参数配置、`DefaultFtpSessionFactory`、`RemoteFileTemplate` 和 `IntegrationFlow` 等核心实现。



## 配置设计

配置设计用于统一管理 FTP 连接、本地文件目录、远程文件目录和任务调度参数。建议将所有 FTP 相关配置集中放在 `application.yml` 中，再通过 `@ConfigurationProperties` 映射为配置类，避免在业务代码中硬编码主机、端口、账号、目录路径和调度周期。

### FTP 连接参数

FTP 连接参数用于描述应用连接 FTP 服务时所需的基础信息，包括主机、端口、用户名、密码、连接超时时间、读取超时时间、编码和被动模式等。该配置是后续创建 `DefaultFtpSessionFactory` 的基础。

常用连接参数如下：

| 参数              | 类型     | 示例        | 说明                                                 |
| ----------------- | -------- | ----------- | ---------------------------------------------------- |
| `host`            | String   | `127.0.0.1` | FTP 服务地址，可以是 IP 或域名                       |
| `port`            | Integer  | `21`        | FTP 服务端口，默认 FTP 端口为 21                     |
| `username`        | String   | `ateng`     | FTP 登录用户名                                       |
| `password`        | String   | `123456`    | FTP 登录密码，生产环境建议通过环境变量或配置中心注入 |
| `client-mode`     | String   | `passive`   | FTP 传输模式，跨网络访问通常使用被动模式             |
| `encoding`        | String   | `UTF-8`     | 文件名编码，涉及中文文件名时需要重点确认             |
| `connect-timeout` | Duration | `10s`       | 建立连接的超时时间                                   |
| `read-timeout`    | Duration | `30s`       | 文件传输或读取响应的超时时间                         |
| `pool-enabled`    | Boolean  | `true`      | 是否启用 FTP Session 缓存                            |
| `pool-size`       | Integer  | `5`         | FTP Session 缓存数量                                 |

推荐配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  application:
    name: spring-integration-ftp-demo

ftp:
  # FTP 连接配置
  host: 127.0.0.1
  port: 21
  username: ateng
  password: 123456

  # FTP 文件名编码，涉及中文文件名时建议使用 UTF-8
  encoding: UTF-8

  # 传输模式：passive 表示被动模式，active 表示主动模式
  client-mode: passive

  # 连接超时时间
  connect-timeout: 10s

  # 读取和传输超时时间
  read-timeout: 30s

  # 是否启用 Session 缓存，频繁上传下载时建议开启
  pool-enabled: true

  # Session 缓存数量，根据并发传输量调整
  pool-size: 5
```

生产环境中，`password` 不建议直接写在配置文件中，可以使用环境变量注入。

```yaml
ftp:
  host: ${FTP_HOST}
  port: ${FTP_PORT:21}
  username: ${FTP_USERNAME}
  password: ${FTP_PASSWORD}
```

这种方式可以避免敏感信息进入代码仓库。部署时由启动脚本、容器环境变量、Kubernetes Secret 或配置中心提供真实值。

### 本地目录配置

本地目录配置用于定义应用服务器上的文件存放位置。FTP 文件传输通常涉及待上传目录、已下载目录、临时目录、备份目录和失败目录，建议按用途拆分，避免文件混放。

常用本地目录如下：

| 参数                 | 示例                 | 说明                           |
| -------------------- | -------------------- | ------------------------------ |
| `local.upload-dir`   | `/data/ftp/upload`   | 待上传文件目录                 |
| `local.download-dir` | `/data/ftp/download` | 远程文件下载后的本地存放目录   |
| `local.temp-dir`     | `/data/ftp/temp`     | 文件传输过程中的临时目录       |
| `local.backup-dir`   | `/data/ftp/backup`   | 上传或处理成功后的本地备份目录 |
| `local.error-dir`    | `/data/ftp/error`    | 处理失败文件的本地存放目录     |

推荐配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
ftp:
  local:
    # 本地待上传目录
    upload-dir: /data/ftp/upload

    # 本地下载目录
    download-dir: /data/ftp/download

    # 临时文件目录，用于下载中、上传中等中间状态文件
    temp-dir: /data/ftp/temp

    # 本地备份目录，成功处理后的文件可移动到该目录
    backup-dir: /data/ftp/backup

    # 本地失败目录，处理失败的文件可移动到该目录
    error-dir: /data/ftp/error
```

目录设计建议如下：

| 设计项   | 建议                                                   |
| -------- | ------------------------------------------------------ |
| 目录路径 | 生产环境使用绝对路径，不建议使用项目相对路径           |
| 目录权限 | 应用运行用户需要具备读、写、创建和移动文件权限         |
| 临时目录 | 下载大文件时建议先写入临时目录，完成后再移动到正式目录 |
| 文件命名 | 可在文件名中加入业务类型、日期、批次号，便于排查       |
| 磁盘空间 | 下载目录和备份目录需要纳入磁盘监控                     |

本地目录可以在应用启动时自动检查和创建，后续实现时可以使用 Hutool 的 `FileUtil.mkdir()` 处理目录初始化。

### 远程目录配置

远程目录配置用于定义 FTP 服务器上的业务目录。建议根据文件处理阶段拆分上传目录、下载目录、备份目录、失败目录和临时目录，避免不同业务状态的文件混在一起。

常用远程目录如下：

| 参数                  | 示例        | 说明                                             |
| --------------------- | ----------- | ------------------------------------------------ |
| `remote.upload-dir`   | `/upload`   | 本系统上传文件到 FTP 的目标目录                  |
| `remote.download-dir` | `/download` | 本系统从 FTP 拉取文件的来源目录                  |
| `remote.temp-dir`     | `/temp`     | 远程临时目录，可用于上传中间文件                 |
| `remote.backup-dir`   | `/backup`   | 远程备份目录，下载成功后可将远程文件移动到该目录 |
| `remote.error-dir`    | `/error`    | 远程失败目录，处理失败文件可移动到该目录         |

推荐配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
ftp:
  remote:
    # 远程上传目录，本系统生成的文件上传到该目录
    upload-dir: /upload

    # 远程下载目录，本系统从该目录拉取文件
    download-dir: /download

    # 远程临时目录，适用于先上传临时文件再重命名的场景
    temp-dir: /temp

    # 远程备份目录，下载或处理成功后可移动到该目录
    backup-dir: /backup

    # 远程失败目录，处理失败后可移动到该目录
    error-dir: /error
```

远程目录设计建议如下：

| 设计项   | 建议                                                         |
| -------- | ------------------------------------------------------------ |
| 上传目录 | 应用需要具备写入和创建目录权限                               |
| 下载目录 | 应用需要具备读取和列目录权限                                 |
| 删除功能 | 只有业务明确要求时才开启远程删除权限                         |
| 目录创建 | 上传前应检查远程目录是否存在，不存在时按需创建               |
| 临时文件 | 上传大文件时建议先上传到临时文件名，完成后再重命名为正式文件 |
| 文件隔离 | 多业务线共用 FTP 时，建议按业务编码拆分子目录                |

示例目录结构如下：

```text
/upload
  /order
  /report
  /settlement

/download
  /receipt
  /reconciliation

/backup
  /upload
  /download

/error
  /upload
  /download
```

这种目录结构可以按业务和文件状态进行隔离。后续排查问题时，可以快速判断文件当前处于待处理、已处理、备份或失败状态。

### 任务调度配置

任务调度配置用于控制定时上传、定时下载和定时同步的执行周期。对于简单场景，可以使用 Spring `@Scheduled`；对于文件流处理场景，可以使用 Spring Integration Poller。

常用调度参数如下：

| 参数                        | 示例             | 说明                 |
| --------------------------- | ---------------- | -------------------- |
| `schedule.download-enabled` | `true`           | 是否启用定时下载     |
| `schedule.upload-enabled`   | `false`          | 是否启用定时上传     |
| `schedule.sync-enabled`     | `true`           | 是否启用定时同步     |
| `schedule.download-cron`    | `0 */5 * * * ?`  | 定时下载 Cron 表达式 |
| `schedule.upload-cron`      | `0 */10 * * * ?` | 定时上传 Cron 表达式 |
| `schedule.sync-cron`        | `0 0/15 * * * ?` | 定时同步 Cron 表达式 |
| `schedule.max-fetch-size`   | `20`             | 单次最多拉取文件数量 |
| `schedule.retry-count`      | `3`              | 失败重试次数         |

推荐配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
ftp:
  schedule:
    # 是否启用定时下载远程文件
    download-enabled: true

    # 是否启用定时上传本地文件
    upload-enabled: false

    # 是否启用文件同步任务
    sync-enabled: true

    # 定时下载任务，每 5 分钟执行一次
    download-cron: "0 */5 * * * ?"

    # 定时上传任务，每 10 分钟执行一次
    upload-cron: "0 */10 * * * ?"

    # 定时同步任务，每 15 分钟执行一次
    sync-cron: "0 0/15 * * * ?"

    # 单次最多处理文件数量，避免一次拉取过多文件影响系统稳定性
    max-fetch-size: 20

    # 失败重试次数
    retry-count: 3
```

可以使用如下配置类承载上述参数。

文件位置：`src/main/java/io/github/atengk/ftp/config/FtpProperties.java`

```java
package io.github.atengk.ftp.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * FTP 配置属性
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Validated
@ConfigurationProperties(prefix = "ftp")
public class FtpProperties {

    /**
     * FTP 主机地址
     */
    @NotBlank(message = "FTP 主机地址不能为空")
    private String host;

    /**
     * FTP 端口
     */
    @Min(value = 1, message = "FTP 端口必须大于 0")
    private Integer port = 21;

    /**
     * FTP 用户名
     */
    @NotBlank(message = "FTP 用户名不能为空")
    private String username;

    /**
     * FTP 密码
     */
    @NotBlank(message = "FTP 密码不能为空")
    private String password;

    /**
     * 文件名编码
     */
    @NotBlank(message = "FTP 编码不能为空")
    private String encoding = "UTF-8";

    /**
     * 客户端模式：passive 或 active
     */
    @NotBlank(message = "FTP 客户端模式不能为空")
    private String clientMode = "passive";

    /**
     * 连接超时时间
     */
    @NotNull(message = "FTP 连接超时时间不能为空")
    private Duration connectTimeout = Duration.ofSeconds(10);

    /**
     * 读取超时时间
     */
    @NotNull(message = "FTP 读取超时时间不能为空")
    private Duration readTimeout = Duration.ofSeconds(30);

    /**
     * 是否启用 Session 缓存
     */
    private Boolean poolEnabled = true;

    /**
     * Session 缓存数量
     */
    @Min(value = 1, message = "FTP Session 缓存数量必须大于 0")
    private Integer poolSize = 5;

    /**
     * 本地目录配置
     */
    @Valid
    @NotNull(message = "FTP 本地目录配置不能为空")
    private Local local = new Local();

    /**
     * 远程目录配置
     */
    @Valid
    @NotNull(message = "FTP 远程目录配置不能为空")
    private Remote remote = new Remote();

    /**
     * 调度配置
     */
    @Valid
    @NotNull(message = "FTP 调度配置不能为空")
    private Schedule schedule = new Schedule();

    /**
     * 本地目录配置
     *
     * @author Ateng
     * @since 2026-05-07
     */
    @Data
    public static class Local {

        @NotBlank(message = "本地上传目录不能为空")
        private String uploadDir;

        @NotBlank(message = "本地下载目录不能为空")
        private String downloadDir;

        @NotBlank(message = "本地临时目录不能为空")
        private String tempDir;

        @NotBlank(message = "本地备份目录不能为空")
        private String backupDir;

        @NotBlank(message = "本地失败目录不能为空")
        private String errorDir;
    }

    /**
     * 远程目录配置
     *
     * @author Ateng
     * @since 2026-05-07
     */
    @Data
    public static class Remote {

        @NotBlank(message = "远程上传目录不能为空")
        private String uploadDir;

        @NotBlank(message = "远程下载目录不能为空")
        private String downloadDir;

        @NotBlank(message = "远程临时目录不能为空")
        private String tempDir;

        @NotBlank(message = "远程备份目录不能为空")
        private String backupDir;

        @NotBlank(message = "远程失败目录不能为空")
        private String errorDir;
    }

    /**
     * 任务调度配置
     *
     * @author Ateng
     * @since 2026-05-07
     */
    @Data
    public static class Schedule {

        private Boolean downloadEnabled = true;

        private Boolean uploadEnabled = false;

        private Boolean syncEnabled = true;

        @NotBlank(message = "定时下载 Cron 表达式不能为空")
        private String downloadCron;

        @NotBlank(message = "定时上传 Cron 表达式不能为空")
        private String uploadCron;

        @NotBlank(message = "定时同步 Cron 表达式不能为空")
        private String syncCron;

        @Min(value = 1, message = "单次处理文件数量必须大于 0")
        private Integer maxFetchSize = 20;

        @Min(value = 0, message = "失败重试次数不能小于 0")
        private Integer retryCount = 3;
    }
}
```

该配置类需要在 Spring Boot 启动类或配置类中启用。

```java
package io.github.atengk.ftp;

import io.github.atengk.ftp.config.FtpProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * FTP 示例应用启动类
 *
 * @author Ateng
 * @since 2026-05-07
 */
@EnableConfigurationProperties(FtpProperties.class)
@SpringBootApplication
public class FtpApplication {

    public static void main(String[] args) {
        SpringApplication.run(FtpApplication.class, args);
    }
}
```

## 核心流程设计

核心流程设计用于说明 FTP 文件上传、下载、同步和删除的处理步骤。建议在开发前先明确文件状态流转、目录规则、异常处理策略和幂等控制方式，避免后续出现重复下载、重复上传、文件覆盖、误删除等问题。

### 文件上传流程

文件上传流程用于将本地文件传输到远程 FTP 指定目录。上传前需要检查本地文件是否存在、远程目录是否存在、文件名是否符合规则；上传后需要记录结果，并根据业务要求进行本地备份或失败处理。

推荐上传流程如下：

| 步骤 | 处理动作       | 说明                                               |
| ---- | -------------- | -------------------------------------------------- |
| 1    | 接收上传请求   | 获取本地文件路径、远程目录、目标文件名等参数       |
| 2    | 校验本地文件   | 判断文件是否存在、是否为普通文件、大小是否符合限制 |
| 3    | 检查远程目录   | 判断远程目录是否存在，不存在则自动创建             |
| 4    | 生成上传文件名 | 根据业务类型、日期、批次号生成目标文件名           |
| 5    | 上传临时文件   | 先上传为 `.tmp` 临时文件，避免第三方读取未完成文件 |
| 6    | 重命名正式文件 | 上传完成后将临时文件重命名为正式文件名             |
| 7    | 记录上传结果   | 记录上传状态、远程路径、文件大小、耗时和异常信息   |
| 8    | 本地文件后处理 | 上传成功后可移动到备份目录，失败则移动到失败目录   |

流程示意如下：

```text
本地文件
  -> 参数校验
  -> 文件存在性校验
  -> 远程目录检查
  -> 上传临时文件
  -> 重命名正式文件
  -> 记录上传日志
  -> 本地文件备份或失败处理
```

上传流程设计要点如下：

| 设计点       | 建议                                                 |
| ------------ | ---------------------------------------------------- |
| 临时文件策略 | 上传时先写入 `xxx.tmp`，完成后再改名为 `xxx.csv`     |
| 文件覆盖策略 | 默认不覆盖远程同名文件，除非业务明确允许             |
| 目录创建策略 | 上传前自动创建远程目录，减少人工维护成本             |
| 异常处理     | 捕获连接异常、权限异常、文件不存在异常和传输异常     |
| 操作日志     | 记录本地路径、远程路径、文件大小、上传耗时和处理结果 |

上传成功后的文件状态可以设计为：

| 状态             | 说明     |
| ---------------- | -------- |
| `WAIT_UPLOAD`    | 待上传   |
| `UPLOADING`      | 上传中   |
| `UPLOAD_SUCCESS` | 上传成功 |
| `UPLOAD_FAILED`  | 上传失败 |

如果系统需要持久化上传记录，建议为每个文件生成唯一业务流水号，避免同一个文件被重复上传后难以追踪。

### 文件下载流程

文件下载流程用于从远程 FTP 目录拉取文件到本地目录。下载前需要获取远程文件列表并执行过滤，下载过程中建议使用临时文件，下载完成后再移动到正式目录，避免业务服务读取到未下载完成的文件。

推荐下载流程如下：

| 步骤 | 处理动作         | 说明                                               |
| ---- | ---------------- | -------------------------------------------------- |
| 1    | 获取远程文件列表 | 查询远程下载目录中的文件                           |
| 2    | 文件过滤         | 按文件名、后缀、时间、是否已处理等规则过滤         |
| 3    | 判断幂等记录     | 已下载或已处理的文件不重复处理                     |
| 4    | 下载到临时目录   | 先下载为本地临时文件                               |
| 5    | 校验下载结果     | 校验文件大小、是否为空、必要时校验摘要             |
| 6    | 移动到正式目录   | 下载完成后移动到本地下载目录                       |
| 7    | 远程文件后处理   | 可删除、移动到远程备份目录或保留                   |
| 8    | 记录下载结果     | 记录文件名、远程路径、本地路径、下载状态和异常信息 |

流程示意如下：

```text
远程下载目录
  -> 获取文件列表
  -> 文件过滤
  -> 幂等判断
  -> 下载到本地临时目录
  -> 下载结果校验
  -> 移动到本地正式目录
  -> 远程文件备份或删除
  -> 记录下载日志
```

下载流程设计要点如下：

| 设计点     | 建议                                                       |
| ---------- | ---------------------------------------------------------- |
| 文件过滤   | 只处理符合业务规则的文件，例如 `.csv`、`.txt`、`.zip`      |
| 幂等控制   | 使用远程路径、文件名、文件大小、修改时间组合判断是否已处理 |
| 临时文件   | 下载时使用 `.downloading` 后缀，完成后再改为正式文件名     |
| 空文件处理 | 根据业务规则决定空文件是否允许处理                         |
| 远程后处理 | 推荐下载成功后移动到远程备份目录，而不是直接删除           |
| 异常恢复   | 下载失败时保留临时文件或清理临时文件需要有明确规则         |

下载成功后的文件状态可以设计为：

| 状态               | 说明             |
| ------------------ | ---------------- |
| `WAIT_DOWNLOAD`    | 待下载           |
| `DOWNLOADING`      | 下载中           |
| `DOWNLOAD_SUCCESS` | 下载成功         |
| `DOWNLOAD_FAILED`  | 下载失败         |
| `PROCESSED`        | 文件业务处理完成 |

如果后续还有文件解析入库流程，建议将“下载成功”和“业务处理成功”拆成两个状态，不要混为一个状态。

### 文件同步流程

文件同步流程用于定时将远程目录中的文件同步到本地，或者将本地目录中的文件同步到远程。同步流程通常由定时任务触发，核心要求是可重复执行、可过滤、可追踪、可重试。

远程到本地的同步流程如下：

| 步骤 | 处理动作         | 说明                                         |
| ---- | ---------------- | -------------------------------------------- |
| 1    | 定时任务触发     | 根据 `download-cron` 或 `sync-cron` 周期执行 |
| 2    | 获取远程文件列表 | 查询远程目录下的待同步文件                   |
| 3    | 执行文件过滤     | 排除目录、临时文件、已处理文件和非法文件     |
| 4    | 限制处理数量     | 根据 `max-fetch-size` 控制单次拉取数量       |
| 5    | 执行下载         | 按文件逐个下载到本地临时目录                 |
| 6    | 更新同步记录     | 记录同步状态、耗时、失败原因                 |
| 7    | 文件后处理       | 远程文件移动到备份目录或保持原位置           |
| 8    | 触发业务处理     | 下载完成后可投递事件或调用业务服务解析文件   |

流程示意如下：

```text
定时任务
  -> 扫描远程目录
  -> 文件过滤
  -> 幂等判断
  -> 限制单次数量
  -> 批量下载
  -> 更新同步状态
  -> 远程文件后处理
  -> 触发后续业务处理
```

本地到远程的同步流程如下：

| 步骤 | 处理动作           | 说明                           |
| ---- | ------------------ | ------------------------------ |
| 1    | 定时任务触发       | 根据 `upload-cron` 周期执行    |
| 2    | 扫描本地上传目录   | 获取待上传文件列表             |
| 3    | 执行文件过滤       | 排除临时文件、空文件和非法文件 |
| 4    | 判断上传记录       | 已上传成功的文件不重复上传     |
| 5    | 上传到远程临时文件 | 先上传为临时文件               |
| 6    | 重命名为正式文件   | 上传完成后发布正式文件         |
| 7    | 本地文件归档       | 上传成功后移动到本地备份目录   |
| 8    | 记录同步结果       | 记录上传状态和异常信息         |

流程示意如下：

```text
定时任务
  -> 扫描本地上传目录
  -> 文件过滤
  -> 幂等判断
  -> 上传远程临时文件
  -> 重命名正式文件
  -> 本地文件归档
  -> 记录同步结果
```

同步流程设计要点如下：

| 设计点   | 建议                                                   |
| -------- | ------------------------------------------------------ |
| 并发控制 | 同一个同步任务不建议并发执行，避免重复处理同一批文件   |
| 单次数量 | 使用 `max-fetch-size` 限制单次处理文件数               |
| 幂等记录 | 记录文件名、路径、大小、修改时间和处理状态             |
| 失败重试 | 失败文件按配置重试，超过次数后进入失败状态             |
| 文件锁定 | 正在处理的文件需要标记为处理中，避免被其他任务再次处理 |
| 调度开关 | 上传、下载、同步任务需要可单独启停                     |

如果文件量较大，建议将“扫描文件”和“处理文件”拆开。扫描任务只负责生成待处理记录，处理任务根据记录逐个上传或下载，这样更容易实现限流、重试和失败恢复。

### 文件删除流程

文件删除流程用于删除 FTP 远程文件或本地文件。删除操作风险较高，建议只在业务明确要求时开放，并增加参数校验、权限控制、日志记录和必要的保护规则。

推荐远程文件删除流程如下：

| 步骤 | 处理动作         | 说明                                     |
| ---- | ---------------- | ---------------------------------------- |
| 1    | 接收删除请求     | 获取远程目录和文件名                     |
| 2    | 参数校验         | 校验目录、文件名、业务类型和操作人       |
| 3    | 路径安全检查     | 禁止删除根目录、上级目录和非法路径       |
| 4    | 查询远程文件     | 判断文件是否存在                         |
| 5    | 判断是否允许删除 | 根据文件状态和业务规则判断是否可删       |
| 6    | 执行删除或移动   | 优先移动到备份目录，确需删除时再执行删除 |
| 7    | 记录操作日志     | 记录操作人、文件路径、删除结果和失败原因 |
| 8    | 返回处理结果     | 返回删除成功、文件不存在或删除失败       |

流程示意如下：

```text
删除请求
  -> 参数校验
  -> 路径安全检查
  -> 查询远程文件
  -> 判断是否允许删除
  -> 删除或移动到备份目录
  -> 记录操作日志
  -> 返回处理结果
```

删除策略建议如下：

| 删除策略       | 说明                                      | 推荐程度 |
| -------------- | ----------------------------------------- | -------- |
| 直接删除       | 调用 FTP 删除命令移除远程文件             | 谨慎使用 |
| 移动到备份目录 | 将文件移动到 `/backup` 或 `/deleted` 目录 | 推荐     |
| 标记删除       | 只更新业务记录，不直接删除物理文件        | 推荐     |
| 延迟清理       | 定时清理超过保留期的备份文件              | 推荐     |

路径安全检查需要重点处理以下情况：

| 风险路径   | 处理建议                               |
| ---------- | -------------------------------------- |
| `/`        | 禁止删除 FTP 根目录                    |
| `..`       | 禁止路径中包含上级目录跳转             |
| 空路径     | 禁止执行删除                           |
| 通配符路径 | 禁止直接传入 `*`、`?` 等通配符执行删除 |
| 非业务目录 | 只能删除配置范围内的业务目录文件       |

删除流程设计要点如下：

| 设计点   | 建议                                                   |
| -------- | ------------------------------------------------------ |
| 默认策略 | 默认不直接删除，优先移动到备份目录                     |
| 权限控制 | 删除接口需要增加权限校验或操作确认                     |
| 操作审计 | 删除操作必须记录日志，便于追踪                         |
| 幂等处理 | 删除文件不存在时，可以返回“文件不存在”而不是直接抛异常 |
| 保留周期 | 备份目录中的文件可以按保留天数定期清理                 |
| 误删防护 | 禁止删除根目录、父级目录和非配置目录下的文件           |

如果业务允许远程删除，建议将删除接口设计为“逻辑删除优先、物理删除可选”。默认只移动到远程备份目录，只有明确配置 `physical-delete-enabled=true` 时才允许直接删除远程文件。



## Spring Integration FTP 集成实现

Spring Integration FTP 集成实现主要包括 `SessionFactory`、`RemoteFileTemplate`、`IntegrationFlow`、`MessageChannel` 和消息头设计。Spring Integration FTP 官方支持 FTP/FTPS 文件发送与接收，并提供 inbound channel adapter、outbound channel adapter、outbound gateway 等客户端端点；在配置这些 FTP 端点之前，需要先配置 FTP Session Factory。([Home](https://docs.spring.vmware.com/spring-integration/reference/ftp.html?utm_source=chatgpt.com))

本章节示例默认承接前文的 `FtpProperties` 配置类，包路径使用 `io.github.atengk.ftp`。如果项目已有统一包名，只需要同步调整 `package` 和文件路径即可。

### FTP SessionFactory 配置

`FTP SessionFactory` 用于创建和管理 FTP 会话，是所有 FTP 上传、下载、删除、查询操作的基础组件。Spring Integration FTP 中常用的实现类是 `DefaultFtpSessionFactory`，如果希望复用 FTP 连接，可以再包一层 `CachingSessionFactory`。官方文档也说明，`DefaultFtpSessionFactory` 是配置 FTP Session Factory 的常用实现，FTP Session 缓存需要通过缓存工厂单独处理。([Home](https://docs.spring.io/spring-integration/docs/5.0.0.M2/reference/html/ftp.html?utm_source=chatgpt.com))

本示例使用 `DefaultFtpSessionFactory` 创建底层 FTP 会话，再根据配置决定是否启用 `CachingSessionFactory`。

文件位置：`src/main/java/io/github/atengk/ftp/config/FtpIntegrationConfig.java`

该配置类用于创建 FTP SessionFactory、RemoteFileTemplate 和基础消息通道。

```java
package io.github.atengk.ftp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;
import org.springframework.messaging.MessageChannel;

/**
 * FTP 集成配置
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class FtpIntegrationConfig {

    private final FtpProperties ftpProperties;

    @Bean
    public SessionFactory<FTPFile> ftpSessionFactory() {
        DefaultFtpSessionFactory sessionFactory = new DefaultFtpSessionFactory();
        sessionFactory.setHost(ftpProperties.getHost());
        sessionFactory.setPort(ftpProperties.getPort());
        sessionFactory.setUsername(ftpProperties.getUsername());
        sessionFactory.setPassword(ftpProperties.getPassword());
        sessionFactory.setControlEncoding(ftpProperties.getEncoding());

        if ("passive".equalsIgnoreCase(ftpProperties.getClientMode())) {
            sessionFactory.setClientMode(FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE);
            log.info("FTP客户端模式：被动模式");
        } else {
            sessionFactory.setClientMode(FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE);
            log.info("FTP客户端模式：主动模式");
        }

        sessionFactory.setFileType(FTPClient.BINARY_FILE_TYPE);

        if (Boolean.TRUE.equals(ftpProperties.getPoolEnabled())) {
            log.info("启用FTP Session缓存，缓存数量：{}", ftpProperties.getPoolSize());
            return new CachingSessionFactory<>(sessionFactory, ftpProperties.getPoolSize());
        }

        log.info("未启用FTP Session缓存");
        return sessionFactory;
    }

    @Bean
    public FtpRemoteFileTemplate ftpRemoteFileTemplate(SessionFactory<FTPFile> ftpSessionFactory) {
        FtpRemoteFileTemplate template = new FtpRemoteFileTemplate(ftpSessionFactory);
        template.setAutoCreateDirectory(true);
        return template;
    }

    @Bean
    public MessageChannel ftpUploadChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel ftpDownloadChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel ftpErrorChannel() {
        return new DirectChannel();
    }
}
```

配置说明如下：

| 配置项                     | 说明                                                  |
| -------------------------- | ----------------------------------------------------- |
| `DefaultFtpSessionFactory` | 创建 FTP 会话，负责连接 FTP 服务器                    |
| `CachingSessionFactory`    | 缓存 FTP Session，减少频繁创建连接的成本              |
| `setControlEncoding`       | 设置 FTP 控制连接编码，中文文件名场景建议使用 `UTF-8` |
| `setClientMode`            | 设置主动模式或被动模式，跨网络访问通常使用被动模式    |
| `setFileType`              | 设置文件传输类型，业务文件建议使用二进制模式          |
| `FtpRemoteFileTemplate`    | 封装常用远程文件操作，例如上传、下载、删除、重命名    |
| `DirectChannel`            | 点对点同步消息通道，适合简单业务流转                  |

如果生产环境中 FTP 服务位于防火墙、NAT、云服务器或跨网段环境，通常建议使用被动模式。主动模式需要 FTP 服务端主动连接客户端的数据端口，网络限制更明显。

### FTP RemoteFileTemplate 配置

`FtpRemoteFileTemplate` 是 Spring Integration FTP 提供的远程文件操作模板，适合在业务 Service 中直接调用。它可以封装上传、下载、删除、重命名等操作，避免业务代码直接管理 FTP Session。官方文档说明，`RemoteFileTemplate` 提供 `send`、`retrieve`、`remove`、`rename` 等方法，并负责可靠关闭 Session；FTP 对应的子类是 `FtpRemoteFileTemplate`。([Home](https://www.springframework.org/spring-integration/docs/4.2.8.RELEASE/reference/htmlsingle/?utm_source=chatgpt.com))

在普通业务系统中，如果只是提供接口式上传、下载、删除和文件列表查询，优先使用 `FtpRemoteFileTemplate` 会更直接。`IntegrationFlow` 更适合需要定时轮询、消息驱动、通道编排的场景。

文件位置：`src/main/java/io/github/atengk/ftp/service/FtpTemplateService.java`

该服务类封装常用 FTP 模板操作，包括上传、下载、删除和查询远程文件是否存在。

```java
package io.github.atengk.ftp.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * FTP 模板操作服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FtpTemplateService {

    private final FtpRemoteFileTemplate ftpRemoteFileTemplate;

    public boolean upload(String localFilePath, String remoteDirectory, String remoteFileName) {
        if (CharSequenceUtil.hasBlank(localFilePath, remoteDirectory, remoteFileName)) {
            throw new IllegalArgumentException("上传参数不能为空");
        }

        File localFile = FileUtil.file(localFilePath);
        if (!FileUtil.exist(localFile) || !FileUtil.isFile(localFile)) {
            throw new IllegalArgumentException("本地文件不存在：" + localFilePath);
        }

        log.info("开始上传FTP文件，本地文件：{}，远程目录：{}，远程文件名：{}", localFilePath, remoteDirectory, remoteFileName);

        boolean result = ftpRemoteFileTemplate.execute(session -> {
            if (!session.exists(remoteDirectory)) {
                session.mkdir(remoteDirectory);
                log.info("远程目录不存在，已自动创建：{}", remoteDirectory);
            }
            return session.write(Files.newInputStream(localFile.toPath()), remoteDirectory + "/" + remoteFileName);
        });

        log.info("FTP文件上传完成，本地文件：{}，远程路径：{}，结果：{}", localFilePath, remoteDirectory + "/" + remoteFileName, result);
        return result;
    }

    public boolean download(String remoteFilePath, String localFilePath) {
        if (CharSequenceUtil.hasBlank(remoteFilePath, localFilePath)) {
            throw new IllegalArgumentException("下载参数不能为空");
        }

        Path localPath = Path.of(localFilePath);
        FileUtil.mkdir(localPath.getParent().toFile());

        log.info("开始下载FTP文件，远程文件：{}，本地文件：{}", remoteFilePath, localFilePath);

        boolean result = ftpRemoteFileTemplate.execute(session -> {
            if (!session.exists(remoteFilePath)) {
                log.warn("远程文件不存在：{}", remoteFilePath);
                return false;
            }

            try (OutputStream outputStream = Files.newOutputStream(localPath)) {
                return session.read(remoteFilePath, outputStream);
            }
        });

        log.info("FTP文件下载完成，远程文件：{}，本地文件：{}，结果：{}", remoteFilePath, localFilePath, result);
        return result;
    }

    public boolean remove(String remoteFilePath) {
        if (CharSequenceUtil.isBlank(remoteFilePath)) {
            throw new IllegalArgumentException("远程文件路径不能为空");
        }

        log.info("开始删除FTP远程文件：{}", remoteFilePath);
        boolean result = ftpRemoteFileTemplate.remove(remoteFilePath);
        log.info("FTP远程文件删除完成，远程文件：{}，结果：{}", remoteFilePath, result);
        return result;
    }

    public boolean exists(String remoteFilePath) {
        if (CharSequenceUtil.isBlank(remoteFilePath)) {
            return false;
        }

        return ftpRemoteFileTemplate.execute(session -> session.exists(remoteFilePath));
    }

    public FTPFile[] list(String remoteDirectory) {
        if (CharSequenceUtil.isBlank(remoteDirectory)) {
            throw new IllegalArgumentException("远程目录不能为空");
        }

        log.info("查询FTP远程目录文件列表：{}", remoteDirectory);
        return ftpRemoteFileTemplate.execute(session -> session.list(remoteDirectory));
    }
}
```

使用 `FtpRemoteFileTemplate` 时需要注意：

| 注意项       | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| Session 释放 | 使用 `execute` 时，模板会处理 Session 获取与释放             |
| 目录创建     | 上传前需要确认远程目录是否存在，不存在则创建                 |
| 文件路径     | 远程路径建议统一使用 `/`，避免 Windows 本地路径分隔符混入远程路径 |
| 大文件下载   | 建议下载到临时文件，完成后再移动为正式文件                   |
| 删除操作     | 建议先移动到备份目录，确需物理删除时再调用 `remove`          |

如果文件操作逻辑比较简单，可以直接通过该 Service 对 Controller 暴露能力。如果需要定时扫描目录、批量拉取文件或消息流处理，则继续使用 `IntegrationFlow`。

### Integration Flow 配置

`IntegrationFlow` 用于声明 Spring Integration 的消息处理流程。FTP 场景中，常见 Flow 包括：本地文件上传到远程 FTP、定时扫描远程目录并下载到本地、下载完成后将文件交给业务通道处理。Spring Integration FTP 官方支持 inbound channel adapter、outbound channel adapter 和 outbound gateway，分别适合文件接收、文件发送和远程命令操作。([Home](https://docs.spring.vmware.com/spring-integration/reference/ftp.html?utm_source=chatgpt.com))

本节给出两个典型 Flow：上传 Flow 和下载 Flow。上传 Flow 从 `ftpUploadChannel` 接收本地文件消息并上传到远程目录；下载 Flow 定时扫描远程目录，将文件下载到本地目录后发送到 `ftpDownloadChannel`。

文件位置：`src/main/java/io/github/atengk/ftp/config/FtpFlowConfig.java`

该配置类用于声明 FTP 上传和下载 Integration Flow。

```java
package io.github.atengk.ftp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.ftp.dsl.Ftp;
import org.springframework.messaging.MessageChannel;

import java.io.File;

/**
 * FTP 文件流配置
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class FtpFlowConfig {

    private final FtpProperties ftpProperties;

    @Bean
    public IntegrationFlow ftpUploadFlow(SessionFactory<FTPFile> ftpSessionFactory,
                                         MessageChannel ftpUploadChannel) {
        return IntegrationFlow.from(ftpUploadChannel)
                .handle(Ftp.outboundAdapter(ftpSessionFactory, FileExistsMode.FAIL)
                        .autoCreateDirectory(true)
                        .useTemporaryFileName(true)
                        .remoteDirectoryExpression("headers['ftp_remote_directory']")
                        .fileNameExpression("headers['" + FileHeaders.FILENAME + "']"))
                .get();
    }

    @Bean
    public IntegrationFlow ftpDownloadFlow(SessionFactory<FTPFile> ftpSessionFactory,
                                           MessageChannel ftpDownloadChannel) {
        return IntegrationFlow.from(
                        Ftp.inboundAdapter(ftpSessionFactory)
                                .preserveTimestamp(true)
                                .remoteDirectory(ftpProperties.getRemote().getDownloadDir())
                                .localDirectory(new File(ftpProperties.getLocal().getDownloadDir()))
                                .autoCreateLocalDirectory(true)
                                .patternFilter("*.txt")
                                .maxFetchSize(ftpProperties.getSchedule().getMaxFetchSize()),
                        endpoint -> endpoint
                                .id("ftpDownloadInboundAdapter")
                                .autoStartup(Boolean.TRUE.equals(ftpProperties.getSchedule().getDownloadEnabled()))
                                .poller(Pollers.cron(ftpProperties.getSchedule().getDownloadCron())
                                        .maxMessagesPerPoll(ftpProperties.getSchedule().getMaxFetchSize())))
                .channel(ftpDownloadChannel)
                .handle(message -> {
                    File file = (File) message.getPayload();
                    log.info("FTP文件下载完成，进入后续处理流程，本地文件：{}", file.getAbsolutePath());
                })
                .get();
    }
}
```

上传 Flow 的处理逻辑如下：

| 步骤 | 说明                                              |
| ---- | ------------------------------------------------- |
| 1    | 从 `ftpUploadChannel` 接收消息                    |
| 2    | 消息体 `payload` 使用本地 `File` 对象             |
| 3    | 从消息头 `ftp_remote_directory` 获取远程目录      |
| 4    | 从消息头 `file_name` 获取远程文件名               |
| 5    | 使用 FTP outbound adapter 上传文件                |
| 6    | 远程目录不存在时自动创建                          |
| 7    | 使用临时文件名上传，避免远程系统读取未完成文件    |
| 8    | 如果远程文件已存在，按 `FileExistsMode.FAIL` 处理 |

下载 Flow 的处理逻辑如下：

| 步骤 | 说明                                             |
| ---- | ------------------------------------------------ |
| 1    | 按 `download-cron` 周期触发轮询                  |
| 2    | 扫描 `remote.download-dir` 远程目录              |
| 3    | 使用 `patternFilter("*.txt")` 过滤文件           |
| 4    | 单次最多拉取 `max-fetch-size` 个文件             |
| 5    | 下载到 `local.download-dir` 本地目录             |
| 6    | 下载完成后发送到 `ftpDownloadChannel`            |
| 7    | 后续可以接入文件解析、入库、备份、通知等业务逻辑 |

如果需要处理多种文件类型，可以将 `patternFilter("*.txt")` 调整为更适合业务的规则，例如 `.csv`、`.zip` 或按业务前缀过滤。

```java
.patternFilter("ORDER_*.csv")
```

如果业务需要更复杂的过滤规则，例如根据文件大小、修改时间、是否已处理记录进行判断，可以自定义 `FileListFilter<FTPFile>`，后续在“文件过滤规则”章节中单独实现。

### Channel 与 Message 设计

Channel 与 Message 是 Spring Integration 的核心概念。Channel 用于连接不同处理节点，Message 用于承载业务数据和元信息。在 FTP 场景中，上传消息的 `payload` 通常是本地文件，消息头中携带远程目录、远程文件名、业务类型、批次号等信息；下载消息的 `payload` 通常是已经下载到本地的文件。

建议在 FTP 模块中统一定义消息头名称，避免业务代码到处手写字符串。

文件位置：`src/main/java/io/github/atengk/ftp/constant/FtpMessageHeaders.java`

该常量类用于统一维护 FTP 消息头名称。

```java
package io.github.atengk.ftp.constant;

/**
 * FTP 消息头常量
 *
 * @author Ateng
 * @since 2026-05-07
 */
public final class FtpMessageHeaders {

    /**
     * 远程目录
     */
    public static final String REMOTE_DIRECTORY = "ftp_remote_directory";

    /**
     * 远程文件路径
     */
    public static final String REMOTE_FILE_PATH = "ftp_remote_file_path";

    /**
     * 业务类型
     */
    public static final String BUSINESS_TYPE = "ftp_business_type";

    /**
     * 批次号
     */
    public static final String BATCH_NO = "ftp_batch_no";

    /**
     * 操作类型
     */
    public static final String OPERATION_TYPE = "ftp_operation_type";

    private FtpMessageHeaders() {
    }
}
```

推荐消息头设计如下：

| 消息头                 | 说明                                                         | 示例                               |
| ---------------------- | ------------------------------------------------------------ | ---------------------------------- |
| `file_name`            | Spring Integration 文件名头，通常由 `FileHeaders.FILENAME` 设置 | `ORDER_20260507.csv`               |
| `ftp_remote_directory` | 远程目标目录                                                 | `/upload/order`                    |
| `ftp_remote_file_path` | 远程完整文件路径                                             | `/upload/order/ORDER_20260507.csv` |
| `ftp_business_type`    | 业务类型                                                     | `order`                            |
| `ftp_batch_no`         | 批次号                                                       | `202605070001`                     |
| `ftp_operation_type`   | 操作类型                                                     | `upload`、`download`、`delete`     |

Channel 设计建议如下：

| Channel              | 类型            | 用途                             |
| -------------------- | --------------- | -------------------------------- |
| `ftpUploadChannel`   | `DirectChannel` | 接收上传消息，触发 FTP 上传 Flow |
| `ftpDownloadChannel` | `DirectChannel` | 接收下载完成后的本地文件消息     |
| `ftpErrorChannel`    | `DirectChannel` | 接收 FTP 流程中的异常消息        |
| `ftpParseChannel`    | 可选            | 下载后进入文件解析流程           |
| `ftpBackupChannel`   | 可选            | 文件处理完成后进入备份流程       |

如果上传需要由业务代码主动触发，可以封装一个发送消息的 Service。

文件位置：`src/main/java/io/github/atengk/ftp/service/FtpMessageService.java`

该服务类用于向上传 Channel 发送 FTP 文件上传消息。

```java
package io.github.atengk.ftp.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import io.github.atengk.ftp.constant.FtpMessageHeaders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.file.FileHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * FTP 消息发送服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FtpMessageService {

    private final MessageChannel ftpUploadChannel;

    public boolean sendUploadMessage(String localFilePath,
                                     String remoteDirectory,
                                     String remoteFileName,
                                     String businessType,
                                     String batchNo) {
        if (CharSequenceUtil.hasBlank(localFilePath, remoteDirectory, remoteFileName)) {
            throw new IllegalArgumentException("FTP上传消息参数不能为空");
        }

        File localFile = FileUtil.file(localFilePath);
        if (!FileUtil.exist(localFile) || !FileUtil.isFile(localFile)) {
            throw new IllegalArgumentException("本地文件不存在：" + localFilePath);
        }

        Message<File> message = MessageBuilder.withPayload(localFile)
                .setHeader(FileHeaders.FILENAME, remoteFileName)
                .setHeader(FtpMessageHeaders.REMOTE_DIRECTORY, remoteDirectory)
                .setHeader(FtpMessageHeaders.BUSINESS_TYPE, businessType)
                .setHeader(FtpMessageHeaders.BATCH_NO, batchNo)
                .setHeader(FtpMessageHeaders.OPERATION_TYPE, "upload")
                .build();

        log.info("发送FTP上传消息，本地文件：{}，远程目录：{}，远程文件名：{}，业务类型：{}，批次号：{}",
                localFilePath, remoteDirectory, remoteFileName, businessType, batchNo);

        return ftpUploadChannel.send(message);
    }
}
```

调用示例如下：

```java
ftpMessageService.sendUploadMessage(
        "/data/ftp/upload/ORDER_20260507.csv",
        "/upload/order",
        "ORDER_20260507.csv",
        "order",
        "202605070001"
);
```

消息设计要点如下：

| 设计点    | 建议                                                         |
| --------- | ------------------------------------------------------------ |
| `payload` | 上传场景使用 `File`，下载场景也通常使用本地 `File`           |
| 文件名    | 使用 `FileHeaders.FILENAME`，避免自定义文件名头与框架约定冲突 |
| 远程目录  | 使用自定义头 `ftp_remote_directory`，便于 Flow 中通过表达式读取 |
| 业务字段  | 批次号、业务类型、操作类型放到消息头中，便于日志和后续处理   |
| 通道类型  | 简单同步流程使用 `DirectChannel`，异步处理可换成 `ExecutorChannel` |
| 异常处理  | 生产环境建议配置 error channel，集中记录异常和失败消息       |

如果上传请求需要等待明确结果，建议优先使用前文的 `FtpRemoteFileTemplate` 同步封装。如果只是将文件投递到集成流中处理，或者后续还有解析、备份、通知等多个节点，则更适合使用 `MessageChannel` + `IntegrationFlow`。



## 文件上传功能开发

文件上传功能用于将本地文件发送到远程 FTP 目录，核心能力包括单文件上传、批量上传、远程目录自动创建和上传结果处理。Spring Integration 的 `RemoteFileTemplate` 提供了发送、获取、删除、重命名、列目录和执行 Session 回调等远程文件操作能力，并由模板负责处理 Session 获取与释放；FTP 场景可以使用其子类 `FtpRemoteFileTemplate`。([Home](https://docs.spring.io/spring-integration/reference/ftp/rft.html?utm_source=chatgpt.com))

本章节基于前文已经配置好的 `FtpRemoteFileTemplate` 和 `FtpProperties` 实现，重点封装业务可直接调用的上传服务。

推荐文件结构如下：

```text
src/main/java/io/github/atengk/ftp
├── model
│   ├── FtpTransferResult.java
│   ├── FtpUploadRequest.java
│   ├── FtpDownloadRequest.java
│   └── FtpFileInfo.java
└── service
    ├── FtpFileService.java
    └── impl
        └── FtpFileServiceImpl.java
```

文件位置：`src/main/java/io/github/atengk/ftp/model/FtpTransferResult.java`

该结果类用于统一封装 FTP 上传、下载、删除等操作结果。

```java
package io.github.atengk.ftp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FTP 文件传输结果
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FtpTransferResult {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 本地文件路径
     */
    private String localFilePath;

    /**
     * 远程文件路径
     */
    private String remoteFilePath;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小，单位：字节
     */
    private Long fileSize;

    /**
     * 失败原因
     */
    private String errorMessage;

    /**
     * 处理耗时，单位：毫秒
     */
    private Long costMillis;
}
```

文件位置：`src/main/java/io/github/atengk/ftp/model/FtpUploadRequest.java`

该请求类用于封装单文件上传和批量上传时需要的参数。

```java
package io.github.atengk.ftp.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * FTP 文件上传请求
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class FtpUploadRequest {

    /**
     * 本地文件路径
     */
    @NotBlank(message = "本地文件路径不能为空")
    private String localFilePath;

    /**
     * 远程目录
     */
    @NotBlank(message = "远程目录不能为空")
    private String remoteDirectory;

    /**
     * 远程文件名，为空时默认使用本地文件名
     */
    private String remoteFileName;

    /**
     * 是否覆盖远程同名文件
     */
    private Boolean overwrite = false;
}
```

### 单文件上传

单文件上传用于处理明确指定本地文件和远程目录的场景，例如上传一份报表、推送一个对账文件、发送一个第三方接口文件。实现时需要先校验本地文件，再检查远程目录，最后执行上传并返回统一结果。

文件位置：`src/main/java/io/github/atengk/ftp/service/FtpFileService.java`

该接口定义 FTP 文件上传与下载的核心能力，后续 Controller 或定时任务统一调用该接口。

```java
package io.github.atengk.ftp.service;

import io.github.atengk.ftp.model.FtpDownloadRequest;
import io.github.atengk.ftp.model.FtpFileInfo;
import io.github.atengk.ftp.model.FtpTransferResult;
import io.github.atengk.ftp.model.FtpUploadRequest;

import java.util.List;

/**
 * FTP 文件服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface FtpFileService {

    /**
     * 上传单个文件
     *
     * @param request 上传请求
     * @return 上传结果
     */
    FtpTransferResult uploadFile(FtpUploadRequest request);

    /**
     * 批量上传文件
     *
     * @param requests 上传请求列表
     * @return 上传结果列表
     */
    List<FtpTransferResult> uploadBatch(List<FtpUploadRequest> requests);

    /**
     * 下载指定文件
     *
     * @param request 下载请求
     * @return 下载结果
     */
    FtpTransferResult downloadFile(FtpDownloadRequest request);

    /**
     * 拉取远程目录文件
     *
     * @param remoteDirectory 远程目录
     * @param localDirectory 本地目录
     * @param fileNamePattern 文件名正则表达式
     * @param maxFetchSize 单次最大拉取数量
     * @return 下载结果列表
     */
    List<FtpTransferResult> pullDirectory(String remoteDirectory,
                                          String localDirectory,
                                          String fileNamePattern,
                                          Integer maxFetchSize);

    /**
     * 查询远程目录文件列表
     *
     * @param remoteDirectory 远程目录
     * @param fileNamePattern 文件名正则表达式
     * @return 文件信息列表
     */
    List<FtpFileInfo> listFiles(String remoteDirectory, String fileNamePattern);
}
```

文件位置：`src/main/java/io/github/atengk/ftp/service/impl/FtpFileServiceImpl.java`

该实现类封装 FTP 上传、下载、目录创建、文件过滤和后处理逻辑。

```java
package io.github.atengk.ftp.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ReUtil;
import io.github.atengk.ftp.model.FtpDownloadRequest;
import io.github.atengk.ftp.model.FtpFileInfo;
import io.github.atengk.ftp.model.FtpTransferResult;
import io.github.atengk.ftp.model.FtpUploadRequest;
import io.github.atengk.ftp.service.FtpFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/**
 * FTP 文件服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FtpFileServiceImpl implements FtpFileService {

    private static final String OPERATION_UPLOAD = "upload";
    private static final String OPERATION_DOWNLOAD = "download";
    private static final String TEMP_UPLOAD_SUFFIX = ".uploading";
    private static final String TEMP_DOWNLOAD_SUFFIX = ".downloading";

    private final FtpRemoteFileTemplate ftpRemoteFileTemplate;

    /**
     * 上传单个文件
     *
     * @param request 上传请求
     * @return 上传结果
     */
    @Override
    public FtpTransferResult uploadFile(FtpUploadRequest request) {
        long startTime = System.currentTimeMillis();
        String localFilePath = request.getLocalFilePath();
        String remoteDirectory = request.getRemoteDirectory();

        File localFile = FileUtil.file(localFilePath);
        String remoteFileName = CharSequenceUtil.blankToDefault(request.getRemoteFileName(), localFile.getName());
        String remoteFilePath = buildRemotePath(remoteDirectory, remoteFileName);
        String remoteTempFilePath = remoteFilePath + TEMP_UPLOAD_SUFFIX;

        try {
            validateLocalFile(localFile);

            log.info("开始上传FTP文件，本地文件：{}，远程文件：{}", localFilePath, remoteFilePath);

            boolean uploaded = ftpRemoteFileTemplate.execute(session -> {
                ensureRemoteDirectory(remoteDirectory);

                if (Boolean.FALSE.equals(request.getOverwrite()) && session.exists(remoteFilePath)) {
                    throw new IllegalStateException("远程文件已存在：" + remoteFilePath);
                }

                boolean writeResult = session.write(Files.newInputStream(localFile.toPath()), remoteTempFilePath);
                if (!writeResult) {
                    return false;
                }

                if (session.exists(remoteFilePath)) {
                    session.remove(remoteFilePath);
                }

                session.rename(remoteTempFilePath, remoteFilePath);
                return true;
            });

            FtpTransferResult result = buildResult(true, OPERATION_UPLOAD, localFilePath, remoteFilePath,
                    remoteFileName, FileUtil.size(localFile), null, startTime);

            handleUploadResult(result);
            return result;
        } catch (Exception e) {
            log.error("FTP文件上传失败，本地文件：{}，远程文件：{}，原因：{}", localFilePath, remoteFilePath, e.getMessage(), e);

            FtpTransferResult result = buildResult(false, OPERATION_UPLOAD, localFilePath, remoteFilePath,
                    remoteFileName, FileUtil.exist(localFile) ? FileUtil.size(localFile) : 0L, e.getMessage(), startTime);

            handleUploadResult(result);
            return result;
        }
    }

    /**
     * 批量上传文件
     *
     * @param requests 上传请求列表
     * @return 上传结果列表
     */
    @Override
    public List<FtpTransferResult> uploadBatch(List<FtpUploadRequest> requests) {
        if (CollUtil.isEmpty(requests)) {
            log.warn("批量上传FTP文件请求为空");
            return List.of();
        }

        log.info("开始批量上传FTP文件，文件数量：{}", requests.size());

        List<FtpTransferResult> results = requests.stream()
                .map(this::uploadFile)
                .toList();

        long successCount = results.stream()
                .filter(result -> Boolean.TRUE.equals(result.getSuccess()))
                .count();

        log.info("批量上传FTP文件完成，总数：{}，成功：{}，失败：{}",
                results.size(), successCount, results.size() - successCount);

        return results;
    }

    /**
     * 下载指定文件
     *
     * @param request 下载请求
     * @return 下载结果
     */
    @Override
    public FtpTransferResult downloadFile(FtpDownloadRequest request) {
        long startTime = System.currentTimeMillis();
        String remoteFilePath = request.getRemoteFilePath();
        String fileName = extractFileName(remoteFilePath);
        String localFilePath = buildLocalPath(request.getLocalDirectory(), fileName);
        String localTempFilePath = localFilePath + TEMP_DOWNLOAD_SUFFIX;

        try {
            log.info("开始下载FTP文件，远程文件：{}，本地文件：{}", remoteFilePath, localFilePath);

            FileUtil.mkdir(request.getLocalDirectory());

            boolean downloaded = ftpRemoteFileTemplate.get(remoteFilePath, inputStream -> {
                try (OutputStream outputStream = Files.newOutputStream(Path.of(localTempFilePath))) {
                    inputStream.transferTo(outputStream);
                }
            });

            if (!downloaded) {
                throw new IllegalStateException("远程文件下载失败：" + remoteFilePath);
            }

            FileUtil.move(FileUtil.file(localTempFilePath), FileUtil.file(localFilePath), true);

            File localFile = FileUtil.file(localFilePath);
            FtpTransferResult result = buildResult(true, OPERATION_DOWNLOAD, localFilePath, remoteFilePath,
                    fileName, FileUtil.size(localFile), null, startTime);

            handleDownloadResult(result, request);
            return result;
        } catch (Exception e) {
            log.error("FTP文件下载失败，远程文件：{}，本地目录：{}，原因：{}",
                    remoteFilePath, request.getLocalDirectory(), e.getMessage(), e);

            FileUtil.del(localTempFilePath);

            FtpTransferResult result = buildResult(false, OPERATION_DOWNLOAD, localFilePath, remoteFilePath,
                    fileName, 0L, e.getMessage(), startTime);

            handleDownloadResult(result, request);
            return result;
        }
    }

    /**
     * 拉取远程目录文件
     *
     * @param remoteDirectory 远程目录
     * @param localDirectory 本地目录
     * @param fileNamePattern 文件名正则表达式
     * @param maxFetchSize 单次最大拉取数量
     * @return 下载结果列表
     */
    @Override
    public List<FtpTransferResult> pullDirectory(String remoteDirectory,
                                                 String localDirectory,
                                                 String fileNamePattern,
                                                 Integer maxFetchSize) {
        List<FtpFileInfo> files = listFiles(remoteDirectory, fileNamePattern);
        int fetchSize = maxFetchSize == null || maxFetchSize <= 0 ? files.size() : maxFetchSize;

        return files.stream()
                .sorted(Comparator.comparing(FtpFileInfo::getFileName))
                .limit(fetchSize)
                .map(fileInfo -> {
                    FtpDownloadRequest request = new FtpDownloadRequest();
                    request.setRemoteFilePath(fileInfo.getRemoteFilePath());
                    request.setLocalDirectory(localDirectory);
                    request.setDeleteRemoteAfterDownload(false);
                    return downloadFile(request);
                })
                .toList();
    }

    /**
     * 查询远程目录文件列表
     *
     * @param remoteDirectory 远程目录
     * @param fileNamePattern 文件名正则表达式
     * @return 文件信息列表
     */
    @Override
    public List<FtpFileInfo> listFiles(String remoteDirectory, String fileNamePattern) {
        if (CharSequenceUtil.isBlank(remoteDirectory)) {
            throw new IllegalArgumentException("远程目录不能为空");
        }

        FTPFile[] ftpFiles = ftpRemoteFileTemplate.list(remoteDirectory);
        if (ftpFiles == null || ftpFiles.length == 0) {
            return List.of();
        }

        return java.util.Arrays.stream(ftpFiles)
                .filter(FTPFile::isFile)
                .filter(file -> filterFile(file, fileNamePattern))
                .map(file -> FtpFileInfo.builder()
                        .fileName(file.getName())
                        .remoteFilePath(buildRemotePath(remoteDirectory, file.getName()))
                        .fileSize(file.getSize())
                        .timestamp(file.getTimestamp() == null ? null : file.getTimestamp().getTimeInMillis())
                        .build())
                .toList();
    }

    /**
     * 校验本地文件
     *
     * @param localFile 本地文件
     */
    private void validateLocalFile(File localFile) {
        if (!FileUtil.exist(localFile)) {
            throw new IllegalArgumentException("本地文件不存在：" + localFile.getAbsolutePath());
        }
        if (!FileUtil.isFile(localFile)) {
            throw new IllegalArgumentException("本地路径不是文件：" + localFile.getAbsolutePath());
        }
        if (FileUtil.size(localFile) <= 0) {
            throw new IllegalArgumentException("本地文件为空：" + localFile.getAbsolutePath());
        }
    }

    /**
     * 自动创建远程目录
     *
     * @param remoteDirectory 远程目录
     */
    private void ensureRemoteDirectory(String remoteDirectory) {
        ftpRemoteFileTemplate.execute(session -> {
            if (session.exists(remoteDirectory)) {
                return true;
            }

            String[] directories = CharSequenceUtil.removePrefix(remoteDirectory, "/").split("/");
            String currentPath = "";

            for (String directory : directories) {
                if (CharSequenceUtil.isBlank(directory)) {
                    continue;
                }

                currentPath = currentPath + "/" + directory;
                if (!session.exists(currentPath)) {
                    session.mkdir(currentPath);
                    log.info("创建FTP远程目录：{}", currentPath);
                }
            }

            return true;
        });
    }

    /**
     * 过滤远程文件
     *
     * @param ftpFile FTP 文件
     * @param fileNamePattern 文件名正则表达式
     * @return 是否通过过滤
     */
    private boolean filterFile(FTPFile ftpFile, String fileNamePattern) {
        String fileName = ftpFile.getName();

        if (CharSequenceUtil.startWith(fileName, ".")) {
            return false;
        }
        if (CharSequenceUtil.endWith(fileName, TEMP_UPLOAD_SUFFIX)
                || CharSequenceUtil.endWith(fileName, TEMP_DOWNLOAD_SUFFIX)) {
            return false;
        }
        if (CharSequenceUtil.isBlank(fileNamePattern)) {
            return true;
        }

        return ReUtil.isMatch(fileNamePattern, fileName);
    }

    /**
     * 上传结果处理
     *
     * @param result 上传结果
     */
    private void handleUploadResult(FtpTransferResult result) {
        if (Boolean.TRUE.equals(result.getSuccess())) {
            log.info("FTP上传成功，文件：{}，远程路径：{}，耗时：{}ms",
                    result.getFileName(), result.getRemoteFilePath(), result.getCostMillis());
            return;
        }

        log.warn("FTP上传失败，文件：{}，远程路径：{}，原因：{}",
                result.getFileName(), result.getRemoteFilePath(), result.getErrorMessage());
    }

    /**
     * 下载结果处理
     *
     * @param result 下载结果
     * @param request 下载请求
     */
    private void handleDownloadResult(FtpTransferResult result, FtpDownloadRequest request) {
        if (!Boolean.TRUE.equals(result.getSuccess())) {
            log.warn("FTP下载失败，远程路径：{}，原因：{}", result.getRemoteFilePath(), result.getErrorMessage());
            return;
        }

        if (Boolean.TRUE.equals(request.getDeleteRemoteAfterDownload())) {
            boolean removed = ftpRemoteFileTemplate.remove(request.getRemoteFilePath());
            log.info("下载完成后删除远程文件，远程路径：{}，删除结果：{}", request.getRemoteFilePath(), removed);
        }

        log.info("FTP下载成功，本地路径：{}，远程路径：{}，耗时：{}ms",
                result.getLocalFilePath(), result.getRemoteFilePath(), result.getCostMillis());
    }

    /**
     * 构建操作结果
     *
     * @param success 是否成功
     * @param operationType 操作类型
     * @param localFilePath 本地路径
     * @param remoteFilePath 远程路径
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @param errorMessage 错误信息
     * @param startTime 开始时间
     * @return FTP 传输结果
     */
    private FtpTransferResult buildResult(Boolean success,
                                          String operationType,
                                          String localFilePath,
                                          String remoteFilePath,
                                          String fileName,
                                          Long fileSize,
                                          String errorMessage,
                                          Long startTime) {
        return FtpTransferResult.builder()
                .success(success)
                .operationType(operationType)
                .localFilePath(localFilePath)
                .remoteFilePath(remoteFilePath)
                .fileName(fileName)
                .fileSize(fileSize)
                .errorMessage(errorMessage)
                .costMillis(System.currentTimeMillis() - startTime)
                .build();
    }

    /**
     * 构建远程路径
     *
     * @param remoteDirectory 远程目录
     * @param fileName 文件名
     * @return 远程完整路径
     */
    private String buildRemotePath(String remoteDirectory, String fileName) {
        return CharSequenceUtil.removeSuffix(remoteDirectory, "/") + "/" + fileName;
    }

    /**
     * 构建本地路径
     *
     * @param localDirectory 本地目录
     * @param fileName 文件名
     * @return 本地完整路径
     */
    private String buildLocalPath(String localDirectory, String fileName) {
        return FileUtil.file(localDirectory, fileName).getAbsolutePath();
    }

    /**
     * 提取文件名
     *
     * @param filePath 文件路径
     * @return 文件名
     */
    private String extractFileName(String filePath) {
        return CharSequenceUtil.subAfter(filePath, "/", true);
    }
}
```

### 批量文件上传

批量上传用于一次性上传多个本地文件，例如批量推送日报、结算文件、订单文件或接口文件。批量上传不建议因为单个文件失败而中断整个批次，推荐逐个处理并返回每个文件的独立结果。

批量上传调用示例：

```java
List<FtpUploadRequest> requests = List.of(
        buildUploadRequest("/data/ftp/upload/ORDER_20260507.csv", "/upload/order", "ORDER_20260507.csv"),
        buildUploadRequest("/data/ftp/upload/ORDER_20260508.csv", "/upload/order", "ORDER_20260508.csv")
);

List<FtpTransferResult> results = ftpFileService.uploadBatch(requests);
```

辅助构建方法如下：

```java
private FtpUploadRequest buildUploadRequest(String localFilePath, String remoteDirectory, String remoteFileName) {
    FtpUploadRequest request = new FtpUploadRequest();
    request.setLocalFilePath(localFilePath);
    request.setRemoteDirectory(remoteDirectory);
    request.setRemoteFileName(remoteFileName);
    request.setOverwrite(false);
    return request;
}
```

批量上传处理建议如下：

| 设计项     | 建议                             |
| ---------- | -------------------------------- |
| 单文件失败 | 不影响后续文件上传               |
| 返回结果   | 返回每个文件的成功或失败结果     |
| 日志记录   | 记录总数、成功数、失败数         |
| 幂等控制   | 默认不覆盖远程同名文件           |
| 失败重试   | 可由定时任务或业务调度层控制重试 |
| 文件归档   | 上传成功后可移动到本地备份目录   |

### 远程目录自动创建

远程目录自动创建用于解决上传前目标目录不存在的问题。对于 `/upload/order/20260507` 这种多级目录，需要逐级判断并创建，不能只创建最后一级目录。

本实现中的 `ensureRemoteDirectory` 方法会按路径层级逐级创建目录：

```java
private void ensureRemoteDirectory(String remoteDirectory) {
    ftpRemoteFileTemplate.execute(session -> {
        if (session.exists(remoteDirectory)) {
            return true;
        }

        String[] directories = CharSequenceUtil.removePrefix(remoteDirectory, "/").split("/");
        String currentPath = "";

        for (String directory : directories) {
            if (CharSequenceUtil.isBlank(directory)) {
                continue;
            }

            currentPath = currentPath + "/" + directory;
            if (!session.exists(currentPath)) {
                session.mkdir(currentPath);
                log.info("创建FTP远程目录：{}", currentPath);
            }
        }

        return true;
    });
}
```

目录创建策略建议如下：

| 策略       | 说明                                             |
| ---------- | ------------------------------------------------ |
| 上传前创建 | 每次上传前检查目标目录是否存在                   |
| 逐级创建   | 支持多级目录，例如 `/upload/order/20260507`      |
| 权限最小化 | FTP 账号只授予业务目录下的建目录权限             |
| 异常明确化 | 无权限创建目录时返回清晰错误信息                 |
| 目录缓存   | 高频上传场景可缓存已确认存在的目录，减少远程交互 |

### 上传结果处理

上传结果处理用于统一记录上传成功、失败、耗时、远程路径和文件大小等信息。生产环境中建议将上传结果落库，便于后续查询、重试、审计和问题排查。

上传结果字段建议如下：

| 字段             | 说明                    |
| ---------------- | ----------------------- |
| `success`        | 是否上传成功            |
| `operationType`  | 操作类型，例如 `upload` |
| `localFilePath`  | 本地文件路径            |
| `remoteFilePath` | 远程文件路径            |
| `fileName`       | 文件名                  |
| `fileSize`       | 文件大小                |
| `errorMessage`   | 失败原因                |
| `costMillis`     | 上传耗时                |

上传结果处理建议如下：

| 场景           | 处理方式                                 |
| -------------- | ---------------------------------------- |
| 上传成功       | 记录成功日志，可将本地文件移动到备份目录 |
| 上传失败       | 记录失败日志，可将本地文件移动到失败目录 |
| 远程文件已存在 | 默认返回失败，除非请求明确允许覆盖       |
| 本地文件不存在 | 直接返回失败，不执行 FTP 操作            |
| 远程目录无权限 | 返回失败并记录远程目录路径               |
| 网络中断       | 返回失败，由外部调度或重试机制处理       |

如果项目需要数据库记录，可以将 `FtpTransferResult` 转换为 `ftp_transfer_record` 表记录，按 `fileName + remoteFilePath + operationType + batchNo` 做唯一约束，避免重复上传记录混乱。

## 文件下载功能开发

文件下载功能用于从远程 FTP 目录获取文件并保存到本地目录。核心能力包括指定文件下载、目录文件拉取、文件过滤和下载后处理。`RemoteFileTemplate` 提供 `get`、`list`、`remove`、`rename` 等方法，可以覆盖常见远程文件读取、目录扫描和后处理需求。([Home](https://docs.spring.io/spring-integration/api/org/springframework/integration/file/remote/RemoteFileTemplate.html?utm_source=chatgpt.com))

### 指定文件下载

指定文件下载用于从 FTP 下载一个明确路径的文件，例如 `/download/receipt/ORDER_RESULT_20260507.csv`。实现时建议先下载到临时文件，下载完成后再移动为正式文件，避免后续业务读取到未下载完成的文件。

文件位置：`src/main/java/io/github/atengk/ftp/model/FtpDownloadRequest.java`

该请求类用于封装指定文件下载参数。

```java
package io.github.atengk.ftp.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * FTP 文件下载请求
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class FtpDownloadRequest {

    /**
     * 远程文件完整路径
     */
    @NotBlank(message = "远程文件路径不能为空")
    private String remoteFilePath;

    /**
     * 本地保存目录
     */
    @NotBlank(message = "本地保存目录不能为空")
    private String localDirectory;

    /**
     * 下载完成后是否删除远程文件
     */
    private Boolean deleteRemoteAfterDownload = false;
}
```

指定文件下载调用示例：

```java
FtpDownloadRequest request = new FtpDownloadRequest();
request.setRemoteFilePath("/download/receipt/ORDER_RESULT_20260507.csv");
request.setLocalDirectory("/data/ftp/download/receipt");
request.setDeleteRemoteAfterDownload(false);

FtpTransferResult result = ftpFileService.downloadFile(request);
```

指定文件下载处理建议如下：

| 设计项   | 建议                                   |
| -------- | -------------------------------------- |
| 临时文件 | 下载时使用 `.downloading` 后缀         |
| 正式文件 | 下载完成后再移动为正式文件名           |
| 本地目录 | 下载前自动创建本地目录                 |
| 远程删除 | 默认不删除远程文件                     |
| 异常处理 | 下载失败时清理本地临时文件             |
| 结果记录 | 记录远程路径、本地路径、文件大小和耗时 |

### 目录文件拉取

目录文件拉取用于定时扫描远程目录，并将符合规则的文件批量下载到本地。例如每 5 分钟扫描 `/download/reconciliation` 目录，拉取所有 `RECON_*.csv` 文件。

文件位置：`src/main/java/io/github/atengk/ftp/model/FtpFileInfo.java`

该信息类用于承载远程目录扫描得到的文件信息。

```java
package io.github.atengk.ftp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FTP 文件信息
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FtpFileInfo {

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 远程文件完整路径
     */
    private String remoteFilePath;

    /**
     * 文件大小，单位：字节
     */
    private Long fileSize;

    /**
     * 文件时间戳
     */
    private Long timestamp;
}
```

目录拉取调用示例：

```java
List<FtpTransferResult> results = ftpFileService.pullDirectory(
        "/download/reconciliation",
        "/data/ftp/download/reconciliation",
        "^RECON_.*\\.csv$",
        20
);
```

目录拉取处理流程如下：

| 步骤 | 说明                           |
| ---- | ------------------------------ |
| 1    | 查询远程目录文件列表           |
| 2    | 排除目录、隐藏文件、临时文件   |
| 3    | 按文件名正则表达式过滤         |
| 4    | 按最大拉取数量限制本次处理文件 |
| 5    | 逐个执行指定文件下载           |
| 6    | 汇总每个文件的下载结果         |
| 7    | 记录成功数和失败数             |

目录拉取建议不要一次性处理过多文件。可以通过 `maxFetchSize` 限制单次拉取数量，避免大量文件同时下载导致 FTP 服务、应用线程或磁盘 IO 压力过高。

### 文件过滤规则

文件过滤规则用于决定哪些远程文件可以被下载或同步。常见过滤条件包括文件类型、文件名前缀、文件后缀、临时文件后缀、隐藏文件、文件大小、文件修改时间和是否已处理。

本实现中的基础过滤逻辑如下：

```java
private boolean filterFile(FTPFile ftpFile, String fileNamePattern) {
    String fileName = ftpFile.getName();

    if (CharSequenceUtil.startWith(fileName, ".")) {
        return false;
    }
    if (CharSequenceUtil.endWith(fileName, TEMP_UPLOAD_SUFFIX)
            || CharSequenceUtil.endWith(fileName, TEMP_DOWNLOAD_SUFFIX)) {
        return false;
    }
    if (CharSequenceUtil.isBlank(fileNamePattern)) {
        return true;
    }

    return ReUtil.isMatch(fileNamePattern, fileName);
}
```

推荐过滤规则如下：

| 过滤规则     | 示例                           | 说明                   |
| ------------ | ------------------------------ | ---------------------- |
| 文件后缀     | `.*\\.csv$`                    | 只拉取 CSV 文件        |
| 文件前缀     | `^ORDER_.*`                    | 只拉取订单文件         |
| 日期批次     | `^RECON_20260507_.*\\.csv$`    | 只拉取指定日期批次文件 |
| 排除临时文件 | `*.uploading`、`*.downloading` | 避免处理未完成文件     |
| 排除隐藏文件 | `.DS_Store`、`.temp`           | 避免处理系统文件       |
| 文件大小     | `size > 0`                     | 排除空文件             |
| 幂等记录     | 已处理文件不再拉取             | 避免重复处理           |

常见文件名正则示例：

```text
^ORDER_.*\.csv$
^RECON_\d{8}_.*\.txt$
^REPORT_\d{6}\.zip$
^PAYMENT_RESULT_.*\.csv$
```

如果业务需要严格幂等，建议将已处理文件记录到数据库中，字段至少包含：

| 字段                 | 说明         |
| -------------------- | ------------ |
| `remote_file_path`   | 远程文件路径 |
| `file_name`          | 文件名       |
| `file_size`          | 文件大小     |
| `last_modified_time` | 远程修改时间 |
| `operation_type`     | 操作类型     |
| `process_status`     | 处理状态     |
| `created_time`       | 创建时间     |
| `updated_time`       | 更新时间     |

仅依赖文件名判断幂等不够可靠，因为外部系统可能会重新生成同名文件。更稳妥的方式是结合远程路径、文件名、文件大小和修改时间判断。

### 下载后处理策略

下载后处理用于定义文件下载成功或失败后的动作。常见动作包括本地文件转移、远程文件删除、远程文件移动到备份目录、触发业务解析、记录处理日志等。

下载后处理策略建议如下：

| 策略               | 说明                                 | 推荐程度          |
| ------------------ | ------------------------------------ | ----------------- |
| 保留远程文件       | 下载后不修改远程文件                 | 适合只读 FTP 场景 |
| 删除远程文件       | 下载成功后删除远程文件               | 谨慎使用          |
| 移动到远程备份目录 | 下载成功后将远程文件移动到 `/backup` | 推荐              |
| 本地备份           | 业务处理成功后移动到本地备份目录     | 推荐              |
| 本地失败目录       | 业务处理失败后移动到本地失败目录     | 推荐              |
| 投递业务事件       | 下载完成后通知解析服务处理           | 推荐              |

如果希望下载完成后将远程文件移动到备份目录，可以在 Service 中增加如下方法。

```java
/**
 * 移动远程文件
 *
 * @param sourcePath 源文件路径
 * @param targetPath 目标文件路径
 */
private void moveRemoteFile(String sourcePath, String targetPath) {
    ftpRemoteFileTemplate.execute(session -> {
        String targetDirectory = CharSequenceUtil.subBefore(targetPath, "/", true);
        ensureRemoteDirectory(targetDirectory);
        session.rename(sourcePath, targetPath);
        log.info("移动FTP远程文件，源路径：{}，目标路径：{}", sourcePath, targetPath);
        return true;
    });
}
```

下载后处理流程建议如下：

```text
远程文件下载成功
  -> 本地临时文件改名为正式文件
  -> 记录下载成功日志
  -> 远程文件移动到备份目录或保留
  -> 投递业务处理事件
  -> 业务处理成功后移动到本地备份目录
  -> 业务处理失败后移动到本地失败目录
```

如果业务文件需要解析入库，建议不要在 FTP 下载方法内部直接完成复杂解析。更合理的方式是下载成功后生成处理记录或发送本地事件，由独立的业务处理服务读取本地文件并解析。这样 FTP 模块只负责可靠传输，业务模块负责文件内容处理，职责边界更清晰。



## 定时同步任务

定时同步任务用于在无人值守场景下自动拉取远程 FTP 文件，或者将本地待上传文件推送到远程 FTP 目录。定时任务需要重点处理任务开关、单次处理数量、幂等控制、失败重试和异常日志，避免重复上传、重复下载或因为单个文件失败影响整批任务。

在 Spring Boot 中启用定时任务，需要在启动类上增加 `@EnableScheduling`。

文件位置：`src/main/java/io/github/atengk/ftp/FtpApplication.java`

该启动类用于启用 Spring Boot 应用和定时任务能力。

```java
package io.github.atengk.ftp;

import io.github.atengk.ftp.config.FtpProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * FTP 示例应用启动类
 *
 * @author Ateng
 * @since 2026-05-07
 */
@EnableScheduling
@EnableConfigurationProperties(FtpProperties.class)
@SpringBootApplication
public class FtpApplication {

    public static void main(String[] args) {
        SpringApplication.run(FtpApplication.class, args);
    }
}
```

### 定时拉取远程文件

定时拉取远程文件用于按照配置的 Cron 周期扫描远程 FTP 目录，并将符合规则的文件下载到本地目录。该任务适用于对账文件、回执文件、结果文件、第三方批量文件等场景。

推荐流程如下：

| 步骤 | 说明                                                  |
| ---- | ----------------------------------------------------- |
| 1    | 根据 `ftp.schedule.download-enabled` 判断任务是否启用 |
| 2    | 扫描 `ftp.remote.download-dir` 远程下载目录           |
| 3    | 按文件名规则过滤目标文件                              |
| 4    | 根据幂等记录排除已处理文件                            |
| 5    | 按 `max-fetch-size` 限制单次拉取数量                  |
| 6    | 下载文件到 `ftp.local.download-dir`                   |
| 7    | 记录下载结果                                          |
| 8    | 下载成功后更新幂等记录                                |

文件位置：`src/main/java/io/github/atengk/ftp/task/FtpSyncTask.java`

该定时任务类用于定时拉取远程文件和定时上传本地文件。

```java
package io.github.atengk.ftp.task;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import io.github.atengk.ftp.config.FtpProperties;
import io.github.atengk.ftp.model.FtpFileInfo;
import io.github.atengk.ftp.model.FtpTransferResult;
import io.github.atengk.ftp.model.FtpUploadRequest;
import io.github.atengk.ftp.service.FtpFileService;
import io.github.atengk.ftp.service.FtpIdempotentService;
import io.github.atengk.ftp.support.FtpRetryExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

/**
 * FTP 定时同步任务
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FtpSyncTask {

    private static final String DEFAULT_DOWNLOAD_PATTERN = ".*\\.(csv|txt|zip)$";
    private static final String DEFAULT_UPLOAD_PATTERN = ".*\\.(csv|txt|zip)$";

    private final FtpProperties ftpProperties;
    private final FtpFileService ftpFileService;
    private final FtpIdempotentService ftpIdempotentService;
    private final FtpRetryExecutor ftpRetryExecutor;

    /**
     * 定时拉取远程文件
     */
    @Scheduled(cron = "${ftp.schedule.download-cron}")
    public void pullRemoteFiles() {
        if (!Boolean.TRUE.equals(ftpProperties.getSchedule().getDownloadEnabled())) {
            log.debug("FTP定时下载任务未启用，跳过执行");
            return;
        }

        String remoteDirectory = ftpProperties.getRemote().getDownloadDir();
        String localDirectory = ftpProperties.getLocal().getDownloadDir();
        Integer maxFetchSize = ftpProperties.getSchedule().getMaxFetchSize();

        log.info("开始执行FTP定时下载任务，远程目录：{}，本地目录：{}，单次最大数量：{}",
                remoteDirectory, localDirectory, maxFetchSize);

        ftpRetryExecutor.execute("FTP定时下载任务", () -> {
            List<FtpFileInfo> remoteFiles = ftpFileService.listFiles(remoteDirectory, DEFAULT_DOWNLOAD_PATTERN);
            if (CollUtil.isEmpty(remoteFiles)) {
                log.info("FTP远程目录暂无待下载文件，远程目录：{}", remoteDirectory);
                return;
            }

            List<FtpFileInfo> waitingFiles = remoteFiles.stream()
                    .filter(file -> !ftpIdempotentService.isProcessed(file.getRemoteFilePath(), file.getFileSize(), "download"))
                    .limit(maxFetchSize)
                    .toList();

            if (CollUtil.isEmpty(waitingFiles)) {
                log.info("FTP远程目录文件均已处理，无需重复下载，远程目录：{}", remoteDirectory);
                return;
            }

            waitingFiles.forEach(fileInfo -> {
                FtpTransferResult result = ftpFileService.downloadFile(buildDownloadRequest(fileInfo.getRemoteFilePath(), localDirectory));
                if (Boolean.TRUE.equals(result.getSuccess())) {
                    ftpIdempotentService.markProcessed(fileInfo.getRemoteFilePath(), fileInfo.getFileSize(), "download");
                }
            });
        });

        log.info("FTP定时下载任务执行结束，远程目录：{}", remoteDirectory);
    }

    /**
     * 定时上传本地文件
     */
    @Scheduled(cron = "${ftp.schedule.upload-cron}")
    public void uploadLocalFiles() {
        if (!Boolean.TRUE.equals(ftpProperties.getSchedule().getUploadEnabled())) {
            log.debug("FTP定时上传任务未启用，跳过执行");
            return;
        }

        String localUploadDir = ftpProperties.getLocal().getUploadDir();
        String remoteUploadDir = ftpProperties.getRemote().getUploadDir();
        Integer maxFetchSize = ftpProperties.getSchedule().getMaxFetchSize();

        log.info("开始执行FTP定时上传任务，本地目录：{}，远程目录：{}，单次最大数量：{}",
                localUploadDir, remoteUploadDir, maxFetchSize);

        ftpRetryExecutor.execute("FTP定时上传任务", () -> {
            List<File> localFiles = FileUtil.loopFiles(localUploadDir, file ->
                    FileUtil.isFile(file) && file.getName().matches(DEFAULT_UPLOAD_PATTERN));

            if (CollUtil.isEmpty(localFiles)) {
                log.info("本地目录暂无待上传文件，本地目录：{}", localUploadDir);
                return;
            }

            localFiles.stream()
                    .filter(file -> !ftpIdempotentService.isProcessed(file.getAbsolutePath(), FileUtil.size(file), "upload"))
                    .limit(maxFetchSize)
                    .forEach(file -> {
                        FtpUploadRequest request = new FtpUploadRequest();
                        request.setLocalFilePath(file.getAbsolutePath());
                        request.setRemoteDirectory(remoteUploadDir);
                        request.setRemoteFileName(file.getName());
                        request.setOverwrite(false);

                        FtpTransferResult result = ftpFileService.uploadFile(request);
                        if (Boolean.TRUE.equals(result.getSuccess())) {
                            ftpIdempotentService.markProcessed(file.getAbsolutePath(), FileUtil.size(file), "upload");
                        }
                    });
        });

        log.info("FTP定时上传任务执行结束，本地目录：{}", localUploadDir);
    }

    /**
     * 构建下载请求
     *
     * @param remoteFilePath 远程文件路径
     * @param localDirectory 本地目录
     * @return 下载请求
     */
    private io.github.atengk.ftp.model.FtpDownloadRequest buildDownloadRequest(String remoteFilePath, String localDirectory) {
        io.github.atengk.ftp.model.FtpDownloadRequest request = new io.github.atengk.ftp.model.FtpDownloadRequest();
        request.setRemoteFilePath(remoteFilePath);
        request.setLocalDirectory(localDirectory);
        request.setDeleteRemoteAfterDownload(false);
        return request;
    }
}
```

该任务默认只处理 `.csv`、`.txt`、`.zip` 文件。实际项目中可以将文件名规则抽取到配置项，例如 `ftp.schedule.download-pattern` 和 `ftp.schedule.upload-pattern`，便于不同环境调整。

### 本地文件定时上传

本地文件定时上传用于定期扫描本地上传目录，将符合规则的文件上传到远程 FTP 目录。该任务适用于报表推送、业务文件归档、接口文件批量发送等场景。

推荐流程如下：

| 步骤 | 说明                                                |
| ---- | --------------------------------------------------- |
| 1    | 根据 `ftp.schedule.upload-enabled` 判断任务是否启用 |
| 2    | 扫描 `ftp.local.upload-dir` 本地上传目录            |
| 3    | 排除目录、临时文件和不符合规则的文件                |
| 4    | 根据幂等记录排除已上传文件                          |
| 5    | 按 `max-fetch-size` 限制单次上传数量                |
| 6    | 调用 `uploadFile` 上传到远程目录                    |
| 7    | 上传成功后记录幂等状态                              |
| 8    | 可根据业务要求将本地文件移动到备份目录              |

定时上传建议默认关闭，只在业务明确需要本地目录自动推送时开启。

文件位置：`src/main/resources/application.yml`

```yaml
ftp:
  schedule:
    # 定时上传默认关闭，避免误传本地文件
    upload-enabled: false

    # 每 10 分钟扫描一次本地上传目录
    upload-cron: "0 */10 * * * ?"

    # 单次最多上传 20 个文件
    max-fetch-size: 20
```

定时上传注意事项如下：

| 注意项   | 说明                                     |
| -------- | ---------------------------------------- |
| 默认关闭 | 避免测试文件、临时文件被误上传           |
| 文件规则 | 只上传符合业务命名规范的文件             |
| 幂等控制 | 已上传成功的文件不能重复上传             |
| 覆盖策略 | 默认不覆盖远程同名文件                   |
| 本地归档 | 上传成功后建议移动到本地备份目录         |
| 日志记录 | 记录文件名、远程目录、上传结果和失败原因 |

### 幂等处理

幂等处理用于避免同一个文件被重复上传或重复下载。FTP 场景中，仅凭文件名判断是否处理过不够可靠，因为外部系统可能会重新生成同名文件。因此建议使用“文件唯一键 + 文件大小 + 操作类型”的组合进行判断。

生产环境推荐使用数据库或 Redis 存储幂等记录。文档示例先给出一个内存版实现，便于开发阶段验证流程；正式项目应替换为 MySQL、PostgreSQL、Redis 或业务处理记录表。

文件位置：`src/main/java/io/github/atengk/ftp/service/FtpIdempotentService.java`

该接口定义 FTP 文件处理幂等能力。

```java
package io.github.atengk.ftp.service;

/**
 * FTP 幂等服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface FtpIdempotentService {

    /**
     * 判断文件是否已处理
     *
     * @param fileKey 文件唯一标识
     * @param fileSize 文件大小
     * @param operationType 操作类型
     * @return 是否已处理
     */
    boolean isProcessed(String fileKey, Long fileSize, String operationType);

    /**
     * 标记文件已处理
     *
     * @param fileKey 文件唯一标识
     * @param fileSize 文件大小
     * @param operationType 操作类型
     */
    void markProcessed(String fileKey, Long fileSize, String operationType);
}
```

文件位置：`src/main/java/io/github/atengk/ftp/service/impl/MemoryFtpIdempotentServiceImpl.java`

该实现类使用内存 Map 记录已处理文件，仅适用于本地开发和功能验证。

```java
package io.github.atengk.ftp.service.impl;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.core.text.CharSequenceUtil;
import io.github.atengk.ftp.service.FtpIdempotentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存版 FTP 幂等服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
public class MemoryFtpIdempotentServiceImpl implements FtpIdempotentService {

    private final Set<String> processedKeys = ConcurrentHashMap.newKeySet();

    /**
     * 判断文件是否已处理
     *
     * @param fileKey 文件唯一标识
     * @param fileSize 文件大小
     * @param operationType 操作类型
     * @return 是否已处理
     */
    @Override
    public boolean isProcessed(String fileKey, Long fileSize, String operationType) {
        String key = buildKey(fileKey, fileSize, operationType);
        boolean processed = processedKeys.contains(key);
        if (processed) {
            log.info("FTP文件已处理，跳过重复操作，标识：{}", fileKey);
        }
        return processed;
    }

    /**
     * 标记文件已处理
     *
     * @param fileKey 文件唯一标识
     * @param fileSize 文件大小
     * @param operationType 操作类型
     */
    @Override
    public void markProcessed(String fileKey, Long fileSize, String operationType) {
        String key = buildKey(fileKey, fileSize, operationType);
        processedKeys.add(key);
        log.info("记录FTP文件幂等标识，文件：{}，操作：{}", fileKey, operationType);
    }

    /**
     * 构建幂等键
     *
     * @param fileKey 文件唯一标识
     * @param fileSize 文件大小
     * @param operationType 操作类型
     * @return 幂等键
     */
    private String buildKey(String fileKey, Long fileSize, String operationType) {
        if (CharSequenceUtil.hasBlank(fileKey, operationType)) {
            throw new IllegalArgumentException("FTP幂等参数不能为空");
        }
        String source = fileKey + ":" + fileSize + ":" + operationType;
        return SecureUtil.md5(source);
    }
}
```

生产环境表结构可以参考如下设计。

```sql
CREATE TABLE ftp_transfer_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    file_key VARCHAR(500) NOT NULL COMMENT '文件唯一标识，可使用远程路径或本地路径',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    file_size BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小',
    operation_type VARCHAR(32) NOT NULL COMMENT '操作类型：upload/download/delete',
    process_status VARCHAR(32) NOT NULL COMMENT '处理状态：success/failed/processing',
    error_message VARCHAR(1000) NULL COMMENT '失败原因',
    created_time DATETIME NOT NULL COMMENT '创建时间',
    updated_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_file_operation (file_key, file_size, operation_type)
) COMMENT='FTP文件传输记录表';
```

幂等设计建议如下：

| 场景     | 幂等键建议                           |
| -------- | ------------------------------------ |
| 上传     | 本地文件路径 + 文件大小 + `upload`   |
| 下载     | 远程文件路径 + 文件大小 + `download` |
| 删除     | 远程文件路径 + `delete`              |
| 文件解析 | 本地文件路径 + 文件大小 + 业务类型   |
| 批量任务 | 批次号 + 文件路径 + 操作类型         |

### 异常重试

异常重试用于处理 FTP 连接闪断、网络抖动、远程服务短暂不可用等临时异常。重试不应无条件无限执行，应通过配置控制最大重试次数，并在每次失败时记录日志。

本示例使用简单的手动重试实现，不额外引入 Spring Retry 依赖。

文件位置：`src/main/java/io/github/atengk/ftp/support/FtpRetryExecutor.java`

该组件用于统一执行带重试的 FTP 任务。

```java
package io.github.atengk.ftp.support;

import cn.hutool.core.thread.ThreadUtil;
import io.github.atengk.ftp.config.FtpProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * FTP 重试执行器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FtpRetryExecutor {

    private final FtpProperties ftpProperties;

    /**
     * 执行带重试的任务
     *
     * @param taskName 任务名称
     * @param runnable 任务逻辑
     */
    public void execute(String taskName, Runnable runnable) {
        Integer retryCount = ftpProperties.getSchedule().getRetryCount();
        int maxAttempts = retryCount == null ? 1 : retryCount + 1;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                runnable.run();
                return;
            } catch (Exception e) {
                log.warn("FTP任务执行失败，任务：{}，第{}次尝试，总次数：{}，原因：{}",
                        taskName, attempt, maxAttempts, e.getMessage(), e);

                if (attempt >= maxAttempts) {
                    log.error("FTP任务超过最大重试次数，任务：{}，总次数：{}", taskName, maxAttempts);
                    return;
                }

                ThreadUtil.sleep(1000L * attempt);
            }
        }
    }
}
```

重试策略建议如下：

| 策略       | 说明                                                 |
| ---------- | ---------------------------------------------------- |
| 最大次数   | 通过 `ftp.schedule.retry-count` 控制                 |
| 重试间隔   | 可按尝试次数递增，避免频繁打满 FTP 服务              |
| 可重试异常 | 网络抖动、连接超时、读取超时等临时异常               |
| 不重试异常 | 参数错误、本地文件不存在、远程目录无权限等确定性错误 |
| 失败记录   | 超过最大重试次数后记录失败状态                       |
| 告警通知   | 生产环境可接入日志告警、企业微信、钉钉或监控平台     |

对于关键业务文件，不建议只依赖定时任务日志。应将失败记录写入数据库，后续通过后台页面、运维任务或人工按钮重新触发。

## 业务接口设计

业务接口用于将 FTP 上传、下载、文件列表查询和删除能力暴露给业务系统或测试工具。接口层负责参数校验、请求接收、结果返回和基础日志记录，具体 FTP 操作仍由 `FtpFileService` 完成。

推荐接口路径统一使用 `/api/ftp/files`，便于后续做权限控制、审计日志和接口网关转发。

### 上传接口

上传接口用于根据本地文件路径将文件上传到远程 FTP 目录。该接口适合后端系统内部调用或测试环境验证，不建议直接暴露给公网用户，因为它接收的是服务器本地文件路径。

接口设计如下：

| 项目     | 内容                              |
| -------- | --------------------------------- |
| 请求方式 | `POST`                            |
| 接口路径 | `/api/ftp/files/upload`           |
| 请求类型 | `application/json`                |
| 功能说明 | 上传服务器本地文件到 FTP 远程目录 |

请求示例：

```json
{
  "localFilePath": "/data/ftp/upload/ORDER_20260507.csv",
  "remoteDirectory": "/upload/order",
  "remoteFileName": "ORDER_20260507.csv",
  "overwrite": false
}
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "success": true,
    "operationType": "upload",
    "localFilePath": "/data/ftp/upload/ORDER_20260507.csv",
    "remoteFilePath": "/upload/order/ORDER_20260507.csv",
    "fileName": "ORDER_20260507.csv",
    "fileSize": 10240,
    "errorMessage": null,
    "costMillis": 326
  }
}
```

### 下载接口

下载接口用于将 FTP 远程指定文件下载到服务器本地目录。该接口适合业务系统主动拉取指定文件，例如根据回执文件名下载第三方处理结果。

接口设计如下：

| 项目     | 内容                            |
| -------- | ------------------------------- |
| 请求方式 | `POST`                          |
| 接口路径 | `/api/ftp/files/download`       |
| 请求类型 | `application/json`              |
| 功能说明 | 下载 FTP 远程指定文件到本地目录 |

请求示例：

```json
{
  "remoteFilePath": "/download/receipt/ORDER_RESULT_20260507.csv",
  "localDirectory": "/data/ftp/download/receipt",
  "deleteRemoteAfterDownload": false
}
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "success": true,
    "operationType": "download",
    "localFilePath": "/data/ftp/download/receipt/ORDER_RESULT_20260507.csv",
    "remoteFilePath": "/download/receipt/ORDER_RESULT_20260507.csv",
    "fileName": "ORDER_RESULT_20260507.csv",
    "fileSize": 20480,
    "errorMessage": null,
    "costMillis": 412
  }
}
```

### 文件列表查询接口

文件列表查询接口用于查询远程 FTP 目录中的文件列表，并支持通过正则表达式过滤文件名。该接口常用于下载前预览、运维排查、业务页面展示或测试验证。

接口设计如下：

| 项目     | 内容                                 |
| -------- | ------------------------------------ |
| 请求方式 | `GET`                                |
| 接口路径 | `/api/ftp/files/list`                |
| 请求参数 | `remoteDirectory`、`fileNamePattern` |
| 功能说明 | 查询远程目录文件列表                 |

请求示例：

```bash
curl "http://localhost:8080/api/ftp/files/list?remoteDirectory=/download/receipt&fileNamePattern=^ORDER_.*\.csv$"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "fileName": "ORDER_20260507.csv",
      "remoteFilePath": "/download/receipt/ORDER_20260507.csv",
      "fileSize": 10240,
      "timestamp": 1778102400000
    }
  ]
}
```

### 删除接口

删除接口用于删除 FTP 远程文件。删除操作风险较高，建议默认只允许删除业务目录下的文件，并记录完整操作日志。生产环境更推荐“移动到远程备份目录”而不是直接物理删除。

接口设计如下：

| 项目     | 内容                    |
| -------- | ----------------------- |
| 请求方式 | `DELETE`                |
| 接口路径 | `/api/ftp/files/delete` |
| 请求类型 | `application/json`      |
| 功能说明 | 删除 FTP 远程指定文件   |

请求示例：

```json
{
  "remoteFilePath": "/upload/order/ORDER_20260507.csv"
}
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": true
}
```

业务接口相关代码如下。

文件位置：`src/main/java/io/github/atengk/ftp/model/FtpDeleteRequest.java`

该请求类用于封装远程文件删除参数。

```java
package io.github.atengk.ftp.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * FTP 文件删除请求
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class FtpDeleteRequest {

    /**
     * 远程文件完整路径
     */
    @NotBlank(message = "远程文件路径不能为空")
    private String remoteFilePath;
}
```

文件位置：`src/main/java/io/github/atengk/ftp/model/ApiResult.java`

该返回类用于统一封装业务接口响应结果。

```java
package io.github.atengk.ftp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口统一返回结果
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /**
     * 响应编码
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
     * @return 统一结果
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "操作成功", data);
    }

    /**
     * 失败响应
     *
     * @param message 失败消息
     * @return 统一结果
     */
    public static <T> ApiResult<T> fail(String message) {
        return new ApiResult<>(500, message, null);
    }
}
```

需要在前文 `FtpFileService` 中补充删除方法。

文件位置：`src/main/java/io/github/atengk/ftp/service/FtpFileService.java`

```java
/**
 * 删除远程文件
 *
 * @param remoteFilePath 远程文件路径
 * @return 删除结果
 */
boolean deleteRemoteFile(String remoteFilePath);
```

需要在前文 `FtpFileServiceImpl` 中补充删除实现。

文件位置：`src/main/java/io/github/atengk/ftp/service/impl/FtpFileServiceImpl.java`

```java
/**
 * 删除远程文件
 *
 * @param remoteFilePath 远程文件路径
 * @return 删除结果
 */
@Override
public boolean deleteRemoteFile(String remoteFilePath) {
    if (CharSequenceUtil.isBlank(remoteFilePath)) {
        throw new IllegalArgumentException("远程文件路径不能为空");
    }
    if ("/".equals(remoteFilePath) || CharSequenceUtil.contains(remoteFilePath, "..")) {
        throw new IllegalArgumentException("非法远程文件路径：" + remoteFilePath);
    }

    log.info("开始删除FTP远程文件：{}", remoteFilePath);

    boolean result = ftpRemoteFileTemplate.execute(session -> {
        if (!session.exists(remoteFilePath)) {
            log.warn("FTP远程文件不存在，无需删除：{}", remoteFilePath);
            return false;
        }
        return session.remove(remoteFilePath);
    });

    log.info("FTP远程文件删除完成，远程路径：{}，删除结果：{}", remoteFilePath, result);
    return result;
}
```

文件位置：`src/main/java/io/github/atengk/ftp/controller/FtpFileController.java`

该控制器提供上传、下载、文件列表查询和删除接口。

```java
package io.github.atengk.ftp.controller;

import cn.hutool.core.text.CharSequenceUtil;
import io.github.atengk.ftp.model.ApiResult;
import io.github.atengk.ftp.model.FtpDeleteRequest;
import io.github.atengk.ftp.model.FtpDownloadRequest;
import io.github.atengk.ftp.model.FtpFileInfo;
import io.github.atengk.ftp.model.FtpTransferResult;
import io.github.atengk.ftp.model.FtpUploadRequest;
import io.github.atengk.ftp.service.FtpFileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * FTP 文件接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ftp/files")
public class FtpFileController {

    private final FtpFileService ftpFileService;

    /**
     * 上传文件
     *
     * @param request 上传请求
     * @return 上传结果
     */
    @PostMapping("/upload")
    public ApiResult<FtpTransferResult> upload(@Valid @RequestBody FtpUploadRequest request) {
        log.info("接收FTP上传请求，本地文件：{}，远程目录：{}", request.getLocalFilePath(), request.getRemoteDirectory());
        return ApiResult.success(ftpFileService.uploadFile(request));
    }

    /**
     * 下载文件
     *
     * @param request 下载请求
     * @return 下载结果
     */
    @PostMapping("/download")
    public ApiResult<FtpTransferResult> download(@Valid @RequestBody FtpDownloadRequest request) {
        log.info("接收FTP下载请求，远程文件：{}，本地目录：{}", request.getRemoteFilePath(), request.getLocalDirectory());
        return ApiResult.success(ftpFileService.downloadFile(request));
    }

    /**
     * 查询远程文件列表
     *
     * @param remoteDirectory 远程目录
     * @param fileNamePattern 文件名正则表达式
     * @return 文件列表
     */
    @GetMapping("/list")
    public ApiResult<List<FtpFileInfo>> list(@RequestParam String remoteDirectory,
                                             @RequestParam(required = false) String fileNamePattern) {
        if (CharSequenceUtil.isBlank(remoteDirectory)) {
            throw new IllegalArgumentException("远程目录不能为空");
        }

        log.info("接收FTP文件列表查询请求，远程目录：{}，文件规则：{}", remoteDirectory, fileNamePattern);
        return ApiResult.success(ftpFileService.listFiles(remoteDirectory, fileNamePattern));
    }

    /**
     * 删除远程文件
     *
     * @param request 删除请求
     * @return 删除结果
     */
    @DeleteMapping("/delete")
    public ApiResult<Boolean> delete(@Valid @RequestBody FtpDeleteRequest request) {
        log.info("接收FTP删除请求，远程文件：{}", request.getRemoteFilePath());
        return ApiResult.success(ftpFileService.deleteRemoteFile(request.getRemoteFilePath()));
    }
}
```

接口调用命令如下。

上传文件：

```bash
curl -X POST "http://localhost:8080/api/ftp/files/upload" \
  -H "Content-Type: application/json" \
  -d '{
    "localFilePath": "/data/ftp/upload/ORDER_20260507.csv",
    "remoteDirectory": "/upload/order",
    "remoteFileName": "ORDER_20260507.csv",
    "overwrite": false
  }'
```

下载文件：

```bash
curl -X POST "http://localhost:8080/api/ftp/files/download" \
  -H "Content-Type: application/json" \
  -d '{
    "remoteFilePath": "/download/receipt/ORDER_RESULT_20260507.csv",
    "localDirectory": "/data/ftp/download/receipt",
    "deleteRemoteAfterDownload": false
  }'
```

查询远程文件列表：

```bash
curl "http://localhost:8080/api/ftp/files/list?remoteDirectory=/download/receipt&fileNamePattern=^ORDER_.*\.csv$"
```

删除远程文件：

```bash
curl -X DELETE "http://localhost:8080/api/ftp/files/delete" \
  -H "Content-Type: application/json" \
  -d '{
    "remoteFilePath": "/upload/order/ORDER_20260507.csv"
  }'
```

接口设计注意事项如下：

| 注意项   | 说明                                                         |
| -------- | ------------------------------------------------------------ |
| 权限控制 | 上传、下载、删除接口建议接入 Sa-Token、Spring Security 或网关鉴权 |
| 路径校验 | 禁止传入 `/`、`..`、空路径和非业务目录路径                   |
| 删除保护 | 删除接口默认应限制使用，生产环境优先移动到备份目录           |
| 参数校验 | 请求 DTO 使用 `jakarta.validation` 注解进行基础校验          |
| 操作日志 | 上传、下载、删除必须记录请求参数、结果和异常                 |
| 文件路径 | 接口接收的是服务器路径，不是用户电脑路径                     |
| 统一返回 | 使用统一响应结构，便于前端或调用方处理                       |



## 异常处理与日志

异常处理与日志用于保证 FTP 文件传输过程中的问题可定位、可追踪、可恢复。FTP 操作容易受到网络、权限、目录、文件状态和远程服务稳定性的影响，因此需要对连接异常、传输异常、参数异常和业务操作日志进行统一设计。

建议将 FTP 异常分为三类：参数类异常、连接类异常和传输类异常。参数类异常通常不需要重试，连接类异常可以按策略重试，传输类异常需要结合文件状态判断是否可以再次执行。

### FTP 连接异常

FTP 连接异常通常发生在创建连接、登录认证、进入被动模式、读取远程目录或连接超时时。常见原因包括 FTP 地址错误、端口不通、账号密码错误、防火墙拦截、被动端口未开放、FTP 服务不可用等。

常见连接异常如下：

| 异常场景     | 常见原因                               | 处理建议                                  |
| ------------ | -------------------------------------- | ----------------------------------------- |
| 连接超时     | FTP 主机不可达、端口未开放、防火墙拦截 | 检查网络、安全组、防火墙和端口映射        |
| 登录失败     | 用户名或密码错误、账号被禁用           | 检查账号密码和 FTP 服务端用户状态         |
| 列目录失败   | 目录不存在、账号无权限、被动端口异常   | 检查远程目录权限和被动模式配置            |
| 数据连接失败 | 被动端口未开放、NAT 地址错误           | 检查 `PASV_ADDRESS` 和被动端口范围        |
| 连接被重置   | FTP 服务异常、网络中断、长时间空闲     | 增加重试、缩短任务批次、启用 Session 缓存 |

建议定义统一的 FTP 业务异常，避免 Controller 或定时任务直接抛出底层异常。

文件位置：`src/main/java/io/github/atengk/ftp/exception/FtpBizException.java`

该异常类用于封装 FTP 模块中的业务异常和传输异常。

```java
package io.github.atengk.ftp.exception;

import lombok.Getter;

/**
 * FTP 业务异常
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Getter
public class FtpBizException extends RuntimeException {

    /**
     * 异常编码
     */
    private final String code;

    public FtpBizException(String message) {
        super(message);
        this.code = "FTP_ERROR";
    }

    public FtpBizException(String code, String message) {
        super(message);
        this.code = code;
    }

    public FtpBizException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
```

在 FTP Service 中捕获底层异常时，可以转换为业务异常，便于全局异常处理统一返回。

```java
try {
    return ftpRemoteFileTemplate.execute(session -> session.exists(remoteFilePath));
} catch (Exception e) {
    log.error("FTP连接或远程操作失败，远程路径：{}，原因：{}", remoteFilePath, e.getMessage(), e);
    throw new FtpBizException("FTP_CONNECTION_ERROR", "FTP连接或远程操作失败：" + e.getMessage(), e);
}
```

连接异常处理建议如下：

| 处理项   | 建议                                                    |
| -------- | ------------------------------------------------------- |
| 日志级别 | 连接失败、认证失败使用 `error`，短暂超时重试使用 `warn` |
| 重试策略 | 连接超时、读取超时可重试，账号密码错误不应重试          |
| 告警策略 | 连续失败超过阈值后触发告警                              |
| 敏感信息 | 日志中禁止打印 FTP 密码                                 |
| 排查信息 | 日志应包含主机、端口、远程目录、操作类型和异常摘要      |

### 文件传输异常

文件传输异常通常发生在上传、下载、删除、重命名、移动文件的过程中。该类异常需要重点关注文件是否处于中间状态，例如远程是否已生成临时文件、本地是否已有 `.downloading` 文件、远程正式文件是否已覆盖等。

常见传输异常如下：

| 异常场景   | 常见原因                                     | 处理建议                                 |
| ---------- | -------------------------------------------- | ---------------------------------------- |
| 上传失败   | 本地文件不存在、远程目录无权限、远程磁盘满   | 校验本地文件，检查远程目录权限和空间     |
| 下载失败   | 远程文件不存在、本地目录无权限、磁盘空间不足 | 检查远程路径、本地权限和磁盘空间         |
| 重命名失败 | 目标文件已存在、无权限、FTP 服务限制         | 上传前判断同名文件，必要时按覆盖策略处理 |
| 删除失败   | 账号无删除权限、文件被占用、路径非法         | 默认不直接删除，优先移动到备份目录       |
| 文件不完整 | 网络中断、传输过程异常                       | 使用临时文件名，完成后再改为正式文件名   |

建议统一定义异常编码，便于日志检索和前端展示。

| 异常编码               | 说明             |
| ---------------------- | ---------------- |
| `FTP_CONNECTION_ERROR` | FTP 连接异常     |
| `FTP_AUTH_ERROR`       | FTP 登录认证异常 |
| `FTP_UPLOAD_ERROR`     | FTP 文件上传异常 |
| `FTP_DOWNLOAD_ERROR`   | FTP 文件下载异常 |
| `FTP_DELETE_ERROR`     | FTP 文件删除异常 |
| `FTP_PARAM_ERROR`      | FTP 参数校验异常 |
| `FTP_PATH_ERROR`       | FTP 路径非法异常 |

文件传输异常处理原则如下：

| 原则             | 说明                                                       |
| ---------------- | ---------------------------------------------------------- |
| 上传使用临时文件 | 上传为 `.uploading`，成功后重命名为正式文件                |
| 下载使用临时文件 | 下载为 `.downloading`，成功后移动为正式文件                |
| 失败保留上下文   | 日志记录本地路径、远程路径、文件名、文件大小               |
| 避免重复处理     | 传输成功后记录幂等状态                                     |
| 失败可重试       | 网络类异常可重试，参数和权限类异常不重试                   |
| 不吞异常         | 定时任务可以捕获异常继续处理下一个文件，但必须记录失败原因 |

### 参数校验异常

参数校验异常主要来自接口请求参数、配置参数和路径参数。FTP 接口涉及服务器文件路径和远程路径，如果缺少校验，容易导致误删、越权访问、覆盖文件或处理非业务目录文件。

建议使用 `jakarta.validation` 对请求 DTO 做基础校验，再通过业务方法做路径安全校验。

文件位置：`src/main/java/io/github/atengk/ftp/handler/GlobalExceptionHandler.java`

该全局异常处理器用于统一处理参数异常、FTP 业务异常和系统异常。

```java
package io.github.atengk.ftp.handler;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.ftp.exception.FtpBizException;
import io.github.atengk.ftp.model.ApiResult;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

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
     * 处理请求体参数校验异常
     *
     * @param e 参数异常
     * @return 统一响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<String> messages = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        String message = CollUtil.join(messages, "；");
        log.warn("请求参数校验失败：{}", message);
        return ApiResult.fail(message);
    }

    /**
     * 处理表单参数绑定异常
     *
     * @param e 参数绑定异常
     * @return 统一响应
     */
    @ExceptionHandler(BindException.class)
    public ApiResult<Void> handleBindException(BindException e) {
        List<String> messages = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        String message = CollUtil.join(messages, "；");
        log.warn("请求参数绑定失败：{}", message);
        return ApiResult.fail(message);
    }

    /**
     * 处理单参数校验异常
     *
     * @param e 参数校验异常
     * @return 统一响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResult<Void> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("请求参数校验失败：{}", e.getMessage());
        return ApiResult.fail(e.getMessage());
    }

    /**
     * 处理非法参数异常
     *
     * @param e 非法参数异常
     * @return 统一响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResult<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("请求参数非法：{}", e.getMessage());
        return ApiResult.fail(e.getMessage());
    }

    /**
     * 处理 FTP 业务异常
     *
     * @param e FTP 业务异常
     * @return 统一响应
     */
    @ExceptionHandler(FtpBizException.class)
    public ApiResult<Void> handleFtpBizException(FtpBizException e) {
        log.error("FTP业务异常，编码：{}，原因：{}", e.getCode(), e.getMessage(), e);
        return ApiResult.fail(e.getMessage());
    }

    /**
     * 处理未知异常
     *
     * @param e 未知异常
     * @return 统一响应
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception e) {
        log.error("系统异常：{}", e.getMessage(), e);
        return ApiResult.fail("系统异常，请联系管理员");
    }
}
```

路径参数需要额外校验，尤其是删除接口和下载接口。建议增加统一路径校验工具类。

文件位置：`src/main/java/io/github/atengk/ftp/support/FtpPathValidator.java`

该工具类用于校验远程路径，避免根目录、上级目录和空路径等危险操作。

```java
package io.github.atengk.ftp.support;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;

import java.util.List;

/**
 * FTP 路径校验工具
 *
 * @author Ateng
 * @since 2026-05-07
 */
public final class FtpPathValidator {

    private static final List<String> DANGEROUS_TOKENS = List.of("..", "*", "?", "\\");
    private static final List<String> FORBIDDEN_PATHS = List.of("/", ".");

    private FtpPathValidator() {
    }

    /**
     * 校验远程文件路径
     *
     * @param remoteFilePath 远程文件路径
     */
    public static void validateRemoteFilePath(String remoteFilePath) {
        if (CharSequenceUtil.isBlank(remoteFilePath)) {
            throw new IllegalArgumentException("远程文件路径不能为空");
        }
        if (CollUtil.contains(FORBIDDEN_PATHS, remoteFilePath)) {
            throw new IllegalArgumentException("禁止操作危险远程路径：" + remoteFilePath);
        }
        boolean dangerous = DANGEROUS_TOKENS.stream()
                .anyMatch(token -> CharSequenceUtil.contains(remoteFilePath, token));
        if (dangerous) {
            throw new IllegalArgumentException("远程文件路径包含非法字符：" + remoteFilePath);
        }
    }

    /**
     * 校验远程目录
     *
     * @param remoteDirectory 远程目录
     */
    public static void validateRemoteDirectory(String remoteDirectory) {
        if (CharSequenceUtil.isBlank(remoteDirectory)) {
            throw new IllegalArgumentException("远程目录不能为空");
        }
        if (CollUtil.contains(FORBIDDEN_PATHS, remoteDirectory)) {
            throw new IllegalArgumentException("禁止操作危险远程目录：" + remoteDirectory);
        }
        boolean dangerous = DANGEROUS_TOKENS.stream()
                .anyMatch(token -> CharSequenceUtil.contains(remoteDirectory, token));
        if (dangerous) {
            throw new IllegalArgumentException("远程目录包含非法字符：" + remoteDirectory);
        }
    }
}
```

参数校验建议如下：

| 校验项     | 建议                                          |
| ---------- | --------------------------------------------- |
| 本地路径   | 不允许为空，必须在业务允许目录内              |
| 远程路径   | 不允许为空，不允许包含 `..`、`*`、`?`、反斜杠 |
| 删除路径   | 禁止删除 `/`、`.`、空路径和非业务目录         |
| 文件名     | 建议限制长度、后缀和命名规则                  |
| 覆盖参数   | 默认不覆盖，必须显式传入 `overwrite=true`     |
| 下载后删除 | 默认不删除远程文件                            |

### 操作日志记录

操作日志用于记录 FTP 操作的关键上下文，包括操作类型、文件路径、文件大小、处理结果、耗时和失败原因。生产环境中，建议日志和数据库记录同时使用：日志用于排查问题，数据库记录用于查询、审计、重试和统计。

日志字段建议如下：

| 字段             | 说明                                          |
| ---------------- | --------------------------------------------- |
| `operationType`  | 操作类型，例如 `upload`、`download`、`delete` |
| `localFilePath`  | 本地文件路径                                  |
| `remoteFilePath` | 远程文件路径                                  |
| `fileName`       | 文件名                                        |
| `fileSize`       | 文件大小                                      |
| `success`        | 是否成功                                      |
| `costMillis`     | 操作耗时                                      |
| `errorMessage`   | 失败原因                                      |
| `batchNo`        | 批次号，可选                                  |
| `businessType`   | 业务类型，可选                                |

如果需要使用 AOP 统一记录接口操作日志，需要补充 AOP 依赖。

文件位置：`pom.xml`

```xml
<!-- AOP 支持：用于统一记录接口操作日志，可选 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

文件位置：`src/main/java/io/github/atengk/ftp/annotation/FtpOperationLog.java`

该注解用于标记需要记录操作日志的 FTP 接口或方法。

```java
package io.github.atengk.ftp.annotation;

import java.lang.annotation.*;

/**
 * FTP 操作日志注解
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FtpOperationLog {

    /**
     * 操作名称
     *
     * @return 操作名称
     */
    String value();

    /**
     * 操作类型
     *
     * @return 操作类型
     */
    String operationType();
}
```

文件位置：`src/main/java/io/github/atengk/ftp/aspect/FtpOperationLogAspect.java`

该切面用于统一记录 FTP 接口操作日志和耗时。

```java
package io.github.atengk.ftp.aspect;

import cn.hutool.core.date.StopWatch;
import cn.hutool.json.JSONUtil;
import io.github.atengk.ftp.annotation.FtpOperationLog;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * FTP 操作日志切面
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Aspect
@Component
public class FtpOperationLogAspect {

    /**
     * 记录 FTP 操作日志
     *
     * @param joinPoint 切点
     * @param operationLog 操作日志注解
     * @return 方法执行结果
     * @throws Throwable 执行异常
     */
    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, FtpOperationLog operationLog) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();

            log.info("FTP操作成功，名称：{}，类型：{}，耗时：{}ms，参数：{}，结果：{}",
                    operationLog.value(),
                    operationLog.operationType(),
                    stopWatch.getTotalTimeMillis(),
                    JSONUtil.toJsonStr(joinPoint.getArgs()),
                    JSONUtil.toJsonStr(result));

            return result;
        } catch (Throwable e) {
            stopWatch.stop();

            log.error("FTP操作失败，名称：{}，类型：{}，耗时：{}ms，参数：{}，原因：{}",
                    operationLog.value(),
                    operationLog.operationType(),
                    stopWatch.getTotalTimeMillis(),
                    JSONUtil.toJsonStr(joinPoint.getArgs()),
                    e.getMessage(),
                    e);

            throw e;
        }
    }
}
```

在 Controller 方法上使用示例：

```java
@FtpOperationLog(value = "上传FTP文件", operationType = "upload")
@PostMapping("/upload")
public ApiResult<FtpTransferResult> upload(@Valid @RequestBody FtpUploadRequest request) {
    return ApiResult.success(ftpFileService.uploadFile(request));
}
```

日志设计注意事项如下：

| 注意项         | 说明                                                   |
| -------------- | ------------------------------------------------------ |
| 禁止打印密码   | FTP 密码、密钥、Token 不允许进入日志                   |
| 文件路径完整   | 上传、下载、删除日志应包含完整远程路径                 |
| 异常栈保留     | 异常日志应保留堆栈，便于定位根因                       |
| 结果可检索     | 操作类型、文件名、业务类型建议作为固定字段输出         |
| 日志级别清晰   | 成功用 `info`，可恢复问题用 `warn`，失败异常用 `error` |
| 大参数谨慎打印 | 文件内容、超大集合不应直接打印                         |

## 测试与验证

测试与验证用于确认 FTP 服务、Spring Boot 配置、上传下载功能、定时任务和异常处理是否符合预期。建议先验证本地 FTP 环境，再验证接口功能，最后验证定时同步任务。

### 本地 FTP 环境验证

本地 FTP 环境验证的目标是确认 FTP 服务可登录、可列目录、可创建目录、可上传文件和可下载文件。只有本地 FTP 服务正常，Spring Boot 集成测试才有意义。

启动本地 FTP 服务：

```bash
mkdir -p ~/docker-data/ftp/data

docker run -d \
  --name local-ftp \
  -p 21:21 \
  -p 21100-21110:21100-21110 \
  -e FTP_USER=ateng \
  -e FTP_PASS=123456 \
  -e PASV_ADDRESS=127.0.0.1 \
  -e PASV_MIN_PORT=21100 \
  -e PASV_MAX_PORT=21110 \
  -v ~/docker-data/ftp/data:/home/vsftpd \
  fauria/vsftpd
```

验证 FTP 服务状态：

```bash
docker ps | grep local-ftp
docker logs --tail=100 local-ftp
```

使用 `lftp` 验证登录和文件操作：

```bash
echo "hello ftp" > /tmp/ftp-test.txt

lftp -u ateng,123456 127.0.0.1 <<EOF
pwd
ls
mkdir -p upload/order
cd upload/order
put /tmp/ftp-test.txt -o ftp-test.txt
ls
get ftp-test.txt -o /tmp/ftp-download-test.txt
bye
EOF

cat /tmp/ftp-download-test.txt
```

命令验证点如下：

| 验证项      | 预期结果                       |
| ----------- | ------------------------------ |
| `docker ps` | 能看到 `local-ftp` 容器运行中  |
| `lftp` 登录 | 可以正常连接并登录             |
| `mkdir -p`  | 可以创建远程目录               |
| `put`       | 可以上传本地文件               |
| `get`       | 可以下载远程文件               |
| `cat`       | 下载后的文件内容与上传文件一致 |

如果登录成功但 `ls`、`put`、`get` 卡住，优先检查被动模式端口映射和 `PASV_ADDRESS` 配置。

### 上传功能测试

上传功能测试用于确认接口可以将服务器本地文件上传到 FTP 远程目录，并正确返回上传结果。测试前需要先在服务器本地准备待上传文件。

准备本地测试文件：

```bash
mkdir -p /data/ftp/upload
echo "order_id,amount" > /data/ftp/upload/ORDER_20260507.csv
echo "1001,99.90" >> /data/ftp/upload/ORDER_20260507.csv
```

调用上传接口：

```bash
curl -X POST "http://localhost:8080/api/ftp/files/upload" \
  -H "Content-Type: application/json" \
  -d '{
    "localFilePath": "/data/ftp/upload/ORDER_20260507.csv",
    "remoteDirectory": "/upload/order",
    "remoteFileName": "ORDER_20260507.csv",
    "overwrite": false
  }'
```

验证远程文件是否上传成功：

```bash
lftp -u ateng,123456 127.0.0.1 <<EOF
ls /upload/order
bye
EOF
```

上传功能验证点如下：

| 验证项   | 预期结果                                           |
| -------- | -------------------------------------------------- |
| 接口响应 | `success=true`                                     |
| 远程目录 | `/upload/order` 自动创建                           |
| 远程文件 | 存在 `ORDER_20260507.csv`                          |
| 文件大小 | 与本地文件大小一致                                 |
| 应用日志 | 输出上传开始、上传完成和耗时日志                   |
| 重复上传 | `overwrite=false` 时，同名文件应返回失败或明确提示 |

### 下载功能测试

下载功能测试用于确认接口可以从 FTP 远程路径下载指定文件到本地目录。测试前可以先通过 `lftp` 或上传接口准备远程文件。

准备远程测试文件：

```bash
echo "result_id,status" > /tmp/ORDER_RESULT_20260507.csv
echo "1001,SUCCESS" >> /tmp/ORDER_RESULT_20260507.csv

lftp -u ateng,123456 127.0.0.1 <<EOF
mkdir -p /download/receipt
put /tmp/ORDER_RESULT_20260507.csv -o /download/receipt/ORDER_RESULT_20260507.csv
bye
EOF
```

调用下载接口：

```bash
curl -X POST "http://localhost:8080/api/ftp/files/download" \
  -H "Content-Type: application/json" \
  -d '{
    "remoteFilePath": "/download/receipt/ORDER_RESULT_20260507.csv",
    "localDirectory": "/data/ftp/download/receipt",
    "deleteRemoteAfterDownload": false
  }'
```

验证本地文件是否下载成功：

```bash
ls -l /data/ftp/download/receipt
cat /data/ftp/download/receipt/ORDER_RESULT_20260507.csv
```

下载功能验证点如下：

| 验证项   | 预期结果                                           |
| -------- | -------------------------------------------------- |
| 接口响应 | `success=true`                                     |
| 本地目录 | 自动创建 `/data/ftp/download/receipt`              |
| 本地文件 | 存在 `ORDER_RESULT_20260507.csv`                   |
| 文件内容 | 与远程文件内容一致                                 |
| 临时文件 | 下载完成后不应残留 `.downloading` 文件             |
| 远程文件 | `deleteRemoteAfterDownload=false` 时远程文件仍存在 |

### 定时同步测试

定时同步测试用于确认系统可以按照配置周期自动扫描远程目录或本地目录，并执行下载或上传操作。测试前需要确认 `ftp.schedule.download-enabled` 或 `ftp.schedule.upload-enabled` 已开启。

测试定时下载配置：

```yaml
ftp:
  schedule:
    # 开启定时下载
    download-enabled: true

    # 每 30 秒执行一次，测试环境使用；生产环境应调大间隔
    download-cron: "*/30 * * * * ?"

    # 单次最多处理 5 个文件
    max-fetch-size: 5
```

准备远程待下载文件：

```bash
echo "sync test" > /tmp/SYNC_20260507.txt

lftp -u ateng,123456 127.0.0.1 <<EOF
mkdir -p /download
put /tmp/SYNC_20260507.txt -o /download/SYNC_20260507.txt
bye
EOF
```

观察应用日志：

```bash
tail -f logs/spring-integration-ftp-demo.log
```

验证本地下载结果：

```bash
ls -l /data/ftp/download
cat /data/ftp/download/SYNC_20260507.txt
```

测试定时上传配置：

```yaml
ftp:
  schedule:
    # 开启定时上传
    upload-enabled: true

    # 每 30 秒执行一次，测试环境使用；生产环境应调大间隔
    upload-cron: "*/30 * * * * ?"

    # 单次最多处理 5 个文件
    max-fetch-size: 5
```

准备本地待上传文件：

```bash
mkdir -p /data/ftp/upload
echo "local sync upload" > /data/ftp/upload/LOCAL_SYNC_20260507.txt
```

验证远程上传结果：

```bash
lftp -u ateng,123456 127.0.0.1 <<EOF
ls /upload
bye
EOF
```

定时同步验证点如下：

| 验证项   | 预期结果                                                     |
| -------- | ------------------------------------------------------------ |
| 任务开关 | `download-enabled=false` 或 `upload-enabled=false` 时任务不执行 |
| 执行周期 | 按 Cron 表达式触发                                           |
| 文件过滤 | 只处理符合规则的文件                                         |
| 单次数量 | 不超过 `max-fetch-size`                                      |
| 幂等处理 | 同一文件不会重复处理                                         |
| 异常重试 | 失败时按 `retry-count` 重试                                  |
| 日志输出 | 能看到任务开始、处理结果、任务结束日志                       |

## 部署与运行

部署与运行用于说明生产环境上线前需要调整的配置、目录权限和启动验证步骤。FTP 模块涉及外部网络和文件系统权限，部署时需要同时检查应用配置、服务器目录、FTP 网络连通性和运行日志。

### 配置文件调整

生产环境需要将本地测试 FTP 地址、账号、目录和调度周期替换为真实配置。敏感信息不建议直接写入配置文件，应通过环境变量、配置中心、Kubernetes Secret 或服务器启动参数注入。

生产配置示例：

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  application:
    name: spring-integration-ftp-demo

server:
  port: 8080

ftp:
  # FTP 服务地址，生产环境通过环境变量注入
  host: ${FTP_HOST}

  # FTP 服务端口，默认 21
  port: ${FTP_PORT:21}

  # FTP 用户名
  username: ${FTP_USERNAME}

  # FTP 密码，不建议明文写入配置文件
  password: ${FTP_PASSWORD}

  # 文件名编码
  encoding: UTF-8

  # 跨网络访问通常建议使用 passive
  client-mode: passive

  # 连接超时时间
  connect-timeout: 10s

  # 读取超时时间
  read-timeout: 30s

  # 启用 Session 缓存
  pool-enabled: true

  # Session 缓存数量
  pool-size: 5

  local:
    # 本地待上传目录
    upload-dir: /data/app/ftp/upload

    # 本地下载目录
    download-dir: /data/app/ftp/download

    # 本地临时目录
    temp-dir: /data/app/ftp/temp

    # 本地备份目录
    backup-dir: /data/app/ftp/backup

    # 本地失败目录
    error-dir: /data/app/ftp/error

  remote:
    # 远程上传目录
    upload-dir: /upload

    # 远程下载目录
    download-dir: /download

    # 远程临时目录
    temp-dir: /temp

    # 远程备份目录
    backup-dir: /backup

    # 远程失败目录
    error-dir: /error

  schedule:
    # 根据业务需要开启
    download-enabled: true
    upload-enabled: false
    sync-enabled: true

    # 生产环境建议使用较低频率，避免频繁扫描 FTP
    download-cron: "0 */5 * * * ?"
    upload-cron: "0 */10 * * * ?"
    sync-cron: "0 0/15 * * * ?"

    # 单次最大处理数量
    max-fetch-size: 20

    # 失败重试次数
    retry-count: 3

logging:
  file:
    name: logs/spring-integration-ftp-demo.log
  level:
    io.github.atengk.ftp: info
    org.springframework.integration: warn
```

生产环境启动变量示例：

```bash
export FTP_HOST="ftp.example.com"
export FTP_PORT="21"
export FTP_USERNAME="ftp_user"
export FTP_PASSWORD="your_password"
```

配置调整检查项如下：

| 检查项    | 说明                                         |
| --------- | -------------------------------------------- |
| FTP 地址  | 是否为生产 FTP 地址                          |
| FTP 端口  | 控制端口是否开放                             |
| FTP 账号  | 是否具备读、写、建目录、删除等必要权限       |
| 被动模式  | 跨网段访问通常应开启                         |
| 远程目录  | 上传、下载、备份、失败目录是否存在或允许创建 |
| 本地目录  | 是否使用服务器绝对路径                       |
| 调度开关  | 生产环境是否只开启需要的任务                 |
| Cron 周期 | 是否避免过高频率扫描                         |
| 敏感信息  | 密码是否通过环境变量或密钥管理注入           |

### 目录权限检查

目录权限检查用于确认应用运行用户对本地 FTP 工作目录具备必要权限。上传、下载、临时文件、备份文件和失败文件都依赖本地文件系统，如果目录权限不足，会导致下载失败、上传失败或文件后处理失败。

创建目录：

```bash
sudo mkdir -p /data/app/ftp/upload
sudo mkdir -p /data/app/ftp/download
sudo mkdir -p /data/app/ftp/temp
sudo mkdir -p /data/app/ftp/backup
sudo mkdir -p /data/app/ftp/error
```

调整目录归属，假设应用运行用户为 `appuser`：

```bash
sudo chown -R appuser:appuser /data/app/ftp
sudo chmod -R 750 /data/app/ftp
```

验证写入权限：

```bash
sudo -u appuser touch /data/app/ftp/upload/permission-test.txt
sudo -u appuser touch /data/app/ftp/download/permission-test.txt
sudo -u appuser rm -f /data/app/ftp/upload/permission-test.txt
sudo -u appuser rm -f /data/app/ftp/download/permission-test.txt
```

目录权限建议如下：

| 目录                     | 权限建议 | 说明                   |
| ------------------------ | -------- | ---------------------- |
| `/data/app/ftp/upload`   | 读写     | 本地待上传文件目录     |
| `/data/app/ftp/download` | 读写     | 下载后的正式文件目录   |
| `/data/app/ftp/temp`     | 读写     | 上传或下载临时文件目录 |
| `/data/app/ftp/backup`   | 读写     | 成功处理后的备份目录   |
| `/data/app/ftp/error`    | 读写     | 失败文件目录           |
| `logs`                   | 写       | 应用日志目录           |

如果使用 Docker 或 Kubernetes 部署，需要确认挂载卷权限与容器内运行用户一致。尤其是非 root 用户运行容器时，宿主机目录权限不足会导致文件创建失败。

### 服务启动验证

服务启动验证用于确认应用可以正常读取配置、连接 FTP 服务、启动定时任务并提供接口服务。建议按“配置检查、网络检查、启动检查、接口检查、日志检查”的顺序执行。

打包应用：

```bash
mvn clean package -DskipTests
```

启动应用：

```bash
java -jar target/spring-integration-ftp-demo-1.0.0.jar \
  --spring.profiles.active=prod
```

后台启动示例：

```bash
nohup java -jar target/spring-integration-ftp-demo-1.0.0.jar \
  --spring.profiles.active=prod \
  > logs/startup.log 2>&1 &
```

检查进程和端口：

```bash
ps -ef | grep spring-integration-ftp-demo | grep -v grep
netstat -tunlp | grep 8080
```

检查日志：

```bash
tail -f logs/spring-integration-ftp-demo.log
```

检查应用健康状态：

```bash
curl "http://localhost:8080/actuator/health"
```

检查 FTP 文件列表接口：

```bash
curl "http://localhost:8080/api/ftp/files/list?remoteDirectory=/download"
```

启动验证检查项如下：

| 检查项   | 预期结果                                     |
| -------- | -------------------------------------------- |
| 应用进程 | 进程正常存在                                 |
| 服务端口 | `8080` 正常监听                              |
| 健康检查 | `/actuator/health` 返回 `UP`                 |
| 配置加载 | 日志中没有配置绑定失败                       |
| FTP 连接 | 文件列表查询接口可正常返回                   |
| 定时任务 | 开启后能按 Cron 输出执行日志                 |
| 本地目录 | 上传、下载、临时目录均可读写                 |
| 异常日志 | 启动阶段无连接失败、权限失败和 Bean 创建失败 |

常见部署问题与处理方式如下：

| 问题                 | 可能原因                                            | 处理方式                        |
| -------------------- | --------------------------------------------------- | ------------------------------- |
| 应用启动失败         | 配置缺失、端口占用、Bean 注入失败                   | 检查启动日志和配置项            |
| FTP 登录失败         | 账号密码错误、账号被禁用                            | 联系 FTP 服务方确认账号         |
| 可以登录但不能列目录 | 被动端口未开放、目录权限不足                        | 检查被动模式端口和目录权限      |
| 上传失败             | 远程目录无写权限、本地文件不存在                    | 检查远程目录权限和本地文件路径  |
| 下载失败             | 本地目录无写权限、远程文件不存在                    | 检查本地目录权限和远程文件路径  |
| 定时任务不执行       | 开关未启用、Cron 配置错误、未加 `@EnableScheduling` | 检查调度配置和启动类注解        |
| 文件重复处理         | 幂等记录未落库、内存记录重启丢失                    | 使用数据库或 Redis 存储处理记录 |

生产环境上线前，建议至少完成一次完整链路验证：本地生成文件、上传到 FTP、远程确认文件存在、从 FTP 下载文件、本地确认文件内容、查看操作日志和传输记录。
