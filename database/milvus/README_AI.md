# Milvus 向量数据库开发

本文档用于说明如何在 Spring Boot 3 项目中集成 Milvus 向量数据库，完成向量数据写入、相似度检索、Collection 管理以及业务检索召回等核心能力。当前章节重点描述模块定位、使用场景、技术选型和本地开发环境准备。

## 模块概述

本模块主要面向文本向量、图片向量、知识库向量、商品特征向量等非结构化数据检索场景，通过 Milvus 提供高维向量存储和相似度检索能力。Spring Boot 3 项目负责业务接口、参数校验、数据封装、检索流程编排和异常处理，Milvus 负责向量数据的存储、索引构建和近似最近邻查询。

### 功能定位

Milvus 向量数据库模块用于在业务系统中提供向量数据的统一接入能力，将业务数据、向量字段、元数据字段和检索条件封装为标准操作接口。模块不直接负责文本 Embedding 模型推理，而是接收上游模型生成的向量结果，再完成入库、检索、删除和 Collection 管理。

模块核心能力包括：

| 功能            | 说明                                                         |
| --------------- | ------------------------------------------------------------ |
| Collection 管理 | 创建、判断、加载、释放、删除 Milvus Collection               |
| 向量写入        | 支持单条和批量向量数据写入                                   |
| 向量检索        | 支持 TopK 相似度检索、条件过滤检索、结果解析                 |
| 数据维护        | 支持根据主键查询、删除、批量删除                             |
| 业务封装        | 对外提供统一的 Spring Boot Service 和 REST API               |
| 配置管理        | 统一管理 Milvus 地址、认证信息、Collection 名称、向量维度等参数 |

在系统分层中，Milvus 模块通常位于基础能力层或数据检索层。业务模块只需要调用统一的 Service 方法，不直接感知 Milvus Java SDK 的底层请求对象。

### 使用场景

