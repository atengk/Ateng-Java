# Spring Data Elasticsearch

Spring Data Elasticsearch 是 Spring Data 体系中面向 Elasticsearch 的数据访问模块，适合在 Spring Boot 3 项目中实现索引管理、文档读写、分页排序、复杂检索、高亮查询和业务数据同步等能力。

## 模块概述

本模块用于封装业务系统与 Elasticsearch 之间的数据访问逻辑。它不直接替代业务数据库，而是作为搜索引擎访问层，为业务服务提供统一、稳定、可复用的搜索能力。

### 技术定位

Spring Data Elasticsearch 的技术定位是 Spring Boot 应用中的搜索数据访问层。它基于 Spring Data 编程模型，为 Elasticsearch 提供 Repository、ElasticsearchOperations、对象映射、索引操作和查询封装能力。

在项目分层中，推荐将 Spring Data Elasticsearch 放在 Service 层之后，作为独立的数据访问模块使用：

| 层级                                  | 职责                                           |
| ------------------------------------- | ---------------------------------------------- |
| Controller                            | 接收搜索请求、写入请求、索引管理请求           |
| Service                               | 编排业务逻辑，处理参数校验、权限判断、数据转换 |
| Elasticsearch Repository / Operations | 执行索引操作、文档写入、查询检索、分页排序     |
| Elasticsearch                         | 存储搜索文档并执行检索                         |

Spring Data Elasticsearch 主要提供以下能力：

| 能力            | 说明                                                         |
| --------------- | ------------------------------------------------------------ |
| 文档映射        | 使用 `@Document`、`@Field` 等注解定义索引和字段              |
| Repository 开发 | 通过 `ElasticsearchRepository` 快速完成基础 CRUD 和派生查询  |
| Operations 开发 | 通过 `ElasticsearchOperations` 实现复杂查询、索引管理和批量操作 |
| 查询封装        | 支持精确查询、模糊查询、范围查询、组合查询、高亮查询         |
| 分页排序        | 支持搜索结果分页、字段排序和结果转换                         |
| 数据同步        | 将业务库中的数据同步为 Elasticsearch 搜索文档                |

