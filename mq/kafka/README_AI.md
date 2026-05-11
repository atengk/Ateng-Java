# Kafka 开发

## 项目概述

本章节用于说明 Kafka 开发文档的整体建设背景、建设目标、适用场景、技术选型和功能边界。通过项目概述，可以先明确 Kafka 在系统中的定位、解决的问题、适合承载的业务能力，以及本项目需要建设和不需要建设的内容范围。

Kafka 在本项目中主要作为分布式消息中间件使用，用于解决系统间异步解耦、流量削峰、事件驱动、数据同步、日志采集和消费状态追踪等问题。后续章节中的 Topic 设计、消息模型设计、Producer 开发、Consumer 开发、Offset 管理、异常处理、重试机制、幂等性设计、监控告警和运维管理，均围绕该定位展开。

### 项目背景

随着业务系统逐步拆分为多个服务，服务之间的调用关系会越来越复杂。如果所有业务流程都依赖同步接口调用，一旦下游服务响应变慢、短暂不可用或处理能力不足，上游服务就容易出现接口超时、线程阻塞、重复重试和级联故障。

在订单、支付、库存、通知、日志、数据同步等业务场景中，很多操作并不一定要求在主流程中同步完成。例如订单创建成功后，可以异步触发积分发放、优惠券核销、消息通知、数据统计和风控分析。这类场景更适合通过消息机制完成解耦处理。

Kafka 具备高吞吐、可持久化、可水平扩展、支持消费组和支持消息回溯等特点，适合承载高并发业务事件流。通过引入 Kafka，可以将业务系统中的部分同步调用改造为异步消息驱动模式，使核心链路更短、更稳定，也便于后续扩展新的消费方。

本项目建设 Kafka 开发能力，不只是完成简单的消息发送和消费，而是要形成统一的工程规范，包括消息模型、Topic 命名、生产者封装、消费者封装、异常重试、死信处理、幂等消费、日志追踪、监控告警和测试验证等内容。

### 建设目标

本项目的建设目标是形成一套面向 Java 和 Spring Boot 项目的 Kafka 开发规范，使业务系统可以稳定、统一、可观测地接入 Kafka。

主要建设目标如下：

| 建设目标         | 说明                                                         |
| ---------------- | ------------------------------------------------------------ |
| 统一接入规范     | 统一 Kafka 依赖、配置文件、Topic 命名、消息结构、Header 设计和日志格式 |
| 提供可靠发送能力 | 支持同步发送、异步发送、指定 Key、指定 Header、发送回调、失败处理和重试配置 |
| 提供稳定消费能力 | 支持单条消费、批量消费、手动提交 Offset、异常重试、死信投递和消费幂等 |
| 降低系统耦合     | 通过消息机制减少服务之间的直接同步依赖，提高系统扩展能力     |
| 提升链路可追踪性 | 通过消息 ID、业务 Key、TraceId、发送日志和消费日志串联完整消息链路 |
| 支持异常治理     | 对消费失败、重复消费、消息堆积、死信消息和重试失败提供标准处理方式 |
| 支持监控告警     | 对 Producer、Consumer、Consumer Lag、失败次数、死信数量和消费耗时进行监控 |
| 支持测试验证     | 提供本地 Kafka 环境、集成测试、异常测试、幂等测试和性能测试方案 |
| 形成交付标准     | 明确开发规范、配置规范、测试验收标准、部署验收标准和运维交接内容 |

建设完成后，业务模块不应直接零散使用 Kafka 原生客户端，而应优先使用项目统一封装的 Producer、Consumer、异常处理、日志追踪和监控组件。

### 适用场景

Kafka 适用于需要异步处理、事件通知、流量缓冲、数据同步和多消费者订阅的业务场景。它更适合处理持续产生的业务事件，而不是替代所有同步接口调用。

典型适用场景如下：

| 适用场景       | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| 异步业务解耦   | 订单创建后异步通知积分、优惠券、消息推送、风控、数据统计等模块 |
| 削峰填谷       | 高峰流量先写入 Kafka，由消费者按照自身处理能力平稳消费       |
| 事件驱动架构   | 通过订单状态变更、支付成功、库存变更等领域事件驱动后续业务处理 |
| 数据同步       | 将业务系统的数据变化同步到搜索服务、缓存、报表系统或外部系统 |
| 日志采集       | 采集用户行为日志、接口访问日志、业务埋点数据和系统运行日志   |
| 消息广播       | 一个业务事件可以被多个 Consumer Group 独立消费，互不影响     |
| 延迟与补偿处理 | 结合 Retry Topic、数据库任务表或 Redis ZSet 实现延迟处理和失败补偿 |
| 消费状态追踪   | 记录消息消费状态、失败原因、重试次数和死信信息，便于排查问题 |
| 多实例水平扩展 | 多个消费者实例通过 Consumer Group 分摊 Partition，提高消费吞吐量 |

不建议优先使用 Kafka 的场景如下：

| 不适用场景           | 原因                                                         |
| -------------------- | ------------------------------------------------------------ |
| 强同步响应场景       | Kafka 是异步消息系统，不适合替代必须立即返回结果的同步接口   |
| 严格全局顺序场景     | Kafka 只能较好保证单 Partition 内顺序，不能天然保证跨 Partition 全局顺序 |
| 极低频简单通知       | 如果业务量很小且无需消息堆积能力，直接接口调用可能更简单     |
| 大文件传输           | Kafka 适合传递事件消息，不适合作为大文件存储或传输系统       |
| 无幂等能力的核心链路 | Kafka 消费存在重复投递可能，核心业务必须先具备幂等保护能力   |

### 技术选型

本项目基于 Java 和 Spring Boot 技术栈建设 Kafka 开发能力。应用侧优先使用 Spring for Apache Kafka，避免直接操作过多底层 Kafka Client API，从而降低开发复杂度并统一工程规范。

推荐技术选型如下：

| 技术类别       | 推荐选型                        | 说明                                                         |
| -------------- | ------------------------------- | ------------------------------------------------------------ |
| JDK            | JDK 17 或更高版本               | 适配 Spring Boot 3 项目基线                                  |
| 应用框架       | Spring Boot 3.x                 | 作为业务服务基础框架，统一配置、自动装配和监控接入           |
| Kafka 接入组件 | Spring for Apache Kafka         | 使用 KafkaTemplate、@KafkaListener、监听容器和错误处理器完成开发 |
| 消息中间件     | Apache Kafka 3.x                | 作为消息存储、发布订阅、消费组和事件流处理基础设施           |
| 序列化方式     | JSON 优先，Avro / Protobuf 可选 | 普通业务消息优先使用 JSON，强 Schema 场景可引入 Avro 或 Protobuf |
| JSON 处理      | Jackson                         | 与 Spring Boot 默认 JSON 体系保持一致                        |
| 工具类         | Hutool                          | 用于字符串、集合、日期、JSON、ID 生成和对象判断等通用处理    |
| 监控指标       | Micrometer + Prometheus         | 采集 Producer、Consumer、Listener、异常和消费延迟指标        |
| 可视化面板     | Grafana                         | 展示消费堆积、失败次数、死信增长和消费耗时等指标             |
| 本地环境       | Docker Compose                  | 快速启动 Kafka、Kafka UI 等本地开发依赖                      |
| 测试方案       | Embedded Kafka / Testcontainers | 用于 Producer、Consumer、异常重试、死信和幂等测试            |
| 数据存储       | MySQL / PostgreSQL              | 存储发送记录、消费记录、幂等记录、死信消息和重试记录         |
| 缓存组件       | Redis / Redisson                | 用于短期去重、分布式锁、消费状态缓存和延迟消息辅助处理       |

版本选择需要遵循以下原则：

1. Kafka 相关依赖版本应由父工程或统一依赖管理模块控制，业务模块不单独指定版本。
2. Spring Boot、Spring for Apache Kafka、Kafka Client 和 Kafka Broker 版本需要保持兼容。
3. 生产 Kafka Broker 版本应由基础设施或中间件团队统一维护，业务系统只负责客户端连接和使用。
4. 本地开发环境应尽量使用与生产兼容的 Kafka 版本，避免开发环境和生产环境行为差异过大。
5. 如果需要使用事务消息、SASL_SSL、ACL、Schema Registry 等能力，应提前完成客户端和服务端兼容性验证。

### 功能边界

本项目重点建设应用侧 Kafka 开发能力，主要关注业务系统如何规范地发送消息、消费消息、处理异常、保证幂等、记录日志、接入监控和完成测试。Kafka Broker 的生产集群部署、磁盘规划、跨机房灾备和底层运维能力通常由基础设施或中间件团队负责。

当前包含的功能范围如下：

| 功能范围              | 说明                                                         |
| --------------------- | ------------------------------------------------------------ |
| Producer 消息发送封装 | 提供同步发送、异步发送、指定 Topic、指定 Key、指定 Header、发送回调和失败处理 |
| Consumer 消息消费封装 | 提供 @KafkaListener 使用规范、单条消费、批量消费、手动提交和异常处理 |
| Topic 设计规范        | 定义 Topic 命名、分区规划、副本规划、保留策略和初始化方式    |
| 消息模型规范          | 定义消息体、Header、业务 Key、消息 ID、时间字段和版本字段    |
| Offset 管理           | 说明自动提交、手动提交、AckMode、重复消费和消息丢失防护      |
| 异常重试与死信        | 提供重试策略、死信 Topic、死信投递和死信消息处理规范         |
| 幂等消费              | 提供数据库唯一约束、Redis 去重、消费状态表和并发幂等控制方案 |
| 日志链路追踪          | 统一 Producer 日志、Consumer 日志、异常日志、TraceId 和消息链路日志 |
| 监控告警              | 覆盖 Consumer Lag、消费失败、死信增长、消息堆积和消费耗时等指标 |
| 本地开发环境          | 提供 Docker Compose、Kafka UI、Topic 初始化和联调方式        |
| 自动化测试            | 覆盖 Producer、Consumer、异常重试、死信、幂等和性能压测      |

当前不包含或仅部分包含的功能范围如下：

| 功能范围                  | 边界说明                                                     |
| ------------------------- | ------------------------------------------------------------ |
| Kafka Broker 生产集群部署 | 不包含 Broker 安装、扩容、磁盘规划和集群调优                 |
| Kafka 服务端源码改造      | 不修改 Kafka Broker 或 Kafka Client 底层源码                 |
| 大数据实时计算平台        | 不重点建设 Kafka Streams、Flink、Spark Streaming 等实时计算能力 |
| 企业级消息治理平台        | 可提供基础管理接口设计，但不建设完整消息治理后台             |
| 跨地域灾备方案            | 仅说明设计注意事项，具体复制、切换和容灾方案由基础设施团队负责 |
| 业务强一致事务            | Kafka 不替代数据库事务，核心状态变更仍以本地事务和补偿机制为主 |

在业务设计中，应明确 Kafka 主要用于异步事件流转和最终一致性处理。对于核心状态变更，应优先保证本地事务正确，再通过可靠消息、幂等消费、重试补偿和状态校验保证后续流程的最终一致性。


## Kafka 基础知识

本章节用于说明 Kafka 开发前必须掌握的基础概念。后续 Producer 开发、Consumer 开发、Offset 管理、异常处理、重试机制、幂等性设计和监控告警，都会依赖这些基础概念。

Kafka 是一个分布式事件流平台，主要用于发布、订阅、存储和处理事件流。Kafka 官方文档将事件描述为业务世界中“某件事情已经发生”的记录，事件通常包含 key、value、timestamp 和可选的 metadata headers。([Apache Kafka](https://kafka.apache.org/intro/?utm_source=chatgpt.com))

### Kafka 核心概念

Kafka 的核心模型可以理解为：Producer 将消息写入 Topic，Topic 被拆分为多个 Partition，Broker 负责存储 Partition，Consumer 从 Partition 中拉取消息，Consumer Group 用于组织多个消费者共同消费消息。

核心概念如下：

| 概念                     | 说明                                                         |
| ------------------------ | ------------------------------------------------------------ |
| Message / Record / Event | Kafka 中的一条消息，也可以称为记录或事件                     |
| Topic                    | 消息的逻辑分类，生产者向 Topic 写入消息，消费者从 Topic 读取消息 |
| Partition                | Topic 的物理分片，是 Kafka 并行写入、并行消费和顺序保证的基本单位 |
| Producer                 | 消息生产者，负责将业务消息发送到指定 Topic                   |
| Consumer                 | 消息消费者，负责订阅 Topic 并处理消息                        |
| Consumer Group           | 消费者组，用于实现组内负载均衡和组间广播消费                 |
| Offset                   | 消息在 Partition 中的位置编号，用于标识消费进度              |
| Broker                   | Kafka 服务节点，负责存储消息、处理读写请求和参与集群协调     |
| Cluster                  | 多个 Broker 组成的 Kafka 集群                                |
| Replica                  | Partition 的副本，用于提升高可用能力                         |
| Leader                   | Partition 的主副本，负责处理该 Partition 的读写请求          |
| Follower                 | Partition 的从副本，负责从 Leader 同步数据                   |
| ISR                      | In-Sync Replicas，与 Leader 保持同步的副本集合               |

在应用开发中，最常接触的是 Topic、Producer、Consumer、Consumer Group 和 Offset。Broker、Replica、ISR 更多属于部署和运维层面的概念，但会直接影响消息可靠性、可用性和故障恢复能力。

### Topic 与 Partition

Topic 是 Kafka 中消息的逻辑分类。业务上通常会按照事件类型或业务动作划分 Topic，例如 `order-created-topic`、`payment-success-topic`、`user-login-log-topic`。Kafka Topic 支持多个生产者写入，也支持多个消费者订阅。官方文档说明，Topic 是多生产者、多订阅者的，Topic 中的事件不会因为被消费而立即删除，而是根据保留策略进行保存。([Apache Kafka](https://kafka.apache.org/intro/?utm_source=chatgpt.com))

Partition 是 Topic 的物理分片。一个 Topic 可以包含一个或多个 Partition，每个 Partition 内部是有序、不可变、持续追加的日志结构。Kafka 通过 Partition 实现数据分布、并行写入、并行消费和水平扩展。

Topic 与 Partition 的关系如下：

| 项目      | 说明                                                       |
| --------- | ---------------------------------------------------------- |
| Topic     | 业务维度的消息分类                                         |
| Partition | Topic 的物理分片                                           |
| 消息写入  | 每条消息最终会写入某一个 Partition                         |
| 消息顺序  | Kafka 只保证同一个 Partition 内的消息顺序                  |
| 并行能力  | Partition 数量决定同一个 Consumer Group 内的最大消费并行度 |
| 扩容影响  | 增加 Partition 可以提升吞吐，但可能影响基于 Key 的顺序性   |

Partition 设计需要重点关注两个问题：吞吐量和顺序性。如果某类业务消息要求同一个业务主体有序，例如同一个订单的状态变更事件，就需要使用相同的业务 Key，使其尽量进入同一个 Partition。如果追求更高吞吐量，可以适当增加 Partition 数量，但不能盲目增加，否则会增加 Broker 元数据、文件句柄、Leader 选举和客户端协调成本。

### Producer 与 Consumer

Producer 是消息生产者，负责将业务系统中的事件发送到 Kafka Topic。Producer 发送消息时可以指定 Topic、Key、Value、Partition 和 Header。Kafka 官方文档说明，Producer 负责决定记录被分配到 Topic 的哪个 Partition，可以通过轮询方式平衡负载，也可以基于消息 Key 使用分区策略。([Apache Kafka](https://kafka.apache.org/20/getting-started/introduction/?utm_source=chatgpt.com))

Consumer 是消息消费者，负责订阅一个或多个 Topic，并从 Topic 的 Partition 中拉取消息进行处理。Kafka 的消费模型不是 Broker 主动推送，而是 Consumer 主动拉取。这样 Consumer 可以根据自身处理能力控制消费速度。

Producer 与 Consumer 的关系如下：

| 角色     | 主要职责                | 项目开发关注点                                               |
| -------- | ----------------------- | ------------------------------------------------------------ |
| Producer | 发送消息到 Kafka        | 发送可靠性、序列化、Key 设计、Header 设置、发送回调、异常处理 |
| Consumer | 从 Kafka 拉取并处理消息 | 消费幂等、Offset 提交、异常重试、死信投递、消费并发、日志追踪 |

在 Spring Boot 项目中，Producer 通常通过 `KafkaTemplate` 发送消息，Consumer 通常通过 `@KafkaListener` 监听消息。Spring Boot 会基于 `spring-kafka` 提供 Kafka 自动配置，Kafka 连接和客户端配置通常通过 `spring.kafka.*` 完成。([Home](https://docs.spring.io/spring-boot/reference/messaging/kafka.html?utm_source=chatgpt.com))

### Consumer Group

Consumer Group 是 Kafka 的消费者分组机制。多个 Consumer 可以使用相同的 `group.id` 组成一个 Consumer Group，共同消费一个或多个 Topic。

Consumer Group 有两个核心特性：

| 特性         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 组内负载均衡 | 同一个 Consumer Group 内，一条消息只会被其中一个 Consumer 实例消费 |
| 组间广播消费 | 不同 Consumer Group 订阅同一个 Topic 时，每个 Group 都可以独立消费完整消息流 |

例如，一个 `order-created-topic` 可以同时被多个 Consumer Group 订阅：

| Consumer Group           | 用途           |
| ------------------------ | -------------- |
| `order-points-group`     | 处理订单积分   |
| `order-coupon-group`     | 处理优惠券核销 |
| `order-statistics-group` | 处理订单统计   |
| `order-risk-group`       | 处理风控分析   |

这几个 Consumer Group 彼此独立，互不影响。每个 Group 都可以消费到 `order-created-topic` 中的消息。而在同一个 Group 内，如果有多个实例，则由这些实例分摊 Partition。

需要注意，单个 Consumer Group 内的并行度受 Partition 数量限制。如果某个 Topic 只有 3 个 Partition，那么同一个 Consumer Group 内最多只有 3 个 Consumer 实例可以同时有效消费该 Topic。超过 Partition 数量的 Consumer 实例会处于空闲状态。

### Offset 管理

Offset 是消息在 Partition 中的位置编号。每个 Partition 内的消息都会按照写入顺序分配递增 Offset。Consumer 通过 Offset 记录自己已经消费到哪个位置，从而在重启、扩容、异常恢复或 Rebalance 后继续消费。

Offset 管理方式主要分为自动提交和手动提交：

| 方式     | 说明                                 | 适用场景                                   |
| -------- | ------------------------------------ | ------------------------------------------ |
| 自动提交 | Consumer 按照配置周期自动提交 Offset | 简单消费、允许少量重复或丢失风险较低的场景 |
| 手动提交 | 业务处理成功后由程序主动提交 Offset  | 重要业务消息、需要控制消费成功边界的场景   |

在可靠消费场景中，推荐使用手动提交 Offset。处理流程通常为：拉取消息，执行业务逻辑，业务处理成功，提交 Offset。如果业务处理失败，则不提交 Offset，并进入重试、死信或补偿流程。

Offset 使用时需要注意以下问题：

| 问题                     | 说明                                                         |
| ------------------------ | ------------------------------------------------------------ |
| 先提交 Offset 后处理业务 | 可能导致消息丢失，因为业务失败后 Kafka 认为消息已消费        |
| 先处理业务后提交 Offset  | 可能导致重复消费，因为业务成功但提交 Offset 失败时会再次消费 |
| 消费重复                 | Kafka 消费端需要默认按照“可能重复”设计，业务侧必须做幂等     |
| Offset Reset             | 当没有已提交 Offset 或 Offset 不存在时，由 `auto.offset.reset` 决定从最早还是最新位置消费 |

因此，Kafka 开发中通常不追求“绝对不重复”，而是通过手动提交、幂等消费、唯一约束、状态表和补偿机制实现业务上的可靠处理。

### Broker 与 Cluster

Broker 是 Kafka 集群中的服务节点。一个 Kafka 集群由一个或多个 Broker 组成，每个 Broker 负责存储一部分 Partition，并对外提供消息读写能力。

Cluster 是多个 Broker 组成的 Kafka 集群。Kafka 通过集群模式实现数据分布、故障转移和水平扩展。Topic 的多个 Partition 会分布在不同 Broker 上，从而让 Producer 和 Consumer 可以并行访问多个 Broker，提高整体吞吐能力。

Broker 与 Cluster 的关系如下：

| 概念               | 说明                                             |
| ------------------ | ------------------------------------------------ |
| Broker             | 单个 Kafka 服务节点                              |
| Cluster            | 多个 Broker 组成的集群                           |
| Controller         | 集群中的控制角色，负责管理元数据、分区 Leader 等 |
| Partition Leader   | 某个 Partition 的主副本，负责处理读写请求        |
| Partition Follower | 某个 Partition 的从副本，负责复制 Leader 数据    |

在开发环境中，可以使用单 Broker Kafka 进行本地测试。在生产环境中，应使用多 Broker 集群，并为重要 Topic 配置合理的副本数，例如副本数为 3。

### Replication 与 ISR

Replication 是 Kafka 的副本机制。每个 Partition 可以配置多个副本，分布在不同 Broker 上。副本机制用于提升消息数据的高可用能力，当某个 Broker 故障时，Kafka 可以从其他副本中选举新的 Leader 继续提供服务。

Kafka 官方介绍中说明，Topic 可以进行副本复制，常见生产配置是 replication factor 为 3，即数据保留 3 份副本，复制是在 Topic-Partition 级别进行的。([Apache Kafka](https://kafka.apache.org/intro/?utm_source=chatgpt.com))

副本相关概念如下：

| 概念               | 说明                                                   |
| ------------------ | ------------------------------------------------------ |
| Replica            | Partition 的副本                                       |
| Leader Replica     | 主副本，处理该 Partition 的读写请求                    |
| Follower Replica   | 从副本，从 Leader 拉取数据进行同步                     |
| ISR                | 与 Leader 保持同步的副本集合                           |
| Replication Factor | 副本因子，表示每个 Partition 有多少份副本              |
| Min ISR            | 最小同步副本数，用于控制写入成功所需的最少同步副本数量 |

ISR 是消息可靠性设计中的关键概念。如果 Producer 配置 `acks=all`，Kafka 需要等待 ISR 中满足条件的副本确认后，才认为消息写入成功。如果 ISR 数量不足，并且 Topic 配置了 `min.insync.replicas`，则写入可能失败，从而避免消息只写入少数副本就被认为成功。

生产环境中常见配置建议如下：

| 配置项                | 建议值 | 说明                                       |
| --------------------- | ------ | ------------------------------------------ |
| `replication.factor`  | `3`    | 每个 Partition 保留 3 份副本               |
| `min.insync.replicas` | `2`    | 至少 2 个同步副本确认                      |
| `acks`                | `all`  | Producer 等待所有要求的同步副本确认        |
| `enable.idempotence`  | `true` | 开启 Producer 幂等，减少重试导致的重复写入 |

### 消息顺序性

Kafka 的顺序性是 Partition 级别的。也就是说，Kafka 可以保证同一个 Partition 内消息按照写入顺序被读取，但不能保证一个 Topic 下多个 Partition 之间的全局顺序。官方文档说明，具有相同事件 Key 的事件会写入同一个 Partition，并且 Kafka 保证消费者按写入顺序读取给定 Topic-Partition 中的事件。([Apache Kafka](https://kafka.apache.org/intro/?utm_source=chatgpt.com))

常见顺序场景如下：

| 场景                   | 设计方式                                      |
| ---------------------- | --------------------------------------------- |
| 同一个订单状态变更有序 | 使用 `orderId` 作为消息 Key                   |
| 同一个用户行为事件有序 | 使用 `userId` 作为消息 Key                    |
| 同一个设备上报数据有序 | 使用 `deviceId` 作为消息 Key                  |
| 全局严格有序           | Topic 只设置 1 个 Partition，但吞吐能力会受限 |

顺序消息设计时需要注意以下问题：

1. 相同业务 Key 的消息应发送到同一个 Topic，并使用相同 Key。
2. 不要在业务中随机指定 Partition，否则会破坏 Key 与 Partition 的映射关系。
3. 如果后期增加 Topic 的 Partition 数量，Key 到 Partition 的映射可能发生变化，需要评估顺序性影响。
4. Consumer 并发消费时，需要避免同一个 Key 的消息被并发处理导致状态乱序。
5. 如果某条顺序消息消费失败，需要谨慎处理后续消息，避免跳过失败消息导致业务状态不一致。

在大多数业务系统中，不建议追求全局顺序，而应优先设计局部顺序。例如按订单、用户、设备、账户等业务维度保证顺序。

### 消息可靠性

Kafka 消息可靠性需要从 Producer、Broker、Consumer 三个层面共同设计。只配置其中一个环节，不能保证完整链路可靠。

Producer 侧主要关注消息是否成功写入 Kafka；Broker 侧主要关注消息是否被可靠复制和持久化；Consumer 侧主要关注消息是否被正确处理并提交 Offset。

可靠性设计如下：

| 层面     | 关键配置或机制            | 说明                                         |
| -------- | ------------------------- | -------------------------------------------- |
| Producer | `acks=all`                | 等待同步副本确认，提高写入可靠性             |
| Producer | `retries`                 | 发送失败时自动重试                           |
| Producer | `enable.idempotence=true` | 开启幂等 Producer，降低重复写入风险          |
| Producer | `delivery.timeout.ms`     | 控制消息发送整体超时时间                     |
| Broker   | `replication.factor=3`    | 提高数据副本可用性                           |
| Broker   | `min.insync.replicas=2`   | 控制最少同步副本数量                         |
| Consumer | 手动提交 Offset           | 业务处理成功后再提交消费进度                 |
| Consumer | 幂等消费                  | 防止重复消费导致重复扣减、重复通知、重复写库 |
| Consumer | 死信 Topic                | 多次失败后转入死信，避免阻塞主消费链路       |
| 业务侧   | 消费状态表                | 记录消息处理状态，支持追踪、补偿和重放       |

可靠性设计需要接受一个基本事实：Kafka 可以提供高可靠的消息存储和传输能力，但业务系统仍然需要处理重复消费、消费失败、重试失败、部分成功和最终一致性问题。因此，项目中必须将消息唯一标识、幂等控制、异常重试、死信处理和补偿机制作为标准能力建设。

## 项目环境准备

本章节用于说明 Kafka 开发所需的基础环境，包括 JDK、Spring Boot、Kafka 版本、Maven 依赖、本地 Kafka 启动方式、Docker Compose 环境和 Kafka UI 管理工具。环境准备完成后，后续章节中的 Producer、Consumer、Topic 管理和测试代码才能正常运行。

### JDK 版本要求

本项目推荐使用 JDK 17 或更高版本。Spring Boot 3.x 从 3.0 开始要求 Java 17，Spring Boot 3.2.x 官方文档也说明至少需要 Java 17，并要求 Spring Framework 6.1.x 或更高版本。([Home](https://docs.spring.io/spring-boot/docs/3.0.0/reference/html/getting-started.html?utm_source=chatgpt.com))

推荐版本如下：

| 环境     | 推荐版本         | 说明                                              |
| -------- | ---------------- | ------------------------------------------------- |
| 本地开发 | JDK 17           | 与 Spring Boot 3 基线保持一致                     |
| 测试环境 | JDK 17           | 保持与开发环境一致                                |
| 生产环境 | JDK 17 或 JDK 21 | 生产环境可使用 LTS 版本，但需统一压测和验证       |
| 构建工具 | Maven 3.6.3+     | Spring Boot 3.2.x 明确支持 Maven 3.6.3 或更高版本 |

本地检查 JDK 版本：

```bash
java -version
javac -version
mvn -version
```

以上命令分别用于检查 Java 运行时版本、Java 编译器版本和 Maven 版本。如果 `java` 与 `javac` 版本不一致，需要检查 `JAVA_HOME` 和 `PATH` 配置。

Linux 或 macOS 可以通过以下命令检查环境变量：

```bash
echo $JAVA_HOME
which java
which javac
```

`JAVA_HOME` 应指向 JDK 安装目录，不应指向 JRE 目录。`which java` 和 `which javac` 应尽量来自同一个 JDK 目录，避免编译和运行环境不一致。

### Spring Boot 3 版本选择

本项目推荐使用 Spring Boot 3.2.x、3.3.x 或 3.5.x 的稳定版本。对于企业项目，优先选择当前团队已统一验证过的 Spring Boot 3 小版本，不建议业务模块自行升级到未统一验证的新版本。

版本选择建议如下：

| 场景         | 推荐选择                         | 说明                                  |
| ------------ | -------------------------------- | ------------------------------------- |
| 新项目       | Spring Boot 3.5.x 或团队统一版本 | 优先使用较新的稳定版本                |
| 存量项目升级 | Spring Boot 3.2.x / 3.3.x        | 优先考虑兼容性和迁移成本              |
| 生产项目     | 团队统一维护版本                 | 避免不同服务 Spring Boot 版本过于分散 |
| 技术预研     | 可单独验证新版本                 | 不应直接影响生产主干项目              |

使用 Spring Boot 3 时需要注意以下变化：

1. Java 基线为 JDK 17。
2. Spring Framework 基线升级到 6.x。
3. Java EE 包名迁移到 Jakarta EE，例如 `javax.*` 迁移为 `jakarta.*`。
4. 老版本依赖如果仍使用 `javax.*`，可能需要升级依赖版本。
5. Kafka 相关依赖应优先交给 Spring Boot 依赖管理，不建议手动指定多个不一致版本。

Spring Boot 对 Kafka 的支持通过 `spring-kafka` 项目提供自动配置，常用配置位于 `spring.kafka.*`，例如 `spring.kafka.bootstrap-servers` 和 `spring.kafka.consumer.group-id`。([Spring Enterprise 文档](https://docs.enterprise.spring.io/spring-boot/reference/messaging/kafka.html?utm_source=chatgpt.com))

### Kafka 版本选择

Kafka 版本选择需要同时考虑 Broker 版本、Kafka Client 版本、Spring Kafka 版本和 Spring Boot 依赖管理。Apache Kafka 4.2.0 已于 2026 年 2 月 17 日发布，官方 downloads 页面也列出了 `apache/kafka:4.2.0` Docker 镜像和 `kafka_2.13-4.2.0.tgz` 二进制包。([Apache Kafka](https://kafka.apache.org/community/downloads/?utm_source=chatgpt.com))

项目版本建议如下：

| 类型         | 推荐版本                      | 说明                                  |
| ------------ | ----------------------------- | ------------------------------------- |
| 生产 Broker  | 以基础设施团队版本为准        | 业务系统不自行决定生产 Kafka 集群版本 |
| 本地 Broker  | 与生产主版本兼容              | 本地可使用 Docker 快速启动            |
| Kafka Client | 默认使用 Spring Boot 管理版本 | 避免手动指定不兼容版本                |
| Spring Kafka | 默认使用 Spring Boot 管理版本 | 除非有明确兼容性需求，否则不单独覆盖  |
| 测试依赖     | `spring-kafka-test`           | 用于 Embedded Kafka 或集成测试        |

如果业务项目使用 Spring Boot 3.4.x 或 3.5.x，但希望覆盖 Kafka Client 或 Spring Kafka 版本，需要通过 Maven 属性显式覆盖。Spring Kafka 官方文档说明，Spring Boot 应用中的 Apache Kafka 依赖版本由 Spring Boot dependency management 决定；如需使用不同的 `kafka-clients`、`kafka-streams` 或 `spring-kafka` 版本，可以设置 `kafka.version` 或 `spring-kafka.version`。([Home](https://docs.spring.io/spring-kafka/reference/appendix/override-boot-dependencies.html?utm_source=chatgpt.com))

一般建议如下：

1. 普通业务项目不要主动覆盖 `kafka.version`。
2. 只有在需要使用特定 Kafka 新特性、修复特定客户端问题或适配特定 Broker 行为时，才覆盖版本。
3. 覆盖版本后必须进行 Producer、Consumer、事务、重试、死信和压测验证。
4. Kafka Broker 升级需要先确认客户端兼容性，再逐步灰度验证。

### Maven 依赖配置

本项目使用 Maven 管理依赖。Spring Boot 项目中推荐使用 `spring-boot-starter-parent` 或统一 BOM 管理依赖版本，Kafka 相关依赖不建议手动写死版本。

文件位置：`pom.xml`

以下配置用于 Spring Boot 3 项目接入 Kafka、JSON、Hutool、Lombok、测试和监控能力。

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot 统一管理依赖版本，Kafka 相关依赖默认跟随 Boot 管理 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.5</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>kafka-develop-demo</artifactId>
    <version>1.0.0</version>
    <name>kafka-develop-demo</name>
    <description>Kafka 开发示例项目</description>

    <properties>
        <!-- Spring Boot 3 推荐使用 JDK 17 或更高版本 -->
        <java.version>17</java.version>
        <!-- Hutool 工具类版本，统一在 properties 中维护 -->
        <hutool.version>5.8.38</hutool.version>
    </properties>

    <dependencies>
        <!-- Web 能力：用于后续提供消息发送测试接口、健康检查接口等 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Kafka 核心依赖：提供 KafkaTemplate、@KafkaListener、监听容器、错误处理器等能力 -->
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>

        <!-- Actuator：用于暴露应用健康状态和监控指标 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Prometheus 指标导出：用于对接 Prometheus 和 Grafana -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>

        <!-- Hutool：用于字符串、集合、日期、JSON、ID 等常用工具处理 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok：减少 DTO、配置类、日志对象等样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot 测试基础依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Kafka 测试依赖：支持 Embedded Kafka、Kafka 测试工具等 -->
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot 打包插件：用于生成可执行 Jar -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

如果项目由公司统一父工程管理，则业务模块只需要声明 `spring-kafka`、`hutool-all`、`spring-kafka-test` 等依赖，不需要重复声明 `spring-boot-starter-parent`。

验证依赖是否正常：

```bash
mvn dependency:tree | grep kafka
mvn clean test
```

`mvn dependency:tree | grep kafka` 用于查看 Kafka 相关依赖版本，确认是否由 Spring Boot 统一管理。`mvn clean test` 用于执行基础构建和测试，验证依赖解析是否正常。

### 本地 Kafka 环境搭建

本地 Kafka 环境用于开发联调、Topic 创建、消息发送、消息消费和异常处理测试。Apache Kafka 官方 Quickstart 中，Kafka 4.2.0 本地环境要求 Java 17+，并可以通过下载包或 Docker 镜像运行。([Apache Kafka](https://kafka.apache.org/quickstart?utm_source=chatgpt.com))

本地开发推荐优先使用 Docker 方式，原因如下：

| 方式           | 优点                              | 缺点                                 |
| -------------- | --------------------------------- | ------------------------------------ |
| 本地安装包     | 接近原生命令，便于学习 Kafka 脚本 | 需要手动配置 JDK、环境变量、日志目录 |
| Docker 单容器  | 启动简单，适合快速验证            | 配置复杂场景不如 Compose 清晰        |
| Docker Compose | 依赖清晰，便于同时启动 Kafka UI   | 需要提前安装 Docker 和 Compose       |

如果使用 Apache Kafka 官方安装包，可以按照以下步骤运行单节点 Kafka。

下载并解压 Kafka：

```bash
tar -xzf kafka_2.13-4.2.0.tgz
cd kafka_2.13-4.2.0
```

生成 KRaft 集群 ID 并格式化日志目录：

```bash
KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
bin/kafka-storage.sh format --standalone -t "$KAFKA_CLUSTER_ID" -c config/server.properties
```

启动 Kafka：

```bash
bin/kafka-server-start.sh config/server.properties
```

新开一个终端，创建测试 Topic：

```bash
bin/kafka-topics.sh \
  --create \
  --topic ateng.kafka.demo.topic \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1
```

查看 Topic：

```bash
bin/kafka-topics.sh \
  --list \
  --bootstrap-server localhost:9092
```

发送测试消息：

```bash
bin/kafka-console-producer.sh \
  --topic ateng.kafka.demo.topic \
  --bootstrap-server localhost:9092
```

消费测试消息：

```bash
bin/kafka-console-consumer.sh \
  --topic ateng.kafka.demo.topic \
  --bootstrap-server localhost:9092 \
  --from-beginning
```

以上命令中，`--bootstrap-server` 指定 Kafka 连接地址，`--topic` 指定 Topic 名称，`--partitions` 指定分区数量，`--replication-factor` 指定副本数。本地单节点环境只能使用副本数 `1`，否则会因为 Broker 数量不足导致创建失败。

### Docker Compose 环境搭建

Docker Compose 方式适合本地开发环境。通过一个配置文件同时启动 Kafka 和 Kafka UI，便于开发人员创建 Topic、查看消息、观察 Consumer Group 和排查消费问题。Apache Kafka 官方 Docker 文档说明，JVM 版 Apache Kafka Docker 镜像可以通过 `apache/kafka:4.2.0` 拉取，并可直接映射 `9092` 端口运行。([Apache Kafka](https://kafka.apache.org/42/getting-started/docker/?utm_source=chatgpt.com))

文件位置：`docker/docker-compose.yml`

以下配置用于启动单节点 Kafka KRaft 环境和 Kafka UI 管理界面。

```yaml
services:
  kafka:
    image: apache/kafka:4.2.0
    container_name: ateng-kafka
    hostname: kafka
    ports:
      # 宿主机访问 Kafka 使用 localhost:9092
      - "9092:9092"
    environment:
      # 当前节点 ID，单节点环境固定为 1
      KAFKA_NODE_ID: 1

      # KRaft 模式下同时作为 broker 和 controller
      KAFKA_PROCESS_ROLES: broker,controller

      # Kafka 监听地址：PLAINTEXT 用于客户端访问，CONTROLLER 用于内部控制器通信
      KAFKA_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093

      # 对外暴露地址：宿主机 Spring Boot 项目连接 localhost:9092
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092

      # Controller 通信监听器名称
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER

      # 监听器安全协议映射，本地开发使用 PLAINTEXT
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT

      # 单节点 Controller 投票配置
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093

      # 本地单节点环境内部主题副本数只能设置为 1
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1

      # 本地开发可开启自动创建 Topic，生产环境不建议开启
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"

      # Kafka 日志目录
      KAFKA_LOG_DIRS: /tmp/kraft-combined-logs

      # 单节点 KRaft 集群 ID，本地固定即可
      CLUSTER_ID: MkU3OEVBNTcwNTJENDM2Qk
    volumes:
      # 持久化 Kafka 数据，避免容器重启后 Topic 和消息丢失
      - kafka-data:/tmp/kraft-combined-logs

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: ateng-kafka-ui
    depends_on:
      - kafka
    ports:
      # 浏览器访问 http://localhost:8080
      - "8080:8080"
    environment:
      # UI 中展示的集群名称
      KAFKA_CLUSTERS_0_NAME: local-kafka

      # Kafka UI 在容器网络内访问 Kafka，需要使用 kafka:9092
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092

      # 允许在 UI 中动态调整配置，本地开发使用
      DYNAMIC_CONFIG_ENABLED: "true"

volumes:
  kafka-data:
    driver: local
```

启动环境：

```bash
cd docker
docker compose up -d
```

查看容器状态：

```bash
docker compose ps
docker logs -f ateng-kafka
```

创建测试 Topic：

```bash
docker exec -it ateng-kafka /opt/kafka/bin/kafka-topics.sh \
  --create \
  --topic ateng.kafka.demo.topic \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1
```

查看 Topic 列表：

```bash
docker exec -it ateng-kafka /opt/kafka/bin/kafka-topics.sh \
  --list \
  --bootstrap-server localhost:9092
```

查看 Topic 详情：

```bash
docker exec -it ateng-kafka /opt/kafka/bin/kafka-topics.sh \
  --describe \
  --topic ateng.kafka.demo.topic \
  --bootstrap-server localhost:9092
```

发送测试消息：

```bash
docker exec -it ateng-kafka /opt/kafka/bin/kafka-console-producer.sh \
  --topic ateng.kafka.demo.topic \
  --bootstrap-server localhost:9092
```

消费测试消息：

```bash
docker exec -it ateng-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --topic ateng.kafka.demo.topic \
  --bootstrap-server localhost:9092 \
  --from-beginning
```

停止环境：

```bash
cd docker
docker compose down
```

删除环境和数据卷：

```bash
cd docker
docker compose down -v
```

`docker compose up -d` 用于后台启动 Kafka 和 Kafka UI。`docker exec -it ateng-kafka` 用于进入 Kafka 容器执行 Kafka 脚本。`docker compose down -v` 会删除数据卷，Topic 和历史消息也会被清理，执行前需要确认不再需要本地数据。

### Kafka UI 管理工具

Kafka UI 用于在本地或测试环境中查看 Kafka 集群信息、Topic、Partition、Consumer Group、消息内容和消费 Lag。`provectuslabs/kafka-ui` 是常见的开源 Kafka 管理界面，官方仓库说明它可用于监控和管理 Kafka 集群，支持查看 Brokers、Topics、Partitions、Consumer Groups、消息浏览和动态 Topic 配置等能力。([GitHub](https://github.com/provectus/kafka-ui?utm_source=chatgpt.com))

本地访问地址：

```text
http://localhost:8080
```

Kafka UI 常用功能如下：

| 功能            | 说明                                                         |
| --------------- | ------------------------------------------------------------ |
| Brokers         | 查看 Broker 节点、Controller 状态和集群基础信息              |
| Topics          | 查看 Topic 列表、Partition 数量、副本状态和 Topic 配置       |
| Messages        | 浏览 Topic 中的消息内容，支持按 Offset、Partition 等条件查看 |
| Consumer Groups | 查看消费组、消费进度、Lag 和成员信息                         |
| Create Topic    | 本地开发环境可以通过 UI 创建测试 Topic                       |
| Produce Message | 本地验证时可以通过 UI 发送测试消息                           |
| Dynamic Config  | 本地开发可临时调整部分配置，生产环境应谨慎开放               |

本地开发建议：

1. Kafka UI 只建议在本地、开发或测试环境使用。
2. 生产环境如需使用 Kafka UI，必须接入认证、授权、审计和网络访问控制。
3. 不建议在生产环境通过 UI 随意创建、删除或修改 Topic。
4. 浏览消息内容时需要注意敏感字段，例如手机号、身份证号、Token、密钥和业务隐私数据。
5. 如果 Topic 消息量较大，不要频繁进行大范围消息扫描，避免影响 Broker 和网络资源。

Spring Boot 项目连接本地 Kafka 时，可以先在 `application.yml` 中配置基础连接地址：

```yaml
spring:
  kafka:
    # 本地 Docker Compose 暴露给宿主机的 Kafka 地址
    bootstrap-servers: localhost:9092

    consumer:
      # 默认消费组，后续业务监听器可以单独覆盖
      group-id: ateng-kafka-demo-group

      # 当没有已提交 Offset 时，从最早消息开始消费，适合本地开发验证
      auto-offset-reset: earliest

    producer:
      # Key 序列化器
      key-serializer: org.apache.kafka.common.serialization.StringSerializer

      # Value 序列化器，基础示例先使用字符串
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
```

完成配置后，可以先通过 Kafka UI 确认 Topic 已存在，再启动 Spring Boot 应用进行 Producer 和 Consumer 联调。后续正式章节中，应继续补充 JSON 序列化、Header 设计、手动提交 Offset、异常处理和死信 Topic 配置。

## 项目结构设计

本章节用于定义 Kafka 开发项目的 Maven 模块、包目录、配置类、生产者、消费者、消息模型、常量枚举、异常处理和测试目录。清晰的项目结构可以降低后续功能扩展成本，避免 Kafka 相关代码分散在各业务模块中，导致配置重复、异常处理不一致和消息模型不可控。

Kafka 开发项目建议采用“配置集中、发送统一、消费隔离、模型明确、异常标准化”的结构原则。Producer 和 Consumer 可以共用消息模型、常量枚举和基础配置，但业务消费逻辑应保持独立，避免多个业务场景混杂在同一个监听类中。

### Maven 模块结构

Maven 模块结构需要根据项目规模选择。小型项目可以采用单模块结构，中大型项目建议采用多模块结构，将 Kafka 公共能力、业务实现和测试验证拆分开。

小型项目推荐结构如下：

```text
kafka-develop-demo
├── pom.xml
└── src
    ├── main
    │   ├── java
    │   │   └── io.github.atengk.kafka
    │   │       ├── config
    │   │       ├── constant
    │   │       ├── enums
    │   │       ├── exception
    │   │       ├── model
    │   │       ├── producer
    │   │       └── consumer
    │   └── resources
    │       ├── application.yml
    │       ├── application-dev.yml
    │       ├── application-test.yml
    │       └── application-prod.yml
    └── test
        └── java
            └── io.github.atengk.kafka
```

中大型项目推荐结构如下：

```text
kafka-develop-parent
├── pom.xml
├── kafka-common
│   ├── pom.xml
│   └── src/main/java/io/github/atengk/kafka/common
│       ├── constant
│       ├── enums
│       ├── exception
│       └── model
├── kafka-core
│   ├── pom.xml
│   └── src/main/java/io/github/atengk/kafka/core
│       ├── config
│       ├── properties
│       ├── producer
│       ├── consumer
│       ├── handler
│       └── support
├── kafka-service
│   ├── pom.xml
│   └── src/main/java/io/github/atengk/kafka/service
│       ├── order
│       ├── payment
│       ├── user
│       └── log
└── kafka-test
    ├── pom.xml
    └── src/test/java/io/github/atengk/kafka
        ├── producer
        ├── consumer
        ├── retry
        └── integration
```

各模块职责如下：

| 模块            | 职责                                                         |
| --------------- | ------------------------------------------------------------ |
| `kafka-common`  | 存放公共常量、枚举、异常、消息模型和基础 DTO                 |
| `kafka-core`    | 存放 Kafka 配置、Producer 封装、Consumer 基础组件、异常处理器和通用支持类 |
| `kafka-service` | 存放具体业务消息发送和消费逻辑，例如订单、支付、用户、日志等 |
| `kafka-test`    | 存放 Kafka 集成测试、异常重试测试、死信测试和性能测试代码    |

父工程通过 `modules` 管理子模块，统一控制版本和依赖。

文件位置：`pom.xml`

该配置用于定义 Kafka 多模块项目的父工程结构。

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- 统一使用 Spring Boot 父工程管理依赖版本 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.5</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>kafka-develop-parent</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <!-- 子模块统一由父工程管理 -->
    <modules>
        <module>kafka-common</module>
        <module>kafka-core</module>
        <module>kafka-service</module>
        <module>kafka-test</module>
    </modules>

    <properties>
        <!-- Spring Boot 3 推荐使用 JDK 17 或更高版本 -->
        <java.version>17</java.version>

        <!-- Hutool 工具类版本 -->
        <hutool.version>5.8.38</hutool.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Hutool 版本统一管理，子模块直接引用即可 -->
            <dependency>
                <groupId>cn.hutool</groupId>
                <artifactId>hutool-all</artifactId>
                <version>${hutool.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

### 包目录规划

包目录规划应按照 Kafka 能力边界进行拆分，而不是按照技术类名随意堆放。建议以 `io.github.atengk.kafka` 作为基础包路径。

推荐包目录如下：

```text
io.github.atengk.kafka
├── config
│   ├── KafkaProducerConfig.java
│   ├── KafkaConsumerConfig.java
│   ├── KafkaListenerConfig.java
│   └── KafkaTopicConfig.java
├── properties
│   ├── KafkaTopicProperties.java
│   └── KafkaSecurityProperties.java
├── producer
│   ├── KafkaMessageProducer.java
│   ├── DefaultKafkaMessageProducer.java
│   └── KafkaSendResultHandler.java
├── consumer
│   ├── OrderMessageConsumer.java
│   ├── PaymentMessageConsumer.java
│   └── LogMessageConsumer.java
├── model
│   ├── MessageEnvelope.java
│   ├── MessageHeader.java
│   ├── OrderCreatedMessage.java
│   └── PaymentSuccessMessage.java
├── constant
│   ├── KafkaTopicConstant.java
│   ├── KafkaHeaderConstant.java
│   └── KafkaGroupConstant.java
├── enums
│   ├── MessageTypeEnum.java
│   ├── MessageStatusEnum.java
│   └── RetrySceneEnum.java
├── exception
│   ├── KafkaSendException.java
│   ├── KafkaConsumeException.java
│   └── KafkaRetryException.java
├── handler
│   ├── KafkaErrorHandler.java
│   ├── KafkaDeadLetterHandler.java
│   └── KafkaRetryHandler.java
└── support
    ├── KafkaMessageIdGenerator.java
    ├── KafkaTraceSupport.java
    └── KafkaMessageConverter.java
```

目录职责如下：

| 目录         | 职责                                                         |
| ------------ | ------------------------------------------------------------ |
| `config`     | Kafka Producer、Consumer、Listener、Topic、ErrorHandler 等配置类 |
| `properties` | 自定义配置属性绑定类，用于承接 `application.yml` 中的业务配置 |
| `producer`   | 统一消息发送接口、默认实现、发送结果处理和失败处理           |
| `consumer`   | 具体业务消费者监听类                                         |
| `model`      | 消息体、消息信封、Header、业务事件模型                       |
| `constant`   | Topic、Header、Consumer Group 等常量                         |
| `enums`      | 消息类型、消息状态、重试场景等枚举                           |
| `exception`  | Kafka 发送、消费、重试和死信相关异常                         |
| `handler`    | 消费异常、重试、死信和降级处理器                             |
| `support`    | 消息 ID、TraceId、消息转换、日志辅助等支撑类                 |

### 配置类目录

配置类目录用于集中管理 Kafka 相关 Bean，例如 Topic 初始化、Producer 配置、Consumer 配置、监听容器工厂、错误处理器、死信发布器和消息转换器。配置类不应混入具体业务逻辑，只负责基础设施配置。

推荐配置类如下：

| 配置类                    | 职责                                                         |
| ------------------------- | ------------------------------------------------------------ |
| `KafkaProducerConfig`     | 配置 ProducerFactory、KafkaTemplate、发送参数                |
| `KafkaConsumerConfig`     | 配置 ConsumerFactory、反序列化、消费参数                     |
| `KafkaListenerConfig`     | 配置 ConcurrentKafkaListenerContainerFactory、AckMode、并发数和错误处理 |
| `KafkaTopicConfig`        | 配置 Topic 自动创建和 Topic 初始化                           |
| `KafkaErrorHandlerConfig` | 配置 DefaultErrorHandler、重试策略和死信处理                 |
| `KafkaSecurityConfig`     | 配置 SASL、SSL 等安全认证相关参数                            |

配置类命名应以 `Kafka` 开头，以 `Config` 结尾，便于扫描和识别。例如：

```text
src/main/java/io/github/atengk/kafka/config/KafkaProducerConfig.java
src/main/java/io/github/atengk/kafka/config/KafkaConsumerConfig.java
src/main/java/io/github/atengk/kafka/config/KafkaListenerConfig.java
src/main/java/io/github/atengk/kafka/config/KafkaTopicConfig.java
```

### 生产者目录

生产者目录用于存放统一的消息发送接口和默认实现。业务模块不建议直接散落使用 `KafkaTemplate`，而应通过统一封装的 Producer 组件发送消息，便于统一设置 Header、TraceId、消息 ID、发送日志、异常处理和发送结果回调。

推荐结构如下：

```text
producer
├── KafkaMessageProducer.java
├── DefaultKafkaMessageProducer.java
├── KafkaSendRequest.java
├── KafkaSendResult.java
└── KafkaSendResultHandler.java
```

生产者目录职责如下：

| 文件                          | 职责                                                  |
| ----------------------------- | ----------------------------------------------------- |
| `KafkaMessageProducer`        | 定义统一消息发送接口                                  |
| `DefaultKafkaMessageProducer` | 基于 `KafkaTemplate` 实现消息发送                     |
| `KafkaSendRequest`            | 封装 Topic、Key、消息体、Header、Partition 等发送参数 |
| `KafkaSendResult`             | 封装发送结果、Topic、Partition、Offset 和异常信息     |
| `KafkaSendResultHandler`      | 处理发送成功、发送失败、日志记录和异常转换            |

生产者封装应满足以下要求：

1. 所有消息发送必须指定 Topic。
2. 关键业务消息必须指定业务 Key。
3. 发送前统一补充消息 ID、TraceId、消息类型、发送时间等 Header。
4. 发送成功后记录 Topic、Partition、Offset、业务 Key 和消息 ID。
5. 发送失败后记录异常原因，并按业务需要抛出自定义异常或进入补偿流程。

### 消费者目录

消费者目录用于存放具体业务消息监听类。每个消费者类应围绕单一业务场景设计，避免一个监听类中处理过多不相关 Topic。

推荐结构如下：

```text
consumer
├── OrderMessageConsumer.java
├── PaymentMessageConsumer.java
├── UserMessageConsumer.java
└── LogMessageConsumer.java
```

消费者命名建议如下：

| 命名方式          | 示例                      |
| ----------------- | ------------------------- |
| 按业务领域命名    | `OrderMessageConsumer`    |
| 按 Topic 语义命名 | `PaymentSuccessConsumer`  |
| 按处理动作命名    | `OrderStatisticsConsumer` |
| 按数据流向命名    | `UserSyncConsumer`        |

消费者开发应遵循以下规范：

1. 每个消费者方法只处理一种明确的业务消息。
2. 消费成功后再提交 Offset。
3. 消费逻辑必须具备幂等能力。
4. 异常不得被随意吞掉，应交给统一异常处理器或明确记录失败状态。
5. 消费日志必须包含 Topic、Partition、Offset、Key、消息 ID 和 TraceId。
6. 批量消费时，应明确单条失败对整批消息的影响策略。

### 消息模型目录

消息模型目录用于存放 Kafka 消息体、消息信封、消息 Header 和具体业务事件模型。建议所有业务消息都采用统一的外层结构，便于后续扩展版本号、消息 ID、TraceId、发送时间和业务类型。

推荐结构如下：

```text
model
├── MessageEnvelope.java
├── MessageHeader.java
├── OrderCreatedMessage.java
├── PaymentSuccessMessage.java
└── UserLoginMessage.java
```

消息模型建议分为两层：

| 层级       | 说明                                                         |
| ---------- | ------------------------------------------------------------ |
| 消息信封   | 统一字段，例如消息 ID、消息类型、版本、发送时间、TraceId、业务 Key |
| 业务消息体 | 具体业务字段，例如订单 ID、用户 ID、支付单号、金额、状态等   |

推荐统一消息结构如下：

```json
{
  "messageId": "1870000000000000001",
  "messageType": "ORDER_CREATED",
  "version": "1.0",
  "traceId": "trace-20260511-001",
  "bizKey": "ORDER_10001",
  "sendTime": "2026-05-11T10:30:00",
  "payload": {
    "orderId": "10001",
    "userId": "20001",
    "amount": 199.90
  }
}
```

消息模型设计要求如下：

1. `messageId` 必须全局唯一，用于幂等、追踪和重放。
2. `messageType` 用于区分业务事件类型。
3. `version` 用于消息结构演进。
4. `traceId` 用于链路追踪。
5. `bizKey` 用于业务定位和分区 Key 选择。
6. `payload` 只存放业务数据，不存放 Kafka 技术字段。

### 常量与枚举目录

常量与枚举目录用于统一管理 Topic、Consumer Group、Header、消息类型、消息状态和重试场景，避免字符串硬编码散落在代码中。

推荐结构如下：

```text
constant
├── KafkaTopicConstant.java
├── KafkaHeaderConstant.java
└── KafkaGroupConstant.java

enums
├── MessageTypeEnum.java
├── MessageStatusEnum.java
└── RetrySceneEnum.java
```

常量分类如下：

| 常量类                | 职责                     |
| --------------------- | ------------------------ |
| `KafkaTopicConstant`  | 定义 Topic 名称          |
| `KafkaHeaderConstant` | 定义消息 Header Key      |
| `KafkaGroupConstant`  | 定义 Consumer Group 名称 |

枚举分类如下：

| 枚举类              | 职责                                           |
| ------------------- | ---------------------------------------------- |
| `MessageTypeEnum`   | 定义业务消息类型                               |
| `MessageStatusEnum` | 定义消息处理状态                               |
| `RetrySceneEnum`    | 定义重试场景，例如发送重试、消费重试、死信重放 |

常量类示例：

文件位置：`src/main/java/io/github/atengk/kafka/constant/KafkaTopicConstant.java`

该常量类用于统一管理 Kafka Topic 名称，避免业务代码中直接硬编码 Topic 字符串。

```java
package io.github.atengk.kafka.constant;

/**
 * Kafka Topic 常量
 *
 * @author Ateng
 * @since 2026-05-11
 */
public final class KafkaTopicConstant {

    /**
     * 订单创建消息 Topic
     */
    public static final String ORDER_CREATED_TOPIC = "ateng.order.created.topic";

    /**
     * 支付成功消息 Topic
     */
    public static final String PAYMENT_SUCCESS_TOPIC = "ateng.payment.success.topic";

    /**
     * 通用死信消息 Topic
     */
    public static final String DEAD_LETTER_TOPIC = "ateng.common.dead-letter.topic";

    private KafkaTopicConstant() {
    }

}
```

### 异常处理目录

异常处理目录用于存放 Kafka 发送、消费、重试、死信和反序列化相关异常。统一异常类型可以让上层业务、日志组件和监控告警更容易识别问题来源。

推荐结构如下：

```text
exception
├── KafkaSendException.java
├── KafkaConsumeException.java
├── KafkaRetryException.java
├── KafkaDeadLetterException.java
└── KafkaDeserializeException.java
```

异常分类如下：

| 异常类                      | 使用场景                             |
| --------------------------- | ------------------------------------ |
| `KafkaSendException`        | 消息发送失败、发送超时、发送结果异常 |
| `KafkaConsumeException`     | 消息消费失败、业务处理失败           |
| `KafkaRetryException`       | 重试处理失败、超过最大重试次数       |
| `KafkaDeadLetterException`  | 死信投递失败、死信重放失败           |
| `KafkaDeserializeException` | 消息反序列化失败、消息格式不兼容     |

异常处理规范如下：

1. 发送异常应记录 Topic、Key、消息 ID 和异常原因。
2. 消费异常应记录 Topic、Partition、Offset、Key、消息 ID 和 TraceId。
3. 可重试异常交给重试机制处理。
4. 不可重试异常应直接进入死信或记录失败状态。
5. 异常日志中不得打印敏感明文字段，例如密码、Token、证件号和密钥。

### 测试目录

测试目录用于存放 Producer、Consumer、配置、异常重试、死信处理和幂等消费相关测试。Kafka 相关功能不建议只做单元测试，还应通过 Embedded Kafka 或 Testcontainers 完成集成验证。

推荐测试结构如下：

```text
src/test/java/io/github/atengk/kafka
├── producer
│   ├── KafkaMessageProducerTest.java
│   └── KafkaSendResultHandlerTest.java
├── consumer
│   ├── OrderMessageConsumerTest.java
│   └── PaymentMessageConsumerTest.java
├── config
│   ├── KafkaTopicConfigTest.java
│   └── KafkaListenerConfigTest.java
├── retry
│   ├── KafkaRetryTest.java
│   └── KafkaDeadLetterTest.java
└── integration
    ├── KafkaEmbeddedIntegrationTest.java
    └── KafkaTestcontainersIntegrationTest.java
```

测试内容建议如下：

| 测试类型       | 验证内容                                                    |
| -------------- | ----------------------------------------------------------- |
| Producer 测试  | 验证消息发送成功、发送失败、Header 设置、Key 设置和回调处理 |
| Consumer 测试  | 验证消息监听、业务处理、Offset 提交和异常处理               |
| Topic 配置测试 | 验证 Topic 自动创建、分区数、副本数和配置项                 |
| 重试测试       | 验证消费失败后的重试次数、重试间隔和异常分类                |
| 死信测试       | 验证超过重试次数后消息是否进入死信 Topic                    |
| 幂等测试       | 验证重复消息不会导致重复写库、重复扣减或重复通知            |
| 性能测试       | 验证批量发送、批量消费、并发消费和消息堆积处理能力          |

## Kafka 配置管理

本章节用于定义 Kafka 在 Spring Boot 项目中的配置管理方式，包括 `application.yml` 配置规划、Bootstrap Servers、Producer 通用配置、Consumer 通用配置、Listener 容器配置、Topic 自动创建、多环境配置和敏感配置管理。

Kafka 配置应遵循“通用配置集中管理、业务差异局部覆盖、敏感信息外部注入、生产配置严格控制”的原则。开发环境可以适当简化，生产环境必须明确可靠性、安全性、可观测性和可维护性要求。

### application.yml 配置规划

`application.yml` 是 Spring Boot 项目中 Kafka 客户端配置的主要入口。通用配置建议放在 `application.yml`，环境差异配置放在 `application-dev.yml`、`application-test.yml` 和 `application-prod.yml` 中。

推荐配置文件结构如下：

```text
src/main/resources
├── application.yml
├── application-dev.yml
├── application-test.yml
└── application-prod.yml
```

基础配置建议分为两类：

| 配置类型              | 配置前缀       | 说明                                                         |
| --------------------- | -------------- | ------------------------------------------------------------ |
| Spring Kafka 原生配置 | `spring.kafka` | Spring Boot 自动识别，用于配置 Producer、Consumer、Listener、Admin |
| 项目自定义配置        | `app.kafka`    | 项目自定义业务配置，例如 Topic 初始化、业务开关、死信配置等  |

文件位置：`src/main/resources/application.yml`

该配置用于定义 Kafka 通用连接、生产者、消费者、监听容器和项目自定义 Topic 初始化参数。

```yaml
spring:
  application:
    name: kafka-develop-demo

  kafka:
    # Kafka 集群连接地址，多个地址使用逗号分隔
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}

    # Kafka 客户端 ID，便于 Broker 日志和监控中识别来源应用
    client-id: ${spring.application.name}

    admin:
      # 是否启用 KafkaAdmin，开启后可配合 NewTopic Bean 初始化 Topic
      auto-create: true

    producer:
      # Producer 发送确认机制，all 表示等待所有同步副本确认
      acks: all

      # 发送失败重试次数
      retries: 3

      # Key 序列化器
      key-serializer: org.apache.kafka.common.serialization.StringSerializer

      # Value 序列化器，业务对象推荐使用 JsonSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

      properties:
        # 开启幂等 Producer，降低重试导致的重复写入风险
        enable.idempotence: true

        # 幂等 Producer 下建议不超过 5
        max.in.flight.requests.per.connection: 5

        # 单条消息发送完整生命周期超时时间
        delivery.timeout.ms: 120000

        # 请求 Broker 响应超时时间
        request.timeout.ms: 30000

        # 批量发送等待时间，适当增加可提升吞吐
        linger.ms: 20

        # 批量发送缓冲大小，单位字节
        batch.size: 32768

        # 消息压缩方式，本地和通用业务推荐 lz4
        compression.type: lz4

        # 不在消息 Header 中添加 Java 类型信息，降低跨语言消费耦合
        spring.json.add.type.headers: false

    consumer:
      # 默认消费组，具体业务监听器可以通过 @KafkaListener 单独指定
      group-id: ${KAFKA_CONSUMER_GROUP:kafka-develop-demo-group}

      # 关闭自动提交 Offset，业务处理成功后手动提交
      enable-auto-commit: false

      # 没有已提交 Offset 时，从最早消息开始消费
      auto-offset-reset: earliest

      # Key 反序列化器
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer

      # Value 使用错误处理反序列化器，避免反序列化异常直接阻断消费线程
      value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer

      properties:
        # 实际 Value 反序列化委托类
        spring.deserializer.value.delegate.class: org.springframework.kafka.support.serializer.JsonDeserializer

        # 允许反序列化的包路径，生产环境不建议使用 *
        spring.json.trusted.packages: io.github.atengk.kafka.model,java.util,java.lang

        # 不依赖 Header 中的类型信息进行反序列化
        spring.json.use.type.headers: false

        # 默认消息类型，后续可替换为统一消息信封类
        spring.json.value.default.type: io.github.atengk.kafka.model.MessageEnvelope

        # 单次 poll 最大拉取消息数
        max.poll.records: 100

        # 两次 poll 最大间隔，业务处理较慢时需要适当调大
        max.poll.interval.ms: 300000

        # Consumer 会话超时时间
        session.timeout.ms: 45000

        # Consumer 心跳间隔，通常小于 session.timeout.ms
        heartbeat.interval.ms: 15000

    listener:
      # 手动立即提交 Offset，适合重要业务消息
      ack-mode: manual_immediate

      # 默认监听并发数，不能盲目大于 Topic 分区数
      concurrency: 3

      # 单条消息监听模式
      type: single

      # Topic 不存在时不直接启动失败，本地和测试环境较友好
      missing-topics-fatal: false

management:
  endpoints:
    web:
      exposure:
        # 暴露健康检查和 Prometheus 指标
        include: health,info,prometheus

  endpoint:
    health:
      # 展示详细健康信息，生产环境可按安全要求调整
      show-details: when_authorized

app:
  kafka:
    topic:
      # 本地和测试环境可开启，生产环境建议关闭并走 Topic 申请流程
      auto-create: true

      topics:
        - name: ateng.order.created.topic
          partitions: 3
          replicas: 1
          configs:
            # 消息保留 7 天
            retention.ms: "604800000"
            # 删除策略
            cleanup.policy: delete

        - name: ateng.payment.success.topic
          partitions: 3
          replicas: 1
          configs:
            retention.ms: "604800000"
            cleanup.policy: delete

        - name: ateng.common.dead-letter.topic
          partitions: 3
          replicas: 1
          configs:
            # 死信消息建议保留更长时间，便于排查和重放
            retention.ms: "1209600000"
            cleanup.policy: delete
```

### Bootstrap Servers 配置

`bootstrap-servers` 是 Kafka 客户端连接 Kafka 集群的入口地址。它不要求配置所有 Broker，但建议配置多个 Broker 地址，避免单个地址不可用时影响客户端初始化。

配置示例：

```yaml
spring:
  kafka:
    # 开发环境单节点
    bootstrap-servers: localhost:9092
```

多 Broker 配置示例：

```yaml
spring:
  kafka:
    # 测试或生产环境建议配置多个 Broker 地址
    bootstrap-servers: kafka-01:9092,kafka-02:9092,kafka-03:9092
```

使用环境变量配置示例：

```yaml
spring:
  kafka:
    # 优先读取环境变量，未配置时使用 localhost:9092
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
```

配置要求如下：

| 要求                 | 说明                                                      |
| -------------------- | --------------------------------------------------------- |
| 不写死生产地址       | 生产地址应通过环境变量、配置中心或部署平台注入            |
| 配置多个 Broker      | 提升客户端初始化连接的可用性                              |
| 区分内外网地址       | Docker、本地、Kubernetes 和生产环境访问地址可能不同       |
| 避免使用容器内部地址 | 宿主机访问 Kafka 时不能使用仅容器网络可见的地址           |
| 保持端口一致性       | Kafka `advertised.listeners` 必须与客户端实际访问地址匹配 |

常见问题是 Kafka 容器内部监听地址和宿主机访问地址不一致。例如 Spring Boot 在宿主机运行时，应连接 `localhost:9092`；如果 Spring Boot 也在 Docker Compose 网络内运行，则应连接 `kafka:9092`。

### Producer 通用配置

Producer 通用配置用于控制消息发送可靠性、吞吐量、序列化方式、压缩方式和超时时间。可靠业务消息推荐优先保证可靠性，再根据吞吐需求调整批量和压缩参数。

推荐配置如下：

```yaml
spring:
  kafka:
    producer:
      # 等待所有同步副本确认，可靠性最高
      acks: all

      # 发送失败重试次数
      retries: 3

      # Key 使用字符串序列化，便于按业务 Key 分区
      key-serializer: org.apache.kafka.common.serialization.StringSerializer

      # Value 使用 JSON 序列化，适合 Spring Boot 业务对象
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

      properties:
        # 开启幂等发送
        enable.idempotence: true

        # 控制未确认请求数量，避免重试时破坏顺序
        max.in.flight.requests.per.connection: 5

        # Producer 完成一次发送的最大总耗时
        delivery.timeout.ms: 120000

        # 单次请求等待 Broker 响应的超时时间
        request.timeout.ms: 30000

        # 批量发送等待时间
        linger.ms: 20

        # 批量发送大小
        batch.size: 32768

        # 缓冲区大小
        buffer.memory: 33554432

        # 压缩方式
        compression.type: lz4

        # 不写入 Java 类型 Header，降低跨服务耦合
        spring.json.add.type.headers: false
```

关键参数说明如下：

| 参数                                    | 推荐值     | 说明                         |
| --------------------------------------- | ---------- | ---------------------------- |
| `acks`                                  | `all`      | 等待同步副本确认，提升可靠性 |
| `retries`                               | `3` 或更高 | 发送失败后自动重试           |
| `enable.idempotence`                    | `true`     | 开启 Producer 幂等           |
| `max.in.flight.requests.per.connection` | `5`        | 配合幂等发送控制顺序风险     |
| `delivery.timeout.ms`                   | `120000`   | 控制消息发送总超时时间       |
| `request.timeout.ms`                    | `30000`    | 控制单次请求超时时间         |
| `linger.ms`                             | `20`       | 提高批量聚合能力             |
| `batch.size`                            | `32768`    | 增大批量发送吞吐             |
| `compression.type`                      | `lz4`      | 通用场景压缩性能较好         |

生产环境中，`acks=all`、`enable.idempotence=true` 和合理的 `retries` 应作为可靠消息发送的基础配置。对延迟极其敏感的场景，可以降低 `linger.ms`，但需要接受吞吐下降。

### Consumer 通用配置

Consumer 通用配置用于控制消费组、Offset 提交、反序列化、单次拉取数量、心跳、会话超时和消费间隔。重要业务消息建议关闭自动提交 Offset，改为业务处理成功后手动提交。

推荐配置如下：

```yaml
spring:
  kafka:
    consumer:
      # 默认消费组，业务监听器可单独覆盖
      group-id: ${KAFKA_CONSUMER_GROUP:kafka-develop-demo-group}

      # 关闭自动提交 Offset
      enable-auto-commit: false

      # 没有 Offset 时从最早消息开始消费
      auto-offset-reset: earliest

      # Key 反序列化器
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer

      # Value 使用错误处理反序列化器，避免脏数据阻塞消费线程
      value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer

      properties:
        # 委托 JSON 反序列化器处理消息体
        spring.deserializer.value.delegate.class: org.springframework.kafka.support.serializer.JsonDeserializer

        # 限制可信包路径
        spring.json.trusted.packages: io.github.atengk.kafka.model,java.util,java.lang

        # 不依赖消息 Header 中的 Java 类型
        spring.json.use.type.headers: false

        # 默认反序列化类型
        spring.json.value.default.type: io.github.atengk.kafka.model.MessageEnvelope

        # 单次拉取最大消息数
        max.poll.records: 100

        # poll 最大间隔，业务处理时间不能超过该值
        max.poll.interval.ms: 300000

        # 会话超时时间
        session.timeout.ms: 45000

        # 心跳间隔
        heartbeat.interval.ms: 15000
```

关键参数说明如下：

| 参数                           | 推荐值                 | 说明                                        |
| ------------------------------ | ---------------------- | ------------------------------------------- |
| `enable-auto-commit`           | `false`                | 业务处理成功后再提交 Offset                 |
| `auto-offset-reset`            | `earliest` 或 `latest` | 开发环境可用 `earliest`，生产按业务语义选择 |
| `max.poll.records`             | `100`                  | 控制单次拉取消息数量                        |
| `max.poll.interval.ms`         | `300000`               | 控制两次 poll 最大间隔                      |
| `session.timeout.ms`           | `45000`                | 控制 Consumer 会话超时                      |
| `heartbeat.interval.ms`        | `15000`                | 控制心跳频率                                |
| `spring.json.trusted.packages` | 指定业务包             | 避免反序列化安全风险                        |

`auto-offset-reset` 选择建议如下：

| 值         | 说明                     | 适用场景                                 |
| ---------- | ------------------------ | ---------------------------------------- |
| `earliest` | 从最早可用消息开始消费   | 本地开发、测试环境、需要补历史消息的场景 |
| `latest`   | 从最新消息开始消费       | 生产新消费组上线，且不需要处理历史消息   |
| `none`     | 没有 Offset 时直接抛异常 | 对 Offset 起点要求严格的场景             |

### Listener 容器配置

Listener 容器配置用于控制 `@KafkaListener` 的运行方式，包括 AckMode、并发数、单条或批量监听、异常处理器和消息转换器。对于重要业务消费，推荐使用手动提交 Offset，并通过统一容器工厂管理监听行为。

推荐配置如下：

```yaml
spring:
  kafka:
    listener:
      # 手动立即提交 Offset
      ack-mode: manual_immediate

      # 默认并发数
      concurrency: 3

      # 单条消费模式，可选 single 或 batch
      type: single

      # Topic 不存在时是否启动失败
      missing-topics-fatal: false
```

Listener 参数说明如下：

| 参数                   | 推荐值             | 说明                                   |
| ---------------------- | ------------------ | -------------------------------------- |
| `ack-mode`             | `manual_immediate` | 业务处理成功后手动确认                 |
| `concurrency`          | `3`                | 监听并发数，通常不超过 Topic 分区数    |
| `type`                 | `single`           | 单条消费更容易处理幂等和异常           |
| `missing-topics-fatal` | `false`            | 本地和测试环境更友好，生产可按规范调整 |

如果需要统一定义监听容器工厂，可以增加配置类。

文件位置：`src/main/java/io/github/atengk/kafka/config/KafkaListenerConfig.java`

该配置类用于统一定义 Kafka 监听容器工厂，设置手动提交 Offset、并发数和基础容器行为。

```java
package io.github.atengk.kafka.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

/**
 * Kafka 监听容器配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Configuration
public class KafkaListenerConfig {

    /**
     * 配置 Kafka 监听容器工厂
     *
     * @param consumerFactory 消费者工厂
     * @return Kafka 监听容器工厂
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);

        // 手动立即提交 Offset，业务处理成功后再 ack
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // 监听容器启动日志，便于确认配置已生效
        log.info("初始化Kafka监听容器工厂，ackMode=MANUAL_IMMEDIATE，concurrency=3");
        return factory;
    }

}
```

使用该容器工厂时，监听器可以指定 `containerFactory`：

```java
@KafkaListener(
        topics = KafkaTopicConstant.ORDER_CREATED_TOPIC,
        groupId = KafkaGroupConstant.ORDER_GROUP,
        containerFactory = "kafkaListenerContainerFactory"
)
public void consume(Object message, Acknowledgment acknowledgment) {
    // 业务处理成功后手动提交 Offset
    acknowledgment.acknowledge();
}
```

### Topic 自动创建配置

Topic 自动创建配置用于在应用启动时初始化开发或测试环境所需 Topic。生产环境不建议由业务应用自动创建 Topic，而应通过 Topic 申请流程、运维平台或基础设施脚本统一管理。

Topic 自动创建建议遵循以下原则：

| 环境     | 是否建议自动创建 | 说明                         |
| -------- | ---------------- | ---------------------------- |
| 本地环境 | 建议开启         | 方便开发人员快速联调         |
| 测试环境 | 可开启           | 便于自动化测试和集成验证     |
| 预发环境 | 谨慎开启         | 应尽量模拟生产流程           |
| 生产环境 | 不建议开启       | 应由运维或中间件平台统一管理 |

文件位置：`src/main/java/io/github/atengk/kafka/properties/KafkaTopicProperties.java`

该配置属性类用于承接 `app.kafka.topic` 下的 Topic 初始化配置。

```java
package io.github.atengk.kafka.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * Kafka Topic 初始化配置属性
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@ConfigurationProperties(prefix = "app.kafka.topic")
public class KafkaTopicProperties {

    /**
     * 是否启用应用启动时自动创建 Topic
     */
    private Boolean autoCreate = false;

    /**
     * Topic 配置列表
     */
    private List<TopicItem> topics = List.of();

    /**
     * Topic 配置项
     */
    @Data
    public static class TopicItem {

        /**
         * Topic 名称
         */
        private String name;

        /**
         * 分区数量
         */
        private Integer partitions = 3;

        /**
         * 副本数量
         */
        private Short replicas = 1;

        /**
         * Topic 级别配置
         */
        private Map<String, String> configs = Map.of();

    }

}
```

文件位置：`src/main/java/io/github/atengk/kafka/config/KafkaTopicConfig.java`

该配置类用于读取 `application.yml` 中的 Topic 配置，并在应用启动时通过 `KafkaAdmin` 初始化 Topic。

```java
package io.github.atengk.kafka.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.kafka.properties.KafkaTopicProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.Map;

/**
 * Kafka Topic 初始化配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class KafkaTopicConfig {

    private final KafkaTopicProperties kafkaTopicProperties;

    /**
     * 创建 Kafka Topic 配置 Bean
     *
     * @return Kafka Topic 集合
     */
    @Bean
    @ConditionalOnProperty(prefix = "app.kafka.topic", name = "auto-create", havingValue = "true")
    public KafkaAdmin.NewTopics kafkaTopics() {
        if (CollUtil.isEmpty(kafkaTopicProperties.getTopics())) {
            log.warn("未配置需要初始化的Kafka Topic");
            return new KafkaAdmin.NewTopics();
        }

        NewTopic[] topics = kafkaTopicProperties.getTopics()
                .stream()
                .filter(item -> StrUtil.isNotBlank(item.getName()))
                .map(item -> {
                    Map<String, String> configs = item.getConfigs() == null ? Map.of() : item.getConfigs();
                    log.info("初始化Kafka Topic配置，topic={}，partitions={}，replicas={}",
                            item.getName(), item.getPartitions(), item.getReplicas());

                    return TopicBuilder.name(item.getName())
                            .partitions(item.getPartitions())
                            .replicas(item.getReplicas())
                            .configs(configs)
                            .build();
                })
                .toArray(NewTopic[]::new);

        return new KafkaAdmin.NewTopics(topics);
    }

}
```

Topic 配置示例：

```yaml
app:
  kafka:
    topic:
      # 是否启用 Topic 自动创建
      auto-create: true

      topics:
        - name: ateng.order.created.topic
          partitions: 3
          replicas: 1
          configs:
            # 消息保留 7 天
            retention.ms: "604800000"
            cleanup.policy: delete

        - name: ateng.common.dead-letter.topic
          partitions: 3
          replicas: 1
          configs:
            # 死信保留 14 天
            retention.ms: "1209600000"
            cleanup.policy: delete
```

需要注意，本地单 Broker 环境中 `replicas` 只能配置为 `1`。生产多 Broker 环境建议副本数配置为 `3`，并配合 `min.insync.replicas=2` 提升可靠性。

### 多环境配置

多环境配置用于区分本地、测试、预发和生产环境的 Kafka 连接地址、Topic 自动创建策略、消费起点、安全认证和监控暴露范围。不同环境不应共用同一份完整 Kafka 配置。

推荐环境划分如下：

| 环境     | 配置文件               | 说明                                                   |
| -------- | ---------------------- | ------------------------------------------------------ |
| 本地环境 | `application-dev.yml`  | 连接本地 Docker Kafka，可开启 Topic 自动创建           |
| 测试环境 | `application-test.yml` | 连接测试 Kafka，可开启 Topic 自动创建或由测试脚本创建  |
| 预发环境 | `application-pre.yml`  | 尽量模拟生产配置，谨慎自动创建 Topic                   |
| 生产环境 | `application-prod.yml` | 连接生产 Kafka，关闭自动创建 Topic，启用认证和严格监控 |

文件位置：`src/main/resources/application-dev.yml`

开发环境配置示例：

```yaml
spring:
  kafka:
    # 本地 Docker Compose Kafka 地址
    bootstrap-servers: localhost:9092

    consumer:
      # 开发环境从最早消息开始消费，便于验证历史消息
      auto-offset-reset: earliest

    listener:
      # 本地开发允许 Topic 暂时不存在
      missing-topics-fatal: false

app:
  kafka:
    topic:
      # 本地开发允许自动创建 Topic
      auto-create: true
```

文件位置：`src/main/resources/application-test.yml`

测试环境配置示例：

```yaml
spring:
  kafka:
    # 测试环境 Kafka 地址，通过环境变量注入
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:kafka-test-01:9092,kafka-test-02:9092}

    consumer:
      # 测试环境按测试用例需要选择 earliest
      auto-offset-reset: earliest

app:
  kafka:
    topic:
      # 测试环境可开启，便于自动化集成测试
      auto-create: true
```

文件位置：`src/main/resources/application-prod.yml`

生产环境配置示例：

```yaml
spring:
  kafka:
    # 生产环境必须通过环境变量或配置中心注入，不允许写死
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}

    consumer:
      # 生产新消费组通常从最新消息开始，是否使用 latest 需要按业务确认
      auto-offset-reset: latest

    listener:
      # 生产环境 Topic 不存在时应尽早暴露问题
      missing-topics-fatal: true

app:
  kafka:
    topic:
      # 生产环境不建议业务应用自动创建 Topic
      auto-create: false
```

生产环境配置要求如下：

1. 不允许在代码仓库中写死 Kafka 生产地址。
2. 不允许在代码仓库中写死 Kafka 用户名、密码、证书路径和密钥。
3. 不建议开启 Topic 自动创建。
4. Consumer Group 名称必须稳定，避免发布后生成新的消费组导致重复消费历史消息。
5. `auto-offset-reset` 必须结合业务语义确认，不能随意使用 `earliest`。
6. 分区数、副本数、保留时间和压缩策略应通过 Topic 申请或运维流程确认。

### 敏感配置管理

敏感配置包括 Kafka 用户名、密码、SASL JAAS 配置、SSL 证书路径、TrustStore 密码、KeyStore 密码、生产集群地址和内部网络信息。这些配置不应提交到 Git 仓库，也不应直接写在镜像中。

敏感配置建议通过以下方式管理：

| 方式              | 说明                                |
| ----------------- | ----------------------------------- |
| 环境变量          | 适合 Docker、Kubernetes、CI/CD 注入 |
| 配置中心          | 适合统一管理不同环境配置            |
| Kubernetes Secret | 适合 K8s 部署场景                   |
| 密钥管理服务      | 适合安全要求较高的生产环境          |
| 本地 `.env` 文件  | 仅适合本地开发，不能提交仓库        |

SASL_SSL 配置示例：

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}

    properties:
      # 启用 SASL_SSL 安全协议
      security.protocol: SASL_SSL

      # SASL 认证机制，具体值以 Kafka 集群配置为准
      sasl.mechanism: SCRAM-SHA-512

      # 通过环境变量注入用户名和密码，禁止写死明文
      sasl.jaas.config: org.apache.kafka.common.security.scram.ScramLoginModule required username="${KAFKA_USERNAME}" password="${KAFKA_PASSWORD}";

      # TrustStore 证书路径，容器环境中通常挂载到固定目录
      ssl.truststore.location: ${KAFKA_SSL_TRUSTSTORE_LOCATION}

      # TrustStore 密码，通过环境变量或 Secret 注入
      ssl.truststore.password: ${KAFKA_SSL_TRUSTSTORE_PASSWORD}

      # TrustStore 类型
      ssl.truststore.type: JKS
```

Kubernetes Secret 环境变量示例：

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: kafka-secret
type: Opaque
stringData:
  # Kafka 认证用户名
  KAFKA_USERNAME: "your-username"

  # Kafka 认证密码，生产环境应由密钥系统注入
  KAFKA_PASSWORD: "your-password"

  # TrustStore 密码
  KAFKA_SSL_TRUSTSTORE_PASSWORD: "your-truststore-password"
```

Deployment 中引用 Secret 的示例：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kafka-develop-demo
spec:
  replicas: 2
  selector:
    matchLabels:
      app: kafka-develop-demo
  template:
    metadata:
      labels:
        app: kafka-develop-demo
    spec:
      containers:
        - name: kafka-develop-demo
          image: kafka-develop-demo:1.0.0
          env:
            # Kafka 集群地址
            - name: KAFKA_BOOTSTRAP_SERVERS
              value: "kafka-01:9093,kafka-02:9093,kafka-03:9093"

            # Kafka 用户名来自 Secret
            - name: KAFKA_USERNAME
              valueFrom:
                secretKeyRef:
                  name: kafka-secret
                  key: KAFKA_USERNAME

            # Kafka 密码来自 Secret
            - name: KAFKA_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: kafka-secret
                  key: KAFKA_PASSWORD

            # TrustStore 密码来自 Secret
            - name: KAFKA_SSL_TRUSTSTORE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: kafka-secret
                  key: KAFKA_SSL_TRUSTSTORE_PASSWORD
```

敏感配置管理要求如下：

1. 禁止将 Kafka 生产用户名、密码、证书和密钥提交到 Git。
2. 禁止在日志中打印完整的 SASL JAAS 配置。
3. 禁止将生产 Kafka 地址写死在代码或公共配置模板中。
4. 本地 `.env`、证书文件和密钥文件必须加入 `.gitignore`。
5. 生产环境应通过 Secret、配置中心或密钥管理服务注入。
6. 排查问题时只打印必要配置，例如协议类型、认证机制、Broker 数量，不打印密码和证书内容。


## Topic 设计

本章节用于定义 Kafka Topic 的命名、分区、副本、保留策略、压缩策略、创建方式和初始化配置。Topic 是 Kafka 消息治理的核心对象，设计不合理会直接影响消息顺序性、消费并发、存储成本、故障恢复和后续运维管理。

Topic 设计应遵循“语义清晰、职责单一、容量可控、便于治理”的原则。一个 Topic 应尽量表达一种明确的业务事件，不建议将多个不相关业务事件混合写入同一个 Topic。

### Topic 命名规范

Topic 命名用于统一 Kafka 消息主题的识别方式。规范的命名可以帮助开发、测试、运维和监控系统快速判断 Topic 所属业务、事件含义和使用场景。

推荐命名格式如下：

```text
{系统标识}.{业务域}.{事件名称}.{消息类型}
```

示例：

```text
ateng.order.created.topic
ateng.order.cancelled.topic
ateng.payment.success.topic
ateng.user.login-log.topic
ateng.common.dead-letter.topic
```

命名字段说明如下：

| 字段     | 说明                   | 示例                              |
| -------- | ---------------------- | --------------------------------- |
| 系统标识 | 表示当前系统或项目名称 | `ateng`                           |
| 业务域   | 表示业务模块或领域     | `order`、`payment`、`user`        |
| 事件名称 | 表示具体业务事件       | `created`、`success`、`login-log` |
| 消息类型 | 表示 Topic 类型        | `topic`、`retry`、`dead-letter`   |

命名规范要求如下：

1. 统一使用小写字母、数字、点号和中横线。
2. 不建议使用中文、空格、特殊符号和大小写混合命名。
3. 不建议使用过于宽泛的名称，例如 `message-topic`、`business-topic`、`data-topic`。
4. Topic 名称应能体现业务语义，避免只从技术角度命名。
5. 重试 Topic 和死信 Topic 应与原始 Topic 保持可识别关系。
6. 生产环境 Topic 名称一旦确定，不应随意修改，否则会影响 Producer、Consumer、监控、告警和历史数据追踪。

推荐命名示例：

| 场景             | Topic 名称                        |
| ---------------- | --------------------------------- |
| 订单创建事件     | `ateng.order.created.topic`       |
| 订单取消事件     | `ateng.order.cancelled.topic`     |
| 支付成功事件     | `ateng.payment.success.topic`     |
| 用户登录日志     | `ateng.user.login-log.topic`      |
| 订单创建重试消息 | `ateng.order.created.retry`       |
| 订单创建死信消息 | `ateng.order.created.dead-letter` |
| 通用死信消息     | `ateng.common.dead-letter.topic`  |

不推荐命名示例：

| 不推荐名称          | 问题                               |
| ------------------- | ---------------------------------- |
| `topic1`            | 无业务语义                         |
| `order`             | 语义过宽，不清楚表示订单的什么事件 |
| `OrderCreatedTopic` | 大小写混合，不利于统一管理         |
| `订单创建Topic`     | 包含中文，跨平台和脚本处理不友好   |
| `test`              | 环境和业务含义不清晰               |

### Topic 分区规划

Partition 是 Kafka 并行写入、并行消费和局部顺序保证的基本单位。Topic 分区数量需要结合消息吞吐量、消费者并发数、顺序性要求和后续扩展能力综合评估。

分区规划需要关注以下因素：

| 因素        | 说明                                                         |
| ----------- | ------------------------------------------------------------ |
| 消息吞吐量  | 消息量越大，通常需要更多 Partition 支撑并发读写              |
| 消费并发数  | 同一个 Consumer Group 内，最大有效并发数通常不超过 Partition 数量 |
| 顺序性要求  | 如果要求同一业务 Key 有序，需要保证相同 Key 进入同一 Partition |
| Broker 数量 | Partition 应合理分布到多个 Broker 上                         |
| 后续扩展    | 分区数可以增加，但增加后可能影响 Key 到 Partition 的映射     |
| 运维成本    | 分区过多会增加元数据、文件句柄、Leader 选举和恢复成本        |

推荐分区规划如下：

| 场景                 | 推荐分区数  | 说明                     |
| -------------------- | ----------- | ------------------------ |
| 本地开发             | `1` 或 `3`  | 便于调试和验证           |
| 普通业务事件         | `3` 到 `6`  | 满足一般异步业务处理     |
| 中高吞吐日志类消息   | `6` 到 `12` | 支持更高并发消费         |
| 大流量埋点或采集消息 | `12` 或更多 | 需要结合压测结果确定     |
| 严格全局顺序消息     | `1`         | 牺牲并发能力换取全局顺序 |

分区数量建议按以下方式评估：

```text
Topic 分区数 >= 目标 Consumer Group 内的最大有效消费者并发数
```

例如，一个订单事件 Topic 预计同一消费组最多部署 6 个消费实例并发处理，则分区数可以规划为 6。后续如果消费实例增加到 10 个，但 Topic 仍只有 6 个分区，则同一 Consumer Group 内最多只有 6 个实例能够分配到 Partition，剩余实例会空闲。

需要注意，分区数不是越多越好。分区数过多会带来以下问题：

1. Broker 维护的 Partition 元数据增多。
2. 文件句柄和磁盘目录数量增加。
3. Controller 管理和故障恢复成本增加。
4. Consumer Rebalance 时间可能变长。
5. 增加分区后，基于 Key 的分区映射可能变化，影响局部顺序。

### Topic 副本规划

副本用于提升 Kafka 消息的高可用能力。一个 Partition 可以有多个副本，分布在不同 Broker 上。Leader 副本负责读写请求，Follower 副本从 Leader 同步数据。

副本规划需要结合环境和 Broker 数量确定：

| 环境     | 推荐副本数 | 说明                            |
| -------- | ---------- | ------------------------------- |
| 本地环境 | `1`        | 单 Broker 环境只能配置 1 个副本 |
| 测试环境 | `1` 或 `2` | 根据测试 Kafka 集群规模确定     |
| 预发环境 | `3`        | 尽量贴近生产环境                |
| 生产环境 | `3`        | 常见高可用配置                  |

生产环境推荐组合如下：

| 配置项                | 推荐值 | 说明                                      |
| --------------------- | ------ | ----------------------------------------- |
| `replication.factor`  | `3`    | 每个 Partition 保留 3 个副本              |
| `min.insync.replicas` | `2`    | 至少 2 个同步副本确认                     |
| Producer `acks`       | `all`  | Producer 等待同步副本确认后才认为写入成功 |

副本规划要求如下：

1. 副本数不能大于 Broker 数量。
2. 生产环境重要 Topic 不建议使用单副本。
3. 可靠消息应配合 `acks=all` 和 `min.insync.replicas` 使用。
4. 副本数越高，数据可靠性越好，但存储成本和复制流量也会增加。
5. 本地 Docker 单节点 Kafka 中，`replication-factor` 必须设置为 `1`。

创建 Topic 时指定副本数示例：

```bash
docker exec -it ateng-kafka /opt/kafka/bin/kafka-topics.sh \
  --create \
  --topic ateng.order.created.topic \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1
```

上述命令适用于本地单节点 Kafka。生产环境如果有 3 个或更多 Broker，可以将 `--replication-factor` 调整为 `3`。

### Topic 保留策略

Topic 保留策略用于控制消息在 Kafka 中保存多久或最多占用多少空间。Kafka 消息被消费后不会立即删除，而是按照 Topic 的保留策略进行清理。

常见保留配置如下：

| 配置项            | 说明                                 |
| ----------------- | ------------------------------------ |
| `retention.ms`    | 按时间保留消息                       |
| `retention.bytes` | 按 Topic 分区大小限制保留消息        |
| `segment.ms`      | 日志段滚动时间                       |
| `segment.bytes`   | 日志段大小                           |
| `cleanup.policy`  | 清理策略，常用 `delete` 或 `compact` |

推荐保留策略如下：

| Topic 类型     | 推荐保留时间 | 说明                         |
| -------------- | ------------ | ---------------------------- |
| 普通业务事件   | 3 到 7 天    | 满足常规问题排查和短期回溯   |
| 重要业务事件   | 7 到 30 天   | 便于补偿和审计               |
| 日志采集类消息 | 1 到 3 天    | 通常下游会落库或进入数据平台 |
| 重试 Topic     | 1 到 7 天    | 根据重试周期确定             |
| 死信 Topic     | 14 到 30 天  | 便于排查、修复和重放         |
| 临时测试 Topic | 1 天以内     | 避免占用本地或测试环境磁盘   |

Topic 保留配置示例：

```bash
docker exec -it ateng-kafka /opt/kafka/bin/kafka-configs.sh \
  --bootstrap-server localhost:9092 \
  --entity-type topics \
  --entity-name ateng.order.created.topic \
  --alter \
  --add-config retention.ms=604800000,cleanup.policy=delete
```

参数说明如下：

| 参数                     | 说明                          |
| ------------------------ | ----------------------------- |
| `retention.ms=604800000` | 消息保留 7 天                 |
| `cleanup.policy=delete`  | 按保留时间或空间删除旧消息    |
| `--entity-name`          | 指定需要修改配置的 Topic 名称 |

需要注意，保留时间不是消费成功时间，而是消息写入 Kafka 后的保存时间。即使消息没有被消费，超过保留时间后也可能被清理。因此，长期未消费的 Consumer Group 可能因为消息过期而无法补齐历史数据。

### Topic 压缩策略

Topic 压缩策略主要包括日志删除和日志压缩两类。普通业务消息通常使用 `delete` 策略，状态类消息或按 Key 保留最新值的场景可以考虑 `compact` 策略。

常见策略如下：

| 策略     | 配置值                          | 说明                          |
| -------- | ------------------------------- | ----------------------------- |
| 删除策略 | `cleanup.policy=delete`         | 按时间或大小清理旧消息        |
| 压缩策略 | `cleanup.policy=compact`        | 对相同 Key 的消息保留较新的值 |
| 混合策略 | `cleanup.policy=delete,compact` | 同时启用删除和压缩能力        |

使用建议如下：

| 场景         | 推荐策略  | 说明                               |
| ------------ | --------- | ---------------------------------- |
| 订单创建事件 | `delete`  | 每条事件都需要保留一段时间         |
| 支付成功事件 | `delete`  | 属于事件流水，不适合只保留最新值   |
| 用户状态同步 | `compact` | 相同用户 ID 只关注最新状态时可使用 |
| 配置变更同步 | `compact` | 相同配置 Key 通常只关注最新值      |
| 死信消息     | `delete`  | 每条死信都需要排查，不应被压缩覆盖 |
| 日志采集     | `delete`  | 日志通常是追加型事件               |

`compact` 策略要求消息必须有明确 Key。没有 Key 的消息无法按照业务维度压缩。对于事件类 Topic，不建议随意使用 `compact`，否则可能导致历史事件被清理，只保留某个 Key 的最新消息。

配置 Topic 压缩策略示例：

```bash
docker exec -it ateng-kafka /opt/kafka/bin/kafka-configs.sh \
  --bootstrap-server localhost:9092 \
  --entity-type topics \
  --entity-name ateng.user.status.topic \
  --alter \
  --add-config cleanup.policy=compact
```

### Topic 创建方式

Topic 创建方式主要有三种：命令行创建、应用启动自动创建、运维平台或脚本创建。不同环境应选择不同方式。

创建方式对比如下：

| 创建方式       | 适用环境             | 说明                                        |
| -------------- | -------------------- | ------------------------------------------- |
| 命令行创建     | 本地、测试、运维操作 | 直接使用 Kafka 脚本创建，适合手动验证       |
| 应用自动创建   | 本地、测试           | 通过 Spring `KafkaAdmin` 和 `NewTopic` 创建 |
| 运维平台创建   | 生产                 | 通过公司中间件平台或审批流程创建            |
| 初始化脚本创建 | 测试、预发、生产     | 通过脚本统一创建和修改 Topic                |

本地命令行创建示例：

```bash
docker exec -it ateng-kafka /opt/kafka/bin/kafka-topics.sh \
  --create \
  --topic ateng.payment.success.topic \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1 \
  --config retention.ms=604800000 \
  --config cleanup.policy=delete
```

查看 Topic 详情：

```bash
docker exec -it ateng-kafka /opt/kafka/bin/kafka-topics.sh \
  --describe \
  --topic ateng.payment.success.topic \
  --bootstrap-server localhost:9092
```

删除 Topic 示例：

```bash
docker exec -it ateng-kafka /opt/kafka/bin/kafka-topics.sh \
  --delete \
  --topic ateng.payment.success.topic \
  --bootstrap-server localhost:9092
```

删除 Topic 属于高风险操作，生产环境必须通过审批和变更流程执行。删除后，Topic 中历史消息和消费进度依赖都会受到影响。

### Topic 配置初始化

Topic 配置初始化用于在本地或测试环境中自动创建 Topic，并统一设置分区数、副本数和保留策略。生产环境不建议业务应用自动创建 Topic，应通过运维流程或中间件平台进行管理。

文件位置：`src/main/resources/application.yml`

以下配置用于定义项目需要初始化的 Topic 列表。

```yaml
app:
  kafka:
    topic:
      # 是否启用 Topic 自动初始化，本地和测试环境可开启，生产环境建议关闭
      auto-create: true

      topics:
        - name: ateng.order.created.topic
          partitions: 3
          replicas: 1
          configs:
            # 普通业务消息保留 7 天
            retention.ms: "604800000"
            cleanup.policy: delete

        - name: ateng.payment.success.topic
          partitions: 3
          replicas: 1
          configs:
            # 支付成功消息保留 7 天
            retention.ms: "604800000"
            cleanup.policy: delete

        - name: ateng.common.dead-letter.topic
          partitions: 3
          replicas: 1
          configs:
            # 死信消息保留 14 天
            retention.ms: "1209600000"
            cleanup.policy: delete
```

文件位置：`src/main/java/io/github/atengk/kafka/properties/KafkaTopicProperties.java`

该配置类用于绑定 `app.kafka.topic` 下的 Topic 初始化配置。

```java
package io.github.atengk.kafka.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * Kafka Topic 配置属性
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@ConfigurationProperties(prefix = "app.kafka.topic")
public class KafkaTopicProperties {

    /**
     * 是否自动创建 Topic
     */
    private Boolean autoCreate = false;

    /**
     * Topic 列表
     */
    private List<TopicItem> topics = List.of();

    /**
     * Topic 配置项
     *
     * @author Ateng
     * @since 2026-05-11
     */
    @Data
    public static class TopicItem {

        /**
         * Topic 名称
         */
        private String name;

        /**
         * 分区数
         */
        private Integer partitions = 3;

        /**
         * 副本数
         */
        private Short replicas = 1;

        /**
         * Topic 自定义配置
         */
        private Map<String, String> configs = Map.of();

    }

}
```

文件位置：`src/main/java/io/github/atengk/kafka/config/KafkaTopicConfig.java`

该配置类用于根据配置文件中的 Topic 列表自动创建 `NewTopic` Bean。

```java
package io.github.atengk.kafka.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.kafka.properties.KafkaTopicProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.Map;

/**
 * Kafka Topic 初始化配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class KafkaTopicConfig {

    private final KafkaTopicProperties kafkaTopicProperties;

    /**
     * 初始化 Kafka Topic
     *
     * @return Topic 集合
     */
    @Bean
    @ConditionalOnProperty(prefix = "app.kafka.topic", name = "auto-create", havingValue = "true")
    public KafkaAdmin.NewTopics kafkaTopics() {
        if (CollUtil.isEmpty(kafkaTopicProperties.getTopics())) {
            log.warn("未配置Kafka Topic初始化列表");
            return new KafkaAdmin.NewTopics();
        }

        NewTopic[] topics = kafkaTopicProperties.getTopics()
                .stream()
                .filter(item -> StrUtil.isNotBlank(item.getName()))
                .map(item -> {
                    Map<String, String> configs = item.getConfigs() == null ? Map.of() : item.getConfigs();
                    log.info("初始化Kafka Topic，topic={}，partitions={}，replicas={}",
                            item.getName(), item.getPartitions(), item.getReplicas());

                    return TopicBuilder.name(item.getName())
                            .partitions(item.getPartitions())
                            .replicas(item.getReplicas())
                            .configs(configs)
                            .build();
                })
                .toArray(NewTopic[]::new);

        return new KafkaAdmin.NewTopics(topics);
    }

}
```

验证 Topic 是否创建成功：

```bash
docker exec -it ateng-kafka /opt/kafka/bin/kafka-topics.sh \
  --list \
  --bootstrap-server localhost:9092

docker exec -it ateng-kafka /opt/kafka/bin/kafka-topics.sh \
  --describe \
  --topic ateng.order.created.topic \
  --bootstrap-server localhost:9092
```

第一条命令用于查看 Topic 列表，第二条命令用于查看指定 Topic 的分区、副本和 Leader 信息。

## 消息模型设计

本章节用于定义 Kafka 消息体结构、Header、业务 Key、消息唯一标识、时间字段、版本字段和序列化模型。消息模型设计是 Kafka 开发中的关键部分，直接影响消费幂等、链路追踪、顺序性、兼容性和后续问题排查。

推荐将 Kafka 消息设计为“统一信封 + 业务载荷”的结构。统一信封承载技术字段和治理字段，业务载荷承载具体业务数据。

### 消息体结构设计

消息体结构应保持稳定、清晰、可扩展。推荐所有业务消息统一使用外层消息信封，避免每个业务 Topic 自行定义完全不同的字段结构。

推荐消息结构如下：

```json
{
  "messageId": "1900000000000000001",
  "messageType": "ORDER_CREATED",
  "version": "1.0",
  "bizKey": "ORDER_10001",
  "traceId": "trace-20260511-001",
  "source": "order-service",
  "sendTime": "2026-05-11T10:30:00",
  "payload": {
    "orderId": "10001",
    "userId": "20001",
    "amount": 199.90,
    "currency": "CNY"
  }
}
```

字段说明如下：

| 字段          | 类型            | 是否必填 | 说明                                     |
| ------------- | --------------- | -------- | ---------------------------------------- |
| `messageId`   | `String`        | 是       | 消息唯一标识，用于幂等、追踪和重放       |
| `messageType` | `String`        | 是       | 消息类型，例如 `ORDER_CREATED`           |
| `version`     | `String`        | 是       | 消息模型版本，例如 `1.0`                 |
| `bizKey`      | `String`        | 是       | 业务 Key，例如订单 ID、用户 ID、支付单号 |
| `traceId`     | `String`        | 建议必填 | 链路追踪 ID                              |
| `source`      | `String`        | 建议必填 | 消息来源服务                             |
| `sendTime`    | `LocalDateTime` | 是       | 消息发送时间                             |
| `payload`     | `Object`        | 是       | 具体业务数据                             |

文件位置：`src/main/java/io/github/atengk/kafka/model/MessageEnvelope.java`

该消息信封类用于统一包装 Kafka 业务消息，承载消息治理字段和业务载荷。

```java
package io.github.atengk.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Kafka 消息信封
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageEnvelope<T> {

    /**
     * 消息唯一标识
     */
    private String messageId;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 消息版本
     */
    private String version;

    /**
     * 业务 Key
     */
    private String bizKey;

    /**
     * 链路追踪 ID
     */
    private String traceId;

    /**
     * 消息来源服务
     */
    private String source;

    /**
     * 消息发送时间
     */
    private LocalDateTime sendTime;

    /**
     * 业务载荷
     */
    private T payload;

}
```

文件位置：`src/main/java/io/github/atengk/kafka/model/OrderCreatedMessage.java`

该业务消息类用于表示订单创建事件的业务载荷。

```java
package io.github.atengk.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单创建消息
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedMessage {

    /**
     * 订单 ID
     */
    private String orderId;

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 订单金额
     */
    private BigDecimal amount;

    /**
     * 币种
     */
    private String currency;

}
```

消息体设计要求如下：

1. 外层字段用于治理，不应混入具体业务数据。
2. `payload` 只存放业务字段，避免写入 Kafka 技术字段。
3. 金额字段应使用 `BigDecimal`，不建议使用 `double`。
4. 时间字段应统一格式，避免不同服务使用不同格式。
5. 字段新增应保持向后兼容，不建议随意删除或改变字段含义。
6. 不应在消息体中传输密码、Token、密钥、完整证件号等敏感字段。

### 消息 Header 设计

Kafka Header 用于携带轻量级元数据，适合存放 TraceId、消息类型、版本、来源服务、租户 ID、重试次数等信息。Header 不建议存放大型业务对象，也不建议替代消息体。

推荐 Header 设计如下：

| Header Key          | 说明         | 示例                  |
| ------------------- | ------------ | --------------------- |
| `x-message-id`      | 消息唯一标识 | `1900000000000000001` |
| `x-message-type`    | 消息类型     | `ORDER_CREATED`       |
| `x-message-version` | 消息版本     | `1.0`                 |
| `x-trace-id`        | 链路追踪 ID  | `trace-20260511-001`  |
| `x-source-service`  | 来源服务     | `order-service`       |
| `x-biz-key`         | 业务 Key     | `ORDER_10001`         |
| `x-retry-count`     | 重试次数     | `0`                   |
| `x-tenant-id`       | 租户 ID      | `tenant_001`          |

文件位置：`src/main/java/io/github/atengk/kafka/constant/KafkaHeaderConstant.java`

该常量类用于统一管理 Kafka Header Key，避免业务代码中硬编码 Header 名称。

```java
package io.github.atengk.kafka.constant;

/**
 * Kafka Header 常量
 *
 * @author Ateng
 * @since 2026-05-11
 */
public final class KafkaHeaderConstant {

    /**
     * 消息唯一标识
     */
    public static final String MESSAGE_ID = "x-message-id";

    /**
     * 消息类型
     */
    public static final String MESSAGE_TYPE = "x-message-type";

    /**
     * 消息版本
     */
    public static final String MESSAGE_VERSION = "x-message-version";

    /**
     * 链路追踪 ID
     */
    public static final String TRACE_ID = "x-trace-id";

    /**
     * 来源服务
     */
    public static final String SOURCE_SERVICE = "x-source-service";

    /**
     * 业务 Key
     */
    public static final String BIZ_KEY = "x-biz-key";

    /**
     * 重试次数
     */
    public static final String RETRY_COUNT = "x-retry-count";

    /**
     * 租户 ID
     */
    public static final String TENANT_ID = "x-tenant-id";

    private KafkaHeaderConstant() {
    }

}
```

Header 设计要求如下：

1. Header Key 使用小写字母和中横线。
2. Header 只存放元数据，不存放完整业务对象。
3. Header 中的字段应和消息体中的关键治理字段保持一致。
4. 重试次数、来源服务、TraceId 等字段应由统一 Producer 封装组件写入。
5. 消费端读取 Header 时需要兼容字段不存在的情况。
6. 不建议依赖 Spring JSON 类型 Header 进行跨服务反序列化，避免服务之间强绑定 Java 类型。

### 业务 Key 设计

业务 Key 是 Kafka 消息的重要字段，通常用于分区路由、顺序保证、幂等处理和问题排查。Producer 发送消息时，如果指定 Key，Kafka 会根据分区策略将相同 Key 的消息尽量发送到同一个 Partition。

业务 Key 设计建议如下：

| 业务场景 | 推荐 Key                 | 说明                                   |
| -------- | ------------------------ | -------------------------------------- |
| 订单事件 | `orderId`                | 保证同一订单事件尽量进入同一 Partition |
| 支付事件 | `paymentId` 或 `orderId` | 根据消费侧关注维度选择                 |
| 用户事件 | `userId`                 | 保证同一用户事件局部有序               |
| 设备上报 | `deviceId`               | 保证同一设备数据局部有序               |
| 库存事件 | `skuId`                  | 保证同一商品库存事件局部有序           |
| 租户消息 | `tenantId + bizId`       | 多租户场景避免 Key 过于集中            |

业务 Key 设计要求如下：

1. 重要业务消息必须设置 Key。
2. Key 应优先选择稳定的业务唯一标识。
3. 同一类事件应使用一致的 Key 规则。
4. 不建议使用随机 UUID 作为分区 Key，否则无法保证同一业务主体有序。
5. 不建议使用低基数字段作为 Key，例如状态值、类型值、固定租户 ID。
6. Key 应避免过长，通常使用业务 ID 或组合业务 ID 即可。

推荐 Key 格式如下：

```text
ORDER:{orderId}
PAYMENT:{paymentId}
USER:{userId}
SKU:{skuId}
TENANT:{tenantId}:ORDER:{orderId}
```

示例：

```text
ORDER:10001
PAYMENT:202605110001
USER:20001
TENANT:tenant_001:ORDER:10001
```

如果业务要求同一个订单的创建、支付、取消、退款事件按顺序处理，应优先使用 `orderId` 作为 Key，而不是每种事件使用不同 Key。

### 消息唯一标识

消息唯一标识用于标记一条业务消息，是幂等消费、日志追踪、异常重试、死信重放和消息查询的基础字段。每条消息在发送前必须生成全局唯一的 `messageId`。

消息 ID 生成方式建议如下：

| 方式          | 是否推荐              | 说明                         |
| ------------- | --------------------- | ---------------------------- |
| 雪花 ID       | 推荐                  | 趋势递增，适合分布式系统     |
| UUID          | 可用                  | 唯一性较好，但较长且无序     |
| 数据库自增 ID | 不建议作为通用消息 ID | 依赖数据库，跨服务不方便     |
| 业务单号      | 不建议直接作为消息 ID | 同一业务单号可能产生多条事件 |

文件位置：`src/main/java/io/github/atengk/kafka/support/KafkaMessageIdGenerator.java`

该工具类用于生成 Kafka 消息唯一标识，默认使用 Hutool 雪花 ID。

```java
package io.github.atengk.kafka.support;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

/**
 * Kafka 消息 ID 生成器
 *
 * @author Ateng
 * @since 2026-05-11
 */
public final class KafkaMessageIdGenerator {

    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake(1, 1);

    private KafkaMessageIdGenerator() {
    }

    /**
     * 生成消息 ID
     *
     * @return 消息 ID
     */
    public static String nextMessageId() {
        return SNOWFLAKE.nextIdStr();
    }

}
```

消息唯一标识使用要求如下：

1. Producer 发送消息前必须生成 `messageId`。
2. `messageId` 应同时写入消息体和 Header。
3. Consumer 幂等表应以 `messageId` 或 `messageId + consumerGroup` 作为唯一约束。
4. 死信消息必须保留原始 `messageId`，不能生成新的 ID 覆盖。
5. 消息重放时可以生成新的重放 ID，但必须保留原始消息 ID 字段，例如 `originalMessageId`。

### 消息时间字段

消息时间字段用于记录消息创建、发送、接收、消费和失败时间。合理的时间字段可以帮助排查消息延迟、消费耗时、重试间隔和死信堆积问题。

推荐时间字段如下：

| 字段          | 所属位置            | 说明                          |
| ------------- | ------------------- | ----------------------------- |
| `sendTime`    | 消息体              | Producer 创建并发送消息的时间 |
| `eventTime`   | 业务载荷            | 业务事件实际发生时间          |
| `consumeTime` | 消费记录表          | Consumer 开始处理消息的时间   |
| `successTime` | 消费记录表          | 消费成功时间                  |
| `failTime`    | 消费记录表 / 死信表 | 消费失败时间                  |
| `retryTime`   | 重试记录表          | 下一次重试时间                |

时间字段设计要求如下：

1. 消息体中的时间建议使用 ISO-8601 格式。
2. Java 模型中建议使用 `LocalDateTime` 或 `Instant`。
3. 跨时区系统建议使用 UTC 时间或明确时区字段。
4. 数据库存储建议统一使用 `datetime`、`timestamp` 或带时区时间类型。
5. 日志中必须打印消息发送时间和消费时间，便于计算消费延迟。

消息时间示例：

```json
{
  "messageId": "1900000000000000001",
  "messageType": "ORDER_CREATED",
  "sendTime": "2026-05-11T10:30:00",
  "payload": {
    "orderId": "10001",
    "eventTime": "2026-05-11T10:29:58"
  }
}
```

`sendTime` 表示消息发送到 Kafka 的时间，`eventTime` 表示业务事件真实发生时间。两者不一定完全相同。

### 消息版本字段

消息版本字段用于支持消息模型演进。随着业务发展，消息字段可能新增、废弃或调整。如果没有版本字段，消费端很难判断当前消息结构，容易导致反序列化失败或业务兼容问题。

推荐版本格式如下：

```text
1.0
1.1
2.0
```

版本变更建议如下：

| 变更类型         | 是否兼容 | 示例                                     |
| ---------------- | -------- | ---------------------------------------- |
| 新增非必填字段   | 通常兼容 | 新增 `remark` 字段                       |
| 新增有默认值字段 | 通常兼容 | 新增 `source` 字段，默认 `order-service` |
| 删除字段         | 不兼容   | 删除消费端仍依赖的 `userId`              |
| 修改字段类型     | 不兼容   | `amount` 从字符串改成数字                |
| 修改字段语义     | 不兼容   | `status` 含义发生变化                    |
| 重命名字段       | 不兼容   | `orderId` 改为 `id`                      |

版本设计要求如下：

1. 所有消息必须包含 `version` 字段。
2. 小版本升级应尽量保持向后兼容。
3. 大版本升级应创建新的消息类型或新 Topic，避免影响旧消费端。
4. 消费端应兼容旧版本消息，不能只假设最新结构。
5. 字段废弃应先标记废弃，再等待所有消费方升级后移除。

消息版本示例：

```json
{
  "messageId": "1900000000000000001",
  "messageType": "ORDER_CREATED",
  "version": "1.1",
  "bizKey": "ORDER:10001",
  "payload": {
    "orderId": "10001",
    "userId": "20001",
    "amount": 199.90,
    "currency": "CNY",
    "channel": "APP"
  }
}
```

如果 `1.1` 版本新增了 `channel` 字段，旧消费端应在字段缺失时使用默认处理逻辑，而不是直接抛出异常。

### JSON 序列化模型

JSON 是 Spring Boot 项目中最常见的 Kafka 消息序列化方式。它可读性好、调试方便、接入成本低，适合大多数普通业务消息场景。

JSON 序列化推荐配置如下：

```yaml
spring:
  kafka:
    producer:
      # Key 使用字符串序列化
      key-serializer: org.apache.kafka.common.serialization.StringSerializer

      # Value 使用 Spring Kafka JSON 序列化
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

      properties:
        # 不添加 Java 类型 Header，降低跨语言和跨服务耦合
        spring.json.add.type.headers: false

    consumer:
      # Key 使用字符串反序列化
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer

      # 使用错误处理反序列化器，避免脏数据直接阻断消费线程
      value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer

      properties:
        # 委托 JsonDeserializer 处理消息体
        spring.deserializer.value.delegate.class: org.springframework.kafka.support.serializer.JsonDeserializer

        # 限制可信反序列化包路径
        spring.json.trusted.packages: io.github.atengk.kafka.model,java.util,java.lang

        # 不依赖 Header 中的 Java 类型信息
        spring.json.use.type.headers: false

        # 默认消息信封类型
        spring.json.value.default.type: io.github.atengk.kafka.model.MessageEnvelope
```

文件位置：`src/main/java/io/github/atengk/kafka/support/KafkaMessageEnvelopeBuilder.java`

该构建类用于快速创建统一消息信封，自动补充消息 ID、版本、来源服务和发送时间。

```java
package io.github.atengk.kafka.support;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.kafka.model.MessageEnvelope;

import java.time.LocalDateTime;

/**
 * Kafka 消息信封构建器
 *
 * @author Ateng
 * @since 2026-05-11
 */
public final class KafkaMessageEnvelopeBuilder {

    private static final String DEFAULT_VERSION = "1.0";

    private KafkaMessageEnvelopeBuilder() {
    }

    /**
     * 构建消息信封
     *
     * @param messageType 消息类型
     * @param bizKey      业务 Key
     * @param traceId     链路追踪 ID
     * @param source      来源服务
     * @param payload     业务载荷
     * @param <T>         载荷类型
     * @return 消息信封
     */
    public static <T> MessageEnvelope<T> build(String messageType,
                                               String bizKey,
                                               String traceId,
                                               String source,
                                               T payload) {
        String currentTraceId = StrUtil.blankToDefault(traceId, KafkaMessageIdGenerator.nextMessageId());

        return MessageEnvelope.<T>builder()
                .messageId(KafkaMessageIdGenerator.nextMessageId())
                .messageType(messageType)
                .version(DEFAULT_VERSION)
                .bizKey(bizKey)
                .traceId(currentTraceId)
                .source(source)
                .sendTime(LocalDateTime.now())
                .payload(payload)
                .build();
    }

}
```

JSON 模型使用建议如下：

1. 普通业务系统优先使用 JSON，降低开发和排查成本。
2. JSON 字段命名应统一使用小驼峰，例如 `messageId`、`sendTime`。
3. 生产者和消费者应使用统一的消息模型依赖或明确的接口契约。
4. 消费端应允许新增字段存在，避免新增字段导致反序列化失败。
5. 不建议依赖 Java 类型 Header 完成反序列化，跨语言场景会产生耦合。
6. 对性能、带宽和强 Schema 有更高要求时，再考虑 Avro 或 Protobuf。

### Avro 或 Protobuf 模型

Avro 和 Protobuf 适合对 Schema 管理、跨语言通信、序列化性能和消息兼容性要求较高的场景。普通 Spring Boot 业务系统可以先使用 JSON，当消息规模、跨语言协作或 Schema 治理要求提升后，再引入 Avro 或 Protobuf。

Avro 与 Protobuf 对比如下：

| 方案     | 特点                                   | 适用场景                                     |
| -------- | -------------------------------------- | -------------------------------------------- |
| JSON     | 可读性强，接入简单                     | 普通业务消息、调试频繁、团队 Java 技术栈统一 |
| Avro     | Schema 明确，常与 Schema Registry 配合 | 数据平台、流处理、强 Schema 管理             |
| Protobuf | 体积小，性能好，跨语言友好             | 高性能服务间通信、多语言系统、移动端或边缘端 |

Avro Schema 示例：

```json
{
  "type": "record",
  "name": "OrderCreatedEvent",
  "namespace": "io.github.atengk.kafka.schema",
  "fields": [
    {
      "name": "messageId",
      "type": "string"
    },
    {
      "name": "messageType",
      "type": "string"
    },
    {
      "name": "version",
      "type": "string"
    },
    {
      "name": "bizKey",
      "type": "string"
    },
    {
      "name": "sendTime",
      "type": "string"
    },
    {
      "name": "orderId",
      "type": "string"
    },
    {
      "name": "userId",
      "type": "string"
    },
    {
      "name": "amount",
      "type": "string"
    }
  ]
}
```

Protobuf Schema 示例：

```protobuf
syntax = "proto3";

package io.github.atengk.kafka.schema;

option java_package = "io.github.atengk.kafka.schema";
option java_outer_classname = "OrderCreatedEventProto";

// 订单创建事件
message OrderCreatedEvent {
  // 消息唯一标识
  string message_id = 1;

  // 消息类型
  string message_type = 2;

  // 消息版本
  string version = 3;

  // 业务 Key
  string biz_key = 4;

  // 发送时间
  string send_time = 5;

  // 订单 ID
  string order_id = 6;

  // 用户 ID
  string user_id = 7;

  // 订单金额，建议使用字符串避免精度问题
  string amount = 8;
}
```

选择 Avro 或 Protobuf 时需要注意：

1. 必须建立 Schema 管理机制，不能让各业务服务自行维护不一致的 Schema。
2. 字段编号、字段类型、默认值和兼容性规则需要提前约定。
3. 消息演进必须遵循兼容规则，避免旧消费者无法解析新消息。
4. 使用 Avro 或 Protobuf 后，本地调试可读性会下降，需要配套工具查看消息内容。
5. 如果引入 Schema Registry，需要规划注册中心地址、认证、权限和发布流程。
6. 不建议在项目早期为了技术复杂度而过早引入 Avro 或 Protobuf，除非确实存在跨语言、强 Schema 或高性能要求。


## Producer 开发

本章节用于说明 Kafka Producer 在 Spring Boot 项目中的开发方式，包括 `KafkaTemplate` 使用、同步发送、异步发送、指定 Topic、指定 Partition、指定 Key、Header 设置、发送结果回调、失败处理和统一 Producer 封装组件。

在 Spring Kafka 中，`KafkaTemplate` 是发送消息的核心组件。官方文档说明，`KafkaTemplate` 对 Producer 进行了封装，并提供了多个 `send` 方法用于向指定 Topic、Key、Partition 或 `ProducerRecord` 发送消息，发送结果通常以 `CompletableFuture<SendResult<K,V>>` 返回。([Home](https://docs.spring.io/spring-kafka/reference/kafka/sending-messages.html?utm_source=chatgpt.com))

### KafkaTemplate 使用

`KafkaTemplate` 适合在 Spring Boot 项目中完成 Kafka 消息发送。简单场景可以直接注入 `KafkaTemplate` 使用，但在正式业务项目中，不建议所有业务类直接操作 `KafkaTemplate`，而应通过统一 Producer 组件封装发送逻辑。

基础使用方式如下：

```java
@Autowired
private KafkaTemplate<String, Object> kafkaTemplate;
```

常用发送方法如下：

| 方法                                                   | 说明                                                         |
| ------------------------------------------------------ | ------------------------------------------------------------ |
| `send(String topic, V data)`                           | 向指定 Topic 发送消息，不指定 Key                            |
| `send(String topic, K key, V data)`                    | 向指定 Topic 发送消息，并指定 Key                            |
| `send(String topic, Integer partition, K key, V data)` | 向指定 Topic 的指定 Partition 发送消息                       |
| `send(ProducerRecord<K, V> record)`                    | 使用 `ProducerRecord` 发送消息，可设置 Header、Partition、Timestamp |
| `send(Message<?> message)`                             | 使用 Spring Messaging Message 发送消息                       |

项目中推荐优先使用 `ProducerRecord`，因为它可以完整控制 Topic、Partition、Key、Value、Timestamp 和 Header。

### 同步发送消息

同步发送是指调用发送方法后等待 Kafka Broker 返回发送结果。同步发送适合强依赖发送结果的场景，例如业务接口必须确认消息写入 Kafka 后才返回成功。

同步发送示例：

```java
package io.github.atengk.kafka.producer;

import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.model.OrderCreatedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Kafka 同步发送示例
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaSyncSendExample {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 同步发送订单创建消息
     *
     * @param message 订单创建消息
     */
    public void sendOrderCreatedMessage(OrderCreatedMessage message) {
        String key = "ORDER:" + message.getOrderId();

        try {
            SendResult<String, Object> result = kafkaTemplate
                    .send(KafkaTopicConstant.ORDER_CREATED_TOPIC, key, message)
                    .get(Duration.ofSeconds(10).toMillis(), TimeUnit.MILLISECONDS);

            log.info("Kafka消息同步发送成功，topic={}，partition={}，offset={}，key={}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset(),
                    key);
        } catch (Exception e) {
            log.error("Kafka消息同步发送失败，topic={}，key={}", KafkaTopicConstant.ORDER_CREATED_TOPIC, key, e);
            throw new KafkaSendException("Kafka消息同步发送失败", e);
        }
    }

}
```

同步发送的优点是调用方可以明确知道消息是否发送成功，缺点是会阻塞当前线程。如果接口并发较高，频繁同步等待会降低接口吞吐量。因此，非强依赖发送结果的场景优先使用异步发送。

### 异步发送消息

异步发送是 Producer 开发中的常用方式。调用 `send` 后立即返回 `CompletableFuture`，业务线程不需要阻塞等待 Broker 响应，可以通过回调处理发送成功或失败。

异步发送示例：

```java
package io.github.atengk.kafka.producer;

import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.model.OrderCreatedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka 异步发送示例
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaAsyncSendExample {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 异步发送订单创建消息
     *
     * @param message 订单创建消息
     */
    public void sendOrderCreatedMessage(OrderCreatedMessage message) {
        String key = "ORDER:" + message.getOrderId();

        kafkaTemplate.send(KafkaTopicConstant.ORDER_CREATED_TOPIC, key, message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka消息异步发送失败，topic={}，key={}", KafkaTopicConstant.ORDER_CREATED_TOPIC, key, ex);
                        return;
                    }

                    log.info("Kafka消息异步发送成功，topic={}，partition={}，offset={}，key={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            key);
                });
    }

}
```

异步发送适合大多数业务通知、事件分发、日志采集和数据同步场景。需要注意，异步发送不是不处理结果，而是将结果处理从主业务线程转移到回调逻辑中。

### 指定 Topic 发送

指定 Topic 发送是 Producer 最基础的使用方式。Topic 应优先使用常量类管理，避免在业务代码中硬编码字符串。

示例：

```java
kafkaTemplate.send(KafkaTopicConstant.ORDER_CREATED_TOPIC, orderCreatedMessage);
```

在正式项目中，Topic 名称应来自以下位置之一：

| 方式       | 说明                                  |
| ---------- | ------------------------------------- |
| 常量类     | 适合固定业务 Topic                    |
| 配置文件   | 适合不同环境 Topic 名称存在差异的场景 |
| 枚举类     | 适合 Topic 与消息类型强关联的场景     |
| 数据库配置 | 适合消息平台化或动态路由场景          |

推荐使用常量类：

```java
package io.github.atengk.kafka.constant;

/**
 * Kafka Topic 常量
 *
 * @author Ateng
 * @since 2026-05-11
 */
public final class KafkaTopicConstant {

    /**
     * 订单创建 Topic
     */
    public static final String ORDER_CREATED_TOPIC = "ateng.order.created.topic";

    /**
     * 支付成功 Topic
     */
    public static final String PAYMENT_SUCCESS_TOPIC = "ateng.payment.success.topic";

    /**
     * 通用死信 Topic
     */
    public static final String DEAD_LETTER_TOPIC = "ateng.common.dead-letter.topic";

    private KafkaTopicConstant() {
    }

}
```

### 指定 Partition 发送

指定 Partition 发送可以将消息写入某个明确的分区。它适合特殊路由、测试验证或强控制分区的场景，但普通业务不建议频繁手动指定 Partition。

示例：

```java
Integer partition = 0;
String key = "ORDER:10001";

kafkaTemplate.send(
        KafkaTopicConstant.ORDER_CREATED_TOPIC,
        partition,
        key,
        orderCreatedMessage
);
```

指定 Partition 时需要注意：

1. Partition 编号从 `0` 开始。
2. 指定的 Partition 必须存在，否则发送会失败。
3. 手动指定 Partition 会绕过默认分区策略。
4. 如果业务已经通过 Key 保证局部顺序，不建议再手动指定 Partition。
5. Topic 扩容后，硬编码 Partition 的逻辑可能需要调整。

指定 Partition 更多适合测试和特殊治理场景。普通业务消息建议指定 Key，由 Kafka 分区器根据 Key 决定目标 Partition。

### 指定 Key 发送

Key 是 Kafka Producer 开发中的重要参数。指定 Key 后，相同 Key 的消息通常会进入同一个 Partition，从而实现同一业务主体的局部顺序。

示例：

```java
String key = "ORDER:" + orderCreatedMessage.getOrderId();

kafkaTemplate.send(
        KafkaTopicConstant.ORDER_CREATED_TOPIC,
        key,
        orderCreatedMessage
);
```

常见 Key 设计如下：

| 场景           | 推荐 Key                            |
| -------------- | ----------------------------------- |
| 订单事件       | `ORDER:{orderId}`                   |
| 支付事件       | `PAYMENT:{paymentId}`               |
| 用户事件       | `USER:{userId}`                     |
| 库存事件       | `SKU:{skuId}`                       |
| 多租户订单事件 | `TENANT:{tenantId}:ORDER:{orderId}` |

Key 使用要求如下：

1. 重要业务消息必须设置 Key。
2. 同一业务主体需要有序时，必须使用稳定一致的 Key。
3. 不建议使用随机 UUID 作为分区 Key。
4. 不建议使用状态、类型、固定租户 ID 等低基数字段作为 Key。
5. 日志中应打印 Key，便于排查消息路由和消费问题。

### Header 设置

Header 用于携带消息元数据，例如消息 ID、消息类型、版本、TraceId、来源服务、重试次数等。设置 Header 时推荐使用 `ProducerRecord`。

示例：

```java
package io.github.atengk.kafka.producer;

import io.github.atengk.kafka.constant.KafkaHeaderConstant;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.model.OrderCreatedMessage;
import io.github.atengk.kafka.support.KafkaMessageIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Kafka Header 发送示例
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaHeaderSendExample {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 发送带 Header 的订单创建消息
     *
     * @param message 订单创建消息
     */
    public void sendWithHeader(OrderCreatedMessage message) {
        String messageId = KafkaMessageIdGenerator.nextMessageId();
        String key = "ORDER:" + message.getOrderId();

        ProducerRecord<String, Object> record = new ProducerRecord<>(
                KafkaTopicConstant.ORDER_CREATED_TOPIC,
                key,
                message
        );

        record.headers().add(new RecordHeader(KafkaHeaderConstant.MESSAGE_ID, messageId.getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader(KafkaHeaderConstant.MESSAGE_TYPE, "ORDER_CREATED".getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader(KafkaHeaderConstant.MESSAGE_VERSION, "1.0".getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader(KafkaHeaderConstant.BIZ_KEY, key.getBytes(StandardCharsets.UTF_8)));

        kafkaTemplate.send(record)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka带Header消息发送失败，topic={}，key={}，messageId={}",
                                KafkaTopicConstant.ORDER_CREATED_TOPIC, key, messageId, ex);
                        return;
                    }

                    log.info("Kafka带Header消息发送成功，topic={}，partition={}，offset={}，key={}，messageId={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            key,
                            messageId);
                });
    }

}
```

Header 设置要求如下：

| Header              | 是否建议     | 说明                         |
| ------------------- | ------------ | ---------------------------- |
| `x-message-id`      | 必须         | 用于消息幂等、追踪和死信重放 |
| `x-message-type`    | 必须         | 用于识别消息类型             |
| `x-message-version` | 必须         | 用于消息模型兼容             |
| `x-trace-id`        | 建议         | 用于链路追踪                 |
| `x-source-service`  | 建议         | 用于识别消息来源             |
| `x-biz-key`         | 建议         | 用于业务排查                 |
| `x-retry-count`     | 重试场景必填 | 用于记录重试次数             |

### 发送结果回调

发送结果回调用于处理 Kafka 返回的发送结果。结果中包含 Topic、Partition、Offset、Timestamp 等信息，这些信息应记录到日志或消息发送记录表中。

回调处理示例：

```java
kafkaTemplate.send(record)
        .whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Kafka消息发送失败，topic={}，key={}", record.topic(), record.key(), ex);
                return;
            }

            log.info("Kafka消息发送成功，topic={}，partition={}，offset={}，timestamp={}，key={}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset(),
                    result.getRecordMetadata().timestamp(),
                    record.key());
        });
```

发送成功后建议记录以下字段：

| 字段        | 说明                      |
| ----------- | ------------------------- |
| `topic`     | 消息写入的 Topic          |
| `partition` | 消息写入的 Partition      |
| `offset`    | 消息在 Partition 中的位置 |
| `key`       | 消息 Key                  |
| `messageId` | 消息唯一标识              |
| `traceId`   | 链路追踪 ID               |
| `timestamp` | Kafka 记录时间戳          |

如果系统需要消息发送审计，可以将发送结果写入消息发送记录表。普通系统至少需要记录结构化日志，便于按 `messageId`、`key` 和 `traceId` 查询。

### 发送失败处理

发送失败处理用于应对 Kafka 不可用、网络异常、认证失败、序列化失败、Topic 不存在、请求超时等问题。发送失败不能简单忽略，否则会导致业务事件丢失。

常见失败类型如下：

| 失败类型      | 说明                         | 处理建议                                |
| ------------- | ---------------------------- | --------------------------------------- |
| Broker 不可用 | Kafka 集群不可连接           | 记录错误，触发告警，必要时降级          |
| Topic 不存在  | 目标 Topic 未创建            | 本地可自动创建，生产应走 Topic 申请流程 |
| 序列化失败    | 消息对象无法序列化           | 直接失败，修复消息模型                  |
| 请求超时      | Broker 响应超时              | 依赖 Producer 重试，超过超时后失败      |
| 认证失败      | SASL / SSL 配置错误          | 直接失败，检查配置和权限                |
| 消息过大      | 超过 Broker 或 Producer 限制 | 优化消息体，不应传输大对象              |

失败处理建议如下：

1. 可重试异常交给 Producer 自身重试机制处理。
2. 超过 Producer 超时时间仍失败时，记录失败日志。
3. 重要业务消息发送失败后，应写入本地消息表或补偿任务表。
4. 不可恢复异常不应无限重试，例如序列化失败、权限失败、Topic 配置错误。
5. 失败日志必须包含 Topic、Key、messageId、traceId 和异常原因。
6. 不应在异常日志中打印完整敏感消息体。

自定义发送异常示例：

```java
package io.github.atengk.kafka.producer;

/**
 * Kafka 消息发送异常
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class KafkaSendException extends RuntimeException {

    /**
     * 构造 Kafka 发送异常
     *
     * @param message 异常信息
     */
    public KafkaSendException(String message) {
        super(message);
    }

    /**
     * 构造 Kafka 发送异常
     *
     * @param message 异常信息
     * @param cause   原始异常
     */
    public KafkaSendException(String message, Throwable cause) {
        super(message, cause);
    }

}
```

### Producer 封装组件

Producer 封装组件用于统一消息发送入口，避免业务代码散落使用 `KafkaTemplate`。封装组件应统一完成参数校验、Header 设置、同步发送、异步发送、发送结果转换、日志记录和异常处理。

推荐文件结构如下：

```text
src/main/java/io/github/atengk/kafka/producer
├── KafkaSendRequest.java
├── KafkaSendResult.java
├── KafkaMessageProducer.java
├── DefaultKafkaMessageProducer.java
└── KafkaSendException.java
```

文件位置：`src/main/java/io/github/atengk/kafka/producer/KafkaSendRequest.java`

该请求对象用于封装 Kafka 消息发送参数，包括 Topic、Key、Partition、Header、消息体和同步等待超时时间。

```java
package io.github.atengk.kafka.producer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 消息发送请求
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaSendRequest<T> {

    /**
     * Topic 名称
     */
    private String topic;

    /**
     * 分区编号，不指定时由 Kafka 分区器决定
     */
    private Integer partition;

    /**
     * 消息 Key
     */
    private String key;

    /**
     * 消息唯一标识
     */
    private String messageId;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 消息版本
     */
    private String version;

    /**
     * 链路追踪 ID
     */
    private String traceId;

    /**
     * 来源服务
     */
    private String sourceService;

    /**
     * 自定义 Header
     */
    @Builder.Default
    private Map<String, String> headers = new HashMap<>();

    /**
     * 消息体
     */
    private T payload;

    /**
     * 同步发送等待超时时间
     */
    @Builder.Default
    private Duration timeout = Duration.ofSeconds(10);

}
```

文件位置：`src/main/java/io/github/atengk/kafka/producer/KafkaSendResult.java`

该结果对象用于封装 Kafka 发送成功后的元数据，便于业务层记录发送结果。

```java
package io.github.atengk.kafka.producer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka 消息发送结果
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaSendResult {

    /**
     * 消息唯一标识
     */
    private String messageId;

    /**
     * Topic 名称
     */
    private String topic;

    /**
     * 分区编号
     */
    private Integer partition;

    /**
     * Offset
     */
    private Long offset;

    /**
     * Kafka 记录时间戳
     */
    private Long timestamp;

    /**
     * 消息 Key
     */
    private String key;

}
```

文件位置：`src/main/java/io/github/atengk/kafka/producer/KafkaMessageProducer.java`

该接口用于定义统一 Producer 发送能力，支持同步发送和异步发送。

```java
package io.github.atengk.kafka.producer;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka 消息生产者
 *
 * @author Ateng
 * @since 2026-05-11
 */
public interface KafkaMessageProducer {

    /**
     * 同步发送消息
     *
     * @param request 发送请求
     * @return 发送结果
     */
    KafkaSendResult sendSync(KafkaSendRequest<?> request);

    /**
     * 异步发送消息
     *
     * @param request 发送请求
     * @return 发送结果 Future
     */
    CompletableFuture<KafkaSendResult> sendAsync(KafkaSendRequest<?> request);

}
```

文件位置：`src/main/java/io/github/atengk/kafka/producer/DefaultKafkaMessageProducer.java`

该实现类基于 `KafkaTemplate` 封装统一发送逻辑，自动设置 Header、记录发送日志并处理异常。

```java
package io.github.atengk.kafka.producer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.kafka.constant.KafkaHeaderConstant;
import io.github.atengk.kafka.support.KafkaMessageIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Kafka 默认消息生产者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultKafkaMessageProducer implements KafkaMessageProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 同步发送消息
     *
     * @param request 发送请求
     * @return 发送结果
     */
    @Override
    public KafkaSendResult sendSync(KafkaSendRequest<?> request) {
        try {
            Duration timeout = request.getTimeout() == null ? Duration.ofSeconds(10) : request.getTimeout();
            return sendAsync(request).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("Kafka消息同步发送异常，topic={}，key={}，messageId={}",
                    request.getTopic(), request.getKey(), request.getMessageId(), e);
            throw new KafkaSendException("Kafka消息同步发送失败", e);
        }
    }

    /**
     * 异步发送消息
     *
     * @param request 发送请求
     * @return 发送结果 Future
     */
    @Override
    public CompletableFuture<KafkaSendResult> sendAsync(KafkaSendRequest<?> request) {
        validateRequest(request);
        fillDefaultValue(request);

        ProducerRecord<String, Object> record = buildProducerRecord(request);

        return kafkaTemplate.send(record)
                .thenApply(sendResult -> {
                    KafkaSendResult result = convertSendResult(request, sendResult);
                    log.info("Kafka消息发送成功，topic={}，partition={}，offset={}，key={}，messageId={}",
                            result.getTopic(),
                            result.getPartition(),
                            result.getOffset(),
                            result.getKey(),
                            result.getMessageId());
                    return result;
                })
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka消息发送失败，topic={}，key={}，messageId={}",
                                request.getTopic(), request.getKey(), request.getMessageId(), ex);
                    }
                });
    }

    /**
     * 校验发送请求
     *
     * @param request 发送请求
     */
    private void validateRequest(KafkaSendRequest<?> request) {
        if (request == null) {
            throw new KafkaSendException("Kafka发送请求不能为空");
        }
        if (StrUtil.isBlank(request.getTopic())) {
            throw new KafkaSendException("Kafka Topic不能为空");
        }
        if (request.getPayload() == null) {
            throw new KafkaSendException("Kafka消息体不能为空");
        }
    }

    /**
     * 填充默认字段
     *
     * @param request 发送请求
     */
    private void fillDefaultValue(KafkaSendRequest<?> request) {
        if (StrUtil.isBlank(request.getMessageId())) {
            request.setMessageId(KafkaMessageIdGenerator.nextMessageId());
        }
        if (StrUtil.isBlank(request.getVersion())) {
            request.setVersion("1.0");
        }
    }

    /**
     * 构建 ProducerRecord
     *
     * @param request 发送请求
     * @return ProducerRecord
     */
    private ProducerRecord<String, Object> buildProducerRecord(KafkaSendRequest<?> request) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(
                request.getTopic(),
                request.getPartition(),
                request.getKey(),
                request.getPayload()
        );

        Headers headers = record.headers();
        addHeader(headers, KafkaHeaderConstant.MESSAGE_ID, request.getMessageId());
        addHeader(headers, KafkaHeaderConstant.MESSAGE_TYPE, request.getMessageType());
        addHeader(headers, KafkaHeaderConstant.MESSAGE_VERSION, request.getVersion());
        addHeader(headers, KafkaHeaderConstant.TRACE_ID, request.getTraceId());
        addHeader(headers, KafkaHeaderConstant.SOURCE_SERVICE, request.getSourceService());
        addHeader(headers, KafkaHeaderConstant.BIZ_KEY, request.getKey());

        if (CollUtil.isNotEmpty(request.getHeaders())) {
            for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
                addHeader(headers, entry.getKey(), entry.getValue());
            }
        }

        return record;
    }

    /**
     * 添加 Header
     *
     * @param headers Header 集合
     * @param key     Header Key
     * @param value   Header Value
     */
    private void addHeader(Headers headers, String key, String value) {
        if (StrUtil.isBlank(key) || value == null) {
            return;
        }
        headers.add(new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * 转换发送结果
     *
     * @param request    发送请求
     * @param sendResult Spring Kafka 发送结果
     * @return 统一发送结果
     */
    private KafkaSendResult convertSendResult(KafkaSendRequest<?> request, SendResult<String, Object> sendResult) {
        return KafkaSendResult.builder()
                .messageId(request.getMessageId())
                .topic(sendResult.getRecordMetadata().topic())
                .partition(sendResult.getRecordMetadata().partition())
                .offset(sendResult.getRecordMetadata().offset())
                .timestamp(sendResult.getRecordMetadata().timestamp())
                .key(request.getKey())
                .build();
    }

}
```

业务调用示例：

```java
KafkaSendRequest<OrderCreatedMessage> request = KafkaSendRequest.<OrderCreatedMessage>builder()
        .topic(KafkaTopicConstant.ORDER_CREATED_TOPIC)
        .key("ORDER:" + message.getOrderId())
        .messageType("ORDER_CREATED")
        .traceId("trace-20260511-001")
        .sourceService("order-service")
        .payload(message)
        .build();

kafkaMessageProducer.sendAsync(request);
```

项目中建议 Controller、Service 或领域事件发布器只依赖 `KafkaMessageProducer` 接口，不直接依赖 `KafkaTemplate`。这样后续需要增加发送记录表、失败补偿、统一埋点、熔断降级或消息审计时，可以在 Producer 封装层统一扩展。

## Producer 可靠性设计

本章节用于说明 Producer 侧的可靠性配置，包括 Acknowledgment、Retries、Idempotence、Delivery Timeout、Request Timeout、In Flight Requests、消息压缩、批量发送和发送顺序保证。

Producer 可靠性不是单一参数决定的，而是由 `acks`、`retries`、`enable.idempotence`、`delivery.timeout.ms`、`request.timeout.ms`、`max.in.flight.requests.per.connection`、Topic 副本数和 `min.insync.replicas` 共同决定。Apache Kafka 官方配置说明中也明确指出，重试会受到 `delivery.timeout.ms` 限制；开启幂等要求相关配置满足约束，包括 `acks=all`、`retries>0` 和 `max.in.flight.requests.per.connection<=5`。([Apache Kafka](https://kafka.apache.org/36/configuration/producer-configs/?utm_source=chatgpt.com))

### Acknowledgment 配置

Acknowledgment 配置对应 Producer 的 `acks` 参数，用于控制 Producer 发送消息后等待 Broker 确认的级别。

常见配置如下：

| 配置值     | 说明                        | 可靠性 | 性能 |
| ---------- | --------------------------- | ------ | ---- |
| `acks=0`   | Producer 不等待 Broker 确认 | 最低   | 最高 |
| `acks=1`   | Leader 写入成功即确认       | 中等   | 较高 |
| `acks=all` | 等待所有要求的同步副本确认  | 最高   | 较低 |

生产环境重要业务消息推荐使用：

```yaml
spring:
  kafka:
    producer:
      # 等待所有同步副本确认后才认为写入成功
      acks: all
```

使用 `acks=all` 时，建议 Topic 同时配置：

```text
replication.factor=3
min.insync.replicas=2
```

这样可以避免消息只写入单个副本就返回成功。对于订单、支付、库存、账户、审计等重要业务事件，`acks=all` 应作为默认选择。

### Retries 配置

`retries` 用于控制 Producer 发送失败后的重试次数。它适合处理短暂网络抖动、Broker 短暂不可用、Leader 切换等临时故障。

推荐配置：

```yaml
spring:
  kafka:
    producer:
      # 发送失败自动重试次数
      retries: 3
```

需要注意，`retries` 并不是无限保证成功。Producer 是否继续重试，还会受到 `delivery.timeout.ms` 的整体发送超时时间限制。即使重试次数没有耗尽，只要超过 `delivery.timeout.ms`，发送也会失败。([Apache Kafka](https://kafka.apache.org/36/configuration/producer-configs/?utm_source=chatgpt.com))

配置建议如下：

| 场景         | 建议                                                 |
| ------------ | ---------------------------------------------------- |
| 普通业务消息 | `retries=3`                                          |
| 重要业务消息 | `retries=5` 或更高，并配合本地消息表                 |
| 日志采集消息 | 可适当降低重试，避免阻塞                             |
| 不可恢复错误 | 不依赖重试，例如序列化失败、认证失败、Topic 权限不足 |

Producer 重试只能解决发送到 Kafka 之前的问题，不能解决 Consumer 业务处理失败的问题。Consumer 失败需要通过消费重试、死信 Topic 和补偿机制处理。

### Idempotence 配置

`enable.idempotence` 用于开启 Producer 幂等发送。开启后，Producer 可以降低因网络异常和重试导致的重复写入风险。Kafka 官方配置说明中提到，启用幂等时需要满足 `acks=all`、`retries>0`、`max.in.flight.requests.per.connection<=5` 等条件。([Apache Kafka](https://kafka.apache.org/33/configuration/producer-configs/?utm_source=chatgpt.com))

推荐配置：

```yaml
spring:
  kafka:
    producer:
      acks: all
      retries: 3
      properties:
        # 开启 Producer 幂等发送
        enable.idempotence: true

        # 幂等发送要求不超过 5
        max.in.flight.requests.per.connection: 5
```

幂等 Producer 需要注意：

1. Producer 幂等只保证单个 Producer 会话内对同一 Partition 的重复写入控制。
2. Producer 重启后，仍然可能因为业务重发产生重复业务消息。
3. Producer 幂等不能替代 Consumer 幂等。
4. Consumer 端仍必须基于 `messageId` 或业务唯一键做幂等处理。
5. 对跨分区、跨会话、跨系统的业务幂等，仍需依赖数据库唯一约束、状态表或 Redis 去重。

### Delivery Timeout 配置

`delivery.timeout.ms` 表示 Producer 发送一条消息的完整生命周期超时时间。它覆盖从消息进入 Producer 缓冲区、等待批量发送、发送请求、失败重试到最终成功或失败的整体时间。

推荐配置：

```yaml
spring:
  kafka:
    producer:
      properties:
        # 消息发送完整生命周期最大超时时间
        delivery.timeout.ms: 120000
```

配置建议如下：

| 场景           | 建议值                 |
| -------------- | ---------------------- |
| 普通业务消息   | `120000`               |
| 延迟敏感接口   | `30000` 到 `60000`     |
| 高可靠异步消息 | `120000` 或更高        |
| 日志采集类消息 | 根据吞吐和延迟要求调整 |

`delivery.timeout.ms` 不能设置得过低，否则短暂抖动就可能导致发送失败；也不能设置得过高，否则发送失败反馈太慢，不利于接口快速降级和告警。

### Request Timeout 配置

`request.timeout.ms` 表示 Producer 单次请求等待 Broker 响应的超时时间。它控制的是单次请求维度，而 `delivery.timeout.ms` 控制的是消息完整发送生命周期。

推荐配置：

```yaml
spring:
  kafka:
    producer:
      properties:
        # 单次请求等待 Broker 响应的超时时间
        request.timeout.ms: 30000
```

`request.timeout.ms` 与 `delivery.timeout.ms` 的区别如下：

| 参数                  | 作用范围 | 说明                                         |
| --------------------- | -------- | -------------------------------------------- |
| `request.timeout.ms`  | 单次请求 | 控制 Producer 等待 Broker 响应的时间         |
| `delivery.timeout.ms` | 整体发送 | 控制消息从进入缓冲区到最终成功或失败的总时间 |

一般情况下，`delivery.timeout.ms` 应大于 `request.timeout.ms`，否则重试空间会很小。

### In Flight Requests 配置

`max.in.flight.requests.per.connection` 用于控制 Producer 在单个连接上未收到响应的最大请求数。该参数会影响吞吐、重试和顺序性。

推荐配置：

```yaml
spring:
  kafka:
    producer:
      properties:
        # 开启幂等时建议不超过 5
        max.in.flight.requests.per.connection: 5
```

顺序性风险说明：

如果没有开启幂等，并且 `retries>0`，同时 `max.in.flight.requests.per.connection>1`，当第一批消息发送失败但第二批消息发送成功时，重试后的第一批消息可能出现在第二批消息之后，从而导致同一 Partition 内消息顺序异常。Kafka 官方配置说明也明确提到，这类组合可能改变记录顺序。([Apache Kafka](https://kafka.apache.org/36/configuration/producer-configs/?utm_source=chatgpt.com))

配置建议如下：

| 场景                 | 建议                                                         |
| -------------------- | ------------------------------------------------------------ |
| 普通可靠消息         | `enable.idempotence=true`，`max.in.flight.requests.per.connection=5` |
| 强顺序消息           | 可设置为 `1`，但吞吐会下降                                   |
| 高吞吐非强顺序消息   | 可使用默认或适度调优                                         |
| 未开启幂等但开启重试 | 不建议将该值设置得过大                                       |

### 消息压缩配置

消息压缩用于降低网络传输量和 Broker 存储成本。Kafka Producer 支持多种压缩类型，常见包括 `none`、`gzip`、`snappy`、`lz4` 和 `zstd`。

推荐配置：

```yaml
spring:
  kafka:
    producer:
      properties:
        # 通用业务场景推荐 lz4，兼顾压缩率和性能
        compression.type: lz4
```

压缩类型选择建议如下：

| 压缩类型 | 特点                   | 适用场景                 |
| -------- | ---------------------- | ------------------------ |
| `none`   | 不压缩，CPU 消耗低     | 消息量小、延迟敏感       |
| `gzip`   | 压缩率高，CPU 消耗较高 | 带宽敏感、吞吐不是极高   |
| `snappy` | 压缩和解压速度较快     | 通用场景                 |
| `lz4`    | 性能较好，延迟较低     | 推荐通用业务使用         |
| `zstd`   | 压缩率和性能较均衡     | 大流量场景，可压测后使用 |

压缩使用要求如下：

1. 压缩适合消息量较大、消息体重复字段较多的场景。
2. 小消息量或极低延迟场景可以不启用压缩。
3. 压缩会增加 Producer 和 Consumer 的 CPU 消耗。
4. 是否启用压缩应通过压测验证，不应只凭经验配置。
5. JSON 消息通常字段名重复较多，启用压缩收益较明显。

### 批量发送配置

Kafka Producer 默认会对消息进行批量聚合后发送。批量发送可以提升吞吐量，但也可能增加少量延迟。主要参数包括 `batch.size`、`linger.ms` 和 `buffer.memory`。

推荐配置：

```yaml
spring:
  kafka:
    producer:
      properties:
        # 单个批次大小，单位字节
        batch.size: 32768

        # 批次等待时间，适当增加可提升聚合效果
        linger.ms: 20

        # Producer 总缓冲区大小
        buffer.memory: 33554432
```

参数说明如下：

| 参数            | 说明                        | 建议                        |
| --------------- | --------------------------- | --------------------------- |
| `batch.size`    | 单个 Partition 批次缓冲大小 | 普通业务可设置 `32768`      |
| `linger.ms`     | 等待更多消息组成批次的时间  | 普通业务可设置 `10` 到 `20` |
| `buffer.memory` | Producer 可用总缓冲区大小   | 默认通常够用，大流量可调大  |

场景建议如下：

| 场景         | 配置建议                                   |
| ------------ | ------------------------------------------ |
| 接口实时通知 | 降低 `linger.ms`，减少发送延迟             |
| 日志采集     | 增大 `batch.size` 和 `linger.ms`，提升吞吐 |
| 大流量事件流 | 结合压缩、批量和分区数量一起压测           |
| 低频业务消息 | 不需要过度调优，保持默认或较小调整         |

批量发送不是指业务一次调用发送多条消息，而是 Producer 客户端内部将多条消息合并成批次发送。业务侧仍可以逐条调用发送接口，Producer 会根据配置自动聚合。

### 发送顺序保证

Producer 发送顺序保证需要同时考虑 Key、Partition、重试、幂等和 In Flight Requests。Kafka 只能保证同一个 Partition 内的消息顺序，不能保证跨 Partition 全局顺序。

顺序消息推荐设计如下：

| 要求             | 设计方式                          |
| ---------------- | --------------------------------- |
| 同一订单有序     | 使用 `ORDER:{orderId}` 作为 Key   |
| 同一用户有序     | 使用 `USER:{userId}` 作为 Key     |
| 同一商品库存有序 | 使用 `SKU:{skuId}` 作为 Key       |
| 全局有序         | Topic 只设置 1 个 Partition       |
| 发送重试不乱序   | 开启幂等，控制 In Flight Requests |
| 消费不乱序       | 同一 Key 的消息避免并发处理冲突   |

推荐配置：

```yaml
spring:
  kafka:
    producer:
      acks: all
      retries: 3
      properties:
        # 开启幂等，降低重试乱序和重复写入风险
        enable.idempotence: true

        # 普通顺序场景使用 5，极强顺序要求可改为 1
        max.in.flight.requests.per.connection: 5
```

强顺序场景可以进一步配置：

```yaml
spring:
  kafka:
    producer:
      acks: all
      retries: 3
      properties:
        # 强顺序场景可降低未确认请求数量，但吞吐会下降
        max.in.flight.requests.per.connection: 1
```

顺序保证要求如下：

1. 同一业务主体必须使用相同 Key。
2. 不要随机指定 Partition。
3. 不要在后续版本中随意改变 Key 规则。
4. 增加 Topic 分区数前，需要评估 Key 到 Partition 映射变化。
5. Producer 侧保证发送顺序后，Consumer 侧仍需要避免同一业务 Key 并发处理导致业务乱序。
6. 对全局强顺序要求极高的场景，应评估是否真的适合 Kafka，因为单 Partition 会限制吞吐能力。


## Consumer 开发

本章节用于说明 Kafka Consumer 在 Spring Boot 项目中的开发方式，包括 `@KafkaListener` 使用、单 Topic 消费、多 Topic 消费、Consumer Group 配置、并发消费、批量消费、Header 读取、Key 读取、`ConsumerRecord` 使用和统一 Consumer 封装组件。

Consumer 是 Kafka 消息处理链路中的核心部分。相比 Producer，Consumer 更需要重点关注业务幂等、异常处理、Offset 提交、重复消费、死信处理和消费性能。项目中不建议将复杂业务逻辑直接堆在监听方法中，而应将监听、解析、幂等、业务处理和异常处理拆分清楚。

### KafkaListener 使用

`@KafkaListener` 是 Spring Kafka 中最常用的消息监听方式。它可以声明监听的 Topic、Consumer Group、监听容器工厂和并发参数。Spring Boot 启动后，会自动创建监听容器并持续拉取 Kafka 消息。

基础监听示例：

```java
package io.github.atengk.kafka.consumer;

import io.github.atengk.kafka.constant.KafkaGroupConstant;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.model.OrderCreatedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

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
     * @param message 订单创建消息
     */
    @KafkaListener(
            topics = KafkaTopicConstant.ORDER_CREATED_TOPIC,
            groupId = KafkaGroupConstant.ORDER_CONSUMER_GROUP
    )
    public void consumeOrderCreated(OrderCreatedMessage message) {
        log.info("收到订单创建消息，orderId={}，userId={}", message.getOrderId(), message.getUserId());

        // 执行业务处理，例如积分发放、数据同步、统计计算等
    }

}
```

`@KafkaListener` 常用属性如下：

| 属性               | 说明                          |
| ------------------ | ----------------------------- |
| `topics`           | 指定监听的 Topic              |
| `topicPattern`     | 按正则表达式匹配 Topic        |
| `groupId`          | 指定 Consumer Group           |
| `containerFactory` | 指定监听容器工厂              |
| `concurrency`      | 指定当前监听器并发数          |
| `id`               | 指定监听器 ID，便于管理和监控 |
| `autoStartup`      | 是否随应用启动自动启动监听器  |

基础监听适合简单业务验证。正式项目中，建议监听方法接收 `ConsumerRecord` 和 `Acknowledgment`，这样可以读取 Topic、Partition、Offset、Key、Header，并在业务处理成功后手动提交 Offset。

### 单 Topic 消费

单 Topic 消费用于监听一种明确的业务事件，例如订单创建、支付成功、用户登录等。单 Topic 消费的业务边界清晰，便于配置独立 Consumer Group、独立异常处理和独立监控指标。

推荐写法如下：

```java
package io.github.atengk.kafka.consumer;

import io.github.atengk.kafka.constant.KafkaGroupConstant;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.model.OrderCreatedMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 订单创建消息消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class OrderCreatedConsumer {

    /**
     * 消费订单创建消息
     *
     * @param record         Kafka 消息记录
     * @param acknowledgment Offset 确认对象
     */
    @KafkaListener(
            topics = KafkaTopicConstant.ORDER_CREATED_TOPIC,
            groupId = KafkaGroupConstant.ORDER_CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, OrderCreatedMessage> record,
                        Acknowledgment acknowledgment) {
        try {
            OrderCreatedMessage message = record.value();

            log.info("开始消费订单创建消息，topic={}，partition={}，offset={}，key={}，orderId={}",
                    record.topic(), record.partition(), record.offset(), record.key(), message.getOrderId());

            // 业务处理成功后再提交 Offset
            acknowledgment.acknowledge();

            log.info("订单创建消息消费成功，topic={}，partition={}，offset={}，key={}，orderId={}",
                    record.topic(), record.partition(), record.offset(), record.key(), message.getOrderId());
        } catch (Exception e) {
            log.error("订单创建消息消费失败，topic={}，partition={}，offset={}，key={}",
                    record.topic(), record.partition(), record.offset(), record.key(), e);
            throw e;
        }
    }

}
```

单 Topic 消费建议遵循以下规范：

1. 一个监听方法只处理一种业务事件。
2. 监听方法名称应体现业务含义，例如 `consumeOrderCreated`。
3. 日志必须包含 Topic、Partition、Offset 和 Key。
4. 重要业务处理成功后再提交 Offset。
5. 异常不要随意吞掉，应交给统一错误处理器处理。
6. 消费逻辑必须具备幂等能力，避免重复消费导致重复写库或重复通知。

### 多 Topic 消费

多 Topic 消费用于一个消费者同时处理多个 Topic。它适合多个 Topic 消息结构相同、处理逻辑相近的场景。例如多个业务日志 Topic 统一进入日志处理消费者。

多 Topic 消费示例：

```java
package io.github.atengk.kafka.consumer;

import io.github.atengk.kafka.constant.KafkaGroupConstant;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 通用业务日志消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class BusinessLogConsumer {

    /**
     * 消费多个日志 Topic
     *
     * @param record         Kafka 消息记录
     * @param acknowledgment Offset 确认对象
     */
    @KafkaListener(
            topics = {
                    KafkaTopicConstant.USER_LOGIN_LOG_TOPIC,
                    KafkaTopicConstant.USER_OPERATE_LOG_TOPIC
            },
            groupId = KafkaGroupConstant.LOG_CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeLog(ConsumerRecord<String, Object> record,
                           Acknowledgment acknowledgment) {
        try {
            log.info("开始消费业务日志消息，topic={}，partition={}，offset={}，key={}",
                    record.topic(), record.partition(), record.offset(), record.key());

            // 根据 topic 区分处理逻辑
            if (KafkaTopicConstant.USER_LOGIN_LOG_TOPIC.equals(record.topic())) {
                log.info("处理用户登录日志消息，key={}", record.key());
            } else if (KafkaTopicConstant.USER_OPERATE_LOG_TOPIC.equals(record.topic())) {
                log.info("处理用户操作日志消息，key={}", record.key());
            }

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("业务日志消息消费失败，topic={}，partition={}，offset={}，key={}",
                    record.topic(), record.partition(), record.offset(), record.key(), e);
            throw e;
        }
    }

}
```

多 Topic 消费使用要求如下：

| 要求                | 说明                                   |
| ------------------- | -------------------------------------- |
| 消息结构相近        | 多个 Topic 的消息模型应基本一致        |
| 处理逻辑相近        | 不建议把完全无关的业务塞进同一个监听器 |
| 日志区分 Topic      | 必须在日志中打印 Topic，便于排查       |
| 异常策略一致        | 多个 Topic 应能共用相同异常处理策略    |
| Consumer Group 明确 | 避免和其他业务消费组混用               |

如果多个 Topic 的消息结构和处理逻辑差异较大，应拆分为多个 Consumer，而不是在一个方法中写大量 `if else` 分支。

### Consumer Group 配置

Consumer Group 用于控制消息的消费分组。同一个 Consumer Group 内，同一条消息只会被一个消费者实例消费；不同 Consumer Group 可以各自独立消费同一个 Topic 的完整消息流。

推荐定义 Consumer Group 常量：

```java
package io.github.atengk.kafka.constant;

/**
 * Kafka Consumer Group 常量
 *
 * @author Ateng
 * @since 2026-05-11
 */
public final class KafkaGroupConstant {

    /**
     * 订单消费组
     */
    public static final String ORDER_CONSUMER_GROUP = "ateng.order.consumer.group";

    /**
     * 支付消费组
     */
    public static final String PAYMENT_CONSUMER_GROUP = "ateng.payment.consumer.group";

    /**
     * 日志消费组
     */
    public static final String LOG_CONSUMER_GROUP = "ateng.log.consumer.group";

    private KafkaGroupConstant() {
    }

}
```

Consumer Group 命名建议如下：

```text
{系统标识}.{业务域}.consumer.group
```

示例：

| 业务场景     | Consumer Group                  |
| ------------ | ------------------------------- |
| 订单事件消费 | `ateng.order.consumer.group`    |
| 支付事件消费 | `ateng.payment.consumer.group`  |
| 积分发放消费 | `ateng.points.consumer.group`   |
| 用户日志消费 | `ateng.user-log.consumer.group` |

Consumer Group 配置要求如下：

1. 生产环境 Consumer Group 名称必须稳定，不应每次发布动态变化。
2. 不同业务处理逻辑应使用不同 Consumer Group。
3. 同一个业务多实例部署时，应使用相同 Consumer Group。
4. 临时测试 Consumer Group 不应与生产业务 Group 混用。
5. 修改 Consumer Group 会导致 Kafka 认为这是新的消费组，可能从 `auto-offset-reset` 指定位置重新消费。

### 并发消费配置

并发消费用于提升 Consumer 处理能力。Spring Kafka 可以通过监听容器的 `concurrency` 参数启动多个消费线程。同一个 Consumer Group 内，最大有效并发通常不超过 Topic 的 Partition 数量。

配置方式一：全局配置。

```yaml
spring:
  kafka:
    listener:
      # 默认监听并发数
      concurrency: 3
```

配置方式二：在监听器上指定并发数。

```java
@KafkaListener(
        topics = KafkaTopicConstant.ORDER_CREATED_TOPIC,
        groupId = KafkaGroupConstant.ORDER_CONSUMER_GROUP,
        concurrency = "3",
        containerFactory = "kafkaListenerContainerFactory"
)
public void consume(ConsumerRecord<String, OrderCreatedMessage> record,
                    Acknowledgment acknowledgment) {
    // 消费处理
}
```

并发数规划建议如下：

| Topic 分区数 | Consumer 实例数 | 每实例 concurrency | 最大有效并发         |
| ------------ | --------------- | ------------------ | -------------------- |
| 3            | 1               | 3                  | 3                    |
| 6            | 2               | 3                  | 6                    |
| 12           | 3               | 4                  | 12                   |
| 12           | 6               | 4                  | 12，部分线程可能空闲 |

并发消费注意事项如下：

1. 并发数不应盲目大于 Partition 数。
2. 同一个 Partition 在同一时刻只会分配给同一个 Consumer 线程消费。
3. 如果同一业务 Key 要求顺序处理，需要避免并发处理破坏业务顺序。
4. 并发提升后，数据库、Redis、下游接口也需要具备相应处理能力。
5. 如果消费逻辑耗时较长，需要同步调整 `max.poll.interval.ms`。
6. 并发消费必须配合幂等处理，否则重复消费风险会被放大。

### 批量消费配置

批量消费是指 Consumer 一次拉取并处理多条消息。它适合日志采集、数据同步、批量入库、批量写 ES 等场景。批量消费可以提升吞吐，但异常处理和 Offset 提交会更复杂。

批量消费需要配置专用监听容器工厂。

文件位置：`src/main/java/io/github/atengk/kafka/config/KafkaBatchListenerConfig.java`

该配置类用于定义批量消费监听容器，开启批量监听并使用手动提交 Offset。

```java
package io.github.atengk.kafka.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

/**
 * Kafka 批量消费监听配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Configuration
public class KafkaBatchListenerConfig {

    /**
     * 配置批量消费监听容器工厂
     *
     * @param consumerFactory 消费者工厂
     * @return 批量消费监听容器工厂
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaBatchListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        log.info("初始化Kafka批量消费监听容器，batchListener=true，ackMode=MANUAL_IMMEDIATE");
        return factory;
    }

}
```

批量消费示例：

```java
package io.github.atengk.kafka.consumer;

import io.github.atengk.kafka.constant.KafkaGroupConstant;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 批量日志消息消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class BatchLogConsumer {

    /**
     * 批量消费日志消息
     *
     * @param records        Kafka 消息列表
     * @param acknowledgment Offset 确认对象
     */
    @KafkaListener(
            topics = KafkaTopicConstant.USER_OPERATE_LOG_TOPIC,
            groupId = KafkaGroupConstant.LOG_CONSUMER_GROUP,
            containerFactory = "kafkaBatchListenerContainerFactory"
    )
    public void consumeBatch(List<ConsumerRecord<String, Object>> records,
                             Acknowledgment acknowledgment) {
        if (records == null || records.isEmpty()) {
            return;
        }

        try {
            log.info("开始批量消费Kafka消息，count={}，topic={}", records.size(), records.get(0).topic());

            for (ConsumerRecord<String, Object> record : records) {
                log.info("处理批量消息，topic={}，partition={}，offset={}，key={}",
                        record.topic(), record.partition(), record.offset(), record.key());

                // 批量处理中的单条业务逻辑
            }

            acknowledgment.acknowledge();
            log.info("批量Kafka消息消费成功，count={}", records.size());
        } catch (Exception e) {
            log.error("批量Kafka消息消费失败，count={}", records.size(), e);
            throw e;
        }
    }

}
```

批量消费配置建议：

```yaml
spring:
  kafka:
    consumer:
      properties:
        # 单次 poll 最大拉取数量，批量消费时需要结合业务处理能力设置
        max.poll.records: 100

    listener:
      # 批量监听模式
      type: batch
```

批量消费注意事项如下：

1. 批量消费适合吞吐优先场景，不适合每条消息都需要复杂独立事务的场景。
2. 批量中某条消息失败时，需要明确整批失败、跳过失败项还是拆分重试。
3. 批量提交 Offset 前必须确认整批消息都已处理成功。
4. 批量处理时间不能超过 `max.poll.interval.ms`。
5. 批量消费仍需做单条幂等处理，不能只做整批幂等。

### 消息 Header 读取

Consumer 读取 Header 可以获取消息 ID、TraceId、消息类型、版本、来源服务、重试次数等元数据。Header 读取建议封装为工具类，避免每个 Consumer 重复解析。

文件位置：`src/main/java/io/github/atengk/kafka/support/KafkaHeaderSupport.java`

该工具类用于从 Kafka Header 中读取字符串值，并兼容 Header 不存在的情况。

```java
package io.github.atengk.kafka.support;

import cn.hutool.core.util.StrUtil;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;

/**
 * Kafka Header 工具类
 *
 * @author Ateng
 * @since 2026-05-11
 */
public final class KafkaHeaderSupport {

    private KafkaHeaderSupport() {
    }

    /**
     * 读取 Header 字符串
     *
     * @param headers Header 集合
     * @param key     Header Key
     * @return Header 值
     */
    public static String getString(Headers headers, String key) {
        if (headers == null || StrUtil.isBlank(key)) {
            return null;
        }

        Header header = headers.lastHeader(key);
        if (header == null || header.value() == null) {
            return null;
        }

        return new String(header.value(), StandardCharsets.UTF_8);
    }

}
```

Consumer 中读取 Header 示例：

```java
String messageId = KafkaHeaderSupport.getString(record.headers(), KafkaHeaderConstant.MESSAGE_ID);
String traceId = KafkaHeaderSupport.getString(record.headers(), KafkaHeaderConstant.TRACE_ID);
String messageType = KafkaHeaderSupport.getString(record.headers(), KafkaHeaderConstant.MESSAGE_TYPE);

log.info("读取Kafka消息Header，messageId={}，traceId={}，messageType={}",
        messageId, traceId, messageType);
```

Header 读取要求如下：

1. 读取 Header 时要兼容不存在的情况。
2. Header 值统一使用 UTF-8 编码。
3. 日志中优先打印 `messageId` 和 `traceId`。
4. 不应将 Header 作为唯一业务数据来源，关键业务字段仍应在消息体中保留。
5. 重试、死信和重放时应保留原始关键 Header。

### 消息 Key 读取

Consumer 读取消息 Key 可以用于日志定位、幂等处理、顺序控制和业务分流。Key 通常是业务主体标识，例如订单 ID、用户 ID、支付单号或商品 ID。

读取 Key 示例：

```java
String key = record.key();

log.info("读取Kafka消息Key，topic={}，partition={}，offset={}，key={}",
        record.topic(), record.partition(), record.offset(), key);
```

Key 使用建议如下：

| 场景     | 用法                                |
| -------- | ----------------------------------- |
| 日志定位 | 将 Key 打印到消费日志中             |
| 幂等处理 | 结合 `messageId` 或业务 ID 进行去重 |
| 顺序控制 | 同一 Key 的消息避免并发乱序处理     |
| 业务分流 | 根据 Key 前缀区分业务主体           |

需要注意，Kafka 消息 Key 可能为空。Consumer 不能假设所有消息都有 Key。如果业务强依赖 Key，应在 Producer 侧强制校验，并在 Consumer 侧发现 Key 为空时记录异常或进入死信处理。

### ConsumerRecord 使用

`ConsumerRecord` 是 Kafka 消费端最完整的消息记录对象。它包含 Topic、Partition、Offset、Key、Value、Timestamp、Headers 等信息。正式项目中推荐监听方法接收 `ConsumerRecord`，不要只接收消息体。

`ConsumerRecord` 常用字段如下：

| 字段            | 说明               |
| --------------- | ------------------ |
| `topic()`       | 消息所属 Topic     |
| `partition()`   | 消息所属 Partition |
| `offset()`      | 消息 Offset        |
| `key()`         | 消息 Key           |
| `value()`       | 消息体             |
| `timestamp()`   | 消息时间戳         |
| `headers()`     | 消息 Header        |
| `leaderEpoch()` | Leader Epoch 信息  |

推荐使用示例：

```java
@KafkaListener(
        topics = KafkaTopicConstant.ORDER_CREATED_TOPIC,
        groupId = KafkaGroupConstant.ORDER_CONSUMER_GROUP,
        containerFactory = "kafkaListenerContainerFactory"
)
public void consume(ConsumerRecord<String, OrderCreatedMessage> record,
                    Acknowledgment acknowledgment) {
    String topic = record.topic();
    int partition = record.partition();
    long offset = record.offset();
    String key = record.key();
    OrderCreatedMessage message = record.value();

    log.info("收到Kafka消息，topic={}，partition={}，offset={}，key={}，timestamp={}",
            topic, partition, offset, key, record.timestamp());

    // 业务处理完成后提交 Offset
    acknowledgment.acknowledge();
}
```

推荐在正式 Consumer 中使用 `ConsumerRecord` 的原因：

1. 可以记录完整消费位置。
2. 可以读取 Header 中的消息治理字段。
3. 可以根据 Topic 或 Partition 做特殊处理。
4. 可以将 Topic、Partition、Offset 写入消费记录表。
5. 发生异常时更容易定位具体消息。

### Consumer 封装组件

Consumer 封装组件用于统一处理消息解析、日志记录、Header 读取、异常转换、幂等检查和 Offset 提交。由于 `@KafkaListener` 本身必须写在具体监听类中，因此 Consumer 封装通常采用“监听器轻量化 + 处理器标准化”的方式。

推荐结构如下：

```text
src/main/java/io/github/atengk/kafka/consumer
├── KafkaMessageConsumer.java
├── KafkaConsumeContext.java
├── KafkaConsumeHandler.java
└── DefaultKafkaConsumeHandler.java
```

文件位置：`src/main/java/io/github/atengk/kafka/consumer/KafkaConsumeContext.java`

该上下文对象用于封装 ConsumerRecord 中的关键消费信息，便于业务处理器统一使用。

```java
package io.github.atengk.kafka.consumer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka 消费上下文
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaConsumeContext<T> {

    /**
     * Topic 名称
     */
    private String topic;

    /**
     * 分区编号
     */
    private Integer partition;

    /**
     * Offset
     */
    private Long offset;

    /**
     * 消息 Key
     */
    private String key;

    /**
     * 消息唯一标识
     */
    private String messageId;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 链路追踪 ID
     */
    private String traceId;

    /**
     * 消息体
     */
    private T payload;

}
```

文件位置：`src/main/java/io/github/atengk/kafka/consumer/KafkaConsumeHandler.java`

该接口用于定义统一消费处理入口。

```java
package io.github.atengk.kafka.consumer;

/**
 * Kafka 消费处理器
 *
 * @author Ateng
 * @since 2026-05-11
 */
public interface KafkaConsumeHandler<T> {

    /**
     * 处理 Kafka 消息
     *
     * @param context 消费上下文
     */
    void handle(KafkaConsumeContext<T> context);

}
```

文件位置：`src/main/java/io/github/atengk/kafka/consumer/DefaultKafkaConsumeHandler.java`

该处理器用于演示统一消费处理逻辑，实际项目中可以在此处接入幂等、监控、业务分发和异常转换。

```java
package io.github.atengk.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 默认 Kafka 消费处理器
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class DefaultKafkaConsumeHandler<T> implements KafkaConsumeHandler<T> {

    /**
     * 处理 Kafka 消息
     *
     * @param context 消费上下文
     */
    @Override
    public void handle(KafkaConsumeContext<T> context) {
        log.info("统一处理Kafka消息，topic={}，partition={}，offset={}，key={}，messageId={}，traceId={}",
                context.getTopic(),
                context.getPartition(),
                context.getOffset(),
                context.getKey(),
                context.getMessageId(),
                context.getTraceId());

        // 实际项目中可以在这里接入幂等校验、业务路由、监控埋点等
    }

}
```

监听器中使用封装组件示例：

```java
package io.github.atengk.kafka.consumer;

import io.github.atengk.kafka.constant.KafkaGroupConstant;
import io.github.atengk.kafka.constant.KafkaHeaderConstant;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.model.OrderCreatedMessage;
import io.github.atengk.kafka.support.KafkaHeaderSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 封装式订单消息消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WrappedOrderConsumer {

    private final KafkaConsumeHandler<OrderCreatedMessage> kafkaConsumeHandler;

    /**
     * 消费订单创建消息
     *
     * @param record         Kafka 消息记录
     * @param acknowledgment Offset 确认对象
     */
    @KafkaListener(
            topics = KafkaTopicConstant.ORDER_CREATED_TOPIC,
            groupId = KafkaGroupConstant.ORDER_CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, OrderCreatedMessage> record,
                        Acknowledgment acknowledgment) {
        try {
            KafkaConsumeContext<OrderCreatedMessage> context = KafkaConsumeContext.<OrderCreatedMessage>builder()
                    .topic(record.topic())
                    .partition(record.partition())
                    .offset(record.offset())
                    .key(record.key())
                    .messageId(KafkaHeaderSupport.getString(record.headers(), KafkaHeaderConstant.MESSAGE_ID))
                    .messageType(KafkaHeaderSupport.getString(record.headers(), KafkaHeaderConstant.MESSAGE_TYPE))
                    .traceId(KafkaHeaderSupport.getString(record.headers(), KafkaHeaderConstant.TRACE_ID))
                    .payload(record.value())
                    .build();

            kafkaConsumeHandler.handle(context);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("封装式Kafka消息消费失败，topic={}，partition={}，offset={}，key={}",
                    record.topic(), record.partition(), record.offset(), record.key(), e);
            throw e;
        }
    }

}
```

这种封装方式的好处是：监听器只负责接收 Kafka 消息和提交 Offset，通用处理逻辑集中到 `KafkaConsumeHandler` 中，便于后续接入幂等表、消费日志表、失败记录表、监控指标和死信处理。

## Offset 管理

本章节用于说明 Kafka Offset 的提交方式和管理策略，包括自动提交、手动提交、AckMode 配置、同步提交、异步提交、重复消费处理、消息丢失防护和 Offset 重置策略。

Offset 是 Consumer 在 Partition 中的消费进度。Offset 提交位置决定了 Consumer 重启、扩容、故障恢复或 Rebalance 后从哪里继续消费。Offset 管理不当会导致重复消费或消息丢失，因此重要业务消息必须明确提交策略。

### 自动提交 Offset

自动提交 Offset 是 Kafka Consumer 的默认能力之一。开启自动提交后，Consumer 会按照配置周期自动提交当前拉取位置。它配置简单，但不适合重要业务消息。

自动提交配置示例：

```yaml
spring:
  kafka:
    consumer:
      # 开启自动提交 Offset
      enable-auto-commit: true

      properties:
        # 自动提交间隔，单位毫秒
        auto.commit.interval.ms: 5000
```

自动提交的风险在于，Offset 提交和业务处理不是强绑定关系。如果 Consumer 拉取到消息后 Offset 已自动提交，但业务处理失败或应用宕机，重启后可能从已提交 Offset 之后继续消费，导致这条消息被跳过。

自动提交适用场景：

| 场景       | 说明             |
| ---------- | ---------------- |
| 日志采集   | 少量丢失可接受   |
| 非核心统计 | 数据允许轻微误差 |
| 临时测试   | 快速验证消费能力 |
| 低价值事件 | 对可靠性要求不高 |

不建议在订单、支付、库存、账户、审计、通知状态变更等重要业务中使用自动提交。

### 手动提交 Offset

手动提交 Offset 是可靠消费的推荐方式。Consumer 在业务处理成功后主动提交 Offset，确保“处理成功”和“消费进度推进”尽量绑定。

推荐配置：

```yaml
spring:
  kafka:
    consumer:
      # 关闭自动提交 Offset
      enable-auto-commit: false

    listener:
      # 手动立即提交
      ack-mode: manual_immediate
```

手动提交示例：

```java
@KafkaListener(
        topics = KafkaTopicConstant.ORDER_CREATED_TOPIC,
        groupId = KafkaGroupConstant.ORDER_CONSUMER_GROUP,
        containerFactory = "kafkaListenerContainerFactory"
)
public void consume(ConsumerRecord<String, OrderCreatedMessage> record,
                    Acknowledgment acknowledgment) {
    try {
        log.info("开始处理订单消息，topic={}，partition={}，offset={}，key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        // 业务处理成功后提交 Offset
        acknowledgment.acknowledge();

        log.info("订单消息Offset提交成功，topic={}，partition={}，offset={}",
                record.topic(), record.partition(), record.offset());
    } catch (Exception e) {
        log.error("订单消息处理失败，不提交Offset，topic={}，partition={}，offset={}，key={}",
                record.topic(), record.partition(), record.offset(), record.key(), e);
        throw e;
    }
}
```

手动提交要求如下：

1. 只有业务处理成功后才调用 `acknowledge()`。
2. 业务失败时不要提交 Offset，应抛出异常交给错误处理器。
3. 如果业务中存在部分成功，需要先设计幂等和补偿机制。
4. 手动提交不能消除重复消费，只能降低消息丢失风险。
5. 业务侧必须具备幂等处理能力。

### AckMode 配置

AckMode 是 Spring Kafka 对 Offset 提交时机的抽象配置。不同 AckMode 决定了监听容器何时提交 Offset。

常见 AckMode 如下：

| AckMode            | 说明                         | 使用建议                 |
| ------------------ | ---------------------------- | ------------------------ |
| `RECORD`           | 每处理一条记录后提交         | 简单但提交频繁           |
| `BATCH`            | 每批 poll 的消息处理完后提交 | 吞吐较好，但失败影响整批 |
| `TIME`             | 按时间间隔提交               | 适合非核心场景           |
| `COUNT`            | 按处理数量提交               | 适合吞吐优先场景         |
| `COUNT_TIME`       | 数量或时间满足其一提交       | 折中方案                 |
| `MANUAL`           | 手动确认，由容器后续提交     | 需要开发者调用 ack       |
| `MANUAL_IMMEDIATE` | 手动确认后立即提交           | 重要业务推荐             |

重要业务推荐配置：

```yaml
spring:
  kafka:
    listener:
      # 手动立即提交 Offset
      ack-mode: manual_immediate
```

监听容器配置示例：

```java
factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
```

选择建议如下：

| 场景           | 推荐 AckMode                   |
| -------------- | ------------------------------ |
| 重要业务消息   | `MANUAL_IMMEDIATE`             |
| 普通异步通知   | `MANUAL` 或 `MANUAL_IMMEDIATE` |
| 高吞吐日志消费 | `BATCH`                        |
| 批量入库       | `BATCH` 或 `MANUAL_IMMEDIATE`  |
| 临时测试       | `BATCH` 或自动提交             |

项目中建议统一使用 `MANUAL_IMMEDIATE` 作为默认配置，特殊高吞吐场景再单独定义批量监听容器。

### 同步提交

同步提交是 Kafka 原生 Consumer 的提交方式之一，通常通过 `commitSync()` 完成。同步提交会阻塞当前线程，直到 Broker 返回提交结果或发生异常。

在 Spring Kafka 中，手动调用 `Acknowledgment#acknowledge()` 通常已经能满足大多数手动提交需求。如果确实需要使用原生 Consumer，可以在监听方法中注入 `Consumer` 对象。

同步提交示例：

```java
package io.github.atengk.kafka.consumer;

import io.github.atengk.kafka.constant.KafkaGroupConstant;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.model.OrderCreatedMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka 同步提交 Offset 消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class SyncCommitConsumer {

    /**
     * 消费并同步提交 Offset
     *
     * @param record   Kafka 消息记录
     * @param consumer Kafka 原生 Consumer
     */
    @KafkaListener(
            topics = KafkaTopicConstant.ORDER_CREATED_TOPIC,
            groupId = KafkaGroupConstant.ORDER_CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, OrderCreatedMessage> record,
                        Consumer<String, OrderCreatedMessage> consumer) {
        try {
            log.info("开始处理消息并同步提交Offset，topic={}，partition={}，offset={}，key={}",
                    record.topic(), record.partition(), record.offset(), record.key());

            // 执行业务逻辑

            consumer.commitSync();

            log.info("同步提交Offset成功，topic={}，partition={}，offset={}",
                    record.topic(), record.partition(), record.offset());
        } catch (Exception e) {
            log.error("同步提交Offset失败，topic={}，partition={}，offset={}，key={}",
                    record.topic(), record.partition(), record.offset(), record.key(), e);
            throw e;
        }
    }

}
```

同步提交特点如下：

| 特点         | 说明                       |
| ------------ | -------------------------- |
| 可靠性较高   | 可以明确知道提交是否成功   |
| 性能较低     | 提交过程会阻塞消费线程     |
| 适合重要场景 | 适合对提交结果敏感的业务   |
| 需要异常处理 | 提交失败时可能导致重复消费 |

大多数 Spring Boot 项目中，建议优先使用 `Acknowledgment#acknowledge()`，不直接操作原生 `commitSync()`，除非确实需要精细控制提交行为。

### 异步提交

异步提交通过 `commitAsync()` 完成，不会阻塞消费线程。它适合高吞吐场景，但提交失败后处理复杂。如果异步提交失败，而后续 Offset 已经提交成功，重试旧 Offset 可能导致提交顺序问题。

异步提交示例：

```java
package io.github.atengk.kafka.consumer;

import io.github.atengk.kafka.constant.KafkaGroupConstant;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.model.OrderCreatedMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka 异步提交 Offset 消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class AsyncCommitConsumer {

    /**
     * 消费并异步提交 Offset
     *
     * @param record   Kafka 消息记录
     * @param consumer Kafka 原生 Consumer
     */
    @KafkaListener(
            topics = KafkaTopicConstant.ORDER_CREATED_TOPIC,
            groupId = KafkaGroupConstant.ORDER_CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, OrderCreatedMessage> record,
                        Consumer<String, OrderCreatedMessage> consumer) {
        try {
            log.info("开始处理消息并异步提交Offset，topic={}，partition={}，offset={}，key={}",
                    record.topic(), record.partition(), record.offset(), record.key());

            // 执行业务逻辑

            consumer.commitAsync((offsets, exception) -> {
                if (exception != null) {
                    log.error("异步提交Offset失败，offsets={}", offsets, exception);
                    return;
                }
                log.info("异步提交Offset成功，offsets={}", offsets);
            });
        } catch (Exception e) {
            log.error("异步提交Offset消费失败，topic={}，partition={}，offset={}，key={}",
                    record.topic(), record.partition(), record.offset(), record.key(), e);
            throw e;
        }
    }

}
```

异步提交适用场景：

| 场景             | 说明                   |
| ---------------- | ---------------------- |
| 日志消费         | 吞吐优先，允许少量重复 |
| 统计类消费       | 允许通过后续任务修正   |
| 高吞吐非核心消息 | 更关注消费速度         |
| 批量处理场景     | 可降低提交阻塞         |

重要业务不建议直接使用异步提交作为唯一提交方式。更常见的做法是业务处理成功后使用 `Acknowledgment#acknowledge()`，由 Spring Kafka 容器按配置处理提交。

### 重复消费处理

Kafka 消费端必须按照“可能重复消费”设计。即使使用手动提交 Offset，也可能出现业务处理成功但 Offset 提交失败、Consumer 重启、Rebalance、网络抖动等情况，导致同一条消息再次被消费。

重复消费常见原因如下：

| 原因                       | 说明                                         |
| -------------------------- | -------------------------------------------- |
| 业务成功但 Offset 提交失败 | Consumer 重启后会重新消费已处理消息          |
| Consumer 处理超时          | 超过 `max.poll.interval.ms` 后触发 Rebalance |
| 应用宕机                   | 已处理但未提交 Offset 的消息会再次消费       |
| Rebalance                  | 分区重新分配后可能从已提交 Offset 继续       |
| 手动重置 Offset            | 运维操作导致重复消费历史消息                 |
| Producer 重发              | 上游业务补偿或重试产生相同业务消息           |

重复消费处理方式如下：

| 方式           | 说明                                     |
| -------------- | ---------------------------------------- |
| 数据库唯一约束 | 以 `messageId` 或业务唯一键建立唯一索引  |
| 消费状态表     | 记录消息处理状态，处理前先查询           |
| Redis 去重     | 适合短期高频去重                         |
| 本地缓存去重   | 适合短时间内的重复过滤，不适合强一致     |
| 业务状态机     | 根据状态判断是否允许重复处理             |
| 幂等接口       | 下游接口支持相同请求重复调用不产生副作用 |

数据库幂等表建议：

```sql
CREATE TABLE kafka_consume_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息唯一标识',
    consumer_group VARCHAR(128) NOT NULL COMMENT '消费组',
    topic VARCHAR(128) NOT NULL COMMENT 'Topic名称',
    partition_id INT NOT NULL COMMENT '分区编号',
    offset_value BIGINT NOT NULL COMMENT 'Offset',
    consume_status VARCHAR(32) NOT NULL COMMENT '消费状态',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '异常信息',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_message_group (message_id, consumer_group),
    KEY idx_topic_partition_offset (topic, partition_id, offset_value)
) COMMENT='Kafka消费记录表';
```

重复消费处理要求如下：

1. 重要业务必须做幂等。
2. 幂等键优先使用 `messageId + consumerGroup`。
3. 如果 Producer 可能对同一业务事件生成不同 `messageId`，还需要增加业务唯一键幂等。
4. 重复消息不能直接报错，应识别后安全跳过并提交 Offset。
5. 幂等记录需要设置合理清理策略，避免表无限增长。

### 消息丢失防护

消息丢失通常不是 Kafka 单点问题，而是 Producer、Broker、Consumer 和业务代码共同作用的结果。Consumer 侧消息丢失主要来自 Offset 提交过早、异常被吞掉、批量处理不完整和自动提交配置不当。

Consumer 侧丢失风险如下：

| 风险                     | 示例                     | 防护方式                       |
| ------------------------ | ------------------------ | ------------------------------ |
| 先提交 Offset 后处理业务 | Offset 已提交，业务失败  | 业务成功后再提交               |
| 自动提交 Offset          | 拉取后未处理完已提交     | 关闭自动提交                   |
| 异常被吞掉               | catch 后只打印日志不抛出 | 异常交给错误处理器             |
| 批量部分失败             | 整批提交但部分消息失败   | 单条幂等和失败记录             |
| 反序列化失败             | 消息无法进入监听方法     | 使用 ErrorHandlingDeserializer |
| Rebalance 前未提交       | 分区回收时进度丢失       | 处理好 Rebalance 和提交策略    |

推荐配置如下：

```yaml
spring:
  kafka:
    consumer:
      # 关闭自动提交，避免业务未完成但Offset已提交
      enable-auto-commit: false

      # 使用错误处理反序列化器，避免反序列化失败直接阻塞
      value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer

      properties:
        spring.deserializer.value.delegate.class: org.springframework.kafka.support.serializer.JsonDeserializer

    listener:
      # 手动提交 Offset
      ack-mode: manual_immediate
```

Consumer 代码要求如下：

```java
try {
    // 1. 读取消息
    // 2. 幂等检查
    // 3. 执行业务逻辑
    // 4. 记录消费成功状态
    acknowledgment.acknowledge();
} catch (Exception e) {
    // 不提交 Offset，抛出异常交给统一错误处理器
    throw e;
}
```

消息丢失防护原则如下：

1. 关闭自动提交 Offset。
2. 业务处理成功后再提交 Offset。
3. 不吞异常，不伪造消费成功。
4. 反序列化异常需要进入统一异常处理或死信逻辑。
5. 重要业务消息处理结果应落库。
6. 死信 Topic 不能替代业务幂等和补偿机制。
7. 批量消费时必须明确单条失败如何处理。

### Offset 重置策略

`auto.offset.reset` 用于控制 Consumer Group 没有已提交 Offset，或者已提交 Offset 不存在时，从哪里开始消费。它只在没有可用 Offset 时生效，不会覆盖已经存在的提交进度。

常见配置如下：

| 值         | 说明                   | 适用场景                         |
| ---------- | ---------------------- | -------------------------------- |
| `earliest` | 从最早可用消息开始消费 | 本地开发、测试、补历史数据       |
| `latest`   | 从最新消息开始消费     | 生产新消费组上线且不需要历史消息 |
| `none`     | 没有 Offset 时抛出异常 | 对消费起点严格控制的场景         |

配置示例：

```yaml
spring:
  kafka:
    consumer:
      # 没有 Offset 时从最早消息开始消费
      auto-offset-reset: earliest
```

不同环境建议如下：

| 环境       | 建议值             | 说明                           |
| ---------- | ------------------ | ------------------------------ |
| 本地开发   | `earliest`         | 方便重复验证历史消息           |
| 自动化测试 | `earliest`         | 便于测试用例读取已发送消息     |
| 预发环境   | 按业务决定         | 尽量模拟生产                   |
| 生产环境   | `latest` 或 `none` | 避免新消费组误消费大量历史消息 |

Offset 重置操作也可以通过 Kafka 命令完成。例如，将某个消费组的 Offset 重置到最早位置：

```bash
docker exec -it ateng-kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group ateng.order.consumer.group \
  --topic ateng.order.created.topic \
  --reset-offsets \
  --to-earliest \
  --execute
```

将消费组 Offset 重置到最新位置：

```bash
docker exec -it ateng-kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group ateng.order.consumer.group \
  --topic ateng.order.created.topic \
  --reset-offsets \
  --to-latest \
  --execute
```

查看消费组 Offset：

```bash
docker exec -it ateng-kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group ateng.order.consumer.group \
  --describe
```

这些命令中，`--group` 指定消费组，`--topic` 指定 Topic，`--reset-offsets` 表示执行 Offset 重置操作，`--to-earliest` 表示重置到最早可用消息，`--to-latest` 表示重置到最新位置。生产环境执行 Offset 重置前必须先确认影响范围，并暂停相关消费者实例，避免边消费边重置导致结果不可控。


## 消费异常处理

本章节用于说明 Kafka Consumer 消费失败后的处理方式，包括 Listener 异常处理、`DefaultErrorHandler` 配置、重试间隔、最大重试次数、不可重试异常、死信 Topic、`DeadLetterPublishingRecoverer` 使用、死信消息处理和异常日志规范。

Kafka 消费异常不能简单通过 `try catch` 打印日志后吞掉。如果异常被吞掉并提交 Offset，Kafka 会认为该消息已经消费成功，后续不会再投递该消息，可能造成业务数据丢失。推荐做法是：业务处理失败时抛出异常，由统一错误处理器接管，按照重试策略进行重试，超过重试次数后投递到死信 Topic，再由死信处理流程进行排查、修复和重放。

### Listener 异常处理

Listener 异常处理是消费异常处理的第一层。监听方法内部只负责业务处理和必要日志，不建议在监听方法内完成复杂重试、死信投递和降级逻辑。

推荐写法如下：

```java
package io.github.atengk.kafka.consumer;

import io.github.atengk.kafka.constant.KafkaGroupConstant;
import io.github.atengk.kafka.constant.KafkaHeaderConstant;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.model.OrderCreatedMessage;
import io.github.atengk.kafka.support.KafkaHeaderSupport;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 订单创建消息消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class OrderCreatedExceptionConsumer {

    /**
     * 消费订单创建消息
     *
     * @param record         Kafka 消息记录
     * @param acknowledgment Offset 确认对象
     */
    @KafkaListener(
            topics = KafkaTopicConstant.ORDER_CREATED_TOPIC,
            groupId = KafkaGroupConstant.ORDER_CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, OrderCreatedMessage> record,
                        Acknowledgment acknowledgment) {
        String messageId = KafkaHeaderSupport.getString(record.headers(), KafkaHeaderConstant.MESSAGE_ID);
        String traceId = KafkaHeaderSupport.getString(record.headers(), KafkaHeaderConstant.TRACE_ID);

        try {
            OrderCreatedMessage message = record.value();

            log.info("开始消费订单创建消息，topic={}，partition={}，offset={}，key={}，messageId={}，traceId={}，orderId={}",
                    record.topic(), record.partition(), record.offset(), record.key(), messageId, traceId, message.getOrderId());

            // 执行业务处理，例如积分发放、数据同步、统计计算等

            acknowledgment.acknowledge();

            log.info("订单创建消息消费成功，topic={}，partition={}，offset={}，key={}，messageId={}，traceId={}",
                    record.topic(), record.partition(), record.offset(), record.key(), messageId, traceId);
        } catch (Exception e) {
            log.error("订单创建消息消费失败，topic={}，partition={}，offset={}，key={}，messageId={}，traceId={}",
                    record.topic(), record.partition(), record.offset(), record.key(), messageId, traceId, e);

            // 不提交 Offset，继续抛出异常，由 DefaultErrorHandler 统一处理
            throw e;
        }
    }

}
```

Listener 异常处理要求如下：

| 要求              | 说明                                                       |
| ----------------- | ---------------------------------------------------------- |
| 不吞异常          | 消费失败必须抛出异常，交给统一错误处理器                   |
| 成功后提交 Offset | 业务处理成功后再调用 `acknowledge()`                       |
| 失败不提交 Offset | 失败时不要手动提交 Offset                                  |
| 日志字段完整      | 至少包含 Topic、Partition、Offset、Key、messageId、traceId |
| 不做复杂重试      | 重试交给统一错误处理器或 Retry Topic                       |
| 做好幂等          | 重试和重复投递都可能再次执行消费逻辑                       |

不推荐写法如下：

```java
try {
    // 执行业务逻辑
} catch (Exception e) {
    log.error("消费失败", e);
}

// 错误：业务失败后仍提交 Offset，可能导致消息丢失
acknowledgment.acknowledge();
```

这种写法会导致业务失败但 Offset 已提交，Kafka 不会再次投递该消息，后续只能依赖人工补偿，风险较高。

### DefaultErrorHandler 配置

`DefaultErrorHandler` 是 Spring Kafka 中常用的消费异常处理器，可以配置重试间隔、最大重试次数、不可重试异常和死信恢复器。它适合处理消费端同步重试场景。

推荐文件结构如下：

```text
src/main/java/io/github/atengk/kafka
├── config
│   └── KafkaErrorHandlerConfig.java
├── constant
│   └── KafkaTopicConstant.java
└── exception
    ├── NonRetryableBusinessException.java
    └── RetryableBusinessException.java
```

文件位置：`src/main/java/io/github/atengk/kafka/config/KafkaErrorHandlerConfig.java`

该配置类用于定义 Kafka 消费异常处理器，支持固定间隔重试、不可重试异常识别和死信 Topic 投递。

```java
package io.github.atengk.kafka.config;

import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.exception.NonRetryableBusinessException;
import io.github.atengk.kafka.exception.RetryableBusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka 消费异常处理配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Configuration
public class KafkaErrorHandlerConfig {

    /**
     * 配置 Kafka 消费异常处理器
     *
     * @param kafkaTemplate Kafka 消息发送模板
     * @return Kafka 通用异常处理器
     */
    @Bean
    public CommonErrorHandler kafkaCommonErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> {
                    log.error("Kafka消息超过最大重试次数，准备投递死信Topic，topic={}，partition={}，offset={}，key={}",
                            record.topic(), record.partition(), record.offset(), record.key(), exception);

                    // 死信消息默认投递到统一死信 Topic，并尽量保持原分区编号
                    return new TopicPartition(KafkaTopicConstant.DEAD_LETTER_TOPIC, record.partition());
                }
        );

        // 每次间隔 2 秒，最多重试 3 次，不包含首次消费
        FixedBackOff fixedBackOff = new FixedBackOff(2000L, 3L);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, fixedBackOff);

        // 不可重试异常：反序列化、参数错误、业务明确不可恢复异常等
        errorHandler.addNotRetryableExceptions(
                DeserializationException.class,
                IllegalArgumentException.class,
                NonRetryableBusinessException.class
        );

        // 可重试异常：临时网络异常、下游服务短暂不可用、数据库短暂异常等
        errorHandler.addRetryableExceptions(
                KafkaException.class,
                ListenerExecutionFailedException.class,
                RetryableBusinessException.class
        );

        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("Kafka消息消费重试，topic={}，partition={}，offset={}，key={}，deliveryAttempt={}",
                        record.topic(), record.partition(), record.offset(), record.key(), deliveryAttempt, ex)
        );

        log.info("初始化Kafka消费异常处理器，retryInterval={}ms，maxRetryTimes={}", 2000, 3);
        return errorHandler;
    }

}
```

如果项目中已有 `kafkaListenerContainerFactory`，需要将错误处理器注入监听容器。

文件位置：`src/main/java/io/github/atengk/kafka/config/KafkaListenerConfig.java`

该配置类用于给 Kafka 监听容器绑定统一异常处理器。

```java
package io.github.atengk.kafka.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;

/**
 * Kafka 监听容器配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Configuration
public class KafkaListenerConfig {

    /**
     * 配置 Kafka 监听容器工厂
     *
     * @param consumerFactory    消费者工厂
     * @param commonErrorHandler 通用异常处理器
     * @return Kafka 监听容器工厂
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            CommonErrorHandler commonErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(commonErrorHandler);
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        log.info("初始化Kafka监听容器工厂，ackMode=MANUAL_IMMEDIATE，concurrency=3");
        return factory;
    }

}
```

### 重试间隔配置

重试间隔用于控制消费失败后多久再次尝试消费。间隔过短会导致失败消息频繁重试，压垮下游服务；间隔过长会影响消息恢复速度。

常见重试间隔策略如下：

| 策略             | 说明                     | 适用场景                     |
| ---------------- | ------------------------ | ---------------------------- |
| 固定间隔         | 每次重试间隔相同         | 简单业务、短暂异常           |
| 指数退避         | 每次重试间隔递增         | 下游服务不稳定、需要保护下游 |
| 随机退避         | 在一定范围内随机间隔     | 避免大量消息同时重试         |
| Retry Topic 延迟 | 将失败消息转入延迟 Topic | 非阻塞重试、长时间延迟重试   |

固定间隔示例：

```java
FixedBackOff fixedBackOff = new FixedBackOff(2000L, 3L);
DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, fixedBackOff);
```

配置含义如下：

| 参数    | 说明                          |
| ------- | ----------------------------- |
| `2000L` | 每次重试间隔 2 秒             |
| `3L`    | 最多重试 3 次，不包含首次消费 |

如果下游服务存在明显抖动或限流，建议不要使用过短间隔，例如 `100ms` 或 `500ms`。这类配置可能导致失败消息在短时间内反复打到下游，扩大故障影响。

### 最大重试次数配置

最大重试次数用于控制一条消息消费失败后最多尝试多少次。重试次数过少可能无法覆盖短暂故障；重试次数过多会阻塞当前 Partition 的后续消息，尤其是阻塞式重试场景。

推荐配置如下：

```java
FixedBackOff fixedBackOff = new FixedBackOff(2000L, 3L);
```

重试次数建议如下：

| 场景           | 建议重试次数     | 说明                     |
| -------------- | ---------------- | ------------------------ |
| 参数错误       | `0`              | 不应重试，直接死信或失败 |
| 反序列化失败   | `0`              | 重试无法恢复             |
| 下游短暂超时   | `3` 到 `5`       | 可短暂重试               |
| 数据库短暂异常 | `3` 到 `5`       | 可短暂重试               |
| 第三方服务限流 | `1` 到 `3`       | 不宜频繁重试             |
| 长时间不可用   | 使用 Retry Topic | 不建议阻塞主消费线程     |

最大重试次数不是越多越好。对于同一个 Partition，阻塞式重试期间后续消息可能无法继续处理。如果失败原因短时间内无法恢复，应尽快转入死信或非阻塞重试链路。

### 不可重试异常配置

不可重试异常是指再次执行也不会成功，或者不应该继续重试的异常。例如消息格式错误、参数缺失、反序列化失败、业务状态非法、权限错误等。这类异常应直接进入死信或失败记录，不应占用重试资源。

推荐自定义异常如下：

文件位置：`src/main/java/io/github/atengk/kafka/exception/NonRetryableBusinessException.java`

该异常用于标识不可重试业务异常，例如参数缺失、状态非法、消息版本不兼容等。

```java
package io.github.atengk.kafka.exception;

/**
 * 不可重试业务异常
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class NonRetryableBusinessException extends RuntimeException {

    /**
     * 构造不可重试业务异常
     *
     * @param message 异常信息
     */
    public NonRetryableBusinessException(String message) {
        super(message);
    }

    /**
     * 构造不可重试业务异常
     *
     * @param message 异常信息
     * @param cause   原始异常
     */
    public NonRetryableBusinessException(String message, Throwable cause) {
        super(message, cause);
    }

}
```

文件位置：`src/main/java/io/github/atengk/kafka/exception/RetryableBusinessException.java`

该异常用于标识可重试业务异常，例如下游服务短暂不可用、数据库临时异常等。

```java
package io.github.atengk.kafka.exception;

/**
 * 可重试业务异常
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class RetryableBusinessException extends RuntimeException {

    /**
     * 构造可重试业务异常
     *
     * @param message 异常信息
     */
    public RetryableBusinessException(String message) {
        super(message);
    }

    /**
     * 构造可重试业务异常
     *
     * @param message 异常信息
     * @param cause   原始异常
     */
    public RetryableBusinessException(String message, Throwable cause) {
        super(message, cause);
    }

}
```

错误处理器中配置不可重试异常：

```java
errorHandler.addNotRetryableExceptions(
        DeserializationException.class,
        IllegalArgumentException.class,
        NonRetryableBusinessException.class
);
```

不可重试异常判断建议如下：

| 异常类型       | 是否重试 | 原因                       |
| -------------- | -------- | -------------------------- |
| 反序列化失败   | 否       | 消息格式错误，重试无法恢复 |
| 参数缺失       | 否       | 消息体不合法               |
| 消息版本不兼容 | 否       | 需要升级消费者或转换消息   |
| 业务状态非法   | 否       | 重试不会改变状态           |
| 数据库连接超时 | 是       | 可能是短暂故障             |
| 下游接口超时   | 是       | 可能是短暂故障             |
| 第三方限流     | 视情况   | 短重试或延迟重试           |

### 死信 Topic 设计

死信 Topic 用于保存多次重试仍失败、不可重试或无法正常处理的消息。死信 Topic 不是最终处理结果，而是异常消息的隔离区。后续需要通过人工排查、修复数据、重放消息或降级处理完成闭环。

死信 Topic 命名建议如下：

```text
{系统标识}.{业务域}.{事件名称}.dead-letter
```

示例：

| 原始 Topic                    | 死信 Topic                          |
| ----------------------------- | ----------------------------------- |
| `ateng.order.created.topic`   | `ateng.order.created.dead-letter`   |
| `ateng.payment.success.topic` | `ateng.payment.success.dead-letter` |
| `ateng.user.login-log.topic`  | `ateng.user.login-log.dead-letter`  |

如果项目早期不想为每个业务 Topic 建独立死信 Topic，也可以先使用通用死信 Topic：

```text
ateng.common.dead-letter.topic
```

两种设计对比如下：

| 设计方式       | 优点                             | 缺点                                   |
| -------------- | -------------------------------- | -------------------------------------- |
| 独立死信 Topic | 业务隔离清晰，便于单独监控和重放 | Topic 数量较多                         |
| 通用死信 Topic | 管理简单，接入成本低             | 需要通过 Header 或消息体区分原始 Topic |
| 混合方式       | 重要业务独立，普通业务通用       | 需要明确治理规则                       |

死信消息应保留以下信息：

| 字段           | 说明               |
| -------------- | ------------------ |
| 原始 Topic     | 消息来源 Topic     |
| 原始 Partition | 消息来源 Partition |
| 原始 Offset    | 消息来源 Offset    |
| 原始 Key       | 消息 Key           |
| 原始 Headers   | 消息 Header        |
| 原始 Value     | 原始消息体         |
| 异常类型       | 失败异常类名       |
| 异常信息       | 失败原因摘要       |
| 失败时间       | 进入死信时间       |
| 重试次数       | 已重试次数         |
| Consumer Group | 消费组名称         |

死信 Topic 保留时间建议比普通业务 Topic 更长，例如 14 到 30 天，便于问题排查和人工处理。

### DeadLetterPublishingRecoverer 使用

`DeadLetterPublishingRecoverer` 用于在消息超过重试次数后，将失败消息重新发布到死信 Topic。它通常与 `DefaultErrorHandler` 配合使用。

文件位置：`src/main/java/io/github/atengk/kafka/config/KafkaDeadLetterConfig.java`

该配置类用于定义死信恢复器，并根据原始 Topic 路由到对应死信 Topic。

```java
package io.github.atengk.kafka.config;

import io.github.atengk.kafka.constant.KafkaTopicConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;

import java.util.Map;

/**
 * Kafka 死信投递配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Configuration
public class KafkaDeadLetterConfig {

    private static final Map<String, String> DEAD_LETTER_TOPIC_MAPPING = Map.of(
            KafkaTopicConstant.ORDER_CREATED_TOPIC, KafkaTopicConstant.ORDER_CREATED_DEAD_LETTER_TOPIC,
            KafkaTopicConstant.PAYMENT_SUCCESS_TOPIC, KafkaTopicConstant.PAYMENT_SUCCESS_DEAD_LETTER_TOPIC
    );

    /**
     * 配置死信消息发布恢复器
     *
     * @param kafkaTemplate Kafka 消息发送模板
     * @return 死信消息发布恢复器
     */
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaTemplate<String, Object> kafkaTemplate) {
        return new DeadLetterPublishingRecoverer(kafkaTemplate, (record, exception) -> {
            String deadLetterTopic = DEAD_LETTER_TOPIC_MAPPING.getOrDefault(
                    record.topic(),
                    KafkaTopicConstant.DEAD_LETTER_TOPIC
            );

            log.error("Kafka消息准备投递死信Topic，sourceTopic={}，deadLetterTopic={}，partition={}，offset={}，key={}",
                    record.topic(), deadLetterTopic, record.partition(), record.offset(), record.key(), exception);

            return new TopicPartition(deadLetterTopic, record.partition());
        });
    }

}
```

对应 Topic 常量示例：

```java
package io.github.atengk.kafka.constant;

/**
 * Kafka Topic 常量
 *
 * @author Ateng
 * @since 2026-05-11
 */
public final class KafkaTopicConstant {

    /**
     * 订单创建 Topic
     */
    public static final String ORDER_CREATED_TOPIC = "ateng.order.created.topic";

    /**
     * 支付成功 Topic
     */
    public static final String PAYMENT_SUCCESS_TOPIC = "ateng.payment.success.topic";

    /**
     * 订单创建死信 Topic
     */
    public static final String ORDER_CREATED_DEAD_LETTER_TOPIC = "ateng.order.created.dead-letter";

    /**
     * 支付成功死信 Topic
     */
    public static final String PAYMENT_SUCCESS_DEAD_LETTER_TOPIC = "ateng.payment.success.dead-letter";

    /**
     * 通用死信 Topic
     */
    public static final String DEAD_LETTER_TOPIC = "ateng.common.dead-letter.topic";

    private KafkaTopicConstant() {
    }

}
```

将 `DeadLetterPublishingRecoverer` 注入 `DefaultErrorHandler`：

```java
@Bean
public CommonErrorHandler kafkaCommonErrorHandler(DeadLetterPublishingRecoverer recoverer) {
    FixedBackOff fixedBackOff = new FixedBackOff(2000L, 3L);
    DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, fixedBackOff);

    errorHandler.addNotRetryableExceptions(
            DeserializationException.class,
            IllegalArgumentException.class,
            NonRetryableBusinessException.class
    );

    return errorHandler;
}
```

### 死信消息处理

死信消息处理用于对进入死信 Topic 的异常消息进行排查、修复和重放。死信消息不能只进入 Topic 后无人处理，否则死信 Topic 会变成新的消息垃圾堆。

死信处理流程建议如下：

```text
消费失败
  -> 达到最大重试次数
  -> 投递死信 Topic
  -> 记录死信日志或死信表
  -> 告警通知
  -> 人工或自动分析失败原因
  -> 修复业务数据或程序缺陷
  -> 重放死信消息
  -> 标记处理完成
```

死信消费者示例：

```java
package io.github.atengk.kafka.consumer;

import io.github.atengk.kafka.constant.KafkaGroupConstant;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka 死信消息消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class KafkaDeadLetterConsumer {

    /**
     * 消费通用死信消息
     *
     * @param record         Kafka 死信消息
     * @param acknowledgment Offset 确认对象
     */
    @KafkaListener(
            topics = KafkaTopicConstant.DEAD_LETTER_TOPIC,
            groupId = KafkaGroupConstant.DEAD_LETTER_CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeDeadLetter(ConsumerRecord<String, Object> record,
                                  Acknowledgment acknowledgment) {
        try {
            log.error("收到Kafka死信消息，topic={}，partition={}，offset={}，key={}，value={}",
                    record.topic(), record.partition(), record.offset(), record.key(), record.value());

            // 建议在此处写入死信消息表，或触发告警，不建议直接自动重放
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Kafka死信消息处理失败，topic={}，partition={}，offset={}，key={}",
                    record.topic(), record.partition(), record.offset(), record.key(), e);
            throw e;
        }
    }

}
```

死信处理要求如下：

1. 死信消息必须可查询。
2. 死信增长必须接入告警。
3. 死信重放前必须确认失败原因已经修复。
4. 死信重放必须保留原始 `messageId` 或 `originalMessageId`。
5. 死信重放仍需做幂等，避免重复执行业务。
6. 死信消息表应记录处理状态，例如 `PENDING`、`REPLAYED`、`IGNORED`、`FAILED`。

死信消息表建议：

```sql
CREATE TABLE kafka_dead_letter_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(64) DEFAULT NULL COMMENT '消息唯一标识',
    source_topic VARCHAR(128) NOT NULL COMMENT '原始Topic',
    source_partition INT NOT NULL COMMENT '原始分区',
    source_offset BIGINT NOT NULL COMMENT '原始Offset',
    message_key VARCHAR(255) DEFAULT NULL COMMENT '消息Key',
    message_value TEXT NOT NULL COMMENT '消息内容',
    exception_class VARCHAR(255) DEFAULT NULL COMMENT '异常类名',
    exception_message VARCHAR(1000) DEFAULT NULL COMMENT '异常信息',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    process_status VARCHAR(32) NOT NULL COMMENT '处理状态',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    KEY idx_message_id (message_id),
    KEY idx_source_topic_offset (source_topic, source_partition, source_offset),
    KEY idx_process_status (process_status)
) COMMENT='Kafka死信消息表';
```

### 异常日志规范

异常日志是 Kafka 问题排查的基础。消费异常日志必须做到“能定位消息、能定位业务、能定位链路、能定位失败原因”。

消费异常日志必须包含以下字段：

| 字段               | 说明         |
| ------------------ | ------------ |
| `topic`            | 消息 Topic   |
| `partition`        | 消息分区     |
| `offset`           | 消息 Offset  |
| `key`              | 消息 Key     |
| `messageId`        | 消息唯一标识 |
| `traceId`          | 链路追踪 ID  |
| `consumerGroup`    | 消费组       |
| `messageType`      | 消息类型     |
| `exceptionClass`   | 异常类名     |
| `exceptionMessage` | 异常摘要     |
| `retryCount`       | 当前重试次数 |

推荐日志格式：

```text
Kafka消息消费失败，topic={}，partition={}，offset={}，key={}，messageId={}，traceId={}，consumerGroup={}，messageType={}，retryCount={}
```

异常日志要求如下：

1. 错误日志必须打印异常堆栈。
2. 日志中必须包含 Topic、Partition、Offset。
3. 重要业务必须打印 `messageId` 和 `traceId`。
4. 不应打印完整敏感消息体，例如密码、Token、证件号、密钥等。
5. 批量消费失败时，应打印批量数量和失败消息位置。
6. 死信投递日志应打印原始 Topic 和死信 Topic。
7. 重试日志建议使用 `warn` 级别，最终失败使用 `error` 级别。

## 消息重试机制

本章节用于说明 Kafka 消息重试机制，包括 Producer 重试、Consumer 重试、阻塞重试、非阻塞重试、Retry Topic 设计、延迟重试设计、重试次数记录、重试消息幂等和重试失败降级。

消息重试的目标是处理临时性失败，例如网络抖动、数据库短暂不可用、下游接口超时、服务限流等。重试不能解决所有问题，对于消息格式错误、参数缺失、业务状态非法、权限不足等不可恢复问题，应直接进入死信或失败记录。

### Producer 重试

Producer 重试发生在消息发送到 Kafka 的阶段，主要用于处理 Broker 临时不可用、网络抖动、Leader 切换、请求超时等问题。

推荐配置如下：

```yaml
spring:
  kafka:
    producer:
      # 等待同步副本确认，提高写入可靠性
      acks: all

      # 发送失败自动重试次数
      retries: 3

      properties:
        # 开启幂等 Producer，降低重试导致的重复写入风险
        enable.idempotence: true

        # 消息发送完整生命周期超时时间
        delivery.timeout.ms: 120000

        # 单次请求 Broker 响应超时时间
        request.timeout.ms: 30000

        # 开启幂等时建议不超过 5
        max.in.flight.requests.per.connection: 5
```

Producer 重试适合解决“消息还没有成功写入 Kafka”的问题。它不能解决消息已经写入 Kafka 后 Consumer 业务处理失败的问题。

Producer 重试注意事项如下：

1. `retries` 受 `delivery.timeout.ms` 限制。
2. 重要消息应开启 `enable.idempotence=true`。
3. 发送失败最终仍需要记录失败日志或写入本地消息表。
4. 序列化失败、认证失败、权限不足不应依赖重试解决。
5. 对外接口中同步等待 Kafka 发送结果时，需要设置合理超时时间。

### Consumer 重试

Consumer 重试发生在消息已经写入 Kafka 后，消费端处理业务失败的阶段。常见方式包括阻塞重试和非阻塞重试。

Consumer 重试分类如下：

| 类型       | 说明                                                   | 适用场景                             |
| ---------- | ------------------------------------------------------ | ------------------------------------ |
| 阻塞重试   | 当前线程等待一段时间后重新处理同一条消息               | 短暂异常，重试时间短                 |
| 非阻塞重试 | 失败消息发送到 Retry Topic，主消费线程继续处理后续消息 | 延迟较长、失败量较大、避免阻塞主链路 |
| 手动补偿   | 记录失败状态，由补偿任务或人工重放                     | 复杂业务、需要人工确认               |
| 死信处理   | 多次失败后进入死信 Topic                               | 最终失败隔离                         |

Consumer 重试设计要求如下：

1. 只对可恢复异常重试。
2. 不可重试异常直接进入死信或失败记录。
3. 重试期间不能破坏业务幂等。
4. 重试次数必须可记录、可查询、可告警。
5. 重试失败后必须有最终处置方式，不能无限循环。
6. 阻塞重试和非阻塞重试不能混用得过于复杂，应按场景分层设计。

### 阻塞重试

阻塞重试是指消费失败后，当前 Consumer 线程等待一段时间，再重新处理同一条消息。在 Spring Kafka 中，`DefaultErrorHandler` 配合 `FixedBackOff` 就是常见的阻塞重试方式。

阻塞重试配置示例：

```java
FixedBackOff fixedBackOff = new FixedBackOff(2000L, 3L);
DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, fixedBackOff);
```

阻塞重试流程如下：

```text
消费消息
  -> 业务处理失败
  -> 等待 2 秒
  -> 第 1 次重试
  -> 失败后继续等待 2 秒
  -> 第 2 次重试
  -> 失败后继续等待 2 秒
  -> 第 3 次重试
  -> 仍失败
  -> 投递死信 Topic
```

阻塞重试适用场景：

| 场景               | 说明             |
| ------------------ | ---------------- |
| 数据库短暂连接失败 | 短时间后可能恢复 |
| 下游服务短暂超时   | 少量重试即可恢复 |
| Redis 短暂抖动     | 适合短间隔重试   |
| 偶发网络异常       | 重试成本较低     |

阻塞重试不适合以下场景：

| 场景                         | 原因                     |
| ---------------------------- | ------------------------ |
| 下游长时间不可用             | 会持续阻塞当前 Partition |
| 大量消息同时失败             | 容易造成消费堆积         |
| 需要延迟数分钟或数小时重试   | 不适合占用消费线程       |
| 消息顺序要求不高但吞吐要求高 | 阻塞会降低整体吞吐       |

### 非阻塞重试

非阻塞重试是指消费失败后，不在当前消费线程中等待，而是将失败消息发送到专门的 Retry Topic，主消费线程继续处理后续消息。Retry Topic 中的消息经过延迟后再次进入消费链路。

非阻塞重试适合以下场景：

| 场景             | 说明                               |
| ---------------- | ---------------------------------- |
| 下游接口限流     | 需要延迟一段时间后重试             |
| 第三方服务不可用 | 不希望阻塞主 Topic 消费            |
| 失败量较大       | 防止主消费线程被失败消息占满       |
| 重试周期较长     | 例如 1 分钟、5 分钟、30 分钟后重试 |
| 顺序要求不强     | 允许后续消息先处理                 |

非阻塞重试流程如下：

```text
主 Topic 消费失败
  -> 判断是否可重试
  -> 写入 Retry Topic
  -> 主 Topic 提交 Offset
  -> Retry Topic 延迟后重新消费
  -> 成功则结束
  -> 失败且未达最大次数则进入下一级 Retry Topic
  -> 超过最大次数后进入死信 Topic
```

需要注意，非阻塞重试会改变消息处理顺序。如果某个业务要求同一 Key 严格顺序处理，不应随意使用非阻塞重试，否则后续消息可能先于失败消息完成处理。

### Retry Topic 设计

Retry Topic 用于存放等待重试的失败消息。推荐按业务 Topic 和重试级别设计 Retry Topic。

命名格式如下：

```text
{系统标识}.{业务域}.{事件名称}.retry.{级别}
```

示例：

| 重试级别    | Retry Topic                       |
| ----------- | --------------------------------- |
| 第 1 级重试 | `ateng.order.created.retry.1m`    |
| 第 2 级重试 | `ateng.order.created.retry.5m`    |
| 第 3 级重试 | `ateng.order.created.retry.30m`   |
| 最终失败    | `ateng.order.created.dead-letter` |

Retry Topic 设计建议如下：

| 配置项   | 建议                               |
| -------- | ---------------------------------- |
| 分区数   | 与原始 Topic 保持一致              |
| 副本数   | 生产环境建议为 3                   |
| 保留时间 | 大于最大重试周期                   |
| Key      | 保持原始消息 Key                   |
| Header   | 保留原始 Header，并追加重试信息    |
| 消息体   | 保留原始消息体，避免重试时结构变化 |

Retry Topic 配置示例：

```yaml
app:
  kafka:
    topic:
      auto-create: true
      topics:
        - name: ateng.order.created.retry.1m
          partitions: 3
          replicas: 1
          configs:
            # 重试消息保留 1 天
            retention.ms: "86400000"
            cleanup.policy: delete

        - name: ateng.order.created.retry.5m
          partitions: 3
          replicas: 1
          configs:
            retention.ms: "86400000"
            cleanup.policy: delete

        - name: ateng.order.created.dead-letter
          partitions: 3
          replicas: 1
          configs:
            # 死信消息保留 14 天
            retention.ms: "1209600000"
            cleanup.policy: delete
```

### 延迟重试设计

Kafka 原生不提供像延迟队列一样的精确延迟消息能力。延迟重试通常需要结合 Retry Topic、定时任务、数据库任务表、Redis ZSet 或外部调度系统实现。

常见方案如下：

| 方案                   | 说明                                      | 适用场景                 |
| ---------------------- | ----------------------------------------- | ------------------------ |
| Retry Topic + 定时扫描 | 失败消息写入数据库，到期后重新发送        | 可靠性要求高，可查询     |
| Redis ZSet             | 使用 score 存储下次重试时间，到期取出重发 | 中低成本延迟重试         |
| 多级 Retry Topic       | 按 1m、5m、30m 分级重试                   | 重试级别固定             |
| Kafka 延迟消费         | Consumer 读取后判断时间未到则暂停或跳过   | 实现复杂，不建议通用使用 |
| XXL-JOB / 调度任务     | 定时扫描失败表并重发                      | 企业项目常用             |

推荐采用“失败记录表 + 定时任务重发”方式实现可靠延迟重试，特别适合重要业务消息。

重试记录表建议：

```sql
CREATE TABLE kafka_retry_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息唯一标识',
    source_topic VARCHAR(128) NOT NULL COMMENT '原始Topic',
    retry_topic VARCHAR(128) DEFAULT NULL COMMENT '重试Topic',
    message_key VARCHAR(255) DEFAULT NULL COMMENT '消息Key',
    message_value TEXT NOT NULL COMMENT '消息内容',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    max_retry_count INT NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    next_retry_time DATETIME NOT NULL COMMENT '下次重试时间',
    retry_status VARCHAR(32) NOT NULL COMMENT '重试状态',
    last_error_message VARCHAR(1000) DEFAULT NULL COMMENT '最近一次异常信息',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_message_retry (message_id, retry_status),
    KEY idx_next_retry_time (next_retry_time),
    KEY idx_retry_status (retry_status)
) COMMENT='Kafka消息重试记录表';
```

重试状态建议：

| 状态          | 说明       |
| ------------- | ---------- |
| `PENDING`     | 待重试     |
| `RETRYING`    | 重试中     |
| `SUCCESS`     | 重试成功   |
| `FAILED`      | 重试失败   |
| `DEAD_LETTER` | 已进入死信 |
| `IGNORED`     | 已忽略     |

### 重试次数记录

重试次数必须显式记录，不能只依赖内存计数。否则应用重启、消息重新投递或死信重放时，无法判断该消息已经重试过多少次。

重试次数可以记录在以下位置：

| 位置       | 说明                             |
| ---------- | -------------------------------- |
| Header     | 轻量记录，便于 Retry Topic 传递  |
| 消息体     | 可读性较好，但会污染业务模型     |
| 重试记录表 | 最可靠，便于查询、告警和人工处理 |
| 死信表     | 记录最终失败前的重试次数         |

推荐 Header 常量：

```java
package io.github.atengk.kafka.constant;

/**
 * Kafka Header 常量
 *
 * @author Ateng
 * @since 2026-05-11
 */
public final class KafkaHeaderConstant {

    /**
     * 消息唯一标识
     */
    public static final String MESSAGE_ID = "x-message-id";

    /**
     * 链路追踪 ID
     */
    public static final String TRACE_ID = "x-trace-id";

    /**
     * 重试次数
     */
    public static final String RETRY_COUNT = "x-retry-count";

    /**
     * 原始 Topic
     */
    public static final String ORIGINAL_TOPIC = "x-original-topic";

    /**
     * 原始 Partition
     */
    public static final String ORIGINAL_PARTITION = "x-original-partition";

    /**
     * 原始 Offset
     */
    public static final String ORIGINAL_OFFSET = "x-original-offset";

    private KafkaHeaderConstant() {
    }

}
```

读取重试次数示例：

```java
String retryCountValue = KafkaHeaderSupport.getString(record.headers(), KafkaHeaderConstant.RETRY_COUNT);
int retryCount = NumberUtil.parseInt(retryCountValue, 0);
```

如果使用 Hutool，可以通过 `NumberUtil.parseInt` 对空值或非法数字进行兜底处理。需要注意，Header 中的重试次数只适合消息流转时传递，最终仍建议落入重试记录表或死信表。

### 重试消息幂等

重试消息幂等用于确保同一消息被多次消费时，不会造成重复写库、重复扣减、重复通知、重复生成业务记录等问题。所有 Consumer 重试机制都必须以幂等为前提。

重试场景下可能出现的重复包括：

| 重复来源          | 说明                                     |
| ----------------- | ---------------------------------------- |
| Producer 重试     | 发送阶段可能产生重复业务消息             |
| Consumer 阻塞重试 | 同一条消息会多次进入业务处理             |
| Retry Topic 重试  | 失败消息被重新发送并再次消费             |
| 死信重放          | 人工或自动重放导致再次处理               |
| Offset 提交失败   | 业务成功但 Offset 未提交，重启后再次消费 |
| 调度任务重复执行  | 重试任务并发扫描导致重复重发             |

幂等设计建议如下：

| 方式                                 | 说明                                          |
| ------------------------------------ | --------------------------------------------- |
| `messageId + consumerGroup` 唯一约束 | 防止同一消费组重复处理同一消息                |
| 业务唯一键约束                       | 防止不同 messageId 对同一业务重复处理         |
| 消费状态表                           | 记录 `PROCESSING`、`SUCCESS`、`FAILED` 等状态 |
| Redis 短期去重                       | 适合高频短期重复过滤                          |
| 分布式锁                             | 防止同一业务 Key 并发处理                     |
| 状态机校验                           | 根据业务状态判断是否允许处理                  |

幂等处理流程建议：

```text
收到消息
  -> 获取 messageId 和 consumerGroup
  -> 查询消费记录
  -> 如果已 SUCCESS，直接跳过并提交 Offset
  -> 如果未处理，插入 PROCESSING 记录
  -> 执行业务处理
  -> 成功后更新为 SUCCESS
  -> 提交 Offset
  -> 失败后更新为 FAILED，并抛出异常进入重试
```

消费状态表建议在“幂等性设计”章节中进一步展开。此处需要明确：没有幂等控制的消息，不应启用自动重试和死信重放。

### 重试失败降级

重试失败降级用于处理多次重试仍失败的消息。降级不等于忽略错误，而是将失败消息从主消费链路中隔离出来，避免持续阻塞正常消息，同时保留后续恢复能力。

常见降级方式如下：

| 降级方式         | 说明                               |
| ---------------- | ---------------------------------- |
| 投递死信 Topic   | 保留失败消息，等待人工或自动处理   |
| 写入失败记录表   | 记录失败详情，便于查询和补偿       |
| 触发告警         | 通知开发、运维或业务负责人         |
| 跳过非核心逻辑   | 例如积分、通知类非主链路可后续补偿 |
| 关闭异常消费逻辑 | 极端情况下临时关闭故障消费者       |
| 人工重放         | 修复问题后由管理接口重放消息       |

重试失败处理流程建议：

```text
消费失败
  -> 判断是否可重试
  -> 达到最大重试次数
  -> 写入死信 Topic
  -> 写入死信表
  -> 触发告警
  -> 主消费链路继续处理其他消息
  -> 修复后重放
```

重试失败降级要求如下：

1. 重要业务失败必须可查询。
2. 死信和失败记录必须有告警。
3. 降级不能导致核心状态不一致。
4. 重放前必须确认幂等能力。
5. 非核心通知类失败可以延后补偿，但不能静默丢弃。
6. 如果失败量突然增加，应优先排查下游服务、数据库、消息格式和版本兼容问题。

最终建议是：短暂异常使用阻塞重试，较长延迟使用 Retry Topic 或任务表，最终失败进入死信 Topic，并通过死信表、告警和重放接口形成闭环。


## 幂等性设计

本章节用于说明 Kafka 消费链路中的幂等设计，包括幂等场景分析、消息唯一键、数据库唯一约束、Redis 去重、本地缓存去重、消费状态表、幂等过期策略和并发幂等控制。

Kafka Consumer 必须按照“消息可能被重复消费”的前提进行设计。即使 Producer、Broker、Consumer 配置都比较可靠，也仍然可能因为 Producer 重发、Consumer 重试、Offset 提交失败、应用重启、Rebalance、死信重放、人工补偿等原因导致同一业务消息被多次处理。因此，Kafka 消费端的核心原则不是假设消息只会消费一次，而是保证重复消费不会产生重复业务副作用。

### 幂等场景分析

幂等场景分析用于识别哪些业务处理可能因重复消费产生副作用。只要消息消费后会写数据库、调下游接口、扣减库存、发放权益、发送通知或改变业务状态，就必须考虑幂等。

常见重复消费场景如下：

| 场景            | 说明                                     | 幂等要求                     |
| --------------- | ---------------------------------------- | ---------------------------- |
| Offset 提交失败 | 业务处理成功，但 Offset 没有提交成功     | 再次消费时不能重复执行业务   |
| Consumer 重启   | 应用宕机或发布重启，未提交消息会再次消费 | 依赖消费记录或业务唯一键判断 |
| Rebalance       | 分区重新分配后，从已提交 Offset 继续消费 | 可能重复处理最后一批消息     |
| Consumer 重试   | 消费异常后多次重试同一条消息             | 每次重试都必须可重复执行     |
| Retry Topic     | 失败消息进入重试 Topic 后再次消费        | 保留原始 messageId 并做幂等  |
| 死信重放        | 修复问题后人工或自动重放死信消息         | 必须识别已成功处理过的消息   |
| Producer 重发   | 上游补偿任务重新发送同一业务事件         | 需要业务唯一键兜底           |
| 接口超时重试    | 消费端调用下游接口超时后重试             | 下游接口也需要幂等           |

幂等设计需要区分两类问题：

| 类型       | 说明                                                  | 示例                             |
| ---------- | ----------------------------------------------------- | -------------------------------- |
| 消息级幂等 | 同一个 `messageId` 不能被同一个消费组重复处理         | `messageId + consumerGroup` 唯一 |
| 业务级幂等 | 即使不同 `messageId` 表示同一业务动作，也不能重复处理 | `orderId + eventType` 唯一       |

例如，订单创建事件如果被重复消费，不能重复发放积分；支付成功事件如果被重复消费，不能重复更新支付状态或重复发货。对于这类业务，仅依赖 Kafka Offset 不够，必须在业务侧建立幂等保护。

### 消息唯一键设计

消息唯一键是幂等处理的基础。每条 Kafka 消息必须具备全局唯一的 `messageId`，同时重要业务消息还应具备稳定的业务唯一键，例如订单 ID、支付单号、退款单号、用户 ID 或业务流水号。

推荐唯一键设计如下：

| 唯一键              | 作用                | 示例                         |
| ------------------- | ------------------- | ---------------------------- |
| `messageId`         | 标识一条 Kafka 消息 | `1900000000000000001`        |
| `consumerGroup`     | 标识当前消费逻辑    | `ateng.order.consumer.group` |
| `bizKey`            | 标识业务主体        | `ORDER:10001`                |
| `eventType`         | 标识业务事件类型    | `ORDER_CREATED`              |
| `businessUniqueKey` | 标识业务动作唯一性  | `ORDER:10001:ORDER_CREATED`  |

推荐组合方式如下：

```text
消息级幂等键 = messageId + consumerGroup
业务级幂等键 = bizKey + eventType
```

示例：

```text
messageId = 1900000000000000001
consumerGroup = ateng.points.consumer.group
messageIdempotentKey = 1900000000000000001:ateng.points.consumer.group

bizKey = ORDER:10001
eventType = ORDER_CREATED
businessIdempotentKey = ORDER:10001:ORDER_CREATED
```

设计要求如下：

1. `messageId` 必须由 Producer 生成，并写入消息体和 Header。
2. `consumerGroup` 必须稳定，不能每次发布动态变化。
3. 同一类业务事件必须使用一致的 `bizKey` 规则。
4. 如果同一业务动作可能被上游多次发送，必须增加业务级唯一约束。
5. 死信重放时应保留原始 `messageId`，不要直接覆盖。
6. 如果重放需要生成新的消息 ID，应额外保留 `originalMessageId`。

### 数据库唯一约束

数据库唯一约束是最可靠的幂等方式之一。它依赖数据库唯一索引保证同一个幂等键只能写入一次，适合订单、支付、库存、权益发放、资金流水、审计记录等重要业务。

消费幂等表建议如下：

```sql
CREATE TABLE kafka_consume_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息唯一标识',
    consumer_group VARCHAR(128) NOT NULL COMMENT '消费组',
    business_key VARCHAR(255) DEFAULT NULL COMMENT '业务幂等键',
    topic VARCHAR(128) NOT NULL COMMENT 'Topic名称',
    partition_id INT NOT NULL COMMENT '分区编号',
    offset_value BIGINT NOT NULL COMMENT 'Offset',
    consume_status VARCHAR(32) NOT NULL COMMENT '消费状态：PROCESSING、SUCCESS、FAILED',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '异常信息',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_message_group (message_id, consumer_group),
    UNIQUE KEY uk_business_group (business_key, consumer_group),
    KEY idx_topic_partition_offset (topic, partition_id, offset_value),
    KEY idx_consume_status (consume_status)
) COMMENT='Kafka消费幂等记录表';
```

唯一索引说明如下：

| 索引                         | 作用                                   |
| ---------------------------- | -------------------------------------- |
| `uk_message_group`           | 防止同一个消息在同一个消费组中重复处理 |
| `uk_business_group`          | 防止同一业务动作被不同消息重复触发     |
| `idx_topic_partition_offset` | 便于根据 Kafka 消费位置排查问题        |
| `idx_consume_status`         | 便于扫描失败、处理中或待补偿记录       |

数据库幂等处理流程如下：

```text
收到消息
  -> 生成 messageId + consumerGroup 幂等键
  -> 尝试插入 PROCESSING 记录
  -> 插入成功，执行业务逻辑
  -> 业务成功，更新为 SUCCESS
  -> 提交 Offset
  -> 插入失败，查询原记录
  -> 如果原记录为 SUCCESS，直接跳过并提交 Offset
  -> 如果原记录为 PROCESSING 或 FAILED，按策略重试或补偿
```

需要注意，数据库唯一约束不仅用于防重复，也用于提供可查询的消费状态。重要业务不建议只依赖 Redis 或本地缓存做幂等，因为缓存可能过期、丢失或被清理。

### Redis 去重

Redis 去重适合短时间内的高频重复消息过滤，例如日志、通知、行为事件、非资金类异步任务等。Redis 去重性能较好，但可靠性弱于数据库唯一约束，不适合作为核心资金、库存、订单状态变更的唯一幂等保障。

Redis 幂等常用方式是 `SET key value NX EX seconds`。只有第一次设置成功才执行业务逻辑，后续相同 Key 在过期前会被识别为重复。

文件位置：`src/main/java/io/github/atengk/kafka/idempotent/RedisIdempotentService.java`

该服务用于基于 Redis 实现短期消息去重，适合非核心但高频的消费场景。

```java
package io.github.atengk.kafka.idempotent;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis 幂等服务
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisIdempotentService {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试占用幂等键
     *
     * @param key     幂等键
     * @param timeout 过期时间
     * @return true 表示首次处理，false 表示重复消息
     */
    public boolean tryAcquire(String key, Duration timeout) {
        if (StrUtil.isBlank(key)) {
            throw new IllegalArgumentException("Redis幂等键不能为空");
        }

        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", timeout);

        boolean acquired = Boolean.TRUE.equals(success);
        if (!acquired) {
            log.warn("检测到重复Kafka消息，idempotentKey={}", key);
        }
        return acquired;
    }

    /**
     * 删除幂等键
     *
     * @param key 幂等键
     */
    public void release(String key) {
        if (StrUtil.isBlank(key)) {
            return;
        }
        stringRedisTemplate.delete(key);
        log.info("释放Kafka幂等键，idempotentKey={}", key);
    }

}
```

使用示例：

```java
String idempotentKey = "kafka:consume:" + messageId + ":" + consumerGroup;
boolean firstConsume = redisIdempotentService.tryAcquire(idempotentKey, Duration.ofHours(24));

if (!firstConsume) {
    log.info("Kafka消息已处理或处理中，跳过重复消费，messageId={}，consumerGroup={}", messageId, consumerGroup);
    acknowledgment.acknowledge();
    return;
}

// 执行业务逻辑
```

Redis 去重注意事项如下：

1. Redis 去重必须设置过期时间，避免 Key 无限增长。
2. 过期时间必须大于消息最大重试周期和可接受重复窗口。
3. Redis 宕机或数据淘汰可能导致幂等失效。
4. 业务处理失败时是否删除 Key，需要按场景决定。
5. 核心业务应使用数据库唯一约束兜底，不应只依赖 Redis。

### 本地缓存去重

本地缓存去重适合短时间内的重复消息过滤，例如同一实例内由于快速重试导致的重复处理。它性能高、实现简单，但只在单个应用实例内有效，不适合分布式强幂等。

常见本地缓存方案如下：

| 方案              | 说明                                 |
| ----------------- | ------------------------------------ |
| Caffeine          | 推荐，支持过期、容量限制和高性能访问 |
| Guava Cache       | 可用，但新项目更推荐 Caffeine        |
| ConcurrentHashMap | 简单可用，但需要自行处理过期和容量   |
| 本地 LRU          | 可自定义，但维护成本较高             |

本地缓存适用场景：

| 场景               | 是否适合 |
| ------------------ | -------- |
| 日志类重复过滤     | 适合     |
| 短时间重复重试拦截 | 适合     |
| 核心订单状态变更   | 不适合   |
| 支付成功处理       | 不适合   |
| 多实例全局幂等     | 不适合   |

本地缓存去重要求如下：

1. 必须设置最大容量，避免内存无限增长。
2. 必须设置过期时间，避免长期占用内存。
3. 只能作为性能优化手段，不能作为核心幂等唯一依据。
4. 多实例部署时，每个实例的本地缓存互不共享。
5. 应与数据库或 Redis 幂等配合使用。

本地缓存更适合作为“第一层快速过滤”，数据库或 Redis 作为“最终幂等判断”。

### 消费状态表

消费状态表用于记录每条消息在某个消费组中的处理状态。它不仅能做幂等，还能支持异常排查、失败补偿、人工重放、消费审计和监控统计。

推荐状态如下：

| 状态          | 说明       |
| ------------- | ---------- |
| `PROCESSING`  | 正在处理   |
| `SUCCESS`     | 处理成功   |
| `FAILED`      | 处理失败   |
| `DEAD_LETTER` | 已进入死信 |
| `IGNORED`     | 已忽略     |
| `REPLAYED`    | 已重放     |

消费状态流转如下：

```text
收到消息
  -> PROCESSING
  -> SUCCESS

收到消息
  -> PROCESSING
  -> FAILED
  -> 重试
  -> SUCCESS

收到消息
  -> PROCESSING
  -> FAILED
  -> 超过最大重试次数
  -> DEAD_LETTER

重复消息
  -> 查询到 SUCCESS
  -> IGNORED 或直接跳过
```

文件位置：`src/main/java/io/github/atengk/kafka/enums/ConsumeStatusEnum.java`

该枚举用于定义 Kafka 消费状态，便于消费记录表和业务代码统一使用。

```java
package io.github.atengk.kafka.enums;

import lombok.Getter;

/**
 * Kafka 消费状态枚举
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Getter
public enum ConsumeStatusEnum {

    /**
     * 处理中
     */
    PROCESSING("处理中"),

    /**
     * 处理成功
     */
    SUCCESS("处理成功"),

    /**
     * 处理失败
     */
    FAILED("处理失败"),

    /**
     * 已进入死信
     */
    DEAD_LETTER("已进入死信"),

    /**
     * 已忽略
     */
    IGNORED("已忽略"),

    /**
     * 已重放
     */
    REPLAYED("已重放");

    private final String description;

    ConsumeStatusEnum(String description) {
        this.description = description;
    }

}
```

消费状态表使用要求如下：

1. 插入 `PROCESSING` 记录应和幂等判断绑定。
2. 业务成功后必须更新为 `SUCCESS`。
3. 业务失败后应记录异常摘要并更新为 `FAILED`。
4. 死信投递成功后应更新为 `DEAD_LETTER`。
5. 重复消费时如果状态为 `SUCCESS`，可以直接跳过并提交 Offset。
6. 如果状态长期停留在 `PROCESSING`，需要补偿任务识别并修复。

### 幂等过期策略

幂等数据不能无限保留，也不能过早删除。过期策略需要根据 Topic 保留时间、最大重试时间、死信保留时间、业务审计要求和数据量综合设计。

不同幂等方式的过期建议如下：

| 幂等方式       | 过期策略                                        |
| -------------- | ----------------------------------------------- |
| 数据库消费记录 | 核心业务保留 30 到 180 天，审计类按合规要求保留 |
| Redis 去重 Key | 保留时间大于最大重试周期，通常 1 到 7 天        |
| 本地缓存       | 几分钟到数小时，主要用于短期重复过滤            |
| 死信记录       | 建议保留 14 到 30 天或更长                      |
| 重试记录       | 成功后可保留 7 到 30 天，失败记录保留更久       |

幂等过期设计要求如下：

1. 幂等记录保留时间应大于 Kafka Topic 消息保留时间。
2. 幂等记录保留时间应大于最大重试周期。
3. 死信可重放期间，幂等记录不应提前清理。
4. 核心业务幂等记录不应只保留几小时。
5. 清理任务必须按时间范围分批删除，避免大事务影响数据库。
6. 清理前应确认是否还有补偿、重放或审计需求。

数据库清理示例：

```sql
DELETE FROM kafka_consume_record
WHERE consume_status IN ('SUCCESS', 'IGNORED')
  AND update_time < DATE_SUB(NOW(), INTERVAL 90 DAY)
LIMIT 1000;
```

该 SQL 用于分批清理 90 天以前的成功或忽略记录。生产环境建议通过定时任务循环分批执行，避免一次删除过多数据造成锁表或主从延迟。

### 并发幂等控制

并发幂等控制用于解决多个消费者线程、多个应用实例或重复消息同时处理同一业务 Key 的问题。并发场景下，仅靠先查再插容易出现竞态条件，必须依赖唯一约束、分布式锁或数据库行锁等机制保证原子性。

常见并发风险如下：

| 风险                           | 说明                             |
| ------------------------------ | -------------------------------- |
| 两个消费者同时处理同一业务 Key | 可能重复写业务数据               |
| 先查后插非原子                 | 两个线程都查不到，然后都执行处理 |
| Redis Key 设置不原子           | 非 `SET NX` 方式可能失效         |
| 本地缓存多实例不共享           | 不同实例可能同时处理             |
| 业务状态更新无条件             | 重复消息可能覆盖新状态           |

推荐控制方式如下：

| 方式              | 说明                       |
| ----------------- | -------------------------- |
| 数据库唯一索引    | 最可靠，适合作为最终防线   |
| Redis `SET NX`    | 适合短期并发互斥           |
| Redisson 分布式锁 | 适合复杂业务临界区         |
| 状态机条件更新    | 只允许合法状态流转         |
| 乐观锁版本号      | 防止并发覆盖               |
| 悲观锁            | 适合强一致但吞吐较低的场景 |

并发幂等处理建议：

```text
收到消息
  -> 使用 messageId + consumerGroup 做消息级唯一约束
  -> 使用 businessKey + consumerGroup 做业务级唯一约束
  -> 对核心业务状态更新增加条件判断
  -> 必要时使用 Redis / Redisson 锁控制同一业务 Key 并发
  -> 业务成功后提交 Offset
```

对于订单状态更新，推荐使用条件更新：

```sql
UPDATE order_info
SET order_status = 'PAID',
    update_time = NOW()
WHERE order_id = '10001'
  AND order_status = 'WAIT_PAY';
```

这种写法可以避免重复支付消息把已经取消、退款或关闭的订单错误更新为已支付。业务幂等不能只看消息是否重复，还要看当前业务状态是否允许执行该动作。

## 事务消息

本章节用于说明 Kafka 事务消息相关内容，包括 Kafka 事务基础、Producer 事务配置、`Transactional ID` 设计、`KafkaTemplate` 事务发送、本地事务与消息事务、事务回滚处理、Exactly Once 语义和事务使用限制。

Kafka 事务主要用于保证 Kafka 消息写入的原子性，尤其适用于“消费一个 Topic，处理后再发送到另一个 Topic”的 Kafka 内部流转场景。需要明确的是，Kafka 事务不能天然覆盖外部数据库事务。对于“写数据库 + 发 Kafka 消息”的场景，需要结合本地消息表、事务事件表、Outbox Pattern 或补偿任务来实现最终一致性。

### Kafka 事务基础

Kafka 事务允许 Producer 将多条消息作为一个原子单元提交到 Kafka。事务提交后，消费者才能读取到这些消息；事务回滚后，消费者不会读取到这些未提交消息。

Kafka 事务主要解决以下问题：

| 问题                 | 说明                                                 |
| -------------------- | ---------------------------------------------------- |
| 多条消息原子发送     | 多个 Topic 或 Partition 的消息要么都提交，要么都回滚 |
| 消费-处理-生产一致性 | 从一个 Topic 消费消息后，处理结果写入另一个 Topic    |
| 避免部分写入         | 中途失败时回滚本次事务内已发送的 Kafka 消息          |
| 配合幂等 Producer    | 降低重试导致的重复写入问题                           |

Kafka 事务涉及以下核心概念：

| 概念                | 说明                                 |
| ------------------- | ------------------------------------ |
| `transactional.id`  | Producer 事务 ID，用于标识事务生产者 |
| 事务 Producer       | 开启事务能力的 Producer              |
| `beginTransaction`  | 开启事务                             |
| `commitTransaction` | 提交事务                             |
| `abortTransaction`  | 回滚事务                             |
| `isolation.level`   | Consumer 读取事务消息的隔离级别      |

Consumer 读取事务消息时，建议配置：

```yaml
spring:
  kafka:
    consumer:
      properties:
        # 只读取已提交事务的消息
        isolation.level: read_committed
```

`read_committed` 可以避免 Consumer 读取到已回滚事务中的消息。普通非事务消息也可以被正常读取。

### Producer 事务配置

Spring Kafka 中可以通过配置 `transaction-id-prefix` 开启事务 Producer。配置后，`KafkaTemplate` 可以在事务中发送消息。

推荐配置如下：

```yaml
spring:
  kafka:
    producer:
      # 开启事务 Producer 的事务 ID 前缀
      transaction-id-prefix: ateng-kafka-tx-

      # 事务消息推荐等待所有同步副本确认
      acks: all

      # 事务 Producer 需要配合幂等能力
      properties:
        enable.idempotence: true
        max.in.flight.requests.per.connection: 5

    consumer:
      properties:
        # 消费端仅读取已提交事务消息
        isolation.level: read_committed
```

事务配置要求如下：

1. `transaction-id-prefix` 必须稳定，不能每次启动随机变化。
2. 同一个应用多个实例不能使用完全相同的 `transactional.id`。
3. Spring Kafka 会基于 `transaction-id-prefix` 创建具体事务 ID。
4. Producer 事务会增加额外协调成本，不适合所有普通消息都开启。
5. 消费端如果需要避免读取回滚消息，应配置 `isolation.level=read_committed`。
6. Kafka Broker 需要支持事务相关内部 Topic，例如 `__transaction_state`。

如果项目只是普通异步消息发送，不涉及多条 Kafka 消息原子提交或 Kafka 消费后再生产，通常不需要开启 Kafka 事务。

### Transactional ID 设计

`transactional.id` 是 Kafka 用于识别事务 Producer 的关键标识。它必须在 Producer 重启后保持稳定，以便 Kafka 识别同一个事务生产者并完成事务恢复或隔离旧 Producer。

推荐格式如下：

```text
{系统标识}-{服务名}-{业务场景}-{实例标识}
```

示例：

```text
ateng-order-service-order-created-0
ateng-order-service-order-created-1
ateng-payment-service-payment-success-0
```

在 Spring Kafka 中通常配置事务 ID 前缀：

```yaml
spring:
  kafka:
    producer:
      transaction-id-prefix: ateng-order-service-tx-
```

设计要求如下：

1. 同一个应用实例的事务 ID 必须唯一。
2. 同一个事务 Producer 重启后应尽量复用稳定事务 ID。
3. 不要所有实例使用完全相同的事务 ID。
4. 不要使用每次启动随机变化的事务 ID 前缀。
5. 事务 ID 应能从监控或日志中识别来源服务。
6. Kubernetes 环境中可以结合 Pod 名称、实例序号或 StatefulSet 序号设计。

事务 ID 冲突可能导致旧 Producer 被 fencing，即旧 Producer 被 Kafka 认为失效，后续发送会失败。因此，多实例部署时需要特别注意事务 ID 的唯一性。

### KafkaTemplate 事务发送

`KafkaTemplate` 支持通过 `executeInTransaction` 执行事务内发送。事务中可以发送多条消息到同一个或不同 Topic，只要事务提交成功，这些消息才会对 `read_committed` 消费者可见。

文件位置：`src/main/java/io/github/atengk/kafka/producer/KafkaTransactionProducer.java`

该组件用于演示通过 `KafkaTemplate` 在一个 Kafka 事务中发送多条消息。

```java
package io.github.atengk.kafka.producer;

import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.model.OrderCreatedMessage;
import io.github.atengk.kafka.model.PaymentSuccessMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka 事务消息生产者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaTransactionProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 在同一个 Kafka 事务中发送订单和支付消息
     *
     * @param orderMessage   订单创建消息
     * @param paymentMessage 支付成功消息
     */
    public void sendInTransaction(OrderCreatedMessage orderMessage, PaymentSuccessMessage paymentMessage) {
        kafkaTemplate.executeInTransaction(operations -> {
            String orderKey = "ORDER:" + orderMessage.getOrderId();
            String paymentKey = "PAYMENT:" + paymentMessage.getPaymentId();

            operations.send(KafkaTopicConstant.ORDER_CREATED_TOPIC, orderKey, orderMessage);
            operations.send(KafkaTopicConstant.PAYMENT_SUCCESS_TOPIC, paymentKey, paymentMessage);

            log.info("Kafka事务消息发送完成，orderId={}，paymentId={}",
                    orderMessage.getOrderId(), paymentMessage.getPaymentId());

            return true;
        });
    }

}
```

事务发送适用场景：

| 场景                                            | 是否适合            |
| ----------------------------------------------- | ------------------- |
| 一次发送多条 Kafka 消息，要求全部成功或全部失败 | 适合                |
| 消费 Topic A 后发送 Topic B                     | 适合                |
| 写数据库后发送 Kafka 消息                       | 不能只靠 Kafka 事务 |
| 调用外部接口后发送 Kafka 消息                   | 不能只靠 Kafka 事务 |
| 普通单条异步通知                                | 通常不需要事务      |

### 本地事务与消息事务

本地事务通常指数据库事务，Kafka 事务指 Kafka 内部消息发送事务。两者不是天然同一个事务。也就是说，数据库提交成功但 Kafka 发送失败，或者 Kafka 发送成功但数据库回滚，都会导致数据不一致。

典型问题如下：

```text
开启数据库事务
  -> 写订单表成功
  -> 发送 Kafka 消息成功
  -> 数据库提交失败
  -> Kafka 中已经有消息，但数据库没有对应订单
```

另一个问题：

```text
开启数据库事务
  -> 写订单表成功
  -> 发送 Kafka 消息失败
  -> 数据库提交成功
  -> 订单已创建，但下游收不到消息
```

推荐解决方案是本地消息表或 Outbox Pattern：

```text
开启数据库事务
  -> 写业务表
  -> 写本地消息表
  -> 提交数据库事务
  -> 后台任务扫描本地消息表
  -> 发送 Kafka
  -> 发送成功后更新消息表状态
```

本地消息表建议：

```sql
CREATE TABLE kafka_outbox_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息唯一标识',
    topic VARCHAR(128) NOT NULL COMMENT 'Topic名称',
    message_key VARCHAR(255) DEFAULT NULL COMMENT '消息Key',
    message_body TEXT NOT NULL COMMENT '消息内容',
    message_status VARCHAR(32) NOT NULL COMMENT '消息状态：PENDING、SENT、FAILED',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    next_retry_time DATETIME DEFAULT NULL COMMENT '下次重试时间',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '异常信息',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_status_retry_time (message_status, next_retry_time)
) COMMENT='Kafka本地消息表';
```

本地事务与 Kafka 消息一致性建议：

1. 写业务数据和写本地消息表必须在同一个数据库事务中完成。
2. Kafka 发送由后台任务或事务提交后的事件异步完成。
3. 发送成功后更新本地消息表状态为 `SENT`。
4. 发送失败后记录异常，并根据重试策略继续发送。
5. 消费端仍必须做幂等，因为本地消息表重试可能导致重复发送。
6. 不建议在数据库事务中长时间同步等待 Kafka 发送结果。

### 事务回滚处理

Kafka 事务回滚用于撤销当前事务中已经发送但尚未提交的 Kafka 消息。使用 `KafkaTemplate.executeInTransaction` 时，如果事务回调中抛出异常，Spring Kafka 会回滚 Kafka 事务。

事务回滚示例：

```java
package io.github.atengk.kafka.producer;

import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.model.OrderCreatedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka 事务回滚示例
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaTransactionRollbackProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 发送事务消息并模拟异常回滚
     *
     * @param message 订单创建消息
     */
    public void sendAndRollback(OrderCreatedMessage message) {
        try {
            kafkaTemplate.executeInTransaction(operations -> {
                String key = "ORDER:" + message.getOrderId();

                operations.send(KafkaTopicConstant.ORDER_CREATED_TOPIC, key, message);
                log.info("Kafka事务内消息已发送，orderId={}", message.getOrderId());

                // 模拟业务异常，事务会回滚
                throw new IllegalStateException("模拟Kafka事务回滚");
            });
        } catch (Exception e) {
            log.error("Kafka事务消息发送失败，事务已回滚，orderId={}", message.getOrderId(), e);
        }
    }

}
```

事务回滚注意事项如下：

1. Kafka 事务只能回滚当前 Kafka 事务内发送的消息。
2. Kafka 事务不能回滚已经提交的数据库事务。
3. Kafka 事务不能回滚已经调用成功的外部 HTTP 接口。
4. 事务内不要执行耗时过长的逻辑，避免事务超时。
5. 回滚后应记录日志，必要时写入失败记录表。
6. 对消费者不可见的前提是 Consumer 使用 `read_committed`。

### Exactly Once 语义

Exactly Once 语义通常容易被误解。Kafka 的 Exactly Once 主要适用于 Kafka 内部的“读 Kafka、处理、写 Kafka”场景，即消费输入 Topic，处理后写入输出 Topic，并通过事务提交消费 Offset 和输出消息。

它不等于以下含义：

| 误解                                  | 正确认识                         |
| ------------------------------------- | -------------------------------- |
| Kafka 可以保证业务代码只执行一次      | 不能，业务代码仍可能重复执行     |
| Kafka 可以保证数据库只写一次          | 不能，数据库需要自己的幂等和事务 |
| Kafka 可以保证外部接口只调用一次      | 不能，下游接口需要幂等           |
| 开启事务后 Consumer 不需要幂等        | 错，Consumer 仍需幂等            |
| Exactly Once 适用于所有端到端业务链路 | 不适用外部系统的一致性边界       |

Kafka Exactly Once 更准确的使用边界如下：

| 场景                                   | 说明                    |
| -------------------------------------- | ----------------------- |
| Kafka Topic A -> 处理 -> Kafka Topic B | 适合使用 Kafka 事务     |
| Kafka Streams 处理链路                 | 适合                    |
| 消费 Kafka 后写数据库                  | 仍需数据库幂等          |
| 写数据库后发 Kafka                     | 推荐本地消息表或 Outbox |
| 消费 Kafka 后调用第三方接口            | 仍需接口幂等和补偿      |

因此，业务项目中应将 Exactly Once 理解为 Kafka 内部链路的事务语义，而不是整个业务系统的绝对一次执行语义。对于 Java 后端业务系统，最稳妥的实践仍然是：Producer 可靠发送、Consumer 手动提交 Offset、业务幂等、本地消息表、失败重试和死信补偿。

### 事务使用限制

Kafka 事务虽然可以提升 Kafka 内部消息写入的一致性，但也会增加复杂度和性能成本。不是所有消息都应该启用事务。

事务使用限制如下：

| 限制             | 说明                                                 |
| ---------------- | ---------------------------------------------------- |
| 性能成本更高     | 事务需要协调器参与，吞吐低于普通发送                 |
| 配置更复杂       | 需要正确配置 `transactional.id`、幂等和隔离级别      |
| 不覆盖外部系统   | 不能保证数据库、Redis、HTTP 接口和 Kafka 原子一致    |
| 事务 ID 管理复杂 | 多实例部署必须避免事务 ID 冲突                       |
| 事务超时风险     | 事务内逻辑过慢可能导致事务失败                       |
| 运维排查复杂     | 事务状态、Producer fencing、未完成事务都需要排查能力 |

不建议使用 Kafka 事务的场景：

| 场景                     | 原因                         |
| ------------------------ | ---------------------------- |
| 普通单条异步通知         | 事务收益不明显               |
| 简单日志采集             | 追求吞吐，事务成本过高       |
| 写数据库后发 Kafka       | Kafka 事务不能覆盖数据库事务 |
| 调用第三方接口后发 Kafka | Kafka 事务不能回滚外部接口   |
| 团队缺少事务运维经验     | 可能增加故障排查难度         |

推荐使用 Kafka 事务的场景：

| 场景                                | 原因                         |
| ----------------------------------- | ---------------------------- |
| 消费 Kafka 后再发送 Kafka           | 可保证 Offset 和输出消息一致 |
| 多个 Kafka Topic 原子写入           | 要么都提交，要么都回滚       |
| Kafka Streams 状态处理              | 与 Kafka 生态事务能力匹配    |
| 对重复输出极其敏感的 Kafka 内部链路 | 可以减少重复输出             |

最终建议是：普通业务消息优先采用“可靠 Producer + 手动 Offset + 幂等 Consumer + 本地消息表 + 死信补偿”方案；只有在明确需要 Kafka 内部事务边界时，再启用 Kafka 事务。


## 顺序消息

本章节用于说明 Kafka 顺序消息的设计方式，包括顺序消息场景、Key 与 Partition 的关系、单分区顺序消费、局部顺序设计、并发消费与顺序冲突、顺序消息失败处理和顺序消息对性能的影响。

Kafka 的顺序性是 Partition 级别的。也就是说，Kafka 可以保证同一个 Partition 内的消息按照写入顺序被消费，但不能保证一个 Topic 下多个 Partition 之间的全局顺序。因此，顺序消息设计的核心不是让整个 Topic 全局有序，而是根据业务维度设计局部顺序，例如同一个订单、同一个用户、同一个账户或同一个设备的消息有序。

### 顺序消息场景

顺序消息适用于业务状态存在前后依赖的场景。如果后面的消息先于前面的消息被处理，就可能导致状态错乱、数据覆盖或业务异常。

常见顺序消息场景如下：

| 场景         | 顺序维度    | 示例                                     |
| ------------ | ----------- | ---------------------------------------- |
| 订单状态流转 | `orderId`   | 创建订单 -> 支付成功 -> 发货 -> 确认收货 |
| 支付状态流转 | `paymentId` | 支付创建 -> 支付处理中 -> 支付成功       |
| 退款状态流转 | `refundId`  | 退款申请 -> 退款处理中 -> 退款成功       |
| 用户状态变更 | `userId`    | 注册 -> 实名认证 -> 冻结 -> 解冻         |
| 库存变更     | `skuId`     | 入库 -> 锁定库存 -> 扣减库存 -> 释放库存 |
| 设备数据上报 | `deviceId`  | 设备状态连续上报                         |
| 账户流水     | `accountId` | 入账 -> 出账 -> 冻结 -> 解冻             |

不需要顺序消息的场景如下：

| 场景         | 原因                                       |
| ------------ | ------------------------------------------ |
| 日志采集     | 通常只关注最终写入和分析，不强依赖严格顺序 |
| 用户行为埋点 | 大多数统计场景允许乱序或后续排序           |
| 异步通知     | 多数通知只关注是否送达                     |
| 独立任务处理 | 各任务之间没有状态依赖                     |
| 批量数据同步 | 可以通过更新时间或版本号处理乱序           |

设计顺序消息前，需要先明确顺序范围。很多业务并不需要全局顺序，只需要同一个业务主体内有序。例如订单系统通常只需要同一个订单的状态消息有序，不需要所有订单之间有序。

### Key 与 Partition 关系

Kafka Producer 发送消息时，如果指定了 Key，默认分区策略会根据 Key 计算目标 Partition。相同 Key 的消息通常会被发送到同一个 Partition，因此可以保证同一 Key 在同一 Partition 内的写入和消费顺序。

Key 与 Partition 的关系如下：

```text
message key -> partitioner -> target partition -> append log -> consumer poll
```

示例：

| 消息            | Key           | Partition |
| --------------- | ------------- | --------- |
| 订单 10001 创建 | `ORDER:10001` | `0`       |
| 订单 10001 支付 | `ORDER:10001` | `0`       |
| 订单 10001 发货 | `ORDER:10001` | `0`       |
| 订单 10002 创建 | `ORDER:10002` | `2`       |
| 订单 10003 创建 | `ORDER:10003` | `1`       |

推荐 Key 设计如下：

| 业务场景     | 推荐 Key              |
| ------------ | --------------------- |
| 订单状态消息 | `ORDER:{orderId}`     |
| 支付状态消息 | `PAYMENT:{paymentId}` |
| 退款状态消息 | `REFUND:{refundId}`   |
| 用户状态消息 | `USER:{userId}`       |
| 库存变更消息 | `SKU:{skuId}`         |
| 账户流水消息 | `ACCOUNT:{accountId}` |

发送顺序消息时，应保证同一业务主体使用相同 Key。

```java
String key = "ORDER:" + orderMessage.getOrderId();

kafkaTemplate.send(
        KafkaTopicConstant.ORDER_STATUS_TOPIC,
        key,
        orderMessage
);
```

Key 设计要求如下：

1. 同一业务主体必须使用稳定一致的 Key。
2. 不要使用随机 UUID 作为顺序消息 Key。
3. 不要使用状态值、类型值等低基数字段作为 Key。
4. 不要随意手动指定 Partition，避免破坏 Key 路由策略。
5. Topic 增加 Partition 前，必须评估 Key 到 Partition 映射变化带来的顺序影响。
6. 如果消息必须跨事件类型有序，不同事件类型也要使用相同业务 Key。

### 单分区顺序消费

单分区顺序消费是最简单的全局顺序方案。将 Topic 分区数设置为 `1`，所有消息都写入同一个 Partition，Consumer 按照 Partition 内顺序逐条消费。

创建单分区 Topic 示例：

```bash
docker exec -it ateng-kafka /opt/kafka/bin/kafka-topics.sh \
  --create \
  --topic ateng.order.status.topic \
  --bootstrap-server localhost:9092 \
  --partitions 1 \
  --replication-factor 1
```

单分区顺序消费示例：

```java
package io.github.atengk.kafka.consumer;

import io.github.atengk.kafka.constant.KafkaGroupConstant;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.model.OrderStatusMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 单分区订单状态消息消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class SinglePartitionOrderStatusConsumer {

    /**
     * 消费订单状态消息
     *
     * @param record         Kafka 消息记录
     * @param acknowledgment Offset 确认对象
     */
    @KafkaListener(
            topics = KafkaTopicConstant.ORDER_STATUS_TOPIC,
            groupId = KafkaGroupConstant.ORDER_STATUS_CONSUMER_GROUP,
            concurrency = "1",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, OrderStatusMessage> record,
                        Acknowledgment acknowledgment) {
        try {
            OrderStatusMessage message = record.value();

            log.info("顺序消费订单状态消息，topic={}，partition={}，offset={}，key={}，orderId={}，status={}",
                    record.topic(), record.partition(), record.offset(), record.key(),
                    message.getOrderId(), message.getOrderStatus());

            // 按顺序处理订单状态变更

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("订单状态消息顺序消费失败，topic={}，partition={}，offset={}，key={}",
                    record.topic(), record.partition(), record.offset(), record.key(), e);
            throw e;
        }
    }

}
```

单分区顺序消费适用场景：

| 场景           | 说明                         |
| -------------- | ---------------------------- |
| 消息量较小     | 单分区吞吐可以满足业务需求   |
| 全局顺序要求强 | 所有消息必须按写入顺序处理   |
| 业务处理简单   | 单线程处理不会造成严重堆积   |
| 管理类事件     | 配置变更、状态控制等低频事件 |

单分区顺序消费的限制如下：

1. 只有一个 Partition，消费并发能力受限。
2. 一个消息处理慢，会阻塞后续所有消息。
3. Topic 后续扩容 Partition 会影响全局顺序。
4. 不适合高吞吐业务事件。
5. 生产环境使用前必须压测确认吞吐和延迟。

### 局部顺序设计

局部顺序是 Kafka 顺序消息的推荐设计方式。它不是保证整个 Topic 全局有序，而是保证同一个业务主体内有序。例如同一个订单的消息有序，不同订单之间可以并行消费。

局部顺序设计示例：

```text
订单 10001 的消息 -> key=ORDER:10001 -> partition 0 -> 顺序消费
订单 10002 的消息 -> key=ORDER:10002 -> partition 1 -> 顺序消费
订单 10003 的消息 -> key=ORDER:10003 -> partition 2 -> 顺序消费
```

订单状态消息模型示例：

```java
package io.github.atengk.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 订单状态消息
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusMessage {

    /**
     * 订单 ID
     */
    private String orderId;

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 订单状态
     */
    private String orderStatus;

    /**
     * 状态版本号
     */
    private Long statusVersion;

    /**
     * 事件发生时间
     */
    private LocalDateTime eventTime;

}
```

发送局部顺序消息示例：

```java
package io.github.atengk.kafka.producer;

import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.model.OrderStatusMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 订单状态顺序消息生产者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 发送订单状态消息
     *
     * @param message 订单状态消息
     */
    public void sendOrderStatusMessage(OrderStatusMessage message) {
        String key = "ORDER:" + message.getOrderId();

        kafkaTemplate.send(KafkaTopicConstant.ORDER_STATUS_TOPIC, key, message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("订单状态顺序消息发送失败，orderId={}，key={}",
                                message.getOrderId(), key, ex);
                        return;
                    }

                    log.info("订单状态顺序消息发送成功，topic={}，partition={}，offset={}，orderId={}，key={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            message.getOrderId(),
                            key);
                });
    }

}
```

局部顺序设计要求如下：

1. 同一业务主体必须使用同一个 Key。
2. Consumer 并发数可以大于 1，但同一个 Partition 同一时刻只会被一个消费者线程处理。
3. 业务处理逻辑中仍要校验状态版本，防止异常重放或历史消息覆盖新状态。
4. 如果业务状态有版本号，建议按 `statusVersion` 做状态递增校验。
5. 如果同一订单跨多个 Topic 流转，单 Topic 内有序不能保证跨 Topic 有序，应重新设计消息流。

订单状态更新建议使用条件更新，避免旧消息覆盖新状态：

```sql
UPDATE order_info
SET order_status = 'PAID',
    status_version = 2,
    update_time = NOW()
WHERE order_id = '10001'
  AND status_version < 2;
```

这种方式可以在数据库层面防止历史状态消息覆盖较新的订单状态。

### 并发消费与顺序冲突

并发消费可以提升吞吐，但也可能引入顺序冲突。Kafka 能保证同一个 Partition 内的消息按顺序拉取，但如果业务代码内部又将消息提交到线程池异步处理，就可能破坏顺序。

常见顺序冲突如下：

| 冲突场景                       | 说明                                |
| ------------------------------ | ----------------------------------- |
| Consumer 内部异步线程池处理    | 后提交的任务可能先执行完成          |
| 批量消费中并行处理同一 Key     | 同一业务主体消息可能乱序            |
| 多 Topic 消费同一业务主体      | 不同 Topic 之间无法保证顺序         |
| Retry Topic 非阻塞重试         | 后续消息可能先于失败消息处理        |
| 手动指定 Partition 不一致      | 同一业务 Key 被写入不同 Partition   |
| 增加 Partition 后 Key 映射变化 | 同一 Key 后续可能路由到新 Partition |

不推荐写法：

```java
@KafkaListener(topics = KafkaTopicConstant.ORDER_STATUS_TOPIC)
public void consume(ConsumerRecord<String, OrderStatusMessage> record,
                    Acknowledgment acknowledgment) {
    executorService.submit(() -> {
        // 异步线程池处理可能导致同一Partition内消息完成顺序不一致
        process(record.value());
    });

    // 错误：异步任务还没完成就提交 Offset
    acknowledgment.acknowledge();
}
```

推荐写法：

```java
@KafkaListener(
        topics = KafkaTopicConstant.ORDER_STATUS_TOPIC,
        groupId = KafkaGroupConstant.ORDER_STATUS_CONSUMER_GROUP,
        containerFactory = "kafkaListenerContainerFactory"
)
public void consume(ConsumerRecord<String, OrderStatusMessage> record,
                    Acknowledgment acknowledgment) {
    try {
        // 当前消费线程内同步完成业务处理，保证同一Partition处理顺序
        process(record.value());

        acknowledgment.acknowledge();
    } catch (Exception e) {
        throw e;
    }
}
```

如果必须使用线程池提升性能，需要按业务 Key 做串行化处理，例如同一个 Key 的消息进入同一个单线程队列，不同 Key 可以并行处理。但这种方案实现复杂，通常只有在吞吐和顺序都要求较高时才考虑。

并发消费建议如下：

| 场景         | 建议                                             |
| ------------ | ------------------------------------------------ |
| 严格顺序     | 不在 Consumer 内部使用异步线程池                 |
| 局部顺序     | 按业务 Key 分区，并在单 Partition 内同步处理     |
| 高吞吐非顺序 | 可以使用批量消费或线程池                         |
| 失败重试     | 顺序敏感场景优先阻塞重试，不随意进入 Retry Topic |
| 状态更新     | 增加版本号或状态机条件判断                       |

### 顺序消息失败处理

顺序消息失败处理比普通消息更复杂。普通消息失败后可以跳过、重试或进入死信；顺序消息如果跳过失败消息，后续消息可能基于错误状态继续执行，导致业务状态不一致。

顺序消息失败处理策略如下：

| 策略               | 说明                                  | 适用场景             |
| ------------------ | ------------------------------------- | -------------------- |
| 阻塞重试           | 当前消息失败后阻塞当前 Partition 重试 | 强顺序场景           |
| 进入死信并暂停分区 | 失败消息进入死信后暂停对应 Partition  | 需要人工介入         |
| 状态机兜底         | 后续消息按业务状态判断是否允许执行    | 状态更新类业务       |
| 补偿任务修复       | 失败后通过补偿任务修正状态            | 可最终一致场景       |
| 跳过失败消息       | 记录失败并继续消费                    | 只适合非核心顺序场景 |

顺序消息不建议默认使用非阻塞 Retry Topic。因为失败消息进入 Retry Topic 后，主 Topic 后续消息可能继续被消费，从而破坏业务顺序。

顺序消息失败流程建议：

```text
消费顺序消息
  -> 业务处理失败
  -> 判断是否可重试
  -> 短间隔阻塞重试
  -> 仍失败
  -> 记录失败状态
  -> 投递死信 Topic
  -> 触发告警
  -> 暂停或人工处理对应业务 Key / Partition
```

失败处理代码示例：

```java
package io.github.atengk.kafka.consumer;

import io.github.atengk.kafka.constant.KafkaGroupConstant;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import io.github.atengk.kafka.model.OrderStatusMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 顺序消息失败处理消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class OrderedFailureConsumer {

    /**
     * 消费顺序订单状态消息
     *
     * @param record         Kafka 消息记录
     * @param acknowledgment Offset 确认对象
     */
    @KafkaListener(
            topics = KafkaTopicConstant.ORDER_STATUS_TOPIC,
            groupId = KafkaGroupConstant.ORDER_STATUS_CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, OrderStatusMessage> record,
                        Acknowledgment acknowledgment) {
        try {
            OrderStatusMessage message = record.value();

            log.info("开始处理顺序消息，topic={}，partition={}，offset={}，key={}，orderId={}，status={}",
                    record.topic(), record.partition(), record.offset(), record.key(),
                    message.getOrderId(), message.getOrderStatus());

            // 先做状态机校验，再执行业务更新
            processOrderStatus(message);

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("顺序消息处理失败，不提交Offset，topic={}，partition={}，offset={}，key={}",
                    record.topic(), record.partition(), record.offset(), record.key(), e);
            throw e;
        }
    }

    /**
     * 处理订单状态
     *
     * @param message 订单状态消息
     */
    private void processOrderStatus(OrderStatusMessage message) {
        // 实际项目中应结合订单状态机和版本号做条件更新
        log.info("处理订单状态变更，orderId={}，status={}，version={}",
                message.getOrderId(), message.getOrderStatus(), message.getStatusVersion());
    }

}
```

顺序消息失败处理要求如下：

1. 失败时不要提前提交 Offset。
2. 强顺序场景优先使用阻塞重试。
3. 进入死信后必须触发告警。
4. 死信重放前必须确认前置状态已修复。
5. 业务表应使用状态机或版本号防止乱序覆盖。
6. 不要在顺序敏感链路中随意使用异步线程池和非阻塞重试。

### 顺序消息性能影响

顺序消息通常会降低系统吞吐能力。顺序范围越大，并发能力越弱；顺序要求越严格，失败消息对后续消息的阻塞越明显。

性能影响如下：

| 设计方式              | 顺序能力   | 性能影响                 |
| --------------------- | ---------- | ------------------------ |
| 单 Partition 全局顺序 | 最强       | 吞吐最低                 |
| 按 Key 局部顺序       | 较强       | 吞吐较好                 |
| 多 Partition 无序     | 无全局顺序 | 吞吐最高                 |
| 阻塞重试              | 保持顺序   | 失败时阻塞当前 Partition |
| 非阻塞重试            | 顺序较弱   | 主链路吞吐较好           |

优化建议如下：

1. 优先选择局部顺序，不要轻易要求全局顺序。
2. 合理设计 Key，避免大量消息集中到少数 Key。
3. 对热点 Key 做业务拆分或降级，例如按订单行、仓库、租户进一步拆分。
4. 对状态类业务增加版本号和状态机校验，降低对 Kafka 严格顺序的依赖。
5. 对高吞吐日志、埋点、统计类消息不要强行做顺序消费。
6. 对强顺序 Topic 做单独监控，重点关注 Consumer Lag 和失败重试次数。

顺序消息设计的基本结论是：能局部顺序就不要全局顺序，能用状态机兜底就不要完全依赖 Kafka 顺序，能异步最终一致就不要把所有消息串行化。

## 延迟消息

本章节用于说明 Kafka 延迟消息的实现方式，包括 Kafka 延迟消息限制、基于 Retry Topic 的延迟、基于时间轮的延迟、基于数据库任务表的延迟、基于 Redis ZSet 的延迟和延迟消息补偿机制。

Kafka 原生并不是标准延迟队列。Kafka 更适合高吞吐事件流、顺序日志和消息持久化。延迟消息通常需要通过业务层设计实现，例如 Retry Topic、数据库任务表、Redis ZSet、时间轮、调度任务或专门的延迟队列中间件。

### Kafka 延迟消息限制

Kafka 消息写入 Topic 后，Consumer 正常情况下可以立即拉取。Kafka 不像某些消息队列一样直接提供“消息在指定时间后可消费”的标准延迟消息能力。

Kafka 实现延迟消息面临以下限制：

| 限制                             | 说明                                                  |
| -------------------------------- | ----------------------------------------------------- |
| 原生不支持精确投递时间           | Kafka 不负责按业务时间触发消息                        |
| Consumer 拉取后不能长时间阻塞    | 长时间阻塞可能触发 `max.poll.interval.ms` 超时        |
| 单条延迟会影响分区后续消息       | 如果在 Consumer 端等待，可能阻塞同 Partition 后续消息 |
| 延迟精度依赖外部调度             | 数据库、Redis、定时任务或时间轮决定触发精度           |
| 大量延迟消息会增加存储和扫描成本 | 需要合理设计索引、分桶和清理策略                      |
| 顺序和延迟可能冲突               | 延迟重试可能导致后续消息先被处理                      |

因此，不建议在 Kafka Consumer 中直接使用 `Thread.sleep()` 等方式实现延迟消费。

不推荐写法：

```java
@KafkaListener(topics = KafkaTopicConstant.ORDER_DELAY_TOPIC)
public void consume(ConsumerRecord<String, Object> record,
                    Acknowledgment acknowledgment) throws InterruptedException {
    // 错误：长时间sleep会阻塞消费线程，可能触发max.poll.interval.ms超时
    Thread.sleep(60000);

    process(record.value());
    acknowledgment.acknowledge();
}
```

推荐做法是将“等待”交给外部调度机制，到期后再发送到目标 Topic 或执行业务处理。

### 基于 Retry Topic 的延迟

基于 Retry Topic 的延迟适合消费失败后的分级延迟重试。它通过不同级别的 Retry Topic 表示不同延迟时间，例如 1 分钟、5 分钟、30 分钟。

Topic 设计示例：

| 重试级别    | Topic                             | 延迟时间     |
| ----------- | --------------------------------- | ------------ |
| 第 1 次重试 | `ateng.order.created.retry.1m`    | 1 分钟       |
| 第 2 次重试 | `ateng.order.created.retry.5m`    | 5 分钟       |
| 第 3 次重试 | `ateng.order.created.retry.30m`   | 30 分钟      |
| 最终失败    | `ateng.order.created.dead-letter` | 不再自动重试 |

处理流程如下：

```text
主 Topic 消费失败
  -> 写入 retry.1m
  -> 到期后重新发送主 Topic 或直接消费 retry.1m
  -> 再失败写入 retry.5m
  -> 再失败写入 retry.30m
  -> 仍失败写入 dead-letter
```

Retry Topic 配置示例：

```yaml
app:
  kafka:
    topic:
      auto-create: true
      topics:
        - name: ateng.order.created.retry.1m
          partitions: 3
          replicas: 1
          configs:
            retention.ms: "86400000"
            cleanup.policy: delete

        - name: ateng.order.created.retry.5m
          partitions: 3
          replicas: 1
          configs:
            retention.ms: "86400000"
            cleanup.policy: delete

        - name: ateng.order.created.retry.30m
          partitions: 3
          replicas: 1
          configs:
            retention.ms: "172800000"
            cleanup.policy: delete
```

Retry Topic 延迟需要一个调度组件判断消息是否到期。简单方式是在消息 Header 中写入 `x-next-retry-time`，Retry Consumer 读取后判断是否到期。未到期消息不建议长时间阻塞当前线程，实际项目中更推荐写入数据库任务表或 Redis ZSet 由调度任务触发。

Retry Topic 方式适用场景：

| 场景           | 说明                     |
| -------------- | ------------------------ |
| 消费失败重试   | 最常见用途               |
| 固定阶梯延迟   | 1m、5m、30m 等固定级别   |
| 非核心业务补偿 | 通知、积分、统计、同步等 |
| 允许顺序弱化   | 后续消息可以先处理       |

Retry Topic 方式不适合精确到秒级的大量定时任务，也不适合强顺序业务链路。

### 基于时间轮的延迟

时间轮是一种高效管理大量定时任务的数据结构。它将未来时间划分成多个槽位，到达某个时间槽时批量触发对应任务。时间轮适合高并发、短延迟、大量定时任务的场景。

时间轮延迟处理流程如下：

```text
收到延迟消息
  -> 计算触发时间
  -> 放入时间轮槽位
  -> 时间轮指针转动
  -> 到期取出任务
  -> 发送到 Kafka 目标 Topic 或执行业务
```

时间轮适用场景：

| 场景             | 说明                     |
| ---------------- | ------------------------ |
| 大量短延迟任务   | 例如数秒到数分钟         |
| 延迟精度要求中等 | 允许一定时间误差         |
| 内存调度         | 任务量可控且有持久化兜底 |
| 高性能调度系统   | 比频繁扫描数据库更高效   |

时间轮限制如下：

1. 纯内存时间轮在应用重启后会丢失任务。
2. 需要持久化任务数据作为恢复依据。
3. 多实例部署需要任务分片或分布式协调。
4. 延迟时间跨度过大时，时间轮层级设计会变复杂。
5. 不建议普通业务系统自行实现复杂时间轮，除非有明确性能需求。

时间轮方案建议与数据库任务表结合使用：数据库负责持久化，时间轮负责近期任务调度。应用启动后，从数据库加载一段时间窗口内的待触发任务进入时间轮。

### 基于数据库任务表的延迟

基于数据库任务表的延迟是企业业务系统中最稳妥的方案之一。它通过数据库保存延迟任务，到期后由定时任务扫描并发送 Kafka 消息或执行业务处理。优点是可靠、可查询、可补偿、易审计；缺点是延迟精度和吞吐受数据库扫描能力影响。

延迟任务表建议：

```sql
CREATE TABLE kafka_delay_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id VARCHAR(64) NOT NULL COMMENT '任务唯一标识',
    topic VARCHAR(128) NOT NULL COMMENT '目标Topic',
    message_key VARCHAR(255) DEFAULT NULL COMMENT '消息Key',
    message_body TEXT NOT NULL COMMENT '消息内容',
    execute_time DATETIME NOT NULL COMMENT '计划执行时间',
    task_status VARCHAR(32) NOT NULL COMMENT '任务状态：PENDING、PROCESSING、SUCCESS、FAILED、CANCELLED',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    max_retry_count INT NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '异常信息',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_task_id (task_id),
    KEY idx_status_execute_time (task_status, execute_time)
) COMMENT='Kafka延迟任务表';
```

任务状态建议：

| 状态         | 说明     |
| ------------ | -------- |
| `PENDING`    | 待执行   |
| `PROCESSING` | 执行中   |
| `SUCCESS`    | 执行成功 |
| `FAILED`     | 执行失败 |
| `CANCELLED`  | 已取消   |

延迟任务扫描流程如下：

```text
定时任务启动
  -> 查询 execute_time <= now 且 status=PENDING 的任务
  -> 将任务更新为 PROCESSING
  -> 发送 Kafka 消息
  -> 发送成功，更新为 SUCCESS
  -> 发送失败，增加 retry_count 并更新 next execute_time
  -> 超过最大重试次数，更新为 FAILED 并告警
```

文件位置：`src/main/java/io/github/atengk/kafka/delay/DelayTaskStatusEnum.java`

该枚举用于定义延迟任务状态。

```java
package io.github.atengk.kafka.delay;

import lombok.Getter;

/**
 * Kafka 延迟任务状态枚举
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Getter
public enum DelayTaskStatusEnum {

    /**
     * 待执行
     */
    PENDING("待执行"),

    /**
     * 执行中
     */
    PROCESSING("执行中"),

    /**
     * 执行成功
     */
    SUCCESS("执行成功"),

    /**
     * 执行失败
     */
    FAILED("执行失败"),

    /**
     * 已取消
     */
    CANCELLED("已取消");

    private final String description;

    DelayTaskStatusEnum(String description) {
        this.description = description;
    }

}
```

数据库任务表方案设计要求如下：

1. 必须有任务唯一 ID。
2. 必须有执行时间索引。
3. 必须有状态字段，防止重复执行。
4. 多实例扫描时需要通过状态更新或分布式锁防止并发抢占。
5. 任务执行成功后不建议立即物理删除，应保留一段时间用于审计。
6. 延迟精度通常由调度频率决定，例如每 5 秒或每 10 秒扫描一次。

### 基于 Redis ZSet 的延迟

Redis ZSet 可以使用 score 存储消息执行时间戳，通过定时任务扫描 score 小于当前时间的元素实现延迟触发。它性能较高，适合中短期延迟任务、通知类任务、非核心补偿任务。

Redis ZSet 设计如下：

```text
key = kafka:delay:task
member = taskId
score = executeTimestampMillis
```

任务内容可以存储在 Redis String、Hash 或数据库中。更推荐 Redis ZSet 只存任务 ID，任务详情存数据库，这样可靠性和可查询性更好。

文件位置：`src/main/java/io/github/atengk/kafka/delay/RedisDelayQueueService.java`

该服务用于基于 Redis ZSet 管理延迟任务 ID。

```java
package io.github.atengk.kafka.delay;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

/**
 * Redis ZSet 延迟队列服务
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisDelayQueueService {

    private static final String DELAY_QUEUE_KEY = "kafka:delay:task";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 添加延迟任务
     *
     * @param taskId      任务 ID
     * @param executeTime 执行时间
     */
    public void addDelayTask(String taskId, Instant executeTime) {
        if (StrUtil.isBlank(taskId)) {
            throw new IllegalArgumentException("延迟任务ID不能为空");
        }
        if (executeTime == null) {
            throw new IllegalArgumentException("延迟任务执行时间不能为空");
        }

        stringRedisTemplate.opsForZSet().add(DELAY_QUEUE_KEY, taskId, executeTime.toEpochMilli());
        log.info("添加Redis延迟任务，taskId={}，executeTime={}", taskId, executeTime);
    }

    /**
     * 获取到期任务
     *
     * @param limit 最大数量
     * @return 到期任务 ID 集合
     */
    public Set<String> getDueTasks(long limit) {
        long now = Instant.now().toEpochMilli();
        return stringRedisTemplate.opsForZSet()
                .rangeByScore(DELAY_QUEUE_KEY, 0, now, 0, limit);
    }

    /**
     * 删除延迟任务
     *
     * @param taskId 任务 ID
     */
    public void removeTask(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            return;
        }

        stringRedisTemplate.opsForZSet().remove(DELAY_QUEUE_KEY, taskId);
        log.info("删除Redis延迟任务，taskId={}", taskId);
    }

}
```

Redis ZSet 扫描任务示例：

```java
package io.github.atengk.kafka.delay;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Redis 延迟任务扫描器
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisDelayTaskScanner {

    private final RedisDelayQueueService redisDelayQueueService;

    /**
     * 扫描到期延迟任务
     */
    @Scheduled(fixedDelay = 5000)
    public void scanDueTasks() {
        Set<String> taskIds = redisDelayQueueService.getDueTasks(100);
        if (taskIds == null || taskIds.isEmpty()) {
            return;
        }

        for (String taskId : taskIds) {
            try {
                log.info("处理到期Redis延迟任务，taskId={}", taskId);

                // 实际项目中应根据 taskId 查询任务详情，然后发送 Kafka 消息

                redisDelayQueueService.removeTask(taskId);
            } catch (Exception e) {
                log.error("处理Redis延迟任务失败，taskId={}", taskId, e);
            }
        }
    }

}
```

Redis ZSet 方案注意事项如下：

1. Redis ZSet 适合中短期延迟，不适合长期大量任务唯一存储。
2. 任务详情建议落数据库，Redis 只作为调度索引。
3. 多实例扫描时需要通过 Lua 脚本或分布式锁保证原子抢占。
4. 处理成功后要删除 ZSet 中的任务 ID。
5. Redis 数据丢失时，需要从数据库任务表恢复。
6. 对核心业务延迟任务，不能只依赖 Redis。

### 延迟消息补偿机制

延迟消息补偿机制用于处理延迟任务丢失、扫描失败、发送 Kafka 失败、Redis 数据丢失、任务长期卡在处理中等异常情况。只要延迟消息参与重要业务流程，就必须设计补偿机制。

常见异常场景如下：

| 异常场景            | 影响                 | 补偿方式                     |
| ------------------- | -------------------- | ---------------------------- |
| 应用重启            | 内存时间轮任务丢失   | 从数据库重新加载任务         |
| Redis 数据丢失      | ZSet 调度索引丢失    | 从数据库重建 ZSet            |
| 扫描任务失败        | 到期任务未触发       | 下次扫描继续处理             |
| Kafka 发送失败      | 延迟消息未投递       | 增加重试次数并更新执行时间   |
| 任务卡在 PROCESSING | 任务无法再次扫描     | 定时恢复超时任务             |
| 重复扫描            | 任务被多实例重复处理 | 状态抢占、唯一约束、幂等消费 |

补偿任务 SQL 示例：

```sql
UPDATE kafka_delay_task
SET task_status = 'PENDING',
    update_time = NOW()
WHERE task_status = 'PROCESSING'
  AND update_time < DATE_SUB(NOW(), INTERVAL 10 MINUTE);
```

该 SQL 用于将长时间处于 `PROCESSING` 状态的任务恢复为 `PENDING`，让后续调度任务重新处理。

延迟任务失败重试 SQL 示例：

```sql
UPDATE kafka_delay_task
SET task_status = 'PENDING',
    retry_count = retry_count + 1,
    execute_time = DATE_ADD(NOW(), INTERVAL 5 MINUTE),
    error_message = 'Kafka发送失败，等待下次重试',
    update_time = NOW()
WHERE task_id = 'TASK_10001'
  AND retry_count < max_retry_count;
```

超过最大重试次数后更新为失败：

```sql
UPDATE kafka_delay_task
SET task_status = 'FAILED',
    error_message = '超过最大重试次数',
    update_time = NOW()
WHERE task_id = 'TASK_10001'
  AND retry_count >= max_retry_count;
```

延迟消息补偿要求如下：

1. 延迟任务必须有唯一任务 ID。
2. 任务状态必须可查询、可更新、可补偿。
3. 到期任务扫描必须支持幂等处理。
4. Kafka 发送失败后不能直接丢弃任务。
5. 超过最大重试次数必须告警。
6. 内存或 Redis 调度方案必须有数据库持久化兜底。
7. 延迟消息最终进入 Kafka 后，Consumer 仍必须做幂等处理。

不同延迟方案选择建议如下：

| 方案         | 推荐场景                             |
| ------------ | ------------------------------------ |
| Retry Topic  | 消费失败后的固定阶梯重试             |
| 数据库任务表 | 重要业务延迟任务、需要审计和补偿     |
| Redis ZSet   | 中短期延迟、较高吞吐、可由数据库兜底 |
| 时间轮       | 大量短延迟任务，需要较高调度性能     |
| 专用延迟队列 | 延迟能力是核心基础设施时考虑         |

最终建议是：普通失败重试使用 Retry Topic 或数据库任务表；重要业务延迟任务优先使用数据库任务表；高吞吐短延迟场景可以使用 Redis ZSet 或时间轮，但必须有持久化和补偿机制。


## 批量消息

本章节用于说明 Kafka 批量消息的开发方式，包括批量发送配置、批量消费配置、批量消息模型、批量异常处理、批量提交 Offset、批量消费幂等和批量性能优化。

Kafka 的批量能力主要分为两类：Producer 端的批量聚合发送，以及 Consumer 端的批量拉取消费。Producer 批量发送通常由 Kafka 客户端根据 `batch.size`、`linger.ms` 等参数自动聚合完成；Consumer 批量消费则需要通过监听容器开启 batch 模式，让监听方法一次接收多条消息。

### 批量发送配置

批量发送配置用于提升 Producer 吞吐能力。Kafka Producer 并不要求业务代码一次性传入多条消息，它会在客户端内部按照 Topic、Partition 对消息进行批量聚合后发送。

推荐配置如下：

```yaml
spring:
  kafka:
    producer:
      # 等待所有同步副本确认，提高可靠性
      acks: all

      # 发送失败重试次数
      retries: 3

      properties:
        # 开启幂等发送
        enable.idempotence: true

        # 单个批次大小，单位字节
        batch.size: 32768

        # 等待更多消息进入同一批次的时间，单位毫秒
        linger.ms: 20

        # Producer 可用总缓冲区大小，单位字节
        buffer.memory: 33554432

        # 消息压缩方式，批量消息通常建议开启压缩
        compression.type: lz4

        # 单条消息发送完整生命周期超时时间
        delivery.timeout.ms: 120000

        # 单次请求 Broker 响应超时时间
        request.timeout.ms: 30000
```

核心参数说明如下：

| 参数                  | 说明                        | 建议                                      |
| --------------------- | --------------------------- | ----------------------------------------- |
| `batch.size`          | 单个 Partition 批次缓冲大小 | 普通业务可设置为 `32768`                  |
| `linger.ms`           | Producer 等待形成批次的时间 | 吞吐优先可设置 `10` 到 `50`               |
| `buffer.memory`       | Producer 总缓冲区大小       | 大流量场景可适当调大                      |
| `compression.type`    | 批次压缩方式                | JSON 消息推荐 `lz4` 或 `zstd`             |
| `delivery.timeout.ms` | 发送总超时时间              | 需要大于 `linger.ms + request.timeout.ms` |

批量发送使用建议如下：

1. 低延迟接口不宜设置过大的 `linger.ms`。
2. 日志、埋点、同步类消息可以适当增大 `batch.size` 和 `linger.ms`。
3. 开启压缩后可以降低网络和磁盘成本，但会增加 CPU 消耗。
4. 批量发送调优必须结合压测结果，不应只凭经验配置。
5. Producer 内部批量聚合不等于业务批量事务，业务仍需处理单条消息失败和幂等问题。

业务侧循环发送示例：

```java
for (OrderCreatedMessage message : messages) {
    String key = "ORDER:" + message.getOrderId();
    kafkaTemplate.send(KafkaTopicConstant.ORDER_CREATED_TOPIC, key, message);
}
```

上述代码虽然是逐条调用 `send`，但 Kafka Producer 会根据配置自动将消息聚合成批次发送。

### 批量消费配置

批量消费配置用于让 Consumer 一次接收多条消息。它适合日志采集、行为埋点、批量入库、批量写 Elasticsearch、批量同步外部系统等场景。

批量消费需要开启监听容器的 `batchListener`。

文件位置：`src/main/java/io/github/atengk/kafka/config/KafkaBatchListenerConfig.java`

该配置类用于定义 Kafka 批量消费监听容器，开启批量监听并使用手动提交 Offset。

```java
package io.github.atengk.kafka.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

/**
 * Kafka 批量消费监听配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Configuration
public class KafkaBatchListenerConfig {

    /**
     * 创建批量消费监听容器工厂
     *
     * @param consumerFactory 消费者工厂
     * @return 批量消费监听容器工厂
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaBatchListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        log.info("初始化Kafka批量消费监听容器，batchListener=true，ackMode=MANUAL_IMMEDIATE，concurrency=3");
        return factory;
    }

}
```

批量消费配置示例：

```yaml
spring:
  kafka:
    consumer:
      properties:
        # 单次 poll 最大拉取消息数
        max.poll.records: 100

        # 两次 poll 最大间隔，批量处理较慢时需要适当调大
        max.poll.interval.ms: 300000

    listener:
      # 批量监听模式
      type: batch

      # 手动提交 Offset
      ack-mode: manual_immediate
```

批量监听示例：

```java
package io.github.atengk.kafka.consumer;

import io.github.atengk.kafka.constant.KafkaGroupConstant;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

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
     * 批量消费业务消息
     *
     * @param records        Kafka 消息列表
     * @param acknowledgment Offset 确认对象
     */
    @KafkaListener(
            topics = KafkaTopicConstant.USER_OPERATE_LOG_TOPIC,
            groupId = KafkaGroupConstant.LOG_CONSUMER_GROUP,
            containerFactory = "kafkaBatchListenerContainerFactory"
    )
    public void consumeBatch(List<ConsumerRecord<String, Object>> records,
                             Acknowledgment acknowledgment) {
        if (records == null || records.isEmpty()) {
            return;
        }

        try {
            log.info("开始批量消费Kafka消息，count={}，topic={}", records.size(), records.get(0).topic());

            for (ConsumerRecord<String, Object> record : records) {
                log.info("处理批量消息，topic={}，partition={}，offset={}，key={}",
                        record.topic(), record.partition(), record.offset(), record.key());

                // 实际业务处理
            }

            acknowledgment.acknowledge();
            log.info("批量Kafka消息消费成功，count={}", records.size());
        } catch (Exception e) {
            log.error("批量Kafka消息消费失败，count={}", records.size(), e);
            throw e;
        }
    }

}
```

批量消费适合吞吐优先场景。如果每条消息都涉及复杂事务、强幂等、强顺序或独立失败处理，优先使用单条消费。

### 批量消息模型

批量消息模型有两种设计方式：多条普通消息组成批次发送，或者一条 Kafka 消息内部包含业务列表。两种方式适用场景不同。

| 模型            | 说明                                                      | 适用场景                       |
| --------------- | --------------------------------------------------------- | ------------------------------ |
| 多条 Kafka 消息 | 每条业务数据对应一条 Kafka 消息，由 Producer 自动批量发送 | 推荐通用方案                   |
| 单条批量消息    | 一条 Kafka 消息中包含 `List<T>`                           | 批量导入、批量同步、一次性任务 |

推荐优先使用多条 Kafka 消息，因为它更容易做到单条幂等、单条重试、单条死信和单条追踪。

如果业务确实需要一条消息承载多条数据，可以定义批量消息模型。

文件位置：`src/main/java/io/github/atengk/kafka/model/BatchMessageEnvelope.java`

该模型用于封装一条 Kafka 批量业务消息，适合一次性批量同步场景。

```java
package io.github.atengk.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Kafka 批量消息信封
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchMessageEnvelope<T> {

    /**
     * 批次 ID
     */
    private String batchId;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 消息版本
     */
    private String version;

    /**
     * 来源服务
     */
    private String source;

    /**
     * 批次大小
     */
    private Integer batchSize;

    /**
     * 发送时间
     */
    private LocalDateTime sendTime;

    /**
     * 批量业务数据
     */
    private List<T> payloadList;

}
```

批量消息示例：

```json
{
  "batchId": "BATCH_1900000000000000001",
  "messageType": "USER_OPERATE_LOG_BATCH",
  "version": "1.0",
  "source": "user-service",
  "batchSize": 2,
  "sendTime": "2026-05-11T10:30:00",
  "payloadList": [
    {
      "userId": "10001",
      "operateType": "LOGIN"
    },
    {
      "userId": "10002",
      "operateType": "LOGOUT"
    }
  ]
}
```

批量消息模型设计要求如下：

1. 必须包含 `batchId`，用于批次级追踪。
2. 每条业务数据仍建议包含单条唯一 ID，便于单条幂等。
3. `batchSize` 应与 `payloadList.size()` 保持一致。
4. 单条批量消息不宜过大，避免超过 Kafka 单条消息大小限制。
5. 批量消息失败时，需要明确是整批失败还是部分失败。

### 批量异常处理

批量异常处理比单条消息更复杂。因为一批消息中可能只有部分消息失败，如果直接抛出异常，整批消息可能都会被重试；如果直接提交 Offset，失败消息又可能丢失。

常见处理策略如下：

| 策略           | 说明                           | 适用场景             |
| -------------- | ------------------------------ | -------------------- |
| 整批失败重试   | 任意一条失败，整批抛异常重试   | 批量强一致场景       |
| 单条失败记录   | 成功的正常处理，失败的写失败表 | 日志、同步类场景     |
| 单条失败进死信 | 失败单条投递死信 Topic         | 可独立处理的业务消息 |
| 拆分重试       | 失败后拆分成单条消息重试       | 批量消息体场景       |
| 跳过失败项     | 记录失败后继续处理             | 非核心数据           |

批量异常处理示例：

```java
package io.github.atengk.kafka.consumer;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.kafka.constant.KafkaGroupConstant;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量异常处理消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class BatchExceptionConsumer {

    /**
     * 批量消费并记录失败项
     *
     * @param records        Kafka 消息列表
     * @param acknowledgment Offset 确认对象
     */
    @KafkaListener(
            topics = KafkaTopicConstant.USER_OPERATE_LOG_TOPIC,
            groupId = KafkaGroupConstant.LOG_CONSUMER_GROUP,
            containerFactory = "kafkaBatchListenerContainerFactory"
    )
    public void consumeBatch(List<ConsumerRecord<String, Object>> records,
                             Acknowledgment acknowledgment) {
        List<ConsumerRecord<String, Object>> failedRecords = new ArrayList<>();

        try {
            for (ConsumerRecord<String, Object> record : records) {
                try {
                    processSingleRecord(record);
                } catch (Exception e) {
                    failedRecords.add(record);
                    log.error("批量消息单条处理失败，topic={}，partition={}，offset={}，key={}",
                            record.topic(), record.partition(), record.offset(), record.key(), e);
                }
            }

            if (CollUtil.isNotEmpty(failedRecords)) {
                // 实际项目中应写失败表或投递死信Topic
                log.warn("批量消息存在失败项，failedCount={}，totalCount={}", failedRecords.size(), records.size());
            }

            acknowledgment.acknowledge();
            log.info("批量消息处理完成，totalCount={}，failedCount={}", records.size(), failedRecords.size());
        } catch (Exception e) {
            log.error("批量消息整体处理异常，count={}", records.size(), e);
            throw e;
        }
    }

    /**
     * 处理单条消息
     *
     * @param record Kafka 消息记录
     */
    private void processSingleRecord(ConsumerRecord<String, Object> record) {
        log.info("处理单条批量消息，topic={}，partition={}，offset={}，key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        // 单条业务处理逻辑
    }

}
```

批量异常处理要求如下：

1. 核心业务不建议简单“部分失败也提交 Offset”，除非失败项已可靠落库或投递死信。
2. 批量入库时应明确事务边界，是整批事务还是单条事务。
3. 批量失败日志必须记录失败条数和失败消息位置。
4. 失败项必须可查询、可重试、可补偿。
5. 批量处理成功后再提交 Offset。

### 批量提交 Offset

批量消费时，Offset 提交通常代表本批次消息已经处理完成。如果整批中仍存在未处理成功的消息，就不能简单提交 Offset，除非失败消息已经进入可靠补偿链路。

批量手动提交示例：

```java
@KafkaListener(
        topics = KafkaTopicConstant.USER_OPERATE_LOG_TOPIC,
        groupId = KafkaGroupConstant.LOG_CONSUMER_GROUP,
        containerFactory = "kafkaBatchListenerContainerFactory"
)
public void consumeBatch(List<ConsumerRecord<String, Object>> records,
                         Acknowledgment acknowledgment) {
    try {
        // 批量业务处理
        acknowledgment.acknowledge();
    } catch (Exception e) {
        // 不提交Offset，交给错误处理器重试
        throw e;
    }
}
```

批量 Offset 提交策略如下：

| 策略               | 说明                         |
| ------------------ | ---------------------------- |
| 整批成功后提交     | 最简单，适合整批强一致       |
| 失败项落库后提交   | 适合部分失败可补偿场景       |
| 失败项死信后提交   | 适合单条消息可独立失败的场景 |
| 不提交并整批重试   | 适合批量事务场景             |
| 拆分单条重试后提交 | 适合批量消息体拆分处理       |

批量提交注意事项如下：

1. 批量提交 Offset 后，该批次之前的消息默认不再重新投递。
2. 如果部分失败项没有可靠记录，提交 Offset 会造成消息丢失。
3. 批量处理耗时不能超过 `max.poll.interval.ms`。
4. 批量消费场景需要重点监控处理耗时和失败条数。
5. 批量消费仍可能重复，业务侧仍需幂等。

### 批量消费幂等

批量消费幂等用于保证一批消息中每条消息重复处理时不会产生副作用。不能只用 `batchId` 做幂等，因为一个批次中可能部分成功、部分失败。

幂等粒度建议如下：

| 粒度            | 说明                   | 建议               |
| --------------- | ---------------------- | ------------------ |
| 批次级幂等      | 按 `batchId` 去重      | 只适合整批原子处理 |
| 单条级幂等      | 按 `messageId` 去重    | 推荐               |
| 业务级幂等      | 按业务唯一键去重       | 核心业务必须具备   |
| 批次 + 单条幂等 | 同时记录批次和单条状态 | 批量业务推荐       |

批量消费幂等表建议：

```sql
CREATE TABLE kafka_batch_consume_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    batch_id VARCHAR(64) DEFAULT NULL COMMENT '批次ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息唯一标识',
    consumer_group VARCHAR(128) NOT NULL COMMENT '消费组',
    business_key VARCHAR(255) DEFAULT NULL COMMENT '业务幂等键',
    topic VARCHAR(128) NOT NULL COMMENT 'Topic名称',
    partition_id INT NOT NULL COMMENT '分区编号',
    offset_value BIGINT NOT NULL COMMENT 'Offset',
    consume_status VARCHAR(32) NOT NULL COMMENT '消费状态',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '异常信息',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_message_group (message_id, consumer_group),
    KEY idx_batch_id (batch_id),
    KEY idx_consume_status (consume_status)
) COMMENT='Kafka批量消费记录表';
```

批量幂等处理流程如下：

```text
批量拉取消息
  -> 遍历每条消息
  -> 按 messageId + consumerGroup 判断是否已处理
  -> 已处理则跳过
  -> 未处理则插入 PROCESSING
  -> 执行业务逻辑
  -> 成功更新 SUCCESS
  -> 失败记录 FAILED 或进入死信
  -> 全部成功或失败项已可靠记录后提交 Offset
```

批量消费幂等要求如下：

1. 每条消息都必须有唯一标识。
2. 批次 ID 不能替代单条消息 ID。
3. 批量入库建议使用唯一索引兜底。
4. 失败项必须保留处理状态。
5. 死信重放时仍按单条消息幂等处理。

### 批量性能优化

批量性能优化需要同时关注 Producer、Consumer、Broker、数据库和下游系统。只增大 Kafka 批量参数而不优化业务处理，可能会把瓶颈转移到数据库或外部接口。

优化方向如下：

| 层面     | 优化项                                                       |
| -------- | ------------------------------------------------------------ |
| Producer | 调整 `batch.size`、`linger.ms`、`compression.type`、`buffer.memory` |
| Consumer | 调整 `max.poll.records`、`concurrency`、批量监听模式         |
| Topic    | 合理规划 Partition 数量                                      |
| 数据库   | 使用批量插入、批量更新、索引优化                             |
| 下游接口 | 支持批量接口或限流保护                                       |
| 消息体   | 减少无用字段，避免大消息                                     |
| JVM      | 关注 GC、线程池、堆内存                                      |
| 监控     | 观察 Consumer Lag、处理耗时、失败率                          |

推荐配置示例：

```yaml
spring:
  kafka:
    producer:
      properties:
        batch.size: 65536
        linger.ms: 50
        compression.type: lz4
        buffer.memory: 67108864

    consumer:
      properties:
        max.poll.records: 500
        max.poll.interval.ms: 600000

    listener:
      concurrency: 6
      type: batch
```

性能优化注意事项如下：

1. `max.poll.records` 增大后，单次业务处理耗时也会增加。
2. 批量消费并发数不应超过 Topic 分区数太多。
3. 批量入库需要使用数据库批处理能力，避免逐条写库。
4. 批量消息失败处理要先设计清楚，再做吞吐优化。
5. 大批量处理必须监控 Consumer Lag 和处理耗时。
6. 所有参数调整都应通过压测验证。

## 序列化与反序列化

本章节用于说明 Kafka 消息的序列化与反序列化设计，包括 String 序列化、JSON 序列化、自定义序列化器、自定义反序列化器、反序列化异常处理、类型映射配置和消息兼容性设计。

Kafka 传输的是字节数组，Producer 发送前需要将对象序列化为字节，Consumer 消费时需要将字节反序列化为对象。序列化方案直接影响消息可读性、跨语言能力、性能、兼容性和异常处理方式。

### String 序列化

String 序列化是最简单的方式，适合发送纯文本、简单 JSON 字符串、日志字符串或调试消息。它使用 Kafka 原生 `StringSerializer` 和 `StringDeserializer`。

配置示例：

```yaml
spring:
  kafka:
    producer:
      # Key 使用字符串序列化
      key-serializer: org.apache.kafka.common.serialization.StringSerializer

      # Value 使用字符串序列化
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

    consumer:
      # Key 使用字符串反序列化
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer

      # Value 使用字符串反序列化
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
```

发送字符串消息示例：

```java
kafkaTemplate.send(
        KafkaTopicConstant.USER_OPERATE_LOG_TOPIC,
        "USER:10001",
        "{\"userId\":\"10001\",\"operateType\":\"LOGIN\"}"
);
```

String 序列化适用场景：

| 场景           | 说明                                |
| -------------- | ----------------------------------- |
| 本地调试       | 使用控制台 Producer / Consumer 方便 |
| 简单日志       | 直接传输文本日志                    |
| 跨语言简单接入 | 各语言都容易处理字符串              |
| 临时验证       | 不需要复杂对象模型                  |

String 序列化的限制如下：

1. 业务对象需要手动转换 JSON。
2. 字段校验和类型约束较弱。
3. 消费端需要自行解析字符串。
4. 不适合复杂对象长期维护。
5. 不利于统一消息模型和版本管理。

### JSON 序列化

JSON 序列化是 Spring Boot Kafka 项目中最常用的方案。它可读性好、调试方便、接入成本低，适合大多数业务消息。

推荐配置如下：

```yaml
spring:
  kafka:
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer

      # 使用 Spring Kafka JSON 序列化器
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

      properties:
        # 不添加 Java 类型 Header，降低跨服务和跨语言耦合
        spring.json.add.type.headers: false

    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer

      # 使用错误处理反序列化器包裹 JsonDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer

      properties:
        # 委托 JSON 反序列化器
        spring.deserializer.value.delegate.class: org.springframework.kafka.support.serializer.JsonDeserializer

        # 可信包路径，不建议生产环境使用 *
        spring.json.trusted.packages: io.github.atengk.kafka.model,java.util,java.lang

        # 不使用 Header 中的 Java 类型信息
        spring.json.use.type.headers: false

        # 默认反序列化类型
        spring.json.value.default.type: io.github.atengk.kafka.model.MessageEnvelope
```

JSON 消息示例：

```json
{
  "messageId": "1900000000000000001",
  "messageType": "ORDER_CREATED",
  "version": "1.0",
  "bizKey": "ORDER:10001",
  "traceId": "trace-20260511-001",
  "sendTime": "2026-05-11T10:30:00",
  "payload": {
    "orderId": "10001",
    "userId": "20001",
    "amount": 199.90
  }
}
```

JSON 序列化建议如下：

1. 普通业务消息优先使用 JSON。
2. 字段命名统一使用小驼峰。
3. 时间格式统一，例如 ISO-8601。
4. 金额字段使用 `BigDecimal`，不使用 `double`。
5. 不建议依赖 Java 类型 Header。
6. 消费端应兼容新增字段和缺失非核心字段。

### 自定义序列化器

自定义序列化器适合需要统一加密、压缩、脱敏、特殊 JSON 配置、兼容旧系统或统一消息包装的场景。普通业务优先使用 Spring Kafka 内置 JSON 序列化器，只有明确需要时再自定义。

文件位置：`src/main/java/io/github/atengk/kafka/serializer/AtengJsonSerializer.java`

该序列化器用于将 Java 对象序列化为 JSON 字节数组，并统一处理空值和序列化异常。

```java
package io.github.atengk.kafka.serializer;

import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Kafka JSON 自定义序列化器
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class AtengJsonSerializer implements Serializer<Object> {

    private final ObjectMapper objectMapper;

    public AtengJsonSerializer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 序列化 Kafka 消息
     *
     * @param topic Topic 名称
     * @param data  消息对象
     * @return JSON 字节数组
     */
    @Override
    public byte[] serialize(String topic, Object data) {
        if (ObjectUtil.isNull(data)) {
            return new byte[0];
        }

        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            log.error("Kafka消息序列化失败，topic={}，dataType={}",
                    topic, data.getClass().getName(), e);
            throw new SerializationException("Kafka消息序列化失败", e);
        }
    }

}
```

配置自定义序列化器：

```yaml
spring:
  kafka:
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer

      # 使用自定义 JSON 序列化器
      value-serializer: io.github.atengk.kafka.serializer.AtengJsonSerializer
```

自定义序列化器使用要求如下：

1. 序列化失败必须抛出异常，不能返回错误内容。
2. 不要在日志中打印完整敏感消息体。
3. ObjectMapper 配置应全局一致。
4. 时间、枚举、BigDecimal 等类型需要提前验证。
5. 修改序列化逻辑前必须评估历史消费者兼容性。

### 自定义反序列化器

自定义反序列化器适合需要统一解析消息信封、兼容旧格式、处理特殊字段、做基础校验或进行脱敏读取的场景。需要注意，反序列化阶段不应执行业务逻辑，也不应访问数据库或外部服务。

文件位置：`src/main/java/io/github/atengk/kafka/serializer/AtengMessageEnvelopeDeserializer.java`

该反序列化器用于将 JSON 字节数组反序列化为 `MessageEnvelope` 对象。

```java
package io.github.atengk.kafka.serializer;

import cn.hutool.core.util.ArrayUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.atengk.kafka.model.MessageEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * Kafka 消息信封反序列化器
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class AtengMessageEnvelopeDeserializer implements Deserializer<MessageEnvelope<?>> {

    private final ObjectMapper objectMapper;

    public AtengMessageEnvelopeDeserializer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 反序列化 Kafka 消息
     *
     * @param topic Topic 名称
     * @param data  消息字节数组
     * @return 消息信封
     */
    @Override
    public MessageEnvelope<?> deserialize(String topic, byte[] data) {
        if (ArrayUtil.isEmpty(data)) {
            return null;
        }

        try {
            return objectMapper.readValue(data, MessageEnvelope.class);
        } catch (Exception e) {
            log.error("Kafka消息反序列化失败，topic={}，payloadSize={}", topic, data.length, e);
            throw new SerializationException("Kafka消息反序列化失败", e);
        }
    }

}
```

配置自定义反序列化器时，建议仍然用 `ErrorHandlingDeserializer` 包裹，避免反序列化异常直接导致消费线程异常退出。

```yaml
spring:
  kafka:
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer

      # 使用错误处理反序列化器
      value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer

      properties:
        # 委托自定义反序列化器处理消息体
        spring.deserializer.value.delegate.class: io.github.atengk.kafka.serializer.AtengMessageEnvelopeDeserializer
```

自定义反序列化器要求如下：

1. 只做格式转换，不做业务处理。
2. 反序列化失败必须抛出标准异常。
3. 不要吞掉异常后返回空对象。
4. 不要打印完整敏感消息体。
5. 历史消息格式变更时，需要兼容旧字段。
6. 重要消费链路必须配合反序列化异常处理机制。

### 反序列化异常处理

反序列化异常通常发生在消息进入 Listener 方法之前。如果没有正确配置，Consumer 可能无法将异常消息交给业务监听方法处理，导致消费线程反复失败或卡住。

推荐使用 `ErrorHandlingDeserializer`：

```yaml
spring:
  kafka:
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer

      # 包裹真实反序列化器，捕获反序列化异常
      value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer

      properties:
        # 真实 JSON 反序列化器
        spring.deserializer.value.delegate.class: org.springframework.kafka.support.serializer.JsonDeserializer

        # 默认消息类型
        spring.json.value.default.type: io.github.atengk.kafka.model.MessageEnvelope

        # 可信包路径
        spring.json.trusted.packages: io.github.atengk.kafka.model,java.util,java.lang
```

错误处理器中将反序列化异常标记为不可重试：

```java
errorHandler.addNotRetryableExceptions(
        org.springframework.kafka.support.serializer.DeserializationException.class,
        IllegalArgumentException.class
);
```

反序列化异常处理建议如下：

| 异常原因       | 处理方式                   |
| -------------- | -------------------------- |
| JSON 格式错误  | 不重试，进入死信或失败记录 |
| 类型不匹配     | 不重试，检查消息模型版本   |
| 缺少必要字段   | 视业务决定，通常不重试     |
| 时间格式错误   | 不重试，修复 Producer      |
| 历史版本不兼容 | 增加兼容逻辑或版本转换     |
| 非法枚举值     | 不重试或按默认值降级       |

反序列化异常日志必须包含 Topic、Partition、Offset、Key 和异常摘要。如果消息内容可能包含敏感字段，不要直接打印完整原文。

### 类型映射配置

类型映射用于在使用 JSON 序列化时，将消息类型与 Java 类型建立映射。Spring Kafka 支持通过类型 Header 或配置方式进行类型映射。但在跨服务、跨语言或长期演进场景中，不建议强依赖 Java 类型 Header。

如果使用类型映射，可以配置：

```yaml
spring:
  kafka:
    producer:
      properties:
        # 类型映射，发送时将 Java 类型映射为短名称
        spring.json.type.mapping: orderCreated:io.github.atengk.kafka.model.OrderCreatedMessage,paymentSuccess:io.github.atengk.kafka.model.PaymentSuccessMessage

    consumer:
      properties:
        # 消费端使用相同映射
        spring.json.type.mapping: orderCreated:io.github.atengk.kafka.model.OrderCreatedMessage,paymentSuccess:io.github.atengk.kafka.model.PaymentSuccessMessage
```

类型映射适用场景：

| 场景                   | 是否适合                        |
| ---------------------- | ------------------------------- |
| 单 Java 技术栈内部系统 | 可以使用                        |
| 多语言消费             | 不建议依赖 Java 类型            |
| 统一消息信封模型       | 通常不需要类型 Header           |
| 消息类型较多           | 可用 `messageType` 字段自行分发 |
| 强 Schema 管理         | 更建议 Avro / Protobuf          |

推荐方式是使用统一消息信封，并通过 `messageType` 字段做业务分发。

```json
{
  "messageType": "ORDER_CREATED",
  "payload": {
    "orderId": "10001"
  }
}
```

消费端可以根据 `messageType` 将 `payload` 转换为具体业务对象。

文件位置：`src/main/java/io/github/atengk/kafka/support/KafkaPayloadConverter.java`

该转换器用于根据目标类型转换消息载荷，适合统一消息信封场景。

```java
package io.github.atengk.kafka.support;

import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Kafka 消息载荷转换器
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Component
@RequiredArgsConstructor
public class KafkaPayloadConverter {

    private final ObjectMapper objectMapper;

    /**
     * 转换消息载荷
     *
     * @param payload    原始载荷
     * @param targetType 目标类型
     * @param <T>        目标类型泛型
     * @return 转换后的对象
     */
    public <T> T convert(Object payload, Class<T> targetType) {
        if (ObjectUtil.isNull(payload)) {
            return null;
        }
        return objectMapper.convertValue(payload, targetType);
    }

}
```

这种方式可以减少对 Java 类型 Header 的依赖，更适合跨服务长期维护。

### 消息兼容性设计

消息兼容性设计用于保证 Producer 和 Consumer 在不同版本发布过程中不会因为字段变化导致消费失败。Kafka 消息通常会在 Topic 中保留一段时间，因此 Consumer 可能同时读到新版本和旧版本消息。

常见兼容性变更如下：

| 变更类型         | 是否兼容 | 说明                         |
| ---------------- | -------- | ---------------------------- |
| 新增非必填字段   | 通常兼容 | 旧消费者会忽略未知字段       |
| 新增有默认值字段 | 通常兼容 | 消费端可以使用默认值         |
| 删除字段         | 不兼容   | 旧消费者可能依赖该字段       |
| 修改字段类型     | 不兼容   | 可能导致反序列化失败         |
| 修改字段含义     | 不兼容   | 最危险，可能导致业务错误     |
| 枚举新增值       | 部分兼容 | 旧消费者需要有默认处理       |
| 字段重命名       | 不兼容   | 等价于删除旧字段、新增新字段 |

兼容性设计要求如下：

1. 所有消息必须包含 `version` 字段。
2. 新增字段优先设计为非必填。
3. 消费端对未知字段应保持兼容。
4. 不要直接删除仍有消费者依赖的字段。
5. 不要随意修改字段类型和字段语义。
6. 枚举字段必须有未知值兜底处理。
7. 大版本不兼容变更建议使用新 `messageType` 或新 Topic。
8. 发布顺序通常应先升级 Consumer，再升级 Producer。

推荐版本演进方式如下：

```text
v1.0:
{
  "orderId": "10001",
  "amount": 199.90
}

v1.1:
{
  "orderId": "10001",
  "amount": 199.90,
  "currency": "CNY"
}
```

`v1.1` 新增 `currency` 字段属于兼容变更，旧消费者可以忽略，新消费者可以在字段缺失时使用默认值。

不推荐变更：

```text
v1.0:
{
  "amount": "199.90"
}

v2.0:
{
  "amount": 199.90
}
```

这种变更将 `amount` 从字符串改成数字，可能导致旧消费者反序列化失败。更稳妥的方式是新增字段，例如 `amountValue`，等待所有消费者升级后再废弃旧字段。

消息兼容性最终建议如下：

| 场景             | 建议                                 |
| ---------------- | ------------------------------------ |
| 普通 JSON 消息   | 使用统一信封、版本字段和兼容新增字段 |
| 多语言强约束     | 使用 Avro 或 Protobuf                |
| 字段变更频繁     | 建立消息契约评审流程                 |
| 核心业务事件     | 变更前确认所有消费方                 |
| 历史消息保留较久 | Consumer 必须长期兼容旧版本          |
| 不兼容升级       | 使用新 Topic 或新 messageType 过渡   |


## Schema 管理

本章节用于说明 Kafka 消息 Schema 的管理方式，包括 Schema Registry 概念、Avro 集成、Protobuf 集成、Schema 版本管理、向前兼容、向后兼容和 Schema 演进规范。

在 Kafka 项目中，如果消息只在少量 Java 服务之间流转，且团队对消息结构变化控制较好，可以优先使用 JSON 模型。如果消息需要跨语言消费、长期保留、频繁演进、接入数据平台或要求强 Schema 校验，则应引入 Schema 管理机制。常见方案是使用 Schema Registry 管理 Avro、Protobuf 或 JSON Schema。Confluent Schema Registry 提供 REST 接口用于存储和获取 Schema，并基于 Subject 维护 Schema 的版本历史和兼容性规则。([Confluent 文档](https://docs.confluent.io/platform/7.5/schema-registry/fundamentals/index.html?utm_source=chatgpt.com))

### Schema Registry 概念

Schema Registry 是消息结构的集中管理服务。Producer 发送消息时，可以将消息对应的 Schema 注册到 Schema Registry；Consumer 消费消息时，可以根据消息中的 Schema ID 或 Subject 信息获取对应 Schema，再完成反序列化和兼容性校验。

Schema Registry 主要解决以下问题：

| 问题             | 说明                                                       |
| ---------------- | ---------------------------------------------------------- |
| 消息结构分散     | 不同服务各自维护消息模型，容易出现字段不一致               |
| 版本不可控       | Producer 修改字段后，Consumer 可能无法解析历史消息或新消息 |
| 兼容性缺失       | 字段删除、类型修改、枚举变更可能直接导致消费失败           |
| 跨语言困难       | Java、Go、Python 等语言需要统一消息契约                    |
| 数据平台接入困难 | 数据湖、实时计算、报表系统需要稳定 Schema                  |

Schema Registry 常见核心概念如下：

| 概念          | 说明                                                         |
| ------------- | ------------------------------------------------------------ |
| Schema        | 消息结构定义，例如 Avro Schema、Protobuf `.proto`、JSON Schema |
| Subject       | Schema 的注册主体，通常与 Topic、Key、Value 相关             |
| Version       | 同一个 Subject 下的 Schema 版本号                            |
| Schema ID     | Schema Registry 为 Schema 分配的全局唯一 ID                  |
| Compatibility | 兼容性规则，例如 BACKWARD、FORWARD、FULL                     |
| Serializer    | Producer 侧序列化器，负责注册或查找 Schema                   |
| Deserializer  | Consumer 侧反序列化器，负责根据 Schema 解析消息              |

常见 Subject 命名策略如下：

| 策略                    | 示例                                                         | 说明                                       |
| ----------------------- | ------------------------------------------------------------ | ------------------------------------------ |
| TopicNameStrategy       | `ateng.order.created.topic-value`                            | 默认常见策略，按 Topic 的 Key / Value 区分 |
| RecordNameStrategy      | `io.github.atengk.kafka.schema.OrderCreatedEvent`            | 按记录类型区分                             |
| TopicRecordNameStrategy | `ateng.order.created.topic-io.github.atengk.kafka.schema.OrderCreatedEvent` | Topic 和记录类型共同区分                   |

项目建议：

1. 普通业务系统优先使用 `TopicNameStrategy`，便于按 Topic 管理消息结构。
2. 多种事件共用一个 Topic 时，可以考虑 `RecordNameStrategy` 或 `TopicRecordNameStrategy`。
3. 一个 Topic 中不建议混放过多完全不同的消息结构。
4. Schema Registry 地址、认证信息和兼容性策略应按环境独立配置。
5. Schema 变更应进入接口契约评审流程，不应由单个服务随意修改。

### Avro 集成

Avro 是 Kafka 生态中常见的强 Schema 序列化方案，适合数据平台、实时计算、跨系统数据交换和需要 Schema 演进的场景。Schema Registry 最初主要支持 Avro，后续也支持 Protobuf 和 JSON Schema。([Confluent 文档](https://docs.confluent.io/platform/7.5/schema-registry/fundamentals/index.html?utm_source=chatgpt.com))

Maven 依赖示例：

文件位置：`pom.xml`

以下依赖用于 Spring Boot 项目集成 Confluent Avro Serializer。具体版本应由项目统一依赖管理控制。

```xml
<properties>
    <!-- Confluent 版本应与公司 Kafka / Schema Registry 平台版本保持一致 -->
    <confluent.version>${confluent.version}</confluent.version>
</properties>

<repositories>
    <!-- Confluent 组件仓库 -->
    <repository>
        <id>confluent</id>
        <url>https://packages.confluent.io/maven/</url>
    </repository>
</repositories>

<dependencies>
    <!-- Spring Kafka 基础依赖 -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>

    <!-- Avro Schema 支持 -->
    <dependency>
        <groupId>org.apache.avro</groupId>
        <artifactId>avro</artifactId>
    </dependency>

    <!-- Confluent Avro 序列化器和反序列化器 -->
    <dependency>
        <groupId>io.confluent</groupId>
        <artifactId>kafka-avro-serializer</artifactId>
        <version>${confluent.version}</version>
    </dependency>
</dependencies>
```

Avro Schema 示例：

文件位置：`src/main/avro/OrderCreatedEvent.avsc`

该 Schema 用于定义订单创建事件结构，字段应尽量提供默认值，便于后续兼容演进。

```json
{
  "type": "record",
  "name": "OrderCreatedEvent",
  "namespace": "io.github.atengk.kafka.schema",
  "fields": [
    {
      "name": "messageId",
      "type": "string"
    },
    {
      "name": "messageType",
      "type": "string",
      "default": "ORDER_CREATED"
    },
    {
      "name": "version",
      "type": "string",
      "default": "1.0"
    },
    {
      "name": "bizKey",
      "type": "string"
    },
    {
      "name": "orderId",
      "type": "string"
    },
    {
      "name": "userId",
      "type": "string"
    },
    {
      "name": "amount",
      "type": "string"
    },
    {
      "name": "currency",
      "type": "string",
      "default": "CNY"
    },
    {
      "name": "remark",
      "type": [
        "null",
        "string"
      ],
      "default": null
    }
  ]
}
```

Avro Producer 配置示例：

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}

    producer:
      # Key 使用字符串序列化
      key-serializer: org.apache.kafka.common.serialization.StringSerializer

      # Value 使用 Avro 序列化器
      value-serializer: io.confluent.kafka.serializers.KafkaAvroSerializer

      properties:
        # Schema Registry 地址
        schema.registry.url: ${SCHEMA_REGISTRY_URL:http://localhost:8081}

        # 是否自动注册 Schema，开发环境可开启，生产环境建议结合发布流程控制
        auto.register.schemas: false

        # 使用最新版本 Schema，生产环境需谨慎开启
        use.latest.version: false
```

Avro Consumer 配置示例：

```yaml
spring:
  kafka:
    consumer:
      # Key 使用字符串反序列化
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer

      # Value 使用 Avro 反序列化器
      value-deserializer: io.confluent.kafka.serializers.KafkaAvroDeserializer

      properties:
        # Schema Registry 地址
        schema.registry.url: ${SCHEMA_REGISTRY_URL:http://localhost:8081}

        # true 表示反序列化为生成的 SpecificRecord 类型
        specific.avro.reader: true
```

Avro 使用要求如下：

1. Schema 文件应进入代码仓库，随代码评审。
2. 生产环境不建议随 Producer 自动注册 Schema，应通过 CI/CD 或 Schema 发布流程注册。
3. 字段新增应提供默认值，避免旧消息无法被新 Schema 读取。
4. 金额字段可使用字符串或 Avro 逻辑类型，项目内必须统一。
5. Schema 变更前必须检查兼容性。
6. 消费端升级应先于生产端不兼容变更。

### Protobuf 集成

Protobuf 是跨语言、高性能、二进制编码的 Schema 方案，适合多语言服务、移动端、边缘端、RPC 系统和对消息体积敏感的场景。Confluent Schema Registry 支持 Protobuf，并提供对应的 Kafka Serializer 和 Deserializer。Protobuf 具有语言中立、平台中立和可扩展的特点。([Confluent 文档](https://docs.confluent.io/platform/7.1/schema-registry/serdes-develop/serdes-protobuf.html?utm_source=chatgpt.com))

Maven 依赖示例：

文件位置：`pom.xml`

以下依赖用于 Kafka 集成 Protobuf Schema 序列化。

```xml
<properties>
    <!-- Confluent 版本由项目统一管理 -->
    <confluent.version>${confluent.version}</confluent.version>
    <!-- Protobuf Java 版本由项目统一管理 -->
    <protobuf.version>${protobuf.version}</protobuf.version>
</properties>

<repositories>
    <!-- Confluent 组件仓库 -->
    <repository>
        <id>confluent</id>
        <url>https://packages.confluent.io/maven/</url>
    </repository>
</repositories>

<dependencies>
    <!-- Protobuf Java 运行时 -->
    <dependency>
        <groupId>com.google.protobuf</groupId>
        <artifactId>protobuf-java</artifactId>
        <version>${protobuf.version}</version>
    </dependency>

    <!-- Confluent Protobuf 序列化器和反序列化器 -->
    <dependency>
        <groupId>io.confluent</groupId>
        <artifactId>kafka-protobuf-serializer</artifactId>
        <version>${confluent.version}</version>
    </dependency>
</dependencies>
```

Protobuf Schema 示例：

文件位置：`src/main/proto/order_created_event.proto`

该 `.proto` 文件用于定义订单创建事件结构。字段编号一旦发布，不应随意复用或改变语义。

```protobuf
syntax = "proto3";

package io.github.atengk.kafka.schema;

option java_package = "io.github.atengk.kafka.schema";
option java_outer_classname = "OrderCreatedEventProto";

// 订单创建事件
message OrderCreatedEvent {
  // 消息唯一标识
  string message_id = 1;

  // 消息类型
  string message_type = 2;

  // 消息版本
  string version = 3;

  // 业务Key
  string biz_key = 4;

  // 订单ID
  string order_id = 5;

  // 用户ID
  string user_id = 6;

  // 订单金额，使用字符串避免精度问题
  string amount = 7;

  // 币种
  string currency = 8;
}
```

Protobuf Producer 配置示例：

```yaml
spring:
  kafka:
    producer:
      # Key 使用字符串序列化
      key-serializer: org.apache.kafka.common.serialization.StringSerializer

      # Value 使用 Protobuf 序列化器
      value-serializer: io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer

      properties:
        # Schema Registry 地址
        schema.registry.url: ${SCHEMA_REGISTRY_URL:http://localhost:8081}

        # 生产环境建议关闭自动注册，由发布流程控制 Schema
        auto.register.schemas: false
```

Protobuf Consumer 配置示例：

```yaml
spring:
  kafka:
    consumer:
      # Key 使用字符串反序列化
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer

      # Value 使用 Protobuf 反序列化器
      value-deserializer: io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer

      properties:
        # Schema Registry 地址
        schema.registry.url: ${SCHEMA_REGISTRY_URL:http://localhost:8081}

        # 指定反序列化目标类型
        specific.protobuf.value.type: io.github.atengk.kafka.schema.OrderCreatedEventProto$OrderCreatedEvent
```

Protobuf 使用要求如下：

1. 字段编号发布后不得随意修改。
2. 删除字段后，应保留字段编号，避免未来误用。
3. 字段语义不能随意改变。
4. 多语言项目应统一 `.proto` 发布和依赖生成流程。
5. Protobuf 消息可读性弱于 JSON，本地调试需要配套工具。
6. 对 Protobuf 兼容性要单独评估，不能简单套用 JSON 字段变更习惯。

### Schema 版本管理

Schema 版本管理用于控制消息结构随时间演进。Schema Registry 会为同一个 Subject 下的 Schema 维护版本历史。新 Schema 注册成功后，会获得新的版本号和 Schema ID。Schema Registry 的兼容性检查正是基于这些版本历史完成的。([Confluent 文档](https://docs.confluent.io/platform/7.7/schema-registry/fundamentals/schema-evolution.html?utm_source=chatgpt.com))

版本管理建议如下：

| 规则                                  | 说明                                    |
| ------------------------------------- | --------------------------------------- |
| 每次 Schema 修改必须生成新版本        | 不允许覆盖历史 Schema                   |
| Schema 变更必须经过兼容性检查         | 防止 Producer 发布后 Consumer 无法解析  |
| Schema 文件必须纳入代码评审           | 避免字段随意变更                        |
| 生产环境应关闭自动注册                | 避免应用启动或发送消息时隐式修改 Schema |
| 版本变更应记录变更说明                | 便于排查历史消息问题                    |
| 不兼容变更应使用新 Subject 或新 Topic | 避免影响旧消费者                        |

推荐 Schema 发布流程：

```text
开发修改 Schema
  -> 本地生成代码
  -> 执行兼容性检查
  -> 提交代码评审
  -> CI 注册测试环境 Schema
  -> 测试 Producer / Consumer
  -> 生产发布前注册生产 Schema
  -> 先发布兼容 Consumer
  -> 再发布 Producer
```

Schema Registry 常见兼容性等级包括 `BACKWARD`、`FORWARD`、`FULL`、`BACKWARD_TRANSITIVE`、`FORWARD_TRANSITIVE`、`FULL_TRANSITIVE` 和 `NONE`。这些等级可用于不同 Schema 格式的兼容性检查。([Confluent 文档](https://docs.confluent.io/platform/current/schema-registry/fundamentals/serdes-develop/index.html?utm_source=chatgpt.com))

项目推荐配置：

| 环境     | 推荐兼容策略                               |
| -------- | ------------------------------------------ |
| 本地环境 | `BACKWARD` 或 `NONE`                       |
| 测试环境 | `BACKWARD`                                 |
| 预发环境 | 与生产一致                                 |
| 生产环境 | `BACKWARD_TRANSITIVE` 或 `FULL_TRANSITIVE` |

生产环境不建议使用 `NONE`，因为它会放弃兼容性保护。

### 向前兼容

向前兼容是指旧 Consumer 可以读取新 Producer 发送的新 Schema 消息。也就是说，Producer 升级后，尚未升级的 Consumer 仍能处理新消息。

向前兼容适合以下发布场景：

```text
Producer 先发布
Consumer 后发布
```

常见向前兼容变更如下：

| 变更                     | 是否通常安全 | 说明                     |
| ------------------------ | ------------ | ------------------------ |
| 新增可选字段             | 是           | 旧 Consumer 可以忽略     |
| 新增有默认值字段         | 是           | 旧 Consumer 不依赖该字段 |
| 修改字段类型             | 否           | 旧 Consumer 可能解析失败 |
| 删除旧 Consumer 依赖字段 | 否           | 旧 Consumer 无法正常处理 |
| 修改字段语义             | 否           | 可能导致业务错误         |

JSON 场景中，旧 Consumer 通常会忽略未知字段。Avro、Protobuf 场景下，是否兼容取决于具体 Schema 格式、字段默认值、字段编号和兼容性策略。Confluent 文档也说明，不同 Schema 格式的兼容性规则并不完全相同，需要分别评估。([Confluent 文档](https://docs.confluent.io/platform/7.7/schema-registry/fundamentals/schema-evolution.html?utm_source=chatgpt.com))

向前兼容建议：

1. 新增字段尽量设计为可选。
2. 新增字段必须有默认处理逻辑。
3. 不要让旧 Consumer 必须理解新字段才能工作。
4. 不要修改已有字段类型。
5. 不要改变已有字段语义。
6. Producer 先发场景必须重点验证向前兼容。

### 向后兼容

向后兼容是指新 Consumer 可以读取旧 Producer 或历史 Kafka 消息。由于 Kafka Topic 中的消息会保留一段时间，新版本 Consumer 上线后经常会读取旧版本消息，因此向后兼容在 Kafka 项目中非常重要。

向后兼容适合以下发布场景：

```text
Consumer 先发布
Producer 后发布
```

常见向后兼容变更如下：

| 变更                   | 是否通常安全 | 说明                         |
| ---------------------- | ------------ | ---------------------------- |
| 新 Consumer 兼容旧字段 | 是           | 推荐做法                     |
| 新字段有默认值         | 是           | 旧消息没有该字段时可正常处理 |
| 删除旧字段读取逻辑     | 否           | 读取历史消息时可能失败       |
| 修改字段类型           | 否           | 历史消息可能无法解析         |
| 修改枚举处理逻辑       | 视情况       | 需要有未知值兜底             |

向后兼容示例：

```json
{
  "messageId": "1900000000000000001",
  "messageType": "ORDER_CREATED",
  "version": "1.0",
  "payload": {
    "orderId": "10001",
    "amount": "199.90"
  }
}
```

新版本新增 `currency` 字段后，Consumer 应提供默认值：

```java
String currency = StrUtil.blankToDefault(message.getCurrency(), "CNY");
```

向后兼容建议：

1. 新 Consumer 必须能读取 Topic 保留期内的旧消息。
2. 新增字段应有默认值或空值处理。
3. 删除字段前必须确认 Topic 中旧消息已过保留期，且所有 Consumer 已升级。
4. 枚举字段必须增加未知值兜底。
5. 重要业务消息建议保留版本转换逻辑。
6. Consumer 发布通常应先于 Producer 发布。

### Schema 演进规范

Schema 演进规范用于约束消息结构变更方式，避免随意修改字段导致消费失败或业务错误。Schema 演进应遵循“只做兼容变更，不做隐式破坏性变更”的原则。

推荐变更规则如下：

| 变更类型     | 处理建议                             |
| ------------ | ------------------------------------ |
| 新增字段     | 优先新增可选字段，并设置默认值       |
| 删除字段     | 先废弃，等待所有消费方升级后再删除   |
| 修改字段名称 | 不建议，等价于删除旧字段并新增新字段 |
| 修改字段类型 | 不建议，使用新字段替代               |
| 修改字段含义 | 禁止，容易造成隐式业务错误           |
| 新增枚举值   | Consumer 必须有默认分支              |
| 删除枚举值   | 谨慎，需要确认历史消息不再使用       |
| 字段复用     | 禁止，尤其是 Protobuf 字段编号       |

Schema 演进流程如下：

```text
提出 Schema 变更
  -> 判断是否兼容
  -> 补充默认值或兼容逻辑
  -> 更新 Schema 文件
  -> 本地生成代码
  -> 执行兼容性测试
  -> 评审并发布 Consumer
  -> 注册 Schema
  -> 发布 Producer
  -> 观察消费失败率和反序列化异常
```

Schema 演进要求如下：

1. Schema 文件必须与代码一起版本管理。
2. Schema 变更必须有评审记录。
3. 不兼容变更必须创建新 Topic、新 Subject 或新消息类型。
4. Producer 和 Consumer 发布顺序必须明确。
5. 生产环境 Schema 注册应由 CI/CD 或平台流程完成。
6. 反序列化异常必须接入监控和告警。
7. 历史消息保留期内，Consumer 必须兼容旧 Schema。

## 安全认证

本章节用于说明 Kafka 安全认证与访问控制设计，包括 PLAINTEXT 模式、SASL 认证、SSL 加密、SASL_SSL 配置、用户权限控制、ACL 权限设计、证书配置管理和安全配置验证。

Kafka 安全能力主要包括客户端与 Broker 之间的认证、数据传输加密、读写操作授权和可插拔授权服务。Apache Kafka 官方安全文档说明，Kafka 支持通过 SSL 或 SASL 对客户端、Broker 和工具连接进行认证，支持 SSL 加密传输，并支持对读写操作进行授权控制。([Apache Kafka](https://kafka.apache.org/42/security/security-overview/?utm_source=chatgpt.com))

### PLAINTEXT 模式

PLAINTEXT 是 Kafka 的非加密、非认证通信模式。它适合本地开发、单机测试和临时验证，不适合生产环境暴露使用。

本地开发配置示例：

```yaml
spring:
  kafka:
    # 本地 PLAINTEXT Kafka 地址
    bootstrap-servers: localhost:9092

    properties:
      # PLAINTEXT 表示不启用认证和加密
      security.protocol: PLAINTEXT
```

Docker Compose 本地 Kafka 常见监听配置示例：

```yaml
services:
  kafka:
    image: apache/kafka:4.2.0
    container_name: ateng-kafka
    ports:
      - "9092:9092"
    environment:
      # 本地开发使用 PLAINTEXT
      KAFKA_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
```

PLAINTEXT 使用要求如下：

1. 只允许用于本地、开发或隔离测试环境。
2. 不应在公网或跨网络生产环境中使用。
3. 不提供身份认证，任何能连通 Broker 的客户端都可能访问 Kafka。
4. 不提供传输加密，网络中间节点可能看到消息内容。
5. 生产环境应使用 SASL、SSL 或 SASL_SSL。

### SASL 认证

SASL 用于对 Kafka 客户端进行身份认证。Kafka 支持多种 SASL 机制，包括 GSSAPI、PLAIN、SCRAM-SHA-256、SCRAM-SHA-512 和 OAUTHBEARER。([Apache Kafka](https://kafka.apache.org/42/security/security-overview/?utm_source=chatgpt.com))

常见 SASL 机制如下：

| 机制            | 说明                             | 适用场景             |
| --------------- | -------------------------------- | -------------------- |
| `PLAIN`         | 用户名密码认证，通常必须配合 SSL | 简单内部系统         |
| `SCRAM-SHA-256` | 基于 SCRAM 的用户名密码认证      | 推荐通用安全场景     |
| `SCRAM-SHA-512` | 更强的 SCRAM 哈希机制            | 推荐生产环境         |
| `GSSAPI`        | Kerberos 认证                    | 大型企业统一认证     |
| `OAUTHBEARER`   | OAuth Token 认证                 | 云平台或统一身份体系 |

SASL_PLAINTEXT 配置示例：

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}

    properties:
      # SASL_PLAINTEXT 表示启用认证，但不启用传输加密
      security.protocol: SASL_PLAINTEXT

      # SASL 认证机制
      sasl.mechanism: SCRAM-SHA-512

      # 用户名和密码通过环境变量注入，禁止写死
      sasl.jaas.config: org.apache.kafka.common.security.scram.ScramLoginModule required username="${KAFKA_USERNAME}" password="${KAFKA_PASSWORD}";
```

SASL 使用要求如下：

1. 生产环境优先使用 `SASL_SSL`，不建议只使用 `SASL_PLAINTEXT`。
2. 用户名和密码必须通过环境变量、Secret 或配置中心注入。
3. 不允许在 Git 仓库中提交真实密码。
4. 不允许在日志中打印完整 JAAS 配置。
5. 每个应用应使用独立 Kafka 用户，便于权限隔离和审计。
6. 密码应定期轮换。

### SSL 加密

SSL 用于 Kafka 客户端和 Broker 之间的传输加密，也可以用于客户端证书认证。开启 SSL 后，网络传输中的消息内容会被加密，可以降低中间人窃听风险。Kafka 官方文档说明，Kafka 支持使用 SSL 对 Broker 与客户端、Broker 与 Broker、Broker 与工具之间传输的数据进行加密。([Apache Kafka](https://kafka.apache.org/42/security/security-overview/?utm_source=chatgpt.com))

SSL 常见配置项如下：

| 配置项                                  | 说明                                  |
| --------------------------------------- | ------------------------------------- |
| `security.protocol`                     | 设置为 `SSL` 或 `SASL_SSL`            |
| `ssl.truststore.location`               | TrustStore 文件路径                   |
| `ssl.truststore.password`               | TrustStore 密码                       |
| `ssl.truststore.type`                   | TrustStore 类型，例如 `JKS`、`PKCS12` |
| `ssl.keystore.location`                 | KeyStore 文件路径，双向认证时使用     |
| `ssl.keystore.password`                 | KeyStore 密码                         |
| `ssl.key.password`                      | 私钥密码                              |
| `ssl.endpoint.identification.algorithm` | 主机名校验算法，通常为 `https`        |

SSL 单独加密配置示例：

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}

    properties:
      # SSL 表示启用传输加密
      security.protocol: SSL

      # TrustStore 证书路径
      ssl.truststore.location: ${KAFKA_SSL_TRUSTSTORE_LOCATION}

      # TrustStore 密码
      ssl.truststore.password: ${KAFKA_SSL_TRUSTSTORE_PASSWORD}

      # TrustStore 类型
      ssl.truststore.type: JKS

      # 主机名校验，生产环境不建议关闭
      ssl.endpoint.identification.algorithm: https
```

SSL 使用要求如下：

1. TrustStore 和 KeyStore 文件不能提交到代码仓库。
2. 证书文件应通过 Secret、配置中心或挂载卷注入。
3. 生产环境不建议关闭主机名校验。
4. 证书过期时间必须纳入监控。
5. 证书轮换需要提前演练。
6. SSL 会增加一定 CPU 开销，需要结合压测评估。

### SASL_SSL 配置

SASL_SSL 是生产环境常见推荐方案，同时提供身份认证和传输加密。SASL 负责认证客户端身份，SSL 负责加密网络传输。

Spring Boot 配置示例：

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}

    properties:
      # SASL_SSL 同时启用认证和加密
      security.protocol: SASL_SSL

      # 推荐生产环境使用 SCRAM-SHA-512，具体以 Kafka 集群配置为准
      sasl.mechanism: SCRAM-SHA-512

      # JAAS 配置通过环境变量注入，禁止写死
      sasl.jaas.config: org.apache.kafka.common.security.scram.ScramLoginModule required username="${KAFKA_USERNAME}" password="${KAFKA_PASSWORD}";

      # TrustStore 证书路径
      ssl.truststore.location: ${KAFKA_SSL_TRUSTSTORE_LOCATION}

      # TrustStore 密码
      ssl.truststore.password: ${KAFKA_SSL_TRUSTSTORE_PASSWORD}

      # TrustStore 类型
      ssl.truststore.type: JKS

      # 主机名校验
      ssl.endpoint.identification.algorithm: https
```

Kubernetes Secret 示例：

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: kafka-security-secret
type: Opaque
stringData:
  # Kafka 用户名
  KAFKA_USERNAME: "your-username"

  # Kafka 密码
  KAFKA_PASSWORD: "your-password"

  # TrustStore 密码
  KAFKA_SSL_TRUSTSTORE_PASSWORD: "your-truststore-password"
```

Deployment 引用示例：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kafka-develop-demo
spec:
  replicas: 2
  selector:
    matchLabels:
      app: kafka-develop-demo
  template:
    metadata:
      labels:
        app: kafka-develop-demo
    spec:
      containers:
        - name: kafka-develop-demo
          image: kafka-develop-demo:1.0.0
          env:
            # Kafka Broker 地址
            - name: KAFKA_BOOTSTRAP_SERVERS
              value: "kafka-01:9093,kafka-02:9093,kafka-03:9093"

            # Kafka 用户名
            - name: KAFKA_USERNAME
              valueFrom:
                secretKeyRef:
                  name: kafka-security-secret
                  key: KAFKA_USERNAME

            # Kafka 密码
            - name: KAFKA_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: kafka-security-secret
                  key: KAFKA_PASSWORD

            # TrustStore 路径
            - name: KAFKA_SSL_TRUSTSTORE_LOCATION
              value: "/etc/kafka/secrets/kafka.client.truststore.jks"

            # TrustStore 密码
            - name: KAFKA_SSL_TRUSTSTORE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: kafka-security-secret
                  key: KAFKA_SSL_TRUSTSTORE_PASSWORD
          volumeMounts:
            - name: kafka-certs
              mountPath: /etc/kafka/secrets
              readOnly: true
      volumes:
        - name: kafka-certs
          secret:
            secretName: kafka-cert-secret
```

SASL_SSL 使用要求如下：

1. 认证信息和证书必须外部注入。
2. 不同应用使用不同 Kafka 用户。
3. 每个用户只分配所需 Topic 和 Group 权限。
4. 证书和密码轮换必须有流程。
5. 应用启动时不要打印明文认证信息。
6. 生产环境应配合 ACL 控制读写权限。

### 用户权限控制

用户权限控制用于限制不同应用对 Kafka 资源的访问范围。Kafka 用户不应共享，尤其是生产环境。每个业务应用应使用独立用户，并只授予该应用所需 Topic、Consumer Group 和事务资源权限。

用户规划示例：

| 应用         | Kafka 用户         | 权限范围                       |
| ------------ | ------------------ | ------------------------------ |
| 订单服务     | `order-service`    | 写订单 Topic，读订单相关 Topic |
| 支付服务     | `payment-service`  | 写支付 Topic，读支付相关 Topic |
| 通知服务     | `notify-service`   | 读通知相关 Topic               |
| 数据同步服务 | `sync-service`     | 读业务 Topic，写同步 Topic     |
| 运维工具     | `kafka-admin-tool` | 受控管理权限                   |

权限控制原则如下：

1. 一个应用一个 Kafka 用户。
2. 不同环境使用不同用户。
3. 生产用户不得用于本地调试。
4. Producer 只授予目标 Topic 写权限。
5. Consumer 只授予目标 Topic 读权限和对应 Group 权限。
6. 管理权限只授予平台或运维工具。
7. 权限变更必须有审批和审计记录。

### ACL 权限设计

ACL 用于控制 Kafka 用户对 Topic、Consumer Group、Cluster、TransactionalId 等资源的操作权限。Kafka 官方安全能力包含对客户端读写操作的授权控制。([Apache Kafka](https://kafka.apache.org/42/security/security-overview/?utm_source=chatgpt.com))

常见 ACL 操作如下：

| 操作                       | 说明                     |
| -------------------------- | ------------------------ |
| `Read`                     | 读取 Topic               |
| `Write`                    | 写入 Topic               |
| `Create`                   | 创建资源                 |
| `Delete`                   | 删除资源                 |
| `Alter`                    | 修改资源配置             |
| `Describe`                 | 查看资源信息             |
| `Read` on Group            | 使用 Consumer Group 消费 |
| `Write` on TransactionalId | 使用事务 ID              |

Producer 权限示例：

```bash
kafka-acls.sh \
  --bootstrap-server kafka-01:9093 \
  --command-config client-admin.properties \
  --add \
  --allow-principal User:order-service \
  --operation Write \
  --operation Describe \
  --topic ateng.order.created.topic
```

Consumer 权限示例：

```bash
kafka-acls.sh \
  --bootstrap-server kafka-01:9093 \
  --command-config client-admin.properties \
  --add \
  --allow-principal User:points-service \
  --operation Read \
  --operation Describe \
  --topic ateng.order.created.topic
```

Consumer Group 权限示例：

```bash
kafka-acls.sh \
  --bootstrap-server kafka-01:9093 \
  --command-config client-admin.properties \
  --add \
  --allow-principal User:points-service \
  --operation Read \
  --group ateng.points.consumer.group
```

事务权限示例：

```bash
kafka-acls.sh \
  --bootstrap-server kafka-01:9093 \
  --command-config client-admin.properties \
  --add \
  --allow-principal User:order-service \
  --operation Write \
  --transactional-id ateng-order-service-tx-
```

查看 ACL 示例：

```bash
kafka-acls.sh \
  --bootstrap-server kafka-01:9093 \
  --command-config client-admin.properties \
  --list \
  --principal User:order-service
```

ACL 设计建议如下：

1. Producer 至少需要目标 Topic 的 `Write` 和 `Describe` 权限。
2. Consumer 至少需要目标 Topic 的 `Read`、`Describe` 权限，以及 Consumer Group 的 `Read` 权限。
3. 自动创建 Topic 需要额外 `Create` 权限，生产环境不建议授予普通业务应用。
4. 事务 Producer 需要 TransactionalId 相关权限。
5. 不建议使用通配符授予所有 Topic 权限。
6. 删除、修改 Topic 配置等高危权限只应授予平台管理员。

### 证书配置管理

证书配置管理用于保证 SSL 证书和密钥安全存储、分发、更新和审计。Kafka SSL 通常涉及 CA 证书、TrustStore、KeyStore、证书密码和私钥密码。

证书文件规划如下：

| 文件                          | 说明                         |
| ----------------------------- | ---------------------------- |
| `ca.crt`                      | CA 根证书                    |
| `kafka.client.truststore.jks` | 客户端信任库                 |
| `kafka.client.keystore.jks`   | 客户端密钥库，双向认证时使用 |
| `client.properties`           | Kafka 命令行工具客户端配置   |
| `jaas.conf`                   | JAAS 认证配置，部分场景使用  |

证书管理要求如下：

1. 证书文件不得提交到 Git。
2. 证书通过 Kubernetes Secret、配置中心、密钥系统或挂载卷分发。
3. 证书密码通过环境变量或 Secret 注入。
4. 证书过期时间必须纳入监控。
5. 证书轮换前必须验证新旧证书兼容期。
6. 证书路径应固定，便于部署脚本和应用配置引用。
7. 不同环境使用不同证书，避免开发环境证书访问生产 Kafka。

本地 `client.properties` 示例：

```properties
# Kafka 安全协议
security.protocol=SASL_SSL

# SASL 机制
sasl.mechanism=SCRAM-SHA-512

# SASL 用户名密码
sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required username="your-username" password="your-password";

# TrustStore 路径
ssl.truststore.location=/etc/kafka/secrets/kafka.client.truststore.jks

# TrustStore 密码
ssl.truststore.password=your-truststore-password

# TrustStore 类型
ssl.truststore.type=JKS

# 主机名校验
ssl.endpoint.identification.algorithm=https
```

生产环境中，该文件不应包含真实密码。命令行工具可使用临时受控文件，并在使用后清理。

### 安全配置验证

安全配置验证用于确认 Kafka 客户端能否正常认证、加密通信、读写 Topic 和使用 Consumer Group。验证应覆盖连接、认证、授权、生产、消费和 ACL 限制。

验证连接和认证：

```bash
kafka-topics.sh \
  --bootstrap-server kafka-01:9093 \
  --command-config client.properties \
  --list
```

验证 Topic 描述权限：

```bash
kafka-topics.sh \
  --bootstrap-server kafka-01:9093 \
  --command-config client.properties \
  --describe \
  --topic ateng.order.created.topic
```

验证 Producer 写权限：

```bash
kafka-console-producer.sh \
  --bootstrap-server kafka-01:9093 \
  --producer.config client.properties \
  --topic ateng.order.created.topic
```

验证 Consumer 读权限：

```bash
kafka-console-consumer.sh \
  --bootstrap-server kafka-01:9093 \
  --consumer.config client.properties \
  --topic ateng.order.created.topic \
  --group ateng.security.verify.group \
  --from-beginning \
  --timeout-ms 10000
```

验证 Consumer Group：

```bash
kafka-consumer-groups.sh \
  --bootstrap-server kafka-01:9093 \
  --command-config client.properties \
  --group ateng.security.verify.group \
  --describe
```

安全验证清单如下：

| 验证项        | 预期结果                                          |
| ------------- | ------------------------------------------------- |
| Broker 连接   | 能成功列出或描述 Topic                            |
| SASL 认证     | 用户名密码正确时通过，错误时失败                  |
| SSL 证书      | TrustStore 正确时连接成功，证书错误时失败         |
| Producer 权限 | 有 Write 权限的 Topic 可写，无权限 Topic 写入失败 |
| Consumer 权限 | 有 Read 权限的 Topic 可读，无权限 Topic 读取失败  |
| Group 权限    | 有 Group Read 权限时可消费                        |
| ACL 限制      | 未授权操作应被拒绝                                |
| 日志安全      | 应用日志不输出密码、证书内容和完整 JAAS           |

常见错误排查如下：

| 错误现象           | 可能原因                                                  |
| ------------------ | --------------------------------------------------------- |
| 认证失败           | 用户名、密码、SASL 机制错误                               |
| SSL 握手失败       | TrustStore 路径、密码、证书链或主机名校验错误             |
| Topic 无法写入     | 缺少 Topic `Write` 或 `Describe` 权限                     |
| 无法消费           | 缺少 Topic `Read` 或 Group `Read` 权限                    |
| 连接超时           | Broker 地址、网络、防火墙或 advertised.listeners 配置错误 |
| 本地可连生产不可连 | 网络策略、安全组、证书或 DNS 配置差异                     |

安全配置最终建议是：本地开发可以使用 PLAINTEXT；测试环境至少启用 SASL；生产环境优先使用 SASL_SSL，并配合 ACL、Secret 管理、证书轮换、权限最小化和安全验证流程。


## 多 Kafka 集群

本章节用于说明一个 Spring Boot 项目同时接入多个 Kafka 集群的设计方式，包括多集群使用场景、多 Producer 配置、多 Consumer 配置、多 `KafkaTemplate` 配置、多 `ListenerContainerFactory` 配置、集群隔离设计和配置命名规范。

多 Kafka 集群接入通常用于业务隔离、环境隔离、数据同步、跨区域消息流转或历史系统迁移。多集群场景下，不能继续只依赖 Spring Boot 默认的 `spring.kafka.*` 单集群自动配置，而应为不同集群显式定义独立的连接配置、ProducerFactory、ConsumerFactory、KafkaTemplate 和监听容器工厂。

### 多集群使用场景

多集群使用场景用于判断项目是否真的需要同时连接多个 Kafka 集群。普通业务系统通常只需要连接一个 Kafka 集群，只有在明确存在隔离、迁移、同步或多环境并行需求时，才建议引入多集群配置。

常见多集群场景如下：

| 场景           | 说明                                                 |
| -------------- | ---------------------------------------------------- |
| 业务集群隔离   | 核心交易消息和日志采集消息分别写入不同 Kafka 集群    |
| 数据同步       | 从业务 Kafka 集群消费消息，再写入数据平台 Kafka 集群 |
| 跨区域部署     | 不同地域使用独立 Kafka 集群，应用需要按区域路由      |
| 历史系统迁移   | 新旧 Kafka 集群并行运行，应用同时写入或消费          |
| 内外部系统隔离 | 内部业务消息和对外开放消息使用不同集群               |
| 安全等级隔离   | 敏感业务消息和普通业务消息分别使用不同认证和权限策略 |
| 灾备切换       | 主集群异常时，应用具备切换到备用集群的能力           |

不建议使用多集群的场景：

| 场景                     | 原因                                     |
| ------------------------ | ---------------------------------------- |
| 只是 Topic 较多          | Topic 多不代表需要多个 Kafka 集群        |
| 只是 Consumer Group 较多 | 消费组属于同一集群内的逻辑隔离           |
| 为了临时测试             | 临时测试应使用独立环境或测试 Topic       |
| 没有明确隔离需求         | 多集群会增加配置、运维、监控和排查复杂度 |

多集群接入前需要明确以下问题：

1. 每个集群的用途是什么。
2. 哪些 Topic 属于哪个集群。
3. Producer 和 Consumer 是否都需要多集群。
4. 多集群之间是否需要消息同步。
5. 是否存在跨集群事务或一致性要求。
6. 每个集群的认证、权限和监控是否独立。
7. 故障时是否需要自动切换或人工切换。

### 多 Producer 配置

多 Producer 配置用于让应用向不同 Kafka 集群发送消息。每个集群应有独立的 Producer 配置，不能共用同一个 `ProducerFactory`。

配置文件示例：

文件位置：`src/main/resources/application.yml`

该配置用于定义两个 Kafka 集群：业务集群 `biz` 和数据集群 `data`。

```yaml
app:
  kafka:
    clusters:
      biz:
        bootstrap-servers: ${KAFKA_BIZ_BOOTSTRAP_SERVERS:localhost:9092}
        producer:
          acks: all
          retries: 3
          key-serializer: org.apache.kafka.common.serialization.StringSerializer
          value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
          properties:
            enable.idempotence: true
            max.in.flight.requests.per.connection: 5
            compression.type: lz4
            spring.json.add.type.headers: false

      data:
        bootstrap-servers: ${KAFKA_DATA_BOOTSTRAP_SERVERS:localhost:19092}
        producer:
          acks: all
          retries: 3
          key-serializer: org.apache.kafka.common.serialization.StringSerializer
          value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
          properties:
            enable.idempotence: true
            max.in.flight.requests.per.connection: 5
            compression.type: lz4
            spring.json.add.type.headers: false
```

配置属性类如下：

文件位置：`src/main/java/io/github/atengk/kafka/config/MultiKafkaProperties.java`

该配置类用于承接多 Kafka 集群配置。

```java
package io.github.atengk.kafka.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 多 Kafka 集群配置属性
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@ConfigurationProperties(prefix = "app.kafka")
public class MultiKafkaProperties {

    /**
     * Kafka 集群配置
     */
    private Map<String, ClusterProperties> clusters = new HashMap<>();

    /**
     * 单个 Kafka 集群配置
     *
     * @author Ateng
     * @since 2026-05-11
     */
    @Data
    public static class ClusterProperties {

        /**
         * Kafka Broker 地址
         */
        private String bootstrapServers;

        /**
         * Producer 配置
         */
        private ClientProperties producer = new ClientProperties();

        /**
         * Consumer 配置
         */
        private ClientProperties consumer = new ClientProperties();

    }

    /**
     * Kafka 客户端配置
     *
     * @author Ateng
     * @since 2026-05-11
     */
    @Data
    public static class ClientProperties {

        /**
         * Key 序列化器或反序列化器
         */
        private String keySerializer;

        /**
         * Value 序列化器或反序列化器
         */
        private String valueSerializer;

        /**
         * Key 反序列化器
         */
        private String keyDeserializer;

        /**
         * Value 反序列化器
         */
        private String valueDeserializer;

        /**
         * ACK 配置
         */
        private String acks = "all";

        /**
         * 重试次数
         */
        private Integer retries = 3;

        /**
         * Consumer Group
         */
        private String groupId;

        /**
         * 是否自动提交 Offset
         */
        private Boolean enableAutoCommit = false;

        /**
         * Offset 重置策略
         */
        private String autoOffsetReset = "earliest";

        /**
         * 扩展配置
         */
        private Map<String, Object> properties = new HashMap<>();

    }

}
```

多 Producer 配置类如下：

文件位置：`src/main/java/io/github/atengk/kafka/config/MultiKafkaProducerConfig.java`

该配置类用于为不同 Kafka 集群创建独立的 `ProducerFactory` 和 `KafkaTemplate`。

```java
package io.github.atengk.kafka.config;

import cn.hutool.core.map.MapUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 多 Kafka 集群 Producer 配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(MultiKafkaProperties.class)
public class MultiKafkaProducerConfig {

    private final MultiKafkaProperties multiKafkaProperties;

    /**
     * 业务集群 ProducerFactory
     *
     * @return ProducerFactory
     */
    @Bean
    public ProducerFactory<String, Object> bizProducerFactory() {
        return createProducerFactory("biz");
    }

    /**
     * 业务集群 KafkaTemplate
     *
     * @return KafkaTemplate
     */
    @Bean
    public KafkaTemplate<String, Object> bizKafkaTemplate() {
        return new KafkaTemplate<>(bizProducerFactory());
    }

    /**
     * 数据集群 ProducerFactory
     *
     * @return ProducerFactory
     */
    @Bean
    public ProducerFactory<String, Object> dataProducerFactory() {
        return createProducerFactory("data");
    }

    /**
     * 数据集群 KafkaTemplate
     *
     * @return KafkaTemplate
     */
    @Bean
    public KafkaTemplate<String, Object> dataKafkaTemplate() {
        return new KafkaTemplate<>(dataProducerFactory());
    }

    /**
     * 创建 ProducerFactory
     *
     * @param clusterName 集群名称
     * @return ProducerFactory
     */
    private ProducerFactory<String, Object> createProducerFactory(String clusterName) {
        MultiKafkaProperties.ClusterProperties cluster = multiKafkaProperties.getClusters().get(clusterName);
        if (cluster == null) {
            throw new IllegalStateException("Kafka集群配置不存在：" + clusterName);
        }

        MultiKafkaProperties.ClientProperties producer = cluster.getProducer();
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, cluster.getBootstrapServers());
        config.put(ProducerConfig.ACKS_CONFIG, producer.getAcks());
        config.put(ProducerConfig.RETRIES_CONFIG, producer.getRetries());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, producer.getKeySerializer());
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, producer.getValueSerializer());

        if (MapUtil.isNotEmpty(producer.getProperties())) {
            config.putAll(producer.getProperties());
        }

        log.info("初始化Kafka ProducerFactory，cluster={}，bootstrapServers={}",
                clusterName, cluster.getBootstrapServers());
        return new DefaultKafkaProducerFactory<>(config);
    }

}
```

使用方式如下：

```java
package io.github.atengk.kafka.producer;

import io.github.atengk.kafka.constant.KafkaTopicConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 多集群 Kafka 消息生产者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MultiClusterMessageProducer {

    @Qualifier("bizKafkaTemplate")
    private final KafkaTemplate<String, Object> bizKafkaTemplate;

    @Qualifier("dataKafkaTemplate")
    private final KafkaTemplate<String, Object> dataKafkaTemplate;

    /**
     * 发送业务集群消息
     *
     * @param key     消息Key
     * @param message 消息内容
     */
    public void sendBizMessage(String key, Object message) {
        bizKafkaTemplate.send(KafkaTopicConstant.ORDER_CREATED_TOPIC, key, message);
        log.info("发送业务Kafka集群消息，topic={}，key={}", KafkaTopicConstant.ORDER_CREATED_TOPIC, key);
    }

    /**
     * 发送数据集群消息
     *
     * @param key     消息Key
     * @param message 消息内容
     */
    public void sendDataMessage(String key, Object message) {
        dataKafkaTemplate.send(KafkaTopicConstant.DATA_SYNC_TOPIC, key, message);
        log.info("发送数据Kafka集群消息，topic={}，key={}", KafkaTopicConstant.DATA_SYNC_TOPIC, key);
    }

}
```

多 Producer 配置要求如下：

1. 每个集群独立配置 `bootstrap.servers`。
2. 每个集群独立创建 `ProducerFactory`。
3. 每个集群独立创建 `KafkaTemplate`。
4. 业务代码注入 `KafkaTemplate` 时必须使用 `@Qualifier`。
5. 不同集群的 Topic 常量应清晰区分。
6. 不同集群的安全认证配置应独立维护。

### 多 Consumer 配置

多 Consumer 配置用于让应用从不同 Kafka 集群消费消息。每个集群应有独立的 `ConsumerFactory` 和监听容器工厂。

配置文件示例：

```yaml
app:
  kafka:
    clusters:
      biz:
        bootstrap-servers: ${KAFKA_BIZ_BOOTSTRAP_SERVERS:localhost:9092}
        consumer:
          group-id: ateng-biz-consumer-group
          enable-auto-commit: false
          auto-offset-reset: earliest
          key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
          value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
          properties:
            spring.deserializer.value.delegate.class: org.springframework.kafka.support.serializer.JsonDeserializer
            spring.json.trusted.packages: io.github.atengk.kafka.model,java.util,java.lang
            spring.json.value.default.type: io.github.atengk.kafka.model.MessageEnvelope

      data:
        bootstrap-servers: ${KAFKA_DATA_BOOTSTRAP_SERVERS:localhost:19092}
        consumer:
          group-id: ateng-data-consumer-group
          enable-auto-commit: false
          auto-offset-reset: earliest
          key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
          value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
          properties:
            spring.deserializer.value.delegate.class: org.springframework.kafka.support.serializer.JsonDeserializer
            spring.json.trusted.packages: io.github.atengk.kafka.model,java.util,java.lang
            spring.json.value.default.type: io.github.atengk.kafka.model.MessageEnvelope
```

多 Consumer 配置类如下：

文件位置：`src/main/java/io/github/atengk/kafka/config/MultiKafkaConsumerConfig.java`

该配置类用于为不同 Kafka 集群创建独立的 `ConsumerFactory`。

```java
package io.github.atengk.kafka.config;

import cn.hutool.core.map.MapUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 多 Kafka 集群 Consumer 配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MultiKafkaConsumerConfig {

    private final MultiKafkaProperties multiKafkaProperties;

    /**
     * 业务集群 ConsumerFactory
     *
     * @return ConsumerFactory
     */
    @Bean
    public ConsumerFactory<String, Object> bizConsumerFactory() {
        return createConsumerFactory("biz");
    }

    /**
     * 数据集群 ConsumerFactory
     *
     * @return ConsumerFactory
     */
    @Bean
    public ConsumerFactory<String, Object> dataConsumerFactory() {
        return createConsumerFactory("data");
    }

    /**
     * 创建 ConsumerFactory
     *
     * @param clusterName 集群名称
     * @return ConsumerFactory
     */
    private ConsumerFactory<String, Object> createConsumerFactory(String clusterName) {
        MultiKafkaProperties.ClusterProperties cluster = multiKafkaProperties.getClusters().get(clusterName);
        if (cluster == null) {
            throw new IllegalStateException("Kafka集群配置不存在：" + clusterName);
        }

        MultiKafkaProperties.ClientProperties consumer = cluster.getConsumer();
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, cluster.getBootstrapServers());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, consumer.getGroupId());
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, consumer.getEnableAutoCommit());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumer.getAutoOffsetReset());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, consumer.getKeyDeserializer());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, consumer.getValueDeserializer());

        if (MapUtil.isNotEmpty(consumer.getProperties())) {
            config.putAll(consumer.getProperties());
        }

        log.info("初始化Kafka ConsumerFactory，cluster={}，bootstrapServers={}，groupId={}",
                clusterName, cluster.getBootstrapServers(), consumer.getGroupId());
        return new DefaultKafkaConsumerFactory<>(config);
    }

}
```

多 Consumer 配置要求如下：

1. 每个集群独立配置 Consumer Group。
2. 不同集群的 Consumer Group 命名必须包含集群或业务语义。
3. 不同集群使用独立的反序列化配置。
4. 不同集群应使用独立监听容器工厂。
5. 多集群消费日志必须打印集群名称，便于排查问题。
6. 多集群消费指标应按集群维度区分。

### 多 KafkaTemplate 配置

多 `KafkaTemplate` 配置用于在同一个应用中明确区分不同 Kafka 集群的发送入口。每个 `KafkaTemplate` 应有清晰的 Bean 名称。

推荐 Bean 命名如下：

| 集群     | ProducerFactory Bean      | KafkaTemplate Bean      |
| -------- | ------------------------- | ----------------------- |
| 业务集群 | `bizProducerFactory`      | `bizKafkaTemplate`      |
| 数据集群 | `dataProducerFactory`     | `dataKafkaTemplate`     |
| 日志集群 | `logProducerFactory`      | `logKafkaTemplate`      |
| 外部集群 | `externalProducerFactory` | `externalKafkaTemplate` |

使用示例：

```java
@Qualifier("bizKafkaTemplate")
private final KafkaTemplate<String, Object> bizKafkaTemplate;

@Qualifier("dataKafkaTemplate")
private final KafkaTemplate<String, Object> dataKafkaTemplate;
```

如果项目中存在默认 `KafkaTemplate`，需要明确哪个集群作为主集群。可以使用 `@Primary` 标记主集群模板，但不建议在多集群项目中过度依赖默认注入。

示例：

```java
@Bean
@Primary
public KafkaTemplate<String, Object> bizKafkaTemplate() {
    return new KafkaTemplate<>(bizProducerFactory());
}
```

多 `KafkaTemplate` 使用要求如下：

1. 业务代码注入时必须使用 `@Qualifier`。
2. Bean 名称必须体现集群语义。
3. 不同集群的发送日志必须打印集群名称。
4. 不要把多个集群的发送逻辑隐藏在同一个模糊方法中。
5. 如果封装统一 Producer，应在请求参数中明确 `clusterName`。

统一多集群发送请求示例：

```java
package io.github.atengk.kafka.producer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 多集群 Kafka 发送请求
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiClusterKafkaSendRequest {

    /**
     * 集群名称
     */
    private String clusterName;

    /**
     * Topic 名称
     */
    private String topic;

    /**
     * 消息 Key
     */
    private String key;

    /**
     * 消息内容
     */
    private Object payload;

}
```

### 多 ListenerContainerFactory 配置

多 `ListenerContainerFactory` 配置用于区分不同 Kafka 集群的监听容器。每个集群的监听器应指定对应的 `containerFactory`，避免监听器连接到错误集群。

配置类示例：

文件位置：`src/main/java/io/github/atengk/kafka/config/MultiKafkaListenerContainerConfig.java`

该配置类用于为业务集群和数据集群分别创建监听容器工厂。

```java
package io.github.atengk.kafka.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

/**
 * 多 Kafka 集群监听容器配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Configuration
public class MultiKafkaListenerContainerConfig {

    /**
     * 业务集群监听容器工厂
     *
     * @param consumerFactory 业务集群消费者工厂
     * @return 监听容器工厂
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> bizKafkaListenerContainerFactory(
            @Qualifier("bizConsumerFactory") ConsumerFactory<String, Object> consumerFactory) {
        return createFactory("biz", consumerFactory);
    }

    /**
     * 数据集群监听容器工厂
     *
     * @param consumerFactory 数据集群消费者工厂
     * @return 监听容器工厂
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> dataKafkaListenerContainerFactory(
            @Qualifier("dataConsumerFactory") ConsumerFactory<String, Object> consumerFactory) {
        return createFactory("data", consumerFactory);
    }

    /**
     * 创建监听容器工厂
     *
     * @param clusterName     集群名称
     * @param consumerFactory 消费者工厂
     * @return 监听容器工厂
     */
    private ConcurrentKafkaListenerContainerFactory<String, Object> createFactory(
            String clusterName,
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        log.info("初始化Kafka监听容器工厂，cluster={}，ackMode=MANUAL_IMMEDIATE，concurrency=3", clusterName);
        return factory;
    }

}
```

监听器使用示例：

```java
package io.github.atengk.kafka.consumer;

import io.github.atengk.kafka.constant.KafkaGroupConstant;
import io.github.atengk.kafka.constant.KafkaTopicConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 多集群 Kafka 消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class MultiClusterConsumer {

    /**
     * 消费业务集群消息
     *
     * @param record         Kafka 消息记录
     * @param acknowledgment Offset确认对象
     */
    @KafkaListener(
            topics = KafkaTopicConstant.ORDER_CREATED_TOPIC,
            groupId = KafkaGroupConstant.BIZ_ORDER_CONSUMER_GROUP,
            containerFactory = "bizKafkaListenerContainerFactory"
    )
    public void consumeBizMessage(ConsumerRecord<String, Object> record,
                                  Acknowledgment acknowledgment) {
        log.info("消费业务集群Kafka消息，topic={}，partition={}，offset={}，key={}",
                record.topic(), record.partition(), record.offset(), record.key());
        acknowledgment.acknowledge();
    }

    /**
     * 消费数据集群消息
     *
     * @param record         Kafka 消息记录
     * @param acknowledgment Offset确认对象
     */
    @KafkaListener(
            topics = KafkaTopicConstant.DATA_SYNC_TOPIC,
            groupId = KafkaGroupConstant.DATA_SYNC_CONSUMER_GROUP,
            containerFactory = "dataKafkaListenerContainerFactory"
    )
    public void consumeDataMessage(ConsumerRecord<String, Object> record,
                                   Acknowledgment acknowledgment) {
        log.info("消费数据集群Kafka消息，topic={}，partition={}，offset={}，key={}",
                record.topic(), record.partition(), record.offset(), record.key());
        acknowledgment.acknowledge();
    }

}
```

多监听容器要求如下：

1. 每个监听器必须显式指定 `containerFactory`。
2. 不同集群监听器不要共用同一个 ConsumerFactory。
3. 监听日志中应打印集群语义。
4. 多集群监听器应独立配置并发、错误处理器和死信策略。
5. 如果不同集群认证方式不同，必须分别配置安全参数。

### 集群隔离设计

集群隔离设计用于避免多 Kafka 集群之间的配置、Topic、Consumer Group、权限和监控混淆。多集群项目中最常见的问题是：消息发错集群、Consumer 连错集群、Topic 命名冲突、Group 命名冲突和权限配置混乱。

隔离维度如下：

| 隔离维度   | 说明                                                         |
| ---------- | ------------------------------------------------------------ |
| 配置隔离   | 每个集群独立配置 `bootstrap-servers`、认证和客户端参数       |
| Bean 隔离  | 每个集群独立定义 ProducerFactory、ConsumerFactory、KafkaTemplate |
| Topic 隔离 | Topic 命名体现业务和集群用途                                 |
| Group 隔离 | Consumer Group 名称体现集群和业务语义                        |
| 权限隔离   | 每个集群使用独立 Kafka 用户和 ACL                            |
| 日志隔离   | 日志打印集群名称                                             |
| 指标隔离   | 监控指标带上 cluster 标签                                    |
| 死信隔离   | 不同集群使用不同死信 Topic 或死信表字段                      |

集群隔离建议如下：

1. 业务代码发送消息时必须明确目标集群。
2. 禁止使用模糊 Bean 名称，例如 `kafkaTemplate1`、`kafkaTemplate2`。
3. 配置文件中集群名称必须语义明确，例如 `biz`、`data`、`log`、`external`。
4. 消息发送记录表应增加 `cluster_name` 字段。
5. 消费记录表应增加 `cluster_name` 字段。
6. 死信消息表应增加 `cluster_name` 字段。
7. 告警规则应区分集群，避免误判故障范围。

发送记录表扩展示例：

```sql
ALTER TABLE kafka_outbox_message
    ADD COLUMN cluster_name VARCHAR(64) NOT NULL DEFAULT 'biz' COMMENT 'Kafka集群名称';

CREATE INDEX idx_cluster_topic ON kafka_outbox_message (cluster_name, topic);
```

消费记录表扩展示例：

```sql
ALTER TABLE kafka_consume_record
    ADD COLUMN cluster_name VARCHAR(64) NOT NULL DEFAULT 'biz' COMMENT 'Kafka集群名称';

CREATE INDEX idx_cluster_topic_offset ON kafka_consume_record (cluster_name, topic, partition_id, offset_value);
```

### 配置命名规范

配置命名规范用于统一多 Kafka 集群配置项、Bean 名称、Topic 常量、Consumer Group 常量和环境变量名称。命名不规范会导致后期维护和排查成本明显增加。

推荐配置命名如下：

| 类型                     | 命名示例                           |
| ------------------------ | ---------------------------------- |
| 配置前缀                 | `app.kafka.clusters.biz`           |
| Broker 环境变量          | `KAFKA_BIZ_BOOTSTRAP_SERVERS`      |
| 用户名环境变量           | `KAFKA_BIZ_USERNAME`               |
| 密码环境变量             | `KAFKA_BIZ_PASSWORD`               |
| ProducerFactory          | `bizProducerFactory`               |
| ConsumerFactory          | `bizConsumerFactory`               |
| KafkaTemplate            | `bizKafkaTemplate`                 |
| ListenerContainerFactory | `bizKafkaListenerContainerFactory` |

集群名称建议如下：

| 集群名     | 含义                   |
| ---------- | ---------------------- |
| `biz`      | 业务消息集群           |
| `data`     | 数据同步或数据平台集群 |
| `log`      | 日志采集集群           |
| `external` | 外部系统对接集群       |
| `backup`   | 备用集群               |
| `archive`  | 归档集群               |

不推荐命名如下：

| 不推荐命名 | 问题                   |
| ---------- | ---------------------- |
| `kafka1`   | 无业务含义             |
| `kafka2`   | 无法判断用途           |
| `new`      | 后续会变成历史命名     |
| `old`      | 迁移后语义不清晰       |
| `test`     | 容易与环境名混淆       |
| `default`  | 多集群场景下含义不明确 |

环境变量命名示例：

```text
KAFKA_BIZ_BOOTSTRAP_SERVERS
KAFKA_BIZ_USERNAME
KAFKA_BIZ_PASSWORD
KAFKA_DATA_BOOTSTRAP_SERVERS
KAFKA_DATA_USERNAME
KAFKA_DATA_PASSWORD
```

多集群命名要求如下：

1. 集群名必须反映用途。
2. Bean 名称必须包含集群名。
3. 环境变量必须包含集群名。
4. Topic 和 Group 常量应按集群或业务域归类。
5. 不同集群的安全配置不能混用。
6. 文档、监控、告警和日志中的集群名称必须一致。

## 业务接口设计

本章节用于说明 Kafka 项目中常见业务接口的设计方式，包括消息发送接口、消息查询接口、消费状态接口、死信消息重放接口、Topic 管理接口、健康检查接口和管理接口权限控制。

业务接口不是 Kafka 的必需能力，但在企业项目中通常需要提供管理、排查和验证能力。例如开发人员需要通过接口发送测试消息，运维人员需要查询消息发送状态，业务人员需要查看死信消息并触发重放。接口设计应以安全、可追踪、可审计、可限制为前提，不应直接开放高危 Kafka 操作。

### 消息发送接口

消息发送接口用于业务系统或管理后台触发 Kafka 消息发送。该接口适合测试联调、业务补偿、管理后台手动触发等场景。生产环境中，消息发送接口必须有权限控制、参数校验、日志审计和限流保护。

接口设计如下：

| 项目     | 说明                                        |
| -------- | ------------------------------------------- |
| 接口路径 | `POST /api/kafka/messages/send`             |
| 接口用途 | 发送 Kafka 消息                             |
| 请求方式 | `POST`                                      |
| 权限要求 | 管理员或具备消息发送权限的角色              |
| 审计要求 | 记录操作人、Topic、Key、messageId、发送结果 |

请求参数示例：

```json
{
  "clusterName": "biz",
  "topic": "ateng.order.created.topic",
  "key": "ORDER:10001",
  "messageType": "ORDER_CREATED",
  "payload": {
    "orderId": "10001",
    "userId": "20001",
    "amount": 199.90
  }
}
```

响应示例：

```json
{
  "success": true,
  "message": "发送成功",
  "data": {
    "messageId": "1900000000000000001",
    "clusterName": "biz",
    "topic": "ateng.order.created.topic",
    "partition": 0,
    "offset": 1024,
    "key": "ORDER:10001"
  }
}
```

请求 DTO 示例：

文件位置：`src/main/java/io/github/atengk/kafka/controller/dto/KafkaMessageSendRequest.java`

该请求对象用于承接消息发送接口参数。

```java
package io.github.atengk.kafka.controller.dto;

import lombok.Data;

import java.util.Map;

/**
 * Kafka 消息发送请求
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
public class KafkaMessageSendRequest {

    /**
     * Kafka 集群名称
     */
    private String clusterName;

    /**
     * Topic 名称
     */
    private String topic;

    /**
     * 消息 Key
     */
    private String key;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 消息内容
     */
    private Map<String, Object> payload;

}
```

Controller 示例：

文件位置：`src/main/java/io/github/atengk/kafka/controller/KafkaMessageController.java`

该 Controller 用于提供 Kafka 消息发送接口，接口内部调用统一 Producer 组件完成消息发送。

```java
package io.github.atengk.kafka.controller;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.kafka.controller.dto.KafkaMessageSendRequest;
import io.github.atengk.kafka.producer.KafkaSendResult;
import io.github.atengk.kafka.service.KafkaMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Kafka 消息接口
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/kafka/messages")
public class KafkaMessageController {

    private final KafkaMessageService kafkaMessageService;

    /**
     * 发送 Kafka 消息
     *
     * @param request 发送请求
     * @return 发送结果
     */
    @PostMapping("/send")
    public KafkaSendResult sendMessage(@RequestBody KafkaMessageSendRequest request) {
        if (request == null || StrUtil.isBlank(request.getTopic())) {
            throw new IllegalArgumentException("Kafka Topic不能为空");
        }

        log.info("接收Kafka消息发送请求，clusterName={}，topic={}，key={}，messageType={}",
                request.getClusterName(), request.getTopic(), request.getKey(), request.getMessageType());

        return kafkaMessageService.sendMessage(request);
    }

}
```

消息发送接口要求如下：

1. 生产环境必须限制可发送 Topic 白名单。
2. 不允许任意用户向任意 Topic 发送消息。
3. 请求体大小必须限制，避免大消息冲击 Kafka。
4. 必须记录操作审计日志。
5. 发送失败必须返回明确错误信息。
6. 不允许通过接口发送包含密码、Token、密钥等敏感字段的消息。

### 消息查询接口

消息查询接口用于查询消息发送记录、业务消息记录或死信消息记录。Kafka 本身不是按业务字段查询消息的数据库，因此业务查询通常依赖消息发送记录表、消费记录表、死信消息表或审计表。

接口设计如下：

| 项目     | 说明                                                         |
| -------- | ------------------------------------------------------------ |
| 接口路径 | `GET /api/kafka/messages`                                    |
| 接口用途 | 查询消息发送记录                                             |
| 请求方式 | `GET`                                                        |
| 查询条件 | `messageId`、`topic`、`key`、`status`、`startTime`、`endTime` |
| 权限要求 | 只读管理权限                                                 |

查询参数示例：

```text
GET /api/kafka/messages?messageId=1900000000000000001&topic=ateng.order.created.topic
```

响应示例：

```json
{
  "success": true,
  "data": {
    "messageId": "1900000000000000001",
    "clusterName": "biz",
    "topic": "ateng.order.created.topic",
    "key": "ORDER:10001",
    "partition": 0,
    "offset": 1024,
    "sendStatus": "SUCCESS",
    "createTime": "2026-05-11T10:30:00"
  }
}
```

查询接口建议支持以下条件：

| 条件                    | 说明                           |
| ----------------------- | ------------------------------ |
| `messageId`             | 精确查询单条消息               |
| `topic`                 | 按 Topic 查询                  |
| `key`                   | 按业务 Key 查询                |
| `clusterName`           | 多集群场景下区分集群           |
| `sendStatus`            | 查询发送成功、失败、待重试记录 |
| `startTime` / `endTime` | 按时间范围查询                 |
| `pageNum` / `pageSize`  | 分页查询                       |

消息查询接口要求如下：

1. 查询必须走业务记录表，不建议直接扫描 Kafka Topic。
2. 查询结果不能返回完整敏感消息体。
3. 大字段应按需查看，列表页只返回摘要。
4. 分页参数必须限制最大页大小。
5. 查询操作应记录审计日志，尤其是查看敏感业务消息时。

### 消费状态接口

消费状态接口用于查询某条消息在不同 Consumer Group 中的处理情况。它通常依赖消费记录表，例如 `kafka_consume_record`。

接口设计如下：

| 项目     | 说明                                            |
| -------- | ----------------------------------------------- |
| 接口路径 | `GET /api/kafka/consume-records`                |
| 接口用途 | 查询消费状态                                    |
| 请求方式 | `GET`                                           |
| 查询条件 | `messageId`、`consumerGroup`、`topic`、`status` |
| 权限要求 | 只读管理权限                                    |

查询参数示例：

```text
GET /api/kafka/consume-records?messageId=1900000000000000001
```

响应示例：

```json
{
  "success": true,
  "data": [
    {
      "messageId": "1900000000000000001",
      "clusterName": "biz",
      "topic": "ateng.order.created.topic",
      "consumerGroup": "ateng.points.consumer.group",
      "partition": 0,
      "offset": 1024,
      "consumeStatus": "SUCCESS",
      "retryCount": 0,
      "consumeTime": "2026-05-11T10:30:03"
    },
    {
      "messageId": "1900000000000000001",
      "clusterName": "biz",
      "topic": "ateng.order.created.topic",
      "consumerGroup": "ateng.statistics.consumer.group",
      "partition": 0,
      "offset": 1024,
      "consumeStatus": "FAILED",
      "retryCount": 3,
      "errorMessage": "下游服务超时",
      "consumeTime": "2026-05-11T10:30:05"
    }
  ]
}
```

消费状态字段建议如下：

| 字段            | 说明           |
| --------------- | -------------- |
| `messageId`     | 消息唯一标识   |
| `clusterName`   | Kafka 集群名称 |
| `topic`         | Topic 名称     |
| `consumerGroup` | 消费组         |
| `partition`     | 分区           |
| `offset`        | Offset         |
| `consumeStatus` | 消费状态       |
| `retryCount`    | 重试次数       |
| `errorMessage`  | 异常摘要       |
| `createTime`    | 首次消费时间   |
| `updateTime`    | 最近更新时间   |

消费状态接口要求如下：

1. 必须支持按 `messageId` 查询完整消费链路。
2. 必须支持按 `consumerGroup` 查询失败记录。
3. 异常信息只返回摘要，不返回完整堆栈。
4. 查询结果应支持分页。
5. 对死信、失败、重试中状态应重点展示。

### 死信消息重放接口

死信消息重放接口用于将死信表或死信 Topic 中的消息重新发送到原始 Topic 或指定 Topic。该接口风险较高，必须有严格权限控制、幂等校验、操作审计和重放前确认。

接口设计如下：

| 项目     | 说明                                          |
| -------- | --------------------------------------------- |
| 接口路径 | `POST /api/kafka/dead-letters/{id}/replay`    |
| 接口用途 | 重放死信消息                                  |
| 请求方式 | `POST`                                        |
| 权限要求 | 高级管理员或运维角色                          |
| 审计要求 | 必须记录操作人、死信 ID、目标 Topic、重放结果 |

请求示例：

```json
{
  "targetTopic": "ateng.order.created.topic",
  "keepOriginalMessageId": true,
  "reason": "修复订单状态数据后重放"
}
```

响应示例：

```json
{
  "success": true,
  "message": "死信消息重放成功",
  "data": {
    "deadLetterId": 10001,
    "originalMessageId": "1900000000000000001",
    "targetTopic": "ateng.order.created.topic",
    "newMessageId": "1900000000000000001",
    "replayStatus": "SUCCESS"
  }
}
```

请求 DTO 示例：

文件位置：`src/main/java/io/github/atengk/kafka/controller/dto/DeadLetterReplayRequest.java`

该请求对象用于承接死信消息重放参数。

```java
package io.github.atengk.kafka.controller.dto;

import lombok.Data;

/**
 * Kafka 死信消息重放请求
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
public class DeadLetterReplayRequest {

    /**
     * 目标 Topic
     */
    private String targetTopic;

    /**
     * 是否保留原始消息 ID
     */
    private Boolean keepOriginalMessageId = true;

    /**
     * 重放原因
     */
    private String reason;

}
```

接口示例：

```java
package io.github.atengk.kafka.controller;

import io.github.atengk.kafka.controller.dto.DeadLetterReplayRequest;
import io.github.atengk.kafka.service.KafkaDeadLetterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Kafka 死信消息接口
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/kafka/dead-letters")
public class KafkaDeadLetterController {

    private final KafkaDeadLetterService kafkaDeadLetterService;

    /**
     * 重放死信消息
     *
     * @param id      死信消息ID
     * @param request 重放请求
     * @return 重放结果
     */
    @PostMapping("/{id}/replay")
    public Object replay(@PathVariable Long id, @RequestBody DeadLetterReplayRequest request) {
        log.warn("接收Kafka死信消息重放请求，deadLetterId={}，targetTopic={}，reason={}",
                id, request.getTargetTopic(), request.getReason());

        return kafkaDeadLetterService.replay(id, request);
    }

}
```

死信重放要求如下：

1. 重放前必须确认失败原因已经修复。
2. 重放操作必须有权限控制。
3. 重放操作必须记录审计日志。
4. 重放消息必须保留原始消息 ID 或记录 `originalMessageId`。
5. Consumer 必须具备幂等能力。
6. 不允许批量无限制重放，应设置单次重放上限。
7. 重放失败需要记录失败原因，不能静默忽略。

### Topic 管理接口

Topic 管理接口用于查看 Topic 信息、创建测试 Topic、查看分区信息和配置项。生产环境中，Topic 管理属于高危操作，不建议由普通业务系统直接开放完整创建、修改和删除能力。

接口设计建议如下：

| 接口                                | 方法     | 说明                           |
| ----------------------------------- | -------- | ------------------------------ |
| `/api/kafka/topics`                 | `GET`    | 查询 Topic 列表                |
| `/api/kafka/topics/{topic}`         | `GET`    | 查看 Topic 详情                |
| `/api/kafka/topics`                 | `POST`   | 创建 Topic，生产环境慎用       |
| `/api/kafka/topics/{topic}/configs` | `GET`    | 查看 Topic 配置                |
| `/api/kafka/topics/{topic}`         | `DELETE` | 删除 Topic，生产环境不建议开放 |

Topic 创建请求示例：

```json
{
  "clusterName": "biz",
  "topic": "ateng.order.created.topic",
  "partitions": 3,
  "replicas": 3,
  "configs": {
    "retention.ms": "604800000",
    "cleanup.policy": "delete"
  }
}
```

请求 DTO 示例：

文件位置：`src/main/java/io/github/atengk/kafka/controller/dto/TopicCreateRequest.java`

该请求对象用于承接 Topic 创建参数。

```java
package io.github.atengk.kafka.controller.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Topic 创建请求
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
public class TopicCreateRequest {

    /**
     * Kafka 集群名称
     */
    private String clusterName;

    /**
     * Topic 名称
     */
    private String topic;

    /**
     * 分区数
     */
    private Integer partitions = 3;

    /**
     * 副本数
     */
    private Short replicas = 1;

    /**
     * Topic 配置
     */
    private Map<String, String> configs = new HashMap<>();

}
```

Topic 管理接口要求如下：

1. 本地和测试环境可以开放创建 Topic 能力。
2. 生产环境 Topic 创建应走审批流程或中间件平台。
3. 删除 Topic 接口默认不开放。
4. 修改 Topic 配置必须记录审计日志。
5. Topic 名称必须符合命名规范。
6. 分区数增加前必须评估顺序性影响。
7. 副本数不能大于 Broker 数量。
8. Topic 配置必须做白名单校验，不能允许任意配置透传。

### 健康检查接口

健康检查接口用于判断应用与 Kafka 集群的连接状态、Producer 可用性、Consumer 状态和必要 Topic 是否存在。Spring Boot Actuator 可以提供基础健康检查，但业务项目通常还需要补充 Kafka 连接和 Topic 维度的自定义检查。

接口设计如下：

| 项目     | 说明                             |
| -------- | -------------------------------- |
| 接口路径 | `GET /api/kafka/health`          |
| 接口用途 | 查看 Kafka 连接和 Topic 健康状态 |
| 请求方式 | `GET`                            |
| 权限要求 | 运维或只读管理权限               |

响应示例：

```json
{
  "success": true,
  "data": {
    "clusters": [
      {
        "clusterName": "biz",
        "status": "UP",
        "bootstrapServers": "kafka-01:9092,kafka-02:9092",
        "topicCheck": [
          {
            "topic": "ateng.order.created.topic",
            "exists": true,
            "partitions": 3
          }
        ]
      },
      {
        "clusterName": "data",
        "status": "DOWN",
        "errorMessage": "连接超时"
      }
    ]
  }
}
```

健康检查内容建议如下：

| 检查项          | 说明                             |
| --------------- | -------------------------------- |
| Broker 连接     | 能否连接 Kafka 集群              |
| Topic 存在性    | 核心 Topic 是否存在              |
| Topic 分区数    | 分区数是否符合预期               |
| Producer 可用性 | 是否能创建 Producer 或获取元数据 |
| Consumer Group  | 是否能查询消费组状态             |
| Consumer Lag    | 是否存在严重堆积                 |
| 认证状态        | SASL / SSL 配置是否正常          |
| 多集群状态      | 每个集群分别检查                 |

健康检查实现建议使用 Kafka AdminClient 查询元数据。生产环境不建议健康检查接口频繁发送真实业务消息，因为这可能污染 Topic 数据。可以通过查询 Topic 元数据、Describe Cluster、Describe Topics 等方式完成检查。

健康检查要求如下：

1. 健康检查必须区分集群。
2. 不应暴露敏感认证信息。
3. 不应返回完整 Broker 内网敏感细节给普通用户。
4. 检查超时时间要短，避免接口阻塞。
5. 健康检查失败应接入告警。
6. 对核心 Topic 缺失、分区异常、Lag 过高等情况应返回明确状态。

### 管理接口权限控制

管理接口权限控制用于保护消息发送、死信重放、Topic 管理、消费状态查询等高敏感接口。Kafka 管理接口一旦被滥用，可能导致重复发送消息、误重放死信、误删 Topic、泄露业务数据或影响生产消费链路。

接口权限建议如下：

| 接口类型           | 权限等级 | 说明                     |
| ------------------ | -------- | ------------------------ |
| 消息发送接口       | 高       | 可能产生真实业务消息     |
| 消息查询接口       | 中       | 可能查看业务数据         |
| 消费状态接口       | 中       | 可查看消费状态和异常摘要 |
| 死信重放接口       | 高       | 可能重复执行业务         |
| Topic 创建接口     | 高       | 影响 Kafka 资源          |
| Topic 删除接口     | 极高     | 生产环境默认禁用         |
| 健康检查接口       | 低到中   | 运维只读能力             |
| ACL 或安全配置接口 | 极高     | 建议不在业务系统开放     |

权限控制建议使用角色模型：

| 角色             | 权限                               |
| ---------------- | ---------------------------------- |
| `KAFKA_VIEWER`   | 只读查询 Topic、消息记录、消费状态 |
| `KAFKA_OPERATOR` | 可执行死信重放、失败重试、健康检查 |
| `KAFKA_ADMIN`    | 可管理 Topic、配置和高级操作       |
| `SYSTEM_ADMIN`   | 系统级权限，包含所有操作           |

接口安全要求如下：

1. 所有管理接口必须登录认证。
2. 高危接口必须校验角色权限。
3. 死信重放、Topic 创建、Topic 删除必须记录审计日志。
4. 生产环境高危接口建议增加二次确认或审批流程。
5. 接口应限制请求频率，避免批量误操作。
6. 查询接口应做数据脱敏。
7. 返回结果中不得包含 Kafka 密码、证书、JAAS 明文和敏感消息字段。
8. 生产环境默认关闭 Topic 删除能力。

权限注解示例：

```java
@PreAuthorize("hasAuthority('KAFKA_OPERATOR')")
@PostMapping("/{id}/replay")
public Object replay(@PathVariable Long id, @RequestBody DeadLetterReplayRequest request) {
    return kafkaDeadLetterService.replay(id, request);
}
```

审计日志建议字段如下：

| 字段           | 说明                                      |
| -------------- | ----------------------------------------- |
| `operator`     | 操作人                                    |
| `operation`    | 操作类型                                  |
| `resourceType` | 资源类型，例如 Topic、Message、DeadLetter |
| `resourceId`   | 资源 ID                                   |
| `requestBody`  | 请求摘要                                  |
| `resultStatus` | 操作结果                                  |
| `clientIp`     | 客户端 IP                                 |
| `operateTime`  | 操作时间                                  |

管理接口最终建议是：只读接口可以适度开放给开发和运维角色；消息发送、死信重放、Topic 管理等接口必须严格限制权限；Topic 删除、ACL 修改、安全配置变更等能力不建议放在普通业务系统中，应由中间件平台或运维流程统一管理。

## 数据库设计

本章节用于说明 Kafka 开发中常用的数据库表设计，包括消息发送记录表、消息消费记录表、消费幂等表、死信消息表、重试记录表、Topic 配置表、表索引设计和数据清理策略。

Kafka 本身负责消息存储和分发，但不适合作为业务查询、审计、补偿和状态管理数据库。实际项目中，如果需要查询消息发送状态、消费结果、失败原因、重试次数、死信内容和人工重放记录，就需要配套数据库表保存关键链路数据。

### 消息发送记录表

消息发送记录表用于记录 Producer 发送 Kafka 消息的过程和结果。它适用于可靠消息、本地消息表、发送失败补偿、发送审计和问题排查。

推荐表结构如下：

```sql
CREATE TABLE kafka_send_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息唯一标识',
    cluster_name VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT 'Kafka集群名称',
    topic VARCHAR(128) NOT NULL COMMENT 'Topic名称',
    message_key VARCHAR(255) DEFAULT NULL COMMENT '消息Key',
    message_type VARCHAR(64) DEFAULT NULL COMMENT '消息类型',
    message_version VARCHAR(32) DEFAULT NULL COMMENT '消息版本',
    trace_id VARCHAR(128) DEFAULT NULL COMMENT '链路追踪ID',
    message_body MEDIUMTEXT NOT NULL COMMENT '消息内容',
    send_status VARCHAR(32) NOT NULL COMMENT '发送状态：PENDING、SENDING、SUCCESS、FAILED',
    partition_id INT DEFAULT NULL COMMENT '写入分区',
    offset_value BIGINT DEFAULT NULL COMMENT '写入Offset',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '发送重试次数',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '异常信息摘要',
    send_time DATETIME DEFAULT NULL COMMENT '发送时间',
    success_time DATETIME DEFAULT NULL COMMENT '发送成功时间',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_topic_key (topic, message_key),
    KEY idx_trace_id (trace_id),
    KEY idx_send_status_time (send_status, create_time),
    KEY idx_cluster_topic (cluster_name, topic)
) COMMENT='Kafka消息发送记录表';
```

字段说明如下：

| 字段            | 说明                                         |
| --------------- | -------------------------------------------- |
| `message_id`    | 全局唯一消息 ID，用于链路追踪、幂等和重放    |
| `cluster_name`  | 多 Kafka 集群场景下区分目标集群              |
| `topic`         | 目标 Topic                                   |
| `message_key`   | Kafka Key，通常是业务 Key                    |
| `message_type`  | 消息类型，例如 `ORDER_CREATED`               |
| `trace_id`      | 链路追踪 ID                                  |
| `message_body`  | 发送消息内容，生产环境可按需脱敏或只保存摘要 |
| `send_status`   | 发送状态                                     |
| `partition_id`  | Kafka 返回的分区                             |
| `offset_value`  | Kafka 返回的 Offset                          |
| `retry_count`   | Producer 或补偿任务发送次数                  |
| `error_message` | 发送失败原因摘要                             |

发送状态建议如下：

| 状态      | 说明     |
| --------- | -------- |
| `PENDING` | 待发送   |
| `SENDING` | 发送中   |
| `SUCCESS` | 发送成功 |
| `FAILED`  | 发送失败 |

使用要求如下：

1. 重要业务消息建议先写发送记录，再发送 Kafka。
2. 发送成功后更新 `partition_id`、`offset_value`、`send_status` 和 `success_time`。
3. 发送失败后更新 `send_status`、`retry_count` 和 `error_message`。
4. 如果消息体包含敏感信息，`message_body` 应脱敏或只保存摘要。
5. 查询接口应优先通过 `message_id`、`trace_id`、`topic + key` 查询。

### 消息消费记录表

消息消费记录表用于记录 Consumer 对消息的处理过程和结果。它是排查消费失败、重复消费、消费延迟、死信重放和消费审计的基础。

推荐表结构如下：

```sql
CREATE TABLE kafka_consume_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息唯一标识',
    cluster_name VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT 'Kafka集群名称',
    topic VARCHAR(128) NOT NULL COMMENT 'Topic名称',
    partition_id INT NOT NULL COMMENT '分区编号',
    offset_value BIGINT NOT NULL COMMENT 'Offset',
    message_key VARCHAR(255) DEFAULT NULL COMMENT '消息Key',
    message_type VARCHAR(64) DEFAULT NULL COMMENT '消息类型',
    trace_id VARCHAR(128) DEFAULT NULL COMMENT '链路追踪ID',
    consumer_group VARCHAR(128) NOT NULL COMMENT '消费组',
    consume_status VARCHAR(32) NOT NULL COMMENT '消费状态：PROCESSING、SUCCESS、FAILED、DEAD_LETTER、IGNORED',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '消费重试次数',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '异常信息摘要',
    consume_start_time DATETIME DEFAULT NULL COMMENT '开始消费时间',
    consume_end_time DATETIME DEFAULT NULL COMMENT '消费结束时间',
    cost_millis BIGINT DEFAULT NULL COMMENT '消费耗时毫秒',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_message_group (message_id, consumer_group),
    KEY idx_topic_partition_offset (topic, partition_id, offset_value),
    KEY idx_consumer_status_time (consumer_group, consume_status, update_time),
    KEY idx_trace_id (trace_id),
    KEY idx_cluster_topic_group (cluster_name, topic, consumer_group)
) COMMENT='Kafka消息消费记录表';
```

消费状态建议如下：

| 状态          | 说明                       |
| ------------- | -------------------------- |
| `PROCESSING`  | 正在消费                   |
| `SUCCESS`     | 消费成功                   |
| `FAILED`      | 消费失败，等待重试或补偿   |
| `DEAD_LETTER` | 已进入死信                 |
| `IGNORED`     | 重复消息或无需处理，已忽略 |

使用要求如下：

1. 消费开始时写入或更新为 `PROCESSING`。
2. 消费成功后更新为 `SUCCESS`，并记录耗时。
3. 消费失败后更新为 `FAILED`，并记录异常摘要。
4. 进入死信后更新为 `DEAD_LETTER`。
5. 重复消费且原状态为 `SUCCESS` 时，可以标记为 `IGNORED` 或直接跳过。
6. 消费记录必须包含 `topic`、`partition_id`、`offset_value`，便于精准定位 Kafka 消息位置。

### 消费幂等表

消费幂等表用于防止同一消息或同一业务动作被重复处理。它可以与消费记录表合并，也可以独立设计。对于核心业务，推荐独立设计幂等表或在消费记录表上建立严格唯一约束。

推荐表结构如下：

```sql
CREATE TABLE kafka_consume_idempotent (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    idempotent_key VARCHAR(255) NOT NULL COMMENT '幂等键',
    message_id VARCHAR(64) NOT NULL COMMENT '消息唯一标识',
    business_key VARCHAR(255) DEFAULT NULL COMMENT '业务幂等键',
    consumer_group VARCHAR(128) NOT NULL COMMENT '消费组',
    topic VARCHAR(128) NOT NULL COMMENT 'Topic名称',
    process_status VARCHAR(32) NOT NULL COMMENT '处理状态：PROCESSING、SUCCESS、FAILED',
    expire_time DATETIME DEFAULT NULL COMMENT '幂等记录过期时间',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_idempotent_key (idempotent_key),
    UNIQUE KEY uk_message_group (message_id, consumer_group),
    KEY idx_business_key (business_key),
    KEY idx_expire_time (expire_time),
    KEY idx_status_time (process_status, update_time)
) COMMENT='Kafka消费幂等表';
```

幂等键建议如下：

| 幂等键                                    | 说明           |
| ----------------------------------------- | -------------- |
| `messageId + consumerGroup`               | 消息级幂等     |
| `businessKey + consumerGroup`             | 业务级幂等     |
| `messageId + messageType + consumerGroup` | 多事件类型场景 |
| `orderId + eventType + consumerGroup`     | 订单事件幂等   |
| `paymentId + eventType + consumerGroup`   | 支付事件幂等   |

处理流程如下：

```text
收到消息
  -> 生成 idempotentKey
  -> 插入幂等表 PROCESSING
  -> 插入成功，执行业务逻辑
  -> 业务成功，更新 SUCCESS
  -> 插入失败，查询原记录
  -> 原记录 SUCCESS，跳过并提交 Offset
  -> 原记录 PROCESSING / FAILED，按策略重试或补偿
```

设计要求如下：

1. 幂等判断必须依赖唯一索引，不能只依赖先查再插。
2. `idempotent_key` 必须稳定。
3. 核心业务需要同时考虑消息级幂等和业务级幂等。
4. 幂等记录保留时间应大于 Topic 保留时间和最大重试周期。
5. 幂等表清理必须谨慎，避免死信重放时失去幂等保护。

### 死信消息表

死信消息表用于保存进入死信 Topic 或死信处理流程的消息。它支持问题排查、人工处理、消息重放、失败统计和告警展示。

推荐表结构如下：

```sql
CREATE TABLE kafka_dead_letter_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(64) DEFAULT NULL COMMENT '消息唯一标识',
    original_message_id VARCHAR(64) DEFAULT NULL COMMENT '原始消息ID',
    cluster_name VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT 'Kafka集群名称',
    source_topic VARCHAR(128) NOT NULL COMMENT '原始Topic',
    source_partition INT NOT NULL COMMENT '原始分区',
    source_offset BIGINT NOT NULL COMMENT '原始Offset',
    dead_letter_topic VARCHAR(128) NOT NULL COMMENT '死信Topic',
    message_key VARCHAR(255) DEFAULT NULL COMMENT '消息Key',
    message_type VARCHAR(64) DEFAULT NULL COMMENT '消息类型',
    trace_id VARCHAR(128) DEFAULT NULL COMMENT '链路追踪ID',
    consumer_group VARCHAR(128) DEFAULT NULL COMMENT '消费组',
    message_headers TEXT DEFAULT NULL COMMENT '消息Header JSON',
    message_body MEDIUMTEXT NOT NULL COMMENT '消息内容',
    exception_class VARCHAR(255) DEFAULT NULL COMMENT '异常类名',
    exception_message VARCHAR(1000) DEFAULT NULL COMMENT '异常信息摘要',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    process_status VARCHAR(32) NOT NULL COMMENT '处理状态：PENDING、REPLAYED、IGNORED、FAILED',
    replay_count INT NOT NULL DEFAULT 0 COMMENT '重放次数',
    last_replay_time DATETIME DEFAULT NULL COMMENT '最近重放时间',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    KEY idx_message_id (message_id),
    KEY idx_original_message_id (original_message_id),
    KEY idx_source_topic_offset (source_topic, source_partition, source_offset),
    KEY idx_process_status (process_status, update_time),
    KEY idx_trace_id (trace_id),
    KEY idx_cluster_topic (cluster_name, source_topic)
) COMMENT='Kafka死信消息表';
```

死信处理状态建议如下：

| 状态       | 说明         |
| ---------- | ------------ |
| `PENDING`  | 待处理       |
| `REPLAYED` | 已重放       |
| `IGNORED`  | 已忽略       |
| `FAILED`   | 死信处理失败 |

使用要求如下：

1. 死信表必须保存原始 Topic、Partition、Offset。
2. 死信表必须保存异常类名和异常摘要。
3. 死信重放必须更新 `replay_count` 和 `last_replay_time`。
4. 重放时应保留 `original_message_id`。
5. 死信消息内容展示时必须脱敏。
6. 死信消息增长应接入监控和告警。

### 重试记录表

重试记录表用于记录 Producer 发送重试、Consumer 消费重试、延迟重试和补偿任务的执行状态。它可以支撑延迟重试、失败补偿和人工排查。

推荐表结构如下：

```sql
CREATE TABLE kafka_retry_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    retry_id VARCHAR(64) NOT NULL COMMENT '重试记录ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息唯一标识',
    cluster_name VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT 'Kafka集群名称',
    source_topic VARCHAR(128) NOT NULL COMMENT '原始Topic',
    retry_topic VARCHAR(128) DEFAULT NULL COMMENT '重试Topic',
    message_key VARCHAR(255) DEFAULT NULL COMMENT '消息Key',
    message_type VARCHAR(64) DEFAULT NULL COMMENT '消息类型',
    trace_id VARCHAR(128) DEFAULT NULL COMMENT '链路追踪ID',
    retry_scene VARCHAR(32) NOT NULL COMMENT '重试场景：PRODUCER、CONSUMER、DELAY、DEAD_LETTER',
    retry_status VARCHAR(32) NOT NULL COMMENT '重试状态：PENDING、RETRYING、SUCCESS、FAILED、DEAD_LETTER',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '当前重试次数',
    max_retry_count INT NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    next_retry_time DATETIME DEFAULT NULL COMMENT '下次重试时间',
    last_retry_time DATETIME DEFAULT NULL COMMENT '最近重试时间',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '最近异常信息摘要',
    message_body MEDIUMTEXT DEFAULT NULL COMMENT '消息内容',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_retry_id (retry_id),
    KEY idx_message_id (message_id),
    KEY idx_retry_status_time (retry_status, next_retry_time),
    KEY idx_scene_status (retry_scene, retry_status),
    KEY idx_trace_id (trace_id)
) COMMENT='Kafka消息重试记录表';
```

重试状态建议如下：

| 状态          | 说明       |
| ------------- | ---------- |
| `PENDING`     | 待重试     |
| `RETRYING`    | 重试中     |
| `SUCCESS`     | 重试成功   |
| `FAILED`      | 重试失败   |
| `DEAD_LETTER` | 已进入死信 |

使用要求如下：

1. 每次重试必须增加 `retry_count`。
2. 重试失败必须记录 `error_message`。
3. 延迟重试必须维护 `next_retry_time`。
4. 超过最大重试次数后应进入死信或标记 `FAILED`。
5. 调度扫描时应按 `retry_status + next_retry_time` 查询。
6. 多实例执行重试任务时必须做任务抢占或分布式锁。

### Topic 配置表

Topic 配置表用于保存项目内 Topic 的申请、初始化和治理配置。它不一定替代中间件平台，但可以作为业务系统维护 Topic 元数据和文档化配置的依据。

推荐表结构如下：

```sql
CREATE TABLE kafka_topic_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    cluster_name VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT 'Kafka集群名称',
    topic VARCHAR(128) NOT NULL COMMENT 'Topic名称',
    business_domain VARCHAR(64) DEFAULT NULL COMMENT '业务域',
    message_type VARCHAR(64) DEFAULT NULL COMMENT '消息类型',
    partitions INT NOT NULL COMMENT '分区数',
    replicas INT NOT NULL COMMENT '副本数',
    retention_ms BIGINT DEFAULT NULL COMMENT '保留时间毫秒',
    cleanup_policy VARCHAR(64) DEFAULT 'delete' COMMENT '清理策略',
    compression_type VARCHAR(32) DEFAULT NULL COMMENT '压缩类型',
    owner VARCHAR(64) DEFAULT NULL COMMENT '负责人',
    description VARCHAR(500) DEFAULT NULL COMMENT '说明',
    config_status VARCHAR(32) NOT NULL COMMENT '状态：ACTIVE、DISABLED、DEPRECATED',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_cluster_topic (cluster_name, topic),
    KEY idx_business_domain (business_domain),
    KEY idx_message_type (message_type),
    KEY idx_config_status (config_status)
) COMMENT='Kafka Topic配置表';
```

Topic 状态建议如下：

| 状态         | 说明             |
| ------------ | ---------------- |
| `ACTIVE`     | 正常使用         |
| `DISABLED`   | 暂停使用         |
| `DEPRECATED` | 已废弃，等待下线 |

使用要求如下：

1. Topic 名称必须唯一。
2. 多集群场景下使用 `cluster_name + topic` 唯一约束。
3. 应记录 Topic 所属业务域和负责人。
4. 分区数、副本数和保留时间应与实际 Kafka 配置保持一致。
5. 生产环境 Topic 变更应通过审批或中间件平台执行。
6. 废弃 Topic 不应立即删除，应先标记 `DEPRECATED` 并观察消费情况。

### 表索引设计

表索引设计用于保证 Kafka 链路查询、幂等判断、重试扫描和死信处理具备稳定性能。Kafka 相关表通常写入量较大，如果索引设计不合理，会影响消费性能和管理接口查询性能。

推荐索引设计如下：

| 表                          | 推荐索引                     | 用途                   |
| --------------------------- | ---------------------------- | ---------------------- |
| `kafka_send_record`         | `uk_message_id`              | 按消息 ID 查询发送记录 |
| `kafka_send_record`         | `idx_trace_id`               | 按 TraceId 查询链路    |
| `kafka_send_record`         | `idx_send_status_time`       | 扫描待发送或失败记录   |
| `kafka_consume_record`      | `uk_message_group`           | 消费幂等               |
| `kafka_consume_record`      | `idx_topic_partition_offset` | 按 Kafka 位置查询      |
| `kafka_consume_record`      | `idx_consumer_status_time`   | 查询消费失败记录       |
| `kafka_consume_idempotent`  | `uk_idempotent_key`          | 幂等唯一约束           |
| `kafka_dead_letter_message` | `idx_process_status`         | 查询待处理死信         |
| `kafka_retry_record`        | `idx_retry_status_time`      | 扫描到期重试任务       |
| `kafka_topic_config`        | `uk_cluster_topic`           | Topic 唯一约束         |

索引设计要求如下：

1. 幂等表必须有唯一索引，不能只依赖代码判断。
2. 重试表必须有 `retry_status + next_retry_time` 索引。
3. 死信表必须有 `process_status + update_time` 索引。
4. 查询接口常用条件必须有组合索引支持。
5. 不要为每个字段都建索引，避免写入性能下降。
6. 大字段如 `message_body`、`message_headers` 不应直接建普通索引。
7. 高频写入表需要关注索引数量和字段长度。

### 数据清理策略

数据清理策略用于防止 Kafka 相关数据库表无限增长。发送记录、消费记录、幂等记录、重试记录和死信记录都会持续写入，如果不清理，会影响查询性能、备份成本和存储成本。

推荐保留策略如下：

| 表                          | 建议保留时间                             |
| --------------------------- | ---------------------------------------- |
| `kafka_send_record`         | 普通业务 30 到 90 天，核心审计按要求延长 |
| `kafka_consume_record`      | 30 到 90 天                              |
| `kafka_consume_idempotent`  | 大于 Topic 保留时间和最大重试周期        |
| `kafka_dead_letter_message` | 30 到 180 天，未处理死信不清理           |
| `kafka_retry_record`        | 成功记录 30 天，失败记录按排查要求保留   |
| `kafka_topic_config`        | 长期保留                                 |

清理成功消费记录示例：

```sql
DELETE FROM kafka_consume_record
WHERE consume_status IN ('SUCCESS', 'IGNORED')
  AND update_time < DATE_SUB(NOW(), INTERVAL 90 DAY)
LIMIT 1000;
```

清理成功发送记录示例：

```sql
DELETE FROM kafka_send_record
WHERE send_status = 'SUCCESS'
  AND update_time < DATE_SUB(NOW(), INTERVAL 90 DAY)
LIMIT 1000;
```

清理已成功重试记录示例：

```sql
DELETE FROM kafka_retry_record
WHERE retry_status = 'SUCCESS'
  AND update_time < DATE_SUB(NOW(), INTERVAL 30 DAY)
LIMIT 1000;
```

清理策略要求如下：

1. 清理任务必须分批执行，避免大事务。
2. 未处理死信、失败记录、重试中记录不得直接清理。
3. 幂等记录清理前必须确认不会再发生死信重放。
4. 清理任务应记录执行日志和删除数量。
5. 核心业务审计数据应按合规要求保留。
6. 大数据量场景建议按月分表或归档到历史表。

## 日志设计

本章节用于说明 Kafka 消息链路中的日志设计，包括 Producer 日志、Consumer 日志、异常日志、重试日志、死信日志、TraceId 透传、消息链路日志和日志脱敏。

Kafka 日志的目标是让开发和运维人员可以基于 `traceId`、`messageId`、`topic`、`partition`、`offset`、`key` 快速定位一条消息从发送到消费、重试、死信和重放的完整过程。日志应做到字段统一、级别清晰、可检索、可脱敏。

### Producer 日志

Producer 日志用于记录消息发送前、发送成功和发送失败的关键信息。Producer 日志不应只打印“发送成功”或“发送失败”，必须包含可定位消息的字段。

发送前日志建议：

```text
准备发送Kafka消息，cluster={}，topic={}，key={}，messageId={}，messageType={}，traceId={}
```

发送成功日志建议：

```text
Kafka消息发送成功，cluster={}，topic={}，partition={}，offset={}，key={}，messageId={}，traceId={}，costMillis={}
```

发送失败日志建议：

```text
Kafka消息发送失败，cluster={}，topic={}，key={}，messageId={}，messageType={}，traceId={}，error={}
```

Producer 日志字段建议如下：

| 字段          | 说明           |
| ------------- | -------------- |
| `cluster`     | Kafka 集群名称 |
| `topic`       | Topic 名称     |
| `partition`   | 写入分区       |
| `offset`      | 写入 Offset    |
| `key`         | 消息 Key       |
| `messageId`   | 消息唯一标识   |
| `messageType` | 消息类型       |
| `traceId`     | 链路追踪 ID    |
| `costMillis`  | 发送耗时       |
| `error`       | 失败原因摘要   |

Producer 日志要求如下：

1. 发送成功使用 `info` 级别。
2. 发送失败使用 `error` 级别，并打印异常堆栈。
3. 发送重试可以使用 `warn` 级别。
4. 不要打印完整敏感消息体。
5. 批量发送应打印批次大小和关键摘要。
6. 多集群发送必须打印 `cluster`。

### Consumer 日志

Consumer 日志用于记录消息接收、业务处理成功、Offset 提交和重复消费跳过等行为。Consumer 日志是排查消息是否被处理、处理到哪里、耗时多久的关键依据。

开始消费日志建议：

```text
开始消费Kafka消息，cluster={}，topic={}，partition={}，offset={}，key={}，messageId={}，messageType={}，traceId={}，consumerGroup={}
```

消费成功日志建议：

```text
Kafka消息消费成功，cluster={}，topic={}，partition={}，offset={}，key={}，messageId={}，traceId={}，consumerGroup={}，costMillis={}
```

重复消费跳过日志建议：

```text
检测到Kafka重复消息，跳过处理，topic={}，partition={}，offset={}，key={}，messageId={}，consumerGroup={}
```

Offset 提交日志建议：

```text
Kafka消息Offset提交成功，topic={}，partition={}，offset={}，messageId={}，consumerGroup={}
```

Consumer 日志字段建议如下：

| 字段            | 说明           |
| --------------- | -------------- |
| `cluster`       | Kafka 集群名称 |
| `topic`         | Topic 名称     |
| `partition`     | 分区编号       |
| `offset`        | 消息 Offset    |
| `key`           | 消息 Key       |
| `messageId`     | 消息唯一标识   |
| `messageType`   | 消息类型       |
| `traceId`       | 链路追踪 ID    |
| `consumerGroup` | 消费组         |
| `costMillis`    | 消费耗时       |

Consumer 日志要求如下：

1. 开始消费和消费成功使用 `info` 级别。
2. 重复消息跳过使用 `warn` 或 `info`，按业务重要性决定。
3. Offset 提交失败必须使用 `error`。
4. 批量消费必须打印批次数量、失败数量和耗时。
5. 消费日志不应打印完整敏感消息体。
6. 业务处理过程中的关键状态变更也应打印业务日志。

### 异常日志

异常日志用于记录消费失败、发送失败、反序列化失败、Offset 提交失败和业务处理失败。异常日志必须包含足够的上下文，避免只看到堆栈却不知道是哪条消息失败。

异常日志建议格式：

```text
Kafka消息处理异常，cluster={}，topic={}，partition={}，offset={}，key={}，messageId={}，messageType={}，traceId={}，consumerGroup={}，exceptionClass={}，errorMessage={}
```

异常日志字段如下：

| 字段              | 说明                     |
| ----------------- | ------------------------ |
| `exceptionClass`  | 异常类名                 |
| `errorMessage`    | 异常摘要                 |
| `stackTrace`      | 异常堆栈                 |
| `retryable`       | 是否可重试               |
| `retryCount`      | 当前重试次数             |
| `deadLetterTopic` | 死信 Topic，若已进入死信 |

异常日志要求如下：

1. 最终失败必须使用 `error` 级别。
2. 可重试失败可以使用 `warn` 级别。
3. 必须打印异常堆栈。
4. 必须打印 Kafka 定位字段：Topic、Partition、Offset。
5. 必须打印业务定位字段：messageId、key、traceId。
6. 不要把完整消息体作为错误日志默认输出。
7. 反序列化异常应打印 payload 大小，不打印完整原文。

### 重试日志

重试日志用于记录消息进入重试、正在重试、重试成功和重试失败的过程。重试日志对判断下游系统是否抖动、消息是否反复失败、重试是否生效非常重要。

进入重试日志建议：

```text
Kafka消息进入重试，topic={}，partition={}，offset={}，key={}，messageId={}，retryCount={}，nextRetryTime={}，reason={}
```

重试成功日志建议：

```text
Kafka消息重试成功，topic={}，key={}，messageId={}，retryCount={}，traceId={}，costMillis={}
```

重试失败日志建议：

```text
Kafka消息重试失败，topic={}，key={}，messageId={}，retryCount={}，maxRetryCount={}，traceId={}，reason={}
```

重试日志字段建议如下：

| 字段            | 说明                                     |
| --------------- | ---------------------------------------- |
| `retryCount`    | 当前重试次数                             |
| `maxRetryCount` | 最大重试次数                             |
| `nextRetryTime` | 下次重试时间                             |
| `retryTopic`    | 重试 Topic                               |
| `retryScene`    | 重试场景，例如 Producer、Consumer、Delay |
| `reason`        | 重试原因                                 |

重试日志要求如下：

1. 单次重试失败使用 `warn`。
2. 超过最大重试次数使用 `error`。
3. 重试日志必须包含 `retryCount`。
4. 延迟重试必须打印 `nextRetryTime`。
5. Retry Topic 重试必须打印 `retryTopic`。
6. 重试失败进入死信时，需要同时打印死信 Topic。

### 死信日志

死信日志用于记录消息进入死信、死信保存、死信重放和死信处理失败的过程。死信日志一般代表消息已经无法通过普通重试恢复，需要人工或补偿流程介入。

进入死信日志建议：

```text
Kafka消息进入死信，sourceTopic={}，deadLetterTopic={}，partition={}，offset={}，key={}，messageId={}，traceId={}，consumerGroup={}，retryCount={}，reason={}
```

死信重放日志建议：

```text
Kafka死信消息重放，deadLetterId={}，sourceTopic={}，targetTopic={}，messageId={}，originalMessageId={}，operator={}，reason={}
```

死信重放成功日志建议：

```text
Kafka死信消息重放成功，deadLetterId={}，targetTopic={}，partition={}，offset={}，messageId={}，operator={}
```

死信日志字段建议如下：

| 字段                | 说明               |
| ------------------- | ------------------ |
| `deadLetterId`      | 死信表主键 ID      |
| `sourceTopic`       | 原始 Topic         |
| `deadLetterTopic`   | 死信 Topic         |
| `targetTopic`       | 重放目标 Topic     |
| `originalMessageId` | 原始消息 ID        |
| `operator`          | 操作人             |
| `reason`            | 进入死信或重放原因 |

死信日志要求如下：

1. 进入死信必须使用 `error` 级别。
2. 死信重放必须使用 `warn` 级别。
3. 死信重放失败必须使用 `error` 级别。
4. 死信日志必须能关联死信表记录。
5. 死信日志必须记录操作人和重放原因。
6. 死信消息体展示和日志打印都必须脱敏。

### TraceId 透传

TraceId 透传用于将一次业务请求、Kafka 发送、Kafka 消费、下游调用和异常处理串联起来。TraceId 应同时写入日志 MDC、Kafka Header 和消息体治理字段中。

推荐 Header：

```text
x-trace-id
```

Producer 发送时应写入 TraceId：

```java
record.headers().add(new RecordHeader(
        KafkaHeaderConstant.TRACE_ID,
        traceId.getBytes(StandardCharsets.UTF_8)
));
```

Consumer 消费时应读取 TraceId 并放入 MDC。

文件位置：`src/main/java/io/github/atengk/kafka/support/KafkaTraceSupport.java`

该工具类用于从 Kafka Header 中读取 TraceId，并写入日志 MDC。

```java
package io.github.atengk.kafka.support;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.kafka.constant.KafkaHeaderConstant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;

/**
 * Kafka TraceId 支持工具
 *
 * @author Ateng
 * @since 2026-05-11
 */
public final class KafkaTraceSupport {

    private static final String MDC_TRACE_ID = "traceId";

    private KafkaTraceSupport() {
    }

    /**
     * 将 Kafka 消息中的 TraceId 写入 MDC
     *
     * @param record Kafka 消息记录
     */
    public static void putTraceId(ConsumerRecord<?, ?> record) {
        if (record == null) {
            return;
        }

        String traceId = KafkaHeaderSupport.getString(record.headers(), KafkaHeaderConstant.TRACE_ID);
        if (StrUtil.isBlank(traceId)) {
            traceId = KafkaMessageIdGenerator.nextMessageId();
        }

        MDC.put(MDC_TRACE_ID, traceId);
    }

    /**
     * 清理 MDC TraceId
     */
    public static void clear() {
        MDC.remove(MDC_TRACE_ID);
    }

}
```

Consumer 中使用示例：

```java
try {
    KafkaTraceSupport.putTraceId(record);

    log.info("开始消费Kafka消息，topic={}，partition={}，offset={}，key={}",
            record.topic(), record.partition(), record.offset(), record.key());

    // 执行业务逻辑
} finally {
    KafkaTraceSupport.clear();
}
```

TraceId 透传要求如下：

1. HTTP 请求入口应生成或接收 TraceId。
2. Producer 发送 Kafka 消息时必须写入 Header。
3. Consumer 消费 Kafka 消息时必须读取 Header 并写入 MDC。
4. Consumer 调用下游 HTTP、RPC 或数据库日志时应继续透传 TraceId。
5. 如果消息缺失 TraceId，应生成新的 TraceId 并记录。
6. 日志格式中必须包含 `%X{traceId}`。

Logback 日志格式示例：

```xml
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{traceId}] %logger{36} - %msg%n</pattern>
```

### 消息链路日志

消息链路日志用于串联一条消息从 Producer 到 Consumer、重试、死信和重放的完整生命周期。链路日志应围绕 `messageId` 和 `traceId` 建立统一查询能力。

完整链路示例：

```text
HTTP请求进入，traceId=T001
  -> 业务创建订单
  -> 写入 kafka_send_record，messageId=M001
  -> Producer发送消息，topic=ateng.order.created.topic
  -> Kafka返回 partition=0，offset=1024
  -> Consumer收到消息，messageId=M001，traceId=T001
  -> 写入 kafka_consume_record
  -> 幂等校验通过
  -> 业务处理成功
  -> 提交 Offset
```

如果失败：

```text
Consumer收到消息
  -> 业务处理失败
  -> 第1次重试
  -> 第2次重试
  -> 第3次重试
  -> 投递死信Topic
  -> 写入kafka_dead_letter_message
  -> 触发告警
  -> 人工修复
  -> 重放死信消息
  -> Consumer重新处理成功
```

链路日志建议字段如下：

| 阶段     | 必要字段                                              |
| -------- | ----------------------------------------------------- |
| 发送前   | `traceId`、`messageId`、`topic`、`key`、`messageType` |
| 发送成功 | `partition`、`offset`、`costMillis`                   |
| 消费开始 | `consumerGroup`、`partition`、`offset`                |
| 幂等判断 | `idempotentKey`、`processStatus`                      |
| 消费成功 | `costMillis`、`consumeStatus`                         |
| 消费失败 | `exceptionClass`、`errorMessage`                      |
| 重试     | `retryCount`、`nextRetryTime`                         |
| 死信     | `deadLetterTopic`、`deadLetterId`                     |
| 重放     | `operator`、`targetTopic`、`originalMessageId`        |

链路日志要求如下：

1. `messageId` 必须贯穿发送、消费、重试、死信和重放。
2. `traceId` 必须贯穿请求链路和异步链路。
3. 所有日志字段命名应保持一致。
4. 关键阶段应落库，日志只作为检索和排查辅助。
5. 日志平台应支持按 `traceId` 和 `messageId` 检索。
6. 多集群场景必须加入 `clusterName`。

### 日志脱敏

日志脱敏用于防止 Kafka 消息体、异常日志、接口请求和死信内容泄露敏感信息。Kafka 消息经常包含用户 ID、手机号、地址、证件号、支付信息、Token 或内部业务字段，日志必须按规范脱敏。

常见敏感字段如下：

| 字段类型 | 示例                                   |
| -------- | -------------------------------------- |
| 手机号   | `phone`、`mobile`                      |
| 身份证号 | `idCard`、`certificateNo`              |
| 银行卡   | `bankCardNo`                           |
| 密码     | `password`                             |
| Token    | `token`、`accessToken`、`refreshToken` |
| 密钥     | `secret`、`privateKey`、`apiKey`       |
| 地址     | `address`                              |
| 邮箱     | `email`                                |

脱敏规则建议如下：

| 类型     | 脱敏示例               |
| -------- | ---------------------- |
| 手机号   | `138****8000`          |
| 身份证号 | `110101********1234`   |
| 银行卡   | `6222************1234` |
| 邮箱     | `at***@example.com`    |
| Token    | 只显示前 6 位和后 4 位 |
| 密码     | 永远不打印             |
| 密钥     | 永远不打印             |

文件位置：`src/main/java/io/github/atengk/kafka/support/LogMaskSupport.java`

该工具类用于对日志中的常见敏感字段进行脱敏处理。

```java
package io.github.atengk.kafka.support;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 日志脱敏工具
 *
 * @author Ateng
 * @since 2026-05-11
 */
public final class LogMaskSupport {

    private LogMaskSupport() {
    }

    /**
     * 脱敏手机号
     *
     * @param mobile 手机号
     * @return 脱敏后的手机号
     */
    public static String mobile(String mobile) {
        if (StrUtil.isBlank(mobile)) {
            return mobile;
        }
        return DesensitizedUtil.mobilePhone(mobile);
    }

    /**
     * 脱敏身份证号
     *
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    public static String idCard(String idCard) {
        if (StrUtil.isBlank(idCard)) {
            return idCard;
        }
        return DesensitizedUtil.idCardNum(idCard, 6, 4);
    }

    /**
     * 脱敏邮箱
     *
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    public static String email(String email) {
        if (StrUtil.isBlank(email)) {
            return email;
        }
        return DesensitizedUtil.email(email);
    }

}
```

使用示例：

```java
log.info("处理用户消息，userId={}，mobile={}",
        userId,
        LogMaskSupport.mobile(mobile));
```

日志脱敏要求如下：

1. 默认不打印完整消息体。
2. 需要打印消息体时，只打印摘要或脱敏后的内容。
3. 密码、Token、密钥、证书内容禁止打印。
4. 异常日志中不要拼接完整请求体。
5. 死信消息展示接口必须做脱敏。
6. 日志平台和数据库中的敏感字段访问应受权限控制。
7. 安全认证配置中的 JAAS、密码和证书路径不应完整输出。

最终建议是：日志用于定位链路，数据库用于记录状态，监控用于发现异常，三者结合才能形成完整的 Kafka 可观测和可运维体系。

## 监控与指标

本章节用于说明 Kafka 项目中的监控指标设计，包括 Kafka Broker 指标、Producer 指标、Consumer 指标、Consumer Lag、消息堆积、消费失败、死信消息、Micrometer 集成、Prometheus 集成和 Grafana 面板设计。

Kafka 监控应覆盖三个层面：Kafka 集群侧、应用客户端侧和业务消息侧。Kafka 集群侧关注 Broker、Topic、Partition、ISR、磁盘和网络；应用客户端侧关注 Producer 发送成功率、发送耗时、Consumer 消费速率和 Consumer Lag；业务消息侧关注消费失败、重试、死信、幂等跳过和消息链路状态。

### Kafka Broker 指标

Kafka Broker 指标用于观察 Kafka 集群本身是否健康。Broker 层面的异常通常会直接影响 Producer 发送、Consumer 消费、Topic 分区可用性和消息可靠性。

Broker 监控重点如下：

| 指标类别                    | 监控内容                 | 说明                                        |
| --------------------------- | ------------------------ | ------------------------------------------- |
| Broker 存活                 | Broker 是否在线          | Broker 异常下线会影响分区 Leader 和副本同步 |
| Controller 状态             | 当前 Controller 是否稳定 | Controller 频繁切换通常表示集群不稳定       |
| Under Replicated Partitions | 未充分复制分区数量       | 大于 0 说明副本同步异常                     |
| Offline Partitions          | 离线分区数量             | 大于 0 说明部分分区不可用                   |
| ISR Shrink / Expand         | ISR 收缩和扩张频率       | ISR 频繁变化说明副本同步波动                |
| Request Handler 空闲率      | 请求处理线程空闲比例     | 长期过低说明 Broker 压力较高                |
| Network Processor 空闲率    | 网络线程空闲比例         | 长期过低说明网络处理压力较高                |
| 磁盘使用率                  | Kafka 日志目录磁盘占用   | 磁盘满会导致 Broker 不可用                  |
| Page Cache / IO             | 磁盘读写压力             | IO 压力高会影响读写延迟                     |
| Topic 分区数                | Broker 承载分区数量      | 分区过多会增加元数据和恢复成本              |

Broker 侧建议重点关注以下异常：

| 异常                                  | 风险                          |
| ------------------------------------- | ----------------------------- |
| Offline Partition 大于 0              | 对应分区无法读写              |
| Under Replicated Partition 持续大于 0 | 副本同步异常，可靠性下降      |
| Active Controller 数量不为 1          | Controller 状态异常           |
| Broker 磁盘使用率过高                 | 可能触发写入失败              |
| 请求延迟持续升高                      | Producer 和 Consumer 延迟增加 |
| ISR 频繁收缩                          | 集群复制链路不稳定            |

Broker 指标通常由 Kafka Exporter、JMX Exporter、云厂商 Kafka 监控或中间件平台采集。业务应用不应只依赖自身指标判断 Kafka 是否健康，必须结合 Broker 侧指标一起观察。

### Producer 指标

Producer 指标用于观察应用发送 Kafka 消息的情况，包括发送速率、发送耗时、发送失败、重试次数、批次大小、压缩率和缓冲区状态。Producer 指标异常会直接影响业务事件是否能及时写入 Kafka。

Producer 监控重点如下：

| 指标                   | 说明                                |
| ---------------------- | ----------------------------------- |
| 发送成功数量           | 成功写入 Kafka 的消息数量           |
| 发送失败数量           | 发送失败的消息数量                  |
| 发送失败率             | 失败数量 / 总发送数量               |
| 发送耗时               | Producer 从发送到 Broker 确认的耗时 |
| 发送重试次数           | Producer 自动重试次数               |
| Record Send Rate       | 每秒发送消息数                      |
| Record Error Rate      | 每秒发送失败数                      |
| Request Latency        | 请求 Broker 延迟                    |
| Batch Size             | 批次大小                            |
| Compression Rate       | 压缩效果                            |
| Buffer Available Bytes | Producer 可用缓冲区                 |
| Buffer Exhausted       | 缓冲区耗尽次数                      |

应用侧建议增加业务自定义指标：

| 指标名称                       | 类型    | 标签                                         |
| ------------------------------ | ------- | -------------------------------------------- |
| `kafka_producer_send_total`    | Counter | `cluster`、`topic`、`message_type`、`status` |
| `kafka_producer_send_seconds`  | Timer   | `cluster`、`topic`、`message_type`           |
| `kafka_producer_retry_total`   | Counter | `cluster`、`topic`、`message_type`           |
| `kafka_producer_failure_total` | Counter | `cluster`、`topic`、`exception`              |

Producer 发送成功日志和指标应同时存在。日志用于排查单条消息，指标用于发现整体趋势。

Producer 指标埋点示例：

文件位置：`src/main/java/io/github/atengk/kafka/monitor/KafkaProducerMetrics.java`

该组件用于记录 Producer 发送成功、发送失败和发送耗时指标。

```java
package io.github.atengk.kafka.monitor;

import cn.hutool.core.util.StrUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Kafka Producer 指标组件
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Component
@RequiredArgsConstructor
public class KafkaProducerMetrics {

    private final MeterRegistry meterRegistry;

    /**
     * 记录发送成功
     *
     * @param clusterName 集群名称
     * @param topic       Topic名称
     * @param messageType 消息类型
     * @param costMillis  耗时毫秒
     */
    public void recordSuccess(String clusterName, String topic, String messageType, long costMillis) {
        String cluster = StrUtil.blankToDefault(clusterName, "default");
        String type = StrUtil.blankToDefault(messageType, "UNKNOWN");

        meterRegistry.counter(
                "kafka_producer_send_total",
                "cluster", cluster,
                "topic", topic,
                "message_type", type,
                "status", "success"
        ).increment();

        Timer.builder("kafka_producer_send_seconds")
                .tag("cluster", cluster)
                .tag("topic", topic)
                .tag("message_type", type)
                .register(meterRegistry)
                .record(Duration.ofMillis(costMillis));
    }

    /**
     * 记录发送失败
     *
     * @param clusterName 集群名称
     * @param topic       Topic名称
     * @param messageType 消息类型
     * @param exception   异常类型
     */
    public void recordFailure(String clusterName, String topic, String messageType, String exception) {
        String cluster = StrUtil.blankToDefault(clusterName, "default");
        String type = StrUtil.blankToDefault(messageType, "UNKNOWN");
        String error = StrUtil.blankToDefault(exception, "UNKNOWN");

        meterRegistry.counter(
                "kafka_producer_send_total",
                "cluster", cluster,
                "topic", topic,
                "message_type", type,
                "status", "failure"
        ).increment();

        meterRegistry.counter(
                "kafka_producer_failure_total",
                "cluster", cluster,
                "topic", topic,
                "message_type", type,
                "exception", error
        ).increment();
    }

}
```

Producer 指标要求如下：

1. 每次发送成功和失败都应记录指标。
2. 指标标签应包含 `cluster`、`topic`、`message_type`。
3. 异常类型标签不应过细，避免标签基数过高。
4. 不要把 `messageId`、`key`、`traceId` 放入指标标签，避免指标爆炸。
5. 单条消息排查使用日志和数据库记录，不使用指标标签承载。

### Consumer 指标

Consumer 指标用于观察应用消费 Kafka 消息的速度、结果和耗时。Consumer 指标不仅要关注 Kafka 客户端自身状态，还要关注业务处理状态。

Consumer 监控重点如下：

| 指标                  | 说明                       |
| --------------------- | -------------------------- |
| 消费总数              | Consumer 处理的消息数量    |
| 消费成功数            | 业务处理成功数量           |
| 消费失败数            | 业务处理失败数量           |
| 消费失败率            | 失败数量 / 总消费数量      |
| 消费耗时              | 单条或批量消息处理耗时     |
| 重复消息数量          | 幂等跳过的消息数量         |
| 批量消费条数          | 批量监听一次处理的消息数量 |
| Poll Rate             | Consumer poll 频率         |
| Records Consumed Rate | 每秒消费消息数             |
| Commit Rate           | Offset 提交频率            |
| Rebalance 次数        | 消费组重平衡频率           |

应用侧建议增加业务自定义指标：

| 指标名称                         | 类型                   | 标签                                           |
| -------------------------------- | ---------------------- | ---------------------------------------------- |
| `kafka_consumer_consume_total`   | Counter                | `cluster`、`topic`、`consumer_group`、`status` |
| `kafka_consumer_consume_seconds` | Timer                  | `cluster`、`topic`、`consumer_group`           |
| `kafka_consumer_duplicate_total` | Counter                | `cluster`、`topic`、`consumer_group`           |
| `kafka_consumer_batch_size`      | Summary / Distribution | `cluster`、`topic`、`consumer_group`           |

Consumer 指标埋点示例：

文件位置：`src/main/java/io/github/atengk/kafka/monitor/KafkaConsumerMetrics.java`

该组件用于记录 Consumer 消费成功、失败、重复跳过和消费耗时指标。

```java
package io.github.atengk.kafka.monitor;

import cn.hutool.core.util.StrUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Kafka Consumer 指标组件
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Component
@RequiredArgsConstructor
public class KafkaConsumerMetrics {

    private final MeterRegistry meterRegistry;

    /**
     * 记录消费成功
     *
     * @param clusterName   集群名称
     * @param topic         Topic名称
     * @param consumerGroup 消费组
     * @param costMillis    消费耗时毫秒
     */
    public void recordSuccess(String clusterName, String topic, String consumerGroup, long costMillis) {
        String cluster = StrUtil.blankToDefault(clusterName, "default");
        String group = StrUtil.blankToDefault(consumerGroup, "UNKNOWN");

        meterRegistry.counter(
                "kafka_consumer_consume_total",
                "cluster", cluster,
                "topic", topic,
                "consumer_group", group,
                "status", "success"
        ).increment();

        Timer.builder("kafka_consumer_consume_seconds")
                .tag("cluster", cluster)
                .tag("topic", topic)
                .tag("consumer_group", group)
                .register(meterRegistry)
                .record(Duration.ofMillis(costMillis));
    }

    /**
     * 记录消费失败
     *
     * @param clusterName   集群名称
     * @param topic         Topic名称
     * @param consumerGroup 消费组
     * @param exception     异常类型
     */
    public void recordFailure(String clusterName, String topic, String consumerGroup, String exception) {
        String cluster = StrUtil.blankToDefault(clusterName, "default");
        String group = StrUtil.blankToDefault(consumerGroup, "UNKNOWN");
        String error = StrUtil.blankToDefault(exception, "UNKNOWN");

        meterRegistry.counter(
                "kafka_consumer_consume_total",
                "cluster", cluster,
                "topic", topic,
                "consumer_group", group,
                "status", "failure"
        ).increment();

        meterRegistry.counter(
                "kafka_consumer_failure_total",
                "cluster", cluster,
                "topic", topic,
                "consumer_group", group,
                "exception", error
        ).increment();
    }

    /**
     * 记录重复消息
     *
     * @param clusterName   集群名称
     * @param topic         Topic名称
     * @param consumerGroup 消费组
     */
    public void recordDuplicate(String clusterName, String topic, String consumerGroup) {
        meterRegistry.counter(
                "kafka_consumer_duplicate_total",
                "cluster", StrUtil.blankToDefault(clusterName, "default"),
                "topic", topic,
                "consumer_group", StrUtil.blankToDefault(consumerGroup, "UNKNOWN")
        ).increment();
    }

}
```

Consumer 指标要求如下：

1. 消费成功、失败、重复跳过都应记录。
2. 消费耗时应使用 Timer。
3. 批量消费应记录批次大小和失败条数。
4. 指标标签不要包含 `messageId`、`offset`、`key`。
5. 失败异常标签应按异常类型归类，避免标签过多。

### Consumer Lag 指标

Consumer Lag 是 Kafka 监控中最重要的消费进度指标之一。它表示 Consumer Group 当前提交 Offset 与 Topic 最新 Offset 之间的差距。Lag 越大，说明消费越滞后。

Consumer Lag 的含义如下：

```text
Consumer Lag = Log End Offset - Current Committed Offset
```

Lag 监控维度如下：

| 维度             | 说明         |
| ---------------- | ------------ |
| `cluster`        | Kafka 集群   |
| `topic`          | Topic 名称   |
| `consumer_group` | 消费组       |
| `partition`      | 分区         |
| `lag`            | 当前堆积数量 |

Lag 需要重点关注以下情况：

| 情况              | 说明                              |
| ----------------- | --------------------------------- |
| Lag 持续增长      | 消费速度低于生产速度              |
| Lag 突然暴涨      | Producer 流量突增或 Consumer 异常 |
| 单分区 Lag 异常高 | 分区热点或某个 Consumer 线程卡住  |
| 所有分区 Lag 都高 | 消费组整体处理能力不足            |
| Lag 长时间不下降  | Consumer 停止、异常重试或下游阻塞 |

Lag 监控建议如下：

1. 按 Topic + Consumer Group 维度聚合总 Lag。
2. 按 Partition 维度展示最大 Lag。
3. 同时观察消费速率和生产速率。
4. 对核心 Topic 设置更严格 Lag 阈值。
5. 对日志类 Topic 可使用更宽松阈值。
6. Lag 告警应结合持续时间，避免瞬时流量造成误报。

PromQL 示例：

```promql
sum(kafka_consumergroup_lag) by (consumergroup, topic)
```

按分区查看最大 Lag：

```promql
max(kafka_consumergroup_lag) by (consumergroup, topic, partition)
```

不同 exporter 暴露的指标名称可能不同，例如 Kafka Exporter、Burrow、云 Kafka 监控或自研采集器的指标命名不完全一致。实际项目中应以当前监控系统中的指标名称为准。

### 消息堆积监控

消息堆积监控用于判断 Consumer 是否处理不过来。消息堆积通常由流量突增、Consumer 宕机、消费异常阻塞、下游服务慢、数据库慢、重试过多或分区热点导致。

堆积判断不能只看 Lag 数量，还应结合增长速度和业务可接受延迟。例如一个日志 Topic 短时间堆积 10 万条可能可以接受，但支付成功 Topic 堆积几百条就可能影响业务。

堆积监控指标如下：

| 指标         | 说明                            |
| ------------ | ------------------------------- |
| 总 Lag       | Topic + Consumer Group 的总堆积 |
| 最大分区 Lag | 单个 Partition 最大堆积         |
| Lag 增长速率 | 单位时间内 Lag 增长量           |
| 消费速率     | 每秒消费消息数                  |
| 生产速率     | 每秒写入消息数                  |
| 预计清理时间 | Lag / 消费速率                  |
| 消费耗时     | 单条或批量处理耗时              |

预计清理时间计算方式：

```text
预计清理时间秒数 = 当前 Lag / 当前消费速率
```

PromQL 示例：

```promql
sum(kafka_consumergroup_lag) by (topic, consumergroup)
```

Lag 增长趋势：

```promql
increase(kafka_consumergroup_lag[5m])
```

消费速率示例：

```promql
sum(rate(kafka_consumer_consume_total{status="success"}[5m])) by (topic, consumer_group)
```

堆积监控要求如下：

1. 核心业务 Topic 必须配置 Lag 监控。
2. 堆积告警必须包含 Topic、Consumer Group 和当前 Lag。
3. 单分区热点要单独展示。
4. 堆积告警应结合持续时间，例如持续 5 分钟。
5. 堆积恢复也应通知，便于闭环。
6. 堆积严重时需要配合限流、扩容、暂停非核心消费或降级处理。

### 消费失败监控

消费失败监控用于发现业务处理异常、反序列化异常、下游服务异常和幂等处理异常。消费失败比 Lag 更直接反映业务处理质量。

消费失败指标如下：

| 指标                     | 说明                         |
| ------------------------ | ---------------------------- |
| 消费失败总数             | 失败消息数量                 |
| 消费失败率               | 失败数 / 消费总数            |
| 异常类型分布             | 按异常类统计失败             |
| 失败 Topic 分布          | 哪些 Topic 失败最多          |
| 失败 Consumer Group 分布 | 哪些消费组失败最多           |
| 连续失败次数             | 同一 Topic 或 Group 连续失败 |
| 失败重试次数             | 重试前后的失败情况           |

PromQL 示例：

```promql
sum(rate(kafka_consumer_failure_total[5m])) by (topic, consumer_group, exception)
```

消费失败率示例：

```promql
sum(rate(kafka_consumer_consume_total{status="failure"}[5m])) by (topic, consumer_group)
/
sum(rate(kafka_consumer_consume_total[5m])) by (topic, consumer_group)
```

消费失败监控要求如下：

1. 失败指标必须带 `topic` 和 `consumer_group` 标签。
2. 异常类型标签应归类，避免每种错误信息都成为标签。
3. 消费失败率比单纯失败数量更适合判断异常比例。
4. 对核心业务，任何持续失败都应告警。
5. 对非核心日志类消费，可按失败率和持续时间告警。
6. 失败监控应关联死信增长和重试次数。

### 死信消息监控

死信消息监控用于发现无法通过普通重试恢复的消息。死信增长通常表示消息格式错误、业务状态异常、下游长期不可用、版本不兼容或代码缺陷。

死信监控指标如下：

| 指标               | 说明                           |
| ------------------ | ------------------------------ |
| 死信新增数量       | 单位时间新增死信数量           |
| 死信累计未处理数量 | 当前待处理死信数量             |
| 死信重放数量       | 已重放死信数量                 |
| 死信重放失败数量   | 重放后仍失败数量               |
| 死信异常类型分布   | 按异常类型统计                 |
| 死信 Topic 分布    | 按原始 Topic 或死信 Topic 统计 |
| 死信最长未处理时间 | 最早待处理死信距当前时间       |

应用侧建议增加指标：

| 指标名称                         | 类型    | 标签                                                        |
| -------------------------------- | ------- | ----------------------------------------------------------- |
| `kafka_dead_letter_total`        | Counter | `cluster`、`source_topic`、`dead_letter_topic`、`exception` |
| `kafka_dead_letter_pending`      | Gauge   | `cluster`、`source_topic`、`process_status`                 |
| `kafka_dead_letter_replay_total` | Counter | `cluster`、`source_topic`、`status`                         |

死信指标组件示例：

文件位置：`src/main/java/io/github/atengk/kafka/monitor/KafkaDeadLetterMetrics.java`

该组件用于记录死信新增和死信重放结果指标。

```java
package io.github.atengk.kafka.monitor;

import cn.hutool.core.util.StrUtil;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Kafka 死信指标组件
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Component
@RequiredArgsConstructor
public class KafkaDeadLetterMetrics {

    private final MeterRegistry meterRegistry;

    /**
     * 记录死信消息
     *
     * @param clusterName     集群名称
     * @param sourceTopic     原始Topic
     * @param deadLetterTopic 死信Topic
     * @param exception       异常类型
     */
    public void recordDeadLetter(String clusterName, String sourceTopic, String deadLetterTopic, String exception) {
        meterRegistry.counter(
                "kafka_dead_letter_total",
                "cluster", StrUtil.blankToDefault(clusterName, "default"),
                "source_topic", sourceTopic,
                "dead_letter_topic", deadLetterTopic,
                "exception", StrUtil.blankToDefault(exception, "UNKNOWN")
        ).increment();
    }

    /**
     * 记录死信重放结果
     *
     * @param clusterName 集群名称
     * @param sourceTopic 原始Topic
     * @param status      重放状态
     */
    public void recordReplay(String clusterName, String sourceTopic, String status) {
        meterRegistry.counter(
                "kafka_dead_letter_replay_total",
                "cluster", StrUtil.blankToDefault(clusterName, "default"),
                "source_topic", sourceTopic,
                "status", StrUtil.blankToDefault(status, "UNKNOWN")
        ).increment();
    }

}
```

死信监控要求如下：

1. 死信新增必须告警。
2. 核心业务死信应按 P1 或 P2 处理。
3. 死信待处理数量必须定期统计。
4. 死信长时间未处理需要告警。
5. 死信重放失败需要单独告警。
6. 死信监控应关联死信表和死信 Topic。

### Micrometer 集成

Micrometer 是 Spring Boot 常用的指标门面。通过 Micrometer 可以将应用内 Producer、Consumer、死信、重试和业务处理指标统一暴露给 Prometheus。

Maven 依赖如下：

文件位置：`pom.xml`

以下依赖用于启用 Spring Boot Actuator 和 Prometheus 指标导出。

```xml
<dependencies>
    <!-- Spring Boot 监控端点 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Prometheus 指标导出 -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
</dependencies>
```

基础配置如下：

文件位置：`src/main/resources/application.yml`

该配置用于开放 Prometheus 指标采集端点。

```yaml
management:
  endpoints:
    web:
      exposure:
        # 暴露健康检查、基础信息和 Prometheus 指标
        include: health,info,prometheus

  endpoint:
    health:
      # 展示健康详情，生产环境可按权限要求调整
      show-details: when_authorized

  metrics:
    tags:
      # 所有指标默认带应用名标签
      application: ${spring.application.name}
```

访问指标端点：

```bash
curl http://localhost:8080/actuator/prometheus
```

该命令用于查看当前应用暴露给 Prometheus 的指标内容。如果返回中包含 `jvm_`、`process_`、`http_`、`kafka_` 或自定义 `kafka_producer_` 指标，说明指标端点已正常暴露。

Micrometer 使用要求如下：

1. 自定义指标命名必须统一。
2. 标签数量要可控，避免高基数标签。
3. 不要将 `messageId`、`traceId`、`offset` 放入指标标签。
4. 指标应覆盖成功、失败、耗时和堆积相关维度。
5. 与日志、数据库记录形成互补关系。

### Prometheus 集成

Prometheus 用于定期抓取 Spring Boot 应用暴露的 `/actuator/prometheus` 指标。生产环境中通常由运维平台统一配置 scrape 规则。

Prometheus 抓取配置示例：

文件位置：`prometheus.yml`

该配置用于让 Prometheus 抓取 Kafka 应用的指标。

```yaml
scrape_configs:
  - job_name: "kafka-develop-demo"
    metrics_path: "/actuator/prometheus"
    scrape_interval: 15s
    static_configs:
      - targets:
          # Kafka 应用实例地址
          - "kafka-develop-demo:8080"
```

Kubernetes ServiceMonitor 示例：

文件位置：`k8s/kafka-service-monitor.yml`

该配置用于 Prometheus Operator 通过 ServiceMonitor 抓取应用指标。

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: kafka-develop-demo-monitor
  labels:
    app: kafka-develop-demo
spec:
  selector:
    matchLabels:
      app: kafka-develop-demo
  endpoints:
    - port: http
      path: /actuator/prometheus
      interval: 15s
```

Prometheus 集成要求如下：

1. 应用必须暴露 `/actuator/prometheus`。
2. 生产环境应限制 actuator 访问范围。
3. 抓取周期建议 15 秒到 30 秒。
4. 多实例部署时指标应按 `instance` 区分。
5. 多集群 Kafka 应用指标必须带 `cluster` 标签。
6. 告警规则应统一放在监控配置仓库中管理。

### Grafana 面板设计

Grafana 面板用于展示 Kafka 消息链路的关键指标。面板应按使用者分层：开发关注异常和链路，运维关注 Broker 和资源，业务关注消息积压、失败和死信。

推荐面板分组如下：

| 面板分组      | 展示内容                                                     |
| ------------- | ------------------------------------------------------------ |
| Kafka 总览    | 集群状态、核心 Topic Lag、发送速率、消费速率、失败率         |
| Broker 面板   | Broker 存活、Under Replicated Partitions、Offline Partitions、磁盘、请求延迟 |
| Producer 面板 | 发送量、失败量、发送耗时、重试次数                           |
| Consumer 面板 | 消费量、消费耗时、失败率、重复消息                           |
| Lag 面板      | Topic + Consumer Group 总 Lag、最大分区 Lag、Lag 增长趋势    |
| 死信面板      | 死信新增、待处理死信、重放成功率、死信异常类型               |
| 重试面板      | 重试数量、重试失败、下次重试积压                             |
| 业务链路面板  | 按 messageType 展示发送、消费、失败、死信全链路              |

Grafana 关键图表建议：

| 图表                        | 类型             |
| --------------------------- | ---------------- |
| Producer 发送速率           | Time series      |
| Producer 发送失败率         | Time series      |
| Consumer 消费速率           | Time series      |
| Consumer 消费耗时 P95 / P99 | Time series      |
| Consumer Lag Top 10         | Bar gauge        |
| 死信新增趋势                | Time series      |
| 死信待处理数量              | Stat             |
| 重试失败趋势                | Time series      |
| Broker 异常分区数           | Stat             |
| Topic 分区 Lag 分布         | Heatmap 或 Table |

面板变量建议：

| 变量              | 说明           |
| ----------------- | -------------- |
| `$cluster`        | Kafka 集群     |
| `$topic`          | Topic          |
| `$consumer_group` | Consumer Group |
| `$application`    | 应用名         |
| `$instance`       | 应用实例       |
| `$message_type`   | 消息类型       |

Grafana 设计要求如下：

1. 总览页应优先展示异常指标。
2. 核心 Topic 单独建面板。
3. Lag 图表必须支持按 Topic 和 Consumer Group 过滤。
4. 死信面板必须展示待处理数量。
5. 失败率应同时展示数量和比例。
6. 面板应有跳转链接到日志平台或管理后台查询页面。
7. 不要在 Grafana 标签中展示敏感字段。

## 告警设计

本章节用于说明 Kafka 项目的告警设计，包括消息堆积告警、消费失败告警、死信增长告警、Broker 异常告警、Topic 分区异常告警、消费延迟告警、告警分级和告警降噪。

Kafka 告警设计应避免两个极端：一是告警太少，故障已经影响业务才发现；二是告警太多，导致开发和运维对告警麻木。告警应围绕业务影响程度设计，并配合持续时间、阈值、分级和抑制规则。

### 消息堆积告警

消息堆积告警用于发现 Consumer 消费能力不足或消费异常。核心指标是 Consumer Lag。

告警规则建议如下：

| 级别 | 条件                            | 说明               |
| ---- | ------------------------------- | ------------------ |
| P1   | 核心 Topic Lag 持续高于严重阈值 | 可能影响核心业务   |
| P2   | 普通业务 Topic Lag 持续增长     | 需要及时处理       |
| P3   | 非核心 Topic 短期堆积           | 观察或低优先级处理 |

Prometheus 告警规则示例：

```yaml
groups:
  - name: kafka-lag-alerts
    rules:
      - alert: KafkaConsumerLagHigh
        expr: sum(kafka_consumergroup_lag) by (topic, consumergroup) > 10000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Kafka消费堆积过高"
          description: "Topic={{ $labels.topic }}，ConsumerGroup={{ $labels.consumergroup }} 的Lag超过10000，持续5分钟"
```

核心 Topic 可以使用更严格阈值：

```yaml
groups:
  - name: kafka-core-lag-alerts
    rules:
      - alert: KafkaCoreConsumerLagCritical
        expr: sum(kafka_consumergroup_lag{topic=~"ateng.order.*|ateng.payment.*"}) by (topic, consumergroup) > 1000
        for: 3m
        labels:
          severity: critical
        annotations:
          summary: "核心Kafka Topic消费堆积严重"
          description: "核心Topic={{ $labels.topic }}，ConsumerGroup={{ $labels.consumergroup }} 的Lag超过1000，持续3分钟"
```

堆积告警要求如下：

1. 不同 Topic 使用不同阈值。
2. 核心业务 Topic 阈值应更低。
3. 告警必须包含 Topic 和 Consumer Group。
4. 告警应设置持续时间，避免瞬时峰值误报。
5. 应同时观察消费速率和 Lag 增长趋势。
6. 告警恢复应通知，便于闭环。

### 消费失败告警

消费失败告警用于发现业务处理异常、反序列化失败、下游异常和消费逻辑缺陷。

失败数量告警示例：

```yaml
groups:
  - name: kafka-consume-failure-alerts
    rules:
      - alert: KafkaConsumerFailureHigh
        expr: sum(rate(kafka_consumer_failure_total[5m])) by (topic, consumer_group) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Kafka消费失败率较高"
          description: "Topic={{ $labels.topic }}，ConsumerGroup={{ $labels.consumer_group }} 消费失败持续发生"
```

失败率告警示例：

```yaml
groups:
  - name: kafka-consume-failure-rate-alerts
    rules:
      - alert: KafkaConsumerFailureRateHigh
        expr: |
          sum(rate(kafka_consumer_consume_total{status="failure"}[5m])) by (topic, consumer_group)
          /
          sum(rate(kafka_consumer_consume_total[5m])) by (topic, consumer_group)
          > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Kafka消费失败率超过阈值"
          description: "Topic={{ $labels.topic }}，ConsumerGroup={{ $labels.consumer_group }} 消费失败率超过5%"
```

消费失败告警要求如下：

1. 核心 Topic 出现持续失败应立即告警。
2. 失败率告警比失败数量更适合高流量 Topic。
3. 低流量核心 Topic 可按失败数量告警。
4. 反序列化异常应单独告警。
5. 告警内容应包含异常类型 Top 列表或日志查询链接。
6. 消费失败告警应关联死信增长告警。

### 死信增长告警

死信增长告警用于发现无法通过重试恢复的消息。死信通常代表需要人工介入或代码修复。

死信新增告警示例：

```yaml
groups:
  - name: kafka-dead-letter-alerts
    rules:
      - alert: KafkaDeadLetterIncreased
        expr: sum(increase(kafka_dead_letter_total[5m])) by (source_topic, dead_letter_topic) > 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Kafka死信消息增长"
          description: "SourceTopic={{ $labels.source_topic }}，DeadLetterTopic={{ $labels.dead_letter_topic }} 5分钟内出现新增死信"
```

死信待处理数量告警示例：

```yaml
groups:
  - name: kafka-dead-letter-pending-alerts
    rules:
      - alert: KafkaDeadLetterPendingHigh
        expr: sum(kafka_dead_letter_pending{process_status="PENDING"}) by (source_topic) > 100
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Kafka待处理死信数量过高"
          description: "SourceTopic={{ $labels.source_topic }} 待处理死信数量超过100，持续10分钟"
```

死信告警要求如下：

1. 核心业务死信新增即告警。
2. 普通业务可按数量和持续时间告警。
3. 死信待处理数量必须监控。
4. 死信重放失败必须告警。
5. 告警应包含死信查询入口。
6. 死信告警不能长期无人处理，否则应升级告警级别。

### Broker 异常告警

Broker 异常告警用于发现 Kafka 集群层面的风险，例如 Broker 下线、Controller 异常、副本不同步、磁盘不足、请求延迟过高等。

Broker 告警项如下：

| 告警项                     | 条件                             |
| -------------------------- | -------------------------------- |
| Broker 下线                | Broker 存活数低于预期            |
| Offline Partition          | 离线分区数量大于 0               |
| Under Replicated Partition | 未充分复制分区数量持续大于 0     |
| Controller 异常            | Active Controller 数量不为 1     |
| 磁盘使用率过高             | 磁盘使用率超过 80% / 90%         |
| 请求延迟过高               | Produce / Fetch 请求延迟持续升高 |
| ISR 频繁收缩               | ISR Shrink 速率异常              |

Broker 异常告警要求如下：

1. Offline Partition 必须 P1 告警。
2. Under Replicated Partition 持续存在必须告警。
3. Broker 磁盘超过 80% 预警，超过 90% 严重告警。
4. Controller 频繁切换需要告警。
5. Broker 告警应由中间件平台或运维侧统一管理。
6. 应用侧告警需要与 Broker 告警联动，避免重复排查。

### Topic 分区异常告警

Topic 分区异常告警用于发现 Topic 分区不可用、Leader 缺失、副本不足、分区热点和分区 Lag 不均衡。

Topic 分区异常包括：

| 异常            | 说明                              |
| --------------- | --------------------------------- |
| 分区无 Leader   | 分区无法正常读写                  |
| 副本不足        | 副本数不符合预期                  |
| ISR 数量不足    | 同步副本数量低于要求              |
| 单分区 Lag 过高 | 可能存在热点 Key 或 Consumer 卡住 |
| 分区写入不均衡  | Key 设计不合理或分区策略异常      |
| 分区数配置错误  | 与项目规划不一致                  |

分区 Lag 不均衡 PromQL 示例：

```promql
max(kafka_consumergroup_lag) by (topic, consumergroup)
/
avg(kafka_consumergroup_lag) by (topic, consumergroup)
> 5
```

分区异常告警要求如下：

1. 分区无 Leader 必须严重告警。
2. ISR 不足应结合 Topic 重要性分级。
3. 单分区 Lag 异常高需要提示可能存在热点 Key。
4. 分区写入不均衡需要检查 Producer Key 设计。
5. 分区数变更应有审计和变更记录。
6. 分区异常应关联 Broker 指标一起分析。

### 消费延迟告警

消费延迟告警不同于 Lag 告警。Lag 关注堆积数量，消费延迟关注消息从发送到被消费完成的时间。对于核心业务，延迟时间通常比堆积数量更能反映用户影响。

消费延迟可以通过以下方式计算：

| 方式            | 说明                                     |
| --------------- | ---------------------------------------- |
| Kafka timestamp | 使用 `record.timestamp()` 与当前时间计算 |
| 消息体 sendTime | 使用 Producer 写入的 `sendTime`          |
| 消费记录表      | 用 `consume_end_time - send_time` 计算   |
| 自定义指标      | Consumer 消费完成时记录延迟 Timer        |

消费延迟指标建议：

| 指标名称                              | 类型            | 标签                                                 |
| ------------------------------------- | --------------- | ---------------------------------------------------- |
| `kafka_message_consume_delay_seconds` | Timer / Summary | `cluster`、`topic`、`consumer_group`、`message_type` |

消费延迟记录示例：

```java
long delayMillis = System.currentTimeMillis() - record.timestamp();

Timer.builder("kafka_message_consume_delay_seconds")
        .tag("topic", record.topic())
        .tag("consumer_group", consumerGroup)
        .register(meterRegistry)
        .record(Duration.ofMillis(delayMillis));
```

消费延迟告警示例：

```yaml
groups:
  - name: kafka-consume-delay-alerts
    rules:
      - alert: KafkaConsumeDelayHigh
        expr: histogram_quantile(0.95, sum(rate(kafka_message_consume_delay_seconds_bucket[5m])) by (le, topic, consumer_group)) > 60
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Kafka消息消费延迟过高"
          description: "Topic={{ $labels.topic }}，ConsumerGroup={{ $labels.consumer_group }} 的P95消费延迟超过60秒"
```

消费延迟告警要求如下：

1. 核心业务应配置 P95 或 P99 延迟告警。
2. 延迟告警应结合 Lag 告警分析。
3. 延迟计算应统一使用 Kafka timestamp 或消息体 sendTime。
4. 批量消费应记录批次中最大延迟。
5. 延迟过高时应检查消费耗时、Lag、下游服务和数据库性能。

### 告警分级

告警分级用于区分不同故障的处理优先级。不是所有 Kafka 告警都需要半夜电话通知，但核心业务不可用、消息丢失风险、Broker 分区不可用和死信暴增必须快速响应。

推荐分级如下：

| 级别 | 说明                                     | 示例                                                     |
| ---- | ---------------------------------------- | -------------------------------------------------------- |
| P1   | 严重故障，影响核心业务或存在数据丢失风险 | Offline Partition、核心 Topic 死信增长、核心消费完全停止 |
| P2   | 重要故障，可能影响业务时效性             | 核心 Topic Lag 持续升高、消费失败率高                    |
| P3   | 一般故障，需要工作时间处理               | 普通 Topic 堆积、非核心死信少量增长                      |
| P4   | 观察提醒                                 | 短时波动、低优先级延迟升高                               |

分级建议如下：

| 告警类型                            | 默认级别 |
| ----------------------------------- | -------- |
| Broker Offline                      | P1       |
| Offline Partition                   | P1       |
| Under Replicated Partition 持续存在 | P2       |
| 核心 Topic 死信新增                 | P1 / P2  |
| 普通 Topic 死信新增                 | P2 / P3  |
| 核心 Topic Lag 严重堆积             | P1 / P2  |
| 普通 Topic Lag 堆积                 | P3       |
| 消费失败率持续高                    | P2       |
| 消费延迟高                          | P2 / P3  |
| 磁盘使用率 80%                      | P3       |
| 磁盘使用率 90%                      | P1 / P2  |

告警分级要求如下：

1. 分级应结合业务重要性，不只看技术指标。
2. 核心 Topic 应维护清单。
3. 告警级别应和通知渠道绑定。
4. P1 告警必须有值班响应机制。
5. P2 告警必须有明确负责人。
6. P3 / P4 告警可以进入工作流或日报。

### 告警降噪

告警降噪用于减少重复告警、瞬时抖动告警和无行动价值告警。Kafka 指标容易受流量波峰、发布、Rebalance、Broker 短暂抖动影响，因此必须设计合理的持续时间、聚合规则和抑制规则。

常见降噪方式如下：

| 方式                | 说明                            |
| ------------------- | ------------------------------- |
| 设置 `for` 持续时间 | 避免瞬时波动告警                |
| 按 Topic 重要性分级 | 核心和非核心不同阈值            |
| 聚合相同告警        | 同一 Topic + Group 合并通知     |
| 设置恢复通知        | 告警恢复时闭环                  |
| 维护静默窗口        | 发布、扩容、演练期间临时静默    |
| 告警抑制            | Broker 故障时抑制应用侧衍生告警 |
| 设置最小流量条件    | 低流量下避免失败率误报          |
| 限制重复通知频率    | 避免同一故障刷屏                |

失败率告警增加最小流量条件示例：

```yaml
groups:
  - name: kafka-failure-rate-noise-reduction
    rules:
      - alert: KafkaConsumerFailureRateHighWithTraffic
        expr: |
          (
            sum(rate(kafka_consumer_consume_total{status="failure"}[5m])) by (topic, consumer_group)
            /
            sum(rate(kafka_consumer_consume_total[5m])) by (topic, consumer_group)
          ) > 0.05
          and
          sum(rate(kafka_consumer_consume_total[5m])) by (topic, consumer_group) > 10
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Kafka消费失败率较高"
          description: "Topic={{ $labels.topic }}，ConsumerGroup={{ $labels.consumer_group }} 消费失败率超过5%，且消费速率超过10条/秒"
```

降噪要求如下：

1. 告警必须可行动，不产生无意义通知。
2. 瞬时波动应使用持续时间过滤。
3. 发布期间可以设置临时静默，但必须有过期时间。
4. Broker 级故障发生时，应抑制由其引发的大量 Producer / Consumer 告警。
5. 对低流量 Topic，失败率告警必须结合失败数量。
6. 对高流量 Topic，数量告警应结合失败率。
7. 告警消息中应包含处理建议、日志链接或仪表盘链接。

最终建议是：Kafka 监控以 Broker 健康、Consumer Lag、消费失败、死信增长和消费延迟为核心；告警以核心业务影响为优先级；日志和数据库记录用于单条消息排查；Grafana 用于趋势观察；Prometheus 告警用于及时发现异常。

## 性能优化

本章节用于说明 Kafka 项目中的性能优化策略，包括 Producer 批量参数、压缩、Consumer 并发、Consumer 拉取参数、Partition 数量、消息大小、JVM、网络和数据库写入优化。性能优化应建立在可靠性、幂等性和监控完善的基础上，不能为了吞吐量牺牲消息可靠性和业务一致性。

Kafka 性能优化通常不是单个参数决定的，而是 Producer、Broker、Consumer、Topic、数据库、网络和业务处理共同作用的结果。优化前应先通过监控定位瓶颈，再针对性调整参数，最后通过压测验证效果。

### Producer 批量参数优化

Producer 批量参数用于控制 Kafka 客户端如何将多条消息聚合成批次发送。合理的批量参数可以显著提升吞吐量，降低网络请求次数，但会带来一定发送延迟。

推荐关注以下参数：

| 参数                  | 说明                                   | 优化方向                         |
| --------------------- | -------------------------------------- | -------------------------------- |
| `batch.size`          | 单个 Partition 批次大小                | 增大可提升吞吐，但会增加内存占用 |
| `linger.ms`           | 等待更多消息组成批次的时间             | 增大可提升批量效果，但会增加延迟 |
| `buffer.memory`       | Producer 总缓冲区大小                  | 大流量场景可适当增大             |
| `delivery.timeout.ms` | 单条消息完整发送生命周期超时           | 需要大于请求超时和批量等待时间   |
| `max.block.ms`        | 发送时等待元数据或缓冲区可用的最大时间 | 缓冲区不足时影响业务线程         |

通用业务推荐配置如下：

```yaml
spring:
  kafka:
    producer:
      properties:
        # 单个批次大小，普通业务可从 32KB 开始压测
        batch.size: 32768

        # 批量等待时间，兼顾吞吐和延迟
        linger.ms: 20

        # Producer 总缓冲区大小，默认通常为 32MB
        buffer.memory: 33554432

        # 消息完整发送生命周期超时时间
        delivery.timeout.ms: 120000

        # 业务线程等待缓冲区或元数据的最大时间
        max.block.ms: 60000
```

高吞吐日志类场景可以适当提高批量参数：

```yaml
spring:
  kafka:
    producer:
      properties:
        # 日志类消息可以适当增大批次
        batch.size: 65536

        # 吞吐优先时可以增加等待时间
        linger.ms: 50

        # 大流量场景增加缓冲区
        buffer.memory: 67108864

        # 配合压缩提升吞吐
        compression.type: lz4
```

优化建议如下：

1. 接口强实时场景不宜设置过大的 `linger.ms`。
2. 日志、埋点、数据同步类消息可以适当增大 `batch.size` 和 `linger.ms`。
3. `batch.size` 不是越大越好，消息量不足时批次可能无法填满。
4. `buffer.memory` 过小可能导致业务线程阻塞。
5. 调优后必须观察发送耗时、失败率、缓冲区等待和 Broker 压力。

### Producer 压缩优化

Producer 压缩用于减少网络传输量和 Broker 存储成本。Kafka 支持 `none`、`gzip`、`snappy`、`lz4`、`zstd` 等压缩方式。压缩可以提升整体吞吐，但会增加 Producer 和 Consumer 的 CPU 消耗。

推荐配置如下：

```yaml
spring:
  kafka:
    producer:
      properties:
        # 通用业务推荐 lz4，兼顾压缩率和性能
        compression.type: lz4
```

压缩方式选择建议如下：

| 压缩方式 | 特点                     | 适用场景               |
| -------- | ------------------------ | ---------------------- |
| `none`   | 无压缩，CPU 消耗最低     | 小流量、极低延迟       |
| `gzip`   | 压缩率较高，CPU 消耗较高 | 带宽敏感、吞吐中等     |
| `snappy` | 压缩和解压速度较快       | 通用场景               |
| `lz4`    | 低延迟、高性能           | 推荐大多数业务使用     |
| `zstd`   | 压缩率和性能均衡         | 大流量场景，需压测验证 |

压缩优化要求如下：

1. JSON 消息字段重复较多，通常压缩收益明显。
2. 大流量 Topic 建议开启压缩。
3. CPU 已经较高的应用需要谨慎开启高压缩率算法。
4. 压缩前后应对比网络流量、发送延迟、CPU 和 Broker 磁盘占用。
5. 不同 Topic 可以采用不同压缩策略，不需要全局统一。

### Consumer 并发优化

Consumer 并发优化用于提升消费吞吐能力。Spring Kafka 中可以通过 `concurrency` 启动多个消费线程，但同一个 Consumer Group 内的最大有效并发通常不会超过 Topic 分区数。

配置示例：

```yaml
spring:
  kafka:
    listener:
      # 默认监听并发数
      concurrency: 3
```

监听器单独配置并发：

```java
@KafkaListener(
        topics = KafkaTopicConstant.ORDER_CREATED_TOPIC,
        groupId = KafkaGroupConstant.ORDER_CONSUMER_GROUP,
        concurrency = "6",
        containerFactory = "kafkaListenerContainerFactory"
)
public void consume(ConsumerRecord<String, Object> record, Acknowledgment acknowledgment) {
    // 消费处理
}
```

并发规划建议如下：

| Topic 分区数 | 应用实例数 | 每实例并发数 | 最大有效并发 |
| ------------ | ---------- | ------------ | ------------ |
| 3            | 1          | 3            | 3            |
| 6            | 2          | 3            | 6            |
| 12           | 3          | 4            | 12           |
| 24           | 4          | 6            | 24           |

Consumer 并发优化要求如下：

1. 并发数不应长期大于 Partition 数过多。
2. 提升 Consumer 并发前，应确认数据库和下游服务能承载更高并发。
3. 顺序消息场景不能随意增加内部异步并发。
4. 消费耗时较长时，需要调大 `max.poll.interval.ms`。
5. 扩容 Consumer 实例会触发 Rebalance，应关注重平衡耗时和消费抖动。
6. 并发优化后必须观察 Consumer Lag 是否下降。

### Consumer 拉取参数优化

Consumer 拉取参数用于控制每次从 Broker 拉取多少消息、等待多久、最多拉取多少数据。合理配置可以降低网络请求次数并提升吞吐。

推荐关注以下参数：

| 参数                        | 说明                          | 优化方向                  |
| --------------------------- | ----------------------------- | ------------------------- |
| `max.poll.records`          | 单次 poll 最大消息数          | 批量消费可适当增大        |
| `fetch.min.bytes`           | Broker 返回数据的最小字节数   | 吞吐优先可适当增大        |
| `fetch.max.wait.ms`         | Broker 等待凑够数据的最大时间 | 与 `fetch.min.bytes` 配合 |
| `max.partition.fetch.bytes` | 单个分区单次拉取最大数据量    | 大消息场景需要调大        |
| `max.poll.interval.ms`      | 两次 poll 最大间隔            | 业务处理慢时必须调大      |

普通业务配置示例：

```yaml
spring:
  kafka:
    consumer:
      properties:
        # 单次拉取最大消息数量
        max.poll.records: 100

        # 两次 poll 最大间隔
        max.poll.interval.ms: 300000

        # 单分区拉取最大字节数
        max.partition.fetch.bytes: 1048576
```

高吞吐批量消费配置示例：

```yaml
spring:
  kafka:
    consumer:
      properties:
        # 批量消费场景可适当增大
        max.poll.records: 500

        # 拉取更多数据后返回，减少空拉取
        fetch.min.bytes: 1048576

        # 等待数据聚合的最大时间
        fetch.max.wait.ms: 500

        # 批量处理耗时更长，需要增大 poll 间隔
        max.poll.interval.ms: 600000
```

优化建议如下：

1. `max.poll.records` 增大后，单次业务处理耗时也会增加。
2. 如果处理耗时超过 `max.poll.interval.ms`，Consumer 会被踢出消费组并触发 Rebalance。
3. 批量消费时，应确保整批消息能在 `max.poll.interval.ms` 内处理完。
4. 大消息场景需要调整 `max.partition.fetch.bytes`。
5. 拉取参数调优必须结合消费耗时、Lag 和业务失败率观察。

### Partition 数量优化

Partition 数量决定 Topic 的并行能力，也影响 Broker 元数据管理、文件句柄、Leader 选举、Consumer Rebalance 和顺序性。Partition 数量过少会限制吞吐，过多会增加集群负担。

Partition 数量规划因素如下：

| 因素          | 说明                               |
| ------------- | ---------------------------------- |
| Producer 吞吐 | 分区越多，写入并行能力越强         |
| Consumer 并发 | 同一消费组最大有效并发受分区数限制 |
| Broker 数量   | 分区应均匀分布在 Broker 上         |
| 顺序性要求    | 同一业务 Key 需要进入同一分区      |
| 消息保留时间  | 分区越多，日志文件越多             |
| 后续扩容      | 增加分区可能改变 Key 路由          |

分区数量估算方式：

```text
分区数 >= 目标消费并发数
分区数 >= 目标吞吐量 / 单分区可承载吞吐量
```

规划建议如下：

| 场景           | 建议分区数       |
| -------------- | ---------------- |
| 本地开发       | 1 到 3           |
| 普通业务事件   | 3 到 6           |
| 中等吞吐业务   | 6 到 12          |
| 高吞吐日志采集 | 12 到 48，需压测 |
| 严格全局顺序   | 1                |

Partition 优化要求如下：

1. 不要盲目创建大量分区。
2. 增加分区前需要评估 Key 顺序性影响。
3. 分区热点通常说明 Key 设计不均匀。
4. 单分区 Lag 过高时，应检查是否存在热点 Key。
5. 分区数量调整属于 Topic 级变更，生产环境需要审批和回滚方案。
6. 分区增加后不能直接减少，减少通常需要新建 Topic 并迁移。

### 消息大小优化

消息大小直接影响 Producer 发送耗时、Broker 网络和磁盘、Consumer 拉取耗时、反序列化成本和数据库落库成本。Kafka 适合传递事件消息，不适合作为大文件或大对象传输系统。

消息体设计建议如下：

| 优化项       | 说明                                     |
| ------------ | ---------------------------------------- |
| 只传必要字段 | 不传完整对象快照                         |
| 不传大文件   | 文件应存对象存储，消息只传 URL 或文件 ID |
| 不传大数组   | 大批量数据应拆分                         |
| 字段命名简洁 | JSON 字段名过长会增加体积                |
| 开启压缩     | JSON 重复字段压缩收益较好                |
| 避免嵌套过深 | 降低解析成本                             |
| 脱敏后传输   | 避免敏感大字段进入 Kafka                 |

不推荐消息示例：

```json
{
  "orderId": "10001",
  "userSnapshot": {
    "name": "张三",
    "phone": "13800008000",
    "addressList": [
      {
        "province": "xx",
        "city": "xx",
        "detail": "很长的地址内容"
      }
    ]
  },
  "productSnapshotList": [
    {
      "skuId": "SKU001",
      "description": "大量商品描述..."
    }
  ]
}
```

推荐消息示例：

```json
{
  "messageId": "1900000000000000001",
  "messageType": "ORDER_CREATED",
  "bizKey": "ORDER:10001",
  "payload": {
    "orderId": "10001",
    "userId": "20001",
    "amount": "199.90"
  }
}
```

消息大小优化要求如下：

1. 单条消息尽量控制在较小范围内，例如几十 KB 以内。
2. 大文件、大图片、大报表不应直接写入 Kafka。
3. 大批量数据应拆分为多条消息或通过文件引用传递。
4. 如果确实需要大消息，需要同步调整 Producer、Broker、Consumer 限制，并进行压测。
5. 消息越大，失败重试和死信存储成本越高。

### JVM 参数优化

Kafka 应用的 JVM 优化主要关注 GC、堆内存、线程数、对象分配和序列化成本。Producer 和 Consumer 都会产生一定缓冲对象、反序列化对象和业务对象，如果消息量较大，JVM 配置不合理会导致频繁 GC 或延迟抖动。

基础 JVM 参数示例：

```bash
java \
  -Xms1024m \
  -Xmx1024m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/data/logs/heapdump.hprof \
  -jar kafka-develop-demo.jar
```

参数说明如下：

| 参数                         | 说明               |
| ---------------------------- | ------------------ |
| `-Xms`                       | 初始堆内存         |
| `-Xmx`                       | 最大堆内存         |
| `-XX:+UseG1GC`               | 使用 G1 垃圾收集器 |
| `-XX:MaxGCPauseMillis`       | GC 停顿目标        |
| `HeapDumpOnOutOfMemoryError` | OOM 时生成堆转储   |
| `HeapDumpPath`               | 堆转储文件路径     |

JVM 优化建议如下：

1. 生产环境建议 `-Xms` 和 `-Xmx` 设置一致。
2. 高吞吐 Consumer 需要关注反序列化对象创建和批量处理对象堆积。
3. 批量消费时，不要一次拉取过多导致瞬时内存压力过大。
4. 日志打印大对象会增加内存和 CPU 消耗。
5. 需要监控 JVM Heap、GC Pause、线程数和 Direct Memory。
6. 如果使用容器部署，应结合容器内存限制设置 JVM 参数。

### 网络参数优化

Kafka 对网络较敏感。Producer 发送、Consumer 拉取、Broker 副本同步都依赖网络。网络延迟、丢包、带宽不足或 DNS 异常都会影响 Kafka 性能和稳定性。

客户端网络相关参数如下：

| 参数                      | 说明                           |
| ------------------------- | ------------------------------ |
| `request.timeout.ms`      | 请求等待 Broker 响应的超时时间 |
| `delivery.timeout.ms`     | Producer 消息发送总超时时间    |
| `connections.max.idle.ms` | 空闲连接关闭时间               |
| `reconnect.backoff.ms`    | 重连退避时间                   |
| `retry.backoff.ms`        | 重试退避时间                   |
| `receive.buffer.bytes`    | Socket 接收缓冲区              |
| `send.buffer.bytes`       | Socket 发送缓冲区              |

配置示例：

```yaml
spring:
  kafka:
    producer:
      properties:
        # 单次请求超时时间
        request.timeout.ms: 30000

        # 重试退避时间
        retry.backoff.ms: 100

        # 重连退避时间
        reconnect.backoff.ms: 100

    consumer:
      properties:
        # Consumer 请求超时时间
        request.timeout.ms: 30000

        # Socket 接收缓冲区，-1 表示使用系统默认
        receive.buffer.bytes: -1

        # Socket 发送缓冲区，-1 表示使用系统默认
        send.buffer.bytes: -1
```

网络优化建议如下：

1. 应用和 Kafka Broker 尽量部署在同地域或同可用区。
2. 跨地域访问 Kafka 会显著增加延迟，不适合高吞吐核心链路。
3. Docker 或 Kubernetes 环境需要确认 `advertised.listeners` 配置正确。
4. TLS 加密会增加 CPU 和握手成本，需要压测评估。
5. 网络抖动时应观察 Producer 重试、请求延迟和 Consumer Lag。
6. 不要使用不稳定 DNS 或短 TTL 导致频繁解析异常。

### 数据库写入优化

Consumer 业务处理通常会写数据库。Kafka 消费能力提升后，数据库往往成为瓶颈。数据库写入优化是 Consumer 性能优化的重要部分。

数据库写入优化方向如下：

| 优化项           | 说明                               |
| ---------------- | ---------------------------------- |
| 批量插入         | 批量消费场景使用批量写库           |
| 唯一索引设计     | 幂等依赖唯一索引，但索引不能过多   |
| 减少事务范围     | 事务只包裹必要业务操作             |
| 避免逐条远程调用 | 批量接口或异步处理                 |
| 分库分表         | 大数据量消费记录可按时间或业务分表 |
| 异步落审计       | 非核心审计可异步写入               |
| 连接池优化       | 调整最大连接数、超时和监控         |
| 慢 SQL 优化      | 消费耗时高时优先排查 SQL           |

批量写入建议流程：

```text
批量拉取消息
  -> 单条幂等过滤
  -> 组装待写入列表
  -> 批量写数据库
  -> 批量更新消费状态
  -> 提交 Offset
```

数据库优化要求如下：

1. Kafka 消费并发不能超过数据库承载能力。
2. 幂等表唯一索引必须保留，但非必要索引应减少。
3. 高频写入表应考虑按月分表或归档。
4. 批量消费时应避免每条消息单独提交事务。
5. 消费失败不能因为数据库异常被吞掉。
6. 数据库连接池耗尽会直接导致 Consumer 堆积，应接入监控。

## 容错与高可用

本章节用于说明 Kafka 项目的容错和高可用设计，包括 Broker 高可用、Topic 副本机制、Producer 失败重试、Consumer 自动再均衡、消费者实例扩缩容、应用优雅停机、异常恢复策略和降级策略。

Kafka 高可用不是只依赖集群部署，还需要 Producer、Consumer、Topic、应用实例、数据库和下游服务共同设计。高可用目标是：Broker 故障时消息不丢，Producer 异常时可重试，Consumer 异常时可恢复，应用发布时不造成大量重复消费或消息堆积。

### Broker 高可用

Broker 高可用依赖 Kafka 集群多 Broker 部署、Controller 管理、Partition Leader 选举和副本同步机制。生产环境不建议使用单 Broker Kafka。

Broker 高可用建议如下：

| 项目        | 建议                                                         |
| ----------- | ------------------------------------------------------------ |
| Broker 数量 | 生产环境至少 3 个                                            |
| Controller  | 使用 Kafka 集群机制保证 Controller 高可用                    |
| 机架感知    | 跨机架、跨可用区时配置 rack awareness                        |
| 磁盘        | Kafka 日志目录使用可靠磁盘并监控容量                         |
| 网络        | Broker 之间网络稳定、低延迟                                  |
| 监控        | Broker 存活、Offline Partition、Under Replicated Partition 必须监控 |

Broker 故障影响如下：

| 故障            | 影响                            |
| --------------- | ------------------------------- |
| 单 Broker 下线  | 其上的 Leader 分区需要重新选主  |
| 多 Broker 下线  | 可能导致分区不可用              |
| 磁盘满          | Broker 可能无法继续写入         |
| 网络隔离        | ISR 收缩，Producer 写入可能失败 |
| Controller 异常 | 分区管理和选主受影响            |

应用侧应配置多个 Bootstrap Servers：

```yaml
spring:
  kafka:
    # 生产环境配置多个Broker地址，避免单点入口失败
    bootstrap-servers: kafka-01:9092,kafka-02:9092,kafka-03:9092
```

Broker 高可用要求如下：

1. 生产环境必须多 Broker 部署。
2. 业务应用必须配置多个 Broker 地址。
3. 核心 Topic 必须配置多副本。
4. Broker 异常应通过监控平台告警。
5. Kafka 集群运维应有扩容、替换、磁盘清理和故障恢复流程。

### Topic 副本机制

Topic 副本机制用于保证 Partition 数据在多个 Broker 上有副本。当 Leader 副本所在 Broker 故障时，可以从 ISR 中选举新的 Leader。

生产推荐配置如下：

| 配置项                           | 推荐值  | 说明                                   |
| -------------------------------- | ------- | -------------------------------------- |
| `replication.factor`             | `3`     | 每个 Partition 三个副本                |
| `min.insync.replicas`            | `2`     | 至少两个同步副本                       |
| Producer `acks`                  | `all`   | 等待同步副本确认                       |
| `unclean.leader.election.enable` | `false` | 避免非同步副本成为 Leader 导致数据丢失 |

Topic 创建示例：

```bash
kafka-topics.sh \
  --bootstrap-server kafka-01:9092 \
  --create \
  --topic ateng.order.created.topic \
  --partitions 6 \
  --replication-factor 3 \
  --config min.insync.replicas=2
```

Topic 副本设计要求如下：

1. 副本数不能大于 Broker 数量。
2. 核心业务 Topic 不应使用单副本。
3. `replication.factor=3` 应配合 `min.insync.replicas=2` 和 `acks=all`。
4. 本地单节点环境只能使用副本数 `1`。
5. Under Replicated Partition 持续存在时应立即排查。
6. 副本分布应尽量均衡，避免集中在少数 Broker。

### Producer 失败重试

Producer 失败重试用于处理发送阶段的临时异常，例如 Broker Leader 切换、网络抖动、请求超时等。Producer 重试只能解决消息写入 Kafka 之前的问题，不能解决 Consumer 业务处理失败。

推荐配置如下：

```yaml
spring:
  kafka:
    producer:
      # 等待同步副本确认
      acks: all

      # 发送失败自动重试
      retries: 3

      properties:
        # 开启幂等发送
        enable.idempotence: true

        # 幂等发送要求不超过5
        max.in.flight.requests.per.connection: 5

        # 发送总超时时间
        delivery.timeout.ms: 120000

        # 请求超时时间
        request.timeout.ms: 30000

        # 重试退避时间
        retry.backoff.ms: 100
```

Producer 失败处理流程建议：

```text
业务产生消息
  -> Producer 发送 Kafka
  -> 成功，记录 partition 和 offset
  -> 失败，Producer 自动重试
  -> 超过超时时间仍失败
  -> 记录发送失败
  -> 写入本地消息表或补偿任务
  -> 定时任务继续补偿发送
```

Producer 容错要求如下：

1. 核心业务必须开启 `acks=all`。
2. 核心业务建议开启 `enable.idempotence=true`。
3. 最终发送失败必须记录，不允许静默丢弃。
4. 发送失败补偿应有最大重试次数和告警。
5. 序列化失败、认证失败、权限不足通常不可重试，应直接告警。
6. 写数据库加发 Kafka 的场景推荐使用本地消息表保证最终一致性。

### Consumer 自动再均衡

Consumer 自动再均衡是 Kafka Consumer Group 的核心能力。当消费者实例加入、离开、宕机、超时或订阅 Topic 分区变化时，Kafka 会重新分配 Partition。

Rebalance 常见触发原因如下：

| 原因              | 说明                        |
| ----------------- | --------------------------- |
| Consumer 实例启动 | 新实例加入消费组            |
| Consumer 实例停止 | 实例离开消费组              |
| Consumer 心跳超时 | Broker 认为实例失效         |
| 处理时间过长      | 超过 `max.poll.interval.ms` |
| Topic 分区变化    | 分区数增加                  |
| 网络抖动          | 心跳异常导致再均衡          |

Rebalance 风险如下：

| 风险         | 说明                                     |
| ------------ | ---------------------------------------- |
| 消费短暂停顿 | 分区重新分配期间消费暂停                 |
| 重复消费     | 已处理但未提交 Offset 的消息会重新消费   |
| 延迟升高     | 大消费组 Rebalance 时间较长              |
| 顺序影响     | 分区切换实例后需要依赖 Offset 和幂等保护 |

Consumer 配置建议：

```yaml
spring:
  kafka:
    consumer:
      properties:
        # 单次处理耗时不能超过该值
        max.poll.interval.ms: 300000

        # 会话超时时间
        session.timeout.ms: 45000

        # 心跳间隔
        heartbeat.interval.ms: 15000
```

自动再均衡要求如下：

1. 消费逻辑必须具备幂等能力。
2. 业务处理时间不能超过 `max.poll.interval.ms`。
3. 发布和扩缩容会触发 Rebalance，应避开高峰期。
4. 批量消费需要控制单批处理时间。
5. Rebalance 频繁发生时，应检查 GC、网络、处理耗时和实例稳定性。
6. 对延迟敏感业务，应尽量减少频繁扩缩容。

### 消费者实例扩缩容

消费者实例扩缩容用于根据消息堆积和消费压力调整 Consumer 处理能力。扩容可以提升消费能力，但最大有效消费并发受 Topic Partition 数限制。

扩容判断依据如下：

| 指标                  | 说明                 |
| --------------------- | -------------------- |
| Consumer Lag 持续增长 | 消费速度低于生产速度 |
| 消费耗时升高          | 单条处理变慢         |
| CPU 使用率            | 应用处理能力是否耗尽 |
| 数据库压力            | 下游是否成为瓶颈     |
| 分区数                | 是否支持更多并发     |
| 失败率                | 是否因为异常导致堆积 |

扩容原则如下：

```text
最大有效消费者线程数 <= Topic Partition 数
```

扩容示例：

| 分区数 | 当前实例 | 每实例并发 | 当前有效并发 | 可扩容空间       |
| ------ | -------- | ---------- | ------------ | ---------------- |
| 6      | 2        | 2          | 4            | 可扩到 6         |
| 6      | 3        | 2          | 6            | 再扩实例效果有限 |
| 12     | 3        | 3          | 9            | 可扩到 12        |
| 12     | 6        | 3          | 12           | 部分线程可能空闲 |

Kubernetes 扩容示例：

```bash
kubectl scale deployment kafka-consumer-demo --replicas=4
```

该命令用于将消费者应用扩容到 4 个实例。扩容后应观察 Consumer Group Rebalance、Consumer Lag、消费失败率和下游数据库压力。

扩缩容要求如下：

1. 扩容前确认 Topic 分区数是否足够。
2. 扩容后观察 Rebalance 是否正常结束。
3. 扩容可能增加数据库和下游接口压力。
4. 缩容前应优雅停止实例，避免处理中消息被中断。
5. 自动扩缩容应使用 Lag、CPU、消费耗时等多指标综合判断。
6. 不建议仅基于 CPU 自动扩容 Kafka Consumer。

### 应用优雅停机

应用优雅停机用于保证服务发布、重启、缩容时，正在处理的 Kafka 消息尽量完成处理并提交 Offset，避免大量重复消费或业务处理中断。

Spring Boot 优雅停机配置示例：

```yaml
server:
  # 开启优雅停机
  shutdown: graceful

spring:
  lifecycle:
    # 优雅停机等待时间
    timeout-per-shutdown-phase: 30s
```

Kafka 监听容器停机管理示例：

文件位置：`src/main/java/io/github/atengk/kafka/lifecycle/KafkaGracefulShutdownHandler.java`

该组件用于在应用关闭前停止 Kafka 监听容器，减少关闭过程中继续拉取新消息的情况。

```java
package io.github.atengk.kafka.lifecycle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Component;

/**
 * Kafka 优雅停机处理器
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaGracefulShutdownHandler implements ApplicationListener<ContextClosedEvent> {

    private final KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    /**
     * 应用关闭时停止 Kafka 监听容器
     *
     * @param event Spring容器关闭事件
     */
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        log.info("应用准备关闭，开始停止Kafka监听容器");

        kafkaListenerEndpointRegistry.getListenerContainers().forEach(container -> {
            log.info("停止Kafka监听容器，listenerId={}", container.getListenerId());
            container.stop();
        });

        log.info("Kafka监听容器停止完成");
    }

}
```

Kubernetes 优雅停机建议：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kafka-consumer-demo
spec:
  template:
    spec:
      terminationGracePeriodSeconds: 60
      containers:
        - name: kafka-consumer-demo
          image: kafka-consumer-demo:1.0.0
          lifecycle:
            preStop:
              exec:
                command:
                  # 预留时间让服务从注册中心摘除并停止接收新流量
                  - /bin/sh
                  - -c
                  - "sleep 10"
```

优雅停机要求如下：

1. 收到关闭信号后，应停止拉取新消息。
2. 已经开始处理的消息应尽量处理完成。
3. 处理成功后再提交 Offset。
4. 关闭等待时间应大于单条或单批消息的常规处理时间。
5. 批量消费场景需要设置更长的优雅停机时间。
6. 发布时应观察 Consumer Lag 和消费失败率。

### 异常恢复策略

异常恢复策略用于在 Kafka、应用、数据库、下游服务或网络异常后恢复消息处理。恢复策略必须结合消息状态、重试记录、死信表和幂等记录设计。

常见异常和恢复方式如下：

| 异常              | 恢复策略                               |
| ----------------- | -------------------------------------- |
| Producer 发送失败 | 本地消息表补偿发送                     |
| Consumer 处理失败 | 阻塞重试、Retry Topic、死信            |
| Consumer 宕机     | 依赖 Consumer Group 重新分配 Partition |
| Offset 提交失败   | 依赖幂等处理重复消息                   |
| 数据库短暂异常    | Consumer 重试或延迟重试                |
| 下游服务不可用    | 熔断、降级、延迟重试                   |
| 反序列化失败      | 进入死信，修复消息模型                 |
| Broker 短暂不可用 | Producer 重试，Consumer 自动恢复       |
| 大量死信          | 暂停消费、修复数据、批量重放           |

异常恢复流程建议：

```text
发现异常
  -> 判断异常范围：Producer / Consumer / Broker / DB / 下游
  -> 判断是否影响核心业务
  -> 暂停或降级非核心链路
  -> 修复根因
  -> 观察 Kafka Lag 和失败率
  -> 处理重试记录和死信消息
  -> 验证业务数据一致性
```

恢复策略要求如下：

1. 所有失败消息必须有最终状态。
2. 重试不能无限循环。
3. 死信必须可查询、可重放、可忽略。
4. 恢复后应优先处理核心业务 Topic。
5. 重放消息前必须确认幂等保护有效。
6. 异常恢复过程必须记录操作日志和审计日志。

### 降级策略

降级策略用于在 Kafka 或下游系统异常时，保护核心业务链路。降级不是直接丢消息，而是在可接受范围内暂缓、隔离或跳过非核心处理。

常见降级方式如下：

| 降级方式                 | 说明                               |
| ------------------------ | ---------------------------------- |
| 非核心消息暂停发送       | 暂停日志、埋点、通知类消息         |
| 非核心 Consumer 暂停消费 | 释放数据库和下游压力               |
| 写本地消息表             | Kafka 不可用时先落库，后续补偿发送 |
| 延迟重试                 | 下游不可用时延后处理               |
| 死信隔离                 | 多次失败后进入死信，避免阻塞主链路 |
| 限流                     | 限制消息发送或消费速率             |
| 熔断                     | 下游持续失败时短时间停止调用       |
| 降级为最终一致           | 主流程先成功，后续异步补偿         |

降级场景示例：

| 场景             | 降级策略                                   |
| ---------------- | ------------------------------------------ |
| Kafka 短暂不可用 | 核心业务写本地消息表，非核心消息丢弃或延迟 |
| 通知服务不可用   | 消费失败进入延迟重试，不影响订单主流程     |
| 积分服务异常     | 订单成功，积分发放后续补偿                 |
| 数据统计服务慢   | 暂停统计 Consumer 或降低并发               |
| 死信暴增         | 暂停相关 Consumer，修复后重放              |
| 数据库压力过高   | 降低 Consumer 并发或暂停非核心 Topic       |

降级配置示例：

```yaml
app:
  kafka:
    degrade:
      # 是否启用 Kafka 降级开关
      enabled: false

      # Kafka 发送失败时是否写入本地消息表
      write-outbox-on-send-failure: true

      # 非核心消息是否允许丢弃
      allow-drop-non-core-message: false

      # Consumer 异常时是否进入死信
      enable-dead-letter: true
```

降级判断服务示例：

文件位置：`src/main/java/io/github/atengk/kafka/degrade/KafkaDegradeService.java`

该服务用于判断当前是否启用 Kafka 降级，以及非核心消息是否允许跳过。

```java
package io.github.atengk.kafka.degrade;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Kafka 降级服务
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
public class KafkaDegradeService {

    /**
     * 判断是否启用降级
     *
     * @return true表示启用降级
     */
    public boolean isDegradeEnabled() {
        // 实际项目中可以从配置中心、数据库或开关平台读取
        return false;
    }

    /**
     * 判断非核心消息是否允许跳过
     *
     * @param messageType 消息类型
     * @return true表示允许跳过
     */
    public boolean allowSkipNonCoreMessage(String messageType) {
        boolean allowSkip = isDegradeEnabled() && isNonCoreMessage(messageType);
        if (allowSkip) {
            log.warn("Kafka降级生效，非核心消息允许跳过，messageType={}", messageType);
        }
        return allowSkip;
    }

    /**
     * 判断是否为非核心消息
     *
     * @param messageType 消息类型
     * @return true表示非核心消息
     */
    private boolean isNonCoreMessage(String messageType) {
        return "USER_OPERATE_LOG".equals(messageType)
                || "STATISTICS_EVENT".equals(messageType)
                || "NOTIFY_EVENT".equals(messageType);
    }

}
```

降级策略要求如下：

1. 核心业务消息不应直接丢弃。
2. 非核心消息是否可丢弃必须由业务确认。
3. 降级开关必须可观测、可审计。
4. 降级期间产生的本地消息或失败任务必须后续补偿。
5. 降级恢复后应观察 Lag、失败率、死信和业务数据一致性。
6. 降级策略应提前演练，不应故障时临时设计。

## 再均衡机制

本章节用于说明 Kafka Consumer Group 的再均衡机制，包括 Rebalance 触发场景、Rebalance 影响、Partition Assignment 策略、Cooperative Sticky 策略、Rebalance 监听器、再均衡期间 Offset 处理和降低 Rebalance 频率的方法。

Rebalance 是 Kafka Consumer Group 自动协调分区归属的机制。当消费组成员变化、订阅 Topic 分区变化或消费者心跳异常时，Kafka 会重新分配 Partition。Rebalance 可以让消费者实例自动扩缩容，但也会带来短暂停顿、重复消费和消费延迟抖动。因此，Kafka Consumer 必须按照“Rebalance 随时可能发生”的前提设计幂等、Offset 提交和优雅停机。

### Rebalance 触发场景

Rebalance 触发场景用于识别消费组为什么会重新分配 Partition。频繁 Rebalance 通常不是正常现象，可能表示消费者处理过慢、应用频繁发布、网络抖动、GC 停顿或参数配置不合理。

常见触发场景如下：

| 触发场景            | 说明                                                   |
| ------------------- | ------------------------------------------------------ |
| Consumer 实例启动   | 新实例加入 Consumer Group，需要重新分配 Partition      |
| Consumer 实例关闭   | 原实例离开 Consumer Group，其负责的 Partition 需要转移 |
| Consumer 心跳超时   | Broker 长时间未收到心跳，认为消费者失效                |
| 消费处理超时        | 两次 `poll` 间隔超过 `max.poll.interval.ms`            |
| Topic 分区数变化    | Topic 增加 Partition 后，消费组需要重新分配            |
| 订阅 Topic 变化     | Consumer 订阅列表变化                                  |
| 网络抖动            | Consumer 与 Broker 通信异常                            |
| JVM 长时间 GC       | 心跳或 poll 被阻塞，导致被踢出消费组                   |
| Kubernetes 滚动发布 | Pod 重启、扩缩容触发消费组成员变化                     |

相关配置如下：

```yaml
spring:
  kafka:
    consumer:
      properties:
        # Consumer会话超时时间，超过后Broker认为该Consumer失效
        session.timeout.ms: 45000

        # Consumer心跳间隔，通常小于session.timeout.ms
        heartbeat.interval.ms: 15000

        # 两次poll最大间隔，业务处理不能长期超过该值
        max.poll.interval.ms: 300000
```

配置说明如下：

| 参数                    | 说明                                |
| ----------------------- | ----------------------------------- |
| `session.timeout.ms`    | Consumer 与 Broker 会话超时时间     |
| `heartbeat.interval.ms` | Consumer 心跳发送间隔               |
| `max.poll.interval.ms`  | Consumer 两次调用 `poll` 的最大间隔 |
| `max.poll.records`      | 单次拉取记录数，间接影响处理时间    |

如果业务处理耗时经常接近或超过 `max.poll.interval.ms`，应优先优化消费逻辑、降低 `max.poll.records`、增加处理能力，或者将长耗时任务改为异步任务表处理，而不是简单无限调大参数。

### Rebalance 影响

Rebalance 影响主要体现在消费暂停、重复消费、延迟抖动和状态迁移。Kafka 应用必须明确这些影响，并通过幂等、手动提交 Offset、优雅停机和合理参数减少业务风险。

常见影响如下：

| 影响               | 说明                                             |
| ------------------ | ------------------------------------------------ |
| 消费短暂停顿       | Rebalance 期间，部分 Partition 暂停消费          |
| 重复消费           | 已处理但未提交 Offset 的消息可能重新消费         |
| 消费延迟升高       | Rebalance 期间 Lag 可能增长                      |
| Partition 归属变化 | 原实例处理的分区可能分配给其他实例               |
| 本地缓存失效       | 基于本地缓存的分区状态可能不再准确               |
| 顺序处理受影响     | 分区切换后，仍需依赖 Offset 和业务幂等保证正确性 |
| 批量处理中断       | 正在处理的批次如果未完成，可能被重新消费         |

典型重复消费场景如下：

```text
Consumer 拉取消息
  -> 业务处理成功
  -> 还未提交 Offset
  -> 发生 Rebalance
  -> Partition 转移到其他 Consumer
  -> 新 Consumer 从上次已提交 Offset 继续消费
  -> 同一消息再次被处理
```

因此，Rebalance 场景下必须满足以下要求：

1. Consumer 业务处理必须幂等。
2. 业务成功后尽快提交 Offset。
3. 失败消息不要伪造成功提交。
4. 处理时间不要超过 `max.poll.interval.ms`。
5. 发布和扩缩容应尽量避免高峰期。
6. 批量消费需要控制单批处理量和处理耗时。

### Partition Assignment 策略

Partition Assignment 策略用于决定 Consumer Group 中的 Consumer 如何分配 Topic Partition。不同分配策略对分区均衡、稳定性和 Rebalance 成本有不同影响。

常见分配策略如下：

| 策略                      | 类名                                                         | 特点                              |
| ------------------------- | ------------------------------------------------------------ | --------------------------------- |
| RangeAssignor             | `org.apache.kafka.clients.consumer.RangeAssignor`            | 按 Topic 范围分配，可能出现不均衡 |
| RoundRobinAssignor        | `org.apache.kafka.clients.consumer.RoundRobinAssignor`       | 跨 Topic 轮询分配，整体更均衡     |
| StickyAssignor            | `org.apache.kafka.clients.consumer.StickyAssignor`           | 尽量保持原分配结果，降低迁移      |
| CooperativeStickyAssignor | `org.apache.kafka.clients.consumer.CooperativeStickyAssignor` | 协作式增量再均衡，减少停顿        |

配置示例：

```yaml
spring:
  kafka:
    consumer:
      properties:
        # 使用协作式Sticky分配策略，减少全量Rebalance影响
        partition.assignment.strategy: org.apache.kafka.clients.consumer.CooperativeStickyAssignor
```

策略选择建议如下：

| 场景                                     | 推荐策略                                            |
| ---------------------------------------- | --------------------------------------------------- |
| 普通单 Topic 消费                        | `CooperativeStickyAssignor`                         |
| 多 Topic 消费且分区数差异较大            | `RoundRobinAssignor` 或 `CooperativeStickyAssignor` |
| 希望降低分区迁移                         | `StickyAssignor` 或 `CooperativeStickyAssignor`     |
| 新项目 Spring Boot 3 + 较新 Kafka 客户端 | 优先 `CooperativeStickyAssignor`                    |

Assignment 策略要求如下：

1. 同一个 Consumer Group 内建议使用一致的分配策略。
2. 修改分配策略会触发 Rebalance。
3. 多实例部署时，配置必须保持一致。
4. 对延迟敏感的消费组，应优先选择迁移较少的策略。
5. 策略调整后应观察 Rebalance 次数、Lag 和消费延迟。

### Cooperative Sticky 策略

Cooperative Sticky 策略是一种协作式增量 Rebalance 策略。它尽量避免所有 Consumer 一次性撤销全部 Partition，而是分阶段迁移必要的 Partition，从而降低 Rebalance 对消费的影响。

配置方式如下：

```yaml
spring:
  kafka:
    consumer:
      properties:
        # 协作式增量分配策略
        partition.assignment.strategy: org.apache.kafka.clients.consumer.CooperativeStickyAssignor
```

Cooperative Sticky 的优势如下：

| 优势         | 说明                             |
| ------------ | -------------------------------- |
| 降低消费停顿 | 不必每次全量撤销所有 Partition   |
| 降低分区迁移 | 尽量保持原有分配结果             |
| 适合滚动发布 | 发布过程中消费组抖动更小         |
| 适合扩缩容   | 新实例加入时只迁移必要 Partition |
| 对延迟更友好 | 减少 Rebalance 导致的 Lag 波动   |

使用注意事项如下：

1. 消费组内所有 Consumer 应使用相同分配策略。
2. 从旧策略迁移到 Cooperative Sticky 时，应规划发布顺序。
3. 如果混用不同策略，可能导致消费组异常或 Rebalance 不稳定。
4. Cooperative Sticky 不能消除 Rebalance，只能降低影响。
5. 业务幂等和 Offset 正确处理仍然必须保留。

适合场景如下：

```text
多实例 Consumer
  -> 经常滚动发布
  -> 对消费停顿敏感
  -> 希望降低 Rebalance 影响
  -> 使用 CooperativeStickyAssignor
```

### Rebalance 监听器

Rebalance 监听器用于在 Partition 被撤销或分配时执行自定义逻辑。常见用途包括记录日志、提交 Offset、清理本地缓存、加载分区状态和监控 Rebalance 事件。

文件位置：`src/main/java/io/github/atengk/kafka/config/KafkaRebalanceConfig.java`

该配置类用于给 Kafka 监听容器设置 ConsumerRebalanceListener，记录分区撤销和分配事件。

```java
package io.github.atengk.kafka.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.Collection;

/**
 * Kafka Rebalance 监听配置
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Configuration
public class KafkaRebalanceConfig {

    /**
     * 配置带 Rebalance 监听器的 Kafka 监听容器工厂
     *
     * @param consumerFactory 消费者工厂
     * @return Kafka 监听容器工厂
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaRebalanceListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.getContainerProperties().setConsumerRebalanceListener(consumerRebalanceListener());

        log.info("初始化Kafka Rebalance监听容器工厂，ackMode=MANUAL_IMMEDIATE，concurrency=3");
        return factory;
    }

    /**
     * 创建 Rebalance 监听器
     *
     * @return Rebalance 监听器
     */
    @Bean
    public ConsumerRebalanceListener consumerRebalanceListener() {
        return new ConsumerRebalanceListener() {

            /**
             * 分区撤销前回调
             *
             * @param partitions 被撤销的分区
             */
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                log.warn("Kafka分区即将被撤销，partitions={}", partitions);

                // 可在此处清理本地分区缓存、记录状态或做必要的资源释放
            }

            /**
             * 分区分配后回调
             *
             * @param partitions 新分配的分区
             */
            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                log.info("Kafka分区已分配，partitions={}", partitions);

                // 可在此处加载分区级状态或记录Rebalance完成事件
            }
        };
    }

}
```

监听器使用示例：

```java
@KafkaListener(
        topics = KafkaTopicConstant.ORDER_CREATED_TOPIC,
        groupId = KafkaGroupConstant.ORDER_CONSUMER_GROUP,
        containerFactory = "kafkaRebalanceListenerContainerFactory"
)
public void consume(ConsumerRecord<String, Object> record, Acknowledgment acknowledgment) {
    try {
        log.info("消费Kafka消息，topic={}，partition={}，offset={}，key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        acknowledgment.acknowledge();
    } catch (Exception e) {
        log.error("Kafka消息消费失败，topic={}，partition={}，offset={}，key={}",
                record.topic(), record.partition(), record.offset(), record.key(), e);
        throw e;
    }
}
```

Rebalance 监听器使用要求如下：

1. 回调逻辑必须轻量，不能执行长耗时操作。
2. 不要在回调中调用慢接口或复杂数据库操作。
3. 撤销分区时可以清理本地缓存或记录日志。
4. 分配分区时可以加载必要状态。
5. Offset 提交应优先通过正常消费流程完成，不建议在回调中做复杂提交逻辑。
6. Rebalance 日志必须包含分区列表，便于排查分区迁移。

### 再均衡期间 Offset 处理

再均衡期间 Offset 处理是避免重复消费和消息丢失的关键。Rebalance 发生时，如果消息已处理但 Offset 未提交，新消费者可能会重新消费这部分消息；如果消息未处理完成却提前提交 Offset，则可能导致消息丢失。

推荐处理原则如下：

| 原则             | 说明                               |
| ---------------- | ---------------------------------- |
| 业务成功后再提交 | 避免消息未处理却提交 Offset        |
| 失败不提交       | 交给错误处理器重试或死信           |
| 提交必须尽快     | 减少 Rebalance 时重复窗口          |
| 幂等兜底         | 允许重复消费但不重复产生业务副作用 |
| 批量处理谨慎提交 | 整批成功或失败项可靠记录后再提交   |

推荐消费流程：

```text
poll 消息
  -> 读取 messageId
  -> 幂等检查
  -> 业务处理
  -> 更新消费状态 SUCCESS
  -> acknowledgment.acknowledge()
  -> Rebalance 时即使重复消费，也可通过幂等跳过
```

代码示例：

```java
try {
    // 1. 幂等检查
    // 2. 业务处理
    // 3. 更新消费记录
    acknowledgment.acknowledge();
} catch (Exception e) {
    // 不提交Offset，抛出异常由错误处理器接管
    throw e;
}
```

再均衡期间 Offset 处理要求如下：

1. 不要开启自动提交处理重要业务消息。
2. 手动提交应放在业务处理成功之后。
3. 批量消费时，提交 Offset 前必须确认失败项已经可靠处理。
4. Rebalance 可能导致重复消费，Consumer 必须幂等。
5. 应监控 Offset 提交失败和 Rebalance 次数。
6. 应用发布前后应观察 Lag 和重复消费日志。

### 降低 Rebalance 频率

降低 Rebalance 频率用于减少消费暂停、延迟抖动和重复消费窗口。频繁 Rebalance 通常说明消费组不稳定，需要从应用生命周期、消费耗时、JVM、网络和配置参数上排查。

优化方向如下：

| 优化项                  | 说明                             |
| ----------------------- | -------------------------------- |
| 减少频繁发布            | 频繁滚动重启会触发消费组成员变化 |
| 控制单批处理时间        | 避免超过 `max.poll.interval.ms`  |
| 降低 `max.poll.records` | 单批处理过慢时减少拉取条数       |
| 优化业务逻辑            | 避免消费线程执行长耗时操作       |
| 优化 GC                 | 避免长时间 Stop-The-World        |
| 使用 Cooperative Sticky | 降低 Rebalance 停顿              |
| 开启优雅停机            | 停机前完成处理中消息             |
| 稳定网络                | 减少心跳异常                     |
| 合理设置心跳参数        | 避免误判 Consumer 失效           |

配置建议：

```yaml
spring:
  kafka:
    consumer:
      properties:
        # 使用协作式Sticky分配策略
        partition.assignment.strategy: org.apache.kafka.clients.consumer.CooperativeStickyAssignor

        # 会话超时时间
        session.timeout.ms: 45000

        # 心跳间隔
        heartbeat.interval.ms: 15000

        # 最大poll间隔，批量消费或慢业务需要适当调大
        max.poll.interval.ms: 300000

        # 控制单次拉取数量，避免单批处理过慢
        max.poll.records: 100
```

降低 Rebalance 频率的要求如下：

1. 消费逻辑应避免长时间阻塞。
2. 批量消费应控制单批数量。
3. JVM GC 指标必须纳入监控。
4. 发布应使用滚动发布和优雅停机。
5. 消费组实例不要频繁扩缩容。
6. 频繁 Rebalance 应作为告警或监控项展示。

## 测试设计

本章节用于说明 Kafka 项目的测试设计，包括单元测试、Producer 测试、Consumer 测试、Embedded Kafka 测试、Testcontainers Kafka 测试、异常重试测试、死信 Topic 测试、幂等消费测试和性能压测。

Kafka 测试不应只覆盖“能发送、能消费”的正向流程，还需要覆盖异常重试、死信投递、重复消费、Offset 提交、幂等保护、批量消费和性能边界。测试环境建议分层：普通业务逻辑用单元测试，Kafka 组件集成用 Embedded Kafka 或 Testcontainers，端到端环境用真实 Kafka 测试集群。

### 单元测试

单元测试用于验证与 Kafka 无关或弱相关的纯业务逻辑，例如消息模型构建、幂等键生成、Header 解析、状态转换、异常分类和参数校验。单元测试不依赖真实 Kafka，执行速度快，适合在每次构建时运行。

适合单元测试的内容如下：

| 测试对象     | 测试内容                                  |
| ------------ | ----------------------------------------- |
| 消息构建器   | messageId、version、sendTime 是否正确生成 |
| Header 工具  | Header 读取、缺失字段处理                 |
| 幂等键生成   | messageId + consumerGroup 组合是否稳定    |
| 异常分类     | 可重试异常和不可重试异常是否识别正确      |
| Topic 常量   | Topic 命名规范校验                        |
| Payload 转换 | Map / JSON 转业务对象                     |

文件位置：`src/test/java/io/github/atengk/kafka/support/KafkaHeaderSupportTest.java`

该测试类用于验证 Kafka Header 工具类能正确读取 Header，并兼容缺失字段。

```java
package io.github.atengk.kafka.support;

import io.github.atengk.kafka.constant.KafkaHeaderConstant;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

/**
 * Kafka Header 工具测试
 *
 * @author Ateng
 * @since 2026-05-11
 */
class KafkaHeaderSupportTest {

    /**
     * 测试读取存在的Header
     */
    @Test
    void shouldReadHeaderValue() {
        RecordHeaders headers = new RecordHeaders();
        headers.add(KafkaHeaderConstant.MESSAGE_ID, "1900000000000000001".getBytes(StandardCharsets.UTF_8));

        String messageId = KafkaHeaderSupport.getString(headers, KafkaHeaderConstant.MESSAGE_ID);

        Assertions.assertEquals("1900000000000000001", messageId);
    }

    /**
     * 测试Header不存在时返回null
     */
    @Test
    void shouldReturnNullWhenHeaderMissing() {
        RecordHeaders headers = new RecordHeaders();

        String messageId = KafkaHeaderSupport.getString(headers, KafkaHeaderConstant.MESSAGE_ID);

        Assertions.assertNull(messageId);
    }

}
```

单元测试要求如下：

1. 不依赖真实 Kafka。
2. 只测试单个类或单个方法的行为。
3. 覆盖空值、异常值和边界值。
4. 幂等、Header、消息构建器等工具类必须有单元测试。
5. 单元测试应在 CI 中快速执行。

### Producer 测试

Producer 测试用于验证消息是否能正确发送，包括 Topic、Key、Header、Payload、发送成功回调、发送失败处理和发送记录更新。Producer 测试可以分为 Mock 测试和集成测试。

Mock 测试适合验证封装逻辑，不依赖真实 Kafka。

文件位置：`src/test/java/io/github/atengk/kafka/producer/DefaultKafkaMessageProducerTest.java`

该测试类用于验证 Producer 封装组件会校验请求参数，并在 Topic 为空时抛出异常。

```java
package io.github.atengk.kafka.producer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * 默认 Kafka Producer 测试
 *
 * @author Ateng
 * @since 2026-05-11
 */
class DefaultKafkaMessageProducerTest {

    /**
     * 测试Topic为空时抛出异常
     */
    @Test
    void shouldThrowExceptionWhenTopicBlank() {
        KafkaTemplate<String, Object> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        DefaultKafkaMessageProducer producer = new DefaultKafkaMessageProducer(kafkaTemplate);

        KafkaSendRequest<String> request = KafkaSendRequest.<String>builder()
                .topic("")
                .key("ORDER:10001")
                .payload("test-message")
                .build();

        Assertions.assertThrows(KafkaSendException.class, () -> producer.sendAsync(request));
    }

}
```

Producer 集成测试应验证以下内容：

| 测试点          | 说明                                           |
| --------------- | ---------------------------------------------- |
| 指定 Topic 发送 | 消息写入目标 Topic                             |
| 指定 Key 发送   | Consumer 能读取到正确 Key                      |
| Header 设置     | messageId、traceId、messageType 正确写入       |
| 发送失败        | Topic 不存在、认证失败或序列化失败时能记录异常 |
| 发送结果        | partition、offset 能正确返回                   |
| 批量发送        | 多条消息发送后能全部消费                       |

Producer 测试要求如下：

1. 封装层参数校验使用单元测试。
2. Kafka 写入验证使用 Embedded Kafka 或 Testcontainers。
3. 发送结果必须验证 Topic、Partition、Offset。
4. Header 必须验证。
5. 失败场景必须覆盖。

### Consumer 测试

Consumer 测试用于验证消息监听、业务处理、Header 读取、Key 读取、手动提交 Offset、异常抛出和幂等跳过等逻辑。Consumer 测试比 Producer 更复杂，因为它涉及异步监听和 Offset 管理。

Consumer 测试重点如下：

| 测试点       | 说明                          |
| ------------ | ----------------------------- |
| 正常消费     | 收到消息后执行业务逻辑        |
| Header 读取  | 能正确读取 messageId、traceId |
| Key 读取     | 能正确读取业务 Key            |
| 手动 Ack     | 业务成功后提交 Offset         |
| 消费异常     | 业务异常时抛出异常            |
| 重复消息     | 已处理 messageId 能跳过       |
| 批量消费     | 批量消息处理结果正确          |
| 消费状态记录 | 成功、失败状态正确落库        |

Consumer 业务逻辑应尽量从 `@KafkaListener` 中拆出独立 Service，这样可以先对 Service 做单元测试，再用集成测试验证监听链路。

推荐结构：

```text
consumer
├── OrderCreatedConsumer.java
└── OrderCreatedConsumeService.java
```

测试建议如下：

1. 业务逻辑用单元测试。
2. Kafka 监听链路用集成测试。
3. 消费测试必须设置超时时间，避免 CI 卡死。
4. 异步断言可以使用 Awaitility。
5. Consumer 测试应使用独立 Topic 和独立 Consumer Group，避免互相干扰。

### Embedded Kafka 测试

Embedded Kafka 是 Spring Kafka 提供的嵌入式 Kafka 测试能力，适合在单元或集成测试中快速启动 Kafka 环境。它不依赖 Docker，适合验证 Producer、Consumer 和 Listener 逻辑。

Maven 依赖如下：

文件位置：`pom.xml`

以下依赖用于启用 Spring Kafka 测试工具。

```xml
<dependencies>
    <!-- Spring Boot 测试基础依赖，包含JUnit 5和Spring Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Spring Kafka 测试工具，包含Embedded Kafka支持 -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

文件位置：`src/test/java/io/github/atengk/kafka/integration/EmbeddedKafkaProducerTest.java`

该测试类使用 Embedded Kafka 验证 KafkaTemplate 可以正常发送和消费消息。

```java
package io.github.atengk.kafka.integration;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

/**
 * Embedded Kafka Producer 测试
 *
 * @author Ateng
 * @since 2026-05-11
 */
@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = "ateng.test.embedded.topic",
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:0",
                "auto.create.topics.enable=true"
        }
)
class EmbeddedKafkaProducerTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    /**
     * 测试发送并消费消息
     */
    @Test
    void shouldSendAndReceiveMessage() {
        String topic = "ateng.test.embedded.topic";
        String key = "TEST:10001";
        String value = "hello-kafka";

        kafkaTemplate.send(topic, key, value);

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "ateng-test-group",
                "true",
                embeddedKafkaBroker
        );

        Consumer<String, Object> consumer = new DefaultKafkaConsumerFactory<String, Object>(consumerProps)
                .createConsumer();

        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, topic);

        ConsumerRecord<String, Object> record = KafkaTestUtils.getSingleRecord(consumer, topic);

        Assertions.assertEquals(key, record.key());
        Assertions.assertEquals(value, record.value());

        consumer.close();
    }

}
```

Embedded Kafka 使用要求如下：

1. 测试 Topic 应使用独立名称。
2. Consumer Group 应使用测试专用名称。
3. 测试结束后关闭 Consumer。
4. 不适合验证复杂生产部署配置，例如 SASL_SSL、ACL、多 Broker 真实网络。
5. 如果需要更接近真实环境，使用 Testcontainers。

### Testcontainers Kafka 测试

Testcontainers Kafka 测试用于在 Docker 中启动真实 Kafka 容器，更接近生产运行环境。它适合验证 Spring Boot 与 Kafka 的端到端集成、序列化配置、网络配置、Topic 初始化和多容器交互。

Maven 依赖如下：

文件位置：`pom.xml`

以下依赖用于启用 Testcontainers Kafka 测试。

```xml
<dependencies>
    <!-- Testcontainers JUnit 5支持 -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Testcontainers Kafka模块 -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>kafka</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

文件位置：`src/test/java/io/github/atengk/kafka/integration/TestcontainersKafkaTest.java`

该测试类使用 Testcontainers 启动 Kafka 容器，并通过 `DynamicPropertySource` 将 Kafka 地址注入 Spring Boot。

```java
package io.github.atengk.kafka.integration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers Kafka 集成测试
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Testcontainers
@SpringBootTest
class TestcontainersKafkaTest {

    @Container
    static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1")
    );

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 动态注入 Kafka Broker 地址
     *
     * @param registry 动态属性注册器
     */
    @DynamicPropertySource
    static void registerKafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
    }

    /**
     * 测试Kafka容器启动和KafkaTemplate加载
     */
    @Test
    void shouldStartKafkaContainerAndLoadKafkaTemplate() {
        Assertions.assertTrue(KAFKA_CONTAINER.isRunning());
        Assertions.assertNotNull(kafkaTemplate);
    }

}
```

Testcontainers 使用要求如下：

1. 本地和 CI 环境必须可用 Docker。
2. 镜像版本应与测试目标 Kafka 版本尽量接近。
3. 测试启动速度慢于 Embedded Kafka，适合集成测试阶段。
4. 每个测试类应使用独立 Topic，避免并发测试互相影响。
5. CI 中应配置 Docker 缓存，降低拉取镜像耗时。

### 异常重试测试

异常重试测试用于验证 Consumer 处理失败后是否按照配置进行重试，并在超过最大重试次数后进入死信或失败记录。该测试应覆盖可重试异常和不可重试异常。

测试内容如下：

| 测试点       | 说明                     |
| ------------ | ------------------------ |
| 可重试异常   | 抛出可重试异常后触发重试 |
| 不可重试异常 | 直接进入死信或失败处理   |
| 最大重试次数 | 达到上限后停止重试       |
| 重试间隔     | 重试间隔符合配置         |
| 重试日志     | 每次重试有日志和指标     |
| 重试记录     | 数据库重试次数正确更新   |

异常重试测试建议流程：

```text
发送测试消息
  -> Consumer 人为抛出 RetryableBusinessException
  -> 等待重试
  -> 验证重试次数
  -> 验证最终进入死信或失败表
```

重试测试 Consumer 示例：

文件位置：`src/test/java/io/github/atengk/kafka/testsupport/RetryTestConsumer.java`

该测试消费者用于模拟消费失败，方便验证重试次数。

```java
package io.github.atengk.kafka.testsupport;

import io.github.atengk.kafka.exception.RetryableBusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Kafka 重试测试消费者
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class RetryTestConsumer {

    private final AtomicInteger consumeCount = new AtomicInteger();

    /**
     * 模拟消费失败
     *
     * @param record Kafka消息记录
     */
    @KafkaListener(
            topics = "ateng.test.retry.topic",
            groupId = "ateng.test.retry.group"
    )
    public void consume(ConsumerRecord<String, Object> record) {
        int currentCount = consumeCount.incrementAndGet();
        log.warn("执行Kafka重试测试消费，count={}，topic={}，offset={}",
                currentCount, record.topic(), record.offset());

        throw new RetryableBusinessException("模拟可重试异常");
    }

    /**
     * 获取消费次数
     *
     * @return 消费次数
     */
    public int getConsumeCount() {
        return consumeCount.get();
    }

}
```

异常重试测试要求如下：

1. 测试中应使用较短重试间隔，避免测试耗时过长。
2. 可重试和不可重试异常都要覆盖。
3. 重试次数断言应考虑首次消费不属于重试次数。
4. 最终失败后应验证死信或失败记录。
5. 异步测试应设置最大等待时间，避免测试卡死。

### 死信 Topic 测试

死信 Topic 测试用于验证消息超过最大重试次数或遇到不可重试异常后，是否被正确投递到死信 Topic，并保留原始 Key、Header 和消息体。

测试内容如下：

| 测试点          | 说明                              |
| --------------- | --------------------------------- |
| 死信投递        | 失败消息进入死信 Topic            |
| 原始 Key 保留   | 死信消息 Key 与原消息一致         |
| Header 保留     | messageId、traceId 等 Header 保留 |
| 原始 Topic 信息 | Header 或死信表记录原始 Topic     |
| 死信记录落库    | 死信表写入成功                    |
| 死信重放        | 死信消息可重新发送到目标 Topic    |

测试流程如下：

```text
发送测试消息
  -> Consumer 持续抛出异常
  -> DefaultErrorHandler 重试耗尽
  -> DeadLetterPublishingRecoverer 投递死信 Topic
  -> 测试 Consumer 从死信 Topic 读取消息
  -> 断言 Key、Value、Header
```

死信 Topic 测试要求如下：

1. 死信 Topic 使用独立测试 Topic。
2. 重试次数和间隔在测试环境中设置较小。
3. 验证死信消息内容时不要只验证数量，也要验证 Key 和 Header。
4. 如果项目有死信表，应同时验证数据库记录。
5. 死信重放接口应单独做接口测试和权限测试。

### 幂等消费测试

幂等消费测试用于验证同一条消息重复消费时，不会重复执行业务逻辑。它是 Kafka Consumer 测试中最重要的一类测试之一。

测试内容如下：

| 测试点                | 说明                                     |
| --------------------- | ---------------------------------------- |
| 同 messageId 重复消费 | 第二次消费应跳过                         |
| 同业务 Key 重复消费   | 不应重复执行业务动作                     |
| 并发重复消费          | 多线程同时处理同一幂等键，只允许一个成功 |
| 失败后重试            | 第一次失败不应错误标记为成功             |
| 死信重放              | 已成功处理过的消息重放后应跳过           |
| 幂等记录过期          | 过期后行为符合业务预期                   |

幂等服务测试示例：

文件位置：`src/test/java/io/github/atengk/kafka/idempotent/IdempotentKeyTest.java`

该测试类用于验证幂等键生成规则稳定。

```java
package io.github.atengk.kafka.idempotent;

import cn.hutool.core.util.StrUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Kafka 幂等键测试
 *
 * @author Ateng
 * @since 2026-05-11
 */
class IdempotentKeyTest {

    /**
     * 测试消息级幂等键
     */
    @Test
    void shouldBuildMessageIdempotentKey() {
        String messageId = "1900000000000000001";
        String consumerGroup = "ateng.order.consumer.group";

        String idempotentKey = StrUtil.format("{}:{}", messageId, consumerGroup);

        Assertions.assertEquals("1900000000000000001:ateng.order.consumer.group", idempotentKey);
    }

    /**
     * 测试业务级幂等键
     */
    @Test
    void shouldBuildBusinessIdempotentKey() {
        String bizKey = "ORDER:10001";
        String eventType = "ORDER_CREATED";
        String consumerGroup = "ateng.points.consumer.group";

        String idempotentKey = StrUtil.format("{}:{}:{}", bizKey, eventType, consumerGroup);

        Assertions.assertEquals("ORDER:10001:ORDER_CREATED:ateng.points.consumer.group", idempotentKey);
    }

}
```

幂等消费测试要求如下：

1. 幂等必须依赖唯一约束或原子操作。
2. 重复消息测试不能只验证无异常，还要验证业务表没有重复数据。
3. 并发幂等测试需要覆盖多线程同时插入场景。
4. 死信重放必须验证幂等保护是否仍然有效。
5. 幂等测试应覆盖 `SUCCESS`、`PROCESSING`、`FAILED` 状态。

### 性能压测

性能压测用于验证 Kafka 项目在目标流量下的发送吞吐、消费吞吐、延迟、失败率、Consumer Lag、数据库写入能力和 JVM 稳定性。性能压测应在接近生产的环境中执行，不能只依赖本地单机测试。

压测目标应明确：

| 目标          | 示例                  |
| ------------- | --------------------- |
| Producer 吞吐 | 每秒发送 5000 条消息  |
| Consumer 吞吐 | 每秒消费 3000 条消息  |
| 端到端延迟    | P95 小于 5 秒         |
| 失败率        | 小于 0.1%             |
| Consumer Lag  | 稳态下不持续增长      |
| 数据库写入    | 批量写入无明显慢 SQL  |
| JVM           | 无频繁 Full GC 或 OOM |

Kafka 自带压测命令示例：

```bash
kafka-producer-perf-test.sh \
  --topic ateng.perf.test.topic \
  --num-records 1000000 \
  --record-size 1024 \
  --throughput 10000 \
  --producer-props bootstrap.servers=localhost:9092 acks=all compression.type=lz4
```

该命令用于向指定 Topic 发送 100 万条消息，每条消息 1024 字节，目标吞吐为每秒 10000 条，并使用 `acks=all` 和 `lz4` 压缩。

Consumer 压测命令示例：

```bash
kafka-consumer-perf-test.sh \
  --bootstrap-server localhost:9092 \
  --topic ateng.perf.test.topic \
  --messages 1000000 \
  --group ateng.perf.test.group
```

该命令用于从指定 Topic 消费 100 万条消息，并输出消费吞吐统计。

应用级压测还应覆盖真实业务处理：

```text
准备测试 Topic
  -> 准备测试数据库
  -> 启动 Producer 压测程序
  -> 启动 Consumer 应用
  -> 观察发送速率、消费速率、Lag、失败率、数据库耗时
  -> 调整 batch.size、linger.ms、max.poll.records、concurrency
  -> 重复压测并记录结果
```

压测记录表建议：

| 参数            | 压测值       |
| --------------- | ------------ |
| Topic 分区数    | 例如 12      |
| Producer 实例数 | 例如 3       |
| Consumer 实例数 | 例如 4       |
| Consumer 并发数 | 例如每实例 3 |
| 单条消息大小    | 例如 1KB     |
| Producer TPS    | 实测值       |
| Consumer TPS    | 实测值       |
| P95 延迟        | 实测值       |
| P99 延迟        | 实测值       |
| 最大 Lag        | 实测值       |
| CPU / 内存 / GC | 实测值       |
| 数据库 QPS      | 实测值       |
| 失败率          | 实测值       |

性能压测要求如下：

1. 压测环境应尽量接近生产。
2. 压测数据不能污染生产 Topic。
3. 压测 Topic、Group、数据库表应使用独立命名。
4. 需要分别压 Producer、Consumer 和端到端链路。
5. 调参一次只改少量参数，便于判断效果。
6. 压测报告应记录参数、结果、瓶颈和建议配置。
7. 性能优化不能绕过幂等、异常处理和可靠性要求。

## 部署方案

本章节用于说明 Kafka 项目的部署方式，包括本地部署、Docker 部署、Kubernetes 部署、配置中心接入、环境变量配置、CI/CD 流程、灰度发布和回滚方案。部署方案需要同时关注应用配置、Kafka 连接、安全认证、Topic 初始化、健康检查、日志采集和监控接入。

Kafka 应用部署不只是启动 Spring Boot 服务，还需要保证不同环境的 Kafka 地址、Topic、Consumer Group、认证信息、证书、消息保留策略和告警规则都能正确生效。生产环境应避免把 Kafka 账号密码、证书路径、Topic 配置和环境参数硬编码在代码中。

### 本地部署

本地部署用于开发人员在本机启动 Kafka 项目，完成 Producer、Consumer、Topic、序列化、重试、死信和接口联调。推荐本地使用 Docker Compose 启动 Kafka，再通过 Spring Boot 本地配置连接。

本地启动前应准备以下内容：

| 项目                  | 说明                                                |
| --------------------- | --------------------------------------------------- |
| JDK                   | 推荐 JDK 17 或更高                                  |
| Maven                 | 用于构建 Spring Boot 项目                           |
| Kafka                 | 本地 Docker Compose 启动                            |
| Kafka UI              | 可选，用于查看 Topic 和消息                         |
| MySQL / Redis         | 如果项目使用消费记录表、幂等表、重试表或 Redis 去重 |
| application-local.yml | 本地环境配置文件                                    |

本地配置示例：

文件位置：`src/main/resources/application-local.yml`

该配置用于本地连接 Kafka、MySQL 和 Redis，适合开发调试使用。

```yaml
server:
  # 本地服务端口
  port: 8080

spring:
  application:
    # 应用名称
    name: kafka-develop-demo

  kafka:
    # 本地 Kafka 地址
    bootstrap-servers: localhost:9092

    producer:
      # Key 使用字符串序列化
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      # Value 使用 JSON 序列化
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
      properties:
        # 本地开发也建议关闭 Java 类型 Header，降低服务耦合
        spring.json.add.type.headers: false
        # 开启幂等发送
        enable.idempotence: true

    consumer:
      # 本地测试消费组
      group-id: ateng-local-consumer-group
      # 关闭自动提交 Offset
      enable-auto-commit: false
      # 本地从最早消息开始消费，方便重复验证
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
      properties:
        # 委托 JSON 反序列化器
        spring.deserializer.value.delegate.class: org.springframework.kafka.support.serializer.JsonDeserializer
        # 可信包路径
        spring.json.trusted.packages: io.github.atengk.kafka.model,java.util,java.lang
        # 默认消息类型
        spring.json.value.default.type: io.github.atengk.kafka.model.MessageEnvelope

    listener:
      # 手动提交 Offset
      ack-mode: manual_immediate
      # 本地并发数
      concurrency: 1
```

本地启动命令：

```bash
# 使用 local 环境启动 Spring Boot 应用
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

打包后启动：

```bash
# 打包项目
mvn clean package -DskipTests

# 使用 local 环境运行 jar
java -jar target/kafka-develop-demo.jar --spring.profiles.active=local
```

验证命令：

```bash
# 查看 Topic 列表
docker exec -it ateng-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --list

# 查看消费组
docker exec -it ateng-kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --list
```

本地部署要求如下：

1. 本地环境可以使用 `PLAINTEXT`，但不能套用到生产环境。
2. 本地 Consumer Group 应与测试、生产隔离。
3. 本地 Topic 可以自动创建，生产环境不建议业务应用自动创建。
4. 本地测试数据应定期清理。
5. 本地配置不得包含生产 Kafka 地址和生产账号密码。

### Docker 部署

Docker 部署用于将 Spring Boot Kafka 应用打包为容器镜像，便于在测试、预发或容器平台运行。Docker 镜像中不应内置环境敏感配置，Kafka 地址、账号、密码和证书路径应通过环境变量注入。

文件位置：`Dockerfile`

该 Dockerfile 用于构建 Spring Boot Kafka 应用镜像。

```dockerfile
# 使用 JDK 17 运行环境
FROM eclipse-temurin:17-jre

# 应用工作目录
WORKDIR /app

# 复制应用 Jar 包
COPY target/kafka-develop-demo.jar /app/kafka-develop-demo.jar

# 暴露应用端口
EXPOSE 8080

# JVM 参数和 Spring Profile 通过环境变量注入
ENV JAVA_OPTS="-Xms512m -Xmx512m"
ENV SPRING_PROFILES_ACTIVE="prod"

# 启动应用
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/kafka-develop-demo.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
```

构建镜像：

```bash
# 打包 Spring Boot 应用
mvn clean package -DskipTests

# 构建 Docker 镜像
docker build -t kafka-develop-demo:1.0.0 .
```

运行容器：

```bash
docker run -d \
  --name kafka-develop-demo \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=test \
  -e KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
  -e JAVA_OPTS="-Xms512m -Xmx512m" \
  kafka-develop-demo:1.0.0
```

Docker Compose 部署示例：

文件位置：`docker-compose-app.yml`

该配置用于在 Docker Compose 中启动 Kafka 应用，并通过环境变量连接 Kafka。

```yaml
services:
  kafka-develop-demo:
    # 应用镜像
    image: kafka-develop-demo:1.0.0
    container_name: kafka-develop-demo
    ports:
      # 暴露应用端口
      - "8080:8080"
    environment:
      # Spring 环境
      SPRING_PROFILES_ACTIVE: test
      # Kafka Broker 地址
      KAFKA_BOOTSTRAP_SERVERS: ateng-kafka:9092
      # JVM 参数
      JAVA_OPTS: "-Xms512m -Xmx512m"
    networks:
      - kafka-net

networks:
  kafka-net:
    external: true
```

启动命令：

```bash
# 启动应用容器
docker compose -f docker-compose-app.yml up -d

# 查看日志
docker logs -f kafka-develop-demo

# 健康检查
curl http://localhost:8080/actuator/health
```

Docker 部署要求如下：

1. 镜像中不写死 Kafka 地址。
2. 镜像中不包含生产密码和证书。
3. 不同环境通过环境变量或配置中心区分。
4. 日志输出到标准输出，便于容器平台采集。
5. 容器 JVM 内存应结合容器内存限制配置。
6. 应用应暴露健康检查接口。

### Kubernetes 部署

Kubernetes 部署用于在容器编排平台中运行 Kafka 应用。部署时需要关注 ConfigMap、Secret、Deployment、Service、健康检查、资源限制、优雅停机和滚动发布策略。

文件位置：`k8s/configmap.yml`

该 ConfigMap 用于保存非敏感配置。

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: kafka-develop-demo-config
data:
  # Spring 环境
  SPRING_PROFILES_ACTIVE: "prod"

  # Kafka Broker 地址
  KAFKA_BOOTSTRAP_SERVERS: "kafka-01:9092,kafka-02:9092,kafka-03:9092"

  # JVM 参数
  JAVA_OPTS: "-Xms1024m -Xmx1024m -XX:+UseG1GC"
```

文件位置：`k8s/secret.yml`

该 Secret 用于保存 Kafka 用户名、密码等敏感配置。实际生产环境应由密钥平台或 CI/CD 注入，不建议明文维护。

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: kafka-develop-demo-secret
type: Opaque
stringData:
  # Kafka 用户名
  KAFKA_USERNAME: "your-username"

  # Kafka 密码
  KAFKA_PASSWORD: "your-password"
```

文件位置：`k8s/deployment.yml`

该 Deployment 用于部署 Kafka 应用，包含健康检查、资源限制和优雅停机配置。

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kafka-develop-demo
spec:
  replicas: 3
  selector:
    matchLabels:
      app: kafka-develop-demo
  strategy:
    type: RollingUpdate
    rollingUpdate:
      # 滚动发布时最多新增一个实例
      maxSurge: 1
      # 滚动发布时不可用实例数为0，减少消费中断
      maxUnavailable: 0
  template:
    metadata:
      labels:
        app: kafka-develop-demo
    spec:
      # 优雅停机时间
      terminationGracePeriodSeconds: 60
      containers:
        - name: kafka-develop-demo
          image: kafka-develop-demo:1.0.0
          ports:
            - containerPort: 8080
          envFrom:
            # 注入非敏感配置
            - configMapRef:
                name: kafka-develop-demo-config
            # 注入敏感配置
            - secretRef:
                name: kafka-develop-demo-secret
          resources:
            requests:
              cpu: "500m"
              memory: "1Gi"
            limits:
              cpu: "2"
              memory: "2Gi"
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 20
          lifecycle:
            preStop:
              exec:
                command:
                  # 预留时间给服务摘流和Kafka监听容器停止
                  - /bin/sh
                  - -c
                  - "sleep 10"
```

文件位置：`k8s/service.yml`

该 Service 用于暴露应用内部访问地址。

```yaml
apiVersion: v1
kind: Service
metadata:
  name: kafka-develop-demo
spec:
  selector:
    app: kafka-develop-demo
  ports:
    - name: http
      port: 8080
      targetPort: 8080
```

部署命令：

```bash
# 应用 Kubernetes 配置
kubectl apply -f k8s/configmap.yml
kubectl apply -f k8s/secret.yml
kubectl apply -f k8s/deployment.yml
kubectl apply -f k8s/service.yml

# 查看 Pod 状态
kubectl get pods -l app=kafka-develop-demo

# 查看应用日志
kubectl logs -f deployment/kafka-develop-demo
```

Kubernetes 部署要求如下：

1. Kafka 地址、账号、密码通过 ConfigMap 和 Secret 注入。
2. 生产环境必须配置资源 requests 和 limits。
3. Consumer 应用需要优雅停机，避免发布时大量重复消费。
4. 滚动发布建议设置 `maxUnavailable=0`。
5. 健康检查不能过于激进，避免启动阶段被误杀。
6. 多实例 Consumer 扩容前需要确认 Topic 分区数足够。

### 配置中心接入

配置中心接入用于统一管理 Kafka 地址、Topic 名称、Consumer Group、重试参数、死信开关、降级开关和安全配置。常见配置中心包括 Nacos、Apollo、Spring Cloud Config 或企业内部配置平台。

建议放入配置中心的配置如下：

| 配置              | 说明                                |
| ----------------- | ----------------------------------- |
| Kafka Broker 地址 | 不同环境不同地址                    |
| Topic 名称        | 不同环境可加环境前缀                |
| Consumer Group    | 稳定配置，避免误变更                |
| Producer 参数     | acks、retries、compression          |
| Consumer 参数     | concurrency、max.poll.records       |
| 重试参数          | 最大重试次数、重试间隔              |
| 死信开关          | 是否启用死信                        |
| 降级开关          | 是否启用降级                        |
| Topic 白名单      | 管理接口发送限制                    |
| 安全认证配置      | 账号、证书路径等，敏感值应放 Secret |

配置示例：

```yaml
app:
  kafka:
    # Kafka集群名称
    cluster-name: biz

    # Topic白名单，管理接口只能向白名单内Topic发送消息
    topic-whitelist:
      - ateng.order.created.topic
      - ateng.payment.success.topic

    producer:
      # 是否启用发送记录
      enable-send-record: true

    consumer:
      # 是否启用消费幂等
      enable-idempotent: true

    retry:
      # 最大重试次数
      max-retry-count: 3
      # 重试间隔毫秒
      retry-interval-ms: 2000

    dead-letter:
      # 是否启用死信
      enabled: true
      # 通用死信Topic
      topic: ateng.common.dead-letter.topic

    degrade:
      # 是否启用降级
      enabled: false
```

配置中心接入要求如下：

1. 配置项必须有默认值和说明。
2. 高风险配置变更必须审批，例如 Consumer Group、Topic、重试次数。
3. 敏感配置不应明文放入普通配置中心。
4. 配置变更应记录操作人和变更历史。
5. 对动态刷新配置要谨慎，Consumer Group、Kafka 地址等不建议运行中随意刷新。
6. 降级开关、Topic 白名单、重试间隔等可以支持动态刷新。

### 环境变量配置

环境变量配置用于在容器、Kubernetes、CI/CD 和不同运行环境中注入 Kafka 应用参数。环境变量命名应统一，避免混乱。

推荐环境变量如下：

```text
SPRING_PROFILES_ACTIVE
KAFKA_BOOTSTRAP_SERVERS
KAFKA_USERNAME
KAFKA_PASSWORD
KAFKA_SECURITY_PROTOCOL
KAFKA_SASL_MECHANISM
KAFKA_SSL_TRUSTSTORE_LOCATION
KAFKA_SSL_TRUSTSTORE_PASSWORD
JAVA_OPTS
```

Spring Boot 配置引用示例：

```yaml
spring:
  kafka:
    # Kafka地址从环境变量注入
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}

    properties:
      # 安全协议，默认本地PLAINTEXT
      security.protocol: ${KAFKA_SECURITY_PROTOCOL:PLAINTEXT}
      # SASL机制，生产环境按集群要求设置
      sasl.mechanism: ${KAFKA_SASL_MECHANISM:}
      # JAAS配置，生产环境通过环境变量注入
      sasl.jaas.config: ${KAFKA_SASL_JAAS_CONFIG:}
```

SASL 环境变量示例：

```bash
export KAFKA_BOOTSTRAP_SERVERS="kafka-01:9093,kafka-02:9093,kafka-03:9093"
export KAFKA_SECURITY_PROTOCOL="SASL_SSL"
export KAFKA_SASL_MECHANISM="SCRAM-SHA-512"
export KAFKA_SASL_JAAS_CONFIG='org.apache.kafka.common.security.scram.ScramLoginModule required username="order-service" password="your-password";'
```

环境变量配置要求如下：

1. 环境变量名使用大写加下划线。
2. 多集群场景应加集群标识，例如 `KAFKA_BIZ_BOOTSTRAP_SERVERS`。
3. 密码、证书密码和 JAAS 配置必须通过 Secret 注入。
4. 不要在启动日志中打印完整环境变量。
5. 本地默认值只允许指向本地 Kafka。
6. CI/CD 中应按环境注入不同变量。

### CI/CD 流程

CI/CD 流程用于将 Kafka 应用从代码提交到测试、构建、镜像推送、部署、验证和发布形成自动化链路。Kafka 项目在 CI/CD 中需要额外关注消息契约、Topic 初始化、配置校验和回滚策略。

推荐流水线流程如下：

```text
代码提交
  -> 单元测试
  -> Kafka集成测试
  -> 构建Jar包
  -> 构建Docker镜像
  -> 推送镜像仓库
  -> 部署测试环境
  -> 自动化接口测试
  -> 部署预发环境
  -> 灰度发布生产
  -> 监控Lag、失败率、死信
  -> 全量发布
```

GitHub Actions 示例：

文件位置：`.github/workflows/deploy.yml`

该流水线用于构建 Spring Boot 项目、构建镜像并推送镜像仓库。

```yaml
name: kafka-develop-demo-ci

on:
  push:
    branches:
      # main分支提交后触发
      - main

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout source code
        uses: actions/checkout@v4

      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: "17"
          distribution: "temurin"

      - name: Build with Maven
        run: |
          # 执行测试并打包
          mvn clean package

      - name: Build Docker image
        run: |
          # 构建应用镜像
          docker build -t registry.example.com/kafka-develop-demo:${{ github.sha }} .

      - name: Push Docker image
        run: |
          # 推送镜像，实际项目中需要先登录镜像仓库
          docker push registry.example.com/kafka-develop-demo:${{ github.sha }}
```

CI/CD 要求如下：

1. 每次构建必须执行单元测试。
2. Kafka 集成测试可以放在测试流水线或夜间流水线。
3. Schema 文件变更必须执行兼容性校验。
4. 构建镜像时不内置环境敏感配置。
5. 发布后自动检查健康状态、Consumer Lag 和死信增长。
6. 失败时应支持快速回滚到上一版本镜像。

### 灰度发布

灰度发布用于降低新版本 Consumer 或 Producer 对生产消息链路的影响。Kafka 应用灰度发布需要特别关注 Consumer Group、消息兼容性、幂等和 Rebalance。

Producer 灰度发布关注点：

| 项目         | 说明                                        |
| ------------ | ------------------------------------------- |
| 消息模型兼容 | 新 Producer 发送的新字段必须兼容旧 Consumer |
| Topic 不变   | 不应在灰度中随意切换 Topic                  |
| Header 兼容  | 新增 Header 不能影响旧 Consumer             |
| 发送失败率   | 灰度实例发送失败率必须监控                  |
| 消息量比例   | 控制灰度实例流量比例                        |

Consumer 灰度发布关注点：

| 项目           | 说明                             |
| -------------- | -------------------------------- |
| Consumer Group | 同一业务消费应继续使用同一 Group |
| Rebalance      | 灰度实例加入会触发 Rebalance     |
| 幂等           | 新旧版本都必须幂等               |
| 消费失败率     | 新版本失败率不能高于旧版本       |
| 死信增长       | 灰度期间死信增长必须重点观察     |
| 兼容旧消息     | 新 Consumer 必须能消费历史消息   |

灰度流程建议：

```text
发布 1 个新版本实例
  -> 观察 Producer 发送失败率
  -> 观察 Consumer Lag
  -> 观察消费失败率
  -> 观察死信增长
  -> 观察业务指标
  -> 无异常后逐步扩大实例比例
  -> 全量发布
```

Kubernetes 灰度可以通过副本数控制：

```bash
# 先将新版本部署为1个副本
kubectl set image deployment/kafka-develop-demo kafka-develop-demo=kafka-develop-demo:1.1.0

# 观察发布状态
kubectl rollout status deployment/kafka-develop-demo
```

灰度发布要求如下：

1. 不兼容消息模型变更不能直接灰度 Producer。
2. 先发布兼容 Consumer，再发布 Producer。
3. 灰度期间必须观察 Lag、失败率和死信。
4. 灰度实例异常时应立即停止扩容并回滚。
5. 灰度期间不建议同时修改 Topic、Consumer Group 和消息模型。
6. 核心业务发布应避开流量高峰。

### 回滚方案

回滚方案用于在新版本发布后出现消费失败、发送失败、死信增长、Lag 暴涨或业务异常时快速恢复。Kafka 应用回滚不仅是回滚镜像，还要确认消息模型、Offset、死信和补偿任务的影响。

Kubernetes 回滚命令：

```bash
# 查看发布历史
kubectl rollout history deployment/kafka-develop-demo

# 回滚到上一个版本
kubectl rollout undo deployment/kafka-develop-demo

# 查看回滚状态
kubectl rollout status deployment/kafka-develop-demo
```

回滚前需要判断：

| 判断项                  | 说明                           |
| ----------------------- | ------------------------------ |
| 是否是代码问题          | 如果是代码问题，回滚镜像有效   |
| 是否是消息模型不兼容    | 需要确认新消息是否已进入 Topic |
| 是否写入了新格式消息    | 旧版本 Consumer 是否能处理     |
| 是否产生死信            | 回滚后是否需要重放死信         |
| 是否修改 Consumer Group | 回滚后消费进度是否变化         |
| 是否修改数据库结构      | 旧版本是否兼容新表结构         |
| 是否修改 Topic 配置     | 是否需要恢复 Topic 配置        |

回滚流程建议：

```text
发现异常
  -> 暂停继续发布
  -> 判断异常范围
  -> 回滚应用镜像
  -> 观察应用健康状态
  -> 观察 Consumer Lag 是否下降
  -> 观察消费失败率是否恢复
  -> 处理发布期间产生的失败记录和死信
  -> 复盘根因
```

回滚要求如下：

1. 镜像必须保留历史版本。
2. 数据库变更必须向后兼容，避免应用回滚后无法运行。
3. 消息 Schema 变更必须兼容旧版本。
4. 回滚后需要处理新版本产生的死信和失败记录。
5. 回滚不应随意重置 Offset，除非明确知道影响。
6. 生产回滚必须记录操作日志和原因。

## 运维管理

本章节用于说明 Kafka 项目的常见运维操作，包括 Topic 创建与修改、Consumer Group 管理、Offset 查询、Offset 重置、消息查询、死信消息重放、磁盘容量管理和数据保留管理。

Kafka 运维操作通常具有较高风险，尤其是 Topic 删除、Offset 重置、分区增加、保留策略修改和死信批量重放。生产环境中应通过中间件平台、审批流程或受控脚本执行，并保留操作审计记录。

### Topic 创建与修改

Topic 创建与修改用于管理 Kafka 中的业务消息主题。Topic 创建前应确认命名、分区数、副本数、保留时间、清理策略、负责人和使用场景。

创建 Topic 示例：

```bash
kafka-topics.sh \
  --bootstrap-server kafka-01:9092 \
  --create \
  --topic ateng.order.created.topic \
  --partitions 6 \
  --replication-factor 3 \
  --config min.insync.replicas=2 \
  --config retention.ms=604800000 \
  --config cleanup.policy=delete
```

参数说明如下：

| 参数                   | 说明           |
| ---------------------- | -------------- |
| `--topic`              | Topic 名称     |
| `--partitions`         | 分区数         |
| `--replication-factor` | 副本数         |
| `min.insync.replicas`  | 最小同步副本数 |
| `retention.ms`         | 消息保留时间   |
| `cleanup.policy`       | 清理策略       |

查看 Topic：

```bash
kafka-topics.sh \
  --bootstrap-server kafka-01:9092 \
  --describe \
  --topic ateng.order.created.topic
```

修改 Topic 配置：

```bash
kafka-configs.sh \
  --bootstrap-server kafka-01:9092 \
  --entity-type topics \
  --entity-name ateng.order.created.topic \
  --alter \
  --add-config retention.ms=1209600000
```

增加分区：

```bash
kafka-topics.sh \
  --bootstrap-server kafka-01:9092 \
  --alter \
  --topic ateng.order.created.topic \
  --partitions 12
```

Topic 运维要求如下：

1. 生产 Topic 创建必须审批。
2. 副本数不能大于 Broker 数量。
3. 增加分区前必须评估顺序消息影响。
4. 修改保留时间前必须评估磁盘容量。
5. 删除 Topic 属于高危操作，生产环境默认禁止。
6. Topic 变更应记录操作人、时间、变更内容和原因。

### Consumer Group 管理

Consumer Group 管理用于查看消费组状态、消费成员、分区分配、当前 Offset 和消费堆积情况。它是排查消费延迟和消息堆积的核心运维操作。

查看消费组列表：

```bash
kafka-consumer-groups.sh \
  --bootstrap-server kafka-01:9092 \
  --list
```

查看消费组详情：

```bash
kafka-consumer-groups.sh \
  --bootstrap-server kafka-01:9092 \
  --group ateng.order.consumer.group \
  --describe
```

输出字段通常包括：

| 字段             | 说明              |
| ---------------- | ----------------- |
| `GROUP`          | 消费组名称        |
| `TOPIC`          | Topic 名称        |
| `PARTITION`      | 分区编号          |
| `CURRENT-OFFSET` | 当前已提交 Offset |
| `LOG-END-OFFSET` | 分区最新 Offset   |
| `LAG`            | 消费堆积数量      |
| `CONSUMER-ID`    | 消费者实例 ID     |
| `HOST`           | 消费者所在主机    |
| `CLIENT-ID`      | 客户端 ID         |

查看消费组状态：

```bash
kafka-consumer-groups.sh \
  --bootstrap-server kafka-01:9092 \
  --group ateng.order.consumer.group \
  --state
```

Consumer Group 管理要求如下：

1. 消费组名称必须稳定。
2. 不同业务逻辑使用不同 Consumer Group。
3. 查询 Lag 时必须明确 Topic 和 Group。
4. Rebalance 频繁时应查看 Consumer 成员变化。
5. 生产环境不要随意删除 Consumer Group。
6. 消费组异常应结合应用日志和 Broker 指标排查。

### Offset 查询

Offset 查询用于确认 Consumer 当前消费到哪里，以及某个 Topic 分区的起始 Offset、最新 Offset 和消费组提交 Offset。Offset 查询是排查消息是否被消费、是否堆积和是否需要重置的基础。

查看消费组 Offset：

```bash
kafka-consumer-groups.sh \
  --bootstrap-server kafka-01:9092 \
  --group ateng.order.consumer.group \
  --describe
```

查看 Topic 最新 Offset 可以使用控制台工具或监控系统。常见判断方式如下：

```text
Lag = LOG-END-OFFSET - CURRENT-OFFSET
```

查询结果解读：

| 情况                 | 说明                            |
| -------------------- | ------------------------------- |
| `LAG = 0`            | 当前分区无堆积                  |
| `LAG 持续增长`       | 消费速度不足或 Consumer 异常    |
| `CURRENT-OFFSET = -` | 消费组可能未消费过该分区        |
| 单分区 Lag 很高      | 可能存在热点 Key 或处理线程卡住 |
| 所有分区 Lag 高      | 消费组整体处理能力不足          |

Offset 查询要求如下：

1. 查询 Offset 时必须确认环境和集群。
2. 多集群场景下必须指定正确 Bootstrap Server。
3. 查询结果应结合时间趋势，不只看单次值。
4. Offset 查询不改变消费进度，属于安全操作。
5. Offset 查询结果应与 Grafana Lag 面板交叉验证。

### Offset 重置

Offset 重置用于改变 Consumer Group 的消费位置。它是高风险操作，可能导致重复消费大量历史消息，也可能跳过未消费消息。生产环境必须审批并提前备份当前 Offset。

重置到最早位置：

```bash
kafka-consumer-groups.sh \
  --bootstrap-server kafka-01:9092 \
  --group ateng.order.consumer.group \
  --topic ateng.order.created.topic \
  --reset-offsets \
  --to-earliest \
  --execute
```

重置到最新位置：

```bash
kafka-consumer-groups.sh \
  --bootstrap-server kafka-01:9092 \
  --group ateng.order.consumer.group \
  --topic ateng.order.created.topic \
  --reset-offsets \
  --to-latest \
  --execute
```

重置到指定 Offset：

```bash
kafka-consumer-groups.sh \
  --bootstrap-server kafka-01:9092 \
  --group ateng.order.consumer.group \
  --topic ateng.order.created.topic:0 \
  --reset-offsets \
  --to-offset 1024 \
  --execute
```

按时间重置：

```bash
kafka-consumer-groups.sh \
  --bootstrap-server kafka-01:9092 \
  --group ateng.order.consumer.group \
  --topic ateng.order.created.topic \
  --reset-offsets \
  --to-datetime 2026-05-11T10:00:00.000 \
  --execute
```

执行前预览：

```bash
kafka-consumer-groups.sh \
  --bootstrap-server kafka-01:9092 \
  --group ateng.order.consumer.group \
  --topic ateng.order.created.topic \
  --reset-offsets \
  --to-earliest \
  --dry-run
```

Offset 重置要求如下：

1. 执行前必须暂停对应 Consumer。
2. 先使用 `--dry-run` 预览影响。
3. 记录重置前的当前 Offset。
4. 确认业务幂等能力有效。
5. 重置后观察消费速率、Lag、失败率和死信。
6. 不允许在不了解影响的情况下重置生产核心消费组。

### 消息查询

Kafka 原生不适合按业务字段查询消息。生产项目中，消息查询应优先依赖发送记录表、消费记录表、死信消息表和日志平台。如果确实需要查看 Topic 中的消息，可以使用 Kafka 控制台 Consumer 临时消费，但要避免影响正式 Consumer Group。

通过控制台查看消息：

```bash
kafka-console-consumer.sh \
  --bootstrap-server kafka-01:9092 \
  --topic ateng.order.created.topic \
  --from-beginning \
  --max-messages 10
```

使用临时消费组查看：

```bash
kafka-console-consumer.sh \
  --bootstrap-server kafka-01:9092 \
  --topic ateng.order.created.topic \
  --group ateng.temp.query.group \
  --from-beginning \
  --timeout-ms 10000
```

按业务记录查询建议：

```sql
SELECT *
FROM kafka_send_record
WHERE message_id = '1900000000000000001';

SELECT *
FROM kafka_consume_record
WHERE message_id = '1900000000000000001';

SELECT *
FROM kafka_dead_letter_message
WHERE message_id = '1900000000000000001'
   OR original_message_id = '1900000000000000001';
```

消息查询要求如下：

1. 不要使用生产业务 Consumer Group 做临时查询。
2. 临时查询使用独立 Group，避免影响正式 Offset。
3. 查询敏感消息内容时必须脱敏。
4. 大量历史消息查询应走数据库或日志平台，不直接扫描 Kafka。
5. 查询结果应通过 `messageId`、`traceId`、`topic + partition + offset` 关联完整链路。

### 死信消息重放

死信消息重放用于在修复数据、修复代码或恢复下游服务后，将死信消息重新发送到原始 Topic 或指定补偿 Topic。死信重放是高风险操作，必须保证幂等、审计和权限控制。

重放流程建议：

```text
查询死信消息
  -> 分析失败原因
  -> 修复业务数据或代码
  -> 确认Consumer幂等有效
  -> 发起重放
  -> 更新死信状态
  -> 观察消费结果
  -> 记录重放审计
```

重放接口示例：

```bash
curl -X POST "http://localhost:8080/api/kafka/dead-letters/10001/replay" \
  -H "Content-Type: application/json" \
  -d '{
    "targetTopic": "ateng.order.created.topic",
    "keepOriginalMessageId": true,
    "reason": "修复订单状态后重放"
  }'
```

批量重放时应设置限制：

```text
单次重放数量 <= 100
只允许重放 PENDING 或 FAILED 状态
必须记录操作人和原因
核心 Topic 重放需要审批
```

死信重放要求如下：

1. 未修复根因前不得重放。
2. 重放前必须确认幂等逻辑。
3. 重放应保留原始 `messageId` 或 `originalMessageId`。
4. 重放后必须更新死信表状态。
5. 重放失败需要记录并告警。
6. 不允许无限制批量重放。

### 磁盘容量管理

磁盘容量管理用于防止 Kafka Broker 因日志目录磁盘不足导致写入失败、Broker 异常或分区不可用。Kafka 的磁盘占用主要由 Topic 数量、分区数量、副本数、消息量、消息大小和保留策略决定。

磁盘容量影响因素如下：

| 因素         | 说明                           |
| ------------ | ------------------------------ |
| 消息写入速率 | 写入越多，磁盘增长越快         |
| 消息大小     | 单条消息越大，占用越高         |
| 保留时间     | `retention.ms` 越大，保留越久  |
| 保留大小     | `retention.bytes` 控制空间上限 |
| 副本数       | 副本数越多，总存储成本越高     |
| 压缩         | Producer 压缩可降低磁盘占用    |
| 分区数       | 分区越多，日志段文件越多       |

容量估算示例：

```text
每日存储量 = 每秒消息数 * 平均消息大小 * 86400 * 副本数
```

例如：

```text
每秒 1000 条
平均消息 1KB
副本数 3

每日存储量约 = 1000 * 1KB * 86400 * 3 ≈ 247GB
```

磁盘管理建议：

1. Broker 磁盘使用率超过 80% 预警。
2. Broker 磁盘使用率超过 90% 严重告警。
3. 日志类 Topic 应设置较短保留时间。
4. 死信 Topic 保留时间应满足排查需要，但不能无限保留。
5. 大消息 Topic 应单独评估容量。
6. 定期检查 Topic 保留策略和异常增长 Topic。

查看 Topic 配置：

```bash
kafka-configs.sh \
  --bootstrap-server kafka-01:9092 \
  --entity-type topics \
  --entity-name ateng.order.created.topic \
  --describe
```

调整保留时间：

```bash
kafka-configs.sh \
  --bootstrap-server kafka-01:9092 \
  --entity-type topics \
  --entity-name ateng.order.created.topic \
  --alter \
  --add-config retention.ms=604800000
```

### 数据保留管理

数据保留管理用于控制 Kafka Topic 中消息保存时间和空间。Kafka 消息被消费后不会立即删除，而是按照 Topic 的保留策略清理。因此，保留策略必须根据业务回溯、补偿、审计和磁盘成本综合设置。

常见配置如下：

| 配置项            | 说明                                 |
| ----------------- | ------------------------------------ |
| `retention.ms`    | 按时间保留消息                       |
| `retention.bytes` | 按空间限制保留消息                   |
| `cleanup.policy`  | 清理策略，常用 `delete` 或 `compact` |
| `segment.ms`      | 日志段滚动时间                       |
| `segment.bytes`   | 日志段大小                           |

推荐保留策略如下：

| Topic 类型     | 建议保留时间      |
| -------------- | ----------------- |
| 普通业务 Topic | 3 到 7 天         |
| 核心业务 Topic | 7 到 30 天        |
| 日志采集 Topic | 1 到 3 天         |
| Retry Topic    | 大于最大重试周期  |
| 死信 Topic     | 14 到 30 天或更长 |
| 临时测试 Topic | 1 天以内          |

配置保留时间：

```bash
kafka-configs.sh \
  --bootstrap-server kafka-01:9092 \
  --entity-type topics \
  --entity-name ateng.payment.success.topic \
  --alter \
  --add-config retention.ms=1209600000
```

配置保留大小：

```bash
kafka-configs.sh \
  --bootstrap-server kafka-01:9092 \
  --entity-type topics \
  --entity-name ateng.log.collect.topic \
  --alter \
  --add-config retention.bytes=10737418240
```

配置压缩清理策略：

```bash
kafka-configs.sh \
  --bootstrap-server kafka-01:9092 \
  --entity-type topics \
  --entity-name ateng.user.status.topic \
  --alter \
  --add-config cleanup.policy=compact
```

数据保留管理要求如下：

1. 保留时间不能小于最大允许补偿时间。
2. Consumer 长期停滞可能导致消息过期后无法补消费。
3. 调整保留时间会影响磁盘容量，需要提前评估。
4. `compact` 只适合按 Key 保留最新状态的场景。
5. 死信 Topic 不建议使用 `compact`，否则可能覆盖历史失败消息。
6. 数据保留策略变更必须记录操作审计。

## 安全与合规

本章节用于说明 Kafka 消息链路中的安全与合规设计，包括消息敏感字段识别、消息脱敏、消息加密、权限最小化、审计日志、数据保留周期和数据删除策略。

Kafka 消息通常会在多个系统之间流转，并且会被 Producer、Broker、Consumer、日志系统、死信表、重试表和管理后台多处接触。如果消息中包含手机号、身份证号、银行卡号、地址、Token、密码、密钥或其他敏感业务数据，就必须在消息模型、日志、存储、查询和重放环节建立统一安全规范。

### 消息敏感字段识别

消息敏感字段识别用于在消息发送、落库、日志打印和管理后台展示前，判断消息中是否包含敏感数据。敏感字段识别应同时依赖字段名规则和业务字段清单，不能只依赖开发人员自觉。

常见敏感字段如下：

| 类型     | 常见字段名                             | 处理建议                         |
| -------- | -------------------------------------- | -------------------------------- |
| 手机号   | `mobile`、`phone`、`telephone`         | 脱敏后展示                       |
| 身份证号 | `idCard`、`certNo`、`certificateNo`    | 脱敏后展示                       |
| 银行卡号 | `bankCardNo`、`cardNo`                 | 脱敏后展示                       |
| 邮箱     | `email`                                | 脱敏后展示                       |
| 地址     | `address`、`detailAddress`             | 按需脱敏或禁止日志输出           |
| 密码     | `password`、`pwd`                      | 禁止进入 Kafka                   |
| Token    | `token`、`accessToken`、`refreshToken` | 禁止日志输出，原则上不进入 Kafka |
| 密钥     | `secret`、`privateKey`、`apiKey`       | 禁止进入 Kafka                   |
| 生物信息 | `faceImage`、`fingerprint`             | 禁止进入普通 Kafka 消息          |
| 支付信息 | `payPassword`、`cvv`                   | 禁止进入 Kafka                   |

敏感字段枚举建议如下。

文件位置：`src/main/java/io/github/atengk/kafka/security/SensitiveFieldEnum.java`

该枚举用于统一定义 Kafka 消息中需要识别和处理的敏感字段。

```java
package io.github.atengk.kafka.security;

import lombok.Getter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Kafka 敏感字段枚举
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Getter
public enum SensitiveFieldEnum {

    /**
     * 手机号
     */
    MOBILE("mobile"),

    /**
     * 电话
     */
    PHONE("phone"),

    /**
     * 身份证号
     */
    ID_CARD("idCard"),

    /**
     * 证件号
     */
    CERTIFICATE_NO("certificateNo"),

    /**
     * 银行卡号
     */
    BANK_CARD_NO("bankCardNo"),

    /**
     * 邮箱
     */
    EMAIL("email"),

    /**
     * 地址
     */
    ADDRESS("address"),

    /**
     * 密码
     */
    PASSWORD("password"),

    /**
     * Token
     */
    TOKEN("token"),

    /**
     * 访问令牌
     */
    ACCESS_TOKEN("accessToken"),

    /**
     * 私钥
     */
    PRIVATE_KEY("privateKey"),

    /**
     * API密钥
     */
    API_KEY("apiKey");

    private final String fieldName;

    SensitiveFieldEnum(String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * 获取敏感字段集合
     *
     * @return 敏感字段集合
     */
    public static Set<String> fieldNames() {
        return Arrays.stream(values())
                .map(SensitiveFieldEnum::getFieldName)
                .collect(Collectors.toSet());
    }

}
```

敏感字段识别要求如下：

1. 密码、Token、密钥、私钥原则上禁止写入 Kafka。
2. 手机号、身份证号、银行卡号、邮箱等字段需要按场景脱敏。
3. 发送记录表、死信表、重试表保存消息体时必须评估敏感字段。
4. 管理后台展示消息体时必须脱敏。
5. 日志默认不打印完整消息体。
6. 敏感字段清单应由研发、安全和业务共同维护。

### 消息脱敏

消息脱敏用于在日志打印、接口返回、死信展示和发送记录查询时隐藏敏感内容。脱敏应在展示层、日志层和落库前分别考虑，避免敏感数据扩散。

脱敏规则建议如下：

| 类型     | 原始值               | 脱敏值               |
| -------- | -------------------- | -------------------- |
| 手机号   | `13800008000`        | `138****8000`        |
| 身份证号 | `110101199001011234` | `110101********1234` |
| 银行卡号 | `6222020202021234`   | `6222********1234`   |
| 邮箱     | `ateng@example.com`  | `a****@example.com`  |
| Token    | `abcdef1234567890`   | `abcdef******7890`   |
| 密码     | 任意                 | `******`             |

文件位置：`src/main/java/io/github/atengk/kafka/security/KafkaMessageMaskService.java`

该服务用于对 Kafka 消息 Map 中的常见敏感字段进行脱敏处理，适合日志打印、管理接口返回和死信消息展示前调用。

```java
package io.github.atengk.kafka.security;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 消息脱敏服务
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
public class KafkaMessageMaskService {

    /**
     * 对消息字段进行脱敏
     *
     * @param source 原始消息
     * @return 脱敏后的消息
     */
    public Map<String, Object> mask(Map<String, Object> source) {
        if (MapUtil.isEmpty(source)) {
            return source;
        }

        Map<String, Object> result = new HashMap<>(source.size());
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();

            if (fieldValue instanceof Map<?, ?> nestedMap) {
                result.put(fieldName, maskNestedMap(nestedMap));
                continue;
            }

            result.put(fieldName, maskValue(fieldName, fieldValue));
        }

        return result;
    }

    /**
     * 脱敏嵌套Map
     *
     * @param nestedMap 嵌套Map
     * @return 脱敏后的Map
     */
    private Map<String, Object> maskNestedMap(Map<?, ?> nestedMap) {
        Map<String, Object> convertedMap = new HashMap<>(nestedMap.size());
        for (Map.Entry<?, ?> entry : nestedMap.entrySet()) {
            if (entry.getKey() != null) {
                convertedMap.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return mask(convertedMap);
    }

    /**
     * 根据字段名脱敏字段值
     *
     * @param fieldName  字段名
     * @param fieldValue 字段值
     * @return 脱敏后的字段值
     */
    private Object maskValue(String fieldName, Object fieldValue) {
        if (fieldValue == null) {
            return null;
        }

        String value = String.valueOf(fieldValue);
        if (StrUtil.isBlank(value)) {
            return value;
        }

        String lowerFieldName = StrUtil.lowerFirst(fieldName);

        if (StrUtil.containsAnyIgnoreCase(lowerFieldName, "mobile", "phone", "telephone")) {
            return DesensitizedUtil.mobilePhone(value);
        }
        if (StrUtil.containsAnyIgnoreCase(lowerFieldName, "idCard", "certificateNo", "certNo")) {
            return DesensitizedUtil.idCardNum(value, 6, 4);
        }
        if (StrUtil.containsAnyIgnoreCase(lowerFieldName, "bankCard", "cardNo")) {
            return DesensitizedUtil.bankCard(value);
        }
        if (StrUtil.containsAnyIgnoreCase(lowerFieldName, "email")) {
            return DesensitizedUtil.email(value);
        }
        if (StrUtil.containsAnyIgnoreCase(lowerFieldName, "password", "pwd", "secret", "privateKey", "apiKey")) {
            return "******";
        }
        if (StrUtil.containsAnyIgnoreCase(lowerFieldName, "token")) {
            return maskToken(value);
        }

        return fieldValue;
    }

    /**
     * 脱敏Token
     *
     * @param token Token
     * @return 脱敏后的Token
     */
    private String maskToken(String token) {
        if (StrUtil.length(token) <= 10) {
            return "******";
        }
        return StrUtil.subPre(token, 6) + "******" + StrUtil.subSuf(token, token.length() - 4);
    }

}
```

使用示例：

```java
Map<String, Object> maskedPayload = kafkaMessageMaskService.mask(payload);
log.info("Kafka消息内容摘要，messageId={}，payload={}", messageId, maskedPayload);
```

消息脱敏要求如下：

1. 日志、接口返回、管理后台展示必须脱敏。
2. 密码、Token、密钥类字段不做部分展示，直接隐藏。
3. 原始消息是否落库应按业务和合规要求决定。
4. 死信消息展示必须脱敏，但重放时应使用原始消息。
5. 脱敏逻辑应统一封装，不能散落在各个 Controller 和 Consumer 中。

### 消息加密

消息加密用于保护 Kafka 消息中的敏感内容。Kafka 的 SSL 只能保护传输链路，不能防止 Broker 存储、日志记录、死信落库、管理后台查看时泄露敏感字段。因此，对于必须进入 Kafka 的敏感字段，可以在业务层进行字段级加密。

推荐加密方式如下：

| 加密方式     | 说明                                                |
| ------------ | --------------------------------------------------- |
| 传输加密     | 使用 SSL / SASL_SSL，保护客户端到 Broker 链路       |
| 字段级加密   | 对消息体中的敏感字段加密                            |
| 整体消息加密 | 对整个 payload 加密，Consumer 解密后处理            |
| 密钥托管     | 密钥保存在 KMS、Vault、配置中心 Secret 或云密钥服务 |
| 密钥轮换     | 定期轮换密钥，并保留历史密钥解密能力                |

字段级加密适合只保护部分字段。整体消息加密安全性更强，但会降低可观测性、调试便利性和数据平台可用性。

文件位置：`src/main/java/io/github/atengk/kafka/security/KafkaMessageCryptoService.java`

该服务使用 AES-GCM 对字符串内容进行加密和解密，适合加密 Kafka 消息中的敏感字段。密钥应从环境变量、Secret 或密钥服务中读取。

```java
package io.github.atengk.kafka.security;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Kafka 消息加密服务
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
public class KafkaMessageCryptoService {

    private static final String AES = "AES";
    private static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKeySpec secretKeySpec;

    /**
     * 创建Kafka消息加密服务
     *
     * @param base64Key Base64编码后的AES密钥
     */
    public KafkaMessageCryptoService(@Value("${app.kafka.security.message-aes-key}") String base64Key) {
        if (StrUtil.isBlank(base64Key)) {
            throw new IllegalArgumentException("Kafka消息加密密钥不能为空");
        }
        byte[] keyBytes = Base64.decode(base64Key);
        this.secretKeySpec = new SecretKeySpec(keyBytes, AES);
    }

    /**
     * 加密明文
     *
     * @param plainText 明文
     * @return Base64编码后的密文
     */
    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }

        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] result = new byte[iv.length + cipherBytes.length];

            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(cipherBytes, 0, result, iv.length, cipherBytes.length);

            return Base64.encode(result);
        } catch (Exception e) {
            log.error("Kafka消息字段加密失败", e);
            throw new IllegalStateException("Kafka消息字段加密失败", e);
        }
    }

    /**
     * 解密密文
     *
     * @param cipherText Base64编码后的密文
     * @return 明文
     */
    public String decrypt(String cipherText) {
        if (StrUtil.isBlank(cipherText)) {
            return cipherText;
        }

        try {
            byte[] encryptedBytes = Base64.decode(cipherText);
            byte[] iv = new byte[IV_LENGTH];
            byte[] payload = new byte[encryptedBytes.length - IV_LENGTH];

            System.arraycopy(encryptedBytes, 0, iv, 0, IV_LENGTH);
            System.arraycopy(encryptedBytes, IV_LENGTH, payload, 0, payload.length);

            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] plainBytes = cipher.doFinal(payload);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Kafka消息字段解密失败", e);
            throw new IllegalStateException("Kafka消息字段解密失败", e);
        }
    }

}
```

配置示例：

```yaml
app:
  kafka:
    security:
      # Base64编码后的AES密钥，生产环境必须通过Secret或配置中心密文注入
      message-aes-key: ${KAFKA_MESSAGE_AES_KEY}
```

生成本地测试密钥：

```bash
# 生成32字节AES密钥，并输出Base64
openssl rand -base64 32
```

消息加密要求如下：

1. Kafka SSL 不能替代业务字段级加密。
2. 密钥不能写入代码仓库。
3. 密钥不能打印到日志。
4. 加密字段应在消息 Schema 中明确标识。
5. 密钥轮换时需要支持旧消息解密。
6. 消息加密会增加 CPU 成本，应压测验证。
7. 加密字段会降低查询和调试便利性，应只加密确有必要的字段。

### 权限最小化

权限最小化用于确保每个应用、用户和运维工具只拥有完成职责所需的最小 Kafka 权限。生产环境不应所有服务共享同一个 Kafka 超级用户。

权限最小化设计如下：

| 对象          | 权限建议                                                     |
| ------------- | ------------------------------------------------------------ |
| Producer 应用 | 只授予目标 Topic 的 `Write` 和 `Describe` 权限               |
| Consumer 应用 | 只授予目标 Topic 的 `Read`、`Describe` 和指定 Group 的 `Read` 权限 |
| 管理后台      | 只授予查询、死信重放所需权限，不授予删除 Topic 权限          |
| 运维平台      | 按审批授予 Topic 创建、配置修改权限                          |
| 本地开发账号  | 只能访问开发环境 Kafka                                       |
| CI/CD 账号    | 只允许发布流程需要的 Schema 或 Topic 操作                    |

ACL 示例：

```bash
# 授予订单服务写订单Topic权限
kafka-acls.sh \
  --bootstrap-server kafka-01:9093 \
  --command-config admin.properties \
  --add \
  --allow-principal User:order-service \
  --operation Write \
  --operation Describe \
  --topic ateng.order.created.topic

# 授予积分服务消费订单Topic和消费组权限
kafka-acls.sh \
  --bootstrap-server kafka-01:9093 \
  --command-config admin.properties \
  --add \
  --allow-principal User:points-service \
  --operation Read \
  --operation Describe \
  --topic ateng.order.created.topic

kafka-acls.sh \
  --bootstrap-server kafka-01:9093 \
  --command-config admin.properties \
  --add \
  --allow-principal User:points-service \
  --operation Read \
  --group ateng.points.consumer.group
```

权限最小化要求如下：

1. 一个应用使用一个 Kafka 用户。
2. 不同环境使用不同 Kafka 用户。
3. 生产账号不能用于本地调试。
4. 普通业务应用不授予 Topic 删除权限。
5. 不建议使用通配符授予所有 Topic 权限。
6. 权限变更必须审批并记录审计。
7. 离职、系统下线或 Topic 废弃后应及时回收权限。

### 审计日志

审计日志用于记录 Kafka 管理操作、消息重放、Offset 重置、Topic 修改、权限变更和敏感消息查看行为。审计日志不是普通业务日志，它需要长期保存并支持追溯。

需要审计的操作如下：

| 操作                     | 风险                   |
| ------------------------ | ---------------------- |
| 发送管理测试消息         | 可能触发真实业务       |
| 死信消息重放             | 可能重复执行业务       |
| Offset 重置              | 可能重复消费或跳过消息 |
| Topic 创建 / 修改 / 删除 | 影响消息链路           |
| 查看敏感消息体           | 可能涉及数据泄露       |
| 导出消息记录             | 可能涉及批量数据外泄   |
| 修改重试或降级配置       | 影响消息可靠性         |
| ACL 权限变更             | 影响访问边界           |

审计表建议如下：

```sql
CREATE TABLE kafka_operation_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    operator VARCHAR(128) NOT NULL COMMENT '操作人',
    operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
    resource_type VARCHAR(64) NOT NULL COMMENT '资源类型',
    resource_id VARCHAR(255) DEFAULT NULL COMMENT '资源标识',
    cluster_name VARCHAR(64) DEFAULT NULL COMMENT 'Kafka集群名称',
    topic VARCHAR(128) DEFAULT NULL COMMENT 'Topic名称',
    request_summary TEXT DEFAULT NULL COMMENT '请求摘要',
    result_status VARCHAR(32) NOT NULL COMMENT '结果状态：SUCCESS、FAILED',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '异常信息',
    client_ip VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
    user_agent VARCHAR(500) DEFAULT NULL COMMENT 'User-Agent',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    KEY idx_operator_time (operator, create_time),
    KEY idx_operation_type_time (operation_type, create_time),
    KEY idx_resource (resource_type, resource_id)
) COMMENT='Kafka操作审计日志表';
```

审计日志要求如下：

1. 高危操作必须记录操作人、时间、资源、原因和结果。
2. 审计日志不能被普通业务用户修改或删除。
3. 请求内容应保存摘要，不保存明文密码、Token、证书和完整敏感消息体。
4. 审计日志应支持按操作人、资源、时间范围查询。
5. 生产环境死信重放、Offset 重置、Topic 删除必须强制审计。
6. 审计数据保留周期应符合公司合规要求。

### 数据保留周期

数据保留周期用于规定 Kafka Topic、发送记录、消费记录、幂等记录、死信记录、重试记录和审计日志的保存时间。保留周期需要在可追溯、可补偿和存储成本之间取得平衡。

推荐保留周期如下：

| 数据类型          | 建议保留周期                      |
| ----------------- | --------------------------------- |
| 普通业务 Topic    | 3 到 7 天                         |
| 核心业务 Topic    | 7 到 30 天                        |
| Retry Topic       | 大于最大重试周期                  |
| Dead Letter Topic | 14 到 30 天或更长                 |
| 发送记录表        | 30 到 90 天                       |
| 消费记录表        | 30 到 90 天                       |
| 幂等记录表        | 大于 Topic 保留周期和最大重放周期 |
| 死信消息表        | 30 到 180 天，未处理记录不清理    |
| 重试记录表        | 成功记录 30 天，失败记录更长      |
| 审计日志表        | 按合规要求，通常 180 天或更长     |

保留周期设计要求如下：

1. 幂等记录保留时间不能小于死信可重放时间。
2. Topic 消息过期后，不能再依赖 Kafka 补历史数据。
3. 未处理死信不应自动删除。
4. 审计日志保留周期应满足安全合规要求。
5. 存储压力较大时，应优先归档，而不是直接删除核心记录。
6. 保留周期变更必须评估补偿能力和审计要求。

### 数据删除策略

数据删除策略用于规范 Kafka 消息、数据库记录、死信消息、审计日志和敏感数据的清理方式。删除策略必须避免误删未处理失败消息、未重放死信或仍需幂等保护的数据。

Kafka Topic 数据删除主要依赖保留策略：

```bash
# 修改Topic保留时间为7天
kafka-configs.sh \
  --bootstrap-server kafka-01:9092 \
  --entity-type topics \
  --entity-name ateng.order.created.topic \
  --alter \
  --add-config retention.ms=604800000
```

数据库分批删除示例：

```sql
DELETE FROM kafka_send_record
WHERE send_status = 'SUCCESS'
  AND update_time < DATE_SUB(NOW(), INTERVAL 90 DAY)
LIMIT 1000;
```

死信记录清理示例：

```sql
DELETE FROM kafka_dead_letter_message
WHERE process_status IN ('REPLAYED', 'IGNORED')
  AND update_time < DATE_SUB(NOW(), INTERVAL 180 DAY)
LIMIT 1000;
```

不允许直接清理的记录：

| 数据                  | 原因                       |
| --------------------- | -------------------------- |
| `PENDING` 死信        | 仍待处理                   |
| `FAILED` 死信         | 仍需排查                   |
| `RETRYING` 重试记录   | 仍在重试链路               |
| `PROCESSING` 消费记录 | 可能是异常中断，需要补偿   |
| 未过期幂等记录        | 清理后可能导致重复执行业务 |
| 审计日志              | 受合规周期约束             |

数据删除要求如下：

1. 删除任务必须分批执行。
2. 删除前必须过滤状态。
3. 删除任务必须记录删除数量和执行结果。
4. 核心数据建议先归档再删除。
5. 删除策略应有灰度执行和回滚预案。
6. 敏感数据删除应符合数据安全和隐私合规要求。

## 常见问题

本章节用于说明 Kafka 开发和运维中的常见问题，包括消息发送失败、消息重复消费、消息丢失、消息堆积、消费延迟过高、反序列化失败、Offset 提交异常、Rebalance 频繁和死信消息过多。

常见问题排查应遵循统一思路：先确认影响范围，再确认是 Producer、Broker、Consumer、业务逻辑、数据库、网络还是配置问题，最后根据日志、指标、数据库记录和 Kafka 命令定位根因。

### 消息发送失败

消息发送失败通常发生在 Producer 到 Kafka Broker 的写入阶段。常见原因包括 Broker 不可用、Topic 不存在、权限不足、认证失败、序列化失败、消息过大、请求超时和缓冲区不足。

常见原因如下：

| 原因                | 表现                                 |
| ------------------- | ------------------------------------ |
| Kafka 地址错误      | 连接超时、无法获取元数据             |
| Topic 不存在        | 发送失败或等待元数据超时             |
| ACL 权限不足        | `TopicAuthorizationException`        |
| SASL / SSL 配置错误 | 认证失败、SSL 握手失败               |
| 序列化失败          | `SerializationException`             |
| 消息过大            | `RecordTooLargeException`            |
| Broker 不可用       | 请求超时、重试耗尽                   |
| 缓冲区不足          | `TimeoutException`、等待 buffer 超时 |

排查命令：

```bash
# 查看Topic是否存在
kafka-topics.sh \
  --bootstrap-server kafka-01:9092 \
  --list | grep ateng.order.created.topic

# 查看Topic详情
kafka-topics.sh \
  --bootstrap-server kafka-01:9092 \
  --describe \
  --topic ateng.order.created.topic
```

排查建议：

1. 检查 `bootstrap-servers` 是否正确。
2. 检查 Topic 是否存在。
3. 检查 Kafka 用户是否有 `Write` 和 `Describe` 权限。
4. 检查 SASL / SSL 配置和证书。
5. 检查 Producer 日志中的异常类。
6. 检查消息体大小是否超过限制。
7. 检查 Broker 是否存在 Offline Partition 或磁盘满。
8. 发送失败后应记录发送记录并进入补偿流程。

### 消息重复消费

消息重复消费是 Kafka Consumer 的正常风险之一。即使配置正确，也可能因为 Offset 提交失败、Consumer 重启、Rebalance、业务处理超时、死信重放或 Producer 重发导致重复。

常见原因如下：

| 原因                     | 说明                           |
| ------------------------ | ------------------------------ |
| 业务成功但 Offset 未提交 | 重启后从旧 Offset 再次消费     |
| Rebalance                | 分区转移导致未提交消息重新消费 |
| 手动重置 Offset          | 人工操作导致历史消息重放       |
| 死信重放                 | 死信消息重新进入主 Topic       |
| Producer 重发            | 上游补偿任务重复发送           |
| 自动提交时机不当         | Offset 提交和业务处理不一致    |

处理方式如下：

1. Consumer 必须做幂等。
2. 使用 `messageId + consumerGroup` 做消息级幂等。
3. 使用业务唯一键做业务级幂等。
4. 成功处理后再提交 Offset。
5. 重复消息识别后应安全跳过并提交 Offset。
6. 死信重放前必须确认幂等能力。

幂等判断示例：

```text
收到消息
  -> 查询 messageId + consumerGroup
  -> 已 SUCCESS，跳过
  -> 未处理，插入 PROCESSING
  -> 执行业务
  -> 成功更新 SUCCESS
```

结论是：不要试图完全避免重复消费，而是要保证重复消费不会产生重复业务副作用。

### 消息丢失

消息丢失通常不是单一原因造成的，可能发生在 Producer、Broker 或 Consumer 任意环节。排查消息丢失时，应先确认消息是否发送成功、是否写入 Kafka、是否被消费、是否被提交 Offset、是否进入死信。

常见原因如下：

| 环节     | 原因                          |
| -------- | ----------------------------- |
| Producer | 发送失败但未记录补偿          |
| Producer | `acks=0` 或可靠性配置过低     |
| Broker   | 副本不足、非同步副本选主      |
| Broker   | Topic 保留时间过短，消息过期  |
| Consumer | 自动提交 Offset 后业务失败    |
| Consumer | catch 异常后吞掉并提交 Offset |
| Consumer | 批量消费部分失败但整体提交    |
| 运维     | Offset 被重置到最新位置       |

排查路径：

```text
messageId
  -> 查询 kafka_send_record
  -> 查询 Producer 日志
  -> 查询 Topic 是否有对应 offset
  -> 查询 kafka_consume_record
  -> 查询 kafka_dead_letter_message
  -> 查询 Offset 重置审计日志
```

防护建议：

1. Producer 使用 `acks=all`。
2. 核心 Topic 使用副本数 3 和 `min.insync.replicas=2`。
3. Producer 最终失败必须写入本地消息表或失败记录。
4. Consumer 关闭自动提交。
5. Consumer 业务成功后再提交 Offset。
6. 异常不能吞掉。
7. 批量消费失败项必须可靠落库或死信。
8. Topic 保留时间应大于补偿窗口。

### 消息堆积

消息堆积表示 Consumer 消费速度低于 Producer 生产速度。堆积通常体现为 Consumer Lag 持续增长。

常见原因如下：

| 原因              | 说明                       |
| ----------------- | -------------------------- |
| Consumer 实例不足 | 消费能力不够               |
| Topic 分区数不足  | 限制最大并发               |
| 业务处理慢        | 数据库、下游接口或锁竞争慢 |
| 消费失败阻塞      | 同一消息反复重试           |
| 下游服务异常      | Consumer 等待下游响应      |
| 单分区热点        | 某个 Key 消息过多          |
| Rebalance 频繁    | 消费组不稳定               |
| 批量参数不合理    | 单批处理太慢或拉取太少     |

排查命令：

```bash
kafka-consumer-groups.sh \
  --bootstrap-server kafka-01:9092 \
  --group ateng.order.consumer.group \
  --describe
```

处理建议：

1. 先确认是否所有分区都堆积。
2. 如果单分区堆积，检查热点 Key。
3. 如果所有分区堆积，检查 Consumer 实例、并发和业务耗时。
4. 扩容前确认 Topic 分区数是否足够。
5. 检查消费失败和重试日志。
6. 检查数据库慢 SQL 和下游接口延迟。
7. 对非核心 Consumer 可暂停或降级，优先恢复核心链路。

### 消费延迟过高

消费延迟过高表示消息从写入 Kafka 到被 Consumer 成功处理的时间过长。它可能由消息堆积导致，也可能由单条业务处理慢导致。

常见原因如下：

| 原因            | 说明                       |
| --------------- | -------------------------- |
| Consumer Lag 高 | 消息排队等待消费           |
| 单条处理慢      | 数据库、接口、锁、事务耗时 |
| 批量过大        | 单批处理时间过长           |
| 下游限流        | Consumer 等待下游          |
| 重试间隔长      | 消息多次重试后才成功       |
| Rebalance 频繁  | 消费持续被打断             |
| GC 停顿         | 应用暂停处理               |

排查建议：

1. 查看 Consumer Lag。
2. 查看消费耗时 P95 / P99。
3. 查看数据库慢 SQL。
4. 查看下游接口耗时和错误率。
5. 查看 Consumer GC 日志。
6. 查看 Rebalance 次数。
7. 查看重试和死信情况。

优化建议：

1. 增加 Consumer 并发或实例数。
2. 优化业务处理逻辑。
3. 降低 `max.poll.records`，避免单批过慢。
4. 对下游调用增加超时、限流和降级。
5. 批量写库替代逐条写库。
6. 对热点 Key 做业务拆分。

### 反序列化失败

反序列化失败通常发生在消息进入 Listener 方法之前。常见原因包括 JSON 格式错误、类型不匹配、字段类型变更、缺少默认类型、可信包配置错误或 Producer / Consumer 消息模型不一致。

常见原因如下：

| 原因                    | 说明                                        |
| ----------------------- | ------------------------------------------- |
| JSON 格式错误           | 消息不是合法 JSON                           |
| 类型不匹配              | Consumer 目标类型和实际消息不一致           |
| 字段类型变更            | 例如字符串改成数字                          |
| 缺少默认类型            | `spring.json.value.default.type` 未配置     |
| 可信包错误              | `spring.json.trusted.packages` 不包含目标类 |
| Java 类型 Header 不一致 | Producer 和 Consumer 类型映射不一致         |
| 历史消息不兼容          | 新 Consumer 无法读取旧消息                  |

推荐配置：

```yaml
spring:
  kafka:
    consumer:
      # 使用错误处理反序列化器，避免反序列化异常卡住消费
      value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
      properties:
        # 委托JsonDeserializer处理
        spring.deserializer.value.delegate.class: org.springframework.kafka.support.serializer.JsonDeserializer
        # 配置信任包
        spring.json.trusted.packages: io.github.atengk.kafka.model,java.util,java.lang
        # 配置默认类型
        spring.json.value.default.type: io.github.atengk.kafka.model.MessageEnvelope
```

处理建议：

1. 使用 `ErrorHandlingDeserializer`。
2. 将反序列化异常设置为不可重试。
3. 进入死信或失败记录，避免阻塞主消费。
4. 检查 Producer 和 Consumer 消息模型是否一致。
5. 检查历史消息兼容性。
6. 不兼容字段变更应使用新字段、新消息类型或新 Topic。

### Offset 提交异常

Offset 提交异常会导致 Consumer 重启或 Rebalance 后重复消费。它不一定导致消息丢失，但会扩大重复消费窗口。

常见原因如下：

| 原因               | 说明                        |
| ------------------ | --------------------------- |
| Rebalance 期间提交 | 分区已被撤销，提交失败      |
| Consumer 已关闭    | 应用正在停机                |
| Broker 短暂不可用  | Offset 提交请求失败         |
| 处理时间过长       | 超过 `max.poll.interval.ms` |
| 网络异常           | Consumer 与 Broker 通信失败 |
| 自动提交配置混乱   | 自动提交和手动提交混用      |

排查建议：

1. 查看 Consumer 日志中的 CommitFailedException。
2. 查看是否发生 Rebalance。
3. 查看消费耗时是否超过 `max.poll.interval.ms`。
4. 查看 Broker 和网络状态。
5. 确认是否关闭自动提交。
6. 检查是否在异步线程中错误提交 Offset。

处理建议：

1. 业务成功后尽快提交 Offset。
2. 避免长时间处理阻塞 Consumer 线程。
3. 批量消费控制单批数量。
4. 使用幂等兜底重复消费。
5. 优雅停机，减少关闭期间提交失败。
6. 不要混用自动提交和手动提交。

### Rebalance 频繁

Rebalance 频繁会导致消费暂停、Lag 波动和重复消费增加。它通常表示 Consumer Group 不稳定。

常见原因如下：

| 原因              | 说明                                                     |
| ----------------- | -------------------------------------------------------- |
| 应用频繁发布      | Consumer 实例反复加入和离开                              |
| Consumer 处理过慢 | 超过 `max.poll.interval.ms`                              |
| JVM GC 时间过长   | 心跳或 poll 延迟                                         |
| 网络抖动          | 心跳超时                                                 |
| Pod 频繁重启      | Kubernetes 健康检查或资源不足                            |
| 扩缩容频繁        | 消费组成员变化                                           |
| 参数不合理        | `session.timeout.ms`、`heartbeat.interval.ms` 配置不合理 |

处理建议：

1. 使用 `CooperativeStickyAssignor`。
2. 降低 `max.poll.records`，缩短单批处理时间。
3. 优化业务处理和数据库访问。
4. 调整 `max.poll.interval.ms`。
5. 检查 JVM GC 和容器内存。
6. 优化 Kubernetes 探针和资源限制。
7. 发布时使用滚动发布和优雅停机。
8. 避免频繁自动扩缩容。

推荐配置：

```yaml
spring:
  kafka:
    consumer:
      properties:
        # 使用协作式再均衡策略
        partition.assignment.strategy: org.apache.kafka.clients.consumer.CooperativeStickyAssignor

        # 会话超时时间
        session.timeout.ms: 45000

        # 心跳间隔
        heartbeat.interval.ms: 15000

        # 最大poll间隔
        max.poll.interval.ms: 300000
```

### 死信消息过多

死信消息过多说明大量消息无法通过正常消费和重试恢复。它通常不是死信机制本身的问题，而是消息格式、业务状态、下游服务、版本兼容或代码逻辑存在问题。

常见原因如下：

| 原因           | 说明                                |
| -------------- | ----------------------------------- |
| 消息格式错误   | Consumer 无法反序列化               |
| 业务参数缺失   | 消息缺少必要字段                    |
| 业务状态非法   | 当前状态不允许执行消息动作          |
| 下游长期不可用 | 重试耗尽后进入死信                  |
| 版本不兼容     | Producer 新消息旧 Consumer 无法处理 |
| 幂等逻辑异常   | 幂等表冲突或状态处理错误            |
| 重试次数过少   | 短暂故障未恢复就进入死信            |
| 代码缺陷       | Consumer 逻辑异常                   |

处理流程建议：

```text
发现死信增长
  -> 按 sourceTopic 和 exceptionClass 分类
  -> 查看最近发布和配置变更
  -> 抽样查看死信消息摘要
  -> 判断是否格式错误、业务错误或下游错误
  -> 修复根因
  -> 小批量重放验证
  -> 批量重放剩余死信
  -> 观察失败率和死信增长是否恢复
```

处理建议：

1. 先分类统计死信异常类型。
2. 不要直接批量重放。
3. 修复根因后先小批量重放。
4. 重放前确认 Consumer 幂等有效。
5. 对版本不兼容问题，先升级 Consumer。
6. 对下游故障问题，等待下游恢复后重放。
7. 对消息格式错误问题，应修复 Producer 或增加兼容逻辑。
8. 死信增长必须接入告警和审计。

常见问题处理总原则是：发送失败看 Producer 和 Broker，重复消费看 Offset 和幂等，消息丢失看提交时机和可靠性配置，消息堆积看 Lag 和消费能力，死信过多看异常分类和消息兼容性。

## 项目交付

本章节用于说明 Kafka 项目交付前需要统一的开发规范、配置规范、Topic 申请规范、日志规范、测试验收标准、部署验收标准和运维交接清单。项目交付不是代码合并完成，而是需要保证消息链路可运行、可观测、可回滚、可补偿、可审计。

### 开发规范

开发规范用于统一 Kafka Producer、Consumer、Topic、消息模型、异常处理、幂等处理和日志格式，避免不同开发人员各自实现导致后期维护困难。

Kafka 开发应遵循以下规范：

| 规范项              | 要求                                                         |
| ------------------- | ------------------------------------------------------------ |
| Topic 常量          | 所有 Topic 统一定义在常量类中，不允许代码中散落字符串        |
| Consumer Group 常量 | 所有消费组统一定义，生产环境 Group 名称必须稳定              |
| 消息模型            | 使用统一消息信封，包含 `messageId`、`messageType`、`version`、`bizKey`、`traceId`、`sendTime` |
| Message Key         | 顺序消息必须使用稳定业务 Key，例如 `ORDER:{orderId}`         |
| Header              | 统一传递 `x-message-id`、`x-trace-id`、`x-message-type`      |
| Producer            | 统一封装发送组件，不直接在业务代码中散落 `KafkaTemplate.send` |
| Consumer            | 监听器只负责接收消息，业务处理下沉到 Service                 |
| Offset              | 重要业务关闭自动提交，业务成功后手动提交                     |
| 幂等                | 重要业务必须有幂等控制                                       |
| 异常                | 不吞异常，交给统一错误处理器                                 |
| 死信                | 超过重试次数或不可恢复异常进入死信                           |
| 日志                | 必须打印 Topic、Partition、Offset、Key、messageId、traceId   |

代码结构建议如下：

```text
src/main/java/io/github/atengk/kafka
├── config                  # Kafka配置类
├── constant                # Topic、Group、Header常量
├── controller              # 管理接口
├── consumer                # 消费者监听器
├── producer                # 生产者封装
├── model                   # 消息模型
├── service                 # 业务服务
├── idempotent              # 幂等处理
├── retry                   # 重试处理
├── deadletter              # 死信处理
├── monitor                 # 指标监控
├── security                # 脱敏、加密、安全
└── support                 # 通用工具
```

交付要求如下：

1. 不允许业务代码直接写死 Topic。
2. 不允许生产环境 Consumer Group 使用随机值。
3. 不允许重要业务开启自动提交 Offset。
4. 不允许 Consumer 捕获异常后直接提交 Offset。
5. 不允许死信消息无人处理。
6. 不允许管理接口绕过权限控制。
7. 不允许日志打印密码、Token、密钥和完整敏感消息体。

### 配置规范

配置规范用于统一 Kafka 地址、Producer、Consumer、Listener、重试、死信、安全认证和多环境配置。配置项应通过 `application.yml`、环境变量、配置中心或 Secret 管理，不应硬编码。

配置分层建议如下：

| 配置层                  | 说明                             |
| ----------------------- | -------------------------------- |
| `application.yml`       | 通用默认配置                     |
| `application-local.yml` | 本地开发配置                     |
| `application-test.yml`  | 测试环境配置                     |
| `application-prod.yml`  | 生产环境配置                     |
| 环境变量                | Kafka 地址、账号、密码、证书路径 |
| 配置中心                | Topic 白名单、重试参数、降级开关 |
| Secret                  | 密码、Token、证书密码、密钥      |

配置命名规范如下：

```yaml
app:
  kafka:
    # Kafka集群名称
    cluster-name: biz

    # 是否启用发送记录
    enable-send-record: true

    # 是否启用消费记录
    enable-consume-record: true

    # 是否启用消费幂等
    enable-idempotent: true

    retry:
      # 最大重试次数
      max-retry-count: 3

      # 重试间隔毫秒
      interval-ms: 2000

    dead-letter:
      # 是否启用死信
      enabled: true

      # 通用死信Topic
      topic: ateng.common.dead-letter.topic
```

环境变量命名示例：

```text
KAFKA_BOOTSTRAP_SERVERS
KAFKA_USERNAME
KAFKA_PASSWORD
KAFKA_SECURITY_PROTOCOL
KAFKA_SASL_MECHANISM
KAFKA_SSL_TRUSTSTORE_LOCATION
KAFKA_SSL_TRUSTSTORE_PASSWORD
KAFKA_MESSAGE_AES_KEY
```

配置规范要求如下：

1. 生产环境 Kafka 地址必须通过环境变量或配置中心注入。
2. Kafka 密码、证书密码、加密密钥必须通过 Secret 注入。
3. Topic、Group、重试次数、死信 Topic 等配置必须有说明。
4. 生产环境不建议动态修改 Consumer Group。
5. 配置变更必须记录操作人、时间、原因和变更内容。
6. 高风险配置变更需要灰度验证。

### Topic 申请规范

Topic 申请规范用于统一 Topic 的命名、用途、分区数、副本数、保留时间、负责人和权限配置。生产 Topic 不应由业务应用随意自动创建。

Topic 申请字段建议如下：

| 字段       | 说明                     |
| ---------- | ------------------------ |
| Topic 名称 | 符合命名规范             |
| 所属系统   | 例如 `order-service`     |
| 业务域     | 订单、支付、用户、库存等 |
| 消息类型   | 例如 `ORDER_CREATED`     |
| 生产者     | 哪些服务写入             |
| 消费者     | 哪些服务消费             |
| 分区数     | 根据吞吐和并发规划       |
| 副本数     | 生产环境建议 3           |
| 保留时间   | 根据补偿和审计需求设置   |
| 清理策略   | `delete` 或 `compact`    |
| 消息大小   | 预估平均大小和最大大小   |
| 顺序要求   | 是否按 Key 局部顺序      |
| 负责人     | 业务和技术负责人         |
| 告警要求   | Lag、失败、死信阈值      |

Topic 命名建议：

```text
{系统标识}.{业务域}.{事件名称}.topic
```

示例：

```text
ateng.order.created.topic
ateng.payment.success.topic
ateng.inventory.changed.topic
```

Retry Topic 和 Dead Letter Topic 命名：

```text
ateng.order.created.retry.1m
ateng.order.created.retry.5m
ateng.order.created.dead-letter
```

Topic 申请要求如下：

1. 生产 Topic 创建必须审批。
2. Topic 名称一旦上线不应随意修改。
3. 分区数增加前必须评估顺序消息影响。
4. 核心业务 Topic 必须配置多副本。
5. 死信 Topic 保留时间应长于普通 Topic。
6. Topic 负责人必须明确，便于故障处理。

### 日志规范

日志规范用于统一 Kafka 消息发送、消费、异常、重试、死信和重放日志格式。日志必须能通过 `messageId`、`traceId`、`topic`、`partition`、`offset` 定位完整链路。

Producer 成功日志：

```text
Kafka消息发送成功，cluster={}，topic={}，partition={}，offset={}，key={}，messageId={}，messageType={}，traceId={}，costMillis={}
```

Consumer 成功日志：

```text
Kafka消息消费成功，cluster={}，topic={}，partition={}，offset={}，key={}，messageId={}，consumerGroup={}，traceId={}，costMillis={}
```

消费异常日志：

```text
Kafka消息消费失败，cluster={}，topic={}，partition={}，offset={}，key={}，messageId={}，consumerGroup={}，traceId={}，exceptionClass={}，errorMessage={}
```

死信日志：

```text
Kafka消息进入死信，sourceTopic={}，deadLetterTopic={}，partition={}，offset={}，key={}，messageId={}，consumerGroup={}，traceId={}，retryCount={}，reason={}
```

日志规范要求如下：

1. `info` 用于发送成功、消费成功、Offset 提交成功。
2. `warn` 用于重试、重复消费跳过、降级、死信重放。
3. `error` 用于发送失败、消费失败、死信投递、重放失败。
4. 异常日志必须打印堆栈。
5. 不允许打印完整敏感消息体。
6. 日志中必须包含 `messageId` 和 `traceId`。
7. 批量消费必须打印总数、成功数、失败数和耗时。

### 测试验收标准

测试验收标准用于确认 Kafka 项目在交付前已经覆盖核心功能、异常场景、可靠性、幂等、死信和性能。

验收项如下：

| 类别          | 验收标准                                      |
| ------------- | --------------------------------------------- |
| Producer 测试 | 能发送指定 Topic、Key、Header、Payload        |
| Consumer 测试 | 能正常消费并手动提交 Offset                   |
| 序列化测试    | JSON、Avro 或 Protobuf 能正常序列化和反序列化 |
| 反序列化异常  | 格式错误消息能进入异常处理或死信              |
| 重试测试      | 可重试异常按配置重试                          |
| 不可重试异常  | 不重复重试，进入死信或失败记录                |
| 死信测试      | 超过最大重试次数后进入死信 Topic              |
| 幂等测试      | 重复消息不会重复执行业务                      |
| Offset 测试   | 业务成功后提交，失败不提交                    |
| 批量测试      | 批量消费成功、部分失败处理符合设计            |
| 安全测试      | 无权限用户不能读写 Topic                      |
| 性能测试      | 满足目标 TPS、延迟和 Lag 要求                 |

测试验收要求如下：

1. 单元测试必须通过。
2. Producer 和 Consumer 集成测试必须通过。
3. 死信和重试流程必须可验证。
4. 幂等测试必须覆盖重复消费和并发消费。
5. 性能压测必须有测试报告。
6. 核心业务 Topic 必须完成端到端测试。
7. 测试环境 Topic 和 Group 不得影响生产。

### 部署验收标准

部署验收标准用于确认应用在目标环境部署后能够正常连接 Kafka、发送消息、消费消息、暴露监控、记录日志并支持故障恢复。

验收项如下：

| 类别          | 验收标准                            |
| ------------- | ----------------------------------- |
| 应用启动      | Spring Boot 应用启动成功            |
| Kafka 连接    | 能连接目标 Kafka 集群               |
| Topic 检查    | 核心 Topic 已存在且配置正确         |
| Producer 验证 | 测试消息发送成功                    |
| Consumer 验证 | 测试消息消费成功                    |
| Offset 验证   | 消费组 Offset 正常提交              |
| 健康检查      | `/actuator/health` 正常             |
| 指标暴露      | `/actuator/prometheus` 正常         |
| 日志采集      | 日志平台可检索 messageId 和 traceId |
| 告警配置      | Lag、失败、死信告警已启用           |
| 安全认证      | SASL / SSL 配置验证通过             |
| 权限验证      | 无权限 Topic 读写被拒绝             |
| 优雅停机      | 发布或重启时无明显异常              |

部署验收命令示例：

```bash
# 检查应用健康状态
curl http://localhost:8080/actuator/health

# 检查Prometheus指标
curl http://localhost:8080/actuator/prometheus | grep kafka

# 查看消费组状态
kafka-consumer-groups.sh \
  --bootstrap-server kafka-01:9092 \
  --group ateng.order.consumer.group \
  --describe
```

部署验收要求如下：

1. 生产发布前必须验证 Kafka 连接和权限。
2. 发布后观察至少一个完整消息链路。
3. 发布后观察 Consumer Lag、消费失败率和死信增长。
4. 监控和告警必须在发布前完成。
5. 回滚镜像和配置必须可用。
6. 部署过程必须记录版本、镜像、配置和操作人。

### 运维交接清单

运维交接清单用于保证项目上线后，运维、开发和值班人员知道如何查看状态、处理堆积、重置 Offset、重放死信和回滚应用。

交接内容如下：

| 项目                | 内容                                        |
| ------------------- | ------------------------------------------- |
| 应用信息            | 应用名、部署环境、实例数、负责人            |
| Kafka 集群          | 集群地址、认证方式、集群负责人              |
| Topic 清单          | Topic 名称、用途、分区数、副本数、保留时间  |
| Consumer Group 清单 | Group 名称、用途、所属服务                  |
| 告警规则            | Lag、失败、死信、Broker 告警阈值            |
| Grafana 面板        | 监控面板地址和使用说明                      |
| 日志查询            | 日志平台检索字段：messageId、traceId、topic |
| 数据库表            | 发送记录、消费记录、死信表、重试表          |
| 常用命令            | Topic、Group、Offset、Lag 查询命令          |
| 死信处理            | 查询、分析、重放、忽略流程                  |
| 回滚方式            | 镜像回滚、配置回滚、注意事项                |
| 应急联系人          | 开发、运维、DBA、中间件负责人               |

交接要求如下：

1. 必须提供 Topic 和 Consumer Group 清单。
2. 必须提供死信处理流程。
3. 必须提供 Offset 重置审批和操作流程。
4. 必须提供监控面板和告警说明。
5. 必须提供回滚方案。
6. 必须明确核心业务消息的负责人。

## 附录

本章节提供 Kafka 项目常用依赖、配置、Docker Compose、Topic 命令、Consumer Group 命令、Offset 操作命令、推荐参数配置和参考资料，便于开发、测试、部署和运维直接复制使用。

### Maven 依赖示例

以下 Maven 依赖适用于 Spring Boot 3 + Spring Kafka 项目。版本建议由父工程或依赖管理统一控制。

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供管理接口和健康检查接口 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Kafka，用于Producer、Consumer、KafkaTemplate和Listener容器 -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>

    <!-- Spring Boot Actuator，用于健康检查和运行指标 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Prometheus指标导出，用于监控系统采集 -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>

    <!-- Hutool工具类，用于字符串、集合、脱敏、时间等常用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
    </dependency>

    <!-- Lombok，用于简化DTO、日志和构造器代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- MyBatis-Plus，用于消息记录表、死信表、重试表等CRUD -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>${mybatis-plus.version}</version>
    </dependency>

    <!-- MySQL驱动，用于连接业务数据库 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Redis支持，用于短期幂等、延迟队列或缓存 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- Spring Boot测试基础依赖 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Spring Kafka测试工具，支持Embedded Kafka -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Testcontainers Kafka，用于更接近真实环境的集成测试 -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>kafka</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

使用说明：

1. `spring-kafka` 是核心依赖。
2. `actuator + micrometer-registry-prometheus` 用于监控。
3. `spring-kafka-test` 适合 Embedded Kafka 测试。
4. `testcontainers-kafka` 适合 Docker 化集成测试。
5. Avro、Protobuf、Schema Registry 相关依赖按项目需要单独添加。

### application.yml 示例

以下配置适合作为通用 Spring Boot Kafka 项目的基础配置。生产环境需要将 Kafka 地址、账号、密码、证书路径和加密密钥改为环境变量或 Secret 注入。

```yaml
server:
  # 应用端口
  port: 8080

spring:
  application:
    # 应用名称
    name: kafka-develop-demo

  kafka:
    # Kafka Broker地址，生产环境通过环境变量注入
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}

    producer:
      # Key序列化器
      key-serializer: org.apache.kafka.common.serialization.StringSerializer

      # Value序列化器
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

      # 等待所有同步副本确认
      acks: all

      # Producer发送失败重试次数
      retries: 3

      properties:
        # 开启幂等Producer
        enable.idempotence: true

        # 开启幂等时建议不超过5
        max.in.flight.requests.per.connection: 5

        # 批次大小，提升吞吐
        batch.size: 32768

        # 批量等待时间，兼顾吞吐和延迟
        linger.ms: 20

        # 消息压缩方式
        compression.type: lz4

        # 不添加Java类型Header，降低服务耦合
        spring.json.add.type.headers: false

    consumer:
      # 默认消费组，实际业务建议在@KafkaListener中显式指定
      group-id: ateng-default-consumer-group

      # 关闭自动提交Offset
      enable-auto-commit: false

      # 无提交Offset时从最早消息开始消费，本地和测试常用
      auto-offset-reset: earliest

      # Key反序列化器
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer

      # 使用错误处理反序列化器包装真实反序列化器
      value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer

      properties:
        # 委托JsonDeserializer反序列化消息体
        spring.deserializer.value.delegate.class: org.springframework.kafka.support.serializer.JsonDeserializer

        # 可信包路径，生产环境不建议使用*
        spring.json.trusted.packages: io.github.atengk.kafka.model,java.util,java.lang

        # 默认消息类型
        spring.json.value.default.type: io.github.atengk.kafka.model.MessageEnvelope

        # 单次poll最大拉取数量
        max.poll.records: 100

        # 两次poll最大间隔，业务处理必须在该时间内完成
        max.poll.interval.ms: 300000

        # 使用协作式Sticky再均衡策略，降低Rebalance影响
        partition.assignment.strategy: org.apache.kafka.clients.consumer.CooperativeStickyAssignor

    listener:
      # 手动立即提交Offset
      ack-mode: manual_immediate

      # 默认并发数
      concurrency: 3

      # 单条监听模式，批量消费可改为batch
      type: single

management:
  endpoints:
    web:
      exposure:
        # 暴露健康检查和Prometheus指标
        include: health,info,prometheus

  metrics:
    tags:
      # 指标默认应用标签
      application: ${spring.application.name}

app:
  kafka:
    # 当前Kafka集群名称
    cluster-name: biz

    # 是否启用消费幂等
    enable-idempotent: true

    retry:
      # 最大重试次数
      max-retry-count: 3

      # 重试间隔毫秒
      interval-ms: 2000

    dead-letter:
      # 是否启用死信
      enabled: true

      # 通用死信Topic
      topic: ateng.common.dead-letter.topic
```

配置说明：

1. `enable-auto-commit=false` 是重要业务推荐配置。
2. `ack-mode=manual_immediate` 表示业务手动确认后立即提交 Offset。
3. `ErrorHandlingDeserializer` 用于接管反序列化异常。
4. `CooperativeStickyAssignor` 用于降低 Rebalance 停顿。
5. `messageId`、`traceId`、死信和幂等仍需要业务代码配合。

### Docker Compose 示例

以下 Docker Compose 示例用于本地启动 Kafka 和 Kafka UI。示例使用单节点 Kafka，适合本地开发，不适合生产部署。

```yaml
services:
  kafka:
    # Apache Kafka镜像
    image: apache/kafka:4.0.0
    container_name: ateng-kafka
    ports:
      # Kafka客户端访问端口
      - "9092:9092"
    environment:
      # Kafka节点ID
      KAFKA_NODE_ID: 1

      # KRaft模式角色
      KAFKA_PROCESS_ROLES: broker,controller

      # Controller投票配置
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@ateng-kafka:9093

      # 监听器配置
      KAFKA_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093

      # 对外暴露地址，本地访问使用localhost
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092

      # 监听器协议映射
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT

      # Controller监听器名称
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER

      # Broker间通信监听器
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT

      # 单节点环境下Offset主题副本数设为1
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

      # 单节点环境下事务状态主题副本数设为1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1

      # 单节点环境下事务状态最小ISR设为1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
    networks:
      - kafka-net

  kafka-ui:
    # Kafka UI管理工具
    image: provectuslabs/kafka-ui:latest
    container_name: ateng-kafka-ui
    ports:
      # Kafka UI访问端口
      - "8088:8080"
    environment:
      # 集群名称
      KAFKA_CLUSTERS_0_NAME: local

      # Kafka地址，容器内访问使用服务名
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: ateng-kafka:9092
    depends_on:
      - kafka
    networks:
      - kafka-net

networks:
  kafka-net:
    name: kafka-net
```

启动命令：

```bash
# 启动Kafka和Kafka UI
docker compose up -d

# 查看容器
docker ps

# 查看Kafka日志
docker logs -f ateng-kafka

# 访问Kafka UI
open http://localhost:8088
```

命令说明：

1. `docker compose up -d` 后台启动服务。
2. `ateng-kafka:9092` 是容器网络内部地址。
3. `localhost:9092` 是本机应用访问地址。
4. Kafka UI 本地访问地址为 `http://localhost:8088`。

### Topic 命令示例

以下命令用于 Topic 创建、查看、修改和配置查询。Apache Kafka 官方运维文档也提供了 Topic、Consumer Group 和 Offset 管理命令示例，Offset 重置要求指定范围，并建议确保对应消费者实例处于非活跃状态后再执行。([Apache Kafka](https://kafka.apache.org/42/operations/basic-kafka-operations/?utm_source=chatgpt.com))

创建 Topic：

```bash
kafka-topics.sh \
  --bootstrap-server kafka-01:9092 \
  --create \
  --topic ateng.order.created.topic \
  --partitions 6 \
  --replication-factor 3 \
  --config min.insync.replicas=2 \
  --config retention.ms=604800000 \
  --config cleanup.policy=delete
```

查看 Topic 列表：

```bash
kafka-topics.sh \
  --bootstrap-server kafka-01:9092 \
  --list
```

查看 Topic 详情：

```bash
kafka-topics.sh \
  --bootstrap-server kafka-01:9092 \
  --describe \
  --topic ateng.order.created.topic
```

增加分区：

```bash
kafka-topics.sh \
  --bootstrap-server kafka-01:9092 \
  --alter \
  --topic ateng.order.created.topic \
  --partitions 12
```

查看 Topic 配置：

```bash
kafka-configs.sh \
  --bootstrap-server kafka-01:9092 \
  --entity-type topics \
  --entity-name ateng.order.created.topic \
  --describe
```

修改保留时间：

```bash
kafka-configs.sh \
  --bootstrap-server kafka-01:9092 \
  --entity-type topics \
  --entity-name ateng.order.created.topic \
  --alter \
  --add-config retention.ms=1209600000
```

命令说明：

1. `--bootstrap-server` 指定 Kafka Broker 地址。
2. `--partitions` 指定分区数，增加分区前需要评估顺序消息影响。
3. `--replication-factor` 指定副本数，不能大于 Broker 数量。
4. `retention.ms` 控制消息保留时间。
5. `cleanup.policy=delete` 表示按保留策略删除历史消息。

### Consumer Group 命令示例

以下命令用于查看 Consumer Group 列表、状态、消费进度和 Lag。

查看消费组列表：

```bash
kafka-consumer-groups.sh \
  --bootstrap-server kafka-01:9092 \
  --list
```

查看消费组详情：

```bash
kafka-consumer-groups.sh \
  --bootstrap-server kafka-01:9092 \
  --group ateng.order.consumer.group \
  --describe
```

查看消费组状态：

```bash
kafka-consumer-groups.sh \
  --bootstrap-server kafka-01:9092 \
  --group ateng.order.consumer.group \
  --state
```

删除消费组：

```bash
kafka-consumer-groups.sh \
  --bootstrap-server kafka-01:9092 \
  --delete \
  --group ateng.temp.consumer.group
```

命令说明：

1. `--describe` 可以查看 `CURRENT-OFFSET`、`LOG-END-OFFSET` 和 `LAG`。
2. `--state` 可以查看消费组状态和分配策略。
3. 删除消费组属于高风险操作，只能删除无活跃成员的消费组。
4. 生产环境删除消费组必须审批。

### Offset 操作命令示例

以下命令用于 Offset 查询和重置。Offset 重置是高风险操作，可能导致重复消费或跳过消息，生产环境执行前必须暂停 Consumer、备份当前 Offset，并先执行 `--dry-run`。

查询当前 Offset：

```bash
kafka-consumer-groups.sh \
  --bootstrap-server kafka-01:9092 \
  --group ateng.order.consumer.group \
  --describe
```

预览重置到最早位置：

```bash
kafka-consumer-groups.sh \
  --bootstrap-server kafka-01:9092 \
  --group ateng.order.consumer.group \
  --topic ateng.order.created.topic \
  --reset-offsets \
  --to-earliest \
  --dry-run
```

执行重置到最早位置：

```bash
kafka-consumer-groups.sh \
  --bootstrap-server kafka-01:9092 \
  --group ateng.order.consumer.group \
  --topic ateng.order.created.topic \
  --reset-offsets \
  --to-earliest \
  --execute
```

执行重置到最新位置：

```bash
kafka-consumer-groups.sh \
  --bootstrap-server kafka-01:9092 \
  --group ateng.order.consumer.group \
  --topic ateng.order.created.topic \
  --reset-offsets \
  --to-latest \
  --execute
```

执行重置到指定 Offset：

```bash
kafka-consumer-groups.sh \
  --bootstrap-server kafka-01:9092 \
  --group ateng.order.consumer.group \
  --topic ateng.order.created.topic:0 \
  --reset-offsets \
  --to-offset 1024 \
  --execute
```

按时间重置：

```bash
kafka-consumer-groups.sh \
  --bootstrap-server kafka-01:9092 \
  --group ateng.order.consumer.group \
  --topic ateng.order.created.topic \
  --reset-offsets \
  --to-datetime 2026-05-11T10:00:00.000 \
  --execute
```

Offset 操作要求如下：

1. 执行前必须确认环境、集群、Topic 和 Group。
2. 执行前先使用 `--dry-run`。
3. 执行前暂停对应 Consumer。
4. 执行后观察 Lag、失败率和死信。
5. 核心业务 Offset 重置必须审批和审计。
6. 重置后可能产生重复消费，Consumer 必须具备幂等能力。

### 推荐参数配置

以下参数适合作为常见 Spring Boot Kafka 项目的起始配置，最终值需要结合业务吞吐、消息大小、延迟要求和压测结果调整。Producer 配置中，`bootstrap.servers` 用于客户端建立初始连接，官方配置说明建议配置多个地址以提高可用性；`batch.size` 会影响 Producer 聚合批次和吞吐表现。([Confluent 文档](https://docs.confluent.io/platform/current/installation/configuration/producer-configs.html?utm_source=chatgpt.com))

Producer 推荐参数：

| 参数                                    | 推荐值             | 说明                   |
| --------------------------------------- | ------------------ | ---------------------- |
| `acks`                                  | `all`              | 核心业务推荐           |
| `retries`                               | `3` 到 `5`         | 处理临时发送失败       |
| `enable.idempotence`                    | `true`             | 开启幂等 Producer      |
| `max.in.flight.requests.per.connection` | `5`                | 开启幂等时建议不超过 5 |
| `batch.size`                            | `32768` 或 `65536` | 吞吐优先可调大         |
| `linger.ms`                             | `10` 到 `50`       | 提升批量发送效果       |
| `compression.type`                      | `lz4` 或 `zstd`    | JSON 消息推荐压缩      |
| `delivery.timeout.ms`                   | `120000`           | 发送完整生命周期超时   |
| `request.timeout.ms`                    | `30000`            | 单次请求超时           |

Consumer 推荐参数：

| 参数                            | 推荐值                      | 说明                                |
| ------------------------------- | --------------------------- | ----------------------------------- |
| `enable-auto-commit`            | `false`                     | 重要业务关闭自动提交                |
| `auto-offset-reset`             | `earliest` / `latest`       | 本地测试用 earliest，生产按业务决定 |
| `max.poll.records`              | `100` 到 `500`              | 批量消费可调大                      |
| `max.poll.interval.ms`          | `300000` 到 `600000`        | 处理慢时调大                        |
| `session.timeout.ms`            | `45000`                     | 会话超时                            |
| `heartbeat.interval.ms`         | `15000`                     | 心跳间隔                            |
| `partition.assignment.strategy` | `CooperativeStickyAssignor` | 降低 Rebalance 影响                 |
| `isolation.level`               | `read_committed`            | 读取事务消息时使用                  |

Listener 推荐参数：

| 参数          | 推荐值             | 说明                       |
| ------------- | ------------------ | -------------------------- |
| `ack-mode`    | `manual_immediate` | 业务成功后手动提交         |
| `concurrency` | 不超过分区数       | 提升消费并发               |
| `type`        | `single` / `batch` | 普通业务单条，吞吐场景批量 |

Topic 推荐参数：

| 参数                  | 推荐值      | 说明                   |
| --------------------- | ----------- | ---------------------- |
| `partitions`          | `3` 到 `12` | 按吞吐和并发规划       |
| `replication.factor`  | `3`         | 生产核心 Topic 推荐    |
| `min.insync.replicas` | `2`         | 配合 `acks=all`        |
| `retention.ms`        | `604800000` | 默认 7 天              |
| `cleanup.policy`      | `delete`    | 普通业务 Topic         |
| `compression.type`    | `producer`  | 使用 Producer 压缩策略 |

配置建议：

1. 核心业务优先可靠性，再优化吞吐。
2. 日志和埋点类 Topic 可以更偏吞吐。
3. 顺序消息不要盲目增加分区。
4. 批量消费要同步调整 `max.poll.interval.ms`。
5. 所有推荐值都需要压测验证。

### 参考资料

Kafka 项目开发和运维应优先参考官方文档、Spring Kafka 文档和当前公司中间件平台规范。Spring for Apache Kafka 官方参考文档覆盖 `KafkaTemplate`、`@KafkaListener`、错误处理、事务、测试等 Spring 集成能力；Apache Kafka 官方运维文档覆盖 Topic、Consumer Group、Offset 等基础运维命令；Confluent 配置参考提供 Producer、Consumer、Broker、Topic 等参数说明。([Home](https://docs.spring.io/spring-kafka/reference/reference.html?utm_source=chatgpt.com))

推荐资料如下：

| 类型                     | 资料                                             |
| ------------------------ | ------------------------------------------------ |
| Kafka 基础与运维         | Apache Kafka 官方文档                            |
| Consumer Group 和 Offset | Apache Kafka Basic Kafka Operations              |
| Spring Boot 集成         | Spring for Apache Kafka Reference                |
| Producer / Consumer 参数 | Confluent Kafka Configuration Reference          |
| Schema 管理              | Confluent Schema Registry 文档                   |
| 监控指标                 | Micrometer、Prometheus、Grafana 官方文档         |
| 容器部署                 | Docker、Kubernetes 官方文档                      |
| 安全认证                 | Apache Kafka Security 文档                       |
| 企业规范                 | 公司中间件平台规范、Topic 申请规范、权限申请规范 |

项目落地时，参考资料不能替代内部规范。最终配置应以当前 Kafka 集群版本、Spring Boot 版本、公司安全要求、生产压测结果和中间件团队建议为准。