Milvus 适合用于高维向量数据的存储与相似度搜索，尤其适用于传统关系型数据库难以高效处理的语义检索和近似匹配场景。Milvus 官方当前文档支持通过 Java SDK 连接服务端，并提供 Collection 管理、插入、删除、单向量检索和混合检索等基础能力。([Milvus](https://milvus.io/docs/install-java.md/?utm_source=chatgpt.com))

常见使用场景如下：

| 场景         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 知识库问答   | 将文档片段向量化后写入 Milvus，根据用户问题向量召回相似文档片段 |
| 语义搜索     | 用户输入自然语言查询，不依赖关键词完全匹配，而是基于语义相似度返回结果 |
| 商品推荐     | 将商品标题、描述、图片或用户行为转为向量，召回相似商品       |
| 图片检索     | 将图片特征向量写入 Milvus，根据图片向量搜索相似图片          |
| 内容去重     | 对文本、图片、音频等内容做向量相似度判断，识别重复或高度相似数据 |
| RAG 检索增强 | 在大模型回答前，通过 Milvus 召回相关知识片段，作为上下文输入给大模型 |

在 Spring Boot 3 项目中，推荐将 Milvus 作为独立基础组件封装，避免在 Controller 中直接调用 SDK。业务流程通常为：业务数据准备 → 向量生成 → 向量和元数据写入 Milvus → 用户查询向量化 → Milvus TopK 召回 → 业务侧二次过滤或重排。

### 技术选型

本模块基于 Spring Boot 3、Java 17、Milvus 2.6.x 和 Milvus Java SDK 进行开发。Spring Boot 官方文档显示，Spring Boot 3.5.14 是当前 Spring Boot 3 稳定分支之一；虽然 Spring Boot 4 已是最新稳定大版本，但本文档限定在 Spring Boot 3 技术栈内，因此推荐使用 3.5.x 分支。([Home](https://docs.spring.io/spring-boot/3.5/index.html))

| 技术            | 推荐版本 | 说明                                                         |
| --------------- | -------- | ------------------------------------------------------------ |
| JDK             | 17+      | Spring Boot 3 基线要求，推荐使用 JDK 17 或 JDK 21            |
| Spring Boot     | 3.5.14   | 当前 Spring Boot 3 稳定分支版本                              |
| Milvus          | 2.6.x    | 向量数据库服务端，开发环境可使用 standalone 模式             |
| Milvus Java SDK | 2.6.17   | 官方安装文档当前示例版本为 `io.milvus:milvus-sdk-java:2.6.17` ([Milvus](https://milvus.io/docs/install-java.md/?utm_source=chatgpt.com)) |
| Maven           | 3.9+     | 项目依赖管理和构建工具                                       |
| Docker Compose  | v2       | 本地启动 Milvus standalone 服务                              |
| Hutool          | 5.8.x    | Java 工具类库，用于字符串、集合、对象校验等辅助处理          |
| Lombok          | 1.18.x   | 简化 DTO、配置类、日志对象等样板代码                         |

Milvus Java SDK 当前推荐使用 `MilvusClientV2` 客户端。该客户端通过 `ConnectConfig` 统一配置连接地址、Token、数据库名、超时时间、KeepAlive、TLS 等参数，适合在 Spring Boot 中封装为单例 Bean。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Client/MilvusClientV2.md?utm_source=chatgpt.com))

## 环境准备

本章节用于准备 Milvus 服务、Spring Boot 3 项目基础结构和 Maven 依赖。开发环境建议先使用 Docker Compose 启动 Milvus standalone，待功能验证通过后，再根据生产环境要求切换为 Milvus 集群或云服务。

### Milvus 服务部署

开发环境推荐使用 Docker Compose 部署 Milvus standalone。Milvus 官方文档提供了 standalone Docker Compose 配置文件，启动后通常包含 `milvus-standalone`、`milvus-etcd`、`milvus-minio` 三个容器，Milvus 服务默认暴露 `19530` 端口，WebUI 可通过本地 `9091` 访问。([Milvus](https://milvus.io/docs/it/install_standalone-docker-compose.md?utm_source=chatgpt.com))

在服务器或本地开发机创建 Milvus 工作目录，并下载官方 Compose 配置文件。

```bash
# 创建 Milvus 工作目录
mkdir -p /data/milvus
cd /data/milvus

# 下载 Milvus standalone Docker Compose 配置文件
wget https://github.com/milvus-io/milvus/releases/download/v2.6.11/milvus-standalone-docker-compose.yml -O docker-compose.yml

# 启动 Milvus、etcd、MinIO
docker compose up -d

# 查看容器状态
docker compose ps
```

以上命令会在当前目录生成 Milvus 运行环境。`docker compose up -d` 表示后台启动服务，`docker compose ps` 用于检查容器是否处于运行状态。开发环境重点确认 `milvus-standalone` 已启动，并且本机可以访问 `19530` 端口。

验证 Milvus 端口是否可访问：

```bash
# 查看 Milvus standalone 服务端口映射
docker port milvus-standalone 19530/tcp

# 查看 Milvus 容器日志
docker logs -f milvus-standalone
```

浏览器访问 Milvus WebUI：

```text
http://127.0.0.1:9091/webui/
```

停止 Milvus 服务：

```bash
# 停止服务，保留数据卷
docker compose down

# 如需清理本地测试数据，谨慎删除 volumes 目录
rm -rf volumes
```

生产环境不建议直接删除 `volumes` 目录。该目录通常包含 Milvus、etcd、MinIO 的本地持久化数据，删除后测试数据会丢失。

### Spring Boot 3 项目环境

Spring Boot 3 项目建议使用标准 Maven 工程结构，基础包名使用 `io.github.atengk`，后续 Milvus 相关配置、客户端和业务封装统一放在 `milvus` 模块包下。

推荐目录结构如下：

```text
milvus-demo
├── pom.xml
└── src
    ├── main
    │   ├── java
    │   │   └── io
    │   │       └── github
    │   │           └── atengk
    │   │               └── milvus
    │   │                   ├── MilvusApplication.java
    │   │                   ├── config
    │   │                   │   └── MilvusProperties.java
    │   │                   ├── client
    │   │                   │   └── MilvusClientConfig.java
    │   │                   ├── service
    │   │                   └── controller
    │   └── resources
    │       └── application.yml
    └── test
        └── java
```

项目入口类放在根包路径下，保证 Spring Boot 可以自动扫描 `config`、`client`、`service`、`controller` 等子包。

文件位置：`src/main/java/io/github/atengk/milvus/MilvusApplication.java`

```java
package io.github.atengk.milvus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Milvus 向量数据库开发示例启动类
 *
 * @author Ateng
 * @since 2026-05-07
 */
@SpringBootApplication
public class MilvusApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MilvusApplication.class, args);
    }

}
```

基础配置文件用于声明服务端口、应用名称和 Milvus 连接信息。后续客户端初始化、Collection 创建和向量检索都会读取这里的配置。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 当前服务端口
  port: 8080

spring:
  application:
    # 应用名称
    name: milvus-demo

milvus:
  # Milvus 服务地址，本地 Docker Compose 默认使用 19530
  uri: http://127.0.0.1:19530
  # 默认数据库名称，不配置时使用 Milvus 默认数据库
  database-name: default
  # 如果 Milvus 开启鉴权，可使用 root:Milvus 或云服务 API Key
  token: root:Milvus
  # 连接超时时间，单位毫秒
  connect-timeout-ms: 10000
  # RPC 调用超时时间，0 表示不启用全局 deadline
  rpc-deadline-ms: 0
  # 默认 Collection 名称
  collection-name: document_vector
  # 默认向量维度，需要与 Embedding 模型输出维度保持一致
  dimension: 1024
```

本地启动前应确认 JDK、Maven 和 Docker 环境可用：

```bash
# 检查 JDK 版本，建议 17 或 21
java -version

# 检查 Maven 版本，建议 3.9+
mvn -version

# 检查 Docker Compose 版本
docker compose version
```

### Maven 依赖配置

Maven 依赖用于引入 Spring Boot Web、参数校验、Actuator、Milvus Java SDK、Hutool 和 Lombok。Spring Boot starter 相关依赖由 `spring-boot-starter-parent` 统一管理版本，Milvus Java SDK 需要显式指定版本。Spring Boot Maven parent 会提供 Java 17 默认编译级别、UTF-8 编码、依赖管理和 `repackage` 打包能力。([Home](https://docs.spring.io/spring-boot/docs/3.2.6/maven-plugin/reference/htmlsingle/?utm_source=chatgpt.com))

文件位置：`pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="
             http://maven.apache.org/POM/4.0.0
             https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot 3 稳定分支父工程，统一管理 Spring 生态依赖版本 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.14</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>milvus-demo</artifactId>
    <version>1.0.0</version>
    <name>milvus-demo</name>
    <description>Spring Boot 3 集成 Milvus 向量数据库示例工程</description>

    <properties>
        <!-- Spring Boot 3 推荐使用 Java 17+ -->
        <java.version>17</java.version>
        <!-- Milvus Java SDK 当前官方安装文档示例版本 -->
        <milvus.version>2.6.17</milvus.version>
        <!-- Hutool 工具类库版本 -->
        <hutool.version>5.8.38</hutool.version>
        <!-- Lombok 版本 -->
        <lombok.version>1.18.42</lombok.version>
    </properties>

    <dependencies>
        <!-- Web 接口能力，用于提供向量写入、检索、删除等 REST API -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc</artifactId>
        </dependency>

        <!-- 参数校验能力，用于 Controller 入参校验 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- 监控与健康检查能力，可用于暴露应用健康状态 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Milvus Java SDK，用于连接 Milvus 并执行 Collection、Insert、Search 等操作 -->
        <dependency>
            <groupId>io.milvus</groupId>
            <artifactId>milvus-sdk-java</artifactId>
            <version>${milvus.version}</version>
        </dependency>

        <!-- Hutool 工具类库，用于字符串、集合、对象、JSON 等通用处理 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok 简化实体类、配置类和日志对象代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot 配置元数据生成器，便于 IDE 识别自定义 application.yml 配置 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot 测试依赖，用于连接测试、Collection 创建测试、向量检索测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot Maven 插件，用于打包可执行 Jar -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

依赖配置完成后，在项目根目录执行以下命令验证依赖是否可正常解析：

```bash
# 清理并编译项目
mvn clean compile

# 启动 Spring Boot 应用
mvn spring-boot:run
```

启动成功后，可以先验证 Spring Boot 服务是否正常：

```bash
# 验证应用健康状态
curl http://127.0.0.1:8080/actuator/health
```

如果返回 `UP`，说明 Spring Boot 基础环境已准备完成。后续章节可以继续补充 Milvus 连接配置、客户端 Bean 初始化、Collection 创建、向量写入和相似度检索实现。



## 基础配置

本章节用于定义 Spring Boot 3 项目连接 Milvus 所需的基础参数，包括连接地址、认证信息、Collection 名称、向量维度、字段名称、索引类型和相似度度量方式。Milvus Java SDK v2 使用 `MilvusClientV2` 作为客户端对象，并通过 `ConnectConfig` 统一配置 `uri`、`token`、`dbName`、连接超时时间、RPC deadline、TLS 等连接参数。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Client/ConnectConfig.md?utm_source=chatgpt.com))

### Milvus 连接配置

Milvus 连接配置用于集中管理客户端初始化参数，避免在业务代码中硬编码连接地址、Token、数据库名和超时时间。开发环境一般连接本地 Docker Compose 启动的 Milvus standalone，默认地址为 `http://127.0.0.1:19530`；如果开启鉴权，可以使用 `root:Milvus` 这类用户名和密码拼接形式作为 Token。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Client/MilvusClientV2.md?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: milvus-demo

milvus:
  # Milvus 服务地址，本地 standalone 默认端口为 19530
  uri: http://127.0.0.1:19530

  # Milvus 数据库名称，不指定时通常使用 default
  database-name: default

  # Milvus 访问凭证，开启鉴权时配置，格式通常为 username:password
  token: root:Milvus

  # 本地环境不启用 TLS；生产环境如使用 HTTPS 或证书认证，需要调整为 true
  secure: false

  # 建立连接的超时时间，单位毫秒
  connect-timeout-ms: 10000

  # RPC 调用 deadline，0 表示不启用全局 deadline
  rpc-deadline-ms: 0

  collection:
    # Collection 名称，建议按业务域命名
    name: document_vector

    # Collection 描述，便于后续运维识别
    description: 文档片段向量集合

    # 主键字段名称
    primary-field-name: doc_id

    # 向量字段名称
    vector-field-name: embedding

    # 文本内容字段名称
    content-field-name: content

    # 元数据字段名称
    metadata-field-name: metadata

    # 向量维度，必须与 Embedding 模型输出维度一致
    dimension: 1024

    # 向量索引类型，开发和通用生产场景推荐先使用 AUTOINDEX
    index-type: AUTOINDEX

    # 相似度度量方式，文本语义检索通常使用 COSINE
    metric-type: COSINE

    # 是否启用动态字段，开启后非 Schema 字段会进入 Milvus 预留动态字段
    enable-dynamic-field: true
```

配置类用于接收 `application.yml` 中的 Milvus 配置，并在启动阶段完成基础参数校验。

文件位置：`src/main/java/io/github/atengk/milvus/config/MilvusProperties.java`

```java
package io.github.atengk.milvus.config;

import cn.hutool.core.util.StrUtil;
import io.milvus.v2.common.IndexParam;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Milvus 配置属性
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Validated
@ConfigurationProperties(prefix = "milvus")
public class MilvusProperties {

    /**
     * Milvus 服务地址
     */
    @NotBlank(message = "Milvus 服务地址不能为空")
    private String uri = "http://127.0.0.1:19530";

    /**
     * Milvus 数据库名称
     */
    private String databaseName = "default";

    /**
     * Milvus 访问凭证
     */
    private String token = "root:Milvus";

    /**
     * 是否启用 TLS
     */
    private Boolean secure = Boolean.FALSE;

    /**
     * 连接超时时间，单位毫秒
     */
    @Min(value = 1000, message = "Milvus 连接超时时间不能小于 1000 毫秒")
    private Long connectTimeoutMs = 10000L;

    /**
     * RPC 调用超时时间，0 表示不启用全局 deadline
     */
    @Min(value = 0, message = "Milvus RPC deadline 不能小于 0")
    private Long rpcDeadlineMs = 0L;

    /**
     * Collection 配置
     */
    @Valid
    private CollectionConfig collection = new CollectionConfig();

    /**
     * 获取实际数据库名称
     *
     * @return 数据库名称
     */
    public String getActualDatabaseName() {
        return StrUtil.blankToDefault(databaseName, "default");
    }

    /**
     * 获取实际 Token
     *
     * @return Token
     */
    public String getActualToken() {
        return StrUtil.blankToDefault(token, null);
    }

    /**
     * Milvus Collection 配置
     *
     * @author Ateng
     * @since 2026-05-07
     */
    @Data
    public static class CollectionConfig {

        /**
         * Collection 名称
         */
        @NotBlank(message = "Milvus Collection 名称不能为空")
        private String name = "document_vector";

        /**
         * Collection 描述
         */
        private String description = "文档片段向量集合";

        /**
         * 主键字段名称
         */
        @NotBlank(message = "Milvus 主键字段名称不能为空")
        private String primaryFieldName = "doc_id";

        /**
         * 向量字段名称
         */
        @NotBlank(message = "Milvus 向量字段名称不能为空")
        private String vectorFieldName = "embedding";

        /**
         * 文本内容字段名称
         */
        @NotBlank(message = "Milvus 文本字段名称不能为空")
        private String contentFieldName = "content";

        /**
         * 元数据字段名称
         */
        @NotBlank(message = "Milvus 元数据字段名称不能为空")
        private String metadataFieldName = "metadata";

        /**
         * 向量维度
         */
        @Min(value = 2, message = "Milvus 向量维度不能小于 2")
        @Max(value = 32768, message = "Milvus 向量维度不能大于 32768")
        private Integer dimension = 1024;

        /**
         * 向量索引类型
         */
        @NotBlank(message = "Milvus 向量索引类型不能为空")
        private String indexType = IndexParam.IndexType.AUTOINDEX.name();

        /**
         * 相似度度量方式
         */
        @NotBlank(message = "Milvus 相似度度量方式不能为空")
        private String metricType = IndexParam.MetricType.COSINE.name();

        /**
         * 是否启用动态字段
         */
        private Boolean enableDynamicField = Boolean.TRUE;

        /**
         * 获取向量索引类型枚举
         *
         * @return Milvus 索引类型
         */
        public IndexParam.IndexType getIndexTypeEnum() {
            return IndexParam.IndexType.valueOf(StrUtil.upperCase(indexType));
        }

        /**
         * 获取相似度度量方式枚举
         *
         * @return Milvus 度量方式
         */
        public IndexParam.MetricType getMetricTypeEnum() {
            return IndexParam.MetricType.valueOf(StrUtil.upperCase(metricType));
        }

    }

}
```

客户端配置类用于将 `MilvusProperties` 转换为 `MilvusClientV2` Bean，后续 Collection 管理、向量写入、向量检索都通过该 Bean 完成。

文件位置：`src/main/java/io/github/atengk/milvus/config/MilvusClientConfig.java`

```java
package io.github.atengk.milvus.config;

import cn.hutool.core.util.StrUtil;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus 客户端配置
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(MilvusProperties.class)
public class MilvusClientConfig {

    private final MilvusProperties milvusProperties;

    /**
     * 创建 Milvus 客户端
     *
     * @return Milvus 客户端
     */
    @Bean
    public MilvusClientV2 milvusClientV2() {
        ConnectConfig connectConfig = ConnectConfig.builder()
                .uri(milvusProperties.getUri())
                .token(milvusProperties.getActualToken())
                .dbName(milvusProperties.getActualDatabaseName())
                .connectTimeoutMs(milvusProperties.getConnectTimeoutMs())
                .rpcDeadlineMs(milvusProperties.getRpcDeadlineMs())
                .secure(milvusProperties.getSecure())
                .build();

        log.info("初始化 Milvus 客户端，uri={}，database={}，secure={}",
                milvusProperties.getUri(),
                milvusProperties.getActualDatabaseName(),
                milvusProperties.getSecure());

        if (StrUtil.isBlank(milvusProperties.getActualToken())) {
            log.warn("Milvus Token 未配置，如果服务端已开启鉴权，连接可能失败");
        }

        return new MilvusClientV2(connectConfig);
    }

}
```

`ConnectConfig` 中的 `connectTimeoutMs` 默认值为 10000 毫秒，`rpcDeadlineMs` 默认值为 0，表示不启用全局 RPC deadline；如果生产环境调用链较长，可以结合接口超时、网关超时和 Milvus 查询复杂度统一调整。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Client/MilvusClientV2.md?utm_source=chatgpt.com))

### 向量维度配置

向量维度表示 Embedding 向量中浮点数元素的数量，必须与上游 Embedding 模型输出维度严格一致。Milvus 的 `FLOAT_VECTOR`、`FLOAT16_VECTOR`、`BFLOAT16_VECTOR`、`INT8_VECTOR` 支持的维度范围为 `2` 到 `32768`，本文档示例使用 `FLOAT_VECTOR` 类型。([Milvus](https://milvus.io/docs/id/metric.md?utm_source=chatgpt.com))

常见模型维度示例：

| 模型类型                   | 常见维度         | 配置示例                    |
| -------------------------- | ---------------- | --------------------------- |
| 通用文本 Embedding         | 768              | `dimension: 768`            |
| BGE / E5 部分模型          | 1024             | `dimension: 1024`           |
| OpenAI 部分 Embedding 模型 | 1536 / 3072      | `dimension: 1536` 或 `3072` |
| 图片特征模型               | 512 / 768 / 1024 | 按实际模型输出配置          |

在工程中，向量维度不建议写死在业务代码中，应统一从 `milvus.collection.dimension` 读取。向量写入前必须校验集合长度，避免维度不一致导致写入失败。

文件位置：`src/main/java/io/github/atengk/milvus/model/VectorDocument.java`

```java
package io.github.atengk.milvus.model;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 向量文档数据模型
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorDocument {

    /**
     * 文档主键
     */
    @NotBlank(message = "文档主键不能为空")
    private String docId;

    /**
     * 文档标题
     */
    private String title;

    /**
     * 文档内容
     */
    @NotBlank(message = "文档内容不能为空")
    private String content;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 租户标识
     */
    private String tenantId;

    /**
     * 数据来源
     */
    private String source;

    /**
     * 文档创建时间戳
     */
    private Long createTime;

    /**
     * 扩展元数据
     */
    private Map<String, Object> metadata;

    /**
     * 文本 Embedding 向量
     */
    @NotEmpty(message = "文档向量不能为空")
    private List<Float> embedding;

    /**
     * 校验向量维度
     *
     * @param expectedDimension 期望维度
     */
    public void validateDimension(Integer expectedDimension) {
        if (expectedDimension == null || expectedDimension <= 0) {
            throw new IllegalArgumentException("向量维度配置不合法");
        }
        if (CollUtil.isEmpty(embedding)) {
            throw new IllegalArgumentException("向量数据不能为空");
        }
        if (embedding.size() != expectedDimension) {
            throw new IllegalArgumentException(
                    StrUtil.format("向量维度不一致，期望维度={}，实际维度={}", expectedDimension, embedding.size())
            );
        }
    }

}
```

维度配置的关键规则如下：

| 规则                        | 说明                                                         |
| --------------------------- | ------------------------------------------------------------ |
| 与模型输出一致              | Embedding 模型输出 1024 维，Milvus Collection 必须配置 1024 维 |
| Collection 创建后不随意变更 | 已创建 Collection 的向量字段维度不应在业务中临时修改         |
| 写入前校验                  | 插入 Milvus 前先校验 `embedding.size()`                      |
| 多模型隔离                  | 不同维度模型应使用不同 Collection 或不同向量字段设计         |
| 灰度切换谨慎                | 模型升级导致维度变化时，应新建 Collection 做数据迁移         |

### 集合与字段配置

Collection 是 Milvus 中存储实体数据的核心结构，类似关系型数据库中的表；Schema 用于定义 Collection 的字段结构，每行插入数据都必须满足 Schema 约束。Milvus 文档说明，一个 Collection Schema 需要包含主键字段、至少一个向量字段以及若干标量字段。([Milvus](https://milvus.io/docs/pt/schema.md?utm_source=chatgpt.com))

推荐的文档向量 Collection 字段设计如下：

| 字段名        | Milvus 类型   | 是否必填 | 说明                                       |
| ------------- | ------------- | -------- | ------------------------------------------ |
| `doc_id`      | `VarChar`     | 是       | 主键，业务侧生成，便于删除、更新、追踪     |
| `chunk_id`    | `VarChar`     | 否       | 文档分片 ID，适合 RAG 文档切片场景         |
| `title`       | `VarChar`     | 否       | 文档标题                                   |
| `content`     | `VarChar`     | 是       | 文档片段原文或摘要                         |
| `biz_type`    | `VarChar`     | 否       | 业务类型，如 `knowledge`、`product`、`faq` |
| `tenant_id`   | `VarChar`     | 否       | 租户 ID，用于多租户过滤                    |
| `source`      | `VarChar`     | 否       | 数据来源，如文件名、URL、业务系统编码      |
| `create_time` | `Int64`       | 否       | 创建时间戳，便于时间过滤                   |
| `metadata`    | `JSON`        | 否       | 扩展元数据                                 |
| `embedding`   | `FloatVector` | 是       | 向量字段，用于相似度检索                   |

主键字段用于唯一标识每条实体数据，Milvus 要求每个 Collection 必须有且只有一个主键字段，主键字段不能为 null，且字段类型在创建后不能修改。([Milvus](https://milvus.io/docs/ja/primary-field.md?utm_source=chatgpt.com))

字段配置建议：

| 配置项     | 推荐值        | 说明                                                |
| ---------- | ------------- | --------------------------------------------------- |
| 主键类型   | `VarChar`     | 业务系统通常已有字符串 ID，便于幂等写入和按主键删除 |
| 主键生成   | 业务侧生成    | 不推荐文档检索场景使用 AutoID，否则删除和更新不方便 |
| 向量字段   | `FloatVector` | 文本 Embedding 最常用的向量类型                     |
| 文本字段   | `VarChar`     | 保存召回后需要返回给大模型或前端展示的内容          |
| 元数据字段 | `JSON`        | 保存扩展信息，避免频繁调整 Schema                   |
| 动态字段   | 按需开启      | 原型和多业务接入可开启，强 Schema 生产场景可关闭    |

Milvus 支持在创建 Collection 时定义 Schema、索引参数、相似度度量方式，并可指定创建后是否加载 Collection。创建索引可以提升向量字段检索性能，向量字段需要设置索引类型和度量方式，标量字段如果经常参与过滤，也建议创建索引。([Milvus](https://milvus.io/docs/create-collection.md?utm_source=chatgpt.com))

## 核心设计

核心设计用于明确向量数据如何建模、Collection 如何组织、索引如何选择、相似度如何计算。该部分的目标不是堆叠 Milvus 概念，而是形成一套适合 Spring Boot 业务系统长期维护的设计约束。

### 向量数据模型设计

向量数据模型应同时覆盖“检索所需字段”和“业务回显所需字段”。向量字段用于 Milvus 相似度搜索，标量字段用于过滤、删除、权限隔离和结果展示。对于 RAG 知识库场景，建议以“文档分片”为最小向量实体，而不是以完整文档为最小实体。

推荐数据模型如下：

| 字段         | 作用     | 设计说明                         |
| ------------ | -------- | -------------------------------- |
| `docId`      | 主键     | 对应 Milvus 主键字段 `doc_id`    |
| `chunkId`    | 分片 ID  | 一个文档可拆分为多个片段         |
| `title`      | 标题     | 用于搜索结果展示                 |
| `content`    | 内容     | 用于召回后组装上下文             |
| `bizType`    | 业务类型 | 用于多业务共用 Collection 时过滤 |
| `tenantId`   | 租户标识 | 用于多租户数据隔离               |
| `source`     | 来源     | 用于追踪原始文件、URL 或业务表   |
| `createTime` | 创建时间 | 用于时间范围过滤                 |
| `metadata`   | 扩展字段 | 保存不稳定或非核心字段           |
| `embedding`  | 向量     | 与 Embedding 模型输出一致        |

对于知识库问答场景，推荐流程如下：

```text
原始文档
  -> 文本清洗
  -> 文档切片
  -> 调用 Embedding 模型生成向量
  -> 组装 VectorDocument
  -> 校验向量维度
  -> 写入 Milvus Collection
  -> 查询时向量化问题
  -> Milvus TopK 检索
  -> 根据 tenant_id、biz_type 等条件过滤
  -> 返回 content、title、source 等字段
```

模型设计注意事项：

| 注意项                    | 说明                                                         |
| ------------------------- | ------------------------------------------------------------ |
| 不将大对象直接写入 Milvus | 大文件、图片、PDF 原文应存对象存储或业务库，Milvus 保存引用信息 |
| 保留业务主键              | 删除、重建、同步失败重试都依赖稳定主键                       |
| 元数据不过度膨胀          | 高频过滤字段应独立建标量字段，不应全部塞入 JSON              |
| 向量和文本保持一致        | 文本更新后必须重新生成向量并更新 Milvus                      |
| 租户字段前置设计          | 多租户系统必须在写入和搜索时同时带上 `tenant_id`             |

### Collection 结构设计

Collection 结构应围绕检索性能、过滤能力和维护成本设计。对于普通文本语义检索，一个 Collection 中建议只保存同一类 Embedding 模型生成的向量数据，避免不同模型、不同维度、不同语义空间的数据混用。

推荐 Collection 设计：

```text
Collection: document_vector

Primary Key:
  doc_id: VarChar

Vector Field:
  embedding: FloatVector(dim=1024)

Scalar Fields:
  chunk_id: VarChar
  title: VarChar
  content: VarChar
  biz_type: VarChar
  tenant_id: VarChar
  source: VarChar
  create_time: Int64
  metadata: JSON

Index:
  embedding -> AUTOINDEX + COSINE
  tenant_id -> AUTOINDEX
  biz_type -> AUTOINDEX
```

字段划分建议：

| 字段类别 | 字段示例                               | 设计目的             |
| -------- | -------------------------------------- | -------------------- |
| 主键字段 | `doc_id`                               | 精确定位、删除、更新 |
| 向量字段 | `embedding`                            | 相似度召回           |
| 过滤字段 | `tenant_id`、`biz_type`、`create_time` | 搜索时缩小候选范围   |
| 回显字段 | `title`、`content`、`source`           | 返回给接口调用方     |
| 扩展字段 | `metadata`                             | 保存非固定业务属性   |

Collection 拆分策略：

| 策略                     | 适用场景                      | 说明                              |
| ------------------------ | ----------------------------- | --------------------------------- |
| 按模型拆分               | 不同 Embedding 维度或模型版本 | 避免向量空间混乱                  |
| 按业务拆分               | 业务数据差异大、字段差异大    | 降低 Schema 复杂度                |
| 按租户拆分               | 超大租户、强隔离要求          | 提升隔离性，但增加运维成本        |
| 单 Collection 多字段过滤 | 中小规模多业务共用            | 通过 `tenant_id`、`biz_type` 过滤 |

在大多数 Spring Boot 业务项目中，推荐先采用“按模型维度拆分 Collection + 通过标量字段做业务过滤”的方式。这样可以保持 Collection 数量可控，同时避免不同向量模型混用。

### 索引类型设计

索引用于提升向量检索性能。Milvus Java SDK 的 `IndexParam` 支持通过 `fieldName`、`indexName`、`indexType`、`metricType` 和 `extraParams` 构造索引参数；其中 `metricType` 仅适用于向量字段，用于指定向量相似度计算方式。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Management/IndexParam.md?utm_source=chatgpt.com))

Milvus Java SDK v2.6 的 `IndexType` 枚举包含 `FLAT`、`IVF_FLAT`、`IVF_SQ8`、`IVF_PQ`、`HNSW`、`DISKANN`、`AUTOINDEX`、`SCANN`、GPU 相关索引以及部分稀疏向量索引。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Management/IndexType.md?utm_source=chatgpt.com))

常用索引类型建议如下：

| 索引类型    | 适用场景                             | 优点                 | 注意事项             |
| ----------- | ------------------------------------ | -------------------- | -------------------- |
| `AUTOINDEX` | 默认推荐、快速接入、云服务或通用生产 | 参数少，维护成本低   | 精细调优空间较少     |
| `FLAT`      | 小数据量、测试、精确检索             | 结果精确             | 大数据量性能较差     |
| `HNSW`      | 高召回、低延迟检索                   | 查询速度快，召回率高 | 内存占用相对更高     |
| `IVF_FLAT`  | 中大规模数据、需要平衡性能和召回     | 性能和召回较均衡     | 需要调参             |
| `IVF_PQ`    | 大规模、内存敏感场景                 | 节省内存             | 精度可能下降         |
| `DISKANN`   | 超大规模、磁盘索引场景               | 降低内存压力         | 部署和调优复杂度更高 |

开发阶段推荐使用 `AUTOINDEX`，先验证 Collection 创建、写入、搜索和过滤逻辑。等数据规模、QPS、延迟和召回率有明确指标后，再针对性切换为 `HNSW`、`IVF_FLAT` 或其他索引。

用于生成向量字段索引参数的示例代码如下。

文件位置：`src/main/java/io/github/atengk/milvus/design/MilvusIndexFactory.java`

```java
package io.github.atengk.milvus.design;

import io.github.atengk.milvus.config.MilvusProperties;
import io.milvus.v2.common.IndexParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Milvus 索引参数工厂
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MilvusIndexFactory {

    private final MilvusProperties milvusProperties;

    /**
     * 构建向量字段索引参数
     *
     * @return 向量字段索引参数
     */
    public IndexParam buildVectorIndexParam() {
        MilvusProperties.CollectionConfig collection = milvusProperties.getCollection();

        IndexParam indexParam = IndexParam.builder()
                .fieldName(collection.getVectorFieldName())
                .indexName(collection.getVectorFieldName() + "_idx")
                .indexType(collection.getIndexTypeEnum())
                .metricType(collection.getMetricTypeEnum())
                .build();

        log.info("构建 Milvus 向量索引参数，field={}，indexType={}，metricType={}",
                collection.getVectorFieldName(),
                collection.getIndexType(),
                collection.getMetricType());

        return indexParam;
    }

    /**
     * 构建标量字段索引参数
     *
     * @param fieldName 字段名称
     * @return 标量字段索引参数
     */
    public IndexParam buildScalarAutoIndexParam(String fieldName) {
        IndexParam indexParam = IndexParam.builder()
                .fieldName(fieldName)
                .indexName(fieldName + "_idx")
                .indexType(IndexParam.IndexType.AUTOINDEX)
                .build();

        log.info("构建 Milvus 标量索引参数，field={}，indexType={}", fieldName, IndexParam.IndexType.AUTOINDEX);

        return indexParam;
    }

}
```

索引设计原则：

| 原则                     | 说明                                                |
| ------------------------ | --------------------------------------------------- |
| 向量字段必须重点关注索引 | 向量检索性能主要由向量索引决定                      |
| 过滤字段建议建标量索引   | `tenant_id`、`biz_type` 等高频过滤字段应建立索引    |
| 不盲目调复杂参数         | 未形成性能基线前，优先使用 `AUTOINDEX`              |
| 索引和度量方式绑定设计   | `COSINE`、`L2`、`IP` 的选择会影响搜索结果解释       |
| 生产环境压测后再调整     | 根据 P95 延迟、召回率、内存占用和数据规模做最终选择 |

### 相似度度量方式

相似度度量方式决定 Milvus 如何比较两个向量的接近程度。Milvus 当前支持 `L2`、`IP`、`COSINE`、`JACCARD`、`HAMMING`、`BM25` 等度量方式；对于 `FLOAT_VECTOR` 类型，常用度量方式是 `COSINE`、`L2` 和 `IP`，默认度量方式为 `COSINE`。([Milvus](https://milvus.io/docs/id/metric.md?utm_source=chatgpt.com))

常用度量方式对比如下：

| 度量方式  | 适用向量类型 | 分数含义                           | 推荐场景                         |
| --------- | ------------ | ---------------------------------- | -------------------------------- |
| `COSINE`  | 浮点向量     | 值越大越相似，范围通常为 `[-1, 1]` | 文本语义检索、RAG、知识库问答    |
| `L2`      | 浮点向量     | 值越小越相似，范围为 `[0, ∞)`      | 欧氏距离敏感的特征检索           |
| `IP`      | 浮点向量     | 值越大越相似                       | 已归一化向量或关注向量内积的场景 |
| `HAMMING` | 二进制向量   | 值越小越相似                       | 二进制特征检索                   |
| `JACCARD` | 二进制向量   | 值越小越相似                       | 集合相似度、二进制集合特征       |
| `BM25`    | 稀疏向量     | 值越大相关性越高                   | Milvus 全文检索相关场景          |

文本语义检索默认推荐使用 `COSINE`。如果 Embedding 模型输出已经归一化，`IP` 在部分场景下也可以用于相似度排序；Milvus 文档说明，归一化后内积与余弦相似度等价。([Milvus](https://milvus.io/docs/id/metric.md?utm_source=chatgpt.com))

推荐配置：

```yaml
milvus:
  collection:
    # 文本语义检索默认推荐 COSINE
    metric-type: COSINE

    # 通用场景优先使用 AUTOINDEX，后续根据压测结果调整
    index-type: AUTOINDEX
```

选择建议：

| 业务场景         | 推荐度量方式          | 说明                               |
| ---------------- | --------------------- | ---------------------------------- |
| 知识库问答       | `COSINE`              | 更关注语义方向相似度               |
| FAQ 相似问题匹配 | `COSINE`              | 适合自然语言相似度比较             |
| 商品标题语义搜索 | `COSINE`              | 用户查询和商品文本通常长度差异较大 |
| 图片特征检索     | `COSINE` 或 `L2`      | 根据模型训练方式选择               |
| 已归一化向量检索 | `IP` 或 `COSINE`      | 需保持写入和查询向量处理一致       |
| 二进制特征检索   | `HAMMING` / `JACCARD` | 仅适合二进制向量                   |

相似度度量方式一旦用于索引和搜索，应在同一个 Collection 内保持一致。不要写入时使用一种向量归一化策略，搜索时又切换另一种度量方式，否则召回结果会不稳定。

## Milvus 客户端封装

本章节用于对 Milvus Java SDK 进行二次封装，统一完成客户端初始化、连接参数管理、基础健康检查和通用 Collection 操作。Milvus Java SDK v2 通过 `MilvusClientV2` 表示客户端实例，并使用 `ConnectConfig` 集中管理连接地址、Token、数据库名、超时时间、TLS、KeepAlive 等连接参数。([milvus.io](https://milvus.io/api-reference/java/v2.6.x/v2/Client/MilvusClientV2.md?utm_source=chatgpt.com)) 该章节对应开发大纲中的「Milvus 客户端封装」部分。

### 客户端初始化

客户端初始化用于在 Spring Boot 启动阶段创建全局可复用的 `MilvusClientV2` Bean。业务代码不应在每次请求中重复创建客户端，否则会增加连接建立成本，也不利于统一配置、日志追踪和异常处理。

推荐文件结构如下：

```text
src/main/java/io/github/atengk/milvus
├── config
│   ├── MilvusProperties.java
│   └── MilvusClientConfig.java
├── support
│   ├── MilvusClientProvider.java
│   └── MilvusHealthIndicator.java
└── service
    ├── MilvusTemplate.java
    └── impl
        └── MilvusTemplateImpl.java
```

客户端初始化配置类用于创建 `MilvusClientV2`，并在启动日志中输出基础连接信息。注意不要输出完整 Token，避免敏感信息进入日志。

文件位置：`src/main/java/io/github/atengk/milvus/config/MilvusClientConfig.java`

```java
package io.github.atengk.milvus.config;

import cn.hutool.core.util.StrUtil;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus 客户端初始化配置
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(MilvusProperties.class)
public class MilvusClientConfig {

    private final MilvusProperties milvusProperties;

    /**
     * 初始化 Milvus V2 客户端
     *
     * @return Milvus 客户端
     */
    @Bean
    public MilvusClientV2 milvusClientV2() {
        ConnectConfig.ConnectConfigBuilder builder = ConnectConfig.builder()
                .uri(milvusProperties.getUri())
                .dbName(milvusProperties.getActualDatabaseName())
                .connectTimeoutMs(milvusProperties.getConnectTimeoutMs())
                .rpcDeadlineMs(milvusProperties.getRpcDeadlineMs())
                .secure(milvusProperties.getSecure());

        if (StrUtil.isNotBlank(milvusProperties.getActualToken())) {
            builder.token(milvusProperties.getActualToken());
        }

        MilvusClientV2 client = new MilvusClientV2(builder.build());

        log.info("Milvus 客户端初始化完成，uri={}，database={}，secure={}",
                milvusProperties.getUri(),
                milvusProperties.getActualDatabaseName(),
                milvusProperties.getSecure());

        return client;
    }

}
```

`ConnectConfig` 中的 `token` 可以使用 `username:password` 格式，例如默认的 `root:Milvus`；`dbName` 用于指定目标数据库；`connectTimeoutMs` 默认值为 10000 毫秒；`rpcDeadlineMs` 默认值为 0，表示不启用全局 RPC deadline。([milvus.io](https://milvus.io/api-reference/java/v2.6.x/v2/Client/MilvusClientV2.md?utm_source=chatgpt.com))

为了避免业务层直接依赖 Bean 名称，可以再封装一个客户端提供器。该类只负责返回客户端实例和连接基础信息，不承载具体业务操作。

文件位置：`src/main/java/io/github/atengk/milvus/support/MilvusClientProvider.java`

```java
package io.github.atengk.milvus.support;

import io.github.atengk.milvus.config.MilvusProperties;
import io.milvus.v2.client.MilvusClientV2;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Milvus 客户端提供器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Component
@RequiredArgsConstructor
public class MilvusClientProvider {

    private final MilvusClientV2 milvusClientV2;

    private final MilvusProperties milvusProperties;

    /**
     * 获取 Milvus 客户端
     *
     * @return Milvus 客户端
     */
    public MilvusClientV2 getClient() {
        return milvusClientV2;
    }

    /**
     * 获取当前数据库名称
     *
     * @return 数据库名称
     */
    public String getDatabaseName() {
        return milvusProperties.getActualDatabaseName();
    }

    /**
     * 获取默认 Collection 名称
     *
     * @return Collection 名称
     */
    public String getDefaultCollectionName() {
        return milvusProperties.getCollection().getName();
    }

}
```

如果项目启用了 `spring-boot-starter-actuator`，可以增加 Milvus 健康检查。这里通过 `listCollectionsV2` 执行轻量级请求，判断 Milvus 是否可连接。Milvus Java SDK v2.6 的 `listCollectionsV2` 用于查询指定数据库下已有 Collection 列表，返回 `ListCollectionsResp`。([milvus.io](https://milvus.io/api-reference/java/v2.6.x/v2/Collections/ListCollectionsV2.md?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/milvus/support/MilvusHealthIndicator.java`

```java
package io.github.atengk.milvus.support;

import io.github.atengk.milvus.config.MilvusProperties;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.collection.request.ListCollectionsReq;
import io.milvus.v2.service.collection.response.ListCollectionsResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Milvus 健康检查
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MilvusHealthIndicator implements HealthIndicator {

    private final MilvusClientV2 milvusClientV2;

    private final MilvusProperties milvusProperties;

    /**
     * 检查 Milvus 连接状态
     *
     * @return 健康状态
     */
    @Override
    public Health health() {
        try {
            ListCollectionsReq request = ListCollectionsReq.builder()
                    .databaseName(milvusProperties.getActualDatabaseName())
                    .build();

            ListCollectionsResp response = milvusClientV2.listCollectionsV2(request);

            return Health.up()
                    .withDetail("database", milvusProperties.getActualDatabaseName())
                    .withDetail("uri", milvusProperties.getUri())
                    .withDetail("collectionCount", response.getCollectionNames().size())
                    .build();
        } catch (Exception e) {
            log.error("Milvus 健康检查失败，database={}，uri={}",
                    milvusProperties.getActualDatabaseName(),
                    milvusProperties.getUri(),
                    e);

            return Health.down()
                    .withDetail("database", milvusProperties.getActualDatabaseName())
                    .withDetail("uri", milvusProperties.getUri())
                    .withDetail("message", e.getMessage())
                    .build();
        }
    }

}
```

验证健康检查：

```bash
# 启动 Spring Boot 应用后访问健康检查接口
curl http://127.0.0.1:8080/actuator/health
```

如果 Milvus 正常连接，响应中会包含 `UP` 状态；如果 Milvus 服务未启动、地址错误或 Token 不正确，健康检查会返回 `DOWN`，并在应用日志中输出失败原因。

### 连接参数管理

连接参数管理用于把 Milvus 地址、数据库、认证、超时、Collection 名称、字段名称、索引类型和度量方式收敛到统一配置类中。这样可以避免多个 Service 中重复读取配置，也方便在不同环境中使用不同参数。

推荐配置项如下：

| 配置项                          | 示例值                   | 说明                 |
| ------------------------------- | ------------------------ | -------------------- |
| `milvus.uri`                    | `http://127.0.0.1:19530` | Milvus 服务地址      |
| `milvus.database-name`          | `default`                | 数据库名称           |
| `milvus.token`                  | `root:Milvus`            | 认证 Token           |
| `milvus.secure`                 | `false`                  | 是否启用 TLS         |
| `milvus.connect-timeout-ms`     | `10000`                  | 连接超时时间         |
| `milvus.rpc-deadline-ms`        | `0`                      | RPC 全局 deadline    |
| `milvus.collection.name`        | `document_vector`        | 默认 Collection 名称 |
| `milvus.collection.dimension`   | `1024`                   | 向量维度             |
| `milvus.collection.index-type`  | `AUTOINDEX`              | 向量索引类型         |
| `milvus.collection.metric-type` | `COSINE`                 | 相似度度量方式       |

连接参数建议按环境拆分，例如开发环境连接本地 Milvus，测试环境连接测试集群，生产环境通过环境变量注入地址和 Token。

文件位置：`src/main/resources/application-dev.yml`

```yaml
milvus:
  # 开发环境连接本地 Milvus
  uri: http://127.0.0.1:19530
  database-name: default
  token: root:Milvus
  secure: false
  connect-timeout-ms: 10000
  rpc-deadline-ms: 0
  collection:
    name: document_vector_dev
    dimension: 1024
    index-type: AUTOINDEX
    metric-type: COSINE
```

文件位置：`src/main/resources/application-prod.yml`

```yaml
milvus:
  # 生产环境建议通过环境变量注入，避免在配置文件中写死敏感信息
  uri: ${MILVUS_URI}
  database-name: ${MILVUS_DATABASE_NAME:default}
  token: ${MILVUS_TOKEN}
  secure: ${MILVUS_SECURE:true}
  connect-timeout-ms: ${MILVUS_CONNECT_TIMEOUT_MS:10000}
  rpc-deadline-ms: ${MILVUS_RPC_DEADLINE_MS:0}
  collection:
    name: ${MILVUS_COLLECTION_NAME:document_vector}
    dimension: ${MILVUS_VECTOR_DIMENSION:1024}
    index-type: ${MILVUS_INDEX_TYPE:AUTOINDEX}
    metric-type: ${MILVUS_METRIC_TYPE:COSINE}
```

生产环境启动时可通过环境变量覆盖配置：

```bash
export MILVUS_URI="https://milvus.example.com:19530"
export MILVUS_DATABASE_NAME="default"
export MILVUS_TOKEN="your_user:your_password"
export MILVUS_SECURE="true"
export MILVUS_COLLECTION_NAME="document_vector"
export MILVUS_VECTOR_DIMENSION="1024"

java -jar milvus-demo.jar --spring.profiles.active=prod
```

连接参数管理建议遵循以下规则：

| 规则                        | 说明                                     |
| --------------------------- | ---------------------------------------- |
| 地址和 Token 不写死在代码中 | 使用 `application.yml` 或环境变量注入    |
| 维度不在业务方法中硬编码    | 统一读取 `milvus.collection.dimension`   |
| Collection 名称按环境隔离   | 避免开发环境误写生产 Collection          |
| Token 不打印完整值          | 日志中只打印连接地址、数据库和安全模式   |
| 超时参数统一管理            | 避免每个方法设置不同超时时间导致排查困难 |
| TLS 按环境控制              | 本地可关闭，生产环境按部署要求开启       |

### 通用操作封装

通用操作封装用于屏蔽 Milvus Java SDK 的请求对象构造细节，使业务层只关注“判断 Collection 是否存在、加载 Collection、释放 Collection、查看加载状态、列出 Collection”等通用能力。Milvus Java SDK v2.6 中，`hasCollection` 用于判断 Collection 是否存在，`loadCollection` 用于将 Collection 加载到内存，`releaseCollection` 用于从内存释放 Collection，`getLoadState` 用于查询加载状态。([milvus.io](https://milvus.io/api-reference/java/v2.6.x/v2/Collections/hasCollection.md?utm_source=chatgpt.com)) ([milvus.io](https://milvus.io/api-reference/java/v2.6.x/v2/Management/loadCollection.md?utm_source=chatgpt.com)) ([milvus.io](https://milvus.io/api-reference/java/v2.6.x/v2/Management/releaseCollection.md?utm_source=chatgpt.com)) ([milvus.io](https://milvus.io/api-reference/java/v2.6.x/v2/Management/getLoadState.md?utm_source=chatgpt.com))

先定义通用模板接口，后续 Collection 管理、向量写入、向量检索都可以复用该接口。

文件位置：`src/main/java/io/github/atengk/milvus/service/MilvusTemplate.java`

```java
package io.github.atengk.milvus.service;

import io.milvus.v2.service.collection.response.DescribeCollectionResp;

import java.util.List;

/**
 * Milvus 通用操作模板
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface MilvusTemplate {

    /**
     * 查询 Collection 是否存在
     *
     * @param collectionName Collection 名称
     * @return 是否存在
     */
    Boolean hasCollection(String collectionName);

    /**
     * 查询默认 Collection 是否存在
     *
     * @return 是否存在
     */
    Boolean hasDefaultCollection();

    /**
     * 查询 Collection 详情
     *
     * @param collectionName Collection 名称
     * @return Collection 详情
     */
    DescribeCollectionResp describeCollection(String collectionName);

    /**
     * 查询所有 Collection 名称
     *
     * @return Collection 名称列表
     */
    List<String> listCollectionNames();

    /**
     * 加载 Collection
     *
     * @param collectionName Collection 名称
     */
    void loadCollection(String collectionName);

    /**
     * 释放 Collection
     *
     * @param collectionName Collection 名称
     */
    void releaseCollection(String collectionName);

    /**
     * 查询 Collection 是否已加载
     *
     * @param collectionName Collection 名称
     * @return 是否已加载
     */
    Boolean isCollectionLoaded(String collectionName);

}
```

下面是通用操作模板的实现类，统一处理空参数校验、日志输出和异常转换。

文件位置：`src/main/java/io/github/atengk/milvus/service/impl/MilvusTemplateImpl.java`

```java
package io.github.atengk.milvus.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.milvus.config.MilvusProperties;
import io.github.atengk.milvus.service.MilvusTemplate;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.collection.request.DescribeCollectionReq;
import io.milvus.v2.service.collection.request.GetLoadStateReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.ListCollectionsReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.collection.request.ReleaseCollectionReq;
import io.milvus.v2.service.collection.response.DescribeCollectionResp;
import io.milvus.v2.service.collection.response.ListCollectionsResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Milvus 通用操作模板实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MilvusTemplateImpl implements MilvusTemplate {

    private final MilvusClientV2 milvusClientV2;

    private final MilvusProperties milvusProperties;

    /**
     * 查询 Collection 是否存在
     *
     * @param collectionName Collection 名称
     * @return 是否存在
     */
    @Override
    public Boolean hasCollection(String collectionName) {
        String actualCollectionName = checkCollectionName(collectionName);

        try {
            HasCollectionReq request = HasCollectionReq.builder()
                    .databaseName(milvusProperties.getActualDatabaseName())
                    .collectionName(actualCollectionName)
                    .build();

            Boolean exists = milvusClientV2.hasCollection(request);

            log.info("查询 Milvus Collection 是否存在，collection={}，exists={}", actualCollectionName, exists);
            return exists;
        } catch (Exception e) {
            log.error("查询 Milvus Collection 是否存在失败，collection={}", actualCollectionName, e);
            throw new IllegalStateException("查询 Milvus Collection 是否存在失败：" + actualCollectionName, e);
        }
    }

    /**
     * 查询默认 Collection 是否存在
     *
     * @return 是否存在
     */
    @Override
    public Boolean hasDefaultCollection() {
        return hasCollection(milvusProperties.getCollection().getName());
    }

    /**
     * 查询 Collection 详情
     *
     * @param collectionName Collection 名称
     * @return Collection 详情
     */
    @Override
    public DescribeCollectionResp describeCollection(String collectionName) {
        String actualCollectionName = checkCollectionName(collectionName);

        try {
            DescribeCollectionReq request = DescribeCollectionReq.builder()
                    .databaseName(milvusProperties.getActualDatabaseName())
                    .collectionName(actualCollectionName)
                    .build();

            DescribeCollectionResp response = milvusClientV2.describeCollection(request);

            log.info("查询 Milvus Collection 详情成功，collection={}", actualCollectionName);
            return response;
        } catch (Exception e) {
            log.error("查询 Milvus Collection 详情失败，collection={}", actualCollectionName, e);
            throw new IllegalStateException("查询 Milvus Collection 详情失败：" + actualCollectionName, e);
        }
    }

    /**
     * 查询所有 Collection 名称
     *
     * @return Collection 名称列表
     */
    @Override
    public List<String> listCollectionNames() {
        try {
            ListCollectionsReq request = ListCollectionsReq.builder()
                    .databaseName(milvusProperties.getActualDatabaseName())
                    .build();

            ListCollectionsResp response = milvusClientV2.listCollectionsV2(request);
            List<String> collectionNames = response.getCollectionNames();

            if (CollUtil.isEmpty(collectionNames)) {
                log.info("Milvus 当前数据库下暂无 Collection，database={}", milvusProperties.getActualDatabaseName());
                return Collections.emptyList();
            }

            log.info("查询 Milvus Collection 列表成功，database={}，count={}",
                    milvusProperties.getActualDatabaseName(),
                    collectionNames.size());

            return collectionNames;
        } catch (Exception e) {
            log.error("查询 Milvus Collection 列表失败，database={}", milvusProperties.getActualDatabaseName(), e);
            throw new IllegalStateException("查询 Milvus Collection 列表失败", e);
        }
    }

    /**
     * 加载 Collection
     *
     * @param collectionName Collection 名称
     */
    @Override
    public void loadCollection(String collectionName) {
        String actualCollectionName = checkCollectionName(collectionName);

        try {
            LoadCollectionReq request = LoadCollectionReq.builder()
                    .databaseName(milvusProperties.getActualDatabaseName())
                    .collectionName(actualCollectionName)
                    .async(Boolean.FALSE)
                    .sync(Boolean.TRUE)
                    .timeout(60000L)
                    .build();

            milvusClientV2.loadCollection(request);

            log.info("加载 Milvus Collection 成功，collection={}", actualCollectionName);
        } catch (Exception e) {
            log.error("加载 Milvus Collection 失败，collection={}", actualCollectionName, e);
            throw new IllegalStateException("加载 Milvus Collection 失败：" + actualCollectionName, e);
        }
    }

    /**
     * 释放 Collection
     *
     * @param collectionName Collection 名称
     */
    @Override
    public void releaseCollection(String collectionName) {
        String actualCollectionName = checkCollectionName(collectionName);

        try {
            ReleaseCollectionReq request = ReleaseCollectionReq.builder()
                    .databaseName(milvusProperties.getActualDatabaseName())
                    .collectionName(actualCollectionName)
                    .async(Boolean.FALSE)
                    .timeout(60000L)
                    .build();

            milvusClientV2.releaseCollection(request);

            log.info("释放 Milvus Collection 成功，collection={}", actualCollectionName);
        } catch (Exception e) {
            log.error("释放 Milvus Collection 失败，collection={}", actualCollectionName, e);
            throw new IllegalStateException("释放 Milvus Collection 失败：" + actualCollectionName, e);
        }
    }

    /**
     * 查询 Collection 是否已加载
     *
     * @param collectionName Collection 名称
     * @return 是否已加载
     */
    @Override
    public Boolean isCollectionLoaded(String collectionName) {
        String actualCollectionName = checkCollectionName(collectionName);

        try {
            GetLoadStateReq request = GetLoadStateReq.builder()
                    .databaseName(milvusProperties.getActualDatabaseName())
                    .collectionName(actualCollectionName)
                    .build();

            Boolean loaded = milvusClientV2.getLoadState(request);

            log.info("查询 Milvus Collection 加载状态成功，collection={}，loaded={}", actualCollectionName, loaded);
            return loaded;
        } catch (Exception e) {
            log.error("查询 Milvus Collection 加载状态失败，collection={}", actualCollectionName, e);
            throw new IllegalStateException("查询 Milvus Collection 加载状态失败：" + actualCollectionName, e);
        }
    }

    /**
     * 校验 Collection 名称
     *
     * @param collectionName Collection 名称
     * @return 合法 Collection 名称
     */
    private String checkCollectionName(String collectionName) {
        if (StrUtil.isBlank(collectionName)) {
            throw new IllegalArgumentException("Collection 名称不能为空");
        }
        return StrUtil.trim(collectionName);
    }

}
```

为了便于本地验证，可以临时提供一个调试 Controller。生产环境如果不希望暴露 Collection 管理能力，应通过权限控制、内部接口或运维接口隔离。

文件位置：`src/main/java/io/github/atengk/milvus/controller/MilvusClientController.java`

```java
package io.github.atengk.milvus.controller;

import io.github.atengk.milvus.service.MilvusTemplate;
import io.milvus.v2.service.collection.response.DescribeCollectionResp;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Milvus 客户端调试接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/milvus/client")
public class MilvusClientController {

    private final MilvusTemplate milvusTemplate;

    /**
     * 查询 Collection 列表
     *
     * @return Collection 名称列表
     */
    @GetMapping("/collections")
    public List<String> listCollectionNames() {
        return milvusTemplate.listCollectionNames();
    }

    /**
     * 判断 Collection 是否存在
     *
     * @param collectionName Collection 名称
     * @return 是否存在
     */
    @GetMapping("/collections/{collectionName}/exists")
    public Boolean hasCollection(@PathVariable String collectionName) {
        return milvusTemplate.hasCollection(collectionName);
    }

    /**
     * 查询 Collection 详情
     *
     * @param collectionName Collection 名称
     * @return Collection 详情
     */
    @GetMapping("/collections/{collectionName}")
    public DescribeCollectionResp describeCollection(@PathVariable String collectionName) {
        return milvusTemplate.describeCollection(collectionName);
    }

    /**
     * 加载 Collection
     *
     * @param collectionName Collection 名称
     */
    @PostMapping("/collections/{collectionName}/load")
    public void loadCollection(@PathVariable String collectionName) {
        milvusTemplate.loadCollection(collectionName);
    }

    /**
     * 释放 Collection
     *
     * @param collectionName Collection 名称
     */
    @PostMapping("/collections/{collectionName}/release")
    public void releaseCollection(@PathVariable String collectionName) {
        milvusTemplate.releaseCollection(collectionName);
    }

    /**
     * 查询 Collection 加载状态
     *
     * @param collectionName Collection 名称
     * @return 是否已加载
     */
    @GetMapping("/collections/{collectionName}/loaded")
    public Boolean isCollectionLoaded(@PathVariable String collectionName) {
        return milvusTemplate.isCollectionLoaded(collectionName);
    }

}
```

接口验证示例：

```bash
# 查询当前数据库下的 Collection 列表
curl http://127.0.0.1:8080/api/milvus/client/collections

# 判断 Collection 是否存在
curl http://127.0.0.1:8080/api/milvus/client/collections/document_vector/exists

# 查询 Collection 详情
curl http://127.0.0.1:8080/api/milvus/client/collections/document_vector

# 加载 Collection
curl -X POST http://127.0.0.1:8080/api/milvus/client/collections/document_vector/load

# 查询 Collection 是否已加载
curl http://127.0.0.1:8080/api/milvus/client/collections/document_vector/loaded

# 释放 Collection
curl -X POST http://127.0.0.1:8080/api/milvus/client/collections/document_vector/release
```

通用封装完成后，后续章节中的 Collection 创建、删除、向量写入、向量检索都应优先复用 `MilvusTemplate` 和 `MilvusProperties`，不要在业务代码中重复构造连接参数、字段名称和 Collection 名称。这样可以保证 Milvus 调用链路清晰，也便于统一增加日志、监控、异常转换和权限控制。

## Collection 管理

本章节用于封装 Milvus Collection 的生命周期管理能力，包括创建、判断是否存在、加载、释放和删除。Collection 是 Milvus 中存储向量数据和标量字段的核心结构，创建时可以指定 Schema、索引参数、相似度度量方式和动态字段配置。Milvus Java SDK v2 的 `createCollection` 支持通过 `CollectionSchema` 自定义字段结构，并通过 `indexParams` 指定索引配置。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Collections/createCollection.md?utm_source=chatgpt.com)) 该章节对应开发大纲中的「Collection 管理」部分。

推荐文件结构如下：

```text
src/main/java/io/github/atengk/milvus
├── service
│   ├── MilvusCollectionService.java
│   └── impl
│       └── MilvusCollectionServiceImpl.java
├── controller
│   └── MilvusCollectionController.java
└── common
    └── Result.java
```

### 创建 Collection

创建 Collection 时需要先定义 Schema，再定义向量字段索引，最后调用 `createCollection`。本文档采用自定义 Schema 方式创建 Collection，主键使用 `VarChar`，向量字段使用 `FloatVector`，文本和业务字段使用 `VarChar`，扩展字段使用 `JSON`。Milvus Java SDK 的 `DataType` 枚举支持 `VarChar`、`JSON`、`FloatVector` 等字段类型；`AddFieldReq` 用于向 Schema 中追加字段。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Collections/DataType.md?utm_source=chatgpt.com))

先定义 Collection 管理接口，便于 Controller 和业务服务统一调用。

文件位置：`src/main/java/io/github/atengk/milvus/service/MilvusCollectionService.java`

```java
package io.github.atengk.milvus.service;

import java.util.List;

/**
 * Milvus Collection 管理服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface MilvusCollectionService {

    /**
     * 创建默认 Collection
     */
    void createDefaultCollection();

    /**
     * 创建指定 Collection
     *
     * @param collectionName Collection 名称
     */
    void createCollection(String collectionName);

    /**
     * 判断 Collection 是否存在
     *
     * @param collectionName Collection 名称
     * @return 是否存在
     */
    Boolean hasCollection(String collectionName);

    /**
     * 查询所有 Collection 名称
     *
     * @return Collection 名称列表
     */
    List<String> listCollections();

    /**
     * 加载 Collection
     *
     * @param collectionName Collection 名称
     */
    void loadCollection(String collectionName);

    /**
     * 释放 Collection
     *
     * @param collectionName Collection 名称
     */
    void releaseCollection(String collectionName);

    /**
     * 删除 Collection
     *
     * @param collectionName Collection 名称
     */
    void dropCollection(String collectionName);

}
```

下面的实现类完成 Collection 创建、存在性判断、加载、释放和删除。创建 Collection 时会先判断是否已存在，避免重复创建导致异常。

文件位置：`src/main/java/io/github/atengk/milvus/service/impl/MilvusCollectionServiceImpl.java`

```java
package io.github.atengk.milvus.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.milvus.config.MilvusProperties;
import io.github.atengk.milvus.service.MilvusCollectionService;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.DropCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.ListCollectionsReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.collection.request.ReleaseCollectionReq;
import io.milvus.v2.service.collection.response.ListCollectionsResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Milvus Collection 管理服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MilvusCollectionServiceImpl implements MilvusCollectionService {

    private static final Integer PRIMARY_KEY_MAX_LENGTH = 128;
    private static final Integer SHORT_TEXT_MAX_LENGTH = 512;
    private static final Integer CONTENT_MAX_LENGTH = 65535;
    private static final Long DEFAULT_TIMEOUT_MS = 60000L;

    private final MilvusClientV2 milvusClientV2;

    private final MilvusProperties milvusProperties;

    /**
     * 创建默认 Collection
     */
    @Override
    public void createDefaultCollection() {
        createCollection(milvusProperties.getCollection().getName());
    }

    /**
     * 创建指定 Collection
     *
     * @param collectionName Collection 名称
     */
    @Override
    public void createCollection(String collectionName) {
        String actualCollectionName = checkCollectionName(collectionName);
        MilvusProperties.CollectionConfig collectionConfig = milvusProperties.getCollection();

        if (hasCollection(actualCollectionName)) {
            log.info("Milvus Collection 已存在，跳过创建，collection={}", actualCollectionName);
            return;
        }

        try {
            CreateCollectionReq.CollectionSchema schema = MilvusClientV2.createSchema();
            schema.setEnableDynamicField(collectionConfig.getEnableDynamicField());

            schema.addField(AddFieldReq.builder()
                    .fieldName(collectionConfig.getPrimaryFieldName())
                    .description("文档主键")
                    .dataType(DataType.VarChar)
                    .isPrimaryKey(Boolean.TRUE)
                    .autoID(Boolean.FALSE)
                    .maxLength(PRIMARY_KEY_MAX_LENGTH)
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName("chunk_id")
                    .description("文档分片 ID")
                    .dataType(DataType.VarChar)
                    .maxLength(PRIMARY_KEY_MAX_LENGTH)
                    .isNullable(Boolean.TRUE)
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName("title")
                    .description("文档标题")
                    .dataType(DataType.VarChar)
                    .maxLength(SHORT_TEXT_MAX_LENGTH)
                    .isNullable(Boolean.TRUE)
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName(collectionConfig.getContentFieldName())
                    .description("文档内容")
                    .dataType(DataType.VarChar)
                    .maxLength(CONTENT_MAX_LENGTH)
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName("biz_type")
                    .description("业务类型")
                    .dataType(DataType.VarChar)
                    .maxLength(SHORT_TEXT_MAX_LENGTH)
                    .isNullable(Boolean.TRUE)
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName("tenant_id")
                    .description("租户标识")
                    .dataType(DataType.VarChar)
                    .maxLength(SHORT_TEXT_MAX_LENGTH)
                    .isNullable(Boolean.TRUE)
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName("source")
                    .description("数据来源")
                    .dataType(DataType.VarChar)
                    .maxLength(SHORT_TEXT_MAX_LENGTH)
                    .isNullable(Boolean.TRUE)
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName("create_time")
                    .description("创建时间戳")
                    .dataType(DataType.Int64)
                    .isNullable(Boolean.TRUE)
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName(collectionConfig.getMetadataFieldName())
                    .description("扩展元数据")
                    .dataType(DataType.JSON)
                    .isNullable(Boolean.TRUE)
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName(collectionConfig.getVectorFieldName())
                    .description("文本向量")
                    .dataType(DataType.FloatVector)
                    .dimension(collectionConfig.getDimension())
                    .build());

            IndexParam vectorIndex = IndexParam.builder()
                    .fieldName(collectionConfig.getVectorFieldName())
                    .indexName(collectionConfig.getVectorFieldName() + "_idx")
                    .indexType(collectionConfig.getIndexTypeEnum())
                    .metricType(collectionConfig.getMetricTypeEnum())
                    .build();

            CreateCollectionReq request = CreateCollectionReq.builder()
                    .databaseName(milvusProperties.getActualDatabaseName())
                    .collectionName(actualCollectionName)
                    .description(collectionConfig.getDescription())
                    .collectionSchema(schema)
                    .indexParams(Collections.singletonList(vectorIndex))
                    .enableDynamicField(collectionConfig.getEnableDynamicField())
                    .build();

            milvusClientV2.createCollection(request);

            log.info("创建 Milvus Collection 成功，collection={}，dimension={}，indexType={}，metricType={}",
                    actualCollectionName,
                    collectionConfig.getDimension(),
                    collectionConfig.getIndexType(),
                    collectionConfig.getMetricType());
        } catch (Exception e) {
            log.error("创建 Milvus Collection 失败，collection={}", actualCollectionName, e);
            throw new IllegalStateException("创建 Milvus Collection 失败：" + actualCollectionName, e);
        }
    }

    /**
     * 判断 Collection 是否存在
     *
     * @param collectionName Collection 名称
     * @return 是否存在
     */
    @Override
    public Boolean hasCollection(String collectionName) {
        String actualCollectionName = checkCollectionName(collectionName);

        try {
            HasCollectionReq request = HasCollectionReq.builder()
                    .databaseName(milvusProperties.getActualDatabaseName())
                    .collectionName(actualCollectionName)
                    .build();

            Boolean exists = milvusClientV2.hasCollection(request);
            log.info("判断 Milvus Collection 是否存在，collection={}，exists={}", actualCollectionName, exists);
            return exists;
        } catch (Exception e) {
            log.error("判断 Milvus Collection 是否存在失败，collection={}", actualCollectionName, e);
            throw new IllegalStateException("判断 Milvus Collection 是否存在失败：" + actualCollectionName, e);
        }
    }

    /**
     * 查询所有 Collection 名称
     *
     * @return Collection 名称列表
     */
    @Override
    public List<String> listCollections() {
        try {
            ListCollectionsReq request = ListCollectionsReq.builder()
                    .databaseName(milvusProperties.getActualDatabaseName())
                    .build();

            ListCollectionsResp response = milvusClientV2.listCollectionsV2(request);
            List<String> collectionNames = response.getCollectionNames();

            if (CollUtil.isEmpty(collectionNames)) {
                log.info("当前 Milvus 数据库下暂无 Collection，database={}", milvusProperties.getActualDatabaseName());
                return Collections.emptyList();
            }

            log.info("查询 Milvus Collection 列表成功，database={}，count={}",
                    milvusProperties.getActualDatabaseName(),
                    collectionNames.size());

            return collectionNames;
        } catch (Exception e) {
            log.error("查询 Milvus Collection 列表失败，database={}", milvusProperties.getActualDatabaseName(), e);
            throw new IllegalStateException("查询 Milvus Collection 列表失败", e);
        }
    }

    /**
     * 加载 Collection
     *
     * @param collectionName Collection 名称
     */
    @Override
    public void loadCollection(String collectionName) {
        String actualCollectionName = checkCollectionName(collectionName);

        if (!hasCollection(actualCollectionName)) {
            throw new IllegalArgumentException("Milvus Collection 不存在：" + actualCollectionName);
        }

        try {
            LoadCollectionReq request = LoadCollectionReq.builder()
                    .databaseName(milvusProperties.getActualDatabaseName())
                    .collectionName(actualCollectionName)
                    .async(Boolean.FALSE)
                    .sync(Boolean.TRUE)
                    .timeout(DEFAULT_TIMEOUT_MS)
                    .build();

            milvusClientV2.loadCollection(request);

            log.info("加载 Milvus Collection 成功，collection={}", actualCollectionName);
        } catch (Exception e) {
            log.error("加载 Milvus Collection 失败，collection={}", actualCollectionName, e);
            throw new IllegalStateException("加载 Milvus Collection 失败：" + actualCollectionName, e);
        }
    }

    /**
     * 释放 Collection
     *
     * @param collectionName Collection 名称
     */
    @Override
    public void releaseCollection(String collectionName) {
        String actualCollectionName = checkCollectionName(collectionName);

        if (!hasCollection(actualCollectionName)) {
            throw new IllegalArgumentException("Milvus Collection 不存在：" + actualCollectionName);
        }

        try {
            ReleaseCollectionReq request = ReleaseCollectionReq.builder()
                    .databaseName(milvusProperties.getActualDatabaseName())
                    .collectionName(actualCollectionName)
                    .async(Boolean.FALSE)
                    .timeout(DEFAULT_TIMEOUT_MS)
                    .build();

            milvusClientV2.releaseCollection(request);

            log.info("释放 Milvus Collection 成功，collection={}", actualCollectionName);
        } catch (Exception e) {
            log.error("释放 Milvus Collection 失败，collection={}", actualCollectionName, e);
            throw new IllegalStateException("释放 Milvus Collection 失败：" + actualCollectionName, e);
        }
    }

    /**
     * 删除 Collection
     *
     * @param collectionName Collection 名称
     */
    @Override
    public void dropCollection(String collectionName) {
        String actualCollectionName = checkCollectionName(collectionName);

        if (!hasCollection(actualCollectionName)) {
            log.info("Milvus Collection 不存在，跳过删除，collection={}", actualCollectionName);
            return;
        }

        try {
            DropCollectionReq request = DropCollectionReq.builder()
                    .collectionName(actualCollectionName)
                    .timeout(DEFAULT_TIMEOUT_MS)
                    .build();

            milvusClientV2.dropCollection(request);

            log.warn("删除 Milvus Collection 成功，collection={}", actualCollectionName);
        } catch (Exception e) {
            log.error("删除 Milvus Collection 失败，collection={}", actualCollectionName, e);
            throw new IllegalStateException("删除 Milvus Collection 失败：" + actualCollectionName, e);
        }
    }

    /**
     * 校验 Collection 名称
     *
     * @param collectionName Collection 名称
     * @return 合法 Collection 名称
     */
    private String checkCollectionName(String collectionName) {
        if (StrUtil.isBlank(collectionName)) {
            throw new IllegalArgumentException("Collection 名称不能为空");
        }
        return StrUtil.trim(collectionName);
    }

}
```

创建 Collection 的关键点如下：

| 配置项     | 示例值                  | 说明                                          |
| ---------- | ----------------------- | --------------------------------------------- |
| 主键字段   | `doc_id`                | 使用业务侧生成的字符串 ID，便于删除和幂等写入 |
| 向量字段   | `embedding`             | 类型为 `FloatVector`，维度来自配置            |
| 文本字段   | `content`               | 保存召回后需要返回的文本内容                  |
| 过滤字段   | `tenant_id`、`biz_type` | 用于检索时做条件过滤                          |
| 元数据字段 | `metadata`              | 使用 `JSON` 保存扩展属性                      |
| 索引类型   | `AUTOINDEX`             | 开发和通用场景优先使用                        |
| 度量方式   | `COSINE`                | 文本语义检索默认推荐                          |

### 判断 Collection 是否存在

判断 Collection 是否存在通常用于创建前幂等校验、写入前防御性检查、检索前状态校验和删除前确认。Milvus Java SDK v2 的 `hasCollection` 接收 `HasCollectionReq`，返回布尔值表示目标 Collection 是否存在。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Collections/hasCollection.md?utm_source=chatgpt.com))

