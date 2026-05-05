# Spring Cloud Stream

Spring Cloud Stream 是 Spring Cloud 体系中面向消息驱动微服务的开发框架，用于屏蔽 RabbitMQ、Kafka 等消息中间件的接入差异，让业务代码以统一的函数式模型完成消息生产、消费、转换和路由。官方文档对其定位是：基于 Spring Boot 构建生产级应用，并通过 Spring Integration 连接消息代理，同时引入发布订阅、消费组和分区等概念。([Home](https://docs.spring.io/spring-cloud-stream/docs/current-snapshot/reference/htmlsingle/index.html))

## 技术概述

### Spring Cloud Stream 定位

Spring Cloud Stream 的核心定位不是替代 RabbitMQ、Kafka 原生客户端，而是在 Spring Boot 应用和消息中间件之间增加一层统一抽象。开发者主要关注业务函数、输入输出绑定、消息结构和异常处理；中间件连接、目标队列或 Topic 绑定、消息转换、消费组、分区等底层细节由 Binder 负责处理。

在 Spring Boot 3 项目中，推荐使用 Spring Cloud Stream 的函数式编程模型，也就是通过 `Supplier`、`Function`、`Consumer` 暴露消息生产、处理和消费逻辑。官方文档明确说明，从 Spring Cloud Stream 3.x 开始推荐使用函数式实现。([Home](https://docs.spring.io/spring-cloud-stream/reference/4.3/spring-cloud-stream/producing-and-consuming-messages.html?utm_source=chatgpt.com))

它在系统架构中的位置可以理解为：

```text
业务服务
  ↓
Spring Cloud Stream 函数式模型
  ↓
Binding 输入/输出绑定
  ↓
Binder 中间件适配层
  ↓
RabbitMQ / Kafka / 其他消息系统
```

实际开发中，Spring Cloud Stream 更适合做“业务事件流转层”。例如订单创建后发布 `order.created` 事件，库存服务、积分服务、通知服务分别消费该事件，而订单服务不需要直接依赖这些下游服务。

### 核心概念

Spring Cloud Stream 的核心概念围绕“业务函数”和“消息绑定”展开。官方文档描述的应用模型是：应用本身保持中间件无关，通过输入、输出绑定与外部 Broker 的 Destination 建立连接，具体连接细节由 Binder 处理。([Home](https://docs.spring.io/spring-cloud-stream/docs/current-snapshot/reference/htmlsingle/index.html))

| 概念           | 说明                                                         | 常见配置或代码                            |
| -------------- | ------------------------------------------------------------ | ----------------------------------------- |
| Message        | 消息数据结构，包含 `payload` 和 `headers`                    | `Message<T>`                              |
| Destination    | 消息目标，在 Kafka 中通常是 Topic，在 RabbitMQ 中通常对应 Exchange 或 Queue 绑定关系 | `destination: order-event`                |
| Binder         | 消息中间件适配器，负责连接 RabbitMQ、Kafka 等具体系统        | `spring-cloud-starter-stream-rabbit`      |
| Binding        | 应用函数输入/输出与 Destination 的绑定关系                   | `orderConsumer-in-0`                      |
| Supplier       | 无输入、有输出，常用于定时或主动生产消息                     | `Supplier<Message<OrderEvent>>`           |
| Function       | 有输入、有输出，常用于消息转换、加工、路由                   | `Function<OrderEvent, NotifyEvent>`       |
| Consumer       | 有输入、无输出，常用于最终消费和业务落库                     | `Consumer<OrderEvent>`                    |
| Consumer Group | 消费组，同组内多个实例竞争消费，不同组之间各自获得一份消息   | `group: order-service`                    |
| Partition      | 分区，用于按业务键保证局部顺序和横向扩展                     | `partitionKeyExpression: payload.orderId` |
| StreamBridge   | 用于在 REST 接口、定时任务、业务服务等非函数入口中主动发送消息 | `streamBridge.send("order-out-0", data)`  |

消费组是理解消息消费行为的重点。同一个 Destination 下，不同消费组都会收到一份消息；同一个消费组内，通常只有一个实例处理某一条消息，适合服务多实例扩容。官方文档也说明了这个竞争消费模型。([Home](https://docs.spring.io/spring-cloud-stream/docs/current-snapshot/reference/htmlsingle/index.html))

### 适用场景

Spring Cloud Stream 适合用于服务间异步解耦、事件驱动架构、消息广播、削峰填谷、数据同步和流式处理前置接入。

典型场景如下：

| 场景         | 示例                                  | 适用原因                               |
| ------------ | ------------------------------------- | -------------------------------------- |
| 业务事件发布 | 订单创建、支付成功、退款完成          | 上游只发布事件，不关心下游有多少消费者 |
| 服务异步解耦 | 下单后异步扣库存、发短信、加积分      | 降低接口链路耗时，避免同步调用雪崩     |
| 广播消费     | 多个系统同时订阅用户变更事件          | 不同消费组分别接收同一份消息           |
| 分组消费     | 订单服务部署 3 个实例共同消费订单消息 | 同组竞争消费，便于横向扩容             |
| 流水型处理   | 原始日志 → 清洗 → 聚合 → 入库         | 可用 `Function` 组合形成处理链         |
| 削峰填谷     | 秒杀、活动报名、批量导入              | 请求先入消息队列，后端按消费能力处理   |
| 跨系统同步   | 主系统变更后同步搜索索引、缓存、报表  | 消息驱动最终一致性                     |

不建议使用的场景也需要明确：强同步查询、强一致事务、必须立即返回下游处理结果的接口，不应优先使用消息流。如果业务必须在同一个事务内同时完成多个系统状态变更，应优先评估本地事务、补偿事务、Saga 或 TCC，而不是简单依赖消息发送。

## 开发环境准备

### 版本选型

Spring Boot 3 项目需要选择匹配的 Spring Cloud Release Train。Spring 官方兼容表显示，Spring Cloud `2025.0.x` 对应 Spring Boot `3.5.x`，Spring Cloud `2024.0.x` 对应 Spring Boot `3.4.x`，Spring Cloud `2023.0.x` 对应 Spring Boot `3.3.x`、`3.2.x`。([Home](https://spring.io/projects/spring-cloud/))

截至 2026-05-05，Spring Cloud `2025.0.2` 发布说明中包含 Spring Cloud Stream `4.3.2`，适合作为 Spring Boot 3.5.x 项目的稳定组合；Spring Boot `3.5.12` 已发布并可从 Maven Central 获取。([GitHub](https://github.com/spring-cloud/spring-cloud-release/wiki/Spring-Cloud-2025.0-Release-Notes)) ([Home](https://spring.io/blog/2026/03/19/spring-boot-3-5-12-available-now?utm_source=chatgpt.com))

推荐版本组合如下：

| 组件                | 推荐版本 | 说明                                                   |
| ------------------- | -------- | ------------------------------------------------------ |
| JDK                 | 17 或 21 | Spring Boot 3 最低要求 JDK 17；生产环境建议统一 JDK 21 |
| Spring Boot         | 3.5.12   | Spring Boot 3.5.x 当前维护版本示例                     |
| Spring Cloud        | 2025.0.2 | 与 Spring Boot 3.5.x 匹配                              |
| Spring Cloud Stream | 4.3.2    | 由 Spring Cloud BOM 管理，不建议手写版本               |
| Maven               | 3.9.x    | 兼容 Spring Boot 3.x 构建                              |
| RabbitMQ            | 3.x      | 适合普通业务消息、延迟队列、路由场景                   |
| Kafka               | 3.x/4.x  | 适合高吞吐、日志流、顺序分区、流式处理场景             |
| Docker              | 24+      | 本地快速启动消息中间件                                 |

Binder 选择建议：

| Binder          | 适用情况                                     | 依赖                                 |
| --------------- | -------------------------------------------- | ------------------------------------ |
| RabbitMQ Binder | 业务消息、路由、延迟消息、死信队列、后台任务 | `spring-cloud-starter-stream-rabbit` |
| Kafka Binder    | 高吞吐日志、行为流、事件流、按分区顺序处理   | `spring-cloud-starter-stream-kafka`  |
| Test Binder     | 单元测试、无中间件测试消息逻辑               | `spring-cloud-stream-test-binder`    |

如果是普通业务系统，建议先用 RabbitMQ Binder 起步，配置和排查成本较低。如果是日志、埋点、交易流水、设备上报等高吞吐场景，优先选择 Kafka Binder。

### Maven 依赖配置

Spring Cloud Stream 的依赖建议通过 Spring Cloud BOM 管理版本，不要给 `spring-cloud-starter-stream-rabbit` 或 `spring-cloud-starter-stream-kafka` 单独指定版本。RabbitMQ Binder 和 Kafka Binder 官方文档都提供了对应 starter 依赖方式。([docs.enterprise.spring.io](https://docs.enterprise.spring.io/spring-cloud-stream/docs/4.0.8/reference/html/spring-cloud-stream-binder-rabbit.html?utm_source=chatgpt.com)) ([docs.enterprise.spring.io](https://docs.enterprise.spring.io/spring-cloud-stream/docs/4.0.8/reference/html/spring-cloud-stream-binder-kafka.html?utm_source=chatgpt.com))

文件位置：`pom.xml`

下面的 Maven 配置以 RabbitMQ Binder 为默认开发环境，同时提供 Kafka Profile，便于后续切换中间件。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot 3.5.x 版本，需与 Spring Cloud 2025.0.x 搭配 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.12</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>spring-cloud-stream-demo</artifactId>
    <version>1.0.0</version>
    <name>spring-cloud-stream-demo</name>
    <description>Spring Boot 3 Spring Cloud Stream 开发示例</description>

    <properties>
        <!-- Spring Boot 3 最低 JDK 17，生产环境可统一使用 JDK 21 -->
        <java.version>17</java.version>

        <!-- Spring Cloud 2025.0.x 适配 Spring Boot 3.5.x -->
        <spring-cloud.version>2025.0.2</spring-cloud.version>

        <!-- Hutool 常用工具类，业务代码中可用于 JSON、日期、字符串、集合等处理 -->
        <hutool.version>5.8.37</hutool.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- 使用 Spring Cloud BOM 统一管理 Spring Cloud Stream、Binder、Function 等版本 -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- 提供 REST 接口，便于后续用 HTTP 触发消息发送 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- 暴露健康检查、指标监控等端点，便于观察消息应用运行状态 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Spring Cloud Stream 核心能力由具体 Binder Starter 间接引入 -->

        <!-- Hutool 工具包，后续业务代码中可用于 JSON、ID、日期、集合等通用处理 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok 简化 DTO、事件对象、日志对象代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot 单元测试基础依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Spring Cloud Stream 测试 Binder，可在不启动 RabbitMQ/Kafka 的情况下测试消息逻辑 -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-stream-test-binder</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <profiles>
        <!-- 默认使用 RabbitMQ Binder，适合普通业务消息开发 -->
        <profile>
            <id>rabbit</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <dependencies>
                <dependency>
                    <groupId>org.springframework.cloud</groupId>
                    <artifactId>spring-cloud-starter-stream-rabbit</artifactId>
                </dependency>
            </dependencies>
        </profile>

        <!-- 使用 Kafka Binder 时，通过 mvn -Pkafka 启用 -->
        <profile>
            <id>kafka</id>
            <dependencies>
                <dependency>
                    <groupId>org.springframework.cloud</groupId>
                    <artifactId>spring-cloud-starter-stream-kafka</artifactId>
                </dependency>
            </dependencies>
        </profile>
    </profiles>

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

常用构建命令如下：

```bash
# 使用默认 RabbitMQ Binder 构建
mvn clean package

# 使用 Kafka Binder 构建
mvn clean package -Pkafka

# 跳过测试快速打包
mvn clean package -DskipTests
```

`-Pkafka` 用于启用 Kafka Profile。默认情况下会启用 `rabbit` Profile，适合本地先完成普通消息发送、消费、异常处理和死信队列验证。

注意：除非明确需要多 Binder 场景，不建议 RabbitMQ Binder 和 Kafka Binder 同时放到主依赖中。多个 Binder 同时存在时，需要通过 `spring.cloud.stream.defaultBinder` 或每个 Binding 的 `binder` 属性明确指定，否则配置复杂度会增加。

### 消息中间件准备

开发环境建议使用 Docker Compose 启动中间件，避免本地安装污染系统环境。普通业务消息建议先准备 RabbitMQ；如果后续章节涉及高吞吐、分区顺序、日志流或 Kafka Streams，再准备 Kafka。

文件位置：`docker-compose-rabbitmq.yml`

下面的配置用于启动本地 RabbitMQ，并开启管理控制台。

```yaml
services:
  rabbitmq:
    image: rabbitmq:3-management
    container_name: ateng-rabbitmq
    restart: unless-stopped
    ports:
      # AMQP 连接端口，Spring Cloud Stream Rabbit Binder 默认连接该端口
      - "5672:5672"
      # RabbitMQ 管理控制台端口
      - "15672:15672"
    environment:
      # 本地开发账号
      RABBITMQ_DEFAULT_USER: ateng
      # 本地开发密码，生产环境必须改为密文或环境变量注入
      RABBITMQ_DEFAULT_PASS: 123456
    volumes:
      # 持久化 RabbitMQ 数据，避免容器重启后队列和交换机丢失
      - rabbitmq_data:/var/lib/rabbitmq

volumes:
  rabbitmq_data:
```

启动 RabbitMQ：

```bash
docker compose -f docker-compose-rabbitmq.yml up -d
docker ps | grep ateng-rabbitmq
```

访问管理控制台：

```text
地址：http://localhost:15672
账号：ateng
密码：123456
```

Spring Boot 本地连接 RabbitMQ 的基础参数如下，后续“基础配置”章节可以在此基础上继续补充 Binding 配置。

```yaml
spring:
  rabbitmq:
    # RabbitMQ 服务地址
    host: localhost
    # AMQP 连接端口
    port: 5672
    # 本地开发账号
    username: ateng
    # 本地开发密码
    password: 123456
```

文件位置：`docker-compose-kafka.yml`

下面的配置用于启动单节点 Kafka，适合本地开发和功能验证。

```yaml
services:
  kafka:
    image: apache/kafka:3.9.1
    container_name: ateng-kafka
    restart: unless-stopped
    ports:
      # Kafka 客户端连接端口
      - "9092:9092"
    environment:
      # 单节点 KRaft 模式节点 ID
      KAFKA_NODE_ID: 1
      # 同一个节点同时作为 broker 和 controller
      KAFKA_PROCESS_ROLES: broker,controller
      # Broker 与 Controller 监听地址
      KAFKA_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      # 暴露给本机应用访问的 Kafka 地址
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      # Controller 使用的监听器名称
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      # 监听器协议映射
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      # 单节点 Controller 投票配置
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093
      # 本地开发环境副本数设置为 1
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      # 减少本地消费组首次 rebalance 等待时间
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
      # 默认 Topic 分区数
      KAFKA_NUM_PARTITIONS: 3
    volumes:
      # 持久化 Kafka 数据
      - kafka_data:/tmp/kraft-combined-logs

volumes:
  kafka_data:
```

启动 Kafka：

```bash
docker compose -f docker-compose-kafka.yml up -d
docker ps | grep ateng-kafka
```

进入 Kafka 容器并创建测试 Topic：

```bash
docker exec -it ateng-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create \
  --topic order-event \
  --partitions 3 \
  --replication-factor 1
```

查看 Topic：

```bash
docker exec -it ateng-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --list
```

Spring Boot 本地连接 Kafka 的基础参数如下，后续“基础配置”章节可以在此基础上继续补充 Binding、消费组、分区和重试配置。

```yaml
spring:
  cloud:
    stream:
      kafka:
        binder:
          # Kafka Broker 地址
          brokers: localhost:9092
```

本地开发建议先确认以下检查项：

| 检查项                    | RabbitMQ                          | Kafka                        |
| ------------------------- | --------------------------------- | ---------------------------- |
| 容器是否运行              | `docker ps                        | grep ateng-rabbitmq`         |
| 管理入口                  | `http://localhost:15672`          | 通过 Kafka CLI 查看          |
| 应用连接端口              | `5672`                            | `9092`                       |
| 用户密码                  | `ateng / 123456`                  | 默认无认证                   |
| 是否需要提前建 Topic/队列 | RabbitMQ 通常可由 Binder 自动声明 | Kafka 建议本地提前创建 Topic |
| 是否适合入门章节          | 是                                | 适合进阶消息和高吞吐场景     |

本章完成后，项目应具备三个基础条件：Spring Boot 3.5.x 与 Spring Cloud 2025.0.x 版本匹配，Maven 已通过 BOM 管理 Spring Cloud Stream 依赖，RabbitMQ 或 Kafka 至少有一个中间件可以在本地启动并被应用连接。

## 基础配置

本章节继续补充 Spring Boot 3 + Spring Cloud Stream 的基础配置内容，覆盖应用公共配置、Binder 中间件适配配置、Binding 输入输出绑定配置，以及消费组配置。以下内容对应你上传的大纲中的“基础配置”部分。

### 应用基础配置

应用基础配置用于定义服务名、端口、激活环境、函数绑定入口、消息序列化类型和监控端点。Spring Cloud Stream 使用 Spring Cloud Function 作为函数式编程模型基础，通常通过 `spring.cloud.function.definition` 指定需要绑定到消息系统的函数；当应用中存在多个函数 Bean 时，建议显式配置该属性。([Home](https://docs.spring.io/spring-cloud-function/docs/current-SNAPSHOT/reference/html/spring-cloud-function.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

下面的配置提供应用公共参数，不直接绑定具体中间件，RabbitMQ 或 Kafka 的连接参数放到对应环境配置中。

```yaml
server:
  # 应用 HTTP 端口，方便后续通过 REST 接口触发消息发送
  port: 18080

spring:
  application:
    # 应用名称，建议与服务注册、日志检索、链路追踪中的服务名保持一致
    name: spring-cloud-stream-demo

  profiles:
    # 默认使用 RabbitMQ 环境；切换 Kafka 时可使用 --spring.profiles.active=kafka
    active: rabbit

  jackson:
    # JSON 时间字段序列化格式
    date-format: yyyy-MM-dd HH:mm:ss
    # JSON 时间字段时区
    time-zone: Asia/Shanghai

  cloud:
    function:
      # 指定需要由 Spring Cloud Stream 绑定的函数 Bean
      # 多个函数使用英文分号分隔，例如：orderCreatedConsumer;orderNotifyFunction
      definition: orderCreatedConsumer

    stream:
      # 默认消息内容类型，通常使用 JSON 承载业务事件
      default:
        contentType: application/json

      # 默认 Binder 名称；单 Binder 应用可以不配置，多 Binder 应用建议显式配置
      defaultBinder: rabbit

management:
  endpoints:
    web:
      exposure:
        # 暴露健康检查和基础指标，便于本地调试与容器探针使用
        include: health,info,metrics

  endpoint:
    health:
      # 显示详细健康信息，本地开发方便排查 Binder 连接状态
      show-details: always

logging:
  level:
    # 业务包日志级别
    io.github.atengk: info
    # Spring Cloud Stream 核心日志，本地排查绑定问题时可临时调整为 debug
    org.springframework.cloud.stream: info
```

应用基础配置中最关键的是 `spring.cloud.function.definition` 和 `spring.cloud.stream.default.contentType`。前者决定哪些函数 Bean 会被 Spring Cloud Stream 识别并创建 Binding；后者决定消息默认序列化类型。Spring Cloud Stream 的 Binding 通用属性支持通过 `spring.cloud.stream.default.*` 设置所有 Binding 的默认值，也支持通过 `spring.cloud.stream.bindings.<bindingName>.*` 针对单个 Binding 覆盖。([Home](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/binding-properties.html?utm_source=chatgpt.com))

常见配置项说明如下：

| 配置项                                      | 作用                        | 建议值                                   |
| ------------------------------------------- | --------------------------- | ---------------------------------------- |
| `spring.application.name`                   | 应用名称                    | 与服务名一致                             |
| `spring.profiles.active`                    | 激活环境                    | `rabbit`、`kafka`、`dev`、`test`、`prod` |
| `spring.cloud.function.definition`          | 指定参与消息绑定的函数 Bean | 单函数写函数名，多函数用 `;` 分隔        |
| `spring.cloud.stream.default.contentType`   | 默认消息类型                | `application/json`                       |
| `spring.cloud.stream.defaultBinder`         | 默认 Binder                 | 单 Binder 可省略，多 Binder 建议配置     |
| `management.endpoints.web.exposure.include` | 监控端点暴露范围            | 本地可多一些，生产按需收敛               |

函数 Bean 名称和 Binding 名称存在固定关系。函数式模型下，输入 Binding 默认命名为 `<functionName>-in-<index>`，输出 Binding 默认命名为 `<functionName>-out-<index>`；普通单输入或单输出函数的 `index` 通常是 `0`。([Home](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/functional-binding-names.html?utm_source=chatgpt.com))

| 函数类型         | Bean 名称              | 输入 Binding                | 输出 Binding                 |
| ---------------- | ---------------------- | --------------------------- | ---------------------------- |
| `Consumer<T>`    | `orderCreatedConsumer` | `orderCreatedConsumer-in-0` | 无                           |
| `Supplier<T>`    | `orderCreatedSupplier` | 无                          | `orderCreatedSupplier-out-0` |
| `Function<T, R>` | `orderNotifyFunction`  | `orderNotifyFunction-in-0`  | `orderNotifyFunction-out-0`  |

### Binder 配置

Binder 配置用于指定 Spring Cloud Stream 连接哪个消息中间件。Binder 是 Spring Cloud Stream 对 RabbitMQ、Kafka 等 Broker 的适配层；通用 Binder 配置使用 `spring.cloud.stream.binders.<configurationName>` 前缀，其中 `type` 表示 Binder 类型。([Home](https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/spring-cloud-stream.html?utm_source=chatgpt.com))

单 Binder 应用通常不需要显式配置 `spring.cloud.stream.binders.*`，只需要引入一个 Binder 依赖并配置中间件连接参数即可。多 Binder 应用才需要显式定义多个 Binder，并在不同 Binding 上指定 `binder`。

文件位置：`src/main/resources/application-rabbit.yml`

下面的配置用于 RabbitMQ Binder。本地普通业务消息建议先使用该配置。

```yaml
spring:
  cloud:
    stream:
      # 当前环境默认使用 RabbitMQ Binder
      defaultBinder: rabbit

      bindings:
        orderCreatedConsumer-in-0:
          # 消费订单创建事件
          destination: order.created
          # 消费组名称，生产环境必须配置
          group: order-service

      rabbit:
        default:
          consumer:
            # 自动声明死信队列，后续异常处理章节会继续展开
            autoBindDlq: true
            # 消费失败后将异常信息重新发布到死信队列消息头中
            republishToDlq: true
          producer:
            # RabbitMQ 生产端路由键表达式，普通场景可不配置
            routingKeyExpression: '''order.created'''

  rabbitmq:
    # RabbitMQ 服务地址
    host: localhost
    # RabbitMQ AMQP 端口
    port: 5672
    # 本地开发账号
    username: ateng
    # 本地开发密码
    password: 123456
    # 连接超时时间
    connection-timeout: 10s
```

RabbitMQ Binder 默认使用 Spring Boot 提供的 `ConnectionFactory`，因此连接地址、端口、账号、密码等基础连接项使用 `spring.rabbitmq.*` 前缀。RabbitMQ Binder 还支持自己的扩展配置，例如 `spring.cloud.stream.rabbit.binder.*` 和 `spring.cloud.stream.rabbit.bindings.<channelName>.consumer.*`。([Home](https://docs.spring.io/spring-cloud-stream/reference/rabbit/rabbit_overview/binder-properties.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application-kafka.yml`

下面的配置用于 Kafka Binder。适合高吞吐、分区顺序消费、日志流和事件流场景。

```yaml
spring:
  cloud:
    stream:
      # 当前环境默认使用 Kafka Binder
      defaultBinder: kafka

      bindings:
        orderCreatedConsumer-in-0:
          # Kafka Topic 名称
          destination: order.created
          # Kafka Consumer Group ID
          group: order-service

      kafka:
        binder:
          # Kafka Broker 地址，多个地址使用英文逗号分隔
          brokers: localhost:9092
          # 本地开发允许自动创建 Topic，生产环境建议关闭并由运维或 IaC 管理
          autoCreateTopics: true
          # 本地开发分区自动扩容默认关闭，避免误改生产 Topic 分区
          autoAddPartitions: false
          # 生产端确认级别，生产环境可根据可靠性要求调整
          requiredAcks: 1

          configuration:
            # Kafka 客户端通用配置，生产环境可在这里配置安全协议、认证、SSL 等参数
            client.id: ${spring.application.name}

        default:
          consumer:
            # 默认消费并发数；不能盲目大于 Topic 分区数
            concurrency: 1
```

Kafka Binder 的 `spring.cloud.stream.kafka.binder.brokers` 用于配置 Broker 列表，默认 Broker 地址是 `localhost`，默认端口是 `9092`；`configuration` 可向生产者和消费者传递通用 Kafka 客户端属性。([Home](https://docs.spring.io/spring-cloud-stream/reference/kafka/kafka-binder/config-options.html?utm_source=chatgpt.com))

如果一个应用同时需要连接 RabbitMQ 和 Kafka，可以使用多 Binder 配置。多 Binder 不建议作为入门默认方案，因为配置复杂度和排查成本更高。

文件位置：`src/main/resources/application-multi-binder.yml`

下面的配置演示同一应用中同时声明 RabbitMQ Binder 和 Kafka Binder，并在 Binding 上显式指定使用哪个 Binder。

```yaml
spring:
  cloud:
    stream:
      # 多 Binder 场景必须明确默认 Binder
      defaultBinder: rabbit

      binders:
        rabbit:
          # Binder 类型，对应 classpath 中的 Rabbit Binder
          type: rabbit
          # 是否继承应用主环境配置
          inheritEnvironment: true
          environment:
            spring:
              rabbitmq:
                host: localhost
                port: 5672
                username: ateng
                password: 123456

        kafka:
          # Binder 类型，对应 classpath 中的 Kafka Binder
          type: kafka
          inheritEnvironment: true
          environment:
            spring:
              cloud:
                stream:
                  kafka:
                    binder:
                      brokers: localhost:9092

      bindings:
        orderCreatedConsumer-in-0:
          # 订单事件从 RabbitMQ 消费
          binder: rabbit
          destination: order.created
          group: order-service

        orderLogConsumer-in-0:
          # 日志事件从 Kafka 消费
          binder: kafka
          destination: order.log
          group: order-log-service
```

多 Binder 场景下，`defaultBinder` 只解决未显式指定 Binder 的 Binding 归属。为了降低误连风险，建议每个关键 Binding 都配置 `binder`，尤其是订单、支付、库存这类核心业务消息。

### Binding 配置

Binding 配置用于把函数 Bean 的输入、输出绑定到具体消息目标。Spring Cloud Stream 官方文档说明，Binding 属性格式是 `spring.cloud.stream.bindings.<bindingName>.<property>`，其中 `<bindingName>` 就是需要配置的输入或输出绑定名称。([Home](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/binding-properties.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application-rabbit.yml`

下面的配置演示 `Consumer`、`Supplier`、`Function` 三类函数的常见 Binding 写法。

```yaml
spring:
  cloud:
    function:
      # 同时启用三个函数
      # orderCreatedConsumer：消费订单创建事件
      # orderCreatedSupplier：主动生产订单创建事件
      # orderNotifyFunction：消费订单事件并输出通知事件
      definition: orderCreatedConsumer;orderCreatedSupplier;orderNotifyFunction

    stream:
      bindings:
        orderCreatedConsumer-in-0:
          # 输入 Binding，监听订单创建事件
          destination: order.created
          # 消费组，同组内多个实例竞争消费
          group: order-service
          # 当前 Binding 单独指定消息类型
          contentType: application/json
          consumer:
            # 消费失败最大尝试次数，包含首次消费
            maxAttempts: 3
            # 单实例消费并发数，RabbitMQ 会创建多个消费者线程
            concurrency: 2

        orderCreatedSupplier-out-0:
          # 输出 Binding，向订单创建事件目标发送消息
          destination: order.created
          contentType: application/json
          producer:
            # 是否要求生产端分区，普通 RabbitMQ 场景可不启用
            partitionCount: 1

        orderNotifyFunction-in-0:
          # Function 输入端，消费订单创建事件
          destination: order.created
          group: order-notify-service
          contentType: application/json

        orderNotifyFunction-out-0:
          # Function 输出端，发送订单通知事件
          destination: order.notify
          contentType: application/json
```

Binding 配置中最常用的属性如下：

| 属性                              | 适用方向   | 说明                                                         |
| --------------------------------- | ---------- | ------------------------------------------------------------ |
| `destination`                     | 输入、输出 | 消息目标；RabbitMQ 通常映射到 Exchange，Kafka 通常映射到 Topic |
| `group`                           | 输入       | 消费组；只对消费者 Binding 生效                              |
| `contentType`                     | 输入、输出 | 消息内容类型，默认通常使用 `application/json`                |
| `binder`                          | 输入、输出 | 指定当前 Binding 使用哪个 Binder                             |
| `consumer.maxAttempts`            | 输入       | 消费失败最大尝试次数                                         |
| `consumer.concurrency`            | 输入       | 消费并发数                                                   |
| `producer.partitionCount`         | 输出       | 生产端分区数量                                               |
| `producer.partitionKeyExpression` | 输出       | 分区键表达式                                                 |

如果配置项很多，可以把公共配置提取到默认配置中，减少重复。

文件位置：`src/main/resources/application.yml`

下面的配置演示如何设置全局默认 Binding 属性，再在具体 Binding 中覆盖。

```yaml
spring:
  cloud:
    stream:
      default:
        # 所有 Binding 默认使用 JSON
        contentType: application/json

      bindings:
        orderCreatedConsumer-in-0:
          # 覆盖具体输入 Binding 的消息目标
          destination: order.created
          group: order-service

        orderNotifyConsumer-in-0:
          # 另一个消费者监听同一个目标，但属于不同消费组
          destination: order.created
          group: order-notify-service
```

函数式 Binding 默认命名虽然可以通过 `spring.cloud.stream.function.bindings.<binding-name>` 映射为自定义名称，但官方文档并不建议在普通场景中过度使用这种“重命名”，因为它会增加函数名称和 Binding 名称之间的间接映射。([Home](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/functional-binding-names.html?utm_source=chatgpt.com))

不推荐的复杂映射方式如下：

```yaml
spring:
  cloud:
    stream:
      function:
        bindings:
          # 将 orderCreatedConsumer-in-0 映射为 input
          # 普通业务项目不建议这样写，排查时不容易从配置反推函数名
          orderCreatedConsumer-in-0: input

      bindings:
        input:
          destination: order.created
          group: order-service
```

推荐直接使用默认函数式 Binding 名称：

```yaml
spring:
  cloud:
    stream:
      bindings:
        # 从名称可以直接看出该 Binding 属于 orderCreatedConsumer 函数的输入端
        orderCreatedConsumer-in-0:
          destination: order.created
          group: order-service
```

### 消费组配置

消费组配置用于控制消息在多个服务实例和多个业务消费者之间的分发行为。`group` 属性只适用于输入 Binding；默认值为 `null`，表示匿名消费者。匿名消费者适合临时调试，不适合生产环境，因为 RabbitMQ 场景下通常会创建自动删除队列，Kafka 场景下也不利于稳定维护消费位点。([Home](https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/spring-cloud-stream.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

下面的配置演示“同组竞争消费”。多个 `order-service` 实例使用同一个消费组时，一条消息通常只会被其中一个实例处理。

```yaml
spring:
  cloud:
    function:
      # 启用订单创建事件消费者
      definition: orderCreatedConsumer

    stream:
      bindings:
        orderCreatedConsumer-in-0:
          # 消息目标
          destination: order.created
          # 同一个服务的多个实例必须使用相同 group，形成竞争消费
          group: order-service
          consumer:
            # 单实例消费并发数，根据业务处理耗时和中间件能力调整
            concurrency: 2
            # 消费失败最多尝试 3 次
            maxAttempts: 3
```

同组竞争消费适合服务多实例部署，例如 `order-service` 部署 3 个实例，三个实例都配置 `group: order-service`。这样可以提高消费吞吐，同时避免同一条消息被同一个业务服务重复处理。

下面的配置演示“不同组广播消费”。订单创建事件会分别被订单服务、通知服务、审计服务消费。

```yaml
spring:
  cloud:
    function:
      # 启用多个消费者函数
      definition: orderCreatedConsumer;orderNotifyConsumer;orderAuditConsumer

    stream:
      bindings:
        orderCreatedConsumer-in-0:
          # 三个消费者监听同一个消息目标
          destination: order.created
          # 订单服务消费组
          group: order-service

        orderNotifyConsumer-in-0:
          destination: order.created
          # 通知服务消费组
          group: order-notify-service

        orderAuditConsumer-in-0:
          destination: order.created
          # 审计服务消费组
          group: order-audit-service
```

不同消费组会各自收到一份消息，适合事件广播场景。例如订单创建后，订单服务更新状态，通知服务发送短信，审计服务记录审计日志，三个动作互不影响。

RabbitMQ Binder 中，默认会把每个 `destination` 映射到一个 `TopicExchange`，每个消费组会绑定一个队列到该 Exchange；同一个消费组中的消费者实例消费该组队列中的消息。([Spring Enterprise Docs](https://docs.enterprise.spring.io/spring-cloud-stream/docs/4.0.8/reference/html/spring-cloud-stream-binder-rabbit.html?utm_source=chatgpt.com))

消费组命名建议如下：

| 场景                 | 推荐 group                | 说明                                 |
| -------------------- | ------------------------- | ------------------------------------ |
| 订单服务消费订单事件 | `order-service`           | 服务名作为消费组，便于多实例竞争消费 |
| 通知服务消费订单事件 | `order-notify-service`    | 与订单服务分组不同，可独立收到消息   |
| 审计服务消费订单事件 | `order-audit-service`     | 单独保留审计消费链路                 |
| 临时本地调试         | `order-debug-${username}` | 避免抢占正式消费组消息               |
| 生产环境             | 必须固定 group            | 不建议匿名消费                       |

生产环境必须避免以下配置：

```yaml
spring:
  cloud:
    stream:
      bindings:
        orderCreatedConsumer-in-0:
          destination: order.created
          # 不配置 group 会形成匿名消费，不适合生产环境
          # group: 
```

更稳妥的生产配置如下：

```yaml
spring:
  cloud:
    stream:
      bindings:
        orderCreatedConsumer-in-0:
          destination: order.created
          # 固定消费组，保证服务重启后仍按同一组继续消费
          group: order-service
          consumer:
            # 根据业务处理能力设置并发，不要盲目加大
            concurrency: 4
            # 失败重试次数，后续应配合死信队列和告警处理
            maxAttempts: 3
```

消费组配置的基本原则是：同一个业务服务的多个实例使用相同 `group`，不同业务服务使用不同 `group`，本地调试不要使用生产消费组，生产环境不要匿名消费。这样可以同时兼顾横向扩容、事件广播和消费位点稳定性。

## 消息生产者开发

消息生产者负责把业务事件转换为标准消息，并发送到指定的输出 Binding。Spring Cloud Stream 4.x 推荐函数式模型，输出 Binding 默认命名规则是 `<functionName>-out-0`；如果使用 `StreamBridge` 从 REST 接口、业务服务、定时任务等非 Stream 函数入口发送消息，也可以通过 `spring.cloud.stream.output-bindings` 预创建输出 Binding。([Home](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/functional-binding-names.html?utm_source=chatgpt.com))

### 输出通道定义

输出通道本质上是“业务代码中的发送入口”和“消息中间件中的目标 Destination”之间的绑定关系。生产者不直接操作 RabbitMQ Exchange 或 Kafka Topic，而是向某个 Binding 名称发送消息，再由 Binder 转发到对应 Destination。

文件位置：`src/main/resources/application-rabbit.yml`

这里定义一个订单创建事件输出通道，Binding 名称为 `orderCreated-out-0`，目标消息地址为 `order.created`。

```yaml
spring:
  cloud:
    stream:
      # 预创建 StreamBridge 使用的输出 Binding
      output-bindings: orderCreated-out-0

      bindings:
        orderCreated-out-0:
          # RabbitMQ 中通常映射为 Exchange，Kafka 中通常映射为 Topic
          destination: order.created
          # 统一使用 JSON 消息格式
          contentType: application/json
          producer:
            # 普通业务消息暂不启用分区；顺序消息章节再单独配置
            partitionCount: 1
```

如果使用 Kafka，只需要保持 Binding 名称不变，切换 Binder 和 Broker 配置即可。

文件位置：`src/main/resources/application-kafka.yml`

```yaml
spring:
  cloud:
    stream:
      defaultBinder: kafka

      output-bindings: orderCreated-out-0

      bindings:
        orderCreated-out-0:
          # Kafka Topic 名称
          destination: order.created
          contentType: application/json

      kafka:
        binder:
          # Kafka Broker 地址
          brokers: localhost:9092
```

推荐在业务代码中把 Binding 名称定义为常量，避免字符串散落在 Controller、Service、定时任务中。

```text
orderCreated-out-0  ->  order.created
业务发送代码           消息中间件目标
```

### 消息发送方式

生产者常见发送方式有两类：业务触发型发送和函数式主动生产。业务触发型发送推荐使用 `StreamBridge`，例如 REST 接口下单后发送订单事件；主动生产可以使用 `Supplier`，例如定时生成消息、读取外部数据源并投递到消息系统。`StreamBridge` 支持发送 POJO 或 `Message`，也支持动态 Destination；如果输出 Binding 不存在，它可以在首次调用时创建并缓存。([Home](https://docs.spring.io/spring-cloud-stream/reference/4.3/spring-cloud-stream/producing-and-consuming-messages.html?utm_source=chatgpt.com))

先定义消息发送所需的目录结构：

```text
src/main/java/io/github/atengk/stream/
├── controller/OrderEventController.java
├── event/OrderCreatedEvent.java
├── producer/OrderEventProducer.java
└── request/OrderSendRequest.java
```

文件位置：`src/main/java/io/github/atengk/stream/event/OrderCreatedEvent.java`

该类定义订单创建事件的消息体，生产者和消费者建议共用同一份事件契约，或者通过独立的 API/SDK 模块维护。

```java
package io.github.atengk.stream.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单创建事件
 *
 * @author Ateng
 * @since 2026-05-05
 */
public record OrderCreatedEvent(
        String messageId,
        String eventType,
        String orderId,
        String userId,
        BigDecimal amount,
        LocalDateTime occurredAt,
        String source
) {
}
```

文件位置：`src/main/java/io/github/atengk/stream/request/OrderSendRequest.java`

该类定义接口接收的请求参数，实际项目中可以换成订单服务内部的业务 DTO。

```java
package io.github.atengk.stream.request;

import java.math.BigDecimal;

/**
 * 订单消息发送请求
 *
 * @author Ateng
 * @since 2026-05-05
 */
public record OrderSendRequest(
        String orderId,
        String userId,
        BigDecimal amount
) {
}
```

文件位置：`src/main/java/io/github/atengk/stream/producer/OrderEventProducer.java`

该类使用 `StreamBridge` 发送订单创建事件，并通过 `MessageBuilder` 设置业务消息头。

```java
package io.github.atengk.stream.producer;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.stream.event.OrderCreatedEvent;
import io.github.atengk.stream.request.OrderSendRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.time.LocalDateTime;

/**
 * 订单事件生产者
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventProducer {

    /**
     * 订单创建事件输出 Binding
     */
    public static final String ORDER_CREATED_OUT = "orderCreated-out-0";

    private final StreamBridge streamBridge;

    /**
     * 发送订单创建事件
     *
     * @param request 订单发送请求
     * @return 消息 ID
     */
    public String sendOrderCreated(OrderSendRequest request) {
        checkRequest(request);

        String messageId = IdUtil.fastSimpleUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(
                messageId,
                "ORDER_CREATED",
                request.orderId(),
                request.userId(),
                request.amount(),
                LocalDateTime.now(),
                "order-service"
        );

        Message<OrderCreatedEvent> message = MessageBuilder.withPayload(event)
                // 消息唯一 ID，消费端可用于幂等处理
                .setHeader("messageId", messageId)
                // 事件类型，便于消费端做路由或日志检索
                .setHeader("eventType", event.eventType())
                // 业务键，通常使用订单号、用户 ID、流水号等
                .setHeader("businessKey", event.orderId())
                // 指定消息内容类型，优先级高于 Binding 默认 contentType
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
                .build();

        boolean sent = streamBridge.send(ORDER_CREATED_OUT, message);
        if (!sent) {
            log.error("订单创建消息发送失败，messageId={}，orderId={}", messageId, event.orderId());
            throw new IllegalStateException("订单创建消息发送失败");
        }

        log.info("订单创建消息发送成功，messageId={}，orderId={}，payload={}",
                messageId, event.orderId(), JSONUtil.toJsonStr(event));
        return messageId;
    }

    /**
     * 校验订单发送请求
     *
     * @param request 订单发送请求
     */
    private void checkRequest(OrderSendRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("订单消息发送请求不能为空");
        }
        if (StrUtil.isBlank(request.orderId())) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        if (StrUtil.isBlank(request.userId())) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        if (request.amount() == null) {
            throw new IllegalArgumentException("订单金额不能为空");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/stream/controller/OrderEventController.java`

该接口用于本地验证生产者发送流程，收到 HTTP 请求后发送一条订单创建消息。

```java
package io.github.atengk.stream.controller;

import cn.hutool.core.lang.Dict;
import io.github.atengk.stream.producer.OrderEventProducer;
import io.github.atengk.stream.request.OrderSendRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 订单事件接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@RestController
@RequestMapping("/api/messages/orders")
@RequiredArgsConstructor
public class OrderEventController {

    private final OrderEventProducer orderEventProducer;

    /**
     * 发送订单创建事件
     *
     * @param request 订单发送请求
     * @return 发送结果
     */
    @PostMapping("/created")
    public ResponseEntity<Dict> sendOrderCreated(@RequestBody OrderSendRequest request) {
        String messageId = orderEventProducer.sendOrderCreated(request);
        Dict body = Dict.create()
                .set("messageId", messageId)
                .set("sent", true);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
    }
}
```

调用示例：

```bash
curl -X POST "http://localhost:18080/api/messages/orders/created" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORDER_10001",
    "userId": "USER_90001",
    "amount": 199.90
  }'
```

响应示例：

```json
{
  "messageId": "9f4b5f5e1f4b43c1951d6a5a5a6f9c9b",
  "sent": true
}
```

如果是定时或主动生产消息，可以使用 `Supplier`。Spring Cloud Stream 的 `Supplier` 默认由框架轮询触发，官方文档说明默认轮询机制会定期调用 `Supplier`，默认间隔为 1 秒，因此生产环境必须显式控制触发频率或谨慎启用。([Home](https://docs.spring.io/spring-cloud-stream/reference/4.3/spring-cloud-stream/producing-and-consuming-messages.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/stream/producer/DemoOrderSupplierConfig.java`

该配置类演示通过 `Supplier` 主动生产消息，只建议用于本地演示或受控的定时消息场景。

```java
package io.github.atengk.stream.producer;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.stream.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * 演示订单消息供应者配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Configuration
public class DemoOrderSupplierConfig {

    /**
     * 主动生产订单创建事件
     *
     * @return 订单创建事件供应者
     */
    @Bean
    public Supplier<Message<OrderCreatedEvent>> demoOrderCreatedSupplier() {
        return () -> {
            String messageId = IdUtil.fastSimpleUUID();
            OrderCreatedEvent event = new OrderCreatedEvent(
                    messageId,
                    "ORDER_CREATED",
                    "DEMO_ORDER_" + System.currentTimeMillis(),
                    "DEMO_USER",
                    BigDecimal.valueOf(99.90),
                    LocalDateTime.now(),
                    "demo-supplier"
            );

            log.info("Supplier 生产订单创建消息，messageId={}，orderId={}", messageId, event.orderId());

            return MessageBuilder.withPayload(event)
                    .setHeader("messageId", messageId)
                    .setHeader("eventType", event.eventType())
                    .setHeader("businessKey", event.orderId())
                    .build();
        };
    }
}
```

对应配置如下：

```yaml
spring:
  cloud:
    function:
      # 只有需要启用 Supplier 自动生产时才配置该函数
      definition: demoOrderCreatedSupplier

    stream:
      bindings:
        demoOrderCreatedSupplier-out-0:
          destination: order.created
          contentType: application/json
```

### 消息结构设计

消息结构建议分为两层：业务消息体 `payload` 和消息头 `headers`。`payload` 只承载业务事件数据，`headers` 承载消息 ID、事件类型、业务键、追踪 ID、来源服务等技术或路由信息。Spring Cloud Stream 中消息的标准数据结构是 Spring `Message`，框架会根据 `contentType` 和目标函数参数类型执行消息转换。([Home](https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/spring-cloud-stream.html?utm_source=chatgpt.com))

推荐结构如下：

| 位置    | 字段          | 示例                  | 说明                     |
| ------- | ------------- | --------------------- | ------------------------ |
| Header  | `messageId`   | `9f4b...`             | 消息唯一 ID，用于幂等    |
| Header  | `eventType`   | `ORDER_CREATED`       | 事件类型                 |
| Header  | `businessKey` | `ORDER_10001`         | 业务主键，便于检索和分区 |
| Header  | `traceId`     | 链路追踪 ID           | 可与日志链路集成         |
| Payload | `orderId`     | `ORDER_10001`         | 订单号                   |
| Payload | `userId`      | `USER_90001`          | 用户 ID                  |
| Payload | `amount`      | `199.90`              | 订单金额                 |
| Payload | `occurredAt`  | `2026-05-05 10:00:00` | 事件发生时间             |
| Payload | `source`      | `order-service`       | 事件来源服务             |

消息设计建议遵守以下原则：

第一，`messageId` 必须由生产者生成，并在整个消息生命周期中保持不变。消费端幂等处理不应临时拼接随机值，否则无法识别重复投递。

第二，`eventType` 应使用稳定枚举值，例如 `ORDER_CREATED`、`ORDER_PAID`、`ORDER_CANCELED`。不要直接使用中文描述作为事件类型。

第三，`payload` 中的字段要面向事件事实，而不是接口请求。例如“订单已创建事件”应该描述订单已创建后的核心数据，而不是复用下单接口的所有请求参数。

第四，金额字段使用 `BigDecimal`，时间字段使用 `LocalDateTime`、`OffsetDateTime` 或毫秒时间戳，并在团队内统一序列化标准。

第五，不建议在消息体中放大对象，例如完整用户资料、完整商品详情、完整订单明细。消息应只携带消费端真正需要的数据，或者携带业务 ID 后由消费端查询详情。

### 发送结果处理

`StreamBridge.send(...)` 返回 `boolean`，用于表示消息是否成功提交到输出 Binding。它不等价于“消息一定已经被消费者处理成功”。对于业务代码，应至少处理三类结果：返回 `false`、发送过程抛异常、发送成功但后续消费失败。Spring Cloud Stream 文档也说明，`StreamBridge` 默认使用调用方线程发送；如需异步发送，可调用 `setAsync(true)`，但异步发送会带来链路上下文传播问题，需要额外处理。([Home](https://docs.spring.io/spring-cloud-stream/reference/4.3/spring-cloud-stream/producing-and-consuming-messages.html?utm_source=chatgpt.com))

推荐处理方式如下：

| 场景                | 处理方式                                                     |
| ------------------- | ------------------------------------------------------------ |
| `send` 返回 `true`  | 记录业务日志，返回消息 ID                                    |
| `send` 返回 `false` | 记录错误日志，抛出业务异常                                   |
| `send` 抛出异常     | 捕获后记录异常日志，再抛出业务异常                           |
| 消费端失败          | 依赖消费端重试、死信队列、补偿任务处理                       |
| 需要严格可靠投递    | 结合本地消息表、事务外盒、补偿扫描，而不是只依赖 `send` 返回值 |

可以在生产者外层增加统一封装：

```java
try {
    String messageId = orderEventProducer.sendOrderCreated(request);
    log.info("订单事件提交完成，messageId={}", messageId);
} catch (Exception e) {
    log.error("订单事件提交异常，orderId={}", request.orderId(), e);
    throw new IllegalStateException("订单事件提交失败，请稍后重试");
}
```

生产环境中，如果“订单创建”和“消息发送”必须保持最终一致，推荐使用本地消息表或事务外盒模式。业务事务先落订单表和消息表，再由补偿任务或消息发布器扫描待发送消息并发送到 MQ，发送成功后更新消息状态。这样可以避免业务数据库提交成功但消息发送失败造成事件丢失。

## 消息消费者开发

消息消费者负责从输入 Binding 接收消息，完成反序列化、业务校验、幂等判断、业务处理和异常传播。消费端不要吞掉关键异常；如果希望触发框架重试或死信队列，应让异常继续向外抛出。Spring Cloud Stream 默认在消息处理函数抛出异常后由 Binder 处理重试，默认会尝试 3 次，重试失败后再根据错误处理配置决定丢弃、重新入队或进入 DLQ。([Home](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/overview-error-handling.html?utm_source=chatgpt.com))

### 输入通道定义

输入通道用于绑定消费者函数和消息目标。函数式模型下，`Consumer<T>` 的输入 Binding 默认命名为 `<functionName>-in-0`。例如消费者函数名为 `orderCreatedConsumer`，则输入 Binding 名称为 `orderCreatedConsumer-in-0`。([Home](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/functional-binding-names.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application-rabbit.yml`

下面的配置定义订单创建事件消费者，启用固定消费组、消费重试和 RabbitMQ 死信队列。

```yaml
spring:
  cloud:
    function:
      # 启用订单创建事件消费者
      definition: orderCreatedConsumer

    stream:
      bindings:
        orderCreatedConsumer-in-0:
          # 监听订单创建事件
          destination: order.created
          # 固定消费组，生产环境必须配置
          group: order-service
          # 使用 JSON 反序列化消息体
          contentType: application/json
          consumer:
            # 消费失败最大尝试次数，包含首次消费
            maxAttempts: 3
            # 初始重试间隔，单位毫秒
            backOffInitialInterval: 1000
            # 最大重试间隔，单位毫秒
            backOffMaxInterval: 10000
            # 重试间隔倍率
            backOffMultiplier: 2.0
            # 单实例消费并发数
            concurrency: 2

      rabbit:
        bindings:
          orderCreatedConsumer-in-0:
            consumer:
              # 自动创建死信队列
              autoBindDlq: true
              # 失败消息重新发布到 DLQ，并附加异常堆栈等头信息
              republishToDlq: true
              # 不建议默认重新入队，避免异常消息无限循环
              requeueRejected: false
```

RabbitMQ Binder 支持 `autoBindDlq` 创建死信队列；使用 DLQ 时至少应配置消费组 `group`，否则无法形成稳定的死信队列命名。RabbitMQ Binder 文档也提示，`requeueRejected=true` 可能导致消息持续重新入队和重复投递，除非明确是短暂性故障，否则不建议这样配置。([Home](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/overview-error-handling.html?utm_source=chatgpt.com))

Kafka 输入通道配置示例：

```yaml
spring:
  cloud:
    function:
      definition: orderCreatedConsumer

    stream:
      bindings:
        orderCreatedConsumer-in-0:
          destination: order.created
          group: order-service
          contentType: application/json
          consumer:
            maxAttempts: 3
            concurrency: 1

      kafka:
        binder:
          brokers: localhost:9092
```

Kafka 的 `concurrency` 不应盲目大于 Topic 分区数。即使配置更高并发，也无法超过分区分配带来的实际并行度。

### 消息监听实现

消费者监听实现推荐使用 `Consumer<Message<T>>`。相比 `Consumer<T>`，它可以同时获取消息体和消息头，便于记录 `messageId`、`eventType`、`businessKey`，也便于幂等处理和问题排查。

先定义消费者相关目录结构：

```text
src/main/java/io/github/atengk/stream/
├── config/OrderCreatedConsumerConfig.java
├── event/OrderCreatedEvent.java
└── service/
    ├── OrderEventConsumeService.java
    ├── MessageIdempotentService.java
    └── impl/
        ├── OrderEventConsumeServiceImpl.java
        └── RedisMessageIdempotentService.java
```

文件位置：`src/main/java/io/github/atengk/stream/config/OrderCreatedConsumerConfig.java`

该配置类声明 `orderCreatedConsumer` 函数，函数名需要与 `spring.cloud.function.definition` 和 Binding 名称保持一致。

```java
package io.github.atengk.stream.config;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.stream.event.OrderCreatedEvent;
import io.github.atengk.stream.service.OrderEventConsumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

/**
 * 订单创建事件消费者配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class OrderCreatedConsumerConfig {

    private final OrderEventConsumeService orderEventConsumeService;

    /**
     * 订单创建事件消费者
     *
     * @return 消费函数
     */
    @Bean
    public Consumer<Message<OrderCreatedEvent>> orderCreatedConsumer() {
        return message -> {
            OrderCreatedEvent event = message.getPayload();
            if (event == null) {
                throw new IllegalArgumentException("订单创建事件消息体不能为空");
            }
            if (StrUtil.isBlank(event.messageId())) {
                throw new IllegalArgumentException("订单创建事件 messageId 不能为空");
            }

            log.info("收到订单创建消息，messageId={}，eventType={}，businessKey={}",
                    event.messageId(),
                    message.getHeaders().get("eventType"),
                    message.getHeaders().get("businessKey"));

            orderEventConsumeService.consume(event, message.getHeaders());
        };
    }
}
```

文件位置：`src/main/java/io/github/atengk/stream/service/OrderEventConsumeService.java`

该接口用于隔离消息监听和业务处理，避免把复杂业务直接写在 `Consumer` 函数中。

```java
package io.github.atengk.stream.service;

import io.github.atengk.stream.event.OrderCreatedEvent;
import org.springframework.messaging.MessageHeaders;

/**
 * 订单事件消费服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface OrderEventConsumeService {

    /**
     * 消费订单创建事件
     *
     * @param event   订单创建事件
     * @param headers 消息头
     */
    void consume(OrderCreatedEvent event, MessageHeaders headers);
}
```

文件位置：`src/main/java/io/github/atengk/stream/service/impl/OrderEventConsumeServiceImpl.java`

该实现类完成业务校验、幂等判断和业务处理。示例中用日志代替真实业务落库，实际项目中应在这里调用订单、库存、积分、通知或审计相关服务。

```java
package io.github.atengk.stream.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.stream.event.OrderCreatedEvent;
import io.github.atengk.stream.service.MessageIdempotentService;
import io.github.atengk.stream.service.OrderEventConsumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 订单事件消费服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventConsumeServiceImpl implements OrderEventConsumeService {

    private final MessageIdempotentService messageIdempotentService;

    /**
     * 消费订单创建事件
     *
     * @param event   订单创建事件
     * @param headers 消息头
     */
    @Override
    public void consume(OrderCreatedEvent event, MessageHeaders headers) {
        checkEvent(event);

        String messageId = event.messageId();
        boolean locked = messageIdempotentService.tryLock(messageId, Duration.ofHours(24));
        if (!locked) {
            log.warn("订单创建消息重复消费，直接跳过，messageId={}，orderId={}", messageId, event.orderId());
            return;
        }

        try {
            // TODO 这里编写真实业务逻辑，例如更新订单状态、写入审计日志、触发通知等
            log.info("订单创建事件处理完成，messageId={}，orderId={}，payload={}",
                    messageId, event.orderId(), JSONUtil.toJsonStr(event));

            messageIdempotentService.markConsumed(messageId, Duration.ofDays(7));
        } catch (RuntimeException e) {
            // 释放处理中标记，让框架重试时可以再次处理该消息
            messageIdempotentService.release(messageId);
            log.error("订单创建事件处理失败，messageId={}，orderId={}", messageId, event.orderId(), e);
            throw e;
        }
    }

    /**
     * 校验订单创建事件
     *
     * @param event 订单创建事件
     */
    private void checkEvent(OrderCreatedEvent event) {
        if (StrUtil.isBlank(event.messageId())) {
            throw new IllegalArgumentException("消息 ID 不能为空");
        }
        if (StrUtil.isBlank(event.orderId())) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        if (StrUtil.isBlank(event.userId())) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        if (event.amount() == null) {
            throw new IllegalArgumentException("订单金额不能为空");
        }
    }
}
```

### 消息反序列化处理

Spring Cloud Stream 的反序列化由 `MessageConverter` 完成。框架会根据消息头、Binding 配置或默认值确定 `contentType`；优先级依次是消息 Header、Binding 配置、默认值。如果未显式配置，默认 `contentType` 是 `application/json`。([Home](https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/spring-cloud-stream.html?utm_source=chatgpt.com))

常用写法如下：

```java
@Bean
public Consumer<OrderCreatedEvent> orderCreatedConsumer() {
    return event -> {
        // 只能拿到 payload，拿不到完整 headers
    };
}
```

如果需要消息头，推荐使用：

```java
@Bean
public Consumer<Message<OrderCreatedEvent>> orderCreatedConsumer() {
    return message -> {
        OrderCreatedEvent event = message.getPayload();
        Object messageId = message.getHeaders().get("messageId");
    };
}
```

Spring Cloud Stream 内置的 `JsonMessageConverter` 支持在 `contentType=application/json` 时将消息体转换为 POJO；如果找不到合适的转换器，框架会抛出异常，此时应检查 `contentType`、目标参数类型、JSON 字段结构和时间格式。([Home](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/provided-messageconverters.html?utm_source=chatgpt.com))

建议在配置中显式声明 JSON：

```yaml
spring:
  cloud:
    stream:
      default:
        # 所有 Binding 默认使用 JSON
        contentType: application/json

      bindings:
        orderCreatedConsumer-in-0:
          destination: order.created
          group: order-service
          # 当前消费者明确使用 JSON
          contentType: application/json
```

常见反序列化问题如下：

| 问题                         | 常见原因                       | 处理方式                           |
| ---------------------------- | ------------------------------ | ---------------------------------- |
| `MessageConversionException` | JSON 与 Java 类型不匹配        | 检查字段名、字段类型、时间格式     |
| 消息体为 `byte[]`            | 未正确设置 `contentType`       | 配置 `application/json` 或手动转换 |
| 时间字段解析失败             | 生产端和消费端时间格式不统一   | 统一 Jackson 时间配置              |
| 金额精度异常                 | 使用 `Double` 承载金额         | 改为 `BigDecimal`                  |
| 拿不到消息头                 | 使用了 `Consumer<T>`           | 改为 `Consumer<Message<T>>`        |
| 字段丢失                     | 生产者和消费者事件类版本不一致 | 维护事件契约版本                   |

如果确实需要手动处理原始消息，可以使用 `Consumer<Message<byte[]>>`，然后通过 Hutool 或 Jackson 手动反序列化。

```java
@Bean
public Consumer<Message<byte[]>> rawOrderCreatedConsumer() {
    return message -> {
        String json = new String(message.getPayload());
        OrderCreatedEvent event = cn.hutool.json.JSONUtil.toBean(json, OrderCreatedEvent.class);
        // 处理 event
    };
}
```

正常业务项目不建议优先使用原始 `byte[]`，除非需要兼容非 Spring Cloud Stream 生产者、历史消息格式或特殊协议。

### 幂等消费处理

消息系统通常只能保证“至少一次投递”或在特定条件下实现更强语义，业务消费者必须具备幂等能力。重复消费可能来自消费端处理超时、应用重启、Broker 重投、网络抖动、手动补偿、死信重放等场景。消费端幂等不应依赖“框架不会重复投递”的假设。

生产级幂等推荐使用 Redis `SETNX`、数据库唯一索引或业务状态机。这里给出 Redis 方案：第一次消费时写入 `PROCESSING` 标记，处理成功后改为 `CONSUMED`，处理失败释放标记，让框架重试时可以再次执行。

需要增加 Redis 依赖。

文件位置：`pom.xml`

```xml
<!-- Redis 支持，用于消息幂等、消费状态缓存等场景 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

Redis 连接配置示例：

```yaml
spring:
  data:
    redis:
      # Redis 服务地址
      host: localhost
      # Redis 端口
      port: 6379
      # 本地开发可不配置密码，生产环境必须使用安全配置
      password:
      # Redis 数据库
      database: 0
      # 连接超时时间
      timeout: 5s
```

文件位置：`src/main/java/io/github/atengk/stream/service/MessageIdempotentService.java`

该接口定义消息幂等处理能力。

```java
package io.github.atengk.stream.service;

import java.time.Duration;

/**
 * 消息幂等服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface MessageIdempotentService {

    /**
     * 尝试锁定消息
     *
     * @param messageId 消息 ID
     * @param ttl       锁定时间
     * @return 是否锁定成功
     */
    boolean tryLock(String messageId, Duration ttl);

    /**
     * 标记消息已消费
     *
     * @param messageId 消息 ID
     * @param ttl       保留时间
     */
    void markConsumed(String messageId, Duration ttl);

    /**
     * 释放消息锁定
     *
     * @param messageId 消息 ID
     */
    void release(String messageId);
}
```

文件位置：`src/main/java/io/github/atengk/stream/service/impl/RedisMessageIdempotentService.java`

该实现类使用 Redis `setIfAbsent` 完成幂等锁定。

```java
package io.github.atengk.stream.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.stream.service.MessageIdempotentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis 消息幂等服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisMessageIdempotentService implements MessageIdempotentService {

    private static final String KEY_PREFIX = "stream:message:idempotent:";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_CONSUMED = "CONSUMED";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试锁定消息
     *
     * @param messageId 消息 ID
     * @param ttl       锁定时间
     * @return 是否锁定成功
     */
    @Override
    public boolean tryLock(String messageId, Duration ttl) {
        checkMessageId(messageId);

        String key = buildKey(messageId);
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, STATUS_PROCESSING, ttl);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 标记消息已消费
     *
     * @param messageId 消息 ID
     * @param ttl       保留时间
     */
    @Override
    public void markConsumed(String messageId, Duration ttl) {
        checkMessageId(messageId);

        String key = buildKey(messageId);
        stringRedisTemplate.opsForValue().set(key, STATUS_CONSUMED, ttl);
        log.info("消息已标记为消费完成，messageId={}", messageId);
    }

    /**
     * 释放消息锁定
     *
     * @param messageId 消息 ID
     */
    @Override
    public void release(String messageId) {
        checkMessageId(messageId);

        String key = buildKey(messageId);
        stringRedisTemplate.delete(key);
        log.warn("消息处理失败，已释放幂等锁，messageId={}", messageId);
    }

    /**
     * 构建 Redis Key
     *
     * @param messageId 消息 ID
     * @return Redis Key
     */
    private String buildKey(String messageId) {
        return KEY_PREFIX + messageId;
    }

    /**
     * 校验消息 ID
     *
     * @param messageId 消息 ID
     */
    private void checkMessageId(String messageId) {
        if (StrUtil.isBlank(messageId)) {
            throw new IllegalArgumentException("消息 ID 不能为空");
        }
    }
}
```

幂等处理的关键点是异常传播。如果业务处理失败，不要吞掉异常，否则框架会认为消息已成功消费，重试和死信队列都不会生效。消费失败时释放 `PROCESSING` 标记并重新抛出异常；消费成功后写入 `CONSUMED` 标记并设置较长 TTL，避免后续重复投递再次执行业务逻辑。

生产环境还可以使用数据库唯一索引增强幂等，例如创建 `message_consume_log` 表，并对 `message_id` 建唯一索引。Redis 适合高性能快速去重，数据库适合强审计和长期追溯。对于订单、支付、库存这类核心链路，建议 Redis 幂等和业务状态机同时使用：Redis 拦截重复消息，业务状态机保证重复执行也不会造成错误状态流转。



## 函数式编程模型

Spring Cloud Stream 4.x 推荐使用函数式编程模型开发消息生产者、处理器和消费者。函数式模型主要围绕 Java 标准函数接口展开：`Supplier` 用于生产消息，`Function` 用于接收并转换消息，`Consumer` 用于最终消费消息。函数式 Binding 默认命名规则为：输入端 `<functionName>-in-<index>`，输出端 `<functionName>-out-<index>`，普通单输入或单输出函数的 `index` 通常为 `0`。([Spring Enterprise Docs](https://docs.enterprise.spring.io/spring-cloud-stream/reference/spring-cloud-stream/functional-binding-names.html?utm_source=chatgpt.com))

### Supplier 使用

`Supplier` 表示无输入、有输出的消息生产函数，适合定时生成消息、轮询外部数据源、扫描本地消息表后投递 MQ 等场景。Spring Cloud Stream 会通过轮询机制调用 `Supplier`，默认轮询间隔是 1000ms，每次轮询默认最多产生 1 条消息；可以通过 `spring.integration.poller.*` 或单个 Binding 的 `producer.poller.*` 配置调整。([Home](https://docs.spring.io/spring-cloud-stream/reference/4.3/spring-cloud-stream/producing-and-consuming-messages.html?utm_source=chatgpt.com))

`Supplier` 不适合直接处理 HTTP 请求触发的消息发送。HTTP、业务服务、定时任务中主动发送消息，优先使用 `StreamBridge`；`Supplier` 更适合框架驱动的周期性消息生产。

文件位置：`src/main/resources/application.yml`

下面配置启用一个订单事件供应函数，并设置单独的轮询间隔。

```yaml
spring:
  cloud:
    function:
      # 启用 Supplier 函数
      definition: orderCreatedSupplier

    stream:
      bindings:
        orderCreatedSupplier-out-0:
          # 订单创建事件目标
          destination: order.created
          # 使用 JSON 消息格式
          contentType: application/json
          producer:
            poller:
              # 当前 Supplier 每 5 秒触发一次
              fixed-delay: 5000
              # 每次轮询最多生产 1 条消息
              max-messages-per-poll: 1
```

文件位置：`src/main/java/io/github/atengk/stream/function/OrderSupplierConfig.java`

下面的配置类定义一个 `Supplier<Message<OrderCreatedEvent>>`，用于周期性生产演示订单消息。

```java
package io.github.atengk.stream.function;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.stream.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * 订单消息供应函数配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Configuration
public class OrderSupplierConfig {

    /**
     * 周期性生产订单创建事件
     *
     * @return 订单创建事件供应函数
     */
    @Bean
    public Supplier<Message<OrderCreatedEvent>> orderCreatedSupplier() {
        return () -> {
            String messageId = IdUtil.fastSimpleUUID();
            String orderId = "SUPPLIER_ORDER_" + System.currentTimeMillis();

            OrderCreatedEvent event = new OrderCreatedEvent(
                    messageId,
                    "ORDER_CREATED",
                    orderId,
                    "SUPPLIER_USER",
                    BigDecimal.valueOf(99.90),
                    LocalDateTime.now(),
                    "order-supplier"
            );

            log.info("Supplier 生产订单创建事件，messageId={}，orderId={}", messageId, orderId);

            return MessageBuilder.withPayload(event)
                    .setHeader("messageId", messageId)
                    .setHeader("eventType", event.eventType())
                    .setHeader("businessKey", event.orderId())
                    .build();
        };
    }
}
```

`Supplier` 在生产环境中要谨慎启用。如果它扫描数据库消息表，必须保证每次扫描具有状态控制，例如 `WAITING`、`SENDING`、`SENT`、`FAILED`，否则应用重启或多实例部署时容易重复发送。

### Function 使用

`Function` 表示有输入、有输出的消息处理函数，适合消息转换、消息清洗、事件拆分前的加工、跨 Topic 转发等场景。它既有输入 Binding，也有输出 Binding，因此默认会生成 `<functionName>-in-0` 和 `<functionName>-out-0` 两个绑定名称。([Spring Enterprise Docs](https://docs.enterprise.spring.io/spring-cloud-stream/reference/spring-cloud-stream/functional-binding-names.html?utm_source=chatgpt.com))

典型场景是：消费订单创建事件，转换成通知事件，再发送到通知消息目标。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  cloud:
    function:
      # 启用订单通知转换函数
      definition: orderNotifyFunction

    stream:
      bindings:
        orderNotifyFunction-in-0:
          # 输入：订单创建事件
          destination: order.created
          # 当前函数所在服务的消费组
          group: order-notify-transform-service
          contentType: application/json

        orderNotifyFunction-out-0:
          # 输出：订单通知事件
          destination: order.notify
          contentType: application/json
```

文件位置：`src/main/java/io/github/atengk/stream/event/OrderNotifyEvent.java`

该类定义订单通知事件。

```java
package io.github.atengk.stream.event;

import java.time.LocalDateTime;

/**
 * 订单通知事件
 *
 * @author Ateng
 * @since 2026-05-05
 */
public record OrderNotifyEvent(
        String messageId,
        String eventType,
        String orderId,
        String userId,
        String notifyType,
        String content,
        LocalDateTime occurredAt,
        String source
) {
}
```

文件位置：`src/main/java/io/github/atengk/stream/function/OrderNotifyFunctionConfig.java`

下面的配置类将订单创建事件转换为订单通知事件。

```java
package io.github.atengk.stream.function;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.stream.event.OrderCreatedEvent;
import io.github.atengk.stream.event.OrderNotifyEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.time.LocalDateTime;
import java.util.function.Function;

/**
 * 订单通知转换函数配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Configuration
public class OrderNotifyFunctionConfig {

    /**
     * 将订单创建事件转换为订单通知事件
     *
     * @return 订单通知转换函数
     */
    @Bean
    public Function<Message<OrderCreatedEvent>, Message<OrderNotifyEvent>> orderNotifyFunction() {
        return message -> {
            OrderCreatedEvent orderEvent = message.getPayload();
            if (orderEvent == null || StrUtil.isBlank(orderEvent.orderId())) {
                throw new IllegalArgumentException("订单创建事件无效，无法转换通知事件");
            }

            String notifyMessageId = IdUtil.fastSimpleUUID();
            OrderNotifyEvent notifyEvent = new OrderNotifyEvent(
                    notifyMessageId,
                    "ORDER_NOTIFY",
                    orderEvent.orderId(),
                    orderEvent.userId(),
                    "SMS",
                    StrUtil.format("订单 {} 已创建成功", orderEvent.orderId()),
                    LocalDateTime.now(),
                    "order-notify-function"
            );

            log.info("订单事件转换为通知事件，sourceMessageId={}，notifyMessageId={}，orderId={}",
                    orderEvent.messageId(), notifyMessageId, orderEvent.orderId());

            return MessageBuilder.withPayload(notifyEvent)
                    .setHeader("messageId", notifyMessageId)
                    .setHeader("eventType", notifyEvent.eventType())
                    .setHeader("businessKey", notifyEvent.orderId())
                    .setHeader("sourceMessageId", orderEvent.messageId())
                    .build();
        };
    }
}
```

`Function` 中不要直接吞掉异常。如果转换失败，应抛出异常，让框架触发重试、错误通道或死信队列。对于业务校验失败且明确不可重试的消息，可以在异常处理章节中使用专门的错误处理策略。

### Consumer 使用

`Consumer` 表示有输入、无输出的消息消费者，适合最终业务处理，例如落库、调用内部服务、更新缓存、发送短信、写审计日志等。`Consumer<T>` 只能拿到消息体；`Consumer<Message<T>>` 可以同时获取消息体和消息头，生产项目更推荐后者。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  cloud:
    function:
      # 启用订单创建事件消费者
      definition: orderCreatedConsumer

    stream:
      bindings:
        orderCreatedConsumer-in-0:
          # 消费订单创建事件
          destination: order.created
          # 固定消费组，生产环境必须配置
          group: order-service
          contentType: application/json
          consumer:
            # 消费失败最大尝试次数，包含首次消费
            maxAttempts: 3
            # 单实例消费并发数
            concurrency: 2
```

文件位置：`src/main/java/io/github/atengk/stream/function/OrderConsumerConfig.java`

下面的配置类声明订单创建事件消费者，实际业务处理委托给 Service。

```java
package io.github.atengk.stream.function;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.stream.event.OrderCreatedEvent;
import io.github.atengk.stream.service.OrderEventConsumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

/**
 * 订单消息消费函数配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class OrderConsumerConfig {

    private final OrderEventConsumeService orderEventConsumeService;

    /**
     * 消费订单创建事件
     *
     * @return 订单创建事件消费函数
     */
    @Bean
    public Consumer<Message<OrderCreatedEvent>> orderCreatedConsumer() {
        return message -> {
            OrderCreatedEvent event = message.getPayload();
            if (event == null) {
                throw new IllegalArgumentException("订单创建事件不能为空");
            }
            if (StrUtil.isBlank(event.messageId())) {
                throw new IllegalArgumentException("订单创建事件 messageId 不能为空");
            }

            log.info("Consumer 收到订单创建事件，messageId={}，orderId={}",
                    event.messageId(), event.orderId());

            orderEventConsumeService.consume(event, message.getHeaders());
        };
    }
}
```

`Consumer` 的重点不是“收到消息”，而是“正确处理失败”。消费端业务失败后应抛出异常，不要只记录日志后返回，否则框架会认为消息处理成功。是否进入重试、死信队列或错误通道，取决于后续异常处理配置。

### 多函数绑定配置

一个 Spring Boot 应用可以同时声明多个函数 Bean，但生产项目中不建议在同一个服务里塞入过多无关函数。推荐同一服务内只保留同一业务边界内的函数，例如订单服务可以同时包含订单创建消费、订单支付消费、订单取消消费；但不建议同时包含订单、库存、通知、审计等多个领域的函数。

多个函数通过 `spring.cloud.function.definition` 使用英文分号分隔。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  cloud:
    function:
      # 同时启用多个独立函数
      definition: orderCreatedConsumer;orderPaidConsumer;orderNotifyFunction

    stream:
      bindings:
        orderCreatedConsumer-in-0:
          # 消费订单创建事件
          destination: order.created
          group: order-service
          contentType: application/json

        orderPaidConsumer-in-0:
          # 消费订单支付事件
          destination: order.paid
          group: order-service
          contentType: application/json

        orderNotifyFunction-in-0:
          # 订单通知转换函数输入
          destination: order.created
          group: order-notify-transform-service
          contentType: application/json

        orderNotifyFunction-out-0:
          # 订单通知转换函数输出
          destination: order.notify
          contentType: application/json
```

也可以使用函数组合，例如 `orderValidateFunction|orderNotifyFunction`，表示先执行订单校验函数，再执行通知转换函数。但函数组合更适合纯转换流水线；涉及复杂业务副作用、数据库事务、外部接口调用时，应谨慎使用组合，避免排查困难。

推荐的多函数组织方式如下：

| 场景             | 推荐方式                       |
| ---------------- | ------------------------------ |
| 多个独立消费者   | 使用 `funcA;funcB;funcC`       |
| 纯数据转换流水线 | 可以使用 `funcA                |
| 涉及数据库写入   | 优先拆成独立 `Consumer`        |
| 涉及外部接口调用 | 优先拆成独立服务或独立消费者   |
| 多个 Binder      | 每个 Binding 显式配置 `binder` |

## 常用消息场景

常用消息场景主要包括普通消息发送、广播消费、分组消费、延迟消息和顺序消息。普通、广播、分组属于基础消息模型；延迟消息依赖具体 Binder 能力；顺序消息通常依赖分区键和消费并发控制。Spring Cloud Stream 分区机制要求同时配置生产端和消费端，生产端需要配置 `partitionKeyExpression` 或 `partitionKeyExtractorName`，同时配置 `partitionCount`。([Home](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/overview-partitioning.html?utm_source=chatgpt.com))

### 普通消息发送

普通消息发送是最常见的场景：业务接口或业务服务通过 `StreamBridge` 向指定 Binding 发送消息，消费者按固定消费组接收并处理。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  cloud:
    stream:
      # 预创建普通订单事件输出 Binding
      output-bindings: orderCreated-out-0

      bindings:
        orderCreated-out-0:
          # 生产者发送到该目标
          destination: order.created
          contentType: application/json

        orderCreatedConsumer-in-0:
          # 消费者监听同一个目标
          destination: order.created
          # 固定消费组
          group: order-service
          contentType: application/json
```

普通消息发送代码通常使用上一章的 `OrderEventProducer`。调用接口后，可以在 RabbitMQ 管理控制台中观察 Exchange、Queue、Binding 是否创建成功，也可以观察消费者日志是否输出：

```text
订单创建消息发送成功，messageId=xxx，orderId=ORDER_10001
Consumer 收到订单创建事件，messageId=xxx，orderId=ORDER_10001
订单创建事件处理完成，messageId=xxx，orderId=ORDER_10001
```

普通消息适合大多数业务事件，例如订单创建、支付成功、用户注册、文件上传完成、报表生成完成等。

### 广播消费

广播消费表示同一条消息被多个业务服务分别消费。实现方式是：多个消费者监听同一个 `destination`，但配置不同的 `group`。Spring Cloud Stream 的消费组模型中，不同消费组会各自收到消息，同组内实例竞争消费。([Home](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/overview-partitioning.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  cloud:
    function:
      # 启用三个消费者函数
      definition: orderStatusConsumer;orderNotifyConsumer;orderAuditConsumer

    stream:
      bindings:
        orderStatusConsumer-in-0:
          # 三个消费者监听同一个订单创建事件
          destination: order.created
          # 订单状态处理消费组
          group: order-status-service
          contentType: application/json

        orderNotifyConsumer-in-0:
          destination: order.created
          # 通知服务消费组
          group: order-notify-service
          contentType: application/json

        orderAuditConsumer-in-0:
          destination: order.created
          # 审计服务消费组
          group: order-audit-service
          contentType: application/json
```

广播消费的消息流如下：

```text
order.created
  ├── order-status-service      处理订单状态
  ├── order-notify-service      发送通知
  └── order-audit-service       写审计日志
```

广播消费要注意两点。第一，不同消费组之间互不影响，一个消费组失败不会阻塞其他消费组。第二，每个消费组都要独立配置异常处理、重试和死信队列，否则某个下游服务异常时仍可能产生消息堆积或丢失。

### 分组消费

分组消费表示同一个消费组内多个实例共同消费同一个消息目标。它解决的是服务横向扩容问题，而不是广播问题。

例如订单服务部署 3 个实例，它们都配置：

```yaml
spring:
  cloud:
    stream:
      bindings:
        orderCreatedConsumer-in-0:
          destination: order.created
          # 三个实例使用同一个 group
          group: order-service
          contentType: application/json
```

三个实例共同组成 `order-service` 消费组。消息到达后，同组内通常只有一个实例处理某一条消息。这个模型适合提高吞吐量，也适合无状态业务服务横向扩容。

分组消费部署示例：

```text
order.created
  └── group: order-service
      ├── order-service-1
      ├── order-service-2
      └── order-service-3
```

分组消费配置建议如下：

| 配置项        | 建议                                     |
| ------------- | ---------------------------------------- |
| `group`       | 生产环境必须固定                         |
| `concurrency` | 根据 CPU、业务耗时、分区数或队列能力调整 |
| 幂等          | 必须实现                                 |
| 异常处理      | 必须配置重试和死信队列                   |
| 本地调试      | 不要使用生产消费组                       |

在 Kafka 场景下，消费并发受 Topic 分区数影响。如果 Topic 只有 3 个分区，即使部署 10 个消费者实例，同一消费组内也只有最多 3 个实例能同时消费该 Topic 的分区。

### 延迟消息

延迟消息表示消息发送后不立即投递给消费者，而是在指定时间后再投递。常见场景包括订单超时关闭、支付超时检查、优惠券延迟生效、任务延迟执行等。

Spring Cloud Stream 的延迟消息能力依赖具体 Binder。RabbitMQ Binder 支持通过 `delayedExchange` 声明延迟交换机，并通过 `delayExpression` 根据消息内容或 Header 计算延迟时间；该能力要求 RabbitMQ Broker 安装 Delayed Message Exchange 插件。([Home](https://docs.spring.io/spring-cloud-stream/docs/current-SNAPSHOT/reference/html/spring-cloud-stream-binder-rabbit.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application-rabbit.yml`

下面配置一个订单关闭延迟消息输出 Binding。

```yaml
spring:
  cloud:
    stream:
      output-bindings: orderCloseDelay-out-0

      bindings:
        orderCloseDelay-out-0:
          # 延迟订单关闭事件目标
          destination: order.close.delay
          contentType: application/json

        orderCloseDelayConsumer-in-0:
          # 消费延迟投递后的订单关闭事件
          destination: order.close.delay
          group: order-close-service
          contentType: application/json

      rabbit:
        bindings:
          orderCloseDelay-out-0:
            producer:
              # 声明 RabbitMQ 延迟交换机，要求 Broker 安装 delayed message exchange 插件
              delayedExchange: true
              # 从消息 Header 中读取 delayMillis，并转换为 x-delay
              delayExpression: headers['delayMillis']

          orderCloseDelayConsumer-in-0:
            consumer:
              # 消费端也建议声明为延迟交换机，避免生产端和消费端声明类型不一致
              delayedExchange: true
```

文件位置：`src/main/java/io/github/atengk/stream/event/OrderCloseDelayEvent.java`

该类定义订单延迟关闭事件。

```java
package io.github.atengk.stream.event;

import java.time.LocalDateTime;

/**
 * 订单延迟关闭事件
 *
 * @author Ateng
 * @since 2026-05-05
 */
public record OrderCloseDelayEvent(
        String messageId,
        String eventType,
        String orderId,
        LocalDateTime occurredAt,
        String source
) {
}
```

文件位置：`src/main/java/io/github/atengk/stream/producer/OrderDelayProducer.java`

下面的生产者发送延迟关闭消息，并通过 Header 设置延迟毫秒数。

```java
package io.github.atengk.stream.producer;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.stream.event.OrderCloseDelayEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 订单延迟消息生产者
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderDelayProducer {

    private static final String ORDER_CLOSE_DELAY_OUT = "orderCloseDelay-out-0";

    private final StreamBridge streamBridge;

    /**
     * 发送订单延迟关闭消息
     *
     * @param orderId     订单号
     * @param delayMillis 延迟毫秒数
     * @return 消息 ID
     */
    public String sendOrderCloseDelay(String orderId, long delayMillis) {
        if (StrUtil.isBlank(orderId)) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        if (delayMillis <= 0) {
            throw new IllegalArgumentException("延迟时间必须大于 0");
        }

        String messageId = IdUtil.fastSimpleUUID();
        OrderCloseDelayEvent event = new OrderCloseDelayEvent(
                messageId,
                "ORDER_CLOSE_DELAY",
                orderId,
                LocalDateTime.now(),
                "order-service"
        );

        Message<OrderCloseDelayEvent> message = MessageBuilder.withPayload(event)
                .setHeader("messageId", messageId)
                .setHeader("eventType", event.eventType())
                .setHeader("businessKey", event.orderId())
                // RabbitMQ Binder delayExpression 会读取该 Header
                .setHeader("delayMillis", delayMillis)
                .build();

        boolean sent = streamBridge.send(ORDER_CLOSE_DELAY_OUT, message);
        if (!sent) {
            log.error("订单延迟关闭消息发送失败，messageId={}，orderId={}，delayMillis={}",
                    messageId, orderId, delayMillis);
            throw new IllegalStateException("订单延迟关闭消息发送失败");
        }

        log.info("订单延迟关闭消息发送成功，messageId={}，orderId={}，delayMillis={}",
                messageId, orderId, delayMillis);
        return messageId;
    }
}
```

文件位置：`src/main/java/io/github/atengk/stream/function/OrderCloseDelayConsumerConfig.java`

下面的消费者处理延迟投递后的订单关闭事件。

```java
package io.github.atengk.stream.function;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.stream.event.OrderCloseDelayEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

/**
 * 订单延迟关闭消费者配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Configuration
public class OrderCloseDelayConsumerConfig {

    /**
     * 消费订单延迟关闭事件
     *
     * @return 订单延迟关闭消费函数
     */
    @Bean
    public Consumer<Message<OrderCloseDelayEvent>> orderCloseDelayConsumer() {
        return message -> {
            OrderCloseDelayEvent event = message.getPayload();
            if (event == null || StrUtil.isBlank(event.orderId())) {
                throw new IllegalArgumentException("订单延迟关闭事件无效");
            }

            // TODO 查询订单状态，只有未支付或待关闭状态才执行关闭
            log.info("收到订单延迟关闭事件，messageId={}，orderId={}",
                    event.messageId(), event.orderId());
        };
    }
}
```

延迟消息需要注意：RabbitMQ 延迟交换机插件不是普通队列 TTL 的同一种实现；如果目标 Exchange 已经用普通类型创建，再改为延迟交换机可能会出现声明冲突，需要删除旧 Exchange 或使用新的 `destination`。生产环境中，订单超时关闭还应在消费端二次查询订单状态，不能因为收到延迟消息就直接关闭订单。

### 顺序消息

顺序消息表示同一业务键下的消息按发送顺序被同一个消费链路处理。典型场景是同一个订单的状态变更：创建、支付、发货、完成必须按顺序处理。Spring Cloud Stream 的分区机制可用于保证相同分区键的消息进入同一分区，并由同一消费实例处理；Kafka 原生支持 Topic 分区，因此更适合实现顺序消息。([Home](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/overview-partitioning.html?utm_source=chatgpt.com))

顺序消息的关键不是全局顺序，而是业务键维度的局部顺序。例如“同一个订单号有序”是合理目标，“所有订单全局有序”通常会严重牺牲吞吐。

文件位置：`src/main/resources/application-kafka.yml`

下面配置按 `orderId` 分区的顺序消息。

```yaml
spring:
  cloud:
    stream:
      defaultBinder: kafka

      output-bindings: orderStatus-out-0

      bindings:
        orderStatus-out-0:
          # 订单状态事件 Topic
          destination: order.status
          contentType: application/json
          producer:
            # 使用 payload.orderId 作为分区键，相同订单号进入同一分区
            partitionKeyExpression: payload.orderId
            # 生产端声明分区数量，需与 Kafka Topic 分区规划匹配
            partitionCount: 6

        orderStatusConsumer-in-0:
          destination: order.status
          group: order-status-service
          contentType: application/json
          consumer:
            # 消费并发数不要超过 Topic 分区数
            concurrency: 3

      kafka:
        binder:
          brokers: localhost:9092
          # 本地开发可自动创建 Topic；生产环境建议关闭
          autoCreateTopics: true
          # 本地开发允许自动增加分区需谨慎，生产环境建议由运维统一管理
          autoAddPartitions: false
```

文件位置：`src/main/java/io/github/atengk/stream/event/OrderStatusEvent.java`

该类定义订单状态事件，`orderId` 是顺序消息的分区键。

```java
package io.github.atengk.stream.event;

import java.time.LocalDateTime;

/**
 * 订单状态事件
 *
 * @author Ateng
 * @since 2026-05-05
 */
public record OrderStatusEvent(
        String messageId,
        String eventType,
        String orderId,
        String status,
        LocalDateTime occurredAt,
        String source
) {
}
```

文件位置：`src/main/java/io/github/atengk/stream/producer/OrderStatusProducer.java`

下面的生产者发送订单状态消息，相同 `orderId` 会根据配置进入同一分区。

```java
package io.github.atengk.stream.producer;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.stream.event.OrderStatusEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 订单状态消息生产者
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStatusProducer {

    private static final String ORDER_STATUS_OUT = "orderStatus-out-0";

    private final StreamBridge streamBridge;

    /**
     * 发送订单状态事件
     *
     * @param orderId 订单号
     * @param status  订单状态
     * @return 消息 ID
     */
    public String sendOrderStatus(String orderId, String status) {
        if (StrUtil.isBlank(orderId)) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        if (StrUtil.isBlank(status)) {
            throw new IllegalArgumentException("订单状态不能为空");
        }

        String messageId = IdUtil.fastSimpleUUID();
        OrderStatusEvent event = new OrderStatusEvent(
                messageId,
                "ORDER_STATUS_CHANGED",
                orderId,
                status,
                LocalDateTime.now(),
                "order-service"
        );

        Message<OrderStatusEvent> message = MessageBuilder.withPayload(event)
                .setHeader("messageId", messageId)
                .setHeader("eventType", event.eventType())
                .setHeader("businessKey", event.orderId())
                .build();

        boolean sent = streamBridge.send(ORDER_STATUS_OUT, message);
        if (!sent) {
            log.error("订单状态消息发送失败，messageId={}，orderId={}，status={}",
                    messageId, orderId, status);
            throw new IllegalStateException("订单状态消息发送失败");
        }

        log.info("订单状态消息发送成功，messageId={}，orderId={}，status={}",
                messageId, orderId, status);
        return messageId;
    }
}
```

文件位置：`src/main/java/io/github/atengk/stream/function/OrderStatusConsumerConfig.java`

消费者按订单号处理状态消息。顺序消息仍然需要幂等和状态校验，因为重复投递、重试和人工补偿仍可能发生。

```java
package io.github.atengk.stream.function;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.stream.event.OrderStatusEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

/**
 * 订单状态消息消费者配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Configuration
public class OrderStatusConsumerConfig {

    /**
     * 消费订单状态事件
     *
     * @return 订单状态事件消费函数
     */
    @Bean
    public Consumer<Message<OrderStatusEvent>> orderStatusConsumer() {
        return message -> {
            OrderStatusEvent event = message.getPayload();
            if (event == null || StrUtil.isBlank(event.orderId())) {
                throw new IllegalArgumentException("订单状态事件无效");
            }

            // TODO 查询当前订单状态，校验状态流转是否合法，再更新业务状态
            log.info("收到订单状态事件，messageId={}，orderId={}，status={}",
                    event.messageId(), event.orderId(), event.status());
        };
    }
}
```

顺序消息配置建议如下：

| 要点               | 建议                                                  |
| ------------------ | ----------------------------------------------------- |
| 顺序范围           | 优先保证同一业务键有序，不追求全局有序                |
| 分区键             | 使用稳定业务键，例如 `orderId`、`userId`、`accountId` |
| Kafka Topic 分区数 | 提前规划，避免频繁变更                                |
| 消费并发           | 不要超过分区数，且要评估业务处理耗时                  |
| 消费端逻辑         | 必须校验状态机，不能只依赖 MQ 顺序                    |
| 重试策略           | 重试可能阻塞同分区后续消息，需结合死信和补偿设计      |

如果使用 Kafka 原生消息 Key，也可以在发送时设置 `KafkaHeaders.KEY`。但在 Spring Cloud Stream 的通用抽象中，优先使用 `partitionKeyExpression` 更统一；如果项目强依赖 Kafka 原生行为，再使用 Kafka 专属 Header。Kafka Binder 文档也说明，相同业务键进入特定分区有利于严格顺序处理，但 Topic 需要具备足够分区数来支撑消费并发。([Spring Enterprise Docs](https://docs.enterprise.spring.io/spring-cloud-stream/reference/kafka/kafka-binder/partitions.html?utm_source=chatgpt.com))

## 异常处理

异常处理用于控制消息消费失败后的行为，包括是否重试、是否进入死信队列、是否重新入队、是否记录错误上下文。Spring Cloud Stream 中，消费函数抛出异常后，异常会传播给 Binder；框架默认会尝试重试，重试失败后再根据 Binder 能力决定丢弃、重新入队或发送到 DLQ。默认重试次数为 3 次，包含首次消费。([Spring Enterprise Docs](https://docs.enterprise.spring.io/spring-cloud-stream/reference/spring-cloud-stream/overview-error-handling.html?utm_source=chatgpt.com))

### 消费异常处理

消费异常分为可重试异常和不可重试异常。可重试异常通常是网络超时、数据库短暂不可用、第三方接口临时失败；不可重试异常通常是消息格式错误、业务状态非法、必要字段缺失、反序列化失败等。

推荐处理原则如下：

| 异常类型     | 示例                                       | 处理方式                     |
| ------------ | ------------------------------------------ | ---------------------------- |
| 可重试异常   | 数据库连接超时、Redis 短暂不可用、HTTP 502 | 抛出异常，让框架重试         |
| 不可重试异常 | 字段为空、金额非法、状态流转非法           | 记录错误，进入死信或异常表   |
| 幂等重复消息 | `messageId` 已消费                         | 直接跳过，不抛异常           |
| 业务处理中断 | 应用重启、线程中断                         | 抛出异常，依赖 MQ 重投或重试 |
| 未知异常     | 空指针、未覆盖分支                         | 记录完整上下文后抛出         |

消费端不要简单写成下面这种形式：

```java
try {
    // 处理业务
} catch (Exception e) {
    log.error("消费失败", e);
    // 错误示例：吞掉异常后，框架会认为消息消费成功
}
```

推荐写法是：只在本地记录必要上下文，然后继续抛出异常，让 Spring Cloud Stream 的重试、错误通道或死信机制接管。

文件位置：`src/main/java/io/github/atengk/stream/function/OrderCreatedConsumerConfig.java`

该配置类演示消费异常的推荐处理方式：重复消息直接跳过，业务异常继续抛出。

```java
package io.github.atengk.stream.function;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.stream.event.OrderCreatedEvent;
import io.github.atengk.stream.service.OrderEventConsumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

/**
 * 订单创建事件消费者配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class OrderCreatedConsumerConfig {

    private final OrderEventConsumeService orderEventConsumeService;

    /**
     * 消费订单创建事件
     *
     * @return 订单创建事件消费函数
     */
    @Bean
    public Consumer<Message<OrderCreatedEvent>> orderCreatedConsumer() {
        return message -> {
            OrderCreatedEvent event = message.getPayload();
            String messageId = getMessageId(event, message);

            try {
                log.info("开始消费订单创建消息，messageId={}，headers={}", messageId, message.getHeaders());
                orderEventConsumeService.consume(event, message.getHeaders());
                log.info("订单创建消息消费完成，messageId={}，orderId={}", messageId, event.orderId());
            } catch (IllegalArgumentException e) {
                log.error("订单创建消息参数非法，messageId={}，payload={}", messageId, event, e);
                throw e;
            } catch (Exception e) {
                log.error("订单创建消息消费异常，messageId={}，payload={}", messageId, event, e);
                throw e;
            }
        };
    }

    /**
     * 获取消息 ID
     *
     * @param event   订单创建事件
     * @param message 原始消息
     * @return 消息 ID
     */
    private String getMessageId(OrderCreatedEvent event, Message<OrderCreatedEvent> message) {
        if (event != null && StrUtil.isNotBlank(event.messageId())) {
            return event.messageId();
        }
        Object headerMessageId = message.getHeaders().get("messageId");
        return headerMessageId == null ? "UNKNOWN" : String.valueOf(headerMessageId);
    }
}
```

如果希望应用内统一记录错误消息，可以订阅全局 `errorChannel`。Spring Cloud Stream 会将各个消费者的错误通道桥接到全局 Spring Integration `errorChannel`；也可以使用具体通道名，例如 `<destination>.<group>.errors`。没有显式消费错误通道时，错误通常会被框架日志记录，后续处理仍由 Binder 配置决定。([Home](https://docs.spring.io/spring-cloud-stream/docs/current-snapshot/reference/htmlsingle/index.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/stream/error/StreamGlobalErrorHandler.java`

该类统一记录 Spring Cloud Stream 消费异常上下文。

```java
package io.github.atengk.stream.error;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.stereotype.Component;

/**
 * Stream 全局错误处理器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
public class StreamGlobalErrorHandler {

    /**
     * 处理全局错误通道消息
     *
     * @param errorMessage 错误消息
     */
    @ServiceActivator(inputChannel = "errorChannel")
    public void handleError(ErrorMessage errorMessage) {
        Throwable throwable = errorMessage.getPayload();
        Message<?> originalMessage = errorMessage.getOriginalMessage();

        if (originalMessage == null) {
            log.error("Stream 消费异常，未获取到原始消息", throwable);
            return;
        }

        log.error("Stream 消费异常，headers={}，payload={}",
                JSONUtil.toJsonStr(originalMessage.getHeaders()),
                JSONUtil.toJsonStr(originalMessage.getPayload()),
                throwable);
    }
}
```

### 重试机制配置

重试机制用于处理短暂性异常。Spring Cloud Stream 的通用消费者重试配置位于 `spring.cloud.stream.bindings.<bindingName>.consumer.*` 下，常用配置包括 `maxAttempts`、`backOffInitialInterval`、`backOffMaxInterval`、`backOffMultiplier` 和 `defaultRetryable`。([Home](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/binding-properties.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

下面配置订单创建消费者的重试策略。

```yaml
spring:
  cloud:
    function:
      # 启用订单创建事件消费者
      definition: orderCreatedConsumer

    stream:
      bindings:
        orderCreatedConsumer-in-0:
          # 订单创建事件目标
          destination: order.created
          # 固定消费组，生产环境必须配置
          group: order-service
          contentType: application/json
          consumer:
            # 最大消费尝试次数，包含首次消费；设置为 1 表示关闭框架重试
            maxAttempts: 3
            # 初始重试间隔，单位毫秒
            backOffInitialInterval: 1000
            # 最大重试间隔，单位毫秒
            backOffMaxInterval: 10000
            # 重试间隔倍率
            backOffMultiplier: 2.0
            # 未显式声明的异常默认可重试
            defaultRetryable: true
            # 指定异常是否可重试；参数非法通常不需要重复消费
            retryableExceptions:
              java.lang.IllegalArgumentException: false
```

重试参数建议如下：

| 场景             | `maxAttempts` | 建议                       |
| ---------------- | ------------- | -------------------------- |
| 普通业务消息     | 3             | 默认即可，失败后进死信     |
| 外部接口短暂失败 | 3 到 5        | 配合退避间隔，避免打爆下游 |
| 参数非法         | 1 或不可重试  | 不要浪费重试次数           |
| 高吞吐日志消息   | 1 到 2        | 优先保证吞吐，失败进入 DLT |
| 金融支付类消息   | 3 到 5        | 配合本地事务、幂等和补偿表 |

重试次数不是越大越好。重试会阻塞当前消费者线程；Kafka 场景下，如果同一分区中某条消息反复失败，还可能影响该分区后续消息处理。对于明确不可恢复的业务错误，应尽快进入死信队列或异常表，后续由人工或补偿任务处理。

### 死信队列配置

死信队列用于保存最终消费失败的消息。它不是异常处理的终点，而是异常消息的隔离区。生产环境中，死信队列必须配合告警、排查、重放或人工处理流程，否则只是把失败消息换了一个地方堆积。

RabbitMQ Binder 支持 `autoBindDlq` 自动声明 DLQ；当启用重试且 `maxAttempts > 1` 时，重试耗尽后失败消息会进入 DLQ。`republishToDlq` 会让 Binder 重新发布失败消息到 DLQ，并附加异常堆栈等 Header 信息。([Home](https://docs.spring.io/spring-cloud-stream-binder-rabbit/docs/current-SNAPSHOT/reference/html/spring-cloud-stream-binder-rabbit.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application-rabbit.yml`

下面配置 RabbitMQ 消费失败后的 DLQ 行为。

```yaml
spring:
  cloud:
    stream:
      bindings:
        orderCreatedConsumer-in-0:
          # 订单创建事件
          destination: order.created
          # 固定消费组；DLQ 场景必须配置稳定 group
          group: order-service
          contentType: application/json
          consumer:
            # 重试 3 次后仍失败，再进入 DLQ
            maxAttempts: 3
            backOffInitialInterval: 1000
            backOffMaxInterval: 10000
            backOffMultiplier: 2.0

      rabbit:
        bindings:
          orderCreatedConsumer-in-0:
            consumer:
              # 自动绑定死信队列
              autoBindDlq: true
              # 将失败消息重新发布到 DLQ，并附加异常信息 Header
              republishToDlq: true
              # 不重新入队，避免异常消息无限循环
              requeueRejected: false
              # 自定义死信交换机名称
              deadLetterExchange: order.created.dlx
              # 自定义死信队列名称
              deadLetterQueueName: order.created.order-service.dlq
              # 自定义死信路由键
              deadLetterRoutingKey: order.created.dlq
```

Kafka Binder 的死信目标通常称为 DLT 或 DLQ Topic。Kafka Binder 中 DLQ 默认不启用，需要显式设置 `spring.cloud.stream.kafka.bindings.<binding-name>.consumer.enable-dlq=true`，并且必须配置消费组；匿名消费者不能启用 DLQ。重试耗尽后，失败记录会发送到 DLQ Topic，默认 Topic 名称为 `error.<destination>.<group>`，也可以通过 `dlqName` 指定。([Home](https://docs.spring.io/spring-cloud-stream/reference/kafka/kafka-binder/dlq.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application-kafka.yml`

下面配置 Kafka 消费失败后的 DLT 行为。

```yaml
spring:
  cloud:
    stream:
      bindings:
        orderCreatedConsumer-in-0:
          # Kafka Topic
          destination: order.created
          # 固定消费组；Kafka DLQ 必须配置 group
          group: order-service
          contentType: application/json
          consumer:
            # 重试耗尽后进入 DLT
            maxAttempts: 3
            backOffInitialInterval: 1000
            backOffMaxInterval: 10000
            backOffMultiplier: 2.0

      kafka:
        binder:
          # Kafka Broker 地址
          brokers: localhost:9092

        bindings:
          orderCreatedConsumer-in-0:
            consumer:
              # 启用 Kafka DLT
              enableDlq: true
              # 自定义 DLT Topic 名称
              dlqName: order.created.order-service.dlt
```

死信队列处理建议如下：

| 项目         | 建议                                                       |
| ------------ | ---------------------------------------------------------- |
| DLQ 命名     | `<destination>.<group>.dlq` 或 `<destination>.<group>.dlt` |
| 是否自动创建 | 本地可自动创建，生产建议由运维或 IaC 管理                  |
| 告警         | DLQ 有消息必须触发告警                                     |
| 重放         | 提供受控重放工具，不要直接手工复制消息                     |
| 幂等         | 重放消息必须经过幂等校验                                   |
| 保留时间     | 根据业务审计要求设置 TTL 或 Topic 保留周期                 |

### 错误日志记录

错误日志记录的目标是让排查人员能快速定位“哪条消息、哪个业务键、哪个消费者、失败原因是什么”。日志不要只记录异常栈，也不要只记录完整大对象。推荐记录消息 ID、事件类型、业务键、消费组、Destination、异常类型和关键业务字段。

文件位置：`src/main/java/io/github/atengk/stream/error/StreamErrorLogUtil.java`

该工具类用于统一格式化错误日志中的消息上下文。

```java
package io.github.atengk.stream.error;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * Stream 错误日志工具类
 *
 * @author Ateng
 * @since 2026-05-05
 */
public class StreamErrorLogUtil {

    private StreamErrorLogUtil() {
    }

    /**
     * 构建消息错误上下文
     *
     * @param message 原始消息
     * @return 错误上下文 JSON
     */
    public static String buildErrorContext(Message<?> message) {
        if (message == null) {
            return "{}";
        }

        MessageHeaders headers = message.getHeaders();
        Dict context = Dict.create()
                .set("messageId", getHeader(headers, "messageId"))
                .set("eventType", getHeader(headers, "eventType"))
                .set("businessKey", getHeader(headers, "businessKey"))
                .set("contentType", headers.get(MessageHeaders.CONTENT_TYPE))
                .set("payload", message.getPayload());

        return JSONUtil.toJsonStr(context);
    }

    /**
     * 获取消息头
     *
     * @param headers 消息头
     * @param name    消息头名称
     * @return 消息头值
     */
    private static String getHeader(MessageHeaders headers, String name) {
        Object value = headers.get(name);
        return value == null ? StrUtil.EMPTY : String.valueOf(value);
    }
}
```

在消费者中使用：

```java
try {
    orderEventConsumeService.consume(event, message.getHeaders());
} catch (Exception e) {
    log.error("订单创建消息处理失败，context={}", StreamErrorLogUtil.buildErrorContext(message), e);
    throw e;
}
```

错误日志建议至少包含以下字段：

| 字段               | 说明                            |
| ------------------ | ------------------------------- |
| `messageId`        | 消息唯一 ID，幂等和追踪核心字段 |
| `eventType`        | 事件类型                        |
| `businessKey`      | 订单号、用户 ID、流水号等       |
| `destination`      | 消息目标                        |
| `group`            | 消费组                          |
| `exceptionClass`   | 异常类名                        |
| `exceptionMessage` | 异常摘要                        |
| `payload`          | 消息体，注意脱敏                |
| `traceId`          | 链路追踪 ID                     |

生产环境中，日志里的手机号、身份证号、银行卡号、Token、地址等敏感字段必须脱敏。对于大消息体，不建议完整打印，可只打印业务主键和摘要字段。

## 消息可靠性

消息可靠性不是单一配置项，而是生产端、Broker、消费端和业务幂等共同组成的结果。Spring Cloud Stream 能提供统一的绑定、重试、错误通道和 Binder 能力，但不能单独保证“业务端到端 exactly once”。生产项目通常需要结合本地消息表、发布确认、死信队列、幂等消费、状态机和补偿任务。

### 生产端可靠性

生产端可靠性关注“业务操作完成后，消息是否可靠发出”。`StreamBridge.send(...)` 返回 `boolean`，只能说明消息是否成功提交给输出绑定，不等价于消息已经被 Broker 持久化，也不等价于消费者已处理成功。对于普通业务事件，可以检查返回值并记录消息 ID；对于核心链路，应使用本地消息表或事务外盒模式。

推荐生产端可靠性分级：

| 等级           | 适用场景                 | 方案                              |
| -------------- | ------------------------ | --------------------------------- |
| 基础可靠       | 通知、日志、非核心事件   | `StreamBridge.send` + 错误日志    |
| 中等可靠       | 订单事件、状态同步       | 发送失败抛异常 + 补偿任务         |
| 高可靠         | 支付、资金、库存关键变更 | 本地消息表 / Transactional Outbox |
| Broker 确认    | RabbitMQ 高可靠投递      | Publisher Confirm                 |
| Kafka 可靠发送 | Kafka 生产端             | `acks`、重试、幂等生产者配置      |

RabbitMQ Binder 支持发布者确认。RabbitMQ 文档说明，使用发布确认时连接工厂需要配置 `publisherConfirmType=CORRELATED`；Spring Cloud Stream Rabbit Binder 支持 `useConfirmHeader` 或 `confirmAckChannel` 两种机制，且二者互斥。([Home](https://docs.spring.io/spring-cloud-stream/docs/current-SNAPSHOT/reference/html/spring-cloud-stream-binder-rabbit.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application-rabbit.yml`

下面配置 RabbitMQ 发布确认相关参数。

```yaml
spring:
  rabbitmq:
    # 启用发布者确认，RabbitMQ Publisher Confirm 需要该配置
    publisher-confirm-type: correlated
    # 启用消息返回，路由失败时可以感知 returned message
    publisher-returns: true

  cloud:
    stream:
      bindings:
        orderCreated-out-0:
          destination: order.created
          contentType: application/json
          producer:
            # 启用生产者错误通道，便于捕获异步发送异常
            errorChannelEnabled: true

      rabbit:
        bindings:
          orderCreated-out-0:
            producer:
              # 使用确认 Header 方式获取发布结果
              useConfirmHeader: true
```

如果业务要求“数据库事务提交成功后消息不能丢”，推荐使用本地消息表。业务事务中同时写业务表和消息表，事务提交后由发布任务扫描消息表并发送 MQ，发送成功后更新状态。

文件位置：`sql/mq_message_outbox.sql`

下面的 SQL 用于创建本地消息表，适合 MySQL 或兼容数据库。

```sql
CREATE TABLE mq_message_outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息唯一 ID',
    destination VARCHAR(128) NOT NULL COMMENT '消息目标',
    event_type VARCHAR(64) NOT NULL COMMENT '事件类型',
    business_key VARCHAR(128) NOT NULL COMMENT '业务主键',
    payload JSON NOT NULL COMMENT '消息内容',
    status VARCHAR(32) NOT NULL COMMENT '消息状态：WAITING、SENDING、SENT、FAILED',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    next_retry_time DATETIME NULL COMMENT '下次重试时间',
    last_error VARCHAR(1000) NULL COMMENT '最后一次错误信息',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_status_next_retry_time (status, next_retry_time),
    KEY idx_business_key (business_key)
) COMMENT='MQ 本地消息表';
```

本地消息表的核心流程如下：

```text
业务事务开始
  ├── 写订单表
  ├── 写 mq_message_outbox，状态 WAITING
业务事务提交
  ↓
消息发布任务扫描 WAITING / FAILED
  ↓
发送到 Spring Cloud Stream Binding
  ↓
发送成功：状态改为 SENT
发送失败：记录错误，增加 retry_count，设置 next_retry_time
```

这种模式避免了“业务数据已提交，但 MQ 发送失败”的不一致问题。它不能消除重复发送，因此消费端仍然必须做幂等。

### 消费端可靠性

消费端可靠性关注“消息被投递后，业务是否正确处理”。消费者必须做到：业务处理成功后再确认消息，业务处理失败时抛出异常，重复消息不重复执行业务副作用，不可恢复异常进入死信或异常表。

消费端可靠性建议如下：

| 能力     | 做法                                                      |
| -------- | --------------------------------------------------------- |
| 幂等     | 使用 `messageId`、业务唯一键、数据库唯一索引或 Redis 去重 |
| 事务     | 业务落库和消费日志写入放在同一个事务内                    |
| 异常传播 | 处理失败必须抛异常                                        |
| 状态机   | 按业务状态判断是否允许处理                                |
| 死信     | 重试耗尽后进入 DLQ/DLT                                    |
| 补偿     | 提供异常消息重放和人工处理能力                            |

文件位置：`src/main/java/io/github/atengk/stream/service/impl/ReliableOrderConsumeServiceImpl.java`

下面的示例演示消费端可靠处理骨架：先幂等锁定，再执行业务，成功后标记已消费，失败时释放锁并抛异常。

```java
package io.github.atengk.stream.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.stream.event.OrderCreatedEvent;
import io.github.atengk.stream.service.MessageIdempotentService;
import io.github.atengk.stream.service.OrderEventConsumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * 可靠订单消费服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class ReliableOrderConsumeServiceImpl implements OrderEventConsumeService {

    private final MessageIdempotentService messageIdempotentService;

    /**
     * 可靠消费订单创建事件
     *
     * @param event   订单创建事件
     * @param headers 消息头
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void consume(OrderCreatedEvent event, MessageHeaders headers) {
        checkEvent(event);

        String messageId = event.messageId();
        boolean locked = messageIdempotentService.tryLock(messageId, Duration.ofMinutes(30));
        if (!locked) {
            log.warn("订单创建消息已处理或处理中，跳过重复消费，messageId={}，orderId={}",
                    messageId, event.orderId());
            return;
        }

        try {
            // TODO 在同一个事务中完成业务落库、消费日志写入、状态更新等操作
            log.info("可靠消费订单创建消息，messageId={}，orderId={}", messageId, event.orderId());

            messageIdempotentService.markConsumed(messageId, Duration.ofDays(7));
        } catch (Exception e) {
            messageIdempotentService.release(messageId);
            log.error("可靠消费订单创建消息失败，messageId={}，orderId={}", messageId, event.orderId(), e);
            throw e;
        }
    }

    /**
     * 校验订单事件
     *
     * @param event 订单创建事件
     */
    private void checkEvent(OrderCreatedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("订单创建事件不能为空");
        }
        if (StrUtil.isBlank(event.messageId())) {
            throw new IllegalArgumentException("消息 ID 不能为空");
        }
        if (StrUtil.isBlank(event.orderId())) {
            throw new IllegalArgumentException("订单号不能为空");
        }
    }
}
```

如果业务已经具备强状态机，例如订单只能从 `CREATED` 流转到 `PAID`，消费端应先读取当前状态，再判断是否允许流转。即使重复消费，也只能命中“状态已处理，直接跳过”的分支。

### 消息确认机制

消息确认机制用于告诉 Broker 当前消息是否已经被成功处理。不同 Binder 的确认机制不同，Spring Cloud Stream 默认会尽量屏蔽底层差异：消费函数正常返回通常表示处理成功，抛出异常表示处理失败。

RabbitMQ 场景中，普通业务消费通常使用自动确认语义：监听方法正常结束后确认消息，抛出异常后根据重试、拒绝、重新入队、DLQ 配置处理。不要在业务代码中吞异常，否则消息会被当作成功处理。

Kafka 场景中，Offset 提交就是确认机制的核心。Kafka Binder 支持通过 `ackMode` 配置确认模式；当配置为 `MANUAL` 或 `MANUAL_IMMEDIATE` 时，消费消息 Header 中会出现 `kafka_acknowledgment`，应用可以在业务处理成功后手动提交。([Home](https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/spring-cloud-stream-binder-kafka.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application-kafka.yml`

下面配置 Kafka 手动确认。

```yaml
spring:
  cloud:
    stream:
      bindings:
        orderCreatedManualAckConsumer-in-0:
          destination: order.created
          group: order-manual-ack-service
          contentType: application/json

      kafka:
        bindings:
          orderCreatedManualAckConsumer-in-0:
            consumer:
              # 使用手动确认模式
              ackMode: MANUAL
```

文件位置：`src/main/java/io/github/atengk/stream/function/OrderManualAckConsumerConfig.java`

该配置类演示 Kafka 手动确认。只有业务处理成功后才调用 `acknowledge()`；失败时继续抛出异常。

```java
package io.github.atengk.stream.function;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.stream.event.OrderCreatedEvent;
import io.github.atengk.stream.service.OrderEventConsumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

/**
 * Kafka 手动确认消费者配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class OrderManualAckConsumerConfig {

    private final OrderEventConsumeService orderEventConsumeService;

    /**
     * 手动确认订单创建事件
     *
     * @return 手动确认消费函数
     */
    @Bean
    public Consumer<Message<OrderCreatedEvent>> orderCreatedManualAckConsumer() {
        return message -> {
            OrderCreatedEvent event = message.getPayload();
            if (event == null || StrUtil.isBlank(event.messageId())) {
                throw new IllegalArgumentException("订单创建事件无效");
            }

            Acknowledgment acknowledgment = message.getHeaders()
                    .get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment.class);

            try {
                orderEventConsumeService.consume(event, message.getHeaders());

                if (acknowledgment != null) {
                    acknowledgment.acknowledge();
                    log.info("Kafka 消息手动确认完成，messageId={}，orderId={}",
                            event.messageId(), event.orderId());
                } else {
                    log.warn("未获取到 Kafka Acknowledgment，messageId={}，orderId={}",
                            event.messageId(), event.orderId());
                }
            } catch (Exception e) {
                log.error("Kafka 手动确认消费失败，messageId={}，orderId={}",
                        event.messageId(), event.orderId(), e);
                throw e;
            }
        };
    }
}
```

手动确认适合需要精确控制 Offset 提交时机的场景，但会增加复杂度。普通业务场景优先使用默认确认模型，再通过幂等、重试和 DLQ 保证处理可靠性。

### 重复消费处理

重复消费是消息系统中的常态风险，不能依赖配置完全避免。重复消费可能来自网络抖动、消费者处理超时、应用重启、Broker 重投、死信重放、补偿任务重复发送、生产者重复投递等。消费端应按“消息一定可能重复”设计。

重复消费处理方案如下：

| 方案           | 适用场景           | 优点             | 注意点             |
| -------------- | ------------------ | ---------------- | ------------------ |
| Redis `SETNX`  | 高吞吐快速去重     | 性能高，实现简单 | TTL 设计要合理     |
| 数据库唯一索引 | 核心业务、长期审计 | 强一致，可追溯   | 写入压力较高       |
| 业务状态机     | 订单、支付、库存   | 符合业务语义     | 状态模型要完整     |
| 消费日志表     | 关键消息追踪       | 便于排查和重放   | 需要清理策略       |
| 幂等接口       | 外部调用           | 可跨系统防重复   | 需要对方支持幂等键 |

文件位置：`sql/mq_consume_log.sql`

下面的 SQL 用于创建消费日志表，通过 `message_id` 唯一索引防止重复消费。

```sql
CREATE TABLE mq_consume_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息唯一 ID',
    destination VARCHAR(128) NOT NULL COMMENT '消息目标',
    consumer_group VARCHAR(128) NOT NULL COMMENT '消费组',
    business_key VARCHAR(128) NOT NULL COMMENT '业务主键',
    status VARCHAR(32) NOT NULL COMMENT '消费状态：PROCESSING、CONSUMED、FAILED',
    error_message VARCHAR(1000) NULL COMMENT '错误信息',
    consumed_at DATETIME NULL COMMENT '消费完成时间',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_message_id_group (message_id, consumer_group),
    KEY idx_business_key (business_key),
    KEY idx_status_created_at (status, created_at)
) COMMENT='MQ 消费日志表';
```

消费端幂等推荐流程如下：

```text
收到消息
  ↓
读取 messageId + group
  ↓
尝试写入消费日志，唯一键为 messageId + consumerGroup
  ├── 写入失败：说明已处理或处理中，跳过
  └── 写入成功：继续处理业务
        ↓
      业务处理成功：状态改为 CONSUMED
      业务处理失败：状态改为 FAILED 或删除 PROCESSING 标记，并抛异常
```

对于订单类业务，最终还要依赖业务状态机兜底。例如重复收到 `ORDER_PAID` 消息时，如果订单已经是 `PAID`，应该直接跳过；如果订单仍是 `CREATED`，才执行支付成功后的状态流转；如果订单已是 `CLOSED`，应记录异常事件并进入人工核查。这样即使幂等缓存失效，也不会因为重复消息导致错误状态变更。



## 接口与业务集成

接口与业务集成主要解决两个问题：第一，外部请求如何触发消息发送；第二，业务服务如何在事务、异常、幂等和补偿边界内正确使用消息。Spring Cloud Stream 中，REST 接口、定时任务、普通业务 Service 这类“非 Stream 函数入口”推荐通过 `StreamBridge` 发送消息；`StreamBridge` 可以在首次调用时创建输出 Binding，也可以通过 `spring.cloud.stream.output-bindings` 在应用启动时预创建输出 Binding。([Home](https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/spring-cloud-stream.html?utm_source=chatgpt.com))

### REST 接口触发消息

REST 接口触发消息适合本地调试、后台管理、业务操作后发布事件等场景。接口层只负责参数接收和基础校验，不建议直接拼装 MQ 消息；应把消息发送逻辑封装到业务 Service 或 Producer 中，便于复用、测试和统一处理异常。

文件位置：`src/main/resources/application.yml`

下面配置 REST 接口触发消息所需的输出 Binding。

```yaml
server:
  port: 18080

spring:
  application:
    name: spring-cloud-stream-demo

  cloud:
    stream:
      # 预创建 REST 接口使用的输出 Binding
      output-bindings: orderCreated-out-0

      bindings:
        orderCreated-out-0:
          # 订单创建事件目标，RabbitMQ 中通常映射为 Exchange，Kafka 中通常映射为 Topic
          destination: order.created
          # 使用 JSON 承载业务事件
          contentType: application/json
```

文件位置：`src/main/java/io/github/atengk/stream/request/CreateOrderRequest.java`

该类定义创建订单接口的请求参数。

```java
package io.github.atengk.stream.request;

import java.math.BigDecimal;

/**
 * 创建订单请求
 *
 * @author Ateng
 * @since 2026-05-05
 */
public record CreateOrderRequest(
        String orderId,
        String userId,
        BigDecimal amount
) {
}
```

文件位置：`src/main/java/io/github/atengk/stream/response/MessageSendResponse.java`

该类定义接口触发消息后的响应结构。

```java
package io.github.atengk.stream.response;

/**
 * 消息发送响应
 *
 * @author Ateng
 * @since 2026-05-05
 */
public record MessageSendResponse(
        Boolean sent,
        String messageId,
        String businessKey
) {
}
```

文件位置：`src/main/java/io/github/atengk/stream/controller/OrderController.java`

该接口接收创建订单请求，调用业务服务完成订单创建和消息发送。

```java
package io.github.atengk.stream.controller;

import io.github.atengk.stream.request.CreateOrderRequest;
import io.github.atengk.stream.response.MessageSendResponse;
import io.github.atengk.stream.service.OrderApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 订单业务接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderApplicationService orderApplicationService;

    /**
     * 创建订单并发送订单创建事件
     *
     * @param request 创建订单请求
     * @return 消息发送响应
     */
    @PostMapping
    public ResponseEntity<MessageSendResponse> createOrder(@RequestBody CreateOrderRequest request) {
        MessageSendResponse response = orderApplicationService.createOrderAndSendEvent(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
```

调用接口：

```bash
curl -X POST "http://localhost:18080/api/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORDER_10001",
    "userId": "USER_90001",
    "amount": 199.90
  }'
```

响应示例：

```json
{
  "sent": true,
  "messageId": "e17f5f44c4c047cda1d9f5a55d1dfc5f",
  "businessKey": "ORDER_10001"
}
```

REST 接口只表示“业务请求已接收，消息已提交到输出 Binding”。如果后续消费者失败，需要依赖消费端重试、死信队列、补偿任务或人工处理流程，不应让 REST 接口同步等待所有下游消费者执行完成。

### 业务服务发送消息

业务服务发送消息时，建议单独封装消息生产者，避免 Controller、Service、定时任务直接使用散落的 Binding 字符串。`StreamBridge.send` 支持发送 POJO，也支持发送 `Message`；如果发送 `Message`，消息头中的 `contentType` 等信息会被框架识别并用于输出转换。([Home](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/producing-and-consuming-messages.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/stream/event/OrderCreatedEvent.java`

该事件类作为订单创建后的业务事件消息体。

```java
package io.github.atengk.stream.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单创建事件
 *
 * @author Ateng
 * @since 2026-05-05
 */
public record OrderCreatedEvent(
        String messageId,
        String eventType,
        String orderId,
        String userId,
        BigDecimal amount,
        LocalDateTime occurredAt,
        String source
) {
}
```

文件位置：`src/main/java/io/github/atengk/stream/producer/OrderMessageProducer.java`

该生产者统一封装订单消息发送逻辑，业务层只需要传入事件对象。

```java
package io.github.atengk.stream.producer;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.stream.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

/**
 * 订单消息生产者
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageProducer {

    /**
     * 订单创建事件输出 Binding
     */
    public static final String ORDER_CREATED_OUT = "orderCreated-out-0";

    private final StreamBridge streamBridge;

    /**
     * 发送订单创建事件
     *
     * @param event 订单创建事件
     */
    public void sendOrderCreated(OrderCreatedEvent event) {
        checkEvent(event);

        Message<OrderCreatedEvent> message = MessageBuilder.withPayload(event)
                .setHeader("messageId", event.messageId())
                .setHeader("eventType", event.eventType())
                .setHeader("businessKey", event.orderId())
                .setHeader("source", event.source())
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
                .build();

        boolean sent = streamBridge.send(ORDER_CREATED_OUT, message);
        if (!sent) {
            log.error("订单创建事件发送失败，messageId={}，orderId={}，payload={}",
                    event.messageId(), event.orderId(), JSONUtil.toJsonStr(event));
            throw new IllegalStateException("订单创建事件发送失败");
        }

        log.info("订单创建事件发送成功，messageId={}，orderId={}",
                event.messageId(), event.orderId());
    }

    /**
     * 校验订单创建事件
     *
     * @param event 订单创建事件
     */
    private void checkEvent(OrderCreatedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("订单创建事件不能为空");
        }
        if (StrUtil.isBlank(event.messageId())) {
            throw new IllegalArgumentException("消息 ID 不能为空");
        }
        if (StrUtil.isBlank(event.orderId())) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        if (StrUtil.isBlank(event.userId())) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        if (event.amount() == null) {
            throw new IllegalArgumentException("订单金额不能为空");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/stream/service/OrderApplicationService.java`

该接口定义订单业务服务能力。

```java
package io.github.atengk.stream.service;

import io.github.atengk.stream.request.CreateOrderRequest;
import io.github.atengk.stream.response.MessageSendResponse;

/**
 * 订单应用服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface OrderApplicationService {

    /**
     * 创建订单并发送订单创建事件
     *
     * @param request 创建订单请求
     * @return 消息发送响应
     */
    MessageSendResponse createOrderAndSendEvent(CreateOrderRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/stream/service/impl/OrderApplicationServiceImpl.java`

该实现类演示业务服务中如何创建业务数据并发送消息。示例中用日志代替真实数据库写入，实际项目中应替换为 Mapper、Repository 或领域服务调用。

```java
package io.github.atengk.stream.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.stream.event.OrderCreatedEvent;
import io.github.atengk.stream.producer.OrderMessageProducer;
import io.github.atengk.stream.request.CreateOrderRequest;
import io.github.atengk.stream.response.MessageSendResponse;
import io.github.atengk.stream.service.OrderApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 订单应用服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderApplicationServiceImpl implements OrderApplicationService {

    private final OrderMessageProducer orderMessageProducer;

    /**
     * 创建订单并发送订单创建事件
     *
     * @param request 创建订单请求
     * @return 消息发送响应
     */
    @Override
    public MessageSendResponse createOrderAndSendEvent(CreateOrderRequest request) {
        checkRequest(request);

        // TODO 这里替换为真实订单落库逻辑，例如 orderMapper.insert(order)
        log.info("订单创建成功，orderId={}，userId={}，amount={}",
                request.orderId(), request.userId(), request.amount());

        String messageId = IdUtil.fastSimpleUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(
                messageId,
                "ORDER_CREATED",
                request.orderId(),
                request.userId(),
                request.amount(),
                LocalDateTime.now(),
                "order-service"
        );

        orderMessageProducer.sendOrderCreated(event);
        return new MessageSendResponse(true, messageId, request.orderId());
    }

    /**
     * 校验创建订单请求
     *
     * @param request 创建订单请求
     */
    private void checkRequest(CreateOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("创建订单请求不能为空");
        }
        if (StrUtil.isBlank(request.orderId())) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        if (StrUtil.isBlank(request.userId())) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        if (request.amount() == null) {
            throw new IllegalArgumentException("订单金额不能为空");
        }
    }
}
```

这种写法适合普通业务消息。如果订单落库成功后消息绝对不能丢，需要使用本地消息表或事务外盒模式，不应只依赖 `StreamBridge.send` 返回值。

### 消费端业务处理

消费端业务处理建议分为三层：函数入口、业务消费服务、幂等或消费日志服务。函数入口只负责接收消息和异常传播；业务消费服务负责校验、事务、幂等和业务落库；幂等服务负责识别重复消息。

文件位置：`src/main/resources/application.yml`

下面配置订单创建事件消费者。函数式 Binding 的命名规则是输入端 `<functionName>-in-0`、输出端 `<functionName>-out-0`；例如 `orderCreatedConsumer` 的输入 Binding 是 `orderCreatedConsumer-in-0`。([Home](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/functional-binding-names.html?utm_source=chatgpt.com))

```yaml
spring:
  cloud:
    function:
      # 启用订单创建事件消费者
      definition: orderCreatedConsumer

    stream:
      bindings:
        orderCreatedConsumer-in-0:
          # 消费订单创建事件
          destination: order.created
          # 固定消费组，生产环境必须配置
          group: order-service
          contentType: application/json
          consumer:
            # 消费失败最大尝试次数，包含首次消费
            maxAttempts: 3
            # 单实例消费并发数
            concurrency: 2
```

文件位置：`src/main/java/io/github/atengk/stream/function/OrderCreatedConsumerConfig.java`

该配置类定义消息监听函数，并将实际业务处理委托给 `OrderConsumeService`。

```java
package io.github.atengk.stream.function;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.stream.event.OrderCreatedEvent;
import io.github.atengk.stream.service.OrderConsumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

/**
 * 订单创建消息消费者配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class OrderCreatedConsumerConfig {

    private final OrderConsumeService orderConsumeService;

    /**
     * 消费订单创建事件
     *
     * @return 订单创建事件消费者
     */
    @Bean
    public Consumer<Message<OrderCreatedEvent>> orderCreatedConsumer() {
        return message -> {
            OrderCreatedEvent event = message.getPayload();
            if (event == null || StrUtil.isBlank(event.messageId())) {
                throw new IllegalArgumentException("订单创建事件无效");
            }

            log.info("收到订单创建事件，messageId={}，orderId={}，headers={}",
                    event.messageId(), event.orderId(), message.getHeaders());

            orderConsumeService.handleOrderCreated(event);
        };
    }
}
```

文件位置：`src/main/java/io/github/atengk/stream/service/OrderConsumeService.java`

该接口定义消费端业务处理能力。

```java
package io.github.atengk.stream.service;

import io.github.atengk.stream.event.OrderCreatedEvent;

/**
 * 订单消费服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface OrderConsumeService {

    /**
     * 处理订单创建事件
     *
     * @param event 订单创建事件
     */
    void handleOrderCreated(OrderCreatedEvent event);
}
```

文件位置：`src/main/java/io/github/atengk/stream/service/impl/OrderConsumeServiceImpl.java`

该实现类演示消费端业务处理流程：校验消息、执行幂等、处理业务、失败时抛出异常。

```java
package io.github.atengk.stream.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.stream.event.OrderCreatedEvent;
import io.github.atengk.stream.service.MessageIdempotentService;
import io.github.atengk.stream.service.OrderConsumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * 订单消费服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderConsumeServiceImpl implements OrderConsumeService {

    private final MessageIdempotentService messageIdempotentService;

    /**
     * 处理订单创建事件
     *
     * @param event 订单创建事件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleOrderCreated(OrderCreatedEvent event) {
        checkEvent(event);

        boolean locked = messageIdempotentService.tryLock(event.messageId(), Duration.ofMinutes(30));
        if (!locked) {
            log.warn("订单创建事件重复消费，直接跳过，messageId={}，orderId={}",
                    event.messageId(), event.orderId());
            return;
        }

        try {
            // TODO 替换为真实业务处理，例如写订单扩展表、更新统计表、发送站内信等
            log.info("订单创建事件业务处理完成，messageId={}，orderId={}，userId={}",
                    event.messageId(), event.orderId(), event.userId());

            messageIdempotentService.markConsumed(event.messageId(), Duration.ofDays(7));
        } catch (Exception e) {
            messageIdempotentService.release(event.messageId());
            log.error("订单创建事件业务处理失败，messageId={}，orderId={}",
                    event.messageId(), event.orderId(), e);
            throw e;
        }
    }

    /**
     * 校验订单创建事件
     *
     * @param event 订单创建事件
     */
    private void checkEvent(OrderCreatedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("订单创建事件不能为空");
        }
        if (StrUtil.isBlank(event.messageId())) {
            throw new IllegalArgumentException("消息 ID 不能为空");
        }
        if (StrUtil.isBlank(event.orderId())) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        if (StrUtil.isBlank(event.userId())) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        if (event.amount() == null) {
            throw new IllegalArgumentException("订单金额不能为空");
        }
    }
}
```

消费端业务处理失败时必须抛出异常。RabbitMQ Binder 在配置 `autoBindDlq=true` 后可以自动声明死信队列；如果启用重试，重试耗尽后的失败消息会进入 DLQ。([Spring Enterprise Docs](https://docs.enterprise.spring.io/spring-cloud-stream/reference/rabbit/rabbit_overview.html?utm_source=chatgpt.com))

### 事务边界设计

事务边界设计是消息系统中最容易出错的部分。需要明确：数据库事务和 MQ 发送不是天然的同一个事务。普通写法中，如果数据库提交成功但消息发送失败，就会产生“业务数据已变更、事件未发布”的不一致；如果消息先发送成功但数据库回滚，则会产生“下游收到不存在或未提交业务数据”的问题。

常见事务边界方案如下：

| 方案                | 适用场景       | 说明                               |
| ------------------- | -------------- | ---------------------------------- |
| 先写库后发消息      | 普通业务消息   | 简单，但存在消息发送失败风险       |
| 本地消息表          | 核心业务事件   | 业务表和消息表同事务，异步发布消息 |
| 事务外盒            | 微服务事件发布 | 与本地消息表类似，适合事件驱动架构 |
| 消费端幂等 + 状态机 | 所有消费端     | 防止重复消费和乱序状态变更         |
| 分布式事务          | 少数强一致场景 | 成本高，优先级低于最终一致设计     |

推荐核心链路使用本地消息表：

```text
创建订单请求
  ↓
开启数据库事务
  ├── 写订单表
  └── 写 mq_message_outbox 表，状态 WAITING
提交数据库事务
  ↓
后台任务扫描 WAITING 消息
  ↓
发送 Spring Cloud Stream 消息
  ↓
发送成功后更新消息状态为 SENT
```

文件位置：`src/main/java/io/github/atengk/stream/service/impl/OrderApplicationServiceWithOutboxImpl.java`

该实现类演示本地消息表模式的业务边界。示例中省略真实 Mapper，只保留关键事务流程。

```java
package io.github.atengk.stream.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.stream.event.OrderCreatedEvent;
import io.github.atengk.stream.request.CreateOrderRequest;
import io.github.atengk.stream.response.MessageSendResponse;
import io.github.atengk.stream.service.OrderApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 基于本地消息表的订单应用服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class OrderApplicationServiceWithOutboxImpl implements OrderApplicationService {

    /**
     * 创建订单并写入本地消息表
     *
     * @param request 创建订单请求
     * @return 消息发送响应
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageSendResponse createOrderAndSendEvent(CreateOrderRequest request) {
        checkRequest(request);

        String messageId = IdUtil.fastSimpleUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(
                messageId,
                "ORDER_CREATED",
                request.orderId(),
                request.userId(),
                request.amount(),
                LocalDateTime.now(),
                "order-service"
        );

        // TODO 写订单表：orderMapper.insert(order)
        log.info("订单数据已写入，orderId={}，userId={}", request.orderId(), request.userId());

        // TODO 写本地消息表：mqMessageOutboxMapper.insert(...)
        log.info("本地消息表已写入，messageId={}，destination={}，payload={}",
                messageId, "order.created", JSONUtil.toJsonStr(event));

        // 此处不直接发送 MQ，避免数据库事务未提交时消息已被下游消费
        return new MessageSendResponse(true, messageId, request.orderId());
    }

    /**
     * 校验创建订单请求
     *
     * @param request 创建订单请求
     */
    private void checkRequest(CreateOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("创建订单请求不能为空");
        }
        if (StrUtil.isBlank(request.orderId())) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        if (StrUtil.isBlank(request.userId())) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        if (request.amount() == null) {
            throw new IllegalArgumentException("订单金额不能为空");
        }
    }
}
```

事务边界的基本原则是：生产端不要在数据库事务未提交前让下游看到消息；消费端不要在业务处理失败后确认消息；重复消息必须通过幂等和业务状态机兜底。

## 本地调试与验证

本地调试的目标是验证完整链路：消息中间件可用，应用启动成功，输出 Binding 和输入 Binding 创建成功，REST 接口可以发送消息，消费者可以收到消息，异常场景可以触发重试或死信。Binding 配置格式统一为 `spring.cloud.stream.bindings.<bindingName>.<property>`，公共默认项可以通过 `spring.cloud.stream.default.*` 设置。([Home](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/binding-properties.html?utm_source=chatgpt.com))

### 启动消息中间件

本地优先使用 RabbitMQ 验证普通业务消息，原因是管理控制台直观，队列、交换机、死信队列容易观察。

文件位置：`docker-compose-rabbitmq.yml`

下面配置用于启动本地 RabbitMQ 和管理控制台。

```yaml
services:
  rabbitmq:
    image: rabbitmq:3-management
    container_name: ateng-rabbitmq
    restart: unless-stopped
    ports:
      # AMQP 连接端口
      - "5672:5672"
      # 管理控制台端口
      - "15672:15672"
    environment:
      # 本地开发账号
      RABBITMQ_DEFAULT_USER: ateng
      # 本地开发密码
      RABBITMQ_DEFAULT_PASS: 123456
    volumes:
      # 持久化 RabbitMQ 数据
      - rabbitmq_data:/var/lib/rabbitmq

volumes:
  rabbitmq_data:
```

启动命令：

```bash
docker compose -f docker-compose-rabbitmq.yml up -d
docker ps | grep ateng-rabbitmq
```

访问控制台：

```text
地址：http://localhost:15672
账号：ateng
密码：123456
```

如果需要验证 Kafka 场景，可以启动 Kafka：

```bash
docker compose -f docker-compose-kafka.yml up -d
docker ps | grep ateng-kafka
```

创建测试 Topic：

```bash
docker exec -it ateng-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create \
  --topic order.created \
  --partitions 3 \
  --replication-factor 1
```

查看 Topic：

```bash
docker exec -it ateng-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --list
```

### 启动应用服务

应用启动前需要确认 Maven 依赖、Profile、RabbitMQ 连接参数和函数定义是否正确。

文件位置：`src/main/resources/application-rabbit.yml`

下面配置本地 RabbitMQ 调试环境。

```yaml
spring:
  rabbitmq:
    # RabbitMQ 服务地址
    host: localhost
    # RabbitMQ AMQP 端口
    port: 5672
    # 本地开发账号
    username: ateng
    # 本地开发密码
    password: 123456

  cloud:
    function:
      # 同时启用订单消费者
      definition: orderCreatedConsumer

    stream:
      defaultBinder: rabbit

      # REST 接口触发消息发送时预创建输出 Binding
      output-bindings: orderCreated-out-0

      bindings:
        orderCreated-out-0:
          # 生产者发送到订单创建事件目标
          destination: order.created
          contentType: application/json

        orderCreatedConsumer-in-0:
          # 消费者监听同一个目标
          destination: order.created
          # 固定消费组
          group: order-service
          contentType: application/json
          consumer:
            maxAttempts: 3
            concurrency: 2

      rabbit:
        bindings:
          orderCreatedConsumer-in-0:
            consumer:
              # 本地调试开启 DLQ，便于观察失败消息
              autoBindDlq: true
              republishToDlq: true
              requeueRejected: false
```

启动应用：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=rabbit
```

或者先打包再启动：

```bash
mvn clean package -DskipTests
java -jar target/spring-cloud-stream-demo-1.0.0.jar --spring.profiles.active=rabbit
```

启动后重点观察日志中是否有以下信息：

```text
Started spring-cloud-stream-demo
Binding orderCreated-out-0
Binding orderCreatedConsumer-in-0
```

RabbitMQ 控制台中应能看到类似资源：

```text
Exchange: order.created
Queue: order.created.order-service
DLQ: order.created.order-service.dlq
```

如果没有看到消费者队列，优先检查 `spring.cloud.function.definition` 是否包含 `orderCreatedConsumer`，以及 Binding 名称是否写成了 `orderCreatedConsumer-in-0`。

### 发送测试消息

测试消息可以通过 REST 接口发送，也可以直接在 RabbitMQ 控制台向 Exchange 发布。推荐先使用 REST 接口，因为它能覆盖 Controller、Service、Producer、Binding 和消费者完整链路。

使用 REST 接口发送：

```bash
curl -X POST "http://localhost:18080/api/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORDER_10001",
    "userId": "USER_90001",
    "amount": 199.90
  }'
```

预期响应：

```json
{
  "sent": true,
  "messageId": "e17f5f44c4c047cda1d9f5a55d1dfc5f",
  "businessKey": "ORDER_10001"
}
```

也可以使用 RabbitMQ 控制台手动发布消息。进入 `Exchanges`，选择 `order.created`，在 `Publish message` 中填写：

```json
{
  "messageId": "MANUAL_10001",
  "eventType": "ORDER_CREATED",
  "orderId": "ORDER_10001",
  "userId": "USER_90001",
  "amount": 199.90,
  "occurredAt": "2026-05-05T10:00:00",
  "source": "rabbitmq-console"
}
```

Properties 中建议添加：

```text
content_type = application/json
```

手动发布时要注意 JSON 字段必须和 Java 事件类字段对应，否则消费端可能反序列化失败。

### 验证消费结果

验证消费结果分为四类：接口响应、应用日志、RabbitMQ 控制台和异常链路。

应用日志中应看到生产者和消费者日志：

```text
订单数据已写入，orderId=ORDER_10001，userId=USER_90001
订单创建事件发送成功，messageId=xxx，orderId=ORDER_10001
收到订单创建事件，messageId=xxx，orderId=ORDER_10001
订单创建事件业务处理完成，messageId=xxx，orderId=ORDER_10001，userId=USER_90001
```

RabbitMQ 控制台中重点查看：

| 位置          | 验证点                                 |
| ------------- | -------------------------------------- |
| Exchanges     | 是否存在 `order.created`               |
| Queues        | 是否存在 `order.created.order-service` |
| Queue Ready   | 正常消费后应接近 `0`                   |
| Queue Unacked | 长时间不归零说明消费者处理慢或卡住     |
| DLQ           | 异常消息是否进入死信队列               |

如果要验证异常重试和死信队列，可以临时在消费者中制造异常：

```java
if ("ORDER_ERROR".equals(event.orderId())) {
    throw new IllegalStateException("模拟订单消费异常");
}
```

发送异常测试消息：

```bash
curl -X POST "http://localhost:18080/api/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORDER_ERROR",
    "userId": "USER_90001",
    "amount": 199.90
  }'
```

预期现象：

```text
1. 消费者日志出现多次消费异常
2. 重试次数达到 maxAttempts 后停止重试
3. RabbitMQ DLQ 中出现失败消息
4. 正常消息不受异常消息影响
```

常见问题排查如下：

| 问题                       | 可能原因                                        | 处理方式                               |
| -------------------------- | ----------------------------------------------- | -------------------------------------- |
| 接口返回成功但消费者没日志 | `spring.cloud.function.definition` 未启用消费者 | 检查函数名和 Binding 名称              |
| RabbitMQ 没有队列          | 未配置 `group` 或消费者未启动                   | 配置固定消费组并检查启动日志           |
| 消息反序列化失败           | `contentType` 错误或 JSON 字段不匹配            | 配置 `application/json` 并检查事件类   |
| 消息重复消费               | 消费端异常、重投、补偿重复发送                  | 增加幂等处理                           |
| DLQ 没有消息               | 未配置 `autoBindDlq` 或异常被吞掉               | 开启 DLQ，消费端抛出异常               |
| Kafka 消费不均衡           | 分区数小于消费者实例或并发数                    | 调整 Topic 分区和 consumer concurrency |

本地验证通过的最低标准是：REST 接口能发送消息，消费者能正常消费，重复消息能被幂等跳过，异常消息能进入 DLQ，日志中能通过 `messageId` 和 `businessKey` 追踪完整链路。



## 测试方案

测试方案需要覆盖四个层级：纯单元测试、无真实中间件的 Stream 集成测试、真实 RabbitMQ/Kafka 集成测试，以及异常和幂等专项测试。Spring Cloud Stream 官方提供了 Test Binder，可以在不连接真实消息中间件的情况下测试 Stream 应用组件；它基于 Spring Integration，在 JVM 内模拟消息 Broker，并提供 `InputDestination` 和 `OutputDestination` 用于发送和接收测试消息。([Home](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/spring_integration_test_binder.html?utm_source=chatgpt.com))

### 单元测试

单元测试不启动 Spring 容器，也不连接 RabbitMQ 或 Kafka，主要验证生产者、消费者、消息转换函数和幂等服务的业务逻辑。它的目标是快速发现参数校验、消息头构造、异常传播和幂等判断问题。

需要先确认测试依赖。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot 单元测试，包含 JUnit 5、AssertJ、Mockito 等常用测试能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Spring Cloud Stream Test Binder，用于无真实 MQ 的 Stream 集成测试 -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-stream-test-binder</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

文件位置：`src/test/java/io/github/atengk/stream/producer/OrderMessageProducerTest.java`

下面的测试类使用 Mockito 模拟 `StreamBridge`，验证生产者是否正确发送消息，以及发送失败时是否抛出异常。

```java
package io.github.atengk.stream.producer;

import io.github.atengk.stream.event.OrderCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 订单消息生产者单元测试
 *
 * @author Ateng
 * @since 2026-05-05
 */
class OrderMessageProducerTest {

    private StreamBridge streamBridge;
    private OrderMessageProducer orderMessageProducer;

    /**
     * 初始化测试对象
     */
    @BeforeEach
    void setUp() {
        streamBridge = mock(StreamBridge.class);
        orderMessageProducer = new OrderMessageProducer(streamBridge);
    }

    /**
     * 测试订单创建事件发送成功
     */
    @Test
    void shouldSendOrderCreatedEvent() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                "MSG_10001",
                "ORDER_CREATED",
                "ORDER_10001",
                "USER_90001",
                BigDecimal.valueOf(199.90),
                LocalDateTime.now(),
                "order-service"
        );

        when(streamBridge.send(eq(OrderMessageProducer.ORDER_CREATED_OUT), any(Message.class)))
                .thenReturn(true);

        orderMessageProducer.sendOrderCreated(event);

        ArgumentCaptor<Message<OrderCreatedEvent>> captor = ArgumentCaptor.forClass(Message.class);
        verify(streamBridge).send(eq(OrderMessageProducer.ORDER_CREATED_OUT), captor.capture());

        Message<OrderCreatedEvent> message = captor.getValue();
        assertThat(message.getPayload()).isEqualTo(event);
        assertThat(message.getHeaders().get("messageId")).isEqualTo("MSG_10001");
        assertThat(message.getHeaders().get("eventType")).isEqualTo("ORDER_CREATED");
        assertThat(message.getHeaders().get("businessKey")).isEqualTo("ORDER_10001");
    }

    /**
     * 测试消息发送失败时抛出异常
     */
    @Test
    void shouldThrowExceptionWhenSendFailed() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                "MSG_10002",
                "ORDER_CREATED",
                "ORDER_10002",
                "USER_90002",
                BigDecimal.valueOf(99.90),
                LocalDateTime.now(),
                "order-service"
        );

        when(streamBridge.send(eq(OrderMessageProducer.ORDER_CREATED_OUT), any(Message.class)))
                .thenReturn(false);

        assertThatThrownBy(() -> orderMessageProducer.sendOrderCreated(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("订单创建事件发送失败");
    }

    /**
     * 测试空消息体时抛出异常
     */
    @Test
    void shouldThrowExceptionWhenEventIsNull() {
        assertThatThrownBy(() -> orderMessageProducer.sendOrderCreated(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("订单创建事件不能为空");
    }
}
```

文件位置：`src/test/java/io/github/atengk/stream/function/OrderNotifyFunctionConfigTest.java`

下面的测试类直接调用 `Function`，验证订单创建事件是否能转换为订单通知事件。

```java
package io.github.atengk.stream.function;

import io.github.atengk.stream.event.OrderCreatedEvent;
import io.github.atengk.stream.event.OrderNotifyEvent;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 订单通知转换函数单元测试
 *
 * @author Ateng
 * @since 2026-05-05
 */
class OrderNotifyFunctionConfigTest {

    /**
     * 测试订单事件转换为通知事件
     */
    @Test
    void shouldConvertOrderCreatedEventToNotifyEvent() {
        OrderNotifyFunctionConfig config = new OrderNotifyFunctionConfig();
        Function<Message<OrderCreatedEvent>, Message<OrderNotifyEvent>> function = config.orderNotifyFunction();

        OrderCreatedEvent orderEvent = new OrderCreatedEvent(
                "MSG_10001",
                "ORDER_CREATED",
                "ORDER_10001",
                "USER_90001",
                BigDecimal.valueOf(199.90),
                LocalDateTime.now(),
                "order-service"
        );

        Message<OrderCreatedEvent> input = MessageBuilder.withPayload(orderEvent)
                .setHeader("messageId", orderEvent.messageId())
                .setHeader("businessKey", orderEvent.orderId())
                .build();

        Message<OrderNotifyEvent> output = function.apply(input);

        assertThat(output.getPayload().orderId()).isEqualTo("ORDER_10001");
        assertThat(output.getPayload().userId()).isEqualTo("USER_90001");
        assertThat(output.getPayload().eventType()).isEqualTo("ORDER_NOTIFY");
        assertThat(output.getHeaders().get("sourceMessageId")).isEqualTo("MSG_10001");
    }
}
```

单元测试重点关注“代码逻辑正确”，不要把 RabbitMQ、Kafka、网络、序列化链路都放进单元测试。真实 Binder 行为应放到集成测试中验证。

### 集成测试

集成测试用于验证 Spring Cloud Stream 的函数绑定、消息转换、输入输出 Destination 和 Binding 配置是否正确。Test Binder 支持 `@EnableTestBinder`，并提供 `InputDestination` 发送消息、`OutputDestination` 接收消息；当配置了 `destination` 映射时，测试中可以使用实际 destination 名称发送或接收。([Home](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/spring_integration_test_binder.html?utm_source=chatgpt.com))

文件位置：`src/test/java/io/github/atengk/stream/integration/OrderNotifyFunctionIntegrationTest.java`

下面的测试类验证 `orderNotifyFunction` 的输入输出绑定是否正常。

```java
package io.github.atengk.stream.integration;

import cn.hutool.json.JSONUtil;
import io.github.atengk.stream.event.OrderCreatedEvent;
import io.github.atengk.stream.event.OrderNotifyEvent;
import io.github.atengk.stream.function.OrderNotifyFunctionConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.EnableTestBinder;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeTypeUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 订单通知函数集成测试
 *
 * @author Ateng
 * @since 2026-05-05
 */
@EnableTestBinder
@EnableAutoConfiguration
@Import(OrderNotifyFunctionConfig.class)
@SpringBootTest(properties = {
        "spring.cloud.function.definition=orderNotifyFunction",
        "spring.cloud.stream.bindings.orderNotifyFunction-in-0.destination=order.created",
        "spring.cloud.stream.bindings.orderNotifyFunction-out-0.destination=order.notify",
        "spring.cloud.stream.bindings.orderNotifyFunction-in-0.contentType=application/json",
        "spring.cloud.stream.bindings.orderNotifyFunction-out-0.contentType=application/json"
})
class OrderNotifyFunctionIntegrationTest {

    @Autowired
    private InputDestination inputDestination;

    @Autowired
    private OutputDestination outputDestination;

    /**
     * 测试订单创建事件通过函数转换为通知事件
     */
    @Test
    void shouldTransformOrderCreatedToOrderNotify() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                "MSG_10001",
                "ORDER_CREATED",
                "ORDER_10001",
                "USER_90001",
                BigDecimal.valueOf(199.90),
                LocalDateTime.now(),
                "integration-test"
        );

        Message<String> inputMessage = MessageBuilder.withPayload(JSONUtil.toJsonStr(event))
                .setHeader("contentType", MimeTypeUtils.APPLICATION_JSON_VALUE)
                .setHeader("messageId", event.messageId())
                .setHeader("businessKey", event.orderId())
                .build();

        inputDestination.send(inputMessage, "order.created");

        Message<byte[]> outputMessage = outputDestination.receive(3000, "order.notify");
        assertThat(outputMessage).isNotNull();

        String json = new String(outputMessage.getPayload(), StandardCharsets.UTF_8);
        OrderNotifyEvent notifyEvent = JSONUtil.toBean(json, OrderNotifyEvent.class);

        assertThat(notifyEvent.orderId()).isEqualTo("ORDER_10001");
        assertThat(notifyEvent.userId()).isEqualTo("USER_90001");
        assertThat(notifyEvent.eventType()).isEqualTo("ORDER_NOTIFY");
    }
}
```

集成测试建议覆盖以下内容：

| 测试项        | 验证目标                                               |
| ------------- | ------------------------------------------------------ |
| 输入 Binding  | 消息能否进入目标函数                                   |
| 输出 Binding  | 函数输出是否能被正确接收                               |
| `contentType` | JSON 是否能正确转换为 Java 对象                        |
| 消息头        | `messageId`、`businessKey` 是否保留                    |
| 多函数配置    | `spring.cloud.function.definition` 是否正确            |
| Binding 名称  | `<functionName>-in-0`、`<functionName>-out-0` 是否匹配 |

如果要验证真实 RabbitMQ 或 Kafka 行为，例如 Exchange、Queue、DLQ、Kafka Offset、分区顺序，需要使用真实中间件集成测试，Test Binder 不能替代所有 Broker 特性。

### 异常场景测试

异常场景测试重点验证消费失败时异常是否向外传播，避免业务代码吞掉异常后导致框架误判为消费成功。Spring Cloud Stream 的错误处理依赖函数抛出异常；重试和死信队列通常由 Binder 接管。([Home](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream.html?utm_source=chatgpt.com))

文件位置：`src/test/java/io/github/atengk/stream/function/OrderCreatedConsumerExceptionTest.java`

下面的测试类验证消费者在业务处理失败时会抛出异常。

```java
package io.github.atengk.stream.function;

import io.github.atengk.stream.event.OrderCreatedEvent;
import io.github.atengk.stream.service.OrderConsumeService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 订单创建消费者异常测试
 *
 * @author Ateng
 * @since 2026-05-05
 */
class OrderCreatedConsumerExceptionTest {

    /**
     * 测试业务异常会继续向外抛出
     */
    @Test
    void shouldThrowExceptionWhenBusinessFailed() {
        OrderConsumeService orderConsumeService = event -> {
            throw new IllegalStateException("模拟订单消费失败");
        };

        OrderCreatedConsumerConfig config = new OrderCreatedConsumerConfig(orderConsumeService);
        Consumer<Message<OrderCreatedEvent>> consumer = config.orderCreatedConsumer();

        OrderCreatedEvent event = new OrderCreatedEvent(
                "MSG_ERROR_10001",
                "ORDER_CREATED",
                "ORDER_ERROR",
                "USER_90001",
                BigDecimal.valueOf(199.90),
                LocalDateTime.now(),
                "exception-test"
        );

        Message<OrderCreatedEvent> message = MessageBuilder.withPayload(event)
                .setHeader("messageId", event.messageId())
                .setHeader("businessKey", event.orderId())
                .build();

        assertThatThrownBy(() -> consumer.accept(message))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("模拟订单消费失败");
    }

    /**
     * 测试空消息体会抛出参数异常
     */
    @Test
    void shouldThrowExceptionWhenPayloadIsNull() {
        OrderConsumeService orderConsumeService = event -> {
        };

        OrderCreatedConsumerConfig config = new OrderCreatedConsumerConfig(orderConsumeService);
        Consumer<Message<OrderCreatedEvent>> consumer = config.orderCreatedConsumer();

        Message<OrderCreatedEvent> message = MessageBuilder.withPayload(null)
                .setHeader("messageId", "MSG_EMPTY")
                .build();

        assertThatThrownBy(() -> consumer.accept(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("订单创建事件无效");
    }
}
```

异常场景建议补充真实中间件验证：

```text
1. 启动 RabbitMQ。
2. 启动应用并开启 autoBindDlq。
3. 发送 orderId=ORDER_ERROR 的测试消息。
4. 消费者主动抛出异常。
5. 观察日志中是否出现 maxAttempts 次重试。
6. 观察 DLQ 中是否出现失败消息。
```

RabbitMQ 异常测试配置示例：

```yaml
spring:
  cloud:
    stream:
      bindings:
        orderCreatedConsumer-in-0:
          destination: order.created
          group: order-service
          contentType: application/json
          consumer:
            # 本地测试时降低重试次数，避免测试等待过久
            maxAttempts: 2
            backOffInitialInterval: 500
            backOffMaxInterval: 1000

      rabbit:
        bindings:
          orderCreatedConsumer-in-0:
            consumer:
              # 开启死信队列
              autoBindDlq: true
              # 失败消息附加异常信息后重新发布到 DLQ
              republishToDlq: true
              # 不重新入队，避免异常消息反复打满消费者
              requeueRejected: false
```

异常测试的目标不是证明“不会失败”，而是证明失败后消息不会静默丢失，并且可以通过日志、DLQ 和业务主键定位。

### 消息幂等测试

消息幂等测试用于验证同一条消息被重复投递时，业务不会重复执行。幂等测试可以分为 Redis 方案、数据库唯一索引方案和业务状态机方案。这里以 `MessageIdempotentService` 为例，验证重复 `messageId` 只能成功锁定一次。

文件位置：`src/test/java/io/github/atengk/stream/service/MessageIdempotentServiceTest.java`

下面的测试类用内存实现模拟幂等服务，验证消费逻辑是否能跳过重复消息。

```java
package io.github.atengk.stream.service;

import cn.hutool.core.collection.ConcurrentHashSet;
import io.github.atengk.stream.event.OrderCreatedEvent;
import io.github.atengk.stream.service.impl.OrderConsumeServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 消息幂等测试
 *
 * @author Ateng
 * @since 2026-05-05
 */
class MessageIdempotentServiceTest {

    /**
     * 测试重复消息只会被处理一次
     */
    @Test
    void shouldConsumeSameMessageOnlyOnce() {
        MemoryMessageIdempotentService idempotentService = new MemoryMessageIdempotentService();
        OrderConsumeServiceImpl consumeService = new OrderConsumeServiceImpl(idempotentService);

        OrderCreatedEvent event = new OrderCreatedEvent(
                "MSG_REPEAT_10001",
                "ORDER_CREATED",
                "ORDER_10001",
                "USER_90001",
                BigDecimal.valueOf(199.90),
                LocalDateTime.now(),
                "idempotent-test"
        );

        consumeService.handleOrderCreated(event);
        consumeService.handleOrderCreated(event);

        assertThat(idempotentService.consumedCount()).isEqualTo(1);
    }

    /**
     * 内存消息幂等服务，仅用于单元测试
     *
     * @author Ateng
     * @since 2026-05-05
     */
    static class MemoryMessageIdempotentService implements MessageIdempotentService {

        private final Set<String> processing = new ConcurrentHashSet<>();
        private final Set<String> consumed = new ConcurrentHashSet<>();

        /**
         * 尝试锁定消息
         *
         * @param messageId 消息 ID
         * @param ttl       锁定时间
         * @return 是否锁定成功
         */
        @Override
        public boolean tryLock(String messageId, Duration ttl) {
            if (consumed.contains(messageId)) {
                return false;
            }
            return processing.add(messageId);
        }

        /**
         * 标记消息已消费
         *
         * @param messageId 消息 ID
         * @param ttl       保留时间
         */
        @Override
        public void markConsumed(String messageId, Duration ttl) {
            processing.remove(messageId);
            consumed.add(messageId);
        }

        /**
         * 释放消息锁定
         *
         * @param messageId 消息 ID
         */
        @Override
        public void release(String messageId) {
            processing.remove(messageId);
        }

        /**
         * 获取已消费数量
         *
         * @return 已消费数量
         */
        int consumedCount() {
            return consumed.size();
        }
    }
}
```

幂等测试建议至少覆盖以下场景：

| 测试场景                  | 预期结果                       |
| ------------------------- | ------------------------------ |
| 首次消费                  | 正常处理，标记已消费           |
| 同一 `messageId` 重复消费 | 直接跳过，不重复执行业务       |
| 处理失败后重试            | 释放处理中标记，允许下次重试   |
| 已消费后死信重放          | 识别为已消费，直接跳过         |
| 不同消费组消费同一消息    | 各消费组独立处理               |
| 相同业务键不同消息        | 根据业务状态机判断是否允许处理 |

对于订单、支付、库存等核心业务，幂等测试不能只测 Redis Key，还要测试业务状态机。例如订单已经是 `PAID` 时，再次收到 `ORDER_PAID` 消息应该跳过，而不是重复扣减库存或重复发放积分。

## 部署配置

部署配置需要保证同一份应用镜像可以在开发、测试、预发和生产环境中复用，通过外部配置决定 Binder、Broker 地址、消费组、并发数、重试、DLQ、日志级别等运行参数。Spring Boot 支持外部化配置，可以通过 YAML、Properties、环境变量、命令行参数等方式覆盖应用配置；后加载的配置源可以覆盖先加载的配置源。([Spring Enterprise Docs](https://docs.enterprise.spring.io/spring-boot/reference/features/external-config.html?utm_source=chatgpt.com))

### 多环境配置

多环境配置建议分为公共配置和环境配置。公共配置放在 `application.yml`，环境差异放在 `application-dev.yml`、`application-test.yml`、`application-prod.yml`。Binding 通用属性使用 `spring.cloud.stream.bindings.<bindingName>.<property>`，公共默认值可以使用 `spring.cloud.stream.default.*` 减少重复配置。([Home](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/binding-properties.html?utm_source=chatgpt.com))

文件结构建议如下：

```text
src/main/resources/
├── application.yml
├── application-dev.yml
├── application-test.yml
└── application-prod.yml
```

文件位置：`src/main/resources/application.yml`

公共配置只放所有环境一致的内容。

```yaml
server:
  port: 18080

spring:
  application:
    # 应用名称，建议和部署服务名保持一致
    name: spring-cloud-stream-demo

  cloud:
    stream:
      # 所有 Binding 默认使用 JSON
      default:
        contentType: application/json

      # 预创建业务发送使用的输出 Binding
      output-bindings: orderCreated-out-0

      bindings:
        orderCreated-out-0:
          # 生产者输出 Binding
          destination: order.created
          contentType: application/json

        orderCreatedConsumer-in-0:
          # 消费者输入 Binding
          destination: order.created
          group: order-service
          contentType: application/json

management:
  endpoints:
    web:
      exposure:
        # 生产环境可按需收敛
        include: health,info,metrics

logging:
  level:
    io.github.atengk: info
```

文件位置：`src/main/resources/application-dev.yml`

开发环境配置允许自动创建资源，并开启更详细日志。

```yaml
spring:
  cloud:
    function:
      # 开发环境启用订单消费者
      definition: orderCreatedConsumer

    stream:
      defaultBinder: rabbit

      bindings:
        orderCreatedConsumer-in-0:
          consumer:
            # 开发环境降低并发，方便观察日志
            concurrency: 1
            maxAttempts: 2

      rabbit:
        bindings:
          orderCreatedConsumer-in-0:
            consumer:
              # 开发环境自动创建 DLQ
              autoBindDlq: true
              republishToDlq: true
              requeueRejected: false

  rabbitmq:
    host: localhost
    port: 5672
    username: ateng
    password: 123456

logging:
  level:
    io.github.atengk: debug
    org.springframework.cloud.stream: info
```

文件位置：`src/main/resources/application-prod.yml`

生产环境配置避免硬编码账号密码，统一通过环境变量注入。

```yaml
spring:
  cloud:
    function:
      definition: orderCreatedConsumer

    stream:
      defaultBinder: rabbit

      bindings:
        orderCreatedConsumer-in-0:
          consumer:
            # 生产环境根据业务处理能力调整并发
            concurrency: ${STREAM_ORDER_CONSUMER_CONCURRENCY:4}
            maxAttempts: ${STREAM_ORDER_CONSUMER_MAX_ATTEMPTS:3}
            backOffInitialInterval: ${STREAM_ORDER_BACKOFF_INITIAL:1000}
            backOffMaxInterval: ${STREAM_ORDER_BACKOFF_MAX:10000}
            backOffMultiplier: ${STREAM_ORDER_BACKOFF_MULTIPLIER:2.0}

      rabbit:
        bindings:
          orderCreatedConsumer-in-0:
            consumer:
              autoBindDlq: ${STREAM_RABBIT_AUTO_BIND_DLQ:true}
              republishToDlq: true
              requeueRejected: false
              deadLetterExchange: order.created.dlx
              deadLetterQueueName: order.created.order-service.dlq
              deadLetterRoutingKey: order.created.dlq

  rabbitmq:
    host: ${RABBITMQ_HOST}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}
    virtual-host: ${RABBITMQ_VIRTUAL_HOST:/}
    connection-timeout: 10s

logging:
  level:
    io.github.atengk: ${LOG_LEVEL_APP:info}
    org.springframework.cloud.stream: ${LOG_LEVEL_STREAM:info}
```

生产环境配置原则如下：

| 配置项       | 建议                        |
| ------------ | --------------------------- |
| Broker 地址  | 环境变量或配置中心注入      |
| 用户名密码   | Secret 注入，不写入镜像     |
| 消费组       | 固定，不随 Pod 名称变化     |
| 并发数       | 通过环境变量控制            |
| DLQ          | 生产必须启用，并配套告警    |
| 自动创建资源 | 生产建议由运维或 IaC 管理   |
| 日志级别     | 默认 `info`，排障时临时调整 |

### Docker 部署

Docker 部署建议采用“先打包 Jar，再构建镜像”的方式。Spring Boot Maven 插件也支持通过 Cloud Native Buildpacks 构建 OCI 镜像，执行 `mvn spring-boot:build-image` 即可生成镜像；该目标需要可用的 Docker Daemon。([Home](https://docs.spring.io/spring-boot/maven-plugin/build-image.html?utm_source=chatgpt.com))

如果团队习惯使用 Dockerfile，可以使用下面的多阶段构建。

文件位置：`Dockerfile`

```dockerfile
# 构建阶段：使用 Maven 和 JDK 17 编译项目
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 先复制 pom.xml，利用 Docker 缓存下载依赖
COPY pom.xml .

# 下载依赖，减少后续源码变更导致的重复下载
RUN mvn -B dependency:go-offline

# 复制源码并打包
COPY src ./src
RUN mvn -B clean package -DskipTests

# 运行阶段：使用更小的 JRE 镜像运行应用
FROM eclipse-temurin:17-jre

WORKDIR /app

# 创建非 root 用户，降低容器运行风险
RUN useradd -r -u 10001 appuser

# 复制构建好的应用 Jar
COPY --from=builder /app/target/spring-cloud-stream-demo-1.0.0.jar /app/app.jar

# 暴露应用端口
EXPOSE 18080

# 设置 JVM 参数，可通过环境变量覆盖
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# 使用非 root 用户运行
USER appuser

# 启动应用，Profile 通过 SPRING_PROFILES_ACTIVE 注入
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
```

构建镜像：

```bash
docker build -t spring-cloud-stream-demo:1.0.0 .
```

使用本地 RabbitMQ 启动应用容器：

```bash
docker run -d \
  --name spring-cloud-stream-demo \
  --network host \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e RABBITMQ_HOST=localhost \
  -e RABBITMQ_PORT=5672 \
  -e RABBITMQ_USERNAME=ateng \
  -e RABBITMQ_PASSWORD=123456 \
  -e STREAM_ORDER_CONSUMER_CONCURRENCY=2 \
  spring-cloud-stream-demo:1.0.0
```

如果不使用 `--network host`，建议通过 Docker Compose 把应用和 RabbitMQ 放到同一个网络中。

文件位置：`docker-compose.yml`

```yaml
services:
  rabbitmq:
    image: rabbitmq:3-management
    container_name: ateng-rabbitmq
    restart: unless-stopped
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      # RabbitMQ 本地账号
      RABBITMQ_DEFAULT_USER: ateng
      # RabbitMQ 本地密码
      RABBITMQ_DEFAULT_PASS: 123456
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq

  stream-demo:
    image: spring-cloud-stream-demo:1.0.0
    container_name: spring-cloud-stream-demo
    restart: unless-stopped
    depends_on:
      - rabbitmq
    ports:
      - "18080:18080"
    environment:
      # 激活生产配置
      SPRING_PROFILES_ACTIVE: prod
      # RabbitMQ 地址使用服务名
      RABBITMQ_HOST: rabbitmq
      RABBITMQ_PORT: 5672
      RABBITMQ_USERNAME: ateng
      RABBITMQ_PASSWORD: 123456
      # 消费并发数
      STREAM_ORDER_CONSUMER_CONCURRENCY: 2
      # JVM 参数
      JAVA_OPTS: "-Xms256m -Xmx512m"

volumes:
  rabbitmq_data:
```

启动：

```bash
docker compose up -d
docker compose logs -f stream-demo
```

验证：

```bash
curl -X POST "http://localhost:18080/api/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORDER_DOCKER_10001",
    "userId": "USER_90001",
    "amount": 199.90
  }'
```

Docker 部署时，镜像内不应写死 RabbitMQ、Kafka、Redis、数据库等环境参数。镜像只包含应用代码和默认配置，环境差异通过环境变量、Compose、Kubernetes ConfigMap 或 Secret 注入。

### Kubernetes 部署

Kubernetes 部署建议把应用配置分成三类：非敏感配置放 `ConfigMap`，敏感配置放 `Secret`，容器运行参数放 `Deployment`。Spring Boot 支持通过环境变量覆盖配置，因此 Kubernetes 中可以直接使用环境变量注入 Spring 配置。([Spring Enterprise Docs](https://docs.enterprise.spring.io/spring-boot/reference/features/external-config.html?utm_source=chatgpt.com))

文件位置：`k8s/configmap.yaml`

下面的 ConfigMap 保存非敏感配置。

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: stream-demo-config
  namespace: default
data:
  # 激活生产环境配置
  SPRING_PROFILES_ACTIVE: "prod"
  # RabbitMQ 服务地址
  RABBITMQ_HOST: "rabbitmq.default.svc.cluster.local"
  # RabbitMQ 端口
  RABBITMQ_PORT: "5672"
  # 消费并发数
  STREAM_ORDER_CONSUMER_CONCURRENCY: "4"
  # 消费最大重试次数
  STREAM_ORDER_CONSUMER_MAX_ATTEMPTS: "3"
  # 应用日志级别
  LOG_LEVEL_APP: "info"
```

文件位置：`k8s/secret.yaml`

下面的 Secret 保存 RabbitMQ 用户名和密码。示例使用 `stringData`，实际落库后 Kubernetes 会转换为 base64。

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: stream-demo-secret
  namespace: default
type: Opaque
stringData:
  # RabbitMQ 用户名
  RABBITMQ_USERNAME: "ateng"
  # RabbitMQ 密码
  RABBITMQ_PASSWORD: "123456"
```

文件位置：`k8s/deployment.yaml`

下面的 Deployment 部署 Spring Cloud Stream 应用，并配置健康检查、资源限制和滚动更新。

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: spring-cloud-stream-demo
  namespace: default
  labels:
    app: spring-cloud-stream-demo
spec:
  # 多副本部署，同一消费组内竞争消费
  replicas: 3
  selector:
    matchLabels:
      app: spring-cloud-stream-demo
  strategy:
    type: RollingUpdate
    rollingUpdate:
      # 滚动更新时最多新增 1 个 Pod
      maxSurge: 1
      # 滚动更新时最多不可用 1 个 Pod
      maxUnavailable: 1
  template:
    metadata:
      labels:
        app: spring-cloud-stream-demo
    spec:
      containers:
        - name: app
          image: spring-cloud-stream-demo:1.0.0
          imagePullPolicy: IfNotPresent
          ports:
            - name: http
              containerPort: 18080

          envFrom:
            # 注入非敏感配置
            - configMapRef:
                name: stream-demo-config
            # 注入敏感配置
            - secretRef:
                name: stream-demo-secret

          env:
            # JVM 参数
            - name: JAVA_OPTS
              value: "-Xms512m -Xmx512m"

          resources:
            requests:
              # CPU 最小申请
              cpu: "500m"
              # 内存最小申请
              memory: "768Mi"
            limits:
              # CPU 最大限制
              cpu: "1000m"
              # 内存最大限制
              memory: "1024Mi"

          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 18080
            initialDelaySeconds: 20
            periodSeconds: 10
            failureThreshold: 6

          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 18080
            initialDelaySeconds: 60
            periodSeconds: 20
            failureThreshold: 3
```

文件位置：`k8s/service.yaml`

下面的 Service 暴露应用 HTTP 接口。

```yaml
apiVersion: v1
kind: Service
metadata:
  name: spring-cloud-stream-demo
  namespace: default
  labels:
    app: spring-cloud-stream-demo
spec:
  type: ClusterIP
  selector:
    app: spring-cloud-stream-demo
  ports:
    - name: http
      port: 18080
      targetPort: 18080
```

部署命令：

```bash
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
```

查看部署状态：

```bash
kubectl get pods -l app=spring-cloud-stream-demo
kubectl logs -f deployment/spring-cloud-stream-demo
kubectl describe deployment spring-cloud-stream-demo
```

集群内验证接口：

```bash
kubectl port-forward service/spring-cloud-stream-demo 18080:18080
curl -X POST "http://localhost:18080/api/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORDER_K8S_10001",
    "userId": "USER_90001",
    "amount": 199.90
  }'
```

Kubernetes 多副本部署时，要特别注意消费组和并发数。多个 Pod 使用同一个 `group`，表示同组竞争消费；如果配置成不同 `group`，就会变成广播消费，可能导致同一业务消息被多个副本重复处理。

### 配置参数管理

配置参数管理的目标是让部署环境可以安全、可控地调整运行参数，而不需要重新构建镜像。Spring Cloud Stream 的 Binder、Binding、Consumer、Producer 都可以通过外部配置管理；Binder 服务属性中也包含 `defaultBinder`、`instanceCount`、`instanceIndex`、`dynamicDestinations`、`bindingRetryInterval` 等运行参数。([Home](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/binding-service-properties.html?utm_source=chatgpt.com))

建议把配置参数分为以下几类：

| 类型            | 示例                         | 管理方式             |
| --------------- | ---------------------------- | -------------------- |
| 应用基础参数    | 端口、Profile、日志级别      | ConfigMap / 环境变量 |
| Binder 连接参数 | RabbitMQ/Kafka 地址          | ConfigMap            |
| 敏感参数        | 用户名、密码、Token          | Secret               |
| Binding 参数    | Destination、Group           | ConfigMap / 配置中心 |
| 消费能力参数    | `concurrency`、`maxAttempts` | ConfigMap            |
| 可靠性参数      | DLQ、重试、确认机制          | ConfigMap            |
| JVM 参数        | `-Xms`、`-Xmx`、GC 参数      | 环境变量             |
| 动态路由白名单  | `dynamicDestinations`        | 配置中心或环境变量   |

环境变量命名建议：

```text
SPRING_PROFILES_ACTIVE=prod
RABBITMQ_HOST=rabbitmq.default.svc.cluster.local
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=ateng
RABBITMQ_PASSWORD=******
STREAM_ORDER_CONSUMER_CONCURRENCY=4
STREAM_ORDER_CONSUMER_MAX_ATTEMPTS=3
STREAM_RABBIT_AUTO_BIND_DLQ=true
LOG_LEVEL_APP=info
JAVA_OPTS=-Xms512m -Xmx512m
```

配置参数管理建议遵守以下原则：

第一，镜像不可变。不要因为环境不同构建不同镜像，应使用同一个镜像搭配不同配置。

第二，敏感信息不进入 Git 仓库。RabbitMQ、Kafka、Redis、数据库的账号密码应通过 Secret 或安全配置中心注入。

第三，生产环境消费组固定。`group` 不应包含 Pod 名、随机数或时间戳，否则会导致每次发布都形成新消费组，出现重复消费。

第四，关键 Destination 固定。订单、支付、库存等核心消息目标不应通过动态参数随意修改，防止误投递。

第五，调整消费并发要配合 Broker 能力。Kafka 消费并发受 Topic 分区数限制；RabbitMQ 消费并发受消费者线程、队列堆积、业务耗时和数据库压力影响。

第六，所有 DLQ/DLT 必须纳入监控。仅开启死信队列不等于解决问题，必须有告警、排查、重放和归档流程。

生产配置上线前建议检查：

```text
1. SPRING_PROFILES_ACTIVE 是否为 prod。
2. RabbitMQ/Kafka 地址是否指向生产 Broker。
3. 消费组 group 是否固定且符合命名规范。
4. maxAttempts、backoff、DLQ 是否已配置。
5. 消费端幂等是否启用。
6. 日志是否包含 messageId、eventType、businessKey。
7. Actuator 健康检查是否可用。
8. Kubernetes Secret 是否替代明文密码。
9. 多副本部署是否符合预期消费模型。
10. DLQ/DLT 是否接入监控和告警。
```

至此，Spring Boot 3 + Spring Cloud Stream 的基础开发大纲已经覆盖从概念、环境、配置、生产者、消费者、函数式模型、常用场景、异常处理、可靠性、业务集成、本地验证，到测试和部署的完整主线。