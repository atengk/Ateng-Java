# RabbitMQ 开发使用文档

本文档用于整理 RabbitMQ 在 Java 后端开发中的核心概念、集成方式、常用消息模型、可靠性设计以及典型业务实践。当前章节重点说明文档定位和 RabbitMQ 基础概念，为后续 Spring Boot 集成、消息发送、消息消费、死信队列、延迟队列和生产可靠性设计打基础。

## 文档概述

本章节用于说明本文档的适用对象、技术栈边界和学习开发目标。RabbitMQ 本身是基于 AMQP 协议的消息代理，Spring Boot 提供了 `spring-boot-starter-amqp` 等集成能力，可以通过外部配置、`RabbitTemplate`、`@RabbitListener` 等方式完成发送、接收和监听容器配置。:contentReference[oaicite:1]{index=1}

### 适用范围

本文档适用于 Java 后端开发人员在 Spring Boot 项目中使用 RabbitMQ 完成异步解耦、削峰填谷、事件通知、延迟处理、重试补偿、死信兜底等业务场景。

本文档重点面向开发使用，不作为 RabbitMQ 集群运维手册。也就是说，本文会关注如何在项目中定义交换机、队列、绑定关系、生产者、消费者、消息确认、异常处理和业务落地方式；但不会深入展开 RabbitMQ 集群高可用、镜像队列迁移、Quorum Queue 调优、磁盘告警、网络分区治理等偏运维主题。

推荐阅读对象包括：

| 角色             | 适用说明                                                     |
| ---------------- | ------------------------------------------------------------ |
| Java 后端开发    | 掌握 Spring Boot 项目中 RabbitMQ 的开发、配置和问题排查      |
| 初级开发人员     | 建立 Producer、Consumer、Exchange、Queue、Binding 等基础认知 |
| 业务系统开发人员 | 将消息队列用于订单、通知、日志、任务、数据同步等业务场景     |
| 接口联调人员     | 理解消息发送、消费、重试、死信和延迟处理的验证方式           |

本文档默认读者已经具备 Spring Boot、Maven、基础 Linux 命令和 REST 接口开发经验。对于 RabbitMQ 尚未熟悉的读者，可以先从基础概念章节开始，再逐步进入开发环境、集成配置和业务实践章节。

### 技术栈说明

本文档默认以 JDK 21 和 Spring Boot 3 为主要开发环境，使用 Spring AMQP 对 RabbitMQ 进行集成。Spring AMQP 提供了面向 AMQP 消息开发的抽象能力，常用能力包括 `RabbitTemplate`、监听容器、`RabbitAdmin`、交换机队列绑定声明等。:contentReference[oaicite:2]{index=2}

| 技术                | 说明                                                         |
| ------------------- | ------------------------------------------------------------ |
| JDK 21              | 项目运行与编译环境，适用于 Spring Boot 3.x 项目              |
| Spring Boot 3.x     | 后端基础框架，负责自动配置、Bean 管理、配置加载和应用启动    |
| Spring AMQP         | Spring 对 AMQP 消息模型的封装，提供模板、监听器和管理组件    |
| RabbitMQ            | 消息代理服务，负责接收、路由、存储和投递消息                 |
| Maven               | 项目依赖管理和构建工具                                       |
| Docker              | 本地快速启动 RabbitMQ 服务的推荐方式                         |
| RabbitMQ Management | RabbitMQ 管理控制台，用于查看交换机、队列、绑定、连接、通道和消息堆积情况 |

在 Spring Boot 项目中，RabbitMQ 连接信息通常通过 `spring.rabbitmq.*` 进行配置，例如主机、端口、账号、密码和虚拟主机等。Spring Boot 也会根据项目依赖自动配置常用 AMQP 组件，例如 `AmqpTemplate`、`AmqpAdmin`、监听容器工厂和消息转换器。:contentReference[oaicite:3]{index=3}

本文档后续代码示例将默认使用以下约定：

| 项目            | 默认约定                             |
| --------------- | ------------------------------------ |
| 基础包名        | `io.github.atengk`                   |
| 配置文件        | `src/main/resources/application.yml` |
| 消息发送组件    | `producer` 或 `sender` 包            |
| 消息消费组件    | `consumer` 或 `listener` 包          |
| RabbitMQ 配置类 | `config` 包                          |
| 常量类          | `constant` 包                        |
| 消息实体        | `model`、`dto` 或 `message` 包       |

### 学习与开发目标

学习 RabbitMQ 的目标不是单纯掌握 API 调用，而是能够根据业务场景设计合适的消息模型，并保证消息在生产、路由、消费、异常处理和补偿链路中的行为可控。

完成本文档学习后，应能够达到以下目标：

| 目标           | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| 理解基础模型   | 能够说明 Producer、Consumer、Broker、Exchange、Queue、Binding 的职责 |
| 掌握路由关系   | 能够区分 Routing Key、Binding Key 以及不同交换机的路由行为   |
| 完成项目集成   | 能够在 Spring Boot 3 项目中配置 RabbitMQ 并完成消息发送和消费 |
| 处理消费异常   | 能够设计手动确认、异常拒绝、重试、死信和兜底处理逻辑         |
| 提升消息可靠性 | 能够使用生产者确认、返回机制、持久化和幂等设计降低消息丢失风险 |
| 支持业务落地   | 能够将 RabbitMQ 应用于订单超时关闭、异步通知、日志收集、数据同步等场景 |
| 具备排查能力   | 能够通过管理控制台、日志和配置定位消息发送失败、重复消费、消息堆积等问题 |

在实际开发中，RabbitMQ 的重点不只是“把消息发出去”，而是要保证消息的生命周期清晰：谁发送、发送到哪个交换机、通过什么规则路由到哪个队列、由哪个消费者处理、失败后如何重试、最终无法处理时如何兜底。

## RabbitMQ 基础概念

本章节用于建立 RabbitMQ 的核心概念模型。RabbitMQ 的核心流程可以概括为：生产者发送消息到交换机，交换机根据绑定关系和路由规则将消息投递到队列，消费者从队列中获取消息并完成业务处理。Exchange 的职责是路由消息，Queue 的职责是保存等待消费的消息，Binding 用于建立 Exchange 与 Queue 或 Exchange 与 Exchange 之间的路由关系。

### 消息队列核心作用

消息队列是一种用于系统之间传递消息的中间件机制。它将消息发送方和消息处理方解耦，使发送方不需要同步等待处理方完成业务逻辑，从而提高系统的吞吐能力、可扩展性和容错能力。

在业务系统中，RabbitMQ 常用于以下场景：

| 场景     | 说明                                                   |
| -------- | ------------------------------------------------------ |
| 异步解耦 | 下单成功后发送短信、邮件、站内信，不阻塞主交易链路     |
| 削峰填谷 | 高并发请求先写入消息队列，消费者按处理能力逐步消费     |
| 事件驱动 | 订单创建、支付成功、库存变更等事件通过消息通知下游系统 |
| 重试补偿 | 第三方接口调用失败后，通过消息重试机制进行补偿         |
| 延迟处理 | 订单超时关闭、优惠券过期提醒、延迟通知等场景           |
| 日志收集 | 业务日志、操作日志异步写入存储或分析系统               |

消息队列的价值主要体现在“削弱系统之间的同步依赖”。例如，订单服务在创建订单后，不需要直接调用短信服务、积分服务、库存同步服务，而是发送一条订单事件消息。后续各业务消费者根据自己的队列进行处理，即使某个消费者短暂不可用，也不会直接影响订单创建主流程。

但消息队列也会带来新的复杂度，例如消息重复消费、消息丢失、消息顺序、消息堆积、消费失败、死信处理和最终一致性问题。因此，在开发 RabbitMQ 时，需要同时关注功能实现和可靠性设计。

### Producer、Consumer 与 Broker

Producer、Consumer 和 Broker 是消息队列中最基础的三个角色。Producer 负责发送消息，Consumer 负责消费消息，Broker 负责接收、存储、路由和投递消息。

| 角色     | 中文名称 | 主要职责                                        |
| -------- | -------- | ----------------------------------------------- |
| Producer | 生产者   | 创建消息并发送到 RabbitMQ                       |
| Consumer | 消费者   | 从队列中获取消息并执行业务处理                  |
| Broker   | 消息代理 | RabbitMQ 服务端，负责消息接收、路由、存储和投递 |

在 RabbitMQ 中，生产者一般不会直接把消息发送到队列，而是先发送到 Exchange。Exchange 根据类型、Routing Key、Binding Key 和绑定关系决定消息最终进入哪些 Queue。消费者再监听指定 Queue，获取消息并处理业务逻辑。RabbitMQ 官方文档也将 Exchange 描述为发布者发送消息的实体，它负责将消息路由到队列、流或其他交换机。

典型流程如下：

```text
Producer -> Exchange -> Binding -> Queue -> Consumer
```

在 Spring Boot 项目中，常见映射关系如下：

| RabbitMQ 概念 | Spring Boot 开发组件                                   |
| ------------- | ------------------------------------------------------ |
| Producer      | 使用 `RabbitTemplate` 或业务封装的 Producer 组件       |
| Consumer      | 使用 `@RabbitListener` 标注的监听方法                  |
| Broker        | RabbitMQ 服务实例                                      |
| Queue         | `Queue` Bean 或在 RabbitMQ 控制台中声明的队列          |
| Exchange      | `DirectExchange`、`FanoutExchange`、`TopicExchange` 等 |
| Binding       | `Binding` Bean 或控制台绑定关系                        |

开发时需要特别注意：生产者只关心“消息发送到哪里”，消费者只关心“监听哪个队列”，而 RabbitMQ Broker 负责根据拓扑结构完成中间路由。因此，交换机、队列和绑定关系的设计是否合理，会直接影响消息能否被正确投递。

### Exchange、Queue 与 Binding

Exchange、Queue 和 Binding 是 RabbitMQ 路由模型的核心。Exchange 负责接收生产者发送的消息并执行路由逻辑；Queue 负责保存消息，等待消费者消费；Binding 则负责描述 Exchange 和 Queue 之间的绑定关系。RabbitMQ 中 Exchange 被声明后，本身并不知道要投递到哪些队列，需要通过 Binding 建立路由规则。([RabbitMQ](https://www.rabbitmq.com/docs/next/exchanges?utm_source=chatgpt.com))

| 概念         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| Exchange     | 交换机，负责接收生产者消息并根据规则路由                     |
| Queue        | 队列，负责保存等待消费的消息                                 |
| Binding      | 绑定关系，负责连接 Exchange 与 Queue                         |
| Binding 参数 | 不同交换机类型可以使用不同参数，例如 Routing Key、Headers 参数等 |

可以将三者理解为：

```text
Exchange：消息入口和路由器
Queue：消息存储容器
Binding：路由规则
```

例如，订单服务发送一条订单创建消息：

```text
Exchange：order.exchange
Routing Key：order.created
Queue：order.created.queue
Binding Key：order.created
```

当消息发送到 `order.exchange` 后，RabbitMQ 会检查该交换机下的绑定关系。如果某个队列使用 `order.created` 作为 Binding Key 绑定到该交换机，那么这条消息就会被路由到对应队列中。

常见设计方式如下：

| 业务场景     | Exchange               | Queue                 | Binding         |
| ------------ | ---------------------- | --------------------- | --------------- |
| 订单创建通知 | `order.exchange`       | `order.created.queue` | `order.created` |
| 支付成功通知 | `pay.exchange`         | `pay.success.queue`   | `pay.success`   |
| 用户注册事件 | `user.exchange`        | `user.register.queue` | `user.register` |
| 日志广播     | `log.fanout.exchange`  | 多个日志队列          | Fanout 绑定     |
| 业务事件分类 | `event.topic.exchange` | 多个事件队列          | Topic 通配绑定  |

Exchange 并不负责长时间保存消息，Queue 才是消息等待消费的主要位置。因此，如果交换机没有匹配到任何队列，消息可能无法被正常投递。生产环境中通常需要结合生产者返回机制、备用交换机或监控告警来避免消息被静默丢弃。

### Routing Key 与 Binding Key

Routing Key 是生产者发送消息时携带的路由标识，Binding Key 是队列绑定到交换机时设置的匹配规则。RabbitMQ 会根据交换机类型，使用 Routing Key 和 Binding Key 判断消息应该进入哪些队列。

| 概念        | 所属阶段     | 说明                     |
| ----------- | ------------ | ------------------------ |
| Routing Key | 发送消息阶段 | 生产者发送消息时指定     |
| Binding Key | 绑定队列阶段 | 队列绑定交换机时指定     |
| 匹配规则    | 消息路由阶段 | 由交换机类型决定如何匹配 |

在 Direct Exchange 中，Routing Key 和 Binding Key 必须完全相等才会路由成功。RabbitMQ 官方文档说明 Direct Exchange 会基于绑定路由键的精确等值匹配进行路由。([RabbitMQ](https://www.rabbitmq.com/docs/next/exchanges?utm_source=chatgpt.com))

示例：

```text
Exchange：order.direct.exchange
Routing Key：order.created
Binding Key：order.created
路由结果：匹配成功
```

如果 Routing Key 是 `order.cancelled`，而 Binding Key 是 `order.created`，则不会匹配成功。

在 Topic Exchange 中，Binding Key 支持通配符匹配：

| 通配符 | 含义               |
| ------ | ------------------ |
| `*`    | 匹配一个单词       |
| `#`    | 匹配零个或多个单词 |

示例：

```text
Routing Key：order.pay.success
Binding Key：order.*.success
路由结果：匹配成功

Routing Key：order.pay.success
Binding Key：order.#
路由结果：匹配成功
```

RabbitMQ 的 Topic Exchange 会将路由键按 `.` 分隔成多个片段，`*` 匹配一个片段，`#` 匹配零个或多个片段。([RabbitMQ](https://www.rabbitmq.com/docs/next/exchanges?utm_source=chatgpt.com))

在实际开发中，Routing Key 应该具备明确的业务语义，建议采用以下格式：

```text
业务域.业务动作.业务状态
```

例如：

```text
order.create.success
order.pay.success
order.close.timeout
user.register.success
stock.deduct.failed
```

这种命名方式便于后续扩展 Topic Exchange，也方便在控制台、日志和监控中快速定位消息来源和业务含义。

### Virtual Host 与权限模型

Virtual Host，简称 vhost，是 RabbitMQ 中用于资源隔离的逻辑空间。每个 vhost 内部都有自己独立的 Exchange、Queue、Binding、权限和连接上下文。RabbitMQ 文档说明，Exchange 等拓扑元素都属于某一个 Virtual Host，不同 vhost 中可以存在同名资源，但它们彼此独立。([RabbitMQ](https://www.rabbitmq.com/docs/next/exchanges?utm_source=chatgpt.com))

可以将 vhost 理解为 RabbitMQ 中的“逻辑命名空间”：

```text
/dev
/test
/prod
/order
/message
```

在开发环境中，常见做法是为不同环境或不同业务系统创建独立 vhost：

| vhost      | 使用场景         |
| ---------- | ---------------- |
| `/dev`     | 本地开发环境     |
| `/test`    | 测试环境         |
| `/prod`    | 生产环境         |
| `/order`   | 订单系统专用     |
| `/message` | 消息通知系统专用 |

RabbitMQ 权限控制是基于用户和 vhost 进行授权的。用户连接 RabbitMQ 时，需要指定用户名、密码和目标 vhost；RabbitMQ 会先检查该用户是否有访问对应 vhost 的权限，再检查其对资源的具体操作权限。RabbitMQ 权限模型主要包括 `configure`、`write` 和 `read` 三类权限。([RabbitMQ](https://www.rabbitmq.com/docs/access-control?utm_source=chatgpt.com))

| 权限      | 说明                                               |
| --------- | -------------------------------------------------- |
| configure | 创建、删除或修改 Exchange、Queue、Binding 等资源   |
| write     | 向 Exchange 或 Queue 写入消息                      |
| read      | 从 Queue 读取消息，或在某些绑定操作中读取 Exchange |

常见授权示例：

```text
用户：order_user
vhost：/order
configure：^order\..*
write：^order\..*
read：^order\..*
```

这表示 `order_user` 只能在 `/order` 这个 vhost 下操作以 `order.` 开头的资源。生产环境中不建议直接使用 `guest/guest` 作为业务账号，因为 RabbitMQ 默认的 `guest` 用户通常只允许本机连接，并且官方也建议生产环境创建新的用户和凭据。([RabbitMQ](https://www.rabbitmq.com/docs/access-control?utm_source=chatgpt.com))

在 Spring Boot 中，连接 vhost 通常通过配置项指定：

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: order_user
    password: order_password
    virtual-host: /order
```

上面的配置表示当前服务启动后，会使用 `order_user` 用户连接 RabbitMQ 的 `/order` vhost。后续所有通过该连接声明和访问的交换机、队列、绑定关系，都默认位于 `/order` 这个逻辑空间内。

开发时建议遵循以下原则：

| 原则         | 说明                                             |
| ------------ | ------------------------------------------------ |
| 环境隔离     | 开发、测试、生产使用不同 vhost，避免误发误消费   |
| 最小权限     | 业务用户只授予必要的 configure、write、read 权限 |
| 账号隔离     | 不同业务系统使用不同 RabbitMQ 用户               |
| 命名规范     | Exchange、Queue、Routing Key 使用统一业务前缀    |
| 禁用默认账号 | 生产环境不要使用默认 `guest/guest` 账号          |
| 配置外置     | 用户名、密码、vhost 通过环境变量或配置中心管理   |

Virtual Host 和权限模型是 RabbitMQ 多业务、多环境隔离的基础。设计清晰的 vhost 和用户权限，可以降低误操作风险，也能让消息资源的管理边界更加明确。

## 开发环境准备

本章节用于说明 RabbitMQ 开发前需要准备的基础环境，包括 JDK21、Spring Boot 3 项目结构、RabbitMQ 服务启动方式、管理控制台访问方式以及 Maven 依赖配置。后续章节中的消息发送、消息消费、可靠性设计和业务实践，都默认基于本章节中的开发环境展开。

### JDK21 环境要求

本文档默认使用 JDK 21 作为 Java 运行环境。Spring Boot 3 项目已经全面迁移到 Jakarta EE 体系，建议使用 JDK 17 及以上版本进行开发；在实际新项目中，优先使用 JDK 21 可以获得更长周期的语言特性支持和更好的运行时能力。

开发前需要确认本地已经正确安装 JDK 21，并配置好 `JAVA_HOME` 和 `PATH` 环境变量。

检查当前 Java 版本。

```bash
# 查看 Java 运行时版本
java -version

# 查看 Java 编译器版本
javac -version
```

正常情况下，应能看到类似下面的输出。

```text
java version "21.x.x"
Java(TM) SE Runtime Environment
Java HotSpot(TM) 64-Bit Server VM
```

如果使用 Linux 或 macOS，可以通过以下命令检查 `JAVA_HOME`。

```bash
# 查看当前 JAVA_HOME 配置
echo $JAVA_HOME

# 查看 java 命令实际路径
which java
```

如果使用 Windows，可以在命令行中执行以下命令。

```cmd
:: 查看当前 JAVA_HOME 配置
echo %JAVA_HOME%

:: 查看 java 命令实际路径
where java
```

JDK 21 环境建议满足以下要求：

| 项目        | 要求                                |
| ----------- | ----------------------------------- |
| JDK 版本    | JDK 21                              |
| 编译版本    | Java 21                             |
| 项目编码    | UTF-8                               |
| 构建工具    | Maven 3.8+                          |
| Spring Boot | Spring Boot 3.x                     |
| IDE         | IntelliJ IDEA 2023+ 或同类 Java IDE |

在 Maven 项目中，需要统一指定 Java 编译版本。

```xml
<properties>
    <!-- 项目源码编码 -->
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

    <!-- Java 编译版本 -->
    <java.version>21</java.version>

    <!-- Maven 编译插件使用的源码版本 -->
    <maven.compiler.source>21</maven.compiler.source>

    <!-- Maven 编译插件生成的目标字节码版本 -->
    <maven.compiler.target>21</maven.compiler.target>
</properties>
```

开发时需要注意，JDK 版本、Maven 编译版本和 IDE 项目 SDK 应保持一致。否则可能出现本地 IDE 能运行、命令行 Maven 打包失败，或者 Maven 能打包、IDE 编译报错的问题。

### Spring Boot 3 项目结构

本文档后续示例默认基于标准 Spring Boot 3 单体后端项目结构展开。实际业务项目可以是单模块项目，也可以是多模块项目；但 RabbitMQ 的核心配置、生产者、消费者、消息实体和常量类建议保持清晰分层。

推荐基础目录结构如下。

```text
rabbitmq-demo
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── io
│   │   │       └── github
│   │   │           └── atengk
│   │   │               ├── RabbitMqDemoApplication.java
│   │   │               ├── config
│   │   │               │   └── RabbitMqConfig.java
│   │   │               ├── constant
│   │   │               │   └── RabbitMqConstant.java
│   │   │               ├── message
│   │   │               │   └── OrderMessage.java
│   │   │               ├── producer
│   │   │               │   └── OrderMessageProducer.java
│   │   │               └── consumer
│   │   │                   └── OrderMessageConsumer.java
│   │   └── resources
│   │       ├── application.yml
│   │       └── application-dev.yml
│   └── test
│       └── java
│           └── io
│               └── github
│                   └── atengk
│                       └── RabbitMqDemoApplicationTests.java
```

各目录职责建议如下。

| 目录或文件            | 说明                                            |
| --------------------- | ----------------------------------------------- |
| `config`              | RabbitMQ 交换机、队列、绑定、消息转换器等配置   |
| `constant`            | 交换机名称、队列名称、Routing Key 等常量        |
| `message`             | MQ 消息实体类，例如订单消息、通知消息、日志消息 |
| `producer`            | 消息生产者组件，封装消息发送逻辑                |
| `consumer`            | 消息消费者组件，封装消息监听和业务处理逻辑      |
| `application.yml`     | 通用配置                                        |
| `application-dev.yml` | 开发环境配置                                    |
| `test`                | 单元测试和本地集成测试                          |

项目启动类建议放在基础包根路径下，确保能够扫描到 `config`、`producer`、`consumer` 等子包。

```java
package io.github.atengk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RabbitMQ 示例项目启动类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootApplication
public class RabbitMqDemoApplication {

    /**
     * 项目启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(RabbitMqDemoApplication.class, args);
    }

}
```

在实际项目中，不建议将交换机名称、队列名称和 Routing Key 直接散落在业务代码中。推荐统一放到常量类中，避免后续修改困难，也避免生产者和消费者配置不一致。

### RabbitMQ 服务安装方式

开发环境中推荐使用 Docker 启动 RabbitMQ，因为 Docker 方式安装简单、隔离性好、便于清理和重建环境。对于本地开发，优先选择带管理控制台插件的镜像。

使用 Docker 快速启动 RabbitMQ。

```bash
# 拉取带管理控制台的 RabbitMQ 镜像
docker pull rabbitmq:3-management

# 启动 RabbitMQ 容器
docker run -d \
  --name rabbitmq \
  --hostname rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=admin \
  -e RABBITMQ_DEFAULT_PASS=admin123 \
  rabbitmq:3-management
```

上述命令说明如下。

| 参数                    | 说明                                               |
| ----------------------- | -------------------------------------------------- |
| `--name rabbitmq`       | 指定容器名称                                       |
| `--hostname rabbitmq`   | 指定 RabbitMQ 节点主机名                           |
| `-p 5672:5672`          | 暴露 AMQP 连接端口，Spring Boot 项目通过该端口连接 |
| `-p 15672:15672`        | 暴露管理控制台端口                                 |
| `RABBITMQ_DEFAULT_USER` | 初始化 RabbitMQ 登录用户名                         |
| `RABBITMQ_DEFAULT_PASS` | 初始化 RabbitMQ 登录密码                           |
| `rabbitmq:3-management` | 带 Management 插件的 RabbitMQ 镜像                 |

如果希望使用 Docker Compose 管理 RabbitMQ，可以在项目根目录创建 `docker-compose.yml`。

```yaml
services:
  rabbitmq:
    # 使用带管理控制台的 RabbitMQ 镜像
    image: rabbitmq:3-management
    container_name: rabbitmq
    hostname: rabbitmq
    restart: unless-stopped
    ports:
      # AMQP 协议端口，供 Spring Boot 应用连接
      - "5672:5672"
      # 管理控制台端口，供浏览器访问
      - "15672:15672"
    environment:
      # 默认登录用户名
      RABBITMQ_DEFAULT_USER: admin
      # 默认登录密码，仅用于本地开发环境
      RABBITMQ_DEFAULT_PASS: admin123
      # 默认虚拟主机
      RABBITMQ_DEFAULT_VHOST: /dev
    volumes:
      # 持久化 RabbitMQ 数据，避免容器删除后数据丢失
      - rabbitmq_data:/var/lib/rabbitmq

volumes:
  rabbitmq_data:
    # 使用 Docker 命名卷保存 RabbitMQ 数据
    driver: local
```

在 `docker-compose.yml` 所在目录执行以下命令。

```bash
# 启动 RabbitMQ 服务
docker compose up -d

# 查看容器运行状态
docker compose ps

# 查看 RabbitMQ 容器日志
docker logs -f rabbitmq

# 停止 RabbitMQ 服务
docker compose down
```

如果需要彻底清理本地 RabbitMQ 数据，可以删除命名卷。

```bash
# 停止并删除容器，同时删除挂载的数据卷
docker compose down -v
```

注意：`docker compose down -v` 会删除 RabbitMQ 的持久化数据，包括队列、交换机、绑定关系和消息数据。该命令只建议在本地开发环境使用，不要在生产环境执行。

如果不使用 Docker，也可以采用以下方式安装：

| 安装方式       | 适用场景                                  |
| -------------- | ----------------------------------------- |
| Docker         | 推荐用于本地开发和快速验证                |
| Docker Compose | 推荐用于项目级本地环境编排                |
| Linux 软件包   | 适用于固定服务器部署                      |
| Kubernetes     | 适用于云原生环境部署                      |
| Windows 安装包 | 适用于 Windows 本地调试，但不推荐生产使用 |

本文档后续默认使用 Docker 或 Docker Compose 方式启动 RabbitMQ。

### RabbitMQ 管理控制台

RabbitMQ Management 控制台用于查看和管理 RabbitMQ 中的连接、通道、交换机、队列、绑定关系、用户、权限和消息状态。开发过程中，管理控制台是排查消息是否发送成功、队列是否有堆积、消费者是否连接正常的重要工具。

如果使用前面 Docker 命令启动 RabbitMQ，可以通过浏览器访问：

```text
http://localhost:15672
```

默认登录信息如下。

| 项目       | 值                       |
| ---------- | ------------------------ |
| 地址       | `http://localhost:15672` |
| 用户名     | `admin`                  |
| 密码       | `admin123`               |
| AMQP 端口  | `5672`                   |
| 控制台端口 | `15672`                  |
| 默认 vhost | `/dev`                   |

登录后常用页面说明如下。

| 页面               | 说明                                                         |
| ------------------ | ------------------------------------------------------------ |
| Overview           | 查看 RabbitMQ 节点状态、消息速率、连接数量、队列数量等总体信息 |
| Connections        | 查看客户端连接，例如 Spring Boot 应用连接                    |
| Channels           | 查看连接下的通道信息                                         |
| Exchanges          | 查看交换机列表、类型、属性和绑定关系                         |
| Queues and Streams | 查看队列、消息堆积、消费者数量和消费速率                     |
| Admin              | 管理用户、权限、vhost 和策略                                 |

本地开发时，可以在控制台中手动验证以下内容：

| 验证项         | 说明                                                 |
| -------------- | ---------------------------------------------------- |
| 连接是否正常   | 查看 `Connections` 页面是否存在 Spring Boot 应用连接 |
| 交换机是否创建 | 查看 `Exchanges` 页面是否存在项目配置的交换机        |
| 队列是否创建   | 查看 `Queues and Streams` 页面是否存在项目配置的队列 |
| 绑定是否正确   | 查看队列详情或交换机详情中的 Bindings                |
| 消息是否堆积   | 查看队列 Ready、Unacked、Total 数量                  |
| 消费者是否在线 | 查看队列 Consumers 数量                              |

队列中几个关键指标需要重点关注：

| 指标          | 说明                                   |
| ------------- | -------------------------------------- |
| Ready         | 已经进入队列，等待消费者处理的消息数量 |
| Unacked       | 已经投递给消费者，但尚未确认的消息数量 |
| Total         | 队列中的消息总量                       |
| Consumers     | 当前监听该队列的消费者数量             |
| Publish       | 消息发布速率                           |
| Deliver / Get | 消息投递或拉取速率                     |
| Ack           | 消息确认速率                           |

如果 `Ready` 持续增长，通常说明消费者处理能力不足、消费者未启动、消费者异常退出或者绑定关系错误。如果 `Unacked` 持续较高，通常说明消费者已经收到消息，但业务处理较慢、手动确认未执行或者消费线程阻塞。

### Maven 依赖配置

Spring Boot 3 项目集成 RabbitMQ，核心依赖是 `spring-boot-starter-amqp`。该依赖会引入 Spring AMQP 和 RabbitMQ Java Client，并配合 Spring Boot 自动配置生成常用的消息组件。

推荐在 `pom.xml` 中添加以下依赖。

```xml
<dependencies>
    <!-- Spring Web：用于提供接口测试入口，例如通过 HTTP 触发消息发送 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring AMQP：集成 RabbitMQ 的核心依赖，包含 RabbitTemplate、监听容器等组件 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>

    <!-- Spring Validation：用于接口参数校验和消息对象校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Spring Boot Actuator：用于暴露健康检查和运行状态，便于部署后观测服务状态 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Hutool：常用 Java 工具包，便于处理 JSON、日期、字符串、集合等常见逻辑 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.35</version>
    </dependency>

    <!-- Lombok：简化实体类、日志对象和构造方法代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot Test：单元测试和集成测试基础依赖 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Spring Rabbit Test：用于 RabbitMQ 相关测试场景 -->
    <dependency>
        <groupId>org.springframework.amqp</groupId>
        <artifactId>spring-rabbit-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目使用 Spring Boot 父工程管理依赖版本，通常不需要手动指定 `spring-boot-starter-amqp`、`spring-boot-starter-web` 等 Spring Boot 相关依赖的版本。

推荐的 `pom.xml` 基础结构如下。

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="
             http://maven.apache.org/POM/4.0.0
             https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot 父工程：统一管理 Spring Boot 相关依赖版本和插件版本 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.0</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>rabbitmq-demo</artifactId>
    <version>1.0.0</version>
    <name>rabbitmq-demo</name>
    <description>RabbitMQ 开发使用示例项目</description>

    <properties>
        <!-- 项目源码编码 -->
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <!-- Java 编译版本 -->
        <java.version>21</java.version>

        <!-- Hutool 工具包版本 -->
        <hutool.version>5.8.35</hutool.version>
    </properties>

    <dependencies>
        <!-- Spring Web：提供接口测试入口 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring AMQP：RabbitMQ 集成核心依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>

        <!-- Spring Validation：参数校验和消息体校验 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Spring Boot Actuator：健康检查和运行状态观测 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Hutool：常用工具类封装 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok：减少样板代码，编译期生效 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot Test：项目测试基础依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Spring Rabbit Test：RabbitMQ 测试支持 -->
        <dependency>
            <groupId>org.springframework.amqp</groupId>
            <artifactId>spring-rabbit-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot Maven 插件：用于项目打包和运行 -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>

            <!-- Maven Compiler 插件：指定 Java 编译版本 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <encoding>${project.build.sourceEncoding}</encoding>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
```

添加依赖后，可以执行以下命令检查项目是否能够正常编译。

```bash
# 清理并编译项目
mvn clean compile

# 执行测试
mvn test

# 打包项目
mvn clean package -DskipTests
```

如果 Maven 能够正常下载依赖并完成编译，说明项目基础依赖配置没有明显问题。后续只需要在 `application.yml` 中补充 RabbitMQ 连接配置，即可开始进行消息发送和消费开发。

## 集成 RabbitMQ

本章节用于说明 Spring Boot 3 项目如何连接 RabbitMQ，并完成基础消息发送、消息转换和 RabbitMQ 管理组件使用。后续消息发送、消息消费、可靠性确认、死信队列和延迟队列章节，都会基于本章节的配置继续展开。该章节对应上传大纲中的“集成 RabbitMQ”部分。

Spring Boot 对 RabbitMQ 提供了自动配置能力，常用连接配置可以通过 `spring.rabbitmq.*` 完成；项目引入 `spring-boot-starter-amqp` 后，可以直接注入 `RabbitTemplate`、`AmqpAdmin` 等组件使用。([Home](https://docs.spring.io/spring-boot/reference/messaging/amqp.html?utm_source=chatgpt.com))

### RabbitMQ 连接配置

RabbitMQ 连接配置用于声明 Spring Boot 应用连接哪个 RabbitMQ Broker，包括服务地址、端口、用户名、密码、Virtual Host、连接超时时间、监听容器参数等。开发环境建议将连接配置放到 `application-dev.yml` 中，生产环境则建议通过环境变量、配置中心或 Kubernetes Secret 管理账号密码。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  application:
    # 当前应用名称，建议和 RabbitMQ 连接名称、日志追踪保持一致
    name: rabbitmq-demo

  profiles:
    # 默认启用开发环境配置
    active: dev
```

文件位置：`src/main/resources/application-dev.yml`

```yaml
spring:
  rabbitmq:
    # RabbitMQ 服务地址
    host: localhost

    # RabbitMQ AMQP 协议端口，默认 5672
    port: 5672

    # RabbitMQ 登录用户名
    username: admin

    # RabbitMQ 登录密码，仅用于本地开发环境
    password: admin123

    # RabbitMQ 虚拟主机，用于隔离不同环境或不同业务系统
    virtual-host: /dev

    # 连接超时时间
    connection-timeout: 10s

    # 心跳检测时间，用于检测连接是否存活
    requested-heartbeat: 30s

    template:
      # 发送重试配置，用于处理短暂网络抖动或 Broker 临时不可用
      retry:
        enabled: true
        initial-interval: 1s
        max-attempts: 3
        multiplier: 2
        max-interval: 5s

      # mandatory 为 true 时，消息无法路由到队列会触发 returns 回调
      mandatory: true

    listener:
      simple:
        # 消费者并发数，开发环境保持较小即可
        concurrency: 1

        # 最大消费者并发数
        max-concurrency: 3

        # 每个消费者一次最多拉取的未确认消息数
        prefetch: 10

        # 开发初期可以使用 auto，后续可靠性章节会切换为 manual
        acknowledge-mode: auto

management:
  endpoints:
    web:
      exposure:
        # 暴露健康检查和基础应用信息，便于本地验证服务状态
        include: health,info
```

如果 RabbitMQ 是单节点，本地开发通常使用 `host` 和 `port` 即可。如果是多地址连接，也可以使用 `addresses` 配置。需要注意，使用 `spring.rabbitmq.addresses` 后，`host` 和 `port` 会被忽略。([Home](https://docs.spring.io/spring-boot/reference/messaging/amqp.html?utm_source=chatgpt.com))

```yaml
spring:
  rabbitmq:
    # 使用 addresses 时，host 和 port 配置会被忽略
    addresses: amqp://admin:admin123@localhost:5672
    virtual-host: /dev
```

连接配置中几个关键参数需要重点理解。

| 配置项                             | 说明                               |
| ---------------------------------- | ---------------------------------- |
| `host`                             | RabbitMQ Broker 地址               |
| `port`                             | AMQP 协议端口，默认 `5672`         |
| `username`                         | RabbitMQ 用户名                    |
| `password`                         | RabbitMQ 密码                      |
| `virtual-host`                     | 虚拟主机，用于隔离资源             |
| `connection-timeout`               | 建立连接的超时时间                 |
| `requested-heartbeat`              | 客户端和 Broker 之间的心跳检测时间 |
| `template.retry.enabled`           | 是否开启 `RabbitTemplate` 发送重试 |
| `template.mandatory`               | 消息无法路由时是否触发返回机制     |
| `listener.simple.prefetch`         | 每个消费者预取消息数量             |
| `listener.simple.acknowledge-mode` | 消费者确认模式                     |

开发环境中，如果服务启动时报连接失败，优先检查以下内容：

| 检查项                | 说明                                                    |
| --------------------- | ------------------------------------------------------- |
| RabbitMQ 容器是否启动 | 执行 `docker ps` 查看容器状态                           |
| 端口是否正确          | AMQP 端口是 `5672`，管理控制台端口是 `15672`            |
| 账号密码是否正确      | 确认和 Docker 启动参数一致                              |
| vhost 是否存在        | 确认 `/dev` 已创建或通过环境变量初始化                  |
| 用户是否有权限        | 确认用户对 vhost 具备 `configure`、`write`、`read` 权限 |

### RabbitTemplate 基础使用

`RabbitTemplate` 是 Spring AMQP 中最常用的消息发送组件，主要用于向交换机发送消息、指定 Routing Key、设置消息属性以及执行同步接收等操作。Spring Boot 引入 RabbitMQ 相关依赖后，会自动配置 `RabbitTemplate`，业务代码中可以直接注入使用。Spring Boot 官方文档也说明，`AmqpTemplate`、`AmqpAdmin` 可以直接注入到业务 Bean 中使用。([Home](https://docs.spring.io/spring-boot/4.1-SNAPSHOT/reference/messaging/amqp.html?utm_source=chatgpt.com))

在使用 `RabbitTemplate` 前，建议先统一定义交换机、队列和 Routing Key 常量，避免字符串散落在业务代码中。

文件位置：`src/main/java/io/github/atengk/constant/RabbitMqConstant.java`

```java
package io.github.atengk.constant;

/**
 * RabbitMQ 常量配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class RabbitMqConstant {

    /**
     * 订单业务交换机
     */
    public static final String ORDER_EXCHANGE = "order.direct.exchange";

    /**
     * 订单创建队列
     */
    public static final String ORDER_CREATE_QUEUE = "order.create.queue";

    /**
     * 订单创建 Routing Key
     */
    public static final String ORDER_CREATE_ROUTING_KEY = "order.create";

    private RabbitMqConstant() {
    }

}
```

下面定义一个基础订单消息实体，用于演示对象消息发送。

文件位置：`src/main/java/io/github/atengk/message/OrderMessage.java`

```java
package io.github.atengk.message;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单消息实体
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessage {

    /**
     * 订单号
     */
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订单金额
     */
    private BigDecimal amount;

    /**
     * 订单状态
     */
    private String status;

    /**
     * 消息创建时间
     */
    private LocalDateTime createTime;

}
```

下面定义 RabbitMQ 的交换机、队列和绑定关系。项目启动后，Spring 会通过 RabbitMQ 管理组件将这些 Bean 声明到 RabbitMQ Broker 中。

文件位置：`src/main/java/io/github/atengk/config/RabbitMqConfig.java`

```java
package io.github.atengk.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atengk.constant.RabbitMqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 基础配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
public class RabbitMqConfig {

    @Bean
    public DirectExchange orderExchange() {
        return ExchangeBuilder
                .directExchange(RabbitMqConstant.ORDER_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue orderCreateQueue() {
        return QueueBuilder
                .durable(RabbitMqConstant.ORDER_CREATE_QUEUE)
                .build();
    }

    @Bean
    public Binding orderCreateBinding(Queue orderCreateQueue, DirectExchange orderExchange) {
        return BindingBuilder
                .bind(orderCreateQueue)
                .to(orderExchange)
                .with(RabbitMqConstant.ORDER_CREATE_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

}
```

`RabbitTemplate` 发送消息时，常用方法是 `convertAndSend(exchange, routingKey, message)`。该方法会将 Java 对象交给 `MessageConverter` 转换为 RabbitMQ 消息体，再发送到指定交换机。

文件位置：`src/main/java/io/github/atengk/producer/OrderMessageProducer.java`

```java
package io.github.atengk.producer;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.constant.RabbitMqConstant;
import io.github.atengk.message.OrderMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

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

    private final RabbitTemplate rabbitTemplate;

    public void sendOrderMessage(OrderMessage orderMessage) {
        String orderNo = StrUtil.blankToDefault(orderMessage.getOrderNo(), IdUtil.fastSimpleUUID());
        orderMessage.setOrderNo(orderNo);

        if (orderMessage.getCreateTime() == null) {
            orderMessage.setCreateTime(LocalDateTime.now());
        }

        rabbitTemplate.convertAndSend(
                RabbitMqConstant.ORDER_EXCHANGE,
                RabbitMqConstant.ORDER_CREATE_ROUTING_KEY,
                orderMessage
        );

        log.info("订单消息发送完成，exchange={}，routingKey={}，orderNo={}",
                RabbitMqConstant.ORDER_EXCHANGE,
                RabbitMqConstant.ORDER_CREATE_ROUTING_KEY,
                orderMessage.getOrderNo());
    }

    public void sendOrderCloseMessage(String orderNo) {
        OrderMessage orderMessage = OrderMessage.builder()
                .orderNo(orderNo)
                .status("WAIT_CLOSE")
                .createTime(LocalDateTime.now())
                .build();

        this.sendOrderMessage(orderMessage);
    }

}
```

为了方便本地验证，可以提供一个测试接口，通过 HTTP 请求触发消息发送。

文件位置：`src/main/java/io/github/atengk/controller/OrderMessageController.java`

```java
package io.github.atengk.controller;

import io.github.atengk.message.OrderMessage;
import io.github.atengk.producer.OrderMessageProducer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 订单消息测试接口
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequestMapping("/api/mq/order")
@RequiredArgsConstructor
public class OrderMessageController {

    private final OrderMessageProducer orderMessageProducer;

    @PostMapping("/send")
    public ResponseEntity<String> sendOrderMessage(@Valid @RequestBody OrderMessage orderMessage) {
        orderMessageProducer.sendOrderMessage(orderMessage);
        return ResponseEntity.ok("订单消息发送成功");
    }

    @GetMapping("/close/{orderNo}")
    public ResponseEntity<String> sendOrderCloseMessage(@PathVariable String orderNo) {
        orderMessageProducer.sendOrderCloseMessage(orderNo);
        return ResponseEntity.ok("订单关闭消息发送成功");
    }

}
```

启动项目后，可以使用以下命令发送测试消息。

```bash
curl -X POST "http://localhost:8080/api/mq/order/send" \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "ORDER_202604300001",
    "userId": 10001,
    "amount": 99.90,
    "status": "CREATED"
  }'
```

也可以发送订单关闭测试消息。

```bash
curl -X GET "http://localhost:8080/api/mq/order/close/ORDER_202604300001"
```

发送成功后，可以进入 RabbitMQ 管理控制台，查看 `order.create.queue` 队列是否出现消息。如果暂时没有消费者监听该队列，消息会处于 `Ready` 状态；如果后续消费者正常监听并处理，消息会被消费掉。

### MessageConverter 配置

`MessageConverter` 用于处理 Java 对象和 RabbitMQ 消息体之间的转换。`RabbitTemplate` 的发送和接收方法会委托 `MessageConverter` 完成对象转换；如果没有显式配置，默认使用 `SimpleMessageConverter`。Spring AMQP 文档说明，`SimpleMessageConverter` 可以处理字符串、字节数组和 Java 序列化对象，但实际业务系统中更推荐使用 JSON 作为跨语言、可读性更好的消息格式。([Home](https://docs.spring.io/spring-amqp/reference/amqp/message-converters.html?utm_source=chatgpt.com))

在 Spring Boot 3 项目中，推荐配置 JSON 消息转换器。对于常见 Spring Boot 3.x 依赖组合，可以使用 `Jackson2JsonMessageConverter`；该类是 Spring AMQP 3.x 中基于 Jackson 2 的 JSON 消息转换器。([Home](https://docs.spring.io/spring-amqp/docs/3.0.12/api/org/springframework/amqp/support/converter/Jackson2JsonMessageConverter.html?utm_source=chatgpt.com))

前面 `RabbitMqConfig` 中已经定义了如下配置：

```java
@Bean
public MessageConverter messageConverter(ObjectMapper objectMapper) {
    return new Jackson2JsonMessageConverter(objectMapper);
}
```

Spring Boot 检测到 `MessageConverter` Bean 后，会将其关联到自动配置的 `AmqpTemplate`，也会关联到默认的监听容器工厂中。([Home](https://docs.spring.io/spring-boot/reference/messaging/amqp.html?utm_source=chatgpt.com))

使用 JSON 消息转换器后，生产者可以直接发送 Java 对象。

```java
rabbitTemplate.convertAndSend(
        RabbitMqConstant.ORDER_EXCHANGE,
        RabbitMqConstant.ORDER_CREATE_ROUTING_KEY,
        orderMessage
);
```

发送到 RabbitMQ 后，消息体会被转换为 JSON 格式，便于控制台查看和跨语言系统消费。示例消息内容如下。

```json
{
  "orderNo": "ORDER_202604300001",
  "userId": 10001,
  "amount": 99.90,
  "status": "CREATED",
  "createTime": "2026-04-30T10:30:00"
}
```

如果项目中已经自定义了全局 `ObjectMapper`，例如统一配置 Java 8 时间类型、空值处理、枚举序列化等，`Jackson2JsonMessageConverter` 可以直接复用 Spring 容器中的 `ObjectMapper`，保证 HTTP 接口 JSON 和 MQ 消息 JSON 的序列化风格尽量一致。

下面是一个更完整的 `ObjectMapper` 配置示例，用于统一日期时间格式和空值处理。

文件位置：`src/main/java/io/github/atengk/config/JacksonConfig.java`

```java
package io.github.atengk.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson JSON 配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.findAndRegisterModules();

        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return objectMapper;
    }

}
```

配置 `MessageConverter` 时需要注意以下事项：

| 注意事项                   | 说明                                                 |
| -------------------------- | ---------------------------------------------------- |
| 不建议使用 Java 原生序列化 | Java 序列化可读性差，跨语言能力弱，也存在安全风险    |
| 推荐使用 JSON              | JSON 可读性强，便于排查问题，也便于其他语言消费      |
| 生产者和消费者模型要一致   | 发送对象字段和消费对象字段应保持兼容                 |
| 时间字段要统一格式         | 建议统一使用 `LocalDateTime` 并配置 Jackson 时间模块 |
| 字段变更要兼容             | 消息实体新增字段时，消费者应能兼容旧消息             |
| 不要发送过大的对象         | MQ 消息应保存必要业务数据，不建议塞入大文本或大文件  |

实际项目中，消息体应尽量保持轻量。推荐消息中只包含业务处理需要的关键字段，例如订单号、用户 ID、业务状态、操作时间等。对于大对象，可以只发送业务主键，由消费者根据主键查询数据库获取完整数据。

### RabbitAdmin 管理组件

`RabbitAdmin` 是 Spring AMQP 提供的 RabbitMQ 管理组件，用于声明交换机、队列、绑定关系，以及查询队列属性等。Spring Boot 通常会自动配置 `AmqpAdmin`，业务代码中可以直接注入使用。官方文档也说明，`AmqpAdmin` 和 `AmqpTemplate` 可以被自动配置并注入到应用 Bean 中。([Home](https://docs.spring.io/spring-boot/4.1-SNAPSHOT/reference/messaging/amqp.html?utm_source=chatgpt.com))

在开发中，RabbitMQ 资源管理有两种常见方式。

| 方式          | 说明                                                         | 推荐程度     |
| ------------- | ------------------------------------------------------------ | ------------ |
| Bean 声明方式 | 通过 `@Bean` 声明 Exchange、Queue、Binding，项目启动时自动声明 | 推荐         |
| 编程声明方式  | 通过 `AmqpAdmin` 或 `RabbitAdmin` 在代码中动态声明资源       | 特定场景使用 |

前面 `RabbitMqConfig` 中的 `DirectExchange`、`Queue`、`Binding` Bean 就属于推荐的 Bean 声明方式。Spring Boot 文档说明，如果存在 `Queue` Bean，会自动在 RabbitMQ 实例中声明对应队列。([Home](https://docs.spring.io/spring-boot/reference/messaging/amqp.html?utm_source=chatgpt.com))

如果确实需要在运行时动态声明交换机、队列和绑定关系，可以封装一个管理服务。

文件位置：`src/main/java/io/github/atengk/service/RabbitMqAdminService.java`

```java
package io.github.atengk.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * RabbitMQ 管理服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMqAdminService {

    private final AmqpAdmin amqpAdmin;

    public void declareDirectQueue(String exchangeName, String queueName, String routingKey) {
        if (StrUtil.hasBlank(exchangeName, queueName, routingKey)) {
            throw new IllegalArgumentException("交换机名称、队列名称和 Routing Key 不能为空");
        }

        DirectExchange exchange = ExchangeBuilder
                .directExchange(exchangeName)
                .durable(true)
                .build();

        Queue queue = QueueBuilder
                .durable(queueName)
                .build();

        Binding binding = BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(routingKey);

        amqpAdmin.declareExchange(exchange);
        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareBinding(binding);

        log.info("RabbitMQ资源声明完成，exchange={}，queue={}，routingKey={}",
                exchangeName, queueName, routingKey);
    }

    public Properties getQueueProperties(String queueName) {
        if (StrUtil.isBlank(queueName)) {
            throw new IllegalArgumentException("队列名称不能为空");
        }

        Properties properties = amqpAdmin.getQueueProperties(queueName);
        if (properties == null) {
            log.warn("RabbitMQ队列不存在，queueName={}", queueName);
            return new Properties();
        }

        log.info("RabbitMQ队列属性查询完成，queueName={}，properties={}", queueName, properties);
        return properties;
    }

}
```

如果需要通过接口触发管理操作，可以增加一个仅用于开发环境的测试接口。生产环境不建议暴露动态创建队列和交换机的接口，避免误操作造成资源膨胀。

文件位置：`src/main/java/io/github/atengk/controller/RabbitMqAdminController.java`

```java
package io.github.atengk.controller;

import io.github.atengk.service.RabbitMqAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Properties;

/**
 * RabbitMQ 管理测试接口
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Profile("dev")
@RestController
@RequestMapping("/api/mq/admin")
@RequiredArgsConstructor
public class RabbitMqAdminController {

    private final RabbitMqAdminService rabbitMqAdminService;

    @PostMapping("/direct")
    public ResponseEntity<String> declareDirectQueue(@RequestParam String exchangeName,
                                                     @RequestParam String queueName,
                                                     @RequestParam String routingKey) {
        rabbitMqAdminService.declareDirectQueue(exchangeName, queueName, routingKey);
        return ResponseEntity.ok("RabbitMQ资源声明成功");
    }

    @GetMapping("/queue/{queueName}")
    public ResponseEntity<Properties> getQueueProperties(@PathVariable String queueName) {
        return ResponseEntity.ok(rabbitMqAdminService.getQueueProperties(queueName));
    }

}
```

本地可以通过以下命令动态声明一个 Direct Exchange、Queue 和 Binding。

```bash
curl -X POST "http://localhost:8080/api/mq/admin/direct?exchangeName=test.direct.exchange&queueName=test.direct.queue&routingKey=test.direct"
```

查询队列属性。

```bash
curl -X GET "http://localhost:8080/api/mq/admin/queue/test.direct.queue"
```

返回内容中通常会包含队列消息数量、消费者数量等信息。`RabbitAdmin` 的 API 中也提供了 `getQueueProperties(String)` 等方法用于获取队列属性。([Home](https://docs.spring.io/spring-amqp/docs/current/api/org/springframework/amqp/rabbit/core/RabbitAdmin.html?utm_source=chatgpt.com))

在实际开发中，推荐优先使用配置类统一声明固定的交换机、队列和绑定关系；动态声明只适合租户隔离、临时队列、插件化业务或需要运行时扩展消息拓扑的场景。

RabbitAdmin 使用时需要注意以下事项：

| 注意事项                   | 说明                                                 |
| -------------------------- | ---------------------------------------------------- |
| 固定资源优先使用 Bean 声明 | 交换机、队列、绑定关系更清晰，便于代码审查           |
| 动态声明要限制入口         | 不建议生产环境开放任意创建 MQ 资源的接口             |
| 队列参数要保持一致         | 已存在队列再次声明时，参数不一致会导致启动或声明失败 |
| 命名要规范                 | 建议统一使用业务前缀，例如 `order.`、`pay.`、`user.` |
| 权限要匹配                 | 当前用户必须具备对应 vhost 下的 `configure` 权限     |
| 管理操作要有日志           | 动态声明资源必须记录关键参数，便于问题追踪           |

完成本章节后，项目已经具备 RabbitMQ 的基础连接能力、消息发送能力、JSON 消息转换能力和资源声明能力。后续即可继续展开常用交换机模型、消息发送开发和消息消费开发。

## 常用交换机模型

本章节用于说明 RabbitMQ 中常用的四种交换机模型：Direct、Fanout、Topic 和 Headers。交换机负责接收生产者发送的消息，并根据自身类型、Routing Key、Binding Key 或 Header 参数，将消息路由到一个或多个队列。该章节对应上传大纲中的“常用交换机模型”部分。

在业务开发中，交换机模型选择会直接影响消息的投递方式。一般来说，精确业务事件使用 Direct Exchange，广播通知使用 Fanout Exchange，分类订阅使用 Topic Exchange，基于消息头复杂匹配的场景才考虑 Headers Exchange。

| 交换机类型       | 路由依据                               | 典型场景                         | 使用频率 |
| ---------------- | -------------------------------------- | -------------------------------- | -------- |
| Direct Exchange  | Routing Key 精确匹配 Binding Key       | 订单创建、支付成功、库存扣减     | 高       |
| Fanout Exchange  | 不判断 Routing Key，广播到所有绑定队列 | 广播通知、缓存刷新、多系统同步   | 中       |
| Topic Exchange   | Routing Key 与 Binding Key 通配符匹配  | 业务事件分类、日志分级、模块订阅 | 高       |
| Headers Exchange | 消息 Header 参数匹配                   | 多条件路由、非字符串路由场景     | 低       |

### Direct Exchange

Direct Exchange 是最常用、最直观的交换机类型。它根据消息发送时携带的 Routing Key 与队列绑定时设置的 Binding Key 进行精确匹配。只有 Routing Key 和 Binding Key 完全一致时，消息才会被路由到对应队列。

Direct Exchange 适合用于明确的一对一或一对多业务事件投递。例如订单创建消息只进入订单创建队列，支付成功消息只进入支付成功队列，库存扣减消息只进入库存扣减队列。

Direct Exchange 的路由关系如下。

```text
Producer
   |
   | routingKey = order.create
   v
Direct Exchange
   |
   | bindingKey = order.create
   v
order.create.queue
```

如果同一个 Direct Exchange 下有多个队列使用相同 Binding Key 绑定，那么一条消息会被同时路由到多个队列。这种方式可以实现“精确事件的一对多订阅”。

```text
Direct Exchange: order.direct.exchange

routingKey = order.create

绑定关系：
order.create.queue       bindingKey = order.create
order.audit.queue        bindingKey = order.create
order.statistics.queue   bindingKey = order.create

路由结果：
三个队列都会收到消息
```

Direct Exchange 适合以下场景。

| 场景     | 说明                                |
| -------- | ----------------------------------- |
| 订单创建 | `order.create` 路由到订单创建队列   |
| 订单取消 | `order.cancel` 路由到订单取消队列   |
| 支付成功 | `pay.success` 路由到支付成功队列    |
| 库存扣减 | `stock.deduct` 路由到库存扣减队列   |
| 审核完成 | `audit.complete` 路由到审核完成队列 |

Direct Exchange 常量定义建议如下。

文件位置：`src/main/java/io/github/atengk/constant/RabbitMqDirectConstant.java`

```java
package io.github.atengk.constant;

/**
 * Direct Exchange 常量
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class RabbitMqDirectConstant {

    /**
     * 订单 Direct 交换机
     */
    public static final String ORDER_DIRECT_EXCHANGE = "order.direct.exchange";

    /**
     * 订单创建队列
     */
    public static final String ORDER_CREATE_QUEUE = "order.create.queue";

    /**
     * 订单取消队列
     */
    public static final String ORDER_CANCEL_QUEUE = "order.cancel.queue";

    /**
     * 订单创建 Routing Key
     */
    public static final String ORDER_CREATE_ROUTING_KEY = "order.create";

    /**
     * 订单取消 Routing Key
     */
    public static final String ORDER_CANCEL_ROUTING_KEY = "order.cancel";

    private RabbitMqDirectConstant() {
    }

}
```

下面配置 Direct Exchange、Queue 和 Binding 关系。

文件位置：`src/main/java/io/github/atengk/config/RabbitMqDirectConfig.java`

```java
package io.github.atengk.config;

import io.github.atengk.constant.RabbitMqDirectConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Direct Exchange 配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
public class RabbitMqDirectConfig {

    @Bean
    public DirectExchange orderDirectExchange() {
        return ExchangeBuilder
                .directExchange(RabbitMqDirectConstant.ORDER_DIRECT_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue orderCreateQueue() {
        return QueueBuilder
                .durable(RabbitMqDirectConstant.ORDER_CREATE_QUEUE)
                .build();
    }

    @Bean
    public Queue orderCancelQueue() {
        return QueueBuilder
                .durable(RabbitMqDirectConstant.ORDER_CANCEL_QUEUE)
                .build();
    }

    @Bean
    public Binding orderCreateBinding(DirectExchange orderDirectExchange, Queue orderCreateQueue) {
        return BindingBuilder
                .bind(orderCreateQueue)
                .to(orderDirectExchange)
                .with(RabbitMqDirectConstant.ORDER_CREATE_ROUTING_KEY);
    }

    @Bean
    public Binding orderCancelBinding(DirectExchange orderDirectExchange, Queue orderCancelQueue) {
        return BindingBuilder
                .bind(orderCancelQueue)
                .to(orderDirectExchange)
                .with(RabbitMqDirectConstant.ORDER_CANCEL_ROUTING_KEY);
    }

}
```

Direct Exchange 消息发送示例。

```java
rabbitTemplate.convertAndSend(
        RabbitMqDirectConstant.ORDER_DIRECT_EXCHANGE,
        RabbitMqDirectConstant.ORDER_CREATE_ROUTING_KEY,
        orderMessage
);
```

Direct Exchange 使用时需要注意以下事项。

| 注意事项                 | 说明                                               |
| ------------------------ | -------------------------------------------------- |
| Routing Key 必须精确匹配 | `order.create` 和 `order.created` 是两个不同路由键 |
| 命名要有业务语义         | 推荐使用 `业务域.动作` 格式                        |
| 适合明确事件             | 不适合复杂模糊匹配场景                             |
| 可一对多投递             | 多个队列使用相同 Binding Key 绑定即可              |
| 推荐高频使用             | 大多数业务消息都可以优先考虑 Direct Exchange       |

### Fanout Exchange

Fanout Exchange 是广播交换机。它不会判断 Routing Key，也不会根据 Binding Key 过滤消息，只要队列绑定到该交换机，消息就会被投递到所有绑定队列。

Fanout Exchange 适合用于广播类业务场景，例如系统通知、缓存刷新、配置变更、多系统数据同步等。生产者只需要把消息发送到 Fanout Exchange，所有绑定队列都会收到同一份消息。

Fanout Exchange 的路由关系如下。

```text
Producer
   |
   | routingKey 会被忽略
   v
Fanout Exchange
   |------------------> system.notice.queue
   |------------------> cache.refresh.queue
   |------------------> data.sync.queue
```

Fanout Exchange 适合以下场景。

| 场景         | 说明                                         |
| ------------ | -------------------------------------------- |
| 系统广播通知 | 后台发布通知后，多个业务系统同时接收         |
| 缓存刷新     | 数据变更后，多个服务分别刷新本地缓存         |
| 配置变更     | 配置中心变更后，多个业务模块同时收到变更事件 |
| 多系统同步   | 主业务事件发生后，多个下游系统独立处理       |
| 日志广播     | 同一日志消息分发到存储、检索、告警等多个队列 |

Fanout Exchange 常量定义建议如下。

文件位置：`src/main/java/io/github/atengk/constant/RabbitMqFanoutConstant.java`

```java
package io.github.atengk.constant;

/**
 * Fanout Exchange 常量
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class RabbitMqFanoutConstant {

    /**
     * 系统通知 Fanout 交换机
     */
    public static final String SYSTEM_FANOUT_EXCHANGE = "system.fanout.exchange";

    /**
     * 站内信通知队列
     */
    public static final String NOTICE_SITE_QUEUE = "notice.site.queue";

    /**
     * 短信通知队列
     */
    public static final String NOTICE_SMS_QUEUE = "notice.sms.queue";

    /**
     * 邮件通知队列
     */
    public static final String NOTICE_EMAIL_QUEUE = "notice.email.queue";

    private RabbitMqFanoutConstant() {
    }

}
```

下面配置 Fanout Exchange 和多个队列绑定关系。

文件位置：`src/main/java/io/github/atengk/config/RabbitMqFanoutConfig.java`

```java
package io.github.atengk.config;

import io.github.atengk.constant.RabbitMqFanoutConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fanout Exchange 配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
public class RabbitMqFanoutConfig {

    @Bean
    public FanoutExchange systemFanoutExchange() {
        return ExchangeBuilder
                .fanoutExchange(RabbitMqFanoutConstant.SYSTEM_FANOUT_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue noticeSiteQueue() {
        return QueueBuilder
                .durable(RabbitMqFanoutConstant.NOTICE_SITE_QUEUE)
                .build();
    }

    @Bean
    public Queue noticeSmsQueue() {
        return QueueBuilder
                .durable(RabbitMqFanoutConstant.NOTICE_SMS_QUEUE)
                .build();
    }

    @Bean
    public Queue noticeEmailQueue() {
        return QueueBuilder
                .durable(RabbitMqFanoutConstant.NOTICE_EMAIL_QUEUE)
                .build();
    }

    @Bean
    public Binding noticeSiteBinding(FanoutExchange systemFanoutExchange, Queue noticeSiteQueue) {
        return BindingBuilder
                .bind(noticeSiteQueue)
                .to(systemFanoutExchange);
    }

    @Bean
    public Binding noticeSmsBinding(FanoutExchange systemFanoutExchange, Queue noticeSmsQueue) {
        return BindingBuilder
                .bind(noticeSmsQueue)
                .to(systemFanoutExchange);
    }

    @Bean
    public Binding noticeEmailBinding(FanoutExchange systemFanoutExchange, Queue noticeEmailQueue) {
        return BindingBuilder
                .bind(noticeEmailQueue)
                .to(systemFanoutExchange);
    }

}
```

Fanout Exchange 消息发送时，Routing Key 可以传空字符串，因为 Fanout Exchange 不会使用 Routing Key 做路由判断。

```java
rabbitTemplate.convertAndSend(
        RabbitMqFanoutConstant.SYSTEM_FANOUT_EXCHANGE,
        "",
        noticeMessage
);
```

Fanout Exchange 使用时需要注意以下事项。

| 注意事项                 | 说明                                            |
| ------------------------ | ----------------------------------------------- |
| Routing Key 会被忽略     | 即使传入 Routing Key，也不会影响路由结果        |
| 所有绑定队列都会收到消息 | 适合广播，不适合精确投递                        |
| 消费者互不影响           | 每个队列都有独立消息副本                        |
| 队列数量要控制           | 绑定队列过多会增加 Broker 路由和存储压力        |
| 不适合按条件过滤         | 如果需要按业务条件过滤，优先考虑 Topic Exchange |

Fanout Exchange 与多个消费者直接监听同一个队列不同。多个消费者监听同一个队列时，一条消息只会被其中一个消费者消费；而 Fanout Exchange 绑定多个队列时，每个队列都会收到一份消息副本，各自独立消费。

### Topic Exchange

Topic Exchange 是基于通配符规则进行路由的交换机。它会将 Routing Key 按照 `.` 分隔成多个单词，然后使用 Binding Key 中的通配符进行匹配。Topic Exchange 比 Direct Exchange 更灵活，适合按照业务域、事件类型、状态、模块等维度进行分类订阅。

Topic Exchange 支持两个通配符。

| 通配符 | 含义               | 示例                                                 |
| ------ | ------------------ | ---------------------------------------------------- |
| `*`    | 匹配一个单词       | `order.*.success` 可匹配 `order.pay.success`         |
| `#`    | 匹配零个或多个单词 | `order.#` 可匹配 `order.create`、`order.pay.success` |

Topic Exchange 的路由关系如下。

```text
Producer
   |
   | routingKey = order.pay.success
   v
Topic Exchange
   |
   | bindingKey = order.*.success
   v
order.success.queue

   |
   | bindingKey = order.#
   v
order.all.queue
```

Topic Exchange 适合以下场景。

| 场景           | 说明                                        |
| -------------- | ------------------------------------------- |
| 业务事件分类   | 按订单、支付、库存、用户等业务域订阅        |
| 状态事件订阅   | 只订阅成功、失败、超时等某类状态            |
| 日志分级处理   | 按 `log.info`、`log.error`、`log.warn` 分类 |
| 多模块订阅     | 不同模块只订阅自己关心的消息                |
| 扩展型消息路由 | 后续可以新增 Binding Key，而不影响原生产者  |

推荐使用具备层级语义的 Routing Key。

```text
业务域.业务动作.业务状态
```

例如：

```text
order.create.success
order.pay.success
order.close.timeout
pay.refund.failed
stock.deduct.success
user.register.success
```

Topic Exchange 常量定义建议如下。

文件位置：`src/main/java/io/github/atengk/constant/RabbitMqTopicConstant.java`

```java
package io.github.atengk.constant;

/**
 * Topic Exchange 常量
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class RabbitMqTopicConstant {

    /**
     * 业务事件 Topic 交换机
     */
    public static final String EVENT_TOPIC_EXCHANGE = "event.topic.exchange";

    /**
     * 订单全部事件队列
     */
    public static final String ORDER_ALL_EVENT_QUEUE = "order.all.event.queue";

    /**
     * 订单成功事件队列
     */
    public static final String ORDER_SUCCESS_EVENT_QUEUE = "order.success.event.queue";

    /**
     * 业务失败事件队列
     */
    public static final String BUSINESS_FAILED_EVENT_QUEUE = "business.failed.event.queue";

    /**
     * 订单全部事件 Binding Key
     */
    public static final String ORDER_ALL_BINDING_KEY = "order.#";

    /**
     * 订单成功事件 Binding Key
     */
    public static final String ORDER_SUCCESS_BINDING_KEY = "order.*.success";

    /**
     * 业务失败事件 Binding Key
     */
    public static final String BUSINESS_FAILED_BINDING_KEY = "#.failed";

    /**
     * 订单支付成功 Routing Key
     */
    public static final String ORDER_PAY_SUCCESS_ROUTING_KEY = "order.pay.success";

    /**
     * 订单关闭超时 Routing Key
     */
    public static final String ORDER_CLOSE_TIMEOUT_ROUTING_KEY = "order.close.timeout";

    private RabbitMqTopicConstant() {
    }

}
```

下面配置 Topic Exchange 和多种通配符绑定关系。

文件位置：`src/main/java/io/github/atengk/config/RabbitMqTopicConfig.java`

```java
package io.github.atengk.config;

import io.github.atengk.constant.RabbitMqTopicConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topic Exchange 配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
public class RabbitMqTopicConfig {

    @Bean
    public TopicExchange eventTopicExchange() {
        return ExchangeBuilder
                .topicExchange(RabbitMqTopicConstant.EVENT_TOPIC_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue orderAllEventQueue() {
        return QueueBuilder
                .durable(RabbitMqTopicConstant.ORDER_ALL_EVENT_QUEUE)
                .build();
    }

    @Bean
    public Queue orderSuccessEventQueue() {
        return QueueBuilder
                .durable(RabbitMqTopicConstant.ORDER_SUCCESS_EVENT_QUEUE)
                .build();
    }

    @Bean
    public Queue businessFailedEventQueue() {
        return QueueBuilder
                .durable(RabbitMqTopicConstant.BUSINESS_FAILED_EVENT_QUEUE)
                .build();
    }

    @Bean
    public Binding orderAllEventBinding(TopicExchange eventTopicExchange, Queue orderAllEventQueue) {
        return BindingBuilder
                .bind(orderAllEventQueue)
                .to(eventTopicExchange)
                .with(RabbitMqTopicConstant.ORDER_ALL_BINDING_KEY);
    }

    @Bean
    public Binding orderSuccessEventBinding(TopicExchange eventTopicExchange, Queue orderSuccessEventQueue) {
        return BindingBuilder
                .bind(orderSuccessEventQueue)
                .to(eventTopicExchange)
                .with(RabbitMqTopicConstant.ORDER_SUCCESS_BINDING_KEY);
    }

    @Bean
    public Binding businessFailedEventBinding(TopicExchange eventTopicExchange, Queue businessFailedEventQueue) {
        return BindingBuilder
                .bind(businessFailedEventQueue)
                .to(eventTopicExchange)
                .with(RabbitMqTopicConstant.BUSINESS_FAILED_BINDING_KEY);
    }

}
```

Topic Exchange 消息发送示例。

```java
rabbitTemplate.convertAndSend(
        RabbitMqTopicConstant.EVENT_TOPIC_EXCHANGE,
        RabbitMqTopicConstant.ORDER_PAY_SUCCESS_ROUTING_KEY,
        orderMessage
);
```

如果发送消息的 Routing Key 是 `order.pay.success`，则匹配结果如下。

| Binding Key       | 是否匹配 | 原因                                   |
| ----------------- | -------- | -------------------------------------- |
| `order.#`         | 是       | `#` 可匹配 `pay.success`               |
| `order.*.success` | 是       | `*` 匹配 `pay`，最后一段匹配 `success` |
| `#.failed`        | 否       | Routing Key 不是以 `failed` 结尾       |

如果发送消息的 Routing Key 是 `pay.refund.failed`，则匹配结果如下。

| Binding Key       | 是否匹配 | 原因                                               |
| ----------------- | -------- | -------------------------------------------------- |
| `order.#`         | 否       | 第一段不是 `order`                                 |
| `order.*.success` | 否       | 不符合订单成功事件格式                             |
| `#.failed`        | 是       | `#` 匹配前面的 `pay.refund`，最后一段匹配 `failed` |

Topic Exchange 使用时需要注意以下事项。

| 注意事项               | 说明                                         |
| ---------------------- | -------------------------------------------- |
| Routing Key 要分层清晰 | 推荐使用 `业务域.动作.状态`                  |
| 不要滥用 `#`           | 过多使用 `#` 会导致消息被过度投递            |
| 匹配规则要可读         | Binding Key 过于复杂会增加排查成本           |
| 适合扩展订阅           | 新增队列和绑定关系通常不影响生产者           |
| 注意重复匹配           | 同一队列不建议配置多条容易重复覆盖的绑定规则 |

Topic Exchange 是实际业务中非常常用的交换机类型。相比 Direct Exchange，它更适合业务事件模型；相比 Fanout Exchange，它又具备更强的过滤能力。

### Headers Exchange

Headers Exchange 是基于消息 Header 参数进行匹配的交换机。它不依赖 Routing Key，而是根据消息头中的键值对和绑定时设置的 Header 条件进行路由。

Headers Exchange 支持通过 `x-match` 参数控制匹配模式。

| `x-match` 值 | 说明                                 |
| ------------ | ------------------------------------ |
| `all`        | 消息 Header 必须匹配所有绑定参数     |
| `any`        | 消息 Header 匹配任意一个绑定参数即可 |

Headers Exchange 的路由关系如下。

```text
Producer
   |
   | headers: platform=ios, type=notice
   v
Headers Exchange
   |
   | x-match=all, platform=ios, type=notice
   v
ios.notice.queue
```

Headers Exchange 适合以下场景。

| 场景             | 说明                                   |
| ---------------- | -------------------------------------- |
| 多条件消息路由   | 根据平台、渠道、地区、业务类型组合判断 |
| 非字符串路由场景 | 不希望使用 Routing Key 表达路由条件    |
| 兼容特殊系统     | 某些历史系统通过 Header 携带路由信息   |
| 临时复杂过滤     | 少量多条件消息投递场景                 |

不过在大多数 Java 业务系统中，Headers Exchange 使用频率较低。因为 Topic Exchange 通常已经可以满足大部分分类路由需求，并且 Routing Key 的可读性、可观测性和排查便利性通常更好。

Headers Exchange 常量定义建议如下。

文件位置：`src/main/java/io/github/atengk/constant/RabbitMqHeadersConstant.java`

```java
package io.github.atengk.constant;

/**
 * Headers Exchange 常量
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class RabbitMqHeadersConstant {

    /**
     * 通知 Headers 交换机
     */
    public static final String NOTICE_HEADERS_EXCHANGE = "notice.headers.exchange";

    /**
     * iOS 通知队列
     */
    public static final String IOS_NOTICE_QUEUE = "notice.ios.queue";

    /**
     * Android 通知队列
     */
    public static final String ANDROID_NOTICE_QUEUE = "notice.android.queue";

    private RabbitMqHeadersConstant() {
    }

}
```

下面配置 Headers Exchange 和 Header 匹配规则。

文件位置：`src/main/java/io/github/atengk/config/RabbitMqHeadersConfig.java`

```java
package io.github.atengk.config;

import io.github.atengk.constant.RabbitMqHeadersConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Headers Exchange 配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
public class RabbitMqHeadersConfig {

    @Bean
    public HeadersExchange noticeHeadersExchange() {
        return ExchangeBuilder
                .headersExchange(RabbitMqHeadersConstant.NOTICE_HEADERS_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue iosNoticeQueue() {
        return QueueBuilder
                .durable(RabbitMqHeadersConstant.IOS_NOTICE_QUEUE)
                .build();
    }

    @Bean
    public Queue androidNoticeQueue() {
        return QueueBuilder
                .durable(RabbitMqHeadersConstant.ANDROID_NOTICE_QUEUE)
                .build();
    }

    @Bean
    public Binding iosNoticeBinding(HeadersExchange noticeHeadersExchange, Queue iosNoticeQueue) {
        return BindingBuilder
                .bind(iosNoticeQueue)
                .to(noticeHeadersExchange)
                .whereAll(Map.of(
                        "platform", "ios",
                        "type", "notice"
                ))
                .match();
    }

    @Bean
    public Binding androidNoticeBinding(HeadersExchange noticeHeadersExchange, Queue androidNoticeQueue) {
        return BindingBuilder
                .bind(androidNoticeQueue)
                .to(noticeHeadersExchange)
                .whereAny(Map.of(
                        "platform", "android",
                        "type", "notice"
                ))
                .match();
    }

}
```

发送 Headers Exchange 消息时，需要通过 `MessagePostProcessor` 设置消息头。Routing Key 可以传空字符串，因为 Headers Exchange 不依赖 Routing Key 进行匹配。

```java
rabbitTemplate.convertAndSend(
        RabbitMqHeadersConstant.NOTICE_HEADERS_EXCHANGE,
        "",
        noticeMessage,
        message -> {
            message.getMessageProperties().setHeader("platform", "ios");
            message.getMessageProperties().setHeader("type", "notice");
            return message;
        }
);
```

上面的消息会匹配 `iosNoticeBinding`，因为该绑定要求同时满足：

```text
platform = ios
type = notice
```

如果使用 `whereAny`，则表示任意一个 Header 条件满足即可匹配。需要注意，`any` 匹配虽然灵活，但也更容易造成消息被投递到非预期队列，因此生产环境中要谨慎使用。

Headers Exchange 使用时需要注意以下事项。

| 注意事项           | 说明                                          |
| ------------------ | --------------------------------------------- |
| 不依赖 Routing Key | 路由条件来自消息 Header                       |
| 可读性相对较弱     | 控制台排查时不如 Routing Key 直观             |
| 不建议高频使用     | 大多数业务用 Direct 或 Topic 更合适           |
| Header 名称要统一  | 生产者和绑定关系必须使用一致的 Header Key     |
| 谨慎使用 `any`     | 任意匹配可能导致消息进入多个非预期队列        |
| 适合特殊条件路由   | 多维条件确实无法通过 Routing Key 表达时再使用 |

四种交换机模型的选择建议如下。

| 业务需求                     | 推荐交换机       |
| ---------------------------- | ---------------- |
| 明确业务事件，精确投递       | Direct Exchange  |
| 一个消息通知多个业务系统     | Fanout Exchange  |
| 按业务域、动作、状态灵活订阅 | Topic Exchange   |
| 根据消息头多个条件组合投递   | Headers Exchange |

在大多数 Spring Boot 业务系统中，推荐优先使用 Direct Exchange 和 Topic Exchange。Direct Exchange 适合简单明确的业务路由，Topic Exchange 适合事件体系较多、订阅关系可能扩展的业务系统。Fanout Exchange 用于广播，Headers Exchange 只在确实需要 Header 条件路由时使用。

## 消息发送开发

本章节用于说明 Spring Boot 项目中常见的 RabbitMQ 消息发送方式，包括简单字符串消息、对象消息、延迟消息和批量消息。消息发送主要依赖 `RabbitTemplate`，它提供了 `convertAndSend` 等方法，可以将 Java 对象转换为 AMQP 消息后发送到指定交换机和 Routing Key。([Home](https://docs.spring.io/spring-amqp/docs/current/api/org/springframework/amqp/rabbit/core/RabbitTemplate.html?utm_source=chatgpt.com)) 本章节对应上传大纲中的“消息发送开发”部分。

在业务开发中，不建议在 Controller 中直接调用 `RabbitTemplate`。推荐封装独立的 Producer 组件，由业务层调用 Producer，再由 Producer 统一处理交换机、Routing Key、消息 ID、日志、异常和消息属性。

推荐消息发送结构如下。

```text
src/main/java/io/github/atengk
├── constant
│   └── RabbitMqSendConstant.java
├── message
│   └── OrderMessage.java
├── producer
│   └── MessageSendProducer.java
└── controller
    └── MessageSendController.java
```

### 简单消息发送

简单消息发送通常用于发送字符串、数字、状态标识、简单通知等轻量消息。它适合用于快速验证 RabbitMQ 是否连通、交换机和队列是否绑定正确、消费者是否能够正常监听。

简单消息发送的核心方法如下。

```java
rabbitTemplate.convertAndSend(exchange, routingKey, message);
```

其中，`exchange` 表示目标交换机名称，`routingKey` 表示消息路由键，`message` 表示消息内容。`RabbitTemplate` 的 `convertAndSend` 方法会将传入对象转换为 AMQP 消息后发送到指定位置。([Home](https://docs.spring.io/spring-amqp/docs/current/api/org/springframework/amqp/rabbit/core/RabbitTemplate.html?utm_source=chatgpt.com))

先定义消息发送相关常量。

文件位置：`src/main/java/io/github/atengk/constant/RabbitMqSendConstant.java`

```java
package io.github.atengk.constant;

/**
 * RabbitMQ 消息发送常量
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class RabbitMqSendConstant {

    /**
     * 普通 Direct 交换机
     */
    public static final String SIMPLE_DIRECT_EXCHANGE = "simple.direct.exchange";

    /**
     * 简单消息队列
     */
    public static final String SIMPLE_MESSAGE_QUEUE = "simple.message.queue";

    /**
     * 简单消息 Routing Key
     */
    public static final String SIMPLE_MESSAGE_ROUTING_KEY = "simple.message";

    /**
     * 订单 Direct 交换机
     */
    public static final String ORDER_DIRECT_EXCHANGE = "order.direct.exchange";

    /**
     * 订单消息队列
     */
    public static final String ORDER_MESSAGE_QUEUE = "order.message.queue";

    /**
     * 订单消息 Routing Key
     */
    public static final String ORDER_MESSAGE_ROUTING_KEY = "order.message";

    /**
     * 延迟交换机
     */
    public static final String DELAYED_EXCHANGE = "order.delayed.exchange";

    /**
     * 延迟订单队列
     */
    public static final String DELAYED_ORDER_QUEUE = "order.delayed.queue";

    /**
     * 延迟订单 Routing Key
     */
    public static final String DELAYED_ORDER_ROUTING_KEY = "order.delayed";

    private RabbitMqSendConstant() {
    }

}
```

下面配置简单消息和订单消息使用的 Direct Exchange、Queue 和 Binding。

文件位置：`src/main/java/io/github/atengk/config/RabbitMqSendConfig.java`

```java
package io.github.atengk.config;

import io.github.atengk.constant.RabbitMqSendConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * RabbitMQ 消息发送配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
public class RabbitMqSendConfig {

    @Bean
    public DirectExchange simpleDirectExchange() {
        return ExchangeBuilder
                .directExchange(RabbitMqSendConstant.SIMPLE_DIRECT_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue simpleMessageQueue() {
        return QueueBuilder
                .durable(RabbitMqSendConstant.SIMPLE_MESSAGE_QUEUE)
                .build();
    }

    @Bean
    public Binding simpleMessageBinding(DirectExchange simpleDirectExchange, Queue simpleMessageQueue) {
        return BindingBuilder
                .bind(simpleMessageQueue)
                .to(simpleDirectExchange)
                .with(RabbitMqSendConstant.SIMPLE_MESSAGE_ROUTING_KEY);
    }

    @Bean
    public DirectExchange orderDirectExchange() {
        return ExchangeBuilder
                .directExchange(RabbitMqSendConstant.ORDER_DIRECT_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue orderMessageQueue() {
        return QueueBuilder
                .durable(RabbitMqSendConstant.ORDER_MESSAGE_QUEUE)
                .build();
    }

    @Bean
    public Binding orderMessageBinding(DirectExchange orderDirectExchange, Queue orderMessageQueue) {
        return BindingBuilder
                .bind(orderMessageQueue)
                .to(orderDirectExchange)
                .with(RabbitMqSendConstant.ORDER_MESSAGE_ROUTING_KEY);
    }

    @Bean
    public CustomExchange delayedExchange() {
        return new CustomExchange(
                RabbitMqSendConstant.DELAYED_EXCHANGE,
                "x-delayed-message",
                true,
                false,
                Map.of("x-delayed-type", "direct")
        );
    }

    @Bean
    public Queue delayedOrderQueue() {
        return QueueBuilder
                .durable(RabbitMqSendConstant.DELAYED_ORDER_QUEUE)
                .build();
    }

    @Bean
    public Binding delayedOrderBinding(CustomExchange delayedExchange, Queue delayedOrderQueue) {
        return BindingBuilder
                .bind(delayedOrderQueue)
                .to(delayedExchange)
                .with(RabbitMqSendConstant.DELAYED_ORDER_ROUTING_KEY)
                .noargs();
    }

}
```

下面封装简单字符串消息发送方法。

文件位置：`src/main/java/io/github/atengk/producer/MessageSendProducer.java`

```java
package io.github.atengk.producer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.constant.RabbitMqSendConstant;
import io.github.atengk.message.OrderMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RabbitMQ 消息发送生产者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSendProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendSimpleMessage(String content) {
        if (StrUtil.isBlank(content)) {
            throw new IllegalArgumentException("消息内容不能为空");
        }

        String messageId = IdUtil.fastSimpleUUID();

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqSendConstant.SIMPLE_DIRECT_EXCHANGE,
                    RabbitMqSendConstant.SIMPLE_MESSAGE_ROUTING_KEY,
                    content,
                    message -> {
                        message.getMessageProperties().setMessageId(messageId);
                        message.getMessageProperties().setContentType("text/plain");
                        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        message.getMessageProperties().setTimestamp(new java.util.Date());
                        return message;
                    }
            );

            log.info("简单消息发送成功，messageId={}，content={}", messageId, content);
        } catch (AmqpException e) {
            log.error("简单消息发送失败，messageId={}，content={}", messageId, content, e);
            throw e;
        }
    }

    public void sendOrderMessage(OrderMessage orderMessage) {
        fillOrderMessage(orderMessage);

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqSendConstant.ORDER_DIRECT_EXCHANGE,
                    RabbitMqSendConstant.ORDER_MESSAGE_ROUTING_KEY,
                    orderMessage,
                    message -> {
                        message.getMessageProperties().setMessageId(orderMessage.getMessageId());
                        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        message.getMessageProperties().setHeader("businessType", "ORDER");
                        message.getMessageProperties().setHeader("sendTime", LocalDateTimeUtil.format(LocalDateTime.now(), DatePattern.NORM_DATETIME_PATTERN));
                        return message;
                    }
            );

            log.info("订单对象消息发送成功，messageId={}，orderNo={}",
                    orderMessage.getMessageId(), orderMessage.getOrderNo());
        } catch (AmqpException e) {
            log.error("订单对象消息发送失败，messageId={}，orderNo={}",
                    orderMessage.getMessageId(), orderMessage.getOrderNo(), e);
            throw e;
        }
    }

    public void sendDelayedOrderMessage(OrderMessage orderMessage, Integer delayMillis) {
        fillOrderMessage(orderMessage);

        if (delayMillis == null || delayMillis <= 0) {
            throw new IllegalArgumentException("延迟时间必须大于0毫秒");
        }

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqSendConstant.DELAYED_EXCHANGE,
                    RabbitMqSendConstant.DELAYED_ORDER_ROUTING_KEY,
                    orderMessage,
                    message -> {
                        message.getMessageProperties().setMessageId(orderMessage.getMessageId());
                        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        message.getMessageProperties().setHeader("x-delay", delayMillis);
                        message.getMessageProperties().setHeader("businessType", "ORDER_DELAYED");
                        return message;
                    }
            );

            log.info("延迟订单消息发送成功，messageId={}，orderNo={}，delayMillis={}",
                    orderMessage.getMessageId(), orderMessage.getOrderNo(), delayMillis);
        } catch (AmqpException e) {
            log.error("延迟订单消息发送失败，messageId={}，orderNo={}，delayMillis={}",
                    orderMessage.getMessageId(), orderMessage.getOrderNo(), delayMillis, e);
            throw e;
        }
    }

    public int sendOrderMessageBatch(List<OrderMessage> orderMessageList) {
        if (CollUtil.isEmpty(orderMessageList)) {
            log.warn("批量订单消息为空，跳过发送");
            return 0;
        }

        int successCount = 0;

        for (OrderMessage orderMessage : orderMessageList) {
            try {
                this.sendOrderMessage(orderMessage);
                successCount++;
            } catch (Exception e) {
                log.error("批量订单消息发送失败，orderNo={}",
                        orderMessage == null ? null : orderMessage.getOrderNo(), e);
            }
        }

        log.info("批量订单消息发送完成，总数={}，成功数={}，失败数={}",
                orderMessageList.size(), successCount, orderMessageList.size() - successCount);

        return successCount;
    }

    private void fillOrderMessage(OrderMessage orderMessage) {
        if (orderMessage == null) {
            throw new IllegalArgumentException("订单消息不能为空");
        }

        if (StrUtil.isBlank(orderMessage.getOrderNo())) {
            orderMessage.setOrderNo("ORDER_" + IdUtil.getSnowflakeNextIdStr());
        }

        if (StrUtil.isBlank(orderMessage.getMessageId())) {
            orderMessage.setMessageId(IdUtil.fastSimpleUUID());
        }

        if (orderMessage.getCreateTime() == null) {
            orderMessage.setCreateTime(LocalDateTime.now());
        }
    }

}
```

简单消息发送适合用于测试链路连通性。发送后可以在 RabbitMQ 管理控制台查看 `simple.message.queue` 队列，如果没有消费者，消息会进入 `Ready` 状态；如果消费者在线，消息会被立即投递并消费。

### 对象消息发送

对象消息发送是业务开发中最常见的方式。生产者通常不会只发送字符串，而是发送一个包含业务字段的消息对象，例如订单消息、支付消息、通知消息、日志消息等。

对象消息发送依赖前面章节配置的 `MessageConverter`。如果使用 `Jackson2JsonMessageConverter`，`RabbitTemplate` 会将 Java 对象序列化为 JSON 消息体，消费者也可以按对象类型接收消息。Spring AMQP 的消息转换器用于在 Java 对象和 AMQP Message 之间转换，JSON 转换器是实际业务中更适合跨语言和排查问题的选择。([Home](https://docs.spring.io/spring-amqp/docs/current/api/org/springframework/amqp/rabbit/core/RabbitTemplate.html?utm_source=chatgpt.com))

订单消息实体建议包含消息 ID、业务主键、业务状态和创建时间等字段。消息 ID 后续可以用于生产确认、日志追踪和幂等消费。

文件位置：`src/main/java/io/github/atengk/message/OrderMessage.java`

```java
package io.github.atengk.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单业务消息
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessage {

    /**
     * 消息ID，用于日志追踪和幂等处理
     */
    private String messageId;

    /**
     * 订单号
     */
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 订单金额
     */
    private BigDecimal amount;

    /**
     * 订单状态
     */
    private String status;

    /**
     * 消息创建时间
     */
    private LocalDateTime createTime;

}
```

对象消息发送调用方式如下。

```java
OrderMessage orderMessage = OrderMessage.builder()
        .orderNo("ORDER_202604300001")
        .userId(10001L)
        .amount(new BigDecimal("99.90"))
        .status("CREATED")
        .build();

messageSendProducer.sendOrderMessage(orderMessage);
```

为了便于本地联调，可以提供一个 HTTP 接口触发对象消息发送。

文件位置：`src/main/java/io/github/atengk/controller/MessageSendController.java`

```java
package io.github.atengk.controller;

import io.github.atengk.message.OrderMessage;
import io.github.atengk.producer.MessageSendProducer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RabbitMQ 消息发送测试接口
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequestMapping("/api/mq/send")
@RequiredArgsConstructor
public class MessageSendController {

    private final MessageSendProducer messageSendProducer;

    @PostMapping("/simple")
    public ResponseEntity<String> sendSimpleMessage(@RequestParam String content) {
        messageSendProducer.sendSimpleMessage(content);
        return ResponseEntity.ok("简单消息发送成功");
    }

    @PostMapping("/order")
    public ResponseEntity<String> sendOrderMessage(@Valid @RequestBody OrderMessage orderMessage) {
        messageSendProducer.sendOrderMessage(orderMessage);
        return ResponseEntity.ok("订单消息发送成功");
    }

    @PostMapping("/order/delayed")
    public ResponseEntity<String> sendDelayedOrderMessage(@Valid @RequestBody OrderMessage orderMessage,
                                                          @RequestParam Integer delayMillis) {
        messageSendProducer.sendDelayedOrderMessage(orderMessage, delayMillis);
        return ResponseEntity.ok("延迟订单消息发送成功");
    }

    @PostMapping("/order/batch")
    public ResponseEntity<String> sendOrderMessageBatch(@RequestBody List<OrderMessage> orderMessageList) {
        int successCount = messageSendProducer.sendOrderMessageBatch(orderMessageList);
        return ResponseEntity.ok("批量订单消息发送完成，成功数：" + successCount);
    }

}
```

本地验证对象消息发送。

```bash
curl -X POST "http://localhost:8080/api/mq/send/order" \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "ORDER_202604300001",
    "userId": 10001,
    "amount": 99.90,
    "status": "CREATED"
  }'
```

对象消息发送时建议遵循以下原则。

| 原则               | 说明                                      |
| ------------------ | ----------------------------------------- |
| 消息对象保持轻量   | 只放消费者处理所需字段，不放完整大对象    |
| 必须包含业务主键   | 例如订单号、支付单号、用户 ID 等          |
| 建议包含消息 ID    | 用于日志追踪、幂等消费和问题定位          |
| 字段变更要兼容     | 新增字段时消费者应兼容旧消息              |
| 不直接发送敏感数据 | 密码、密钥、身份证等敏感字段不建议进入 MQ |
| 时间格式要统一     | 建议使用统一 `ObjectMapper` 处理时间类型  |

### 延迟消息发送

延迟消息是指生产者发送消息后，消费者不会立即收到，而是在指定时间之后才收到。RabbitMQ 常见延迟消息实现方式有两类：一种是使用消息 TTL 配合死信交换机，另一种是使用 RabbitMQ Delayed Message Exchange 插件。RabbitMQ TTL 表示消息或队列的存活时间，消息超过 TTL 后会过期；过期消息可以结合死信交换机进入目标队列，实现延迟消费。([RabbitMQ](https://www.rabbitmq.com/docs/4.0/ttl?utm_source=chatgpt.com))

本小节先演示基于 `x-delayed-message` 插件的发送方式，因为它可以按每条消息设置不同延迟时间。RabbitMQ delayed message exchange 插件支持声明 `x-delayed-message` 类型交换机，并通过消息头 `x-delay` 指定延迟毫秒数，延迟到期后再将消息路由到目标队列。([GitHub](https://github.com/rabbitmq/rabbitmq-delayed-message-exchange?utm_source=chatgpt.com))

使用插件前，需要在 RabbitMQ 节点启用插件。

```bash
# 进入 RabbitMQ 容器
docker exec -it rabbitmq bash

# 启用 RabbitMQ 延迟消息插件
rabbitmq-plugins enable rabbitmq_delayed_message_exchange

# 查看插件列表
rabbitmq-plugins list
```

如果镜像中没有内置该插件，需要下载与 RabbitMQ 版本匹配的 `.ez` 插件文件，并放入 RabbitMQ 插件目录后再启用。插件的每个发布版本通常对应特定 RabbitMQ 发布系列，因此需要注意 RabbitMQ 服务版本和插件版本匹配。([GitHub](https://github.com/rabbitmq/rabbitmq-delayed-message-exchange?utm_source=chatgpt.com))

延迟交换机已经在前面的配置类中声明。

```java
@Bean
public CustomExchange delayedExchange() {
    return new CustomExchange(
            RabbitMqSendConstant.DELAYED_EXCHANGE,
            "x-delayed-message",
            true,
            false,
            Map.of("x-delayed-type", "direct")
    );
}
```

其中，`x-delayed-type` 表示延迟到期后使用哪种交换机路由行为。设置为 `direct` 时，延迟交换机会按照 Direct Exchange 的方式使用 Routing Key 进行精确路由。RabbitMQ delayed message exchange 插件也说明，`x-delayed-type` 用于指定该延迟交换机实际代理的路由类型。([GitHub](https://github.com/rabbitmq/rabbitmq-delayed-message-exchange?utm_source=chatgpt.com))

延迟消息发送方法如下。

```java
rabbitTemplate.convertAndSend(
        RabbitMqSendConstant.DELAYED_EXCHANGE,
        RabbitMqSendConstant.DELAYED_ORDER_ROUTING_KEY,
        orderMessage,
        message -> {
            message.getMessageProperties().setHeader("x-delay", delayMillis);
            return message;
        }
);
```

本地验证发送一条 10 秒后投递的订单消息。

```bash
curl -X POST "http://localhost:8080/api/mq/send/order/delayed?delayMillis=10000" \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "ORDER_DELAY_202604300001",
    "userId": 10001,
    "amount": 199.90,
    "status": "WAIT_CLOSE"
  }'
```

发送后，消息不会立刻进入 `order.delayed.queue`。等待约 10 秒后，消息才会被路由到队列中。如果此时消费者在线，消息会被立即消费；如果消费者不在线，消息会在队列中处于 `Ready` 状态。

延迟消息常见业务场景如下。

| 场景         | 示例                                   |
| ------------ | -------------------------------------- |
| 订单超时关闭 | 下单 30 分钟未支付后关闭订单           |
| 支付结果补偿 | 支付状态不确定时延迟查询第三方支付结果 |
| 通知延迟发送 | 注册后 10 分钟发送引导通知             |
| 任务延迟重试 | 外部接口失败后延迟 1 分钟重试          |
| 活动状态变更 | 活动开始或结束时延迟触发状态更新       |

延迟消息使用时需要注意以下事项。

| 注意事项               | 说明                                                         |
| ---------------------- | ------------------------------------------------------------ |
| 插件需要额外安装       | `x-delayed-message` 不是所有 RabbitMQ 镜像都默认可用         |
| 延迟时间使用毫秒       | `x-delay=10000` 表示延迟 10 秒                               |
| 不适合超长期调度       | 插件文档明确提示不适合作为长期调度方案，长周期任务建议使用数据库和外部调度器 |
| 大量延迟消息要谨慎     | 高数量延迟消息可能带来性能和存储压力                         |
| 生产环境要验证插件版本 | RabbitMQ 版本和插件版本需要匹配                              |
| 关键业务要有兜底       | 订单超时关闭等场景建议同时具备定时任务扫描补偿               |

对于订单超时关闭这类关键业务，不建议只依赖 MQ 延迟消息。更稳妥的方式是“延迟消息触发 + 定时任务兜底扫描”。延迟消息负责大多数正常关闭场景，定时任务负责补偿 MQ 异常、服务停机、消息丢失或消费失败等边界情况。

### 批量消息发送

批量消息发送用于一次性发送多条业务消息，例如批量通知、批量同步、批量创建任务、批量补偿数据等。RabbitMQ 本身的发送动作仍然是一条消息一次 publish，业务代码中的“批量发送”通常是指循环发送多条消息，并统计成功和失败结果。

批量发送不建议简单粗暴地吞掉异常。实际业务中至少需要记录失败消息、失败原因和业务主键，必要时落库用于后续补偿。

前面 `MessageSendProducer` 中已经提供了基础批量发送方法。

```java
public int sendOrderMessageBatch(List<OrderMessage> orderMessageList) {
    if (CollUtil.isEmpty(orderMessageList)) {
        log.warn("批量订单消息为空，跳过发送");
        return 0;
    }

    int successCount = 0;

    for (OrderMessage orderMessage : orderMessageList) {
        try {
            this.sendOrderMessage(orderMessage);
            successCount++;
        } catch (Exception e) {
            log.error("批量订单消息发送失败，orderNo={}",
                    orderMessage == null ? null : orderMessage.getOrderNo(), e);
        }
    }

    log.info("批量订单消息发送完成，总数={}，成功数={}，失败数={}",
            orderMessageList.size(), successCount, orderMessageList.size() - successCount);

    return successCount;
}
```

本地验证批量发送订单消息。

```bash
curl -X POST "http://localhost:8080/api/mq/send/order/batch" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "orderNo": "ORDER_BATCH_202604300001",
      "userId": 10001,
      "amount": 99.90,
      "status": "CREATED"
    },
    {
      "orderNo": "ORDER_BATCH_202604300002",
      "userId": 10002,
      "amount": 199.90,
      "status": "CREATED"
    },
    {
      "orderNo": "ORDER_BATCH_202604300003",
      "userId": 10003,
      "amount": 299.90,
      "status": "CREATED"
    }
  ]'
```

如果批量消息对可靠性要求较高，建议返回更详细的发送结果，而不是只返回成功数量。下面定义一个批量发送结果对象。

文件位置：`src/main/java/io/github/atengk/message/BatchSendResult.java`

```java
package io.github.atengk.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量消息发送结果
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSendResult {

    /**
     * 总数量
     */
    private Integer totalCount;

    /**
     * 成功数量
     */
    private Integer successCount;

    /**
     * 失败数量
     */
    private Integer failCount;

    /**
     * 失败订单号列表
     */
    private List<String> failOrderNoList;

}
```

下面是一个返回详细结果的批量发送方法，可用于替换基础统计版本。

```java
public BatchSendResult sendOrderMessageBatchWithResult(List<OrderMessage> orderMessageList) {
    if (CollUtil.isEmpty(orderMessageList)) {
        log.warn("批量订单消息为空，跳过发送");
        return BatchSendResult.builder()
                .totalCount(0)
                .successCount(0)
                .failCount(0)
                .failOrderNoList(List.of())
                .build();
    }

    List<String> failOrderNoList = CollUtil.newArrayList();
    int successCount = 0;

    for (OrderMessage orderMessage : orderMessageList) {
        try {
            this.sendOrderMessage(orderMessage);
            successCount++;
        } catch (Exception e) {
            String orderNo = orderMessage == null ? null : orderMessage.getOrderNo();
            failOrderNoList.add(orderNo);
            log.error("批量订单消息发送失败，orderNo={}", orderNo, e);
        }
    }

    BatchSendResult result = BatchSendResult.builder()
            .totalCount(orderMessageList.size())
            .successCount(successCount)
            .failCount(failOrderNoList.size())
            .failOrderNoList(failOrderNoList)
            .build();

    log.info("批量订单消息发送结果，result={}", result);
    return result;
}
```

批量发送时需要根据业务重要程度选择不同策略。

| 策略     | 说明                           | 适用场景                   |
| -------- | ------------------------------ | -------------------------- |
| 遇错继续 | 单条失败不影响后续消息发送     | 批量通知、日志同步         |
| 遇错终止 | 任意一条失败立即停止           | 强一致批处理、顺序敏感业务 |
| 失败落库 | 发送失败记录到数据库，后续补偿 | 重要业务消息               |
| 异步批量 | 将批次拆分后异步发送           | 大批量低实时性任务         |
| 限流发送 | 控制发送速率，避免冲击 Broker  | 大规模补偿或导入任务       |

批量发送开发建议如下。

| 建议              | 说明                                         |
| ----------------- | -------------------------------------------- |
| 控制单批数量      | 不建议一次接口请求发送几万条消息             |
| 记录业务主键      | 日志中必须包含订单号、用户 ID 等可追踪字段   |
| 区分成功和失败    | 返回或记录成功数、失败数、失败明细           |
| 重要消息要补偿    | 发送失败要落库或进入补偿任务                 |
| 避免大事务包裹 MQ | 数据库事务和 MQ 发送不要随意混在一个长事务中 |
| 配合确认机制      | 生产者确认机制会在后续可靠性章节展开         |

完成本章节后，项目已经具备常见消息发送能力：可以发送字符串消息、对象消息、延迟消息和批量消息。后续章节将继续说明如何开发消费者、如何处理手动确认、消费异常、并发消费以及可靠性保障。

## 消息消费开发

本章节用于说明 Spring Boot 项目中 RabbitMQ 消息消费的常见开发方式，包括基于 `@RabbitListener` 的消费者、手动确认消费、消费异常处理和并发消费配置。RabbitMQ 的消费者确认机制用于告诉 Broker 消息是否已经被正确处理，手动确认模式下通常通过 `basicAck`、`basicNack` 或 `basicReject` 控制消息确认、拒绝、丢弃或重新入队。RabbitMQ 官方文档说明，`basic.ack` 用于正向确认，`basic.nack` 和 `basic.reject` 用于负向确认，且 delivery tag 是在 Channel 范围内标识一次投递的编号。([RabbitMQ](https://www.rabbitmq.com/docs/4.0/confirms?utm_source=chatgpt.com)) 本章节对应上传大纲中的“消息消费开发”部分。

在 Spring Boot 项目中，推荐使用 `@RabbitListener` 声明消费者。固定队列的消费者建议单独放在 `consumer` 包中，消费者只处理消息接收、参数校验、日志记录、确认消息和调用业务服务，不建议把复杂业务逻辑全部写在监听方法中。

推荐目录结构如下。

```text
src/main/java/io/github/atengk
├── config
│   └── RabbitMqListenerConfig.java
├── consumer
│   ├── SimpleMessageConsumer.java
│   └── OrderMessageConsumer.java
├── constant
│   └── RabbitMqSendConstant.java
├── message
│   └── OrderMessage.java
└── service
    └── OrderMessageService.java
```

### 基于注解的消费者

基于注解的消费者是 Spring Boot 集成 RabbitMQ 最常用的消费方式。通过 `@RabbitListener` 指定监听队列，Spring AMQP 会创建监听容器并自动从队列中拉取消息，然后调用对应方法执行业务逻辑。

最简单的字符串消息消费者如下。

文件位置：`src/main/java/io/github/atengk/consumer/SimpleMessageConsumer.java`

```java
package io.github.atengk.consumer;

import io.github.atengk.constant.RabbitMqSendConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 简单消息消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class SimpleMessageConsumer {

    /**
     * 消费简单字符串消息
     *
     * @param content 消息内容
     */
    @RabbitListener(queues = RabbitMqSendConstant.SIMPLE_MESSAGE_QUEUE)
    public void consumeSimpleMessage(String content) {
        log.info("接收到简单消息，content={}", content);

        // 这里编写实际业务处理逻辑
        log.info("简单消息处理完成，content={}", content);
    }

}
```

对象消息消费者如下。该消费者依赖前面章节配置的 `Jackson2JsonMessageConverter`，生产者发送 `OrderMessage` 对象后，消费者可以直接使用 `OrderMessage` 参数接收。

文件位置：`src/main/java/io/github/atengk/consumer/OrderMessageConsumer.java`

```java
package io.github.atengk.consumer;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.constant.RabbitMqSendConstant;
import io.github.atengk.message.OrderMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 订单消息消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class OrderMessageConsumer {

    /**
     * 消费订单对象消息
     *
     * @param orderMessage 订单消息
     */
    @RabbitListener(queues = RabbitMqSendConstant.ORDER_MESSAGE_QUEUE)
    public void consumeOrderMessage(OrderMessage orderMessage) {
        if (orderMessage == null || StrUtil.isBlank(orderMessage.getOrderNo())) {
            log.warn("订单消息为空或订单号为空，message={}", orderMessage);
            return;
        }

        log.info("接收到订单消息，messageId={}，orderNo={}，status={}",
                orderMessage.getMessageId(), orderMessage.getOrderNo(), orderMessage.getStatus());

        // 这里调用订单业务服务，例如更新订单状态、发送通知、写入日志等
        log.info("订单消息处理完成，messageId={}，orderNo={}",
                orderMessage.getMessageId(), orderMessage.getOrderNo());
    }

}
```

如果当前使用的是自动确认模式，监听方法正常执行完成后，Spring AMQP 会认为消息已经处理成功；如果监听方法抛出异常，则由监听容器的异常策略决定是否重新入队、重试或丢弃。自动确认模式适合简单、低风险的消费场景，但对于订单、支付、库存、账务等重要业务，推荐使用手动确认模式。

`@RabbitListener` 常用配置如下。

| 配置               | 说明                                                 |
| ------------------ | ---------------------------------------------------- |
| `queues`           | 指定监听的队列名称                                   |
| `containerFactory` | 指定监听容器工厂，用于控制确认模式、并发、预取数量等 |
| `id`               | 指定监听器 ID，便于管理和日志定位                    |
| `concurrency`      | 指定当前监听器并发，例如 `2-6`                       |
| `autoStartup`      | 是否随应用启动自动启动监听器                         |

例如，为当前消费者单独设置监听器 ID 和并发范围。

```java
@RabbitListener(
        id = "orderMessageListener",
        queues = RabbitMqSendConstant.ORDER_MESSAGE_QUEUE,
        concurrency = "2-4"
)
public void consumeOrderMessage(OrderMessage orderMessage) {
    log.info("接收到订单消息，orderNo={}", orderMessage.getOrderNo());
}
```

### 手动确认消费

手动确认消费用于显式控制消息的确认行为。消费者只有在业务处理成功后才调用 `basicAck`，如果业务处理失败，则根据异常类型选择 `basicNack` 或 `basicReject`，并决定是否重新入队。RabbitMQ 官方文档说明，手动确认可以使用 `basic.ack`、`basic.nack` 和 `basic.reject`，其中 `basic.nack` 是 RabbitMQ 对 AMQP 0-9-1 的扩展，并支持批量拒绝。([RabbitMQ](https://www.rabbitmq.com/docs/4.0/confirms?utm_source=chatgpt.com))

推荐在重要业务消息中使用手动确认，例如订单创建、支付成功、库存扣减、数据同步等场景。手动确认的关键点是：业务成功后再确认，业务失败时不要盲目重新入队，否则可能造成消息无限重投。

先配置一个手动确认监听容器工厂。

文件位置：`src/main/java/io/github/atengk/config/RabbitMqListenerConfig.java`

```java
package io.github.atengk.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 监听容器配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
@RequiredArgsConstructor
public class RabbitMqListenerConfig {

    private final ConnectionFactory connectionFactory;

    private final MessageConverter messageConverter;

    /**
     * 手动确认监听容器工厂
     *
     * @return 手动确认监听容器工厂
     */
    @Bean(name = "manualRabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory manualRabbitListenerContainerFactory() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();

        // RabbitMQ 连接工厂
        factory.setConnectionFactory(connectionFactory);

        // 使用项目统一的 JSON 消息转换器
        factory.setMessageConverter(messageConverter);

        // 手动确认模式，业务处理成功后需要显式 basicAck
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);

        // 每个消费者最多同时持有的未确认消息数
        factory.setPrefetchCount(10);

        // 初始消费者数量
        factory.setConcurrentConsumers(2);

        // 最大消费者数量
        factory.setMaxConcurrentConsumers(6);

        // 消费异常默认不重新入队，避免异常消息无限重试
        factory.setDefaultRequeueRejected(false);

        return factory;
    }

}
```

下面是手动确认的订单消费者示例。该示例在业务处理成功时调用 `basicAck`；在业务异常时调用 `basicNack`，并设置 `requeue=false`，避免异常消息不断重新进入队列造成死循环。

文件位置：`src/main/java/io/github/atengk/consumer/OrderManualAckConsumer.java`

```java
package io.github.atengk.consumer;

import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.Channel;
import io.github.atengk.constant.RabbitMqSendConstant;
import io.github.atengk.message.OrderMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 订单手动确认消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class OrderManualAckConsumer {

    /**
     * 手动确认消费订单消息
     *
     * @param orderMessage 订单消息
     * @param message      RabbitMQ 原始消息
     * @param channel      RabbitMQ 通道
     * @throws IOException 确认消息失败时抛出
     */
    @RabbitListener(
            queues = RabbitMqSendConstant.ORDER_MESSAGE_QUEUE,
            containerFactory = "manualRabbitListenerContainerFactory"
    )
    public void consumeOrderMessage(OrderMessage orderMessage, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            if (orderMessage == null || StrUtil.isBlank(orderMessage.getOrderNo())) {
                log.warn("订单消息非法，直接拒绝，deliveryTag={}，message={}", deliveryTag, orderMessage);
                channel.basicReject(deliveryTag, false);
                return;
            }

            log.info("开始处理订单消息，deliveryTag={}，messageId={}，orderNo={}",
                    deliveryTag, orderMessage.getMessageId(), orderMessage.getOrderNo());

            // 这里调用真实业务处理逻辑，例如订单状态更新、积分发放、通知发送等
            this.handleOrderBusiness(orderMessage);

            // 业务处理成功后确认消息
            channel.basicAck(deliveryTag, false);

            log.info("订单消息确认成功，deliveryTag={}，messageId={}，orderNo={}",
                    deliveryTag, orderMessage.getMessageId(), orderMessage.getOrderNo());
        } catch (Exception e) {
            log.error("订单消息处理失败，准备拒绝消息，deliveryTag={}，message={}", deliveryTag, orderMessage, e);

            // requeue=false 表示不重新入队，后续可配合死信队列兜底
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 处理订单业务
     *
     * @param orderMessage 订单消息
     */
    private void handleOrderBusiness(OrderMessage orderMessage) {
        log.info("执行订单业务处理，orderNo={}，status={}",
                orderMessage.getOrderNo(), orderMessage.getStatus());

        // 示例：这里替换为真实业务代码
    }

}
```

`basicAck`、`basicNack` 和 `basicReject` 的常用参数含义如下。

| 方法                                        | 参数             | 说明                                       |
| ------------------------------------------- | ---------------- | ------------------------------------------ |
| `basicAck(deliveryTag, multiple)`           | `multiple=false` | 只确认当前这一条消息                       |
| `basicAck(deliveryTag, multiple)`           | `multiple=true`  | 确认当前 deliveryTag 之前所有未确认消息    |
| `basicNack(deliveryTag, multiple, requeue)` | `requeue=true`   | 拒绝消息并重新入队                         |
| `basicNack(deliveryTag, multiple, requeue)` | `requeue=false`  | 拒绝消息且不重新入队，通常进入死信或被丢弃 |
| `basicReject(deliveryTag, requeue)`         | `requeue=true`   | 拒绝单条消息并重新入队                     |
| `basicReject(deliveryTag, requeue)`         | `requeue=false`  | 拒绝单条消息且不重新入队                   |

手动确认模式下，建议默认使用 `multiple=false`。批量确认虽然可以减少网络交互，但一旦业务处理和确认边界设计不严谨，容易误确认尚未真正处理成功的消息。RabbitMQ 文档也说明，当 `multiple=true` 时，会确认当前 delivery tag 及其之前所有未确认的投递。([RabbitMQ](https://www.rabbitmq.com/docs/3.13/confirms?utm_source=chatgpt.com))

手动确认消费建议如下。

| 建议                 | 说明                                                   |
| -------------------- | ------------------------------------------------------ |
| 成功后再 ack         | 业务处理完成后再调用 `basicAck`                        |
| 异常不盲目 requeue   | 持续失败的消息反复入队会造成死循环                     |
| 非法消息直接 reject  | 参数缺失、格式错误等消息不应重复消费                   |
| 配合死信队列         | `requeue=false` 时建议有死信交换机承接失败消息         |
| 日志必须包含业务主键 | 至少记录 messageId、orderNo、deliveryTag               |
| 不跨线程持有 Channel | Channel 应在当前消费上下文内使用，不建议传递到异步线程 |

### 消费异常处理

消费异常处理用于控制消息处理失败后的行为。常见异常包括参数异常、消息反序列化异常、业务校验失败、数据库异常、第三方接口异常和临时网络异常。异常处理策略不应一刀切，需要根据异常是否可恢复决定是重试、拒绝、进入死信还是记录告警。

消费异常可以按以下方式分类。

| 异常类型         | 示例                          | 建议处理                   |
| ---------------- | ----------------------------- | -------------------------- |
| 参数异常         | 订单号为空、金额非法          | 直接拒绝，不重新入队       |
| 反序列化异常     | JSON 格式错误、字段类型不匹配 | 拒绝或进入死信             |
| 业务不可恢复异常 | 订单不存在、状态不允许处理    | 拒绝并记录业务日志         |
| 临时外部异常     | 第三方接口超时、网络抖动      | 可重试，重试耗尽后进入死信 |
| 数据库异常       | 连接超时、死锁、锁等待超时    | 可重试，但要控制次数       |
| 代码缺陷         | 空指针、类型转换错误          | 不建议反复重试，应告警修复 |

如果使用手动确认，可以在消费者内部显式处理异常。下面示例展示了针对不同异常选择不同确认策略。

文件位置：`src/main/java/io/github/atengk/consumer/OrderExceptionConsumer.java`

```java
package io.github.atengk.consumer;

import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.Channel;
import io.github.atengk.constant.RabbitMqSendConstant;
import io.github.atengk.message.OrderMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 订单异常处理消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class OrderExceptionConsumer {

    /**
     * 消费订单消息并处理异常
     *
     * @param orderMessage 订单消息
     * @param message      RabbitMQ 原始消息
     * @param channel      RabbitMQ 通道
     * @throws IOException 确认消息失败时抛出
     */
    @RabbitListener(
            queues = RabbitMqSendConstant.ORDER_MESSAGE_QUEUE,
            containerFactory = "manualRabbitListenerContainerFactory"
    )
    public void consumeWithExceptionHandle(OrderMessage orderMessage, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            this.validateMessage(orderMessage);

            log.info("开始消费订单消息，deliveryTag={}，messageId={}，orderNo={}",
                    deliveryTag, orderMessage.getMessageId(), orderMessage.getOrderNo());

            this.handleBusiness(orderMessage);

            channel.basicAck(deliveryTag, false);

            log.info("订单消息消费成功，deliveryTag={}，messageId={}，orderNo={}",
                    deliveryTag, orderMessage.getMessageId(), orderMessage.getOrderNo());
        } catch (IllegalArgumentException e) {
            log.warn("订单消息参数异常，拒绝且不重新入队，deliveryTag={}，message={}", deliveryTag, orderMessage, e);
            channel.basicReject(deliveryTag, false);
        } catch (TransientDataAccessException e) {
            log.error("订单消息发生临时数据库异常，拒绝并重新入队，deliveryTag={}，message={}", deliveryTag, orderMessage, e);
            channel.basicNack(deliveryTag, false, true);
        } catch (Exception e) {
            log.error("订单消息发生未知异常，拒绝且不重新入队，deliveryTag={}，message={}", deliveryTag, orderMessage, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 校验订单消息
     *
     * @param orderMessage 订单消息
     */
    private void validateMessage(OrderMessage orderMessage) {
        if (orderMessage == null) {
            throw new IllegalArgumentException("订单消息不能为空");
        }

        if (StrUtil.isBlank(orderMessage.getOrderNo())) {
            throw new IllegalArgumentException("订单号不能为空");
        }

        if (orderMessage.getUserId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
    }

    /**
     * 处理订单业务
     *
     * @param orderMessage 订单消息
     */
    private void handleBusiness(OrderMessage orderMessage) {
        log.info("处理订单业务逻辑，orderNo={}，status={}",
                orderMessage.getOrderNo(), orderMessage.getStatus());

        // 示例：替换为真实业务逻辑
    }

}
```

上面的代码中，参数异常直接拒绝且不重新入队，临时数据库异常选择重新入队，未知异常拒绝且不重新入队。实际生产中，是否 `requeue=true` 要非常谨慎。如果异常一直存在，消息会被不断投递、消费、失败、重新入队，最终造成 CPU 空转、日志刷屏和队列阻塞。RabbitMQ 文档说明，被 `basic.nack` 重新入队的消息会尽量放回原位置，如果存在并发投递和确认，可能更接近队列头部。([RabbitMQ](https://www.rabbitmq.com/docs/4.1/nack?utm_source=chatgpt.com))

如果希望异常消息不要无限重新入队，推荐使用以下组合。

```text
手动确认 + basicNack(requeue=false) + 死信交换机 + 死信队列 + 告警/补偿任务
```

异常处理策略建议如下。

| 场景           | 推荐动作                           | 说明                                           |
| -------------- | ---------------------------------- | ---------------------------------------------- |
| 消息格式错误   | `basicReject(false)`               | 消息本身不可恢复，直接丢弃或进入死信           |
| 参数校验失败   | `basicReject(false)`               | 缺少业务主键时不应重试                         |
| 业务状态不允许 | `basicAck` 或 `basicReject(false)` | 如果业务已达到最终状态，可确认消息避免重复处理 |
| 临时网络异常   | `basicNack(true)` 或重试机制       | 仅在确认可恢复时重新入队                       |
| 多次重试失败   | `basicNack(false)`                 | 进入死信队列后人工或任务补偿                   |
| 代码异常       | `basicNack(false)`                 | 需要告警和修复代码，不建议无限重试             |

对于重要业务，建议将消费失败信息记录到日志、数据库或监控系统中。至少需要记录以下字段。

| 字段               | 说明              |
| ------------------ | ----------------- |
| `messageId`        | 消息唯一标识      |
| `orderNo`          | 业务主键          |
| `queue`            | 消费队列          |
| `deliveryTag`      | RabbitMQ 投递标识 |
| `exceptionType`    | 异常类型          |
| `exceptionMessage` | 异常摘要          |
| `requeue`          | 是否重新入队      |
| `createTime`       | 失败发生时间      |

### 并发消费配置

并发消费用于提升消费者处理能力。当队列消息堆积较多时，可以增加消费者并发数，让多个消费者同时从同一个队列获取消息。Spring AMQP 的 `SimpleMessageListenerContainer` 默认启动一个消费者，可以通过 `concurrentConsumers`、`maxConcurrentConsumers` 或 `concurrency` 控制并发范围；官方文档也说明，`concurrency` 可以使用 `2-4` 这类格式表示最小和最大消费者数量。([Spring Enterprise Docs](https://docs.enterprise.spring.io/spring-amqp/reference/amqp/listener-concurrency.html?utm_source=chatgpt.com))

并发消费可以通过配置文件统一设置。

文件位置：`src/main/resources/application-dev.yml`

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        # 初始消费者数量
        concurrency: 2

        # 最大消费者数量
        max-concurrency: 6

        # 每个消费者最多持有的未确认消息数量
        prefetch: 10

        # 手动确认模式，重要业务推荐使用 manual
        acknowledge-mode: manual

        # 消费失败后是否默认重新入队，false 可避免异常消息无限重试
        default-requeue-rejected: false

        retry:
          # 是否启用监听容器重试，具体使用需结合业务异常策略
          enabled: false
```

也可以在单个 `@RabbitListener` 上单独设置并发。

```java
@RabbitListener(
        id = "orderConcurrentListener",
        queues = RabbitMqSendConstant.ORDER_MESSAGE_QUEUE,
        containerFactory = "manualRabbitListenerContainerFactory",
        concurrency = "2-6"
)
public void consumeOrderMessage(OrderMessage orderMessage, Message message, Channel channel) {
    log.info("并发消费订单消息，orderNo={}", orderMessage.getOrderNo());
}
```

如果需要用 Java 配置控制并发，可以在监听容器工厂中设置。

```java
factory.setConcurrentConsumers(2);
factory.setMaxConcurrentConsumers(6);
factory.setPrefetchCount(10);
```

并发配置中几个参数需要重点理解。

| 参数                       | 说明                                            |
| -------------------------- | ----------------------------------------------- |
| `concurrency`              | 初始消费者数量                                  |
| `max-concurrency`          | 最大消费者数量                                  |
| `prefetch`                 | 每个消费者最多预取的未确认消息数量              |
| `acknowledge-mode`         | 消费确认模式，常见值为 `auto`、`manual`、`none` |
| `default-requeue-rejected` | 消费失败后是否默认重新入队                      |
| `concurrency = "2-6"`      | 表示最少 2 个消费者，最多 6 个消费者            |

并发消费并不是越高越好。提高并发会提升吞吐量，但也会带来数据库压力、下游接口压力、线程切换成本和消息顺序问题。Spring AMQP API 文档也提示，提高消费者数量可以扩展队列消费能力，但一旦多个消费者注册到同一队列，就会失去严格的顺序保证。([Home](https://docs.spring.io/spring-amqp/docs/3.2.5/api/org/springframework/amqp/rabbit/listener/SimpleMessageListenerContainer.html?utm_source=chatgpt.com))

并发消费建议如下。

| 场景           | 建议                                        |
| -------------- | ------------------------------------------- |
| 低流量队列     | `concurrency=1`，保持处理简单和顺序稳定     |
| 普通业务队列   | `concurrency=2`，`max-concurrency=4` 或 `6` |
| 高吞吐异步任务 | 根据数据库和下游能力逐步压测后提高          |
| 顺序敏感业务   | 尽量单消费者，或按业务主键拆分队列          |
| 慢接口消费     | 控制并发，避免把压力打到第三方服务          |
| CPU 密集型消费 | 并发不应远大于 CPU 核心数                   |
| IO 密集型消费  | 可以适当提高并发，但要配合连接池容量        |

`prefetch` 也需要谨慎设置。`prefetch` 过大时，一个消费者会提前拿到较多未确认消息，可能导致消息分配不均；`prefetch` 过小时，吞吐量可能不足。对于手动确认模式，常见初始值可以设置为 `10`，再根据消息耗时、消费速率和队列堆积情况调整。

并发消费验证方式如下。

```bash
# 连续发送多条订单消息，观察消费者并发处理日志
for i in $(seq 1 20); do
  curl -s -X POST "http://localhost:8080/api/mq/send/order" \
    -H "Content-Type: application/json" \
    -d "{
      \"orderNo\": \"ORDER_CONCURRENT_${i}\",
      \"userId\": 10001,
      \"amount\": 99.90,
      \"status\": \"CREATED\"
    }"
  echo
done
```

执行后可以观察以下内容。

| 验证项          | 说明                                       |
| --------------- | ------------------------------------------ |
| 应用日志        | 是否出现多个消费线程并发处理消息           |
| RabbitMQ 控制台 | `Ready` 是否下降，`Consumers` 是否符合预期 |
| 队列指标        | `Unacked` 是否在合理范围内                 |
| 业务系统        | 数据库、Redis、第三方接口是否出现压力异常  |
| 消费顺序        | 如果业务依赖顺序，检查是否出现乱序处理     |

消息消费开发的核心原则是：简单场景可以先使用注解消费者和自动确认，重要业务应使用手动确认、明确异常处理策略，并通过合理的并发和预取配置控制吞吐量。对于生产环境，消费异常、死信队列、幂等消费和监控告警需要配套设计，不能只依赖监听方法本身完成可靠性保障。

## 消息可靠性设计

本章节用于说明 RabbitMQ 在生产环境中常见的消息可靠性设计，包括生产者确认机制、消息返回机制、消费者确认机制、消息持久化和幂等消费设计。RabbitMQ 官方文档将生产者确认和消费者确认都归类为数据安全机制：生产者确认用于确认消息是否被 Broker 接收并处理，消费者确认用于确认消息是否被消费者成功处理。([RabbitMQ](https://www.rabbitmq.com/docs/3.13/confirms?utm_source=chatgpt.com)) 本章节对应上传大纲中的“消息可靠性设计”部分。

RabbitMQ 的可靠性不是单个配置项可以完全保证的，而是由多个环节共同完成。

```text
生产者发送
  -> Broker 接收确认
  -> Exchange 路由
  -> Queue 持久化
  -> Consumer 投递
  -> 业务处理
  -> Consumer 手动确认
  -> 幂等与补偿
```

生产环境中建议至少具备以下能力。

| 可靠性环节     | 解决的问题                               |
| -------------- | ---------------------------------------- |
| 生产者确认机制 | 确认消息是否成功到达 RabbitMQ Broker     |
| 消息返回机制   | 发现消息到达交换机但无法路由到队列的问题 |
| 消费者确认机制 | 确认消费者是否真正处理完成消息           |
| 消息持久化     | 降低 Broker 重启导致消息丢失的风险       |
| 幂等消费设计   | 解决消息重复投递、重复消费导致的数据错误 |

### 生产者确认机制

生产者确认机制，也称 Publisher Confirms，用于确认生产者发送的消息是否已经被 RabbitMQ Broker 接收并处理。RabbitMQ 文档说明，启用 confirm 模式后，Broker 会通过 `basic.ack` 或 `basic.nack` 告知生产者消息处理结果；对于已经路由到持久化队列的持久化消息，Broker 通常会在消息持久化到磁盘后发送确认。([RabbitMQ](https://www.rabbitmq.com/docs/3.13/confirms?utm_source=chatgpt.com))

生产者确认解决的是“消息有没有成功到达 Broker”的问题。它不能保证消息一定被消费者成功处理，消费者处理结果需要依赖消费者确认机制。

先在配置文件中开启生产者确认。

文件位置：`src/main/resources/application-dev.yml`

```yaml
spring:
  rabbitmq:
    # 开启生产者确认，correlated 可以通过 CorrelationData 关联每一条发送消息
    publisher-confirm-type: correlated

    # 开启消息返回机制，消息无法路由到队列时触发 returns 回调
    publisher-returns: true

    template:
      # mandatory 为 true 时，消息无法路由到队列会返回给生产者
      mandatory: true

      retry:
        # 发送失败时启用重试，用于处理短暂网络抖动
        enabled: true
        initial-interval: 1s
        max-attempts: 3
        multiplier: 2
        max-interval: 5s
```

常见确认类型如下。

| 配置值       | 说明                                       | 适用场景                 |
| ------------ | ------------------------------------------ | ------------------------ |
| `none`       | 不启用发布确认                             | 日志类、低可靠性要求消息 |
| `simple`     | 使用简单确认机制，可配合 `waitForConfirms` | 同步等待确认的少量消息   |
| `correlated` | 使用 `CorrelationData` 关联消息确认结果    | 推荐用于业务消息         |

业务系统中推荐使用 `correlated`，因为它可以将确认结果和具体业务消息关联起来，例如通过 `messageId` 找到是哪一条订单消息发送成功或失败。

下面配置 `RabbitTemplate` 的 ConfirmCallback 和 ReturnsCallback。这里使用 `RabbitTemplateConfigurer` 保留 Spring Boot 对 `RabbitTemplate` 的自动配置，再补充可靠性回调。

文件位置：`src/main/java/io/github/atengk/config/RabbitMqReliableConfig.java`

```java
package io.github.atengk.config;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.autoconfigure.RabbitTemplateConfigurer;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 可靠性配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RabbitMqReliableConfig {

    private final MessageConverter messageConverter;

    /**
     * 配置 RabbitTemplate 可靠性回调
     *
     * @param configurer        RabbitTemplate 自动配置器
     * @param connectionFactory RabbitMQ 连接工厂
     * @return RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(RabbitTemplateConfigurer configurer,
                                         ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate();
        configurer.configure(rabbitTemplate, connectionFactory);

        // 使用项目统一的 JSON 消息转换器
        rabbitTemplate.setMessageConverter(messageConverter);

        // 开启 mandatory，消息无法路由到队列时触发 returns 回调
        rabbitTemplate.setMandatory(true);

        // 生产者确认回调：确认消息是否到达 Broker
        rabbitTemplate.setConfirmCallback(this::handleConfirmCallback);

        // 消息返回回调：处理已经到达交换机但无法路由到队列的消息
        rabbitTemplate.setReturnsCallback(returned -> {
            String messageId = returned.getMessage().getMessageProperties().getMessageId();
            log.error("RabbitMQ消息路由失败，messageId={}，exchange={}，routingKey={}，replyCode={}，replyText={}",
                    messageId,
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    returned.getReplyCode(),
                    returned.getReplyText());

            // 生产环境建议在这里记录失败消息到数据库，后续由补偿任务重新发送
        });

        return rabbitTemplate;
    }

    /**
     * 处理生产者确认回调
     *
     * @param correlationData 关联数据
     * @param ack             是否确认成功
     * @param cause           失败原因
     */
    private void handleConfirmCallback(CorrelationData correlationData, boolean ack, String cause) {
        String correlationId = correlationData == null ? null : correlationData.getId();

        if (ack) {
            log.info("RabbitMQ消息发送到Broker成功，correlationId={}", correlationId);

            // 生产环境建议更新消息发送日志状态为 SEND_SUCCESS
            return;
        }

        log.error("RabbitMQ消息发送到Broker失败，correlationId={}，cause={}",
                correlationId, StrUtil.blankToDefault(cause, "未知原因"));

        // 生产环境建议更新消息发送日志状态为 SEND_FAILED，并交给补偿任务处理
    }

}
```

发送消息时，需要传入 `CorrelationData`。`CorrelationData` 的 ID 建议使用业务消息 ID，例如订单消息中的 `messageId`，这样在 ConfirmCallback 中可以定位具体消息。Spring AMQP 的 `RabbitTemplate` 支持带 `CorrelationData` 的发送方法，用于关联 publisher confirms。([Home](https://docs.spring.io/spring-amqp/docs/current/api/org/springframework/amqp/rabbit/core/RabbitTemplate.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/producer/ReliableOrderProducer.java`

```java
package io.github.atengk.producer;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.constant.RabbitMqSendConstant;
import io.github.atengk.message.OrderMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 可靠订单消息生产者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReliableOrderProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送可靠订单消息
     *
     * @param orderMessage 订单消息
     */
    public void sendReliableOrderMessage(OrderMessage orderMessage) {
        this.fillMessage(orderMessage);

        CorrelationData correlationData = new CorrelationData(orderMessage.getMessageId());

        rabbitTemplate.convertAndSend(
                RabbitMqSendConstant.ORDER_DIRECT_EXCHANGE,
                RabbitMqSendConstant.ORDER_MESSAGE_ROUTING_KEY,
                orderMessage,
                message -> {
                    message.getMessageProperties().setMessageId(orderMessage.getMessageId());
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    message.getMessageProperties().setHeader("businessType", "ORDER");
                    message.getMessageProperties().setHeader("orderNo", orderMessage.getOrderNo());
                    return message;
                },
                correlationData
        );

        log.info("可靠订单消息已提交发送，messageId={}，orderNo={}，exchange={}，routingKey={}",
                orderMessage.getMessageId(),
                orderMessage.getOrderNo(),
                RabbitMqSendConstant.ORDER_DIRECT_EXCHANGE,
                RabbitMqSendConstant.ORDER_MESSAGE_ROUTING_KEY);
    }

    /**
     * 填充消息基础字段
     *
     * @param orderMessage 订单消息
     */
    private void fillMessage(OrderMessage orderMessage) {
        if (orderMessage == null) {
            throw new IllegalArgumentException("订单消息不能为空");
        }

        if (StrUtil.isBlank(orderMessage.getMessageId())) {
            orderMessage.setMessageId(IdUtil.fastSimpleUUID());
        }

        if (StrUtil.isBlank(orderMessage.getOrderNo())) {
            orderMessage.setOrderNo("ORDER_" + IdUtil.getSnowflakeNextIdStr());
        }

        if (orderMessage.getCreateTime() == null) {
            orderMessage.setCreateTime(LocalDateTime.now());
        }
    }

}
```

生产者确认机制的处理建议如下。

| 确认结果               | 说明                        | 建议处理                               |
| ---------------------- | --------------------------- | -------------------------------------- |
| `ack=true`             | Broker 已确认接收并处理消息 | 记录发送成功                           |
| `ack=false`            | Broker 未能成功处理消息     | 记录失败，进入补偿流程                 |
| `correlationData=null` | 没有传入关联数据            | 检查发送代码是否传入 `CorrelationData` |
| `cause` 有值           | Broker 返回失败原因         | 记录日志并告警                         |
| 长时间无回调           | 可能连接异常或服务异常      | 结合发送日志和定时补偿处理             |

生产环境中，不建议只依赖日志处理发送失败。可靠业务建议建立消息发送日志表，发送前记录 `WAIT_SEND`，ConfirmCallback 成功后更新为 `SEND_SUCCESS`，失败则更新为 `SEND_FAILED`，再由定时任务扫描失败记录重发。

### 消息返回机制

消息返回机制用于处理“消息已经到达交换机，但无法路由到任何队列”的情况。例如交换机存在，但 Routing Key 写错，或者队列没有正确绑定到交换机。RabbitMQ 文档说明，对于不可路由消息，如果消息以 mandatory 方式发布，Broker 会先向客户端发送 `basic.return`，然后再发送 publisher confirm。([RabbitMQ](https://www.rabbitmq.com/docs/3.13/confirms?utm_source=chatgpt.com))

生产者确认和消息返回解决的问题不同。

| 机制       | 判断内容                         | 示例                         |
| ---------- | -------------------------------- | ---------------------------- |
| 生产者确认 | 消息是否到达 Broker              | Broker 是否接收消息          |
| 消息返回   | 消息是否从 Exchange 路由到 Queue | Routing Key 是否匹配队列绑定 |

因此，`ack=true` 不代表消息一定进入了队列。如果消息发送到了存在的交换机，但没有任何队列匹配，ConfirmCallback 仍可能收到 `ack=true`，而 ReturnsCallback 会收到不可路由消息。Spring AMQP 文档也说明，ReturnsCallback 的返回对象包含消息、replyCode、replyText、exchange 和 routingKey 等信息。([Home](https://docs.spring.io/spring-amqp/docs/2.4.x/reference/html/?utm_source=chatgpt.com))

消息返回机制需要同时满足以下条件。

| 条件                      | 说明                                           |
| ------------------------- | ---------------------------------------------- |
| `publisher-returns=true`  | 开启发布返回能力                               |
| `template.mandatory=true` | 不可路由消息返回给生产者                       |
| 配置 `ReturnsCallback`    | 在代码中处理返回消息                           |
| 发送到已存在交换机        | 如果交换机不存在，通常属于发送异常或 nack 场景 |

配置文件如下。

文件位置：`src/main/resources/application-dev.yml`

```yaml
spring:
  rabbitmq:
    # 开启消息返回能力
    publisher-returns: true

    template:
      # 不可路由消息返回给生产者
      mandatory: true
```

`RabbitTemplate` 中的返回回调已经在前面 `RabbitMqReliableConfig` 中配置。

```java
rabbitTemplate.setReturnsCallback(returned -> {
    String messageId = returned.getMessage().getMessageProperties().getMessageId();
    log.error("RabbitMQ消息路由失败，messageId={}，exchange={}，routingKey={}，replyCode={}，replyText={}",
            messageId,
            returned.getExchange(),
            returned.getRoutingKey(),
            returned.getReplyCode(),
            returned.getReplyText());

    // 生产环境建议在这里记录失败消息到数据库，后续由补偿任务重新发送
});
```

可以通过故意发送一个错误 Routing Key 验证 ReturnsCallback 是否生效。

```java
rabbitTemplate.convertAndSend(
        RabbitMqSendConstant.ORDER_DIRECT_EXCHANGE,
        "order.not.exists",
        orderMessage,
        message -> {
            message.getMessageProperties().setMessageId(orderMessage.getMessageId());
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            return message;
        },
        new CorrelationData(orderMessage.getMessageId())
);
```

如果交换机存在，但没有队列绑定 `order.not.exists`，则会触发返回回调。日志中通常可以看到 `NO_ROUTE` 相关返回原因。

消息返回机制建议如下。

| 建议             | 说明                                                         |
| ---------------- | ------------------------------------------------------------ |
| 生产环境必须开启 | 重要业务消息建议开启 `publisher-returns` 和 `mandatory`      |
| 记录完整返回信息 | 至少记录 messageId、exchange、routingKey、replyCode、replyText |
| 不要只打印日志   | 重要消息应落库并进入补偿流程                                 |
| 重点检查绑定关系 | 返回消息通常与 Routing Key 或 Binding 配置错误有关           |
| 配合启动检查     | 项目启动后可检查关键交换机、队列、绑定是否存在               |

消息返回失败常见原因如下。

| 原因             | 说明                                           |
| ---------------- | ---------------------------------------------- |
| Routing Key 写错 | 生产者发送的 Routing Key 与 Binding Key 不匹配 |
| 队列未绑定       | Queue 存在，但没有绑定到目标 Exchange          |
| 绑定到错误 vhost | 生产者连接的 vhost 和控制台查看的 vhost 不一致 |
| 配置未生效       | `mandatory` 或 `ReturnsCallback` 未正确配置    |
| 动态资源声明失败 | 交换机、队列或绑定关系启动时声明失败           |

### 消费者确认机制

消费者确认机制用于确认消费者是否已经成功处理消息。RabbitMQ 将消息投递给消费者后，需要知道这条消息什么时候可以从队列中删除。手动确认模式下，消费者成功处理后调用 `basicAck`；处理失败时可以调用 `basicNack` 或 `basicReject`。RabbitMQ 官方文档说明，手动确认可以使用 `basic.ack`、`basic.nack` 和 `basic.reject`，并且 delivery tag 只在当前 Channel 内有效，必须在接收消息的同一个 Channel 上确认。([RabbitMQ](https://www.rabbitmq.com/docs/4.0/confirms?utm_source=chatgpt.com))

重要业务推荐使用手动确认模式。

文件位置：`src/main/resources/application-dev.yml`

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        # 重要业务建议使用手动确认
        acknowledge-mode: manual

        # 每个消费者最多持有的未确认消息数量
        prefetch: 10

        # 消费失败默认不重新入队，避免异常消息无限循环
        default-requeue-rejected: false
```

也可以通过监听容器工厂配置手动确认。

文件位置：`src/main/java/io/github/atengk/config/RabbitMqManualAckConfig.java`

```java
package io.github.atengk.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 手动确认配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
@RequiredArgsConstructor
public class RabbitMqManualAckConfig {

    private final ConnectionFactory connectionFactory;

    private final MessageConverter messageConverter;

    /**
     * 手动确认监听容器工厂
     *
     * @return 手动确认监听容器工厂
     */
    @Bean(name = "reliableManualAckContainerFactory")
    public SimpleRabbitListenerContainerFactory reliableManualAckContainerFactory() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();

        // 设置 RabbitMQ 连接工厂
        factory.setConnectionFactory(connectionFactory);

        // 设置统一 JSON 消息转换器
        factory.setMessageConverter(messageConverter);

        // 使用手动确认模式
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);

        // 控制每个消费者未确认消息数量
        factory.setPrefetchCount(10);

        // 设置初始消费者数量
        factory.setConcurrentConsumers(2);

        // 设置最大消费者数量
        factory.setMaxConcurrentConsumers(6);

        // 消费异常默认不重新入队
        factory.setDefaultRequeueRejected(false);

        return factory;
    }

}
```

下面是消费者手动确认的完整示例。业务处理成功后确认消息，参数异常直接拒绝，其他异常拒绝且不重新入队，后续可以配合死信队列处理。

文件位置：`src/main/java/io/github/atengk/consumer/ReliableOrderConsumer.java`

```java
package io.github.atengk.consumer;

import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.Channel;
import io.github.atengk.constant.RabbitMqSendConstant;
import io.github.atengk.message.OrderMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 可靠订单消息消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class ReliableOrderConsumer {

    /**
     * 消费订单消息
     *
     * @param orderMessage 订单消息
     * @param message      RabbitMQ 原始消息
     * @param channel      RabbitMQ 通道
     * @throws IOException 消息确认失败时抛出
     */
    @RabbitListener(
            queues = RabbitMqSendConstant.ORDER_MESSAGE_QUEUE,
            containerFactory = "reliableManualAckContainerFactory"
    )
    public void consumeOrderMessage(OrderMessage orderMessage, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            this.validateOrderMessage(orderMessage);

            log.info("开始消费可靠订单消息，deliveryTag={}，messageId={}，orderNo={}",
                    deliveryTag, orderMessage.getMessageId(), orderMessage.getOrderNo());

            this.handleOrderBusiness(orderMessage);

            channel.basicAck(deliveryTag, false);

            log.info("可靠订单消息消费成功，deliveryTag={}，messageId={}，orderNo={}",
                    deliveryTag, orderMessage.getMessageId(), orderMessage.getOrderNo());
        } catch (IllegalArgumentException e) {
            log.warn("订单消息参数异常，拒绝且不重新入队，deliveryTag={}，message={}",
                    deliveryTag, orderMessage, e);
            channel.basicReject(deliveryTag, false);
        } catch (Exception e) {
            log.error("订单消息消费失败，拒绝且不重新入队，deliveryTag={}，message={}",
                    deliveryTag, orderMessage, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 校验订单消息
     *
     * @param orderMessage 订单消息
     */
    private void validateOrderMessage(OrderMessage orderMessage) {
        if (orderMessage == null) {
            throw new IllegalArgumentException("订单消息不能为空");
        }

        if (StrUtil.isBlank(orderMessage.getMessageId())) {
            throw new IllegalArgumentException("消息ID不能为空");
        }

        if (StrUtil.isBlank(orderMessage.getOrderNo())) {
            throw new IllegalArgumentException("订单号不能为空");
        }
    }

    /**
     * 处理订单业务
     *
     * @param orderMessage 订单消息
     */
    private void handleOrderBusiness(OrderMessage orderMessage) {
        log.info("执行订单业务处理，messageId={}，orderNo={}，status={}",
                orderMessage.getMessageId(),
                orderMessage.getOrderNo(),
                orderMessage.getStatus());

        // 示例：这里替换为真实业务，例如更新订单状态、发放积分、写入业务流水等
    }

}
```

消费者确认方式建议如下。

| 场景                   | 推荐处理                                       |
| ---------------------- | ---------------------------------------------- |
| 业务处理成功           | `basicAck(deliveryTag, false)`                 |
| 参数非法且不可恢复     | `basicReject(deliveryTag, false)`              |
| 业务异常且不应立即重试 | `basicNack(deliveryTag, false, false)`         |
| 临时异常且允许重试     | 谨慎使用 `basicNack(deliveryTag, false, true)` |
| 配合死信队列           | 推荐失败时 `requeue=false`，由死信队列兜底     |

`requeue=true` 要谨慎使用。如果异常一直存在，消息会不断重新入队、重新投递、重新失败，造成消费死循环。RabbitMQ 文档说明，手动确认配合 prefetch 可以限制消费者端未确认消息窗口，避免消费者被过多未处理消息压垮。([RabbitMQ](https://www.rabbitmq.com/docs/4.0/confirms?utm_source=chatgpt.com))

### 消息持久化

消息持久化用于降低 RabbitMQ 重启、节点故障等情况下的消息丢失风险。RabbitMQ 的持久化通常需要同时满足三个条件：交换机持久化、队列持久化、消息持久化。RabbitMQ 文档说明，如果可靠性重要，应用应使用 durable queue，并确保发布者将消息标记为 persistent；持久化队列会在节点启动时恢复，其中已发布为 persistent 的消息也会被恢复。([RabbitMQ](https://www.rabbitmq.com/docs/4.0/queues?utm_source=chatgpt.com))

持久化配置拆分如下。

| 持久化对象 | 配置方式                            | 作用                          |
| ---------- | ----------------------------------- | ----------------------------- |
| Exchange   | `durable(true)`                     | Broker 重启后交换机仍存在     |
| Queue      | `durable(...)`                      | Broker 重启后队列元数据仍存在 |
| Message    | `MessageDeliveryMode.PERSISTENT`    | 消息以持久化方式发布          |
| Confirm    | `publisher-confirm-type=correlated` | 确认 Broker 已接收并处理消息  |

RabbitMQ 官方文档还指出，持久化消息路由到 durable queue 时，Broker 的确认通常会在消息持久化到磁盘后发送；如果发布者未启用 confirms，即使使用了持久化消息，也可能无法知道消息是否在故障前真正落盘。([RabbitMQ](https://www.rabbitmq.com/docs/3.13/confirms?utm_source=chatgpt.com))

交换机和队列持久化示例。

文件位置：`src/main/java/io/github/atengk/config/RabbitMqPersistentConfig.java`

```java
package io.github.atengk.config;

import io.github.atengk.constant.RabbitMqSendConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 持久化资源配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
public class RabbitMqPersistentConfig {

    /**
     * 持久化订单交换机
     *
     * @return Direct 交换机
     */
    @Bean
    public DirectExchange persistentOrderExchange() {
        return ExchangeBuilder
                .directExchange(RabbitMqSendConstant.ORDER_DIRECT_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 持久化订单队列
     *
     * @return 订单队列
     */
    @Bean
    public Queue persistentOrderQueue() {
        return QueueBuilder
                .durable(RabbitMqSendConstant.ORDER_MESSAGE_QUEUE)
                .build();
    }

    /**
     * 订单交换机与队列绑定关系
     *
     * @param persistentOrderExchange 订单交换机
     * @param persistentOrderQueue    订单队列
     * @return 绑定关系
     */
    @Bean
    public Binding persistentOrderBinding(DirectExchange persistentOrderExchange, Queue persistentOrderQueue) {
        return BindingBuilder
                .bind(persistentOrderQueue)
                .to(persistentOrderExchange)
                .with(RabbitMqSendConstant.ORDER_MESSAGE_ROUTING_KEY);
    }

}
```

消息持久化发送示例。

```java
rabbitTemplate.convertAndSend(
        RabbitMqSendConstant.ORDER_DIRECT_EXCHANGE,
        RabbitMqSendConstant.ORDER_MESSAGE_ROUTING_KEY,
        orderMessage,
        message -> {
            message.getMessageProperties().setMessageId(orderMessage.getMessageId());

            // 将消息标记为持久化消息
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);

            return message;
        },
        new CorrelationData(orderMessage.getMessageId())
);
```

持久化验证方式如下。

```bash
# 发送一条订单消息后，停止 RabbitMQ 容器
docker stop rabbitmq

# 再次启动 RabbitMQ 容器
docker start rabbitmq
```

然后进入 RabbitMQ 管理控制台，查看对应队列是否仍存在，以及未消费的持久化消息是否仍保留。

持久化设计注意事项如下。

| 注意事项             | 说明                                                    |
| -------------------- | ------------------------------------------------------- |
| 只设置队列持久化不够 | 消息本身也要设置为 persistent                           |
| 只设置消息持久化不够 | 队列如果是 transient，重启后队列会丢失                  |
| 交换机也建议持久化   | 防止重启后拓扑结构缺失                                  |
| 配合生产者确认       | 持久化消息需要结合 confirm 才能确认 Broker 接收处理结果 |
| 持久化不是绝对不丢   | 极端故障下仍需要业务补偿和发送日志                      |
| 持久化会影响性能     | 重要消息使用持久化，低价值日志类消息可按需取舍          |

持久化是可靠性基础，但不是完整可靠性方案。生产环境还需要配合生产者确认、消费者确认、死信队列、幂等消费和补偿任务。

### 幂等消费设计

幂等消费用于解决同一条消息被重复投递、重复消费时产生的数据错误。RabbitMQ 可能因为消费者异常、手动确认失败、网络断开、Broker 重投、生产者补偿重发等原因导致消息重复出现。因此消费者必须具备“同一业务消息处理一次和处理多次结果一致”的能力。

常见重复消费原因如下。

| 原因                      | 说明                                 |
| ------------------------- | ------------------------------------ |
| 消费者处理成功但 ack 失败 | Broker 未收到确认，会重新投递消息    |
| 消费者处理过程中宕机      | 消息未确认，连接断开后会重新入队     |
| 生产者补偿重发            | 生产端无法确认发送结果时可能重新发送 |
| 网络异常                  | 确认包或发送包丢失导致两端状态不一致 |
| 手动重放死信消息          | 运维或补偿任务重新投递消息           |

幂等设计的核心是为每条业务消息建立唯一标识，并在消费前判断该标识是否已经处理过。唯一标识可以使用 `messageId`，也可以使用业务主键组合，例如 `orderNo + eventType`。

推荐幂等键设计如下。

```text
mq:idempotent:{业务类型}:{消息ID}
```

例如：

```text
mq:idempotent:order:3f6c9fbc05fc41e58e9bd63c5f4b71df
mq:idempotent:pay:PAY_202604300001
mq:idempotent:stock:ORDER_202604300001:SKU_10001
```

下面给出一个基于 Redis 的幂等服务示例。该示例使用 `StringRedisTemplate` 的 `setIfAbsent` 实现幂等占位，只有第一次写入成功的消费者可以继续处理业务。

如果项目尚未引入 Redis，需要添加依赖。

文件位置：`pom.xml`

```xml
<!-- Spring Data Redis：用于消息幂等、消费状态缓存和分布式标识记录 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

Redis 基础配置如下。

文件位置：`src/main/resources/application-dev.yml`

```yaml
spring:
  data:
    redis:
      # Redis 服务地址
      host: localhost

      # Redis 服务端口
      port: 6379

      # Redis 数据库索引
      database: 0

      # Redis 连接超时时间
      timeout: 3s
```

幂等服务完整代码如下。

文件位置：`src/main/java/io/github/atengk/service/MessageIdempotentService.java`

```java
package io.github.atengk.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 消息幂等服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageIdempotentService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String IDEMPOTENT_KEY_PREFIX = "mq:idempotent:";

    /**
     * 尝试获取消息处理权限
     *
     * @param businessType 业务类型
     * @param messageId    消息ID
     * @param ttl          幂等键过期时间
     * @return true 表示首次处理，false 表示重复消息
     */
    public boolean tryProcess(String businessType, String messageId, Duration ttl) {
        if (StrUtil.hasBlank(businessType, messageId)) {
            throw new IllegalArgumentException("业务类型和消息ID不能为空");
        }

        String key = this.buildKey(businessType, messageId);
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, "PROCESSING", ttl);

        if (Boolean.TRUE.equals(success)) {
            log.info("获取消息处理权限成功，key={}", key);
            return true;
        }

        log.warn("检测到重复消息，跳过处理，key={}", key);
        return false;
    }

    /**
     * 标记消息处理成功
     *
     * @param businessType 业务类型
     * @param messageId    消息ID
     * @param ttl          成功状态保留时间
     */
    public void markSuccess(String businessType, String messageId, Duration ttl) {
        String key = this.buildKey(businessType, messageId);
        stringRedisTemplate.opsForValue().set(key, "SUCCESS", ttl);
        log.info("消息处理状态已标记成功，key={}", key);
    }

    /**
     * 清理消息处理标识
     *
     * @param businessType 业务类型
     * @param messageId    消息ID
     */
    public void clear(String businessType, String messageId) {
        String key = this.buildKey(businessType, messageId);
        stringRedisTemplate.delete(key);
        log.info("消息处理标识已清理，key={}", key);
    }

    /**
     * 构建幂等键
     *
     * @param businessType 业务类型
     * @param messageId    消息ID
     * @return 幂等键
     */
    private String buildKey(String businessType, String messageId) {
        return IDEMPOTENT_KEY_PREFIX + businessType + ":" + messageId;
    }

}
```

下面将幂等服务集成到消费者中。消费开始前先尝试获取处理权限，如果返回 `false`，说明消息已经处理过或正在处理，此时直接 `basicAck`，避免重复业务处理。

文件位置：`src/main/java/io/github/atengk/consumer/IdempotentOrderConsumer.java`

```java
package io.github.atengk.consumer;

import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.Channel;
import io.github.atengk.constant.RabbitMqSendConstant;
import io.github.atengk.message.OrderMessage;
import io.github.atengk.service.MessageIdempotentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

/**
 * 幂等订单消息消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotentOrderConsumer {

    private final MessageIdempotentService messageIdempotentService;

    /**
     * 幂等消费订单消息
     *
     * @param orderMessage 订单消息
     * @param message      RabbitMQ 原始消息
     * @param channel      RabbitMQ 通道
     * @throws IOException 消息确认失败时抛出
     */
    @RabbitListener(
            queues = RabbitMqSendConstant.ORDER_MESSAGE_QUEUE,
            containerFactory = "reliableManualAckContainerFactory"
    )
    public void consumeOrderMessage(OrderMessage orderMessage, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            this.validateMessage(orderMessage);

            boolean firstProcess = messageIdempotentService.tryProcess(
                    "order",
                    orderMessage.getMessageId(),
                    Duration.ofHours(2)
            );

            if (!firstProcess) {
                channel.basicAck(deliveryTag, false);
                log.info("订单消息重复投递，已直接确认，deliveryTag={}，messageId={}，orderNo={}",
                        deliveryTag, orderMessage.getMessageId(), orderMessage.getOrderNo());
                return;
            }

            log.info("开始幂等消费订单消息，deliveryTag={}，messageId={}，orderNo={}",
                    deliveryTag, orderMessage.getMessageId(), orderMessage.getOrderNo());

            this.handleOrderBusiness(orderMessage);

            messageIdempotentService.markSuccess(
                    "order",
                    orderMessage.getMessageId(),
                    Duration.ofDays(7)
            );

            channel.basicAck(deliveryTag, false);

            log.info("订单消息幂等消费成功，deliveryTag={}，messageId={}，orderNo={}",
                    deliveryTag, orderMessage.getMessageId(), orderMessage.getOrderNo());
        } catch (IllegalArgumentException e) {
            log.warn("订单消息参数异常，拒绝且不重新入队，deliveryTag={}，message={}",
                    deliveryTag, orderMessage, e);
            channel.basicReject(deliveryTag, false);
        } catch (Exception e) {
            if (orderMessage != null && StrUtil.isNotBlank(orderMessage.getMessageId())) {
                messageIdempotentService.clear("order", orderMessage.getMessageId());
            }

            log.error("订单消息幂等消费失败，拒绝且不重新入队，deliveryTag={}，message={}",
                    deliveryTag, orderMessage, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 校验消息
     *
     * @param orderMessage 订单消息
     */
    private void validateMessage(OrderMessage orderMessage) {
        if (orderMessage == null) {
            throw new IllegalArgumentException("订单消息不能为空");
        }

        if (StrUtil.isBlank(orderMessage.getMessageId())) {
            throw new IllegalArgumentException("消息ID不能为空");
        }

        if (StrUtil.isBlank(orderMessage.getOrderNo())) {
            throw new IllegalArgumentException("订单号不能为空");
        }
    }

    /**
     * 处理订单业务
     *
     * @param orderMessage 订单消息
     */
    private void handleOrderBusiness(OrderMessage orderMessage) {
        log.info("执行订单幂等业务处理，messageId={}，orderNo={}，status={}",
                orderMessage.getMessageId(),
                orderMessage.getOrderNo(),
                orderMessage.getStatus());

        // 示例：这里替换为真实业务逻辑
        // 推荐在数据库层增加唯一索引，例如 order_no + event_type，进一步保证幂等
    }

}
```

Redis 幂等适合多数业务场景，但对于支付、库存、账务等强一致场景，还建议在数据库层增加唯一约束，避免 Redis 数据过期、误删或极端并发情况下出现重复写入。

数据库幂等设计示例。

```sql
CREATE TABLE mq_consume_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息ID',
    business_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    business_key VARCHAR(128) NOT NULL COMMENT '业务主键',
    consume_status VARCHAR(32) NOT NULL COMMENT '消费状态',
    error_message VARCHAR(1000) NULL COMMENT '异常信息',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_business_key (business_type, business_key)
) COMMENT='MQ消费日志表';
```

幂等消费方案选择建议如下。

| 方案                | 优点             | 缺点                     | 适用场景             |
| ------------------- | ---------------- | ------------------------ | -------------------- |
| Redis `setIfAbsent` | 性能高，实现简单 | 依赖过期时间和缓存可靠性 | 通知、同步、异步任务 |
| 数据库唯一索引      | 强一致，可靠性高 | 性能低于 Redis           | 支付、库存、账务     |
| 业务状态判断        | 不需要额外表     | 依赖业务模型清晰         | 订单状态流转         |
| 消费日志表          | 可审计、可补偿   | 实现成本较高             | 重要业务消息         |
| Redis + 数据库      | 性能和可靠性兼顾 | 实现复杂                 | 高并发重要业务       |

幂等消费设计建议如下。

| 建议                     | 说明                                   |
| ------------------------ | -------------------------------------- |
| 每条消息必须有唯一 ID    | 推荐由生产者生成 `messageId`           |
| 优先使用业务唯一键       | 例如 `orderNo + eventType`             |
| 业务处理和幂等记录要一致 | 强一致业务建议使用数据库事务           |
| 重复消息直接 ack         | 已处理消息不应再次进入业务逻辑         |
| 失败后释放处理中标识     | 避免异常导致消息永久处于 PROCESSING    |
| 保留消费日志             | 重要消息建议保留消费状态和异常信息     |
| 配合死信和补偿           | 幂等只解决重复问题，不解决失败兜底问题 |

消息可靠性设计可以总结为以下组合。

```text
生产者确认机制
  + 消息返回机制
  + Exchange / Queue / Message 持久化
  + 消费者手动确认
  + 幂等消费
  + 死信队列
  + 补偿任务
  + 监控告警
```

在生产环境中，不应假设 MQ 只会投递一次，也不应假设消息发送成功就代表业务处理成功。正确的设计思路是：允许消息重复，允许短暂失败，通过确认、持久化、幂等、死信和补偿机制保证最终结果可控。

## 死信队列与重试机制

本章节用于说明 RabbitMQ 中死信队列和消费失败重试机制的设计方式。死信队列不是一种特殊队列，本质上仍然是普通队列；区别在于业务队列中的消息因为拒绝、过期、队列长度超限等原因被重新发布到 Dead Letter Exchange，再由死信交换机路由到对应的死信队列。RabbitMQ 官方文档说明，消息在 `basic.reject` 或 `basic.nack` 且 `requeue=false`、消息 TTL 过期、队列长度超限、Quorum Queue 超过投递限制等情况下会发生死信转发。([RabbitMQ](https://www.rabbitmq.com/docs/next/dlx?utm_source=chatgpt.com)) 本章节对应上传大纲中的“死信队列与重试机制”部分。

死信队列和重试机制通常一起使用。消费失败后，消息先进入重试队列；重试队列通过 TTL 延迟一段时间后再把消息投递回业务队列；如果重试次数耗尽，则进入兜底死信队列，等待人工处理、补偿任务处理或告警通知。

推荐处理链路如下。

```text
业务交换机
  -> 业务队列
  -> 消费失败 basicNack(requeue=false)
  -> 重试交换机
  -> 重试队列
  -> TTL 到期
  -> 回到业务交换机
  -> 业务队列重新消费
  -> 超过最大重试次数
  -> 兜底交换机
  -> 兜底死信队列
```

### 死信队列使用场景

死信队列用于承接业务队列中无法正常消费的异常消息，避免异常消息阻塞正常队列，也避免消费者无限重试同一条错误消息。

常见死信场景如下。

| 场景             | 说明                                                         | 推荐处理           |
| ---------------- | ------------------------------------------------------------ | ------------------ |
| 消费者拒绝消息   | 消费者调用 `basicReject(false)` 或 `basicNack(false, false)` | 进入死信或重试队列 |
| 消息 TTL 过期    | 消息在队列中超过 TTL 时间仍未被消费                          | 进入死信交换机     |
| 队列长度超限     | 队列达到最大长度，旧消息被丢弃或转发                         | 进入死信队列       |
| 重试次数耗尽     | 多次消费失败后不再重试                                       | 进入兜底队列       |
| 消息格式错误     | JSON 反序列化失败、字段缺失、类型错误                        | 直接进入兜底队列   |
| 业务不可恢复异常 | 订单不存在、状态非法、参数永久错误                           | 直接进入兜底队列   |
| 第三方服务异常   | 网络抖动、接口超时、服务不可用                               | 先重试，耗尽后兜底 |

死信队列主要解决两个问题：第一，避免异常消息反复进入业务队列导致正常消息被阻塞；第二，保存异常现场，便于后续排查、补偿和人工处理。

死信队列中的消息通常需要保留以下信息。

| 信息               | 说明                                   |
| ------------------ | -------------------------------------- |
| `messageId`        | 消息唯一标识                           |
| `businessType`     | 业务类型，例如 `ORDER`、`PAY`、`STOCK` |
| `businessKey`      | 业务主键，例如订单号、支付单号         |
| `exchange`         | 原始交换机                             |
| `routingKey`       | 原始 Routing Key                       |
| `x-death`          | RabbitMQ 记录的死信历史                |
| `exceptionMessage` | 消费失败原因                           |
| `createTime`       | 消息创建时间                           |
| `deadTime`         | 进入兜底队列时间                       |

实际业务中，死信队列不应该长期无人处理。生产环境建议配合告警、补偿任务和死信消息管理页面，定期处理兜底队列中的消息。

### 死信交换机配置

死信交换机配置用于声明业务队列、重试队列和兜底死信队列之间的路由关系。RabbitMQ 中队列可以通过 `x-dead-letter-exchange` 指定死信交换机，通过 `x-dead-letter-routing-key` 指定死信转发时使用的 Routing Key。RabbitMQ 官方文档说明，如果队列设置了 `x-dead-letter-routing-key`，死信消息会使用该 Routing Key 重新发布到死信交换机；否则使用消息原始 Routing Key。([RabbitMQ](https://www.rabbitmq.com/docs/next/dlx?utm_source=chatgpt.com))

本节采用“业务队列 + 重试队列 + 兜底队列”的结构。

```text
order.business.exchange
  -> order.business.queue

order.retry.exchange
  -> order.retry.queue
  -> TTL 到期后回到 order.business.exchange

order.fallback.exchange
  -> order.fallback.queue
```

先定义死信队列和重试机制相关常量。

文件位置：`src/main/java/io/github/atengk/constant/RabbitMqDeadLetterConstant.java`

```java
package io.github.atengk.constant;

/**
 * RabbitMQ 死信与重试常量
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class RabbitMqDeadLetterConstant {

    /**
     * 订单业务交换机
     */
    public static final String ORDER_BUSINESS_EXCHANGE = "order.business.exchange";

    /**
     * 订单业务队列
     */
    public static final String ORDER_BUSINESS_QUEUE = "order.business.queue";

    /**
     * 订单业务 Routing Key
     */
    public static final String ORDER_BUSINESS_ROUTING_KEY = "order.business";

    /**
     * 订单重试交换机
     */
    public static final String ORDER_RETRY_EXCHANGE = "order.retry.exchange";

    /**
     * 订单重试队列
     */
    public static final String ORDER_RETRY_QUEUE = "order.retry.queue";

    /**
     * 订单重试 Routing Key
     */
    public static final String ORDER_RETRY_ROUTING_KEY = "order.retry";

    /**
     * 订单兜底交换机
     */
    public static final String ORDER_FALLBACK_EXCHANGE = "order.fallback.exchange";

    /**
     * 订单兜底死信队列
     */
    public static final String ORDER_FALLBACK_QUEUE = "order.fallback.queue";

    /**
     * 订单兜底 Routing Key
     */
    public static final String ORDER_FALLBACK_ROUTING_KEY = "order.fallback";

    /**
     * 最大重试次数
     */
    public static final long MAX_RETRY_COUNT = 3L;

    /**
     * 重试队列 TTL，单位毫秒
     */
    public static final int RETRY_TTL_MILLIS = 10_000;

    private RabbitMqDeadLetterConstant() {
    }

}
```

下面配置业务交换机、业务队列、重试交换机、重试队列、兜底交换机和兜底队列。

文件位置：`src/main/java/io/github/atengk/config/RabbitMqDeadLetterConfig.java`

```java
package io.github.atengk.config;

import io.github.atengk.constant.RabbitMqDeadLetterConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 死信与重试配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
public class RabbitMqDeadLetterConfig {

    /**
     * 订单业务交换机
     *
     * @return 业务交换机
     */
    @Bean
    public DirectExchange orderBusinessExchange() {
        return ExchangeBuilder
                .directExchange(RabbitMqDeadLetterConstant.ORDER_BUSINESS_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 订单业务队列
     *
     * @return 业务队列
     */
    @Bean
    public Queue orderBusinessQueue() {
        return QueueBuilder
                .durable(RabbitMqDeadLetterConstant.ORDER_BUSINESS_QUEUE)
                // 业务消费失败后进入重试交换机
                .deadLetterExchange(RabbitMqDeadLetterConstant.ORDER_RETRY_EXCHANGE)
                // 指定进入重试队列的 Routing Key
                .deadLetterRoutingKey(RabbitMqDeadLetterConstant.ORDER_RETRY_ROUTING_KEY)
                .build();
    }

    /**
     * 订单业务绑定关系
     *
     * @param orderBusinessExchange 业务交换机
     * @param orderBusinessQueue    业务队列
     * @return 绑定关系
     */
    @Bean
    public Binding orderBusinessBinding(DirectExchange orderBusinessExchange, Queue orderBusinessQueue) {
        return BindingBuilder
                .bind(orderBusinessQueue)
                .to(orderBusinessExchange)
                .with(RabbitMqDeadLetterConstant.ORDER_BUSINESS_ROUTING_KEY);
    }

    /**
     * 订单重试交换机
     *
     * @return 重试交换机
     */
    @Bean
    public DirectExchange orderRetryExchange() {
        return ExchangeBuilder
                .directExchange(RabbitMqDeadLetterConstant.ORDER_RETRY_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 订单重试队列
     *
     * @return 重试队列
     */
    @Bean
    public Queue orderRetryQueue() {
        return QueueBuilder
                .durable(RabbitMqDeadLetterConstant.ORDER_RETRY_QUEUE)
                // 消息在重试队列中等待指定时间
                .ttl(RabbitMqDeadLetterConstant.RETRY_TTL_MILLIS)
                // TTL 到期后重新回到业务交换机
                .deadLetterExchange(RabbitMqDeadLetterConstant.ORDER_BUSINESS_EXCHANGE)
                // 指定回到业务队列的 Routing Key
                .deadLetterRoutingKey(RabbitMqDeadLetterConstant.ORDER_BUSINESS_ROUTING_KEY)
                .build();
    }

    /**
     * 订单重试绑定关系
     *
     * @param orderRetryExchange 重试交换机
     * @param orderRetryQueue    重试队列
     * @return 绑定关系
     */
    @Bean
    public Binding orderRetryBinding(DirectExchange orderRetryExchange, Queue orderRetryQueue) {
        return BindingBuilder
                .bind(orderRetryQueue)
                .to(orderRetryExchange)
                .with(RabbitMqDeadLetterConstant.ORDER_RETRY_ROUTING_KEY);
    }

    /**
     * 订单兜底交换机
     *
     * @return 兜底交换机
     */
    @Bean
    public DirectExchange orderFallbackExchange() {
        return ExchangeBuilder
                .directExchange(RabbitMqDeadLetterConstant.ORDER_FALLBACK_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 订单兜底死信队列
     *
     * @return 兜底死信队列
     */
    @Bean
    public Queue orderFallbackQueue() {
        return QueueBuilder
                .durable(RabbitMqDeadLetterConstant.ORDER_FALLBACK_QUEUE)
                .build();
    }

    /**
     * 订单兜底绑定关系
     *
     * @param orderFallbackExchange 兜底交换机
     * @param orderFallbackQueue    兜底队列
     * @return 绑定关系
     */
    @Bean
    public Binding orderFallbackBinding(DirectExchange orderFallbackExchange, Queue orderFallbackQueue) {
        return BindingBuilder
                .bind(orderFallbackQueue)
                .to(orderFallbackExchange)
                .with(RabbitMqDeadLetterConstant.ORDER_FALLBACK_ROUTING_KEY);
    }

}
```

该配置的核心逻辑如下。

| 队列                   | 死信交换机                | 死信 Routing Key | 作用                       |
| ---------------------- | ------------------------- | ---------------- | -------------------------- |
| `order.business.queue` | `order.retry.exchange`    | `order.retry`    | 业务消费失败后进入重试队列 |
| `order.retry.queue`    | `order.business.exchange` | `order.business` | TTL 到期后回到业务队列     |
| `order.fallback.queue` | 无                        | 无               | 保存最终无法处理的消息     |

RabbitMQ 的 TTL 表示消息在队列中可以保留的时间，队列级消息 TTL 可以通过 `x-message-ttl` 或策略设置，单位为毫秒；当消息超过 TTL 后会过期，过期消息可以继续通过死信交换机转发。([RabbitMQ](https://www.rabbitmq.com/docs/4.0/ttl?utm_source=chatgpt.com)) 上述配置中的 `.ttl(10_000)` 表示消息进入重试队列后等待 10 秒，再被死信转发回业务队列进行下一次消费。

需要注意，队列一旦创建，队列参数不能随意变更。例如已经存在的 `order.business.queue` 如果原来没有死信参数，后续代码改成带死信参数再次声明，RabbitMQ 可能会因为队列参数不一致而报错。开发环境可以删除队列后重新启动；生产环境应通过新队列迁移、维护窗口或策略方式谨慎调整。

### 消费失败重试

消费失败重试用于处理临时性异常，例如第三方接口超时、数据库短暂不可用、网络抖动等。重试机制不应处理明显不可恢复的异常，例如消息格式错误、业务主键为空、订单不存在等，这类异常应直接进入兜底处理。

本节采用 Broker 侧 TTL 重试方案，核心流程如下。

```text
消费者处理失败
  -> basicNack(requeue=false)
  -> 消息进入 order.retry.exchange
  -> 消息路由到 order.retry.queue
  -> 等待 TTL
  -> TTL 到期进入 order.business.exchange
  -> 消息重新回到 order.business.queue
  -> 消费者再次消费
```

生产者发送消息到业务队列。

文件位置：`src/main/java/io/github/atengk/producer/DeadLetterOrderProducer.java`

```java
package io.github.atengk.producer;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.constant.RabbitMqDeadLetterConstant;
import io.github.atengk.message.OrderMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 死信重试订单生产者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterOrderProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送订单业务消息
     *
     * @param orderMessage 订单消息
     */
    public void sendOrderBusinessMessage(OrderMessage orderMessage) {
        this.fillMessage(orderMessage);

        rabbitTemplate.convertAndSend(
                RabbitMqDeadLetterConstant.ORDER_BUSINESS_EXCHANGE,
                RabbitMqDeadLetterConstant.ORDER_BUSINESS_ROUTING_KEY,
                orderMessage,
                message -> {
                    message.getMessageProperties().setMessageId(orderMessage.getMessageId());
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    message.getMessageProperties().setHeader("businessType", "ORDER");
                    message.getMessageProperties().setHeader("orderNo", orderMessage.getOrderNo());
                    return message;
                },
                new CorrelationData(orderMessage.getMessageId())
        );

        log.info("订单业务消息发送完成，messageId={}，orderNo={}",
                orderMessage.getMessageId(), orderMessage.getOrderNo());
    }

    /**
     * 填充消息基础字段
     *
     * @param orderMessage 订单消息
     */
    private void fillMessage(OrderMessage orderMessage) {
        if (orderMessage == null) {
            throw new IllegalArgumentException("订单消息不能为空");
        }

        if (StrUtil.isBlank(orderMessage.getMessageId())) {
            orderMessage.setMessageId(IdUtil.fastSimpleUUID());
        }

        if (StrUtil.isBlank(orderMessage.getOrderNo())) {
            orderMessage.setOrderNo("ORDER_" + IdUtil.getSnowflakeNextIdStr());
        }

        if (orderMessage.getCreateTime() == null) {
            orderMessage.setCreateTime(LocalDateTime.now());
        }
    }

}
```

消费者需要判断当前消息已经重试了多少次。RabbitMQ 会在死信消息中维护 `x-death` 头信息，用于记录死信发生历史。Spring AMQP 文档也提到 Broker 侧重试可以通过 DLX 重新路由，并由 `x-death` Header 控制重试行为。([Home](https://docs.spring.io/spring-amqp/reference/amqp/resilience-recovering-from-errors-and-broker-failures.html?utm_source=chatgpt.com))

下面是带最大重试次数判断的业务消费者。没有超过最大次数时，失败消息进入重试队列；超过最大次数后，消费者将消息发送到兜底交换机，并确认原消息，避免继续循环重试。

文件位置：`src/main/java/io/github/atengk/consumer/DeadLetterOrderConsumer.java`

```java
package io.github.atengk.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.Channel;
import io.github.atengk.constant.RabbitMqDeadLetterConstant;
import io.github.atengk.message.OrderMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 死信重试订单消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterOrderConsumer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 消费订单业务消息
     *
     * @param orderMessage 订单消息
     * @param message      RabbitMQ 原始消息
     * @param channel      RabbitMQ 通道
     * @throws IOException 消息确认失败时抛出
     */
    @RabbitListener(
            queues = RabbitMqDeadLetterConstant.ORDER_BUSINESS_QUEUE,
            containerFactory = "reliableManualAckContainerFactory"
    )
    public void consumeOrderBusinessMessage(OrderMessage orderMessage, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        long retryCount = this.getRetryCount(message);

        try {
            this.validateMessage(orderMessage);

            log.info("开始消费订单业务消息，deliveryTag={}，messageId={}，orderNo={}，retryCount={}",
                    deliveryTag, orderMessage.getMessageId(), orderMessage.getOrderNo(), retryCount);

            this.handleOrderBusiness(orderMessage);

            channel.basicAck(deliveryTag, false);

            log.info("订单业务消息消费成功，deliveryTag={}，messageId={}，orderNo={}",
                    deliveryTag, orderMessage.getMessageId(), orderMessage.getOrderNo());
        } catch (IllegalArgumentException e) {
            log.warn("订单消息参数异常，直接进入兜底队列，deliveryTag={}，message={}",
                    deliveryTag, orderMessage, e);

            this.sendToFallback(orderMessage, message, "参数异常：" + e.getMessage());

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            if (retryCount >= RabbitMqDeadLetterConstant.MAX_RETRY_COUNT) {
                log.error("订单消息重试次数耗尽，进入兜底队列，deliveryTag={}，retryCount={}，message={}",
                        deliveryTag, retryCount, orderMessage, e);

                this.sendToFallback(orderMessage, message, "重试耗尽：" + e.getMessage());

                channel.basicAck(deliveryTag, false);
                return;
            }

            log.error("订单消息消费失败，进入重试队列，deliveryTag={}，retryCount={}，message={}",
                    deliveryTag, retryCount, orderMessage, e);

            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 校验订单消息
     *
     * @param orderMessage 订单消息
     */
    private void validateMessage(OrderMessage orderMessage) {
        if (orderMessage == null) {
            throw new IllegalArgumentException("订单消息不能为空");
        }

        if (StrUtil.isBlank(orderMessage.getMessageId())) {
            throw new IllegalArgumentException("消息ID不能为空");
        }

        if (StrUtil.isBlank(orderMessage.getOrderNo())) {
            throw new IllegalArgumentException("订单号不能为空");
        }
    }

    /**
     * 处理订单业务
     *
     * @param orderMessage 订单消息
     */
    private void handleOrderBusiness(OrderMessage orderMessage) {
        log.info("执行订单业务逻辑，messageId={}，orderNo={}，status={}",
                orderMessage.getMessageId(), orderMessage.getOrderNo(), orderMessage.getStatus());

        // 示例：替换为真实业务逻辑
        // throw new RuntimeException("模拟第三方接口调用失败");
    }

    /**
     * 获取当前消息重试次数
     *
     * @param message RabbitMQ 原始消息
     * @return 重试次数
     */
    @SuppressWarnings("unchecked")
    private long getRetryCount(Message message) {
        Object xDeathHeader = message.getMessageProperties().getHeaders().get("x-death");
        if (!(xDeathHeader instanceof List<?> xDeathList) || CollUtil.isEmpty(xDeathList)) {
            return 0L;
        }

        long retryCount = 0L;
        for (Object item : xDeathList) {
            if (!(item instanceof Map<?, ?> deathInfo)) {
                continue;
            }

            Object queue = deathInfo.get("queue");
            Object count = deathInfo.get("count");

            if (RabbitMqDeadLetterConstant.ORDER_BUSINESS_QUEUE.equals(queue) && count instanceof Number number) {
                retryCount += number.longValue();
            }
        }

        return retryCount;
    }

    /**
     * 发送消息到兜底队列
     *
     * @param orderMessage  订单消息
     * @param sourceMessage 原始消息
     * @param reason        兜底原因
     */
    private void sendToFallback(OrderMessage orderMessage, Message sourceMessage, String reason) {
        rabbitTemplate.convertAndSend(
                RabbitMqDeadLetterConstant.ORDER_FALLBACK_EXCHANGE,
                RabbitMqDeadLetterConstant.ORDER_FALLBACK_ROUTING_KEY,
                orderMessage,
                message -> {
                    message.getMessageProperties().setMessageId(sourceMessage.getMessageProperties().getMessageId());
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    message.getMessageProperties().setHeader("fallbackReason", reason);
                    message.getMessageProperties().setHeader("sourceQueue", RabbitMqDeadLetterConstant.ORDER_BUSINESS_QUEUE);
                    message.getMessageProperties().setHeader("sourceExchange", RabbitMqDeadLetterConstant.ORDER_BUSINESS_EXCHANGE);
                    return message;
                }
        );

        log.warn("订单消息已发送到兜底队列，messageId={}，orderNo={}，reason={}",
                orderMessage == null ? null : orderMessage.getMessageId(),
                orderMessage == null ? null : orderMessage.getOrderNo(),
                reason);
    }

}
```

上述消费者中的关键逻辑如下。

| 逻辑           | 说明                                           |
| -------------- | ---------------------------------------------- |
| 参数异常       | 直接发送到兜底队列，然后 `basicAck` 原消息     |
| 普通业务异常   | 未达到最大重试次数时 `basicNack(false, false)` |
| 重试未耗尽     | 消息进入重试交换机和重试队列                   |
| TTL 到期       | 重试队列把消息投递回业务队列                   |
| 重试耗尽       | 手动转发到兜底交换机，然后确认原消息           |
| 兜底成功后 ack | 避免原消息继续循环重试                         |

如果使用 `basicNack(deliveryTag, false, false)`，消息不会重新进入当前队列，而是根据当前队列配置的死信交换机进行转发。RabbitMQ 官方文档明确说明，`basic.reject` 或 `basic.nack` 设置 `requeue=false` 时，消息会发生死信转发。([RabbitMQ](https://www.rabbitmq.com/docs/next/dlx?utm_source=chatgpt.com))

### 重试耗尽后的兜底处理

重试耗尽后的兜底处理用于保存最终无法自动处理的消息。兜底队列中的消息不应继续自动重试，否则会形成死循环。生产环境通常需要对兜底队列做三类处理：告警通知、落库记录、人工或定时任务补偿。

兜底处理建议链路如下。

```text
重试次数耗尽
  -> 发送到 order.fallback.exchange
  -> 路由到 order.fallback.queue
  -> 兜底消费者记录失败信息
  -> 发送告警
  -> 人工确认或补偿任务重新处理
```

兜底队列消费者可以只负责记录失败信息和告警，不再执行业务重试。

文件位置：`src/main/java/io/github/atengk/consumer/OrderFallbackConsumer.java`

```java
package io.github.atengk.consumer;

import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.Channel;
import io.github.atengk.constant.RabbitMqDeadLetterConstant;
import io.github.atengk.message.OrderMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 订单兜底死信消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class OrderFallbackConsumer {

    /**
     * 消费兜底死信消息
     *
     * @param orderMessage 订单消息
     * @param message      RabbitMQ 原始消息
     * @param channel      RabbitMQ 通道
     * @throws IOException 消息确认失败时抛出
     */
    @RabbitListener(
            queues = RabbitMqDeadLetterConstant.ORDER_FALLBACK_QUEUE,
            containerFactory = "reliableManualAckContainerFactory"
    )
    public void consumeFallbackMessage(OrderMessage orderMessage, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            String messageId = message.getMessageProperties().getMessageId();
            Object fallbackReason = message.getMessageProperties().getHeaders().get("fallbackReason");
            Object sourceQueue = message.getMessageProperties().getHeaders().get("sourceQueue");

            log.error("接收到订单兜底死信消息，deliveryTag={}，messageId={}，orderNo={}，sourceQueue={}，fallbackReason={}",
                    deliveryTag,
                    messageId,
                    orderMessage == null ? null : orderMessage.getOrderNo(),
                    sourceQueue,
                    fallbackReason);

            this.recordFallbackMessage(orderMessage, message);

            this.sendAlert(orderMessage, StrUtil.toString(fallbackReason));

            channel.basicAck(deliveryTag, false);

            log.info("订单兜底死信消息处理完成，deliveryTag={}，messageId={}", deliveryTag, messageId);
        } catch (Exception e) {
            log.error("订单兜底死信消息处理失败，deliveryTag={}，message={}", deliveryTag, orderMessage, e);

            // 兜底队列自身不建议无限重试，处理失败时可保留在队列或转入人工处理
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 记录兜底死信消息
     *
     * @param orderMessage 订单消息
     * @param message      RabbitMQ 原始消息
     */
    private void recordFallbackMessage(OrderMessage orderMessage, Message message) {
        log.warn("记录兜底死信消息，messageId={}，orderNo={}，headers={}",
                message.getMessageProperties().getMessageId(),
                orderMessage == null ? null : orderMessage.getOrderNo(),
                message.getMessageProperties().getHeaders());

        // 生产环境建议写入 mq_dead_letter_log 表
    }

    /**
     * 发送告警通知
     *
     * @param orderMessage 订单消息
     * @param reason       失败原因
     */
    private void sendAlert(OrderMessage orderMessage, String reason) {
        log.error("发送订单死信告警，orderNo={}，reason={}",
                orderMessage == null ? null : orderMessage.getOrderNo(),
                reason);

        // 生产环境可接入企业微信、钉钉、短信或告警平台
    }

}
```

生产环境建议将兜底死信消息落库，便于查询、人工处理和补偿。表结构示例如下。

```sql
CREATE TABLE mq_dead_letter_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息ID',
    business_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    business_key VARCHAR(128) NOT NULL COMMENT '业务主键',
    source_exchange VARCHAR(128) NULL COMMENT '来源交换机',
    source_queue VARCHAR(128) NULL COMMENT '来源队列',
    source_routing_key VARCHAR(128) NULL COMMENT '来源Routing Key',
    fallback_reason VARCHAR(1000) NULL COMMENT '兜底原因',
    message_body TEXT NULL COMMENT '消息内容',
    message_headers TEXT NULL COMMENT '消息头',
    handle_status VARCHAR(32) NOT NULL DEFAULT 'WAIT_HANDLE' COMMENT '处理状态',
    handle_remark VARCHAR(1000) NULL COMMENT '处理备注',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_business_key (business_type, business_key),
    KEY idx_handle_status (handle_status)
) COMMENT='MQ死信消息记录表';
```

兜底处理状态建议如下。

| 状态             | 说明         |
| ---------------- | ------------ |
| `WAIT_HANDLE`    | 等待处理     |
| `HANDLING`       | 处理中       |
| `HANDLE_SUCCESS` | 处理成功     |
| `HANDLE_FAILED`  | 处理失败     |
| `IGNORED`        | 确认无需处理 |

兜底队列中的消息处理方式可以按业务重要程度选择。

| 方式         | 说明                                 | 适用场景                   |
| ------------ | ------------------------------------ | -------------------------- |
| 人工处理     | 运维或业务人员查看失败原因后手动补偿 | 订单、支付、库存等关键业务 |
| 定时任务补偿 | 定时扫描死信记录，满足条件后重新投递 | 可自动恢复的业务           |
| 管理后台重放 | 在后台页面选择消息重新发送           | 需要人工判断的消息         |
| 告警通知     | 死信数量超过阈值时发送告警           | 所有生产业务               |
| 直接归档     | 确认无业务影响后标记忽略             | 低价值日志或通知类消息     |

如果需要提供补偿重放能力，可以通过生产者重新发送兜底消息到业务交换机。重放前必须确认幂等设计已经完成，避免重复执行业务造成数据错误。

重放逻辑示例。

```java
rabbitTemplate.convertAndSend(
        RabbitMqDeadLetterConstant.ORDER_BUSINESS_EXCHANGE,
        RabbitMqDeadLetterConstant.ORDER_BUSINESS_ROUTING_KEY,
        orderMessage,
        message -> {
            message.getMessageProperties().setMessageId(orderMessage.getMessageId());
            message.getMessageProperties().setHeader("replay", true);
            message.getMessageProperties().setHeader("replaySource", "dead-letter-admin");
            return message;
        }
);
```

重试和兜底处理建议如下。

| 建议                     | 说明                                        |
| ------------------------ | ------------------------------------------- |
| 区分可恢复和不可恢复异常 | 参数错误不应重试，临时网络异常可以重试      |
| 控制最大重试次数         | 建议 3 到 5 次，不建议无限重试              |
| 重试要有间隔             | 使用 TTL 队列或延迟交换机，避免立即反复失败 |
| 兜底队列要告警           | 死信数量增长通常代表业务异常或外部依赖异常  |
| 兜底消息要落库           | 便于查询、人工处理、补偿和审计              |
| 重放前保证幂等           | 避免重复扣库存、重复发券、重复记账          |
| 避免死信环路             | 不要让兜底队列再次死信回业务队列形成循环    |

死信队列与重试机制的核心目标不是“永远重试”，而是让异常消息有明确的流转路径。临时异常通过有限次数重试自动恢复；不可恢复异常和重试耗尽的消息进入兜底队列，交给补偿任务、人工处理和告警体系继续处理。

## 延迟队列使用

本章节用于说明 RabbitMQ 中延迟队列的常见实现方式，包括 TTL 实现延迟消息、死信队列实现延迟消费、RabbitMQ 延迟插件方案以及典型业务使用场景。RabbitMQ 本身没有直接内置“延迟队列”这一独立队列类型，常见实现通常依赖消息 TTL、死信交换机或 `rabbitmq_delayed_message_exchange` 插件。RabbitMQ 官方文档说明，消息 TTL 用于控制消息在队列中最多保留多久，过期消息可以结合死信交换机重新路由。([RabbitMQ](https://www.rabbitmq.com/docs/4.0/ttl?utm_source=chatgpt.com)) 本章节对应上传大纲中的“延迟队列使用”部分。

常见延迟方案对比如下。

| 方案                 | 核心机制                        | 优点                         | 缺点                                               | 适用场景                     |
| -------------------- | ------------------------------- | ---------------------------- | -------------------------------------------------- | ---------------------------- |
| TTL 实现延迟消息     | 给消息或队列设置过期时间        | RabbitMQ 原生支持            | 单独 TTL 只能让消息过期，通常需要配合 DLX 才能消费 | 理解延迟基础机制             |
| 死信队列实现延迟消费 | TTL + DLX                       | 不需要额外插件，稳定通用     | 不适合大量不同延迟时间的精细调度                   | 固定延迟，如 5 分钟、30 分钟 |
| 延迟插件方案         | `x-delayed-message` + `x-delay` | 支持每条消息设置不同延迟时间 | 需要安装插件，有版本和性能限制                     | 多种延迟时间、业务灵活延迟   |
| 外部调度方案         | 数据库 + 定时任务 / XXL-JOB     | 可控、可审计、适合长周期     | 实现成本更高                                       | 天级、月级、长期定时任务     |

### TTL 实现延迟消息

TTL 是 Time-To-Live 的缩写，用于表示消息或队列的存活时间。RabbitMQ 支持给队列设置消息 TTL，也支持生产者给单条消息设置 TTL。消息在队列中停留超过 TTL 后会过期；如果该队列配置了死信交换机，过期消息会被转发到死信交换机，否则会被丢弃。RabbitMQ 文档说明，消息 TTL 可以按队列设置，也可以按消息设置，过期消息不会再被投递给消费者，也无法通过 `basic.get` 直接获取。([RabbitMQ](https://www.rabbitmq.com/docs/4.0/ttl?utm_source=chatgpt.com))

TTL 本身只负责“让消息过期”，并不等于“延迟后自动消费”。要实现延迟消费，通常需要将 TTL 和死信交换机结合使用。

TTL 有两种常见配置方式。

| 类型       | 配置位置                   | 说明                           |
| ---------- | -------------------------- | ------------------------------ |
| 队列级 TTL | Queue 参数 `x-message-ttl` | 队列中所有消息使用相同过期时间 |
| 消息级 TTL | Message 属性 `expiration`  | 每条消息可以设置不同过期时间   |

队列级 TTL 示例。

```java
QueueBuilder
        .durable("order.ttl.queue")
        .ttl(30_000)
        .build();
```

消息级 TTL 示例。

```java
rabbitTemplate.convertAndSend(
        "order.ttl.exchange",
        "order.ttl",
        orderMessage,
        message -> {
            // 单条消息 30 秒后过期
            message.getMessageProperties().setExpiration("30000");
            return message;
        }
);
```

如果仅设置 TTL，但没有配置死信交换机，消息过期后会被 RabbitMQ 丢弃。这种方式不能满足订单超时关闭、延迟通知等需要“到期后处理”的业务场景。

因此，TTL 实现延迟消息时需要重点关注以下配置。

| 配置                        | 说明                             |
| --------------------------- | -------------------------------- |
| `x-message-ttl`             | 队列级消息 TTL                   |
| `expiration`                | 单条消息 TTL                     |
| `x-dead-letter-exchange`    | 消息过期后转发到的死信交换机     |
| `x-dead-letter-routing-key` | 消息过期后转发使用的 Routing Key |

TTL 延迟消息的基础流程如下。

```text
生产者发送消息
  -> 消息进入 TTL 队列
  -> 消息在 TTL 队列中等待
  -> TTL 到期
  -> 消息转发到死信交换机
  -> 消息进入业务消费队列
  -> 消费者处理消息
```

TTL 方案的主要限制是：如果业务需要大量不同的延迟时间，例如 1 分钟、3 分钟、7 分钟、15 分钟、30 分钟混合使用，单一 TTL 队列不够灵活。队列级 TTL 更适合固定延迟时间，消息级 TTL 虽然可以设置不同过期时间，但在实际使用中仍需要谨慎设计队列结构，避免消息到期和投递行为不符合业务预期。

TTL 使用建议如下。

| 建议                         | 说明                                   |
| ---------------------------- | -------------------------------------- |
| 固定延迟优先使用队列级 TTL   | 例如订单 30 分钟超时关闭               |
| 不同延迟时间使用不同延迟队列 | 例如 5 分钟、30 分钟、2 小时分别建队列 |
| 过期后必须配置 DLX           | 否则消息过期后会被丢弃                 |
| 不建议用于长期调度           | 天级、月级任务建议使用数据库和调度器   |
| 关键业务必须有补偿任务       | 例如订单超时关闭需要定时任务兜底       |

### 死信队列实现延迟消费

死信队列实现延迟消费是 RabbitMQ 中最常见的无插件延迟方案。它通过“延迟队列设置 TTL + 延迟队列配置死信交换机”的方式，让消息先进入延迟队列等待，到期后自动转发到业务队列，再由消费者处理。

该方案的核心是：生产者并不直接把消息发送到业务队列，而是发送到延迟交换机；消息进入延迟队列并等待 TTL；TTL 到期后，消息被 RabbitMQ 投递到业务交换机；最终进入业务队列被消费者消费。

延迟消费链路如下。

```text
Producer
  -> order.delay.exchange
  -> order.delay.queue
  -> TTL 到期
  -> order.business.exchange
  -> order.business.queue
  -> Consumer
```

先定义延迟队列相关常量。

文件位置：`src/main/java/io/github/atengk/constant/RabbitMqDelayConstant.java`

```java
package io.github.atengk.constant;

/**
 * RabbitMQ 延迟队列常量
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class RabbitMqDelayConstant {

    /**
     * 订单延迟交换机
     */
    public static final String ORDER_DELAY_EXCHANGE = "order.delay.exchange";

    /**
     * 订单延迟队列
     */
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";

    /**
     * 订单延迟 Routing Key
     */
    public static final String ORDER_DELAY_ROUTING_KEY = "order.delay";

    /**
     * 订单业务交换机
     */
    public static final String ORDER_BUSINESS_EXCHANGE = "order.delay.business.exchange";

    /**
     * 订单业务队列
     */
    public static final String ORDER_BUSINESS_QUEUE = "order.delay.business.queue";

    /**
     * 订单业务 Routing Key
     */
    public static final String ORDER_BUSINESS_ROUTING_KEY = "order.delay.business";

    /**
     * 订单超时关闭延迟时间，单位毫秒
     */
    public static final int ORDER_CLOSE_DELAY_MILLIS = 30 * 60 * 1000;

    private RabbitMqDelayConstant() {
    }

}
```

下面配置延迟交换机、延迟队列、业务交换机、业务队列和绑定关系。

文件位置：`src/main/java/io/github/atengk/config/RabbitMqDelayQueueConfig.java`

```java
package io.github.atengk.config;

import io.github.atengk.constant.RabbitMqDelayConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ TTL 死信延迟队列配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
public class RabbitMqDelayQueueConfig {

    /**
     * 订单延迟交换机
     *
     * @return 延迟交换机
     */
    @Bean
    public DirectExchange orderDelayExchange() {
        return ExchangeBuilder
                .directExchange(RabbitMqDelayConstant.ORDER_DELAY_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 订单延迟队列
     *
     * @return 延迟队列
     */
    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder
                .durable(RabbitMqDelayConstant.ORDER_DELAY_QUEUE)
                // 队列中消息等待 30 分钟
                .ttl(RabbitMqDelayConstant.ORDER_CLOSE_DELAY_MILLIS)
                // TTL 到期后转发到订单业务交换机
                .deadLetterExchange(RabbitMqDelayConstant.ORDER_BUSINESS_EXCHANGE)
                // TTL 到期后使用该 Routing Key 进入业务队列
                .deadLetterRoutingKey(RabbitMqDelayConstant.ORDER_BUSINESS_ROUTING_KEY)
                .build();
    }

    /**
     * 订单延迟绑定关系
     *
     * @param orderDelayExchange 延迟交换机
     * @param orderDelayQueue    延迟队列
     * @return 绑定关系
     */
    @Bean
    public Binding orderDelayBinding(DirectExchange orderDelayExchange, Queue orderDelayQueue) {
        return BindingBuilder
                .bind(orderDelayQueue)
                .to(orderDelayExchange)
                .with(RabbitMqDelayConstant.ORDER_DELAY_ROUTING_KEY);
    }

    /**
     * 订单业务交换机
     *
     * @return 业务交换机
     */
    @Bean
    public DirectExchange orderDelayBusinessExchange() {
        return ExchangeBuilder
                .directExchange(RabbitMqDelayConstant.ORDER_BUSINESS_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 订单业务队列
     *
     * @return 业务队列
     */
    @Bean
    public Queue orderDelayBusinessQueue() {
        return QueueBuilder
                .durable(RabbitMqDelayConstant.ORDER_BUSINESS_QUEUE)
                .build();
    }

    /**
     * 订单业务绑定关系
     *
     * @param orderDelayBusinessExchange 业务交换机
     * @param orderDelayBusinessQueue    业务队列
     * @return 绑定关系
     */
    @Bean
    public Binding orderDelayBusinessBinding(DirectExchange orderDelayBusinessExchange, Queue orderDelayBusinessQueue) {
        return BindingBuilder
                .bind(orderDelayBusinessQueue)
                .to(orderDelayBusinessExchange)
                .with(RabbitMqDelayConstant.ORDER_BUSINESS_ROUTING_KEY);
    }

}
```

生产者发送订单延迟消息。

文件位置：`src/main/java/io/github/atengk/producer/DelayOrderProducer.java`

```java
package io.github.atengk.producer;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.constant.RabbitMqDelayConstant;
import io.github.atengk.message.OrderMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 订单延迟消息生产者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DelayOrderProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送订单超时关闭延迟消息
     *
     * @param orderMessage 订单消息
     */
    public void sendOrderCloseDelayMessage(OrderMessage orderMessage) {
        this.fillMessage(orderMessage);

        rabbitTemplate.convertAndSend(
                RabbitMqDelayConstant.ORDER_DELAY_EXCHANGE,
                RabbitMqDelayConstant.ORDER_DELAY_ROUTING_KEY,
                orderMessage,
                message -> {
                    message.getMessageProperties().setMessageId(orderMessage.getMessageId());
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    message.getMessageProperties().setHeader("businessType", "ORDER_CLOSE_DELAY");
                    message.getMessageProperties().setHeader("orderNo", orderMessage.getOrderNo());
                    return message;
                },
                new CorrelationData(orderMessage.getMessageId())
        );

        log.info("订单超时关闭延迟消息发送成功，messageId={}，orderNo={}，delayMillis={}",
                orderMessage.getMessageId(),
                orderMessage.getOrderNo(),
                RabbitMqDelayConstant.ORDER_CLOSE_DELAY_MILLIS);
    }

    /**
     * 填充消息基础字段
     *
     * @param orderMessage 订单消息
     */
    private void fillMessage(OrderMessage orderMessage) {
        if (orderMessage == null) {
            throw new IllegalArgumentException("订单消息不能为空");
        }

        if (StrUtil.isBlank(orderMessage.getMessageId())) {
            orderMessage.setMessageId(IdUtil.fastSimpleUUID());
        }

        if (StrUtil.isBlank(orderMessage.getOrderNo())) {
            orderMessage.setOrderNo("ORDER_" + IdUtil.getSnowflakeNextIdStr());
        }

        if (orderMessage.getCreateTime() == null) {
            orderMessage.setCreateTime(LocalDateTime.now());
        }
    }

}
```

消费者监听的是业务队列，不是延迟队列。延迟队列只负责等待 TTL 到期，不应该配置业务消费者直接消费。

文件位置：`src/main/java/io/github/atengk/consumer/DelayOrderConsumer.java`

```java
package io.github.atengk.consumer;

import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.Channel;
import io.github.atengk.constant.RabbitMqDelayConstant;
import io.github.atengk.message.OrderMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 订单延迟消息消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class DelayOrderConsumer {

    /**
     * 消费订单超时关闭消息
     *
     * @param orderMessage 订单消息
     * @param message      RabbitMQ 原始消息
     * @param channel      RabbitMQ 通道
     * @throws IOException 消息确认失败时抛出
     */
    @RabbitListener(
            queues = RabbitMqDelayConstant.ORDER_BUSINESS_QUEUE,
            containerFactory = "reliableManualAckContainerFactory"
    )
    public void consumeOrderCloseDelayMessage(OrderMessage orderMessage, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            this.validateMessage(orderMessage);

            log.info("开始处理订单超时关闭消息，deliveryTag={}，messageId={}，orderNo={}",
                    deliveryTag, orderMessage.getMessageId(), orderMessage.getOrderNo());

            this.closeOrderIfUnpaid(orderMessage);

            channel.basicAck(deliveryTag, false);

            log.info("订单超时关闭消息处理完成，deliveryTag={}，messageId={}，orderNo={}",
                    deliveryTag, orderMessage.getMessageId(), orderMessage.getOrderNo());
        } catch (IllegalArgumentException e) {
            log.warn("订单超时关闭消息参数异常，拒绝且不重新入队，deliveryTag={}，message={}",
                    deliveryTag, orderMessage, e);
            channel.basicReject(deliveryTag, false);
        } catch (Exception e) {
            log.error("订单超时关闭消息处理失败，拒绝且不重新入队，deliveryTag={}，message={}",
                    deliveryTag, orderMessage, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 校验订单消息
     *
     * @param orderMessage 订单消息
     */
    private void validateMessage(OrderMessage orderMessage) {
        if (orderMessage == null) {
            throw new IllegalArgumentException("订单消息不能为空");
        }

        if (StrUtil.isBlank(orderMessage.getMessageId())) {
            throw new IllegalArgumentException("消息ID不能为空");
        }

        if (StrUtil.isBlank(orderMessage.getOrderNo())) {
            throw new IllegalArgumentException("订单号不能为空");
        }
    }

    /**
     * 未支付时关闭订单
     *
     * @param orderMessage 订单消息
     */
    private void closeOrderIfUnpaid(OrderMessage orderMessage) {
        log.info("检查订单是否需要超时关闭，orderNo={}", orderMessage.getOrderNo());

        // 示例：实际业务中应查询数据库订单状态
        // 如果订单仍为 WAIT_PAY，则关闭订单
        // 如果订单已支付、已取消或已关闭，则直接跳过，保证幂等
    }

}
```

本地测试接口可以复用前面 Controller，也可以新增一个延迟消息接口。

文件位置：`src/main/java/io/github/atengk/controller/DelayOrderController.java`

```java
package io.github.atengk.controller;

import io.github.atengk.message.OrderMessage;
import io.github.atengk.producer.DelayOrderProducer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 订单延迟消息测试接口
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequestMapping("/api/mq/delay/order")
@RequiredArgsConstructor
public class DelayOrderController {

    private final DelayOrderProducer delayOrderProducer;

    /**
     * 发送订单超时关闭延迟消息
     *
     * @param orderMessage 订单消息
     * @return 发送结果
     */
    @PostMapping("/close")
    public ResponseEntity<String> sendOrderCloseDelayMessage(@Valid @RequestBody OrderMessage orderMessage) {
        delayOrderProducer.sendOrderCloseDelayMessage(orderMessage);
        return ResponseEntity.ok("订单超时关闭延迟消息发送成功");
    }

}
```

本地验证命令如下。

```bash
curl -X POST "http://localhost:8080/api/mq/delay/order/close" \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "ORDER_DELAY_CLOSE_202604300001",
    "userId": 10001,
    "amount": 99.90,
    "status": "WAIT_PAY"
  }'
```

执行后可以在 RabbitMQ 控制台观察：

| 阶段       | 队列                         | 现象                       |
| ---------- | ---------------------------- | -------------------------- |
| 刚发送     | `order.delay.queue`          | 消息进入延迟队列           |
| TTL 未到期 | `order.delay.queue`          | 消息暂时不被业务消费者消费 |
| TTL 到期   | `order.delay.business.queue` | 消息进入业务队列           |
| 消费成功   | `order.delay.business.queue` | 消息被消费者确认并移除     |

死信队列实现延迟消费的注意事项如下。

| 注意事项                   | 说明                                   |
| -------------------------- | -------------------------------------- |
| 延迟队列不要配置业务消费者 | 否则消息会被提前消费                   |
| 业务消费者监听业务队列     | 延迟到期后的消息才进入业务队列         |
| 固定延迟适合队列级 TTL     | 一个固定延迟对应一个延迟队列           |
| 多延迟时间需要多个延迟队列 | 例如 5 分钟、30 分钟、2 小时分别建队列 |
| 队列参数不能随意改         | 已存在队列参数变更可能导致声明失败     |
| 关键业务需要兜底扫描       | 订单超时关闭不能只依赖 MQ              |

### RabbitMQ 延迟插件方案

RabbitMQ 延迟插件方案基于 `rabbitmq_delayed_message_exchange` 插件实现。启用插件后，可以声明类型为 `x-delayed-message` 的交换机，并在发送消息时通过 `x-delay` Header 指定延迟毫秒数。插件文档说明，`x-delay` 的单位是毫秒，消息会在指定延迟时间后再路由到目标队列。([GitHub](https://github.com/rabbitmq/rabbitmq-delayed-message-exchange?utm_source=chatgpt.com))

插件方案相比 TTL + DLX 更灵活，因为它支持每条消息设置不同延迟时间。比如同一个交换机可以同时发送 1 分钟、5 分钟、30 分钟后投递的消息。

使用插件前需要启用插件。

```bash
# 查看 RabbitMQ 插件目录
docker exec -it rabbitmq rabbitmq-plugins directories -s

# 启用延迟消息插件
docker exec -it rabbitmq rabbitmq-plugins enable rabbitmq_delayed_message_exchange

# 查看插件是否启用
docker exec -it rabbitmq rabbitmq-plugins list
```

如果当前 RabbitMQ 镜像没有内置该插件，需要下载与 RabbitMQ 版本匹配的 `.ez` 文件，并放入 RabbitMQ 插件目录后再启用。插件仓库说明，每个插件发布版本通常对应一个 RabbitMQ 发布系列，因此生产环境必须确认 RabbitMQ 版本、Erlang/OTP 版本和插件版本匹配。([GitHub](https://github.com/rabbitmq/rabbitmq-delayed-message-exchange?utm_source=chatgpt.com))

插件延迟交换机配置如下。

文件位置：`src/main/java/io/github/atengk/config/RabbitMqDelayedPluginConfig.java`

```java
package io.github.atengk.config;

import io.github.atengk.constant.RabbitMqPluginDelayConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * RabbitMQ 延迟插件配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
public class RabbitMqDelayedPluginConfig {

    /**
     * 插件延迟交换机
     *
     * @return 延迟交换机
     */
    @Bean
    public CustomExchange pluginDelayExchange() {
        return new CustomExchange(
                RabbitMqPluginDelayConstant.PLUGIN_DELAY_EXCHANGE,
                "x-delayed-message",
                true,
                false,
                Map.of("x-delayed-type", "direct")
        );
    }

    /**
     * 插件延迟业务队列
     *
     * @return 业务队列
     */
    @Bean
    public Queue pluginDelayOrderQueue() {
        return QueueBuilder
                .durable(RabbitMqPluginDelayConstant.PLUGIN_DELAY_ORDER_QUEUE)
                .build();
    }

    /**
     * 插件延迟绑定关系
     *
     * @param pluginDelayExchange   插件延迟交换机
     * @param pluginDelayOrderQueue 业务队列
     * @return 绑定关系
     */
    @Bean
    public Binding pluginDelayOrderBinding(CustomExchange pluginDelayExchange, Queue pluginDelayOrderQueue) {
        return BindingBuilder
                .bind(pluginDelayOrderQueue)
                .to(pluginDelayExchange)
                .with(RabbitMqPluginDelayConstant.PLUGIN_DELAY_ORDER_ROUTING_KEY)
                .noargs();
    }

}
```

插件延迟常量如下。

文件位置：`src/main/java/io/github/atengk/constant/RabbitMqPluginDelayConstant.java`

```java
package io.github.atengk.constant;

/**
 * RabbitMQ 插件延迟消息常量
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class RabbitMqPluginDelayConstant {

    /**
     * 插件延迟交换机
     */
    public static final String PLUGIN_DELAY_EXCHANGE = "plugin.delay.exchange";

    /**
     * 插件延迟订单队列
     */
    public static final String PLUGIN_DELAY_ORDER_QUEUE = "plugin.delay.order.queue";

    /**
     * 插件延迟订单 Routing Key
     */
    public static final String PLUGIN_DELAY_ORDER_ROUTING_KEY = "plugin.delay.order";

    private RabbitMqPluginDelayConstant() {
    }

}
```

插件延迟消息生产者如下。Spring AMQP 支持通过 `MessageProperties#setDelay` 设置延迟时间，也可以直接设置 `x-delay` Header；Spring AMQP 文档说明，发送延迟消息时可以通过 `MessageProperties#setDelay` 或消息后置处理器设置延迟。([Home](https://docs.spring.io/spring-amqp/reference/amqp/delayed-message-exchange.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/producer/PluginDelayOrderProducer.java`

```java
package io.github.atengk.producer;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.constant.RabbitMqPluginDelayConstant;
import io.github.atengk.message.OrderMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * RabbitMQ 插件延迟订单生产者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PluginDelayOrderProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送插件延迟订单消息
     *
     * @param orderMessage 订单消息
     * @param delayMillis  延迟毫秒数
     */
    public void sendPluginDelayOrderMessage(OrderMessage orderMessage, Integer delayMillis) {
        this.fillMessage(orderMessage);

        if (delayMillis == null || delayMillis <= 0) {
            throw new IllegalArgumentException("延迟时间必须大于0毫秒");
        }

        rabbitTemplate.convertAndSend(
                RabbitMqPluginDelayConstant.PLUGIN_DELAY_EXCHANGE,
                RabbitMqPluginDelayConstant.PLUGIN_DELAY_ORDER_ROUTING_KEY,
                orderMessage,
                message -> {
                    message.getMessageProperties().setMessageId(orderMessage.getMessageId());
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    message.getMessageProperties().setDelay(delayMillis);
                    message.getMessageProperties().setHeader("businessType", "PLUGIN_DELAY_ORDER");
                    message.getMessageProperties().setHeader("orderNo", orderMessage.getOrderNo());
                    return message;
                }
        );

        log.info("插件延迟订单消息发送成功，messageId={}，orderNo={}，delayMillis={}",
                orderMessage.getMessageId(), orderMessage.getOrderNo(), delayMillis);
    }

    /**
     * 填充消息基础字段
     *
     * @param orderMessage 订单消息
     */
    private void fillMessage(OrderMessage orderMessage) {
        if (orderMessage == null) {
            throw new IllegalArgumentException("订单消息不能为空");
        }

        if (StrUtil.isBlank(orderMessage.getMessageId())) {
            orderMessage.setMessageId(IdUtil.fastSimpleUUID());
        }

        if (StrUtil.isBlank(orderMessage.getOrderNo())) {
            orderMessage.setOrderNo("ORDER_" + IdUtil.getSnowflakeNextIdStr());
        }

        if (orderMessage.getCreateTime() == null) {
            orderMessage.setCreateTime(LocalDateTime.now());
        }
    }

}
```

插件延迟消费者直接监听目标业务队列。

文件位置：`src/main/java/io/github/atengk/consumer/PluginDelayOrderConsumer.java`

```java
package io.github.atengk.consumer;

import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.Channel;
import io.github.atengk.constant.RabbitMqPluginDelayConstant;
import io.github.atengk.message.OrderMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * RabbitMQ 插件延迟订单消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class PluginDelayOrderConsumer {

    /**
     * 消费插件延迟订单消息
     *
     * @param orderMessage 订单消息
     * @param message      RabbitMQ 原始消息
     * @param channel      RabbitMQ 通道
     * @throws IOException 消息确认失败时抛出
     */
    @RabbitListener(
            queues = RabbitMqPluginDelayConstant.PLUGIN_DELAY_ORDER_QUEUE,
            containerFactory = "reliableManualAckContainerFactory"
    )
    public void consumePluginDelayOrderMessage(OrderMessage orderMessage, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            if (orderMessage == null || StrUtil.isBlank(orderMessage.getOrderNo())) {
                throw new IllegalArgumentException("订单消息为空或订单号为空");
            }

            Integer receivedDelay = message.getMessageProperties().getReceivedDelay();

            log.info("接收到插件延迟订单消息，deliveryTag={}，messageId={}，orderNo={}，receivedDelay={}",
                    deliveryTag, orderMessage.getMessageId(), orderMessage.getOrderNo(), receivedDelay);

            // 示例：这里替换为真实业务处理逻辑

            channel.basicAck(deliveryTag, false);

            log.info("插件延迟订单消息处理完成，deliveryTag={}，messageId={}，orderNo={}",
                    deliveryTag, orderMessage.getMessageId(), orderMessage.getOrderNo());
        } catch (Exception e) {
            log.error("插件延迟订单消息处理失败，deliveryTag={}，message={}",
                    deliveryTag, orderMessage, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

}
```

测试接口如下。

文件位置：`src/main/java/io/github/atengk/controller/PluginDelayOrderController.java`

```java
package io.github.atengk.controller;

import io.github.atengk.message.OrderMessage;
import io.github.atengk.producer.PluginDelayOrderProducer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * RabbitMQ 插件延迟消息测试接口
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequestMapping("/api/mq/plugin-delay/order")
@RequiredArgsConstructor
public class PluginDelayOrderController {

    private final PluginDelayOrderProducer pluginDelayOrderProducer;

    /**
     * 发送插件延迟订单消息
     *
     * @param orderMessage 订单消息
     * @param delayMillis  延迟毫秒数
     * @return 发送结果
     */
    @PostMapping("/send")
    public ResponseEntity<String> sendPluginDelayOrderMessage(@Valid @RequestBody OrderMessage orderMessage,
                                                              @RequestParam Integer delayMillis) {
        pluginDelayOrderProducer.sendPluginDelayOrderMessage(orderMessage, delayMillis);
        return ResponseEntity.ok("插件延迟订单消息发送成功");
    }

}
```

本地验证发送 10 秒延迟消息。

```bash
curl -X POST "http://localhost:8080/api/mq/plugin-delay/order/send?delayMillis=10000" \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "ORDER_PLUGIN_DELAY_202604300001",
    "userId": 10001,
    "amount": 199.90,
    "status": "WAIT_PAY"
  }'
```

插件方案注意事项如下。

| 注意事项             | 说明                                                         |
| -------------------- | ------------------------------------------------------------ |
| 需要安装插件         | 不是所有 RabbitMQ 环境默认启用                               |
| 版本必须匹配         | 插件版本通常对应 RabbitMQ 发布系列                           |
| 延迟单位是毫秒       | `setDelay(10000)` 表示延迟 10 秒                             |
| 不适合长期调度       | 插件文档建议用于秒、分钟、小时级延迟，不适合作为长期调度方案 |
| 高数量延迟消息要谨慎 | 插件文档提示大量延迟消息可能带来性能和存储问题               |
| 不需要延迟时不要滥用 | 普通消息应使用普通 Direct、Topic 或 Fanout Exchange          |
| 关键业务仍需补偿     | 插件延迟不能替代数据库兜底和定时扫描                         |

RabbitMQ 延迟插件文档明确说明，该插件适合延迟数秒、数分钟或数小时，最多一两天；如果需要延迟数天、数周、数月或数年，应考虑使用适合长期存储的数据存储和外部调度工具。([GitHub](https://github.com/rabbitmq/rabbitmq-delayed-message-exchange?utm_source=chatgpt.com))

### 业务使用场景

延迟队列适用于“当前不立即处理，而是在指定时间后再处理”的业务场景。常见场景包括订单超时关闭、支付结果补偿、延迟通知、任务重试、优惠券过期提醒和活动状态变更。

典型业务场景如下。

| 场景           | 延迟时间       | 推荐方案             | 说明                                 |
| -------------- | -------------- | -------------------- | ------------------------------------ |
| 订单超时关闭   | 15 到 30 分钟  | TTL + DLX 或延迟插件 | 下单后未支付，到期检查订单状态并关闭 |
| 支付状态补偿   | 30 秒到 5 分钟 | 延迟插件             | 支付回调未及时到达时延迟查询支付状态 |
| 第三方接口重试 | 秒级到分钟级   | TTL + DLX 重试队列   | 接口短暂失败后延迟重试               |
| 注册后引导通知 | 5 到 30 分钟   | 延迟插件             | 用户注册后一段时间发送使用引导       |
| 优惠券过期提醒 | 小时级到天级   | 数据库 + 调度器      | 长周期任务不建议只依赖 MQ            |
| 活动开始提醒   | 小时级到天级   | 数据库 + 调度器      | 需要可查询、可取消、可调整           |
| 缓存延迟刷新   | 秒级           | 延迟插件             | 数据变更后延迟刷新缓存，降低频繁更新 |
| 自动确认收货   | 天级           | 数据库 + 调度器      | 长周期、高可靠业务应使用任务表兜底   |

以订单超时关闭为例，推荐处理流程如下。

```text
用户创建订单
  -> 订单状态为 WAIT_PAY
  -> 发送 30 分钟延迟消息
  -> 延迟消息到期
  -> 消费者查询订单状态
  -> 如果仍为 WAIT_PAY，则关闭订单
  -> 如果已支付或已取消，则直接忽略
```

订单超时关闭必须保证幂等。消费者不能直接根据 MQ 消息无条件关闭订单，而是必须查询数据库当前订单状态。只有订单仍处于待支付状态时，才允许关闭。

推荐业务判断逻辑如下。

```text
if 订单不存在:
    记录日志并确认消息

if 订单状态 = WAIT_PAY:
    关闭订单
    释放库存
    记录订单日志
    确认消息

if 订单状态 = PAID:
    跳过处理
    确认消息

if 订单状态 = CLOSED:
    跳过处理
    确认消息
```

延迟队列方案选择建议如下。

| 业务特点                 | 推荐方案                                  |
| ------------------------ | ----------------------------------------- |
| 固定延迟时间             | TTL + DLX                                 |
| 每条消息延迟时间不同     | RabbitMQ 延迟插件                         |
| 延迟时间较短             | TTL + DLX 或延迟插件                      |
| 延迟时间很长             | 数据库任务表 + 调度器                     |
| 需要取消或修改延迟任务   | 数据库任务表 + 调度器                     |
| 高可靠核心业务           | MQ 延迟 + 定时任务兜底                    |
| 大量延迟任务             | 优先评估数据库、Redis ZSet 或专业调度系统 |
| 需要精确到秒级但数量不大 | 延迟插件                                  |

生产环境中，延迟队列不应单独承担最终可靠性。对于订单超时关闭、自动取消、延迟补偿等关键业务，建议使用以下组合。

```text
延迟消息触发
  + 数据库状态校验
  + 幂等处理
  + 消费失败死信兜底
  + 定时任务补偿扫描
  + 监控告警
```

延迟队列使用建议如下。

| 建议                    | 说明                                            |
| ----------------------- | ----------------------------------------------- |
| 消费时必须查询最新状态  | MQ 消息只代表触发信号，不代表当前业务状态       |
| 业务处理必须幂等        | 防止重复消息导致重复关闭、重复通知、重复扣减    |
| 长周期任务不要只依赖 MQ | 天级以上任务建议使用数据库和调度器              |
| 延迟时间要可配置        | 不建议把 30 分钟、10 秒等时间硬编码在业务方法中 |
| 失败消息进入死信队列    | 延迟消费失败后应有兜底处理                      |
| 关键链路加监控          | 监控延迟队列、业务队列、死信队列消息堆积        |
| 保留补偿入口            | 支持人工或定时任务重新处理失败消息              |

延迟队列的核心定位是“延迟触发”，不是“长期任务调度中心”。在中短期延迟、异步补偿和状态检查场景中，RabbitMQ 延迟队列非常实用；但对于长期、可取消、可修改、强审计的任务，应优先使用数据库任务表、调度中心或专门的延迟任务系统。

## 业务开发实践

本章节用于说明 RabbitMQ 在实际业务系统中的典型落地方式，包括订单超时关闭、异步通知处理、日志异步收集和数据同步任务。前面章节已经说明了交换机、队列、消息发送、消息消费、可靠性、死信队列和延迟队列，本章节重点从业务链路角度说明如何组合使用这些能力。该章节对应上传大纲中的“业务开发实践”部分。

业务开发中使用 RabbitMQ 时，不建议只关注“消息是否发送成功”。更重要的是明确消息代表什么业务事件、消费端如何保证幂等、失败后如何重试、无法处理时如何兜底，以及是否存在定时任务或人工补偿入口。

推荐业务实践目录结构如下。

```text
src/main/java/io/github/atengk
├── constant
│   └── RabbitMqBusinessConstant.java
├── message
│   ├── OrderTimeoutMessage.java
│   ├── NotifyMessage.java
│   ├── OperationLogMessage.java
│   └── DataSyncMessage.java
├── producer
│   ├── OrderTimeoutProducer.java
│   ├── NotifyProducer.java
│   ├── OperationLogProducer.java
│   └── DataSyncProducer.java
├── consumer
│   ├── OrderTimeoutConsumer.java
│   ├── NotifyConsumer.java
│   ├── OperationLogConsumer.java
│   └── DataSyncConsumer.java
└── service
    ├── OrderTimeoutService.java
    ├── NotifyService.java
    ├── OperationLogService.java
    └── DataSyncService.java
```

### 订单超时关闭

订单超时关闭是延迟队列最常见的业务场景。用户创建订单后，如果在指定时间内未完成支付，系统需要自动关闭订单、释放库存、记录订单日志，并通知用户或运营系统。

订单超时关闭不能简单地在延迟消息到期后直接关闭订单。正确做法是：延迟消息只作为“触发信号”，消费者收到消息后必须查询数据库中的订单最新状态。只有订单仍然处于待支付状态时，才允许关闭订单；如果订单已经支付、取消或关闭，则直接确认消息并跳过处理。

订单超时关闭推荐链路如下。

```text
用户创建订单
  -> 保存订单，状态为 WAIT_PAY
  -> 发送订单超时关闭延迟消息
  -> 延迟时间到期
  -> 消费者收到消息
  -> 查询订单最新状态
  -> 如果仍为 WAIT_PAY，则关闭订单并释放库存
  -> 如果已支付、已关闭或已取消，则跳过处理
  -> 消费成功后手动 ack
```

订单超时消息实体如下。

文件位置：`src/main/java/io/github/atengk/message/OrderTimeoutMessage.java`

```java
package io.github.atengk.message;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 订单超时关闭消息
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderTimeoutMessage {

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 订单号
     */
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订单创建时间
     */
    private LocalDateTime orderCreateTime;

    /**
     * 订单过期时间
     */
    private LocalDateTime expireTime;

}
```

业务实践相关常量统一放到常量类中，避免交换机、队列和 Routing Key 分散在业务代码里。

文件位置：`src/main/java/io/github/atengk/constant/RabbitMqBusinessConstant.java`

```java
package io.github.atengk.constant;

/**
 * RabbitMQ 业务实践常量
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class RabbitMqBusinessConstant {

    /**
     * 订单延迟交换机
     */
    public static final String ORDER_TIMEOUT_DELAY_EXCHANGE = "business.order.timeout.delay.exchange";

    /**
     * 订单超时关闭队列
     */
    public static final String ORDER_TIMEOUT_QUEUE = "business.order.timeout.queue";

    /**
     * 订单超时关闭 Routing Key
     */
    public static final String ORDER_TIMEOUT_ROUTING_KEY = "business.order.timeout";

    /**
     * 通知交换机
     */
    public static final String NOTIFY_DIRECT_EXCHANGE = "business.notify.direct.exchange";

    /**
     * 短信通知队列
     */
    public static final String NOTIFY_SMS_QUEUE = "business.notify.sms.queue";

    /**
     * 邮件通知队列
     */
    public static final String NOTIFY_EMAIL_QUEUE = "business.notify.email.queue";

    /**
     * 站内信通知队列
     */
    public static final String NOTIFY_SITE_QUEUE = "business.notify.site.queue";

    /**
     * 短信通知 Routing Key
     */
    public static final String NOTIFY_SMS_ROUTING_KEY = "business.notify.sms";

    /**
     * 邮件通知 Routing Key
     */
    public static final String NOTIFY_EMAIL_ROUTING_KEY = "business.notify.email";

    /**
     * 站内信通知 Routing Key
     */
    public static final String NOTIFY_SITE_ROUTING_KEY = "business.notify.site";

    /**
     * 日志交换机
     */
    public static final String LOG_DIRECT_EXCHANGE = "business.log.direct.exchange";

    /**
     * 操作日志队列
     */
    public static final String OPERATION_LOG_QUEUE = "business.operation.log.queue";

    /**
     * 操作日志 Routing Key
     */
    public static final String OPERATION_LOG_ROUTING_KEY = "business.operation.log";

    /**
     * 数据同步 Topic 交换机
     */
    public static final String DATA_SYNC_TOPIC_EXCHANGE = "business.data.sync.topic.exchange";

    /**
     * 订单数据同步队列
     */
    public static final String DATA_SYNC_ORDER_QUEUE = "business.data.sync.order.queue";

    /**
     * 用户数据同步队列
     */
    public static final String DATA_SYNC_USER_QUEUE = "business.data.sync.user.queue";

    /**
     * 订单数据同步 Binding Key
     */
    public static final String DATA_SYNC_ORDER_BINDING_KEY = "sync.order.#";

    /**
     * 用户数据同步 Binding Key
     */
    public static final String DATA_SYNC_USER_BINDING_KEY = "sync.user.#";

    private RabbitMqBusinessConstant() {
    }

}
```

如果使用 RabbitMQ 延迟插件，可以直接声明 `x-delayed-message` 交换机，并通过 `x-delay` 控制延迟时间。

文件位置：`src/main/java/io/github/atengk/config/RabbitMqBusinessConfig.java`

```java
package io.github.atengk.config;

import io.github.atengk.constant.RabbitMqBusinessConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * RabbitMQ 业务实践配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
public class RabbitMqBusinessConfig {

    @Bean
    public CustomExchange orderTimeoutDelayExchange() {
        return new CustomExchange(
                RabbitMqBusinessConstant.ORDER_TIMEOUT_DELAY_EXCHANGE,
                "x-delayed-message",
                true,
                false,
                Map.of("x-delayed-type", "direct")
        );
    }

    @Bean
    public Queue orderTimeoutQueue() {
        return QueueBuilder
                .durable(RabbitMqBusinessConstant.ORDER_TIMEOUT_QUEUE)
                .build();
    }

    @Bean
    public Binding orderTimeoutBinding(CustomExchange orderTimeoutDelayExchange, Queue orderTimeoutQueue) {
        return BindingBuilder
                .bind(orderTimeoutQueue)
                .to(orderTimeoutDelayExchange)
                .with(RabbitMqBusinessConstant.ORDER_TIMEOUT_ROUTING_KEY)
                .noargs();
    }

    @Bean
    public DirectExchange notifyDirectExchange() {
        return ExchangeBuilder
                .directExchange(RabbitMqBusinessConstant.NOTIFY_DIRECT_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue notifySmsQueue() {
        return QueueBuilder
                .durable(RabbitMqBusinessConstant.NOTIFY_SMS_QUEUE)
                .build();
    }

    @Bean
    public Queue notifyEmailQueue() {
        return QueueBuilder
                .durable(RabbitMqBusinessConstant.NOTIFY_EMAIL_QUEUE)
                .build();
    }

    @Bean
    public Queue notifySiteQueue() {
        return QueueBuilder
                .durable(RabbitMqBusinessConstant.NOTIFY_SITE_QUEUE)
                .build();
    }

    @Bean
    public Binding notifySmsBinding(DirectExchange notifyDirectExchange, Queue notifySmsQueue) {
        return BindingBuilder
                .bind(notifySmsQueue)
                .to(notifyDirectExchange)
                .with(RabbitMqBusinessConstant.NOTIFY_SMS_ROUTING_KEY);
    }

    @Bean
    public Binding notifyEmailBinding(DirectExchange notifyDirectExchange, Queue notifyEmailQueue) {
        return BindingBuilder
                .bind(notifyEmailQueue)
                .to(notifyDirectExchange)
                .with(RabbitMqBusinessConstant.NOTIFY_EMAIL_ROUTING_KEY);
    }

    @Bean
    public Binding notifySiteBinding(DirectExchange notifyDirectExchange, Queue notifySiteQueue) {
        return BindingBuilder
                .bind(notifySiteQueue)
                .to(notifyDirectExchange)
                .with(RabbitMqBusinessConstant.NOTIFY_SITE_ROUTING_KEY);
    }

    @Bean
    public DirectExchange logDirectExchange() {
        return ExchangeBuilder
                .directExchange(RabbitMqBusinessConstant.LOG_DIRECT_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue operationLogQueue() {
        return QueueBuilder
                .durable(RabbitMqBusinessConstant.OPERATION_LOG_QUEUE)
                .build();
    }

    @Bean
    public Binding operationLogBinding(DirectExchange logDirectExchange, Queue operationLogQueue) {
        return BindingBuilder
                .bind(operationLogQueue)
                .to(logDirectExchange)
                .with(RabbitMqBusinessConstant.OPERATION_LOG_ROUTING_KEY);
    }

    @Bean
    public TopicExchange dataSyncTopicExchange() {
        return ExchangeBuilder
                .topicExchange(RabbitMqBusinessConstant.DATA_SYNC_TOPIC_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue dataSyncOrderQueue() {
        return QueueBuilder
                .durable(RabbitMqBusinessConstant.DATA_SYNC_ORDER_QUEUE)
                .build();
    }

    @Bean
    public Queue dataSyncUserQueue() {
        return QueueBuilder
                .durable(RabbitMqBusinessConstant.DATA_SYNC_USER_QUEUE)
                .build();
    }

    @Bean
    public Binding dataSyncOrderBinding(TopicExchange dataSyncTopicExchange, Queue dataSyncOrderQueue) {
        return BindingBuilder
                .bind(dataSyncOrderQueue)
                .to(dataSyncTopicExchange)
                .with(RabbitMqBusinessConstant.DATA_SYNC_ORDER_BINDING_KEY);
    }

    @Bean
    public Binding dataSyncUserBinding(TopicExchange dataSyncTopicExchange, Queue dataSyncUserQueue) {
        return BindingBuilder
                .bind(dataSyncUserQueue)
                .to(dataSyncTopicExchange)
                .with(RabbitMqBusinessConstant.DATA_SYNC_USER_BINDING_KEY);
    }

}
```

订单超时关闭生产者如下。创建订单成功后调用该组件发送延迟消息。

文件位置：`src/main/java/io/github/atengk/producer/OrderTimeoutProducer.java`

```java
package io.github.atengk.producer;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.constant.RabbitMqBusinessConstant;
import io.github.atengk.message.OrderTimeoutMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 订单超时关闭生产者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送订单超时关闭延迟消息
     *
     * @param message     订单超时关闭消息
     * @param delayMillis 延迟毫秒数
     */
    public void sendOrderTimeoutMessage(OrderTimeoutMessage message, Integer delayMillis) {
        this.fillMessage(message);

        if (delayMillis == null || delayMillis <= 0) {
            throw new IllegalArgumentException("订单超时关闭延迟时间必须大于0毫秒");
        }

        rabbitTemplate.convertAndSend(
                RabbitMqBusinessConstant.ORDER_TIMEOUT_DELAY_EXCHANGE,
                RabbitMqBusinessConstant.ORDER_TIMEOUT_ROUTING_KEY,
                message,
                rabbitMessage -> {
                    rabbitMessage.getMessageProperties().setMessageId(message.getMessageId());
                    rabbitMessage.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    rabbitMessage.getMessageProperties().setDelay(delayMillis);
                    rabbitMessage.getMessageProperties().setHeader("businessType", "ORDER_TIMEOUT_CLOSE");
                    rabbitMessage.getMessageProperties().setHeader("orderNo", message.getOrderNo());
                    return rabbitMessage;
                },
                new CorrelationData(message.getMessageId())
        );

        log.info("订单超时关闭延迟消息发送成功，messageId={}，orderNo={}，delayMillis={}",
                message.getMessageId(), message.getOrderNo(), delayMillis);
    }

    /**
     * 填充消息基础字段
     *
     * @param message 订单超时关闭消息
     */
    private void fillMessage(OrderTimeoutMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("订单超时关闭消息不能为空");
        }

        if (StrUtil.isBlank(message.getMessageId())) {
            message.setMessageId(IdUtil.fastSimpleUUID());
        }

        if (message.getOrderCreateTime() == null) {
            message.setOrderCreateTime(LocalDateTime.now());
        }
    }

}
```

订单超时关闭消费者只负责接收消息、确认消息和调用业务服务。具体关闭订单逻辑应放到 Service 中。

文件位置：`src/main/java/io/github/atengk/consumer/OrderTimeoutConsumer.java`

```java
package io.github.atengk.consumer;

import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.Channel;
import io.github.atengk.constant.RabbitMqBusinessConstant;
import io.github.atengk.message.OrderTimeoutMessage;
import io.github.atengk.service.OrderTimeoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 订单超时关闭消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutConsumer {

    private final OrderTimeoutService orderTimeoutService;

    /**
     * 消费订单超时关闭消息
     *
     * @param timeoutMessage 订单超时关闭消息
     * @param message        RabbitMQ 原始消息
     * @param channel        RabbitMQ 通道
     * @throws IOException 消息确认失败时抛出
     */
    @RabbitListener(
            queues = RabbitMqBusinessConstant.ORDER_TIMEOUT_QUEUE,
            containerFactory = "reliableManualAckContainerFactory"
    )
    public void consumeOrderTimeoutMessage(OrderTimeoutMessage timeoutMessage,
                                           Message message,
                                           Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            if (timeoutMessage == null || StrUtil.isBlank(timeoutMessage.getOrderNo())) {
                throw new IllegalArgumentException("订单超时关闭消息为空或订单号为空");
            }

            log.info("开始处理订单超时关闭消息，deliveryTag={}，messageId={}，orderNo={}",
                    deliveryTag, timeoutMessage.getMessageId(), timeoutMessage.getOrderNo());

            orderTimeoutService.closeOrderIfTimeout(timeoutMessage);

            channel.basicAck(deliveryTag, false);

            log.info("订单超时关闭消息处理完成，deliveryTag={}，messageId={}，orderNo={}",
                    deliveryTag, timeoutMessage.getMessageId(), timeoutMessage.getOrderNo());
        } catch (IllegalArgumentException e) {
            log.warn("订单超时关闭消息参数异常，拒绝且不重新入队，deliveryTag={}，message={}",
                    deliveryTag, timeoutMessage, e);
            channel.basicReject(deliveryTag, false);
        } catch (Exception e) {
            log.error("订单超时关闭消息处理失败，拒绝且不重新入队，deliveryTag={}，message={}",
                    deliveryTag, timeoutMessage, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

}
```

订单超时关闭业务服务中必须进行状态校验，避免订单已支付后又被延迟消息关闭。

文件位置：`src/main/java/io/github/atengk/service/OrderTimeoutService.java`

```java
package io.github.atengk.service;

import io.github.atengk.message.OrderTimeoutMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单超时关闭业务服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
public class OrderTimeoutService {

    /**
     * 订单待支付状态
     */
    private static final String ORDER_STATUS_WAIT_PAY = "WAIT_PAY";

    /**
     * 订单已支付状态
     */
    private static final String ORDER_STATUS_PAID = "PAID";

    /**
     * 订单已关闭状态
     */
    private static final String ORDER_STATUS_CLOSED = "CLOSED";

    /**
     * 超时关闭订单
     *
     * @param message 订单超时关闭消息
     */
    public void closeOrderIfTimeout(OrderTimeoutMessage message) {
        String orderNo = message.getOrderNo();

        String currentStatus = this.getOrderStatus(orderNo);

        log.info("查询订单当前状态，orderNo={}，status={}", orderNo, currentStatus);

        if (ORDER_STATUS_WAIT_PAY.equals(currentStatus)) {
            this.closeOrder(orderNo);
            this.releaseStock(orderNo);
            this.recordOrderLog(orderNo, "订单超时未支付，系统自动关闭");
            log.info("订单超时关闭完成，orderNo={}", orderNo);
            return;
        }

        if (ORDER_STATUS_PAID.equals(currentStatus)) {
            log.info("订单已支付，跳过超时关闭，orderNo={}", orderNo);
            return;
        }

        if (ORDER_STATUS_CLOSED.equals(currentStatus)) {
            log.info("订单已关闭，跳过重复处理，orderNo={}", orderNo);
            return;
        }

        log.warn("订单状态不满足超时关闭条件，orderNo={}，status={}", orderNo, currentStatus);
    }

    /**
     * 获取订单状态
     *
     * @param orderNo 订单号
     * @return 订单状态
     */
    private String getOrderStatus(String orderNo) {
        // 示例：实际项目中应从数据库查询订单状态
        return ORDER_STATUS_WAIT_PAY;
    }

    /**
     * 关闭订单
     *
     * @param orderNo 订单号
     */
    private void closeOrder(String orderNo) {
        // 示例：实际项目中应使用 update ... where order_no = ? and status = 'WAIT_PAY'
        log.info("执行订单关闭，orderNo={}", orderNo);
    }

    /**
     * 释放库存
     *
     * @param orderNo 订单号
     */
    private void releaseStock(String orderNo) {
        log.info("释放订单占用库存，orderNo={}", orderNo);
    }

    /**
     * 记录订单日志
     *
     * @param orderNo 订单号
     * @param content 日志内容
     */
    private void recordOrderLog(String orderNo, String content) {
        log.info("记录订单日志，orderNo={}，content={}", orderNo, content);
    }

}
```

订单超时关闭建议使用数据库条件更新保证幂等。

```sql
UPDATE t_order
SET status = 'CLOSED',
    close_time = NOW(),
    update_time = NOW()
WHERE order_no = #{orderNo}
  AND status = 'WAIT_PAY';
```

如果更新影响行数为 `1`，表示本次成功关闭订单；如果影响行数为 `0`，表示订单已经不是待支付状态，应直接跳过。

订单超时关闭开发注意事项如下。

| 注意事项               | 说明                                         |
| ---------------------- | -------------------------------------------- |
| 延迟消息只是触发信号   | 不能直接根据 MQ 消息关闭订单                 |
| 必须查询订单最新状态   | 防止订单已支付后被误关闭                     |
| 数据库更新要带状态条件 | 使用 `where status = 'WAIT_PAY'` 保证幂等    |
| 释放库存也要幂等       | 避免重复释放库存                             |
| 消费失败要进入死信     | 不能无限重试阻塞队列                         |
| 需要定时任务兜底       | 定时扫描超时未支付订单，防止 MQ 异常导致漏关 |

### 异步通知处理

异步通知处理适用于短信、邮件、站内信、Webhook、App 推送等非核心链路。业务主流程只负责产生通知消息，不同步等待通知渠道处理完成。消费者根据通知类型调用不同渠道服务。

异步通知推荐链路如下。

```text
业务事件发生
  -> 发送通知消息
  -> RabbitMQ 路由到短信、邮件或站内信队列
  -> 对应消费者处理
  -> 调用第三方渠道
  -> 成功后 ack
  -> 失败后重试或进入死信队列
```

通知消息实体如下。

文件位置：`src/main/java/io/github/atengk/message/NotifyMessage.java`

```java
package io.github.atengk.message;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通知消息
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyMessage {

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 通知类型：SMS、EMAIL、SITE
     */
    @NotBlank(message = "通知类型不能为空")
    private String notifyType;

    /**
     * 接收人
     */
    @NotBlank(message = "接收人不能为空")
    private String receiver;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    @NotBlank(message = "通知内容不能为空")
    private String content;

    /**
     * 模板编码
     */
    private String templateCode;

    /**
     * 模板参数
     */
    private Map<String, Object> templateParams;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
```

通知生产者根据通知类型选择不同 Routing Key。

文件位置：`src/main/java/io/github/atengk/producer/NotifyProducer.java`

```java
package io.github.atengk.producer;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.constant.RabbitMqBusinessConstant;
import io.github.atengk.message.NotifyMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 异步通知生产者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送通知消息
     *
     * @param notifyMessage 通知消息
     */
    public void sendNotifyMessage(NotifyMessage notifyMessage) {
        this.fillMessage(notifyMessage);

        String routingKey = this.getRoutingKey(notifyMessage.getNotifyType());

        rabbitTemplate.convertAndSend(
                RabbitMqBusinessConstant.NOTIFY_DIRECT_EXCHANGE,
                routingKey,
                notifyMessage,
                message -> {
                    message.getMessageProperties().setMessageId(notifyMessage.getMessageId());
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    message.getMessageProperties().setHeader("notifyType", notifyMessage.getNotifyType());
                    message.getMessageProperties().setHeader("receiver", notifyMessage.getReceiver());
                    return message;
                },
                new CorrelationData(notifyMessage.getMessageId())
        );

        log.info("异步通知消息发送成功，messageId={}，notifyType={}，receiver={}",
                notifyMessage.getMessageId(), notifyMessage.getNotifyType(), notifyMessage.getReceiver());
    }

    /**
     * 获取通知 Routing Key
     *
     * @param notifyType 通知类型
     * @return Routing Key
     */
    private String getRoutingKey(String notifyType) {
        if (StrUtil.equalsIgnoreCase("SMS", notifyType)) {
            return RabbitMqBusinessConstant.NOTIFY_SMS_ROUTING_KEY;
        }

        if (StrUtil.equalsIgnoreCase("EMAIL", notifyType)) {
            return RabbitMqBusinessConstant.NOTIFY_EMAIL_ROUTING_KEY;
        }

        if (StrUtil.equalsIgnoreCase("SITE", notifyType)) {
            return RabbitMqBusinessConstant.NOTIFY_SITE_ROUTING_KEY;
        }

        throw new IllegalArgumentException("不支持的通知类型：" + notifyType);
    }

    /**
     * 填充消息基础字段
     *
     * @param notifyMessage 通知消息
     */
    private void fillMessage(NotifyMessage notifyMessage) {
        if (notifyMessage == null) {
            throw new IllegalArgumentException("通知消息不能为空");
        }

        if (StrUtil.isBlank(notifyMessage.getMessageId())) {
            notifyMessage.setMessageId(IdUtil.fastSimpleUUID());
        }

        if (notifyMessage.getCreateTime() == null) {
            notifyMessage.setCreateTime(LocalDateTime.now());
        }
    }

}
```

通知业务服务负责调用具体渠道。下面代码用日志模拟第三方调用，实际项目中应替换为短信服务、邮件服务或站内信入库逻辑。

文件位置：`src/main/java/io/github/atengk/service/NotifyService.java`

```java
package io.github.atengk.service;

import io.github.atengk.message.NotifyMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 异步通知业务服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
public class NotifyService {

    /**
     * 发送短信通知
     *
     * @param message 通知消息
     */
    public void sendSms(NotifyMessage message) {
        log.info("发送短信通知，receiver={}，content={}", message.getReceiver(), message.getContent());

        // 示例：实际项目中调用短信服务商 API
    }

    /**
     * 发送邮件通知
     *
     * @param message 通知消息
     */
    public void sendEmail(NotifyMessage message) {
        log.info("发送邮件通知，receiver={}，title={}，content={}",
                message.getReceiver(), message.getTitle(), message.getContent());

        // 示例：实际项目中调用邮件服务
    }

    /**
     * 发送站内信通知
     *
     * @param message 通知消息
     */
    public void sendSiteMessage(NotifyMessage message) {
        log.info("写入站内信通知，receiver={}，title={}，content={}",
                message.getReceiver(), message.getTitle(), message.getContent());

        // 示例：实际项目中写入站内信表
    }

}
```

通知消费者可以按渠道拆分，避免一个消费者处理所有通知类型导致逻辑臃肿。

文件位置：`src/main/java/io/github/atengk/consumer/NotifyConsumer.java`

```java
package io.github.atengk.consumer;

import com.rabbitmq.client.Channel;
import io.github.atengk.constant.RabbitMqBusinessConstant;
import io.github.atengk.message.NotifyMessage;
import io.github.atengk.service.NotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 异步通知消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyConsumer {

    private final NotifyService notifyService;

    /**
     * 消费短信通知
     *
     * @param notifyMessage 通知消息
     * @param message       RabbitMQ 原始消息
     * @param channel       RabbitMQ 通道
     * @throws IOException 消息确认失败时抛出
     */
    @RabbitListener(
            queues = RabbitMqBusinessConstant.NOTIFY_SMS_QUEUE,
            containerFactory = "reliableManualAckContainerFactory"
    )
    public void consumeSmsNotify(NotifyMessage notifyMessage, Message message, Channel channel) throws IOException {
        this.handleNotify(message, channel, notifyMessage, "SMS");
    }

    /**
     * 消费邮件通知
     *
     * @param notifyMessage 通知消息
     * @param message       RabbitMQ 原始消息
     * @param channel       RabbitMQ 通道
     * @throws IOException 消息确认失败时抛出
     */
    @RabbitListener(
            queues = RabbitMqBusinessConstant.NOTIFY_EMAIL_QUEUE,
            containerFactory = "reliableManualAckContainerFactory"
    )
    public void consumeEmailNotify(NotifyMessage notifyMessage, Message message, Channel channel) throws IOException {
        this.handleNotify(message, channel, notifyMessage, "EMAIL");
    }

    /**
     * 消费站内信通知
     *
     * @param notifyMessage 通知消息
     * @param message       RabbitMQ 原始消息
     * @param channel       RabbitMQ 通道
     * @throws IOException 消息确认失败时抛出
     */
    @RabbitListener(
            queues = RabbitMqBusinessConstant.NOTIFY_SITE_QUEUE,
            containerFactory = "reliableManualAckContainerFactory"
    )
    public void consumeSiteNotify(NotifyMessage notifyMessage, Message message, Channel channel) throws IOException {
        this.handleNotify(message, channel, notifyMessage, "SITE");
    }

    /**
     * 处理通知消息
     *
     * @param message       RabbitMQ 原始消息
     * @param channel       RabbitMQ 通道
     * @param notifyMessage 通知消息
     * @param notifyType    通知类型
     * @throws IOException 消息确认失败时抛出
     */
    private void handleNotify(Message message,
                              Channel channel,
                              NotifyMessage notifyMessage,
                              String notifyType) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            log.info("开始处理异步通知，deliveryTag={}，messageId={}，notifyType={}，receiver={}",
                    deliveryTag, notifyMessage.getMessageId(), notifyType, notifyMessage.getReceiver());

            if ("SMS".equals(notifyType)) {
                notifyService.sendSms(notifyMessage);
            } else if ("EMAIL".equals(notifyType)) {
                notifyService.sendEmail(notifyMessage);
            } else if ("SITE".equals(notifyType)) {
                notifyService.sendSiteMessage(notifyMessage);
            } else {
                throw new IllegalArgumentException("不支持的通知类型：" + notifyType);
            }

            channel.basicAck(deliveryTag, false);

            log.info("异步通知处理成功，deliveryTag={}，messageId={}，notifyType={}",
                    deliveryTag, notifyMessage.getMessageId(), notifyType);
        } catch (Exception e) {
            log.error("异步通知处理失败，deliveryTag={}，notifyType={}，message={}",
                    deliveryTag, notifyType, notifyMessage, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

}
```

异步通知处理建议如下。

| 建议                   | 说明                                       |
| ---------------------- | ------------------------------------------ |
| 通知发送不要阻塞主流程 | 下单、注册、支付等主流程只负责投递通知消息 |
| 不同渠道拆分队列       | 短信、邮件、站内信分开消费，避免互相影响   |
| 第三方失败要重试       | 短信、邮件服务商短暂失败可以进入重试队列   |
| 通知记录建议落库       | 便于查询发送状态、失败原因和补发           |
| 重要通知需要幂等       | 避免重复发送短信或邮件                     |
| 低价值通知可降级       | 非关键通知失败后可记录日志，不阻断主业务   |

### 日志异步收集

日志异步收集适用于操作日志、审计日志、访问日志、行为日志等场景。业务接口中只负责快速投递日志消息，日志消费者异步写入数据库、搜索引擎、日志平台或对象存储，从而降低主接口响应耗时。

日志异步收集推荐链路如下。

```text
用户执行业务操作
  -> 业务接口生成操作日志消息
  -> 发送到日志交换机
  -> 日志消费者异步消费
  -> 写入日志表或日志系统
```

操作日志消息实体如下。

文件位置：`src/main/java/io/github/atengk/message/OperationLogMessage.java`

```java
package io.github.atengk.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 操作日志消息
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationLogMessage {

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人名称
     */
    private String operatorName;

    /**
     * 操作模块
     */
    private String module;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 业务主键
     */
    private String businessKey;

    /**
     * 操作内容
     */
    private String content;

    /**
     * 请求IP
     */
    private String requestIp;

    /**
     * 扩展参数
     */
    private Map<String, Object> extra;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;

}
```

日志生产者如下。日志类消息通常可以容忍一定程度延迟，但审计日志仍建议持久化并配合失败兜底。

文件位置：`src/main/java/io/github/atengk/producer/OperationLogProducer.java`

```java
package io.github.atengk.producer;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.constant.RabbitMqBusinessConstant;
import io.github.atengk.message.OperationLogMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 操作日志生产者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperationLogProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送操作日志消息
     *
     * @param logMessage 操作日志消息
     */
    public void sendOperationLog(OperationLogMessage logMessage) {
        this.fillMessage(logMessage);

        rabbitTemplate.convertAndSend(
                RabbitMqBusinessConstant.LOG_DIRECT_EXCHANGE,
                RabbitMqBusinessConstant.OPERATION_LOG_ROUTING_KEY,
                logMessage,
                message -> {
                    message.getMessageProperties().setMessageId(logMessage.getMessageId());
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    message.getMessageProperties().setHeader("module", logMessage.getModule());
                    message.getMessageProperties().setHeader("operationType", logMessage.getOperationType());
                    return message;
                }
        );

        log.debug("操作日志消息发送成功，messageId={}，module={}，operationType={}",
                logMessage.getMessageId(), logMessage.getModule(), logMessage.getOperationType());
    }

    /**
     * 填充日志消息基础字段
     *
     * @param logMessage 操作日志消息
     */
    private void fillMessage(OperationLogMessage logMessage) {
        if (logMessage == null) {
            throw new IllegalArgumentException("操作日志消息不能为空");
        }

        if (StrUtil.isBlank(logMessage.getMessageId())) {
            logMessage.setMessageId(IdUtil.fastSimpleUUID());
        }

        if (logMessage.getOperationTime() == null) {
            logMessage.setOperationTime(LocalDateTime.now());
        }
    }

}
```

日志服务负责写入存储介质。下面代码用日志模拟，实际项目中可以写入 MySQL、PostgreSQL、Elasticsearch、ClickHouse 或日志平台。

文件位置：`src/main/java/io/github/atengk/service/OperationLogService.java`

```java
package io.github.atengk.service;

import io.github.atengk.message.OperationLogMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 操作日志业务服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
public class OperationLogService {

    /**
     * 保存操作日志
     *
     * @param message 操作日志消息
     */
    public void saveOperationLog(OperationLogMessage message) {
        log.info("保存操作日志，messageId={}，operatorId={}，module={}，operationType={}，businessKey={}",
                message.getMessageId(),
                message.getOperatorId(),
                message.getModule(),
                message.getOperationType(),
                message.getBusinessKey());

        // 示例：实际项目中写入操作日志表、ES、ClickHouse 或日志系统
    }

}
```

日志消费者如下。

文件位置：`src/main/java/io/github/atengk/consumer/OperationLogConsumer.java`

```java
package io.github.atengk.consumer;

import com.rabbitmq.client.Channel;
import io.github.atengk.constant.RabbitMqBusinessConstant;
import io.github.atengk.message.OperationLogMessage;
import io.github.atengk.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 操作日志消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperationLogConsumer {

    private final OperationLogService operationLogService;

    /**
     * 消费操作日志消息
     *
     * @param logMessage 操作日志消息
     * @param message    RabbitMQ 原始消息
     * @param channel    RabbitMQ 通道
     * @throws IOException 消息确认失败时抛出
     */
    @RabbitListener(
            queues = RabbitMqBusinessConstant.OPERATION_LOG_QUEUE,
            containerFactory = "reliableManualAckContainerFactory"
    )
    public void consumeOperationLog(OperationLogMessage logMessage, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            operationLogService.saveOperationLog(logMessage);

            channel.basicAck(deliveryTag, false);

            log.debug("操作日志消息消费成功，deliveryTag={}，messageId={}",
                    deliveryTag, logMessage.getMessageId());
        } catch (Exception e) {
            log.error("操作日志消息消费失败，deliveryTag={}，message={}", deliveryTag, logMessage, e);

            // 审计类日志建议进入死信队列，普通访问日志可根据业务等级决定是否丢弃
            channel.basicNack(deliveryTag, false, false);
        }
    }

}
```

业务接口中发送操作日志示例。

```java
OperationLogMessage logMessage = OperationLogMessage.builder()
        .operatorId(10001L)
        .operatorName("admin")
        .module("订单管理")
        .operationType("关闭订单")
        .businessKey("ORDER_202604300001")
        .content("用户手动关闭订单")
        .requestIp("127.0.0.1")
        .build();

operationLogProducer.sendOperationLog(logMessage);
```

日志异步收集建议如下。

| 建议             | 说明                                              |
| ---------------- | ------------------------------------------------- |
| 日志消息不要过大 | 请求体、响应体和异常堆栈要控制长度                |
| 审计日志要可靠   | 涉及安全、资金、权限的日志建议持久化并死信兜底    |
| 普通日志可降级   | 访问日志、行为日志可以按业务价值选择可靠等级      |
| 消费端批量写入   | 高吞吐日志可在消费端缓冲后批量入库                |
| 不要影响主业务   | 日志发送失败不应阻塞核心业务流程                  |
| 注意敏感字段脱敏 | 密码、身份证、手机号、Token 等字段要脱敏或不入 MQ |

### 数据同步任务

数据同步任务适用于一个系统的数据变更后，需要通知其他系统或模块进行缓存刷新、搜索索引更新、报表聚合、数据仓库同步等场景。RabbitMQ 可以将数据变更事件异步分发给多个消费者，让不同下游系统独立处理。

数据同步推荐使用 Topic Exchange，因为它可以按业务域和事件类型灵活订阅。

推荐 Routing Key 规范如下。

```text
sync.业务域.动作
```

示例：

```text
sync.order.created
sync.order.updated
sync.order.closed
sync.user.created
sync.user.updated
```

数据同步消息实体如下。

文件位置：`src/main/java/io/github/atengk/message/DataSyncMessage.java`

```java
package io.github.atengk.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 数据同步消息
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSyncMessage {

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 业务域，例如 order、user、product
     */
    private String domain;

    /**
     * 业务动作，例如 created、updated、deleted、closed
     */
    private String action;

    /**
     * 业务主键
     */
    private String businessKey;

    /**
     * 数据版本
     */
    private Long dataVersion;

    /**
     * 变更字段
     */
    private Map<String, Object> changedFields;

    /**
     * 事件发生时间
     */
    private LocalDateTime eventTime;

}
```

数据同步生产者根据业务域和动作生成 Routing Key。

文件位置：`src/main/java/io/github/atengk/producer/DataSyncProducer.java`

```java
package io.github.atengk.producer;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.constant.RabbitMqBusinessConstant;
import io.github.atengk.message.DataSyncMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 数据同步生产者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSyncProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送数据同步消息
     *
     * @param syncMessage 数据同步消息
     */
    public void sendDataSyncMessage(DataSyncMessage syncMessage) {
        this.fillMessage(syncMessage);

        String routingKey = StrUtil.format("sync.{}.{}", syncMessage.getDomain(), syncMessage.getAction());

        rabbitTemplate.convertAndSend(
                RabbitMqBusinessConstant.DATA_SYNC_TOPIC_EXCHANGE,
                routingKey,
                syncMessage,
                message -> {
                    message.getMessageProperties().setMessageId(syncMessage.getMessageId());
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    message.getMessageProperties().setHeader("domain", syncMessage.getDomain());
                    message.getMessageProperties().setHeader("action", syncMessage.getAction());
                    message.getMessageProperties().setHeader("businessKey", syncMessage.getBusinessKey());
                    return message;
                },
                new CorrelationData(syncMessage.getMessageId())
        );

        log.info("数据同步消息发送成功，messageId={}，routingKey={}，businessKey={}",
                syncMessage.getMessageId(), routingKey, syncMessage.getBusinessKey());
    }

    /**
     * 填充消息基础字段
     *
     * @param syncMessage 数据同步消息
     */
    private void fillMessage(DataSyncMessage syncMessage) {
        if (syncMessage == null) {
            throw new IllegalArgumentException("数据同步消息不能为空");
        }

        if (StrUtil.isBlank(syncMessage.getMessageId())) {
            syncMessage.setMessageId(IdUtil.fastSimpleUUID());
        }

        if (StrUtil.hasBlank(syncMessage.getDomain(), syncMessage.getAction(), syncMessage.getBusinessKey())) {
            throw new IllegalArgumentException("数据同步消息的业务域、动作和业务主键不能为空");
        }

        if (syncMessage.getEventTime() == null) {
            syncMessage.setEventTime(LocalDateTime.now());
        }
    }

}
```

数据同步业务服务负责根据消息内容执行具体同步动作。对于数据同步任务，建议消费者通过业务主键重新查询最新数据，而不是完全依赖 MQ 消息体中的字段。

文件位置：`src/main/java/io/github/atengk/service/DataSyncService.java`

```java
package io.github.atengk.service;

import io.github.atengk.message.DataSyncMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 数据同步业务服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
public class DataSyncService {

    /**
     * 同步订单数据
     *
     * @param message 数据同步消息
     */
    public void syncOrder(DataSyncMessage message) {
        log.info("同步订单数据，messageId={}，businessKey={}，action={}，dataVersion={}",
                message.getMessageId(), message.getBusinessKey(), message.getAction(), message.getDataVersion());

        // 示例：实际项目中根据订单号查询最新订单数据，再同步到缓存、ES、报表系统等
    }

    /**
     * 同步用户数据
     *
     * @param message 数据同步消息
     */
    public void syncUser(DataSyncMessage message) {
        log.info("同步用户数据，messageId={}，businessKey={}，action={}，dataVersion={}",
                message.getMessageId(), message.getBusinessKey(), message.getAction(), message.getDataVersion());

        // 示例：实际项目中根据用户ID查询最新用户数据，再同步到缓存、ES、画像系统等
    }

}
```

数据同步消费者按业务域拆分。订单同步队列只处理 `sync.order.#`，用户同步队列只处理 `sync.user.#`。

文件位置：`src/main/java/io/github/atengk/consumer/DataSyncConsumer.java`

```java
package io.github.atengk.consumer;

import com.rabbitmq.client.Channel;
import io.github.atengk.constant.RabbitMqBusinessConstant;
import io.github.atengk.message.DataSyncMessage;
import io.github.atengk.service.DataSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 数据同步消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSyncConsumer {

    private final DataSyncService dataSyncService;

    /**
     * 消费订单数据同步消息
     *
     * @param syncMessage 数据同步消息
     * @param message     RabbitMQ 原始消息
     * @param channel     RabbitMQ 通道
     * @throws IOException 消息确认失败时抛出
     */
    @RabbitListener(
            queues = RabbitMqBusinessConstant.DATA_SYNC_ORDER_QUEUE,
            containerFactory = "reliableManualAckContainerFactory"
    )
    public void consumeOrderSyncMessage(DataSyncMessage syncMessage, Message message, Channel channel) throws IOException {
        this.handleSync(message, channel, syncMessage, "order");
    }

    /**
     * 消费用户数据同步消息
     *
     * @param syncMessage 数据同步消息
     * @param message     RabbitMQ 原始消息
     * @param channel     RabbitMQ 通道
     * @throws IOException 消息确认失败时抛出
     */
    @RabbitListener(
            queues = RabbitMqBusinessConstant.DATA_SYNC_USER_QUEUE,
            containerFactory = "reliableManualAckContainerFactory"
    )
    public void consumeUserSyncMessage(DataSyncMessage syncMessage, Message message, Channel channel) throws IOException {
        this.handleSync(message, channel, syncMessage, "user");
    }

    /**
     * 处理数据同步消息
     *
     * @param message      RabbitMQ 原始消息
     * @param channel      RabbitMQ 通道
     * @param syncMessage  数据同步消息
     * @param expectedDomain 期望业务域
     * @throws IOException 消息确认失败时抛出
     */
    private void handleSync(Message message,
                            Channel channel,
                            DataSyncMessage syncMessage,
                            String expectedDomain) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            log.info("开始处理数据同步消息，deliveryTag={}，messageId={}，domain={}，action={}，businessKey={}",
                    deliveryTag,
                    syncMessage.getMessageId(),
                    syncMessage.getDomain(),
                    syncMessage.getAction(),
                    syncMessage.getBusinessKey());

            if ("order".equals(expectedDomain)) {
                dataSyncService.syncOrder(syncMessage);
            } else if ("user".equals(expectedDomain)) {
                dataSyncService.syncUser(syncMessage);
            } else {
                throw new IllegalArgumentException("不支持的数据同步业务域：" + expectedDomain);
            }

            channel.basicAck(deliveryTag, false);

            log.info("数据同步消息处理成功，deliveryTag={}，messageId={}，businessKey={}",
                    deliveryTag, syncMessage.getMessageId(), syncMessage.getBusinessKey());
        } catch (Exception e) {
            log.error("数据同步消息处理失败，deliveryTag={}，message={}", deliveryTag, syncMessage, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

}
```

发送订单数据同步消息示例。

```java
DataSyncMessage syncMessage = DataSyncMessage.builder()
        .domain("order")
        .action("updated")
        .businessKey("ORDER_202604300001")
        .dataVersion(2L)
        .build();

dataSyncProducer.sendDataSyncMessage(syncMessage);
```

发送用户数据同步消息示例。

```java
DataSyncMessage syncMessage = DataSyncMessage.builder()
        .domain("user")
        .action("updated")
        .businessKey("10001")
        .dataVersion(5L)
        .build();

dataSyncProducer.sendDataSyncMessage(syncMessage);
```

数据同步任务建议如下。

| 建议                 | 说明                                   |
| -------------------- | -------------------------------------- |
| 使用 Topic Exchange  | 便于按业务域、动作和状态灵活订阅       |
| 消息中携带业务主键   | 消费者根据主键查询最新数据             |
| 不要完全依赖消息快照 | MQ 消息可能延迟，消费时应获取最新状态  |
| 数据同步要幂等       | 同一条同步消息重复消费不能导致脏数据   |
| 可以携带数据版本     | 低版本消息不应覆盖高版本数据           |
| 同步失败进入死信     | 防止持续失败阻塞业务队列               |
| 下游系统独立消费     | 搜索索引、缓存、报表系统应使用独立队列 |

数据同步任务的核心不是“把完整数据塞进 MQ”，而是通过 MQ 传递数据变更事件。消费者收到事件后，基于业务主键查询权威数据源，再同步到目标系统。这样可以降低消息体大小，也能避免消息延迟导致旧数据覆盖新数据的问题。

## 接口与代码结构设计

本章节用于规范 RabbitMQ 在 Spring Boot 项目中的代码组织方式，包括配置类、常量类、消息实体、生产者组件和消费者组件的设计。该章节不是单独讲某一个业务功能，而是用于形成一套可复用、可维护、可扩展的 RabbitMQ 开发结构，避免交换机、队列、Routing Key、消息体和消费逻辑散落在项目各处。该部分对应上传大纲中的“接口与代码结构设计”章节。

推荐基础结构如下。

```text
src/main/java/io/github/atengk
├── config
│   ├── RabbitMqInfrastructureConfig.java
│   └── RabbitMqOrderConfig.java
├── constant
│   └── RabbitMqOrderConstant.java
├── message
│   ├── BaseRabbitMessage.java
│   └── OrderCreatedMessage.java
├── producer
│   └── OrderMessageProducer.java
├── consumer
│   └── OrderCreatedConsumer.java
└── service
    └── OrderBusinessService.java
```

各层职责建议如下。

| 分层       | 职责                                                         |
| ---------- | ------------------------------------------------------------ |
| `config`   | 声明交换机、队列、绑定关系、消息转换器、监听容器、可靠性回调 |
| `constant` | 统一维护 Exchange、Queue、Routing Key、Header Key、业务类型等常量 |
| `message`  | 定义 MQ 消息实体，承载消息 ID、业务主键、事件类型、创建时间等字段 |
| `producer` | 封装消息发送逻辑，屏蔽 `RabbitTemplate` 细节                 |
| `consumer` | 封装消息监听、参数校验、手动确认、异常处理和业务服务调用     |
| `service`  | 执行业务逻辑，例如订单状态更新、通知发送、日志落库、数据同步 |

### 配置类设计

配置类用于集中声明 RabbitMQ 的基础能力和业务拓扑。基础配置建议只放通用能力，例如 `MessageConverter`、`RabbitTemplate`、监听容器工厂等；业务配置建议按业务域拆分，例如订单配置、支付配置、通知配置、日志配置等。

推荐拆分方式如下。

| 配置类                         | 说明                                                         |
| ------------------------------ | ------------------------------------------------------------ |
| `RabbitMqInfrastructureConfig` | RabbitMQ 基础设施配置，例如 JSON 转换器、监听容器、可靠性回调 |
| `RabbitMqOrderConfig`          | 订单业务交换机、队列和绑定关系                               |
| `RabbitMqNotifyConfig`         | 通知业务交换机、队列和绑定关系                               |
| `RabbitMqDeadLetterConfig`     | 死信交换机、重试队列、兜底队列                               |
| `RabbitMqDelayConfig`          | 延迟交换机、延迟队列或插件延迟配置                           |

基础设施配置类负责声明通用 MQ 组件。

文件位置：`src/main/java/io/github/atengk/config/RabbitMqInfrastructureConfig.java`

```java
package io.github.atengk.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.autoconfigure.RabbitTemplateConfigurer;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 基础设施配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RabbitMqInfrastructureConfig {

    private final ConnectionFactory connectionFactory;

    /**
     * JSON 消息转换器
     *
     * @param objectMapper JSON 序列化组件
     * @return 消息转换器
     */
    @Bean
    public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * RabbitTemplate 可靠性配置
     *
     * @param configurer       RabbitTemplate 自动配置器
     * @param messageConverter 消息转换器
     * @return RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(RabbitTemplateConfigurer configurer,
                                         MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate();
        configurer.configure(rabbitTemplate, connectionFactory);

        // 使用 JSON 格式发送对象消息
        rabbitTemplate.setMessageConverter(messageConverter);

        // 消息无法路由到队列时触发 ReturnsCallback
        rabbitTemplate.setMandatory(true);

        // 生产者确认回调，确认消息是否到达 Broker
        rabbitTemplate.setConfirmCallback(this::handleConfirm);

        // 消息返回回调，处理无法路由到队列的消息
        rabbitTemplate.setReturnsCallback(returned -> {
            String messageId = returned.getMessage().getMessageProperties().getMessageId();
            log.error("RabbitMQ消息路由失败，messageId={}，exchange={}，routingKey={}，replyCode={}，replyText={}",
                    messageId,
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    returned.getReplyCode(),
                    returned.getReplyText());

            // 生产环境建议记录到消息发送日志表，后续由补偿任务处理
        });

        return rabbitTemplate;
    }

    /**
     * 手动确认监听容器工厂
     *
     * @param messageConverter 消息转换器
     * @return 监听容器工厂
     */
    @Bean(name = "manualRabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory manualRabbitListenerContainerFactory(MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();

        // RabbitMQ 连接工厂
        factory.setConnectionFactory(connectionFactory);

        // 统一使用 JSON 消息转换器
        factory.setMessageConverter(messageConverter);

        // 重要业务使用手动确认模式
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);

        // 每个消费者最多持有的未确认消息数量
        factory.setPrefetchCount(10);

        // 初始消费者数量
        factory.setConcurrentConsumers(2);

        // 最大消费者数量
        factory.setMaxConcurrentConsumers(6);

        // 消费异常默认不重新入队，避免异常消息无限循环
        factory.setDefaultRequeueRejected(false);

        return factory;
    }

    /**
     * 处理生产者确认回调
     *
     * @param correlationData 关联数据
     * @param ack             是否确认成功
     * @param cause           失败原因
     */
    private void handleConfirm(CorrelationData correlationData, boolean ack, String cause) {
        String correlationId = correlationData == null ? null : correlationData.getId();

        if (ack) {
            log.info("RabbitMQ消息发送到Broker成功，correlationId={}", correlationId);
            return;
        }

        log.error("RabbitMQ消息发送到Broker失败，correlationId={}，cause={}", correlationId, cause);

        // 生产环境建议更新消息发送状态，并由补偿任务重新发送
    }

}
```

订单业务配置类只负责订单相关的交换机、队列和绑定关系。

文件位置：`src/main/java/io/github/atengk/config/RabbitMqOrderConfig.java`

```java
package io.github.atengk.config;

import io.github.atengk.constant.RabbitMqOrderConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 订单业务配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
public class RabbitMqOrderConfig {

    /**
     * 订单交换机
     *
     * @return Direct 交换机
     */
    @Bean
    public DirectExchange orderDirectExchange() {
        return ExchangeBuilder
                .directExchange(RabbitMqOrderConstant.ORDER_DIRECT_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 订单创建队列
     *
     * @return 队列
     */
    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder
                .durable(RabbitMqOrderConstant.ORDER_CREATED_QUEUE)
                .build();
    }

    /**
     * 订单取消队列
     *
     * @return 队列
     */
    @Bean
    public Queue orderCancelledQueue() {
        return QueueBuilder
                .durable(RabbitMqOrderConstant.ORDER_CANCELLED_QUEUE)
                .build();
    }

    /**
     * 订单创建绑定关系
     *
     * @param orderDirectExchange 订单交换机
     * @param orderCreatedQueue   订单创建队列
     * @return 绑定关系
     */
    @Bean
    public Binding orderCreatedBinding(DirectExchange orderDirectExchange, Queue orderCreatedQueue) {
        return BindingBuilder
                .bind(orderCreatedQueue)
                .to(orderDirectExchange)
                .with(RabbitMqOrderConstant.ORDER_CREATED_ROUTING_KEY);
    }

    /**
     * 订单取消绑定关系
     *
     * @param orderDirectExchange 订单交换机
     * @param orderCancelledQueue 订单取消队列
     * @return 绑定关系
     */
    @Bean
    public Binding orderCancelledBinding(DirectExchange orderDirectExchange, Queue orderCancelledQueue) {
        return BindingBuilder
                .bind(orderCancelledQueue)
                .to(orderDirectExchange)
                .with(RabbitMqOrderConstant.ORDER_CANCELLED_ROUTING_KEY);
    }

}
```

配置类设计建议如下。

| 建议                             | 说明                                          |
| -------------------------------- | --------------------------------------------- |
| 基础配置和业务配置分离           | 避免一个配置类过大                            |
| 交换机、队列、绑定通过 Bean 声明 | 项目启动时自动声明资源，便于维护              |
| 队列参数不要频繁修改             | 已存在队列参数不一致会导致声明失败            |
| 重要业务使用手动确认             | 配合 `basicAck`、`basicNack` 控制消息生命周期 |
| 生产者回调统一配置               | 不要在多个地方重复设置 ConfirmCallback        |
| 配置类不要写业务逻辑             | 配置类只负责资源声明和基础组件配置            |

### 常量类设计

常量类用于统一维护 RabbitMQ 的资源名称和业务标识。交换机名称、队列名称、Routing Key、Header Key、业务类型等都应放到常量类中，避免字符串直接写在生产者、消费者或配置类中。

推荐按业务域拆分常量类，而不是把所有常量都放在一个大类中。

| 常量类                       | 说明                   |
| ---------------------------- | ---------------------- |
| `RabbitMqOrderConstant`      | 订单业务 MQ 常量       |
| `RabbitMqPayConstant`        | 支付业务 MQ 常量       |
| `RabbitMqNotifyConstant`     | 通知业务 MQ 常量       |
| `RabbitMqLogConstant`        | 日志业务 MQ 常量       |
| `RabbitMqDeadLetterConstant` | 死信和重试相关 MQ 常量 |

订单常量类示例如下。

文件位置：`src/main/java/io/github/atengk/constant/RabbitMqOrderConstant.java`

```java
package io.github.atengk.constant;

/**
 * RabbitMQ 订单业务常量
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class RabbitMqOrderConstant {

    /**
     * 订单 Direct 交换机
     */
    public static final String ORDER_DIRECT_EXCHANGE = "order.direct.exchange";

    /**
     * 订单创建队列
     */
    public static final String ORDER_CREATED_QUEUE = "order.created.queue";

    /**
     * 订单取消队列
     */
    public static final String ORDER_CANCELLED_QUEUE = "order.cancelled.queue";

    /**
     * 订单创建 Routing Key
     */
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";

    /**
     * 订单取消 Routing Key
     */
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.cancelled";

    /**
     * 订单业务类型
     */
    public static final String BUSINESS_TYPE_ORDER = "ORDER";

    /**
     * 消息头：业务类型
     */
    public static final String HEADER_BUSINESS_TYPE = "businessType";

    /**
     * 消息头：业务主键
     */
    public static final String HEADER_BUSINESS_KEY = "businessKey";

    /**
     * 消息头：发送时间
     */
    public static final String HEADER_SEND_TIME = "sendTime";

    private RabbitMqOrderConstant() {
    }

}
```

常量命名建议如下。

| 类型              | 命名示例                      | 说明                   |
| ----------------- | ----------------------------- | ---------------------- |
| Exchange          | `order.direct.exchange`       | `业务域.类型.exchange` |
| Queue             | `order.created.queue`         | `业务域.事件.queue`    |
| Routing Key       | `order.created`               | `业务域.动作`          |
| Topic Routing Key | `order.pay.success`           | `业务域.动作.状态`     |
| Header Key        | `businessType`、`businessKey` | 使用清晰业务语义       |
| 业务类型          | `ORDER`、`PAY`、`STOCK`       | 建议使用大写           |

常量类设计建议如下。

| 建议                         | 说明                                             |
| ---------------------------- | ------------------------------------------------ |
| 按业务域拆分                 | 订单、支付、通知、日志分别维护                   |
| 使用 `final class`           | 明确该类不用于继承                               |
| 私有构造方法                 | 避免常量类被实例化                               |
| 不在业务代码中硬编码 MQ 名称 | 所有 Exchange、Queue、Routing Key 均从常量类引用 |
| 名称保持稳定                 | 生产环境中修改队列名会影响已有消费者和消息       |
| 命名体现业务语义             | 排查控制台和日志时更容易定位问题                 |

### 消息实体设计

消息实体用于承载业务消息内容。一个合格的消息实体不应只包含业务字段，还应包含消息 ID、业务主键、事件类型、创建时间等元数据，方便后续做日志追踪、幂等消费、异常排查和补偿处理。

推荐所有业务消息继承或组合一个基础消息结构。

文件位置：`src/main/java/io/github/atengk/message/BaseRabbitMessage.java`

```java
package io.github.atengk.message;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * RabbitMQ 基础消息
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Data
public class BaseRabbitMessage {

    /**
     * 消息ID，用于日志追踪和幂等处理
     */
    private String messageId;

    /**
     * 业务类型，例如 ORDER、PAY、STOCK
     */
    private String businessType;

    /**
     * 业务主键，例如订单号、支付单号、用户ID
     */
    private String businessKey;

    /**
     * 事件类型，例如 CREATED、PAID、CANCELLED
     */
    private String eventType;

    /**
     * 消息创建时间
     */
    private LocalDateTime createTime;

}
```

订单创建消息实体如下。

文件位置：`src/main/java/io/github/atengk/message/OrderCreatedMessage.java`

```java
package io.github.atengk.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * 订单创建消息
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderCreatedMessage extends BaseRabbitMessage {

    /**
     * 订单号
     */
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

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
     * 订单状态
     */
    private String orderStatus;

}
```

消息实体字段建议如下。

| 字段           | 是否建议 | 说明                                  |
| -------------- | -------- | ------------------------------------- |
| `messageId`    | 必须     | 用于日志追踪、生产确认、幂等消费      |
| `businessType` | 建议     | 标识业务域，例如订单、支付、库存      |
| `businessKey`  | 必须     | 业务主键，例如订单号、支付单号        |
| `eventType`    | 建议     | 标识事件类型，例如创建、支付、取消    |
| `createTime`   | 建议     | 记录消息创建时间                      |
| 业务字段       | 按需     | 只放消费者必要字段                    |
| 大对象字段     | 不建议   | 大文本、大文件、完整对象不建议放入 MQ |

消息实体设计建议如下。

| 建议                   | 说明                                         |
| ---------------------- | -------------------------------------------- |
| 消息体保持轻量         | 只传必要字段，大对象通过业务主键查询         |
| 必须有业务主键         | 消费者必须能根据消息定位业务数据             |
| 必须支持幂等           | 至少有 `messageId` 或业务唯一键              |
| 不传敏感字段           | 密码、Token、密钥、身份证等不应进入 MQ       |
| 字段变更保持兼容       | 新增字段要允许消费者处理旧消息               |
| 时间类型统一           | 使用统一 `ObjectMapper` 处理 `LocalDateTime` |
| 消费者不要完全依赖快照 | 对订单、支付等关键业务，应查询数据库最新状态 |

### 生产者组件设计

生产者组件用于封装消息发送逻辑。业务代码不应直接到处注入 `RabbitTemplate`，否则交换机、Routing Key、消息属性、日志、确认关联数据都会分散在多个业务类中，后续维护困难。

生产者组件一般负责以下内容。

| 职责                     | 说明                                                         |
| ------------------------ | ------------------------------------------------------------ |
| 填充消息元数据           | 生成 `messageId`、`businessType`、`businessKey`、`createTime` |
| 设置消息属性             | 设置持久化、Header、CorrelationData                          |
| 选择交换机和 Routing Key | 根据业务事件选择目标路由                                     |
| 记录发送日志             | 记录 messageId、业务主键、交换机、Routing Key                |
| 抛出发送异常             | 不吞异常，交给业务层或补偿机制处理                           |

订单消息生产者示例如下。

文件位置：`src/main/java/io/github/atengk/producer/OrderMessageProducer.java`

```java
package io.github.atengk.producer;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.constant.RabbitMqOrderConstant;
import io.github.atengk.message.OrderCreatedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

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

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送订单创建消息
     *
     * @param orderMessage 订单创建消息
     */
    public void sendOrderCreatedMessage(OrderCreatedMessage orderMessage) {
        this.fillOrderCreatedMessage(orderMessage);

        CorrelationData correlationData = new CorrelationData(orderMessage.getMessageId());

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqOrderConstant.ORDER_DIRECT_EXCHANGE,
                    RabbitMqOrderConstant.ORDER_CREATED_ROUTING_KEY,
                    orderMessage,
                    message -> {
                        message.getMessageProperties().setMessageId(orderMessage.getMessageId());
                        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        message.getMessageProperties().setHeader(RabbitMqOrderConstant.HEADER_BUSINESS_TYPE, orderMessage.getBusinessType());
                        message.getMessageProperties().setHeader(RabbitMqOrderConstant.HEADER_BUSINESS_KEY, orderMessage.getBusinessKey());
                        message.getMessageProperties().setHeader(
                                RabbitMqOrderConstant.HEADER_SEND_TIME,
                                LocalDateTimeUtil.format(LocalDateTime.now(), DatePattern.NORM_DATETIME_PATTERN)
                        );
                        return message;
                    },
                    correlationData
            );

            log.info("订单创建消息发送成功，messageId={}，orderNo={}，exchange={}，routingKey={}",
                    orderMessage.getMessageId(),
                    orderMessage.getOrderNo(),
                    RabbitMqOrderConstant.ORDER_DIRECT_EXCHANGE,
                    RabbitMqOrderConstant.ORDER_CREATED_ROUTING_KEY);
        } catch (AmqpException e) {
            log.error("订单创建消息发送失败，messageId={}，orderNo={}",
                    orderMessage.getMessageId(), orderMessage.getOrderNo(), e);
            throw e;
        }
    }

    /**
     * 填充订单创建消息
     *
     * @param orderMessage 订单创建消息
     */
    private void fillOrderCreatedMessage(OrderCreatedMessage orderMessage) {
        if (orderMessage == null) {
            throw new IllegalArgumentException("订单创建消息不能为空");
        }

        if (StrUtil.isBlank(orderMessage.getMessageId())) {
            orderMessage.setMessageId(IdUtil.fastSimpleUUID());
        }

        if (StrUtil.isBlank(orderMessage.getBusinessType())) {
            orderMessage.setBusinessType(RabbitMqOrderConstant.BUSINESS_TYPE_ORDER);
        }

        if (StrUtil.isBlank(orderMessage.getBusinessKey())) {
            orderMessage.setBusinessKey(orderMessage.getOrderNo());
        }

        if (StrUtil.isBlank(orderMessage.getEventType())) {
            orderMessage.setEventType("ORDER_CREATED");
        }

        if (orderMessage.getCreateTime() == null) {
            orderMessage.setCreateTime(LocalDateTime.now());
        }
    }

}
```

业务层调用生产者时，只需要构建业务消息对象，不需要关心 RabbitMQ 底层细节。

```java
OrderCreatedMessage message = OrderCreatedMessage.builder()
        .orderNo("ORDER_202604300001")
        .userId(10001L)
        .amount(new BigDecimal("99.90"))
        .orderStatus("CREATED")
        .build();

orderMessageProducer.sendOrderCreatedMessage(message);
```

生产者组件设计建议如下。

| 建议                                        | 说明                                                  |
| ------------------------------------------- | ----------------------------------------------------- |
| 不在 Controller 中直接使用 `RabbitTemplate` | Controller 只负责接收请求和调用业务服务               |
| 发送逻辑统一封装                            | 交换机、Routing Key、消息属性集中管理                 |
| 每条消息设置 `messageId`                    | 便于确认回调、幂等和排查                              |
| 重要消息设置持久化                          | 使用 `MessageDeliveryMode.PERSISTENT`                 |
| 使用 `CorrelationData`                      | 生产者确认时关联业务消息                              |
| 发送失败不要静默吞掉                        | 记录日志并交给补偿机制处理                            |
| 日志包含关键字段                            | 至少包含 messageId、businessKey、exchange、routingKey |

### 消费者组件设计

消费者组件用于监听队列、接收消息、校验参数、调用业务服务、处理异常和确认消息。消费者不建议承载大量业务逻辑，应将核心业务处理放到 Service 中，消费者只负责消息消费流程控制。

消费者组件一般负责以下内容。

| 职责         | 说明                                               |
| ------------ | -------------------------------------------------- |
| 监听指定队列 | 使用 `@RabbitListener` 指定队列                    |
| 接收消息对象 | 使用统一 JSON 转换器反序列化消息                   |
| 校验消息参数 | 检查 messageId、业务主键等必要字段                 |
| 调用业务服务 | 将实际业务处理交给 Service                         |
| 手动确认消息 | 成功 `basicAck`，失败 `basicNack` 或 `basicReject` |
| 记录消费日志 | 记录 deliveryTag、messageId、业务主键              |
| 控制异常流转 | 区分参数异常、业务异常、系统异常                   |

订单创建消费者示例如下。

文件位置：`src/main/java/io/github/atengk/consumer/OrderCreatedConsumer.java`

```java
package io.github.atengk.consumer;

import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.Channel;
import io.github.atengk.constant.RabbitMqOrderConstant;
import io.github.atengk.message.OrderCreatedMessage;
import io.github.atengk.service.OrderBusinessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 订单创建消息消费者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final OrderBusinessService orderBusinessService;

    /**
     * 消费订单创建消息
     *
     * @param orderMessage 订单创建消息
     * @param message      RabbitMQ 原始消息
     * @param channel      RabbitMQ 通道
     * @throws IOException 消息确认失败时抛出
     */
    @RabbitListener(
            queues = RabbitMqOrderConstant.ORDER_CREATED_QUEUE,
            containerFactory = "manualRabbitListenerContainerFactory"
    )
    public void consumeOrderCreatedMessage(OrderCreatedMessage orderMessage,
                                           Message message,
                                           Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            this.validateMessage(orderMessage);

            log.info("开始消费订单创建消息，deliveryTag={}，messageId={}，orderNo={}",
                    deliveryTag, orderMessage.getMessageId(), orderMessage.getOrderNo());

            orderBusinessService.handleOrderCreated(orderMessage);

            channel.basicAck(deliveryTag, false);

            log.info("订单创建消息消费成功，deliveryTag={}，messageId={}，orderNo={}",
                    deliveryTag, orderMessage.getMessageId(), orderMessage.getOrderNo());
        } catch (IllegalArgumentException e) {
            log.warn("订单创建消息参数异常，拒绝且不重新入队，deliveryTag={}，message={}",
                    deliveryTag, orderMessage, e);
            channel.basicReject(deliveryTag, false);
        } catch (Exception e) {
            log.error("订单创建消息消费失败，拒绝且不重新入队，deliveryTag={}，message={}",
                    deliveryTag, orderMessage, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 校验订单创建消息
     *
     * @param orderMessage 订单创建消息
     */
    private void validateMessage(OrderCreatedMessage orderMessage) {
        if (orderMessage == null) {
            throw new IllegalArgumentException("订单创建消息不能为空");
        }

        if (StrUtil.isBlank(orderMessage.getMessageId())) {
            throw new IllegalArgumentException("消息ID不能为空");
        }

        if (StrUtil.isBlank(orderMessage.getOrderNo())) {
            throw new IllegalArgumentException("订单号不能为空");
        }

        if (orderMessage.getUserId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
    }

}
```

业务服务只处理业务逻辑，不直接依赖 `Channel`、`Message` 等 RabbitMQ 底层对象。

文件位置：`src/main/java/io/github/atengk/service/OrderBusinessService.java`

```java
package io.github.atengk.service;

import io.github.atengk.message.OrderCreatedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单业务服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
public class OrderBusinessService {

    /**
     * 处理订单创建事件
     *
     * @param orderMessage 订单创建消息
     */
    public void handleOrderCreated(OrderCreatedMessage orderMessage) {
        log.info("处理订单创建业务，messageId={}，orderNo={}，userId={}，amount={}",
                orderMessage.getMessageId(),
                orderMessage.getOrderNo(),
                orderMessage.getUserId(),
                orderMessage.getAmount());

        // 示例：实际项目中可处理积分、通知、日志、数据同步等业务
        // 重要业务需要增加幂等控制，例如 messageId 唯一记录或 orderNo + eventType 唯一约束
    }

}
```

消费者异常处理建议如下。

| 异常类型       | 推荐处理                                        |
| -------------- | ----------------------------------------------- |
| 消息为空       | `basicReject(false)`                            |
| 必填字段缺失   | `basicReject(false)`                            |
| 业务状态不允许 | 根据业务结果 `basicAck` 或 `basicReject(false)` |
| 临时网络异常   | 可进入重试队列，不建议无限重新入队              |
| 数据库异常     | 可有限重试，重试耗尽后进入死信                  |
| 未知系统异常   | `basicNack(false, false)`，进入死信兜底         |

消费者组件设计建议如下。

| 建议                       | 说明                                          |
| -------------------------- | --------------------------------------------- |
| 一个消费者监听一个明确队列 | 避免单个消费者处理过多业务                    |
| 消费者只做流程控制         | 复杂业务逻辑放到 Service                      |
| 重要业务使用手动确认       | 成功后 `basicAck`，失败按策略处理             |
| 参数异常不要重新入队       | 非法消息重复消费没有意义                      |
| 必须记录关键日志           | deliveryTag、messageId、businessKey、异常原因 |
| 消费逻辑必须幂等           | 允许同一消息重复投递                          |
| 失败消息要有兜底           | 配合死信队列、补偿任务和告警                  |

完整调用链建议如下。

```text
Controller / Service
  -> Producer
  -> RabbitTemplate
  -> Exchange
  -> Queue
  -> Consumer
  -> Business Service
  -> basicAck / basicNack / basicReject
```

整体代码结构设计的核心原则是：配置集中、常量统一、消息标准化、发送封装化、消费流程化、业务服务独立化。这样可以降低 RabbitMQ 接入成本，也能让后续的可靠性确认、死信重试、延迟消息、幂等消费和监控告警更容易统一扩展。

## 测试与验证

本章节用于说明 RabbitMQ 开发完成后的测试与验证方式，包括本地联调流程、RabbitMQ 管理控制台验证、单元测试设计以及消息异常场景验证。RabbitMQ 的测试不能只验证接口返回成功，还需要验证交换机、队列、绑定关系、消息路由、消费者确认、异常重试、死信兜底和幂等逻辑是否符合预期。该章节对应上传大纲中的“测试与验证”部分。

RabbitMQ 测试建议分为四层：

| 测试层级     | 验证目标                                                     |
| ------------ | ------------------------------------------------------------ |
| 本地联调     | 验证 Spring Boot 应用和 RabbitMQ 是否能正常连接、发送、消费  |
| 控制台验证   | 验证 Exchange、Queue、Binding、Ready、Unacked、Consumers 等状态 |
| 自动化测试   | 验证 Producer、Consumer、Service、消息转换和确认逻辑         |
| 异常场景验证 | 验证不可路由、消费失败、重复消费、死信、延迟和重试等边界情况 |

### 本地联调流程

本地联调用于验证 RabbitMQ 服务、Spring Boot 应用、生产者、消费者和管理控制台之间是否能够正常协作。联调前需要先启动 RabbitMQ，再启动 Spring Boot 项目，最后通过接口或测试方法发送消息。

推荐本地联调流程如下。

```text
启动 RabbitMQ
  -> 检查管理控制台
  -> 启动 Spring Boot 应用
  -> 检查交换机、队列、绑定关系是否自动声明
  -> 调用接口发送消息
  -> 查看应用日志和 RabbitMQ 控制台
  -> 验证消费者是否正常消费
```

使用 Docker 启动 RabbitMQ。

```bash
# 启动 RabbitMQ 容器
docker run -d \
  --name rabbitmq \
  --hostname rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=admin \
  -e RABBITMQ_DEFAULT_PASS=admin123 \
  -e RABBITMQ_DEFAULT_VHOST=/dev \
  rabbitmq:3-management
```

如果容器已经存在，可以直接启动。

```bash
# 启动已有容器
docker start rabbitmq

# 查看容器状态
docker ps | grep rabbitmq

# 查看 RabbitMQ 日志
docker logs -f rabbitmq
```

确认 Spring Boot 的开发环境配置。

文件位置：`src/main/resources/application-dev.yml`

```yaml
spring:
  rabbitmq:
    # RabbitMQ 服务地址
    host: localhost

    # AMQP 协议端口
    port: 5672

    # 登录用户名
    username: admin

    # 登录密码，仅用于本地开发环境
    password: admin123

    # 虚拟主机，需要和 RabbitMQ 中的 vhost 保持一致
    virtual-host: /dev

    # 开启生产者确认
    publisher-confirm-type: correlated

    # 开启消息返回
    publisher-returns: true

    template:
      # mandatory 为 true 时，不可路由消息会触发 returns 回调
      mandatory: true

    listener:
      simple:
        # 重要业务使用手动确认
        acknowledge-mode: manual

        # 每个消费者最多预取的未确认消息数
        prefetch: 10

        # 初始消费者并发数
        concurrency: 1

        # 最大消费者并发数
        max-concurrency: 3

        # 消费异常默认不重新入队
        default-requeue-rejected: false
```

启动 Spring Boot 项目。

```bash
# 使用 dev 环境启动项目
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

启动成功后，重点观察日志中是否有以下异常。

| 异常类型                           | 常见原因                               |
| ---------------------------------- | -------------------------------------- |
| `Connection refused`               | RabbitMQ 未启动，或端口不是 `5672`     |
| `ACCESS_REFUSED`                   | 用户名、密码、vhost 或权限错误         |
| `NOT_FOUND - no exchange`          | 交换机不存在，或配置类没有声明         |
| `PRECONDITION_FAILED`              | 队列已存在，但参数与当前声明不一致     |
| `ListenerExecutionFailedException` | 消费者处理异常、消息转换异常或业务异常 |

发送简单消息进行联调。

```bash
curl -X POST "http://localhost:8080/api/mq/send/simple?content=hello-rabbitmq"
```

发送对象消息进行联调。

```bash
curl -X POST "http://localhost:8080/api/mq/send/order" \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "ORDER_TEST_202604300001",
    "userId": 10001,
    "amount": 99.90,
    "status": "CREATED"
  }'
```

发送延迟消息进行联调。

```bash
curl -X POST "http://localhost:8080/api/mq/plugin-delay/order/send?delayMillis=10000" \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "ORDER_DELAY_TEST_202604300001",
    "userId": 10001,
    "amount": 199.90,
    "status": "WAIT_PAY"
  }'
```

本地联调时建议按以下顺序排查问题。

| 步骤 | 检查内容                              |
| ---- | ------------------------------------- |
| 1    | RabbitMQ 容器是否运行                 |
| 2    | `http://localhost:15672` 是否能访问   |
| 3    | Spring Boot 是否连接到正确 vhost      |
| 4    | Exchange、Queue、Binding 是否创建成功 |
| 5    | 生产者发送日志是否正常                |
| 6    | ConfirmCallback 是否返回 `ack=true`   |
| 7    | ReturnsCallback 是否有不可路由日志    |
| 8    | 消费者日志是否正常打印                |
| 9    | 消息是否被 `basicAck` 确认            |
| 10   | 死信队列或重试队列是否存在异常消息    |

### 管理控制台验证

RabbitMQ 管理控制台用于直观验证消息资源和消息状态。本地默认访问地址为：

```text
http://localhost:15672
```

登录信息通常为：

| 项目       | 值         |
| ---------- | ---------- |
| 用户名     | `admin`    |
| 密码       | `admin123` |
| vhost      | `/dev`     |
| AMQP 端口  | `5672`     |
| 控制台端口 | `15672`    |

进入控制台后，优先检查 `Virtual Hosts` 是否存在 `/dev`。如果应用连接的是 `/dev`，但控制台当前查看的是 `/`，会出现“代码已经声明资源，但控制台看不到”的错觉。

管理控制台常用页面如下。

| 页面               | 验证内容                                   |
| ------------------ | ------------------------------------------ |
| Overview           | Broker 状态、消息速率、连接数、队列数      |
| Connections        | Spring Boot 应用是否已连接                 |
| Channels           | 应用连接下的 Channel 是否正常              |
| Exchanges          | 交换机是否存在、类型是否正确               |
| Queues and Streams | 队列是否存在、消息是否堆积、消费者是否在线 |
| Admin              | 用户、vhost、权限是否正确                  |

队列验证重点如下。

| 指标          | 含义                     | 判断方式                               |
| ------------- | ------------------------ | -------------------------------------- |
| Ready         | 等待消费的消息数量       | 持续增长说明消费者未消费或处理太慢     |
| Unacked       | 已投递但未确认的消息数量 | 长时间不下降说明消费者可能阻塞或未 ack |
| Total         | 队列总消息数             | `Ready + Unacked`                      |
| Consumers     | 当前消费者数量           | 为 0 说明没有消费者监听该队列          |
| Publish       | 消息发布速率             | 调用发送接口后应出现变化               |
| Deliver / Get | 消息投递速率             | 消费者在线时应出现变化                 |
| Ack           | 消息确认速率             | 手动确认成功后应出现变化               |

验证交换机和绑定关系时，进入 `Exchanges` 页面，点击具体交换机，查看 `Bindings` 区域。需要确认以下内容：

| 验证项           | 正确表现                                      |
| ---------------- | --------------------------------------------- |
| Exchange 类型    | Direct、Topic、Fanout、Headers 与代码声明一致 |
| Exchange Durable | 重要业务交换机应为 durable                    |
| Binding 目标     | 队列名称正确                                  |
| Binding Key      | Direct 精确匹配，Topic 通配符符合预期         |
| vhost            | 与应用连接的 vhost 一致                       |

验证队列参数时，进入队列详情页，重点检查以下参数：

| 参数                        | 说明                                    |
| --------------------------- | --------------------------------------- |
| Durable                     | 队列是否持久化                          |
| Arguments                   | 是否包含死信交换机、TTL、最大长度等参数 |
| `x-dead-letter-exchange`    | 死信交换机是否正确                      |
| `x-dead-letter-routing-key` | 死信 Routing Key 是否正确               |
| `x-message-ttl`             | TTL 延迟时间是否正确                    |
| Consumers                   | 消费者数量是否符合配置                  |

如果需要手动发布一条测试消息，可以进入交换机详情页，在 `Publish message` 区域填写 Routing Key 和 Payload。

示例 Payload：

```json
{
  "messageId": "MANUAL_TEST_202604300001",
  "orderNo": "ORDER_MANUAL_TEST_001",
  "userId": 10001,
  "amount": 99.90,
  "status": "CREATED",
  "createTime": "2026-04-30T10:00:00"
}
```

如果项目使用 `Jackson2JsonMessageConverter`，手动发送时建议设置消息属性：

| 属性            | 值                 |
| --------------- | ------------------ |
| `content_type`  | `application/json` |
| `delivery_mode` | `2`                |

控制台验证建议如下。

| 建议                    | 说明                                                 |
| ----------------------- | ---------------------------------------------------- |
| 先看 vhost              | 很多问题是应用和控制台看的 vhost 不一致              |
| 再看 Binding            | 消息不可路由多数是 Binding Key 或 Routing Key 错误   |
| 观察 Ready              | Ready 不下降通常说明消费者未启动或消费失败           |
| 观察 Unacked            | Unacked 不下降通常说明手动确认未执行或业务阻塞       |
| 检查 Arguments          | 死信、TTL、延迟队列问题通常与队列参数有关            |
| 不要随意 Purge 生产队列 | 清空队列会删除未消费消息，本地可以使用，生产谨慎使用 |

### 单元测试设计

单元测试用于验证生产者、消费者和业务服务的核心逻辑是否符合预期。RabbitMQ 测试可以分为纯单元测试和集成测试：纯单元测试使用 Mockito 模拟 `RabbitTemplate`、`Channel`、业务 Service；集成测试连接真实 RabbitMQ，验证消息是否能够发送、路由和接收。

建议测试范围如下。

| 测试对象    | 测试重点                                                     |
| ----------- | ------------------------------------------------------------ |
| Producer    | 是否调用正确 Exchange、Routing Key、消息属性、CorrelationData |
| Consumer    | 参数异常是否拒绝，成功处理是否 ack，失败是否 nack            |
| Service     | 业务状态判断、幂等处理、异常分支                             |
| Config      | 交换机、队列、绑定是否能正常声明                             |
| Integration | 真实 RabbitMQ 下消息是否能发送和接收                         |

测试依赖建议如下。

文件位置：`pom.xml`

```xml
<!-- Spring Boot Test：包含 JUnit、Mockito、AssertJ 等测试能力 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Spring Rabbit Test：提供 RabbitMQ 测试支持 -->
<dependency>
    <groupId>org.springframework.amqp</groupId>
    <artifactId>spring-rabbit-test</artifactId>
    <scope>test</scope>
</dependency>
```

测试环境配置建议单独放到 `application-test.yml`。

文件位置：`src/test/resources/application-test.yml`

```yaml
spring:
  rabbitmq:
    # 测试环境 RabbitMQ 地址
    host: localhost
    port: 5672
    username: admin
    password: admin123
    virtual-host: /dev

    # 测试生产者确认
    publisher-confirm-type: correlated

    # 测试消息返回机制
    publisher-returns: true

    template:
      mandatory: true

    listener:
      simple:
        acknowledge-mode: manual
        prefetch: 1
        concurrency: 1
        max-concurrency: 1
        default-requeue-rejected: false
```

生产者单元测试示例。该测试不连接真实 RabbitMQ，只验证生产者是否调用了 `RabbitTemplate` 的发送方法。

文件位置：`src/test/java/io/github/atengk/producer/ReliableOrderProducerTest.java`

```java
package io.github.atengk.producer;

import io.github.atengk.message.OrderMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 可靠订单消息生产者测试
 *
 * @author Ateng
 * @since 2026-04-30
 */
@ExtendWith(MockitoExtension.class)
class ReliableOrderProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ReliableOrderProducer reliableOrderProducer;

    /**
     * 测试订单消息发送
     */
    @Test
    void shouldSendReliableOrderMessage() {
        OrderMessage orderMessage = OrderMessage.builder()
                .orderNo("ORDER_TEST_202604300001")
                .userId(10001L)
                .amount(new BigDecimal("99.90"))
                .status("CREATED")
                .build();

        reliableOrderProducer.sendReliableOrderMessage(orderMessage);

        verify(rabbitTemplate, times(1)).convertAndSend(
                anyString(),
                anyString(),
                eq(orderMessage),
                any(MessagePostProcessor.class),
                any(CorrelationData.class)
        );
    }

}
```

消费者单元测试示例。该测试通过 Mock `Channel` 验证消费成功后是否执行 `basicAck`。

文件位置：`src/test/java/io/github/atengk/consumer/ReliableOrderConsumerTest.java`

```java
package io.github.atengk.consumer;

import com.rabbitmq.client.Channel;
import io.github.atengk.message.OrderMessage;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 可靠订单消息消费者测试
 *
 * @author Ateng
 * @since 2026-04-30
 */
class ReliableOrderConsumerTest {

    /**
     * 测试订单消息消费成功后确认消息
     *
     * @throws Exception 测试异常
     */
    @Test
    void shouldAckWhenConsumeSuccess() throws Exception {
        ReliableOrderConsumer consumer = new ReliableOrderConsumer();
        Channel channel = mock(Channel.class);

        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setDeliveryTag(1001L);

        Message message = new Message("{}".getBytes(StandardCharsets.UTF_8), messageProperties);

        OrderMessage orderMessage = OrderMessage.builder()
                .messageId("MSG_TEST_001")
                .orderNo("ORDER_TEST_202604300001")
                .userId(10001L)
                .amount(new BigDecimal("99.90"))
                .status("CREATED")
                .build();

        consumer.consumeOrderMessage(orderMessage, message, channel);

        verify(channel, times(1)).basicAck(1001L, false);
    }

}
```

如果 `ReliableOrderConsumer` 的构造方法依赖其他 Service，应使用 Mockito 注入依赖，而不是直接 `new ReliableOrderConsumer()`。示例：

```java
OrderBusinessService orderBusinessService = mock(OrderBusinessService.class);
OrderCreatedConsumer consumer = new OrderCreatedConsumer(orderBusinessService);
```

RabbitMQ 集成测试示例。该测试连接本地 RabbitMQ，动态声明测试交换机和测试队列，发送消息后再接收验证。

文件位置：`src/test/java/io/github/atengk/integration/RabbitMqIntegrationTest.java`

```java
package io.github.atengk.integration;

import cn.hutool.core.util.IdUtil;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RabbitMQ 集成测试
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootTest
@ActiveProfiles("test")
class RabbitMqIntegrationTest {

    private static final String TEST_EXCHANGE = "test.integration.direct.exchange";

    private static final String TEST_QUEUE = "test.integration.queue";

    private static final String TEST_ROUTING_KEY = "test.integration";

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 测试消息发送和接收
     */
    @Test
    void shouldSendAndReceiveMessage() {
        DirectExchange exchange = ExchangeBuilder
                .directExchange(TEST_EXCHANGE)
                .durable(false)
                .build();

        Queue queue = QueueBuilder
                .nonDurable(TEST_QUEUE)
                .build();

        Binding binding = BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(TEST_ROUTING_KEY);

        amqpAdmin.declareExchange(exchange);
        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareBinding(binding);

        String content = "hello-" + IdUtil.fastSimpleUUID();

        rabbitTemplate.convertAndSend(TEST_EXCHANGE, TEST_ROUTING_KEY, content);

        Object received = rabbitTemplate.receiveAndConvert(TEST_QUEUE, 5000);

        assertThat(received).isEqualTo(content);
    }

}
```

集成测试执行命令如下。

```bash
# 确保 RabbitMQ 已启动后执行测试
mvn test -Dtest=RabbitMqIntegrationTest
```

单元测试设计建议如下。

| 建议                                  | 说明                                             |
| ------------------------------------- | ------------------------------------------------ |
| Producer 测试不必连接真实 MQ          | 使用 Mockito 验证发送参数即可                    |
| Consumer 测试重点验证 ack/nack/reject | Mock `Channel` 和业务 Service                    |
| Service 测试重点验证业务状态          | 例如订单待支付才关闭，已支付跳过                 |
| 集成测试使用独立测试队列              | 避免影响开发队列和生产队列                       |
| 测试队列使用清晰前缀                  | 例如 `test.` 开头                                |
| 测试后清理资源                        | 临时队列可以使用 non-durable，或在测试结束后删除 |
| 不依赖消费顺序                        | 并发消费下顺序不能作为稳定断言                   |

### 消息异常场景验证

异常场景验证用于确认系统在消息发送失败、消息不可路由、消费失败、重复消费、死信转发、延迟到期等情况下是否符合预期。生产环境中的 RabbitMQ 问题通常不是正常链路，而是异常链路没有设计好。

建议至少验证以下异常场景。

| 场景             | 验证目标                                 |
| ---------------- | ---------------------------------------- |
| RabbitMQ 未启动  | 应用是否启动失败或发送失败日志是否清晰   |
| 交换机不存在     | 生产者确认是否返回失败                   |
| Routing Key 错误 | ReturnsCallback 是否触发                 |
| 消费者异常       | 是否进入 `basicNack`、重试队列或死信队列 |
| 消息格式错误     | 是否拒绝消息，避免无限重试               |
| 重复消息         | 幂等逻辑是否生效                         |
| 消费者未 ack     | `Unacked` 是否持续存在                   |
| 延迟消息         | 是否按预期时间进入业务队列               |
| 死信消息         | 是否进入兜底队列                         |
| 队列参数不一致   | 应用启动是否报 `PRECONDITION_FAILED`     |

验证消息不可路由。可以故意发送一个错误 Routing Key，检查 ReturnsCallback 是否打印路由失败日志。

```java
rabbitTemplate.convertAndSend(
        "order.direct.exchange",
        "order.not.exists",
        orderMessage,
        message -> {
            message.getMessageProperties().setMessageId(orderMessage.getMessageId());
            return message;
        }
);
```

预期结果如下。

| 验证项          | 预期结果                           |
| --------------- | ---------------------------------- |
| ConfirmCallback | 交换机存在时通常 `ack=true`        |
| ReturnsCallback | 触发返回回调                       |
| RabbitMQ 队列   | 目标队列没有新增消息               |
| 应用日志        | 出现 `NO_ROUTE` 或路由失败相关日志 |

验证消费异常。可以在消费者业务方法中临时抛出异常。

```java
private void handleOrderBusiness(OrderMessage orderMessage) {
    log.info("模拟订单消费异常，orderNo={}", orderMessage.getOrderNo());
    throw new RuntimeException("模拟消费失败");
}
```

预期结果如下。

| 配置                      | 预期结果                                         |
| ------------------------- | ------------------------------------------------ |
| `basicNack(false, false)` | 消息不重新入队                                   |
| 业务队列配置了 DLX        | 消息进入死信或重试队列                           |
| 业务队列未配置 DLX        | 消息可能被丢弃                                   |
| 控制台                    | 业务队列 Ready 不应无限增长                      |
| 日志                      | 应记录 messageId、orderNo、deliveryTag、异常原因 |

验证重复消费。可以发送相同 `messageId` 的消息两次，检查消费者幂等逻辑是否跳过第二次处理。

```bash
curl -X POST "http://localhost:8080/api/mq/send/order" \
  -H "Content-Type: application/json" \
  -d '{
    "messageId": "MSG_DUPLICATE_TEST_001",
    "orderNo": "ORDER_DUPLICATE_TEST_001",
    "userId": 10001,
    "amount": 99.90,
    "status": "CREATED"
  }'

curl -X POST "http://localhost:8080/api/mq/send/order" \
  -H "Content-Type: application/json" \
  -d '{
    "messageId": "MSG_DUPLICATE_TEST_001",
    "orderNo": "ORDER_DUPLICATE_TEST_001",
    "userId": 10001,
    "amount": 99.90,
    "status": "CREATED"
  }'
```

预期结果如下。

| 验证项     | 预期结果                               |
| ---------- | -------------------------------------- |
| 第一次消费 | 正常执行业务                           |
| 第二次消费 | 识别为重复消息                         |
| 业务数据   | 不应重复扣库存、重复发券、重复关闭订单 |
| 消息确认   | 重复消息也应 `basicAck`，避免继续投递  |

验证死信转发。可以发送一条会触发消费异常的消息，并确认它进入死信队列。

```bash
curl -X POST "http://localhost:8080/api/mq/send/order" \
  -H "Content-Type: application/json" \
  -d '{
    "messageId": "MSG_DLX_TEST_001",
    "orderNo": "ORDER_DLX_TEST_001",
    "userId": 10001,
    "amount": 99.90,
    "status": "ERROR_TEST"
  }'
```

预期检查点如下。

| 检查位置     | 预期结果               |
| ------------ | ---------------------- |
| 应用日志     | 消费失败日志正常打印   |
| 业务队列     | 消息不应一直卡在 Ready |
| 死信队列     | 出现失败消息           |
| 消息 Headers | 能看到 `x-death` 信息  |
| 兜底消费者   | 能记录失败原因并 ack   |

验证延迟消息。发送一条延迟 10 秒消息，观察消息是否延迟进入业务队列。

```bash
curl -X POST "http://localhost:8080/api/mq/plugin-delay/order/send?delayMillis=10000" \
  -H "Content-Type: application/json" \
  -d '{
    "messageId": "MSG_DELAY_TEST_001",
    "orderNo": "ORDER_DELAY_TEST_001",
    "userId": 10001,
    "amount": 99.90,
    "status": "WAIT_PAY"
  }'
```

预期结果如下。

| 时间点         | 预期结果                         |
| -------------- | -------------------------------- |
| 发送后立即查看 | 消费者不应马上收到消息           |
| 约 10 秒后     | 消费者收到消息并处理             |
| 应用日志       | 打印延迟消息消费日志             |
| 业务处理       | 查询订单最新状态后再决定是否关闭 |

验证消费者未确认场景。可以临时注释 `basicAck`，发送一条消息后观察控制台。

```java
// channel.basicAck(deliveryTag, false);
```

预期结果如下。

| 指标         | 预期结果                 |
| ------------ | ------------------------ |
| `Unacked`    | 消息进入未确认状态       |
| `Ready`      | 消息不在待消费状态       |
| 消费者断开后 | 未确认消息重新回到 Ready |
| 应用日志     | 没有确认成功日志         |

该验证只允许在本地开发环境进行，完成后必须恢复 `basicAck`。

异常场景验证建议如下。

| 建议                     | 说明                                               |
| ------------------------ | -------------------------------------------------- |
| 每个异常场景单独验证     | 不要同时制造多个异常，避免判断混乱                 |
| 验证前记录队列初始状态   | 方便对比 Ready、Unacked、Total 变化                |
| 验证后清理测试消息       | 本地可以 Purge 测试队列，生产禁止随意清空          |
| 重要异常要有日志         | 必须包含 messageId、业务主键、exchange、routingKey |
| 死信队列必须监控         | 死信数量增长代表业务异常或外部依赖异常             |
| 重复消费必须验证         | RabbitMQ 不保证业务只处理一次，幂等必须测试        |
| 延迟业务必须验证状态判断 | 订单超时关闭等场景必须查询最新状态                 |

完成测试与验证后，RabbitMQ 功能才算具备基本上线条件。最低要求是：正常发送和消费可用，消息不可路由能发现，消费失败有明确流转路径，重复消息不会造成业务错误，死信队列可观测，关键业务具备补偿能力。

## 部署与运行

本章节用于说明 RabbitMQ 服务和 Spring Boot 应用在开发、测试、生产环境中的部署与运行方式，包括 Docker 部署 RabbitMQ、Spring Boot 配置隔离、开发环境与生产环境差异、启动检查和连通性验证。该章节对应上传大纲中的“部署与运行”部分。

RabbitMQ 部署与运行的核心目标是保证应用能够稳定连接 Broker，并且不同环境之间的账号、vhost、交换机、队列、确认策略、消费并发和告警配置互不干扰。开发环境可以追求快速启动和快速验证，生产环境则必须关注安全、持久化、权限隔离、监控和故障恢复。

### Docker 部署 RabbitMQ

Docker 部署适合本地开发、测试环境和小型验证环境。通过 Docker 可以快速启动 RabbitMQ 服务，并暴露 AMQP 端口和管理控制台端口。

RabbitMQ 常用端口如下。

| 端口    | 说明                                                   |
| ------- | ------------------------------------------------------ |
| `5672`  | AMQP 协议端口，Spring Boot 应用通过该端口连接 RabbitMQ |
| `15672` | RabbitMQ Management 管理控制台端口                     |
| `25672` | RabbitMQ 节点间通信端口，单节点开发环境通常不需要暴露  |
| `4369`  | Erlang EPMD 端口，集群环境可能需要                     |

本地快速启动 RabbitMQ。

```bash
# 拉取带管理控制台的 RabbitMQ 镜像
docker pull rabbitmq:3-management

# 启动 RabbitMQ 单节点容器
docker run -d \
  --name rabbitmq \
  --hostname rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=admin \
  -e RABBITMQ_DEFAULT_PASS=admin123 \
  -e RABBITMQ_DEFAULT_VHOST=/dev \
  rabbitmq:3-management
```

命令参数说明如下。

| 参数                     | 说明                                   |
| ------------------------ | -------------------------------------- |
| `--name rabbitmq`        | 容器名称，便于后续启动、停止、查看日志 |
| `--hostname rabbitmq`    | RabbitMQ 节点主机名                    |
| `-p 5672:5672`           | 暴露 AMQP 连接端口                     |
| `-p 15672:15672`         | 暴露管理控制台端口                     |
| `RABBITMQ_DEFAULT_USER`  | 初始化默认用户                         |
| `RABBITMQ_DEFAULT_PASS`  | 初始化默认密码                         |
| `RABBITMQ_DEFAULT_VHOST` | 初始化默认虚拟主机                     |
| `rabbitmq:3-management`  | 带管理控制台插件的镜像                 |

推荐在项目中使用 Docker Compose 管理 RabbitMQ，便于团队成员保持一致的本地环境。

文件位置：`docker-compose.yml`

```yaml
services:
  rabbitmq:
    # 使用带管理控制台的 RabbitMQ 镜像
    image: rabbitmq:3-management
    container_name: rabbitmq
    hostname: rabbitmq
    restart: unless-stopped
    ports:
      # AMQP 协议端口，Spring Boot 应用通过该端口连接
      - "5672:5672"
      # 管理控制台端口，浏览器访问 http://localhost:15672
      - "15672:15672"
    environment:
      # 本地开发用户
      RABBITMQ_DEFAULT_USER: admin
      # 本地开发密码，生产环境不要明文写在 Compose 文件中
      RABBITMQ_DEFAULT_PASS: admin123
      # 本地开发虚拟主机
      RABBITMQ_DEFAULT_VHOST: /dev
    volumes:
      # RabbitMQ 数据持久化目录
      - rabbitmq_data:/var/lib/rabbitmq
    networks:
      - rabbitmq_net

volumes:
  rabbitmq_data:
    driver: local

networks:
  rabbitmq_net:
    driver: bridge
```

启动、停止和查看日志。

```bash
# 启动 RabbitMQ
docker compose up -d

# 查看容器状态
docker compose ps

# 查看 RabbitMQ 日志
docker logs -f rabbitmq

# 停止 RabbitMQ
docker compose down

# 停止并删除数据卷，仅建议本地开发环境使用
docker compose down -v
```

如果需要进入容器内部执行 RabbitMQ 命令，可以使用以下方式。

```bash
# 进入 RabbitMQ 容器
docker exec -it rabbitmq bash

# 查看 RabbitMQ 状态
rabbitmqctl status

# 查看用户列表
rabbitmqctl list_users

# 查看 vhost 列表
rabbitmqctl list_vhosts

# 查看队列列表
rabbitmqctl list_queues -p /dev name messages_ready messages_unacknowledged consumers

# 查看交换机列表
rabbitmqctl list_exchanges -p /dev name type durable

# 查看绑定关系列表
rabbitmqctl list_bindings -p /dev
```

如果需要启用延迟消息插件，可以执行以下命令。

```bash
# 启用延迟消息插件
docker exec -it rabbitmq rabbitmq-plugins enable rabbitmq_delayed_message_exchange

# 查看插件列表
docker exec -it rabbitmq rabbitmq-plugins list
```

生产环境如果使用 Docker 部署，至少需要注意以下事项。

| 项目     | 建议                                                         |
| -------- | ------------------------------------------------------------ |
| 数据目录 | 必须挂载持久化目录或数据卷                                   |
| 用户密码 | 不要使用默认密码，不要明文提交到代码仓库                     |
| vhost    | 按环境或业务系统隔离                                         |
| 端口暴露 | 管理控制台不要直接暴露到公网                                 |
| 镜像版本 | 固定明确版本，不建议生产使用浮动标签                         |
| 监控     | 接入 RabbitMQ 指标监控和告警                                 |
| 备份     | 重要环境需要备份定义、配置和必要数据                         |
| 集群     | 生产高可用场景应评估集群、Quorum Queue、镜像策略或云托管方案 |

### Spring Boot 配置隔离

Spring Boot 配置隔离用于保证开发、测试、生产环境使用不同的 RabbitMQ 地址、账号、密码、vhost、确认策略和消费并发。开发环境可以使用本地 Docker，测试环境使用测试 RabbitMQ，生产环境使用独立生产集群或云托管 RabbitMQ。

推荐配置文件结构如下。

```text
src/main/resources
├── application.yml
├── application-dev.yml
├── application-test.yml
└── application-prod.yml
```

`application.yml` 只放通用配置和 profile 选择。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  application:
    # 应用名称，建议与服务注册、日志链路、监控名称保持一致
    name: rabbitmq-demo

  profiles:
    # 默认开发环境，生产部署时通过启动参数覆盖
    active: dev

management:
  endpoints:
    web:
      exposure:
        # 暴露健康检查和应用信息
        include: health,info
```

开发环境配置。

文件位置：`src/main/resources/application-dev.yml`

```yaml
spring:
  rabbitmq:
    # 本地 RabbitMQ 地址
    host: localhost
    port: 5672

    # 本地开发账号
    username: admin
    password: admin123

    # 开发环境 vhost
    virtual-host: /dev

    # 开发环境连接超时时间
    connection-timeout: 10s

    # 开启生产者确认
    publisher-confirm-type: correlated

    # 开启不可路由消息返回
    publisher-returns: true

    template:
      # 不可路由消息返回给生产者
      mandatory: true
      retry:
        # 开发环境开启发送重试，便于处理短暂连接抖动
        enabled: true
        initial-interval: 1s
        max-attempts: 3
        multiplier: 2
        max-interval: 5s

    listener:
      simple:
        # 开发环境并发较低，便于观察日志
        concurrency: 1
        max-concurrency: 3
        prefetch: 10
        acknowledge-mode: manual
        default-requeue-rejected: false
```

测试环境配置。

文件位置：`src/main/resources/application-test.yml`

```yaml
spring:
  rabbitmq:
    # 测试环境 RabbitMQ 地址，实际值建议由环境变量注入
    host: ${RABBITMQ_HOST:rabbitmq-test}
    port: ${RABBITMQ_PORT:5672}

    # 测试环境账号
    username: ${RABBITMQ_USERNAME:rabbit_test_user}
    password: ${RABBITMQ_PASSWORD:rabbit_test_password}

    # 测试环境 vhost
    virtual-host: ${RABBITMQ_VHOST:/test}

    publisher-confirm-type: correlated
    publisher-returns: true

    template:
      mandatory: true
      retry:
        enabled: true
        initial-interval: 1s
        max-attempts: 3
        multiplier: 2
        max-interval: 5s

    listener:
      simple:
        # 测试环境可以模拟一定并发
        concurrency: 2
        max-concurrency: 6
        prefetch: 10
        acknowledge-mode: manual
        default-requeue-rejected: false
```

生产环境配置。

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  rabbitmq:
    # 生产环境建议通过环境变量或配置中心注入
    host: ${RABBITMQ_HOST}
    port: ${RABBITMQ_PORT:5672}

    # 生产账号密码不得写死在配置文件中
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}

    # 生产环境 vhost
    virtual-host: ${RABBITMQ_VHOST:/prod}

    # 生产环境连接超时
    connection-timeout: 10s

    # 生产环境必须开启生产者确认
    publisher-confirm-type: correlated

    # 生产环境建议开启消息返回
    publisher-returns: true

    template:
      # 不可路由消息必须返回，便于发现绑定错误
      mandatory: true
      retry:
        enabled: true
        initial-interval: 1s
        max-attempts: 3
        multiplier: 2
        max-interval: 10s

    listener:
      simple:
        # 生产并发需要结合压测结果配置
        concurrency: ${RABBITMQ_CONSUMER_CONCURRENCY:3}
        max-concurrency: ${RABBITMQ_CONSUMER_MAX_CONCURRENCY:10}

        # 控制消费者未确认消息数量，避免单个消费者积压过多消息
        prefetch: ${RABBITMQ_CONSUMER_PREFETCH:20}

        # 重要业务使用手动确认
        acknowledge-mode: manual

        # 消费异常不默认重新入队，避免无限失败循环
        default-requeue-rejected: false
```

生产启动时通过环境变量注入敏感配置。

```bash
# 设置生产环境变量
export SPRING_PROFILES_ACTIVE=prod
export RABBITMQ_HOST=10.0.0.10
export RABBITMQ_PORT=5672
export RABBITMQ_USERNAME=order_prod_user
export RABBITMQ_PASSWORD='替换为生产密码'
export RABBITMQ_VHOST=/prod
export RABBITMQ_CONSUMER_CONCURRENCY=3
export RABBITMQ_CONSUMER_MAX_CONCURRENCY=10
export RABBITMQ_CONSUMER_PREFETCH=20

# 启动应用
java -jar rabbitmq-demo.jar
```

如果使用 Docker 部署 Spring Boot 应用，可以在容器启动时注入环境变量。

```bash
docker run -d \
  --name rabbitmq-demo \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e RABBITMQ_HOST=10.0.0.10 \
  -e RABBITMQ_PORT=5672 \
  -e RABBITMQ_USERNAME=order_prod_user \
  -e RABBITMQ_PASSWORD='替换为生产密码' \
  -e RABBITMQ_VHOST=/prod \
  rabbitmq-demo:1.0.0
```

配置隔离建议如下。

| 建议                   | 说明                                             |
| ---------------------- | ------------------------------------------------ |
| 不同环境使用不同 vhost | 避免开发、测试、生产消息互相污染                 |
| 账号按环境隔离         | 不同环境使用不同 RabbitMQ 用户                   |
| 生产密码不写入代码仓库 | 使用环境变量、配置中心或 Secret 管理             |
| 消费并发按环境区分     | 开发低并发，生产按压测结果配置                   |
| 生产必须开启确认机制   | 包括生产者确认、消息返回和消费者手动确认         |
| 队列名称保持稳定       | 生产环境修改队列名称可能导致消息丢失或消费者断开 |
| 配置变更要走发布流程   | RabbitMQ 队列参数变更不能随意上线                |

### 开发环境与生产环境差异

开发环境和生产环境的 RabbitMQ 使用目标不同。开发环境关注快速验证和问题复现，生产环境关注可靠性、安全性、可观测性、性能和故障恢复。

主要差异如下。

| 项目       | 开发环境       | 生产环境                    |
| ---------- | -------------- | --------------------------- |
| 部署方式   | Docker 单节点  | 集群、云托管或高可用部署    |
| 用户账号   | 简单账号即可   | 独立业务账号，最小权限      |
| 密码管理   | 可本地配置     | 环境变量、配置中心或 Secret |
| vhost      | `/dev`         | `/prod` 或按业务系统拆分    |
| 消费并发   | 较低，便于调试 | 按压测和下游能力配置        |
| 消息确认   | 可逐步开启     | 必须开启可靠确认            |
| 消息持久化 | 建议开启       | 重要业务必须开启            |
| 死信队列   | 可选           | 重要业务必须配置            |
| 延迟插件   | 本地可按需启用 | 生产必须评估版本和稳定性    |
| 监控告警   | 简单观察控制台 | 必须接入监控和告警          |
| 日志级别   | 可使用 DEBUG   | 通常 INFO，异常 ERROR       |
| 清空队列   | 可用于测试     | 禁止随意 Purge 生产队列     |
| 数据保留   | 可删除重建     | 需要备份、审计和恢复策略    |

生产环境至少需要具备以下能力。

| 能力           | 说明                                             |
| -------------- | ------------------------------------------------ |
| 连接监控       | 监控连接数、通道数、消费者数量                   |
| 队列监控       | 监控 Ready、Unacked、Total、消息速率             |
| 死信监控       | 死信队列有消息时告警                             |
| 消费延迟监控   | 队列堆积或消费速率下降时告警                     |
| 生产者失败监控 | ConfirmCallback 失败、ReturnsCallback 失败要告警 |
| 消费者失败监控 | 消费异常、重试耗尽、兜底失败要告警               |
| 权限审计       | 控制用户对 vhost 的 configure、write、read 权限  |
| 补偿机制       | 支持失败消息重放、人工处理和定时补偿             |

生产环境不建议执行以下操作。

| 操作                        | 风险                           |
| --------------------------- | ------------------------------ |
| 使用默认 `guest/guest` 账号 | 安全风险高                     |
| 直接暴露管理控制台到公网    | 容易被扫描和攻击               |
| 随意删除队列                | 未消费消息会丢失               |
| 随意 Purge 队列             | 会清空 Ready 消息              |
| 修改已有队列参数            | 可能触发 `PRECONDITION_FAILED` |
| 无限重新入队                | 异常消息会造成死循环           |
| 未压测就提高并发            | 可能打垮数据库或第三方系统     |
| 不配置死信队列              | 异常消息无法追踪和补偿         |

生产环境发布前检查清单如下。

| 检查项           | 要求                               |
| ---------------- | ---------------------------------- |
| RabbitMQ 地址    | 指向生产 RabbitMQ                  |
| vhost            | 使用生产 vhost                     |
| 用户权限         | 仅授予必要权限                     |
| 生产者确认       | 已开启                             |
| 消息返回机制     | 已开启                             |
| 消费者手动确认   | 重要业务已开启                     |
| Exchange / Queue | 均为 durable                       |
| 消息持久化       | 重要消息已设置 persistent          |
| 死信队列         | 重要队列已配置                     |
| 幂等消费         | 重复消息不会导致业务错误           |
| 监控告警         | 队列堆积、死信、连接异常已告警     |
| 补偿入口         | 失败消息可查询、可重放、可人工处理 |

### 启动与连通性检查

启动与连通性检查用于确认 RabbitMQ 服务、Spring Boot 应用、交换机、队列、绑定关系和消费者均处于正常状态。部署完成后，不应只看应用进程是否启动，还要检查应用是否真正连接到 RabbitMQ，消费者是否在线，消息能否正常发送和消费。

RabbitMQ 服务检查命令如下。

```bash
# 查看 RabbitMQ 容器状态
docker ps | grep rabbitmq

# 查看 RabbitMQ 服务日志
docker logs --tail=200 rabbitmq

# 查看 RabbitMQ 节点状态
docker exec -it rabbitmq rabbitmqctl status

# 查看 vhost
docker exec -it rabbitmq rabbitmqctl list_vhosts

# 查看用户权限
docker exec -it rabbitmq rabbitmqctl list_permissions -p /dev
```

队列与消费者检查命令如下。

```bash
# 查看队列消息和消费者数量
docker exec -it rabbitmq rabbitmqctl list_queues -p /dev \
  name messages_ready messages_unacknowledged messages consumers

# 查看交换机
docker exec -it rabbitmq rabbitmqctl list_exchanges -p /dev name type durable

# 查看绑定关系
docker exec -it rabbitmq rabbitmqctl list_bindings -p /dev
```

Spring Boot 应用启动检查命令如下。

```bash
# 使用 dev 环境启动
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 使用 jar 包启动
java -jar rabbitmq-demo.jar --spring.profiles.active=dev

# 查看应用健康检查
curl -X GET "http://localhost:8080/actuator/health"
```

如果需要在应用启动后主动检查 RabbitMQ 连接，可以增加一个本地或内部接口。该接口通过 `RabbitTemplate` 读取连接工厂信息，并返回基础状态。

文件位置：`src/main/java/io/github/atengk/controller/RabbitMqHealthController.java`

```java
package io.github.atengk.controller;

import cn.hutool.core.map.MapUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RabbitMQ 连通性检查接口
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class RabbitMqHealthController {

    private final ConnectionFactory connectionFactory;

    /**
     * 检查 RabbitMQ 连接状态
     *
     * @return RabbitMQ 连接信息
     */
    @GetMapping("/api/mq/health")
    public ResponseEntity<Object> checkRabbitMqHealth() {
        try {
            org.springframework.amqp.rabbit.connection.Connection connection = connectionFactory.createConnection();

            boolean open = connection.isOpen();

            log.info("RabbitMQ连通性检查完成，open={}", open);

            return ResponseEntity.ok(MapUtil.builder()
                    .put("open", open)
                    .put("localPort", connection.getLocalPort())
                    .build());
        } catch (Exception e) {
            log.error("RabbitMQ连通性检查失败", e);

            return ResponseEntity.internalServerError().body(MapUtil.builder()
                    .put("open", false)
                    .put("message", e.getMessage())
                    .build());
        }
    }

}
```

调用检查接口。

```bash
curl -X GET "http://localhost:8080/api/mq/health"
```

预期返回示例。

```json
{
  "open": true,
  "localPort": 54321
}
```

发送和消费链路检查建议使用一条测试消息完成。

```bash
# 发送测试订单消息
curl -X POST "http://localhost:8080/api/mq/send/order" \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "ORDER_HEALTH_CHECK_001",
    "userId": 10001,
    "amount": 1.00,
    "status": "CREATED"
  }'
```

检查项如下。

| 检查位置        | 预期结果                            |
| --------------- | ----------------------------------- |
| 生产者日志      | 打印消息发送成功日志                |
| ConfirmCallback | 打印 `ack=true` 日志                |
| ReturnsCallback | 不应出现不可路由日志                |
| RabbitMQ 控制台 | 队列 Ready 短暂变化或直接被消费     |
| 消费者日志      | 打印消费成功日志                    |
| 队列指标        | Ready 和 Unacked 最终归零或保持稳定 |
| 死信队列        | 不应新增异常消息                    |

启动与连通性检查建议如下。

| 建议                   | 说明                                                   |
| ---------------------- | ------------------------------------------------------ |
| 每次部署后发送测试消息 | 验证完整生产消费链路                                   |
| 检查 vhost 是否正确    | 应用连接的 vhost 必须与控制台查看一致                  |
| 检查消费者数量         | 关键队列 Consumers 不能为 0                            |
| 检查不可路由日志       | ReturnsCallback 出现说明 Binding 或 Routing Key 有问题 |
| 检查死信队列           | 部署后死信增长通常代表消费异常                         |
| 检查 Unacked           | 长时间不下降说明 ack 或业务处理存在问题                |

## 常见问题处理

本章节用于整理 RabbitMQ 开发和运行过程中常见问题的现象、原因、排查方式和处理建议，包括消息发送失败、消息重复消费、消息堆积、消费者连接异常和队列参数不一致错误。该章节对应上传大纲中的“常见问题处理”部分。

排查 RabbitMQ 问题时，建议按照以下顺序进行：

```text
先看应用日志
  -> 再看 RabbitMQ 控制台
  -> 再看连接和消费者
  -> 再看交换机、队列、绑定关系
  -> 再看确认、返回、死信和重试配置
```

### 消息发送失败

消息发送失败表示生产者调用 `RabbitTemplate` 后，消息没有按预期到达 RabbitMQ Broker 或没有被正确路由到队列。发送失败可能发生在连接阶段、认证阶段、交换机路由阶段、确认回调阶段或消息转换阶段。

常见现象如下。

| 现象                             | 说明                                          |
| -------------------------------- | --------------------------------------------- |
| 应用启动时报连接异常             | RabbitMQ 地址、端口、账号或 vhost 错误        |
| 发送接口返回异常                 | `RabbitTemplate` 发送时连接失败或消息转换失败 |
| ConfirmCallback 返回 `ack=false` | Broker 未确认该消息                           |
| ReturnsCallback 被触发           | 消息到达交换机，但无法路由到队列              |
| 控制台队列无消息                 | 交换机、Routing Key 或 Binding 配置错误       |
| 消费者无日志                     | 消息未进入消费者监听的队列                    |

常见原因和处理方式如下。

| 原因             | 排查方式                            | 处理建议                         |
| ---------------- | ----------------------------------- | -------------------------------- |
| RabbitMQ 未启动  | `docker ps`、`docker logs rabbitmq` | 启动 RabbitMQ 服务               |
| 端口配置错误     | 检查 `spring.rabbitmq.port`         | AMQP 端口应为 `5672`             |
| 账号密码错误     | 应用日志出现 `ACCESS_REFUSED`       | 修改账号密码或重新授权           |
| vhost 不存在     | 控制台 Admin 查看 vhost             | 创建 vhost 并授权                |
| 交换机不存在     | 控制台 Exchanges 查看               | 检查配置类是否声明 Exchange      |
| Routing Key 错误 | 查看 ReturnsCallback 日志           | 修正生产者 Routing Key           |
| 队列未绑定       | 控制台查看 Bindings                 | 添加正确 Binding                 |
| 消息转换失败     | 查看序列化异常                      | 检查 MessageConverter 和消息实体 |
| Broker 权限不足  | 查看用户权限                        | 授予 configure、write、read 权限 |

排查命令如下。

```bash
# 查看 RabbitMQ 容器是否运行
docker ps | grep rabbitmq

# 查看 RabbitMQ 日志
docker logs --tail=200 rabbitmq

# 查看 vhost
docker exec -it rabbitmq rabbitmqctl list_vhosts

# 查看指定 vhost 下交换机
docker exec -it rabbitmq rabbitmqctl list_exchanges -p /dev name type durable

# 查看指定 vhost 下队列
docker exec -it rabbitmq rabbitmqctl list_queues -p /dev name messages consumers

# 查看绑定关系
docker exec -it rabbitmq rabbitmqctl list_bindings -p /dev
```

Spring Boot 配置重点检查如下。

```yaml
spring:
  rabbitmq:
    # 确认地址和端口正确
    host: localhost
    port: 5672

    # 确认账号密码正确
    username: admin
    password: admin123

    # 确认 vhost 正确
    virtual-host: /dev

    # 生产者确认
    publisher-confirm-type: correlated

    # 消息返回
    publisher-returns: true

    template:
      # 不可路由消息返回
      mandatory: true
```

发送失败处理建议如下。

| 建议             | 说明                                              |
| ---------------- | ------------------------------------------------- |
| 开启生产者确认   | 使用 `publisher-confirm-type=correlated`          |
| 开启消息返回机制 | 使用 `publisher-returns=true` 和 `mandatory=true` |
| 日志记录完整参数 | 记录 messageId、exchange、routingKey、businessKey |
| 发送失败落库     | 重要消息发送失败应记录到消息表                    |
| 定时任务补偿     | 扫描发送失败消息并重新发送                        |
| 不要吞掉异常     | 发送失败必须暴露给日志、监控或补偿流程            |
| 部署后做链路验证 | 每次部署后发送一条测试消息确认路由正常            |

### 消息重复消费

消息重复消费表示同一条业务消息被消费者处理了多次。RabbitMQ 只能保证消息至少一次投递，无法从业务层面保证只处理一次。因此业务系统必须通过幂等设计解决重复消费问题。

常见现象如下。

| 现象                              | 说明                               |
| --------------------------------- | ---------------------------------- |
| 同一个订单被处理多次              | 订单关闭、发券、通知等逻辑重复执行 |
| 数据库出现重复记录                | 消费端没有唯一约束或幂等判断       |
| 消费日志中同一 messageId 多次出现 | 同一消息发生重投或生产者重复发送   |
| 消费成功但消息再次出现            | ack 失败、连接断开或消费者宕机     |
| 补偿任务重复发送                  | 生产端无法判断发送结果而重复投递   |

常见原因如下。

| 原因                | 说明                          |
| ------------------- | ----------------------------- |
| 消费成功但 ack 失败 | Broker 未收到确认，会重新投递 |
| 消费过程中应用宕机  | 未确认消息会重新回到队列      |
| 生产者补偿重发      | 发送结果不确定时可能再次发送  |
| 手动重放死信消息    | 人工或补偿任务重复投递        |
| 消息 ID 不唯一      | 无法识别重复消息              |
| 消费端无幂等控制    | 业务处理不检查是否已执行      |

处理重复消费的核心是幂等。推荐使用 `messageId` 或业务唯一键作为幂等键。

```text
mq:idempotent:{businessType}:{messageId}
```

Redis 幂等判断示例。

```java
Boolean success = stringRedisTemplate.opsForValue()
        .setIfAbsent("mq:idempotent:order:" + messageId, "PROCESSING", Duration.ofHours(2));

if (!Boolean.TRUE.equals(success)) {
    log.warn("检测到重复消息，直接跳过，messageId={}", messageId);
    channel.basicAck(deliveryTag, false);
    return;
}
```

数据库幂等建议增加唯一索引。

```sql
CREATE TABLE mq_consume_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息ID',
    business_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    business_key VARCHAR(128) NOT NULL COMMENT '业务主键',
    consume_status VARCHAR(32) NOT NULL COMMENT '消费状态',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_business_key (business_type, business_key)
) COMMENT='MQ消费日志表';
```

不同业务的幂等策略如下。

| 业务         | 幂等方式                                                |
| ------------ | ------------------------------------------------------- |
| 订单关闭     | `update ... where order_no = ? and status = 'WAIT_PAY'` |
| 发放优惠券   | 用户 ID + 优惠券 ID 建唯一索引                          |
| 支付成功处理 | 支付单号建唯一索引                                      |
| 库存扣减     | 订单号 + 商品 ID 建唯一扣减记录                         |
| 日志入库     | messageId 唯一索引                                      |
| 数据同步     | businessKey + dataVersion 控制覆盖顺序                  |

重复消费处理建议如下。

| 建议                         | 说明                                   |
| ---------------------------- | -------------------------------------- |
| 不要假设消息只消费一次       | RabbitMQ 业务语义应按至少一次投递设计  |
| 每条消息必须有唯一 ID        | 生产者生成 `messageId`                 |
| 消费端必须幂等               | 重复投递时不能重复执行业务副作用       |
| 重复消息也要 ack             | 已处理过的消息应直接确认，避免继续投递 |
| 强一致业务使用数据库唯一约束 | Redis 幂等不适合所有强一致场景         |
| 死信重放前检查幂等           | 人工重放和补偿重发都可能造成重复       |

### 消息堆积

消息堆积表示队列中的消息生产速度大于消费速度，导致 `Ready` 数量持续增长。消息堆积会增加消息延迟，严重时可能造成磁盘压力、内存压力、消费者阻塞或业务处理延迟。

常见现象如下。

| 现象                     | 说明               |
| ------------------------ | ------------------ |
| 队列 `Ready` 持续增长    | 消息等待消费       |
| 队列 `Unacked` 持续较高  | 消息已投递但未确认 |
| 消费者数量为 0           | 没有消费者监听队列 |
| 消费速率低于发布速率     | 消费能力不足       |
| 应用日志消费很慢         | 业务处理耗时过长   |
| 数据库或第三方接口压力高 | 下游瓶颈导致消费慢 |

常见原因如下。

| 原因                | 说明                             |
| ------------------- | -------------------------------- |
| 消费者未启动        | 队列没有消费者                   |
| 队列绑定错误        | 消费者监听了错误队列             |
| 消费者异常退出      | 应用报错或容器崩溃               |
| 消费逻辑耗时过长    | 数据库慢 SQL、第三方接口慢       |
| 消费并发太低        | 消费能力不足                     |
| prefetch 设置不合理 | 消息分配不均或未确认消息过多     |
| 消费失败反复重试    | 异常消息阻塞正常消费             |
| 下游系统限流        | 数据库、Redis、外部 API 成为瓶颈 |

排查命令如下。

```bash
# 查看队列消息堆积情况
docker exec -it rabbitmq rabbitmqctl list_queues -p /dev \
  name messages_ready messages_unacknowledged messages consumers

# 查看消费者连接
docker exec -it rabbitmq rabbitmqctl list_consumers -p /dev

# 查看连接数量
docker exec -it rabbitmq rabbitmqctl list_connections name peer_host peer_port state

# 查看通道数量
docker exec -it rabbitmq rabbitmqctl list_channels connection number consumer_count messages_unacknowledged
```

管理控制台重点观察以下指标。

| 指标            | 判断方式                              |
| --------------- | ------------------------------------- |
| `Ready` 高      | 消费者没有及时消费                    |
| `Unacked` 高    | 消费者拿到消息但未确认                |
| `Consumers = 0` | 消费者未启动或监听失败                |
| `Publish > Ack` | 生产速度大于消费速度                  |
| `Deliver` 很低  | RabbitMQ 投递能力或消费者拉取能力不足 |
| 死信队列增长    | 消费失败大量进入死信                  |

消息堆积处理方式如下。

| 处理方式           | 说明                                    |
| ------------------ | --------------------------------------- |
| 启动消费者         | 如果 Consumers 为 0，优先恢复消费者     |
| 增加消费并发       | 提高 `concurrency` 和 `max-concurrency` |
| 调整 prefetch      | 根据业务耗时调整预取数量                |
| 优化业务逻辑       | 优化慢 SQL、减少同步外部调用            |
| 批量处理           | 日志、同步类业务可批量写入              |
| 拆分队列           | 按业务类型或优先级拆分队列              |
| 限制生产速度       | 高峰期对生产端限流                      |
| 处理死信和异常消息 | 避免异常消息反复消耗资源                |
| 扩容消费者实例     | 多实例横向扩容消费能力                  |

配置示例。

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        # 根据压测结果提高初始消费者数量
        concurrency: 3

        # 根据下游承载能力设置最大消费者数量
        max-concurrency: 10

        # 每个消费者最多持有的未确认消息数
        prefetch: 20

        # 重要业务继续保持手动确认
        acknowledge-mode: manual
```

消息堆积处理建议如下。

| 建议                   | 说明                                |
| ---------------------- | ----------------------------------- |
| 不要盲目提高并发       | 先确认瓶颈是消费者线程还是下游系统  |
| 关注 Unacked           | Unacked 高通常是 ack 或业务耗时问题 |
| 区分正常高峰和异常堆积 | 秒杀、批量导入可能是短时高峰        |
| 重要队列设置告警阈值   | Ready、Unacked、消费者数量都应告警  |
| 低优先级消息拆分队列   | 避免日志类消息影响订单、支付类消息  |
| 消费失败要进入死信     | 不要让异常消息反复阻塞业务队列      |
| 建立补偿和重放机制     | 堆积处理后可能需要重新处理失败消息  |

### 消费者连接异常

消费者连接异常表示 Spring Boot 应用无法连接 RabbitMQ，或者连接成功后消费者无法正常监听队列。该问题通常与网络、账号权限、vhost、队列声明、监听容器配置或 RabbitMQ 服务状态有关。

常见现象如下。

| 现象                            | 说明                            |
| ------------------------------- | ------------------------------- |
| 应用启动失败                    | RabbitMQ 连接或认证失败         |
| 控制台 Consumers 为 0           | 消费者没有成功监听队列          |
| 日志出现 `ACCESS_REFUSED`       | 账号、密码、vhost 或权限错误    |
| 日志出现 `Connection refused`   | RabbitMQ 地址或端口不可达       |
| 日志出现 `NOT_FOUND - no queue` | 监听队列不存在                  |
| 日志出现 `PRECONDITION_FAILED`  | 队列参数与已有队列不一致        |
| 消费中途断开                    | 网络异常、Broker 重启或应用重启 |

常见原因和处理建议如下。

| 原因            | 排查方式                                | 处理建议                    |
| --------------- | --------------------------------------- | --------------------------- |
| RabbitMQ 未启动 | `docker ps`、控制台访问                 | 启动 RabbitMQ               |
| 地址端口错误    | 检查配置和网络连通性                    | 修正 `host`、`port`         |
| 用户密码错误    | 查看应用日志                            | 修正账号密码                |
| vhost 不存在    | `rabbitmqctl list_vhosts`               | 创建 vhost                  |
| 用户无权限      | `rabbitmqctl list_permissions`          | 授权 configure、write、read |
| 队列不存在      | 控制台查看 Queues                       | 确认配置类声明或手动创建    |
| 队列参数冲突    | 查看启动异常                            | 删除本地队列或迁移生产队列  |
| 消费方法异常    | 查看 `ListenerExecutionFailedException` | 修复反序列化和业务异常      |
| 网络不稳定      | 查看连接断开日志                        | 检查网络、DNS、负载均衡     |

权限配置命令示例。

```bash
# 创建 vhost
docker exec -it rabbitmq rabbitmqctl add_vhost /dev

# 创建用户
docker exec -it rabbitmq rabbitmqctl add_user order_user order_password

# 给用户授权
docker exec -it rabbitmq rabbitmqctl set_permissions -p /dev order_user ".*" ".*" ".*"

# 查看权限
docker exec -it rabbitmq rabbitmqctl list_permissions -p /dev
```

生产环境建议不要使用 `.*` 授权全部资源，而是按业务前缀授权。

```bash
# 示例：仅允许 order_user 操作 order. 开头的资源
rabbitmqctl set_permissions -p /prod order_user "^order\\..*" "^order\\..*" "^order\\..*"
```

消费者连接异常排查命令如下。

```bash
# 查看消费者
docker exec -it rabbitmq rabbitmqctl list_consumers -p /dev

# 查看连接
docker exec -it rabbitmq rabbitmqctl list_connections name user peer_host state channels

# 查看通道
docker exec -it rabbitmq rabbitmqctl list_channels number connection consumer_count messages_unacknowledged

# 查看队列
docker exec -it rabbitmq rabbitmqctl list_queues -p /dev name durable arguments consumers
```

Spring Boot 消费者配置示例。

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        # 消费者并发
        concurrency: 2
        max-concurrency: 6

        # 手动确认
        acknowledge-mode: manual

        # 每个消费者预取数量
        prefetch: 10

        # 异常时不默认重新入队
        default-requeue-rejected: false
```

消费者连接异常处理建议如下。

| 建议                       | 说明                                                |
| -------------------------- | --------------------------------------------------- |
| 先确认 RabbitMQ 可达       | 使用控制台、telnet、日志确认                        |
| 再确认 vhost 和权限        | 大量连接问题来自 vhost 或权限不匹配                 |
| 检查队列是否存在           | `@RabbitListener` 监听不存在的队列会异常            |
| 保持队列声明一致           | 配置类声明参数必须与已有队列一致                    |
| 关注消费者数量             | 控制台 Consumers 为 0 说明监听未生效                |
| 避免消费方法启动即异常     | 反序列化、Bean 注入、业务初始化异常都会导致监听失败 |
| 生产配置不要依赖本地默认值 | 必须明确注入生产地址、账号和 vhost                  |

### 队列参数不一致错误

队列参数不一致错误通常表现为 `PRECONDITION_FAILED`。该错误表示代码声明的队列参数与 RabbitMQ 中已经存在的同名队列参数不一致。例如，原队列是非持久化队列，代码改成了持久化队列；原队列没有死信参数，代码新增了 `x-dead-letter-exchange`；原队列 TTL 是 10 秒，代码改成了 30 秒。

常见错误日志如下。

```text
PRECONDITION_FAILED - inequivalent arg 'durable' for queue 'order.queue'
PRECONDITION_FAILED - inequivalent arg 'x-message-ttl' for queue 'order.queue'
PRECONDITION_FAILED - inequivalent arg 'x-dead-letter-exchange' for queue 'order.queue'
```

常见触发场景如下。

| 场景                 | 说明                                |
| -------------------- | ----------------------------------- |
| 修改 durable         | 原队列非持久化，代码改为持久化      |
| 修改 TTL             | 原队列 TTL 与新代码不一致           |
| 新增死信交换机       | 原队列没有 `x-dead-letter-exchange` |
| 修改死信 Routing Key | 原队列死信路由键和新代码不同        |
| 修改最大长度         | `x-max-length` 不一致               |
| 修改队列类型         | classic、quorum 等类型不一致        |
| 同名队列多项目共用   | 不同项目对同一队列声明参数不同      |

排查方式如下。

```bash
# 查看队列参数
docker exec -it rabbitmq rabbitmqctl list_queues -p /dev name durable arguments

# 查看指定 vhost 下所有队列
docker exec -it rabbitmq rabbitmqctl list_queues -p /dev name durable auto_delete arguments
```

也可以在 RabbitMQ 管理控制台中进入队列详情页，查看 `Durable`、`Auto delete` 和 `Arguments`。

本地开发环境处理方式较简单，可以删除队列后重新启动应用，让配置类重新声明队列。

```bash
# 删除本地开发队列，仅限开发环境
docker exec -it rabbitmq rabbitmqctl delete_queue -p /dev order.queue
```

也可以在管理控制台的队列详情页中删除队列。删除后重启 Spring Boot 应用，队列会按新代码重新声明。

生产环境不能随意删除队列，因为队列中可能存在未消费消息。生产环境建议采用以下方式处理。

| 方式         | 说明                                         |
| ------------ | -------------------------------------------- |
| 新建队列     | 使用新队列名和新参数，逐步切换生产者和消费者 |
| 迁移消息     | 停止生产者后消费完旧队列，再切换到新队列     |
| 维护窗口修改 | 在停机维护窗口删除旧队列并重新声明           |
| 使用策略     | 部分参数可通过 RabbitMQ policy 管理          |
| 提前评审     | 队列参数变更必须经过发布评审                 |

错误示例：同名队列参数前后不一致。

```java
// 旧代码：普通持久化队列
QueueBuilder
        .durable("order.queue")
        .build();

// 新代码：同名队列增加 TTL 和死信参数
QueueBuilder
        .durable("order.queue")
        .ttl(30000)
        .deadLetterExchange("order.dlx.exchange")
        .build();
```

如果 RabbitMQ 中已经存在旧的 `order.queue`，新代码启动时可能报 `PRECONDITION_FAILED`。

推荐做法是使用新队列名。

```java
QueueBuilder
        .durable("order.timeout.queue")
        .ttl(30000)
        .deadLetterExchange("order.dlx.exchange")
        .deadLetterRoutingKey("order.timeout.dlx")
        .build();
```

队列命名建议区分用途。

| 用途         | 命名示例                    |
| ------------ | --------------------------- |
| 普通业务队列 | `order.created.queue`       |
| 延迟队列     | `order.timeout.delay.queue` |
| 重试队列     | `order.retry.queue`         |
| 死信队列     | `order.dead.queue`          |
| 兜底队列     | `order.fallback.queue`      |

队列参数不一致处理建议如下。

| 建议                       | 说明                                 |
| -------------------------- | ------------------------------------ |
| 开发环境可删除重建         | 本地没有重要消息时可以直接删队列     |
| 生产环境禁止直接删除       | 可能导致未消费消息丢失               |
| 队列参数变更走迁移流程     | 新队列、新绑定、灰度切换、消费旧消息 |
| 队列名称体现用途           | 不同用途不要复用同一个队列名         |
| 固定队列参数后不要随意修改 | TTL、DLX、队列类型等变更风险高       |
| 上线前检查 Arguments       | 控制台或命令确认队列参数符合预期     |
| 多服务共用队列要统一声明   | 避免不同服务声明同名队列但参数不同   |

常见问题排查总结如下。

| 问题           | 优先检查                                                     |
| -------------- | ------------------------------------------------------------ |
| 消息发送失败   | RabbitMQ 连接、账号权限、交换机、Routing Key、ConfirmCallback |
| 消息重复消费   | messageId、幂等表、Redis 幂等、ack 是否成功                  |
| 消息堆积       | Ready、Unacked、Consumers、消费耗时、并发配置                |
| 消费者连接异常 | vhost、权限、队列是否存在、监听容器配置                      |
| 队列参数不一致 | durable、TTL、DLX、queue type、Arguments                     |

RabbitMQ 常见问题处理的核心原则是：先确认消息处于生命周期的哪个阶段，再针对该阶段排查。发送阶段看连接、权限和确认；路由阶段看交换机、Routing Key 和绑定关系；消费阶段看消费者数量、确认模式和业务异常；失败阶段看死信队列、重试队列和补偿机制。
