# RocketMQ 开发使用文档

## RocketMQ 概述

RocketMQ 是一款分布式消息中间件，主要用于解决系统之间的异步通信、业务解耦、削峰填谷、事件驱动和最终一致性问题。在 Spring Boot 项目中，RocketMQ 通常作为业务系统之间的消息通道，由生产者发送业务事件，由消费者订阅并处理消息，从而降低系统之间的直接调用依赖。

RocketMQ 的基础消息模型是发布订阅模型，核心角色包括 Producer、Consumer、Topic、Message、NameServer 和 Broker。Producer 负责生产并发送消息，Consumer 负责订阅并消费消息，Topic 用于按业务语义组织消息，Broker 负责消息存储与投递，NameServer 负责 Broker 路由注册与发现。([RocketMQ](https://rocketmq.apache.org/docs/4.x/introduction/02whatis/?utm_source=chatgpt.com))

### 核心概念

本节介绍 RocketMQ 开发中最常接触的核心概念。理解这些概念后，后续的消息发送、消息消费、事务消息、顺序消息和异常处理会更容易落地。

| 概念           | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| Producer       | 消息生产者，通常集成在业务系统中，用于将订单、支付、库存、日志等业务数据封装为消息并发送到 RocketMQ。Producer 支持同步发送、异步发送、单向发送、顺序发送、事务消息等方式。 |
| Consumer       | 消息消费者，通常集成在下游业务系统中，用于订阅 Topic 并处理消息。Consumer 可以按消费组进行水平扩展，同一消费组内的消费者通常采用负载均衡方式消费。 |
| Topic          | 消息主题，是 RocketMQ 中消息归类和路由的顶层容器。实际开发中通常按照业务域划分 Topic，例如订单 Topic、支付 Topic、库存 Topic。 |
| Message        | 消息，是 RocketMQ 中传输数据的最小单位。消息一般包含 Topic、Tag、Key、消息体和扩展属性。业务数据通常序列化后放入消息体。 |
| Tag            | 消息标签，用于在 Topic 内进一步细分消息类型。消费者可以按 Tag 过滤消息，例如同一个订单 Topic 下区分 `created`、`paid`、`cancelled` 等事件。 |
| Key            | 消息业务键，用于消息检索和问题排查。实际项目中通常使用订单号、支付单号、用户 ID 等唯一或近似唯一的业务标识作为 Key。 |
| MessageQueue   | Topic 下的队列分区，是 RocketMQ 消息存储和负载均衡的基本单位。顺序消息通常依赖相同业务键路由到同一个队列来保证局部顺序。 |
| Consumer Group | 消费者组，表示一组具有相同消费逻辑的消费者实例。同一消费组内的实例共同消费消息，用于实现消费端水平扩展。 |
| NameServer     | 路由注册中心，负责维护 Broker 和 Topic 的路由信息。Producer 和 Consumer 会通过 NameServer 获取 Broker 地址。 |
| Broker         | 消息服务器，负责消息存储、投递、查询和高可用能力，是 RocketMQ 服务端的核心组件。 |

RocketMQ 5.0 文档中还明确区分了消息类型，包括普通消息、FIFO 顺序消息、事务消息和延迟消息。实际项目中不要把不同语义的消息混用在同一个 Topic 中，尤其是 RocketMQ 5.x 项目，应尽量按消息类型和业务语义规划 Topic。([RocketMQ](https://rocketmq.apache.org/docs/introduction/02concepts/?utm_source=chatgpt.com))

### 典型使用场景

本节说明 RocketMQ 在业务系统中的常见落地场景。实际开发时，应先判断业务是否需要异步化、可靠投递、消费重试、顺序保证或最终一致性，再选择合适的消息模型。

| 场景                 | 说明                                                         | 示例                                                         |
| -------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| 异步解耦             | 将主流程与非核心流程拆开，主流程只负责发送消息，下游系统异步处理。 | 用户下单后，订单系统发送订单创建消息，积分、优惠券、通知系统分别订阅处理。 |
| 削峰填谷             | 高并发请求先进入消息队列，下游按自身处理能力消费，避免瞬时流量压垮数据库或外部接口。 | 秒杀下单、活动报名、批量导入任务。                           |
| 事件驱动             | 业务系统通过事件消息驱动状态变更，降低模块之间的同步调用依赖。 | 订单已支付事件驱动发货、开票、会员成长值更新。               |
| 数据同步             | 上游系统将数据变更封装为消息，下游系统订阅后更新本地数据。   | 商品信息同步到搜索系统，用户数据同步到风控系统。             |
| 顺序处理             | 同一业务对象的多个事件需要按固定顺序处理。                   | 同一个订单的创建、支付、发货、完成事件需要按顺序消费。       |
| 分布式事务最终一致性 | 本地事务执行成功后，通过事务消息通知下游系统，保证核心业务与消息发送之间的最终一致性。 | 支付成功后发送支付完成消息，下游系统更新订单状态、账户流水和通知记录。 |
| 延迟处理             | 消息在指定延迟后才被消费，用于超时关闭、延迟检查等场景。     | 订单创建 30 分钟未支付自动取消，任务提交后延迟检查处理结果。 |

普通消息适合异步解耦、数据集成和事件驱动场景；顺序消息适合状态同步、交易撮合、增量数据同步等需要保证局部顺序的场景；事务消息适合本地事务与消息发送需要最终一致的分布式业务场景。([RocketMQ](https://rocketmq.apache.org/docs/featureBehavior/01normalmessage/?utm_source=chatgpt.com))

在项目设计时，不建议把 RocketMQ 当成普通方法调用的替代品。消息通信天然具有异步、重试、重复消费和最终一致性的特点，因此消费者必须具备幂等处理能力，生产者也需要记录关键业务日志，便于后续排查消息发送、消费失败和数据不一致问题。

### Spring Boot 集成方式

本节介绍 Spring Boot 项目中集成 RocketMQ 的整体方式。详细依赖、配置、发送代码和消费代码会在后续章节展开，这里只说明推荐的集成思路和开发边界。

Spring Boot 项目通常通过 `rocketmq-spring-boot-starter` 集成 RocketMQ。该 Starter 提供了 `RocketMQTemplate`、消息监听注解、自动配置和常用消息发送能力，可以减少直接使用原生 RocketMQ Client 的样板代码。Apache RocketMQ Spring 项目说明其目标是帮助开发者快速集成 RocketMQ 与 Spring Boot，并支持同步发送、异步发送、单向发送、顺序消息、批量消息、事务消息、延迟消息、广播/集群消费、Tag 或 SQL92 过滤等能力。([GitHub](https://github.com/apache/rocketmq-spring?utm_source=chatgpt.com))

Spring Boot 3 项目需要特别关注 RocketMQ Spring 版本兼容性。RocketMQ Spring 发布记录中，`rocketmq-spring-all-2.3.0` 已包含 Spring Boot 3.x 支持相关变更；实际项目中应结合 JDK、Spring Boot、RocketMQ 服务端版本和公司依赖管理规范统一选择版本。([GitHub](https://github.com/apache/rocketmq-spring/releases?utm_source=chatgpt.com))

Maven 依赖通常放在业务服务的 `pom.xml` 中，后续章节会给出完整版本管理方式。

```xml
<!-- RocketMQ Spring Boot Starter：用于在 Spring Boot 中集成 RocketMQ 生产者、消费者和 RocketMQTemplate -->
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <version>${rocketmq-spring.version}</version>
</dependency>
```

基础配置通常放在 `src/main/resources/application.yml` 中，核心配置包括 NameServer 地址、生产者组、发送超时时间、重试次数等。

```yaml
rocketmq:
  # RocketMQ NameServer 地址，多个地址可按客户端规范配置
  name-server: 127.0.0.1:9876
  producer:
    # 当前服务的生产者组名称，建议按应用或业务域命名
    group: demo-producer-group
    # 消息发送超时时间，单位毫秒
    send-message-timeout: 3000
    # 同步发送失败后的重试次数
    retry-times-when-send-failed: 2
```

在代码层面，Spring Boot 集成 RocketMQ 通常分为两类开发方式。

第一类是生产者发送消息。业务代码通过注入 `RocketMQTemplate` 发送消息，适合封装统一消息发送组件，例如统一处理 Topic、Tag、Key、消息体序列化、日志记录和异常转换。

第二类是消费者监听消息。消费端通过 `@RocketMQMessageListener` 声明 Topic、Consumer Group、消费模式和选择器表达式，并实现 `RocketMQListener<T>` 处理具体业务逻辑。消费者代码中必须考虑重复消费、消费失败重试、异常日志、业务幂等和死信队列处理。

实际项目中建议按以下方式组织 RocketMQ 集成代码：

| 模块       | 建议职责                                                     |
| ---------- | ------------------------------------------------------------ |
| `config`   | 放置 RocketMQ 相关配置、常量、属性类。                       |
| `producer` | 封装统一消息发送组件，避免业务代码散落调用 `RocketMQTemplate`。 |
| `consumer` | 放置各业务消费者，按 Topic 或业务域分包管理。                |
| `model`    | 定义消息体对象、消息事件对象和通用消息包装结构。             |
| `constant` | 定义 Topic、Tag、Consumer Group、Message Key 前缀等常量。    |

Spring Boot 集成 RocketMQ 时，建议遵循以下原则。

1. Topic 按业务域划分，Tag 按事件类型细分，避免一个 Topic 承载过多无关业务。
2. Producer 不要在每次发送消息时重复创建和销毁，应交由 Spring 容器统一管理。
3. Consumer 必须保证幂等，因为 RocketMQ 消息在异常、超时、重试等场景下可能被重复投递。
4. 消息体要保持稳定的数据结构，避免直接发送数据库实体类。
5. 关键消息必须设置业务 Key，便于按订单号、支付单号或业务流水号排查问题。
6. 发送失败和消费失败必须记录清晰日志，日志中至少包含 Topic、Tag、Key、业务 ID 和异常信息。

## 开发环境

本节用于统一 RocketMQ 开发环境的基础约束，包括 JDK 版本、Spring Boot 版本、RocketMQ 服务端版本和本地启动方式。后续所有生产者、消费者、事务消息、顺序消息和测试示例，都默认基于本节环境展开。

### JDK 21 环境说明

项目开发环境建议统一使用 JDK 21。JDK 21 是当前 Java 生态中常用的长期支持版本，Oracle 已在 Java 21 发布说明中说明会为 JDK 21 提供至少 8 年长期支持；Spring Boot 3 系列最低要求 Java 17，因此使用 JDK 21 能满足 Spring Boot 3 的运行要求，并便于后续使用虚拟线程、Record、模式匹配等较新的 Java 语言能力。([Oracle 博客](https://blogs.oracle.com/java/post/the-arrival-of-java-21?utm_source=chatgpt.com))

开发机、构建机和运行容器建议保持同一主版本，避免本地使用 JDK 21、CI 使用 JDK 17、生产环境使用其他版本导致编译参数、依赖兼容性或运行行为不一致。

本地先检查 JDK 版本，确认 `java` 和 `javac` 都指向 JDK 21。

```bash
# 查看 Java 运行时版本
java -version

# 查看 Java 编译器版本
javac -version

# 查看当前 JAVA_HOME 配置
echo $JAVA_HOME
```

如果使用 Maven 构建，建议在 `pom.xml` 中显式声明 Java 版本，避免项目被低版本 JDK 误编译。

文件位置：`pom.xml`

```xml
<properties>
    <!-- 项目统一使用 JDK 21 编译 -->
    <java.version>21</java.version>

    <!-- Maven 编译插件使用的源码版本 -->
    <maven.compiler.source>21</maven.compiler.source>

    <!-- Maven 编译插件生成的目标字节码版本 -->
    <maven.compiler.target>21</maven.compiler.target>

    <!-- Spring Boot 版本由父工程或 dependencyManagement 统一管理 -->
    <spring-boot.version>3.5.13</spring-boot.version>
</properties>
```

如果团队中存在多 JDK 并存的情况，建议使用 SDKMAN、jEnv、asdf 或 IDE Project SDK 固定项目 JDK。对于后端服务，不建议只依赖开发人员全局环境，CI/CD 也需要明确指定 JDK 21。

### Spring Boot 3 版本要求

RocketMQ 集成示例默认使用 Spring Boot 3。Spring Boot 当前官方系统要求页面列出了仍处于稳定维护序列的 Spring Boot 3.5、3.4、3.3 等版本；Spring Boot 3.3 系列最低要求 Java 17，并依赖 Spring Framework 6.1 以上版本，因此在 JDK 21 环境下运行 Spring Boot 3 是合理的。([Home](https://docs.spring.io/spring-boot/system-requirements.html?utm_source=chatgpt.com))

项目中建议优先使用公司统一版本。如果没有统一约束，可以按以下策略选择：

| 场景                      | 建议版本策略                                                 |
| ------------------------- | ------------------------------------------------------------ |
| 新项目                    | 优先选择当前稳定维护的 Spring Boot 3.5.x。                   |
| 已有 Spring Boot 3.4 项目 | 保持 3.4.x，并只做补丁版本升级。                             |
| 已有 Spring Boot 3.3 项目 | 可以继续维护，但不建议在新项目中主动选择旧小版本。           |
| Spring Boot 2.x 老项目    | 不建议直接套用本文档示例，需要先处理 `javax.*` 到 `jakarta.*` 的迁移问题。 |

RocketMQ Spring 当前已发布 2.3.3，RocketMQ Spring 2.3.0 发布说明中包含 Spring Boot 3.x 支持相关改动，2.3.3 是后续补丁版本，修复了异步发送 Future 未完成、延迟消息 `delayTime=0` 等问题。Spring Boot 3 项目建议使用 RocketMQ Spring 2.3.x 系列，并优先选择较新的补丁版本。([RocketMQ](https://rocketmq.apache.org/zh/release-notes/2024/02/19/release-notes-rocketmq-spring-2.3.0/?utm_source=chatgpt.com))

Maven 依赖版本建议统一放在父工程或当前服务的 `properties` 中，避免不同模块使用不同 RocketMQ Spring 版本。

文件位置：`pom.xml`

```xml
<properties>
    <!-- Spring Boot 3 稳定版本，实际项目可按公司 BOM 统一管理 -->
    <spring-boot.version>3.5.13</spring-boot.version>

    <!-- RocketMQ Spring Boot Starter 版本，Spring Boot 3 项目建议使用 2.3.x 系列 -->
    <rocketmq-spring.version>2.3.3</rocketmq-spring.version>
</properties>

<dependencies>
    <!-- Spring Boot Web：用于提供测试接口、健康检查和业务 API -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- RocketMQ Spring Boot Starter：用于集成 RocketMQTemplate 和消息监听器 -->
    <dependency>
        <groupId>org.apache.rocketmq</groupId>
        <artifactId>rocketmq-spring-boot-starter</artifactId>
        <version>${rocketmq-spring.version}</version>
    </dependency>

    <!-- Lombok：减少 DTO、配置类和日志对象中的样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Hutool：用于 JSON、字符串、集合、日期等常用工具处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>
</dependencies>
```

Spring Boot 3 使用 `jakarta.*` 命名空间。项目中如果存在老版本依赖，尤其是 Servlet、Validation、JPA、Swagger、老版安全组件或老版中间件 Starter，需要重点检查是否仍依赖 `javax.*`，否则可能出现启动失败、类找不到或自动配置不生效的问题。

### RocketMQ 服务端版本选择

RocketMQ 服务端建议选择 5.x 系列。当前 RocketMQ 官方下载页已列出 5.4.0，发布时间为 2025-12-24；5.4.0 发布说明中说明该版本新增 Priority Message，并将 Timer Message、Transaction Message 和 Index 基于 RocksDB 实现，同时包含若干修复。([RocketMQ](https://rocketmq.apache.org/zh/download/?utm_source=chatgpt.com))

本地开发建议使用与测试环境、生产环境尽量一致的 RocketMQ 服务端版本。若新项目没有历史包袱，可以使用 5.4.0；如果团队当前仍以 5.3.x 为主，则本地开发应与团队环境保持一致，避免本地验证通过但测试环境行为不一致。

| 版本                   | 建议使用场景                                                 |
| ---------------------- | ------------------------------------------------------------ |
| RocketMQ 5.4.0         | 新项目、本地新环境、需要跟进当前 5.x 能力的项目。            |
| RocketMQ 5.3.4 / 5.3.3 | 已有 5.3.x 环境，适合保持小版本一致并逐步升级。              |
| RocketMQ 5.3.2         | 官方 Docker 快速开始文档仍以该版本作为示例，适合参考文档命令进行本地验证。 |
| RocketMQ 4.9.x         | 老项目兼容场景，不建议新项目主动选择。                       |

需要注意，RocketMQ 服务端版本、RocketMQ Spring 版本、客户端协议和项目使用特性需要一起评估。普通消息、顺序消息、延迟消息、事务消息等常用能力在 5.x 中都可以使用，但如果项目使用 5.x 新特性，例如 Proxy、gRPC、优先级消息、RocksDB 相关能力，则需要确认服务端、客户端和 Starter 版本是否匹配。

本地开发默认采用以下版本组合：

| 组件            | 推荐版本                  |
| --------------- | ------------------------- |
| JDK             | 21                        |
| Spring Boot     | 3.5.x                     |
| RocketMQ Spring | 2.3.3                     |
| RocketMQ Server | 5.4.0                     |
| 构建工具        | Maven 3.9.x 或 Gradle 8.x |
| 本地部署方式    | Docker / Docker Compose   |

### 本地开发环境准备

本地开发推荐使用 Docker 启动单节点 RocketMQ，用于验证消息发送、消息消费、消费重试和基本 Topic 配置。RocketMQ 官方 Docker 快速开始文档说明，Docker 部署会启动 NameServer、Broker 和 Proxy，并通过容器网络完成组件间通信；官方示例中 NameServer 端口为 `9876`，Broker 常用端口包括 `10909`、`10911`、`10912`，Proxy 常用端口包括 `8080`、`8081`。([RocketMQ](https://rocketmq.apache.org/zh/docs/quickStart/02quickstartWithDocker/?utm_source=chatgpt.com))

本地建议准备以下软件：

| 软件           | 用途                             |
| -------------- | -------------------------------- |
| JDK 21         | 编译和运行 Spring Boot 项目。    |
| Maven 3.9.x    | 构建项目、管理依赖。             |
| Docker         | 启动本地 RocketMQ 服务端。       |
| Docker Compose | 编排 NameServer、Broker、Proxy。 |
| IntelliJ IDEA  | 开发和调试 Spring Boot 服务。    |
| curl / Postman | 调用测试接口，验证消息发送。     |

先创建本地 RocketMQ 工作目录，并准备 Broker 配置文件。

```bash
# 创建 RocketMQ 本地工作目录
mkdir -p ~/develop/rocketmq

# 进入工作目录
cd ~/develop/rocketmq

# 创建 Broker 配置文件
cat > broker.conf <<'EOF'
# Broker 所属集群名称，本地单节点使用默认集群即可
brokerClusterName=DefaultCluster

# Broker 名称，本地单节点使用 broker-a
brokerName=broker-a

# Broker ID，0 表示 Master
brokerId=0

# 本地开发固定为 127.0.0.1，便于宿主机 Spring Boot 服务访问
brokerIP1=127.0.0.1

# 本地开发允许自动创建 Topic，生产环境不建议开启
autoCreateTopicEnable=true

# 删除过期消息的时间点，默认凌晨 4 点
deleteWhen=04

# 消息文件保留时间，单位小时
fileReservedTime=48
EOF
```

使用 Docker 启动 NameServer、Broker 和 Proxy。这里使用 `apache/rocketmq:5.4.0`，Docker Hub 当前已存在该镜像标签；如果团队环境固定为 5.3.2，可以把命令中的 `5.4.0` 替换为 `5.3.2`，并同步修改挂载路径中的版本号。([Docker Hub](https://hub.docker.com/r/apache/rocketmq?utm_source=chatgpt.com))

```bash
# 创建 RocketMQ 容器网络，已存在时忽略错误
docker network create rocketmq || true

# 启动 NameServer
docker run -d \
  --name rmqnamesrv \
  --network rocketmq \
  -p 9876:9876 \
  apache/rocketmq:5.4.0 \
  sh mqnamesrv

# 查看 NameServer 启动日志
docker logs -f rmqnamesrv
```

看到 `The Name Server boot success` 相关日志后，再启动 Broker 和 Proxy。

```bash
# 启动 Broker，并启用 Proxy
docker run -d \
  --name rmqbroker \
  --network rocketmq \
  -p 10909:10909 \
  -p 10911:10911 \
  -p 10912:10912 \
  -p 8080:8080 \
  -p 8081:8081 \
  -e "NAMESRV_ADDR=rmqnamesrv:9876" \
  -v "$(pwd)/broker.conf:/home/rocketmq/rocketmq-5.4.0/conf/broker.conf" \
  apache/rocketmq:5.4.0 \
  sh mqbroker --enable-proxy \
  -c /home/rocketmq/rocketmq-5.4.0/conf/broker.conf

# 查看 Broker 容器日志
docker logs -f rmqbroker
```

启动完成后，可以通过以下命令检查容器状态和端口映射。

```bash
# 查看 RocketMQ 相关容器
docker ps --filter "name=rmq"

# 查看 NameServer 日志
docker logs --tail 100 rmqnamesrv

# 查看 Broker 日志
docker logs --tail 100 rmqbroker
```

Spring Boot 项目连接本地 RocketMQ 时，基础配置放在 `src/main/resources/application.yml`。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 本地开发服务端口
  port: 8088

spring:
  application:
    # 当前 Spring Boot 服务名称
    name: rocketmq-demo-service

rocketmq:
  # 本地 RocketMQ NameServer 地址
  name-server: 127.0.0.1:9876

  producer:
    # 生产者组名称，建议按应用名或业务域命名
    group: rocketmq-demo-producer-group

    # 消息发送超时时间，单位毫秒
    send-message-timeout: 3000

    # 同步发送失败后的重试次数
    retry-times-when-send-failed: 2

    # 异步发送失败后的重试次数
    retry-times-when-send-async-failed: 2
```

本地开发时，常用管理命令如下。

```bash
# 停止 RocketMQ 容器
docker stop rmqbroker rmqnamesrv

# 删除 RocketMQ 容器
docker rm rmqbroker rmqnamesrv

# 删除 RocketMQ 网络
docker network rm rocketmq

# 重新启动已有容器
docker start rmqnamesrv rmqbroker
```

本地环境验证重点不是只看容器是否启动，而是要确认 Spring Boot 服务可以正常连接 NameServer，并且生产者能够成功发送消息、消费者能够正常订阅消息。后续章节中的同步消息发送、普通消息消费和本地 RocketMQ 验证，会基于这里的 `127.0.0.1:9876` 继续展开。

## RocketMQ 基础架构

本节补充 RocketMQ 基础架构章节，围绕 Producer、Consumer、Topic、Message、NameServer 和 Broker 说明各组件职责、交互关系和开发注意事项。该章节对应你上传的大纲中的“RocketMQ 基础架构”部分。

RocketMQ 的基础消息模型是发布订阅模型。业务系统作为 Producer 将消息发送到 Topic，Consumer 订阅 Topic 后从 Broker 获取并处理消息。为了支持高并发和水平扩展，RocketMQ 会将 Topic 拆分为多个 MessageQueue；为了支持多实例消费，RocketMQ 引入 Consumer Group，由同一消费组内的多个消费者共同承担消费任务。([RocketMQ](https://rocketmq.apache.org/docs/4.x/introduction/02whatis/?utm_source=chatgpt.com))

RocketMQ 的核心架构可以理解为四类角色：Producer 负责发送消息，Consumer 负责消费消息，NameServer 负责维护路由信息，Broker 负责存储、投递和查询消息。Producer 和 Consumer 不直接依赖固定 Broker 地址，而是先从 NameServer 获取 Topic 路由，再与对应 Broker 建立连接。([RocketMQ](https://rocketmq.apache.org/docs/4.x/introduction/02whatis/?utm_source=chatgpt.com))

整体交互流程如下：

```text
业务系统 Producer
        |
        | 1. 从 NameServer 获取 Topic 路由
        v
NameServer 路由注册中心
        |
        | 2. 返回 Topic 对应的 Broker 和 MessageQueue 信息
        v
Producer 选择 MessageQueue 并发送消息
        |
        v
Broker 存储消息
        |
        | 3. Consumer 根据订阅关系消费消息
        v
业务系统 Consumer
```

### Producer 生产者

Producer 是消息生产者，通常集成在业务系统内部，负责将订单、支付、库存、通知、日志等业务数据封装成 RocketMQ 消息并发送到 Broker。Producer 本身不保存消息，主要职责是构造消息、选择发送方式、选择目标 Topic 和 MessageQueue，并处理发送结果。RocketMQ 官方文档将 Producer 定义为创建消息并发送到服务端的功能实体，通常集成在业务系统中。([RocketMQ](https://rocketmq.apache.org/docs/introduction/02concepts/?utm_source=chatgpt.com))

在 Spring Boot 项目中，Producer 一般不会直接使用原生 `DefaultMQProducer`，而是通过 `RocketMQTemplate` 或项目封装的消息发送组件发送消息。这样可以统一处理 Topic、Tag、Key、消息体序列化、发送日志和异常转换。

Producer 常见发送方式包括同步发送、异步发送、单向发送、顺序发送、延迟发送和事务消息发送。不同方式适用的业务场景不同：同步发送适合关注发送结果的核心业务；异步发送适合吞吐量要求较高但仍需要回调结果的业务；单向发送适合日志、埋点等不强依赖发送结果的场景；事务消息适合本地事务和消息发送需要最终一致的场景。RocketMQ 4.x 文档中也说明 Producer 支持同步、异步、顺序和单向等发送方式。([RocketMQ](https://rocketmq.apache.org/docs/4.x/introduction/02whatis/?utm_source=chatgpt.com))

Producer 开发时需要重点关注以下事项：

| 关注点         | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| Producer Group | 生产者组应按应用或业务域命名，避免多个无关业务共用同一个生产者组。 |
| Topic          | Producer 发送消息时必须指定 Topic，Topic 应表达清晰的业务域。 |
| Tag            | Tag 用于细分消息类型，便于消费者过滤。                       |
| Key            | Key 应使用订单号、支付单号、业务流水号等关键标识，便于排查和检索消息。 |
| 发送结果       | 关键业务必须判断发送结果，不建议忽略异常。                   |
| 超时与重试     | 需要结合业务容忍度设置发送超时和重试次数。                   |
| 日志           | 发送日志中至少记录 Topic、Tag、Key、业务 ID 和发送结果。     |

Producer 不应该承担复杂业务编排职责。推荐做法是业务服务先完成本地核心逻辑，再通过统一消息发送组件发布业务事件。消息发送组件只负责可靠发送、日志记录、异常包装和基础参数校验，不应耦合过多业务规则。

### Consumer 消费者

Consumer 是消息消费者，通常集成在下游业务系统中，负责订阅 Topic、拉取或接收消息，并将消息转换为业务逻辑可以处理的数据。RocketMQ 官方文档将 Consumer 定义为接收并处理消息的实体，通常集成在业务系统中，从 Broker 获取消息并转换为业务逻辑可处理的信息。([RocketMQ](https://rocketmq.apache.org/docs/introduction/02concepts/?utm_source=chatgpt.com))

Consumer 通常以 Consumer Group 为单位工作。同一个 Consumer Group 内的多个 Consumer 实例会共同消费订阅消息，常用于消费端水平扩展。不同 Consumer Group 之间互不影响，适合多个业务系统分别订阅同一个 Topic 的同一类事件。RocketMQ 文档说明 Consumer Group 是由具有相同消费行为的消费者组成的负载均衡分组。([RocketMQ](https://rocketmq.apache.org/docs/introduction/02concepts/?utm_source=chatgpt.com))

Consumer 的消费模式主要包括集群消费和广播消费。集群消费是常见模式，同一消费组内每条消息通常只会被其中一个消费者实例处理；广播消费则会让同一消费组内的每个消费者实例都处理一份消息。RocketMQ 4.x 架构文档说明 Consumer 支持集群模式和广播模式，其中集群模式是最常用模式。([RocketMQ](https://rocketmq.apache.org/docs/4.x/introduction/02whatis/?utm_source=chatgpt.com))

Consumer 开发时需要重点关注以下事项：

| 关注点         | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| Consumer Group | 同一组消费者应具备相同消费逻辑，不要把不同业务处理逻辑放在同一个消费组内。 |
| 订阅关系       | Consumer 需要明确订阅 Topic 和 Tag，避免无意义地订阅过多消息。 |
| 幂等处理       | 消息可能因重试、超时、异常等原因重复投递，消费端必须具备幂等能力。 |
| 异常处理       | 消费失败时应明确抛出异常或返回失败状态，交由 RocketMQ 重试机制处理。 |
| 消费耗时       | 单条消息处理时间不宜过长，耗时任务建议拆分或异步化。         |
| 日志记录       | 消费日志中应包含 Topic、Tag、Key、业务 ID、消费结果和异常信息。 |
| 死信处理       | 多次消费失败的消息需要进入死信处理流程，不能长期无人关注。   |

Consumer 不建议直接依赖消息体中的所有字段。更稳妥的方式是定义稳定的消息 DTO，只传递业务处理所需的关键字段。消费者收到消息后，再根据业务 ID 查询本地或上游系统的最新状态，避免因为消息体过大、字段变化或旧消息重放导致消费逻辑不稳定。

### Topic 主题

Topic 是 RocketMQ 中消息归类和路由的顶层容器，用于承载属于同一类业务语义的消息。RocketMQ 5.0 概念文档将 Topic 定义为用于传输和存储同一业务逻辑消息的顶层容器。([RocketMQ](https://rocketmq.apache.org/docs/introduction/02concepts/?utm_source=chatgpt.com))

Topic 的设计会直接影响后续消息治理、权限控制、监控告警、消费隔离和问题排查。一个好的 Topic 应该能从名称上判断消息所属业务域，例如订单事件、支付事件、库存事件、用户事件等。不要把多个无关业务混放在同一个 Topic 中，也不要为每个非常细小的动作都创建独立 Topic。

RocketMQ 5.0 对消息类型有更明确的管理能力，官方文档说明 RocketMQ 支持普通消息、FIFO 消息、事务消息和延迟消息等消息类型，并且从 5.0 开始支持消息类型校验，一个 Topic 只允许发送一种消息类型，以便生产系统运维管理并避免混乱。([RocketMQ](https://rocketmq.apache.org/docs/introduction/02concepts/?utm_source=chatgpt.com))

Topic 设计建议如下：

| 设计项   | 建议                                                         |
| -------- | ------------------------------------------------------------ |
| 命名语义 | 按业务域命名，例如 `order-event-topic`、`payment-event-topic`、`inventory-event-topic`。 |
| 消息类型 | 普通消息、顺序消息、事务消息、延迟消息建议使用不同 Topic。   |
| 业务边界 | 一个 Topic 不要承载多个无关业务域。                          |
| 环境隔离 | 开发、测试、预发、生产环境应使用不同环境或不同命名规则隔离。 |
| 权限管理 | 生产环境 Topic 创建和修改应由运维或平台统一管理，不建议应用自动创建。 |
| 监控告警 | 核心 Topic 应配置积压量、消费延迟、失败重试和死信告警。      |

Topic 与 Tag 的职责要清晰区分。Topic 表示业务域，Tag 表示业务域下的事件类型。例如订单业务可以使用 `order-event-topic` 作为 Topic，再通过 `created`、`paid`、`cancelled`、`completed` 等 Tag 区分具体事件。这样消费者既可以订阅整个订单事件，也可以只订阅关心的订单事件类型。

### Message 消息

Message 是 RocketMQ 中传输数据的最小单位。RocketMQ 5.0 概念文档说明，Message 是数据传输的最小单位，Producer 将业务数据和扩展属性封装为 Message 发送到 Broker，Broker 再按语义将消息投递给 Consumer。([RocketMQ](https://rocketmq.apache.org/docs/introduction/02concepts/?utm_source=chatgpt.com))

一条业务消息通常由 Topic、Tag、Key、消息体和属性组成。Topic 决定消息所属业务域，Tag 决定消息类型，Key 用于检索和排查，消息体保存业务数据，属性用于保存扩展元数据。RocketMQ 4.x Producer 文档中也说明 Message 包含 topic、body、properties 和 transactionId，并建议设置 Keys 便于通过 Topic 和 Keys 查询消息。([RocketMQ](https://rocketmq.apache.org/docs/4.x/producer/01concept1/?utm_source=chatgpt.com))

常见 Message 字段设计如下：

| 字段       | 建议内容                                                   |
| ---------- | ---------------------------------------------------------- |
| Topic      | 业务域级别的消息主题，例如订单事件 Topic。                 |
| Tag        | 当前消息的事件类型，例如订单创建、订单支付、订单取消。     |
| Key        | 业务唯一标识，例如订单号、支付单号、用户 ID 或业务流水号。 |
| Body       | 业务消息体，通常使用 JSON 序列化后的 DTO。                 |
| Properties | 扩展属性，例如来源系统、链路追踪 ID、租户 ID、业务版本号。 |

业务消息体不建议直接使用数据库实体类。实体类通常包含数据库字段、持久化注解和内部状态，不适合作为系统间通信协议。推荐单独定义消息 DTO，例如 `OrderCreatedMessage`、`PaymentSuccessMessage`、`InventoryDeductMessage`，只保留消费者需要的字段。

消息体设计建议如下：

| 设计项      | 建议                                                         |
| ----------- | ------------------------------------------------------------ |
| 字段稳定    | 已发布字段尽量不随意删除或改变含义。                         |
| 类型明确    | 金额使用整数分或 `BigDecimal`，时间使用标准格式。            |
| 保留业务 ID | 消息体中必须包含可用于幂等和查询的业务 ID。                  |
| 控制大小    | 消息体不应过大，大对象建议存储到数据库或对象存储，只在消息中传递引用 ID。 |
| 版本管理    | 复杂业务消息可以增加 `version` 字段，便于后续兼容。          |
| 链路追踪    | 建议携带 `traceId`，方便跨系统排查。                         |

在消费端，Message 不应被视为“数据库最终状态”。由于消息可能延迟、重试或乱序到达，消费者处理消息时需要结合业务状态判断是否可以执行。例如订单支付成功消息到达时，消费者应根据订单号查询当前订单状态，再决定是否执行后续逻辑，而不是无条件覆盖本地数据。

### NameServer

NameServer 是 RocketMQ 的路由注册中心，负责维护 Broker、Topic 和队列的路由信息。Producer 和 Consumer 通过 NameServer 获取 Topic 对应的 Broker 地址和队列信息，然后再直接与 Broker 通信。RocketMQ 4.x 架构文档说明 NameServer 是简单的 Topic 路由注册中心，支持 Broker 和 Topic 的动态注册与发现。([RocketMQ](https://rocketmq.apache.org/docs/4.x/introduction/02whatis/?utm_source=chatgpt.com))

NameServer 主要有两个职责：第一是 Broker 管理，接收 Broker 集群注册信息并通过心跳机制检查 Broker 是否存活；第二是路由信息管理，保存 Broker 集群和队列路由信息，供 Producer 和 Consumer 查询。([RocketMQ](https://rocketmq.apache.org/docs/4.x/introduction/02whatis/?utm_source=chatgpt.com))

NameServer 通常多实例部署，并且实例之间不进行信息同步。Broker 会向所有 NameServer 注册自己的路由信息，因此每个 NameServer 都保存完整路由信息。当某个 NameServer 下线时，客户端仍可以从其他 NameServer 获取路由信息。([RocketMQ](https://rocketmq.apache.org/docs/4.x/introduction/02whatis/?utm_source=chatgpt.com))

NameServer 开发和部署注意事项如下：

| 关注点     | 说明                                                        |
| ---------- | ----------------------------------------------------------- |
| 多实例部署 | 测试和生产环境建议部署多个 NameServer，提高路由发现可用性。 |
| 客户端配置 | Producer 和 Consumer 应配置多个 NameServer 地址。           |
| 无状态特性 | NameServer 本身不存储消息，故障恢复成本较低。               |
| 路由延迟   | Topic 或 Broker 变更后，客户端感知存在一定延迟。            |
| 网络连通性 | 应保证应用服务到 NameServer、Broker 的网络都可连通。        |

NameServer 不参与消息存储，也不参与消息投递。消息发送和消费的核心链路最终都会落到 Broker 上。开发人员排查问题时，需要区分是路由发现问题还是 Broker 存储投递问题：如果应用无法获取 Topic 路由，优先排查 NameServer 和 Topic 注册；如果可以获取路由但发送或消费失败，重点排查 Broker、Topic、队列、权限和客户端配置。

### Broker

Broker 是 RocketMQ 服务端的核心组件，负责消息存储、消息投递、消息查询和高可用能力。RocketMQ 4.x 架构文档说明 Broker 主要负责消息存储、投递和查询，并提供服务高可用保障。([RocketMQ](https://rocketmq.apache.org/docs/4.x/introduction/02whatis/?utm_source=chatgpt.com))

Broker 启动后会与 NameServer 建立长连接，并定期向 NameServer 注册 Topic 路由信息。Producer 发送消息时，最终会根据 Topic 路由将消息写入对应 Broker；Consumer 消费消息时，也会与承载该 Topic 的 Broker 建立连接并读取消息。([RocketMQ](https://rocketmq.apache.org/docs/4.x/introduction/02whatis/?utm_source=chatgpt.com))

在主从架构中，Broker 分为 Master 和 Slave。RocketMQ 文档说明，一个 Master 可以对应多个 Slave，一个 Slave 只能对应一个 Master；`brokerId` 为 `0` 表示 Master，非 `0` 表示 Slave。([RocketMQ](https://rocketmq.apache.org/docs/4.x/introduction/02whatis/?utm_source=chatgpt.com))

Broker 需要重点关注以下能力：

| 能力       | 说明                                       |
| ---------- | ------------------------------------------ |
| 消息写入   | 接收 Producer 发送的消息，并写入存储文件。 |
| 消息存储   | 负责消息持久化，支持后续消费、查询和重试。 |
| 消息投递   | 根据订阅关系向 Consumer 提供消息。         |
| 队列管理   | 管理 Topic 下的 MessageQueue。             |
| 消费进度   | 记录 Consumer Group 的消费位点。           |
| 重试与死信 | 管理消费失败后的重试消息和死信消息。       |
| 高可用     | 通过主从、复制和部署策略提升服务可用性。   |

Broker 部署和使用注意事项如下：

| 关注点     | 说明                                                         |
| ---------- | ------------------------------------------------------------ |
| 存储磁盘   | Broker 强依赖磁盘性能，生产环境应使用稳定、低延迟、高吞吐的磁盘。 |
| 内存配置   | Broker 需要足够内存支撑消息写入、索引、缓存和网络处理。      |
| 主从复制   | 生产环境应根据可靠性要求选择同步复制或异步复制策略。         |
| Topic 创建 | 生产环境不建议开启自动创建 Topic，避免误发消息生成错误 Topic。 |
| 消息保留   | 应根据业务排查和存储成本设置合理的消息保留时间。             |
| 监控告警   | 需要监控 Broker 存活、磁盘使用率、消息堆积、消费延迟和写入失败。 |

Broker 是 RocketMQ 故障排查的核心位置。发送失败、消费延迟、消息堆积、死信消息增多、磁盘空间不足、主从同步异常等问题，最终都需要结合 Broker 日志、Broker 监控指标和 Topic 队列状态进行定位。

本章中的基础架构可以归纳为一句话：Producer 负责生产消息，Consumer 负责消费消息，Topic 负责组织消息，Message 承载业务数据，NameServer 负责路由发现，Broker 负责消息存储与投递。后续 Spring Boot 集成、消息发送开发和消息消费开发，都会围绕这几个核心组件展开。

## Spring Boot 3 集成 RocketMQ

本节用于说明 Spring Boot 3 项目中集成 RocketMQ 的基础方式，内容包括 Maven 依赖、`application.yml` 配置、`RocketMQTemplate` 使用方式、生产者配置和消费者配置。本节对应原文档大纲中的“Spring Boot 3 集成 RocketMQ”章节。

RocketMQ Spring 项目用于帮助开发者快速将 RocketMQ 集成到 Spring Boot，并提供同步发送、异步发送、单向发送、顺序消息、批量消息、事务消息、延迟消息、集群/广播消费、顺序消费、Tag 或 SQL92 过滤等能力。本文档后续示例默认使用 `rocketmq-spring-boot-starter`、`RocketMQTemplate` 和 `@RocketMQMessageListener` 完成生产者与消费者开发。([GitHub](https://github.com/apache/rocketmq-spring?utm_source=chatgpt.com))

截至当前 Maven Central 信息，`org.apache.rocketmq:rocketmq-spring-boot-starter` 已发布到 `2.3.5`，因此 Spring Boot 3 项目建议优先使用 `2.3.x` 最新补丁版本。([Maven Repository](https://repo.maven.apache.org/maven2/org/apache/rocketmq/rocketmq-spring-boot-starter/?utm_source=chatgpt.com))

### Maven 依赖配置

本节给出 Spring Boot 3 集成 RocketMQ 的 Maven 基础依赖。实际项目中建议将版本统一放到父工程、`dependencyManagement` 或公司内部 BOM 中管理，避免不同业务模块使用不同版本。

文件位置：`pom.xml`

```xml
<properties>
    <!-- 项目统一使用 JDK 21 -->
    <java.version>21</java.version>

    <!-- RocketMQ Spring Boot Starter 当前建议使用 2.3.x 最新补丁版本 -->
    <rocketmq-spring.version>2.3.5</rocketmq-spring.version>

    <!-- Hutool 工具包版本，用于 JSON、字符串、ID、日期等常用处理 -->
    <hutool.version>5.8.36</hutool.version>
</properties>

<dependencies>
    <!-- Spring Boot Web：用于提供测试接口和基础 Web 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- RocketMQ Spring Boot Starter：集成 RocketMQTemplate、监听器和自动配置 -->
    <dependency>
        <groupId>org.apache.rocketmq</groupId>
        <artifactId>rocketmq-spring-boot-starter</artifactId>
        <version>${rocketmq-spring.version}</version>
    </dependency>

    <!-- Hutool：用于 JSON 序列化、字符串校验、ID 生成等工具处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok：减少日志对象、构造方法、Getter、Setter 等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot Test：用于单元测试和集成测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目使用 Spring Boot 父工程，可以由 `spring-boot-starter-parent` 管理 Spring 相关依赖版本，只需要单独声明 RocketMQ Spring 和 Hutool 的版本。

文件位置：`pom.xml`

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <!-- Spring Boot 版本以公司统一版本为准，这里使用 3.5.x 作为示例 -->
    <version>3.5.13</version>
    <relativePath/>
</parent>
```

如果公司内部已经统一了 Spring Boot BOM，则业务服务不要重复声明 Spring Boot 版本，只保留 RocketMQ 相关依赖即可。

### application.yml 配置

本节给出 RocketMQ 的基础配置。Spring Boot 项目启动后，`rocketmq-spring-boot-starter` 会读取 `rocketmq` 前缀下的配置，并自动创建生产者相关 Bean；消费者则主要通过 `@RocketMQMessageListener` 声明消费组、Topic、Tag 和消费模式。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 当前服务端口
  port: 8088

spring:
  application:
    # 当前应用名称，建议与服务注册名称保持一致
    name: rocketmq-demo-service

rocketmq:
  # RocketMQ NameServer 地址；多个地址通常使用英文分号分隔
  name-server: 127.0.0.1:9876

  producer:
    # 生产者组名称，建议按应用名或业务域命名
    group: rocketmq-demo-producer-group

    # 消息发送超时时间，单位毫秒
    send-message-timeout: 3000

    # 同步发送失败后的重试次数
    retry-times-when-send-failed: 2

    # 异步发送失败后的重试次数
    retry-times-when-send-async-failed: 2

    # 是否在发送失败后尝试其他 Broker
    retry-next-server: true

    # 消息体超过该阈值后进行压缩，单位字节
    compress-message-body-threshold: 4096

    # 单条消息最大大小，单位字节，默认通常为 4MB
    max-message-size: 4194304

    # 是否开启消息轨迹，生产环境可按平台规范开启
    enable-msg-trace: true

    # 自定义消息轨迹 Topic，不配置时使用默认轨迹 Topic
    customized-trace-topic: RMQ_SYS_TRACE_TOPIC
```

多环境项目建议按环境拆分配置，例如 `application-dev.yml`、`application-test.yml`、`application-prod.yml`。开发环境可以连接本地 RocketMQ，测试和生产环境应连接对应集群。

文件位置：`src/main/resources/application-dev.yml`

```yaml
rocketmq:
  # 开发环境 RocketMQ NameServer
  name-server: 127.0.0.1:9876

  producer:
    # 开发环境生产者组
    group: rocketmq-demo-dev-producer-group
```

文件位置：`src/main/resources/application-prod.yml`

```yaml
rocketmq:
  # 生产环境 RocketMQ NameServer，示例地址需替换为真实集群地址
  name-server: 10.10.1.11:9876;10.10.1.12:9876

  producer:
    # 生产环境生产者组
    group: rocketmq-demo-prod-producer-group

    # 生产环境建议保留发送超时配置，避免外部调用长时间阻塞
    send-message-timeout: 3000

    # 生产环境建议开启消息轨迹，便于排查消息链路
    enable-msg-trace: true
```

如果生产环境开启 ACL 鉴权，需要增加 `access-key` 和 `secret-key`。RocketMQ Spring Wiki 中说明，生产者和消费者都可以配置 AK/SK，消费者也可以通过 `@RocketMQMessageListener` 单独配置。([GitHub](https://github.com/apache/rocketmq-spring/wiki/Authentication-and-authorisation?utm_source=chatgpt.com))

文件位置：`src/main/resources/application-prod.yml`

```yaml
rocketmq:
  name-server: 10.10.1.11:9876;10.10.1.12:9876

  producer:
    # 生产者组名称
    group: rocketmq-demo-prod-producer-group

    # RocketMQ ACL AccessKey，建议从环境变量或配置中心注入
    access-key: ${ROCKETMQ_ACCESS_KEY}

    # RocketMQ ACL SecretKey，建议从环境变量或配置中心注入
    secret-key: ${ROCKETMQ_SECRET_KEY}

  consumer:
    # 消费者默认 AccessKey，具体消费者也可以在注解中单独指定
    access-key: ${ROCKETMQ_ACCESS_KEY}

    # 消费者默认 SecretKey，具体消费者也可以在注解中单独指定
    secret-key: ${ROCKETMQ_SECRET_KEY}
```

生产环境不建议将密钥直接写死在配置文件中，应通过环境变量、Kubernetes Secret、配置中心或密钥管理系统注入。

### RocketMQTemplate 使用方式

本节说明 `RocketMQTemplate` 的基础使用方式。`RocketMQTemplate` 是 Spring Boot 项目中发送 RocketMQ 消息的常用入口，适合封装到统一消息发送组件中，避免业务代码到处直接拼接 Topic、Tag、Key 和消息体。

示例文件结构如下：

```text
src/main/java/io/github/atengk/rocketmq/
├── constant/
│   └── RocketMqConstant.java
├── model/
│   └── OrderPaidMessage.java
├── producer/
│   └── OrderMessageProducer.java
└── consumer/
    └── OrderPaidMessageConsumer.java
```

先定义 Topic、Tag 和 Consumer Group 常量，避免在业务代码中硬编码字符串。

文件位置：`src/main/java/io/github/atengk/rocketmq/constant/RocketMqConstant.java`

```java
package io.github.atengk.rocketmq.constant;

/**
 * RocketMQ 常量定义
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class RocketMqConstant {

    /**
     * 订单事件 Topic
     */
    public static final String ORDER_EVENT_TOPIC = "order-event-topic";

    /**
     * 订单已支付 Tag
     */
    public static final String ORDER_PAID_TAG = "order_paid";

    /**
     * 订单消费者组
     */
    public static final String ORDER_CONSUMER_GROUP = "order-paid-consumer-group";

    private RocketMqConstant() {
    }

    /**
     * 构建 RocketMQ 发送目标，格式为 topic:tag
     *
     * @param topic Topic 名称
     * @param tag   Tag 名称
     * @return RocketMQ 发送目标
     */
    public static String destination(String topic, String tag) {
        return topic + ":" + tag;
    }
}
```

定义消息体 DTO。消息体不要直接使用数据库实体类，应单独定义面向消息通信的模型。

文件位置：`src/main/java/io/github/atengk/rocketmq/model/OrderPaidMessage.java`

```java
package io.github.atengk.rocketmq.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单已支付消息
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderPaidMessage(
        String messageId,
        String orderNo,
        String userId,
        BigDecimal payAmount,
        LocalDateTime paidTime,
        String traceId
) {
}
```

下面的生产者封装了同步发送和异步发送两个常用方法。发送前使用 Hutool 做参数校验和 JSON 序列化，发送时设置消息 Key，便于后续按业务单号排查消息。

文件位置：`src/main/java/io/github/atengk/rocketmq/producer/OrderMessageProducer.java`

```java
package io.github.atengk.rocketmq.producer;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.rocketmq.constant.RocketMqConstant;
import io.github.atengk.rocketmq.model.OrderPaidMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 订单消息生产者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageProducer {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 同步发送订单已支付消息
     *
     * @param message 订单已支付消息
     * @return 发送结果
     */
    public SendResult sendOrderPaidMessage(OrderPaidMessage message) {
        checkOrderPaidMessage(message);

        String destination = RocketMqConstant.destination(
                RocketMqConstant.ORDER_EVENT_TOPIC,
                RocketMqConstant.ORDER_PAID_TAG
        );

        Message<String> rocketMessage = buildMessage(message);
        SendResult sendResult = rocketMQTemplate.syncSend(destination, rocketMessage);

        log.info("订单已支付消息发送成功，topic={}，tag={}，orderNo={}，messageId={}，sendStatus={}",
                RocketMqConstant.ORDER_EVENT_TOPIC,
                RocketMqConstant.ORDER_PAID_TAG,
                message.orderNo(),
                message.messageId(),
                sendResult.getSendStatus());

        return sendResult;
    }

    /**
     * 异步发送订单已支付消息
     *
     * @param message 订单已支付消息
     */
    public void asyncSendOrderPaidMessage(OrderPaidMessage message) {
        checkOrderPaidMessage(message);

        String destination = RocketMqConstant.destination(
                RocketMqConstant.ORDER_EVENT_TOPIC,
                RocketMqConstant.ORDER_PAID_TAG
        );

        Message<String> rocketMessage = buildMessage(message);
        rocketMQTemplate.asyncSend(destination, rocketMessage, new SendCallback() {

            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("订单已支付消息异步发送成功，topic={}，tag={}，orderNo={}，messageId={}，sendStatus={}",
                        RocketMqConstant.ORDER_EVENT_TOPIC,
                        RocketMqConstant.ORDER_PAID_TAG,
                        message.orderNo(),
                        message.messageId(),
                        sendResult.getSendStatus());
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("订单已支付消息异步发送失败，topic={}，tag={}，orderNo={}，messageId={}",
                        RocketMqConstant.ORDER_EVENT_TOPIC,
                        RocketMqConstant.ORDER_PAID_TAG,
                        message.orderNo(),
                        message.messageId(),
                        throwable);
            }
        });
    }

    /**
     * 构建 Spring Message
     *
     * @param message 订单已支付消息
     * @return Spring Message
     */
    private Message<String> buildMessage(OrderPaidMessage message) {
        String messageId = StrUtil.blankToDefault(message.messageId(), IdUtil.fastSimpleUUID());
        String body = JSONUtil.toJsonStr(message);

        return MessageBuilder.withPayload(body)
                // RocketMQ 消息 Key，建议使用业务唯一标识，便于检索和排查
                .setHeader(RocketMQHeaders.KEYS, message.orderNo())
                // 自定义消息 ID，便于业务日志串联
                .setHeader("messageId", messageId)
                // 链路追踪 ID，便于跨系统排查
                .setHeader("traceId", message.traceId())
                .build();
    }

    /**
     * 校验订单已支付消息
     *
     * @param message 订单已支付消息
     */
    private void checkOrderPaidMessage(OrderPaidMessage message) {
        Assert.notNull(message, "订单已支付消息不能为空");
        Assert.notBlank(message.orderNo(), "订单号不能为空");
        Assert.notBlank(message.userId(), "用户ID不能为空");
        Assert.notNull(message.payAmount(), "支付金额不能为空");
        Assert.notNull(message.paidTime(), "支付时间不能为空");
    }
}
```

`RocketMQTemplate` 发送目标通常使用 `topic:tag` 格式。例如订单已支付消息可以发送到 `order-event-topic:order_paid`。如果不需要 Tag，也可以只传入 Topic，但生产项目建议明确区分 Tag，便于消费者过滤和问题排查。

### 生产者配置

本节说明生产者的配置和开发约束。生产者配置的核心目标是保证消息可以稳定发送，并且在发送失败时有清晰的日志、重试和排查入口。

生产者基础配置如下：

文件位置：`src/main/resources/application.yml`

```yaml
rocketmq:
  name-server: 127.0.0.1:9876

  producer:
    # 生产者组名称，同一应用内建议保持统一
    group: rocketmq-demo-producer-group

    # 发送超时时间，单位毫秒；核心链路不建议设置过长
    send-message-timeout: 3000

    # 同步发送失败后的重试次数
    retry-times-when-send-failed: 2

    # 异步发送失败后的重试次数
    retry-times-when-send-async-failed: 2

    # 发送失败后是否尝试其他 Broker
    retry-next-server: true

    # 是否开启消息轨迹
    enable-msg-trace: true
```

生产者开发建议如下：

| 配置或设计项     | 建议                                                         |
| ---------------- | ------------------------------------------------------------ |
| `producer.group` | 按应用或业务域命名，例如 `order-service-producer-group`。    |
| 发送超时         | 常规业务建议 3 秒左右起步，核心链路按接口 SLA 调整。         |
| 发送重试         | 关键消息建议开启重试，但不能依赖无限重试解决业务一致性问题。 |
| Message Key      | 必须设置业务 Key，例如订单号、支付单号、流水号。             |
| 日志记录         | 发送成功和失败都应记录 Topic、Tag、Key、业务 ID。            |
| 发送封装         | 不建议业务代码直接到处调用 `RocketMQTemplate`，应封装统一生产者组件。 |
| 消息体           | 不建议直接发送数据库实体类，应发送稳定 DTO。                 |

下面是一个简单的接口示例，用于验证生产者是否可以正常发送消息。该接口只用于本地开发或测试环境，生产环境不建议暴露这种测试接口。

文件位置：`src/main/java/io/github/atengk/rocketmq/controller/OrderMessageTestController.java`

```java
package io.github.atengk.rocketmq.controller;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.rocketmq.model.OrderPaidMessage;
import io.github.atengk.rocketmq.producer.OrderMessageProducer;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.client.producer.SendResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单消息测试接口
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequiredArgsConstructor
public class OrderMessageTestController {

    private final OrderMessageProducer orderMessageProducer;

    /**
     * 同步发送订单已支付测试消息
     *
     * @return 发送结果
     */
    @GetMapping("/test/rocketmq/order-paid/sync")
    public String sendSyncOrderPaidMessage() {
        OrderPaidMessage message = new OrderPaidMessage(
                IdUtil.fastSimpleUUID(),
                "ORDER" + System.currentTimeMillis(),
                "USER10001",
                new BigDecimal("99.90"),
                LocalDateTime.now(),
                IdUtil.fastSimpleUUID()
        );

        SendResult sendResult = orderMessageProducer.sendOrderPaidMessage(message);
        return "发送成功：" + sendResult.getSendStatus();
    }

    /**
     * 异步发送订单已支付测试消息
     *
     * @return 操作结果
     */
    @GetMapping("/test/rocketmq/order-paid/async")
    public String sendAsyncOrderPaidMessage() {
        OrderPaidMessage message = new OrderPaidMessage(
                IdUtil.fastSimpleUUID(),
                "ORDER" + System.currentTimeMillis(),
                "USER10001",
                new BigDecimal("199.90"),
                LocalDateTime.now(),
                IdUtil.fastSimpleUUID()
        );

        orderMessageProducer.asyncSendOrderPaidMessage(message);
        return "异步消息已提交";
    }
}
```

本地启动服务后，可以通过以下命令验证生产者发送能力。

```bash
# 同步发送订单已支付消息
curl "http://127.0.0.1:8088/test/rocketmq/order-paid/sync"

# 异步发送订单已支付消息
curl "http://127.0.0.1:8088/test/rocketmq/order-paid/async"
```

命令说明：第一个接口会等待 RocketMQ 返回发送结果，适合验证同步发送链路；第二个接口提交异步发送请求后立即返回，实际发送结果需要查看应用日志中的异步回调日志。

### 消费者配置

本节说明消费者的配置方式。RocketMQ Spring 消费者通常通过 `@RocketMQMessageListener` 声明 Topic、Consumer Group、Tag 过滤条件、消费模式和消息模型，再通过实现 `RocketMQListener<T>` 处理消息。

下面的消费者监听 `order-event-topic` 下的 `order_paid` Tag，并使用集群消费模式。集群消费模式下，同一 Consumer Group 内的多个消费者实例共同消费消息，通常是生产环境最常用的方式。

文件位置：`src/main/java/io/github/atengk/rocketmq/consumer/OrderPaidMessageConsumer.java`

```java
package io.github.atengk.rocketmq.consumer;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.rocketmq.constant.RocketMqConstant;
import io.github.atengk.rocketmq.model.OrderPaidMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 订单已支付消息消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = RocketMqConstant.ORDER_EVENT_TOPIC,
        consumerGroup = RocketMqConstant.ORDER_CONSUMER_GROUP,
        selectorExpression = RocketMqConstant.ORDER_PAID_TAG,
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.CLUSTERING
)
public class OrderPaidMessageConsumer implements RocketMQListener<String> {

    /**
     * 消费订单已支付消息
     *
     * @param messageBody 消息体 JSON 字符串
     */
    @Override
    public void onMessage(String messageBody) {
        if (StrUtil.isBlank(messageBody)) {
            log.warn("订单已支付消息为空，跳过消费");
            return;
        }

        OrderPaidMessage message = JSONUtil.toBean(messageBody, OrderPaidMessage.class);
        log.info("开始消费订单已支付消息，orderNo={}，userId={}，payAmount={}，traceId={}",
                message.orderNo(),
                message.userId(),
                message.payAmount(),
                message.traceId());

        // 这里编写真实业务逻辑，例如更新订单状态、生成流水、发送通知等
        // 消费端必须做幂等控制，避免消息重复投递导致重复处理

        log.info("订单已支付消息消费完成，orderNo={}，messageId={}",
                message.orderNo(),
                message.messageId());
    }
}
```

消费者常用注解参数说明如下：

| 参数                 | 说明                                                         |
| -------------------- | ------------------------------------------------------------ |
| `topic`              | 当前消费者订阅的 Topic。                                     |
| `consumerGroup`      | 当前消费者所属消费组，同一消费组内应保持相同消费逻辑。       |
| `selectorExpression` | Tag 过滤表达式，例如 `order_paid` 或 `tagA                   |
| `consumeMode`        | 消费模式，常用值为并发消费 `CONCURRENTLY` 和顺序消费 `ORDERLY`。 |
| `messageModel`       | 消息模型，常用值为集群消费 `CLUSTERING` 和广播消费 `BROADCASTING`。 |

如果需要广播消费，可以将 `messageModel` 改为 `BROADCASTING`。广播消费会让同一消费组内的每个实例都消费一份消息，适合本地缓存刷新、配置同步等场景，不适合订单扣减、支付回调、发券等只能处理一次的业务。

文件位置：`src/main/java/io/github/atengk/rocketmq/consumer/OrderPaidBroadcastConsumer.java`

```java
package io.github.atengk.rocketmq.consumer;

import io.github.atengk.rocketmq.constant.RocketMqConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 订单已支付广播消费示例
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = RocketMqConstant.ORDER_EVENT_TOPIC,
        consumerGroup = "order-paid-broadcast-consumer-group",
        selectorExpression = RocketMqConstant.ORDER_PAID_TAG,
        messageModel = MessageModel.BROADCASTING
)
public class OrderPaidBroadcastConsumer implements RocketMQListener<String> {

    /**
     * 广播消费订单已支付消息
     *
     * @param messageBody 消息体 JSON 字符串
     */
    @Override
    public void onMessage(String messageBody) {
        log.info("广播消费订单已支付消息，messageBody={}", messageBody);
    }
}
```

消费者开发建议如下：

| 配置或设计项   | 建议                                                         |
| -------------- | ------------------------------------------------------------ |
| Consumer Group | 同一消费组内必须保持一致的消费逻辑，不要多个不同业务共用同一个消费组。 |
| Topic 与 Tag   | 消费者应只订阅自己关心的 Topic 和 Tag，减少无效消息过滤。    |
| 幂等处理       | 消费逻辑必须支持重复消费，建议基于业务唯一键做幂等表或状态判断。 |
| 异常处理       | 消费失败时不要吞异常，应记录日志并让 RocketMQ 触发重试。     |
| 消费耗时       | 不建议在消费者中执行长时间阻塞任务，必要时拆分为异步任务。   |
| 日志字段       | 至少记录 Topic、Tag、Key、业务 ID、消息 ID、消费结果。       |
| 死信处理       | 多次失败后的消息需要有补偿流程，不能只依赖自动重试。         |

消费者验证方式如下：

```bash
# 启动 Spring Boot 服务
mvn spring-boot:run

# 发送一条同步测试消息
curl "http://127.0.0.1:8088/test/rocketmq/order-paid/sync"

# 查看应用日志，确认消费者收到消息
tail -f logs/rocketmq-demo-service.log
```

如果没有收到消息，优先检查以下内容：

| 检查项                  | 说明                                                      |
| ----------------------- | --------------------------------------------------------- |
| NameServer 地址         | `rocketmq.name-server` 是否可以从应用所在机器访问。       |
| Topic 是否存在          | 本地开发可开启自动创建 Topic，生产环境应提前创建。        |
| Tag 是否匹配            | 生产者发送的 Tag 与消费者 `selectorExpression` 是否一致。 |
| Consumer Group 是否唯一 | 不同消费逻辑不要复用同一个 Consumer Group。               |
| Broker 日志             | Broker 是否正常启动，是否存在 Topic 路由或权限错误。      |
| 应用日志                | 消费者启动时是否成功注册监听器，消费时是否抛出异常。      |

本章集成完成后，项目已经具备 RocketMQ 的基础发送与消费能力。后续“消息发送开发”和“消息消费开发”章节可以在此基础上继续展开同步消息、异步消息、单向消息、延迟消息、顺序消息、事务消息、消费重试和消费幂等。

## 消息发送开发

本节用于说明 Spring Boot 3 项目中常见的 RocketMQ 消息发送方式，包括同步消息、异步消息、单向消息、延迟消息、顺序消息和事务消息。该章节对应原文档大纲中的“消息发送开发”部分。

RocketMQ Spring 支持同步发送、异步发送、单向发送、顺序消息、批量消息、事务消息和延迟级别消息等能力，Spring Boot 项目中通常通过 `RocketMQTemplate` 作为统一发送入口。([GitHub](https://github.com/apache/rocketmq-spring?utm_source=chatgpt.com))

本章示例默认已经完成前面章节中的 Maven 依赖和 `application.yml` 配置，核心依赖为 `rocketmq-spring-boot-starter`，基础配置中已经声明 `rocketmq.name-server` 和 `rocketmq.producer.group`。

示例文件结构如下：

```text
src/main/java/io/github/atengk/rocketmq/
├── constant/
│   └── RocketMqSendConstant.java
├── model/
│   └── OrderEventMessage.java
├── producer/
│   └── OrderMessageSendProducer.java
├── transaction/
│   └── OrderTransactionMessageListener.java
└── controller/
    └── OrderMessageSendTestController.java
```

先定义本章使用的 Topic、Tag 和发送目标常量。

文件位置：`src/main/java/io/github/atengk/rocketmq/constant/RocketMqSendConstant.java`

```java
package io.github.atengk.rocketmq.constant;

/**
 * RocketMQ 消息发送常量
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class RocketMqSendConstant {

    /**
     * 订单事件 Topic
     */
    public static final String ORDER_EVENT_TOPIC = "order-event-topic";

    /**
     * 订单创建 Tag
     */
    public static final String ORDER_CREATED_TAG = "order_created";

    /**
     * 订单支付 Tag
     */
    public static final String ORDER_PAID_TAG = "order_paid";

    /**
     * 订单取消 Tag
     */
    public static final String ORDER_CANCEL_TAG = "order_cancel";

    /**
     * 订单状态变更 Tag
     */
    public static final String ORDER_STATUS_CHANGE_TAG = "order_status_change";

    /**
     * 订单事务消息 Tag
     */
    public static final String ORDER_TRANSACTION_TAG = "order_transaction";

    private RocketMqSendConstant() {
    }

    /**
     * 构建 RocketMQ 发送目标
     *
     * @param topic Topic 名称
     * @param tag   Tag 名称
     * @return 发送目标，格式为 topic:tag
     */
    public static String destination(String topic, String tag) {
        return topic + ":" + tag;
    }
}
```

定义订单事件消息体。实际项目中不要直接发送数据库实体类，应使用稳定的消息 DTO。

文件位置：`src/main/java/io/github/atengk/rocketmq/model/OrderEventMessage.java`

```java
package io.github.atengk.rocketmq.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单事件消息
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderEventMessage(
        String messageId,
        String orderNo,
        String userId,
        String eventType,
        BigDecimal amount,
        LocalDateTime eventTime,
        String traceId
) {
}
```

### 同步消息发送

同步消息发送是指生产者发送消息后，会等待 RocketMQ Broker 返回发送结果。RocketMQ 官方文档说明，同步发送会在收到服务端响应后再继续发送下一条消息，适合重要通知、短信通知、核心业务事件等对发送结果有明确要求的场景。([RocketMQ](https://rocketmq.apache.org/docs/4.x/producer/02message1/?utm_source=chatgpt.com))

同步发送适合以下场景：

| 场景         | 说明                                              |
| ------------ | ------------------------------------------------- |
| 订单创建事件 | 订单主流程完成后，需要确认消息已经提交到 Broker。 |
| 支付成功事件 | 支付结果属于核心业务事件，不能随意忽略发送结果。  |
| 库存扣减事件 | 需要明确知道消息是否发送成功，便于失败补偿。      |
| 重要通知事件 | 发送失败需要记录日志、告警或进入补偿流程。        |

下面的生产者类统一封装同步、异步、单向、延迟、顺序和事务消息发送方法。同步发送方法使用 `rocketMQTemplate.syncSend`，并返回 `SendResult`。

文件位置：`src/main/java/io/github/atengk/rocketmq/producer/OrderMessageSendProducer.java`

```java
package io.github.atengk.rocketmq.producer;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.rocketmq.constant.RocketMqSendConstant;
import io.github.atengk.rocketmq.model.OrderEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 订单消息发送生产者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageSendProducer {

    private static final long DEFAULT_SEND_TIMEOUT = 3000L;

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 同步发送订单创建消息
     *
     * @param message 订单事件消息
     * @return 发送结果
     */
    public SendResult syncSendOrderCreated(OrderEventMessage message) {
        checkMessage(message);

        String destination = RocketMqSendConstant.destination(
                RocketMqSendConstant.ORDER_EVENT_TOPIC,
                RocketMqSendConstant.ORDER_CREATED_TAG
        );

        Message<String> rocketMessage = buildMessage(message);
        SendResult sendResult = rocketMQTemplate.syncSend(destination, rocketMessage, DEFAULT_SEND_TIMEOUT);

        log.info("同步发送订单创建消息成功，topic={}，tag={}，orderNo={}，messageId={}，sendStatus={}",
                RocketMqSendConstant.ORDER_EVENT_TOPIC,
                RocketMqSendConstant.ORDER_CREATED_TAG,
                message.orderNo(),
                message.messageId(),
                sendResult.getSendStatus());

        return sendResult;
    }

    /**
     * 异步发送订单支付消息
     *
     * @param message 订单事件消息
     */
    public void asyncSendOrderPaid(OrderEventMessage message) {
        checkMessage(message);

        String destination = RocketMqSendConstant.destination(
                RocketMqSendConstant.ORDER_EVENT_TOPIC,
                RocketMqSendConstant.ORDER_PAID_TAG
        );

        Message<String> rocketMessage = buildMessage(message);
        rocketMQTemplate.asyncSend(destination, rocketMessage, new SendCallback() {

            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("异步发送订单支付消息成功，topic={}，tag={}，orderNo={}，messageId={}，sendStatus={}",
                        RocketMqSendConstant.ORDER_EVENT_TOPIC,
                        RocketMqSendConstant.ORDER_PAID_TAG,
                        message.orderNo(),
                        message.messageId(),
                        sendResult.getSendStatus());
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("异步发送订单支付消息失败，topic={}，tag={}，orderNo={}，messageId={}",
                        RocketMqSendConstant.ORDER_EVENT_TOPIC,
                        RocketMqSendConstant.ORDER_PAID_TAG,
                        message.orderNo(),
                        message.messageId(),
                        throwable);
            }
        }, DEFAULT_SEND_TIMEOUT);
    }

    /**
     * 单向发送订单操作日志消息
     *
     * @param message 订单事件消息
     */
    public void sendOneWayOrderEvent(OrderEventMessage message) {
        checkMessage(message);

        String destination = RocketMqSendConstant.destination(
                RocketMqSendConstant.ORDER_EVENT_TOPIC,
                RocketMqSendConstant.ORDER_STATUS_CHANGE_TAG
        );

        Message<String> rocketMessage = buildMessage(message);
        rocketMQTemplate.sendOneWay(destination, rocketMessage);

        log.info("单向发送订单事件消息完成，topic={}，tag={}，orderNo={}，messageId={}",
                RocketMqSendConstant.ORDER_EVENT_TOPIC,
                RocketMqSendConstant.ORDER_STATUS_CHANGE_TAG,
                message.orderNo(),
                message.messageId());
    }

    /**
     * 发送订单延迟取消消息
     *
     * @param message    订单事件消息
     * @param delayLevel 延迟级别，实际延迟时间以 Broker 的 messageDelayLevel 配置为准
     * @return 发送结果
     */
    public SendResult syncSendDelayOrderCancel(OrderEventMessage message, int delayLevel) {
        checkMessage(message);
        Assert.isTrue(delayLevel > 0, "延迟级别必须大于0");

        String destination = RocketMqSendConstant.destination(
                RocketMqSendConstant.ORDER_EVENT_TOPIC,
                RocketMqSendConstant.ORDER_CANCEL_TAG
        );

        Message<String> rocketMessage = buildMessage(message);
        SendResult sendResult = rocketMQTemplate.syncSend(destination, rocketMessage, DEFAULT_SEND_TIMEOUT, delayLevel);

        log.info("发送订单延迟取消消息成功，topic={}，tag={}，orderNo={}，messageId={}，delayLevel={}，sendStatus={}",
                RocketMqSendConstant.ORDER_EVENT_TOPIC,
                RocketMqSendConstant.ORDER_CANCEL_TAG,
                message.orderNo(),
                message.messageId(),
                delayLevel,
                sendResult.getSendStatus());

        return sendResult;
    }

    /**
     * 顺序发送订单状态变更消息
     *
     * @param message 订单事件消息
     * @return 发送结果
     */
    public SendResult syncSendOrderlyStatusChange(OrderEventMessage message) {
        checkMessage(message);

        String destination = RocketMqSendConstant.destination(
                RocketMqSendConstant.ORDER_EVENT_TOPIC,
                RocketMqSendConstant.ORDER_STATUS_CHANGE_TAG
        );

        Message<String> rocketMessage = buildMessage(message);

        // 使用订单号作为 hashKey，保证同一订单的状态变更消息路由到同一个队列
        SendResult sendResult = rocketMQTemplate.syncSendOrderly(
                destination,
                rocketMessage,
                message.orderNo(),
                DEFAULT_SEND_TIMEOUT
        );

        log.info("顺序发送订单状态变更消息成功，topic={}，tag={}，orderNo={}，eventType={}，messageId={}，sendStatus={}",
                RocketMqSendConstant.ORDER_EVENT_TOPIC,
                RocketMqSendConstant.ORDER_STATUS_CHANGE_TAG,
                message.orderNo(),
                message.eventType(),
                message.messageId(),
                sendResult.getSendStatus());

        return sendResult;
    }

    /**
     * 发送订单事务消息
     *
     * @param message 订单事件消息
     * @return 事务发送结果
     */
    public TransactionSendResult sendOrderTransactionMessage(OrderEventMessage message) {
        checkMessage(message);

        String destination = RocketMqSendConstant.destination(
                RocketMqSendConstant.ORDER_EVENT_TOPIC,
                RocketMqSendConstant.ORDER_TRANSACTION_TAG
        );

        Message<String> rocketMessage = buildMessage(message);
        TransactionSendResult sendResult = rocketMQTemplate.sendMessageInTransaction(
                destination,
                rocketMessage,
                message.orderNo()
        );

        log.info("发送订单事务消息完成，topic={}，tag={}，orderNo={}，messageId={}，localTransactionState={}",
                RocketMqSendConstant.ORDER_EVENT_TOPIC,
                RocketMqSendConstant.ORDER_TRANSACTION_TAG,
                message.orderNo(),
                message.messageId(),
                sendResult.getLocalTransactionState());

        return sendResult;
    }

    /**
     * 构建 RocketMQ 消息
     *
     * @param message 订单事件消息
     * @return Spring Message
     */
    private Message<String> buildMessage(OrderEventMessage message) {
        String messageId = StrUtil.blankToDefault(message.messageId(), IdUtil.fastSimpleUUID());
        String body = JSONUtil.toJsonStr(message);

        return MessageBuilder.withPayload(body)
                // RocketMQ 消息 Key，建议使用业务唯一标识
                .setHeader(RocketMQHeaders.KEYS, message.orderNo())
                // 业务消息 ID，便于系统内日志串联
                .setHeader("messageId", messageId)
                // 订单号，事务回查和业务排查时使用
                .setHeader("orderNo", message.orderNo())
                // 事件类型，便于统一消费端区分业务动作
                .setHeader("eventType", message.eventType())
                // 链路追踪 ID，便于跨系统排查
                .setHeader("traceId", message.traceId())
                .build();
    }

    /**
     * 校验订单事件消息
     *
     * @param message 订单事件消息
     */
    private void checkMessage(OrderEventMessage message) {
        Assert.notNull(message, "订单事件消息不能为空");
        Assert.notBlank(message.orderNo(), "订单号不能为空");
        Assert.notBlank(message.userId(), "用户ID不能为空");
        Assert.notBlank(message.eventType(), "事件类型不能为空");
        Assert.notNull(message.eventTime(), "事件时间不能为空");
    }
}
```

同步发送的核心关注点是发送结果。业务代码不能只调用发送方法而不处理异常，核心消息发送失败后应结合业务场景进入重试、补偿或告警流程。

### 异步消息发送

异步消息发送是指生产者发送消息后不阻塞当前线程等待结果，而是通过 `SendCallback` 接收发送成功或失败的回调。异步发送适合吞吐量要求较高、业务流程不希望被消息发送长时间阻塞，但仍然需要关注发送结果的场景。

异步发送适合以下场景：

| 场景           | 说明                                       |
| -------------- | ------------------------------------------ |
| 用户行为事件   | 不阻塞主流程，但发送失败需要记录日志。     |
| 支付后通知事件 | 主流程可以快速返回，发送结果通过回调记录。 |
| 批量业务事件   | 大量消息发送时减少同步等待带来的耗时。     |
| 非强同步链路   | 允许消息发送结果异步回调处理。             |

本章中的 `asyncSendOrderPaid` 方法已经封装异步发送逻辑。需要注意，异步发送不是“不关心结果”，而是“结果通过回调处理”。如果消息属于核心业务事件，`onException` 中不能只打印日志后结束，应结合业务补偿机制落库或告警。

关键代码如下：

```java
rocketMQTemplate.asyncSend(destination, rocketMessage, new SendCallback() {

    @Override
    public void onSuccess(SendResult sendResult) {
        log.info("异步消息发送成功，sendStatus={}", sendResult.getSendStatus());
    }

    @Override
    public void onException(Throwable throwable) {
        log.error("异步消息发送失败", throwable);
    }
}, 3000L);
```

异步发送常见问题是应用进程提前关闭，导致异步回调还没有执行完成。对于命令行任务、短生命周期任务或测试代码，不建议随意使用异步发送；如果确实需要使用，应确保进程生命周期足够长，或者在任务退出前等待异步发送完成。

### 单向消息发送

单向消息发送是指生产者只负责发送请求，不等待 Broker 返回结果，也不会触发回调。RocketMQ 官方文档说明，单向发送不会等待响应，也不会处理返回结果，适合耗时极短、可靠性要求不高的场景，例如日志采集。([RocketMQ](https://rocketmq.apache.org/docs/4.x/producer/02message1/?utm_source=chatgpt.com))

单向发送适合以下场景：

| 场景     | 说明                                   |
| -------- | -------------------------------------- |
| 操作日志 | 少量丢失可以接受，不影响主业务正确性。 |
| 埋点数据 | 以吞吐量为优先，允许少量数据缺失。     |
| 监控事件 | 非核心链路，失败后不做强补偿。         |

不建议使用单向发送处理订单、支付、库存、发券等核心业务消息。因为单向发送没有发送结果，调用方无法准确知道消息是否成功写入 Broker。

本章中的 `sendOneWayOrderEvent` 方法已经封装单向发送逻辑。单向发送完成日志只能表示客户端已经发起发送动作，不能表示 Broker 一定成功持久化消息。

关键代码如下：

```java
rocketMQTemplate.sendOneWay(destination, rocketMessage);

log.info("单向消息发送完成，destination={}", destination);
```

### 延迟消息发送

延迟消息是指消息发送到 RocketMQ 后，不会立即被消费者消费，而是在指定延迟时间后才变为可消费状态。RocketMQ 官方文档说明，延迟消息适用于分布式定时调度、任务超时处理等场景；在 RocketMQ 5.x 语义中，延迟或定时消息会在服务端存储到指定投递时间后再对消费者可见。([RocketMQ](https://rocketmq.apache.org/docs/featureBehavior/02delaymessage/?utm_source=chatgpt.com))

延迟消息适合以下场景：

| 场景         | 说明                                   |
| ------------ | -------------------------------------- |
| 订单超时取消 | 订单创建后延迟一段时间检查是否支付。   |
| 支付结果检查 | 支付请求提交后延迟查询第三方支付状态。 |
| 任务超时补偿 | 异步任务提交后延迟检查处理结果。       |
| 预约提醒     | 到达指定时间后触发提醒消息。           |

Spring Boot 中常见写法是使用 `rocketMQTemplate.syncSend(destination, message, timeout, delayLevel)` 发送延迟级别消息。具体延迟时间由 Broker 的 `messageDelayLevel` 配置决定，不应在业务代码中硬编码理解为绝对时间。

本章中的 `syncSendDelayOrderCancel` 方法使用延迟级别发送订单取消消息：

```java
SendResult sendResult = rocketMQTemplate.syncSend(
        destination,
        rocketMessage,
        3000L,
        delayLevel
);
```

本地测试时可以先使用较短延迟级别验证消费效果。生产环境中，订单超时取消这类业务不建议只依赖延迟消息本身，还应保留数据库状态校验。消费者收到延迟取消消息后，应先查询订单状态，只有订单仍处于未支付状态时才执行取消，避免误取消已支付订单。

延迟消息消费端处理逻辑建议如下：

```text
收到订单超时取消消息
        |
        v
根据 orderNo 查询订单
        |
        v
判断订单是否仍为待支付
        |
        +-- 是：执行取消订单
        |
        +-- 否：记录日志并跳过
```

### 顺序消息发送

顺序消息用于保证同一业务对象的多条消息按发送顺序进入同一个队列，并由顺序消费者按顺序处理。RocketMQ 官方文档说明，顺序消息可以根据 ShardingKey 将同一类消息路由到同一个队列中，例如同一个订单的创建、支付、发货应按顺序处理。([RocketMQ](https://rocketmq.apache.org/docs/4.x/producer/03message2/?utm_source=chatgpt.com))

顺序消息适合以下场景：

| 场景         | 说明                                               |
| ------------ | -------------------------------------------------- |
| 订单状态流转 | 同一个订单的创建、支付、发货、完成需要按顺序处理。 |
| 账户流水     | 同一个账户的入账、出账、冻结、解冻需要按顺序处理。 |
| 库存变更     | 同一个 SKU 的库存变更事件需要按顺序处理。          |
| 审批流程     | 同一业务单据的审批节点需要按顺序执行。             |

顺序发送的核心是 hashKey。相同 hashKey 的消息会被路由到同一个 MessageQueue，从而保证局部顺序。实际项目中通常使用订单号、账户号、用户 ID、SKU 编码等作为 hashKey。

本章中的 `syncSendOrderlyStatusChange` 方法使用订单号作为 hashKey：

```java
SendResult sendResult = rocketMQTemplate.syncSendOrderly(
        destination,
        rocketMessage,
        message.orderNo(),
        3000L
);
```

顺序消息不是全局顺序，而是分区顺序。也就是说，同一个订单的消息可以保证顺序，不同订单之间不保证顺序。实际设计时不要为了追求全局顺序把所有消息都路由到同一个队列，否则会严重降低并发能力。

顺序消息使用建议如下：

| 关注点   | 建议                                                         |
| -------- | ------------------------------------------------------------ |
| hashKey  | 使用业务对象唯一标识，例如订单号、账户号、SKU。              |
| Topic    | 顺序消息建议使用独立 Topic，避免与普通消息混用。             |
| 消费者   | 发送顺序消息后，消费端也应使用顺序消费模式。                 |
| 失败处理 | 某条消息消费失败会影响后续同队列消息消费，需要控制消费逻辑耗时和失败率。 |
| 幂等处理 | 顺序消息仍可能重复投递，消费端仍必须做幂等。                 |

### 事务消息发送

事务消息用于保证本地事务执行结果和消息发送结果最终一致。RocketMQ 官方文档说明，事务消息通过半消息、执行本地事务、提交或回滚事务状态、事务状态回查等机制，实现消息生产和本地事务之间的最终一致性。([RocketMQ](https://rocketmq.apache.org/docs/featureBehavior/04transactionmessage/?utm_source=chatgpt.com))

事务消息适合以下场景：

| 场景               | 说明                                       |
| ------------------ | ------------------------------------------ |
| 支付成功后通知订单 | 本地支付流水成功后，通知订单系统更新状态。 |
| 创建订单后通知库存 | 订单本地事务成功后，通知库存系统预扣库存。 |
| 账户变更后通知积分 | 账户本地变更成功后，通知积分系统异步处理。 |
| 核心业务事件发布   | 本地数据库事务和消息发送必须保持最终一致。 |

事务消息不保证下游消费者一定消费成功。官方文档也明确说明，事务消息保证的是本地核心事务与下游分支之间的最终一致性，但不保证消费结果和上游执行结果一致，因此下游业务仍需要通过消费重试等机制保证正确处理。([RocketMQ](https://rocketmq.apache.org/docs/featureBehavior/04transactionmessage/?utm_source=chatgpt.com))

事务消息发送分为三步：

```text
生产者发送半消息
        |
        v
执行本地事务
        |
        v
根据本地事务结果提交 COMMIT / ROLLBACK / UNKNOWN
        |
        v
Broker 根据事务状态决定是否投递消息
```

事务消息需要实现 `RocketMQLocalTransactionListener`，用于执行本地事务和处理事务状态回查。RocketMQ Spring Wiki 示例也说明，事务消息通过 `sendMessageInTransaction` 发送，并通过 `@RocketMQTransactionListener` 实现本地事务执行和本地事务检查。([GitHub](https://github.com/apache/rocketmq-spring/wiki/Transactional-Message?utm_source=chatgpt.com))

下面示例中的事务监听器用于模拟订单本地事务。真实项目中，`executeLocalTransaction` 应该执行数据库事务，`checkLocalTransaction` 应该根据本地事务表、订单表、支付流水表等可靠数据源查询事务状态，而不是依赖内存变量。

文件位置：`src/main/java/io/github/atengk/rocketmq/transaction/OrderTransactionMessageListener.java`

```java
package io.github.atengk.rocketmq.transaction;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.messaging.Message;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单事务消息监听器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@RocketMQTransactionListener
public class OrderTransactionMessageListener implements RocketMQLocalTransactionListener {

    /**
     * 示例用本地事务状态缓存。
     * 真实项目应使用订单表、事务日志表或消息发送记录表做事务状态回查。
     */
    private static final Map<String, RocketMQLocalTransactionState> TRANSACTION_STATE_CACHE = new ConcurrentHashMap<>();

    /**
     * 执行本地事务
     *
     * @param message 事务消息
     * @param arg     业务参数
     * @return 本地事务状态
     */
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message message, Object arg) {
        String orderNo = String.valueOf(arg);

        if (StrUtil.isBlank(orderNo)) {
            log.error("订单事务消息本地事务执行失败，订单号为空");
            return RocketMQLocalTransactionState.ROLLBACK;
        }

        try {
            log.info("开始执行订单本地事务，orderNo={}", orderNo);

            // 示例逻辑：真实项目中应在这里执行数据库本地事务
            // 例如：创建订单、保存事务日志、更新业务状态等
            if (StrUtil.containsIgnoreCase(orderNo, "ROLLBACK")) {
                TRANSACTION_STATE_CACHE.put(orderNo, RocketMQLocalTransactionState.ROLLBACK);
                log.warn("订单本地事务模拟回滚，orderNo={}", orderNo);
                return RocketMQLocalTransactionState.ROLLBACK;
            }

            if (StrUtil.containsIgnoreCase(orderNo, "UNKNOWN")) {
                TRANSACTION_STATE_CACHE.put(orderNo, RocketMQLocalTransactionState.UNKNOWN);
                log.warn("订单本地事务模拟未知状态，orderNo={}", orderNo);
                return RocketMQLocalTransactionState.UNKNOWN;
            }

            TRANSACTION_STATE_CACHE.put(orderNo, RocketMQLocalTransactionState.COMMIT);
            log.info("订单本地事务执行成功，orderNo={}", orderNo);
            return RocketMQLocalTransactionState.COMMIT;
        } catch (Exception exception) {
            TRANSACTION_STATE_CACHE.put(orderNo, RocketMQLocalTransactionState.UNKNOWN);
            log.error("订单本地事务执行异常，orderNo={}", orderNo, exception);
            return RocketMQLocalTransactionState.UNKNOWN;
        }
    }

    /**
     * 回查本地事务状态
     *
     * @param message 事务消息
     * @return 本地事务状态
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message message) {
        Object orderNoHeader = message.getHeaders().get("orderNo");
        String orderNo = orderNoHeader == null ? null : String.valueOf(orderNoHeader);

        if (StrUtil.isBlank(orderNo)) {
            log.error("订单事务消息回查失败，消息头中订单号为空");
            return RocketMQLocalTransactionState.UNKNOWN;
        }

        RocketMQLocalTransactionState transactionState = TRANSACTION_STATE_CACHE.get(orderNo);
        if (transactionState == null) {
            log.warn("订单事务状态不存在，等待下次回查，orderNo={}", orderNo);
            return RocketMQLocalTransactionState.UNKNOWN;
        }

        log.info("订单事务消息回查完成，orderNo={}，transactionState={}", orderNo, transactionState);
        return transactionState;
    }
}
```

事务消息开发注意事项如下：

| 关注点       | 建议                                                         |
| ------------ | ------------------------------------------------------------ |
| 本地事务     | 必须以数据库事务结果为准，不能只依赖内存状态。               |
| 回查逻辑     | 回查时应查询事务日志表或业务主表，判断本地事务是否成功。     |
| UNKNOWN 状态 | 本地事务仍在处理中时可以返回 UNKNOWN，不要过早 COMMIT 或 ROLLBACK。 |
| 超时处理     | 长时间无法确认状态的事务需要告警和人工排查。                 |
| 消费端       | 事务消息投递后仍可能重复消费，消费者必须幂等。               |
| Topic 类型   | RocketMQ 5.x 中事务消息 Topic 应按事务消息类型规划，不要和普通消息混用。 |

下面提供测试接口，用于本地验证本章中的几种发送方式。

文件位置：`src/main/java/io/github/atengk/rocketmq/controller/OrderMessageSendTestController.java`

```java
package io.github.atengk.rocketmq.controller;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.rocketmq.model.OrderEventMessage;
import io.github.atengk.rocketmq.producer.OrderMessageSendProducer;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单消息发送测试接口
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequiredArgsConstructor
public class OrderMessageSendTestController {

    private final OrderMessageSendProducer orderMessageSendProducer;

    /**
     * 测试同步发送
     *
     * @return 发送结果
     */
    @GetMapping("/test/rocketmq/send/sync")
    public String syncSend() {
        OrderEventMessage message = buildMessage("ORDER_CREATED");
        SendResult sendResult = orderMessageSendProducer.syncSendOrderCreated(message);
        return "同步发送成功：" + sendResult.getSendStatus();
    }

    /**
     * 测试异步发送
     *
     * @return 操作结果
     */
    @GetMapping("/test/rocketmq/send/async")
    public String asyncSend() {
        OrderEventMessage message = buildMessage("ORDER_PAID");
        orderMessageSendProducer.asyncSendOrderPaid(message);
        return "异步消息已提交，请查看应用日志确认发送结果";
    }

    /**
     * 测试单向发送
     *
     * @return 操作结果
     */
    @GetMapping("/test/rocketmq/send/one-way")
    public String sendOneWay() {
        OrderEventMessage message = buildMessage("ORDER_LOG");
        orderMessageSendProducer.sendOneWayOrderEvent(message);
        return "单向消息已发送";
    }

    /**
     * 测试延迟消息发送
     *
     * @param delayLevel 延迟级别
     * @return 发送结果
     */
    @GetMapping("/test/rocketmq/send/delay/{delayLevel}")
    public String sendDelay(@PathVariable Integer delayLevel) {
        OrderEventMessage message = buildMessage("ORDER_DELAY_CANCEL");
        SendResult sendResult = orderMessageSendProducer.syncSendDelayOrderCancel(message, delayLevel);
        return "延迟消息发送成功：" + sendResult.getSendStatus();
    }

    /**
     * 测试顺序消息发送
     *
     * @return 发送结果
     */
    @GetMapping("/test/rocketmq/send/orderly")
    public String sendOrderly() {
        String orderNo = "ORDER" + System.currentTimeMillis();

        OrderEventMessage createdMessage = buildMessage(orderNo, "ORDER_CREATED");
        OrderEventMessage paidMessage = buildMessage(orderNo, "ORDER_PAID");
        OrderEventMessage completedMessage = buildMessage(orderNo, "ORDER_COMPLETED");

        orderMessageSendProducer.syncSendOrderlyStatusChange(createdMessage);
        orderMessageSendProducer.syncSendOrderlyStatusChange(paidMessage);
        SendResult sendResult = orderMessageSendProducer.syncSendOrderlyStatusChange(completedMessage);

        return "顺序消息发送成功：" + sendResult.getSendStatus();
    }

    /**
     * 测试事务消息发送
     *
     * @param flag 事务标记，NORMAL、ROLLBACK、UNKNOWN
     * @return 事务发送结果
     */
    @GetMapping("/test/rocketmq/send/transaction/{flag}")
    public String sendTransaction(@PathVariable String flag) {
        String orderNo = "ORDER_" + flag + "_" + System.currentTimeMillis();
        OrderEventMessage message = buildMessage(orderNo, "ORDER_TRANSACTION");

        TransactionSendResult sendResult = orderMessageSendProducer.sendOrderTransactionMessage(message);
        return "事务消息发送完成：" + sendResult.getLocalTransactionState();
    }

    /**
     * 构建测试消息
     *
     * @param eventType 事件类型
     * @return 订单事件消息
     */
    private OrderEventMessage buildMessage(String eventType) {
        return buildMessage("ORDER" + System.currentTimeMillis(), eventType);
    }

    /**
     * 构建测试消息
     *
     * @param orderNo   订单号
     * @param eventType 事件类型
     * @return 订单事件消息
     */
    private OrderEventMessage buildMessage(String orderNo, String eventType) {
        return new OrderEventMessage(
                IdUtil.fastSimpleUUID(),
                orderNo,
                "USER10001",
                eventType,
                new BigDecimal("99.90"),
                LocalDateTime.now(),
                IdUtil.fastSimpleUUID()
        );
    }
}
```

本地验证命令如下：

```bash
# 启动 Spring Boot 服务
mvn spring-boot:run

# 同步消息发送
curl "http://127.0.0.1:8088/test/rocketmq/send/sync"

# 异步消息发送
curl "http://127.0.0.1:8088/test/rocketmq/send/async"

# 单向消息发送
curl "http://127.0.0.1:8088/test/rocketmq/send/one-way"

# 延迟消息发送，delayLevel 以 Broker 配置为准
curl "http://127.0.0.1:8088/test/rocketmq/send/delay/3"

# 顺序消息发送
curl "http://127.0.0.1:8088/test/rocketmq/send/orderly"

# 事务消息提交
curl "http://127.0.0.1:8088/test/rocketmq/send/transaction/NORMAL"

# 事务消息回滚
curl "http://127.0.0.1:8088/test/rocketmq/send/transaction/ROLLBACK"

# 事务消息未知状态，等待 Broker 后续回查
curl "http://127.0.0.1:8088/test/rocketmq/send/transaction/UNKNOWN"
```

命令说明：这些接口只用于本地开发环境验证消息发送链路。生产环境不建议暴露测试发送接口，核心业务消息应由真实业务流程触发，并结合数据库事务、发送日志、消费日志和告警系统进行闭环治理。

## 消息消费开发

本节用于说明 Spring Boot 3 项目中 RocketMQ 消息消费的常见开发方式，包括普通消息消费、顺序消息消费、广播模式消费、集群模式消费、消费重试机制和消费幂等处理。本节对应原文档大纲中的“消息消费开发”章节。

RocketMQ 消费端通常以 Consumer Group 为单位运行。普通业务场景中，一个 Consumer Group 内的多个消费者实例共同消费消息；不同 Consumer Group 之间互不影响，可以分别订阅同一个 Topic 下的消息。RocketMQ Push Consumer 默认使用集群消费模式，同一消费组内的消费者共同承担消费任务；广播模式下，同一消费组内的每个消费者实例都会消费完整消息。([RocketMQ](https://rocketmq.apache.org/docs/4.x/consumer/02push/?utm_source=chatgpt.com))

本节示例默认已经完成前面章节中的 Maven 依赖、`application.yml` 配置、Topic 和 Tag 常量定义。为了便于展示消费重试、消息元数据和幂等处理，消费者示例优先使用 `MessageExt` 接收原始消息。

示例文件结构如下：

```text
src/main/java/io/github/atengk/rocketmq/
├── constant/
│   └── RocketMqConsumerConstant.java
├── consumer/
│   ├── OrderNormalMessageConsumer.java
│   ├── OrderOrderlyMessageConsumer.java
│   ├── OrderBroadcastMessageConsumer.java
│   ├── OrderClusterMessageConsumer.java
│   └── OrderRetryMessageConsumer.java
├── model/
│   └── OrderEventMessage.java
└── service/
    ├── MessageIdempotentService.java
    └── RedisMessageIdempotentService.java
```

本章先定义消费者使用的 Topic、Tag 和 Consumer Group 常量。生产项目中建议把这些常量统一放在公共模块或基础组件中，避免生产者和消费者各自硬编码字符串。

文件位置：`src/main/java/io/github/atengk/rocketmq/constant/RocketMqConsumerConstant.java`

```java
package io.github.atengk.rocketmq.constant;

/**
 * RocketMQ 消费者常量
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class RocketMqConsumerConstant {

    /**
     * 订单事件 Topic
     */
    public static final String ORDER_EVENT_TOPIC = "order-event-topic";

    /**
     * 普通订单事件 Tag
     */
    public static final String ORDER_NORMAL_TAG = "order_normal";

    /**
     * 订单状态变更 Tag
     */
    public static final String ORDER_STATUS_CHANGE_TAG = "order_status_change";

    /**
     * 订单广播事件 Tag
     */
    public static final String ORDER_BROADCAST_TAG = "order_broadcast";

    /**
     * 订单重试测试 Tag
     */
    public static final String ORDER_RETRY_TAG = "order_retry";

    /**
     * 普通消息消费组
     */
    public static final String ORDER_NORMAL_CONSUMER_GROUP = "order-normal-consumer-group";

    /**
     * 顺序消息消费组
     */
    public static final String ORDER_ORDERLY_CONSUMER_GROUP = "order-orderly-consumer-group";

    /**
     * 广播消息消费组
     */
    public static final String ORDER_BROADCAST_CONSUMER_GROUP = "order-broadcast-consumer-group";

    /**
     * 集群消息消费组
     */
    public static final String ORDER_CLUSTER_CONSUMER_GROUP = "order-cluster-consumer-group";

    /**
     * 重试消息消费组
     */
    public static final String ORDER_RETRY_CONSUMER_GROUP = "order-retry-consumer-group";

    private RocketMqConsumerConstant() {
    }
}
```

### 普通消息消费

普通消息消费是最常见的消费方式，适用于不要求严格顺序的业务事件，例如订单创建通知、支付结果通知、用户行为事件、库存同步事件等。普通消息通常使用并发消费模式，消费端可以通过多个线程并发处理消息，提高吞吐量。

RocketMQ Push Consumer 支持并发消费和顺序消费。并发消费中，同一个队列内也可能由多个线程并发处理消息，因此即使发送端保证消息进入同一个队列，也不能依赖并发消费保证实际处理顺序。([RocketMQ](https://rocketmq.apache.org/docs/4.x/consumer/02push/?utm_source=chatgpt.com))

下面的普通消费者监听 `order-event-topic` 下的 `order_normal` Tag，并使用集群消费模式。消费逻辑中先解析消息体，再记录关键日志，最后执行业务处理。

文件位置：`src/main/java/io/github/atengk/rocketmq/consumer/OrderNormalMessageConsumer.java`

```java
package io.github.atengk.rocketmq.consumer;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.rocketmq.constant.RocketMqConsumerConstant;
import io.github.atengk.rocketmq.model.OrderEventMessage;
import io.github.atengk.rocketmq.service.MessageIdempotentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 订单普通消息消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = RocketMqConsumerConstant.ORDER_EVENT_TOPIC,
        consumerGroup = RocketMqConsumerConstant.ORDER_NORMAL_CONSUMER_GROUP,
        selectorExpression = RocketMqConsumerConstant.ORDER_NORMAL_TAG,
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.CLUSTERING
)
public class OrderNormalMessageConsumer implements RocketMQListener<MessageExt> {

    private final MessageIdempotentService messageIdempotentService;

    /**
     * 消费普通订单消息
     *
     * @param messageExt RocketMQ 原始消息
     */
    @Override
    public void onMessage(MessageExt messageExt) {
        String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        if (StrUtil.isBlank(body)) {
            log.warn("订单普通消息为空，topic={}，tag={}，msgId={}",
                    messageExt.getTopic(), messageExt.getTags(), messageExt.getMsgId());
            return;
        }

        OrderEventMessage message = JSONUtil.toBean(body, OrderEventMessage.class);
        String idempotentKey = buildIdempotentKey(messageExt, message);

        if (!messageIdempotentService.tryConsume(idempotentKey)) {
            log.info("订单普通消息已消费，跳过重复处理，orderNo={}，msgId={}，keys={}",
                    message.orderNo(), messageExt.getMsgId(), messageExt.getKeys());
            return;
        }

        try {
            log.info("开始消费订单普通消息，topic={}，tag={}，orderNo={}，eventType={}，reconsumeTimes={}",
                    messageExt.getTopic(),
                    messageExt.getTags(),
                    message.orderNo(),
                    message.eventType(),
                    messageExt.getReconsumeTimes());

            // 这里编写真实业务逻辑，例如保存订单事件、同步状态、触发后续流程等
            // 业务处理必须保证幂等，不能假设消息只会投递一次

            messageIdempotentService.confirmConsumed(idempotentKey);
            log.info("订单普通消息消费完成，orderNo={}，eventType={}，msgId={}",
                    message.orderNo(), message.eventType(), messageExt.getMsgId());
        } catch (Exception exception) {
            messageIdempotentService.cancelConsume(idempotentKey);
            log.error("订单普通消息消费失败，orderNo={}，eventType={}，msgId={}，reconsumeTimes={}",
                    message.orderNo(),
                    message.eventType(),
                    messageExt.getMsgId(),
                    messageExt.getReconsumeTimes(),
                    exception);

            // 抛出异常后，RocketMQ 会按消费重试策略重新投递消息
            throw exception;
        }
    }

    /**
     * 构建幂等键
     *
     * @param messageExt RocketMQ 原始消息
     * @param message    订单事件消息
     * @return 幂等键
     */
    private String buildIdempotentKey(MessageExt messageExt, OrderEventMessage message) {
        String businessKey = StrUtil.blankToDefault(message.orderNo(), messageExt.getKeys());
        return StrUtil.format("rocketmq:consume:{}:{}:{}",
                messageExt.getTopic(),
                messageExt.getTags(),
                businessKey);
    }
}
```

普通消息消费建议如下：

| 关注点   | 建议                                                 |
| -------- | ---------------------------------------------------- |
| 消费模式 | 默认使用并发消费，提升吞吐量。                       |
| 消费组   | 同一消费组内的消费者必须保持相同业务逻辑。           |
| Tag 过滤 | 消费者只订阅自己关心的 Tag，减少无效消息处理。       |
| 日志字段 | 至少记录 Topic、Tag、Key、MsgId、业务 ID、重试次数。 |
| 异常处理 | 业务处理失败时应抛出异常，不要吞掉异常后返回成功。   |
| 幂等处理 | 普通消息也必须做幂等，避免重复投递导致重复业务处理。 |

### 顺序消息消费

顺序消息消费用于保证同一业务对象的消息按顺序处理，例如同一个订单的创建、支付、发货、完成事件，或者同一个账户的入账、出账、冻结、解冻事件。顺序消费需要发送端和消费端同时配合：发送端使用相同业务键作为 hashKey 路由到同一个队列，消费端使用顺序消费模式处理消息。

RocketMQ 文档说明，顺序消费需要使用有序消费监听方式；如果使用并发消费，即使发送端保证消息按 FIFO 顺序进入同一个队列，实际消费时也不能保证顺序。([RocketMQ](https://rocketmq.apache.org/docs/4.x/consumer/02push/?utm_source=chatgpt.com))

下面的消费者使用 `ConsumeMode.ORDERLY`，表示按顺序消费消息。

文件位置：`src/main/java/io/github/atengk/rocketmq/consumer/OrderOrderlyMessageConsumer.java`

```java
package io.github.atengk.rocketmq.consumer;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.rocketmq.constant.RocketMqConsumerConstant;
import io.github.atengk.rocketmq.model.OrderEventMessage;
import io.github.atengk.rocketmq.service.MessageIdempotentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 订单顺序消息消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = RocketMqConsumerConstant.ORDER_EVENT_TOPIC,
        consumerGroup = RocketMqConsumerConstant.ORDER_ORDERLY_CONSUMER_GROUP,
        selectorExpression = RocketMqConsumerConstant.ORDER_STATUS_CHANGE_TAG,
        consumeMode = ConsumeMode.ORDERLY,
        messageModel = MessageModel.CLUSTERING
)
public class OrderOrderlyMessageConsumer implements RocketMQListener<MessageExt> {

    private final MessageIdempotentService messageIdempotentService;

    /**
     * 消费订单顺序消息
     *
     * @param messageExt RocketMQ 原始消息
     */
    @Override
    public void onMessage(MessageExt messageExt) {
        String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        OrderEventMessage message = JSONUtil.toBean(body, OrderEventMessage.class);
        String idempotentKey = buildIdempotentKey(messageExt, message);

        if (!messageIdempotentService.tryConsume(idempotentKey)) {
            log.info("订单顺序消息已消费，跳过重复处理，orderNo={}，eventType={}，msgId={}",
                    message.orderNo(), message.eventType(), messageExt.getMsgId());
            return;
        }

        try {
            log.info("开始消费订单顺序消息，orderNo={}，eventType={}，queueId={}，queueOffset={}，reconsumeTimes={}",
                    message.orderNo(),
                    message.eventType(),
                    messageExt.getQueueId(),
                    messageExt.getQueueOffset(),
                    messageExt.getReconsumeTimes());

            // 这里编写订单状态流转逻辑
            // 示例：ORDER_CREATED -> ORDER_PAID -> ORDER_DELIVERED -> ORDER_COMPLETED
            // 真实项目中应查询当前订单状态，校验状态机是否允许流转

            messageIdempotentService.confirmConsumed(idempotentKey);
            log.info("订单顺序消息消费完成，orderNo={}，eventType={}，queueId={}，queueOffset={}",
                    message.orderNo(),
                    message.eventType(),
                    messageExt.getQueueId(),
                    messageExt.getQueueOffset());
        } catch (Exception exception) {
            messageIdempotentService.cancelConsume(idempotentKey);
            log.error("订单顺序消息消费失败，orderNo={}，eventType={}，queueId={}，queueOffset={}",
                    message.orderNo(),
                    message.eventType(),
                    messageExt.getQueueId(),
                    messageExt.getQueueOffset(),
                    exception);

            // 顺序消费失败会影响同队列后续消息，必须谨慎处理异常
            throw exception;
        }
    }

    /**
     * 构建幂等键
     *
     * @param messageExt RocketMQ 原始消息
     * @param message    订单事件消息
     * @return 幂等键
     */
    private String buildIdempotentKey(MessageExt messageExt, OrderEventMessage message) {
        String businessKey = StrUtil.blankToDefault(message.orderNo(), messageExt.getKeys());
        return StrUtil.format("rocketmq:consume:orderly:{}:{}:{}:{}",
                messageExt.getTopic(),
                messageExt.getTags(),
                businessKey,
                message.eventType());
    }
}
```

顺序消息消费建议如下：

| 关注点         | 建议                                                         |
| -------------- | ------------------------------------------------------------ |
| 发送端 hashKey | 使用订单号、账户号、SKU 等业务对象标识，确保同一对象进入同一队列。 |
| 消费模式       | 消费端必须使用 `ConsumeMode.ORDERLY`。                       |
| 顺序范围       | RocketMQ 顺序消息通常保证局部顺序，不是全局顺序。            |
| 消费耗时       | 顺序消费中单条消息处理时间不宜过长，否则会阻塞同队列后续消息。 |
| 异常处理       | 顺序消息失败会影响后续消息，应减少非必要外部依赖调用。       |
| 状态校验       | 消费端仍应校验业务状态机，不能只依赖消息到达顺序。           |

### 广播模式消费

广播模式消费是指同一 Consumer Group 内的每个消费者实例都会消费一份完整消息。该模式适合本地缓存刷新、本地配置同步、节点级通知等场景，不适合订单扣款、发券、库存扣减等只能执行一次的业务。

RocketMQ 文档说明，广播模式下，同一消费组内的每个消费者都会消费完整消息；而集群模式下，同一消费组内的消费者共同消费消息。([RocketMQ](https://rocketmq.apache.org/docs/4.x/consumer/02push/?utm_source=chatgpt.com))

下面的消费者使用 `MessageModel.BROADCASTING` 声明广播模式。

文件位置：`src/main/java/io/github/atengk/rocketmq/consumer/OrderBroadcastMessageConsumer.java`

```java
package io.github.atengk.rocketmq.consumer;

import cn.hutool.json.JSONUtil;
import io.github.atengk.rocketmq.constant.RocketMqConsumerConstant;
import io.github.atengk.rocketmq.model.OrderEventMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 订单广播消息消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = RocketMqConsumerConstant.ORDER_EVENT_TOPIC,
        consumerGroup = RocketMqConsumerConstant.ORDER_BROADCAST_CONSUMER_GROUP,
        selectorExpression = RocketMqConsumerConstant.ORDER_BROADCAST_TAG,
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.BROADCASTING
)
public class OrderBroadcastMessageConsumer implements RocketMQListener<MessageExt> {

    /**
     * 消费广播消息
     *
     * @param messageExt RocketMQ 原始消息
     */
    @Override
    public void onMessage(MessageExt messageExt) {
        String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        OrderEventMessage message = JSONUtil.toBean(body, OrderEventMessage.class);

        log.info("收到订单广播消息，当前实例将独立处理，topic={}，tag={}，orderNo={}，eventType={}，msgId={}",
                messageExt.getTopic(),
                messageExt.getTags(),
                message.orderNo(),
                message.eventType(),
                messageExt.getMsgId());

        // 示例：刷新本机缓存、更新本地内存配置、触发当前节点上的本地动作
        // 广播消费不适合执行扣款、扣库存、发券等只能全局执行一次的业务
    }
}
```

广播模式使用建议如下：

| 场景                   | 是否适合广播 |
| ---------------------- | ------------ |
| 本地缓存刷新           | 适合         |
| 本地配置刷新           | 适合         |
| 每个节点都要执行的通知 | 适合         |
| 扣减库存               | 不适合       |
| 发放优惠券             | 不适合       |
| 支付记账               | 不适合       |
| 创建订单               | 不适合       |

广播模式下，如果服务部署 5 个实例，同一条广播消息会被 5 个实例分别消费。因此任何会造成全局数据变化的逻辑，都不应该放在广播消费者中执行。

### 集群模式消费

集群模式是 RocketMQ 最常用的消费模式。同一 Consumer Group 内可以部署多个消费者实例，每条消息通常只会被其中一个实例消费，从而实现消费端水平扩展和负载均衡。RocketMQ Push Consumer 默认使用集群模式。([RocketMQ](https://rocketmq.apache.org/docs/4.x/consumer/02push/?utm_source=chatgpt.com))

下面的消费者显式声明 `MessageModel.CLUSTERING`。虽然集群模式通常是默认值，但在团队文档和生产代码中显式声明更利于阅读和排查。

文件位置：`src/main/java/io/github/atengk/rocketmq/consumer/OrderClusterMessageConsumer.java`

```java
package io.github.atengk.rocketmq.consumer;

import cn.hutool.json.JSONUtil;
import io.github.atengk.rocketmq.constant.RocketMqConsumerConstant;
import io.github.atengk.rocketmq.model.OrderEventMessage;
import io.github.atengk.rocketmq.service.MessageIdempotentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 订单集群消息消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = RocketMqConsumerConstant.ORDER_EVENT_TOPIC,
        consumerGroup = RocketMqConsumerConstant.ORDER_CLUSTER_CONSUMER_GROUP,
        selectorExpression = RocketMqConsumerConstant.ORDER_NORMAL_TAG,
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.CLUSTERING
)
public class OrderClusterMessageConsumer implements RocketMQListener<MessageExt> {

    private final MessageIdempotentService messageIdempotentService;

    /**
     * 消费集群消息
     *
     * @param messageExt RocketMQ 原始消息
     */
    @Override
    public void onMessage(MessageExt messageExt) {
        String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        OrderEventMessage message = JSONUtil.toBean(body, OrderEventMessage.class);
        String idempotentKey = "rocketmq:consume:cluster:" + messageExt.getTopic() + ":" + messageExt.getTags() + ":" + message.orderNo();

        if (!messageIdempotentService.tryConsume(idempotentKey)) {
            log.info("订单集群消息重复消费，跳过处理，orderNo={}，msgId={}",
                    message.orderNo(), messageExt.getMsgId());
            return;
        }

        try {
            log.info("开始消费订单集群消息，orderNo={}，eventType={}，msgId={}，queueId={}",
                    message.orderNo(),
                    message.eventType(),
                    messageExt.getMsgId(),
                    messageExt.getQueueId());

            // 这里编写集群消费业务逻辑
            // 同一消费组内只有一个实例处理该消息，适合大部分业务事件消费

            messageIdempotentService.confirmConsumed(idempotentKey);
            log.info("订单集群消息消费完成，orderNo={}，eventType={}",
                    message.orderNo(), message.eventType());
        } catch (Exception exception) {
            messageIdempotentService.cancelConsume(idempotentKey);
            log.error("订单集群消息消费失败，orderNo={}，eventType={}，msgId={}",
                    message.orderNo(), message.eventType(), messageExt.getMsgId(), exception);
            throw exception;
        }
    }
}
```

集群模式使用建议如下：

| 关注点     | 建议                                                       |
| ---------- | ---------------------------------------------------------- |
| 扩容方式   | 增加同一 Consumer Group 的实例数量，提高消费能力。         |
| 消费组语义 | 同一 Consumer Group 表示同一类消费逻辑。                   |
| 多业务订阅 | 不同业务系统应使用不同 Consumer Group 订阅同一个 Topic。   |
| 消息重复   | 即使是集群模式，也仍然可能重复投递，必须做幂等。           |
| 负载均衡   | 队列数量会影响消费者实例的并发能力，Topic 队列数不能过少。 |

如果一个 Topic 只有 4 个队列，而同一 Consumer Group 部署了 8 个实例，则最多只有 4 个实例能直接分配到队列，其余实例可能处于空闲或低负载状态。生产环境中应结合 Topic 队列数、消费者实例数和消费耗时一起规划吞吐能力。

### 消费重试机制

消费重试是 RocketMQ 保证消费完整性的重要机制。当消费者处理失败、抛出异常、返回失败状态，或者消费超时后，Broker 会根据消费重试策略重新投递消息；如果超过最大重试次数仍然失败，消息会进入死信队列。RocketMQ 官方文档说明，消费重试是针对业务逻辑失败的保护措施，不能用于控制业务流程，也不应被当作限流手段。([RocketMQ](https://rocketmq.apache.org/docs/featureBehavior/10consumerretrypolicy/?utm_source=chatgpt.com))

RocketMQ PushConsumer 的重试状态包括 Ready、Inflight、WaitingRetry、Commit 和 DLQ。普通无序消息的重试间隔通常是递增的，例如第 1 次 10 秒、第 2 次 30 秒、第 3 次 1 分钟，超过 16 次后后续每次间隔为 2 小时；顺序消息的重试间隔通常是固定的。最大重试次数由 Consumer Group 元数据指定，如果最大重试次数为 3，则消息最多会被投递 4 次，即 1 次原始投递和 3 次重试。([RocketMQ](https://rocketmq.apache.org/docs/featureBehavior/10consumerretrypolicy/?utm_source=chatgpt.com))

RocketMQ 文档还明确提醒，PushConsumer 不应在消息未处理完成前提前返回消费成功，也不应把消息转交给自定义线程后提前返回成功；否则 RocketMQ 无法感知真实消费结果，也不会触发正确的重试。([RocketMQ](https://rocketmq.apache.org/docs/featureBehavior/06consumertype/?utm_source=chatgpt.com))

下面的消费者用于演示消费重试。示例中当订单号包含 `RETRY` 时主动抛出异常，用于触发 RocketMQ 重试机制。

文件位置：`src/main/java/io/github/atengk/rocketmq/consumer/OrderRetryMessageConsumer.java`

```java
package io.github.atengk.rocketmq.consumer;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.rocketmq.constant.RocketMqConsumerConstant;
import io.github.atengk.rocketmq.model.OrderEventMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 订单消费重试示例消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = RocketMqConsumerConstant.ORDER_EVENT_TOPIC,
        consumerGroup = RocketMqConsumerConstant.ORDER_RETRY_CONSUMER_GROUP,
        selectorExpression = RocketMqConsumerConstant.ORDER_RETRY_TAG,
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.CLUSTERING
)
public class OrderRetryMessageConsumer implements RocketMQListener<MessageExt> {

    /**
     * 消费重试测试消息
     *
     * @param messageExt RocketMQ 原始消息
     */
    @Override
    public void onMessage(MessageExt messageExt) {
        String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        OrderEventMessage message = JSONUtil.toBean(body, OrderEventMessage.class);

        log.info("开始消费重试测试消息，orderNo={}，eventType={}，msgId={}，reconsumeTimes={}",
                message.orderNo(),
                message.eventType(),
                messageExt.getMsgId(),
                messageExt.getReconsumeTimes());

        if (StrUtil.containsIgnoreCase(message.orderNo(), "RETRY")) {
            log.warn("模拟订单消息消费失败，准备触发重试，orderNo={}，reconsumeTimes={}",
                    message.orderNo(), messageExt.getReconsumeTimes());
            throw new IllegalStateException("模拟消费失败，触发 RocketMQ 消费重试");
        }

        log.info("重试测试消息消费成功，orderNo={}，msgId={}",
                message.orderNo(), messageExt.getMsgId());
    }
}
```

消费重试使用建议如下：

| 关注点       | 建议                                                       |
| ------------ | ---------------------------------------------------------- |
| 适用场景     | 适合临时异常、下游短暂不可用、业务状态短时间不可达等情况。 |
| 不适用场景   | 不要把消费失败当作业务分支控制，也不要用失败重试实现限流。 |
| 异常处理     | 需要重试时抛出异常，不需要重试的业务异常应记录后正常返回。 |
| 最大重试次数 | 设置合理次数，避免大量消息无限重试拖垮系统。               |
| 死信队列     | 超过最大重试次数后需要有死信处理和人工补偿流程。           |
| 日志字段     | 必须记录 `reconsumeTimes`，便于判断消息是否已经多次失败。  |

实际项目中，不是所有异常都应该触发重试。可以按以下原则处理：

```text
消费异常
  |
  +-- 参数缺失、格式错误、业务对象不存在且不可恢复
  |       |
  |       +-- 记录错误日志，保存异常消息，正常返回，避免无意义重试
  |
  +-- 数据库临时异常、下游服务短暂不可用、状态稍后可恢复
  |       |
  |       +-- 抛出异常，交给 RocketMQ 触发重试
  |
  +-- 未知异常
          |
          +-- 记录完整上下文，按业务重要性决定重试或进入补偿流程
```

### 消费幂等处理

消费幂等是 RocketMQ 消费端必须具备的基础能力。消息可能因为网络抖动、消费超时、消费者重启、Broker 重试、手动补偿等原因被重复投递。如果消费端不做幂等，可能导致重复扣款、重复发券、重复生成流水、重复发送通知等问题。

RocketMQ 的消费语义应按“至少消费一次”来设计。也就是说，系统应假设消息可能被重复消费，然后通过业务唯一键、状态机、数据库唯一索引、Redis 幂等键或消费记录表来保证业务结果只生效一次。

常见幂等方案如下：

| 方案           | 说明                                                         | 适用场景                           |
| -------------- | ------------------------------------------------------------ | ---------------------------------- |
| 数据库唯一索引 | 使用业务唯一键建立唯一索引，重复插入时直接忽略或转为已处理。 | 支付流水、发券记录、订单事件记录。 |
| 状态机判断     | 根据当前业务状态判断是否允许执行当前事件。                   | 订单状态流转、审批流程。           |
| Redis 幂等键   | 使用 `SETNX` 抢占消费资格，消费完成后标记完成。              | 高并发消费、轻量级去重。           |
| 消费记录表     | 记录 Topic、Tag、Key、MsgId、消费状态和异常信息。            | 核心业务、可审计消息消费。         |
| 业务版本号     | 消息带版本号，消费者只处理比当前版本新的消息。               | 数据同步、缓存刷新。               |

如果项目已经使用 Redis，可以先使用 Redis 实现轻量级幂等。下面示例使用 `StringRedisTemplate` 实现消费抢占、确认和回滚。需要先补充 Redis 依赖。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Data Redis：用于实现消费幂等键、缓存和分布式状态标记 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
</dependencies>
```

Redis 基础配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  data:
    redis:
      # Redis 地址，生产环境建议使用配置中心或环境变量管理
      host: 127.0.0.1
      # Redis 端口
      port: 6379
      # Redis 数据库索引
      database: 0
      # Redis 连接超时时间
      timeout: 3000ms
```

先定义幂等服务接口，屏蔽底层存储实现。

文件位置：`src/main/java/io/github/atengk/rocketmq/service/MessageIdempotentService.java`

```java
package io.github.atengk.rocketmq.service;

/**
 * 消息消费幂等服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface MessageIdempotentService {

    /**
     * 尝试获取消费资格
     *
     * @param idempotentKey 幂等键
     * @return true 表示允许消费，false 表示重复消息
     */
    boolean tryConsume(String idempotentKey);

    /**
     * 确认消费完成
     *
     * @param idempotentKey 幂等键
     */
    void confirmConsumed(String idempotentKey);

    /**
     * 取消本次消费占用
     *
     * @param idempotentKey 幂等键
     */
    void cancelConsume(String idempotentKey);
}
```

下面的 Redis 实现使用 `SETNX` 抢占消费资格。消费开始时写入 `PROCESSING`，消费成功后更新为 `CONSUMED`；如果消费异常，则删除幂等键，让后续重试消息可以再次消费。

文件位置：`src/main/java/io/github/atengk/rocketmq/service/RedisMessageIdempotentService.java`

```java
package io.github.atengk.rocketmq.service;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis 消息消费幂等服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisMessageIdempotentService implements MessageIdempotentService {

    private static final String PROCESSING = "PROCESSING";

    private static final String CONSUMED = "CONSUMED";

    private static final Duration PROCESSING_EXPIRE = Duration.ofMinutes(10);

    private static final Duration CONSUMED_EXPIRE = Duration.ofDays(7);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试获取消费资格
     *
     * @param idempotentKey 幂等键
     * @return true 表示允许消费，false 表示重复消息
     */
    @Override
    public boolean tryConsume(String idempotentKey) {
        if (StrUtil.isBlank(idempotentKey)) {
            throw new IllegalArgumentException("消息消费幂等键不能为空");
        }

        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, PROCESSING, PROCESSING_EXPIRE);

        if (BooleanUtil.isTrue(success)) {
            log.info("获取消息消费资格成功，idempotentKey={}", idempotentKey);
            return true;
        }

        String currentState = stringRedisTemplate.opsForValue().get(idempotentKey);
        log.info("消息消费资格已存在，跳过重复消费，idempotentKey={}，state={}",
                idempotentKey, currentState);
        return false;
    }

    /**
     * 确认消费完成
     *
     * @param idempotentKey 幂等键
     */
    @Override
    public void confirmConsumed(String idempotentKey) {
        stringRedisTemplate.opsForValue().set(idempotentKey, CONSUMED, CONSUMED_EXPIRE);
        log.info("消息消费幂等状态已确认完成，idempotentKey={}", idempotentKey);
    }

    /**
     * 取消本次消费占用
     *
     * @param idempotentKey 幂等键
     */
    @Override
    public void cancelConsume(String idempotentKey) {
        String currentState = stringRedisTemplate.opsForValue().get(idempotentKey);

        if (StrUtil.equals(PROCESSING, currentState)) {
            stringRedisTemplate.delete(idempotentKey);
            log.warn("消息消费失败，已释放消费幂等键，idempotentKey={}", idempotentKey);
            return;
        }

        log.info("消息消费幂等键未释放，idempotentKey={}，state={}", idempotentKey, currentState);
    }
}
```

Redis 幂等方案适合大多数轻量场景，但核心资金类、库存类、发券类业务更建议使用数据库唯一索引或消费记录表作为最终约束。Redis 可以作为第一层快速去重，但不能替代数据库层面的业务一致性保护。

消费幂等键建议按以下格式设计：

```text
rocketmq:consume:{topic}:{tag}:{businessKey}
```

其中 `businessKey` 优先使用业务唯一键，例如订单号、支付单号、发券记录号、库存流水号。如果业务消息没有稳定业务键，才考虑使用 RocketMQ `keys` 或 `msgId`。一般不建议只使用 `msgId` 做业务幂等，因为补偿消息、重发消息和人工修复消息可能拥有不同 `msgId`，但代表同一个业务动作。

本章消费者启动后，可以通过以下方式验证消费链路：

```bash
# 启动 Redis，示例仅用于本地开发
docker run -d \
  --name redis-dev \
  -p 6379:6379 \
  redis:7

# 启动 Spring Boot 服务
mvn spring-boot:run

# 发送普通消息后，查看消费者日志
tail -f logs/rocketmq-demo-service.log

# 查看 Redis 中的消费幂等键
docker exec -it redis-dev redis-cli keys "rocketmq:consume:*"
```

命令说明：`redis:7` 用于本地验证幂等键写入；`mvn spring-boot:run` 启动消费者服务；`redis-cli keys` 仅适合本地开发查看，不建议在生产环境对大规模 Redis 实例执行 `keys` 命令，生产环境应使用 `scan` 或通过业务后台查询消费记录。

本章的核心原则是：消费者处理成功才算消息完成；处理失败时应明确抛出异常触发重试；任何消费逻辑都必须支持幂等；广播模式只用于每个节点都需要执行的场景；顺序消息需要发送端和消费端同时保证顺序语义。

## Topic 与 Tag 设计

本节用于规范 RocketMQ 中 Topic、Tag、消息模型和业务消息体的设计方式，避免后续出现 Topic 混乱、Tag 滥用、消费者过滤困难、消息体不兼容和问题排查困难等问题。本节对应原文档大纲中的“Topic 与 Tag 设计”章节。

RocketMQ 中，Topic 是消息资源的顶层容器，适合按业务类型进行消息分类和隔离；Topic 内部由一个或多个队列组成，实际消息存储和扩展能力基于队列实现。RocketMQ 5.x 还支持为 Topic 指定消息类型，例如普通消息、FIFO 消息、延迟消息和事务消息，并支持对消息类型进行校验，因此实际项目中应避免在同一个 Topic 中混合多种消息语义。([RocketMQ](https://rocketmq.apache.org/docs/domainModel/02topic/?utm_source=chatgpt.com))

### Topic 命名规范

Topic 命名用于表达消息所属的业务域和消息语义。Topic 不应该按单个接口、单个方法或单个数据库表随意创建，而应围绕业务领域、消息类型和环境隔离进行统一规划。

推荐命名格式如下：

```text
{业务域}-{消息语义}-{消息类型}-topic
```

常见示例：

| Topic                          | 说明                                                 |
| ------------------------------ | ---------------------------------------------------- |
| `order-event-normal-topic`     | 订单普通事件消息，例如订单创建、订单支付、订单取消。 |
| `order-status-fifo-topic`      | 订单状态顺序消息，例如创建、支付、发货、完成。       |
| `order-timeout-delay-topic`    | 订单超时延迟消息，例如超时未支付自动取消。           |
| `payment-event-normal-topic`   | 支付普通事件消息，例如支付成功、支付失败、退款成功。 |
| `inventory-event-normal-topic` | 库存普通事件消息，例如库存扣减、库存释放。           |
| `account-transaction-topic`    | 账户事务消息，例如账户变更后通知下游系统。           |

Topic 命名建议如下：

| 规范项       | 建议                                                         |
| ------------ | ------------------------------------------------------------ |
| 业务域清晰   | Topic 名称应能直接看出所属业务域，例如 `order`、`payment`、`inventory`。 |
| 消息语义明确 | 名称中应体现事件、状态、超时、事务等语义。                   |
| 消息类型隔离 | 普通消息、顺序消息、延迟消息、事务消息建议拆分不同 Topic。   |
| 环境隔离     | 开发、测试、预发、生产环境应通过集群、命名空间或统一前缀隔离。 |
| 避免过细     | 不建议为每个小动作创建一个 Topic，细分动作应优先使用 Tag。   |
| 避免过粗     | 不建议把订单、支付、库存、通知等无关业务混在一个 Topic。     |
| 统一小写     | 建议使用小写字母、数字和中横线，减少跨语言、跨平台使用差异。 |

推荐命名：

```text
order-event-normal-topic
order-status-fifo-topic
order-timeout-delay-topic
payment-event-normal-topic
inventory-event-normal-topic
```

不推荐命名：

```text
topic1
test-topic
order
OrderTopic
send-message-topic
business-topic
```

Topic 设计时需要重点区分“业务域”和“事件类型”。例如订单业务中的订单创建、订单支付、订单取消都属于订单事件，可以放在同一个订单普通事件 Topic 中，再通过不同 Tag 区分。如果是订单状态严格流转，则应单独使用顺序消息 Topic，避免与普通事件混用。

生产环境中不建议开启自动创建 Topic。Topic 应由平台、运维或基础架构模块统一创建，并明确队列数、消息类型、权限、保留时间和监控告警策略。RocketMQ Topic 名称在集群内具有唯一性，Topic 还承担消息分类、订阅隔离和权限管理职责，因此 Topic 设计属于架构设计的一部分，而不是单纯的字符串命名。([RocketMQ](https://rocketmq.apache.org/docs/domainModel/02topic/?utm_source=chatgpt.com))

### Tag 使用规范

Tag 是 Topic 下的细粒度消息分类属性。RocketMQ 的消息过滤功能支持基于 Tag 的过滤，消费者可以只订阅自己关心的 Tag，从而减少无关消息投递和消费端判断逻辑。RocketMQ 官方文档说明，生产者在消息上设置 Tag，消费者在订阅时设置过滤表达式，Broker 会根据过滤条件筛选后再投递给消费者。([RocketMQ](https://rocketmq.apache.org/docs/featureBehavior/07messagefilter/?utm_source=chatgpt.com))

Tag 推荐用于表达事件类型，而不是表达业务对象 ID、用户 ID、订单号或租户 ID。业务对象 ID 应放在 Message Key 或消息体中，不能放在 Tag 中。

推荐命名格式如下：

```text
{业务动作}_{业务状态}
```

订单业务常见 Tag：

| Tag                    | 说明             |
| ---------------------- | ---------------- |
| `order_created`        | 订单已创建。     |
| `order_paid`           | 订单已支付。     |
| `order_cancelled`      | 订单已取消。     |
| `order_completed`      | 订单已完成。     |
| `order_timeout_cancel` | 订单超时取消。   |
| `order_status_changed` | 订单状态已变更。 |

支付业务常见 Tag：

| Tag               | 说明           |
| ----------------- | -------------- |
| `payment_created` | 支付单已创建。 |
| `payment_success` | 支付成功。     |
| `payment_failed`  | 支付失败。     |
| `refund_success`  | 退款成功。     |
| `refund_failed`   | 退款失败。     |

Tag 使用建议如下：

| 规范项      | 建议                                                         |
| ----------- | ------------------------------------------------------------ |
| 单一职责    | 一个 Tag 表示一种明确事件类型。                              |
| 不放业务 ID | 不要把订单号、用户 ID、租户 ID 放在 Tag 中。                 |
| 不表达环境  | 环境隔离应由集群、命名空间或 Topic 命名控制，不应放在 Tag 中。 |
| 不频繁变更  | Tag 属于消费订阅契约，频繁变更会影响消费者。                 |
| 命名稳定    | 已上线 Tag 尽量只新增，不删除、不重命名、不改变语义。        |
| 消费组一致  | 同一 Consumer Group 内的订阅表达式应保持一致。               |
| 精准过滤    | 消费者应只订阅自己关心的 Tag，避免使用 `*` 接收所有消息后再自行判断。 |

RocketMQ Tag 过滤支持单 Tag 匹配、多 Tag 匹配和全量匹配。多 Tag 过滤使用 `||` 分隔，例如 `order_created || order_paid`；全量匹配使用 `*`。官方文档还说明，生产者发送消息时每条消息只设置一个 Tag，Tag 推荐最大长度为 128 个字符。([RocketMQ](https://rocketmq.apache.org/docs/featureBehavior/07messagefilter/?utm_source=chatgpt.com))

Spring Boot 消费者中可以通过 `selectorExpression` 指定 Tag 过滤条件。

文件位置：`src/main/java/io/github/atengk/rocketmq/consumer/OrderPaidConsumer.java`

```java
package io.github.atengk.rocketmq.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 订单支付消息消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "order-event-normal-topic",
        consumerGroup = "order-paid-consumer-group",
        selectorExpression = "order_paid",
        messageModel = MessageModel.CLUSTERING
)
public class OrderPaidConsumer implements RocketMQListener<String> {

    /**
     * 消费订单支付消息
     *
     * @param messageBody 消息体
     */
    @Override
    public void onMessage(String messageBody) {
        log.info("收到订单支付消息，messageBody={}", messageBody);
    }
}
```

如果同一个消费者确实需要消费多个 Tag，可以使用 `||`。

```java
@RocketMQMessageListener(
        topic = "order-event-normal-topic",
        consumerGroup = "order-event-consumer-group",
        selectorExpression = "order_created || order_paid || order_cancelled"
)
```

不建议在核心业务消费者中长期使用 `*`。`*` 会订阅 Topic 下所有 Tag，后续 Topic 增加新事件时，旧消费者也会收到新消息，容易引发兼容性问题。

### 消息模型设计

消息模型用于规范 Producer 和 Consumer 之间的通信契约。一个稳定的消息模型至少需要明确 Topic、Tag、Key、消息体、扩展属性、事件时间和版本信息。RocketMQ 官方概念文档将 Message 定义为数据传输的最小单位，生产者会将业务数据和扩展属性封装为消息发送给 Broker，再由 Broker 按语义投递给消费者。([RocketMQ](https://rocketmq.apache.org/docs/domainModel/04message/?utm_source=chatgpt.com))

推荐的 RocketMQ 消息模型如下：

| 字段       | 位置     | 说明                                                    |
| ---------- | -------- | ------------------------------------------------------- |
| Topic      | 发送目标 | 表示业务域和消息类型，例如 `order-event-normal-topic`。 |
| Tag        | 发送目标 | 表示事件类型，例如 `order_paid`。                       |
| Key        | 消息头   | 表示业务唯一标识，例如订单号、支付单号、流水号。        |
| Body       | 消息体   | 表示业务数据，建议使用 JSON DTO。                       |
| Properties | 消息属性 | 表示扩展元数据，例如 traceId、tenantId、sourceSystem。  |
| Event Time | 消息体   | 表示业务事件发生时间，而不是消息发送时间。              |
| Version    | 消息体   | 表示消息结构版本，用于兼容升级。                        |

消息模型常量建议集中管理。下面示例定义订单 Topic、Tag、Consumer Group 和发送目标构造方法。

文件位置：`src/main/java/io/github/atengk/rocketmq/constant/OrderRocketMqConstant.java`

```java
package io.github.atengk.rocketmq.constant;

import cn.hutool.core.util.StrUtil;

/**
 * 订单 RocketMQ 常量
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class OrderRocketMqConstant {

    /**
     * 订单普通事件 Topic
     */
    public static final String ORDER_EVENT_NORMAL_TOPIC = "order-event-normal-topic";

    /**
     * 订单顺序事件 Topic
     */
    public static final String ORDER_STATUS_FIFO_TOPIC = "order-status-fifo-topic";

    /**
     * 订单延迟事件 Topic
     */
    public static final String ORDER_TIMEOUT_DELAY_TOPIC = "order-timeout-delay-topic";

    /**
     * 订单已创建 Tag
     */
    public static final String TAG_ORDER_CREATED = "order_created";

    /**
     * 订单已支付 Tag
     */
    public static final String TAG_ORDER_PAID = "order_paid";

    /**
     * 订单已取消 Tag
     */
    public static final String TAG_ORDER_CANCELLED = "order_cancelled";

    /**
     * 订单超时取消 Tag
     */
    public static final String TAG_ORDER_TIMEOUT_CANCEL = "order_timeout_cancel";

    /**
     * 订单支付消费者组
     */
    public static final String ORDER_PAID_CONSUMER_GROUP = "order-paid-consumer-group";

    private OrderRocketMqConstant() {
    }

    /**
     * 构建发送目标
     *
     * @param topic Topic
     * @param tag   Tag
     * @return RocketMQ 发送目标
     */
    public static String destination(String topic, String tag) {
        if (StrUtil.isBlank(topic)) {
            throw new IllegalArgumentException("Topic不能为空");
        }
        if (StrUtil.isBlank(tag)) {
            return topic;
        }
        return topic + ":" + tag;
    }
}
```

Producer 发送消息时，应明确设置 Topic、Tag、Key 和扩展属性。Key 用于消息检索和业务排查，建议使用业务唯一标识。RocketMQ 概念文档中也说明 MessageKey 是面向消息的索引属性，通过设置消息索引可以快速查找对应消息内容。([RocketMQ](https://rocketmq.apache.org/docs/introduction/02concepts/?utm_source=chatgpt.com))

下面示例展示一个订单事件消息的发送模型。

文件位置：`src/main/java/io/github/atengk/rocketmq/producer/OrderEventProducer.java`

```java
package io.github.atengk.rocketmq.producer;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.rocketmq.constant.OrderRocketMqConstant;
import io.github.atengk.rocketmq.model.OrderEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 订单事件生产者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 发送订单已支付事件
     *
     * @param message 订单事件消息
     * @return 发送结果
     */
    public SendResult sendOrderPaidEvent(OrderEventMessage message) {
        checkMessage(message);

        String destination = OrderRocketMqConstant.destination(
                OrderRocketMqConstant.ORDER_EVENT_NORMAL_TOPIC,
                OrderRocketMqConstant.TAG_ORDER_PAID
        );

        Message<String> rocketMessage = MessageBuilder.withPayload(JSONUtil.toJsonStr(message))
                // RocketMQ 消息 Key，建议使用业务唯一标识
                .setHeader(RocketMQHeaders.KEYS, message.orderNo())
                // 业务消息 ID，用于业务系统内部日志追踪
                .setHeader("messageId", message.messageId())
                // 链路追踪 ID，用于跨系统排查
                .setHeader("traceId", message.traceId())
                // 来源系统，用于排查消息来源
                .setHeader("sourceSystem", message.sourceSystem())
                .build();

        SendResult sendResult = rocketMQTemplate.syncSend(destination, rocketMessage, 3000L);

        log.info("订单已支付事件发送成功，topic={}，tag={}，orderNo={}，messageId={}，sendStatus={}",
                OrderRocketMqConstant.ORDER_EVENT_NORMAL_TOPIC,
                OrderRocketMqConstant.TAG_ORDER_PAID,
                message.orderNo(),
                message.messageId(),
                sendResult.getSendStatus());

        return sendResult;
    }

    /**
     * 校验订单事件消息
     *
     * @param message 订单事件消息
     */
    private void checkMessage(OrderEventMessage message) {
        Assert.notNull(message, "订单事件消息不能为空");
        Assert.notBlank(message.messageId(), "消息ID不能为空");
        Assert.notBlank(message.orderNo(), "订单号不能为空");
        Assert.notBlank(message.eventType(), "事件类型不能为空");
        Assert.notNull(message.eventTime(), "事件时间不能为空");
    }

    /**
     * 创建基础订单事件消息 ID
     *
     * @return 消息 ID
     */
    public String createMessageId() {
        return IdUtil.fastSimpleUUID();
    }
}
```

消息模型设计建议如下：

| 设计项     | 建议                                               |
| ---------- | -------------------------------------------------- |
| Topic      | 按业务域和消息类型规划，不要承载无关业务。         |
| Tag        | 按事件类型规划，不要承载业务对象 ID。              |
| Key        | 必须设置业务唯一键，例如订单号、支付单号、流水号。 |
| Body       | 使用独立消息 DTO，不直接使用数据库实体类。         |
| Header     | 放置 traceId、sourceSystem、tenantId 等扩展信息。  |
| Version    | 重要消息建议增加消息版本，便于后续兼容升级。       |
| Event Time | 使用业务事件发生时间，不要只依赖发送时间。         |

### 业务消息体设计

业务消息体是 Producer 和 Consumer 之间的通信协议。消息一旦发送出去，就应当被视为已经发生的事实事件，不应在消费端修改消息本身。RocketMQ 官方文档说明，消息具有不可变性，消息生成后内容不会改变；默认情况下，RocketMQ 会持久化消息，使消息在系统故障后仍可追踪和恢复。([RocketMQ](https://rocketmq.apache.org/docs/domainModel/04message/?utm_source=chatgpt.com))

业务消息体设计原则如下：

| 原则     | 说明                                                      |
| -------- | --------------------------------------------------------- |
| 独立 DTO | 消息体使用独立 DTO，不直接暴露数据库 Entity。             |
| 字段稳定 | 已发布字段不随意删除、不随意改变含义。                    |
| 必填明确 | 业务主键、事件类型、事件时间、消息版本应明确。            |
| 类型清晰 | 金额、时间、枚举、状态字段要有明确类型和格式。            |
| 控制大小 | 不传大对象、大文本、大集合，必要时只传引用 ID。           |
| 兼容升级 | 新增字段应保证旧消费者可以忽略，删除字段应有过渡期。      |
| 可追踪   | 消息体或消息头中应包含 messageId、traceId、sourceSystem。 |
| 可幂等   | 消息体中必须包含可用于消费幂等的业务唯一键。              |

推荐的通用业务消息体结构如下：

```json
{
  "messageId": "9c8d0b0d3d204b5e9f1c6b9a1b7e6a10",
  "version": "1.0",
  "eventType": "order_paid",
  "eventTime": "2026-04-30T10:30:00",
  "sourceSystem": "order-service",
  "traceId": "9b95e5a5747344e09c4d8949f6f9489a",
  "tenantId": "default",
  "orderNo": "ORDER202604300001",
  "userId": "USER10001",
  "amount": 99.90
}
```

下面是订单事件消息体的 Java 定义。

文件位置：`src/main/java/io/github/atengk/rocketmq/model/OrderEventMessage.java`

```java
package io.github.atengk.rocketmq.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单事件消息
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderEventMessage(
        String messageId,
        String version,
        String eventType,
        LocalDateTime eventTime,
        String sourceSystem,
        String traceId,
        String tenantId,
        String orderNo,
        String userId,
        BigDecimal amount
) {
}
```

如果项目不适合使用 `record`，也可以使用普通 DTO。下面示例使用 Lombok，适合需要兼容更多序列化框架或需要无参构造方法的项目。

文件位置：`src/main/java/io/github/atengk/rocketmq/model/OrderEventMessageDTO.java`

```java
package io.github.atengk.rocketmq.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单事件消息 DTO
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Data
public class OrderEventMessageDTO {

    /**
     * 消息 ID
     */
    private String messageId;

    /**
     * 消息版本
     */
    private String version;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 事件发生时间
     */
    private LocalDateTime eventTime;

    /**
     * 来源系统
     */
    private String sourceSystem;

    /**
     * 链路追踪 ID
     */
    private String traceId;

    /**
     * 租户 ID
     */
    private String tenantId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 订单金额
     */
    private BigDecimal amount;
}
```

消息体字段建议如下：

| 字段           | 是否建议必填 | 说明                                               |
| -------------- | ------------ | -------------------------------------------------- |
| `messageId`    | 是           | 业务消息 ID，用于日志串联和排查。                  |
| `version`      | 是           | 消息结构版本，例如 `1.0`、`1.1`。                  |
| `eventType`    | 是           | 事件类型，应与 Tag 语义保持一致。                  |
| `eventTime`    | 是           | 业务事件发生时间。                                 |
| `sourceSystem` | 是           | 来源系统，例如 `order-service`。                   |
| `traceId`      | 是           | 链路追踪 ID。                                      |
| `tenantId`     | 按需         | 多租户系统建议必填。                               |
| `orderNo`      | 是           | 订单业务唯一键，可用于幂等。                       |
| `userId`       | 按需         | 与用户相关的消息建议保留。                         |
| `amount`       | 按需         | 金额字段建议使用 `BigDecimal` 或最小货币单位整数。 |

业务消息体不建议这样设计：

```json
{
  "id": 1,
  "name": "test",
  "data": "{...}",
  "entity": "{数据库实体完整内容}",
  "list": ["大量明细数据"]
}
```

主要问题是字段语义不清晰、消息体过大、与数据库结构强耦合、后续版本难以兼容。更合理的方式是只传递消费者处理所需的稳定字段；如果下游需要完整详情，应根据业务 ID 查询服务接口、数据库视图或数据同步结果。

对于核心业务消息，建议在生产者发送前进行基础校验。下面示例使用 Hutool 的 `Assert` 和 `StrUtil` 对消息体进行校验。

文件位置：`src/main/java/io/github/atengk/rocketmq/model/OrderEventMessageValidator.java`

```java
package io.github.atengk.rocketmq.model;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

import java.math.BigDecimal;

/**
 * 订单事件消息校验器
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class OrderEventMessageValidator {

    private OrderEventMessageValidator() {
    }

    /**
     * 校验订单事件消息
     *
     * @param message 订单事件消息
     */
    public static void validate(OrderEventMessage message) {
        Assert.notNull(message, "订单事件消息不能为空");
        Assert.notBlank(message.messageId(), "消息ID不能为空");
        Assert.notBlank(message.version(), "消息版本不能为空");
        Assert.notBlank(message.eventType(), "事件类型不能为空");
        Assert.notNull(message.eventTime(), "事件时间不能为空");
        Assert.notBlank(message.sourceSystem(), "来源系统不能为空");
        Assert.notBlank(message.traceId(), "链路追踪ID不能为空");
        Assert.notBlank(message.orderNo(), "订单号不能为空");

        if (StrUtil.equalsAny(message.eventType(), "order_paid", "order_refunded")) {
            Assert.notNull(message.amount(), "支付或退款消息金额不能为空");
            Assert.isTrue(message.amount().compareTo(BigDecimal.ZERO) >= 0, "消息金额不能小于0");
        }
    }
}
```

Topic、Tag 和消息体之间的推荐关系如下：

```text
Topic：order-event-normal-topic
  |
  +-- Tag：order_created
  |       |
  |       +-- Body：OrderEventMessage(eventType=order_created, orderNo, userId, eventTime...)
  |
  +-- Tag：order_paid
  |       |
  |       +-- Body：OrderEventMessage(eventType=order_paid, orderNo, userId, amount, eventTime...)
  |
  +-- Tag：order_cancelled
          |
          +-- Body：OrderEventMessage(eventType=order_cancelled, orderNo, cancelReason, eventTime...)
```

最终设计建议是：Topic 表达业务域和消息类型，Tag 表达事件类型，Key 表达业务唯一标识，Body 表达稳定业务数据，Properties 表达扩展元数据。只要这个边界清晰，后续消息发送、消费过滤、幂等处理、重试补偿、问题排查和版本升级都会更容易维护。

## 事务消息

本节用于说明 RocketMQ 事务消息的执行流程、本地事务执行方式、事务状态回查逻辑和使用边界。本节对应原文档大纲中的“事务消息”章节。

事务消息用于解决“本地事务执行成功，但消息发送失败”或“消息发送成功，但本地事务失败”这类一致性问题。RocketMQ 官方文档将事务消息定义为一种高级消息类型，用于保障消息生产和本地事务之间的最终一致性；它解决的是生产者本地核心事务与消息提交之间的一致性，不直接保证消费者业务一定执行成功。([RocketMQ](https://rocketmq.apache.org/zh/docs/featureBehavior/04transactionmessage/?utm_source=chatgpt.com))

### 事务消息执行流程

事务消息采用类似两阶段提交的机制。Producer 先发送半事务消息，Broker 持久化后暂不投递给消费者；Producer 再执行本地事务，并根据本地事务结果向 Broker 提交 Commit、Rollback 或 Unknown。Broker 收到 Commit 后才会将消息标记为可投递，收到 Rollback 后不会投递该消息；如果 Broker 没有收到二次确认，或者状态为 Unknown，会在固定时间后向 Producer 发起事务状态回查。([RocketMQ](https://rocketmq.apache.org/zh/docs/featureBehavior/04transactionmessage/?utm_source=chatgpt.com))

事务消息整体流程如下：

```text
Producer 发送半事务消息
        |
        v
Broker 持久化半消息，消息暂不可投递
        |
        v
Producer 执行本地事务
        |
        v
Producer 提交事务状态
        |
        +-- COMMIT   -> Broker 投递消息给 Consumer
        |
        +-- ROLLBACK -> Broker 回滚半消息，不投递
        |
        +-- UNKNOWN  -> Broker 后续发起事务状态回查
```

事务消息中的几个关键状态如下：

| 状态       | 说明                                                  |
| ---------- | ----------------------------------------------------- |
| 半事务消息 | 已发送到 Broker，但暂时不可投递给消费者。             |
| Commit     | 本地事务执行成功，Broker 将消息标记为可投递。         |
| Rollback   | 本地事务执行失败，Broker 回滚该消息，不投递给消费者。 |
| Unknown    | 本地事务状态暂时无法确认，等待后续事务回查。          |
| 事务回查   | Broker 向 Producer 查询本地事务最终状态。             |

RocketMQ 5.x 中，事务消息需要使用事务消息类型的 Topic。官方文档说明，事务消息只能用于 MessageType 为 Transaction 的 Topic，普通 Topic 不支持发送事务消息。([RocketMQ](https://rocketmq.apache.org/docs/featureBehavior/04transactionmessage/?utm_source=chatgpt.com))

本地或测试环境可以通过 `mqadmin` 创建事务消息 Topic。实际命令中的 NameServer 地址、Topic、集群名称需要按环境替换。

```bash
# 创建事务消息 Topic
sh mqadmin updateTopic \
  -n 127.0.0.1:9876 \
  -t order-transaction-topic \
  -c DefaultCluster \
  -a +message.type=Transaction
```

命令说明：`-n` 指定 NameServer 地址；`-t` 指定 Topic 名称；`-c` 指定 Broker 集群名称；`-a +message.type=Transaction` 用于声明该 Topic 支持事务消息。生产环境中 Topic 应由运维或消息平台统一创建，不建议业务应用自动创建。

### 本地事务执行

本地事务执行是事务消息的核心环节。Producer 发送半事务消息成功后，会进入本地事务逻辑，例如创建订单、更新支付流水、保存事务日志或修改业务状态。本地事务执行完成后，需要返回明确的事务状态。

Spring Boot 项目中，RocketMQ Spring 通过 `RocketMQTemplate.sendMessageInTransaction` 发送事务消息，并通过 `@RocketMQTransactionListener` 与 `RocketMQLocalTransactionListener` 执行本地事务和事务状态回查。RocketMQ Spring 官方 Wiki 示例也说明，需要实现 `executeLocalTransaction` 和 `checkLocalTransaction` 两个方法，分别处理本地事务执行和事务状态检查。([GitHub](https://github.com/apache/rocketmq-spring/wiki/Transactional-Message?utm_source=chatgpt.com))

示例文件结构如下：

```text
src/main/java/io/github/atengk/rocketmq/
├── constant/
│   └── OrderTransactionMqConstant.java
├── model/
│   └── OrderCreatedTransactionMessage.java
├── producer/
│   └── OrderTransactionProducer.java
├── service/
│   ├── OrderLocalTransactionService.java
│   └── impl/
│       └── OrderLocalTransactionServiceImpl.java
└── transaction/
    └── OrderTransactionMessageListener.java
```

先定义事务消息 Topic、Tag 和事务状态常量。

文件位置：`src/main/java/io/github/atengk/rocketmq/constant/OrderTransactionMqConstant.java`

```java
package io.github.atengk.rocketmq.constant;

/**
 * 订单事务消息常量
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class OrderTransactionMqConstant {

    /**
     * 订单事务消息 Topic
     */
    public static final String ORDER_TRANSACTION_TOPIC = "order-transaction-topic";

    /**
     * 订单创建事务消息 Tag
     */
    public static final String TAG_ORDER_CREATED = "order_created_transaction";

    /**
     * 本地事务执行中
     */
    public static final String STATE_PROCESSING = "PROCESSING";

    /**
     * 本地事务已提交
     */
    public static final String STATE_COMMIT = "COMMIT";

    /**
     * 本地事务已回滚
     */
    public static final String STATE_ROLLBACK = "ROLLBACK";

    private OrderTransactionMqConstant() {
    }

    /**
     * 构建发送目标
     *
     * @param topic Topic
     * @param tag   Tag
     * @return RocketMQ 发送目标
     */
    public static String destination(String topic, String tag) {
        return topic + ":" + tag;
    }
}
```

定义订单创建事务消息体。事务消息中必须包含可用于本地事务回查的业务唯一键，例如订单号、支付单号或事务流水号。

文件位置：`src/main/java/io/github/atengk/rocketmq/model/OrderCreatedTransactionMessage.java`

```java
package io.github.atengk.rocketmq.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单创建事务消息
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderCreatedTransactionMessage(
        String messageId,
        String orderNo,
        String userId,
        BigDecimal orderAmount,
        LocalDateTime orderTime,
        String traceId
) {
}
```

事务消息发送组件负责构造消息、设置业务 Key、发送半事务消息。这里使用订单号作为 Key 和事务参数，后续本地事务执行与回查都以订单号作为判断依据。

文件位置：`src/main/java/io/github/atengk/rocketmq/producer/OrderTransactionProducer.java`

```java
package io.github.atengk.rocketmq.producer;

import cn.hutool.core.lang.Assert;
import cn.hutool.json.JSONUtil;
import io.github.atengk.rocketmq.constant.OrderTransactionMqConstant;
import io.github.atengk.rocketmq.model.OrderCreatedTransactionMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 订单事务消息生产者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTransactionProducer {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 发送订单创建事务消息
     *
     * @param message 订单创建事务消息
     * @return 事务发送结果
     */
    public TransactionSendResult sendOrderCreatedTransactionMessage(OrderCreatedTransactionMessage message) {
        checkMessage(message);

        String destination = OrderTransactionMqConstant.destination(
                OrderTransactionMqConstant.ORDER_TRANSACTION_TOPIC,
                OrderTransactionMqConstant.TAG_ORDER_CREATED
        );

        Message<String> rocketMessage = MessageBuilder.withPayload(JSONUtil.toJsonStr(message))
                // RocketMQ 消息 Key，事务消息必须设置稳定业务键
                .setHeader(RocketMQHeaders.KEYS, message.orderNo())
                // 订单号用于本地事务执行和事务状态回查
                .setHeader("orderNo", message.orderNo())
                // 业务消息 ID，用于日志串联
                .setHeader("messageId", message.messageId())
                // 链路追踪 ID，用于跨系统排查
                .setHeader("traceId", message.traceId())
                .build();

        TransactionSendResult sendResult = rocketMQTemplate.sendMessageInTransaction(
                destination,
                rocketMessage,
                message.orderNo()
        );

        log.info("订单创建事务消息发送完成，topic={}，tag={}，orderNo={}，messageId={}，localTransactionState={}",
                OrderTransactionMqConstant.ORDER_TRANSACTION_TOPIC,
                OrderTransactionMqConstant.TAG_ORDER_CREATED,
                message.orderNo(),
                message.messageId(),
                sendResult.getLocalTransactionState());

        return sendResult;
    }

    /**
     * 校验订单事务消息
     *
     * @param message 订单事务消息
     */
    private void checkMessage(OrderCreatedTransactionMessage message) {
        Assert.notNull(message, "订单创建事务消息不能为空");
        Assert.notBlank(message.messageId(), "消息ID不能为空");
        Assert.notBlank(message.orderNo(), "订单号不能为空");
        Assert.notBlank(message.userId(), "用户ID不能为空");
        Assert.notNull(message.orderAmount(), "订单金额不能为空");
        Assert.notNull(message.orderTime(), "订单时间不能为空");
    }
}
```

本地事务状态不能只放在内存中。生产环境应使用数据库业务表、事务日志表或消息发送记录表作为事务状态判断依据。下面给出一个最小事务日志表示例。

文件位置：`db/order_transaction_log.sql`

```sql
-- 订单事务日志表：用于事务消息本地事务执行和事务状态回查
CREATE TABLE order_transaction_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    transaction_id VARCHAR(64) NOT NULL COMMENT '事务ID，通常使用订单号或业务流水号',
    message_id VARCHAR(64) NOT NULL COMMENT '业务消息ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    transaction_state VARCHAR(32) NOT NULL COMMENT '事务状态：PROCESSING、COMMIT、ROLLBACK',
    error_message VARCHAR(512) DEFAULT NULL COMMENT '异常信息',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_transaction_id (transaction_id),
    KEY idx_order_no (order_no)
) COMMENT='订单事务消息本地事务日志表';
```

定义本地事务服务接口，用于执行本地事务和查询事务状态。

文件位置：`src/main/java/io/github/atengk/rocketmq/service/OrderLocalTransactionService.java`

```java
package io.github.atengk.rocketmq.service;

import io.github.atengk.rocketmq.model.OrderCreatedTransactionMessage;

/**
 * 订单本地事务服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface OrderLocalTransactionService {

    /**
     * 执行订单创建本地事务
     *
     * @param message 订单创建事务消息
     * @return 本地事务状态
     */
    String executeCreateOrderTransaction(OrderCreatedTransactionMessage message);

    /**
     * 查询本地事务状态
     *
     * @param orderNo 订单号
     * @return 本地事务状态
     */
    String getTransactionState(String orderNo);
}
```

下面使用 `JdbcTemplate` 演示本地事务执行逻辑。真实项目中可以替换为 MyBatis-Plus、JPA 或自研仓储层，但核心原则不变：本地事务中必须同时完成业务数据写入和事务日志写入。

文件位置：`src/main/java/io/github/atengk/rocketmq/service/impl/OrderLocalTransactionServiceImpl.java`

```java
package io.github.atengk.rocketmq.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.rocketmq.constant.OrderTransactionMqConstant;
import io.github.atengk.rocketmq.model.OrderCreatedTransactionMessage;
import io.github.atengk.rocketmq.service.OrderLocalTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 订单本地事务服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderLocalTransactionServiceImpl implements OrderLocalTransactionService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 执行订单创建本地事务
     *
     * @param message 订单创建事务消息
     * @return 本地事务状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String executeCreateOrderTransaction(OrderCreatedTransactionMessage message) {
        String orderNo = message.orderNo();
        LocalDateTime now = LocalDateTime.now();

        try {
            log.info("开始执行订单创建本地事务，orderNo={}，messageId={}", orderNo, message.messageId());

            insertTransactionLog(message, OrderTransactionMqConstant.STATE_PROCESSING, null, now);

            // 示例：真实项目中这里应写入订单表、订单明细表、库存预占表等业务数据
            // 如果任一业务写入失败，整个本地事务应回滚
            if (StrUtil.containsIgnoreCase(orderNo, "ROLLBACK")) {
                throw new IllegalStateException("模拟订单创建本地事务失败");
            }

            updateTransactionState(orderNo, OrderTransactionMqConstant.STATE_COMMIT, null);

            log.info("订单创建本地事务执行成功，orderNo={}，messageId={}", orderNo, message.messageId());
            return OrderTransactionMqConstant.STATE_COMMIT;
        } catch (Exception exception) {
            log.error("订单创建本地事务执行失败，orderNo={}，messageId={}", orderNo, message.messageId(), exception);

            // 这里的状态更新仅用于示例。实际项目中，如果事务整体回滚，需要使用独立事务记录失败状态，或通过业务表缺失判断为回滚。
            throw exception;
        }
    }

    /**
     * 查询本地事务状态
     *
     * @param orderNo 订单号
     * @return 本地事务状态
     */
    @Override
    public String getTransactionState(String orderNo) {
        String sql = "SELECT transaction_state FROM order_transaction_log WHERE order_no = ? LIMIT 1";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, orderNo);
        } catch (Exception exception) {
            log.warn("未查询到订单本地事务状态，orderNo={}", orderNo);
            return null;
        }
    }

    /**
     * 写入事务日志
     *
     * @param message          订单创建事务消息
     * @param transactionState 事务状态
     * @param errorMessage     异常信息
     * @param now              当前时间
     */
    private void insertTransactionLog(OrderCreatedTransactionMessage message,
                                      String transactionState,
                                      String errorMessage,
                                      LocalDateTime now) {
        String sql = """
                INSERT INTO order_transaction_log
                (transaction_id, message_id, order_no, transaction_state, error_message, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(sql,
                message.orderNo(),
                message.messageId(),
                message.orderNo(),
                transactionState,
                errorMessage,
                LocalDateTimeUtil.formatNormal(now),
                LocalDateTimeUtil.formatNormal(now));
    }

    /**
     * 更新事务状态
     *
     * @param orderNo           订单号
     * @param transactionState  事务状态
     * @param errorMessage      异常信息
     */
    private void updateTransactionState(String orderNo, String transactionState, String errorMessage) {
        String sql = """
                UPDATE order_transaction_log
                SET transaction_state = ?, error_message = ?, updated_at = ?
                WHERE order_no = ?
                """;

        jdbcTemplate.update(sql,
                transactionState,
                errorMessage,
                LocalDateTimeUtil.formatNormal(LocalDateTime.now()),
                orderNo);
    }
}
```

上面的示例中，如果本地事务抛出异常，Spring 会回滚事务。生产项目中建议额外设计可靠的事务日志策略，例如使用独立事务记录失败状态，或者通过业务主表是否存在、业务状态是否已提交来判断事务状态。不能只依赖 Java 内存变量判断事务结果。

### 事务状态回查

事务状态回查用于处理 Producer 提交二次确认失败、网络中断、应用重启或本地事务返回 Unknown 的情况。Broker 会向 Producer 发起回查，Producer 应根据本地可靠数据源判断事务最终状态，并返回 Commit、Rollback 或 Unknown。官方文档明确说明，回查时应检查半消息对应的本地事务执行结果，再向 Broker 返回新的确认结果。([RocketMQ](https://rocketmq.apache.org/zh/docs/featureBehavior/04transactionmessage/?utm_source=chatgpt.com))

下面的监听器实现本地事务执行和事务状态回查。`executeLocalTransaction` 中执行本地事务；`checkLocalTransaction` 中根据订单号查询事务日志表，判断是否提交或回滚。

文件位置：`src/main/java/io/github/atengk/rocketmq/transaction/OrderTransactionMessageListener.java`

```java
package io.github.atengk.rocketmq.transaction;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.rocketmq.constant.OrderTransactionMqConstant;
import io.github.atengk.rocketmq.model.OrderCreatedTransactionMessage;
import io.github.atengk.rocketmq.service.OrderLocalTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * 订单事务消息监听器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQTransactionListener
public class OrderTransactionMessageListener implements RocketMQLocalTransactionListener {

    private final OrderLocalTransactionService orderLocalTransactionService;

    /**
     * 执行本地事务
     *
     * @param message 事务消息
     * @param arg     事务参数
     * @return 本地事务状态
     */
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message message, Object arg) {
        String orderNo = String.valueOf(arg);

        try {
            String payload = String.valueOf(message.getPayload());
            OrderCreatedTransactionMessage transactionMessage = JSONUtil.toBean(payload, OrderCreatedTransactionMessage.class);

            log.info("开始执行订单事务消息本地事务，orderNo={}，messageId={}",
                    orderNo, transactionMessage.messageId());

            String transactionState = orderLocalTransactionService.executeCreateOrderTransaction(transactionMessage);
            if (StrUtil.equals(transactionState, OrderTransactionMqConstant.STATE_COMMIT)) {
                log.info("订单事务消息本地事务提交，orderNo={}，messageId={}",
                        orderNo, transactionMessage.messageId());
                return RocketMQLocalTransactionState.COMMIT;
            }

            if (StrUtil.equals(transactionState, OrderTransactionMqConstant.STATE_ROLLBACK)) {
                log.warn("订单事务消息本地事务回滚，orderNo={}，messageId={}",
                        orderNo, transactionMessage.messageId());
                return RocketMQLocalTransactionState.ROLLBACK;
            }

            log.warn("订单事务消息本地事务状态未知，orderNo={}，transactionState={}", orderNo, transactionState);
            return RocketMQLocalTransactionState.UNKNOWN;
        } catch (Exception exception) {
            log.error("订单事务消息本地事务执行异常，orderNo={}", orderNo, exception);

            // 发生未知异常时，不建议盲目返回 COMMIT；交给回查根据本地可靠数据源判断
            return RocketMQLocalTransactionState.UNKNOWN;
        }
    }

    /**
     * 回查本地事务状态
     *
     * @param message 事务消息
     * @return 本地事务状态
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message message) {
        Object orderNoHeader = message.getHeaders().get("orderNo");
        String orderNo = orderNoHeader == null ? null : String.valueOf(orderNoHeader);

        if (StrUtil.isBlank(orderNo)) {
            log.error("订单事务消息回查失败，消息头缺少订单号");
            return RocketMQLocalTransactionState.UNKNOWN;
        }

        String transactionState = orderLocalTransactionService.getTransactionState(orderNo);
        log.info("订单事务消息状态回查，orderNo={}，transactionState={}", orderNo, transactionState);

        if (StrUtil.equals(transactionState, OrderTransactionMqConstant.STATE_COMMIT)) {
            return RocketMQLocalTransactionState.COMMIT;
        }

        if (StrUtil.equals(transactionState, OrderTransactionMqConstant.STATE_ROLLBACK)) {
            return RocketMQLocalTransactionState.ROLLBACK;
        }

        // 本地事务仍在执行或状态暂不可判断时，继续返回 UNKNOWN
        return RocketMQLocalTransactionState.UNKNOWN;
    }
}
```

事务回查必须遵循以下原则：

| 原则               | 说明                                                      |
| ------------------ | --------------------------------------------------------- |
| 以本地可靠数据为准 | 回查必须查询数据库业务表、事务日志表或消息发送记录表。    |
| 不依赖内存         | 应用重启后内存状态会丢失，不能作为事务结果依据。          |
| 处理中返回 Unknown | 如果本地事务仍在执行，不要提前返回 Commit 或 Rollback。   |
| 成功才 Commit      | 只有确认本地事务已经提交成功，才能返回 Commit。           |
| 失败才 Rollback    | 只有确认本地事务已失败或业务数据不存在，才返回 Rollback。 |
| 记录回查日志       | 回查日志必须包含业务键、消息 ID、事务状态和异常信息。     |

RocketMQ 官方文档也提醒，事务回查时不要对仍在执行中的事务直接返回 Rollback 或 Commit，而应继续保持 Unknown；大量 Unknown 和频繁回查会影响系统性能，因此生产者应尽量避免大量本地事务长期处于未知状态。([RocketMQ](https://rocketmq.apache.org/docs/featureBehavior/04transactionmessage/?utm_source=chatgpt.com))

本地验证事务消息时，可以通过接口触发不同结果。下面的接口只适合开发环境使用。

文件位置：`src/main/java/io/github/atengk/rocketmq/controller/OrderTransactionTestController.java`

```java
package io.github.atengk.rocketmq.controller;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.rocketmq.model.OrderCreatedTransactionMessage;
import io.github.atengk.rocketmq.producer.OrderTransactionProducer;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单事务消息测试接口
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequiredArgsConstructor
public class OrderTransactionTestController {

    private final OrderTransactionProducer orderTransactionProducer;

    /**
     * 发送订单创建事务消息
     *
     * @param flag 测试标记，NORMAL 或 ROLLBACK
     * @return 事务消息发送结果
     */
    @GetMapping("/test/rocketmq/transaction/order/{flag}")
    public String sendOrderTransaction(@PathVariable String flag) {
        String orderNo = "ORDER_" + flag + "_" + System.currentTimeMillis();

        OrderCreatedTransactionMessage message = new OrderCreatedTransactionMessage(
                IdUtil.fastSimpleUUID(),
                orderNo,
                "USER10001",
                new BigDecimal("99.90"),
                LocalDateTime.now(),
                IdUtil.fastSimpleUUID()
        );

        TransactionSendResult sendResult = orderTransactionProducer.sendOrderCreatedTransactionMessage(message);
        return "事务消息发送完成：" + sendResult.getLocalTransactionState();
    }
}
```

验证命令如下：

```bash
# 启动 Spring Boot 服务
mvn spring-boot:run

# 正常提交本地事务，Broker 后续可投递消息
curl "http://127.0.0.1:8088/test/rocketmq/transaction/order/NORMAL"

# 模拟本地事务异常，等待事务回查判断最终状态
curl "http://127.0.0.1:8088/test/rocketmq/transaction/order/ROLLBACK"
```

命令说明：`NORMAL` 会模拟本地事务成功，最终应返回 Commit；`ROLLBACK` 会模拟本地事务失败，示例中会先返回 Unknown，后续由事务回查根据本地事务日志或业务表判断最终结果。生产环境中应通过业务状态和事务日志表进行可靠判断。

### 事务消息使用边界

事务消息只适合解决“本地事务与消息发送之间的最终一致性”，不能把它理解为完整的分布式事务框架。RocketMQ 官方文档明确说明，事务消息保证的是本地核心事务与下游分支之间的最终一致性，但不保证消息消费结果与上游执行结果一致；下游消费者仍需要通过消费重试、幂等和补偿机制保证处理正确。([RocketMQ](https://rocketmq.apache.org/docs/featureBehavior/04transactionmessage/?utm_source=chatgpt.com))

适合使用事务消息的场景：

| 场景                 | 说明                                               |
| -------------------- | -------------------------------------------------- |
| 订单创建后发布事件   | 订单本地事务成功后，通知库存、优惠券、营销等系统。 |
| 支付成功后发布事件   | 支付流水本地事务成功后，通知订单系统更新状态。     |
| 账户变更后发布事件   | 账户余额或积分变更成功后，通知下游系统同步。       |
| 本地事务与消息强关联 | 本地数据提交成功是消息投递的前提。                 |

不适合使用事务消息的场景：

| 场景                         | 原因                                               |
| ---------------------------- | -------------------------------------------------- |
| 希望同步等待所有下游成功     | 事务消息是异步最终一致，不保证消费者立即处理成功。 |
| 消费端必须和生产端同事务提交 | RocketMQ 事务消息不覆盖消费者本地事务。            |
| 长时间本地事务               | 本地事务过长会导致大量半消息和频繁回查。           |
| 只需要普通异步通知           | 普通消息即可，不需要增加事务消息复杂度。           |
| 用来替代幂等                 | 事务消息不能避免重复消费，消费者仍必须幂等。       |
| 用来替代补偿任务             | 异常、死信、超时仍需要补偿和告警机制。             |

事务消息设计建议如下：

| 设计项       | 建议                                                     |
| ------------ | -------------------------------------------------------- |
| Topic 类型   | 事务消息使用独立 Transaction Topic，不与普通消息混用。   |
| 业务键       | 必须设置稳定业务 Key，例如订单号、支付单号、流水号。     |
| 事务日志     | 核心业务必须有事务日志表或消息发送记录表。               |
| 回查依据     | 回查必须依赖数据库可靠状态，不依赖内存。                 |
| Unknown 控制 | 避免大量事务长期 Unknown，防止回查压力过大。             |
| 消费幂等     | 消费端必须做幂等处理，因为 Commit 后消息仍可能重复投递。 |
| 死信补偿     | 下游消费失败进入死信后，需要人工或自动补偿流程。         |
| 监控告警     | 需要监控事务回查次数、Unknown 数量、发送失败、死信消息。 |

事务消息的推荐落地模式如下：

```text
业务接口接收请求
        |
        v
发送事务消息半消息
        |
        v
执行本地数据库事务
        |
        +-- 成功：写业务表 + 写事务日志 COMMIT
        |
        +-- 失败：事务回滚或写事务日志 ROLLBACK
        |
        v
事务监听器返回 COMMIT / ROLLBACK / UNKNOWN
        |
        v
Broker 根据事务状态决定是否投递消息
        |
        v
Consumer 消费消息，并基于业务键做幂等
```

最终原则是：事务消息只负责“本地事务成功后，消息最终可被投递”；消费者是否成功处理，需要依赖消费重试、幂等表、业务状态机、死信队列和补偿任务共同保证。

## 顺序消息

本节用于说明 RocketMQ 顺序消息的适用场景、MessageQueue 选择策略、分区顺序保证和顺序消费注意事项。本节对应原文档大纲中的“顺序消息”章节。

顺序消息也称 FIFO 消息，核心目标是保证同一业务分组内的消息按照发送顺序进行存储和消费。RocketMQ 官方文档说明，顺序消息通过 ShardingKey 或 MessageGroup 将同一分组的消息路由到相同队列中，从而保证该分组内消息按 FIFO 顺序处理；不同分组之间不保证顺序。([RocketMQ](https://rocketmq.apache.org/docs/4.x/producer/03message2/?utm_source=chatgpt.com))

在 Spring Boot 项目中，RocketMQ Spring 支持发送顺序消息和消费顺序消息，通常使用 `RocketMQTemplate.syncSendOrderly` 发送，并在消费者上配置 `consumeMode = ConsumeMode.ORDERLY`。([GitHub](https://github.com/apache/rocketmq-spring?utm_source=chatgpt.com))

### 顺序消息适用场景

顺序消息适用于同一个业务对象存在严格状态流转要求的场景。典型特点是：同一业务键下的多条消息必须按顺序执行，但不同业务键之间可以并发处理。

常见适用场景如下：

| 场景         | 顺序键                | 顺序要求                                           |
| ------------ | --------------------- | -------------------------------------------------- |
| 订单状态流转 | 订单号 `orderNo`      | 同一订单必须按创建、支付、发货、完成顺序处理。     |
| 支付流水处理 | 支付单号 `paymentNo`  | 同一支付单的创建、成功、退款、关闭需要按顺序处理。 |
| 账户资金变更 | 账户号 `accountNo`    | 同一账户的入账、出账、冻结、解冻需要按顺序处理。   |
| 库存变更     | 商品 SKU `skuCode`    | 同一 SKU 的扣减、释放、回补需要按顺序处理。        |
| 审批流程     | 业务单号 `businessNo` | 同一单据的提交、审批、驳回、归档需要按顺序处理。   |
| 增量数据同步 | 数据主键 `dataId`     | 同一数据对象的新增、修改、删除需要按变更顺序同步。 |

RocketMQ 官方文档中也将有序事件处理、交易撮合、实时增量数据同步列为顺序消息典型场景。对于数据同步场景，如果新增、修改、删除消息乱序到达，下游系统可能出现状态不一致；顺序消息可以保证同一分组内的变更按发送顺序处理。([RocketMQ](https://rocketmq.apache.org/docs/featureBehavior/03fifomessage/?utm_source=chatgpt.com))

不建议使用顺序消息的场景如下：

| 场景         | 原因                                                     |
| ------------ | -------------------------------------------------------- |
| 普通通知消息 | 不要求严格顺序，使用普通消息即可。                       |
| 日志采集     | 更关注吞吐量，不需要严格顺序。                           |
| 用户行为埋点 | 一般允许乱序或按时间聚合处理。                           |
| 全局顺序要求 | RocketMQ 更适合分区顺序，全局顺序会严重降低吞吐量。      |
| 耗时消费逻辑 | 顺序消费中单条消息失败或耗时过长，会阻塞同队列后续消息。 |

顺序消息设计时要先明确“谁需要顺序”。如果只是同一个订单需要顺序，则使用订单号作为顺序键；如果同一个用户下的所有订单都要顺序，则使用用户 ID 作为顺序键。顺序键粒度越粗，单个队列压力越大；顺序键粒度越细，并发能力越好。

### MessageQueue 选择策略

RocketMQ 的 Topic 由多个 MessageQueue 组成。队列是 RocketMQ 消息存储和传输的最小容器，Topic 通过多个队列实现水平分区和扩展；队列内部天然具有有序存储特性，消息会按照进入队列的顺序保存，并通过 offset 标识位置。([RocketMQ](https://rocketmq.apache.org/docs/domainModel/03messagequeue/?utm_source=chatgpt.com))

顺序消息的关键是让同一业务分组的消息进入同一个 MessageQueue。普通消息通常会被负载均衡到不同队列中，如果同一个订单的多条消息被发送到不同队列，就无法保证它们之间的消费顺序。顺序消息通过 hashKey、ShardingKey 或 MessageGroup 选择队列，使同一业务键下的消息进入同一个队列。([RocketMQ](https://rocketmq.apache.org/docs/4.x/producer/03message2/?utm_source=chatgpt.com))

推荐的 MessageQueue 选择策略如下：

| 策略           | 说明                                   | 示例         |
| -------------- | -------------------------------------- | ------------ |
| 按订单号选择   | 同一订单所有状态消息进入同一个队列。   | `orderNo`    |
| 按支付单号选择 | 同一支付单所有事件进入同一个队列。     | `paymentNo`  |
| 按账户号选择   | 同一账户资金流水进入同一个队列。       | `accountNo`  |
| 按 SKU 选择    | 同一商品库存变更进入同一个队列。       | `skuCode`    |
| 按业务单号选择 | 同一审批单、工单、合同进入同一个队列。 | `businessNo` |

不推荐的选择策略如下：

| 策略                       | 问题                                             |
| -------------------------- | ------------------------------------------------ |
| 固定常量 hashKey           | 所有消息进入同一个队列，吞吐量低，容易积压。     |
| 随机 UUID                  | 同一业务对象无法稳定进入同一个队列，顺序失效。   |
| 用户昵称、手机号等可变字段 | 字段可能变化，不适合作为稳定顺序键。             |
| 时间戳                     | 无法表达业务对象分组，不能保证同一业务对象顺序。 |

下面定义顺序消息使用的常量和消息体。

文件位置：`src/main/java/io/github/atengk/rocketmq/constant/OrderFifoMqConstant.java`

```java
package io.github.atengk.rocketmq.constant;

/**
 * 订单顺序消息常量
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class OrderFifoMqConstant {

    /**
     * 订单状态顺序消息 Topic
     */
    public static final String ORDER_STATUS_FIFO_TOPIC = "order-status-fifo-topic";

    /**
     * 订单状态变更 Tag
     */
    public static final String TAG_ORDER_STATUS_CHANGED = "order_status_changed";

    /**
     * 订单顺序消费组
     */
    public static final String ORDER_STATUS_FIFO_CONSUMER_GROUP = "order-status-fifo-consumer-group";

    private OrderFifoMqConstant() {
    }

    /**
     * 构建发送目标
     *
     * @param topic Topic
     * @param tag   Tag
     * @return RocketMQ 发送目标
     */
    public static String destination(String topic, String tag) {
        return topic + ":" + tag;
    }
}
```

文件位置：`src/main/java/io/github/atengk/rocketmq/model/OrderStatusMessage.java`

```java
package io.github.atengk.rocketmq.model;

import java.time.LocalDateTime;

/**
 * 订单状态顺序消息
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderStatusMessage(
        String messageId,
        String orderNo,
        String userId,
        String orderStatus,
        Integer statusVersion,
        LocalDateTime eventTime,
        String traceId
) {
}
```

下面的生产者使用 `RocketMQTemplate.syncSendOrderly` 发送顺序消息。这里使用 `orderNo` 作为 hashKey，保证同一个订单的状态消息进入同一个队列。

文件位置：`src/main/java/io/github/atengk/rocketmq/producer/OrderFifoMessageProducer.java`

```java
package io.github.atengk.rocketmq.producer;

import cn.hutool.core.lang.Assert;
import cn.hutool.json.JSONUtil;
import io.github.atengk.rocketmq.constant.OrderFifoMqConstant;
import io.github.atengk.rocketmq.model.OrderStatusMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 订单顺序消息生产者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderFifoMessageProducer {

    private static final long SEND_TIMEOUT = 3000L;

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 发送订单状态顺序消息
     *
     * @param message 订单状态消息
     * @return 发送结果
     */
    public SendResult sendOrderStatusMessage(OrderStatusMessage message) {
        checkMessage(message);

        String destination = OrderFifoMqConstant.destination(
                OrderFifoMqConstant.ORDER_STATUS_FIFO_TOPIC,
                OrderFifoMqConstant.TAG_ORDER_STATUS_CHANGED
        );

        Message<String> rocketMessage = MessageBuilder.withPayload(JSONUtil.toJsonStr(message))
                // RocketMQ 消息 Key，用于消息检索和问题排查
                .setHeader(RocketMQHeaders.KEYS, message.orderNo())
                // 订单号作为业务顺序键
                .setHeader("orderNo", message.orderNo())
                // 状态版本，用于消费端状态机校验
                .setHeader("statusVersion", message.statusVersion())
                // 链路追踪 ID
                .setHeader("traceId", message.traceId())
                .build();

        // hashKey 使用订单号，保证同一订单的消息进入同一个 MessageQueue
        SendResult sendResult = rocketMQTemplate.syncSendOrderly(
                destination,
                rocketMessage,
                message.orderNo(),
                SEND_TIMEOUT
        );

        log.info("订单顺序消息发送成功，topic={}，tag={}，orderNo={}，orderStatus={}，statusVersion={}，sendStatus={}",
                OrderFifoMqConstant.ORDER_STATUS_FIFO_TOPIC,
                OrderFifoMqConstant.TAG_ORDER_STATUS_CHANGED,
                message.orderNo(),
                message.orderStatus(),
                message.statusVersion(),
                sendResult.getSendStatus());

        return sendResult;
    }

    /**
     * 校验订单状态消息
     *
     * @param message 订单状态消息
     */
    private void checkMessage(OrderStatusMessage message) {
        Assert.notNull(message, "订单状态消息不能为空");
        Assert.notBlank(message.messageId(), "消息ID不能为空");
        Assert.notBlank(message.orderNo(), "订单号不能为空");
        Assert.notBlank(message.userId(), "用户ID不能为空");
        Assert.notBlank(message.orderStatus(), "订单状态不能为空");
        Assert.notNull(message.statusVersion(), "状态版本不能为空");
        Assert.notNull(message.eventTime(), "事件时间不能为空");
    }
}
```

如果需要一次发送同一订单的多条状态消息，应确保这些消息使用同一个 `orderNo` 作为 hashKey，并且发送顺序与业务状态顺序一致。

文件位置：`src/main/java/io/github/atengk/rocketmq/controller/OrderFifoMessageTestController.java`

```java
package io.github.atengk.rocketmq.controller;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.rocketmq.model.OrderStatusMessage;
import io.github.atengk.rocketmq.producer.OrderFifoMessageProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 订单顺序消息测试接口
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequiredArgsConstructor
public class OrderFifoMessageTestController {

    private final OrderFifoMessageProducer orderFifoMessageProducer;

    /**
     * 发送同一订单的多条顺序消息
     *
     * @return 操作结果
     */
    @GetMapping("/test/rocketmq/orderly/order-status")
    public String sendOrderStatusMessages() {
        String orderNo = "ORDER" + System.currentTimeMillis();
        String userId = "USER10001";
        String traceId = IdUtil.fastSimpleUUID();

        orderFifoMessageProducer.sendOrderStatusMessage(buildMessage(orderNo, userId, "CREATED", 1, traceId));
        orderFifoMessageProducer.sendOrderStatusMessage(buildMessage(orderNo, userId, "PAID", 2, traceId));
        orderFifoMessageProducer.sendOrderStatusMessage(buildMessage(orderNo, userId, "DELIVERED", 3, traceId));
        orderFifoMessageProducer.sendOrderStatusMessage(buildMessage(orderNo, userId, "COMPLETED", 4, traceId));

        return "订单顺序消息发送完成，orderNo=" + orderNo;
    }

    /**
     * 构建订单状态消息
     *
     * @param orderNo       订单号
     * @param userId        用户ID
     * @param orderStatus   订单状态
     * @param statusVersion 状态版本
     * @param traceId       链路追踪ID
     * @return 订单状态消息
     */
    private OrderStatusMessage buildMessage(String orderNo,
                                            String userId,
                                            String orderStatus,
                                            Integer statusVersion,
                                            String traceId) {
        return new OrderStatusMessage(
                IdUtil.fastSimpleUUID(),
                orderNo,
                userId,
                orderStatus,
                statusVersion,
                LocalDateTime.now(),
                traceId
        );
    }
}
```

RocketMQ 5.x 中，顺序消息应使用 FIFO 类型 Topic。官方文档说明，顺序消息仅支持 MessageType 为 FIFO 的 Topic；创建 Topic 时需要声明消息类型。([RocketMQ](https://rocketmq.apache.org/docs/featureBehavior/03fifomessage/?utm_source=chatgpt.com))

```bash
# 创建 FIFO 顺序消息 Topic
sh mqadmin updateTopic \
  -n 127.0.0.1:9876 \
  -t order-status-fifo-topic \
  -c DefaultCluster \
  -a +message.type=FIFO

# 创建支持顺序消费的消费组
sh mqadmin updateSubGroup \
  -n 127.0.0.1:9876 \
  -c DefaultCluster \
  -g order-status-fifo-consumer-group \
  -o true
```

命令说明：`-n` 指定 NameServer 地址；`-t` 指定 Topic 名称；`-c` 指定集群名称；`-g` 指定消费组；`-a +message.type=FIFO` 用于创建 FIFO 类型 Topic；`-o true` 表示创建支持顺序语义的订阅组。生产环境中 Topic 和消费组应由平台统一创建，不建议业务应用自动创建。

### 分区顺序保证

RocketMQ 顺序消息通常保证的是分区顺序，而不是全局顺序。也就是说，同一个业务分组内的消息有序，不同业务分组之间不保证相对顺序。官方文档说明，RocketMQ 使用 MessageGroup 决定顺序范围，同一 MessageGroup 内的消息按 FIFO 处理；不同 MessageGroup 之间不适用顺序保证。([RocketMQ](https://rocketmq.apache.org/docs/featureBehavior/03fifomessage/?utm_source=chatgpt.com))

例如以下订单状态消息：

```text
订单 A：CREATED -> PAID -> DELIVERED -> COMPLETED
订单 B：CREATED -> PAID -> CANCELLED
```

如果使用订单号作为 hashKey，则订单 A 的消息会进入同一个队列，订单 B 的消息也会进入某个固定队列。RocketMQ 需要保证的是：

```text
订单 A 内部顺序：CREATED -> PAID -> DELIVERED -> COMPLETED
订单 B 内部顺序：CREATED -> PAID -> CANCELLED
```

但不保证订单 A 和订单 B 之间的全局顺序。例如实际消费顺序可能是：

```text
订单 A：CREATED
订单 B：CREATED
订单 A：PAID
订单 A：DELIVERED
订单 B：PAID
订单 A：COMPLETED
订单 B：CANCELLED
```

这种结果是正常的，因为不同订单之间没有顺序依赖。

分区顺序需要同时满足发送端和消费端条件。RocketMQ 官方文档说明，生产顺序要求单个 Producer 串行发送；如果多个 Producer 或多个线程并发发送，即使配置了相同 MessageGroup，RocketMQ 也无法判断不同线程或不同系统之间的先后顺序。消费顺序要求消费者按照“接收、处理、提交结果”的路径执行，不能在消息未处理完成前提前返回成功。([RocketMQ](https://rocketmq.apache.org/docs/featureBehavior/03fifomessage/?utm_source=chatgpt.com))

分区顺序保证条件如下：

| 环节     | 要求                                                         |
| -------- | ------------------------------------------------------------ |
| Topic    | 使用顺序消息 Topic，RocketMQ 5.x 中应使用 FIFO 类型 Topic。  |
| 顺序键   | 同一业务对象必须使用相同 hashKey、ShardingKey 或 MessageGroup。 |
| 发送端   | 同一业务对象的消息应按业务顺序串行发送。                     |
| 队列选择 | 同一顺序键的消息必须进入同一个 MessageQueue。                |
| 消费端   | 使用顺序消费模式，不使用并发消费处理同一队列消息。           |
| 业务处理 | 消费完成后再提交消费结果，不能提前 ack。                     |
| 幂等控制 | 顺序消息仍可能重复投递，消费端必须幂等。                     |

顺序消息的常见错误设计如下：

| 错误设计                         | 问题                                                         |
| -------------------------------- | ------------------------------------------------------------ |
| 使用随机 hashKey                 | 同一订单消息进入不同队列，顺序失效。                         |
| 所有消息使用同一个 hashKey       | 所有消息进入同一队列，吞吐量严重下降。                       |
| 发送端多线程并发发送同一订单消息 | RocketMQ 无法判断并发线程中的真实业务顺序。                  |
| 消费端使用并发消费模式           | 队列内消息可能被并发处理，业务顺序无法保证。                 |
| 消费端先提交成功再异步处理       | RocketMQ 认为消息已成功消费，后续异步失败无法触发重试。      |
| 消费端缺少状态校验               | 即使 MQ 保证顺序，也可能因补偿、重放、人工修复导致业务状态异常。 |

为了增强业务侧顺序可靠性，建议消息体中增加 `statusVersion` 或 `eventVersion` 字段。消费端收到消息后，不仅依赖 MQ 顺序，还要校验当前业务状态是否允许流转。

示例状态机校验规则：

```text
当前状态：INIT
  |
  +-- 允许 CREATED

当前状态：CREATED
  |
  +-- 允许 PAID
  +-- 允许 CANCELLED

当前状态：PAID
  |
  +-- 允许 DELIVERED
  +-- 允许 REFUNDED

当前状态：DELIVERED
  |
  +-- 允许 COMPLETED
```

### 顺序消费注意事项

顺序消费需要消费者使用 `ConsumeMode.ORDERLY`。RocketMQ 官方文档说明，如果顺序消息发生消费失败或超时，会触发服务端重试；对于顺序消息，失败消息后面的消息只有在该失败消息消费成功后才能继续处理。([RocketMQ](https://rocketmq.apache.org/docs/featureBehavior/03fifomessage/?utm_source=chatgpt.com))

下面是订单顺序消息消费者示例。该消费者使用 `MessageExt` 获取 Topic、Tag、Key、队列 ID、队列 offset 和重试次数，便于日志排查。幂等服务可以复用前面“消费幂等处理”章节中的 `MessageIdempotentService`。

文件位置：`src/main/java/io/github/atengk/rocketmq/consumer/OrderFifoMessageConsumer.java`

```java
package io.github.atengk.rocketmq.consumer;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.rocketmq.constant.OrderFifoMqConstant;
import io.github.atengk.rocketmq.model.OrderStatusMessage;
import io.github.atengk.rocketmq.service.MessageIdempotentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 订单顺序消息消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = OrderFifoMqConstant.ORDER_STATUS_FIFO_TOPIC,
        consumerGroup = OrderFifoMqConstant.ORDER_STATUS_FIFO_CONSUMER_GROUP,
        selectorExpression = OrderFifoMqConstant.TAG_ORDER_STATUS_CHANGED,
        consumeMode = ConsumeMode.ORDERLY,
        messageModel = MessageModel.CLUSTERING
)
public class OrderFifoMessageConsumer implements RocketMQListener<MessageExt> {

    private final MessageIdempotentService messageIdempotentService;

    /**
     * 消费订单顺序消息
     *
     * @param messageExt RocketMQ 原始消息
     */
    @Override
    public void onMessage(MessageExt messageExt) {
        String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        if (StrUtil.isBlank(body)) {
            log.warn("订单顺序消息体为空，topic={}，tag={}，msgId={}",
                    messageExt.getTopic(), messageExt.getTags(), messageExt.getMsgId());
            return;
        }

        OrderStatusMessage message = JSONUtil.toBean(body, OrderStatusMessage.class);
        String idempotentKey = buildIdempotentKey(messageExt, message);

        if (!messageIdempotentService.tryConsume(idempotentKey)) {
            log.info("订单顺序消息重复消费，跳过处理，orderNo={}，orderStatus={}，statusVersion={}，msgId={}",
                    message.orderNo(), message.orderStatus(), message.statusVersion(), messageExt.getMsgId());
            return;
        }

        try {
            log.info("开始消费订单顺序消息，orderNo={}，orderStatus={}，statusVersion={}，queueId={}，queueOffset={}，reconsumeTimes={}",
                    message.orderNo(),
                    message.orderStatus(),
                    message.statusVersion(),
                    messageExt.getQueueId(),
                    messageExt.getQueueOffset(),
                    messageExt.getReconsumeTimes());

            // 这里编写真实订单状态流转逻辑
            // 建议先查询订单当前状态，再根据状态机判断是否允许流转
            checkOrderStatusFlow(message);

            // 示例：更新订单状态、写入订单状态流水、触发后续业务动作等

            messageIdempotentService.confirmConsumed(idempotentKey);
            log.info("订单顺序消息消费完成，orderNo={}，orderStatus={}，statusVersion={}，msgId={}",
                    message.orderNo(), message.orderStatus(), message.statusVersion(), messageExt.getMsgId());
        } catch (Exception exception) {
            messageIdempotentService.cancelConsume(idempotentKey);
            log.error("订单顺序消息消费失败，orderNo={}，orderStatus={}，statusVersion={}，queueId={}，queueOffset={}",
                    message.orderNo(),
                    message.orderStatus(),
                    message.statusVersion(),
                    messageExt.getQueueId(),
                    messageExt.getQueueOffset(),
                    exception);

            // 顺序消费失败后应抛出异常，让 RocketMQ 后续重试，避免跳过当前消息导致后续消息乱序处理
            throw exception;
        }
    }

    /**
     * 构建幂等键
     *
     * @param messageExt RocketMQ 原始消息
     * @param message    订单状态消息
     * @return 幂等键
     */
    private String buildIdempotentKey(MessageExt messageExt, OrderStatusMessage message) {
        return StrUtil.format("rocketmq:consume:fifo:{}:{}:{}:{}",
                messageExt.getTopic(),
                messageExt.getTags(),
                message.orderNo(),
                message.statusVersion());
    }

    /**
     * 校验订单状态流转
     *
     * @param message 订单状态消息
     */
    private void checkOrderStatusFlow(OrderStatusMessage message) {
        if (message.statusVersion() <= 0) {
            throw new IllegalArgumentException("订单状态版本不合法，orderNo=" + message.orderNo());
        }

        log.info("订单状态流转校验通过，orderNo={}，orderStatus={}，statusVersion={}",
                message.orderNo(), message.orderStatus(), message.statusVersion());
    }
}
```

顺序消费注意事项如下：

| 注意事项               | 说明                                                         |
| ---------------------- | ------------------------------------------------------------ |
| 使用 `ORDERLY`         | 消费者必须配置 `consumeMode = ConsumeMode.ORDERLY`。         |
| 不要提前返回成功       | 消息业务处理完成前，不要提交消费成功。                       |
| 不要异步转交后直接返回 | 将消息提交到本地线程池后立即返回成功，会破坏 RocketMQ 的重试语义。 |
| 控制消费耗时           | 单条消息处理时间过长会阻塞同队列后续消息。                   |
| 控制失败率             | 顺序消息失败会影响后续消息消费，应减少不稳定外部依赖。       |
| 保留幂等能力           | 顺序消息仍可能因重试、超时、重放产生重复消费。               |
| 保留状态机校验         | MQ 顺序不等于业务状态一定正确，消费端仍需校验业务状态。      |
| 合理设置重试次数       | 顺序消息无限重试会阻塞后续消息，需要告警和补偿流程。         |

本地验证命令如下：

```bash
# 启动 Spring Boot 服务
mvn spring-boot:run

# 发送同一订单的多条顺序消息
curl "http://127.0.0.1:8088/test/rocketmq/orderly/order-status"

# 查看应用日志，确认同一 orderNo 下的状态顺序
tail -f logs/rocketmq-demo-service.log
```

验证时重点观察以下字段：

```text
orderNo
orderStatus
statusVersion
queueId
queueOffset
reconsumeTimes
```

同一个 `orderNo` 的消息应进入同一个 `queueId`，并且 `statusVersion` 应按 `1 -> 2 -> 3 -> 4` 的顺序被消费。不同 `orderNo` 的消息可能分布在不同队列中，消费顺序不需要相互比较。

顺序消息最终落地原则是：发送端使用稳定业务键选择队列，消费端使用顺序消费模式处理消息，业务端使用状态机和幂等机制兜底。不要把顺序消息理解为全局串行队列，也不要依赖 MQ 顺序替代业务状态校验。

## 异常处理

本节用于说明 RocketMQ 开发中的异常处理方式，包括发送失败处理、消费失败处理、重试与死信队列、日志记录规范。本节对应原文档大纲中的“异常处理”章节。

RocketMQ 异常处理需要分为发送端和消费端两部分。发送端重点关注消息是否成功写入 Broker，消费端重点关注业务是否成功处理、失败是否应该重试、重复消费是否幂等、最终失败是否进入死信处理。RocketMQ 官方文档说明，发送重试不能保证失败消息最终一定发送成功，最终重试失败后仍需要调用方捕获异常并提供兜底保护；同时发送重试可能导致 Broker 上存在重复消息，因此业务逻辑必须能处理重复消息。([RocketMQ](https://rocketmq.apache.org/docs/featureBehavior/05sendretrypolicy/?utm_source=chatgpt.com))

### 发送失败处理

发送失败通常发生在 Producer 调用 Broker 发送消息时，常见原因包括网络异常、请求超时、Broker 重启、Broker 处理较慢、服务端返回错误码、Broker 限流等。RocketMQ 发送重试支持同步发送和异步发送；同步发送会阻塞调用线程直到重试成功或最终失败，异步发送会通过成功或异常回调返回结果。([RocketMQ](https://rocketmq.apache.org/docs/featureBehavior/05sendretrypolicy/?utm_source=chatgpt.com))

发送失败处理建议如下：

| 异常类型      | 处理方式                                                     |
| ------------- | ------------------------------------------------------------ |
| 网络超时      | 记录完整上下文，触发客户端内置重试，最终失败后进入补偿表。   |
| Broker 不可用 | 尝试其他 Broker，最终失败后告警。                            |
| Topic 不存在  | 属于配置或运维问题，应立即告警，不应无限重试。               |
| 权限失败      | 检查 ACL、AK/SK、Topic 权限，不能通过业务重试解决。          |
| 消息体过大    | 调整消息体设计，不应通过重试解决。                           |
| Broker 限流   | 降低发送速率、扩容 Broker 或优化消费者积压。                 |
| 事务消息异常  | 事务消息在网络异常或超时场景下不做普通发送重试，应通过事务回查和本地状态兜底。 |

生产项目中不建议业务代码直接散落调用 `RocketMQTemplate`。推荐封装统一发送组件，集中处理参数校验、消息构造、发送日志、异常捕获和失败补偿。

示例文件结构如下：

```text
src/main/java/io/github/atengk/rocketmq/
├── constant/
│   └── MqExceptionConstant.java
├── exception/
│   └── MqSendException.java
├── model/
│   └── MqSendFailureRecord.java
├── producer/
│   └── SafeRocketMqProducer.java
└── service/
    ├── MqSendFailureRecordService.java
    └── impl/
        └── MqSendFailureRecordServiceImpl.java
```

先定义异常处理相关常量。

文件位置：`src/main/java/io/github/atengk/rocketmq/constant/MqExceptionConstant.java`。推荐封装统一发送组件，集中处理参数校验、消息构造、发送日志、异常捕获和失败补偿。

示例文件结构如下：

```text
src/main/java/io/github/atengk/rocketmq/
├── constant/
│   └── MqExceptionConstant.java
├── exception/
│   └── MqSendException.java
├── model/
│   └── MqSendFailureRecord.java
├── producer/
│   └── SafeRocketMqProducer.java
└── service/
    ├── MqSendFailureRecordService.java
    └── impl/
        └── MqSendFailureRecordServiceImpl.java
```

先`

```java
package io.github.atengk.rocketmq.constant;

/**
 * RocketMQ 异常处理常量
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class MqExceptionConstant {

    /**
     * 消息发送失败状态
     */
    public static final String SEND_FAILED = "SEND_FAILED";

    /**
     * 消息发送成功状态
     */
    public static final String SEND_SUCCESS = "SEND_SUCCESS";

    /**
     * 消息待补偿状态
     */
    public static final String WAIT_COMPENSATE = "WAIT_COMPENSATE";

    /**
     * 发送失败记录默认最大重试次数
     */
    public static final int DEFAULT_MAX_RETRY_TIMES = 3;

    private MqExceptionConstant() {
    }
}
```

自定义发送异常，用于向业务层屏蔽 RocketMQ 底层异常细节，同时保留必要上下文。

文件位置：`src/main/java/io/github/atengk/rocketmq/exception/MqSendException.java`

```java
package io.github.atengk.rocketmq.exception;

import lombok.Getter;

/**
 * RocketMQ 消息发送异常
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Getter
public class MqSendException extends RuntimeException {

    private final String topic;

    private final String tag;

    private final String businessKey;

    /**
     * 创建消息发送异常
     *
     * @param topic       Topic
     * @param tag         Tag
     * @param businessKey 业务键
     * @param message     异常消息
     * @param cause       原始异常
     */
    public MqSendException(String topic, String tag, String businessKey, String message, Throwable cause) {
        super(message, cause);
        this.topic = topic;
        this.tag = tag;
        this.businessKey = businessKey;
    }
}
```

定义发送失败记录模型。实际项目中建议落库，用于后续补偿任务重新发送或人工处理。

文件位置：`src/main/java/io/github/atengk/rocketmq/model/MqSendFailureRecord.java`

```java
package io.github.atengk.rocketmq.model;

import java.time.LocalDateTime;

/**
 * RocketMQ 发送失败记录
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record MqSendFailureRecord(
        String messageId,
        String topic,
        String tag,
        String businessKey,
        String messageBody,
        String errorMessage,
        Integer retryTimes,
        String status,
        LocalDateTime createdTime
) {
}
```

定义发送失败记录服务接口。这里用于演示补偿入口，实际项目可以使用 MyBatis-Plus、JPA 或 JDBC 落库。

文件位置：`src/main/java/io/github/atengk/rocketmq/service/MqSendFailureRecordService.java`

```java
package io.github.atengk.rocketmq.service;

import io.github.atengk.rocketmq.model.MqSendFailureRecord;

/**
 * RocketMQ 发送失败记录服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface MqSendFailureRecordService {

    /**
     * 保存发送失败记录
     *
     * @param record 发送失败记录
     */
    void saveFailureRecord(MqSendFailureRecord record);
}
```

下面的实现仅记录日志。生产项目应替换为数据库持久化，并配合定时补偿任务处理。

文件位置：`src/main/java/io/github/atengk/rocketmq/service/impl/MqSendFailureRecordServiceImpl.java`

```java
package io.github.atengk.rocketmq.service.impl;

import cn.hutool.json.JSONUtil;
import io.github.atengk.rocketmq.model.MqSendFailureRecord;
import io.github.atengk.rocketmq.service.MqSendFailureRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * RocketMQ 发送失败记录服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
public class MqSendFailureRecordServiceImpl implements MqSendFailureRecordService {

    /**
     * 保存发送失败记录
     *
     * @param record 发送失败记录
     */
    @Override
    public void saveFailureRecord(MqSendFailureRecord record) {
        // 示例只打印日志，生产环境应保存到数据库补偿表
        log.error("保存RocketMQ发送失败记录，record={}", JSONUtil.toJsonStr(record));
    }
}
```

下面的安全发送组件封装同步发送和异步发送异常处理。同步发送最终失败后会保存失败记录并抛出业务异常；异步发送失败会在回调中保存失败记录并记录错误日志。

文件位置：`src/main/java/io/github/atengk/rocketmq/producer/SafeRocketMqProducer.java`

```java
package io.github.atengk.rocketmq.producer;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.rocketmq.constant.MqExceptionConstant;
import io.github.atengk.rocketmq.exception.MqSendException;
import io.github.atengk.rocketmq.model.MqSendFailureRecord;
import io.github.atengk.rocketmq.service.MqSendFailureRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * RocketMQ 安全发送组件
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SafeRocketMqProducer {

    private static final long DEFAULT_SEND_TIMEOUT = 3000L;

    private final RocketMQTemplate rocketMQTemplate;

    private final MqSendFailureRecordService mqSendFailureRecordService;

    /**
     * 同步发送消息
     *
     * @param topic       Topic
     * @param tag         Tag
     * @param businessKey 业务键
     * @param payload     消息体
     * @return 发送结果
     */
    public SendResult syncSend(String topic, String tag, String businessKey, Object payload) {
        checkSendParam(topic, businessKey, payload);

        String messageId = IdUtil.fastSimpleUUID();
        String destination = buildDestination(topic, tag);
        String messageBody = JSONUtil.toJsonStr(payload);
        Message<String> message = buildMessage(messageId, businessKey, messageBody);

        try {
            SendResult sendResult = rocketMQTemplate.syncSend(destination, message, DEFAULT_SEND_TIMEOUT);

            log.info("RocketMQ同步消息发送成功，topic={}，tag={}，businessKey={}，messageId={}，sendStatus={}，msgId={}",
                    topic, tag, businessKey, messageId, sendResult.getSendStatus(), sendResult.getMsgId());

            return sendResult;
        } catch (Exception exception) {
            log.error("RocketMQ同步消息发送失败，topic={}，tag={}，businessKey={}，messageId={}，messageBody={}",
                    topic, tag, businessKey, messageId, messageBody, exception);

            saveFailureRecord(messageId, topic, tag, businessKey, messageBody, exception);
            throw new MqSendException(topic, tag, businessKey, "RocketMQ同步消息发送失败", exception);
        }
    }

    /**
     * 异步发送消息
     *
     * @param topic       Topic
     * @param tag         Tag
     * @param businessKey 业务键
     * @param payload     消息体
     */
    public void asyncSend(String topic, String tag, String businessKey, Object payload) {
        checkSendParam(topic, businessKey, payload);

        String messageId = IdUtil.fastSimpleUUID();
        String destination = buildDestination(topic, tag);
        String messageBody = JSONUtil.toJsonStr(payload);
        Message<String> message = buildMessage(messageId, businessKey, messageBody);

        rocketMQTemplate.asyncSend(destination, message, new SendCallback() {

            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("RocketMQ异步消息发送成功，topic={}，tag={}，businessKey={}，messageId={}，sendStatus={}，msgId={}",
                        topic, tag, businessKey, messageId, sendResult.getSendStatus(), sendResult.getMsgId());
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("RocketMQ异步消息发送失败，topic={}，tag={}，businessKey={}，messageId={}，messageBody={}",
                        topic, tag, businessKey, messageId, messageBody, throwable);

                saveFailureRecord(messageId, topic, tag, businessKey, messageBody, throwable);
            }
        }, DEFAULT_SEND_TIMEOUT);
    }

    /**
     * 构建发送目标
     *
     * @param topic Topic
     * @param tag   Tag
     * @return 发送目标
     */
    private String buildDestination(String topic, String tag) {
        return StrUtil.isBlank(tag) ? topic : topic + ":" + tag;
    }

    /**
     * 构建消息
     *
     * @param messageId   消息ID
     * @param businessKey 业务键
     * @param messageBody 消息体
     * @return Spring 消息
     */
    private Message<String> buildMessage(String messageId, String businessKey, String messageBody) {
        return MessageBuilder.withPayload(messageBody)
                // RocketMQ 消息 Key，便于消息检索和故障排查
                .setHeader(RocketMQHeaders.KEYS, businessKey)
                // 业务消息 ID，便于生产者日志、消费者日志、补偿记录串联
                .setHeader("messageId", messageId)
                .build();
    }

    /**
     * 保存发送失败记录
     *
     * @param messageId   消息ID
     * @param topic       Topic
     * @param tag         Tag
     * @param businessKey 业务键
     * @param messageBody 消息体
     * @param throwable   异常
     */
    private void saveFailureRecord(String messageId,
                                   String topic,
                                   String tag,
                                   String businessKey,
                                   String messageBody,
                                   Throwable throwable) {
        MqSendFailureRecord record = new MqSendFailureRecord(
                messageId,
                topic,
                tag,
                businessKey,
                messageBody,
                StrUtil.maxLength(throwable.getMessage(), 500),
                0,
                MqExceptionConstant.WAIT_COMPENSATE,
                LocalDateTime.now()
        );

        mqSendFailureRecordService.saveFailureRecord(record);
    }

    /**
     * 校验发送参数
     *
     * @param topic       Topic
     * @param businessKey 业务键
     * @param payload     消息体
     */
    private void checkSendParam(String topic, String businessKey, Object payload) {
        Assert.notBlank(topic, "Topic不能为空");
        Assert.notBlank(businessKey, "业务键不能为空");
        Assert.notNull(payload, "消息体不能为空");
    }
}
```

发送失败处理原则如下：

| 原则             | 说明                                                         |
| ---------------- | ------------------------------------------------------------ |
| 核心消息必须兜底 | 订单、支付、库存、账([RocketMQ](https://rocketmq.apache.org/docs/featureBehavior/10consumerretrypolicy/?utm_source=chatgpt.com))核心消息可降级 |
| 不无限重试       | 客户端重试次数必须受控，最终失败交给补偿任务。               |
| 避免重复副作用   | 发送重试可能产生重复消息，消费者必须幂等。                   |
| 区分异常类型     | 配置错误、权限错误、消息体过大等不应依赖重试解决。           |
| 记录完整上下文   | 日志中必须包含 Topic、Tag、Key、messageId、异常原因。        |

### 消费失败处理

消费失败是指 Consumer 收到消息后，业务处理未能成功完成。常见原因包括消息体格式错误、业务参数缺失、数据库异常、下游服务不可用、消费超时、幂等锁冲突、业务状态不允许等。

RocketMQ 消费重试适用于业务处理失败且后续可能恢复的场景，例如下游状态暂时不可用、事务状态暂未返回、偶发异常等；不应将消费失败作为业务分流条件，也不应用消费失败来实现限流。citeturn223529search0

消费失败处理要先区分异常是否可恢复：

| 异常类型                     | 是否建议重试 | 处理建议                                                     |
| ---------------------------- | ------------ | ------------------------------------------------------------ |
| JSON 格式错误                | 否           | 记录异常消息，进入人工处理或异常消息表。                     |
| 必填字段缺失                 | 否           | 记录异常消息，避免无意义重试。                               |
| 业务对象不存在但可能稍后创建 | 是           | 抛出异常触发重试。                                           |
| 数据库短暂异常               | 是           | 抛出异常触发重试。                                           |
| 下游服务临时不可用           | 是           | 抛出异常触发重试。                                           |
| 业务状态不允许               | 视情况       | 如果是最终状态冲突，记录后返回成功；如果是状态延迟，触发重试。 |
| 幂等已处理                   | 否           | 直接返回成功，避免重复处理。                                 |

下面的消费者示例展示了可恢复异常和不可恢复异常的处理方式。不可恢复异常记录后返回成功，避免无意义重试；可恢复异常抛出，交给 RocketMQ 重试。

文件位置：`src/main/java/io/github/atengk/rocketmq/consumer/OrderExceptionConsumer.java`

~~~java
package io.github.atengk.rocketmq.consumer;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONUtil;
import io.github.atengk.rocketmq.model.OrderEventMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 订单异常消费示例
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "order-event-normal-topic",
        consumerGroup = "order-exception-consumer-group",
        selectorExpression = "order_paid",
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.CLUSTERING,
        maxReconsumeTimes = 5
)
public class OrderExceptionConsumer implements RocketMQListener<MessageExt> {

    /**
     * 消费订单消息
     *
     * @param messageExt RocketMQ 原始消息
     */
    @Override
    public void onMessage(MessageExt messageExt) {
        String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);

        try {
            OrderEventMessage message = parseMessage(messageExt, body);
            log.info("开始消费订单消息，topic={}，tag={}，keys={}，msgId={}，orderNo={}，reconsumeTimes={}",
                    messageExt.getTopic(),
                    messageExt.getTags(),
                    messageExt.getKeys(),
                    messageExt.getMsgId(),
                    message.orderNo(),
                    messageExt.getReconsumeTimes());

            handleBusiness(message);

            log.info("订单消息消费成功，topic={}，tag={}，keys={}，msgId={}，orderNo={}",
                    messageExt.getTopic(),
                    messageExt.getTags(),
                    messageExt.getKeys(),
                    messageExt.getMsgId(),
                    message.orderNo());
        } catch (IllegalArgumentException | JSONException exception) {
            // 不可恢复异常：消息格式或参数错误，重试没有意义
            log.error("订单消息不可恢复异常，跳过重试，topic={}，tag={}，keys={}，msgId={}，body={}",
                    messageExt.getTopic(),
                    messageExt.getTags(),
                    messageExt.getKeys(),
                    messageExt.getMsgId(),
                    body,
                    exception);

            // 这里建议保存异常消息表，供人工排查
        } catch (DataAccessException exception) {
            // 可恢复异常：数据库短暂异常，抛出后触发 RocketMQ 重试
            log.error("订单消息数据库异常，准备触发重试，topic={}，tag={}，keys={}，msgId={}，reconsumeTimes={}",
                    messageExt.getTopic(),
                    messageExt.getTags(),
                    messageExt.getKeys(),
                    messageExt.getMsgId(),
                    messageExt.getReconsumeTimes(),
                    exception);
            throw exception;
        } catch (Exception exception) {
            // 未知异常：默认按可恢复异常处理，避免误吞核心业务消息
            log.error("订单消息消费未知异常，准备触发重试，topic={}，tag={}，keys={}，msgId={}，reconsumeTimes={}",
                    messageExt.getTopic(),
                    messageExt.getTags(),
                    messageExt.getKeys(),
                    messageExt.getMsgId(),
                    messageExt.getReconsumeTimes(),
                    exception);
            throw exception;
        }
    }

    /**
     * 解析消息体
     *
     * @param messageExt RocketMQ 原始消息
     * @param body       消息体
     * @return 订单事件消息
     */
    private OrderEventMessage parseMessage(MessageExt messageExt, String body) {
        if (StrUtil.isBlank(body)) {
            throw new IllegalArgumentException("消息体为空，msgId=" + messageExt.getMsgId());
        }

        OrderEventMessage message = JSONUtil.toBean(body, OrderEventMessage.class);
        if (StrUtil.isBlank(message.orderNo())) {
            throw new IllegalArgumentException("订单号为空，msgId=" + messageExt.getMsgId());
        }

        return message;
    }

    /**
     * 处理业务逻辑
     *
     * @param message 订单事件消息
     */
    private void handleBusiness(OrderEventMessage message) {
        // 示例：真实项目中执行订单状态更新、流水生成、通知下游等业务逻辑
        if (StrUtil.containsIgnoreCase(message.orderNo(), "DB_ERROR")) {
            throw new org.springframework.dao.QueryTimeoutException("模拟数据库查询超时");
        }

        log.info("订单业务逻辑处理完成，orderNo={}，eventType={}", message.orderNo(), message.eventType());
 :contentReference[oaicite:4]{index=4}
| 原则 | 说明 |
| --- | --- |
| 不要吞异常 | 需要重试的异常必须抛出，让 RocketMQ 感知消费失败。 |
| 不要盲目重试 | 参数错误、格式错误、永久性业务错误不应重复重试。 |
| 不要提前成功 | 消息未处理完成前，不应返回消费成功。 |
| 不要异步丢给线程池后直接返回 | RocketMQ 会认为消费成功，后续异步失败无法触发重试。 |
| 保留幂等 | 消费失败后重试可能重复处理，必:contentReference[oaicite:5]{index=5}不可恢复异常应落库或进入异常消息处理流程。 |

### 重试与死信队列

RocketMQ 消费重试的作用是保证消费链路完整性。当 Consumer 返回失败、抛出异常、消费超时:contentReference[oaicite:6]{index=6}照消费重试策略重新投递消息；如果超过最大重试次数仍失败，消息会进入死信队列 DLQ。citeturn223529search0

PushConsumer 的消息状态包括 Ready、Inflight、WaitingRetry、Commit 和 DLQ。无序消息的重试间隔通常递增，例如第 1 次 10 秒、第 2 次 30 秒、第 3 次 1 分钟，超过第 16 次后每次间隔为 2 小时；顺序消息通常使用固定重试间隔。最大重试次数由 Consumer Group 元数据指定，如果最大重试次数为 3，则最多投递 4 次，即 1 次原始投递和 3 次重试。citeturn223529search0

需要特别注意：RocketMQ 4.x PushConsumer 文档说明，消息重试只在集群模式下生效，广播模式不提供消息重试；广播模式消费失败后不会重试，而是继续消费新消息。citeturn223529search2

重试与死信处理建议如下：

| 阶段 | 处理建议 |
| --- | --- |
| 第 1 次失败 | 记录错误日志，抛出异常触发重试。 |
| 多次失败 | 日志中重点输出 `reconsumeTimes`，必要时提前告警。 |
| 接近最大重试次数 | 保存异常上下文，准备补偿或人工处理。 |
| 进入死信队列 | 建立死信消息巡检、告警、补偿和人工处理流程。 |
| 死信重新投递 | 重新投递前必须确认业务状态和幂等策略。 |

下面给出一个死信消息处理消费者示例。RocketMQ 4.x 常见死信 Topic 命名格式为 `%DLQ%{consumerGroup}`；实际生产环境应以消息平台或运维规范为准。

文件位置：`src/main/java/io/github/atengk/rocketmq/consumer/OrderDeadLetterConsumer.java`

```java id="mzc2fh"
package io.github.atengk.rocketmq.consumer;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 订单死信消息消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "%DLQ%order-exception-consumer-group",
        consumerGroup = "order-dlq-handler-consumer-group",
        selectorExpression = "*",
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.CLUSTERING
)
public class OrderDeadLetterConsumer implements RocketMQListener<MessageExt> {

    /**
     * 消费死信消息
     *
     * @param messageExt RocketMQ 原始消息
     */
    @Override
    public void onMessage(MessageExt messageExt) {
        String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);

        log.error("收到订单死信消息，topic={}，tag={}，keys={}，msgId={}，queueId={}，queueOffset={}，reconsumeTimes={}，body={}",
                messageExt.getTopic(),
                messageExt.getTags(),
                messageExt.getKeys(),
                messageExt.getMsgId(),
                messageExt.getQueueId(),
                messageExt.getQueueOffset(),
                messageExt.getReconsumeTimes(),
                StrUtil.maxLength(body, 2000));

        // 生产环境建议执行以下动作：
        // 1. 保存死信消息到数据库
        // 2. 触发告警
        // 3. 标记业务补偿状态
        // 4. 由后台系统或补偿任务人工确认后重新处理
    }
}
~~~

死信消息不建议被消费者直接无条件重新执行业务逻辑。正确做法是先持久化死信内容，再通过后台页面或补偿任务进行人工确认、状态校验和幂等处理。

死信消息表可以按以下结构设计。

文件位置：`db/mq_dead_letter_message.sql`

```sql
-- RocketMQ 死信消息表：用于保存最终消费失败的消息，支持人工排查和补偿
CREATE TABLE mq_dead_letter_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    topic VARCHAR(255) NOT NULL COMMENT '死信Topic',
    original_topic VARCHAR(255) DEFAULT NULL COMMENT '原始Topic',
    tags VARCHAR(255) DEFAULT NULL COMMENT '消息Tag',
    message_key VARCHAR(255) DEFAULT NULL COMMENT '消息Key',
    msg_id VARCHAR(128) NOT NULL COMMENT 'RocketMQ消息ID',
    business_key VARCHAR(255) DEFAULT NULL COMMENT '业务键',
    message_body TEXT NOT NULL COMMENT '消息体',
    reconsume_times INT DEFAULT 0 COMMENT '重试次数',
    handle_status VARCHAR(32) NOT NULL COMMENT '处理状态：WAIT_HANDLE、HANDLED、IGNORED',
    error_message VARCHAR(1024) DEFAULT NULL COMMENT '异常信息',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_msg_id (msg_id),
    KEY idx_business_key (business_key),
    KEY idx_handle_status (handle_status)
) COMMENT='RocketMQ死信消息表';
```

重试与死信队列使用边界如下：

| 边界               | 说明                                                         |
| ------------------ | ------------------------------------------------------------ |
| 重试不是限流       | 限流应通过消费速率控制、线程池、队列堆积治理实现，不应故意返回失败触发重试。 |
| 重试不是业务分支   | 不能把消费失败设计成正常业务路径。                           |
| 死信不是终点       | 死信消息必须有巡检、告警、补偿或人工处理机制。               |
| 死信补偿必须幂等   | 死信重新处理可能与原业务状态冲突，必须先校验状态。           |
| 广播消费不依赖重试 | 广播模式失败不会按集群模式进行消息重试。                     |

### 日志记录规范

RocketMQ 日志记录的目标是支持消息链路排查。一次完整的消息排查通常需要从 Producer 发送日志、Broker 消息信息、Consumer 消费日志、业务表状态、补偿记录和死信记录中串联上下文。因此日志字段必须稳定、完整、可检索。

建议 Producer 发送日志至少包含以下字段：

| 字段           | 说明                               |
| -------------- | ---------------------------------- |
| `topic`        | 消息 Topic。                       |
| `tag`          | 消息 Tag。                         |
| `businessKey`  | 业务唯一键，例如订单号、支付单号。 |
| `messageId`    | 业务消息 ID。                      |
| `msgId`        | RocketMQ 返回的消息 ID。           |
| `sendStatus`   | 发送结果。                         |
| `traceId`      | 链路追踪 ID。                      |
| `cost`         | 发送耗时。                         |
| `errorMessage` | 失败原因。                         |

建议 Consumer 消费日志至少包含以下字段：

| 字段             | 说明                |
| ---------------- | ------------------- |
| `topic`          | 消息 Topic。        |
| `tag`            | 消息 Tag。          |
| `keys`           | RocketMQ 消息 Key。 |
| `msgId`          | RocketMQ 消息 ID。  |
| `businessKey`    | 业务唯一键。        |
| `consumerGroup`  | 消费组。            |
| `queueId`        | 队列 ID。           |
| `queueOffset`    | 队列 offset。       |
| `reconsumeTimes` | 当前重试次数。      |
| `traceId`        | 链路追踪 ID。       |
| `consumeResult`  | 消费结果。          |
| `errorMessage`   | 失败原因。          |

下面是一个统一日志工具类，用于规范 Producer 和 Consumer 日志输出。该类只做日志格式封装，不承载业务逻辑。

文件位置：`src/main/java/io/github/atengk/rocketmq/util/MqLogUtil.java`

```java
package io.github.atengk.rocketmq.util;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.MessageExt;

/**
 * RocketMQ 日志工具类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public final class MqLogUtil {

    private MqLogUtil() {
    }

    /**
     * 记录发送成功日志
     *
     * @param topic       Topic
     * @param tag         Tag
     * @param businessKey 业务键
     * @param messageId   消息ID
     * @param sendResult  发送结果
     * @param costMillis  发送耗时
     */
    public static void logSendSuccess(String topic,
                                      String tag,
                                      String businessKey,
                                      String messageId,
                                      SendResult sendResult,
                                      long costMillis) {
        log.info("RocketMQ消息发送成功，topic={}，tag={}，businessKey={}，messageId={}，msgId={}，sendStatus={}，costMillis={}",
                topic,
                tag,
                businessKey,
                messageId,
                sendResult.getMsgId(),
                sendResult.getSendStatus(),
                costMillis);
    }

    /**
     * 记录发送失败日志
     *
     * @param topic       Topic
     * @param tag         Tag
     * @param businessKey 业务键
     * @param messageId   消息ID
     * @param costMillis  发送耗时
     * @param throwable   异常
     */
    public static void logSendFailure(String topic,
                                      String tag,
                                      String businessKey,
                                      String messageId,
                                      long costMillis,
                                      Throwable throwable) {
        log.error("RocketMQ消息发送失败，topic={}，tag={}，businessKey={}，messageId={}，costMillis={}，errorMessage={}",
                topic,
                tag,
                businessKey,
                messageId,
                costMillis,
                StrUtil.maxLength(throwable.getMessage(), 500),
                throwable);
    }

    /**
     * 记录消费开始日志
     *
     * @param consumerGroup 消费组
     * @param messageExt    RocketMQ 原始消息
     * @param businessKey   业务键
     */
    public static void logConsumeStart(String consumerGroup, MessageExt messageExt, String businessKey) {
        log.info("RocketMQ消息开始消费，consumerGroup={}，topic={}，tag={}，keys={}，msgId={}，businessKey={}，queueId={}，queueOffset={}，reconsumeTimes={}",
                consumerGroup,
                messageExt.getTopic(),
                messageExt.getTags(),
                messageExt.getKeys(),
                messageExt.getMsgId(),
                businessKey,
                messageExt.getQueueId(),
                messageExt.getQueueOffset(),
                messageExt.getReconsumeTimes());
    }

    /**
     * 记录消费成功日志
     *
     * @param consumerGroup 消费组
     * @param messageExt    RocketMQ 原始消息
     * @param businessKey   业务键
     * @param costMillis    消费耗时
     */
    public static void logConsumeSuccess(String consumerGroup,
                                         MessageExt messageExt,
                                         String businessKey,
                                         long costMillis) {
        log.info("RocketMQ消息消费成功，consumerGroup={}，topic={}，tag={}，keys={}，msgId={}，businessKey={}，queueId={}，queueOffset={}，reconsumeTimes={}，costMillis={}",
                consumerGroup,
                messageExt.getTopic(),
                messageExt.getTags(),
                messageExt.getKeys(),
                messageExt.getMsgId(),
                businessKey,
                messageExt.getQueueId(),
                messageExt.getQueueOffset(),
                messageExt.getReconsumeTimes(),
                costMillis);
    }

    /**
     * 记录消费失败日志
     *
     * @param consumerGroup 消费组
     * @param messageExt    RocketMQ 原始消息
     * @param businessKey   业务键
     * @param costMillis    消费耗时
     * @param throwable     异常
     */
    public static void logConsumeFailure(String consumerGroup,
                                         MessageExt messageExt,
                                         String businessKey,
                                         long costMillis,
                                         Throwable throwable) {
        log.error("RocketMQ消息消费失败，consumerGroup={}，topic={}，tag={}，keys={}，msgId={}，businessKey={}，queueId={}，queueOffset={}，reconsumeTimes={}，costMillis={}，errorMessage={}",
                consumerGroup,
                messageExt.getTopic(),
                messageExt.getTags(),
                messageExt.getKeys(),
                messageExt.getMsgId(),
                businessKey,
                messageExt.getQueueId(),
                messageExt.getQueueOffset(),
                messageExt.getReconsumeTimes(),
                costMillis,
                StrUtil.maxLength(throwable.getMessage(), 500),
                throwable);
    }
}
```

日志级别建议如下：

| 日志级别 | 使用场景                                             |
| -------- | ---------------------------------------------------- |
| `INFO`   | 发送成功、消费成功、关键业务状态变更。               |
| `WARN`   | 重复消费、即将重试、业务状态暂不可处理、非核心异常。 |
| `ERROR`  |                                                      |

发送最终失败、消费失败、死信消息、不可恢复异常。 |
| `DEBUG` | 本地调试消息体明细，生产环境慎用。 |

日志记录注意事项如下：

| 注意事项           | 说明                                                |
| ------------------ | --------------------------------------------------- |
| 不记录完整敏感信息 | 身份证、手机号、银行卡、Token、密钥等必须脱敏。     |
| 不输出超大消息体   | 消息体过大时应截断，避免日志爆炸。                  |
| 不只记录异常堆栈   | 必须同时记录 Topic、Tag、Key、业务 ID。             |
| 不忽略重试次数     | `reconsumeTimes` 是判断消费失败严重程度的重要字段。 |
| 不使用模糊日志     | 避免只写“发送失败”“消费异常”，必须包含可检索字段。  |

本章最终处理原则是：发送失败要有补偿，消费失败要区分是否可恢复，重试要受控，死信要治理，日志要能串联完整链路。RocketMQ 只提供可靠消息机制，业务系统仍需要通过幂等、补偿、告警和审计记录保证最终业务一致性。

## 测试与验证

本节用于说明 RocketMQ 在 Spring Boot 3 项目中的测试与验证方式，包括单元测试、集成测试、本地 RocketMQ 验证、消息发送验证和消息消费验证。本节对应原文档大纲中的“测试与验证”章节。

RocketMQ 测试建议分层处理：单元测试只验证本地代码逻辑，不依赖真实 RocketMQ；集成测试验证应用与 RocketMQ 的真实连接、发送和消费链路；本地验证用于开发人员手动检查 NameServer、Broker、Topic、发送结果和消费日志。RocketMQ 官方 Docker 快速开始文档说明，可以通过 Docker 启动单节点 RocketMQ，并完成基本消息发送与接收验证，因此本章本地验证默认基于 Docker 部署的 RocketMQ 服务。([RocketMQ](https://rocketmq.apache.org/docs/quickStart/02quickstartWithDocker/?utm_source=chatgpt.com))

### 单元测试

单元测试用于验证生产者封装、消费者解析、幂等判断、异常分支和参数校验逻辑。单元测试不应该依赖真实 RocketMQ、真实 Broker 或真实网络，推荐使用 Mockito Mock `RocketMQTemplate`、幂等服务和业务服务。

测试依赖建议放在 `pom.xml` 中。Spring Boot Starter Test 已包含 JUnit Jupiter、Mockito、AssertJ 等常用测试能力；如果需要等待异步消费或异步回调，可以额外引入 Awaitility。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Test：包含 JUnit 5、Mockito、AssertJ、Spring Test 等测试能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Awaitility：用于等待异步发送回调、异步消费结果等场景 -->
    <dependency>
        <groupId>org.awaitility</groupId>
        <artifactId>awaitility</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

下面示例测试生产者同步发送逻辑。测试目标是验证：消息参数合法时会调用 `RocketMQTemplate.syncSend`，并且返回发送结果；消息参数不合法时会直接抛出异常，不会调用 RocketMQ。

文件位置：`src/test/java/io/github/atengk/rocketmq/producer/OrderMessageSendProducerTest.java`

```java
package io.github.atengk.rocketmq.producer;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.rocketmq.model.OrderEventMessage;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.messaging.Message;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 订单消息发送生产者单元测试
 *
 * @author Ateng
 * @since 2026-04-30
 */
class OrderMessageSendProducerTest {

    private RocketMQTemplate rocketMQTemplate;

    private OrderMessageSendProducer orderMessageSendProducer;

    /**
     * 初始化测试对象
     */
    @BeforeEach
    void setUp() {
        rocketMQTemplate = mock(RocketMQTemplate.class);
        orderMessageSendProducer = new OrderMessageSendProducer(rocketMQTemplate);
    }

    /**
     * 测试同步发送订单创建消息成功
     */
    @Test
    void shouldSyncSendOrderCreatedSuccess() {
        SendResult sendResult = new SendResult();
        sendResult.setSendStatus(SendStatus.SEND_OK);
        sendResult.setMsgId(IdUtil.fastSimpleUUID());

        when(rocketMQTemplate.syncSend(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.<Message<String>>any(),
                ArgumentMatchers.anyLong()
        )).thenReturn(sendResult);

        OrderEventMessage message = buildMessage("ORDER_CREATED");
        SendResult result = orderMessageSendProducer.syncSendOrderCreated(message);

        assertThat(result).isNotNull();
        assertThat(result.getSendStatus()).isEqualTo(SendStatus.SEND_OK);

        verify(rocketMQTemplate, times(1)).syncSend(
                ArgumentMatchers.eq("order-event-topic:order_created"),
                ArgumentMatchers.<Message<String>>any(),
                ArgumentMatchers.anyLong()
        );
    }

    /**
     * 测试消息缺少订单号时抛出异常
     */
    @Test
    void shouldThrowExceptionWhenOrderNoIsBlank() {
        OrderEventMessage message = new OrderEventMessage(
                IdUtil.fastSimpleUUID(),
                "",
                "USER10001",
                "ORDER_CREATED",
                new BigDecimal("99.90"),
                LocalDateTime.now(),
                IdUtil.fastSimpleUUID()
        );

        assertThatThrownBy(() -> orderMessageSendProducer.syncSendOrderCreated(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("订单号不能为空");

        verifyNoInteractions(rocketMQTemplate);
    }

    /**
     * 构建订单事件消息
     *
     * @param eventType 事件类型
     * @return 订单事件消息
     */
    private OrderEventMessage buildMessage(String eventType) {
        return new OrderEventMessage(
                IdUtil.fastSimpleUUID(),
                "ORDER" + System.currentTimeMillis(),
                "USER10001",
                eventType,
                new BigDecimal("99.90"),
                LocalDateTime.now(),
                IdUtil.fastSimpleUUID()
        );
    }
}
```

下面示例测试消费者逻辑。测试目标是验证：正常消息会执行幂等占用和确认；重复消息不会再次处理；消费异常时会释放幂等键并向外抛出异常。

文件位置：`src/test/java/io/github/atengk/rocketmq/consumer/OrderNormalMessageConsumerTest.java`

```java
package io.github.atengk.rocketmq.consumer;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.rocketmq.model.OrderEventMessage;
import io.github.atengk.rocketmq.service.MessageIdempotentService;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 订单普通消息消费者单元测试
 *
 * @author Ateng
 * @since 2026-04-30
 */
class OrderNormalMessageConsumerTest {

    private MessageIdempotentService messageIdempotentService;

    private OrderNormalMessageConsumer orderNormalMessageConsumer;

    /**
     * 初始化测试对象
     */
    @BeforeEach
    void setUp() {
        messageIdempotentService = mock(MessageIdempotentService.class);
        orderNormalMessageConsumer = new OrderNormalMessageConsumer(messageIdempotentService);
    }

    /**
     * 测试普通消息消费成功
     */
    @Test
    void shouldConsumeNormalMessageSuccess() {
        when(messageIdempotentService.tryConsume(anyString())).thenReturn(true);

        MessageExt messageExt = buildMessageExt("order_normal", buildMessage("ORDER_NORMAL"));
        orderNormalMessageConsumer.onMessage(messageExt);

        verify(messageIdempotentService, times(1)).tryConsume(anyString());
        verify(messageIdempotentService, times(1)).confirmConsumed(anyString());
        verify(messageIdempotentService, never()).cancelConsume(anyString());
    }

    /**
     * 测试重复消息跳过消费
     */
    @Test
    void shouldSkipWhenMessageAlreadyConsumed() {
        when(messageIdempotentService.tryConsume(anyString())).thenReturn(false);

        MessageExt messageExt = buildMessageExt("order_normal", buildMessage("ORDER_NORMAL"));
        orderNormalMessageConsumer.onMessage(messageExt);

        verify(messageIdempotentService, times(1)).tryConsume(anyString());
        verify(messageIdempotentService, never()).confirmConsumed(anyString());
        verify(messageIdempotentService, never()).cancelConsume(anyString());
    }

    /**
     * 构建 RocketMQ 原始消息
     *
     * @param tag     Tag
     * @param message 订单事件消息
     * @return RocketMQ 原始消息
     */
    private MessageExt buildMessageExt(String tag, OrderEventMessage message) {
        MessageExt messageExt = new MessageExt();
        messageExt.setTopic("order-event-topic");
        messageExt.setTags(tag);
        messageExt.setKeys(message.orderNo());
        messageExt.setMsgId(IdUtil.fastSimpleUUID());
        messageExt.setQueueId(0);
        messageExt.setQueueOffset(1L);
        messageExt.setBody(JSONUtil.toJsonStr(message).getBytes(StandardCharsets.UTF_8));
        return messageExt;
    }

    /**
     * 构建订单事件消息
     *
     * @param eventType 事件类型
     * @return 订单事件消息
     */
    private OrderEventMessage buildMessage(String eventType) {
        return new OrderEventMessage(
                IdUtil.fastSimpleUUID(),
                "ORDER" + System.currentTimeMillis(),
                "USER10001",
                eventType,
                new BigDecimal("99.90"),
                LocalDateTime.now(),
                IdUtil.fastSimpleUUID()
        );
    }
}
```

单元测试建议覆盖以下内容：

| 测试对象 | 测试重点                                                     |
| -------- | ------------------------------------------------------------ |
| Producer | 参数校验、Topic/Tag 拼接、Key 设置、同步发送、异步回调、异常捕获。 |
| Consumer | 消息体解析、空消息处理、幂等判断、业务异常、重试异常抛出。   |
| 幂等服务 | 首次消费、重复消费、消费确认、消费失败释放。                 |
| 消息模型 | 必填字段校验、JSON 序列化、版本字段兼容。                    |
| 常量类   | Topic、Tag、Consumer Group 是否符合命名规范。                |

### 集成测试

集成测试用于验证 Spring Boot 应用与真实 RocketMQ 服务之间的连接、发送和消费链路。集成测试可以使用本地 Docker Compose 启动 RocketMQ，也可以使用 CI 环境中预置的测试 RocketMQ 集群。RocketMQ 官方 Docker 文档提供了 NameServer、Broker 和 Proxy 的单节点启动方式，适合本地开发和测试环境快速搭建。([RocketMQ](https://rocketmq.apache.org/docs/quickStart/02quickstartWithDocker/?utm_source=chatgpt.com))

由于 Testcontainers 官方模块列表中没有 RocketMQ 专用模块，若团队要用 Testcontainers 管理 RocketMQ，通常需要使用 `GenericContainer` 或 Docker Compose 自行编排 NameServer 和 Broker；Testcontainers Java 的 JUnit 5 集成通过 `@Testcontainers` 和 `@Container` 管理容器生命周期。([Testcontainers](https://testcontainers.com/modules/?utm_source=chatgpt.com)) ([java.testcontainers.org](https://java.testcontainers.org/test_framework_integration/junit_5/?utm_source=chatgpt.com))

本文档推荐的集成测试策略是：本地和 CI 使用 Docker Compose 启动 RocketMQ，Spring Boot 集成测试通过 `application-test.yml` 连接固定的 `127.0.0.1:9876`。

文件位置：`src/test/resources/application-test.yml`

```yaml
server:
  # 测试环境随机端口，避免端口冲突
  port: 0

spring:
  application:
    # 测试环境应用名称
    name: rocketmq-demo-service-test

rocketmq:
  # 集成测试 RocketMQ NameServer 地址
  name-server: 127.0.0.1:9876

  producer:
    # 集成测试生产者组
    group: rocketmq-demo-test-producer-group

    # 测试环境发送超时时间，单位毫秒
    send-message-timeout: 3000

    # 测试环境同步发送失败重试次数
    retry-times-when-send-failed: 1

    # 测试环境异步发送失败重试次数
    retry-times-when-send-async-failed: 1
```

为了验证消费链路，测试环境可以提供一个消息收集器。消费者收到消息后写入收集器，测试用例使用 Awaitility 等待消息到达。

文件位置：`src/test/java/io/github/atengk/rocketmq/support/RocketMqTestMessageCollector.java`

```java
package io.github.atengk.rocketmq.support;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * RocketMQ 测试消息收集器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class RocketMqTestMessageCollector {

    private final List<String> messages = new CopyOnWriteArrayList<>();

    /**
     * 添加消息
     *
     * @param message 消息内容
     */
    public void addMessage(String message) {
        messages.add(message);
        log.info("测试消息已收集，message={}", message);
    }

    /**
     * 判断是否包含指定内容
     *
     * @param keyword 关键字
     * @return true 表示包含
     */
    public boolean contains(String keyword) {
        return messages.stream().anyMatch(message -> message.contains(keyword));
    }

    /**
     * 清空消息
     */
    public void clear() {
        messages.clear();
        log.info("测试消息收集器已清空");
    }

    /**
     * 获取消息数量
     *
     * @return 消息数量
     */
    public int size() {
        return CollUtil.size(messages);
    }
}
```

测试消费者只放在 `src/test/java` 下，不参与生产环境打包。它用于监听测试 Topic，并将收到的消息写入收集器。

文件位置：`src/test/java/io/github/atengk/rocketmq/support/RocketMqIntegrationTestConsumer.java`

```java
package io.github.atengk.rocketmq.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 集成测试消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "rocketmq-integration-test-topic",
        consumerGroup = "rocketmq-integration-test-consumer-group",
        selectorExpression = "integration_test",
        messageModel = MessageModel.CLUSTERING
)
public class RocketMqIntegrationTestConsumer implements RocketMQListener<String> {

    private final RocketMqTestMessageCollector messageCollector;

    /**
     * 消费集成测试消息
     *
     * @param message 消息内容
     */
    @Override
    public void onMessage(String message) {
        log.info("收到RocketMQ集成测试消息，message={}", message);
        messageCollector.addMessage(message);
    }
}
```

下面是集成测试用例。该测试会通过 `RocketMQTemplate` 发送消息，然后等待测试消费者收到消息。

文件位置：`src/test/java/io/github/atengk/rocketmq/RocketMqIntegrationTest.java`

```java
package io.github.atengk.rocketmq;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.rocketmq.support.RocketMqTestMessageCollector;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * RocketMQ 集成测试
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootTest
@ActiveProfiles("test")
@RequiredArgsConstructor
class RocketMqIntegrationTest {

    private final RocketMQTemplate rocketMQTemplate;

    private final RocketMqTestMessageCollector messageCollector;

    /**
     * 初始化测试数据
     */
    @BeforeEach
    void setUp() {
        messageCollector.clear();
    }

    /**
     * 测试消息发送和消费完整链路
     */
    @Test
    void shouldSendAndConsumeMessage() {
        String messageId = IdUtil.fastSimpleUUID();
        String orderNo = "ORDER" + System.currentTimeMillis();

        Map<String, Object> payload = Map.of(
                "messageId", messageId,
                "orderNo", orderNo,
                "eventType", "integration_test"
        );

        SendResult sendResult = rocketMQTemplate.syncSend(
                "rocketmq-integration-test-topic:integration_test",
                MessageBuilder.withPayload(JSONUtil.toJsonStr(payload))
                        .setHeader("KEYS", orderNo)
                        .setHeader("messageId", messageId)
                        .build(),
                3000L
        );

        assertThat(sendResult).isNotNull();
        assertThat(sendResult.getSendStatus()).isEqualTo(SendStatus.SEND_OK);

        await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> assertThat(messageCollector.contains(orderNo)).isTrue());
    }
}
```

集成测试建议覆盖以下内容：

| 测试项   | 验证目标                                    |
| -------- | ------------------------------------------- |
| 应用启动 | RocketMQ 自动配置是否正常加载。             |
| 同步发送 | `syncSend` 是否返回 `SEND_OK`。             |
| 异步发送 | 回调是否进入 `onSuccess` 或 `onException`。 |
| 普通消费 | 消费者是否收到目标 Topic 和 Tag 的消息。    |
| Tag 过滤 | 非目标 Tag 是否不会被当前消费者处理。       |
| 消费重试 | 消费者抛出异常后是否触发重复投递。          |
| 幂等处理 | 重复消息是否被正确跳过。                    |
| 顺序消息 | 同一业务键消息是否按顺序处理。              |

### 本地 RocketMQ 验证

本地 RocketMQ 验证用于确认开发机上的 NameServer、Broker 和应用连接是否正常。官方 Docker 快速开始文档说明，RocketMQ Docker 部署需要创建容器网络，分别启动 NameServer 和 Broker/Proxy，并通过日志确认服务启动成功。([RocketMQ](https://rocketmq.apache.org/docs/quickStart/02quickstartWithDocker/?utm_source=chatgpt.com))

推荐在项目根目录准备 Docker Compose 文件，便于开发人员一键启动本地 RocketMQ。

文件位置：`docker/rocketmq/broker.conf`

```properties
# Broker 所属集群名称
brokerClusterName=DefaultCluster

# Broker 名称
brokerName=broker-a

# Broker ID，0 表示 Master
brokerId=0

# 本地开发使用 127.0.0.1，便于宿主机应用访问
brokerIP1=127.0.0.1

# 本地开发允许自动创建 Topic，生产环境不建议开启
autoCreateTopicEnable=true

# 删除过期消息的时间点
deleteWhen=04

# 消息文件保留时间，单位小时
fileReservedTime=48
```

文件位置：`docker/rocketmq/docker-compose.yml`

```yaml
services:
  rmqnamesrv:
    image: apache/rocketmq:5.4.0
    container_name: rmqnamesrv
    ports:
      # NameServer 端口
      - "9876:9876"
    networks:
      - rocketmq
    command: sh mqnamesrv

  rmqbroker:
    image: apache/rocketmq:5.4.0
    container_name: rmqbroker
    depends_on:
      - rmqnamesrv
    ports:
      # Broker 端口
      - "10909:10909"
      - "10911:10911"
      - "10912:10912"
      # Proxy 端口
      - "8080:8080"
      - "8081:8081"
    environment:
      # Broker 连接 NameServer 的地址
      NAMESRV_ADDR: rmqnamesrv:9876
    volumes:
      # Broker 配置文件
      - ./broker.conf:/home/rocketmq/rocketmq-5.4.0/conf/broker.conf
    networks:
      - rocketmq
    command: sh mqbroker --enable-proxy -c /home/rocketmq/rocketmq-5.4.0/conf/broker.conf

networks:
  rocketmq:
    name: rocketmq
```

启动本地 RocketMQ：

```bash
# 进入 Docker Compose 目录
cd docker/rocketmq

# 启动 RocketMQ
docker compose up -d

# 查看容器状态
docker compose ps

# 查看 NameServer 日志
docker logs --tail 100 rmqnamesrv

# 查看 Broker 日志
docker logs --tail 100 rmqbroker
```

命令说明：`docker compose up -d` 会在后台启动 NameServer 和 Broker；`docker compose ps` 用于确认容器状态；`docker logs` 用于检查启动日志。看到 NameServer 和 Broker 启动成功日志后，再启动 Spring Boot 应用进行发送和消费验证。

如果使用 RocketMQ 5.3.2，需要同步修改镜像版本和挂载路径。官方 Docker 快速开始文档示例使用 `apache/rocketmq:5.3.2`，并将配置文件挂载到 `/home/rocketmq/rocketmq-5.3.2/conf/broker.conf`。([RocketMQ](https://rocketmq.apache.org/docs/quickStart/02quickstartWithDocker/?utm_source=chatgpt.com))

本地环境检查命令如下：

```bash
# 检查 NameServer 端口
nc -vz 127.0.0.1 9876

# 检查 Broker 端口
nc -vz 127.0.0.1 10911

# 检查 Proxy 端口
nc -vz 127.0.0.1 8081

# 查看 RocketMQ 容器
docker ps --filter "name=rmq"
```

本地验证常见问题如下：

| 问题                    | 排查方式                                                     |
| ----------------------- | ------------------------------------------------------------ |
| 应用连接不上 NameServer | 检查 `rocketmq.name-server` 是否为 `127.0.0.1:9876`，检查端口是否映射。 |
| Broker 启动失败         | 检查 `broker.conf` 挂载路径是否和镜像版本一致。              |
| 发送超时                | 检查 Broker 端口、Broker IP、Topic 是否存在。                |
| 消费者不消费            | 检查 Topic、Tag、Consumer Group、消费模式和应用日志。        |
| Topic 不存在            | 本地确认 `autoCreateTopicEnable=true`，生产环境应由平台提前创建。 |

### 消息发送验证

消息发送验证用于确认 Producer 是否能将消息成功写入 Broker。核心判断标准不是接口返回成功，而是 `SendResult` 返回 `SEND_OK`，并且日志中包含 Topic、Tag、业务 Key、业务消息 ID 和 RocketMQ 消息 ID。

建议准备一个测试接口，只在 `dev` 或 `test` 环境启用。下面示例通过 `@Profile` 限制接口不会在生产环境加载。

文件位置：`src/main/java/io/github/atengk/rocketmq/controller/RocketMqSendVerifyController.java`

```java
package io.github.atengk.rocketmq.controller;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * RocketMQ 发送验证接口
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@RestController
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class RocketMqSendVerifyController {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 验证同步消息发送
     *
     * @return 发送结果
     */
    @GetMapping("/verify/rocketmq/send")
    public String verifySend() {
        String topic = "rocketmq-verify-topic";
        String tag = "send_verify";
        String orderNo = "ORDER" + System.currentTimeMillis();
        String messageId = IdUtil.fastSimpleUUID();

        Map<String, Object> payload = Map.of(
                "messageId", messageId,
                "orderNo", orderNo,
                "eventType", "send_verify",
                "eventTime", LocalDateTime.now().toString(),
                "traceId", IdUtil.fastSimpleUUID()
        );

        SendResult sendResult = rocketMQTemplate.syncSend(
                topic + ":" + tag,
                MessageBuilder.withPayload(JSONUtil.toJsonStr(payload))
                        // RocketMQ 消息 Key，便于按订单号排查
                        .setHeader(RocketMQHeaders.KEYS, orderNo)
                        // 业务消息 ID
                        .setHeader("messageId", messageId)
                        .build(),
                3000L
        );

        log.info("RocketMQ发送验证成功，topic={}，tag={}，orderNo={}，messageId={}，msgId={}，sendStatus={}",
                topic,
                tag,
                orderNo,
                messageId,
                sendResult.getMsgId(),
                sendResult.getSendStatus());

        return "发送成功：" + sendResult.getSendStatus() + "，orderNo=" + orderNo;
    }
}
```

启动应用后执行以下命令验证发送：

```bash
# 使用 dev 环境启动应用
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 调用发送验证接口
curl "http://127.0.0.1:8088/verify/rocketmq/send"
```

验证发送成功时，应用日志中应出现类似字段：

```text
RocketMQ发送验证成功，topic=rocketmq-verify-topic，tag=send_verify，orderNo=ORDERxxx，messageId=xxx，msgId=xxx，sendStatus=SEND_OK
```

消息发送验证检查项如下：

| 检查项      | 预期结果                                                 |
| ----------- | -------------------------------------------------------- |
| 接口返回    | 返回 `发送成功：SEND_OK`。                               |
| 应用日志    | 包含 Topic、Tag、orderNo、messageId、msgId、sendStatus。 |
| Broker 日志 | 无 Topic 路由、权限、存储异常。                          |
| Topic       | 本地环境可自动创建，生产环境应提前创建。                 |
| 消息 Key    | 应设置为稳定业务键，例如订单号。                         |

如果发送失败，优先检查以下内容：

| 失败现象                      | 可能原因                                                     |
| ----------------------------- | ------------------------------------------------------------ |
| `No route info of this topic` | Topic 不存在，或 Producer 未从 NameServer 获取到 Topic 路由。 |
| 发送超时                      | Broker 不可达、Broker IP 配置错误、网络异常。                |
| 权限异常                      | ACL 配置错误，AK/SK 不正确或无 Topic 权限。                  |
| 消息体过大                    | 消息超过 Broker 或 Producer 最大消息大小限制。               |
| 返回非 `SEND_OK`              | Broker 存储异常、刷盘异常或其他服务端问题。                  |

### 消息消费验证

消息消费验证用于确认 Consumer 能够正确订阅 Topic 和 Tag，并完成业务处理。消费验证不能只看消息是否收到，还要验证 Tag 过滤、幂等处理、异常重试和日志字段是否完整。

下面的消费者用于本地验证消费链路，只在 `dev` 和 `test` 环境启用。

文件位置：`src/main/java/io/github/atengk/rocketmq/consumer/RocketMqConsumeVerifyConsumer.java`

```java
package io.github.atengk.rocketmq.consumer;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * RocketMQ 消费验证消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@Profile({"dev", "test"})
@RocketMQMessageListener(
        topic = "rocketmq-verify-topic",
        consumerGroup = "rocketmq-verify-consumer-group",
        selectorExpression = "send_verify",
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.CLUSTERING
)
public class RocketMqConsumeVerifyConsumer implements RocketMQListener<MessageExt> {

    /**
     * 消费验证消息
     *
     * @param messageExt RocketMQ 原始消息
     */
    @Override
    public void onMessage(MessageExt messageExt) {
        String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);

        if (StrUtil.isBlank(body)) {
            log.warn("RocketMQ消费验证消息为空，topic={}，tag={}，msgId={}",
                    messageExt.getTopic(), messageExt.getTags(), messageExt.getMsgId());
            return;
        }

        log.info("RocketMQ消费验证成功，topic={}，tag={}，keys={}，msgId={}，queueId={}，queueOffset={}，reconsumeTimes={}，body={}",
                messageExt.getTopic(),
                messageExt.getTags(),
                messageExt.getKeys(),
                messageExt.getMsgId(),
                messageExt.getQueueId(),
                messageExt.getQueueOffset(),
                messageExt.getReconsumeTimes(),
                JSONUtil.toJsonStr(JSONUtil.parseObj(body)));
    }
}
```

消费验证步骤如下：

```bash
# 启动本地 RocketMQ
cd docker/rocketmq
docker compose up -d

# 启动 Spring Boot 应用
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 发送验证消息
curl "http://127.0.0.1:8088/verify/rocketmq/send"

# 查看应用日志
tail -f logs/rocketmq-demo-service.log
```

消费成功时，日志中应出现类似字段：

```text
RocketMQ消费验证成功，topic=rocketmq-verify-topic，tag=send_verify，keys=ORDERxxx，msgId=xxx，queueId=0，queueOffset=1，reconsumeTimes=0
```

消息消费验证检查项如下：

| 检查项         | 预期结果                                         |
| -------------- | ------------------------------------------------ |
| Consumer 启动  | 应用启动日志中没有消费者注册异常。               |
| Topic 匹配     | 消费者订阅的 Topic 与生产者发送 Topic 一致。     |
| Tag 匹配       | `selectorExpression` 与发送 Tag 一致。           |
| Consumer Group | 消费组名称唯一且语义明确。                       |
| 消息体         | 能正常反序列化为业务消息对象。                   |
| 重试次数       | 正常消费时 `reconsumeTimes=0`。                  |
| 幂等           | 重复消息不会重复执行业务逻辑。                   |
| 日志           | 包含 Topic、Tag、Key、MsgId、业务 ID、重试次数。 |

如果消费者没有收到消息，按以下顺序排查：

```text
1. 确认 RocketMQ 容器是否正常运行
2. 确认应用 rocketmq.name-server 是否正确
3. 确认生产者发送返回 SEND_OK
4. 确认消费者 Topic 与生产者 Topic 一致
5. 确认消费者 selectorExpression 与生产者 Tag 一致
6. 确认 Consumer Group 是否与其他测试实例共用
7. 查看 Broker 日志和应用异常日志
8. 检查是否已经被同 Consumer Group 的其他实例消费
```

本章最终建议是：单元测试使用 Mock 保证代码逻辑稳定，集成测试连接真实 RocketMQ 保证链路可用，本地验证使用 Docker 快速启动服务，发送验证关注 `SEND_OK` 和消息 Key，消费验证关注 Topic、Tag、Consumer Group、重试次数和业务幂等。

## 项目实践建议

本节用于总结 RocketMQ 在 Spring Boot 3 项目中的落地实践，包括统一消息发送组件、统一消息模型、消费者幂等设计、配置隔离与多环境管理、生产环境使用注意事项。本节对应原文档大纲中的“项目实践建议”章节。

RocketMQ 接入项目后，不建议让业务代码直接散落调用 `RocketMQTemplate`，也不建议每个消费者各自定义消息结构、幂等规则和异常处理方式。更合理的方式是将消息发送、消息模型、Topic/Tag、幂等、日志、配置和补偿流程沉淀为统一规范，降低后续项目维护成本。

### 封装统一消息发送组件

统一消息发送组件用于屏蔽 `RocketMQTemplate` 的直接调用细节，集中处理 Topic、Tag、Key、消息体序列化、超时时间、延迟级别、顺序键、发送日志和异常处理。业务服务只需要构造标准发送请求，不需要关心底层发送方法差异。

建议文件结构如下：

```text
src/main/java/io/github/atengk/rocketmq/
├── constant/
│   └── MqTopicConstant.java
├── model/
│   ├── BaseMqMessage.java
│   └── MqSendRequest.java
└── producer/
    └── UnifiedRocketMqProducer.java
```

先定义统一 Topic 和 Tag 常量。生产项目中建议将常量放在公共模块中，由生产者和消费者共同引用。

文件位置：`src/main/java/io/github/atengk/rocketmq/constant/MqTopicConstant.java`

```java
package io.github.atengk.rocketmq.constant;

/**
 * RocketMQ Topic 与 Tag 常量
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class MqTopicConstant {

    /**
     * 订单普通事件 Topic
     */
    public static final String ORDER_EVENT_NORMAL_TOPIC = "order-event-normal-topic";

    /**
     * 订单顺序事件 Topic
     */
    public static final String ORDER_STATUS_FIFO_TOPIC = "order-status-fifo-topic";

    /**
     * 订单延迟事件 Topic
     */
    public static final String ORDER_TIMEOUT_DELAY_TOPIC = "order-timeout-delay-topic";

    /**
     * 订单已创建 Tag
     */
    public static final String TAG_ORDER_CREATED = "order_created";

    /**
     * 订单已支付 Tag
     */
    public static final String TAG_ORDER_PAID = "order_paid";

    /**
     * 订单已取消 Tag
     */
    public static final String TAG_ORDER_CANCELLED = "order_cancelled";

    /**
     * 订单状态变更 Tag
     */
    public static final String TAG_ORDER_STATUS_CHANGED = "order_status_changed";

    private MqTopicConstant() {
    }

    /**
     * 构建发送目标
     *
     * @param topic Topic
     * @param tag   Tag
     * @return 发送目标
     */
    public static String destination(String topic, String tag) {
        return topic + ":" + tag;
    }
}
```

定义统一消息发送请求。该对象用于承载发送时需要的 Topic、Tag、业务 Key、消息体、超时时间、延迟级别和顺序键。

文件位置：`src/main/java/io/github/atengk/rocketmq/model/MqSendRequest.java`

```java
package io.github.atengk.rocketmq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RocketMQ 统一发送请求
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqSendRequest<T> {

    /**
     * Topic 名称
     */
    private String topic;

    /**
     * Tag 名称
     */
    private String tag;

    /**
     * 业务唯一键，例如订单号、支付单号、流水号
     */
    private String businessKey;

    /**
     * 业务消息 ID
     */
    private String messageId;

    /**
     * 链路追踪 ID
     */
    private String traceId;

    /**
     * 来源系统
     */
    private String sourceSystem;

    /**
     * 消息体
     */
    private T payload;

    /**
     * 发送超时时间，单位毫秒
     */
    private Long timeoutMillis;

    /**
     * 延迟级别，延迟消息使用
     */
    private Integer delayLevel;

    /**
     * 顺序消息分片键，例如订单号、账户号、SKU
     */
    private String shardingKey;
}
```

下面的统一生产者封装同步、异步、单向、延迟和顺序发送。业务代码应优先调用该组件，而不是直接依赖 `RocketMQTemplate`。

文件位置：`src/main/java/io/github/atengk/rocketmq/producer/UnifiedRocketMqProducer.java`

```java
package io.github.atengk.rocketmq.producer;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.rocketmq.model.MqSendRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 统一消息发送组件
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnifiedRocketMqProducer {

    private static final long DEFAULT_TIMEOUT_MILLIS = 3000L;

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 同步发送消息
     *
     * @param request 发送请求
     * @return 发送结果
     */
    public SendResult syncSend(MqSendRequest<?> request) {
        checkRequest(request);

        String destination = buildDestination(request);
        Message<String> message = buildMessage(request);
        long timeoutMillis = ObjectUtil.defaultIfNull(request.getTimeoutMillis(), DEFAULT_TIMEOUT_MILLIS);

        SendResult sendResult = rocketMQTemplate.syncSend(destination, message, timeoutMillis);

        log.info("RocketMQ同步消息发送成功，topic={}，tag={}，businessKey={}，messageId={}，msgId={}，sendStatus={}",
                request.getTopic(),
                request.getTag(),
                request.getBusinessKey(),
                request.getMessageId(),
                sendResult.getMsgId(),
                sendResult.getSendStatus());

        return sendResult;
    }

    /**
     * 异步发送消息
     *
     * @param request 发送请求
     */
    public void asyncSend(MqSendRequest<?> request) {
        checkRequest(request);

        String destination = buildDestination(request);
        Message<String> message = buildMessage(request);
        long timeoutMillis = ObjectUtil.defaultIfNull(request.getTimeoutMillis(), DEFAULT_TIMEOUT_MILLIS);

        rocketMQTemplate.asyncSend(destination, message, new SendCallback() {

            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("RocketMQ异步消息发送成功，topic={}，tag={}，businessKey={}，messageId={}，msgId={}，sendStatus={}",
                        request.getTopic(),
                        request.getTag(),
                        request.getBusinessKey(),
                        request.getMessageId(),
                        sendResult.getMsgId(),
                        sendResult.getSendStatus());
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("RocketMQ异步消息发送失败，topic={}，tag={}，businessKey={}，messageId={}",
                        request.getTopic(),
                        request.getTag(),
                        request.getBusinessKey(),
                        request.getMessageId(),
                        throwable);
            }
        }, timeoutMillis);
    }

    /**
     * 单向发送消息
     *
     * @param request 发送请求
     */
    public void sendOneWay(MqSendRequest<?> request) {
        checkRequest(request);

        String destination = buildDestination(request);
        Message<String> message = buildMessage(request);

        rocketMQTemplate.sendOneWay(destination, message);

        log.info("RocketMQ单向消息发送完成，topic={}，tag={}，businessKey={}，messageId={}",
                request.getTopic(),
                request.getTag(),
                request.getBusinessKey(),
                request.getMessageId());
    }

    /**
     * 发送延迟消息
     *
     * @param request 发送请求
     * @return 发送结果
     */
    public SendResult delaySend(MqSendRequest<?> request) {
        checkRequest(request);
        Assert.notNull(request.getDelayLevel(), "延迟级别不能为空");
        Assert.isTrue(request.getDelayLevel() > 0, "延迟级别必须大于0");

        String destination = buildDestination(request);
        Message<String> message = buildMessage(request);
        long timeoutMillis = ObjectUtil.defaultIfNull(request.getTimeoutMillis(), DEFAULT_TIMEOUT_MILLIS);

        SendResult sendResult = rocketMQTemplate.syncSend(
                destination,
                message,
                timeoutMillis,
                request.getDelayLevel()
        );

        log.info("RocketMQ延迟消息发送成功，topic={}，tag={}，businessKey={}，messageId={}，delayLevel={}，msgId={}，sendStatus={}",
                request.getTopic(),
                request.getTag(),
                request.getBusinessKey(),
                request.getMessageId(),
                request.getDelayLevel(),
                sendResult.getMsgId(),
                sendResult.getSendStatus());

        return sendResult;
    }

    /**
     * 发送顺序消息
     *
     * @param request 发送请求
     * @return 发送结果
     */
    public SendResult orderlySend(MqSendRequest<?> request) {
        checkRequest(request);
        Assert.notBlank(request.getShardingKey(), "顺序消息分片键不能为空");

        String destination = buildDestination(request);
        Message<String> message = buildMessage(request);
        long timeoutMillis = ObjectUtil.defaultIfNull(request.getTimeoutMillis(), DEFAULT_TIMEOUT_MILLIS);

        SendResult sendResult = rocketMQTemplate.syncSendOrderly(
                destination,
                message,
                request.getShardingKey(),
                timeoutMillis
        );

        log.info("RocketMQ顺序消息发送成功，topic={}，tag={}，businessKey={}，messageId={}，shardingKey={}，msgId={}，sendStatus={}",
                request.getTopic(),
                request.getTag(),
                request.getBusinessKey(),
                request.getMessageId(),
                request.getShardingKey(),
                sendResult.getMsgId(),
                sendResult.getSendStatus());

        return sendResult;
    }

    /**
     * 构建发送目标
     *
     * @param request 发送请求
     * @return 发送目标
     */
    private String buildDestination(MqSendRequest<?> request) {
        return StrUtil.isBlank(request.getTag()) ? request.getTopic() : request.getTopic() + ":" + request.getTag();
    }

    /**
     * 构建 RocketMQ 消息
     *
     * @param request 发送请求
     * @return Spring 消息
     */
    private Message<String> buildMessage(MqSendRequest<?> request) {
        if (StrUtil.isBlank(request.getMessageId())) {
            request.setMessageId(IdUtil.fastSimpleUUID());
        }

        return MessageBuilder.withPayload(JSONUtil.toJsonStr(request.getPayload()))
                // RocketMQ 消息 Key，建议使用业务唯一键
                .setHeader(RocketMQHeaders.KEYS, request.getBusinessKey())
                // 业务消息 ID，便于日志串联
                .setHeader("messageId", request.getMessageId())
                // 链路追踪 ID，便于跨系统排查
                .setHeader("traceId", request.getTraceId())
                // 来源系统，便于排查消息来源
                .setHeader("sourceSystem", request.getSourceSystem())
                .build();
    }

    /**
     * 校验发送请求
     *
     * @param request 发送请求
     */
    private void checkRequest(MqSendRequest<?> request) {
        Assert.notNull(request, "发送请求不能为空");
        Assert.notBlank(request.getTopic(), "Topic不能为空");
        Assert.notBlank(request.getBusinessKey(), "业务唯一键不能为空");
        Assert.notNull(request.getPayload(), "消息体不能为空");
    }
}
```

业务代码使用示例：

```java
MqSendRequest<OrderPaidMessage> request = MqSendRequest.<OrderPaidMessage>builder()
        .topic(MqTopicConstant.ORDER_EVENT_NORMAL_TOPIC)
        .tag(MqTopicConstant.TAG_ORDER_PAID)
        .businessKey(orderPaidMessage.orderNo())
        .messageId(IdUtil.fastSimpleUUID())
        .traceId(IdUtil.fastSimpleUUID())
        .sourceSystem("order-service")
        .payload(orderPaidMessage)
        .timeoutMillis(3000L)
        .build();

unifiedRocketMqProducer.syncSend(request);
```

统一发送组件建议遵循以下原则：

| 原则         | 说明                                             |
| ------------ | ------------------------------------------------ |
| 统一入口     | 业务代码不直接散落调用 `RocketMQTemplate`。      |
| 统一日志     | 发送成功和失败日志字段保持一致。                 |
| 统一异常     | 发送失败统一转换为业务异常或补偿记录。           |
| 统一消息 Key | 强制要求业务唯一键，不允许空 Key。               |
| 统一序列化   | 统一使用 JSON 序列化，不同服务保持兼容。         |
| 统一配置     | 超时、重试、消息轨迹等配置由组件或配置文件控制。 |

### 定义统一消息模型

统一消息模型用于规范生产者和消费者之间的通信契约。业务消息不应直接发送数据库实体类，也不应每个业务随意定义完全不同的基础字段。建议所有消息都包含消息 ID、版本号、事件类型、事件时间、来源系统、链路追踪 ID、租户 ID 和业务数据。

文件位置：`src/main/java/io/github/atengk/rocketmq/model/BaseMqMessage.java`

```java
package io.github.atengk.rocketmq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * RocketMQ 统一消息模型
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseMqMessage<T> {

    /**
     * 业务消息 ID
     */
    private String messageId;

    /**
     * 消息版本
     */
    private String version;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 事件发生时间
     */
    private LocalDateTime eventTime;

    /**
     * 来源系统
     */
    private String sourceSystem;

    /**
     * 链路追踪 ID
     */
    private String traceId;

    /**
     * 租户 ID
     */
    private String tenantId;

    /**
     * 业务唯一键，例如订单号、支付单号、流水号
     */
    private String businessKey;

    /**
     * 业务数据
     */
    private T data;
}
```

订单消息业务数据单独定义，避免与数据库实体耦合。

文件位置：`src/main/java/io/github/atengk/rocketmq/model/OrderPaidData.java`

```java
package io.github.atengk.rocketmq.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单已支付业务数据
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderPaidData(
        String orderNo,
        String userId,
        String paymentNo,
        BigDecimal payAmount,
        LocalDateTime paidTime
) {
}
```

构建统一消息示例：

```java
BaseMqMessage<OrderPaidData> message = BaseMqMessage.<OrderPaidData>builder()
        .messageId(IdUtil.fastSimpleUUID())
        .version("1.0")
        .eventType("order_paid")
        .eventTime(LocalDateTime.now())
        .sourceSystem("order-service")
        .traceId(IdUtil.fastSimpleUUID())
        .tenantId("default")
        .businessKey(orderPaidData.orderNo())
        .data(orderPaidData)
        .build();
```

统一消息模型字段建议如下：

| 字段           | 是否必填 | 说明                                  |
| -------------- | -------- | ------------------------------------- |
| `messageId`    | 是       | 业务消息 ID，用于日志串联和补偿处理。 |
| `version`      | 是       | 消息结构版本，例如 `1.0`、`1.1`。     |
| `eventType`    | 是       | 事件类型，建议与 Tag 保持一致。       |
| `eventTime`    | 是       | 业务事件发生时间。                    |
| `sourceSystem` | 是       | 来源系统，例如 `order-service`。      |
| `traceId`      | 是       | 链路追踪 ID。                         |
| `tenantId`     | 按需     | 多租户系统建议必填。                  |
| `businessKey`  | 是       | 业务唯一键，用于幂等、排查和补偿。    |
| `data`         | 是       | 业务数据对象。                        |

消息模型设计注意事项：

| 注意事项         | 说明                                               |
| ---------------- | -------------------------------------------------- |
| 不发送 Entity    | Entity 与数据库结构耦合，字段变化会影响消费者。    |
| 不发送超大对象   | 大对象应落库或对象存储，消息中只传引用 ID。        |
| 不随意删除字段   | 已发布字段删除会影响旧消费者。                     |
| 新增字段保持兼容 | 新增字段应允许旧消费者忽略。                       |
| 事件类型稳定     | `eventType` 一旦上线，不建议改变语义。             |
| 金额类型统一     | 金额建议统一使用 `BigDecimal` 或最小货币单位整数。 |
| 时间格式统一     | 建议使用 ISO 时间格式或统一序列化配置。            |

### 消费者幂等设计

消费者幂等是生产环境必须具备的能力。RocketMQ 消息可能因为网络异常、消费超时、消费者重启、Broker 重试、人工补偿等原因被重复投递。消费端必须保证同一业务动作重复执行时，最终业务结果只生效一次。

推荐的幂等层次如下：

```text
第一层：业务状态机校验
        |
第二层：消费记录表或数据库唯一索引
        |
第三层：Redis 幂等键加速去重
        |
第四层：死信队列与人工补偿
```

核心业务建议使用数据库消费记录表作为最终幂等依据。Redis 可以作为快速去重手段，但不能替代数据库约束。

文件位置：`db/mq_consume_record.sql`

```sql
-- RocketMQ 消费记录表：用于消费幂等、异常排查和补偿审计
CREATE TABLE mq_consume_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    topic VARCHAR(255) NOT NULL COMMENT 'Topic',
    tag VARCHAR(255) DEFAULT NULL COMMENT 'Tag',
    consumer_group VARCHAR(255) NOT NULL COMMENT '消费组',
    business_key VARCHAR(255) NOT NULL COMMENT '业务唯一键',
    message_id VARCHAR(128) DEFAULT NULL COMMENT '业务消息ID',
    msg_id VARCHAR(128) DEFAULT NULL COMMENT 'RocketMQ消息ID',
    consume_status VARCHAR(32) NOT NULL COMMENT '消费状态：PROCESSING、SUCCESS、FAILED',
    retry_times INT DEFAULT 0 COMMENT '重试次数',
    error_message VARCHAR(1024) DEFAULT NULL COMMENT '异常信息',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_consume_business (
        topic,
        tag,
        consumer_group,
        business_key
    ),
    KEY idx_msg_id (msg_id),
    KEY idx_consume_status (consume_status)
) COMMENT='RocketMQ消费记录表';
```

定义消费幂等服务接口。

文件位置：`src/main/java/io/github/atengk/rocketmq/service/ConsumerIdempotentService.java`

```java
package io.github.atengk.rocketmq.service;

/**
 * 消费者幂等服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface ConsumerIdempotentService {

    /**
     * 尝试开始消费
     *
     * @param topic         Topic
     * @param tag           Tag
     * @param consumerGroup 消费组
     * @param businessKey   业务唯一键
     * @param messageId     业务消息ID
     * @param msgId         RocketMQ消息ID
     * @return true 表示可以消费，false 表示重复消息
     */
    boolean tryStartConsume(String topic,
                            String tag,
                            String consumerGroup,
                            String businessKey,
                            String messageId,
                            String msgId);

    /**
     * 标记消费成功
     *
     * @param topic         Topic
     * @param tag           Tag
     * @param consumerGroup 消费组
     * @param businessKey   业务唯一键
     */
    void markSuccess(String topic, String tag, String consumerGroup, String businessKey);

    /**
     * 标记消费失败
     *
     * @param topic         Topic
     * @param tag           Tag
     * @param consumerGroup 消费组
     * @param businessKey   业务唯一键
     * @param errorMessage  异常信息
     */
    void markFailed(String topic, String tag, String consumerGroup, String businessKey, String errorMessage);
}
```

下面给出基于数据库唯一索引的幂等实现。核心逻辑是先插入消费记录，如果唯一索引冲突，说明该业务消息已经被处理或正在处理。

文件位置：`src/main/java/io/github/atengk/rocketmq/service/impl/JdbcConsumerIdempotentServiceImpl.java`

```java
package io.github.atengk.rocketmq.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.rocketmq.service.ConsumerIdempotentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * JDBC 消费者幂等服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JdbcConsumerIdempotentServiceImpl implements ConsumerIdempotentService {

    private static final String STATUS_PROCESSING = "PROCESSING";

    private static final String STATUS_SUCCESS = "SUCCESS";

    private static final String STATUS_FAILED = "FAILED";

    private final JdbcTemplate jdbcTemplate;

    /**
     * 尝试开始消费
     *
     * @param topic         Topic
     * @param tag           Tag
     * @param consumerGroup 消费组
     * @param businessKey   业务唯一键
     * @param messageId     业务消息ID
     * @param msgId         RocketMQ消息ID
     * @return true 表示可以消费，false 表示重复消息
     */
    @Override
    public boolean tryStartConsume(String topic,
                                   String tag,
                                   String consumerGroup,
                                   String businessKey,
                                   String messageId,
                                   String msgId) {
        String sql = """
                INSERT INTO mq_consume_record
                (topic, tag, consumer_group, business_key, message_id, msg_id, consume_status, retry_times, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        String now = LocalDateTimeUtil.formatNormal(LocalDateTime.now());

        try {
            jdbcTemplate.update(sql,
                    topic,
                    tag,
                    consumerGroup,
                    businessKey,
                    messageId,
                    msgId,
                    STATUS_PROCESSING,
                    0,
                    now,
                    now);

            log.info("消息消费幂等记录创建成功，topic={}，tag={}，consumerGroup={}，businessKey={}，msgId={}",
                    topic, tag, consumerGroup, businessKey, msgId);
            return true;
        } catch (DuplicateKeyException exception) {
            String status = queryConsumeStatus(topic, tag, consumerGroup, businessKey);
            log.info("消息已存在消费记录，跳过重复消费，topic={}，tag={}，consumerGroup={}，businessKey={}，status={}",
                    topic, tag, consumerGroup, businessKey, status);
            return false;
        }
    }

    /**
     * 标记消费成功
     *
     * @param topic         Topic
     * @param tag           Tag
     * @param consumerGroup 消费组
     * @param businessKey   业务唯一键
     */
    @Override
    public void markSuccess(String topic, String tag, String consumerGroup, String businessKey) {
        updateStatus(topic, tag, consumerGroup, businessKey, STATUS_SUCCESS, null);
        log.info("消息消费幂等记录已标记成功，topic={}，tag={}，consumerGroup={}，businessKey={}",
                topic, tag, consumerGroup, businessKey);
    }

    /**
     * 标记消费失败
     *
     * @param topic         Topic
     * @param tag           Tag
     * @param consumerGroup 消费组
     * @param businessKey   业务唯一键
     * @param errorMessage  异常信息
     */
    @Override
    public void markFailed(String topic, String tag, String consumerGroup, String businessKey, String errorMessage) {
        updateStatus(topic, tag, consumerGroup, businessKey, STATUS_FAILED, StrUtil.maxLength(errorMessage, 1000));
        log.warn("消息消费幂等记录已标记失败，topic={}，tag={}，consumerGroup={}，businessKey={}，errorMessage={}",
                topic, tag, consumerGroup, businessKey, StrUtil.maxLength(errorMessage, 300));
    }

    /**
     * 查询消费状态
     *
     * @param topic         Topic
     * @param tag           Tag
     * @param consumerGroup 消费组
     * @param businessKey   业务唯一键
     * @return 消费状态
     */
    private String queryConsumeStatus(String topic, String tag, String consumerGroup, String businessKey) {
        String sql = """
                SELECT consume_status
                FROM mq_consume_record
                WHERE topic = ? AND tag = ? AND consumer_group = ? AND business_key = ?
                LIMIT 1
                """;

        return jdbcTemplate.queryForObject(sql, String.class, topic, tag, consumerGroup, businessKey);
    }

    /**
     * 更新消费状态
     *
     * @param topic         Topic
     * @param tag           Tag
     * @param consumerGroup 消费组
     * @param businessKey   业务唯一键
     * @param status        消费状态
     * @param errorMessage  异常信息
     */
    private void updateStatus(String topic,
                              String tag,
                              String consumerGroup,
                              String businessKey,
                              String status,
                              String errorMessage) {
        String sql = """
                UPDATE mq_consume_record
                SET consume_status = ?, error_message = ?, updated_at = ?
                WHERE topic = ? AND tag = ? AND consumer_group = ? AND business_key = ?
                """;

        jdbcTemplate.update(sql,
                status,
                errorMessage,
                LocalDateTimeUtil.formatNormal(LocalDateTime.now()),
                topic,
                tag,
                consumerGroup,
                businessKey);
    }
}
```

消费者使用幂等服务时，应遵循“先占用、再执行业务、成功确认、失败标记并抛出异常”的流程。

```text
收到消息
  |
  v
根据 Topic + Tag + ConsumerGroup + BusinessKey 创建消费记录
  |
  +-- 创建失败：说明重复消费，直接返回成功
  |
  +-- 创建成功：执行业务逻辑
          |
          +-- 成功：标记 SUCCESS
          |
          +-- 失败：标记 FAILED，抛出异常触发 RocketMQ 重试
```

幂等设计建议如下：

| 场景         | 推荐方案                                                    |
| ------------ | ----------------------------------------------------------- |
| 支付流水     | 数据库唯一索引，唯一键为支付单号或流水号。                  |
| 发券         | 发券记录表唯一索引，唯一键为活动 ID + 用户 ID + 券模板 ID。 |
| 库存扣减     | 库存流水表唯一索引，唯一键为业务单号 + SKU。                |
| 订单状态流转 | 状态机校验 + 消费记录表。                                   |
| 缓存刷新     | Redis 幂等键或直接允许重复刷新。                            |
| 通知类消息   | 可按业务通知 ID 做轻量幂等。                                |

### 配置隔离与多环境管理

RocketMQ 配置必须按环境隔离。开发、测试、预发和生产环境不能共用 Topic、Consumer Group 或 NameServer。尤其是 Consumer Group，如果不同环境误连同一个集群，可能导致测试消费者消费生产消息，或者生产消费者消费测试消息。

推荐配置文件结构如下：

```text
src/main/resources/
├── application.yml
├── application-dev.yml
├── application-test.yml
├── application-pre.yml
└── application-prod.yml
```

公共配置放在 `application.yml`，只保留通用参数。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  application:
    # 当前应用名称
    name: order-service

rocketmq:
  producer:
    # 默认发送超时时间，单位毫秒
    send-message-timeout: 3000

    # 同步发送失败重试次数
    retry-times-when-send-failed: 2

    # 异步发送失败重试次数
    retry-times-when-send-async-failed: 2

    # 发送失败后是否尝试其他 Broker
    retry-next-server: true

    # 是否开启消息轨迹
    enable-msg-trace: true
```

开发环境配置使用本地 RocketMQ。

文件位置：`src/main/resources/application-dev.yml`

```yaml
rocketmq:
  # 开发环境 NameServer
  name-server: 127.0.0.1:9876

  producer:
    # 开发环境生产者组
    group: order-service-dev-producer-group
```

测试环境配置使用测试集群，Topic 和 Consumer Group 必须带测试环境标识或由独立集群隔离。

文件位置：`src/main/resources/application-test.yml`

```yaml
rocketmq:
  # 测试环境 NameServer
  name-server: 10.20.1.11:9876;10.20.1.12:9876

  producer:
    # 测试环境生产者组
    group: order-service-test-producer-group
```

生产环境配置不能写死敏感信息，ACL 密钥应通过环境变量、配置中心或密钥管理系统注入。

文件位置：`src/main/resources/application-prod.yml`

```yaml
rocketmq:
  # 生产环境 NameServer
  name-server: ${ROCKETMQ_NAME_SERVER}

  producer:
    # 生产环境生产者组
    group: order-service-prod-producer-group

    # 生产环境 AccessKey，从环境变量或配置中心注入
    access-key: ${ROCKETMQ_ACCESS_KEY}

    # 生产环境 SecretKey，从环境变量或配置中心注入
    secret-key: ${ROCKETMQ_SECRET_KEY}

    # 生产环境开启消息轨迹，便于排查
    enable-msg-trace: true

  consumer:
    # 消费者 AccessKey
    access-key: ${ROCKETMQ_ACCESS_KEY}

    # 消费者 SecretKey
    secret-key: ${ROCKETMQ_SECRET_KEY}
```

Topic 和 Consumer Group 命名建议按环境隔离：

| 环境 | Topic 示例                      | Consumer Group 示例              |
| ---- | ------------------------------- | -------------------------------- |
| 开发 | `dev-order-event-normal-topic`  | `dev-order-paid-consumer-group`  |
| 测试 | `test-order-event-normal-topic` | `test-order-paid-consumer-group` |
| 预发 | `pre-order-event-normal-topic`  | `pre-order-paid-consumer-group`  |
| 生产 | `order-event-normal-topic`      | `order-paid-consumer-group`      |

如果公司使用不同 RocketMQ 集群隔离环境，生产环境 Topic 可以不加 `prod` 前缀；如果多个环境共用同一集群，Topic 和 Consumer Group 必须加环境前缀，避免误消费。

多环境管理建议如下：

| 配置项         | 建议                                   |
| -------------- | -------------------------------------- |
| NameServer     | 不同环境使用不同地址，避免误连。       |
| Topic          | 不同环境隔离命名或独立集群隔离。       |
| Consumer Group | 不同环境必须隔离。                     |
| AK/SK          | 不同环境使用不同权限账号。             |
| 自动创建 Topic | 本地可开启，生产禁止。                 |
| 测试接口       | 仅在 `dev`、`test` Profile 启用。      |
| 密钥配置       | 通过环境变量、配置中心或 Secret 注入。 |

### 生产环境使用注意事项

生产环境使用 RocketMQ 时，关注点不只是代码能发送和消费，还包括 Topic 治理、容量规划、监控告警、消息堆积、死信处理、补偿机制、权限控制和版本升级。

生产环境使用建议如下：

| 类别       | 注意事项                                                     |
| ---------- | ------------------------------------------------------------ |
| Topic 管理 | Topic 应提前创建，明确消息类型、队列数、权限和负责人。       |
| 消费组管理 | Consumer Group 命名清晰，同一组内消费逻辑保持一致。          |
| 生产者     | 核心消息发送失败必须有补偿记录，不允许只打印日志。           |
| 消费者     | 所有消费者必须幂等，失败时按可恢复性决定是否重试。           |
| 顺序消息   | 只保证分区顺序，不要误用为全局顺序。                         |
| 事务消息   | 只解决本地事务与消息发送一致性，不保证消费端成功。           |
| 延迟消息   | 消费时必须重新查询业务状态，避免误处理过期状态。             |
| 死信消息   | 必须有告警、巡检、人工处理或自动补偿流程。                   |
| 监控告警   | 关注消息堆积、消费延迟、发送失败、消费失败、死信数量。       |
| 安全权限   | 生产环境开启 ACL，按应用授权最小 Topic 权限。                |
| 日志规范   | 日志必须包含 Topic、Tag、Key、MsgId、业务 ID、重试次数。     |
| 容量规划   | 根据峰值 TPS、消息大小、消费耗时和队列数规划 Broker 与消费者实例。 |

生产环境禁止事项：

```text
1. 禁止生产环境开启自动创建 Topic
2. 禁止多个无关业务共用同一个 Consumer Group
3. 禁止核心业务消息不设置业务 Key
4. 禁止消费者无幂等直接处理核心业务
5. 禁止消费失败后吞异常并返回成功
6. 禁止广播模式处理扣款、扣库存、发券等全局只应执行一次的业务
7. 禁止在消息体中传输超大对象、敏感明文或完整数据库实体
8. 禁止生产环境暴露测试发送接口
9. 禁止将 AK/SK 写死在代码或 Git 仓库中
10. 禁止死信消息无人处理
```

生产环境上线检查清单如下：

| 检查项                                | 是否必须 |
| ------------------------------------- | -------- |
| Topic 已创建并确认消息类型            | 是       |
| Consumer Group 已创建或确认订阅关系   | 是       |
| Producer 配置了正确 NameServer        | 是       |
| Consumer 配置了正确 Topic、Tag、Group | 是       |
| 消息 Key 使用业务唯一键               | 是       |
| 消息体字段完成兼容性评审              | 是       |
| 消费端完成幂等设计                    | 是       |
| 发送失败有补偿方案                    | 是       |
| 消费失败有重试和死信处理方案          | 是       |
| 监控告警已配置                        | 是       |
| ACL 权限已验证                        | 是       |
| 压测结果满足峰值流量                  | 是       |
| 回滚方案已准备                        | 是       |

建议监控指标如下：

| 指标              | 说明                                       |
| ----------------- | ------------------------------------------ |
| 发送成功率        | Producer 发送成功数量与失败数量。          |
| 发送耗时          | Producer 到 Broker 的发送耗时。            |
| 消费 TPS          | Consumer 每秒消费消息数量。                |
| 消费失败数        | 消费者业务处理失败数量。                   |
| 消费重试次数      | 消息重复投递次数。                         |
| 消息堆积量        | Topic 或 Consumer Group 下未消费消息数量。 |
| 消费延迟          | 消息从发送到被消费的时间差。               |
| 死信消息数        | 进入 DLQ 的消息数量。                      |
| Broker 磁盘使用率 | Broker 存储空间是否接近阈值。              |
| Broker 存活状态   | Broker、NameServer、Proxy 是否可用。       |

最终落地建议是：RocketMQ 接入项目后，应优先沉淀统一发送组件、统一消息模型、统一 Topic/Tag 命名、统一幂等方案、统一日志规范和统一补偿流程。业务代码只负责表达业务事件，基础组件负责保障消息发送、消费、排查和治理的一致性。