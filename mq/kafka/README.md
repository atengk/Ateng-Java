# Kafka 开发使用文档

本文档用于说明 Kafka 在 Java / Spring Boot 项目中的常见开发使用方式，重点覆盖基础概念、环境准备、依赖配置、消息生产、消息消费、序列化、消费位点管理、常用开发场景、测试验证以及生产使用建议。

文档内容面向后端开发人员，目标是帮助开发者在 Spring Boot 3 项目中快速接入 Kafka，并理解 Kafka 在实际业务系统中的核心使用方式和注意事项。

## 文档概述

本章节用于说明文档的适用范围、使用前提和技术环境。Kafka 属于分布式消息系统，在业务系统中通常用于异步解耦、削峰填谷、日志采集、事件驱动和数据同步等场景。

在阅读后续章节前，需要先明确 Kafka 的定位：它不是普通的任务队列，而是基于 Topic 和 Partition 组织消息、通过 Consumer Group 实现并行消费和消费位点管理的分布式消息平台。

### 适用场景

Kafka 适合用于高吞吐、可扩展、可持久化的消息处理场景。它通常出现在多个系统之间需要异步通信，或者单个系统内部需要将耗时操作从主业务流程中拆分出来的场景。

常见适用场景如下：

| 场景     | 说明                                                         |
| -------- | ------------------------------------------------------------ |
| 异步解耦 | 下单、支付、通知、积分、库存等业务之间通过消息解耦，降低系统间直接依赖 |
| 削峰填谷 | 高并发请求先写入 Kafka，由消费者按自身处理能力逐步消费       |
| 事件驱动 | 用户注册、订单创建、支付完成等业务事件通过 Kafka 分发给多个下游系统 |
| 日志采集 | 应用日志、访问日志、行为日志统一写入 Kafka，再进入日志平台或数据仓库 |
| 数据同步 | 将业务系统中的变更事件同步给搜索、推荐、风控、报表等系统     |
| 流式处理 | 与 Flink、Spark Streaming、Kafka Streams 等组件配合处理实时数据 |

Kafka 不适合对单条消息强事务一致性要求极高、消息量很小且系统复杂度需要尽量降低的场景。对于简单的延迟任务、轻量级异步处理或强业务事务队列，可以根据项目情况评估 RabbitMQ、Redis Stream、数据库任务表等方案。

### 技术环境

本文档默认基于 Java 后端项目进行说明，开发环境以 Spring Boot 3 和 JDK 21 为基础。后续示例会围绕 Spring Kafka 的常见用法展开。

推荐技术环境如下：

| 组件         | 推荐版本                | 说明                                                         |
| ------------ | ----------------------- | ------------------------------------------------------------ |
| JDK          | 21                      | Spring Boot 3 支持的长期支持版本，适合作为新项目默认 Java 版本 |
| Spring Boot  | 3.x                     | 后续示例默认使用 Spring Boot 3 项目结构                      |
| Spring Kafka | 与 Spring Boot 版本匹配 | 建议通过 Spring Boot 依赖管理统一控制版本                    |
| Kafka Server | 3.x                     | 本地开发、测试环境和生产环境建议保持大版本一致               |
| Maven        | 3.8+                    | 用于项目依赖管理和构建                                       |
| Docker       | 可选                    | 本地开发环境可通过 Docker 快速启动 Kafka                     |

实际项目中需要重点关注 Spring Boot、Spring Kafka 和 Kafka Server 的版本兼容性。通常建议优先使用 Spring Boot 官方依赖管理提供的 Spring Kafka 版本，避免手动指定不兼容版本。

## Kafka 基础概念

本章节用于说明 Kafka 开发中必须掌握的核心概念。理解这些概念后，再阅读消息发送、消息消费、序列化和消费位点管理等章节会更加清晰。

Kafka 的核心模型可以简单理解为：Producer 将消息发送到 Topic，Topic 按 Partition 存储消息，Consumer 按 Consumer Group 订阅 Topic 并消费消息，消费进度通过 Offset 记录。

### Topic 与 Partition

Topic 是 Kafka 中消息的逻辑分类。业务系统发送消息时，通常会根据业务类型创建不同的 Topic，例如订单事件 Topic、用户事件 Topic、支付事件 Topic、日志采集 Topic 等。

Partition 是 Topic 下的物理分片。一个 Topic 可以包含多个 Partition，每个 Partition 内部的消息是有序追加写入的。Kafka 通过 Partition 实现并行写入、并行消费和水平扩展。

可以将 Topic 和 Partition 理解为如下关系：

```text
Topic: order-event-topic
 ├── Partition 0
 ├── Partition 1
 └── Partition 2
```

Topic 用来区分业务消息类型，Partition 用来提升吞吐能力和并发能力。

Kafka 只保证同一个 Partition 内的消息有序，不保证整个 Topic 全局有序。如果业务要求同一类数据严格顺序消费，例如同一个订单的状态变更必须按顺序处理，就需要在发送消息时使用相同的 Key，使同一个 Key 的消息尽量进入同一个 Partition。

常见设计建议如下：

| 设计点         | 建议                                                         |
| -------------- | ------------------------------------------------------------ |
| Topic 命名     | 使用业务语义命名，例如 `order-event-topic`、`user-event-topic` |
| Partition 数量 | 根据吞吐量、消费者并发度和后续扩容空间规划                   |
| 消息顺序       | 只依赖同一 Partition 内的顺序，不依赖 Topic 全局顺序         |
| 消息 Key       | 有顺序要求的数据建议使用业务主键作为 Key，例如订单 ID、用户 ID |
| Topic 拆分     | 不同业务语义、不同消费模型、不同保留策略的消息建议拆分 Topic |

Partition 数量并不是越多越好。Partition 过少会限制并发能力，Partition 过多会增加 Broker、Controller、Consumer Rebalance 和文件句柄等方面的开销。生产环境需要结合业务吞吐量、Broker 数量和消费者并发能力综合规划。

### Producer 与 Consumer

Producer 是消息生产者，负责将业务消息发送到 Kafka。常见的 Producer 来源包括订单服务、用户服务、支付服务、日志采集服务等。

Consumer 是消息消费者，负责从 Kafka 中拉取消息并执行业务处理。常见的 Consumer 用途包括发送通知、更新报表、同步搜索索引、执行风控规则、写入数据仓库等。

Producer 和 Consumer 的基本关系如下：

```text
业务系统 Producer
        │
        ▼
Kafka Topic
        │
        ▼
业务系统 Consumer
```

Producer 发送消息时，通常需要指定 Topic，也可以指定 Key、Partition、Header 等信息。Kafka 会根据发送配置、消息 Key 和分区策略决定消息最终写入哪个 Partition。

Consumer 消费消息时，通常订阅一个或多个 Topic。Kafka 会把 Topic 下的 Partition 分配给 Consumer Group 中的 Consumer 实例，由这些实例共同完成消费。

Producer 开发时需要重点关注以下内容：

| 关注点       | 说明                                                |
| ------------ | --------------------------------------------------- |
| 发送可靠性   | 是否需要等待 Broker 确认消息写入成功                |
| Key 设计     | 是否需要通过 Key 保证同类业务消息进入同一 Partition |
| 序列化方式   | 字符串、JSON、Avro、Protobuf 或自定义对象           |
| 发送结果处理 | 是否需要记录发送成功、失败、异常重试等日志          |
| 幂等与重试   | 发送失败后是否允许重试，重试是否可能造成重复消息    |

Consumer 开发时需要重点关注以下内容：

| 关注点      | 说明                                     |
| ----------- | ---------------------------------------- |
| 消费并发    | Consumer 实例数和 Partition 数量是否匹配 |
| 消费异常    | 业务异常、反序列化异常、网络异常如何处理 |
| Offset 提交 | 使用自动提交还是手动提交                 |
| 幂等处理    | 重复消费时业务是否能够安全处理           |
| 消费耗时    | 单条消息处理过慢是否会影响整体消费进度   |

在实际开发中，Producer 侧通常重点关注“消息能否可靠写入 Kafka”，Consumer 侧通常重点关注“消息能否稳定、幂等、可观测地处理完成”。

### Consumer Group

Consumer Group 是 Kafka 实现水平扩展和负载均衡消费的核心机制。多个 Consumer 使用相同的 Group ID 订阅同一个 Topic 时，它们属于同一个 Consumer Group。

同一个 Consumer Group 内，同一个 Partition 在同一时刻只会分配给一个 Consumer 实例消费。因此，Consumer Group 内的多个 Consumer 可以并行消费一个 Topic 的多个 Partition，但不会重复消费同一个 Partition。

示例关系如下：

```text
Topic: order-event-topic
 ├── Partition 0  -> Consumer A
 ├── Partition 1  -> Consumer B
 └── Partition 2  -> Consumer C

Consumer Group: order-service-group
```

如果 Consumer 数量大于 Partition 数量，多出来的 Consumer 实例会处于空闲状态，因为一个 Partition 不能在同一个 Consumer Group 内同时分配给多个 Consumer。

```text
Topic: order-event-topic
 ├── Partition 0  -> Consumer A
 ├── Partition 1  -> Consumer B
 └── Partition 2  -> Consumer C

Consumer D 空闲
Consumer Group: order-service-group
```

如果多个不同的业务系统都需要消费同一个 Topic，可以使用不同的 Consumer Group。不同 Consumer Group 之间互不影响，每个 Group 都会维护自己的消费进度。

```text
Topic: order-event-topic
 ├── order-service-group
 ├── report-service-group
 └── search-service-group
```

这种机制适合事件驱动架构。例如订单创建事件可以同时被订单后置处理服务、报表服务、搜索服务、风控服务消费，每个服务使用独立的 Consumer Group。

Consumer Group 设计建议如下：

| 场景                         | 建议                                             |
| ---------------------------- | ------------------------------------------------ |
| 同一个业务服务多实例部署     | 使用相同的 Group ID，实现负载均衡消费            |
| 不同业务服务都要消费同一消息 | 使用不同的 Group ID，实现广播式业务消费          |
| 需要提升消费并发             | 增加 Partition 数量，并增加 Consumer 实例数      |
| 需要避免重复业务处理         | Consumer 侧做好幂等处理，不只依赖 Kafka 投递语义 |

Consumer Group 发生成员变化、Topic Partition 变化或 Consumer 长时间无响应时，Kafka 会触发 Rebalance。Rebalance 会重新分配 Partition，在此期间可能造成短暂消费暂停。因此，消费者业务逻辑应避免长时间阻塞，并合理配置消费超时和批量处理参数。

### Offset 与消息提交

Offset 是 Kafka 中消息在 Partition 内的位点。每个 Partition 内的消息都有一个递增的 Offset，用于标识消息位置。

Offset 只在 Partition 内有意义，不同 Partition 的 Offset 互不关联。例如 Partition 0 的 Offset 100 和 Partition 1 的 Offset 100 不是同一条消息，只是各自分区中的位置编号相同。

Consumer 消费消息后，需要提交 Offset，用于记录当前 Consumer Group 已经消费到哪个位置。下次 Consumer 重启或 Rebalance 后，会从已提交的 Offset 继续消费。

基本关系如下：

```text
Partition 0:
Offset 0 -> Offset 1 -> Offset 2 -> Offset 3 -> Offset 4
                         ↑
                 当前已提交 Offset
```

Offset 提交方式主要分为自动提交和手动提交。

| 提交方式 | 说明                                  | 适用场景                                   |
| -------- | ------------------------------------- | ------------------------------------------ |
| 自动提交 | Kafka 客户端按固定间隔自动提交 Offset | 简单消费场景，对重复消费和消息丢失不敏感   |
| 手动提交 | 业务处理成功后由代码主动提交 Offset   | 生产业务场景，便于控制消息处理完成后再确认 |
| 批量提交 | 一批消息处理完成后统一提交 Offset     | 高吞吐消费场景，需要平衡性能和可靠性       |

生产环境中更推荐使用手动提交 Offset。原因是自动提交可能在业务逻辑尚未真正处理完成时就提交 Offset，一旦服务异常退出，可能造成消息丢失风险。手动提交可以在业务处理成功后再提交 Offset，从而降低消息丢失概率。

不过，手动提交并不代表完全不会重复消费。如果 Consumer 在业务处理成功后、Offset 提交前宕机，消息仍可能被再次消费。因此，Kafka 消费端必须默认考虑重复消费问题，并通过业务唯一键、去重表、状态判断、幂等更新等方式保证业务安全。

常见 Offset 处理建议如下：

| 处理点       | 建议                                                |
| ------------ | --------------------------------------------------- |
| 是否自动提交 | 生产业务优先考虑关闭自动提交                        |
| 何时提交     | 业务处理成功后再提交                                |
| 异常处理     | 业务失败时不要盲目提交 Offset，应进入重试或死信处理 |
| 重复消费     | Consumer 侧必须设计幂等逻辑                         |
| 消息丢失     | 避免业务未完成时提前提交 Offset                     |
| 消息堆积     | 结合消费耗时、Partition 数量和 Consumer 并发度排查  |

Offset 管理是 Kafka 消费开发中最容易被忽略的部分。实际项目中，不能只关注“能否消费到消息”，还需要关注“处理失败怎么办”“是否会重复消费”“Offset 是否提交过早”“服务重启后从哪里继续消费”等问题。



## 环境准备

本章节用于说明 Kafka 开发前需要准备的本地环境、项目结构和 Kafka 服务。后续依赖配置、消息发送和消息消费示例都默认基于 JDK 21、Spring Boot 3 和本地 Kafka 服务展开。

### JDK 21 环境要求

Kafka 客户端本身可以运行在多个 Java 版本上，但本文档中的 Spring Boot 3 示例统一使用 JDK 21。JDK 21 是当前 Java 长期支持版本之一，适合作为新项目的默认运行环境。

开发环境需要确认本机已经正确安装 JDK 21，并且 `JAVA_HOME` 指向 JDK 21 安装目录。

```bash
java -version
javac -version
echo $JAVA_HOME
```

以上命令用于验证 Java 运行环境、Java 编译器和环境变量是否正确。`java -version` 应输出 `21` 相关版本信息，`javac -version` 应与运行时版本一致，`JAVA_HOME` 应指向 JDK 21 根目录。

如果是在 Linux 或 macOS 环境中配置 JDK 21，可以在 Shell 配置文件中加入如下内容。

文件位置：`~/.bashrc` 或 `~/.zshrc`

```bash
# JDK 21 安装目录，按实际路径调整
export JAVA_HOME=/usr/lib/jvm/jdk-21

# 将 JDK 命令加入 PATH
export PATH=$JAVA_HOME/bin:$PATH
```

修改完成后执行下面的命令使配置生效。

```bash
source ~/.bashrc

# 如果使用 zsh，则执行
source ~/.zshrc
```

在 Spring Boot 3 项目中，建议同时在 Maven 中指定 Java 版本，避免本地编译版本和运行版本不一致。

文件位置：`pom.xml`

```xml
<properties>
    <!-- 项目统一使用 JDK 21 编译和运行 -->
    <java.version>21</java.version>

    <!-- Maven 编译插件使用的源码版本 -->
    <maven.compiler.source>21</maven.compiler.source>

    <!-- Maven 编译插件生成的目标字节码版本 -->
    <maven.compiler.target>21</maven.compiler.target>

    <!-- 项目源码编码 -->
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

本地验证时，可以执行 Maven 编译命令。

```bash
mvn clean compile
```

该命令会清理旧的构建产物并重新编译项目。如果 JDK 版本不正确，通常会出现 `release version 21 not supported` 或相关编译错误，需要检查 Maven 使用的 JDK 是否与系统默认 JDK 一致。

### Spring Boot 3 项目结构

本文档后续示例默认基于标准 Spring Boot 3 后端项目结构。为了让 Kafka 相关代码清晰可维护，建议将消息生产、消息消费、消息模型和配置类按模块拆分。

推荐项目结构如下：

```text
kafka-demo
├── pom.xml
└── src
    ├── main
    │   ├── java
    │   │   └── io
    │   │       └── github
    │   │           └── atengk
    │   │               └── kafka
    │   │                   ├── KafkaDemoApplication.java
    │   │                   ├── config
    │   │                   │   ├── KafkaProducerConfig.java
    │   │                   │   └── KafkaConsumerConfig.java
    │   │                   ├── constant
    │   │                   │   └── KafkaTopicConstant.java
    │   │                   ├── consumer
    │   │                   │   └── OrderEventConsumer.java
    │   │                   ├── controller
    │   │                   │   └── KafkaMessageController.java
    │   │                   ├── dto
    │   │                   │   └── OrderEventMessage.java
    │   │                   └── producer
    │   │                       └── OrderEventProducer.java
    │   └── resources
    │       ├── application.yml
    │       └── logback-spring.xml
    └── test
        └── java
            └── io
                └── github
                    └── atengk
                        └── kafka
                            └── KafkaDemoApplicationTests.java
```

各目录职责建议如下：

| 路径                        | 说明                                               |
| --------------------------- | -------------------------------------------------- |
| `config`                    | Kafka Producer、Consumer、序列化、监听容器等配置类 |
| `constant`                  | Topic 名称、Consumer Group 名称等常量              |
| `producer`                  | 消息发送组件，封装 `KafkaTemplate`                 |
| `consumer`                  | 消息消费组件，存放 `@KafkaListener` 监听方法       |
| `dto`                       | Kafka 消息体对象，例如订单事件、用户事件、支付事件 |
| `controller`                | 本地测试接口或业务触发入口                         |
| `resources/application.yml` | Kafka 连接参数和生产消费配置                       |

项目入口类保持 Spring Boot 标准写法即可。

文件位置：`src/main/java/io/github/atengk/kafka/KafkaDemoApplication.java`

```java
package io.github.atengk.kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Kafka 示例项目启动类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootApplication
public class KafkaDemoApplication {

    /**
     * 启动 Kafka 示例项目。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(KafkaDemoApplication.class, args);
    }

}
```

Topic 和 Consumer Group 建议统一放在常量类中，避免在生产者和消费者代码中硬编码字符串。

文件位置：`src/main/java/io/github/atengk/kafka/constant/KafkaTopicConstant.java`

```java
package io.github.atengk.kafka.constant;

/**
 * Kafka Topic 常量
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class KafkaTopicConstant {

    /**
     * 订单事件 Topic
     */
    public static final String ORDER_EVENT_TOPIC = "order-event-topic";

    /**
     * 订单事件消费者组
     */
    public static final String ORDER_EVENT_GROUP = "order-event-group";

    private KafkaTopicConstant() {
    }

}
```

这种结构适合后续继续扩展多个 Topic、多个消费者组和多个业务消息类型。实际生产项目中，如果 Kafka 相关代码较多，也可以按业务域拆分，例如 `order.kafka`、`user.kafka`、`payment.kafka`。

### Kafka 服务准备

本地开发需要准备可访问的 Kafka 服务。开发环境可以使用 Docker Compose 快速启动单节点 Kafka；生产环境通常由运维或平台团队统一提供 Kafka 集群地址。

本地推荐使用 Kafka KRaft 模式启动，不再依赖 ZooKeeper。下面示例仅用于本地开发和功能验证，不建议直接作为生产配置。

文件位置：`docker-compose.yml`

```yaml
services:
  kafka:
    # 本地开发 Kafka 镜像，生产环境应使用公司统一镜像仓库和固定版本
    image: apache/kafka:3.8.1
    container_name: kafka-dev
    ports:
      # 宿主机访问 Kafka 的端口
      - "9092:9092"
    environment:
      # KRaft 模式下的节点 ID
      KAFKA_NODE_ID: 1

      # 同时作为 broker 和 controller
      KAFKA_PROCESS_ROLES: broker,controller

      # Controller 选举配置，单节点环境只配置自身
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093

      # 监听器配置，PLAINTEXT 用于客户端访问，CONTROLLER 用于内部控制器通信
      KAFKA_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093

      # 宿主机客户端连接地址，本地 Spring Boot 项目使用 127.0.0.1:9092
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://127.0.0.1:9092

      # 指定 Controller 使用的监听器名称
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER

      # 监听器安全协议映射
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT

      # Broker 间通信使用的监听器名称
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT

      # 单节点开发环境副本因子设置为 1
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

      # 事务状态日志副本因子，单节点开发环境设置为 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1

      # 事务状态日志最小同步副本数，单节点开发环境设置为 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
```

启动 Kafka 服务。

```bash
docker compose up -d
docker compose ps
docker logs -f kafka-dev
```

`docker compose up -d` 用于后台启动 Kafka，`docker compose ps` 用于查看容器状态，`docker logs -f kafka-dev` 用于查看 Kafka 启动日志。如果日志中没有持续报错，并且容器状态为 `running`，说明本地 Kafka 服务已启动。

可以进入容器创建测试 Topic。

```bash
docker exec -it kafka-dev bash

# 创建订单事件 Topic，分区数为 3，副本数为 1
/opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server 127.0.0.1:9092 \
  --create \
  --topic order-event-topic \
  --partitions 3 \
  --replication-factor 1

# 查看 Topic 列表
/opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server 127.0.0.1:9092 \
  --list

# 查看 Topic 详情
/opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server 127.0.0.1:9092 \
  --describe \
  --topic order-event-topic
```

以上命令中，`--partitions 3` 表示 Topic 有 3 个分区，适合本地验证 Consumer 并发消费；`--replication-factor 1` 表示副本数为 1，只适合单节点开发环境。生产环境副本数通常不应为 1，需要结合 Broker 数量和可用性要求规划。

也可以使用 Kafka 命令行工具进行发送和消费测试。

```bash
# 启动命令行生产者
/opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server 127.0.0.1:9092 \
  --topic order-event-topic

# 另开一个终端，启动命令行消费者
/opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server 127.0.0.1:9092 \
  --topic order-event-topic \
  --from-beginning
```

如果在生产者终端输入消息后，消费者终端可以看到对应内容，说明 Kafka 服务、Topic 和基础收发链路正常。

## 依赖与配置

本章节用于说明 Spring Boot 3 项目接入 Kafka 所需的 Maven 依赖和 `application.yml` 配置。后续消息生产、消息消费、序列化和 Offset 管理章节都基于本章节配置展开。

### Maven 依赖配置

Spring Boot 项目推荐使用 `spring-kafka` 进行 Kafka 客户端集成。版本建议交给 Spring Boot 依赖管理统一控制，不要随意手动指定 `spring-kafka` 和 `kafka-clients` 的版本，避免出现兼容性问题。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Web：用于提供本地测试接口，方便触发 Kafka 消息发送 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Kafka：提供 KafkaTemplate、@KafkaListener、监听容器等能力 -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>

    <!-- Validation：用于后续接口参数校验和消息对象校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Lombok：减少实体、DTO、构造方法和日志对象样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Hutool：常用工具类库，用于字符串、对象、JSON、集合等处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.32</version>
    </dependency>

    <!-- Spring Boot Test：用于单元测试和集成测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Spring Kafka Test：提供 Kafka 测试相关工具，适合集成测试场景 -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果是独立示例项目，可以使用 Spring Boot 父工程统一管理依赖版本。

文件位置：`pom.xml`

```xml
<parent>
    <!-- Spring Boot 父工程，用于统一管理 Spring 生态依赖版本 -->
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>

    <!-- 示例版本按项目实际 Spring Boot 3 版本调整 -->
    <version>3.3.6</version>

    <relativePath/>
