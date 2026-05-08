# Spring Boot 集成 HBase 开发

本文主要说明如何在 Spring Boot 3 项目中集成 Apache HBase 客户端，完成 HBase 连接配置、依赖引入和基础客户端参数准备。后续章节可继续基于该配置扩展表管理、数据写入、RowKey 查询、Scan 扫描、批量操作和统一异常处理。

## 项目概述

### 功能定位

本项目定位为 Spring Boot 3 与 HBase 的后端集成示例，用于在 Java 服务中封装 HBase 的基础访问能力。系统通过 HBase Java Client 建立与 HBase 集群的连接，并在业务服务中提供表管理、数据写入、单条查询、批量查询、范围扫描和数据删除等能力。

在整体设计上，项目不直接在 Controller 层操作 HBase 原生 API，而是通过配置类、工具类、模板类和业务服务层进行分层封装。这样可以降低业务代码对 HBase 原生对象的耦合，便于后续统一处理连接生命周期、异常转换、日志记录、参数校验和返回结果格式。

HBase 客户端的核心连接对象由 `ConnectionFactory` 创建，官方 API 说明中也明确由调用方负责管理 `Connection` 生命周期，并通过 `Connection#getTable` 获取表操作对象。([hbase.apache.org](https://hbase.apache.org/2.6/devapidocs/org/apache/hadoop/hbase/client/ConnectionFactory.html)) 因此，本文后续实现会将 `Connection` 作为 Spring Bean 管理，避免每次请求重复创建连接。

### 使用场景

该集成方案适合需要高吞吐、宽表结构和海量稀疏数据存储的后端系统。典型使用场景包括用户行为日志、设备采集数据、订单轨迹、风控流水、物联网时序数据、画像标签、消息明细和历史归档数据查询。

HBase 的优势在于按 RowKey 进行快速定位查询，并支持按 RowKey 范围进行 Scan 扫描。对于数据量较大、字段结构经常扩展、单行列数量较多、需要按主键或前缀范围查询的场景，HBase 比传统关系型数据库更适合承载明细型、日志型和宽表型数据。

本项目重点覆盖以下开发场景：

| 场景       | 说明                                            |
| ---------- | ----------------------------------------------- |
| 数据写入   | 将业务数据按照 RowKey、列族、列限定符写入 HBase |
| 单条查询   | 根据表名和 RowKey 查询一行数据                  |
| 批量查询   | 根据多个 RowKey 批量获取多行数据                |
| 条件扫描   | 根据 RowKey 起止范围或前缀进行 Scan 查询        |
| 表结构管理 | 创建表、删除表、判断表是否存在                  |
| 统一封装   | 封装通用 HBase 操作，减少业务层重复代码         |

### 技术选型

本项目采用 Spring Boot 3 作为应用基础框架，使用 HBase 2.x Java Client 连接 HBase 集群。Spring Boot 3.5.x 官方要求至少 Java 17，并显式支持 Maven 3.6.3 或更高版本。([Home](https://docs.spring.io/spring-boot/3.5/system-requirements.html))

| 技术         | 版本建议 | 说明                                              |
| ------------ | -------- | ------------------------------------------------- |
| JDK          | 17+      | Spring Boot 3 最低要求 Java 17                    |
| Spring Boot  | 3.5.x    | 示例文档以 Spring Boot 3.5.x 为基线               |
| Maven        | 3.6.3+   | Spring Boot 3.5.x 官方支持 Maven 3.6.3 或更高版本 |
| HBase Client | 2.6.x    | 与 HBase 2.x 集群保持主版本一致                   |
| Hadoop       | 3.x      | 生产环境常见组合，依赖版本需与集群一致            |
| Hutool       | 5.8.x    | 用于字符串、集合、对象判断等通用工具能力          |
| Lombok       | 1.18.x   | 简化配置对象、DTO、VO 和日志对象编写              |

HBase 2.x 客户端默认依赖 ZooKeeper 发现集群元数据，客户端需要能够访问 ZooKeeper 集群，并配置 `hbase.zookeeper.quorum`。HBase 官方参考文档说明，2.x 客户端默认以 ZooKeeper 作为连接注册信息来源。([hbase.apache.org](https://hbase.apache.org/book.html))

## 环境准备

### HBase 环境说明

在开发前，需要确认已经具备可访问的 HBase 环境。HBase 可以部署为单机模式、伪分布式模式或完全分布式模式。开发和测试环境可以使用单机或伪分布式模式，生产环境通常使用完全分布式模式，并依赖 HDFS、ZooKeeper、HMaster 和 RegionServer 共同运行。

对于 Spring Boot 应用而言，重点不是直接管理 HBase 服务端进程，而是准备客户端可访问的连接信息。HBase 2.x 客户端最小配置通常包括 ZooKeeper 地址、ZooKeeper 客户端端口和 ZNode 根路径。

| 配置项                                | 示例值                             | 说明                                                     |
| ------------------------------------- | ---------------------------------- | -------------------------------------------------------- |
| `hbase.zookeeper.quorum`              | `hbase-zk01,hbase-zk02,hbase-zk03` | ZooKeeper 集群地址，多个节点使用英文逗号分隔             |
| `hbase.zookeeper.property.clientPort` | `2181`                             | ZooKeeper 客户端连接端口，默认值为 `2181`                |
| `zookeeper.znode.parent`              | `/hbase`                           | HBase 在 ZooKeeper 中使用的根节点，默认值通常为 `/hbase` |
| `hbase.client.operation.timeout`      | `30000`                            | 客户端操作超时时间                                       |
| `hbase.rpc.timeout`                   | `30000`                            | RPC 调用超时时间                                         |
| `hbase.client.retries.number`         | `3`                                | 客户端失败重试次数                                       |

HBase 官方默认配置说明中，`hbase.zookeeper.quorum` 是 ZooKeeper ensemble 的逗号分隔地址，完全分布式环境应配置完整 ZooKeeper 节点列表；`hbase.zookeeper.property.clientPort` 默认值为 `2181`；`zookeeper.znode.parent` 默认值为 `/hbase`。([hbase.apache.org](https://hbase.apache.org/docs/configuration/default))

开发环境可以先使用 HBase Shell 验证集群是否可用：

```bash
# 进入 HBase Shell
hbase shell

# 查看 HBase 状态
status

# 查看已有表
list
```

以上命令用于确认 HBase 服务端可正常响应。`status` 可检查 Master 和 RegionServer 状态，`list` 可确认当前命名空间下已有表信息。如果这些命令执行失败，应先排查 HBase 服务、ZooKeeper 地址、网络连通性和客户端配置文件。

### Spring Boot 3 版本要求

Spring Boot 3 项目需要使用 Java 17 或更高版本。本文示例建议使用 Java 17 作为运行基线，避免因过高 JDK 版本导致 Hadoop、HBase 或其他底层依赖出现兼容性问题。

建议环境如下：

| 环境项      | 建议版本                | 说明                                      |
| ----------- | ----------------------- | ----------------------------------------- |
| JDK         | 17                      | Spring Boot 3 基础版本，兼容性相对稳定    |
| Spring Boot | 3.5.x                   | 当前示例使用 Spring Boot 3.5 系列         |
| Maven       | 3.6.3+                  | 与 Spring Boot 3.5.x 官方构建要求一致     |
| 编码        | UTF-8                   | 保证 RowKey、列名、日志和接口返回字符一致 |
| 运行系统    | Linux / macOS / Windows | 生产环境建议使用 Linux                    |

检查本地 Java 和 Maven 版本：

```bash
# 查看 Java 版本，建议为 17 或更高
java -version

# 查看 Maven 版本，建议为 3.6.3 或更高
mvn -version
```

如果项目使用 Spring Boot 3.5.x，官方文档要求 Java 17 及以上，同时 Maven 需要 3.6.3 或更高版本。([Home](https://docs.spring.io/spring-boot/3.5/system-requirements.html))

### Maven 依赖配置

HBase 官方参考文档建议 Java 应用通过 Maven 连接 HBase 集群时使用 `hbase-shaded-client`，这样可以减少底层 Hadoop、Netty、Guava、Protobuf 等依赖与 Spring Boot 项目依赖之间的冲突。([hbase.apache.org](https://hbase.apache.org/book.html))

文件位置：`pom.xml`

```xml
<properties>
    <!-- Java 版本，Spring Boot 3 要求至少 Java 17 -->
    <java.version>17</java.version>

    <!-- HBase 客户端版本，需要与服务端 HBase 主版本保持一致 -->
    <hbase.version>2.6.4-hadoop3</hbase.version>

    <!-- Hutool 工具包版本，用于字符串、集合、对象等通用工具处理 -->
    <hutool.version>5.8.40</hutool.version>
</properties>

<dependencies>
    <!-- Spring Web，用于提供 REST API 接口 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验，用于 Controller 入参和配置属性校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- HBase Shaded Client，推荐用于连接 HBase 集群，降低依赖冲突风险 -->
    <dependency>
        <groupId>org.apache.hbase</groupId>
        <artifactId>hbase-shaded-client</artifactId>
        <version>${hbase.version}</version>
    </dependency>

    <!-- Hutool 工具类，后续工具类封装中用于字符串、集合、对象判断 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok，简化实体类、配置类、DTO、VO 和日志对象编写 -->
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
</dependencies>
```

`2.6.4-hadoop3` 属于 Maven Central 中可用的 HBase 2.6.x Hadoop 3 构建版本。([repo1.maven.org](https://repo1.maven.org/maven2/org/apache/hbase/hbase-shaded-client/2.6.4-hadoop3/?utm_source=chatgpt.com)) 如果 HBase 集群不是 Hadoop 3 版本，可以根据服务端实际版本改为对应的 `2.6.x` 或 `2.5.x` 客户端版本；HBase 客户端版本原则上应与服务端主版本一致，至少保持同一个大版本系列。

如果项目中已经存在 Hadoop、ZooKeeper、Guava、Netty、Protobuf 等依赖，优先使用 `hbase-shaded-client`。如果公司内部已经统一管理 Hadoop/HBase 依赖版本，也可以使用 `hbase-client`，但需要额外处理依赖冲突。

### HBase 客户端配置

HBase 客户端配置可以通过两种方式提供。第一种是将 `hbase-site.xml` 放入项目 `classpath`，例如 `src/main/resources/hbase-site.xml`。第二种是在 Spring Boot 的 `application.yml` 中配置自定义属性，再由配置类创建 HBase `Configuration` 对象。

HBase 官方文档说明，Java 客户端配置保存在 `HBaseConfiguration` 中，`HBaseConfiguration.create()` 会读取 classpath 中的第一个 `hbase-site.xml`，也可以通过代码直接设置 ZooKeeper 地址等配置。([hbase.apache.org](https://hbase.apache.org/book.html))

推荐在 Spring Boot 项目中使用 `application.yml` 管理环境差异：

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: springboot-hbase-demo

hbase:
  # 是否启用 HBase 客户端，便于本地开发时临时关闭
  enabled: true

  # ZooKeeper 集群地址，多个节点使用英文逗号分隔
  zookeeper-quorum: hbase-zk01,hbase-zk02,hbase-zk03

  # ZooKeeper 客户端端口，HBase 默认值通常为 2181
  zookeeper-client-port: 2181

  # HBase 在 ZooKeeper 中的根节点，默认一般为 /hbase
  znode-parent: /hbase

  # HBase 客户端操作超时时间，单位：毫秒
  operation-timeout: 30000

  # HBase RPC 调用超时时间，单位：毫秒
  rpc-timeout: 30000

  # HBase 客户端重试次数，生产环境可按集群稳定性调整
  retries-number: 3
```

如果团队更倾向于使用原生 HBase 配置文件，也可以放置 `hbase-site.xml`：

文件位置：`src/main/resources/hbase-site.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- ZooKeeper 集群地址，多个节点使用英文逗号分隔 -->
    <property>
        <name>hbase.zookeeper.quorum</name>
        <value>hbase-zk01,hbase-zk02,hbase-zk03</value>
    </property>

    <!-- ZooKeeper 客户端端口，默认通常为 2181 -->
    <property>
        <name>hbase.zookeeper.property.clientPort</name>
        <value>2181</value>
    </property>

    <!-- HBase 在 ZooKeeper 中的根节点 -->
    <property>
        <name>zookeeper.znode.parent</name>
        <value>/hbase</value>
    </property>

    <!-- 客户端操作超时时间，单位：毫秒 -->
    <property>
        <name>hbase.client.operation.timeout</name>
        <value>30000</value>
    </property>

    <!-- RPC 调用超时时间，单位：毫秒 -->
    <property>
        <name>hbase.rpc.timeout</name>
        <value>30000</value>
    </property>

    <!-- 客户端失败重试次数 -->
    <property>
        <name>hbase.client.retries.number</name>
        <value>3</value>
    </property>
</configuration>
```

实际项目中建议二选一，不要同时维护两套来源相同但值可能不同的配置。若使用 `application.yml`，后续配置类中显式构建 `HBaseConfiguration`；若使用 `hbase-site.xml`，则确保该文件被打入应用 classpath，并随不同环境进行配置替换。

生产环境还需要额外确认以下事项：Spring Boot 应用服务器必须能访问 ZooKeeper 端口和 HBase RegionServer；DNS 或 hosts 解析必须能解析 HBase 返回的节点主机名；如果集群开启 Kerberos，还需要配置 `hbase.client.keytab.file` 和 `hbase.client.kerberos.principal`，HBase 2.6 API 文档说明 `ConnectionFactory` 创建的连接支持通过这两个配置连接 Kerberos 集群。([hbase.apache.org](https://hbase.apache.org/2.6/devapidocs/org/apache/hadoop/hbase/client/ConnectionFactory.html))



## 项目结构设计

本节用于说明 Spring Boot 3 集成 HBase 时推荐的工程分层。HBase 原生 API 不建议散落在 Controller 或业务代码中，应通过配置层、工具层、数据访问层和业务服务层逐级封装，保证连接管理、异常处理、日志记录和业务逻辑边界清晰。

推荐项目结构如下：

```text
springboot-hbase-demo
├── pom.xml
└── src
    └── main
        ├── java
        │   └── io
        │       └── github
        │           └── atengk
        │               └── hbase
        │                   ├── HBaseApplication.java
        │                   ├── config
        │                   │   ├── HBaseClientConfig.java
        │                   │   └── HBaseProperties.java
        │                   ├── core
        │                   │   └── HBaseAccessTemplate.java
        │                   ├── exception
        │                   │   └── HBaseClientException.java
        │                   ├── repository
        │                   │   └── HBaseTableRepository.java
        │                   └── service
        │                       ├── HBaseTableService.java
        │                       └── impl
        │                           └── HBaseTableServiceImpl.java
        └── resources
            ├── application.yml
            └── hbase-site.xml
```

其中 `application.yml` 和 `hbase-site.xml` 通常二选一。若使用 Spring Boot 配置方式，推荐通过 `application.yml` 管理不同环境的 HBase 地址；若已有统一的 Hadoop/HBase 客户端配置文件，则可以将 `hbase-site.xml` 放入 `classpath` 中。

### 配置类结构

配置类主要负责读取 HBase 客户端参数，构建 Hadoop `Configuration`，并初始化 HBase `Connection`。该层只处理基础设施配置，不直接编写表创建、数据写入、数据查询等业务逻辑。

建议配置类拆分为两个文件：

| 文件                     | 作用                                            |
| ------------------------ | ----------------------------------------------- |
| `HBaseProperties.java`   | 读取 `application.yml` 中的 `hbase` 配置项      |
| `HBaseClientConfig.java` | 创建 HBase `Configuration` 和 `Connection` Bean |

`HBaseProperties` 用于承接配置文件中的 HBase 参数，并通过 Spring Boot 配置绑定机制注入。

文件位置：`src/main/java/io/github/atengk/hbase/config/HBaseProperties.java`

下面的代码用于绑定 `application.yml` 中 `hbase` 前缀下的配置参数，并提供基础校验。

```java
package io.github.atengk.hbase.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * HBase 客户端配置属性。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Validated
@ConfigurationProperties(prefix = "hbase")
public class HBaseProperties {

    /**
     * 是否启用 HBase 客户端。
     */
    private Boolean enabled = true;

    /**
     * ZooKeeper 集群地址，多个节点使用英文逗号分隔。
     */
    @NotBlank(message = "HBase ZooKeeper 地址不能为空")
    private String zookeeperQuorum;

    /**
     * ZooKeeper 客户端端口。
     */
    @Min(value = 1, message = "ZooKeeper 客户端端口必须大于 0")
    private Integer zookeeperClientPort = 2181;

    /**
     * HBase 在 ZooKeeper 中的根节点。
     */
    private String znodeParent = "/hbase";

    /**
     * HBase 客户端操作超时时间，单位：毫秒。
     */
    @Min(value = 1000, message = "HBase 操作超时时间不能小于 1000 毫秒")
    private Integer operationTimeout = 30000;

    /**
     * HBase RPC 调用超时时间，单位：毫秒。
     */
    @Min(value = 1000, message = "HBase RPC 超时时间不能小于 1000 毫秒")
    private Integer rpcTimeout = 30000;

    /**
     * HBase 客户端失败重试次数。
     */
    @Min(value = 0, message = "HBase 重试次数不能小于 0")
    private Integer retriesNumber = 3;

}
```

`HBaseClientConfig` 负责将 Spring Boot 配置转换为 HBase 客户端可识别的 Hadoop `Configuration`，并创建全局复用的 `Connection`。

文件位置：`src/main/java/io/github/atengk/hbase/config/HBaseClientConfig.java`

下面的代码用于初始化 HBase 客户端核心 Bean，应用关闭时会自动关闭 `Connection`。

```java
package io.github.atengk.hbase.config;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

/**
 * HBase 客户端核心配置。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@org.springframework.context.annotation.Configuration
@EnableConfigurationProperties(HBaseProperties.class)
@ConditionalOnProperty(prefix = "hbase", name = "enabled", havingValue = "true", matchIfMissing = true)
public class HBaseClientConfig {

    /**
     * 创建 HBase 客户端配置对象。
     *
     * @param properties HBase 配置属性
     * @return HBase 客户端配置对象
     */
    @Bean
    public org.apache.hadoop.conf.Configuration hbaseConfiguration(HBaseProperties properties) {
        org.apache.hadoop.conf.Configuration configuration = HBaseConfiguration.create();

        String znodeParent = StrUtil.blankToDefault(properties.getZnodeParent(), "/hbase");

        configuration.set("hbase.zookeeper.quorum", properties.getZookeeperQuorum());
        configuration.setInt("hbase.zookeeper.property.clientPort", properties.getZookeeperClientPort());
        configuration.set("zookeeper.znode.parent", znodeParent);
        configuration.setInt("hbase.client.operation.timeout", properties.getOperationTimeout());
        configuration.setInt("hbase.rpc.timeout", properties.getRpcTimeout());
        configuration.setInt("hbase.client.retries.number", properties.getRetriesNumber());

        log.info("初始化 HBase Configuration，zookeeperQuorum={}，clientPort={}，znodeParent={}",
                properties.getZookeeperQuorum(),
                properties.getZookeeperClientPort(),
                znodeParent);

        return configuration;
    }

    /**
     * 创建 HBase 连接对象。
     *
     * @param hbaseConfiguration HBase 客户端配置对象
     * @return HBase 连接对象
     * @throws IOException HBase 连接创建异常
     */
    @Bean(destroyMethod = "close")
    public Connection hbaseConnection(org.apache.hadoop.conf.Configuration hbaseConfiguration) throws IOException {
        log.info("开始初始化 HBase Connection");
        Connection connection = ConnectionFactory.createConnection(hbaseConfiguration);
        log.info("HBase Connection 初始化完成");
        return connection;
    }

}
```

### 工具类结构

工具类结构主要用于封装 HBase 原生对象的获取和释放逻辑。HBase 的 `Table` 和 `Admin` 对象使用后需要关闭，因此不建议在业务层手动反复写 `try-with-resources`，更适合封装为模板方法。

建议工具类放在 `core` 包中：

| 文件                        | 作用                                              |
| --------------------------- | ------------------------------------------------- |
| `HBaseAccessTemplate.java`  | 封装 `Admin` 和 `Table` 获取、关闭、异常转换      |
| `HBaseClientException.java` | 将底层 `IOException` 转换为业务可识别的运行时异常 |

文件位置：`src/main/java/io/github/atengk/hbase/exception/HBaseClientException.java`

下面的代码用于封装 HBase 客户端异常，避免业务层直接感知底层 `IOException`。

```java
package io.github.atengk.hbase.exception;

/**
 * HBase 客户端异常。
 *
 * @author Ateng
 * @since 2026-05-07
 */
public class HBaseClientException extends RuntimeException {

    /**
     * 创建 HBase 客户端异常。
     *
     * @param message 异常消息
     */
    public HBaseClientException(String message) {
        super(message);
    }

    /**
     * 创建 HBase 客户端异常。
     *
     * @param message 异常消息
     * @param cause   原始异常
     */
    public HBaseClientException(String message, Throwable cause) {
        super(message, cause);
    }

}
```

文件位置：`src/main/java/io/github/atengk/hbase/core/HBaseAccessTemplate.java`

下面的代码用于统一执行 HBase `Admin` 和 `Table` 操作，并自动释放相关资源。

```java
package io.github.atengk.hbase.core;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.hbase.exception.HBaseClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Table;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * HBase 访问模板。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HBaseAccessTemplate {

    private final Connection hbaseConnection;

    /**
     * 执行 HBase Admin 操作。
     *
     * @param callback Admin 回调
     * @param <T>      返回结果类型
     * @return 操作结果
     */
    public <T> T executeAdmin(AdminCallback<T> callback) {
        try (Admin admin = hbaseConnection.getAdmin()) {
            return callback.doInAdmin(admin);
        } catch (IOException e) {
            log.error("HBase Admin 操作失败", e);
            throw new HBaseClientException("HBase Admin 操作失败", e);
        }
    }

    /**
     * 执行 HBase Table 操作。
     *
     * @param tableName 表名
     * @param callback  Table 回调
     * @param <T>       返回结果类型
     * @return 操作结果
     */
    public <T> T executeTable(String tableName, TableCallback<T> callback) {
        if (StrUtil.isBlank(tableName)) {
            throw new HBaseClientException("HBase 表名不能为空");
        }

        TableName hbaseTableName = TableName.valueOf(tableName);

        try (Table table = hbaseConnection.getTable(hbaseTableName)) {
            return callback.doInTable(table);
        } catch (IOException e) {
            log.error("HBase Table 操作失败，tableName={}", tableName, e);
            throw new HBaseClientException("HBase Table 操作失败：" + tableName, e);
        }
    }

    /**
     * 获取 HBase Table 操作对象。
     *
     * @param tableName 表名
     * @return Table 操作对象
     */
    public Table getTable(String tableName) {
        if (StrUtil.isBlank(tableName)) {
            throw new HBaseClientException("HBase 表名不能为空");
        }

        try {
            return hbaseConnection.getTable(TableName.valueOf(tableName));
        } catch (IOException e) {
            log.error("获取 HBase Table 对象失败，tableName={}", tableName, e);
            throw new HBaseClientException("获取 HBase Table 对象失败：" + tableName, e);
        }
    }

    /**
     * Admin 操作回调。
     *
     * @param <T> 返回结果类型
     */
    @FunctionalInterface
    public interface AdminCallback<T> {

        /**
         * 执行 Admin 操作。
         *
         * @param admin HBase Admin 对象
         * @return 操作结果
         * @throws IOException HBase 操作异常
         */
        T doInAdmin(Admin admin) throws IOException;

    }

    /**
     * Table 操作回调。
     *
     * @param <T> 返回结果类型
     */
    @FunctionalInterface
    public interface TableCallback<T> {

        /**
         * 执行 Table 操作。
         *
         * @param table HBase Table 对象
         * @return 操作结果
         * @throws IOException HBase 操作异常
         */
        T doInTable(Table table) throws IOException;

    }

}
```

`executeTable` 是推荐使用方式，它会自动关闭 `Table`。`getTable` 只适合特殊场景，例如需要把 `Table` 交给更底层的批处理逻辑，此时调用方必须自行关闭 `Table`，否则可能造成连接资源泄露。

### 数据访问层结构

数据访问层负责操作 HBase 原生 API，例如判断表是否存在、创建表、查询行数据、写入数据和删除数据。该层不处理 Controller 入参，不拼接接口返回结果，也不编写复杂业务规则。

建议数据访问层只依赖 `HBaseAccessTemplate`，不直接注入 `Connection`。这样可以统一复用异常处理、日志记录和资源关闭逻辑。

文件位置：`src/main/java/io/github/atengk/hbase/repository/HBaseTableRepository.java`

下面的代码给出数据访问层的基础结构，先实现表是否存在的判断，后续创建表、删除表等操作可继续放在该类中。

```java
package io.github.atengk.hbase.repository;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.hbase.core.HBaseAccessTemplate;
import io.github.atengk.hbase.exception.HBaseClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.TableName;
import org.springframework.stereotype.Repository;

/**
 * HBase 表数据访问对象。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class HBaseTableRepository {

    private final HBaseAccessTemplate hbaseAccessTemplate;

    /**
     * 判断 HBase 表是否存在。
     *
     * @param tableName 表名
     * @return true 表示存在，false 表示不存在
     */
    public boolean existsTable(String tableName) {
        if (StrUtil.isBlank(tableName)) {
            throw new HBaseClientException("HBase 表名不能为空");
        }

        return hbaseAccessTemplate.executeAdmin(admin -> {
            boolean exists = admin.tableExists(TableName.valueOf(tableName));
            log.info("检查 HBase 表是否存在，tableName={}，exists={}", tableName, exists);
            return exists;
        });
    }

}
```

后续基础操作章节可以继续在 `repository` 层扩展如下方法：

```java
public void createTable(String tableName, List<String> columnFamilies);
public void deleteTable(String tableName);
public void putRow(String tableName, String rowKey, String columnFamily, String qualifier, String value);
public Map<String, Object> getRow(String tableName, String rowKey);
public List<Map<String, Object>> scan(String tableName, String startRow, String stopRow);
public void deleteRow(String tableName, String rowKey);
```

### 业务服务层结构

业务服务层负责承接业务语义，对外提供更稳定的方法定义。它可以调用一个或多个 Repository，并负责业务参数校验、业务日志、异常语义转换和事务边界设计。

对于 HBase 来说，常规写入和查询通常不使用关系型数据库事务，因此业务服务层更关注参数合法性、RowKey 规则、列族约束、数据转换和调用编排。

文件位置：`src/main/java/io/github/atengk/hbase/service/HBaseTableService.java`

下面的代码定义表管理相关业务接口。

```java
package io.github.atengk.hbase.service;

/**
 * HBase 表业务服务。
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface HBaseTableService {

    /**
     * 判断 HBase 表是否存在。
     *
     * @param tableName 表名
     * @return true 表示存在，false 表示不存在
     */
    boolean existsTable(String tableName);

}
```

文件位置：`src/main/java/io/github/atengk/hbase/service/impl/HBaseTableServiceImpl.java`

下面的代码实现表管理业务服务，并将具体 HBase 访问委托给 Repository。

```java
package io.github.atengk.hbase.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.hbase.exception.HBaseClientException;
import io.github.atengk.hbase.repository.HBaseTableRepository;
import io.github.atengk.hbase.service.HBaseTableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * HBase 表业务服务实现。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HBaseTableServiceImpl implements HBaseTableService {

    private final HBaseTableRepository hbaseTableRepository;

    /**
     * 判断 HBase 表是否存在。
     *
     * @param tableName 表名
     * @return true 表示存在，false 表示不存在
     */
    @Override
    public boolean existsTable(String tableName) {
        if (StrUtil.isBlank(tableName)) {
            throw new HBaseClientException("HBase 表名不能为空");
        }

        log.info("开始检查 HBase 表状态，tableName={}", tableName);
        return hbaseTableRepository.existsTable(tableName);
    }

}
```

业务服务层不要直接依赖 `Admin`、`Table`、`Put`、`Get`、`Scan` 等 HBase 原生对象。这样可以避免业务代码与 HBase 客户端强耦合，也方便后续切换封装方式或增加统一监控。

## 核心配置

核心配置用于完成 HBase 客户端连接初始化。Spring Boot 启动后，应创建一个全局复用的 `Connection`，后续通过该连接获取 `Admin` 和 `Table` 对象。`Connection` 是重量级对象，不建议在每次请求中重复创建。

### HBase 连接配置

HBase 连接配置建议统一写入 `application.yml`，由 `HBaseProperties` 读取，并在 `HBaseClientConfig` 中转换为 Hadoop `Configuration`。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: springboot-hbase-demo

hbase:
  # 是否启用 HBase 客户端
  enabled: true

  # ZooKeeper 集群地址，多个节点使用英文逗号分隔
  zookeeper-quorum: hbase-zk01,hbase-zk02,hbase-zk03

  # ZooKeeper 客户端端口
  zookeeper-client-port: 2181

  # HBase 在 ZooKeeper 中的根节点
  znode-parent: /hbase

  # HBase 客户端操作超时时间，单位：毫秒
  operation-timeout: 30000

  # HBase RPC 调用超时时间，单位：毫秒
  rpc-timeout: 30000

  # HBase 客户端失败重试次数
  retries-number: 3
```

配置项说明如下：

| 配置项                        | 说明                                        |
| ----------------------------- | ------------------------------------------- |
| `hbase.enabled`               | 是否启用 HBase 自动配置，本地开发可临时关闭 |
| `hbase.zookeeper-quorum`      | ZooKeeper 地址，多个节点使用英文逗号分隔    |
| `hbase.zookeeper-client-port` | ZooKeeper 客户端端口，常用值为 `2181`       |
| `hbase.znode-parent`          | HBase 在 ZooKeeper 中的根节点               |
| `hbase.operation-timeout`     | HBase 客户端操作超时时间                    |
| `hbase.rpc-timeout`           | HBase RPC 调用超时时间                      |
| `hbase.retries-number`        | 客户端失败重试次数                          |

本地开发时，如果暂时没有 HBase 环境，可以设置：

```yaml
hbase:
  enabled: false
```

关闭后，`HBaseClientConfig` 不会创建 HBase 相关 Bean。但如果业务类强依赖 `HBaseAccessTemplate`、`HBaseTableRepository` 或 `HBaseTableService`，还需要通过 `@ConditionalOnProperty` 同步控制这些 Bean 的创建，或在本地 Profile 中禁用相关接口。

### Connection 初始化

`Connection` 是 HBase 客户端访问集群的核心入口，项目中应尽量保持全局单例复用。Spring Boot 中推荐通过 `@Bean(destroyMethod = "close")` 管理连接生命周期，应用关闭时由 Spring 容器自动调用 `close` 方法释放资源。

核心初始化逻辑如下：

```java
@Bean(destroyMethod = "close")
public Connection hbaseConnection(org.apache.hadoop.conf.Configuration hbaseConfiguration) throws IOException {
    log.info("开始初始化 HBase Connection");
    Connection connection = ConnectionFactory.createConnection(hbaseConfiguration);
    log.info("HBase Connection 初始化完成");
    return connection;
}
```

该 Bean 在项目启动时创建。如果 ZooKeeper 地址错误、网络不通、HBase 服务不可用或认证配置不正确，启动阶段可能出现连接异常。生产环境建议将 HBase 配置独立到测试环境和生产环境 Profile 中，避免不同环境误连。

常见 Profile 配置示例：

文件位置：`src/main/resources/application-dev.yml`

```yaml
hbase:
  # 开发环境 HBase 地址
  zookeeper-quorum: dev-hbase-zk01,dev-hbase-zk02,dev-hbase-zk03
  zookeeper-client-port: 2181
  znode-parent: /hbase
  operation-timeout: 30000
  rpc-timeout: 30000
  retries-number: 3
```

文件位置：`src/main/resources/application-prod.yml`

```yaml
hbase:
  # 生产环境 HBase 地址
  zookeeper-quorum: prod-hbase-zk01,prod-hbase-zk02,prod-hbase-zk03
  zookeeper-client-port: 2181
  znode-parent: /hbase
  operation-timeout: 60000
  rpc-timeout: 60000
  retries-number: 5
```

启动时指定环境：

```bash
# 使用开发环境配置启动
java -jar springboot-hbase-demo.jar --spring.profiles.active=dev

# 使用生产环境配置启动
java -jar springboot-hbase-demo.jar --spring.profiles.active=prod
```

`--spring.profiles.active` 用于指定 Spring Boot 当前激活的配置环境。实际部署时也可以通过环境变量 `SPRING_PROFILES_ACTIVE=prod` 指定，避免把环境信息写死在启动脚本中。

### Admin 初始化

`Admin` 用于执行 HBase 表级管理操作，例如创建表、删除表、禁用表、启用表、判断表是否存在等。普通数据写入和查询不需要使用 `Admin`，只需要使用 `Table`。

推荐做法是通过 `Connection` 按需获取 `Admin`，并在使用结束后立即关闭：

```java
public <T> T executeAdmin(AdminCallback<T> callback) {
    try (Admin admin = hbaseConnection.getAdmin()) {
        return callback.doInAdmin(admin);
    } catch (IOException e) {
        log.error("HBase Admin 操作失败", e);
        throw new HBaseClientException("HBase Admin 操作失败", e);
    }
}
```

不建议在普通业务服务中长期持有 `Admin` 对象。表管理操作通常频率较低，按需创建并关闭更清晰，也更容易避免资源泄露。

使用示例：

```java
public boolean existsTable(String tableName) {
    return hbaseAccessTemplate.executeAdmin(admin ->
            admin.tableExists(TableName.valueOf(tableName))
    );
}
```

如果项目确实需要将 `Admin` 注入为 Bean，也可以使用下面方式，但需要明确它只用于低频管理操作：

```java
@Bean(destroyMethod = "close")
public Admin hbaseAdmin(Connection hbaseConnection) throws IOException {
    log.info("初始化 HBase Admin");
    return hbaseConnection.getAdmin();
}
```

更推荐本文前面的 `HBaseAccessTemplate#executeAdmin` 方式，因为它能保证每次操作后自动关闭 `Admin`，也能统一异常转换和日志记录。

### Table 操作对象获取

`Table` 用于执行具体表的数据操作，例如 `put`、`get`、`scan`、`delete`、`batch` 等。`Table` 对象不建议长期缓存，也不建议作为单例 Bean 注入。每次操作时通过 `Connection#getTable` 获取，使用完成后关闭。

推荐使用模板方法获取并执行：

```java
public <T> T executeTable(String tableName, TableCallback<T> callback) {
    if (StrUtil.isBlank(tableName)) {
        throw new HBaseClientException("HBase 表名不能为空");
    }

    TableName hbaseTableName = TableName.valueOf(tableName);

    try (Table table = hbaseConnection.getTable(hbaseTableName)) {
        return callback.doInTable(table);
    } catch (IOException e) {
        log.error("HBase Table 操作失败，tableName={}", tableName, e);
        throw new HBaseClientException("HBase Table 操作失败：" + tableName, e);
    }
}
```

调用示例一：根据表名执行存在性检查以外的数据操作。

```java
String tableName = "default:user_profile";

String result = hbaseAccessTemplate.executeTable(tableName, table -> {
    // 这里可以执行 Get、Put、Scan、Delete 等操作
    return table.getName().getNameAsString();
});
```

调用示例二：特殊场景下手动获取 `Table`。

```java
try (Table table = hbaseAccessTemplate.getTable("default:user_profile")) {
    // 手动执行复杂批量操作
    String realTableName = table.getName().getNameAsString();
    log.info("获取 HBase Table 成功，tableName={}", realTableName);
} catch (Exception e) {
    log.error("手动操作 HBase Table 失败", e);
    throw new HBaseClientException("手动操作 HBase Table 失败", e);
}
```

实际开发中优先使用 `executeTable`，只有在复杂批处理、多个操作需要共享同一个 `Table` 对象、或需要兼容已有底层代码时，才使用 `getTable` 并手动关闭。



## 表结构设计

HBase 表结构设计需要围绕查询模式展开，而不是围绕传统关系型数据库的实体关系展开。设计前应先明确数据按什么维度写入、按什么 RowKey 查询、是否需要范围扫描、是否存在热点写入，以及列族是否需要长期稳定。

### 命名空间设计

命名空间用于对 HBase 表进行逻辑隔离，作用类似关系型数据库中的 schema。建议按照业务域、系统模块或环境进行划分，避免所有表都堆放在 `default` 命名空间下。

推荐命名规则如下：

| 命名空间  | 适用场景             | 示例                   |
| --------- | -------------------- | ---------------------- |
| `default` | 本地测试、临时验证   | `default:user_profile` |
| `app`     | 应用业务数据         | `app:user_profile`     |
| `log`     | 日志、行为、埋点数据 | `log:user_event`       |
| `iot`     | 设备采集、时序数据   | `iot:device_metric`    |
| `risk`    | 风控、审计、流水数据 | `risk:risk_event`      |

命名空间建议使用小写字母和下划线，不建议使用中文、空格、特殊符号。生产环境中，命名空间应提前规划，并与权限、备份、生命周期策略保持一致。

示例：

```text
app:user_profile
log:user_event
iot:device_metric
risk:risk_record
```

### 表名设计

HBase 表名由命名空间和表名组成，完整格式为：

```text
namespace:table_name
```

表名应表达数据主题，而不是表达接口名称或临时功能名称。建议采用小写下划线格式，避免使用驼峰、中文或过长名称。

推荐命名方式：

| 类型     | 表名示例            | 说明                               |
| -------- | ------------------- | ---------------------------------- |
| 用户画像 | `app:user_profile`  | 存储用户基础画像、标签、扩展属性   |
| 用户行为 | `log:user_event`    | 存储用户点击、浏览、下单等行为事件 |
| 设备指标 | `iot:device_metric` | 存储设备采集指标                   |
| 风控记录 | `risk:risk_record`  | 存储风控识别结果和审计信息         |
| 订单轨迹 | `app:order_trace`   | 存储订单状态流转明细               |

不建议将日期直接写入表名，例如 `user_event_20260507`。如果确实需要按时间归档，应结合业务数据量、TTL、Region 规划和运维策略进行统一设计，而不是由业务代码随意创建大量日期表。

### RowKey 设计

RowKey 是 HBase 表设计中最关键的部分。HBase 按 RowKey 字典序存储数据，RowKey 设计会直接影响查询效率、扫描效率和写入热点。

RowKey 设计原则如下：

| 原则           | 说明                                           |
| -------------- | ---------------------------------------------- |
| 与查询条件匹配 | 高频查询字段应尽量进入 RowKey                  |
| 避免单调递增   | 避免时间戳、自增 ID 直接作为 RowKey 前缀       |
| 控制长度       | RowKey 不宜过长，建议保持稳定、紧凑            |
| 保持可排序     | 需要范围扫描时，应让 RowKey 的排序符合查询需求 |
| 增加散列前缀   | 高并发写入场景可增加 hash/salt 前缀避免热点    |

常见 RowKey 设计示例：

| 场景     | RowKey 示例                               | 说明                     |
| -------- | ----------------------------------------- | ------------------------ |
| 用户画像 | `userId`                                  | 适合按用户 ID 精确查询   |
| 用户行为 | `hash(userId)_userId_reverseTime_eventId` | 适合按用户和时间倒序查询 |
| 设备指标 | `hash(deviceId)_deviceId_timestamp`       | 适合按设备和时间范围扫描 |
| 订单轨迹 | `orderId_timestamp_status`                | 适合按订单 ID 查询轨迹   |
| 风控记录 | `hash(userId)_userId_eventTime_riskType`  | 适合按用户查询风控明细   |

用户行为 RowKey 示例：

```text
03_10001_8295638399000_evt_202605070001
```

其中：

| 片段               | 说明                                         |
| ------------------ | -------------------------------------------- |
| `03`               | 对用户 ID 取 hash 后的分桶前缀，用于分散写入 |
| `10001`            | 用户 ID                                      |
| `8295638399000`    | 反转时间戳，用于倒序查询                     |
| `evt_202605070001` | 事件唯一 ID                                  |

反转时间戳可以使用 `Long.MAX_VALUE - 当前时间戳` 生成，使最新数据在 RowKey 排序中更靠前。

### 列族设计

列族是 HBase 物理存储和压缩的基本单位，创建表时必须提前定义。列族数量不宜过多，通常建议 1 到 3 个。列族过多会增加存储文件、Compaction 和读写维护成本。

推荐列族设计如下：

| 列族   | 适用数据           | 示例                         |
| ------ | ------------------ | ---------------------------- |
| `info` | 基础信息、核心字段 | 用户名、手机号、设备型号     |
| `ext`  | 扩展属性、低频字段 | 标签、备注、扩展 JSON        |
| `stat` | 统计指标           | 次数、金额、评分             |
| `meta` | 元数据             | 创建时间、更新时间、来源系统 |

用户画像表示例：

```text
表名：app:user_profile

列族：
- info：用户基础信息
- ext：用户扩展属性
- meta：数据元信息
```

用户行为表示例：

```text
表名：log:user_event

列族：
- info：事件基础信息
- ext：事件扩展参数
```

列族一旦创建后虽然可以修改，但不建议频繁调整。生产环境建表前应尽量确定列族边界，避免后续频繁变更表结构。

### 列限定符设计

列限定符是列族下的具体字段名称。HBase 支持动态列，因此列限定符不需要像关系型数据库字段一样提前建好，但仍需要统一命名规范。

推荐列限定符设计如下：

| 列族   | 列限定符      | 说明          |
| ------ | ------------- | ------------- |
| `info` | `name`        | 用户名称      |
| `info` | `phone`       | 手机号        |
| `info` | `event_type`  | 事件类型      |
| `info` | `event_time`  | 事件时间      |
| `ext`  | `tags`        | 标签 JSON     |
| `ext`  | `properties`  | 扩展参数 JSON |
| `meta` | `create_time` | 创建时间      |
| `meta` | `update_time` | 更新时间      |

列限定符建议保持短小、稳定、语义清晰。对于不固定字段，可以将扩展属性序列化为 JSON 字符串放入 `ext:properties`，避免过度膨胀列数量。

示例数据结构：

```text
RowKey: 10001

info:name        = 张三
info:phone       = 13800000000
info:gender      = male
ext:tags         = ["vip","active"]
meta:create_time = 2026-05-07 10:00:00
```

## 基础操作开发

本节基于前文已经创建的 `HBaseAccessTemplate`，实现 HBase 表管理和数据操作。基础操作建议放在 Repository 层，由 Service 层按业务语义进行调用，不建议在 Controller 层直接操作 HBase 原生 API。

本节完整实现以下能力：

| 操作                 | 方法          |
| -------------------- | ------------- |
| 创建表               | `createTable` |
| 删除表               | `deleteTable` |
| 判断表是否存在       | `existsTable` |
| 新增数据             | `putRow`      |
| 根据 RowKey 查询数据 | `getRow`      |
| 批量查询数据         | `getRows`     |
| 扫描表数据           | `scanRows`    |
| 删除数据             | `deleteRow`   |

文件位置：`src/main/java/io/github/atengk/hbase/repository/HBaseTableRepository.java`

下面的代码封装 HBase 表管理和基础数据操作，业务层可直接调用该 Repository。

```java
package io.github.atengk.hbase.repository;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.hbase.core.HBaseAccessTemplate;
import io.github.atengk.hbase.exception.HBaseClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.NamespaceNotFoundException;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptor;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.apache.hadoop.hbase.NamespaceDescriptor.DEFAULT_NAMESPACE_NAME_STR;

/**
 * HBase 表数据访问对象。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class HBaseTableRepository {

    private final HBaseAccessTemplate hbaseAccessTemplate;

    /**
     * 创建 HBase 表。
     *
     * @param tableName      表名，格式为 namespace:table
     * @param columnFamilies 列族列表
     */
    public void createTable(String tableName, List<String> columnFamilies) {
        checkTableName(tableName);

        if (CollUtil.isEmpty(columnFamilies)) {
            throw new HBaseClientException("HBase 列族不能为空");
        }

        hbaseAccessTemplate.executeAdmin(admin -> {
            TableName hbaseTableName = TableName.valueOf(tableName);

            if (admin.tableExists(hbaseTableName)) {
                log.info("HBase 表已存在，跳过创建，tableName={}", tableName);
                return null;
            }

            createNamespaceIfNecessary(hbaseTableName);

            TableDescriptorBuilder tableDescriptorBuilder = TableDescriptorBuilder.newBuilder(hbaseTableName);

            for (String columnFamily : columnFamilies) {
                if (StrUtil.isBlank(columnFamily)) {
                    throw new HBaseClientException("HBase 列族名称不能为空");
                }

                ColumnFamilyDescriptor columnFamilyDescriptor = ColumnFamilyDescriptorBuilder
                        .newBuilder(Bytes.toBytes(columnFamily))
                        .build();

                tableDescriptorBuilder.setColumnFamily(columnFamilyDescriptor);
            }

            TableDescriptor tableDescriptor = tableDescriptorBuilder.build();
            admin.createTable(tableDescriptor);

            log.info("创建 HBase 表成功，tableName={}，columnFamilies={}", tableName, columnFamilies);
            return null;
        });
    }

    /**
     * 删除 HBase 表。
     *
     * @param tableName 表名，格式为 namespace:table
     */
    public void deleteTable(String tableName) {
        checkTableName(tableName);

        hbaseAccessTemplate.executeAdmin(admin -> {
            TableName hbaseTableName = TableName.valueOf(tableName);

            if (!admin.tableExists(hbaseTableName)) {
                log.info("HBase 表不存在，跳过删除，tableName={}", tableName);
                return null;
            }

            if (admin.isTableEnabled(hbaseTableName)) {
                admin.disableTable(hbaseTableName);
                log.info("禁用 HBase 表成功，tableName={}", tableName);
            }

            admin.deleteTable(hbaseTableName);
            log.info("删除 HBase 表成功，tableName={}", tableName);
            return null;
        });
    }

    /**
     * 判断 HBase 表是否存在。
     *
     * @param tableName 表名，格式为 namespace:table
     * @return true 表示存在，false 表示不存在
     */
    public boolean existsTable(String tableName) {
        checkTableName(tableName);

        return hbaseAccessTemplate.executeAdmin(admin -> {
            boolean exists = admin.tableExists(TableName.valueOf(tableName));
            log.info("检查 HBase 表是否存在，tableName={}，exists={}", tableName, exists);
            return exists;
        });
    }

    /**
     * 新增或覆盖一行数据。
     *
     * @param tableName    表名，格式为 namespace:table
     * @param rowKey       RowKey
     * @param columnFamily 列族
     * @param data         列限定符和值
     */
    public void putRow(String tableName, String rowKey, String columnFamily, Map<String, String> data) {
        checkTableName(tableName);
        checkRowKey(rowKey);

        if (StrUtil.isBlank(columnFamily)) {
            throw new HBaseClientException("HBase 列族不能为空");
        }
        if (CollUtil.isEmpty(data)) {
            throw new HBaseClientException("HBase 写入数据不能为空");
        }

        hbaseAccessTemplate.executeTable(tableName, table -> {
            Put put = new Put(Bytes.toBytes(rowKey));

            data.forEach((qualifier, value) -> {
                if (StrUtil.isBlank(qualifier)) {
                    throw new HBaseClientException("HBase 列限定符不能为空");
                }
                if (value != null) {
                    put.addColumn(
                            Bytes.toBytes(columnFamily),
                            Bytes.toBytes(qualifier),
                            Bytes.toBytes(value)
                    );
                }
            });

            if (put.isEmpty()) {
                throw new HBaseClientException("HBase 写入数据不能为空，所有字段值均为空");
            }

            table.put(put);
            log.info("写入 HBase 数据成功，tableName={}，rowKey={}，columnFamily={}",
                    tableName, rowKey, columnFamily);
            return null;
        });
    }

    /**
     * 根据 RowKey 查询一行数据。
     *
     * @param tableName 表名，格式为 namespace:table
     * @param rowKey    RowKey
     * @return 行数据
     */
    public Map<String, Object> getRow(String tableName, String rowKey) {
        checkTableName(tableName);
        checkRowKey(rowKey);

        return hbaseAccessTemplate.executeTable(tableName, table -> {
            Get get = new Get(Bytes.toBytes(rowKey));
            Result result = table.get(get);

            Map<String, Object> row = toRowMap(result);
            log.info("根据 RowKey 查询 HBase 数据完成，tableName={}，rowKey={}，empty={}",
                    tableName, rowKey, row.isEmpty());
            return row;
        });
    }

    /**
     * 根据多个 RowKey 批量查询数据。
     *
     * @param tableName 表名，格式为 namespace:table
     * @param rowKeys   RowKey 列表
     * @return 行数据列表
     */
    public List<Map<String, Object>> getRows(String tableName, List<String> rowKeys) {
        checkTableName(tableName);

        if (CollUtil.isEmpty(rowKeys)) {
            throw new HBaseClientException("HBase 批量查询 RowKey 不能为空");
        }

        List<Get> gets = rowKeys.stream()
                .filter(StrUtil::isNotBlank)
                .map(rowKey -> new Get(Bytes.toBytes(rowKey)))
                .toList();

        if (CollUtil.isEmpty(gets)) {
            throw new HBaseClientException("HBase 批量查询 RowKey 不能为空");
        }

        return hbaseAccessTemplate.executeTable(tableName, table -> {
            Result[] results = table.get(gets);

            List<Map<String, Object>> rows = new ArrayList<>();
            for (Result result : results) {
                Map<String, Object> row = toRowMap(result);
                if (CollUtil.isNotEmpty(row)) {
                    rows.add(row);
                }
            }

            log.info("批量查询 HBase 数据完成，tableName={}，requestSize={}，hitSize={}",
                    tableName, rowKeys.size(), rows.size());
            return rows;
        });
    }

    /**
     * 扫描 HBase 表数据。
     *
     * @param tableName 表名，格式为 namespace:table
     * @param startRow  开始 RowKey，包含该值
     * @param stopRow   结束 RowKey，不包含该值
     * @param limit     最大返回条数
     * @return 行数据列表
     */
    public List<Map<String, Object>> scanRows(String tableName, String startRow, String stopRow, int limit) {
        checkTableName(tableName);

        if (limit <= 0) {
            throw new HBaseClientException("HBase 扫描 limit 必须大于 0");
        }

        return hbaseAccessTemplate.executeTable(tableName, table -> {
            Scan scan = new Scan();

            if (StrUtil.isNotBlank(startRow)) {
                scan.withStartRow(Bytes.toBytes(startRow));
            }
            if (StrUtil.isNotBlank(stopRow)) {
                scan.withStopRow(Bytes.toBytes(stopRow));
            }

            scan.setCaching(Math.min(limit, 500));
            scan.setLimit(limit);

            List<Map<String, Object>> rows = new ArrayList<>();

            try (ResultScanner scanner = table.getScanner(scan)) {
                for (Result result : scanner) {
                    Map<String, Object> row = toRowMap(result);
                    if (CollUtil.isNotEmpty(row)) {
                        rows.add(row);
                    }
                    if (rows.size() >= limit) {
                        break;
                    }
                }
            }

            log.info("扫描 HBase 表数据完成，tableName={}，startRow={}，stopRow={}，limit={}，hitSize={}",
                    tableName, startRow, stopRow, limit, rows.size());
            return rows;
        });
    }

    /**
     * 根据 RowKey 删除一行数据。
     *
     * @param tableName 表名，格式为 namespace:table
     * @param rowKey    RowKey
     */
    public void deleteRow(String tableName, String rowKey) {
        checkTableName(tableName);
        checkRowKey(rowKey);

        hbaseAccessTemplate.executeTable(tableName, table -> {
            Delete delete = new Delete(Bytes.toBytes(rowKey));
            table.delete(delete);

            log.info("删除 HBase 行数据成功，tableName={}，rowKey={}", tableName, rowKey);
            return null;
        });
    }

    /**
     * 创建命名空间，如果命名空间已存在则跳过。
     *
     * @param tableName HBase 表名
     * @throws IOException HBase 操作异常
     */
    private void createNamespaceIfNecessary(TableName tableName) throws IOException {
        String namespace = tableName.getNamespaceAsString();

        if (StrUtil.equals(namespace, DEFAULT_NAMESPACE_NAME_STR)) {
            return;
        }

        hbaseAccessTemplate.executeAdmin(admin -> {
            try {
                admin.getNamespaceDescriptor(namespace);
                log.info("HBase 命名空间已存在，namespace={}", namespace);
            } catch (NamespaceNotFoundException e) {
                NamespaceDescriptor namespaceDescriptor = NamespaceDescriptor.create(namespace).build();
                admin.createNamespace(namespaceDescriptor);
                log.info("创建 HBase 命名空间成功，namespace={}", namespace);
            }
            return null;
        });
    }

    /**
     * 将 HBase Result 转换为 Map。
     *
     * @param result HBase 查询结果
     * @return 行数据
     */
    private Map<String, Object> toRowMap(Result result) {
        Map<String, Object> row = new LinkedHashMap<>();

        if (result == null || result.isEmpty()) {
            return row;
        }

        row.put("rowKey", Bytes.toString(result.getRow()));

        for (Cell cell : result.rawCells()) {
            String family = Bytes.toString(CellUtil.cloneFamily(cell));
            String qualifier = Bytes.toString(CellUtil.cloneQualifier(cell));
            String value = Bytes.toString(CellUtil.cloneValue(cell));

            row.put(family + ":" + qualifier, value);
        }

        return row;
    }

    /**
     * 校验表名。
     *
     * @param tableName 表名
     */
    private void checkTableName(String tableName) {
        if (StrUtil.isBlank(tableName)) {
            throw new HBaseClientException("HBase 表名不能为空");
        }
    }

    /**
     * 校验 RowKey。
     *
     * @param rowKey RowKey
     */
    private void checkRowKey(String rowKey) {
        if (StrUtil.isBlank(rowKey)) {
            throw new HBaseClientException("HBase RowKey 不能为空");
        }
    }

}
```

### 创建表

创建表时需要指定表名和列族。HBase 的列族必须在建表时定义，列限定符可以在写入数据时动态增加。

调用示例：

```java
List<String> columnFamilies = List.of("info", "ext", "meta");
hBaseTableRepository.createTable("app:user_profile", columnFamilies);
```

创建完成后，可通过 HBase Shell 验证：

```bash
# 查看表是否存在
exists 'app:user_profile'

# 查看表结构
describe 'app:user_profile'
```

`exists` 用于检查表是否存在，`describe` 用于查看表的列族、版本数、压缩、TTL 等表结构信息。

### 删除表

删除表前需要先禁用表。上面的 `deleteTable` 方法内部已经判断表是否启用，如果表处于启用状态，会先执行 `disableTable`，再执行 `deleteTable`。

调用示例：

```java
hBaseTableRepository.deleteTable("app:user_profile");
```

验证命令：

```bash
# 查看表是否还存在
exists 'app:user_profile'
```

删除表属于高风险操作，生产环境建议只在运维脚本或管理接口中开放，并增加权限校验、二次确认和操作审计日志。

### 判断表是否存在

判断表是否存在主要用于建表前校验、启动检查、运维接口和测试验证。该操作通过 `Admin#tableExists` 实现。

调用示例：

```java
boolean exists = hBaseTableRepository.existsTable("app:user_profile");
```

返回值说明：

| 返回值  | 说明     |
| ------- | -------- |
| `true`  | 表存在   |
| `false` | 表不存在 |

如果表名为空，会抛出 `HBaseClientException`，避免底层 HBase API 接收到非法参数。

### 新增数据

新增数据使用 `Put` 实现。HBase 中同一个 RowKey、列族、列限定符重复写入时，会生成新版本或覆盖可见值，具体表现与列族版本配置有关。当前示例按字符串写入，后续可扩展 JSON、Long、Double、BigDecimal 等序列化方式。

调用示例：

```java
Map<String, String> data = new LinkedHashMap<>();
data.put("name", "张三");
data.put("phone", "13800000000");
data.put("gender", "male");

hBaseTableRepository.putRow("app:user_profile", "10001", "info", data);
```

写入扩展属性示例：

```java
Map<String, String> ext = new LinkedHashMap<>();
ext.put("tags", "[\"vip\",\"active\"]");
ext.put("source", "app");

hBaseTableRepository.putRow("app:user_profile", "10001", "ext", ext);
```

验证命令：

```bash
# 查询 RowKey 为 10001 的整行数据
get 'app:user_profile', '10001'
```

### 根据 RowKey 查询数据

根据 RowKey 查询使用 `Get` 实现，适合精确查询单行数据。该方式是 HBase 最常见、性能最稳定的查询模式。

调用示例：

```java
Map<String, Object> row = hBaseTableRepository.getRow("app:user_profile", "10001");
```

返回示例：

```json
{
  "rowKey": "10001",
  "info:name": "张三",
  "info:phone": "13800000000",
  "info:gender": "male",
  "ext:tags": "[\"vip\",\"active\"]",
  "ext:source": "app"
}
```

如果 RowKey 不存在，方法返回空 Map：

```json
{}
```

业务层可以根据返回结果是否为空判断数据是否存在。

### 批量查询数据

批量查询使用多个 `Get` 一次性提交到 HBase，适合根据多个 RowKey 批量获取数据。相比循环调用单条查询，批量查询可以减少网络往返次数。

调用示例：

```java
List<String> rowKeys = List.of("10001", "10002", "10003");
List<Map<String, Object>> rows = hBaseTableRepository.getRows("app:user_profile", rowKeys);
```

返回示例：

```json
[
  {
    "rowKey": "10001",
    "info:name": "张三",
    "info:phone": "13800000000"
  },
  {
    "rowKey": "10002",
    "info:name": "李四",
    "info:phone": "13900000000"
  }
]
```

批量查询时需要控制 RowKey 数量。业务接口中不建议一次传入过大的 RowKey 列表，可根据实际情况限制为 100、500 或 1000 条，避免请求过大导致 HBase 和应用内存压力升高。

### 扫描表数据

扫描数据使用 `Scan` 实现，适合按 RowKey 范围查询。需要注意的是，HBase Scan 是范围扫描，不适合在没有明确 RowKey 范围的情况下全表扫描。

调用示例：

```java
List<Map<String, Object>> rows = hBaseTableRepository.scanRows(
        "app:user_profile",
        "10000",
        "20000",
        100
);
```

参数说明：

| 参数        | 说明                    |
| ----------- | ----------------------- |
| `tableName` | 表名                    |
| `startRow`  | 起始 RowKey，包含该值   |
| `stopRow`   | 结束 RowKey，不包含该值 |
| `limit`     | 最大返回条数            |

验证命令：

```bash
# 扫描 RowKey 范围
scan 'app:user_profile', {STARTROW => '10000', STOPROW => '20000', LIMIT => 100}
```

生产环境中应避免无边界扫描：

```java
hBaseTableRepository.scanRows("app:user_profile", null, null, 100000);
```

这类调用可能触发大范围扫描，导致 RegionServer 压力升高。建议业务接口必须传入 RowKey 前缀、起止 RowKey、时间范围或分页游标。

### 删除数据

删除整行数据使用 `Delete` 实现。当前示例按 RowKey 删除整行，如果只需要删除某个列族或某个列限定符，可以在后续扩展 `Delete#addFamily` 或 `Delete#addColumns`。

调用示例：

```java
hBaseTableRepository.deleteRow("app:user_profile", "10001");
```

验证命令：

```bash
# 删除后再次查询，正常情况下应无结果
get 'app:user_profile', '10001'
```

删除操作需要谨慎处理。HBase 删除通常会先写入删除标记，真实数据清理由后续 Compaction 完成。因此，业务上不要假设删除后底层文件立即释放空间。



## 封装通用组件

通用组件用于将 HBase 原生 API 进一步封装为项目内部稳定接口，减少业务层对 `Put`、`Get`、`Scan`、`Result`、`Cell` 等底层对象的直接依赖。建议将通用组件分为工具类、模板类、查询条件对象和返回结果对象四类。

推荐目录结构如下：

```text
src/main/java/io/github/atengk/hbase
├── core
│   └── HBaseTemplate.java
├── model
│   ├── HBaseColumnValue.java
│   ├── HBaseQueryCondition.java
│   ├── HBaseRowResult.java
│   └── HBaseScanResult.java
└── util
    ├── HBaseRowKeyUtil.java
    └── HBaseValueUtil.java
```

其中 `util` 包负责值转换和 RowKey 生成，`model` 包负责查询条件和返回结果定义，`core` 包负责封装通用读写模板。业务服务层后续只需要调用 `HBaseTemplate`，不需要直接操作 HBase 原生对象。

### HBase 工具类封装

HBase 工具类主要处理两类逻辑：第一类是 RowKey 生成，第二类是 HBase 查询结果转换。RowKey 工具类建议只封装通用规则，不要写死具体业务字段；结果转换工具类负责将 HBase `Result` 转换为项目内部统一的 `HBaseRowResult`。

文件位置：`src/main/java/io/github/atengk/hbase/util/HBaseRowKeyUtil.java`

下面的工具类用于生成带散列前缀的 RowKey，以及生成适合倒序时间查询的反转时间戳。

```java
package io.github.atengk.hbase.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.hbase.exception.HBaseClientException;

/**
 * HBase RowKey 工具类。
 *
 * @author Ateng
 * @since 2026-05-07
 */
public final class HBaseRowKeyUtil {

    private HBaseRowKeyUtil() {
    }

    /**
     * 生成散列前缀。
     *
     * @param value      业务值
     * @param bucketSize 分桶数量
     * @return 散列前缀
     */
    public static String saltPrefix(String value, int bucketSize) {
        if (StrUtil.isBlank(value)) {
            throw new HBaseClientException("生成 RowKey 失败，业务值不能为空");
        }
        if (bucketSize <= 0) {
            throw new HBaseClientException("生成 RowKey 失败，分桶数量必须大于 0");
        }

        int bucket = Math.floorMod(value.hashCode(), bucketSize);
        return StrUtil.padPre(String.valueOf(bucket), 2, '0');
    }

    /**
     * 生成带散列前缀的 RowKey。
     *
     * @param businessKey 业务主键
     * @param bucketSize  分桶数量
     * @return RowKey
     */
    public static String withSalt(String businessKey, int bucketSize) {
        return StrUtil.format("{}_{}", saltPrefix(businessKey, bucketSize), businessKey);
    }

    /**
     * 生成反转时间戳。
     *
     * @param timestamp 时间戳
     * @return 反转时间戳
     */
    public static String reverseTimestamp(long timestamp) {
        return String.valueOf(Long.MAX_VALUE - timestamp);
    }

    /**
     * 生成用户时间维度 RowKey。
     *
     * @param userId     用户 ID
     * @param eventId    事件 ID
     * @param bucketSize 分桶数量
     * @return RowKey
     */
    public static String userEventRowKey(String userId, String eventId, int bucketSize) {
        if (StrUtil.hasBlank(userId, eventId)) {
            throw new HBaseClientException("生成用户事件 RowKey 失败，userId 和 eventId 不能为空");
        }

        String salt = saltPrefix(userId, bucketSize);
        String reverseTime = reverseTimestamp(DateUtil.current());

        return StrUtil.format("{}_{}_{}_{}", salt, userId, reverseTime, eventId);
    }

}
```

使用示例：

```java
String rowKey = HBaseRowKeyUtil.userEventRowKey("10001", "evt_202605070001", 16);
// 示例结果：03_10001_9223370272710795807_evt_202605070001
```

文件位置：`src/main/java/io/github/atengk/hbase/util/HBaseValueUtil.java`

下面的工具类用于将 HBase 查询结果转换为统一返回对象，并提供列名拼接和值转换能力。

```java
package io.github.atengk.hbase.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.hbase.model.HBaseColumnValue;
import io.github.atengk.hbase.model.HBaseRowResult;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HBase 值转换工具类。
 *
 * @author Ateng
 * @since 2026-05-07
 */
public final class HBaseValueUtil {

    private HBaseValueUtil() {
    }

    /**
     * 拼接列名。
     *
     * @param family    列族
     * @param qualifier 列限定符
     * @return 完整列名
     */
    public static String columnKey(String family, String qualifier) {
        return StrUtil.format("{}:{}", family, qualifier);
    }

    /**
     * 字符串转字节数组。
     *
     * @param value 字符串值
     * @return 字节数组
     */
    public static byte[] toBytes(String value) {
        return Bytes.toBytes(StrUtil.nullToEmpty(value));
    }

    /**
     * 字节数组转字符串。
     *
     * @param bytes 字节数组
     * @return 字符串值
     */
    public static String toString(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return Bytes.toString(bytes);
    }

    /**
     * 将 HBase Result 转换为行结果。
     *
     * @param result HBase 查询结果
     * @return 行结果
     */
    public static HBaseRowResult toRowResult(Result result) {
        if (result == null || result.isEmpty()) {
            return HBaseRowResult.empty();
        }

        Map<String, String> data = new LinkedHashMap<>();
        Map<String, HBaseColumnValue> columns = new LinkedHashMap<>();

        for (Cell cell : result.rawCells()) {
            String family = toString(CellUtil.cloneFamily(cell));
            String qualifier = toString(CellUtil.cloneQualifier(cell));
            String value = toString(CellUtil.cloneValue(cell));
            long timestamp = cell.getTimestamp();

            String columnKey = columnKey(family, qualifier);

            data.put(columnKey, value);
            columns.put(columnKey, HBaseColumnValue.builder()
                    .family(family)
                    .qualifier(qualifier)
                    .value(value)
                    .timestamp(timestamp)
                    .build());
        }

        return HBaseRowResult.builder()
                .rowKey(toString(result.getRow()))
                .empty(CollUtil.isEmpty(data))
                .data(data)
                .columns(columns)
                .build();
    }

}
```

### HBase 模板类封装

模板类是业务代码访问 HBase 的统一入口。它负责封装表名校验、RowKey 校验、`Put`、`Get`、`Scan`、`Delete` 创建、结果转换、资源关闭和日志记录。

与前文 Repository 相比，`HBaseTemplate` 更通用，不绑定具体业务表，也不关心业务含义。Service 层可以基于该模板组合出用户画像、用户行为、设备指标等具体业务能力。

文件位置：`src/main/java/io/github/atengk/hbase/core/HBaseTemplate.java`

下面的模板类封装 HBase 常用读写操作，后续业务层可直接复用。

```java
package io.github.atengk.hbase.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.hbase.exception.HBaseClientException;
import io.github.atengk.hbase.model.HBaseQueryCondition;
import io.github.atengk.hbase.model.HBaseRowResult;
import io.github.atengk.hbase.model.HBaseScanResult;
import io.github.atengk.hbase.util.HBaseValueUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * HBase 通用操作模板。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HBaseTemplate {

    private static final int DEFAULT_SCAN_LIMIT = 100;
    private static final int MAX_SCAN_LIMIT = 1000;
    private static final int DEFAULT_SCAN_CACHING = 100;

    private final HBaseAccessTemplate hbaseAccessTemplate;

    /**
     * 新增或覆盖一行数据。
     *
     * @param tableName    表名
     * @param rowKey       RowKey
     * @param columnFamily 列族
     * @param data         列限定符和值
     */
    public void put(String tableName, String rowKey, String columnFamily, Map<String, String> data) {
        checkTableName(tableName);
        checkRowKey(rowKey);

        if (StrUtil.isBlank(columnFamily)) {
            throw new HBaseClientException("HBase 列族不能为空");
        }
        if (CollUtil.isEmpty(data)) {
            throw new HBaseClientException("HBase 写入数据不能为空");
        }

        hbaseAccessTemplate.executeTable(tableName, table -> {
            Put put = new Put(HBaseValueUtil.toBytes(rowKey));

            data.forEach((qualifier, value) -> {
                if (StrUtil.isBlank(qualifier)) {
                    throw new HBaseClientException("HBase 列限定符不能为空");
                }
                if (value != null) {
                    put.addColumn(
                            HBaseValueUtil.toBytes(columnFamily),
                            HBaseValueUtil.toBytes(qualifier),
                            HBaseValueUtil.toBytes(value)
                    );
                }
            });

            if (put.isEmpty()) {
                throw new HBaseClientException("HBase 写入数据不能为空，所有字段值均为空");
            }

            table.put(put);
            log.info("HBase 写入数据成功，tableName={}，rowKey={}，columnFamily={}", tableName, rowKey, columnFamily);
            return null;
        });
    }

    /**
     * 根据 RowKey 查询单行数据。
     *
     * @param tableName 表名
     * @param rowKey    RowKey
     * @return 行结果
     */
    public HBaseRowResult get(String tableName, String rowKey) {
        checkTableName(tableName);
        checkRowKey(rowKey);

        return hbaseAccessTemplate.executeTable(tableName, table -> {
            Get get = new Get(HBaseValueUtil.toBytes(rowKey));
            Result result = table.get(get);

            HBaseRowResult rowResult = HBaseValueUtil.toRowResult(result);
            log.info("HBase 单行查询完成，tableName={}，rowKey={}，empty={}",
                    tableName, rowKey, rowResult.getEmpty());
            return rowResult;
        });
    }

    /**
     * 根据 RowKey 和列族查询单行数据。
     *
     * @param tableName    表名
     * @param rowKey       RowKey
     * @param columnFamily 列族
     * @return 行结果
     */
    public HBaseRowResult getFamily(String tableName, String rowKey, String columnFamily) {
        checkTableName(tableName);
        checkRowKey(rowKey);

        if (StrUtil.isBlank(columnFamily)) {
            throw new HBaseClientException("HBase 列族不能为空");
        }

        return hbaseAccessTemplate.executeTable(tableName, table -> {
            Get get = new Get(HBaseValueUtil.toBytes(rowKey));
            get.addFamily(HBaseValueUtil.toBytes(columnFamily));

            Result result = table.get(get);
            HBaseRowResult rowResult = HBaseValueUtil.toRowResult(result);

            log.info("HBase 按列族查询完成，tableName={}，rowKey={}，columnFamily={}，empty={}",
                    tableName, rowKey, columnFamily, rowResult.getEmpty());
            return rowResult;
        });
    }

    /**
     * 批量查询数据。
     *
     * @param tableName 表名
     * @param rowKeys   RowKey 列表
     * @return 行结果列表
     */
    public List<HBaseRowResult> batchGet(String tableName, List<String> rowKeys) {
        checkTableName(tableName);

        if (CollUtil.isEmpty(rowKeys)) {
            throw new HBaseClientException("HBase 批量查询 RowKey 不能为空");
        }

        List<Get> gets = rowKeys.stream()
                .filter(StrUtil::isNotBlank)
                .map(rowKey -> new Get(HBaseValueUtil.toBytes(rowKey)))
                .toList();

        if (CollUtil.isEmpty(gets)) {
            throw new HBaseClientException("HBase 批量查询 RowKey 不能为空");
        }

        return hbaseAccessTemplate.executeTable(tableName, table -> {
            Result[] results = table.get(gets);

            List<HBaseRowResult> rowResults = new ArrayList<>();
            for (Result result : results) {
                HBaseRowResult rowResult = HBaseValueUtil.toRowResult(result);
                if (!rowResult.getEmpty()) {
                    rowResults.add(rowResult);
                }
            }

            log.info("HBase 批量查询完成，tableName={}，requestSize={}，hitSize={}",
                    tableName, rowKeys.size(), rowResults.size());
            return rowResults;
        });
    }

    /**
     * 扫描表数据。
     *
     * @param condition 查询条件
     * @return 扫描结果
     */
    public HBaseScanResult scan(HBaseQueryCondition condition) {
        checkCondition(condition);

        return hbaseAccessTemplate.executeTable(condition.getTableName(), table -> {
            Scan scan = buildScan(condition);
            List<HBaseRowResult> rows = new ArrayList<>();

            try (ResultScanner scanner = table.getScanner(scan)) {
                for (Result result : scanner) {
                    HBaseRowResult rowResult = HBaseValueUtil.toRowResult(result);
                    if (!rowResult.getEmpty()) {
                        rows.add(rowResult);
                    }
                    if (rows.size() >= condition.getLimit()) {
                        break;
                    }
                }
            }

            log.info("HBase 扫描完成，tableName={}，startRow={}，stopRow={}，limit={}，hitSize={}",
                    condition.getTableName(),
                    condition.getStartRow(),
                    condition.getStopRow(),
                    condition.getLimit(),
                    rows.size());

            return HBaseScanResult.builder()
                    .tableName(condition.getTableName())
                    .startRow(condition.getStartRow())
                    .stopRow(condition.getStopRow())
                    .limit(condition.getLimit())
                    .size(rows.size())
                    .rows(rows)
                    .build();
        });
    }

    /**
     * 删除整行数据。
     *
     * @param tableName 表名
     * @param rowKey    RowKey
     */
    public void deleteRow(String tableName, String rowKey) {
        checkTableName(tableName);
        checkRowKey(rowKey);

        hbaseAccessTemplate.executeTable(tableName, table -> {
            Delete delete = new Delete(HBaseValueUtil.toBytes(rowKey));
            table.delete(delete);

            log.info("HBase 删除整行数据成功，tableName={}，rowKey={}", tableName, rowKey);
            return null;
        });
    }

    /**
     * 删除指定列。
     *
     * @param tableName    表名
     * @param rowKey       RowKey
     * @param columnFamily 列族
     * @param qualifier    列限定符
     */
    public void deleteColumn(String tableName, String rowKey, String columnFamily, String qualifier) {
        checkTableName(tableName);
        checkRowKey(rowKey);

        if (StrUtil.hasBlank(columnFamily, qualifier)) {
            throw new HBaseClientException("HBase 删除列失败，列族和列限定符不能为空");
        }

        hbaseAccessTemplate.executeTable(tableName, table -> {
            Delete delete = new Delete(HBaseValueUtil.toBytes(rowKey));
            delete.addColumns(HBaseValueUtil.toBytes(columnFamily), HBaseValueUtil.toBytes(qualifier));
            table.delete(delete);

            log.info("HBase 删除列数据成功，tableName={}，rowKey={}，column={}:{}",
                    tableName, rowKey, columnFamily, qualifier);
            return null;
        });
    }

    /**
     * 构建 Scan 对象。
     *
     * @param condition 查询条件
     * @return Scan 对象
     */
    private Scan buildScan(HBaseQueryCondition condition) {
        Scan scan = new Scan();

        if (StrUtil.isNotBlank(condition.getStartRow())) {
            scan.withStartRow(HBaseValueUtil.toBytes(condition.getStartRow()));
        }
        if (StrUtil.isNotBlank(condition.getStopRow())) {
            scan.withStopRow(HBaseValueUtil.toBytes(condition.getStopRow()));
        }
        if (StrUtil.isNotBlank(condition.getColumnFamily())) {
            scan.addFamily(HBaseValueUtil.toBytes(condition.getColumnFamily()));
        }

        scan.setCaching(condition.getCaching());
        scan.setLimit(condition.getLimit());
        scan.setCacheBlocks(condition.getCacheBlocks());

        return scan;
    }

    /**
     * 校验查询条件。
     *
     * @param condition 查询条件
     */
    private void checkCondition(HBaseQueryCondition condition) {
        if (condition == null) {
            throw new HBaseClientException("HBase 查询条件不能为空");
        }

        checkTableName(condition.getTableName());

        if (condition.getLimit() == null || condition.getLimit() <= 0) {
            condition.setLimit(DEFAULT_SCAN_LIMIT);
        }
        if (condition.getLimit() > MAX_SCAN_LIMIT) {
            throw new HBaseClientException("HBase 扫描 limit 不能超过 " + MAX_SCAN_LIMIT);
        }

        if (condition.getCaching() == null || condition.getCaching() <= 0) {
            condition.setCaching(Math.min(condition.getLimit(), DEFAULT_SCAN_CACHING));
        }

        if (condition.getCacheBlocks() == null) {
            condition.setCacheBlocks(false);
        }
    }

    /**
     * 校验表名。
     *
     * @param tableName 表名
     */
    private void checkTableName(String tableName) {
        if (StrUtil.isBlank(tableName)) {
            throw new HBaseClientException("HBase 表名不能为空");
        }
    }

    /**
     * 校验 RowKey。
     *
     * @param rowKey RowKey
     */
    private void checkRowKey(String rowKey) {
        if (StrUtil.isBlank(rowKey)) {
            throw new HBaseClientException("HBase RowKey 不能为空");
        }
    }

}
```

调用示例：

```java
Map<String, String> data = new LinkedHashMap<>();
data.put("name", "张三");
data.put("phone", "13800000000");

hBaseTemplate.put("app:user_profile", "10001", "info", data);

HBaseRowResult rowResult = hBaseTemplate.get("app:user_profile", "10001");

HBaseQueryCondition condition = HBaseQueryCondition.builder()
        .tableName("app:user_profile")
        .startRow("10000")
        .stopRow("20000")
        .limit(100)
        .build();

HBaseScanResult scanResult = hBaseTemplate.scan(condition);
```

### 查询条件对象设计

查询条件对象用于封装 HBase 扫描参数，避免方法参数过多。它主要适用于 `Scan` 查询，包括表名、起始 RowKey、结束 RowKey、列族、返回条数、缓存条数和是否缓存数据块等配置。

文件位置：`src/main/java/io/github/atengk/hbase/model/HBaseQueryCondition.java`

下面的对象用于承载 HBase 范围扫描条件。

```java
package io.github.atengk.hbase.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * HBase 查询条件。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HBaseQueryCondition {

    /**
     * 表名，格式为 namespace:table。
     */
    private String tableName;

    /**
     * 起始 RowKey，包含该值。
     */
    private String startRow;

    /**
     * 结束 RowKey，不包含该值。
     */
    private String stopRow;

    /**
     * 列族，不传则查询所有列族。
     */
    private String columnFamily;

    /**
     * 最大返回条数。
     */
    private Integer limit;

    /**
     * 客户端每批拉取数量。
     */
    private Integer caching;

    /**
     * 是否缓存数据块。
     * 
     * 在线查询建议为 false，避免大范围扫描污染 BlockCache。
     */
    private Boolean cacheBlocks;

}
```

字段说明：

| 字段           | 类型      | 说明                               |
| -------------- | --------- | ---------------------------------- |
| `tableName`    | `String`  | HBase 表名，必填                   |
| `startRow`     | `String`  | 起始 RowKey，包含该值              |
| `stopRow`      | `String`  | 结束 RowKey，不包含该值            |
| `columnFamily` | `String`  | 指定列族，不传则扫描所有列族       |
| `limit`        | `Integer` | 最大返回条数，建议业务接口限制上限 |
| `caching`      | `Integer` | 每批从 RegionServer 拉取的记录数   |
| `cacheBlocks`  | `Boolean` | 是否缓存扫描到的数据块             |

使用示例：

```java
HBaseQueryCondition condition = HBaseQueryCondition.builder()
        .tableName("log:user_event")
        .startRow("03_10001_")
        .stopRow("03_10001_~")
        .columnFamily("info")
        .limit(100)
        .caching(100)
        .cacheBlocks(false)
        .build();

HBaseScanResult result = hBaseTemplate.scan(condition);
```

这里的 `~` 通常用于构造字符串范围上界，因为在 ASCII 排序中它比常见数字、字母和下划线更靠后。实际业务中应根据 RowKey 编码规则确认范围边界。

### 返回结果对象设计

返回结果对象用于统一封装 HBase 查询结果。建议不要直接把 HBase `Result`、`Cell` 返回给 Controller 或业务调用方，否则上层代码会强依赖 HBase 客户端 API，后续维护成本较高。

#### 单列结果对象

文件位置：`src/main/java/io/github/atengk/hbase/model/HBaseColumnValue.java`

下面的对象用于表示 HBase 中的一个单元格数据。

```java
package io.github.atengk.hbase.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * HBase 单列数据。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HBaseColumnValue {

    /**
     * 列族。
     */
    private String family;

    /**
     * 列限定符。
     */
    private String qualifier;

    /**
     * 单元格值。
     */
    private String value;

    /**
     * 数据版本时间戳。
     */
    private Long timestamp;

}
```

#### 单行结果对象

文件位置：`src/main/java/io/github/atengk/hbase/model/HBaseRowResult.java`

下面的对象用于表示 HBase 中的一行数据，同时保留扁平 Map 和结构化列信息。

```java
package io.github.atengk.hbase.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Map;

/**
 * HBase 单行查询结果。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HBaseRowResult {

    /**
     * RowKey。
     */
    private String rowKey;

    /**
     * 是否为空结果。
     */
    private Boolean empty;

    /**
     * 扁平化数据，格式为 columnFamily:qualifier -> value。
     */
    private Map<String, String> data;

    /**
     * 结构化列数据，格式为 columnFamily:qualifier -> columnValue。
     */
    private Map<String, HBaseColumnValue> columns;

    /**
     * 创建空行结果。
     *
     * @return 空行结果
     */
    public static HBaseRowResult empty() {
        return HBaseRowResult.builder()
                .empty(true)
                .data(Collections.emptyMap())
                .columns(Collections.emptyMap())
                .build();
    }

}
```

单行查询返回示例：

```json
{
  "rowKey": "10001",
  "empty": false,
  "data": {
    "info:name": "张三",
    "info:phone": "13800000000",
    "ext:tags": "[\"vip\",\"active\"]"
  },
  "columns": {
    "info:name": {
      "family": "info",
      "qualifier": "name",
      "value": "张三",
      "timestamp": 1788746400000
    }
  }
}
```

#### 扫描结果对象

文件位置：`src/main/java/io/github/atengk/hbase/model/HBaseScanResult.java`

下面的对象用于表示 HBase 范围扫描结果，包含扫描条件回显和命中数据。

```java
package io.github.atengk.hbase.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * HBase 扫描查询结果。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HBaseScanResult {

    /**
     * 表名。
     */
    private String tableName;

    /**
     * 起始 RowKey。
     */
    private String startRow;

    /**
     * 结束 RowKey。
     */
    private String stopRow;

    /**
     * 请求返回条数限制。
     */
    private Integer limit;

    /**
     * 实际返回条数。
     */
    private Integer size;

    /**
     * 行数据列表。
     */
    private List<HBaseRowResult> rows;

}
```

扫描查询返回示例：

```json
{
  "tableName": "app:user_profile",
  "startRow": "10000",
  "stopRow": "20000",
  "limit": 100,
  "size": 2,
  "rows": [
    {
      "rowKey": "10001",
      "empty": false,
      "data": {
        "info:name": "张三",
        "info:phone": "13800000000"
      }
    },
    {
      "rowKey": "10002",
      "empty": false,
      "data": {
        "info:name": "李四",
        "info:phone": "13900000000"
      }
    }
  ]
}
```

通用组件封装完成后，后续业务接口开发可以直接基于 `HBaseTemplate` 组织接口逻辑。例如用户画像查询接口只需要接收 `userId`，拼接 RowKey 后调用 `hBaseTemplate.get("app:user_profile", userId)`；用户行为查询接口可以构造 `HBaseQueryCondition`，按用户散列前缀和时间范围扫描数据。

以下继续补充“业务接口开发”和“异常处理与日志”部分，可直接接在前文“封装通用组件”后。该部分对应你上传大纲中的接口开发、异常封装、全局异常处理、日志记录和连接异常排查章节。

## 业务接口开发

业务接口层用于将前文封装好的 `HBaseTemplate` 暴露为 REST API，供前端、测试工具或其他后端服务调用。Controller 层只负责接收请求、参数校验和返回统一响应，不直接操作 HBase 原生 API。

本节接口基于以下组件继续开发：

| 组件                   | 作用                 |
| ---------------------- | -------------------- |
| `HBaseTemplate`        | HBase 通用读写模板   |
| `HBaseQueryCondition`  | Scan 查询条件对象    |
| `HBaseRowResult`       | 单行查询结果对象     |
| `HBaseScanResult`      | 扫描查询结果对象     |
| `HBaseClientException` | HBase 客户端业务异常 |

推荐接口目录结构如下：

```text
src/main/java/io/github/atengk/hbase
├── controller
│   └── HBaseDataController.java
├── dto
│   ├── HBaseBatchGetRequest.java
│   ├── HBaseDeleteColumnRequest.java
│   ├── HBaseDeleteRowRequest.java
│   └── HBasePutRequest.java
├── service
│   ├── HBaseDataService.java
│   └── impl
│       └── HBaseDataServiceImpl.java
└── web
    └── ApiResult.java
```

### 数据写入接口

数据写入接口用于向指定 HBase 表写入一行数据。请求中需要包含表名、RowKey、列族和列限定符数据。当前示例按 `Map<String, String>` 写入，适合字符串、JSON 字符串、枚举值、时间字符串等常见业务数据。

文件位置：`src/main/java/io/github/atengk/hbase/dto/HBasePutRequest.java`

下面的 DTO 用于接收 HBase 写入请求参数。

```java
package io.github.atengk.hbase.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Map;

/**
 * HBase 数据写入请求。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class HBasePutRequest {

    /**
     * 表名，格式为 namespace:table。
     */
    @NotBlank(message = "表名不能为空")
    private String tableName;

    /**
     * RowKey。
     */
    @NotBlank(message = "RowKey 不能为空")
    private String rowKey;

    /**
     * 列族。
     */
    @NotBlank(message = "列族不能为空")
    private String columnFamily;

    /**
     * 列限定符和值。
     */
    @NotEmpty(message = "写入数据不能为空")
    private Map<String, String> data;

}
```

### 单条查询接口

单条查询接口用于根据表名和 RowKey 查询一行数据。该接口是 HBase 最常用的查询模式，性能稳定，适合详情页、状态查询、画像查询和主键精确查询。

接口参数通过 URL Query 传入：

```text
GET /api/hbase/rows?tableName=app:user_profile&rowKey=10001
```

返回数据使用前文定义的 `HBaseRowResult`，包含 `rowKey`、`empty`、`data` 和 `columns`。

### 条件扫描接口

条件扫描接口用于按 RowKey 范围扫描表数据。该接口适合按用户、设备、订单、时间维度进行范围查询，但必须限制 `limit`，避免接口触发全表扫描。

请求体直接复用前文定义的 `HBaseQueryCondition`：

```json
{
  "tableName": "app:user_profile",
  "startRow": "10000",
  "stopRow": "20000",
  "columnFamily": "info",
  "limit": 100,
  "caching": 100,
  "cacheBlocks": false
}
```

### 批量操作接口

批量查询接口用于根据多个 RowKey 一次性查询多行数据。相比循环调用单条查询接口，批量查询能减少网络往返次数，但仍需要控制单次请求的 RowKey 数量。

文件位置：`src/main/java/io/github/atengk/hbase/dto/HBaseBatchGetRequest.java`

下面的 DTO 用于接收 HBase 批量查询请求参数。

```java
package io.github.atengk.hbase.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * HBase 批量查询请求。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class HBaseBatchGetRequest {

    /**
     * 表名，格式为 namespace:table。
     */
    @NotBlank(message = "表名不能为空")
    private String tableName;

    /**
     * RowKey 列表。
     */
    @NotEmpty(message = "RowKey 列表不能为空")
    private List<String> rowKeys;

}
```

### 删除数据接口

删除接口分为删除整行和删除指定列两类。删除整行适合清理某个 RowKey 下的全部数据；删除指定列适合只清理某个列族下的某个字段。

文件位置：`src/main/java/io/github/atengk/hbase/dto/HBaseDeleteRowRequest.java`

下面的 DTO 用于接收 HBase 整行删除请求参数。

```java
package io.github.atengk.hbase.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * HBase 整行删除请求。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class HBaseDeleteRowRequest {

    /**
     * 表名，格式为 namespace:table。
     */
    @NotBlank(message = "表名不能为空")
    private String tableName;

    /**
     * RowKey。
     */
    @NotBlank(message = "RowKey 不能为空")
    private String rowKey;

}
```

文件位置：`src/main/java/io/github/atengk/hbase/dto/HBaseDeleteColumnRequest.java`

下面的 DTO 用于接收 HBase 指定列删除请求参数。

```java
package io.github.atengk.hbase.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * HBase 指定列删除请求。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class HBaseDeleteColumnRequest {

    /**
     * 表名，格式为 namespace:table。
     */
    @NotBlank(message = "表名不能为空")
    private String tableName;

    /**
     * RowKey。
     */
    @NotBlank(message = "RowKey 不能为空")
    private String rowKey;

    /**
     * 列族。
     */
    @NotBlank(message = "列族不能为空")
    private String columnFamily;

    /**
     * 列限定符。
     */
    @NotBlank(message = "列限定符不能为空")
    private String qualifier;

}
```

文件位置：`src/main/java/io/github/atengk/hbase/web/ApiResult.java`

下面的对象用于统一 REST 接口返回格式。

```java
package io.github.atengk.hbase.web;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口统一返回结果。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /**
     * 响应状态码。
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
     * 成功返回。
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 统一返回结果
     */
    public static <T> ApiResult<T> success(T data) {
        return ApiResult.<T>builder()
                .code(200)
                .message("操作成功")
                .data(data)
                .build();
    }

    /**
     * 成功返回。
     *
     * @return 统一返回结果
     */
    public static ApiResult<Void> success() {
        return ApiResult.<Void>builder()
                .code(200)
                .message("操作成功")
                .build();
    }

    /**
     * 失败返回。
     *
     * @param code    响应状态码
     * @param message 响应消息
     * @return 统一返回结果
     */
    public static ApiResult<Void> fail(Integer code, String message) {
        return ApiResult.<Void>builder()
                .code(code)
                .message(message)
                .build();
    }

}
```

文件位置：`src/main/java/io/github/atengk/hbase/service/HBaseDataService.java`

下面的接口定义 HBase 数据读写业务能力。

```java
package io.github.atengk.hbase.service;

import io.github.atengk.hbase.dto.HBaseBatchGetRequest;
import io.github.atengk.hbase.dto.HBaseDeleteColumnRequest;
import io.github.atengk.hbase.dto.HBaseDeleteRowRequest;
import io.github.atengk.hbase.dto.HBasePutRequest;
import io.github.atengk.hbase.model.HBaseQueryCondition;
import io.github.atengk.hbase.model.HBaseRowResult;
import io.github.atengk.hbase.model.HBaseScanResult;

import java.util.List;

/**
 * HBase 数据业务服务。
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface HBaseDataService {

    /**
     * 写入数据。
     *
     * @param request 写入请求
     */
    void put(HBasePutRequest request);

    /**
     * 根据 RowKey 查询数据。
     *
     * @param tableName 表名
     * @param rowKey    RowKey
     * @return 单行结果
     */
    HBaseRowResult get(String tableName, String rowKey);

    /**
     * 扫描数据。
     *
     * @param condition 查询条件
     * @return 扫描结果
     */
    HBaseScanResult scan(HBaseQueryCondition condition);

    /**
     * 批量查询数据。
     *
     * @param request 批量查询请求
     * @return 行结果列表
     */
    List<HBaseRowResult> batchGet(HBaseBatchGetRequest request);

    /**
     * 删除整行数据。
     *
     * @param request 删除请求
     */
    void deleteRow(HBaseDeleteRowRequest request);

    /**
     * 删除指定列数据。
     *
     * @param request 删除列请求
     */
    void deleteColumn(HBaseDeleteColumnRequest request);

}
```

文件位置：`src/main/java/io/github/atengk/hbase/service/impl/HBaseDataServiceImpl.java`

下面的实现类将业务请求委托给 `HBaseTemplate`，并在业务入口记录关键操作日志。

```java
package io.github.atengk.hbase.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.hbase.core.HBaseTemplate;
import io.github.atengk.hbase.dto.HBaseBatchGetRequest;
import io.github.atengk.hbase.dto.HBaseDeleteColumnRequest;
import io.github.atengk.hbase.dto.HBaseDeleteRowRequest;
import io.github.atengk.hbase.dto.HBasePutRequest;
import io.github.atengk.hbase.exception.HBaseClientException;
import io.github.atengk.hbase.model.HBaseQueryCondition;
import io.github.atengk.hbase.model.HBaseRowResult;
import io.github.atengk.hbase.model.HBaseScanResult;
import io.github.atengk.hbase.service.HBaseDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * HBase 数据业务服务实现。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HBaseDataServiceImpl implements HBaseDataService {

    private static final int MAX_BATCH_GET_SIZE = 500;

    private final HBaseTemplate hBaseTemplate;

    /**
     * 写入数据。
     *
     * @param request 写入请求
     */
    @Override
    public void put(HBasePutRequest request) {
        log.info("开始写入 HBase 数据，tableName={}，rowKey={}，columnFamily={}",
                request.getTableName(), request.getRowKey(), request.getColumnFamily());

        hBaseTemplate.put(
                request.getTableName(),
                request.getRowKey(),
                request.getColumnFamily(),
                request.getData()
        );
    }

    /**
     * 根据 RowKey 查询数据。
     *
     * @param tableName 表名
     * @param rowKey    RowKey
     * @return 单行结果
     */
    @Override
    public HBaseRowResult get(String tableName, String rowKey) {
        if (StrUtil.hasBlank(tableName, rowKey)) {
            throw new HBaseClientException("表名和 RowKey 不能为空");
        }

        log.info("开始查询 HBase 单行数据，tableName={}，rowKey={}", tableName, rowKey);
        return hBaseTemplate.get(tableName, rowKey);
    }

    /**
     * 扫描数据。
     *
     * @param condition 查询条件
     * @return 扫描结果
     */
    @Override
    public HBaseScanResult scan(HBaseQueryCondition condition) {
        if (condition == null) {
            throw new HBaseClientException("HBase 查询条件不能为空");
        }

        log.info("开始扫描 HBase 数据，tableName={}，startRow={}，stopRow={}，limit={}",
                condition.getTableName(), condition.getStartRow(), condition.getStopRow(), condition.getLimit());

        return hBaseTemplate.scan(condition);
    }

    /**
     * 批量查询数据。
     *
     * @param request 批量查询请求
     * @return 行结果列表
     */
    @Override
    public List<HBaseRowResult> batchGet(HBaseBatchGetRequest request) {
        if (CollUtil.isEmpty(request.getRowKeys())) {
            throw new HBaseClientException("批量查询 RowKey 不能为空");
        }
        if (request.getRowKeys().size() > MAX_BATCH_GET_SIZE) {
            throw new HBaseClientException("批量查询 RowKey 数量不能超过 " + MAX_BATCH_GET_SIZE);
        }

        log.info("开始批量查询 HBase 数据，tableName={}，requestSize={}",
                request.getTableName(), request.getRowKeys().size());

        return hBaseTemplate.batchGet(request.getTableName(), request.getRowKeys());
    }

    /**
     * 删除整行数据。
     *
     * @param request 删除请求
     */
    @Override
    public void deleteRow(HBaseDeleteRowRequest request) {
        log.info("开始删除 HBase 整行数据，tableName={}，rowKey={}",
                request.getTableName(), request.getRowKey());

        hBaseTemplate.deleteRow(request.getTableName(), request.getRowKey());
    }

    /**
     * 删除指定列数据。
     *
     * @param request 删除列请求
     */
    @Override
    public void deleteColumn(HBaseDeleteColumnRequest request) {
        log.info("开始删除 HBase 指定列数据，tableName={}，rowKey={}，column={}:{}",
                request.getTableName(), request.getRowKey(), request.getColumnFamily(), request.getQualifier());

        hBaseTemplate.deleteColumn(
                request.getTableName(),
                request.getRowKey(),
                request.getColumnFamily(),
                request.getQualifier()
        );
    }

}
```

文件位置：`src/main/java/io/github/atengk/hbase/controller/HBaseDataController.java`

下面的 Controller 提供 HBase 数据写入、单条查询、条件扫描、批量查询和删除接口。

```java
package io.github.atengk.hbase.controller;

import io.github.atengk.hbase.dto.HBaseBatchGetRequest;
import io.github.atengk.hbase.dto.HBaseDeleteColumnRequest;
import io.github.atengk.hbase.dto.HBaseDeleteRowRequest;
import io.github.atengk.hbase.dto.HBasePutRequest;
import io.github.atengk.hbase.model.HBaseQueryCondition;
import io.github.atengk.hbase.model.HBaseRowResult;
import io.github.atengk.hbase.model.HBaseScanResult;
import io.github.atengk.hbase.service.HBaseDataService;
import io.github.atengk.hbase.web.ApiResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * HBase 数据接口。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hbase")
public class HBaseDataController {

    private final HBaseDataService hBaseDataService;

    /**
     * 写入 HBase 数据。
     *
     * @param request 写入请求
     * @return 统一返回结果
     */
    @PostMapping("/rows")
    public ApiResult<Void> put(@Valid @RequestBody HBasePutRequest request) {
        hBaseDataService.put(request);
        return ApiResult.success();
    }

    /**
     * 根据 RowKey 查询 HBase 数据。
     *
     * @param tableName 表名
     * @param rowKey    RowKey
     * @return 单行结果
     */
    @GetMapping("/rows")
    public ApiResult<HBaseRowResult> get(@NotBlank(message = "表名不能为空") String tableName,
                                         @NotBlank(message = "RowKey 不能为空") String rowKey) {
        return ApiResult.success(hBaseDataService.get(tableName, rowKey));
    }

    /**
     * 扫描 HBase 数据。
     *
     * @param condition 查询条件
     * @return 扫描结果
     */
    @PostMapping("/scan")
    public ApiResult<HBaseScanResult> scan(@Valid @RequestBody HBaseQueryCondition condition) {
        return ApiResult.success(hBaseDataService.scan(condition));
    }

    /**
     * 批量查询 HBase 数据。
     *
     * @param request 批量查询请求
     * @return 行结果列表
     */
    @PostMapping("/batch-get")
    public ApiResult<List<HBaseRowResult>> batchGet(@Valid @RequestBody HBaseBatchGetRequest request) {
        return ApiResult.success(hBaseDataService.batchGet(request));
    }

    /**
     * 删除 HBase 整行数据。
     *
     * @param request 删除请求
     * @return 统一返回结果
     */
    @DeleteMapping("/rows")
    public ApiResult<Void> deleteRow(@Valid @RequestBody HBaseDeleteRowRequest request) {
        hBaseDataService.deleteRow(request);
        return ApiResult.success();
    }

    /**
     * 删除 HBase 指定列数据。
     *
     * @param request 删除列请求
     * @return 统一返回结果
     */
    @DeleteMapping("/columns")
    public ApiResult<Void> deleteColumn(@Valid @RequestBody HBaseDeleteColumnRequest request) {
        hBaseDataService.deleteColumn(request);
        return ApiResult.success();
    }

}
```

接口调用示例：

```bash
# 写入数据
curl -X POST "http://localhost:8080/api/hbase/rows" \
  -H "Content-Type: application/json" \
  -d '{
    "tableName": "app:user_profile",
    "rowKey": "10001",
    "columnFamily": "info",
    "data": {
      "name": "张三",
      "phone": "13800000000",
      "gender": "male"
    }
  }'

# 根据 RowKey 查询
curl -X GET "http://localhost:8080/api/hbase/rows?tableName=app:user_profile&rowKey=10001"

# 条件扫描
curl -X POST "http://localhost:8080/api/hbase/scan" \
  -H "Content-Type: application/json" \
  -d '{
    "tableName": "app:user_profile",
    "startRow": "10000",
    "stopRow": "20000",
    "columnFamily": "info",
    "limit": 100,
    "caching": 100,
    "cacheBlocks": false
  }'

# 批量查询
curl -X POST "http://localhost:8080/api/hbase/batch-get" \
  -H "Content-Type: application/json" \
  -d '{
    "tableName": "app:user_profile",
    "rowKeys": ["10001", "10002", "10003"]
  }'

# 删除整行
curl -X DELETE "http://localhost:8080/api/hbase/rows" \
  -H "Content-Type: application/json" \
  -d '{
    "tableName": "app:user_profile",
    "rowKey": "10001"
  }'

# 删除指定列
curl -X DELETE "http://localhost:8080/api/hbase/columns" \
  -H "Content-Type: application/json" \
  -d '{
    "tableName": "app:user_profile",
    "rowKey": "10001",
    "columnFamily": "info",
    "qualifier": "phone"
  }'
```

以上接口适合作为开发测试、内部管理或服务间调用接口。生产环境对外开放时，应增加鉴权、限流、表名白名单和操作审计，避免任意表被外部请求直接读写。

## 异常处理与日志

HBase 操作涉及网络、ZooKeeper、RegionServer、表状态、列族、RowKey、权限和序列化等多类问题。项目中应将底层异常统一转换为业务异常，并通过全局异常处理器返回稳定的接口结构。

### HBase 异常封装

前文已经定义了 `HBaseClientException`，这里进一步补充异常码枚举，便于接口返回和日志定位。

文件位置：`src/main/java/io/github/atengk/hbase/exception/HBaseErrorCode.java`

下面的枚举用于定义 HBase 业务异常码。

```java
package io.github.atengk.hbase.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * HBase 异常码。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Getter
@AllArgsConstructor
public enum HBaseErrorCode {

    /**
     * 参数错误。
     */
    PARAM_ERROR(400, "参数错误"),

    /**
     * HBase 操作失败。
     */
    OPERATION_FAILED(500, "HBase 操作失败"),

    /**
     * HBase 连接失败。
     */
    CONNECTION_FAILED(501, "HBase 连接失败"),

    /**
     * HBase 表不存在。
     */
    TABLE_NOT_FOUND(404, "HBase 表不存在"),

    /**
     * HBase 扫描范围非法。
     */
    SCAN_RANGE_INVALID(422, "HBase 扫描范围非法");

    /**
     * 异常码。
     */
    private final Integer code;

    /**
     * 异常消息。
     */
    private final String message;

}
```

文件位置：`src/main/java/io/github/atengk/hbase/exception/HBaseClientException.java`

下面的异常类支持携带错误码，便于全局异常处理器输出更明确的响应。

```java
package io.github.atengk.hbase.exception;

import lombok.Getter;

/**
 * HBase 客户端异常。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Getter
public class HBaseClientException extends RuntimeException {

    /**
     * 异常码。
     */
    private final Integer code;

    /**
     * 创建 HBase 客户端异常。
     *
     * @param message 异常消息
     */
    public HBaseClientException(String message) {
        super(message);
        this.code = HBaseErrorCode.OPERATION_FAILED.getCode();
    }

    /**
     * 创建 HBase 客户端异常。
     *
     * @param errorCode 异常码
     */
    public HBaseClientException(HBaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 创建 HBase 客户端异常。
     *
     * @param message 异常消息
     * @param cause   原始异常
     */
    public HBaseClientException(String message, Throwable cause) {
        super(message, cause);
        this.code = HBaseErrorCode.OPERATION_FAILED.getCode();
    }

    /**
     * 创建 HBase 客户端异常。
     *
     * @param errorCode 异常码
     * @param cause     原始异常
     */
    public HBaseClientException(HBaseErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
    }

}
```

异常使用示例：

```java
if (StrUtil.isBlank(tableName)) {
    throw new HBaseClientException(HBaseErrorCode.PARAM_ERROR);
}

try {
    return hbaseConnection.getTable(TableName.valueOf(tableName));
} catch (IOException e) {
    log.error("获取 HBase Table 对象失败，tableName={}", tableName, e);
    throw new HBaseClientException(HBaseErrorCode.CONNECTION_FAILED, e);
}
```

### 全局异常处理

全局异常处理器用于拦截业务异常、参数校验异常和未预期异常，保证接口返回结构统一。这样前端或调用方无需解析 Spring Boot 默认错误响应。

文件位置：`src/main/java/io/github/atengk/hbase/exception/GlobalExceptionHandler.java`

下面的代码统一处理 HBase 异常、参数校验异常和系统异常。

```java
package io.github.atengk.hbase.exception;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.hbase.web.ApiResult;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 HBase 客户端异常。
     *
     * @param e HBase 客户端异常
     * @return 统一返回结果
     */
    @ExceptionHandler(HBaseClientException.class)
    public ApiResult<Void> handleHBaseClientException(HBaseClientException e) {
        log.error("HBase 业务处理异常，code={}，message={}", e.getCode(), e.getMessage(), e);
        return ApiResult.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理请求体参数校验异常。
     *
     * @param e 参数校验异常
     * @return 统一返回结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElse("请求参数不合法");

        log.warn("请求体参数校验失败，message={}", message);
        return ApiResult.fail(HBaseErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理表单参数绑定异常。
     *
     * @param e 参数绑定异常
     * @return 统一返回结果
     */
    @ExceptionHandler(BindException.class)
    public ApiResult<Void> handleBindException(BindException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElse("请求参数绑定失败");

        log.warn("请求参数绑定失败，message={}", message);
        return ApiResult.fail(HBaseErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理 URL 参数校验异常。
     *
     * @param e 参数校验异常
     * @return 统一返回结果
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResult<Void> handleConstraintViolationException(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();

        String message = CollUtil.emptyIfNull(violations)
                .stream()
                .map(ConstraintViolation::getMessage)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining("；"));

        log.warn("URL 参数校验失败，message={}", message);
        return ApiResult.fail(HBaseErrorCode.PARAM_ERROR.getCode(), StrUtil.blankToDefault(message, "请求参数不合法"));
    }

    /**
     * 处理请求体解析异常。
     *
     * @param e 请求体解析异常
     * @return 统一返回结果
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResult<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败，message={}", e.getMessage());
        return ApiResult.fail(HBaseErrorCode.PARAM_ERROR.getCode(), "请求体格式错误");
    }

    /**
     * 处理系统未知异常。
     *
     * @param e 系统异常
     * @return 统一返回结果
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception e) {
        log.error("系统未知异常", e);
        return ApiResult.fail(500, "系统繁忙，请稍后重试");
    }

}
```

全局异常返回示例：

```json
{
  "code": 400,
  "message": "RowKey 不能为空",
  "data": null
}
```

HBase 操作失败返回示例：

```json
{
  "code": 500,
  "message": "HBase 操作失败",
  "data": null
}
```

### 操作日志记录

HBase 接口建议记录关键业务日志，但不要记录过大的数据内容。写入、删除、扫描等操作应记录表名、RowKey、列族、扫描范围、请求数量、命中数量和耗时，避免把完整字段值、手机号、身份证号、Token 等敏感数据写入日志。

推荐日志记录位置如下：

| 层级             | 记录内容                                |
| ---------------- | --------------------------------------- |
| Controller       | 一般不记录，避免重复日志                |
| Service          | 记录业务入口、表名、RowKey、请求数量    |
| Template         | 记录 HBase 操作结果、命中数量、扫描范围 |
| ExceptionHandler | 记录异常码、异常消息、堆栈              |
| Config           | 记录连接初始化参数，但不要记录认证密钥  |

如果希望统一记录 HBase 操作耗时，可以封装一个简单的日志工具方法。

文件位置：`src/main/java/io/github/atengk/hbase/util/HBaseLogUtil.java`

下面的工具类用于计算操作耗时，并输出统一格式日志。

```java
package io.github.atengk.hbase.util;

import cn.hutool.core.date.SystemClock;
import lombok.extern.slf4j.Slf4j;

/**
 * HBase 日志工具类。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
public final class HBaseLogUtil {

    private HBaseLogUtil() {
    }

    /**
     * 获取当前时间戳。
     *
     * @return 当前时间戳
     */
    public static long start() {
        return SystemClock.now();
    }

    /**
     * 记录操作耗时。
     *
     * @param operation 操作名称
     * @param tableName 表名
     * @param startTime 开始时间
     */
    public static void cost(String operation, String tableName, long startTime) {
        long cost = SystemClock.now() - startTime;
        log.info("HBase 操作完成，operation={}，tableName={}，cost={}ms", operation, tableName, cost);
    }

}
```

使用示例：

```java
long startTime = HBaseLogUtil.start();

try {
    HBaseRowResult result = hBaseTemplate.get("app:user_profile", "10001");
    HBaseLogUtil.cost("get", "app:user_profile", startTime);
    return result;
} catch (Exception e) {
    log.error("HBase 查询失败，tableName={}，rowKey={}", "app:user_profile", "10001", e);
    throw e;
}
```

如果项目已经接入链路追踪系统，例如 SkyWalking、Zipkin、OpenTelemetry 或日志平台，建议在日志中增加 `traceId`、`spanId`、请求来源和业务流水号，便于跨服务定位。

### 连接异常排查

HBase 连接异常通常发生在应用启动阶段或首次请求阶段。排查时应从配置、网络、ZooKeeper、HBase 服务状态、主机名解析和权限认证几个方向检查。

常见异常及处理方式如下：

| 异常现象            | 可能原因                              | 排查方式                                    |
| ------------------- | ------------------------------------- | ------------------------------------------- |
| 连接 ZooKeeper 超时 | ZooKeeper 地址错误或网络不通          | 检查 `hbase.zookeeper.quorum`、端口、防火墙 |
| 找不到 RegionServer | HBase 服务异常或 DNS 解析失败         | 检查 RegionServer 状态和 hosts/DNS          |
| 表不存在            | 表名或命名空间错误                    | 使用 HBase Shell 执行 `list`、`exists`      |
| 列族不存在          | 写入的列族未建表定义                  | 使用 `describe 'tableName'` 查看列族        |
| 认证失败            | Kerberos 配置缺失或票据过期           | 检查 keytab、principal、krb5.conf           |
| 请求卡顿            | RPC 超时、Region 热点或 Scan 范围过大 | 检查 RegionServer 日志和接口扫描范围        |

本地网络连通性检查：

```bash
# 检查 ZooKeeper 端口是否可访问
nc -vz hbase-zk01 2181
nc -vz hbase-zk02 2181
nc -vz hbase-zk03 2181

# 检查主机名解析
nslookup hbase-zk01
ping hbase-zk01
```

HBase Shell 检查：

```bash
# 进入 HBase Shell
hbase shell

# 查看集群状态
status

# 查看表列表
list

# 判断表是否存在
exists 'app:user_profile'

# 查看表结构
describe 'app:user_profile'

# 查询一行数据
get 'app:user_profile', '10001'
```

应用配置检查：

```bash
# 查看当前激活环境
java -jar springboot-hbase-demo.jar --spring.profiles.active=dev

# 临时覆盖 ZooKeeper 地址启动
java -jar springboot-hbase-demo.jar \
  --spring.profiles.active=dev \
  --hbase.zookeeper-quorum=hbase-zk01,hbase-zk02,hbase-zk03 \
  --hbase.zookeeper-client-port=2181 \
  --hbase.znode-parent=/hbase
```

命令说明：`nc -vz` 用于检查目标主机端口是否可连接；`nslookup` 用于检查 DNS 解析；`hbase shell` 用于从 HBase 客户端侧验证集群状态；启动参数中的 `--hbase.*` 可以临时覆盖 `application.yml` 中的配置，便于排查环境配置问题。

如果应用启动时报 `NoClassDefFoundError`、`ClassNotFoundException`、`NoSuchMethodError` 等依赖类异常，优先检查 HBase Client、Hadoop、ZooKeeper、Protobuf、Guava 和 Netty 版本冲突。此时建议使用前文 Maven 配置中的 `hbase-shaded-client`，并通过以下命令查看依赖树：

```bash
# 查看 HBase 相关依赖树
mvn dependency:tree | grep -E "hbase|hadoop|zookeeper|guava|protobuf|netty"
```

如果接口扫描速度较慢，应重点检查 RowKey 设计和扫描范围，而不是简单调大超时时间。大范围 Scan 会给 RegionServer 带来明显压力，生产环境接口必须限制 `limit`，并尽量通过 RowKey 前缀、起止 RowKey 或分页游标缩小扫描范围。



## 测试验证

测试验证用于确认 HBase 配置、连接、表结构、数据写入、数据查询和接口封装是否符合预期。建议按照“工具类单元测试、接口测试、HBase Shell 验证、日志排查”的顺序执行，避免一开始就直接联调复杂业务接口。

### 单元测试

单元测试优先覆盖不依赖真实 HBase 集群的工具类和参数转换逻辑，例如 RowKey 生成、反转时间戳、返回对象转换、请求参数限制等。对于依赖真实 HBase 集群的读写操作，建议归类为集成测试，不要与普通单元测试混在一起。

文件位置：`src/test/java/io/github/atengk/hbase/util/HBaseRowKeyUtilTest.java`

下面的测试类用于验证 RowKey 工具类的散列前缀、反转时间戳和用户事件 RowKey 生成逻辑。

```java
package io.github.atengk.hbase.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * HBase RowKey 工具类测试。
 *
 * @author Ateng
 * @since 2026-05-07
 */
class HBaseRowKeyUtilTest {

    /**
     * 测试生成散列前缀。
     */
    @Test
    void testSaltPrefix() {
        String prefix = HBaseRowKeyUtil.saltPrefix("10001", 16);

        Assertions.assertNotNull(prefix);
        Assertions.assertEquals(2, prefix.length());
    }

    /**
     * 测试生成带散列前缀的 RowKey。
     */
    @Test
    void testWithSalt() {
        String rowKey = HBaseRowKeyUtil.withSalt("10001", 16);

        Assertions.assertNotNull(rowKey);
        Assertions.assertTrue(rowKey.endsWith("_10001"));
    }

    /**
     * 测试生成反转时间戳。
     */
    @Test
    void testReverseTimestamp() {
        long timestamp = 1788746400000L;
        String reverseTimestamp = HBaseRowKeyUtil.reverseTimestamp(timestamp);

        Assertions.assertEquals(String.valueOf(Long.MAX_VALUE - timestamp), reverseTimestamp);
    }

    /**
     * 测试生成用户事件 RowKey。
     */
    @Test
    void testUserEventRowKey() {
        String rowKey = HBaseRowKeyUtil.userEventRowKey("10001", "evt_202605070001", 16);

        Assertions.assertNotNull(rowKey);
        Assertions.assertTrue(rowKey.contains("_10001_"));
        Assertions.assertTrue(rowKey.endsWith("_evt_202605070001"));
    }

}
```

文件位置：`src/test/java/io/github/atengk/hbase/util/HBaseValueUtilTest.java`

下面的测试类用于验证 HBase 列名拼接和值转换逻辑。

```java
package io.github.atengk.hbase.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * HBase 值转换工具类测试。
 *
 * @author Ateng
 * @since 2026-05-07
 */
class HBaseValueUtilTest {

    /**
     * 测试拼接列名。
     */
    @Test
    void testColumnKey() {
        String columnKey = HBaseValueUtil.columnKey("info", "name");

        Assertions.assertEquals("info:name", columnKey);
    }

    /**
     * 测试字符串和字节数组互转。
     */
    @Test
    void testBytesConvert() {
        byte[] bytes = HBaseValueUtil.toBytes("张三");
        String value = HBaseValueUtil.toString(bytes);

        Assertions.assertEquals("张三", value);
    }

    /**
     * 测试空字符串转换。
     */
    @Test
    void testNullToBytes() {
        byte[] bytes = HBaseValueUtil.toBytes(null);
        String value = HBaseValueUtil.toString(bytes);

        Assertions.assertEquals("", value);
    }

}
```

执行单元测试：

```bash
# 执行全部测试
mvn test

# 只执行 RowKey 工具类测试
mvn -Dtest=HBaseRowKeyUtilTest test

# 只执行值转换工具类测试
mvn -Dtest=HBaseValueUtilTest test
```

命令说明：`mvn test` 会执行 `src/test/java` 下的测试类；`-Dtest=类名` 可以只运行指定测试类，适合本地快速验证某个工具类是否正确。

如果需要测试 `HBaseTemplate` 的业务逻辑，建议连接测试环境 HBase 或使用专门的集成测试环境。不要在普通单元测试中直接连接生产 HBase。

### 接口测试

接口测试用于验证 Controller、Service、Template、HBase Client 的完整链路是否可用。测试前需要确认 HBase 表已经存在，或者先通过前文表管理方法创建测试表。

建议先准备测试表：

```bash
# 进入 HBase Shell
hbase shell

# 创建命名空间
create_namespace 'app'

# 创建测试表
create 'app:user_profile', 'info', 'ext', 'meta'

# 查看表结构
describe 'app:user_profile'
```

启动 Spring Boot 服务：

```bash
# 使用本地开发配置启动
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 或者打包后启动
mvn clean package -DskipTests
java -jar target/springboot-hbase-demo.jar --spring.profiles.active=dev
```

接口测试建议按照写入、查询、批量查询、扫描、删除的顺序执行。

写入数据：

```bash
curl -X POST "http://localhost:8080/api/hbase/rows" \
  -H "Content-Type: application/json" \
  -d '{
    "tableName": "app:user_profile",
    "rowKey": "10001",
    "columnFamily": "info",
    "data": {
      "name": "张三",
      "phone": "13800000000",
      "gender": "male"
    }
  }'
```

预期响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

单条查询：

```bash
curl -X GET "http://localhost:8080/api/hbase/rows?tableName=app:user_profile&rowKey=10001"
```

预期响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "rowKey": "10001",
    "empty": false,
    "data": {
      "info:name": "张三",
      "info:phone": "13800000000",
      "info:gender": "male"
    }
  }
}
```

批量查询：

```bash
curl -X POST "http://localhost:8080/api/hbase/batch-get" \
  -H "Content-Type: application/json" \
  -d '{
    "tableName": "app:user_profile",
    "rowKeys": ["10001", "10002", "10003"]
  }'
```

条件扫描：

```bash
curl -X POST "http://localhost:8080/api/hbase/scan" \
  -H "Content-Type: application/json" \
  -d '{
    "tableName": "app:user_profile",
    "startRow": "10000",
    "stopRow": "20000",
    "columnFamily": "info",
    "limit": 100,
    "caching": 100,
    "cacheBlocks": false
  }'
```

删除整行：

```bash
curl -X DELETE "http://localhost:8080/api/hbase/rows" \
  -H "Content-Type: application/json" \
  -d '{
    "tableName": "app:user_profile",
    "rowKey": "10001"
  }'
```

参数校验测试：

```bash
curl -X POST "http://localhost:8080/api/hbase/rows" \
  -H "Content-Type: application/json" \
  -d '{
    "tableName": "",
    "rowKey": "",
    "columnFamily": "info",
    "data": {}
  }'
```

预期返回参数错误：

```json
{
  "code": 400,
  "message": "表名不能为空",
  "data": null
}
```

接口测试通过后，应继续观察应用日志，确认是否输出了写入、查询、扫描、删除等关键日志。如果接口返回成功但 HBase Shell 查询不到数据，需要重点检查表名、命名空间、RowKey、列族和环境配置是否一致。

### 数据写入验证

数据写入验证用于确认接口写入的数据已经真实落入 HBase。建议从接口响应、应用日志、HBase Shell 三个角度共同验证。

先通过接口写入一行基础数据：

```bash
curl -X POST "http://localhost:8080/api/hbase/rows" \
  -H "Content-Type: application/json" \
  -d '{
    "tableName": "app:user_profile",
    "rowKey": "10001",
    "columnFamily": "info",
    "data": {
      "name": "张三",
      "phone": "13800000000",
      "gender": "male"
    }
  }'
```

再写入扩展列族数据：

```bash
curl -X POST "http://localhost:8080/api/hbase/rows" \
  -H "Content-Type: application/json" \
  -d '{
    "tableName": "app:user_profile",
    "rowKey": "10001",
    "columnFamily": "ext",
    "data": {
      "tags": "[\"vip\",\"active\"]",
      "source": "app"
    }
  }'
```

使用 HBase Shell 验证：

```bash
# 查询整行数据
get 'app:user_profile', '10001'

# 只查询 info 列族
get 'app:user_profile', '10001', 'info'

# 查询指定列
get 'app:user_profile', '10001', 'info:name'
```

预期能看到类似数据：

```text
COLUMN                          CELL
 ext:source                     timestamp=1788746400000, value=app
 ext:tags                       timestamp=1788746400000, value=["vip","active"]
 info:gender                    timestamp=1788746400000, value=male
 info:name                      timestamp=1788746400000, value=张三
 info:phone                     timestamp=1788746400000, value=13800000000
```

如果接口返回成功但 Shell 查询不到数据，按以下顺序检查：

| 检查项          | 说明                                                  |
| --------------- | ----------------------------------------------------- |
| 表名是否一致    | 确认接口中的 `app:user_profile` 与 Shell 查询表名一致 |
| RowKey 是否一致 | 确认写入和查询使用同一个 RowKey                       |
| 列族是否存在    | 确认 `info`、`ext` 已经在建表时创建                   |
| 环境是否一致    | 确认应用连接的 HBase 与 Shell 连接的是同一套集群      |
| 日志是否异常    | 检查是否存在连接重试、RPC 超时、权限失败等日志        |

### 数据查询验证

数据查询验证用于确认单条查询、批量查询和范围扫描都能返回预期结果。验证时应准备多条 RowKey 有序的数据，便于观察 Scan 范围是否正确。

准备测试数据：

```bash
curl -X POST "http://localhost:8080/api/hbase/rows" \
  -H "Content-Type: application/json" \
  -d '{"tableName":"app:user_profile","rowKey":"10001","columnFamily":"info","data":{"name":"张三","phone":"13800000000"}}'

curl -X POST "http://localhost:8080/api/hbase/rows" \
  -H "Content-Type: application/json" \
  -d '{"tableName":"app:user_profile","rowKey":"10002","columnFamily":"info","data":{"name":"李四","phone":"13900000000"}}'

curl -X POST "http://localhost:8080/api/hbase/rows" \
  -H "Content-Type: application/json" \
  -d '{"tableName":"app:user_profile","rowKey":"10003","columnFamily":"info","data":{"name":"王五","phone":"13700000000"}}'
```

验证单条查询：

```bash
curl -X GET "http://localhost:8080/api/hbase/rows?tableName=app:user_profile&rowKey=10001"
```

验证批量查询：

```bash
curl -X POST "http://localhost:8080/api/hbase/batch-get" \
  -H "Content-Type: application/json" \
  -d '{
    "tableName": "app:user_profile",
    "rowKeys": ["10001", "10002", "10003"]
  }'
```

验证范围扫描：

```bash
curl -X POST "http://localhost:8080/api/hbase/scan" \
  -H "Content-Type: application/json" \
  -d '{
    "tableName": "app:user_profile",
    "startRow": "10001",
    "stopRow": "10004",
    "columnFamily": "info",
    "limit": 10,
    "caching": 10,
    "cacheBlocks": false
  }'
```

HBase Shell 对照验证：

```bash
# 单行查询
get 'app:user_profile', '10001'

# 范围扫描，STOPROW 不包含边界值
scan 'app:user_profile', {STARTROW => '10001', STOPROW => '10004', LIMIT => 10}
```

查询验证时需要注意 `STOPROW` 是不包含结束边界的。例如 `STARTROW => '10001', STOPROW => '10004'` 会扫描 `10001`、`10002`、`10003`，不会包含 `10004`。

## 部署与运行

部署与运行用于说明项目在本地、测试环境和生产环境中的启动方式、配置差异和常用运维命令。HBase 项目部署时重点关注配置隔离、网络连通性、依赖冲突、连接稳定性和日志排查。

### 本地运行配置

本地运行主要用于开发调试和接口验证。推荐使用 `application-dev.yml` 管理本地或开发环境 HBase 配置。

文件位置：`src/main/resources/application-dev.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: springboot-hbase-demo

hbase:
  # 本地开发环境启用 HBase 客户端
  enabled: true

  # 开发环境 ZooKeeper 地址
  zookeeper-quorum: dev-hbase-zk01,dev-hbase-zk02,dev-hbase-zk03

  # ZooKeeper 客户端端口
  zookeeper-client-port: 2181

  # HBase ZNode 根路径
  znode-parent: /hbase

  # 开发环境超时时间可适当短一些，便于快速暴露配置问题
  operation-timeout: 30000
  rpc-timeout: 30000

  # 开发环境重试次数不宜过多
  retries-number: 3
```

本地启动方式：

```bash
# 编译项目
mvn clean package -DskipTests

# 使用 dev 配置启动
java -jar target/springboot-hbase-demo.jar --spring.profiles.active=dev
```

开发阶段如果暂时没有 HBase 环境，可以临时关闭 HBase 配置：

```yaml
hbase:
  enabled: false
```

关闭 HBase 客户端后，依赖 `HBaseTemplate` 的接口也应同步禁用或避免访问，否则会因为缺少 Bean 导致启动失败或调用失败。更严谨的做法是给 HBase Controller、Service、Template 统一增加 `@ConditionalOnProperty(prefix = "hbase", name = "enabled", havingValue = "true", matchIfMissing = true)`。

### 测试环境配置

测试环境通常连接独立的 HBase 测试集群，用于联调、压测、接口测试和数据验证。测试环境配置应与生产环境尽量接近，但表名、命名空间、数据量和权限应与生产隔离。

文件位置：`src/main/resources/application-test.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: springboot-hbase-demo

hbase:
  # 测试环境启用 HBase 客户端
  enabled: true

  # 测试环境 ZooKeeper 地址
  zookeeper-quorum: test-hbase-zk01,test-hbase-zk02,test-hbase-zk03

  # ZooKeeper 客户端端口
  zookeeper-client-port: 2181

  # 测试环境 HBase 根节点
  znode-parent: /hbase

  # 测试环境可以接近生产配置
  operation-timeout: 60000
  rpc-timeout: 60000
  retries-number: 5
```

测试环境启动：

```bash
# 使用 test 配置启动
java -jar target/springboot-hbase-demo.jar --spring.profiles.active=test
```

测试环境发布前建议执行以下检查：

| 检查项       | 验证方式                                       |
| ------------ | ---------------------------------------------- |
| 配置是否正确 | 检查 `application-test.yml` 中 ZooKeeper 地址  |
| 网络是否连通 | `nc -vz test-hbase-zk01 2181`                  |
| 表是否存在   | HBase Shell 执行 `exists 'app:user_profile'`   |
| 列族是否正确 | HBase Shell 执行 `describe 'app:user_profile'` |
| 接口是否可用 | 执行写入、查询、扫描、删除 curl                |
| 日志是否正常 | 检查是否存在超时、重试、权限异常               |

测试环境可以保留调试接口，但需要注意表名白名单和数据清理策略，避免测试数据长期堆积影响扫描性能。

### 生产环境配置

生产环境配置应重点关注稳定性、安全性和可观测性。不要在生产环境开放任意表读写接口，建议增加表名白名单、接口鉴权、限流、审计日志和操作权限控制。

文件位置：`src/main/resources/application-prod.yml`

```yaml
server:
  port: 8080
  shutdown: graceful

spring:
  application:
    name: springboot-hbase-demo
  lifecycle:
    # 优雅停机等待时间
    timeout-per-shutdown-phase: 30s

hbase:
  # 生产环境启用 HBase 客户端
  enabled: true

  # 生产环境 ZooKeeper 地址
  zookeeper-quorum: prod-hbase-zk01,prod-hbase-zk02,prod-hbase-zk03

  # ZooKeeper 客户端端口
  zookeeper-client-port: 2181

  # 生产环境 HBase 根节点
  znode-parent: /hbase

  # 生产环境超时时间和重试次数应结合 SLA、网络质量和接口耗时评估
  operation-timeout: 60000
  rpc-timeout: 60000
  retries-number: 5

logging:
  level:
    # 业务包日志级别
    io.github.atengk.hbase: info
    # HBase 客户端日志级别，生产环境不建议开启 debug
    org.apache.hadoop.hbase: warn
    org.apache.zookeeper: warn
```

生产环境启动示例：

```bash
# 使用生产配置启动
java -jar springboot-hbase-demo.jar \
  --spring.profiles.active=prod \
  --server.port=8080
```

使用 systemd 管理服务时，可以增加服务文件。

文件位置：`/etc/systemd/system/springboot-hbase-demo.service`

下面的配置用于在 Linux 服务器中通过 systemd 托管 Spring Boot 应用。

```ini
[Unit]
Description=Spring Boot HBase Demo Service
After=network.target

[Service]
Type=simple
User=app
Group=app
WorkingDirectory=/opt/springboot-hbase-demo
ExecStart=/usr/bin/java -jar /opt/springboot-hbase-demo/springboot-hbase-demo.jar --spring.profiles.active=prod
Restart=always
RestartSec=10
SuccessExitStatus=143

# JVM 参数可根据服务器内存和 GC 策略调整
Environment="JAVA_OPTS=-Xms512m -Xmx1024m"

[Install]
WantedBy=multi-user.target
```

加载并启动服务：

```bash
# 重新加载 systemd 配置
systemctl daemon-reload

# 设置开机自启
systemctl enable springboot-hbase-demo

# 启动服务
systemctl start springboot-hbase-demo

# 查看服务状态
systemctl status springboot-hbase-demo
```

命令说明：`daemon-reload` 用于重新加载 systemd 服务定义；`enable` 用于设置开机自启；`start` 用于启动服务；`status` 用于查看服务运行状态和最近日志。

生产环境还需要注意以下事项：

| 事项       | 说明                                               |
| ---------- | -------------------------------------------------- |
| 表名白名单 | 不允许外部请求任意指定表名                         |
| 接口鉴权   | 写入、删除、扫描接口必须受权限控制                 |
| 扫描限制   | 强制限制 `limit`，禁止无边界全表扫描               |
| 日志脱敏   | 不记录手机号、身份证号、Token 等敏感信息           |
| 连接复用   | 保持 `Connection` 单例，不在请求中重复创建         |
| 监控告警   | 监控接口耗时、错误率、超时次数和 RegionServer 状态 |

### 常用运维命令

常用运维命令用于日常排查 HBase 集群状态、表结构、数据读写、应用日志和网络连通性。开发和测试阶段可以频繁使用，生产环境执行删除、禁用表等命令前必须确认影响范围。

HBase Shell 常用命令：

```bash
# 进入 HBase Shell
hbase shell

# 查看集群状态
status

# 查看表列表
list

# 查看命名空间
list_namespace

# 创建命名空间
create_namespace 'app'

# 查看表是否存在
exists 'app:user_profile'

# 查看表结构
describe 'app:user_profile'

# 创建表
create 'app:user_profile', 'info', 'ext', 'meta'

# 查询单行数据
get 'app:user_profile', '10001'

# 扫描表数据
scan 'app:user_profile', {LIMIT => 10}

# 按 RowKey 范围扫描
scan 'app:user_profile', {STARTROW => '10001', STOPROW => '10004', LIMIT => 10}

# 删除整行数据
deleteall 'app:user_profile', '10001'

# 禁用表
disable 'app:user_profile'

# 删除表
drop 'app:user_profile'
```

应用运维命令：

```bash
# 查看 Java 进程
jps -l

# 查看应用端口
netstat -tunlp | grep 8080

# 查看应用日志
tail -f /opt/springboot-hbase-demo/logs/app.log

# 查看 systemd 服务状态
systemctl status springboot-hbase-demo

# 重启应用
systemctl restart springboot-hbase-demo

# 停止应用
systemctl stop springboot-hbase-demo
```

网络排查命令：

```bash
# 检查 ZooKeeper 端口
nc -vz prod-hbase-zk01 2181

# 检查 DNS 解析
nslookup prod-hbase-zk01

# 检查主机连通性
ping prod-hbase-zk01

# 查看本机路由
ip route
```

Maven 依赖排查命令：

```bash
# 查看 HBase、Hadoop、ZooKeeper 等相关依赖
mvn dependency:tree | grep -E "hbase|hadoop|zookeeper|guava|protobuf|netty"

# 跳过测试打包
mvn clean package -DskipTests

# 清理并重新下载依赖
mvn clean package -U -DskipTests
```

命令说明：`dependency:tree` 用于排查依赖冲突；`-U` 会强制更新快照依赖和远程依赖元数据；`-DskipTests` 会跳过测试执行，适合部署构建，但提交代码前仍建议执行完整测试。

生产环境高风险命令包括 `disable`、`drop`、`deleteall` 和大范围 `scan`。这些命令应只由具备权限的运维或开发负责人执行，并提前确认表名、命名空间、RowKey 范围和备份策略。
