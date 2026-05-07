# Hadoop HDFS 开发

## 项目概述

本项目基于 Spring Boot 3 对 Hadoop HDFS 文件系统进行服务化封装，向业务系统提供统一的文件上传、下载、删除、重命名、目录创建、文件列表查询和文件状态查询能力。Spring Boot 3.5.x 官方要求至少 Java 17，Maven 版本要求 3.6.3 及以上，因此本项目默认以 JDK 17、Spring Boot 3.5.x、Maven 3.9.x 作为开发基线。([Home](https://docs.spring.io/spring-boot/3.5/system-requirements.html))

### 功能定位

本模块的核心定位是将 Hadoop HDFS 的底层 Java API 封装为 Spring Boot 后端服务，屏蔽业务系统直接操作 `FileSystem`、`Path`、`FSDataInputStream`、`FSDataOutputStream` 等底层对象的复杂度。

在系统架构中，该模块通常作为“分布式文件存储访问层”存在。业务系统不直接依赖 Hadoop 客户端细节，而是通过统一的 Service 或 REST API 完成文件操作。这样可以降低业务代码与 HDFS 的耦合度，便于后续替换存储实现，例如从 HDFS 扩展到 MinIO、S3、OSS 或其他对象存储。

Hadoop 官方 `FileSystem` API 是面向通用文件系统抽象设计的，HDFS 场景下常用它完成文件创建、读取、删除、状态查询、本地文件上传和下载等操作；官方文档也说明，面向 HDFS 的用户代码通常应通过 `FileSystem` 或 `FileContext` 访问文件系统。([Apache Hadoop](https://hadoop.apache.org/docs/current/api/org/apache/hadoop/fs/FileSystem.html))

模块主要提供以下能力：

| 能力         | 说明                                                       |
| ------------ | ---------------------------------------------------------- |
| 文件上传     | 将本地文件、接口上传文件或业务生成文件写入 HDFS 指定路径   |
| 文件下载     | 从 HDFS 读取文件并返回给前端、网关或其他业务服务           |
| 文件删除     | 删除 HDFS 文件或目录，可根据业务要求控制是否递归删除       |
| 文件重命名   | 对 HDFS 文件或目录进行移动、重命名                         |
| 目录管理     | 创建目录、判断目录是否存在、查询目录下文件列表             |
| 状态查询     | 查询文件大小、路径类型、权限、修改时间、副本数等元信息     |
| 统一异常处理 | 将 HDFS 连接异常、权限异常、路径不存在等异常转换为业务响应 |

该模块不负责 HDFS 集群安装、NameNode 高可用部署、DataNode 扩容、Hadoop 安全体系建设等运维工作，只负责 Spring Boot 应用侧的 HDFS 访问与接口封装。

### 应用场景

HDFS 适合存储大规模、批量化、吞吐优先的文件数据。它更适合大文件、日志文件、离线计算数据、归档文件和数据湖底层存储，不适合作为高频小文件、强事务或低延迟随机读写系统。

常见使用场景如下：

| 场景         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 日志归档     | 将业务日志、访问日志、审计日志按日期写入 HDFS，用于后续离线分析 |
| 数据采集落盘 | 接收接口、MQ、Flume、Logstash 等来源的数据文件，统一落入 HDFS |
| 离线计算输入 | 为 Hive、Spark、Flink Batch、MapReduce 等计算任务提供输入数据 |
| 报表结果存储 | 将离线任务生成的 CSV、Parquet、JSON、文本文件写入 HDFS       |
| 大文件管理   | 存储体积较大的压缩包、数据包、导入导出文件                   |
| 数据中转区   | 作为业务系统与大数据平台之间的文件交换区                     |

在 Spring Boot 项目中，典型调用链如下：

```text
前端 / 第三方系统
        |
        v
Spring Boot Controller
        |
        v
HdfsService
        |
        v
Hadoop FileSystem API
        |
        v
HDFS NameNode / DataNode
```

该调用链的重点是：Controller 只处理 HTTP 请求与响应，Service 负责编排业务逻辑，Hadoop `FileSystem` 只在基础设施层或客户端封装类中使用，避免底层 API 散落在多个业务类中。

### 技术选型

本项目采用 Spring Boot 3 + Hadoop Client 的方式完成 HDFS 集成。Spring Boot 负责 Web 接口、配置管理、Bean 生命周期、异常处理和参数校验；Hadoop Client 负责连接 HDFS 并执行具体文件操作。

建议技术栈如下：

| 技术                        | 建议版本                  | 作用                                                         |
| --------------------------- | ------------------------- | ------------------------------------------------------------ |
| JDK                         | 17 或 21                  | Spring Boot 3 运行基础，推荐 JDK 17 作为兼容基线             |
| Spring Boot                 | 3.5.x                     | Web 服务、配置绑定、依赖管理、统一异常处理                   |
| Maven                       | 3.9.x                     | 项目构建与依赖管理，最低需满足 Spring Boot 3.5.x 的 Maven 3.6.3+ 要求 |
| Hadoop Client               | 3.3.x / 3.4.x             | HDFS Java 客户端访问能力                                     |
| Lombok                      | 1.18.x                    | 简化 DTO、配置类、日志对象等代码                             |
| Hutool                      | 5.8.x                     | 文件名、字符串、路径、IO 等工具处理                          |
| Spring Validation           | Spring Boot 管理版本      | 接口入参校验                                                 |
| Knife4j / Springdoc OpenAPI | 适配 Spring Boot 3 的版本 | 接口文档，可选                                               |

Hadoop 版本应优先与实际 HDFS 集群版本保持一致。Apache Hadoop 3.4.1 属于 Hadoop 3.4.x 分支，官方文档标识其为 Hadoop 3.4.x release branch 的更新版本；如果集群仍使用 Hadoop 3.3.x，应用侧客户端依赖也应优先选择 3.3.x，避免客户端与服务端协议、依赖传递和安全认证行为出现不一致。([Apache Hadoop](https://hadoop.apache.org/docs/r3.4.1/index.html))

技术选型原则如下：

| 选型点            | 推荐做法                                                     |
| ----------------- | ------------------------------------------------------------ |
| Spring Boot 版本  | 使用 Spring Boot 3.x，默认 JDK 17 起步                       |
| Hadoop 客户端版本 | 与 HDFS 集群主版本一致，至少保持 Hadoop 3.x 内部兼容         |
| 配置方式          | 使用 `application.yml` 管理 HDFS 地址、用户、根目录、超时时间等参数 |
| 客户端封装        | 将 `FileSystem` 初始化和关闭逻辑封装为独立配置类或客户端类   |
| 接口设计          | Controller 不直接操作 Hadoop API，只调用业务 Service         |
| 路径规范          | 所有业务路径统一挂载到配置的 HDFS 根目录下，避免任意路径访问 |
| 异常处理          | 对连接失败、文件不存在、权限不足、路径非法等场景做统一响应   |

## 环境准备

环境准备的目标是保证本地 Spring Boot 应用能够正常编译、启动，并且可以连接目标 HDFS 集群执行基本文件操作。环境准备分为三部分：JDK 与 Spring Boot 版本、Hadoop 与 HDFS 环境、本地开发环境配置。

### JDK 与 Spring Boot 版本

Spring Boot 3.5.14 官方要求至少 Java 17，并兼容到 Java 25；同时要求 Maven 3.6.3 或更高版本，Gradle 则支持 7.x 或 8.x 中的指定版本范围。为了降低团队环境差异，本文推荐统一使用 JDK 17 + Maven 3.9.x + Spring Boot 3.5.x。([Home](https://docs.spring.io/spring-boot/3.5/system-requirements.html))

推荐版本如下：

| 环境项      | 推荐值                        | 说明                                      |
| ----------- | ----------------------------- | ----------------------------------------- |
| JDK         | 17                            | Spring Boot 3 基线版本，兼容性稳定        |
| Spring Boot | 3.5.x                         | 当前 Spring Boot 3 稳定分支之一           |
| Maven       | 3.9.x                         | 满足 Spring Boot 3.5.x Maven 3.6.3+ 要求  |
| 编码        | UTF-8                         | 避免文件名、接口响应、日志中文乱码        |
| 操作系统    | Linux / macOS / Windows + WSL | 生产环境通常为 Linux，本地可使用 WSL 模拟 |

检查本地 JDK 和 Maven 环境：

```bash
# 查看 JDK 版本，建议输出 17.x
java -version

# 查看 Maven 版本，建议使用 3.9.x
mvn -version

# 查看 JAVA_HOME 是否正确
echo $JAVA_HOME
```

如果使用 Windows 开发，建议优先使用 WSL2 或 Docker Desktop 提供类 Linux 环境。Hadoop 客户端在 Windows 原生环境下可能涉及 `winutils.exe`、本地权限模型、路径分隔符等额外问题，开发调试成本高于 Linux 或 WSL。

### Hadoop 与 HDFS 环境

本项目要求存在可访问的 HDFS 环境。HDFS 可以是公司已有测试集群、开发环境集群，也可以是本地伪分布式 Hadoop 环境。对于 Spring Boot 应用来说，最关键的是能够访问 NameNode 地址，并且当前访问用户具备目标目录的读写权限。

HDFS 环境至少需要确认以下信息：

| 配置项        | 示例                         | 说明                                              |
| ------------- | ---------------------------- | ------------------------------------------------- |
| NameNode 地址 | `hdfs://192.168.1.10:8020`   | Spring Boot 应用连接 HDFS 的入口地址              |
| HDFS 用户     | `hdfs` / `ateng` / `bigdata` | 应用访问 HDFS 使用的用户身份                      |
| 业务根目录    | `/data/app/hdfs-demo`        | 项目所有文件操作的根路径                          |
| 读写权限      | `rwx`                        | 应用用户需要对业务根目录具备读写权限              |
| Hadoop 版本   | `3.3.x` / `3.4.x`            | 应与项目 Hadoop Client 依赖版本尽量一致           |
| 安全认证      | simple / Kerberos            | 本文基础示例默认 simple 模式，Kerberos 需额外配置 |

可以在 Hadoop 节点上使用以下命令确认 HDFS 是否正常：

```bash
# 查看 HDFS 根目录
hdfs dfs -ls /

# 创建项目业务目录
hdfs dfs -mkdir -p /data/app/hdfs-demo

# 给测试用户授权，按实际用户替换 ateng
hdfs dfs -chown -R ateng:ateng /data/app/hdfs-demo

# 验证目录权限
hdfs dfs -ls /data/app

# 写入一个测试文件
echo "hello hdfs" > /tmp/hdfs-test.txt
hdfs dfs -put -f /tmp/hdfs-test.txt /data/app/hdfs-demo/

# 查看测试文件
hdfs dfs -cat /data/app/hdfs-demo/hdfs-test.txt
```

上述命令中，`hdfs dfs -mkdir -p` 用于递归创建 HDFS 目录，`hdfs dfs -chown` 用于调整目录所属用户，`hdfs dfs -put -f` 用于将本地文件覆盖上传到 HDFS，`hdfs dfs -cat` 用于读取 HDFS 文件内容。后续 Spring Boot 接口测试时，可以使用这些命令交叉验证接口结果是否真实写入 HDFS。

如果本地没有现成 HDFS 集群，可以使用以下两种方式准备开发环境：

| 方式                | 适用场景             | 说明                                           |
| ------------------- | -------------------- | ---------------------------------------------- |
| 连接开发集群        | 团队已有 Hadoop 环境 | 最接近真实环境，推荐优先使用                   |
| 本地伪分布式 Hadoop | 个人学习、离线开发   | 需要本地安装 Hadoop，配置 NameNode 和 DataNode |
| Docker Hadoop 环境  | 快速验证 Demo        | 启动快，但镜像版本和网络配置需要统一           |

开发文档中的示例默认使用如下 HDFS 参数：

```yaml
# HDFS 开发环境连接参数示例，后续依赖与配置章节可直接沿用
hdfs:
  # HDFS NameNode 地址，按实际集群修改
  uri: hdfs://192.168.1.10:8020
  # HDFS 访问用户，需具备业务根目录读写权限
  user: ateng
  # 项目文件操作根目录，业务代码中不建议允许访问根目录之外的路径
  base-path: /data/app/hdfs-demo
```

### 本地开发环境配置

本地开发环境主要用于完成代码编写、接口调试和 HDFS 连通性验证。建议本地环境与测试环境保持同一套配置键，只通过 Spring Profile 区分不同环境的值。

推荐本地项目结构如下：

```text
springboot3-hdfs-demo
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── io/github/atengk
│   │   │       └── hdfs
│   │   │           ├── HdfsApplication.java
│   │   │           ├── config
│   │   │           ├── controller
│   │   │           ├── service
│   │   │           └── common
│   │   └── resources
│   │       ├── application.yml
│   │       └── application-dev.yml
│   └── test
│       └── java
└── README.md
```

本地开发建议使用 `dev` 环境启动，并将 HDFS 连接参数放在 `application-dev.yml` 中。后续进入“依赖与配置”章节时，可以继续补充 Maven 依赖、配置属性类和 Hadoop `Configuration` 初始化逻辑。

本地开发配置示例：

```yaml
# 文件位置：src/main/resources/application.yml
server:
  # 本地开发服务端口
  port: 8080

spring:
  application:
    # 应用名称，用于日志、链路追踪和服务识别
    name: springboot3-hdfs-demo
  profiles:
    # 默认启用开发环境配置
    active: dev

logging:
  level:
    # 项目包日志级别，开发阶段可使用 debug
    io.github.atengk: debug
# 文件位置：src/main/resources/application-dev.yml
hdfs:
  # 开发环境 HDFS NameNode 地址
  uri: hdfs://192.168.1.10:8020
  # 开发环境访问用户
  user: ateng
  # 开发环境业务根目录
  base-path: /data/app/hdfs-demo
  # 是否启用连接测试，启动时可验证 HDFS 是否可访问
  check-enabled: true
```

启动前先确认本机网络能够访问 NameNode 端口：

```bash
# 检查 NameNode 端口是否可访问，按实际 IP 和端口替换
nc -vz 192.168.1.10 8020

# 如果系统没有 nc，也可以使用 telnet
telnet 192.168.1.10 8020
```

如果使用 Maven 启动项目，可以执行：

```bash
# 清理并编译项目
mvn clean package -DskipTests

# 使用 dev 环境启动 Spring Boot 应用
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

本地调试时需要重点检查以下问题：

| 检查项            | 处理方式                                                     |
| ----------------- | ------------------------------------------------------------ |
| JDK 版本不匹配    | 确认 `java -version` 输出为 17 或更高版本                    |
| Maven 版本过低    | 升级到 Maven 3.6.3 以上，推荐 3.9.x                          |
| NameNode 无法连接 | 检查网络、防火墙、端口、HDFS 地址是否正确                    |
| HDFS 用户无权限   | 使用 `hdfs dfs -chown` 或 `hdfs dfs -chmod` 调整业务目录权限 |
| 路径不存在        | 启动前创建 `/data/app/hdfs-demo` 等业务根目录                |
| Windows 本地异常  | 优先切换到 WSL2、Linux 虚拟机或 Docker 环境                  |

本地环境准备完成后，应至少满足三个条件：Spring Boot 应用可以正常启动，应用服务器可以访问 NameNode 端口，配置的 HDFS 用户可以在业务根目录下创建、读取和删除文件。后续章节即可在此基础上继续编写 Maven 依赖、Hadoop `Configuration` 初始化、HDFS 客户端封装和文件操作接口。



## 依赖与配置

本节用于完成 Spring Boot 项目访问 HDFS 的基础依赖、连接参数和 Hadoop `Configuration` 初始化。后续所有 HDFS 文件操作都基于这里初始化出来的 `FileSystem` 对象完成。

### Maven 依赖配置

Spring Boot 3 项目建议使用 `spring-boot-starter-parent` 管理 Spring 生态依赖版本。示例使用 Spring Boot `3.5.6`，该版本已发布到 Maven Central；Hadoop 客户端示例使用 `hadoop-client-api` 和 `hadoop-client-runtime`，Hadoop 3.4.3 的相关客户端包也已在 Maven Central 发布。Hutool 示例使用 `5.8.38`，用于简化字符串、路径、文件和 IO 操作。([Maven Repository](https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-parent/3.5.6/?utm_source=chatgpt.com))

文件位置：`pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot 3 父工程，统一管理 Spring 相关依赖版本 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.6</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>springboot3-hdfs-demo</artifactId>
    <version>1.0.0</version>
    <name>springboot3-hdfs-demo</name>
    <description>Spring Boot 3 集成 Hadoop HDFS 示例项目</description>

    <properties>
        <!-- Spring Boot 3 建议以 JDK 17 作为基础版本 -->
        <java.version>17</java.version>

        <!-- Hadoop 客户端版本建议与实际 HDFS 集群版本保持一致 -->
        <hadoop.version>3.4.3</hadoop.version>

        <!-- Hutool 工具类版本 -->
        <hutool.version>5.8.38</hutool.version>
    </properties>

    <dependencies>
        <!-- Web 接口开发依赖，包含 Spring MVC、Tomcat、Jackson 等 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- 参数校验依赖，用于配置项和接口入参校验 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Hadoop 客户端 API，提供 FileSystem、Path、FileStatus 等核心类 -->
        <dependency>
            <groupId>org.apache.hadoop</groupId>
            <artifactId>hadoop-client-api</artifactId>
            <version>${hadoop.version}</version>
        </dependency>

        <!-- Hadoop 客户端运行时依赖，提供 HDFS 访问所需运行环境 -->
        <dependency>
            <groupId>org.apache.hadoop</groupId>
            <artifactId>hadoop-client-runtime</artifactId>
            <version>${hadoop.version}</version>
        </dependency>

        <!-- Hutool 工具类，用于字符串、文件、IO 等常用处理 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok，减少 Getter、Setter、构造器、日志对象等样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 配置元数据生成器，用于 IDE 提示 application.yml 配置项 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 单元测试依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot 打包插件 -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <!-- 打包时排除 Lombok，避免进入最终运行包 -->
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
```

如果实际 Hadoop 集群版本是 `3.3.x`，建议将 `<hadoop.version>` 改成集群对应版本，例如 `3.3.6`。Hadoop 客户端通常可以跨小版本访问，但生产项目不建议随意使用明显高于集群版本的客户端依赖，避免协议、认证、依赖传递和运行时行为差异。

### HDFS 连接参数配置

HDFS 连接参数应统一放在 `application.yml` 或环境专用配置文件中，避免硬编码到 Java 类。常用配置包括 NameNode 地址、访问用户、业务根目录、IO 缓冲区大小、启动时连通性检查和 DataNode 主机名访问策略。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 服务端口
  port: 8080

spring:
  application:
    # 应用名称
    name: springboot3-hdfs-demo
  profiles:
    # 默认启用开发环境
    active: dev

logging:
  level:
    # 项目包日志级别
    io.github.atengk: debug
```

文件位置：`src/main/resources/application-dev.yml`

```yaml
hdfs:
  # HDFS NameNode 地址，按实际集群修改
  uri: hdfs://192.168.1.10:8020

  # HDFS 访问用户，需要具备 base-path 的读写权限
  user: ateng

  # 业务根目录，后续所有文件操作都限制在该目录下
  base-path: /data/app/hdfs-demo

  # IO 缓冲区大小，上传下载文件时使用
  buffer-size: 8192

  # 是否在项目启动时检查 HDFS 连接并自动创建业务根目录
  check-enabled: true

  # 是否使用 DataNode 主机名访问；Docker、跨网段、容器网络场景可能需要开启
  use-datanode-hostname: false
```

配置项说明如下：

| 配置项                       | 说明                         | 示例                       |
| ---------------------------- | ---------------------------- | -------------------------- |
| `hdfs.uri`                   | HDFS NameNode 地址           | `hdfs://192.168.1.10:8020` |
| `hdfs.user`                  | HDFS 访问用户                | `ateng`                    |
| `hdfs.base-path`             | 业务根目录                   | `/data/app/hdfs-demo`      |
| `hdfs.buffer-size`           | 文件读写缓冲区大小           | `8192`                     |
| `hdfs.check-enabled`         | 启动时是否检查连接           | `true`                     |
| `hdfs.use-datanode-hostname` | 是否通过 DataNode 主机名访问 | `false`                    |

这里建议将所有业务文件路径都设计为相对路径，例如 `upload/2026/05/test.txt`。应用内部再将它拼接到 `hdfs.base-path` 下，避免外部请求直接操作 HDFS 任意目录。

### Hadoop Configuration 初始化

Hadoop `Configuration` 用于设置 HDFS 的连接地址、客户端参数和底层文件系统行为。HDFS 操作最终通过 `FileSystem` 完成，Hadoop 官方 `FileSystem` API 提供了 `get`、`create`、`open`、`delete`、`rename`、`listStatus`、`getFileStatus` 等方法，用于创建客户端、读写文件、删除文件、重命名、查询列表和获取状态。([hadoop.apache.org](https://hadoop.apache.org/docs/r3.4.3/api/org/apache/hadoop/fs/FileSystem.html?utm_source=chatgpt.com))

本节先创建配置属性类，再创建 Hadoop 初始化配置类。

文件位置：`src/main/java/io/github/atengk/hdfs/config/HdfsProperties.java`

```java
package io.github.atengk.hdfs.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * HDFS 配置属性
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
@Validated
@ConfigurationProperties(prefix = "hdfs")
public class HdfsProperties {

    /**
     * HDFS NameNode 地址，例如 hdfs://192.168.1.10:8020
     */
    @NotBlank(message = "HDFS地址不能为空")
    private String uri;

    /**
     * HDFS 访问用户
     */
    @NotBlank(message = "HDFS访问用户不能为空")
    private String user;

    /**
     * HDFS 业务根目录
     */
    @NotBlank(message = "HDFS业务根目录不能为空")
    private String basePath;

    /**
     * IO 缓冲区大小
     */
    @Min(value = 1024, message = "HDFS缓冲区大小不能小于1024")
    private int bufferSize = 8192;

    /**
     * 是否启动时检查 HDFS 连接
     */
    private boolean checkEnabled = true;

    /**
     * 是否使用 DataNode 主机名访问
     */
    private boolean useDatanodeHostname = false;

}
```

文件位置：`src/main/java/io/github/atengk/hdfs/config/HdfsAutoConfiguration.java`

```java
package io.github.atengk.hdfs.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URI;

/**
 * HDFS 自动配置
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(HdfsProperties.class)
public class HdfsAutoConfiguration {

    private final HdfsProperties hdfsProperties;

    /**
     * 初始化 Hadoop Configuration
     *
     * @return Hadoop 配置对象
     */
    @Bean
    public org.apache.hadoop.conf.Configuration hadoopConfiguration() {
        org.apache.hadoop.conf.Configuration configuration = new org.apache.hadoop.conf.Configuration();

        // 设置默认 HDFS 文件系统地址
        configuration.set("fs.defaultFS", hdfsProperties.getUri());

        // 设置 IO 缓冲区大小
        configuration.setInt("io.file.buffer.size", hdfsProperties.getBufferSize());

        // 容器化、跨网段或 DNS 特殊场景下可能需要开启
        configuration.setBoolean("dfs.client.use.datanode.hostname", hdfsProperties.isUseDatanodeHostname());

        log.info("初始化Hadoop配置完成，uri={}, user={}, basePath={}",
                hdfsProperties.getUri(), hdfsProperties.getUser(), hdfsProperties.getBasePath());
        return configuration;
    }

    /**
     * 初始化 HDFS FileSystem 客户端
     *
     * @param hadoopConfiguration Hadoop 配置对象
     * @return HDFS 文件系统客户端
     * @throws IOException          IO 异常
     * @throws InterruptedException 线程中断异常
     */
    @Bean(destroyMethod = "close")
    public FileSystem hdfsFileSystem(org.apache.hadoop.conf.Configuration hadoopConfiguration)
            throws IOException, InterruptedException {
        URI uri = URI.create(hdfsProperties.getUri());
        FileSystem fileSystem = FileSystem.get(uri, hadoopConfiguration, hdfsProperties.getUser());

        if (hdfsProperties.isCheckEnabled()) {
            Path basePath = new Path(hdfsProperties.getBasePath());
            if (!fileSystem.exists(basePath)) {
                boolean created = fileSystem.mkdirs(basePath);
                log.info("HDFS业务根目录不存在，自动创建结果：path={}, created={}", basePath, created);
            }
            log.info("HDFS连接检查完成，uri={}, basePath={}", hdfsProperties.getUri(), hdfsProperties.getBasePath());
        }

        return fileSystem;
    }

}
```

该配置类启动时会创建一个 Spring Bean 级别的 `FileSystem` 客户端，并在应用关闭时自动执行 `close`。如果 `check-enabled=true`，应用启动时会检查业务根目录是否存在；不存在时自动创建，便于开发环境快速启动。

## 核心功能开发

核心功能开发的目标是将 HDFS 原生 API 封装成项目内部可复用的客户端组件。Controller、Service 或定时任务不应直接操作 Hadoop `FileSystem`，而应统一调用封装后的 `HdfsClient`。

本节核心文件如下：

```text
src/main/java/io/github/atengk/hdfs
├── client
│   └── HdfsClient.java
├── config
│   ├── HdfsAutoConfiguration.java
│   └── HdfsProperties.java
└── model
    └── HdfsFileInfo.java
```

### HDFS 客户端封装

HDFS 客户端封装类负责路径安全处理、目录自动创建、文件上传、文件下载、删除、重命名、列表查询和状态查询。业务层只需要传入相对业务路径，不需要关心 `Path`、`FileStatus`、`FSDataInputStream` 等 Hadoop 细节。

文件位置：`src/main/java/io/github/atengk/hdfs/model/HdfsFileInfo.java`

```java
package io.github.atengk.hdfs.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * HDFS 文件信息
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
@Builder
public class HdfsFileInfo {

    /**
     * 业务相对路径
     */
    private String path;

    /**
     * 文件或目录名称
     */
    private String name;

    /**
     * 是否为目录
     */
    private Boolean directory;

    /**
     * 文件大小，目录通常为 0
     */
    private Long length;

    /**
     * 文件副本数
     */
    private Short replication;

    /**
     * 块大小
     */
    private Long blockSize;

    /**
     * 文件所属用户
     */
    private String owner;

    /**
     * 文件所属用户组
     */
    private String group;

    /**
     * 文件权限
     */
    private String permission;

    /**
     * 最后修改时间
     */
    private LocalDateTime modificationTime;

    /**
     * 最后访问时间
     */
    private LocalDateTime accessTime;

}
```

下面的客户端封装了本节所有核心文件操作，后续接口层可以直接注入该组件使用。

文件位置：`src/main/java/io/github/atengk/hdfs/client/HdfsClient.java`

```java
package io.github.atengk.hdfs.client;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.hdfs.config.HdfsProperties;
import io.github.atengk.hdfs.model.HdfsFileInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

/**
 * HDFS 文件客户端
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HdfsClient {

    private final FileSystem fileSystem;
    private final HdfsProperties hdfsProperties;

    /**
     * 上传输入流到 HDFS
     *
     * @param inputStream  文件输入流
     * @param targetPath   目标业务相对路径
     * @param overwrite    是否覆盖已存在文件
     */
    public void upload(InputStream inputStream, String targetPath, boolean overwrite) {
        Assert.notNull(inputStream, "文件输入流不能为空");
        Assert.notBlank(targetPath, "目标路径不能为空");

        Path hdfsPath = resolvePath(targetPath);
        try {
            Path parent = hdfsPath.getParent();
            if (parent != null && !fileSystem.exists(parent)) {
                boolean created = fileSystem.mkdirs(parent);
                log.info("HDFS父目录不存在，自动创建结果：path={}, created={}", parent, created);
            }

            if (fileSystem.exists(hdfsPath) && !overwrite) {
                throw new IllegalStateException("HDFS文件已存在：" + targetPath);
            }

            try (FSDataOutputStream outputStream = fileSystem.create(hdfsPath, overwrite)) {
                IoUtil.copy(inputStream, outputStream, hdfsProperties.getBufferSize());
            }

            log.info("HDFS文件上传成功：targetPath={}, overwrite={}", targetPath, overwrite);
        } catch (Exception e) {
            log.error("HDFS文件上传失败：targetPath={}", targetPath, e);
            throw new IllegalStateException("HDFS文件上传失败：" + e.getMessage(), e);
        } finally {
            IoUtil.close(inputStream);
        }
    }

    /**
     * 上传本地文件到 HDFS
     *
     * @param localFilePath 本地文件路径
     * @param targetPath    目标业务相对路径
     * @param overwrite     是否覆盖已存在文件
     */
    public void uploadLocalFile(String localFilePath, String targetPath, boolean overwrite) {
        Assert.notBlank(localFilePath, "本地文件路径不能为空");

        File file = FileUtil.file(localFilePath);
        if (!FileUtil.exist(file) || !FileUtil.isFile(file)) {
            throw new IllegalArgumentException("本地文件不存在：" + localFilePath);
        }

        upload(FileUtil.getInputStream(file), targetPath, overwrite);
    }

    /**
     * 下载 HDFS 文件到输出流
     *
     * @param sourcePath   源业务相对路径
     * @param outputStream 输出流
     */
    public void download(String sourcePath, OutputStream outputStream) {
        Assert.notBlank(sourcePath, "源路径不能为空");
        Assert.notNull(outputStream, "输出流不能为空");

        Path hdfsPath = resolvePath(sourcePath);
        try {
            if (!fileSystem.exists(hdfsPath)) {
                throw new IllegalArgumentException("HDFS文件不存在：" + sourcePath);
            }

            try (FSDataInputStream inputStream = fileSystem.open(hdfsPath)) {
                IoUtil.copy(inputStream, outputStream, hdfsProperties.getBufferSize());
                outputStream.flush();
            }

            log.info("HDFS文件下载成功：sourcePath={}", sourcePath);
        } catch (Exception e) {
            log.error("HDFS文件下载失败：sourcePath={}", sourcePath, e);
            throw new IllegalStateException("HDFS文件下载失败：" + e.getMessage(), e);
        }
    }

    /**
     * 下载 HDFS 文件到本地
     *
     * @param sourcePath    源业务相对路径
     * @param localFilePath 本地文件路径
     */
    public void downloadToLocalFile(String sourcePath, String localFilePath) {
        Assert.notBlank(localFilePath, "本地文件路径不能为空");

        File localFile = FileUtil.file(localFilePath);
        FileUtil.mkParentDirs(localFile);

        try (OutputStream outputStream = FileUtil.getOutputStream(localFile)) {
            download(sourcePath, outputStream);
            log.info("HDFS文件下载到本地成功：sourcePath={}, localFilePath={}", sourcePath, localFilePath);
        } catch (Exception e) {
            log.error("HDFS文件下载到本地失败：sourcePath={}, localFilePath={}", sourcePath, localFilePath, e);
            throw new IllegalStateException("HDFS文件下载到本地失败：" + e.getMessage(), e);
        }
    }

    /**
     * 删除 HDFS 文件或目录
     *
     * @param path      业务相对路径
     * @param recursive 是否递归删除
     * @return 是否删除成功
     */
    public boolean delete(String path, boolean recursive) {
        Assert.notBlank(path, "删除路径不能为空");

        Path hdfsPath = resolvePath(path);
        try {
            if (!fileSystem.exists(hdfsPath)) {
                log.info("HDFS删除路径不存在，跳过删除：path={}", path);
                return false;
            }

            boolean deleted = fileSystem.delete(hdfsPath, recursive);
            log.info("HDFS删除完成：path={}, recursive={}, deleted={}", path, recursive, deleted);
            return deleted;
        } catch (Exception e) {
            log.error("HDFS删除失败：path={}, recursive={}", path, recursive, e);
            throw new IllegalStateException("HDFS删除失败：" + e.getMessage(), e);
        }
    }

    /**
     * 重命名或移动 HDFS 文件
     *
     * @param sourcePath 源业务相对路径
     * @param targetPath 目标业务相对路径
     * @return 是否重命名成功
     */
    public boolean rename(String sourcePath, String targetPath) {
        Assert.notBlank(sourcePath, "源路径不能为空");
        Assert.notBlank(targetPath, "目标路径不能为空");

        Path source = resolvePath(sourcePath);
        Path target = resolvePath(targetPath);

        try {
            if (!fileSystem.exists(source)) {
                throw new IllegalArgumentException("HDFS源路径不存在：" + sourcePath);
            }

            Path targetParent = target.getParent();
            if (targetParent != null && !fileSystem.exists(targetParent)) {
                boolean created = fileSystem.mkdirs(targetParent);
                log.info("HDFS目标父目录不存在，自动创建结果：path={}, created={}", targetParent, created);
            }

            boolean renamed = fileSystem.rename(source, target);
            log.info("HDFS重命名完成：sourcePath={}, targetPath={}, renamed={}", sourcePath, targetPath, renamed);
            return renamed;
        } catch (Exception e) {
            log.error("HDFS重命名失败：sourcePath={}, targetPath={}", sourcePath, targetPath, e);
            throw new IllegalStateException("HDFS重命名失败：" + e.getMessage(), e);
        }
    }

    /**
     * 创建 HDFS 目录
     *
     * @param directoryPath 目录业务相对路径
     * @return 是否创建成功
     */
    public boolean mkdirs(String directoryPath) {
        Assert.notBlank(directoryPath, "目录路径不能为空");

        Path hdfsPath = resolvePath(directoryPath);
        try {
            if (fileSystem.exists(hdfsPath)) {
                log.info("HDFS目录已存在：directoryPath={}", directoryPath);
                return true;
            }

            boolean created = fileSystem.mkdirs(hdfsPath);
            log.info("HDFS目录创建完成：directoryPath={}, created={}", directoryPath, created);
            return created;
        } catch (Exception e) {
            log.error("HDFS目录创建失败：directoryPath={}", directoryPath, e);
            throw new IllegalStateException("HDFS目录创建失败：" + e.getMessage(), e);
        }
    }

    /**
     * 查询 HDFS 文件列表
     *
     * @param directoryPath 目录业务相对路径
     * @return 文件信息列表
     */
    public List<HdfsFileInfo> list(String directoryPath) {
        String safePath = StrUtil.blankToDefault(directoryPath, "/");
        Path hdfsPath = resolvePath(safePath);

        try {
            if (!fileSystem.exists(hdfsPath)) {
                log.info("HDFS列表查询目录不存在：directoryPath={}", safePath);
                return CollUtil.newArrayList();
            }

            FileStatus[] statuses = fileSystem.listStatus(hdfsPath);
            if (statuses == null || statuses.length == 0) {
                return CollUtil.newArrayList();
            }

            List<HdfsFileInfo> list = Arrays.stream(statuses)
                    .map(this::toFileInfo)
                    .toList();

            log.info("HDFS列表查询完成：directoryPath={}, count={}", safePath, list.size());
            return list;
        } catch (Exception e) {
            log.error("HDFS列表查询失败：directoryPath={}", safePath, e);
            throw new IllegalStateException("HDFS列表查询失败：" + e.getMessage(), e);
        }
    }

    /**
     * 查询 HDFS 文件或目录状态
     *
     * @param path 业务相对路径
     * @return 文件信息
     */
    public HdfsFileInfo getStatus(String path) {
        Assert.notBlank(path, "查询路径不能为空");

        Path hdfsPath = resolvePath(path);
        try {
            if (!fileSystem.exists(hdfsPath)) {
                throw new IllegalArgumentException("HDFS路径不存在：" + path);
            }

            FileStatus status = fileSystem.getFileStatus(hdfsPath);
            HdfsFileInfo fileInfo = toFileInfo(status);
            log.info("HDFS状态查询完成：path={}, directory={}, length={}",
                    path, fileInfo.getDirectory(), fileInfo.getLength());
            return fileInfo;
        } catch (Exception e) {
            log.error("HDFS状态查询失败：path={}", path, e);
            throw new IllegalStateException("HDFS状态查询失败：" + e.getMessage(), e);
        }
    }

    /**
     * 判断 HDFS 路径是否存在
     *
     * @param path 业务相对路径
     * @return 是否存在
     */
    public boolean exists(String path) {
        Assert.notBlank(path, "判断路径不能为空");

        try {
            return fileSystem.exists(resolvePath(path));
        } catch (Exception e) {
            log.error("HDFS路径存在性判断失败：path={}", path, e);
            throw new IllegalStateException("HDFS路径存在性判断失败：" + e.getMessage(), e);
        }
    }

    /**
     * 解析业务路径为 HDFS 绝对路径
     *
     * @param businessPath 业务相对路径
     * @return HDFS Path
     */
    private Path resolvePath(String businessPath) {
        String path = StrUtil.blankToDefault(businessPath, "/");
        path = StrUtil.replace(path, "\\", "/");

        if (StrUtil.startWithIgnoreCase(path, "hdfs://")
                || StrUtil.startWithIgnoreCase(path, "file://")
                || StrUtil.contains(path, "..")) {
            throw new IllegalArgumentException("非法HDFS业务路径：" + businessPath);
        }

        path = StrUtil.removePrefix(path, "/");

        String basePath = StrUtil.removeSuffix(hdfsProperties.getBasePath(), "/");
        if (StrUtil.isBlank(path)) {
            return new Path(basePath);
        }

        return new Path(basePath + "/" + path);
    }

    /**
     * 转换 HDFS 文件状态
     *
     * @param status HDFS 文件状态
     * @return 文件信息
     */
    private HdfsFileInfo toFileInfo(FileStatus status) {
        Path path = status.getPath();
        String fullPath = path.toUri().getPath();
        String basePath = StrUtil.removeSuffix(hdfsProperties.getBasePath(), "/");

        String businessPath = StrUtil.removePrefix(fullPath, basePath);
        businessPath = StrUtil.blankToDefault(businessPath, "/");

        return HdfsFileInfo.builder()
                .path(businessPath)
                .name(path.getName())
                .directory(status.isDirectory())
                .length(status.getLen())
                .replication(status.getReplication())
                .blockSize(status.getBlockSize())
                .owner(status.getOwner())
                .group(status.getGroup())
                .permission(status.getPermission().toString())
                .modificationTime(toLocalDateTime(status.getModificationTime()))
                .accessTime(toLocalDateTime(status.getAccessTime()))
                .build();
    }

    /**
     * 时间戳转换为本地时间
     *
     * @param timestamp 时间戳
     * @return 本地时间
     */
    private LocalDateTime toLocalDateTime(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }

}
```

### 文件上传

文件上传用于将本地文件、接口上传文件或业务生成文件写入 HDFS。底层调用 `fileSystem.create(path, overwrite)` 创建 HDFS 输出流，再通过 Hutool `IoUtil.copy` 完成流复制。Hadoop `FileSystem` API 提供 `create` 和 `open` 等文件读写入口，适合封装为 Spring Boot 服务内部的基础能力。([hadoop.apache.org](https://hadoop.apache.org/docs/r3.4.3/api/org/apache/hadoop/fs/FileSystem.html?utm_source=chatgpt.com))

常用调用方式如下：

```java
@Autowired
private HdfsClient hdfsClient;

public void uploadExample() {
    // 上传本地文件到 HDFS 业务目录
    hdfsClient.uploadLocalFile(
            "/tmp/demo.txt",
            "upload/2026/05/demo.txt",
            true
    );
}
```

如果后续在 Controller 中接收 `MultipartFile`，可以使用：

```java
public void uploadFromMultipartFile(MultipartFile file) throws IOException {
    // 文件名建议在接口层做安全处理，避免直接信任原始文件名
    String targetPath = "upload/2026/05/" + file.getOriginalFilename();

    hdfsClient.upload(file.getInputStream(), targetPath, true);
}
```

上传路径建议统一按业务维度组织，例如：

```text
/data/app/hdfs-demo/upload/2026/05/demo.txt
/data/app/hdfs-demo/report/daily/2026-05-08/result.csv
/data/app/hdfs-demo/logs/order/2026/05/08/order.log
```

### 文件下载

文件下载用于从 HDFS 读取文件内容，并写入本地文件、HTTP 响应流或其他输出流。底层通过 `fileSystem.open(path)` 获取输入流，再将内容复制到目标输出流。

下载到本地文件示例：

```java
@Autowired
private HdfsClient hdfsClient;

public void downloadExample() {
    hdfsClient.downloadToLocalFile(
            "upload/2026/05/demo.txt",
            "/tmp/hdfs-demo-download.txt"
    );
}
```

下载到 HTTP 响应流时，后续接口层可以这样接入：

```java
public void downloadToResponse(HttpServletResponse response) throws IOException {
    response.setContentType("application/octet-stream");
    response.setHeader("Content-Disposition", "attachment; filename=\"demo.txt\"");

    hdfsClient.download("upload/2026/05/demo.txt", response.getOutputStream());
}
```

下载接口中不建议一次性将大文件全部读入内存。对于 HDFS 大文件，应始终使用流式下载，避免 `byte[]` 或 `String` 承载完整文件内容。

### 文件删除

文件删除用于清理 HDFS 中的文件或目录。删除目录时，需要根据业务规则决定是否允许递归删除。Hadoop `FileSystem#delete(Path, boolean)` 的第二个参数用于控制递归删除行为。([hadoop.apache.org](https://hadoop.apache.org/docs/r3.4.3/api/org/apache/hadoop/fs/FileSystem.html?utm_source=chatgpt.com))

删除文件示例：

```java
@Autowired
private HdfsClient hdfsClient;

public void deleteFileExample() {
    boolean deleted = hdfsClient.delete("upload/2026/05/demo.txt", false);
    log.info("删除HDFS文件结果：deleted={}", deleted);
}
```

删除目录示例：

```java
@Autowired
private HdfsClient hdfsClient;

public void deleteDirectoryExample() {
    // 删除目录时需要显式开启 recursive
    boolean deleted = hdfsClient.delete("upload/2026/05", true);
    log.info("删除HDFS目录结果：deleted={}", deleted);
}
```

生产环境建议对递归删除增加权限控制或白名单限制，避免误删业务根目录下的大批量数据。接口层也应禁止删除空路径、根路径或高风险路径。

### 文件重命名

文件重命名用于修改 HDFS 文件名，也可以用于在同一 HDFS 文件系统内移动文件。底层调用 `fileSystem.rename(source, target)`。Hadoop `FileSystem` API 提供 `rename(Path, Path)` 用于路径重命名或移动。([hadoop.apache.org](https://hadoop.apache.org/docs/r3.4.3/api/org/apache/hadoop/fs/FileSystem.html?utm_source=chatgpt.com))

文件重命名示例：

```java
@Autowired
private HdfsClient hdfsClient;

public void renameExample() {
    boolean renamed = hdfsClient.rename(
            "upload/2026/05/demo.txt",
            "upload/2026/05/demo-new.txt"
    );
    log.info("HDFS文件重命名结果：renamed={}", renamed);
}
```

移动文件示例：

```java
@Autowired
private HdfsClient hdfsClient;

public void moveExample() {
    boolean moved = hdfsClient.rename(
            "upload/2026/05/demo-new.txt",
            "archive/2026/05/demo-new.txt"
    );
    log.info("HDFS文件移动结果：moved={}", moved);
}
```

重命名或移动前应确认源文件存在，目标路径的父目录不存在时可以自动创建。目标文件是否允许覆盖应由业务规则控制；当前示例使用 Hadoop 默认 `rename` 行为，不主动覆盖目标路径。

### 文件列表查询

文件列表查询用于展示 HDFS 目录下的文件和子目录信息。底层调用 `fileSystem.listStatus(path)`，返回 `FileStatus[]` 后再转换为业务 DTO。Hadoop 官方文档说明 `listStatus` 可用于列出目录下文件或目录状态，返回结果不保证天然有序，因此业务层如需排序应自行处理。([hadoop.apache.org](https://hadoop.apache.org/docs/r3.4.3/api/org/apache/hadoop/fs/FileSystem.html?utm_source=chatgpt.com))

查询目录示例：

```java
@Autowired
private HdfsClient hdfsClient;

public void listExample() {
    List<HdfsFileInfo> fileList = hdfsClient.list("upload/2026/05");

    fileList.forEach(fileInfo -> log.info(
            "HDFS文件：path={}, directory={}, length={}",
            fileInfo.getPath(),
            fileInfo.getDirectory(),
            fileInfo.getLength()
    ));
}
```

如果接口层需要返回给前端，建议在列表结果中保留以下字段：

| 字段               | 用途                         |
| ------------------ | ---------------------------- |
| `path`             | 后续下载、删除、重命名时使用 |
| `name`             | 前端展示文件名               |
| `directory`        | 区分文件和目录               |
| `length`           | 展示文件大小                 |
| `modificationTime` | 展示修改时间                 |
| `permission`       | 排查权限问题                 |
| `owner` / `group`  | 排查用户和用户组问题         |

如果目录下文件数量较大，后续可以扩展分页、前缀过滤、文件类型过滤和递归查询。本节先提供基础目录列表能力。

### 文件与目录状态查询

文件与目录状态查询用于判断路径是否存在、区分文件和目录、获取文件大小、权限、所属用户、修改时间等信息。底层调用 `fileSystem.getFileStatus(path)`。Hadoop `FileSystem` 官方文档也建议使用 `getFileStatus(Path)` 或 `listStatus()` 返回的 `FileStatus` 复用文件状态信息。([hadoop.apache.org](https://hadoop.apache.org/docs/r3.4.3/api/org/apache/hadoop/fs/FileSystem.html?utm_source=chatgpt.com))

查询文件状态示例：

```java
@Autowired
private HdfsClient hdfsClient;

public void getFileStatusExample() {
    HdfsFileInfo fileInfo = hdfsClient.getStatus("upload/2026/05/demo.txt");

    log.info("HDFS文件状态：path={}, directory={}, length={}, permission={}",
            fileInfo.getPath(),
            fileInfo.getDirectory(),
            fileInfo.getLength(),
            fileInfo.getPermission());
}
```

判断路径是否存在示例：

```java
@Autowired
private HdfsClient hdfsClient;

public void existsExample() {
    boolean exists = hdfsClient.exists("upload/2026/05/demo.txt");
    log.info("HDFS路径是否存在：exists={}", exists);
}
```

创建目录示例：

```java
@Autowired
private HdfsClient hdfsClient;

public void mkdirsExample() {
    boolean created = hdfsClient.mkdirs("upload/2026/05");
    log.info("HDFS目录创建结果：created={}", created);
}
```

状态查询通常用于以下场景：

| 场景       | 说明                                     |
| ---------- | ---------------------------------------- |
| 上传前检查 | 判断目标文件是否已存在，决定是否覆盖     |
| 下载前检查 | 判断源路径是否存在，避免下载空文件       |
| 删除前检查 | 判断路径类型，目录删除时要求确认递归参数 |
| 前端展示   | 展示文件大小、权限、修改时间、所属用户   |
| 问题排查   | 排查权限不足、目录不存在、路径错误等问题 |

到这里，HDFS 的基础客户端能力已经完成。后续“接口设计”章节可以在 `HdfsClient` 之上继续封装 Controller，例如上传接口调用 `upload`，下载接口调用 `download`，删除接口调用 `delete`，列表接口调用 `list`，目录创建接口调用 `mkdirs`。



## 接口设计

接口层用于将 HDFS 文件操作能力暴露为 HTTP API。Controller 不直接操作 Hadoop `FileSystem`，只负责参数接收、基础校验、响应封装和文件流输出，实际 HDFS 操作统一委托给前文封装的 `HdfsClient`。

本节涉及的主要文件如下：

```text
src/main/java/io/github/atengk/hdfs
├── controller
│   └── HdfsController.java
├── common
│   └── ApiResult.java
├── model
│   ├── HdfsFileInfo.java
│   ├── request
│   │   ├── HdfsDeleteRequest.java
│   │   └── HdfsMkdirRequest.java
│   └── vo
│       └── HdfsUploadVO.java
└── client
    └── HdfsClient.java
```

### 文件上传接口

文件上传接口用于接收前端或第三方系统上传的文件，并写入 HDFS 指定目录。接口支持指定上传目录、是否覆盖已有文件，同时会对上传文件名做基础安全处理，避免文件名为空或携带本地路径。

接口设计如下：

| 项目         | 说明                                         |
| ------------ | -------------------------------------------- |
| 请求路径     | `/api/hdfs/files/upload`                     |
| 请求方式     | `POST`                                       |
| Content-Type | `multipart/form-data`                        |
| 参数         | `file`、`directory`、`overwrite`             |
| 返回值       | 上传后的 HDFS 业务相对路径、文件名、文件大小 |

文件位置：`src/main/java/io/github/atengk/hdfs/model/vo/HdfsUploadVO.java`

```java
package io.github.atengk.hdfs.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * HDFS 文件上传结果
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
@Builder
public class HdfsUploadVO {

    /**
     * 上传后的业务相对路径
     */
    private String path;

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * HDFS 文件名
     */
    private String filename;

    /**
     * 文件大小，单位字节
     */
    private Long size;

}
```

下面是统一返回对象，后续上传、删除、列表、目录创建等接口都使用该结构返回。

文件位置：`src/main/java/io/github/atengk/hdfs/common/ApiResult.java`

```java
package io.github.atengk.hdfs.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口统一返回对象
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /**
     * 业务状态码
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
     * 请求成功
     *
     * @param data 响应数据
     * @return 统一响应对象
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "操作成功", data);
    }

    /**
     * 请求成功
     *
     * @param message 响应消息
     * @param data    响应数据
     * @return 统一响应对象
     */
    public static <T> ApiResult<T> success(String message, T data) {
        return new ApiResult<>(200, message, data);
    }

    /**
     * 请求失败
     *
     * @param code    业务状态码
     * @param message 响应消息
     * @return 统一响应对象
     */
    public static <T> ApiResult<T> fail(Integer code, String message) {
        return new ApiResult<>(code, message, null);
    }

}
```

Controller 中上传接口的完整实现如下。这里使用 Hutool 处理字符串和文件名，避免在接口层写过多重复判断逻辑。

文件位置：`src/main/java/io/github/atengk/hdfs/controller/HdfsController.java`

```java
package io.github.atengk.hdfs.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.hdfs.client.HdfsClient;
import io.github.atengk.hdfs.common.ApiResult;
import io.github.atengk.hdfs.model.HdfsFileInfo;
import io.github.atengk.hdfs.model.request.HdfsDeleteRequest;
import io.github.atengk.hdfs.model.request.HdfsMkdirRequest;
import io.github.atengk.hdfs.model.vo.HdfsUploadVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * HDFS 文件接口
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class HdfsController {

    private final HdfsClient hdfsClient;

    /**
     * 上传文件到 HDFS
     *
     * @param file      上传文件
     * @param directory 上传目录
     * @param overwrite 是否覆盖已有文件
     * @return 上传结果
     * @throws Exception 文件读取异常
     */
    @PostMapping("/api/hdfs/files/upload")
    public ApiResult<HdfsUploadVO> upload(MultipartFile file,
                                          String directory,
                                          Boolean overwrite) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String originalFilename = StrUtil.blankToDefault(file.getOriginalFilename(), "unknown");
        String filename = sanitizeFilename(originalFilename);
        String targetDirectory = normalizeDirectory(directory);
        String targetPath = joinPath(targetDirectory, filename);

        hdfsClient.upload(file.getInputStream(), targetPath, Boolean.TRUE.equals(overwrite));

        HdfsUploadVO uploadVO = HdfsUploadVO.builder()
                .path(targetPath)
                .originalFilename(originalFilename)
                .filename(filename)
                .size(file.getSize())
                .build();

        log.info("接口上传HDFS文件成功：filename={}, targetPath={}, size={}",
                filename, targetPath, file.getSize());
        return ApiResult.success("文件上传成功", uploadVO);
    }

    /**
     * 下载 HDFS 文件
     *
     * @param path     文件业务相对路径
     * @param filename 下载文件名
     * @param response HTTP 响应对象
     */
    @GetMapping("/api/hdfs/files/download")
    public void download(@NotBlank(message = "文件路径不能为空") String path,
                         String filename,
                         HttpServletResponse response) {
        String downloadFilename = StrUtil.blankToDefault(filename, FileUtil.getName(path));
        String safeFilename = sanitizeFilename(downloadFilename);

        response.setContentType("application/octet-stream");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                        .filename(safeFilename, StandardCharsets.UTF_8)
                        .build()
                        .toString());

        hdfsClient.download(path, response.getOutputStream());
        log.info("接口下载HDFS文件成功：path={}, filename={}", path, safeFilename);
    }

    /**
     * 删除 HDFS 文件或目录
     *
     * @param request 删除请求
     * @return 删除结果
     */
    @DeleteMapping("/api/hdfs/files")
    public ApiResult<Boolean> delete(@Valid @ModelAttribute HdfsDeleteRequest request) {
        boolean deleted = hdfsClient.delete(request.getPath(), request.getRecursive());
        log.info("接口删除HDFS路径完成：path={}, recursive={}, deleted={}",
                request.getPath(), request.getRecursive(), deleted);
        return ApiResult.success("删除完成", deleted);
    }

    /**
     * 查询 HDFS 文件列表
     *
     * @param directory 目录业务相对路径
     * @return 文件列表
     */
    @GetMapping("/api/hdfs/files")
    public ApiResult<List<HdfsFileInfo>> list(String directory) {
        List<HdfsFileInfo> fileList = hdfsClient.list(directory);
        log.info("接口查询HDFS文件列表完成：directory={}, count={}",
                directory, fileList.size());
        return ApiResult.success(fileList);
    }

    /**
     * 创建 HDFS 目录
     *
     * @param request 目录创建请求
     * @return 创建结果
     */
    @PostMapping("/api/hdfs/directories")
    public ApiResult<Boolean> mkdirs(@Valid @ModelAttribute HdfsMkdirRequest request) {
        boolean created = hdfsClient.mkdirs(request.getPath());
        log.info("接口创建HDFS目录完成：path={}, created={}", request.getPath(), created);
        return ApiResult.success("目录创建完成", created);
    }

    /**
     * 规范化目录路径
     *
     * @param directory 原始目录
     * @return 规范化后的目录
     */
    private String normalizeDirectory(String directory) {
        String value = StrUtil.blankToDefault(directory, "/");
        value = StrUtil.replace(value, "\\", "/");
        value = StrUtil.removePrefix(value, "/");
        value = StrUtil.removeSuffix(value, "/");
        return value;
    }

    /**
     * 拼接业务路径
     *
     * @param directory 目录
     * @param filename  文件名
     * @return 业务相对路径
     */
    private String joinPath(String directory, String filename) {
        if (StrUtil.isBlank(directory)) {
            return filename;
        }
        return directory + "/" + filename;
    }

    /**
     * 清理文件名
     *
     * @param filename 原始文件名
     * @return 安全文件名
     */
    private String sanitizeFilename(String filename) {
        String value = FileUtil.getName(StrUtil.blankToDefault(filename, "unknown"));
        value = StrUtil.replace(value, "\\", "_");
        value = StrUtil.replace(value, "/", "_");
        value = StrUtil.replace(value, ":", "_");

        if (StrUtil.isBlank(value) || StrUtil.equals(value, ".") || StrUtil.equals(value, "..")) {
            throw new IllegalArgumentException("文件名不合法");
        }

        return value;
    }

}
```

上传接口调用示例：

```bash
curl -X POST "http://localhost:8080/api/hdfs/files/upload" \
  -F "file=@/tmp/demo.txt" \
  -F "directory=upload/2026/05" \
  -F "overwrite=true"
```

响应示例：

```json
{
  "code": 200,
  "message": "文件上传成功",
  "data": {
    "path": "upload/2026/05/demo.txt",
    "originalFilename": "demo.txt",
    "filename": "demo.txt",
    "size": 1024
  }
}
```

### 文件下载接口

文件下载接口用于将 HDFS 文件以附件方式返回给调用方。该接口不返回统一 JSON，而是直接向 `HttpServletResponse` 写入文件流。

接口设计如下：

| 项目     | 说明                       |
| -------- | -------------------------- |
| 请求路径 | `/api/hdfs/files/download` |
| 请求方式 | `GET`                      |
| 参数     | `path`、`filename`         |
| 返回值   | 文件流                     |

调用示例：

```bash
curl -L -o /tmp/demo-download.txt \
  "http://localhost:8080/api/hdfs/files/download?path=upload/2026/05/demo.txt&filename=demo.txt"
```

关键点如下：

| 处理点     | 说明                                                    |
| ---------- | ------------------------------------------------------- |
| `path`     | HDFS 业务相对路径，例如 `upload/2026/05/demo.txt`       |
| `filename` | 下载时显示的文件名，不传时从 `path` 中提取              |
| 响应头     | 使用 `Content-Disposition: attachment`                  |
| 文件内容   | 通过 `hdfsClient.download(path, outputStream)` 流式写出 |

下载接口不建议返回 `byte[]`，尤其是 HDFS 中的大文件。直接流式写出可以减少内存占用，并且更适合大文件下载场景。

### 文件删除接口

文件删除接口用于删除 HDFS 文件或目录。删除目录时需要通过 `recursive=true` 显式启用递归删除，避免误删目录内容。

文件位置：`src/main/java/io/github/atengk/hdfs/model/request/HdfsDeleteRequest.java`

```java
package io.github.atengk.hdfs.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * HDFS 删除请求
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
public class HdfsDeleteRequest {

    /**
     * HDFS 业务相对路径
     */
    @NotBlank(message = "删除路径不能为空")
    private String path;

    /**
     * 是否递归删除
     */
    private Boolean recursive = false;

    /**
     * 获取递归删除标记
     *
     * @return 是否递归删除
     */
    public Boolean getRecursive() {
        return Boolean.TRUE.equals(recursive);
    }

}
```

接口设计如下：

| 项目     | 说明                |
| -------- | ------------------- |
| 请求路径 | `/api/hdfs/files`   |
| 请求方式 | `DELETE`            |
| 参数     | `path`、`recursive` |
| 返回值   | 是否删除成功        |

删除文件示例：

```bash
curl -X DELETE \
  "http://localhost:8080/api/hdfs/files?path=upload/2026/05/demo.txt&recursive=false"
```

删除目录示例：

```bash
curl -X DELETE \
  "http://localhost:8080/api/hdfs/files?path=upload/2026/05&recursive=true"
```

响应示例：

```json
{
  "code": 200,
  "message": "删除完成",
  "data": true
}
```

删除接口需要注意以下约束：

| 约束                         | 说明                                                       |
| ---------------------------- | ---------------------------------------------------------- |
| 禁止空路径                   | 避免误删业务根目录                                         |
| 目录删除需显式递归           | `recursive=true` 才允许删除非空目录                        |
| 路径必须走 `HdfsClient` 校验 | 禁止绕过 `base-path` 访问任意 HDFS 路径                    |
| 生产环境建议加权限           | 删除接口属于高风险接口，应结合登录用户、角色或审批逻辑控制 |

### 文件列表接口

文件列表接口用于查询 HDFS 指定目录下的文件和子目录。返回值使用前文定义的 `HdfsFileInfo`，包含路径、文件名、目录标识、文件大小、权限、用户、修改时间等信息。

接口设计如下：

| 项目     | 说明              |
| -------- | ----------------- |
| 请求路径 | `/api/hdfs/files` |
| 请求方式 | `GET`             |
| 参数     | `directory`       |
| 返回值   | 文件信息列表      |

调用示例：

```bash
curl "http://localhost:8080/api/hdfs/files?directory=upload/2026/05"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "path": "/upload/2026/05/demo.txt",
      "name": "demo.txt",
      "directory": false,
      "length": 1024,
      "replication": 3,
      "blockSize": 134217728,
      "owner": "ateng",
      "group": "ateng",
      "permission": "rw-r--r--",
      "modificationTime": "2026-05-08T10:30:00",
      "accessTime": "2026-05-08T10:30:00"
    }
  ]
}
```

当前列表接口只查询当前目录下一级内容。如果业务需要递归查询、分页、排序、文件类型筛选，可以在 `HdfsClient` 上继续扩展 `listRecursive`、`listPage`、`listBySuffix` 等方法。

### 目录创建接口

目录创建接口用于在 HDFS 业务根目录下创建子目录。该接口通常用于前端文件管理器、任务输出目录初始化、批处理结果目录准备等场景。

文件位置：`src/main/java/io/github/atengk/hdfs/model/request/HdfsMkdirRequest.java`

```java
package io.github.atengk.hdfs.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * HDFS 创建目录请求
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
public class HdfsMkdirRequest {

    /**
     * 目录业务相对路径
     */
    @NotBlank(message = "目录路径不能为空")
    private String path;

}
```

接口设计如下：

| 项目     | 说明                    |
| -------- | ----------------------- |
| 请求路径 | `/api/hdfs/directories` |
| 请求方式 | `POST`                  |
| 参数     | `path`                  |
| 返回值   | 是否创建成功            |

调用示例：

```bash
curl -X POST \
  "http://localhost:8080/api/hdfs/directories?path=upload/2026/05"
```

响应示例：

```json
{
  "code": 200,
  "message": "目录创建完成",
  "data": true
}
```

目录创建接口应保持幂等。也就是说，目标目录已经存在时，可以直接返回成功，而不是抛出异常。这种设计更适合初始化目录、重复执行任务和自动化部署场景。

## 异常处理

异常处理用于将 HDFS 底层异常、业务参数异常、文件操作异常统一转换成标准接口响应。这样可以避免接口直接暴露 Java 异常堆栈，同时便于前端和调用方根据状态码进行处理。

### HDFS 连接异常

HDFS 连接异常通常发生在应用启动、创建 `FileSystem`、首次访问 NameNode 或 DataNode 读写文件时。常见原因包括 NameNode 地址错误、网络不通、端口未开放、HDFS 服务未启动、用户无权限、Kerberos 配置缺失等。

建议定义专门的 HDFS 基础异常，便于全局异常处理器识别。

文件位置：`src/main/java/io/github/atengk/hdfs/exception/HdfsException.java`

```java
package io.github.atengk.hdfs.exception;

/**
 * HDFS 基础异常
 *
 * @author Ateng
 * @since 2026-05-08
 */
public class HdfsException extends RuntimeException {

    /**
     * 创建 HDFS 基础异常
     *
     * @param message 异常消息
     */
    public HdfsException(String message) {
        super(message);
    }

    /**
     * 创建 HDFS 基础异常
     *
     * @param message 异常消息
     * @param cause   原始异常
     */
    public HdfsException(String message, Throwable cause) {
        super(message, cause);
    }

}
```

文件位置：`src/main/java/io/github/atengk/hdfs/exception/HdfsConnectionException.java`

```java
package io.github.atengk.hdfs.exception;

/**
 * HDFS 连接异常
 *
 * @author Ateng
 * @since 2026-05-08
 */
public class HdfsConnectionException extends HdfsException {

    /**
     * 创建 HDFS 连接异常
     *
     * @param message 异常消息
     */
    public HdfsConnectionException(String message) {
        super(message);
    }

    /**
     * 创建 HDFS 连接异常
     *
     * @param message 异常消息
     * @param cause   原始异常
     */
    public HdfsConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

}
```

可以将前文 `HdfsAutoConfiguration` 中初始化 `FileSystem` 的异常包装为 `HdfsConnectionException`。示例调整如下：

```java
@Bean(destroyMethod = "close")
public FileSystem hdfsFileSystem(org.apache.hadoop.conf.Configuration hadoopConfiguration) {
    try {
        URI uri = URI.create(hdfsProperties.getUri());
        FileSystem fileSystem = FileSystem.get(uri, hadoopConfiguration, hdfsProperties.getUser());

        if (hdfsProperties.isCheckEnabled()) {
            Path basePath = new Path(hdfsProperties.getBasePath());
            if (!fileSystem.exists(basePath)) {
                boolean created = fileSystem.mkdirs(basePath);
                log.info("HDFS业务根目录不存在，自动创建结果：path={}, created={}", basePath, created);
            }
            log.info("HDFS连接检查完成，uri={}, basePath={}", hdfsProperties.getUri(), hdfsProperties.getBasePath());
        }

        return fileSystem;
    } catch (Exception e) {
        log.error("HDFS连接初始化失败：uri={}, user={}", hdfsProperties.getUri(), hdfsProperties.getUser(), e);
        throw new HdfsConnectionException("HDFS连接初始化失败，请检查NameNode地址、网络、端口和用户权限", e);
    }
}
```

连接异常排查重点如下：

| 异常表现          | 可能原因                            | 处理方式                                   |
| ----------------- | ----------------------------------- | ------------------------------------------ |
| 连接超时          | NameNode 地址或端口不可达           | 检查网络、防火墙、端口                     |
| UnknownHost       | 主机名无法解析                      | 配置 DNS 或 `/etc/hosts`                   |
| Permission denied | HDFS 用户无权限                     | 调整目录所属用户或权限                     |
| Kerberos 认证失败 | 缺少 keytab、principal 或 krb5 配置 | 补充安全认证配置                           |
| DataNode 访问失败 | DataNode 地址对应用不可达           | 检查 DataNode 主机名、容器网络、跨网段访问 |

### 文件操作异常

文件操作异常通常发生在上传、下载、删除、重命名、目录创建、列表查询和状态查询过程中。建议将此类异常统一包装为 `HdfsOperationException`。

文件位置：`src/main/java/io/github/atengk/hdfs/exception/HdfsOperationException.java`

```java
package io.github.atengk.hdfs.exception;

/**
 * HDFS 文件操作异常
 *
 * @author Ateng
 * @since 2026-05-08
 */
public class HdfsOperationException extends HdfsException {

    /**
     * 创建 HDFS 文件操作异常
     *
     * @param message 异常消息
     */
    public HdfsOperationException(String message) {
        super(message);
    }

    /**
     * 创建 HDFS 文件操作异常
     *
     * @param message 异常消息
     * @param cause   原始异常
     */
    public HdfsOperationException(String message, Throwable cause) {
        super(message, cause);
    }

}
```

建议将前文 `HdfsClient` 中的 `IllegalStateException` 替换为 `HdfsOperationException`，这样全局异常处理器可以更准确地区分 HDFS 操作失败和普通业务参数错误。

示例调整如下：

```java
try {
    hdfsClient.upload(file.getInputStream(), targetPath, Boolean.TRUE.equals(overwrite));
} catch (Exception e) {
    log.error("接口上传HDFS文件失败：targetPath={}", targetPath, e);
    throw new HdfsOperationException("HDFS文件上传失败", e);
}
```

在 `HdfsClient` 内部也建议统一写法：

```java
catch (Exception e) {
    log.error("HDFS文件上传失败：targetPath={}", targetPath, e);
    throw new HdfsOperationException("HDFS文件上传失败：" + e.getMessage(), e);
}
```

文件操作异常的常见类型如下：

| 操作     | 常见异常                                         | 处理建议                               |
| -------- | ------------------------------------------------ | -------------------------------------- |
| 上传     | 目录不存在、无写权限、文件已存在                 | 自动创建父目录，明确 overwrite 策略    |
| 下载     | 文件不存在、无读权限、输出流异常                 | 下载前检查路径，接口返回明确错误       |
| 删除     | 路径不存在、目录非空、无删除权限                 | 区分文件和目录，目录删除要求 recursive |
| 重命名   | 源路径不存在、目标路径已存在、跨文件系统移动失败 | 先检查源路径和目标父目录               |
| 列表查询 | 目录不存在、权限不足                             | 不存在时可返回空列表或明确错误         |
| 状态查询 | 路径不存在                                       | 返回业务错误，提示路径不存在           |

### 统一异常返回

统一异常返回通过 `@RestControllerAdvice` 实现，集中处理参数校验、HDFS 连接异常、HDFS 文件操作异常、上传文件异常和兜底系统异常。

文件位置：`src/main/java/io/github/atengk/hdfs/exception/GlobalExceptionHandler.java`

```java
package io.github.atengk.hdfs.exception;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.hdfs.common.ApiResult;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 HDFS 连接异常
     *
     * @param e HDFS 连接异常
     * @return 统一响应
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(HdfsConnectionException.class)
    public ApiResult<Void> handleHdfsConnectionException(HdfsConnectionException e) {
        log.error("HDFS连接异常：{}", e.getMessage(), e);
        return ApiResult.fail(1501, e.getMessage());
    }

    /**
     * 处理 HDFS 文件操作异常
     *
     * @param e HDFS 文件操作异常
     * @return 统一响应
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(HdfsOperationException.class)
    public ApiResult<Void> handleHdfsOperationException(HdfsOperationException e) {
        log.error("HDFS文件操作异常：{}", e.getMessage(), e);
        return ApiResult.fail(1502, e.getMessage());
    }

    /**
     * 处理 HDFS 基础异常
     *
     * @param e HDFS 基础异常
     * @return 统一响应
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(HdfsException.class)
    public ApiResult<Void> handleHdfsException(HdfsException e) {
        log.error("HDFS异常：{}", e.getMessage(), e);
        return ApiResult.fail(1500, e.getMessage());
    }

    /**
     * 处理 Bean 参数校验异常
     *
     * @param e 参数校验异常
     * @return 统一响应
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + "：" + error.getDefaultMessage())
                .collect(Collectors.joining("；"));

        log.info("接口参数校验失败：{}", message);
        return ApiResult.fail(1400, StrUtil.blankToDefault(message, "请求参数不合法"));
    }

    /**
     * 处理普通参数约束异常
     *
     * @param e 参数约束异常
     * @return 统一响应
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResult<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations()
                .stream()
                .map(item -> item.getPropertyPath() + "：" + item.getMessage())
                .collect(Collectors.joining("；"));

        log.info("接口参数约束失败：{}", message);
        return ApiResult.fail(1400, StrUtil.blankToDefault(message, "请求参数不合法"));
    }

    /**
     * 处理请求参数缺失异常
     *
     * @param e 请求参数缺失异常
     * @return 统一响应
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResult<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        String message = "缺少请求参数：" + e.getParameterName();
        log.info("请求参数缺失：{}", message);
        return ApiResult.fail(1400, message);
    }

    /**
     * 处理参数类型转换异常
     *
     * @param e 参数类型转换异常
     * @return 统一响应
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(TypeMismatchException.class)
    public ApiResult<Void> handleTypeMismatchException(TypeMismatchException e) {
        String message = "参数类型不正确：" + e.getPropertyName();
        log.info("参数类型转换失败：{}", message);
        return ApiResult.fail(1400, message);
    }

    /**
     * 处理非法参数异常
     *
     * @param e 非法参数异常
     * @return 统一响应
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResult<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.info("非法请求参数：{}", e.getMessage());
        return ApiResult.fail(1400, e.getMessage());
    }

    /**
     * 处理文件上传大小超限异常
     *
     * @param e 文件上传大小超限异常
     * @return 统一响应
     */
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResult<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("上传文件大小超过限制：{}", e.getMessage());
        return ApiResult.fail(1401, "上传文件大小超过限制");
    }

    /**
     * 处理请求方法不支持异常
     *
     * @param e 请求方法异常
     * @return 统一响应
     */
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ApiResult<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        String message = "请求方法不支持：" + e.getMethod();
        log.info("请求方法不支持：{}", e.getMessage());
        return ApiResult.fail(1405, message);
    }

    /**
     * 处理兜底系统异常
     *
     * @param e 系统异常
     * @return 统一响应
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception e) {
        log.error("系统异常：{}", e.getMessage(), e);
        return ApiResult.fail(1500, "系统异常，请联系管理员");
    }

}
```

统一错误码建议如下：

| 错误码 | HTTP 状态 | 说明              |
| ------ | --------- | ----------------- |
| `1400` | 400       | 请求参数错误      |
| `1401` | 413       | 上传文件过大      |
| `1405` | 405       | 请求方法不支持    |
| `1500` | 500       | 系统通用异常      |
| `1501` | 500       | HDFS 连接异常     |
| `1502` | 500       | HDFS 文件操作异常 |

失败响应示例：

```json
{
  "code": 1502,
  "message": "HDFS文件下载失败：HDFS路径不存在：upload/2026/05/demo.txt",
  "data": null
}
```

上传文件大小可以在配置文件中限制，避免超大文件直接打满应用内存、网关或临时目录。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  servlet:
    multipart:
      # 单个文件最大大小
      max-file-size: 200MB
      # 单次请求最大大小
      max-request-size: 200MB
      # 文件达到阈值后写入磁盘临时目录，避免大文件全部占用内存
      file-size-threshold: 2MB
```

到这里，HDFS 模块已经具备完整的接口入口和统一异常响应能力。后续“功能验证”章节可以继续基于这些接口编写 curl 测试、HDFS 命令行校验和常见问题排查。



## 功能验证

功能验证用于确认 Spring Boot 应用、HDFS 客户端封装、HTTP 接口和 HDFS 集群之间能够正常协作。验证时建议同时使用接口调用和 HDFS 命令行交叉检查，避免只看接口返回成功但文件未真实落入 HDFS。

### 本地接口测试

本地接口测试前，需要先确认 Spring Boot 应用已经使用 `dev` 环境正常启动，并且 `application-dev.yml` 中的 `hdfs.uri`、`hdfs.user`、`hdfs.base-path` 配置正确。

启动项目：

```bash
# 编译项目，跳过测试
mvn clean package -DskipTests

# 使用 dev 环境启动项目
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

启动成功后，控制台应能看到类似日志：

```text
初始化Hadoop配置完成，uri=hdfs://192.168.1.10:8020, user=ateng, basePath=/data/app/hdfs-demo
HDFS连接检查完成，uri=hdfs://192.168.1.10:8020, basePath=/data/app/hdfs-demo
```

如果启动阶段已经抛出 HDFS 连接异常，需要先排查 NameNode 地址、端口、网络、HDFS 用户和业务根目录权限，再继续测试接口。

准备本地测试文件：

```bash
# 创建测试文件
echo "hello springboot hdfs" > /tmp/hdfs-demo.txt

# 查看文件内容
cat /tmp/hdfs-demo.txt
```

测试文件上传接口：

```bash
curl -X POST "http://localhost:8080/api/hdfs/files/upload" \
  -F "file=@/tmp/hdfs-demo.txt" \
  -F "directory=upload/2026/05" \
  -F "overwrite=true"
```

预期响应：

```json
{
  "code": 200,
  "message": "文件上传成功",
  "data": {
    "path": "upload/2026/05/hdfs-demo.txt",
    "originalFilename": "hdfs-demo.txt",
    "filename": "hdfs-demo.txt",
    "size": 22
  }
}
```

测试文件列表接口：

```bash
curl "http://localhost:8080/api/hdfs/files?directory=upload/2026/05"
```

预期响应中应包含刚才上传的文件：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "path": "/upload/2026/05/hdfs-demo.txt",
      "name": "hdfs-demo.txt",
      "directory": false,
      "length": 22,
      "replication": 3,
      "blockSize": 134217728,
      "owner": "ateng",
      "group": "ateng",
      "permission": "rw-r--r--"
    }
  ]
}
```

测试文件下载接口：

```bash
curl -L -o /tmp/hdfs-demo-download.txt \
  "http://localhost:8080/api/hdfs/files/download?path=upload/2026/05/hdfs-demo.txt&filename=hdfs-demo-download.txt"

# 查看下载后的内容
cat /tmp/hdfs-demo-download.txt
```

预期输出：

```text
hello springboot hdfs
```

测试目录创建接口：

```bash
curl -X POST \
  "http://localhost:8080/api/hdfs/directories?path=upload/2026/06"
```

预期响应：

```json
{
  "code": 200,
  "message": "目录创建完成",
  "data": true
}
```

测试文件删除接口：

```bash
curl -X DELETE \
  "http://localhost:8080/api/hdfs/files?path=upload/2026/05/hdfs-demo.txt&recursive=false"
```

预期响应：

```json
{
  "code": 200,
  "message": "删除完成",
  "data": true
}
```

测试删除后的列表结果：

```bash
curl "http://localhost:8080/api/hdfs/files?directory=upload/2026/05"
```

如果目录为空，预期返回空数组：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": []
}
```

接口验证建议按以下顺序执行：

| 顺序 | 操作         | 验证目标                   |
| ---- | ------------ | -------------------------- |
| 1    | 启动应用     | 验证配置加载和 HDFS 连接   |
| 2    | 创建目录     | 验证目录写权限             |
| 3    | 上传文件     | 验证文件写入能力           |
| 4    | 查询列表     | 验证目录读取能力           |
| 5    | 下载文件     | 验证文件读取和 HTTP 流输出 |
| 6    | 删除文件     | 验证删除权限和路径处理     |
| 7    | 再次查询列表 | 验证删除结果               |

### HDFS 命令行校验

HDFS 命令行校验用于确认接口操作是否真实作用到 HDFS 集群。接口返回成功后，应通过 `hdfs dfs` 命令检查目标路径、文件内容、权限和目录状态。

假设配置中的业务根目录为：

```text
/data/app/hdfs-demo
```

查看业务根目录：

```bash
hdfs dfs -ls /data/app/hdfs-demo
```

查看上传目录：

```bash
hdfs dfs -ls /data/app/hdfs-demo/upload/2026/05
```

查看文件内容：

```bash
hdfs dfs -cat /data/app/hdfs-demo/upload/2026/05/hdfs-demo.txt
```

下载 HDFS 文件到本地进行对比：

```bash
hdfs dfs -get -f \
  /data/app/hdfs-demo/upload/2026/05/hdfs-demo.txt \
  /tmp/hdfs-demo-from-cli.txt

cat /tmp/hdfs-demo-from-cli.txt
```

查看文件状态：

```bash
hdfs dfs -stat "%n %b %r %u %g %y" \
  /data/app/hdfs-demo/upload/2026/05/hdfs-demo.txt
```

其中常用格式含义如下：

| 格式 | 说明               |
| ---- | ------------------ |
| `%n` | 文件名             |
| `%b` | 文件大小，单位字节 |
| `%r` | 副本数             |
| `%u` | 所属用户           |
| `%g` | 所属用户组         |
| `%y` | 修改时间           |

校验目录创建结果：

```bash
hdfs dfs -test -d /data/app/hdfs-demo/upload/2026/06

if [ $? -eq 0 ]; then
  echo "目录存在"
else
  echo "目录不存在"
fi
```

校验文件删除结果：

```bash
hdfs dfs -test -e /data/app/hdfs-demo/upload/2026/05/hdfs-demo.txt

if [ $? -eq 0 ]; then
  echo "文件仍然存在"
else
  echo "文件已删除"
fi
```

如果需要清理测试目录，可以执行：

```bash
# 递归删除测试目录，执行前确认路径正确
hdfs dfs -rm -r -f /data/app/hdfs-demo/upload/2026/05
```

该命令会递归删除指定 HDFS 目录，生产环境应谨慎执行。建议只清理明确的测试目录，不要直接删除业务根目录。

### 常见问题排查

常见问题排查应优先从启动日志、接口响应、HDFS 命令行和 NameNode Web UI 四个方向定位。Spring Boot 侧主要关注配置、依赖、网络和异常堆栈；HDFS 侧主要关注路径、权限、用户、NameNode 和 DataNode 状态。

常见问题如下：

| 问题现象                         | 可能原因                                         | 处理方式                                                     |
| -------------------------------- | ------------------------------------------------ | ------------------------------------------------------------ |
| 应用启动失败，提示连接 HDFS 失败 | `hdfs.uri` 配置错误、NameNode 不可达、端口未开放 | 使用 `nc -vz host port` 检查网络，确认 NameNode 地址和端口   |
| `UnknownHostException`           | HDFS 返回的主机名本机无法解析                    | 配置 `/etc/hosts`、DNS，或按环境开启 `dfs.client.use.datanode.hostname` |
| `Permission denied`              | `hdfs.user` 对 `base-path` 没有读写权限          | 使用 `hdfs dfs -chown` 或 `hdfs dfs -chmod` 调整目录权限     |
| 上传接口返回文件已存在           | 目标路径已有文件且 `overwrite=false`             | 改为 `overwrite=true`，或上传到新路径                        |
| 下载接口提示路径不存在           | 请求中的 `path` 与实际 HDFS 路径不一致           | 使用 `hdfs dfs -ls` 检查完整路径                             |
| 删除目录失败                     | 删除的是非空目录，但 `recursive=false`           | 删除目录时传入 `recursive=true`                              |
| 文件列表为空                     | 目录不存在或目录下无文件                         | 用 `hdfs dfs -ls` 直接检查目录                               |
| Windows 本地运行异常             | 缺少 Hadoop Windows 本地支持、路径兼容问题       | 优先使用 WSL2、Linux 虚拟机或 Docker 环境                    |
| 上传大文件失败                   | 上传大小超过 Spring Multipart 限制               | 调整 `spring.servlet.multipart.max-file-size` 和 `max-request-size` |
| 下载大文件内存占用高             | 接口层将文件读入内存                             | 使用响应流直接写出，不要返回 `byte[]`                        |

网络连通性排查：

```bash
# 检查 NameNode RPC 端口
nc -vz 192.168.1.10 8020

# 检查 NameNode Web UI 端口，端口按实际环境调整
nc -vz 192.168.1.10 9870
```

HDFS 权限排查：

```bash
# 查看业务根目录权限
hdfs dfs -ls /data/app

# 查看具体目录权限
hdfs dfs -ls /data/app/hdfs-demo

# 修改目录所属用户，按实际用户替换
hdfs dfs -chown -R ateng:ateng /data/app/hdfs-demo

# 修改目录权限，开发环境可放宽，生产环境按最小权限控制
hdfs dfs -chmod -R 755 /data/app/hdfs-demo
```

路径问题排查：

```bash
# 查看业务根目录
hdfs dfs -ls /data/app/hdfs-demo

# 查看上传目录
hdfs dfs -ls /data/app/hdfs-demo/upload

# 查看指定日期目录
hdfs dfs -ls /data/app/hdfs-demo/upload/2026/05
```

Spring Boot 配置排查：

```bash
# 查看当前启动环境
echo $SPRING_PROFILES_ACTIVE

# 使用指定 profile 启动
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

如果接口报错但命令行可以正常操作 HDFS，通常优先检查以下内容：

| 检查点        | 说明                                                |
| ------------- | --------------------------------------------------- |
| 应用运行用户  | Java 应用中的 `hdfs.user` 是否与命令行用户一致      |
| 业务根目录    | `hdfs.base-path` 是否和命令行校验路径一致           |
| 请求路径      | 接口传入的是业务相对路径，不是完整 HDFS 路径        |
| 网络环境      | 应用服务器是否与 Hadoop 节点处于同一网络            |
| DataNode 地址 | NameNode 返回的 DataNode 地址是否能被应用服务器访问 |

## 项目总结

本项目通过 Spring Boot 3 对 Hadoop HDFS Java 客户端进行封装，形成了一个可复用、可扩展、可验证的 HDFS 文件服务模块。整体设计重点是：配置集中管理、客户端统一封装、接口层职责清晰、异常响应统一、验证手段完整。

### 核心流程回顾

整个 HDFS 开发流程可以概括为以下几个阶段：

```text
环境准备
  |
  v
Maven 依赖配置
  |
  v
HDFS 连接参数配置
  |
  v
Hadoop Configuration 初始化
  |
  v
FileSystem 客户端创建
  |
  v
HdfsClient 核心能力封装
  |
  v
Controller 接口暴露
  |
  v
统一异常处理
  |
  v
接口测试 + HDFS 命令行校验
```

核心实现关系如下：

| 层级     | 主要职责                                     | 关键类                                   |
| -------- | -------------------------------------------- | ---------------------------------------- |
| 配置层   | 读取 HDFS 地址、用户、根目录、缓冲区等参数   | `HdfsProperties`                         |
| 初始化层 | 创建 Hadoop `Configuration` 和 `FileSystem`  | `HdfsAutoConfiguration`                  |
| 客户端层 | 封装上传、下载、删除、重命名、列表、状态查询 | `HdfsClient`                             |
| 模型层   | 定义文件信息、上传结果、请求参数对象         | `HdfsFileInfo`、`HdfsUploadVO`、请求 DTO |
| 接口层   | 暴露 HTTP API，处理请求响应和文件流          | `HdfsController`                         |
| 异常层   | 统一处理连接异常、文件操作异常、参数异常     | `GlobalExceptionHandler`                 |

核心设计原则如下：

| 原则           | 说明                                                 |
| -------------- | ---------------------------------------------------- |
| 业务路径相对化 | 外部接口只接收业务相对路径，不直接接收完整 HDFS 地址 |
| 根目录统一限制 | 所有操作都限制在 `hdfs.base-path` 下                 |
| 文件流式处理   | 上传下载均使用流处理，避免大文件占用大量内存         |
| 客户端集中封装 | 业务代码不直接依赖 Hadoop `FileSystem`               |
| 异常统一返回   | 对外返回稳定 JSON 错误结构，避免暴露底层异常堆栈     |
| 命令行交叉验证 | 接口测试后使用 `hdfs dfs` 校验真实文件状态           |

本项目已经覆盖 HDFS 文件服务的基础能力，包括：

| 功能                          | 是否完成 |
| ----------------------------- | -------- |
| HDFS 连接参数配置             | 已完成   |
| Hadoop `Configuration` 初始化 | 已完成   |
| `FileSystem` Bean 创建        | 已完成   |
| 文件上传                      | 已完成   |
| 文件下载                      | 已完成   |
| 文件删除                      | 已完成   |
| 文件重命名                    | 已完成   |
| 文件列表查询                  | 已完成   |
| 文件与目录状态查询            | 已完成   |
| 目录创建                      | 已完成   |
| REST 接口封装                 | 已完成   |
| 统一异常处理                  | 已完成   |
| 本地接口验证                  | 已完成   |
| HDFS 命令行校验               | 已完成   |

### 后续扩展方向

当前项目已经可以满足基础 HDFS 文件管理需求。后续可根据生产环境要求继续扩展安全、性能、可观测性、文件管理和多存储适配能力。

可扩展方向如下：

| 方向          | 说明                                               |
| ------------- | -------------------------------------------------- |
| Kerberos 认证 | 支持生产 Hadoop 集群常见的 Kerberos 安全认证       |
| 多 HDFS 集群  | 支持多个 NameNode 或多个业务集群动态路由           |
| HA NameNode   | 支持基于 `nameservices` 的 HDFS 高可用访问配置     |
| 文件分片上传  | 支持大文件前端分片上传、服务端合并后写入 HDFS      |
| 断点续传      | 针对大文件上传或下载增加断点续传能力               |
| 文件预览      | 对文本、CSV、JSON、日志文件提供在线预览            |
| 文件权限控制  | 按登录用户、角色、租户控制目录访问权限             |
| 操作审计日志  | 记录上传、下载、删除、重命名等操作审计信息         |
| 文件生命周期  | 按时间、目录、文件类型自动归档或清理历史文件       |
| 接口文档      | 集成 Springdoc OpenAPI 或 Knife4j 生成接口文档     |
| 监控指标      | 统计上传次数、下载次数、失败次数、文件大小、耗时   |
| 存储适配层    | 抽象统一文件存储接口，兼容 HDFS、MinIO、S3、OSS    |
| 异步任务      | 对大文件复制、批量删除、批量迁移等操作改为异步执行 |
| 限流与熔断    | 避免大文件高并发访问压垮 HDFS 或应用服务           |

如果要继续向生产级演进，优先建议补充以下能力：

```text
1. Kerberos 认证配置
2. HDFS HA 配置
3. 接口鉴权和目录权限控制
4. 上传下载审计日志
5. 大文件上传下载性能优化
6. Prometheus 监控指标
7. 统一文件存储抽象接口
```

生产级扩展示例结构可以调整为：

```text
src/main/java/io/github/atengk/hdfs
├── audit
│   └── HdfsOperationLogService.java
├── auth
│   └── HdfsPathPermissionService.java
├── client
│   └── HdfsClient.java
├── config
│   ├── HdfsAutoConfiguration.java
│   ├── HdfsKerberosConfiguration.java
│   └── HdfsProperties.java
├── controller
│   └── HdfsController.java
├── exception
│   ├── GlobalExceptionHandler.java
│   ├── HdfsConnectionException.java
│   └── HdfsOperationException.java
├── metrics
│   └── HdfsMetricsService.java
└── storage
    ├── FileStorageService.java
    ├── HdfsFileStorageServiceImpl.java
    └── MinioFileStorageServiceImpl.java
```

最终建议将 HDFS 能力从“具体文件系统操作工具类”升级为“统一文件存储服务”。这样业务系统只依赖 `FileStorageService` 抽象接口，不直接感知底层是 HDFS、MinIO、S3 还是 OSS，后续迁移和扩展成本会更低。