</parent>
```

如果项目已经有公司统一父 POM 或 BOM，不建议重复引入 `spring-boot-starter-parent`。这种情况下只需要在当前模块加入 `spring-kafka` 依赖，并由父工程统一管理版本。

完成依赖配置后执行 Maven 命令验证依赖是否正常解析。

```bash
mvn clean dependency:tree
```

该命令会输出项目完整依赖树。可以重点检查 `spring-kafka`、`kafka-clients`、`spring-boot-starter-web` 是否被正确引入。

### Kafka 连接配置

Kafka 连接配置主要包括 Broker 地址、客户端 ID、序列化方式、反序列化方式和消费者组等。Spring Boot 会根据 `spring.kafka` 下的配置自动创建 Kafka 相关组件。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 本地测试服务端口
  port: 8080

spring:
  application:
    # 应用名称，用于日志、监控和客户端标识
    name: kafka-demo

  kafka:
    # Kafka Broker 地址，多个地址使用逗号分隔
    bootstrap-servers: 127.0.0.1:9092

    # Kafka 客户端通用属性
    properties:
      # 客户端请求超时时间
      request.timeout.ms: 30000

      # 元数据最大空闲时间，避免长时间不使用后元数据过期
      metadata.max.idle.ms: 300000

      # 单次拉取请求最大等待时间
      fetch.max.wait.ms: 500

    # 生产者配置
    producer:
      # 生产者客户端 ID，便于在 Kafka 日志和监控中定位来源
      client-id: kafka-demo-producer

      # 消息 Key 序列化方式
      key-serializer: org.apache.kafka.common.serialization.StringSerializer

      # 消息 Value 序列化方式，本文基础配置先使用字符串
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

    # 消费者配置
    consumer:
      # 默认消费者组，具体业务消费者也可以在 @KafkaListener 中单独指定
      group-id: order-event-group

      # 消息 Key 反序列化方式
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer

      # 消息 Value 反序列化方式，本文基础配置先使用字符串
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
```

如果连接的是生产 Kafka 集群，`bootstrap-servers` 应配置为内网域名或平台提供的 Broker 地址，例如：

```yaml
spring:
  kafka:
    # 生产环境示例，多个 Broker 地址可以提升客户端连接可用性
    bootstrap-servers: kafka-01.internal:9092,kafka-02.internal:9092,kafka-03.internal:9092
```

生产环境通常还会涉及认证、授权和加密传输，例如 SASL、SSL 或 SASL_SSL。本文基础开发文档先以无认证的本地开发环境为主，认证配置可以在后续生产部署章节单独展开。

### Producer 配置

Producer 配置决定消息发送的可靠性、吞吐量、重试行为和序列化方式。开发环境可以使用相对简单的配置，生产环境需要更关注确认机制、重试、幂等和批量发送参数。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  kafka:
    producer:
      # 生产者客户端 ID，建议按服务名区分
      client-id: kafka-demo-producer

      # Key 使用字符串序列化
      key-serializer: org.apache.kafka.common.serialization.StringSerializer

      # Value 使用字符串序列化，JSON 对象会在后续章节单独配置
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

      # 等待所有 ISR 副本确认后才认为发送成功，可靠性更高
      acks: all

      # 发送失败后的重试次数，适合处理短暂网络抖动或 Broker 临时不可用
      retries: 3

      # 批量发送大小，达到该大小或 linger 时间后触发发送
      batch-size: 16384

      # 生产者缓冲区大小，消息会先进入缓冲区再批量发送
      buffer-memory: 33554432

      # 单条消息最大请求大小，超过需要调整 Broker 和客户端配置
      properties:
        max.request.size: 1048576

        # 等待更多消息进入同一批次的时间，适当增加可提升吞吐
        linger.ms: 10

        # 开启生产者幂等，降低重试导致的重复写入风险
        enable.idempotence: true

        # 单个连接上未确认请求的最大数量，配合幂等生产者使用
        max.in.flight.requests.per.connection: 5

        # 消息压缩方式，可选 none、gzip、snappy、lz4、zstd
        compression.type: lz4
```

关键配置说明如下：

| 配置                 | 建议值          | 说明                               |
| -------------------- | --------------- | ---------------------------------- |
| `acks`               | `all`           | 等待所有同步副本确认，提高可靠性   |
| `retries`            | `3` 或更高      | 发送失败时自动重试，适合短暂故障   |
| `enable.idempotence` | `true`          | 开启幂等生产，降低重复写入风险     |
| `linger.ms`          | `5` 到 `20`     | 等待更多消息合并批量发送，提高吞吐 |
| `batch-size`         | 按消息大小调整  | 控制批量发送大小                   |
| `compression.type`   | `lz4` 或 `zstd` | 降低网络传输量，适合高吞吐场景     |

开发阶段可以先使用 `StringSerializer`，便于快速验证消息发送链路。后续处理 JSON 对象时，可以切换为 Spring Kafka 的 `JsonSerializer`，或者在业务代码中使用 Hutool 将对象转换为 JSON 字符串后再发送。

如果希望 Topic 在应用启动时自动创建，可以增加 Topic Bean 配置。但生产环境是否允许应用自动创建 Topic，需要遵循平台规范。

文件位置：`src/main/java/io/github/atengk/kafka/config/KafkaTopicConfig.java`

```java
package io.github.atengk.kafka.config;

import io.github.atengk.kafka.constant.KafkaTopicConstant;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Kafka Topic 配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
public class KafkaTopicConfig {

    /**
     * 创建订单事件 Topic。
     *
     * @return Topic 配置
     */
    @Bean
    public NewTopic orderEventTopic() {
        // 本地开发使用 3 个分区、1 个副本；生产环境应按集群规模调整副本数
        return new NewTopic(KafkaTopicConstant.ORDER_EVENT_TOPIC, 3, (short) 1);
    }

}
```

该配置适合本地开发和测试环境。生产环境中，Topic 通常由平台或运维提前创建，并统一配置分区数、副本数、保留时间、压缩策略和权限。

### Consumer 配置

Consumer 配置决定消息从哪里开始消费、是否自动提交 Offset、一次拉取多少消息、消费失败如何处理以及监听容器的并发度。生产业务中，Consumer 配置通常比 Producer 配置更需要谨慎。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  kafka:
    consumer:
      # 默认消费者组，业务消费者可以在 @KafkaListener 中覆盖
      group-id: order-event-group

      # Key 使用字符串反序列化
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer

      # Value 使用字符串反序列化
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer

      # 关闭自动提交 Offset，推荐业务处理成功后手动提交
      enable-auto-commit: false

      # 没有已提交 Offset 时，从最早消息开始消费；生产环境也可根据业务改为 latest
      auto-offset-reset: earliest

      # 单次 poll 拉取的最大消息数，批量消费时需要重点调整
      max-poll-records: 100

      properties:
        # 两次 poll 之间允许的最大间隔，业务处理过慢时需要适当调大
        max.poll.interval.ms: 300000

        # Consumer 与 Broker 心跳间隔
        heartbeat.interval.ms: 3000

        # Consumer 会话超时时间，超过后会触发 Rebalance
        session.timeout.ms: 45000

    listener:
      # 手动确认模式，业务处理成功后调用 Acknowledgment.acknowledge()
      ack-mode: manual

      # 监听容器并发数，不能超过 Topic 分区数太多，否则多余线程会空闲
      concurrency: 3

      # 单条消费模式，批量消费章节会单独说明 batch 模式
      type: single

      # 消费者启动时自动启动监听容器
      auto-startup: true

      # 缺少 Topic 时是否启动失败，本地开发可设置 false，生产建议提前创建 Topic
      missing-topics-fatal: false
```

关键配置说明如下：

| 配置                   | 建议值                 | 说明                           |
| ---------------------- | ---------------------- | ------------------------------ |
| `enable-auto-commit`   | `false`                | 生产业务推荐关闭自动提交       |
| `ack-mode`             | `manual`               | 业务处理成功后手动提交 Offset  |
| `auto-offset-reset`    | `earliest` 或 `latest` | 无历史 Offset 时的起始消费位置 |
| `max-poll-records`     | 按处理能力调整         | 控制单次拉取消息数量           |
| `max.poll.interval.ms` | 按业务耗时调整         | 避免处理耗时过长导致 Rebalance |
| `concurrency`          | 小于或等于分区数       | 控制监听容器并发消费线程数     |

如果只做本地快速测试，可以使用自动提交 Offset，配置会更简单。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  kafka:
    consumer:
      # 本地简单测试可以开启自动提交，生产业务不建议直接使用
      enable-auto-commit: true

      # 自动提交间隔，表示客户端定期提交当前消费进度
      auto-commit-interval: 1000

    listener:
      # 自动提交模式下可以使用 record，表示每条记录处理后由容器管理
      ack-mode: record
```

不过在真实业务中，更推荐使用手动提交。手动提交可以保证业务逻辑处理成功后再提交 Offset，降低消息还未处理完成但位点已经推进造成的消息丢失风险。

消费者配置需要重点注意以下问题：

| 问题                    | 处理建议                                             |
| ----------------------- | ---------------------------------------------------- |
| 消费失败是否提交 Offset | 一般不应直接提交，应进入重试、告警或死信 Topic       |
| 消费耗时过长            | 调整 `max.poll.interval.ms`，并优化业务处理逻辑      |
| 消费并发不足            | 增加 Topic 分区数，并合理提高 `listener.concurrency` |
| 消息重复消费            | Consumer 侧必须做幂等处理                            |
| 反序列化失败            | 需要配置异常处理策略，避免消费者线程反复失败         |
| Rebalance 频繁          | 检查消费耗时、心跳配置、实例稳定性和部署变更频率     |

本章节配置完成后，项目已经具备 Kafka 基础接入能力。后续消息生产章节可以基于 `KafkaTemplate` 发送普通字符串消息、带 Key 消息和 JSON 消息；消息消费章节可以基于 `@KafkaListener` 实现单条消费、批量消费和异常处理。



## 消息生产

本章节用于说明 Spring Boot 3 项目中如何通过 `KafkaTemplate` 发送 Kafka 消息。消息生产是 Kafka 开发中最常见的入口，业务系统通常会在订单创建、支付完成、用户注册、状态变更等操作完成后，将业务事件发送到 Kafka。

本章节示例默认基于前面章节中的配置：

```yaml
spring:
  kafka:
    bootstrap-servers: 127.0.0.1:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
```

当前章节先使用字符串消息发送，业务对象会通过 Hutool 的 `JSONUtil` 转换为 JSON 字符串后发送到 Kafka。后续 `消息序列化` 章节可以再单独说明 `JsonSerializer` 和自定义对象消息处理。

### KafkaTemplate 使用

`KafkaTemplate` 是 Spring Kafka 提供的核心发送组件，用于封装 Kafka Producer 的发送能力。业务代码中通常不直接操作 Kafka 原生 Producer，而是通过 `KafkaTemplate` 完成消息发送、结果回调和异常处理。

在本示例中，`KafkaTemplate<String, String>` 表示消息 Key 和消息 Value 都使用字符串类型。对应前面配置中的 `StringSerializer`。

建议将 Kafka 发送逻辑封装到独立的 Producer 类中，而不是直接在 Controller 或业务 Service 中散落调用 `KafkaTemplate`。这样便于统一处理日志、异常、发送结果和后续重试策略。

本章节涉及的关键文件如下：

```text
src/main/java/io/github/atengk/kafka/dto/OrderEventMessage.java
src/main/java/io/github/atengk/kafka/producer/OrderEventProducer.java
src/main/java/io/github/atengk/kafka/controller/KafkaMessageController.java
```

文件位置：`src/main/java/io/github/atengk/kafka/dto/OrderEventMessage.java`

下面的代码定义订单事件消息对象，业务发送前会将该对象转换为 JSON 字符串。

```java
package io.github.atengk.kafka.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单事件消息
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Data
public class OrderEventMessage {

    /**
     * 消息ID，用于业务幂等和日志追踪
     */
    private String messageId;

    /**
     * 订单ID
     */
    @NotBlank(message = "订单ID不能为空")
    private String orderId;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 订单金额
     */
    @NotNull(message = "订单金额不能为空")
    private BigDecimal amount;

    /**
     * 事件类型，例如 CREATED、PAID、CANCELLED
     */
    @NotBlank(message = "事件类型不能为空")
    private String eventType;

    /**
     * 事件时间
     */
    private LocalDateTime eventTime;

}
```

文件位置：`src/main/java/io/github/atengk/kafka/producer/OrderEventProducer.java`

下面的代码封装 Kafka 消息发送逻辑，包含普通消息发送、带 Key 消息发送和发送结果回调处理。

```java
package io.github.atengk.kafka.producer;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.dto.OrderEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * 订单事件消息生产者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 发送普通字符串消息。
     *
     * @param message 消息内容
     * @return 发送结果
     */
    public CompletableFuture<SendResult<String, String>> sendTextMessage(String message) {
        if (StrUtil.isBlank(message)) {
            log.warn("发送Kafka普通消息失败，消息内容为空");
            throw new IllegalArgumentException("消息内容不能为空");
        }

        log.info("开始发送Kafka普通消息，topic={}，message={}", KafkaTopicConstant.ORDER_EVENT_TOPIC, message);
        return kafkaTemplate.send(KafkaTopicConstant.ORDER_EVENT_TOPIC, message);
    }

    /**
     * 发送订单事件消息。
     *
     * @param message 订单事件消息
     * @return 发送结果
     */
    public CompletableFuture<SendResult<String, String>> sendOrderEvent(OrderEventMessage message) {
        if (ObjUtil.isNull(message)) {
            log.warn("发送订单事件失败，消息对象为空");
            throw new IllegalArgumentException("订单事件消息不能为空");
        }
        if (StrUtil.isBlank(message.getOrderId())) {
            log.warn("发送订单事件失败，订单ID为空");
            throw new IllegalArgumentException("订单ID不能为空");
        }

        fillDefaultValue(message);

        String key = message.getOrderId();
        String jsonMessage = JSONUtil.toJsonStr(message);

        log.info("开始发送订单事件，topic={}，key={}，messageId={}，eventType={}",
                KafkaTopicConstant.ORDER_EVENT_TOPIC,
                key,
                message.getMessageId(),
                message.getEventType());

        return kafkaTemplate.send(KafkaTopicConstant.ORDER_EVENT_TOPIC, key, jsonMessage);
    }

    /**
     * 发送订单事件并处理发送结果。
     *
     * @param message 订单事件消息
     */
    public void sendOrderEventWithCallback(OrderEventMessage message) {
        CompletableFuture<SendResult<String, String>> future = this.sendOrderEvent(message);

        future.whenComplete((sendResult, throwable) -> {
            if (ObjUtil.isNotNull(throwable)) {
                log.error("订单事件发送失败，orderId={}，messageId={}，原因={}",
                        message.getOrderId(),
                        message.getMessageId(),
                        throwable.getMessage(),
                        throwable);
                return;
            }

            RecordMetadata metadata = sendResult.getRecordMetadata();
            log.info("订单事件发送成功，orderId={}，messageId={}，topic={}，partition={}，offset={}",
                    message.getOrderId(),
                    message.getMessageId(),
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset());
        });
    }

    /**
     * 填充消息默认值。
     *
     * @param message 订单事件消息
     */
    private void fillDefaultValue(OrderEventMessage message) {
        if (StrUtil.isBlank(message.getMessageId())) {
            message.setMessageId(IdUtil.fastSimpleUUID());
        }
        if (ObjUtil.isNull(message.getEventTime())) {
            message.setEventTime(LocalDateTime.now());
        }
    }

}
```

这里使用 `CompletableFuture<SendResult<String, String>>` 接收发送结果。Spring Kafka 3 中 `KafkaTemplate#send` 返回的是 `CompletableFuture`，适合使用 `whenComplete` 处理发送成功和发送失败。

### 普通消息发送

普通消息发送适合本地联调、简单通知、日志类消息或不需要按业务 Key 分区的场景。调用 `kafkaTemplate.send(topic, message)` 时，只指定 Topic 和消息内容，Kafka 会根据默认分区策略选择写入的 Partition。

为了方便本地测试，可以提供一个简单的 Controller 接口触发消息发送。

文件位置：`src/main/java/io/github/atengk/kafka/controller/KafkaMessageController.java`

下面的代码提供 Kafka 消息发送测试接口，方便通过 HTTP 请求触发普通消息和订单事件消息发送。

```java
package io.github.atengk.kafka.controller;

import io.github.atengk.kafka.dto.OrderEventMessage;
import io.github.atengk.kafka.producer.OrderEventProducer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Kafka 消息测试接口
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequestMapping("/api/kafka/messages")
@RequiredArgsConstructor
public class KafkaMessageController {

    private final OrderEventProducer orderEventProducer;

    /**
     * 发送普通文本消息。
     *
     * @param message 消息内容
     * @return 处理结果
     */
    @PostMapping("/text")
    public String sendTextMessage(@RequestParam String message) {
        orderEventProducer.sendTextMessage(message);
        return "普通消息已提交发送";
    }

    /**
     * 发送订单事件消息。
     *
     * @param message 订单事件消息
     * @return 处理结果
     */
    @PostMapping("/order-event")
    public String sendOrderEvent(@Valid @RequestBody OrderEventMessage message) {
        orderEventProducer.sendOrderEventWithCallback(message);
        return "订单事件消息已提交发送";
    }

}
```

接口使用方式如下：

```bash
curl -X POST 'http://localhost:8080/api/kafka/messages/text?message=hello-kafka'
```

如果 Kafka 服务正常，应用日志中会输出发送日志。此时可以在 Kafka 容器中启动消费者验证消息是否进入 Topic。

```bash
docker exec -it kafka-dev bash

/opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server 127.0.0.1:9092 \
  --topic order-event-topic \
  --from-beginning
```

普通消息发送的优点是简单直接，但缺点是无法通过业务 Key 控制消息进入哪个 Partition。如果业务对同一订单、同一用户或同一设备的消息有顺序处理要求，不建议只发送普通消息，应使用带 Key 的消息发送方式。

### 带 Key 的消息发送

带 Key 的消息发送适合需要按照业务标识进行分区的场景。Kafka 默认会根据 Key 的哈希结果选择 Partition，因此相同 Key 的消息通常会进入同一个 Partition。

例如订单事件中，可以使用 `orderId` 作为消息 Key。这样同一个订单的创建、支付、取消等事件会进入同一个 Partition，有利于消费者按顺序处理同一个订单的状态变化。

订单事件请求示例如下：

```json
{
  "orderId": "ORDER_10001",
  "userId": 1001,
  "amount": 99.80,
  "eventType": "CREATED"
}
```

发送请求：

```bash
curl -X POST 'http://localhost:8080/api/kafka/messages/order-event' \
  -H 'Content-Type: application/json' \
  -d '{
    "orderId": "ORDER_10001",
    "userId": 1001,
    "amount": 99.80,
    "eventType": "CREATED"
  }'
```

如果连续发送同一个 `orderId` 的多条消息，它们通常会进入同一个 Partition。

```bash
curl -X POST 'http://localhost:8080/api/kafka/messages/order-event' \
  -H 'Content-Type: application/json' \
  -d '{
    "orderId": "ORDER_10001",
    "userId": 1001,
    "amount": 99.80,
    "eventType": "CREATED"
  }'

curl -X POST 'http://localhost:8080/api/kafka/messages/order-event' \
  -H 'Content-Type: application/json' \
  -d '{
    "orderId": "ORDER_10001",
    "userId": 1001,
    "amount": 99.80,
    "eventType": "PAID"
  }'
```

带 Key 消息适合以下场景：

| 场景         | 推荐 Key                  |
| ------------ | ------------------------- |
| 订单状态变更 | `orderId`                 |
| 用户事件流转 | `userId`                  |
| 设备数据上报 | `deviceId`                |
| 商户交易流水 | `merchantId` 或 `tradeId` |
| 支付状态通知 | `paymentId` 或 `orderId`  |

使用 Key 时需要注意，Kafka 只保证同一个 Partition 内消息有序。如果同一个业务对象的消息必须按顺序消费，应确保它们使用相同 Key，并且不要在中途随意改变分区策略。

如果某个 Key 的消息量特别大，也可能导致单个 Partition 负载过高。例如某个大商户、热门用户或高频设备持续产生大量消息，此时需要结合业务场景设计更合理的 Key，例如增加业务分片字段。

### 发送结果处理

Kafka 消息发送不是简单的本地方法调用。调用 `send` 方法后，消息会经过序列化、进入 Producer 缓冲区、批量发送到 Broker，并等待 Broker 返回确认结果。因此生产环境中必须处理发送成功和发送失败两种情况。

发送结果中常用信息包括：

| 信息        | 说明                      |
| ----------- | ------------------------- |
| `topic`     | 消息写入的 Topic          |
| `partition` | 消息写入的 Partition      |
| `offset`    | 消息在 Partition 中的位点 |
| `timestamp` | Kafka 记录的消息时间戳    |
| `exception` | 发送失败时的异常信息      |

前面的 `sendOrderEventWithCallback` 方法已经通过 `whenComplete` 处理了发送结果：

```java
future.whenComplete((sendResult, throwable) -> {
    if (ObjUtil.isNotNull(throwable)) {
        log.error("订单事件发送失败，orderId={}，messageId={}，原因={}",
                message.getOrderId(),
                message.getMessageId(),
                throwable.getMessage(),
                throwable);
        return;
    }

    RecordMetadata metadata = sendResult.getRecordMetadata();
    log.info("订单事件发送成功，orderId={}，messageId={}，topic={}，partition={}，offset={}",
            message.getOrderId(),
            message.getMessageId(),
            metadata.topic(),
            metadata.partition(),
            metadata.offset());
});
```

对于普通业务系统，发送结果处理建议如下：

| 场景           | 处理建议                                                 |
| -------------- | -------------------------------------------------------- |
| 发送成功       | 记录 `topic`、`partition`、`offset`、业务 ID，便于排查   |
| 发送失败       | 记录异常堆栈、业务 ID、消息 ID，必要时落库或进入重试流程 |
| 接口同步发送   | 不建议长时间阻塞等待 Kafka 结果，避免影响接口响应        |
| 关键业务消息   | 可结合本地消息表或事务外盒模式保证最终发送               |
| 非关键日志消息 | 可以只记录失败日志，不阻塞主业务流程                     |

如果业务要求调用方明确知道消息是否发送成功，也可以同步等待发送结果。但这种方式会增加接口响应耗时，不建议在高并发主链路中滥用。

下面示例提供同步等待发送结果的方法，适合管理后台、测试接口或低频关键操作。

文件位置：`src/main/java/io/github/atengk/kafka/producer/OrderEventProducer.java`

下面的方法可以追加到前面的 `OrderEventProducer` 类中，用于同步发送并返回 Kafka 写入位置。

```java
/**
 * 同步发送订单事件并返回写入位置。
 *
 * @param message 订单事件消息
 * @return Kafka 写入位置
 */
public String sendOrderEventSync(OrderEventMessage message) {
    try {
        SendResult<String, String> sendResult = this.sendOrderEvent(message).get();
        RecordMetadata metadata = sendResult.getRecordMetadata();

        String result = StrUtil.format("topic={}, partition={}, offset={}",
                metadata.topic(),
                metadata.partition(),
                metadata.offset());

        log.info("订单事件同步发送成功，orderId={}，messageId={}，{}",
                message.getOrderId(),
                message.getMessageId(),
                result);

        return result;
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.error("订单事件同步发送被中断，orderId={}，messageId={}",
                message.getOrderId(),
                message.getMessageId(),
                e);
        throw new IllegalStateException("订单事件同步发送被中断", e);
    } catch (Exception e) {
        log.error("订单事件同步发送失败，orderId={}，messageId={}，原因={}",
                message.getOrderId(),
                message.getMessageId(),
                e.getMessage(),
                e);
        throw new IllegalStateException("订单事件同步发送失败", e);
    }
}
```