在业务中建议封装为统一方法，不要在多个 Service 中重复构造请求对象。上面的 `hasCollection` 方法已经完成该封装：

```java
@Override
public Boolean hasCollection(String collectionName) {
    String actualCollectionName = checkCollectionName(collectionName);

    HasCollectionReq request = HasCollectionReq.builder()
            .databaseName(milvusProperties.getActualDatabaseName())
            .collectionName(actualCollectionName)
            .build();

    return milvusClientV2.hasCollection(request);
}
```

常见使用方式：

```java
if (!milvusCollectionService.hasCollection("document_vector")) {
    milvusCollectionService.createCollection("document_vector");
}
```

判断 Collection 是否存在时，只代表元数据存在，不代表该 Collection 已加载到内存。检索前还需要根据业务策略调用 `loadCollection` 或检查加载状态。

### 加载与释放 Collection

Milvus 在执行向量检索前通常需要将 Collection 加载到内存。Java SDK v2 的 `loadCollection` 用于加载 Collection，支持设置 `databaseName`、`collectionName`、副本数、同步等待、超时时间、加载字段和资源组等参数；`releaseCollection` 用于将 Collection 从内存中释放。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Management/loadCollection.md?utm_source=chatgpt.com))

加载 Collection：

```java
LoadCollectionReq request = LoadCollectionReq.builder()
        .databaseName(milvusProperties.getActualDatabaseName())
        .collectionName(collectionName)
        .async(Boolean.FALSE)
        .sync(Boolean.TRUE)
        .timeout(60000L)
        .build();

milvusClientV2.loadCollection(request);
```

