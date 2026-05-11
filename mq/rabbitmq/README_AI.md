# RabbitMQ 开发

## 项目概述

本章节用于说明 RabbitMQ 在项目中的建设背景、目标范围、适用业务场景、技术栈选择以及功能边界。通过该章节可以先明确：为什么引入 RabbitMQ、项目准备解决哪些问题、哪些能力属于本开发文档覆盖范围，哪些能力不在本阶段建设范围内。

### 项目背景

随着业务系统规模扩大，系统之间的调用关系会逐渐从单体内部方法调用演变为多个服务之间的远程调用。如果所有业务操作都采用同步调用方式，系统会面临响应时间变长、服务耦合度升高、峰值流量难以削峰、下游服务异常影响主流程等问题。

RabbitMQ 作为成熟的消息中间件，可以在系统之间引入异步解耦机制。生产者只负责将消息发送到 RabbitMQ，消费者按照自身处理能力从队列中消费消息，从而降低系统之间的直接依赖。

在实际业务开发中，常见场景包括订单创建后异步通知库存、支付成功后异步更新订单状态、用户注册后异步发送短信或邮件、业务操作后异步写入日志、耗时任务异步执行等。通过 RabbitMQ 可以将主流程中的非核心、耗时、可异步处理的逻辑拆分出去，提高接口响应速度和系统整体吞吐能力。

本项目以 Spring Boot 3 集成 RabbitMQ 为核心，围绕消息发送、消息消费、消息可靠性、死信队列、延迟消息、幂等消费、异常处理、日志记录、监控运维等内容，形成一套可复用、可扩展、可落地的 RabbitMQ 开发方案。

### 建设目标

本项目的主要目标是建设一套面向业务系统的 RabbitMQ 开发规范和基础能力，使项目能够稳定、可靠、可维护地使用消息队列完成异步通信。

具体目标包括：

| 目标             | 说明                                                         |
| ---------------- | ------------------------------------------------------------ |
| 降低系统耦合     | 通过消息队列隔离生产者与消费者，减少系统之间的直接依赖       |
| 提升接口响应速度 | 将短信通知、日志采集、异步任务等非核心流程从主链路中拆分出去 |
| 支持流量削峰     | 利用队列缓冲突发流量，避免瞬时高并发直接冲击下游服务         |
| 提高消息可靠性   | 通过消息持久化、发布确认、消费确认、失败重试、死信队列等机制降低消息丢失风险 |
| 规范消息模型     | 统一 Exchange、Queue、Routing Key、消息体、消息头、消息 ID 等设计规则 |
| 支持异常排查     | 建立发送日志、消费日志、失败日志、死信日志，方便定位消息链路问题 |
| 支持业务扩展     | 提供统一消息发送接口和消费者编写规范，便于后续业务模块快速接入 |
| 支持运维监控     | 对队列深度、消费速率、连接数、死信数量等关键指标进行监控     |

最终建设结果应满足以下要求：

1. 开发人员可以按照统一规范完成消息生产者和消费者开发。
2. 消息发送、路由、消费、确认、重试、死信处理等流程具备清晰的实现方式。
3. 常见业务场景可以直接复用现有配置和代码结构。
4. 异常消息可以被记录、追踪、重试或人工处理。
5. RabbitMQ 相关配置具备多环境适配能力，便于本地、测试、生产环境部署。

### 适用场景

RabbitMQ 适合用于需要异步处理、系统解耦、削峰填谷、最终一致性和事件驱动的业务场景。本项目重点覆盖企业级后端系统中常见的消息队列使用方式。

常见适用场景如下：

| 场景       | 说明                                         | 示例                                           |
| ---------- | -------------------------------------------- | ---------------------------------------------- |
| 异步处理   | 将非核心、耗时操作从主流程拆分出去           | 用户注册后发送短信、订单创建后发送通知         |
| 系统解耦   | 生产者不直接调用消费者服务，双方通过消息交互 | 订单服务发送订单事件，库存服务订阅库存扣减消息 |
| 流量削峰   | 高峰请求先进入队列，消费者按能力处理         | 秒杀下单、批量导入、活动通知                   |
| 延迟处理   | 消息在指定时间后再被消费                     | 订单超时未支付自动关闭、延迟提醒               |
| 失败重试   | 消费失败后按规则重新投递或进入死信队列       | 第三方接口调用失败后重试                       |
| 最终一致性 | 使用消息驱动多个系统之间的数据状态同步       | 支付成功后更新订单、积分、优惠券状态           |
| 日志采集   | 业务系统异步发送日志消息，日志服务集中处理   | 操作日志、审计日志、行为日志                   |
| 任务分发   | 多个消费者共同处理队列中的任务               | 图片处理、文件解析、报表生成                   |

不建议使用 RabbitMQ 的场景包括：

| 场景               | 原因                                                        |
| ------------------ | ----------------------------------------------------------- |
| 强实时同步返回结果 | RabbitMQ 偏向异步通信，不适合必须立即拿到处理结果的核心链路 |
| 简单本地方法调用   | 如果业务逻辑只在单个服务内部完成，引入 MQ 会增加复杂度      |
| 超大文件传输       | MQ 不适合直接传输大文件，应传输文件地址或任务 ID            |
| 高频低延迟交易撮合 | 对极致低延迟有要求的场景需要评估专用架构                    |
| 无法接受最终一致性 | MQ 场景通常需要接受短时间状态不一致，并通过补偿保证最终一致 |

### 技术选型

本项目以 Spring Boot 3 作为后端基础框架，使用 Spring AMQP 集成 RabbitMQ，结合 Docker、Redis、数据库、Actuator 等组件完成开发、测试、部署和监控。

推荐技术选型如下：

| 技术      | 推荐选型                 | 用途                                       |
| --------- | ------------------------ | ------------------------------------------ |
| JDK       | Java 17 或更高版本       | Spring Boot 3 基础运行环境                 |
| 后端框架  | Spring Boot 3.x          | 项目基础框架、自动配置、依赖管理           |
| 消息组件  | RabbitMQ                 | 消息路由、队列存储、异步通信               |
| 集成框架  | Spring AMQP              | Spring Boot 与 RabbitMQ 的集成封装         |
| 消息发送  | RabbitTemplate           | 生产者发送消息、设置消息头、处理发送结果   |
| 消息消费  | @RabbitListener          | 声明式监听队列并消费消息                   |
| 消息格式  | JSON                     | 业务消息体序列化格式                       |
| JSON 工具 | Jackson                  | Spring AMQP 默认推荐的 JSON 转换基础       |
| 工具类    | Hutool                   | 字符串、集合、JSON、ID、日期等常用工具处理 |
| 日志框架  | SLF4J + Logback          | 发送、消费、确认、异常、死信等日志记录     |
| 幂等存储  | Redis / 数据库唯一索引   | 防止消息重复消费                           |
| 数据库    | MySQL / PostgreSQL       | 消息记录、发送日志、消费日志、死信消息存储 |
| 本地部署  | Docker / Docker Compose  | 本地快速启动 RabbitMQ 和依赖服务           |
| 监控组件  | Spring Boot Actuator     | 应用健康检查和指标暴露                     |
| 指标采集  | Prometheus               | 采集应用和 RabbitMQ 运行指标               |
| 可视化    | Grafana                  | 展示队列深度、消费速率、连接数等监控面板   |
| 测试工具  | JUnit 5 / Testcontainers | 单元测试和集成测试                         |
| 构建工具  | Maven                    | 依赖管理、项目构建、打包发布               |

技术选型原则如下：

1. 优先使用 Spring Boot 官方生态组件，减少额外封装成本。
2. RabbitMQ 客户端能力优先通过 Spring AMQP 使用，避免直接操作底层 Client API。
3. 消息格式统一使用 JSON，便于调试、日志查看和跨语言扩展。
4. 可靠性能力必须覆盖生产端确认、消费端确认、失败重试和死信处理。
5. 本地环境优先使用 Docker Compose，降低团队成员环境搭建成本。
6. 生产环境需要结合集群、高可用、监控告警和容量规划进行部署。

### 功能边界

本项目围绕 RabbitMQ 在 Spring Boot 后端系统中的开发实践展开，重点关注业务系统如何规范、可靠地发送和消费消息。功能边界需要提前明确，避免将消息中间件建设范围无限扩大。

本项目覆盖的功能范围如下：

| 模块             | 覆盖内容                                                     |
| ---------------- | ------------------------------------------------------------ |
| 基础环境         | RabbitMQ 安装、Docker 启动、管理控制台、用户权限、Virtual Host 配置 |
| Spring Boot 集成 | Maven 依赖、application.yml 配置、RabbitTemplate、RabbitAdmin、MessageConverter 配置 |
| 消息模型         | 简单队列、工作队列、发布订阅、路由、主题、RPC、延迟、死信队列 |
| 生产者开发       | 统一发送接口、消息 ID、消息头、发送日志、Confirm、Return     |
| 消费者开发       | @RabbitListener、并发消费、手动确认、异常处理、消费日志      |
| 可靠性设计       | 消息持久化、队列持久化、发布确认、消费确认、重试机制、死信队列 |
| 延迟消息         | TTL 延迟、死信延迟、延迟插件方案                             |
| 幂等设计         | Redis 幂等、数据库唯一索引、业务状态机控制                   |
| 顺序性设计       | 单队列顺序消费、分片队列、Routing Key 分片                   |
| 日志与排查       | 发送日志、接收日志、失败日志、重试日志、死信日志             |
| 监控运维         | 管理控制台、队列深度、消费速率、连接数、死信队列监控         |
| 测试验证         | 单元测试、集成测试、Testcontainers、异常场景测试             |
| 部署交付         | Docker Compose、环境变量、健康检查、灰度发布、回滚策略       |

本项目不覆盖或不作为重点覆盖的内容如下：

| 不覆盖内容               | 说明                                                         |
| ------------------------ | ------------------------------------------------------------ |
| RabbitMQ Broker 源码分析 | 本文档关注应用开发，不深入 RabbitMQ Erlang 源码实现          |
| 超大规模集群调优         | 仅提供常见集群和高可用设计思路，不替代专业容量规划           |
| 跨数据中心复杂架构       | Federation、Shovel 仅作为扩展说明，不作为核心实现            |
| 完整消息平台建设         | 不建设独立 MQ 管理平台，只提供必要的消息查询、重发、死信处理接口设计 |
| 所有业务模块完整实现     | 仅以订单、支付、库存、通知等典型场景说明集成方式             |
| 替代业务事务             | RabbitMQ 用于异步解耦和最终一致性，不替代数据库本地事务      |
| 替代定时任务平台         | 延迟消息可处理部分延迟场景，但不替代 XXL-JOB 等定时任务系统  |
| 绝对不丢消息承诺         | 通过可靠性设计降低丢失风险，但仍需结合业务补偿、日志追踪和运维机制 |

在实际落地时，建议将 RabbitMQ 能力分为三层：

| 层级       | 说明                                                         |
| ---------- | ------------------------------------------------------------ |
| 基础能力层 | 负责连接配置、交换机、队列、绑定、消息转换器等基础配置       |
| 通用封装层 | 负责统一发送接口、消息 ID、日志记录、Confirm、Return、异常处理 |
| 业务接入层 | 由订单、支付、库存、通知等业务模块按规范发送和消费消息       |

通过以上边界划分，可以避免业务代码直接散落大量 RabbitMQ 细节，也便于后续统一维护消息可靠性、日志追踪和异常补偿能力。


## RabbitMQ 基础概念

本章节用于说明 RabbitMQ 开发中最核心的基础概念。RabbitMQ 的使用并不是简单地“发送消息到队列”，而是由 Producer、Broker、Exchange、Queue、Binding、Routing Key、Consumer 等多个组件共同完成消息投递、路由、存储和消费。

### 消息队列核心概念

消息队列是一种用于系统之间异步通信的中间件机制。生产者将消息发送到消息中间件，消费者从消息中间件中获取并处理消息，生产者和消费者不需要直接调用对方。

在没有消息队列的同步调用模式中，A 服务调用 B 服务时必须等待 B 服务处理完成。如果 B 服务处理慢、不可用或瞬时流量过高，A 服务的主流程也会受到影响。引入 RabbitMQ 后，A 服务只需要将消息发送到 RabbitMQ，由 RabbitMQ 负责暂存消息，B 服务按照自身处理能力消费消息。

消息队列主要解决以下问题：

| 问题       | 说明                                                 |
| ---------- | ---------------------------------------------------- |
| 系统耦合   | 生产者和消费者通过消息通信，避免直接依赖具体服务接口 |
| 异步处理   | 主流程只发送消息，耗时逻辑由消费者异步执行           |
| 流量削峰   | 高峰流量先进入队列，消费者按固定速率处理             |
| 失败缓冲   | 下游服务短暂异常时，消息可保留在队列中等待后续处理   |
| 最终一致性 | 多个系统之间通过事件消息完成状态同步                 |

在 RabbitMQ 中，消息一般不会直接发送到 Queue，而是先发送到 Exchange，再由 Exchange 根据 Binding 和 Routing Key 将消息路由到一个或多个 Queue，最后由 Consumer 从 Queue 中消费消息。

典型流程如下：

```text
Producer -> Exchange -> Binding -> Queue -> Consumer
```

其中，Producer 负责发送消息，Exchange 负责路由消息，Queue 负责存储消息，Consumer 负责消费消息。

### Producer

Producer 表示消息生产者，也就是发送消息的一方。在业务系统中，Producer 通常是某个服务、某个接口、某个定时任务或某个业务事件处理逻辑。

例如，在订单系统中，用户下单成功后，订单服务可以作为 Producer 发送一条“订单已创建”的消息。库存服务、通知服务、积分服务等可以根据需要消费这条消息。

Producer 的核心职责包括：

| 职责             | 说明                                                         |
| ---------------- | ------------------------------------------------------------ |
| 构造消息体       | 根据业务场景生成消息内容，例如订单 ID、用户 ID、业务状态等   |
| 设置消息属性     | 设置消息 ID、消息类型、消息头、过期时间、持久化标识等        |
| 选择 Exchange    | 指定消息发送到哪个交换机                                     |
| 设置 Routing Key | 指定消息路由规则                                             |
| 处理发送结果     | 根据 Confirm 和 Return 机制处理发送成功、失败、不可路由等情况 |
| 记录发送日志     | 记录消息发送时间、业务标识、消息 ID、发送状态等信息          |

Producer 不应该关心具体有多少个消费者，也不应该直接依赖消费者服务的接口。它只需要按照约定发送符合格式规范的消息。

在 Spring Boot 项目中，Producer 通常通过 `RabbitTemplate` 发送消息。实际开发中建议对 `RabbitTemplate` 进行统一封装，避免业务代码中到处散落交换机名称、队列名称、Routing Key 和消息属性设置逻辑。

### Consumer

Consumer 表示消息消费者，也就是从队列中获取并处理消息的一方。在业务系统中，Consumer 通常用于执行异步业务逻辑，例如扣减库存、发送短信、同步状态、写入日志、处理延迟任务等。

Consumer 的核心职责包括：

| 职责           | 说明                                         |
| -------------- | -------------------------------------------- |
| 监听 Queue     | 通过监听指定队列获取消息                     |
| 解析消息体     | 将消息内容转换为业务对象                     |
| 校验消息合法性 | 校验消息 ID、业务字段、消息版本等            |
| 执行业务处理   | 根据消息内容完成具体业务逻辑                 |
| 控制消费确认   | 成功后 Ack，失败后 Nack 或 Reject            |
| 处理异常消息   | 对失败消息进行重试、丢弃或投递到死信队列     |
| 保证消费幂等   | 防止同一消息被重复消费导致业务重复执行       |
| 记录消费日志   | 记录消费结果、异常原因、耗时、重试次数等信息 |

Consumer 通常需要重点关注可靠性。因为 RabbitMQ 本身可能因为网络抖动、消费者宕机、手动 Nack、重试机制等原因导致同一条消息被再次投递，所以消费者逻辑必须具备幂等能力。

在 Spring Boot 项目中，Consumer 通常通过 `@RabbitListener` 声明监听队列。对于重要业务场景，建议使用手动确认模式，明确控制消息什么时候确认成功，什么时候重新入队，什么时候进入死信队列。

### Broker

Broker 表示 RabbitMQ 服务端，也就是负责接收、路由、存储和投递消息的核心服务。开发人员通常所说的“连接 RabbitMQ”，本质上就是应用程序连接 RabbitMQ Broker。

Broker 的核心能力包括：

| 能力       | 说明                                                   |
| ---------- | ------------------------------------------------------ |
| 接收消息   | 接收 Producer 发送过来的消息                           |
| 路由消息   | 通过 Exchange、Binding、Routing Key 将消息路由到 Queue |
| 存储消息   | 将消息保存到队列中，等待消费者消费                     |
| 投递消息   | 将队列中的消息推送给 Consumer                          |
| 权限控制   | 通过用户、密码、Virtual Host、权限规则控制访问范围     |
| 连接管理   | 管理 Connection、Channel、Consumer 等客户端连接        |
| 高可用支持 | 支持集群、Quorum Queue、镜像队列等高可用能力           |
| 管理监控   | 提供管理控制台、队列状态、连接数、消费速率等监控信息   |

在实际部署中，Broker 可以是单节点，也可以是集群。开发和测试环境通常使用单节点 RabbitMQ；生产环境需要根据业务重要性、消息量、可用性要求决定是否使用集群和高可用队列。

需要注意的是，Broker 不是业务数据库。RabbitMQ 可以持久化消息，但它的核心职责是消息中转和异步通信，不适合长期保存大量历史业务数据。消息处理状态、发送记录、消费记录、死信记录等业务审计信息，建议落库保存。

### Exchange

Exchange 表示交换机，是 RabbitMQ 中负责消息路由的组件。Producer 通常不会直接把消息发送到 Queue，而是发送到 Exchange，由 Exchange 决定消息应该进入哪些 Queue。

Exchange 根据自身类型、Binding 关系和 Routing Key 进行消息路由。常见 Exchange 类型如下：

| 类型             | 说明                                       | 常见场景               |
| ---------------- | ------------------------------------------ | ---------------------- |
| Direct Exchange  | 根据完全匹配的 Routing Key 路由消息        | 精确路由、业务类型分发 |
| Fanout Exchange  | 忽略 Routing Key，将消息广播到所有绑定队列 | 发布订阅、广播通知     |
| Topic Exchange   | 根据通配符 Routing Key 路由消息            | 多维度分类路由         |
| Headers Exchange | 根据消息头匹配规则路由消息                 | 特殊字段匹配，使用较少 |
| Delayed Exchange | 延迟投递消息，需要插件支持                 | 延迟任务、订单超时处理 |

Exchange 本身不存储消息。如果消息发送到 Exchange 后没有匹配到任何 Queue，消息可能会被丢弃；如果开启了 `mandatory` 参数并配置了 ReturnCallback，则生产者可以收到不可路由消息的回调通知。

Exchange 设计时需要重点关注命名规范和职责边界。一个 Exchange 不建议承载过多无关业务，否则后期 Routing Key 和 Binding 会变得混乱。常见做法是按照业务域或消息类型划分 Exchange，例如订单交换机、支付交换机、通知交换机等。

### Queue

Queue 表示队列，是 RabbitMQ 中真正存储消息的组件。Exchange 将消息路由到 Queue 后，Consumer 再从 Queue 中消费消息。

Queue 的核心特点包括：

| 特点           | 说明                                              |
| -------------- | ------------------------------------------------- |
| 存储消息       | 消息在被消费前会保存在队列中                      |
| 支持持久化     | 队列可以声明为 durable，Broker 重启后队列仍然存在 |
| 支持消息堆积   | 消费者处理不过来时，消息会在队列中堆积            |
| 支持多个消费者 | 多个消费者可以共同消费同一个队列，提高处理能力    |
| 支持参数配置   | 可以设置 TTL、最大长度、死信交换机、优先级等参数  |
| 支持确认机制   | 消费成功后确认，失败后重新入队或进入死信队列      |

队列设计时需要关注以下问题：

1. 队列是否需要持久化。
2. 消息是否需要持久化。
3. 是否需要绑定死信交换机。
4. 是否需要设置消息 TTL。
5. 是否允许多个消费者并发消费。
6. 是否需要保证消息顺序。
7. 队列名称是否符合统一命名规范。

在生产环境中，关键业务队列通常需要配置为持久化队列，并结合持久化消息、手动 Ack、死信队列和监控告警共同保证可靠性。

### Binding

Binding 表示绑定关系，用于连接 Exchange 和 Queue。Exchange 收到消息后，会根据 Binding 规则判断消息应该路由到哪些 Queue。

Binding 通常由以下内容组成：

| 组成        | 说明                                       |
| ----------- | ------------------------------------------ |
| Exchange    | 消息来源交换机                             |
| Queue       | 消息目标队列                               |
| Routing Key | 路由匹配规则                               |
| Arguments   | 可选参数，例如 Headers Exchange 的匹配条件 |

对于不同类型的 Exchange，Binding 的作用方式不同：

| Exchange 类型    | Binding 作用                                         |
| ---------------- | ---------------------------------------------------- |
| Direct Exchange  | Binding Key 必须与消息 Routing Key 完全一致          |
| Fanout Exchange  | Binding Key 通常无实际作用，所有绑定队列都会收到消息 |
| Topic Exchange   | Binding Key 支持 `*` 和 `#` 通配符匹配               |
| Headers Exchange | 根据消息头参数进行匹配                               |

Binding 是 RabbitMQ 路由规则的关键。一个 Exchange 可以绑定多个 Queue，一个 Queue 也可以绑定到多个 Exchange。通过 Binding 可以实现一条消息被投递到一个队列、多个队列，或者根据不同路由规则进入不同业务队列。

### Routing Key

Routing Key 表示路由键，是 Producer 发送消息时指定的路由标识。Exchange 会结合 Routing Key 和 Binding 规则决定消息最终进入哪些 Queue。

不同 Exchange 对 Routing Key 的使用方式不同：

| Exchange 类型    | Routing Key 使用方式                   |
| ---------------- | -------------------------------------- |
| Direct Exchange  | 完全匹配 Routing Key                   |
| Fanout Exchange  | 忽略 Routing Key                       |
| Topic Exchange   | 按通配符规则匹配 Routing Key           |
| Headers Exchange | 通常不依赖 Routing Key，而是依赖消息头 |

Routing Key 建议按照业务语义进行设计，避免使用无意义字符串。常见命名方式如下：

```text
业务域.业务动作.业务状态
```

示例：

```text
order.created
order.paid
order.cancelled
payment.success
payment.failed
stock.deduct
notice.sms.send
```

对于 Topic Exchange，可以使用多级 Routing Key 配合通配符实现灵活匹配。例如：

| Binding Key | 可匹配 Routing Key                      |
| ----------- | --------------------------------------- |
| `order.*`   | `order.created`、`order.paid`           |
| `order.#`   | `order.created`、`order.status.changed` |
| `*.success` | `payment.success`、`refund.success`     |

Routing Key 设计不宜过细，也不宜过粗。过细会导致绑定关系复杂，过粗会导致消费者收到过多无关消息。建议按照业务领域、事件类型和处理目标进行平衡设计。

### Virtual Host

Virtual Host 简称 vhost，是 RabbitMQ 中的逻辑隔离空间。每个 vhost 内部可以拥有独立的 Exchange、Queue、Binding、权限配置等资源。

vhost 的作用类似数据库中的 database 或命名空间，用于隔离不同系统、不同环境或不同业务域。

常见划分方式如下：

| 划分方式     | 示例                               |
| ------------ | ---------------------------------- |
| 按环境划分   | `/dev`、`/test`、`/prod`           |
| 按系统划分   | `/order-system`、`/payment-system` |
| 按业务域划分 | `/trade`、`/message`、`/log`       |

在实际项目中，建议至少按环境隔离 vhost，避免开发、测试、生产环境之间消息互相影响。例如：

```text
/dev
/test
/prod
```

vhost 还可以配合用户权限进行访问控制。例如，订单服务用户只允许访问订单相关 vhost，日志采集服务只允许访问日志相关 vhost。这样可以避免误操作导致跨业务消息污染。

需要注意的是，连接 RabbitMQ 时必须指定 vhost。如果应用配置中的 vhost 不存在，或者当前用户没有权限访问该 vhost，应用会连接失败。

### Message

Message 表示消息，是 RabbitMQ 中传递的具体数据。一个完整的消息通常由消息体和消息属性组成。

消息体是业务数据，例如订单 ID、用户 ID、支付金额、业务状态等。消息属性用于描述消息本身，例如消息 ID、内容类型、消息头、持久化标识、过期时间、优先级等。

常见消息组成如下：

| 组成          | 说明                                   |
| ------------- | -------------------------------------- |
| Body          | 消息体，通常为 JSON 字符串或二进制数据 |
| MessageId     | 消息唯一标识，用于链路追踪和幂等控制   |
| ContentType   | 内容类型，例如 `application/json`      |
| Headers       | 消息头，用于传递扩展信息               |
| DeliveryMode  | 投递模式，是否持久化                   |
| Expiration    | 消息过期时间                           |
| Priority      | 消息优先级                             |
| Timestamp     | 消息创建时间                           |
| CorrelationId | 关联 ID，常用于 RPC 或链路追踪         |
| ReplyTo       | 回复队列，常用于 RPC 模式              |

在业务开发中，建议消息体保持清晰、稳定、可扩展。常见 JSON 消息结构如下：

```json
{
  "messageId": "1888888888888888888",
  "eventType": "order.created",
  "version": "1.0",
  "timestamp": "2026-05-11 10:30:00",
  "data": {
    "orderId": "ORDER202605110001",
    "userId": 10001,
    "amount": 199.00
  }
}
```

消息设计建议遵循以下原则：

1. 每条消息必须有全局唯一的 `messageId`。
2. 消息体中应包含明确的业务事件类型。
3. 消息结构应支持版本号，便于后续兼容升级。
4. 消息中不要直接传输超大对象或文件内容。
5. 敏感字段需要脱敏、加密或避免进入消息体。
6. 消费者不能假设消息一定只会被消费一次，必须做好幂等控制。

## RabbitMQ 消息模型

本章节用于说明 RabbitMQ 常见消息模型。不同消息模型对应不同业务场景，实际项目中应根据消息投递范围、消费方式、路由规则、可靠性要求和扩展能力选择合适的模型。

### 简单队列模式

简单队列模式是 RabbitMQ 最基础的消息模型。一个生产者发送消息到一个队列，一个消费者从该队列中消费消息。

典型流程如下：

```text
Producer -> Queue -> Consumer
```

在 RabbitMQ 中，即使使用简单队列模式，底层仍然会经过默认交换机。默认交换机是一个 Direct Exchange，名称为空字符串，Routing Key 通常等于队列名称。

简单队列模式适合以下场景：

| 场景         | 说明                               |
| ------------ | ---------------------------------- |
| 单一异步任务 | 一个业务动作只需要一个消费者处理   |
| 简单通知     | 发送后只由一个服务消费             |
| 本地验证     | 初学、测试、调试 RabbitMQ 基础功能 |
| 低复杂度业务 | 不需要复杂路由和广播能力           |

优点是结构简单、容易理解、开发成本低。缺点是扩展能力有限，不适合复杂路由、多个消费者分组和多业务事件分发场景。

实际业务中，简单队列模式通常用于入门示例或非常简单的异步任务。如果项目中已经有统一的 Exchange 和 Routing Key 规范，建议仍然通过 Exchange 显式路由消息，而不是长期依赖默认交换机。

### 工作队列模式

工作队列模式也称 Work Queue 模式，表示多个消费者共同消费同一个队列中的消息。RabbitMQ 会将队列中的消息分发给不同消费者处理。

典型流程如下：

```text
Producer -> Queue -> Consumer A
                  -> Consumer B
                  -> Consumer C
```

工作队列模式适合处理耗时任务和任务分发场景。例如，图片压缩、文件解析、报表生成、批量数据处理等任务都可以放入同一个队列，由多个消费者实例共同处理。

工作队列模式的核心特点如下：

| 特点             | 说明                             |
| ---------------- | -------------------------------- |
| 多消费者竞争消费 | 一条消息只会被其中一个消费者消费 |
| 提高处理能力     | 可以通过增加消费者实例提升吞吐量 |
| 支持横向扩展     | 消费者服务可以部署多个实例       |
| 适合任务分发     | 每个任务只需要被处理一次         |

工作队列模式需要关注消息分发公平性。RabbitMQ 默认可能会按轮询方式分发消息，但如果某些任务耗时较长，可能导致部分消费者压力较大。通常可以通过 `prefetchCount` 控制消费者一次最多获取多少条未确认消息，从而提升分发合理性。

在重要业务中，工作队列模式建议配合手动 Ack 使用。消费者处理成功后再确认消息，处理失败时可以重新入队、重试或进入死信队列。

### 发布订阅模式

发布订阅模式表示一条消息可以被多个队列接收，每个队列可以由不同消费者处理。在 RabbitMQ 中，发布订阅模式通常通过 Fanout Exchange 实现。

典型流程如下：

```text
Producer -> Fanout Exchange -> Queue A -> Consumer A
                         -> Queue B -> Consumer B
                         -> Queue C -> Consumer C
```

Fanout Exchange 会忽略 Routing Key，将消息投递到所有与该 Exchange 绑定的队列。

发布订阅模式适合以下场景：

| 场景     | 说明                                       |
| -------- | ------------------------------------------ |
| 广播通知 | 一条业务事件需要通知多个系统               |
| 事件分发 | 多个服务对同一个事件分别处理               |
| 日志分发 | 一份日志同时进入存储、分析、告警等多个队列 |
| 缓存刷新 | 一个变更事件通知多个应用刷新缓存           |

例如，订单创建成功后，可以通过发布订阅模式将消息同时发送给库存服务、通知服务、积分服务和风控服务。每个服务拥有自己的队列，互不影响。

发布订阅模式的关键点是：每个消费者组应拥有独立队列。如果多个消费者监听同一个队列，则它们是竞争消费；如果多个消费者监听不同队列，并且这些队列都绑定到同一个 Fanout Exchange，则它们都可以收到同一条消息。

### 路由模式

路由模式表示 Exchange 根据 Routing Key 将消息投递到指定队列。在 RabbitMQ 中，路由模式通常通过 Direct Exchange 实现。

典型流程如下：

```text
Producer -> Direct Exchange -> Queue A
                          -> Queue B
```

Direct Exchange 会将消息的 Routing Key 与 Binding Key 进行完全匹配。只有匹配成功的队列才能收到消息。

示例：

| Routing Key       | 目标队列     |
| ----------------- | ------------ |
| `order.created`   | 订单创建队列 |
| `order.paid`      | 订单支付队列 |
| `order.cancelled` | 订单取消队列 |

路由模式适合以下场景：

| 场景         | 说明                                        |
| ------------ | ------------------------------------------- |
| 精确业务分发 | 不同业务事件进入不同队列                    |
| 日志级别分发 | error、warn、info 日志进入不同队列          |
| 状态事件分发 | created、paid、cancelled 等状态事件分别处理 |
| 服务定向通知 | 指定类型消息只给特定消费者处理              |

路由模式比发布订阅模式更精确。它不是把消息广播给所有队列，而是根据明确的 Routing Key 投递给匹配队列。

在实际项目中，Direct Exchange 常用于明确业务事件的分发。例如订单服务可以使用同一个订单交换机，根据不同 Routing Key 将消息发送到订单创建队列、订单支付队列、订单取消队列等。

### 主题模式

主题模式表示 Exchange 根据通配符规则匹配 Routing Key。在 RabbitMQ 中，主题模式通常通过 Topic Exchange 实现。

Topic Exchange 支持两种通配符：

| 通配符 | 说明               |
| ------ | ------------------ |
| `*`    | 匹配一个单词       |
| `#`    | 匹配零个或多个单词 |

Routing Key 通常使用点号分隔，例如：

```text
order.created
order.status.changed
payment.success
payment.refund.failed
```

Binding Key 示例：

| Binding Key | 匹配示例                                   |
| ----------- | ------------------------------------------ |
| `order.*`   | `order.created`、`order.paid`              |
| `order.#`   | `order.created`、`order.status.changed`    |
| `*.success` | `payment.success`、`refund.success`        |
| `payment.#` | `payment.success`、`payment.refund.failed` |

主题模式适合以下场景：

| 场景           | 说明                                     |
| -------------- | ---------------------------------------- |
| 多维度消息分类 | 按业务域、动作、状态进行组合路由         |
| 灵活订阅       | 消费者可以订阅某一类消息                 |
| 事件总线       | 多种业务事件通过统一 Topic Exchange 分发 |
| 日志分类       | 按系统、模块、级别、环境进行日志分发     |

主题模式比路由模式更灵活，但也更容易设计混乱。Routing Key 层级应保持稳定，不建议随意增加层级或改变含义。

推荐格式如下：

```text
业务域.事件动作.事件状态
```

示例：

```text
order.pay.success
order.pay.failed
payment.refund.success
notice.sms.send
```

主题模式适合中大型系统中的事件分发，但需要提前制定命名规范，否则后续 Binding Key 会难以维护。

### RPC 模式

RPC 模式表示生产者发送请求消息后，需要等待消费者返回处理结果。RabbitMQ 可以通过 `replyTo` 和 `correlationId` 实现 RPC 通信。

典型流程如下：

```text
Client -> Request Queue -> Server
Client <- Reply Queue   <- Server
```

RPC 模式中，请求方发送消息时会指定：

| 属性            | 说明                |
| --------------- | ------------------- |
| `replyTo`       | 响应消息返回的队列  |
| `correlationId` | 请求和响应的关联 ID |

消费者处理请求后，将响应结果发送到 `replyTo` 指定的队列，并携带相同的 `correlationId`。请求方根据 `correlationId` 判断响应属于哪一次请求。

RPC 模式适合以下场景：

| 场景         | 说明                                       |
| ------------ | ------------------------------------------ |
| 异步请求响应 | 请求方仍然需要获得处理结果                 |
| 跨服务计算   | 某个服务提供计算能力，其他服务通过消息调用 |
| 临时解耦     | 需要通过 MQ 隔离服务，但仍要求返回结果     |

不过，在实际业务开发中，不建议大量使用 RabbitMQ 实现 RPC。原因如下：

1. RPC 会削弱消息队列异步解耦的价值。
2. 请求方仍然需要等待响应，容易形成隐式同步依赖。
3. 超时、重试、重复响应、响应队列清理等问题会增加复杂度。
4. 大量 RPC 请求可能导致响应队列和关联关系管理复杂。

如果业务天然是同步查询或强实时调用，通常优先考虑 HTTP、gRPC 或 Dubbo 等 RPC 框架。RabbitMQ RPC 更适合作为特殊场景下的补充方案，而不是常规服务调用方式。

### 延迟消息模式

延迟消息模式表示消息发送后不会立即被消费者消费，而是在指定延迟时间后再投递或被消费。RabbitMQ 原生不直接提供普通队列级别的任意延迟消息能力，但可以通过 TTL + 死信队列或延迟插件实现。

常见延迟消息方案如下：

| 方案                          | 说明                                                       |
| ----------------------------- | ---------------------------------------------------------- |
| TTL + 死信队列                | 消息先进入延迟队列，过期后转发到死信交换机，再进入业务队列 |
| 队列 TTL                      | 队列中所有消息使用相同过期时间                             |
| 消息 TTL                      | 每条消息可以设置不同过期时间                               |
| Delayed Message Exchange 插件 | 使用 `x-delayed-message` 插件实现延迟投递                  |

延迟消息适合以下场景：

| 场景         | 说明                             |
| ------------ | -------------------------------- |
| 订单超时关闭 | 下单后 30 分钟未支付自动关闭     |
| 延迟通知     | 指定时间后发送提醒消息           |
| 重试补偿     | 调用失败后延迟一段时间再重试     |
| 任务延后执行 | 某个业务任务需要在未来时间点处理 |

TTL + 死信队列的典型流程如下：

```text
Producer -> Delay Exchange -> Delay Queue
                              消息过期
                          -> Dead Letter Exchange -> Business Queue -> Consumer
```

这种方案的优点是不依赖插件，兼容性较好。缺点是如果同一个队列中存在不同 TTL 的消息，可能受到队头阻塞影响，导致后面的短延迟消息不能及时过期投递。

延迟插件方案更适合需要灵活设置不同延迟时间的场景，但需要在 RabbitMQ Broker 上安装插件，生产环境使用前需要评估插件兼容性、运维成本和集群部署方式。

### 死信队列模式

死信队列模式用于处理无法被正常消费的消息。所谓死信，就是由于某些原因不能继续在原队列中正常流转的消息。RabbitMQ 可以将这些消息投递到指定的死信交换机，再由死信交换机路由到死信队列。

消息成为死信的常见原因如下：

| 原因         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 消息被拒绝   | Consumer 执行 `basic.reject` 或 `basic.nack`，并且 `requeue=false` |
| 消息过期     | 消息 TTL 到期后仍未被消费                                    |
| 队列超长     | 队列达到最大长度限制，旧消息被挤出                           |
| 队列存储超限 | 队列达到最大容量限制后消息被转为死信                         |

死信队列典型流程如下：

```text
Producer -> Business Exchange -> Business Queue -> Consumer
                                      |
                                      | 消费失败 / 消息过期 / 队列超长
                                      v
                              Dead Letter Exchange -> Dead Letter Queue -> Dead Letter Consumer
```

死信队列适合以下场景：

| 场景         | 说明                                           |
| ------------ | ---------------------------------------------- |
| 失败消息隔离 | 将多次消费失败的消息隔离出来，避免阻塞正常队列 |
| 异常排查     | 保存异常消息，便于开发和运维排查               |
| 人工补偿     | 管理端查看死信消息后人工处理                   |
| 延迟消息     | 通过 TTL + 死信机制实现延迟投递                |
| 重试兜底     | 消息重试多次仍失败后进入死信队列               |

死信队列不是“垃圾队列”，而是可靠性设计中的重要兜底机制。对于核心业务消息，不建议消费失败后直接丢弃，应优先进入死信队列，并记录失败原因、异常堆栈、消息内容、业务标识、重试次数等信息。

死信队列设计建议如下：

1. 每个重要业务队列都应配置对应的死信 Exchange 和死信 Queue。
2. 死信队列应设置独立消费者或管理端处理入口。
3. 死信消息需要记录业务主键、消息 ID、异常原因和发生时间。
4. 死信处理应支持查询、重发、忽略、标记已处理等操作。
5. 死信队列需要纳入监控告警，避免异常消息长期堆积。


## 项目环境准备

本章节用于说明 RabbitMQ 开发前需要准备的基础环境，包括 JDK、Spring Boot、RabbitMQ、Maven、Docker 以及本地服务规划。环境准备的目标是保证团队成员可以使用一致的版本、端口、账号和目录结构进行开发、调试和验证。

### JDK 版本要求

本项目基于 Spring Boot 3 开发，JDK 版本建议使用 `17` 或更高版本。Spring Boot 3.x 的核心要求是 Java 17 起步，Spring Boot 官方文档也明确说明 Spring Boot 3.5 需要 Java SDK 17 或更高版本，并且 Maven 构建需要 Maven 3.6.3 或更高版本。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/installing.html?utm_source=chatgpt.com))

推荐版本如下：

| 环境     | 推荐版本        | 说明                                    |
| -------- | --------------- | --------------------------------------- |
| 本地开发 | JDK 17 / JDK 21 | 与 Spring Boot 3 兼容，推荐统一团队版本 |
| 测试环境 | JDK 17 / JDK 21 | 与本地开发保持一致                      |
| 生产环境 | JDK 17 / JDK 21 | 按企业基础镜像和运行规范统一选择        |

本地验证 JDK 环境：

```bash
# 查看 Java 版本
java -version

# 查看 Java 编译器版本
javac -version

# 查看 JAVA_HOME 环境变量
echo $JAVA_HOME
```

命令说明：`java -version` 用于确认运行时版本，`javac -version` 用于确认 JDK 编译器版本，`JAVA_HOME` 用于确认当前系统引用的 JDK 安装路径。Spring Boot 3 项目不建议使用 JDK 8 或 JDK 11，否则会在编译或启动阶段出现兼容性问题。

### Spring Boot 版本要求

本项目建议使用 Spring Boot 3.x 版本，并统一使用稳定小版本作为项目基线。对于新项目，建议优先选择当前团队已经验证过的 Spring Boot 3.3.x、3.4.x 或 3.5.x 版本，避免随意混用不同小版本。

推荐版本策略如下：

| 组件                 | 推荐版本              | 说明                                 |
| -------------------- | --------------------- | ------------------------------------ |
| Spring Boot          | 3.3.x / 3.4.x / 3.5.x | 选择企业内部统一基线版本             |
| Spring Framework     | 随 Spring Boot 管理   | 不建议手动覆盖版本                   |
| Spring AMQP          | 随 Spring Boot 管理   | 使用 `spring-boot-starter-amqp` 引入 |
| RabbitMQ Java Client | 随 Spring AMQP 管理   | 不建议业务项目直接指定版本           |

版本管理建议：

1. 使用 `spring-boot-starter-parent` 统一管理依赖版本。
2. 不建议手动指定 `spring-rabbit`、`amqp-client` 的版本，避免与 Spring Boot BOM 冲突。
3. 多模块项目应在父工程中统一配置 Spring Boot 版本。
4. 生产项目升级 Spring Boot 小版本前，需要回归验证 RabbitMQ 发送、消费、确认、重试和死信流程。

Maven 父工程版本示例：

```xml
<!-- Spring Boot 父工程，用于统一依赖版本管理 -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.0</version>
    <relativePath/>
</parent>
```

### RabbitMQ 版本要求

本项目建议使用 RabbitMQ 4.x 版本进行开发和验证。Docker 官方 RabbitMQ 镜像当前提供了 `4.3.0`、`4.3.0-management`、`4.2.6-management`、`4.1.8-management` 等标签，因此本地开发可以优先使用带管理控制台的 `rabbitmq:4.3.0-management` 镜像。([Docker Hub](https://hub.docker.com/_/rabbitmq?utm_source=chatgpt.com))

推荐版本如下：

| 环境     | 推荐版本                    | 说明                             |
| -------- | --------------------------- | -------------------------------- |
| 本地开发 | `rabbitmq:4.3.0-management` | 包含 Web 管理控制台，便于调试    |
| 测试环境 | 固定 4.x 小版本             | 与生产基线保持一致               |
| 生产环境 | 企业统一 RabbitMQ 4.x 基线  | 结合集群、高可用、监控和备份策略 |

如果使用 Docker 镜像部署 RabbitMQ，Erlang 运行环境已经包含在镜像中，通常不需要单独安装 Erlang。如果采用二进制包、RPM、DEB 等方式安装 RabbitMQ，则必须关注 RabbitMQ 与 Erlang/OTP 的兼容关系。RabbitMQ 官方文档说明 RabbitMQ 通常支持最近两个 Erlang release series，并提供了 RabbitMQ 与 Erlang 的兼容矩阵。([RabbitMQ](https://www.rabbitmq.com/docs/4.2/which-erlang?utm_source=chatgpt.com))

版本选择建议：

1. 本地开发优先使用 Docker 镜像，降低 Erlang 环境维护成本。
2. 不建议直接使用 `latest` 标签，生产和测试环境应固定具体版本。
3. 管理控制台调试环境使用 `management` 镜像。
4. 生产环境升级 RabbitMQ 前，需要验证队列声明、消息确认、死信、延迟插件、监控指标和客户端兼容性。
5. 如果使用延迟消息插件，需要确认插件版本与 RabbitMQ 版本兼容。

### Maven 环境准备

Maven 用于项目依赖管理、编译、测试和打包。Spring Boot 3.5 官方文档说明 Maven 需要 3.6.3 或更高版本；Apache Maven 官方安装文档显示 Maven 可通过包管理器或手动下载压缩包安装，并需要配置 JDK 或 `JAVA_HOME`。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/installing.html?utm_source=chatgpt.com))

推荐版本如下：

| 工具         | 推荐版本     | 说明                             |
| ------------ | ------------ | -------------------------------- |
| Maven        | 3.9.x        | 推荐使用较新的稳定版本           |
| JDK          | 17+          | Maven 执行编译时需要引用正确 JDK |
| settings.xml | 企业私服配置 | 配置 Maven 仓库、镜像、认证信息  |

本地验证 Maven 环境：

```bash
# 查看 Maven 版本
mvn -version

# 查看 Maven 使用的 Java 版本
mvn -v

# 查看当前项目依赖树
mvn dependency:tree

# 清理并打包项目，跳过测试
mvn clean package -DskipTests
```

命令说明：`mvn -version` 可以同时查看 Maven 版本和 Maven 当前使用的 Java 版本；`mvn dependency:tree` 用于排查依赖冲突；`mvn clean package -DskipTests` 用于本地快速构建项目。

Maven 本地仓库和镜像配置建议：

```xml
<!-- 文件位置：~/.m2/settings.xml -->
<settings>
    <!-- 本地仓库路径，建议配置到磁盘空间充足的位置 -->
    <localRepository>/data/maven/repository</localRepository>

    <mirrors>
        <!-- 企业内部 Maven 仓库或公共镜像仓库 -->
        <mirror>
            <id>company-maven</id>
            <mirrorOf>*</mirrorOf>
            <name>Company Maven Repository</name>
            <url>https://maven.example.com/repository/maven-public/</url>
        </mirror>
    </mirrors>

    <profiles>
        <profile>
            <id>jdk-17</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <!-- 项目统一 Java 编译版本 -->
                <maven.compiler.source>17</maven.compiler.source>
                <maven.compiler.target>17</maven.compiler.target>
                <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
            </properties>
        </profile>
    </profiles>
</settings>
```

配置说明：`localRepository` 用于指定本地依赖缓存目录，`mirrors` 用于配置企业私服或公共镜像仓库，`maven.compiler.source` 和 `maven.compiler.target` 用于统一编译版本。

### Docker 环境准备

Docker 用于本地快速启动 RabbitMQ，避免开发人员在本机手动安装 Erlang 和 RabbitMQ。Docker Compose 则适合统一管理 RabbitMQ、Redis、MySQL、PostgreSQL 等多个本地开发依赖。Docker 官方说明 Docker Compose 用于定义和运行多容器应用，并可以通过单个 YAML 文件管理服务、网络和数据卷。([Docker Documentation](https://docs.docker.com/compose/?utm_source=chatgpt.com))

本地建议安装以下工具：

| 工具                           | 说明                                     |
| ------------------------------ | ---------------------------------------- |
| Docker Engine / Docker Desktop | 容器运行环境                             |
| Docker Compose Plugin          | 使用 `docker compose` 命令管理多容器服务 |
| curl                           | 用于接口验证                             |
| netcat / telnet                | 用于端口连通性验证                       |
| jq                             | 用于 JSON 输出格式化，可选               |

Docker Compose 官方推荐优先使用 Docker Desktop 或 Compose Plugin，Standalone 方式属于旧兼容方式。([Docker Documentation](https://docs.docker.com/compose/install/?utm_source=chatgpt.com))

本地验证 Docker 环境：

```bash
# 查看 Docker 版本
docker version

# 查看 Docker 服务信息
docker info

# 查看 Docker Compose 版本
docker compose version

# 拉取 RabbitMQ 管理版镜像
docker pull rabbitmq:4.3.0-management
```

命令说明：`docker version` 用于确认 Docker 客户端和服务端版本，`docker compose version` 用于确认 Compose 插件是否可用，`docker pull` 用于提前拉取 RabbitMQ 镜像，避免首次启动时等待镜像下载。

### 本地开发环境规划

本地开发环境需要统一端口、容器名称、账号、密码、Virtual Host 和数据卷目录，避免团队成员之间配置不一致导致问题难以复现。

推荐本地规划如下：

| 项目            | 推荐值               | 说明                       |
| --------------- | -------------------- | -------------------------- |
| RabbitMQ 容器名 | `rabbitmq-dev`       | 本地开发 RabbitMQ 容器     |
| AMQP 端口       | `5672`               | 应用连接 RabbitMQ 的端口   |
| 管理控制台端口  | `15672`              | 浏览器访问 RabbitMQ 控制台 |
| 默认账号        | `ateng`              | 本地开发账号               |
| 默认密码        | `Ateng@123456`       | 本地开发密码               |
| 默认 vhost      | `/dev`               | 本地开发 Virtual Host      |
| 数据卷          | `rabbitmq_data`      | 保存 RabbitMQ 数据         |
| 配置目录        | `./docker/rabbitmq`  | 保存 RabbitMQ 配置文件     |
| Compose 文件    | `docker-compose.yml` | 本地依赖统一编排文件       |

本地项目目录建议：

```text
rabbitmq-demo/
├── docker/
│   └── rabbitmq/
│       ├── enabled_plugins
│       └── rabbitmq.conf
├── src/
│   └── main/
│       ├── java/
│       └── resources/
│           └── application.yml
├── docker-compose.yml
└── pom.xml
```

目录说明：`docker/rabbitmq` 用于存放 RabbitMQ 配置文件，`docker-compose.yml` 用于启动本地 RabbitMQ，`application.yml` 用于配置 Spring Boot 连接信息。

本地端口占用检查：

```bash
# 检查 AMQP 端口是否被占用
lsof -i :5672

# 检查 RabbitMQ 管理控制台端口是否被占用
lsof -i :15672

# Linux 环境也可以使用 ss 查看端口
ss -lntp | grep -E '5672|15672'
```

命令说明：`lsof -i` 用于查看指定端口是否被进程占用，`ss -lntp` 用于查看监听中的 TCP 端口。启动 RabbitMQ 前应确认 `5672` 和 `15672` 没有被其他服务占用。

## RabbitMQ 安装与启动

本章节用于说明 RabbitMQ 在本地开发环境中的安装、启动、访问、用户权限、Virtual Host 配置和常用管理命令。开发环境优先使用 Docker 或 Docker Compose，便于快速创建、销毁和重置 RabbitMQ 实例。

### Docker 安装 RabbitMQ

Docker 单容器方式适合快速验证 RabbitMQ 功能。该方式不依赖额外配置文件，启动命令较短，适合本地临时调试。

直接启动 RabbitMQ 管理版容器：

```bash
# 创建并启动 RabbitMQ 容器
docker run -d \
  --name rabbitmq-dev \
  --hostname rabbitmq-dev \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=ateng \
  -e RABBITMQ_DEFAULT_PASS=Ateng@123456 \
  -e RABBITMQ_DEFAULT_VHOST=/dev \
  rabbitmq:4.3.0-management
```

命令说明：`--name` 指定容器名称，`--hostname` 指定 RabbitMQ 节点主机名，`-p 5672:5672` 暴露 AMQP 端口，`-p 15672:15672` 暴露管理控制台端口，`RABBITMQ_DEFAULT_USER`、`RABBITMQ_DEFAULT_PASS`、`RABBITMQ_DEFAULT_VHOST` 用于初始化默认账号、密码和 vhost。

查看容器状态：

```bash
# 查看 RabbitMQ 容器是否运行
docker ps | grep rabbitmq-dev

# 查看 RabbitMQ 容器日志
docker logs -f rabbitmq-dev
```

停止和删除容器：

```bash
# 停止 RabbitMQ 容器
docker stop rabbitmq-dev

# 删除 RabbitMQ 容器
docker rm rabbitmq-dev
```

如果需要保留数据，建议增加数据卷挂载：

```bash
# 使用 Docker 数据卷保存 RabbitMQ 数据
docker volume create rabbitmq_data

docker run -d \
  --name rabbitmq-dev \
  --hostname rabbitmq-dev \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=ateng \
  -e RABBITMQ_DEFAULT_PASS=Ateng@123456 \
  -e RABBITMQ_DEFAULT_VHOST=/dev \
  -v rabbitmq_data:/var/lib/rabbitmq \
  rabbitmq:4.3.0-management
```

数据卷说明：`rabbitmq_data:/var/lib/rabbitmq` 用于保存 RabbitMQ 的队列、交换机、绑定、用户、权限等运行数据。删除容器不会删除数据卷，适合本地持续开发。

### Docker Compose 安装 RabbitMQ

Docker Compose 方式适合项目长期使用。它可以将 RabbitMQ 的端口、账号、密码、vhost、数据卷、健康检查等配置固化到项目文件中，便于团队成员统一启动。

文件位置：`docker-compose.yml`

```yaml
services:
  rabbitmq:
    # 使用带管理控制台的 RabbitMQ 镜像
    image: rabbitmq:4.3.0-management
    container_name: rabbitmq-dev
    hostname: rabbitmq-dev
    restart: unless-stopped

    ports:
      # 应用连接 RabbitMQ 使用的 AMQP 端口
      - "5672:5672"
      # RabbitMQ Web 管理控制台端口
      - "15672:15672"

    environment:
      # 本地开发账号
      RABBITMQ_DEFAULT_USER: ateng
      # 本地开发密码
      RABBITMQ_DEFAULT_PASS: Ateng@123456
      # 本地开发 Virtual Host
      RABBITMQ_DEFAULT_VHOST: /dev

    volumes:
      # RabbitMQ 数据持久化目录
      - rabbitmq_data:/var/lib/rabbitmq

    healthcheck:
      # 检查 RabbitMQ 节点是否正常
      test: ["CMD", "rabbitmq-diagnostics", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  # Docker 命名数据卷，用于保存 RabbitMQ 数据
  rabbitmq_data:
```

启动 RabbitMQ：

```bash
# 在 docker-compose.yml 所在目录执行
docker compose up -d

# 查看服务状态
docker compose ps

# 查看 RabbitMQ 日志
docker compose logs -f rabbitmq
```

命令说明：`docker compose up -d` 表示后台启动 Compose 文件中的服务，`docker compose ps` 用于查看服务运行状态，`docker compose logs -f rabbitmq` 用于实时查看 RabbitMQ 服务日志。

停止和清理 RabbitMQ：

```bash
# 停止服务，但保留数据卷
docker compose down

# 停止服务并删除数据卷，谨慎执行
docker compose down -v
```

注意事项：`docker compose down -v` 会删除 RabbitMQ 数据卷，本地已创建的队列、交换机、用户、权限和消息数据会被清理。只有在需要彻底重置本地环境时才执行。

### RabbitMQ 管理控制台

RabbitMQ 管理控制台用于查看连接、Channel、Exchange、Queue、Binding、Consumer、消息速率、队列深度等信息。使用 `management` 镜像启动后，默认会启用管理插件。Docker 官方 RabbitMQ 镜像说明中也明确提供了带 management plugin 的镜像标签，并说明管理控制台默认使用 `15672` 端口。([Docker Hub](https://hub.docker.com/_/rabbitmq?utm_source=chatgpt.com))

本地访问地址：

```text
http://localhost:15672
```

登录信息：

| 项目   | 值             |
| ------ | -------------- |
| 用户名 | `ateng`        |
| 密码   | `Ateng@123456` |
| vhost  | `/dev`         |

管理控制台常用页面说明：

| 页面               | 说明                                           |
| ------------------ | ---------------------------------------------- |
| Overview           | 查看 RabbitMQ 节点、消息速率、连接数、队列总览 |
| Connections        | 查看客户端连接信息                             |
| Channels           | 查看 Channel 使用情况                          |
| Exchanges          | 查看和创建交换机                               |
| Queues and Streams | 查看和创建队列，查看队列深度                   |
| Admin              | 管理用户、权限、Virtual Host、策略等           |

验证管理控制台是否可用：

```bash
# 检查管理控制台 HTTP 端口
curl -I http://localhost:15672

# 检查 RabbitMQ 容器健康状态
docker inspect --format='{{json .State.Health}}' rabbitmq-dev
```

命令说明：`curl -I` 用于请求管理控制台响应头，`docker inspect` 用于查看容器健康检查结果。如果管理控制台无法访问，优先检查容器是否启动、端口是否映射、日志是否存在异常。

### 用户与权限配置

RabbitMQ 用户用于控制客户端连接和管理控制台访问权限。生产环境不建议使用默认 `guest` 用户，也不建议将管理账号直接配置到业务应用中。应根据用途区分管理员账号、应用账号和只读监控账号。

常见用户规划：

| 用户          | 角色     | 说明                  |
| ------------- | -------- | --------------------- |
| `admin`       | 管理员   | 管理控制台使用        |
| `app_order`   | 应用用户 | 订单服务连接 RabbitMQ |
| `app_payment` | 应用用户 | 支付服务连接 RabbitMQ |
| `monitor`     | 监控用户 | 只读查看指标和状态    |

在容器中创建用户：

```bash
# 进入 RabbitMQ 容器
docker exec -it rabbitmq-dev bash

# 创建应用用户
rabbitmqctl add_user app_order 'AppOrder@123456'

# 设置用户标签，普通业务应用不需要 administrator
rabbitmqctl set_user_tags app_order management

# 给 app_order 授权访问 /dev vhost
rabbitmqctl set_permissions -p /dev app_order ".*" ".*" ".*"

# 查看用户列表
rabbitmqctl list_users

# 查看指定 vhost 权限
rabbitmqctl list_permissions -p /dev
```

命令说明：`add_user` 用于创建用户，`set_user_tags` 用于设置用户标签，`set_permissions` 用于配置用户在指定 vhost 下的配置、写入和读取权限。权限表达式的三个参数分别对应 `configure`、`write`、`read`。

生产环境权限建议：

| 权限          | 建议                               |
| ------------- | ---------------------------------- |
| configure     | 应用账号尽量只允许配置自身业务资源 |
| write         | 只允许写入指定业务 Exchange        |
| read          | 只允许读取指定业务 Queue           |
| administrator | 只授予运维管理员                   |
| guest         | 禁止远程使用，生产环境不使用       |

更严格的业务权限示例：

```bash
# 只允许 app_order 配置、写入、读取 order 前缀资源
rabbitmqctl set_permissions -p /dev app_order "^order\\..*" "^order\\..*" "^order\\..*"
```

该命令通过正则限制 `app_order` 用户只能操作 `order.` 前缀的 RabbitMQ 资源，适合多业务共用同一个 vhost 的场景。

### Virtual Host 配置

Virtual Host 是 RabbitMQ 的逻辑隔离空间。不同 vhost 中的 Exchange、Queue、Binding、权限互相隔离。开发、测试、生产环境应使用不同 vhost，避免消息互相污染。

推荐 vhost 规划：

| 环境     | vhost    | 说明         |
| -------- | -------- | ------------ |
| 本地开发 | `/dev`   | 本地开发调试 |
| 测试环境 | `/test`  | 测试环境联调 |
| 预发环境 | `/stage` | 生产前验证   |
| 生产环境 | `/prod`  | 生产业务使用 |

创建 vhost：

```bash
# 创建测试环境 vhost
rabbitmqctl add_vhost /test

# 创建生产环境 vhost
rabbitmqctl add_vhost /prod

# 查看 vhost 列表
rabbitmqctl list_vhosts
```

给用户授权指定 vhost：

```bash
# 给订单服务账号授权访问 /test
rabbitmqctl set_permissions -p /test app_order ".*" ".*" ".*"

# 查看 /test 下的权限配置
rabbitmqctl list_permissions -p /test
```

删除 vhost：

```bash
# 删除指定 vhost，谨慎执行
rabbitmqctl delete_vhost /test
```

注意事项：删除 vhost 会删除该 vhost 下的 Exchange、Queue、Binding、消息和权限配置，测试环境清理时可以使用，生产环境必须严格审批。

Spring Boot 配置中的 vhost 示例：

```yaml
spring:
  rabbitmq:
    # RabbitMQ 服务地址
    host: localhost
    # AMQP 端口
    port: 5672
    # 应用连接账号
    username: app_order
    # 应用连接密码
    password: AppOrder@123456
    # 当前应用使用的 Virtual Host
    virtual-host: /dev
```

配置说明：`virtual-host` 必须与 RabbitMQ 中实际存在的 vhost 一致，并且当前用户需要拥有该 vhost 的访问权限，否则应用启动或首次连接 RabbitMQ 时会失败。

### 常用管理命令

RabbitMQ 常用管理命令主要包括节点状态、用户管理、vhost 管理、交换机管理、队列管理、绑定管理、消息查看和插件管理。本地开发阶段可以直接通过 `docker exec` 进入容器执行命令。

进入容器：

```bash
# 进入 RabbitMQ 容器交互终端
docker exec -it rabbitmq-dev bash
```

查看节点状态：

```bash
# 查看 RabbitMQ 节点状态
rabbitmqctl status

# 查看 RabbitMQ 集群状态
rabbitmqctl cluster_status

# 检查 RabbitMQ 节点是否可用
rabbitmq-diagnostics ping
```

用户管理命令：

```bash
# 查看用户列表
rabbitmqctl list_users

# 新增用户
rabbitmqctl add_user app_order 'AppOrder@123456'

# 修改用户密码
rabbitmqctl change_password app_order 'NewPassword@123456'

# 删除用户
rabbitmqctl delete_user app_order

# 设置用户标签
rabbitmqctl set_user_tags app_order management
```

Virtual Host 管理命令：

```bash
# 查看 vhost 列表
rabbitmqctl list_vhosts

# 新增 vhost
rabbitmqctl add_vhost /dev

# 删除 vhost，谨慎执行
rabbitmqctl delete_vhost /dev

# 查看指定 vhost 下的权限
rabbitmqctl list_permissions -p /dev
```

权限管理命令：

```bash
# 给用户授予指定 vhost 权限
rabbitmqctl set_permissions -p /dev app_order ".*" ".*" ".*"

# 清除用户在指定 vhost 下的权限
rabbitmqctl clear_permissions -p /dev app_order

# 查看某个用户的全部权限
rabbitmqctl list_user_permissions app_order
```

Exchange 管理命令：

```bash
# 查看 /dev 下的交换机
rabbitmqctl list_exchanges -p /dev name type durable auto_delete internal

# 通过 rabbitmqadmin 创建 Direct Exchange
rabbitmqadmin -V /dev declare exchange name=order.exchange type=direct durable=true
```

Queue 管理命令：

```bash
# 查看 /dev 下的队列
rabbitmqctl list_queues -p /dev name durable messages consumers

# 查看队列详细状态
rabbitmqctl list_queues -p /dev name messages_ready messages_unacknowledged consumers memory

# 通过 rabbitmqadmin 创建队列
rabbitmqadmin -V /dev declare queue name=order.created.queue durable=true
```

Binding 管理命令：

```bash
# 查看绑定关系
rabbitmqctl list_bindings -p /dev

# 绑定 Exchange 和 Queue
rabbitmqadmin -V /dev declare binding \
  source=order.exchange \
  destination=order.created.queue \
  routing_key=order.created
```

消息发布和消费测试：

```bash
# 发布一条测试消息
rabbitmqadmin -V /dev publish \
  exchange=order.exchange \
  routing_key=order.created \
  payload='{"messageId":"test-001","eventType":"order.created","data":{"orderId":"ORDER001"}}'

# 从队列中获取一条消息，ackmode=ack_requeue_false 表示取出后不重新入队
rabbitmqadmin -V /dev get \
  queue=order.created.queue \
  ackmode=ack_requeue_false
```

插件管理命令：

```bash
# 查看插件列表
rabbitmq-plugins list

# 启用管理插件
rabbitmq-plugins enable rabbitmq_management

# 启用延迟消息插件，前提是插件文件已经安装到 plugins 目录
rabbitmq-plugins enable rabbitmq_delayed_message_exchange
```

清理本地队列消息：

```bash
# 清空指定队列中的消息，谨慎执行
rabbitmqctl purge_queue -p /dev order.created.queue
```

命令说明：`rabbitmqctl` 是 RabbitMQ 自带的核心管理命令，适合节点、用户、vhost、权限、队列状态等管理操作；`rabbitmqadmin` 更适合通过 HTTP API 创建交换机、队列、绑定以及发布测试消息。使用 `purge_queue` 会清空队列中的消息，本地调试可以使用，生产环境必须谨慎执行。


## Spring Boot 3 项目初始化

本章节用于说明 Spring Boot 3 集成 RabbitMQ 的基础项目结构、Maven 依赖、配置文件、多环境配置、连接参数和日志配置。完成本章节后，项目应具备连接 RabbitMQ、发送消息、监听队列和输出关键日志的基础能力。

### 项目目录结构

项目目录结构建议按照 Spring Boot 常规分层组织，同时将 RabbitMQ 相关配置、常量、消息 DTO、生产者、消费者单独归类，避免 MQ 代码散落在业务代码中。

推荐目录结构如下：

```text
rabbitmq-demo/
├── pom.xml
├── docker-compose.yml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── io/
│   │   │       └── github/
│   │   │           └── atengk/
│   │   │               └── rabbitmq/
│   │   │                   ├── RabbitMqApplication.java
│   │   │                   ├── config/
│   │   │                   │   ├── RabbitMqConfig.java
│   │   │                   │   └── RabbitMqMessageConfig.java
│   │   │                   ├── constant/
│   │   │                   │   └── RabbitMqConstant.java
│   │   │                   ├── dto/
│   │   │                   │   └── OrderCreatedMessage.java
│   │   │                   ├── producer/
│   │   │                   │   └── OrderMessageProducer.java
│   │   │                   └── consumer/
│   │   │                       └── OrderMessageConsumer.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-test.yml
│   │       ├── application-prod.yml
│   │       └── logback-spring.xml
│   └── test/
│       └── java/
└── README.md
```

目录职责说明如下：

| 目录或文件             | 说明                                                    |
| ---------------------- | ------------------------------------------------------- |
| `config`               | RabbitMQ 交换机、队列、绑定、消息转换器、监听容器等配置 |
| `constant`             | Exchange、Queue、Routing Key 等常量                     |
| `dto`                  | 消息体对象，建议按业务事件拆分                          |
| `producer`             | 消息生产者封装                                          |
| `consumer`             | 消息消费者监听器                                        |
| `application.yml`      | 通用配置                                                |
| `application-dev.yml`  | 本地开发配置                                            |
| `application-test.yml` | 测试环境配置                                            |
| `application-prod.yml` | 生产环境配置                                            |
| `logback-spring.xml`   | 日志输出配置                                            |

项目结构设计原则：

1. Exchange、Queue、Routing Key 不直接写死在业务方法中，应统一放在常量类或配置类中。
2. 消息 DTO 与业务实体分离，避免数据库实体直接作为 MQ 消息体。
3. 生产者负责封装发送逻辑，业务 Service 不直接操作 `RabbitTemplate`。
4. 消费者只处理消息入口，复杂业务逻辑应下沉到业务 Service。
5. RabbitMQ 配置按基础配置、消息转换、可靠性配置逐步拆分，避免单个配置类过大。

### Maven 依赖配置

Maven 依赖用于引入 Spring Boot、Spring AMQP、RabbitMQ、JSON、校验、监控、日志和工具类能力。Spring Boot 官方文档说明 RabbitMQ 支持由 `spring-boot-starter-amqp` 提供，常用连接配置可以通过 `spring.rabbitmq.*` 完成。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/messaging/amqp.html?utm_source=chatgpt.com))

文件位置：`pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot 父工程，统一管理 Spring 生态依赖版本 -->
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
    <description>Spring Boot 3 RabbitMQ 开发示例</description>

    <properties>
        <!-- Spring Boot 3 要求 Java 17+ -->
        <java.version>17</java.version>
        <!-- Hutool 工具类版本，可按企业依赖基线统一管理 -->
        <hutool.version>5.8.36</hutool.version>
    </properties>

    <dependencies>
        <!-- Web 能力：用于提供测试接口、健康检查接口等 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- RabbitMQ 集成：包含 Spring AMQP、RabbitTemplate、@RabbitListener 等能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>

        <!-- 参数校验：用于消息 DTO、接口入参校验 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Actuator：用于健康检查和运行指标暴露 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Hutool：用于 ID、日期、字符串、集合、JSON 等常用工具处理 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok：减少 DTO、日志对象等样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 测试依赖：用于单元测试和 Spring Boot 集成测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- RabbitMQ 测试支持：用于 RabbitMQ 相关测试场景 -->
        <dependency>
            <groupId>org.springframework.amqp</groupId>
            <artifactId>spring-rabbit-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot 打包插件，用于生成可执行 jar -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>

            <!-- Maven 编译插件，明确 Java 编译版本 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <encoding>UTF-8</encoding>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

配置说明：

| 依赖                             | 作用                                                         |
| -------------------------------- | ------------------------------------------------------------ |
| `spring-boot-starter-amqp`       | 提供 RabbitMQ 自动配置、`RabbitTemplate`、`@RabbitListener` 等核心能力 |
| `spring-boot-starter-validation` | 用于消息对象字段校验                                         |
| `spring-boot-starter-actuator`   | 用于健康检查和监控指标                                       |
| `hutool-all`                     | 用于生成消息 ID、处理日期、字符串、对象判断等                |
| `spring-rabbit-test`             | 用于 RabbitMQ 测试场景                                       |

如果项目已经统一使用企业父工程或 BOM，应以企业版本基线为准，不建议在业务模块中随意覆盖 Spring AMQP、RabbitMQ Java Client、Jackson 等底层依赖版本。

### application.yml 配置

`application.yml` 用于放置项目通用配置。环境差异较大的配置，例如 RabbitMQ 地址、账号、密码、vhost 等，应放到对应环境配置文件或配置中心中。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 应用服务端口
  port: 8080

spring:
  application:
    # 应用名称，会用于日志、连接名、监控指标等
    name: rabbitmq-demo

  profiles:
    # 默认启用本地开发环境
    active: dev

  jackson:
    # JSON 日期时间格式
    date-format: yyyy-MM-dd HH:mm:ss
    # 默认时区
    time-zone: Asia/Shanghai

management:
  endpoints:
    web:
      exposure:
        # 暴露健康检查和指标端点
        include: health,info,metrics
  endpoint:
    health:
      # 显示健康检查详情
      show-details: always

logging:
  level:
    # 项目日志级别
    io.github.atengk.rabbitmq: info
    # Spring AMQP 关键日志
    org.springframework.amqp: info
    # RabbitMQ 客户端日志
    com.rabbitmq.client: warn
```

配置说明：

| 配置项                                      | 说明                           |
| ------------------------------------------- | ------------------------------ |
| `spring.application.name`                   | 应用名称，建议与服务名保持一致 |
| `spring.profiles.active`                    | 当前启用环境                   |
| `management.endpoints.web.exposure.include` | 暴露 Actuator 端点             |
| `logging.level.org.springframework.amqp`    | Spring AMQP 日志级别           |
| `logging.level.com.rabbitmq.client`         | RabbitMQ 客户端日志级别        |

### 多环境配置

多环境配置用于隔离本地、测试、生产环境的 RabbitMQ 地址、账号、密码和 vhost。开发环境可以使用 Docker 本地 RabbitMQ，测试和生产环境应使用独立 RabbitMQ 服务。

文件位置：`src/main/resources/application-dev.yml`

```yaml
spring:
  rabbitmq:
    # 本地 RabbitMQ 地址
    host: localhost
    # AMQP 默认端口
    port: 5672
    # 本地开发账号
    username: ateng
    # 本地开发密码
    password: Ateng@123456
    # 本地开发 Virtual Host
    virtual-host: /dev
```

文件位置：`src/main/resources/application-test.yml`

```yaml
spring:
  rabbitmq:
    # 测试环境 RabbitMQ 地址
    host: rabbitmq-test.example.com
    port: 5672
    # 测试环境应用账号，建议从配置中心或环境变量注入
    username: ${RABBITMQ_USERNAME:app_order}
    password: ${RABBITMQ_PASSWORD:AppOrder@123456}
    virtual-host: /test
```

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  rabbitmq:
    # 生产环境建议使用 addresses 支持多个节点
    addresses: ${RABBITMQ_ADDRESSES:amqp://app_order:password@rabbitmq-prod-01:5672,rabbitmq-prod-02:5672}
    # 生产环境 Virtual Host
    virtual-host: /prod
```

Spring Boot 支持使用 `spring.rabbitmq.addresses` 配置连接地址；当使用 `addresses` 时，`host` 和 `port` 会被忽略。官方文档还说明，如果地址使用 `amqps` 协议，SSL 支持会自动启用。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/messaging/amqp.html?utm_source=chatgpt.com))

多环境启动方式：

```bash
# 使用默认 dev 环境启动
mvn spring-boot:run

# 指定 test 环境启动
mvn spring-boot:run -Dspring-boot.run.profiles=test

# jar 包方式指定 prod 环境启动
java -jar rabbitmq-demo-1.0.0.jar --spring.profiles.active=prod
```

命令说明：`spring.profiles.active` 用于选择当前运行环境。生产环境不建议把账号密码写死在配置文件中，应通过环境变量、配置中心或密钥管理系统注入。

### RabbitMQ 连接配置

RabbitMQ 连接配置用于控制连接地址、账号、vhost、发布确认、不可路由消息回调、消费确认、并发消费、预取数量、重试等参数。Spring Boot RabbitMQ 配置主要位于 `spring.rabbitmq.*` 下。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/messaging/amqp.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application-dev.yml`

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: ateng
    password: Ateng@123456
    virtual-host: /dev

    # 发布确认模式：correlated 表示回调中携带 CorrelationData，便于定位消息
    publisher-confirm-type: correlated

    # 开启不可路由消息返回
    publisher-returns: true

    template:
      # mandatory=true 时，消息无法路由到队列会触发 ReturnCallback
      mandatory: true
      retry:
        # 开启 RabbitTemplate 发送重试
        enabled: true
        # 首次重试间隔
        initial-interval: 1s
        # 最大重试次数
        max-attempts: 3
        # 最大重试间隔
        max-interval: 5s
        # 重试间隔倍率
        multiplier: 2

    listener:
      type: simple
      simple:
        # 手动确认模式，消费者业务处理成功后手动 ack
        acknowledge-mode: manual
        # 最小消费者数量
        concurrency: 2
        # 最大消费者数量
        max-concurrency: 8
        # 每个消费者一次最多拉取的未确认消息数
        prefetch: 10
        retry:
          # 开启消费者重试
          enabled: true
          # 最大重试次数
          max-attempts: 3
          # 首次重试间隔
          initial-interval: 1s
          # 最大重试间隔
          max-interval: 5s
          # 重试间隔倍率
          multiplier: 2
```

关键配置说明：

| 配置项                             | 说明                                               |
| ---------------------------------- | -------------------------------------------------- |
| `publisher-confirm-type`           | 生产者消息到达 Exchange 后触发确认回调             |
| `publisher-returns`                | 消息无法路由到 Queue 时触发返回回调                |
| `template.mandatory`               | 配合 ReturnCallback 使用，避免不可路由消息静默丢弃 |
| `listener.simple.acknowledge-mode` | 消费确认模式，重要业务建议使用 `manual`            |
| `listener.simple.concurrency`      | 初始消费者并发数                                   |
| `listener.simple.max-concurrency`  | 最大消费者并发数                                   |
| `listener.simple.prefetch`         | 单个消费者预取消息数量                             |
| `listener.simple.retry.enabled`    | 是否开启消费重试                                   |

Spring Boot 官方文档说明，`RabbitTemplate` 的重试默认关闭；监听器重试也默认关闭，开启后当重试耗尽时，默认会拒绝消息，消息可能被丢弃或在 Broker 配置死信交换机时进入死信交换机。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/messaging/amqp.html?utm_source=chatgpt.com))

### 日志配置

日志配置用于输出 RabbitMQ 发送、确认、返回、消费、异常和重试等关键链路信息。开发环境可以适当提高 Spring AMQP 日志级别，生产环境应控制日志量，避免高频消息导致日志文件过大。

文件位置：`src/main/resources/logback-spring.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 应用名称 -->
    <springProperty scope="context" name="APP_NAME" source="spring.application.name" defaultValue="rabbitmq-demo"/>

    <!-- 日志目录，可通过环境变量覆盖 -->
    <property name="LOG_PATH" value="${LOG_PATH:-./logs}"/>

    <!-- 控制台日志格式 -->
    <property name="CONSOLE_LOG_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"/>

    <!-- 文件日志格式 -->
    <property name="FILE_LOG_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{64} - %msg%n"/>

    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${CONSOLE_LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 应用普通日志 -->
    <appender name="APP_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${APP_NAME}.log</file>
        <encoder>
            <pattern>${FILE_LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- 按天滚动日志 -->
            <fileNamePattern>${LOG_PATH}/${APP_NAME}.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxHistory>30</maxHistory>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <!-- 单个日志文件最大 100MB -->
                <maxFileSize>100MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
        </rollingPolicy>
    </appender>

    <!-- RabbitMQ 业务日志 -->
    <appender name="MQ_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${APP_NAME}-mq.log</file>
        <encoder>
            <pattern>${FILE_LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- RabbitMQ 日志按天滚动 -->
            <fileNamePattern>${LOG_PATH}/${APP_NAME}-mq.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxHistory>30</maxHistory>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>100MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
        </rollingPolicy>
    </appender>

    <!-- 项目业务日志 -->
    <logger name="io.github.atengk.rabbitmq" level="INFO" additivity="false">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="APP_FILE"/>
    </logger>

    <!-- RabbitMQ 生产者和消费者日志可单独输出 -->
    <logger name="io.github.atengk.rabbitmq.producer" level="INFO" additivity="false">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="MQ_FILE"/>
    </logger>

    <logger name="io.github.atengk.rabbitmq.consumer" level="INFO" additivity="false">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="MQ_FILE"/>
    </logger>

    <!-- Spring AMQP 框架日志 -->
    <logger name="org.springframework.amqp" level="INFO"/>

    <!-- RabbitMQ Java Client 日志 -->
    <logger name="com.rabbitmq.client" level="WARN"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="APP_FILE"/>
    </root>
</configuration>
```

日志建议：

1. 生产者发送消息时记录 `messageId`、`exchange`、`routingKey`、业务主键。
2. Confirm 回调记录消息是否到达 Exchange。
3. Return 回调记录不可路由消息。
4. 消费者接收消息时记录 `messageId`、队列名、业务主键。
5. 消费异常时记录异常类型、异常原因、重试次数和消息内容摘要。
6. 不建议在生产环境完整打印过大的消息体或敏感字段。

## RabbitMQ 自动配置解析

本章节用于说明 Spring Boot 3 集成 RabbitMQ 后自动装配的核心组件。理解这些组件有助于后续统一封装生产者、配置消息转换器、控制消费确认、处理发送确认和定制监听容器。

Spring Boot 在检测到 RabbitMQ 和 Spring AMQP 相关依赖后，会根据 `spring.rabbitmq.*` 配置创建连接工厂、模板对象、管理对象和监听容器工厂。RabbitMQ 基础设施存在时，任意 Bean 都可以通过 `@RabbitListener` 创建监听端点；如果没有自定义监听容器工厂，Spring Boot 会自动配置默认的 `SimpleRabbitListenerContainerFactory`。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/messaging/amqp.html?utm_source=chatgpt.com))

### Spring AMQP 核心组件

Spring AMQP 是 Spring 对 AMQP 消息协议和 RabbitMQ 客户端能力的封装。它屏蔽了 RabbitMQ Java Client 的大量底层细节，让业务代码可以通过模板类、注解监听、消息转换器和容器工厂完成消息开发。

核心组件如下：

| 组件                                   | 说明                                              |
| -------------------------------------- | ------------------------------------------------- |
| `ConnectionFactory`                    | RabbitMQ 连接工厂，负责创建和缓存连接             |
| `RabbitTemplate`                       | 消息发送模板，用于发送消息和接收消息              |
| `RabbitAdmin` / `AmqpAdmin`            | 管理组件，用于声明 Exchange、Queue、Binding       |
| `@RabbitListener`                      | 消息监听注解，用于声明消费者方法                  |
| `SimpleRabbitListenerContainerFactory` | 简单监听容器工厂，用于创建消费者监听容器          |
| `MessageConverter`                     | 消息转换器，用于 Java 对象和 MQ 消息之间转换      |
| `MessageProperties`                    | 消息属性对象，用于设置消息 ID、内容类型、消息头等 |
| `CorrelationData`                      | 发布确认关联数据，用于 Confirm 回调定位消息       |

常见自动配置关系如下：

```text
spring.rabbitmq.* 配置
        |
        v
CachingConnectionFactory
        |
        |---- RabbitTemplate
        |---- RabbitAdmin
        |---- SimpleRabbitListenerContainerFactory
        |---- @RabbitListener 消费监听容器
```

在实际开发中，建议遵循以下原则：

1. 发送消息统一使用业务封装后的 `RabbitTemplate`，不要在业务代码中重复设置公共属性。
2. 消费消息统一使用 `@RabbitListener`，重要业务使用手动 Ack。
3. Exchange、Queue、Binding 使用配置类声明，避免手工创建后环境不可复现。
4. 消息体统一使用 JSON 格式，并配置统一 `MessageConverter`。
5. 发布确认、不可路由回调、消费异常、死信处理必须有日志记录。

### RabbitTemplate

`RabbitTemplate` 是 Spring AMQP 中最常用的消息发送组件，主要用于向 Exchange 发送消息，也可以用于同步接收消息。Spring Boot 会根据自动配置创建 `RabbitTemplate`，并且支持通过 `RabbitTemplateCustomizer` 进行应用级定制。官方文档说明，如果定义了 `MessageConverter` Bean，会自动关联到自动配置的 `AmqpTemplate`。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/messaging/amqp.html?utm_source=chatgpt.com))

`RabbitTemplate` 常见能力如下：

| 能力                  | 说明                           |
| --------------------- | ------------------------------ |
| `convertAndSend`      | 将 Java 对象转换为消息后发送   |
| `send`                | 发送已经构造好的 `Message`     |
| `receive`             | 从队列中接收消息               |
| `setConfirmCallback`  | 设置发布确认回调               |
| `setReturnsCallback`  | 设置不可路由消息返回回调       |
| `setMessageConverter` | 设置消息转换器                 |
| `setMandatory`        | 控制不可路由消息是否返回生产者 |

生产者封装示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/producer/OrderMessageProducer.java`

```java
package io.github.atengk.rabbitmq.producer;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.rabbitmq.constant.RabbitMqConstant;
import io.github.atengk.rabbitmq.dto.OrderCreatedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 订单消息生产者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送订单创建消息
     *
     * @param message 订单创建消息
     * @return 消息ID
     */
    public String sendOrderCreatedMessage(OrderCreatedMessage message) {
        String messageId = StrUtil.blankToDefault(message.getMessageId(), UUID.fastUUID().toString(true));
        message.setMessageId(messageId);

        CorrelationData correlationData = new CorrelationData(messageId);
        rabbitTemplate.convertAndSend(
                RabbitMqConstant.ORDER_EXCHANGE,
                RabbitMqConstant.ORDER_CREATED_ROUTING_KEY,
                message,
                correlationData
        );

        log.info("订单创建消息已发送，messageId={}，orderId={}，exchange={}，routingKey={}",
                messageId,
                message.getOrderId(),
                RabbitMqConstant.ORDER_EXCHANGE,
                RabbitMqConstant.ORDER_CREATED_ROUTING_KEY
        );
        return messageId;
    }
}
```

配套常量类示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/constant/RabbitMqConstant.java`

```java
package io.github.atengk.rabbitmq.constant;

/**
 * RabbitMQ 常量
 *
 * @author Ateng
 * @since 2026-05-11
 */
public final class RabbitMqConstant {

    private RabbitMqConstant() {
    }

    /**
     * 订单交换机
     */
    public static final String ORDER_EXCHANGE = "order.exchange";

    /**
     * 订单创建队列
     */
    public static final String ORDER_CREATED_QUEUE = "order.created.queue";

    /**
     * 订单创建路由键
     */
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
}
```

消息 DTO 示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/dto/OrderCreatedMessage.java`

```java
package io.github.atengk.rabbitmq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单创建消息
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
public class OrderCreatedMessage {

    /**
     * 消息ID
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
     * 创建时间
     */
    private LocalDateTime createdTime;
}
```

使用 `RabbitTemplate` 时应注意：发送成功不等于消费者处理成功。生产者侧 Confirm 只能确认消息是否到达 Exchange，Return 只能确认消息是否不可路由，消费者业务处理结果需要通过消费日志、状态表或业务回调判断。

### RabbitAdmin

`RabbitAdmin` 是 Spring AMQP 提供的 RabbitMQ 管理组件，通常通过 `AmqpAdmin` 接口使用。它可以声明 Exchange、Queue、Binding 等资源。Spring Boot 官方文档说明，如果将 `Queue` 定义为 Bean，会自动用于在 RabbitMQ 实例上声明对应队列。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/messaging/amqp.html?utm_source=chatgpt.com))

在 Spring Boot 项目中，更推荐通过 `@Bean` 声明 Exchange、Queue、Binding，由 Spring AMQP 自动完成声明。这样可以保证本地、测试、生产环境的 RabbitMQ 资源结构由代码管理，减少手工配置带来的不一致。

RabbitMQ 资源声明示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/RabbitMqConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import io.github.atengk.rabbitmq.constant.RabbitMqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 基础资源配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class RabbitMqConfig {

    /**
     * 声明订单交换机
     *
     * @return Direct 类型订单交换机
     */
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(
                RabbitMqConstant.ORDER_EXCHANGE,
                true,
                false
        );
    }

    /**
     * 声明订单创建队列
     *
     * @return 持久化订单创建队列
     */
    @Bean
    public Queue orderCreatedQueue() {
        return new Queue(
                RabbitMqConstant.ORDER_CREATED_QUEUE,
                true,
                false,
                false
        );
    }

    /**
     * 绑定订单交换机和订单创建队列
     *
     * @return Binding 绑定关系
     */
    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder
                .bind(orderCreatedQueue())
                .to(orderExchange())
                .with(RabbitMqConstant.ORDER_CREATED_ROUTING_KEY);
    }
}
```

配置说明：

| Bean               | 说明                              |
| ------------------ | --------------------------------- |
| `DirectExchange`   | 声明订单业务交换机                |
| `Queue`            | 声明订单创建队列                  |
| `Binding`          | 将交换机、队列和 Routing Key 绑定 |
| `durable=true`     | Broker 重启后资源仍然存在         |
| `autoDelete=false` | 不随最后一个消费者断开而自动删除  |

注意事项：

1. 生产环境中，队列、交换机参数一旦创建，部分属性不能直接修改。
2. 如果代码声明的参数与 RabbitMQ 中已存在资源参数不一致，应用启动可能报错。
3. 重要队列建议在上线前通过变更脚本或运维流程创建，应用声明作为兜底和环境一致性保障。
4. 不建议业务系统在运行期频繁动态创建大量队列。

### SimpleRabbitListenerContainerFactory

`SimpleRabbitListenerContainerFactory` 用于创建基于 `@RabbitListener` 的监听容器。它负责管理消费者线程、队列监听、消息分发、Ack 模式、重试策略、异常处理、预取数量等行为。

Spring Boot 官方文档说明，当 RabbitMQ 基础设施存在时，任意 Bean 可以使用 `@RabbitListener` 创建监听端点；如果没有定义 `RabbitListenerContainerFactory`，Spring Boot 会自动配置默认的 `SimpleRabbitListenerContainerFactory`，也可以通过 `spring.rabbitmq.listener.type` 切换到 Direct 类型容器。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/messaging/amqp.html?utm_source=chatgpt.com))

消费者示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/consumer/OrderMessageConsumer.java`

```java
package io.github.atengk.rabbitmq.consumer;

import com.rabbitmq.client.Channel;
import io.github.atengk.rabbitmq.constant.RabbitMqConstant;
import io.github.atengk.rabbitmq.dto.OrderCreatedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 订单消息消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class OrderMessageConsumer {

    /**
     * 消费订单创建消息
     *
     * @param orderMessage 订单创建消息
     * @param message      原始消息
     * @param channel      RabbitMQ Channel
     * @throws IOException Ack 或 Nack 失败时抛出
     */
    @RabbitListener(queues = RabbitMqConstant.ORDER_CREATED_QUEUE)
    public void consumeOrderCreatedMessage(OrderCreatedMessage orderMessage, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            log.info("开始消费订单创建消息，messageId={}，orderId={}，deliveryTag={}",
                    orderMessage.getMessageId(),
                    orderMessage.getOrderId(),
                    deliveryTag
            );

            // 这里调用订单后续业务逻辑，例如发送通知、同步积分、写入业务日志等
            handleOrderCreated(orderMessage);

            channel.basicAck(deliveryTag, false);
            log.info("订单创建消息消费成功，messageId={}，orderId={}",
                    orderMessage.getMessageId(),
                    orderMessage.getOrderId()
            );
        } catch (Exception ex) {
            log.error("订单创建消息消费失败，messageId={}，orderId={}，原因={}",
                    orderMessage.getMessageId(),
                    orderMessage.getOrderId(),
                    ex.getMessage(),
                    ex
            );

            // requeue=false 表示不重新入队，若队列配置了死信交换机，则进入死信队列
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 处理订单创建业务
     *
     * @param orderMessage 订单创建消息
     */
    private void handleOrderCreated(OrderCreatedMessage orderMessage) {
        log.info("执行订单创建后置业务，orderId={}，userId={}",
                orderMessage.getOrderId(),
                orderMessage.getUserId()
        );
    }
}
```

如果需要自定义监听容器工厂，可以显式声明 `SimpleRabbitListenerContainerFactory`。官方文档说明，Spring Boot 提供 `SimpleRabbitListenerContainerFactoryConfigurer`，可以用与自动配置一致的设置初始化自定义工厂。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/messaging/amqp.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/RabbitMqMessageConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 消息监听配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class RabbitMqMessageConfig {

    /**
     * 自定义手动确认监听容器工厂
     *
     * @param configurer        Spring Boot 自动配置辅助对象
     * @param connectionFactory RabbitMQ 连接工厂
     * @return 手动确认监听容器工厂
     */
    @Bean
    public SimpleRabbitListenerContainerFactory manualRabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);

        // 重要业务建议使用手动确认
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        // 每个消费者最多预取 10 条未确认消息
        factory.setPrefetchCount(10);
        // 初始并发消费者数量
        factory.setConcurrentConsumers(2);
        // 最大并发消费者数量
        factory.setMaxConcurrentConsumers(8);

        return factory;
    }
}
```

使用自定义监听工厂：

```java
@RabbitListener(
        queues = RabbitMqConstant.ORDER_CREATED_QUEUE,
        containerFactory = "manualRabbitListenerContainerFactory"
)
public void consumeOrderCreatedMessage(OrderCreatedMessage orderMessage, Message message, Channel channel) {
    // 消费逻辑
}
```

监听容器配置建议：

| 参数                     | 建议                                                 |
| ------------------------ | ---------------------------------------------------- |
| `acknowledgeMode`        | 重要业务使用 `MANUAL`                                |
| `prefetchCount`          | 根据单条消息处理耗时设置，避免消费者一次拉取过多消息 |
| `concurrentConsumers`    | 设置基础消费者数量                                   |
| `maxConcurrentConsumers` | 设置高峰期最大消费者数量                             |
| `defaultRequeueRejected` | 结合死信队列设计，避免失败消息无限重入队             |
| `adviceChain`            | 可用于高级重试、拦截和异常处理                       |

### MessageConverter

`MessageConverter` 用于 Java 对象和 RabbitMQ 消息之间的转换。默认情况下，如果没有配置 JSON 转换器，复杂对象可能以 Java 序列化形式发送，不利于跨语言、排查和兼容。实际项目中建议统一使用 JSON 消息格式。

Spring Boot 官方文档说明，如果定义了 `MessageConverter` Bean，会自动关联到自动配置的 `AmqpTemplate`；如果定义了 `MessageConverter` 或 `MessageRecoverer` Bean，也会自动关联到默认监听容器工厂。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/messaging/amqp.html?utm_source=chatgpt.com))

JSON 消息转换器配置示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/RabbitMqMessageConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 消息转换器配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class RabbitMqMessageConfig {

    /**
     * 配置 JSON 消息转换器
     *
     * @param objectMapper Spring Boot 自动配置的 ObjectMapper
     * @return JSON 消息转换器
     */
    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new JacksonJsonMessageConverter(objectMapper);
    }
}
```

注意：不同 Spring AMQP 版本中 JSON 转换器类名可能存在差异，常见类包括 `JacksonJsonMessageConverter` 或 `Jackson2JsonMessageConverter`。以当前项目实际依赖版本可用类为准。如果 IDE 无法识别类名，应检查 `spring-amqp` 版本和导入包路径。

消息转换建议：

1. 消息体统一使用 JSON，不使用 Java 原生序列化。
2. 消息 DTO 字段命名保持稳定，不随意删除字段。
3. 新增字段应保证消费者向后兼容。
4. `LocalDateTime` 等时间字段应统一格式。
5. 消费端不能只依赖 Java 类型推断，关键消息类型建议通过 `eventType` 或消息头识别。
6. 跨系统消息不建议直接暴露内部实体类结构。

### ConnectionFactory

`ConnectionFactory` 是 RabbitMQ 连接工厂，负责创建应用到 RabbitMQ Broker 的连接。Spring Boot 通常会自动创建 `CachingConnectionFactory`，并根据 `spring.rabbitmq.*` 配置连接地址、账号、密码、vhost、发布确认等参数。

Spring Boot 官方文档说明，如果存在 `ConnectionNameStrategy` Bean，会自动用于命名由自动配置的 `CachingConnectionFactory` 创建的连接；如果需要定制底层连接工厂细节，可以定义 `ConnectionFactoryCustomizer` Bean。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/messaging/amqp.html?utm_source=chatgpt.com))

连接工厂相关配置关注点如下：

| 关注点                   | 说明             |
| ------------------------ | ---------------- |
| `host` / `port`          | 单节点连接配置   |
| `addresses`              | 多节点连接配置   |
| `username` / `password`  | 连接账号密码     |
| `virtual-host`           | 逻辑隔离空间     |
| `publisher-confirm-type` | 发布确认模式     |
| `publisher-returns`      | 不可路由消息返回 |
| `connection-timeout`     | 连接超时时间     |
| `requested-heartbeat`    | 心跳检测间隔     |
| `cache.channel.size`     | Channel 缓存数量 |

连接名称配置示例，便于在 RabbitMQ 管理控制台中识别应用来源。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/RabbitMqConnectionConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import cn.hutool.core.util.StrUtil;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 连接配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class RabbitMqConnectionConfig {

    /**
     * 配置 RabbitMQ 连接名称
     *
     * @param applicationName 应用名称
     * @return 连接名称策略
     */
    @Bean
    public ConnectionNameStrategy connectionNameStrategy(
            @Value("${spring.application.name:rabbitmq-demo}") String applicationName
    ) {
        return connectionFactory -> StrUtil.format("{}-{}", applicationName, connectionFactory.getHost());
    }
}
```

生产环境连接配置示例：

```yaml
spring:
  rabbitmq:
    # 多节点地址，适合生产环境
    addresses: amqp://app_order:password@rabbitmq-prod-01:5672,amqp://app_order:password@rabbitmq-prod-02:5672
    virtual-host: /prod

    # 连接超时时间
    connection-timeout: 10s

    # 心跳检测间隔
    requested-heartbeat: 30s

    cache:
      channel:
        # Channel 缓存数量，根据并发发送和消费情况调整
        size: 50
      connection:
        # 默认 channel 模式通常足够，特殊场景再评估 connection 模式
        mode: channel
```

ConnectionFactory 使用建议：

1. 一般不需要手动创建 `ConnectionFactory`，优先使用 Spring Boot 自动配置。
2. 多节点生产环境建议使用 `addresses`。
3. RabbitMQ 管理控制台中应能通过连接名称识别具体应用。
4. 不建议每次发送消息都创建新连接，应复用自动配置的连接工厂。
5. Channel 缓存参数需要结合生产者并发、消费者并发和 Broker 资源进行压测调整。
6. 连接异常、认证失败、vhost 不存在、权限不足是本地开发最常见的连接问题。


## 基础消息发送与消费

本章节用于完成 RabbitMQ 最小可用开发闭环，包括队列定义、普通消息发送、普通消息消费、消息对象序列化、监听配置和基础验证。完成本章节后，项目可以通过接口发送一条消息，并由消费者监听队列完成消费。

### 定义队列

队列是 RabbitMQ 中保存消息的核心组件。基础消息发送与消费可以先从一个普通持久化队列开始，生产者将消息发送到交换机，交换机根据 Routing Key 将消息路由到队列，消费者监听队列并处理消息。

基础示例采用 Direct Exchange，资源规划如下：

| 资源类型    | 名称                    | 说明                   |
| ----------- | ----------------------- | ---------------------- |
| Exchange    | `basic.direct.exchange` | 基础消息 Direct 交换机 |
| Queue       | `basic.message.queue`   | 基础消息队列           |
| Routing Key | `basic.message.send`    | 基础消息路由键         |

文件位置：`src/main/java/io/github/atengk/rabbitmq/constant/RabbitMqBasicConstant.java`

```java
package io.github.atengk.rabbitmq.constant;

/**
 * RabbitMQ 基础消息常量
 *
 * @author Ateng
 * @since 2026-05-11
 */
public final class RabbitMqBasicConstant {

    private RabbitMqBasicConstant() {
    }

    public static final String BASIC_DIRECT_EXCHANGE = "basic.direct.exchange";

    public static final String BASIC_MESSAGE_QUEUE = "basic.message.queue";

    public static final String BASIC_MESSAGE_ROUTING_KEY = "basic.message.send";
}
```

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/RabbitMqBasicConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import io.github.atengk.rabbitmq.constant.RabbitMqBasicConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 基础消息配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class RabbitMqBasicConfig {

    @Bean
    public DirectExchange basicDirectExchange() {
        return new DirectExchange(
                RabbitMqBasicConstant.BASIC_DIRECT_EXCHANGE,
                true,
                false
        );
    }

    @Bean
    public Queue basicMessageQueue() {
        return new Queue(
                RabbitMqBasicConstant.BASIC_MESSAGE_QUEUE,
                true,
                false,
                false
        );
    }

    @Bean
    public Binding basicMessageBinding() {
        return BindingBuilder
                .bind(basicMessageQueue())
                .to(basicDirectExchange())
                .with(RabbitMqBasicConstant.BASIC_MESSAGE_ROUTING_KEY);
    }
}
```

这段配置会在应用启动时声明一个持久化 Direct Exchange、一个持久化队列以及一条绑定关系。开发环境中，如果 RabbitMQ 中不存在这些资源，Spring AMQP 会自动创建；如果资源已存在且参数不一致，启动时可能报错，因此生产环境要避免随意修改已存在队列和交换机的关键参数。

### 发送普通消息

普通消息发送通常由业务 Service 或 Controller 调用生产者组件完成。业务层不建议直接操作 `RabbitTemplate`，应通过生产者类统一封装交换机、Routing Key、消息 ID、发送日志和异常处理。

基础消息对象如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/dto/BasicMessage.java`

```java
package io.github.atengk.rabbitmq.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 基础消息对象
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
public class BasicMessage {

    private String messageId;

    @NotBlank(message = "消息标题不能为空")
    private String title;

    @NotBlank(message = "消息内容不能为空")
    private String content;

    private LocalDateTime sendTime;
}
```

基础消息生产者如下，负责生成消息 ID、补充发送时间并发送到 RabbitMQ。

文件位置：`src/main/java/io/github/atengk/rabbitmq/producer/BasicMessageProducer.java`

```java
package io.github.atengk.rabbitmq.producer;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.rabbitmq.constant.RabbitMqBasicConstant;
import io.github.atengk.rabbitmq.dto.BasicMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 基础消息生产者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BasicMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public String send(BasicMessage message) {
        if (ObjectUtil.isNull(message)) {
            throw new IllegalArgumentException("消息对象不能为空");
        }

        String messageId = IdUtil.fastSimpleUUID();
        message.setMessageId(messageId);
        message.setSendTime(LocalDateTime.now());

        CorrelationData correlationData = new CorrelationData(messageId);
        rabbitTemplate.convertAndSend(
                RabbitMqBasicConstant.BASIC_DIRECT_EXCHANGE,
                RabbitMqBasicConstant.BASIC_MESSAGE_ROUTING_KEY,
                message,
                correlationData
        );

        log.info("基础消息发送成功，messageId={}，title={}，exchange={}，routingKey={}",
                messageId,
                message.getTitle(),
                RabbitMqBasicConstant.BASIC_DIRECT_EXCHANGE,
                RabbitMqBasicConstant.BASIC_MESSAGE_ROUTING_KEY
        );
        return messageId;
    }
}
```

对外提供一个测试接口，便于通过 HTTP 触发消息发送。

文件位置：`src/main/java/io/github/atengk/rabbitmq/controller/BasicMessageController.java`

```java
package io.github.atengk.rabbitmq.controller;

import io.github.atengk.rabbitmq.dto.BasicMessage;
import io.github.atengk.rabbitmq.producer.BasicMessageProducer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 基础消息测试接口
 *
 * @author Ateng
 * @since 2026-05-11
 */
@RestController
@RequestMapping("/api/rabbitmq/basic")
@RequiredArgsConstructor
public class BasicMessageController {

    private final BasicMessageProducer basicMessageProducer;

    @PostMapping("/send")
    public String send(@Valid @RequestBody BasicMessage message) {
        return basicMessageProducer.send(message);
    }
}
```

接口说明：

| 项目         | 内容                       |
| ------------ | -------------------------- |
| 请求地址     | `/api/rabbitmq/basic/send` |
| 请求方式     | `POST`                     |
| Content-Type | `application/json`         |
| 返回值       | 消息 ID                    |

请求示例：

```bash
curl -X POST 'http://localhost:8080/api/rabbitmq/basic/send' \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "RabbitMQ基础消息",
    "content": "这是一条通过Spring Boot发送的RabbitMQ普通消息"
  }'
```

命令说明：该命令会调用 Spring Boot 测试接口，由接口内部调用 `BasicMessageProducer`，最终通过 `RabbitTemplate` 将消息发送到 `basic.direct.exchange`。

### 消费普通消息

消费者负责监听队列并处理消息。基础示例使用 `@RabbitListener` 监听 `basic.message.queue`，消费成功后手动 Ack，消费失败后 Nack 且不重新入队。如果队列后续配置了死信交换机，失败消息会进入死信队列。

文件位置：`src/main/java/io/github/atengk/rabbitmq/consumer/BasicMessageConsumer.java`

```java
package io.github.atengk.rabbitmq.consumer;

import com.rabbitmq.client.Channel;
import io.github.atengk.rabbitmq.constant.RabbitMqBasicConstant;
import io.github.atengk.rabbitmq.dto.BasicMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 基础消息消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class BasicMessageConsumer {

    @RabbitListener(queues = RabbitMqBasicConstant.BASIC_MESSAGE_QUEUE)
    public void consume(BasicMessage basicMessage, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            log.info("收到基础消息，messageId={}，title={}，content={}，deliveryTag={}",
                    basicMessage.getMessageId(),
                    basicMessage.getTitle(),
                    basicMessage.getContent(),
                    deliveryTag
            );

            handleMessage(basicMessage);

            channel.basicAck(deliveryTag, false);
            log.info("基础消息消费完成，messageId={}，title={}",
                    basicMessage.getMessageId(),
                    basicMessage.getTitle()
            );
        } catch (Exception ex) {
            log.error("基础消息消费失败，messageId={}，title={}，原因={}",
                    basicMessage.getMessageId(),
                    basicMessage.getTitle(),
                    ex.getMessage(),
                    ex
            );

            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void handleMessage(BasicMessage basicMessage) {
        log.info("执行业务处理，messageId={}，content={}",
                basicMessage.getMessageId(),
                basicMessage.getContent()
        );
    }
}
```

消费逻辑说明：

| 处理动作  | 说明                                             |
| --------- | ------------------------------------------------ |
| 接收消息  | `@RabbitListener` 从队列中接收消息               |
| 转换对象  | `MessageConverter` 将 JSON 转换为 `BasicMessage` |
| 执行业务  | 调用业务方法处理消息                             |
| 手动 Ack  | 业务成功后确认消息                               |
| 手动 Nack | 业务失败后拒绝消息                               |
| 记录日志  | 记录消息 ID、标题、处理结果和异常信息            |

对于正式业务消费者，建议将 `handleMessage` 中的业务逻辑下沉到 Service 层。消费者类只负责消息入口、日志、异常处理和 Ack 控制。

### 消息对象序列化

RabbitMQ 传输的是字节数据。Spring Boot 项目中发送 Java 对象时，需要通过 `MessageConverter` 将 Java 对象转换为消息体，消费时再将消息体转换为 Java 对象。实际项目建议统一使用 JSON 序列化，避免 Java 原生序列化带来的可读性差、跨语言困难和兼容性问题。

JSON 消息转换器配置如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/RabbitMqMessageConverterConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 消息序列化配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class RabbitMqMessageConverterConfig {

    @Bean
    public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new JacksonJsonMessageConverter(objectMapper);
    }
}
```

如果项目中的 `LocalDateTime` 需要统一格式，可以在 Spring Boot 全局 Jackson 配置中处理。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  jackson:
    # 统一 JSON 时间格式
    date-format: yyyy-MM-dd HH:mm:ss
    # 统一时区
    time-zone: Asia/Shanghai
    serialization:
      # 避免日期被序列化为时间戳
      write-dates-as-timestamps: false
```

消息对象设计建议：

| 建议         | 说明                                              |
| ------------ | ------------------------------------------------- |
| 使用独立 DTO | 不要直接发送 Entity、VO 或数据库对象              |
| 包含消息 ID  | 用于日志追踪、幂等控制和异常排查                  |
| 包含业务主键 | 例如订单 ID、用户 ID、支付单号等                  |
| 包含事件类型 | 便于消费者识别消息语义                            |
| 包含版本号   | 后续消息结构升级时便于兼容                        |
| 避免大对象   | 大文件、大文本、大集合应改为传递引用地址或业务 ID |
| 避免敏感数据 | 密码、证件号、密钥等不应直接进入消息体            |

### 消息监听配置

消息监听配置用于控制消费者并发数量、预取数量、确认模式和重试行为。基础业务可以使用 Spring Boot 默认配置；核心业务建议显式配置手动确认模式和预取数量。

文件位置：`src/main/resources/application-dev.yml`

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        # 手动确认，消费者处理成功后再 ack
        acknowledge-mode: manual
        # 初始消费者并发数
        concurrency: 2
        # 最大消费者并发数
        max-concurrency: 8
        # 单个消费者最多持有的未确认消息数
        prefetch: 10
        retry:
          # 基础示例先关闭监听重试，后续章节单独说明重试机制
          enabled: false
```

监听参数说明：

| 参数               | 说明           | 建议                       |
| ------------------ | -------------- | -------------------------- |
| `acknowledge-mode` | 消费确认模式   | 核心业务使用 `manual`      |
| `concurrency`      | 初始消费者数量 | 根据业务处理耗时设置       |
| `max-concurrency`  | 最大消费者数量 | 根据机器资源和队列压力设置 |
| `prefetch`         | 预取数量       | 耗时任务不宜过大           |
| `retry.enabled`    | 消费失败重试   | 与死信队列统一设计后再开启 |

如果不同队列需要不同消费参数，可以定义多个监听容器工厂。基础消息监听工厂如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/RabbitMqListenerConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 监听容器配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class RabbitMqListenerConfig {

    @Bean
    public SimpleRabbitListenerContainerFactory basicRabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(8);
        factory.setPrefetchCount(10);
        return factory;
    }
}
```

消费者使用指定监听工厂：

```java
@RabbitListener(
        queues = RabbitMqBasicConstant.BASIC_MESSAGE_QUEUE,
        containerFactory = "basicRabbitListenerContainerFactory"
)
public void consume(BasicMessage basicMessage, Message message, Channel channel) {
    // 消费逻辑
}
```

实际项目中，如果所有消费者使用同一套监听配置，可以直接通过 `application.yml` 配置，不必为每个队列单独定义监听工厂。只有当不同业务队列存在不同并发、Ack、重试、预取策略时，才建议显式定义多个容器工厂。

### 基础功能验证

基础功能验证用于确认 RabbitMQ 资源声明、消息发送、消息路由、消费者监听和对象序列化是否正常。验证时应同时观察应用日志和 RabbitMQ 管理控制台。

启动 RabbitMQ：

```bash
docker compose up -d
```

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

发送测试消息：

```bash
curl -X POST 'http://localhost:8080/api/rabbitmq/basic/send' \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "基础消息验证",
    "content": "验证RabbitMQ普通消息发送和消费"
  }'
```

应用日志中应看到类似输出：

```text
基础消息发送成功，messageId=xxx，title=基础消息验证，exchange=basic.direct.exchange，routingKey=basic.message.send
收到基础消息，messageId=xxx，title=基础消息验证，content=验证RabbitMQ普通消息发送和消费，deliveryTag=1
执行业务处理，messageId=xxx，content=验证RabbitMQ普通消息发送和消费
基础消息消费完成，messageId=xxx，title=基础消息验证
```

管理控制台验证项：

| 验证项            | 检查位置           | 预期结果                            |
| ----------------- | ------------------ | ----------------------------------- |
| Exchange 是否存在 | Exchanges          | 存在 `basic.direct.exchange`        |
| Queue 是否存在    | Queues and Streams | 存在 `basic.message.queue`          |
| Binding 是否存在  | Queue 详情页       | Routing Key 为 `basic.message.send` |
| 消费者是否在线    | Queue 详情页       | Consumers 数量大于 0                |
| 消息是否堆积      | Queue 详情页       | 消费正常时 Ready 数量为 0           |
| 日志是否完整      | 应用控制台         | 有发送、接收、处理、Ack 日志        |

常见问题排查：

| 问题                 | 可能原因                              | 处理方式                            |
| -------------------- | ------------------------------------- | ----------------------------------- |
| 应用启动失败         | vhost 不存在或账号无权限              | 创建 vhost 并授权                   |
| 消息发送后无消费日志 | 消费者未启动或监听队列名称错误        | 检查 `@RabbitListener` 队列名       |
| 队列中消息堆积       | 消费者异常或未 Ack                    | 查看消费者日志和异常堆栈            |
| 消息无法路由         | Exchange、Routing Key、Binding 不匹配 | 检查绑定关系                        |
| 对象反序列化失败     | 消息转换器未配置或字段不兼容          | 检查 `MessageConverter` 和 DTO 字段 |

## Exchange 开发实践

本章节用于说明 RabbitMQ 常见 Exchange 类型的开发实践。Exchange 是 RabbitMQ 消息路由的核心组件，生产者将消息发送到 Exchange，Exchange 根据类型、Routing Key、Binding 和 Header 参数将消息投递到对应队列。

### Direct Exchange

Direct Exchange 根据 Routing Key 完全匹配进行消息路由。只有消息的 Routing Key 与队列绑定时的 Binding Key 完全一致，消息才会进入该队列。

Direct Exchange 适合业务事件明确、路由规则精确的场景，例如订单创建、订单支付、订单取消等事件分别进入不同队列。

资源规划如下：

| 资源类型    | 名称                              |
| ----------- | --------------------------------- |
| Exchange    | `demo.direct.exchange`            |
| Queue       | `demo.direct.order.created.queue` |
| Queue       | `demo.direct.order.paid.queue`    |
| Routing Key | `order.created`                   |
| Routing Key | `order.paid`                      |

Direct Exchange 配置如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/DirectExchangeConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Direct Exchange 示例配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class DirectExchangeConfig {

    public static final String DIRECT_EXCHANGE = "demo.direct.exchange";
    public static final String ORDER_CREATED_QUEUE = "demo.direct.order.created.queue";
    public static final String ORDER_PAID_QUEUE = "demo.direct.order.paid.queue";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String ORDER_PAID_ROUTING_KEY = "order.paid";

    @Bean
    public DirectExchange demoDirectExchange() {
        return ExchangeBuilder
                .directExchange(DIRECT_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue directOrderCreatedQueue() {
        return QueueBuilder
                .durable(ORDER_CREATED_QUEUE)
                .build();
    }

    @Bean
    public Queue directOrderPaidQueue() {
        return QueueBuilder
                .durable(ORDER_PAID_QUEUE)
                .build();
    }

    @Bean
    public Binding directOrderCreatedBinding() {
        return BindingBuilder
                .bind(directOrderCreatedQueue())
                .to(demoDirectExchange())
                .with(ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding directOrderPaidBinding() {
        return BindingBuilder
                .bind(directOrderPaidQueue())
                .to(demoDirectExchange())
                .with(ORDER_PAID_ROUTING_KEY);
    }
}
```

发送 Direct 消息示例：

```java
rabbitTemplate.convertAndSend("demo.direct.exchange", "order.created", message);
rabbitTemplate.convertAndSend("demo.direct.exchange", "order.paid", message);
```

Direct Exchange 使用建议：

| 场景                     | 建议                     |
| ------------------------ | ------------------------ |
| 业务事件分类明确         | 优先使用 Direct Exchange |
| 一个事件只进入指定队列   | 使用 Direct Exchange     |
| Routing Key 需要完全匹配 | 使用 Direct Exchange     |
| 需要通配符匹配           | 改用 Topic Exchange      |
| 需要广播所有队列         | 改用 Fanout Exchange     |

### Fanout Exchange

Fanout Exchange 会忽略 Routing Key，将消息投递到所有绑定到该交换机的队列。它适合广播类消息场景，例如订单创建后同时通知库存、积分、通知等多个系统。

资源规划如下：

| 资源类型 | 名称                       |
| -------- | -------------------------- |
| Exchange | `demo.fanout.exchange`     |
| Queue    | `demo.fanout.notice.queue` |
| Queue    | `demo.fanout.log.queue`    |
| Queue    | `demo.fanout.stat.queue`   |

Fanout Exchange 配置如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/FanoutExchangeConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fanout Exchange 示例配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class FanoutExchangeConfig {

    public static final String FANOUT_EXCHANGE = "demo.fanout.exchange";
    public static final String NOTICE_QUEUE = "demo.fanout.notice.queue";
    public static final String LOG_QUEUE = "demo.fanout.log.queue";
    public static final String STAT_QUEUE = "demo.fanout.stat.queue";

    @Bean
    public FanoutExchange demoFanoutExchange() {
        return ExchangeBuilder
                .fanoutExchange(FANOUT_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue fanoutNoticeQueue() {
        return QueueBuilder.durable(NOTICE_QUEUE).build();
    }

    @Bean
    public Queue fanoutLogQueue() {
        return QueueBuilder.durable(LOG_QUEUE).build();
    }

    @Bean
    public Queue fanoutStatQueue() {
        return QueueBuilder.durable(STAT_QUEUE).build();
    }

    @Bean
    public Binding fanoutNoticeBinding() {
        return BindingBuilder
                .bind(fanoutNoticeQueue())
                .to(demoFanoutExchange());
    }

    @Bean
    public Binding fanoutLogBinding() {
        return BindingBuilder
                .bind(fanoutLogQueue())
                .to(demoFanoutExchange());
    }

    @Bean
    public Binding fanoutStatBinding() {
        return BindingBuilder
                .bind(fanoutStatQueue())
                .to(demoFanoutExchange());
    }
}
```

发送 Fanout 消息示例：

```java
rabbitTemplate.convertAndSend("demo.fanout.exchange", "", message);
```

Fanout Exchange 的 Routing Key 不参与路由判断，因此通常传空字符串即可。只要队列绑定到了该交换机，就会收到同一条消息。

Fanout Exchange 使用建议：

| 场景                         | 建议                     |
| ---------------------------- | ------------------------ |
| 一条消息需要广播给多个业务方 | 使用 Fanout Exchange     |
| 多个消费者组都要收到同一事件 | 每个消费者组使用独立队列 |
| 不关心 Routing Key           | 使用 Fanout Exchange     |
| 需要精确路由                 | 改用 Direct Exchange     |
| 需要按规则订阅部分消息       | 改用 Topic Exchange      |

### Topic Exchange

Topic Exchange 根据 Routing Key 和 Binding Key 的通配符规则进行路由。它比 Direct Exchange 更灵活，适合按业务域、模块、事件类型、状态等多维度路由消息。

Topic Exchange 支持以下通配符：

| 通配符 | 说明               |
| ------ | ------------------ |
| `*`    | 匹配一个单词       |
| `#`    | 匹配零个或多个单词 |

资源规划如下：

| 资源类型    | 名称                       |
| ----------- | -------------------------- |
| Exchange    | `demo.topic.exchange`      |
| Queue       | `demo.topic.order.queue`   |
| Queue       | `demo.topic.payment.queue` |
| Queue       | `demo.topic.all.queue`     |
| Binding Key | `order.*`                  |
| Binding Key | `payment.*`                |
| Binding Key | `#`                        |

Topic Exchange 配置如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/TopicExchangeConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topic Exchange 示例配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class TopicExchangeConfig {

    public static final String TOPIC_EXCHANGE = "demo.topic.exchange";
    public static final String ORDER_QUEUE = "demo.topic.order.queue";
    public static final String PAYMENT_QUEUE = "demo.topic.payment.queue";
    public static final String ALL_QUEUE = "demo.topic.all.queue";

    @Bean
    public TopicExchange demoTopicExchange() {
        return ExchangeBuilder
                .topicExchange(TOPIC_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue topicOrderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE).build();
    }

    @Bean
    public Queue topicPaymentQueue() {
        return QueueBuilder.durable(PAYMENT_QUEUE).build();
    }

    @Bean
    public Queue topicAllQueue() {
        return QueueBuilder.durable(ALL_QUEUE).build();
    }

    @Bean
    public Binding topicOrderBinding() {
        return BindingBuilder
                .bind(topicOrderQueue())
                .to(demoTopicExchange())
                .with("order.*");
    }

    @Bean
    public Binding topicPaymentBinding() {
        return BindingBuilder
                .bind(topicPaymentQueue())
                .to(demoTopicExchange())
                .with("payment.*");
    }

    @Bean
    public Binding topicAllBinding() {
        return BindingBuilder
                .bind(topicAllQueue())
                .to(demoTopicExchange())
                .with("#");
    }
}
```

发送 Topic 消息示例：

```java
rabbitTemplate.convertAndSend("demo.topic.exchange", "order.created", message);
rabbitTemplate.convertAndSend("demo.topic.exchange", "order.paid", message);
rabbitTemplate.convertAndSend("demo.topic.exchange", "payment.success", message);
rabbitTemplate.convertAndSend("demo.topic.exchange", "payment.failed", message);
```

路由结果示例：

| Routing Key       | 命中队列                                           |
| ----------------- | -------------------------------------------------- |
| `order.created`   | `demo.topic.order.queue`、`demo.topic.all.queue`   |
| `order.paid`      | `demo.topic.order.queue`、`demo.topic.all.queue`   |
| `payment.success` | `demo.topic.payment.queue`、`demo.topic.all.queue` |
| `payment.failed`  | `demo.topic.payment.queue`、`demo.topic.all.queue` |

Topic Exchange 使用建议：

1. Routing Key 层级应稳定，不要频繁调整语义。
2. `#` 匹配范围很大，生产环境谨慎使用。
3. 通配符设计应简单明确，避免消费者收到过多无关消息。
4. 推荐使用 `业务域.事件动作` 或 `业务域.模块.事件动作` 作为命名结构。
5. 如果只需要完全匹配，不要为了灵活性滥用 Topic Exchange。

### Headers Exchange

Headers Exchange 不依赖 Routing Key，而是根据消息头中的键值对进行路由。它适合根据多个属性组合进行匹配的场景，但实际项目中使用频率低于 Direct、Fanout 和 Topic。

Headers Exchange 通过绑定参数中的 `x-match` 控制匹配方式：

| x-match | 说明                           |
| ------- | ------------------------------ |
| `all`   | 消息头必须匹配所有绑定参数     |
| `any`   | 消息头匹配任意一个绑定参数即可 |

资源规划如下：

| 资源类型 | 名称                       |
| -------- | -------------------------- |
| Exchange | `demo.headers.exchange`    |
| Queue    | `demo.headers.sms.queue`   |
| Queue    | `demo.headers.email.queue` |
| Header   | `noticeType=sms`           |
| Header   | `noticeType=email`         |

Headers Exchange 配置如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/HeadersExchangeConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Headers Exchange 示例配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class HeadersExchangeConfig {

    public static final String HEADERS_EXCHANGE = "demo.headers.exchange";
    public static final String SMS_QUEUE = "demo.headers.sms.queue";
    public static final String EMAIL_QUEUE = "demo.headers.email.queue";

    @Bean
    public HeadersExchange demoHeadersExchange() {
        return ExchangeBuilder
                .headersExchange(HEADERS_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue headersSmsQueue() {
        return QueueBuilder.durable(SMS_QUEUE).build();
    }

    @Bean
    public Queue headersEmailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE).build();
    }

    @Bean
    public Binding headersSmsBinding() {
        return BindingBuilder
                .bind(headersSmsQueue())
                .to(demoHeadersExchange())
                .where("noticeType")
                .matches("sms");
    }

    @Bean
    public Binding headersEmailBinding() {
        return BindingBuilder
                .bind(headersEmailQueue())
                .to(demoHeadersExchange())
                .where("noticeType")
                .matches("email");
    }
}
```

发送 Headers 消息需要设置消息头。

```java
rabbitTemplate.convertAndSend(
        "demo.headers.exchange",
        "",
        message,
        rabbitMessage -> {
            rabbitMessage.getMessageProperties().setHeader("noticeType", "sms");
            return rabbitMessage;
        }
);
```

Headers Exchange 使用建议：

| 场景                     | 建议                      |
| ------------------------ | ------------------------- |
| 根据多个消息头属性路由   | 可以使用 Headers Exchange |
| 路由规则与业务字段强相关 | 可以考虑 Headers Exchange |
| 普通业务事件路由         | 优先使用 Direct 或 Topic  |
| 追求简单可维护           | 避免滥用 Headers Exchange |

Headers Exchange 的可读性通常不如 Routing Key，因此除非确实需要基于多个 Header 参数组合匹配，否则建议优先使用 Direct Exchange 或 Topic Exchange。

### Exchange 声明方式

RabbitMQ 中 Exchange 可以通过多种方式声明。开发项目中推荐以代码声明为主，管理控制台和命令行声明为辅。

常见声明方式如下：

| 声明方式                   | 说明                    | 适用场景             |
| -------------------------- | ----------------------- | -------------------- |
| Spring `@Bean` 声明        | 通过配置类声明 Exchange | 应用开发首选         |
| RabbitMQ 管理控制台        | 在 Web 页面手动创建     | 临时调试             |
| `rabbitmqadmin` 命令       | 通过 HTTP API 创建      | 本地测试、脚本化操作 |
| `rabbitmqctl` 命令         | 服务端命令行管理        | 运维管理             |
| Terraform / Helm / Ansible | 基础设施即代码          | 生产环境自动化       |

Spring Bean 声明方式：

```java
@Bean
public DirectExchange orderDirectExchange() {
    return ExchangeBuilder
            .directExchange("order.direct.exchange")
            .durable(true)
            .build();
}
```

`rabbitmqadmin` 命令声明方式：

```bash
rabbitmqadmin -V /dev declare exchange name=order.direct.exchange type=direct durable=true
rabbitmqadmin -V /dev declare exchange name=notice.fanout.exchange type=fanout durable=true
rabbitmqadmin -V /dev declare exchange name=event.topic.exchange type=topic durable=true
```

命令说明：`-V /dev` 指定 Virtual Host，`declare exchange` 表示声明交换机，`name` 指定交换机名称，`type` 指定交换机类型，`durable=true` 表示持久化。

Exchange 参数说明：

| 参数         | 说明                                            |
| ------------ | ----------------------------------------------- |
| `name`       | 交换机名称                                      |
| `type`       | 交换机类型，例如 direct、fanout、topic、headers |
| `durable`    | 是否持久化                                      |
| `autoDelete` | 最后一个绑定删除后是否自动删除                  |
| `internal`   | 是否仅允许 Exchange 内部转发                    |
| `arguments`  | 扩展参数，例如延迟交换机参数                    |

声明建议：

1. 开发环境可以由应用启动自动声明。
2. 测试环境应保持声明方式与生产环境一致。
3. 生产环境建议通过变更脚本或基础设施工具声明核心资源。
4. 应用代码中的声明应与实际 RabbitMQ 资源参数保持一致。
5. 不要在生产环境随意删除并重建已有 Exchange。

### Exchange 使用场景

不同 Exchange 类型适合不同业务场景。实际项目中应根据消息投递范围、路由精度、订阅灵活度和维护成本进行选择。

Exchange 选型建议如下：

| Exchange 类型    | 适用场景               | 示例                             |
| ---------------- | ---------------------- | -------------------------------- |
| Direct Exchange  | 精确匹配 Routing Key   | 订单创建、支付成功、退款失败     |
| Fanout Exchange  | 广播给所有绑定队列     | 系统公告、缓存刷新、事件广播     |
| Topic Exchange   | 通配符匹配 Routing Key | 事件总线、日志分类、多业务订阅   |
| Headers Exchange | 根据消息头匹配         | 按渠道、租户、区域等 Header 路由 |
| Delayed Exchange | 延迟投递消息           | 订单超时关闭、延迟通知           |

业务选型示例：

| 业务需求                                     | 推荐类型             | 原因              |
| -------------------------------------------- | -------------------- | ----------------- |
| 订单创建后只通知库存服务                     | Direct               | 路由目标明确      |
| 订单创建后通知库存、积分、短信多个服务       | Fanout               | 广播同一事件      |
| 不同服务订阅 `order.*`、`payment.*` 类型事件 | Topic                | 支持通配符订阅    |
| 根据 `tenantId`、`channel` 消息头路由        | Headers              | Header 匹配更直接 |
| 30 分钟后关闭未支付订单                      | Delayed / TTL + 死信 | 支持延迟处理      |

项目实践建议：

1. 单一业务事件精确投递，优先使用 Direct Exchange。
2. 一条消息需要多个业务方独立处理，使用 Fanout Exchange。
3. 需要按照业务域、事件类型灵活订阅，使用 Topic Exchange。
4. Headers Exchange 使用门槛较高，只有在 Header 路由明显优于 Routing Key 时使用。
5. 不同业务域建议使用不同 Exchange，避免一个 Exchange 承载过多无关业务。
6. Exchange 命名建议包含业务域和类型，例如 `order.direct.exchange`、`notice.fanout.exchange`、`event.topic.exchange`。
7. 生产环境应统一维护 Exchange、Queue、Binding 的命名规范和变更流程。


## Queue 开发实践

本章节用于说明 RabbitMQ 队列的常见开发方式。Queue 是消息真正存储的位置，Exchange 只负责路由，消息最终需要进入 Queue 后才能被 Consumer 消费。队列设计需要重点关注是否持久化、是否自动删除、是否排他、是否支持优先级、是否配置 TTL、是否绑定死信交换机等参数。

### 普通队列

普通队列是 RabbitMQ 中最基础的队列类型，主要用于保存待消费消息。普通队列可以是持久化的，也可以是非持久化的；可以被多个消费者共同消费，也可以只被单个消费者监听。

普通队列适合以下场景：

| 场景         | 说明                             |
| ------------ | -------------------------------- |
| 普通异步任务 | 例如发送通知、写入日志、同步状态 |
| 业务事件消费 | 例如订单创建、支付成功、库存扣减 |
| 本地开发测试 | 用于验证消息发送、路由和消费流程 |
| 常规任务分发 | 多个消费者共同消费同一个队列     |

普通队列配置示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/QueuePracticeConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 队列实践配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class QueuePracticeConfig {

    public static final String NORMAL_QUEUE = "practice.normal.queue";

    /**
     * 声明普通持久化队列
     *
     * @return 普通队列
     */
    @Bean
    public Queue normalQueue() {
        return new Queue(
                NORMAL_QUEUE,
                true,
                false,
                false
        );
    }
}
```

`Queue` 构造参数说明：

| 参数         | 说明         |
| ------------ | ------------ |
| `name`       | 队列名称     |
| `durable`    | 是否持久化   |
| `exclusive`  | 是否排他     |
| `autoDelete` | 是否自动删除 |

普通业务队列通常建议设置为 `durable=true`、`exclusive=false`、`autoDelete=false`，这样 Broker 重启后队列仍然存在，并且可以被多个消费者实例共同消费。

### 持久化队列

持久化队列表示队列元数据会保存到磁盘中。RabbitMQ Broker 重启后，持久化队列仍然存在。需要注意的是，队列持久化只保证队列本身存在，并不等于消息一定持久化。消息也需要设置为持久化消息，才能降低 Broker 重启导致消息丢失的风险。

持久化能力需要同时关注三点：

| 对象     | 要求                      |
| -------- | ------------------------- |
| Exchange | `durable=true`            |
| Queue    | `durable=true`            |
| Message  | `deliveryMode=PERSISTENT` |

持久化队列配置示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/DurableQueueConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 持久化队列配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class DurableQueueConfig {

    public static final String DURABLE_QUEUE = "practice.durable.queue";

    /**
     * 声明持久化队列
     *
     * @return 持久化队列
     */
    @Bean
    public Queue durableQueue() {
        return QueueBuilder
                .durable(DURABLE_QUEUE)
                .build();
    }
}
```

发送持久化消息示例。

```java
rabbitTemplate.convertAndSend(
        "practice.direct.exchange",
        "practice.durable",
        message,
        rabbitMessage -> {
            rabbitMessage.getMessageProperties().setDeliveryMode(
                    org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT
            );
            return rabbitMessage;
        }
);
```

在 Spring AMQP 中，通过 `convertAndSend` 发送对象消息时，通常会使用持久化消息属性；但在可靠性要求较高的业务中，建议显式设置消息持久化，并配合 Publisher Confirm 确认消息是否到达 Exchange。

持久化队列适合订单、支付、库存、通知、任务调度等核心业务场景。不重要的临时通知、测试消息、短生命周期数据可以根据情况使用非持久化队列。

### 临时队列

临时队列通常用于短生命周期场景，例如临时订阅、调试、RPC 回复队列、一次性任务监听等。临时队列一般不持久化，并且可以设置为自动删除。

临时队列常见特征如下：

| 特征               | 说明                         |
| ------------------ | ---------------------------- |
| 非持久化           | Broker 重启后队列不存在      |
| 自动删除           | 最后一个消费者断开后自动删除 |
| 名称可自动生成     | 适合临时消费场景             |
| 不保存长期业务消息 | 不适合核心业务消息存储       |

临时队列配置示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/TemporaryQueueConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 临时队列配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class TemporaryQueueConfig {

    /**
     * 声明匿名临时队列
     *
     * @return 匿名临时队列
     */
    @Bean
    public Queue temporaryQueue() {
        return new AnonymousQueue();
    }
}
```

也可以声明一个固定名称的临时队列。

```java
@Bean
public Queue fixedTemporaryQueue() {
    return new Queue(
            "practice.temporary.queue",
            false,
            false,
            true
    );
}
```

固定名称临时队列参数说明：

| 参数         | 值      | 说明               |
| ------------ | ------- | ------------------ |
| `durable`    | `false` | 队列不持久化       |
| `exclusive`  | `false` | 不限制当前连接独占 |
| `autoDelete` | `true`  | 无消费者后自动删除 |

临时队列不适合保存订单、支付、库存等核心业务消息。如果消费者断开、Broker 重启或队列被自动删除，临时队列中的消息可能丢失。

### 排他队列

排他队列是只能被声明它的连接使用的队列。当该连接关闭后，排他队列会自动删除。排他队列通常用于客户端私有队列、RPC 临时回复队列、临时监听等场景。

排他队列特征如下：

| 特征             | 说明                               |
| ---------------- | ---------------------------------- |
| 连接独占         | 只能被当前连接使用                 |
| 自动删除         | 连接断开后队列删除                 |
| 不适合多实例消费 | 多个应用实例无法共享同一个排他队列 |
| 常用于临时场景   | 适合 RPC Reply Queue 或临时任务    |

排他队列配置示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/ExclusiveQueueConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 排他队列配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class ExclusiveQueueConfig {

    public static final String EXCLUSIVE_QUEUE = "practice.exclusive.queue";

    /**
     * 声明排他队列
     *
     * @return 排他队列
     */
    @Bean
    public Queue exclusiveQueue() {
        return new Queue(
                EXCLUSIVE_QUEUE,
                false,
                true,
                true
        );
    }
}
```

排他队列参数说明：

| 参数         | 值      | 说明               |
| ------------ | ------- | ------------------ |
| `durable`    | `false` | 通常不持久化       |
| `exclusive`  | `true`  | 当前连接独占       |
| `autoDelete` | `true`  | 连接断开后自动删除 |

排他队列使用注意事项：

1. 不要将核心业务队列设置为排他队列。
2. 不要在多实例消费者中使用固定名称排他队列。
3. 排他队列适合临时连接场景，不适合稳定业务消费。
4. 如果应用重启后需要继续消费历史消息，不应使用排他队列。

### 优先级队列

优先级队列允许消息带有优先级属性，RabbitMQ 会尽量优先投递高优先级消息。优先级队列需要在声明队列时设置 `x-max-priority` 参数，消息发送时再设置 `priority` 属性。

优先级队列适合以下场景：

| 场景                 | 说明                     |
| -------------------- | ------------------------ |
| 重要通知优先发送     | 例如紧急短信、告警消息   |
| VIP 用户任务优先处理 | 高等级用户任务先消费     |
| 运维控制消息优先处理 | 控制指令优先于普通任务   |
| 重试任务分级处理     | 高优先级补偿任务优先执行 |

优先级队列配置示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/PriorityQueueConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 优先级队列配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class PriorityQueueConfig {

    public static final String PRIORITY_QUEUE = "practice.priority.queue";

    /**
     * 声明优先级队列
     *
     * @return 优先级队列
     */
    @Bean
    public Queue priorityQueue() {
        return QueueBuilder
                .durable(PRIORITY_QUEUE)
                // 最大优先级，常用范围为 0 到 10
                .maxPriority(10)
                .build();
    }
}
```

发送带优先级的消息。

```java
rabbitTemplate.convertAndSend(
        "practice.direct.exchange",
        "practice.priority",
        message,
        rabbitMessage -> {
            // 优先级范围需要小于等于队列 x-max-priority
            rabbitMessage.getMessageProperties().setPriority(8);
            return rabbitMessage;
        }
);
```

优先级队列使用注意事项：

1. 只有队列声明了 `x-max-priority`，消息优先级才会生效。
2. 优先级不是严格实时抢占，已经投递给消费者的消息不会被后续高优先级消息抢占。
3. `prefetch` 过大时，消费者可能提前拉取大量低优先级消息，削弱优先级效果。
4. 优先级等级不宜设置过多，常用 `5` 或 `10` 即可。
5. 高优先级消息过多时，低优先级消息可能长期得不到消费，需要业务上控制比例。

### 队列参数配置

队列参数用于扩展队列能力，例如消息 TTL、队列最大长度、死信交换机、优先级、懒加载队列等。实际项目中，核心业务队列通常需要配置死信交换机和必要的容量限制。

常见队列参数如下：

| 参数                        | 说明                           |
| --------------------------- | ------------------------------ |
| `x-message-ttl`             | 队列内消息过期时间，单位毫秒   |
| `x-expires`                 | 队列空闲自动删除时间，单位毫秒 |
| `x-max-length`              | 队列最大消息数量               |
| `x-max-length-bytes`        | 队列最大占用字节数             |
| `x-dead-letter-exchange`    | 死信交换机                     |
| `x-dead-letter-routing-key` | 死信 Routing Key               |
| `x-max-priority`            | 队列最大优先级                 |
| `x-queue-mode`              | 队列模式，例如 lazy            |
| `x-queue-type`              | 队列类型，例如 classic、quorum |

综合队列参数配置示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/QueueArgumentConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 队列参数配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class QueueArgumentConfig {

    public static final String BUSINESS_QUEUE = "practice.business.queue";
    public static final String DEAD_LETTER_EXCHANGE = "practice.dead.exchange";
    public static final String DEAD_LETTER_ROUTING_KEY = "practice.dead";

    /**
     * 声明带参数的业务队列
     *
     * @return 业务队列
     */
    @Bean
    public Queue businessQueue() {
        return QueueBuilder
                .durable(BUSINESS_QUEUE)
                // 消息在队列中最多保留 60 秒
                .ttl(60_000)
                // 队列最多保留 10000 条消息
                .maxLength(10_000)
                // 消息成为死信后投递到指定死信交换机
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                // 消息成为死信后使用指定死信路由键
                .deadLetterRoutingKey(DEAD_LETTER_ROUTING_KEY)
                // 设置最大优先级
                .maxPriority(10)
                .build();
    }
}
```

队列参数配置建议：

| 参数类型   | 建议                                         |
| ---------- | -------------------------------------------- |
| TTL        | 延迟、超时、临时消息场景使用                 |
| 最大长度   | 防止异常情况下队列无限堆积                   |
| 死信交换机 | 核心业务队列建议配置                         |
| 优先级     | 只有明确优先级业务时配置                     |
| 队列类型   | 高可用场景优先评估 Quorum Queue              |
| Lazy Queue | 大量堆积场景可评估，但需要结合版本和性能测试 |

需要注意：队列创建后，部分参数不能直接修改。如果 RabbitMQ 中已有同名队列，而代码声明的参数与已有队列不一致，应用启动时可能出现 `PRECONDITION_FAILED` 异常。生产环境修改队列参数应走变更流程，必要时创建新队列并迁移流量。

## Binding 开发实践

本章节用于说明 RabbitMQ 中 Exchange 与 Queue 的绑定实践。Binding 决定消息从 Exchange 到 Queue 的路由关系，是 RabbitMQ 路由设计的核心。不同 Exchange 类型对应不同 Binding 规则。

### Direct Binding

Direct Binding 用于 Direct Exchange。它要求消息的 Routing Key 与 Binding Key 完全一致，消息才会被路由到对应队列。

Direct Binding 适合以下场景：

| 场景                 | 说明                               |
| -------------------- | ---------------------------------- |
| 精确事件分发         | 例如 `order.created`、`order.paid` |
| 不同状态进入不同队列 | 创建、支付、取消分别消费           |
| 单一业务队列         | 一个 Routing Key 对应一个目标队列  |
| 关键业务隔离         | 不同事件使用不同队列处理           |

Direct Binding 配置示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/DirectBindingConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Direct Binding 配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class DirectBindingConfig {

    public static final String ORDER_DIRECT_EXCHANGE = "binding.order.direct.exchange";
    public static final String ORDER_CREATED_QUEUE = "binding.order.created.queue";
    public static final String ORDER_PAID_QUEUE = "binding.order.paid.queue";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String ORDER_PAID_ROUTING_KEY = "order.paid";

    @Bean
    public DirectExchange bindingOrderDirectExchange() {
        return ExchangeBuilder
                .directExchange(ORDER_DIRECT_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue bindingOrderCreatedQueue() {
        return QueueBuilder
                .durable(ORDER_CREATED_QUEUE)
                .build();
    }

    @Bean
    public Queue bindingOrderPaidQueue() {
        return QueueBuilder
                .durable(ORDER_PAID_QUEUE)
                .build();
    }

    @Bean
    public Binding bindingOrderCreatedQueueToDirectExchange() {
        return BindingBuilder
                .bind(bindingOrderCreatedQueue())
                .to(bindingOrderDirectExchange())
                .with(ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding bindingOrderPaidQueueToDirectExchange() {
        return BindingBuilder
                .bind(bindingOrderPaidQueue())
                .to(bindingOrderDirectExchange())
                .with(ORDER_PAID_ROUTING_KEY);
    }
}
```

发送示例：

```java
rabbitTemplate.convertAndSend("binding.order.direct.exchange", "order.created", message);
rabbitTemplate.convertAndSend("binding.order.direct.exchange", "order.paid", message);
```

路由结果如下：

| Routing Key       | 目标队列                            |
| ----------------- | ----------------------------------- |
| `order.created`   | `binding.order.created.queue`       |
| `order.paid`      | `binding.order.paid.queue`          |
| `order.cancelled` | 无匹配队列，可能触发 ReturnCallback |

Direct Binding 的核心是“完全匹配”。如果 Routing Key 写错，或者 Binding Key 和发送时的 Routing Key 不一致，消息不会进入目标队列。

### Fanout Binding

Fanout Binding 用于 Fanout Exchange。Fanout Exchange 会忽略 Routing Key，将消息投递到所有绑定队列。

Fanout Binding 适合以下场景：

| 场景               | 说明                     |
| ------------------ | ------------------------ |
| 广播事件           | 一条消息被多个业务方接收 |
| 多消费者组独立消费 | 每个消费者组有独立队列   |
| 缓存刷新           | 多个服务同时接收刷新通知 |
| 日志分发           | 一份日志进入多个处理链路 |

Fanout Binding 配置示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/FanoutBindingConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Fanout Binding 配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class FanoutBindingConfig {

    public static final String ORDER_FANOUT_EXCHANGE = "binding.order.fanout.exchange";
    public static final String INVENTORY_QUEUE = "binding.order.inventory.queue";
    public static final String NOTICE_QUEUE = "binding.order.notice.queue";
    public static final String POINT_QUEUE = "binding.order.point.queue";

    @Bean
    public FanoutExchange bindingOrderFanoutExchange() {
        return ExchangeBuilder
                .fanoutExchange(ORDER_FANOUT_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue bindingInventoryQueue() {
        return QueueBuilder.durable(INVENTORY_QUEUE).build();
    }

    @Bean
    public Queue bindingNoticeQueue() {
        return QueueBuilder.durable(NOTICE_QUEUE).build();
    }

    @Bean
    public Queue bindingPointQueue() {
        return QueueBuilder.durable(POINT_QUEUE).build();
    }

    @Bean
    public Binding bindingInventoryQueueToFanoutExchange() {
        return BindingBuilder
                .bind(bindingInventoryQueue())
                .to(bindingOrderFanoutExchange());
    }

    @Bean
    public Binding bindingNoticeQueueToFanoutExchange() {
        return BindingBuilder
                .bind(bindingNoticeQueue())
                .to(bindingOrderFanoutExchange());
    }

    @Bean
    public Binding bindingPointQueueToFanoutExchange() {
        return BindingBuilder
                .bind(bindingPointQueue())
                .to(bindingOrderFanoutExchange());
    }
}
```

发送示例：

```java
rabbitTemplate.convertAndSend("binding.order.fanout.exchange", "", message);
```

Fanout Binding 路由结果：

| 队列                            | 是否收到消息 |
| ------------------------------- | ------------ |
| `binding.order.inventory.queue` | 是           |
| `binding.order.notice.queue`    | 是           |
| `binding.order.point.queue`     | 是           |

Fanout Binding 使用注意事项：

1. Routing Key 不参与路由，发送时通常传空字符串。
2. 每个消费者组应使用独立队列，否则会变成竞争消费。
3. Fanout Exchange 适合广播，不适合精确筛选。
4. 如果广播范围越来越复杂，应考虑改用 Topic Exchange。

### Topic Binding

Topic Binding 用于 Topic Exchange。它通过通配符 Binding Key 匹配消息 Routing Key，适合灵活订阅不同业务事件。

Topic Binding 通配符如下：

| 通配符 | 说明               |
| ------ | ------------------ |
| `*`    | 匹配一个单词       |
| `#`    | 匹配零个或多个单词 |

Topic Binding 配置示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/TopicBindingConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Topic Binding 配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class TopicBindingConfig {

    public static final String EVENT_TOPIC_EXCHANGE = "binding.event.topic.exchange";
    public static final String ORDER_EVENT_QUEUE = "binding.event.order.queue";
    public static final String PAYMENT_EVENT_QUEUE = "binding.event.payment.queue";
    public static final String ALL_EVENT_QUEUE = "binding.event.all.queue";

    @Bean
    public TopicExchange bindingEventTopicExchange() {
        return ExchangeBuilder
                .topicExchange(EVENT_TOPIC_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue bindingOrderEventQueue() {
        return QueueBuilder.durable(ORDER_EVENT_QUEUE).build();
    }

    @Bean
    public Queue bindingPaymentEventQueue() {
        return QueueBuilder.durable(PAYMENT_EVENT_QUEUE).build();
    }

    @Bean
    public Queue bindingAllEventQueue() {
        return QueueBuilder.durable(ALL_EVENT_QUEUE).build();
    }

    @Bean
    public Binding bindingOrderEventQueueToTopicExchange() {
        return BindingBuilder
                .bind(bindingOrderEventQueue())
                .to(bindingEventTopicExchange())
                .with("order.*");
    }

    @Bean
    public Binding bindingPaymentEventQueueToTopicExchange() {
        return BindingBuilder
                .bind(bindingPaymentEventQueue())
                .to(bindingEventTopicExchange())
                .with("payment.*");
    }

    @Bean
    public Binding bindingAllEventQueueToTopicExchange() {
        return BindingBuilder
                .bind(bindingAllEventQueue())
                .to(bindingEventTopicExchange())
                .with("#");
    }
}
```

发送示例：

```java
rabbitTemplate.convertAndSend("binding.event.topic.exchange", "order.created", message);
rabbitTemplate.convertAndSend("binding.event.topic.exchange", "order.paid", message);
rabbitTemplate.convertAndSend("binding.event.topic.exchange", "payment.success", message);
```

路由结果如下：

| Routing Key       | 命中队列                                                 |
| ----------------- | -------------------------------------------------------- |
| `order.created`   | `binding.event.order.queue`、`binding.event.all.queue`   |
| `order.paid`      | `binding.event.order.queue`、`binding.event.all.queue`   |
| `payment.success` | `binding.event.payment.queue`、`binding.event.all.queue` |
| `stock.deducted`  | `binding.event.all.queue`                                |

Topic Binding 使用建议：

1. Routing Key 层级要稳定，例如 `业务域.事件动作`。
2. `*` 用于匹配固定层级中的一个单词。
3. `#` 匹配范围很大，应谨慎使用。
4. 不建议使用过深层级，避免 Binding Key 难以维护。
5. 如果所有 Binding 都是精确匹配，优先使用 Direct Exchange。

### 多队列绑定

多队列绑定表示一个 Exchange 绑定多个 Queue。根据 Exchange 类型不同，同一条消息可能进入一个队列，也可能进入多个队列。

常见多队列绑定场景如下：

| 场景       | 说明                                     |
| ---------- | ---------------------------------------- |
| 广播消息   | 一个事件进入多个业务队列                 |
| 事件分流   | 不同 Routing Key 进入不同队列            |
| 统一审计   | 所有事件除进入业务队列外，还进入审计队列 |
| 多业务订阅 | 多个业务系统订阅同一事件源               |

Direct Exchange 多队列绑定示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/MultiQueueBindingConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 多队列绑定配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class MultiQueueBindingConfig {

    public static final String ORDER_EXCHANGE = "binding.multi.order.exchange";
    public static final String ORDER_BUSINESS_QUEUE = "binding.multi.order.business.queue";
    public static final String ORDER_AUDIT_QUEUE = "binding.multi.order.audit.queue";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";

    @Bean
    public DirectExchange bindingMultiOrderExchange() {
        return ExchangeBuilder
                .directExchange(ORDER_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue bindingMultiOrderBusinessQueue() {
        return QueueBuilder
                .durable(ORDER_BUSINESS_QUEUE)
                .build();
    }

    @Bean
    public Queue bindingMultiOrderAuditQueue() {
        return QueueBuilder
                .durable(ORDER_AUDIT_QUEUE)
                .build();
    }

    @Bean
    public Binding bindingOrderBusinessQueue() {
        return BindingBuilder
                .bind(bindingMultiOrderBusinessQueue())
                .to(bindingMultiOrderExchange())
                .with(ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding bindingOrderAuditQueue() {
        return BindingBuilder
                .bind(bindingMultiOrderAuditQueue())
                .to(bindingMultiOrderExchange())
                .with(ORDER_CREATED_ROUTING_KEY);
    }
}
```

发送示例：

```java
rabbitTemplate.convertAndSend("binding.multi.order.exchange", "order.created", message);
```

在该示例中，`binding.multi.order.business.queue` 和 `binding.multi.order.audit.queue` 都绑定了相同 Routing Key，因此一条 `order.created` 消息会同时进入两个队列。

多队列绑定注意事项：

1. 多个队列收到的是各自独立的消息副本。
2. 每个队列的消费进度互不影响。
3. 业务队列消费失败不会影响审计队列消费。
4. 如果多个消费者监听同一个队列，则是竞争消费，不是广播消费。
5. 多队列绑定会增加消息存储量，需要关注磁盘和队列深度。

### 多 Exchange 绑定

RabbitMQ 支持 Exchange 与 Exchange 之间绑定，也支持一个 Queue 绑定到多个 Exchange。多 Exchange 绑定适合消息汇聚、路由转发、事件总线等场景。

常见使用方式如下：

| 方式                         | 说明                                      |
| ---------------------------- | ----------------------------------------- |
| 一个 Queue 绑定多个 Exchange | 队列接收多个消息来源                      |
| Exchange 绑定 Exchange       | 消息从一个 Exchange 转发到另一个 Exchange |
| Topic Exchange 汇聚事件      | 多业务事件汇聚到统一事件队列              |
| Fanout 转 Topic              | 广播事件后再按主题分流                    |

一个 Queue 绑定多个 Exchange 示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/MultiExchangeBindingConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 多 Exchange 绑定配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class MultiExchangeBindingConfig {

    public static final String ORDER_EXCHANGE = "binding.multi.exchange.order";
    public static final String PAYMENT_EXCHANGE = "binding.multi.exchange.payment";
    public static final String AUDIT_QUEUE = "binding.multi.exchange.audit.queue";

    @Bean
    public DirectExchange bindingOrderSourceExchange() {
        return ExchangeBuilder
                .directExchange(ORDER_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public DirectExchange bindingPaymentSourceExchange() {
        return ExchangeBuilder
                .directExchange(PAYMENT_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue bindingAuditQueue() {
        return QueueBuilder
                .durable(AUDIT_QUEUE)
                .build();
    }

    @Bean
    public Binding bindingAuditQueueToOrderExchange() {
        return BindingBuilder
                .bind(bindingAuditQueue())
                .to(bindingOrderSourceExchange())
                .with("order.created");
    }

    @Bean
    public Binding bindingAuditQueueToPaymentExchange() {
        return BindingBuilder
                .bind(bindingAuditQueue())
                .to(bindingPaymentSourceExchange())
                .with("payment.success");
    }
}
```

Exchange 绑定 Exchange 示例。

```java
@Bean
public Binding bindingExchangeToExchange(
        TopicExchange sourceExchange,
        DirectExchange targetExchange
) {
    return BindingBuilder
            .bind(targetExchange)
            .to(sourceExchange)
            .with("order.#");
}
```

多 Exchange 绑定适合有明确消息汇聚需求的场景，例如审计服务需要同时接收订单事件和支付事件。但不建议为了“省队列”或“图方便”随意绑定多个 Exchange，否则后续消息来源会变得不清晰，排查问题困难。

### Routing Key 设计

Routing Key 是 RabbitMQ 路由设计中最重要的命名元素之一。好的 Routing Key 可以让消息流向清晰、Binding 简单、消费者职责明确；不合理的 Routing Key 会导致路由混乱、绑定关系复杂、消息误投递或无法路由。

推荐命名格式：

```text
业务域.事件动作
```

或在需要更细粒度时使用：

```text
业务域.业务对象.事件动作
```

常见示例：

```text
order.created
order.paid
order.cancelled
payment.success
payment.failed
stock.deducted
notice.sms.send
notice.email.send
```

对于 Topic Exchange，可以使用更完整的层级：

```text
业务域.模块.动作.状态
```

示例：

```text
order.pay.status.success
order.pay.status.failed
payment.refund.status.success
payment.refund.status.failed
```

Routing Key 设计建议如下：

| 建议           | 说明                                         |
| -------------- | -------------------------------------------- |
| 使用小写字母   | 避免大小写混用导致匹配错误                   |
| 使用点号分隔   | 便于 Topic Exchange 通配符匹配               |
| 保持业务语义   | 名称应能直接表达事件含义                     |
| 避免过度缩写   | `order.created` 比 `od.crt` 更清晰           |
| 层级保持稳定   | 不要频繁调整层级含义                         |
| 避免包含环境名 | 环境应通过 vhost 隔离，不放到 Routing Key    |
| 避免包含随机值 | 订单号、用户 ID 等动态值不应放入 Routing Key |
| 区分命令和事件 | 命令表示要做什么，事件表示已经发生什么       |

推荐命名对照：

| 类型         | 推荐                    | 不推荐          |
| ------------ | ----------------------- | --------------- |
| 订单创建事件 | `order.created`         | `orderCreate`   |
| 支付成功事件 | `payment.success`       | `pay_ok`        |
| 短信发送命令 | `notice.sms.send`       | `sendSmsToUser` |
| 库存扣减事件 | `stock.deducted`        | `stock_001`     |
| 退款失败事件 | `payment.refund.failed` | `refundFailMsg` |

Direct Exchange 中的 Routing Key 应尽量精确：

```text
order.created
order.paid
order.cancelled
```

Topic Exchange 中的 Routing Key 应便于订阅：

```text
order.created
order.paid
payment.success
payment.failed
```

对应 Binding Key：

```text
order.*
payment.*
*.success
#
```

Routing Key 设计注意事项：

1. 不要将用户 ID、订单 ID、租户 ID 等高基数字段放入 Routing Key。
2. 不要让 Routing Key 同时表达多个不相关语义。
3. 不要让一个 Routing Key 对应多个含义不同的消息体。
4. Topic Exchange 的层级不宜过深，一般控制在 2 到 4 层。
5. 需要按租户、渠道、区域路由时，优先评估是否应使用独立 vhost、独立 Exchange、Header 或业务字段，而不是直接把所有维度堆到 Routing Key 中。

较合理的项目级 Routing Key 规划如下：

| 业务域 | Routing Key             | 说明           |
| ------ | ----------------------- | -------------- |
| 订单   | `order.created`         | 订单已创建     |
| 订单   | `order.paid`            | 订单已支付     |
| 订单   | `order.cancelled`       | 订单已取消     |
| 支付   | `payment.success`       | 支付成功       |
| 支付   | `payment.failed`        | 支付失败       |
| 库存   | `stock.deducted`        | 库存已扣减     |
| 通知   | `notice.sms.send`       | 发送短信通知   |
| 通知   | `notice.email.send`     | 发送邮件通知   |
| 日志   | `log.operation.created` | 操作日志已产生 |
| 审计   | `audit.event.created`   | 审计事件已产生 |

在正式项目中，建议将 Exchange、Queue、Routing Key 统一收敛到常量类或配置表中，并在开发规范中明确命名规则，避免不同模块各自定义风格不一致的路由键。


## 消息生产者开发

本章节用于说明 RabbitMQ 生产者侧的标准开发方式。生产者不应只是在业务代码中直接调用 `RabbitTemplate.convertAndSend`，而应围绕消息构造、消息 ID、消息头、发送日志、Confirm 回调、Return 回调和异常处理建立统一封装。

### 生产者模块结构

生产者模块用于承接业务系统中的消息发送能力。它应当屏蔽 RabbitMQ 的底层发送细节，让业务 Service 只关注“发送什么业务消息”，而不是关心 Exchange、Routing Key、消息头、Confirm、Return 等细节。

推荐模块结构如下：

```text
src/main/java/io/github/atengk/rabbitmq/
├── config/
│   ├── RabbitMqProducerConfig.java
│   └── RabbitTemplateCallbackInitializer.java
├── constant/
│   └── RabbitMqProducerConstant.java
├── dto/
│   ├── MqSendRequest.java
│   ├── MqSendResult.java
│   └── OrderPaidMessage.java
├── enums/
│   └── MqSendStatusEnum.java
├── producer/
│   ├── MessageSendService.java
│   ├── RabbitMessageSendService.java
│   └── OrderMessageProducer.java
└── util/
    └── MqMessageIdUtil.java
```

目录职责说明如下：

| 目录或类   | 说明                                            |
| ---------- | ----------------------------------------------- |
| `config`   | 生产者侧 RabbitTemplate、Confirm、Return 等配置 |
| `constant` | Exchange、Routing Key、消息头名称等常量         |
| `dto`      | 消息发送请求、发送结果、业务消息体              |
| `enums`    | 消息发送状态枚举                                |
| `producer` | 统一发送接口、RabbitTemplate 封装、业务生产者   |
| `util`     | 消息 ID、消息头、公共工具方法                   |

生产者模块设计原则：

1. 业务代码不直接操作 `RabbitTemplate`。
2. Exchange、Routing Key 不在业务方法中硬编码。
3. 每条消息必须有全局唯一 `messageId`。
4. 发送消息时必须记录发送日志。
5. 核心业务消息必须配置 Publisher Confirm。
6. 不可路由消息必须通过 ReturnCallback 记录或补偿。
7. 发送方法应返回明确结果，便于接口层或业务层判断。

### 消息发送服务设计

消息发送服务用于定义统一的发送入口。不同业务模块可以复用同一套发送能力，只需要传入 Exchange、Routing Key、消息体、业务标识和消息头即可。

文件位置：`src/main/java/io/github/atengk/rabbitmq/dto/MqSendRequest.java`

```java
package io.github.atengk.rabbitmq.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * MQ 消息发送请求
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
public class MqSendRequest<T> {

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 交换机
     */
    private String exchange;

    /**
     * 路由键
     */
    private String routingKey;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 业务主键
     */
    private String businessKey;

    /**
     * 消息体
     */
    private T payload;

    /**
     * 消息头
     */
    private Map<String, Object> headers;
}
```

文件位置：`src/main/java/io/github/atengk/rabbitmq/dto/MqSendResult.java`

```java
package io.github.atengk.rabbitmq.dto;

import lombok.Builder;
import lombok.Data;

/**
 * MQ 消息发送结果
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
public class MqSendResult {

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 是否提交到发送流程
     */
    private Boolean submitted;

    /**
     * 交换机
     */
    private String exchange;

    /**
     * 路由键
     */
    private String routingKey;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 业务主键
     */
    private String businessKey;

    /**
     * 结果说明
     */
    private String message;
}
```

文件位置：`src/main/java/io/github/atengk/rabbitmq/enums/MqSendStatusEnum.java`

```java
package io.github.atengk.rabbitmq.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * MQ 消息发送状态枚举
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Getter
@AllArgsConstructor
public enum MqSendStatusEnum {

    SUBMITTED("submitted", "已提交发送"),

    CONFIRMED("confirmed", "交换机确认成功"),

    CONFIRM_FAILED("confirm_failed", "交换机确认失败"),

    RETURNED("returned", "消息不可路由"),

    SEND_FAILED("send_failed", "发送异常");

    private final String code;

    private final String description;
}
```

统一发送接口如下。业务生产者只依赖该接口，不直接依赖 `RabbitTemplate`。

文件位置：`src/main/java/io/github/atengk/rabbitmq/producer/MessageSendService.java`

```java
package io.github.atengk.rabbitmq.producer;

import io.github.atengk.rabbitmq.dto.MqSendRequest;
import io.github.atengk.rabbitmq.dto.MqSendResult;

/**
 * MQ 消息发送服务
 *
 * @author Ateng
 * @since 2026-05-11
 */
public interface MessageSendService {

    /**
     * 发送普通消息
     *
     * @param request 消息发送请求
     * @return 消息发送结果
     */
    MqSendResult send(MqSendRequest<?> request);
}
```

该接口的返回结果只表示消息已经提交到 RabbitMQ 发送流程，不代表消费者已经处理成功。消费者处理结果应通过消费日志、业务状态表或消息记录表判断。

### RabbitTemplate 封装

`RabbitTemplate` 封装用于统一处理参数校验、消息 ID 生成、消息属性设置、消息头设置、日志记录和异常捕获。这样可以避免每个业务生产者重复编写相同逻辑。

文件位置：`src/main/java/io/github/atengk/rabbitmq/constant/RabbitMqProducerConstant.java`

```java
package io.github.atengk.rabbitmq.constant;

/**
 * RabbitMQ 生产者常量
 *
 * @author Ateng
 * @since 2026-05-11
 */
public final class RabbitMqProducerConstant {

    private RabbitMqProducerConstant() {
    }

    public static final String HEADER_MESSAGE_ID = "x-message-id";

    public static final String HEADER_BUSINESS_TYPE = "x-business-type";

    public static final String HEADER_BUSINESS_KEY = "x-business-key";

    public static final String HEADER_SEND_TIME = "x-send-time";

    public static final String ORDER_EXCHANGE = "producer.order.exchange";

    public static final String ORDER_PAID_ROUTING_KEY = "order.paid";
}
```

文件位置：`src/main/java/io/github/atengk/rabbitmq/util/MqMessageIdUtil.java`

```java
package io.github.atengk.rabbitmq.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

/**
 * MQ 消息ID工具类
 *
 * @author Ateng
 * @since 2026-05-11
 */
public final class MqMessageIdUtil {

    private MqMessageIdUtil() {
    }

    /**
     * 生成消息ID
     *
     * @param businessType 业务类型
     * @return 消息ID
     */
    public static String generate(String businessType) {
        String prefix = StrUtil.blankToDefault(businessType, "mq");
        return StrUtil.format("{}_{}_{}", prefix, DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss"), IdUtil.fastSimpleUUID());
    }
}
```

文件位置：`src/main/java/io/github/atengk/rabbitmq/producer/RabbitMessageSendService.java`

```java
package io.github.atengk.rabbitmq.producer;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.rabbitmq.constant.RabbitMqProducerConstant;
import io.github.atengk.rabbitmq.dto.MqSendRequest;
import io.github.atengk.rabbitmq.dto.MqSendResult;
import io.github.atengk.rabbitmq.util.MqMessageIdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * RabbitTemplate 消息发送服务实现
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMessageSendService implements MessageSendService {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送普通消息
     *
     * @param request 消息发送请求
     * @return 消息发送结果
     */
    @Override
    public MqSendResult send(MqSendRequest<?> request) {
        validateRequest(request);

        String messageId = StrUtil.blankToDefault(
                request.getMessageId(),
                MqMessageIdUtil.generate(request.getBusinessType())
        );
        request.setMessageId(messageId);

        try {
            CorrelationData correlationData = new CorrelationData(messageId);

            rabbitTemplate.convertAndSend(
                    request.getExchange(),
                    request.getRoutingKey(),
                    request.getPayload(),
                    message -> {
                        message.getMessageProperties().setMessageId(messageId);
                        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        message.getMessageProperties().setTimestamp(DateUtil.date());
                        message.getMessageProperties().setHeader(RabbitMqProducerConstant.HEADER_MESSAGE_ID, messageId);
                        message.getMessageProperties().setHeader(RabbitMqProducerConstant.HEADER_BUSINESS_TYPE, request.getBusinessType());
                        message.getMessageProperties().setHeader(RabbitMqProducerConstant.HEADER_BUSINESS_KEY, request.getBusinessKey());
                        message.getMessageProperties().setHeader(RabbitMqProducerConstant.HEADER_SEND_TIME, DateUtil.now());

                        Map<String, Object> headers = request.getHeaders();
                        if (MapUtil.isNotEmpty(headers)) {
                            headers.forEach((key, value) -> message.getMessageProperties().setHeader(key, value));
                        }

                        return message;
                    },
                    correlationData
            );

            log.info("MQ消息已提交发送，messageId={}，businessType={}，businessKey={}，exchange={}，routingKey={}",
                    messageId,
                    request.getBusinessType(),
                    request.getBusinessKey(),
                    request.getExchange(),
                    request.getRoutingKey()
            );

            return MqSendResult.builder()
                    .messageId(messageId)
                    .submitted(true)
                    .exchange(request.getExchange())
                    .routingKey(request.getRoutingKey())
                    .businessType(request.getBusinessType())
                    .businessKey(request.getBusinessKey())
                    .message("消息已提交发送")
                    .build();
        } catch (Exception ex) {
            log.error("MQ消息发送异常，businessType={}，businessKey={}，exchange={}，routingKey={}，原因={}",
                    request.getBusinessType(),
                    request.getBusinessKey(),
                    request.getExchange(),
                    request.getRoutingKey(),
                    ex.getMessage(),
                    ex
            );

            return MqSendResult.builder()
                    .messageId(messageId)
                    .submitted(false)
                    .exchange(request.getExchange())
                    .routingKey(request.getRoutingKey())
                    .businessType(request.getBusinessType())
                    .businessKey(request.getBusinessKey())
                    .message("消息发送异常：" + ex.getMessage())
                    .build();
        }
    }

    /**
     * 校验发送请求
     *
     * @param request 消息发送请求
     */
    private void validateRequest(MqSendRequest<?> request) {
        if (ObjectUtil.isNull(request)) {
            throw new IllegalArgumentException("MQ消息发送请求不能为空");
        }
        if (StrUtil.isBlank(request.getExchange())) {
            throw new IllegalArgumentException("MQ交换机不能为空");
        }
        if (StrUtil.isBlank(request.getRoutingKey())) {
            throw new IllegalArgumentException("MQ路由键不能为空");
        }
        if (ObjectUtil.isNull(request.getPayload())) {
            throw new IllegalArgumentException("MQ消息体不能为空");
        }
    }
}
```

封装后的发送服务具备以下能力：

| 能力            | 说明                                      |
| --------------- | ----------------------------------------- |
| 参数校验        | 校验 Exchange、Routing Key、Payload       |
| 消息 ID         | 没有传入时自动生成                        |
| 消息持久化      | 设置 `MessageDeliveryMode.PERSISTENT`     |
| 消息头          | 写入消息 ID、业务类型、业务主键、发送时间 |
| CorrelationData | 使用消息 ID 关联 Confirm 回调             |
| 发送日志        | 记录发送关键字段                          |
| 异常处理        | 捕获发送异常并返回失败结果                |

### 统一消息发送接口

统一消息发送接口用于对业务模块暴露稳定 API。业务生产者只需要组装业务消息，然后调用 `MessageSendService`。

示例业务消息如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/dto/OrderPaidMessage.java`

```java
package io.github.atengk.rabbitmq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单支付成功消息
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
public class OrderPaidMessage {

    @NotBlank(message = "订单ID不能为空")
    private String orderId;

    @NotBlank(message = "支付单号不能为空")
    private String payNo;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "支付金额不能为空")
    private BigDecimal amount;

    @NotNull(message = "支付时间不能为空")
    private LocalDateTime paidTime;
}
```

业务生产者封装如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/producer/OrderMessageProducer.java`

```java
package io.github.atengk.rabbitmq.producer;

import cn.hutool.core.map.MapUtil;
import io.github.atengk.rabbitmq.constant.RabbitMqProducerConstant;
import io.github.atengk.rabbitmq.dto.MqSendRequest;
import io.github.atengk.rabbitmq.dto.MqSendResult;
import io.github.atengk.rabbitmq.dto.OrderPaidMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单消息生产者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageProducer {

    private final MessageSendService messageSendService;

    /**
     * 发送订单支付成功消息
     *
     * @param message 订单支付成功消息
     * @return 消息发送结果
     */
    public MqSendResult sendOrderPaidMessage(OrderPaidMessage message) {
        MqSendRequest<OrderPaidMessage> request = MqSendRequest.<OrderPaidMessage>builder()
                .exchange(RabbitMqProducerConstant.ORDER_EXCHANGE)
                .routingKey(RabbitMqProducerConstant.ORDER_PAID_ROUTING_KEY)
                .businessType("order_paid")
                .businessKey(message.getOrderId())
                .payload(message)
                .headers(MapUtil.<String, Object>builder()
                        .put("x-source-service", "order-service")
                        .put("x-event-type", "order.paid")
                        .build()
                )
                .build();

        MqSendResult result = messageSendService.send(request);
        log.info("订单支付消息发送结果，orderId={}，messageId={}，submitted={}",
                message.getOrderId(),
                result.getMessageId(),
                result.getSubmitted()
        );
        return result;
    }
}
```

接口层调用示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/controller/OrderMessageController.java`

```java
package io.github.atengk.rabbitmq.controller;

import io.github.atengk.rabbitmq.dto.MqSendResult;
import io.github.atengk.rabbitmq.dto.OrderPaidMessage;
import io.github.atengk.rabbitmq.producer.OrderMessageProducer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单消息测试接口
 *
 * @author Ateng
 * @since 2026-05-11
 */
@RestController
@RequestMapping("/api/rabbitmq/order")
@RequiredArgsConstructor
public class OrderMessageController {

    private final OrderMessageProducer orderMessageProducer;

    /**
     * 发送订单支付成功消息
     *
     * @param message 订单支付成功消息
     * @return 消息发送结果
     */
    @PostMapping("/paid/send")
    public MqSendResult sendOrderPaidMessage(@Valid @RequestBody OrderPaidMessage message) {
        return orderMessageProducer.sendOrderPaidMessage(message);
    }
}
```

请求示例：

```bash
curl -X POST 'http://localhost:8080/api/rabbitmq/order/paid/send' \
  -H 'Content-Type: application/json' \
  -d '{
    "orderId": "ORDER202605110001",
    "payNo": "PAY202605110001",
    "userId": 10001,
    "amount": 199.00,
    "paidTime": "2026-05-11 10:30:00"
  }'
```

该接口返回 `MqSendResult`，其中 `submitted=true` 表示消息已提交到 RabbitMQ 发送流程。后续是否成功到达 Exchange，需要结合 Publisher Confirm 日志判断；是否成功进入 Queue，需要结合 ReturnCallback 判断；是否被消费者成功处理，需要结合消费日志判断。

### 消息 ID 生成

消息 ID 是消息链路追踪、发送确认、消费幂等、异常排查和重发补偿的基础字段。每条消息都必须具备全局唯一的 `messageId`。

消息 ID 可以采用以下方式生成：

| 方式                   | 示例                            | 说明                      |
| ---------------------- | ------------------------------- | ------------------------- |
| UUID                   | `order_paid_xxx`                | 简单通用，适合大多数场景  |
| 雪花 ID                | `1888888888888888888`           | 数字型 ID，适合数据库存储 |
| 业务前缀 + 时间 + UUID | `order_paid_20260511103000_xxx` | 便于日志识别              |
| 业务主键 + 事件类型    | `ORDER001_order_paid`           | 需要防止重复事件冲突      |

推荐格式：

```text
业务类型_yyyyMMddHHmmss_UUID
```

示例：

```text
order_paid_20260511103000_6f2f3fbda1ec45f9a24a19950b3f6b3c
```

消息 ID 设计建议：

1. 不依赖数据库自增 ID，避免高并发下产生瓶颈。
2. 包含业务类型前缀，便于日志检索。
3. 保证全局唯一，不能只使用订单号作为消息 ID。
4. Confirm 回调、Return 回调、消费日志中必须打印同一个消息 ID。
5. 幂等表或 Redis 幂等 Key 应使用消息 ID 或业务唯一键。

如果业务要求“同一业务事件只能发送一次”，可以使用业务唯一键辅助防重，例如：

```text
mq:idempotent:send:order_paid:ORDER202605110001
```

该 Key 表示订单 `ORDER202605110001` 的支付成功事件已经发送过，适合配合 Redis 或本地消息表实现生产端防重。

### 消息头设置

消息头用于承载消息体之外的元数据，例如消息 ID、业务类型、业务主键、来源服务、发送时间、版本号、链路追踪 ID 等。消息头不应承载大字段或复杂业务对象。

推荐消息头如下：

| Header              | 说明        |
| ------------------- | ----------- |
| `x-message-id`      | 消息唯一 ID |
| `x-business-type`   | 业务类型    |
| `x-business-key`    | 业务主键    |
| `x-source-service`  | 来源服务    |
| `x-event-type`      | 事件类型    |
| `x-message-version` | 消息版本    |
| `x-send-time`       | 发送时间    |
| `x-trace-id`        | 链路追踪 ID |

消息头设置示例：

```java
rabbitTemplate.convertAndSend(
        exchange,
        routingKey,
        payload,
        message -> {
            message.getMessageProperties().setMessageId(messageId);
            message.getMessageProperties().setHeader("x-message-id", messageId);
            message.getMessageProperties().setHeader("x-business-type", businessType);
            message.getMessageProperties().setHeader("x-business-key", businessKey);
            message.getMessageProperties().setHeader("x-message-version", "1.0");
            message.getMessageProperties().setHeader("x-source-service", "order-service");
            return message;
        },
        new CorrelationData(messageId)
);
```

消息头设计建议：

1. 固定公共 Header 名称，避免不同业务模块各自定义。
2. 业务主键放入 `x-business-key`，便于日志检索。
3. 事件类型放入 `x-event-type`，便于消费者识别。
4. 消息版本放入 `x-message-version`，便于后续兼容升级。
5. Header 中不要放敏感信息和大对象。
6. 消费端获取 Header 时要做好空值兼容。

### 消息发送日志

消息发送日志用于排查生产者是否发送消息、发送到哪个 Exchange、使用哪个 Routing Key、消息 ID 是什么、业务主键是什么。核心业务消息必须记录发送日志。

推荐发送日志字段如下：

| 字段             | 说明         |
| ---------------- | ------------ |
| `messageId`      | 消息唯一 ID  |
| `businessType`   | 业务类型     |
| `businessKey`    | 业务主键     |
| `exchange`       | 交换机       |
| `routingKey`     | 路由键       |
| `payloadSummary` | 消息摘要     |
| `sendTime`       | 发送时间     |
| `submitted`      | 是否提交发送 |
| `errorMessage`   | 异常原因     |

日志示例：

```text
MQ消息已提交发送，messageId=order_paid_20260511103000_xxx，businessType=order_paid，businessKey=ORDER202605110001，exchange=producer.order.exchange，routingKey=order.paid
```

异常日志示例：

```text
MQ消息发送异常，businessType=order_paid，businessKey=ORDER202605110001，exchange=producer.order.exchange，routingKey=order.paid，原因=Connection refused
```

日志记录建议：

1. 日志中必须包含 `messageId`。
2. 日志中必须包含业务主键，例如 `orderId`、`payNo`。
3. 不建议完整打印大消息体。
4. 敏感字段需要脱敏后再打印。
5. Confirm、Return 和消费日志中的 `messageId` 必须一致。
6. 高并发场景下应避免过度打印 INFO 日志，可以将消息体摘要放到 DEBUG。

### 发送结果处理

发送结果处理主要包括三类：同步调用异常、Publisher Confirm、Publisher Return。

| 类型         | 含义                                 | 处理方式                                      |
| ------------ | ------------------------------------ | --------------------------------------------- |
| 同步调用异常 | 调用 `RabbitTemplate` 时直接抛出异常 | 捕获异常，记录失败日志，可写入本地消息表      |
| Confirm 成功 | 消息到达 Exchange                    | 更新发送状态为成功                            |
| Confirm 失败 | 消息未到达 Exchange                  | 记录失败原因，触发重发或补偿                  |
| Return 回调  | 消息到达 Exchange 但无法路由到 Queue | 记录不可路由消息，检查 Binding 或 Routing Key |

Confirm 和 Return 初始化配置如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/RabbitTemplateCallbackInitializer.java`

```java
package io.github.atengk.rabbitmq.config;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * RabbitTemplate 回调初始化配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitTemplateCallbackInitializer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 初始化 RabbitTemplate 回调
     */
    @PostConstruct
    public void initCallback() {
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            String messageId = correlationData == null ? "unknown" : correlationData.getId();

            if (ack) {
                log.info("MQ消息交换机确认成功，messageId={}", messageId);
                return;
            }

            log.error("MQ消息交换机确认失败，messageId={}，原因={}",
                    messageId,
                    StrUtil.blankToDefault(cause, "未知原因")
            );

            // 生产环境建议在这里更新消息记录表状态，或投递补偿任务
        });

        rabbitTemplate.setReturnsCallback(returned -> {
            String messageId = returned.getMessage().getMessageProperties().getMessageId();

            log.error("MQ消息不可路由，messageId={}，exchange={}，routingKey={}，replyCode={}，replyText={}",
                    messageId,
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    returned.getReplyCode(),
                    returned.getReplyText()
            );

            // 生产环境建议在这里记录不可路由消息，并触发告警或补偿处理
        });
    }
}
```

配套配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  rabbitmq:
    # 开启发布确认，correlated 模式可以通过 CorrelationData 关联消息ID
    publisher-confirm-type: correlated
    # 开启不可路由消息返回
    publisher-returns: true
    template:
      # 消息无法路由时返回给生产者，配合 ReturnsCallback 使用
      mandatory: true
```

发送结果处理建议：

1. `convertAndSend` 不抛异常，只表示消息提交到客户端发送流程，不代表 Broker 已确认。
2. Confirm 成功表示消息到达 Exchange，不代表消息一定进入 Queue。
3. Return 回调表示消息不可路由，需要检查 Exchange、Routing Key、Binding。
4. Confirm 失败需要记录并补偿，不应只打印日志。
5. 重要业务建议引入本地消息表，记录 `待发送`、`已发送`、`确认成功`、`确认失败`、`不可路由` 等状态。

## 消息消费者开发

本章节用于说明 RabbitMQ 消费者侧的标准开发方式。消费者是消息可靠性设计中最容易出问题的位置，需要重点关注监听规范、消费日志、异常处理、手动确认、重复消费、并发参数和幂等控制。

### 消费者模块结构

消费者模块用于承接 RabbitMQ 消息入口。消费者类不建议承载大量业务逻辑，而应作为消息入口层，将具体业务处理交给 Service 层。

推荐模块结构如下：

```text
src/main/java/io/github/atengk/rabbitmq/
├── config/
│   └── RabbitMqListenerConfig.java
├── consumer/
│   ├── OrderPaidMessageConsumer.java
│   └── NoticeMessageConsumer.java
├── service/
│   ├── MessageIdempotentService.java
│   ├── OrderPaidHandleService.java
│   └── impl/
│       ├── RedisMessageIdempotentServiceImpl.java
│       └── OrderPaidHandleServiceImpl.java
├── dto/
│   └── OrderPaidMessage.java
└── constant/
    └── RabbitMqConsumerConstant.java
```

消费者模块职责说明：

| 模块                       | 说明                                             |
| -------------------------- | ------------------------------------------------ |
| `consumer`                 | 只负责监听消息、解析消息、调用业务服务、Ack/Nack |
| `service`                  | 负责具体业务处理                                 |
| `MessageIdempotentService` | 负责消费幂等判断                                 |
| `config`                   | 负责监听容器、并发、Ack、重试等配置              |
| `constant`                 | 负责队列、交换机、Routing Key 常量               |

消费者开发原则：

1. 消费者方法必须记录接收日志和结果日志。
2. 核心业务消费者建议使用手动 Ack。
3. 业务处理成功后再 Ack。
4. 业务处理失败时根据异常类型决定 Nack、Reject 或进入死信。
5. 消费者必须具备幂等能力。
6. 不要在消费者中写大量业务逻辑，应调用业务 Service。
7. 不要吞掉异常后直接 Ack，除非业务明确允许忽略。

### Listener 编写规范

`@RabbitListener` 是 Spring AMQP 中最常用的消费者声明方式。规范的 Listener 应包含队列名称、监听容器工厂、消息体、原始消息和 Channel 参数，便于进行手动 Ack 和消息头读取。

推荐写法如下：

```java
@RabbitListener(
        queues = RabbitMqConsumerConstant.ORDER_PAID_QUEUE,
        containerFactory = "manualRabbitListenerContainerFactory"
)
public void consume(OrderPaidMessage payload, Message message, Channel channel) throws IOException {
    // 消费逻辑
}
```

Listener 方法参数说明：

| 参数                       | 说明                                       |
| -------------------------- | ------------------------------------------ |
| `OrderPaidMessage payload` | 反序列化后的业务消息体                     |
| `Message message`          | RabbitMQ 原始消息，可读取消息属性和 Header |
| `Channel channel`          | RabbitMQ Channel，用于手动 Ack/Nack        |

消费者常量示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/constant/RabbitMqConsumerConstant.java`

```java
package io.github.atengk.rabbitmq.constant;

/**
 * RabbitMQ 消费者常量
 *
 * @author Ateng
 * @since 2026-05-11
 */
public final class RabbitMqConsumerConstant {

    private RabbitMqConsumerConstant() {
    }

    public static final String ORDER_PAID_QUEUE = "consumer.order.paid.queue";

    public static final String ORDER_PAID_QUEUE_BAK = "consumer.order.paid.bak.queue";
}
```

Listener 编写建议：

1. 方法名使用 `consumeXxxMessage`，表达消费的消息类型。
2. 日志中必须包含 `messageId` 和业务主键。
3. 先做幂等判断，再执行业务处理。
4. 业务成功后调用 `basicAck`。
5. 业务失败后根据策略调用 `basicNack` 或 `basicReject`。
6. 不要在 Listener 中做耗时阻塞操作，耗时任务应评估线程池或异步处理。
7. 不要在同一个方法中消费多个语义差异很大的消息。

### 单队列消费

单队列消费表示一个消费者方法监听一个队列。这是最常见、最清晰的消费方式，适合订单支付、库存扣减、短信发送等明确业务场景。

文件位置：`src/main/java/io/github/atengk/rabbitmq/service/OrderPaidHandleService.java`

```java
package io.github.atengk.rabbitmq.service;

import io.github.atengk.rabbitmq.dto.OrderPaidMessage;

/**
 * 订单支付消息处理服务
 *
 * @author Ateng
 * @since 2026-05-11
 */
public interface OrderPaidHandleService {

    /**
     * 处理订单支付成功消息
     *
     * @param message 订单支付成功消息
     */
    void handle(OrderPaidMessage message);
}
```

文件位置：`src/main/java/io/github/atengk/rabbitmq/service/impl/OrderPaidHandleServiceImpl.java`

```java
package io.github.atengk.rabbitmq.service.impl;

import io.github.atengk.rabbitmq.dto.OrderPaidMessage;
import io.github.atengk.rabbitmq.service.OrderPaidHandleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单支付消息处理服务实现
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
public class OrderPaidHandleServiceImpl implements OrderPaidHandleService {

    /**
     * 处理订单支付成功消息
     *
     * @param message 订单支付成功消息
     */
    @Override
    public void handle(OrderPaidMessage message) {
        log.info("处理订单支付成功业务，orderId={}，payNo={}，userId={}，amount={}",
                message.getOrderId(),
                message.getPayNo(),
                message.getUserId(),
                message.getAmount()
        );

        // 这里编写订单状态更新、积分发放、通知发送等业务逻辑
    }
}
```

文件位置：`src/main/java/io/github/atengk/rabbitmq/consumer/OrderPaidMessageConsumer.java`

```java
package io.github.atengk.rabbitmq.consumer;

import com.rabbitmq.client.Channel;
import io.github.atengk.rabbitmq.constant.RabbitMqConsumerConstant;
import io.github.atengk.rabbitmq.dto.OrderPaidMessage;
import io.github.atengk.rabbitmq.service.MessageIdempotentService;
import io.github.atengk.rabbitmq.service.OrderPaidHandleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 订单支付消息消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaidMessageConsumer {

    private final OrderPaidHandleService orderPaidHandleService;

    private final MessageIdempotentService messageIdempotentService;

    /**
     * 消费订单支付成功消息
     *
     * @param payload 订单支付成功消息
     * @param message 原始消息
     * @param channel RabbitMQ Channel
     * @throws IOException Ack 或 Nack 失败时抛出
     */
    @RabbitListener(
            queues = RabbitMqConsumerConstant.ORDER_PAID_QUEUE,
            containerFactory = "manualRabbitListenerContainerFactory"
    )
    public void consumeOrderPaidMessage(OrderPaidMessage payload, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();

        try {
            log.info("收到订单支付消息，messageId={}，orderId={}，payNo={}，deliveryTag={}",
                    messageId,
                    payload.getOrderId(),
                    payload.getPayNo(),
                    deliveryTag
            );

            if (messageIdempotentService.isConsumed(messageId)) {
                log.warn("订单支付消息已消费，直接确认，messageId={}，orderId={}", messageId, payload.getOrderId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            orderPaidHandleService.handle(payload);
            messageIdempotentService.markConsumed(messageId);

            channel.basicAck(deliveryTag, false);
            log.info("订单支付消息消费成功，messageId={}，orderId={}", messageId, payload.getOrderId());
        } catch (Exception ex) {
            log.error("订单支付消息消费失败，messageId={}，orderId={}，原因={}",
                    messageId,
                    payload.getOrderId(),
                    ex.getMessage(),
                    ex
            );

            channel.basicNack(deliveryTag, false, false);
        }
    }
}
```

单队列消费适合大多数业务场景。它的优点是监听关系清晰、日志容易排查、失败影响范围小。

### 多队列消费

多队列消费表示同一个消费者方法监听多个队列。它适合多个队列中的消息结构一致、处理逻辑相同的场景。若消息结构不同或业务语义不同，不建议放到同一个消费者方法中处理。

多队列消费示例：

```java
@RabbitListener(
        queues = {
                RabbitMqConsumerConstant.ORDER_PAID_QUEUE,
                RabbitMqConsumerConstant.ORDER_PAID_QUEUE_BAK
        },
        containerFactory = "manualRabbitListenerContainerFactory"
)
public void consumeMultiQueue(OrderPaidMessage payload, Message message, Channel channel) throws IOException {
    String queue = message.getMessageProperties().getConsumerQueue();
    log.info("收到多队列消息，queue={}，orderId={}", queue, payload.getOrderId());
}
```

完整消费者示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/consumer/MultiQueueMessageConsumer.java`

```java
package io.github.atengk.rabbitmq.consumer;

import com.rabbitmq.client.Channel;
import io.github.atengk.rabbitmq.constant.RabbitMqConsumerConstant;
import io.github.atengk.rabbitmq.dto.OrderPaidMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 多队列消息消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class MultiQueueMessageConsumer {

    /**
     * 消费多个队列中的订单支付消息
     *
     * @param payload 订单支付消息
     * @param message 原始消息
     * @param channel RabbitMQ Channel
     * @throws IOException Ack 或 Nack 失败时抛出
     */
    @RabbitListener(
            queues = {
                    RabbitMqConsumerConstant.ORDER_PAID_QUEUE,
                    RabbitMqConsumerConstant.ORDER_PAID_QUEUE_BAK
            },
            containerFactory = "manualRabbitListenerContainerFactory"
    )
    public void consume(OrderPaidMessage payload, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String queue = message.getMessageProperties().getConsumerQueue();
        String messageId = message.getMessageProperties().getMessageId();

        try {
            log.info("收到多队列订单支付消息，queue={}，messageId={}，orderId={}",
                    queue,
                    messageId,
                    payload.getOrderId()
            );

            // 多队列消息结构一致时，可以复用同一套处理逻辑

            channel.basicAck(deliveryTag, false);
            log.info("多队列订单支付消息消费成功，queue={}，messageId={}", queue, messageId);
        } catch (Exception ex) {
            log.error("多队列订单支付消息消费失败，queue={}，messageId={}，原因={}",
                    queue,
                    messageId,
                    ex.getMessage(),
                    ex
            );

            channel.basicNack(deliveryTag, false, false);
        }
    }
}
```

多队列消费建议：

1. 只有消息结构一致、处理逻辑一致时才使用。
2. 日志中必须打印 `consumerQueue`，否则无法判断消息来自哪个队列。
3. 不同业务语义的队列不要混在同一个 Listener。
4. 多队列消费失败时，需要明确失败影响范围。
5. 如果不同队列需要不同并发或重试策略，应拆分消费者方法和监听容器。

### 并发消费配置

并发消费配置用于提升队列处理能力。Spring AMQP 的 `SimpleRabbitListenerContainerFactory` 可以通过 `concurrentConsumers`、`maxConcurrentConsumers` 和 `prefetchCount` 控制消费并发。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/RabbitMqListenerConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 消费监听配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class RabbitMqListenerConfig {

    /**
     * 手动确认监听容器工厂
     *
     * @param configurer        Spring Boot 监听容器配置器
     * @param connectionFactory RabbitMQ 连接工厂
     * @return 监听容器工厂
     */
    @Bean
    public SimpleRabbitListenerContainerFactory manualRabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);

        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(8);
        factory.setPrefetchCount(10);
        factory.setDefaultRequeueRejected(false);

        return factory;
    }
}
```

也可以直接通过配置文件控制。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        # 手动确认模式
        acknowledge-mode: manual
        # 初始消费者数量
        concurrency: 2
        # 最大消费者数量
        max-concurrency: 8
        # 单个消费者最多持有未确认消息数
        prefetch: 10
        # 消费异常后不默认重新入队，避免无限失败循环
        default-requeue-rejected: false
```

并发参数说明：

| 参数                       | 说明                 | 建议                         |
| -------------------------- | -------------------- | ---------------------------- |
| `concurrency`              | 初始消费者数量       | 根据基础吞吐量设置           |
| `max-concurrency`          | 最大消费者数量       | 根据高峰吞吐量和机器资源设置 |
| `prefetch`                 | 每个消费者预取数量   | 耗时任务设置小一些           |
| `acknowledge-mode`         | 确认模式             | 核心业务建议 `manual`        |
| `default-requeue-rejected` | 异常是否默认重新入队 | 配合死信队列时建议 `false`   |

并发消费注意事项：

1. 并发消费会影响消息顺序性。
2. `prefetch` 过大可能导致消息分配不均。
3. 消费者实例数和线程数不是越大越好，需要结合数据库、Redis、第三方接口能力评估。
4. 幂等控制必须在并发场景下仍然有效。
5. 如果必须保证严格顺序，应使用单队列单消费者或分片队列方案。

### 消息消费日志

消费日志用于记录消费者是否收到消息、是否处理成功、失败原因是什么、处理耗时是多少。没有消费日志，消息问题排查会非常困难。

推荐消费日志字段如下：

| 字段            | 说明              |
| --------------- | ----------------- |
| `messageId`     | 消息唯一 ID       |
| `businessType`  | 业务类型          |
| `businessKey`   | 业务主键          |
| `queue`         | 消费队列          |
| `deliveryTag`   | RabbitMQ 投递标签 |
| `redelivered`   | 是否重复投递      |
| `consumeStatus` | 消费状态          |
| `costMs`        | 消费耗时          |
| `errorMessage`  | 异常原因          |

消费日志示例：

```text
收到订单支付消息，messageId=order_paid_20260511103000_xxx，orderId=ORDER202605110001，payNo=PAY202605110001，deliveryTag=1
订单支付消息消费成功，messageId=order_paid_20260511103000_xxx，orderId=ORDER202605110001，costMs=35
```

带耗时统计的消费示例：

```java
long startTime = System.currentTimeMillis();
try {
    orderPaidHandleService.handle(payload);
    long costMs = System.currentTimeMillis() - startTime;
    log.info("订单支付消息消费成功，messageId={}，orderId={}，costMs={}", messageId, payload.getOrderId(), costMs);
} catch (Exception ex) {
    long costMs = System.currentTimeMillis() - startTime;
    log.error("订单支付消息消费失败，messageId={}，orderId={}，costMs={}，原因={}",
            messageId,
            payload.getOrderId(),
            costMs,
            ex.getMessage(),
            ex
    );
}
```

日志建议：

1. 收到消息时打印接收日志。
2. 消费成功时打印成功日志。
3. 消费失败时打印异常日志。
4. 重复消息应打印幂等命中日志。
5. 不要在高并发场景完整打印大消息体。
6. 消息体包含敏感字段时必须脱敏。

### 消费异常处理

消费异常处理决定消息失败后是重新入队、进入死信队列、直接丢弃，还是人工补偿。核心业务中不建议捕获异常后直接 Ack，否则会造成消息丢失且难以追踪。

常见异常处理方式如下：

| 方式                         | 说明                     | 适用场景                     |
| ---------------------------- | ------------------------ | ---------------------------- |
| `basicAck`                   | 确认消费成功             | 业务处理成功或重复消息已处理 |
| `basicNack(requeue=true)`    | 拒绝并重新入队           | 临时异常，但要防止无限重试   |
| `basicNack(requeue=false)`   | 拒绝且不重新入队         | 配合死信队列处理失败消息     |
| `basicReject(requeue=false)` | 拒绝单条消息且不重新入队 | 单条消息不可处理             |
| 抛出异常                     | 交给监听容器异常策略处理 | 配合 Spring Retry 使用       |

异常分类建议：

| 异常类型 | 示例                       | 建议处理                 |
| -------- | -------------------------- | ------------------------ |
| 参数异常 | 消息缺字段、格式错误       | 不重新入队，进入死信     |
| 业务异常 | 订单不存在、状态非法       | 按业务判断是否死信或忽略 |
| 临时异常 | 网络抖动、第三方超时       | 可重试，重试失败后死信   |
| 系统异常 | 数据库不可用、Redis 不可用 | 可重新入队或进入重试流程 |
| 幂等冲突 | 消息已处理                 | Ack 确认，不重复处理     |

异常处理示例：

```java
try {
    orderPaidHandleService.handle(payload);
    channel.basicAck(deliveryTag, false);
} catch (IllegalArgumentException ex) {
    log.error("消息参数异常，不重新入队，messageId={}，原因={}", messageId, ex.getMessage(), ex);
    channel.basicNack(deliveryTag, false, false);
} catch (Exception ex) {
    log.error("消息消费异常，进入死信或后续补偿，messageId={}，原因={}", messageId, ex.getMessage(), ex);
    channel.basicNack(deliveryTag, false, false);
}
```

消费异常处理建议：

1. 不要对所有异常都 `requeue=true`，否则可能造成无限消费失败。
2. 参数错误、数据格式错误通常不应重新入队。
3. 临时网络异常可以重试，但应限制次数。
4. 核心业务队列应配置死信队列。
5. 死信消息应支持查询、重发、忽略和人工处理。
6. 异常日志必须包含 `messageId` 和业务主键。

### 消费幂等控制

RabbitMQ 在网络异常、消费者宕机、Ack 失败、重试机制、手动 Nack 等情况下，都可能导致消息重复投递。因此消费者必须具备幂等能力，不能假设一条消息只会被消费一次。

常见幂等方案如下：

| 方案           | 说明                         | 适用场景               |
| -------------- | ---------------------------- | ---------------------- |
| Redis SETNX    | 使用消息 ID 或业务唯一键加锁 | 高并发、低延迟场景     |
| 数据库唯一索引 | 消费记录表建立唯一索引       | 强一致审计场景         |
| 业务状态机     | 根据业务状态判断是否可执行   | 订单、支付、库存等场景 |
| 本地消息表     | 记录消息消费状态             | 需要查询和补偿的场景   |

Redis 幂等服务接口如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/service/MessageIdempotentService.java`

```java
package io.github.atengk.rabbitmq.service;

/**
 * 消息消费幂等服务
 *
 * @author Ateng
 * @since 2026-05-11
 */
public interface MessageIdempotentService {

    /**
     * 判断消息是否已消费
     *
     * @param messageId 消息ID
     * @return 是否已消费
     */
    boolean isConsumed(String messageId);

    /**
     * 标记消息已消费
     *
     * @param messageId 消息ID
     */
    void markConsumed(String messageId);
}
```

基于 Redis 的幂等实现如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/service/impl/RedisMessageIdempotentServiceImpl.java`

```java
package io.github.atengk.rabbitmq.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.rabbitmq.service.MessageIdempotentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis 消息消费幂等服务实现
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisMessageIdempotentServiceImpl implements MessageIdempotentService {

    private static final String CONSUMED_KEY_PREFIX = "mq:consumed:";

    private static final Duration CONSUMED_KEY_TTL = Duration.ofDays(7);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 判断消息是否已消费
     *
     * @param messageId 消息ID
     * @return 是否已消费
     */
    @Override
    public boolean isConsumed(String messageId) {
        if (StrUtil.isBlank(messageId)) {
            return false;
        }
        String key = buildConsumedKey(messageId);
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    /**
     * 标记消息已消费
     *
     * @param messageId 消息ID
     */
    @Override
    public void markConsumed(String messageId) {
        if (StrUtil.isBlank(messageId)) {
            log.warn("消息ID为空，跳过幂等标记");
            return;
        }

        String key = buildConsumedKey(messageId);
        stringRedisTemplate.opsForValue().set(key, "1", CONSUMED_KEY_TTL);
        log.info("消息消费幂等标记完成，messageId={}，key={}", messageId, key);
    }

    /**
     * 构建已消费 Key
     *
     * @param messageId 消息ID
     * @return Redis Key
     */
    private String buildConsumedKey(String messageId) {
        return CONSUMED_KEY_PREFIX + messageId;
    }
}
```

如果要避免并发场景下多个消费者同时处理同一条消息，建议使用 `SETNX` 方式抢占处理权，而不是先判断再标记。示例接口可以扩展为 `tryConsume`。

```java
Boolean success = stringRedisTemplate.opsForValue()
        .setIfAbsent("mq:consume:lock:" + messageId, "1", Duration.ofMinutes(10));
```

消费幂等设计建议：

1. 幂等 Key 优先使用 `messageId`，业务强约束场景可使用业务唯一键。
2. Redis 幂等适合高并发，但要设置合理过期时间。
3. 数据库唯一索引适合需要长期审计和强一致记录的场景。
4. 业务状态机是最可靠的最终防线，例如订单已支付则不重复支付。
5. 幂等判断应在业务处理前执行。
6. 幂等标记应在业务处理成功后写入。
7. 对于本地事务和幂等标记的一致性要求较高的场景，应使用数据库消费记录表。


## 消息格式设计

本章节用于统一 RabbitMQ 消息的数据结构、基础字段、业务字段、消息头、版本号和兼容性规则。消息格式一旦被多个服务依赖，就不能随意变更，因此需要在项目早期明确规范。

### JSON 消息格式

RabbitMQ 传输的是字节数据，业务系统通常会将 Java 对象序列化为 JSON 后发送。JSON 格式可读性好，便于日志排查、管理控制台查看、跨语言消费和后续扩展。

推荐统一使用“消息信封 + 业务数据”的结构：

```json
{
  "messageId": "order_paid_20260511103000_6f2f3fbda1ec45f9a24a19950b3f6b3c",
  "eventType": "order.paid",
  "version": "1.0",
  "source": "order-service",
  "traceId": "trace-202605111030001001",
  "timestamp": "2026-05-11 10:30:00",
  "data": {
    "orderId": "ORDER202605110001",
    "payNo": "PAY202605110001",
    "userId": 10001,
    "amount": 199.00,
    "paidTime": "2026-05-11 10:29:58"
  }
}
```

该结构分为两层：

| 层级         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 消息基础字段 | 描述消息本身，例如消息 ID、事件类型、版本号、来源服务、链路 ID、发送时间 |
| 业务数据字段 | 描述具体业务内容，例如订单 ID、支付单号、金额、支付时间      |

建议不要直接将数据库 Entity 作为消息体发送。数据库实体通常包含内部字段、审计字段、逻辑删除字段、敏感字段等内容，不适合作为跨系统消息契约。

### 消息基础字段

消息基础字段用于支撑链路追踪、幂等控制、版本兼容、异常排查和消息治理。所有业务消息都应包含这些字段。

推荐基础字段如下：

| 字段        | 类型   | 是否必填 | 说明                              |
| ----------- | ------ | -------- | --------------------------------- |
| `messageId` | String | 是       | 消息唯一 ID，用于追踪、确认和幂等 |
| `eventType` | String | 是       | 事件类型，例如 `order.paid`       |
| `version`   | String | 是       | 消息版本，例如 `1.0`              |
| `source`    | String | 是       | 来源服务，例如 `order-service`    |
| `traceId`   | String | 否       | 链路追踪 ID                       |
| `timestamp` | String | 是       | 消息创建时间                      |
| `data`      | Object | 是       | 业务数据对象                      |

通用消息信封对象如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/dto/MqMessage.java`

```java
package io.github.atengk.rabbitmq.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MQ 通用消息信封
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
public class MqMessage<T> {

    /**
     * 消息唯一ID
     */
    @NotBlank(message = "消息ID不能为空")
    private String messageId;

    /**
     * 事件类型
     */
    @NotBlank(message = "事件类型不能为空")
    private String eventType;

    /**
     * 消息版本
     */
    @NotBlank(message = "消息版本不能为空")
    private String version;

    /**
     * 来源服务
     */
    @NotBlank(message = "来源服务不能为空")
    private String source;

    /**
     * 链路追踪ID
     */
    private String traceId;

    /**
     * 消息创建时间
     */
    @NotNull(message = "消息创建时间不能为空")
    private LocalDateTime timestamp;

    /**
     * 业务数据
     */
    @Valid
    @NotNull(message = "业务数据不能为空")
    private T data;
}
```

消息基础字段设计建议：

1. `messageId` 必须全局唯一，不建议只使用订单号、用户 ID 等业务 ID。
2. `eventType` 表示已经发生的业务事件，推荐使用过去式或状态型命名，例如 `order.created`、`payment.success`。
3. `version` 从 `1.0` 开始，结构发生不兼容变更时升级主版本。
4. `source` 用于定位消息来源服务。
5. `traceId` 用于与接口日志、链路追踪系统关联。
6. `timestamp` 使用统一格式，避免不同服务输出不一致。

### 业务数据字段

业务数据字段用于承载具体业务内容。不同事件类型对应不同业务数据对象，建议每种事件定义独立 DTO，不要多个事件共用一个含大量可空字段的大对象。

订单支付成功业务数据示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/dto/OrderPaidData.java`

```java
package io.github.atengk.rabbitmq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单支付成功业务数据
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
public class OrderPaidData {

    /**
     * 订单ID
     */
    @NotBlank(message = "订单ID不能为空")
    private String orderId;

    /**
     * 支付单号
     */
    @NotBlank(message = "支付单号不能为空")
    private String payNo;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 支付金额
     */
    @NotNull(message = "支付金额不能为空")
    private BigDecimal amount;

    /**
     * 支付时间
     */
    @NotNull(message = "支付时间不能为空")
    private LocalDateTime paidTime;
}
```

业务字段设计建议：

| 建议                | 说明                                           |
| ------------------- | ---------------------------------------------- |
| 使用独立 DTO        | 每类消息定义独立业务数据对象                   |
| 字段语义清晰        | 字段名应直接表达业务含义                       |
| 避免发送 Entity     | 防止内部字段泄露和结构耦合                     |
| 避免大对象          | 大文件、大文本、大集合应传引用地址或业务 ID    |
| 避免敏感字段        | 密码、证件号、密钥、银行卡号等不应直接进入消息 |
| 保留业务主键        | 消费端需要根据业务主键执行业务处理和排查问题   |
| 金额使用 BigDecimal | 避免使用 Float、Double 表示金额                |
| 时间格式统一        | 推荐使用 `yyyy-MM-dd HH:mm:ss`                 |

组装完整消息示例。

```java
MqMessage<OrderPaidData> message = MqMessage.<OrderPaidData>builder()
        .messageId("order_paid_20260511103000_6f2f3fbda1ec45f9a24a19950b3f6b3c")
        .eventType("order.paid")
        .version("1.0")
        .source("order-service")
        .traceId("trace-202605111030001001")
        .timestamp(LocalDateTime.now())
        .data(OrderPaidData.builder()
                .orderId("ORDER202605110001")
                .payNo("PAY202605110001")
                .userId(10001L)
                .amount(new BigDecimal("199.00"))
                .paidTime(LocalDateTime.now())
                .build())
        .build();
```

### 消息头设计

消息头用于承载消息元数据。部分信息既可以放在消息体中，也可以放在 Header 中。建议重要链路字段在消息体中保留，同时在 Header 中冗余一份，便于 Confirm、Return、消费异常、死信排查时快速读取。

推荐消息头如下：

| Header              | 说明        |
| ------------------- | ----------- |
| `x-message-id`      | 消息唯一 ID |
| `x-event-type`      | 事件类型    |
| `x-message-version` | 消息版本    |
| `x-source-service`  | 来源服务    |
| `x-business-key`    | 业务主键    |
| `x-trace-id`        | 链路追踪 ID |
| `x-send-time`       | 发送时间    |

消息头常量如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/constant/MqHeaderConstant.java`

```java
package io.github.atengk.rabbitmq.constant;

/**
 * MQ 消息头常量
 *
 * @author Ateng
 * @since 2026-05-11
 */
public final class MqHeaderConstant {

    private MqHeaderConstant() {
    }

    public static final String MESSAGE_ID = "x-message-id";

    public static final String EVENT_TYPE = "x-event-type";

    public static final String MESSAGE_VERSION = "x-message-version";

    public static final String SOURCE_SERVICE = "x-source-service";

    public static final String BUSINESS_KEY = "x-business-key";

    public static final String TRACE_ID = "x-trace-id";

    public static final String SEND_TIME = "x-send-time";
}
```

发送消息时设置 Header 示例。

```java
rabbitTemplate.convertAndSend(
        exchange,
        routingKey,
        mqMessage,
        message -> {
            message.getMessageProperties().setMessageId(mqMessage.getMessageId());
            message.getMessageProperties().setHeader(MqHeaderConstant.MESSAGE_ID, mqMessage.getMessageId());
            message.getMessageProperties().setHeader(MqHeaderConstant.EVENT_TYPE, mqMessage.getEventType());
            message.getMessageProperties().setHeader(MqHeaderConstant.MESSAGE_VERSION, mqMessage.getVersion());
            message.getMessageProperties().setHeader(MqHeaderConstant.SOURCE_SERVICE, mqMessage.getSource());
            message.getMessageProperties().setHeader(MqHeaderConstant.TRACE_ID, mqMessage.getTraceId());
            message.getMessageProperties().setHeader(MqHeaderConstant.SEND_TIME, mqMessage.getTimestamp().toString());
            return message;
        }
);
```

消息头设计建议：

1. Header 名称统一使用小写横线风格，例如 `x-message-id`。
2. Header 中只放元数据，不放复杂业务对象。
3. 消费端读取 Header 时要做好空值兼容。
4. 不要只依赖 Header 表达业务语义，核心业务数据仍应放在消息体中。
5. 死信消息排查时，Header 比消息体更容易被快速检索，因此 `messageId`、`businessKey`、`eventType` 建议写入 Header。

### 消息版本设计

消息版本用于解决消息结构演进问题。生产者和消费者可能由不同服务、不同团队维护，不能假设所有服务会同时升级。因此，消息格式必须支持版本管理。

推荐版本规则如下：

| 版本变化     | 示例                | 说明                            |
| ------------ | ------------------- | ------------------------------- |
| 兼容新增字段 | `1.0 -> 1.1`        | 消费者可以忽略新字段            |
| 字段含义变化 | `1.x -> 2.0`        | 不兼容变更，需要新版本          |
| 字段删除     | `1.x -> 2.0`        | 不兼容变更，需要评估消费者影响  |
| 字段类型变化 | `1.x -> 2.0`        | 不兼容变更，例如 String 改 Long |
| 新事件类型   | 新增 `order.closed` | 不影响原事件                    |

版本字段建议放在消息体和 Header 中：

```json
{
  "messageId": "order_paid_20260511103000_xxx",
  "eventType": "order.paid",
  "version": "1.0",
  "source": "order-service",
  "timestamp": "2026-05-11 10:30:00",
  "data": {
    "orderId": "ORDER202605110001",
    "payNo": "PAY202605110001"
  }
}
```

消费者处理版本示例。

```java
String version = mqMessage.getVersion();
if ("1.0".equals(version)) {
    // 按 1.0 结构处理
} else if ("1.1".equals(version)) {
    // 按 1.1 结构处理
} else {
    log.warn("不支持的消息版本，messageId={}，version={}", mqMessage.getMessageId(), version);
}
```

版本设计建议：

1. 只新增可选字段时，不需要升级主版本。
2. 删除字段、修改字段类型、改变字段含义时，需要升级主版本。
3. 消费者应忽略未知字段。
4. 消费者不应因为新增字段而反序列化失败。
5. 多版本并行期间，消费者需要兼容旧版本。
6. 重大版本升级时可以使用新的 `eventType` 或新的 Queue 隔离。

### 消息兼容性设计

消息兼容性设计用于保证生产者升级后，不会导致旧消费者无法消费；消费者升级后，也能继续处理旧消息。兼容性问题通常发生在字段删除、字段改名、字段类型变化、时间格式变化、枚举值新增等场景。

常见兼容性风险如下：

| 风险         | 示例                         | 影响                   |
| ------------ | ---------------------------- | ---------------------- |
| 字段删除     | 删除 `payNo`                 | 旧消费者读取字段为空   |
| 字段改名     | `orderId` 改为 `id`          | 消费者无法映射字段     |
| 类型变化     | `userId` 从 Long 改为 String | 反序列化失败或业务异常 |
| 时间格式变化 | 时间戳改字符串               | 消费者解析失败         |
| 枚举新增     | 新增 `REFUNDING`             | 旧消费者无法识别       |
| 嵌套结构变化 | `data.user.id` 改为 `userId` | 消费者解析逻辑失效     |

兼容性规则如下：

1. 可以新增字段，但新增字段必须允许为空或有默认值。
2. 不要直接删除字段，先废弃，再等待所有消费者升级后删除。
3. 不要改变已有字段类型。
4. 不要改变已有字段业务含义。
5. 枚举新增时，消费者必须有默认处理分支。
6. 时间字段格式必须统一。
7. 对外消息 DTO 和内部 Entity 分离，避免数据库字段变化影响消息格式。

推荐使用 Jackson 忽略未知字段，提升消费者兼容性。

文件位置：`src/main/java/io/github/atengk/rabbitmq/dto/CompatibleOrderPaidData.java`

```java
package io.github.atengk.rabbitmq.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 兼容性订单支付消息数据
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompatibleOrderPaidData {

    private String orderId;

    private String payNo;

    private Long userId;

    private BigDecimal amount;

    private LocalDateTime paidTime;
}
```

兼容性处理建议：

| 场景         | 推荐处理                                   |
| ------------ | ------------------------------------------ |
| 新增字段     | 消费者忽略未知字段                         |
| 字段废弃     | 保留字段但标记 deprecated                  |
| 新旧结构并行 | 使用版本号区分处理逻辑                     |
| 枚举扩展     | 增加 default 分支                          |
| 消息重构     | 新增事件类型或新队列，避免直接破坏旧消费者 |

## 消息转换器配置

本章节用于说明 Spring Boot 3 中 RabbitMQ 消息转换器的配置方式。消息转换器负责 Java 对象与 RabbitMQ 消息之间的序列化和反序列化。项目应统一使用 JSON 转换器，并配置时间格式、类型映射和异常处理策略。

### Jackson2JsonMessageConverter

`Jackson2JsonMessageConverter` 是 Spring AMQP 常用的 JSON 消息转换器，可以将 Java 对象序列化为 JSON，也可以将 JSON 消息反序列化为 Java 对象。实际项目中建议显式配置该转换器，避免使用 Java 原生序列化。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/RabbitMqMessageConverterConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 消息转换器配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class RabbitMqMessageConverterConfig {

    /**
     * 配置 Jackson JSON 消息转换器
     *
     * @param objectMapper 自定义 ObjectMapper
     * @return 消息转换器
     */
    @Bean
    public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
```

如果当前项目依赖版本中 `Jackson2JsonMessageConverter` 不可用，可以检查 Spring AMQP 版本；部分新版本可能提供 `JacksonJsonMessageConverter`。项目应以当前依赖实际可用类为准，并保持生产者和消费者两侧配置一致。

配置生效后，`RabbitTemplate.convertAndSend` 发送对象时会自动转换为 JSON，`@RabbitListener` 方法参数也可以直接接收 Java DTO。

### 自定义 ObjectMapper

自定义 `ObjectMapper` 用于统一 JSON 序列化规则，例如时间格式、未知字段处理、空字段处理、枚举处理等。Spring Boot 项目可以定义一个全局 `ObjectMapper`，RabbitMQ 消息转换器复用该对象。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/JacksonConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson JSON 配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class JacksonConfig {

    /**
     * 自定义 ObjectMapper
     *
     * @return ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 支持 LocalDateTime、LocalDate、LocalTime 等 Java 8 时间类型
        objectMapper.registerModule(new JavaTimeModule());

        // 日期不输出为时间戳
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 忽略未知字段，提升消息兼容性
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // 空字段不输出，减少消息体大小
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        return objectMapper;
    }
}
```

也可以通过 `application.yml` 配置基础 Jackson 行为。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  jackson:
    # 统一日期时间格式
    date-format: yyyy-MM-dd HH:mm:ss
    # 统一时区
    time-zone: Asia/Shanghai
    serialization:
      # 日期不输出为时间戳
      write-dates-as-timestamps: false
    deserialization:
      # 反序列化时忽略未知字段
      fail-on-unknown-properties: false
    default-property-inclusion: non_null
```

配置建议：

1. Java 时间类型必须注册 `JavaTimeModule`。
2. 消费端建议忽略未知字段。
3. 不建议将 `BigDecimal` 转为浮点数。
4. 不建议启用复杂类型的默认多态反序列化。
5. 生产者和消费者应保持时间格式一致。
6. 如果项目已有全局 Jackson 配置，RabbitMQ 消息转换器应复用同一个 `ObjectMapper`。

### LocalDateTime 序列化

`LocalDateTime` 是业务消息中常见的时间字段，例如订单创建时间、支付时间、发送时间等。默认序列化格式如果不统一，可能出现数组格式、时间戳格式或时区不一致问题。

推荐使用字符串格式：

```text
yyyy-MM-dd HH:mm:ss
```

可以通过字段注解控制单个字段格式。

文件位置：`src/main/java/io/github/atengk/rabbitmq/dto/OrderPaidTimeData.java`

```java
package io.github.atengk.rabbitmq.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单支付时间消息数据
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
public class OrderPaidTimeData {

    private String orderId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime paidTime;
}
```

也可以通过全局序列化器控制所有 `LocalDateTime` 字段格式。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/JacksonDateTimeConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 日期时间序列化配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class JacksonDateTimeConfig {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 配置 Java 时间模块
     *
     * @return JavaTimeModule
     */
    @Bean
    public JavaTimeModule javaTimeModule() {
        JavaTimeModule module = new JavaTimeModule();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
        return module;
    }
}
```

LocalDateTime 使用建议：

| 建议                   | 说明                                          |
| ---------------------- | --------------------------------------------- |
| 统一格式               | 推荐 `yyyy-MM-dd HH:mm:ss`                    |
| 统一时区               | 国内业务通常使用 `Asia/Shanghai`              |
| 避免数组格式           | 不利于排查和跨语言消费                        |
| 避免混用时间戳和字符串 | 会增加消费者解析复杂度                        |
| 跨时区业务谨慎使用     | 国际化场景可考虑 `OffsetDateTime` 或 UTC 时间 |

### 消息类型映射

消息类型映射用于解决消费者如何根据消息类型反序列化为对应 Java 类的问题。简单场景下，`@RabbitListener` 方法参数已经明确指定 DTO 类型，Spring AMQP 可以直接转换。复杂场景下，如果一个队列消费多种消息类型，可以通过 Header 中的类型字段或 Jackson 类型映射来处理。

推荐优先使用 `eventType` 字段进行业务分发，而不是强依赖 Java 类名。这样可以减少跨服务包名变化带来的耦合。

事件类型常量如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/constant/MqEventTypeConstant.java`

```java
package io.github.atengk.rabbitmq.constant;

/**
 * MQ 事件类型常量
 *
 * @author Ateng
 * @since 2026-05-11
 */
public final class MqEventTypeConstant {

    private MqEventTypeConstant() {
    }

    public static final String ORDER_CREATED = "order.created";

    public static final String ORDER_PAID = "order.paid";

    public static final String ORDER_CANCELLED = "order.cancelled";
}
```

基于 `eventType` 的分发示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/consumer/EventMessageConsumer.java`

```java
package io.github.atengk.rabbitmq.consumer;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import io.github.atengk.rabbitmq.constant.MqEventTypeConstant;
import io.github.atengk.rabbitmq.dto.MqMessage;
import io.github.atengk.rabbitmq.dto.OrderPaidData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 事件消息消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventMessageConsumer {

    private final ObjectMapper objectMapper;

    /**
     * 消费事件消息
     *
     * @param mqMessage 消息信封
     * @param message   原始消息
     * @param channel   RabbitMQ Channel
     * @throws IOException Ack 或 Nack 失败时抛出
     */
    @RabbitListener(queues = "event.common.queue")
    public void consume(MqMessage<?> mqMessage, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            log.info("收到事件消息，messageId={}，eventType={}，version={}",
                    mqMessage.getMessageId(),
                    mqMessage.getEventType(),
                    mqMessage.getVersion()
            );

            if (MqEventTypeConstant.ORDER_PAID.equals(mqMessage.getEventType())) {
                OrderPaidData data = objectMapper.convertValue(mqMessage.getData(), OrderPaidData.class);
                log.info("处理订单支付事件，orderId={}，payNo={}", data.getOrderId(), data.getPayNo());
            } else {
                log.warn("未支持的事件类型，messageId={}，eventType={}，data={}",
                        mqMessage.getMessageId(),
                        mqMessage.getEventType(),
                        JSONUtil.toJsonStr(mqMessage.getData())
                );
            }

            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("事件消息消费失败，messageId={}，eventType={}，原因={}",
                    mqMessage.getMessageId(),
                    mqMessage.getEventType(),
                    ex.getMessage(),
                    ex
            );
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
```

如果项目需要使用 Jackson 类型映射，可以配置类型标识。该方式更适合 Java 服务之间通信，不推荐作为跨语言消息契约的唯一依据。

```java
@Bean
public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
    Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);

    // 默认通过消息头中的类型信息进行转换，适合 Java 服务之间使用
    return converter;
}
```

消息类型映射建议：

1. 单一队列只消费一种消息类型时，直接使用明确 DTO 参数。
2. 一个队列消费多种事件时，使用 `eventType` 分发。
3. 不建议让消费者强依赖生产者的 Java 包名。
4. 跨语言消息不要依赖 Java 类类型头。
5. `eventType + version` 是更稳定的消息契约。

### 序列化异常处理

序列化异常可能发生在生产者发送前，也可能发生在消费者方法调用前。生产者侧通常表现为 `convertAndSend` 抛出异常；消费者侧如果消息体无法反序列化，可能还没有进入 Listener 方法，直接由监听容器异常处理器处理。

常见序列化异常如下：

| 场景               | 原因                                                 |
| ------------------ | ---------------------------------------------------- |
| 生产者序列化失败   | 消息对象包含不可序列化字段、循环引用、时间类型未配置 |
| 消费者反序列化失败 | JSON 格式错误、字段类型不匹配、时间格式不匹配        |
| 类型映射失败       | 消息类型头缺失或无法匹配 DTO                         |
| 版本不兼容         | 生产者字段变更导致旧消费者无法解析                   |
| 未知字段失败       | 消费端未配置忽略未知字段                             |

生产者侧异常处理示例。

```java
try {
    rabbitTemplate.convertAndSend(exchange, routingKey, payload);
    log.info("MQ消息序列化并发送成功，exchange={}，routingKey={}", exchange, routingKey);
} catch (Exception ex) {
    log.error("MQ消息序列化或发送失败，exchange={}，routingKey={}，原因={}",
            exchange,
            routingKey,
            ex.getMessage(),
            ex
    );
    throw ex;
}
```

消费者侧可以配置监听容器异常处理器，捕获转换失败、监听执行失败等异常。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/RabbitMqListenerErrorConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ErrorHandler;

/**
 * RabbitMQ 监听异常处理配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Configuration
public class RabbitMqListenerErrorConfig {

    /**
     * 配置监听异常处理器
     *
     * @return ErrorHandler
     */
    @Bean
    public ErrorHandler rabbitListenerErrorHandler() {
        return new ConditionalRejectingErrorHandler(throwable -> {
            log.error("RabbitMQ监听异常，原因={}", throwable.getMessage(), throwable);
            return true;
        });
    }
}
```

将异常处理器配置到监听容器工厂中。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/RabbitMqListenerConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ErrorHandler;

/**
 * RabbitMQ 监听容器配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
@RequiredArgsConstructor
public class RabbitMqListenerConfig {

    private final ErrorHandler rabbitListenerErrorHandler;

    /**
     * 手动确认监听容器工厂
     *
     * @param configurer        Spring Boot 监听容器配置器
     * @param connectionFactory RabbitMQ 连接工厂
     * @return 监听容器工厂
     */
    @Bean
    public SimpleRabbitListenerContainerFactory manualRabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);

        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(8);
        factory.setPrefetchCount(10);
        factory.setDefaultRequeueRejected(false);
        factory.setErrorHandler(rabbitListenerErrorHandler);

        return factory;
    }
}
```

序列化异常处理建议：

1. 生产者发送前应保证消息对象字段可被 Jackson 序列化。
2. 消费端应配置忽略未知字段，提升兼容性。
3. 时间字段必须统一格式。
4. 不支持的事件类型不要直接抛空指针，应记录日志并按策略处理。
5. 反序列化失败的消息不建议无限重新入队。
6. 核心业务队列应配置死信队列，异常消息进入死信后再人工排查或重发。
7. 生产环境应对序列化异常设置告警，因为这通常表示消息契约已经不兼容。

基础验证方式如下：

```bash
curl -X POST 'http://localhost:8080/api/rabbitmq/order/paid/send' \
  -H 'Content-Type: application/json' \
  -d '{
    "orderId": "ORDER202605110001",
    "payNo": "PAY202605110001",
    "userId": 10001,
    "amount": 199.00,
    "paidTime": "2026-05-11 10:29:58"
  }'
```

验证时重点观察三处：生产者日志中是否输出 `messageId`、RabbitMQ 管理控制台中消息体是否为可读 JSON、消费者日志中 `LocalDateTime` 是否能正常反序列化。如果任一环节失败，应优先检查 `MessageConverter`、`ObjectMapper`、DTO 字段类型和时间格式配置。


## 消息可靠性设计

本章节用于说明 RabbitMQ 消息从生产者发送、Broker 路由、队列存储到消费者处理全过程中的可靠性设计。RabbitMQ 的可靠性不是单一配置可以保证的，而是由消息持久化、队列持久化、Exchange 持久化、Publisher Confirm、Publisher Return、Consumer Acknowledge、重试机制、死信队列、幂等控制和业务补偿共同组成。

完整可靠链路如下：

```text
生产者构造消息
    -> 消息持久化
    -> 发送到 Exchange
    -> Publisher Confirm 确认是否到达 Exchange
    -> Publisher Return 判断是否成功路由到 Queue
    -> Queue 持久化保存消息
    -> Consumer 手动消费确认
    -> 消费失败重试或进入死信队列
    -> 幂等控制防止重复消费
```

### 消息持久化

消息持久化表示消息本身会以持久化方式写入 RabbitMQ。只有队列持久化而消息不持久化时，Broker 重启后队列仍然存在，但队列中的非持久化消息可能丢失。因此，核心业务消息必须设置为持久化消息。

消息持久化需要满足以下条件：

| 条件            | 说明                                          |
| --------------- | --------------------------------------------- |
| Exchange 持久化 | Exchange 使用 `durable=true`                  |
| Queue 持久化    | Queue 使用 `durable=true`                     |
| Message 持久化  | Message 使用 `MessageDeliveryMode.PERSISTENT` |
| 发送确认        | 开启 Publisher Confirm                        |
| 消费确认        | 使用 Consumer Acknowledge                     |

发送持久化消息示例。

```java
rabbitTemplate.convertAndSend(
        exchange,
        routingKey,
        payload,
        message -> {
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            message.getMessageProperties().setMessageId(messageId);
            return message;
        },
        new CorrelationData(messageId)
);
```

为了避免每次发送都重复设置消息属性，建议在统一发送服务中默认设置消息持久化。

下面的代码用于统一设置消息持久化、消息 ID 和消息头，适合放在生产者统一发送服务中。

文件位置：`src/main/java/io/github/atengk/rabbitmq/producer/ReliableMessageSendService.java`

```java
package io.github.atengk.rabbitmq.producer;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * 可靠消息发送服务
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReliableMessageSendService {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送持久化消息
     *
     * @param exchange     交换机
     * @param routingKey   路由键
     * @param businessKey  业务主键
     * @param payload      消息体
     * @return 消息ID
     */
    public String sendPersistentMessage(String exchange, String routingKey, String businessKey, Object payload) {
        String messageId = StrUtil.format("msg_{}_{}", DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss"), IdUtil.fastSimpleUUID());

        rabbitTemplate.convertAndSend(
                exchange,
                routingKey,
                payload,
                message -> {
                    message.getMessageProperties().setMessageId(messageId);
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    message.getMessageProperties().setTimestamp(DateUtil.date());
                    message.getMessageProperties().setHeader("x-message-id", messageId);
                    message.getMessageProperties().setHeader("x-business-key", businessKey);
                    message.getMessageProperties().setHeader("x-send-time", DateUtil.now());
                    return message;
                },
                new CorrelationData(messageId)
        );

        log.info("持久化消息已提交发送，messageId={}，businessKey={}，exchange={}，routingKey={}",
                messageId,
                businessKey,
                exchange,
                routingKey
        );
        return messageId;
    }
}
```

消息持久化注意事项：

1. 持久化消息可以降低 Broker 重启导致消息丢失的风险，但不等于绝对不丢消息。
2. 消息持久化会增加磁盘写入成本，核心业务使用，非核心高频临时消息可按需评估。
3. 生产者还需要开启 Publisher Confirm，否则无法确认消息是否真正到达 Broker。
4. 消费者需要手动 Ack，否则消费者异常时可能造成消息提前确认。

### 队列持久化

队列持久化表示 Queue 的元数据会保存到 RabbitMQ 中。Broker 重启后，持久化队列仍然存在。核心业务队列必须使用持久化队列。

队列持久化配置示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/RabbitMqReliableQueueConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 可靠队列配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class RabbitMqReliableQueueConfig {

    public static final String ORDER_PAID_QUEUE = "reliable.order.paid.queue";

    /**
     * 声明订单支付持久化队列
     *
     * @return 持久化队列
     */
    @Bean
    public Queue orderPaidReliableQueue() {
        return QueueBuilder
                .durable(ORDER_PAID_QUEUE)
                .build();
    }
}
```

队列持久化建议：

| 场景             | 是否持久化 | 说明             |
| ---------------- | ---------- | ---------------- |
| 订单消息         | 是         | 核心业务消息     |
| 支付消息         | 是         | 核心资金链路     |
| 库存消息         | 是         | 影响库存一致性   |
| 通知消息         | 建议是     | 取决于通知重要性 |
| 临时测试消息     | 否         | 可接受丢失       |
| RPC 临时回复队列 | 否         | 生命周期短       |

需要注意：队列创建后，`durable`、`exclusive`、`autoDelete` 和部分 `arguments` 参数不能随意修改。如果 RabbitMQ 中已经存在同名队列，而代码声明参数不一致，应用启动时可能出现 `PRECONDITION_FAILED` 异常。生产环境调整队列参数时，建议创建新队列并逐步迁移流量。

### Exchange 持久化

Exchange 持久化表示交换机元数据会保存到 RabbitMQ 中。Broker 重启后，持久化 Exchange 仍然存在。核心业务 Exchange 必须设置为持久化。

Exchange 持久化配置示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/RabbitMqReliableExchangeConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 可靠交换机配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class RabbitMqReliableExchangeConfig {

    public static final String ORDER_DIRECT_EXCHANGE = "reliable.order.direct.exchange";

    /**
     * 声明订单持久化交换机
     *
     * @return Direct Exchange
     */
    @Bean
    public DirectExchange orderReliableDirectExchange() {
        return ExchangeBuilder
                .directExchange(ORDER_DIRECT_EXCHANGE)
                .durable(true)
                .build();
    }
}
```

完整 Exchange、Queue、Binding 可靠配置如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/RabbitMqReliableConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 可靠消息资源配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class RabbitMqReliableConfig {

    public static final String ORDER_EXCHANGE = "reliable.order.exchange";

    public static final String ORDER_PAID_QUEUE = "reliable.order.paid.queue";

    public static final String ORDER_PAID_ROUTING_KEY = "order.paid";

    /**
     * 声明订单交换机
     *
     * @return Direct Exchange
     */
    @Bean
    public DirectExchange reliableOrderExchange() {
        return ExchangeBuilder
                .directExchange(ORDER_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 声明订单支付队列
     *
     * @return Queue
     */
    @Bean
    public Queue reliableOrderPaidQueue() {
        return QueueBuilder
                .durable(ORDER_PAID_QUEUE)
                .build();
    }

    /**
     * 绑定订单交换机和订单支付队列
     *
     * @return Binding
     */
    @Bean
    public Binding reliableOrderPaidBinding() {
        return BindingBuilder
                .bind(reliableOrderPaidQueue())
                .to(reliableOrderExchange())
                .with(ORDER_PAID_ROUTING_KEY);
    }
}
```

Exchange 持久化建议：

1. 核心业务 Exchange 使用 `durable=true`。
2. 临时广播、调试 Exchange 可以使用非持久化，但要明确风险。
3. Exchange 不存储消息，只负责路由；消息可靠性还需要 Queue 和 Message 持久化。
4. 生产环境 Exchange 命名、类型和参数应通过变更流程管理。

### Publisher Confirm

Publisher Confirm 用于确认生产者发送的消息是否到达 Exchange。开启 Confirm 后，RabbitMQ 会对消息进行确认回调。Confirm 成功表示消息到达 Exchange；Confirm 失败表示消息没有成功到达 Exchange。

Confirm 只能解决“消息是否到达 Exchange”的问题，不能证明消息已经进入 Queue，也不能证明消费者已经处理成功。

开启 Publisher Confirm 配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  rabbitmq:
    # correlated 表示 Confirm 回调中可以通过 CorrelationData 关联消息ID
    publisher-confirm-type: correlated
```

Confirm 回调配置如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/RabbitMqConfirmConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ Publisher Confirm 配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMqConfirmConfig {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 初始化 Confirm 回调
     */
    @PostConstruct
    public void initConfirmCallback() {
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            String messageId = correlationData == null ? "unknown" : correlationData.getId();

            if (ack) {
                log.info("MQ消息到达交换机，messageId={}", messageId);

                // 生产环境建议在这里更新消息记录表：CONFIRMED
                return;
            }

            log.error("MQ消息未到达交换机，messageId={}，原因={}",
                    messageId,
                    StrUtil.blankToDefault(cause, "未知原因")
            );

            // 生产环境建议在这里更新消息记录表：CONFIRM_FAILED，并触发补偿任务
        });
    }
}
```

Confirm 处理建议：

| Confirm 结果           | 含义                | 建议处理                             |
| ---------------------- | ------------------- | ------------------------------------ |
| `ack=true`             | 消息到达 Exchange   | 更新消息状态为确认成功               |
| `ack=false`            | 消息未到达 Exchange | 记录失败原因，执行重发或补偿         |
| `correlationData=null` | 无法定位消息        | 检查发送时是否传入 `CorrelationData` |
| `cause` 不为空         | Broker 返回失败原因 | 记录日志并告警                       |

Confirm 使用注意事项：

1. 发送消息时必须传入 `CorrelationData`，建议使用 `messageId` 作为 ID。
2. Confirm 成功不代表消息进入队列。
3. Confirm 失败不应只打印日志，核心业务需要补偿。
4. Confirm 回调中不要执行耗时业务，建议只更新状态或投递补偿任务。
5. 如果同一个应用中多个地方设置 `ConfirmCallback`，后设置的可能覆盖先设置的，应统一配置。

### Publisher Return

Publisher Return 用于处理消息到达 Exchange 后无法路由到 Queue 的情况。典型原因包括 Routing Key 写错、Binding 不存在、Exchange 类型不匹配等。

Return 解决的是“消息是否成功路由到 Queue”的问题。要触发 Return，需要同时开启 `publisher-returns=true` 和 `template.mandatory=true`。

配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  rabbitmq:
    # 开启不可路由消息返回
    publisher-returns: true
    template:
      # mandatory=true 时，不可路由消息会返回给生产者
      mandatory: true
```

Return 回调配置如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/RabbitMqReturnConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ Publisher Return 配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMqReturnConfig {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 初始化 Return 回调
     */
    @PostConstruct
    public void initReturnCallback() {
        rabbitTemplate.setReturnsCallback(returned -> {
            String messageId = returned.getMessage().getMessageProperties().getMessageId();

            log.error("MQ消息不可路由，messageId={}，exchange={}，routingKey={}，replyCode={}，replyText={}",
                    messageId,
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    returned.getReplyCode(),
                    returned.getReplyText()
            );

            // 生产环境建议在这里更新消息记录表：RETURNED，并触发告警或补偿
        });
    }
}
```

Return 常见原因如下：

| 原因              | 说明                             | 处理方式           |
| ----------------- | -------------------------------- | ------------------ |
| Routing Key 错误  | 发送时路由键与 Binding 不匹配    | 修正 Routing Key   |
| 未创建 Binding    | Exchange 没有绑定目标 Queue      | 创建绑定关系       |
| Queue 名称错误    | 绑定到错误队列或队列不存在       | 检查队列声明       |
| Exchange 类型错误 | Direct、Topic 等类型使用方式错误 | 检查 Exchange 类型 |
| 发送到错误 vhost  | 当前 vhost 没有对应资源          | 检查连接配置       |

Publisher Return 使用建议：

1. 核心业务必须开启 Return。
2. Return 回调中的消息应记录完整路由信息。
3. 不可路由消息通常属于配置错误或发布错误，需要告警。
4. Return 和 Confirm 是不同阶段的确认机制，应同时使用。
5. Return 发生时，Confirm 仍可能是成功的，因为消息确实已经到达 Exchange。

### Consumer Acknowledge

Consumer Acknowledge 用于控制消费者什么时候确认消息。自动确认模式下，消息投递给消费者后可能很快被确认，如果业务处理失败，消息也可能已经从队列中删除。核心业务建议使用手动确认模式。

确认模式如下：

| 模式     | 说明                         | 适用场景                     |
| -------- | ---------------------------- | ---------------------------- |
| `AUTO`   | 容器根据方法执行结果自动确认 | 普通业务，可接受框架自动处理 |
| `MANUAL` | 手动调用 Ack/Nack/Reject     | 核心业务推荐                 |
| `NONE`   | 不启用确认机制               | 极少使用，可靠性弱           |

手动确认配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        # 核心业务使用手动确认
        acknowledge-mode: manual
        # 消费异常后不默认重新入队，避免无限循环
        default-requeue-rejected: false
        # 单个消费者预取消息数
        prefetch: 10
```

消费者手动 Ack 示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/consumer/ReliableOrderConsumer.java`

```java
package io.github.atengk.rabbitmq.consumer;

import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.Channel;
import io.github.atengk.rabbitmq.config.RabbitMqReliableConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 可靠订单消息消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class ReliableOrderConsumer {

    /**
     * 消费订单支付消息
     *
     * @param payload 消息体
     * @param message 原始消息
     * @param channel RabbitMQ Channel
     * @throws IOException Ack 或 Nack 失败时抛出
     */
    @RabbitListener(queues = RabbitMqReliableConfig.ORDER_PAID_QUEUE)
    public void consumeOrderPaidMessage(String payload, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();

        try {
            log.info("收到订单支付消息，messageId={}，deliveryTag={}，payload={}",
                    StrUtil.blankToDefault(messageId, "unknown"),
                    deliveryTag,
                    payload
            );

            // 执行业务处理
            handleBusiness(payload);

            // multiple=false 表示只确认当前这条消息
            channel.basicAck(deliveryTag, false);
            log.info("订单支付消息确认成功，messageId={}，deliveryTag={}", messageId, deliveryTag);
        } catch (IllegalArgumentException ex) {
            log.error("订单支付消息参数异常，消息进入死信或丢弃，messageId={}，原因={}",
                    messageId,
                    ex.getMessage(),
                    ex
            );

            // requeue=false 表示不重新入队，若队列配置死信交换机则进入死信队列
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception ex) {
            log.error("订单支付消息消费异常，messageId={}，原因={}",
                    messageId,
                    ex.getMessage(),
                    ex
            );

            // 核心业务建议配合重试和死信，不建议无限 requeue=true
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 处理业务逻辑
     *
     * @param payload 消息体
     */
    private void handleBusiness(String payload) {
        if (StrUtil.isBlank(payload)) {
            throw new IllegalArgumentException("消息体不能为空");
        }

        log.info("执行业务处理，payload={}", payload);
    }
}
```

Ack、Nack、Reject 对比：

| 方法                                   | 说明                                | 常见用途               |
| -------------------------------------- | ----------------------------------- | ---------------------- |
| `basicAck(deliveryTag, false)`         | 确认单条消息成功                    | 业务处理成功           |
| `basicAck(deliveryTag, true)`          | 批量确认当前 deliveryTag 之前的消息 | 批量消费场景           |
| `basicNack(deliveryTag, false, true)`  | 拒绝单条消息并重新入队              | 临时异常，但要限制次数 |
| `basicNack(deliveryTag, false, false)` | 拒绝单条消息且不重新入队            | 配合死信队列           |
| `basicReject(deliveryTag, false)`      | 拒绝单条消息且不重新入队            | 单条异常消息           |

Consumer Acknowledge 建议：

1. 业务处理成功后再 Ack。
2. 参数错误、格式错误不建议重新入队。
3. 临时异常可以重试，但必须限制次数。
4. 核心业务队列必须配合死信队列。
5. 不要在业务未完成时提前 Ack。
6. 不要对所有异常都 `requeue=true`，否则可能造成无限失败循环。

### 生产端可靠性

生产端可靠性关注消息从业务系统产生到成功投递 RabbitMQ 的过程。生产端常见风险包括业务事务提交成功但消息发送失败、消息到达 Exchange 失败、消息不可路由、发送异常未补偿、重复发送等。

生产端可靠性设计目标如下：

| 目标   | 说明                                   |
| ------ | -------------------------------------- |
| 不漏发 | 业务成功后消息必须有发送记录和补偿能力 |
| 可确认 | 能确认消息是否到达 Exchange            |
| 可路由 | 能发现消息是否无法进入 Queue           |
| 可追踪 | 能通过 messageId 查询消息链路          |
| 可补偿 | 发送失败或确认失败后可以重发           |
| 可防重 | 避免同一业务事件重复发送造成异常       |

推荐生产端可靠流程：

```text
业务操作开始
    -> 写入业务数据
    -> 写入本地消息表，状态为待发送
    -> 提交数据库事务
    -> 异步发送 MQ
    -> Confirm 成功，更新为已确认
    -> Return 发生，更新为不可路由
    -> 定时任务扫描失败或超时消息并补偿重发
```

简化消息记录表设计如下。

```sql
CREATE TABLE mq_message_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(100) NOT NULL COMMENT '消息ID',
    business_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    business_key VARCHAR(100) NOT NULL COMMENT '业务主键',
    exchange_name VARCHAR(128) NOT NULL COMMENT '交换机',
    routing_key VARCHAR(128) NOT NULL COMMENT '路由键',
    message_body TEXT NOT NULL COMMENT '消息体',
    send_status VARCHAR(32) NOT NULL COMMENT '发送状态',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    error_message VARCHAR(500) DEFAULT NULL COMMENT '异常信息',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_business_key (business_key),
    KEY idx_send_status (send_status)
) COMMENT='MQ消息记录表';
```

生产端状态建议：

| 状态             | 说明              |
| ---------------- | ----------------- |
| `PENDING`        | 待发送            |
| `SENT`           | 已提交发送        |
| `CONFIRMED`      | Exchange 确认成功 |
| `CONFIRM_FAILED` | Exchange 确认失败 |
| `RETURNED`       | 消息不可路由      |
| `FAILED`         | 发送异常          |
| `RETRYING`       | 重试中            |

生产端可靠性建议：

1. 核心业务使用本地消息表或 Outbox Pattern。
2. 业务数据和消息记录尽量在同一个数据库事务中提交。
3. 发送 MQ 不建议放在数据库事务内部长时间阻塞。
4. Publisher Confirm 失败必须记录并补偿。
5. Publisher Return 必须记录并告警。
6. 发送失败消息需要定时扫描重发。
7. 重发必须有最大次数，超过后进入人工处理。
8. 重复发送不可完全避免，消费者必须做幂等。

### 消费端可靠性

消费端可靠性关注消息从 Queue 投递到消费者后，是否被正确处理。消费端常见风险包括消费者处理失败、处理成功但 Ack 失败、重复消费、业务异常无限重试、死信无人处理等。

消费端可靠性设计目标如下：

| 目标   | 说明                             |
| ------ | -------------------------------- |
| 不误删 | 业务成功后才 Ack                 |
| 可重试 | 临时失败可以重试                 |
| 可隔离 | 多次失败进入死信队列             |
| 可幂等 | 重复投递不会重复执行业务副作用   |
| 可追踪 | 消费过程有完整日志               |
| 可补偿 | 死信和失败消息可以人工处理或重发 |

推荐消费端可靠流程：

```text
消费者收到消息
    -> 读取 messageId 和 businessKey
    -> 幂等判断
    -> 参数校验
    -> 执行业务逻辑
    -> 记录消费成功
    -> 手动 Ack
异常发生
    -> 判断异常类型
    -> 可重试异常进入重试
    -> 不可重试异常 Nack 且 requeue=false
    -> 消息进入死信队列
    -> 死信消费者记录并告警
```

消费幂等接口示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/service/ConsumerIdempotentService.java`

```java
package io.github.atengk.rabbitmq.service;

/**
 * 消费幂等服务
 *
 * @author Ateng
 * @since 2026-05-11
 */
public interface ConsumerIdempotentService {

    /**
     * 尝试开始消费
     *
     * @param messageId 消息ID
     * @return 是否允许继续消费
     */
    boolean tryConsume(String messageId);

    /**
     * 标记消费成功
     *
     * @param messageId 消息ID
     */
    void markSuccess(String messageId);
}
```

基于 Redis 的消费幂等实现示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/service/impl/RedisConsumerIdempotentServiceImpl.java`

```java
package io.github.atengk.rabbitmq.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.rabbitmq.service.ConsumerIdempotentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis 消费幂等服务实现
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisConsumerIdempotentServiceImpl implements ConsumerIdempotentService {

    private static final String CONSUMING_KEY_PREFIX = "mq:consuming:";

    private static final String CONSUMED_KEY_PREFIX = "mq:consumed:";

    private static final Duration CONSUMING_TTL = Duration.ofMinutes(10);

    private static final Duration CONSUMED_TTL = Duration.ofDays(7);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试开始消费
     *
     * @param messageId 消息ID
     * @return 是否允许继续消费
     */
    @Override
    public boolean tryConsume(String messageId) {
        if (StrUtil.isBlank(messageId)) {
            return true;
        }

        String consumedKey = CONSUMED_KEY_PREFIX + messageId;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(consumedKey))) {
            log.warn("消息已消费，跳过重复处理，messageId={}", messageId);
            return false;
        }

        String consumingKey = CONSUMING_KEY_PREFIX + messageId;
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(consumingKey, "1", CONSUMING_TTL);
        return Boolean.TRUE.equals(locked);
    }

    /**
     * 标记消费成功
     *
     * @param messageId 消息ID
     */
    @Override
    public void markSuccess(String messageId) {
        if (StrUtil.isBlank(messageId)) {
            return;
        }

        stringRedisTemplate.delete(CONSUMING_KEY_PREFIX + messageId);
        stringRedisTemplate.opsForValue().set(CONSUMED_KEY_PREFIX + messageId, "1", CONSUMED_TTL);
        log.info("消息消费幂等标记成功，messageId={}", messageId);
    }
}
```

消费端完整可靠处理示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/consumer/ReliableMessageConsumer.java`

```java
package io.github.atengk.rabbitmq.consumer;

import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.Channel;
import io.github.atengk.rabbitmq.config.RabbitMqReliableConfig;
import io.github.atengk.rabbitmq.service.ConsumerIdempotentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 可靠消息消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReliableMessageConsumer {

    private final ConsumerIdempotentService consumerIdempotentService;

    /**
     * 消费可靠订单消息
     *
     * @param payload 消息体
     * @param message 原始消息
     * @param channel RabbitMQ Channel
     * @throws IOException Ack 或 Nack 失败时抛出
     */
    @RabbitListener(queues = RabbitMqReliableConfig.ORDER_PAID_QUEUE)
    public void consume(String payload, Message message, Channel channel) throws IOException {
        long startTime = System.currentTimeMillis();
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();
        String businessKey = String.valueOf(message.getMessageProperties().getHeaders().get("x-business-key"));

        try {
            log.info("收到可靠消息，messageId={}，businessKey={}，deliveryTag={}",
                    messageId,
                    businessKey,
                    deliveryTag
            );

            if (!consumerIdempotentService.tryConsume(messageId)) {
                channel.basicAck(deliveryTag, false);
                log.info("重复消息已确认，messageId={}，businessKey={}", messageId, businessKey);
                return;
            }

            if (StrUtil.isBlank(payload)) {
                throw new IllegalArgumentException("消息体不能为空");
            }

            // 执行业务处理
            handleBusiness(payload, businessKey);

            consumerIdempotentService.markSuccess(messageId);
            channel.basicAck(deliveryTag, false);

            long costMs = System.currentTimeMillis() - startTime;
            log.info("可靠消息消费成功，messageId={}，businessKey={}，costMs={}",
                    messageId,
                    businessKey,
                    costMs
            );
        } catch (IllegalArgumentException ex) {
            log.error("可靠消息参数异常，进入死信，messageId={}，businessKey={}，原因={}",
                    messageId,
                    businessKey,
                    ex.getMessage(),
                    ex
            );

            channel.basicNack(deliveryTag, false, false);
        } catch (Exception ex) {
            log.error("可靠消息消费失败，进入死信或补偿流程，messageId={}，businessKey={}，原因={}",
                    messageId,
                    businessKey,
                    ex.getMessage(),
                    ex
            );

            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 执行业务处理
     *
     * @param payload     消息体
     * @param businessKey 业务主键
     */
    private void handleBusiness(String payload, String businessKey) {
        log.info("执行业务处理，businessKey={}，payload={}", businessKey, payload);
    }
}
```

消费端可靠性建议：

1. 核心消费者使用手动 Ack。
2. 消费成功后再调用 `basicAck`。
3. 消费失败不要无限 `requeue=true`。
4. 所有核心队列配置死信交换机和死信队列。
5. 消费者必须具备幂等控制。
6. 幂等控制需要考虑并发消费场景。
7. 消费日志必须包含 `messageId`、业务主键、队列名和耗时。
8. 死信消息必须可查询、可重发、可忽略、可标记处理。
9. 消费失败后的最终处理策略应由业务决定，不能只依赖 MQ 自动重试。

整体可靠性建议如下：

| 阶段        | 可靠性措施                                              |
| ----------- | ------------------------------------------------------- |
| 消息创建    | 生成唯一 `messageId`，记录业务主键                      |
| 生产发送    | 本地消息表、持久化消息、Confirm、Return                 |
| Broker 存储 | 持久化 Exchange、持久化 Queue、消息持久化               |
| 消息消费    | 手动 Ack、异常分类、重试、死信                          |
| 重复投递    | Redis 或数据库幂等控制                                  |
| 异常补偿    | 定时扫描、人工处理、死信重发                            |
| 链路排查    | 发送日志、Confirm 日志、Return 日志、消费日志、死信日志 |


## Publisher Confirm 实现

本章节用于说明生产者发布确认机制的实现方式。Publisher Confirm 用于确认消息是否成功到达 RabbitMQ Exchange，它解决的是“生产者到 Exchange”这一段链路的可靠性问题。Confirm 成功不代表消息一定进入队列，也不代表消费者已经消费成功。

### ConfirmCallback 配置

ConfirmCallback 是 `RabbitTemplate` 提供的发布确认回调。开启 Publisher Confirm 后，RabbitMQ 会在消息到达 Exchange 或投递失败时回调生产者。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  rabbitmq:
    # 开启发布确认，correlated 模式可以通过 CorrelationData 关联 messageId
    publisher-confirm-type: correlated
    # 开启不可路由消息返回，后续 Publisher Return 使用
    publisher-returns: true
    template:
      # mandatory=true 时，不可路由消息会触发 ReturnCallback
      mandatory: true
```

`publisher-confirm-type` 常见取值如下：

| 取值         | 说明                       |
| ------------ | -------------------------- |
| `none`       | 不开启发布确认             |
| `simple`     | 开启简单确认模式           |
| `correlated` | 开启关联确认模式，推荐使用 |

生产环境推荐使用 `correlated`，因为它可以通过 `CorrelationData` 将 Confirm 回调和业务消息 ID 关联起来。

Confirm 回调建议统一配置，不要在多个业务类中重复设置，否则后设置的回调可能覆盖先设置的回调。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/RabbitMqPublisherConfirmConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.rabbitmq.service.MqMessageRecordService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ Publisher Confirm 配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMqPublisherConfirmConfig {

    private final RabbitTemplate rabbitTemplate;

    private final MqMessageRecordService mqMessageRecordService;

    /**
     * 初始化 Publisher Confirm 回调
     */
    @PostConstruct
    public void initConfirmCallback() {
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            String messageId = correlationData == null ? null : correlationData.getId();

            if (StrUtil.isBlank(messageId)) {
                log.error("MQ发布确认回调缺少messageId，ack={}，cause={}", ack, cause);
                return;
            }

            if (ack) {
                mqMessageRecordService.markConfirmSuccess(messageId);
                log.info("MQ消息发布确认成功，messageId={}", messageId);
                return;
            }

            String failReason = StrUtil.blankToDefault(cause, "RabbitMQ未返回失败原因");
            mqMessageRecordService.markConfirmFailed(messageId, failReason);
            log.error("MQ消息发布确认失败，messageId={}，原因={}", messageId, failReason);
        });
    }
}
```

配套消息记录服务接口如下，用于将 Confirm 结果持久化到数据库或其他可靠存储中。

文件位置：`src/main/java/io/github/atengk/rabbitmq/service/MqMessageRecordService.java`

```java
package io.github.atengk.rabbitmq.service;

/**
 * MQ 消息记录服务
 *
 * @author Ateng
 * @since 2026-05-11
 */
public interface MqMessageRecordService {

    /**
     * 标记消息发布确认成功
     *
     * @param messageId 消息ID
     */
    void markConfirmSuccess(String messageId);

    /**
     * 标记消息发布确认失败
     *
     * @param messageId 消息ID
     * @param reason    失败原因
     */
    void markConfirmFailed(String messageId, String reason);

    /**
     * 标记消息不可路由
     *
     * @param messageId  消息ID
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param reason     失败原因
     */
    void markReturned(String messageId, String exchange, String routingKey, String reason);
}
```

如果当前阶段还没有消息记录表，可以先在实现类中记录日志，后续再替换为数据库实现。

文件位置：`src/main/java/io/github/atengk/rabbitmq/service/impl/LogMqMessageRecordServiceImpl.java`

```java
package io.github.atengk.rabbitmq.service.impl;

import io.github.atengk.rabbitmq.service.MqMessageRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 日志型 MQ 消息记录服务实现
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
public class LogMqMessageRecordServiceImpl implements MqMessageRecordService {

    /**
     * 标记消息发布确认成功
     *
     * @param messageId 消息ID
     */
    @Override
    public void markConfirmSuccess(String messageId) {
        log.info("更新MQ消息状态为CONFIRMED，messageId={}", messageId);
    }

    /**
     * 标记消息发布确认失败
     *
     * @param messageId 消息ID
     * @param reason    失败原因
     */
    @Override
    public void markConfirmFailed(String messageId, String reason) {
        log.warn("更新MQ消息状态为CONFIRM_FAILED，messageId={}，reason={}", messageId, reason);
    }

    /**
     * 标记消息不可路由
     *
     * @param messageId  消息ID
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param reason     失败原因
     */
    @Override
    public void markReturned(String messageId, String exchange, String routingKey, String reason) {
        log.warn("更新MQ消息状态为RETURNED，messageId={}，exchange={}，routingKey={}，reason={}",
                messageId,
                exchange,
                routingKey,
                reason
        );
    }
}
```

### 消息确认流程

Publisher Confirm 的核心流程是：生产者发送消息时携带 `CorrelationData`，RabbitMQ 处理后回调 ConfirmCallback，生产者根据回调结果更新消息状态。

推荐流程如下：

```text
业务产生消息
    -> 生成 messageId
    -> 写入消息记录，状态为 PENDING
    -> 调用 RabbitTemplate.convertAndSend
    -> 更新消息状态为 SENT
    -> RabbitMQ 回调 ConfirmCallback
        -> ack=true，更新为 CONFIRMED
        -> ack=false，更新为 CONFIRM_FAILED
    -> 定时任务扫描失败消息并重发
```

发送消息时必须传入 `CorrelationData`。

下面的代码用于发送带 Confirm 关联 ID 的消息，适合作为生产者统一发送服务的一部分。

文件位置：`src/main/java/io/github/atengk/rabbitmq/producer/ConfirmMessageProducer.java`

```java
package io.github.atengk.rabbitmq.producer;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Confirm 消息生产者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfirmMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送需要发布确认的消息
     *
     * @param exchange    交换机
     * @param routingKey  路由键
     * @param businessKey 业务主键
     * @param payload     消息体
     * @return 消息ID
     */
    public String sendWithConfirm(String exchange, String routingKey, String businessKey, Object payload) {
        String messageId = StrUtil.format("confirm_{}_{}", DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss"), IdUtil.fastSimpleUUID());
        CorrelationData correlationData = new CorrelationData(messageId);

        rabbitTemplate.convertAndSend(
                exchange,
                routingKey,
                payload,
                message -> {
                    message.getMessageProperties().setMessageId(messageId);
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    message.getMessageProperties().setTimestamp(DateUtil.date());
                    message.getMessageProperties().setHeader("x-message-id", messageId);
                    message.getMessageProperties().setHeader("x-business-key", businessKey);
                    message.getMessageProperties().setHeader("x-send-time", DateUtil.now());
                    return message;
                },
                correlationData
        );

        log.info("MQ消息已提交发送，messageId={}，businessKey={}，exchange={}，routingKey={}",
                messageId,
                businessKey,
                exchange,
                routingKey
        );
        return messageId;
    }
}
```

确认流程中的关键点如下：

| 阶段         | 说明                                           |
| ------------ | ---------------------------------------------- |
| 发送前       | 生成 `messageId`，写入消息记录                 |
| 发送时       | 使用 `CorrelationData(messageId)`              |
| Confirm 成功 | 表示消息到达 Exchange                          |
| Confirm 失败 | 表示消息未成功到达 Exchange                    |
| Return 回调  | 表示消息到达 Exchange 但无法路由到 Queue       |
| 消费成功     | 由消费者 Ack 和消费日志判断，不由 Confirm 判断 |

### Confirm 成功处理

Confirm 成功表示 RabbitMQ Broker 已经确认消息到达 Exchange。此时可以将消息记录状态更新为 `CONFIRMED`。

推荐成功处理动作如下：

| 动作           | 说明                                 |
| -------------- | ------------------------------------ |
| 更新状态       | 将消息记录状态更新为 `CONFIRMED`     |
| 记录日志       | 打印 `messageId`、业务主键、确认时间 |
| 清理临时状态   | 如果有 Redis 发送锁，可以释放        |
| 不做耗时操作   | Confirm 回调线程不应执行复杂业务     |
| 不代表消费成功 | 消费结果需要看消费者日志或业务状态   |

数据库状态更新示例 SQL：

```sql
UPDATE mq_message_record
SET send_status = 'CONFIRMED',
    confirm_time = NOW(),
    update_time = NOW()
WHERE message_id = ?;
```

如果使用 MyBatis-Plus，可以在 `markConfirmSuccess` 中更新状态。这里给出核心逻辑示例：

```java
public void markConfirmSuccess(String messageId) {
    log.info("MQ消息确认成功，准备更新消息记录，messageId={}", messageId);

    // 示例：更新消息记录表状态为 CONFIRMED
    // lambdaUpdate()
    //     .eq(MqMessageRecord::getMessageId, messageId)
    //     .set(MqMessageRecord::getSendStatus, "CONFIRMED")
    //     .set(MqMessageRecord::getConfirmTime, LocalDateTime.now())
    //     .set(MqMessageRecord::getUpdateTime, LocalDateTime.now())
    //     .update();
}
```

Confirm 成功后仍需要注意不可路由场景。因为消息可能已经到达 Exchange，但没有匹配到任何 Queue。此类问题由 Publisher Return 处理。

### Confirm 失败处理

Confirm 失败表示消息没有被 RabbitMQ Exchange 正常确认。常见原因包括 Broker 异常、Exchange 不存在、连接异常、Channel 异常、权限问题等。

推荐失败处理动作如下：

| 动作         | 说明                                  |
| ------------ | ------------------------------------- |
| 更新状态     | 将消息记录状态更新为 `CONFIRM_FAILED` |
| 保存原因     | 保存 Confirm 回调中的 `cause`         |
| 记录错误日志 | 打印 `messageId` 和失败原因           |
| 触发告警     | 核心业务消息确认失败需要告警          |
| 进入补偿     | 由定时任务或补偿任务执行重发          |

数据库状态更新示例 SQL：

```sql
UPDATE mq_message_record
SET send_status = 'CONFIRM_FAILED',
    error_message = ?,
    update_time = NOW()
WHERE message_id = ?;
```

Confirm 失败处理示例：

```java
public void markConfirmFailed(String messageId, String reason) {
    log.error("MQ消息确认失败，准备更新消息记录，messageId={}，reason={}", messageId, reason);

    // 示例：更新消息记录表状态为 CONFIRM_FAILED
    // lambdaUpdate()
    //     .eq(MqMessageRecord::getMessageId, messageId)
    //     .set(MqMessageRecord::getSendStatus, "CONFIRM_FAILED")
    //     .set(MqMessageRecord::getErrorMessage, reason)
    //     .set(MqMessageRecord::getUpdateTime, LocalDateTime.now())
    //     .update();

    // 示例：核心业务可在这里发送告警或投递补偿任务
}
```

Confirm 失败不建议在回调中直接无限重发。更稳妥的方式是更新消息状态，由补偿任务按重试次数、重试间隔和最大重试次数进行控制。

### 消息重发策略

消息重发用于处理发送异常、Confirm 失败、长时间未确认等情况。重发必须具备次数限制和状态控制，不能无限重发，否则可能造成消息风暴。

推荐重发触发条件如下：

| 触发条件         | 说明                                 |
| ---------------- | ------------------------------------ |
| `SEND_FAILED`    | 调用 `RabbitTemplate` 发送时直接异常 |
| `CONFIRM_FAILED` | Confirm 回调失败                     |
| `PENDING` 超时   | 消息长时间未发送                     |
| `SENT` 超时      | 消息长时间未收到 Confirm             |
| `RETURNED`       | 不可路由消息，通常先修复配置再重发   |

消息记录表建议增加以下字段：

```sql
ALTER TABLE mq_message_record
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    ADD COLUMN max_retry_count INT NOT NULL DEFAULT 5 COMMENT '最大重试次数',
    ADD COLUMN next_retry_time DATETIME DEFAULT NULL COMMENT '下次重试时间',
    ADD COLUMN confirm_time DATETIME DEFAULT NULL COMMENT '确认时间';
```

重发任务查询条件示例：

```sql
SELECT *
FROM mq_message_record
WHERE send_status IN ('SEND_FAILED', 'CONFIRM_FAILED')
  AND retry_count < max_retry_count
  AND (next_retry_time IS NULL OR next_retry_time <= NOW())
ORDER BY create_time ASC
LIMIT 100;
```

重发策略建议：

| 策略         | 说明                                        |
| ------------ | ------------------------------------------- |
| 最大重试次数 | 建议 3 到 5 次                              |
| 重试间隔     | 可以固定间隔，也可以指数退避                |
| 批量大小     | 每次扫描限制数量，避免瞬时压力              |
| 状态控制     | 重发前更新为 `RETRYING`，避免多实例重复重发 |
| 失败兜底     | 超过最大次数后标记为 `RETRY_FAILED`         |
| 人工处理     | 核心业务重试失败后进入人工处理              |

重发任务核心示例：

```java
public void retryFailedMessages() {
    log.info("开始扫描需要重发的MQ消息");

    // 1. 查询 SEND_FAILED、CONFIRM_FAILED 且未超过最大重试次数的消息
    // 2. 将消息状态更新为 RETRYING，防止多实例重复处理
    // 3. 调用 RabbitTemplate 重新发送
    // 4. 更新 retry_count、next_retry_time、send_status
    // 5. 超过最大重试次数后标记 RETRY_FAILED 并告警

    log.info("MQ失败消息扫描完成");
}
```

如果要在分布式环境中运行重发任务，建议使用数据库乐观锁、Redis 分布式锁或任务调度平台控制并发，避免多个实例同时重发同一条消息。

### Confirm 日志记录

Confirm 日志用于定位消息是否到达 Exchange。生产环境中，Confirm 日志应包含消息 ID、业务类型、业务主键、确认结果、失败原因和确认时间。

推荐日志字段如下：

| 字段           | 说明         |
| -------------- | ------------ |
| `messageId`    | 消息唯一 ID  |
| `businessType` | 业务类型     |
| `businessKey`  | 业务主键     |
| `ack`          | 是否确认成功 |
| `cause`        | 失败原因     |
| `confirmTime`  | 确认时间     |
| `exchange`     | 交换机       |
| `routingKey`   | 路由键       |

成功日志示例：

```text
MQ消息发布确认成功，messageId=confirm_20260511103000_xxx，businessType=order_paid，businessKey=ORDER202605110001，exchange=reliable.order.exchange，routingKey=order.paid
```

失败日志示例：

```text
MQ消息发布确认失败，messageId=confirm_20260511103000_xxx，businessType=order_paid，businessKey=ORDER202605110001，原因=channel error
```

日志建议：

1. Confirm 成功日志可以使用 `INFO`。
2. Confirm 失败日志必须使用 `ERROR`。
3. 高频非核心消息可以降低成功日志级别，避免日志过大。
4. 失败日志必须包含 `messageId`，否则无法和消息记录表关联。
5. 核心业务 Confirm 失败应触发告警，而不是只写日志。

## Publisher Return 实现

本章节用于说明生产者不可路由消息处理机制。Publisher Return 用于处理消息已经到达 Exchange，但 Exchange 没有找到匹配 Queue 的情况。它解决的是“Exchange 到 Queue”这一段链路的可靠性问题。

### ReturnCallback 配置

ReturnCallback 需要配合 `publisher-returns=true` 和 `template.mandatory=true` 使用。否则消息不可路由时，可能不会返回给生产者。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  rabbitmq:
    # 开启不可路由消息返回
    publisher-returns: true
    template:
      # 必须开启 mandatory，消息无法路由到队列时才会返回给生产者
      mandatory: true
```

Return 回调配置如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/RabbitMqPublisherReturnConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.rabbitmq.service.MqMessageRecordService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ Publisher Return 配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMqPublisherReturnConfig {

    private final RabbitTemplate rabbitTemplate;

    private final MqMessageRecordService mqMessageRecordService;

    /**
     * 初始化 Publisher Return 回调
     */
    @PostConstruct
    public void initReturnCallback() {
        rabbitTemplate.setReturnsCallback(returned -> {
            Message message = returned.getMessage();
            String messageId = message.getMessageProperties().getMessageId();

            if (StrUtil.isBlank(messageId)) {
                Object headerMessageId = message.getMessageProperties().getHeaders().get("x-message-id");
                messageId = headerMessageId == null ? "unknown" : String.valueOf(headerMessageId);
            }

            String reason = StrUtil.format(
                    "replyCode={}, replyText={}",
                    returned.getReplyCode(),
                    returned.getReplyText()
            );

            mqMessageRecordService.markReturned(
                    messageId,
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    reason
            );

            log.error("MQ消息不可路由，messageId={}，exchange={}，routingKey={}，replyCode={}，replyText={}",
                    messageId,
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    returned.getReplyCode(),
                    returned.getReplyText()
            );
        });
    }
}
```

ReturnCallback 中可以获取以下信息：

| 信息         | 说明              |
| ------------ | ----------------- |
| `message`    | 被返回的原始消息  |
| `exchange`   | 消息发送的交换机  |
| `routingKey` | 消息发送的路由键  |
| `replyCode`  | RabbitMQ 返回码   |
| `replyText`  | RabbitMQ 返回原因 |

### 不可路由消息处理

不可路由消息表示消息已经到达 Exchange，但没有匹配到任何 Queue。常见原因包括 Binding 未创建、Routing Key 错误、Exchange 类型使用错误、发送到错误 vhost 等。

常见不可路由原因如下：

| 原因              | 示例                                  | 处理方式                         |
| ----------------- | ------------------------------------- | -------------------------------- |
| Routing Key 错误  | 发送 `order.pay`，绑定是 `order.paid` | 修正发送方 Routing Key           |
| Binding 缺失      | Exchange 没有绑定目标队列             | 创建 Binding                     |
| Queue 未声明      | 目标队列不存在                        | 创建 Queue 并绑定                |
| Exchange 类型错误 | Topic 当作 Direct 使用                | 修正 Exchange 类型或 Binding Key |
| vhost 错误        | 应用连接 `/test`，资源在 `/dev`       | 修正 `virtual-host`              |
| 环境资源不一致    | 测试环境缺少绑定                      | 补齐环境初始化脚本               |

不可路由消息处理流程如下：

```text
消息发送到 Exchange
    -> Exchange 根据 Routing Key 查找匹配 Queue
    -> 未找到匹配 Queue
    -> mandatory=true，消息返回生产者
    -> ReturnCallback 记录 RETURNED 状态
    -> 触发告警
    -> 修复 Binding / Routing Key / vhost
    -> 人工或任务重发消息
```

不可路由消息通常属于配置问题或发布问题，不建议自动立即重发。因为如果 Binding 或 Routing Key 没有修复，立即重发仍然会继续 Return。

### mandatory 参数配置

`mandatory` 参数决定消息无法路由到 Queue 时是否返回给生产者。

| mandatory | 行为                                        |
| --------- | ------------------------------------------- |
| `false`   | 不可路由消息可能被 RabbitMQ 直接丢弃        |
| `true`    | 不可路由消息返回生产者，触发 ReturnCallback |

Spring Boot 推荐配置方式如下：

```yaml
spring:
  rabbitmq:
    publisher-returns: true
    template:
      mandatory: true
```

也可以通过代码设置：

```java
rabbitTemplate.setMandatory(true);
```

项目中建议统一使用配置文件方式，不建议在多个业务类中分别设置。只要是核心业务消息，都应开启 `mandatory=true`，否则不可路由消息可能无法被生产者感知。

需要注意：`mandatory=true` 只处理“Exchange 存在但无法路由到 Queue”的场景。如果 Exchange 本身不存在，通常会触发 Channel 异常或 Confirm 失败，不属于 Return 的典型处理范围。

### Return 异常记录

Return 异常记录用于持久化不可路由消息，便于后续排查和补偿。只打印日志不能满足生产环境可靠性要求，核心业务应将 Return 结果写入消息记录表。

推荐记录字段如下：

| 字段           | 说明                    |
| -------------- | ----------------------- |
| `messageId`    | 消息 ID                 |
| `businessType` | 业务类型                |
| `businessKey`  | 业务主键                |
| `exchangeName` | 交换机                  |
| `routingKey`   | 路由键                  |
| `replyCode`    | RabbitMQ 返回码         |
| `replyText`    | RabbitMQ 返回说明       |
| `messageBody`  | 消息体                  |
| `sendStatus`   | 状态，建议为 `RETURNED` |
| `returnTime`   | Return 发生时间         |

消息记录表可增加 Return 字段：

```sql
ALTER TABLE mq_message_record
    ADD COLUMN return_code INT DEFAULT NULL COMMENT 'Return返回码',
    ADD COLUMN return_text VARCHAR(500) DEFAULT NULL COMMENT 'Return返回说明',
    ADD COLUMN return_time DATETIME DEFAULT NULL COMMENT 'Return发生时间';
```

状态更新 SQL 示例：

```sql
UPDATE mq_message_record
SET send_status = 'RETURNED',
    return_code = ?,
    return_text = ?,
    return_time = NOW(),
    error_message = ?,
    update_time = NOW()
WHERE message_id = ?;
```

Return 日志示例：

```text
MQ消息不可路由，messageId=confirm_20260511103000_xxx，exchange=reliable.order.exchange，routingKey=order.pay，replyCode=312，replyText=NO_ROUTE
```

Return 异常记录建议：

1. Return 必须记录为 `ERROR` 日志。
2. 必须保存 `exchange` 和 `routingKey`。
3. 必须保存 RabbitMQ 返回的 `replyCode` 和 `replyText`。
4. 必须触发告警或进入异常消息处理流程。
5. 不能忽略 Return，否则业务会误以为消息已经进入队列。

### Return 补偿处理

Return 补偿用于处理不可路由消息。由于 Return 通常是路由配置错误导致，补偿流程应先修复配置，再执行重发。

推荐补偿流程如下：

```text
发现 RETURNED 消息
    -> 查看 messageId、exchange、routingKey、replyText
    -> 检查 Exchange 是否存在
    -> 检查 Queue 是否存在
    -> 检查 Binding 是否正确
    -> 检查应用 vhost 是否正确
    -> 修复配置
    -> 重发消息
    -> 确认 Confirm 成功且没有再次 Return
```

Return 补偿任务查询示例：

```sql
SELECT *
FROM mq_message_record
WHERE send_status = 'RETURNED'
  AND retry_count < max_retry_count
ORDER BY return_time ASC
LIMIT 100;
```

Return 补偿不建议盲目自动重发。更稳妥的策略如下：

| 场景             | 处理策略             |
| ---------------- | -------------------- |
| 配置未修复       | 不重发，持续告警     |
| Routing Key 写错 | 修正代码或配置后重发 |
| Binding 缺失     | 创建 Binding 后重发  |
| vhost 错误       | 修正连接配置后重发   |
| 临时发布顺序问题 | 资源创建完成后重发   |
| 超过最大重试次数 | 标记人工处理         |

Return 补偿方法示例：

```java
public void retryReturnedMessages() {
    log.info("开始扫描不可路由MQ消息");

    // 1. 查询 send_status = RETURNED 且未超过最大重试次数的消息
    // 2. 检查 Exchange、Queue、Binding 是否已经修复
    // 3. 将消息状态更新为 RETRYING
    // 4. 调用 RabbitTemplate 重新发送原始消息体
    // 5. 更新 retry_count、next_retry_time
    // 6. 若再次 Return，则继续记录 RETURNED 并触发告警

    log.info("不可路由MQ消息扫描完成");
}
```

生产环境建议提供一个管理端接口用于处理 Return 消息：

| 接口             | 说明                                        |
| ---------------- | ------------------------------------------- |
| 查询不可路由消息 | 按状态、业务类型、业务主键、时间范围查询    |
| 查看消息详情     | 查看消息体、Exchange、Routing Key、失败原因 |
| 手动重发         | 修复配置后人工触发重发                      |
| 标记忽略         | 确认无需处理时标记忽略                      |
| 导出异常消息     | 便于排查和审计                              |

最终建议将 Confirm 和 Return 作为生产者可靠性的一组能力同时启用：

| 机制              | 判断内容                         | 失败状态         |
| ----------------- | -------------------------------- | ---------------- |
| Publisher Confirm | 消息是否到达 Exchange            | `CONFIRM_FAILED` |
| Publisher Return  | 消息是否从 Exchange 路由到 Queue | `RETURNED`       |

核心业务消息只有在 Confirm 成功且没有 Return 的情况下，才能认为生产端投递链路基本正常。消费者是否处理成功，还需要继续依赖 Consumer Ack、消费日志、幂等记录和业务状态判断。


## 消费确认机制

本章节用于说明 RabbitMQ 消费端的消息确认机制。消费确认决定消息什么时候从队列中删除、消费失败后是否重新入队、异常消息是否进入死信队列。核心业务不建议使用“收到即确认”的方式，而应在业务处理成功后再确认消息。

### 自动确认模式

自动确认模式通常对应 Spring AMQP 的 `AUTO` 模式。该模式下，监听方法正常执行完成后，容器会自动确认消息；如果监听方法抛出异常，容器会按照异常处理、重试、重新入队或拒绝策略处理消息。Spring AMQP 文档说明，`AUTO` 是容器自动确认模式，监听器抛出异常时不会按成功消息确认。([Home](https://docs.spring.io/spring-amqp/reference/amqp/containerAttributes.html?utm_source=chatgpt.com))

配置示例：

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        # 自动确认模式，由监听容器根据方法执行结果处理 ack
        acknowledge-mode: auto
        # 消费异常是否默认重新入队，核心业务建议结合死信队列设置为 false
        default-requeue-rejected: false
```

自动确认消费者示例：

文件位置：`src/main/java/io/github/atengk/rabbitmq/consumer/AutoAckMessageConsumer.java`

```java
package io.github.atengk.rabbitmq.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 自动确认消息消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class AutoAckMessageConsumer {

    /**
     * 自动确认消费消息
     *
     * @param payload 消息体
     */
    @RabbitListener(queues = "ack.auto.queue")
    public void consume(String payload) {
        log.info("自动确认模式收到消息，payload={}", payload);

        // 方法正常结束后，由监听容器自动确认
        handleBusiness(payload);
    }

    /**
     * 处理业务逻辑
     *
     * @param payload 消息体
     */
    private void handleBusiness(String payload) {
        log.info("自动确认模式执行业务处理，payload={}", payload);
    }
}
```

自动确认模式适合非核心、允许框架托管确认逻辑的场景，例如普通日志、低价值通知、测试消息等。但对于订单、支付、库存等核心业务，建议使用手动确认模式，避免业务流程没有完全成功时消息被提前确认。

### 手动确认模式

手动确认模式对应 `MANUAL`。该模式下，监听方法必须显式调用 `basicAck`、`basicNack` 或 `basicReject`。Spring AMQP 文档说明，`MANUAL` 模式要求监听器通过 `Channel.basicAck()` 等方式自行确认消息。([Home](https://docs.spring.io/spring-amqp/reference/amqp/containerAttributes.html?utm_source=chatgpt.com))

配置示例：

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        # 手动确认模式，核心业务推荐使用
        acknowledge-mode: manual
        # 每个消费者最多持有的未确认消息数
        prefetch: 10
        # 异常后不默认重新入队，避免无限失败循环
        default-requeue-rejected: false
```

手动确认消费者示例：

文件位置：`src/main/java/io/github/atengk/rabbitmq/consumer/ManualAckMessageConsumer.java`

```java
package io.github.atengk.rabbitmq.consumer;

import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 手动确认消息消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class ManualAckMessageConsumer {

    /**
     * 手动确认消费消息
     *
     * @param payload 消息体
     * @param message 原始消息
     * @param channel RabbitMQ Channel
     * @throws IOException Ack 或 Nack 失败时抛出
     */
    @RabbitListener(queues = "ack.manual.queue")
    public void consume(String payload, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();

        try {
            log.info("手动确认模式收到消息，messageId={}，deliveryTag={}，payload={}",
                    StrUtil.blankToDefault(messageId, "unknown"),
                    deliveryTag,
                    payload
            );

            handleBusiness(payload);

            channel.basicAck(deliveryTag, false);
            log.info("消息手动确认成功，messageId={}，deliveryTag={}", messageId, deliveryTag);
        } catch (Exception ex) {
            log.error("消息消费失败，准备拒绝消息，messageId={}，deliveryTag={}，原因={}",
                    messageId,
                    deliveryTag,
                    ex.getMessage(),
                    ex
            );

            // requeue=false：不重新入队，若队列配置了死信交换机，则进入死信队列
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 处理业务逻辑
     *
     * @param payload 消息体
     */
    private void handleBusiness(String payload) {
        if (StrUtil.isBlank(payload)) {
            throw new IllegalArgumentException("消息体不能为空");
        }
        log.info("手动确认模式执行业务处理，payload={}", payload);
    }
}
```

手动确认模式适合核心业务，因为它可以明确控制消息生命周期：业务成功后 Ack，业务失败后 Nack 或 Reject，参数异常或不可恢复异常进入死信队列。

### NONE 模式

`NONE` 模式表示不发送消费者确认。RabbitMQ 会认为消息一旦投递给消费者就已经被确认，这在 RabbitMQ 原生语义中接近 `autoAck=true`。Spring AMQP 文档说明，`NONE` 模式不会发送 ack，Broker 会假定消息发送给消费者后已经被确认。([Home](https://docs.spring.io/spring-amqp/reference/amqp/containerAttributes.html?utm_source=chatgpt.com))

配置示例：

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        # NONE 模式不等待消费者确认，不适合核心业务
        acknowledge-mode: none
```

NONE 模式消费者示例：

```java
@RabbitListener(queues = "ack.none.queue")
public void consume(String payload) {
    log.info("NONE模式收到消息，payload={}", payload);

    // 如果这里抛出异常，消息也可能已经被 Broker 视为确认
    handleBusiness(payload);
}
```

NONE 模式风险较高，通常只适合可以接受丢失的低价值消息，例如非关键日志、临时监控数据、测试消息等。订单、支付、库存、优惠券、积分等业务不应使用该模式。

### Ack 处理

Ack 表示消费者确认消息处理成功。调用 `basicAck` 后，RabbitMQ 会将该消息从队列中删除。

Ack 常见方法：

```java
channel.basicAck(deliveryTag, false);
```

参数说明：

| 参数          | 说明                                                         |
| ------------- | ------------------------------------------------------------ |
| `deliveryTag` | RabbitMQ 对当前 Channel 投递消息的唯一标识                   |
| `multiple`    | 是否批量确认。`false` 表示只确认当前消息，`true` 表示确认当前 deliveryTag 及之前所有未确认消息 |

Ack 处理示例：

```java
try {
    handleBusiness(payload);

    // 只确认当前消息
    channel.basicAck(deliveryTag, false);
    log.info("消息Ack成功，messageId={}，deliveryTag={}", messageId, deliveryTag);
} catch (Exception ex) {
    log.error("消息处理失败，messageId={}，原因={}", messageId, ex.getMessage(), ex);
    channel.basicNack(deliveryTag, false, false);
}
```

Ack 使用建议：

1. 业务逻辑成功执行后再 Ack。
2. 幂等判断发现消息已处理时，也应 Ack，避免重复投递。
3. 普通单条消费使用 `multiple=false`。
4. 批量消费才考虑 `multiple=true`。
5. 不要在业务处理前提前 Ack。
6. 不要在 catch 中无条件 Ack，否则失败消息会被误删。

### Nack 处理

Nack 表示消费者拒绝消息。与 Reject 相比，Nack 支持批量拒绝，并且可以指定是否重新入队。

Nack 常见方法：

```java
channel.basicNack(deliveryTag, false, false);
```

参数说明：

| 参数          | 说明             |
| ------------- | ---------------- |
| `deliveryTag` | 当前消息投递标签 |
| `multiple`    | 是否批量拒绝     |
| `requeue`     | 是否重新入队     |

`requeue` 行为说明：

| requeue | 结果                                                         |
| ------- | ------------------------------------------------------------ |
| `true`  | 消息重新进入队列，后续可能再次投递                           |
| `false` | 消息不重新入队；如果配置死信交换机，则进入死信队列，否则可能被丢弃 |

Nack 示例：

```java
catch (IllegalArgumentException ex) {
    log.error("消息参数异常，不重新入队，messageId={}，原因={}", messageId, ex.getMessage(), ex);

    // 参数错误通常重试也无法恢复，进入死信
    channel.basicNack(deliveryTag, false, false);
} catch (Exception ex) {
    log.error("消息消费异常，不重新入队，messageId={}，原因={}", messageId, ex.getMessage(), ex);

    // 核心业务建议通过 Spring Retry 或死信队列处理，不建议无限 requeue=true
    channel.basicNack(deliveryTag, false, false);
}
```

Nack 使用建议：

1. 参数错误、格式错误、业务不可恢复异常使用 `requeue=false`。
2. 网络抖动、数据库短暂异常可以结合重试机制处理。
3. 不建议直接对所有异常使用 `requeue=true`。
4. 如果使用 `requeue=true`，必须有最大重试次数控制。
5. 配置死信队列后，失败消息建议 `requeue=false` 进入死信。

### Reject 处理

Reject 表示拒绝单条消息。它与 Nack 类似，但不支持批量拒绝。Reject 常用于明确拒绝当前单条异常消息。

Reject 常见方法：

```java
channel.basicReject(deliveryTag, false);
```

参数说明：

| 参数          | 说明             |
| ------------- | ---------------- |
| `deliveryTag` | 当前消息投递标签 |
| `requeue`     | 是否重新入队     |

Reject 示例：

```java
if (StrUtil.isBlank(payload)) {
    log.error("消息体为空，拒绝消息并进入死信，deliveryTag={}", deliveryTag);
    channel.basicReject(deliveryTag, false);
    return;
}
```

Reject 与 Nack 对比：

| 对比项           | Nack               | Reject           |
| ---------------- | ------------------ | ---------------- |
| 是否支持批量     | 支持               | 不支持           |
| 是否支持重新入队 | 支持               | 支持             |
| 常见用途         | 批量或单条失败处理 | 单条异常消息拒绝 |
| 推荐使用         | 更通用             | 简单拒绝场景     |

实际项目中，`basicNack(deliveryTag, false, false)` 使用更普遍，因为它语义完整并且支持批量扩展。

### 消息重新入队

消息重新入队表示消费失败后让消息重新回到队列，等待后续再次投递。它适合短暂异常场景，但必须谨慎使用。

重新入队示例：

```java
channel.basicNack(deliveryTag, false, true);
```

适合重新入队的场景：

| 场景               | 说明         |
| ------------------ | ------------ |
| 数据库短暂不可用   | 稍后可能恢复 |
| Redis 短暂不可用   | 稍后可能恢复 |
| 第三方服务短暂超时 | 后续可能成功 |
| 网络抖动           | 重试可能恢复 |

不适合重新入队的场景：

| 场景           | 原因                   |
| -------------- | ---------------------- |
| JSON 格式错误  | 重试仍然无法解析       |
| 必填字段缺失   | 重试无法恢复           |
| 业务状态非法   | 需要人工或业务补偿     |
| 消费者代码 bug | 重新入队会造成反复失败 |
| 下游长期不可用 | 会造成队列持续堆积     |

重新入队风险：

1. 可能造成消息被无限重复消费。
2. 可能导致队列中异常消息阻塞正常消息。
3. 可能造成消费者日志大量刷屏。
4. 可能加重下游服务压力。
5. 并发消费时可能导致失败消息被快速重复投递。

项目建议优先使用“有限重试 + 死信队列”的方案，而不是直接无限重新入队。Spring Boot 文档也说明，监听器重试默认关闭；当启用重试且重试耗尽时，默认会拒绝消息，消息会被丢弃或在 Broker 配置死信交换机时进入死信交换机。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/messaging/amqp.html?utm_source=chatgpt.com))

### 消息丢弃策略

消息丢弃表示消费失败后既不重新入队，也没有进入可处理的死信流程。核心业务中不建议直接丢弃消息，除非业务明确允许忽略。

消息可能被丢弃的常见情况：

| 场景                                            | 说明                      |
| ----------------------------------------------- | ------------------------- |
| `basicNack(requeue=false)` 且未配置死信交换机   | 消息被 Broker 丢弃        |
| `basicReject(requeue=false)` 且未配置死信交换机 | 消息被 Broker 丢弃        |
| NONE 模式下消费者处理失败                       | Broker 可能已认为消息确认 |
| 消费端异常被吞掉后 Ack                          | 业务失败但消息被删除      |
| 重试耗尽后无死信配置                            | 消息可能被丢弃            |

推荐丢弃策略如下：

| 消息类型       | 处理策略                 |
| -------------- | ------------------------ |
| 核心业务消息   | 不直接丢弃，进入死信队列 |
| 可恢复异常消息 | 重试后仍失败进入死信     |
| 参数错误消息   | 进入死信并记录原因       |
| 重复消息       | 幂等命中后 Ack           |
| 低价值日志消息 | 可按业务允许丢弃         |
| 测试消息       | 可直接丢弃               |

丢弃前建议记录完整日志：

```java
log.error("消息即将被拒绝且不重新入队，messageId={}，businessKey={}，queue={}，reason={}",
        messageId,
        businessKey,
        message.getMessageProperties().getConsumerQueue(),
        "参数校验失败"
);
channel.basicNack(deliveryTag, false, false);
```

核心原则是：只要消息涉及资金、订单、库存、权益、积分、审计，就不应无记录丢弃，至少应进入死信队列或消息记录表。

## 消费重试机制

本章节用于说明 RabbitMQ 消费失败后的重试设计。消费重试用于处理临时异常，例如数据库短暂不可用、第三方接口超时、网络抖动等。重试不是兜底方案，重试耗尽后仍然需要死信队列、失败记录和人工补偿机制。

### Spring Retry 配置

Spring Boot 可以通过 `spring.rabbitmq.listener.simple.retry.*` 为 RabbitMQ Listener 开启消费重试。Spring Boot 3.4 文档中，`spring.rabbitmq.listener.simple.retry.enabled` 默认关闭，`max-attempts` 默认最大尝试次数为 3，`initial-interval` 默认 1000ms，`max-interval` 默认 10000ms，`multiplier` 默认 1。([Home](https://docs.spring.io/spring-boot/3.4/appendix/application-properties/?utm_source=chatgpt.com))

配置示例：

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        # 手动确认模式，核心业务推荐
        acknowledge-mode: manual
        # 消费失败不默认重新入队，配合重试和死信队列使用
        default-requeue-rejected: false
        retry:
          # 开启消费者重试
          enabled: true
          # 最大尝试次数，包含首次消费
          max-attempts: 3
          # 第一次和第二次尝试之间的间隔
          initial-interval: 1s
          # 最大重试间隔
          max-interval: 10s
          # 重试间隔倍率，1 表示固定间隔
          multiplier: 2
          # 默认无状态重试
          stateless: true
```

版本注意：如果项目使用的 Spring Boot 版本文档中属性名为 `max-retries`，则按当前版本的 `Common Application Properties` 调整配置。Spring Boot 当前通用属性文档已经列出 `spring.rabbitmq.listener.simple.retry.max-retries` 这一命名，实际项目应以所用 Spring Boot 版本为准。([Home](https://docs.spring.io/spring-boot/appendix/application-properties/?utm_source=chatgpt.com))

重试生效流程：

```text
消费者收到消息
    -> Listener 执行业务
    -> 抛出异常
    -> Spring Retry 按配置进行重试
    -> 重试成功，消息确认
    -> 重试耗尽，MessageRecoverer 处理
    -> 默认拒绝消息
    -> 如果队列配置死信交换机，消息进入死信队列
```

Spring Boot 文档说明，启用监听器重试后，重试耗尽时默认使用 `RejectAndDontRequeueRecoverer`，消息会被拒绝；如果 Broker 配置了死信交换机，则会进入死信交换机。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/messaging/amqp.html?utm_source=chatgpt.com))

### 最大重试次数

最大重试次数用于限制消费失败后的尝试次数。它通常包含首次消费。例如 `max-attempts=3` 表示最多执行 3 次：首次消费 1 次，加上后续重试 2 次。

推荐配置：

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        retry:
          enabled: true
          # 最多尝试 3 次：1 次首次消费 + 2 次重试
          max-attempts: 3
```

最大重试次数建议：

| 场景           | 推荐次数        | 说明                   |
| -------------- | --------------- | ---------------------- |
| 参数错误       | 不重试          | 重试无法恢复           |
| 数据库短暂异常 | 3 到 5 次       | 可能短时间恢复         |
| 第三方接口超时 | 3 次            | 避免长时间阻塞消费线程 |
| 网络抖动       | 3 到 5 次       | 可短暂重试             |
| 业务状态非法   | 不重试或低次数  | 需要业务补偿           |
| 核心资金消息   | 3 到 5 次后死信 | 死信后人工处理         |

不建议将最大重试次数设置过大。消费线程在重试期间会被占用，如果大量消息同时失败，可能导致消费者吞吐量下降、队列堆积和下游服务压力增大。

### 重试间隔配置

重试间隔用于控制两次消费尝试之间的等待时间。固定间隔适合轻量级、短暂异常场景；对于下游服务不稳定或高峰压力场景，建议使用指数退避。

固定重试间隔配置：

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        retry:
          enabled: true
          max-attempts: 3
          # 固定 2 秒间隔
          initial-interval: 2s
          max-interval: 2s
          multiplier: 1
```

固定间隔说明：

| 参数               | 说明                    |
| ------------------ | ----------------------- |
| `initial-interval` | 初始重试间隔            |
| `max-interval`     | 最大重试间隔            |
| `multiplier`       | 倍率为 1 时表示固定间隔 |

适用场景：

1. 短暂网络抖动。
2. 单次业务处理很快。
3. 下游系统可以承受短时间重复请求。
4. 消息量不大，重试不会明显影响吞吐量。

### 指数退避配置

指数退避表示重试间隔逐步增大。例如初始间隔 1 秒，倍率 2，最大间隔 10 秒，则重试间隔可能是 1 秒、2 秒、4 秒，直到不超过最大间隔。

指数退避配置：

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        retry:
          enabled: true
          max-attempts: 5
          # 初始间隔 1 秒
          initial-interval: 1s
          # 最大间隔 10 秒
          max-interval: 10s
          # 每次间隔按 2 倍增长
          multiplier: 2
```

指数退避适合以下场景：

| 场景               | 说明                     |
| ------------------ | ------------------------ |
| 第三方接口不稳定   | 避免短时间连续冲击第三方 |
| 数据库短暂压力过高 | 给数据库恢复时间         |
| Redis 短暂抖动     | 降低瞬时重试压力         |
| 下游限流           | 避免持续触发限流         |

指数退避建议：

1. `initial-interval` 不宜过大，否则简单抖动恢复慢。
2. `max-interval` 不宜过大，否则消费线程长时间被占用。
3. 核心业务建议重试耗尽后进入死信队列。
4. 对长时间延迟重试，建议使用延迟队列或死信延迟方案，而不是占用消费线程等待。

### 重试异常分类

并不是所有异常都适合重试。重试只对临时性、可恢复异常有意义。参数错误、格式错误、业务状态不可逆等异常，重试通常没有价值。

异常分类建议：

| 异常类型     | 示例                     | 是否重试   | 处理方式             |
| ------------ | ------------------------ | ---------- | -------------------- |
| 参数异常     | 消息缺少订单 ID          | 否         | 进入死信             |
| 反序列化异常 | JSON 格式错误            | 否         | 进入死信             |
| 业务状态异常 | 订单已关闭但收到支付成功 | 视业务而定 | 状态机判断或人工处理 |
| 数据库异常   | 连接池暂时耗尽           | 是         | 重试后失败进入死信   |
| Redis 异常   | Redis 短暂不可用         | 是         | 重试后失败进入死信   |
| 第三方超时   | HTTP 调用超时            | 是         | 重试后失败进入死信   |
| 幂等命中     | 消息已处理               | 否         | 直接 Ack             |
| 代码 Bug     | 空指针、类型错误         | 否         | 进入死信并告警       |

可以通过业务代码主动区分异常类型。不可重试异常直接抛出 `AmqpRejectAndDontRequeueException`，表达该消息不应重新入队。Spring Boot 文档也说明，在未启用重试时，可以通过设置 `defaultRequeueRejected=false` 或抛出 `AmqpRejectAndDontRequeueException` 来避免异常消息无限重新投递。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/messaging/amqp.html?utm_source=chatgpt.com))

异常分类示例：

文件位置：`src/main/java/io/github/atengk/rabbitmq/consumer/RetryClassifyMessageConsumer.java`

```java
package io.github.atengk.rabbitmq.consumer;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 重试异常分类消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class RetryClassifyMessageConsumer {

    /**
     * 消费并分类异常
     *
     * @param payload 消息体
     */
    @RabbitListener(queues = "retry.classify.queue")
    public void consume(String payload) {
        log.info("收到需要分类处理的消息，payload={}", payload);

        if (StrUtil.isBlank(payload)) {
            log.error("消息体为空，不进行重试");
            throw new AmqpRejectAndDontRequeueException("消息体为空，不重试");
        }

        try {
            handleBusiness(payload);
        } catch (IllegalArgumentException ex) {
            log.error("消息参数异常，不进行重试，payload={}，原因={}", payload, ex.getMessage(), ex);
            throw new AmqpRejectAndDontRequeueException("参数异常，不重试", ex);
        } catch (Exception ex) {
            log.error("消息处理出现可重试异常，payload={}，原因={}", payload, ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * 处理业务逻辑
     *
     * @param payload 消息体
     */
    private void handleBusiness(String payload) {
        log.info("执行可重试业务逻辑，payload={}", payload);
    }
}
```

这种方式可以避免明显不可恢复的异常参与无意义重试，减少队列积压和日志噪音。

### 重试失败处理

重试失败处理用于定义消息在达到最大重试次数后如何处理。核心业务建议重试耗尽后进入死信队列，并记录失败原因，后续通过死信消费者或管理端进行排查、重发、忽略或人工补偿。

推荐处理流程：

```text
消费者处理失败
    -> Spring Retry 按配置重试
    -> 重试成功，消息正常 Ack
    -> 重试耗尽
    -> MessageRecoverer 处理
    -> 拒绝消息且不重新入队
    -> Broker 将消息投递到死信交换机
    -> 死信消费者记录异常消息
    -> 人工或任务补偿
```

可以自定义 `MessageRecoverer`，在重试耗尽时记录详细日志。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/RabbitMqRetryRecoverConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 重试失败恢复配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Configuration
public class RabbitMqRetryRecoverConfig {

    /**
     * 配置重试耗尽后的恢复处理器
     *
     * @return MessageRecoverer
     */
    @Bean
    public MessageRecoverer messageRecoverer() {
        return (Message message, Throwable cause) -> {
            String messageId = message.getMessageProperties().getMessageId();
            String queue = message.getMessageProperties().getConsumerQueue();
            Object businessKey = message.getMessageProperties().getHeaders().get("x-business-key");

            log.error("MQ消息重试耗尽，messageId={}，businessKey={}，queue={}，原因={}",
                    StrUtil.blankToDefault(messageId, "unknown"),
                    businessKey,
                    queue,
                    cause.getMessage(),
                    cause
            );

            // 抛出异常后结合 default-requeue-rejected=false，让消息进入死信或被拒绝
            throw new AmqpRejectAfterRetryException("MQ消息重试耗尽，拒绝重新入队", cause);
        };
    }

    /**
     * 重试耗尽后拒绝重新入队异常
     *
     * @author Ateng
     * @since 2026-05-11
     */
    public static class AmqpRejectAfterRetryException extends RuntimeException {

        /**
         * 创建异常
         *
         * @param message 异常消息
         * @param cause   原始异常
         */
        public AmqpRejectAfterRetryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
```

更常见的做法是直接使用默认恢复逻辑，让消息重试耗尽后被拒绝并进入死信队列。自定义 `MessageRecoverer` 主要用于补充日志、落库或告警。

重试失败后的死信队列配置示例：

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 重试失败死信配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class RetryDeadLetterConfig {

    public static final String BUSINESS_EXCHANGE = "retry.business.exchange";
    public static final String BUSINESS_QUEUE = "retry.business.queue";
    public static final String BUSINESS_ROUTING_KEY = "retry.business";

    public static final String DEAD_EXCHANGE = "retry.dead.exchange";
    public static final String DEAD_QUEUE = "retry.dead.queue";
    public static final String DEAD_ROUTING_KEY = "retry.dead";

    /**
     * 声明业务交换机
     *
     * @return Direct Exchange
     */
    @Bean
    public DirectExchange retryBusinessExchange() {
        return ExchangeBuilder.directExchange(BUSINESS_EXCHANGE).durable(true).build();
    }

    /**
     * 声明业务队列，绑定死信交换机
     *
     * @return Queue
     */
    @Bean
    public Queue retryBusinessQueue() {
        return QueueBuilder
                .durable(BUSINESS_QUEUE)
                .deadLetterExchange(DEAD_EXCHANGE)
                .deadLetterRoutingKey(DEAD_ROUTING_KEY)
                .build();
    }

    /**
     * 绑定业务队列
     *
     * @return Binding
     */
    @Bean
    public Binding retryBusinessBinding() {
        return BindingBuilder
                .bind(retryBusinessQueue())
                .to(retryBusinessExchange())
                .with(BUSINESS_ROUTING_KEY);
    }

    /**
     * 声明死信交换机
     *
     * @return Direct Exchange
     */
    @Bean
    public DirectExchange retryDeadExchange() {
        return ExchangeBuilder.directExchange(DEAD_EXCHANGE).durable(true).build();
    }

    /**
     * 声明死信队列
     *
     * @return Queue
     */
    @Bean
    public Queue retryDeadQueue() {
        return QueueBuilder.durable(DEAD_QUEUE).build();
    }

    /**
     * 绑定死信队列
     *
     * @return Binding
     */
    @Bean
    public Binding retryDeadBinding() {
        return BindingBuilder
                .bind(retryDeadQueue())
                .to(retryDeadExchange())
                .with(DEAD_ROUTING_KEY);
    }
}
```

重试失败处理建议：

1. 重试耗尽后不要无限重新入队。
2. 核心业务必须配置死信队列。
3. 死信消息应记录 `messageId`、业务主键、异常原因、原队列、重试次数。
4. 可恢复问题修复后再重发死信消息。
5. 不可恢复消息应支持标记忽略或人工处理。
6. 重试次数、重试间隔和死信处理策略应按业务重要性区分配置。

消费确认与重试的推荐组合如下：

| 业务类型         | 确认模式           | 重试策略 | 失败处理           |
| ---------------- | ------------------ | -------- | ------------------ |
| 订单、支付、库存 | `manual`           | 有限重试 | 进入死信，人工补偿 |
| 通知、短信、邮件 | `manual` 或 `auto` | 有限重试 | 死信或记录失败     |
| 日志采集         | `auto`             | 少量重试 | 可丢弃或降级       |
| 临时测试消息     | `auto` 或 `none`   | 不重试   | 可丢弃             |
| 审计消息         | `manual`           | 有限重试 | 死信并告警         |


## 死信队列设计

本章节用于说明 RabbitMQ 死信队列的设计方式。死信队列不是异常消息的“垃圾桶”，而是可靠性设计中的兜底机制，用于保存消费失败、消息过期、队列超长等异常消息，便于后续排查、重发、忽略或人工补偿。

### 死信队列概念

死信消息是指无法在原业务队列中继续正常流转的消息。RabbitMQ 可以将这些消息重新发布到指定的 Dead Letter Exchange，再由 Dead Letter Exchange 根据 Routing Key 路由到死信队列。

死信队列的核心作用如下：

| 作用     | 说明                                             |
| -------- | ------------------------------------------------ |
| 异常隔离 | 将失败消息从业务队列中隔离出来，避免阻塞正常消息 |
| 问题排查 | 保存异常消息内容、消息头、失败原因和来源队列     |
| 业务补偿 | 支持人工或定时任务重新投递消息                   |
| 延迟处理 | 配合 TTL 可以实现简单延迟消息                    |
| 监控告警 | 死信数量异常增长时触发告警                       |

典型死信流程如下：

```text
Producer
  -> Business Exchange
  -> Business Queue
  -> Consumer
       |
       | 消费失败 / 消息过期 / 队列超长
       v
  -> Dead Letter Exchange
  -> Dead Letter Queue
  -> Dead Letter Consumer
```

业务队列只负责正常业务消费，死信队列负责异常消息沉淀。核心业务消息不建议消费失败后直接丢弃，应优先进入死信队列。

### 死信触发条件

RabbitMQ 中消息进入死信交换机的常见触发条件包括：消费者使用 `basic.reject` 或 `basic.nack` 且 `requeue=false`；消息 TTL 过期；队列超过最大长度；Quorum Queue 中消息超过投递次数限制。RabbitMQ 官方文档也说明，如果整个队列过期，队列中的消息不会被死信化。([rabbitmq.com](https://www.rabbitmq.com/docs/next/dlx?utm_source=chatgpt.com))

常见触发条件如下：

| 触发条件              | 说明                                   |
| --------------------- | -------------------------------------- |
| 消费者拒绝消息        | `basicReject(deliveryTag, false)`      |
| 消费者 Nack 消息      | `basicNack(deliveryTag, false, false)` |
| 消息 TTL 过期         | 消息在队列中超过 TTL                   |
| 队列长度超限          | 队列达到 `x-max-length` 后旧消息被挤出 |
| 队列容量超限          | 队列达到 `x-max-length-bytes`          |
| Quorum Queue 投递超限 | 超过 `delivery-limit` 后进入死信       |

消费者拒绝消息示例：

```java
channel.basicNack(deliveryTag, false, false);
```

参数说明：

| 参数             | 说明                                   |
| ---------------- | -------------------------------------- |
| `deliveryTag`    | 当前消息投递标签                       |
| `multiple=false` | 只处理当前消息                         |
| `requeue=false`  | 不重新入队，若配置死信交换机则进入死信 |

死信触发建议：

1. 参数错误、格式错误、不可恢复业务异常，建议 `requeue=false` 进入死信。
2. 临时异常可以先重试，重试耗尽后进入死信。
3. 不建议对所有异常使用 `requeue=true`，否则可能造成无限失败循环。
4. 核心业务队列必须配置死信交换机。
5. 死信队列必须有消费、告警或管理端处理流程，否则只会把异常消息换个队列堆积。

### 死信 Exchange

死信 Exchange 是普通 Exchange，没有特殊类型限制，可以是 Direct、Topic、Fanout 等。实际项目中通常使用 Direct Exchange，便于按业务队列精确路由死信消息。

推荐命名方式：

```text
业务域.dead.exchange
```

示例：

```text
order.dead.exchange
payment.dead.exchange
notice.dead.exchange
```

死信 Exchange 配置示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/DeadLetterConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 死信交换机配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class DeadLetterConfig {

    public static final String ORDER_DEAD_EXCHANGE = "order.dead.exchange";

    /**
     * 声明订单死信交换机
     *
     * @return Direct 类型死信交换机
     */
    @Bean
    public DirectExchange orderDeadExchange() {
        return ExchangeBuilder
                .directExchange(ORDER_DEAD_EXCHANGE)
                .durable(true)
                .build();
    }
}
```

死信 Exchange 设计建议：

| 建议                   | 说明                             |
| ---------------------- | -------------------------------- |
| 使用持久化 Exchange    | Broker 重启后交换机仍然存在      |
| 按业务域拆分           | 订单、支付、库存等独立死信交换机 |
| 优先 Direct 类型       | 死信路由更明确                   |
| 不与业务 Exchange 混用 | 避免正常消息和异常消息混杂       |
| 命名包含 `dead`        | 便于控制台和日志识别             |

### 死信 Queue

死信 Queue 用于存储死信消息。核心业务建议每个重要业务队列配置对应的死信队列，避免多个业务的异常消息混在一起难以排查。

推荐命名方式：

```text
业务域.业务动作.dead.queue
```

示例：

```text
order.paid.dead.queue
order.created.dead.queue
payment.success.dead.queue
```

完整业务队列和死信队列配置如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/OrderDeadLetterQueueConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 订单死信队列配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class OrderDeadLetterQueueConfig {

    public static final String ORDER_BUSINESS_EXCHANGE = "order.business.exchange";

    public static final String ORDER_PAID_QUEUE = "order.paid.queue";

    public static final String ORDER_PAID_ROUTING_KEY = "order.paid";

    public static final String ORDER_DEAD_EXCHANGE = "order.dead.exchange";

    public static final String ORDER_PAID_DEAD_QUEUE = "order.paid.dead.queue";

    public static final String ORDER_PAID_DEAD_ROUTING_KEY = "order.paid.dead";

    /**
     * 声明订单业务交换机
     *
     * @return 订单业务交换机
     */
    @Bean
    public DirectExchange orderBusinessExchange() {
        return ExchangeBuilder
                .directExchange(ORDER_BUSINESS_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 声明订单支付业务队列，并绑定死信交换机
     *
     * @return 订单支付业务队列
     */
    @Bean
    public Queue orderPaidQueue() {
        return QueueBuilder
                .durable(ORDER_PAID_QUEUE)
                // 消息被拒绝、过期或队列超长后进入该死信交换机
                .deadLetterExchange(ORDER_DEAD_EXCHANGE)
                // 死信消息使用该路由键进入死信队列
                .deadLetterRoutingKey(ORDER_PAID_DEAD_ROUTING_KEY)
                .build();
    }

    /**
     * 绑定订单业务交换机和订单支付业务队列
     *
     * @return 业务绑定关系
     */
    @Bean
    public Binding orderPaidBinding() {
        return BindingBuilder
                .bind(orderPaidQueue())
                .to(orderBusinessExchange())
                .with(ORDER_PAID_ROUTING_KEY);
    }

    /**
     * 声明订单死信交换机
     *
     * @return 订单死信交换机
     */
    @Bean
    public DirectExchange orderDeadExchange() {
        return ExchangeBuilder
                .directExchange(ORDER_DEAD_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 声明订单支付死信队列
     *
     * @return 订单支付死信队列
     */
    @Bean
    public Queue orderPaidDeadQueue() {
        return QueueBuilder
                .durable(ORDER_PAID_DEAD_QUEUE)
                .build();
    }

    /**
     * 绑定订单死信交换机和订单支付死信队列
     *
     * @return 死信绑定关系
     */
    @Bean
    public Binding orderPaidDeadBinding() {
        return BindingBuilder
                .bind(orderPaidDeadQueue())
                .to(orderDeadExchange())
                .with(ORDER_PAID_DEAD_ROUTING_KEY);
    }
}
```

队列参数说明：

| 参数                        | 说明                               |
| --------------------------- | ---------------------------------- |
| `x-dead-letter-exchange`    | 死信交换机                         |
| `x-dead-letter-routing-key` | 死信路由键                         |
| `x-message-ttl`             | 消息 TTL，过期后可进入死信         |
| `x-max-length`              | 队列最大消息数量，超出后可触发死信 |
| `x-max-length-bytes`        | 队列最大字节数                     |

死信 Queue 设计建议：

1. 核心业务队列和死信队列都应持久化。
2. 死信队列不建议设置过短 TTL，避免异常消息尚未排查就被删除。
3. 死信队列应接入监控，关注消息数量和增长速度。
4. 死信队列最好按业务拆分，避免所有异常消息混入一个队列。
5. 死信队列应支持管理端查询、重发、忽略和标记处理。

### 死信 Binding

死信 Binding 用于将 Dead Letter Exchange 和 Dead Letter Queue 绑定起来。业务队列中的消息成为死信后，会被重新发布到 Dead Letter Exchange，并通过死信 Routing Key 路由到死信队列。

死信路由流程如下：

```text
Business Queue
  -- x-dead-letter-exchange=order.dead.exchange
  -- x-dead-letter-routing-key=order.paid.dead
        |
        v
order.dead.exchange
        |
        | routingKey=order.paid.dead
        v
order.paid.dead.queue
```

死信 Binding 示例：

```java
@Bean
public Binding orderPaidDeadBinding() {
    return BindingBuilder
            .bind(orderPaidDeadQueue())
            .to(orderDeadExchange())
            .with(ORDER_PAID_DEAD_ROUTING_KEY);
}
```

如果业务队列没有设置 `x-dead-letter-routing-key`，RabbitMQ 会尝试使用原始消息的 Routing Key 路由死信消息；如果设置了 `x-dead-letter-routing-key`，则使用配置的死信 Routing Key。RabbitMQ 官方文档说明，死信消息会使用队列指定的 dead-letter routing key；如果未设置，则使用原始发布时的 routing key。([rabbitmq.com](https://www.rabbitmq.com/docs/next/dlx?utm_source=chatgpt.com))

死信 Binding 设计建议：

| 场景         | 建议                                              |
| ------------ | ------------------------------------------------- |
| 单一业务队列 | 配置独立死信 Routing Key                          |
| 多业务队列   | 每个队列配置不同死信 Routing Key                  |
| 统一死信队列 | 可多个 Routing Key 绑定到同一队列，但排查成本更高 |
| 重要业务     | 独立死信队列，独立告警                            |
| 延迟队列     | TTL 过期后通过死信 Binding 进入业务队列           |

### 死信消息消费

死信消息消费用于处理进入死信队列的异常消息。死信消费者不应直接无脑重发消息，而应先记录死信原因、来源队列、原始消息、业务主键、异常类型，再根据业务策略决定是否重发或人工处理。

RabbitMQ 会在死信消息 Header 中记录死信历史，例如 `x-death`、`x-first-death-queue`、`x-first-death-reason`、`x-last-death-queue`、`x-last-death-reason` 等信息，用于判断消息第一次和最近一次进入死信的来源及原因。([rabbitmq.com](https://www.rabbitmq.com/docs/next/dlx?utm_source=chatgpt.com))

死信消费者示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/consumer/OrderDeadLetterConsumer.java`

```java
package io.github.atengk.rabbitmq.consumer;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.Channel;
import io.github.atengk.rabbitmq.config.OrderDeadLetterQueueConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 订单死信消息消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class OrderDeadLetterConsumer {

    /**
     * 消费订单支付死信消息
     *
     * @param message 原始消息
     * @param channel RabbitMQ Channel
     * @throws IOException Ack 或 Nack 失败时抛出
     */
    @RabbitListener(queues = OrderDeadLetterQueueConfig.ORDER_PAID_DEAD_QUEUE)
    public void consumeDeadLetter(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        Map<String, Object> headers = message.getMessageProperties().getHeaders();

        String messageId = StrUtil.blankToDefault(message.getMessageProperties().getMessageId(), "unknown");
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        String firstDeathQueue = String.valueOf(MapUtil.get(headers, "x-first-death-queue", "unknown"));
        String firstDeathReason = String.valueOf(MapUtil.get(headers, "x-first-death-reason", "unknown"));
        String lastDeathQueue = String.valueOf(MapUtil.get(headers, "x-last-death-queue", "unknown"));
        String lastDeathReason = String.valueOf(MapUtil.get(headers, "x-last-death-reason", "unknown"));

        try {
            log.error("收到订单死信消息，messageId={}，firstDeathQueue={}，firstDeathReason={}，lastDeathQueue={}，lastDeathReason={}，payload={}",
                    messageId,
                    firstDeathQueue,
                    firstDeathReason,
                    lastDeathQueue,
                    lastDeathReason,
                    payload
            );

            // 生产环境建议在这里落库，记录死信消息、Header、来源队列、死信原因和处理状态
            recordDeadLetter(messageId, payload, headers);

            channel.basicAck(deliveryTag, false);
            log.info("订单死信消息记录完成，messageId={}", messageId);
        } catch (Exception ex) {
            log.error("订单死信消息处理失败，messageId={}，原因={}", messageId, ex.getMessage(), ex);

            // 死信消费者失败时不建议无限重入队，避免死信队列阻塞
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 记录死信消息
     *
     * @param messageId 消息ID
     * @param payload   消息体
     * @param headers   消息头
     */
    private void recordDeadLetter(String messageId, String payload, Map<String, Object> headers) {
        log.info("记录死信消息，messageId={}，headers={}", messageId, headers);
    }
}
```

死信消费建议：

1. 死信消费者优先记录，不要直接删除异常消息。
2. 死信消费成功后 Ack，避免死信消息重复记录。
3. 死信处理失败不建议无限重新入队。
4. 死信消息应写入数据库，状态可设置为 `PENDING`、`RETRIED`、`IGNORED`、`DONE`。
5. 死信消息重发前必须判断失败原因是否已修复。
6. 死信消费者日志必须包含 `messageId`、原队列、死信原因和业务主键。

### 死信消息排查

死信消息排查用于定位消息为什么进入死信队列。排查时应结合 RabbitMQ 管理控制台、应用日志、消息 Header、死信记录表和业务状态进行分析。

排查步骤如下：

| 步骤             | 检查内容                                               |
| ---------------- | ------------------------------------------------------ |
| 查看死信队列深度 | 判断是否持续增长                                       |
| 查看死信 Header  | 重点查看 `x-first-death-reason`、`x-first-death-queue` |
| 查看消费者日志   | 查找对应 `messageId` 的异常堆栈                        |
| 查看业务状态     | 判断是否业务状态非法或重复消费                         |
| 查看重试配置     | 判断是否重试耗尽后进入死信                             |
| 查看队列参数     | 检查 TTL、最大长度、死信交换机配置                     |
| 查看路由绑定     | 检查死信 Exchange、Queue、Binding 是否正确             |

常用命令如下：

```bash
# 查看死信队列消息数量
rabbitmqctl list_queues -p /dev name messages_ready messages_unacknowledged consumers | grep dead

# 查看死信交换机
rabbitmqctl list_exchanges -p /dev name type durable | grep dead

# 查看死信绑定关系
rabbitmqctl list_bindings -p /dev | grep dead

# 获取一条死信消息，ack_requeue_true 表示查看后重新入队
rabbitmqadmin -V /dev get queue=order.paid.dead.queue ackmode=ack_requeue_true
```

命令说明：`list_queues` 用于查看死信队列是否堆积，`list_exchanges` 用于确认死信交换机是否存在，`list_bindings` 用于检查死信路由关系，`rabbitmqadmin get` 用于临时查看消息内容。生产环境查看死信消息时，不要随意使用 `ack_requeue_false`，避免误删消息。

死信原因对照：

| 死信原因         | 说明                      | 处理建议               |
| ---------------- | ------------------------- | ---------------------- |
| `rejected`       | 消费者拒绝消息            | 查看消费者异常日志     |
| `expired`        | 消息 TTL 过期             | 检查延迟设计或消费能力 |
| `maxlen`         | 队列长度超限              | 检查队列堆积和容量配置 |
| `delivery_limit` | Quorum Queue 投递次数超限 | 检查消费者失败原因     |

排查建议：

1. 先看 `x-first-death-reason`，判断第一次进入死信的原因。
2. 再看 `x-first-death-queue`，定位来源业务队列。
3. 根据 `messageId` 查询生产者日志、消费者日志和业务日志。
4. 如果大量消息因 `expired` 进入死信，检查消费者是否停止或延迟队列设计是否合理。
5. 如果大量消息因 `rejected` 进入死信，优先排查消费者业务异常。
6. 如果大量消息因 `maxlen` 进入死信，说明队列容量不足或消费能力不足。

## 延迟消息设计

本章节用于说明 RabbitMQ 延迟消息的常见实现方式。RabbitMQ 常见延迟方案包括 TTL 延迟、死信队列延迟和 `rabbitmq_delayed_message_exchange` 插件方案。不同方案在灵活性、可靠性、运维成本和适用场景上不同。

### TTL 延迟方案

TTL 表示 Time-To-Live，即消息或队列的生存时间。RabbitMQ 支持队列级消息 TTL 和单条消息 TTL。消息超过 TTL 后会过期；如果队列配置了死信交换机，过期消息可以进入死信交换机。RabbitMQ 官方文档说明，消息 TTL 的单位是毫秒，可以通过队列参数或发布消息属性设置。([rabbitmq.com](https://www.rabbitmq.com/docs/4.0/ttl?utm_source=chatgpt.com))

TTL 延迟的核心思路是：

```text
Producer
  -> Delay Queue
  -> 消息 TTL 到期
  -> Dead Letter Exchange
  -> Business Queue
  -> Consumer
```

TTL 方案分为两类：

| 方案     | 说明                           |
| -------- | ------------------------------ |
| 队列 TTL | 队列内所有消息使用相同过期时间 |
| 消息 TTL | 每条消息发送时设置不同过期时间 |

队列 TTL 适合固定延迟场景，例如所有消息都延迟 30 分钟处理。消息 TTL 适合每条消息延迟时间不同的场景，但经典队列中不同 TTL 消息可能受队头阻塞影响；RabbitMQ 官方文档也说明，设置单条消息 TTL 时，过期消息可能排在未过期消息后面，直到前面的消息被消费或过期才被处理。([rabbitmq.com](https://www.rabbitmq.com/docs/4.0/ttl?utm_source=chatgpt.com))

### 死信队列延迟方案

死信队列延迟方案是生产环境最常见的无插件延迟方案。它通过“延迟队列 + TTL + 死信交换机”实现延迟投递。

订单超时关闭示例：

| 资源类型             | 名称                      | 说明               |
| -------------------- | ------------------------- | ------------------ |
| Delay Exchange       | `delay.order.exchange`    | 延迟交换机         |
| Delay Queue          | `delay.order.close.queue` | 延迟队列，设置 TTL |
| Dead Letter Exchange | `order.business.exchange` | 业务交换机         |
| Business Queue       | `order.close.queue`       | 订单关闭业务队列   |
| Delay Routing Key    | `order.close.delay`       | 延迟消息路由键     |
| Business Routing Key | `order.close`             | 到期后业务路由键   |

完整配置如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/OrderDelayQueueConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 订单延迟队列配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class OrderDelayQueueConfig {

    public static final String DELAY_ORDER_EXCHANGE = "delay.order.exchange";

    public static final String DELAY_ORDER_CLOSE_QUEUE = "delay.order.close.queue";

    public static final String DELAY_ORDER_CLOSE_ROUTING_KEY = "order.close.delay";

    public static final String ORDER_BUSINESS_EXCHANGE = "order.business.exchange";

    public static final String ORDER_CLOSE_QUEUE = "order.close.queue";

    public static final String ORDER_CLOSE_ROUTING_KEY = "order.close";

    /**
     * 声明订单延迟交换机
     *
     * @return 延迟交换机
     */
    @Bean
    public DirectExchange delayOrderExchange() {
        return ExchangeBuilder
                .directExchange(DELAY_ORDER_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 声明订单关闭延迟队列
     *
     * @return 延迟队列
     */
    @Bean
    public Queue delayOrderCloseQueue() {
        return QueueBuilder
                .durable(DELAY_ORDER_CLOSE_QUEUE)
                // 队列级 TTL：消息在该队列中保留 30 分钟
                .ttl(30 * 60 * 1000)
                // TTL 到期后进入订单业务交换机
                .deadLetterExchange(ORDER_BUSINESS_EXCHANGE)
                // TTL 到期后使用订单关闭路由键
                .deadLetterRoutingKey(ORDER_CLOSE_ROUTING_KEY)
                .build();
    }

    /**
     * 绑定订单延迟交换机和订单关闭延迟队列
     *
     * @return 延迟绑定关系
     */
    @Bean
    public Binding delayOrderCloseBinding() {
        return BindingBuilder
                .bind(delayOrderCloseQueue())
                .to(delayOrderExchange())
                .with(DELAY_ORDER_CLOSE_ROUTING_KEY);
    }

    /**
     * 声明订单业务交换机
     *
     * @return 订单业务交换机
     */
    @Bean
    public DirectExchange orderBusinessExchange() {
        return ExchangeBuilder
                .directExchange(ORDER_BUSINESS_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 声明订单关闭业务队列
     *
     * @return 订单关闭业务队列
     */
    @Bean
    public Queue orderCloseQueue() {
        return QueueBuilder
                .durable(ORDER_CLOSE_QUEUE)
                .build();
    }

    /**
     * 绑定订单业务交换机和订单关闭队列
     *
     * @return 业务绑定关系
     */
    @Bean
    public Binding orderCloseBinding() {
        return BindingBuilder
                .bind(orderCloseQueue())
                .to(orderBusinessExchange())
                .with(ORDER_CLOSE_ROUTING_KEY);
    }
}
```

该方案中，生产者发送消息到 `delay.order.close.queue`，消费者不会直接消费延迟队列。消息在延迟队列中过期后，会通过死信机制转发到 `order.business.exchange`，最终进入 `order.close.queue`，由订单关闭消费者处理。

### rabbitmq_delayed_message_exchange 插件方案

`rabbitmq_delayed_message_exchange` 插件提供 `x-delayed-message` 类型交换机。启用插件后，生产者可以通过 `x-delay` Header 设置延迟毫秒数，消息会在延迟时间到达后再路由到队列。该插件适合秒、分钟、小时级延迟，不适合作为长期调度系统；插件官方仓库也明确提示，如果需要按天、周、月、年级别调度，应考虑外部调度器和适合长期存储的数据存储。([GitHub](https://github.com/rabbitmq/rabbitmq-delayed-message-exchange?utm_source=chatgpt.com))

启用插件命令：

```bash
# 查看 RabbitMQ 插件目录
rabbitmq-plugins directories -s

# 启用延迟消息插件
rabbitmq-plugins enable rabbitmq_delayed_message_exchange

# 查看插件是否启用
rabbitmq-plugins list | grep delayed
```

命令说明：插件需要安装到 RabbitMQ 节点的 plugins 目录中，然后通过 `rabbitmq-plugins enable` 启用。生产环境集群需要在相关节点上统一安装并验证插件版本兼容性。

插件方案特点如下：

| 项目          | 说明                                                         |
| ------------- | ------------------------------------------------------------ |
| Exchange 类型 | `x-delayed-message`                                          |
| 延迟参数      | 消息 Header：`x-delay`，单位毫秒                             |
| 路由行为      | 通过 `x-delayed-type` 指定底层路由类型，例如 `direct`、`topic` |
| 优点          | 每条消息可设置不同延迟时间                                   |
| 缺点          | 依赖插件，需要额外运维和兼容性验证                           |
| 限制          | 不适合海量长期延迟消息                                       |

插件官方说明中还提到，该插件通过 `x-delayed-type` 指定底层路由类型，发送消息时通过 `x-delay` Header 设置延迟时间；如果不需要延迟，不应把它替代普通交换机使用，因为它有额外性能影响。([GitHub](https://github.com/rabbitmq/rabbitmq-delayed-message-exchange?utm_source=chatgpt.com))

### 延迟 Exchange 配置

Spring AMQP 支持延迟消息交换机。可以在 Exchange Bean 上设置 `delayed=true`，RabbitAdmin 会以 `x-delayed-message` 类型声明交换机，并通过原始 Exchange 类型设置 `x-delayed-type` 参数；发送消息时可以通过 `MessageProperties.setDelay()` 设置延迟时间。([Home](https://docs.spring.io/spring-amqp/reference/amqp/delayed-message-exchange.html?utm_source=chatgpt.com))

延迟插件 Exchange 配置如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/DelayedExchangeConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 延迟插件交换机配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class DelayedExchangeConfig {

    public static final String DELAYED_EXCHANGE = "plugin.delayed.order.exchange";

    public static final String DELAYED_ORDER_CLOSE_QUEUE = "plugin.delayed.order.close.queue";

    public static final String DELAYED_ORDER_CLOSE_ROUTING_KEY = "plugin.order.close";

    /**
     * 声明延迟 Direct Exchange
     *
     * @return 延迟交换机
     */
    @Bean
    public DirectExchange delayedOrderExchange() {
        DirectExchange exchange = ExchangeBuilder
                .directExchange(DELAYED_EXCHANGE)
                .durable(true)
                .build();

        // 设置为 delayed exchange，RabbitAdmin 会声明为 x-delayed-message
        exchange.setDelayed(true);
        return exchange;
    }

    /**
     * 声明延迟订单关闭队列
     *
     * @return 订单关闭队列
     */
    @Bean
    public Queue delayedOrderCloseQueue() {
        return QueueBuilder
                .durable(DELAYED_ORDER_CLOSE_QUEUE)
                .build();
    }

    /**
     * 绑定延迟交换机和订单关闭队列
     *
     * @return 绑定关系
     */
    @Bean
    public Binding delayedOrderCloseBinding() {
        return BindingBuilder
                .bind(delayedOrderCloseQueue())
                .to(delayedOrderExchange())
                .with(DELAYED_ORDER_CLOSE_ROUTING_KEY);
    }
}
```

如果当前 Spring AMQP 版本不支持 `exchange.setDelayed(true)`，可以使用 `CustomExchange` 显式声明。

```java
@Bean
public CustomExchange customDelayedExchange() {
    Map<String, Object> arguments = new HashMap<>();
    arguments.put("x-delayed-type", "direct");

    return new CustomExchange(
            "plugin.delayed.order.exchange",
            "x-delayed-message",
            true,
            false,
            arguments
    );
}
```

使用 `CustomExchange` 时需要补充 `java.util.HashMap` 和 `java.util.Map` 导入。实际项目优先使用 Spring AMQP 提供的 `setDelayed(true)` 方式，代码更简洁。

### 延迟消息发送

TTL 延迟方案和插件延迟方案的发送方式不同。TTL 延迟方案通常发送到延迟队列；插件延迟方案发送到 `x-delayed-message` 交换机，并设置 `x-delay`。

TTL 延迟消息发送示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/producer/OrderTtlDelayProducer.java`

```java
package io.github.atengk.rabbitmq.producer;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.rabbitmq.config.OrderDelayQueueConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 订单 TTL 延迟消息生产者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTtlDelayProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送订单超时关闭延迟消息
     *
     * @param orderId 订单ID
     * @return 消息ID
     */
    public String sendOrderCloseDelayMessage(String orderId) {
        String messageId = StrUtil.format("order_close_delay_{}_{}", DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss"), IdUtil.fastSimpleUUID());

        rabbitTemplate.convertAndSend(
                OrderDelayQueueConfig.DELAY_ORDER_EXCHANGE,
                OrderDelayQueueConfig.DELAY_ORDER_CLOSE_ROUTING_KEY,
                orderId,
                message -> {
                    message.getMessageProperties().setMessageId(messageId);
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    message.getMessageProperties().setHeader("x-message-id", messageId);
                    message.getMessageProperties().setHeader("x-business-key", orderId);
                    message.getMessageProperties().setHeader("x-send-time", DateUtil.now());
                    return message;
                },
                new CorrelationData(messageId)
        );

        log.info("订单超时关闭TTL延迟消息已发送，messageId={}，orderId={}，delayQueue={}",
                messageId,
                orderId,
                OrderDelayQueueConfig.DELAY_ORDER_CLOSE_QUEUE
        );
        return messageId;
    }
}
```

插件延迟消息发送示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/producer/OrderPluginDelayProducer.java`

```java
package io.github.atengk.rabbitmq.producer;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.rabbitmq.config.DelayedExchangeConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 订单插件延迟消息生产者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPluginDelayProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送插件延迟消息
     *
     * @param orderId 订单ID
     * @param delayMs 延迟毫秒数
     * @return 消息ID
     */
    public String sendDelayedMessage(String orderId, Integer delayMs) {
        String messageId = StrUtil.format("plugin_delay_{}_{}", DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss"), IdUtil.fastSimpleUUID());

        rabbitTemplate.convertAndSend(
                DelayedExchangeConfig.DELAYED_EXCHANGE,
                DelayedExchangeConfig.DELAYED_ORDER_CLOSE_ROUTING_KEY,
                orderId,
                message -> {
                    message.getMessageProperties().setMessageId(messageId);
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    // Spring AMQP 会映射为 x-delay Header
                    message.getMessageProperties().setDelay(delayMs);
                    message.getMessageProperties().setHeader("x-message-id", messageId);
                    message.getMessageProperties().setHeader("x-business-key", orderId);
                    message.getMessageProperties().setHeader("x-send-time", DateUtil.now());
                    return message;
                },
                new CorrelationData(messageId)
        );

        log.info("订单插件延迟消息已发送，messageId={}，orderId={}，delayMs={}，exchange={}，routingKey={}",
                messageId,
                orderId,
                delayMs,
                DelayedExchangeConfig.DELAYED_EXCHANGE,
                DelayedExchangeConfig.DELAYED_ORDER_CLOSE_ROUTING_KEY
        );
        return messageId;
    }
}
```

发送建议：

1. 固定延迟时间优先使用队列 TTL + 死信方案。
2. 每条消息延迟时间不同，可以使用插件方案。
3. 如果延迟时间跨度很大，不建议把所有消息放入同一个 TTL 队列。
4. 延迟消息也应设置 `messageId`、业务主键、发送时间。
5. 核心延迟消息仍应开启 Publisher Confirm。
6. 插件方案不适合长期调度和海量延迟消息。

### 延迟消息消费

延迟消息消费和普通消息消费没有本质区别。TTL + 死信方案中，消费者监听的是最终业务队列，不是延迟队列；插件方案中，消费者监听绑定到延迟交换机后的业务队列。

订单关闭消费者示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/consumer/OrderCloseDelayConsumer.java`

```java
package io.github.atengk.rabbitmq.consumer;

import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.Channel;
import io.github.atengk.rabbitmq.config.OrderDelayQueueConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 订单延迟关闭消息消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class OrderCloseDelayConsumer {

    /**
     * 消费订单关闭消息
     *
     * @param message 原始消息
     * @param channel RabbitMQ Channel
     * @throws IOException Ack 或 Nack 失败时抛出
     */
    @RabbitListener(queues = OrderDelayQueueConfig.ORDER_CLOSE_QUEUE)
    public void consumeOrderClose(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = StrUtil.blankToDefault(message.getMessageProperties().getMessageId(), "unknown");
        String orderId = new String(message.getBody(), StandardCharsets.UTF_8);

        try {
            log.info("收到订单延迟关闭消息，messageId={}，orderId={}，deliveryTag={}",
                    messageId,
                    orderId,
                    deliveryTag
            );

            closeOrderIfUnpaid(orderId);

            channel.basicAck(deliveryTag, false);
            log.info("订单延迟关闭消息消费成功，messageId={}，orderId={}", messageId, orderId);
        } catch (Exception ex) {
            log.error("订单延迟关闭消息消费失败，messageId={}，orderId={}，原因={}",
                    messageId,
                    orderId,
                    ex.getMessage(),
                    ex
            );

            // 失败后进入该业务队列配置的死信队列或后续补偿流程
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 关闭未支付订单
     *
     * @param orderId 订单ID
     */
    private void closeOrderIfUnpaid(String orderId) {
        log.info("检查订单支付状态并关闭未支付订单，orderId={}", orderId);

        // 实际业务中需要查询订单状态：
        // 1. 若订单已支付，直接忽略
        // 2. 若订单未支付，更新订单状态为已关闭
        // 3. 若订单不存在，记录异常并进入死信或人工处理
    }
}
```

延迟消费注意事项：

| 注意事项       | 说明                              |
| -------------- | --------------------------------- |
| 必须查业务状态 | 延迟到达时业务状态可能已变化      |
| 必须幂等       | 同一订单关闭消息可能重复投递      |
| 不要直接关单   | 先确认订单仍未支付                |
| 失败进入死信   | 关闭失败后应保留异常消息          |
| 日志要完整     | 打印 messageId、orderId、延迟类型 |

订单超时关闭的正确处理逻辑应是“延迟触发检查”，而不是“延迟后必然关闭”。例如用户可能已经在延迟期间完成支付，消费者收到延迟消息后应先查询订单状态，只有仍未支付时才执行关闭。

### 延迟消息适用场景

延迟消息适合需要在未来某个时间点触发处理的业务。它不是定时任务平台的完全替代方案，更适合和业务事件强关联的延迟动作。

常见适用场景如下：

| 场景             | 说明                         | 推荐方案              |
| ---------------- | ---------------------------- | --------------------- |
| 订单超时关闭     | 下单后 30 分钟未支付自动关闭 | TTL + 死信            |
| 支付结果延迟核查 | 支付处理中延迟查询结果       | 插件或 TTL + 死信     |
| 延迟通知         | 一段时间后发送提醒           | 插件方案              |
| 失败后延迟重试   | 失败任务延迟再次执行         | TTL + 死信或插件      |
| 优惠券过期提醒   | 到期前提醒用户               | 外部调度器或插件      |
| 短周期任务调度   | 几秒到几小时后执行           | 插件方案              |
| 长周期定时任务   | 几天、几周、几个月后执行     | 定时任务平台 + 数据库 |

方案选型建议如下：

| 需求                 | 推荐方案                          |
| -------------------- | --------------------------------- |
| 所有消息固定延迟时间 | 队列 TTL + 死信                   |
| 不同消息延迟时间不同 | 延迟插件                          |
| 不允许安装插件       | TTL + 死信                        |
| 延迟时间很长         | 数据库任务表 + XXL-JOB 等调度平台 |
| 延迟消息数量巨大     | 外部调度器 + 数据库存储           |
| 高可靠核心任务       | 本地任务表 + 调度补偿 + MQ        |

延迟方案对比：

| 方案                  | 优点                             | 缺点                                     |
| --------------------- | -------------------------------- | ---------------------------------------- |
| 队列 TTL + 死信       | 不依赖插件，结构清晰             | 固定延迟更适合，多个延迟级别需要多个队列 |
| 消息 TTL + 死信       | 每条消息可设置 TTL               | 可能存在队头阻塞                         |
| 延迟插件              | 使用简单，每条消息可设置不同延迟 | 依赖插件，有运维和性能限制               |
| 数据库任务表 + 调度器 | 适合长周期和大规模任务           | 实现成本高于 MQ 延迟                     |

实践建议：

1. 订单 30 分钟超时关闭这类固定延迟，优先使用 TTL + 死信。
2. 每条消息延迟时间不同且延迟不长，可以使用延迟插件。
3. 长周期任务不要依赖 RabbitMQ 延迟消息，应使用数据库任务表和调度平台。
4. 延迟消息消费端必须检查业务状态，不能假设延迟到达时业务仍满足处理条件。
5. 延迟消息必须具备幂等控制。
6. 延迟队列和死信队列都需要监控队列深度和消费速率。


## 消息幂等设计

本章节用于说明 RabbitMQ 消费端的幂等设计。RabbitMQ 在网络异常、消费者宕机、Ack 失败、重试机制、生产者重复发送、死信重发等情况下，都可能出现同一条业务消息被多次投递或多次处理的问题。因此，消费者必须具备幂等能力，不能假设消息只会被消费一次。

### 幂等问题来源

幂等问题的本质是：同一条消息或同一业务事件被重复处理后，不能导致业务结果重复变更。比如支付成功消息重复消费时，不能重复发放积分；订单创建消息重复消费时，不能重复扣减库存；短信通知消息重复消费时，不能重复发送多条短信。

常见重复消息来源如下：

| 来源                   | 说明                                                       |
| ---------------------- | ---------------------------------------------------------- |
| 生产者重复发送         | 业务重试、本地消息表补偿、接口重复提交导致同一事件多次发送 |
| Publisher Confirm 异常 | 生产者未收到 Confirm，误以为失败后重发                     |
| Consumer Ack 失败      | 消费者业务处理成功，但 Ack 时网络异常，Broker 重新投递     |
| 消费者宕机             | 消费者处理过程中宕机，未 Ack 的消息重新投递                |
| Spring Retry 重试      | 消费异常后框架自动重试                                     |
| 手动 Nack 重新入队     | 消费者调用 `basicNack(requeue=true)`                       |
| 死信重发               | 死信消息人工或任务重新投递                                 |
| 队列恢复               | Broker 或消费者恢复后重新投递未确认消息                    |

幂等设计目标如下：

| 目标       | 说明                                     |
| ---------- | ---------------------------------------- |
| 防重复处理 | 同一消息重复投递时，只执行一次业务副作用 |
| 可追踪     | 可以通过消息 ID 或业务主键查询处理记录   |
| 可恢复     | 处理失败后允许重新消费                   |
| 可并发     | 多消费者并发场景下仍然有效               |
| 可过期     | 非长期业务消息的幂等记录可以自动失效     |
| 可审计     | 核心业务需要长期保留消费记录             |

### 消息唯一标识

消息唯一标识是幂等控制的基础。每条消息必须有一个全局唯一的 `messageId`，同时建议携带业务唯一键 `businessKey`。两者用途不同，不能完全替代。

| 标识            | 示例                            | 作用                                       |
| --------------- | ------------------------------- | ------------------------------------------ |
| `messageId`     | `order_paid_20260511103000_xxx` | 标识一条 MQ 消息，用于链路追踪和消息级幂等 |
| `businessKey`   | `ORDER202605110001`             | 标识一个业务对象，用于业务级幂等           |
| `eventType`     | `order.paid`                    | 标识业务事件类型                           |
| `idempotentKey` | `order.paid:ORDER202605110001`  | 标识某个业务事件只处理一次                 |

推荐幂等 Key 设计：

```text
mq:idempotent:{eventType}:{businessKey}
```

示例：

```text
mq:idempotent:order.paid:ORDER202605110001
mq:idempotent:stock.deduct:ORDER202605110001
mq:idempotent:notice.sms.send:ORDER202605110001
```

消息对象建议同时包含 `messageId`、`eventType` 和 `businessKey`。

文件位置：`src/main/java/io/github/atengk/rabbitmq/dto/IdempotentMqMessage.java`

```java
package io.github.atengk.rabbitmq.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 幂等 MQ 消息对象
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
public class IdempotentMqMessage<T> {

    /**
     * 消息唯一ID
     */
    @NotBlank(message = "消息ID不能为空")
    private String messageId;

    /**
     * 事件类型
     */
    @NotBlank(message = "事件类型不能为空")
    private String eventType;

    /**
     * 业务唯一键
     */
    @NotBlank(message = "业务唯一键不能为空")
    private String businessKey;

    /**
     * 消息版本
     */
    @NotBlank(message = "消息版本不能为空")
    private String version;

    /**
     * 消息创建时间
     */
    @NotNull(message = "消息创建时间不能为空")
    private LocalDateTime timestamp;

    /**
     * 业务数据
     */
    @Valid
    @NotNull(message = "业务数据不能为空")
    private T data;
}
```

唯一标识设计建议：

1. `messageId` 保证消息级唯一，用于 Confirm、Return、消费日志、死信排查。
2. `businessKey` 保证业务对象可追踪，例如订单号、支付单号、库存流水号。
3. 幂等 Key 建议由 `eventType + businessKey` 组成，避免同一订单不同事件互相影响。
4. 不建议只使用 `messageId` 做业务幂等，因为同一业务事件可能因补偿产生新的 `messageId`。
5. 不建议只使用 `businessKey` 做幂等，因为同一业务对象可能存在多个合法事件，例如 `order.created`、`order.paid`、`order.cancelled`。

### Redis 幂等方案

Redis 幂等方案适合高并发、低延迟、幂等记录不需要永久保存的场景。常见做法是使用 `SETNX` 抢占处理权，业务处理成功后标记为已完成。

Redis Key 推荐分为两类：

| Key                                      | 说明                             |
| ---------------------------------------- | -------------------------------- |
| `mq:consuming:{eventType}:{businessKey}` | 正在消费中的锁，防止并发重复处理 |
| `mq:consumed:{eventType}:{businessKey}`  | 已消费标记，防止后续重复消费     |

幂等服务接口如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/service/MessageIdempotentService.java`

```java
package io.github.atengk.rabbitmq.service;

/**
 * 消息幂等服务
 *
 * @author Ateng
 * @since 2026-05-11
 */
public interface MessageIdempotentService {

    /**
     * 尝试开始消费
     *
     * @param eventType   事件类型
     * @param businessKey 业务唯一键
     * @return 是否允许消费
     */
    boolean tryStartConsume(String eventType, String businessKey);

    /**
     * 标记消费成功
     *
     * @param eventType   事件类型
     * @param businessKey 业务唯一键
     */
    void markConsumed(String eventType, String businessKey);

    /**
     * 标记消费失败
     *
     * @param eventType   事件类型
     * @param businessKey 业务唯一键
     */
    void markConsumeFailed(String eventType, String businessKey);
}
```

下面的实现使用 Redis `setIfAbsent` 抢占消费锁，适合多消费者并发消费场景。

文件位置：`src/main/java/io/github/atengk/rabbitmq/service/impl/RedisMessageIdempotentServiceImpl.java`

```java
package io.github.atengk.rabbitmq.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.rabbitmq.service.MessageIdempotentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis 消息幂等服务实现
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisMessageIdempotentServiceImpl implements MessageIdempotentService {

    private static final String CONSUMING_KEY_PREFIX = "mq:consuming:";

    private static final String CONSUMED_KEY_PREFIX = "mq:consumed:";

    private static final Duration CONSUMING_TTL = Duration.ofMinutes(10);

    private static final Duration CONSUMED_TTL = Duration.ofDays(7);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试开始消费
     *
     * @param eventType   事件类型
     * @param businessKey 业务唯一键
     * @return 是否允许消费
     */
    @Override
    public boolean tryStartConsume(String eventType, String businessKey) {
        String consumedKey = buildConsumedKey(eventType, businessKey);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(consumedKey))) {
            log.warn("消息已完成消费，跳过重复处理，eventType={}，businessKey={}", eventType, businessKey);
            return false;
        }

        String consumingKey = buildConsumingKey(eventType, businessKey);
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(consumingKey, "1", CONSUMING_TTL);
        if (!Boolean.TRUE.equals(locked)) {
            log.warn("消息正在消费中，跳过并发重复处理，eventType={}，businessKey={}", eventType, businessKey);
            return false;
        }

        log.info("消息消费锁获取成功，eventType={}，businessKey={}", eventType, businessKey);
        return true;
    }

    /**
     * 标记消费成功
     *
     * @param eventType   事件类型
     * @param businessKey 业务唯一键
     */
    @Override
    public void markConsumed(String eventType, String businessKey) {
        String consumingKey = buildConsumingKey(eventType, businessKey);
        String consumedKey = buildConsumedKey(eventType, businessKey);

        stringRedisTemplate.delete(consumingKey);
        stringRedisTemplate.opsForValue().set(consumedKey, "1", CONSUMED_TTL);

        log.info("消息消费成功标记完成，eventType={}，businessKey={}", eventType, businessKey);
    }

    /**
     * 标记消费失败
     *
     * @param eventType   事件类型
     * @param businessKey 业务唯一键
     */
    @Override
    public void markConsumeFailed(String eventType, String businessKey) {
        String consumingKey = buildConsumingKey(eventType, businessKey);
        stringRedisTemplate.delete(consumingKey);

        log.warn("消息消费失败，已释放消费锁，eventType={}，businessKey={}", eventType, businessKey);
    }

    /**
     * 构建消费中 Key
     *
     * @param eventType   事件类型
     * @param businessKey 业务唯一键
     * @return Redis Key
     */
    private String buildConsumingKey(String eventType, String businessKey) {
        return CONSUMING_KEY_PREFIX + normalize(eventType) + ":" + normalize(businessKey);
    }

    /**
     * 构建已消费 Key
     *
     * @param eventType   事件类型
     * @param businessKey 业务唯一键
     * @return Redis Key
     */
    private String buildConsumedKey(String eventType, String businessKey) {
        return CONSUMED_KEY_PREFIX + normalize(eventType) + ":" + normalize(businessKey);
    }

    /**
     * 规范化 Key 片段
     *
     * @param value 原始值
     * @return 规范化后的值
     */
    private String normalize(String value) {
        return StrUtil.blankToDefault(value, "unknown").trim();
    }
}
```

消费者中使用 Redis 幂等服务。

文件位置：`src/main/java/io/github/atengk/rabbitmq/consumer/IdempotentOrderPaidConsumer.java`

```java
package io.github.atengk.rabbitmq.consumer;

import com.rabbitmq.client.Channel;
import io.github.atengk.rabbitmq.dto.IdempotentMqMessage;
import io.github.atengk.rabbitmq.service.MessageIdempotentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 幂等订单支付消息消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotentOrderPaidConsumer {

    private final MessageIdempotentService messageIdempotentService;

    /**
     * 消费订单支付消息
     *
     * @param payload 订单支付消息
     * @param message 原始消息
     * @param channel RabbitMQ Channel
     * @throws IOException Ack 或 Nack 失败时抛出
     */
    @RabbitListener(queues = "idempotent.order.paid.queue")
    public void consume(IdempotentMqMessage<?> payload, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            log.info("收到幂等订单消息，messageId={}，eventType={}，businessKey={}",
                    payload.getMessageId(),
                    payload.getEventType(),
                    payload.getBusinessKey()
            );

            boolean allowed = messageIdempotentService.tryStartConsume(payload.getEventType(), payload.getBusinessKey());
            if (!allowed) {
                channel.basicAck(deliveryTag, false);
                log.info("重复消息已确认，messageId={}，businessKey={}", payload.getMessageId(), payload.getBusinessKey());
                return;
            }

            handleBusiness(payload);

            messageIdempotentService.markConsumed(payload.getEventType(), payload.getBusinessKey());
            channel.basicAck(deliveryTag, false);

            log.info("幂等订单消息消费成功，messageId={}，businessKey={}", payload.getMessageId(), payload.getBusinessKey());
        } catch (Exception ex) {
            messageIdempotentService.markConsumeFailed(payload.getEventType(), payload.getBusinessKey());

            log.error("幂等订单消息消费失败，messageId={}，businessKey={}，原因={}",
                    payload.getMessageId(),
                    payload.getBusinessKey(),
                    ex.getMessage(),
                    ex
            );

            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 执行业务处理
     *
     * @param payload 消息对象
     */
    private void handleBusiness(IdempotentMqMessage<?> payload) {
        log.info("执行业务处理，eventType={}，businessKey={}", payload.getEventType(), payload.getBusinessKey());
    }
}
```

Redis 幂等方案建议：

1. 使用 `SETNX` 抢占消费权，避免“先查再写”的并发问题。
2. 消费中 Key 必须设置较短 TTL，防止消费者宕机后锁永久存在。
3. 已消费 Key 设置较长 TTL，覆盖业务允许重复消息出现的时间窗口。
4. 业务处理成功后再写已消费标记。
5. 业务处理失败时释放消费中锁，允许后续重试或死信重发。
6. 对资金、审计等核心业务，仅使用 Redis 幂等不够稳妥，建议结合数据库消费记录或业务状态机。

### 数据库唯一索引方案

数据库唯一索引方案适合对一致性和审计要求较高的业务。它通过消费记录表上的唯一索引保证同一业务事件只能插入一次，从而实现幂等控制。

消费记录表设计如下。

```sql
CREATE TABLE mq_consume_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(100) NOT NULL COMMENT '消息ID',
    event_type VARCHAR(64) NOT NULL COMMENT '事件类型',
    business_key VARCHAR(100) NOT NULL COMMENT '业务唯一键',
    queue_name VARCHAR(128) NOT NULL COMMENT '消费队列',
    consume_status VARCHAR(32) NOT NULL COMMENT '消费状态',
    error_message VARCHAR(500) DEFAULT NULL COMMENT '异常信息',
    consume_time DATETIME DEFAULT NULL COMMENT '消费完成时间',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_event_business (event_type, business_key),
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_consume_status (consume_status),
    KEY idx_business_key (business_key)
) COMMENT='MQ消费记录表';
```

状态建议如下：

| 状态        | 说明                   |
| ----------- | ---------------------- |
| `CONSUMING` | 消费处理中             |
| `SUCCESS`   | 消费成功               |
| `FAILED`    | 消费失败               |
| `IGNORED`   | 重复消息或业务确认忽略 |

数据库幂等流程：

```text
收到消息
    -> 插入消费记录，状态 CONSUMING
    -> 插入成功，执行业务处理
    -> 业务成功，更新状态 SUCCESS，并 Ack
    -> 插入失败，说明重复消息
    -> 查询已有状态
        -> SUCCESS，直接 Ack
        -> CONSUMING，按业务策略稍后重试或拒绝
        -> FAILED，按补偿策略处理
```

数据库幂等服务接口如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/service/DbMessageIdempotentService.java`

```java
package io.github.atengk.rabbitmq.service;

/**
 * 数据库消息幂等服务
 *
 * @author Ateng
 * @since 2026-05-11
 */
public interface DbMessageIdempotentService {

    /**
     * 创建消费记录
     *
     * @param messageId   消息ID
     * @param eventType   事件类型
     * @param businessKey 业务唯一键
     * @param queueName   队列名称
     * @return 是否创建成功
     */
    boolean createConsumeRecord(String messageId, String eventType, String businessKey, String queueName);

    /**
     * 标记消费成功
     *
     * @param messageId 消息ID
     */
    void markSuccess(String messageId);

    /**
     * 标记消费失败
     *
     * @param messageId 消息ID
     * @param reason    失败原因
     */
    void markFailed(String messageId, String reason);
}
```

数据库唯一索引方案建议：

1. 核心业务推荐使用 `eventType + businessKey` 作为业务幂等唯一索引。
2. `messageId` 也应建立唯一索引，用于消息链路追踪。
3. 消费记录和业务更新最好在同一个数据库事务中提交。
4. 如果消费记录插入成功但业务失败，应更新状态为 `FAILED`。
5. 如果重复消息命中已有 `SUCCESS` 记录，可以直接 Ack。
6. 如果已有记录是 `CONSUMING`，需要判断是否超时，避免永久处理中。
7. 数据库幂等性能低于 Redis，但审计能力更强。

### 业务状态机方案

业务状态机方案是最贴近业务语义的幂等方式。它不是只判断消息是否处理过，而是判断当前业务状态是否允许执行某个动作。

以订单支付成功事件为例，订单状态流转可以设计为：

```text
CREATED -> PAID -> FINISHED
CREATED -> CANCELLED
```

当消费者收到 `order.paid` 消息时，只有订单当前状态为 `CREATED` 才允许更新为 `PAID`。如果订单已经是 `PAID`，说明消息重复，直接 Ack；如果订单是 `CANCELLED`，说明业务状态冲突，需要进入异常处理或人工确认。

状态机处理示例：

文件位置：`src/main/java/io/github/atengk/rabbitmq/service/OrderStatusHandleService.java`

```java
package io.github.atengk.rabbitmq.service;

/**
 * 订单状态处理服务
 *
 * @author Ateng
 * @since 2026-05-11
 */
public interface OrderStatusHandleService {

    /**
     * 处理订单支付成功状态
     *
     * @param orderId 订单ID
     * @param payNo   支付单号
     * @return 是否执行了状态变更
     */
    boolean handlePaidStatus(String orderId, String payNo);
}
```

下面的代码展示业务状态机幂等处理逻辑，实际项目中应将状态更新放到数据库事务中。

文件位置：`src/main/java/io/github/atengk/rabbitmq/service/impl/OrderStatusHandleServiceImpl.java`

```java
package io.github.atengk.rabbitmq.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.rabbitmq.service.OrderStatusHandleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单状态处理服务实现
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
public class OrderStatusHandleServiceImpl implements OrderStatusHandleService {

    /**
     * 处理订单支付成功状态
     *
     * @param orderId 订单ID
     * @param payNo   支付单号
     * @return 是否执行了状态变更
     */
    @Override
    public boolean handlePaidStatus(String orderId, String payNo) {
        if (StrUtil.hasBlank(orderId, payNo)) {
            throw new IllegalArgumentException("订单ID或支付单号不能为空");
        }

        String currentStatus = queryOrderStatus(orderId);
        if ("PAID".equals(currentStatus)) {
            log.warn("订单已支付，忽略重复支付消息，orderId={}，payNo={}", orderId, payNo);
            return false;
        }

        if (!"CREATED".equals(currentStatus)) {
            throw new IllegalStateException(StrUtil.format("订单状态不允许支付，orderId={}，status={}", orderId, currentStatus));
        }

        updateOrderStatusToPaid(orderId, payNo);
        log.info("订单状态已更新为已支付，orderId={}，payNo={}", orderId, payNo);
        return true;
    }

    /**
     * 查询订单状态
     *
     * @param orderId 订单ID
     * @return 订单状态
     */
    private String queryOrderStatus(String orderId) {
        // 示例代码，实际项目中从数据库查询订单状态
        return "CREATED";
    }

    /**
     * 更新订单状态为已支付
     *
     * @param orderId 订单ID
     * @param payNo   支付单号
     */
    private void updateOrderStatusToPaid(String orderId, String payNo) {
        // 示例代码，实际项目中执行数据库状态更新
        log.info("执行订单状态更新，orderId={}，payNo={}", orderId, payNo);
    }
}
```

数据库层面建议使用条件更新进一步保证并发安全：

```sql
UPDATE t_order
SET order_status = 'PAID',
    pay_no = ?,
    pay_time = NOW(),
    update_time = NOW()
WHERE order_id = ?
  AND order_status = 'CREATED';
```

如果影响行数为 `1`，说明状态变更成功；如果影响行数为 `0`，说明订单状态已经变化，需要重新查询状态后判断是重复消息还是异常状态。

业务状态机方案建议：

1. 订单、支付、退款、库存等核心业务必须做状态机判断。
2. 不要只依赖 MQ 消息幂等表，业务状态本身也要防重复。
3. 状态更新使用条件更新，避免并发覆盖。
4. 重复消息命中最终状态时应 Ack，不要重复执行业务。
5. 状态冲突消息应进入异常处理或死信队列。
6. 状态机是业务幂等的最终防线。

### 幂等失效策略

幂等失效策略用于控制幂等记录保存多久。不同业务对幂等记录的保存时间要求不同。Redis 幂等需要设置 TTL，数据库幂等需要制定归档或清理策略。

常见失效策略如下：

| 业务类型 | 推荐保存时间     | 说明               |
| -------- | ---------------- | ------------------ |
| 订单支付 | 长期或至少 90 天 | 涉及资金和审计     |
| 库存扣减 | 30 到 90 天      | 取决于订单售后周期 |
| 短信通知 | 1 到 7 天        | 防止短时间重复发送 |
| 日志采集 | 数小时到 1 天    | 低价值消息         |
| 临时任务 | 1 到 3 天        | 过期后可清理       |
| 审计事件 | 长期保存         | 满足审计要求       |

Redis TTL 示例：

```java
private static final Duration CONSUMED_TTL = Duration.ofDays(7);
```

数据库归档示例：

```sql
-- 将 90 天前的成功消费记录归档或清理，执行前需确认审计要求
DELETE FROM mq_consume_record
WHERE consume_status = 'SUCCESS'
  AND consume_time < DATE_SUB(NOW(), INTERVAL 90 DAY);
```

幂等失效设计建议：

1. 资金类、审计类消息不建议只依赖短 TTL Redis Key。
2. Redis 幂等 Key 必须设置过期时间，避免无限增长。
3. 数据库消费记录需要定期归档，避免表过大。
4. 幂等失效时间应覆盖消息可能重发的最长时间窗口。
5. 死信消息可能在较晚时间被人工重发，因此核心业务需要业务状态机兜底。
6. 删除幂等记录前要确认业务是否仍可能发生补偿重发。

### 幂等异常处理

幂等异常处理用于说明幂等服务本身出现异常时应该如何处理。比如 Redis 不可用、数据库唯一索引插入失败、消费记录状态异常等，都可能影响消费者是否继续处理消息。

常见异常及处理建议如下：

| 异常                   | 说明                     | 建议处理                                           |
| ---------------------- | ------------------------ | -------------------------------------------------- |
| Redis 不可用           | 无法判断是否重复消费     | 核心业务 Nack 或进入死信，不建议继续处理           |
| 数据库插入幂等记录失败 | 可能重复消息或数据库异常 | 判断异常类型，唯一索引冲突可 Ack，数据库异常可重试 |
| 幂等 Key 已存在        | 消息已消费或正在消费     | 已成功则 Ack，处理中则稍后重试                     |
| 消费成功但标记失败     | 业务已执行但幂等标记失败 | 依赖业务状态机兜底，并记录异常                     |
| 幂等记录永久处理中     | 消费者宕机或逻辑异常     | 超时后允许补偿处理                                 |

幂等异常处理原则：

1. 核心业务中，幂等系统不可用时不要盲目执行业务。
2. 重复消息已确认处理成功时，应 Ack。
3. 幂等记录处于处理中且未超时时，可以拒绝重新入队或稍后重试。
4. 幂等服务异常需要记录错误日志并触发告警。
5. 业务状态机必须作为最终兜底，防止幂等系统异常导致重复业务变更。

消费者异常处理示例：

```java
try {
    boolean allowed = messageIdempotentService.tryStartConsume(eventType, businessKey);
    if (!allowed) {
        channel.basicAck(deliveryTag, false);
        return;
    }

    handleBusiness(payload);
    messageIdempotentService.markConsumed(eventType, businessKey);
    channel.basicAck(deliveryTag, false);
} catch (Exception ex) {
    log.error("消息幂等或业务处理异常，eventType={}，businessKey={}，原因={}",
            eventType,
            businessKey,
            ex.getMessage(),
            ex
    );

    // 核心业务不建议在幂等异常时直接 Ack
    channel.basicNack(deliveryTag, false, false);
}
```

## 消息顺序性设计

本章节用于说明 RabbitMQ 中顺序消息的设计方式。RabbitMQ 可以保证单个队列中消息按入队顺序存储，但实际消费时是否保持业务顺序，还受到多消费者、并发线程、预取数量、失败重试、重新入队、死信重发等因素影响。

### 顺序消息问题

顺序消息问题通常发生在同一个业务对象的多个事件必须按固定顺序处理时。例如订单状态事件必须先处理 `order.created`，再处理 `order.paid`，最后处理 `order.finished`。如果消费者先处理了支付事件，再处理创建事件，就可能出现业务状态异常。

常见顺序要求场景如下：

| 场景         | 顺序要求                           |
| ------------ | ---------------------------------- |
| 订单状态变更 | 创建 -> 支付 -> 完成 / 取消        |
| 支付状态同步 | 创建支付单 -> 支付成功 -> 对账完成 |
| 库存流水     | 冻结 -> 扣减 -> 解冻               |
| 用户账户流水 | 入账、出账按发生顺序处理           |
| 审批流       | 提交 -> 审批 -> 归档               |
| 日志回放     | 按事件产生顺序回放                 |

RabbitMQ 中影响顺序性的因素如下：

| 因素           | 影响                                                     |
| -------------- | -------------------------------------------------------- |
| 多队列         | 同一业务对象消息进入不同队列，无法保证全局顺序           |
| 多消费者       | 同一队列多个消费者并行处理，完成顺序可能变化             |
| prefetch 过大  | 消息被提前分发到不同消费者，处理顺序不可控               |
| 消费失败重试   | 后续消息可能先成功                                       |
| Nack 重新入队  | 失败消息重新排队后顺序可能变化                           |
| 死信重发       | 死信消息重新投递时已脱离原顺序                           |
| 生产者并发发送 | 多线程发送时消息进入 Broker 的顺序可能不等于业务产生顺序 |

顺序性设计的核心不是追求全局顺序，而是明确“按什么维度保证顺序”。实际项目通常只需要保证同一个订单、同一个用户、同一个账户、同一个库存 SKU 的消息顺序。

### 单队列顺序消费

单队列顺序消费是最简单的顺序方案。它要求同一类顺序消息进入同一个队列，并且该队列只使用一个消费者线程消费，同时设置较小的 `prefetch`。

配置示例：

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        # 手动确认
        acknowledge-mode: manual
        # 单消费者，保证同一队列按顺序处理
        concurrency: 1
        # 最大消费者也保持为 1
        max-concurrency: 1
        # 一次只拉取一条未确认消息
        prefetch: 1
```

顺序队列配置示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/OrderedQueueConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 顺序消息队列配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class OrderedQueueConfig {

    public static final String ORDERED_EXCHANGE = "ordered.order.exchange";

    public static final String ORDERED_QUEUE = "ordered.order.queue";

    public static final String ORDERED_ROUTING_KEY = "ordered.order";

    /**
     * 声明顺序消息交换机
     *
     * @return Direct Exchange
     */
    @Bean
    public DirectExchange orderedExchange() {
        return ExchangeBuilder
                .directExchange(ORDERED_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 声明顺序消息队列
     *
     * @return 顺序消息队列
     */
    @Bean
    public Queue orderedQueue() {
        return QueueBuilder
                .durable(ORDERED_QUEUE)
                .build();
    }

    /**
     * 绑定顺序消息队列
     *
     * @return Binding
     */
    @Bean
    public Binding orderedBinding() {
        return BindingBuilder
                .bind(orderedQueue())
                .to(orderedExchange())
                .with(ORDERED_ROUTING_KEY);
    }
}
```

顺序消费者示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/consumer/OrderedMessageConsumer.java`

```java
package io.github.atengk.rabbitmq.consumer;

import com.rabbitmq.client.Channel;
import io.github.atengk.rabbitmq.config.OrderedQueueConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 顺序消息消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class OrderedMessageConsumer {

    /**
     * 顺序消费订单消息
     *
     * @param payload 消息体
     * @param message 原始消息
     * @param channel RabbitMQ Channel
     * @throws IOException Ack 或 Nack 失败时抛出
     */
    @RabbitListener(queues = OrderedQueueConfig.ORDERED_QUEUE, concurrency = "1")
    public void consume(String payload, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            log.info("收到顺序消息，deliveryTag={}，payload={}", deliveryTag, payload);

            handleOrderedBusiness(payload);

            channel.basicAck(deliveryTag, false);
            log.info("顺序消息消费成功，deliveryTag={}", deliveryTag);
        } catch (Exception ex) {
            log.error("顺序消息消费失败，deliveryTag={}，原因={}", deliveryTag, ex.getMessage(), ex);

            // 顺序场景中不建议直接跳过失败消息，需要按业务策略处理
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 处理顺序业务
     *
     * @param payload 消息体
     */
    private void handleOrderedBusiness(String payload) {
        log.info("执行顺序业务处理，payload={}", payload);
    }
}
```

单队列顺序消费优缺点：

| 项目       | 说明                         |
| ---------- | ---------------------------- |
| 优点       | 实现简单，顺序性最好         |
| 缺点       | 吞吐量低，单点消费能力有限   |
| 适用场景   | 消息量小、顺序要求严格       |
| 不适用场景 | 高吞吐、大量业务对象并发处理 |

### 分片队列方案

分片队列方案是在顺序性和吞吐量之间折中。它按照业务 Key 将消息分发到不同队列，同一个业务 Key 的消息始终进入同一个队列，从而保证单个业务对象内部有序，同时多个分片队列可以并行消费。

分片思路如下：

```text
orderId hash % shardCount = shardIndex

ORDER001 -> shard-0
ORDER002 -> shard-1
ORDER003 -> shard-2
ORDER004 -> shard-0
```

分片队列资源示例：

| 分片 | Queue                   | Routing Key       |
| ---- | ----------------------- | ----------------- |
| 0    | `ordered.order.queue.0` | `ordered.order.0` |
| 1    | `ordered.order.queue.1` | `ordered.order.1` |
| 2    | `ordered.order.queue.2` | `ordered.order.2` |
| 3    | `ordered.order.queue.3` | `ordered.order.3` |

分片队列配置示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/ShardingOrderedQueueConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 分片顺序队列配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class ShardingOrderedQueueConfig {

    public static final String SHARDING_ORDERED_EXCHANGE = "ordered.sharding.order.exchange";

    public static final String QUEUE_PREFIX = "ordered.sharding.order.queue.";

    public static final String ROUTING_KEY_PREFIX = "ordered.order.";

    /**
     * 声明分片顺序交换机
     *
     * @return Direct Exchange
     */
    @Bean
    public DirectExchange shardingOrderedExchange() {
        return ExchangeBuilder
                .directExchange(SHARDING_ORDERED_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue shardingOrderedQueue0() {
        return QueueBuilder.durable(QUEUE_PREFIX + "0").build();
    }

    @Bean
    public Queue shardingOrderedQueue1() {
        return QueueBuilder.durable(QUEUE_PREFIX + "1").build();
    }

    @Bean
    public Queue shardingOrderedQueue2() {
        return QueueBuilder.durable(QUEUE_PREFIX + "2").build();
    }

    @Bean
    public Queue shardingOrderedQueue3() {
        return QueueBuilder.durable(QUEUE_PREFIX + "3").build();
    }

    @Bean
    public Binding shardingOrderedBinding0() {
        return BindingBuilder.bind(shardingOrderedQueue0()).to(shardingOrderedExchange()).with(ROUTING_KEY_PREFIX + "0");
    }

    @Bean
    public Binding shardingOrderedBinding1() {
        return BindingBuilder.bind(shardingOrderedQueue1()).to(shardingOrderedExchange()).with(ROUTING_KEY_PREFIX + "1");
    }

    @Bean
    public Binding shardingOrderedBinding2() {
        return BindingBuilder.bind(shardingOrderedQueue2()).to(shardingOrderedExchange()).with(ROUTING_KEY_PREFIX + "2");
    }

    @Bean
    public Binding shardingOrderedBinding3() {
        return BindingBuilder.bind(shardingOrderedQueue3()).to(shardingOrderedExchange()).with(ROUTING_KEY_PREFIX + "3");
    }
}
```

分片计算工具如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/util/MessageShardUtil.java`

```java
package io.github.atengk.rabbitmq.util;

import cn.hutool.core.util.StrUtil;

/**
 * 消息分片工具
 *
 * @author Ateng
 * @since 2026-05-11
 */
public final class MessageShardUtil {

    private MessageShardUtil() {
    }

    /**
     * 根据业务键计算分片下标
     *
     * @param businessKey 业务键
     * @param shardCount  分片数量
     * @return 分片下标
     */
    public static int shardIndex(String businessKey, int shardCount) {
        if (shardCount <= 0) {
            throw new IllegalArgumentException("分片数量必须大于0");
        }

        String key = StrUtil.blankToDefault(businessKey, "unknown");
        return Math.floorMod(key.hashCode(), shardCount);
    }

    /**
     * 构建分片路由键
     *
     * @param routingKeyPrefix 路由键前缀
     * @param businessKey      业务键
     * @param shardCount       分片数量
     * @return 分片路由键
     */
    public static String buildRoutingKey(String routingKeyPrefix, String businessKey, int shardCount) {
        int shardIndex = shardIndex(businessKey, shardCount);
        return routingKeyPrefix + shardIndex;
    }
}
```

分片队列适合以下场景：

| 场景                  | 说明                        |
| --------------------- | --------------------------- |
| 同一订单消息要求有序  | 按 `orderId` 分片           |
| 同一用户账户流水有序  | 按 `userId` 分片            |
| 同一 SKU 库存流水有序 | 按 `skuId` 分片             |
| 消息量较大            | 多个分片提升吞吐量          |
| 不要求全局顺序        | 只保证同一业务 Key 内部有序 |

### Routing Key 分片策略

Routing Key 分片策略是分片队列方案的核心。生产者根据业务 Key 计算分片下标，然后将消息发送到对应 Routing Key，确保相同业务 Key 的消息始终进入同一个队列。

发送示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/producer/ShardingOrderedProducer.java`

```java
package io.github.atengk.rabbitmq.producer;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.rabbitmq.config.ShardingOrderedQueueConfig;
import io.github.atengk.rabbitmq.util.MessageShardUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 分片顺序消息生产者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShardingOrderedProducer {

    private static final int SHARD_COUNT = 4;

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送分片顺序消息
     *
     * @param businessKey 业务键
     * @param payload     消息体
     * @return 消息ID
     */
    public String send(String businessKey, Object payload) {
        String messageId = StrUtil.format("ordered_{}_{}", DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss"), IdUtil.fastSimpleUUID());
        String routingKey = MessageShardUtil.buildRoutingKey(
                ShardingOrderedQueueConfig.ROUTING_KEY_PREFIX,
                businessKey,
                SHARD_COUNT
        );

        rabbitTemplate.convertAndSend(
                ShardingOrderedQueueConfig.SHARDING_ORDERED_EXCHANGE,
                routingKey,
                payload,
                message -> {
                    message.getMessageProperties().setMessageId(messageId);
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    message.getMessageProperties().setHeader("x-business-key", businessKey);
                    message.getMessageProperties().setHeader("x-sharding-routing-key", routingKey);
                    message.getMessageProperties().setHeader("x-send-time", DateUtil.now());
                    return message;
                },
                new CorrelationData(messageId)
        );

        log.info("分片顺序消息已发送，messageId={}，businessKey={}，routingKey={}",
                messageId,
                businessKey,
                routingKey
        );
        return messageId;
    }
}
```

Routing Key 分片策略建议：

1. 分片 Key 必须选择需要保证顺序的业务维度，例如 `orderId`、`userId`、`skuId`。
2. 相同业务 Key 必须始终路由到同一个分片。
3. 分片数量上线后不要频繁修改，否则相同业务 Key 可能进入不同队列。
4. 如果必须扩容分片，需要设计迁移窗口或新旧路由兼容方案。
5. Routing Key 中不要直接拼接高基数字段，建议只拼接分片编号。
6. 生产者和消费者必须使用相同的分片数量和分片算法。

不推荐：

```text
ordered.order.ORDER202605110001
```

推荐：

```text
ordered.order.0
ordered.order.1
ordered.order.2
ordered.order.3
```

前者会导致 Routing Key 数量爆炸，Binding 难以维护；后者将高基数字段转化为有限分片，更适合 RabbitMQ 路由模型。

### 并发消费对顺序性的影响

并发消费会提升吞吐量，但会破坏同一队列中的严格处理顺序。即使 RabbitMQ 按顺序投递消息，多个消费者并发处理时，也无法保证先投递的消息先完成。

影响顺序性的配置包括：

| 配置                  | 影响                                            |
| --------------------- | ----------------------------------------------- |
| `concurrency > 1`     | 同一队列多个消费者并发处理，完成顺序可能变化    |
| `max-concurrency > 1` | 高峰期自动增加消费者后顺序可能变化              |
| `prefetch > 1`        | 消息提前分发到消费者，本地处理顺序不可控        |
| 多实例部署            | 多个应用实例同时消费同一队列                    |
| 异步线程池处理        | Listener 收到消息后丢给线程池，会进一步破坏顺序 |

严格顺序消费推荐配置：

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        acknowledge-mode: manual
        # 严格顺序消费时保持单消费者
        concurrency: 1
        max-concurrency: 1
        # 一次只预取一条消息
        prefetch: 1
```

分片顺序消费推荐配置：

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        acknowledge-mode: manual
        # 每个队列仍然建议单消费者
        prefetch: 1
```

分片队列消费者可以多个队列并行，但每个分片队列内部保持单消费者。例如 4 个分片队列可以有 4 个消费者分别处理，从而保证单个分片有序，整体并行。

并发消费选择建议：

| 需求             | 建议                               |
| ---------------- | ---------------------------------- |
| 严格全局顺序     | 单队列、单消费者、prefetch=1       |
| 同一订单内有序   | 按 orderId 分片，多队列并行        |
| 只要求最终一致   | 可以多消费者并发                   |
| 高吞吐优先       | 放弃严格顺序，使用幂等和状态机兜底 |
| 消息处理耗时较长 | 不建议单队列严格顺序，应考虑分片   |

### 顺序消费异常处理

顺序消费中，异常处理比普通消费更复杂。因为某一条消息处理失败后，如果直接跳过，后续消息可能基于错误状态继续处理；如果无限重试，又可能阻塞整个队列。

常见异常处理策略如下：

| 策略           | 说明                                   | 适用场景         |
| -------------- | -------------------------------------- | ---------------- |
| 阻塞重试       | 当前消息成功前不处理后续消息           | 强顺序、低吞吐   |
| 有限重试后死信 | 重试失败进入死信，后续继续处理         | 顺序要求中等     |
| 业务状态机兜底 | 允许后续消息先到，但按状态判断是否处理 | 高吞吐、最终一致 |
| 补偿重放       | 修复异常后按业务顺序重新投递           | 核心状态流转     |
| 人工处理       | 异常状态需要人工确认                   | 资金、库存、审计 |

顺序消息异常消费者示例：

文件位置：`src/main/java/io/github/atengk/rabbitmq/consumer/OrderedExceptionConsumer.java`

```java
package io.github.atengk.rabbitmq.consumer;

import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.Channel;
import io.github.atengk.rabbitmq.config.OrderedQueueConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 顺序消息异常处理消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class OrderedExceptionConsumer {

    /**
     * 消费顺序消息并处理异常
     *
     * @param payload 消息体
     * @param message 原始消息
     * @param channel RabbitMQ Channel
     * @throws IOException Ack 或 Nack 失败时抛出
     */
    @RabbitListener(queues = OrderedQueueConfig.ORDERED_QUEUE, concurrency = "1")
    public void consume(String payload, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = StrUtil.blankToDefault(message.getMessageProperties().getMessageId(), "unknown");
        String businessKey = String.valueOf(message.getMessageProperties().getHeaders().get("x-business-key"));

        try {
            log.info("收到顺序消息，messageId={}，businessKey={}，deliveryTag={}",
                    messageId,
                    businessKey,
                    deliveryTag
            );

            validateOrderedMessage(payload);
            handleOrderedBusiness(payload);

            channel.basicAck(deliveryTag, false);
            log.info("顺序消息处理成功，messageId={}，businessKey={}", messageId, businessKey);
        } catch (IllegalArgumentException ex) {
            log.error("顺序消息参数异常，进入死信，messageId={}，businessKey={}，原因={}",
                    messageId,
                    businessKey,
                    ex.getMessage(),
                    ex
            );

            // 参数错误通常无法通过重试恢复，进入死信后人工排查
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception ex) {
            log.error("顺序消息处理失败，进入死信或补偿流程，messageId={}，businessKey={}，原因={}",
                    messageId,
                    businessKey,
                    ex.getMessage(),
                    ex
            );

            // 顺序场景中不建议无限 requeue=true，否则会阻塞整个队列
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 校验顺序消息
     *
     * @param payload 消息体
     */
    private void validateOrderedMessage(String payload) {
        if (StrUtil.isBlank(payload)) {
            throw new IllegalArgumentException("顺序消息体不能为空");
        }
    }

    /**
     * 处理顺序业务
     *
     * @param payload 消息体
     */
    private void handleOrderedBusiness(String payload) {
        log.info("执行顺序业务处理，payload={}", payload);
    }
}
```

顺序消费异常处理建议：

1. 强顺序业务中，失败消息不能简单跳过，必须有补偿或人工处理。
2. 不建议无限 `requeue=true`，否则一个异常消息会阻塞整个队列。
3. 如果允许后续消息继续处理，必须依赖业务状态机判断合法状态。
4. 死信重发时需要注意顺序已经被打破，不能直接无脑重发。
5. 顺序消息应尽量减少复杂外部依赖，降低消费失败概率。
6. 对订单状态类消息，消费者应按当前状态判断事件是否可执行，而不是只按到达顺序处理。
7. 如果必须严格按事件版本处理，可以在消息中增加 `sequence` 字段，并在数据库中记录最后处理序号。

顺序消息中增加序号字段示例：

```json
{
  "messageId": "order_event_20260511103000_xxx",
  "eventType": "order.paid",
  "businessKey": "ORDER202605110001",
  "sequence": 2,
  "timestamp": "2026-05-11 10:30:00",
  "data": {
    "orderId": "ORDER202605110001",
    "status": "PAID"
  }
}
```

数据库可以记录每个业务对象最后处理的序号：

```sql
CREATE TABLE mq_order_sequence_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    business_key VARCHAR(100) NOT NULL COMMENT '业务唯一键',
    last_sequence BIGINT NOT NULL COMMENT '最后处理序号',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_business_key (business_key)
) COMMENT='订单顺序消息处理记录表';
```

处理时判断：

| 条件                           | 处理方式                             |
| ------------------------------ | ------------------------------------ |
| `sequence = last_sequence + 1` | 正常处理并更新序号                   |
| `sequence <= last_sequence`    | 重复消息，直接 Ack                   |
| `sequence > last_sequence + 1` | 前置消息缺失，进入延迟重试或异常队列 |

最终建议：RabbitMQ 中不要轻易追求全局顺序。大多数业务应采用“按业务 Key 分片有序 + 幂等控制 + 状态机兜底”的方案，在保证关键业务对象内部顺序的同时保留系统吞吐能力。


## 消息堆积处理

本章节用于说明 RabbitMQ 消息堆积的原因分析、消费能力评估、线程配置、批量消费、临时扩容、消息过期和监控指标。消息堆积本身不是单一问题，通常是生产速度、消费能力、下游依赖、消费者异常、队列配置和业务流量共同作用的结果。

### 消息堆积原因

消息堆积是指消息持续进入队列，但消费者处理速度低于生产者发送速度，导致队列中的 Ready 消息数量持续增长。堆积如果长期不处理，会造成业务延迟、磁盘压力、内存压力、消费者恢复困难，严重时会影响 RabbitMQ Broker 稳定性。

常见堆积原因如下：

| 原因             | 说明                            | 典型表现                     |
| ---------------- | ------------------------------- | ---------------------------- |
| 生产速度过快     | 短时间大量消息进入队列          | Ready 数量快速上涨           |
| 消费者实例不足   | 消费者数量无法支撑消息量        | Consumers 数量少，消费速率低 |
| 消费逻辑耗时     | 单条消息处理时间长              | Unacked 数量较高             |
| 下游服务变慢     | 数据库、Redis、第三方接口响应慢 | 消费耗时升高                 |
| 消费者异常       | 消费者启动失败或反复报错        | 消费日志大量异常             |
| Ack 阻塞         | 消息处理完成但确认失败或未确认  | Unacked 长时间不下降         |
| prefetch 过大    | 消息被消费者提前拉取但处理慢    | Unacked 很高，Ready 下降慢   |
| 队列路由错误     | 消息进入了没有消费者的队列      | Ready 增长，Consumers 为 0   |
| 消费线程配置过小 | 并发数无法支撑峰值流量          | CPU 空闲但消费慢             |
| 业务限流         | 消费者主动限制消费速度          | 消费速率稳定但低于生产速率   |

排查消息堆积时，应先区分两类指标：

| 指标    | 含义                           | 排查方向                                 |
| ------- | ------------------------------ | ---------------------------------------- |
| Ready   | 队列中等待投递的消息数         | 消费者数量、消费速率、是否有消费者       |
| Unacked | 已投递给消费者但未确认的消息数 | 消费耗时、Ack 逻辑、prefetch、消费者阻塞 |

如果 `Ready` 很高而 `Consumers=0`，通常是消费者未启动、监听队列错误或应用连接异常。如果 `Unacked` 很高，通常是消费者已经拿到消息但处理慢、阻塞或没有及时 Ack。

### 消费能力评估

消费能力评估用于判断当前消费者是否能处理生产端流量。评估时不能只看消费者数量，还要结合单条消息平均耗时、消费者并发数、下游服务吞吐能力和业务高峰流量。

基础计算公式如下：

```text
理论消费速率 = 消费者并发数 × 单个消费者每秒处理消息数
单个消费者每秒处理消息数 = 1000 / 单条消息平均处理耗时毫秒
```

示例：

| 指标                   | 数值     |
| ---------------------- | -------- |
| 单条消息平均耗时       | 200ms    |
| 单个消费者每秒处理能力 | 5 条/秒  |
| 消费者并发数           | 10       |
| 理论消费速率           | 50 条/秒 |

如果生产端峰值发送速度是 200 条/秒，而消费者理论消费能力只有 50 条/秒，那么必然产生堆积。

评估项如下：

| 评估项         | 说明                                    |
| -------------- | --------------------------------------- |
| 平均消费耗时   | 单条消息处理平均耗时                    |
| P95 / P99 耗时 | 高百分位耗时，反映慢请求                |
| 当前消费者数量 | 当前监听队列的 Consumer 数              |
| 单实例线程数   | 每个应用实例的消费线程数                |
| 应用实例数     | 消费者服务部署实例数量                  |
| 生产速率       | 每秒进入队列的消息数量                  |
| 消费速率       | 每秒被 Ack 的消息数量                   |
| 下游容量       | 数据库、Redis、第三方接口是否能支撑扩容 |
| 堆积增长速度   | Ready 数量每分钟增长多少                |

消费能力评估示例：

```text
生产速率：300 条/秒
单条消费耗时：100ms
单线程消费能力：10 条/秒
当前消费线程数：10
当前消费能力：100 条/秒
缺口：200 条/秒
建议：至少扩容到 30 个消费线程，并评估数据库和下游接口容量
```

注意：消费线程不是越多越好。消费者扩容会增加数据库连接、Redis 连接、第三方接口调用和应用 CPU 压力。如果下游系统已经是瓶颈，盲目增加消费者只会把压力从 RabbitMQ 转移到下游系统。

### 消费线程配置

消费线程配置用于提升单个消费者应用实例的并发处理能力。Spring Boot 中常用参数包括 `concurrency`、`max-concurrency` 和 `prefetch`。

基础配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        # 手动确认，业务处理成功后再 Ack
        acknowledge-mode: manual
        # 初始消费者线程数
        concurrency: 4
        # 最大消费者线程数
        max-concurrency: 16
        # 每个消费者最多持有的未确认消息数
        prefetch: 10
        # 消费异常后不默认重新入队，避免异常消息无限循环
        default-requeue-rejected: false
```

参数说明：

| 参数                       | 说明                 | 建议                       |
| -------------------------- | -------------------- | -------------------------- |
| `concurrency`              | 初始消费者线程数     | 根据平峰流量配置           |
| `max-concurrency`          | 最大消费者线程数     | 根据高峰流量配置           |
| `prefetch`                 | 每个消费者预取消息数 | 根据处理耗时调整           |
| `acknowledge-mode`         | 确认模式             | 核心业务使用 `manual`      |
| `default-requeue-rejected` | 异常是否重新入队     | 配合死信队列时建议 `false` |

如果需要按不同队列配置不同并发，可以定义多个监听容器工厂。

下面的配置定义了一个高并发消费容器，适合处理日志、通知、异步任务等可并发业务。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/HighConcurrencyListenerConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 高并发 RabbitMQ 监听容器配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class HighConcurrencyListenerConfig {

    /**
     * 高并发监听容器工厂
     *
     * @param configurer        Spring Boot 监听容器配置器
     * @param connectionFactory RabbitMQ 连接工厂
     * @return 监听容器工厂
     */
    @Bean
    public SimpleRabbitListenerContainerFactory highConcurrencyRabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);

        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConcurrentConsumers(4);
        factory.setMaxConcurrentConsumers(16);
        factory.setPrefetchCount(10);
        factory.setDefaultRequeueRejected(false);

        return factory;
    }
}
```

消费者使用指定容器工厂：

```java
@RabbitListener(
        queues = "notice.send.queue",
        containerFactory = "highConcurrencyRabbitListenerContainerFactory"
)
public void consume(String payload, Message message, Channel channel) {
    // 消费逻辑
}
```

线程配置建议：

1. CPU 密集型任务不宜设置过高并发。
2. IO 密集型任务可以适当提高并发，但要评估数据库、Redis 和第三方接口容量。
3. 顺序消费场景不应提高同一队列并发。
4. `prefetch` 过大会导致单个消费者持有大量未确认消息，异常时恢复较慢。
5. 扩容消费者前，应先确认消费者代码没有阻塞、死锁或异常重试问题。

### 批量消费方案

批量消费用于减少单条消息逐个处理带来的开销，例如批量写数据库、批量调用接口、批量落日志等。批量消费可以提升吞吐量，但会增加单批处理失败、部分成功、批量 Ack 和幂等处理复杂度。

批量消费适合以下场景：

| 场景         | 说明                         |
| ------------ | ---------------------------- |
| 日志采集     | 多条日志批量写入数据库或文件 |
| 数据同步     | 多条变更批量写入目标系统     |
| 通知发送     | 批量提交短信或邮件任务       |
| 统计上报     | 多条事件批量聚合处理         |
| 非强顺序任务 | 不要求单条严格顺序           |

不适合批量消费的场景：

| 场景         | 原因                         |
| ------------ | ---------------------------- |
| 支付消息     | 单条失败需要精确控制         |
| 库存扣减     | 部分失败处理复杂             |
| 严格顺序消息 | 批量处理会增加顺序控制复杂度 |
| 强事务消息   | 批内部分成功难以补偿         |

批量监听配置示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/BatchListenerConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 批量消费监听配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class BatchListenerConfig {

    /**
     * 批量消费监听容器工厂
     *
     * @param configurer        Spring Boot 监听容器配置器
     * @param connectionFactory RabbitMQ 连接工厂
     * @return 批量消费监听容器工厂
     */
    @Bean
    public SimpleRabbitListenerContainerFactory batchRabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);

        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConsumerBatchEnabled(true);
        factory.setBatchListener(true);
        factory.setBatchSize(50);
        factory.setPrefetchCount(100);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(8);

        return factory;
    }
}
```

批量消费者示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/consumer/BatchMessageConsumer.java`

```java
package io.github.atengk.rabbitmq.consumer;

import cn.hutool.core.collection.CollUtil;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 批量消息消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class BatchMessageConsumer {

    /**
     * 批量消费消息
     *
     * @param messages 消息列表
     * @param channel  RabbitMQ Channel
     * @throws IOException Ack 或 Nack 失败时抛出
     */
    @RabbitListener(
            queues = "batch.message.queue",
            containerFactory = "batchRabbitListenerContainerFactory"
    )
    public void consumeBatch(List<Message> messages, Channel channel) throws IOException {
        if (CollUtil.isEmpty(messages)) {
            return;
        }

        long lastDeliveryTag = messages.get(messages.size() - 1).getMessageProperties().getDeliveryTag();

        try {
            log.info("收到批量消息，batchSize={}", messages.size());

            for (Message message : messages) {
                String payload = new String(message.getBody(), StandardCharsets.UTF_8);
                log.info("批量消息内容，messageId={}，payload={}",
                        message.getMessageProperties().getMessageId(),
                        payload
                );
            }

            // 这里可以执行批量入库、批量调用或批量聚合处理
            handleBatch(messages);

            // multiple=true 表示确认当前 deliveryTag 及之前未确认消息
            channel.basicAck(lastDeliveryTag, true);
            log.info("批量消息确认成功，batchSize={}，lastDeliveryTag={}", messages.size(), lastDeliveryTag);
        } catch (Exception ex) {
            log.error("批量消息消费失败，batchSize={}，lastDeliveryTag={}，原因={}",
                    messages.size(),
                    lastDeliveryTag,
                    ex.getMessage(),
                    ex
            );

            // 批量失败后不重新入队，建议进入死信或人工补偿
            channel.basicNack(lastDeliveryTag, true, false);
        }
    }

    /**
     * 批量处理消息
     *
     * @param messages 消息列表
     */
    private void handleBatch(List<Message> messages) {
        log.info("执行批量业务处理，batchSize={}", messages.size());
    }
}
```

批量消费建议：

1. 批量大小不宜过大，常见为 20、50、100。
2. 批量消费必须考虑部分成功问题。
3. 批量 Ack 使用 `multiple=true` 前要确认不会误确认未处理消息。
4. 批量失败后建议进入死信或记录失败明细。
5. 核心资金类消息不建议批量确认。
6. 批量消费更适合日志、报表、统计、通知等场景。

### 临时扩容消费者

临时扩容消费者用于快速处理突发消息堆积。扩容方式包括增加应用实例、提高消费者线程数、临时启动专用消费服务、降低非关键逻辑耗时等。

常见扩容方式如下：

| 扩容方式       | 说明                                     |
| -------------- | ---------------------------------------- |
| 增加应用实例   | Kubernetes、Docker、虚拟机横向扩容       |
| 提高并发线程   | 调整 `concurrency` 和 `max-concurrency`  |
| 临时消费者服务 | 启动只消费堆积队列的临时服务             |
| 禁用非关键逻辑 | 临时关闭短信、日志、远程调用等非核心处理 |
| 批量处理       | 对可批量业务启用批量消费                 |
| 分片队列       | 将单队列拆分为多队列并行消费             |

Kubernetes 临时扩容示例：

```bash
# 将消费者服务扩容到 6 个副本
kubectl scale deployment order-consumer --replicas=6 -n business

# 查看消费者 Pod 状态
kubectl get pod -n business -l app=order-consumer

# 堆积处理完成后恢复到 2 个副本
kubectl scale deployment order-consumer --replicas=2 -n business
```

命令说明：`kubectl scale` 用于调整 Deployment 副本数，`--replicas` 指定目标副本数量，`-n` 指定命名空间。临时扩容前应确认数据库、Redis 和第三方接口能够承受额外压力。

Docker Compose 本地扩容示例：

```bash
# 扩容消费者服务到 3 个实例
docker compose up -d --scale order-consumer=3

# 查看服务实例
docker compose ps
```

临时扩容注意事项：

1. 扩容前先确认队列是否支持并发消费。
2. 顺序消费队列不能盲目扩容消费者。
3. 扩容前检查下游数据库连接池和限流配置。
4. 扩容后观察消费速率、错误率、数据库负载和第三方接口响应时间。
5. 堆积处理完成后应恢复合理副本数，避免长期资源浪费。
6. 如果扩容后消费速率没有提升，瓶颈通常在业务逻辑或下游服务。

### 消息过期策略

消息过期策略用于控制消息在队列中的最大存活时间，避免已经失去业务价值的消息长期堆积。过期消息如果配置了死信交换机，可以进入死信队列；否则可能被直接丢弃。

常见过期策略如下：

| 策略         | 说明                             |
| ------------ | -------------------------------- |
| 队列 TTL     | 队列中所有消息使用相同过期时间   |
| 消息 TTL     | 每条消息单独设置过期时间         |
| 最大队列长度 | 超过长度后旧消息进入死信或被丢弃 |
| 最大队列容量 | 超过字节限制后触发淘汰           |
| 死信兜底     | 过期或淘汰消息进入死信队列       |

队列 TTL 配置示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/ExpireQueueConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 消息过期队列配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class ExpireQueueConfig {

    public static final String EXPIRE_QUEUE = "expire.message.queue";

    public static final String DEAD_EXCHANGE = "expire.dead.exchange";

    public static final String DEAD_ROUTING_KEY = "expire.dead";

    /**
     * 声明带过期策略的队列
     *
     * @return 队列
     */
    @Bean
    public Queue expireMessageQueue() {
        return QueueBuilder
                .durable(EXPIRE_QUEUE)
                // 消息在队列中最多保留 10 分钟
                .ttl(10 * 60 * 1000)
                // 队列最多保留 100000 条消息
                .maxLength(100_000)
                // 过期或超长消息进入死信交换机
                .deadLetterExchange(DEAD_EXCHANGE)
                .deadLetterRoutingKey(DEAD_ROUTING_KEY)
                .build();
    }
}
```

单条消息 TTL 设置示例：

```java
rabbitTemplate.convertAndSend(
        exchange,
        routingKey,
        payload,
        message -> {
            // 单条消息 5 分钟后过期，单位为毫秒字符串
            message.getMessageProperties().setExpiration("300000");
            return message;
        }
);
```

消息过期策略建议：

| 消息类型     | 过期建议                       |
| ------------ | ------------------------------ |
| 支付状态消息 | 不建议短 TTL，核心业务需要补偿 |
| 库存扣减消息 | 不建议直接过期丢弃             |
| 短信通知消息 | 可设置合理 TTL，例如 30 分钟   |
| 日志采集消息 | 可设置短 TTL，避免无价值堆积   |
| 实时推送消息 | 可设置较短 TTL                 |
| 延迟任务消息 | 根据业务延迟时间设置 TTL       |

核心原则：过期不是可靠性方案。对于核心业务消息，即使过期，也应进入死信队列或补偿流程，而不是静默丢弃。

### 堆积监控指标

堆积监控用于提前发现队列压力。生产环境应至少监控队列深度、消息速率、消费者数量、未确认消息、消费耗时和死信数量。

关键监控指标如下：

| 指标                      | 说明               | 风险信号                        |
| ------------------------- | ------------------ | ------------------------------- |
| `messages_ready`          | 等待消费的消息数   | 持续增长表示堆积                |
| `messages_unacknowledged` | 已投递未确认消息数 | 持续过高表示消费者慢或 Ack 异常 |
| `messages`                | 队列总消息数       | 总量持续升高表示处理能力不足    |
| `consumers`               | 消费者数量         | 为 0 表示无人消费               |
| publish rate              | 生产速率           | 高于消费速率会堆积              |
| deliver / ack rate        | 消费确认速率       | 低于生产速率会堆积              |
| redeliver rate            | 重投递速率         | 高说明消费失败或 Ack 异常       |
| dead-letter count         | 死信数量           | 增长说明消费失败或过期          |
| queue memory              | 队列内存占用       | 过高可能影响 Broker             |
| disk free                 | 磁盘剩余空间       | 过低可能触发 Broker 保护机制    |

RabbitMQ 命令查看队列状态：

```bash
# 查看队列消息、Ready、Unacked、消费者数量
rabbitmqctl list_queues -p /dev name messages messages_ready messages_unacknowledged consumers

# 查看队列内存和状态
rabbitmqctl list_queues -p /dev name state memory messages_ready messages_unacknowledged

# 查看连接数量
rabbitmqctl list_connections name user state channels

# 查看 Channel 信息
rabbitmqctl list_channels connection number user messages_unacknowledged
```

命令说明：`messages_ready` 表示等待消费数量，`messages_unacknowledged` 表示已经投递但未确认数量，`consumers` 表示消费者数量。排查堆积时应优先观察这三个指标。

告警建议：

| 指标           | 告警条件示例                |
| -------------- | --------------------------- |
| Ready 消息数   | 连续 5 分钟大于 10000       |
| Unacked 消息数 | 连续 5 分钟大于 5000        |
| 消费者数量     | 核心队列 Consumers = 0      |
| 生产消费差值   | 生产速率持续大于消费速率    |
| 死信数量       | 死信队列消息数持续增长      |
| 队列内存       | 队列内存超过阈值            |
| 磁盘空间       | Broker 磁盘剩余空间低于阈值 |

堆积处理优先级建议：

1. 先确认消费者是否在线。
2. 再确认消费者是否大量异常。
3. 再确认下游依赖是否变慢。
4. 再评估是否需要扩容消费者。
5. 最后考虑队列拆分、批量消费和架构优化。

## 消费并发配置

本章节用于说明 Spring AMQP 中消费者并发相关参数。合理的并发配置可以提升消费吞吐量，不合理的配置会导致消息分配不均、下游压力过大、顺序性被破坏、重复消费风险增加。

### concurrentConsumers

`concurrentConsumers` 表示监听容器启动时创建的初始消费者数量。在 Spring Boot 配置中通常对应 `spring.rabbitmq.listener.simple.concurrency`。

配置示例：

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        # 初始消费者数量
        concurrency: 4
```

Java 配置示例：

```java
factory.setConcurrentConsumers(4);
```

参数含义：

| 参数   | 说明                                     |
| ------ | ---------------------------------------- |
| 值较小 | 消费能力弱，但资源占用低                 |
| 值较大 | 消费能力强，但会增加线程、连接、下游压力 |
| 等于 1 | 适合顺序消费或低流量场景                 |
| 大于 1 | 适合可并发处理的普通消息                 |

配置建议：

1. 普通业务可从 `2` 到 `4` 开始。
2. IO 密集型任务可以适当提高。
3. CPU 密集型任务不宜超过 CPU 核心数过多。
4. 顺序消费队列应设置为 `1`。
5. 多实例部署时，总消费者数等于实例数乘以单实例消费者数。

### maxConcurrentConsumers

`maxConcurrentConsumers` 表示监听容器可扩展到的最大消费者数量。在 Spring Boot 配置中对应 `spring.rabbitmq.listener.simple.max-concurrency`。

配置示例：

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        # 初始消费者数量
        concurrency: 4
        # 最大消费者数量
        max-concurrency: 16
```

Java 配置示例：

```java
factory.setConcurrentConsumers(4);
factory.setMaxConcurrentConsumers(16);
```

参数说明：

| 参数              | 说明                                   |
| ----------------- | -------------------------------------- |
| `concurrency`     | 初始消费者数量                         |
| `max-concurrency` | 最大消费者数量                         |
| 动态扩容          | 消息堆积或负载变化时，容器可增加消费者 |
| 动态缩容          | 空闲后可逐步减少消费者                 |

配置建议：

1. `max-concurrency` 必须大于或等于 `concurrency`。
2. 高峰流量明显的队列可以设置较高上限。
3. 下游服务容量不足时，不要盲目提高该值。
4. 如果需要稳定固定并发，可以让 `concurrency` 和 `max-concurrency` 相等。
5. 顺序消费场景中，两者都应设置为 `1`。

### prefetchCount

`prefetchCount` 表示每个消费者一次最多可以持有多少条未确认消息。在 Spring Boot 配置中对应 `spring.rabbitmq.listener.simple.prefetch`。

配置示例：

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        # 每个消费者最多持有 10 条未确认消息
        prefetch: 10
```

Java 配置示例：

```java
factory.setPrefetchCount(10);
```

`prefetchCount` 对消费行为影响很大：

| 值     | 影响                                 |
| ------ | ------------------------------------ |
| `1`    | 公平分发，适合耗时任务和顺序消费     |
| 较小值 | 降低单个消费者堆积，失败恢复快       |
| 较大值 | 提高吞吐，但可能导致消息分配不均     |
| 过大值 | Unacked 增高，消费者宕机后恢复成本高 |

示例：

```text
消费者数量：10
prefetchCount：20
最大 Unacked 理论值：10 × 20 = 200
```

如果单条消息处理很慢，而 `prefetch=100`，某个消费者可能提前拿到大量消息，其他消费者空闲时也无法处理这些已分配但未确认的消息，从而导致消费不均。

配置建议：

| 场景         | 推荐 prefetch        |
| ------------ | -------------------- |
| 严格顺序消费 | 1                    |
| 耗时任务     | 1 到 5               |
| 普通业务消息 | 5 到 20              |
| 快速轻量消息 | 20 到 100            |
| 批量消费     | 大于或等于 batchSize |
| 消费者不稳定 | 不宜过大             |

### batchSize

`batchSize` 表示监听容器批量处理消息时的批次大小。它适合批量写入、批量提交、批量聚合等场景。使用批量消费时，还需要启用批量监听能力。

配置示例：

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        # 批量大小
        batch-size: 50
        # prefetch 应大于或等于 batch-size
        prefetch: 100
```

Java 配置示例：

```java
factory.setConsumerBatchEnabled(true);
factory.setBatchListener(true);
factory.setBatchSize(50);
factory.setPrefetchCount(100);
```

批量参数关系：

| 参数                   | 说明                                       |
| ---------------------- | ------------------------------------------ |
| `batchSize`            | 单批消息数量                               |
| `prefetchCount`        | 消费者预取数量，通常应大于等于 `batchSize` |
| `consumerBatchEnabled` | 是否启用消费者批量接收                     |
| `batchListener`        | Listener 是否以批量方式接收                |

配置建议：

1. `batchSize` 不宜过大，避免单批失败影响过多消息。
2. `prefetchCount` 应大于或等于 `batchSize`。
3. 批量消费必须设计部分失败处理策略。
4. 批量 Ack 要谨慎使用 `multiple=true`。
5. 核心资金类消息不建议批量消费。
6. 批量消费适合日志、统计、通知、数据同步等场景。

### acknowledgeMode

`acknowledgeMode` 表示消费者确认模式。在 Spring Boot 配置中对应 `spring.rabbitmq.listener.simple.acknowledge-mode`。

配置示例：

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        # 消费确认模式
        acknowledge-mode: manual
```

确认模式对比：

| 模式     | 说明                         | 适用场景           |
| -------- | ---------------------------- | ------------------ |
| `none`   | Broker 不等待消费者确认      | 可丢失的低价值消息 |
| `auto`   | 容器根据方法执行结果自动确认 | 普通业务           |
| `manual` | 业务代码手动 Ack/Nack        | 核心业务           |

手动确认示例：

```java
try {
    handleBusiness(payload);
    channel.basicAck(deliveryTag, false);
} catch (Exception ex) {
    log.error("消息消费失败，deliveryTag={}，原因={}", deliveryTag, ex.getMessage(), ex);
    channel.basicNack(deliveryTag, false, false);
}
```

配置建议：

1. 订单、支付、库存、积分等核心业务使用 `manual`。
2. 普通通知、轻量异步任务可以使用 `auto`。
3. 可接受丢失的临时日志可以使用 `none`。
4. 使用 `manual` 时必须保证所有分支都能 Ack、Nack 或 Reject。
5. 不要在业务处理成功前 Ack。
6. 消费异常时建议结合死信队列处理。

### 并发参数调优

并发参数调优需要结合消息生产速率、消费耗时、消费者实例数、下游容量和业务顺序要求进行。不能只通过提高线程数解决所有堆积问题。

调优流程建议：

```text
观察队列指标
    -> 判断 Ready / Unacked / Consumers
    -> 分析消费耗时和异常率
    -> 评估下游容量
    -> 调整 concurrency / max-concurrency / prefetch
    -> 压测验证
    -> 持续监控
```

推荐初始配置：

| 场景           | concurrency | max-concurrency | prefetch     | acknowledge-mode |
| -------------- | ----------- | --------------- | ------------ | ---------------- |
| 顺序消费       | 1           | 1               | 1            | manual           |
| 普通业务       | 2 到 4      | 8 到 16         | 5 到 20      | manual           |
| IO 密集型任务  | 4 到 8      | 16 到 32        | 10 到 50     | manual           |
| 日志采集       | 4 到 8      | 16 到 32        | 50 到 100    | auto / manual    |
| 批量消费       | 2 到 4      | 8 到 16         | >= batchSize | manual           |
| 第三方接口调用 | 2 到 4      | 4 到 8          | 1 到 5       | manual           |

调优判断方法：

| 现象                           | 可能原因               | 调整建议                   |
| ------------------------------ | ---------------------- | -------------------------- |
| Ready 持续增长，Consumers 正常 | 消费能力不足           | 提高并发或扩容实例         |
| Unacked 很高                   | 消费慢或 prefetch 过大 | 降低 prefetch，排查耗时    |
| Consumers 为 0                 | 消费者未启动           | 检查应用和监听配置         |
| 消费异常率高                   | 业务处理失败           | 先修复异常，不要盲目扩容   |
| CPU 很高                       | 计算瓶颈               | 降低并发或优化代码         |
| 数据库压力高                   | 下游瓶颈               | 限制并发，优化数据库       |
| 消费速率提升不明显             | 非线程瓶颈             | 排查锁、接口、数据库、网络 |

完整调优配置示例：

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        # 核心业务使用手动确认
        acknowledge-mode: manual
        # 平峰消费者数量
        concurrency: 4
        # 高峰最大消费者数量
        max-concurrency: 16
        # 每个消费者预取 10 条，避免 Unacked 过高
        prefetch: 10
        # 消费异常不默认重新入队，配合死信队列处理
        default-requeue-rejected: false
        retry:
          # 开启有限重试
          enabled: true
          # 最多尝试 3 次
          max-attempts: 3
          # 初始重试间隔
          initial-interval: 1s
          # 最大重试间隔
          max-interval: 10s
          # 指数退避倍率
          multiplier: 2
```

并发调优建议：

1. 先看指标，再调参数，不要凭经验直接改大。
2. 优先确认消费者没有异常和阻塞。
3. 提高并发前先评估数据库、Redis、第三方接口容量。
4. `prefetch` 不宜过大，尤其是耗时任务。
5. 顺序消息不要通过提高同一队列并发来加速。
6. 堆积严重时可以临时扩容，但后续应分析根因。
7. 长期堆积应考虑队列拆分、业务削峰、批量处理、异步降级或架构调整。


## 批量消息处理

本章节用于说明 RabbitMQ 批量消息的发送、消费、确认、异常处理和适用场景。批量处理的目标是降低单条消息逐条处理带来的网络、序列化、数据库写入和业务调用开销，但批量处理会增加失败处理、幂等控制、部分成功和补偿复杂度。

### 批量发送

批量发送是指生产者一次处理多条业务数据，并连续发送多条 MQ 消息。RabbitMQ 本身通常仍然是一条一条发布消息，批量发送更多是业务层封装批量循环、统一记录日志、统一生成消息 ID、统一处理失败结果。

批量发送适合以下场景：

| 场景     | 说明                             |
| -------- | -------------------------------- |
| 批量通知 | 批量发送短信、邮件、站内信任务   |
| 数据同步 | 批量同步用户、订单、库存变更事件 |
| 日志采集 | 批量发送操作日志、审计日志       |
| 任务分发 | 批量投递异步任务                 |
| 补偿重发 | 扫描本地消息表后批量重发失败消息 |

批量发送结果对象如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/dto/BatchSendResult.java`

```java
package io.github.atengk.rabbitmq.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 批量消息发送结果
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
public class BatchSendResult {

    /**
     * 总数量
     */
    private Integer total;

    /**
     * 成功数量
     */
    private Integer successCount;

    /**
     * 失败数量
     */
    private Integer failedCount;

    /**
     * 成功消息ID列表
     */
    private List<String> successMessageIds;

    /**
     * 失败消息列表
     */
    private List<String> failedMessages;
}
```

下面的代码封装了批量发送逻辑，逐条生成消息 ID、设置消息持久化属性，并记录每条发送结果。

文件位置：`src/main/java/io/github/atengk/rabbitmq/producer/BatchMessageProducer.java`

```java
package io.github.atengk.rabbitmq.producer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.rabbitmq.dto.BatchSendResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量消息生产者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 批量发送消息
     *
     * @param exchange    交换机
     * @param routingKey  路由键
     * @param businessKey 业务主键
     * @param payloadList 消息体列表
     * @return 批量发送结果
     */
    public BatchSendResult batchSend(String exchange, String routingKey, String businessKey, List<?> payloadList) {
        if (CollUtil.isEmpty(payloadList)) {
            return BatchSendResult.builder()
                    .total(0)
                    .successCount(0)
                    .failedCount(0)
                    .successMessageIds(new ArrayList<>())
                    .failedMessages(new ArrayList<>())
                    .build();
        }

        List<String> successMessageIds = new ArrayList<>();
        List<String> failedMessages = new ArrayList<>();

        for (Object payload : payloadList) {
            String messageId = StrUtil.format("batch_{}_{}", DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss"), IdUtil.fastSimpleUUID());

            try {
                rabbitTemplate.convertAndSend(
                        exchange,
                        routingKey,
                        payload,
                        message -> {
                            message.getMessageProperties().setMessageId(messageId);
                            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                            message.getMessageProperties().setTimestamp(DateUtil.date());
                            message.getMessageProperties().setHeader("x-message-id", messageId);
                            message.getMessageProperties().setHeader("x-business-key", businessKey);
                            message.getMessageProperties().setHeader("x-send-time", DateUtil.now());
                            return message;
                        },
                        new CorrelationData(messageId)
                );

                successMessageIds.add(messageId);
                log.info("批量消息发送成功，messageId={}，exchange={}，routingKey={}", messageId, exchange, routingKey);
            } catch (Exception ex) {
                String errorMessage = StrUtil.format("messageId={}，原因={}", messageId, ex.getMessage());
                failedMessages.add(errorMessage);

                log.error("批量消息发送失败，messageId={}，exchange={}，routingKey={}，原因={}",
                        messageId,
                        exchange,
                        routingKey,
                        ex.getMessage(),
                        ex
                );
            }
        }

        return BatchSendResult.builder()
                .total(payloadList.size())
                .successCount(successMessageIds.size())
                .failedCount(failedMessages.size())
                .successMessageIds(successMessageIds)
                .failedMessages(failedMessages)
                .build();
    }
}
```

批量发送注意事项：

1. 批量发送中某一条失败，不应影响其他消息继续发送，除非业务要求整体失败。
2. 核心业务批量发送前建议先写入本地消息表。
3. 批量发送仍要为每条消息生成独立 `messageId`。
4. Publisher Confirm 回调也是按消息维度确认，不应只记录批次维度。
5. 大批量发送应控制批次大小，例如每批 100、500 或 1000 条，避免瞬时压垮 Broker。
6. 如果批量消息需要严格事务一致性，应优先使用本地消息表或 Outbox Pattern，而不是依赖 RabbitMQ 原生事务。

### 批量消费

批量消费是指消费者一次接收多条消息，并在业务层批量处理。例如批量写数据库、批量写日志、批量调用下游接口等。批量消费可以提升吞吐量，但会增加部分失败和批量确认的复杂度。

批量监听容器配置如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/BatchRabbitListenerConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 批量监听配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class BatchRabbitListenerConfig {

    /**
     * 批量消息监听容器工厂
     *
     * @param configurer        Spring Boot 监听容器配置器
     * @param connectionFactory RabbitMQ 连接工厂
     * @return 批量监听容器工厂
     */
    @Bean
    public SimpleRabbitListenerContainerFactory batchRabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);

        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConsumerBatchEnabled(true);
        factory.setBatchListener(true);
        factory.setBatchSize(50);
        factory.setPrefetchCount(100);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(8);
        factory.setDefaultRequeueRejected(false);

        return factory;
    }
}
```

批量消费者示例如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/consumer/BatchBusinessConsumer.java`

```java
package io.github.atengk.rabbitmq.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 批量业务消息消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class BatchBusinessConsumer {

    /**
     * 批量消费业务消息
     *
     * @param messages 消息列表
     * @param channel  RabbitMQ Channel
     * @throws IOException Ack 或 Nack 失败时抛出
     */
    @RabbitListener(
            queues = "batch.business.queue",
            containerFactory = "batchRabbitListenerContainerFactory"
    )
    public void consumeBatch(List<Message> messages, Channel channel) throws IOException {
        if (CollUtil.isEmpty(messages)) {
            return;
        }

        long lastDeliveryTag = messages.get(messages.size() - 1).getMessageProperties().getDeliveryTag();

        try {
            log.info("收到批量业务消息，batchSize={}，lastDeliveryTag={}", messages.size(), lastDeliveryTag);

            for (Message message : messages) {
                String messageId = StrUtil.blankToDefault(message.getMessageProperties().getMessageId(), "unknown");
                String payload = new String(message.getBody(), StandardCharsets.UTF_8);

                log.info("准备处理批量消息，messageId={}，payload={}", messageId, payload);
            }

            handleBatch(messages);

            channel.basicAck(lastDeliveryTag, true);
            log.info("批量业务消息消费成功，batchSize={}，lastDeliveryTag={}", messages.size(), lastDeliveryTag);
        } catch (Exception ex) {
            log.error("批量业务消息消费失败，batchSize={}，lastDeliveryTag={}，原因={}",
                    messages.size(),
                    lastDeliveryTag,
                    ex.getMessage(),
                    ex
            );

            channel.basicNack(lastDeliveryTag, true, false);
        }
    }

    /**
     * 批量处理消息
     *
     * @param messages 消息列表
     */
    private void handleBatch(List<Message> messages) {
        log.info("执行批量业务处理，batchSize={}", messages.size());

        // 示例：批量解析、批量入库、批量调用接口
    }
}
```

批量消费建议：

1. 批量消费的消息最好业务类型一致。
2. 批量消费前应评估单批最大处理时间。
3. `prefetchCount` 应大于或等于 `batchSize`。
4. 批量消费需要考虑单条失败对整批的影响。
5. 核心交易类消息不建议直接使用批量消费。
6. 批量消费更适合日志、通知、统计、同步任务等场景。

### 批量确认

批量确认是指消费者处理完一批消息后，通过 `basicAck(deliveryTag, true)` 一次确认当前 `deliveryTag` 及之前所有未确认消息。它可以减少 Ack 调用次数，但也存在误确认风险。

批量确认示例：

```java
long lastDeliveryTag = messages.get(messages.size() - 1).getMessageProperties().getDeliveryTag();

// multiple=true 表示确认当前 deliveryTag 及之前所有未确认消息
channel.basicAck(lastDeliveryTag, true);
```

批量拒绝示例：

```java
long lastDeliveryTag = messages.get(messages.size() - 1).getMessageProperties().getDeliveryTag();

// multiple=true 且 requeue=false，表示批量拒绝并不重新入队
channel.basicNack(lastDeliveryTag, true, false);
```

批量确认参数说明：

| 方法                                  | 说明                                      |
| ------------------------------------- | ----------------------------------------- |
| `basicAck(deliveryTag, true)`         | 批量确认当前 deliveryTag 及之前未确认消息 |
| `basicAck(deliveryTag, false)`        | 只确认当前消息                            |
| `basicNack(deliveryTag, true, false)` | 批量拒绝且不重新入队                      |
| `basicNack(deliveryTag, true, true)`  | 批量拒绝并重新入队                        |

批量确认风险：

| 风险         | 说明                                      |
| ------------ | ----------------------------------------- |
| 误确认       | 当前 Channel 上较早未处理消息也可能被确认 |
| 部分失败复杂 | 批内部分成功、部分失败时难以精确 Ack      |
| 重试粒度变粗 | 失败时可能整批进入死信                    |
| 幂等要求更高 | 重发整批时已成功消息可能重复处理          |
| 排查复杂     | 失败日志需要记录批次内每条消息            |

批量确认建议：

1. 只有确定当前 Channel 上前序消息均已成功处理时，才使用 `multiple=true`。
2. 对核心业务，优先使用单条 Ack，降低误确认风险。
3. 批量失败时要记录批次内所有 `messageId`。
4. 批量处理的业务逻辑必须支持幂等。
5. 批量消息进入死信后，管理端需要支持按单条消息拆分处理或整批重发。

### 批量异常处理

批量异常处理的核心问题是：一批消息中只有部分消息失败时，应该如何处理。不同业务场景可以采用不同策略。

常见策略如下：

| 策略             | 说明                             | 适用场景         |
| ---------------- | -------------------------------- | ---------------- |
| 整批成功才 Ack   | 任意一条失败则整批失败           | 数据一致性要求高 |
| 单条处理单条记录 | 每条消息独立处理，失败单独记录   | 通知、日志、任务 |
| 部分成功落库     | 成功的记录状态，失败的进入失败表 | 数据同步         |
| 整批进入死信     | 简单粗暴，但重发成本高           | 低频批量任务     |
| 拆分重试         | 失败后缩小批次重新处理           | 大批量数据同步   |

批量异常处理示例，逐条处理并记录失败明细。

文件位置：`src/main/java/io/github/atengk/rabbitmq/consumer/BatchPartialFailureConsumer.java`

```java
package io.github.atengk.rabbitmq.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量部分失败消息消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class BatchPartialFailureConsumer {

    /**
     * 批量消费并记录部分失败
     *
     * @param messages 消息列表
     * @param channel  RabbitMQ Channel
     * @throws IOException Ack 或 Nack 失败时抛出
     */
    @RabbitListener(
            queues = "batch.partial.queue",
            containerFactory = "batchRabbitListenerContainerFactory"
    )
    public void consume(List<Message> messages, Channel channel) throws IOException {
        if (CollUtil.isEmpty(messages)) {
            return;
        }

        long lastDeliveryTag = messages.get(messages.size() - 1).getMessageProperties().getDeliveryTag();
        List<String> failedMessageIds = new ArrayList<>();

        for (Message message : messages) {
            String messageId = StrUtil.blankToDefault(message.getMessageProperties().getMessageId(), "unknown");
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);

            try {
                handleSingleMessage(messageId, payload);
            } catch (Exception ex) {
                failedMessageIds.add(messageId);
                log.error("批量消息单条处理失败，messageId={}，payload={}，原因={}",
                        messageId,
                        payload,
                        ex.getMessage(),
                        ex
                );

                // 生产环境建议将失败消息写入失败表或死信处理表
                recordFailedMessage(messageId, payload, ex.getMessage());
            }
        }

        if (CollUtil.isEmpty(failedMessageIds)) {
            channel.basicAck(lastDeliveryTag, true);
            log.info("批量消息全部处理成功，batchSize={}", messages.size());
            return;
        }

        // 已经逐条记录失败明细，避免整批反复重试，这里 Ack 当前批次
        channel.basicAck(lastDeliveryTag, true);
        log.warn("批量消息存在部分失败，batchSize={}，failedMessageIds={}", messages.size(), failedMessageIds);
    }

    /**
     * 处理单条消息
     *
     * @param messageId 消息ID
     * @param payload   消息体
     */
    private void handleSingleMessage(String messageId, String payload) {
        if (StrUtil.isBlank(payload)) {
            throw new IllegalArgumentException("消息体不能为空");
        }

        log.info("处理批量中的单条消息，messageId={}", messageId);
    }

    /**
     * 记录失败消息
     *
     * @param messageId 消息ID
     * @param payload   消息体
     * @param reason    失败原因
     */
    private void recordFailedMessage(String messageId, String payload, String reason) {
        log.warn("记录批量失败消息，messageId={}，reason={}", messageId, reason);
    }
}
```

这种方式适合“允许部分成功”的场景。对于强一致业务，不建议在部分失败后直接 Ack 整批消息，而应整批失败后进入死信或补偿流程。

批量异常处理建议：

1. 允许部分成功的业务，需要失败明细表。
2. 不允许部分成功的业务，应整批失败并进入补偿流程。
3. 批量异常日志必须包含每条失败消息的 `messageId`。
4. 批量重发时必须依赖幂等控制，避免已成功消息重复处理。
5. 批量大小越大，异常处理越复杂，不应盲目调大 `batchSize`。

### 批量处理适用场景

批量处理适合吞吐量要求高、单条消息处理成本高、允许批量提交或允许部分成功的场景。不适合强事务、强顺序、强实时和资金类核心链路。

适用场景如下：

| 场景         | 是否适合批量       | 说明                     |
| ------------ | ------------------ | ------------------------ |
| 操作日志采集 | 适合               | 批量写入效率高           |
| 审计日志     | 适合，但需可靠记录 | 可批量入库               |
| 短信通知任务 | 适合               | 可批量提交短信平台       |
| 邮件发送任务 | 适合               | 可批量发送或批量入队     |
| 用户行为上报 | 适合               | 可批量聚合               |
| 报表数据同步 | 适合               | 可批量写入               |
| 订单支付消息 | 不建议             | 单条失败需要精确处理     |
| 库存扣减消息 | 不建议             | 幂等和一致性要求高       |
| 账户资金流水 | 不建议             | 顺序和事务要求高         |
| 严格顺序消息 | 不建议             | 批量会增加顺序控制复杂度 |

批量处理选型建议：

1. 优先用于日志、统计、通知、同步、报表等非交易核心链路。
2. 批量处理必须配套幂等设计。
3. 批量处理必须定义部分失败策略。
4. 批量大小应通过压测确定。
5. 不要为了追求吞吐量牺牲核心业务可靠性。

## 事务消息设计

本章节用于说明 RabbitMQ 与数据库事务之间的一致性设计。RabbitMQ 原生事务可以保证单个 Channel 上的消息发布事务，但性能较差，且无法直接和业务数据库事务组成真正的分布式事务。实际项目中，更推荐使用本地消息表、Outbox Pattern、可靠消息投递和最终一致性方案。

### RabbitMQ 事务机制

RabbitMQ 原生事务通过 AMQP Channel 的 `txSelect`、`txCommit`、`txRollback` 实现。生产者开启事务后，当前 Channel 上的消息发布可以提交或回滚。

事务发送示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/producer/RabbitTransactionProducer.java`

```java
package io.github.atengk.rabbitmq.producer;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * RabbitMQ 原生事务消息生产者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitTransactionProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 使用 RabbitMQ Channel 事务发送消息
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param payload    消息体
     */
    public void sendInRabbitTransaction(String exchange, String routingKey, String payload) {
        rabbitTemplate.execute((ChannelCallback<Void>) channel -> {
            String messageId = StrUtil.format("tx_{}_{}", DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss"), IdUtil.fastSimpleUUID());

            try {
                channel.txSelect();

                AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                        .messageId(messageId)
                        .deliveryMode(2)
                        .timestamp(DateUtil.date())
                        .contentType("application/json")
                        .build();

                channel.basicPublish(
                        exchange,
                        routingKey,
                        true,
                        properties,
                        payload.getBytes(StandardCharsets.UTF_8)
                );

                channel.txCommit();
                log.info("RabbitMQ事务消息提交成功，messageId={}，exchange={}，routingKey={}", messageId, exchange, routingKey);
                return null;
            } catch (Exception ex) {
                rollback(channel);
                log.error("RabbitMQ事务消息发送失败，已回滚，exchange={}，routingKey={}，原因={}",
                        exchange,
                        routingKey,
                        ex.getMessage(),
                        ex
                );
                throw ex;
            }
        });
    }

    /**
     * 回滚 RabbitMQ 事务
     *
     * @param channel RabbitMQ Channel
     */
    private void rollback(Channel channel) {
        try {
            channel.txRollback();
        } catch (Exception rollbackException) {
            log.error("RabbitMQ事务回滚失败，原因={}", rollbackException.getMessage(), rollbackException);
        }
    }
}
```

RabbitMQ 原生事务特点：

| 特点     | 说明                             |
| -------- | -------------------------------- |
| 作用范围 | 只作用于当前 RabbitMQ Channel    |
| 能力     | 支持发布消息提交和回滚           |
| 性能     | 性能明显低于 Publisher Confirm   |
| 局限     | 不能直接和数据库事务形成原子提交 |
| 推荐程度 | 生产业务中一般不推荐             |

不推荐将 RabbitMQ 原生事务作为业务事务消息的主要方案。多数业务场景中，建议使用 Publisher Confirm + 本地消息表或 Outbox Pattern，以更高性能和更强可补偿性实现最终一致性。

### 数据库事务与消息发送

数据库事务与消息发送的核心问题是：业务数据写入数据库和消息发送到 RabbitMQ 不是同一个事务资源，无法天然保证原子性。

常见异常场景如下：

| 场景                               | 后果                           |
| ---------------------------------- | ------------------------------ |
| 数据库提交成功，消息发送失败       | 下游系统无法收到事件           |
| 消息发送成功，数据库回滚           | 消费者收到不存在或无效业务数据 |
| 消息发送成功，Confirm 失败         | 生产者可能重复发送             |
| 数据库提交成功，应用宕机未发送消息 | 消息丢失                       |
| 消费者已处理，生产者补偿重发       | 可能重复消费                   |

错误示例：

```java
@Transactional(rollbackFor = Exception.class)
public void createOrder(OrderCreateRequest request) {
    saveOrder(request);

    // 不推荐：数据库事务还未提交时直接发送 MQ
    rabbitTemplate.convertAndSend("order.exchange", "order.created", request);
}
```

该写法存在风险：如果消息发送成功后数据库事务回滚，消费者可能收到一条数据库中不存在的订单消息。如果数据库提交成功但发送失败，下游系统又无法收到订单创建事件。

更安全的思路是：

```text
数据库事务内：
    -> 写业务表
    -> 写本地消息表，状态 PENDING
事务提交后：
    -> 异步发送 MQ
    -> Confirm 成功后更新消息状态
    -> 失败后由补偿任务重发
```

### 本地消息表方案

本地消息表方案用于解决业务数据库事务和 MQ 发送之间的一致性问题。它将“业务数据”和“待发送消息”写入同一个数据库事务中。只要数据库事务提交成功，就一定有一条待发送消息记录，后续由异步任务发送 MQ。

消息表设计如下。

```sql
CREATE TABLE mq_local_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(100) NOT NULL COMMENT '消息ID',
    event_type VARCHAR(64) NOT NULL COMMENT '事件类型',
    business_key VARCHAR(100) NOT NULL COMMENT '业务主键',
    exchange_name VARCHAR(128) NOT NULL COMMENT '交换机',
    routing_key VARCHAR(128) NOT NULL COMMENT '路由键',
    message_body TEXT NOT NULL COMMENT '消息体',
    message_status VARCHAR(32) NOT NULL COMMENT '消息状态',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    max_retry_count INT NOT NULL DEFAULT 5 COMMENT '最大重试次数',
    next_retry_time DATETIME DEFAULT NULL COMMENT '下次重试时间',
    error_message VARCHAR(500) DEFAULT NULL COMMENT '异常信息',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_status_retry_time (message_status, next_retry_time),
    KEY idx_business_key (business_key)
) COMMENT='本地MQ消息表';
```

消息状态建议：

| 状态           | 说明             |
| -------------- | ---------------- |
| `PENDING`      | 待发送           |
| `SENDING`      | 发送中           |
| `SENT`         | 已提交发送       |
| `CONFIRMED`    | Broker 已确认    |
| `FAILED`       | 发送失败         |
| `RETRY_FAILED` | 超过最大重试次数 |
| `RETURNED`     | 消息不可路由     |

本地消息 DTO 示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/dto/LocalMessageCreateRequest.java`

```java
package io.github.atengk.rabbitmq.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

/**
 * 本地消息创建请求
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
public class LocalMessageCreateRequest {

    @NotBlank(message = "消息ID不能为空")
    private String messageId;

    @NotBlank(message = "事件类型不能为空")
    private String eventType;

    @NotBlank(message = "业务主键不能为空")
    private String businessKey;

    @NotBlank(message = "交换机不能为空")
    private String exchangeName;

    @NotBlank(message = "路由键不能为空")
    private String routingKey;

    @NotBlank(message = "消息体不能为空")
    private String messageBody;
}
```

业务事务中写入业务数据和本地消息。

文件位置：`src/main/java/io/github/atengk/rabbitmq/service/OrderCreateService.java`

```java
package io.github.atengk.rabbitmq.service;

import io.github.atengk.rabbitmq.dto.LocalMessageCreateRequest;

/**
 * 订单创建服务
 *
 * @author Ateng
 * @since 2026-05-11
 */
public interface OrderCreateService {

    /**
     * 创建订单并写入本地消息
     *
     * @param orderId 订单ID
     */
    void createOrder(String orderId);

    /**
     * 保存本地消息
     *
     * @param request 本地消息创建请求
     */
    void saveLocalMessage(LocalMessageCreateRequest request);
}
```

下面的示例展示“业务数据 + 本地消息”在同一个数据库事务中提交。

文件位置：`src/main/java/io/github/atengk/rabbitmq/service/impl/OrderCreateServiceImpl.java`

```java
package io.github.atengk.rabbitmq.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.rabbitmq.dto.LocalMessageCreateRequest;
import io.github.atengk.rabbitmq.service.OrderCreateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 订单创建服务实现
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
public class OrderCreateServiceImpl implements OrderCreateService {

    /**
     * 创建订单并写入本地消息
     *
     * @param orderId 订单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(String orderId) {
        log.info("开始创建订单，orderId={}", orderId);

        // 1. 保存订单业务数据
        saveOrder(orderId);

        // 2. 构造订单创建事件消息
        String messageId = "order_created_" + DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss") + "_" + IdUtil.fastSimpleUUID();
        String messageBody = JSONUtil.toJsonStr(Map.of(
                "messageId", messageId,
                "eventType", "order.created",
                "businessKey", orderId,
                "timestamp", DateUtil.now()
        ));

        // 3. 在同一个数据库事务中保存本地消息
        saveLocalMessage(LocalMessageCreateRequest.builder()
                .messageId(messageId)
                .eventType("order.created")
                .businessKey(orderId)
                .exchangeName("order.exchange")
                .routingKey("order.created")
                .messageBody(messageBody)
                .build());

        log.info("订单创建完成，本地消息已记录，orderId={}，messageId={}", orderId, messageId);
    }

    /**
     * 保存本地消息
     *
     * @param request 本地消息创建请求
     */
    @Override
    public void saveLocalMessage(LocalMessageCreateRequest request) {
        // 示例：实际项目中写入 mq_local_message 表，状态为 PENDING
        log.info("保存本地消息，messageId={}，eventType={}，businessKey={}",
                request.getMessageId(),
                request.getEventType(),
                request.getBusinessKey()
        );
    }

    /**
     * 保存订单
     *
     * @param orderId 订单ID
     */
    private void saveOrder(String orderId) {
        // 示例：实际项目中写入订单表
        log.info("保存订单数据，orderId={}", orderId);
    }
}
```

本地消息表方案的核心是：数据库事务只负责保存业务数据和消息记录，不直接保证 MQ 一定发送成功；MQ 发送由异步投递任务和补偿任务保证最终完成。

### Outbox Pattern

Outbox Pattern 是本地消息表方案的一种标准化模式。业务服务在本地数据库事务中写入业务表和 Outbox 表，事务提交后，由独立投递器扫描 Outbox 表并发布消息到 RabbitMQ。

Outbox Pattern 流程如下：

```text
业务请求
    -> 开启数据库事务
    -> 写业务表
    -> 写 outbox_event 表
    -> 提交事务
    -> Outbox Publisher 扫描待发送事件
    -> 发布 RabbitMQ
    -> Publisher Confirm 成功
    -> 标记事件已发布
    -> 失败事件后续补偿重发
```

Outbox 表设计如下。

```sql
CREATE TABLE outbox_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    event_id VARCHAR(100) NOT NULL COMMENT '事件ID',
    aggregate_type VARCHAR(64) NOT NULL COMMENT '聚合类型',
    aggregate_id VARCHAR(100) NOT NULL COMMENT '聚合ID',
    event_type VARCHAR(64) NOT NULL COMMENT '事件类型',
    exchange_name VARCHAR(128) NOT NULL COMMENT '交换机',
    routing_key VARCHAR(128) NOT NULL COMMENT '路由键',
    payload TEXT NOT NULL COMMENT '事件内容',
    event_status VARCHAR(32) NOT NULL COMMENT '事件状态',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    next_retry_time DATETIME DEFAULT NULL COMMENT '下次重试时间',
    published_time DATETIME DEFAULT NULL COMMENT '发布时间',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_event_id (event_id),
    KEY idx_status_retry_time (event_status, next_retry_time),
    KEY idx_aggregate (aggregate_type, aggregate_id)
) COMMENT='Outbox事件表';
```

Outbox 状态建议：

| 状态         | 说明                           |
| ------------ | ------------------------------ |
| `NEW`        | 新建事件，待发布               |
| `PUBLISHING` | 发布中                         |
| `PUBLISHED`  | 已发布并确认                   |
| `FAILED`     | 发布失败，等待重试             |
| `DEAD`       | 超过最大重试次数，等待人工处理 |

Outbox 发布器示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/service/OutboxPublisher.java`

```java
package io.github.atengk.rabbitmq.service;

/**
 * Outbox 事件发布器
 *
 * @author Ateng
 * @since 2026-05-11
 */
public interface OutboxPublisher {

    /**
     * 发布待发送事件
     */
    void publishPendingEvents();
}
```

下面的示例展示 Outbox 扫描发布的核心流程，实际项目中应通过数据库查询待发布事件，并使用乐观锁或状态抢占避免多实例重复发布。

文件位置：`src/main/java/io/github/atengk/rabbitmq/service/impl/OutboxPublisherImpl.java`

```java
package io.github.atengk.rabbitmq.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import io.github.atengk.rabbitmq.service.OutboxPublisher;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Outbox 事件发布器实现
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisherImpl implements OutboxPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发布待发送事件
     */
    @Override
    public void publishPendingEvents() {
        List<OutboxEventRecord> events = queryPendingEvents();
        if (CollUtil.isEmpty(events)) {
            return;
        }

        for (OutboxEventRecord event : events) {
            try {
                markPublishing(event.getEventId());

                rabbitTemplate.convertAndSend(
                        event.getExchangeName(),
                        event.getRoutingKey(),
                        event.getPayload(),
                        message -> {
                            message.getMessageProperties().setMessageId(event.getEventId());
                            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                            message.getMessageProperties().setHeader("x-event-id", event.getEventId());
                            message.getMessageProperties().setHeader("x-event-type", event.getEventType());
                            message.getMessageProperties().setHeader("x-aggregate-id", event.getAggregateId());
                            message.getMessageProperties().setHeader("x-send-time", DateUtil.now());
                            return message;
                        },
                        new CorrelationData(event.getEventId())
                );

                markSent(event.getEventId());
                log.info("Outbox事件已提交发送，eventId={}，eventType={}，aggregateId={}",
                        event.getEventId(),
                        event.getEventType(),
                        event.getAggregateId()
                );
            } catch (Exception ex) {
                markFailed(event.getEventId(), ex.getMessage());
                log.error("Outbox事件发送失败，eventId={}，原因={}",
                        event.getEventId(),
                        ex.getMessage(),
                        ex
                );
            }
        }
    }

    /**
     * 查询待发布事件
     *
     * @return 事件列表
     */
    private List<OutboxEventRecord> queryPendingEvents() {
        // 示例：实际项目中查询 NEW 或 FAILED 且到达重试时间的事件
        return List.of();
    }

    /**
     * 标记发布中
     *
     * @param eventId 事件ID
     */
    private void markPublishing(String eventId) {
        log.info("标记Outbox事件为发布中，eventId={}", eventId);
    }

    /**
     * 标记已发送
     *
     * @param eventId 事件ID
     */
    private void markSent(String eventId) {
        log.info("标记Outbox事件为已提交发送，eventId={}", eventId);
    }

    /**
     * 标记发送失败
     *
     * @param eventId 事件ID
     * @param reason  失败原因
     */
    private void markFailed(String eventId, String reason) {
        log.warn("标记Outbox事件发送失败，eventId={}，reason={}", eventId, reason);
    }

    /**
     * Outbox 事件记录
     *
     * @author Ateng
     * @since 2026-05-11
     */
    @Data
    @Builder
    private static class OutboxEventRecord {

        private String eventId;

        private String aggregateType;

        private String aggregateId;

        private String eventType;

        private String exchangeName;

        private String routingKey;

        private String payload;
    }
}
```

Outbox Pattern 建议：

1. Outbox 表与业务表放在同一个数据库中。
2. 写业务数据和写 Outbox 事件必须在同一个事务中。
3. Outbox 发布器可以通过定时任务、异步线程或独立服务实现。
4. 发布器需要支持多实例抢占，避免重复发布。
5. 重复发布不可完全避免，消费者必须幂等。
6. Confirm 成功后再标记 `PUBLISHED` 更可靠。
7. 超过最大重试次数的事件应进入人工处理。

### 事务补偿机制

事务补偿机制用于处理消息发送失败、Confirm 失败、Return 不可路由、消费者处理失败、死信堆积等异常情况。补偿机制是最终一致性方案中的关键部分。

常见补偿对象如下：

| 补偿对象         | 触发条件          | 处理方式            |
| ---------------- | ----------------- | ------------------- |
| 待发送消息       | `PENDING` 超时    | 重新发送            |
| 发送失败消息     | `FAILED`          | 按重试策略发送      |
| Confirm 失败消息 | `CONFIRM_FAILED`  | 重发或人工处理      |
| 不可路由消息     | `RETURNED`        | 修复 Binding 后重发 |
| 死信消息         | 进入死信队列      | 排查后重发或忽略    |
| 消费失败记录     | 消费状态 `FAILED` | 业务补偿或人工处理  |

补偿任务流程如下：

```text
定时扫描异常消息
    -> 按状态筛选可补偿数据
    -> 抢占处理权，防止多实例重复补偿
    -> 判断重试次数
    -> 重新发送或重新处理
    -> 更新状态和重试次数
    -> 超过最大重试次数后标记人工处理
```

补偿任务示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/job/MqMessageCompensationJob.java`

```java
package io.github.atengk.rabbitmq.job;

import cn.hutool.core.collection.CollUtil;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MQ 消息补偿任务
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqMessageCompensationJob {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 扫描并补偿发送失败消息
     */
    @Scheduled(fixedDelay = 30_000)
    public void compensateFailedMessages() {
        List<CompensationMessage> messages = queryCompensationMessages();
        if (CollUtil.isEmpty(messages)) {
            return;
        }

        log.info("开始补偿MQ失败消息，count={}", messages.size());

        for (CompensationMessage message : messages) {
            try {
                markRetrying(message.getMessageId());

                rabbitTemplate.convertAndSend(
                        message.getExchangeName(),
                        message.getRoutingKey(),
                        message.getMessageBody()
                );

                markRetrySubmitted(message.getMessageId());
                log.info("MQ失败消息补偿发送成功，messageId={}，businessKey={}",
                        message.getMessageId(),
                        message.getBusinessKey()
                );
            } catch (Exception ex) {
                markRetryFailed(message.getMessageId(), ex.getMessage());
                log.error("MQ失败消息补偿发送失败，messageId={}，businessKey={}，原因={}",
                        message.getMessageId(),
                        message.getBusinessKey(),
                        ex.getMessage(),
                        ex
                );
            }
        }
    }

    /**
     * 查询需要补偿的消息
     *
     * @return 补偿消息列表
     */
    private List<CompensationMessage> queryCompensationMessages() {
        // 示例：查询 PENDING、FAILED、CONFIRM_FAILED 且未超过最大重试次数的消息
        return List.of();
    }

    /**
     * 标记重试中
     *
     * @param messageId 消息ID
     */
    private void markRetrying(String messageId) {
        log.info("标记MQ消息为重试中，messageId={}", messageId);
    }

    /**
     * 标记重试已提交
     *
     * @param messageId 消息ID
     */
    private void markRetrySubmitted(String messageId) {
        log.info("标记MQ消息重试已提交，messageId={}", messageId);
    }

    /**
     * 标记重试失败
     *
     * @param messageId 消息ID
     * @param reason    失败原因
     */
    private void markRetryFailed(String messageId, String reason) {
        log.warn("标记MQ消息重试失败，messageId={}，reason={}", messageId, reason);
    }

    /**
     * 补偿消息对象
     *
     * @author Ateng
     * @since 2026-05-11
     */
    @Data
    @Builder
    private static class CompensationMessage {

        private String messageId;

        private String businessKey;

        private String exchangeName;

        private String routingKey;

        private String messageBody;
    }
}
```

补偿机制建议：

1. 补偿任务必须有最大重试次数。
2. 多实例部署时需要抢占机制，例如状态更新、乐观锁或分布式锁。
3. 补偿重发必须依赖消费者幂等。
4. 不可路由消息应先修复路由配置，再补偿重发。
5. 死信消息重发前应明确失败原因是否已修复。
6. 超过最大重试次数后应进入人工处理，而不是无限重试。
7. 补偿任务应有监控指标和告警。

### 最终一致性设计

最终一致性是指业务系统之间不要求瞬时强一致，但通过可靠消息、重试、补偿、幂等和状态校验，保证最终达到一致状态。RabbitMQ 在分布式业务中通常用于实现最终一致性，而不是强一致事务。

典型订单支付最终一致性流程如下：

```text
支付服务收到支付成功回调
    -> 本地事务更新支付单状态
    -> 写入 outbox_event 支付成功事件
    -> 事务提交
    -> Outbox 发布器发送 payment.success 消息
    -> 订单服务消费 payment.success
    -> 幂等判断
    -> 更新订单状态为已支付
    -> Ack 消息
    -> 失败则重试或进入死信
    -> 补偿任务处理异常消息
```

最终一致性设计要点：

| 要点     | 说明                                 |
| -------- | ------------------------------------ |
| 本地事务 | 每个服务只保证自己的数据库事务       |
| 可靠消息 | 使用本地消息表或 Outbox 保证消息不丢 |
| 消费幂等 | 防止重复消息导致重复业务处理         |
| 状态机   | 根据业务状态判断事件是否可执行       |
| 重试机制 | 临时异常自动重试                     |
| 死信队列 | 多次失败消息进入死信                 |
| 补偿任务 | 扫描异常状态并重发或修复             |
| 对账机制 | 定期校验跨系统状态是否一致           |

最终一致性状态示例：

| 系统     | 本地状态         | 消息事件          | 最终目标     |
| -------- | ---------------- | ----------------- | ------------ |
| 支付服务 | 支付成功         | `payment.success` | 通知订单服务 |
| 订单服务 | 待支付 -> 已支付 | 消费支付成功消息  | 订单状态一致 |
| 积分服务 | 未发放 -> 已发放 | 消费订单支付消息  | 积分状态一致 |
| 通知服务 | 未通知 -> 已通知 | 消费通知消息      | 通知状态一致 |

最终一致性建议：

1. 不要试图通过 RabbitMQ 原生事务实现跨数据库和 MQ 的强一致事务。
2. 每个服务只保证本地事务正确。
3. 跨服务状态同步通过事件消息完成。
4. 消费者必须幂等。
5. 业务状态机必须兜底。
6. 异常消息必须可查询、可重发、可忽略、可人工处理。
7. 核心业务需要定期对账，例如订单状态和支付状态对账。
8. 用户可见状态需要设计“处理中”状态，避免短暂不一致导致误解。

最终推荐方案如下：

| 场景             | 推荐方案                                     |
| ---------------- | -------------------------------------------- |
| 普通异步通知     | RabbitTemplate + Confirm + Return            |
| 核心业务事件     | 本地消息表 / Outbox Pattern + Confirm + 补偿 |
| 支付、订单、库存 | 本地事务 + Outbox + 幂等消费 + 状态机        |
| 高吞吐日志       | 批量发送 + 批量消费 + 可降级                 |
| 长时间异常       | 死信队列 + 管理端人工处理                    |
| 跨系统一致性校验 | 定时对账 + 补偿任务                          |

核心结论：RabbitMQ 事务机制可以了解，但不应作为业务事务消息的主方案。生产项目中更推荐使用“本地事务写业务数据和消息表 + 异步可靠投递 + Confirm/Return + 消费幂等 + 死信补偿 + 定期对账”的最终一致性架构。

## 业务模块集成

本章节用于说明 RabbitMQ 在常见业务模块中的落地方式。RabbitMQ 不建议只作为“技术组件”孤立使用，而应结合订单、支付、库存、通知、日志和异步任务等业务场景，明确消息事件、生产者、消费者、幂等策略、失败处理和补偿方式。

### 订单消息场景

订单消息通常用于驱动订单创建、订单支付、订单取消、订单关闭、订单完成等状态流转后的异步处理。订单服务一般作为生产者，库存、支付、通知、积分、风控等模块作为消费者。

订单消息推荐事件如下：

| 事件       | Routing Key       | 说明                     |
| ---------- | ----------------- | ------------------------ |
| 订单已创建 | `order.created`   | 用户下单成功后发送       |
| 订单已支付 | `order.paid`      | 支付成功后发送           |
| 订单已取消 | `order.cancelled` | 用户取消或系统取消后发送 |
| 订单已关闭 | `order.closed`    | 超时未支付关闭后发送     |
| 订单已完成 | `order.finished`  | 订单完成后发送           |

订单消息对象示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/dto/OrderEventMessage.java`

```java
package io.github.atengk.rabbitmq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单事件消息
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
public class OrderEventMessage {

    @NotBlank(message = "消息ID不能为空")
    private String messageId;

    @NotBlank(message = "事件类型不能为空")
    private String eventType;

    @NotBlank(message = "订单ID不能为空")
    private String orderId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    private BigDecimal amount;

    @NotBlank(message = "订单状态不能为空")
    private String orderStatus;

    @NotNull(message = "事件时间不能为空")
    private LocalDateTime eventTime;
}
```

订单消息生产者示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/producer/OrderEventProducer.java`

```java
package io.github.atengk.rabbitmq.producer;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import io.github.atengk.rabbitmq.dto.OrderEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 订单事件生产者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    public static final String ORDER_EXCHANGE = "business.order.exchange";

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送订单事件消息
     *
     * @param message 订单事件消息
     * @param routingKey 路由键
     * @return 消息ID
     */
    public String sendOrderEvent(OrderEventMessage message, String routingKey) {
        String messageId = "order_" + DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss") + "_" + IdUtil.fastSimpleUUID();
        message.setMessageId(messageId);
        message.setEventTime(LocalDateTime.now());

        rabbitTemplate.convertAndSend(
                ORDER_EXCHANGE,
                routingKey,
                message,
                rabbitMessage -> {
                    rabbitMessage.getMessageProperties().setMessageId(messageId);
                    rabbitMessage.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    rabbitMessage.getMessageProperties().setHeader("x-message-id", messageId);
                    rabbitMessage.getMessageProperties().setHeader("x-event-type", message.getEventType());
                    rabbitMessage.getMessageProperties().setHeader("x-business-key", message.getOrderId());
                    rabbitMessage.getMessageProperties().setHeader("x-send-time", DateUtil.now());
                    return rabbitMessage;
                },
                new CorrelationData(messageId)
        );

        log.info("订单事件消息已发送，messageId={}，eventType={}，orderId={}，routingKey={}",
                messageId,
                message.getEventType(),
                message.getOrderId(),
                routingKey
        );
        return messageId;
    }
}
```

订单场景设计建议：

| 设计点   | 建议                           |
| -------- | ------------------------------ |
| 幂等 Key | `order.{eventType}:{orderId}`  |
| 消费策略 | 状态机判断订单是否允许流转     |
| 失败处理 | 重试后进入死信队列             |
| 可靠性   | 本地消息表 / Outbox Pattern    |
| 顺序性   | 同一订单可按 `orderId` 分片    |
| 补偿     | 定时对账订单状态和下游处理状态 |

订单事件消费者不能只依赖消息到达顺序，应结合订单当前状态判断是否可以处理。例如订单已经支付时，再次收到 `order.paid` 应直接 Ack；订单已取消后收到 `order.paid` 则需要进入异常处理或人工确认。

### 支付消息场景

支付消息通常由支付服务产生，用于通知订单服务、积分服务、通知服务和财务对账服务。支付消息属于核心资金链路，必须保证可靠投递、幂等消费和可追踪。

支付事件推荐如下：

| 事件     | Routing Key              | 说明             |
| -------- | ------------------------ | ---------------- |
| 支付创建 | `payment.created`        | 发起支付单       |
| 支付成功 | `payment.success`        | 支付渠道回调成功 |
| 支付失败 | `payment.failed`         | 支付失败或关闭   |
| 退款成功 | `payment.refund.success` | 退款成功         |
| 退款失败 | `payment.refund.failed`  | 退款失败         |

支付消息对象示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/dto/PaymentEventMessage.java`

```java
package io.github.atengk.rabbitmq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付事件消息
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
public class PaymentEventMessage {

    @NotBlank(message = "消息ID不能为空")
    private String messageId;

    @NotBlank(message = "支付单号不能为空")
    private String payNo;

    @NotBlank(message = "订单ID不能为空")
    private String orderId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "支付金额不能为空")
    private BigDecimal payAmount;

    @NotBlank(message = "支付状态不能为空")
    private String payStatus;

    @NotBlank(message = "支付渠道不能为空")
    private String payChannel;

    private LocalDateTime payTime;
}
```

支付消息消费建议：

| 场景             | 处理方式                                         |
| ---------------- | ------------------------------------------------ |
| 支付成功重复消息 | 根据 `payNo` 或 `orderId + payment.success` 幂等 |
| 支付状态异常     | 查询支付单和订单状态后判断                       |
| 消费失败         | 有限重试后进入死信                               |
| 长时间未处理     | 支付与订单状态对账补偿                           |
| 消息重发         | 必须依赖订单状态机兜底                           |

支付消息属于核心事件，不建议只使用 Redis 短 TTL 做幂等。推荐使用数据库唯一索引或业务状态机兜底，例如订单状态只能从 `WAIT_PAY` 更新为 `PAID`。

### 库存消息场景

库存消息常用于库存冻结、库存扣减、库存释放、库存同步等场景。库存消息需要重点关注幂等、顺序和补偿，因为重复扣减或乱序处理可能直接导致库存异常。

库存事件推荐如下：

| 事件     | Routing Key      | 说明                         |
| -------- | ---------------- | ---------------------------- |
| 库存冻结 | `stock.freeze`   | 下单后冻结库存               |
| 库存扣减 | `stock.deduct`   | 支付成功后扣减库存           |
| 库存释放 | `stock.release`  | 订单取消或超时关闭后释放库存 |
| 库存回滚 | `stock.rollback` | 异常补偿回滚库存             |

库存消息对象示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/dto/StockEventMessage.java`

```java
package io.github.atengk.rabbitmq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存事件消息
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
public class StockEventMessage {

    @NotBlank(message = "消息ID不能为空")
    private String messageId;

    @NotBlank(message = "事件类型不能为空")
    private String eventType;

    @NotBlank(message = "订单ID不能为空")
    private String orderId;

    @NotBlank(message = "商品ID不能为空")
    private String skuId;

    @NotNull(message = "库存数量不能为空")
    private Integer quantity;

    @NotNull(message = "事件时间不能为空")
    private LocalDateTime eventTime;
}
```

库存场景设计建议：

| 设计点   | 建议                                  |
| -------- | ------------------------------------- |
| 幂等 Key | `stock.{eventType}:{orderId}:{skuId}` |
| 顺序维度 | 按 `skuId` 或 `orderId + skuId` 分片  |
| 防重复   | 库存流水表唯一索引                    |
| 状态机   | 冻结、扣减、释放必须有库存流水状态    |
| 异常处理 | 失败进入死信，不直接丢弃              |
| 对账     | 定时核对订单、库存流水和库存余额      |

库存消费端建议使用库存流水表兜底，例如：

```sql
CREATE TABLE stock_flow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    flow_no VARCHAR(100) NOT NULL COMMENT '库存流水号',
    order_id VARCHAR(100) NOT NULL COMMENT '订单ID',
    sku_id VARCHAR(100) NOT NULL COMMENT '商品ID',
    event_type VARCHAR(64) NOT NULL COMMENT '事件类型',
    quantity INT NOT NULL COMMENT '库存数量',
    flow_status VARCHAR(32) NOT NULL COMMENT '流水状态',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_order_sku_event (order_id, sku_id, event_type)
) COMMENT='库存流水表';
```

### 通知消息场景

通知消息用于发送短信、邮件、站内信、App 推送、企业微信、钉钉等通知。通知类消息通常允许一定延迟，但需要防止重复发送和失败无感知。

通知事件推荐如下：

| 事件       | Routing Key         | 说明       |
| ---------- | ------------------- | ---------- |
| 短信发送   | `notice.sms.send`   | 发送短信   |
| 邮件发送   | `notice.email.send` | 发送邮件   |
| 站内信发送 | `notice.inbox.send` | 发送站内信 |
| App 推送   | `notice.push.send`  | 发送推送   |

通知消息对象示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/dto/NoticeMessage.java`

```java
package io.github.atengk.rabbitmq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通知消息
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
public class NoticeMessage {

    @NotBlank(message = "消息ID不能为空")
    private String messageId;

    @NotBlank(message = "通知类型不能为空")
    private String noticeType;

    @NotBlank(message = "接收人不能为空")
    private String receiver;

    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    private Map<String, Object> templateParams;

    @NotNull(message = "发送时间不能为空")
    private LocalDateTime sendTime;
}
```

通知场景设计建议：

| 设计点   | 建议                                                         |
| -------- | ------------------------------------------------------------ |
| 幂等 Key | `notice:{noticeType}:{templateCode}:{receiver}:{businessKey}` |
| 重试     | 第三方超时可重试                                             |
| 失败处理 | 记录失败原因，支持补发                                       |
| 批量     | 短信、邮件可批量消费                                         |
| TTL      | 实时通知可设置过期时间                                       |
| 降级     | 通知通道异常时可切换备用通道                                 |

通知消息不建议无条件重复发送。比如短信通知需要结合业务唯一键防重，避免用户收到多条重复短信。

### 日志采集场景

日志采集消息用于异步收集操作日志、审计日志、行为日志、接口访问日志等。日志类消息通常吞吐量较高，适合批量发送和批量消费。

日志事件推荐如下：

| 事件     | Routing Key             | 说明         |
| -------- | ----------------------- | ------------ |
| 操作日志 | `log.operation.created` | 用户操作日志 |
| 审计日志 | `log.audit.created`     | 审计事件日志 |
| 行为日志 | `log.behavior.created`  | 用户行为日志 |
| 接口日志 | `log.api.created`       | 接口访问日志 |

日志消息对象示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/dto/OperationLogMessage.java`

```java
package io.github.atengk.rabbitmq.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 操作日志消息
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
public class OperationLogMessage {

    @NotBlank(message = "消息ID不能为空")
    private String messageId;

    private Long userId;

    private String username;

    @NotBlank(message = "操作模块不能为空")
    private String module;

    @NotBlank(message = "操作动作不能为空")
    private String action;

    private String requestUri;

    private String requestMethod;

    private String clientIp;

    private Map<String, Object> extra;

    private LocalDateTime operationTime;
}
```

日志采集设计建议：

| 设计点   | 建议                                    |
| -------- | --------------------------------------- |
| Exchange | 可使用 Topic Exchange                   |
| 消费方式 | 批量消费                                |
| Ack 模式 | 普通日志可 auto，审计日志建议 manual    |
| TTL      | 普通行为日志可设置合理 TTL              |
| 死信     | 审计日志建议配置死信                    |
| 存储     | 批量写数据库、ES、ClickHouse 或日志系统 |

日志类消息不要影响主业务链路。生产者发送失败时，可以根据日志重要程度选择降级、落本地文件或写失败记录。

### 异步任务场景

异步任务消息用于执行耗时或非实时任务，例如文件解析、图片处理、报表生成、数据导入、数据同步、缓存刷新等。该类消息通常需要任务状态表配合，便于查询执行结果和失败重试。

异步任务事件推荐如下：

| 事件     | Routing Key            | 说明             |
| -------- | ---------------------- | ---------------- |
| 文件解析 | `task.file.parse`      | 解析上传文件     |
| 图片处理 | `task.image.process`   | 压缩、裁剪、转码 |
| 报表生成 | `task.report.generate` | 生成报表         |
| 数据导入 | `task.data.import`     | 批量导入数据     |
| 缓存刷新 | `task.cache.refresh`   | 刷新缓存         |

异步任务消息对象示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/dto/AsyncTaskMessage.java`

```java
package io.github.atengk.rabbitmq.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 异步任务消息
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
public class AsyncTaskMessage {

    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    @NotBlank(message = "任务类型不能为空")
    private String taskType;

    private String businessKey;

    private Map<String, Object> params;

    private LocalDateTime createTime;
}
```

异步任务设计建议：

| 设计点   | 建议                                                     |
| -------- | -------------------------------------------------------- |
| 任务状态 | 使用任务表记录 `PENDING`、`RUNNING`、`SUCCESS`、`FAILED` |
| 幂等     | 按 `taskId` 幂等                                         |
| 并发     | 根据任务类型配置不同消费者并发                           |
| 失败     | 记录失败原因，支持重新执行                               |
| 超时     | 长任务需要超时控制                                       |
| 进度     | 复杂任务可记录进度百分比                                 |

异步任务表示例：

```sql
CREATE TABLE async_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id VARCHAR(100) NOT NULL COMMENT '任务ID',
    task_type VARCHAR(64) NOT NULL COMMENT '任务类型',
    business_key VARCHAR(100) DEFAULT NULL COMMENT '业务主键',
    task_status VARCHAR(32) NOT NULL COMMENT '任务状态',
    task_params TEXT DEFAULT NULL COMMENT '任务参数',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '异常信息',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_task_id (task_id),
    KEY idx_task_status (task_status),
    KEY idx_business_key (business_key)
) COMMENT='异步任务表';
```

## 接口设计

本章节用于说明 RabbitMQ 管理和业务接入相关接口设计，包括消息发送、消息查询、消息重发、死信处理、状态查询和管理端接口。接口设计的目标是让消息具备可观测、可追踪、可补偿和可人工处理能力。

### 消息发送接口

消息发送接口用于业务系统或测试工具提交 MQ 消息。正式业务中，发送接口通常不直接暴露给外部用户，而是由内部服务调用；开发环境可以提供测试接口用于验证 RabbitMQ 发送链路。

消息发送请求对象如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/dto/MqSendApiRequest.java`

```java
package io.github.atengk.rabbitmq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * MQ 消息发送接口请求
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
public class MqSendApiRequest {

    @NotBlank(message = "交换机不能为空")
    private String exchange;

    @NotBlank(message = "路由键不能为空")
    private String routingKey;

    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    @NotBlank(message = "业务主键不能为空")
    private String businessKey;

    @NotNull(message = "消息体不能为空")
    private Map<String, Object> payload;
}
```

消息发送响应对象如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/dto/MqSendApiResponse.java`

```java
package io.github.atengk.rabbitmq.dto;

import lombok.Builder;
import lombok.Data;

/**
 * MQ 消息发送接口响应
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
public class MqSendApiResponse {

    private String messageId;

    private String sendStatus;

    private String exchange;

    private String routingKey;

    private String businessType;

    private String businessKey;
}
```

消息发送接口示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/controller/MqMessageApiController.java`

```java
package io.github.atengk.rabbitmq.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import io.github.atengk.rabbitmq.dto.MqSendApiRequest;
import io.github.atengk.rabbitmq.dto.MqSendApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * MQ 消息接口
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@RestController
@RequestMapping("/api/mq/messages")
@RequiredArgsConstructor
public class MqMessageApiController {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送 MQ 消息
     *
     * @param request 发送请求
     * @return 发送结果
     */
    @PostMapping("/send")
    public MqSendApiResponse send(@Valid @RequestBody MqSendApiRequest request) {
        String messageId = request.getBusinessType() + "_" + DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss") + "_" + IdUtil.fastSimpleUUID();

        rabbitTemplate.convertAndSend(
                request.getExchange(),
                request.getRoutingKey(),
                request.getPayload(),
                message -> {
                    message.getMessageProperties().setMessageId(messageId);
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    message.getMessageProperties().setHeader("x-message-id", messageId);
                    message.getMessageProperties().setHeader("x-business-type", request.getBusinessType());
                    message.getMessageProperties().setHeader("x-business-key", request.getBusinessKey());
                    message.getMessageProperties().setHeader("x-send-time", DateUtil.now());
                    return message;
                },
                new CorrelationData(messageId)
        );

        log.info("接口发送MQ消息成功，messageId={}，businessType={}，businessKey={}，exchange={}，routingKey={}",
                messageId,
                request.getBusinessType(),
                request.getBusinessKey(),
                request.getExchange(),
                request.getRoutingKey()
        );

        return MqSendApiResponse.builder()
                .messageId(messageId)
                .sendStatus("SENT")
                .exchange(request.getExchange())
                .routingKey(request.getRoutingKey())
                .businessType(request.getBusinessType())
                .businessKey(request.getBusinessKey())
                .build();
    }
}
```

接口说明：

| 项目     | 内容                             |
| -------- | -------------------------------- |
| 请求地址 | `POST /api/mq/messages/send`     |
| 用途     | 发送测试或内部业务消息           |
| 返回值   | 消息 ID 和发送状态               |
| 注意事项 | 生产环境应加权限控制和参数白名单 |

请求示例：

```bash
curl -X POST 'http://localhost:8080/api/mq/messages/send' \
  -H 'Content-Type: application/json' \
  -d '{
    "exchange": "business.order.exchange",
    "routingKey": "order.created",
    "businessType": "order_created",
    "businessKey": "ORDER202605110001",
    "payload": {
      "orderId": "ORDER202605110001",
      "userId": 10001,
      "amount": 199.00
    }
  }'
```

### 消息查询接口

消息查询接口用于按消息 ID、业务类型、业务主键、发送状态、时间范围查询消息记录。它依赖消息记录表或 Outbox 表，不建议直接从 RabbitMQ 队列中查询业务消息，因为队列不是历史消息数据库。

查询条件对象如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/dto/MqMessageQueryRequest.java`

```java
package io.github.atengk.rabbitmq.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * MQ 消息查询请求
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
public class MqMessageQueryRequest {

    private String messageId;

    private String businessType;

    private String businessKey;

    private String sendStatus;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer pageNum = 1;

    private Integer pageSize = 20;
}
```

查询结果对象如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/dto/MqMessageRecordVO.java`

```java
package io.github.atengk.rabbitmq.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MQ 消息记录展示对象
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
public class MqMessageRecordVO {

    private String messageId;

    private String businessType;

    private String businessKey;

    private String exchangeName;

    private String routingKey;

    private String sendStatus;

    private Integer retryCount;

    private String errorMessage;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

查询接口建议：

| 查询条件            | 说明                               |
| ------------------- | ---------------------------------- |
| `messageId`         | 精确定位一条消息                   |
| `businessKey`       | 查询某个订单、支付单、任务相关消息 |
| `businessType`      | 查询某类业务消息                   |
| `sendStatus`        | 查询失败、已确认、不可路由等状态   |
| `startTime/endTime` | 按时间范围筛选                     |
| `pageNum/pageSize`  | 分页查询，避免一次返回过多数据     |

接口设计：

```text
GET /api/mq/messages
GET /api/mq/messages/{messageId}
```

### 消息重发接口

消息重发接口用于对发送失败、Confirm 失败、Return 不可路由、死信处理后的消息进行重新投递。重发接口必须有权限控制和操作日志，避免误重发导致业务重复处理。

重发请求对象如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/dto/MqMessageResendRequest.java`

```java
package io.github.atengk.rabbitmq.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * MQ 消息重发请求
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
public class MqMessageResendRequest {

    @NotBlank(message = "消息ID不能为空")
    private String messageId;

    private String reason;
}
```

重发接口示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/controller/MqMessageResendController.java`

```java
package io.github.atengk.rabbitmq.controller;

import io.github.atengk.rabbitmq.dto.MqMessageResendRequest;
import io.github.atengk.rabbitmq.service.MqMessageResendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * MQ 消息重发接口
 *
 * @author Ateng
 * @since 2026-05-11
 */
@RestController
@RequestMapping("/api/mq/messages")
@RequiredArgsConstructor
public class MqMessageResendController {

    private final MqMessageResendService mqMessageResendService;

    /**
     * 重发消息
     *
     * @param request 重发请求
     * @return 处理结果
     */
    @PostMapping("/resend")
    public String resend(@Valid @RequestBody MqMessageResendRequest request) {
        mqMessageResendService.resend(request.getMessageId(), request.getReason());
        return "消息重发请求已提交";
    }
}
```

重发服务接口如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/service/MqMessageResendService.java`

```java
package io.github.atengk.rabbitmq.service;

/**
 * MQ 消息重发服务
 *
 * @author Ateng
 * @since 2026-05-11
 */
public interface MqMessageResendService {

    /**
     * 重发消息
     *
     * @param messageId 消息ID
     * @param reason    重发原因
     */
    void resend(String messageId, String reason);
}
```

重发规则建议：

| 状态             | 是否允许重发   | 说明                 |
| ---------------- | -------------- | -------------------- |
| `FAILED`         | 允许           | 发送异常             |
| `CONFIRM_FAILED` | 允许           | 未到达交换机         |
| `RETURNED`       | 修复路由后允许 | 不可路由             |
| `RETRY_FAILED`   | 人工确认后允许 | 超过最大重试次数     |
| `CONFIRMED`      | 谨慎           | 可能已经被消费       |
| `PUBLISHED`      | 谨慎           | 需要确认下游是否处理 |

重发接口必须依赖消费者幂等。即使同一消息被重发，消费者也不能重复扣库存、重复发积分、重复更新资金状态。

### 死信处理接口

死信处理接口用于查询、重发、忽略、标记处理死信消息。死信消息通常意味着消费端异常、消息格式异常、TTL 过期或队列超限，需要人工或自动补偿机制处理。

死信处理请求对象如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/dto/DeadLetterHandleRequest.java`

```java
package io.github.atengk.rabbitmq.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 死信消息处理请求
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
public class DeadLetterHandleRequest {

    @NotBlank(message = "死信消息ID不能为空")
    private String deadMessageId;

    @NotBlank(message = "处理动作不能为空")
    private String action;

    private String reason;
}
```

处理动作建议：

| action    | 说明             |
| --------- | ---------------- |
| `RESEND`  | 重新发送原消息   |
| `IGNORE`  | 忽略该死信       |
| `DONE`    | 标记已处理       |
| `EXPORT`  | 导出消息         |
| `RECHECK` | 重新检查业务状态 |

死信处理接口设计：

```text
GET /api/mq/dead-letters
GET /api/mq/dead-letters/{deadMessageId}
POST /api/mq/dead-letters/handle
POST /api/mq/dead-letters/resend
POST /api/mq/dead-letters/ignore
```

死信处理服务接口如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/service/DeadLetterHandleService.java`

```java
package io.github.atengk.rabbitmq.service;

/**
 * 死信消息处理服务
 *
 * @author Ateng
 * @since 2026-05-11
 */
public interface DeadLetterHandleService {

    /**
     * 重发死信消息
     *
     * @param deadMessageId 死信消息ID
     * @param reason        重发原因
     */
    void resend(String deadMessageId, String reason);

    /**
     * 忽略死信消息
     *
     * @param deadMessageId 死信消息ID
     * @param reason        忽略原因
     */
    void ignore(String deadMessageId, String reason);

    /**
     * 标记死信消息已处理
     *
     * @param deadMessageId 死信消息ID
     * @param reason        处理原因
     */
    void markDone(String deadMessageId, String reason);
}
```

死信处理建议：

1. 死信重发前必须先排查失败原因。
2. 消息格式错误类死信不应直接重发。
3. 业务状态冲突类死信应先查询业务状态。
4. 忽略死信必须填写原因。
5. 所有死信处理动作都应记录操作人、操作时间和处理原因。
6. 死信处理接口必须加权限控制。

### 消息状态接口

消息状态接口用于查看消息当前状态和链路状态，包括发送状态、Confirm 状态、Return 状态、消费状态、死信状态和重试状态。

推荐消息状态如下：

| 状态             | 说明           |
| ---------------- | -------------- |
| `PENDING`        | 待发送         |
| `SENT`           | 已提交发送     |
| `CONFIRMED`      | 交换机确认成功 |
| `CONFIRM_FAILED` | 交换机确认失败 |
| `RETURNED`       | 不可路由       |
| `CONSUMING`      | 消费中         |
| `CONSUMED`       | 消费成功       |
| `CONSUME_FAILED` | 消费失败       |
| `DEAD`           | 已进入死信     |
| `RETRYING`       | 重试中         |
| `RETRY_FAILED`   | 重试失败       |
| `IGNORED`        | 已忽略         |
| `DONE`           | 已处理         |

状态查询接口设计：

```text
GET /api/mq/messages/{messageId}/status
GET /api/mq/messages/status/summary
```

状态响应对象示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/dto/MqMessageStatusVO.java`

```java
package io.github.atengk.rabbitmq.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MQ 消息状态展示对象
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
public class MqMessageStatusVO {

    private String messageId;

    private String businessType;

    private String businessKey;

    private String sendStatus;

    private String consumeStatus;

    private String deadLetterStatus;

    private Integer retryCount;

    private String errorMessage;

    private LocalDateTime sendTime;

    private LocalDateTime confirmTime;

    private LocalDateTime consumeTime;

    private LocalDateTime updateTime;
}
```

状态接口应支持从消息记录表、消费记录表、死信记录表中聚合数据，形成完整消息链路视图。

### 管理端接口

管理端接口用于给运维、开发、测试和业务支持人员处理 MQ 异常消息。管理端不应直接操作 RabbitMQ 队列中的消息作为主要手段，而应基于消息记录表、消费记录表和死信记录表进行查询和处理。

推荐管理端能力如下：

| 能力     | 说明                                      |
| -------- | ----------------------------------------- |
| 消息查询 | 按消息 ID、业务主键、状态、时间查询       |
| 消息详情 | 查看消息体、Header、Exchange、Routing Key |
| 消息重发 | 对失败消息执行重发                        |
| 死信处理 | 查看、重发、忽略、标记死信                |
| 状态统计 | 按状态统计消息数量                        |
| 失败告警 | 展示 Confirm 失败、Return、死信堆积       |
| 操作审计 | 记录管理端操作日志                        |
| 导出数据 | 导出失败消息用于排查                      |

管理端接口规划如下：

| 接口                                        | 方法 | 说明         |
| ------------------------------------------- | ---- | ------------ |
| `/api/admin/mq/messages`                    | GET  | 分页查询消息 |
| `/api/admin/mq/messages/{messageId}`        | GET  | 查看消息详情 |
| `/api/admin/mq/messages/{messageId}/resend` | POST | 重发消息     |
| `/api/admin/mq/dead-letters`                | GET  | 查询死信消息 |
| `/api/admin/mq/dead-letters/{id}`           | GET  | 查看死信详情 |
| `/api/admin/mq/dead-letters/{id}/resend`    | POST | 重发死信     |
| `/api/admin/mq/dead-letters/{id}/ignore`    | POST | 忽略死信     |
| `/api/admin/mq/status/summary`              | GET  | 消息状态汇总 |
| `/api/admin/mq/queues/summary`              | GET  | 队列状态汇总 |

管理端操作日志表建议：

```sql
CREATE TABLE mq_admin_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
    target_type VARCHAR(64) NOT NULL COMMENT '目标类型',
    target_id VARCHAR(100) NOT NULL COMMENT '目标ID',
    operator_id VARCHAR(100) DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(100) DEFAULT NULL COMMENT '操作人名称',
    operation_reason VARCHAR(500) DEFAULT NULL COMMENT '操作原因',
    operation_result VARCHAR(32) NOT NULL COMMENT '操作结果',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '异常信息',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    KEY idx_target_id (target_id),
    KEY idx_operation_type (operation_type),
    KEY idx_create_time (create_time)
) COMMENT='MQ管理端操作日志表';
```

管理端设计建议：

1. 管理端接口必须加认证和权限控制。
2. 重发、忽略、标记已处理等操作必须记录操作日志。
3. 重发前应展示消息详情和历史失败原因。
4. 对核心业务消息，管理端操作最好支持二次确认。
5. 管理端只负责触发补偿，不能绕过业务幂等和状态机。
6. 队列状态展示可以对接 RabbitMQ Management HTTP API，但业务消息详情应优先来自业务消息表。
7. 管理端应提供失败消息统计，辅助判断是单条异常还是系统性故障。


## 数据库表设计

本章节用于说明 RabbitMQ 可靠消息体系中需要用到的核心数据库表。数据库表主要用于记录消息发送、Confirm、Return、消费、死信、重试和人工处理状态。RabbitMQ 队列本身不适合作为业务消息审计库，因此核心业务消息必须有业务侧落库记录。

### 消息记录表

消息记录表用于保存 MQ 消息主记录，记录一条消息从创建、发送、确认、不可路由、重试到最终处理的整体状态。该表是消息链路查询、补偿重发和管理端处理的核心表。

消息记录表建议保存生产端视角的主要信息，例如消息 ID、业务类型、业务主键、交换机、路由键、消息体、发送状态、重试次数、异常信息等。

文件位置：`sql/mq_message.sql`

```sql
-- MQ消息记录表：记录消息从创建到发送确认的主状态
CREATE TABLE mq_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(100) NOT NULL COMMENT '消息ID',
    business_type VARCHAR(64) NOT NULL COMMENT '业务类型，例如 order_paid',
    business_key VARCHAR(128) NOT NULL COMMENT '业务主键，例如订单号、支付单号',
    event_type VARCHAR(64) NOT NULL COMMENT '事件类型，例如 order.paid',
    exchange_name VARCHAR(128) NOT NULL COMMENT '交换机名称',
    routing_key VARCHAR(128) NOT NULL COMMENT '路由键',
    message_body LONGTEXT NOT NULL COMMENT '消息体JSON',
    message_headers TEXT DEFAULT NULL COMMENT '消息头JSON',
    message_status VARCHAR(32) NOT NULL COMMENT '消息状态：PENDING/SENT/CONFIRMED/RETURNED/FAILED/RETRY_FAILED',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '当前重试次数',
    max_retry_count INT NOT NULL DEFAULT 5 COMMENT '最大重试次数',
    next_retry_time DATETIME(3) DEFAULT NULL COMMENT '下次重试时间',
    confirm_time DATETIME(3) DEFAULT NULL COMMENT 'Confirm确认时间',
    return_time DATETIME(3) DEFAULT NULL COMMENT 'Return返回时间',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '异常信息',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME(3) NOT NULL COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_business_type_key (business_type, business_key),
    KEY idx_event_type (event_type),
    KEY idx_message_status (message_status),
    KEY idx_retry_status_time (message_status, next_retry_time),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='MQ消息记录表';
```

字段说明如下：

| 字段              | 说明                                                      |
| ----------------- | --------------------------------------------------------- |
| `message_id`      | 全局唯一消息 ID，用于 Confirm、Return、消费、死信链路关联 |
| `business_type`   | 业务类型，例如 `order_paid`、`stock_deduct`               |
| `business_key`    | 业务主键，例如订单号、支付单号、任务 ID                   |
| `event_type`      | 事件类型，例如 `order.paid`、`payment.success`            |
| `exchange_name`   | 发送目标 Exchange                                         |
| `routing_key`     | 发送使用的 Routing Key                                    |
| `message_body`    | 原始消息体 JSON，用于重发                                 |
| `message_headers` | 原始消息头 JSON，用于重发和排查                           |
| `message_status`  | 生产端消息状态                                            |
| `retry_count`     | 当前重试次数                                              |
| `next_retry_time` | 下次补偿重发时间                                          |
| `confirm_time`    | Publisher Confirm 成功时间                                |
| `return_time`     | Publisher Return 发生时间                                 |

推荐消息状态如下：

| 状态             | 说明              |
| ---------------- | ----------------- |
| `PENDING`        | 待发送            |
| `SENT`           | 已提交发送        |
| `CONFIRMED`      | Exchange 确认成功 |
| `CONFIRM_FAILED` | Exchange 确认失败 |
| `RETURNED`       | 消息不可路由      |
| `FAILED`         | 发送异常          |
| `RETRYING`       | 重试中            |
| `RETRY_FAILED`   | 超过最大重试次数  |
| `IGNORED`        | 人工忽略          |
| `DONE`           | 人工标记已处理    |

使用建议：

1. 核心业务发送 MQ 前，先在本地事务中写入 `mq_message`。
2. Confirm 成功后更新为 `CONFIRMED`。
3. Return 发生后更新为 `RETURNED`。
4. 补偿任务扫描 `FAILED`、`CONFIRM_FAILED`、`PENDING` 超时消息。
5. 消息重发必须复用 `message_body` 和 `message_headers`。
6. `message_id` 必须唯一，不能重复生成。

### 消息发送日志表

消息发送日志表用于记录每次发送尝试。消息记录表保存当前主状态，而发送日志表保存历史发送轨迹。对于一条消息，可能存在首次发送、补偿重发、人工重发等多次发送行为。

文件位置：`sql/mq_send_log.sql`

```sql
-- MQ消息发送日志表：记录每一次发送尝试
CREATE TABLE mq_send_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(100) NOT NULL COMMENT '消息ID',
    business_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    business_key VARCHAR(128) NOT NULL COMMENT '业务主键',
    exchange_name VARCHAR(128) NOT NULL COMMENT '交换机名称',
    routing_key VARCHAR(128) NOT NULL COMMENT '路由键',
    send_type VARCHAR(32) NOT NULL COMMENT '发送类型：NORMAL/RETRY/MANUAL',
    send_status VARCHAR(32) NOT NULL COMMENT '发送状态：SUCCESS/FAILED',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '发送时的重试次数',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '异常信息',
    send_time DATETIME(3) NOT NULL COMMENT '发送时间',
    create_time DATETIME(3) NOT NULL COMMENT '创建时间',
    KEY idx_message_id (message_id),
    KEY idx_business_type_key (business_type, business_key),
    KEY idx_send_status (send_status),
    KEY idx_send_time (send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='MQ消息发送日志表';
```

发送类型建议如下：

| 发送类型 | 说明           |
| -------- | -------------- |
| `NORMAL` | 正常业务发送   |
| `RETRY`  | 定时补偿重发   |
| `MANUAL` | 管理端人工重发 |

发送状态建议如下：

| 发送状态  | 说明                               |
| --------- | ---------------------------------- |
| `SUCCESS` | 调用 `RabbitTemplate` 成功提交发送 |
| `FAILED`  | 调用发送方法异常                   |

需要注意，`send_status=SUCCESS` 只表示应用已提交发送调用，不代表消息一定到达 Exchange。是否到达 Exchange 仍以 Publisher Confirm 为准。

### 消息消费日志表

消息消费日志表用于记录消费者处理消息的过程和结果。它主要服务于消费链路排查、幂等判断、失败补偿和审计。对于核心业务，不建议只依赖应用日志，应将消费结果落库。

文件位置：`sql/mq_consume_log.sql`

```sql
-- MQ消息消费日志表：记录消费者消费消息的结果
CREATE TABLE mq_consume_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(100) NOT NULL COMMENT '消息ID',
    business_type VARCHAR(64) DEFAULT NULL COMMENT '业务类型',
    business_key VARCHAR(128) DEFAULT NULL COMMENT '业务主键',
    event_type VARCHAR(64) DEFAULT NULL COMMENT '事件类型',
    queue_name VARCHAR(128) NOT NULL COMMENT '消费队列名称',
    consumer_group VARCHAR(128) DEFAULT NULL COMMENT '消费者分组或服务名称',
    consume_status VARCHAR(32) NOT NULL COMMENT '消费状态：CONSUMING/SUCCESS/FAILED/IGNORED',
    consume_count INT NOT NULL DEFAULT 1 COMMENT '消费次数',
    cost_ms BIGINT DEFAULT NULL COMMENT '消费耗时毫秒',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '异常信息',
    consume_time DATETIME(3) DEFAULT NULL COMMENT '消费完成时间',
    create_time DATETIME(3) NOT NULL COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_message_queue_consumer (message_id, queue_name, consumer_group),
    KEY idx_business_type_key (business_type, business_key),
    KEY idx_event_type (event_type),
    KEY idx_queue_status (queue_name, consume_status),
    KEY idx_consume_time (consume_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='MQ消息消费日志表';
```

消费状态建议如下：

| 状态        | 说明                   |
| ----------- | ---------------------- |
| `CONSUMING` | 消费中                 |
| `SUCCESS`   | 消费成功               |
| `FAILED`    | 消费失败               |
| `IGNORED`   | 重复消息或业务确认忽略 |
| `DEAD`      | 已进入死信处理         |

消费日志设计建议：

1. 消费开始时可以写入 `CONSUMING`。
2. 消费成功后更新为 `SUCCESS`。
3. 消费失败后更新为 `FAILED` 并记录异常原因。
4. 重复消息命中幂等后可记录为 `IGNORED`。
5. 唯一索引建议包含 `message_id + queue_name + consumer_group`，避免不同消费者组互相影响。
6. 核心业务可以结合该表实现数据库幂等。

### 死信消息表

死信消息表用于保存从死信队列消费到的异常消息。死信表应尽量保留原始消息体、原始 Header、来源队列、死信原因、处理状态和人工处理记录，便于后续排查和补偿。

文件位置：`sql/mq_dead_message.sql`

```sql
-- MQ死信消息表：记录进入死信队列的消息
CREATE TABLE mq_dead_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    dead_message_id VARCHAR(100) NOT NULL COMMENT '死信记录ID',
    message_id VARCHAR(100) DEFAULT NULL COMMENT '原消息ID',
    business_type VARCHAR(64) DEFAULT NULL COMMENT '业务类型',
    business_key VARCHAR(128) DEFAULT NULL COMMENT '业务主键',
    event_type VARCHAR(64) DEFAULT NULL COMMENT '事件类型',
    original_exchange VARCHAR(128) DEFAULT NULL COMMENT '原交换机',
    original_routing_key VARCHAR(128) DEFAULT NULL COMMENT '原路由键',
    original_queue VARCHAR(128) DEFAULT NULL COMMENT '原队列',
    dead_exchange VARCHAR(128) DEFAULT NULL COMMENT '死信交换机',
    dead_queue VARCHAR(128) NOT NULL COMMENT '死信队列',
    dead_reason VARCHAR(128) DEFAULT NULL COMMENT '死信原因：rejected/expired/maxlen/delivery_limit',
    message_body LONGTEXT NOT NULL COMMENT '死信消息体JSON',
    message_headers LONGTEXT DEFAULT NULL COMMENT '死信消息头JSON',
    handle_status VARCHAR(32) NOT NULL COMMENT '处理状态：PENDING/RETRIED/IGNORED/DONE/FAILED',
    handle_count INT NOT NULL DEFAULT 0 COMMENT '处理次数',
    last_handle_time DATETIME(3) DEFAULT NULL COMMENT '最后处理时间',
    handle_reason VARCHAR(500) DEFAULT NULL COMMENT '处理原因',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '异常信息',
    create_time DATETIME(3) NOT NULL COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_dead_message_id (dead_message_id),
    KEY idx_message_id (message_id),
    KEY idx_business_type_key (business_type, business_key),
    KEY idx_dead_queue_status (dead_queue, handle_status),
    KEY idx_dead_reason (dead_reason),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='MQ死信消息表';
```

死信处理状态建议如下：

| 状态      | 说明       |
| --------- | ---------- |
| `PENDING` | 待处理     |
| `RETRIED` | 已重发     |
| `IGNORED` | 已忽略     |
| `DONE`    | 已处理完成 |
| `FAILED`  | 处理失败   |

死信原因常见值如下：

| 原因             | 说明                          |
| ---------------- | ----------------------------- |
| `rejected`       | 消费者拒绝消息且不重新入队    |
| `expired`        | 消息 TTL 过期                 |
| `maxlen`         | 队列长度超过限制              |
| `delivery_limit` | Quorum Queue 投递次数超过限制 |

死信表设计建议：

1. `message_body` 和 `message_headers` 必须完整保存。
2. 死信重发前必须确认失败原因是否已修复。
3. 死信忽略必须填写 `handle_reason`。
4. 管理端处理死信时应更新 `handle_status`、`handle_count` 和 `last_handle_time`。
5. 死信表应有按队列、状态、业务主键的查询索引。

### 消息重试记录表

消息重试记录表用于保存每次补偿重试的明细。它与 `mq_send_log` 的区别是：`mq_send_log` 偏发送动作日志，而 `mq_retry_record` 偏补偿策略和调度过程，记录重试来源、重试结果、下次重试时间和最终失败原因。

文件位置：`sql/mq_retry_record.sql`

```sql
-- MQ消息重试记录表：记录消息补偿重试过程
CREATE TABLE mq_retry_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    retry_id VARCHAR(100) NOT NULL COMMENT '重试记录ID',
    message_id VARCHAR(100) NOT NULL COMMENT '消息ID',
    retry_source VARCHAR(32) NOT NULL COMMENT '重试来源：SEND_FAILED/CONFIRM_FAILED/RETURNED/DEAD_LETTER/MANUAL',
    retry_status VARCHAR(32) NOT NULL COMMENT '重试状态：PENDING/RETRYING/SUCCESS/FAILED',
    retry_count INT NOT NULL COMMENT '当前重试次数',
    max_retry_count INT NOT NULL COMMENT '最大重试次数',
    next_retry_time DATETIME(3) DEFAULT NULL COMMENT '下次重试时间',
    retry_time DATETIME(3) DEFAULT NULL COMMENT '本次重试时间',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '异常信息',
    operator_id VARCHAR(100) DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(100) DEFAULT NULL COMMENT '操作人名称',
    operation_reason VARCHAR(500) DEFAULT NULL COMMENT '操作原因',
    create_time DATETIME(3) NOT NULL COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_retry_id (retry_id),
    KEY idx_message_id (message_id),
    KEY idx_retry_status_time (retry_status, next_retry_time),
    KEY idx_retry_source (retry_source),
    KEY idx_retry_time (retry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='MQ消息重试记录表';
```

重试来源建议如下：

| 来源             | 说明                      |
| ---------------- | ------------------------- |
| `SEND_FAILED`    | 调用发送方法失败          |
| `CONFIRM_FAILED` | Publisher Confirm 失败    |
| `RETURNED`       | Publisher Return 不可路由 |
| `DEAD_LETTER`    | 死信消息重发              |
| `MANUAL`         | 管理端人工重发            |

重试状态建议如下：

| 状态       | 说明     |
| ---------- | -------- |
| `PENDING`  | 待重试   |
| `RETRYING` | 重试中   |
| `SUCCESS`  | 重试成功 |
| `FAILED`   | 重试失败 |

重试记录表设计建议：

1. 每次重试都生成独立 `retry_id`。
2. 定时任务扫描 `PENDING` 且 `next_retry_time <= NOW()` 的记录。
3. 多实例任务执行时，应先将状态抢占为 `RETRYING`。
4. 重试成功后更新为 `SUCCESS`。
5. 超过最大重试次数后更新主消息表为 `RETRY_FAILED`。

### 表索引设计

索引设计需要围绕实际查询路径，而不是盲目给所有字段加索引。MQ 相关表常见查询路径包括：按消息 ID 精确查询、按业务主键查询、按状态扫描补偿、按时间分页查询、按队列和状态查询死信。

推荐索引设计如下：

| 表                | 索引                        | 用途                     |
| ----------------- | --------------------------- | ------------------------ |
| `mq_message`      | `uk_message_id`             | 按消息 ID 精确查询       |
| `mq_message`      | `idx_business_type_key`     | 查询某个业务对象相关消息 |
| `mq_message`      | `idx_retry_status_time`     | 补偿任务扫描             |
| `mq_send_log`     | `idx_message_id`            | 查询消息发送历史         |
| `mq_consume_log`  | `uk_message_queue_consumer` | 消费幂等和消费结果查询   |
| `mq_dead_message` | `idx_dead_queue_status`     | 管理端查询待处理死信     |
| `mq_retry_record` | `idx_retry_status_time`     | 重试任务扫描             |

补偿任务常用 SQL 如下：

```sql
-- 查询需要补偿重发的消息
SELECT id, message_id, exchange_name, routing_key, message_body, retry_count, max_retry_count
FROM mq_message
WHERE message_status IN ('FAILED', 'CONFIRM_FAILED')
  AND retry_count < max_retry_count
  AND (next_retry_time IS NULL OR next_retry_time <= NOW(3))
ORDER BY create_time ASC
LIMIT 100;
```

管理端常用 SQL 如下：

```sql
-- 按业务主键查询消息链路
SELECT *
FROM mq_message
WHERE business_type = 'order_paid'
  AND business_key = 'ORDER202605110001'
ORDER BY create_time DESC;

-- 查询待处理死信消息
SELECT *
FROM mq_dead_message
WHERE dead_queue = 'order.paid.dead.queue'
  AND handle_status = 'PENDING'
ORDER BY create_time ASC
LIMIT 50;
```

索引设计建议：

1. `message_id` 必须唯一。
2. `business_type + business_key` 是排查业务问题的高频索引。
3. 补偿扫描使用 `message_status + next_retry_time` 联合索引。
4. 死信查询使用 `dead_queue + handle_status` 联合索引。
5. 日志表数据量较大时，应按时间归档或分区。
6. `message_body`、`message_headers`、`error_message` 不应建立普通索引。
7. 管理端分页查询必须带时间范围，避免全表扫描。

## Redis 设计

本章节用于说明 RabbitMQ 消息体系中的 Redis Key 设计。Redis 主要用于幂等控制、消费锁、重试计数、延迟补偿和短期状态缓存。Redis 不应替代数据库审计表，核心业务仍建议使用数据库记录作为最终依据。

### 消息幂等 Key

消息幂等 Key 用于判断某个业务事件是否已经成功处理。它通常在消费成功后写入，后续重复消息到达时直接 Ack，不再重复执行业务逻辑。

推荐格式：

```text
mq:idempotent:{eventType}:{businessKey}
```

示例：

```text
mq:idempotent:order.paid:ORDER202605110001
mq:idempotent:stock.deduct:ORDER202605110001:SKU10001
mq:idempotent:notice.sms.send:ORDER202605110001
```

值设计建议：

```json
{
  "messageId": "order_paid_20260511103000_xxx",
  "consumeTime": "2026-05-11 10:30:00",
  "consumer": "order-service"
}
```

使用方式示例：

```java
Boolean exists = stringRedisTemplate.hasKey("mq:idempotent:order.paid:ORDER202605110001");
if (Boolean.TRUE.equals(exists)) {
    log.warn("订单支付消息已处理，跳过重复消费，orderId={}", "ORDER202605110001");
    return;
}
```

幂等 Key 设计建议：

1. 幂等 Key 优先使用 `eventType + businessKey`。
2. 不建议只使用 `messageId`，因为同一业务事件补偿重发时可能生成新消息 ID。
3. Key 中不要放过长的 JSON 或特殊字符。
4. 核心业务建议 Redis 幂等 + 数据库状态机双重保障。
5. TTL 要覆盖消息可能重复投递和人工补偿的时间窗口。

### 消息锁 Key

消息锁 Key 用于防止多个消费者并发处理同一业务事件。它通常在消费开始前通过 `SETNX` 写入，处理成功后删除消费中锁并写入幂等 Key，处理失败后释放锁或等待锁过期。

推荐格式：

```text
mq:lock:{eventType}:{businessKey}
```

示例：

```text
mq:lock:order.paid:ORDER202605110001
mq:lock:stock.deduct:ORDER202605110001:SKU10001
```

下面的工具类统一构建 MQ Redis Key，避免各业务模块手写字符串导致格式不一致。

文件位置：`src/main/java/io/github/atengk/rabbitmq/util/MqRedisKeyUtil.java`

```java
package io.github.atengk.rabbitmq.util;

import cn.hutool.core.util.StrUtil;

/**
 * MQ Redis Key 工具类
 *
 * @author Ateng
 * @since 2026-05-11
 */
public final class MqRedisKeyUtil {

    private static final String PREFIX = "mq";

    private MqRedisKeyUtil() {
    }

    /**
     * 构建消息幂等 Key
     *
     * @param eventType   事件类型
     * @param businessKey 业务主键
     * @return Redis Key
     */
    public static String idempotentKey(String eventType, String businessKey) {
        return StrUtil.format("{}:idempotent:{}:{}", PREFIX, normalize(eventType), normalize(businessKey));
    }

    /**
     * 构建消息锁 Key
     *
     * @param eventType   事件类型
     * @param businessKey 业务主键
     * @return Redis Key
     */
    public static String lockKey(String eventType, String businessKey) {
        return StrUtil.format("{}:lock:{}:{}", PREFIX, normalize(eventType), normalize(businessKey));
    }

    /**
     * 构建重试计数 Key
     *
     * @param messageId 消息ID
     * @return Redis Key
     */
    public static String retryCountKey(String messageId) {
        return StrUtil.format("{}:retry:count:{}", PREFIX, normalize(messageId));
    }

    /**
     * 构建延迟补偿 Key
     *
     * @param messageId 消息ID
     * @return Redis Key
     */
    public static String delayCompensationKey(String messageId) {
        return StrUtil.format("{}:delay:compensation:{}", PREFIX, normalize(messageId));
    }

    /**
     * 规范化 Key 片段
     *
     * @param value 原始值
     * @return 规范化后的值
     */
    private static String normalize(String value) {
        return StrUtil.blankToDefault(value, "unknown").trim();
    }
}
```

使用 `SETNX` 获取锁的代码如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/service/impl/RedisMqLockService.java`

```java
package io.github.atengk.rabbitmq.service.impl;

import io.github.atengk.rabbitmq.util.MqRedisKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis MQ 消息锁服务
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisMqLockService {

    private static final Duration LOCK_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试获取消息锁
     *
     * @param eventType   事件类型
     * @param businessKey 业务主键
     * @return 是否获取成功
     */
    public boolean tryLock(String eventType, String businessKey) {
        String key = MqRedisKeyUtil.lockKey(eventType, businessKey);
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_TTL);

        if (Boolean.TRUE.equals(success)) {
            log.info("MQ消息锁获取成功，key={}", key);
            return true;
        }

        log.warn("MQ消息锁已存在，key={}", key);
        return false;
    }

    /**
     * 释放消息锁
     *
     * @param eventType   事件类型
     * @param businessKey 业务主键
     */
    public void unlock(String eventType, String businessKey) {
        String key = MqRedisKeyUtil.lockKey(eventType, businessKey);
        stringRedisTemplate.delete(key);
        log.info("MQ消息锁已释放，key={}", key);
    }
}
```

消息锁 Key 设计建议：

1. 锁必须设置 TTL，避免消费者宕机后永久锁死。
2. TTL 应大于正常消费最大耗时。
3. 消费成功后应主动释放锁并写入幂等 Key。
4. 消费失败后可以释放锁，允许重试；也可以等待锁自然过期，取决于业务策略。
5. 分布式强锁场景可使用 Redisson，但普通 MQ 幂等锁用 `SETNX + TTL` 通常足够。

### 重试计数 Key

重试计数 Key 用于记录短期消费重试次数或业务补偿次数。它适合轻量级重试控制，不适合替代数据库重试记录表。

推荐格式：

```text
mq:retry:count:{messageId}
```

示例：

```text
mq:retry:count:order_paid_20260511103000_xxx
```

使用示例：

```java
String key = MqRedisKeyUtil.retryCountKey(messageId);
Long retryCount = stringRedisTemplate.opsForValue().increment(key);
stringRedisTemplate.expire(key, Duration.ofDays(1));

if (retryCount != null && retryCount > 3) {
    log.error("MQ消息超过最大Redis重试次数，messageId={}，retryCount={}", messageId, retryCount);
}
```

重试计数 Key 建议：

1. 只用于短期快速判断，不作为最终审计依据。
2. 必须设置过期时间。
3. 核心业务重试次数仍应写入数据库。
4. Redis 计数和数据库计数可能不完全一致，应以数据库为准。
5. Redis 重试计数适合消费者内部短时间防抖、限流和降级判断。

### 延迟补偿 Key

延迟补偿 Key 用于记录需要在未来某个时间点检查或补偿的消息。它可以配合 Redis ZSet 实现轻量级延迟任务，也可以只作为延迟补偿状态标记。

推荐格式：

```text
mq:delay:compensation:{messageId}
```

示例：

```text
mq:delay:compensation:order_close_delay_20260511103000_xxx
```

如果使用 Redis ZSet 管理补偿任务，推荐 Key 如下：

```text
mq:delay:compensation:zset
```

ZSet Score 使用未来执行时间戳：

```java
long executeTimeMillis = System.currentTimeMillis() + Duration.ofMinutes(30).toMillis();
stringRedisTemplate.opsForZSet().add("mq:delay:compensation:zset", messageId, executeTimeMillis);
```

扫描到期补偿任务示例：

```java
long now = System.currentTimeMillis();
Set<String> messageIds = stringRedisTemplate.opsForZSet()
        .rangeByScore("mq:delay:compensation:zset", 0, now, 0, 100);
```

延迟补偿 Key 设计建议：

1. Redis ZSet 适合轻量级延迟补偿，不适合海量长期调度。
2. 核心业务延迟补偿应同时有数据库任务记录。
3. 扫描任务处理成功后，应从 ZSet 删除对应 `messageId`。
4. 多实例扫描时需要抢占锁，避免重复补偿。
5. 长周期任务建议使用数据库任务表和调度平台，而不是只依赖 Redis。

### Key 过期策略

Key 过期策略用于控制 Redis 内存占用和幂等窗口。不同 Key 类型的 TTL 应根据业务重要性、重复投递窗口、死信重发窗口和人工补偿周期设计。

推荐过期时间如下：

| Key 类型     | 示例                                | 推荐 TTL       |
| ------------ | ----------------------------------- | -------------- |
| 消息锁 Key   | `mq:lock:order.paid:ORDER001`       | 5 到 30 分钟   |
| 幂等 Key     | `mq:idempotent:order.paid:ORDER001` | 7 到 90 天     |
| 通知幂等 Key | `mq:idempotent:notice.sms.send:xxx` | 1 到 7 天      |
| 重试计数 Key | `mq:retry:count:messageId`          | 1 到 7 天      |
| 延迟补偿 Key | `mq:delay:compensation:messageId`   | 补偿完成后删除 |
| 临时调试 Key | `mq:debug:*`                        | 1 到 24 小时   |

不同业务推荐策略如下：

| 业务     | 幂等 TTL 建议         | 原因                   |
| -------- | --------------------- | ---------------------- |
| 支付成功 | 90 天或数据库长期保存 | 涉及资金和售后周期     |
| 订单状态 | 30 到 90 天           | 订单生命周期较长       |
| 库存扣减 | 30 到 90 天           | 需要覆盖退货和补偿周期 |
| 短信通知 | 1 到 7 天             | 防止短时间重复发送     |
| 日志采集 | 1 到 3 天             | 低价值消息             |
| 异步任务 | 7 到 30 天            | 便于任务补偿和查询     |

Redis 配置和监控建议：

1. 所有 MQ 相关 Redis Key 都必须有明确命名空间，例如 `mq:`。
2. 除长期幂等审计外，Redis Key 应设置 TTL。
3. 核心业务不能只依赖 Redis TTL，应有数据库记录或业务状态机。
4. 需要监控 `mq:*` Key 数量和内存占用。
5. 大量 Key 过期可能造成 Redis 抖动，TTL 应适当打散。
6. 批量写入 Redis 时避免一次性产生过多同一过期时间的 Key。

打散 TTL 示例：

```java
int baseDays = 7;
int randomMinutes = cn.hutool.core.util.RandomUtil.randomInt(0, 120);
Duration ttl = Duration.ofDays(baseDays).plusMinutes(randomMinutes);

stringRedisTemplate.opsForValue().set(idempotentKey, "1", ttl);
```

该方式可以避免大量幂等 Key 在同一时刻集中失效，降低 Redis 过期删除压力。

Redis Key 统一规划如下：

| 用途         | Key 格式                                  | 示例                                |
| ------------ | ----------------------------------------- | ----------------------------------- |
| 消息幂等     | `mq:idempotent:{eventType}:{businessKey}` | `mq:idempotent:order.paid:ORDER001` |
| 消息锁       | `mq:lock:{eventType}:{businessKey}`       | `mq:lock:order.paid:ORDER001`       |
| 重试计数     | `mq:retry:count:{messageId}`              | `mq:retry:count:msg_001`            |
| 延迟补偿     | `mq:delay:compensation:{messageId}`       | `mq:delay:compensation:msg_001`     |
| 延迟补偿集合 | `mq:delay:compensation:zset`              | `mq:delay:compensation:zset`        |
| 消费者限流   | `mq:rate:{queueName}`                     | `mq:rate:order.paid.queue`          |
| 管理端操作锁 | `mq:admin:lock:{messageId}`               | `mq:admin:lock:msg_001`             |

最终建议：Redis 适合做短期状态、锁、幂等窗口和快速计数；数据库适合做长期审计、可靠补偿和管理端查询。核心业务应采用“Redis 快速幂等 + 数据库消费记录 + 业务状态机”的组合方案，而不是只依赖单一组件。

## 统一异常处理

本章节用于说明 RabbitMQ 开发中的异常分类、处理策略和统一记录方式。MQ 异常不应只依赖控制台日志排查，核心业务需要将生产异常、消费异常、序列化异常、路由异常、重试异常和死信异常统一记录，便于后续查询、告警、补偿和人工处理。

推荐异常处理原则如下：

| 原则         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 异常分类     | 区分生产者异常、消费者异常、序列化异常、网络异常、路由异常和业务异常 |
| 不吞异常     | 不能捕获异常后无记录直接忽略                                 |
| 不盲目重试   | 参数错误、格式错误、业务不可恢复异常不应无限重试             |
| 核心业务落库 | 订单、支付、库存等核心消息异常需要写入异常记录表             |
| 配合死信     | 消费失败达到重试上限后进入死信队列                           |
| 保留上下文   | 异常日志必须包含 `messageId`、业务主键、Exchange、Routing Key、Queue |
| 支持补偿     | 异常消息需要具备重发、忽略、标记处理能力                     |

推荐统一异常类型如下：

```text
PRODUCER_SEND_ERROR
PRODUCER_CONFIRM_ERROR
PRODUCER_RETURN_ERROR
CONSUMER_HANDLE_ERROR
MESSAGE_SERIALIZE_ERROR
MESSAGE_DESERIALIZE_ERROR
NETWORK_ERROR
ROUTING_ERROR
BUSINESS_ERROR
DEAD_LETTER_ERROR
```

### 生产者异常

生产者异常主要发生在消息构造、消息序列化、连接 RabbitMQ、调用 `RabbitTemplate`、Publisher Confirm 和 Publisher Return 阶段。

常见生产者异常如下：

| 异常场景        | 说明                              | 处理方式                    |
| --------------- | --------------------------------- | --------------------------- |
| 参数为空        | Exchange、Routing Key、消息体为空 | 直接抛业务异常，不发送      |
| 序列化失败      | 消息对象无法转换为 JSON           | 记录异常，修复消息结构      |
| 连接失败        | RabbitMQ 不可达或认证失败         | 记录失败，进入补偿          |
| Exchange 不存在 | 发送到不存在的交换机              | Confirm 失败或 Channel 异常 |
| 不可路由        | Exchange 存在但无匹配 Queue       | ReturnCallback 记录         |
| Confirm 失败    | Broker 未确认消息到达 Exchange    | 更新状态，补偿重发          |

生产者异常统一封装建议定义自定义异常。

文件位置：`src/main/java/io/github/atengk/rabbitmq/exception/MqProducerException.java`

```java
package io.github.atengk.rabbitmq.exception;

/**
 * MQ 生产者异常
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class MqProducerException extends RuntimeException {

    public MqProducerException(String message) {
        super(message);
    }

    public MqProducerException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

下面的代码用于统一处理生产者发送异常，适合作为所有业务生产者的发送入口。

文件位置：`src/main/java/io/github/atengk/rabbitmq/producer/SafeRabbitMessageProducer.java`

```java
package io.github.atengk.rabbitmq.producer;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.rabbitmq.exception.MqProducerException;
import io.github.atengk.rabbitmq.service.MqExceptionRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 安全 RabbitMQ 消息生产者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SafeRabbitMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    private final MqExceptionRecordService mqExceptionRecordService;

    /**
     * 安全发送消息
     *
     * @param exchange     交换机
     * @param routingKey   路由键
     * @param businessType 业务类型
     * @param businessKey  业务主键
     * @param payload      消息体
     * @return 消息ID
     */
    public String send(String exchange, String routingKey, String businessType, String businessKey, Object payload) {
        validateSendParam(exchange, routingKey, businessType, businessKey, payload);

        String messageId = StrUtil.format("{}_{}_{}", businessType, DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss"), IdUtil.fastSimpleUUID());

        try {
            rabbitTemplate.convertAndSend(
                    exchange,
                    routingKey,
                    payload,
                    message -> {
                        message.getMessageProperties().setMessageId(messageId);
                        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        message.getMessageProperties().setTimestamp(DateUtil.date());
                        message.getMessageProperties().setHeader("x-message-id", messageId);
                        message.getMessageProperties().setHeader("x-business-type", businessType);
                        message.getMessageProperties().setHeader("x-business-key", businessKey);
                        message.getMessageProperties().setHeader("x-send-time", DateUtil.now());
                        return message;
                    },
                    new CorrelationData(messageId)
            );

            log.info("MQ消息提交发送成功，messageId={}，businessType={}，businessKey={}，exchange={}，routingKey={}",
                    messageId,
                    businessType,
                    businessKey,
                    exchange,
                    routingKey
            );
            return messageId;
        } catch (Exception ex) {
            String errorMessage = ExceptionUtil.getMessage(ex);

            mqExceptionRecordService.recordProducerException(
                    messageId,
                    businessType,
                    businessKey,
                    exchange,
                    routingKey,
                    "PRODUCER_SEND_ERROR",
                    errorMessage,
                    ex
            );

            log.error("MQ消息发送异常，messageId={}，businessType={}，businessKey={}，exchange={}，routingKey={}，原因={}",
                    messageId,
                    businessType,
                    businessKey,
                    exchange,
                    routingKey,
                    errorMessage,
                    ex
            );

            throw new MqProducerException("MQ消息发送异常：" + errorMessage, ex);
        }
    }

    /**
     * 校验发送参数
     *
     * @param exchange     交换机
     * @param routingKey   路由键
     * @param businessType 业务类型
     * @param businessKey  业务主键
     * @param payload      消息体
     */
    private void validateSendParam(String exchange, String routingKey, String businessType, String businessKey, Object payload) {
        if (StrUtil.hasBlank(exchange, routingKey, businessType, businessKey)) {
            throw new IllegalArgumentException("MQ发送参数不能为空");
        }
        if (payload == null) {
            throw new IllegalArgumentException("MQ消息体不能为空");
        }
    }
}
```

生产者异常处理建议：

1. 发送前先校验 Exchange、Routing Key、业务类型、业务主键和消息体。
2. 调用 `RabbitTemplate` 的异常要记录到异常表。
3. Confirm 失败不应只打印日志，应更新消息状态。
4. Return 不可路由必须记录 Exchange 和 Routing Key。
5. 核心业务发送失败后应进入本地消息表补偿流程。
6. 生产者异常不代表消费者异常，二者需要分别记录。

### 消费者异常

消费者异常主要发生在消息反序列化、参数校验、业务处理、幂等控制、手动 Ack/Nack、下游服务调用等阶段。

常见消费者异常如下：

| 异常场景     | 说明                           | 建议处理                    |
| ------------ | ------------------------------ | --------------------------- |
| 消息体为空   | 消息内容缺失                   | Nack 且不重新入队，进入死信 |
| 参数缺失     | 订单号、支付单号等必填字段为空 | Nack 进入死信               |
| 幂等服务异常 | Redis 或数据库不可用           | 核心业务不建议继续处理      |
| 业务状态异常 | 当前状态不允许处理该事件       | 视业务进入死信或忽略        |
| 下游服务异常 | 数据库、第三方接口异常         | 有限重试后死信              |
| Ack 失败     | 业务成功但确认失败             | 依赖幂等防止重复消费        |

消费者异常处理示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/consumer/SafeRabbitMessageConsumer.java`

```java
package io.github.atengk.rabbitmq.consumer;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.Channel;
import io.github.atengk.rabbitmq.service.MqExceptionRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 安全 RabbitMQ 消息消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SafeRabbitMessageConsumer {

    private final MqExceptionRecordService mqExceptionRecordService;

    /**
     * 消费安全消息
     *
     * @param payload 消息体
     * @param message 原始消息
     * @param channel RabbitMQ Channel
     * @throws IOException Ack 或 Nack 失败时抛出
     */
    @RabbitListener(queues = "safe.business.queue")
    public void consume(String payload, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = StrUtil.blankToDefault(message.getMessageProperties().getMessageId(), "unknown");
        String queueName = message.getMessageProperties().getConsumerQueue();
        String businessType = String.valueOf(message.getMessageProperties().getHeaders().get("x-business-type"));
        String businessKey = String.valueOf(message.getMessageProperties().getHeaders().get("x-business-key"));

        try {
            log.info("收到MQ消息，messageId={}，businessType={}，businessKey={}，queue={}，deliveryTag={}",
                    messageId,
                    businessType,
                    businessKey,
                    queueName,
                    deliveryTag
            );

            validatePayload(payload);
            handleBusiness(payload);

            channel.basicAck(deliveryTag, false);
            log.info("MQ消息消费成功，messageId={}，businessType={}，businessKey={}，queue={}",
                    messageId,
                    businessType,
                    businessKey,
                    queueName
            );
        } catch (IllegalArgumentException ex) {
            recordConsumerException(messageId, businessType, businessKey, queueName, "BUSINESS_PARAM_ERROR", ex);
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception ex) {
            recordConsumerException(messageId, businessType, businessKey, queueName, "CONSUMER_HANDLE_ERROR", ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 校验消息体
     *
     * @param payload 消息体
     */
    private void validatePayload(String payload) {
        if (StrUtil.isBlank(payload)) {
            throw new IllegalArgumentException("消息体不能为空");
        }
    }

    /**
     * 执行业务处理
     *
     * @param payload 消息体
     */
    private void handleBusiness(String payload) {
        log.info("执行MQ业务处理，payload={}", payload);
    }

    /**
     * 记录消费者异常
     *
     * @param messageId    消息ID
     * @param businessType 业务类型
     * @param businessKey  业务主键
     * @param queueName    队列名称
     * @param errorType    异常类型
     * @param ex           异常对象
     */
    private void recordConsumerException(String messageId, String businessType, String businessKey, String queueName, String errorType, Exception ex) {
        String errorMessage = ExceptionUtil.getMessage(ex);

        mqExceptionRecordService.recordConsumerException(
                messageId,
                businessType,
                businessKey,
                queueName,
                errorType,
                errorMessage,
                ex
        );

        log.error("MQ消息消费异常，messageId={}，businessType={}，businessKey={}，queue={}，errorType={}，原因={}",
                messageId,
                businessType,
                businessKey,
                queueName,
                errorType,
                errorMessage,
                ex
        );
    }
}
```

消费者异常处理建议：

1. 消费者必须区分可重试异常和不可重试异常。
2. 参数错误、格式错误不建议重新入队。
3. 临时网络异常、数据库异常可以有限重试。
4. 消费失败后不要直接 Ack，除非业务明确可忽略。
5. 手动 Ack 模式下所有异常分支都必须明确 Ack、Nack 或 Reject。
6. 消费成功但 Ack 失败时，依赖幂等控制处理后续重复投递。

### 序列化异常

序列化异常包括生产者将 Java 对象转换为 JSON 失败，以及消费者将 JSON 反序列化为 Java 对象失败。序列化异常通常说明消息结构、时间格式、字段类型或版本兼容性存在问题。

常见序列化异常如下：

| 异常场景            | 说明                                     |
| ------------------- | ---------------------------------------- |
| Java 时间类型未配置 | `LocalDateTime` 无法正确序列化或反序列化 |
| 字段类型不匹配      | JSON 中是字符串，DTO 中是数字            |
| 消息体不是合法 JSON | 消费者无法解析                           |
| 消息版本不兼容      | 消费端 DTO 与生产端结构不一致            |
| 未知字段失败        | 消费端未忽略未知字段                     |
| 循环引用            | 对象嵌套导致序列化异常                   |

推荐全局 ObjectMapper 配置。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/MqJacksonConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MQ Jackson 配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class MqJacksonConfig {

    /**
     * 配置 ObjectMapper
     *
     * @return ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 支持 LocalDateTime、LocalDate、LocalTime
        objectMapper.registerModule(new JavaTimeModule());

        // 日期不输出为时间戳
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 反序列化时忽略未知字段，提升兼容性
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // 空字段不输出
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        return objectMapper;
    }
}
```

消息转换器配置。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/MqMessageConverterConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MQ 消息转换器配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class MqMessageConverterConfig {

    /**
     * 配置 JSON 消息转换器
     *
     * @param objectMapper JSON转换器
     * @return 消息转换器
     */
    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
```

序列化异常处理建议：

1. 消息对象不要直接使用数据库 Entity。
2. 消费端应忽略未知字段。
3. 时间字段格式必须统一。
4. 枚举字段新增时，消费端应有默认分支。
5. 生产者和消费者应基于消息版本处理兼容性。
6. 反序列化失败的消息通常不应无限重试，应进入死信或异常记录表。

### 网络异常

网络异常主要包括 RabbitMQ 不可达、连接超时、认证失败、vhost 不存在、Channel 异常、Broker 重启、网络抖动等。网络异常可能影响生产发送，也可能影响消费者连接和 Ack。

常见网络异常场景如下：

| 场景            | 说明                                           |
| --------------- | ---------------------------------------------- |
| RabbitMQ 未启动 | 应用连接失败                                   |
| 网络不通        | 连接超时                                       |
| 账号密码错误    | 认证失败                                       |
| vhost 不存在    | 连接被拒绝                                     |
| 权限不足        | 无法访问 Exchange 或 Queue                     |
| Broker 重启     | 连接断开后自动恢复                             |
| Channel 被关闭  | 参数不一致、Exchange 不存在等导致 Channel 异常 |

推荐连接配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  rabbitmq:
    addresses: amqp://app_user:password@rabbitmq-01:5672,amqp://app_user:password@rabbitmq-02:5672
    virtual-host: /prod
    connection-timeout: 10s
    requested-heartbeat: 30s
    cache:
      channel:
        size: 50
    template:
      retry:
        enabled: true
        initial-interval: 1s
        max-attempts: 3
        max-interval: 5s
        multiplier: 2
```

网络异常处理建议：

1. 生产者发送失败应写入本地消息表，等待补偿重发。
2. 消费者连接断开后应依赖 Spring AMQP 自动恢复。
3. 不要在业务代码中频繁创建连接。
4. 连接失败要记录 RabbitMQ 地址、vhost、用户名和异常原因。
5. 生产环境建议配置多个 RabbitMQ 节点地址。
6. 网络异常期间要关注消息堆积和消费者在线数量。
7. 认证失败、权限不足、vhost 不存在属于配置问题，不应通过重试掩盖。

### 路由异常

路由异常主要指消息到达 Exchange 后无法路由到 Queue，或者发送时 Exchange 不存在、Routing Key 错误、Binding 缺失、Exchange 类型不匹配等。

常见路由异常如下：

| 异常              | 说明                       | 处理方式                    |
| ----------------- | -------------------------- | --------------------------- |
| Exchange 不存在   | 发送目标交换机不存在       | Confirm 失败或 Channel 异常 |
| Routing Key 错误  | 没有匹配绑定               | ReturnCallback 记录         |
| Binding 缺失      | Exchange 未绑定目标队列    | 创建 Binding 后重发         |
| vhost 错误        | 资源在其他 vhost           | 修正连接配置                |
| Exchange 类型错误 | Direct、Topic 使用方式错误 | 修正路由设计                |
| 队列参数冲突      | 已有队列参数与代码声明不同 | 走变更流程处理              |

ReturnCallback 统一记录示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/MqReturnExceptionConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.rabbitmq.service.MqExceptionRecordService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * MQ Return 路由异常配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqReturnExceptionConfig {

    private final RabbitTemplate rabbitTemplate;

    private final MqExceptionRecordService mqExceptionRecordService;

    /**
     * 初始化 Return 回调
     */
    @PostConstruct
    public void initReturnCallback() {
        rabbitTemplate.setReturnsCallback(returned -> {
            Message message = returned.getMessage();
            String messageId = StrUtil.blankToDefault(message.getMessageProperties().getMessageId(), "unknown");
            String businessType = String.valueOf(message.getMessageProperties().getHeaders().get("x-business-type"));
            String businessKey = String.valueOf(message.getMessageProperties().getHeaders().get("x-business-key"));

            String errorMessage = StrUtil.format(
                    "消息不可路由，replyCode={}，replyText={}",
                    returned.getReplyCode(),
                    returned.getReplyText()
            );

            mqExceptionRecordService.recordProducerException(
                    messageId,
                    businessType,
                    businessKey,
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    "ROUTING_ERROR",
                    errorMessage,
                    null
            );

            log.error("MQ路由异常，messageId={}，businessType={}，businessKey={}，exchange={}，routingKey={}，replyCode={}，replyText={}",
                    messageId,
                    businessType,
                    businessKey,
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    returned.getReplyCode(),
                    returned.getReplyText()
            );
        });
    }
}
```

路由异常处理建议：

1. 核心业务必须开启 `publisher-returns=true` 和 `mandatory=true`。
2. Return 异常一般不建议立即自动重发，应先修复路由配置。
3. 路由异常必须记录 Exchange、Routing Key、ReplyCode、ReplyText。
4. 管理端应支持查询不可路由消息。
5. 修复 Binding 或 Routing Key 后再人工重发。
6. 发布前可以通过环境初始化脚本确保 Exchange、Queue、Binding 已创建。

### 业务异常

业务异常是消费者处理消息时最常见的异常类型，例如订单状态不允许流转、库存不足、支付单不存在、用户不存在、通知模板不存在等。业务异常需要区分“可恢复”和“不可恢复”。

业务异常分类如下：

| 类型       | 示例                     | 是否重试 | 处理方式             |
| ---------- | ------------------------ | -------- | -------------------- |
| 参数异常   | 消息缺少订单号           | 否       | 进入死信             |
| 状态异常   | 订单已取消却收到支付成功 | 视业务   | 状态机判断或人工处理 |
| 资源不足   | 库存不足                 | 视业务   | 业务补偿或人工处理   |
| 临时异常   | 数据库超时               | 是       | 有限重试             |
| 第三方异常 | 短信平台超时             | 是       | 有限重试             |
| 重复消息   | 已处理过                 | 否       | Ack 并记录忽略       |

推荐定义业务异常基类，并区分是否可重试。

文件位置：`src/main/java/io/github/atengk/rabbitmq/exception/MqBusinessException.java`

```java
package io.github.atengk.rabbitmq.exception;

/**
 * MQ 业务异常
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class MqBusinessException extends RuntimeException {

    private final boolean retryable;

    public MqBusinessException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public MqBusinessException(String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.retryable = retryable;
    }

    /**
     * 是否可重试
     *
     * @return 是否可重试
     */
    public boolean isRetryable() {
        return retryable;
    }
}
```

消费者中按业务异常类型处理。

```java
try {
    handleBusiness(payload);
    channel.basicAck(deliveryTag, false);
} catch (MqBusinessException ex) {
    if (ex.isRetryable()) {
        log.error("MQ业务异常，可进入重试或死信，messageId={}，原因={}", messageId, ex.getMessage(), ex);
        channel.basicNack(deliveryTag, false, false);
        return;
    }

    log.error("MQ业务异常，不可重试，messageId={}，原因={}", messageId, ex.getMessage(), ex);
    channel.basicNack(deliveryTag, false, false);
}
```

业务异常处理建议：

1. 业务异常要有明确错误码或异常类型。
2. 不可恢复业务异常不要无限重试。
3. 状态异常要结合业务状态机判断。
4. 重复消费不是错误，应 Ack 并记录幂等命中日志。
5. 核心业务异常进入死信后需要支持人工处理。
6. 业务异常日志必须包含业务主键，便于业务人员排查。

### 全局异常记录

全局异常记录用于统一保存 MQ 异常信息。建议使用一张 MQ 异常记录表，记录异常来源、异常类型、消息 ID、业务主键、Exchange、Routing Key、Queue、异常堆栈、处理状态等。

异常记录表建议如下。

```sql
CREATE TABLE mq_exception_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    exception_id VARCHAR(100) NOT NULL COMMENT '异常记录ID',
    message_id VARCHAR(100) DEFAULT NULL COMMENT '消息ID',
    business_type VARCHAR(64) DEFAULT NULL COMMENT '业务类型',
    business_key VARCHAR(128) DEFAULT NULL COMMENT '业务主键',
    exception_source VARCHAR(32) NOT NULL COMMENT '异常来源：PRODUCER/CONSUMER/SYSTEM',
    exception_type VARCHAR(64) NOT NULL COMMENT '异常类型',
    exchange_name VARCHAR(128) DEFAULT NULL COMMENT '交换机',
    routing_key VARCHAR(128) DEFAULT NULL COMMENT '路由键',
    queue_name VARCHAR(128) DEFAULT NULL COMMENT '队列名称',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '异常消息',
    stack_trace LONGTEXT DEFAULT NULL COMMENT '异常堆栈',
    handle_status VARCHAR(32) NOT NULL COMMENT '处理状态：PENDING/DONE/IGNORED',
    create_time DATETIME(3) NOT NULL COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_exception_id (exception_id),
    KEY idx_message_id (message_id),
    KEY idx_business_type_key (business_type, business_key),
    KEY idx_exception_type (exception_type),
    KEY idx_handle_status (handle_status),
    KEY idx_create_time (create_time)
) COMMENT='MQ异常记录表';
```

异常记录服务接口如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/service/MqExceptionRecordService.java`

```java
package io.github.atengk.rabbitmq.service;

/**
 * MQ 异常记录服务
 *
 * @author Ateng
 * @since 2026-05-11
 */
public interface MqExceptionRecordService {

    /**
     * 记录生产者异常
     *
     * @param messageId    消息ID
     * @param businessType 业务类型
     * @param businessKey  业务主键
     * @param exchange     交换机
     * @param routingKey   路由键
     * @param errorType    异常类型
     * @param errorMessage 异常消息
     * @param throwable    异常对象
     */
    void recordProducerException(String messageId, String businessType, String businessKey, String exchange,
                                 String routingKey, String errorType, String errorMessage, Throwable throwable);

    /**
     * 记录消费者异常
     *
     * @param messageId    消息ID
     * @param businessType 业务类型
     * @param businessKey  业务主键
     * @param queueName    队列名称
     * @param errorType    异常类型
     * @param errorMessage 异常消息
     * @param throwable    异常对象
     */
    void recordConsumerException(String messageId, String businessType, String businessKey, String queueName,
                                 String errorType, String errorMessage, Throwable throwable);
}
```

下面的实现先使用日志模拟落库，实际项目中替换为 Mapper 或 Repository 保存数据库。

文件位置：`src/main/java/io/github/atengk/rabbitmq/service/impl/LogMqExceptionRecordServiceImpl.java`

```java
package io.github.atengk.rabbitmq.service.impl;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.IdUtil;
import io.github.atengk.rabbitmq.service.MqExceptionRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 日志型 MQ 异常记录服务实现
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
public class LogMqExceptionRecordServiceImpl implements MqExceptionRecordService {

    /**
     * 记录生产者异常
     *
     * @param messageId    消息ID
     * @param businessType 业务类型
     * @param businessKey  业务主键
     * @param exchange     交换机
     * @param routingKey   路由键
     * @param errorType    异常类型
     * @param errorMessage 异常消息
     * @param throwable    异常对象
     */
    @Override
    public void recordProducerException(String messageId, String businessType, String businessKey, String exchange,
                                        String routingKey, String errorType, String errorMessage, Throwable throwable) {
        String exceptionId = IdUtil.fastSimpleUUID();
        String stackTrace = throwable == null ? null : ExceptionUtil.stacktraceToString(throwable, 5000);

        log.error("记录MQ生产者异常，exceptionId={}，messageId={}，businessType={}，businessKey={}，exchange={}，routingKey={}，errorType={}，errorMessage={}，stackTrace={}",
                exceptionId,
                messageId,
                businessType,
                businessKey,
                exchange,
                routingKey,
                errorType,
                errorMessage,
                stackTrace
        );
    }

    /**
     * 记录消费者异常
     *
     * @param messageId    消息ID
     * @param businessType 业务类型
     * @param businessKey  业务主键
     * @param queueName    队列名称
     * @param errorType    异常类型
     * @param errorMessage 异常消息
     * @param throwable    异常对象
     */
    @Override
    public void recordConsumerException(String messageId, String businessType, String businessKey, String queueName,
                                        String errorType, String errorMessage, Throwable throwable) {
        String exceptionId = IdUtil.fastSimpleUUID();
        String stackTrace = throwable == null ? null : ExceptionUtil.stacktraceToString(throwable, 5000);

        log.error("记录MQ消费者异常，exceptionId={}，messageId={}，businessType={}，businessKey={}，queueName={}，errorType={}，errorMessage={}，stackTrace={}",
                exceptionId,
                messageId,
                businessType,
                businessKey,
                queueName,
                errorType,
                errorMessage,
                stackTrace
        );
    }
}
```

全局异常记录建议：

1. 异常记录表不替代业务表，只用于排查和补偿。
2. 异常堆栈可以截断保存，避免单条记录过大。
3. 核心异常应触发告警，例如 Confirm 失败、Return、死信堆积。
4. 管理端应支持按异常类型、业务主键、消息 ID 查询。
5. 异常处理完成后应更新 `handle_status`。
6. 高频异常需要聚合告警，避免告警风暴。

## 日志设计

本章节用于说明 RabbitMQ 消息链路中的日志规范。良好的日志设计应覆盖消息发送、接收、确认、失败、重试、死信和链路追踪。所有关键日志都应包含 `messageId` 和业务主键，保证从生产者到消费者可以完整串联。

推荐日志字段如下：

| 字段           | 说明         |
| -------------- | ------------ |
| `messageId`    | 消息唯一 ID  |
| `businessType` | 业务类型     |
| `businessKey`  | 业务主键     |
| `eventType`    | 事件类型     |
| `exchange`     | 交换机       |
| `routingKey`   | 路由键       |
| `queue`        | 队列         |
| `deliveryTag`  | 消费投递标签 |
| `traceId`      | 链路追踪 ID  |
| `costMs`       | 处理耗时     |
| `status`       | 当前处理状态 |
| `errorMessage` | 异常原因     |

### 发送日志

发送日志用于记录生产者提交消息的行为。它不代表 Broker 已经确认消息，只表示应用已经调用发送逻辑。

发送日志建议字段：

| 字段           | 说明     |
| -------------- | -------- |
| `messageId`    | 消息 ID  |
| `businessType` | 业务类型 |
| `businessKey`  | 业务主键 |
| `eventType`    | 事件类型 |
| `exchange`     | 交换机   |
| `routingKey`   | 路由键   |
| `sendTime`     | 发送时间 |

发送日志示例：

```text
MQ消息提交发送成功，messageId=order_paid_20260511103000_xxx，businessType=order_paid，businessKey=ORDER202605110001，eventType=order.paid，exchange=business.order.exchange，routingKey=order.paid
```

发送日志代码示例：

```java
log.info("MQ消息提交发送成功，messageId={}，businessType={}，businessKey={}，eventType={}，exchange={}，routingKey={}",
        messageId,
        businessType,
        businessKey,
        eventType,
        exchange,
        routingKey
);
```

发送日志建议：

1. 发送成功使用 `INFO`。
2. 发送异常使用 `ERROR`。
3. 高频非核心消息可以将成功日志降为 `DEBUG`。
4. 不建议完整打印大消息体。
5. 敏感字段必须脱敏后再打印。
6. 发送日志要和 Confirm 日志、Return 日志使用同一个 `messageId`。

### 接收日志

接收日志用于记录消费者收到消息的行为。它表示消息已经投递到消费者，但不代表业务处理成功。

接收日志建议字段：

| 字段           | 说明         |
| -------------- | ------------ |
| `messageId`    | 消息 ID      |
| `businessType` | 业务类型     |
| `businessKey`  | 业务主键     |
| `queue`        | 消费队列     |
| `deliveryTag`  | 投递标签     |
| `redelivered`  | 是否重复投递 |
| `consumer`     | 消费服务名称 |

接收日志示例：

```text
收到MQ消息，messageId=order_paid_20260511103000_xxx，businessType=order_paid，businessKey=ORDER202605110001，queue=order.paid.queue，deliveryTag=1，redelivered=false
```

接收日志代码示例：

```java
log.info("收到MQ消息，messageId={}，businessType={}，businessKey={}，queue={}，deliveryTag={}，redelivered={}",
        messageId,
        businessType,
        businessKey,
        message.getMessageProperties().getConsumerQueue(),
        message.getMessageProperties().getDeliveryTag(),
        message.getMessageProperties().isRedelivered()
);
```

接收日志建议：

1. 消费开始时先打印接收日志。
2. 日志中必须包含队列名。
3. 重复投递时 `redelivered=true`，需要重点关注。
4. 多队列监听时必须打印 `consumerQueue`。
5. 不建议在接收日志中打印完整大消息体。

### 确认日志

确认日志包括生产端 Publisher Confirm、Publisher Return，以及消费端 Ack、Nack、Reject。确认日志用于判断消息链路走到了哪个阶段。

Confirm 成功日志：

```text
MQ消息发布确认成功，messageId=order_paid_20260511103000_xxx，exchange=business.order.exchange
```

Confirm 失败日志：

```text
MQ消息发布确认失败，messageId=order_paid_20260511103000_xxx，原因=channel error
```

消费 Ack 日志：

```text
MQ消息Ack成功，messageId=order_paid_20260511103000_xxx，businessKey=ORDER202605110001，queue=order.paid.queue，deliveryTag=1
```

确认日志代码示例：

```java
log.info("MQ消息Ack成功，messageId={}，businessKey={}，queue={}，deliveryTag={}",
        messageId,
        businessKey,
        queueName,
        deliveryTag
);
```

确认日志建议：

1. Confirm 成功可以使用 `INFO` 或高频场景使用 `DEBUG`。
2. Confirm 失败必须使用 `ERROR`。
3. Return 不可路由必须使用 `ERROR`。
4. Ack 成功可以使用 `INFO`。
5. Nack 和 Reject 应使用 `WARN` 或 `ERROR`，取决于业务重要性。
6. 确认日志必须包含 `messageId`。

### 失败日志

失败日志用于记录消息发送失败、消费失败、序列化失败、路由失败、重试失败和死信处理失败。失败日志必须能支撑排查，不应只输出“消费失败”这类模糊信息。

失败日志建议字段：

| 字段                        | 说明     |
| --------------------------- | -------- |
| `messageId`                 | 消息 ID  |
| `businessType`              | 业务类型 |
| `businessKey`               | 业务主键 |
| `errorType`                 | 异常类型 |
| `errorMessage`              | 异常原因 |
| `exchange/routingKey/queue` | 消息位置 |
| `retryCount`                | 重试次数 |
| `stackTrace`                | 异常堆栈 |

失败日志示例：

```text
MQ消息消费失败，messageId=order_paid_20260511103000_xxx，businessType=order_paid，businessKey=ORDER202605110001，queue=order.paid.queue，errorType=BUSINESS_ERROR，retryCount=2，原因=订单状态不允许支付
```

失败日志代码示例：

```java
log.error("MQ消息消费失败，messageId={}，businessType={}，businessKey={}，queue={}，errorType={}，retryCount={}，原因={}",
        messageId,
        businessType,
        businessKey,
        queueName,
        errorType,
        retryCount,
        ex.getMessage(),
        ex
);
```

失败日志建议：

1. 失败日志必须打印异常堆栈。
2. 业务异常要打印业务主键。
3. 路由异常要打印 Exchange 和 Routing Key。
4. 消费异常要打印 Queue 和 deliveryTag。
5. 序列化异常要记录消息体摘要。
6. 失败日志不能吞掉原始异常。

### 重试日志

重试日志用于记录消息补偿重发或消费重试过程。重试日志需要包含当前重试次数、最大重试次数、下次重试时间和失败原因。

重试日志建议字段：

| 字段            | 说明         |
| --------------- | ------------ |
| `messageId`     | 消息 ID      |
| `businessKey`   | 业务主键     |
| `retrySource`   | 重试来源     |
| `retryCount`    | 当前重试次数 |
| `maxRetryCount` | 最大重试次数 |
| `nextRetryTime` | 下次重试时间 |
| `retryStatus`   | 重试状态     |
| `errorMessage`  | 异常原因     |

重试开始日志：

```text
开始重试MQ消息，messageId=order_paid_20260511103000_xxx，retrySource=CONFIRM_FAILED，retryCount=2，maxRetryCount=5
```

重试成功日志：

```text
MQ消息重试提交成功，messageId=order_paid_20260511103000_xxx，retryCount=2
```

重试失败日志：

```text
MQ消息重试失败，messageId=order_paid_20260511103000_xxx，retryCount=2，nextRetryTime=2026-05-11 10:35:00，原因=Connection refused
```

重试日志建议：

1. 每次重试都要记录日志。
2. 超过最大重试次数必须使用 `ERROR` 并告警。
3. 重试成功不等于最终消费成功，只表示重新提交发送。
4. Return 消息重试前应确认路由配置已修复。
5. 死信重发前应记录操作人和原因。

### 死信日志

死信日志用于记录消息进入死信队列后的处理情况。死信日志必须保留原队列、死信原因、死信队列、消息 ID、业务主键和处理状态。

死信日志建议字段：

| 字段            | 说明              |
| --------------- | ----------------- |
| `messageId`     | 原消息 ID         |
| `deadMessageId` | 死信记录 ID       |
| `businessKey`   | 业务主键          |
| `originalQueue` | 原队列            |
| `deadQueue`     | 死信队列          |
| `deadReason`    | 死信原因          |
| `xDeath`        | RabbitMQ 死信历史 |
| `handleStatus`  | 处理状态          |

死信接收日志：

```text
收到死信消息，deadMessageId=dead_xxx，messageId=order_paid_20260511103000_xxx，businessKey=ORDER202605110001，originalQueue=order.paid.queue，deadQueue=order.paid.dead.queue，deadReason=rejected
```

死信处理日志：

```text
死信消息处理完成，deadMessageId=dead_xxx，messageId=order_paid_20260511103000_xxx，handleStatus=RETRIED，reason=修复业务参数后重发
```

死信日志建议：

1. 死信日志使用 `ERROR` 或 `WARN`。
2. 死信入库成功后记录 `INFO`。
3. 死信重发必须记录操作人和原因。
4. 死信忽略必须记录忽略原因。
5. 死信日志中必须包含原队列和死信原因。
6. 死信数量增长应接入告警。

### 链路追踪日志

链路追踪日志用于将 HTTP 请求、业务操作、MQ 发送、MQ 消费和下游调用串联起来。推荐在消息 Header 中传递 `traceId`，消费者收到消息后写入 MDC，使后续日志自动携带 traceId。

推荐 Header：

```text
x-trace-id
```

发送时设置 traceId：

```java
rabbitTemplate.convertAndSend(
        exchange,
        routingKey,
        payload,
        message -> {
            message.getMessageProperties().setHeader("x-trace-id", traceId);
            message.getMessageProperties().setHeader("x-message-id", messageId);
            message.getMessageProperties().setHeader("x-business-key", businessKey);
            return message;
        }
);
```

消费者中写入 MDC。

文件位置：`src/main/java/io/github/atengk/rabbitmq/consumer/TraceMessageConsumer.java`

```java
package io.github.atengk.rabbitmq.consumer;

import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 链路追踪消息消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class TraceMessageConsumer {

    /**
     * 消费带链路追踪的消息
     *
     * @param payload 消息体
     * @param message 原始消息
     * @param channel RabbitMQ Channel
     * @throws IOException Ack 或 Nack 失败时抛出
     */
    @RabbitListener(queues = "trace.business.queue")
    public void consume(String payload, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String traceId = String.valueOf(message.getMessageProperties().getHeaders().get("x-trace-id"));
        String messageId = StrUtil.blankToDefault(message.getMessageProperties().getMessageId(), "unknown");

        try {
            MDC.put("traceId", StrUtil.blankToDefault(traceId, messageId));
            MDC.put("messageId", messageId);

            log.info("收到链路追踪消息，traceId={}，messageId={}，payload={}", traceId, messageId, payload);

            handleBusiness(payload);

            channel.basicAck(deliveryTag, false);
            log.info("链路追踪消息消费成功，traceId={}，messageId={}", traceId, messageId);
        } catch (Exception ex) {
            log.error("链路追踪消息消费失败，traceId={}，messageId={}，原因={}",
                    traceId,
                    messageId,
                    ex.getMessage(),
                    ex
            );
            channel.basicNack(deliveryTag, false, false);
        } finally {
            MDC.remove("traceId");
            MDC.remove("messageId");
        }
    }

    /**
     * 处理业务逻辑
     *
     * @param payload 消息体
     */
    private void handleBusiness(String payload) {
        log.info("执行链路追踪业务处理，payload={}", payload);
    }
}
```

Logback 日志格式中加入 traceId 和 messageId。

文件位置：`src/main/resources/logback-spring.xml`

```xml
<property name="CONSOLE_LOG_PATTERN"
          value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{traceId}] [%X{messageId}] %logger{36} - %msg%n"/>
```

链路追踪建议：

1. HTTP 入口生成 `traceId`。
2. 发送 MQ 时将 `traceId` 写入 Header。
3. 消费者读取 Header 并写入 MDC。
4. 消费结束后必须清理 MDC，避免线程复用导致日志串号。
5. 生产者、消费者、补偿任务、死信处理都应传递 `messageId`。
6. 管理端查询时应支持按 `traceId` 和 `messageId` 检索完整链路。

完整 MQ 日志链路示例：

```text
HTTP请求进入，traceId=trace_001，orderId=ORDER202605110001
订单创建成功，traceId=trace_001，orderId=ORDER202605110001
MQ消息提交发送成功，traceId=trace_001，messageId=order_created_xxx，businessKey=ORDER202605110001
MQ消息发布确认成功，traceId=trace_001，messageId=order_created_xxx
收到MQ消息，traceId=trace_001，messageId=order_created_xxx，queue=order.created.queue
MQ消息消费成功，traceId=trace_001，messageId=order_created_xxx，businessKey=ORDER202605110001
```

最终建议：日志不只是排查工具，也是可靠消息体系的一部分。生产端、Broker 确认端、消费端、死信端、补偿端必须围绕同一个 `messageId` 建立可追踪链路。核心业务至少要做到“发得出去、查得到、失败能定位、异常能补偿”。

## 监控与运维

本章节用于说明 RabbitMQ 在生产环境中的监控与运维实践。RabbitMQ 不是“启动后不管”的中间件，实际运行中需要持续关注队列深度、消费速率、连接数、Channel 数量、消息确认、死信队列、应用日志和 Broker 资源使用情况。

RabbitMQ 监控目标如下：

| 监控目标         | 说明                                                    |
| ---------------- | ------------------------------------------------------- |
| 及时发现堆积     | 队列 Ready 消息持续增长需要告警                         |
| 及时发现消费异常 | Unacked、redeliver、死信数量异常增长需要排查            |
| 及时发现连接异常 | 连接数、Channel 数异常可能表示应用泄漏或频繁重连        |
| 及时发现路由异常 | Return、不可路由消息需要快速处理                        |
| 及时发现资源风险 | 内存、磁盘、水位、文件句柄需要持续监控                  |
| 支持故障定位     | 通过 messageId、businessKey、traceId 串联日志和消息记录 |

### RabbitMQ 管理控制台

RabbitMQ 管理控制台由 `rabbitmq_management` 插件提供，常用于查看队列、交换机、绑定关系、连接、Channel、消息速率、节点状态和用户权限。

启用管理插件：

```bash
# 启用 RabbitMQ 管理插件
rabbitmq-plugins enable rabbitmq_management

# 查看插件状态
rabbitmq-plugins list | grep management
```

Docker 启动管理版 RabbitMQ：

```bash
docker run -d \
  --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=admin \
  -e RABBITMQ_DEFAULT_PASS=admin123 \
  rabbitmq:3.13-management
```

管理控制台默认访问地址：

```text
http://localhost:15672
```

管理控制台常用页面如下：

| 页面               | 用途                                   |
| ------------------ | -------------------------------------- |
| Overview           | 查看整体消息速率、节点状态、资源水位   |
| Connections        | 查看客户端连接                         |
| Channels           | 查看 Channel、Unacked、Prefetch        |
| Exchanges          | 查看 Exchange 类型、绑定关系、消息流入 |
| Queues and Streams | 查看队列深度、消费者数量、消息速率     |
| Admin              | 管理用户、权限、Virtual Host、Policy   |

管理控制台重点关注：

| 指标         | 正常情况         | 异常信号                        |
| ------------ | ---------------- | ------------------------------- |
| Ready        | 稳定或短时间波动 | 持续增长表示堆积                |
| Unacked      | 随消费波动       | 长时间过高表示消费慢或 Ack 异常 |
| Consumers    | 大于 0           | 为 0 表示无人消费               |
| Publish rate | 与业务流量匹配   | 突增可能造成堆积                |
| Ack rate     | 接近消费速率     | 下降说明消费异常                |
| Redelivered  | 偶发             | 持续增长说明重复投递            |
| Memory       | 低于高水位       | 接近水位会触发流控              |
| Disk free    | 高于磁盘水位     | 过低可能触发阻塞                |

生产环境建议：

1. 管理控制台账号不要使用默认账号。
2. 不要将管理控制台直接暴露到公网。
3. 管理账号、应用账号、只读监控账号应分开。
4. 管理控制台适合排查，不适合作为唯一监控手段。
5. 生产环境应接入 Prometheus、Grafana、告警平台。

### 队列深度监控

队列深度是 RabbitMQ 最重要的监控指标之一。它反映队列中等待消费、正在消费和总消息数量。

关键指标如下：

| 指标                      | 说明                               |
| ------------------------- | ---------------------------------- |
| `messages_ready`          | 等待投递给消费者的消息数           |
| `messages_unacknowledged` | 已投递但尚未确认的消息数           |
| `messages`                | 队列总消息数，等于 Ready + Unacked |
| `consumers`               | 当前队列消费者数量                 |

命令查看队列深度：

```bash
rabbitmqctl list_queues -p /dev \
  name messages messages_ready messages_unacknowledged consumers
```

输出示例：

```text
order.paid.queue       1200    1000    200     4
order.paid.dead.queue  12      12      0       1
```

队列深度排查逻辑：

| 现象                           | 可能原因                        | 处理方式               |
| ------------------------------ | ------------------------------- | ---------------------- |
| Ready 持续增长，Consumers 为 0 | 消费者未启动或监听队列错误      | 启动消费者，检查队列名 |
| Ready 持续增长，Consumers 正常 | 消费能力不足                    | 扩容消费者或优化业务   |
| Unacked 持续很高               | 消费慢、Ack 异常、Prefetch 过大 | 检查消费耗时和 Ack     |
| Dead Queue 增长                | 消费失败、TTL 过期、队列超长    | 查看死信原因           |
| Ready 突增后恢复               | 正常流量峰值                    | 观察是否持续堆积       |

告警建议：

| 队列类型     | 告警条件示例                  |
| ------------ | ----------------------------- |
| 核心业务队列 | Ready 连续 5 分钟大于 10000   |
| 支付队列     | Ready 连续 3 分钟大于 1000    |
| 死信队列     | Ready 大于 0 或持续增长       |
| 日志队列     | Ready 连续 10 分钟大于 100000 |
| 延迟队列     | Ready 异常高于业务预期        |

队列深度监控建议：

1. 不同队列设置不同告警阈值。
2. 核心业务队列阈值应更严格。
3. 死信队列建议一有增长就告警。
4. 只看 Ready 不够，还要看 Unacked。
5. 队列深度需要结合生产速率和消费速率判断。

### 消费速率监控

消费速率用于判断消费者处理能力是否满足生产流量。常见指标包括消息投递速率、Ack 速率、消费失败速率、重投递速率等。

关键指标如下：

| 指标                 | 说明                         |
| -------------------- | ---------------------------- |
| Publish rate         | 生产者发送到 RabbitMQ 的速率 |
| Deliver rate         | RabbitMQ 投递给消费者的速率  |
| Ack rate             | 消费者确认消息的速率         |
| Redeliver rate       | 消息重复投递速率             |
| Get rate             | 手动拉取消息速率             |
| Consumer utilisation | 消费者利用率                 |

消费速率判断：

```text
如果 Publish rate > Ack rate，并且 Ready 持续增长，说明消费能力不足。
如果 Deliver rate 高但 Ack rate 低，说明消费者拿到消息后处理慢或 Ack 异常。
如果 Redeliver rate 持续增长，说明消费失败、Nack 或消费者异常重启较多。
```

排查建议：

| 现象                  | 可能原因                       |
| --------------------- | ------------------------------ |
| Ack rate 下降         | 消费者处理变慢                 |
| Deliver rate 为 0     | 没有消费者或消费者无法接收     |
| Redeliver rate 高     | 消费异常、消费者重启、Ack 失败 |
| Publish rate 突增     | 业务流量突增或补偿任务集中发送 |
| Ack rate 低于生产速率 | 消费能力不足                   |

应用侧也应记录消费耗时：

```java
long startTime = System.currentTimeMillis();

try {
    handleBusiness(payload);
    long costMs = System.currentTimeMillis() - startTime;
    log.info("MQ消息消费成功，messageId={}，businessKey={}，costMs={}", messageId, businessKey, costMs);
} catch (Exception ex) {
    long costMs = System.currentTimeMillis() - startTime;
    log.error("MQ消息消费失败，messageId={}，businessKey={}，costMs={}，原因={}",
            messageId,
            businessKey,
            costMs,
            ex.getMessage(),
            ex
    );
}
```

消费速率监控建议：

1. 监控 RabbitMQ 层面的 Ack rate。
2. 监控应用层面的消费耗时 P95、P99。
3. 消费速率下降时先排查应用日志和下游服务。
4. 生产速率突增时关注队列深度变化。
5. Redeliver rate 持续增长必须排查异常消息。

### 连接数监控

连接数用于观察应用与 RabbitMQ Broker 的 TCP 连接情况。连接数异常增长可能表示应用频繁创建连接、连接泄漏、重连风暴或部署实例异常扩容。

查看连接：

```bash
rabbitmqctl list_connections \
  name user vhost peer_host peer_port state channels recv_cnt send_cnt connected_at
```

连接指标说明：

| 指标                | 说明                      |
| ------------------- | ------------------------- |
| `name`              | 连接名称                  |
| `user`              | 连接使用的 RabbitMQ 用户  |
| `vhost`             | 连接所在 Virtual Host     |
| `peer_host`         | 客户端 IP                 |
| `state`             | 连接状态                  |
| `channels`          | 当前连接上的 Channel 数量 |
| `recv_cnt/send_cnt` | 收发数据计数              |

连接数异常场景：

| 现象             | 可能原因                       |
| ---------------- | ------------------------------ |
| 连接数持续增长   | 应用频繁创建连接未释放         |
| 连接反复断开重连 | 网络不稳定或 Broker 压力大     |
| 单个应用连接过多 | 未复用 ConnectionFactory       |
| 大量 idle 连接   | 应用实例过多或连接池配置不合理 |
| 连接来自未知 IP  | 可能存在未授权客户端           |

Spring Boot 应用通常通过 `CachingConnectionFactory` 复用连接，不应在业务代码中手动频繁创建 RabbitMQ 连接。

连接监控建议：

1. 按应用服务统计连接数。
2. 应用实例数和连接数应基本匹配。
3. 连接数突增需要检查发布版本或异常重连。
4. 生产环境应限制用户连接权限和 vhost 权限。
5. 连接异常断开要结合应用日志和 Broker 日志排查。

### Channel 监控

Channel 是 RabbitMQ 中执行消息发布、消费、确认等操作的逻辑通道。一个 Connection 可以包含多个 Channel。Channel 数量过多可能导致 Broker 资源消耗增加。

查看 Channel：

```bash
rabbitmqctl list_channels \
  connection number user vhost transactional confirm consumer_count messages_unacknowledged prefetch_count
```

Channel 指标说明：

| 指标                      | 说明                      |
| ------------------------- | ------------------------- |
| `connection`              | 所属连接                  |
| `number`                  | Channel 编号              |
| `transactional`           | 是否事务模式              |
| `confirm`                 | 是否 Confirm 模式         |
| `consumer_count`          | 当前 Channel 上消费者数量 |
| `messages_unacknowledged` | 当前 Channel 未确认消息数 |
| `prefetch_count`          | 当前 Channel 预取数量     |

Channel 异常场景：

| 现象                       | 可能原因                                |
| -------------------------- | --------------------------------------- |
| Channel 数持续增长         | 应用创建 Channel 后未释放               |
| Channel 频繁关闭           | 队列参数冲突、Exchange 不存在、权限异常 |
| Unacked 集中在某个 Channel | 某个消费者处理慢                        |
| prefetch 过大              | 单个消费者持有大量消息                  |
| transactional=true         | 使用 RabbitMQ 事务，可能影响性能        |

Channel 监控建议：

1. 正常应用应复用连接和 Channel 缓存。
2. 频繁 Channel 异常关闭要查看 Broker 日志。
3. 单个 Channel Unacked 过高时，排查对应消费者。
4. 不建议在高吞吐生产场景使用 RabbitMQ 事务模式。
5. Channel 缓存大小应根据生产和消费并发合理配置。

Spring Boot Channel 缓存配置示例：

```yaml
spring:
  rabbitmq:
    cache:
      channel:
        # Channel 缓存数量，根据并发和发送压力调整
        size: 50
```

### 消息确认监控

消息确认监控包括生产者 Confirm、生产者 Return、消费者 Ack、消费者 Nack、Reject、Redeliver 等。确认机制直接关系到消息可靠性。

生产端确认指标：

| 指标           | 说明                     |
| -------------- | ------------------------ |
| Confirm 成功数 | 消息到达 Exchange        |
| Confirm 失败数 | 消息未被 Broker 确认     |
| Return 数量    | 消息不可路由             |
| 发送异常数     | 调用 RabbitTemplate 异常 |

消费端确认指标：

| 指标           | 说明             |
| -------------- | ---------------- |
| Ack 数量       | 消费成功确认     |
| Nack 数量      | 消费拒绝         |
| Reject 数量    | 消费拒绝单条消息 |
| Redeliver 数量 | 重复投递         |
| Unacked 数量   | 已投递未确认     |

应用中可以通过 Micrometer 记录确认指标。

文件位置：`src/main/java/io/github/atengk/rabbitmq/monitor/MqMetricsService.java`

```java
package io.github.atengk.rabbitmq.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * MQ 指标记录服务
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Component
public class MqMetricsService {

    private final Counter confirmSuccessCounter;

    private final Counter confirmFailedCounter;

    private final Counter returnCounter;

    private final Counter consumeSuccessCounter;

    private final Counter consumeFailedCounter;

    public MqMetricsService(MeterRegistry meterRegistry) {
        this.confirmSuccessCounter = Counter.builder("mq_confirm_success_total")
                .description("MQ发布确认成功次数")
                .register(meterRegistry);
        this.confirmFailedCounter = Counter.builder("mq_confirm_failed_total")
                .description("MQ发布确认失败次数")
                .register(meterRegistry);
        this.returnCounter = Counter.builder("mq_return_total")
                .description("MQ不可路由消息数量")
                .register(meterRegistry);
        this.consumeSuccessCounter = Counter.builder("mq_consume_success_total")
                .description("MQ消费成功次数")
                .register(meterRegistry);
        this.consumeFailedCounter = Counter.builder("mq_consume_failed_total")
                .description("MQ消费失败次数")
                .register(meterRegistry);
    }

    public void incrementConfirmSuccess() {
        confirmSuccessCounter.increment();
    }

    public void incrementConfirmFailed() {
        confirmFailedCounter.increment();
    }

    public void incrementReturn() {
        returnCounter.increment();
    }

    public void incrementConsumeSuccess() {
        consumeSuccessCounter.increment();
    }

    public void incrementConsumeFailed() {
        consumeFailedCounter.increment();
    }
}
```

确认监控建议：

1. Confirm 失败必须告警。
2. Return 数量大于 0 必须排查路由配置。
3. Nack、Reject 持续增长要排查消费者异常。
4. Redeliver 持续增长说明重复投递严重。
5. Unacked 持续高位说明消费端处理慢或 Ack 异常。
6. 关键确认指标应同时记录到日志和指标系统。

### 死信队列监控

死信队列监控用于发现消费失败、消息过期、队列超长等异常情况。死信队列一旦有消息，通常代表业务消息出现了不可自动处理的问题。

死信队列关键指标：

| 指标           | 说明                            |
| -------------- | ------------------------------- |
| 死信队列 Ready | 待处理死信数量                  |
| 死信消费速率   | 死信消费者处理速度              |
| 死信增长速率   | 单位时间新增死信数量            |
| 死信原因分布   | rejected、expired、maxlen 等    |
| 死信处理状态   | PENDING、RETRIED、IGNORED、DONE |

查看死信队列：

```bash
rabbitmqctl list_queues -p /dev \
  name messages_ready messages_unacknowledged consumers | grep dead
```

死信告警建议：

| 场景             | 告警级别   |
| ---------------- | ---------- |
| 支付死信大于 0   | 严重       |
| 订单死信持续增长 | 严重       |
| 库存死信大于阈值 | 严重       |
| 通知死信持续增长 | 警告       |
| 日志死信增长     | 视业务设置 |

死信监控建议：

1. 核心业务死信队列 Ready 大于 0 即告警。
2. 死信消费者本身也要有异常监控。
3. 死信入库失败必须告警。
4. 死信重发失败需要记录重试记录。
5. 管理端应展示死信数量趋势和原因分布。
6. 死信长期不处理会掩盖业务问题，不应只清空队列。

### 应用日志监控

RabbitMQ 应用日志监控用于从业务应用侧观察发送、确认、消费、重试和死信处理情况。Broker 只知道消息状态，不理解业务语义，因此应用日志是排查业务问题的核心依据。

建议监控的日志关键字：

| 关键字                     | 说明           |
| -------------------------- | -------------- |
| `MQ消息发送异常`           | 生产者发送失败 |
| `MQ消息发布确认失败`       | Confirm 失败   |
| `MQ消息不可路由`           | Return 发生    |
| `MQ消息消费失败`           | 消费者业务异常 |
| `MQ消息重试失败`           | 补偿重试失败   |
| `收到死信消息`             | 死信入库或处理 |
| `消息已消费，跳过重复处理` | 幂等命中       |
| `消息体不能为空`           | 消息格式异常   |

日志告警建议：

| 日志类型        | 告警条件               |
| --------------- | ---------------------- |
| Confirm 失败    | 出现即告警             |
| Return 不可路由 | 出现即告警             |
| 核心消费失败    | 连续出现或数量超过阈值 |
| 死信消息        | 出现即告警             |
| 序列化异常      | 出现即告警             |
| 重试失败        | 超过最大重试次数告警   |

日志规范建议：

1. 所有日志必须包含 `messageId`。
2. 核心业务日志必须包含 `businessKey`。
3. 消费日志必须包含 `queue`。
4. 路由日志必须包含 `exchange` 和 `routingKey`。
5. 异常日志必须打印堆栈。
6. 通过 `traceId` 串联 HTTP 请求和 MQ 链路。

## Spring Boot Actuator 集成

本章节用于说明 Spring Boot 3 项目中如何集成 Actuator，对 RabbitMQ 连接、应用状态、自定义健康检查和 Prometheus 指标进行暴露。Actuator 主要负责应用侧观测，RabbitMQ Broker 指标仍建议通过 RabbitMQ Management、Prometheus 插件或 exporter 采集。

### Actuator 依赖配置

在 Spring Boot 项目中添加 Actuator 和 Prometheus 依赖。

文件位置：`pom.xml`

```xml
<!-- Spring Boot Actuator：提供健康检查、指标、应用信息等端点 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Micrometer Prometheus：将应用指标暴露为 Prometheus 格式 -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

Actuator 基础配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        # 生产环境建议按需开放，不建议直接暴露所有端点
        include: health,info,metrics,prometheus
  endpoint:
    health:
      # 显示健康检查明细
      show-details: when_authorized
  metrics:
    tags:
      application: rabbitmq-demo
```

常用端点：

| 端点                       | 说明                |
| -------------------------- | ------------------- |
| `/actuator/health`         | 应用健康状态        |
| `/actuator/info`           | 应用信息            |
| `/actuator/metrics`        | 应用指标列表        |
| `/actuator/metrics/{name}` | 查看指定指标        |
| `/actuator/prometheus`     | Prometheus 指标格式 |

生产环境建议：

1. Actuator 端点必须加认证或网关访问控制。
2. 不建议暴露 `env`、`beans`、`configprops` 等敏感端点。
3. Prometheus 抓取端点可通过内网开放。
4. 健康检查详情不应对公网匿名用户展示。
5. 应用名、环境、实例 ID 应作为 metrics tag。

### RabbitMQ 健康检查

引入 Spring Boot Actuator 且项目存在 RabbitMQ 相关依赖和连接工厂时，Actuator 可以对 RabbitMQ 连接进行健康检查。健康检查通常会尝试获取 RabbitMQ 连接，以判断应用是否能连通 Broker。

访问健康检查：

```bash
curl http://localhost:8080/actuator/health
```

可能返回：

```json
{
  "status": "UP",
  "components": {
    "rabbit": {
      "status": "UP",
      "details": {
        "version": "3.13.0"
      }
    }
  }
}
```

当 RabbitMQ 不可用时，可能返回：

```json
{
  "status": "DOWN",
  "components": {
    "rabbit": {
      "status": "DOWN",
      "details": {
        "error": "java.net.ConnectException: Connection refused"
      }
    }
  }
}
```

RabbitMQ 健康检查说明：

| 状态             | 说明                   |
| ---------------- | ---------------------- |
| `UP`             | 应用可以连接 RabbitMQ  |
| `DOWN`           | 应用连接 RabbitMQ 失败 |
| `UNKNOWN`        | 健康检查无法明确判断   |
| `OUT_OF_SERVICE` | 服务不可用或被标记下线 |

健康检查建议：

1. RabbitMQ 健康检查失败不一定代表应用所有功能不可用，但 MQ 相关功能不可用。
2. Kubernetes Readiness 可以参考 RabbitMQ 健康状态。
3. Liveness 不建议强依赖外部 RabbitMQ，否则 Broker 短暂异常可能导致应用被反复重启。
4. 核心消费者服务的 Readiness 可以要求 RabbitMQ 连接正常。
5. 生产环境需要区分应用存活和依赖可用。

### 自定义健康检查

默认 RabbitMQ 健康检查只能判断连接是否可用。如果需要检查关键队列是否存在、死信队列是否堆积、消费者是否在线，可以自定义 HealthIndicator。

下面的自定义健康检查使用 `RabbitAdmin` 检查指定队列是否存在，并读取队列消息数量。

文件位置：`src/main/java/io/github/atengk/rabbitmq/health/RabbitMqQueueHealthIndicator.java`

```java
package io.github.atengk.rabbitmq.health;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RabbitMQ 队列健康检查
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Component
@RequiredArgsConstructor
public class RabbitMqQueueHealthIndicator implements HealthIndicator {

    private final RabbitAdmin rabbitAdmin;

    private static final List<String> CORE_QUEUE_LIST = List.of(
            "order.paid.queue",
            "payment.success.queue",
            "stock.deduct.queue"
    );

    /**
     * 检查核心队列健康状态
     *
     * @return 健康状态
     */
    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        boolean healthy = true;

        for (String queueName : CORE_QUEUE_LIST) {
            QueueInformation queueInfo = rabbitAdmin.getQueueInfo(queueName);
            if (queueInfo == null) {
                healthy = false;
                details.put(queueName, "队列不存在");
                continue;
            }

            Map<String, Object> queueDetails = new HashMap<>();
            queueDetails.put("messageCount", queueInfo.getMessageCount());
            queueDetails.put("consumerCount", queueInfo.getConsumerCount());
            details.put(queueName, queueDetails);

            if (queueInfo.getConsumerCount() <= 0) {
                healthy = false;
            }
        }

        if (CollUtil.isEmpty(details)) {
            return Health.unknown()
                    .withDetail("reason", "未配置核心队列")
                    .build();
        }

        if (healthy) {
            return Health.up()
                    .withDetails(details)
                    .build();
        }

        return Health.down()
                .withDetails(details)
                .build();
    }
}
```

访问自定义健康检查：

```bash
curl http://localhost:8080/actuator/health
```

自定义健康检查建议：

1. 不要在健康检查中执行耗时操作。
2. 不要检查过多队列，避免影响健康接口响应。
3. 只检查核心队列即可。
4. 队列不存在应返回 DOWN。
5. 消费者数量为 0 是否 DOWN，应根据应用角色判断。
6. 死信队列存在消息是否 DOWN，应按业务重要性决定。

如果希望独立暴露某类健康组件，可以通过 Spring Boot 健康分组配置：

```yaml
management:
  endpoint:
    health:
      group:
        mq:
          include: rabbit,rabbitMqQueue
          show-details: always
```

访问：

```bash
curl http://localhost:8080/actuator/health/mq
```

### Prometheus 指标暴露

Prometheus 指标暴露用于让 Prometheus 定期抓取 Spring Boot 应用指标。添加 `micrometer-registry-prometheus` 后，Actuator 会提供 `/actuator/prometheus` 端点。

配置示例：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  prometheus:
    metrics:
      export:
        enabled: true
  metrics:
    tags:
      application: rabbitmq-demo
      env: dev
```

访问 Prometheus 指标：

```bash
curl http://localhost:8080/actuator/prometheus
```

自定义 MQ 指标建议：

| 指标名                     | 类型    | 说明                |
| -------------------------- | ------- | ------------------- |
| `mq_send_total`            | Counter | 消息发送总数        |
| `mq_send_failed_total`     | Counter | 消息发送失败总数    |
| `mq_confirm_success_total` | Counter | Confirm 成功总数    |
| `mq_confirm_failed_total`  | Counter | Confirm 失败总数    |
| `mq_return_total`          | Counter | Return 不可路由总数 |
| `mq_consume_success_total` | Counter | 消费成功总数        |
| `mq_consume_failed_total`  | Counter | 消费失败总数        |
| `mq_dead_letter_total`     | Counter | 死信消息总数        |
| `mq_consume_cost_seconds`  | Timer   | 消费耗时            |
| `mq_retry_total`           | Counter | 重试次数            |

下面的服务用于记录带标签的 MQ 指标。

文件位置：`src/main/java/io/github/atengk/rabbitmq/monitor/RabbitMqMetricRecorder.java`

```java
package io.github.atengk.rabbitmq.monitor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * RabbitMQ 指标记录器
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Component
@RequiredArgsConstructor
public class RabbitMqMetricRecorder {

    private final MeterRegistry meterRegistry;

    /**
     * 记录发送成功
     *
     * @param businessType 业务类型
     */
    public void recordSendSuccess(String businessType) {
        meterRegistry.counter("mq_send_total", "businessType", businessType, "status", "success").increment();
    }

    /**
     * 记录发送失败
     *
     * @param businessType 业务类型
     */
    public void recordSendFailed(String businessType) {
        meterRegistry.counter("mq_send_total", "businessType", businessType, "status", "failed").increment();
    }

    /**
     * 记录不可路由消息
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     */
    public void recordReturned(String exchange, String routingKey) {
        meterRegistry.counter("mq_return_total", "exchange", exchange, "routingKey", routingKey).increment();
    }

    /**
     * 记录消费成功
     *
     * @param queueName 队列名称
     */
    public void recordConsumeSuccess(String queueName) {
        meterRegistry.counter("mq_consume_total", "queue", queueName, "status", "success").increment();
    }

    /**
     * 记录消费失败
     *
     * @param queueName 队列名称
     */
    public void recordConsumeFailed(String queueName) {
        meterRegistry.counter("mq_consume_total", "queue", queueName, "status", "failed").increment();
    }

    /**
     * 记录消费耗时
     *
     * @param queueName 队列名称
     * @param costMs    耗时毫秒
     */
    public void recordConsumeCost(String queueName, long costMs) {
        Timer.builder("mq_consume_cost_seconds")
                .description("MQ消息消费耗时")
                .tag("queue", queueName)
                .register(meterRegistry)
                .record(Duration.ofMillis(costMs));
    }
}
```

消费者中记录指标示例：

```java
long startTime = System.currentTimeMillis();
String queueName = message.getMessageProperties().getConsumerQueue();

try {
    handleBusiness(payload);
    metricRecorder.recordConsumeSuccess(queueName);
    metricRecorder.recordConsumeCost(queueName, System.currentTimeMillis() - startTime);
    channel.basicAck(deliveryTag, false);
} catch (Exception ex) {
    metricRecorder.recordConsumeFailed(queueName);
    metricRecorder.recordConsumeCost(queueName, System.currentTimeMillis() - startTime);
    channel.basicNack(deliveryTag, false, false);
}
```

Prometheus 抓取配置示例：

```yaml
scrape_configs:
  - job_name: 'rabbitmq-demo'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
          - 'rabbitmq-demo:8080'
```

指标设计建议：

1. Counter 用于累计次数。
2. Timer 用于耗时统计。
3. 标签不要使用高基数字段，例如 `messageId`、`orderId`。
4. 标签适合使用 `queue`、`businessType`、`exchange`、`status`。
5. 生产环境应控制自定义指标数量，避免指标基数爆炸。
6. RabbitMQ Broker 指标和应用指标应同时采集。

### Grafana 面板设计

Grafana 面板用于可视化 RabbitMQ Broker 指标和 Spring Boot 应用指标。推荐将 Broker 层和应用层分开展示，也可以为核心业务队列建立专用面板。

推荐面板分组如下：

| 面板分组    | 指标                                                 |
| ----------- | ---------------------------------------------------- |
| Broker 总览 | 节点状态、内存、磁盘、连接数、Channel 数             |
| 队列总览    | Ready、Unacked、Consumers、队列总消息数              |
| 消息速率    | Publish rate、Deliver rate、Ack rate、Redeliver rate |
| 死信监控    | 死信队列深度、死信增长速率、死信处理状态             |
| 生产者监控  | 发送成功、发送失败、Confirm 失败、Return 数量        |
| 消费者监控  | 消费成功、消费失败、消费耗时 P95/P99                 |
| 重试补偿    | 重试次数、重试失败数、待补偿数量                     |
| 应用健康    | JVM、CPU、内存、线程、HTTP、Actuator health          |

核心图表建议：

| 图表              | 展示内容                            |
| ----------------- | ----------------------------------- |
| 队列 Ready 趋势   | 每个核心队列 Ready 数量             |
| 队列 Unacked 趋势 | 每个核心队列 Unacked 数量           |
| 生产消费速率对比  | Publish rate 与 Ack rate            |
| 消费失败趋势      | `mq_consume_total{status="failed"}` |
| Confirm 失败趋势  | `mq_confirm_failed_total`           |
| Return 趋势       | `mq_return_total`                   |
| 死信队列趋势      | dead queue messages ready           |
| 消费耗时 P95/P99  | `mq_consume_cost_seconds`           |
| 消费者数量        | 每个队列 Consumers                  |
| 应用实例状态      | UP/DOWN、JVM、线程数                |

PromQL 示例：

```promql
# MQ消费失败速率
rate(mq_consume_total{status="failed"}[5m])

# MQ消费成功速率
rate(mq_consume_total{status="success"}[5m])

# MQ发送失败速率
rate(mq_send_total{status="failed"}[5m])

# MQ不可路由消息增长
increase(mq_return_total[5m])

# 消费耗时 P95
histogram_quantile(0.95, rate(mq_consume_cost_seconds_bucket[5m]))

# 消费耗时 P99
histogram_quantile(0.99, rate(mq_consume_cost_seconds_bucket[5m]))
```

Grafana 告警建议：

| 告警项          | 条件示例                    | 级别        |
| --------------- | --------------------------- | ----------- |
| 核心队列堆积    | Ready 连续 5 分钟大于阈值   | 严重        |
| 死信增长        | 死信队列 5 分钟内新增大于 0 | 严重        |
| Confirm 失败    | 5 分钟内出现 Confirm 失败   | 严重        |
| Return 不可路由 | 5 分钟内出现 Return         | 严重        |
| 消费失败率高    | 失败率超过 5%               | 警告 / 严重 |
| 消费耗时升高    | P95 超过业务阈值            | 警告        |
| Consumers 为 0  | 核心队列消费者为 0          | 严重        |
| Unacked 过高    | 连续 5 分钟超过阈值         | 警告        |
| Broker 磁盘不足 | 低于磁盘水位                | 严重        |
| Broker 内存过高 | 接近内存水位                | 严重        |

面板设计建议：

1. Broker 指标用于判断 RabbitMQ 是否健康。
2. 应用指标用于判断生产者和消费者是否健康。
3. 核心业务队列单独建面板，不要混在总览里。
4. Grafana 变量可以按 `env`、`application`、`queue`、`businessType` 过滤。
5. 不要把 `messageId`、`businessKey` 作为指标标签。
6. 告警应区分警告和严重，避免告警疲劳。
7. 死信、Return、Confirm 失败属于高优先级告警。

## 安全设计

本章节用于说明 RabbitMQ 在项目中的安全设计，包括用户权限、Virtual Host 隔离、管理控制台访问控制、TLS 加密、敏感配置加密和消息敏感字段处理。RabbitMQ 安全设计的核心目标是：最小权限访问、环境隔离、传输加密、配置脱敏、消息脱敏和操作可审计。

### RabbitMQ 用户权限

RabbitMQ 用户权限应按照“最小权限原则”配置。生产环境不应让业务应用使用 `guest`、`admin` 或超级管理员账号连接 RabbitMQ，而应为每个应用、每个环境、每类用途创建独立账号。

推荐用户分类如下：

| 用户类型     | 示例账号         | 用途                  | 权限建议              |
| ------------ | ---------------- | --------------------- | --------------------- |
| 管理员用户   | `rabbit_admin`   | 运维管理              | 管理控制台管理员权限  |
| 应用生产者   | `order_producer` | 订单服务发送消息      | 只允许写指定 Exchange |
| 应用消费者   | `order_consumer` | 订单服务消费消息      | 只允许读指定 Queue    |
| 监控用户     | `rabbit_monitor` | Prometheus / exporter | 只读监控权限          |
| 临时排查用户 | `rabbit_debug`   | 临时排查问题          | 限时、限权限          |

创建用户和授权示例：

```bash
# 创建订单生产者用户
rabbitmqctl add_user order_producer 'Producer@123456'

# 创建订单消费者用户
rabbitmqctl add_user order_consumer 'Consumer@123456'

# 创建监控用户
rabbitmqctl add_user rabbit_monitor 'Monitor@123456'

# 设置用户标签，业务应用用户不设置 administrator
rabbitmqctl set_user_tags order_producer management
rabbitmqctl set_user_tags order_consumer management
rabbitmqctl set_user_tags rabbit_monitor monitoring

# 授权订单生产者访问 /prod vhost
# configure: 允许声明资源的正则
# write: 允许写入的资源正则
# read: 允许读取的资源正则
rabbitmqctl set_permissions -p /prod order_producer '^$' '^business\.order\..*|^delay\.order\..*' '^$'

# 授权订单消费者访问 /prod vhost
rabbitmqctl set_permissions -p /prod order_consumer '^$' '^$' '^order\..*\.queue$|^order\..*\.dead\.queue$'

# 授权监控用户只读
rabbitmqctl set_permissions -p /prod rabbit_monitor '^$' '^$' '.*'
```

命令说明：`set_permissions` 的三个正则参数分别控制配置权限、写权限和读权限。生产者通常只需要写 Exchange，不需要读取 Queue；消费者通常只需要读取 Queue，不需要写 Exchange；是否允许应用声明 Exchange、Queue、Binding，应根据项目资源初始化方式决定。

权限设计建议：

1. 生产环境禁用或限制默认 `guest` 用户。
2. 业务应用不要使用管理员账号连接 RabbitMQ。
3. 不同应用使用不同账号，便于审计和隔离。
4. 生产者和消费者账号尽量拆分。
5. 权限正则尽量收敛到业务前缀，不要直接配置 `.*`。
6. 临时排查账号应设置有效期或事后删除。
7. 用户密码应通过配置中心、环境变量或密钥系统注入，不应写死在代码仓库。

### Virtual Host 隔离

Virtual Host 是 RabbitMQ 的逻辑隔离单元。不同 vhost 下的 Exchange、Queue、Binding、权限互相隔离。项目中建议按环境、系统或租户维度使用 vhost 隔离。

推荐 vhost 规划如下：

| 环境     | vhost    | 说明           |
| -------- | -------- | -------------- |
| 本地开发 | `/dev`   | 本地开发和联调 |
| 测试环境 | `/test`  | 测试环境       |
| 预发环境 | `/stage` | 预发验证       |
| 生产环境 | `/prod`  | 生产业务       |
| 压测环境 | `/perf`  | 压力测试       |

创建 vhost 示例：

```bash
# 创建开发环境 vhost
rabbitmqctl add_vhost /dev

# 创建测试环境 vhost
rabbitmqctl add_vhost /test

# 创建生产环境 vhost
rabbitmqctl add_vhost /prod

# 查看 vhost 列表
rabbitmqctl list_vhosts
```

Spring Boot 连接 vhost 配置示例：

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  rabbitmq:
    # 生产环境 RabbitMQ 地址
    host: rabbitmq-prod.internal
    port: 5672
    # 生产环境独立 vhost
    virtual-host: /prod
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}
```

Virtual Host 设计建议：

1. 不同环境必须使用不同 vhost。
2. 生产环境不要和测试环境共用 vhost。
3. 压测环境建议独立 vhost，避免压测消息污染业务队列。
4. 应用账号只授权访问对应 vhost。
5. 队列、交换机命名不应再包含环境名，环境隔离交给 vhost。
6. 删除 vhost 是高风险操作，会删除该 vhost 下所有资源，生产环境必须禁止随意操作。

### 管理控制台访问控制

RabbitMQ 管理控制台是高权限入口，生产环境必须严格限制访问。管理控制台不应直接暴露到公网，应通过内网、VPN、堡垒机、网关认证或访问白名单控制。

管理控制台常见风险如下：

| 风险     | 说明                               |
| -------- | ---------------------------------- |
| 弱密码   | 管理员密码过弱导致被登录           |
| 公网暴露 | 管理端口暴露到互联网               |
| 权限过大 | 普通排查人员拥有管理员权限         |
| 误操作   | 手动删除队列、清空消息、删除 vhost |
| 无审计   | 无法追踪是谁执行了管理操作         |

Nginx 反向代理和访问限制示例：

文件位置：`/etc/nginx/conf.d/rabbitmq-management.conf`

```nginx
server {
    listen 443 ssl;
    server_name rabbitmq-admin.example.com;

    # TLS 证书配置
    ssl_certificate /etc/nginx/certs/rabbitmq-admin.crt;
    ssl_certificate_key /etc/nginx/certs/rabbitmq-admin.key;

    # 只允许办公网和堡垒机访问
    allow 10.10.0.0/16;
    allow 192.168.10.20;
    deny all;

    location / {
        proxy_pass http://rabbitmq-prod.internal:15672;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

        # 避免长连接页面异常
        proxy_http_version 1.1;
        proxy_set_header Connection "";
    }
}
```

管理控制台安全建议：

1. 管理控制台只允许内网访问。
2. 管理用户启用强密码并定期轮换。
3. 普通开发人员使用只读或低权限账号。
4. 删除队列、清空队列、删除 vhost 等操作必须走变更流程。
5. 生产环境管理控制台访问应接入堡垒机或网关审计。
6. 监控系统使用独立只读账号，不使用管理员账号。

### TLS 配置

TLS 用于加密应用和 RabbitMQ 之间的网络传输，防止账号密码和消息内容在网络中明文传输。生产环境跨机房、跨网络区域、云上公网链路或安全要求较高的业务建议启用 TLS。

RabbitMQ 服务端 TLS 配置示例：

文件位置：`/etc/rabbitmq/rabbitmq.conf`

```properties
# 禁用明文监听端口，根据实际情况决定是否保留
listeners.tcp = none

# 启用 TLS 监听端口
listeners.ssl.default = 5671

# CA 证书
ssl_options.cacertfile = /etc/rabbitmq/certs/ca.crt

# 服务端证书
ssl_options.certfile = /etc/rabbitmq/certs/server.crt

# 服务端私钥
ssl_options.keyfile = /etc/rabbitmq/certs/server.key

# 如果需要客户端证书认证，可设置为 true
ssl_options.verify = verify_peer

# 客户端没有证书时是否失败
ssl_options.fail_if_no_peer_cert = false
```

Spring Boot TLS 连接配置示例：

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  rabbitmq:
    host: rabbitmq-prod.internal
    port: 5671
    virtual-host: /prod
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}
    ssl:
      # 启用 TLS
      enabled: true
      # 信任库路径
      trust-store: classpath:certs/rabbitmq-truststore.p12
      # 信任库密码，生产环境从环境变量或配置中心注入
      trust-store-password: ${RABBITMQ_TRUST_STORE_PASSWORD}
      # 信任库类型
      trust-store-type: PKCS12
```

如果启用双向 TLS，可以配置客户端证书：

```yaml
spring:
  rabbitmq:
    ssl:
      enabled: true
      trust-store: classpath:certs/rabbitmq-truststore.p12
      trust-store-password: ${RABBITMQ_TRUST_STORE_PASSWORD}
      trust-store-type: PKCS12
      key-store: classpath:certs/rabbitmq-client.p12
      key-store-password: ${RABBITMQ_KEY_STORE_PASSWORD}
      key-store-type: PKCS12
```

TLS 配置建议：

1. 生产环境跨网络访问 RabbitMQ 时建议启用 TLS。
2. 证书文件不要提交到代码仓库。
3. 证书密码通过环境变量、配置中心或密钥系统注入。
4. 证书到期时间必须纳入运维监控。
5. 双向 TLS 安全性更高，但运维复杂度也更高。
6. 启用 TLS 后需要压测连接和吞吐性能。

### 敏感配置加密

RabbitMQ 用户名、密码、证书密码、管理端账号等都属于敏感配置，不应明文写入 Git 仓库。常见处理方式包括环境变量、配置中心加密、Kubernetes Secret、Jasypt 加密和密钥管理系统。

推荐敏感配置项如下：

| 配置项                                     | 说明                    |
| ------------------------------------------ | ----------------------- |
| `spring.rabbitmq.username`                 | RabbitMQ 用户名         |
| `spring.rabbitmq.password`                 | RabbitMQ 密码           |
| `spring.rabbitmq.ssl.trust-store-password` | TLS 信任库密码          |
| `spring.rabbitmq.ssl.key-store-password`   | TLS 客户端证书密码      |
| 管理端账号                                 | RabbitMQ 管理控制台账号 |
| 告警 Webhook                               | 运维告警地址            |

环境变量方式：

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST}
    port: ${RABBITMQ_PORT:5672}
    virtual-host: ${RABBITMQ_VHOST:/prod}
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}
```

Kubernetes Secret 示例：

文件位置：`k8s/rabbitmq-secret.yaml`

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: rabbitmq-secret
  namespace: business
type: Opaque
stringData:
  # RabbitMQ 连接账号
  RABBITMQ_USERNAME: order_producer
  # RabbitMQ 连接密码
  RABBITMQ_PASSWORD: Producer@123456
  # RabbitMQ 连接 vhost
  RABBITMQ_VHOST: /prod
```

Deployment 注入 Secret 示例：

文件位置：`k8s/order-service-deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
  namespace: business
spec:
  replicas: 2
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      containers:
        - name: order-service
          image: registry.example.com/order-service:1.0.0
          envFrom:
            # 从 Secret 注入 RabbitMQ 敏感配置
            - secretRef:
                name: rabbitmq-secret
```

敏感配置管理建议：

1. 生产密码不提交 Git。
2. 本地开发可以使用 `.env` 或本地 profile，但不要提交真实密码。
3. 测试、预发、生产密码必须区分。
4. 密码应定期轮换。
5. 轮换密码时要支持灰度切换，避免应用同时断连。
6. 配置中心应开启敏感字段加密和访问审计。

### 消息敏感字段处理

消息体可能包含手机号、身份证号、银行卡号、邮箱、地址、Token、密钥等敏感字段。RabbitMQ 消息可能被日志、管理控制台、死信表、消息记录表和监控系统间接保存，因此消息体应避免携带敏感信息，必须携带时应脱敏或加密。

常见敏感字段如下：

| 字段     | 处理建议                        |
| -------- | ------------------------------- |
| 手机号   | 日志脱敏，消息体尽量只传用户 ID |
| 身份证号 | 不进入 MQ，必要时加密           |
| 银行卡号 | 不进入 MQ，必要时加密           |
| 邮箱     | 日志脱敏                        |
| 地址     | 按业务需要脱敏                  |
| Token    | 禁止进入 MQ                     |
| 密码     | 禁止进入 MQ                     |
| 密钥     | 禁止进入 MQ                     |

消息脱敏工具类如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/util/MqSensitiveUtil.java`

```java
package io.github.atengk.rabbitmq.util;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;

/**
 * MQ 敏感字段处理工具
 *
 * @author Ateng
 * @since 2026-05-11
 */
public final class MqSensitiveUtil {

    private MqSensitiveUtil() {
    }

    /**
     * 手机号脱敏
     *
     * @param mobile 手机号
     * @return 脱敏手机号
     */
    public static String maskMobile(String mobile) {
        if (StrUtil.isBlank(mobile)) {
            return mobile;
        }
        return DesensitizedUtil.mobilePhone(mobile);
    }

    /**
     * 邮箱脱敏
     *
     * @param email 邮箱
     * @return 脱敏邮箱
     */
    public static String maskEmail(String email) {
        if (StrUtil.isBlank(email)) {
            return email;
        }
        return DesensitizedUtil.email(email);
    }

    /**
     * 身份证号脱敏
     *
     * @param idCard 身份证号
     * @return 脱敏身份证号
     */
    public static String maskIdCard(String idCard) {
        if (StrUtil.isBlank(idCard)) {
            return idCard;
        }
        return DesensitizedUtil.idCardNum(idCard, 6, 4);
    }

    /**
     * 银行卡号脱敏
     *
     * @param bankCard 银行卡号
     * @return 脱敏银行卡号
     */
    public static String maskBankCard(String bankCard) {
        if (StrUtil.isBlank(bankCard)) {
            return bankCard;
        }
        return DesensitizedUtil.bankCard(bankCard);
    }
}
```

发送日志中使用脱敏示例：

```java
log.info("发送通知消息，messageId={}，businessKey={}，mobile={}，email={}",
        messageId,
        businessKey,
        MqSensitiveUtil.maskMobile(mobile),
        MqSensitiveUtil.maskEmail(email)
);
```

敏感字段处理建议：

1. MQ 消息体优先传业务 ID，不传完整敏感数据。
2. 消费者需要敏感数据时，通过业务服务按权限查询。
3. 日志中必须脱敏手机号、身份证、银行卡、邮箱等字段。
4. 死信表和消息记录表会保存消息体，因此消息体不应包含明文敏感字段。
5. 禁止将密码、Token、密钥、验证码明文写入 MQ。
6. 如确需传输敏感字段，应使用字段级加密，并做好密钥管理。

## 配置管理

本章节用于说明 RabbitMQ 在本地、测试、生产环境中的配置管理方式。配置管理的目标是环境隔离、参数可控、敏感信息安全、变更可追踪、风险可回滚。

推荐配置拆分如下：

```text
src/main/resources/
├── application.yml
├── application-local.yml
├── application-dev.yml
├── application-test.yml
├── application-stage.yml
└── application-prod.yml
```

公共配置放在 `application.yml`，环境差异放在对应 profile 中。敏感配置通过环境变量、配置中心或 Secret 注入。

### 本地配置

本地配置用于开发人员在本机启动 RabbitMQ 和 Spring Boot 应用。该环境应尽量简单，便于快速调试，但不应与测试或生产环境共用 RabbitMQ 资源。

本地 Docker Compose 示例：

文件位置：`docker/docker-compose-rabbitmq-local.yml`

```yaml
services:
  rabbitmq:
    image: rabbitmq:3.13-management
    container_name: rabbitmq-local
    restart: unless-stopped
    ports:
      # RabbitMQ AMQP 端口
      - "5672:5672"
      # RabbitMQ 管理控制台端口
      - "15672:15672"
    environment:
      # 本地开发账号
      RABBITMQ_DEFAULT_USER: ateng
      # 本地开发密码
      RABBITMQ_DEFAULT_PASS: ateng123
      # 本地开发 vhost
      RABBITMQ_DEFAULT_VHOST: /dev
    volumes:
      # 持久化 RabbitMQ 数据
      - rabbitmq-local-data:/var/lib/rabbitmq

volumes:
  rabbitmq-local-data:
```

启动命令：

```bash
# 进入 docker 目录
cd docker

# 启动本地 RabbitMQ
docker compose -f docker-compose-rabbitmq-local.yml up -d

# 查看容器状态
docker compose -f docker-compose-rabbitmq-local.yml ps
```

本地 Spring Boot 配置：

文件位置：`src/main/resources/application-local.yml`

```yaml
spring:
  rabbitmq:
    # 本地 RabbitMQ 地址
    host: localhost
    port: 5672
    virtual-host: /dev
    username: ateng
    password: ateng123
    publisher-confirm-type: correlated
    publisher-returns: true
    template:
      mandatory: true
    listener:
      simple:
        acknowledge-mode: manual
        prefetch: 10
        concurrency: 1
        max-concurrency: 4
        default-requeue-rejected: false
```

本地配置建议：

1. 本地使用独立 vhost，例如 `/dev`。
2. 本地账号密码可以简单，但不要复用生产密码。
3. 本地队列可以允许代码自动声明。
4. 本地配置可以开启较详细日志。
5. 本地不要连接测试或生产 RabbitMQ，避免误发消息。

### 测试环境配置

测试环境用于功能测试、集成测试和联调。测试环境应与生产环境结构接近，但资源规模和权限可以适当降低。

文件位置：`src/main/resources/application-test.yml`

```yaml
spring:
  rabbitmq:
    addresses: rabbitmq-test-01.internal:5672,rabbitmq-test-02.internal:5672
    virtual-host: /test
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}
    publisher-confirm-type: correlated
    publisher-returns: true
    template:
      mandatory: true
      retry:
        # 发送端短暂异常重试
        enabled: true
        initial-interval: 1s
        max-attempts: 3
        max-interval: 5s
        multiplier: 2
    listener:
      simple:
        acknowledge-mode: manual
        concurrency: 2
        max-concurrency: 8
        prefetch: 10
        default-requeue-rejected: false
        retry:
          # 消费端有限重试
          enabled: true
          max-attempts: 3
          initial-interval: 1s
          max-interval: 10s
          multiplier: 2
```

测试环境建议：

1. 测试环境使用 `/test` vhost。
2. 测试账号与生产账号隔离。
3. 测试环境应开启 Confirm、Return、手动 Ack。
4. 测试环境要验证死信、重试、补偿、幂等流程。
5. 测试环境允许管理端查看和重发消息，但必须记录操作日志。
6. 测试数据清理不能直接删除生产同名资源。

### 生产环境配置

生产环境配置需要优先保证可靠性、安全性和可观测性。生产环境必须开启 Publisher Confirm、Publisher Return、手动 Ack、死信队列、日志记录和监控告警。

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  rabbitmq:
    # 生产 RabbitMQ 集群地址
    addresses: rabbitmq-prod-01.internal:5672,rabbitmq-prod-02.internal:5672,rabbitmq-prod-03.internal:5672
    virtual-host: /prod
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}

    # 连接超时时间
    connection-timeout: 10s
    # 心跳检测
    requested-heartbeat: 30s

    # Publisher Confirm，确认消息是否到达 Exchange
    publisher-confirm-type: correlated
    # Publisher Return，处理不可路由消息
    publisher-returns: true

    template:
      # 不可路由消息返回生产者
      mandatory: true
      retry:
        # RabbitTemplate 调用异常重试
        enabled: true
        initial-interval: 1s
        max-attempts: 3
        max-interval: 5s
        multiplier: 2

    cache:
      channel:
        # Channel 缓存数量，根据并发和压测结果调整
        size: 100

    listener:
      simple:
        # 核心业务手动确认
        acknowledge-mode: manual
        # 初始消费线程数
        concurrency: 4
        # 最大消费线程数
        max-concurrency: 16
        # 每个消费者预取消息数量
        prefetch: 10
        # 异常后不默认重新入队，配合死信队列
        default-requeue-rejected: false
        retry:
          # 消费有限重试
          enabled: true
          max-attempts: 3
          initial-interval: 1s
          max-interval: 10s
          multiplier: 2

management:
  endpoints:
    web:
      exposure:
        # 生产环境只暴露必要端点
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when_authorized
```

生产环境建议：

1. 生产环境不允许业务应用使用管理员账号。
2. 生产环境配置必须通过配置中心、Secret 或环境变量注入。
3. 生产环境不建议由业务应用自动创建核心 Exchange、Queue、Binding。
4. 核心资源变更应通过变更脚本或运维流程执行。
5. 生产环境必须接入监控和告警。
6. 生产配置变更必须支持回滚。
7. 生产环境调整并发、prefetch、TTL、死信等参数前必须评估影响。

### 配置中心集成

配置中心用于统一管理 RabbitMQ 地址、账号、vhost、消费并发、重试次数、开关项等配置。常见配置中心包括 Nacos、Apollo、Spring Cloud Config 等。这里以 Nacos 为例说明。

Maven 依赖示例：

文件位置：`pom.xml`

```xml
<!-- Nacos 配置中心：用于统一管理环境配置 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>

<!-- Nacos 服务发现：如项目需要服务注册发现可引入 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

Nacos 基础配置示例：

文件位置：`src/main/resources/bootstrap.yml`

```yaml
spring:
  application:
    name: rabbitmq-demo
  profiles:
    active: prod
  cloud:
    nacos:
      config:
        # Nacos 配置中心地址
        server-addr: nacos-prod.internal:8848
        # 命名空间隔离环境
        namespace: prod
        # 配置分组
        group: RABBITMQ_GROUP
        # 配置格式
        file-extension: yaml
        # 共享 RabbitMQ 公共配置
        shared-configs:
          - data-id: rabbitmq-common.yaml
            group: RABBITMQ_GROUP
            refresh: true
```

Nacos 中的 RabbitMQ 配置示例：

```yaml
spring:
  rabbitmq:
    addresses: rabbitmq-prod-01.internal:5672,rabbitmq-prod-02.internal:5672
    virtual-host: /prod
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}
    publisher-confirm-type: correlated
    publisher-returns: true
    template:
      mandatory: true
    listener:
      simple:
        acknowledge-mode: manual
        concurrency: 4
        max-concurrency: 16
        prefetch: 10
        default-requeue-rejected: false
```

配置中心管理建议：

1. 不同环境使用不同 namespace。
2. RabbitMQ 公共配置和业务队列配置可以拆分 dataId。
3. 敏感配置使用环境变量、Secret 或配置中心加密能力。
4. 动态刷新只适合部分业务开关，不适合所有 RabbitMQ 底层连接参数。
5. 配置中心变更必须有审批和审计。
6. 配置变更后需要观察连接数、消费者数量、消费速率和异常日志。

### 动态配置设计

动态配置用于在不重新发布应用的情况下调整部分运行参数，例如消费开关、重试开关、限流阈值、补偿任务开关、管理端重发开关等。但并不是所有 RabbitMQ 参数都适合动态变更。

适合动态配置的参数：

| 配置项       | 说明                       |
| ------------ | -------------------------- |
| 消费开关     | 是否启用某类业务消费       |
| 补偿任务开关 | 是否启用失败消息扫描       |
| 最大补偿数量 | 每次扫描处理多少条         |
| 重发开关     | 管理端是否允许重发         |
| 限流阈值     | 消费者每秒处理上限         |
| 告警阈值     | 队列堆积、死信数量告警阈值 |
| 日志级别     | 临时提高排查日志           |

不建议动态变更的参数：

| 配置项                             | 原因                         |
| ---------------------------------- | ---------------------------- |
| RabbitMQ 地址                      | 涉及连接重建                 |
| vhost                              | 资源隔离维度，变更风险高     |
| 用户名密码                         | 需要连接重建和密钥轮换流程   |
| Exchange 类型                      | 已创建资源不可随意修改       |
| Queue durable/exclusive/autoDelete | 与已有队列参数冲突会启动失败 |
| 死信交换机参数                     | 修改可能影响消息流向         |
| TTL 和最大长度                     | 已有队列参数变更有风险       |

动态配置属性示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/MqDynamicProperties.java`

```java
package io.github.atengk.rabbitmq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * MQ 动态配置属性
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "app.mq.dynamic")
public class MqDynamicProperties {

    /**
     * 是否开启消费
     */
    private Boolean consumeEnabled = true;

    /**
     * 是否开启补偿任务
     */
    private Boolean compensationEnabled = true;

    /**
     * 单次补偿最大数量
     */
    private Integer compensationBatchSize = 100;

    /**
     * 是否允许管理端重发
     */
    private Boolean manualResendEnabled = true;

    /**
     * 消费失败告警阈值
     */
    private Integer consumeFailedAlarmThreshold = 10;
}
```

配置示例：

文件位置：`src/main/resources/application.yml`

```yaml
app:
  mq:
    dynamic:
      # 是否允许消费者处理消息
      consume-enabled: true
      # 是否开启补偿任务
      compensation-enabled: true
      # 单次补偿最多处理数量
      compensation-batch-size: 100
      # 是否允许管理端人工重发
      manual-resend-enabled: true
      # 消费失败告警阈值
      consume-failed-alarm-threshold: 10
```

消费者中使用动态开关示例：

文件位置：`src/main/java/io/github/atengk/rabbitmq/consumer/DynamicSwitchConsumer.java`

```java
package io.github.atengk.rabbitmq.consumer;

import com.rabbitmq.client.Channel;
import io.github.atengk.rabbitmq.config.MqDynamicProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 动态开关消息消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicSwitchConsumer {

    private final MqDynamicProperties mqDynamicProperties;

    /**
     * 消费动态开关队列消息
     *
     * @param payload 消息体
     * @param message 原始消息
     * @param channel RabbitMQ Channel
     * @throws IOException Ack 或 Nack 失败时抛出
     */
    @RabbitListener(queues = "dynamic.switch.queue")
    public void consume(String payload, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        if (!Boolean.TRUE.equals(mqDynamicProperties.getConsumeEnabled())) {
            log.warn("MQ消费开关已关闭，消息重新入队，queue={}，deliveryTag={}",
                    message.getMessageProperties().getConsumerQueue(),
                    deliveryTag
            );
            channel.basicNack(deliveryTag, false, true);
            return;
        }

        try {
            log.info("动态开关消费者处理消息，payload={}", payload);
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("动态开关消费者处理失败，原因={}", ex.getMessage(), ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
```

动态配置设计建议：

1. 动态开关只控制业务行为，不随意改变 RabbitMQ 资源定义。
2. 消费开关关闭时要明确是重新入队、进入死信还是暂停容器。
3. 高频配置不建议通过配置中心频繁刷新。
4. 动态配置必须有默认值。
5. 动态配置变更应记录操作人、变更前值和变更后值。
6. 生产环境变更后要观察监控指标。

### 配置变更风险控制

RabbitMQ 配置变更存在较高风险，尤其是 Exchange、Queue、Binding、TTL、死信、vhost、权限、并发和 prefetch 等配置。变更前必须评估影响范围，变更后必须验证消息链路。

高风险变更如下：

| 变更项               | 风险                           |
| -------------------- | ------------------------------ |
| 修改 Queue durable   | 已有队列参数冲突，应用启动失败 |
| 修改 Queue arguments | 可能触发 `PRECONDITION_FAILED` |
| 修改 Exchange 类型   | 已有交换机类型冲突             |
| 修改 Routing Key     | 消息可能不可路由               |
| 修改 Binding         | 消息可能进入错误队列           |
| 修改 vhost           | 应用连接到错误资源             |
| 修改权限             | 生产者或消费者无法访问资源     |
| 修改 prefetch        | 可能导致 Unacked 飙升          |
| 修改 concurrency     | 可能压垮下游服务               |
| 修改 TTL             | 可能导致消息提前过期或延迟异常 |

变更前检查清单：

| 检查项               | 说明                               |
| -------------------- | ---------------------------------- |
| 是否影响生产消息链路 | 确认 Exchange、Queue、Binding 变化 |
| 是否影响消费者       | 检查消费者队列名和权限             |
| 是否影响补偿任务     | 检查死信和重试逻辑                 |
| 是否需要停机         | Queue 参数冲突可能需要新队列迁移   |
| 是否支持回滚         | 准备旧配置和回滚脚本               |
| 是否完成测试验证     | 在测试或预发环境验证               |
| 是否通知相关团队     | 消费者和生产者都要确认             |

生产资源变更建议使用脚本而不是手工点击控制台。

文件位置：`scripts/rabbitmq-prod-resource-check.sh`

```bash
#!/usr/bin/env bash

set -e

VHOST="/prod"

echo "检查 RabbitMQ 生产资源，vhost=${VHOST}"

echo "检查 Exchange"
rabbitmqctl list_exchanges -p "${VHOST}" name type durable | grep 'business.order.exchange'

echo "检查 Queue"
rabbitmqctl list_queues -p "${VHOST}" name durable messages consumers | grep 'order.paid.queue'

echo "检查 Binding"
rabbitmqctl list_bindings -p "${VHOST}" | grep 'order.paid'

echo "检查权限"
rabbitmqctl list_permissions -p "${VHOST}"
```

执行命令：

```bash
# 添加执行权限
chmod +x scripts/rabbitmq-prod-resource-check.sh

# 执行生产资源检查
./scripts/rabbitmq-prod-resource-check.sh
```

命令说明：该脚本用于在变更前后检查核心 Exchange、Queue、Binding 和权限是否存在。生产环境可以将检查结果纳入变更单，避免手工遗漏。

配置变更策略建议：

1. 新增队列优先于原地修改队列参数。
2. Routing Key 变更需要兼容新旧路由一段时间。
3. Queue 参数变更建议创建新队列并迁移流量。
4. 先测试环境验证，再预发验证，最后生产灰度。
5. 并发和 prefetch 调整应小步变更，观察下游压力。
6. 权限变更后立即验证生产者发送和消费者消费。
7. 变更后必须观察 Confirm、Return、死信、队列深度和消费失败日志。

配置变更验收标准：

| 验收项   | 标准                        |
| -------- | --------------------------- |
| 应用启动 | 无 RabbitMQ 资源声明异常    |
| 生产发送 | 发送日志正常                |
| Confirm  | 无 Confirm 失败             |
| Return   | 无不可路由消息              |
| 消费     | 消费者正常 Ack              |
| 死信     | 死信数量无异常增长          |
| 队列深度 | Ready 和 Unacked 无异常堆积 |
| 日志     | 无权限、连接、路由异常      |
| 监控     | 指标趋势稳定                |

最终建议：RabbitMQ 配置变更必须像数据库表结构变更一样谨慎处理。尤其是 Queue 参数、Exchange 类型、Routing Key 和 Binding 关系，一旦变更错误，可能造成消息不可路由、消息堆积、消费者异常或业务数据不一致。

## 性能优化

本章节用于说明 RabbitMQ 在 Spring Boot 项目中的性能优化方向。性能优化不应只依赖单个参数，而应从连接复用、Channel 缓存、消息大小、消费线程、`prefetch`、批量处理、队列拆分和 Broker 参数多个层面综合调优。

性能优化的核心原则是：先监控，再定位瓶颈，最后调参。不要在没有指标依据的情况下盲目提高并发或扩大 `prefetch`，否则可能把 RabbitMQ 的压力转移到数据库、Redis、第三方接口或消费者应用本身。

### 连接池优化

RabbitMQ 客户端连接是较重资源，应用不应在每次发送消息或消费消息时创建新连接。Spring Boot 集成 RabbitMQ 时，通常通过 `CachingConnectionFactory` 复用连接和 Channel。连接池优化的重点是复用连接、控制连接数量、设置合理超时、启用心跳检测和避免频繁重连。

生产环境推荐配置如下。

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  rabbitmq:
    # RabbitMQ 集群地址，客户端会在连接失败时尝试其他地址
    addresses: rabbitmq-prod-01.internal:5672,rabbitmq-prod-02.internal:5672,rabbitmq-prod-03.internal:5672
    virtual-host: /prod
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}

    # 连接超时时间，避免网络异常时长时间阻塞
    connection-timeout: 10s

    # 心跳检测，用于发现异常断开的连接
    requested-heartbeat: 30s

    cache:
      channel:
        # Channel 缓存数量，根据生产者并发和消费者并发调整
        size: 100
```

连接优化建议：

| 优化项     | 建议                                   |
| ---------- | -------------------------------------- |
| 连接复用   | 通过 Spring Boot 自动配置复用连接      |
| 连接数量   | 不要每个业务方法创建 Connection        |
| 心跳检测   | 生产环境开启 heartbeat                 |
| 超时控制   | 设置连接超时，避免无限等待             |
| 多地址配置 | 集群环境配置多个 Broker 地址           |
| 账号隔离   | 不同应用使用独立账号，便于观察连接来源 |

常见问题如下：

| 现象           | 可能原因                   | 处理方式                            |
| -------------- | -------------------------- | ----------------------------------- |
| 连接数持续增长 | 手动创建连接未释放         | 使用 Spring 管理的 `RabbitTemplate` |
| 频繁重连       | 网络不稳定或 Broker 压力大 | 检查网络、Broker 日志和连接配置     |
| 发送偶发超时   | 连接不可用或 Broker 流控   | 检查内存、磁盘水位和发布速率        |
| 单应用连接过多 | 连接工厂重复创建           | 检查 Bean 配置和依赖注入方式        |

### Channel 复用

Channel 是 RabbitMQ 执行消息发布、消费和确认的逻辑通道。相比 Connection，Channel 更轻量，但大量创建和销毁 Channel 仍会带来开销。Spring AMQP 通过 Channel 缓存减少重复创建成本。

Channel 缓存配置示例：

```yaml
spring:
  rabbitmq:
    cache:
      channel:
        # 单连接下缓存 Channel 数量
        size: 100
```

如果生产者并发发送较高，或者消费者监听容器较多，需要适当提高 Channel 缓存大小。可以通过 RabbitMQ 管理控制台或命令观察 Channel 数量：

```bash
# 查看 Channel 使用情况
rabbitmqctl list_channels connection number user vhost confirm consumer_count messages_unacknowledged prefetch_count
```

命令说明：`messages_unacknowledged` 可用于判断某个 Channel 是否积压未确认消息，`prefetch_count` 可用于确认消费者预取参数是否符合预期，`confirm` 可用于判断发布确认是否启用。

Channel 复用建议：

1. 不要在业务代码中手动频繁创建和关闭 Channel。
2. 生产者统一使用 Spring 注入的 `RabbitTemplate`。
3. 消费者统一使用 `@RabbitListener` 和监听容器。
4. Channel 缓存数量应通过压测确定。
5. Channel 数持续增长时，需要排查连接工厂是否重复创建。
6. Channel 频繁关闭时，需要检查 Exchange、Queue、Binding 参数是否冲突。

### 消息大小控制

消息越大，序列化、网络传输、磁盘写入、内存占用和消费者反序列化成本越高。RabbitMQ 更适合传输事件和任务，不适合传输大文件、大对象或大量嵌套数据。

消息大小设计建议：

| 类型         | 建议                               |
| ------------ | ---------------------------------- |
| 普通业务事件 | 尽量控制在 KB 级别                 |
| 大文件       | 只传文件 ID、URL、对象存储 Key     |
| 大文本       | 存数据库或对象存储，消息只传引用   |
| 批量数据     | 拆分为多条消息或传批次 ID          |
| 敏感数据     | 不直接进入消息体，必要时脱敏或加密 |

不推荐的消息体：

```json
{
  "orderId": "ORDER202605110001",
  "userSnapshot": {
    "name": "xxx",
    "mobile": "13800000000",
    "address": "完整地址",
    "historyOrders": [
      "大量历史订单数据"
    ]
  },
  "fileContent": "base64大文件内容"
}
```

推荐的消息体：

```json
{
  "messageId": "order_created_20260511103000_xxx",
  "eventType": "order.created",
  "businessKey": "ORDER202605110001",
  "data": {
    "orderId": "ORDER202605110001",
    "userId": 10001,
    "snapshotId": "SNAPSHOT202605110001",
    "fileObjectKey": "order/2026/05/11/ORDER202605110001.json"
  }
}
```

消息大小控制建议：

1. 消息体只放消费者处理所需的最小字段。
2. 大对象放数据库、MinIO、OSS、S3 等存储中，MQ 只传引用。
3. 不要在消息中传输完整 Entity。
4. 日志中不要完整打印大消息体。
5. 死信表会保存消息体，大消息会增加数据库压力。
6. 高吞吐场景需要重点控制消息大小，否则 Broker 内存和磁盘压力会明显升高。

### 消费线程优化

消费线程优化用于提高消费者处理能力。线程数过小会导致消息堆积，线程数过大又可能压垮数据库、Redis 或第三方接口。

基础配置如下：

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        # 初始消费者数量
        concurrency: 4
        # 最大消费者数量
        max-concurrency: 16
        # 手动确认，业务成功后再 Ack
        acknowledge-mode: manual
        # 每个消费者预取数量
        prefetch: 10
```

线程数估算方式：

```text
单线程消费能力 = 1000 / 单条消息平均耗时毫秒
理论总消费能力 = 消费线程数 × 单线程消费能力
```

示例：

| 项目             | 数值                             |
| ---------------- | -------------------------------- |
| 单条消息平均耗时 | 100ms                            |
| 单线程消费能力   | 10 条/秒                         |
| 当前消费线程数   | 8                                |
| 理论消费能力     | 80 条/秒                         |
| 生产峰值速率     | 200 条/秒                        |
| 结果             | 需要扩容线程、实例或优化业务耗时 |

消费线程优化建议：

| 场景           | 建议                             |
| -------------- | -------------------------------- |
| CPU 密集型     | 线程数接近 CPU 核心数，不宜过高  |
| IO 密集型      | 可适当提高线程数                 |
| 调用第三方接口 | 控制并发，防止触发限流           |
| 数据库写入     | 结合连接池容量调整               |
| 顺序消息       | 单队列单消费者，不通过加线程解决 |
| 批量消费       | 降低线程数，提高单批处理效率     |

线程优化优先级：

1. 先降低单条消息处理耗时。
2. 再调整 `concurrency` 和 `max-concurrency`。
3. 再横向扩容消费者实例。
4. 最后考虑队列拆分和业务架构调整。

### prefetch 参数优化

`prefetch` 表示每个消费者最多可以持有多少条未确认消息。该参数对吞吐量、消息分配公平性、失败恢复速度和内存占用都有明显影响。

配置示例：

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        # 每个消费者最多持有 10 条未确认消息
        prefetch: 10
```

不同场景的推荐值：

| 场景         | 推荐 prefetch      | 说明                     |
| ------------ | ------------------ | ------------------------ |
| 严格顺序消费 | 1                  | 保证单条处理             |
| 耗时任务     | 1 到 5             | 避免单消费者占用太多消息 |
| 普通业务     | 5 到 20            | 平衡吞吐和公平性         |
| 快速轻量消息 | 20 到 100          | 提高吞吐                 |
| 批量消费     | 大于等于 batchSize | 保证批量拉取             |
| 消费者不稳定 | 不宜过大           | 降低故障恢复成本         |

`prefetch` 过大的风险：

| 风险         | 说明                                   |
| ------------ | -------------------------------------- |
| Unacked 过高 | 消息被提前投递但未确认                 |
| 分配不均     | 某些消费者持有大量消息，其他消费者空闲 |
| 故障恢复慢   | 消费者宕机后大量未确认消息需要重新投递 |
| 顺序破坏     | 多消费者提前拉取消息后完成顺序不可控   |
| 内存压力     | 消费者本地持有消息过多                 |

调优建议：

1. 消息处理越慢，`prefetch` 越应该小。
2. 消息处理越快，`prefetch` 可以适当提高。
3. 顺序消费必须使用 `prefetch=1`。
4. 观察 `messages_unacknowledged` 判断是否过大。
5. 调整后观察消费速率、失败率、Unacked 和应用内存。

### 批量处理优化

批量处理可以减少网络往返、数据库提交次数和业务调用次数，适合日志、统计、通知、同步等场景。批量处理不适合支付、库存、账户流水等强一致业务。

批量监听配置示例：

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        # 批量大小
        batch-size: 50
        # prefetch 建议大于等于 batch-size
        prefetch: 100
```

批量监听容器配置如下。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/PerformanceBatchListenerConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 性能批量监听配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class PerformanceBatchListenerConfig {

    /**
     * 批量监听容器工厂
     *
     * @param configurer        监听容器配置器
     * @param connectionFactory RabbitMQ连接工厂
     * @return 批量监听容器工厂
     */
    @Bean
    public SimpleRabbitListenerContainerFactory performanceBatchListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);

        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConsumerBatchEnabled(true);
        factory.setBatchListener(true);
        factory.setBatchSize(50);
        factory.setPrefetchCount(100);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(8);
        factory.setDefaultRequeueRejected(false);

        return factory;
    }
}
```

批量处理优化建议：

1. 批量大小通过压测确定，常见值为 20、50、100。
2. `prefetch` 应大于或等于 `batchSize`。
3. 批量写数据库时要关注事务大小。
4. 批量失败必须有失败明细记录。
5. 已成功处理的消息重发时必须依赖幂等。
6. 不要把强事务消息和普通日志消息放在同一个批量队列。

### 队列拆分优化

当单队列消费能力不足，或者不同消息类型处理耗时差异较大时，应考虑队列拆分。队列拆分可以减少不同业务互相影响，提高消费并发和故障隔离能力。

常见拆分方式：

| 拆分方式       | 示例                                        | 适用场景               |
| -------------- | ------------------------------------------- | ---------------------- |
| 按业务类型拆分 | `order.paid.queue`、`order.cancelled.queue` | 不同业务处理逻辑不同   |
| 按优先级拆分   | `notice.high.queue`、`notice.low.queue`     | 高优先级消息优先处理   |
| 按耗时拆分     | `task.fast.queue`、`task.slow.queue`        | 避免慢任务阻塞快任务   |
| 按分片拆分     | `order.queue.0`、`order.queue.1`            | 提升并发且保持局部顺序 |
| 按租户拆分     | `tenant.a.queue`、`tenant.b.queue`          | 租户隔离               |

分片队列示例：

```text
order.event.queue.0
order.event.queue.1
order.event.queue.2
order.event.queue.3
```

队列拆分建议：

1. 一个队列不要承载过多语义差异很大的消息。
2. 慢任务和快任务分开队列。
3. 核心业务和低价值日志分开队列。
4. 高优先级业务使用独立队列和消费者。
5. 顺序要求按业务 Key 分片，不追求全局顺序。
6. 队列拆分后要同步调整监控、告警、死信和管理端查询。

### Broker 参数优化

Broker 参数优化主要面向 RabbitMQ 服务端资源，包括内存、磁盘、文件句柄、网络、队列类型和策略。服务端参数变更风险较高，生产环境应经过压测和变更审批。

常见 Broker 关注项：

| 参数或指标   | 说明                                   |
| ------------ | -------------------------------------- |
| 内存水位     | 内存达到阈值后 RabbitMQ 会触发流控     |
| 磁盘水位     | 磁盘低于阈值后可能阻塞发布             |
| 文件句柄     | 连接数、队列数、日志文件都会消耗句柄   |
| 队列数量     | 队列过多会增加管理和资源成本           |
| 消息持久化   | 提高可靠性但增加磁盘写入               |
| Quorum Queue | 提高副本可靠性但资源消耗更高           |
| Lazy 行为    | 长队列可降低内存占用，但会影响读取延迟 |

示例配置：

文件位置：`/etc/rabbitmq/rabbitmq.conf`

```properties
# 内存高水位，达到后会触发流控
vm_memory_high_watermark.relative = 0.6

# 磁盘剩余空间低水位
disk_free_limit.relative = 2.0

# TCP 监听端口
listeners.tcp.default = 5672

# 管理插件监听端口
management.tcp.port = 15672

# 默认心跳
heartbeat = 30
```

Broker 优化建议：

1. Broker 调优前必须先确认瓶颈在 Broker，而不是消费者。
2. 长期堆积不能只靠加大 Broker 参数解决，应优化消费能力。
3. 持久化消息需要关注磁盘性能。
4. Quorum Queue 更重视数据安全，资源消耗通常高于普通 classic queue。
5. 队列数量和连接数量都需要监控。
6. Broker 参数变更必须有回滚方案。

## 高可用设计

本章节用于说明 RabbitMQ 的高可用设计，包括集群模式、镜像队列、Quorum Queue、Federation、Shovel、客户端自动恢复、连接失败重试和故障切换策略。RabbitMQ 高可用不是单一功能，而是 Broker 集群、队列副本、客户端恢复、消息可靠性和业务补偿共同组成的体系。

RabbitMQ 官方文档将 Quorum Queue 描述为基于 Raft 共识算法的持久化复制队列，用于提供更明确的数据安全和故障处理语义；在需要复制和高可用队列时，Quorum Queue 应作为默认选择。([rabbitmq.com](https://www.rabbitmq.com/docs/4.0/quorum-queues?utm_source=chatgpt.com))

### RabbitMQ 集群模式

RabbitMQ 集群用于将多个 RabbitMQ 节点组成一个逻辑 Broker。集群中元数据会在节点间共享，例如 Exchange、Binding、用户、权限等；但队列消息是否复制，取决于队列类型和高可用策略。普通 classic queue 默认只在声明它的节点上保存消息，节点故障时该队列可能不可用。

典型三节点集群：

```text
rabbitmq-01
rabbitmq-02
rabbitmq-03
```

集群适合解决：

| 能力       | 说明                                   |
| ---------- | -------------------------------------- |
| 统一入口   | 多节点组成一个逻辑 RabbitMQ 集群       |
| 元数据共享 | Exchange、Binding、用户、权限等共享    |
| 横向扩展   | 连接和部分队列可以分散到不同节点       |
| 高可用基础 | 为 Quorum Queue 等复制队列提供节点基础 |

集群不能单独解决：

| 问题             | 说明                              |
| ---------------- | --------------------------------- |
| 普通队列消息复制 | 普通 classic queue 默认不复制消息 |
| 跨地域容灾       | 单集群通常不适合跨高延迟网络      |
| 消息绝对不丢     | 仍需持久化、Confirm、Ack、补偿    |
| 消费端幂等       | 仍需业务侧实现                    |

客户端连接集群地址示例：

```yaml
spring:
  rabbitmq:
    # 配置多个节点地址，客户端连接失败时可尝试其他节点
    addresses: rabbitmq-01.internal:5672,rabbitmq-02.internal:5672,rabbitmq-03.internal:5672
    virtual-host: /prod
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}
```

集群设计建议：

1. 生产环境至少 3 个节点，便于多数派和故障切换。
2. 集群节点应部署在低延迟、稳定网络内。
3. 不建议跨广域网部署单个 RabbitMQ 集群。
4. 集群高可用必须配合 Quorum Queue 或其他复制机制。
5. 客户端应配置多个 Broker 地址或通过负载均衡访问。
6. 集群节点故障切换后，消费者必须具备自动恢复能力。

### 镜像队列

镜像队列指 classic mirrored queues，是旧版本 RabbitMQ 中用于复制 classic queue 内容的高可用方案。该能力已经不再推荐。RabbitMQ 官方文档明确说明 classic queue mirroring 自 2021 年起已废弃，并从 RabbitMQ 4.0 开始移除；需要高可用复制队列时应使用 Quorum Queue 或 Streams。([rabbitmq.com](https://www.rabbitmq.com/docs/3.13/ha?utm_source=chatgpt.com))

旧版镜像队列策略示例：

```bash
# 旧版本示例：为队列配置镜像策略
# 新项目不推荐使用，RabbitMQ 4.x 已移除 classic mirrored queues
rabbitmqctl set_policy ha-all "^order\." '{"ha-mode":"all"}' --apply-to queues -p /prod
```

镜像队列的问题：

| 问题         | 说明                             |
| ------------ | -------------------------------- |
| 已废弃       | 新版本不应继续使用               |
| 故障语义复杂 | 某些故障场景难以提供清晰保证     |
| 迁移成本     | 升级 RabbitMQ 4.x 前需要迁移     |
| 性能和稳定性 | 不如 Quorum Queue 的现代复制模型 |

迁移建议：

1. 新项目不要使用 classic mirrored queues。
2. 旧项目升级 RabbitMQ 4.x 前必须迁移到 Quorum Queue 或 Streams。
3. 迁移前需要评估队列特性差异。
4. 迁移时先在测试环境验证消息可靠性、消费语义、死信和 TTL 行为。
5. 不要把普通 classic queue 误认为高可用队列。

### Quorum Queue

Quorum Queue 是 RabbitMQ 推荐的现代高可用复制队列类型。它基于 Raft 共识算法，使用 leader 和 follower 副本复制消息，适合对数据安全要求较高的业务。RabbitMQ 官方文档说明 Quorum Queue 始终是 durable 队列，不适合临时队列，也不支持 exclusive 语义。([rabbitmq.com](https://www.rabbitmq.com/docs/4.1/quorum-queues?utm_source=chatgpt.com))

声明 Quorum Queue 示例：

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/QuorumQueueConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Quorum Queue 配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class QuorumQueueConfig {

    public static final String ORDER_QUORUM_EXCHANGE = "ha.order.exchange";

    public static final String ORDER_PAID_QUORUM_QUEUE = "ha.order.paid.quorum.queue";

    public static final String ORDER_PAID_ROUTING_KEY = "order.paid";

    /**
     * 声明订单高可用交换机
     *
     * @return Direct Exchange
     */
    @Bean
    public DirectExchange orderQuorumExchange() {
        return ExchangeBuilder
                .directExchange(ORDER_QUORUM_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 声明订单支付 Quorum Queue
     *
     * @return Quorum Queue
     */
    @Bean
    public Queue orderPaidQuorumQueue() {
        return QueueBuilder
                .durable(ORDER_PAID_QUORUM_QUEUE)
                // 设置队列类型为 quorum
                .withArgument("x-queue-type", "quorum")
                // 设置投递次数限制，超过后可进入死信
                .withArgument("x-delivery-limit", 5)
                .build();
    }

    /**
     * 绑定订单支付 Quorum Queue
     *
     * @return Binding
     */
    @Bean
    public Binding orderPaidQuorumBinding() {
        return BindingBuilder
                .bind(orderPaidQuorumQueue())
                .to(orderQuorumExchange())
                .with(ORDER_PAID_ROUTING_KEY);
    }
}
```

使用策略方式声明 Quorum Queue：

```bash
# 对匹配 order.ha. 前缀的队列设置为 quorum 类型
rabbitmqctl set_policy -p /prod quorum-order "^order\.ha\." \
  '{"queue-type":"quorum"}' \
  --apply-to queues
```

Quorum Queue 适用场景：

| 场景         | 说明                         |
| ------------ | ---------------------------- |
| 订单核心事件 | 需要高可靠复制               |
| 支付成功消息 | 资金链路高可靠要求           |
| 库存扣减消息 | 防止关键消息因节点故障不可用 |
| 审计事件     | 需要较强数据安全             |
| 重要异步任务 | 丢失成本较高                 |

Quorum Queue 注意事项：

1. Quorum Queue 更重视数据安全和故障语义。
2. Quorum Queue 通常需要更多磁盘和内存资源。
3. 不适合作为临时队列。
4. 不支持 exclusive 队列语义。
5. 队列应保持短小，长期堆积会增加资源压力。
6. 高吞吐场景需要压测确认性能。
7. 使用 Quorum Queue 仍然需要 Publisher Confirm、Consumer Ack 和消费幂等。

### Federation

Federation 用于在不同 RabbitMQ Broker 或集群之间按需转发消息，常用于跨地域、跨数据中心、跨团队或松耦合集群之间的数据流动。RabbitMQ Federation 通过 upstream、upstream set 和 policy 配置；官方文档说明，在集群中使用 Federation 时，集群所有节点都应启用 federation 插件。([rabbitmq.com](https://www.rabbitmq.com/docs/3.13/federation?utm_source=chatgpt.com))

启用 Federation 插件：

```bash
# 启用 Federation 插件
rabbitmq-plugins enable rabbitmq_federation

# 启用 Federation 管理插件
rabbitmq-plugins enable rabbitmq_federation_management
```

Federation 适合场景：

| 场景           | 说明                         |
| -------------- | ---------------------------- |
| 跨地域消息同步 | 多地域集群之间按需拉取消息   |
| 松耦合集群     | 不希望组成单个高延迟集群     |
| 业务域隔离     | 不同业务集群之间共享部分消息 |
| 灾备同步       | 将部分事件同步到灾备集群     |
| 组织隔离       | 不同团队维护不同 Broker      |

Federation 不适合场景：

| 场景         | 原因                                            |
| ------------ | ----------------------------------------------- |
| 单机房高可用 | 使用 RabbitMQ 集群和 Quorum Queue 更直接        |
| 强一致复制   | Federation 是跨 Broker 转发，不是强一致存储复制 |
| 超低延迟链路 | 跨地域网络不可控                                |
| 替代业务补偿 | 仍需要幂等和补偿机制                            |

Federation 设计建议：

1. Federation 适合跨集群松耦合，不适合替代集群内部高可用。
2. 跨地域场景不要强行使用单 RabbitMQ 集群。
3. Federation 转发的消息消费者必须幂等。
4. Federation 链路需要单独监控延迟和积压。
5. 配置 upstream 时要使用专用账号和 vhost。
6. 跨集群转发前要明确消息方向和业务边界。

### Shovel

Shovel 用于将消息从一个源端持续转移到一个目标端，可以跨 vhost、跨集群、跨协议。RabbitMQ 官方文档说明，Shovel 是一个单向移动消息的插件，它像一个客户端应用一样连接源和目标，消费后重新发布消息，并使用确认和发布确认处理连接或节点失败。([rabbitmq.com](https://www.rabbitmq.com/docs/3.13/shovel?utm_source=chatgpt.com))

启用 Shovel 插件：

```bash
# 启用 Shovel 插件
rabbitmq-plugins enable rabbitmq_shovel

# 启用 Shovel 管理插件
rabbitmq-plugins enable rabbitmq_shovel_management
```

Shovel 适合场景：

| 场景          | 说明                                  |
| ------------- | ------------------------------------- |
| 消息迁移      | 从旧集群迁移消息到新集群              |
| 跨 vhost 转发 | 将一个 vhost 的消息转移到另一个 vhost |
| 临时数据搬运  | 运维期间临时转移消息                  |
| 跨协议转发    | AMQP 0-9-1 与 AMQP 1.0 之间转发       |
| 灾备补偿      | 将积压消息转移到备用集群              |

Shovel 与 Federation 对比：

| 项目 | Federation         | Shovel                 |
| ---- | ------------------ | ---------------------- |
| 方向 | 通常按策略按需拉取 | 明确从源移动到目标     |
| 适用 | 长期跨集群松耦合   | 迁移、桥接、定向转发   |
| 配置 | upstream + policy  | shovel worker          |
| 行为 | 类似跨 Broker 联邦 | 类似内置消息搬运客户端 |

Shovel 设计建议：

1. Shovel 是单向转发工具，不是队列副本机制。
2. Shovel 转发可能导致目标端重复消费，消费者必须幂等。
3. 迁移前要确认源队列、目标 Exchange、Routing Key 和权限。
4. Shovel 链路需要监控连接状态、转发速率和失败次数。
5. 长期稳定跨集群流量可考虑 Federation，明确搬运场景可考虑 Shovel。

### 客户端自动恢复

RabbitMQ Java 客户端支持网络故障后的自动恢复，包括重连、重新打开 Channel、恢复监听器、恢复 QoS、Publisher Confirm、事务设置以及重新声明拓扑等。官方 Java Client 文档说明，从 Java Client 4.0.0 开始，自动恢复和拓扑恢复默认启用。([rabbitmq.com](https://www.rabbitmq.com/client-libraries/java-api-guide?utm_source=chatgpt.com))

Spring Boot 应用中通常通过 Spring AMQP 管理连接和监听容器，网络抖动后会尝试恢复连接和消费者。应用侧仍需要做到：

| 能力           | 说明                               |
| -------------- | ---------------------------------- |
| 连接自动恢复   | 连接断开后重新连接                 |
| 消费者自动恢复 | 监听容器恢复消费                   |
| 发布失败补偿   | 发送异常写消息表，后续重试         |
| 消费幂等       | 恢复后重复投递不重复处理           |
| 日志告警       | 连接断开、恢复、失败都有日志和告警 |

连接恢复配置示例：

```yaml
spring:
  rabbitmq:
    addresses: rabbitmq-01.internal:5672,rabbitmq-02.internal:5672,rabbitmq-03.internal:5672
    connection-timeout: 10s
    requested-heartbeat: 30s
    template:
      retry:
        # RabbitTemplate 调用异常重试
        enabled: true
        initial-interval: 1s
        max-attempts: 3
        max-interval: 5s
        multiplier: 2
```

客户端恢复注意事项：

1. 自动恢复不能替代消息可靠性设计。
2. 生产者发送失败仍需要本地消息表补偿。
3. 消费者恢复后可能收到重复消息，必须幂等。
4. 连接恢复期间可能出现短时间发送失败或消费暂停。
5. 连接恢复要结合应用日志和 RabbitMQ 连接数监控。
6. 消费者业务处理成功但 Ack 失败时，恢复后可能重复投递。

### 连接失败重试

连接失败重试用于处理 RabbitMQ 短暂不可用、网络抖动、DNS 切换、节点故障等场景。生产者发送端建议配置 `RabbitTemplate` retry，同时核心业务使用本地消息表兜底。

生产者重试配置：

```yaml
spring:
  rabbitmq:
    template:
      retry:
        # 启用发送端调用重试
        enabled: true
        # 初始重试间隔
        initial-interval: 1s
        # 最大尝试次数
        max-attempts: 3
        # 最大重试间隔
        max-interval: 5s
        # 指数退避倍率
        multiplier: 2
```

连接失败处理流程：

```text
发送消息
    -> RabbitTemplate 调用失败
    -> 短时间 retry
    -> 仍失败，记录 mq_message 状态 FAILED
    -> 定时补偿任务扫描
    -> RabbitMQ 恢复后重发
    -> Confirm 成功后更新 CONFIRMED
```

发送失败兜底示例：

```java
try {
    rabbitTemplate.convertAndSend(exchange, routingKey, payload);
    log.info("MQ消息发送提交成功，messageId={}，exchange={}，routingKey={}", messageId, exchange, routingKey);
} catch (Exception ex) {
    log.error("MQ连接或发送异常，消息进入补偿流程，messageId={}，exchange={}，routingKey={}，原因={}",
            messageId,
            exchange,
            routingKey,
            ex.getMessage(),
            ex
    );

    // 生产环境应更新本地消息表状态为 FAILED，等待补偿任务重发
}
```

连接失败重试建议：

1. 短时间网络抖动可以通过 `RabbitTemplate` retry 处理。
2. 长时间故障必须依赖本地消息表补偿。
3. 重试次数不宜过大，避免业务线程长时间阻塞。
4. 连接失败日志必须包含 Broker 地址、vhost、messageId。
5. 多节点地址可提高客户端故障切换能力。
6. 连接失败期间要监控本地待发送消息数量。

### 故障切换策略

故障切换策略用于在 RabbitMQ 节点、队列 leader、网络或客户端应用异常时，保证系统尽快恢复。故障切换不是只靠 RabbitMQ 集群完成，应用侧和业务侧也必须具备容错能力。

常见故障场景：

| 故障                 | 影响                               | 处理策略                             |
| -------------------- | ---------------------------------- | ------------------------------------ |
| 单个 Broker 节点宕机 | 部分连接断开、部分队列 leader 切换 | 客户端重连，Quorum Queue 重新选主    |
| 队列 leader 不可用   | 该队列短暂不可用                   | Quorum Queue 多数派可用时选新 leader |
| 网络抖动             | 发送失败、消费暂停、Ack 失败       | 自动恢复 + 幂等                      |
| Broker 磁盘不足      | 可能阻塞发布                       | 清理磁盘、扩容、处理堆积             |
| 消费者实例宕机       | 未 Ack 消息重新投递                | 消费幂等 + 自动扩容                  |
| 生产者发送失败       | 消息未投递                         | 本地消息表补偿                       |
| 路由配置错误         | 消息不可路由                       | Return 记录 + 修复后重发             |

故障切换推荐架构：

```text
Producer Service
  -> 本地消息表 / Outbox
  -> RabbitMQ Cluster
      -> Quorum Queue
      -> Consumer Service
          -> 幂等控制
          -> 手动 Ack
          -> 死信队列
          -> 补偿任务
```

故障切换检查清单：

| 检查项     | 标准                                       |
| ---------- | ------------------------------------------ |
| 客户端地址 | 配置多个 RabbitMQ 节点                     |
| 队列类型   | 核心队列使用 Quorum Queue                  |
| 消息发送   | 开启 Confirm 和 Return                     |
| 消息消费   | 手动 Ack，失败进入死信                     |
| 幂等控制   | 重复投递不会重复执行业务                   |
| 本地消息表 | 生产者发送失败可补偿                       |
| 死信处理   | 异常消息可查询、重发、忽略                 |
| 监控告警   | 节点、队列、死信、Confirm、Return 均有告警 |
| 演练机制   | 定期做节点宕机和消费者宕机演练             |

故障演练建议：

```bash
# 查看集群状态
rabbitmqctl cluster_status

# 查看 Quorum Queue 状态
rabbitmqctl list_queues -p /prod name type leader members online

# 查看连接
rabbitmqctl list_connections name user peer_host state channels

# 查看核心队列堆积
rabbitmqctl list_queues -p /prod name messages messages_ready messages_unacknowledged consumers
```

命令说明：`cluster_status` 用于查看 RabbitMQ 集群节点状态；`list_queues` 中的 `leader`、`members`、`online` 可用于观察 Quorum Queue 副本和 leader 状态；连接和队列指标用于判断客户端是否完成恢复。

故障切换建议：

1. 核心业务优先使用 Quorum Queue，而不是普通 classic queue。
2. 客户端必须配置多个 RabbitMQ 节点地址。
3. 生产者必须有本地消息表或 Outbox 兜底。
4. 消费者必须具备幂等能力。
5. 消费失败必须进入死信或补偿流程。
6. 定期演练节点宕机、网络断开、消费者宕机、路由错误等场景。
7. 高可用设计的最终目标不是“永不失败”，而是“失败可恢复、消息可追踪、业务可补偿”。

## 测试设计

本章节用于说明 RabbitMQ 项目的测试体系设计。RabbitMQ 测试不能只验证“消息能发出去”，还需要覆盖生产者发送、消费者消费、Confirm、Return、死信、延迟、幂等、重试和高并发场景。

测试分层建议如下：

| 测试类型            | 目标                                       | 是否依赖 RabbitMQ |
| ------------------- | ------------------------------------------ | ----------------- |
| 单元测试            | 验证消息构造、路由计算、幂等 Key、参数校验 | 否                |
| 集成测试            | 验证 Spring Boot 与 RabbitMQ 配置是否正确  | 是                |
| Testcontainers 测试 | 使用临时 RabbitMQ 容器进行自动化测试       | 是                |
| 生产者测试          | 验证发送、Confirm、Return、Header、消息体  | 是                |
| 消费者测试          | 验证监听、Ack、Nack、幂等、异常处理        | 是                |
| 死信队列测试        | 验证失败消息是否进入死信队列               | 是                |
| 延迟消息测试        | 验证 TTL 或延迟插件消息是否按预期到达      | 是                |
| 幂等测试            | 验证重复消息不会重复执行业务               | 可选              |
| 压力测试            | 验证吞吐量、堆积、消费能力和资源瓶颈       | 是                |

推荐测试依赖如下。

文件位置：`pom.xml`

```xml
<!-- Spring Boot 测试基础依赖：JUnit 5、AssertJ、Mockito 等 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Spring Rabbit 测试支持：RabbitMQ 测试工具、监听容器测试能力 -->
<dependency>
    <groupId>org.springframework.amqp</groupId>
    <artifactId>spring-rabbit-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Testcontainers JUnit 5 支持 -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<!-- Testcontainers RabbitMQ 支持 -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>rabbitmq</artifactId>
    <scope>test</scope>
</dependency>

<!-- Awaitility：用于等待异步消费结果 -->
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <scope>test</scope>
</dependency>
```

### 单元测试

单元测试用于验证不依赖 RabbitMQ Broker 的纯业务逻辑，例如消息 ID 生成、Routing Key 分片、消息参数校验、幂等 Key 生成、消息 DTO 构造等。

单元测试适合覆盖以下内容：

| 测试对象         | 测试重点                             |
| ---------------- | ------------------------------------ |
| Routing Key 工具 | 相同业务 Key 是否路由到相同分片      |
| 幂等 Key 工具    | Key 格式是否稳定                     |
| 消息 DTO         | 必填字段校验                         |
| 消息构造服务     | 基础字段是否完整                     |
| 异常分类工具     | 是否正确区分可重试异常和不可重试异常 |

下面的代码用于测试消息分片工具，确保相同业务主键总是路由到同一个分片。

文件位置：`src/test/java/io/github/atengk/rabbitmq/util/MessageShardUtilTest.java`

```java
package io.github.atengk.rabbitmq.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 消息分片工具测试
 *
 * @author Ateng
 * @since 2026-05-11
 */
class MessageShardUtilTest {

    @Test
    void shouldReturnSameShardWhenBusinessKeySame() {
        String businessKey = "ORDER202605110001";
        int shardCount = 8;

        int firstShard = MessageShardUtil.shardIndex(businessKey, shardCount);
        int secondShard = MessageShardUtil.shardIndex(businessKey, shardCount);

        Assertions.assertEquals(firstShard, secondShard);
        Assertions.assertTrue(firstShard >= 0);
        Assertions.assertTrue(firstShard < shardCount);
    }

    @Test
    void shouldBuildRoutingKey() {
        String routingKey = MessageShardUtil.buildRoutingKey("ordered.order.", "ORDER202605110001", 4);

        Assertions.assertTrue(routingKey.startsWith("ordered.order."));
    }

    @Test
    void shouldThrowExceptionWhenShardCountInvalid() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> MessageShardUtil.shardIndex("ORDER001", 0));
    }
}
```

下面的代码用于测试 MQ Redis Key 工具，保证幂等 Key、锁 Key、重试 Key 的格式稳定。

文件位置：`src/test/java/io/github/atengk/rabbitmq/util/MqRedisKeyUtilTest.java`

```java
package io.github.atengk.rabbitmq.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * MQ Redis Key 工具测试
 *
 * @author Ateng
 * @since 2026-05-11
 */
class MqRedisKeyUtilTest {

    @Test
    void shouldBuildIdempotentKey() {
        String key = MqRedisKeyUtil.idempotentKey("order.paid", "ORDER202605110001");

        Assertions.assertEquals("mq:idempotent:order.paid:ORDER202605110001", key);
    }

    @Test
    void shouldBuildLockKey() {
        String key = MqRedisKeyUtil.lockKey("order.paid", "ORDER202605110001");

        Assertions.assertEquals("mq:lock:order.paid:ORDER202605110001", key);
    }

    @Test
    void shouldBuildRetryCountKey() {
        String key = MqRedisKeyUtil.retryCountKey("msg_001");

        Assertions.assertEquals("mq:retry:count:msg_001", key);
    }
}
```

单元测试建议：

1. 不依赖 RabbitMQ、Redis、数据库等外部组件。
2. 测试速度要快，适合每次构建执行。
3. 工具类、消息构造、参数校验、异常分类必须覆盖。
4. 幂等 Key 和 Routing Key 一旦上线，不应随意变更，应通过单元测试锁定格式。
5. 消息 DTO 必填字段建议结合 Bean Validation 测试。

### 集成测试

集成测试用于验证 Spring Boot 与 RabbitMQ 的集成配置是否正确，包括 Exchange、Queue、Binding 声明、消息转换器、RabbitTemplate、监听容器和 Ack 机制等。

集成测试通常需要真实 RabbitMQ 环境，可以使用本地 RabbitMQ、测试环境 RabbitMQ 或 Testcontainers。为了保证测试隔离，推荐优先使用 Testcontainers。

集成测试配置建议：

文件位置：`src/test/resources/application-test.yml`

```yaml
spring:
  rabbitmq:
    virtual-host: /
    username: guest
    password: guest
    publisher-confirm-type: correlated
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

集成测试基础队列配置如下。

文件位置：`src/test/java/io/github/atengk/rabbitmq/config/TestRabbitMqConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * RabbitMQ 测试资源配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@TestConfiguration
public class TestRabbitMqConfig {

    public static final String TEST_EXCHANGE = "test.rabbit.exchange";

    public static final String TEST_QUEUE = "test.rabbit.queue";

    public static final String TEST_ROUTING_KEY = "test.rabbit";

    @Bean
    public DirectExchange testExchange() {
        return ExchangeBuilder.directExchange(TEST_EXCHANGE).durable(false).build();
    }

    @Bean
    public Queue testQueue() {
        return QueueBuilder.nonDurable(TEST_QUEUE).build();
    }

    @Bean
    public Binding testBinding() {
        return BindingBuilder.bind(testQueue()).to(testExchange()).with(TEST_ROUTING_KEY);
    }
}
```

下面的集成测试用于验证消息可以从 Exchange 正确路由到 Queue。

文件位置：`src/test/java/io/github/atengk/rabbitmq/RabbitMqIntegrationTest.java`

```java
package io.github.atengk.rabbitmq;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.rabbitmq.config.TestRabbitMqConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.nio.charset.StandardCharsets;

/**
 * RabbitMQ 集成测试
 *
 * @author Ateng
 * @since 2026-05-11
 */
@SpringBootTest
@Import(TestRabbitMqConfig.class)
class RabbitMqIntegrationTest {

    private final RabbitTemplate rabbitTemplate;

    RabbitMqIntegrationTest(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Test
    void shouldSendAndReceiveMessage() {
        String payload = "hello-rabbit-" + IdUtil.fastSimpleUUID();

        rabbitTemplate.convertAndSend(
                TestRabbitMqConfig.TEST_EXCHANGE,
                TestRabbitMqConfig.TEST_ROUTING_KEY,
                payload
        );

        Message message = rabbitTemplate.receive(TestRabbitMqConfig.TEST_QUEUE, 5000);
        Assertions.assertNotNull(message);

        String actualPayload = new String(message.getBody(), StandardCharsets.UTF_8);
        Assertions.assertEquals(payload, actualPayload);
    }
}
```

集成测试建议：

1. 测试队列、交换机名称使用 `test.` 前缀，避免污染真实业务资源。
2. 每个测试尽量使用独立队列，避免测试互相影响。
3. 测试结束后清理队列消息。
4. 对异步消费使用 Awaitility 等待结果。
5. 不建议直接依赖共享测试环境 RabbitMQ，容易受其他测试影响。

### Testcontainers 测试

Testcontainers 可以在测试执行时自动启动 RabbitMQ 容器，测试结束后自动销毁，适合 CI/CD 环境和本地自动化测试。

下面的代码使用 Testcontainers 启动 RabbitMQ，并将容器地址动态注入 Spring Boot 配置。

文件位置：`src/test/java/io/github/atengk/rabbitmq/support/RabbitMqContainerSupport.java`

```java
package io.github.atengk.rabbitmq.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;

/**
 * RabbitMQ Testcontainers 测试支持
 *
 * @author Ateng
 * @since 2026-05-11
 */
public abstract class RabbitMqContainerSupport {

    @Container
    protected static final RabbitMQContainer RABBITMQ_CONTAINER = new RabbitMQContainer("rabbitmq:3.13-management")
            .withUser("test", "test")
            .withVhost("/");

    /**
     * 注册 RabbitMQ 动态配置
     *
     * @param registry 动态配置注册器
     */
    protected static void registerRabbitMqProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", RABBITMQ_CONTAINER::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ_CONTAINER::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "test");
        registry.add("spring.rabbitmq.password", () -> "test");
        registry.add("spring.rabbitmq.virtual-host", () -> "/");
    }
}
```

Testcontainers 集成测试示例。

文件位置：`src/test/java/io/github/atengk/rabbitmq/RabbitMqContainerIntegrationTest.java`

```java
package io.github.atengk.rabbitmq;

import io.github.atengk.rabbitmq.config.TestRabbitMqConfig;
import io.github.atengk.rabbitmq.support.RabbitMqContainerSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;

/**
 * RabbitMQ 容器集成测试
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Testcontainers
@SpringBootTest
@Import(TestRabbitMqConfig.class)
class RabbitMqContainerIntegrationTest extends RabbitMqContainerSupport {

    private final RabbitTemplate rabbitTemplate;

    RabbitMqContainerIntegrationTest(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @DynamicPropertySource
    static void rabbitMqProperties(DynamicPropertyRegistry registry) {
        registerRabbitMqProperties(registry);
    }

    @Test
    void shouldSendMessageWithTestcontainersRabbitMq() {
        String payload = "testcontainers-rabbitmq";

        rabbitTemplate.convertAndSend(
                TestRabbitMqConfig.TEST_EXCHANGE,
                TestRabbitMqConfig.TEST_ROUTING_KEY,
                payload
        );

        Message message = rabbitTemplate.receive(TestRabbitMqConfig.TEST_QUEUE, 5000);

        Assertions.assertNotNull(message);
        Assertions.assertEquals(payload, new String(message.getBody(), StandardCharsets.UTF_8));
    }
}
```

Testcontainers 测试建议：

1. CI 环境需要支持 Docker。
2. 测试资源使用非持久化队列，减少清理成本。
3. 不要在容器测试中依赖外部 RabbitMQ。
4. 测试名称、队列名称尽量唯一。
5. 复杂测试可以为每个测试类启动独立容器，也可以复用静态容器提高速度。
6. 延迟消息插件测试需要自定义带插件的 RabbitMQ 镜像。

### 生产者测试

生产者测试用于验证消息发送是否符合预期，包括消息体、Header、消息持久化、Routing Key、Confirm 和 Return。

生产者测试重点如下：

| 测试点               | 说明                                           |
| -------------------- | ---------------------------------------------- |
| 消息是否进入正确队列 | Exchange、Routing Key、Binding 是否正确        |
| Header 是否完整      | `messageId`、`businessKey`、`traceId` 是否存在 |
| 消息是否持久化       | `deliveryMode=PERSISTENT`                      |
| Confirm 是否触发     | 消息到达 Exchange 后回调                       |
| Return 是否触发      | 不可路由消息是否返回                           |

下面的测试验证生产者发送后，队列中可以收到消息，并且 Header 正确。

文件位置：`src/test/java/io/github/atengk/rabbitmq/producer/ProducerSendTest.java`

```java
package io.github.atengk.rabbitmq.producer;

import io.github.atengk.rabbitmq.config.TestRabbitMqConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * MQ 生产者发送测试
 *
 * @author Ateng
 * @since 2026-05-11
 */
@SpringBootTest
@Import(TestRabbitMqConfig.class)
class ProducerSendTest {

    private final RabbitTemplate rabbitTemplate;

    ProducerSendTest(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Test
    void shouldSendMessageWithHeaders() {
        String messageId = "test-message-001";
        String businessKey = "ORDER202605110001";

        rabbitTemplate.convertAndSend(
                TestRabbitMqConfig.TEST_EXCHANGE,
                TestRabbitMqConfig.TEST_ROUTING_KEY,
                "producer-test",
                message -> {
                    message.getMessageProperties().setMessageId(messageId);
                    message.getMessageProperties().setHeader("x-business-key", businessKey);
                    return message;
                }
        );

        Message message = rabbitTemplate.receive(TestRabbitMqConfig.TEST_QUEUE, 5000);

        Assertions.assertNotNull(message);
        Assertions.assertEquals(messageId, message.getMessageProperties().getMessageId());
        Assertions.assertEquals(businessKey, message.getMessageProperties().getHeaders().get("x-business-key"));
    }
}
```

Return 测试示例：发送到存在的 Exchange，但使用无法匹配任何队列的 Routing Key。

```java
@Test
void shouldTriggerReturnWhenMessageUnroutable() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicBoolean returnedFlag = new AtomicBoolean(false);

    rabbitTemplate.setReturnsCallback(returned -> {
        returnedFlag.set(true);
        latch.countDown();
    });

    rabbitTemplate.convertAndSend(
            TestRabbitMqConfig.TEST_EXCHANGE,
            "not.match.routing.key",
            "return-test"
    );

    boolean callbackTriggered = latch.await(5, TimeUnit.SECONDS);

    Assertions.assertTrue(callbackTriggered);
    Assertions.assertTrue(returnedFlag.get());
}
```

生产者测试建议：

1. 每次测试前清空目标队列。
2. Confirm 和 Return 是异步回调，需要使用 `CountDownLatch` 或 Awaitility。
3. Return 测试必须开启 `publisher-returns=true` 和 `mandatory=true`。
4. 生产者测试应验证 Header，而不是只验证消息体。
5. 核心生产者要测试消息记录表状态变化。

### 消费者测试

消费者测试用于验证 `@RabbitListener` 是否能正确消费消息、处理业务、执行 Ack、处理异常和幂等逻辑。消费者测试通常是异步测试，需要等待消费结果。

下面的消费者用于测试消息是否被成功处理。

文件位置：`src/test/java/io/github/atengk/rabbitmq/consumer/TestMessageConsumer.java`

```java
package io.github.atengk.rabbitmq.consumer;

import com.rabbitmq.client.Channel;
import io.github.atengk.rabbitmq.config.TestRabbitMqConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 测试消息消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Getter
@Component
public class TestMessageConsumer {

    private final CopyOnWriteArrayList<String> consumedMessages = new CopyOnWriteArrayList<>();

    /**
     * 消费测试消息
     *
     * @param message 原始消息
     * @param channel RabbitMQ Channel
     * @throws IOException Ack 失败时抛出
     */
    @RabbitListener(queues = TestRabbitMqConfig.TEST_QUEUE)
    public void consume(Message message, Channel channel) throws IOException {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        consumedMessages.add(payload);

        log.info("测试消费者收到消息，payload={}", payload);

        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
```

消费者测试示例。

文件位置：`src/test/java/io/github/atengk/rabbitmq/consumer/ConsumerIntegrationTest.java`

```java
package io.github.atengk.rabbitmq.consumer;

import io.github.atengk.rabbitmq.config.TestRabbitMqConfig;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * MQ 消费者集成测试
 *
 * @author Ateng
 * @since 2026-05-11
 */
@SpringBootTest
@Import(TestRabbitMqConfig.class)
class ConsumerIntegrationTest {

    private final RabbitTemplate rabbitTemplate;

    private final TestMessageConsumer testMessageConsumer;

    ConsumerIntegrationTest(RabbitTemplate rabbitTemplate, TestMessageConsumer testMessageConsumer) {
        this.rabbitTemplate = rabbitTemplate;
        this.testMessageConsumer = testMessageConsumer;
    }

    @Test
    void shouldConsumeMessage() {
        String payload = "consumer-test-message";

        rabbitTemplate.convertAndSend(
                TestRabbitMqConfig.TEST_EXCHANGE,
                TestRabbitMqConfig.TEST_ROUTING_KEY,
                payload
        );

        await().atMost(5, SECONDS).untilAsserted(() ->
                assertThat(testMessageConsumer.getConsumedMessages()).contains(payload)
        );
    }
}
```

消费者测试建议：

1. 消费测试要等待异步结果，不能立即断言。
2. 手动 Ack 模式下要覆盖成功 Ack 和失败 Nack。
3. 消费异常测试应验证消息是否进入死信队列。
4. 幂等消费者要验证重复消息只处理一次。
5. 消费测试中尽量使用测试专用队列，避免干扰其他测试。

### 死信队列测试

死信队列测试用于验证消息被消费者拒绝、TTL 过期或队列超限后，是否进入指定死信队列。

死信测试资源配置如下。

文件位置：`src/test/java/io/github/atengk/rabbitmq/config/TestDeadLetterConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * RabbitMQ 死信测试配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@TestConfiguration
public class TestDeadLetterConfig {

    public static final String BUSINESS_EXCHANGE = "test.dlx.business.exchange";

    public static final String BUSINESS_QUEUE = "test.dlx.business.queue";

    public static final String BUSINESS_ROUTING_KEY = "test.dlx.business";

    public static final String DEAD_EXCHANGE = "test.dlx.dead.exchange";

    public static final String DEAD_QUEUE = "test.dlx.dead.queue";

    public static final String DEAD_ROUTING_KEY = "test.dlx.dead";

    @Bean
    public DirectExchange testDlxBusinessExchange() {
        return ExchangeBuilder.directExchange(BUSINESS_EXCHANGE).durable(false).build();
    }

    @Bean
    public Queue testDlxBusinessQueue() {
        return QueueBuilder.nonDurable(BUSINESS_QUEUE)
                .deadLetterExchange(DEAD_EXCHANGE)
                .deadLetterRoutingKey(DEAD_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding testDlxBusinessBinding() {
        return BindingBuilder.bind(testDlxBusinessQueue()).to(testDlxBusinessExchange()).with(BUSINESS_ROUTING_KEY);
    }

    @Bean
    public DirectExchange testDlxDeadExchange() {
        return ExchangeBuilder.directExchange(DEAD_EXCHANGE).durable(false).build();
    }

    @Bean
    public Queue testDlxDeadQueue() {
        return QueueBuilder.nonDurable(DEAD_QUEUE).build();
    }

    @Bean
    public Binding testDlxDeadBinding() {
        return BindingBuilder.bind(testDlxDeadQueue()).to(testDlxDeadExchange()).with(DEAD_ROUTING_KEY);
    }
}
```

下面的测试直接从业务队列取出消息并拒绝，验证消息进入死信队列。

文件位置：`src/test/java/io/github/atengk/rabbitmq/deadletter/DeadLetterQueueTest.java`

```java
package io.github.atengk.rabbitmq.deadletter;

import com.rabbitmq.client.Channel;
import io.github.atengk.rabbitmq.config.TestDeadLetterConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * RabbitMQ 死信队列测试
 *
 * @author Ateng
 * @since 2026-05-11
 */
@SpringBootTest
@Import(TestDeadLetterConfig.class)
class DeadLetterQueueTest {

    private final RabbitTemplate rabbitTemplate;

    DeadLetterQueueTest(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Test
    void shouldRouteRejectedMessageToDeadLetterQueue() {
        rabbitTemplate.convertAndSend(
                TestDeadLetterConfig.BUSINESS_EXCHANGE,
                TestDeadLetterConfig.BUSINESS_ROUTING_KEY,
                "dead-letter-test"
        );

        rabbitTemplate.execute((ChannelCallback<Void>) channel -> {
            long deliveryTag = basicGetAndReject(channel);
            Assertions.assertTrue(deliveryTag > 0);
            return null;
        });

        Message deadMessage = rabbitTemplate.receive(TestDeadLetterConfig.DEAD_QUEUE, 5000);

        Assertions.assertNotNull(deadMessage);
    }

    private long basicGetAndReject(Channel channel) throws Exception {
        com.rabbitmq.client.GetResponse response = channel.basicGet(TestDeadLetterConfig.BUSINESS_QUEUE, false);
        Assertions.assertNotNull(response);

        long deliveryTag = response.getEnvelope().getDeliveryTag();
        channel.basicReject(deliveryTag, false);
        return deliveryTag;
    }
}
```

死信测试建议：

1. 覆盖 `basicReject(requeue=false)` 场景。
2. 覆盖 `basicNack(requeue=false)` 场景。
3. 覆盖 TTL 过期进入死信场景。
4. 验证死信消息 Header 中是否包含 `x-death`。
5. 死信测试不要使用生产死信队列。

### 延迟消息测试

延迟消息测试用于验证 TTL + 死信方案或延迟插件方案是否按预期工作。常规自动化测试建议优先测试 TTL + 死信方案，因为不依赖插件。

TTL 延迟测试配置如下。

文件位置：`src/test/java/io/github/atengk/rabbitmq/config/TestDelayQueueConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * RabbitMQ 延迟队列测试配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@TestConfiguration
public class TestDelayQueueConfig {

    public static final String DELAY_EXCHANGE = "test.delay.exchange";

    public static final String DELAY_QUEUE = "test.delay.queue";

    public static final String DELAY_ROUTING_KEY = "test.delay";

    public static final String BUSINESS_EXCHANGE = "test.delay.business.exchange";

    public static final String BUSINESS_QUEUE = "test.delay.business.queue";

    public static final String BUSINESS_ROUTING_KEY = "test.delay.business";

    @Bean
    public DirectExchange testDelayExchange() {
        return ExchangeBuilder.directExchange(DELAY_EXCHANGE).durable(false).build();
    }

    @Bean
    public Queue testDelayQueue() {
        return QueueBuilder.nonDurable(DELAY_QUEUE)
                .ttl(2000)
                .deadLetterExchange(BUSINESS_EXCHANGE)
                .deadLetterRoutingKey(BUSINESS_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding testDelayBinding() {
        return BindingBuilder.bind(testDelayQueue()).to(testDelayExchange()).with(DELAY_ROUTING_KEY);
    }

    @Bean
    public DirectExchange testDelayBusinessExchange() {
        return ExchangeBuilder.directExchange(BUSINESS_EXCHANGE).durable(false).build();
    }

    @Bean
    public Queue testDelayBusinessQueue() {
        return QueueBuilder.nonDurable(BUSINESS_QUEUE).build();
    }

    @Bean
    public Binding testDelayBusinessBinding() {
        return BindingBuilder.bind(testDelayBusinessQueue()).to(testDelayBusinessExchange()).with(BUSINESS_ROUTING_KEY);
    }
}
```

下面的测试验证消息在延迟队列中等待 TTL 后进入业务队列。

文件位置：`src/test/java/io/github/atengk/rabbitmq/delay/DelayMessageTest.java`

```java
package io.github.atengk.rabbitmq.delay;

import io.github.atengk.rabbitmq.config.TestDelayQueueConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * RabbitMQ 延迟消息测试
 *
 * @author Ateng
 * @since 2026-05-11
 */
@SpringBootTest
@Import(TestDelayQueueConfig.class)
class DelayMessageTest {

    private final RabbitTemplate rabbitTemplate;

    DelayMessageTest(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Test
    void shouldReceiveMessageAfterDelay() {
        rabbitTemplate.convertAndSend(
                TestDelayQueueConfig.DELAY_EXCHANGE,
                TestDelayQueueConfig.DELAY_ROUTING_KEY,
                "delay-test"
        );

        Message immediateMessage = rabbitTemplate.receive(TestDelayQueueConfig.BUSINESS_QUEUE, 500);
        Assertions.assertNull(immediateMessage);

        Message delayedMessage = rabbitTemplate.receive(TestDelayQueueConfig.BUSINESS_QUEUE, 5000);
        Assertions.assertNotNull(delayedMessage);
    }
}
```

延迟消息测试建议：

1. 自动化测试中 TTL 不要设置太长，通常 1 到 3 秒即可。
2. 插件延迟测试需要确保测试 RabbitMQ 镜像已安装插件。
3. 延迟测试不要用严格等于某个毫秒值断言，应允许误差。
4. TTL + 死信测试应验证最终业务队列能收到消息。
5. 订单超时关闭类测试应验证消费者先检查业务状态，而不是直接执行关闭。

### 幂等测试

幂等测试用于验证重复投递同一业务消息时，消费者不会重复执行业务副作用。幂等测试可以基于 Redis、数据库唯一索引或内存模拟服务完成。

下面的内存幂等服务用于测试场景。

文件位置：`src/test/java/io/github/atengk/rabbitmq/support/InMemoryIdempotentService.java`

```java
package io.github.atengk.rabbitmq.support;

import cn.hutool.core.util.StrUtil;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存幂等服务
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class InMemoryIdempotentService {

    private final Set<String> consumedKeys = ConcurrentHashMap.newKeySet();

    /**
     * 尝试消费
     *
     * @param eventType   事件类型
     * @param businessKey 业务主键
     * @return 是否允许消费
     */
    public boolean tryConsume(String eventType, String businessKey) {
        String key = StrUtil.format("{}:{}", eventType, businessKey);
        return consumedKeys.add(key);
    }

    /**
     * 获取已消费数量
     *
     * @return 已消费数量
     */
    public int consumedCount() {
        return consumedKeys.size();
    }
}
```

幂等测试示例。

文件位置：`src/test/java/io/github/atengk/rabbitmq/idempotent/IdempotentConsumeTest.java`

```java
package io.github.atengk.rabbitmq.idempotent;

import io.github.atengk.rabbitmq.support.InMemoryIdempotentService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * MQ 幂等消费测试
 *
 * @author Ateng
 * @since 2026-05-11
 */
class IdempotentConsumeTest {

    @Test
    void shouldOnlyConsumeOnceWhenMessageDuplicated() {
        InMemoryIdempotentService service = new InMemoryIdempotentService();

        boolean firstAllowed = service.tryConsume("order.paid", "ORDER202605110001");
        boolean secondAllowed = service.tryConsume("order.paid", "ORDER202605110001");

        Assertions.assertTrue(firstAllowed);
        Assertions.assertFalse(secondAllowed);
        Assertions.assertEquals(1, service.consumedCount());
    }

    @Test
    void shouldAllowDifferentEventTypeWithSameBusinessKey() {
        InMemoryIdempotentService service = new InMemoryIdempotentService();

        boolean orderCreatedAllowed = service.tryConsume("order.created", "ORDER202605110001");
        boolean orderPaidAllowed = service.tryConsume("order.paid", "ORDER202605110001");

        Assertions.assertTrue(orderCreatedAllowed);
        Assertions.assertTrue(orderPaidAllowed);
        Assertions.assertEquals(2, service.consumedCount());
    }
}
```

幂等测试建议：

1. 测试相同 `eventType + businessKey` 重复消费。
2. 测试不同 `eventType` 但相同 `businessKey` 不互相影响。
3. 测试并发重复消费场景。
4. 测试消费成功后 Ack 失败导致的重复投递。
5. 测试死信重发不会重复执行业务副作用。
6. 核心业务要结合数据库状态机做幂等测试。

### 压力测试

压力测试用于评估 RabbitMQ 消息发送能力、消费能力、堆积情况、Broker 资源消耗和应用瓶颈。压测前必须使用独立环境，不应在生产环境直接压测。

压测指标如下：

| 指标           | 说明                   |
| -------------- | ---------------------- |
| 生产速率       | 每秒发送消息数         |
| 消费速率       | 每秒 Ack 消息数        |
| 平均消费耗时   | 单条消息平均处理时间   |
| P95 / P99 耗时 | 慢消息处理时间         |
| Ready 数量     | 队列等待消息数         |
| Unacked 数量   | 未确认消息数           |
| 死信数量       | 消费失败或过期消息数   |
| CPU / 内存     | 应用和 Broker 资源使用 |
| 磁盘 IO        | 持久化消息写入压力     |
| 网络 IO        | 消息传输压力           |

简单压测生产者示例。

文件位置：`src/test/java/io/github/atengk/rabbitmq/performance/RabbitMqPressureProducerTest.java`

```java
package io.github.atengk.rabbitmq.performance;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.IdUtil;
import io.github.atengk.rabbitmq.config.TestRabbitMqConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * RabbitMQ 简单压力发送测试
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@SpringBootTest
@Import(TestRabbitMqConfig.class)
class RabbitMqPressureProducerTest {

    private final RabbitTemplate rabbitTemplate;

    RabbitMqPressureProducerTest(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Test
    void shouldSendManyMessages() {
        int total = 10_000;
        StopWatch stopWatch = new StopWatch("rabbitmq-pressure-send");
        stopWatch.start("send");

        for (int i = 0; i < total; i++) {
            String payload = "pressure-message-" + i + "-" + IdUtil.fastSimpleUUID();
            rabbitTemplate.convertAndSend(
                    TestRabbitMqConfig.TEST_EXCHANGE,
                    TestRabbitMqConfig.TEST_ROUTING_KEY,
                    payload
            );
        }

        stopWatch.stop();

        double seconds = stopWatch.getTotalTimeSeconds();
        double tps = total / seconds;

        log.info("RabbitMQ压测发送完成，total={}，seconds={}，tps={}", total, seconds, tps);
    }
}
```

压力测试建议：

1. 使用独立压测 vhost。
2. 压测前清理队列，压测后清理消息。
3. 分别测试非持久化消息和持久化消息。
4. 分别测试单生产者、多生产者。
5. 分别测试单消费者、多消费者。
6. 观察 RabbitMQ 管理控制台、应用日志、Prometheus 指标。
7. 不要只看发送 TPS，还要看消费 Ack 速率。
8. 如果压测导致堆积，要记录堆积恢复时间。

## 本地调试

本章节用于说明开发人员如何在本地启动 RabbitMQ、查看消息、手动发送测试消息、断点调试消费者、模拟消息堆积、模拟死信和模拟延迟消息。本地调试的目标是快速验证功能，不应连接生产 RabbitMQ。

### 本地 RabbitMQ 启动

推荐使用 Docker Compose 启动本地 RabbitMQ 管理版镜像。

文件位置：`docker/docker-compose-rabbitmq.yml`

```yaml
services:
  rabbitmq:
    image: rabbitmq:3.13-management
    container_name: rabbitmq-local
    restart: unless-stopped
    ports:
      # AMQP 连接端口
      - "5672:5672"
      # 管理控制台端口
      - "15672:15672"
    environment:
      # 本地开发用户名
      RABBITMQ_DEFAULT_USER: ateng
      # 本地开发密码
      RABBITMQ_DEFAULT_PASS: ateng123
      # 本地开发 vhost
      RABBITMQ_DEFAULT_VHOST: /dev
    volumes:
      # RabbitMQ 数据持久化
      - rabbitmq-local-data:/var/lib/rabbitmq

volumes:
  rabbitmq-local-data:
```

启动命令：

```bash
# 启动本地 RabbitMQ
docker compose -f docker/docker-compose-rabbitmq.yml up -d

# 查看容器状态
docker ps | grep rabbitmq-local

# 查看 RabbitMQ 日志
docker logs -f rabbitmq-local
```

本地 Spring Boot 配置：

文件位置：`src/main/resources/application-local.yml`

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    virtual-host: /dev
    username: ateng
    password: ateng123
    publisher-confirm-type: correlated
    publisher-returns: true
    template:
      mandatory: true
    listener:
      simple:
        acknowledge-mode: manual
        concurrency: 1
        max-concurrency: 4
        prefetch: 10
        default-requeue-rejected: false
```

启动应用：

```bash
# 使用 local profile 启动
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

本地启动建议：

1. 本地使用 `/dev` vhost。
2. 本地账号不要复用测试或生产账号。
3. 本地队列命名可以加 `local.` 或 `test.` 前缀。
4. 本地调试结束后可以删除容器和数据卷，避免历史消息干扰。

### 管理控制台查看消息

本地管理控制台地址：

```text
http://localhost:15672
```

登录信息：

```text
用户名：ateng
密码：ateng123
```

查看队列消息步骤：

1. 打开管理控制台。
2. 进入 `Queues and Streams`。
3. 选择目标 vhost：`/dev`。
4. 点击目标队列。
5. 查看 `Ready`、`Unacked`、`Consumers`。
6. 在 `Get messages` 区域手动拉取消息。

管理控制台常用字段：

| 字段          | 说明                 |
| ------------- | -------------------- |
| Ready         | 等待消费的消息       |
| Unacked       | 已投递但未确认的消息 |
| Total         | 队列总消息数         |
| Consumers     | 消费者数量           |
| Incoming      | 消息进入速率         |
| Deliver / get | 消息投递速率         |
| Ack           | 消费确认速率         |

查看消息时的 Ack Mode：

| Ack Mode                    | 说明             |
| --------------------------- | ---------------- |
| `Nack message requeue true` | 查看后重新入队   |
| `Ack message requeue false` | 查看后确认删除   |
| `Reject requeue false`      | 拒绝且不重新入队 |

本地调试建议：查看生产或测试队列消息时，不要随意使用会删除消息的 Ack 模式。本地环境可以自由操作，生产环境必须谨慎。

### 手动发送测试消息

可以通过管理控制台、HTTP 接口、`rabbitmqadmin` 或应用测试接口发送消息。

方式一：通过 RabbitMQ 管理控制台发送。

操作步骤：

1. 进入 `Exchanges`。
2. 点击目标 Exchange。
3. 找到 `Publish message`。
4. 填写 Routing Key。
5. 填写 Headers 和 Payload。
6. 点击 `Publish message`。

测试消息示例：

```json
{
  "messageId": "manual_test_20260511103000",
  "eventType": "order.created",
  "businessKey": "ORDER202605110001",
  "timestamp": "2026-05-11 10:30:00",
  "data": {
    "orderId": "ORDER202605110001",
    "userId": 10001,
    "amount": 199.00
  }
}
```

方式二：通过接口发送。

```bash
curl -X POST 'http://localhost:8080/api/mq/messages/send' \
  -H 'Content-Type: application/json' \
  -d '{
    "exchange": "business.order.exchange",
    "routingKey": "order.created",
    "businessType": "order_created",
    "businessKey": "ORDER202605110001",
    "payload": {
      "orderId": "ORDER202605110001",
      "userId": 10001,
      "amount": 199.00
    }
  }'
```

方式三：通过 `rabbitmqadmin` 发送。

```bash
rabbitmqadmin -H localhost -P 15672 -u ateng -p ateng123 -V /dev \
  publish exchange=business.order.exchange routing_key=order.created \
  payload='{"messageId":"manual_test_001","businessKey":"ORDER202605110001"}'
```

手动发送建议：

1. 本地手动发送要指定正确 vhost。
2. Routing Key 要和 Binding 匹配。
3. JSON 消息体要保证格式合法。
4. 如果消费者接收 DTO，字段类型要匹配。
5. 测试消息建议使用明显的 `messageId` 前缀，例如 `manual_test_`。

### 消费断点调试

消费者断点调试用于观察消息进入 Listener 后的处理过程。由于 RabbitMQ 消费是异步线程执行，断点调试时要注意 Ack 超时、消息堆积和重复投递。

断点调试步骤：

1. 本地启动 RabbitMQ。
2. 本地启动 Spring Boot 应用 Debug 模式。
3. 在 `@RabbitListener` 方法第一行打断点。
4. 发送测试消息。
5. 等待消费者线程进入断点。
6. 查看 `payload`、`MessageProperties`、Header、deliveryTag。
7. 单步执行业务逻辑。
8. 确认是否执行 Ack 或 Nack。

消费者调试示例。

```java
@RabbitListener(queues = "debug.order.queue")
public void consume(String payload, Message message, Channel channel) throws IOException {
    long deliveryTag = message.getMessageProperties().getDeliveryTag();
    String messageId = message.getMessageProperties().getMessageId();

    log.info("调试消费者收到消息，messageId={}，deliveryTag={}，payload={}", messageId, deliveryTag, payload);

    // 在这里打断点，观察消息体、Header、deliveryTag
    handleBusiness(payload);

    channel.basicAck(deliveryTag, false);
}
```

断点调试建议：

1. 本地调试时将消费者并发设置为 1。
2. `prefetch` 设置为 1，避免一次拉取多条消息。
3. 手动 Ack 模式下，断点停留期间消息会处于 Unacked。
4. 如果调试时停止应用，未 Ack 消息会重新入队。
5. 不要在共享测试环境长时间断点消费者，否则可能造成队列堆积。
6. 断点调试核心业务时要使用测试消息，避免误处理真实业务数据。

本地调试推荐配置：

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        acknowledge-mode: manual
        concurrency: 1
        max-concurrency: 1
        prefetch: 1
```

### 消息堆积模拟

消息堆积模拟用于测试队列堆积、消费者扩容、监控告警和堆积恢复能力。本地可以通过暂停消费者或降低消费速度来制造堆积。

方式一：只发送消息，不启动消费者。

```bash
for i in $(seq 1 1000); do
  rabbitmqadmin -H localhost -P 15672 -u ateng -p ateng123 -V /dev \
    publish exchange=business.order.exchange routing_key=order.created \
    payload="{\"messageId\":\"backlog_test_${i}\",\"businessKey\":\"ORDER${i}\"}"
done
```

方式二：消费者中增加 sleep 模拟慢消费。

```java
@RabbitListener(queues = "backlog.test.queue")
public void consume(String payload, Message message, Channel channel) throws Exception {
    long deliveryTag = message.getMessageProperties().getDeliveryTag();

    log.info("慢消费者收到消息，payload={}", payload);

    // 模拟业务处理耗时
    Thread.sleep(3000);

    channel.basicAck(deliveryTag, false);
}
```

方式三：关闭消费开关，让消息重新入队或暂停处理。

```yaml
app:
  mq:
    dynamic:
      consume-enabled: false
```

堆积观察指标：

| 指标        | 预期变化           |
| ----------- | ------------------ |
| Ready       | 持续增长           |
| Unacked     | 慢消费者下可能增长 |
| Consumers   | 有消费者时大于 0   |
| Ack rate    | 低于 Publish rate  |
| 内存 / 磁盘 | 随消息堆积增长     |

堆积模拟建议：

1. 使用本地或压测环境，不要在生产环境模拟。
2. 先从 1000 条小消息开始，逐步增加。
3. 观察管理控制台中的 Ready、Unacked、Ack rate。
4. 测试扩容消费者后堆积恢复速度。
5. 测试告警阈值是否及时触发。
6. 模拟结束后清理队列。

清空本地测试队列：

```bash
rabbitmqadmin -H localhost -P 15672 -u ateng -p ateng123 -V /dev \
  purge queue name=backlog.test.queue
```

### 死信消息模拟

死信消息模拟用于验证死信队列配置、死信消费者、死信入库、死信告警和死信重发流程。

方式一：消费者主动 Nack 且不重新入队。

```java
@RabbitListener(queues = "order.paid.queue")
public void consume(String payload, Message message, Channel channel) throws IOException {
    long deliveryTag = message.getMessageProperties().getDeliveryTag();
    String messageId = message.getMessageProperties().getMessageId();

    log.error("模拟消费失败，消息进入死信，messageId={}，payload={}", messageId, payload);

    // requeue=false，若队列配置死信交换机，则消息进入死信队列
    channel.basicNack(deliveryTag, false, false);
}
```

方式二：发送格式错误消息，让消费者参数校验失败。

```bash
rabbitmqadmin -H localhost -P 15672 -u ateng -p ateng123 -V /dev \
  publish exchange=business.order.exchange routing_key=order.paid \
  payload='{"messageId":"dead_test_001","orderId":""}'
```

方式三：使用 TTL 过期进入死信。

```bash
rabbitmqadmin -H localhost -P 15672 -u ateng -p ateng123 -V /dev \
  publish exchange=delay.order.exchange routing_key=order.close.delay \
  payload='{"messageId":"ttl_dead_test_001","orderId":"ORDER202605110001"}'
```

查看死信队列：

```bash
rabbitmqadmin -H localhost -P 15672 -u ateng -p ateng123 -V /dev \
  get queue=order.paid.dead.queue ackmode=ack_requeue_true
```

死信模拟验证点：

| 验证点                    | 说明             |
| ------------------------- | ---------------- |
| 死信队列是否收到消息      | Queue Ready 增加 |
| Header 是否包含 `x-death` | 判断死信原因     |
| 死信消费者是否消费        | 查看应用日志     |
| 死信是否入库              | 查询死信消息表   |
| 死信告警是否触发          | 查看告警系统     |
| 死信重发是否成功          | 修复后重新发送   |

死信模拟建议：

1. 本地先验证 Nack 场景。
2. 再验证 TTL 过期场景。
3. 再验证消费者异常场景。
4. 不要在生产环境随意模拟死信。
5. 死信消息查看后使用 `ack_requeue_true`，避免误删除。
6. 死信重发前确认消费者幂等有效。

### 延迟消息模拟

延迟消息模拟用于验证订单超时关闭、延迟通知、延迟重试等功能。常见方式包括 TTL + 死信方案和延迟插件方案。

TTL + 死信方式模拟：

```bash
rabbitmqadmin -H localhost -P 15672 -u ateng -p ateng123 -V /dev \
  publish exchange=delay.order.exchange routing_key=order.close.delay \
  payload='{"messageId":"delay_test_001","orderId":"ORDER202605110001"}'
```

观察步骤：

1. 发送消息后，查看延迟队列 `delay.order.close.queue`。
2. TTL 未到期前，业务队列不应收到消息。
3. TTL 到期后，消息通过死信交换机进入业务队列 `order.close.queue`。
4. 订单关闭消费者收到消息。
5. 消费者先查询订单状态，再决定是否关闭订单。

延迟插件方式模拟：

```bash
rabbitmqadmin -H localhost -P 15672 -u ateng -p ateng123 -V /dev \
  publish exchange=plugin.delayed.order.exchange routing_key=plugin.order.close \
  payload='{"messageId":"plugin_delay_test_001","orderId":"ORDER202605110001"}' \
  properties='{"headers":{"x-delay":5000}}'
```

延迟消费者日志示例：

```text
收到订单延迟关闭消息，messageId=delay_test_001，orderId=ORDER202605110001
检查订单支付状态并关闭未支付订单，orderId=ORDER202605110001
订单已支付，忽略延迟关闭消息，orderId=ORDER202605110001
```

延迟消息模拟建议：

1. 本地 TTL 设置短一些，例如 5 秒或 10 秒。
2. 验证延迟消息不是精确定时器，允许存在小范围延迟。
3. 消费端必须检查业务状态。
4. 延迟消息也要有 `messageId` 和 `businessKey`。
5. 延迟插件需要确认 RabbitMQ 已安装 `rabbitmq_delayed_message_exchange` 插件。
6. 长周期延迟任务不要只依赖 RabbitMQ，本地调试可以模拟，生产应结合任务表和补偿机制。

本地调试最终建议：开发人员应优先掌握“启动 RabbitMQ、发送消息、查看队列、观察 Header、断点消费、模拟死信、模拟堆积、模拟延迟”这几类操作。只要本地能稳定复现，测试和生产环境排查效率会明显提高。

## 部署方案

本章节用于说明 RabbitMQ 相关 Spring Boot 应用的部署方式，包括 Docker 镜像构建、Docker Compose 部署、Kubernetes 部署、环境变量配置、健康检查、灰度发布和回滚策略。部署方案的目标是保证应用可重复构建、配置可隔离、服务可观测、发布可灰度、异常可回滚。

### Docker 镜像构建

Docker 镜像构建用于将 Spring Boot RabbitMQ 应用打包为可部署镜像。推荐使用多阶段构建，先通过 Maven 构建 Jar，再使用 JRE 基础镜像运行，减少最终镜像体积。

文件位置：`Dockerfile`

```dockerfile
# 第一阶段：使用 Maven 构建 Spring Boot 应用
FROM maven:3.9.9-eclipse-temurin-21 AS builder

# 设置构建工作目录
WORKDIR /build

# 复制 Maven 配置和源码
COPY pom.xml .
COPY src ./src

# 构建应用，跳过测试可提升镜像构建速度
RUN mvn clean package -DskipTests

# 第二阶段：使用 JRE 镜像运行应用
FROM eclipse-temurin:21-jre

# 设置应用工作目录
WORKDIR /app

# 设置 JVM 时区
ENV TZ=Asia/Shanghai

# 复制构建产物
COPY --from=builder /build/target/*.jar /app/app.jar

# 暴露应用端口
EXPOSE 8080

# 启动 Spring Boot 应用
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

构建镜像命令如下：

```bash
# 构建 Docker 镜像
docker build -t rabbitmq-demo:1.0.0 .

# 查看镜像
docker images | grep rabbitmq-demo

# 本地运行镜像
docker run -d \
  --name rabbitmq-demo \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=local \
  -e RABBITMQ_HOST=host.docker.internal \
  -e RABBITMQ_PORT=5672 \
  -e RABBITMQ_VHOST=/dev \
  -e RABBITMQ_USERNAME=ateng \
  -e RABBITMQ_PASSWORD=ateng123 \
  rabbitmq-demo:1.0.0
```

命令说明：`docker build` 用于构建镜像；`SPRING_PROFILES_ACTIVE` 用于指定 Spring Boot 环境；RabbitMQ 连接参数通过环境变量注入，避免写死在镜像中。

镜像构建建议：

| 项目     | 建议                                     |
| -------- | ---------------------------------------- |
| 基础镜像 | 使用 JRE 镜像运行，不使用完整 JDK        |
| 配置注入 | 通过环境变量、Secret 或配置中心注入      |
| 镜像标签 | 使用版本号、Git Commit、构建时间标识     |
| 日志输出 | 应用日志输出到控制台，由容器平台采集     |
| 健康检查 | 暴露 Actuator health 端点                |
| 安全     | 镜像中不包含生产密码、证书私钥、敏感配置 |

如果需要加入 JVM 参数，可以使用环境变量方式。

```dockerfile
# 支持外部注入 JVM 参数
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/app.jar"]
```

运行时指定 JVM 参数：

```bash
docker run -d \
  --name rabbitmq-demo \
  -p 8080:8080 \
  -e JAVA_OPTS="-Xms512m -Xmx512m -XX:+UseG1GC" \
  -e SPRING_PROFILES_ACTIVE=prod \
  rabbitmq-demo:1.0.0
```

### Docker Compose 部署

Docker Compose 适合本地联调、测试环境、小规模部署或演示环境。可以同时启动 RabbitMQ、Spring Boot 应用、Prometheus、Grafana 等组件。

下面的配置启动 RabbitMQ 和业务应用。

文件位置：`docker/docker-compose.yml`

```yaml
services:
  rabbitmq:
    image: rabbitmq:3.13-management
    container_name: rabbitmq
    restart: unless-stopped
    ports:
      # RabbitMQ AMQP 端口
      - "5672:5672"
      # RabbitMQ 管理控制台端口
      - "15672:15672"
    environment:
      # RabbitMQ 管理账号
      RABBITMQ_DEFAULT_USER: ateng
      # RabbitMQ 管理密码
      RABBITMQ_DEFAULT_PASS: ateng123
      # 默认 Virtual Host
      RABBITMQ_DEFAULT_VHOST: /dev
    volumes:
      # RabbitMQ 数据持久化
      - rabbitmq-data:/var/lib/rabbitmq
    healthcheck:
      # 检查 RabbitMQ 服务是否可用
      test: ["CMD", "rabbitmq-diagnostics", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  rabbitmq-demo:
    image: rabbitmq-demo:1.0.0
    container_name: rabbitmq-demo
    restart: unless-stopped
    depends_on:
      rabbitmq:
        condition: service_healthy
    ports:
      # Spring Boot 应用端口
      - "8080:8080"
    environment:
      # Spring Boot 启动环境
      SPRING_PROFILES_ACTIVE: local
      # RabbitMQ 连接配置
      RABBITMQ_HOST: rabbitmq
      RABBITMQ_PORT: 5672
      RABBITMQ_VHOST: /dev
      RABBITMQ_USERNAME: ateng
      RABBITMQ_PASSWORD: ateng123
      # JVM 参数
      JAVA_OPTS: "-Xms512m -Xmx512m -XX:+UseG1GC"
    healthcheck:
      # 检查 Spring Boot Actuator 健康状态
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 15s
      timeout: 5s
      retries: 5

volumes:
  rabbitmq-data:
```

启动命令如下：

```bash
# 构建业务镜像
docker build -t rabbitmq-demo:1.0.0 .

# 启动服务
docker compose -f docker/docker-compose.yml up -d

# 查看服务状态
docker compose -f docker/docker-compose.yml ps

# 查看业务应用日志
docker compose -f docker/docker-compose.yml logs -f rabbitmq-demo

# 查看 RabbitMQ 日志
docker compose -f docker/docker-compose.yml logs -f rabbitmq
```

验证命令如下：

```bash
# 查看应用健康状态
curl http://localhost:8080/actuator/health

# 查看 RabbitMQ 管理控制台
# 浏览器访问 http://localhost:15672
```

Docker Compose 部署建议：

1. 本地和测试环境可以使用 Compose。
2. 生产环境更推荐 Kubernetes、ECS 或专用部署平台。
3. Compose 文件中不要写生产真实密码。
4. RabbitMQ 数据卷需要持久化。
5. 应用启动要等待 RabbitMQ 健康检查通过。
6. 本地调试时可以直接查看容器日志和管理控制台。

### Kubernetes 部署

Kubernetes 部署适合生产环境或标准化测试环境。推荐将 RabbitMQ 作为独立中间件集群部署，业务应用通过 Service 或内部域名访问 RabbitMQ。业务应用本身使用 Deployment 部署，通过 ConfigMap 和 Secret 注入配置。

推荐资源结构如下：

```text
k8s/
├── namespace.yaml
├── rabbitmq-demo-configmap.yaml
├── rabbitmq-demo-secret.yaml
├── rabbitmq-demo-deployment.yaml
├── rabbitmq-demo-service.yaml
└── rabbitmq-demo-hpa.yaml
```

命名空间配置如下。

文件位置：`k8s/namespace.yaml`

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: business
```

ConfigMap 用于保存非敏感配置。

文件位置：`k8s/rabbitmq-demo-configmap.yaml`

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: rabbitmq-demo-config
  namespace: business
data:
  # Spring Boot 启动环境
  SPRING_PROFILES_ACTIVE: "prod"
  # RabbitMQ 地址
  RABBITMQ_HOST: "rabbitmq-prod.internal"
  # RabbitMQ 端口
  RABBITMQ_PORT: "5672"
  # RabbitMQ Virtual Host
  RABBITMQ_VHOST: "/prod"
  # JVM 参数
  JAVA_OPTS: "-Xms512m -Xmx512m -XX:+UseG1GC"
```

Secret 用于保存敏感配置。

文件位置：`k8s/rabbitmq-demo-secret.yaml`

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: rabbitmq-demo-secret
  namespace: business
type: Opaque
stringData:
  # RabbitMQ 用户名
  RABBITMQ_USERNAME: "order_producer"
  # RabbitMQ 密码
  RABBITMQ_PASSWORD: "Producer@123456"
```

Deployment 配置如下。

文件位置：`k8s/rabbitmq-demo-deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: rabbitmq-demo
  namespace: business
  labels:
    app: rabbitmq-demo
spec:
  replicas: 2
  selector:
    matchLabels:
      app: rabbitmq-demo
  strategy:
    type: RollingUpdate
    rollingUpdate:
      # 灰度发布时最多新增 1 个 Pod
      maxSurge: 1
      # 发布过程中最多允许 0 个不可用
      maxUnavailable: 0
  template:
    metadata:
      labels:
        app: rabbitmq-demo
    spec:
      containers:
        - name: rabbitmq-demo
          image: registry.example.com/business/rabbitmq-demo:1.0.0
          imagePullPolicy: IfNotPresent
          ports:
            - name: http
              containerPort: 8080
          envFrom:
            # 注入非敏感配置
            - configMapRef:
                name: rabbitmq-demo-config
            # 注入敏感配置
            - secretRef:
                name: rabbitmq-demo-secret
          resources:
            requests:
              cpu: "500m"
              memory: "512Mi"
            limits:
              cpu: "1"
              memory: "1Gi"
          readinessProbe:
            # 就绪检查，决定是否接入流量
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
            timeoutSeconds: 3
            failureThreshold: 3
          livenessProbe:
            # 存活检查，失败后 Kubernetes 会重启容器
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 20
            timeoutSeconds: 3
            failureThreshold: 3
```

Service 配置如下。

文件位置：`k8s/rabbitmq-demo-service.yaml`

```yaml
apiVersion: v1
kind: Service
metadata:
  name: rabbitmq-demo
  namespace: business
spec:
  type: ClusterIP
  selector:
    app: rabbitmq-demo
  ports:
    - name: http
      port: 8080
      targetPort: 8080
```

HPA 配置示例。

文件位置：`k8s/rabbitmq-demo-hpa.yaml`

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: rabbitmq-demo
  namespace: business
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: rabbitmq-demo
  minReplicas: 2
  maxReplicas: 10
  metrics:
    # 根据 CPU 使用率扩缩容
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
```

部署命令如下：

```bash
# 创建命名空间
kubectl apply -f k8s/namespace.yaml

# 创建配置和密钥
kubectl apply -f k8s/rabbitmq-demo-configmap.yaml
kubectl apply -f k8s/rabbitmq-demo-secret.yaml

# 部署应用
kubectl apply -f k8s/rabbitmq-demo-deployment.yaml
kubectl apply -f k8s/rabbitmq-demo-service.yaml
kubectl apply -f k8s/rabbitmq-demo-hpa.yaml

# 查看 Pod 状态
kubectl get pod -n business -l app=rabbitmq-demo

# 查看应用日志
kubectl logs -f deployment/rabbitmq-demo -n business
```

Kubernetes 部署建议：

1. RabbitMQ 集群建议独立部署，不和业务应用放在同一个 Deployment。
2. 业务应用通过 ConfigMap 和 Secret 注入配置。
3. 使用 readinessProbe 控制发布接流量。
4. 使用 livenessProbe 处理应用异常卡死。
5. 消费者服务扩容前必须评估下游系统容量。
6. 顺序消费队列不应通过 HPA 盲目扩容消费者。
7. 核心消费者应监控消费者数量、消费失败率和死信数量。

### 环境变量配置

环境变量用于将运行环境差异从镜像中剥离出来。镜像应保持环境无关，环境差异通过变量、配置中心或 Kubernetes Secret 注入。

推荐环境变量如下：

| 环境变量                 | 说明              | 示例                     |
| ------------------------ | ----------------- | ------------------------ |
| `SPRING_PROFILES_ACTIVE` | Spring Boot 环境  | `prod`                   |
| `RABBITMQ_HOST`          | RabbitMQ 主机     | `rabbitmq-prod.internal` |
| `RABBITMQ_PORT`          | RabbitMQ 端口     | `5672`                   |
| `RABBITMQ_ADDRESSES`     | RabbitMQ 集群地址 | `host1:5672,host2:5672`  |
| `RABBITMQ_VHOST`         | Virtual Host      | `/prod`                  |
| `RABBITMQ_USERNAME`      | 用户名            | `order_producer`         |
| `RABBITMQ_PASSWORD`      | 密码              | `******`                 |
| `JAVA_OPTS`              | JVM 参数          | `-Xms512m -Xmx512m`      |

Spring Boot 配置引用环境变量。

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  rabbitmq:
    # 集群地址优先，未配置时可使用 host + port
    addresses: ${RABBITMQ_ADDRESSES:}
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    virtual-host: ${RABBITMQ_VHOST:/prod}
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}
    publisher-confirm-type: correlated
    publisher-returns: true
    template:
      mandatory: true
    listener:
      simple:
        acknowledge-mode: manual
        concurrency: ${RABBITMQ_CONCURRENCY:4}
        max-concurrency: ${RABBITMQ_MAX_CONCURRENCY:16}
        prefetch: ${RABBITMQ_PREFETCH:10}
        default-requeue-rejected: false
```

环境变量配置建议：

1. 镜像不内置生产配置。
2. 敏感变量通过 Secret 注入。
3. 不同环境使用不同 vhost、账号和密码。
4. 并发、prefetch 等参数可以环境化，但变更前要评估。
5. 配置变更后观察 Confirm、Return、消费失败和队列堆积指标。
6. 不要在日志中打印完整密码、Token、证书路径中的敏感信息。

### 健康检查配置

健康检查用于让部署平台判断应用是否存活、是否就绪。Spring Boot 3 推荐配合 Actuator 使用 liveness 和 readiness 分组。

Actuator 配置如下。

文件位置：`src/main/resources/application-prod.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        # 只暴露必要端点
        include: health,info,metrics,prometheus
  endpoint:
    health:
      # 开启 Kubernetes 探针分组
      probes:
        enabled: true
      show-details: when_authorized
```

健康检查端点：

| 端点                         | 用途                   |
| ---------------------------- | ---------------------- |
| `/actuator/health/liveness`  | 判断应用进程是否存活   |
| `/actuator/health/readiness` | 判断应用是否可以接流量 |
| `/actuator/health`           | 查看整体健康状态       |

Kubernetes 探针配置：

```yaml
readinessProbe:
  # 应用就绪后才接入 Service
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 3
  failureThreshold: 3

livenessProbe:
  # 应用卡死时重启容器
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 20
  timeoutSeconds: 3
  failureThreshold: 3
```

健康检查建议：

1. Liveness 不建议强依赖 RabbitMQ，否则 RabbitMQ 短暂异常会导致应用反复重启。
2. Readiness 可以根据应用角色决定是否依赖 RabbitMQ。
3. 生产者服务 RabbitMQ 不可用时，可以返回未就绪或进入降级。
4. 消费者服务 RabbitMQ 不可用时，通常应返回未就绪。
5. 自定义健康检查不要执行耗时操作。
6. 健康检查失败要结合日志和指标排查，不要只看状态码。

### 灰度发布

灰度发布用于降低新版本发布风险。RabbitMQ 消费者灰度发布需要特别注意消息重复消费、消费者并发变化、顺序消费、死信数量和消费失败率。

Kubernetes 滚动发布配置：

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    # 发布时最多额外启动 1 个新 Pod
    maxSurge: 1
    # 发布过程中不允许减少可用 Pod
    maxUnavailable: 0
```

灰度发布流程建议：

```text
构建新镜像
    -> 部署 1 个新版本实例
    -> 观察健康检查
    -> 观察 RabbitMQ 连接和消费者数量
    -> 观察消费失败率、死信数量、Return、Confirm
    -> 小流量运行一段时间
    -> 扩大副本数
    -> 完成全量发布
```

发布命令示例：

```bash
# 更新镜像版本
kubectl set image deployment/rabbitmq-demo \
  rabbitmq-demo=registry.example.com/business/rabbitmq-demo:1.1.0 \
  -n business

# 查看发布状态
kubectl rollout status deployment/rabbitmq-demo -n business

# 查看新旧 Pod
kubectl get pod -n business -l app=rabbitmq-demo -o wide
```

灰度发布观察指标：

| 指标            | 说明                     |
| --------------- | ------------------------ |
| Pod Ready 状态  | 新版本是否正常就绪       |
| RabbitMQ 连接数 | 是否异常增长或反复重连   |
| Consumers 数量  | 消费者是否符合预期       |
| Ready / Unacked | 队列是否异常堆积         |
| Consume failed  | 消费失败是否增长         |
| Dead Letter     | 死信是否增长             |
| Confirm failed  | 生产确认是否失败         |
| Return          | 是否出现不可路由消息     |
| 应用错误日志    | 是否出现序列化、业务异常 |

灰度发布建议：

1. 消费者版本兼容消息格式后再发布。
2. 新旧版本同时消费时，必须保证幂等逻辑一致。
3. 如果修改了消息结构，先发布兼容消费者，再发布生产者。
4. 如果修改了队列、Exchange、Binding，先变更资源并验证，再发布应用。
5. 顺序消费服务不适合同时运行多个消费者实例。
6. 灰度期间要重点观察死信和消费失败日志。

### 回滚策略

回滚策略用于新版本发布后出现异常时，快速恢复到上一个稳定版本。RabbitMQ 应用回滚需要考虑消息格式兼容、队列资源变更、已发送消息、未消费消息和死信消息。

Kubernetes 回滚命令：

```bash
# 查看发布历史
kubectl rollout history deployment/rabbitmq-demo -n business

# 回滚到上一版本
kubectl rollout undo deployment/rabbitmq-demo -n business

# 回滚到指定版本
kubectl rollout undo deployment/rabbitmq-demo -n business --to-revision=2

# 查看回滚状态
kubectl rollout status deployment/rabbitmq-demo -n business
```

回滚前检查：

| 检查项           | 说明                              |
| ---------------- | --------------------------------- |
| 是否修改消息格式 | 旧版本是否能消费新消息            |
| 是否修改队列资源 | Exchange、Queue、Binding 是否兼容 |
| 是否新增消费者   | 回滚后是否还有消费者处理新队列    |
| 是否新增字段     | 旧版本是否忽略未知字段            |
| 是否发生死信     | 回滚后是否需要处理死信            |
| 是否有本地消息表 | 发送失败消息是否会被旧版本补偿    |

回滚风险：

| 风险           | 说明                               |
| -------------- | ---------------------------------- |
| 消息格式不兼容 | 新版本发出的消息旧版本无法反序列化 |
| 队列变更不可逆 | 修改队列参数可能无法直接回滚       |
| 消费者重复处理 | 回滚过程中消息重复投递             |
| 死信堆积       | 新版本异常产生大量死信             |
| 补偿任务重复   | 新旧版本补偿逻辑不同               |

回滚建议：

1. 消息格式升级要向后兼容。
2. 消费者应忽略未知字段。
3. 删除字段要分阶段进行，不能生产者和消费者同时硬切。
4. Queue 参数变更不要原地修改，优先新建队列灰度迁移。
5. 回滚后检查死信队列和失败消息表。
6. 回滚后必要时暂停补偿任务，避免错误版本继续重发。
7. 回滚完成后应补充异常消息处理和数据核对。

## 常见问题处理

本章节用于整理 RabbitMQ 开发和运维中常见问题的排查思路。排查问题时应先确认现象，再定位生产者、Broker、路由、队列、消费者和业务逻辑中的具体环节。

推荐排查顺序如下：

```text
确认现象
    -> 查看应用日志
    -> 查看 RabbitMQ 管理控制台
    -> 查看 Exchange / Queue / Binding
    -> 查看 Ready / Unacked / Consumers
    -> 查看 Confirm / Return / Dead Letter
    -> 查看业务消息表和消费记录表
    -> 定位根因并处理
```

### 消息发送失败

消息发送失败通常发生在生产者调用 RabbitTemplate 时，常见原因包括连接失败、认证失败、vhost 错误、Exchange 不存在、消息序列化失败、网络异常等。

常见原因如下：

| 原因            | 表现                        | 处理方式              |
| --------------- | --------------------------- | --------------------- |
| RabbitMQ 未启动 | `Connection refused`        | 启动 RabbitMQ         |
| 地址错误        | 连接超时                    | 检查 host、port、网络 |
| 用户密码错误    | 认证失败                    | 检查账号密码          |
| vhost 不存在    | 连接被拒绝                  | 创建 vhost 并授权     |
| Exchange 不存在 | Channel 异常或 Confirm 失败 | 创建 Exchange         |
| 序列化失败      | JSON 转换异常               | 检查消息对象          |
| Broker 流控     | 发送变慢或阻塞              | 检查内存、磁盘水位    |

排查命令：

```bash
# 检查 RabbitMQ 节点状态
rabbitmqctl status

# 检查 vhost
rabbitmqctl list_vhosts

# 检查用户权限
rabbitmqctl list_permissions -p /prod

# 检查 Exchange 是否存在
rabbitmqctl list_exchanges -p /prod name type durable
```

处理建议：

1. 查看生产者异常日志，确认是连接异常、权限异常还是序列化异常。
2. 检查 RabbitMQ 地址、端口、vhost、账号密码。
3. 检查 Exchange 是否存在。
4. 核心业务发送失败后更新本地消息表状态为 `FAILED`。
5. RabbitMQ 恢复后通过补偿任务重发。

### 消息无法路由

消息无法路由表示消息到达 Exchange 后，没有匹配到任何 Queue。典型表现是 ReturnCallback 被触发，日志出现 `NO_ROUTE`。

常见原因如下：

| 原因                | 说明                               |
| ------------------- | ---------------------------------- |
| Routing Key 写错    | 发送路由键与 Binding 不一致        |
| Binding 缺失        | Exchange 没有绑定目标 Queue        |
| Exchange 类型不匹配 | Direct、Topic、Fanout 使用方式错误 |
| vhost 错误          | 发送到了错误 vhost                 |
| Queue 未创建        | 目标队列不存在                     |
| mandatory 未开启    | 不可路由消息未返回，可能被丢弃     |

排查命令：

```bash
# 查看 Exchange
rabbitmqctl list_exchanges -p /prod name type durable

# 查看 Queue
rabbitmqctl list_queues -p /prod name durable messages consumers

# 查看 Binding
rabbitmqctl list_bindings -p /prod | grep order

# 查看权限
rabbitmqctl list_permissions -p /prod
```

Spring Boot 必须开启 Return：

```yaml
spring:
  rabbitmq:
    publisher-returns: true
    template:
      mandatory: true
```

处理建议：

1. 查看 Return 日志中的 `exchange` 和 `routingKey`。
2. 检查 Exchange 类型。
3. 检查 Binding Key 是否匹配 Routing Key。
4. 检查当前应用连接的 vhost。
5. 修复 Binding 或 Routing Key 后重发消息。
6. 不可路由消息不建议盲目自动重发，应先修复配置。

### 消费者不消费

消费者不消费通常表现为队列 Ready 数量持续增长，Consumers 为 0 或 Ack rate 为 0。

常见原因如下：

| 原因            | 表现                   | 处理方式               |
| --------------- | ---------------------- | ---------------------- |
| 应用未启动      | Consumers 为 0         | 启动消费者应用         |
| 队列名错误      | 监听了错误队列         | 检查 `@RabbitListener` |
| vhost 错误      | 消费者连接到其他 vhost | 检查配置               |
| 权限不足        | 消费者无法读取队列     | 授权 read 权限         |
| Listener 未启用 | 容器未启动             | 检查配置和启动日志     |
| 消费线程卡死    | Unacked 高             | 查看线程栈和业务日志   |
| prefetch 过大   | 消息被少数消费者持有   | 调小 prefetch          |

排查命令：

```bash
# 查看队列消费者数量
rabbitmqctl list_queues -p /prod name messages_ready messages_unacknowledged consumers

# 查看连接
rabbitmqctl list_connections user vhost peer_host state channels

# 查看 Channel
rabbitmqctl list_channels user vhost consumer_count messages_unacknowledged prefetch_count
```

处理建议：

1. 查看队列 `Consumers` 是否大于 0。
2. 检查应用启动日志中是否有 Listener 注册。
3. 检查 `@RabbitListener(queues = "...")` 队列名。
4. 检查消费者账号是否有读权限。
5. 如果 Unacked 高，排查消费者业务是否阻塞。
6. 如果消费者代码异常，修复后重新部署。

### 消息重复消费

消息重复消费是 RabbitMQ 中正常可能发生的情况。只要消息未被成功 Ack，Broker 就可能重新投递。重复消费不能完全避免，必须通过幂等控制解决。

常见原因如下：

| 原因                | 说明                    |
| ------------------- | ----------------------- |
| 消费成功但 Ack 失败 | 网络异常或 Channel 关闭 |
| 消费者宕机          | 未 Ack 消息重新投递     |
| Nack 重新入队       | `requeue=true`          |
| Spring Retry 重试   | 消费异常后重试          |
| 死信重发            | 人工或任务重发死信      |
| 生产者重复发送      | Confirm 不确定后重发    |
| 补偿任务重发        | 失败消息被重新发送      |

处理建议：

1. 消费者必须基于 `eventType + businessKey` 做幂等。
2. 核心业务使用数据库唯一索引或状态机兜底。
3. 重复消息命中幂等后直接 Ack。
4. 不要使用 `messageId` 作为唯一业务幂等依据，因为补偿重发可能生成新 messageId。
5. 查询应用日志中的 `redelivered=true` 判断是否为重复投递。
6. 查看消费记录表是否已经存在成功记录。

幂等处理示例：

```java
if (!messageIdempotentService.tryStartConsume(eventType, businessKey)) {
    log.warn("消息重复消费，直接Ack，eventType={}，businessKey={}", eventType, businessKey);
    channel.basicAck(deliveryTag, false);
    return;
}
```

### 消息丢失

消息丢失通常不是单一原因造成，需要分阶段排查。RabbitMQ 链路包括生产者、Exchange、Queue、消费者和业务处理，每个环节都可能因为配置不当导致消息丢失。

常见原因如下：

| 阶段   | 原因                    | 处理方式                           |
| ------ | ----------------------- | ---------------------------------- |
| 生产者 | 发送失败未补偿          | 本地消息表                         |
| 生产者 | 未开启 Confirm          | 开启 Publisher Confirm             |
| 路由   | 不可路由且未开启 Return | 开启 Publisher Return 和 mandatory |
| Broker | Queue 非持久化          | 队列 durable                       |
| Broker | Message 非持久化        | deliveryMode=PERSISTENT            |
| 消费者 | 自动 Ack 后业务失败     | 使用手动 Ack                       |
| 消费者 | Nack 且无死信           | 配置死信队列                       |
| 业务   | 消费异常被吞掉          | 统一异常处理                       |

可靠性配置检查：

```yaml
spring:
  rabbitmq:
    publisher-confirm-type: correlated
    publisher-returns: true
    template:
      mandatory: true
    listener:
      simple:
        acknowledge-mode: manual
        default-requeue-rejected: false
```

处理建议：

1. 先根据 `messageId` 查询消息记录表。
2. 查看发送日志是否成功。
3. 查看 Confirm 是否成功。
4. 查看 Return 是否发生。
5. 查看队列是否收到消息。
6. 查看消费日志是否消费成功。
7. 查看死信队列是否存在消息。
8. 核心业务使用本地消息表和补偿任务兜底。

### 消息堆积

消息堆积表示生产速度长期大于消费速度，或者消费者异常导致队列 Ready 持续增长。

常见原因如下：

| 原因            | 表现                          |
| --------------- | ----------------------------- |
| 消费者未启动    | Consumers 为 0                |
| 消费能力不足    | Ready 持续增长                |
| 消费逻辑慢      | Unacked 高，Ack rate 低       |
| 下游服务慢      | 消费耗时升高                  |
| 失败重试过多    | Redeliver 或 Dead Letter 增长 |
| prefetch 不合理 | 消费不均或 Unacked 高         |
| 单队列瓶颈      | 单队列吞吐达到上限            |

处理步骤：

```text
查看 Ready / Unacked / Consumers
    -> 查看消费者日志
    -> 判断是否有大量异常
    -> 判断下游是否变慢
    -> 临时扩容消费者
    -> 调整 concurrency / prefetch
    -> 拆分队列或批量消费
```

排查命令：

```bash
rabbitmqctl list_queues -p /prod name messages_ready messages_unacknowledged consumers

rabbitmqctl list_channels user vhost consumer_count messages_unacknowledged prefetch_count
```

处理建议：

1. Consumers 为 0 时先恢复消费者。
2. 消费异常率高时先修复异常，不要盲目扩容。
3. 下游服务慢时控制并发，避免压垮下游。
4. 可并发业务临时扩容消费者实例。
5. 顺序消费不能盲目扩容同一队列消费者。
6. 长期堆积应考虑队列拆分、批量消费和削峰设计。

### 死信队列无消息

死信队列无消息通常不一定代表没有异常，也可能是死信配置没有生效，或者消息被直接丢弃。

常见原因如下：

| 原因                               | 说明                          |
| ---------------------------------- | ----------------------------- |
| 业务队列未配置死信交换机           | `x-dead-letter-exchange` 缺失 |
| 死信 Routing Key 配置错误          | 无法路由到死信队列            |
| 死信 Binding 缺失                  | 死信 Exchange 没有绑定 Queue  |
| 消费者使用 Ack                     | 异常消息被确认删除            |
| Nack 使用 `requeue=true`           | 消息重新入队，不进入死信      |
| Nack 使用 `requeue=false` 但无 DLX | 消息被丢弃                    |
| 自动 Ack 模式                      | 消息可能已被自动确认          |
| TTL 未到期                         | 过期消息还未进入死信          |

排查命令：

```bash
# 查看业务队列参数
rabbitmqctl list_queues -p /prod name arguments | grep order.paid.queue

# 查看死信 Exchange
rabbitmqctl list_exchanges -p /prod name type durable | grep dead

# 查看死信 Binding
rabbitmqctl list_bindings -p /prod | grep dead

# 查看死信队列
rabbitmqctl list_queues -p /prod name messages_ready consumers | grep dead
```

处理建议：

1. 确认业务队列配置了 `x-dead-letter-exchange`。
2. 确认死信 Exchange、Queue、Binding 存在。
3. 确认消费者失败时使用 `basicNack(deliveryTag, false, false)`。
4. 不要使用 `requeue=true` 期待消息进入死信。
5. 检查监听容器是否为自动 Ack。
6. 发送测试异常消息验证死信链路。

### 延迟消息不生效

延迟消息不生效通常发生在 TTL + 死信方案配置错误，或延迟插件未安装、Exchange 类型错误、Header 缺失等场景。

TTL + 死信方案常见问题：

| 原因                     | 说明                          |
| ------------------------ | ----------------------------- |
| 延迟队列未设置 TTL       | 消息不会过期                  |
| 延迟队列未配置死信交换机 | 过期后无法进入业务队列        |
| 死信 Routing Key 错误    | 过期消息无法路由              |
| 消费者监听了延迟队列     | 延迟消息被提前消费            |
| 消息 TTL 队头阻塞        | 不同 TTL 消息可能不按预期释放 |
| TTL 单位错误             | RabbitMQ TTL 单位是毫秒       |

延迟插件方案常见问题：

| 原因                  | 说明                       |
| --------------------- | -------------------------- |
| 插件未安装            | 不支持 `x-delayed-message` |
| Exchange 类型错误     | 未声明为延迟 Exchange      |
| 缺少 `x-delay`        | 消息没有延迟               |
| `x-delayed-type` 错误 | 底层路由类型不匹配         |
| 发送到普通 Exchange   | Header 不生效              |

排查命令：

```bash
# 查看插件
rabbitmq-plugins list | grep delayed

# 查看 Exchange 类型
rabbitmqctl list_exchanges -p /prod name type arguments | grep delayed

# 查看延迟队列参数
rabbitmqctl list_queues -p /prod name arguments | grep delay

# 查看绑定关系
rabbitmqctl list_bindings -p /prod | grep delay
```

处理建议：

1. TTL + 死信方案中，消费者应监听最终业务队列，不监听延迟队列。
2. TTL 单位使用毫秒。
3. 队列 TTL 适合固定延迟。
4. 不同延迟时间建议使用多个延迟队列或延迟插件。
5. 插件方案先确认 `rabbitmq_delayed_message_exchange` 已启用。
6. 延迟消息不是精确定时器，允许存在一定误差。
7. 订单超时关闭类业务必须在消费时再次检查订单状态。

### 序列化失败

序列化失败通常发生在生产者发送对象或消费者接收对象时。常见原因包括 JSON 格式错误、字段类型不匹配、`LocalDateTime` 未配置、DTO 版本不兼容、消息体和监听方法参数类型不匹配等。

常见原因如下：

| 原因                | 说明                          |
| ------------------- | ----------------------------- |
| 消息不是合法 JSON   | 消费端无法解析                |
| 字段类型不匹配      | JSON 字符串对应 Java 数字字段 |
| 时间格式不匹配      | `LocalDateTime` 解析失败      |
| 缺少默认构造        | 某些 DTO 反序列化失败         |
| 未忽略未知字段      | 新字段导致旧消费者失败        |
| 监听方法参数错误    | 消息体类型和方法参数不匹配    |
| Content-Type 不正确 | 消息转换器无法识别            |

推荐消息转换器配置：

```java
package io.github.atengk.rabbitmq.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ JSON 消息转换器配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class RabbitMqJsonConverterConfig {

    /**
     * 配置 JSON 消息转换器
     *
     * @return 消息转换器
     */
    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
```

处理建议：

1. 统一使用 JSON 消息格式。
2. 生产者和消费者使用稳定 DTO，不直接使用 Entity。
3. 消费者忽略未知字段，提升兼容性。
4. 时间字段统一格式。
5. 消息版本升级要兼容旧消费者。
6. 序列化失败通常不应无限重试，应进入死信或异常记录表。
7. 手动发送测试消息时，确保 JSON 字段类型和 DTO 一致。

### 连接 RabbitMQ 失败

连接 RabbitMQ 失败是部署和启动阶段最常见的问题之一。常见表现包括应用启动失败、消费者无法注册、RabbitTemplate 发送异常、健康检查 DOWN。

常见原因如下：

| 原因            | 表现                    | 处理方式                          |
| --------------- | ----------------------- | --------------------------------- |
| Host 错误       | DNS 解析失败或连接超时  | 检查地址                          |
| Port 错误       | Connection refused      | 检查端口                          |
| 用户名密码错误  | Authentication failure  | 检查账号密码                      |
| vhost 不存在    | Access refused          | 创建 vhost                        |
| 权限不足        | Operation not permitted | 设置权限                          |
| TLS 配置错误    | SSL handshake failed    | 检查证书                          |
| 网络策略阻断    | 连接超时                | 检查安全组、防火墙、NetworkPolicy |
| RabbitMQ 未启动 | Connection refused      | 启动服务                          |

排查命令：

```bash
# 检查端口连通性
nc -vz rabbitmq-prod.internal 5672

# 查看 RabbitMQ 状态
rabbitmqctl status

# 查看 vhost
rabbitmqctl list_vhosts

# 查看用户
rabbitmqctl list_users

# 查看用户权限
rabbitmqctl list_permissions -p /prod
```

Spring Boot 配置检查：

```yaml
spring:
  rabbitmq:
    host: rabbitmq-prod.internal
    port: 5672
    virtual-host: /prod
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}
    connection-timeout: 10s
    requested-heartbeat: 30s
```

处理建议：

1. 先确认网络连通性。
2. 再确认账号密码。
3. 再确认 vhost 是否存在。
4. 再确认用户是否有该 vhost 权限。
5. TLS 连接失败时检查信任库、证书链和证书有效期。
6. Kubernetes 中检查 Service、DNS、NetworkPolicy 和 Secret。
7. 如果只有消费者失败，检查 Queue 读权限；如果只有生产者失败，检查 Exchange 写权限。

连接问题最终排查顺序：

```text
网络是否通
    -> RabbitMQ 是否启动
    -> 端口是否正确
    -> 账号密码是否正确
    -> vhost 是否存在
    -> 权限是否正确
    -> TLS 是否正确
    -> 应用配置是否加载正确
```

## 项目编码规范

本章节用于统一 RabbitMQ 项目的编码风格，避免不同开发人员在包结构、配置类、常量、DTO、监听器、Exchange、Queue、Routing Key 和日志输出上出现不一致。MQ 项目一旦命名和结构混乱，后续排查消息链路、处理死信、定位异常和扩展业务都会变得困难。

### 包结构规范

包结构应按照功能职责分层，避免将生产者、消费者、配置类、DTO、常量和工具类混放在同一个包下。推荐使用 `io.github.atengk.rabbitmq` 作为 RabbitMQ 模块基础包名。

推荐包结构如下：

```text
src/main/java/io/github/atengk/rabbitmq
├── config              # RabbitMQ 配置类
├── constant            # MQ 常量定义
├── controller          # MQ 管理接口或测试接口
├── dto                 # 消息请求对象、消息体 DTO
├── vo                  # 管理端展示对象
├── producer            # 消息生产者
├── consumer            # 消息消费者
├── listener            # Listener 相关扩展组件
├── service             # MQ 业务服务接口
├── service/impl        # MQ 业务服务实现
├── exception           # MQ 自定义异常
├── job                 # 补偿任务、定时任务
├── monitor             # 指标、监控、健康检查
├── util                # MQ 工具类
└── properties          # 配置属性类
```

包职责说明：

| 包名       | 职责                                                         |
| ---------- | ------------------------------------------------------------ |
| `config`   | Exchange、Queue、Binding、MessageConverter、ListenerContainer 配置 |
| `constant` | Exchange、Queue、Routing Key、Header、状态常量               |
| `dto`      | 消息体、发送请求、重发请求、死信处理请求                     |
| `producer` | 统一消息发送、业务消息发送                                   |
| `consumer` | 业务消息消费、死信消费、延迟消息消费                         |
| `service`  | 消息记录、幂等、重试、死信处理、补偿服务                     |
| `job`      | 本地消息表补偿、死信补偿、延迟补偿任务                       |
| `monitor`  | Micrometer 指标、健康检查、队列监控                          |
| `util`     | 消息 ID、Redis Key、分片、脱敏工具                           |

规范建议：

1. 配置类不要写业务逻辑。
2. 生产者只负责发送消息，不直接处理消费逻辑。
3. 消费者只负责消费入口，复杂业务应下沉到 Service。
4. DTO 不要复用数据库 Entity。
5. 常量集中管理，避免字符串散落在代码中。
6. 测试接口和生产接口分开，生产环境可禁用测试接口。

### 配置类规范

配置类用于声明 RabbitMQ 基础组件，例如 Exchange、Queue、Binding、消息转换器、监听容器、Confirm、Return 等。配置类命名应以业务域或功能域开头，以 `Config` 结尾。

推荐命名：

```text
OrderRabbitMqConfig
PaymentRabbitMqConfig
StockRabbitMqConfig
DeadLetterQueueConfig
DelayQueueConfig
RabbitMqMessageConverterConfig
RabbitMqListenerContainerConfig
RabbitMqConfirmReturnConfig
```

配置类示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/config/OrderRabbitMqConfig.java`

```java
package io.github.atengk.rabbitmq.config;

import io.github.atengk.rabbitmq.constant.OrderMqConstant;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 订单 RabbitMQ 配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class OrderRabbitMqConfig {

    /**
     * 声明订单业务交换机
     *
     * @return Direct Exchange
     */
    @Bean
    public DirectExchange orderBusinessExchange() {
        return ExchangeBuilder
                .directExchange(OrderMqConstant.ORDER_BUSINESS_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 声明订单支付队列
     *
     * @return Queue
     */
    @Bean
    public Queue orderPaidQueue() {
        return QueueBuilder
                .durable(OrderMqConstant.ORDER_PAID_QUEUE)
                .deadLetterExchange(OrderMqConstant.ORDER_DEAD_EXCHANGE)
                .deadLetterRoutingKey(OrderMqConstant.ORDER_PAID_DEAD_ROUTING_KEY)
                .build();
    }

    /**
     * 绑定订单支付队列
     *
     * @return Binding
     */
    @Bean
    public Binding orderPaidBinding() {
        return BindingBuilder
                .bind(orderPaidQueue())
                .to(orderBusinessExchange())
                .with(OrderMqConstant.ORDER_PAID_ROUTING_KEY);
    }
}
```

配置类规范：

| 规范项    | 说明                                                         |
| --------- | ------------------------------------------------------------ |
| 类名      | 以业务域或功能域命名，例如 `OrderRabbitMqConfig`             |
| Bean 名称 | 方法名表达具体资源，例如 `orderPaidQueue`                    |
| 常量引用  | Exchange、Queue、Routing Key 必须引用常量                    |
| 注释      | 类注释必须说明配置用途                                       |
| 资源声明  | 生产环境核心资源建议通过脚本或 IaC 管理，代码声明用于开发和测试 |
| 参数变更  | Queue 参数变更不能随意修改，避免和线上已有队列冲突           |

注意事项：

1. 不同业务域配置类拆开，避免一个配置类过大。
2. 死信、延迟、批量、普通业务队列可以分配置类管理。
3. 队列参数上线后不要随意修改。
4. 配置类中不要写发送、消费、补偿等业务逻辑。
5. 使用常量类统一管理资源名称。

### 常量定义规范

RabbitMQ 常量应集中定义，避免在代码中直接写字符串。常量包括 Exchange、Queue、Routing Key、Header、消息状态、消费状态、死信状态、重试来源等。

推荐按业务域拆分常量类：

```text
OrderMqConstant
PaymentMqConstant
StockMqConstant
NoticeMqConstant
MqHeaderConstant
MqStatusConstant
```

订单 MQ 常量示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/constant/OrderMqConstant.java`

```java
package io.github.atengk.rabbitmq.constant;

/**
 * 订单 MQ 常量
 *
 * @author Ateng
 * @since 2026-05-11
 */
public final class OrderMqConstant {

    private OrderMqConstant() {
    }

    /**
     * 订单业务交换机
     */
    public static final String ORDER_BUSINESS_EXCHANGE = "business.order.exchange";

    /**
     * 订单死信交换机
     */
    public static final String ORDER_DEAD_EXCHANGE = "dead.order.exchange";

    /**
     * 订单支付队列
     */
    public static final String ORDER_PAID_QUEUE = "order.paid.queue";

    /**
     * 订单支付死信队列
     */
    public static final String ORDER_PAID_DEAD_QUEUE = "order.paid.dead.queue";

    /**
     * 订单支付路由键
     */
    public static final String ORDER_PAID_ROUTING_KEY = "order.paid";

    /**
     * 订单支付死信路由键
     */
    public static final String ORDER_PAID_DEAD_ROUTING_KEY = "order.paid.dead";
}
```

消息 Header 常量示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/constant/MqHeaderConstant.java`

```java
package io.github.atengk.rabbitmq.constant;

/**
 * MQ 消息头常量
 *
 * @author Ateng
 * @since 2026-05-11
 */
public final class MqHeaderConstant {

    private MqHeaderConstant() {
    }

    /**
     * 消息ID
     */
    public static final String MESSAGE_ID = "x-message-id";

    /**
     * 业务类型
     */
    public static final String BUSINESS_TYPE = "x-business-type";

    /**
     * 业务主键
     */
    public static final String BUSINESS_KEY = "x-business-key";

    /**
     * 事件类型
     */
    public static final String EVENT_TYPE = "x-event-type";

    /**
     * 链路追踪ID
     */
    public static final String TRACE_ID = "x-trace-id";

    /**
     * 发送时间
     */
    public static final String SEND_TIME = "x-send-time";
}
```

常量定义规范：

1. 常量类必须 `final`，构造方法私有。
2. 常量名称使用大写下划线。
3. Exchange、Queue、Routing Key 不要散落在注解或业务代码中。
4. Header Key 必须统一，避免消费者取不到字段。
5. 状态值建议统一定义常量或枚举。
6. 常量变更要评估历史消息和管理端查询影响。

### DTO 设计规范

DTO 用于定义消息体结构。MQ 消息 DTO 应稳定、轻量、可版本化，不应直接使用数据库 Entity、Request VO 或第三方接口对象。

DTO 设计原则：

| 原则       | 说明                                         |
| ---------- | -------------------------------------------- |
| 字段最小化 | 只放消费者需要的信息                         |
| 字段稳定   | 不频繁删除或修改字段含义                     |
| 可版本化   | 使用 `version` 字段支持兼容                  |
| 可追踪     | 包含 `messageId`、`eventType`、`businessKey` |
| 可校验     | 必填字段添加校验注解                         |
| 不传敏感   | 不传密码、Token、完整证件号等                |

通用消息 DTO 示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/dto/BaseMqMessage.java`

```java
package io.github.atengk.rabbitmq.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 基础 MQ 消息对象
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
public class BaseMqMessage<T> {

    /**
     * 消息ID
     */
    @NotBlank(message = "消息ID不能为空")
    private String messageId;

    /**
     * 事件类型
     */
    @NotBlank(message = "事件类型不能为空")
    private String eventType;

    /**
     * 业务主键
     */
    @NotBlank(message = "业务主键不能为空")
    private String businessKey;

    /**
     * 消息版本
     */
    @NotBlank(message = "消息版本不能为空")
    private String version;

    /**
     * 消息创建时间
     */
    @NotNull(message = "消息创建时间不能为空")
    private LocalDateTime timestamp;

    /**
     * 业务数据
     */
    @Valid
    @NotNull(message = "业务数据不能为空")
    private T data;
}
```

订单支付业务数据 DTO 示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/dto/OrderPaidMessageData.java`

```java
package io.github.atengk.rabbitmq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单支付消息业务数据
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
public class OrderPaidMessageData {

    /**
     * 订单ID
     */
    @NotBlank(message = "订单ID不能为空")
    private String orderId;

    /**
     * 支付单号
     */
    @NotBlank(message = "支付单号不能为空")
    private String payNo;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 支付金额
     */
    @NotNull(message = "支付金额不能为空")
    private BigDecimal payAmount;

    /**
     * 支付时间
     */
    @NotNull(message = "支付时间不能为空")
    private LocalDateTime payTime;
}
```

DTO 规范建议：

1. DTO 字段使用包装类型，避免默认值误判。
2. DTO 不直接引用 Entity。
3. DTO 中不放复杂对象图。
4. 必填字段添加 Bean Validation 注解。
5. 时间字段统一使用 `LocalDateTime`。
6. 金额字段使用 `BigDecimal`。
7. 新增字段要保证旧消费者能忽略未知字段。
8. 删除字段要分阶段完成，不直接硬删除。

### Listener 命名规范

Listener 命名应体现业务域、事件和消费用途。不要使用 `RabbitConsumer`、`MessageListener` 这类过于泛化的命名。

推荐命名格式：

```text
{业务域}{事件}Consumer
{业务域}{事件}Listener
{业务域}{场景}DeadLetterConsumer
{业务域}{场景}DelayConsumer
```

示例：

| 类名                          | 说明                 |
| ----------------------------- | -------------------- |
| `OrderPaidConsumer`           | 消费订单支付消息     |
| `OrderClosedConsumer`         | 消费订单关闭消息     |
| `PaymentSuccessConsumer`      | 消费支付成功消息     |
| `StockDeductConsumer`         | 消费库存扣减消息     |
| `NoticeSmsSendConsumer`       | 消费短信发送消息     |
| `OrderPaidDeadLetterConsumer` | 消费订单支付死信消息 |
| `OrderCloseDelayConsumer`     | 消费订单延迟关闭消息 |

Listener 示例。

文件位置：`src/main/java/io/github/atengk/rabbitmq/consumer/OrderPaidConsumer.java`

```java
package io.github.atengk.rabbitmq.consumer;

import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.Channel;
import io.github.atengk.rabbitmq.constant.OrderMqConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 订单支付消息消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class OrderPaidConsumer {

    /**
     * 消费订单支付消息
     *
     * @param payload 消息体
     * @param message 原始消息
     * @param channel RabbitMQ Channel
     * @throws IOException Ack 或 Nack 失败时抛出
     */
    @RabbitListener(queues = OrderMqConstant.ORDER_PAID_QUEUE)
    public void consume(String payload, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = StrUtil.blankToDefault(message.getMessageProperties().getMessageId(), "unknown");

        try {
            log.info("收到订单支付消息，messageId={}，payload={}", messageId, payload);

            handleOrderPaid(payload);

            channel.basicAck(deliveryTag, false);
            log.info("订单支付消息消费成功，messageId={}", messageId);
        } catch (Exception ex) {
            log.error("订单支付消息消费失败，messageId={}，原因={}", messageId, ex.getMessage(), ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 处理订单支付业务
     *
     * @param payload 消息体
     */
    private void handleOrderPaid(String payload) {
        log.info("处理订单支付业务，payload={}", payload);
    }
}
```

Listener 命名规范：

1. 类名表达业务含义，不使用泛化名称。
2. 消费方法统一命名为 `consume` 或 `consumeXxx`。
3. 监听队列使用常量，不直接写字符串。
4. 一个 Listener 不建议监听过多无关队列。
5. 死信、延迟、普通业务消费者分开命名。
6. Listener 只做入口控制，复杂业务交给 Service。

### Exchange 命名规范

Exchange 命名应体现业务域、用途和类型。命名统一后，管理控制台、日志和告警中可以快速识别消息来源。

推荐格式：

```text
{用途}.{业务域}.exchange
```

常见用途前缀：

| 前缀             | 说明           |
| ---------------- | -------------- |
| `business`       | 普通业务交换机 |
| `dead`           | 死信交换机     |
| `delay`          | 延迟交换机     |
| `plugin.delayed` | 插件延迟交换机 |
| `broadcast`      | 广播交换机     |
| `topic`          | 主题交换机     |

示例：

| Exchange                        | 说明               |
| ------------------------------- | ------------------ |
| `business.order.exchange`       | 订单业务交换机     |
| `business.payment.exchange`     | 支付业务交换机     |
| `dead.order.exchange`           | 订单死信交换机     |
| `delay.order.exchange`          | 订单延迟交换机     |
| `plugin.delayed.order.exchange` | 订单插件延迟交换机 |
| `business.notice.exchange`      | 通知业务交换机     |

命名建议：

1. Exchange 名称全部小写。
2. 单词之间使用点号分隔。
3. 名称中包含业务域。
4. 死信 Exchange 使用 `dead` 前缀。
5. 延迟 Exchange 使用 `delay` 或 `plugin.delayed` 前缀。
6. 不建议在名称中包含环境名，环境隔离交给 vhost。

### Queue 命名规范

Queue 命名应体现业务域、事件和用途。队列是消费者直接监听的资源，命名必须清晰，方便排查消息堆积和死信问题。

推荐格式：

```text
{业务域}.{事件}.{用途}.queue
```

常见用途：

| 用途          | 说明         |
| ------------- | ------------ |
| `queue`       | 普通业务队列 |
| `dead.queue`  | 死信队列     |
| `delay.queue` | 延迟队列     |
| `retry.queue` | 重试队列     |
| `batch.queue` | 批量消费队列 |

示例：

| Queue                       | 说明             |
| --------------------------- | ---------------- |
| `order.paid.queue`          | 订单支付队列     |
| `order.closed.queue`        | 订单关闭队列     |
| `order.paid.dead.queue`     | 订单支付死信队列 |
| `order.close.delay.queue`   | 订单关闭延迟队列 |
| `notice.sms.send.queue`     | 短信发送队列     |
| `log.operation.batch.queue` | 操作日志批量队列 |

Queue 命名建议：

1. Queue 名称全部小写。
2. 单词之间使用点号分隔。
3. 死信队列必须包含 `dead`。
4. 延迟队列必须包含 `delay`。
5. 批量队列可以包含 `batch`。
6. 不建议多个业务共用一个含义模糊的队列，例如 `common.queue`。
7. 核心队列不要频繁改名，改名相当于新建队列，需要迁移消息链路。

### Routing Key 命名规范

Routing Key 用于表达事件类型或路由规则。Direct Exchange 中 Routing Key 应明确指向一个业务事件；Topic Exchange 中 Routing Key 可以表达业务域、事件、版本等层级。

推荐格式：

```text
{业务域}.{事件}
{业务域}.{对象}.{动作}
{业务域}.{对象}.{动作}.{版本}
```

示例：

| Routing Key              | 说明         |
| ------------------------ | ------------ |
| `order.created`          | 订单已创建   |
| `order.paid`             | 订单已支付   |
| `order.cancelled`        | 订单已取消   |
| `payment.success`        | 支付成功     |
| `payment.refund.success` | 退款成功     |
| `stock.deduct`           | 库存扣减     |
| `notice.sms.send`        | 发送短信     |
| `log.operation.created`  | 操作日志创建 |

Topic Routing Key 示例：

```text
order.created.v1
order.paid.v1
order.cancelled.v1
payment.refund.success.v1
notice.sms.send.v1
```

Routing Key 设计建议：

1. 使用小写字母和点号。
2. 表达事件语义，不使用随机字符串。
3. 不建议直接拼接高基数业务 ID，例如订单号。
4. 分片场景使用有限分片编号，例如 `order.event.0`、`order.event.1`。
5. Routing Key 应与事件类型保持一致或可映射。
6. 版本升级可以在消息体中加 `version`，不一定必须放 Routing Key。

### 日志输出规范

日志输出是 MQ 排查问题的核心依据。日志必须围绕 `messageId`、`businessKey`、`exchange`、`routingKey`、`queue`、`deliveryTag`、`traceId` 展开。

推荐日志字段：

| 字段           | 说明        |
| -------------- | ----------- |
| `messageId`    | 消息唯一 ID |
| `businessType` | 业务类型    |
| `businessKey`  | 业务主键    |
| `eventType`    | 事件类型    |
| `exchange`     | 交换机      |
| `routingKey`   | 路由键      |
| `queue`        | 队列        |
| `deliveryTag`  | 投递标签    |
| `traceId`      | 链路追踪 ID |
| `costMs`       | 消费耗时    |
| `errorType`    | 异常类型    |
| `errorMessage` | 异常原因    |

发送日志示例：

```java
log.info("MQ消息发送成功，messageId={}，businessType={}，businessKey={}，exchange={}，routingKey={}",
        messageId,
        businessType,
        businessKey,
        exchange,
        routingKey
);
```

消费成功日志示例：

```java
log.info("MQ消息消费成功，messageId={}，businessKey={}，queue={}，deliveryTag={}，costMs={}",
        messageId,
        businessKey,
        queueName,
        deliveryTag,
        costMs
);
```

消费失败日志示例：

```java
log.error("MQ消息消费失败，messageId={}，businessKey={}，queue={}，deliveryTag={}，errorType={}，原因={}",
        messageId,
        businessKey,
        queueName,
        deliveryTag,
        errorType,
        ex.getMessage(),
        ex
);
```

日志规范建议：

1. 成功发送、成功消费使用 `INFO`。
2. 高频日志、非核心消息成功日志可降为 `DEBUG`。
3. Confirm 失败、Return、死信、消费异常使用 `ERROR`。
4. 重复消费命中幂等使用 `WARN` 或 `INFO`。
5. 不打印完整敏感消息体。
6. 大消息体只打印摘要或业务主键。
7. 所有异常日志必须打印异常堆栈。
8. 所有关键链路日志必须包含 `messageId`。

## 项目交付内容

本章节用于明确 RabbitMQ 开发项目最终需要交付的内容。交付物不应只包含源码，还应包含配置文件、数据库脚本、Docker Compose、接口文档、测试用例、部署文档和运维手册，确保项目可以部署、验证、排查和持续维护。

### 源码结构

源码结构应包含完整的生产者、消费者、配置、异常处理、幂等、死信、延迟、补偿、监控和管理接口能力。

推荐交付结构如下：

```text
rabbitmq-demo/
├── pom.xml
├── Dockerfile
├── README.md
├── src/
│   ├── main/
│   │   ├── java/io/github/atengk/rabbitmq/
│   │   │   ├── config/
│   │   │   ├── constant/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── vo/
│   │   │   ├── producer/
│   │   │   ├── consumer/
│   │   │   ├── service/
│   │   │   ├── service/impl/
│   │   │   ├── exception/
│   │   │   ├── job/
│   │   │   ├── monitor/
│   │   │   ├── properties/
│   │   │   └── util/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       ├── application-test.yml
│   │       └── application-prod.yml
│   └── test/
│       └── java/io/github/atengk/rabbitmq/
├── sql/
├── docker/
├── k8s/
├── scripts/
└── docs/
```

源码交付要求：

1. 代码可以正常编译。
2. 关键类有清晰注释。
3. Exchange、Queue、Routing Key 使用常量。
4. 核心消费者具备手动 Ack 和异常处理。
5. 核心消息具备幂等控制。
6. 死信、延迟、重试、补偿代码完整。
7. 测试用例可以执行。

### 配置文件

配置文件应覆盖本地、测试和生产环境。配置中不能包含生产真实密码，敏感配置应通过环境变量、Secret 或配置中心注入。

需要交付的配置文件：

```text
src/main/resources/application.yml
src/main/resources/application-local.yml
src/main/resources/application-test.yml
src/main/resources/application-prod.yml
```

配置内容应包含：

| 配置          | 说明                                |
| ------------- | ----------------------------------- |
| RabbitMQ 地址 | host、port 或 addresses             |
| vhost         | 环境隔离                            |
| 用户名密码    | 通过变量注入                        |
| Confirm       | `publisher-confirm-type=correlated` |
| Return        | `publisher-returns=true`            |
| mandatory     | 不可路由消息返回                    |
| Listener      | Ack、并发、prefetch、重试           |
| Actuator      | 健康检查和 Prometheus               |
| 日志          | 日志级别、日志格式                  |

配置交付要求：

1. 本地配置可直接启动。
2. 测试配置符合测试环境资源。
3. 生产配置不包含明文密码。
4. 配置项有必要注释。
5. 动态配置项有默认值。
6. 配置变更风险在文档中说明。

### 数据库脚本

数据库脚本用于创建消息记录、发送日志、消费日志、死信消息、重试记录、异常记录和管理端操作日志等表。

推荐脚本结构：

```text
sql/
├── 001_create_mq_message.sql
├── 002_create_mq_send_log.sql
├── 003_create_mq_consume_log.sql
├── 004_create_mq_dead_message.sql
├── 005_create_mq_retry_record.sql
├── 006_create_mq_exception_record.sql
└── 007_create_mq_admin_operation_log.sql
```

数据库脚本交付要求：

1. 表名、字段名、索引名统一。
2. 每个字段有 COMMENT。
3. 主键、唯一键、查询索引完整。
4. 消息体和 Header 字段类型足够容纳内容。
5. 补偿扫描字段有联合索引。
6. 脚本可重复执行策略明确，例如使用迁移工具管理。
7. 生产执行前必须经过测试环境验证。

### Docker Compose 文件

Docker Compose 文件用于本地启动 RabbitMQ 和业务应用，方便开发、联调和演示。

推荐交付文件：

```text
docker/
├── docker-compose-rabbitmq.yml
├── docker-compose-app.yml
└── docker-compose-full.yml
```

交付内容应包含：

| 文件                          | 说明                 |
| ----------------------------- | -------------------- |
| `docker-compose-rabbitmq.yml` | 只启动 RabbitMQ      |
| `docker-compose-app.yml`      | 只启动应用           |
| `docker-compose-full.yml`     | 启动 RabbitMQ 和应用 |
| `Dockerfile`                  | 应用镜像构建文件     |
| `.env.example`                | 环境变量示例         |

`.env.example` 示例：

```text
SPRING_PROFILES_ACTIVE=local
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_VHOST=/dev
RABBITMQ_USERNAME=ateng
RABBITMQ_PASSWORD=ateng123
JAVA_OPTS=-Xms512m -Xmx512m -XX:+UseG1GC
```

Docker Compose 交付要求：

1. 本地可一键启动。
2. RabbitMQ 管理控制台可访问。
3. RabbitMQ 数据卷持久化。
4. 应用通过环境变量连接 RabbitMQ。
5. 配置 healthcheck。
6. 不包含生产真实密码。

### 接口文档

接口文档用于说明 MQ 管理端和调试接口，包括消息发送、查询、重发、死信处理、状态查询和统计接口。

推荐接口文档结构：

```text
docs/api/
├── mq-message-api.md
├── mq-dead-letter-api.md
├── mq-retry-api.md
├── mq-status-api.md
└── mq-admin-api.md
```

接口文档应包含：

| 内容     | 说明              |
| -------- | ----------------- |
| 接口地址 | URL               |
| 请求方法 | GET、POST、PUT 等 |
| 请求参数 | Query、Path、Body |
| 响应字段 | 返回对象说明      |
| 请求示例 | curl 或 JSON      |
| 响应示例 | JSON              |
| 错误码   | 常见错误          |
| 权限要求 | 管理端接口权限    |

接口文档示例：

~~~markdown
### 重发 MQ 消息

接口地址：`POST /api/admin/mq/messages/{messageId}/resend`

请求参数：

| 参数 | 位置 | 必填 | 说明 |
| --- | --- | --- | --- |
| messageId | Path | 是 | 消息ID |
| reason | Body | 是 | 重发原因 |

请求示例：

```json
{
  "reason": "修复路由配置后人工重发"
}
~~~

响应示例：

~~~json
{
  "code": 200,
  "message": "消息重发请求已提交",
  "data": true
}
接口文档交付要求：

1. 管理端接口必须标明权限要求。
2. 重发、忽略、标记处理接口必须要求填写原因。
3. 请求示例可以直接复制测试。
4. 响应示例包含成功和失败场景。
5. 错误码说明清晰。

### 测试用例

测试用例用于验证 RabbitMQ 发送、消费、死信、延迟、幂等、重试、序列化和压力能力。

推荐测试目录：

```text id="test-delivery-list"
src/test/java/io/github/atengk/rabbitmq/
├── producer/
├── consumer/
├── deadletter/
├── delay/
├── idempotent/
├── integration/
├── performance/
└── util/
~~~

测试用例清单：

| 测试类型   | 必测内容                           |
| ---------- | ---------------------------------- |
| 单元测试   | Routing Key、Redis Key、消息构造   |
| 集成测试   | Exchange、Queue、Binding、发送接收 |
| 生产者测试 | Header、Confirm、Return            |
| 消费者测试 | Ack、Nack、异常处理                |
| 死信测试   | Nack 后进入死信                    |
| 延迟测试   | TTL 到期后进入业务队列             |
| 幂等测试   | 重复消息只处理一次                 |
| 压力测试   | 发送和消费吞吐量                   |

测试交付要求：

1. 核心测试可在本地执行。
2. Testcontainers 测试可在 CI 环境执行。
3. 异步消费测试使用 Awaitility 等待结果。
4. 测试资源使用 `test.` 前缀。
5. 测试结束后清理队列。
6. 压力测试不默认进入普通单元测试流程，可单独执行。

### 部署文档

部署文档用于指导开发、测试、运维在不同环境部署应用。部署文档应包含镜像构建、环境变量、Docker Compose、Kubernetes、健康检查、灰度发布和回滚策略。

推荐文档结构：

```text
docs/deploy/
├── local-deploy.md
├── test-deploy.md
├── prod-deploy.md
├── docker-deploy.md
├── kubernetes-deploy.md
├── gray-release.md
└── rollback.md
```

部署文档应包含：

| 内容     | 说明                           |
| -------- | ------------------------------ |
| 环境要求 | JDK、Maven、Docker、Kubernetes |
| 构建命令 | Maven 构建、Docker 构建        |
| 配置说明 | 环境变量、Secret、ConfigMap    |
| 启动命令 | 本地、Docker、Kubernetes       |
| 验证方式 | 健康检查、发送测试消息         |
| 监控检查 | 队列、连接、消费者、死信       |
| 灰度流程 | 小流量发布流程                 |
| 回滚流程 | 回滚命令和注意事项             |

部署文档交付要求：

1. 命令可复制执行。
2. 环境变量说明完整。
3. 健康检查路径明确。
4. 生产环境敏感配置不明文展示。
5. 发布后验证步骤明确。
6. 回滚风险和处理步骤明确。

### 运维手册

运维手册用于生产环境问题处理和日常维护，重点包括队列堆积、死信处理、消息重发、连接异常、路由异常、权限异常和监控告警处理。

推荐文档结构：

```text
docs/ops/
├── rabbitmq-monitoring.md
├── queue-backlog-handle.md
├── dead-letter-handle.md
├── message-resend.md
├── connection-issue.md
├── routing-issue.md
├── permission-issue.md
└── emergency-plan.md
```

运维手册应包含：

| 内容     | 说明                                          |
| -------- | --------------------------------------------- |
| 常用命令 | `rabbitmqctl`、`rabbitmqadmin`、`kubectl`     |
| 指标说明 | Ready、Unacked、Consumers、Ack rate           |
| 告警处理 | 告警含义和处理步骤                            |
| 死信处理 | 查询、排查、重发、忽略                        |
| 堆积处理 | 扩容、限流、排查下游                          |
| 权限处理 | 用户、vhost、权限配置                         |
| 应急预案 | Broker 故障、消费者故障、路由故障             |
| 操作风险 | 清空队列、删除队列、删除 vhost 等高危操作说明 |

运维手册交付要求：

1. 运维命令必须标明 vhost。
2. 高危命令必须加风险说明。
3. 生产环境清空队列、删除队列必须走审批。
4. 死信重发必须记录操作人和原因。
5. 堆积处理必须先判断消费者和下游状态。
6. 应急预案要包含回滚和补偿步骤。

## 项目总结

本章节用于总结 RabbitMQ 开发文档中的核心能力、关键设计、风险点和后续优化方向。项目总结不是形式化收尾，而是帮助团队明确本项目已经具备什么能力、哪些设计必须坚持、哪些风险需要持续关注。

### 核心能力总结

本项目围绕 Spring Boot 3 和 RabbitMQ 构建了一套完整的消息开发体系，覆盖基础发送消费、可靠性、死信、延迟、幂等、顺序、堆积处理、事务消息、监控运维、安全、测试和部署。

核心能力如下：

| 能力         | 说明                                                     |
| ------------ | -------------------------------------------------------- |
| 基础消息能力 | 支持普通消息发送、消费、Exchange、Queue、Binding         |
| 可靠投递     | 支持 Publisher Confirm、Publisher Return、本地消息表补偿 |
| 可靠消费     | 支持手动 Ack、Nack、Reject、有限重试                     |
| 死信处理     | 支持死信队列、死信消费、死信入库、死信重发               |
| 延迟消息     | 支持 TTL + 死信和延迟插件方案                            |
| 幂等控制     | 支持 Redis、数据库唯一索引、业务状态机                   |
| 顺序消费     | 支持单队列顺序和分片队列顺序                             |
| 消息堆积处理 | 支持消费能力评估、扩容、批量消费、监控告警               |
| 事务消息     | 支持本地消息表、Outbox Pattern、最终一致性               |
| 监控运维     | 支持 Actuator、Prometheus、Grafana、管理端排查           |
| 安全控制     | 支持用户权限、vhost 隔离、TLS、敏感字段处理              |
| 测试体系     | 支持单元测试、集成测试、Testcontainers、压力测试         |
| 部署能力     | 支持 Docker、Docker Compose、Kubernetes、灰度和回滚      |

最终形成的能力目标是：消息能可靠发送、可靠消费、失败可追踪、异常可补偿、运行可监控、上线可回滚。

### 关键设计总结

RabbitMQ 项目的关键设计集中在可靠性、幂等、补偿、状态机和可观测性。只要这些设计保持完整，系统即使遇到网络抖动、消费者宕机、Broker 故障、重复投递、路由异常，也能通过补偿和排查恢复。

关键设计如下：

| 设计              | 说明                                                  |
| ----------------- | ----------------------------------------------------- |
| Publisher Confirm | 判断消息是否到达 Exchange                             |
| Publisher Return  | 发现不可路由消息                                      |
| 手动 Ack          | 业务成功后再确认消息                                  |
| 死信队列          | 保存消费失败、过期、超长消息                          |
| 本地消息表        | 解决业务事务和消息发送一致性                          |
| Outbox Pattern    | 标准化最终一致性事件发布                              |
| 消费幂等          | 防止重复消息造成重复业务副作用                        |
| 业务状态机        | 防止乱序或重复事件破坏业务状态                        |
| 重试补偿          | 临时异常自动重试，长期异常人工处理                    |
| 监控告警          | 及时发现堆积、死信、Return、Confirm 失败              |
| 链路日志          | 通过 `messageId`、`businessKey`、`traceId` 串联全链路 |

推荐默认组合如下：

| 场景           | 推荐方案                                          |
| -------------- | ------------------------------------------------- |
| 普通异步任务   | RabbitTemplate + Confirm + 手动 Ack               |
| 核心业务事件   | 本地消息表 / Outbox + Confirm + Return + 手动 Ack |
| 消费失败       | 有限重试 + 死信队列                               |
| 重复消费       | Redis 幂等 + 数据库状态机                         |
| 订单支付库存   | 数据库唯一索引 + 状态机 + 死信补偿                |
| 延迟处理       | 固定延迟用 TTL + 死信，动态延迟用插件             |
| 高可用核心队列 | Quorum Queue + 多节点客户端地址                   |
| 运维排查       | 消息表 + 消费表 + 死信表 + 统一日志               |

### 风险点总结

RabbitMQ 项目中的风险主要来自消息丢失、重复消费、消费失败、消息堆积、死信无人处理、配置错误和发布不兼容。多数风险不是 RabbitMQ 单独造成，而是业务代码、配置管理、消费者幂等、部署流程和运维机制不完整造成。

主要风险如下：

| 风险           | 影响                           | 控制措施                         |
| -------------- | ------------------------------ | -------------------------------- |
| 消息发送失败   | 下游收不到事件                 | 本地消息表、Confirm、补偿        |
| 消息不可路由   | 消息丢失或 Return              | 开启 Return、mandatory、路由检查 |
| 消息重复消费   | 重复扣库存、重复发通知         | 幂等、唯一索引、状态机           |
| 消费异常被 Ack | 消息误删除                     | 手动 Ack，异常 Nack              |
| 无限重试       | 队列阻塞、日志刷屏             | 有限重试、死信队列               |
| 死信无人处理   | 异常消息长期堆积               | 死信入库、告警、管理端           |
| 队列堆积       | 业务延迟、Broker 压力          | 扩容、批量、拆队列、限流         |
| 配置变更错误   | 消息不可路由或应用启动失败     | 变更审批、预发验证、回滚         |
| 消息格式不兼容 | 消费者反序列化失败             | 版本字段、忽略未知字段、灰度     |
| 敏感数据泄露   | 日志、死信、消息表暴露敏感信息 | 脱敏、加密、只传 ID              |
| 高可用误解     | 集群不等于消息复制             | Quorum Queue、持久化、Confirm    |
| 顺序性误判     | 并发消费破坏顺序               | 单队列单消费者或分片队列         |

重点风险控制建议：

1. 核心业务消息必须落库。
2. 核心消费者必须幂等。
3. 核心队列必须配置死信。
4. 核心消息必须开启 Confirm 和 Return。
5. 消费异常不能直接吞掉。
6. 生产环境变更 Queue 参数必须谨慎。
7. 死信和堆积必须接入告警。
8. 发布新消息格式必须保持兼容。

### 后续优化方向

后续优化应围绕可靠性增强、运维自动化、可观测性、性能调优、高可用和平台化能力建设展开。RabbitMQ 项目初期可以先完成核心链路，后续逐步建设消息管理平台和自动化治理能力。

推荐优化方向如下：

| 方向           | 内容                                             |
| -------------- | ------------------------------------------------ |
| 消息管理平台   | 查询消息、重发消息、处理死信、查看链路           |
| 自动补偿平台   | 自动扫描失败消息、按策略重试、超过次数告警       |
| 死信治理       | 死信分类、死信原因统计、批量处理、人工审批       |
| 链路追踪       | traceId 串联 HTTP、MQ、DB、下游调用              |
| 指标体系       | 完善发送、消费、失败、重试、死信、耗时指标       |
| 告警治理       | 按业务等级区分告警阈值和通知人                   |
| 压测基线       | 建立不同队列、不同消息大小、不同并发下的性能基线 |
| 队列治理       | 队列命名规范、资源归属、生命周期管理             |
| 配置治理       | RabbitMQ 资源变更脚本化、审批化、可回滚          |
| 高可用演练     | 节点宕机、消费者宕机、网络中断、死信增长演练     |
| 安全增强       | TLS、细粒度权限、敏感消息加密、管理端审计        |
| 长周期任务治理 | 使用任务表和调度平台替代长时间 MQ 延迟           |

短期优化建议：

1. 完成消息记录表、消费记录表、死信表和重试表。
2. 完成 Confirm、Return、手动 Ack、死信队列。
3. 完成核心业务幂等和状态机。
4. 完成管理端消息查询和死信处理。
5. 完成 Prometheus 和 Grafana 监控。

中期优化建议：

1. 建设消息补偿任务。
2. 建设死信处理平台。
3. 引入 Outbox Pattern。
4. 完成灰度发布和回滚规范。
5. 定期执行故障演练。

长期优化建议：

1. 建设统一消息平台。
2. 建设跨系统消息链路追踪。
3. 建设消息 SLA 和容量评估体系。
4. 建设自动化队列治理和配置变更审计。
5. 针对核心业务引入更完善的高可用和灾备方案。

最终结论：RabbitMQ 项目不是简单的“发送消息、消费消息”，而是一套完整的异步通信和最终一致性基础设施。项目落地时应同时关注可靠投递、可靠消费、幂等控制、死信补偿、监控告警、安全隔离和部署运维，才能在生产环境中稳定支撑订单、支付、库存、通知、日志和异步任务等业务场景。