如果需要暴露同步发送测试接口，可以在 Controller 中增加下面的方法。

文件位置：`src/main/java/io/github/atengk/kafka/controller/KafkaMessageController.java`

```java
/**
 * 同步发送订单事件消息。
 *
 * @param message 订单事件消息
 * @return Kafka 写入位置
 */
@PostMapping("/order-event/sync")
public String sendOrderEventSync(@Valid @RequestBody OrderEventMessage message) {
    return orderEventProducer.sendOrderEventSync(message);
}
```

同步发送测试命令如下：

```bash
curl -X POST 'http://localhost:8080/api/kafka/messages/order-event/sync' \
  -H 'Content-Type: application/json' \
  -d '{
    "orderId": "ORDER_10002",
    "userId": 1002,
    "amount": 199.90,
    "eventType": "CREATED"
  }'
```

响应示例：

```text
topic=order-event-topic, partition=1, offset=12
```

生产环境中，发送结果处理不能只依赖日志。对于订单、支付、资金、库存等关键消息，建议使用本地消息表、事务外盒模式或可靠事件表记录待发送消息，再由定时任务或后台补偿任务保证最终投递成功。Kafka Producer 的重试只能处理部分临时异常，不能替代业务层面的可靠消息设计。

本章节完成后，项目已经具备基本消息生产能力。后续 `消息消费` 章节可以基于当前发送的 `order-event-topic` 实现单条消息消费、批量消息消费和消费异常处理。



## 消息消费

本章节用于说明 Spring Boot 3 项目中如何通过 `@KafkaListener` 消费 Kafka 消息。消息消费是 Kafka 开发中最容易出现问题的部分，除了能正常读取消息，还需要重点关注 Offset 提交、异常处理、重复消费、批量消费和消费幂等。

本章节示例默认基于以下基础配置：

```yaml
spring:
  kafka:
    consumer:
      # 默认消费者组，具体监听器也可以通过 @KafkaListener 单独指定
      group-id: order-event-group

      # 关闭自动提交，业务处理成功后手动确认
      enable-auto-commit: false

      # 没有历史 Offset 时从最早位置开始消费
      auto-offset-reset: earliest

      # Key 使用字符串反序列化
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer

      # Value 使用字符串反序列化
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer

      # 单次 poll 拉取的最大消息数
      max-poll-records: 100

    listener:
      # 手动提交 Offset
      ack-mode: manual

      # 默认单条消息消费
      type: single

      # 监听容器并发数，建议不要超过 Topic 分区数太多
      concurrency: 3
```

本章节涉及的关键文件如下：

```text
src/main/java/io/github/atengk/kafka/constant/KafkaTopicConstant.java
src/main/java/io/github/atengk/kafka/dto/OrderEventMessage.java
src/main/java/io/github/atengk/kafka/consumer/OrderEventConsumer.java
src/main/java/io/github/atengk/kafka/consumer/BatchOrderEventConsumer.java
src/main/java/io/github/atengk/kafka/config/KafkaBatchConsumerConfig.java
src/main/java/io/github/atengk/kafka/config/KafkaConsumerErrorConfig.java
```

如果同一个项目中同时存在多个监听同一 Topic、同一 Group ID 的消费者示例，需要注意它们会共同参与同一个 Consumer Group 的分区分配。实际运行时建议只启用一个示例消费者，或者为不同示例配置不同的 Group ID，避免测试结果混乱。

### KafkaListener 使用

`@KafkaListener` 是 Spring Kafka 提供的消费者监听注解，用于声明一个 Kafka 消费方法。应用启动后，Spring 会自动创建监听容器，订阅指定 Topic，并在拉取到消息后调用对应方法。

常用写法如下：

```java
@KafkaListener(
        topics = KafkaTopicConstant.ORDER_EVENT_TOPIC,
        groupId = KafkaTopicConstant.ORDER_EVENT_GROUP
)
public void consume(String message, Acknowledgment acknowledgment) {
    // 消费逻辑
}
```

`@KafkaListener` 常用参数说明如下：

| 参数               | 说明                                             |
| ------------------ | ------------------------------------------------ |
| `topics`           | 监听的 Topic，可以配置一个或多个                 |
| `groupId`          | 当前消费者所属 Consumer Group                    |
| `containerFactory` | 指定监听容器工厂，常用于批量消费、自定义异常处理 |
| `concurrency`      | 当前监听器并发数，可覆盖全局配置                 |
| `autoStartup`      | 是否随应用启动自动启动监听器                     |
| `id`               | 监听容器 ID，便于管理和日志定位                  |

为了避免 Topic 和 Group ID 散落在代码中，建议统一放到常量类中。

文件位置：`src/main/java/io/github/atengk/kafka/constant/KafkaTopicConstant.java`

```java
package io.github.atengk.kafka.constant;

/**
 * Kafka Topic 常量
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class KafkaTopicConstant {

    /**
     * 订单事件 Topic
     */
    public static final String ORDER_EVENT_TOPIC = "order-event-topic";

    /**
     * 订单事件消费者组
     */
    public static final String ORDER_EVENT_GROUP = "order-event-group";

    /**
     * 订单事件批量消费者组
     */
    public static final String ORDER_EVENT_BATCH_GROUP = "order-event-batch-group";

    /**
     * 订单事件死信 Topic
     */
    public static final String ORDER_EVENT_DLT_TOPIC = "order-event-topic.DLT";

    private KafkaTopicConstant() {
    }

}
```

消费方法中常见可注入参数如下：

| 参数类型                                   | 说明                                                         |
| ------------------------------------------ | ------------------------------------------------------------ |
| `String message`                           | 消息 Value 内容                                              |
| `ConsumerRecord<String, String>`           | 完整 Kafka 消息记录，包含 Topic、Partition、Offset、Key、Value 等 |
| `Acknowledgment`                           | 手动提交 Offset 的确认对象                                   |
| `@Header(KafkaHeaders.RECEIVED_KEY)`       | 获取消息 Key                                                 |
| `@Header(KafkaHeaders.RECEIVED_TOPIC)`     | 获取 Topic                                                   |
| `@Header(KafkaHeaders.RECEIVED_PARTITION)` | 获取 Partition                                               |
| `@Header(KafkaHeaders.OFFSET)`             | 获取 Offset                                                  |

在生产业务中，更推荐使用 `ConsumerRecord`，因为它能拿到完整的 Kafka 元数据，方便记录日志和排查问题。

### 单条消息消费

单条消息消费是最常见的 Kafka 消费方式。每次调用监听方法时只处理一条消息，适合大多数业务事件处理场景，例如订单状态同步、用户事件处理、支付结果通知等。

文件位置：`src/main/java/io/github/atengk/kafka/consumer/OrderEventConsumer.java`

下面的代码用于消费订单事件 JSON 消息，处理成功后手动提交 Offset，处理失败时抛出异常交给 Spring Kafka 异常处理机制。

```java
package io.github.atengk.kafka.consumer;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.dto.OrderEventMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 订单事件单条消息消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class OrderEventConsumer {

    /**
     * 消费订单事件消息。
     *
     * @param record Kafka 消息记录
     * @param acknowledgment Offset 确认对象
     */
    @KafkaListener(
            id = "orderEventConsumer",
            topics = KafkaTopicConstant.ORDER_EVENT_TOPIC,
            groupId = KafkaTopicConstant.ORDER_EVENT_GROUP
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        String key = record.key();
        String jsonMessage = record.value();

        try {
            log.info("开始消费订单事件消息，topic={}，partition={}，offset={}，key={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    key);

            if (StrUtil.isBlank(jsonMessage)) {
                log.warn("订单事件消息为空，直接提交Offset，topic={}，partition={}，offset={}",
                        record.topic(),
                        record.partition(),
                        record.offset());
                acknowledgment.acknowledge();
                return;
            }

            OrderEventMessage message = JSONUtil.toBean(jsonMessage, OrderEventMessage.class);
            if (ObjUtil.isNull(message) || StrUtil.isBlank(message.getOrderId())) {
                log.warn("订单事件消息结构不完整，直接提交Offset，topic={}，partition={}，offset={}，message={}",
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        jsonMessage);
                acknowledgment.acknowledge();
                return;
            }

            this.handleOrderEvent(message);

            // 业务处理成功后再提交 Offset
            acknowledgment.acknowledge();

            log.info("订单事件消息消费完成，orderId={}，messageId={}，eventType={}，partition={}，offset={}",
                    message.getOrderId(),
                    message.getMessageId(),
                    message.getEventType(),
                    record.partition(),
                    record.offset());
        } catch (Exception e) {
            log.error("订单事件消息消费失败，topic={}，partition={}，offset={}，key={}，message={}，原因={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    key,
                    jsonMessage,
                    e.getMessage(),
                    e);

            // 不在失败时提交 Offset，交给异常处理器重试或转入死信 Topic
            throw e;
        }
    }

    /**
     * 处理订单事件业务逻辑。
     *
     * @param message 订单事件消息
     */
    private void handleOrderEvent(OrderEventMessage message) {
        if ("CREATED".equals(message.getEventType())) {
            log.info("处理订单创建事件，orderId={}，userId={}，amount={}",
                    message.getOrderId(),
                    message.getUserId(),
                    message.getAmount());
            return;
        }

        if ("PAID".equals(message.getEventType())) {
            log.info("处理订单支付事件，orderId={}，userId={}，amount={}",
                    message.getOrderId(),
                    message.getUserId(),
                    message.getAmount());
            return;
        }

        if ("CANCELLED".equals(message.getEventType())) {
            log.info("处理订单取消事件，orderId={}，userId={}",
                    message.getOrderId(),
                    message.getUserId());
            return;
        }

        log.warn("未知订单事件类型，orderId={}，eventType={}",
                message.getOrderId(),
                message.getEventType());
    }

}
```

单条消费测试方式如下：

```bash
curl -X POST 'http://localhost:8080/api/kafka/messages/order-event' \
  -H 'Content-Type: application/json' \
  -d '{
    "orderId": "ORDER_30001",
    "userId": 1001,
    "amount": 99.80,
    "eventType": "CREATED"
  }'
```

如果消费成功，应用日志中应能看到类似内容：

```text
开始消费订单事件消息，topic=order-event-topic，partition=0，offset=15，key=ORDER_30001
处理订单创建事件，orderId=ORDER_30001，userId=1001，amount=99.8
订单事件消息消费完成，orderId=ORDER_30001，messageId=xxx，eventType=CREATED，partition=0，offset=15
```

单条消息消费建议如下：

| 处理点      | 建议                                                |
| ----------- | --------------------------------------------------- |
| Offset 提交 | 业务处理成功后再调用 `acknowledgment.acknowledge()` |
| 空消息      | 可以记录警告并提交 Offset，避免重复拉取无效消息     |
| 非法消息    | 可以提交 Offset 或转入死信 Topic，按业务要求决定    |
| 业务异常    | 不要直接吞掉异常，应记录日志并交给异常处理机制      |
| 幂等处理    | 使用 `messageId`、业务唯一键或去重表避免重复处理    |

### 批量消息消费

批量消费适合高吞吐场景。Kafka Consumer 每次 poll 会拉取多条消息，Spring Kafka 可以将这些消息以 `List<ConsumerRecord<String, String>>` 形式一次性传给监听方法。

批量消费可以减少方法调用次数，提高吞吐能力，但也会增加单次处理失败后的复杂度。生产环境中使用批量消费时，需要明确失败策略：整批失败重试、跳过坏消息、部分成功部分失败，或者失败消息进入死信 Topic。

启用批量消费需要配置独立的监听容器工厂。

文件位置：`src/main/java/io/github/atengk/kafka/config/KafkaBatchConsumerConfig.java`

下面的代码创建批量消费监听容器工厂，并设置手动提交 Offset。

```java
package io.github.atengk.kafka.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

/**
 * Kafka 批量消费配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
@RequiredArgsConstructor
public class KafkaBatchConsumerConfig {

    private final ConsumerFactory<String, String> consumerFactory;

    /**
     * 创建批量消费监听容器工厂。
     *
     * @return 批量消费监听容器工厂
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> batchKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        // 开启批量监听模式
        factory.setBatchListener(true);

        // 设置批量消费并发度，建议不超过 Topic 分区数
        factory.setConcurrency(3);

        // 手动确认 Offset
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        return factory;
    }

}
```

同时可以在配置文件中调整单次拉取数量。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  kafka:
    consumer:
      # 批量消费时单次 poll 最多拉取 100 条消息
      max-poll-records: 100

    listener:
      # 全局仍可保持 single，批量监听器通过 containerFactory 单独启用
      type: single
```

批量消费代码如下。

文件位置：`src/main/java/io/github/atengk/kafka/consumer/BatchOrderEventConsumer.java`

下面的代码用于批量消费订单事件消息，所有消息处理完成后统一提交 Offset。

```java
package io.github.atengk.kafka.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.dto.OrderEventMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单事件批量消息消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class BatchOrderEventConsumer {

    /**
     * 批量消费订单事件消息。
     *
     * @param records Kafka 消息记录列表
     * @param acknowledgment Offset 确认对象
     */
    @KafkaListener(
            id = "batchOrderEventConsumer",
            topics = KafkaTopicConstant.ORDER_EVENT_TOPIC,
            groupId = KafkaTopicConstant.ORDER_EVENT_BATCH_GROUP,
            containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void consumeBatch(List<ConsumerRecord<String, String>> records, Acknowledgment acknowledgment) {
        if (CollUtil.isEmpty(records)) {
            log.warn("批量消费订单事件消息跳过，消息列表为空");
            acknowledgment.acknowledge();
            return;
        }

        try {
            log.info("开始批量消费订单事件消息，count={}", records.size());

            for (ConsumerRecord<String, String> record : records) {
                this.handleRecord(record);
            }

            // 整批处理成功后统一提交 Offset
            acknowledgment.acknowledge();

            ConsumerRecord<String, String> lastRecord = records.get(records.size() - 1);
            log.info("批量消费订单事件消息完成，count={}，lastPartition={}，lastOffset={}",
                    records.size(),
                    lastRecord.partition(),
                    lastRecord.offset());
        } catch (Exception e) {
            log.error("批量消费订单事件消息失败，count={}，原因={}",
                    records.size(),
                    e.getMessage(),
                    e);

            // 批量消费失败时不提交 Offset，交给异常处理器处理
            throw e;
        }
    }

    /**
     * 处理单条 Kafka 记录。
     *
     * @param record Kafka 消息记录
     */
    private void handleRecord(ConsumerRecord<String, String> record) {
        String jsonMessage = record.value();

        if (StrUtil.isBlank(jsonMessage)) {
            log.warn("批量消费跳过空消息，topic={}，partition={}，offset={}",
                    record.topic(),
                    record.partition(),
                    record.offset());
            return;
        }

        OrderEventMessage message = JSONUtil.toBean(jsonMessage, OrderEventMessage.class);
        if (message == null || StrUtil.isBlank(message.getOrderId())) {
            log.warn("批量消费跳过结构不完整消息，topic={}，partition={}，offset={}，message={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    jsonMessage);
            return;
        }

        log.info("批量处理订单事件，orderId={}，messageId={}，eventType={}，partition={}，offset={}",
                message.getOrderId(),
                message.getMessageId(),
                message.getEventType(),
                record.partition(),
                record.offset());

        // 这里编写真实批量业务逻辑，示例中仅输出日志
    }

}
```

批量消费时需要重点注意 Offset 提交粒度。上面示例是整批处理成功后统一提交 Offset。如果第 80 条消息处理失败，那么前 79 条消息可能已经执行业务逻辑，但 Offset 没有提交，后续重试时这 79 条消息可能再次被消费。因此批量消费更依赖业务幂等。

批量消费适合以下场景：

| 场景         | 说明                                      |
| ------------ | ----------------------------------------- |
| 写入数据库   | 多条消息聚合后批量 insert 或 batch update |
| 同步搜索引擎 | 多条数据合并后批量写入 Elasticsearch      |
| 日志处理     | 日志类消息通常允许批量处理                |
| 数据统计     | 多条事件聚合后统一计算                    |
| 高吞吐消费   | 消费速度优先，允许通过幂等处理重复消息    |

不适合批量消费的场景包括：单条消息处理耗时差异很大、单条失败需要精细补偿、业务无法接受重复处理、消息之间存在严格顺序依赖。

### 消费异常处理

消费异常处理用于控制消息处理失败后的行为。Kafka Consumer 中常见异常包括 JSON 解析失败、业务校验失败、数据库操作失败、外部接口调用失败、反序列化失败等。

异常处理通常分为三层：

| 层级                    | 处理方式                       | 说明                              |
| ----------------------- | ------------------------------ | --------------------------------- |
| 方法内部处理            | `try-catch`                    | 记录业务日志，决定是否提交 Offset |
| Spring Kafka 异常处理器 | `DefaultErrorHandler`          | 控制重试次数、重试间隔、死信投递  |
| 业务补偿机制            | 本地消息表、重试表、死信 Topic | 处理最终失败消息                  |

对于生产业务，不建议在消费者方法中简单捕获异常后直接 `acknowledgment.acknowledge()`。这样会导致失败消息的 Offset 被提交，消息不会再被消费，可能造成业务丢失。

更推荐的策略是：业务处理失败时抛出异常，由 Spring Kafka 的 `DefaultErrorHandler` 进行有限次数重试，重试仍失败后投递到死信 Topic。

文件位置：`src/main/java/io/github/atengk/kafka/config/KafkaConsumerErrorConfig.java`

下面的代码配置 Kafka 消费异常处理器，失败消息重试 3 次后发送到死信 Topic。

```java
package io.github.atengk.kafka.config;

import io.github.atengk.kafka.constant.KafkaTopicConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka 消费异常处理配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaConsumerErrorConfig {

    /**
     * 创建 Kafka 默认异常处理器。
     *
     * @param kafkaOperations Kafka 操作模板
     * @return 默认异常处理器
     */
    @Bean
    public DefaultErrorHandler kafkaDefaultErrorHandler(KafkaOperations<Object, Object> kafkaOperations) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaOperations,
                (record, exception) -> {
                    log.error("Kafka消息重试耗尽，准备发送到死信Topic，sourceTopic={}，partition={}，offset={}，原因={}",
                            record.topic(),
                            record.partition(),
                            record.offset(),
                            exception.getMessage(),
                            exception);

                    // 将失败消息发送到固定死信 Topic，分区沿用原分区
                    return new TopicPartition(KafkaTopicConstant.ORDER_EVENT_DLT_TOPIC, record.partition());
                }
        );

        // 固定间隔重试：间隔 1000 毫秒，最多重试 3 次
        FixedBackOff fixedBackOff = new FixedBackOff(1000L, 3L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, fixedBackOff);

        // 消息最终进入死信 Topic 后，提交原消息 Offset，避免一直阻塞消费
        errorHandler.setAckAfterHandle(true);

        return errorHandler;
    }

}
```

死信 Topic 需要提前创建，或者通过 `NewTopic` 自动创建。

文件位置：`src/main/java/io/github/atengk/kafka/config/KafkaTopicConfig.java`

```java
package io.github.atengk.kafka.config;

import io.github.atengk.kafka.constant.KafkaTopicConstant;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Kafka Topic 配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
public class KafkaTopicConfig {

    /**
     * 创建订单事件 Topic。
     *
     * @return Topic 配置
     */
    @Bean
    public NewTopic orderEventTopic() {
        return new NewTopic(KafkaTopicConstant.ORDER_EVENT_TOPIC, 3, (short) 1);
    }

    /**
     * 创建订单事件死信 Topic。
     *
     * @return Topic 配置
     */
    @Bean
    public NewTopic orderEventDltTopic() {
        return new NewTopic(KafkaTopicConstant.ORDER_EVENT_DLT_TOPIC, 3, (short) 1);
    }

}
```

可以再增加一个死信 Topic 消费者，用于记录失败消息，后续也可以对接告警、人工处理或补偿任务。

文件位置：`src/main/java/io/github/atengk/kafka/consumer/OrderEventDltConsumer.java`

下面的代码用于消费订单事件死信 Topic，记录失败消息内容和 Kafka 元数据。

```java
package io.github.atengk.kafka.consumer;

import io.github.atengk.kafka.constant.KafkaTopicConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 订单事件死信消息消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class OrderEventDltConsumer {

    /**
     * 消费订单事件死信消息。
     *
     * @param record Kafka 消息记录
     */
    @KafkaListener(
            id = "orderEventDltConsumer",
            topics = KafkaTopicConstant.ORDER_EVENT_DLT_TOPIC,
            groupId = "order-event-dlt-group"
    )
    public void consumeDlt(ConsumerRecord<String, String> record) {
        log.error("收到订单事件死信消息，topic={}，partition={}，offset={}，key={}，value={}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                record.value());

        // 生产环境可在这里写入失败消息表、触发告警、通知人工处理或进入补偿流程
    }

}
```

异常处理验证可以临时在 `handleOrderEvent` 方法中制造异常。

```java
/**
 * 处理订单事件业务逻辑。
 *
 * @param message 订单事件消息
 */
private void handleOrderEvent(OrderEventMessage message) {
    if ("ERROR".equals(message.getEventType())) {
        throw new IllegalStateException("模拟订单事件消费异常");
    }

    log.info("处理订单事件，orderId={}，eventType={}",
            message.getOrderId(),
            message.getEventType());
}
```

发送一条异常消息：

```bash
curl -X POST 'http://localhost:8080/api/kafka/messages/order-event' \
  -H 'Content-Type: application/json' \
  -d '{
    "orderId": "ORDER_ERROR_30001",
    "userId": 1001,
    "amount": 99.80,
    "eventType": "ERROR"
  }'