释放 Collection：

```java
ReleaseCollectionReq request = ReleaseCollectionReq.builder()
        .databaseName(milvusProperties.getActualDatabaseName())
        .collectionName(collectionName)
        .async(Boolean.FALSE)
        .timeout(60000L)
        .build();

milvusClientV2.releaseCollection(request);
```

加载与释放建议：

| 操作                  | 建议                                    |
| --------------------- | --------------------------------------- |
| 应用启动后            | 可加载核心 Collection，降低首次检索延迟 |
| 新建 Collection 后    | 创建完成后按需加载                      |
| 批量导入期间          | 可以先写入数据，完成后再加载检索        |
| 长期不用的 Collection | 可释放以减少内存占用                    |
| 删除 Collection 前    | 如业务明确知道已加载，可先释放再删除    |

### 删除 Collection

删除 Collection 会移除该 Collection 的 Schema、索引和数据，属于高风险操作。Milvus Java SDK v2 的 `dropCollection` 接收 `DropCollectionReq`，用于删除指定 Collection，默认超时时间为 60000 毫秒。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Collections/dropCollection.md?utm_source=chatgpt.com))

删除 Collection 的封装方法如下：

```java
DropCollectionReq request = DropCollectionReq.builder()
        .collectionName(collectionName)
        .timeout(60000L)
        .build();

milvusClientV2.dropCollection(request);
```

生产环境建议对删除操作增加权限控制和二次确认，例如只允许管理员调用、限制接口来源、记录操作审计日志。对于线上业务数据，不建议直接删除 Collection，可优先通过新 Collection 灰度切换、数据备份和别名切换方式降低风险。

为了便于本地验证，可以提供 Collection 管理接口。

文件位置：`src/main/java/io/github/atengk/milvus/controller/MilvusCollectionController.java`

```java
package io.github.atengk.milvus.controller;

import io.github.atengk.milvus.service.MilvusCollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Milvus Collection 管理接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/milvus/collections")
public class MilvusCollectionController {

    private final MilvusCollectionService milvusCollectionService;

    /**
     * 创建默认 Collection
     */
    @PostMapping("/default")
    public void createDefaultCollection() {
        milvusCollectionService.createDefaultCollection();
    }

    /**
     * 创建指定 Collection
     *
     * @param collectionName Collection 名称
     */
    @PostMapping("/{collectionName}")
    public void createCollection(@PathVariable String collectionName) {
        milvusCollectionService.createCollection(collectionName);
    }

    /**
     * 查询 Collection 列表
     *
     * @return Collection 名称列表
     */
    @GetMapping
    public List<String> listCollections() {
        return milvusCollectionService.listCollections();
    }

    /**
     * 判断 Collection 是否存在
     *
     * @param collectionName Collection 名称
     * @return 是否存在
     */
    @GetMapping("/{collectionName}/exists")
    public Boolean hasCollection(@PathVariable String collectionName) {
        return milvusCollectionService.hasCollection(collectionName);
    }

    /**
     * 加载 Collection
     *
     * @param collectionName Collection 名称
     */
    @PostMapping("/{collectionName}/load")
    public void loadCollection(@PathVariable String collectionName) {
        milvusCollectionService.loadCollection(collectionName);
    }

    /**
     * 释放 Collection
     *
     * @param collectionName Collection 名称
     */
    @PostMapping("/{collectionName}/release")
    public void releaseCollection(@PathVariable String collectionName) {
        milvusCollectionService.releaseCollection(collectionName);
    }

    /**
     * 删除 Collection
     *
     * @param collectionName Collection 名称
     */
    @DeleteMapping("/{collectionName}")
    public void dropCollection(@PathVariable String collectionName) {
        milvusCollectionService.dropCollection(collectionName);
    }

}
```

接口验证示例：

```bash
# 创建默认 Collection
curl -X POST http://127.0.0.1:8080/api/milvus/collections/default

# 查询 Collection 列表
curl http://127.0.0.1:8080/api/milvus/collections

# 判断 Collection 是否存在
curl http://127.0.0.1:8080/api/milvus/collections/document_vector/exists

# 加载 Collection
curl -X POST http://127.0.0.1:8080/api/milvus/collections/document_vector/load

# 释放 Collection
curl -X POST http://127.0.0.1:8080/api/milvus/collections/document_vector/release

# 删除 Collection，生产环境需谨慎开放该接口
curl -X DELETE http://127.0.0.1:8080/api/milvus/collections/document_vector
```

## 向量数据写入

本章节用于封装向量数据写入能力，包括单条向量写入、批量向量写入以及 Java 业务对象到 Milvus 字段的映射。Milvus Java SDK v2 的 `insert` 方法接收 `InsertReq`，其中 `data` 是 `List<JsonObject>`，每个 `JsonObject` 表示一行待写入实体数据，方法返回 `InsertResp`。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Vector/insert.md?utm_source=chatgpt.com)) 该章节对应开发大纲中的「向量数据写入」部分。

推荐文件结构如下：

```text
src/main/java/io/github/atengk/milvus
├── model
│   ├── VectorDocument.java
│   └── VectorWriteResult.java
├── service
│   ├── MilvusVectorWriteService.java
│   └── impl
│       └── MilvusVectorWriteServiceImpl.java
└── controller
    └── MilvusVectorWriteController.java
```

### 单条向量写入

单条向量写入用于处理实时文档入库、FAQ 入库、商品特征入库等场景。写入前需要校验 Collection 是否存在、向量是否为空、向量维度是否与配置一致，然后将业务对象转换为 Milvus 要求的 `JsonObject`。

先定义向量文档对象，作为业务侧传入 Milvus 写入服务的统一数据模型。

文件位置：`src/main/java/io/github/atengk/milvus/model/VectorDocument.java`

```java
package io.github.atengk.milvus.model;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 向量文档模型
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorDocument {

    /**
     * 文档主键
     */
    @NotBlank(message = "文档主键不能为空")
    private String docId;

    /**
     * 文档分片 ID
     */
    private String chunkId;

    /**
     * 文档标题
     */
    private String title;

    /**
     * 文档内容
     */
    @NotBlank(message = "文档内容不能为空")
    private String content;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 租户标识
     */
    private String tenantId;

    /**
     * 数据来源
     */
    private String source;

    /**
     * 创建时间戳
     */
    private Long createTime;

    /**
     * 扩展元数据
     */
    private Map<String, Object> metadata;

    /**
     * 文本向量
     */
    @NotEmpty(message = "文本向量不能为空")
    private List<Float> embedding;

    /**
     * 校验基础字段和向量维度
     *
     * @param expectedDimension 期望向量维度
     */
    public void validate(Integer expectedDimension) {
        if (StrUtil.isBlank(docId)) {
            throw new IllegalArgumentException("文档主键不能为空");
        }
        if (StrUtil.isBlank(content)) {
            throw new IllegalArgumentException("文档内容不能为空");
        }
        if (CollUtil.isEmpty(embedding)) {
            throw new IllegalArgumentException("文本向量不能为空");
        }
        if (expectedDimension == null || expectedDimension <= 0) {
            throw new IllegalArgumentException("向量维度配置不合法");
        }
        if (embedding.size() != expectedDimension) {
            throw new IllegalArgumentException(
                    StrUtil.format("向量维度不一致，期望维度={}，实际维度={}", expectedDimension, embedding.size())
            );
        }
    }

}
```

定义写入结果对象，用于统一返回插入条数、Collection 名称和主键信息。

文件位置：`src/main/java/io/github/atengk/milvus/model/VectorWriteResult.java`

```java
package io.github.atengk.milvus.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 向量写入结果
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorWriteResult {

    /**
     * Collection 名称
     */
    private String collectionName;

    /**
     * 写入数量
     */
    private Integer insertCount;

    /**
     * 写入主键列表
     */
    private List<String> docIds;

}
```

定义向量写入服务接口。

文件位置：`src/main/java/io/github/atengk/milvus/service/MilvusVectorWriteService.java`

```java
package io.github.atengk.milvus.service;

import io.github.atengk.milvus.model.VectorDocument;
import io.github.atengk.milvus.model.VectorWriteResult;

import java.util.List;

/**
 * Milvus 向量写入服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface MilvusVectorWriteService {

    /**
     * 写入单条向量文档
     *
     * @param document 向量文档
     * @return 写入结果
     */
    VectorWriteResult insertOne(VectorDocument document);

    /**
     * 批量写入向量文档
     *
     * @param documents 向量文档列表
     * @return 写入结果
     */
    VectorWriteResult insertBatch(List<VectorDocument> documents);

}
```

下面的实现类完成单条写入、批量写入和字段映射。`InsertReq` 的 `data` 字段使用 `List<JsonObject>`，每个 `JsonObject` 中的字段名必须与 Collection Schema 中定义的字段名一致。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Vector/insert.md?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/milvus/service/impl/MilvusVectorWriteServiceImpl.java`

```java
package io.github.atengk.milvus.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.atengk.milvus.config.MilvusProperties;
import io.github.atengk.milvus.model.VectorDocument;
import io.github.atengk.milvus.model.VectorWriteResult;
import io.github.atengk.milvus.service.MilvusCollectionService;
import io.github.atengk.milvus.service.MilvusVectorWriteService;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.response.InsertResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Milvus 向量写入服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MilvusVectorWriteServiceImpl implements MilvusVectorWriteService {

    private final Gson gson = new Gson();

    private final MilvusClientV2 milvusClientV2;

    private final MilvusProperties milvusProperties;

    private final MilvusCollectionService milvusCollectionService;

    /**
     * 写入单条向量文档
     *
     * @param document 向量文档
     * @return 写入结果
     */
    @Override
    public VectorWriteResult insertOne(VectorDocument document) {
        if (document == null) {
            throw new IllegalArgumentException("向量文档不能为空");
        }

        return insertBatch(List.of(document));
    }

    /**
     * 批量写入向量文档
     *
     * @param documents 向量文档列表
     * @return 写入结果
     */
    @Override
    public VectorWriteResult insertBatch(List<VectorDocument> documents) {
        if (CollUtil.isEmpty(documents)) {
            throw new IllegalArgumentException("向量文档列表不能为空");
        }

        String collectionName = milvusProperties.getCollection().getName();

        if (!milvusCollectionService.hasCollection(collectionName)) {
            throw new IllegalStateException("Milvus Collection 不存在，请先创建 Collection：" + collectionName);
        }

        List<JsonObject> rows = documents.stream()
                .peek(this::validateDocument)
                .map(this::toMilvusRow)
                .toList();

        try {
            InsertReq request = InsertReq.builder()
                    .databaseName(milvusProperties.getActualDatabaseName())
                    .collectionName(collectionName)
                    .data(rows)
                    .build();

            InsertResp response = milvusClientV2.insert(request);

            List<String> docIds = documents.stream()
                    .map(VectorDocument::getDocId)
                    .toList();

            log.info("写入 Milvus 向量数据成功，collection={}，requestCount={}，insertCount={}",
                    collectionName,
                    rows.size(),
                    response.getInsertCnt());

            return VectorWriteResult.builder()
                    .collectionName(collectionName)
                    .insertCount(Math.toIntExact(response.getInsertCnt()))
                    .docIds(docIds)
                    .build();
        } catch (Exception e) {
            log.error("写入 Milvus 向量数据失败，collection={}，count={}", collectionName, documents.size(), e);
            throw new IllegalStateException("写入 Milvus 向量数据失败：" + collectionName, e);
        }
    }

    /**
     * 校验向量文档
     *
     * @param document 向量文档
     */
    private void validateDocument(VectorDocument document) {
        if (document == null) {
            throw new IllegalArgumentException("向量文档不能为空");
        }
        document.validate(milvusProperties.getCollection().getDimension());
    }

    /**
     * 将业务文档映射为 Milvus 行数据
     *
     * @param document 向量文档
     * @return Milvus 行数据
     */
    private JsonObject toMilvusRow(VectorDocument document) {
        MilvusProperties.CollectionConfig collectionConfig = milvusProperties.getCollection();

        JsonObject row = new JsonObject();
        row.addProperty(collectionConfig.getPrimaryFieldName(), document.getDocId());
        row.addProperty("chunk_id", StrUtil.blankToDefault(document.getChunkId(), document.getDocId()));
        row.addProperty("title", StrUtil.blankToDefault(document.getTitle(), ""));
        row.addProperty(collectionConfig.getContentFieldName(), document.getContent());
        row.addProperty("biz_type", StrUtil.blankToDefault(document.getBizType(), "default"));
        row.addProperty("tenant_id", StrUtil.blankToDefault(document.getTenantId(), "default"));
        row.addProperty("source", StrUtil.blankToDefault(document.getSource(), ""));
        row.addProperty("create_time", document.getCreateTime() == null ? System.currentTimeMillis() : document.getCreateTime());
        row.add(collectionConfig.getMetadataFieldName(), gson.toJsonTree(document.getMetadata()));
        row.add(collectionConfig.getVectorFieldName(), gson.toJsonTree(document.getEmbedding()));

        return row;
    }

}
```

单条写入调用示例：

```java
VectorDocument document = VectorDocument.builder()
        .docId("doc_10001")
        .chunkId("chunk_10001_001")
        .title("Milvus 集成说明")
        .content("Spring Boot 3 可以通过 Milvus Java SDK 连接 Milvus 并执行向量检索。")
        .bizType("knowledge")
        .tenantId("tenant_001")
        .source("milvus-guide.md")
        .createTime(System.currentTimeMillis())
        .metadata(Map.of("category", "vector", "version", "1.0"))
        .embedding(embedding)
        .build();

VectorWriteResult result = milvusVectorWriteService.insertOne(document);
```

### 批量向量写入

批量写入适用于文档切片入库、离线数据同步、知识库重建和商品特征批量导入等场景。批量写入时建议控制每批数据量，避免单次请求过大导致网络超时或服务端压力过高。

批量写入调用示例：

```java
List<VectorDocument> documents = List.of(
        VectorDocument.builder()
                .docId("doc_10001")
                .chunkId("chunk_10001_001")
                .title("Milvus 基础介绍")
                .content("Milvus 是面向向量数据管理和相似度检索的数据库。")
                .bizType("knowledge")
                .tenantId("tenant_001")
                .source("milvus-guide.md")
                .createTime(System.currentTimeMillis())
                .metadata(Map.of("page", 1))
                .embedding(embeddingOne)
                .build(),
        VectorDocument.builder()
                .docId("doc_10002")
                .chunkId("chunk_10001_002")
                .title("Milvus Collection 管理")
                .content("Collection 用于组织向量字段、标量字段和索引配置。")
                .bizType("knowledge")
                .tenantId("tenant_001")
                .source("milvus-guide.md")
                .createTime(System.currentTimeMillis())
                .metadata(Map.of("page", 2))
                .embedding(embeddingTwo)
                .build()
);

VectorWriteResult result = milvusVectorWriteService.insertBatch(documents);
```

批量写入建议：

| 项目            | 建议                                                         |
| --------------- | ------------------------------------------------------------ |
| 单批大小        | 根据向量维度和网络情况控制，常见可从 100 到 1000 条开始压测  |
| 失败处理        | 捕获异常后记录批次 ID、文档 ID、租户和来源                   |
| 幂等策略        | 使用业务主键作为 Milvus 主键，重建时先删除再写入或使用 upsert 方案 |
| 维度校验        | 每条数据写入前都校验向量长度                                 |
| Collection 校验 | 写入前确认 Collection 已创建                                 |
| 大规模导入      | 超大批量数据可评估 BulkWriter 或离线导入方式                 |

为了便于本地调试，可以增加写入 Controller。注意示例接口直接接收向量数组，仅适合开发测试；生产环境通常由 Embedding 服务生成向量，不建议前端直接提交高维向量。

文件位置：`src/main/java/io/github/atengk/milvus/controller/MilvusVectorWriteController.java`

```java
package io.github.atengk.milvus.controller;

import io.github.atengk.milvus.model.VectorDocument;
import io.github.atengk.milvus.model.VectorWriteResult;
import io.github.atengk.milvus.service.MilvusVectorWriteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Milvus 向量写入接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/milvus/vectors")
public class MilvusVectorWriteController {

    private final MilvusVectorWriteService milvusVectorWriteService;

    /**
     * 写入单条向量数据
     *
     * @param document 向量文档
     * @return 写入结果
     */
    @PostMapping
    public VectorWriteResult insertOne(@Valid @RequestBody VectorDocument document) {
        return milvusVectorWriteService.insertOne(document);
    }

    /**
     * 批量写入向量数据
     *
     * @param request 批量写入请求
     * @return 写入结果
     */
    @PostMapping("/batch")
    public VectorWriteResult insertBatch(@Valid @RequestBody BatchInsertRequest request) {
        return milvusVectorWriteService.insertBatch(request.getDocuments());
    }

    /**
     * 批量写入请求
     *
     * @author Ateng
     * @since 2026-05-07
     */
    @Data
    public static class BatchInsertRequest {

        /**
         * 向量文档列表
         */
        @NotEmpty(message = "向量文档列表不能为空")
        private List<@Valid VectorDocument> documents;

    }

}
```

单条写入请求示例：

```bash
curl -X POST http://127.0.0.1:8080/api/milvus/vectors \
  -H "Content-Type: application/json" \
  -d '{
    "docId": "doc_10001",
    "chunkId": "chunk_10001_001",
    "title": "Milvus 集成说明",
    "content": "Spring Boot 3 可以通过 Milvus Java SDK 连接 Milvus 并执行向量检索。",
    "bizType": "knowledge",
    "tenantId": "tenant_001",
    "source": "milvus-guide.md",
    "createTime": 1760000000000,
    "metadata": {
      "category": "vector",
      "version": "1.0"
    },
    "embedding": [0.012, 0.034, 0.056]
  }'
```

上述示例中的 `embedding` 只展示了 3 个元素，实际调用时必须传入与 `milvus.collection.dimension` 一致的完整向量。例如配置为 `1024` 维时，`embedding` 数组必须包含 1024 个浮点数。

批量写入请求示例：

```bash
curl -X POST http://127.0.0.1:8080/api/milvus/vectors/batch \
  -H "Content-Type: application/json" \
  -d '{
    "documents": [
      {
        "docId": "doc_10001",
        "chunkId": "chunk_10001_001",
        "title": "Milvus 基础介绍",
        "content": "Milvus 是面向向量数据管理和相似度检索的数据库。",
        "bizType": "knowledge",
        "tenantId": "tenant_001",
        "source": "milvus-guide.md",
        "metadata": {
          "page": 1
        },
        "embedding": [0.012, 0.034, 0.056]
      },
      {
        "docId": "doc_10002",
        "chunkId": "chunk_10001_002",
        "title": "Milvus Collection 管理",
        "content": "Collection 用于组织向量字段、标量字段和索引配置。",
        "bizType": "knowledge",
        "tenantId": "tenant_001",
        "source": "milvus-guide.md",
        "metadata": {
          "page": 2
        },
        "embedding": [0.021, 0.043, 0.065]
      }
    ]
  }'
