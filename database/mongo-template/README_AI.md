# Spring Data MongoDB



## 技术概述

Spring Data MongoDB 是 Spring Data 体系中面向 MongoDB 的数据访问组件，主要用于在 Spring Boot 3 项目中简化 MongoDB 的连接配置、对象映射、文档操作、Repository 查询以及聚合查询开发。它将 MongoDB 的文档模型与 Java 对象模型进行映射，使业务代码可以通过 `MongoRepository` 或 `MongoTemplate` 完成常见的数据读写操作。

在 Spring Boot 3 项目中，Spring Data MongoDB 通常用于替代直接使用 MongoDB Java Driver 的低层 API。开发人员不需要手动维护大量连接、集合、文档转换和查询封装代码，而是可以基于 Spring Boot 自动配置能力快速接入 MongoDB，并结合实体类、Repository 接口和模板 API 完成业务开发。

### MongoDB 基本特性

MongoDB 是一种面向文档的 NoSQL 数据库，数据以 BSON 文档形式存储。与传统关系型数据库使用表、行、列组织数据不同，MongoDB 使用数据库、集合和文档组织数据。集合类似关系型数据库中的表，文档类似一条数据记录，但文档结构更加灵活，不要求同一个集合中的所有文档都拥有完全一致的字段结构。

MongoDB 的核心特点是文档模型灵活、天然支持嵌套结构、查询能力较强、扩展性好，适合处理结构变化频繁、读写吞吐较高或数据结构偏对象化的业务数据。

常见特性如下：

| 特性        | 说明                                                         |
| ----------- | ------------------------------------------------------------ |
| 文档存储    | 使用 BSON 文档保存数据，天然适合存储 JSON 风格的数据结构     |
| 动态 Schema | 同一集合中的文档字段可以不完全一致，适合需求变化较快的业务   |
| 嵌套结构    | 支持对象嵌套、数组字段、复杂文档结构，减少多表关联           |
| 丰富查询    | 支持精确查询、范围查询、模糊查询、数组查询、嵌套字段查询等   |
| 索引支持    | 支持普通索引、唯一索引、复合索引、TTL 索引、地理空间索引等   |
| 聚合能力    | 提供 Aggregation Pipeline，可完成分组、统计、过滤、排序、投影等操作 |
| 高可用      | 支持副本集机制，提高数据可用性和容灾能力                     |
| 水平扩展    | 支持分片集群，适合大数据量和高并发场景                       |

MongoDB 中一条典型的文档数据如下：

```json
{
  "_id": "665f2d4f91f3a65a9f2d1001",
  "username": "ateng",
  "nickname": "阿腾",
  "age": 28,
  "enabled": true,
  "address": {
    "province": "浙江省",
    "city": "杭州市"
  },
  "roles": ["admin", "developer"],
  "createdAt": "2026-05-13T10:30:00"
}
```

这类结构在关系型数据库中通常需要拆分为用户表、地址表、角色关联表等多张表，而在 MongoDB 中可以作为一个完整文档保存。对于以对象为中心的业务数据，MongoDB 能减少表关联和对象转换成本。

不过，MongoDB 并不等同于“无需设计”。在实际开发中仍然需要合理设计集合结构、字段类型、索引、文档大小、查询模式和数据生命周期。尤其是高频查询字段必须建立合适索引，否则随着数据量增长，查询性能会明显下降。

### Spring Data MongoDB 定位

Spring Data MongoDB 的定位是为 Spring 应用提供统一、声明式、对象化的 MongoDB 数据访问能力。它屏蔽了 MongoDB Java Driver 的大量底层细节，使开发人员可以使用更符合 Spring 编程模型的方式操作 MongoDB。

在 Spring Boot 3 中，Spring Data MongoDB 主要提供两类开发方式：一种是 Repository 风格，另一种是 MongoTemplate 风格。

Repository 风格适合标准 CRUD、简单查询、分页排序和方法命名查询。开发人员只需要定义接口并继承 `MongoRepository`，Spring Data MongoDB 会在运行时自动生成对应实现。

MongoTemplate 风格适合复杂查询、动态条件、批量更新、聚合查询、精细化控制集合操作等场景。它更接近 MongoDB 原生操作能力，但又保留了 Spring 的对象映射和模板封装优势。

两种方式的定位如下：

| 开发方式          | 适用场景                               | 特点                             |
| ----------------- | -------------------------------------- | -------------------------------- |
| `MongoRepository` | 标准 CRUD、简单条件查询、分页排序      | 代码简洁，声明式强，适合常规业务 |
| `MongoTemplate`   | 动态查询、复杂更新、聚合统计、批量操作 | 控制力更强，适合复杂业务         |
| 原生 Driver       | 极特殊的底层操作或性能调优             | 灵活度最高，但开发成本更高       |

在实际项目中，通常不会只使用其中一种方式。推荐做法是：简单 CRUD 使用 `MongoRepository`，复杂查询和聚合统计使用 `MongoTemplate`。这样既可以保证基础代码简洁，也可以在复杂场景下保留足够的操作能力。

Spring Data MongoDB 在项目中的典型职责包括：

| 职责            | 说明                                             |
| --------------- | ------------------------------------------------ |
| 连接管理        | 通过 Spring Boot 自动配置完成 MongoDB 连接初始化 |
| 对象映射        | 将 Java 实体类与 MongoDB 文档进行映射            |
| 主键处理        | 支持 `@Id` 映射 MongoDB `_id` 字段               |
| 集合映射        | 支持 `@Document` 指定实体类对应的集合            |
| 字段映射        | 支持 `@Field` 指定 Java 字段与文档字段的映射关系 |
| Repository 查询 | 支持接口式 CRUD、方法命名查询、自定义查询        |
| 模板操作        | 支持通过 `MongoTemplate` 编写复杂查询和更新      |
| 聚合查询        | 支持 Aggregation Pipeline 的 Java API 封装       |
| 索引声明        | 支持通过注解声明普通索引、唯一索引、复合索引等   |

从开发分层上看，Spring Data MongoDB 通常位于业务服务层和 MongoDB 数据库之间。Controller 接收请求，Service 处理业务逻辑，Repository 或 MongoTemplate 负责数据访问。

```text
Controller
    ↓
Service
    ↓
Repository / MongoTemplate
    ↓
Spring Data MongoDB
    ↓
MongoDB Java Driver
    ↓
MongoDB Server
```

这种分层方式可以避免业务代码直接依赖底层数据库驱动，使代码结构更清晰，也更容易进行测试、维护和扩展。

### 适用业务场景

Spring Data MongoDB 适用于需要在 Spring Boot 项目中高效操作 MongoDB 的业务场景，尤其适合数据结构灵活、字段变化频繁、对象嵌套明显、读写压力较高或需要快速迭代的系统模块。

常见适用场景如下：

| 场景     | 说明                                                 |
| -------- | ---------------------------------------------------- |
| 用户画像 | 用户标签、偏好、行为特征字段变化较快，适合文档模型   |
| 日志数据 | 操作日志、接口日志、审计日志通常写入频繁、结构可扩展 |
| 内容管理 | 文章、评论、配置项、页面内容等字段结构灵活           |
| IoT 数据 | 设备上报数据字段多变，适合按设备、时间维度存储       |
| 订单快照 | 保存下单时的商品、价格、地址、优惠等完整业务快照     |
| 表单数据 | 动态表单字段不固定，使用关系型表结构维护成本较高     |
| 消息通知 | 站内信、通知记录、推送记录适合按用户和时间查询       |
| 统计结果 | 聚合后的中间结果、报表缓存、宽表数据适合文档存储     |

例如，订单快照是 MongoDB 比较典型的使用场景。订单创建后，需要保存商品名称、规格、价格、收货地址、优惠信息等下单时的完整状态。即使后续商品信息或用户地址发生变化，也不应影响历史订单。此时使用 MongoDB 文档保存订单快照，可以将订单主信息、商品列表、收货地址、优惠信息放在同一个文档中，查询订单详情时不需要跨多张表关联。

MongoDB 也适合动态字段较多的业务。例如低代码表单、扩展属性配置、用户标签系统等。如果使用关系型数据库，字段变更可能需要频繁调整表结构；如果使用 MongoDB，可以将可变属性作为文档字段或嵌套对象存储，降低结构变更成本。

但 MongoDB 并不适合所有场景。以下场景需要谨慎使用：

| 不推荐场景              | 原因                                                         |
| ----------------------- | ------------------------------------------------------------ |
| 强事务核心账务          | 多表强一致事务和复杂约束通常更适合关系型数据库               |
| 高度规范化数据          | 如果业务强依赖多表关联和外键约束，MongoDB 维护成本可能更高   |
| 复杂实时关联查询        | MongoDB 不适合大量类似 SQL Join 的复杂关联查询               |
| 报表型多维分析          | 大规模 OLAP 分析通常更适合 ClickHouse、Doris、StarRocks 等分析型数据库 |
| Schema 极稳定的传统业务 | 如果数据结构长期稳定且关系明确，关系型数据库通常更直接       |

在 Spring Boot 3 项目中选择 Spring Data MongoDB 时，可以按照以下原则判断：

```text
数据结构灵活、文档天然聚合、读写压力较高、关联关系较少 → 适合 MongoDB

强事务、多表关联复杂、数据约束严格、报表分析重 → 优先考虑关系型数据库或分析型数据库
```

因此，Spring Data MongoDB 更适合作为业务系统中的文档型数据访问方案，而不是简单替代 MySQL、PostgreSQL 等关系型数据库。合理的架构通常是多种数据库组合使用：关系型数据库保存强一致核心数据，MongoDB 保存灵活文档数据、日志数据、快照数据和扩展属性数据。

## 环境准备