```

预期现象如下：

| 阶段        | 现象                                                      |
| ----------- | --------------------------------------------------------- |
| 第一次消费  | 消费者抛出异常，日志输出消费失败                          |
| 重试阶段    | `DefaultErrorHandler` 按固定间隔重试                      |
| 重试耗尽    | 消息被发送到 `order-event-topic.DLT`                      |
| 死信消费    | `OrderEventDltConsumer` 输出死信消息日志                  |
| Offset 处理 | 原 Topic 的失败消息 Offset 被提交，主消费链路继续向后消费 |

消费异常处理建议如下：

| 异常类型       | 建议处理方式                                    |
| -------------- | ----------------------------------------------- |
| JSON 格式错误  | 记录原始消息，进入死信 Topic 或提交 Offset 跳过 |
| 参数缺失       | 视为无效消息，进入死信 Topic 或记录后跳过       |
| 数据库短暂异常 | 有限次数重试，仍失败后进入死信 Topic            |
| 外部接口超时   | 有限次数重试，必要时进入业务补偿表              |
| 业务状态不满足 | 根据业务判断是跳过、延迟重试还是进入死信        |
| 代码缺陷       | 快速告警，不应无限重试阻塞消费                  |

消费者开发时需要遵循几个原则：

| 原则         | 说明                                              |
| ------------ | ------------------------------------------------- |
| 不要吞异常   | 消费失败不能只打印日志后继续提交 Offset           |
| 不要无限重试 | 无限重试会阻塞当前 Partition 后续消息             |
| 保证幂等     | Kafka 至少一次投递语义下，重复消费是正常情况      |
| 记录上下文   | 日志中记录 Topic、Partition、Offset、Key、业务 ID |
| 区分异常类型 | 临时异常可重试，永久异常应进入死信或人工处理      |
| 避免长事务   | 单次消费逻辑过长容易导致 Rebalance 和重复消费     |

本章节完成后，项目已经具备单条消费、批量消费和异常处理能力。后续 `消费位点管理` 章节可以继续围绕自动提交 Offset、手动提交 Offset、重复消费和幂等处理展开。

## 消息序列化

本章节用于说明 Kafka 消息在 Spring Boot 3 项目中的序列化和反序列化处理方式。Kafka 底层传输的是字节数据，Producer 发送消息时需要将 Java 对象转换为字节数组，Consumer 消费消息时需要将字节数组还原为 Java 可处理的数据类型。

在实际开发中，常见处理方式有三种：

| 方式           | 说明                                           | 适用场景                                                 |
| -------------- | ---------------------------------------------- | -------------------------------------------------------- |
| String 消息    | 直接发送字符串内容                             | 本地测试、简单文本消息、日志消息                         |
| JSON 消息      | 业务对象转为 JSON 字符串发送                   | 推荐的通用业务消息方式                                   |
| 自定义对象消息 | 使用 Spring Kafka 的 JSON 序列化器直接发送对象 | 项目内部 Topic、类型稳定、生产消费双方都使用 Java 的场景 |

生产项目中更推荐使用 JSON 字符串作为消息格式，因为它语言无关、可读性强、便于排查问题，也更适合被不同技术栈的系统消费。

### String 消息处理

String 消息处理是最基础的 Kafka 消息处理方式。Producer 将字符串发送到 Kafka，Consumer 使用字符串反序列化器读取消息内容。

这种方式适合本地验证 Kafka 链路、发送简单通知、日志类消息或无需复杂结构的文本消息。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  kafka:
    producer:
      # 消息 Key 使用字符串序列化
      key-serializer: org.apache.kafka.common.serialization.StringSerializer

      # 消息 Value 使用字符串序列化
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

    consumer:
      # 消息 Key 使用字符串反序列化
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer

      # 消息 Value 使用字符串反序列化
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
```

字符串消息发送代码如下。

文件位置：`src/main/java/io/github/atengk/kafka/producer/StringMessageProducer.java`

下面的代码用于发送普通字符串消息，适合快速验证 Kafka Topic 是否可正常写入。

```java
package io.github.atengk.kafka.producer;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 字符串消息生产者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StringMessageProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 发送字符串消息。
     *
     * @param message 消息内容
     * @return 发送结果
     */
    public CompletableFuture<?> sendMessage(String message) {
        if (StrUtil.isBlank(message)) {
            log.warn("发送字符串消息失败，消息内容为空");
            throw new IllegalArgumentException("消息内容不能为空");
        }

        log.info("开始发送字符串消息，topic={}，message={}", KafkaTopicConstant.ORDER_EVENT_TOPIC, message);
        return kafkaTemplate.send(KafkaTopicConstant.ORDER_EVENT_TOPIC, message);
    }

}
```

字符串消息消费代码如下。

文件位置：`src/main/java/io/github/atengk/kafka/consumer/StringMessageConsumer.java`

下面的代码用于消费字符串消息，并在业务处理成功后手动提交 Offset。

```java
package io.github.atengk.kafka.consumer;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 字符串消息消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class StringMessageConsumer {

    /**
     * 消费字符串消息。
     *
     * @param message 消息内容
     * @param acknowledgment Offset 确认对象
     */
    @KafkaListener(
            topics = KafkaTopicConstant.ORDER_EVENT_TOPIC,
            groupId = KafkaTopicConstant.ORDER_EVENT_GROUP
    )
    public void consume(String message, Acknowledgment acknowledgment) {
        try {
            if (StrUtil.isBlank(message)) {
                log.warn("消费字符串消息跳过，消息内容为空");
                acknowledgment.acknowledge();
                return;
            }

            log.info("消费字符串消息成功，message={}", message);

            // 业务处理成功后再提交 Offset
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("消费字符串消息失败，message={}，原因={}", message, e.getMessage(), e);
            throw e;
        }
    }

}
```

字符串消息的优点是配置简单、调试方便，缺点是缺少明确的业务结构。随着业务复杂度提升，建议使用 JSON 消息表达业务字段。

### JSON 消息处理

JSON 消息处理是业务开发中最常用的 Kafka 消息处理方式。Producer 将 Java 对象转换为 JSON 字符串发送，Consumer 收到 JSON 字符串后再转换为 Java 对象。

这种方式本质上仍然使用 `StringSerializer` 和 `StringDeserializer`，但业务层使用 Hutool 的 `JSONUtil` 完成对象和 JSON 字符串之间的转换。

推荐这种方式的原因如下：

| 优点       | 说明                                       |
| ---------- | ------------------------------------------ |
| 可读性强   | Kafka 控制台或日志中可以直接查看消息内容   |
| 语言无关   | Java、Go、Python、Node.js 等系统都可以消费 |
| 排查方便   | 消息格式清晰，便于定位字段缺失或数据异常   |
| 兼容性较好 | 新增字段通常不影响旧消费者处理             |
| 配置简单   | 不需要配置复杂的类型映射和可信包           |

订单事件消息对象如下。

文件位置：`src/main/java/io/github/atengk/kafka/dto/OrderEventMessage.java`

```java
package io.github.atengk.kafka.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单事件消息
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Data
public class OrderEventMessage {

    /**
     * 消息ID，用于幂等处理和日志追踪
     */
    private String messageId;

    /**
     * 订单ID
     */
    @NotBlank(message = "订单ID不能为空")
    private String orderId;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 订单金额
     */
    @NotNull(message = "订单金额不能为空")
    private BigDecimal amount;

    /**
     * 事件类型，例如 CREATED、PAID、CANCELLED
     */
    @NotBlank(message = "事件类型不能为空")
    private String eventType;

    /**
     * 事件时间
     */
    private LocalDateTime eventTime;

}
```

JSON 消息生产者如下。

文件位置：`src/main/java/io/github/atengk/kafka/producer/JsonOrderEventProducer.java`

下面的代码使用 Hutool 将订单事件对象转换为 JSON 字符串后发送到 Kafka。

```java
package io.github.atengk.kafka.producer;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.dto.OrderEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * JSON 订单事件生产者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonOrderEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 发送订单事件 JSON 消息。
     *
     * @param message 订单事件消息
     * @return 发送结果
     */
    public CompletableFuture<?> sendOrderEvent(OrderEventMessage message) {
        validateMessage(message);
        fillDefaultValue(message);

        String key = message.getOrderId();
        String jsonMessage = JSONUtil.toJsonStr(message);

        log.info("开始发送订单事件JSON消息，topic={}，key={}，messageId={}，eventType={}",
                KafkaTopicConstant.ORDER_EVENT_TOPIC,
                key,
                message.getMessageId(),
                message.getEventType());

        return kafkaTemplate.send(KafkaTopicConstant.ORDER_EVENT_TOPIC, key, jsonMessage);
    }

    /**
     * 校验订单事件消息。
     *
     * @param message 订单事件消息
     */
    private void validateMessage(OrderEventMessage message) {
        if (ObjUtil.isNull(message)) {
            log.warn("发送订单事件JSON消息失败，消息对象为空");
            throw new IllegalArgumentException("订单事件消息不能为空");
        }
        if (StrUtil.isBlank(message.getOrderId())) {
            log.warn("发送订单事件JSON消息失败，订单ID为空");
            throw new IllegalArgumentException("订单ID不能为空");
        }
        if (ObjUtil.isNull(message.getUserId())) {
            log.warn("发送订单事件JSON消息失败，用户ID为空，orderId={}", message.getOrderId());
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (StrUtil.isBlank(message.getEventType())) {
            log.warn("发送订单事件JSON消息失败，事件类型为空，orderId={}", message.getOrderId());
            throw new IllegalArgumentException("事件类型不能为空");
        }
    }

    /**
     * 填充默认字段。
     *
     * @param message 订单事件消息
     */
    private void fillDefaultValue(OrderEventMessage message) {
        if (StrUtil.isBlank(message.getMessageId())) {
            message.setMessageId(IdUtil.fastSimpleUUID());
        }
        if (ObjUtil.isNull(message.getEventTime())) {
            message.setEventTime(LocalDateTime.now());
        }
    }

}
```

JSON 消息消费者如下。

文件位置：`src/main/java/io/github/atengk/kafka/consumer/JsonOrderEventConsumer.java`

下面的代码消费 JSON 字符串，并使用 Hutool 转换为订单事件对象。

```java
package io.github.atengk.kafka.consumer;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.dto.OrderEventMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * JSON 订单事件消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class JsonOrderEventConsumer {

    /**
     * 消费订单事件 JSON 消息。
     *
     * @param jsonMessage JSON 消息内容
     * @param acknowledgment Offset 确认对象
     */
    @KafkaListener(
            topics = KafkaTopicConstant.ORDER_EVENT_TOPIC,
            groupId = KafkaTopicConstant.ORDER_EVENT_GROUP
    )
    public void consume(String jsonMessage, Acknowledgment acknowledgment) {
        try {
            if (StrUtil.isBlank(jsonMessage)) {
                log.warn("消费订单事件JSON消息跳过，消息内容为空");
                acknowledgment.acknowledge();
                return;
            }

            OrderEventMessage message = JSONUtil.toBean(jsonMessage, OrderEventMessage.class);
            if (ObjUtil.isNull(message) || StrUtil.isBlank(message.getOrderId())) {
                log.warn("消费订单事件JSON消息跳过，消息结构不完整，jsonMessage={}", jsonMessage);
                acknowledgment.acknowledge();
                return;
            }

            log.info("消费订单事件JSON消息成功，orderId={}，messageId={}，eventType={}",
                    message.getOrderId(),
                    message.getMessageId(),
                    message.getEventType());

            // 这里编写真实业务处理逻辑，例如更新订单状态、同步搜索索引、发送通知等

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("消费订单事件JSON消息失败，jsonMessage={}，原因={}", jsonMessage, e.getMessage(), e);
            throw e;
        }
    }

}
```

本地发送 JSON 消息可以继续使用前面章节中的接口。

```bash
curl -X POST 'http://localhost:8080/api/kafka/messages/order-event' \
  -H 'Content-Type: application/json' \
  -d '{
    "orderId": "ORDER_20001",
    "userId": 1001,
    "amount": 99.80,
    "eventType": "CREATED"
  }'
```

也可以通过 Kafka 控制台直接观察 JSON 字符串内容。

```bash
docker exec -it kafka-dev bash

/opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server 127.0.0.1:9092 \
  --topic order-event-topic \
  --from-beginning
```

控制台输出示例：

```json
{"messageId":"d15a6a26df5d4b2cae4a13b57c8d2a75","orderId":"ORDER_20001","userId":1001,"amount":99.8,"eventType":"CREATED","eventTime":"2026-04-30T10:30:00"}
```

JSON 字符串方式需要注意字段兼容性。新增字段时消费者通常可以忽略，删除字段或修改字段类型时可能导致旧消费者解析失败。生产环境建议对消息结构做版本管理，例如增加 `version` 字段。

### 自定义对象消息处理

自定义对象消息处理是指 Producer 直接发送 Java 对象，Consumer 直接接收 Java 对象。Spring Kafka 会通过 `JsonSerializer` 和 `JsonDeserializer` 自动完成对象和 JSON 字节之间的转换。

这种方式代码更简洁，但对生产消费双方的技术栈和类型配置有要求。它更适合生产者和消费者都由同一个 Java 技术体系维护的内部系统，不适合跨语言或公共事件 Topic。

自定义对象消息处理需要单独配置序列化器。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  kafka:
    producer:
      # 消息 Key 使用字符串序列化
      key-serializer: org.apache.kafka.common.serialization.StringSerializer

      # 消息 Value 使用 Spring Kafka JSON 序列化器
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

      properties:
        # 发送消息时不强制添加 Java 类型头，降低消费者对 Java 包名的依赖
        spring.json.add.type.headers: false

    consumer:
      # 消息 Key 使用字符串反序列化
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer

      # 消息 Value 使用 Spring Kafka JSON 反序列化器
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer

      properties:
        # 指定默认反序列化目标类型
        spring.json.value.default.type: io.github.atengk.kafka.dto.OrderEventMessage

        # 配置可信包，生产环境不要直接使用 *，应限制为业务包路径
        spring.json.trusted.packages: io.github.atengk.kafka.dto

        # 消费时不依赖生产者写入的类型头
        spring.json.use.type.headers: false
```

如果项目中同时存在 String 消息和对象消息，不建议在同一个 `KafkaTemplate<String, String>` 中混用。可以单独定义对象类型的 `KafkaTemplate<String, OrderEventMessage>`。

文件位置：`src/main/java/io/github/atengk/kafka/config/KafkaObjectProducerConfig.java`

下面的代码显式定义对象消息发送所需的 ProducerFactory 和 KafkaTemplate，避免与字符串消息配置互相影响。

```java
package io.github.atengk.kafka.config;

import io.github.atengk.kafka.dto.OrderEventMessage;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 对象消息生产者配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
@RequiredArgsConstructor
public class KafkaObjectProducerConfig {

    private final KafkaProperties kafkaProperties;

    /**
     * 创建订单事件对象消息 ProducerFactory。
     *
     * @return ProducerFactory 配置
     */
    @Bean
    public DefaultKafkaProducerFactory<String, OrderEventMessage> orderEventProducerFactory() {
        Map<String, Object> configMap = new HashMap<>(kafkaProperties.buildProducerProperties(null));

        configMap.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configMap.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // 不写入 Java 类型头，避免消费者强依赖生产者包名
        configMap.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaProducerFactory<>(configMap);
    }

    /**
     * 创建订单事件对象消息 KafkaTemplate。
     *
     * @return KafkaTemplate 实例
     */
    @Bean
    public KafkaTemplate<String, OrderEventMessage> orderEventKafkaTemplate() {
        return new KafkaTemplate<>(orderEventProducerFactory());
    }

}
```

对象消息生产者如下。

文件位置：`src/main/java/io/github/atengk/kafka/producer/ObjectOrderEventProducer.java`

下面的代码直接发送 `OrderEventMessage` 对象，不再手动转换 JSON 字符串。

```java
package io.github.atengk.kafka.producer;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.dto.OrderEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * 对象订单事件生产者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ObjectOrderEventProducer {

    private final KafkaTemplate<String, OrderEventMessage> orderEventKafkaTemplate;

    /**
     * 发送订单事件对象消息。
     *
     * @param message 订单事件消息
     * @return 发送结果
     */
    public CompletableFuture<?> sendOrderEvent(OrderEventMessage message) {
        if (ObjUtil.isNull(message)) {
            log.warn("发送订单事件对象消息失败，消息对象为空");
            throw new IllegalArgumentException("订单事件消息不能为空");
        }
        if (StrUtil.isBlank(message.getOrderId())) {
            log.warn("发送订单事件对象消息失败，订单ID为空");
            throw new IllegalArgumentException("订单ID不能为空");
        }

        if (StrUtil.isBlank(message.getMessageId())) {
            message.setMessageId(IdUtil.fastSimpleUUID());
        }
        if (ObjUtil.isNull(message.getEventTime())) {
            message.setEventTime(LocalDateTime.now());
        }

        log.info("开始发送订单事件对象消息，topic={}，key={}，messageId={}，eventType={}",
                KafkaTopicConstant.ORDER_EVENT_TOPIC,
                message.getOrderId(),
                message.getMessageId(),
                message.getEventType());

        return orderEventKafkaTemplate.send(KafkaTopicConstant.ORDER_EVENT_TOPIC, message.getOrderId(), message);
    }

}
```

对象消息消费者如下。

文件位置：`src/main/java/io/github/atengk/kafka/consumer/ObjectOrderEventConsumer.java`

下面的代码直接接收 `OrderEventMessage` 对象，适合生产者和消费者都使用相同消息结构的内部项目。

```java
package io.github.atengk.kafka.consumer;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.dto.OrderEventMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 对象订单事件消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class ObjectOrderEventConsumer {

    /**
     * 消费订单事件对象消息。
     *
     * @param message 订单事件消息
     * @param acknowledgment Offset 确认对象
     */
    @KafkaListener(
            topics = KafkaTopicConstant.ORDER_EVENT_TOPIC,
            groupId = KafkaTopicConstant.ORDER_EVENT_GROUP
    )
    public void consume(OrderEventMessage message, Acknowledgment acknowledgment) {
        try {
            if (ObjUtil.isNull(message) || StrUtil.isBlank(message.getOrderId())) {
                log.warn("消费订单事件对象消息跳过，消息为空或订单ID为空");
                acknowledgment.acknowledge();
                return;
            }

            log.info("消费订单事件对象消息成功，orderId={}，messageId={}，eventType={}",
                    message.getOrderId(),
                    message.getMessageId(),
                    message.getEventType());

            // 这里编写真实业务处理逻辑

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("消费订单事件对象消息失败，orderId={}，messageId={}，原因={}",
                    ObjUtil.isNull(message) ? null : message.getOrderId(),
                    ObjUtil.isNull(message) ? null : message.getMessageId(),
                    e.getMessage(),
                    e);
            throw e;
        }
    }

}
```

自定义对象消息处理虽然代码更直接，但需要注意以下问题：

| 问题           | 建议                                                      |
| -------------- | --------------------------------------------------------- |
| 消费端类型依赖 | 消费者必须知道目标 Java 类型，适合内部 Java 项目          |
| 包名变化       | 如果依赖类型头，包名变化可能导致反序列化失败              |
| 跨语言消费     | 不推荐直接依赖 Java 对象消息，应使用 JSON 字符串或 Schema |
| 可信包配置     | `spring.json.trusted.packages` 不建议生产环境配置为 `*`   |
| 字段兼容性     | 字段删除、改名、类型变更可能影响旧消费者                  |
| 多类型消息     | 不建议一个 Topic 混发过多不同对象类型                     |

生产环境中，如果消息需要被多个系统长期消费，建议优先使用 JSON 字符串，并通过消息版本号维护兼容性。例如：

```json
{
  "version": "1.0",
  "messageId": "d15a6a26df5d4b2cae4a13b57c8d2a75",
  "orderId": "ORDER_20001",
  "userId": 1001,
  "amount": 99.80,
  "eventType": "CREATED",
  "eventTime": "2026-04-30T10:30:00"
}
```

最终选型建议如下：

| 场景                | 推荐方式                      |
| ------------------- | ----------------------------- |
| 本地快速测试        | String 消息                   |
| 普通业务事件        | JSON 字符串                   |
| 跨语言系统消费      | JSON 字符串                   |
| 多系统长期订阅      | JSON 字符串 + 版本号          |
| Java 内部服务间通信 | JSON 字符串或自定义对象       |
| 强 Schema 约束      | Avro、Protobuf 或 JSON Schema |

如果没有明确理由，业务开发中建议默认采用“对象转 JSON 字符串发送，消费端按 JSON 字符串解析”的方式。这种方式配置简单、可读性强、排查方便，也更适合后续接入不同语言和不同平台的消费者。



## 消费位点管理

本章节用于说明 Kafka Consumer 的 Offset 管理方式。Offset 表示消费者在某个 Partition 中已经消费到的位置，Kafka 会基于 Consumer Group 维度记录每个 Partition 的消费进度。

消费位点管理直接影响消息是否会丢失、是否会重复消费、服务重启后从哪里继续消费，以及异常消息是否会阻塞后续消费。生产项目中，不能只关注“能不能消费到消息”，还需要明确“什么时候提交 Offset”和“重复消费时业务是否安全”。

Kafka 消费通常遵循至少一次投递语义。也就是说，只要配置和处理方式合理，消息通常不会丢失，但可能会重复消费。因此，业务消费者必须具备幂等处理能力。

### 自动提交 Offset

自动提交 Offset 是 Kafka Consumer 的默认简化使用方式。开启自动提交后，客户端会按照固定时间间隔自动提交当前消费进度。

这种方式配置简单，但存在明显风险：Offset 可能在业务逻辑真正处理完成前被提交。如果此时应用宕机或处理失败，Kafka 会认为消息已经消费完成，后续不会再投递这条消息，可能造成业务丢失。

自动提交配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  kafka:
    consumer:
      # 开启自动提交 Offset，适合本地测试或非关键消息场景
      enable-auto-commit: true

      # 自动提交间隔，单位毫秒
      auto-commit-interval: 1000

      # 没有已提交 Offset 时，从最早消息开始消费
      auto-offset-reset: earliest

      # 单次 poll 拉取最大消息数
      max-poll-records: 100

    listener:
      # 自动提交时不需要手动 acknowledgment
      ack-mode: record
```

自动提交模式下，消费代码不需要使用 `Acknowledgment`。

文件位置：`src/main/java/io/github/atengk/kafka/consumer/AutoCommitOrderEventConsumer.java`

下面的代码演示自动提交模式下的订单事件消费方式。

```java
package io.github.atengk.kafka.consumer;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.dto.OrderEventMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 自动提交订单事件消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class AutoCommitOrderEventConsumer {

    /**
     * 自动提交模式消费订单事件。
     *
     * @param record Kafka 消息记录
     */
    @KafkaListener(
            id = "autoCommitOrderEventConsumer",
            topics = KafkaTopicConstant.ORDER_EVENT_TOPIC,
            groupId = "order-event-auto-commit-group"
    )
    public void consume(ConsumerRecord<String, String> record) {
        String jsonMessage = record.value();

        if (StrUtil.isBlank(jsonMessage)) {
            log.warn("自动提交消费跳过空消息，topic={}，partition={}，offset={}",
                    record.topic(),
                    record.partition(),
                    record.offset());
            return;
        }

        OrderEventMessage message = JSONUtil.toBean(jsonMessage, OrderEventMessage.class);

        log.info("自动提交消费订单事件，orderId={}，messageId={}，eventType={}，partition={}，offset={}",
                message.getOrderId(),
                message.getMessageId(),
                message.getEventType(),
                record.partition(),
                record.offset());

        // 这里编写业务处理逻辑
    }

}
```

自动提交适合以下场景：

| 场景           | 说明                         |
| -------------- | ---------------------------- |
| 本地开发测试   | 快速验证消费者是否能读取消息 |
| 日志类消息     | 偶发丢失对业务影响较小       |
| 监控采样消息   | 消息价值较低，不要求严格处理 |
| 临时验证 Topic | 不涉及核心业务状态变更       |

不建议在以下场景使用自动提交：

| 场景         | 原因                                             |
| ------------ | ------------------------------------------------ |
| 订单状态处理 | 处理失败后可能因为 Offset 已提交导致订单状态丢失 |
| 支付结果通知 | 支付消息丢失会造成严重业务不一致                 |
| 库存扣减     | 失败后不重试可能导致库存状态错误                 |
| 资金流水     | 对可靠性和可追溯性要求较高                       |
| 需要失败重试 | 自动提交不便于准确控制失败消息位点               |

自动提交可以减少代码复杂度，但可靠性控制较弱。生产业务建议优先使用手动提交 Offset。