```

### 文本与向量字段映射

文本与向量字段映射用于将业务对象中的字段转换为 Milvus Collection Schema 中的字段。映射时字段名必须严格一致，向量字段必须写入 `List<Float>`，JSON 字段建议使用 `Gson` 转换为 `JsonElement`。

推荐映射关系如下：

| Java 字段    | Milvus 字段   | 类型          | 说明        |
| ------------ | ------------- | ------------- | ----------- |
| `docId`      | `doc_id`      | `VarChar`     | 主键        |
| `chunkId`    | `chunk_id`    | `VarChar`     | 文档分片 ID |
| `title`      | `title`       | `VarChar`     | 文档标题    |
| `content`    | `content`     | `VarChar`     | 文档内容    |
| `bizType`    | `biz_type`    | `VarChar`     | 业务类型    |
| `tenantId`   | `tenant_id`   | `VarChar`     | 租户标识    |
| `source`     | `source`      | `VarChar`     | 数据来源    |
| `createTime` | `create_time` | `Int64`       | 创建时间戳  |
| `metadata`   | `metadata`    | `JSON`        | 扩展元数据  |
| `embedding`  | `embedding`   | `FloatVector` | 文本向量    |

映射代码核心片段如下：

```java
JsonObject row = new JsonObject();
row.addProperty("doc_id", document.getDocId());
row.addProperty("chunk_id", StrUtil.blankToDefault(document.getChunkId(), document.getDocId()));
row.addProperty("title", StrUtil.blankToDefault(document.getTitle(), ""));
row.addProperty("content", document.getContent());
row.addProperty("biz_type", StrUtil.blankToDefault(document.getBizType(), "default"));
row.addProperty("tenant_id", StrUtil.blankToDefault(document.getTenantId(), "default"));
row.addProperty("source", StrUtil.blankToDefault(document.getSource(), ""));
row.addProperty("create_time", document.getCreateTime() == null ? System.currentTimeMillis() : document.getCreateTime());
row.add("metadata", gson.toJsonTree(document.getMetadata()));
row.add("embedding", gson.toJsonTree(document.getEmbedding()));
```

字段映射注意事项：

| 注意项                 | 说明                                                   |
| ---------------------- | ------------------------------------------------------ |
| 字段名必须一致         | Java 映射字段必须与 Collection Schema 字段完全一致     |
| 主键不能为空           | 当前设计关闭 AutoID，因此写入时必须提供 `doc_id`       |
| 向量维度必须一致       | `embedding.size()` 必须等于配置中的 `dimension`        |
| 文本字段长度受限       | `VarChar` 字段长度不能超过 Schema 中设置的 `maxLength` |
| 高频过滤字段独立建字段 | 不要把 `tenant_id`、`biz_type` 全部塞入 JSON           |
| Metadata 只放扩展信息  | 核心检索过滤字段应保持独立                             |
| 时间字段使用时间戳     | 便于范围过滤和跨语言处理                               |

完成 Collection 管理和向量写入封装后，后续章节可以继续实现向量相似度搜索、条件过滤搜索、TopK 查询和查询结果解析。

## 向量检索实现

本章节用于实现 Milvus 向量相似度检索能力，包括基础向量搜索、标量字段条件过滤、TopK 控制和检索结果解析。Milvus Java SDK v2 的 `search` 方法通过 `SearchReq` 发起向量检索，请求中可配置 `collectionName`、`annsField`、`topK`、`filter`、`outputFields`、`data` 等参数，其中 `filter` 用于在向量检索前叠加标量字段过滤条件。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Vector/search.md?utm_source=chatgpt.com)) 该章节对应开发大纲中的「向量检索实现」部分。

推荐文件结构如下：

```text
src/main/java/io/github/atengk/milvus
├── model
│   ├── VectorSearchRequest.java
│   └── VectorSearchResult.java
├── service
│   ├── MilvusVectorSearchService.java
│   └── impl
│       └── MilvusVectorSearchServiceImpl.java
└── controller
    └── MilvusVectorSearchController.java
```

### 基础向量相似度搜索

基础向量相似度搜索用于根据查询向量在 Collection 中召回最相似的实体数据。调用前需要确保 Collection 已创建、已加载，并且查询向量维度与 Collection 中的向量字段维度一致。

先定义检索请求对象，用于承载查询向量、TopK、租户、业务类型和扩展过滤表达式。

文件位置：`src/main/java/io/github/atengk/milvus/model/VectorSearchRequest.java`

```java
package io.github.atengk.milvus.model;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 向量检索请求
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorSearchRequest {

    /**
     * 查询向量
     */
    @NotEmpty(message = "查询向量不能为空")
    private List<Float> embedding;

    /**
     * 返回结果数量
     */
    @Min(value = 1, message = "TopK 不能小于 1")
    @Max(value = 100, message = "TopK 不能大于 100")
    private Integer topK = 10;

    /**
     * 租户标识
     */
    private String tenantId;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 扩展过滤表达式，仅建议内部可信调用使用
     */
    private String extraFilter;

    /**
     * 输出字段列表
     */
    private List<String> outputFields;

    /**
     * 校验查询向量维度
     *
     * @param expectedDimension 期望维度
     */
    public void validateDimension(Integer expectedDimension) {
        if (CollUtil.isEmpty(embedding)) {
            throw new IllegalArgumentException("查询向量不能为空");
        }
        if (expectedDimension == null || expectedDimension <= 0) {
            throw new IllegalArgumentException("向量维度配置不合法");
        }
        if (embedding.size() != expectedDimension) {
            throw new IllegalArgumentException(
                    StrUtil.format("查询向量维度不一致，期望维度={}，实际维度={}", expectedDimension, embedding.size())
            );
        }
        if (topK == null) {
            topK = 10;
        }
    }

}
```

检索结果对象用于屏蔽 Milvus SDK 的原始返回结构，便于接口层和业务层直接使用。

文件位置：`src/main/java/io/github/atengk/milvus/model/VectorSearchResult.java`

```java
package io.github.atengk.milvus.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 向量检索结果
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorSearchResult {

    /**
     * Milvus 返回的主键
     */
    private Object id;

    /**
     * 文档主键
     */
    private String docId;

    /**
     * 文档分片 ID
     */
    private String chunkId;

    /**
     * 文档标题
     */
    private String title;

    /**
     * 文档内容
     */
    private String content;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 租户标识
     */
    private String tenantId;

    /**
     * 数据来源
     */
    private String source;

    /**
     * 创建时间戳
     */
    private Long createTime;

    /**
     * 相似度分数
     */
    private Float score;

    /**
     * 扩展元数据
     */
    private Map<String, Object> metadata;

}
```

定义向量检索服务接口。

文件位置：`src/main/java/io/github/atengk/milvus/service/MilvusVectorSearchService.java`

```java
package io.github.atengk.milvus.service;

import io.github.atengk.milvus.model.VectorSearchRequest;
import io.github.atengk.milvus.model.VectorSearchResult;

import java.util.List;

/**
 * Milvus 向量检索服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface MilvusVectorSearchService {

    /**
     * 基础向量相似度检索
     *
     * @param request 检索请求
     * @return 检索结果
     */
    List<VectorSearchResult> search(VectorSearchRequest request);

    /**
     * 带条件过滤的向量检索
     *
     * @param request 检索请求
     * @return 检索结果
     */
    List<VectorSearchResult> searchWithFilter(VectorSearchRequest request);

}
```

下面的实现类完成向量维度校验、Collection 校验、过滤表达式构建、`SearchReq` 构造和结果解析。`FloatVec` 适用于 `FloatVector` 类型字段，Milvus Java SDK 的向量搜索结果通过 `SearchResp#getSearchResults()` 返回，每个 `SearchResult` 包含主键、分数和输出字段实体数据。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Vector/SearchIteratorV2.md?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/milvus/service/impl/MilvusVectorSearchServiceImpl.java`

```java
package io.github.atengk.milvus.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.milvus.config.MilvusProperties;
import io.github.atengk.milvus.model.VectorSearchRequest;
import io.github.atengk.milvus.model.VectorSearchResult;
import io.github.atengk.milvus.service.MilvusCollectionService;
import io.github.atengk.milvus.service.MilvusVectorSearchService;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Milvus 向量检索服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MilvusVectorSearchServiceImpl implements MilvusVectorSearchService {

    private static final Integer DEFAULT_ROUND_DECIMAL = 4;

    private final MilvusClientV2 milvusClientV2;

    private final MilvusProperties milvusProperties;

    private final MilvusCollectionService milvusCollectionService;

    /**
     * 基础向量相似度检索
     *
     * @param request 检索请求
     * @return 检索结果
     */
    @Override
    public List<VectorSearchResult> search(VectorSearchRequest request) {
        request.setExtraFilter(null);
        request.setTenantId(null);
        request.setBizType(null);
        return doSearch(request);
    }

    /**
     * 带条件过滤的向量检索
     *
     * @param request 检索请求
     * @return 检索结果
     */
    @Override
    public List<VectorSearchResult> searchWithFilter(VectorSearchRequest request) {
        return doSearch(request);
    }

    /**
     * 执行向量检索
     *
     * @param request 检索请求
     * @return 检索结果
     */
    private List<VectorSearchResult> doSearch(VectorSearchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("向量检索请求不能为空");
        }

        MilvusProperties.CollectionConfig collectionConfig = milvusProperties.getCollection();
        request.validateDimension(collectionConfig.getDimension());

        String collectionName = collectionConfig.getName();
        if (!milvusCollectionService.hasCollection(collectionName)) {
            throw new IllegalStateException("Milvus Collection 不存在，请先创建 Collection：" + collectionName);
        }

        String filter = buildFilter(request);
        List<String> outputFields = buildOutputFields(request);

        try {
            SearchReq searchReq = SearchReq.builder()
                    .databaseName(milvusProperties.getActualDatabaseName())
                    .collectionName(collectionName)
                    .annsField(collectionConfig.getVectorFieldName())
                    .topK(request.getTopK())
                    .filter(filter)
                    .outputFields(outputFields)
                    .data(Collections.singletonList(new FloatVec(request.getEmbedding())))
                    .roundDecimal(DEFAULT_ROUND_DECIMAL)
                    .build();

            SearchResp searchResp = milvusClientV2.search(searchReq);
            List<VectorSearchResult> results = parseSearchResult(searchResp);

            log.info("Milvus 向量检索完成，collection={}，topK={}，filter={}，resultCount={}",
                    collectionName,
                    request.getTopK(),
                    StrUtil.blankToDefault(filter, "无"),
                    results.size());

            return results;
        } catch (Exception e) {
            log.error("Milvus 向量检索失败，collection={}，filter={}", collectionName, filter, e);
            throw new IllegalStateException("Milvus 向量检索失败：" + collectionName, e);
        }
    }

    /**
     * 构建输出字段
     *
     * @param request 检索请求
     * @return 输出字段列表
     */
    private List<String> buildOutputFields(VectorSearchRequest request) {
        if (CollUtil.isNotEmpty(request.getOutputFields())) {
            return request.getOutputFields();
        }

        MilvusProperties.CollectionConfig collectionConfig = milvusProperties.getCollection();

        return List.of(
                collectionConfig.getPrimaryFieldName(),
                "chunk_id",
                "title",
                collectionConfig.getContentFieldName(),
                "biz_type",
                "tenant_id",
                "source",
                "create_time",
                collectionConfig.getMetadataFieldName()
        );
    }

    /**
     * 构建过滤表达式
     *
     * @param request 检索请求
     * @return 过滤表达式
     */
    private String buildFilter(VectorSearchRequest request) {
        List<String> filters = new ArrayList<>();

        if (StrUtil.isNotBlank(request.getTenantId())) {
            filters.add(StrUtil.format("tenant_id == \"{}\"", escapeFilterValue(request.getTenantId())));
        }

        if (StrUtil.isNotBlank(request.getBizType())) {
            filters.add(StrUtil.format("biz_type == \"{}\"", escapeFilterValue(request.getBizType())));
        }

        if (StrUtil.isNotBlank(request.getExtraFilter())) {
            filters.add(StrUtil.format("({})", request.getExtraFilter()));
        }

        if (CollUtil.isEmpty(filters)) {
            return null;
        }

        return CollUtil.join(filters, " AND ");
    }

    /**
     * 转义过滤值
     *
     * @param value 原始值
     * @return 转义后的值
     */
    private String escapeFilterValue(String value) {
        return StrUtil.replace(value, "\"", "\\\"");
    }

    /**
     * 解析检索结果
     *
     * @param searchResp Milvus 检索响应
     * @return 检索结果
     */
    private List<VectorSearchResult> parseSearchResult(SearchResp searchResp) {
        if (searchResp == null || CollUtil.isEmpty(searchResp.getSearchResults())) {
            return Collections.emptyList();
        }

        List<VectorSearchResult> results = new ArrayList<>();

        for (List<SearchResp.SearchResult> oneQueryResults : searchResp.getSearchResults()) {
            if (CollUtil.isEmpty(oneQueryResults)) {
                continue;
            }

            for (SearchResp.SearchResult item : oneQueryResults) {
                results.add(toVectorSearchResult(item));
            }
        }

        return results;
    }

    /**
     * 转换单条检索结果
     *
     * @param item Milvus 检索结果
     * @return 业务检索结果
     */
    @SuppressWarnings("unchecked")
    private VectorSearchResult toVectorSearchResult(SearchResp.SearchResult item) {
        MilvusProperties.CollectionConfig collectionConfig = milvusProperties.getCollection();
        Map<String, Object> entity = item.getEntity();

        if (MapUtil.isEmpty(entity)) {
            return VectorSearchResult.builder()
                    .id(item.getId())
                    .score(item.getScore())
                    .build();
        }

        Object metadataValue = entity.get(collectionConfig.getMetadataFieldName());
        Map<String, Object> metadata = metadataValue instanceof Map<?, ?> mapValue
                ? (Map<String, Object>) mapValue
                : Collections.emptyMap();

        return VectorSearchResult.builder()
                .id(item.getId())
                .docId(Convert.toStr(entity.get(collectionConfig.getPrimaryFieldName())))
                .chunkId(Convert.toStr(entity.get("chunk_id")))
                .title(Convert.toStr(entity.get("title")))
                .content(Convert.toStr(entity.get(collectionConfig.getContentFieldName())))
                .bizType(Convert.toStr(entity.get("biz_type")))
                .tenantId(Convert.toStr(entity.get("tenant_id")))
                .source(Convert.toStr(entity.get("source")))
                .createTime(Convert.toLong(entity.get("create_time")))
                .metadata(metadata)
                .score(item.getScore())
                .build();
    }

}
```

### 条件过滤搜索

条件过滤搜索用于在向量相似度召回前先通过标量字段缩小候选范围。Milvus 过滤表达式支持比较运算符、范围条件、算术运算符、逻辑运算符，以及 `IS NULL`、`IS NOT NULL` 等条件；这些过滤表达式可应用在 `query`、`search` 和 `delete` 请求中。([Milvus](https://milvus.io/docs/ja/boolean.md?utm_source=chatgpt.com))

常见过滤表达式如下：

| 场景           | 表达式示例                                              |
| -------------- | ------------------------------------------------------- |
| 按租户过滤     | `tenant_id == "tenant_001"`                             |
| 按业务类型过滤 | `biz_type == "knowledge"`                               |
| 按时间过滤     | `create_time >= 1760000000000`                          |
| 按多个类型过滤 | `biz_type IN ["knowledge", "faq"]`                      |
| 组合过滤       | `tenant_id == "tenant_001" AND biz_type == "knowledge"` |
| 模糊匹配       | `source LIKE "milvus%"`                                 |

接口层建议优先暴露明确字段，例如 `tenantId`、`bizType`，不要直接把 `extraFilter` 暴露给不可信前端。`extraFilter` 更适合内部调试、后台管理或受控任务使用。

文件位置：`src/main/java/io/github/atengk/milvus/controller/MilvusVectorSearchController.java`

```java
package io.github.atengk.milvus.controller;

import io.github.atengk.milvus.model.VectorSearchRequest;
import io.github.atengk.milvus.model.VectorSearchResult;
import io.github.atengk.milvus.service.MilvusVectorSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Milvus 向量检索接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/milvus/search")
public class MilvusVectorSearchController {

    private final MilvusVectorSearchService milvusVectorSearchService;

    /**
     * 基础向量相似度检索
     *
     * @param request 检索请求
     * @return 检索结果
     */
    @PostMapping
    public List<VectorSearchResult> search(@Valid @RequestBody VectorSearchRequest request) {
        return milvusVectorSearchService.search(request);
    }

    /**
     * 条件过滤向量检索
     *
     * @param request 检索请求
     * @return 检索结果
     */
    @PostMapping("/filter")
    public List<VectorSearchResult> searchWithFilter(@Valid @RequestBody VectorSearchRequest request) {
        return milvusVectorSearchService.searchWithFilter(request);
    }

}
```

基础检索请求示例：

```bash
curl -X POST http://127.0.0.1:8080/api/milvus/search \
  -H "Content-Type: application/json" \
  -d '{
    "topK": 5,
    "embedding": [0.012, 0.034, 0.056]
  }'
```

条件过滤检索请求示例：

```bash
curl -X POST http://127.0.0.1:8080/api/milvus/search/filter \
  -H "Content-Type: application/json" \
  -d '{
    "topK": 5,
    "tenantId": "tenant_001",
    "bizType": "knowledge",
    "extraFilter": "create_time >= 1760000000000",
    "embedding": [0.012, 0.034, 0.056]
  }'
```

示例中的 `embedding` 只展示了 3 个元素，实际请求必须传入完整向量。例如 Collection 配置为 `1024` 维时，数组必须包含 1024 个 `Float` 值。

### TopK 查询

TopK 表示每个查询向量最多返回多少条相似结果。TopK 越大，召回候选越多，但接口耗时、网络返回体和后续重排成本也会增加。Milvus `SearchReq` 支持通过 `topK` 控制返回结果数量，也支持 `offset`、`limit`、`roundDecimal`、`searchParams` 等高级参数。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Vector/search.md?utm_source=chatgpt.com))

推荐配置策略如下：

| 场景         | 推荐 TopK | 说明                             |
| ------------ | --------- | -------------------------------- |
| 普通语义搜索 | 5 - 20    | 直接返回给前端展示               |
| RAG 文档召回 | 5 - 10    | 控制上下文长度，避免提示词过长   |
| 召回后重排   | 30 - 100  | 先召回更多候选，再由重排模型筛选 |
| 调试验证     | 3 - 5     | 便于观察结果是否符合预期         |
| 批量离线评估 | 50 - 100  | 用于评估召回率和覆盖率           |

业务侧建议限制 TopK 最大值，例如上面的 `VectorSearchRequest` 中限制为 `100`。如果需要更大规模结果遍历，不建议直接把 TopK 设置得非常大，可以评估 `searchIteratorV2`，它用于分批迭代大规模搜索结果。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Vector/SearchIteratorV2.md?utm_source=chatgpt.com))

### 查询结果解析

Milvus 检索结果通常包含三类信息：主键、相似度分数和输出字段。对于文本语义检索，接口层通常只需要返回 `docId`、`chunkId`、`title`、`content`、`source`、`metadata` 和 `score`，不建议把完整向量字段返回给前端或大模型流程。

结果字段建议如下：

| 字段         | 说明                |
| ------------ | ------------------- |
| `id`         | Milvus 返回的主键值 |
| `docId`      | 业务文档主键        |
| `chunkId`    | 文档分片 ID         |
| `title`      | 文档标题            |
| `content`    | 召回文本            |
| `bizType`    | 业务类型            |
| `tenantId`   | 租户标识            |
| `source`     | 数据来源            |
| `createTime` | 创建时间            |
| `score`      | 相似度分数          |
| `metadata`   | 扩展元数据          |

返回示例：

```json
[
  {
    "id": "doc_10001",
    "docId": "doc_10001",
    "chunkId": "chunk_10001_001",
    "title": "Milvus 集成说明",
    "content": "Spring Boot 3 可以通过 Milvus Java SDK 连接 Milvus 并执行向量检索。",
    "bizType": "knowledge",
    "tenantId": "tenant_001",
    "source": "milvus-guide.md",
    "createTime": 1760000000000,
    "score": 0.9234,
    "metadata": {
      "category": "vector",
      "version": "1.0"
    }
  }
]
```

结果解析注意事项：

| 注意项                     | 说明                                                       |
| -------------------------- | ---------------------------------------------------------- |
| 不默认返回向量字段         | 向量字段体积大，通常没有业务展示价值                       |
| 分数方向要结合度量方式解释 | `COSINE`、`IP` 通常分数越大越相似，`L2` 通常距离越小越相似 |
| 输出字段要显式配置         | 减少网络传输和 JSON 解析成本                               |
| 空结果要正常返回空数组     | 不应把未命中当作系统异常                                   |
| RAG 场景需二次组装         | 检索结果应按分数、来源、长度等规则组装上下文               |

## 数据维护

本章节用于实现 Milvus 数据维护能力，包括根据主键查询、根据主键删除和批量删除。Milvus Java SDK v2 提供 `get` 方法按主键获取实体，提供 `query` 方法按过滤表达式查询实体，提供 `delete` 方法按主键或过滤表达式删除实体。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Vector/get.md?utm_source=chatgpt.com)) 该章节对应开发大纲中的「数据维护」部分。

推荐文件结构如下：

```text
src/main/java/io/github/atengk/milvus
├── service
│   ├── MilvusVectorMaintenanceService.java
│   └── impl
│       └── MilvusVectorMaintenanceServiceImpl.java
└── controller
    └── MilvusVectorMaintenanceController.java
```

### 根据主键查询

根据主键查询用于精确读取某条或多条向量实体，常见于写入验证、删除前确认、业务详情回显和问题排查。Milvus `get` 方法接收 `GetReq`，其中 `ids` 表示主键列表，`outputFields` 表示需要返回的字段。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Vector/get.md?utm_source=chatgpt.com))

定义数据维护服务接口。

文件位置：`src/main/java/io/github/atengk/milvus/service/MilvusVectorMaintenanceService.java`

```java
package io.github.atengk.milvus.service;

import io.github.atengk.milvus.model.VectorSearchResult;

import java.util.List;

/**
 * Milvus 向量数据维护服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface MilvusVectorMaintenanceService {

    /**
     * 根据主键查询单条数据
     *
     * @param docId 文档主键
     * @return 查询结果
     */
    VectorSearchResult getById(String docId);

    /**
     * 根据主键批量查询数据
     *
     * @param docIds 文档主键列表
     * @return 查询结果
     */
    List<VectorSearchResult> getByIds(List<String> docIds);

    /**
     * 根据主键删除单条数据
     *
     * @param docId 文档主键
     * @return 删除数量
     */
    Long deleteById(String docId);

    /**
     * 根据主键批量删除数据
     *
     * @param docIds 文档主键列表
     * @return 删除数量
     */
    Long deleteBatchByIds(List<String> docIds);

    /**
     * 根据过滤表达式删除数据
     *
     * @param filter 过滤表达式
     * @return 删除数量
     */
    Long deleteByFilter(String filter);

}
```

下面的实现类封装了 `get` 和 `delete` 操作，并复用 `VectorSearchResult` 作为维护查询结果对象。维护类中的 `score` 字段在主键查询场景下为空，因为该场景不是向量相似度搜索。

文件位置：`src/main/java/io/github/atengk/milvus/service/impl/MilvusVectorMaintenanceServiceImpl.java`