本节用于准备 Spring Boot 3 项目访问 MongoDB 所需的运行环境、项目版本和 Maven 依赖。Spring Data MongoDB 主要通过 `MongoRepository` 和 `MongoTemplate` 两种方式提供数据访问能力，官方文档也建议大多数场景优先使用这两类 API。([Home](https://docs.spring.io/spring-data/mongodb/reference/mongodb.html?utm_source=chatgpt.com))

### MongoDB 服务准备

开发环境建议优先使用 Docker Compose 启动 MongoDB，便于统一端口、账号、密码和数据目录。生产环境应使用独立部署、副本集或云数据库，并根据业务要求配置认证、备份、监控和容量规划。

MongoDB 官方 Docker 镜像支持通过 `MONGO_INITDB_ROOT_USERNAME` 和 `MONGO_INITDB_ROOT_PASSWORD` 初始化 root 用户；如果这两个变量同时存在，MongoDB 会启用认证。([Docker Hub](https://hub.docker.com/_/mongo/?utm_source=chatgpt.com))

文件位置：`docker-compose.yml`

```yaml
services:
  mongodb:
    image: mongo:8.0
    container_name: ateng-mongodb
    restart: always
    ports:
      # 本机开发环境暴露 MongoDB 默认端口
      - "27017:27017"
    environment:
      # 初始化 root 用户，认证库为 admin
      MONGO_INITDB_ROOT_USERNAME: root
      MONGO_INITDB_ROOT_PASSWORD: root123456
      # 初始化业务数据库名称
      MONGO_INITDB_DATABASE: ateng_mongo
    volumes:
      # 持久化 MongoDB 数据
      - ./data/mongodb:/data/db
    networks:
      - ateng-network

networks:
  ateng-network:
    driver: bridge
```

启动 MongoDB 服务：

```bash
docker compose up -d
docker ps | grep ateng-mongodb
docker logs -f ateng-mongodb
```

连接 MongoDB 进行验证：

```bash
docker exec -it ateng-mongodb mongosh \
  -u root \
  -p root123456 \
  --authenticationDatabase admin
```

进入 `mongosh` 后执行以下命令：

```javascript
use ateng_mongo
db.runCommand({ ping: 1 })
db.createCollection("user_info")
show collections
```

如果返回 `ok: 1`，说明 MongoDB 服务可正常访问。

MongoDB 连接字符串通常使用 `mongodb://` 或 `mongodb+srv://` 格式，标准格式中可以包含用户名、密码、主机、端口、默认认证库和连接参数；如果用户名或密码包含 `$ : / ? # [ ] @` 等特殊字符，需要进行 URL 百分号编码。([MongoDB](https://www.mongodb.com/docs/current/reference/connection-string/?utm_source=chatgpt.com))

常见连接字符串示例：

```text
# 无认证，本地开发临时使用
mongodb://localhost:27017/ateng_mongo

# 有认证，用户在 admin 库中创建
mongodb://root:root123456@localhost:27017/ateng_mongo?authSource=admin

# 副本集连接示例
mongodb://root:root123456@mongo-1:27017,mongo-2:27017,mongo-3:27017/ateng_mongo?authSource=admin&replicaSet=rs0
```

开发环境可以先使用单节点 MongoDB，重点验证连接、实体映射、CRUD、索引和查询逻辑。生产环境不建议使用单节点部署，应优先使用副本集或云数据库实例，并限制公网访问、启用认证、配置最小权限账号。

### Spring Boot 3 项目版本要求

Spring Boot 3.x 基于 Jakarta EE 体系，项目最低要求 Java 17。以 Spring Boot 3.3.x 官方文档为例，其要求 Java 17 及以上，并要求 Maven 3.6.3 或更高版本。([Spring Enterprise Docs](https://docs.enterprise.spring.io/spring-boot/system-requirements.html?utm_source=chatgpt.com))

推荐项目版本基线如下：

| 组件                | 推荐版本                | 说明                                                      |
| ------------------- | ----------------------- | --------------------------------------------------------- |
| JDK                 | 17 或 21                | Spring Boot 3 最低要求 Java 17，生产环境推荐使用 LTS 版本 |
| Spring Boot         | 3.x                     | 本文档以 Spring Boot 3 为基线                             |
| Maven               | 3.6.3+                  | Spring Boot 3 官方支持的 Maven 基线                       |
| MongoDB             | 6.x / 7.x / 8.x         | 根据公司基线和驱动兼容性选择                              |
| Spring Data MongoDB | 由 Spring Boot BOM 管理 | 不建议手动指定版本，避免依赖冲突                          |

检查本地 Java 和 Maven 版本：

```bash
java -version
mvn -version
```

项目建议使用 Spring Boot 父工程统一管理依赖版本，不要单独指定 `spring-data-mongodb`、`mongodb-driver-sync` 等底层依赖版本。Spring Boot 会通过依赖管理自动匹配兼容版本，减少驱动版本与 Spring Data 版本不一致的问题。

推荐的基础项目结构如下：

```text
springboot3-mongodb
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── io.github.atengk
│   │   │       ├── MongoApplication.java
│   │   │       ├── config
│   │   │       ├── entity
│   │   │       ├── repository
│   │   │       ├── service
│   │   │       └── controller
│   │   └── resources
│   │       └── application.yml
│   └── test
│       └── java
```

### Maven 依赖配置

Spring Boot 3 项目中接入 MongoDB，核心依赖是 `spring-boot-starter-data-mongodb`。该 Starter 会自动引入 Spring Data MongoDB、MongoDB Java Driver、对象映射和自动配置能力。

文件位置：`pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="
         http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <!-- 使用 Spring Boot 父工程统一管理依赖版本 -->
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.16</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>springboot3-mongodb</artifactId>
    <version>1.0.0</version>
    <name>springboot3-mongodb</name>
    <description>Spring Boot 3 MongoDB 示例项目</description>

    <properties>
        <!-- Spring Boot 3 最低要求 Java 17 -->
        <java.version>17</java.version>
        <!-- Hutool 工具类版本，实际项目可按公司依赖基线统一管理 -->
        <hutool.version>5.8.36</hutool.version>
    </properties>

    <dependencies>
        <!-- Spring Web，用于后续编写 REST 接口 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Data MongoDB，提供 MongoRepository、MongoTemplate、对象映射和自动配置 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-mongodb</artifactId>
        </dependency>

        <!-- 参数校验，后续 DTO 入参校验会使用 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Hutool 工具类，常用于字符串、集合、日期、对象判断等通用处理 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok，减少 Getter、Setter、构造方法等样板代码 -->
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

    <build>
        <plugins>
            <plugin>
                <!-- Spring Boot Maven 插件，用于打包和运行应用 -->
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

依赖配置完成后，可以执行以下命令检查依赖是否正常解析：

```bash
mvn clean package -DskipTests
mvn dependency:tree | grep mongodb
```

如果项目能够正常编译，并且依赖树中存在 `spring-boot-starter-data-mongodb` 和 MongoDB Driver，说明基础依赖配置正确。

## 基础配置

本节用于配置 Spring Boot 3 与 MongoDB 的连接参数、认证参数，以及 `MongoTemplate` 和 `MongoRepository` 的启用方式。Spring Boot 在引入 `spring-boot-starter-data-mongodb` 后，会根据 `spring.data.mongodb.*` 配置自动创建 MongoDB 客户端、`MongoTemplate` 和 Repository 相关基础设施。

### MongoDB 连接配置

MongoDB 连接配置通常写在 `application.yml` 中。开发环境建议使用 `uri` 方式统一配置连接参数，生产环境建议通过环境变量注入账号、密码和地址，避免敏感信息直接写入代码仓库。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: springboot3-mongodb

  data:
    mongodb:
      # MongoDB 连接字符串
      # authSource=admin 表示使用 admin 库中的用户进行认证
      uri: mongodb://root:root123456@localhost:27017/ateng_mongo?authSource=admin

logging:
  level:
    # 查看 Spring Data MongoDB 执行日志，开发环境可开启，生产环境谨慎调整
    org.springframework.data.mongodb.core.MongoTemplate: debug
```

也可以拆分为独立参数形式：

```yaml
spring:
  data:
    mongodb:
      # MongoDB 主机地址
      host: localhost
      # MongoDB 端口
      port: 27017
      # 业务数据库
      database: ateng_mongo
      # 认证用户名
      username: root
      # 认证密码
      password: root123456
      # 认证数据库
      authentication-database: admin
```

两种配置方式选择一种即可。实际项目中更推荐使用 `uri`，因为它可以统一表达单节点、副本集、连接参数、认证库、超时时间等配置。

生产环境示例：

```yaml
spring:
  data:
    mongodb:
      # 通过环境变量注入，避免密码出现在配置文件中
      uri: ${MONGODB_URI}
```

启动时指定环境变量：

```bash
export MONGODB_URI='mongodb://app_user:app_password@mongo-1:27017,mongo-2:27017,mongo-3:27017/ateng_mongo?authSource=admin&replicaSet=rs0'
java -jar springboot3-mongodb.jar
```

常见连接参数示例：

```text
connectTimeoutMS=3000       # 建立连接超时时间
socketTimeoutMS=5000        # Socket 读写超时时间
maxPoolSize=100             # 最大连接池数量
minPoolSize=5               # 最小连接池数量
retryWrites=true            # 是否启用写重试
authSource=admin            # 认证库
replicaSet=rs0              # 副本集名称
```

完整 URI 示例：

```text
mongodb://app_user:app_password@mongo-1:27017,mongo-2:27017,mongo-3:27017/ateng_mongo?authSource=admin&replicaSet=rs0&connectTimeoutMS=3000&socketTimeoutMS=5000&maxPoolSize=100
```

### 数据库认证配置

MongoDB 的认证用户属于指定数据库。常见做法是在 `admin` 数据库中创建管理用户，在业务数据库中创建应用专用用户。应用程序不建议直接使用 root 用户连接 MongoDB，应使用最小权限账号。

下面示例创建一个只对 `ateng_mongo` 数据库具备读写权限的应用用户。

进入 MongoDB：

```bash
docker exec -it ateng-mongodb mongosh \
  -u root \
  -p root123456 \
  --authenticationDatabase admin
```

创建业务用户：

```javascript
use ateng_mongo

db.createUser({
  user: "ateng_app",
  pwd: "Ateng@123456",
  roles: [
    {
      role: "readWrite",
      db: "ateng_mongo"
    }
  ]
})
```

使用业务用户连接：

```bash
docker exec -it ateng-mongodb mongosh \
  "mongodb://ateng_app:Ateng%40123456@localhost:27017/ateng_mongo?authSource=ateng_mongo"
```

这里密码中的 `@` 被编码为 `%40`。MongoDB 官方连接字符串文档说明，用户名或密码中的特殊字符需要进行百分号编码，否则连接字符串可能解析失败。([MongoDB](https://www.mongodb.com/docs/current/reference/connection-string/?utm_source=chatgpt.com))

应用配置调整为业务用户：

```yaml
spring:
  data:
    mongodb:
      # 使用业务库用户进行认证，authSource 指向用户所在数据库
      uri: mongodb://ateng_app:Ateng%40123456@localhost:27017/ateng_mongo?authSource=ateng_mongo
```

认证配置建议遵循以下原则：

| 原则                     | 说明                                     |
| ------------------------ | ---------------------------------------- |
| 不使用 root 用户连接应用 | root 权限过大，应用泄露后风险高          |
| 使用业务库专用用户       | 每个应用使用独立账号，便于权限控制和审计 |
| 密码通过环境变量注入     | 避免敏感信息提交到 Git 仓库              |
| 明确配置 `authSource`    | 防止用户创建库与业务库不一致导致认证失败 |
| 特殊字符进行 URL 编码    | 避免连接字符串解析错误                   |
| 生产环境限制网络访问     | 仅允许应用服务器或内网网段访问 MongoDB   |

### MongoTemplate 与 Repository 配置

Spring Data MongoDB 提供两种常用访问方式：`MongoTemplate` 和 Repository。`MongoTemplate` 是 `MongoOperations` 的主要实现，适合编写复杂查询、更新、删除、聚合和底层操作；Repository 则适合标准 CRUD 和简单条件查询。([Home](https://docs.spring.io/spring-data/data-mongo/docs/current/api/org/springframework/data/mongodb/core/MongoTemplate.html?utm_source=chatgpt.com))

一般情况下，只要引入 `spring-boot-starter-data-mongodb` 并配置 `spring.data.mongodb.uri`，Spring Boot 会自动创建 `MongoTemplate`，并扫描 Repository 接口。

项目启动类：

文件位置：`src/main/java/io/github/atengk/MongoApplication.java`

```java
package io.github.atengk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 3 MongoDB 启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@SpringBootApplication
public class MongoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MongoApplication.class, args);
    }

}
```

如果 Repository 不在启动类同级或子级包下，可以显式配置扫描路径。

文件位置：`src/main/java/io/github/atengk/config/MongoRepositoryConfig.java`

```java
package io.github.atengk.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB Repository 扫描配置
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Configuration
@EnableMongoRepositories(basePackages = "io.github.atengk.repository")
public class MongoRepositoryConfig {
}
```

如果需要对 `MongoTemplate` 进行统一增强，可以单独声明配置类。例如关闭 `_class` 字段写入，避免 MongoDB 文档中出现 Java 类型信息。需要注意，关闭 `_class` 后会降低多态类型自动还原能力，适合实体结构明确、集合模型稳定的业务。

文件位置：`src/main/java/io/github/atengk/config/MongoConfig.java`

```java
package io.github.atengk.config;

import cn.hutool.core.util.ObjectUtil;
import com.mongodb.client.MongoClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

/**
 * MongoDB 基础配置
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Configuration
public class MongoConfig {

    /**
     * 自定义 MongoTemplate。
     *
     * @param mongoDatabaseFactory MongoDB 数据库工厂
     * @param mappingMongoConverter MongoDB 映射转换器
     * @return MongoTemplate
     */
    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory,
                                       MappingMongoConverter mappingMongoConverter) {
        if (ObjectUtil.isNotNull(mappingMongoConverter)) {
            // 移除默认 _class 字段，保持文档结构简洁
            mappingMongoConverter.setTypeMapper(new DefaultMongoTypeMapper(null));
            log.info("MongoDB 映射配置已初始化，已关闭 _class 字段写入");
        }

        return new MongoTemplate(mongoDatabaseFactory, mappingMongoConverter);
    }

}
```

Repository 示例接口：

文件位置：`src/main/java/io/github/atengk/repository/UserInfoRepository.java`

```java
package io.github.atengk.repository;

import io.github.atengk.entity.UserInfo;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * 用户信息 Repository
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface UserInfoRepository extends MongoRepository<UserInfo, String> {

    /**
     * 根据用户名查询用户信息。
     *
     * @param username 用户名
     * @return 用户信息
     */
    Optional<UserInfo> findByUsername(String username);

    /**
     * 判断用户名是否存在。
     *
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);

}
```

最小实体类示例：

文件位置：`src/main/java/io/github/atengk/entity/UserInfo.java`

```java
package io.github.atengk.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 用户信息文档实体
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Document(collection = "user_info")
public class UserInfo {

    /**
     * 文档主键，对应 MongoDB 的 _id 字段
     */
    @Id
    private String id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

}
```

启动后可以通过以下方式快速验证 `MongoTemplate` 和 Repository 是否已注入成功。

文件位置：`src/main/java/io/github/atengk/config/MongoStartupCheck.java`

```java
package io.github.atengk.config;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * MongoDB 启动检查
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MongoStartupCheck implements CommandLineRunner {

    private final MongoTemplate mongoTemplate;
    private final UserInfoRepository userInfoRepository;

    /**
     * 应用启动后检查 MongoDB 连接和集合信息。
     *
     * @param args 启动参数
     */
    @Override
    public void run(String... args) {
        Set<String> collectionNames = mongoTemplate.getCollectionNames();
        if (CollUtil.isEmpty(collectionNames)) {
            log.info("MongoDB 连接成功，当前数据库暂无集合");
            return;
        }

        log.info("MongoDB 连接成功，当前集合数量：{}，集合列表：{}", collectionNames.size(), collectionNames);
        log.info("UserInfoRepository 初始化成功，当前用户文档数量：{}", userInfoRepository.count());
    }

}
```

验证方式：

```bash
mvn spring-boot:run
```

启动日志中如果出现类似内容，说明基础配置生效：

```text
MongoDB 映射配置已初始化，已关闭 _class 字段写入
MongoDB 连接成功，当前集合数量：1，集合列表：[user_info]
UserInfoRepository 初始化成功，当前用户文档数量：0
```

实际项目中，`MongoRepository` 适合放在 `repository` 层处理标准 CRUD 和简单查询；`MongoTemplate` 适合放在 `service` 或专门的 `manager` 层处理动态条件、复杂更新、批量操作和聚合统计。两者可以同时使用，不需要二选一。



## 文档模型设计

文档模型设计决定了 MongoDB 中集合、文档字段、嵌套结构、数组字段、主键和时间字段的组织方式。MongoDB 虽然支持动态 Schema，但在 Spring Boot 3 项目中仍建议按照业务查询方式提前设计文档结构，避免后续出现字段混乱、索引失效、文档过大或查询复杂度过高的问题。

### 集合与文档结构

MongoDB 使用集合保存文档。集合可以理解为一类业务数据的容器，文档则是一条完整的业务记录。设计集合时，应优先考虑业务的读取方式，而不是简单照搬关系型数据库的表结构。

例如用户信息可以设计为一个 `user_info` 集合，用户的基础信息、联系信息、标签、地址等字段可以放在同一个文档中。对于读取用户详情、用户画像、用户标签等场景，这种结构可以减少多表关联和二次查询。

用户信息文档示例：

```json
{
  "_id": "665f2d4f91f3a65a9f2d1001",
  "username": "ateng",
  "nickname": "阿腾",
  "age": 28,
  "enabled": true,
  "email": "ateng@example.com",
  "mobile": "13800000000",
  "address": {
    "province": "浙江省",
    "city": "杭州市",
    "detail": "西湖区文三路"
  },
  "tags": ["java", "springboot", "mongodb"],
  "loginCount": 18,
  "lastLoginTime": "2026-05-13T10:30:00",
  "createdAt": "2026-05-13T09:00:00",
  "updatedAt": "2026-05-13T10:30:00"
}
```

集合设计时建议遵循以下原则：

| 设计原则           | 说明                                        |
| ------------------ | ------------------------------------------- |
| 按业务聚合设计文档 | 经常一起读取的数据可以放在同一个文档中      |
| 避免无意义拆分     | 不要完全按照关系型数据库范式拆成多个集合    |
| 控制文档大小       | 单个文档不要无限增长，尤其是数组字段        |
| 查询字段稳定       | 高频查询字段应保持稳定，便于后续建立索引    |
| 字段命名统一       | 建议统一使用小驼峰或下划线风格，不要混用    |
| 避免深层嵌套       | 嵌套层级过深会增加查询、更新和维护成本      |
| 明确数据生命周期   | 日志、临时数据、验证码等可配合 TTL 索引清理 |

文档结构不建议过度嵌套。例如用户地址可以作为用户文档的嵌套对象，因为地址通常跟随用户一起读取；但用户订单不建议直接嵌套在用户文档中，因为订单数量会不断增长，容易导致用户文档过大。

推荐结构：

```json
{
  "_id": "user_10001",
  "username": "ateng",
  "address": {
    "province": "浙江省",
    "city": "杭州市"
  }
}
```

不推荐结构：

```json
{
  "_id": "user_10001",
  "username": "ateng",
  "orders": [
    {
      "orderNo": "O202605130001",
      "amount": 99.00
    },
    {
      "orderNo": "O202605130002",
      "amount": 199.00
    }
  ]
}
```

如果订单数量不可控，应单独设计 `order_info` 集合，并通过 `userId` 关联用户，而不是将订单数组长期追加到用户文档中。

常见集合命名建议如下：

| 业务对象 | 集合名称         | 说明                       |
| -------- | ---------------- | -------------------------- |
| 用户信息 | `user_info`      | 保存用户基础信息           |
| 操作日志 | `operation_log`  | 保存接口或业务操作日志     |
| 订单快照 | `order_snapshot` | 保存订单创建时的完整快照   |
| 系统配置 | `system_config`  | 保存动态配置项             |
| 消息通知 | `message_notice` | 保存站内信、通知、推送记录 |

### 实体类映射

Spring Data MongoDB 通过实体类与 MongoDB 文档建立映射关系。常用注解包括 `@Document`、`@Id`、`@Field`、`@Transient`、`@CreatedDate`、`@LastModifiedDate` 等。

常用注解说明：

| 注解                | 作用                                          |
| ------------------- | --------------------------------------------- |
| `@Document`         | 标识当前类是 MongoDB 文档实体，并指定集合名称 |
| `@Id`               | 标识主键字段，映射 MongoDB 的 `_id` 字段      |
| `@Field`            | 指定 Java 字段与 MongoDB 文档字段的映射关系   |
| `@Transient`        | 标识字段不持久化到 MongoDB                    |
| `@CreatedDate`      | 自动填充创建时间                              |
| `@LastModifiedDate` | 自动填充更新时间                              |

实体类应尽量保持清晰，不建议把 Controller 入参、接口响应、业务临时字段全部塞进实体类。推荐将实体类、DTO、VO 分开设计：实体类负责数据库映射，DTO 负责接口入参，VO 负责接口出参。

下面代码定义用户文档实体，包含基础字段、嵌套地址对象、数组标签字段和审计时间字段。

文件位置：`src/main/java/io/github/atengk/entity/UserInfo.java`

```java
package io.github.atengk.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息文档实体
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Document(collection = "user_info")
public class UserInfo {

    /**
     * 文档主键，对应 MongoDB 的 _id 字段
     */
    @Id
    private String id;

    /**
     * 用户名
     */
    @Field("username")
    private String username;

    /**
     * 昵称
     */
    @Field("nickname")
    private String nickname;

    /**
     * 年龄
     */
    @Field("age")
    private Integer age;

    /**
     * 邮箱
     */
    @Field("email")
    private String email;

    /**
     * 手机号
     */
    @Field("mobile")
    private String mobile;

    /**
     * 是否启用
     */
    @Field("enabled")
    private Boolean enabled;

    /**
     * 地址信息，映射为嵌套文档
     */
    @Field("address")
    private AddressInfo address;

    /**
     * 用户标签，映射为数组字段
     */
    @Field("tags")
    private List<String> tags;

    /**
     * 登录次数
     */
    @Field("login_count")
    private Integer loginCount;

    /**
     * 最后登录时间
     */
    @Field("last_login_time")
    private LocalDateTime lastLoginTime;

    /**
     * 创建时间
     */
    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 临时展示字段，不会保存到 MongoDB
     */
    @Transient
    private String displayName;

    /**
     * 用户地址信息
     *
     * @author Ateng
     * @since 2026-05-13
     */
    @Data
    public static class AddressInfo {

        /**
         * 省份
         */
        @Field("province")
        private String province;

        /**
         * 城市
         */
        @Field("city")
        private String city;

        /**
         * 详细地址
         */
        @Field("detail")
        private String detail;

    }

}
```

实体类设计建议：

| 建议                    | 说明                                                         |
| ----------------------- | ------------------------------------------------------------ |
| 实体类只表达持久化结构  | 不要混入大量接口展示字段                                     |
| 集合名称显式指定        | 使用 `@Document(collection = "xxx")` 避免默认集合名不符合规范 |
| 字段映射保持统一        | 如果数据库字段使用下划线，建议通过 `@Field` 显式映射         |
| 嵌套对象单独建类        | 嵌套结构复杂时可以拆成独立类或静态内部类                     |
| 临时字段加 `@Transient` | 防止非持久化字段被写入 MongoDB                               |
| 数组字段控制大小        | 不适合无限追加的数组应拆成独立集合                           |

### 主键与字段映射

MongoDB 文档默认使用 `_id` 作为主键。Spring Data MongoDB 中可以使用 `@Id` 将 Java 字段映射到 `_id`。主键类型通常使用 `String`，由 MongoDB 自动生成 ObjectId 后映射为字符串，也可以由业务系统自行生成。

常见主键设计方式：

| 主键方式         | 示例                       | 适用场景               |
| ---------------- | -------------------------- | ---------------------- |
| MongoDB 自动生成 | `665f2d4f91f3a65a9f2d1001` | 普通业务数据           |
| 业务编码         | `USER_10001`               | 需要外部系统识别或对接 |
| 雪花 ID 字符串   | `1789922888123457536`      | 分布式系统统一 ID      |
| UUID             | `0b0cfd49c6fd4ac88f06`     | 对顺序无要求的唯一标识 |

如果业务没有特殊要求，推荐使用 MongoDB 自动生成的 ObjectId。它可以减少主键生成逻辑，也能避免业务编码变更带来的影响。

字段映射时需要注意 Java 字段名与 MongoDB 字段名的关系。例如 Java 中常用小驼峰命名 `loginCount`，而 MongoDB 中可能希望保存为下划线字段 `login_count`。此时可以使用 `@Field("login_count")` 显式映射。

下面代码演示自定义主键和字段映射方式。

文件位置：`src/main/java/io/github/atengk/entity/SystemConfig.java`

```java
package io.github.atengk.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * 系统配置文档实体
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Document(collection = "system_config")
public class SystemConfig {

    /**
     * 配置编码，作为业务主键使用
     */
    @Id
    private String configCode;

    /**
     * 配置名称
     */
    @Field("config_name")
    private String configName;

    /**
     * 配置值
     */
    @Field("config_value")
    private String configValue;

    /**
     * 是否启用
     */
    @Field("enabled")
    private Boolean enabled;

    /**
     * 更新时间
     */
    @Field("updated_at")
    private LocalDateTime updatedAt;

}
```

保存后的 MongoDB 文档结构如下：

```json
{
  "_id": "SITE_TITLE",
  "config_name": "站点标题",
  "config_value": "Spring Boot 3 MongoDB 开发文档",
  "enabled": true,
  "updated_at": "2026-05-13T10:30:00"
}
```

需要注意，`@Id` 修饰的字段无论 Java 字段名是什么，最终都会映射为 MongoDB 的 `_id` 字段。如果需要查询该字段，MongoDB 原生查询中应使用 `_id`，而 Spring Data Repository 方法中使用 Java 字段名即可。

例如：

```java
Optional<SystemConfig> findByConfigCode(String configCode);
```

虽然实体类字段名是 `configCode`，但 MongoDB 中实际字段是 `_id`。

### 时间字段处理

Spring Boot 3 项目中推荐使用 `java.time.LocalDateTime`、`Instant`、`LocalDate` 等 Java 8 时间类型处理时间字段。Spring Data MongoDB 可以完成 Java 时间类型与 MongoDB 日期类型之间的转换。

如果使用 `@CreatedDate` 和 `@LastModifiedDate` 自动填充创建时间、更新时间，需要启用 MongoDB 审计功能。

下面代码开启 MongoDB 审计，用于支持 `@CreatedDate` 和 `@LastModifiedDate` 自动填充。

文件位置：`src/main/java/io/github/atengk/config/MongoAuditingConfig.java`

```java
package io.github.atengk.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * MongoDB 审计字段配置
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Configuration
@EnableMongoAuditing
public class MongoAuditingConfig {
}
```

启用审计后，实体类中如下字段会在新增和更新时自动维护：

```java
@CreatedDate
@Field("created_at")
private LocalDateTime createdAt;

@LastModifiedDate
@Field("updated_at")
private LocalDateTime updatedAt;
```

如果不使用审计注解，也可以在业务代码中手动设置时间。下面代码在新增用户时手动填充创建时间、更新时间，并使用 Hutool 判断字符串和集合参数。

文件位置：`src/main/java/io/github/atengk/service/UserInfoInitService.java`

```java
package io.github.atengk.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.entity.UserInfo;
import io.github.atengk.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息初始化服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoInitService {

    private final UserInfoRepository userInfoRepository;

    /**
     * 创建默认用户。
     *
     * @param username 用户名
     * @param nickname 昵称
     * @param tags 标签
     * @return 用户信息
     */
    public UserInfo createDefaultUser(String username, String nickname, List<String> tags) {
        if (StrUtil.isBlank(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setUsername(username);
        userInfo.setNickname(StrUtil.blankToDefault(nickname, username));
        userInfo.setEnabled(Boolean.TRUE);
        userInfo.setAge(18);
        userInfo.setTags(CollUtil.emptyIfNull(tags));
        userInfo.setLoginCount(0);
        userInfo.setCreatedAt(LocalDateTime.now());
        userInfo.setUpdatedAt(LocalDateTime.now());

        UserInfo savedUser = userInfoRepository.save(userInfo);
        log.info("默认用户创建成功，用户ID：{}，用户名：{}", savedUser.getId(), savedUser.getUsername());
        return savedUser;
    }

}
```

时间字段处理建议：

| 建议                       | 说明                                         |
| -------------------------- | -------------------------------------------- |
| 推荐使用 Java 8 时间类型   | 如 `LocalDateTime`、`LocalDate`、`Instant`   |
| 创建时间和更新时间统一维护 | 可以使用审计注解，也可以在业务层手动维护     |
| 避免使用字符串保存时间     | 字符串不利于范围查询、排序和索引             |
| 前后端明确时间格式         | 接口层统一处理时区和格式化                   |
| 查询条件使用时间类型       | 范围查询时直接使用 `LocalDateTime` 或 `Date` |

如果系统涉及多时区，建议优先使用 `Instant` 存储统一时间点，在接口展示层再转换为用户所在时区。普通国内后台系统如果只使用北京时间，也可以统一使用 `LocalDateTime`，但需要保证服务端、数据库、容器和日志时区配置一致。

## Repository 开发

Repository 是 Spring Data MongoDB 提供的声明式数据访问方式。它适合标准 CRUD、简单条件查询、方法命名查询、自定义查询、分页排序等常见场景。对于复杂动态条件、复杂更新、聚合统计等场景，后续可以使用 `MongoTemplate` 补充。

### MongoRepository 基本使用

`MongoRepository<T, ID>` 是 Spring Data MongoDB 中最常用的 Repository 接口。`T` 表示实体类型，`ID` 表示主键类型。继承该接口后，Spring Data 会自动提供新增、修改、删除、根据 ID 查询、查询全部、分页查询、数量统计等基础方法。

下面代码定义用户信息 Repository，继承 `MongoRepository<UserInfo, String>` 后即可获得基础 CRUD 能力。

文件位置：`src/main/java/io/github/atengk/repository/UserInfoRepository.java`

```java
package io.github.atengk.repository;

import io.github.atengk.entity.UserInfo;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * 用户信息 Repository
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface UserInfoRepository extends MongoRepository<UserInfo, String> {

    /**
     * 根据用户名查询用户信息。
     *
     * @param username 用户名
     * @return 用户信息
     */
    Optional<UserInfo> findByUsername(String username);

    /**
     * 根据启用状态查询用户列表。
     *
     * @param enabled 是否启用
     * @return 用户列表
     */
    List<UserInfo> findByEnabled(Boolean enabled);

    /**
     * 判断用户名是否存在。
     *
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);

}
```

下面代码演示通过 Repository 完成新增、查询、更新和删除操作，适合放在业务 Service 中。

文件位置：`src/main/java/io/github/atengk/service/UserInfoRepositoryService.java`

```java
package io.github.atengk.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.entity.UserInfo;
import io.github.atengk.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息 Repository 业务服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoRepositoryService {

    private final UserInfoRepository userInfoRepository;

    /**
     * 新增用户。
     *
     * @param userInfo 用户信息
     * @return 保存后的用户信息
     */
    public UserInfo create(UserInfo userInfo) {
        if (StrUtil.isBlank(userInfo.getUsername())) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (userInfoRepository.existsByUsername(userInfo.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }

        userInfo.setEnabled(Boolean.TRUE);
        userInfo.setLoginCount(0);
        userInfo.setCreatedAt(LocalDateTime.now());
        userInfo.setUpdatedAt(LocalDateTime.now());

        UserInfo savedUser = userInfoRepository.save(userInfo);
        log.info("用户新增成功，用户ID：{}，用户名：{}", savedUser.getId(), savedUser.getUsername());
        return savedUser;
    }

    /**
     * 根据 ID 查询用户。
     *
     * @param id 用户ID
     * @return 用户信息
     */
    public UserInfo getById(String id) {
        if (StrUtil.isBlank(id)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        return userInfoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    /**
     * 更新用户昵称。
     *
     * @param id 用户ID
     * @param nickname 昵称
     * @return 更新后的用户信息
     */
    public UserInfo updateNickname(String id, String nickname) {
        UserInfo userInfo = this.getById(id);
        userInfo.setNickname(StrUtil.blankToDefault(nickname, userInfo.getUsername()));
        userInfo.setUpdatedAt(LocalDateTime.now());

        UserInfo savedUser = userInfoRepository.save(userInfo);
        log.info("用户昵称更新成功，用户ID：{}，昵称：{}", savedUser.getId(), savedUser.getNickname());
        return savedUser;
    }

    /**
     * 查询启用用户列表。
     *
     * @return 用户列表
     */
    public List<UserInfo> listEnabledUsers() {
        return userInfoRepository.findByEnabled(Boolean.TRUE);
    }

    /**
     * 删除用户。
     *
     * @param id 用户ID
     */
    public void deleteById(String id) {
        UserInfo userInfo = this.getById(id);
        userInfoRepository.deleteById(userInfo.getId());
        log.info("用户删除成功，用户ID：{}，用户名：{}", userInfo.getId(), userInfo.getUsername());
    }

}
```

`save` 方法既可以用于新增，也可以用于全量保存。如果实体对象中没有主键，通常会新增文档；如果实体对象中存在主键，并且数据库中已有对应文档，则会覆盖保存。对于只更新部分字段的场景，后续更推荐使用 `MongoTemplate` 的 `Update` 操作，避免不必要的全量覆盖。

### 方法命名查询

Spring Data MongoDB 支持根据 Repository 方法名自动生成查询逻辑。方法命名查询适合简单、固定、可读性较好的查询条件，例如按用户名查询、按状态查询、按年龄范围查询、按标签查询等。

常见方法命名规则：

| 方法关键字    | 示例                                | 含义            |
| ------------- | ----------------------------------- | --------------- |
| `findBy`      | `findByUsername`                    | 根据字段查询    |
| `And`         | `findByUsernameAndEnabled`          | 多条件 AND 查询 |
| `Or`          | `findByUsernameOrEmail`             | 多条件 OR 查询  |
| `Between`     | `findByAgeBetween`                  | 范围查询        |
| `GreaterThan` | `findByAgeGreaterThan`              | 大于查询        |
| `LessThan`    | `findByAgeLessThan`                 | 小于查询        |
| `Containing`  | `findByNicknameContaining`          | 包含匹配        |
| `In`          | `findByUsernameIn`                  | IN 查询         |
| `OrderBy`     | `findByEnabledOrderByCreatedAtDesc` | 固定排序        |
| `CountBy`     | `countByEnabled`                    | 统计数量        |
| `ExistsBy`    | `existsByUsername`                  | 判断是否存在    |
| `DeleteBy`    | `deleteByEnabled`                   | 根据条件删除    |

下面代码给出常见方法命名查询示例。

文件位置：`src/main/java/io/github/atengk/repository/UserInfoRepository.java`

```java
package io.github.atengk.repository;

import io.github.atengk.entity.UserInfo;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 用户信息 Repository
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface UserInfoRepository extends MongoRepository<UserInfo, String> {

    /**
     * 根据用户名查询用户信息。
     *
     * @param username 用户名
     * @return 用户信息
     */
    Optional<UserInfo> findByUsername(String username);

    /**
     * 根据用户名和启用状态查询用户。
     *
     * @param username 用户名
     * @param enabled 是否启用
     * @return 用户信息
     */
    Optional<UserInfo> findByUsernameAndEnabled(String username, Boolean enabled);

    /**
     * 根据昵称模糊查询用户。
     *
     * @param nickname 昵称关键字
     * @return 用户列表
     */
    List<UserInfo> findByNicknameContaining(String nickname);

    /**
     * 查询年龄大于指定值的用户。
     *
     * @param age 年龄
     * @return 用户列表
     */
    List<UserInfo> findByAgeGreaterThan(Integer age);

    /**
     * 查询年龄在指定范围内的用户。
     *
     * @param minAge 最小年龄
     * @param maxAge 最大年龄
     * @return 用户列表
     */
    List<UserInfo> findByAgeBetween(Integer minAge, Integer maxAge);

    /**
     * 根据用户名集合查询用户。
     *
     * @param usernames 用户名集合
     * @return 用户列表
     */
    List<UserInfo> findByUsernameIn(Collection<String> usernames);

    /**
     * 根据标签查询用户。
     *
     * @param tag 标签
     * @return 用户列表
     */
    List<UserInfo> findByTags(String tag);

    /**
     * 查询指定时间之后创建的用户，并按创建时间倒序排列。
     *
     * @param createdAt 创建时间
     * @return 用户列表
     */
    List<UserInfo> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime createdAt);

    /**
     * 统计启用或禁用用户数量。
     *
     * @param enabled 是否启用
     * @return 用户数量
     */
    long countByEnabled(Boolean enabled);

    /**
     * 判断用户名是否存在。
     *
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);

}
```

方法命名查询虽然方便，但不建议把非常复杂的条件全部塞进方法名中。例如下面这种方法名可读性较差，不推荐使用：

```java
List<UserInfo> findByUsernameContainingAndEnabledAndAgeBetweenAndCreatedAtAfterOrderByCreatedAtDesc(
        String username,
        Boolean enabled,
        Integer minAge,
        Integer maxAge,
        LocalDateTime createdAt
);
```

复杂条件建议使用 `@Query` 或 `MongoTemplate`。如果查询条件是动态组合的，例如用户名可选、年龄可选、状态可选、时间范围可选，更适合使用 `MongoTemplate` 构建 `Criteria`。

### 自定义查询注解

`@Query` 可以直接在 Repository 方法上编写 MongoDB JSON 查询语句，适合方法命名查询表达不清晰或需要精确控制查询字段的场景。它可以用于精确查询、模糊查询、范围查询、数组查询和字段投影。

下面代码演示使用 `@Query` 编写自定义查询。

文件位置：`src/main/java/io/github/atengk/repository/UserInfoQueryRepository.java`

```java
package io.github.atengk.repository;

import io.github.atengk.entity.UserInfo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户信息自定义查询 Repository
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface UserInfoQueryRepository extends MongoRepository<UserInfo, String> {

    /**
     * 根据用户名精确查询。
     *
     * @param username 用户名
     * @return 用户信息
     */
    @Query("{ 'username': ?0 }")
    Optional<UserInfo> queryByUsername(String username);

    /**
     * 根据昵称进行正则模糊查询。
     *
     * @param nickname 昵称关键字
     * @return 用户列表
     */
    @Query("{ 'nickname': { $regex: ?0, $options: 'i' } }")
    List<UserInfo> queryByNicknameLike(String nickname);

    /**
     * 查询指定年龄范围内的启用用户。
     *
     * @param minAge 最小年龄
     * @param maxAge 最大年龄
     * @return 用户列表
     */
    @Query("{ 'enabled': true, 'age': { $gte: ?0, $lte: ?1 } }")
    List<UserInfo> queryEnabledUsersByAgeRange(Integer minAge, Integer maxAge);

    /**
     * 根据标签查询用户。
     *
     * @param tag 标签
     * @return 用户列表
     */
    @Query("{ 'tags': ?0 }")
    List<UserInfo> queryByTag(String tag);

    /**
     * 查询指定时间之后登录过的用户。
     *
     * @param lastLoginTime 最后登录时间
     * @return 用户列表
     */
    @Query("{ 'last_login_time': { $gte: ?0 } }")
    List<UserInfo> queryByLastLoginTimeAfter(LocalDateTime lastLoginTime);

    /**
     * 根据用户名查询用户基础信息，只返回部分字段。
     *
     * @param username 用户名
     * @return 用户信息
     */
    @Query(value = "{ 'username': ?0 }", fields = "{ 'username': 1, 'nickname': 1, 'email': 1, 'enabled': 1 }")
    Optional<UserInfo> queryBaseInfoByUsername(String username);

}
```

`@Query` 中的字段名需要使用 MongoDB 文档中的字段名，而不是 Java 实体类字段名。例如实体类中字段为 `lastLoginTime`，如果通过 `@Field("last_login_time")` 映射到 MongoDB，则 `@Query` 中应写 `last_login_time`。

下面代码演示业务层如何调用自定义查询，并对模糊查询关键字做简单处理。

文件位置：`src/main/java/io/github/atengk/service/UserInfoQueryService.java`

```java
package io.github.atengk.service;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.entity.UserInfo;
import io.github.atengk.repository.UserInfoQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息自定义查询服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoQueryService {

    private final UserInfoQueryRepository userInfoQueryRepository;

    /**
     * 根据昵称关键字模糊查询用户。
     *
     * @param keyword 昵称关键字
     * @return 用户列表
     */
    public List<UserInfo> searchByNickname(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            throw new IllegalArgumentException("昵称关键字不能为空");
        }

        String escapedKeyword = ReUtil.escape(keyword);
        List<UserInfo> users = userInfoQueryRepository.queryByNicknameLike(escapedKeyword);
        log.info("昵称模糊查询完成，关键字：{}，结果数量：{}", keyword, users.size());
        return users;
    }

    /**
     * 查询最近登录用户。
     *
     * @param days 最近天数
     * @return 用户列表
     */
    public List<UserInfo> listRecentlyLoginUsers(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("最近天数必须大于0");
        }

        LocalDateTime startTime = LocalDateTime.now().minusDays(days);
        List<UserInfo> users = userInfoQueryRepository.queryByLastLoginTimeAfter(startTime);
        log.info("最近登录用户查询完成，最近天数：{}，结果数量：{}", days, users.size());
        return users;
    }

}
```

使用正则查询时需要注意用户输入的特殊字符。如果直接将用户输入拼接到 `$regex`，可能导致匹配结果异常或查询成本过高。上面示例使用 Hutool 的 `ReUtil.escape` 对关键字进行转义，适合普通关键字模糊搜索。

`@Query` 适合固定查询，不适合大量动态条件。如果查询条件需要根据入参灵活组合，建议使用 `MongoTemplate`。

### 分页与排序

Spring Data MongoDB 支持使用 `Pageable` 和 `Sort` 完成分页与排序。Repository 方法可以直接接收 `Pageable` 参数，返回 `Page<T>`、`Slice<T>` 或 `List<T>`。

常见返回类型说明：

| 返回类型   | 说明                                             |
| ---------- | ------------------------------------------------ |
| `Page<T>`  | 包含数据列表、总数、总页数等完整分页信息         |
| `Slice<T>` | 只判断是否有下一页，不查询总数，适合大数据量翻页 |
| `List<T>`  | 只返回当前页数据，不包含分页元数据               |

定义分页查询 Repository：

文件位置：`src/main/java/io/github/atengk/repository/UserInfoPageRepository.java`

```java
package io.github.atengk.repository;

import io.github.atengk.entity.UserInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * 用户信息分页 Repository
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface UserInfoPageRepository extends MongoRepository<UserInfo, String> {

    /**
     * 根据启用状态分页查询用户。
     *
     * @param enabled 是否启用
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    Page<UserInfo> findByEnabled(Boolean enabled, Pageable pageable);

    /**
     * 根据昵称关键字分页查询用户。
     *
     * @param nickname 昵称关键字
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    Page<UserInfo> findByNicknameContaining(String nickname, Pageable pageable);

}
```

定义分页查询入参 DTO：

文件位置：`src/main/java/io/github/atengk/dto/UserInfoPageQuery.java`

```java
package io.github.atengk.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 用户信息分页查询参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class UserInfoPageQuery {

    /**
     * 页码，从 1 开始
     */
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能大于100")
    private Integer pageSize = 10;

    /**
     * 昵称关键字
     */
    private String nickname;

    /**
     * 是否启用
     */
    private Boolean enabled;

}
```

下面代码演示分页参数构建、排序字段限制和 Repository 分页查询。

文件位置：`src/main/java/io/github/atengk/service/UserInfoPageService.java`

```java
package io.github.atengk.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.dto.UserInfoPageQuery;
import io.github.atengk.entity.UserInfo;
import io.github.atengk.repository.UserInfoPageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * 用户信息分页查询服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoPageService {

    private final UserInfoPageRepository userInfoPageRepository;

    /**
     * 分页查询用户信息。
     *
     * @param query 查询参数
     * @return 用户分页结果
     */
    public Page<UserInfo> pageUsers(UserInfoPageQuery query) {
        int pageNum = query.getPageNum() == null ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 10 : query.getPageSize();

        PageRequest pageRequest = PageRequest.of(
                pageNum - 1,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        if (StrUtil.isNotBlank(query.getNickname())) {
            Page<UserInfo> page = userInfoPageRepository.findByNicknameContaining(query.getNickname(), pageRequest);
            log.info("按昵称分页查询用户完成，页码：{}，每页数量：{}，结果数量：{}", pageNum, pageSize, page.getNumberOfElements());
            return page;
        }

        Boolean enabled = query.getEnabled() == null ? Boolean.TRUE : query.getEnabled();
        Page<UserInfo> page = userInfoPageRepository.findByEnabled(enabled, pageRequest);
        log.info("按状态分页查询用户完成，启用状态：{}，页码：{}，每页数量：{}，结果数量：{}",
                enabled, pageNum, pageSize, page.getNumberOfElements());
        return page;
    }

}
```

需要注意，`PageRequest.of` 的页码从 `0` 开始，而很多前端接口习惯从 `1` 开始传入页码。因此业务层通常需要执行 `pageNum - 1`。

如果要直接使用 `Sort` 查询全部启用用户并按创建时间倒序排列，可以在 Repository 中定义如下方法：

```java
List<UserInfo> findByEnabled(Boolean enabled, Sort sort);
```

调用方式：

```java
Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
List<UserInfo> users = userInfoRepository.findByEnabled(Boolean.TRUE, sort);
```

分页接口返回时不建议直接把 `Page<UserInfo>` 原样暴露给前端，可以封装成统一分页响应对象，减少响应字段与框架结构耦合。

下面代码定义通用分页响应对象。

文件位置：`src/main/java/io/github/atengk/vo/PageResult.java`

```java
package io.github.atengk.vo;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 分页响应结果
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class PageResult<T> {

    /**
     * 当前页数据
     */
    private List<T> records;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 当前页码
     */
    private int pageNum;

    /**
     * 每页数量
     */
    private int pageSize;

    /**
     * 总页数
     */
    private int pages;

    /**
     * 是否有下一页
     */
    private boolean hasNext;

    /**
     * 根据 Spring Data Page 构建分页结果。
     *
     * @param page 分页对象
     * @param <T> 数据类型
     * @return 分页响应
     */
    public static <T> PageResult<T> of(Page<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(page.getContent());
        result.setTotal(page.getTotalElements());
        result.setPageNum(page.getNumber() + 1);
        result.setPageSize(page.getSize());
        result.setPages(page.getTotalPages());
        result.setHasNext(page.hasNext());
        return result;
    }

}
```

分页查询接口示例：

文件位置：`src/main/java/io/github/atengk/controller/UserInfoController.java`

```java
package io.github.atengk.controller;

import io.github.atengk.dto.UserInfoPageQuery;
import io.github.atengk.entity.UserInfo;
import io.github.atengk.service.UserInfoPageService;
import io.github.atengk.vo.PageResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户信息接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequiredArgsConstructor
public class UserInfoController {

    private final UserInfoPageService userInfoPageService;

    /**
     * 分页查询用户信息。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @GetMapping("/users/page")
    public PageResult<UserInfo> pageUsers(@Valid UserInfoPageQuery query) {
        Page<UserInfo> page = userInfoPageService.pageUsers(query);
        return PageResult.of(page);
    }

}
```

接口调用示例：

```bash
curl -G 'http://localhost:8080/users/page' \
  --data-urlencode 'pageNum=1' \
  --data-urlencode 'pageSize=10' \
  --data-urlencode 'nickname=阿腾' \
  --data-urlencode 'enabled=true'
```

响应示例：

```json
{
  "records": [
    {
      "id": "665f2d4f91f3a65a9f2d1001",
      "username": "ateng",
      "nickname": "阿腾",
      "age": 28,
      "email": "ateng@example.com",
      "mobile": "13800000000",
      "enabled": true,
      "tags": ["java", "springboot", "mongodb"],
      "loginCount": 18,
      "createdAt": "2026-05-13T09:00:00",
      "updatedAt": "2026-05-13T10:30:00"
    }
  ],
  "total": 1,
  "pageNum": 1,
  "pageSize": 10,
  "pages": 1,
  "hasNext": false
}
```

分页与排序注意事项：

| 注意事项                 | 说明                                           |
| ------------------------ | ---------------------------------------------- |
| 页码转换                 | 前端页码通常从 1 开始，`PageRequest` 从 0 开始 |
| 限制最大页大小           | 防止一次查询过多数据造成内存压力               |
| 排序字段应有索引         | 高频排序字段建议建立索引                       |
| 大深度分页需谨慎         | 页码很大时 skip 成本较高                       |
| 大数据滚动查询用 Slice   | 不需要总数时可使用 `Slice` 降低 count 成本     |
| 复杂分页用 MongoTemplate | 多条件动态分页更适合模板方式构建查询           |

Repository 适合常规 CRUD 和简单查询，是 Spring Data MongoDB 开发中最直接的方式。但 Repository 的优势是简洁，不是复杂查询能力。后续涉及动态条件、部分字段更新、批量更新、聚合统计时，应优先使用 `MongoTemplate`。



## MongoTemplate 开发

`MongoTemplate` 是 Spring Data MongoDB 中面向命令式 MongoDB 操作的核心模板类，适合处理复杂查询、动态条件、部分字段更新、批量更新、删除和聚合统计。Spring Data 官方文档说明，`MongoTemplate` 提供创建、更新、删除、查询文档的便捷操作，并负责领域对象与 MongoDB 文档之间的映射。([Home](https://docs.spring.io/spring-data/mongodb/docs/current-SNAPSHOT/reference/html/?utm_source=chatgpt.com))

与 `MongoRepository` 相比，`MongoTemplate` 的代码量更大，但控制力更强。Repository 更适合固定查询和标准 CRUD，`MongoTemplate` 更适合需要手动拼接 `Query`、`Criteria`、`Update`、`Aggregation` 的业务场景。

### 新增与保存文档

新增文档通常使用 `insert` 或 `save`。`insert` 更偏向明确的新增操作，如果主键已存在会抛出重复键异常；`save` 则根据主键判断是新增还是保存已有文档，适合“有则保存、无则新增”的场景。官方文档也将 `MongoTemplate` 的保存、更新、删除作为核心 CRUD 能力进行说明。([Home](https://docs.spring.io/spring-data/mongodb/reference/mongodb/template-crud-operations.html?utm_source=chatgpt.com))

常用方法说明：

| 方法                                                      | 说明                             |
| --------------------------------------------------------- | -------------------------------- |
| `insert(Object objectToSave)`                             | 新增单个文档                     |
| `insert(Collection<?> batchToSave, Class<?> entityClass)` | 批量新增文档                     |
| `save(Object objectToSave)`                               | 根据主键保存文档，主键存在时更新 |
| `insertAll(Collection<?> objectsToSave)`                  | 批量新增多个对象                 |

下面代码演示用户文档的新增、批量新增和保存操作。

文件位置：`src/main/java/io/github/atengk/service/UserInfoTemplateSaveService.java`

```java
package io.github.atengk.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.entity.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息 MongoTemplate 保存服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoTemplateSaveService {

    private final MongoTemplate mongoTemplate;

    /**
     * 新增单个用户文档。
     *
     * @param userInfo 用户信息
     * @return 保存后的用户信息
     */
    public UserInfo insertUser(UserInfo userInfo) {
        if (StrUtil.isBlank(userInfo.getUsername())) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        userInfo.setId(null);
        userInfo.setEnabled(Boolean.TRUE);
        userInfo.setLoginCount(0);
        userInfo.setCreatedAt(now);
        userInfo.setUpdatedAt(now);

        UserInfo savedUser = mongoTemplate.insert(userInfo);
        log.info("MongoTemplate 新增用户成功，用户ID：{}，用户名：{}", savedUser.getId(), savedUser.getUsername());
        return savedUser;
    }

    /**
     * 批量新增用户文档。
     *
     * @param users 用户列表
     * @return 保存后的用户列表
     */
    public List<UserInfo> insertUsers(List<UserInfo> users) {
        if (CollUtil.isEmpty(users)) {
            throw new IllegalArgumentException("用户列表不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        users.forEach(user -> {
            user.setId(null);
            user.setEnabled(user.getEnabled() == null ? Boolean.TRUE : user.getEnabled());
            user.setLoginCount(user.getLoginCount() == null ? 0 : user.getLoginCount());
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
        });

        List<UserInfo> savedUsers = (List<UserInfo>) mongoTemplate.insert(users, UserInfo.class);
        log.info("MongoTemplate 批量新增用户成功，数量：{}", savedUsers.size());
        return savedUsers;
    }

    /**
     * 保存用户文档。
     *
     * @param userInfo 用户信息
     * @return 保存后的用户信息
     */
    public UserInfo saveUser(UserInfo userInfo) {
        if (StrUtil.isBlank(userInfo.getUsername())) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        userInfo.setUpdatedAt(LocalDateTime.now());
        UserInfo savedUser = mongoTemplate.save(userInfo);
        log.info("MongoTemplate 保存用户成功，用户ID：{}，用户名：{}", savedUser.getId(), savedUser.getUsername());
        return savedUser;
    }

}
```

`insert` 和 `save` 的使用建议如下：

| 场景                 | 推荐方法                      | 说明                                         |
| -------------------- | ----------------------------- | -------------------------------------------- |
| 明确新增数据         | `insert`                      | 主键重复时直接失败，便于发现重复写入问题     |
| 批量初始化数据       | `insert(Collection, Class)`   | 适合批量导入或初始化                         |
| 根据主键保存完整对象 | `save`                        | 有主键时更新，无主键时新增                   |
| 只更新部分字段       | `updateFirst` / `updateMulti` | 不建议为了改一个字段使用 `save` 覆盖整个文档 |

需要注意，`save` 更接近完整对象保存。如果实体对象字段不完整，可能导致原文档字段被覆盖或丢失。对于“只更新昵称”“只增加登录次数”“只追加标签”等局部更新场景，应使用 `Update`。

### 条件查询

条件查询通常由 `Query` 和 `Criteria` 组合完成。`Criteria` 用于描述字段条件，`Query` 用于承载条件、排序、分页、字段投影等查询配置。

常用查询方法：

| 方法                       | 说明                   |
| -------------------------- | ---------------------- |
| `findById`                 | 根据主键查询           |
| `findOne`                  | 查询单条文档           |
| `find`                     | 查询多条文档           |
| `exists`                   | 判断文档是否存在       |
| `count`                    | 统计符合条件的文档数量 |
| `query.fields().include()` | 指定返回字段           |
| `query.with(Sort)`         | 指定排序               |
| `query.skip().limit()`     | 指定分页               |

下面代码演示常见条件查询，包括单条查询、多条件查询、字段投影、排序和分页。

文件位置：`src/main/java/io/github/atengk/service/UserInfoTemplateQueryService.java`

```java
package io.github.atengk.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.entity.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息 MongoTemplate 查询服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoTemplateQueryService {

    private final MongoTemplate mongoTemplate;

    /**
     * 根据 ID 查询用户。
     *
     * @param id 用户ID
     * @return 用户信息
     */
    public UserInfo getById(String id) {
        if (StrUtil.isBlank(id)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        UserInfo userInfo = mongoTemplate.findById(id, UserInfo.class);
        if (userInfo == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        return userInfo;
    }

    /**
     * 根据用户名查询单个用户。
     *
     * @param username 用户名
     * @return 用户信息
     */
    public UserInfo getByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        Query query = Query.query(Criteria.where("username").is(username));
        UserInfo userInfo = mongoTemplate.findOne(query, UserInfo.class);
        if (userInfo == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        return userInfo;
    }

    /**
     * 查询指定时间之后创建的启用用户。
     *
     * @param startTime 开始时间
     * @return 用户列表
     */
    public List<UserInfo> listEnabledUsersAfter(LocalDateTime startTime) {
        Query query = Query.query(
                Criteria.where("enabled").is(Boolean.TRUE)
                        .and("createdAt").gte(startTime)
        ).with(Sort.by(Sort.Direction.DESC, "createdAt"));

        List<UserInfo> users = mongoTemplate.find(query, UserInfo.class);
        log.info("查询启用用户完成，开始时间：{}，结果数量：{}", startTime, users.size());
        return users;
    }

    /**
     * 查询用户基础字段。
     *
     * @param username 用户名
     * @return 用户信息
     */
    public UserInfo getBaseInfoByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        Query query = Query.query(Criteria.where("username").is(username));
        query.fields()
                .include("username")
                .include("nickname")
                .include("email")
                .include("enabled")
                .exclude("address")
                .exclude("tags");

        return mongoTemplate.findOne(query, UserInfo.class);
    }

    /**
     * 分页查询启用用户。
     *
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @return 用户列表
     */
    public List<UserInfo> pageEnabledUsers(int pageNum, int pageSize) {
        if (pageNum < 1) {
            throw new IllegalArgumentException("页码不能小于1");
        }
        if (pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("每页数量必须在 1 到 100 之间");
        }

        Query query = Query.query(Criteria.where("enabled").is(Boolean.TRUE))
                .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                .skip((long) (pageNum - 1) * pageSize)
                .limit(pageSize);

        List<UserInfo> users = mongoTemplate.find(query, UserInfo.class);
        log.info("分页查询启用用户完成，页码：{}，每页数量：{}，结果数量：{}", pageNum, pageSize, users.size());
        return users;
    }

    /**
     * 统计启用用户数量。
     *
     * @return 用户数量
     */
    public long countEnabledUsers() {
        Query query = Query.query(Criteria.where("enabled").is(Boolean.TRUE));
        return mongoTemplate.count(query, UserInfo.class);
    }

    /**
     * 判断用户名是否存在。
     *
     * @param username 用户名
     * @return 是否存在
     */
    public boolean existsByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            return false;
        }

        Query query = Query.query(Criteria.where("username").is(username));
        return mongoTemplate.exists(query, UserInfo.class);
    }

}
```

在 `MongoTemplate` 查询中，`Criteria.where("username")` 里的字段通常写实体类字段名，Spring Data MongoDB 会根据映射关系转换为 MongoDB 文档字段。对于 `@Field("created_at")` 这类映射字段，使用实体属性名 `createdAt` 更利于和 Java 代码保持一致。

### 更新操作

更新操作通过 `Query` 指定匹配条件，通过 `Update` 指定更新内容。Spring Data MongoDB 支持更新第一条、更新多条、插入或更新、查询并修改等操作。官方文档说明，`updateFirst` 用于更新第一条匹配文档，`updateMulti` 或 fluent API 的 `all` 用于更新所有匹配文档。([Home](https://docs.spring.io/spring-data/mongodb/reference/mongodb/template-crud-operations.html?utm_source=chatgpt.com))

常用更新方法：

| 方法              | 说明                                 |
| ----------------- | ------------------------------------ |
| `updateFirst`     | 更新第一条匹配文档                   |
| `updateMulti`     | 更新所有匹配文档                     |
| `upsert`          | 存在则更新，不存在则新增             |
| `findAndModify`   | 查询并更新，返回更新前或更新后的文档 |
| `Update.set`      | 设置字段                             |
| `Update.inc`      | 数值递增或递减                       |
| `Update.unset`    | 删除字段                             |
| `Update.push`     | 向数组追加元素                       |
| `Update.addToSet` | 向数组追加不重复元素                 |
| `Update.pull`     | 从数组移除元素                       |

下面代码演示部分字段更新、批量更新、登录次数递增、标签维护和 upsert 操作。

文件位置：`src/main/java/io/github/atengk/service/UserInfoTemplateUpdateService.java`

```java
package io.github.atengk.service;

import cn.hutool.core.util.StrUtil;
import com.mongodb.client.result.UpdateResult;
import io.github.atengk.entity.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户信息 MongoTemplate 更新服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoTemplateUpdateService {

    private final MongoTemplate mongoTemplate;

    /**
     * 更新用户昵称。
     *
     * @param id 用户ID
     * @param nickname 昵称
     * @return 是否更新成功
     */
    public boolean updateNickname(String id, String nickname) {
        if (StrUtil.isBlank(id)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (StrUtil.isBlank(nickname)) {
            throw new IllegalArgumentException("昵称不能为空");
        }

        Query query = Query.query(Criteria.where("_id").is(id));
        Update update = new Update()
                .set("nickname", nickname)
                .set("updatedAt", LocalDateTime.now());

        UpdateResult result = mongoTemplate.updateFirst(query, update, UserInfo.class);
        log.info("用户昵称更新完成，用户ID：{}，匹配数量：{}，修改数量：{}",
                id, result.getMatchedCount(), result.getModifiedCount());
        return result.getModifiedCount() > 0;
    }

    /**
     * 批量禁用长期未登录用户。
     *
     * @param lastLoginTime 最后登录时间阈值
     * @return 修改数量
     */
    public long disableInactiveUsers(LocalDateTime lastLoginTime) {
        Query query = Query.query(
                Criteria.where("enabled").is(Boolean.TRUE)
                        .and("lastLoginTime").lt(lastLoginTime)
        );

        Update update = new Update()
                .set("enabled", Boolean.FALSE)
                .set("updatedAt", LocalDateTime.now());

        UpdateResult result = mongoTemplate.updateMulti(query, update, UserInfo.class);
        log.info("长期未登录用户禁用完成，时间阈值：{}，匹配数量：{}，修改数量：{}",
                lastLoginTime, result.getMatchedCount(), result.getModifiedCount());
        return result.getModifiedCount();
    }

    /**
     * 记录用户登录信息。
     *
     * @param username 用户名
     * @return 更新后的用户信息
     */
    public UserInfo recordLogin(String username) {
        if (StrUtil.isBlank(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        Query query = Query.query(Criteria.where("username").is(username));
        Update update = new Update()
                .inc("loginCount", 1)
                .set("lastLoginTime", LocalDateTime.now())
                .set("updatedAt", LocalDateTime.now());

        FindAndModifyOptions options = FindAndModifyOptions.options()
                .returnNew(true)
                .upsert(false);

        UserInfo userInfo = mongoTemplate.findAndModify(query, update, options, UserInfo.class);
        if (userInfo == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        log.info("用户登录信息更新成功，用户ID：{}，用户名：{}，登录次数：{}",
                userInfo.getId(), userInfo.getUsername(), userInfo.getLoginCount());
        return userInfo;
    }

    /**
     * 添加用户标签，重复标签不会再次添加。
     *
     * @param username 用户名
     * @param tag 标签
     * @return 是否更新成功
     */
    public boolean addTag(String username, String tag) {
        if (StrUtil.hasBlank(username, tag)) {
            throw new IllegalArgumentException("用户名和标签不能为空");
        }

        Query query = Query.query(Criteria.where("username").is(username));
        Update update = new Update()
                .addToSet("tags", tag)
                .set("updatedAt", LocalDateTime.now());

        UpdateResult result = mongoTemplate.updateFirst(query, update, UserInfo.class);
        log.info("用户标签添加完成，用户名：{}，标签：{}，修改数量：{}", username, tag, result.getModifiedCount());
        return result.getModifiedCount() > 0;
    }

    /**
     * 移除用户标签。
     *
     * @param username 用户名
     * @param tag 标签
     * @return 是否更新成功
     */
    public boolean removeTag(String username, String tag) {
        if (StrUtil.hasBlank(username, tag)) {
            throw new IllegalArgumentException("用户名和标签不能为空");
        }

        Query query = Query.query(Criteria.where("username").is(username));
        Update update = new Update()
                .pull("tags", tag)
                .set("updatedAt", LocalDateTime.now());

        UpdateResult result = mongoTemplate.updateFirst(query, update, UserInfo.class);
        log.info("用户标签移除完成，用户名：{}，标签：{}，修改数量：{}", username, tag, result.getModifiedCount());
        return result.getModifiedCount() > 0;
    }

    /**
     * 保存或更新系统内置用户。
     *
     * @param username 用户名
     * @param nickname 昵称
     * @return 是否完成写入
     */
    public boolean upsertSystemUser(String username, String nickname) {
        if (StrUtil.hasBlank(username, nickname)) {
            throw new IllegalArgumentException("用户名和昵称不能为空");
        }

        Query query = Query.query(Criteria.where("username").is(username));
        Update update = new Update()
                .setOnInsert("username", username)
                .setOnInsert("createdAt", LocalDateTime.now())
                .set("nickname", nickname)
                .set("enabled", Boolean.TRUE)
                .set("updatedAt", LocalDateTime.now());

        UpdateResult result = mongoTemplate.upsert(query, update, UserInfo.class);
        log.info("系统用户 upsert 完成，用户名：{}，匹配数量：{}，修改数量：{}，新增ID：{}",
                username, result.getMatchedCount(), result.getModifiedCount(), result.getUpsertedId());
        return result.wasAcknowledged();
    }

}
```

更新操作建议优先使用局部更新，而不是先查询实体、修改字段、再 `save`。局部更新只修改指定字段，网络传输和写入成本更低，也能减少误覆盖字段的风险。

需要特别注意字段名。使用 `Update.set("updatedAt", value)` 时建议写 Java 实体属性名，Spring Data 会结合实体映射进行转换。如果直接操作集合名或原生 `Document`，则应使用 MongoDB 中实际字段名。

### 删除操作

删除操作同样使用 `Query` 指定条件。常见方式包括根据 ID 删除、根据条件删除、查询并删除。删除操作属于破坏性操作，业务系统中更常见的是逻辑删除，即通过 `deleted`、`enabled`、`status` 等字段标记数据状态。

常用删除方法：

| 方法                             | 说明                       |
| -------------------------------- | -------------------------- |
| `remove(Query, Class)`           | 删除匹配条件的文档         |
| `findAndRemove(Query, Class)`    | 查询并删除第一条匹配文档   |
| `findAllAndRemove(Query, Class)` | 查询并删除所有匹配文档     |
| `dropCollection(Class)`          | 删除整个集合，生产环境慎用 |

下面代码演示按 ID 删除、按状态删除和查询后删除。

文件位置：`src/main/java/io/github/atengk/service/UserInfoTemplateDeleteService.java`

```java
package io.github.atengk.service;

import cn.hutool.core.util.StrUtil;
import com.mongodb.client.result.DeleteResult;
import io.github.atengk.entity.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息 MongoTemplate 删除服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoTemplateDeleteService {

    private final MongoTemplate mongoTemplate;

    /**
     * 根据 ID 删除用户。
     *
     * @param id 用户ID
     * @return 是否删除成功
     */
    public boolean deleteById(String id) {
        if (StrUtil.isBlank(id)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        Query query = Query.query(Criteria.where("_id").is(id));
        DeleteResult result = mongoTemplate.remove(query, UserInfo.class);
        log.info("根据 ID 删除用户完成，用户ID：{}，删除数量：{}", id, result.getDeletedCount());
        return result.getDeletedCount() > 0;
    }

    /**
     * 删除指定时间之前的禁用用户。
     *
     * @param updatedAt 更新时间阈值
     * @return 删除数量
     */
    public long deleteDisabledUsersBefore(LocalDateTime updatedAt) {
        Query query = Query.query(
                Criteria.where("enabled").is(Boolean.FALSE)
                        .and("updatedAt").lt(updatedAt)
        );

        DeleteResult result = mongoTemplate.remove(query, UserInfo.class);
        log.info("删除历史禁用用户完成，时间阈值：{}，删除数量：{}", updatedAt, result.getDeletedCount());
        return result.getDeletedCount();
    }

    /**
     * 查询并删除指定用户名的用户。
     *
     * @param username 用户名
     * @return 被删除的用户信息
     */
    public UserInfo findAndRemoveByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        Query query = Query.query(Criteria.where("username").is(username));
        UserInfo removedUser = mongoTemplate.findAndRemove(query, UserInfo.class);
        if (removedUser == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        log.info("查询并删除用户完成，用户ID：{}，用户名：{}", removedUser.getId(), removedUser.getUsername());
        return removedUser;
    }

    /**
     * 查询并删除所有禁用用户。
     *
     * @return 被删除的用户列表
     */
    public List<UserInfo> findAllAndRemoveDisabledUsers() {
        Query query = Query.query(Criteria.where("enabled").is(Boolean.FALSE));
        List<UserInfo> removedUsers = mongoTemplate.findAllAndRemove(query, UserInfo.class);
        log.info("查询并删除禁用用户完成，删除数量：{}", removedUsers.size());
        return removedUsers;
    }

}
```

生产环境中不建议随意执行物理删除。核心业务数据、审计数据、订单数据等通常应采用逻辑删除或归档策略。对于日志、验证码、临时消息等生命周期明确的数据，可以配合 TTL 索引自动清理。

### 聚合查询

聚合查询用于完成分组统计、字段投影、条件过滤、排序、分页、数组展开等操作。Spring Data MongoDB 通过 `Aggregation`、`AggregationOperation`、`AggregationResults` 等对象封装 MongoDB Aggregation Pipeline，最终由 `MongoTemplate.aggregate` 执行。官方文档说明，Spring Data MongoDB 的聚合框架支持基于 `Aggregation`、`AggregationDefinition`、`AggregationResults` 等抽象构建聚合操作。([Home](https://docs.spring.io/spring-data/mongodb/docs/current-SNAPSHOT/reference/html/?utm_source=chatgpt.com))

常用聚合阶段：

| 阶段      | 说明                     |
| --------- | ------------------------ |
| `match`   | 过滤文档，类似查询条件   |
| `group`   | 分组统计                 |
| `project` | 字段投影和字段重命名     |
| `sort`    | 排序                     |
| `skip`    | 跳过指定数量             |
| `limit`   | 限制返回数量             |
| `unwind`  | 展开数组字段             |
| `lookup`  | 集合关联查询，不建议滥用 |

下面先定义用户标签统计响应对象。

文件位置：`src/main/java/io/github/atengk/vo/UserTagStatisticsVO.java`

```java
package io.github.atengk.vo;

import lombok.Data;

/**
 * 用户标签统计响应对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class UserTagStatisticsVO {

    /**
     * 标签名称
     */
    private String tag;

    /**
     * 用户数量
     */
    private Long userCount;

}
```

下面代码演示按标签统计启用用户数量、按城市统计用户数量、按年龄段统计用户数量。

文件位置：`src/main/java/io/github/atengk/service/UserInfoTemplateAggregationService.java`

```java
package io.github.atengk.service;

import io.github.atengk.entity.UserInfo;
import io.github.atengk.vo.UserTagStatisticsVO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户信息 MongoTemplate 聚合统计服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoTemplateAggregationService {

    private final MongoTemplate mongoTemplate;

    /**
     * 按标签统计启用用户数量。
     *
     * @return 标签统计列表
     */
    public List<UserTagStatisticsVO> statisticsByTag() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("enabled").is(Boolean.TRUE)),
                Aggregation.unwind("tags"),
                Aggregation.group("tags").count().as("userCount"),
                Aggregation.project("userCount").and("_id").as("tag").andExclude("_id"),
                Aggregation.sort(Sort.Direction.DESC, "userCount")
        );

        AggregationResults<UserTagStatisticsVO> results = mongoTemplate.aggregate(
                aggregation,
                UserInfo.class,
                UserTagStatisticsVO.class
        );

        List<UserTagStatisticsVO> list = results.getMappedResults();
        log.info("按标签统计用户数量完成，结果数量：{}", list.size());
        return list;
    }

    /**
     * 按城市统计启用用户数量。
     *
     * @return 城市统计列表
     */
    public List<CityStatisticsVO> statisticsByCity() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("enabled").is(Boolean.TRUE)),
                Aggregation.group("address.city").count().as("userCount"),
                Aggregation.project("userCount").and("_id").as("city").andExclude("_id"),
                Aggregation.sort(Sort.Direction.DESC, "userCount")
        );

        AggregationResults<CityStatisticsVO> results = mongoTemplate.aggregate(
                aggregation,
                UserInfo.class,
                CityStatisticsVO.class
        );

        List<CityStatisticsVO> list = results.getMappedResults();
        log.info("按城市统计用户数量完成，结果数量：{}", list.size());
        return list;
    }

    /**
     * 按年龄段统计启用用户数量。
     *
     * @return 年龄段统计列表
     */
    public List<AgeRangeStatisticsVO> statisticsByAgeRange() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("enabled").is(Boolean.TRUE)),
                Aggregation.project("age")
                        .and(
                                ConditionalOperators.switchCases(
                                                ConditionalOperators.Switch.CaseOperator
                                                        .when(Criteria.where("age").lt(18)).then("18岁以下"),
                                                ConditionalOperators.Switch.CaseOperator
                                                        .when(Criteria.where("age").gte(18).lt(30)).then("18-29岁"),
                                                ConditionalOperators.Switch.CaseOperator
                                                        .when(Criteria.where("age").gte(30).lt(45)).then("30-44岁"),
                                                ConditionalOperators.Switch.CaseOperator
                                                        .when(Criteria.where("age").gte(45)).then("45岁及以上")
                                        )
                                        .defaultTo("未知")
                        ).as("ageRange"),
                Aggregation.group("ageRange").count().as("userCount"),
                Aggregation.project("userCount").and("_id").as("ageRange").andExclude("_id"),
                Aggregation.sort(Sort.Direction.DESC, "userCount")
        );

        AggregationResults<AgeRangeStatisticsVO> results = mongoTemplate.aggregate(
                aggregation,
                UserInfo.class,
                AgeRangeStatisticsVO.class
        );

        List<AgeRangeStatisticsVO> list = results.getMappedResults();
        log.info("按年龄段统计用户数量完成，结果数量：{}", list.size());
        return list;
    }

    /**
     * 城市统计响应对象
     *
     * @author Ateng
     * @since 2026-05-13
     */
    @Data
    public static class CityStatisticsVO {

        /**
         * 城市
         */
        private String city;

        /**
         * 用户数量
         */
        private Long userCount;

    }

    /**
     * 年龄段统计响应对象
     *
     * @author Ateng
     * @since 2026-05-13
     */
    @Data
    public static class AgeRangeStatisticsVO {

        /**
         * 年龄段
         */
        private String ageRange;

        /**
         * 用户数量
         */
        private Long userCount;

    }

}
```

聚合查询适合统计分析，但不建议把所有复杂报表都压到 MongoDB 中完成。对于大规模 OLAP 场景，应考虑 ClickHouse、Doris、StarRocks 等分析型数据库。MongoDB 聚合更适合业务库内的轻量统计、中间结果生成和有限规模的数据分析。

## 常用查询场景

本节整理 Spring Boot 3 使用 `MongoTemplate` 开发时最常见的查询写法，包括精确查询、模糊查询、范围查询、嵌套字段查询和数组字段查询。这些查询都可以通过 `Query` 与 `Criteria` 组合实现，适合在业务服务层中按入参动态构建。

### 精确查询

精确查询用于根据字段值完全匹配文档，例如根据用户名、手机号、状态、业务编号查询数据。精确查询通常应配合索引使用，尤其是用户名、手机号、订单号、配置编码等高频查询字段。

常见写法：

```java
Query query = Query.query(Criteria.where("username").is("ateng"));
UserInfo userInfo = mongoTemplate.findOne(query, UserInfo.class);
```

下面代码演示多个精确查询场景。

文件位置：`src/main/java/io/github/atengk/service/UserInfoExactQueryService.java`

```java
package io.github.atengk.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.entity.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 用户信息精确查询服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoExactQueryService {

    private final MongoTemplate mongoTemplate;

    /**
     * 根据用户名精确查询。
     *
     * @param username 用户名
     * @return 用户信息
     */
    public UserInfo getByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        Query query = Query.query(Criteria.where("username").is(username));
        return mongoTemplate.findOne(query, UserInfo.class);
    }

    /**
     * 根据用户名和启用状态查询。
     *
     * @param username 用户名
     * @param enabled 是否启用
     * @return 用户信息
     */
    public UserInfo getByUsernameAndEnabled(String username, Boolean enabled) {
        if (StrUtil.isBlank(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        Query query = Query.query(
                Criteria.where("username").is(username)
                        .and("enabled").is(enabled)
        );
        return mongoTemplate.findOne(query, UserInfo.class);
    }

    /**
     * 根据用户名集合查询。
     *
     * @param usernames 用户名集合
     * @return 用户列表
     */
    public List<UserInfo> listByUsernames(Collection<String> usernames) {
        if (CollUtil.isEmpty(usernames)) {
            throw new IllegalArgumentException("用户名集合不能为空");
        }

        Query query = Query.query(Criteria.where("username").in(usernames));
        List<UserInfo> users = mongoTemplate.find(query, UserInfo.class);
        log.info("根据用户名集合查询完成，查询数量：{}，结果数量：{}", usernames.size(), users.size());
        return users;
    }

}
```

精确查询建议为高频字段建立索引。例如 `username` 如果要求唯一，应建立唯一索引；`enabled` 单独作为索引价值通常有限，但可以和 `createdAt`、`lastLoginTime` 等字段组成复合索引。

### 模糊查询

模糊查询通常使用正则表达式实现，适合昵称、标题、描述、配置名称等文本字段的简单搜索。对于大文本全文搜索，不建议长期依赖 `$regex`，应考虑 MongoDB Text Index、Elasticsearch、OpenSearch 或其他搜索引擎。

常见写法：

```java
Query query = Query.query(Criteria.where("nickname").regex("阿腾", "i"));
List<UserInfo> users = mongoTemplate.find(query, UserInfo.class);
```

下面代码对用户输入关键字进行正则转义，避免特殊字符影响查询逻辑。

文件位置：`src/main/java/io/github/atengk/service/UserInfoFuzzyQueryService.java`

```java
package io.github.atengk.service;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.entity.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户信息模糊查询服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoFuzzyQueryService {

    private final MongoTemplate mongoTemplate;

    /**
     * 根据昵称关键字模糊查询。
     *
     * @param keyword 昵称关键字
     * @return 用户列表
     */
    public List<UserInfo> searchByNickname(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            throw new IllegalArgumentException("昵称关键字不能为空");
        }

        String escapedKeyword = ReUtil.escape(keyword);
        Query query = Query.query(Criteria.where("nickname").regex(escapedKeyword, "i"))
                .with(Sort.by(Sort.Direction.DESC, "createdAt"));

        List<UserInfo> users = mongoTemplate.find(query, UserInfo.class);
        log.info("昵称模糊查询完成，关键字：{}，结果数量：{}", keyword, users.size());
        return users;
    }

    /**
     * 根据用户名或昵称模糊查询。
     *
     * @param keyword 关键字
     * @return 用户列表
     */
    public List<UserInfo> searchByUsernameOrNickname(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            throw new IllegalArgumentException("搜索关键字不能为空");
        }

        String escapedKeyword = ReUtil.escape(keyword);
        Criteria criteria = new Criteria().orOperator(
                Criteria.where("username").regex(escapedKeyword, "i"),
                Criteria.where("nickname").regex(escapedKeyword, "i")
        );

        Query query = Query.query(criteria)
                .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                .limit(50);

        List<UserInfo> users = mongoTemplate.find(query, UserInfo.class);
        log.info("用户名或昵称模糊查询完成，关键字：{}，结果数量：{}", keyword, users.size());
        return users;
    }

}
```

模糊查询注意事项：

| 注意事项                    | 说明                                        |
| --------------------------- | ------------------------------------------- |
| 用户输入需要转义            | 防止正则特殊字符改变查询含义                |
| 避免无上限查询              | 模糊查询建议设置 `limit` 或分页             |
| 谨慎使用前置模糊            | 类似 `.*keyword` 的查询通常难以有效利用索引 |
| 大文本搜索不要依赖 `$regex` | 更适合使用搜索引擎或全文索引                |
| 高频模糊查询要评估性能      | 数据量大时需要专项压测                      |

### 范围查询

范围查询常用于时间、金额、年龄、数量、分数等字段。常用操作符包括 `gt`、`gte`、`lt`、`lte`。范围查询字段通常需要建立索引，否则数据量变大后查询性能会明显下降。

常见写法：

```java
Query query = Query.query(
        Criteria.where("age").gte(18).lte(30)
);
List<UserInfo> users = mongoTemplate.find(query, UserInfo.class);
```

下面代码演示年龄范围、创建时间范围和登录次数范围查询。

文件位置：`src/main/java/io/github/atengk/service/UserInfoRangeQueryService.java`

```java
package io.github.atengk.service;

import io.github.atengk.entity.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息范围查询服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoRangeQueryService {

    private final MongoTemplate mongoTemplate;

    /**
     * 根据年龄范围查询用户。
     *
     * @param minAge 最小年龄
     * @param maxAge 最大年龄
     * @return 用户列表
     */
    public List<UserInfo> listByAgeRange(Integer minAge, Integer maxAge) {
        if (minAge == null || maxAge == null || minAge > maxAge) {
            throw new IllegalArgumentException("年龄范围不合法");
        }

        Query query = Query.query(Criteria.where("age").gte(minAge).lte(maxAge))
                .with(Sort.by(Sort.Direction.ASC, "age"));

        List<UserInfo> users = mongoTemplate.find(query, UserInfo.class);
        log.info("按年龄范围查询用户完成，最小年龄：{}，最大年龄：{}，结果数量：{}", minAge, maxAge, users.size());
        return users;
    }

    /**
     * 根据创建时间范围查询用户。
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 用户列表
     */
    public List<UserInfo> listByCreatedAtRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null || startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("创建时间范围不合法");
        }

        Query query = Query.query(Criteria.where("createdAt").gte(startTime).lte(endTime))
                .with(Sort.by(Sort.Direction.DESC, "createdAt"));

        List<UserInfo> users = mongoTemplate.find(query, UserInfo.class);
        log.info("按创建时间范围查询用户完成，开始时间：{}，结束时间：{}，结果数量：{}",
                startTime, endTime, users.size());
        return users;
    }

    /**
     * 查询登录次数大于等于指定次数的用户。
     *
     * @param loginCount 登录次数
     * @return 用户列表
     */
    public List<UserInfo> listByLoginCountGreaterThan(Integer loginCount) {
        if (loginCount == null || loginCount < 0) {
            throw new IllegalArgumentException("登录次数不能小于0");
        }

        Query query = Query.query(Criteria.where("loginCount").gte(loginCount))
                .with(Sort.by(Sort.Direction.DESC, "loginCount"));

        List<UserInfo> users = mongoTemplate.find(query, UserInfo.class);
        log.info("按登录次数范围查询用户完成，登录次数阈值：{}，结果数量：{}", loginCount, users.size());
        return users;
    }

}
```

范围查询通常与排序一起出现。例如按 `createdAt` 范围查询并按 `createdAt` 倒序返回，此时应考虑在 `createdAt` 上建立索引。如果还有状态条件，例如 `enabled = true`，可以考虑建立 `{ enabled: 1, created_at: -1 }` 这类复合索引。

### 嵌套字段查询

MongoDB 支持嵌套文档，查询嵌套字段时使用点路径，例如 `address.city`。在 Spring Data MongoDB 中，可以通过 `Criteria.where("address.city")` 查询用户地址中的城市字段。

常见文档结构：

```json
{
  "username": "ateng",
  "address": {
    "province": "浙江省",
    "city": "杭州市",
    "detail": "西湖区文三路"
  }
}
```

查询城市为“杭州市”的用户：

```java
Query query = Query.query(Criteria.where("address.city").is("杭州市"));
List<UserInfo> users = mongoTemplate.find(query, UserInfo.class);
```

下面代码演示嵌套字段的精确查询、组合查询和字段存在性查询。

文件位置：`src/main/java/io/github/atengk/service/UserInfoNestedQueryService.java`

```java
package io.github.atengk.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.entity.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户信息嵌套字段查询服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoNestedQueryService {

    private final MongoTemplate mongoTemplate;

    /**
     * 根据城市查询用户。
     *
     * @param city 城市
     * @return 用户列表
     */
    public List<UserInfo> listByCity(String city) {
        if (StrUtil.isBlank(city)) {
            throw new IllegalArgumentException("城市不能为空");
        }

        Query query = Query.query(Criteria.where("address.city").is(city))
                .with(Sort.by(Sort.Direction.DESC, "createdAt"));

        List<UserInfo> users = mongoTemplate.find(query, UserInfo.class);
        log.info("按城市查询用户完成，城市：{}，结果数量：{}", city, users.size());
        return users;
    }

    /**
     * 根据省份和城市查询用户。
     *
     * @param province 省份
     * @param city 城市
     * @return 用户列表
     */
    public List<UserInfo> listByProvinceAndCity(String province, String city) {
        if (StrUtil.hasBlank(province, city)) {
            throw new IllegalArgumentException("省份和城市不能为空");
        }

        Query query = Query.query(
                Criteria.where("address.province").is(province)
                        .and("address.city").is(city)
        ).with(Sort.by(Sort.Direction.DESC, "createdAt"));

        List<UserInfo> users = mongoTemplate.find(query, UserInfo.class);
        log.info("按省份和城市查询用户完成，省份：{}，城市：{}，结果数量：{}", province, city, users.size());
        return users;
    }

    /**
     * 查询已填写地址的用户。
     *
     * @return 用户列表
     */
    public List<UserInfo> listUsersWithAddress() {
        Query query = Query.query(Criteria.where("address.city").exists(true));
        List<UserInfo> users = mongoTemplate.find(query, UserInfo.class);
        log.info("查询已填写地址的用户完成，结果数量：{}", users.size());
        return users;
    }

}
```

嵌套字段查询建议控制嵌套层级。一般业务文档嵌套 1 到 2 层较容易维护，过深嵌套会增加查询、更新、索引设计和前后端协作成本。

### 数组字段查询

MongoDB 支持数组字段查询，例如用户标签、角色编码、权限标识、商品明细、操作记录等。简单数组可以直接使用字段名匹配元素，复杂对象数组可以使用点路径或 `elemMatch` 查询。

常见文档结构：

```json
{
  "username": "ateng",
  "tags": ["java", "springboot", "mongodb"]
}
```

查询包含 `mongodb` 标签的用户：

```java
Query query = Query.query(Criteria.where("tags").is("mongodb"));
List<UserInfo> users = mongoTemplate.find(query, UserInfo.class);
```

下面代码演示数组字段的单元素匹配、多元素匹配、全部包含和数组大小查询。

文件位置：`src/main/java/io/github/atengk/service/UserInfoArrayQueryService.java`

```java
package io.github.atengk.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.entity.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 用户信息数组字段查询服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoArrayQueryService {

    private final MongoTemplate mongoTemplate;

    /**
     * 查询包含指定标签的用户。
     *
     * @param tag 标签
     * @return 用户列表
     */
    public List<UserInfo> listByTag(String tag) {
        if (StrUtil.isBlank(tag)) {
            throw new IllegalArgumentException("标签不能为空");
        }

        Query query = Query.query(Criteria.where("tags").is(tag))
                .with(Sort.by(Sort.Direction.DESC, "createdAt"));

        List<UserInfo> users = mongoTemplate.find(query, UserInfo.class);
        log.info("按单个标签查询用户完成，标签：{}，结果数量：{}", tag, users.size());
        return users;
    }

    /**
     * 查询包含任意指定标签的用户。
     *
     * @param tags 标签集合
     * @return 用户列表
     */
    public List<UserInfo> listByAnyTag(Collection<String> tags) {
        if (CollUtil.isEmpty(tags)) {
            throw new IllegalArgumentException("标签集合不能为空");
        }

        Query query = Query.query(Criteria.where("tags").in(tags))
                .with(Sort.by(Sort.Direction.DESC, "createdAt"));

        List<UserInfo> users = mongoTemplate.find(query, UserInfo.class);
        log.info("按任意标签查询用户完成，标签数量：{}，结果数量：{}", tags.size(), users.size());
        return users;
    }

    /**
     * 查询同时包含所有指定标签的用户。
     *
     * @param tags 标签集合
     * @return 用户列表
     */
    public List<UserInfo> listByAllTags(Collection<String> tags) {
        if (CollUtil.isEmpty(tags)) {
            throw new IllegalArgumentException("标签集合不能为空");
        }

        Query query = Query.query(Criteria.where("tags").all(tags))
                .with(Sort.by(Sort.Direction.DESC, "createdAt"));

        List<UserInfo> users = mongoTemplate.find(query, UserInfo.class);
        log.info("按全部标签查询用户完成，标签数量：{}，结果数量：{}", tags.size(), users.size());
        return users;
    }

    /**
     * 查询标签数量为指定值的用户。
     *
     * @param size 标签数量
     * @return 用户列表
     */
    public List<UserInfo> listByTagSize(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("标签数量不能小于0");
        }

        Query query = Query.query(Criteria.where("tags").size(size));
        List<UserInfo> users = mongoTemplate.find(query, UserInfo.class);
        log.info("按标签数量查询用户完成，标签数量：{}，结果数量：{}", size, users.size());
        return users;
    }

}
```

如果数组中存储的是对象，例如订单明细、审批记录、设备指标，可以使用 `elemMatch` 查询满足多个条件的同一个数组元素。

对象数组示例：

```json
{
  "orderNo": "O202605130001",
  "items": [
    {
      "skuCode": "SKU_001",
      "quantity": 2,
      "amount": 199.00
    },
    {
      "skuCode": "SKU_002",
      "quantity": 1,
      "amount": 99.00
    }
  ]
}
```

`elemMatch` 查询示例：

```java
Query query = Query.query(
        Criteria.where("items").elemMatch(
                Criteria.where("skuCode").is("SKU_001")
                        .and("quantity").gte(2)
        )
);
```

数组字段使用建议：

| 场景                   | 建议                       |
| ---------------------- | -------------------------- |
| 标签、角色、权限       | 可以使用简单字符串数组     |
| 数量可控的明细         | 可以使用对象数组           |
| 数量无限增长的记录     | 不建议长期追加到同一个文档 |
| 高频数组查询           | 根据查询方式建立多键索引   |
| 多条件匹配同一数组元素 | 使用 `elemMatch`           |
| 标签去重追加           | 使用 `Update.addToSet`     |

`MongoTemplate` 的查询能力覆盖了大多数业务场景。实际开发中可以按规则选择：固定简单查询使用 Repository，动态条件查询和局部更新使用 `MongoTemplate`，复杂统计使用 `Aggregation`，大规模搜索或分析交给更适合的搜索引擎或分析型数据库。



## 索引设计

索引用于提升 MongoDB 查询、排序和去重约束的执行效率。Spring Data MongoDB 支持通过 `@Indexed`、`@CompoundIndex` 等注解声明索引，也可以通过 `MongoTemplate.indexOps()` 使用 `IndexOperations` 显式创建索引。Spring Data MongoDB 从 3.0 开始默认不再自动创建索引，需要显式启用或由应用启动逻辑主动创建，官方也更推荐使用显式索引创建方式，以便对索引生命周期和性能影响进行控制。([Home](https://docs.spring.io/spring-data/mongodb/reference/mongodb/mapping/mapping-index-management.html?utm_source=chatgpt.com))

### 普通索引

普通索引用于提升单字段查询或排序性能，适合用户名、手机号、状态、创建时间、城市等高频查询字段。普通索引可以通过 `@Indexed` 声明，也可以通过 `MongoTemplate.indexOps()` 创建。

在实体类中声明普通索引：

文件位置：`src/main/java/io/github/atengk/entity/UserInfo.java`

```java
package io.github.atengk.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息文档实体
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Document(collection = "user_info")
public class UserInfo {

    @Id
    private String id;

    /**
     * 用户名，后续唯一索引章节会调整为唯一索引
     */
    @Field("username")
    private String username;

    /**
     * 昵称
     */
    @Field("nickname")
    private String nickname;

    /**
     * 手机号，普通索引适合按手机号查询
     */
    @Indexed(name = "idx_user_info_mobile")
    @Field("mobile")
    private String mobile;

    /**
     * 是否启用
     */
    @Indexed(name = "idx_user_info_enabled")
    @Field("enabled")
    private Boolean enabled;

    /**
     * 创建时间，适合时间范围查询和倒序排序
     */
    @Indexed(name = "idx_user_info_created_at")
    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("age")
    private Integer age;

    @Field("email")
    private String email;

    @Field("tags")
    private List<String> tags;

    @Field("updated_at")
    private LocalDateTime updatedAt;

}
```

如果不希望依赖自动索引创建，推荐在应用启动后显式创建索引：

文件位置：`src/main/java/io/github/atengk/config/MongoIndexInitializer.java`

```java
package io.github.atengk.config;

import io.github.atengk.entity.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * MongoDB 索引初始化器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MongoIndexInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private final MongoTemplate mongoTemplate;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        mongoTemplate.indexOps(UserInfo.class)
                .ensureIndex(new Index()
                        .on("mobile", Sort.Direction.ASC)
                        .named("idx_user_info_mobile"));

        mongoTemplate.indexOps(UserInfo.class)
                .ensureIndex(new Index()
                        .on("enabled", Sort.Direction.ASC)
                        .named("idx_user_info_enabled"));

        mongoTemplate.indexOps(UserInfo.class)
                .ensureIndex(new Index()
                        .on("created_at", Sort.Direction.DESC)
                        .named("idx_user_info_created_at"));

        log.info("MongoDB 普通索引初始化完成");
    }

}
```

普通索引设计建议：

| 场景         | 建议                                             |
| ------------ | ------------------------------------------------ |
| 高频精确查询 | 建立普通索引，例如手机号、业务编号               |
| 高频范围查询 | 建立范围字段索引，例如创建时间、更新时间         |
| 高频排序字段 | 建立排序字段索引，例如创建时间倒序               |
| 低基数字段   | 单独索引价值有限，例如 `enabled` 只有 true/false |
| 写多读少字段 | 谨慎建索引，索引会增加写入成本                   |

验证索引：

```javascript
use ateng_mongo

db.user_info.getIndexes()
```

如果能看到 `idx_user_info_mobile`、`idx_user_info_enabled`、`idx_user_info_created_at`，说明索引已创建成功。

### 唯一索引

唯一索引用于保证字段值不能重复，例如用户名、手机号、邮箱、业务编码等。Spring Data MongoDB 的 `@Indexed(unique = true)` 可以声明唯一索引，MongoDB 会拒绝插入或更新重复值。`@Indexed` 注解支持 `unique` 参数，用于拒绝重复字段值。([Home](https://docs.spring.io/spring-data/mongodb/reference/mongodb/mapping/mapping.html?utm_source=chatgpt.com))

在用户表中，用户名通常应设置唯一索引：

文件位置：`src/main/java/io/github/atengk/entity/UserInfo.java`

```java
package io.github.atengk.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 用户信息文档实体
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Document(collection = "user_info")
public class UserInfo {

    @Id
    private String id;

    /**
     * 用户名，唯一索引用于防止重复注册
     */
    @Indexed(name = "uk_user_info_username", unique = true)
    @Field("username")
    private String username;

    /**
     * 手机号，允许为空时建议配合 sparse 或 partialFilter 使用
     */
    @Indexed(name = "uk_user_info_mobile", unique = true, sparse = true)
    @Field("mobile")
    private String mobile;

}
```

也可以通过 `MongoTemplate` 显式创建唯一索引：

文件位置：`src/main/java/io/github/atengk/config/MongoUniqueIndexInitializer.java`

```java
package io.github.atengk.config;

import io.github.atengk.entity.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * MongoDB 唯一索引初始化器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MongoUniqueIndexInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private final MongoTemplate mongoTemplate;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        mongoTemplate.indexOps(UserInfo.class)
                .ensureIndex(new Index()
                        .on("username", Sort.Direction.ASC)
                        .unique()
                        .named("uk_user_info_username"));

        mongoTemplate.indexOps(UserInfo.class)
                .ensureIndex(new Index()
                        .on("mobile", Sort.Direction.ASC)
                        .unique()
                        .sparse()
                        .named("uk_user_info_mobile"));

        log.info("MongoDB 唯一索引初始化完成");
    }

}
```

唯一索引注意事项：

| 注意事项               | 说明                                                 |
| ---------------------- | ---------------------------------------------------- |
| 先清理重复数据         | 已存在重复数据时，唯一索引创建会失败                 |
| 谨慎处理空值           | 多条文档字段为空时，唯一索引可能冲突                 |
| 可使用 `sparse`        | 字段不存在时不进入索引，但字段值为 `null` 时仍需谨慎 |
| 唯一约束不替代业务校验 | 接口层仍应提前判断重复值，索引用于最终兜底           |
| 捕获重复键异常         | 需要统一处理 `DuplicateKeyException`                 |

重复键异常通常在新增或更新时抛出，建议在全局异常处理中统一转换为业务提示，例如“用户名已存在”。

### 复合索引

复合索引用于优化多个字段组合查询或组合排序。例如常见分页查询条件是 `enabled = true` 并按 `created_at` 倒序排序，此时可以建立 `{ enabled: 1, created_at: -1 }` 复合索引。Spring Data MongoDB 的 `@CompoundIndex` 用于在类型级别声明复合索引，索引定义使用 JSON 格式，字段值 `1` 表示升序，`-1` 表示降序。([Home](https://docs.spring.io/spring-data/mongodb/reference/mongodb/mapping/mapping-index-management.html?utm_source=chatgpt.com))

在实体类上声明复合索引：

文件位置：`src/main/java/io/github/atengk/entity/UserInfo.java`

```java
package io.github.atengk.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * 用户信息文档实体
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Document(collection = "user_info")
@CompoundIndexes({
        @CompoundIndex(
                name = "idx_user_info_enabled_created_at",
                def = "{'enabled': 1, 'created_at': -1}"
        ),
        @CompoundIndex(
                name = "idx_user_info_city_enabled",
                def = "{'address.city': 1, 'enabled': 1}"
        )
})
public class UserInfo {

    @Id
    private String id;

    @Field("username")
    private String username;

    @Field("enabled")
    private Boolean enabled;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("address")
    private AddressInfo address;

    /**
     * 用户地址信息
     *
     * @author Ateng
     * @since 2026-05-13
     */
    @Data
    public static class AddressInfo {

        @Field("province")
        private String province;

        @Field("city")
        private String city;

        @Field("detail")
        private String detail;

    }

}
```

通过 `MongoTemplate` 显式创建复合索引：

文件位置：`src/main/java/io/github/atengk/config/MongoCompoundIndexInitializer.java`

```java
package io.github.atengk.config;

import io.github.atengk.entity.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.stereotype.Component;

/**
 * MongoDB 复合索引初始化器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MongoCompoundIndexInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private final MongoTemplate mongoTemplate;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        Document enabledCreatedAtIndex = new Document()
                .append("enabled", 1)
                .append("created_at", -1);

        mongoTemplate.indexOps(UserInfo.class)
                .ensureIndex(new CompoundIndexDefinition(enabledCreatedAtIndex)
                        .named("idx_user_info_enabled_created_at"));

        Document cityEnabledIndex = new Document()
                .append("address.city", 1)
                .append("enabled", 1);

        mongoTemplate.indexOps(UserInfo.class)
                .ensureIndex(new CompoundIndexDefinition(cityEnabledIndex)
                        .named("idx_user_info_city_enabled"));

        log.info("MongoDB 复合索引初始化完成");
    }

}
```

复合索引设计建议：

| 查询场景                                              | 推荐索引                          |
| ----------------------------------------------------- | --------------------------------- |
| `enabled = true order by created_at desc`             | `{ enabled: 1, created_at: -1 }`  |
| `address.city = ? and enabled = true`                 | `{ address.city: 1, enabled: 1 }` |
| `username = ? and enabled = true`                     | `{ username: 1, enabled: 1 }`     |
| `created_at between ? and ? order by created_at desc` | `{ created_at: -1 }`              |
| `enabled = ? and age between ?`                       | `{ enabled: 1, age: 1 }`          |

复合索引的字段顺序很重要。通常将等值查询字段放前面，将范围查询或排序字段放后面。例如 `enabled` 是等值条件，`created_at` 是排序字段，因此常用索引顺序是 `{ enabled: 1, created_at: -1 }`。

### TTL 索引

TTL 索引用于自动删除过期数据，适合验证码、临时 Token、操作日志、接口日志、会话记录等有明确生命周期的数据。MongoDB 官方文档说明，TTL 索引是特殊的单字段索引，MongoDB 会在指定时间后自动删除文档；TTL 索引字段必须是日期类型或包含日期值的数组，`expireAfterSeconds` 的取值范围是 `0` 到 `2147483647`。([MongoDB](https://www.mongodb.com/docs/v8.0/core/index-ttl/?utm_source=chatgpt.com))

TTL 索引有几个关键限制：TTL 是单字段索引，复合索引不支持 TTL；`_id` 字段不支持 TTL；如果文档没有 TTL 字段或字段不是日期类型，文档不会自动过期。([MongoDB](https://www.mongodb.com/docs/v8.0/core/index-ttl/?utm_source=chatgpt.com))

定义操作日志实体：

文件位置：`src/main/java/io/github/atengk/entity/OperationLog.java`

```java
package io.github.atengk.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * 操作日志文档实体
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Document(collection = "operation_log")
public class OperationLog {

    @Id
    private String id;

    /**
     * 操作人
     */
    @Field("operator")
    private String operator;

    /**
     * 操作类型
     */
    @Field("operation_type")
    private String operationType;

    /**
     * 操作内容
     */
    @Field("content")
    private String content;

    /**
     * 创建时间
     */
    @Field("created_at")
    private Instant createdAt;

    /**
     * 过期时间，expireAfterSeconds = 0 表示到达该时间点后过期
     */
    @Indexed(name = "idx_operation_log_expire_at_ttl", expireAfterSeconds = 0)
    @Field("expire_at")
    private Instant expireAt;

}
```

插入一条 7 天后过期的操作日志：

文件位置：`src/main/java/io/github/atengk/service/OperationLogService.java`

```java
package io.github.atengk.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.entity.OperationLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 操作日志服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final MongoTemplate mongoTemplate;

    public OperationLog saveLog(String operator, String operationType, String content) {
        if (StrUtil.hasBlank(operator, operationType)) {
            throw new IllegalArgumentException("操作人和操作类型不能为空");
        }

        Instant now = Instant.now();

        OperationLog operationLog = new OperationLog();
        operationLog.setOperator(operator);
        operationLog.setOperationType(operationType);
        operationLog.setContent(content);
        operationLog.setCreatedAt(now);
        operationLog.setExpireAt(now.plus(7, ChronoUnit.DAYS));

        OperationLog savedLog = mongoTemplate.insert(operationLog);
        log.info("操作日志写入成功，日志ID：{}，操作人：{}，操作类型：{}",
                savedLog.getId(), savedLog.getOperator(), savedLog.getOperationType());
        return savedLog;
    }

}
```

使用 `mongosh` 创建 TTL 索引：

```javascript
use ateng_mongo

db.operation_log.createIndex(
  { "expire_at": 1 },
  {
    name: "idx_operation_log_expire_at_ttl",
    expireAfterSeconds: 0
  }
)
```

验证 TTL 索引：

```javascript
db.operation_log.getIndexes()
```

TTL 索引注意事项：

| 注意事项           | 说明                                              |
| ------------------ | ------------------------------------------------- |
| 只支持单字段索引   | 复合索引不支持 TTL                                |
| 字段必须是日期类型 | 推荐使用 `Instant` 或可正确映射为 Date 的时间类型 |
| 删除不是实时秒级   | MongoDB 后台线程周期性清理过期文档                |
| 创建前评估历史数据 | 大量历史过期数据可能在索引创建后集中删除          |
| 不适合核心业务数据 | 订单、支付、审计等数据不应依赖 TTL 自动删除       |

## 接口开发

接口开发用于将前面定义的文档模型、Repository 和 `MongoTemplate` 操作封装为 REST API。这里采用 Controller、Service、DTO、VO 分层结构，Controller 负责接收请求和参数校验，Service 负责业务逻辑和 MongoDB 操作，DTO 负责请求参数，VO 负责响应数据。

接口示例基于以下路径结构：

```text
src/main/java/io/github/atengk
├── controller
│   └── UserInfoApiController.java
├── dto
│   ├── UserCreateRequest.java
│   ├── UserUpdateRequest.java
│   └── UserPageQuery.java
├── service
│   ├── UserInfoApiService.java
│   └── impl
│       └── UserInfoApiServiceImpl.java
└── vo
    ├── ApiResult.java
    ├── PageResult.java
    └── UserInfoVO.java
```

通用响应对象：

文件位置：`src/main/java/io/github/atengk/vo/ApiResult.java`

```java
package io.github.atengk.vo;

import lombok.Data;

/**
 * 通用接口响应对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class ApiResult<T> {

    private Integer code;

    private String message;

    private T data;

    public static <T> ApiResult<T> success(T data) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    public static <T> ApiResult<T> success() {
        return success(null);
    }

    public static <T> ApiResult<T> fail(String message) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(500);
        result.setMessage(message);
        result.setData(null);
        return result;
    }

}
```

分页响应对象：

文件位置：`src/main/java/io/github/atengk/vo/PageResult.java`

```java
package io.github.atengk.vo;

import lombok.Data;

import java.util.List;

/**
 * 分页响应对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class PageResult<T> {

    private List<T> records;

    private Long total;

    private Integer pageNum;

    private Integer pageSize;

    private Integer pages;

    private Boolean hasNext;

    public static <T> PageResult<T> of(List<T> records, long total, int pageNum, int pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setPages((int) Math.ceil((double) total / pageSize));
        result.setHasNext((long) pageNum * pageSize < total);
        return result;
    }

}
```

用户响应对象：

文件位置：`src/main/java/io/github/atengk/vo/UserInfoVO.java`

```java
package io.github.atengk.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息响应对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class UserInfoVO {

    private String id;

    private String username;

    private String nickname;

    private Integer age;

    private String email;

    private String mobile;

    private Boolean enabled;

    private List<String> tags;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
```

### 新增接口

新增接口用于创建用户文档。接口层接收新增参数，Service 层校验用户名是否重复，最后通过 `MongoTemplate.insert` 写入 MongoDB。

新增请求 DTO：

文件位置：`src/main/java/io/github/atengk/dto/UserCreateRequest.java`

```java
package io.github.atengk.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 用户新增请求参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class UserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @Min(value = 0, message = "年龄不能小于0")
    @Max(value = 150, message = "年龄不能大于150")
    private Integer age;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String mobile;

    private List<String> tags;

}
```

### 查询接口

查询接口用于根据用户 ID 获取单条用户文档。这里使用 `MongoTemplate.findById` 查询，如果不存在则抛出业务异常。

### 更新接口

更新接口用于按 ID 修改用户字段。这里使用 `MongoTemplate.updateFirst` 做局部更新，只更新入参中非空字段，避免使用 `save` 覆盖整个文档。

更新请求 DTO：

文件位置：`src/main/java/io/github/atengk/dto/UserUpdateRequest.java`

```java
package io.github.atengk.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.List;

/**
 * 用户更新请求参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class UserUpdateRequest {

    private String nickname;

    @Min(value = 0, message = "年龄不能小于0")
    @Max(value = 150, message = "年龄不能大于150")
    private Integer age;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String mobile;

    private Boolean enabled;

    private List<String> tags;

}
```

### 删除接口

删除接口用于根据 ID 删除用户文档。示例中使用物理删除，实际业务中如果数据需要保留审计记录，建议改为逻辑删除，例如增加 `deleted` 字段并执行局部更新。

### 分页查询接口

分页查询接口用于按关键字、启用状态、创建时间范围进行分页查询。分页场景中建议对查询字段和排序字段建立索引，例如 `{ enabled: 1, created_at: -1 }`。

分页查询 DTO：

文件位置：`src/main/java/io/github/atengk/dto/UserPageQuery.java`

```java
package io.github.atengk.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 用户分页查询参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class UserPageQuery {

    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能大于100")
    private Integer pageSize = 10;

    private String keyword;

    private Boolean enabled;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

}
```

Service 接口：

文件位置：`src/main/java/io/github/atengk/service/UserInfoApiService.java`

```java
package io.github.atengk.service;

import io.github.atengk.dto.UserCreateRequest;
import io.github.atengk.dto.UserPageQuery;
import io.github.atengk.dto.UserUpdateRequest;
import io.github.atengk.vo.PageResult;
import io.github.atengk.vo.UserInfoVO;

/**
 * 用户信息接口服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface UserInfoApiService {

    UserInfoVO create(UserCreateRequest request);

    UserInfoVO getById(String id);

    UserInfoVO updateById(String id, UserUpdateRequest request);

    Boolean deleteById(String id);

    PageResult<UserInfoVO> page(UserPageQuery query);

}
```

Service 实现类：

文件位置：`src/main/java/io/github/atengk/service/impl/UserInfoApiServiceImpl.java`

```java
package io.github.atengk.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import io.github.atengk.dto.UserCreateRequest;
import io.github.atengk.dto.UserPageQuery;
import io.github.atengk.dto.UserUpdateRequest;
import io.github.atengk.entity.UserInfo;
import io.github.atengk.service.UserInfoApiService;
import io.github.atengk.vo.PageResult;
import io.github.atengk.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户信息接口服务实现
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoApiServiceImpl implements UserInfoApiService {

    private final MongoTemplate mongoTemplate;

    @Override
    public UserInfoVO create(UserCreateRequest request) {
        Query existsQuery = Query.query(Criteria.where("username").is(request.getUsername()));
        if (mongoTemplate.exists(existsQuery, UserInfo.class)) {
            throw new IllegalArgumentException("用户名已存在");
        }

        LocalDateTime now = LocalDateTime.now();

        UserInfo userInfo = new UserInfo();
        userInfo.setUsername(request.getUsername());
        userInfo.setNickname(request.getNickname());
        userInfo.setAge(request.getAge());
        userInfo.setEmail(request.getEmail());
        userInfo.setMobile(request.getMobile());
        userInfo.setEnabled(Boolean.TRUE);
        userInfo.setTags(CollUtil.emptyIfNull(request.getTags()));
        userInfo.setCreatedAt(now);
        userInfo.setUpdatedAt(now);

        UserInfo savedUser = mongoTemplate.insert(userInfo);
        log.info("用户新增成功，用户ID：{}，用户名：{}", savedUser.getId(), savedUser.getUsername());
        return this.toVO(savedUser);
    }

    @Override
    public UserInfoVO getById(String id) {
        if (StrUtil.isBlank(id)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        UserInfo userInfo = mongoTemplate.findById(id, UserInfo.class);
        if (userInfo == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        return this.toVO(userInfo);
    }

    @Override
    public UserInfoVO updateById(String id, UserUpdateRequest request) {
        if (StrUtil.isBlank(id)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        Query query = Query.query(Criteria.where("_id").is(id));
        Update update = new Update();
        boolean hasUpdate = false;

        if (StrUtil.isNotBlank(request.getNickname())) {
            update.set("nickname", request.getNickname());
            hasUpdate = true;
        }
        if (request.getAge() != null) {
            update.set("age", request.getAge());
            hasUpdate = true;
        }
        if (StrUtil.isNotBlank(request.getEmail())) {
            update.set("email", request.getEmail());
            hasUpdate = true;
        }
        if (StrUtil.isNotBlank(request.getMobile())) {
            update.set("mobile", request.getMobile());
            hasUpdate = true;
        }
        if (request.getEnabled() != null) {
            update.set("enabled", request.getEnabled());
            hasUpdate = true;
        }
        if (request.getTags() != null) {
            update.set("tags", request.getTags());
            hasUpdate = true;
        }

        if (!hasUpdate) {
            throw new IllegalArgumentException("没有需要更新的字段");
        }

        update.set("updatedAt", LocalDateTime.now());

        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, UserInfo.class);
        if (updateResult.getMatchedCount() == 0) {
            throw new IllegalArgumentException("用户不存在");
        }

        log.info("用户更新成功，用户ID：{}，修改数量：{}", id, updateResult.getModifiedCount());
        return this.getById(id);
    }

    @Override
    public Boolean deleteById(String id) {
        if (StrUtil.isBlank(id)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        Query query = Query.query(Criteria.where("_id").is(id));
        DeleteResult deleteResult = mongoTemplate.remove(query, UserInfo.class);
        if (deleteResult.getDeletedCount() == 0) {
            throw new IllegalArgumentException("用户不存在");
        }

        log.info("用户删除成功，用户ID：{}，删除数量：{}", id, deleteResult.getDeletedCount());
        return Boolean.TRUE;
    }

    @Override
    public PageResult<UserInfoVO> page(UserPageQuery query) {
        int pageNum = query.getPageNum() == null ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 10 : query.getPageSize();

        List<Criteria> criteriaList = new ArrayList<>();

        if (StrUtil.isNotBlank(query.getKeyword())) {
            String keyword = ReUtil.escape(query.getKeyword());
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("username").regex(keyword, "i"),
                    Criteria.where("nickname").regex(keyword, "i")
            ));
        }

        if (query.getEnabled() != null) {
            criteriaList.add(Criteria.where("enabled").is(query.getEnabled()));
        }

        if (query.getStartTime() != null || query.getEndTime() != null) {
            Criteria timeCriteria = Criteria.where("createdAt");
            if (query.getStartTime() != null) {
                timeCriteria.gte(query.getStartTime());
            }
            if (query.getEndTime() != null) {
                timeCriteria.lte(query.getEndTime());
            }
            criteriaList.add(timeCriteria);
        }

        Query countQuery = this.buildQuery(criteriaList);
        long total = mongoTemplate.count(countQuery, UserInfo.class);

        Query pageQuery = this.buildQuery(criteriaList)
                .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                .skip((long) (pageNum - 1) * pageSize)
                .limit(pageSize);

        List<UserInfo> users = mongoTemplate.find(pageQuery, UserInfo.class);
        List<UserInfoVO> records = users.stream()
                .map(this::toVO)
                .toList();

        log.info("用户分页查询完成，页码：{}，每页数量：{}，总数：{}，当前页数量：{}",
                pageNum, pageSize, total, records.size());

        return PageResult.of(records, total, pageNum, pageSize);
    }

    private Query buildQuery(List<Criteria> criteriaList) {
        if (CollUtil.isEmpty(criteriaList)) {
            return new Query();
        }

        Criteria criteria = new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
        return Query.query(criteria);
    }

    private UserInfoVO toVO(UserInfo userInfo) {
        UserInfoVO vo = new UserInfoVO();
        BeanUtil.copyProperties(userInfo, vo);
        return vo;
    }

}
```

Controller 接口：

文件位置：`src/main/java/io/github/atengk/controller/UserInfoApiController.java`

```java
package io.github.atengk.controller;

import io.github.atengk.dto.UserCreateRequest;
import io.github.atengk.dto.UserPageQuery;
import io.github.atengk.dto.UserUpdateRequest;
import io.github.atengk.service.UserInfoApiService;
import io.github.atengk.vo.ApiResult;
import io.github.atengk.vo.PageResult;
import io.github.atengk.vo.UserInfoVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户信息接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserInfoApiController {

    private final UserInfoApiService userInfoApiService;

    @PostMapping
    public ApiResult<UserInfoVO> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResult.success(userInfoApiService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResult<UserInfoVO> getById(@PathVariable String id) {
        return ApiResult.success(userInfoApiService.getById(id));
    }

    @PutMapping("/{id}")
    public ApiResult<UserInfoVO> updateById(@PathVariable String id,
                                            @Valid @RequestBody UserUpdateRequest request) {
        return ApiResult.success(userInfoApiService.updateById(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Boolean> deleteById(@PathVariable String id) {
        return ApiResult.success(userInfoApiService.deleteById(id));
    }

    @GetMapping("/page")
    public ApiResult<PageResult<UserInfoVO>> page(@Valid UserPageQuery query) {
        return ApiResult.success(userInfoApiService.page(query));
    }

}
```

接口调用示例：

```bash
# 新增用户
curl -X POST 'http://localhost:8080/api/users' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "ateng",
    "nickname": "阿腾",
    "age": 28,
    "email": "ateng@example.com",
    "mobile": "13800000000",
    "tags": ["java", "springboot", "mongodb"]
  }'

# 根据 ID 查询用户
curl 'http://localhost:8080/api/users/665f2d4f91f3a65a9f2d1001'

# 更新用户
curl -X PUT 'http://localhost:8080/api/users/665f2d4f91f3a65a9f2d1001' \
  -H 'Content-Type: application/json' \
  -d '{
    "nickname": "阿腾同学",
    "age": 29,
    "enabled": true,
    "tags": ["java", "springboot", "mongodb", "backend"]
  }'

# 删除用户
curl -X DELETE 'http://localhost:8080/api/users/665f2d4f91f3a65a9f2d1001'

# 分页查询用户
curl -G 'http://localhost:8080/api/users/page' \
  --data-urlencode 'pageNum=1' \
  --data-urlencode 'pageSize=10' \
  --data-urlencode 'keyword=ateng' \
  --data-urlencode 'enabled=true' \
  --data-urlencode 'startTime=2026-05-01T00:00:00' \
  --data-urlencode 'endTime=2026-05-31T23:59:59'
```

接口清单：

| 功能     | 请求方法 | 接口路径          | 说明                 |
| -------- | -------- | ----------------- | -------------------- |
| 新增用户 | `POST`   | `/api/users`      | 创建用户文档         |
| 查询用户 | `GET`    | `/api/users/{id}` | 根据 ID 查询用户     |
| 更新用户 | `PUT`    | `/api/users/{id}` | 根据 ID 局部更新用户 |
| 删除用户 | `DELETE` | `/api/users/{id}` | 根据 ID 删除用户     |
| 分页查询 | `GET`    | `/api/users/page` | 条件分页查询用户     |

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": "665f2d4f91f3a65a9f2d1001",
    "username": "ateng",
    "nickname": "阿腾",
    "age": 28,
    "email": "ateng@example.com",
    "mobile": "13800000000",
    "enabled": true,
    "tags": ["java", "springboot", "mongodb"],
    "createdAt": "2026-05-13T09:00:00",
    "updatedAt": "2026-05-13T10:30:00"
  }
}
```

分页响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": "665f2d4f91f3a65a9f2d1001",
        "username": "ateng",
        "nickname": "阿腾",
        "age": 28,
        "email": "ateng@example.com",
        "mobile": "13800000000",
        "enabled": true,
        "tags": ["java", "springboot", "mongodb"],
        "createdAt": "2026-05-13T09:00:00",
        "updatedAt": "2026-05-13T10:30:00"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 1,
    "hasNext": false
  }
}
```

接口开发注意事项：

| 注意事项 | 说明                                               |
| -------- | -------------------------------------------------- |
| 新增接口 | 先做业务唯一性校验，再依赖唯一索引兜底             |
| 查询接口 | 路径参数不能为空，查询不到应返回明确业务异常       |
| 更新接口 | 推荐局部更新，不建议用不完整实体直接 `save`        |
| 删除接口 | 核心业务数据优先逻辑删除，非核心临时数据可物理删除 |
| 分页接口 | 限制 `pageSize` 最大值，避免一次返回过多数据       |
| 模糊查询 | 用户输入需要正则转义，避免特殊字符影响匹配         |
| 索引配合 | 查询字段和排序字段应与索引设计保持一致             |



## 数据校验与异常处理

数据校验与异常处理用于保证接口入参合法、业务错误可控、MongoDB 异常能够转换为统一响应。Spring Boot 3 使用 Jakarta Validation 体系，Controller 参数可以通过 `@Valid`、`@Validated` 和 `jakarta.validation` 约束注解触发校验；Spring MVC 对 `@RequestBody`、`@ModelAttribute` 等参数校验失败时会抛出对应校验异常，适合在全局异常处理中统一处理。([Home](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html?utm_source=chatgpt.com))

### 参数校验

参数校验主要放在 DTO、Controller 入参和 Service 业务入口处。DTO 使用注解声明格式、长度、范围和必填规则；Controller 使用 `@Valid` 触发对象校验；Service 层继续做业务规则校验，例如用户名是否重复、数据是否存在、状态是否允许变更。

常用校验注解如下：

| 注解            | 作用                             | 示例                     |
| --------------- | -------------------------------- | ------------------------ |
| `@NotBlank`     | 字符串不能为空且不能全是空白字符 | 用户名、昵称             |
| `@NotNull`      | 对象不能为空                     | 状态、类型               |
| `@Min` / `@Max` | 数值范围校验                     | 年龄、分页大小           |
| `@Email`        | 邮箱格式校验                     | 邮箱                     |
| `@Size`         | 字符串、集合长度校验             | 标签数量、名称长度       |
| `@Pattern`      | 正则校验                         | 手机号、业务编码         |
| `@AssertTrue`   | 自定义布尔规则校验               | 开始时间不能大于结束时间 |

新增用户请求参数示例：

文件位置：`src/main/java/io/github/atengk/dto/UserCreateRequest.java`

```java
package io.github.atengk.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 用户新增请求参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class UserCreateRequest {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度必须在3到32位之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    /**
     * 昵称
     */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称长度不能超过50位")
    private String nickname;

    /**
     * 年龄
     */
    @Min(value = 0, message = "年龄不能小于0")
    @Max(value = 150, message = "年龄不能大于150")
    private Integer age;

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 手机号
     */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;

    /**
     * 用户标签
     */
    @Size(max = 20, message = "用户标签不能超过20个")
    private List<@Size(max = 30, message = "单个标签长度不能超过30位") String> tags;

}
```

分页查询参数示例：

文件位置：`src/main/java/io/github/atengk/dto/UserPageQuery.java`

```java
package io.github.atengk.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 用户分页查询参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class UserPageQuery {

    /**
     * 页码，从1开始
     */
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能大于100")
    private Integer pageSize = 10;

    /**
     * 搜索关键字
     */
    private String keyword;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 开始时间
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    /**
     * 校验时间范围是否合法。
     *
     * @return 是否合法
     */
    @AssertTrue(message = "开始时间不能大于结束时间")
    public boolean isTimeRangeValid() {
        if (startTime == null || endTime == null) {
            return true;
        }
        return !startTime.isAfter(endTime);
    }

}
```

如果接口路径中经常传 MongoDB ObjectId，可以封装一个自定义校验注解，避免每个接口手动判断 ID 格式。

文件位置：`src/main/java/io/github/atengk/validation/MongoObjectId.java`

```java
package io.github.atengk.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * MongoDB ObjectId 校验注解
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Documented
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = MongoObjectIdValidator.class)
public @interface MongoObjectId {

    String message() default "MongoDB ObjectId 格式不正确";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
```

文件位置：`src/main/java/io/github/atengk/validation/MongoObjectIdValidator.java`

```java
package io.github.atengk.validation;

import cn.hutool.core.util.StrUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.bson.types.ObjectId;

/**
 * MongoDB ObjectId 校验器
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class MongoObjectIdValidator implements ConstraintValidator<MongoObjectId, String> {

    /**
     * 校验 ObjectId 字符串是否合法。
     *
     * @param value 字符串值
     * @param context 校验上下文
     * @return 是否合法
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StrUtil.isBlank(value)) {
            return false;
        }
        return ObjectId.isValid(value);
    }

}
```

Controller 中启用参数校验：

文件位置：`src/main/java/io/github/atengk/controller/UserInfoApiController.java`

```java
package io.github.atengk.controller;

import io.github.atengk.dto.UserCreateRequest;
import io.github.atengk.dto.UserPageQuery;
import io.github.atengk.dto.UserUpdateRequest;
import io.github.atengk.service.UserInfoApiService;
import io.github.atengk.validation.MongoObjectId;
import io.github.atengk.vo.ApiResult;
import io.github.atengk.vo.PageResult;
import io.github.atengk.vo.UserInfoVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户信息接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Validated
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserInfoApiController {

    private final UserInfoApiService userInfoApiService;

    /**
     * 新增用户。
     *
     * @param request 新增参数
     * @return 用户信息
     */
    @PostMapping
    public ApiResult<UserInfoVO> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResult.success(userInfoApiService.create(request));
    }

    /**
     * 根据ID查询用户。
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    public ApiResult<UserInfoVO> getById(@PathVariable @MongoObjectId String id) {
        return ApiResult.success(userInfoApiService.getById(id));
    }

    /**
     * 根据ID更新用户。
     *
     * @param id 用户ID
     * @param request 更新参数
     * @return 用户信息
     */
    @PutMapping("/{id}")
    public ApiResult<UserInfoVO> updateById(@PathVariable @MongoObjectId String id,
                                            @Valid @RequestBody UserUpdateRequest request) {
        return ApiResult.success(userInfoApiService.updateById(id, request));
    }

    /**
     * 根据ID删除用户。
     *
     * @param id 用户ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public ApiResult<Boolean> deleteById(@PathVariable @MongoObjectId String id) {
        return ApiResult.success(userInfoApiService.deleteById(id));
    }

    /**
     * 分页查询用户。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @GetMapping("/page")
    public ApiResult<PageResult<UserInfoVO>> page(@Valid UserPageQuery query) {
        return ApiResult.success(userInfoApiService.page(query));
    }

}
```

参数校验建议：

| 位置         | 处理内容                               |
| ------------ | -------------------------------------- |
| DTO          | 必填、长度、格式、范围、时间区间       |
| Controller   | 使用 `@Valid`、`@Validated` 触发校验   |
| Service      | 数据是否存在、状态是否允许、唯一性校验 |
| MongoDB 索引 | 唯一索引做最终一致性兜底               |
| 全局异常处理 | 将校验异常转换为统一响应               |

### 业务异常封装

业务异常用于表达可预期的业务错误，例如用户不存在、用户名已存在、状态不允许修改、参数组合不合法等。业务异常不应该直接返回 Java 异常堆栈，而应转换为统一的错误码和错误消息。

错误码枚举：

文件位置：`src/main/java/io/github/atengk/exception/ErrorCode.java`

```java
package io.github.atengk.exception;

import lombok.Getter;

/**
 * 业务错误码
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Getter
public enum ErrorCode {

    /**
     * 操作成功
     */
    SUCCESS(200, "操作成功"),

    /**
     * 请求参数错误
     */
    PARAM_ERROR(400, "请求参数错误"),

    /**
     * 未找到数据
     */
    NOT_FOUND(404, "数据不存在"),

    /**
     * 数据冲突
     */
    CONFLICT(409, "数据已存在"),

    /**
     * MongoDB 服务不可用
     */
    MONGO_UNAVAILABLE(503, "MongoDB 服务不可用"),

    /**
     * 系统异常
     */
    SYSTEM_ERROR(500, "系统异常");

    private final Integer code;

    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

}
```

业务异常类：

文件位置：`src/main/java/io/github/atengk/exception/BusinessException.java`

```java
package io.github.atengk.exception;

import lombok.Getter;

/**
 * 业务异常
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    /**
     * 创建业务异常。
     *
     * @param errorCode 错误码
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 创建业务异常。
     *
     * @param errorCode 错误码
     * @param message 错误消息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

}
```

统一响应对象建议支持自定义错误码：

文件位置：`src/main/java/io/github/atengk/vo/ApiResult.java`

```java
package io.github.atengk.vo;

import io.github.atengk.exception.ErrorCode;
import lombok.Data;

/**
 * 通用接口响应对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class ApiResult<T> {

    private Integer code;

    private String message;

    private T data;

    /**
     * 返回成功响应。
     *
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 响应对象
     */
    public static <T> ApiResult<T> success(T data) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(ErrorCode.SUCCESS.getCode());
        result.setMessage(ErrorCode.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    /**
     * 返回无数据成功响应。
     *
     * @param <T> 数据类型
     * @return 响应对象
     */
    public static <T> ApiResult<T> success() {
        return success(null);
    }

    /**
     * 返回失败响应。
     *
     * @param code 错误码
     * @param message 错误消息
     * @param <T> 数据类型
     * @return 响应对象
     */
    public static <T> ApiResult<T> fail(Integer code, String message) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(null);
        return result;
    }

    /**
     * 返回失败响应。
     *
     * @param errorCode 错误码
     * @param <T> 数据类型
     * @return 响应对象
     */
    public static <T> ApiResult<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    }

}
```

Service 中抛出业务异常：

文件位置：`src/main/java/io/github/atengk/service/impl/UserInfoApiServiceImpl.java`

```java
package io.github.atengk.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.entity.UserInfo;
import io.github.atengk.exception.BusinessException;
import io.github.atengk.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

/**
 * 用户信息业务异常示例服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Service
@RequiredArgsConstructor
public class UserInfoApiServiceImpl {

    private final MongoTemplate mongoTemplate;

    /**
     * 根据ID查询用户。
     *
     * @param id 用户ID
     * @return 用户信息
     */
    public UserInfo getEntityById(String id) {
        if (StrUtil.isBlank(id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空");
        }

        UserInfo userInfo = mongoTemplate.findById(id, UserInfo.class);
        if (userInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        return userInfo;
    }

}
```

业务异常封装原则：

| 原则                   | 说明                                             |
| ---------------------- | ------------------------------------------------ |
| 可预期错误使用业务异常 | 用户不存在、状态不合法、数据重复                 |
| 不直接暴露底层异常     | 不把 MongoDB 异常、堆栈信息直接返回前端          |
| 错误码保持稳定         | 前端可根据错误码做交互处理                       |
| 日志记录关键上下文     | 记录用户ID、业务编号、操作类型等，不记录敏感信息 |
| 异常消息面向用户       | 返回消息应明确、简短、可理解                     |

### MongoDB 异常处理

Spring Data MongoDB 会将部分 MongoDB RuntimeException 转换为 Spring `org.springframework.dao` 异常体系，`MongoExceptionTranslator` 的职责就是将 MongoDB 异常转换为合适的 `DataAccessException`。因此全局异常处理中建议优先处理 Spring DAO 异常，例如 `DuplicateKeyException`、`DataAccessResourceFailureException` 和通用 `DataAccessException`。([Home](https://docs.spring.io/spring-data/mongodb/docs/current/api/org/springframework/data/mongodb/core/MongoExceptionTranslator.html?utm_source=chatgpt.com))

全局异常处理类：

文件位置：`src/main/java/io/github/atengk/exception/GlobalExceptionHandler.java`

```java
package io.github.atengk.exception;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.vo.ApiResult;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
     * 处理请求体参数校验异常。
     *
     * @param ex 异常对象
     * @return 响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + "：" + fieldError.getDefaultMessage())
                .findFirst()
                .orElse(ErrorCode.PARAM_ERROR.getMessage());

        log.warn("请求体参数校验失败：{}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.fail(ErrorCode.PARAM_ERROR.getCode(), message));
    }

    /**
     * 处理表单或查询参数绑定异常。
     *
     * @param ex 异常对象
     * @return 响应结果
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResult<Void>> handleBindException(BindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + "：" + fieldError.getDefaultMessage())
                .findFirst()
                .orElse(ErrorCode.PARAM_ERROR.getMessage());

        log.warn("请求参数绑定失败：{}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.fail(ErrorCode.PARAM_ERROR.getCode(), message));
    }

    /**
     * 处理方法参数约束异常。
     *
     * @param ex 异常对象
     * @return 响应结果
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResult<Void>> handleConstraintViolationException(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + "：" + violation.getMessage())
                .findFirst()
                .orElse(ErrorCode.PARAM_ERROR.getMessage());

        log.warn("方法参数校验失败：{}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.fail(ErrorCode.PARAM_ERROR.getCode(), message));
    }

    /**
     * 处理业务异常。
     *
     * @param ex 异常对象
     * @return 响应结果
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleBusinessException(BusinessException ex) {
        Integer code = ex.getCode();
        HttpStatus status = this.resolveHttpStatus(code);

        log.warn("业务异常：code={}，message={}", code, ex.getMessage());
        return ResponseEntity.status(status)
                .body(ApiResult.fail(code, ex.getMessage()));
    }

    /**
     * 处理 MongoDB 唯一索引冲突异常。
     *
     * @param ex 异常对象
     * @return 响应结果
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResult<Void>> handleDuplicateKeyException(DuplicateKeyException ex) {
        log.warn("MongoDB 唯一索引冲突：{}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResult.fail(ErrorCode.CONFLICT.getCode(), "数据已存在，请检查唯一字段"));
    }

    /**
     * 处理 MongoDB 连接资源异常。
     *
     * @param ex 异常对象
     * @return 响应结果
     */
    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ResponseEntity<ApiResult<Void>> handleDataAccessResourceFailureException(DataAccessResourceFailureException ex) {
        log.error("MongoDB 服务不可用：{}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResult.fail(ErrorCode.MONGO_UNAVAILABLE));
    }

    /**
     * 处理 MongoDB 数据访问异常。
     *
     * @param ex 异常对象
     * @return 响应结果
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResult<Void>> handleDataAccessException(DataAccessException ex) {
        log.error("MongoDB 数据访问异常：{}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.fail(ErrorCode.SYSTEM_ERROR.getCode(), "数据访问异常，请稍后重试"));
    }

    /**
     * 处理未知异常。
     *
     * @param ex 异常对象
     * @return 响应结果
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleException(Exception ex) {
        log.error("系统未知异常：{}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.fail(ErrorCode.SYSTEM_ERROR));
    }

    /**
     * 根据业务错误码解析 HTTP 状态。
     *
     * @param code 错误码
     * @return HTTP 状态
     */
    private HttpStatus resolveHttpStatus(Integer code) {
        if (code == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        if (StrUtil.equals(String.valueOf(code), String.valueOf(ErrorCode.PARAM_ERROR.getCode()))) {
            return HttpStatus.BAD_REQUEST;
        }
        if (StrUtil.equals(String.valueOf(code), String.valueOf(ErrorCode.NOT_FOUND.getCode()))) {
            return HttpStatus.NOT_FOUND;
        }
        if (StrUtil.equals(String.valueOf(code), String.valueOf(ErrorCode.CONFLICT.getCode()))) {
            return HttpStatus.CONFLICT;
        }
        if (StrUtil.equals(String.valueOf(code), String.valueOf(ErrorCode.MONGO_UNAVAILABLE.getCode()))) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

}
```

MongoDB 异常处理建议：

| 异常                                 | 常见原因                    | 建议响应                |
| ------------------------------------ | --------------------------- | ----------------------- |
| `DuplicateKeyException`              | 唯一索引冲突                | `409`，提示数据已存在   |
| `DataAccessResourceFailureException` | MongoDB 不可用、连接失败    | `503`，提示服务不可用   |
| `DataAccessException`                | 数据访问通用异常            | `500`，提示数据访问异常 |
| `BusinessException`                  | 业务规则不满足              | 按业务错误码返回        |
| `MethodArgumentNotValidException`    | `@RequestBody` 参数校验失败 | `400`，返回字段错误     |
| `BindException`                      | 查询参数或表单参数绑定失败  | `400`，返回字段错误     |

在生产环境中，日志应记录异常堆栈和关键业务上下文，但接口响应不应暴露数据库地址、集合名、索引内部名称、驱动堆栈等底层细节。

## 测试与验证

测试与验证用于确认实体映射、Repository、`MongoTemplate`、接口参数校验、异常处理和 MongoDB 数据写入是否符合预期。Spring Boot 测试体系支持普通单元测试、切片测试和完整集成测试；对于 MongoDB 相关测试，`@DataMongoTest` 会聚焦 MongoDB 组件，默认配置 `MongoTemplate`、扫描 `@Document` 实体并配置 Spring Data MongoDB Repository。([Home](https://docs.spring.io/spring-boot/4.1-SNAPSHOT/reference/testing/spring-boot-applications.html?utm_source=chatgpt.com))

### 单元测试

单元测试主要验证 Service 中的业务逻辑，不依赖真实 MongoDB。可以使用 Mockito Mock `MongoTemplate`，重点测试参数校验、重复数据判断、异常分支和返回结果转换。

测试依赖通常已经包含在 `spring-boot-starter-test` 中。如果需要使用 Mockito、JUnit Jupiter、AssertJ，`spring-boot-starter-test` 默认已经覆盖常用测试能力。

下面示例测试新增用户成功和用户名重复两个场景。

文件位置：`src/test/java/io/github/atengk/service/UserInfoApiServiceImplTest.java`

```java
package io.github.atengk.service;

import io.github.atengk.dto.UserCreateRequest;
import io.github.atengk.entity.UserInfo;
import io.github.atengk.exception.BusinessException;
import io.github.atengk.service.impl.UserInfoApiServiceImpl;
import io.github.atengk.vo.UserInfoVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 用户信息服务单元测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
class UserInfoApiServiceImplTest {

    private MongoTemplate mongoTemplate;

    private UserInfoApiServiceImpl userInfoApiService;

    @BeforeEach
    void setUp() {
        mongoTemplate = mock(MongoTemplate.class);
        userInfoApiService = new UserInfoApiServiceImpl(mongoTemplate);
    }

    @Test
    void createShouldSuccessWhenUsernameNotExists() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("ateng");
        request.setNickname("阿腾");
        request.setAge(28);
        request.setEmail("ateng@example.com");
        request.setMobile("13800000000");

        when(mongoTemplate.exists(any(Query.class), any(Class.class))).thenReturn(false);
        when(mongoTemplate.insert(any(UserInfo.class))).thenAnswer(invocation -> {
            UserInfo userInfo = invocation.getArgument(0);
            userInfo.setId("665f2d4f91f3a65a9f2d1001");
            return userInfo;
        });

        UserInfoVO result = userInfoApiService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("665f2d4f91f3a65a9f2d1001");
        assertThat(result.getUsername()).isEqualTo("ateng");
        assertThat(result.getNickname()).isEqualTo("阿腾");
    }

    @Test
    void createShouldThrowExceptionWhenUsernameExists() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("ateng");
        request.setNickname("阿腾");

        when(mongoTemplate.exists(any(Query.class), any(Class.class))).thenReturn(true);

        assertThatThrownBy(() -> userInfoApiService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名已存在");
    }

}
```

单元测试关注点：

| 测试点     | 说明                             |
| ---------- | -------------------------------- |
| 参数为空   | Service 是否抛出业务异常         |
| 数据重复   | 是否阻止重复新增                 |
| 数据不存在 | 查询、更新、删除是否返回正确异常 |
| 正常流程   | 返回字段是否正确                 |
| 异常分支   | 异常类型和错误消息是否符合预期   |

### 集成测试

集成测试用于验证 Spring Boot、Spring Data MongoDB、MongoDB Driver 和真实 MongoDB 服务之间的协作。Testcontainers 可以在测试时启动 Docker 容器，适合验证真实后端服务，Spring Boot 官方也将其作为集成测试真实依赖服务的一种方式。([Home](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html?utm_source=chatgpt.com))

测试依赖配置：

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot 测试基础依赖，包含 JUnit Jupiter、AssertJ、Mockito、Spring Test 等 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Spring Boot Testcontainers 支持，可配合服务连接或动态属性使用 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-testcontainers</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Testcontainers JUnit Jupiter 扩展 -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Testcontainers MongoDB 模块 -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>mongodb</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

下面示例使用 `@DataMongoTest` 验证 `MongoTemplate` 的新增、查询和删除能力。`@DataMongoTest` 只加载 MongoDB 相关组件，不会加载普通 `@Service`、`@Controller`，因此适合 Repository 和 MongoTemplate 层测试。([Home](https://docs.spring.io/spring-boot/3.5/api/java/org/springframework/boot/test/autoconfigure/data/mongo/DataMongoTest.html?utm_source=chatgpt.com))

文件位置：`src/test/java/io/github/atengk/mongo/UserInfoMongoTemplateTest.java`

```java
package io.github.atengk.mongo;

import io.github.atengk.entity.UserInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户信息 MongoTemplate 集成测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
@DataMongoTest
@Testcontainers
class UserInfoMongoTemplateTest {

    @Container
    static final MongoDBContainer MONGO_DB_CONTAINER = new MongoDBContainer("mongo:8.0");

    @Autowired
    private MongoTemplate mongoTemplate;

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO_DB_CONTAINER::getReplicaSetUrl);
    }

    @Test
    void shouldInsertAndFindUser() {
        UserInfo userInfo = new UserInfo();
        userInfo.setUsername("ateng");
        userInfo.setNickname("阿腾");
        userInfo.setAge(28);
        userInfo.setEnabled(Boolean.TRUE);
        userInfo.setCreatedAt(LocalDateTime.now());
        userInfo.setUpdatedAt(LocalDateTime.now());

        UserInfo savedUser = mongoTemplate.insert(userInfo);

        Query query = Query.query(Criteria.where("username").is("ateng"));
        UserInfo foundUser = mongoTemplate.findOne(query, UserInfo.class);

        assertThat(savedUser.getId()).isNotBlank();
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getUsername()).isEqualTo("ateng");
        assertThat(foundUser.getNickname()).isEqualTo("阿腾");
    }

    @Test
    void shouldRemoveUser() {
        UserInfo userInfo = new UserInfo();
        userInfo.setUsername("delete_user");
        userInfo.setNickname("待删除用户");
        userInfo.setEnabled(Boolean.TRUE);
        userInfo.setCreatedAt(LocalDateTime.now());
        userInfo.setUpdatedAt(LocalDateTime.now());

        mongoTemplate.insert(userInfo);

        Query query = Query.query(Criteria.where("username").is("delete_user"));
        mongoTemplate.remove(query, UserInfo.class);

        boolean exists = mongoTemplate.exists(query, UserInfo.class);
        assertThat(exists).isFalse();
    }

}
```

如果项目使用 Testcontainers 2.x，`MongoDBContainer` 的包路径可能是 `org.testcontainers.mongodb.MongoDBContainer`；如果使用 Testcontainers 1.x，常见包路径是 `org.testcontainers.containers.MongoDBContainer`。项目编译时以实际依赖版本为准。

执行测试：

```bash
mvn test
```

集成测试建议：

| 测试点             | 说明                             |
| ------------------ | -------------------------------- |
| 实体映射           | 字段名、集合名、主键映射是否正确 |
| 索引约束           | 唯一索引是否生效                 |
| Repository 方法    | 方法命名查询是否符合预期         |
| MongoTemplate 查询 | 条件查询、分页、排序是否正确     |
| 更新操作           | 局部更新是否只修改目标字段       |
| 删除操作           | 物理删除或逻辑删除是否符合设计   |

### 接口测试

接口测试用于验证 Controller 路由、请求体校验、查询参数校验、响应结构和异常处理。可以使用 `MockMvc` 做 Web 层测试，也可以用 curl、Postman、Apifox 等工具对运行中的服务做端到端测试。

下面示例使用 `@WebMvcTest` 测试新增用户接口的参数校验和正常响应。该测试只加载 Web 层，不连接 MongoDB。

文件位置：`src/test/java/io/github/atengk/controller/UserInfoApiControllerTest.java`

```java
package io.github.atengk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atengk.dto.UserCreateRequest;
import io.github.atengk.service.UserInfoApiService;
import io.github.atengk.vo.UserInfoVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户信息接口测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
@WebMvcTest(UserInfoApiController.class)
class UserInfoApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserInfoApiService userInfoApiService;

    @Test
    void createShouldSuccess() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("ateng");
        request.setNickname("阿腾");
        request.setAge(28);
        request.setEmail("ateng@example.com");
        request.setMobile("13800000000");

        UserInfoVO vo = new UserInfoVO();
        vo.setId("665f2d4f91f3a65a9f2d1001");
        vo.setUsername("ateng");
        vo.setNickname("阿腾");
        vo.setAge(28);
        vo.setEmail("ateng@example.com");
        vo.setMobile("13800000000");
        vo.setEnabled(Boolean.TRUE);
        vo.setCreatedAt(LocalDateTime.now());
        vo.setUpdatedAt(LocalDateTime.now());

        when(userInfoApiService.create(any(UserCreateRequest.class))).thenReturn(vo);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("ateng"))
                .andExpect(jsonPath("$.data.nickname").value("阿腾"));
    }

    @Test
    void createShouldReturnBadRequestWhenUsernameBlank() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("");
        request.setNickname("阿腾");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

}
```

也可以在服务启动后使用 curl 验证接口：

```bash
# 新增用户
curl -X POST 'http://localhost:8080/api/users' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "ateng",
    "nickname": "阿腾",
    "age": 28,
    "email": "ateng@example.com",
    "mobile": "13800000000",
    "tags": ["java", "springboot", "mongodb"]
  }'

# 参数校验失败示例
curl -X POST 'http://localhost:8080/api/users' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "",
    "nickname": "",
    "age": 200,
    "email": "error-email"
  }'

# 分页查询
curl -G 'http://localhost:8080/api/users/page' \
  --data-urlencode 'pageNum=1' \
  --data-urlencode 'pageSize=10' \
  --data-urlencode 'keyword=ateng' \
  --data-urlencode 'enabled=true'
```

接口测试关注点：

| 测试点       | 说明                                               |
| ------------ | -------------------------------------------------- |
| 正常请求     | HTTP 状态、响应 code、data 字段                    |
| 参数错误     | 是否返回 400 和明确错误消息                        |
| 业务异常     | 是否返回对应错误码                                 |
| MongoDB 异常 | 唯一索引冲突是否返回 409                           |
| 分页接口     | `pageNum`、`pageSize`、`total`、`records` 是否正确 |
| 删除接口     | 删除后再次查询是否返回不存在                       |

### 数据验证方式

数据验证用于确认 MongoDB 中实际写入的数据、索引、字段类型和查询计划是否符合设计。接口测试验证应用行为，数据验证则直接从 MongoDB 层确认集合、文档、索引和执行计划。

进入 MongoDB：

```bash
docker exec -it ateng-mongodb mongosh \
  -u root \
  -p root123456 \
  --authenticationDatabase admin
```

切换数据库：

```javascript
use ateng_mongo
```

查看集合：

```javascript
show collections
```

查询用户数据：

```javascript
db.user_info.find(
  { "username": "ateng" },
  {
    "username": 1,
    "nickname": 1,
    "enabled": 1,
    "created_at": 1,
    "updated_at": 1
  }
).pretty()
```

验证分页查询条件：

```javascript
db.user_info.find(
  {
    "enabled": true,
    "created_at": {
      "$gte": ISODate("2026-05-01T00:00:00Z"),
      "$lte": ISODate("2026-05-31T23:59:59Z")
    }
  }
).sort({ "created_at": -1 }).limit(10).pretty()
```

查看索引：

```javascript
db.user_info.getIndexes()
```

验证唯一索引：

```javascript
db.user_info.insertOne({
  "username": "ateng",
  "nickname": "重复用户",
  "enabled": true,
  "created_at": new Date(),
  "updated_at": new Date()
})
```

如果 `username` 已建立唯一索引，重复插入应返回 duplicate key 错误。应用层应捕获对应异常并返回 `409`。

查看执行计划：

```javascript
db.user_info.find({
  "enabled": true
}).sort({
  "created_at": -1
}).explain("executionStats")
```

重点关注字段：

| 字段                  | 说明                                |
| --------------------- | ----------------------------------- |
| `winningPlan`         | MongoDB 选择的执行计划              |
| `stage`               | 是否出现 `IXSCAN`，表示使用索引扫描 |
| `totalDocsExamined`   | 扫描文档数量                        |
| `totalKeysExamined`   | 扫描索引键数量                      |
| `executionTimeMillis` | 查询执行耗时                        |

验证 TTL 索引：

```javascript
db.operation_log.getIndexes()

db.operation_log.insertOne({
  "operator": "ateng",
  "operation_type": "TEST",
  "content": "TTL测试日志",
  "created_at": new Date(),
  "expire_at": new Date(Date.now() + 60000)
})
```

TTL 删除不是实时秒级，MongoDB 后台线程会周期性清理过期文档。验证 TTL 时可以等待一段时间后再次查询：

```javascript
db.operation_log.find({
  "operation_type": "TEST"
}).pretty()
```

常用数据验证清单：

| 验证项           | 验证命令                       |
| ---------------- | ------------------------------ |
| 集合是否存在     | `show collections`             |
| 文档是否写入     | `db.user_info.find().pretty()` |
| 字段是否正确映射 | 查询文档字段名                 |
| 唯一索引是否生效 | 插入重复数据                   |
| 普通索引是否存在 | `db.user_info.getIndexes()`    |
| 查询是否使用索引 | `explain("executionStats")`    |
| TTL 是否生效     | 插入过期时间并等待清理         |
| 删除是否成功     | 删除后再次查询                 |

测试与验证建议分层执行：单元测试保证业务逻辑，集成测试保证 MongoDB 访问链路，接口测试保证 HTTP 入参和响应结构，数据验证保证最终落库结果与索引设计一致。