### 手动提交 Offset

手动提交 Offset 是生产环境更常用的方式。消费者在业务逻辑处理成功后，主动调用 `Acknowledgment#acknowledge()` 提交 Offset。

手动提交的核心原则是：先处理业务，业务成功后再提交 Offset。业务失败时不要提交 Offset，而是抛出异常交给异常处理器进行重试、死信投递或补偿处理。

手动提交配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  kafka:
    consumer:
      # 关闭自动提交，避免业务未处理完成时 Offset 被提前提交
      enable-auto-commit: false

      # 没有已提交 Offset 时，从最早位置开始消费
      auto-offset-reset: earliest

      # 单次 poll 拉取最大消息数
      max-poll-records: 100

      properties:
        # 单次 poll 后允许的最大处理间隔，业务处理较慢时需要适当调大
        max.poll.interval.ms: 300000

        # Consumer 心跳间隔
        heartbeat.interval.ms: 3000

        # Consumer 会话超时时间，超过后会触发 Rebalance
        session.timeout.ms: 45000

    listener:
      # 手动提交模式，业务代码调用 acknowledge 后提交 Offset
      ack-mode: manual

      # 单条消息消费模式
      type: single

      # 消费并发数，建议不超过 Topic 分区数
      concurrency: 3
```

手动提交单条消费代码如下。

文件位置：`src/main/java/io/github/atengk/kafka/consumer/ManualCommitOrderEventConsumer.java`

下面的代码演示业务处理成功后手动提交 Offset，处理失败时抛出异常，不提交消费位点。

```java
package io.github.atengk.kafka.consumer;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.dto.OrderEventMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 手动提交订单事件消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class ManualCommitOrderEventConsumer {

    /**
     * 手动提交模式消费订单事件。
     *
     * @param record Kafka 消息记录
     * @param acknowledgment Offset 确认对象
     */
    @KafkaListener(
            id = "manualCommitOrderEventConsumer",
            topics = KafkaTopicConstant.ORDER_EVENT_TOPIC,
            groupId = "order-event-manual-commit-group"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        String jsonMessage = record.value();

        try {
            log.info("开始手动提交消费订单事件，topic={}，partition={}，offset={}，key={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key());

            if (StrUtil.isBlank(jsonMessage)) {
                log.warn("手动提交消费跳过空消息，topic={}，partition={}，offset={}",
                        record.topic(),
                        record.partition(),
                        record.offset());
                acknowledgment.acknowledge();
                return;
            }

            OrderEventMessage message = JSONUtil.toBean(jsonMessage, OrderEventMessage.class);
            if (ObjUtil.isNull(message) || StrUtil.isBlank(message.getOrderId())) {
                log.warn("手动提交消费跳过无效消息，topic={}，partition={}，offset={}，message={}",
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        jsonMessage);
                acknowledgment.acknowledge();
                return;
            }

            this.handleOrderEvent(message);

            // 业务处理成功后再提交 Offset
            acknowledgment.acknowledge();

            log.info("手动提交消费订单事件完成，orderId={}，messageId={}，partition={}，offset={}",
                    message.getOrderId(),
                    message.getMessageId(),
                    record.partition(),
                    record.offset());
        } catch (Exception e) {
            log.error("手动提交消费订单事件失败，topic={}，partition={}，offset={}，message={}，原因={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    jsonMessage,
                    e.getMessage(),
                    e);

            // 失败时不提交 Offset，交给异常处理器重试或投递死信 Topic
            throw e;
        }
    }

    /**
     * 处理订单事件业务逻辑。
     *
     * @param message 订单事件消息
     */
    private void handleOrderEvent(OrderEventMessage message) {
        if ("ERROR".equals(message.getEventType())) {
            throw new IllegalStateException("模拟订单事件处理失败");
        }

        log.info("执行业务处理，orderId={}，messageId={}，eventType={}",
                message.getOrderId(),
                message.getMessageId(),
                message.getEventType());
    }

}
```

`ack-mode` 常用模式说明如下：

| 模式               | 说明                                  | 适用场景                     |
| ------------------ | ------------------------------------- | ---------------------------- |
| `record`           | 每条记录处理后提交                    | 自动管理为主，代码不显式确认 |
| `batch`            | 每批记录处理后提交                    | 批量消费场景                 |
| `manual`           | 调用 `acknowledge()` 后由容器管理提交 | 常用手动提交方式             |
| `manual_immediate` | 调用 `acknowledge()` 后尽快立即提交   | 对提交时机要求更明确的场景   |

如果业务要求调用 `acknowledge()` 后尽快提交 Offset，可以使用 `manual_immediate`。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  kafka:
    listener:
      # 调用 acknowledge 后立即提交 Offset
      ack-mode: manual_immediate
```

批量消费时也可以使用手动提交。整批处理成功后再提交 Offset。

文件位置：`src/main/java/io/github/atengk/kafka/consumer/ManualCommitBatchOrderEventConsumer.java`

下面的代码演示批量消费场景中的手动提交方式。

```java
package io.github.atengk.kafka.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.dto.OrderEventMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 手动提交批量订单事件消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class ManualCommitBatchOrderEventConsumer {

    /**
     * 批量消费订单事件并手动提交 Offset。
     *
     * @param records Kafka 消息记录列表
     * @param acknowledgment Offset 确认对象
     */
    @KafkaListener(
            id = "manualCommitBatchOrderEventConsumer",
            topics = KafkaTopicConstant.ORDER_EVENT_TOPIC,
            groupId = "order-event-manual-batch-group",
            containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void consumeBatch(List<ConsumerRecord<String, String>> records, Acknowledgment acknowledgment) {
        if (CollUtil.isEmpty(records)) {
            log.warn("批量手动提交消费跳过，消息列表为空");
            acknowledgment.acknowledge();
            return;
        }

        try {
            log.info("开始批量手动提交消费订单事件，count={}", records.size());

            for (ConsumerRecord<String, String> record : records) {
                String jsonMessage = record.value();
                if (StrUtil.isBlank(jsonMessage)) {
                    log.warn("批量消费跳过空消息，partition={}，offset={}",
                            record.partition(),
                            record.offset());
                    continue;
                }

                OrderEventMessage message = JSONUtil.toBean(jsonMessage, OrderEventMessage.class);
                log.info("批量处理订单事件，orderId={}，messageId={}，eventType={}，partition={}，offset={}",
                        message.getOrderId(),
                        message.getMessageId(),
                        message.getEventType(),
                        record.partition(),
                        record.offset());
            }

            // 整批处理成功后统一提交 Offset
            acknowledgment.acknowledge();

            log.info("批量手动提交消费订单事件完成，count={}", records.size());
        } catch (Exception e) {
            log.error("批量手动提交消费订单事件失败，count={}，原因={}",
                    records.size(),
                    e.getMessage(),
                    e);
            throw e;
        }
    }

}
```

手动提交需要注意以下问题：

| 问题             | 说明                                                     |
| ---------------- | -------------------------------------------------------- |
| 处理成功后提交   | 必须在业务逻辑成功后调用 `acknowledge()`                 |
| 失败不要提交     | 失败时抛出异常，由异常处理器重试或进入死信 Topic         |
| 空消息是否提交   | 无业务价值的空消息可以记录后提交，避免反复消费           |
| 非法消息是否提交 | 可根据业务选择提交跳过或进入死信 Topic                   |
| 批量消费重复     | 批量中后几条失败时，前面已处理成功的消息可能被重复消费   |
| Rebalance 影响   | 消费过程中发生 Rebalance，未提交的 Offset 可能被重新消费 |

手动提交不能彻底避免重复消费。它只是让 Offset 的推进更受业务控制，最终仍然需要幂等处理保证业务安全。

### 重复消费与幂等处理

重复消费是 Kafka 开发中必须默认考虑的问题。只要使用 Kafka，就不能假设每条消息只会被消费一次。

常见重复消费原因如下：

| 原因                             | 说明                                       |
| -------------------------------- | ------------------------------------------ |
| 业务成功后提交 Offset 前应用宕机 | 业务已执行，但 Kafka 不知道消息已完成      |
| Consumer Rebalance               | 分区重新分配后，未提交位点可能重新消费     |
| 批量消费部分失败                 | 前面处理成功的消息因为整批未提交而再次消费 |
| Producer 重试                    | 发送端重试可能产生重复业务消息             |
| 人工重放消息                     | 排查或补偿时重新投递历史消息               |
| 死信补偿                         | 死信消息修复后重新投递到原 Topic           |

幂等处理的目标是：同一条业务消息无论消费一次还是多次，最终业务结果都保持一致。

推荐在消息体中保留 `messageId` 字段，作为消息级唯一标识。

```json
{
  "messageId": "89d4bbf24d914ba6900c8f73373d4f80",
  "orderId": "ORDER_40001",
  "userId": 1001,
  "amount": 99.80,
  "eventType": "PAID",
  "eventTime": "2026-04-30T10:30:00"
}
```

常见幂等方案如下：

| 方案           | 说明                                   | 适用场景                   |
| -------------- | -------------------------------------- | -------------------------- |
| Redis 去重     | 使用 `SETNX` 记录消息 ID，设置过期时间 | 高吞吐、允许按时间窗口去重 |
| 数据库唯一索引 | 使用 `message_id` 唯一索引防止重复入库 | 关键业务、需要长期可追溯   |
| 业务状态判断   | 根据订单状态判断是否允许重复执行       | 状态机类业务               |
| 乐观锁更新     | 更新时带版本号或状态条件               | 库存、账户、订单状态       |
| 本地消息表     | 记录消息处理状态和补偿信息             | 高可靠业务处理             |

如果项目已经使用 Redis，可以用 Redis 实现轻量级幂等处理。

Maven 依赖如下。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Data Redis：用于实现 Kafka 消息消费幂等去重 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- Hutool：用于字符串、对象、JSON、ID 等工具处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.32</version>
    </dependency>
</dependencies>
```

Redis 配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  data:
    redis:
      # Redis 服务地址
      host: 127.0.0.1

      # Redis 服务端口
      port: 6379

      # Redis 数据库索引
      database: 0

      # Redis 连接超时时间
      timeout: 3000ms
```

幂等服务代码如下。

文件位置：`src/main/java/io/github/atengk/kafka/service/KafkaMessageIdempotentService.java`

下面的代码使用 Redis `SETNX` 实现消息处理前置去重，重复消息会被直接识别并跳过。

```java
package io.github.atengk.kafka.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Kafka 消息幂等服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaMessageIdempotentService {

    private static final String MESSAGE_IDEMPOTENT_KEY_PREFIX = "kafka:message:idempotent:";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试标记消息正在处理。
     *
     * @param messageId 消息ID
     * @return true 表示首次处理，false 表示重复消息
     */
    public boolean tryMarkProcessing(String messageId) {
        if (StrUtil.isBlank(messageId)) {
            log.warn("消息幂等标记失败，messageId为空");
            return false;
        }

        String key = MESSAGE_IDEMPOTENT_KEY_PREFIX + messageId;

        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "PROCESSING", Duration.ofHours(24));

        if (Boolean.TRUE.equals(success)) {
            log.info("消息幂等标记成功，messageId={}", messageId);
            return true;
        }

        log.warn("检测到重复Kafka消息，messageId={}", messageId);
        return false;
    }

    /**
     * 标记消息处理成功。
     *
     * @param messageId 消息ID
     */
    public void markSuccess(String messageId) {
        if (StrUtil.isBlank(messageId)) {
            return;
        }

        String key = MESSAGE_IDEMPOTENT_KEY_PREFIX + messageId;

        // 成功状态保留 7 天，避免短期内重复消息再次处理
        stringRedisTemplate.opsForValue().set(key, "SUCCESS", Duration.ofDays(7));

        log.info("消息幂等处理完成，messageId={}", messageId);
    }

    /**
     * 清理处理失败的幂等标记。
     *
     * @param messageId 消息ID
     */
    public void clearProcessing(String messageId) {
        if (StrUtil.isBlank(messageId)) {
            return;
        }

        String key = MESSAGE_IDEMPOTENT_KEY_PREFIX + messageId;
        stringRedisTemplate.delete(key);

        log.warn("消息处理失败，已清理幂等标记，messageId={}", messageId);
    }

}
```

在消费者中接入幂等服务。

文件位置：`src/main/java/io/github/atengk/kafka/consumer/IdempotentOrderEventConsumer.java`

下面的代码在业务处理前先检查 `messageId`，重复消息直接提交 Offset，不再重复执行业务逻辑。

```java
package io.github.atengk.kafka.consumer;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.dto.OrderEventMessage;
import io.github.atengk.kafka.service.KafkaMessageIdempotentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 幂等订单事件消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotentOrderEventConsumer {

    private final KafkaMessageIdempotentService kafkaMessageIdempotentService;

    /**
     * 幂等消费订单事件。
     *
     * @param record Kafka 消息记录
     * @param acknowledgment Offset 确认对象
     */
    @KafkaListener(
            id = "idempotentOrderEventConsumer",
            topics = KafkaTopicConstant.ORDER_EVENT_TOPIC,
            groupId = "order-event-idempotent-group"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        OrderEventMessage message = null;

        try {
            String jsonMessage = record.value();
            if (StrUtil.isBlank(jsonMessage)) {
                log.warn("幂等消费跳过空消息，partition={}，offset={}",
                        record.partition(),
                        record.offset());
                acknowledgment.acknowledge();
                return;
            }

            message = JSONUtil.toBean(jsonMessage, OrderEventMessage.class);
            if (ObjUtil.isNull(message) || StrUtil.isBlank(message.getMessageId())) {
                log.warn("幂等消费跳过无效消息，partition={}，offset={}，message={}",
                        record.partition(),
                        record.offset(),
                        jsonMessage);
                acknowledgment.acknowledge();
                return;
            }

            boolean firstConsume = kafkaMessageIdempotentService.tryMarkProcessing(message.getMessageId());
            if (!firstConsume) {
                log.warn("重复订单事件消息已跳过，orderId={}，messageId={}，partition={}，offset={}",
                        message.getOrderId(),
                        message.getMessageId(),
                        record.partition(),
                        record.offset());
                acknowledgment.acknowledge();
                return;
            }

            this.handleOrderEvent(message);

            kafkaMessageIdempotentService.markSuccess(message.getMessageId());

            // 幂等标记和业务处理都成功后，再提交 Offset
            acknowledgment.acknowledge();

            log.info("幂等消费订单事件完成，orderId={}，messageId={}，eventType={}，partition={}，offset={}",
                    message.getOrderId(),
                    message.getMessageId(),
                    message.getEventType(),
                    record.partition(),
                    record.offset());
        } catch (Exception e) {
            if (ObjUtil.isNotNull(message)) {
                kafkaMessageIdempotentService.clearProcessing(message.getMessageId());
            }

            log.error("幂等消费订单事件失败，partition={}，offset={}，原因={}",
                    record.partition(),
                    record.offset(),
                    e.getMessage(),
                    e);

            throw e;
        }
    }

    /**
     * 处理订单事件业务逻辑。
     *
     * @param message 订单事件消息
     */
    private void handleOrderEvent(OrderEventMessage message) {
        log.info("执行幂等业务处理，orderId={}，messageId={}，eventType={}",
                message.getOrderId(),
                message.getMessageId(),
                message.getEventType());

        // 示例：根据 eventType 更新订单状态、写入流水、发送通知等
    }

}
```

Redis 幂等方案适合高吞吐场景，但需要注意过期时间。如果消息可能在 7 天后被人工重放，而业务仍然要求去重，则仅靠 Redis 过期 Key 不够，需要使用数据库唯一索引或消息处理记录表。

数据库幂等可以通过唯一索引实现。

文件位置：`sql/kafka_message_record.sql`

下面的 SQL 用于创建 Kafka 消息处理记录表，通过 `message_id` 唯一索引防止重复处理。

```sql
CREATE TABLE kafka_message_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息ID',
    topic_name VARCHAR(128) NOT NULL COMMENT 'Topic名称',
    consumer_group VARCHAR(128) NOT NULL COMMENT '消费者组',
    business_key VARCHAR(128) DEFAULT NULL COMMENT '业务主键，例如订单ID',
    process_status VARCHAR(32) NOT NULL COMMENT '处理状态：PROCESSING、SUCCESS、FAILED',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '失败原因',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_message_id_consumer_group (message_id, consumer_group),
    KEY idx_business_key (business_key),
    KEY idx_topic_group (topic_name, consumer_group)
) COMMENT='Kafka消息处理记录表';
```

数据库唯一索引幂等适合关键业务，尤其是订单、支付、库存、账户等需要长期可追溯的场景。即使 Redis 数据过期，数据库仍然可以判断消息是否已经处理过。

消费位点和幂等处理建议如下：

| 场景         | 推荐处理方式                             |
| ------------ | ---------------------------------------- |
| 本地测试     | 可以使用自动提交                         |
| 普通业务消息 | 手动提交 Offset + Redis 幂等             |
| 核心业务消息 | 手动提交 Offset + 数据库唯一索引         |
| 批量消费     | 整批提交 Offset + 每条消息独立幂等       |
| 异常消息     | 有限重试后进入死信 Topic                 |
| 人工重放     | 依赖业务幂等或数据库处理记录防止重复执行 |

最终建议是：生产环境默认关闭自动提交，业务处理成功后手动提交 Offset，并且所有消费者都按可能重复消费来设计。Offset 管理解决的是“消费进度推进时机”，幂等处理解决的是“重复消费时业务是否安全”，两者需要同时设计，不能互相替代。



## 常用开发场景

本章节用于说明 Kafka 在 Spring Boot 3 项目中的几个高频开发场景。前面章节已经完成了基础发送、消费、序列化、Offset 管理和幂等处理，本章节在这些基础上继续补充生产项目中更常见的组合用法。

实际项目中，Kafka 消费并不只是“收到消息后处理完成”这么简单。更常见的情况是：消息处理失败需要延迟重试，重试耗尽需要进入死信 Topic，不同业务需要监听多个 Topic，不同系统需要使用不同 Consumer Group 消费同一批消息。

本章节涉及的关键文件如下：

```text
src/main/java/io/github/atengk/kafka/constant/KafkaTopicConstant.java
src/main/java/io/github/atengk/kafka/consumer/RetryOrderEventConsumer.java
src/main/java/io/github/atengk/kafka/consumer/OrderEventDltConsumer.java
src/main/java/io/github/atengk/kafka/consumer/MultiTopicEventConsumer.java
src/main/java/io/github/atengk/kafka/consumer/ReportOrderEventConsumer.java
src/main/java/io/github/atengk/kafka/consumer/SearchOrderEventConsumer.java
src/main/java/io/github/atengk/kafka/config/KafkaTopicConfig.java
```

为了便于后续示例统一引用，建议先扩展 Topic 常量。

文件位置：`src/main/java/io/github/atengk/kafka/constant/KafkaTopicConstant.java`

下面的代码统一维护订单事件、用户事件、支付事件和死信 Topic 常量，避免在消费者中硬编码字符串。

```java
package io.github.atengk.kafka.constant;

/**
 * Kafka Topic 常量
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class KafkaTopicConstant {

    /**
     * 订单事件 Topic
     */
    public static final String ORDER_EVENT_TOPIC = "order-event-topic";

    /**
     * 用户事件 Topic
     */
    public static final String USER_EVENT_TOPIC = "user-event-topic";

    /**
     * 支付事件 Topic
     */
    public static final String PAYMENT_EVENT_TOPIC = "payment-event-topic";

    /**
     * 订单事件死信 Topic
     */
    public static final String ORDER_EVENT_DLT_TOPIC = "order-event-topic.DLT";

    /**
     * 用户事件死信 Topic
     */
    public static final String USER_EVENT_DLT_TOPIC = "user-event-topic.DLT";

    /**
     * 订单事件默认消费者组
     */
    public static final String ORDER_EVENT_GROUP = "order-event-group";

    /**
     * 报表服务消费者组
     */
    public static final String REPORT_ORDER_EVENT_GROUP = "report-order-event-group";

    /**
     * 搜索服务消费者组
     */
    public static final String SEARCH_ORDER_EVENT_GROUP = "search-order-event-group";

    /**
     * 多 Topic 事件消费者组
     */
    public static final String MULTI_TOPIC_EVENT_GROUP = "multi-topic-event-group";

    private KafkaTopicConstant() {
    }

}
```

### 延迟重试处理

延迟重试用于处理消费失败后暂时不立即再次消费的场景。例如数据库短暂不可用、第三方接口超时、远程服务限流、依赖系统发布中等异常，这类问题通常不是消息本身错误，而是外部环境短时间不可用。

Kafka 本身没有传统消息队列中“延迟消息”的原生语义。Spring Kafka 提供了 `@RetryableTopic` 能力，可以通过创建重试 Topic 的方式实现延迟重试：消费者处理失败后，消息会被转发到重试 Topic，等待指定时间后再次投递给消费者，超过最大重试次数后进入 DLT。

延迟重试适合以下场景：

| 场景             | 说明                     |
| ---------------- | ------------------------ |
| 外部接口超时     | 短时间后重试可能成功     |
| 数据库连接异常   | 依赖恢复后可继续处理     |
| Redis 短暂不可用 | 等待缓存服务恢复         |
| 下游系统限流     | 延迟后降低失败概率       |
| 业务状态尚未就绪 | 例如支付回调早于订单落库 |

不适合延迟重试的场景：

| 场景          | 建议                              |
| ------------- | --------------------------------- |
| JSON 格式错误 | 直接进入死信 Topic 或记录异常消息 |
| 必填字段缺失  | 属于无效消息，重试意义不大        |
| 代码逻辑缺陷  | 应修复代码，不应无限重试          |
| 永久业务失败  | 例如订单不存在且不会补偿创建      |

使用 `@RetryableTopic` 前，需要确保项目已经引入 `spring-kafka`。如果前面章节已经配置过，则不需要重复引入。

文件位置：`src/main/java/io/github/atengk/kafka/consumer/RetryOrderEventConsumer.java`

下面的代码使用 `@RetryableTopic` 实现订单事件延迟重试，失败后按固定间隔重试，最终进入 DLT。

```java
package io.github.atengk.kafka.consumer;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.dto.OrderEventMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * 订单事件延迟重试消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class RetryOrderEventConsumer {

    /**
     * 消费订单事件消息，失败后进入延迟重试流程。
     *
     * @param record Kafka 消息记录
     * @param acknowledgment Offset 确认对象
     */
    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 3000, multiplier = 2.0),
            autoCreateTopics = "true"
    )
    @KafkaListener(
            id = "retryOrderEventConsumer",
            topics = KafkaTopicConstant.ORDER_EVENT_TOPIC,
            groupId = "order-event-retry-group"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        String jsonMessage = record.value();

        try {
            log.info("开始消费订单事件重试消息，topic={}，partition={}，offset={}，key={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key());

            if (StrUtil.isBlank(jsonMessage)) {
                log.warn("订单事件重试消息为空，直接提交Offset，partition={}，offset={}",
                        record.partition(),
                        record.offset());
                acknowledgment.acknowledge();
                return;
            }

            OrderEventMessage message = JSONUtil.toBean(jsonMessage, OrderEventMessage.class);
            if (ObjUtil.isNull(message) || StrUtil.isBlank(message.getOrderId())) {
                log.warn("订单事件重试消息结构不完整，直接提交Offset，partition={}，offset={}，message={}",
                        record.partition(),
                        record.offset(),
                        jsonMessage);
                acknowledgment.acknowledge();
                return;
            }

            this.handleOrderEvent(message);

            acknowledgment.acknowledge();

            log.info("订单事件重试消息消费成功，orderId={}，messageId={}，eventType={}，partition={}，offset={}",
                    message.getOrderId(),
                    message.getMessageId(),
                    message.getEventType(),
                    record.partition(),
                    record.offset());
        } catch (Exception e) {
            log.error("订单事件重试消息消费失败，topic={}，partition={}，offset={}，message={}，原因={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    jsonMessage,
                    e.getMessage(),
                    e);

            // 抛出异常后，Spring Kafka 会根据 @RetryableTopic 配置转入重试 Topic
            throw e;
        }
    }

    /**
     * 处理订单事件业务逻辑。
     *
     * @param message 订单事件消息
     */
    private void handleOrderEvent(OrderEventMessage message) {
        if ("RETRY_ERROR".equals(message.getEventType())) {
            throw new IllegalStateException("模拟订单事件延迟重试异常");
        }

        log.info("处理订单事件重试消息，orderId={}，messageId={}，eventType={}",
                message.getOrderId(),
                message.getMessageId(),
                message.getEventType());
    }

    /**
     * 处理延迟重试耗尽后的死信消息。
     *
     * @param record Kafka 消息记录
     */
    @DltHandler
    public void dltHandler(ConsumerRecord<String, String> record) {
        log.error("订单事件延迟重试耗尽，进入DLT处理，topic={}，partition={}，offset={}，key={}，value={}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                record.value());

        // 生产环境可在这里写入失败消息表、触发告警、通知人工处理或进入补偿流程
    }

}
```

`@RetryableTopic` 中关键参数说明如下：

| 参数                 | 说明                           |
| -------------------- | ------------------------------ |
| `attempts`           | 总尝试次数，包含第一次正常消费 |
| `backoff.delay`      | 第一次重试延迟时间，单位毫秒   |
| `backoff.multiplier` | 重试间隔倍数，用于指数退避     |
| `autoCreateTopics`   | 是否自动创建重试 Topic 和 DLT  |
| `@DltHandler`        | 重试耗尽后的死信处理方法       |

以上示例中，`attempts = "4"` 表示最多尝试 4 次，即首次消费 1 次，加上后续重试 3 次。`delay = 3000` 和 `multiplier = 2.0` 表示重试间隔大致按 3 秒、6 秒、12 秒递增。

发送一条会触发重试的消息。

```bash
curl -X POST 'http://localhost:8080/api/kafka/messages/order-event' \
  -H 'Content-Type: application/json' \
  -d '{
    "orderId": "ORDER_RETRY_50001",
    "userId": 1001,
    "amount": 99.80,
    "eventType": "RETRY_ERROR"
  }'
```

预期现象如下：

| 阶段       | 现象                    |
| ---------- | ----------------------- |
| 首次消费   | 业务抛出异常            |
| 第一次重试 | 延迟约 3 秒后再次消费   |
| 第二次重试 | 延迟约 6 秒后再次消费   |
| 第三次重试 | 延迟约 12 秒后再次消费  |
| 重试耗尽   | 进入 `@DltHandler` 方法 |

延迟重试需要注意以下问题：

| 问题       | 建议                                                |
| ---------- | --------------------------------------------------- |
| 重试次数   | 不宜过多，避免大量失败消息堆积                      |
| 重试间隔   | 短故障用秒级，外部系统恢复慢时可用分钟级            |
| 幂等处理   | 每次重试都会重新执行业务逻辑，必须保证幂等          |
| 异常分类   | 只有临时异常适合重试，永久异常应直接进入死信        |
| Topic 管理 | 生产环境是否允许自动创建重试 Topic 需要遵循平台规范 |

### 死信 Topic 处理

死信 Topic 用于保存最终无法正常消费的消息。消息进入死信 Topic 后，主消费链路可以继续向后处理，避免某一条异常消息长期阻塞同一个 Partition。

常见进入死信 Topic 的原因如下：

| 原因         | 说明                                     |
| ------------ | ---------------------------------------- |
| 消息格式错误 | JSON 无法解析或字段类型错误              |
| 必填字段缺失 | 缺少订单 ID、用户 ID、事件类型等关键字段 |
| 业务永久失败 | 例如订单不存在且无法补偿                 |
| 重试耗尽     | 多次重试仍失败                           |
| 代码异常     | 消费逻辑存在缺陷，需要人工排查           |

死信 Topic 通常使用固定命名规则，例如：

```text
order-event-topic.DLT
user-event-topic.DLT
payment-event-topic.DLT
```

可以通过 `NewTopic` 提前创建主 Topic 和死信 Topic。

文件位置：`src/main/java/io/github/atengk/kafka/config/KafkaTopicConfig.java`

下面的代码用于声明主业务 Topic 和死信 Topic，本地开发环境可以自动创建。

```java
package io.github.atengk.kafka.config;

import io.github.atengk.kafka.constant.KafkaTopicConstant;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Kafka Topic 配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
public class KafkaTopicConfig {

    /**
     * 创建订单事件 Topic。
     *
     * @return Topic 配置
     */
    @Bean
    public NewTopic orderEventTopic() {
        return new NewTopic(KafkaTopicConstant.ORDER_EVENT_TOPIC, 3, (short) 1);
    }

    /**
     * 创建用户事件 Topic。
     *
     * @return Topic 配置
     */
    @Bean
    public NewTopic userEventTopic() {
        return new NewTopic(KafkaTopicConstant.USER_EVENT_TOPIC, 3, (short) 1);
    }

    /**
     * 创建支付事件 Topic。
     *
     * @return Topic 配置
     */
    @Bean
    public NewTopic paymentEventTopic() {
        return new NewTopic(KafkaTopicConstant.PAYMENT_EVENT_TOPIC, 3, (short) 1);
    }

    /**
     * 创建订单事件死信 Topic。
     *
     * @return Topic 配置
     */
    @Bean
    public NewTopic orderEventDltTopic() {
        return new NewTopic(KafkaTopicConstant.ORDER_EVENT_DLT_TOPIC, 3, (short) 1);
    }

    /**
     * 创建用户事件死信 Topic。
     *
     * @return Topic 配置
     */
    @Bean
    public NewTopic userEventDltTopic() {
        return new NewTopic(KafkaTopicConstant.USER_EVENT_DLT_TOPIC, 3, (short) 1);
    }

}
```

生产环境中，Topic 通常由 Kafka 平台或运维提前创建，不建议业务应用随意自动创建。自动创建适合本地开发、测试环境或独立 Demo 项目。

死信 Topic 消费者如下。

文件位置：`src/main/java/io/github/atengk/kafka/consumer/OrderEventDltConsumer.java`

下面的代码用于消费订单事件死信消息，记录完整 Kafka 元数据和原始消息内容。

```java
package io.github.atengk.kafka.consumer;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 订单事件死信消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class OrderEventDltConsumer {

    /**
     * 消费订单事件死信消息。
     *
     * @param record Kafka 消息记录
     */
    @KafkaListener(
            id = "orderEventDltConsumer",
            topics = KafkaTopicConstant.ORDER_EVENT_DLT_TOPIC,
            groupId = "order-event-dlt-group"
    )
    public void consumeDlt(ConsumerRecord<String, String> record) {
        String value = record.value();

        if (StrUtil.isBlank(value)) {
            log.warn("收到空死信消息，topic={}，partition={}，offset={}，time={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    DateUtil.now());
            return;
        }

        log.error("收到订单事件死信消息，topic={}，partition={}，offset={}，key={}，timestamp={}，value={}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                record.timestamp(),
                value);

        // 生产环境建议在这里落库，例如写入 kafka_failed_message 表
        // 后续可由管理后台、定时任务或人工操作重新投递
    }

}
```

如果死信消息需要落库，建议记录以下字段：

| 字段             | 说明                                    |
| ---------------- | --------------------------------------- |
| `message_id`     | 消息唯一 ID，便于幂等和追踪             |
| `source_topic`   | 原始 Topic                              |
| `dlt_topic`      | 死信 Topic                              |
| `partition_no`   | 原始分区                                |
| `offset_no`      | 原始 Offset                             |
| `message_key`    | 消息 Key                                |
| `message_body`   | 原始消息内容                            |
| `error_message`  | 异常信息                                |
| `process_status` | 处理状态，例如 WAITING、SUCCESS、FAILED |
| `retry_count`    | 人工或补偿重试次数                      |
| `create_time`    | 创建时间                                |
| `update_time`    | 更新时间                                |

死信消息表可以参考下面的结构。

文件位置：`sql/kafka_failed_message.sql`

下面的 SQL 用于创建 Kafka 死信消息记录表，方便后续人工处理和补偿重放。

```sql
CREATE TABLE kafka_failed_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(64) DEFAULT NULL COMMENT '消息ID',
    source_topic VARCHAR(128) NOT NULL COMMENT '原始Topic',
    dlt_topic VARCHAR(128) NOT NULL COMMENT '死信Topic',
    partition_no INT NOT NULL COMMENT '分区编号',
    offset_no BIGINT NOT NULL COMMENT 'Offset位点',
    message_key VARCHAR(256) DEFAULT NULL COMMENT '消息Key',
    message_body TEXT NOT NULL COMMENT '消息内容',
    error_message VARCHAR(2000) DEFAULT NULL COMMENT '异常信息',
    process_status VARCHAR(32) NOT NULL DEFAULT 'WAITING' COMMENT '处理状态：WAITING、SUCCESS、FAILED',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '补偿重试次数',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_topic_partition_offset (source_topic, partition_no, offset_no),
    KEY idx_message_id (message_id),
    KEY idx_process_status (process_status)
) COMMENT='Kafka死信消息记录表';
```

死信 Topic 处理建议如下：

| 处理点           | 建议                                         |
| ---------------- | -------------------------------------------- |
| 是否消费 DLT     | 建议消费并落库，不要只让消息堆在 Topic 中    |
| 是否告警         | 核心业务死信应触发告警                       |
| 是否自动重放     | 只对确认可恢复的异常做自动重放               |
| 是否人工审核     | 资金、支付、库存等关键消息建议人工确认后重放 |
| 是否保留原始消息 | 必须保留，便于排查和补偿                     |
| 是否记录 Offset  | 建议记录原始 Topic、Partition、Offset        |

死信 Topic 不代表消息已经处理成功，它只是把主链路中处理不了的消息隔离出来。进入死信 Topic 后仍然需要有后续处理机制，例如落库、告警、人工修复、补偿重放或废弃归档。

### 多 Topic 消费

多 Topic 消费用于一个消费者同时监听多个 Topic。适合多个 Topic 的消息处理逻辑相似，或者需要统一汇聚不同业务事件的场景。

常见场景如下：

| 场景         | 说明                              |
| ------------ | --------------------------------- |
| 统一事件中心 | 同时消费订单、用户、支付等事件    |
| 日志聚合     | 多个日志 Topic 统一写入日志平台   |
| 监控事件处理 | 多种业务告警事件进入统一处理器    |
| 数据同步     | 多个业务 Topic 统一同步到数据仓库 |

多 Topic 消费可以通过 `@KafkaListener(topics = {...})` 实现。

文件位置：`src/main/java/io/github/atengk/kafka/consumer/MultiTopicEventConsumer.java`

下面的代码同时监听订单事件、用户事件和支付事件，并根据 Topic 分发到不同处理方法。

```java
package io.github.atengk.kafka.consumer;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 多 Topic 事件消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class MultiTopicEventConsumer {

    /**
     * 消费多个业务 Topic 的事件消息。
     *
     * @param record Kafka 消息记录
     * @param acknowledgment Offset 确认对象
     */
    @KafkaListener(
            id = "multiTopicEventConsumer",
            topics = {
                    KafkaTopicConstant.ORDER_EVENT_TOPIC,
                    KafkaTopicConstant.USER_EVENT_TOPIC,
                    KafkaTopicConstant.PAYMENT_EVENT_TOPIC
            },
            groupId = KafkaTopicConstant.MULTI_TOPIC_EVENT_GROUP
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            String topic = record.topic();
            String message = record.value();

            log.info("开始消费多Topic消息，topic={}，partition={}，offset={}，key={}",
                    topic,
                    record.partition(),
                    record.offset(),
                    record.key());

            if (StrUtil.isBlank(message)) {
                log.warn("多Topic消息为空，直接提交Offset，topic={}，partition={}，offset={}",
                        topic,
                        record.partition(),
                        record.offset());
                acknowledgment.acknowledge();
                return;
            }

            if (KafkaTopicConstant.ORDER_EVENT_TOPIC.equals(topic)) {
                this.handleOrderEvent(message);
            } else if (KafkaTopicConstant.USER_EVENT_TOPIC.equals(topic)) {
                this.handleUserEvent(message);
            } else if (KafkaTopicConstant.PAYMENT_EVENT_TOPIC.equals(topic)) {
                this.handlePaymentEvent(message);
            } else {
                log.warn("未识别的Topic消息，topic={}，message={}", topic, message);
            }

            acknowledgment.acknowledge();

            log.info("多Topic消息消费完成，topic={}，partition={}，offset={}",
                    topic,
                    record.partition(),
                    record.offset());
        } catch (Exception e) {
            log.error("多Topic消息消费失败，topic={}，partition={}，offset={}，原因={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    e.getMessage(),
                    e);
            throw e;
        }
    }

    /**
     * 处理订单事件。
     *
     * @param message 消息内容
     */
    private void handleOrderEvent(String message) {
        log.info("处理订单事件消息，message={}", message);
    }

    /**
     * 处理用户事件。
     *
     * @param message 消息内容
     */
    private void handleUserEvent(String message) {
        log.info("处理用户事件消息，message={}", message);
    }

    /**
     * 处理支付事件。
     *
     * @param message 消息内容
     */
    private void handlePaymentEvent(String message) {
        log.info("处理支付事件消息，message={}", message);
    }

}
```

如果多个 Topic 的消息结构完全不同，不建议在一个消费者方法中塞入过多判断逻辑。可以拆成多个监听方法，仍然使用同一个 Consumer Group。

```java
@KafkaListener(
        id = "orderEventListener",
        topics = KafkaTopicConstant.ORDER_EVENT_TOPIC,
        groupId = KafkaTopicConstant.MULTI_TOPIC_EVENT_GROUP
)
public void consumeOrderEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
    // 处理订单事件
}

@KafkaListener(
        id = "userEventListener",
        topics = KafkaTopicConstant.USER_EVENT_TOPIC,
        groupId = KafkaTopicConstant.MULTI_TOPIC_EVENT_GROUP
)
public void consumeUserEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
    // 处理用户事件
}
```

多 Topic 消费需要注意以下问题：

| 问题         | 建议                                        |
| ------------ | ------------------------------------------- |
| 消息结构差异 | 差异较大时拆分多个消费者                    |
| 异常影响范围 | 一个 Topic 处理异常可能影响同一消费者线程   |
| 监控维度     | 日志和指标中必须带上 Topic                  |
| 死信 Topic   | 不同 Topic 建议进入不同 DLT                 |
| 消费组规划   | 确认这些 Topic 是否真的属于同一业务消费模型 |

多 Topic 消费适合逻辑相近的事件聚合，不适合把所有业务 Topic 都放到一个消费者里处理。业务边界清晰时，按业务拆分消费者更容易维护和排查。

### 多 Consumer Group 消费

多 Consumer Group 消费用于多个业务系统独立消费同一个 Topic。Kafka 中不同 Consumer Group 之间互不影响，每个 Group 都会维护自己的 Offset。

例如订单事件 Topic 可以同时被以下系统消费：

| Consumer Group             | 用途                 |
| -------------------------- | -------------------- |
| `order-event-group`        | 订单服务自身后置处理 |
| `report-order-event-group` | 报表服务统计订单数据 |
| `search-order-event-group` | 搜索服务同步订单索引 |
| `risk-order-event-group`   | 风控服务处理风险规则 |

这种模式常用于事件驱动架构。一个业务事件发布后，多个下游系统可以独立处理，不需要上游系统直接调用多个接口。

报表服务消费者如下。

文件位置：`src/main/java/io/github/atengk/kafka/consumer/ReportOrderEventConsumer.java`

下面的代码使用 `report-order-event-group` 消费订单事件，模拟报表统计处理。

```java
package io.github.atengk.kafka.consumer;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.dto.OrderEventMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 报表订单事件消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class ReportOrderEventConsumer {

    /**
     * 消费订单事件并更新报表数据。
     *
     * @param record Kafka 消息记录
     * @param acknowledgment Offset 确认对象
     */
    @KafkaListener(
            id = "reportOrderEventConsumer",
            topics = KafkaTopicConstant.ORDER_EVENT_TOPIC,
            groupId = KafkaTopicConstant.REPORT_ORDER_EVENT_GROUP
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            String jsonMessage = record.value();
            if (StrUtil.isBlank(jsonMessage)) {
                log.warn("报表消费跳过空订单事件，partition={}，offset={}",
                        record.partition(),
                        record.offset());
                acknowledgment.acknowledge();
                return;
            }

            OrderEventMessage message = JSONUtil.toBean(jsonMessage, OrderEventMessage.class);

            log.info("报表服务消费订单事件，orderId={}，messageId={}，eventType={}，amount={}",
                    message.getOrderId(),
                    message.getMessageId(),
                    message.getEventType(),
                    message.getAmount());

            // 示例：更新订单日报、用户消费统计、GMV 汇总等报表数据

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("报表服务消费订单事件失败，partition={}，offset={}，原因={}",
                    record.partition(),
                    record.offset(),
                    e.getMessage(),
                    e);
            throw e;
        }
    }

}
```

搜索服务消费者如下。

文件位置：`src/main/java/io/github/atengk/kafka/consumer/SearchOrderEventConsumer.java`

下面的代码使用 `search-order-event-group` 消费同一个订单事件 Topic，模拟同步搜索索引。

```java
package io.github.atengk.kafka.consumer;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.dto.OrderEventMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 搜索订单事件消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class SearchOrderEventConsumer {

    /**
     * 消费订单事件并同步搜索索引。
     *
     * @param record Kafka 消息记录
     * @param acknowledgment Offset 确认对象
     */
    @KafkaListener(
            id = "searchOrderEventConsumer",
            topics = KafkaTopicConstant.ORDER_EVENT_TOPIC,
            groupId = KafkaTopicConstant.SEARCH_ORDER_EVENT_GROUP
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            String jsonMessage = record.value();
            if (StrUtil.isBlank(jsonMessage)) {
                log.warn("搜索消费跳过空订单事件，partition={}，offset={}",
                        record.partition(),
                        record.offset());
                acknowledgment.acknowledge();
                return;
            }

            OrderEventMessage message = JSONUtil.toBean(jsonMessage, OrderEventMessage.class);

            log.info("搜索服务消费订单事件，orderId={}，messageId={}，eventType={}",
                    message.getOrderId(),
                    message.getMessageId(),
                    message.getEventType());

            // 示例：同步订单索引、删除取消订单索引、刷新订单搜索状态等

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("搜索服务消费订单事件失败，partition={}，offset={}，原因={}",
                    record.partition(),
                    record.offset(),
                    e.getMessage(),
                    e);
            throw e;
        }
    }

}
```

发送一条订单事件后，不同 Consumer Group 都会各自收到一份消息。

```bash
curl -X POST 'http://localhost:8080/api/kafka/messages/order-event' \
  -H 'Content-Type: application/json' \
  -d '{
    "orderId": "ORDER_MULTI_GROUP_50001",
    "userId": 1001,
    "amount": 299.90,
    "eventType": "PAID"
  }'
```

预期日志中可以看到报表服务和搜索服务分别消费同一条订单事件：

```text
报表服务消费订单事件，orderId=ORDER_MULTI_GROUP_50001，messageId=xxx，eventType=PAID，amount=299.9
搜索服务消费订单事件，orderId=ORDER_MULTI_GROUP_50001，messageId=xxx，eventType=PAID
```

多 Consumer Group 和同 Consumer Group 多实例的区别如下：

| 模式                         | 说明                        | 消息是否重复分发                    |
| ---------------------------- | --------------------------- | ----------------------------------- |
| 同一个 Consumer Group 多实例 | 多个实例共同分摊 Topic 分区 | 同一条消息只会被组内一个实例消费    |
| 不同 Consumer Group          | 每个 Group 独立维护 Offset  | 同一条消息会被每个 Group 各消费一次 |

多 Consumer Group 适合广播式业务消费，不同业务系统都需要处理同一个事件。相同 Consumer Group 适合负载均衡消费，同一个业务服务多实例共同处理同一批消息。

多 Consumer Group 设计建议如下：

| 设计点        | 建议                                          |
| ------------- | --------------------------------------------- |
| Group ID 命名 | 使用业务语义，例如 `report-order-event-group` |
| 下游隔离      | 不同业务使用不同 Group，避免相互影响          |
| Offset 独立   | 每个 Group 独立消费和提交 Offset              |
| 幂等处理      | 每个 Group 内部都需要独立设计幂等             |
| 失败处理      | 每个 Group 可以有自己的重试和死信策略         |
| 监控告警      | 按 Group 维度监控消费延迟和失败率             |

在实际项目中，一个 Topic 可以被多个 Consumer Group 消费，但不代表 Topic 可以无限制承载所有业务语义。Topic 的事件定义应保持清晰稳定，下游消费者应围绕这个事件定义扩展，而不是让一个 Topic 混入过多无关业务消息。

本章节完成后，Kafka 项目已经具备常用生产开发场景的基础能力：延迟重试用于处理临时失败，死信 Topic 用于隔离最终失败消息，多 Topic 消费用于统一处理相近事件，多 Consumer Group 消费用于事件广播和下游系统解耦。



## 测试与验证

本章节用于说明 Kafka 功能开发完成后的验证方式。Kafka 测试通常分为三类：通过命令行验证 Kafka 服务本身，通过 HTTP 接口验证应用发送能力，通过自动化集成测试验证 Producer 和 Consumer 的完整链路。

建议开发阶段按下面顺序验证：

```text
Kafka 服务是否可用
        │
        ▼
Topic 是否存在
        │
        ▼
命令行能否发送和消费消息
        │
        ▼
Spring Boot 应用能否发送消息
        │
        ▼
Spring Boot 应用能否消费消息
        │
        ▼
集成测试能否稳定通过
```

本章节涉及的关键文件如下：

```text
src/main/java/io/github/atengk/kafka/controller/KafkaMessageController.java
src/main/java/io/github/atengk/kafka/producer/OrderEventProducer.java
src/main/java/io/github/atengk/kafka/consumer/OrderEventConsumer.java
src/test/java/io/github/atengk/kafka/KafkaProducerIntegrationTest.java
src/test/java/io/github/atengk/kafka/KafkaConsumerIntegrationTest.java
src/test/resources/application-test.yml
```

### 本地消息发送测试

本地消息发送测试用于验证应用是否能够将消息成功写入 Kafka。测试前需要确保 Kafka 服务已经启动，并且 Topic 已经创建。

先确认 Kafka 容器状态。

```bash
docker compose ps
docker logs --tail=100 kafka-dev
```

`docker compose ps` 用于查看 Kafka 容器是否处于运行状态，`docker logs --tail=100 kafka-dev` 用于查看最近启动日志。如果容器未启动，可以执行下面的命令。

```bash
docker compose up -d
```

进入 Kafka 容器后查看 Topic 是否存在。

```bash
docker exec -it kafka-dev bash

/opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server 127.0.0.1:9092 \
  --list
```

如果没有 `order-event-topic`，可以手动创建。

```bash
/opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server 127.0.0.1:9092 \
  --create \
  --topic order-event-topic \
  --partitions 3 \
  --replication-factor 1
```

查看 Topic 详情。

```bash
/opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server 127.0.0.1:9092 \
  --describe \
  --topic order-event-topic
```

以上命令中，`--partitions 3` 表示创建 3 个分区，方便验证消费者并发能力；`--replication-factor 1` 表示副本数为 1，仅适合本地单节点环境。

启动一个 Kafka 命令行消费者，用于观察应用发送的消息。

```bash
/opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server 127.0.0.1:9092 \
  --topic order-event-topic \
  --from-beginning \
  --property print.key=true \
  --property key.separator=" | "
```

`--from-beginning` 表示从 Topic 起始位置消费，适合本地测试；`print.key=true` 用于打印消息 Key，便于验证带 Key 消息是否发送成功。

启动 Spring Boot 应用。

```bash
mvn spring-boot:run
```

应用启动后，使用前面章节提供的 HTTP 接口发送普通文本消息。

```bash
curl -X POST 'http://localhost:8080/api/kafka/messages/text?message=hello-kafka'
```

命令行消费者预期可以看到类似输出：

```text
null | hello-kafka
```

继续发送订单事件消息。

```bash
curl -X POST 'http://localhost:8080/api/kafka/messages/order-event' \
  -H 'Content-Type: application/json' \
  -d '{
    "orderId": "ORDER_TEST_60001",
    "userId": 1001,
    "amount": 99.80,
    "eventType": "CREATED"
  }'
```

命令行消费者预期可以看到类似输出：

```text
ORDER_TEST_60001 | {"messageId":"c4fc77f971e94dedaef3c1e8f80b38f2","orderId":"ORDER_TEST_60001","userId":1001,"amount":99.8,"eventType":"CREATED","eventTime":"2026-04-30T10:30:00"}
```

应用日志中也应输出发送成功信息，例如：

```text
开始发送订单事件，topic=order-event-topic，key=ORDER_TEST_60001，messageId=c4fc77f971e94dedaef3c1e8f80b38f2，eventType=CREATED
订单事件发送成功，orderId=ORDER_TEST_60001，messageId=c4fc77f971e94dedaef3c1e8f80b38f2，topic=order-event-topic，partition=1，offset=20
```

如果发送失败，需要优先检查以下内容：

| 问题             | 排查方式                                                |
| ---------------- | ------------------------------------------------------- |
| Kafka 服务未启动 | 执行 `docker compose ps` 和 `docker logs kafka-dev`     |
| Broker 地址错误  | 检查 `spring.kafka.bootstrap-servers`                   |
| Topic 不存在     | 执行 `kafka-topics.sh --list`                           |
| 序列化配置错误   | 检查 Producer 的 `key-serializer` 和 `value-serializer` |
| 应用端口错误     | 检查 Spring Boot 启动日志中的端口                       |
| 网络不可达       | 在宿主机测试 `127.0.0.1:9092` 是否可连接                |

本地发送测试的目标是确认 Producer 到 Kafka 的链路正常。只要命令行消费者能看到应用发送的消息，就说明应用发送能力基本正常。

### 本地消息消费测试

本地消息消费测试用于验证 Spring Boot 应用是否能够从 Kafka 中读取消息，并正确执行业务逻辑。测试方式可以反过来：使用 Kafka 命令行生产者发送消息，再观察 Spring Boot 应用消费者日志。

先启动 Spring Boot 应用。

```bash
mvn spring-boot:run
```

确认消费者已经启动。应用日志中通常会出现类似 Kafka Consumer 加入 Consumer Group 的日志。如果开启了业务消费者日志，也可以看到监听容器启动信息。

进入 Kafka 容器，启动命令行生产者。

```bash
docker exec -it kafka-dev bash

/opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server 127.0.0.1:9092 \
  --topic order-event-topic \
  --property parse.key=true \
  --property key.separator="|"
```

`parse.key=true` 表示启用 Key 解析，`key.separator="|"` 表示使用竖线分隔 Key 和 Value。输入消息时，竖线左边是消息 Key，右边是消息 Value。

发送一条订单创建消息。

```text
ORDER_CONSUMER_60001|{"messageId":"MSG_CONSUMER_60001","orderId":"ORDER_CONSUMER_60001","userId":1001,"amount":99.80,"eventType":"CREATED","eventTime":"2026-04-30T10:30:00"}
```

Spring Boot 应用日志预期输出类似内容：

```text
开始消费订单事件消息，topic=order-event-topic，partition=0，offset=21，key=ORDER_CONSUMER_60001
处理订单创建事件，orderId=ORDER_CONSUMER_60001，userId=1001，amount=99.8
订单事件消息消费完成，orderId=ORDER_CONSUMER_60001，messageId=MSG_CONSUMER_60001，eventType=CREATED，partition=0，offset=21
```

继续发送一条订单支付消息。

```text
ORDER_CONSUMER_60002|{"messageId":"MSG_CONSUMER_60002","orderId":"ORDER_CONSUMER_60002","userId":1002,"amount":199.90,"eventType":"PAID","eventTime":"2026-04-30T10:31:00"}
```

应用日志预期输出支付事件处理日志。

```text
处理订单支付事件，orderId=ORDER_CONSUMER_60002，userId=1002，amount=199.9
```

如果需要验证消费异常处理，可以发送一条异常事件消息。前提是消费者代码中已经按前面章节加入了 `ERROR` 或 `RETRY_ERROR` 的异常模拟逻辑。

```text
ORDER_ERROR_60001|{"messageId":"MSG_ERROR_60001","orderId":"ORDER_ERROR_60001","userId":1001,"amount":99.80,"eventType":"ERROR","eventTime":"2026-04-30T10:32:00"}
```

预期现象如下：

| 阶段        | 现象                                                         |
| ----------- | ------------------------------------------------------------ |
| 首次消费    | 应用日志输出消费失败                                         |
| 重试处理    | 根据 `DefaultErrorHandler` 或 `@RetryableTopic` 配置进行重试 |
| 重试耗尽    | 消息进入死信 Topic                                           |
| 死信消费    | 死信消费者输出 DLT 日志                                      |
| Offset 处理 | 原 Topic 消费链路继续向后处理                                |

可以使用命令行消费者查看死信 Topic 中是否存在失败消息。

```bash
/opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server 127.0.0.1:9092 \
  --topic order-event-topic.DLT \
  --from-beginning \
  --property print.key=true \
  --property key.separator=" | "
```

如果 Spring Boot 应用没有消费到消息，需要重点检查以下内容：

| 问题                  | 排查方式                                           |
| --------------------- | -------------------------------------------------- |
| Topic 不一致          | 检查命令行发送的 Topic 和 `@KafkaListener` 配置    |
| Group ID 已消费到最新 | 更换新的 Consumer Group 或重置 Offset              |
| 消息格式错误          | 检查 JSON 是否能被消费者解析                       |
| 监听器未启动          | 检查 `spring.kafka.listener.auto-startup`          |
| 消费者异常退出        | 查看应用异常日志                                   |
| 分区被其他实例占用    | 检查是否有同 Group 多实例运行                      |
| Offset 已提交         | 使用新 Group ID 或 `--from-beginning` 验证历史消息 |

如果想查看某个 Consumer Group 的消费位点，可以使用下面的命令。

```bash
/opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server 127.0.0.1:9092 \
  --describe \
  --group order-event-group
```

输出中需要重点关注 `CURRENT-OFFSET`、`LOG-END-OFFSET` 和 `LAG`：

| 字段             | 说明                                 |
| ---------------- | ------------------------------------ |
| `CURRENT-OFFSET` | 当前 Consumer Group 已提交的消费位点 |
| `LOG-END-OFFSET` | Partition 最新消息位点               |
| `LAG`            | 消费积压数量                         |
| `CONSUMER-ID`    | 当前正在消费的消费者实例             |
| `HOST`           | 消费者所在主机                       |
| `CLIENT-ID`      | 消费者客户端 ID                      |

`LAG` 长时间不下降，通常说明消费者处理速度不足、消费者异常、分区分配异常或下游依赖阻塞。

### 集成测试方式

集成测试用于在自动化测试中验证 Kafka 发送和消费链路。Spring Kafka 提供了 `spring-kafka-test`，可以通过 `@EmbeddedKafka` 启动内嵌 Kafka，避免测试依赖外部 Kafka 服务。

测试依赖如下。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Test：提供 JUnit 5、Spring 测试上下文等能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Spring Kafka Test：提供 Embedded Kafka、KafkaTestUtils 等测试工具 -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Hutool：测试中用于构造 JSON 消息、生成 ID 和字符串处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.32</version>
    </dependency>
</dependencies>
```

测试环境可以单独使用 `application-test.yml`，避免测试配置影响本地开发配置。

文件位置：`src/test/resources/application-test.yml`

```yaml
spring:
  kafka:
    # 测试运行时由 @EmbeddedKafka 动态注入 Broker 地址
    bootstrap-servers: ${spring.embedded.kafka.brokers}

    producer:
      # 测试中 Key 使用字符串序列化
      key-serializer: org.apache.kafka.common.serialization.StringSerializer

      # 测试中 Value 使用字符串序列化
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

    consumer:
      # 测试消费者组，避免与本地开发消费者组冲突
      group-id: kafka-test-group

      # 测试中 Key 使用字符串反序列化
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer

      # 测试中 Value 使用字符串反序列化
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer

      # 测试环境从最早位点消费，保证能读取测试发送的消息
      auto-offset-reset: earliest

      # 测试环境可关闭自动提交，由测试代码自行控制
      enable-auto-commit: false

    listener:
      # 测试环境使用手动提交
      ack-mode: manual
```

Producer 集成测试如下。

文件位置：`src/test/java/io/github/atengk/kafka/KafkaProducerIntegrationTest.java`

下面的代码使用内嵌 Kafka 验证 `KafkaTemplate` 能够正常发送消息，并通过测试消费者读取消息内容。

```java
package io.github.atengk.kafka;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.kafka.dto.OrderEventMessage;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Kafka 生产者集成测试
 *
 * @author Ateng
 * @since 2026-04-30
 */
@DirtiesContext
@ActiveProfiles("test")
@SpringBootTest
@EmbeddedKafka(
        partitions = 3,
        topics = KafkaProducerIntegrationTest.TEST_TOPIC,
        brokerProperties = {
                "listeners=PLAINTEXT://127.0.0.1:0",
                "port=0"
        }
)
class KafkaProducerIntegrationTest {

    static final String TEST_TOPIC = "order-event-test-topic";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ConsumerFactory<String, String> consumerFactory;

    /**
     * 验证 KafkaTemplate 可以发送订单事件 JSON 消息。
     */
    @Test
    void shouldSendOrderEventMessage() {
        String orderId = "ORDER_IT_" + IdUtil.fastSimpleUUID();

        OrderEventMessage message = new OrderEventMessage();
        message.setMessageId(IdUtil.fastSimpleUUID());
        message.setOrderId(orderId);
        message.setUserId(1001L);
        message.setAmount(new BigDecimal("99.80"));
        message.setEventType("CREATED");
        message.setEventTime(LocalDateTime.now());

        kafkaTemplate.send(TEST_TOPIC, orderId, JSONUtil.toJsonStr(message));
        kafkaTemplate.flush();

        Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps(
                "producer-integration-test-group",
                "false",
                null
        );
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (Consumer<String, String> consumer = consumerFactory.createConsumer(
                "producer-integration-test-group",
                "producer-integration-test-client"
        )) {
            consumer.subscribe(java.util.List.of(TEST_TOPIC));

            ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, TEST_TOPIC);

            Assertions.assertEquals(orderId, record.key());

            OrderEventMessage actualMessage = JSONUtil.toBean(record.value(), OrderEventMessage.class);
            Assertions.assertEquals(orderId, actualMessage.getOrderId());
            Assertions.assertEquals("CREATED", actualMessage.getEventType());
        }
    }

}
```

如果当前 `ConsumerFactory` 中的消费者组配置不便于测试，也可以在测试类中手动创建 Kafka Consumer。下面这种方式更独立，不依赖应用内消费者配置。

文件位置：`src/test/java/io/github/atengk/kafka/KafkaProducerStandaloneIntegrationTest.java`

下面的代码手动创建测试 Consumer，适合只验证 Producer 发送结果。

```java
package io.github.atengk.kafka;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.kafka.dto.OrderEventMessage;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Kafka 生产者独立集成测试
 *
 * @author Ateng
 * @since 2026-04-30
 */