```java
package io.github.atengk.milvus.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.milvus.config.MilvusProperties;
import io.github.atengk.milvus.model.VectorSearchResult;
import io.github.atengk.milvus.service.MilvusCollectionService;
import io.github.atengk.milvus.service.MilvusVectorMaintenanceService;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.GetReq;
import io.milvus.v2.service.vector.response.DeleteResp;
import io.milvus.v2.service.vector.response.GetResp;
import io.milvus.v2.service.vector.response.QueryResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Milvus 向量数据维护服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MilvusVectorMaintenanceServiceImpl implements MilvusVectorMaintenanceService {

    private final MilvusClientV2 milvusClientV2;

    private final MilvusProperties milvusProperties;

    private final MilvusCollectionService milvusCollectionService;

    /**
     * 根据主键查询单条数据
     *
     * @param docId 文档主键
     * @return 查询结果
     */
    @Override
    public VectorSearchResult getById(String docId) {
        List<VectorSearchResult> results = getByIds(List.of(docId));
        return CollUtil.isEmpty(results) ? null : results.get(0);
    }

    /**
     * 根据主键批量查询数据
     *
     * @param docIds 文档主键列表
     * @return 查询结果
     */
    @Override
    public List<VectorSearchResult> getByIds(List<String> docIds) {
        List<String> actualDocIds = checkDocIds(docIds);
        MilvusProperties.CollectionConfig collectionConfig = milvusProperties.getCollection();
        String collectionName = collectionConfig.getName();

        checkCollectionExists(collectionName);

        try {
            GetReq request = GetReq.builder()
                    .databaseName(milvusProperties.getActualDatabaseName())
                    .collectionName(collectionName)
                    .ids(actualDocIds.stream().map(id -> (Object) id).toList())
                    .outputFields(buildOutputFields())
                    .build();

            GetResp response = milvusClientV2.get(request);
            List<QueryResp.QueryResult> getResults = response.getGetResults();

            if (CollUtil.isEmpty(getResults)) {
                log.info("根据主键查询 Milvus 数据未命中，collection={}，ids={}", collectionName, actualDocIds);
                return Collections.emptyList();
            }

            List<VectorSearchResult> results = getResults.stream()
                    .map(QueryResp.QueryResult::getEntity)
                    .map(this::toVectorSearchResult)
                    .toList();

            log.info("根据主键查询 Milvus 数据成功，collection={}，requestCount={}，resultCount={}",
                    collectionName,
                    actualDocIds.size(),
                    results.size());

            return results;
        } catch (Exception e) {
            log.error("根据主键查询 Milvus 数据失败，collection={}，ids={}", collectionName, actualDocIds, e);
            throw new IllegalStateException("根据主键查询 Milvus 数据失败：" + collectionName, e);
        }
    }

    /**
     * 根据主键删除单条数据
     *
     * @param docId 文档主键
     * @return 删除数量
     */
    @Override
    public Long deleteById(String docId) {
        return deleteBatchByIds(List.of(docId));
    }

    /**
     * 根据主键批量删除数据
     *
     * @param docIds 文档主键列表
     * @return 删除数量
     */
    @Override
    public Long deleteBatchByIds(List<String> docIds) {
        List<String> actualDocIds = checkDocIds(docIds);
        String collectionName = milvusProperties.getCollection().getName();

        checkCollectionExists(collectionName);

        try {
            DeleteReq request = DeleteReq.builder()
                    .databaseName(milvusProperties.getActualDatabaseName())
                    .collectionName(collectionName)
                    .ids(actualDocIds.stream().map(id -> (Object) id).toList())
                    .build();

            DeleteResp response = milvusClientV2.delete(request);

            log.warn("根据主键删除 Milvus 数据成功，collection={}，requestCount={}，deleteCount={}",
                    collectionName,
                    actualDocIds.size(),
                    response.getDeleteCnt());

            return response.getDeleteCnt();
        } catch (Exception e) {
            log.error("根据主键删除 Milvus 数据失败，collection={}，ids={}", collectionName, actualDocIds, e);
            throw new IllegalStateException("根据主键删除 Milvus 数据失败：" + collectionName, e);
        }
    }

    /**
     * 根据过滤表达式删除数据
     *
     * @param filter 过滤表达式
     * @return 删除数量
     */
    @Override
    public Long deleteByFilter(String filter) {
        if (StrUtil.isBlank(filter)) {
            throw new IllegalArgumentException("删除过滤表达式不能为空");
        }

        String collectionName = milvusProperties.getCollection().getName();
        checkCollectionExists(collectionName);

        try {
            DeleteReq request = DeleteReq.builder()
                    .databaseName(milvusProperties.getActualDatabaseName())
                    .collectionName(collectionName)
                    .filter(filter)
                    .build();

            DeleteResp response = milvusClientV2.delete(request);

            log.warn("根据过滤表达式删除 Milvus 数据成功，collection={}，filter={}，deleteCount={}",
                    collectionName,
                    filter,
                    response.getDeleteCnt());

            return response.getDeleteCnt();
        } catch (Exception e) {
            log.error("根据过滤表达式删除 Milvus 数据失败，collection={}，filter={}", collectionName, filter, e);
            throw new IllegalStateException("根据过滤表达式删除 Milvus 数据失败：" + collectionName, e);
        }
    }

    /**
     * 构建输出字段
     *
     * @return 输出字段列表
     */
    private List<String> buildOutputFields() {
        MilvusProperties.CollectionConfig collectionConfig = milvusProperties.getCollection();

        return List.of(
                collectionConfig.getPrimaryFieldName(),
                "chunk_id",
                "title",
                collectionConfig.getContentFieldName(),
                "biz_type",
                "tenant_id",
                "source",
                "create_time",
                collectionConfig.getMetadataFieldName()
        );
    }

    /**
     * 校验文档主键列表
     *
     * @param docIds 文档主键列表
     * @return 合法文档主键列表
     */
    private List<String> checkDocIds(List<String> docIds) {
        if (CollUtil.isEmpty(docIds)) {
            throw new IllegalArgumentException("文档主键列表不能为空");
        }

        List<String> actualDocIds = new ArrayList<>();
        for (String docId : docIds) {
            if (StrUtil.isNotBlank(docId)) {
                actualDocIds.add(StrUtil.trim(docId));
            }
        }

        if (CollUtil.isEmpty(actualDocIds)) {
            throw new IllegalArgumentException("文档主键列表不能为空");
        }

        return actualDocIds;
    }

    /**
     * 校验 Collection 是否存在
     *
     * @param collectionName Collection 名称
     */
    private void checkCollectionExists(String collectionName) {
        if (!milvusCollectionService.hasCollection(collectionName)) {
            throw new IllegalStateException("Milvus Collection 不存在：" + collectionName);
        }
    }

    /**
     * 转换查询结果
     *
     * @param entity Milvus 实体字段
     * @return 查询结果
     */
    @SuppressWarnings("unchecked")
    private VectorSearchResult toVectorSearchResult(Map<String, Object> entity) {
        if (MapUtil.isEmpty(entity)) {
            return null;
        }

        MilvusProperties.CollectionConfig collectionConfig = milvusProperties.getCollection();

        Object metadataValue = entity.get(collectionConfig.getMetadataFieldName());
        Map<String, Object> metadata = metadataValue instanceof Map<?, ?> mapValue
                ? (Map<String, Object>) mapValue
                : Collections.emptyMap();

        return VectorSearchResult.builder()
                .id(entity.get(collectionConfig.getPrimaryFieldName()))
                .docId(Convert.toStr(entity.get(collectionConfig.getPrimaryFieldName())))
                .chunkId(Convert.toStr(entity.get("chunk_id")))
                .title(Convert.toStr(entity.get("title")))
                .content(Convert.toStr(entity.get(collectionConfig.getContentFieldName())))
                .bizType(Convert.toStr(entity.get("biz_type")))
                .tenantId(Convert.toStr(entity.get("tenant_id")))
                .source(Convert.toStr(entity.get("source")))
                .createTime(Convert.toLong(entity.get("create_time")))
                .metadata(metadata)
                .build();
    }

}
```

### 根据主键删除

根据主键删除用于删除指定文档或分片。Milvus `delete` 方法既支持 `ids` 主键列表，也支持 `filter` 布尔表达式；返回的 `DeleteResp` 中包含删除数量。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Vector/delete.md?utm_source=chatgpt.com))

单条删除逻辑：

```java
Long deleteCount = milvusVectorMaintenanceService.deleteById("doc_10001");
```

删除前建议根据业务需要先查询确认：

```java
VectorSearchResult record = milvusVectorMaintenanceService.getById("doc_10001");
if (record != null) {
    Long deleteCount = milvusVectorMaintenanceService.deleteById("doc_10001");
}
```

主键删除建议：

| 项目       | 建议                                     |
| ---------- | ---------------------------------------- |
| 删除粒度   | 如果以分片为实体，主键应对应分片 ID      |
| 删除前校验 | 重要业务可先查询确认再删除               |
| 删除日志   | 记录租户、业务类型、主键和删除数量       |
| 删除权限   | 删除接口不建议直接暴露给普通前端用户     |
| 数据一致性 | 删除后短时间内结果可见性受一致性策略影响 |

### 批量删除数据

批量删除适用于文档重建、知识库清理、租户数据清理和业务数据同步。批量删除优先使用主键列表；如果要按条件删除，应确保过滤表达式足够精确，避免误删。

提供数据维护 Controller，便于本地验证主键查询和删除操作。

文件位置：`src/main/java/io/github/atengk/milvus/controller/MilvusVectorMaintenanceController.java`

```java
package io.github.atengk.milvus.controller;

import io.github.atengk.milvus.model.VectorSearchResult;
import io.github.atengk.milvus.service.MilvusVectorMaintenanceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Milvus 向量数据维护接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/milvus/data")
public class MilvusVectorMaintenanceController {

    private final MilvusVectorMaintenanceService milvusVectorMaintenanceService;

    /**
     * 根据主键查询单条数据
     *
     * @param docId 文档主键
     * @return 查询结果
     */
    @GetMapping("/{docId}")
    public VectorSearchResult getById(@PathVariable String docId) {
        return milvusVectorMaintenanceService.getById(docId);
    }

    /**
     * 根据主键批量查询数据
     *
     * @param request 批量主键请求
     * @return 查询结果
     */
    @PostMapping("/query-batch")
    public List<VectorSearchResult> getByIds(@Valid @RequestBody BatchIdsRequest request) {
        return milvusVectorMaintenanceService.getByIds(request.getDocIds());
    }

    /**
     * 根据主键删除单条数据
     *
     * @param docId 文档主键
     * @return 删除数量
     */
    @DeleteMapping("/{docId}")
    public Long deleteById(@PathVariable String docId) {
        return milvusVectorMaintenanceService.deleteById(docId);
    }

    /**
     * 根据主键批量删除数据
     *
     * @param request 批量主键请求
     * @return 删除数量
     */
    @PostMapping("/delete-batch")
    public Long deleteBatchByIds(@Valid @RequestBody BatchIdsRequest request) {
        return milvusVectorMaintenanceService.deleteBatchByIds(request.getDocIds());
    }

    /**
     * 根据过滤表达式删除数据
     *
     * @param request 过滤删除请求
     * @return 删除数量
     */
    @PostMapping("/delete-by-filter")
    public Long deleteByFilter(@Valid @RequestBody DeleteByFilterRequest request) {
        return milvusVectorMaintenanceService.deleteByFilter(request.getFilter());
    }

    /**
     * 批量主键请求
     *
     * @author Ateng
     * @since 2026-05-07
     */
    @Data
    public static class BatchIdsRequest {

        /**
         * 文档主键列表
         */
        @NotEmpty(message = "文档主键列表不能为空")
        private List<@NotBlank(message = "文档主键不能为空") String> docIds;

    }

    /**
     * 过滤删除请求
     *
     * @author Ateng
     * @since 2026-05-07
     */
    @Data
    public static class DeleteByFilterRequest {

        /**
         * 删除过滤表达式
         */
        @NotBlank(message = "删除过滤表达式不能为空")
        private String filter;

    }

}
```

根据主键查询：

```bash
curl http://127.0.0.1:8080/api/milvus/data/doc_10001
```

批量查询：

```bash
curl -X POST http://127.0.0.1:8080/api/milvus/data/query-batch \
  -H "Content-Type: application/json" \
  -d '{
    "docIds": ["doc_10001", "doc_10002"]
  }'
```

根据主键删除：

```bash
curl -X DELETE http://127.0.0.1:8080/api/milvus/data/doc_10001
```

批量删除：

```bash
curl -X POST http://127.0.0.1:8080/api/milvus/data/delete-batch \
  -H "Content-Type: application/json" \
  -d '{
    "docIds": ["doc_10001", "doc_10002"]
  }'
```

根据过滤表达式删除：

```bash
curl -X POST http://127.0.0.1:8080/api/milvus/data/delete-by-filter \
  -H "Content-Type: application/json" \
  -d '{
    "filter": "tenant_id == \"tenant_001\" AND biz_type == \"knowledge\""
  }'
```

批量删除注意事项：

| 注意项                     | 说明                                                 |
| -------------------------- | ---------------------------------------------------- |
| 优先按主键删除             | 主键删除范围更明确，误删风险更低                     |
| 条件删除必须审计           | 过滤表达式删除需要记录操作人、表达式、时间和删除数量 |
| 租户数据清理必须带租户条件 | 例如 `tenant_id == "tenant_001"`                     |
| 不建议直接暴露任意 filter  | 外部接口应转换为白名单字段条件                       |
| 删除后可查询验证           | 删除完成后可使用 `getByIds` 或检索接口确认结果       |
| 大批量删除分批执行         | 避免单次请求过大导致超时或难以追踪失败范围           |

完成向量检索和数据维护后，后续章节可以继续补充接口设计、业务集成、测试验证和常见问题处理。

## 接口设计

本章节用于定义 Spring Boot 对外暴露的 Milvus 能力接口，包括向量写入、向量搜索、向量删除和 Collection 管理。接口层不直接拼装 Milvus SDK 请求对象，而是调用前面已经封装好的 `MilvusVectorWriteService`、`MilvusVectorSearchService`、`MilvusVectorMaintenanceService` 和 `MilvusCollectionService`。Milvus Java SDK v2 的 `insert`、`search`、`delete` 等方法分别负责写入、检索和删除底层数据，业务接口层应重点处理参数校验、响应结构、权限控制和异常转换。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Vector/insert.md?utm_source=chatgpt.com)) 该章节对应开发大纲中的「接口设计」部分。

推荐接口路径如下：

| 能力                     | 请求方法 | 接口路径                                           | 说明                                |
| ------------------------ | -------- | -------------------------------------------------- | ----------------------------------- |
| 单条向量写入             | `POST`   | `/api/vector/write`                                | 写入一条向量文档                    |
| 批量向量写入             | `POST`   | `/api/vector/write/batch`                          | 批量写入向量文档                    |
| 向量搜索                 | `POST`   | `/api/vector/search`                               | 按查询向量召回相似数据              |
| 条件搜索                 | `POST`   | `/api/vector/search/filter`                        | 按租户、业务类型等条件过滤检索      |
| 单条删除                 | `DELETE` | `/api/vector/{docId}`                              | 根据主键删除                        |
| 批量删除                 | `POST`   | `/api/vector/delete-batch`                         | 根据主键列表批量删除                |
| 创建 Collection          | `POST`   | `/api/vector/collections/{collectionName}`         | 创建指定 Collection                 |
| 判断 Collection 是否存在 | `GET`    | `/api/vector/collections/{collectionName}/exists`  | 判断 Collection 是否存在            |
| 加载 Collection          | `POST`   | `/api/vector/collections/{collectionName}/load`    | 将 Collection 加载到内存            |
| 释放 Collection          | `POST`   | `/api/vector/collections/{collectionName}/release` | 释放 Collection                     |
| 删除 Collection          | `DELETE` | `/api/vector/collections/{collectionName}`         | 删除 Collection，生产环境需严格限制 |

先定义统一接口响应对象，避免 Controller 直接返回裸数据，便于前端和调用方统一处理成功、失败和错误消息。

文件位置：`src/main/java/io/github/atengk/milvus/common/ApiResult.java`

```java
package io.github.atengk.milvus.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口统一响应结果
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
     * 是否成功
     */
    private Boolean success;

    /**
     * 响应编码
     */
    private String code;

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
                .success(Boolean.TRUE)
                .code("0")
                .message("操作成功")
                .data(data)
                .build();
    }

    /**
     * 成功响应
     *
     * @param message 响应消息
     * @param data    响应数据
     * @param <T>     数据类型
     * @return 响应结果
     */
    public static <T> ApiResult<T> success(String message, T data) {
        return ApiResult.<T>builder()
                .success(Boolean.TRUE)
                .code("0")
                .message(message)
                .data(data)
                .build();
    }

    /**
     * 失败响应
     *
     * @param message 错误消息
     * @return 响应结果
     */
    public static ApiResult<Void> fail(String message) {
        return ApiResult.<Void>builder()
                .success(Boolean.FALSE)
                .code("500")
                .message(message)
                .build();
    }

}
```

### 向量写入接口

向量写入接口用于接收业务系统传入的文本、向量、租户、业务类型、来源和扩展元数据，并调用 `MilvusVectorWriteService` 写入 Milvus。Milvus SDK 的 `insert` 操作要求传入目标 Collection 和行数据列表，每一行通常是一个 `JsonObject`，写入后返回插入数量。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Vector/insert.md?utm_source=chatgpt.com))

如果当前接口直接接收 `embedding`，调用方需要先完成文本向量化；如果项目内部集成了 Embedding 服务，也可以在「业务集成」章节中通过业务服务先生成向量，再调用写入接口。

文件位置：`src/main/java/io/github/atengk/milvus/controller/VectorWriteApiController.java`

```java
package io.github.atengk.milvus.controller;

import io.github.atengk.milvus.common.ApiResult;
import io.github.atengk.milvus.model.VectorDocument;
import io.github.atengk.milvus.model.VectorWriteResult;
import io.github.atengk.milvus.service.MilvusVectorWriteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 向量写入 API
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vector/write")
public class VectorWriteApiController {

    private final MilvusVectorWriteService milvusVectorWriteService;

    /**
     * 写入单条向量文档
     *
     * @param document 向量文档
     * @return 写入结果
     */
    @PostMapping
    public ApiResult<VectorWriteResult> insertOne(@Valid @RequestBody VectorDocument document) {
        VectorWriteResult result = milvusVectorWriteService.insertOne(document);
        return ApiResult.success("向量写入成功", result);
    }

    /**
     * 批量写入向量文档
     *
     * @param request 批量写入请求
     * @return 写入结果
     */
    @PostMapping("/batch")
    public ApiResult<VectorWriteResult> insertBatch(@Valid @RequestBody BatchVectorWriteRequest request) {
        VectorWriteResult result = milvusVectorWriteService.insertBatch(request.getDocuments());
        return ApiResult.success("批量向量写入成功", result);
    }

    /**
     * 批量向量写入请求
     *
     * @author Ateng
     * @since 2026-05-07
     */
    @Data
    public static class BatchVectorWriteRequest {

        /**
         * 向量文档列表
         */
        @NotEmpty(message = "向量文档列表不能为空")
        private List<@Valid VectorDocument> documents;

    }

}
```

单条写入请求示例：

```bash
curl -X POST http://127.0.0.1:8080/api/vector/write \
  -H "Content-Type: application/json" \
  -d '{
    "docId": "doc_10001",
    "chunkId": "chunk_10001_001",
    "title": "Milvus 开发文档",
    "content": "Spring Boot 3 可以通过 Milvus Java SDK 实现向量写入和相似度检索。",
    "bizType": "knowledge",
    "tenantId": "tenant_001",
    "source": "milvus-dev.md",
    "metadata": {
      "category": "vector",
      "version": "1.0"
    },
    "embedding": [0.012, 0.034, 0.056]
  }'
```

响应示例：

```json
{
  "success": true,
  "code": "0",
  "message": "向量写入成功",
  "data": {
    "collectionName": "document_vector",
    "insertCount": 1,
    "docIds": [
      "doc_10001"
    ]
  }
}
```

示例中的 `embedding` 仅展示 3 个元素，实际请求必须传入与 Collection 向量维度一致的完整数组。

### 向量搜索接口

向量搜索接口用于根据查询向量返回相似文档。Milvus Java SDK v2 的 `search` 方法支持设置向量字段、TopK、过滤表达式、输出字段、查询向量和一致性参数；其中 `filter` 可以与向量检索一起使用，实现“先按标量条件过滤，再做向量召回”的组合检索。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Vector/search.md?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/milvus/controller/VectorSearchApiController.java`

```java
package io.github.atengk.milvus.controller;

import io.github.atengk.milvus.common.ApiResult;
import io.github.atengk.milvus.model.VectorSearchRequest;
import io.github.atengk.milvus.model.VectorSearchResult;
import io.github.atengk.milvus.service.MilvusVectorSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 向量搜索 API
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vector/search")
public class VectorSearchApiController {

    private final MilvusVectorSearchService milvusVectorSearchService;

    /**
     * 基础向量相似度搜索
     *
     * @param request 搜索请求
     * @return 搜索结果
     */
    @PostMapping
    public ApiResult<List<VectorSearchResult>> search(@Valid @RequestBody VectorSearchRequest request) {
        List<VectorSearchResult> results = milvusVectorSearchService.search(request);
        return ApiResult.success("向量搜索成功", results);
    }

    /**
     * 条件过滤向量搜索
     *
     * @param request 搜索请求
     * @return 搜索结果
     */
    @PostMapping("/filter")
    public ApiResult<List<VectorSearchResult>> searchWithFilter(@Valid @RequestBody VectorSearchRequest request) {
        List<VectorSearchResult> results = milvusVectorSearchService.searchWithFilter(request);
        return ApiResult.success("条件向量搜索成功", results);
    }

}
```

基础搜索请求示例：

```bash
curl -X POST http://127.0.0.1:8080/api/vector/search \
  -H "Content-Type: application/json" \
  -d '{
    "topK": 5,
    "embedding": [0.012, 0.034, 0.056]
  }'
```

条件搜索请求示例：

```bash
curl -X POST http://127.0.0.1:8080/api/vector/search/filter \
  -H "Content-Type: application/json" \
  -d '{
    "topK": 5,
    "tenantId": "tenant_001",
    "bizType": "knowledge",
    "extraFilter": "create_time >= 1760000000000",
    "embedding": [0.012, 0.034, 0.056]
  }'
```

响应示例：

```json
{
  "success": true,
  "code": "0",
  "message": "条件向量搜索成功",
  "data": [
    {
      "id": "doc_10001",
      "docId": "doc_10001",
      "chunkId": "chunk_10001_001",
      "title": "Milvus 开发文档",
      "content": "Spring Boot 3 可以通过 Milvus Java SDK 实现向量写入和相似度检索。",
      "bizType": "knowledge",
      "tenantId": "tenant_001",
      "source": "milvus-dev.md",
      "createTime": 1760000000000,
      "score": 0.9234,
      "metadata": {
        "category": "vector",
        "version": "1.0"
      }
    }
  ]
}
```

接口设计建议：

| 项目           | 建议                                              |
| -------------- | ------------------------------------------------- |
| `topK`         | 建议限制最大值，例如不超过 100                    |
| `tenantId`     | 多租户系统必须传入并强制过滤                      |
| `bizType`      | 多业务共用 Collection 时建议传入                  |
| `extraFilter`  | 只建议内部可信接口使用                            |
| `outputFields` | 默认不返回向量字段，避免响应体过大                |
| `score`        | 需要结合度量方式解释，`COSINE` 通常分数越大越相似 |

### 向量删除接口

向量删除接口用于根据主键删除单条或多条向量数据。Milvus Java SDK v2 的 `delete` 方法支持通过主键列表或过滤表达式删除实体；生产环境应优先使用主键删除，条件删除需要增加权限控制和审计日志。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Client/MilvusClientV2.md?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/milvus/controller/VectorDeleteApiController.java`

```java
package io.github.atengk.milvus.controller;

import io.github.atengk.milvus.common.ApiResult;
import io.github.atengk.milvus.model.VectorSearchResult;
import io.github.atengk.milvus.service.MilvusVectorMaintenanceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 向量删除 API
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vector")
public class VectorDeleteApiController {

    private final MilvusVectorMaintenanceService milvusVectorMaintenanceService;

    /**
     * 根据主键查询向量数据
     *
     * @param docId 文档主键
     * @return 向量数据
     */
    @GetMapping("/{docId}")
    public ApiResult<VectorSearchResult> getById(@PathVariable String docId) {
        VectorSearchResult result = milvusVectorMaintenanceService.getById(docId);
        return ApiResult.success("查询成功", result);
    }

    /**
     * 根据主键删除单条向量数据
     *
     * @param docId 文档主键
     * @return 删除数量
     */
    @DeleteMapping("/{docId}")
    public ApiResult<Long> deleteById(@PathVariable String docId) {
        Long deleteCount = milvusVectorMaintenanceService.deleteById(docId);
        return ApiResult.success("删除成功", deleteCount);
    }

    /**
     * 根据主键批量删除向量数据
     *
     * @param request 批量删除请求
     * @return 删除数量
     */
    @PostMapping("/delete-batch")
    public ApiResult<Long> deleteBatch(@Valid @RequestBody BatchDeleteRequest request) {
        Long deleteCount = milvusVectorMaintenanceService.deleteBatchByIds(request.getDocIds());
        return ApiResult.success("批量删除成功", deleteCount);
    }

    /**
     * 批量删除请求
     *
     * @author Ateng
     * @since 2026-05-07
     */
    @Data
    public static class BatchDeleteRequest {

        /**
         * 文档主键列表
         */
        @NotEmpty(message = "文档主键列表不能为空")
        private List<@NotBlank(message = "文档主键不能为空") String> docIds;

    }

}
```