Spring Data Elasticsearch 官方版本矩阵显示，Spring Data 2025.0 对应 Spring Data Elasticsearch 5.5.x、Elasticsearch 8.18.x、Spring Framework 6.2.x；这与 Spring Boot 3.x 的技术栈更匹配。([Home](https://docs.spring.io/spring-data/elasticsearch/reference/elasticsearch/versions.html?utm_source=chatgpt.com))

### 使用场景

Spring Data Elasticsearch 适用于需要全文搜索、多条件检索、聚合筛选、查询加速和高亮展示的业务场景。它更偏向“读优化”和“搜索体验优化”，不适合作为强事务业务主库。

典型使用场景如下：

| 使用场景       | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| 商品搜索       | 按商品名称、分类、品牌、价格、标签进行搜索和筛选             |
| 文章搜索       | 按标题、正文、作者、发布时间进行全文检索                     |
| 日志检索       | 按关键字、时间范围、日志级别、服务名称查询日志               |
| 订单搜索       | 对订单号、用户信息、商品信息等冗余字段进行快速查询           |
| 用户搜索       | 按昵称、手机号、标签、地区等条件查询用户                     |
| 内容高亮       | 搜索结果中高亮命中的标题、摘要、正文关键字                   |
| 多条件组合查询 | 同时支持关键字、状态、时间范围、分类等条件组合               |
| 宽表查询加速   | 将多表关联后的结果同步到 Elasticsearch，减少复杂 SQL 查询压力 |

适合使用 Elasticsearch 的情况：

| 判断项       | 说明                                 |
| ------------ | ------------------------------------ |
| 查询条件复杂 | 存在多个筛选条件、排序条件、范围条件 |
| 文本检索较多 | 需要分词、模糊匹配、相关性评分       |
| 数据读多写少 | 查询频率高于写入频率                 |
| 允许最终一致 | 搜索结果允许存在短暂同步延迟         |
| 需要结果高亮 | 前端需要展示命中的关键词位置         |

不适合使用 Elasticsearch 的情况：

| 判断项           | 说明                                 |
| ---------------- | ------------------------------------ |
| 强事务写入       | 例如支付、库存扣减、余额变更         |
| 强一致读取       | 查询结果必须立即反映最新写入         |
| 复杂关系建模     | 高度依赖多表关联、外键约束、事务回滚 |
| 小数据量简单查询 | 普通数据库索引已经能满足查询性能     |

### 功能边界

Spring Data Elasticsearch 模块需要明确功能边界，避免把搜索模块设计成业务主存储或通用数据治理平台。

本模块负责以下功能：

| 功能     | 说明                                                 |
| -------- | ---------------------------------------------------- |
| 索引定义 | 定义索引名称、字段类型、分词器、日期格式、关键字字段 |
| 索引管理 | 创建索引、删除索引、判断索引是否存在、刷新索引       |
| 文档写入 | 将业务对象转换为 Elasticsearch 文档并写入索引        |
| 文档更新 | 根据文档 ID 或业务主键更新搜索文档                   |
| 文档删除 | 根据业务删除事件同步删除 Elasticsearch 文档          |
| 条件查询 | 封装精确查询、模糊查询、范围查询、组合查询           |
| 分页排序 | 对搜索结果进行分页、排序和结果转换                   |
| 高亮查询 | 对命中的字段内容进行高亮处理                         |
| 异常处理 | 处理连接异常、索引不存在、查询失败、写入失败等问题   |
| 操作日志 | 记录关键索引操作、数据同步操作和异常查询日志         |

本模块不负责以下功能：

| 非职责         | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| 业务主数据存储 | 主数据仍以 MySQL、PostgreSQL 等业务数据库为准                |
| 分布式事务控制 | Elasticsearch 写入不参与数据库事务提交                       |
| 强一致校验     | 不能依赖 Elasticsearch 做金额、库存、权限等强一致判断        |
| 权限体系设计   | 权限判断应在业务层完成，搜索层只接收已授权的查询条件         |
| 集群运维治理   | 分片规划、冷热分层、ILM、快照备份属于 Elasticsearch 运维范畴 |
| 数据最终解释   | 当主库和 Elasticsearch 数据不一致时，以业务主库为准          |

推荐设计原则：

1. 主库负责写入和强一致数据。
2. Elasticsearch 负责搜索和读优化。
3. 搜索文档可以通过业务事件、消息队列、定时任务或 CDC 同步。
4. 需要提供补偿同步和重建索引能力。
5. 所有搜索接口返回的数据应明确是否允许最终一致。

## 环境与依赖

本节定义 Spring Boot 3 项目接入 Spring Data Elasticsearch 所需的版本、依赖和连接配置。后续索引模型、Repository、ElasticsearchOperations 和查询接口均基于本节配置展开。

### Spring Boot 3 版本要求

Spring Boot 3 项目要求使用 Java 17 及以上版本。Spring Boot 3.5.x 仍属于 Spring Boot 3 技术栈，适合与 Spring Framework 6.2.x、Spring Data Elasticsearch 5.5.x 搭配使用。Spring Boot 官方系统要求说明，Spring Boot 3.x 至少需要 Java 17，并支持 Maven 3.6.3 或更高版本。([Spring Enterprise Docs](https://docs.enterprise.spring.io/spring-boot/system-requirements.html?utm_source=chatgpt.com))

推荐环境如下：

| 组件                      | 推荐版本 | 说明                                                      |
| ------------------------- | -------- | --------------------------------------------------------- |
| JDK                       | 17 或 21 | Spring Boot 3 最低要求 Java 17，生产环境建议使用 LTS 版本 |
| Spring Boot               | 3.5.x    | Spring Boot 3 当前稳定技术栈                              |
| Spring Framework          | 6.2.x    | 由 Spring Boot 3.5.x 依赖管理托管                         |
| Spring Data Elasticsearch | 5.5.x    | 与 Spring Framework 6.2.x 匹配                            |
| Elasticsearch             | 8.18.x   | 与 Spring Data Elasticsearch 5.5.x 匹配                   |
| Maven                     | 3.6.3+   | Spring Boot 3 支持的 Maven 版本                           |

版本选择建议：

| 场景                       | 建议                                                         |
| -------------------------- | ------------------------------------------------------------ |
| 新项目                     | 优先使用 Spring Boot 3.5.x + Spring Data Elasticsearch 5.5.x + Elasticsearch 8.18.x |
| 已有 Boot 3.4 项目         | 可继续使用 Spring Data Elasticsearch 5.4.x                   |
| 已有 Boot 3.3 项目         | 可继续使用 Spring Data Elasticsearch 5.3.x                   |
| 准备使用 Elasticsearch 9.x | 不建议直接放入 Spring Boot 3 项目，需要评估 Spring Framework 7 / Spring Data Elasticsearch 6.x 兼容性 |

注意：Spring Data Elasticsearch 6.0.x 对应的是 Spring Framework 7.0.x 和 Elasticsearch 9.x，不应作为 Spring Boot 3 项目的默认选择。([Home](https://docs.spring.io/spring-data/elasticsearch/reference/elasticsearch/versions.html?utm_source=chatgpt.com))

### Elasticsearch 版本选择

Elasticsearch 版本需要与 Spring Data Elasticsearch 版本保持一致的代际关系。对于 Spring Boot 3.5.x 项目，推荐选择 Elasticsearch 8.18.x。

推荐版本组合如下：

| Spring Boot | Spring Framework | Spring Data Elasticsearch | Elasticsearch | 说明                 |
| ----------- | ---------------- | ------------------------- | ------------- | -------------------- |
| 3.5.x       | 6.2.x            | 5.5.x                     | 8.18.x        | 推荐组合             |
| 3.4.x       | 6.1.x            | 5.4.x                     | 8.15.x        | 旧项目维护可用       |
| 3.3.x       | 6.1.x            | 5.3.x                     | 8.13.x        | 旧项目维护可用       |
| 3.2.x       | 6.1.x            | 5.2.x                     | 8.11.x        | 不建议新项目优先选择 |

版本选择原则：

1. 使用 Spring Boot BOM 托管依赖版本。
2. 不手动指定 `spring-data-elasticsearch` 版本。
3. 不手动指定 `elasticsearch-java` 版本，除非已完成兼容性验证。
4. Elasticsearch Server 与 Elasticsearch Java Client 尽量保持同一大版本。
5. Spring Boot 3 项目默认不要直接连接 Elasticsearch 9.x。
6. 如果公司已有固定 Elasticsearch 集群版本，需要先核对 Spring Data Elasticsearch 官方版本矩阵。

### Maven 依赖配置

Spring Boot 项目推荐使用 `spring-boot-starter-parent` 管理依赖版本。这样可以避免 Spring Data Elasticsearch、Elasticsearch Java Client、Spring Framework 版本不一致的问题。

文件位置：`pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- 使用 Spring Boot Parent 统一管理 Spring、Spring Data、Elasticsearch Client 等依赖版本 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.5</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>spring-data-elasticsearch-demo</artifactId>
    <version>1.0.0</version>
    <name>spring-data-elasticsearch-demo</name>
    <description>Spring Boot 3 Spring Data Elasticsearch 示例项目</description>

    <properties>
        <!-- Spring Boot 3 最低要求 Java 17 -->
        <java.version>17</java.version>

        <!-- Hutool 用于字符串、集合、日期、JSON 等常用工具处理 -->
        <hutool.version>5.8.40</hutool.version>
    </properties>

    <dependencies>
        <!-- Web 接口能力，用于提供索引管理、文档写入、搜索查询等 REST API -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Data Elasticsearch 核心依赖，包含 Repository、ElasticsearchOperations、自动配置等能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
        </dependency>

        <!-- 参数校验能力，用于 Controller 入参校验、DTO 字段校验 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Actuator 健康检查能力，可用于检查应用状态和外部依赖状态 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Lombok 简化 Getter、Setter、Builder、日志对象等样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Hutool 常用工具类 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- 单元测试和集成测试基础依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot 打包插件，用于构建可执行 jar -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

如果项目不使用 `spring-boot-starter-parent`，可以使用 Spring Boot BOM 统一管理版本。

文件位置：`pom.xml`

```xml
<dependencyManagement>
    <dependencies>
        <!-- 使用 Spring Boot BOM 统一管理 Spring Boot、Spring Data、Elasticsearch Client 等依赖版本 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>3.5.5</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

依赖配置注意事项：

| 注意项                                          | 说明                                                         |
| ----------------------------------------------- | ------------------------------------------------------------ |
| 不建议手动指定 `spring-data-elasticsearch` 版本 | 交给 Spring Boot BOM 托管                                    |
| 不建议手动指定 `elasticsearch-java` 版本        | 避免客户端版本与 Spring Data 不兼容                          |
| 不建议引入旧版 `RestHighLevelClient`            | Spring Data Elasticsearch 5.x 已使用新的 Elasticsearch Java Client 体系 |
| 推荐保留 `spring-boot-starter-actuator`         | 便于健康检查和运行状态观测                                   |
| 推荐保留 `spring-boot-starter-validation`       | 搜索参数、写入参数需要做基础校验                             |

### 连接参数配置

Spring Boot 通过 `spring.elasticsearch.*` 配置 Elasticsearch 连接参数。常用配置包括 `spring.elasticsearch.uris`、`username`、`password`、`connection-timeout`、`socket-timeout` 和 `socket-keep-alive`。Spring Boot 官方配置清单中，`spring.elasticsearch.uris` 默认值为 `http://localhost:9200`，`spring.elasticsearch.socket-timeout` 默认值为 `30s`。([Home](https://docs.spring.io/spring-boot/3.4/appendix/application-properties/?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    # 应用名称，用于日志、监控和链路标识
    name: spring-data-elasticsearch-demo

  elasticsearch:
    # Elasticsearch 集群地址，单节点或多节点均可配置
    uris:
      - http://localhost:9200

    # Elasticsearch 用户名，本地关闭安全认证时可以删除
    username: ${ELASTICSEARCH_USERNAME:elastic}

    # Elasticsearch 密码，生产环境不要提交明文密码
    password: ${ELASTICSEARCH_PASSWORD:changeme}

    # 建立连接超时时间
    connection-timeout: 3s

    # 请求读写超时时间
    socket-timeout: 30s

    # 是否启用 socket keep alive
    socket-keep-alive: true

  data:
    elasticsearch:
      repositories:
        # 是否启用 Elasticsearch Repository 自动扫描
        enabled: true

management:
  endpoints:
    web:
      exposure:
        # 暴露健康检查和应用信息端点，生产环境应按安全规范收敛
        include: health,info
  endpoint:
    health:
      # 展示健康检查明细，便于本地和测试环境排查连接问题
      show-details: when_authorized

logging:
  level:
    # Spring Data Elasticsearch 执行日志
    org.springframework.data.elasticsearch: info

    # Elasticsearch Java Client 日志，排查问题时可临时调整为 debug
    co.elastic.clients.elasticsearch: warn
```

生产环境可以配置多个 Elasticsearch 节点，提高连接可用性。

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  elasticsearch:
    # 生产集群建议配置多个节点
    uris:
      - https://es-node-01.example.com:9200
      - https://es-node-02.example.com:9200
      - https://es-node-03.example.com:9200

    # 账号密码通过环境变量或配置中心注入
    username: ${ELASTICSEARCH_USERNAME}
    password: ${ELASTICSEARCH_PASSWORD}

    # 跨网络或复杂查询场景可适当调大超时时间
    connection-timeout: 5s
    socket-timeout: 60s
    socket-keep-alive: true
```

连接参数说明：

| 配置项                                           | 说明                   | 建议值                         |
| ------------------------------------------------ | ---------------------- | ------------------------------ |
| `spring.elasticsearch.uris`                      | Elasticsearch 节点地址 | 本地单节点，生产多节点         |
| `spring.elasticsearch.username`                  | 认证用户名             | 使用环境变量或配置中心         |
| `spring.elasticsearch.password`                  | 认证密码               | 禁止提交真实密码               |
| `spring.elasticsearch.connection-timeout`        | 建立连接超时时间       | `3s` 到 `5s`                   |
| `spring.elasticsearch.socket-timeout`            | 请求读写超时时间       | 普通查询 `30s`，复杂查询 `60s` |
| `spring.elasticsearch.socket-keep-alive`         | 是否保持连接活跃       | 生产环境建议开启               |
| `spring.data.elasticsearch.repositories.enabled` | 是否启用 Repository    | 使用 Repository 时保持 `true`  |

本地开发可以使用 Docker 启动 Elasticsearch 单节点。

```bash
docker run -d \
  --name elasticsearch-8 \
  -p 9200:9200 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  -e "ES_JAVA_OPTS=-Xms1g -Xmx1g" \
  docker.elastic.co/elasticsearch/elasticsearch:8.18.1
```

参数说明：

| 参数                           | 说明                             |
| ------------------------------ | -------------------------------- |
| `discovery.type=single-node`   | 使用单节点模式启动               |
| `xpack.security.enabled=false` | 关闭安全认证，仅建议本地开发使用 |
| `ES_JAVA_OPTS=-Xms1g -Xmx1g`   | 限制 JVM 堆内存                  |
| `-p 9200:9200`                 | 将容器端口映射到本机             |

启动后执行以下命令验证 Elasticsearch 是否可访问：

```bash
curl http://localhost:9200
```

如果返回集群名称、版本号、节点信息等 JSON 内容，说明 Elasticsearch 已启动成功。随后启动 Spring Boot 项目，确认应用日志中没有 Elasticsearch 连接异常，再继续开发索引模型、Repository 和查询功能。

## 索引与文档模型

本节用于定义 Elasticsearch 索引命名、文档实体、字段映射、日期字段和枚举字段的处理方式。Spring Data Elasticsearch 会通过 `MappingElasticsearchConverter` 将 Java 实体对象映射为 Elasticsearch 中的 JSON 文档，实体类上的 `@Document`、`@Id`、`@Field` 等注解会参与索引映射生成和文档读写。([Home](https://docs.spring.io/spring-data/elasticsearch/reference/elasticsearch/object-mapping.html?utm_source=chatgpt.com))

### 索引命名规范

索引名称需要稳定、清晰、可扩展。Elasticsearch 官方要求索引名称只能使用小写，不能包含空格、逗号、`\`、`/`、`*`、`?`、`"`、`<`、`>`、`|`、`#` 等字符，不能以 `-`、`_`、`+` 开头，也不能是 `.` 或 `..`，长度不能超过 255 字节。([Elastic](https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-create-index.html?utm_source=chatgpt.com))

推荐命名格式如下：

```text
业务域_文档类型_v版本号
```

示例：

| 索引名称              | 说明             |
| --------------------- | ---------------- |
| `article_document_v1` | 文章搜索文档索引 |
| `product_document_v1` | 商品搜索文档索引 |
| `order_search_v1`     | 订单搜索宽表索引 |
| `user_profile_v1`     | 用户画像搜索索引 |
| `operation_log_v1`    | 操作日志搜索索引 |

命名建议如下：

| 规则               | 说明                                         |
| ------------------ | -------------------------------------------- |
| 全部小写           | Elasticsearch 索引名称不允许大写             |
| 使用下划线分隔     | 便于阅读，例如 `article_document_v1`         |
| 保留版本号         | 映射变更时可以创建 `v2` 索引并重建数据       |
| 不使用业务环境前缀 | 环境隔离建议通过集群、命名空间或配置文件控制 |
| 不使用中文         | 避免字节长度、编码和运维脚本兼容问题         |
| 不直接使用表名     | 搜索文档通常是业务宽表，不一定等同于数据库表 |

推荐在 Java 中统一维护索引常量，避免实体、Repository、测试代码中重复写字符串。

文件位置：`src/main/java/io/github/atengk/elasticsearch/constant/EsIndexConstant.java`

```java
package io.github.atengk.elasticsearch.constant;

/**
 * Elasticsearch 索引常量
 *
 * @author Ateng
 * @since 2026-05-13
 */
public final class EsIndexConstant {

    /**
     * 文章搜索文档索引
     */
    public static final String ARTICLE_DOCUMENT = "article_document_v1";

    private EsIndexConstant() {
    }
}
```

使用常量后，实体类中的 `@Document` 可以直接引用索引名称，减少硬编码。

```java
@Document(indexName = EsIndexConstant.ARTICLE_DOCUMENT)
public class ArticleDocument {
}
```

### 文档实体定义

文档实体用于描述 Elasticsearch 中的 `_source` 结构。它和数据库实体可以相似，但不建议完全复用数据库实体。搜索文档通常会冗余部分展示字段、聚合字段、标签字段和状态字段，以减少查询时的关联成本。

下面示例定义文章搜索文档，适合支撑文章标题搜索、摘要搜索、分类筛选、标签筛选、状态过滤和发布时间排序。

文件位置：`src/main/java/io/github/atengk/elasticsearch/document/ArticleDocument.java`

```java
package io.github.atengk.elasticsearch.document;

import io.github.atengk.elasticsearch.constant.EsIndexConstant;
import io.github.atengk.elasticsearch.enums.ArticleStatusEnum;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章搜索文档
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Accessors(chain = true)
@Document(indexName = EsIndexConstant.ARTICLE_DOCUMENT, createIndex = true)
public class ArticleDocument {

    /**
     * 文档 ID，建议使用业务主键字符串
     */
    @Id
    private String id;

    /**
     * 文章标题，用于全文搜索
     */
    @Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard")
    private String title;

    /**
     * 文章摘要，用于全文搜索和结果展示
     */
    @Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard")
    private String summary;

    /**
     * 文章正文，用于全文搜索
     */
    @Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard")
    private String content;

    /**
     * 分类编码，用于精确筛选
     */
    @Field(type = FieldType.Keyword)
    private String categoryCode;

    /**
     * 标签列表，用于标签筛选
     */
    @Field(type = FieldType.Keyword)
    private List<String> tags;

    /**
     * 作者 ID，用于精确筛选
     */
    @Field(type = FieldType.Keyword)
    private String authorId;

    /**
     * 文章状态，用于精确筛选
     */
    @Field(type = FieldType.Keyword)
    private ArticleStatusEnum status;

    /**
     * 阅读数量，用于排序和范围查询
     */
    @Field(type = FieldType.Long)
    private Long viewCount;

    /**
     * 是否推荐
     */
    @Field(type = FieldType.Boolean)
    private Boolean recommend;

    /**
     * 发布时间
     */
    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    /**
     * 创建时间
     */
    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
```

实体设计建议如下：

| 字段类型 | 建议                                           |
| -------- | ---------------------------------------------- |
| ID 字段  | 使用 `String`，并通过 `@Id` 标记               |
| 搜索字段 | 使用 `Text`，例如标题、摘要、正文              |
| 筛选字段 | 使用 `Keyword`，例如状态、分类、标签、用户 ID  |
| 排序字段 | 使用数值类型或日期类型，例如发布时间、阅读数   |
| 时间字段 | 使用 `LocalDateTime`，并明确 `Date` 格式       |
| 枚举字段 | 使用 `Keyword` 存储枚举编码或枚举名称          |
| 冗余字段 | 可以保留用于前端展示的分类名称、作者昵称等字段 |

不建议在搜索文档中放入过大的非检索字段，例如完整富文本 HTML、大 JSON 扩展字段、复杂对象树、权限详情列表等。如果确实需要展示，可以只在搜索结果中返回摘要信息，再通过业务接口查询完整详情。

### 字段映射配置

字段映射决定 Elasticsearch 如何存储、分词、查询和排序字段。Spring Data Elasticsearch 的 `@Field` 注解可以声明字段类型、字段名称、分词器、日期格式、是否存储等映射属性。如果不指定字段类型，默认是 `FieldType.Auto`，由 Elasticsearch 动态推断映射；业务项目中不建议依赖动态映射。([Home](https://docs.spring.io/spring-data/elasticsearch/reference/elasticsearch/object-mapping.html?utm_source=chatgpt.com))

常用字段映射如下：

| Java 类型       | Elasticsearch 类型 | Spring Data 写法                   | 适用场景             |
| --------------- | ------------------ | ---------------------------------- | -------------------- |
| `String`        | `Text`             | `@Field(type = FieldType.Text)`    | 全文搜索字段         |
| `String`        | `Keyword`          | `@Field(type = FieldType.Keyword)` | 精确匹配、聚合、排序 |
| `Integer`       | `Integer`          | `@Field(type = FieldType.Integer)` | 数值筛选             |
| `Long`          | `Long`             | `@Field(type = FieldType.Long)`    | ID、计数、排序       |
| `Double`        | `Double`           | `@Field(type = FieldType.Double)`  | 评分、价格、指标     |
| `Boolean`       | `Boolean`          | `@Field(type = FieldType.Boolean)` | 状态开关             |
| `LocalDateTime` | `Date`             | `@Field(type = FieldType.Date)`    | 时间范围查询、排序   |
| `List<String>`  | `Keyword`          | `@Field(type = FieldType.Keyword)` | 标签、编码集合       |
| `Enum`          | `Keyword`          | `@Field(type = FieldType.Keyword)` | 枚举状态筛选         |

字段映射建议如下：

| 场景         | 建议                               |
| ------------ | ---------------------------------- |
| 需要分词搜索 | 使用 `Text`                        |
| 需要精确筛选 | 使用 `Keyword`                     |
| 需要排序     | 不要只使用 `Text` 字段排序         |
| 需要范围查询 | 使用数值类型或日期类型             |
| 需要标签筛选 | 使用 `Keyword` 数组                |
| 需要高亮     | 对 `Text` 字段做高亮               |
| 需要聚合统计 | 优先使用 `Keyword`、数值、日期字段 |
| 不确定类型   | 不建议使用动态映射，应显式声明     |

示例字段配置：

```java
/**
 * 标题，支持分词搜索
 */
@Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard")
private String title;

/**
 * 分类编码，支持精确筛选
 */
@Field(type = FieldType.Keyword)
private String categoryCode;

/**
 * 阅读数量，支持排序和范围查询
 */
@Field(type = FieldType.Long)
private Long viewCount;

/**
 * 发布时间，支持时间范围查询和排序
 */
@Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd HH:mm:ss")
private LocalDateTime publishTime;
```

字段映射注意事项：

1. `Text` 适合搜索，但不适合直接做精确匹配、排序和聚合。
2. `Keyword` 适合精确匹配、排序和聚合，但不分词。
3. 日期字段必须统一格式，避免写入和查询时格式不一致。
4. 金额字段如果需要精确计算，不建议只依赖 Elasticsearch，应以业务数据库为准。
5. 映射变更后，已有索引通常不能直接修改字段类型，推荐新建版本索引并重建数据。
6. 生产环境不建议长期依赖 `createIndex = true` 自动建索引，关键索引建议通过初始化脚本或运维流程创建。

### 日期与枚举字段处理

日期和枚举是搜索文档中最容易出现格式不一致的问题。日期字段建议统一使用 `LocalDateTime`，并在 `@Field` 中显式声明格式。Spring Data Elasticsearch 文档说明，日期字段可以通过 `format` 使用内置格式，也可以通过 `pattern` 指定自定义格式；自定义年份格式建议使用 `uuuu` 而不是 `yyyy`。([Home](https://docs.spring.io/spring-data/elasticsearch/reference/elasticsearch/object-mapping.html?utm_source=chatgpt.com))

日期字段推荐写法：

```java
/**
 * 发布时间
 */
@Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd HH:mm:ss")
private LocalDateTime publishTime;
```

枚举字段建议使用 `Keyword` 类型，存储稳定的枚举编码或枚举名称。对于业务状态字段，推荐定义明确的枚举类。

文件位置：`src/main/java/io/github/atengk/elasticsearch/enums/ArticleStatusEnum.java`

```java
package io.github.atengk.elasticsearch.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文章状态枚举
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Getter
@AllArgsConstructor
public enum ArticleStatusEnum {

    /**
     * 草稿
     */
    DRAFT("草稿"),

    /**
     * 已发布
     */
    PUBLISHED("已发布"),

    /**
     * 已下架
     */
    OFFLINE("已下架");

    /**
     * 状态描述
     */
    private final String description;
}
```

实体中使用枚举字段：

```java
/**
 * 文章状态，用于精确筛选
 */
@Field(type = FieldType.Keyword)
private ArticleStatusEnum status;
```

如果对外接口使用字符串传参，可以在 DTO 中接收 `String status`，Service 层再转换为枚举，避免 Controller 直接暴露枚举转换异常。

文件位置：`src/main/java/io/github/atengk/elasticsearch/dto/ArticleSearchParam.java`

```java
package io.github.atengk.elasticsearch.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章搜索参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class ArticleSearchParam {

    /**
     * 搜索关键词
     */
    private String keyword;

    /**
     * 分类编码
     */
    private String categoryCode;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 状态编码
     */
    private String status;

    /**
     * 是否推荐
     */
    private Boolean recommend;

    /**
     * 发布时间开始
     */
    private LocalDateTime publishStartTime;

    /**
     * 发布时间结束
     */
    private LocalDateTime publishEndTime;
}
```

日期与枚举处理建议：

| 类型     | 建议                                        |
| -------- | ------------------------------------------- |
| 日期写入 | 应用层统一使用 `LocalDateTime` 或 `Instant` |
| 日期格式 | 显式声明 `pattern`，避免动态格式            |
| 日期查询 | 查询参数和写入格式保持一致                  |
| 枚举存储 | 使用 `Keyword`                              |
| 枚举值   | 使用稳定编码，不建议频繁修改                |
| 枚举描述 | 描述字段只用于展示，不建议作为查询条件      |
| 状态筛选 | 优先使用枚举编码或枚举名称进行精确匹配      |

## Repository 开发

Repository 用于封装基础文档读写和简单查询。Spring Data Elasticsearch 支持通过 `ElasticsearchRepository` 快速声明 Repository 接口，并支持方法名派生查询、`@Query` 声明查询、分页排序和自定义 Repository 扩展。官方文档说明，Elasticsearch Repository 支持从方法名派生查询，也支持字符串查询、原生查询和基于 Criteria 的查询方式。([Home](https://docs.spring.io/spring-data/elasticsearch/reference/elasticsearch/repositories/elasticsearch-repository-queries.html?utm_source=chatgpt.com))

### ElasticsearchRepository 基础用法

`ElasticsearchRepository<T, ID>` 适合处理基础 CRUD、按 ID 查询、批量保存、批量删除等简单操作。实体类通过 `@Document` 指定索引后，Repository 可以直接操作对应索引。

文件位置：`src/main/java/io/github/atengk/elasticsearch/repository/ArticleRepository.java`

```java
package io.github.atengk.elasticsearch.repository;

import io.github.atengk.elasticsearch.document.ArticleDocument;
import io.github.atengk.elasticsearch.enums.ArticleStatusEnum;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * 文章搜索 Repository
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface ArticleRepository extends ElasticsearchRepository<ArticleDocument, String> {

    /**
     * 根据分类编码查询文章列表
     *
     * @param categoryCode 分类编码
     * @return 文章列表
     */
    List<ArticleDocument> findByCategoryCode(String categoryCode);

    /**
     * 根据文章状态查询文章列表
     *
     * @param status 文章状态
     * @return 文章列表
     */
    List<ArticleDocument> findByStatus(ArticleStatusEnum status);

    /**
     * 根据推荐状态查询文章列表
     *
     * @param recommend 是否推荐
     * @return 文章列表
     */
    List<ArticleDocument> findByRecommend(Boolean recommend);
}
```

基础保存示例可以放在 Service 中，不建议 Controller 直接调用 Repository。

文件位置：`src/main/java/io/github/atengk/elasticsearch/service/ArticleDocumentService.java`

```java
package io.github.atengk.elasticsearch.service;

import io.github.atengk.elasticsearch.document.ArticleDocument;

import java.util.List;
import java.util.Optional;

/**
 * 文章搜索文档服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface ArticleDocumentService {

    /**
     * 保存文章文档
     *
     * @param document 文章文档
     * @return 保存后的文章文档
     */
    ArticleDocument save(ArticleDocument document);

    /**
     * 批量保存文章文档
     *
     * @param documents 文章文档列表
     * @return 保存后的文章文档列表
     */
    List<ArticleDocument> saveBatch(List<ArticleDocument> documents);

    /**
     * 根据 ID 查询文章文档
     *
     * @param id 文档 ID
     * @return 文章文档
     */
    Optional<ArticleDocument> findById(String id);

    /**
     * 根据 ID 删除文章文档
     *
     * @param id 文档 ID
     */
    void deleteById(String id);
}
```

文件位置：`src/main/java/io/github/atengk/elasticsearch/service/impl/ArticleDocumentServiceImpl.java`

```java
package io.github.atengk.elasticsearch.service.impl;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.elasticsearch.document.ArticleDocument;
import io.github.atengk.elasticsearch.repository.ArticleRepository;
import io.github.atengk.elasticsearch.service.ArticleDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 文章搜索文档服务实现
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleDocumentServiceImpl implements ArticleDocumentService {

    private final ArticleRepository articleRepository;

    /**
     * 保存文章文档
     *
     * @param document 文章文档
     * @return 保存后的文章文档
     */
    @Override
    public ArticleDocument save(ArticleDocument document) {
        ArticleDocument savedDocument = articleRepository.save(document);
        log.info("文章文档保存成功，id={}", savedDocument.getId());
        return savedDocument;
    }

    /**
     * 批量保存文章文档
     *
     * @param documents 文章文档列表
     * @return 保存后的文章文档列表
     */
    @Override
    public List<ArticleDocument> saveBatch(List<ArticleDocument> documents) {
        if (CollUtil.isEmpty(documents)) {
            log.info("文章文档批量保存跳过，原因=文档列表为空");
            return List.of();
        }

        List<ArticleDocument> savedDocuments = CollUtil.newArrayList(articleRepository.saveAll(documents));
        log.info("文章文档批量保存成功，count={}", savedDocuments.size());
        return savedDocuments;
    }

    /**
     * 根据 ID 查询文章文档
     *
     * @param id 文档 ID
     * @return 文章文档
     */
    @Override
    public Optional<ArticleDocument> findById(String id) {
        return articleRepository.findById(id);
    }

    /**
     * 根据 ID 删除文章文档
     *
     * @param id 文档 ID
     */
    @Override
    public void deleteById(String id) {
        articleRepository.deleteById(id);
        log.info("文章文档删除成功，id={}", id);
    }
}
```

基础用法说明：

| 方法         | 说明                               |
| ------------ | ---------------------------------- |
| `save`       | 新增或更新单个文档                 |
| `saveAll`    | 批量新增或更新文档                 |
| `findById`   | 根据文档 ID 查询                   |
| `findAll`    | 查询全部文档，生产环境慎用         |
| `deleteById` | 根据文档 ID 删除                   |
| `deleteAll`  | 删除全部文档，生产环境禁止直接暴露 |

### 方法名派生查询

方法名派生查询适合简单条件查询。Spring Data Elasticsearch 会根据 Repository 方法名生成对应 Elasticsearch 查询。官方文档列出的支持关键字包括 `And`、`Or`、`Between`、`LessThan`、`GreaterThan`、`Like`、`StartingWith`、`EndingWith`、`Containing`、`In`、`NotIn`、`True`、`False`、`OrderBy`、`Exists`、`IsNull`、`IsNotNull` 等。([Home](https://docs.spring.io/spring-data/elasticsearch/reference/elasticsearch/repositories/elasticsearch-repository-queries.html?utm_source=chatgpt.com))

在 `ArticleRepository` 中继续增加派生查询方法。

文件位置：`src/main/java/io/github/atengk/elasticsearch/repository/ArticleRepository.java`

```java
package io.github.atengk.elasticsearch.repository;

import io.github.atengk.elasticsearch.document.ArticleDocument;
import io.github.atengk.elasticsearch.enums.ArticleStatusEnum;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 文章搜索 Repository
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface ArticleRepository extends ElasticsearchRepository<ArticleDocument, String>, ArticleRepositoryCustom {

    /**
     * 根据分类编码查询文章列表
     *
     * @param categoryCode 分类编码
     * @return 文章列表
     */
    List<ArticleDocument> findByCategoryCode(String categoryCode);

    /**
     * 根据文章状态查询文章列表
     *
     * @param status 文章状态
     * @return 文章列表
     */
    List<ArticleDocument> findByStatus(ArticleStatusEnum status);

    /**
     * 根据推荐状态查询文章列表
     *
     * @param recommend 是否推荐
     * @return 文章列表
     */
    List<ArticleDocument> findByRecommend(Boolean recommend);

    /**
     * 根据分类编码和状态查询文章列表
     *
     * @param categoryCode 分类编码
     * @param status       文章状态
     * @return 文章列表
     */
    List<ArticleDocument> findByCategoryCodeAndStatus(String categoryCode, ArticleStatusEnum status);

    /**
     * 根据标题模糊查询文章列表
     *
     * @param title 标题关键词
     * @return 文章列表
     */
    List<ArticleDocument> findByTitleContaining(String title);

    /**
     * 根据发布时间范围查询文章列表
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 文章列表
     */
    List<ArticleDocument> findByPublishTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询指定分类下的文章列表
     *
     * @param categoryCodes 分类编码集合
     * @return 文章列表
     */
    List<ArticleDocument> findByCategoryCodeIn(Collection<String> categoryCodes);

    /**
     * 查询已推荐文章并按发布时间倒序排序
     *
     * @return 文章列表
     */
    List<ArticleDocument> findByRecommendTrueOrderByPublishTimeDesc();
}
```

常用派生查询示例：

| 方法名                                      | 查询含义               |
| ------------------------------------------- | ---------------------- |
| `findByCategoryCode`                        | 按分类编码精确查询     |
| `findByStatus`                              | 按状态精确查询         |
| `findByCategoryCodeAndStatus`               | 分类和状态组合查询     |
| `findByTitleContaining`                     | 标题包含关键词         |
| `findByPublishTimeBetween`                  | 发布时间范围查询       |
| `findByCategoryCodeIn`                      | 分类编码集合查询       |
| `findByRecommendTrueOrderByPublishTimeDesc` | 推荐文章按发布时间倒序 |

方法名派生查询注意事项：

1. 方法名过长时可读性会下降，应改用 `@Query` 或自定义 Repository。
2. `Text` 字段的派生查询通常会走查询字符串逻辑，复杂全文检索建议使用 `ElasticsearchOperations`。
3. `In` 查询用于 `Keyword` 字段更清晰。
4. 范围查询字段应使用数值类型或日期类型。
5. 高亮、聚合、复杂 bool 查询不建议使用方法名派生查询实现。

### 分页与排序查询

Repository 方法可以通过 `Pageable` 和 `Sort` 支持分页与排序。Spring Data Elasticsearch 查询方法支持返回 `Page<T>`、`SearchHits<T>`、`SearchPage<T>` 等结果类型。对于普通业务接口，如果只需要文档内容，可以返回 `Page<ArticleDocument>`；如果需要命中分数、高亮信息等元数据，可以返回 `SearchHits<ArticleDocument>` 或 `SearchPage<ArticleDocument>`。([Home](https://docs.spring.io/spring-data/elasticsearch/reference/elasticsearch/repositories/elasticsearch-repository-queries.html?utm_source=chatgpt.com))

在 Repository 中增加分页方法。

文件位置：`src/main/java/io/github/atengk/elasticsearch/repository/ArticleRepository.java`

```java
package io.github.atengk.elasticsearch.repository;

import io.github.atengk.elasticsearch.document.ArticleDocument;
import io.github.atengk.elasticsearch.enums.ArticleStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 文章搜索 Repository
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface ArticleRepository extends ElasticsearchRepository<ArticleDocument, String>, ArticleRepositoryCustom {

    /**
     * 根据状态分页查询文章
     *
     * @param status   文章状态
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<ArticleDocument> findByStatus(ArticleStatusEnum status, Pageable pageable);

    /**
     * 根据分类编码和状态分页查询文章
     *
     * @param categoryCode 分类编码
     * @param status       文章状态
     * @param pageable     分页参数
     * @return 分页结果
     */
    Page<ArticleDocument> findByCategoryCodeAndStatus(String categoryCode, ArticleStatusEnum status, Pageable pageable);

    /**
     * 根据标题关键词分页查询文章
     *
     * @param title    标题关键词
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<ArticleDocument> findByTitleContaining(String title, Pageable pageable);
}
```

Service 中封装分页参数，避免 Controller 直接拼装 Repository 查询细节。

文件位置：`src/main/java/io/github/atengk/elasticsearch/service/ArticleSearchService.java`

```java
package io.github.atengk.elasticsearch.service;

import io.github.atengk.elasticsearch.document.ArticleDocument;
import io.github.atengk.elasticsearch.enums.ArticleStatusEnum;
import org.springframework.data.domain.Page;

/**
 * 文章搜索服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface ArticleSearchService {

    /**
     * 分页查询已发布文章
     *
     * @param page 页码，从 1 开始
     * @param size 每页数量
     * @return 分页结果
     */
    Page<ArticleDocument> pagePublished(Integer page, Integer size);

    /**
     * 按分类分页查询文章
     *
     * @param categoryCode 分类编码
     * @param status       文章状态
     * @param page         页码，从 1 开始
     * @param size         每页数量
     * @return 分页结果
     */
    Page<ArticleDocument> pageByCategory(String categoryCode, ArticleStatusEnum status, Integer page, Integer size);
}
```

文件位置：`src/main/java/io/github/atengk/elasticsearch/service/impl/ArticleSearchServiceImpl.java`

```java
package io.github.atengk.elasticsearch.service.impl;

import io.github.atengk.elasticsearch.document.ArticleDocument;
import io.github.atengk.elasticsearch.enums.ArticleStatusEnum;
import io.github.atengk.elasticsearch.repository.ArticleRepository;
import io.github.atengk.elasticsearch.service.ArticleSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * 文章搜索服务实现
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleSearchServiceImpl implements ArticleSearchService {

    private final ArticleRepository articleRepository;

    /**
     * 分页查询已发布文章
     *
     * @param page 页码，从 1 开始
     * @param size 每页数量
     * @return 分页结果
     */
    @Override
    public Page<ArticleDocument> pagePublished(Integer page, Integer size) {
        PageRequest pageRequest = buildPageRequest(page, size);
        Page<ArticleDocument> result = articleRepository.findByStatus(ArticleStatusEnum.PUBLISHED, pageRequest);
        log.info("分页查询已发布文章完成，page={}，size={}，total={}", page, size, result.getTotalElements());
        return result;
    }

    /**
     * 按分类分页查询文章
     *
     * @param categoryCode 分类编码
     * @param status       文章状态
     * @param page         页码，从 1 开始
     * @param size         每页数量
     * @return 分页结果
     */
    @Override
    public Page<ArticleDocument> pageByCategory(String categoryCode, ArticleStatusEnum status, Integer page, Integer size) {
        PageRequest pageRequest = buildPageRequest(page, size);
        Page<ArticleDocument> result = articleRepository.findByCategoryCodeAndStatus(categoryCode, status, pageRequest);
        log.info("按分类分页查询文章完成，categoryCode={}，status={}，page={}，size={}，total={}",
                categoryCode, status, page, size, result.getTotalElements());
        return result;
    }

    /**
     * 构建分页参数
     *
     * @param page 页码，从 1 开始
     * @param size 每页数量
     * @return 分页参数
     */
    private PageRequest buildPageRequest(Integer page, Integer size) {
        int pageNumber = page == null || page < 1 ? 0 : page - 1;
        int pageSize = size == null || size < 1 ? 10 : Math.min(size, 100);

        return PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "publishTime")
        );
    }
}
```

分页与排序建议：

| 项目     | 建议                                                         |
| -------- | ------------------------------------------------------------ |
| 页码处理 | 对外接口从 `1` 开始，内部 `PageRequest` 从 `0` 开始          |
| 每页大小 | 限制最大值，例如不超过 `100`                                 |
| 默认排序 | 使用业务稳定字段，例如 `publishTime`、`createTime`           |
| 深分页   | 避免使用过大的 `from + size`，大数据量场景考虑 `search_after` |
| 排序字段 | 必须是可排序字段，避免直接对 `Text` 字段排序                 |
| 总数统计 | 高并发场景注意总数统计成本                                   |

### 自定义 Repository 扩展

当方法名派生查询无法满足复杂查询、高亮、聚合、动态条件拼接等需求时，可以使用自定义 Repository 扩展。Spring Data 推荐使用 Repository Fragment 模式：先定义自定义接口，再提供以 `Impl` 结尾的实现类，最后让主 Repository 继承自定义接口。([Home](https://docs.spring.io/spring-data/relational/reference/data-commons/3.2/repositories/custom-implementations.html?utm_source=chatgpt.com))

推荐文件结构如下：

```text
src/main/java/io/github/atengk/elasticsearch
├── document
│   └── ArticleDocument.java
├── dto
│   └── ArticleSearchParam.java
├── enums
│   └── ArticleStatusEnum.java
└── repository
    ├── ArticleRepository.java
    ├── ArticleRepositoryCustom.java
    └── ArticleRepositoryCustomImpl.java
```

自定义 Repository 接口用于声明复杂查询方法。

文件位置：`src/main/java/io/github/atengk/elasticsearch/repository/ArticleRepositoryCustom.java`

```java
package io.github.atengk.elasticsearch.repository;

import io.github.atengk.elasticsearch.document.ArticleDocument;
import io.github.atengk.elasticsearch.dto.ArticleSearchParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 文章搜索自定义 Repository
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface ArticleRepositoryCustom {

    /**
     * 根据动态条件分页搜索文章
     *
     * @param param    搜索参数
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<ArticleDocument> searchPage(ArticleSearchParam param, Pageable pageable);
}
```

自定义 Repository 实现类中使用 `ElasticsearchOperations` 构造动态查询。该方式适合比方法名查询更复杂的场景。

文件位置：`src/main/java/io/github/atengk/elasticsearch/repository/ArticleRepositoryCustomImpl.java`

```java
package io.github.atengk.elasticsearch.repository;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.elasticsearch.document.ArticleDocument;
import io.github.atengk.elasticsearch.dto.ArticleSearchParam;
import io.github.atengk.elasticsearch.enums.ArticleStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Repository;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;

/**
 * 文章搜索自定义 Repository 实现
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ArticleRepositoryCustomImpl implements ArticleRepositoryCustom {

    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 根据动态条件分页搜索文章
     *
     * @param param    搜索参数
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Override
    @SuppressWarnings("unchecked")
    public Page<ArticleDocument> searchPage(ArticleSearchParam param, Pageable pageable) {
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        if (param != null && StrUtil.isNotBlank(param.getKeyword())) {
            boolQueryBuilder.must(query -> query.multiMatch(multiMatch -> multiMatch
                    .fields("title", "summary", "content")
                    .query(param.getKeyword())
            ));
        }

        if (param != null && StrUtil.isNotBlank(param.getCategoryCode())) {
            boolQueryBuilder.filter(query -> query.term(term -> term
                    .field("categoryCode")
                    .value(param.getCategoryCode())
            ));
        }

        if (param != null && CollUtil.isNotEmpty(param.getTags())) {
            boolQueryBuilder.filter(query -> query.terms(terms -> terms
                    .field("tags")
                    .terms(value -> value.value(
                            param.getTags().stream()
                                    .map(co.elastic.clients.elasticsearch._types.FieldValue::of)
                                    .toList()
                    ))
            ));
        }

        if (param != null && StrUtil.isNotBlank(param.getStatus())) {
            ArticleStatusEnum status = ArticleStatusEnum.valueOf(param.getStatus());
            boolQueryBuilder.filter(query -> query.term(term -> term
                    .field("status")
                    .value(status.name())
            ));
        }

        if (param != null && param.getRecommend() != null) {
            boolQueryBuilder.filter(query -> query.term(term -> term
                    .field("recommend")
                    .value(param.getRecommend())
            ));
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQueryBuilder.build()))
                .withPageable(pageable)
                .build();

        SearchHits<ArticleDocument> searchHits = elasticsearchOperations.search(query, ArticleDocument.class);
        Page<ArticleDocument> result = (Page<ArticleDocument>) SearchHitSupport.searchPageFor(searchHits, pageable);

        log.info("文章动态分页搜索完成，keyword={}，categoryCode={}，page={}，size={}，total={}",
                param == null ? null : param.getKeyword(),
                param == null ? null : param.getCategoryCode(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                result.getTotalElements());

        return result;
    }
}
```

主 Repository 继承自定义接口后，业务层可以同时使用基础 CRUD、派生查询和自定义查询。

文件位置：`src/main/java/io/github/atengk/elasticsearch/repository/ArticleRepository.java`

```java
package io.github.atengk.elasticsearch.repository;

import io.github.atengk.elasticsearch.document.ArticleDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 文章搜索 Repository
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface ArticleRepository extends ElasticsearchRepository<ArticleDocument, String>, ArticleRepositoryCustom {
}
```

Service 中调用自定义查询。

文件位置：`src/main/java/io/github/atengk/elasticsearch/service/impl/ArticleSearchServiceImpl.java`

```java
package io.github.atengk.elasticsearch.service.impl;

import io.github.atengk.elasticsearch.document.ArticleDocument;
import io.github.atengk.elasticsearch.dto.ArticleSearchParam;
import io.github.atengk.elasticsearch.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * 文章搜索服务实现
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleSearchServiceImpl {

    private final ArticleRepository articleRepository;

    /**
     * 动态分页搜索文章
     *
     * @param param 搜索参数
     * @param page  页码，从 1 开始
     * @param size  每页数量
     * @return 分页结果
     */
    public Page<ArticleDocument> searchPage(ArticleSearchParam param, Integer page, Integer size) {
        int pageNumber = page == null || page < 1 ? 0 : page - 1;
        int pageSize = size == null || size < 1 ? 10 : Math.min(size, 100);

        PageRequest pageRequest = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "publishTime")
        );

        Page<ArticleDocument> result = articleRepository.searchPage(param, pageRequest);
        log.info("文章搜索服务调用完成，page={}，size={}，total={}", page, size, result.getTotalElements());
        return result;
    }
}
```

自定义 Repository 适用场景：

| 场景           | 说明                                            |
| -------------- | ----------------------------------------------- |
| 动态条件查询   | 查询条件由用户输入决定，字段不固定              |
| 多字段全文搜索 | 一个关键词同时匹配标题、摘要、正文              |
| 复杂 bool 查询 | 同时包含 `must`、`filter`、`should`、`must_not` |
| 高亮查询       | 需要返回命中字段和高亮片段                      |
| 聚合统计       | 需要按分类、状态、标签等字段聚合                |
| 复杂排序       | 需要按相关性、时间、权重组合排序                |
| 深度定制       | 需要使用 Elasticsearch Java Client 的原生能力   |

Repository 开发建议：

1. 简单 CRUD 使用 `ElasticsearchRepository`。
2. 简单条件查询使用方法名派生查询。
3. 固定 JSON 查询可以使用 `@Query`。
4. 动态复杂查询使用自定义 Repository。
5. 高亮、聚合、复杂分页优先使用 `ElasticsearchOperations`。
6. 不要把复杂业务逻辑写进 Repository，实现类只负责查询构造和数据访问。
7. 参数校验、权限控制、默认值处理应放在 Service 层。



## ElasticsearchOperations 开发

`ElasticsearchOperations` 适合封装比 Repository 更灵活的索引、文档和查询操作。Spring Data Elasticsearch 将操作能力拆分为 `IndexOperations`、`DocumentOperations`、`SearchOperations` 等接口，其中 `ElasticsearchOperations` 组合了文档操作和搜索操作，并可以通过 `indexOps` 获取索引操作对象。官方文档也说明，`IndexOperations` 负责索引创建、删除、映射写入等操作，`DocumentOperations` 负责文档保存、更新、删除和批量写入，`SearchOperations` 负责查询检索。([Home](https://docs.spring.io/spring-data/elasticsearch/reference/elasticsearch/template.html?utm_source=chatgpt.com))

### 索引创建与删除

索引创建与删除建议封装在独立的 Service 中，避免 Controller 直接调用 `ElasticsearchOperations`。开发环境可以使用自动建索引，生产环境更建议通过初始化接口、运维脚本或发布流程显式创建索引。

文件位置：`src/main/java/io/github/atengk/elasticsearch/service/ArticleOperationsService.java`

```java
package io.github.atengk.elasticsearch.service;

import io.github.atengk.elasticsearch.document.ArticleDocument;
import io.github.atengk.elasticsearch.enums.ArticleStatusEnum;
import org.springframework.data.elasticsearch.core.IndexedObjectInformation;

import java.util.List;
import java.util.Map;

/**
 * 文章 Elasticsearch Operations 服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface ArticleOperationsService {

    /**
     * 创建文章索引
     *
     * @return 是否创建成功
     */
    boolean createIndex();

    /**
     * 删除文章索引
     *
     * @return 是否删除成功
     */
    boolean deleteIndex();

    /**
     * 刷新文章索引
     */
    void refreshIndex();

    /**
     * 保存文章文档
     *
     * @param document 文章文档
     * @return 保存后的文章文档
     */
    ArticleDocument save(ArticleDocument document);

    /**
     * 根据 ID 查询文章文档
     *
     * @param id 文档 ID
     * @return 文章文档
     */
    ArticleDocument getById(String id);

    /**
     * 更新文章标题和摘要
     *
     * @param id      文档 ID
     * @param title   文章标题
     * @param summary 文章摘要
     */
    void updateTitleAndSummary(String id, String title, String summary);

    /**
     * 根据 ID 删除文档
     *
     * @param id 文档 ID
     * @return 删除的文档 ID
     */
    String deleteById(String id);

    /**
     * 根据分类和状态查询文章
     *
     * @param categoryCode 分类编码
     * @param status       文章状态
     * @return 文章列表
     */
    List<ArticleDocument> searchByCategoryAndStatus(String categoryCode, ArticleStatusEnum status);

    /**
     * 批量保存文章文档
     *
     * @param documents 文章文档列表
     * @return 写入结果
     */
    List<IndexedObjectInformation> bulkSave(List<ArticleDocument> documents);

    /**
     * 批量更新阅读数量
     *
     * @param viewCountMap 文档 ID 和阅读数量映射
     */
    void bulkUpdateViewCount(Map<String, Long> viewCountMap);
}
```

下面的实现类集中封装索引创建、删除、刷新、单文档写入、局部更新、条件查询和批量操作。

文件位置：`src/main/java/io/github/atengk/elasticsearch/service/impl/ArticleOperationsServiceImpl.java`

```java
package io.github.atengk.elasticsearch.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.elasticsearch.constant.EsIndexConstant;
import io.github.atengk.elasticsearch.document.ArticleDocument;
import io.github.atengk.elasticsearch.enums.ArticleStatusEnum;
import io.github.atengk.elasticsearch.service.ArticleOperationsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.BulkFailureException;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.IndexedObjectInformation;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 文章 Elasticsearch Operations 服务实现
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleOperationsServiceImpl implements ArticleOperationsService {

    private static final IndexCoordinates ARTICLE_INDEX = IndexCoordinates.of(EsIndexConstant.ARTICLE_DOCUMENT);

    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 创建文章索引
     *
     * @return 是否创建成功
     */
    @Override
    public boolean createIndex() {
        IndexOperations indexOperations = elasticsearchOperations.indexOps(ArticleDocument.class);
        if (indexOperations.exists()) {
            log.info("文章索引已存在，index={}", EsIndexConstant.ARTICLE_DOCUMENT);
            return false;
        }

        boolean created = indexOperations.createWithMapping();
        log.info("文章索引创建完成，index={}，created={}", EsIndexConstant.ARTICLE_DOCUMENT, created);
        return created;
    }

    /**
     * 删除文章索引
     *
     * @return 是否删除成功
     */
    @Override
    public boolean deleteIndex() {
        IndexOperations indexOperations = elasticsearchOperations.indexOps(ArticleDocument.class);
        if (!indexOperations.exists()) {
            log.info("文章索引不存在，跳过删除，index={}", EsIndexConstant.ARTICLE_DOCUMENT);
            return false;
        }

        boolean deleted = indexOperations.delete();
        log.info("文章索引删除完成，index={}，deleted={}", EsIndexConstant.ARTICLE_DOCUMENT, deleted);
        return deleted;
    }

    /**
     * 刷新文章索引
     */
    @Override
    public void refreshIndex() {
        elasticsearchOperations.indexOps(ArticleDocument.class).refresh();
        log.info("文章索引刷新完成，index={}", EsIndexConstant.ARTICLE_DOCUMENT);
    }

    /**
     * 保存文章文档
     *
     * @param document 文章文档
     * @return 保存后的文章文档
     */
    @Override
    public ArticleDocument save(ArticleDocument document) {
        if (document == null || StrUtil.isBlank(document.getId())) {
            throw new IllegalArgumentException("文章文档和文档ID不能为空");
        }

        ArticleDocument savedDocument = elasticsearchOperations.save(document);
        log.info("文章文档保存完成，id={}", savedDocument.getId());
        return savedDocument;
    }

    /**
     * 根据 ID 查询文章文档
     *
     * @param id 文档 ID
     * @return 文章文档
     */
    @Override
    public ArticleDocument getById(String id) {
        if (StrUtil.isBlank(id)) {
            throw new IllegalArgumentException("文档ID不能为空");
        }

        ArticleDocument document = elasticsearchOperations.get(id, ArticleDocument.class);
        log.info("文章文档查询完成，id={}，exists={}", id, document != null);
        return document;
    }

    /**
     * 更新文章标题和摘要
     *
     * @param id      文档 ID
     * @param title   文章标题
     * @param summary 文章摘要
     */
    @Override
    public void updateTitleAndSummary(String id, String title, String summary) {
        if (StrUtil.isBlank(id)) {
            throw new IllegalArgumentException("文档ID不能为空");
        }

        Document document = Document.create();

        if (StrUtil.isNotBlank(title)) {
            document.put("title", title);
        }

        if (StrUtil.isNotBlank(summary)) {
            document.put("summary", summary);
        }

        if (document.isEmpty()) {
            log.info("文章文档局部更新跳过，原因=无更新字段，id={}", id);
            return;
        }

        UpdateQuery updateQuery = UpdateQuery.builder(id)
                .withDocument(document)
                .withDocAsUpsert(false)
                .build();

        elasticsearchOperations.update(updateQuery, ARTICLE_INDEX);
        log.info("文章文档局部更新完成，id={}，fields={}", id, document.keySet());
    }

    /**
     * 根据 ID 删除文档
     *
     * @param id 文档 ID
     * @return 删除的文档 ID
     */
    @Override
    public String deleteById(String id) {
        if (StrUtil.isBlank(id)) {
            throw new IllegalArgumentException("文档ID不能为空");
        }

        String deletedId = elasticsearchOperations.delete(id, ARTICLE_INDEX);
        log.info("文章文档删除完成，id={}", deletedId);
        return deletedId;
    }

    /**
     * 根据分类和状态查询文章
     *
     * @param categoryCode 分类编码
     * @param status       文章状态
     * @return 文章列表
     */
    @Override
    public List<ArticleDocument> searchByCategoryAndStatus(String categoryCode, ArticleStatusEnum status) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(bool -> bool
                        .filter(filter -> filter.term(term -> term
                                .field("categoryCode")
                                .value(categoryCode)
                        ))
                        .filter(filter -> filter.term(term -> term
                                .field("status")
                                .value(status.name())
                        ))
                ))
                .build();

        SearchHits<ArticleDocument> searchHits = elasticsearchOperations.search(query, ArticleDocument.class);
        List<ArticleDocument> documents = searchHits.stream()
                .map(SearchHit::getContent)
                .toList();

        log.info("文章条件查询完成，categoryCode={}，status={}，count={}", categoryCode, status, documents.size());
        return documents;
    }

    /**
     * 批量保存文章文档
     *
     * @param documents 文章文档列表
     * @return 写入结果
     */
    @Override
    public List<IndexedObjectInformation> bulkSave(List<ArticleDocument> documents) {
        if (CollUtil.isEmpty(documents)) {
            log.info("文章文档批量保存跳过，原因=文档列表为空");
            return List.of();
        }

        List<IndexQuery> indexQueries = documents.stream()
                .map(document -> new IndexQueryBuilder()
                        .withId(document.getId())
                        .withObject(document)
                        .build())
                .toList();

        try {
            List<IndexedObjectInformation> result = elasticsearchOperations.bulkIndex(indexQueries, ArticleDocument.class);
            elasticsearchOperations.indexOps(ArticleDocument.class).refresh();
            log.info("文章文档批量保存完成，count={}", result.size());
            return result;
        } catch (BulkFailureException ex) {
            log.error("文章文档批量保存失败，failedDocuments={}", ex.getFailedDocuments(), ex);
            throw ex;
        }
    }

    /**
     * 批量更新阅读数量
     *
     * @param viewCountMap 文档 ID 和阅读数量映射
     */
    @Override
    public void bulkUpdateViewCount(Map<String, Long> viewCountMap) {
        if (MapUtil.isEmpty(viewCountMap)) {
            log.info("文章阅读数量批量更新跳过，原因=更新数据为空");
            return;
        }

        List<UpdateQuery> updateQueries = viewCountMap.entrySet().stream()
                .map(entry -> {
                    Document document = Document.create();
                    document.put("viewCount", entry.getValue());

                    return UpdateQuery.builder(entry.getKey())
                            .withDocument(document)
                            .withDocAsUpsert(false)
                            .build();
                })
                .toList();

        try {
            elasticsearchOperations.bulkUpdate(updateQueries, ARTICLE_INDEX);
            elasticsearchOperations.indexOps(ArticleDocument.class).refresh();
            log.info("文章阅读数量批量更新完成，count={}", updateQueries.size());
        } catch (BulkFailureException ex) {
            log.error("文章阅读数量批量更新失败，failedDocuments={}", ex.getFailedDocuments(), ex);
            throw ex;
        }
    }
}
```

### 文档新增与更新

文档新增通常使用 `save`，它会根据文档 ID 执行新增或覆盖更新。局部更新建议使用 `UpdateQuery`，只提交需要变更的字段，避免把空字段覆盖到 Elasticsearch。Spring Data Elasticsearch 的 `DocumentOperations` 提供 `save`、`get`、`delete`、`update`、`bulkIndex`、`bulkUpdate` 等文档操作方法，其中 `bulkIndex` 用于批量保存或更新，`bulkUpdate` 用于批量更新。([Home](https://docs.spring.io/spring-data/elasticsearch/docs/current/api/org/springframework/data/elasticsearch/core/DocumentOperations.html?utm_source=chatgpt.com))

常用写入方式如下：

| 操作     | 推荐方法                                                   | 说明               |
| -------- | ---------------------------------------------------------- | ------------------ |
| 新增文档 | `elasticsearchOperations.save(document)`                   | 适合单条写入       |
| 覆盖更新 | `elasticsearchOperations.save(document)`                   | 文档 ID 相同会更新 |
| 局部更新 | `elasticsearchOperations.update(updateQuery, index)`       | 只更新指定字段     |
| 删除文档 | `elasticsearchOperations.delete(id, index)`                | 根据文档 ID 删除   |
| 批量写入 | `elasticsearchOperations.bulkIndex(indexQueries, clazz)`   | 适合批量同步       |
| 批量更新 | `elasticsearchOperations.bulkUpdate(updateQueries, index)` | 适合批量局部更新   |

开发建议：

1. 新增和全量更新可以使用 `save`。
2. 只更新标题、状态、阅读数等少数字段时，优先使用 `UpdateQuery`。
3. 批量同步数据时优先使用 `bulkIndex`，不要循环调用 `save`。
4. 批量更新失败时需要捕获 `BulkFailureException` 并记录失败文档。
5. 写入后如果测试环境需要立即查询到数据，可以调用 `refresh`；生产环境不要高频刷新索引。

### 条件查询实现

条件查询可以使用 `CriteriaQuery`、`StringQuery` 或 `NativeQuery`。官方文档说明，`CriteriaQuery` 适合用链式条件构造查询，`StringQuery` 适合直接使用 Elasticsearch JSON 查询字符串，`NativeQuery` 适合复杂查询、聚合或需要使用 Elasticsearch Java Client 原生查询 DSL 的场景。([Home](https://docs.spring.io/spring-data/elasticsearch/reference/elasticsearch/template.html?utm_source=chatgpt.com))

常用查询方式对比如下：

| 查询方式              | 适用场景                               | 建议                     |
| --------------------- | -------------------------------------- | ------------------------ |
| `CriteriaQuery`       | 简单字段条件、范围条件                 | 适合快速实现             |
| `StringQuery`         | 已有 Elasticsearch JSON 查询           | 可读性一般，慎用复杂拼接 |
| `NativeQuery`         | 复杂 bool 查询、多字段查询、聚合、高亮 | 推荐用于核心搜索能力     |
| Repository 方法名查询 | 简单固定查询                           | 适合 CRUD 辅助场景       |

条件查询建议优先放在 Service 或自定义 Repository 实现类中，Controller 只负责接收请求和返回结果，不直接拼装 Elasticsearch 查询条件。

### 批量操作实现

批量操作主要用于业务库数据同步、重建索引、批量修复字段、定时补偿同步等场景。`bulkIndex` 会执行批量保存或更新，`bulkUpdate` 会执行批量局部更新；官方 API 文档说明，批量操作失败时会抛出 `BulkFailureException`，其中包含失败文档信息。([Home](https://docs.spring.io/spring-data/elasticsearch/docs/current/api/org/springframework/data/elasticsearch/core/DocumentOperations.html?utm_source=chatgpt.com))

批量操作建议：

| 场景         | 推荐方式                                     |
| ------------ | -------------------------------------------- |
| 首次全量同步 | 分页从业务库读取，使用 `bulkIndex` 分批写入  |
| 定时补偿同步 | 查询最近变更数据，使用 `bulkIndex` 覆盖写入  |
| 局部字段修复 | 使用 `bulkUpdate` 更新指定字段               |
| 删除失效数据 | 根据业务 ID 批量调用删除，或使用 DeleteQuery |
| 重建索引     | 创建新版本索引，全量写入后切换别名           |

批量大小建议根据文档大小和集群能力调整，常见范围是每批 `500` 到 `2000` 条。文档字段较多、正文较长或集群压力较大时，应降低批次大小。

## 查询功能开发

本节封装业务搜索功能，包括精确查询、模糊查询、范围查询、组合条件查询和高亮查询。Spring Data Elasticsearch 的搜索结果会封装为 `SearchHit` 和 `SearchHits`，其中 `SearchHit` 可以获取文档内容、命中分数、排序值和高亮字段；这类返回结构适合处理搜索元数据和高亮结果。([Home](https://docs.spring.io/spring-data/elasticsearch/reference/elasticsearch/template.html?utm_source=chatgpt.com))

### 精确查询

精确查询通常用于 `Keyword`、枚举、布尔值、数字 ID 等字段，例如分类编码、状态、作者 ID、是否推荐等。对于这类字段，建议使用 `term` 查询，不要对 `Text` 字段做精确查询。

文件位置：`src/main/java/io/github/atengk/elasticsearch/service/ArticleQueryService.java`

```java
package io.github.atengk.elasticsearch.service;

import io.github.atengk.elasticsearch.document.ArticleDocument;
import io.github.atengk.elasticsearch.dto.ArticleSearchParam;
import io.github.atengk.elasticsearch.vo.ArticleHighlightVO;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章查询服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface ArticleQueryService {

    /**
     * 根据分类编码精确查询文章
     *
     * @param categoryCode 分类编码
     * @return 文章列表
     */
    List<ArticleDocument> exactQueryByCategory(String categoryCode);

    /**
     * 根据关键词模糊查询文章
     *
     * @param keyword 关键词
     * @return 文章列表
     */
    List<ArticleDocument> fuzzyQuery(String keyword);

    /**
     * 根据发布时间范围查询文章
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 文章列表
     */
    List<ArticleDocument> rangeQueryByPublishTime(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 组合条件分页查询文章
     *
     * @param param 搜索参数
     * @param page  页码，从 1 开始
     * @param size  每页数量
     * @return 分页结果
     */
    Page<ArticleDocument> boolQuery(ArticleSearchParam param, Integer page, Integer size);

    /**
     * 高亮查询文章
     *
     * @param keyword 关键词
     * @return 高亮结果
     */
    List<ArticleHighlightVO> highlightQuery(String keyword);
}
```

### 模糊查询

模糊查询适合标题、摘要、正文等 `Text` 字段。简单关键词检索可以使用 `match` 或 `multi_match`，如果需要容忍拼写错误，可以在 `match` 查询中设置 `fuzziness`。复杂查询场景建议使用 `NativeQuery`，因为它可以直接使用 Elasticsearch Java Client 的 Query DSL。([Home](https://docs.spring.io/spring-data/elasticsearch/reference/elasticsearch/template.html?utm_source=chatgpt.com))

### 范围查询

范围查询适合时间、价格、数量、评分等字段。日期范围字段必须与前文实体映射中的日期格式保持一致，避免写入格式和查询格式不匹配。简单范围查询可以使用 `CriteriaQuery`，代码可读性更好。

### 组合条件查询

组合条件查询通常使用 bool 查询实现。关键词一般放入 `must`，过滤条件一般放入 `filter`。`filter` 不参与相关性评分，适合状态、分类、标签、布尔值等精确筛选条件。Spring Data Elasticsearch 文档也说明，在 Criteria 中组合 AND/OR 条件时，AND 会转换为 must 条件，OR 会转换为 should 条件。([Home](https://docs.spring.io/spring-data/elasticsearch/reference/elasticsearch/template.html?utm_source=chatgpt.com))

下面的实现类包含精确查询、模糊查询、范围查询、组合查询和高亮查询。

文件位置：`src/main/java/io/github/atengk/elasticsearch/service/impl/ArticleQueryServiceImpl.java`

```java
package io.github.atengk.elasticsearch.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.elasticsearch.document.ArticleDocument;
import io.github.atengk.elasticsearch.dto.ArticleSearchParam;
import io.github.atengk.elasticsearch.enums.ArticleStatusEnum;
import io.github.atengk.elasticsearch.service.ArticleQueryService;
import io.github.atengk.elasticsearch.vo.ArticleHighlightVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 文章查询服务实现
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleQueryServiceImpl implements ArticleQueryService {

    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 根据分类编码精确查询文章
     *
     * @param categoryCode 分类编码
     * @return 文章列表
     */
    @Override
    public List<ArticleDocument> exactQueryByCategory(String categoryCode) {
        if (StrUtil.isBlank(categoryCode)) {
            return List.of();
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.term(term -> term
                        .field("categoryCode")
                        .value(categoryCode)
                ))
                .build();

        SearchHits<ArticleDocument> searchHits = elasticsearchOperations.search(query, ArticleDocument.class);
        List<ArticleDocument> documents = toDocumentList(searchHits);

        log.info("文章精确查询完成，categoryCode={}，count={}", categoryCode, documents.size());
        return documents;
    }

    /**
     * 根据关键词模糊查询文章
     *
     * @param keyword 关键词
     * @return 文章列表
     */
    @Override
    public List<ArticleDocument> fuzzyQuery(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return List.of();
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.multiMatch(multiMatch -> multiMatch
                        .fields("title", "summary", "content")
                        .query(keyword)
                        .fuzziness("AUTO")
                ))
                .build();

        SearchHits<ArticleDocument> searchHits = elasticsearchOperations.search(query, ArticleDocument.class);
        List<ArticleDocument> documents = toDocumentList(searchHits);

        log.info("文章模糊查询完成，keyword={}，count={}", keyword, documents.size());
        return documents;
    }

    /**
     * 根据发布时间范围查询文章
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 文章列表
     */
    @Override
    public List<ArticleDocument> rangeQueryByPublishTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null && endTime == null) {
            return List.of();
        }

        Criteria criteria = new Criteria("publishTime");

        if (startTime != null && endTime != null) {
            criteria = criteria.between(startTime, endTime);
        } else if (startTime != null) {
            criteria = criteria.greaterThanEqual(startTime);
        } else {
            criteria = criteria.lessThanEqual(endTime);
        }

        CriteriaQuery query = new CriteriaQuery(criteria);
        query.setPageable(PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "publishTime")));

        SearchHits<ArticleDocument> searchHits = elasticsearchOperations.search(query, ArticleDocument.class);
        List<ArticleDocument> documents = toDocumentList(searchHits);

        log.info("文章发布时间范围查询完成，startTime={}，endTime={}，count={}", startTime, endTime, documents.size());
        return documents;
    }

    /**
     * 组合条件分页查询文章
     *
     * @param param 搜索参数
     * @param page  页码，从 1 开始
     * @param size  每页数量
     * @return 分页结果
     */
    @Override
    public Page<ArticleDocument> boolQuery(ArticleSearchParam param, Integer page, Integer size) {
        PageRequest pageRequest = buildPageRequest(page, size);

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(bool -> {
                    if (param != null && StrUtil.isNotBlank(param.getKeyword())) {
                        bool.must(must -> must.multiMatch(multiMatch -> multiMatch
                                .fields("title", "summary", "content")
                                .query(param.getKeyword())
                        ));
                    }

                    if (param != null && StrUtil.isNotBlank(param.getCategoryCode())) {
                        bool.filter(filter -> filter.term(term -> term
                                .field("categoryCode")
                                .value(param.getCategoryCode())
                        ));
                    }

                    if (param != null && StrUtil.isNotBlank(param.getStatus())) {
                        ArticleStatusEnum status = ArticleStatusEnum.valueOf(param.getStatus());
                        bool.filter(filter -> filter.term(term -> term
                                .field("status")
                                .value(status.name())
                        ));
                    }

                    if (param != null && param.getRecommend() != null) {
                        bool.filter(filter -> filter.term(term -> term
                                .field("recommend")
                                .value(param.getRecommend())
                        ));
                    }

                    if (param != null && CollUtil.isNotEmpty(param.getTags())) {
                        bool.filter(filter -> filter.terms(terms -> terms
                                .field("tags")
                                .terms(value -> value.value(param.getTags().stream()
                                        .map(co.elastic.clients.elasticsearch._types.FieldValue::of)
                                        .toList()))
                        ));
                    }

                    return bool;
                }))
                .withPageable(pageRequest)
                .build();

        SearchHits<ArticleDocument> searchHits = elasticsearchOperations.search(query, ArticleDocument.class);
        List<ArticleDocument> documents = toDocumentList(searchHits);

        Page<ArticleDocument> result = new PageImpl<>(documents, pageRequest, searchHits.getTotalHits());
        log.info("文章组合条件查询完成，page={}，size={}，total={}", page, size, result.getTotalElements());
        return result;
    }

    /**
     * 高亮查询文章
     *
     * @param keyword 关键词
     * @return 高亮结果
     */
    @Override
    public List<ArticleHighlightVO> highlightQuery(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return List.of();
        }

        HighlightParameters highlightParameters = HighlightParameters.builder()
                .withPreTags("<em>")
                .withPostTags("</em>")
                .withRequireFieldMatch(false)
                .withNumberOfFragments(1)
                .withFragmentSize(120)
                .build();

        Highlight highlight = new Highlight(
                highlightParameters,
                List.of(
                        new HighlightField("title"),
                        new HighlightField("summary"),
                        new HighlightField("content")
                )
        );

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.multiMatch(multiMatch -> multiMatch
                        .fields("title", "summary", "content")
                        .query(keyword)
                ))
                .withHighlightQuery(new HighlightQuery(highlight, ArticleDocument.class))
                .withPageable(PageRequest.of(0, 20))
                .build();

        SearchHits<ArticleDocument> searchHits = elasticsearchOperations.search(query, ArticleDocument.class);

        List<ArticleHighlightVO> result = searchHits.stream()
                .map(this::toHighlightVO)
                .toList();

        log.info("文章高亮查询完成，keyword={}，count={}", keyword, result.size());
        return result;
    }

    /**
     * 转换为文档列表
     *
     * @param searchHits 搜索命中结果
     * @return 文档列表
     */
    private List<ArticleDocument> toDocumentList(SearchHits<ArticleDocument> searchHits) {
        return searchHits.stream()
                .map(SearchHit::getContent)
                .toList();
    }

    /**
     * 转换为高亮结果
     *
     * @param searchHit 搜索命中结果
     * @return 高亮结果
     */
    private ArticleHighlightVO toHighlightVO(SearchHit<ArticleDocument> searchHit) {
        ArticleDocument document = searchHit.getContent();
        Map<String, List<String>> highlightFields = searchHit.getHighlightFields();

        return new ArticleHighlightVO()
                .setId(document.getId())
                .setTitle(getFirstHighlight(highlightFields, "title", document.getTitle()))
                .setSummary(getFirstHighlight(highlightFields, "summary", document.getSummary()))
                .setContent(getFirstHighlight(highlightFields, "content", document.getContent()))
                .setScore(searchHit.getScore())
                .setPublishTime(document.getPublishTime());
    }

    /**
     * 获取第一个高亮片段
     *
     * @param highlightFields 高亮字段
     * @param field           字段名称
     * @param defaultValue    默认值
     * @return 高亮片段
     */
    private String getFirstHighlight(Map<String, List<String>> highlightFields, String field, String defaultValue) {
        List<String> values = highlightFields.get(field);
        if (CollUtil.isEmpty(values)) {
            return defaultValue;
        }
        return values.get(0);
    }

    /**
     * 构建分页参数
     *
     * @param page 页码，从 1 开始
     * @param size 每页数量
     * @return 分页参数
     */
    private PageRequest buildPageRequest(Integer page, Integer size) {
        int pageNumber = page == null || page < 1 ? 0 : page - 1;
        int pageSize = size == null || size < 1 ? 10 : Math.min(size, 100);

        return PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "publishTime")
        );
    }
}
```

### 高亮查询

高亮查询用于搜索结果展示。Spring Data Elasticsearch 中，`HighlightQuery` 用于组合高亮定义和实体类型，`Highlight` 可以配置高亮字段，`HighlightParameters` 可以配置高亮标签、片段数量和片段长度；查询返回后，可以从 `SearchHit#getHighlightFields()` 或 `SearchHit#getHighlightField(field)` 获取高亮内容。([Home](https://docs.spring.io/spring-data/elasticsearch/reference/api/java/org/springframework/data/elasticsearch/core/query/HighlightQuery.html?utm_source=chatgpt.com))

高亮结果建议不要直接复用文档实体，推荐单独定义 VO。

文件位置：`src/main/java/io/github/atengk/elasticsearch/vo/ArticleHighlightVO.java`

```java
package io.github.atengk.elasticsearch.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 文章高亮查询结果
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Accessors(chain = true)
public class ArticleHighlightVO {

    /**
     * 文档 ID
     */
    private String id;

    /**
     * 高亮标题
     */
    private String title;

    /**
     * 高亮摘要
     */
    private String summary;

    /**
     * 高亮正文片段
     */
    private String content;

    /**
     * 命中分数
     */
    private Float score;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;
}
```

查询功能使用建议：

| 查询类型 | 推荐字段类型                     | 推荐查询                       |
| -------- | -------------------------------- | ------------------------------ |
| 精确查询 | `Keyword`、`Boolean`、数值、枚举 | `term`                         |
| 模糊查询 | `Text`                           | `match`、`multi_match`         |
| 范围查询 | `Date`、数值                     | `CriteriaQuery` 或 `range`     |
| 组合查询 | 多字段混合                       | `bool`                         |
| 高亮查询 | `Text`                           | `HighlightQuery` + `SearchHit` |

开发注意事项：

1. `Text` 字段适合全文搜索和高亮，不适合排序和精确筛选。
2. `Keyword` 字段适合精确查询、聚合和排序，不适合分词检索。
3. 组合查询中，筛选条件优先放到 `filter`，关键词查询放到 `must`。
4. 高亮结果需要从 `SearchHit` 中读取，不能只取 `getContent()`。
5. 分页查询需要限制 `size` 上限，避免单次查询结果过大。
6. 深分页场景不要长期依赖普通 `from + size`，大数据量检索应评估 `search_after` 或滚动查询。



## 数据同步设计

数据同步用于将业务主库中的数据同步到 Elasticsearch 搜索索引中。Elasticsearch 不作为业务主库使用，数据最终解释权应以 MySQL、PostgreSQL 等业务数据库为准，Elasticsearch 只承担搜索、筛选、排序和结果展示能力。

### 数据写入流程

数据写入流程用于处理业务数据新增后的索引写入。推荐在业务主库写入成功后，再同步写入 Elasticsearch，避免 Elasticsearch 写入成功但主库事务回滚导致脏索引数据。

推荐流程如下：

| 步骤 | 说明                                     |
| ---- | ---------------------------------------- |
| 1    | Controller 接收业务新增请求              |
| 2    | Service 校验参数并写入业务主库           |
| 3    | 主库事务提交成功                         |
| 4    | 组装 Elasticsearch 文档对象              |
| 5    | 写入 Elasticsearch 索引                  |
| 6    | 记录同步日志                             |
| 7    | 如果写入失败，记录失败任务，等待补偿同步 |

同步方式可以分为同步写入和异步写入。

| 同步方式 | 说明                                                   | 适用场景                         |
| -------- | ------------------------------------------------------ | -------------------------------- |
| 同步写入 | 主库写入成功后立即调用 Elasticsearch 写入              | 数据量小、实时性要求高           |
| 异步写入 | 主库写入成功后发送 MQ 消息，由消费者写入 Elasticsearch | 高并发、写入量大、允许短暂延迟   |
| 定时补偿 | 定时扫描主库变更数据，重新同步 Elasticsearch           | 防止消息丢失、修复历史不一致     |
| 全量重建 | 重新创建索引并全量写入主库数据                         | 索引映射变更、数据修复、版本升级 |

下面给出一个同步服务示例，负责将业务数据转换为 `ArticleDocument` 并写入 Elasticsearch。

文件位置：`src/main/java/io/github/atengk/elasticsearch/service/ArticleSyncService.java`

```java
package io.github.atengk.elasticsearch.service;

import io.github.atengk.elasticsearch.dto.ArticleSaveRequest;
import io.github.atengk.elasticsearch.dto.ArticleUpdateRequest;

/**
 * 文章索引同步服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface ArticleSyncService {

    /**
     * 同步新增文章文档
     *
     * @param request 文章新增请求
     * @return 文档 ID
     */
    String syncSave(ArticleSaveRequest request);

    /**
     * 同步更新文章文档
     *
     * @param request 文章更新请求
     */
    void syncUpdate(ArticleUpdateRequest request);

    /**
     * 同步删除文章文档
     *
     * @param id 文档 ID
     */
    void syncDelete(String id);
}
```

文件位置：`src/main/java/io/github/atengk/elasticsearch/service/impl/ArticleSyncServiceImpl.java`

```java
package io.github.atengk.elasticsearch.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.elasticsearch.document.ArticleDocument;
import io.github.atengk.elasticsearch.dto.ArticleSaveRequest;
import io.github.atengk.elasticsearch.dto.ArticleUpdateRequest;
import io.github.atengk.elasticsearch.enums.ArticleStatusEnum;
import io.github.atengk.elasticsearch.service.ArticleOperationsService;
import io.github.atengk.elasticsearch.service.ArticleSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 文章索引同步服务实现
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleSyncServiceImpl implements ArticleSyncService {

    private final ArticleOperationsService articleOperationsService;

    /**
     * 同步新增文章文档
     *
     * @param request 文章新增请求
     * @return 文档 ID
     */
    @Override
    public String syncSave(ArticleSaveRequest request) {
        checkSaveRequest(request);

        LocalDateTime now = LocalDateTime.now();
        ArticleDocument document = new ArticleDocument()
                .setId(request.getId())
                .setTitle(request.getTitle())
                .setSummary(request.getSummary())
                .setContent(request.getContent())
                .setCategoryCode(request.getCategoryCode())
                .setTags(CollUtil.emptyIfNull(request.getTags()))
                .setAuthorId(request.getAuthorId())
                .setStatus(parseStatus(request.getStatus()))
                .setViewCount(request.getViewCount() == null ? 0L : request.getViewCount())
                .setRecommend(Boolean.TRUE.equals(request.getRecommend()))
                .setPublishTime(request.getPublishTime())
                .setCreateTime(now)
                .setUpdateTime(now);

        ArticleDocument savedDocument = articleOperationsService.save(document);
        log.info("文章新增同步完成，id={}", savedDocument.getId());
        return savedDocument.getId();
    }

    /**
     * 同步更新文章文档
     *
     * @param request 文章更新请求
     */
    @Override
    public void syncUpdate(ArticleUpdateRequest request) {
        if (request == null || StrUtil.isBlank(request.getId())) {
            throw new IllegalArgumentException("文章更新请求和文档ID不能为空");
        }

        ArticleDocument oldDocument = articleOperationsService.getById(request.getId());
        if (oldDocument == null) {
            log.info("文章更新同步转为新增同步，原因=索引文档不存在，id={}", request.getId());
            ArticleSaveRequest saveRequest = new ArticleSaveRequest()
                    .setId(request.getId())
                    .setTitle(request.getTitle())
                    .setSummary(request.getSummary())
                    .setContent(request.getContent())
                    .setCategoryCode(request.getCategoryCode())
                    .setTags(request.getTags())
                    .setAuthorId(request.getAuthorId())
                    .setStatus(request.getStatus())
                    .setViewCount(request.getViewCount())
                    .setRecommend(request.getRecommend())
                    .setPublishTime(request.getPublishTime());
            syncSave(saveRequest);
            return;
        }

        ArticleDocument newDocument = oldDocument
                .setTitle(StrUtil.blankToDefault(request.getTitle(), oldDocument.getTitle()))
                .setSummary(StrUtil.blankToDefault(request.getSummary(), oldDocument.getSummary()))
                .setContent(StrUtil.blankToDefault(request.getContent(), oldDocument.getContent()))
                .setCategoryCode(StrUtil.blankToDefault(request.getCategoryCode(), oldDocument.getCategoryCode()))
                .setTags(CollUtil.isEmpty(request.getTags()) ? oldDocument.getTags() : request.getTags())
                .setAuthorId(StrUtil.blankToDefault(request.getAuthorId(), oldDocument.getAuthorId()))
                .setStatus(StrUtil.isBlank(request.getStatus()) ? oldDocument.getStatus() : parseStatus(request.getStatus()))
                .setViewCount(request.getViewCount() == null ? oldDocument.getViewCount() : request.getViewCount())
                .setRecommend(request.getRecommend() == null ? oldDocument.getRecommend() : request.getRecommend())
                .setPublishTime(request.getPublishTime() == null ? oldDocument.getPublishTime() : request.getPublishTime())
                .setUpdateTime(LocalDateTime.now());

        articleOperationsService.save(newDocument);
        log.info("文章更新同步完成，id={}", request.getId());
    }

    /**
     * 同步删除文章文档
     *
     * @param id 文档 ID
     */
    @Override
    public void syncDelete(String id) {
        if (StrUtil.isBlank(id)) {
            throw new IllegalArgumentException("文档ID不能为空");
        }

        articleOperationsService.deleteById(id);
        log.info("文章删除同步完成，id={}", id);
    }

    /**
     * 校验新增请求
     *
     * @param request 新增请求
     */
    private void checkSaveRequest(ArticleSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("文章新增请求不能为空");
        }
        if (StrUtil.isBlank(request.getId())) {
            throw new IllegalArgumentException("文档ID不能为空");
        }
        if (StrUtil.isBlank(request.getTitle())) {
            throw new IllegalArgumentException("文章标题不能为空");
        }
        if (StrUtil.isBlank(request.getStatus())) {
            throw new IllegalArgumentException("文章状态不能为空");
        }
    }

    /**
     * 转换文章状态
     *
     * @param status 状态编码
     * @return 文章状态
     */
    private ArticleStatusEnum parseStatus(String status) {
        try {
            return ArticleStatusEnum.valueOf(status);
        } catch (IllegalArgumentException ex) {
            log.error("文章状态转换失败，status={}", status, ex);
            throw new IllegalArgumentException("文章状态不合法：" + status);
        }
    }
}
```

### 数据更新流程

数据更新流程用于处理业务数据变更后的索引更新。更新同步可以分为全量覆盖更新和局部字段更新。

| 更新方式     | 说明                               | 适用场景                     |
| ------------ | ---------------------------------- | ---------------------------- |
| 全量覆盖更新 | 重新组装完整文档并调用 `save`      | 字段较少、业务逻辑简单       |
| 局部字段更新 | 只提交变更字段并调用 `UpdateQuery` | 阅读数、状态、标题等局部变更 |
| 删除后重建   | 删除旧文档后重新写入新文档         | 映射变化、数据结构变化       |

推荐流程如下：

| 步骤 | 说明                        |
| ---- | --------------------------- |
| 1    | 业务主库更新成功            |
| 2    | 获取最新业务数据            |
| 3    | 重新组装 Elasticsearch 文档 |
| 4    | 判断索引文档是否存在        |
| 5    | 存在则更新，不存在则补写    |
| 6    | 记录同步结果                |
| 7    | 失败时记录补偿任务          |

更新同步注意事项：

1. 不建议仅根据前端传参更新 Elasticsearch，应以主库最新数据组装文档。
2. 如果文档不存在，更新操作应具备补写能力。
3. 对于状态、分类、标签等筛选字段，更新后需要保证字段类型和旧映射一致。
4. 如果更新频率很高，例如阅读数、点赞数，可以考虑异步批量更新，避免频繁写入 Elasticsearch。

### 数据删除流程

数据删除流程用于处理业务数据删除、下架、禁用后的索引处理。业务上需要区分物理删除和逻辑删除。

| 删除类型 | Elasticsearch 处理方式     | 说明                         |
| -------- | -------------------------- | ---------------------------- |
| 物理删除 | 删除 Elasticsearch 文档    | 主库数据已删除，不再参与搜索 |
| 逻辑删除 | 更新状态字段               | 主库数据仍保留，但搜索时过滤 |
| 下架隐藏 | 更新状态为 `OFFLINE`       | 不删除文档，便于后台检索     |
| 批量删除 | 批量删除文档或批量更新状态 | 适合分类下架、批量封禁等场景 |

推荐流程如下：

| 步骤 | 说明                              |
| ---- | --------------------------------- |
| 1    | 业务主库删除或状态变更成功        |
| 2    | 判断业务删除类型                  |
| 3    | 物理删除时删除 Elasticsearch 文档 |
| 4    | 逻辑删除时更新状态字段            |
| 5    | 搜索接口默认过滤不可见状态        |
| 6    | 记录删除同步日志                  |
| 7    | 失败时记录补偿任务                |

删除策略建议：

1. 用户端搜索通常只查询 `PUBLISHED` 状态的数据。
2. 后台管理搜索可以查询 `DRAFT`、`PUBLISHED`、`OFFLINE` 等多种状态。
3. 物理删除要谨慎，删除后如果需要恢复，只能从主库重新同步。
4. 对于审核、风控、下架等场景，推荐逻辑删除或状态更新。

### 数据一致性处理

Elasticsearch 与业务主库之间通常采用最终一致性。主库事务提交成功后，Elasticsearch 同步可能因为网络异常、集群压力、字段映射错误、消息消费失败等原因失败，因此必须设计补偿机制。

常见一致性方案如下：

| 方案        | 说明                                               | 建议                       |
| ----------- | -------------------------------------------------- | -------------------------- |
| 同步双写    | 主库提交后同步写 Elasticsearch                     | 实现简单，但会增加接口耗时 |
| MQ 异步同步 | 主库提交后发送消息，消费者同步 Elasticsearch       | 推荐用于生产高并发场景     |
| 本地消息表  | 主库事务内写业务表和消息表，后台任务投递或执行同步 | 一致性更可控               |
| 定时补偿    | 定时扫描失败记录或最近变更数据重新同步             | 必须保留                   |
| 全量重建    | 从主库重新生成索引                                 | 用于索引版本升级和数据修复 |

推荐增加同步日志表，用于记录同步状态、失败原因和重试次数。

文件位置：`docs/sql/es_sync_log.sql`

```sql
-- Elasticsearch 同步日志表，用于记录业务数据到搜索索引的同步结果
CREATE TABLE es_sync_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    business_id VARCHAR(64) NOT NULL COMMENT '业务数据ID',
    index_name VARCHAR(128) NOT NULL COMMENT '索引名称',
    operation_type VARCHAR(32) NOT NULL COMMENT '操作类型：SAVE、UPDATE、DELETE',
    sync_status VARCHAR(32) NOT NULL COMMENT '同步状态：SUCCESS、FAILED、RETRYING',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    error_message VARCHAR(1024) DEFAULT NULL COMMENT '失败原因',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    KEY idx_business_id (business_id),
    KEY idx_sync_status (sync_status),
    KEY idx_create_time (create_time)
) COMMENT='Elasticsearch 同步日志表';
```

一致性处理建议：

| 问题                   | 处理方式                              |
| ---------------------- | ------------------------------------- |
| Elasticsearch 写入失败 | 记录失败日志，后台重试                |
| 消息重复消费           | 使用业务 ID 作为文档 ID，保证幂等覆盖 |
| 消息乱序               | 使用更新时间或版本号判断是否允许覆盖  |
| 索引文档不存在         | 更新操作转为新增补写                  |
| 主库删除但索引未删     | 定时扫描删除记录或状态字段进行补偿    |
| 字段映射错误           | 停止写入，修复映射后重建索引          |
| 大批量失败             | 降低批次大小，记录失败 ID 后分批重试  |

## 接口设计

接口设计用于为前端、后台管理系统或其他服务提供索引管理、文档写入、搜索查询和批量操作能力。生产环境中，索引创建、删除、重建等高危接口应限制为内部接口或后台管理接口，不能直接暴露给普通用户端。

### 索引管理接口

索引管理接口用于创建索引、删除索引和刷新索引。删除索引属于高危操作，生产环境建议增加权限控制、二次确认和操作审计。

文件位置：`src/main/java/io/github/atengk/elasticsearch/common/ApiResult.java`

```java
package io.github.atengk.elasticsearch.common;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 统一接口响应结果
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Accessors(chain = true)
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
     * @param <T>  数据类型
     * @return 响应结果
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<T>()
                .setCode(200)
                .setMessage("操作成功")
                .setData(data);
    }

    /**
     * 失败响应
     *
     * @param message 响应消息
     * @param <T>     数据类型
     * @return 响应结果
     */
    public static <T> ApiResult<T> fail(String message) {
        return new ApiResult<T>()
                .setCode(500)
                .setMessage(message)
                .setData(null);
    }
}
```

索引管理控制器放在 `admin` 路径下，便于后续接入权限控制。

文件位置：`src/main/java/io/github/atengk/elasticsearch/controller/ArticleIndexController.java`

```java
package io.github.atengk.elasticsearch.controller;

import io.github.atengk.elasticsearch.common.ApiResult;
import io.github.atengk.elasticsearch.service.ArticleOperationsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 文章索引管理接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/es/article/index")
public class ArticleIndexController {

    private final ArticleOperationsService articleOperationsService;

    /**
     * 创建文章索引
     *
     * @return 创建结果
     */
    @PostMapping("/create")
    public ApiResult<Boolean> createIndex() {
        boolean result = articleOperationsService.createIndex();
        log.info("接口调用完成：创建文章索引，result={}", result);
        return ApiResult.success(result);
    }

    /**
     * 删除文章索引
     *
     * @return 删除结果
     */
    @DeleteMapping("/delete")
    public ApiResult<Boolean> deleteIndex() {
        boolean result = articleOperationsService.deleteIndex();
        log.info("接口调用完成：删除文章索引，result={}", result);
        return ApiResult.success(result);
    }

    /**
     * 刷新文章索引
     *
     * @return 刷新结果
     */
    @PostMapping("/refresh")
    public ApiResult<Boolean> refreshIndex() {
        articleOperationsService.refreshIndex();
        log.info("接口调用完成：刷新文章索引");
        return ApiResult.success(true);
    }
}
```

接口说明：

| 接口                                  | 方法     | 说明         |
| ------------------------------------- | -------- | ------------ |
| `/api/admin/es/article/index/create`  | `POST`   | 创建文章索引 |
| `/api/admin/es/article/index/delete`  | `DELETE` | 删除文章索引 |
| `/api/admin/es/article/index/refresh` | `POST`   | 刷新文章索引 |

调用示例：

```bash
# 创建文章索引
curl -X POST http://localhost:8080/api/admin/es/article/index/create

# 刷新文章索引
curl -X POST http://localhost:8080/api/admin/es/article/index/refresh

# 删除文章索引
curl -X DELETE http://localhost:8080/api/admin/es/article/index/delete
```

### 文档写入接口

文档写入接口用于新增、更新、删除搜索文档。实际项目中，这类接口可以由业务系统内部调用，也可以由 MQ 消费者、定时任务或后台管理系统调用。

文件位置：`src/main/java/io/github/atengk/elasticsearch/dto/ArticleSaveRequest.java`

```java
package io.github.atengk.elasticsearch.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章新增请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Accessors(chain = true)
public class ArticleSaveRequest {

    /**
     * 文档 ID
     */
    @NotBlank(message = "文档ID不能为空")
    private String id;

    /**
     * 文章标题
     */
    @NotBlank(message = "文章标题不能为空")
    private String title;

    /**
     * 文章摘要
     */
    private String summary;

    /**
     * 文章正文
     */
    private String content;

    /**
     * 分类编码
     */
    private String categoryCode;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 作者 ID
     */
    private String authorId;

    /**
     * 文章状态
     */
    @NotBlank(message = "文章状态不能为空")
    private String status;

    /**
     * 阅读数量
     */
    private Long viewCount;

    /**
     * 是否推荐
     */
    private Boolean recommend;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;
}
```

文件位置：`src/main/java/io/github/atengk/elasticsearch/dto/ArticleUpdateRequest.java`

```java
package io.github.atengk.elasticsearch.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章更新请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Accessors(chain = true)
public class ArticleUpdateRequest {

    /**
     * 文档 ID
     */
    @NotBlank(message = "文档ID不能为空")
    private String id;

    /**
     * 文章标题
     */
    private String title;

    /**
     * 文章摘要
     */
    private String summary;

    /**
     * 文章正文
     */
    private String content;

    /**
     * 分类编码
     */
    private String categoryCode;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 作者 ID
     */
    private String authorId;

    /**
     * 文章状态
     */
    private String status;

    /**
     * 阅读数量
     */
    private Long viewCount;

    /**
     * 是否推荐
     */
    private Boolean recommend;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;
}
```

文件位置：`src/main/java/io/github/atengk/elasticsearch/controller/ArticleDocumentController.java`

```java
package io.github.atengk.elasticsearch.controller;

import io.github.atengk.elasticsearch.common.ApiResult;
import io.github.atengk.elasticsearch.dto.ArticleSaveRequest;
import io.github.atengk.elasticsearch.dto.ArticleUpdateRequest;
import io.github.atengk.elasticsearch.service.ArticleSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 文章文档写入接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/es/article/document")
public class ArticleDocumentController {

    private final ArticleSyncService articleSyncService;

    /**
     * 新增文章文档
     *
     * @param request 新增请求
     * @return 文档 ID
     */
    @PostMapping
    public ApiResult<String> save(@Valid @RequestBody ArticleSaveRequest request) {
        String id = articleSyncService.syncSave(request);
        log.info("接口调用完成：新增文章文档，id={}", id);
        return ApiResult.success(id);
    }

    /**
     * 更新文章文档
     *
     * @param request 更新请求
     * @return 更新结果
     */
    @PutMapping
    public ApiResult<Boolean> update(@Valid @RequestBody ArticleUpdateRequest request) {
        articleSyncService.syncUpdate(request);
        log.info("接口调用完成：更新文章文档，id={}", request.getId());
        return ApiResult.success(true);
    }

    /**
     * 删除文章文档
     *
     * @param id 文档 ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResult<Boolean> delete(@PathVariable String id) {
        articleSyncService.syncDelete(id);
        log.info("接口调用完成：删除文章文档，id={}", id);
        return ApiResult.success(true);
    }
}
```

调用示例：

```bash
curl -X POST http://localhost:8080/api/es/article/document \
  -H "Content-Type: application/json" \
  -d '{
    "id": "1001",
    "title": "Spring Boot 3 Elasticsearch 开发实践",
    "summary": "介绍 Spring Data Elasticsearch 的基础用法",
    "content": "本文主要介绍索引、文档、查询和同步设计。",
    "categoryCode": "springboot",
    "tags": ["SpringBoot", "Elasticsearch"],
    "authorId": "u1001",
    "status": "PUBLISHED",
    "viewCount": 0,
    "recommend": true,
    "publishTime": "2026-05-13T10:00:00"
  }'
curl -X PUT http://localhost:8080/api/es/article/document \
  -H "Content-Type: application/json" \
  -d '{
    "id": "1001",
    "title": "Spring Boot 3 Elasticsearch 开发文档",
    "summary": "更新后的文章摘要",
    "status": "PUBLISHED",
    "recommend": true
  }'
curl -X DELETE http://localhost:8080/api/es/article/document/1001
```

### 搜索查询接口

搜索查询接口用于对外提供关键词搜索、条件筛选、分页查询和高亮查询。用户端搜索接口应默认过滤不可见状态，例如只查询 `PUBLISHED` 状态。

文件位置：`src/main/java/io/github/atengk/elasticsearch/dto/ArticleSearchRequest.java`

```java
package io.github.atengk.elasticsearch.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章搜索请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class ArticleSearchRequest {

    /**
     * 搜索关键词
     */
    private String keyword;

    /**
     * 分类编码
     */
    private String categoryCode;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 文章状态
     */
    private String status;

    /**
     * 是否推荐
     */
    private Boolean recommend;

    /**
     * 发布时间开始
     */
    private LocalDateTime publishStartTime;

    /**
     * 发布时间结束
     */
    private LocalDateTime publishEndTime;

    /**
     * 页码，从 1 开始
     */
    private Integer page = 1;

    /**
     * 每页数量
     */
    private Integer size = 10;
}
```

文件位置：`src/main/java/io/github/atengk/elasticsearch/controller/ArticleSearchController.java`

```java
package io.github.atengk.elasticsearch.controller;

import cn.hutool.core.bean.BeanUtil;
import io.github.atengk.elasticsearch.common.ApiResult;
import io.github.atengk.elasticsearch.document.ArticleDocument;
import io.github.atengk.elasticsearch.dto.ArticleSearchParam;
import io.github.atengk.elasticsearch.dto.ArticleSearchRequest;
import io.github.atengk.elasticsearch.service.ArticleQueryService;
import io.github.atengk.elasticsearch.vo.ArticleHighlightVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章搜索查询接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/es/article/search")
public class ArticleSearchController {

    private final ArticleQueryService articleQueryService;

    /**
     * 根据分类精确查询文章
     *
     * @param categoryCode 分类编码
     * @return 文章列表
     */
    @GetMapping("/category/{categoryCode}")
    public ApiResult<List<ArticleDocument>> exactByCategory(@PathVariable String categoryCode) {
        List<ArticleDocument> result = articleQueryService.exactQueryByCategory(categoryCode);
        log.info("接口调用完成：分类精确查询文章，categoryCode={}，count={}", categoryCode, result.size());
        return ApiResult.success(result);
    }

    /**
     * 根据关键词模糊查询文章
     *
     * @param keyword 关键词
     * @return 文章列表
     */
    @GetMapping("/fuzzy")
    public ApiResult<List<ArticleDocument>> fuzzy(@RequestParam String keyword) {
        List<ArticleDocument> result = articleQueryService.fuzzyQuery(keyword);
        log.info("接口调用完成：关键词模糊查询文章，keyword={}，count={}", keyword, result.size());
        return ApiResult.success(result);
    }

    /**
     * 根据发布时间范围查询文章
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 文章列表
     */
    @GetMapping("/range")
    public ApiResult<List<ArticleDocument>> range(
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime) {
        List<ArticleDocument> result = articleQueryService.rangeQueryByPublishTime(startTime, endTime);
        log.info("接口调用完成：文章发布时间范围查询，count={}", result.size());
        return ApiResult.success(result);
    }

    /**
     * 组合条件分页查询文章
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @PostMapping("/page")
    public ApiResult<Page<ArticleDocument>> page(@RequestBody ArticleSearchRequest request) {
        ArticleSearchParam param = BeanUtil.copyProperties(request, ArticleSearchParam.class);
        Page<ArticleDocument> result = articleQueryService.boolQuery(param, request.getPage(), request.getSize());
        log.info("接口调用完成：组合条件分页查询文章，page={}，size={}，total={}",
                request.getPage(), request.getSize(), result.getTotalElements());
        return ApiResult.success(result);
    }

    /**
     * 高亮查询文章
     *
     * @param keyword 关键词
     * @return 高亮结果
     */
    @GetMapping("/highlight")
    public ApiResult<List<ArticleHighlightVO>> highlight(@RequestParam String keyword) {
        List<ArticleHighlightVO> result = articleQueryService.highlightQuery(keyword);
        log.info("接口调用完成：文章高亮查询，keyword={}，count={}", keyword, result.size());
        return ApiResult.success(result);
    }
}
```

接口说明：

| 接口                                             | 方法   | 说明             |
| ------------------------------------------------ | ------ | ---------------- |
| `/api/es/article/search/category/{categoryCode}` | `GET`  | 按分类精确查询   |
| `/api/es/article/search/fuzzy`                   | `GET`  | 关键词模糊查询   |
| `/api/es/article/search/range`                   | `GET`  | 发布时间范围查询 |
| `/api/es/article/search/page`                    | `POST` | 组合条件分页查询 |
| `/api/es/article/search/highlight`               | `GET`  | 高亮查询         |

调用示例：

```bash
curl "http://localhost:8080/api/es/article/search/fuzzy?keyword=Spring"
curl -X POST http://localhost:8080/api/es/article/search/page \
  -H "Content-Type: application/json" \
  -d '{
    "keyword": "Elasticsearch",
    "categoryCode": "springboot",
    "tags": ["SpringBoot"],
    "status": "PUBLISHED",
    "recommend": true,
    "page": 1,
    "size": 10
  }'
curl "http://localhost:8080/api/es/article/search/highlight?keyword=Elasticsearch"
```

### 批量操作接口

批量操作接口用于全量同步、批量写入、批量更新和数据修复。生产环境中，批量接口应限制调用权限，并控制每次请求的数据量，避免对 Elasticsearch 集群造成瞬时压力。

文件位置：`src/main/java/io/github/atengk/elasticsearch/dto/ArticleBatchSaveRequest.java`

```java
package io.github.atengk.elasticsearch.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 文章批量新增请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class ArticleBatchSaveRequest {

    /**
     * 文章文档列表
     */
    @Valid
    @NotEmpty(message = "文章文档列表不能为空")
    private List<ArticleSaveRequest> articles;
}
```

文件位置：`src/main/java/io/github/atengk/elasticsearch/dto/ArticleBatchUpdateViewCountRequest.java`

```java
package io.github.atengk.elasticsearch.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Map;

/**
 * 文章批量更新阅读数量请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class ArticleBatchUpdateViewCountRequest {

    /**
     * 文档 ID 和阅读数量映射
     */
    @NotEmpty(message = "阅读数量更新数据不能为空")
    private Map<String, Long> viewCountMap;
}
```

文件位置：`src/main/java/io/github/atengk/elasticsearch/controller/ArticleBatchController.java`

```java
package io.github.atengk.elasticsearch.controller;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.elasticsearch.common.ApiResult;
import io.github.atengk.elasticsearch.document.ArticleDocument;
import io.github.atengk.elasticsearch.dto.ArticleBatchSaveRequest;
import io.github.atengk.elasticsearch.dto.ArticleBatchUpdateViewCountRequest;
import io.github.atengk.elasticsearch.dto.ArticleSaveRequest;
import io.github.atengk.elasticsearch.enums.ArticleStatusEnum;
import io.github.atengk.elasticsearch.service.ArticleOperationsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.IndexedObjectInformation;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章批量操作接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/es/article/batch")
public class ArticleBatchController {

    private final ArticleOperationsService articleOperationsService;

    /**
     * 批量保存文章文档
     *
     * @param request 批量保存请求
     * @return 写入结果
     */
    @PostMapping("/save")
    public ApiResult<List<IndexedObjectInformation>> saveBatch(@Valid @RequestBody ArticleBatchSaveRequest request) {
        List<ArticleDocument> documents = request.getArticles().stream()
                .map(this::toDocument)
                .toList();

        List<IndexedObjectInformation> result = articleOperationsService.bulkSave(documents);
        log.info("接口调用完成：批量保存文章文档，count={}", result.size());
        return ApiResult.success(result);
    }

    /**
     * 批量更新文章阅读数量
     *
     * @param request 批量更新请求
     * @return 更新结果
     */
    @PutMapping("/view-count")
    public ApiResult<Boolean> updateViewCount(@Valid @RequestBody ArticleBatchUpdateViewCountRequest request) {
        articleOperationsService.bulkUpdateViewCount(request.getViewCountMap());
        log.info("接口调用完成：批量更新文章阅读数量，count={}", request.getViewCountMap().size());
        return ApiResult.success(true);
    }

    /**
     * 转换为文章文档
     *
     * @param request 新增请求
     * @return 文章文档
     */
    private ArticleDocument toDocument(ArticleSaveRequest request) {
        LocalDateTime now = LocalDateTime.now();

        return new ArticleDocument()
                .setId(request.getId())
                .setTitle(request.getTitle())
                .setSummary(request.getSummary())
                .setContent(request.getContent())
                .setCategoryCode(request.getCategoryCode())
                .setTags(CollUtil.emptyIfNull(request.getTags()))
                .setAuthorId(request.getAuthorId())
                .setStatus(ArticleStatusEnum.valueOf(request.getStatus()))
                .setViewCount(request.getViewCount() == null ? 0L : request.getViewCount())
                .setRecommend(Boolean.TRUE.equals(request.getRecommend()))
                .setPublishTime(request.getPublishTime())
                .setCreateTime(now)
                .setUpdateTime(now);
    }
}
```

接口说明：

| 接口                                     | 方法   | 说明             |
| ---------------------------------------- | ------ | ---------------- |
| `/api/admin/es/article/batch/save`       | `POST` | 批量保存文章文档 |
| `/api/admin/es/article/batch/view-count` | `PUT`  | 批量更新阅读数量 |

调用示例：

```bash
curl -X POST http://localhost:8080/api/admin/es/article/batch/save \
  -H "Content-Type: application/json" \
  -d '{
    "articles": [
      {
        "id": "1001",
        "title": "Spring Boot 3 Elasticsearch 开发实践",
        "summary": "文章摘要1",
        "content": "文章正文1",
        "categoryCode": "springboot",
        "tags": ["SpringBoot", "Elasticsearch"],
        "authorId": "u1001",
        "status": "PUBLISHED",
        "viewCount": 0,
        "recommend": true,
        "publishTime": "2026-05-13T10:00:00"
      },
      {
        "id": "1002",
        "title": "Spring Data Elasticsearch Repository 使用说明",
        "summary": "文章摘要2",
        "content": "文章正文2",
        "categoryCode": "springboot",
        "tags": ["SpringData", "Elasticsearch"],
        "authorId": "u1002",
        "status": "PUBLISHED",
        "viewCount": 0,
        "recommend": false,
        "publishTime": "2026-05-13T11:00:00"
      }
    ]
  }'
curl -X PUT http://localhost:8080/api/admin/es/article/batch/view-count \
  -H "Content-Type: application/json" \
  -d '{
    "viewCountMap": {
      "1001": 120,
      "1002": 85
    }
  }'
```

批量接口设计建议：

| 项目       | 建议                                                     |
| ---------- | -------------------------------------------------------- |
| 请求数据量 | 单次批量写入建议限制为 `500` 到 `2000` 条                |
| 接口权限   | 批量接口应仅开放给后台或内部服务                         |
| 异常处理   | 捕获批量失败信息并记录失败文档                           |
| 重试机制   | 批量失败后按失败 ID 分批重试                             |
| 集群保护   | 避免高峰期执行全量重建或大批量写入                       |
| 操作审计   | 记录调用人、批次数量、操作类型和结果                     |
| 数据来源   | 全量同步应以业务主库为来源，不应以前端传参作为最终数据源 |



## 异常处理与日志

异常处理用于统一封装 Elasticsearch 连接失败、查询失败、索引不存在、批量写入失败、参数校验失败等问题。日志用于记录关键索引操作、文档写入、查询条件、批量处理结果和异常堆栈，便于开发、测试和生产环境排查问题。

### 连接异常处理

连接异常通常发生在 Elasticsearch 未启动、地址配置错误、认证失败、网络不通、连接超时、读写超时等场景。Spring Boot 项目中，连接地址通常通过 `spring.elasticsearch.uris` 配置，常见超时参数包括 `connection-timeout` 和 `socket-timeout`。Spring Boot 官方配置清单中也提供了这些 Elasticsearch 客户端连接配置项。([Home](https://docs.spring.io/spring-data/elasticsearch/docs/5.5.x/api/org/springframework/data/elasticsearch/core/IndexOperations.html?utm_source=chatgpt.com))

连接异常建议统一捕获，不要把底层异常堆栈直接返回给前端。接口层返回简洁错误信息，日志中保留完整异常。

文件位置：`src/main/java/io/github/atengk/elasticsearch/exception/GlobalExceptionHandler.java`

```java
package io.github.atengk.elasticsearch.exception;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.elasticsearch.common.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.elasticsearch.BulkFailureException;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.data.elasticsearch.UncategorizedElasticsearchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Elasticsearch 连接异常处理
     *
     * @param ex 连接异常
     * @return 响应结果
     */
    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ApiResult<Boolean> handleDataAccessResourceFailureException(DataAccessResourceFailureException ex) {
        log.error("Elasticsearch连接异常，message={}，rootCause={}",
                ex.getMessage(), ExceptionUtil.getRootCauseMessage(ex), ex);
        return ApiResult.fail("Elasticsearch连接失败，请检查服务状态、连接地址、账号密码和网络配置");
    }

    /**
     * Elasticsearch 未分类异常处理
     *
     * @param ex 未分类异常
     * @return 响应结果
     */
    @ExceptionHandler(UncategorizedElasticsearchException.class)
    public ApiResult<Boolean> handleUncategorizedElasticsearchException(UncategorizedElasticsearchException ex) {
        log.error("Elasticsearch操作异常，message={}，rootCause={}",
                ex.getMessage(), ExceptionUtil.getRootCauseMessage(ex), ex);
        return ApiResult.fail("Elasticsearch操作失败，请查看服务端日志");
    }

    /**
     * Elasticsearch 索引不存在异常处理
     *
     * @param ex 索引不存在异常
     * @return 响应结果
     */
    @ExceptionHandler(NoSuchIndexException.class)
    public ApiResult<Boolean> handleNoSuchIndexException(NoSuchIndexException ex) {
        log.error("Elasticsearch索引不存在，message={}", ex.getMessage(), ex);
        return ApiResult.fail("Elasticsearch索引不存在，请先创建索引或检查索引名称");
    }

    /**
     * Elasticsearch 批量操作异常处理
     *
     * @param ex 批量操作异常
     * @return 响应结果
     */
    @ExceptionHandler(BulkFailureException.class)
    public ApiResult<Boolean> handleBulkFailureException(BulkFailureException ex) {
        log.error("Elasticsearch批量操作失败，failedDocuments={}，message={}",
                ex.getFailedDocuments(), ex.getMessage(), ex);
        return ApiResult.fail("Elasticsearch批量操作失败，请检查失败文档和字段映射");
    }

    /**
     * 请求参数校验异常处理
     *
     * @param ex 参数校验异常
     * @return 响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Boolean> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = Objects.requireNonNull(ex.getBindingResult().getFieldError()).getDefaultMessage();
        log.info("请求参数校验失败，message={}", message);
        return ApiResult.fail(message);
    }

    /**
     * 绑定参数异常处理
     *
     * @param ex 参数绑定异常
     * @return 响应结果
     */
    @ExceptionHandler(BindException.class)
    public ApiResult<Boolean> handleBindException(BindException ex) {
        String message = Objects.requireNonNull(ex.getBindingResult().getFieldError()).getDefaultMessage();
        log.info("请求参数绑定失败，message={}", message);
        return ApiResult.fail(message);
    }

    /**
     * JSON 请求体异常处理
     *
     * @param ex JSON 解析异常
     * @return 响应结果
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResult<Boolean> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.info("请求体解析失败，message={}", ex.getMessage());
        return ApiResult.fail("请求体格式错误，请检查 JSON 参数");
    }

    /**
     * 业务参数异常处理
     *
     * @param ex 参数异常
     * @return 响应结果
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResult<Boolean> handleIllegalArgumentException(IllegalArgumentException ex) {
        String message = StrUtil.blankToDefault(ex.getMessage(), "请求参数不合法");
        log.info("业务参数异常，message={}", message);
        return ApiResult.fail(message);
    }

    /**
     * 未知异常处理
     *
     * @param ex 未知异常
     * @return 响应结果
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Boolean> handleException(Exception ex) {
        log.error("系统未知异常，message={}，rootCause={}",
                ex.getMessage(), ExceptionUtil.getRootCauseMessage(ex), ex);
        return ApiResult.fail("系统异常，请稍后重试");
    }
}
```

连接异常排查建议：

| 问题                 | 排查方式                                       |
| -------------------- | ---------------------------------------------- |
| Elasticsearch 未启动 | 执行 `curl http://localhost:9200`              |
| 地址配置错误         | 检查 `spring.elasticsearch.uris`               |
| 账号密码错误         | 检查 `username`、`password` 或安全认证是否关闭 |
| 连接超时             | 检查网络、防火墙、容器端口映射                 |
| 读写超时             | 调整 `socket-timeout`，同时优化查询条件        |
| TLS 配置不一致       | 检查 HTTP/HTTPS、证书和客户端配置              |

### 查询异常处理

查询异常通常来自字段类型不匹配、查询字段不存在、日期格式错误、枚举值非法、DSL 构造错误、分页参数过大等问题。查询异常不应只依赖全局异常兜底，应在业务查询入口提前做参数校验。

建议在查询 Service 中增加参数保护，避免明显非法参数进入 Elasticsearch。

文件位置：`src/main/java/io/github/atengk/elasticsearch/util/EsQueryParamUtil.java`

```java
package io.github.atengk.elasticsearch.util;

import cn.hutool.core.util.StrUtil;

/**
 * Elasticsearch 查询参数工具类
 *
 * @author Ateng
 * @since 2026-05-13
 */
public final class EsQueryParamUtil {

    private static final int DEFAULT_PAGE = 1;

    private static final int DEFAULT_SIZE = 10;

    private static final int MAX_SIZE = 100;

    private EsQueryParamUtil() {
    }

    /**
     * 修正页码
     *
     * @param page 页码
     * @return 修正后的页码
     */
    public static int fixPage(Integer page) {
        if (page == null || page < 1) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    /**
     * 修正每页数量
     *
     * @param size 每页数量
     * @return 修正后的每页数量
     */
    public static int fixSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    /**
     * 校验关键词
     *
     * @param keyword 关键词
     */
    public static void checkKeyword(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            throw new IllegalArgumentException("搜索关键词不能为空");
        }

        if (keyword.length() > 100) {
            throw new IllegalArgumentException("搜索关键词长度不能超过100个字符");
        }
    }

    /**
     * 校验文档 ID
     *
     * @param id 文档 ID
     */
    public static void checkDocumentId(String id) {
        if (StrUtil.isBlank(id)) {
            throw new IllegalArgumentException("文档ID不能为空");
        }

        if (id.length() > 64) {
            throw new IllegalArgumentException("文档ID长度不能超过64个字符");
        }
    }
}
```

查询异常处理建议：

| 异常场景       | 处理方式                                  |
| -------------- | ----------------------------------------- |
| 关键词为空     | 在 Service 层直接返回空列表或抛出参数异常 |
| 页码小于 1     | 修正为第 1 页                             |
| 每页数量过大   | 限制最大值，例如 `100`                    |
| 枚举值非法     | 捕获并返回“状态不合法”                    |
| 日期范围错误   | 开始时间不能大于结束时间                  |
| 字段类型不匹配 | 检查实体 `@Field` 和索引 mapping          |
| 深分页过大     | 限制页码，必要时改用 `search_after`       |

### 索引不存在处理

索引不存在通常发生在本地环境首次启动、测试环境索引被删除、生产环境索引未初始化、索引版本变更后配置未同步等场景。Spring Data Elasticsearch 的 `IndexOperations` 提供 `exists()`、`createWithMapping()`、`delete()`、`refresh()` 等索引操作方法，可用于索引存在性判断、创建、删除和刷新。([Home](https://docs.spring.io/spring-data/elasticsearch/docs/5.5.x/api/org/springframework/data/elasticsearch/core/IndexOperations.html?utm_source=chatgpt.com))

建议将索引检查封装为独立组件，并在应用启动后自动检查关键索引是否存在。生产环境是否自动创建索引需要按团队规范决定；如果不允许自动创建，只记录错误并阻止关键功能调用。

文件位置：`src/main/java/io/github/atengk/elasticsearch/runner/EsIndexCheckRunner.java`

```java
package io.github.atengk.elasticsearch.runner;

import io.github.atengk.elasticsearch.constant.EsIndexConstant;
import io.github.atengk.elasticsearch.document.ArticleDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Elasticsearch 索引检查启动器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsIndexCheckRunner implements ApplicationRunner {

    private final ElasticsearchOperations elasticsearchOperations;

    private final Environment environment;

    /**
     * 应用启动后检查索引
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        IndexOperations indexOperations = elasticsearchOperations.indexOps(ArticleDocument.class);
        boolean exists = indexOperations.exists();

        if (exists) {
            log.info("Elasticsearch索引检查通过，index={}", EsIndexConstant.ARTICLE_DOCUMENT);
            return;
        }

        if (isProdProfile()) {
            log.error("Elasticsearch索引不存在，生产环境不自动创建，index={}", EsIndexConstant.ARTICLE_DOCUMENT);
            return;
        }

        boolean created = indexOperations.createWithMapping();
        log.info("Elasticsearch索引不存在，已在非生产环境自动创建，index={}，created={}",
                EsIndexConstant.ARTICLE_DOCUMENT, created);
    }

    /**
     * 判断是否生产环境
     *
     * @return 是否生产环境
     */
    private boolean isProdProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }
}
```

索引不存在处理策略：

| 环境  | 建议                                       |
| ----- | ------------------------------------------ |
| local | 可以自动创建索引                           |
| dev   | 可以自动创建索引，便于联调                 |
| test  | 可以通过接口或脚本创建索引                 |
| prod  | 不建议应用自动删除或自动重建索引           |
| prod  | 索引创建、删除、重建应走发布流程或运维流程 |

### 关键操作日志记录

关键操作日志用于定位数据同步、索引变更、批量写入、查询性能和异常问题。日志应覆盖关键业务信息，但不能输出敏感数据、完整大文本、认证信息和超长请求体。

推荐日志分类如下：

| 日志类型     | 记录内容                                   |
| ------------ | ------------------------------------------ |
| 索引操作日志 | 索引名称、操作类型、结果、耗时             |
| 文档写入日志 | 文档 ID、操作类型、写入结果                |
| 文档删除日志 | 文档 ID、删除结果                          |
| 查询日志     | 关键词、筛选条件、分页参数、命中数量、耗时 |
| 批量操作日志 | 批次大小、成功数量、失败数量、失败 ID      |
| 异常日志     | 异常类型、错误信息、根因、堆栈             |
| 补偿日志     | 业务 ID、重试次数、失败原因、最终状态      |

可以通过 AOP 统一记录接口耗时，避免每个 Controller 重复编写耗时代码。

文件位置：`src/main/java/io/github/atengk/elasticsearch/aspect/ApiLogAspect.java`

```java
package io.github.atengk.elasticsearch.aspect;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * 接口日志切面
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Aspect
@Component
public class ApiLogAspect {

    /**
     * 记录 Elasticsearch 接口耗时
     *
     * @param joinPoint 切点
     * @return 接口返回值
     * @throws Throwable 执行异常
     */
    @Around("execution(* io.github.atengk.elasticsearch.controller..*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String className = methodSignature.getDeclaringType().getSimpleName();
        String methodName = methodSignature.getMethod().getName();
        String apiName = StrUtil.format("{}.{}", className, methodName);

        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();
            log.info("Elasticsearch接口调用成功，api={}，cost={}ms", apiName, stopWatch.getTotalTimeMillis());
            return result;
        } catch (Throwable ex) {
            stopWatch.stop();
            log.error("Elasticsearch接口调用失败，api={}，cost={}ms，message={}",
                    apiName, stopWatch.getTotalTimeMillis(), ex.getMessage(), ex);
            throw ex;
        }
    }
}
```

如果使用 AOP，需要补充依赖。

文件位置：`pom.xml`

```xml
<!-- AOP 支持，用于统一记录接口耗时和关键调用日志 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

日志配置建议放在 `application.yml` 中，开发环境可以打开更多 Spring Data Elasticsearch 日志，生产环境保持 `info` 或 `warn`。

文件位置：`src/main/resources/application.yml`

```yaml
logging:
  level:
    # 当前项目业务日志
    io.github.atengk.elasticsearch: info

    # Spring Data Elasticsearch 日志
    org.springframework.data.elasticsearch: info

    # Elasticsearch Java Client 日志，排查问题时可临时改为 debug
    co.elastic.clients.elasticsearch: warn

  file:
    # 日志文件路径，容器环境可改为挂载目录
    name: logs/spring-data-elasticsearch-demo.log
```

日志记录注意事项：

1. 不要输出 Elasticsearch 账号、密码、Token、证书内容。
2. 不要输出完整正文、完整大字段、完整批量请求体。
3. 批量失败时记录失败文档 ID 和失败原因。
4. 查询日志需要记录关键词、分页参数、筛选条件和耗时。
5. 异常日志必须保留堆栈，接口响应不要直接返回堆栈。
6. 生产环境不要长期打开底层 Client 的 `debug` 日志。

## 功能验证

功能验证用于确认 Elasticsearch 服务、Spring Boot 应用、索引映射、文档写入和查询接口是否正常。验证顺序建议从底层到上层依次执行：先启动 Elasticsearch，再启动应用，然后验证索引映射、写入文档和查询结果。

### 本地 Elasticsearch 启动

本地开发可以使用 Docker 启动单节点 Elasticsearch。单节点配置通常使用 `discovery.type=single-node`；Elastic 官方文档也说明该配置用于单节点容器场景。([Elastic](https://www.elastic.co/guide/en/elasticsearch/reference/current/security-minimal-setup.html?utm_source=chatgpt.com))

本地开发环境可以关闭安全认证，便于快速联调。生产环境不要关闭安全认证。

```bash
# 删除旧容器，避免端口或容器名称冲突
docker rm -f elasticsearch-8

# 启动 Elasticsearch 8 单节点容器
docker run -d \
  --name elasticsearch-8 \
  -p 9200:9200 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  -e "ES_JAVA_OPTS=-Xms1g -Xmx1g" \
  docker.elastic.co/elasticsearch/elasticsearch:8.18.1

# 查看容器启动日志
docker logs -f elasticsearch-8
```

命令说明：

| 命令或参数                     | 说明                         |
| ------------------------------ | ---------------------------- |
| `docker rm -f elasticsearch-8` | 删除已有同名容器             |
| `--name elasticsearch-8`       | 指定容器名称                 |
| `-p 9200:9200`                 | 映射 Elasticsearch HTTP 端口 |
| `discovery.type=single-node`   | 使用单节点模式               |
| `xpack.security.enabled=false` | 关闭安全认证，仅用于本地开发 |
| `ES_JAVA_OPTS=-Xms1g -Xmx1g`   | 限制 JVM 堆内存              |

验证 Elasticsearch 是否启动成功：

```bash
curl http://localhost:9200
```

正常情况下会返回类似以下 JSON：

```json
{
  "name": "4d8a4c6c5d91",
  "cluster_name": "docker-cluster",
  "version": {
    "number": "8.18.1"
  },
  "tagline": "You Know, for Search"
}
```

启动 Spring Boot 应用：

```bash
mvn spring-boot:run
```

如果应用启动日志中出现 Elasticsearch 连接异常，优先检查以下内容：

| 检查项   | 说明                                                       |
| -------- | ---------------------------------------------------------- |
| 容器状态 | `docker ps` 是否存在 `elasticsearch-8`                     |
| 端口映射 | 本机 `9200` 是否已映射                                     |
| 应用配置 | `spring.elasticsearch.uris` 是否为 `http://localhost:9200` |
| 认证配置 | 本地关闭认证时，应用中不要配置错误账号密码                 |
| 网络访问 | 在应用所在机器执行 `curl http://localhost:9200`            |

### 索引映射验证

索引映射验证用于确认 `ArticleDocument` 上的 `@Document`、`@Field`、日期格式、字段类型是否正确生成。Elasticsearch 提供 `GET /{index}/_mapping` 接口用于获取索引映射定义。([Elastic](https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-get-mapping.html/?utm_source=chatgpt.com))

先调用索引创建接口：

```bash
curl -X POST http://localhost:8080/api/admin/es/article/index/create
```

刷新索引：

```bash
curl -X POST http://localhost:8080/api/admin/es/article/index/refresh
```

查看索引是否存在：

```bash
curl http://localhost:9200/_cat/indices?v
```

查看文章索引 mapping：

```bash
curl http://localhost:9200/article_document_v1/_mapping?pretty
```

重点检查以下字段类型：

| 字段           | 期望类型  |
| -------------- | --------- |
| `title`        | `text`    |
| `summary`      | `text`    |
| `content`      | `text`    |
| `categoryCode` | `keyword` |
| `tags`         | `keyword` |
| `authorId`     | `keyword` |
| `status`       | `keyword` |
| `viewCount`    | `long`    |
| `recommend`    | `boolean` |
| `publishTime`  | `date`    |
| `createTime`   | `date`    |
| `updateTime`   | `date`    |

如果 mapping 不符合预期，需要检查实体字段上的 `@Field` 配置。已有索引中的字段类型通常不能直接修改，建议删除本地索引后重新创建；生产环境应创建新版本索引并重建数据。

本地删除并重建索引：

```bash
# 删除文章索引
curl -X DELETE http://localhost:8080/api/admin/es/article/index/delete

# 重新创建文章索引
curl -X POST http://localhost:8080/api/admin/es/article/index/create

# 查看 mapping
curl http://localhost:9200/article_document_v1/_mapping?pretty
```

### 数据写入验证

数据写入验证用于确认文档新增、更新、删除、批量写入是否正常。Elasticsearch 是近实时搜索引擎，写入后通常需要等待 refresh 后才能被搜索到。Elasticsearch 官方说明，refresh 会让最近的索引操作对搜索可见，但显式 refresh 是同步且有资源成本的，生产环境应谨慎高频调用。([Elastic](https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-indices-refresh?utm_source=chatgpt.com))

新增单条文章文档：

```bash
curl -X POST http://localhost:8080/api/es/article/document \
  -H "Content-Type: application/json" \
  -d '{
    "id": "1001",
    "title": "Spring Boot 3 Elasticsearch 开发实践",
    "summary": "介绍 Spring Data Elasticsearch 的索引、文档和查询开发",
    "content": "本文用于验证 Spring Boot 3 集成 Spring Data Elasticsearch 的写入能力。",
    "categoryCode": "springboot",
    "tags": ["SpringBoot", "Elasticsearch"],
    "authorId": "u1001",
    "status": "PUBLISHED",
    "viewCount": 0,
    "recommend": true,
    "publishTime": "2026-05-13T10:00:00"
  }'
```

刷新索引后查询原始文档：

```bash
# 刷新索引，使刚写入的数据可搜索
curl -X POST http://localhost:9200/article_document_v1/_refresh

# 根据文档 ID 查看数据
curl http://localhost:9200/article_document_v1/_doc/1001?pretty
```

更新文章文档：

```bash
curl -X PUT http://localhost:8080/api/es/article/document \
  -H "Content-Type: application/json" \
  -d '{
    "id": "1001",
    "title": "Spring Boot 3 Elasticsearch 开发文档",
    "summary": "更新后的文章摘要",
    "status": "PUBLISHED",
    "recommend": true,
    "viewCount": 15
  }'
```

验证更新结果：

```bash
curl -X POST http://localhost:9200/article_document_v1/_refresh

curl http://localhost:9200/article_document_v1/_doc/1001?pretty
```

批量写入文章文档：

```bash
curl -X POST http://localhost:8080/api/admin/es/article/batch/save \
  -H "Content-Type: application/json" \
  -d '{
    "articles": [
      {
        "id": "1002",
        "title": "Spring Data Elasticsearch Repository 使用说明",
        "summary": "介绍 Repository 基础用法和派生查询",
        "content": "Repository 适合基础 CRUD、简单条件查询和分页排序。",
        "categoryCode": "springboot",
        "tags": ["SpringData", "Repository"],
        "authorId": "u1002",
        "status": "PUBLISHED",
        "viewCount": 3,
        "recommend": false,
        "publishTime": "2026-05-13T11:00:00"
      },
      {
        "id": "1003",
        "title": "ElasticsearchOperations 查询开发",
        "summary": "介绍 NativeQuery、CriteriaQuery 和高亮查询",
        "content": "ElasticsearchOperations 适合复杂查询、批量操作和索引管理。",
        "categoryCode": "springboot",
        "tags": ["ElasticsearchOperations", "Query"],
        "authorId": "u1003",
        "status": "PUBLISHED",
        "viewCount": 8,
        "recommend": true,
        "publishTime": "2026-05-13T12:00:00"
      }
    ]
  }'
```

删除文章文档：

```bash
curl -X DELETE http://localhost:8080/api/es/article/document/1003
```

验证删除结果：

```bash
curl -X POST http://localhost:9200/article_document_v1/_refresh

curl http://localhost:9200/article_document_v1/_doc/1003?pretty
```

数据写入验证标准：

| 验证项           | 预期结果                       |
| ---------------- | ------------------------------ |
| 新增接口         | 返回文档 ID                    |
| `_doc/{id}` 查询 | 能看到 `_source` 文档内容      |
| 更新接口         | 返回操作成功                   |
| 更新后查询       | 字段值已变化                   |
| 批量写入         | 返回批量写入结果               |
| 删除接口         | 返回操作成功                   |
| 删除后查询       | 文档不存在或返回 `found=false` |

### 查询结果验证

查询结果验证用于确认精确查询、模糊查询、范围查询、组合查询和高亮查询是否符合预期。验证前应确保测试数据已写入，并执行过 refresh 或等待 Elasticsearch 自动 refresh。

按分类精确查询：

```bash
curl http://localhost:8080/api/es/article/search/category/springboot
```

预期结果：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": "1001",
      "categoryCode": "springboot",
      "status": "PUBLISHED"
    }
  ]
}
```

关键词模糊查询：

```bash
curl "http://localhost:8080/api/es/article/search/fuzzy?keyword=Elasticsearch"
```

验证点：

| 验证项   | 说明                                           |
| -------- | ---------------------------------------------- |
| 返回数量 | 应返回标题、摘要或正文中命中的文章             |
| 命中字段 | `title`、`summary`、`content` 任一字段命中即可 |
| 空关键词 | 应返回参数错误或空列表，取决于 Service 设计    |

组合条件分页查询：

```bash
curl -X POST http://localhost:8080/api/es/article/search/page \
  -H "Content-Type: application/json" \
  -d '{
    "keyword": "Elasticsearch",
    "categoryCode": "springboot",
    "tags": ["SpringBoot"],
    "status": "PUBLISHED",
    "recommend": true,
    "page": 1,
    "size": 10
  }'
```

验证点：

| 验证项     | 说明                             |
| ---------- | -------------------------------- |
| 分页页码   | 接口入参从 `1` 开始              |
| 每页数量   | 返回数量不超过 `size`            |
| 分类过滤   | `categoryCode` 应为 `springboot` |
| 状态过滤   | `status` 应为 `PUBLISHED`        |
| 推荐过滤   | `recommend` 应为 `true`          |
| 关键词命中 | 标题、摘要或正文应包含相关内容   |

高亮查询：

```bash
curl "http://localhost:8080/api/es/article/search/highlight?keyword=Elasticsearch"
```

预期结果中应包含 `<em>` 标签：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": "1001",
      "title": "Spring Boot 3 <em>Elasticsearch</em> 开发文档",
      "summary": "介绍 Spring Data <em>Elasticsearch</em> 的索引、文档和查询开发",
      "score": 1.23
    }
  ]
}
```

也可以直接使用 Elasticsearch 原生 `_search` 接口验证索引数据：

```bash
curl -X POST http://localhost:9200/article_document_v1/_search?pretty \
  -H "Content-Type: application/json" \
  -d '{
    "query": {
      "multi_match": {
        "query": "Elasticsearch",
        "fields": ["title", "summary", "content"]
      }
    },
    "highlight": {
      "pre_tags": ["<em>"],
      "post_tags": ["</em>"],
      "fields": {
        "title": {},
        "summary": {},
        "content": {}
      }
    }
  }'
```

查询结果验证标准：

| 查询类型 | 验证标准                                  |
| -------- | ----------------------------------------- |
| 精确查询 | 只返回完全匹配筛选字段的数据              |
| 模糊查询 | 能返回分词命中的数据                      |
| 范围查询 | 返回结果在指定时间范围内                  |
| 组合查询 | 多个条件同时生效                          |
| 高亮查询 | 命中字段包含 `<em>` 和 `</em>`            |
| 分页查询 | 页码、每页数量、总数正确                  |
| 排序查询 | 结果顺序符合 `publishTime` 或指定排序字段 |

常见问题排查：

| 问题             | 原因                            | 处理方式                              |
| ---------------- | ------------------------------- | ------------------------------------- |
| 写入成功但查不到 | 索引未 refresh                  | 等待自动 refresh 或调用 `_refresh`    |
| 精确查询无结果   | 字段不是 `keyword`              | 检查 mapping                          |
| 高亮为空         | 查询字段未命中或字段不是 `text` | 检查查询字段和分词配置                |
| 日期范围查询失败 | 日期格式不一致                  | 检查 `@Field` 日期 pattern 和请求参数 |
| 排序报错         | 对 `text` 字段排序              | 改用 `keyword`、数值或日期字段        |
| 索引不存在       | 未创建索引或索引名错误          | 调用索引创建接口并检查索引常量        |
| 批量写入部分失败 | 字段类型不匹配或单条数据异常    | 查看 `BulkFailureException` 失败文档  |