@DirtiesContext
@ActiveProfiles("test")
@SpringBootTest
@EmbeddedKafka(
        partitions = 3,
        topics = KafkaProducerStandaloneIntegrationTest.TEST_TOPIC,
        brokerProperties = {
                "listeners=PLAINTEXT://127.0.0.1:0",
                "port=0"
        }
)
class KafkaProducerStandaloneIntegrationTest {

    static final String TEST_TOPIC = "order-event-standalone-test-topic";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    /**
     * 验证生产者发送的消息可以被独立测试 Consumer 消费。
     */
    @Test
    void shouldSendAndConsumeByStandaloneConsumer() {
        String orderId = "ORDER_STANDALONE_" + IdUtil.fastSimpleUUID();

        OrderEventMessage message = new OrderEventMessage();
        message.setMessageId(IdUtil.fastSimpleUUID());
        message.setOrderId(orderId);
        message.setUserId(1001L);
        message.setAmount(new BigDecimal("199.90"));
        message.setEventType("PAID");
        message.setEventTime(LocalDateTime.now());

        Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps(
                "standalone-producer-test-group",
                "false",
                embeddedKafkaBroker
        );
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(
                consumerProperties,
                new StringDeserializer(),
                new StringDeserializer()
        );

        try (Consumer<String, String> consumer = consumerFactory.createConsumer()) {
            embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, TEST_TOPIC);

            kafkaTemplate.send(TEST_TOPIC, orderId, JSONUtil.toJsonStr(message));
            kafkaTemplate.flush();

            ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, TEST_TOPIC);