删除接口调用示例：

```bash
# 根据主键查询
curl http://127.0.0.1:8080/api/vector/doc_10001

# 根据主键删除
curl -X DELETE http://127.0.0.1:8080/api/vector/doc_10001

# 批量删除
curl -X POST http://127.0.0.1:8080/api/vector/delete-batch \
  -H "Content-Type: application/json" \
  -d '{
    "docIds": ["doc_10001", "doc_10002"]
  }'
```

### Collection 管理接口

Collection 管理接口用于开发、测试和运维场景，提供创建、判断、加载、释放、删除等能力。创建 Collection 会定义字段 Schema 和索引配置；加载 Collection 后才能稳定执行向量检索；释放 Collection 可降低内存占用；删除 Collection 会清除数据和结构，生产环境必须限制调用权限。Milvus 的 `MilvusClientV2` 是 Java SDK v2 中用于执行 Collection、向量 CRUD 等操作的核心客户端。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Client/MilvusClientV2.md?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/milvus/controller/VectorCollectionApiController.java`

```java
package io.github.atengk.milvus.controller;

import io.github.atengk.milvus.common.ApiResult;
import io.github.atengk.milvus.service.MilvusCollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Collection 管理 API
 *
 * @author Ateng
 * @since 2026-05-07
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vector/collections")
public class VectorCollectionApiController {

    private final MilvusCollectionService milvusCollectionService;

    /**
     * 创建默认 Collection
     *
     * @return 操作结果
     */
    @PostMapping("/default")
    public ApiResult<Void> createDefaultCollection() {
        milvusCollectionService.createDefaultCollection();
        return ApiResult.success("默认 Collection 创建成功", null);
    }

    /**
     * 创建指定 Collection
     *
     * @param collectionName Collection 名称
     * @return 操作结果
     */
    @PostMapping("/{collectionName}")
    public ApiResult<Void> createCollection(@PathVariable String collectionName) {
        milvusCollectionService.createCollection(collectionName);
        return ApiResult.success("Collection 创建成功", null);
    }

    /**
     * 查询 Collection 列表
     *
     * @return Collection 名称列表
     */
    @GetMapping
    public ApiResult<List<String>> listCollections() {
        List<String> collections = milvusCollectionService.listCollections();
        return ApiResult.success("Collection 查询成功", collections);
    }

    /**
     * 判断 Collection 是否存在
     *
     * @param collectionName Collection 名称
     * @return 是否存在
     */
    @GetMapping("/{collectionName}/exists")
    public ApiResult<Boolean> hasCollection(@PathVariable String collectionName) {
        Boolean exists = milvusCollectionService.hasCollection(collectionName);
        return ApiResult.success("Collection 状态查询成功", exists);
    }

    /**
     * 加载 Collection
     *
     * @param collectionName Collection 名称
     * @return 操作结果
     */
    @PostMapping("/{collectionName}/load")
    public ApiResult<Void> loadCollection(@PathVariable String collectionName) {
        milvusCollectionService.loadCollection(collectionName);
        return ApiResult.success("Collection 加载成功", null);
    }

    /**
     * 释放 Collection
     *
     * @param collectionName Collection 名称
     * @return 操作结果
     */
    @PostMapping("/{collectionName}/release")
    public ApiResult<Void> releaseCollection(@PathVariable String collectionName) {
        milvusCollectionService.releaseCollection(collectionName);
        return ApiResult.success("Collection 释放成功", null);
    }

    /**
     * 删除 Collection
     *
     * @param collectionName Collection 名称
     * @return 操作结果
     */
    @DeleteMapping("/{collectionName}")
    public ApiResult<Void> dropCollection(@PathVariable String collectionName) {
        milvusCollectionService.dropCollection(collectionName);
        return ApiResult.success("Collection 删除成功", null);
    }

}
```

Collection 管理接口调用示例：

```bash
# 创建默认 Collection
curl -X POST http://127.0.0.1:8080/api/vector/collections/default

# 创建指定 Collection
curl -X POST http://127.0.0.1:8080/api/vector/collections/document_vector

# 查询 Collection 列表
curl http://127.0.0.1:8080/api/vector/collections

# 判断 Collection 是否存在
curl http://127.0.0.1:8080/api/vector/collections/document_vector/exists

# 加载 Collection
curl -X POST http://127.0.0.1:8080/api/vector/collections/document_vector/load

# 释放 Collection
curl -X POST http://127.0.0.1:8080/api/vector/collections/document_vector/release

# 删除 Collection，生产环境需谨慎开放
curl -X DELETE http://127.0.0.1:8080/api/vector/collections/document_vector
```

## 业务集成

本章节用于说明 Milvus 向量数据库如何接入真实业务流程。对于文本检索或 RAG 场景，完整链路通常包括文本清洗、文本切片、调用 Embedding 模型生成向量、向量入库、用户问题向量化、Milvus 相似度检索、召回结果组装和后续问答生成。Milvus 文档将 Embedding 描述为把文本、图片等数据映射到高维向量空间的过程，相似语义数据在该空间中距离更近，适用于语义搜索、推荐和 RAG 等场景。([Milvus](https://milvus.io/docs/id/embeddings.md?utm_source=chatgpt.com)) 该章节对应开发大纲中的「业务集成」部分。

推荐业务集成结构如下：

```text
src/main/java/io/github/atengk/milvus
├── biz
│   ├── dto
│   │   ├── KnowledgeDocumentRequest.java
│   │   └── KnowledgeSearchRequest.java
│   ├── service
│   │   ├── TextEmbeddingService.java
│   │   ├── TextChunkService.java
│   │   └── KnowledgeVectorService.java
│   └── service/impl
│       ├── MockTextEmbeddingServiceImpl.java
│       ├── SimpleTextChunkServiceImpl.java
│       └── KnowledgeVectorServiceImpl.java
└── controller
    └── KnowledgeVectorController.java
```

### 文本向量化流程

文本向量化流程负责将业务文本转换为固定维度的浮点向量。生产环境通常调用内部 Embedding 服务、云厂商 Embedding API 或本地模型服务；开发环境可以先提供一个 Mock 实现，用于验证 Milvus 写入、检索和删除流程是否可用。Milvus 文档也说明，Dense Embedding 通常是由模型生成的数百到数千维浮点向量。([Milvus](https://milvus.io/docs/id/embeddings.md?utm_source=chatgpt.com))

先定义文本向量化服务接口。

文件位置：`src/main/java/io/github/atengk/milvus/biz/service/TextEmbeddingService.java`

```java
package io.github.atengk.milvus.biz.service;

import java.util.List;

/**
 * 文本向量化服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface TextEmbeddingService {

    /**
     * 生成单条文本向量
     *
     * @param text 文本内容
     * @return 文本向量
     */
    List<Float> embed(String text);

    /**
     * 批量生成文本向量
     *
     * @param texts 文本列表
     * @return 文本向量列表
     */
    List<List<Float>> embedBatch(List<String> texts);

}
```

下面是开发环境 Mock 向量化实现。该实现只用于本地流程验证，不具备真实语义表达能力；生产环境应替换为真实 Embedding 服务。

文件位置：`src/main/java/io/github/atengk/milvus/biz/service/impl/MockTextEmbeddingServiceImpl.java`

```java
package io.github.atengk.milvus.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.HashUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.milvus.biz.service.TextEmbeddingService;
import io.github.atengk.milvus.config.MilvusProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock 文本向量化服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class MockTextEmbeddingServiceImpl implements TextEmbeddingService {

    private final MilvusProperties milvusProperties;

    /**
     * 生成单条文本向量
     *
     * @param text 文本内容
     * @return 文本向量
     */
    @Override
    public List<Float> embed(String text) {
        if (StrUtil.isBlank(text)) {
            throw new IllegalArgumentException("向量化文本不能为空");
        }

        Integer dimension = milvusProperties.getCollection().getDimension();
        int seed = HashUtil.bkdrHash(text);

        List<Float> vector = new ArrayList<>(dimension);
        for (int i = 0; i < dimension; i++) {
            int value = Math.abs(HashUtil.bkdrHash(text + ":" + i + ":" + seed) % 10000);
            vector.add(value / 10000.0F);
        }

        log.info("生成 Mock 文本向量完成，dimension={}，textLength={}", dimension, text.length());
        return vector;
    }

    /**
     * 批量生成文本向量
     *
     * @param texts 文本列表
     * @return 文本向量列表
     */
    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        if (CollUtil.isEmpty(texts)) {
            throw new IllegalArgumentException("向量化文本列表不能为空");
        }

        return texts.stream()
                .map(this::embed)
                .toList();
    }

}
```

生产环境可以保留同一个接口，实现类替换为 HTTP 调用 Embedding 服务，例如：

```text
业务文本
  -> 文本清洗
  -> 调用 TextEmbeddingService.embed(text)
  -> 返回 List<Float>
  -> 校验维度
  -> 写入 Milvus
```

文本向量化注意事项：

| 项目       | 建议                                           |
| ---------- | ---------------------------------------------- |
| 模型维度   | 必须与 `milvus.collection.dimension` 一致      |
| 模型版本   | 建议写入 `metadata.modelVersion`，便于后续迁移 |
| 文本清洗   | 去除无意义空白、控制文本长度、保留业务关键字段 |
| 批量向量化 | 文档切片较多时优先批量调用模型服务             |
| 异常处理   | 模型服务失败时不要写入 Milvus                  |
| Mock 实现  | 只用于流程联调，不用于语义检索质量验证         |

### 向量入库流程

向量入库流程用于将业务文档拆分为多个片段，分别生成向量并写入 Milvus。RAG 和知识库检索场景中，推荐按文档片段入库，而不是把整篇文档生成一个向量；这样可以提升召回粒度，避免返回过长上下文。

先定义业务文档入库请求。

文件位置：`src/main/java/io/github/atengk/milvus/biz/dto/KnowledgeDocumentRequest.java`

```java
package io.github.atengk.milvus.biz.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 知识库文档入库请求
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class KnowledgeDocumentRequest {

    /**
     * 文档 ID
     */
    @NotBlank(message = "文档 ID 不能为空")
    private String docId;

    /**
     * 文档标题
     */
    private String title;

    /**
     * 文档内容
     */
    @NotBlank(message = "文档内容不能为空")
    private String content;

    /**
     * 租户标识
     */
    private String tenantId;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 数据来源
     */
    private String source;

    /**
     * 扩展元数据
     */
    private Map<String, Object> metadata;

}
```

定义文本切片服务。这里使用简单字符长度切片，真实生产环境可以按段落、句子、Markdown 标题或 Token 数切片。

文件位置：`src/main/java/io/github/atengk/milvus/biz/service/TextChunkService.java`

```java
package io.github.atengk.milvus.biz.service;

import java.util.List;

/**
 * 文本切片服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface TextChunkService {

    /**
     * 拆分文本片段
     *
     * @param text 文本内容
     * @return 文本片段列表
     */
    List<String> split(String text);

}
```

文件位置：`src/main/java/io/github/atengk/milvus/biz/service/impl/SimpleTextChunkServiceImpl.java`

```java
package io.github.atengk.milvus.biz.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.milvus.biz.service.TextChunkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 简单文本切片服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
public class SimpleTextChunkServiceImpl implements TextChunkService {

    private static final Integer CHUNK_SIZE = 800;
    private static final Integer OVERLAP_SIZE = 100;

    /**
     * 拆分文本片段
     *
     * @param text 文本内容
     * @return 文本片段列表
     */
    @Override
    public List<String> split(String text) {
        if (StrUtil.isBlank(text)) {
            throw new IllegalArgumentException("切片文本不能为空");
        }

        String actualText = StrUtil.trim(text);
        List<String> chunks = new ArrayList<>();

        int start = 0;
        while (start < actualText.length()) {
            int end = Math.min(start + CHUNK_SIZE, actualText.length());
            chunks.add(actualText.substring(start, end));

            if (end >= actualText.length()) {
                break;
            }

            start = Math.max(0, end - OVERLAP_SIZE);
        }

        log.info("文本切片完成，textLength={}，chunkCount={}", actualText.length(), chunks.size());
        return chunks;
    }

}
```

定义知识库向量业务服务。

文件位置：`src/main/java/io/github/atengk/milvus/biz/service/KnowledgeVectorService.java`

```java
package io.github.atengk.milvus.biz.service;

import io.github.atengk.milvus.biz.dto.KnowledgeDocumentRequest;
import io.github.atengk.milvus.biz.dto.KnowledgeSearchRequest;
import io.github.atengk.milvus.model.VectorSearchResult;
import io.github.atengk.milvus.model.VectorWriteResult;

import java.util.List;

/**
 * 知识库向量业务服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface KnowledgeVectorService {

    /**
     * 文档向量化并入库
     *
     * @param request 文档入库请求
     * @return 写入结果
     */
    VectorWriteResult indexDocument(KnowledgeDocumentRequest request);

    /**
     * 根据问题检索知识片段
     *
     * @param request 知识检索请求
     * @return 检索结果
     */
    List<VectorSearchResult> searchKnowledge(KnowledgeSearchRequest request);

}
```

业务入库实现会完成“切片 → 向量化 → 映射 VectorDocument → 批量写入”的完整流程。

文件位置：`src/main/java/io/github/atengk/milvus/biz/service/impl/KnowledgeVectorServiceImpl.java`

```java
package io.github.atengk.milvus.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.milvus.biz.dto.KnowledgeDocumentRequest;
import io.github.atengk.milvus.biz.dto.KnowledgeSearchRequest;
import io.github.atengk.milvus.biz.service.KnowledgeVectorService;
import io.github.atengk.milvus.biz.service.TextChunkService;
import io.github.atengk.milvus.biz.service.TextEmbeddingService;
import io.github.atengk.milvus.model.VectorDocument;
import io.github.atengk.milvus.model.VectorSearchRequest;
import io.github.atengk.milvus.model.VectorSearchResult;
import io.github.atengk.milvus.model.VectorWriteResult;
import io.github.atengk.milvus.service.MilvusVectorSearchService;
import io.github.atengk.milvus.service.MilvusVectorWriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库向量业务服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeVectorServiceImpl implements KnowledgeVectorService {

    private final TextChunkService textChunkService;

    private final TextEmbeddingService textEmbeddingService;

    private final MilvusVectorWriteService milvusVectorWriteService;

    private final MilvusVectorSearchService milvusVectorSearchService;

    /**
     * 文档向量化并入库
     *
     * @param request 文档入库请求
     * @return 写入结果
     */
    @Override
    public VectorWriteResult indexDocument(KnowledgeDocumentRequest request) {
        checkDocumentRequest(request);

        List<String> chunks = textChunkService.split(request.getContent());
        List<List<Float>> embeddings = textEmbeddingService.embedBatch(chunks);

        if (chunks.size() != embeddings.size()) {
            throw new IllegalStateException("文本切片数量与向量数量不一致");
        }

        List<VectorDocument> documents = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (int i = 0; i < chunks.size(); i++) {
            String chunkId = StrUtil.format("{}_chunk_{}", request.getDocId(), i + 1);

            documents.add(VectorDocument.builder()
                    .docId(chunkId)
                    .chunkId(chunkId)
                    .title(StrUtil.blankToDefault(request.getTitle(), request.getDocId()))
                    .content(chunks.get(i))
                    .bizType(StrUtil.blankToDefault(request.getBizType(), "knowledge"))
                    .tenantId(StrUtil.blankToDefault(request.getTenantId(), "default"))
                    .source(StrUtil.blankToDefault(request.getSource(), "manual"))
                    .createTime(now)
                    .metadata(request.getMetadata())
                    .embedding(embeddings.get(i))
                    .build());
        }

        VectorWriteResult result = milvusVectorWriteService.insertBatch(documents);

        log.info("知识库文档向量入库完成，docId={}，chunkCount={}，insertCount={}",
                request.getDocId(),
                chunks.size(),
                result.getInsertCount());

        return result;
    }

    /**
     * 根据问题检索知识片段
     *
     * @param request 知识检索请求
     * @return 检索结果
     */
    @Override
    public List<VectorSearchResult> searchKnowledge(KnowledgeSearchRequest request) {
        if (request == null || StrUtil.isBlank(request.getQuestion())) {
            throw new IllegalArgumentException("检索问题不能为空");
        }

        List<Float> queryEmbedding = textEmbeddingService.embed(request.getQuestion());

        VectorSearchRequest searchRequest = VectorSearchRequest.builder()
                .embedding(queryEmbedding)
                .topK(request.getTopK())
                .tenantId(request.getTenantId())
                .bizType(StrUtil.blankToDefault(request.getBizType(), "knowledge"))
                .extraFilter(request.getExtraFilter())
                .build();

        List<VectorSearchResult> results = milvusVectorSearchService.searchWithFilter(searchRequest);

        log.info("知识库向量召回完成，questionLength={}，topK={}，resultCount={}",
                request.getQuestion().length(),
                request.getTopK(),
                results.size());

        return results;
    }

    /**
     * 校验文档入库请求
     *
     * @param request 文档入库请求
     */
    private void checkDocumentRequest(KnowledgeDocumentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("文档入库请求不能为空");
        }
        if (StrUtil.isBlank(request.getDocId())) {
            throw new IllegalArgumentException("文档 ID 不能为空");
        }
        if (StrUtil.isBlank(request.getContent())) {
            throw new IllegalArgumentException("文档内容不能为空");
        }
    }

}
```

向量入库流程如下：

```text
业务文档
  -> 参数校验
  -> 文本清洗
  -> 文本切片
  -> 批量生成 Embedding
  -> 组装 VectorDocument
  -> 校验向量维度
  -> 批量写入 Milvus
  -> 返回写入数量和主键列表
```

入库接口：

文件位置：`src/main/java/io/github/atengk/milvus/controller/KnowledgeVectorController.java`

```java
package io.github.atengk.milvus.controller;

import io.github.atengk.milvus.biz.dto.KnowledgeDocumentRequest;
import io.github.atengk.milvus.biz.dto.KnowledgeSearchRequest;
import io.github.atengk.milvus.biz.service.KnowledgeVectorService;
import io.github.atengk.milvus.common.ApiResult;
import io.github.atengk.milvus.model.VectorSearchResult;
import io.github.atengk.milvus.model.VectorWriteResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库向量业务接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/knowledge/vector")
public class KnowledgeVectorController {

    private final KnowledgeVectorService knowledgeVectorService;

    /**
     * 文档向量化并入库
     *
     * @param request 文档入库请求
     * @return 写入结果
     */
    @PostMapping("/index")
    public ApiResult<VectorWriteResult> indexDocument(@Valid @RequestBody KnowledgeDocumentRequest request) {
        VectorWriteResult result = knowledgeVectorService.indexDocument(request);
        return ApiResult.success("知识库文档入库成功", result);
    }

    /**
     * 检索知识片段
     *
     * @param request 检索请求
     * @return 检索结果
     */
    @PostMapping("/search")
    public ApiResult<List<VectorSearchResult>> searchKnowledge(@Valid @RequestBody KnowledgeSearchRequest request) {
        List<VectorSearchResult> results = knowledgeVectorService.searchKnowledge(request);
        return ApiResult.success("知识库检索成功", results);
    }

}
```

入库请求示例：

```bash
curl -X POST http://127.0.0.1:8080/api/knowledge/vector/index \
  -H "Content-Type: application/json" \
  -d '{
    "docId": "doc_milvus_guide",
    "title": "Milvus 开发指南",
    "content": "Milvus 是向量数据库，可以用于语义搜索、知识库问答、推荐系统和 RAG 检索增强。Spring Boot 3 可以通过 Milvus Java SDK 连接 Milvus 服务。",
    "tenantId": "tenant_001",
    "bizType": "knowledge",
    "source": "milvus-guide.md",
    "metadata": {
      "category": "vector",
      "author": "Ateng"
    }
  }'
```

### 检索召回流程

检索召回流程用于把用户问题转换成向量，再从 Milvus 中召回相似文本片段。Milvus 的向量相似度搜索可以叠加标量过滤表达式，这使得业务系统能够在同一个 Collection 中按租户、业务类型、时间范围等条件限定候选数据。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Vector/search.md?utm_source=chatgpt.com)) 如果检索结果量较大，Milvus 还提供 `SearchIteratorV2` 用于迭代搜索结果，适合大结果集遍历场景。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Vector/SearchIteratorV2.md?utm_source=chatgpt.com))

定义知识库检索请求。

文件位置：`src/main/java/io/github/atengk/milvus/biz/dto/KnowledgeSearchRequest.java`

```java
package io.github.atengk.milvus.biz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 知识库检索请求
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class KnowledgeSearchRequest {

    /**
     * 用户问题
     */
    @NotBlank(message = "用户问题不能为空")
    private String question;

    /**
     * 返回数量
     */
    @Min(value = 1, message = "TopK 不能小于 1")
    @Max(value = 100, message = "TopK 不能大于 100")
    private Integer topK = 5;

    /**
     * 租户标识
     */
    private String tenantId;

    /**
     * 业务类型
     */
    private String bizType = "knowledge";

    /**
     * 扩展过滤表达式
     */
    private String extraFilter;

}
```

检索请求示例：

```bash
curl -X POST http://127.0.0.1:8080/api/knowledge/vector/search \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Spring Boot 如何连接 Milvus 做向量检索？",
    "topK": 5,
    "tenantId": "tenant_001",
    "bizType": "knowledge"
  }'
```

响应示例：

```json
{
  "success": true,
  "code": "0",
  "message": "知识库检索成功",
  "data": [
    {
      "id": "doc_milvus_guide_chunk_1",
      "docId": "doc_milvus_guide_chunk_1",
      "chunkId": "doc_milvus_guide_chunk_1",
      "title": "Milvus 开发指南",
      "content": "Milvus 是向量数据库，可以用于语义搜索、知识库问答、推荐系统和 RAG 检索增强。",
      "bizType": "knowledge",
      "tenantId": "tenant_001",
      "source": "milvus-guide.md",
      "score": 0.8941,
      "metadata": {
        "category": "vector",
        "author": "Ateng"
      }
    }
  ]
}
```

检索召回流程如下：

```text
用户问题
  -> 参数校验
  -> 问题文本向量化
  -> 构造 VectorSearchRequest
  -> 按 tenant_id、biz_type 等条件过滤
  -> Milvus TopK 相似度检索
  -> 解析 docId、content、score、metadata
  -> 返回给业务侧
  -> 可选：重排、去重、上下文拼接、调用大模型生成答案
```

RAG 场景下，召回结果通常不会直接作为最终答案返回，而是先按业务规则组装上下文，再传给大模型。Milvus 官方 RAG 示例也采用“生成查询向量 → 从 Milvus 检索相关文本 → 将检索结果作为上下文传入生成模型”的流程。([Milvus](https://milvus.io/docs/build_RAG_with_milvus_and_gemini.md?utm_source=chatgpt.com))

检索召回建议：

| 项目       | 建议                                                     |
| ---------- | -------------------------------------------------------- |
| 问题向量化 | 查询问题必须使用与入库相同的 Embedding 模型              |
| 租户过滤   | 多租户系统必须强制带上 `tenantId`                        |
| TopK       | RAG 场景通常从 5 到 10 开始调试                          |
| 去重       | 同一文档多个片段命中时，可按 `source` 或原始文档 ID 去重 |
| 重排       | 对质量要求高的场景，可增加 rerank 模型                   |
| 上下文长度 | 拼接给大模型前要控制 Token 数                            |
| 分数阈值   | 可设置最低相似度阈值，过滤低质量召回                     |
| 监控指标   | 记录检索耗时、召回数量、空结果率和平均分数               |

完成接口设计和业务集成后，整个 Spring Boot 3 + Milvus 的核心开发链路已经贯通：先通过 Collection 管理接口初始化集合，再通过业务入库接口完成文本切片、向量化和入库，最后通过检索接口实现语义召回。

## 测试与验证

本章节用于验证 Spring Boot 3 与 Milvus 的完整集成链路，包括连接测试、Collection 创建测试、向量写入测试和相似度搜索测试。测试建议优先使用集成测试方式，直接连接本地或测试环境 Milvus 服务，验证真实 SDK 调用链路，而不是只做 Mock。Spring Boot 提供 `spring-boot-starter-test`，其中包含 Spring Boot 测试支持、JUnit Jupiter、AssertJ、Hamcrest 等常用测试依赖，适合编写当前场景的集成测试。([Home](https://docs.spring.io/spring-boot/3.5/reference/testing/index.html?utm_source=chatgpt.com)) 该章节对应开发大纲中的「测试与验证」部分。

测试前需要确认 Milvus 服务已启动，并且 `application-test.yml` 中的连接地址、Token、Collection 名称和向量维度与测试数据一致。

文件位置：`src/test/resources/application-test.yml`

```yaml
spring:
  application:
    name: milvus-demo-test