            Assertions.assertEquals(orderId, record.key());

            OrderEventMessage actualMessage = JSONUtil.toBean(record.value(), OrderEventMessage.class);
            Assertions.assertEquals(orderId, actualMessage.getOrderId());
            Assertions.assertEquals("PAID", actualMessage.getEventType());
        }
    }

}
```

Consumer 集成测试可以通过测试专用监听器和 `CountDownLatch` 验证消息是否被应用消费。测试专用监听器建议只放在测试类内部，避免影响正式业务代码。

文件位置：`src/test/java/io/github/atengk/kafka/KafkaConsumerIntegrationTest.java`

下面的代码启动内嵌 Kafka，发送一条订单事件消息，并验证测试监听器是否成功消费。

```java
package io.github.atengk.kafka;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.kafka.dto.OrderEventMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Kafka 消费者集成测试
 *
 * @author Ateng
 * @since 2026-04-30
 */
@DirtiesContext
@ActiveProfiles("test")
@SpringBootTest
@EmbeddedKafka(
        partitions = 3,
        topics = KafkaConsumerIntegrationTest.TEST_TOPIC,
        brokerProperties = {
                "listeners=PLAINTEXT://127.0.0.1:0",
                "port=0"
        }
)
class KafkaConsumerIntegrationTest {

    static final String TEST_TOPIC = "order-event-consumer-test-topic";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private TestOrderEventConsumer testOrderEventConsumer;

    /**
     * 验证消费者可以收到订单事件消息。
     *
     * @throws InterruptedException 等待消费结果时被中断
     */
    @Test
    void shouldConsumeOrderEventMessage() throws InterruptedException {
        String orderId = "ORDER_CONSUMER_IT_" + IdUtil.fastSimpleUUID();

        OrderEventMessage message = new OrderEventMessage();
        message.setMessageId(IdUtil.fastSimpleUUID());
        message.setOrderId(orderId);
        message.setUserId(1001L);
        message.setAmount(new BigDecimal("299.90"));
        message.setEventType("CREATED");
        message.setEventTime(LocalDateTime.now());

        kafkaTemplate.send(TEST_TOPIC, orderId, JSONUtil.toJsonStr(message));
        kafkaTemplate.flush();

        boolean consumed = testOrderEventConsumer.getLatch().await(10, TimeUnit.SECONDS);

        Assertions.assertTrue(consumed, "消费者未在指定时间内收到消息");
        Assertions.assertNotNull(testOrderEventConsumer.getRecord());
        Assertions.assertEquals(orderId, testOrderEventConsumer.getRecord().key());

        OrderEventMessage actualMessage = JSONUtil.toBean(
                testOrderEventConsumer.getRecord().value(),
                OrderEventMessage.class
        );
        Assertions.assertEquals(orderId, actualMessage.getOrderId());
        Assertions.assertEquals("CREATED", actualMessage.getEventType());
    }

    /**
     * 测试专用订单事件消费者
     *
     * @author Ateng
     * @since 2026-04-30
     */
    @Slf4j
    @Getter
    @Component
    static class TestOrderEventConsumer {

        private final CountDownLatch latch = new CountDownLatch(1);

        private ConsumerRecord<String, String> record;

        /**
         * 消费测试订单事件消息。
         *
         * @param record Kafka 消息记录
         * @param acknowledgment Offset 确认对象
         */
        @KafkaListener(
                id = "testOrderEventConsumer",
                topics = KafkaConsumerIntegrationTest.TEST_TOPIC,
                groupId = "consumer-integration-test-group"
        )
        public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
            this.record = record;

            log.info("测试消费者收到订单事件消息，topic={}，partition={}，offset={}，key={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key());

            acknowledgment.acknowledge();
            latch.countDown();
        }

    }

}
```

执行测试命令如下：

```bash
mvn test
```

如果只执行 Kafka 相关测试，可以使用下面的命令。

```bash
mvn -Dtest=KafkaProducerStandaloneIntegrationTest test
mvn -Dtest=KafkaConsumerIntegrationTest test
```

以上命令中，`-Dtest=类名` 表示只运行指定测试类，适合本地调试某个 Kafka 测试用例。

集成测试注意事项如下：

| 问题           | 建议                                              |
| -------------- | ------------------------------------------------- |
| 测试 Topic     | 使用独立测试 Topic，避免污染本地开发 Topic        |
| Consumer Group | 每个测试使用独立 Group ID，避免 Offset 干扰       |
| 消费等待       | 使用 `CountDownLatch` 等待异步消费结果            |
| 测试超时       | 设置合理超时时间，避免测试永久阻塞                |
| KafkaTemplate  | 发送后调用 `flush()`，确保消息尽快发出            |
| 业务消费者     | 避免正式消费者和测试消费者监听同一测试 Topic      |
| 测试隔离       | 使用 `@DirtiesContext` 避免上下文状态影响其他测试 |

如果集成测试不稳定，通常需要检查以下方面：

| 问题             | 可能原因                                     |
| ---------------- | -------------------------------------------- |
| 偶发消费不到消息 | 消费者还未完成分区分配就发送了消息           |
| 读取到旧消息     | 测试 Topic 或 Group ID 复用                  |
| 测试超时         | 监听器未启动、Topic 不一致或序列化配置错误   |
| 断言 Key 失败    | 发送时没有指定 Key                           |
| JSON 解析失败    | 消息字段类型和 DTO 不一致                    |
| 多消费者抢消息   | 多个监听器使用同一个 Group ID 消费同一 Topic |

测试策略建议如下：

| 测试类型          | 目标                    | 推荐方式                                                 |
| ----------------- | ----------------------- | -------------------------------------------------------- |
| 命令行测试        | 验证 Kafka 服务和 Topic | `kafka-console-producer.sh`、`kafka-console-consumer.sh` |
| 本地接口测试      | 验证应用 Producer       | `curl` 调用发送接口                                      |
| 本地消费测试      | 验证应用 Consumer       | 命令行生产者发送消息，观察应用日志                       |
| Producer 集成测试 | 验证发送链路            | `@EmbeddedKafka` + 测试 Consumer                         |
| Consumer 集成测试 | 验证消费链路            | `@EmbeddedKafka` + `CountDownLatch`                      |
| 异常测试          | 验证重试和死信          | 发送异常事件，检查 DLT 和日志                            |

生产项目中，Kafka 测试不建议只依赖手工验证。至少应保留 Producer 发送测试、Consumer 消费测试和异常处理测试，确保后续改动配置、Topic、序列化方式或消费逻辑时能够及时发现问题。



## 生产使用建议

本章节用于说明 Kafka 在生产环境中的常见规划原则和排查方法。前面章节偏向开发接入和功能实现，本章节重点关注生产可用性、可维护性和可观测性。

Kafka 生产使用不能只关注“能发送、能消费”，还需要关注 Topic 命名、分区数量、副本数量、消费并发、消息堆积、失败重试、死信处理、日志追踪和监控告警。

生产环境建议遵循以下基本原则：

| 方向      | 建议                                                 |
| --------- | ---------------------------------------------------- |
| Topic     | 按业务语义规划，不要混用过多无关事件                 |
| Partition | 按吞吐量和消费并发规划，不要盲目设置过多             |
| Replica   | 生产环境副本数通常不低于 3                           |
| Producer  | 关键消息开启 `acks=all` 和幂等生产                   |
| Consumer  | 关闭自动提交，业务成功后手动提交 Offset              |
| 幂等      | 消费端必须默认支持重复消费                           |
| 重试      | 区分临时异常和永久异常，避免无限重试                 |
| 死信      | 死信消息需要落库、告警和补偿机制                     |
| 日志      | 日志中记录 Topic、Partition、Offset、Key、业务 ID    |
| 监控      | 按 Topic、Consumer Group、Partition 维度监控消费延迟 |

### Topic 规划

Topic 是 Kafka 消息的业务边界。生产环境中，Topic 规划应优先体现业务语义，而不是为了减少 Topic 数量把所有消息都塞进一个 Topic。

推荐命名方式如下：

```text
{业务域}-{事件类型}-topic
```

常见示例：

```text
order-event-topic
payment-event-topic
user-event-topic
inventory-event-topic
order-event-topic.DLT
payment-event-topic.DLT
```

Topic 命名建议如下：

| 规则                | 示例                     | 说明                                     |
| ------------------- | ------------------------ | ---------------------------------------- |
| 使用小写字母        | `order-event-topic`      | 避免不同环境大小写混乱                   |
| 使用中横线分隔      | `payment-result-topic`   | 可读性更好                               |
| 体现业务域          | `order-event-topic`      | 方便定位消息归属                         |
| 体现消息类型        | `user-login-topic`       | 避免 Topic 语义过宽                      |
| 死信 Topic 统一后缀 | `order-event-topic.DLT`  | 方便识别失败消息                         |
| 避免环境前缀混乱    | 不建议 `dev-order-topic` | 环境隔离优先交给集群、命名空间或权限系统 |

Topic 拆分时，可以按以下维度判断：

| 拆分维度               | 是否建议拆分 |
| ---------------------- | ------------ |
| 业务语义不同           | 建议拆分     |
| 消费方完全不同         | 建议拆分     |
| 保留时间不同           | 建议拆分     |
| 消息量差异很大         | 建议拆分     |
| 权限要求不同           | 建议拆分     |
| 消息格式完全不同       | 建议拆分     |
| 只是同一事件的不同字段 | 不建议拆分   |
| 只是临时增加一个消费者 | 不建议拆分   |

例如，订单创建、订单支付、订单取消都属于订单事件，可以统一放在 `order-event-topic` 中，通过 `eventType` 区分：

```json
{
  "messageId": "c4fc77f971e94dedaef3c1e8f80b38f2",
  "orderId": "ORDER_70001",
  "userId": 1001,
  "amount": 99.80,
  "eventType": "PAID",
  "eventTime": "2026-04-30T10:30:00"
}
```

但订单事件和支付回调事件不建议混在一个 Topic 中，因为它们的业务归属、消费方、失败处理和权限边界通常不同。

生产环境创建 Topic 时，需要明确分区数、副本数、保留时间和清理策略。

```bash
# 创建订单事件 Topic
/opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server kafka-01.internal:9092,kafka-02.internal:9092,kafka-03.internal:9092 \
  --create \
  --topic order-event-topic \
  --partitions 12 \
  --replication-factor 3 \
  --config retention.ms=604800000 \
  --config cleanup.policy=delete \
  --config min.insync.replicas=2
```

以上命令中，`--partitions 12` 表示创建 12 个分区，`--replication-factor 3` 表示每条消息保留 3 个副本，`retention.ms=604800000` 表示消息保留 7 天，`cleanup.policy=delete` 表示按保留时间删除旧消息，`min.insync.replicas=2` 表示至少 2 个同步副本可用时才允许满足强确认写入。

Topic 关键配置说明如下：

| 配置                  | 示例值      | 说明                                          |
| --------------------- | ----------- | --------------------------------------------- |
| `partitions`          | `12`        | 决定 Topic 最大并行消费能力                   |
| `replication-factor`  | `3`         | 决定消息副本数量和可用性                      |
| `retention.ms`        | `604800000` | 消息保留时间，示例为 7 天                     |
| `cleanup.policy`      | `delete`    | 日志清理策略，常规业务消息通常使用 delete     |
| `min.insync.replicas` | `2`         | 最小同步副本数，配合 Producer `acks=all` 使用 |
| `max.message.bytes`   | 按业务调整  | 单条消息最大大小，不建议发送过大消息          |

生产环境不建议开启业务应用自动创建 Topic。应用启动时自动创建 Topic 虽然方便，但容易造成分区数、副本数、保留时间不符合生产规范。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  kafka:
    admin:
      # 生产环境建议关闭应用自动创建或修改 Topic，Topic 由平台统一管理
      auto-create: false

    listener:
      # 生产环境中 Topic 缺失应尽早暴露问题
      missing-topics-fatal: true
```

Topic 规划建议如下：

| 项目       | 建议                                                        |
| ---------- | ----------------------------------------------------------- |
| Topic 语义 | 一个 Topic 表达一类清晰业务事件                             |
| 消息格式   | 同一 Topic 内消息结构尽量统一                               |
| 事件类型   | 可以用 `eventType` 区分同一业务域下的不同动作               |
| 死信 Topic | 每个核心 Topic 配套 DLT                                     |
| 保留时间   | 根据业务补偿窗口设置，不要全部使用默认值                    |
| 权限控制   | 按 Topic 分配生产和消费权限                                 |
| 文档登记   | 记录 Topic 名称、负责人、生产者、消费者、字段结构和保留策略 |

Topic 规划不是一次性工作。业务增长后，需要定期检查 Topic 是否语义过宽、消息是否混杂、消费者是否过多、保留策略是否合理。

### Partition 规划

Partition 是 Kafka 扩展吞吐和消费并发的基础。一个 Topic 可以包含多个 Partition，同一个 Consumer Group 内，一个 Partition 同一时刻只能被一个 Consumer 实例消费。

Partition 数量直接影响以下能力：

| 能力     | 影响                                                         |
| -------- | ------------------------------------------------------------ |
| 写入吞吐 | 分区越多，可并行写入能力越强                                 |
| 消费并发 | 分区数决定同一 Consumer Group 最大有效并发数                 |
| 消息顺序 | Kafka 只保证同一 Partition 内有序                            |
| 扩容空间 | 分区数过少会限制后续消费者扩容                               |
| 集群开销 | 分区过多会增加 Broker、Controller、文件句柄和 Rebalance 成本 |

Partition 数量不是越多越好。分区过少会限制吞吐，分区过多会增加集群负担和运维复杂度。

可以按下面思路估算分区数：

```text
Partition 数量 >= max(目标生产吞吐所需分区数, 目标消费并发所需分区数)
```

更具体地说，需要同时考虑生产端和消费端：

| 维度       | 说明                                       |
| ---------- | ------------------------------------------ |
| 生产吞吐   | 单个 Partition 能承载的写入 QPS 和消息大小 |
| 消费吞吐   | 单个 Consumer 实例每秒能处理多少消息       |
| 目标延迟   | 业务允许消息积压多久                       |
| 消费实例数 | 服务最多计划部署多少个实例                 |
| 顺序要求   | 是否需要同一业务 Key 保证顺序              |
| 未来增长   | 是否需要预留 6 到 12 个月增长空间          |

示例：如果订单事件高峰期每秒 6000 条消息，单个消费者线程每秒能稳定处理 800 条，为了消费端不堆积，至少需要：

```text
6000 / 800 = 7.5
```

因此可以设置 8 个以上分区。考虑流量波动和扩容空间，可以规划为 12 或 16 个分区。

常见规划参考如下：

| 场景               | 建议分区数                          |
| ------------------ | ----------------------------------- |
| 本地开发           | 1 到 3                              |
| 小流量业务 Topic   | 3 到 6                              |
| 普通核心业务 Topic | 6 到 12                             |
| 中高吞吐事件 Topic | 12 到 32                            |
| 大规模日志 Topic   | 按实际压测结果规划                  |
| 严格顺序 Topic     | 谨慎增加分区，依赖 Key 保证局部顺序 |

查看 Topic 分区情况：

```bash
/opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server kafka-01.internal:9092,kafka-02.internal:9092,kafka-03.internal:9092 \
  --describe \
  --topic order-event-topic
```

输出中需要关注 `PartitionCount`、`ReplicationFactor`、`Leader`、`Replicas`、`Isr` 等字段。

```text
Topic: order-event-topic  TopicId: xxx  PartitionCount: 12  ReplicationFactor: 3
Topic: order-event-topic  Partition: 0  Leader: 1  Replicas: 1,2,3  Isr: 1,2,3
```

如果后续需要增加分区，可以执行：

```bash
/opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server kafka-01.internal:9092,kafka-02.internal:9092,kafka-03.internal:9092 \
  --alter \
  --topic order-event-topic \
  --partitions 16
```

`--alter --partitions 16` 表示将 Topic 分区数增加到 16。Kafka 支持增加分区，但不支持直接减少分区。因此初始分区数需要谨慎规划，避免后续无法回退。

增加分区需要特别注意消息顺序问题。对于使用 Key 的消息，增加分区后，相同 Key 的分区计算结果可能发生变化，后续同一个 Key 的新消息可能进入新的 Partition，导致跨扩容时间点的顺序无法完全保证。

Partition 规划建议如下：

| 项目       | 建议                                     |
| ---------- | ---------------------------------------- |
| 初始数量   | 按目标吞吐和消费并发规划，不要只用默认值 |
| 是否可增加 | 可以增加，但无法直接减少                 |
| 顺序要求   | 需要顺序的业务必须使用稳定 Key           |
| 热点 Key   | 避免某个 Key 消息过多导致单分区热点      |
| 分区过多   | 会增加 Rebalance 和集群元数据压力        |
| 分区扩容   | 扩容前评估 Key 分布和顺序影响            |
| 压测验证   | 核心 Topic 分区数应通过压测验证          |

如果业务对同一订单的事件顺序有要求，建议使用 `orderId` 作为 Key：

```java
kafkaTemplate.send("order-event-topic", orderId, jsonMessage);
```

这样同一个 `orderId` 的消息通常会进入同一个 Partition。但需要注意，这只保证同一 Partition 内的局部顺序，不保证整个 Topic 全局顺序。

### 消费并发配置

消费并发配置决定 Consumer Group 内有多少消费者线程同时处理消息。Spring Kafka 中通常通过 `spring.kafka.listener.concurrency` 或 `@KafkaListener(concurrency = "...")` 设置。

核心原则是：同一个 Consumer Group 内，有效消费并发不能超过 Topic 分区数。即使配置了更多消费者线程，多出来的线程也无法分配到 Partition，只会空闲。

例如：

```text
Topic 分区数：3
Consumer Group 并发数：5

有效消费线程：3
空闲消费线程：2
```

全局消费并发配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  kafka:
    listener:
      # 全局消费者并发数，建议小于或等于主要 Topic 的分区数
      concurrency: 3

      # 手动提交 Offset，业务处理成功后再确认
      ack-mode: manual

    consumer:
      # 单次 poll 拉取最大消息数，批量处理时需要结合业务耗时调整
      max-poll-records: 100

      properties:
        # 两次 poll 之间允许的最大间隔，处理慢时适当调大
        max.poll.interval.ms: 300000

        # 心跳间隔，通常小于 session.timeout.ms
        heartbeat.interval.ms: 3000

        # 会话超时时间，超时后触发 Rebalance
        session.timeout.ms: 45000
```

也可以在单个监听器上单独配置并发数。

```java
@KafkaListener(
        id = "orderEventConsumer",
        topics = "order-event-topic",
        groupId = "order-event-group",
        concurrency = "6"
)
public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
    // 消费逻辑
}
```

当一个服务同时消费多个 Topic 时，建议为不同业务配置独立监听器，并根据 Topic 分区数和业务处理能力单独设置并发。

消费并发规划需要同时考虑以下因素：

| 因素           | 说明                                     |
| -------------- | ---------------------------------------- |
| Topic 分区数   | 决定同一 Group 最大有效并发              |
| 单条处理耗时   | 单条消息处理越慢，需要更多并发或优化逻辑 |
| 下游承载能力   | 数据库、Redis、HTTP 接口是否能承受并发   |
| 消费顺序       | 并发越高，跨分区顺序越不可控             |
| Offset 提交    | 批量和手动提交会影响重复消费范围         |
| Rebalance 成本 | 并发实例越多，Rebalance 影响越明显       |

可以使用下面命令查看 Consumer Group 消费情况：

```bash
/opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server kafka-01.internal:9092,kafka-02.internal:9092,kafka-03.internal:9092 \
  --describe \
  --group order-event-group
```

重点关注 `LAG`：

```text
GROUP              TOPIC              PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
order-event-group  order-event-topic  0          1200            1300            100
order-event-group  order-event-topic  1          1500            1500            0
order-event-group  order-event-topic  2          800             1200            400
```

`LAG` 表示消费积压数量。如果某些 Partition 的 `LAG` 长时间较高，说明消费速度低于生产速度，可能存在以下问题：

| 现象                  | 可能原因                       |
| --------------------- | ------------------------------ |
| 所有 Partition 都积压 | 消费者整体处理能力不足         |
| 单个 Partition 积压   | 存在热点 Key 或单分区消息异常  |
| 消费者频繁上下线      | 服务不稳定或频繁发布           |
| LAG 不变              | 消费者可能已经停止消费         |
| LAG 上升很快          | 生产流量突增或下游处理变慢     |
| Rebalance 频繁        | 消费超时、实例抖动或配置不合理 |

消费并发调优建议如下：

| 调优方向     | 建议                                              |
| ------------ | ------------------------------------------------- |
| 提高并发     | 增加 `listener.concurrency`，但不要超过分区数太多 |
| 增加分区     | 如果并发受限于分区数，需要扩容 Topic 分区         |
| 优化业务逻辑 | 减少单条消息处理耗时                              |
| 批量处理     | 对数据库写入、搜索同步等场景使用批量消费          |
| 降低外部调用 | 避免每条消息都同步调用慢接口                      |
| 使用异步处理 | 非关键后置动作可拆分到其他 Topic                  |
| 控制重试     | 避免失败消息长时间阻塞主消费链路                  |

消费并发不是越高越好。如果消费者内部会写数据库、调用下游接口或竞争锁资源，并发过高可能导致数据库连接池耗尽、接口限流、锁等待增加，反而降低整体吞吐。

生产环境建议根据压测结果配置并发，而不是只按分区数拉满。

### 日志与问题排查

Kafka 问题排查需要同时看 Producer、Broker、Consumer 和业务系统日志。生产环境中，日志必须包含足够上下文，否则一旦出现消息丢失、重复消费、消费堆积或死信堆积，很难定位问题。

Producer 发送日志建议包含：

| 字段         | 说明                 |
| ------------ | -------------------- |
| `topic`      | 目标 Topic           |
| `key`        | 消息 Key             |
| `messageId`  | 消息唯一 ID          |
| `businessId` | 业务 ID，例如订单 ID |
| `eventType`  | 事件类型             |
| `partition`  | 写入分区             |
| `offset`     | 写入位点             |
| `error`      | 发送失败异常         |

Consumer 消费日志建议包含：

| 字段         | 说明        |
| ------------ | ----------- |
| `topic`      | 消费 Topic  |
| `partition`  | 消费分区    |
| `offset`     | 消息位点    |
| `key`        | 消息 Key    |
| `messageId`  | 消息唯一 ID |
| `businessId` | 业务 ID     |
| `groupId`    | 消费者组    |
| `eventType`  | 事件类型    |
| `costMs`     | 消费耗时    |
| `error`      | 消费异常    |

推荐在消费逻辑中记录处理耗时。

文件位置：`src/main/java/io/github/atengk/kafka/consumer/OrderEventTraceConsumer.java`

下面的代码演示如何在消费者中记录 Topic、Partition、Offset、Key、业务 ID 和消费耗时。

```java
package io.github.atengk.kafka.consumer;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.dto.OrderEventMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 订单事件链路日志消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class OrderEventTraceConsumer {

    /**
     * 消费订单事件并记录链路日志。
     *
     * @param record Kafka 消息记录
     * @param acknowledgment Offset 确认对象
     */
    @KafkaListener(
            id = "orderEventTraceConsumer",
            topics = KafkaTopicConstant.ORDER_EVENT_TOPIC,
            groupId = "order-event-trace-group"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        OrderEventMessage message = null;

        try {
            String jsonMessage = record.value();
            if (StrUtil.isBlank(jsonMessage)) {
                log.warn("Kafka消息为空，topic={}，partition={}，offset={}，key={}",
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        record.key());
                acknowledgment.acknowledge();
                return;
            }

            message = JSONUtil.toBean(jsonMessage, OrderEventMessage.class);

            log.info("开始处理Kafka消息，topic={}，partition={}，offset={}，key={}，orderId={}，messageId={}，eventType={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key(),
                    ObjUtil.isNull(message) ? null : message.getOrderId(),
                    ObjUtil.isNull(message) ? null : message.getMessageId(),
                    ObjUtil.isNull(message) ? null : message.getEventType());

            // 这里编写真正的业务处理逻辑

            acknowledgment.acknowledge();

            stopWatch.stop();

            log.info("Kafka消息处理完成，topic={}，partition={}，offset={}，key={}，orderId={}，messageId={}，costMs={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key(),
                    ObjUtil.isNull(message) ? null : message.getOrderId(),
                    ObjUtil.isNull(message) ? null : message.getMessageId(),
                    stopWatch.getTotalTimeMillis());
        } catch (Exception e) {
            stopWatch.stop();

            log.error("Kafka消息处理失败，topic={}，partition={}，offset={}，key={}，orderId={}，messageId={}，costMs={}，原因={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key(),
                    ObjUtil.isNull(message) ? null : message.getOrderId(),
                    ObjUtil.isNull(message) ? null : message.getMessageId(),
                    stopWatch.getTotalTimeMillis(),
                    e.getMessage(),
                    e);

            throw e;
        }
    }

}
```

常见问题排查方式如下。

| 问题           | 排查重点                                        |
| -------------- | ----------------------------------------------- |
| 消息发送失败   | Broker 地址、Topic 是否存在、权限、序列化、网络 |
| 消息消费不到   | Topic、Group ID、Offset、监听器是否启动         |
| 消息重复消费   | Offset 提交时机、服务重启、Rebalance、幂等逻辑  |
| 消息丢失       | Producer `acks`、异常吞掉、提前提交 Offset      |
| 消息堆积       | LAG、消费耗时、下游依赖、分区数、并发数         |
| 死信堆积       | 异常类型、重试策略、消息格式、补偿任务          |
| Rebalance 频繁 | 消费耗时、心跳配置、实例抖动、发布频率          |
| 单分区积压     | 热点 Key、分区数据倾斜、单条消息处理慢          |

常用排查命令如下。

查看 Topic 列表：

```bash
/opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server kafka-01.internal:9092,kafka-02.internal:9092,kafka-03.internal:9092 \
  --list
```

查看 Topic 详情：

```bash
/opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server kafka-01.internal:9092,kafka-02.internal:9092,kafka-03.internal:9092 \
  --describe \
  --topic order-event-topic
```

查看 Consumer Group 消费进度：

```bash
/opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server kafka-01.internal:9092,kafka-02.internal:9092,kafka-03.internal:9092 \
  --describe \
  --group order-event-group
```

临时消费 Topic 消息：

```bash
/opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka-01.internal:9092,kafka-02.internal:9092,kafka-03.internal:9092 \
  --topic order-event-topic \
  --from-beginning \
  --max-messages 10 \
  --property print.key=true \
  --property key.separator=" | "
```

临时发送测试消息：

```bash
/opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server kafka-01.internal:9092,kafka-02.internal:9092,kafka-03.internal:9092 \
  --topic order-event-topic \
  --property parse.key=true \
  --property key.separator="|"
```

输入示例：

```text
ORDER_DEBUG_70001|{"messageId":"MSG_DEBUG_70001","orderId":"ORDER_DEBUG_70001","userId":1001,"amount":99.80,"eventType":"CREATED","eventTime":"2026-04-30T10:30:00"}
```

查看死信 Topic：

```bash
/opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka-01.internal:9092,kafka-02.internal:9092,kafka-03.internal:9092 \
  --topic order-event-topic.DLT \
  --from-beginning \
  --max-messages 20 \
  --property print.key=true \
  --property key.separator=" | "
```

排查命令中的 `--bootstrap-server` 需要替换为实际生产 Kafka Broker 地址；`--group` 需要替换为实际 Consumer Group；`--max-messages` 用于限制临时消费数量，避免在生产环境一次性打印过多消息。

生产日志配置建议如下。

文件位置：`src/main/resources/logback-spring.xml`

下面的配置示例将应用日志输出到控制台和文件，并保留 Kafka 相关日志排查能力。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 应用名称，用于日志文件区分 -->
    <springProperty scope="context" name="APP_NAME" source="spring.application.name"/>

    <!-- 日志目录，生产环境建议挂载到统一日志路径 -->
    <property name="LOG_PATH" value="./logs"/>

    <!-- 控制台日志格式 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 文件日志，按天滚动 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${APP_NAME}.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- 按日期归档日志 -->
            <fileNamePattern>${LOG_PATH}/${APP_NAME}.%d{yyyy-MM-dd}.log</fileNamePattern>
            <!-- 保留 30 天日志 -->
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>

    <!-- Spring Kafka 日志，生产环境默认 INFO，排查问题时可临时调整 DEBUG -->
    <logger name="org.springframework.kafka" level="INFO"/>

    <!-- Kafka 客户端日志，生产环境默认 WARN，避免输出过多内部日志 -->
    <logger name="org.apache.kafka" level="WARN"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

排查问题时，可以临时将 Kafka 客户端日志调高，但不建议长期保持 DEBUG。

文件位置：`src/main/resources/application.yml`

```yaml
logging:
  level:
    # 排查 Spring Kafka 监听容器、异常处理、提交 Offset 等问题时临时开启
    org.springframework.kafka: DEBUG

    # 排查 Kafka 客户端网络、元数据、消费组等问题时临时开启，日志量较大
    org.apache.kafka: INFO
```

生产监控建议至少包含以下指标：

| 指标                | 说明                            |
| ------------------- | ------------------------------- |
| Producer 发送成功数 | 判断消息发送是否正常            |
| Producer 发送失败数 | 及时发现 Broker、网络或权限问题 |
| Consumer 消费成功数 | 判断消费是否正常                |
| Consumer 消费失败数 | 判断业务异常是否增加            |
| Consumer Lag        | 判断是否存在消息堆积            |
| DLT 消息数          | 判断死信是否增加                |
| 消费耗时            | 判断业务处理是否变慢            |
| Rebalance 次数      | 判断消费者组是否稳定            |
| Broker 磁盘使用率   | 判断消息保留和磁盘容量是否安全  |
| Topic 流入流出速率  | 判断 Topic 是否接近容量上限     |

生产问题处理建议如下：

| 问题           | 处理建议                               |
| -------------- | -------------------------------------- |
| 少量消费失败   | 查看业务异常，确认是否进入重试或 DLT   |
| 大量消费失败   | 可以先暂停消费者，避免失败扩散         |
| 消息严重堆积   | 临时扩容消费者，确认分区数是否限制并发 |
| 单分区堆积     | 检查热点 Key 和单条慢消息              |
| 死信持续增加   | 优先修复根因，再考虑重放               |
| 发送失败增加   | 检查 Broker、网络、权限、Topic 配置    |
| Rebalance 频繁 | 检查实例稳定性、消费耗时和心跳配置     |
| Offset 异常    | 谨慎重置 Offset，操作前确认影响范围    |

重置 Offset 属于高风险操作，生产环境必须谨慎执行。操作前需要确认 Topic、Group ID、时间点、影响消费者和是否允许重复消费。

示例命令如下：

```bash
/opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server kafka-01.internal:9092,kafka-02.internal:9092,kafka-03.internal:9092 \
  --group order-event-group \
  --topic order-event-topic \
  --reset-offsets \
  --to-earliest \
  --dry-run
```

`--dry-run` 表示只预览变更，不真正执行。确认无误后才可以将 `--dry-run` 改为 `--execute`。

```bash
/opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server kafka-01.internal:9092,kafka-02.internal:9092,kafka-03.internal:9092 \
  --group order-event-group \
  --topic order-event-topic \
  --reset-offsets \
  --to-earliest \
  --execute
```

重置 Offset 会导致消息重新消费，业务必须具备幂等能力。对于订单、支付、库存、资金等核心业务，重置前应先评估重复处理风险，并保留操作记录。

生产使用最终建议如下：

| 分类      | 建议                                              |
| --------- | ------------------------------------------------- |
| Topic     | 业务语义清晰，主 Topic 和 DLT 配套规划            |
| Partition | 基于吞吐、并发和压测结果规划                      |
| Producer  | 使用 `acks=all`、幂等生产、必要时落本地消息表     |
| Consumer  | 手动提交 Offset，失败进入重试或死信               |
| 幂等      | 所有消费者默认支持重复消费                        |
| 重试      | 临时异常重试，永久异常死信                        |
| 日志      | 记录完整 Kafka 元数据和业务 ID                    |
| 监控      | 重点关注 Lag、失败率、死信数和消费耗时            |
| 运维      | Offset 重置、Topic 扩容、死信重放都需要审批和记录 |

Kafka 在生产环境中的核心目标不是“绝对不出错”，而是在消息发送失败、消费失败、重复消费、消息堆积和下游异常时，系统仍然能够定位问题、隔离影响、补偿恢复，并保持业务结果一致。