milvus:
  # 测试环境 Milvus 地址
  uri: http://127.0.0.1:19530

  # 测试环境数据库
  database-name: default

  # 本地 standalone 默认账号密码
  token: root:Milvus

  # 本地测试默认不启用 TLS
  secure: false

  # 连接超时时间
  connect-timeout-ms: 10000

  # RPC deadline，0 表示不启用全局 deadline
  rpc-deadline-ms: 0

  collection:
    # 测试 Collection，避免污染开发或生产 Collection
    name: document_vector_test

    # 测试 Collection 描述
    description: Milvus 集成测试集合

    # 主键字段
    primary-field-name: doc_id

    # 向量字段
    vector-field-name: embedding

    # 文本字段
    content-field-name: content

    # 元数据字段
    metadata-field-name: metadata

    # 为了测试数据更短，这里使用 8 维；生产环境按真实 Embedding 模型维度配置
    dimension: 8

    # 通用自动索引
    index-type: AUTOINDEX

    # 文本语义检索推荐余弦相似度
    metric-type: COSINE

    # 测试阶段开启动态字段，便于扩展字段验证
    enable-dynamic-field: true
```

测试工具类用于生成固定维度的测试向量，避免每个测试方法重复编写向量构造逻辑。

文件位置：`src/test/java/io/github/atengk/milvus/MilvusTestVectorFactory.java`

```java
package io.github.atengk.milvus;

import cn.hutool.core.util.HashUtil;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Milvus 测试向量工厂
 *
 * @author Ateng
 * @since 2026-05-07
 */
public class MilvusTestVectorFactory {

    /**
     * 根据文本生成固定维度测试向量
     *
     * @param text      文本内容
     * @param dimension 向量维度
     * @return 测试向量
     */
    public static List<Float> createVector(String text, Integer dimension) {
        if (StrUtil.isBlank(text)) {
            throw new IllegalArgumentException("测试文本不能为空");
        }
        if (dimension == null || dimension <= 0) {
            throw new IllegalArgumentException("测试向量维度不合法");
        }

        List<Float> vector = new ArrayList<>(dimension);
        for (int i = 0; i < dimension; i++) {
            int hash = Math.abs(HashUtil.bkdrHash(text + ":" + i) % 10000);
            vector.add(hash / 10000.0F);
        }
        return vector;
    }

}
```

### Milvus 连接测试

Milvus 连接测试用于验证 Spring Boot 是否能够正确初始化 `MilvusClientV2`，并能向 Milvus 服务发起基础请求。Milvus Java SDK v2 的 `listCollectionsV2` 可用于读取 Collection 列表，适合作为轻量级连通性检查；如果地址、Token、数据库或网络配置异常，该调用会直接失败。`createCollection`、`listCollectionsV2` 等操作都依赖同一个 `MilvusClientV2` 客户端对象。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Collections/createCollection.md?utm_source=chatgpt.com))

文件位置：`src/test/java/io/github/atengk/milvus/MilvusConnectionTest.java`

```java
package io.github.atengk.milvus;

import io.github.atengk.milvus.config.MilvusProperties;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.collection.request.ListCollectionsReq;
import io.milvus.v2.service.collection.response.ListCollectionsResp;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Milvus 连接测试
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class MilvusConnectionTest {

    @Autowired
    private MilvusClientV2 milvusClientV2;

    @Autowired
    private MilvusProperties milvusProperties;

    /**
     * 测试 Milvus 客户端是否可以正常连接
     */
    @Test
    void shouldConnectToMilvus() {
        ListCollectionsReq request = ListCollectionsReq.builder()
                .databaseName(milvusProperties.getActualDatabaseName())
                .build();

        ListCollectionsResp response = milvusClientV2.listCollectionsV2(request);

        assertThat(response).isNotNull();
        assertThat(response.getCollectionNames()).isNotNull();

        log.info("Milvus 连接测试成功，database={}，collectionCount={}",
                milvusProperties.getActualDatabaseName(),
                response.getCollectionNames().size());
    }

}
```

执行命令：

```bash
# 执行 Milvus 连接测试
mvn -Dtest=MilvusConnectionTest test
```

如果测试失败，优先检查 Milvus 容器是否启动、`19530` 端口是否可访问、`milvus.uri` 是否正确、Token 是否匹配。

### Collection 创建测试

Collection 创建测试用于验证 Schema、字段名称、向量维度、索引类型和相似度度量方式是否能够被 Milvus 正确接受。Milvus Java SDK v2 的 `createCollection` 支持通过 `collectionSchema` 自定义 Collection 字段，并通过 `indexParams` 一起创建索引；`getLoadState` 可用于判断 Collection 或 Partition 是否已加载。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Collections/createCollection.md?utm_source=chatgpt.com))

文件位置：`src/test/java/io/github/atengk/milvus/MilvusCollectionIntegrationTest.java`

```java
package io.github.atengk.milvus;

import io.github.atengk.milvus.config.MilvusProperties;
import io.github.atengk.milvus.service.MilvusCollectionService;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.collection.request.GetLoadStateReq;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Milvus Collection 集成测试
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class MilvusCollectionIntegrationTest {

    @Autowired
    private MilvusCollectionService milvusCollectionService;

    @Autowired
    private MilvusProperties milvusProperties;

    @Autowired
    private MilvusClientV2 milvusClientV2;

    /**
     * 测试创建默认 Collection
     */
    @Test
    void shouldCreateDefaultCollection() {
        String collectionName = milvusProperties.getCollection().getName();

        milvusCollectionService.createDefaultCollection();

        Boolean exists = milvusCollectionService.hasCollection(collectionName);
        assertThat(exists).isTrue();

        log.info("Collection 创建测试成功，collection={}", collectionName);
    }

    /**
     * 测试加载 Collection
     */
    @Test
    void shouldLoadCollection() {
        String collectionName = milvusProperties.getCollection().getName();

        milvusCollectionService.createDefaultCollection();
        milvusCollectionService.loadCollection(collectionName);

        GetLoadStateReq request = GetLoadStateReq.builder()
                .databaseName(milvusProperties.getActualDatabaseName())
                .collectionName(collectionName)
                .build();

        Boolean loaded = milvusClientV2.getLoadState(request);
        assertThat(loaded).isTrue();

        log.info("Collection 加载测试成功，collection={}，loaded={}", collectionName, loaded);
    }

}
```

执行命令：

```bash
# 执行 Collection 创建与加载测试
mvn -Dtest=MilvusCollectionIntegrationTest test
```

如果 Collection 已经存在，前面封装的 `createDefaultCollection()` 应该保持幂等，直接跳过创建。测试失败时重点检查字段类型、向量维度、索引类型和 Collection 名称是否合法。

### 向量写入测试

向量写入测试用于验证业务对象能否正确映射为 Milvus 行数据，并成功写入目标 Collection。Milvus Java SDK v2 的 `insert` 操作接收 `InsertReq`，其中 `data` 是 `List<JsonObject>`，返回值 `InsertResp` 包含插入实体数量；按主键读取可以使用 `get` 方法，并指定 `ids` 和 `outputFields`。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Vector/insert.md?utm_source=chatgpt.com))

文件位置：`src/test/java/io/github/atengk/milvus/MilvusVectorWriteIntegrationTest.java`

```java
package io.github.atengk.milvus;

import io.github.atengk.milvus.config.MilvusProperties;
import io.github.atengk.milvus.model.VectorDocument;
import io.github.atengk.milvus.model.VectorSearchResult;
import io.github.atengk.milvus.model.VectorWriteResult;
import io.github.atengk.milvus.service.MilvusCollectionService;
import io.github.atengk.milvus.service.MilvusVectorMaintenanceService;
import io.github.atengk.milvus.service.MilvusVectorWriteService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Milvus 向量写入集成测试
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class MilvusVectorWriteIntegrationTest {

    @Autowired
    private MilvusProperties milvusProperties;

    @Autowired
    private MilvusCollectionService milvusCollectionService;

    @Autowired
    private MilvusVectorWriteService milvusVectorWriteService;

    @Autowired
    private MilvusVectorMaintenanceService milvusVectorMaintenanceService;

    /**
     * 测试单条向量写入
     */
    @Test
    void shouldInsertOneVectorDocument() {
        String collectionName = milvusProperties.getCollection().getName();
        Integer dimension = milvusProperties.getCollection().getDimension();

        milvusCollectionService.createDefaultCollection();
        milvusCollectionService.loadCollection(collectionName);

        VectorDocument document = VectorDocument.builder()
                .docId("test_doc_write_001")
                .chunkId("test_doc_write_001")
                .title("Milvus 写入测试")
                .content("这是一条用于验证 Milvus 向量写入能力的测试数据。")
                .bizType("test")
                .tenantId("tenant_test")
                .source("integration-test")
                .createTime(System.currentTimeMillis())
                .metadata(Map.of("case", "insert-one"))
                .embedding(MilvusTestVectorFactory.createVector("Milvus 写入测试", dimension))
                .build();

        VectorWriteResult writeResult = milvusVectorWriteService.insertOne(document);

        assertThat(writeResult).isNotNull();
        assertThat(writeResult.getInsertCount()).isEqualTo(1);
        assertThat(writeResult.getDocIds()).contains("test_doc_write_001");

        VectorSearchResult queryResult = milvusVectorMaintenanceService.getById("test_doc_write_001");

        assertThat(queryResult).isNotNull();
        assertThat(queryResult.getDocId()).isEqualTo("test_doc_write_001");
        assertThat(queryResult.getContent()).contains("Milvus 向量写入能力");

        log.info("向量写入测试成功，collection={}，docId={}", collectionName, document.getDocId());
    }

}
```

执行命令：

```bash
# 执行向量写入测试
mvn -Dtest=MilvusVectorWriteIntegrationTest test
```

如果写入失败，优先检查 Collection 是否存在、是否已经创建索引、主键字段是否为空、向量维度是否与配置一致、`content` 字段长度是否超过 Schema 中的 `maxLength`。

### 相似度搜索测试

相似度搜索测试用于验证写入的数据是否可以通过查询向量召回。Milvus Java SDK v2 的 `search` 操作支持 `collectionName`、`annsField`、`topK`、`filter`、`outputFields`、`data` 等参数，其中 `filter` 可以叠加标量条件过滤，`topK` 控制返回结果数量。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Vector/search.md?utm_source=chatgpt.com))

文件位置：`src/test/java/io/github/atengk/milvus/MilvusVectorSearchIntegrationTest.java`

```java
package io.github.atengk.milvus;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.milvus.config.MilvusProperties;
import io.github.atengk.milvus.model.VectorDocument;
import io.github.atengk.milvus.model.VectorSearchRequest;
import io.github.atengk.milvus.model.VectorSearchResult;
import io.github.atengk.milvus.service.MilvusCollectionService;
import io.github.atengk.milvus.service.MilvusVectorSearchService;
import io.github.atengk.milvus.service.MilvusVectorWriteService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Milvus 向量搜索集成测试
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class MilvusVectorSearchIntegrationTest {

    @Autowired
    private MilvusProperties milvusProperties;

    @Autowired
    private MilvusCollectionService milvusCollectionService;

    @Autowired
    private MilvusVectorWriteService milvusVectorWriteService;

    @Autowired
    private MilvusVectorSearchService milvusVectorSearchService;

    /**
     * 测试相似度搜索
     */
    @Test
    void shouldSearchSimilarVectorDocuments() {
        String collectionName = milvusProperties.getCollection().getName();
        Integer dimension = milvusProperties.getCollection().getDimension();

        milvusCollectionService.createDefaultCollection();
        milvusCollectionService.loadCollection(collectionName);

        List<Float> queryVector = MilvusTestVectorFactory.createVector("Spring Boot Milvus 检索测试", dimension);

        VectorDocument document = VectorDocument.builder()
                .docId("test_doc_search_001")
                .chunkId("test_doc_search_001")
                .title("Milvus 检索测试")
                .content("Spring Boot 3 集成 Milvus 后，可以根据查询向量执行相似度搜索。")
                .bizType("test")
                .tenantId("tenant_test")
                .source("integration-test")
                .createTime(System.currentTimeMillis())
                .metadata(Map.of("case", "search"))
                .embedding(queryVector)
                .build();

        milvusVectorWriteService.insertOne(document);

        VectorSearchRequest request = VectorSearchRequest.builder()
                .embedding(queryVector)
                .topK(5)
                .tenantId("tenant_test")
                .bizType("test")
                .build();

        List<VectorSearchResult> results = milvusVectorSearchService.searchWithFilter(request);

        assertThat(results).isNotEmpty();
        assertThat(results.stream().map(VectorSearchResult::getDocId).toList())
                .contains("test_doc_search_001");

        VectorSearchResult first = CollUtil.getFirst(results);
        assertThat(first.getScore()).isNotNull();

        log.info("相似度搜索测试成功，collection={}，resultCount={}，firstDocId={}，score={}",
                collectionName,
                results.size(),
                first.getDocId(),
                first.getScore());
    }

}
```

执行命令：

```bash
# 执行相似度搜索测试
mvn -Dtest=MilvusVectorSearchIntegrationTest test
```

如果检索结果为空，重点检查 Collection 是否已加载、写入和查询是否使用相同维度和同一模型、过滤条件是否过窄、TopK 是否设置过小。

完整执行全部 Milvus 集成测试：

```bash
# 执行所有 Milvus 相关测试
mvn -Dtest="Milvus*Test,Milvus*IntegrationTest" test
```

## 常见问题

本章节整理 Spring Boot 3 集成 Milvus 过程中最常见的问题，包括连接失败、向量维度不一致、Collection 未加载和检索结果为空。排查时建议先从环境连通性开始，再检查配置、Collection 状态、数据写入和检索条件。Milvus 的 `getLoadState` 可用于确认 Collection 加载状态，`get` 可用于按主键确认数据是否已经写入，`search` 可用于验证相似度检索链路。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Management/getLoadState.md?utm_source=chatgpt.com)) 该章节对应开发大纲中的「常见问题」部分。

### 连接失败处理

连接失败通常发生在应用启动、健康检查、Collection 查询或首次调用 Milvus SDK 时。常见原因包括 Milvus 服务未启动、端口不可达、`uri` 配置错误、Token 不正确、TLS 配置不匹配、数据库名称错误或 Docker 网络不可达。

排查顺序如下：

| 排查项          | 命令或检查方式                            | 处理建议                                        |
| --------------- | ----------------------------------------- | ----------------------------------------------- |
| Milvus 容器状态 | `docker compose ps`                       | 确认 `milvus-standalone` 正常运行               |
| 服务日志        | `docker logs -f milvus-standalone`        | 检查启动异常、存储异常、端口异常                |
| 端口监听        | `docker port milvus-standalone 19530/tcp` | 确认 Milvus gRPC 端口已暴露                     |
| Spring 配置     | `milvus.uri`                              | 本地一般为 `http://127.0.0.1:19530`             |
| Token           | `milvus.token`                            | 本地默认可使用 `root:Milvus`                    |
| TLS             | `milvus.secure`                           | 本地 HTTP 通常为 `false`，生产 HTTPS 按实际配置 |
| 数据库          | `milvus.database-name`                    | 默认库一般为 `default`                          |

连接失败时先执行以下命令：

```bash
# 查看 Milvus 容器状态
docker compose ps

# 查看 Milvus 日志
docker logs -f milvus-standalone

# 检查端口映射
docker port milvus-standalone 19530/tcp

# 执行连接测试
mvn -Dtest=MilvusConnectionTest test
```

常见错误与处理方式：

| 错误现象             | 可能原因                    | 处理方式                           |
| -------------------- | --------------------------- | ---------------------------------- |
| `Connection refused` | Milvus 未启动或端口不通     | 启动 Milvus，检查 `19530` 端口     |
| `UNAVAILABLE`        | 网络不可达或服务未就绪      | 等待服务启动完成，检查 Docker 网络 |
| `Unauthenticated`    | Token 错误或鉴权配置不一致  | 检查 `milvus.token`                |
| TLS 握手失败         | `secure` 与服务端协议不一致 | 本地设为 `false`，生产按证书配置   |
| 数据库不存在         | `database-name` 配错        | 使用已有数据库或先创建数据库       |

建议在项目中保留 Actuator 健康检查接口，通过 `MilvusHealthIndicator` 提前暴露连接问题，而不是等到业务请求进来后才发现 Milvus 不可用。

### 向量维度不一致

向量维度不一致是向量数据库开发中最常见的写入和检索问题。Milvus Collection 创建时已经确定向量字段维度，后续写入和搜索时，输入向量长度必须与该维度完全一致。Milvus 创建 Collection 时支持通过 `dimension` 或自定义 Schema 设置向量字段维度，搜索时也需要向 `data` 传入与该维度匹配的查询向量。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Collections/createCollection.md?utm_source=chatgpt.com))

典型错误场景：

| 场景                                         | 示例                           |
| -------------------------------------------- | ------------------------------ |
| Collection 配置为 1024 维，写入 768 维       | Embedding 模型换了，但配置没改 |
| Collection 配置为 1536 维，查询传入 3 个元素 | 调试请求只填了示例数组         |
| 入库模型和查询模型不同                       | 写入用 BGE，查询用其他模型     |
| 模型升级后直接复用旧 Collection              | 新模型维度或向量空间变化       |

建议在写入和检索前统一做维度校验。前文 `VectorDocument#validate` 和 `VectorSearchRequest#validateDimension` 已经覆盖该场景：

```java
if (embedding.size() != expectedDimension) {
    throw new IllegalArgumentException(
            StrUtil.format("向量维度不一致，期望维度={}，实际维度={}", expectedDimension, embedding.size())
    );
}
```

处理方式：

| 问题              | 处理                                              |
| ----------------- | ------------------------------------------------- |
| 写入维度错误      | 检查 Embedding 服务返回值长度                     |
| 查询维度错误      | 确认查询问题使用同一 Embedding 模型               |
| 模型升级          | 新建 Collection，重新向量化并迁移数据             |
| 测试请求只传 3 维 | 改为传完整维度，或测试环境临时将 `dimension` 调小 |
| 多模型混用        | 按模型版本或维度拆分 Collection                   |

生产环境建议把模型名称、模型版本和维度写入配置中心，并在 Milvus Collection 元数据或应用配置中同步记录，避免后续无法判断历史数据来自哪个向量模型。

### Collection 未加载

Collection 未加载会导致搜索不可用或首次检索延迟较高。Milvus 的 `loadCollection` 用于将 Collection 数据加载到内存，`releaseCollection` 用于释放内存，`getLoadState` 可以返回指定 Collection 或 Partition 是否处于加载状态。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Management/loadCollection.md?utm_source=chatgpt.com))

判断 Collection 是否加载：

```java
GetLoadStateReq request = GetLoadStateReq.builder()
        .databaseName(milvusProperties.getActualDatabaseName())
        .collectionName(collectionName)
        .build();

Boolean loaded = milvusClientV2.getLoadState(request);
```

加载 Collection：

```java
LoadCollectionReq request = LoadCollectionReq.builder()
        .databaseName(milvusProperties.getActualDatabaseName())
        .collectionName(collectionName)
        .async(Boolean.FALSE)
        .sync(Boolean.TRUE)
        .timeout(60000L)
        .build();

milvusClientV2.loadCollection(request);
```

常见处理方式：

| 场景                       | 处理                                        |
| -------------------------- | ------------------------------------------- |
| 新建 Collection 后立即搜索 | 创建后调用 `loadCollection`                 |
| 手动释放后再次搜索         | 搜索前检查 `getLoadState`，未加载则重新加载 |
| 应用重启后首次搜索慢       | 启动后预加载核心 Collection                 |
| 大 Collection 加载超时     | 增大 timeout，检查资源组、内存和数据规模    |
| 不常用 Collection 占用内存 | 低频业务可按需加载，长期不用则释放          |

建议在业务检索服务中增加防御性检查：如果 Collection 不存在，直接抛出明确异常；如果 Collection 未加载，可根据业务策略自动加载或提示运维处理。

### 检索结果为空

检索结果为空不一定代表 Milvus 异常，更多情况下是数据未写入、过滤条件过严、Collection 未加载、查询向量质量差、入库和查询模型不一致、TopK 设置过小或输出字段配置错误。Milvus 的 `search` 支持 `filter`、`topK`、`outputFields` 和查询向量数据；如果过滤表达式范围过窄，即使向量相似，也不会返回结果。([Milvus](https://milvus.io/api-reference/java/v2.6.x/v2/Vector/search.md?utm_source=chatgpt.com))

排查顺序如下：

| 排查项              | 验证方式                                       | 处理建议                      |
| ------------------- | ---------------------------------------------- | ----------------------------- |
| Collection 是否存在 | `hasCollection`                                | 不存在则先创建                |
| Collection 是否加载 | `getLoadState`                                 | 未加载则执行 `loadCollection` |
| 数据是否写入        | `getById`                                      | 未写入则检查插入流程          |
| 过滤条件是否过严    | 去掉 `tenantId`、`bizType`、`extraFilter` 再试 | 逐步恢复过滤条件              |
| TopK 是否过小       | 调大到 10 或 20                                | 观察是否有结果                |
| 查询向量维度        | 校验 `embedding.size()`                        | 保证与配置一致                |
| 模型是否一致        | 检查模型版本                                   | 入库和查询必须使用同一模型    |
| 文本切片是否合理    | 查看 `content` 字段                            | 切片过短或噪音过多会影响召回  |

建议按以下方式定位问题：

```bash
# 1. 查询 Collection 是否存在
curl http://127.0.0.1:8080/api/vector/collections/document_vector_test/exists

# 2. 加载 Collection
curl -X POST http://127.0.0.1:8080/api/vector/collections/document_vector_test/load

# 3. 根据主键确认数据是否存在
curl http://127.0.0.1:8080/api/vector/test_doc_search_001

# 4. 不带过滤条件执行基础搜索
curl -X POST http://127.0.0.1:8080/api/vector/search \
  -H "Content-Type: application/json" \
  -d '{
    "topK": 10,
    "embedding": [0.012, 0.034, 0.056]
  }'

# 5. 再逐步增加 tenantId、bizType、extraFilter
curl -X POST http://127.0.0.1:8080/api/vector/search/filter \
  -H "Content-Type: application/json" \
  -d '{
    "topK": 10,
    "tenantId": "tenant_test",
    "bizType": "test",
    "embedding": [0.012, 0.034, 0.056]
  }'
```

示例中的 `embedding` 仍然只是占位数组，实际测试必须传入完整维度向量。若测试环境配置为 `dimension: 8`，请求体需要传 8 个浮点数；若生产配置为 `1024`，请求体必须传 1024 个浮点数。

常见解决方案：

| 问题                | 解决方案                                          |
| ------------------- | ------------------------------------------------- |
| 数据未写入          | 先执行向量写入测试，确认 `insertCount > 0`        |
| 写入后立即查不到    | 检查一致性策略，必要时短暂等待或按主键验证        |
| 过滤字段不匹配      | 确认写入的 `tenant_id`、`biz_type` 与查询条件一致 |
| Mock 向量无语义效果 | 使用真实 Embedding 模型验证召回质量               |
| 文档切片效果差      | 调整切片大小、重叠长度和文本清洗规则              |
| 相似度分数偏低      | 检查模型、度量方式、归一化策略和数据质量          |

完成测试与常见问题处理后，Spring Boot 3 + Milvus 的开发文档已经覆盖从环境部署、基础配置、客户端封装、Collection 管理、向量写入、向量检索、数据维护、接口设计、业务集成到测试验证的完整开发链路。