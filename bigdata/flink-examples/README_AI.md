# Flink

Flink 是面向实时数据处理、批流一体计算和状态化流处理的分布式计算框架。本开发文档主要面向 Java 技术栈下的 Flink 作业开发，覆盖作业工程结构、数据源接入、数据转换、状态管理、容错机制、性能优化、部署运维和生产实践等内容。Apache Flink 官方将其定位为用于有界流和无界流上进行状态化计算的框架和分布式处理引擎，并强调其可运行在常见集群环境中，支持内存级性能和大规模计算。([flink.apache.org](https://flink.apache.org/))

## 项目概述

本章节用于说明 Flink 项目的整体定位、适用场景、技术边界和建设目标，帮助开发人员在进入具体编码前先明确项目要解决的问题、采用的技术路线以及生产交付标准。

### Flink 项目定位

Flink 项目定位为企业级实时计算与批流一体数据处理平台中的核心计算引擎，主要负责从 Kafka、数据库 CDC、文件系统、数据湖或其他消息系统中持续读取数据，并通过 DataStream API、Table API 或 SQL 完成清洗、转换、聚合、关联、状态计算和结果输出。

在 Java 开发场景中，Flink 通常不是一个单独运行的简单程序，而是由多个可独立提交、可独立扩缩容、可独立恢复的计算作业组成。每个作业围绕一个明确的数据处理目标设计，例如订单实时清洗、用户行为实时统计、实时风控规则计算、MySQL CDC 数据同步或实时宽表构建。

从工程角度看，Flink 项目的核心价值包括：

| 能力         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 实时处理     | 面向持续到达的数据流，提供低延迟的数据处理能力               |
| 状态计算     | 支持窗口、聚合、去重、规则匹配、维表关联等有状态业务逻辑     |
| 容错恢复     | 通过 Checkpoint、Savepoint、状态后端和重启策略保障作业可靠运行 |
| 批流统一     | 同一套技术体系可以处理无界实时流和有界批数据                 |
| 横向扩展     | 通过并行度、Task Slot 和集群资源实现计算能力扩展             |
| 端到端一致性 | 在 Source、Flink 状态和 Sink 支持的前提下实现 Exactly Once 语义 |

### 典型应用场景

Flink 适合处理数据持续产生、对时效性要求较高、业务逻辑需要状态维护或需要高可靠性的计算任务。官方列出的典型方向包括事件驱动应用、流批分析和数据管道 ETL 等，其中事件驱动应用会消费一个或多个事件流，并根据事件触发计算、状态更新或外部动作。([flink.apache.org](https://flink.apache.org/))

常见业务场景如下：

| 场景         | 说明                                                         | 常见输入                        | 常见输出                         |
| ------------ | ------------------------------------------------------------ | ------------------------------- | -------------------------------- |
| 实时数据清洗 | 对原始日志、埋点、交易消息进行格式校验、字段补全、异常过滤   | Kafka、日志文件、Socket         | Kafka、数据库、数据湖            |
| 实时指标计算 | 按分钟、小时、天维度计算 UV、PV、GMV、订单数、成功率等指标   | Kafka、CDC                      | Redis、Doris、ClickHouse、Kafka  |
| 实时风控     | 对交易、登录、支付、设备行为进行规则判断和风险评分           | Kafka、MySQL CDC、Redis 维表    | 告警系统、风控结果表             |
| 实时宽表     | 将主流数据与维表、配置流或历史数据进行关联，生成面向分析的宽表 | Kafka、CDC、HBase、Redis        | Kafka、Doris、Iceberg、Hive      |
| 数据同步     | 将数据库变更数据实时同步到下游数据仓库、搜索引擎或湖仓表     | MySQL CDC、PostgreSQL CDC       | Kafka、Doris、StarRocks、Iceberg |
| 实时告警     | 根据指标阈值、规则配置或异常模式触发告警                     | Kafka、指标系统                 | 企业微信、钉钉、邮件、告警平台   |
| 离线补数     | 对指定时间范围的历史数据进行重新计算，修复漏算或错算数据     | 文件、Hive、Iceberg、Kafka 回放 | 数仓表、指标表、结果表           |

在实际项目中，应优先区分“实时主链路”和“离线修复链路”。实时主链路关注低延迟、稳定性和连续运行；离线修复链路关注数据完整性、可重复执行和结果覆盖策略。

### 实时计算与批处理边界

Flink 的核心抽象是“流”。从 Flink 视角看，实时计算处理的是无界流，批处理处理的是有界流。无界流有开始但没有明确结束，需要持续处理；有界流有明确开始和结束，可以在读取完整数据后进行计算，处理有界流通常也被称为批处理。([flink.apache.org](https://flink.apache.org/flink-architecture.html))

在项目设计中，可以按以下标准区分实时计算与批处理：

| 维度     | 实时计算                            | 批处理                               |
| -------- | ----------------------------------- | ------------------------------------ |
| 数据形态 | 无界流，数据持续到达                | 有界数据，数据范围明确               |
| 处理方式 | 持续运行，边到达边处理              | 周期运行或一次性运行                 |
| 延迟要求 | 秒级、分钟级                        | 分钟级、小时级、天级                 |
| 状态特点 | 长时间维护状态，需要 TTL 和状态后端 | 状态生命周期较短，通常随任务结束释放 |
| 容错重点 | Checkpoint、重启策略、端到端一致性  | 任务重跑、结果覆盖、幂等写入         |
| 典型任务 | 实时指标、风控、告警、实时同步      | 历史补数、周期报表、离线修复         |

边界判断并不是只看数据源，而是看数据是否有明确处理范围。例如，从 Kafka 消费最近 7 天数据并在处理完成后退出，可以按有界流设计；从文件系统持续监听新增文件并不断处理，则更接近无界流。

在工程实践中，建议将实时作业和补数作业分开设计。实时作业使用稳定的消费位点、Checkpoint 和状态恢复机制；补数作业使用明确的时间范围、批量参数、幂等输出和可重复执行策略，避免补数逻辑影响在线实时链路。

### 项目技术栈

Flink Java 项目的技术栈应围绕“开发、构建、运行、数据接入、数据输出、监控运维”几个层次设计。Apache Flink 官方项目配置文档也说明，Flink 应用至少依赖 Flink API，同时通常还需要 Connector、Format、测试依赖以及用户自定义函数依赖。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/configuration/overview/))

推荐技术栈如下：

| 分类      | 推荐技术                          | 说明                                                         |
| --------- | --------------------------------- | ------------------------------------------------------------ |
| 开发语言  | Java                              | 作为主开发语言，适合企业级工程化、类型约束和复杂业务逻辑     |
| JDK 版本  | Java 17                           | Flink 2.0.0 起默认使用 Java 17，官方也推荐使用 Java 17 运行 Flink；Java 21 为实验性支持，需谨慎评估。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/deployment/java_compatibility/)) |
| 构建工具  | Maven                             | 统一依赖管理、模块管理、打包和 Shade 配置                    |
| Flink API | DataStream API、Table API、SQL    | DataStream 适合复杂逻辑，SQL 适合声明式计算和指标开发        |
| 消息系统  | Kafka                             | 常见实时数据入口和结果中转系统                               |
| CDC       | Flink CDC、Debezium               | 用于数据库变更数据捕获                                       |
| 数据库    | MySQL、PostgreSQL                 | 常见业务库、配置库和维表来源                                 |
| 缓存      | Redis                             | 用于维表缓存、规则缓存、实时结果查询                         |
| OLAP 存储 | Doris、StarRocks、ClickHouse      | 用于实时明细、聚合结果和分析查询                             |
| 数据湖    | Iceberg、Hudi、Hive               | 用于批流一体、湖仓表和历史数据存储                           |
| 序列化    | JSON、Avro、Protobuf              | 根据数据规范、性能和 Schema 演进要求选择                     |
| 日志      | SLF4J、Logback、Log4j2            | 用于作业运行日志、异常日志和排查记录                         |
| 监控      | Flink Web UI、Prometheus、Grafana | 用于作业状态、延迟、反压、Checkpoint 和资源监控              |
| 部署      | Kubernetes、YARN、Standalone      | 根据企业基础设施选择运行环境                                 |

版本选择应遵循公司统一基线。若当前没有历史包袱，可以优先选择当前稳定 Flink 版本与 Java 17 组合；若已有 Flink 1.17、1.18、1.20 等存量集群，则应以集群版本、Connector 兼容性、状态兼容性和 Savepoint 迁移能力为准。

### 项目整体目标

Flink 项目的整体目标不是简单完成一个可运行的流式任务，而是建立一套可开发、可测试、可部署、可恢复、可监控、可演进的实时计算工程体系。

项目建设目标如下：

| 目标       | 说明                                                         |
| ---------- | ------------------------------------------------------------ |
| 规范化开发 | 统一包结构、配置方式、参数解析、日志规范、异常处理和作业入口 |
| 稳定运行   | 合理配置并行度、Slot、Checkpoint、重启策略、状态后端和资源参数 |
| 数据可靠   | 明确 Source、Flink 状态和 Sink 的一致性边界，避免重复、丢失和乱序导致的数据错误 |
| 易于扩展   | 支持多作业、多数据源、多输出端和多环境配置扩展               |
| 易于排查   | 通过日志、指标、告警和运行手册快速定位数据延迟、反压、Checkpoint 失败、Sink 异常等问题 |
| 易于交付   | 提供标准化构建、部署、升级、回滚和运维文档                   |
| 可持续演进 | 支持 Flink 版本升级、Connector 替换、状态迁移和业务规则变更  |

最终交付的 Flink 项目应满足以下标准：

1. 本地可以运行和调试。
2. 测试环境可以通过固定参数提交作业。
3. 生产环境可以通过标准化脚本、YARN 或 Kubernetes 部署。
4. 作业失败后可以基于 Checkpoint 或 Savepoint 恢复。
5. 关键指标可以被监控，异常情况可以及时告警。
6. 数据输出具备幂等、事务或补偿机制。
7. 核心业务逻辑有单元测试、集成测试或数据回放验证。

## Flink 基础知识

本章节用于说明 Flink 开发前必须理解的基础概念，包括核心特性、运行架构、资源模型、数据流模型和一致性语义。掌握这些内容后，后续章节中的 Source、Transformation、Window、State、Checkpoint、部署和性能优化才有明确的上下文。

### Flink 核心特性

Flink 的核心特性可以概括为状态化、低延迟、高吞吐、事件时间、容错恢复和批流统一。官方能力说明中明确包含 Exactly-once 状态一致性、事件时间处理、迟到数据处理、SQL、DataStream API、ProcessFunction、高可用、Savepoint、大状态支持、增量 Checkpoint、低延迟和高吞吐等能力。([flink.apache.org](https://flink.apache.org/))

核心特性如下：

| 特性                    | 说明                                                         |
| ----------------------- | ------------------------------------------------------------ |
| 状态化计算              | 支持在算子中维护状态，用于聚合、去重、Join、规则匹配、窗口计算等场景 |
| 事件时间处理            | 可以按照事件真实发生时间进行计算，而不是只依赖系统处理时间   |
| Watermark 机制          | 用于衡量事件时间推进情况，处理乱序和迟到数据                 |
| Exactly Once 状态一致性 | 在 Checkpoint 和可重放 Source 支持下，保障状态更新的一致性   |
| 高吞吐低延迟            | 通过内存计算、算子链、并行执行和本地状态访问提升处理性能     |
| 批流一体                | 使用统一的数据流抽象处理无界流和有界流                       |
| 高可用与故障恢复        | 支持 Checkpoint、Savepoint、重启策略和 HA 配置               |
| 灵活部署                | 支持 Standalone、YARN、Kubernetes 等运行方式                 |
| 多层 API                | 支持 DataStream API、Table API、SQL 和 ProcessFunction       |
| Connector 生态          | 支持 Kafka、JDBC、文件系统、Elasticsearch、Hive、HBase、数据湖等外部系统集成 |

在 Java 项目中，应根据业务复杂度选择 API。规则复杂、状态复杂、需要精细控制时间和状态时，优先使用 DataStream API 或 ProcessFunction；指标口径清晰、偏 SQL 化的数据加工场景，可以使用 Table API 或 Flink SQL。

### Flink 运行架构

Flink 是分布式系统，需要对计算资源进行分配和管理才能执行流式应用。Flink 可以集成 Hadoop YARN、Kubernetes 等资源管理系统，也可以以 Standalone 集群或类库方式运行。官方架构文档说明，Flink Runtime 主要由 JobManager 和一个或多个 TaskManager 组成。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/concepts/flink-architecture/))

整体运行流程可以概括为：

1. 开发人员编写 Java Flink 作业，并通过 `main()` 方法构建执行逻辑。
2. 客户端将用户程序转换为可执行的数据流图，并提交给 Flink 集群。
3. JobManager 接收作业，生成执行计划，负责调度、Checkpoint 协调和失败恢复。
4. TaskManager 注册到 JobManager，并提供 Task Slot 作为执行资源。
5. JobManager 将任务分发到 TaskManager 的 Slot 中执行。
6. TaskManager 之间通过网络交换数据流。
7. 作业运行过程中持续进行状态更新、Checkpoint、Metrics 上报和异常处理。

可以用以下逻辑理解 Flink 架构：

```text
Client
  |
  | 提交作业
  v
JobManager
  |-- ResourceManager：管理资源和 Slot
  |-- Dispatcher：接收作业提交，提供 REST 接口和 Web UI
  |-- JobMaster：管理单个 JobGraph 的执行
  |
  | 调度 Task
  v
TaskManager 1  ---- 数据交换 ---- TaskManager 2
  |                             |
  |-- Slot                      |-- Slot
  |-- Slot                      |-- Slot
```

在生产环境中，JobManager 和 TaskManager 一般运行在 Kubernetes Pod、YARN Container 或独立服务器进程中。开发人员通常不直接操作 TaskManager 内部线程，而是通过并行度、Slot 数量、资源配置、Checkpoint 配置和部署模式间接控制作业运行行为。

### JobManager 与 TaskManager

JobManager 是 Flink 作业的协调者，TaskManager 是 Flink 作业的执行者。JobManager 负责调度、故障处理、Checkpoint 协调和恢复控制；TaskManager 负责执行具体 Task，并在不同 Task 之间缓冲和交换数据。官方文档说明，JobManager 会决定何时调度任务、响应任务完成或执行失败、协调 Checkpoint，并在失败时协调恢复。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/concepts/flink-architecture/))

JobManager 的主要职责如下：

| 组件                   | 作用                                                         |
| ---------------------- | ------------------------------------------------------------ |
| ResourceManager        | 管理资源申请、释放和 Slot 分配                               |
| Dispatcher             | 提供 REST 接口，接收作业提交，启动 JobMaster，并运行 Flink Web UI |
| JobMaster              | 管理单个 JobGraph 的执行，每个 Job 通常对应一个 JobMaster    |
| Checkpoint Coordinator | 协调各个算子的 Checkpoint 屏障、状态快照和完成确认           |
| Scheduler              | 根据执行计划、并行度和资源情况调度 Task                      |

TaskManager 的主要职责如下：

| 职责      | 说明                                                     |
| --------- | -------------------------------------------------------- |
| 执行 Task | 执行 Source、Map、Filter、KeyBy、Window、Sink 等算子任务 |
| 提供 Slot | 通过 Task Slot 向集群提供可调度执行资源                  |
| 数据交换  | 在上游和下游 Task 之间进行网络传输、缓冲和反压传播       |
| 状态访问  | 维护本地状态，配合状态后端完成快照和恢复                 |
| 指标上报  | 上报吞吐、延迟、反压、内存、GC、Checkpoint 等运行指标    |

在排查问题时，应先判断问题发生在协调层还是执行层。作业提交失败、调度失败、Checkpoint 协调异常，通常优先查看 JobManager 日志；算子异常、反压、OOM、数据解析失败、Sink 写入失败，通常优先查看 TaskManager 日志。

### Task、Slot 与并行度

Task 是 Flink 执行层面的基本工作单元，Slot 是 TaskManager 提供的资源调度单元，并行度决定同一个算子会被拆分为多少个并行子任务。官方架构文档说明，TaskManager 是 JVM 进程，可以在不同线程中执行一个或多个 Subtask；Task Slot 是 TaskManager 接收任务的资源控制单位。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/concepts/flink-architecture/))

核心关系如下：

| 概念        | 说明                                              |
| ----------- | ------------------------------------------------- |
| Operator    | 逻辑算子，例如 Source、Map、Filter、Window、Sink  |
| Subtask     | 算子按照并行度拆分后的并行实例                    |
| Task        | Flink 对多个算子 Subtask 进行链式合并后的执行单元 |
| Task Slot   | TaskManager 中用于承载 Task 的资源调度单位        |
| Parallelism | 算子或作业的并行度，决定同时运行的 Subtask 数量   |

示例说明：

```text
作业并行度 = 4

Kafka Source      -> Map 清洗        -> KeyBy/Window 聚合 -> Kafka Sink
Source Subtask 1     Map Subtask 1      Window Subtask 1     Sink Subtask 1
Source Subtask 2     Map Subtask 2      Window Subtask 2     Sink Subtask 2
Source Subtask 3     Map Subtask 3      Window Subtask 3     Sink Subtask 3
Source Subtask 4     Map Subtask 4      Window Subtask 4     Sink Subtask 4
```

Flink 会将部分上下游算子链在一起形成 Task。算子链可以减少线程切换、数据缓冲和网络传输开销，从而提升吞吐并降低延迟。官方文档也说明，Flink 会将算子 Subtask 链接成 Task，每个 Task 由一个线程执行，算子链是一种减少线程间交接和缓冲开销的优化。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/concepts/flink-architecture/))

Slot 与并行度的常见理解方式如下：

1. 并行度控制“任务拆成几份”。
2. Slot 控制“集群最多能同时承载多少份任务”。
3. TaskManager 控制“这些 Slot 分布在哪些进程或容器上”。
4. CPU、内存、网络、状态大小和数据倾斜共同决定实际处理能力。

需要注意，Slot 主要隔离的是 Managed Memory 等资源，不等同于严格 CPU 隔离。官方文档明确说明，Task Slot 代表 TaskManager 资源的固定子集，但当前 Slot 不做 CPU 隔离，主要用于分离 Managed Memory。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/concepts/flink-architecture/))

### 数据流模型

Flink 的数据流模型可以理解为由 Source、Transformation 和 Sink 组成的有向无环数据流图。Source 负责读取外部数据，Transformation 负责转换和计算，Sink 负责写出处理结果。

典型模型如下：

```text
Source -> Transformation -> Transformation -> Sink
```

更具体的实时作业模型如下：

```text
Kafka Source
  -> JSON 解析
  -> 脏数据侧输出
  -> keyBy 用户或订单
  -> 窗口聚合 / 状态处理 / 维表关联
  -> 结果转换
  -> Kafka / Redis / Doris / JDBC Sink
```

在 Flink 中，数据流图中的每个节点都是一个算子，每条边表示数据传输关系。根据是否发生分区变化，数据传输可以分为窄依赖和宽依赖：

| 类型     | 示例                         | 说明                                           |
| -------- | ---------------------------- | ---------------------------------------------- |
| 窄依赖   | map、filter、flatMap         | 通常不改变数据分区，适合算子链优化             |
| 宽依赖   | keyBy、rebalance、rescale    | 会触发数据重新分发，可能产生网络传输和数据倾斜 |
| 双流依赖 | connect、join、interval join | 涉及多个输入流，通常需要考虑状态和时间语义     |
| 侧输出   | side output                  | 用于输出脏数据、迟到数据、异常数据或分流结果   |

在开发时，应优先画出数据流模型，再编写代码。数据流模型应明确以下内容：

1. 数据从哪里来。
2. 每一步转换做什么。
3. 是否需要按 Key 分组。
4. 是否需要窗口。
5. 是否需要状态。
6. 如何处理乱序、迟到和脏数据。
7. 结果写到哪里。
8. 失败后如何恢复。

### 有界流与无界流

Flink 将所有数据都抽象为流，但流可以分为有界流和无界流。无界流有开始但没有明确结束，会持续产生数据，需要持续处理；有界流有明确开始和结束，可以读取完整数据后再计算。官方文档也说明，有界流处理通常被称为批处理。([flink.apache.org](https://flink.apache.org/flink-architecture.html))

有界流示例：

```text
读取 2026-05-01 至 2026-05-10 的历史订单文件
读取 Hive 表中指定分区的数据
读取 Kafka 中指定起止 Offset 的数据并处理完成后退出
执行一次历史 UV/PV 指标重算
```

无界流示例：

```text
持续消费 Kafka 用户行为日志
持续消费 MySQL Binlog CDC 数据
持续处理设备传感器上报数据
持续计算订单支付成功率
持续监听风控事件并触发告警
```

开发设计差异如下：

| 维度         | 有界流                     | 无界流                                     |
| ------------ | -------------------------- | ------------------------------------------ |
| 作业生命周期 | 处理完成后结束             | 长期运行                                   |
| 时间语义     | 可以按数据范围处理         | 通常需要 Event Time 和 Watermark           |
| 状态管理     | 状态生命周期较短           | 状态可能长期存在，需要 TTL 和清理策略      |
| 容错机制     | 可通过重跑恢复             | 依赖 Checkpoint 和重启策略恢复             |
| 输出策略     | 常见覆盖写、分区写、批量写 | 常见追加写、Upsert、幂等写、事务写         |
| 监控重点     | 成功率、耗时、输出条数     | 延迟、反压、Checkpoint、消费积压、状态大小 |

在 Java Flink 项目中，有界流一般用于补数、修复、历史回放和批量同步；无界流一般用于实时主链路。两类作业可以复用公共解析、转换、校验和写出逻辑，但入口参数、运行模式、状态策略和输出策略应分别设计。

### Exactly Once 语义

Exactly Once 是 Flink 中最容易被误解的概念之一。它并不简单表示“每条数据在物理上只执行一次”，而是表示在故障恢复后，每条数据对 Flink 管理状态的影响恰好一次。Flink 官方故障恢复说明中也强调，Exactly Once 的含义是每个事件对 Flink 管理的状态只产生一次影响，而不是事件在执行过程中绝对只被处理一次。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/learn-flink/fault_tolerance/?utm_source=chatgpt.com))

Flink 的 Exactly Once 通常分为三个层次：

| 层次                 | 说明                                                         |
| -------------------- | ------------------------------------------------------------ |
| Source 端一致性      | Source 必须支持数据重放，例如 Kafka Offset、文件位置、CDC 位点 |
| Flink 内部状态一致性 | Flink 通过 Checkpoint 保存算子状态和 Source 读取位置         |
| Sink 端一致性        | Sink 需要支持事务提交、两阶段提交、幂等写入或去重机制        |

内部状态一致性的基本过程如下：

```text
1. Flink 周期性触发 Checkpoint
2. Source 将当前消费位置纳入快照
3. 各个有状态算子保存当前状态
4. Checkpoint 成功后，状态和位点形成一致性快照
5. 作业失败后，从最近一次成功 Checkpoint 恢复
6. Source 从快照记录的位置重新消费
7. 算子状态恢复到快照时刻，继续处理后续数据
```

端到端 Exactly Once 的要求更高。官方故障恢复说明指出，要实现端到端 Exactly Once，Source 必须可重放，Sink 必须是事务型或幂等型。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/learn-flink/fault_tolerance/?utm_source=chatgpt.com))

常见场景判断如下：

| Source | Sink               | 可能语义                      | 说明                             |
| ------ | ------------------ | ----------------------------- | -------------------------------- |
| Kafka  | Kafka 事务 Sink    | Exactly Once                  | Source 可重放，Sink 支持事务     |
| Kafka  | 幂等 Upsert 数据库 | 接近 Exactly Once             | 依赖主键、版本号或业务唯一键幂等 |
| Kafka  | 普通 JDBC Insert   | At Least Once                 | 失败重放可能导致重复插入         |
| CDC    | Upsert Sink        | 接近 Exactly Once             | 依赖主键语义和下游写入一致性     |
| 文件   | 文件 Sink 提交机制 | Exactly Once 或 At Least Once | 取决于文件提交协议和恢复策略     |
| Socket | 任意 Sink          | 通常无法保证 Exactly Once     | Socket 不具备可靠重放能力        |

工程实践中不要只在代码中开启 Checkpoint 就声称实现了端到端 Exactly Once。必须同时确认：

1. Source 是否支持位点保存和重放。
2. Checkpoint 是否稳定成功。
3. 状态后端和 Checkpoint 存储是否可靠。
4. Sink 是否具备事务、幂等或去重能力。
5. 作业失败恢复后是否会重复写入。
6. 下游结果表是否有主键、唯一键或版本字段。
7. 是否有数据校验、补偿和修复机制。

在生产文档中，建议为每个 Flink 作业单独说明一致性级别，例如：

```text
作业名称：order-pay-realtime-stat-job
Source：Kafka，支持 Offset 重放
State：开启 Checkpoint，使用 RocksDB StateBackend
Sink：Doris Unique Key 表，按 order_id 幂等写入
一致性说明：Flink 内部状态可达到 Exactly Once；端到端依赖 Doris 主键幂等写入，整体按幂等 Exactly Once 设计。
```

这样可以避免将 Flink 内部状态一致性、Sink 写入一致性和业务结果一致性混为一谈。

以下继续展开你大纲中的 `Java 开发环境准备` 与 `项目工程结构` 两部分。

## Java 开发环境准备

本章节用于统一 Flink Java 项目的本地开发基线，包括 JDK、构建工具、Flink 版本、IDE、本地运行方式和日志配置。环境准备的目标不是只让作业能启动，而是保证本地开发、测试环境和生产集群之间的版本、依赖、配置和日志行为尽量一致。

### JDK 版本选择

Flink Java 项目应优先根据 Flink 大版本选择 JDK。对于新建项目，如果没有历史兼容包袱，建议使用 Java 17 作为默认 JDK。Apache Flink 当前稳定文档说明，Flink 2.0.0 起默认使用 Java 17，Java 17 也是推荐运行版本；Java 21 属于实验性支持，需要额外验证依赖、Connector、序列化和 JVM 参数兼容性。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/deployment/java_compatibility/?utm_source=chatgpt.com))

推荐选择如下：

| Flink 版本   | 推荐 JDK         | 说明                                                         |
| ------------ | ---------------- | ------------------------------------------------------------ |
| Flink 2.x    | Java 17          | 新项目优先选择，适合当前主线版本                             |
| Flink 1.20.x | Java 11          | 存量项目常见选择，升级到 Java 17 前应验证 Connector、状态和依赖 |
| Java 21      | 暂不作为默认选择 | Flink 2.x 已有实验性支持，但生产使用前必须专项验证           |

本地开发环境建议统一配置 `JAVA_HOME`，避免 IDE、Maven、命令行和 Flink 集群使用不同 JDK。

Linux 或 macOS 环境可以通过以下命令检查 JDK 版本和环境变量。

```bash
# 查看当前 Java 版本
java -version

# 查看 javac 编译器版本
javac -version

# 查看 JAVA_HOME 配置
echo "$JAVA_HOME"
```

命令说明：`java -version` 用于确认运行时版本，`javac -version` 用于确认编译器版本，`JAVA_HOME` 应指向实际使用的 JDK 目录。Flink 作业如果在本地可运行但提交集群失败，应优先检查本地 JDK 与集群 JDK 是否一致。

Maven 项目中建议显式声明 Java 编译版本，避免不同开发人员本地默认 JDK 不一致。

文件位置：`pom.xml`

```xml
<properties>
    <!-- Java 编译版本，Flink 2.x 项目建议使用 Java 17 -->
    <maven.compiler.release>17</maven.compiler.release>

    <!-- 源码编码，避免中文日志、配置说明和注释乱码 -->
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

    <!-- Flink 版本应与目标运行集群保持一致 -->
    <flink.version>2.2.0</flink.version>
</properties>
```

如果项目仍运行在 Flink 1.20.x 集群，应根据集群实际 JDK 调整为 Java 11，不建议只修改本地编译版本而不验证集群运行时。

### Maven 或 Gradle 构建工具

Flink Java 项目推荐优先使用 Maven。Maven 在企业 Java 后端项目中更常见，便于统一依赖版本、插件配置、Shade 打包、CI/CD 构建和多模块管理。Flink 官方项目配置文档也提供了基于 Maven archetype 的 Java quickstart 创建方式，并同时提供 Gradle 配置方式。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/configuration/overview/?utm_source=chatgpt.com))

推荐策略如下：

| 构建工具 | 推荐场景                                                     |
| -------- | ------------------------------------------------------------ |
| Maven    | 企业 Java 项目、多模块项目、统一依赖管理、Shade 打包、CI/CD 标准化 |
| Gradle   | 已有团队 Gradle 基线、构建逻辑复杂、需要更灵活构建脚本       |
| 混用     | 不推荐，同一个 Flink 工程应固定一种构建体系                  |

新项目可以通过 Flink 官方 quickstart 创建 Maven 工程。

```bash
# 使用 Flink 官方 Maven Archetype 创建 Java 项目
mvn archetype:generate \
  -DarchetypeGroupId=org.apache.flink \
  -DarchetypeArtifactId=flink-quickstart-java \
  -DarchetypeVersion=2.2.0
```

命令说明：`flink-quickstart-java` 会生成基础 Java Flink 项目结构，`archetypeVersion` 应与目标 Flink 版本保持一致。生成后需要根据公司包名、模块结构、日志配置和部署方式进行调整。Flink 下载页显示 Apache Flink 2.2.0 是当前最新稳定版本，并提供 Maven 依赖示例。([flink.apache.org](https://flink.apache.org/downloads/?utm_source=chatgpt.com))

Maven 项目常用基础依赖如下。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Flink Java API，提供基础 Java 开发能力 -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-java</artifactId>
        <version>${flink.version}</version>
        <scope>provided</scope>
    </dependency>

    <!-- Flink DataStream API，开发实时流处理作业的核心依赖 -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-streaming-java</artifactId>
        <version>${flink.version}</version>
        <scope>provided</scope>
    </dependency>

    <!-- Flink 客户端依赖，本地运行和提交作业时常用 -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-clients</artifactId>
        <version>${flink.version}</version>
    </dependency>

    <!-- Hutool 工具类，常用于字符串、集合、JSON、日期和配置处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- Lombok，简化实体类、配置类和日志对象声明 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>1.18.36</version>
        <scope>provided</scope>
    </dependency>

    <!-- JUnit 5，用于单元测试和算子逻辑测试 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.11.4</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

这里将 Flink 核心运行依赖设置为 `provided`，适用于提交到已有 Flink 集群运行的场景，因为集群侧通常已经提供 Flink Runtime。若需要纯本地 IDE 运行，可以保留 `flink-clients` 作为普通依赖，或通过 Maven Profile 单独区分本地运行依赖和生产打包依赖。

Gradle 项目可以采用以下基础配置。

文件位置：`build.gradle`

```gradle
plugins {
    // Java 项目插件
    id 'java'

    // 应用程序插件，便于本地运行 main 方法
    id 'application'
}

group = 'io.github.atengk'
version = '1.0.0'

repositories {
    // 使用 Maven Central 拉取 Flink、Hutool、Lombok 等依赖
    mavenCentral()
}

ext {
    // Flink 版本应与目标运行集群保持一致
    flinkVersion = '2.2.0'
}

dependencies {
    // Flink Java API
    compileOnly "org.apache.flink:flink-java:${flinkVersion}"

    // Flink DataStream API
    compileOnly "org.apache.flink:flink-streaming-java:${flinkVersion}"

    // 本地运行和作业提交客户端
    implementation "org.apache.flink:flink-clients:${flinkVersion}"

    // Hutool 常用工具类
    implementation "cn.hutool:hutool-all:5.8.36"

    // Lombok 编译期依赖
    compileOnly "org.projectlombok:lombok:1.18.36"
    annotationProcessor "org.projectlombok:lombok:1.18.36"

    // JUnit 5 测试依赖
    testImplementation "org.junit.jupiter:junit-jupiter:5.11.4"
}

java {
    // Flink 2.x 项目建议使用 Java 17
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

test {
    // 启用 JUnit Platform
    useJUnitPlatform()
}
```

Maven 和 Gradle 二选一即可。若项目后续需要多模块、统一依赖版本、生产 Fat Jar、CI/CD 制品上传和部署脚本集成，Maven 会更容易形成标准化模板。

### Flink 版本选择

Flink 版本必须与运行集群版本保持一致。开发依赖版本、Connector 版本、SQL 语法、状态格式、Savepoint 兼容性和部署参数都与 Flink 版本密切相关。Apache Flink 下载页显示，Apache Flink 2.2.0 是当前最新稳定版本，Flink CDC 3.6.0 同时提供面向 Flink 1.20.x 和 Flink 2.2.x 的兼容版本。([flink.apache.org](https://flink.apache.org/downloads/?utm_source=chatgpt.com))

推荐选择策略如下：

| 项目类型                      | 推荐版本策略                                      |
| ----------------------------- | ------------------------------------------------- |
| 新建项目                      | 优先选择当前稳定 Flink 2.x 版本，例如 2.2.0       |
| 存量项目                      | 优先保持与现有集群一致，例如 1.20.x、2.0.x、2.1.x |
| 使用 CDC                      | 同时确认 Flink CDC 与 Flink 主版本兼容            |
| 使用 Hive、HBase 等 Connector | 需要额外验证 JDK、Connector 和依赖冲突            |
| 有 Savepoint 升级需求         | 先验证状态兼容性，再升级 Flink 或 Connector       |
| 多集群部署                    | 不同集群版本应通过 Profile 或分支明确隔离         |

版本选择时不要只看 Flink 核心版本，还需要同时确认以下内容：

| 检查项         | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| JDK 版本       | Flink 2.x 推荐 Java 17，存量 1.x 集群可能仍使用 Java 11      |
| Connector 版本 | Kafka、JDBC、CDC、Doris、StarRocks、Iceberg 等依赖必须匹配   |
| Scala 后缀     | 部分旧依赖或分发包仍可能带 `_2.12` 后缀，需要避免混用        |
| 状态兼容       | 使用 RocksDB、TTL、复杂状态类型时，升级前必须测试 Savepoint 恢复 |
| SQL 兼容       | Table API 和 SQL 的语法、函数、类型推断可能随版本变化        |
| 部署模式       | Application Mode、Session Mode、Kubernetes Operator 配置可能随版本变化 |

生产项目建议将版本集中声明到根 `pom.xml` 或 `dependencyManagement` 中，避免各模块自行声明 Flink 版本。

文件位置：`pom.xml`

```xml
<properties>
    <!-- Flink 主版本，必须与目标集群版本一致 -->
    <flink.version>2.2.0</flink.version>

    <!-- Kafka Connector 版本示例，具体版本需要按 Flink 官方兼容矩阵确认 -->
    <flink.kafka.connector.version>4.0.1-2.0</flink.kafka.connector.version>

    <!-- Hutool 工具类版本，统一由根工程管理 -->
    <hutool.version>5.8.36</hutool.version>
</properties>
```

版本升级前应至少完成三类验证：本地编译验证、测试环境端到端运行验证、基于 Savepoint 的恢复验证。涉及生产状态的作业不能只通过单元测试后直接升级。

### IDE 配置

IDE 推荐使用 IntelliJ IDEA。Flink Java 项目对依赖、编译版本、注解处理器、资源目录和运行参数较敏感，IDE 配置不统一容易出现“命令行能运行、IDE 不能运行”或“IDE 能运行、打包后失败”的问题。

建议配置如下：

| 配置项                | 推荐值                                       |
| --------------------- | -------------------------------------------- |
| Project SDK           | Java 17                                      |
| Language Level        | 17                                           |
| Maven Runner JRE      | 与 Project SDK 一致                          |
| 编码                  | UTF-8                                        |
| Annotation Processing | 开启，支持 Lombok                            |
| Maven Import          | 自动导入依赖                                 |
| Compiler              | 使用 Maven 或 Gradle 配置中的版本            |
| 资源目录              | `src/main/resources`、各环境配置目录明确标记 |

IDE 中运行 Flink 作业时，应重点配置以下参数：

| 参数              | 说明                                                         |
| ----------------- | ------------------------------------------------------------ |
| Main class        | 作业入口类，例如 `io.github.atengk.flink.job.UserBehaviorJob` |
| Program arguments | 业务参数，例如 `--env local --jobName user-behavior-job`     |
| VM options        | JVM 参数，例如日志配置、内存参数、JDK 模块开放参数           |
| Working directory | 项目根目录，避免相对路径读取配置失败                         |
| Classpath         | 使用当前模块 classpath，避免选择错误模块                     |

本地运行配置示例：

```text
Main class:
io.github.atengk.flink.job.UserBehaviorJob

Program arguments:
--env local --jobName user-behavior-job --parallelism 2

VM options:
-Dfile.encoding=UTF-8
-Dlog4j.configurationFile=src/main/resources/log4j2-local.properties

Working directory:
$PROJECT_DIR$
```

如果使用 Java 17 并且涉及 Kryo、反射或 JDK 内部类访问，可能需要按 Flink 官方 Java 兼容性说明补充 `--add-opens` 或 `--add-exports` 参数。Flink 文档说明，从 Java 16 开始，JDK 模块化要求应用显式开放部分模块访问，Flink 默认配置已经包含运行自身所需的参数，用户不应删减，只应在必要时扩展。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/deployment/java_compatibility/?utm_source=chatgpt.com))

### 本地运行环境

本地运行环境用于开发阶段快速验证作业逻辑、参数解析、配置加载、Source/Sink 连通性和日志输出。Flink 支持在单机甚至单个 JVM 中运行程序，官方文档说明本地环境适合测试和调试 Flink 程序，很多示例可以直接在 IDE 中点击运行。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.16/docs/dev/dataset/local_execution/?utm_source=chatgpt.com))

本地运行通常分为三种方式：

| 方式                   | 适用场景                                        |
| ---------------------- | ----------------------------------------------- |
| IDE 直接运行 main 方法 | 验证算子逻辑、参数解析、少量测试数据            |
| 本地 MiniCluster       | 验证更接近集群的调度、并行度、Checkpoint 行为   |
| 本地 Standalone Flink  | 验证 Web UI、提交命令、依赖打包、日志和运行参数 |

基础环境建议如下：

| 组件  | 本地建议                                      |
| ----- | --------------------------------------------- |
| JDK   | Java 17                                       |
| Maven | 3.8+ 或团队统一版本                           |
| Flink | 与目标集群一致                                |
| Kafka | Docker Compose 或测试环境 Kafka               |
| MySQL | 本地 Docker 或测试库                          |
| Redis | 本地 Docker 或测试 Redis                      |
| 日志  | `log4j2-local.properties`                     |
| 配置  | `application-local.yml` 或 `local.properties` |

本地 Standalone Flink 可以通过以下方式启动。

```bash
# 解压 Flink 安装包
tar -zxf flink-2.2.0-bin-scala_2.12.tgz

# 进入 Flink 目录
cd flink-2.2.0

# 启动本地 Standalone 集群
./bin/start-cluster.sh

# 查看本地集群进程
jps

# 访问 Flink Web UI
# 默认地址通常为 http://localhost:8081

# 停止本地 Standalone 集群
./bin/stop-cluster.sh
```

命令说明：`start-cluster.sh` 会启动本地 JobManager 和 TaskManager，`jps` 用于查看 Java 进程是否存在，`stop-cluster.sh` 用于停止本地集群。本地 Web UI 可用于查看作业状态、Subtask、日志、异常、Checkpoint 和反压情况。

本地提交作业示例：

```bash
# 打包项目
mvn clean package -DskipTests

# 提交 Flink 作业到本地 Standalone 集群
./bin/flink run \
  -c io.github.atengk.flink.job.UserBehaviorJob \
  /data/flink-jobs/flink-job-user-behavior.jar \
  --env local \
  --jobName user-behavior-job \
  --parallelism 2
```

命令说明：`-c` 指定作业入口类，Jar 路径应指向 Maven 打包后的作业包，`--env`、`--jobName`、`--parallelism` 是用户自定义业务参数。生产环境中应将这些参数标准化，并在部署脚本或 Kubernetes/YARN 配置中统一维护。

### 日志配置

日志配置用于记录作业启动参数、关键业务分支、异常数据、外部系统调用、Checkpoint 相关异常和 Sink 写入失败等信息。Flink 官方日志文档说明，Flink 进程会创建日志文件，日志可通过 JobManager 或 TaskManager 的 Web UI 页面查看，Flink 使用 SLF4J 日志接口，并支持 Log4j 2 配置。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/deployment/advanced/logging/?utm_source=chatgpt.com))

Flink Java 项目建议采用以下日志策略：

| 类型       | 建议                                                 |
| ---------- | ---------------------------------------------------- |
| 日志接口   | 使用 SLF4J                                           |
| 日志实现   | 使用 Flink 默认 Log4j 2                              |
| 本地日志   | 输出到控制台和本地文件                               |
| 生产日志   | 输出到 TaskManager 日志文件，由平台采集              |
| 日志级别   | 默认 INFO，问题排查时局部 DEBUG                      |
| 业务日志   | 记录启动参数、关键统计、异常分支和外部写入失败       |
| 脏数据日志 | 不建议大量打印正文，应落侧输出或文件，日志只记录摘要 |

本地日志配置文件示例。

文件位置：`src/main/resources/log4j2-local.properties`

```properties
# Log4j2 内部配置刷新间隔，单位秒
monitorInterval=30

# 根日志级别，本地开发建议 INFO
rootLogger.level=INFO
rootLogger.appenderRefs=console, file
rootLogger.appenderRef.console.ref=ConsoleAppender
rootLogger.appenderRef.file.ref=FileAppender

# 控制台输出，便于 IDE 调试
appender.console.name=ConsoleAppender
appender.console.type=CONSOLE
appender.console.layout.type=PatternLayout
appender.console.layout.pattern=%d{yyyy-MM-dd HH:mm:ss.SSS} %-5p [%t] %c{36} - %m%n

# 本地文件输出，便于排查长时间运行问题
appender.file.name=FileAppender
appender.file.type=RollingFile
appender.file.fileName=logs/flink-local.log
appender.file.filePattern=logs/flink-local-%d{yyyy-MM-dd}-%i.log.gz
appender.file.layout.type=PatternLayout
appender.file.layout.pattern=%d{yyyy-MM-dd HH:mm:ss.SSS} %-5p [%t] %c{36} - %m%n
appender.file.policies.type=Policies
appender.file.policies.size.type=SizeBasedTriggeringPolicy
appender.file.policies.size.size=100MB
appender.file.strategy.type=DefaultRolloverStrategy
appender.file.strategy.max=10

# Flink 框架日志保持 INFO，避免本地日志过多
logger.flink.name=org.apache.flink
logger.flink.level=INFO

# 项目业务包日志，可以按需调整为 DEBUG
logger.biz.name=io.github.atengk.flink
logger.biz.level=INFO
```

本地运行时指定日志配置：

```bash
# IDE 或命令行运行时指定本地 Log4j2 配置文件
java \
  -Dfile.encoding=UTF-8 \
  -Dlog4j.configurationFile=src/main/resources/log4j2-local.properties \
  -jar target/flink-job-user-behavior.jar \
  --env local \
  --jobName user-behavior-job
```

命令说明：`-Dlog4j.configurationFile` 指定 Log4j2 配置文件，`-Dfile.encoding=UTF-8` 避免中文日志乱码。生产环境中通常使用 Flink 集群自身的日志配置，不建议把本地日志配置强行覆盖到集群运行环境。

日志使用原则如下：

1. 作业启动时打印作业名称、环境、并行度、配置文件路径和关键参数。
2. 连接 Kafka、MySQL、Redis、Doris 等外部系统时打印连接目标摘要，但不要打印密码。
3. 解析异常、脏数据、迟到数据不应逐条大量打印完整内容。
4. Sink 写入失败必须记录目标表、批次大小、异常摘要和恢复建议。
5. 高频算子中避免逐条打印 INFO 日志，必要时使用采样或计数器。
6. 生产环境禁止长期开启全局 DEBUG 日志。

## 项目工程结构

本章节用于定义 Flink Java 项目的目录组织方式。工程结构应服务于多人协作、作业复用、统一配置、标准打包、自动化部署和测试验证。对于一个长期维护的实时计算项目，清晰的模块边界比单个作业的代码简洁更重要。

### Maven 多模块结构

多模块结构适合中大型 Flink 项目，特别是存在多个实时作业、多个数据源、公共工具、统一模型、测试模块和部署脚本的场景。多模块可以隔离作业依赖，避免所有作业共享一个巨大 Fat Jar，同时也便于按业务域独立发布。

推荐结构如下：

```text
flink-realtime-platform/
├── pom.xml
├── README.md
├── flink-common/
│   ├── pom.xml
│   └── src/main/java/io/github/atengk/flink/common/
├── flink-model/
│   ├── pom.xml
│   └── src/main/java/io/github/atengk/flink/model/
├── flink-connector/
│   ├── pom.xml
│   └── src/main/java/io/github/atengk/flink/connector/
├── flink-job-user-behavior/
│   ├── pom.xml
│   └── src/main/java/io/github/atengk/flink/job/userbehavior/
├── flink-job-order/
│   ├── pom.xml
│   └── src/main/java/io/github/atengk/flink/job/order/
├── flink-test/
│   ├── pom.xml
│   └── src/test/java/io/github/atengk/flink/test/
├── config/
│   ├── local/
│   ├── test/
│   └── prod/
├── scripts/
│   ├── local/
│   ├── yarn/
│   └── k8s/
└── docs/
    ├── deploy.md
    ├── operation.md
    └── troubleshooting.md
```

各模块职责建议如下：

| 模块              | 职责                                                     |
| ----------------- | -------------------------------------------------------- |
| `flink-common`    | 公共工具类、参数解析、配置加载、异常定义、常量、日志辅助 |
| `flink-model`     | Java Bean、DTO、事件模型、维表模型、输出模型             |
| `flink-connector` | Kafka、JDBC、Redis、Doris、CDC 等 Source/Sink 封装       |
| `flink-job-*`     | 具体 Flink 作业，每个模块对应一个或一类作业              |
| `flink-test`      | 公共测试工具、测试数据构造、MiniCluster、Testcontainers  |
| `config`          | 多环境配置文件                                           |
| `scripts`         | 本地、YARN、Kubernetes 部署和运维脚本                    |
| `docs`            | 架构、部署、运维、故障处理和发布记录文档                 |

根 `pom.xml` 负责统一模块、版本和插件。

文件位置：`pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- 企业或个人统一包名 -->
    <groupId>io.github.atengk</groupId>
    <artifactId>flink-realtime-platform</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <modules>
        <!-- 公共工具模块 -->
        <module>flink-common</module>

        <!-- 数据模型模块 -->
        <module>flink-model</module>

        <!-- Connector 封装模块 -->
        <module>flink-connector</module>

        <!-- 用户行为实时作业模块 -->
        <module>flink-job-user-behavior</module>

        <!-- 订单实时作业模块 -->
        <module>flink-job-order</module>

        <!-- 测试支撑模块 -->
        <module>flink-test</module>
    </modules>

    <properties>
        <!-- Java 编译版本 -->
        <maven.compiler.release>17</maven.compiler.release>

        <!-- 项目编码 -->
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <!-- Flink 版本统一管理 -->
        <flink.version>2.2.0</flink.version>

        <!-- Hutool 工具类版本统一管理 -->
        <hutool.version>5.8.36</hutool.version>

        <!-- Lombok 版本统一管理 -->
        <lombok.version>1.18.36</lombok.version>
    </properties>
</project>
```

多模块结构的核心原则是：公共能力下沉，业务作业上浮。公共模块不能反向依赖具体作业模块，作业模块可以依赖公共模块、模型模块和 Connector 模块。

### 单模块项目结构

单模块结构适合小型项目、单个作业、验证型项目或临时补数任务。它的优点是简单直接，缺点是随着作业数量增加，依赖、配置和代码边界容易混乱。

推荐结构如下：

```text
flink-job-user-behavior/
├── pom.xml
├── README.md
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── io/github/atengk/flink/
│   │   │       ├── job/
│   │   │       ├── config/
│   │   │       ├── model/
│   │   │       ├── function/
│   │   │       ├── sink/
│   │   │       ├── source/
│   │   │       └── util/
│   │   └── resources/
│   │       ├── application-local.yml
│   │       ├── application-test.yml
│   │       ├── application-prod.yml
│   │       └── log4j2-local.properties
│   └── test/
│       ├── java/
│       └── resources/
├── scripts/
│   ├── run-local.sh
│   ├── submit-yarn.sh
│   └── submit-k8s.sh
└── docs/
    ├── deploy.md
    └── operation.md
```

单模块项目中，包结构仍应清晰划分：

| 包名        | 职责                                                         |
| ----------- | ------------------------------------------------------------ |
| `job`       | 作业入口类                                                   |
| `config`    | 配置加载、参数对象、环境枚举                                 |
| `model`     | 输入、输出、维表、聚合结果模型                               |
| `function`  | MapFunction、FlatMapFunction、ProcessFunction、WindowFunction |
| `source`    | Source 构建封装                                              |
| `sink`      | Sink 构建封装                                                |
| `util`      | 通用工具类                                                   |
| `constant`  | 常量定义                                                     |
| `exception` | 自定义异常                                                   |

单模块不是随意堆代码。即使只有一个作业，也应将入口、配置、模型、函数、Source、Sink 分开，避免所有逻辑写在一个 `main()` 方法中。

### 配置文件目录

配置文件目录用于管理本地、测试、生产等不同环境的运行参数。Flink 作业通常会涉及 Kafka、数据库、Redis、Checkpoint、并行度、状态后端、Sink 批量写入等配置，必须避免硬编码在 Java 代码中。

推荐目录结构如下：

```text
config/
├── local/
│   ├── application.yml
│   └── log4j2.properties
├── test/
│   ├── application.yml
│   └── log4j2.properties
└── prod/
    ├── application.yml
    └── log4j2.properties
```

配置文件示例。

文件位置：`config/local/application.yml`

```yaml
# 作业基础配置
job:
  name: user-behavior-job
  env: local
  parallelism: 2

# Flink Checkpoint 配置
checkpoint:
  enabled: true
  interval-ms: 60000
  timeout-ms: 300000
  min-pause-ms: 30000
  storage: file:///tmp/flink-checkpoints/user-behavior-job

# Kafka Source 配置
kafka:
  bootstrap-servers: localhost:9092
  group-id: user-behavior-job-local
  topic: user_behavior_log
  auto-offset-reset: earliest

# Sink 配置
sink:
  type: kafka
  topic: user_behavior_clean
  batch-size: 500

# 脏数据处理配置
dirty:
  enabled: true
  output-path: file:///tmp/flink-dirty/user-behavior-job
```

配置目录设计建议：

1. 本地配置允许使用本地 Kafka、MySQL、Redis 和文件路径。
2. 测试配置应接入测试环境中间件，禁止连接生产资源。
3. 生产配置不应提交明文密码，应通过 Secret、环境变量或配置中心注入。
4. 配置项命名应保持稳定，避免每个作业自定义一套风格。
5. 作业启动日志中应打印配置摘要，但敏感字段必须脱敏。

### 公共工具模块

公共工具模块用于沉淀多个作业复用的基础能力，例如参数解析、配置加载、JSON 工具、时间工具、Kafka Source 构建、Sink 参数校验、脏数据封装和日志辅助。公共模块的目标是减少重复代码，但不能承载具体业务逻辑。

推荐包结构如下：

```text
flink-common/
└── src/main/java/io/github/atengk/flink/common/
    ├── config/
    ├── constant/
    ├── exception/
    ├── param/
    ├── util/
    └── validation/
```

公共模块建议包含以下能力：

| 能力      | 说明                                                         |
| --------- | ------------------------------------------------------------ |
| 参数解析  | 解析 `--env`、`--jobName`、`--parallelism`、`--config` 等启动参数 |
| 配置加载  | 根据环境加载 YAML、Properties 或配置中心参数                 |
| JSON 处理 | 使用 Hutool 或 Jackson 统一处理 JSON 解析和异常              |
| 时间处理  | 统一时间格式、时间戳、时区和窗口参数                         |
| 异常定义  | 定义业务异常、配置异常、数据解析异常                         |
| 数据校验  | 校验必填字段、枚举值、时间范围和参数合法性                   |
| 日志辅助  | 敏感字段脱敏、启动参数摘要、异常摘要格式化                   |

公共模块应遵循以下约束：

1. 不依赖具体作业模块。
2. 不包含具体业务规则。
3. 不直接写入具体业务 Sink。
4. 不引入过重依赖。
5. 所有公共方法必须有明确边界，避免形成“万能工具类”。

### 作业模块

作业模块是 Flink 项目的核心交付单元。一个作业模块通常包含一个主作业入口，以及该作业专属的 Source、Function、Sink、模型和测试。多作业项目中建议按业务域或作业粒度拆分模块，避免多个完全不同的作业堆在同一个模块中。

推荐结构如下：

```text
flink-job-user-behavior/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/io/github/atengk/flink/job/userbehavior/
    │   │   ├── UserBehaviorJob.java
    │   │   ├── config/
    │   │   ├── model/
    │   │   ├── function/
    │   │   ├── source/
    │   │   └── sink/
    │   └── resources/
    │       ├── application-local.yml
    │       ├── application-test.yml
    │       ├── application-prod.yml
    │       └── log4j2-local.properties
    └── test/
        └── java/io/github/atengk/flink/job/userbehavior/
```

作业模块中各目录职责如下：

| 目录                   | 职责                                               |
| ---------------------- | -------------------------------------------------- |
| `UserBehaviorJob.java` | 作业入口，负责环境初始化、参数解析、流程编排和提交 |
| `config`               | 作业专属配置对象                                   |
| `model`                | 作业输入、输出、中间结果模型                       |
| `function`             | 作业专属算子函数                                   |
| `source`               | 作业数据源构建逻辑                                 |
| `sink`                 | 作业结果输出逻辑                                   |
| `resources`            | 作业默认配置和本地日志配置                         |
| `test`                 | 单元测试、算子测试、集成测试                       |

作业入口类不应写大量业务处理逻辑。推荐只做以下事情：

1. 解析启动参数。
2. 加载环境配置。
3. 初始化 `StreamExecutionEnvironment`。
4. 配置 Checkpoint、并行度、Restart Strategy。
5. 构建 Source。
6. 编排核心处理流程。
7. 构建 Sink。
8. 调用 `env.execute(jobName)` 提交作业。

具体的解析、转换、聚合、维表关联、状态处理和输出逻辑应拆到独立 Function 或 Service 类中，便于测试和复用。

### 测试模块

测试模块用于验证 Flink 作业的核心逻辑、配置加载、算子行为、数据解析、端到端链路和异常场景。实时作业不能只依赖人工观察日志验证，至少应覆盖关键算子和核心数据路径。

测试目录建议如下：

```text
flink-test/
└── src/test/java/io/github/atengk/flink/test/
    ├── fixture/
    ├── function/
    ├── integration/
    ├── mini/
    └── util/
```

常见测试类型如下：

| 测试类型         | 说明                                                         |
| ---------------- | ------------------------------------------------------------ |
| 单元测试         | 测试工具类、参数解析、配置加载、JSON 解析                    |
| 算子测试         | 测试 MapFunction、FlatMapFunction、ProcessFunction 等业务逻辑 |
| 状态测试         | 测试 ValueState、MapState、Timer、去重和规则匹配             |
| 集成测试         | 联合 Kafka、MySQL、Redis、Sink 验证完整链路                  |
| MiniCluster 测试 | 使用本地 Flink 集群验证更接近真实运行环境的行为              |
| 数据回放测试     | 使用历史样本数据验证计算结果一致性                           |
| 性能测试         | 验证吞吐、延迟、反压和资源占用                               |

测试数据建议统一放到 `src/test/resources`。

```text
src/test/resources/
├── data/
│   ├── user_behavior_normal.json
│   ├── user_behavior_dirty.json
│   └── user_behavior_late.json
├── config/
│   └── application-test.yml
└── expected/
    └── user_behavior_result.json
```

测试原则如下：

1. 正常数据、脏数据、空数据、迟到数据都要覆盖。
2. 涉及状态的算子必须验证状态更新和清理逻辑。
3. 涉及时间语义的逻辑必须验证 Watermark 和窗口触发结果。
4. Sink 写入逻辑至少要验证幂等键、批量大小和异常处理。
5. 测试环境配置必须与生产配置隔离，禁止测试误写生产系统。

### 部署脚本目录

部署脚本目录用于管理本地运行、YARN 提交、Kubernetes 提交、Savepoint 停止、恢复启动和状态检查等操作。脚本目录应与工程代码一起维护，避免部署命令散落在个人文档或聊天记录中。

推荐目录结构如下：

```text
scripts/
├── local/
│   ├── run-user-behavior-local.sh
│   └── run-order-local.sh
├── yarn/
│   ├── submit-user-behavior-yarn.sh
│   ├── stop-with-savepoint.sh
│   └── restore-from-savepoint.sh
├── k8s/
│   ├── build-image.sh
│   ├── deploy-user-behavior.yaml
│   └── delete-user-behavior.sh
└── common/
    ├── env.sh
    └── check.sh
```

本地运行脚本示例。

文件位置：`scripts/local/run-user-behavior-local.sh`

```bash
#!/usr/bin/env bash

# 遇到错误立即退出，避免继续执行错误命令
set -e

# 作业名称
JOB_NAME="user-behavior-job"

# 作业入口类
MAIN_CLASS="io.github.atengk.flink.job.userbehavior.UserBehaviorJob"

# 作业 Jar 路径
JOB_JAR="target/flink-job-user-behavior.jar"

# 本地配置文件路径
CONFIG_FILE="config/local/application.yml"

# 打包作业
mvn clean package -DskipTests

# 本地运行作业
java \
  -Dfile.encoding=UTF-8 \
  -Dlog4j.configurationFile=src/main/resources/log4j2-local.properties \
  -cp "${JOB_JAR}" \
  "${MAIN_CLASS}" \
  --env local \
  --jobName "${JOB_NAME}" \
  --config "${CONFIG_FILE}" \
  --parallelism 2
```

命令说明：`set -e` 用于在命令失败时停止脚本，`MAIN_CLASS` 指定 Flink 作业入口类，`CONFIG_FILE` 指定本地配置，`--parallelism` 用于覆盖默认并行度。若作业使用 Flink 集群运行方式，本地脚本应改为 `flink run` 提交。

YARN 提交脚本示例。

文件位置：`scripts/yarn/submit-user-behavior-yarn.sh`

```bash
#!/usr/bin/env bash

# 遇到错误立即退出
set -e

# Flink 安装目录
FLINK_HOME="/opt/flink"

# 作业名称
JOB_NAME="user-behavior-job"

# 作业入口类
MAIN_CLASS="io.github.atengk.flink.job.userbehavior.UserBehaviorJob"

# 作业 Jar 路径
JOB_JAR="/data/flink/jobs/flink-job-user-behavior.jar"

# 生产配置文件
CONFIG_FILE="/data/flink/config/prod/application.yml"

# 提交到 YARN 集群
"${FLINK_HOME}/bin/flink" run \
  -t yarn-application \
  -Djobmanager.memory.process.size=1024m \
  -Dtaskmanager.memory.process.size=4096m \
  -Dtaskmanager.numberOfTaskSlots=4 \
  -c "${MAIN_CLASS}" \
  "${JOB_JAR}" \
  --env prod \
  --jobName "${JOB_NAME}" \
  --config "${CONFIG_FILE}" \
  --parallelism 4
```

命令说明：`-t yarn-application` 表示使用 YARN Application 模式提交，`-D` 参数用于覆盖 Flink 运行参数，`-c` 指定入口类，Jar 后面的参数会传递给用户程序。实际生产环境还应增加队列、日志、用户、依赖路径、Savepoint 恢复参数等配置。

部署脚本管理原则如下：

1. 每个生产作业必须有明确的提交脚本或部署 YAML。
2. 启动、停止、Savepoint、恢复命令应独立维护。
3. 脚本中的环境变量、路径、队列、资源参数应集中管理。
4. 敏感信息不得写入脚本。
5. 所有脚本应支持 `test` 和 `prod` 环境隔离。
6. 发布前必须在测试环境使用同一套脚本验证。

以下继续展开 `Maven 依赖管理` 与 `Flink 程序入口设计` 两部分，延续你上传的大纲结构。

## Maven 依赖管理

本章节用于统一 Flink Java 项目的 Maven 依赖组织方式，包括 Flink 核心依赖、Streaming 依赖、Table API 依赖、Connector 依赖、日志、序列化、测试依赖和冲突处理。Flink 官方项目配置文档明确区分了 DataStream、Table API、Table API 与 DataStream 桥接依赖，并建议用户作业 Jar 打包应用代码、Connector、Format 和第三方依赖，但不要把 Flink Runtime 已提供的 Java API 和运行时模块打入作业 Uber Jar。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/configuration/overview/?utm_source=chatgpt.com))

### Flink 核心依赖

Flink 核心依赖主要用于提供 Java API、客户端提交能力、公共类型、配置对象和运行环境入口。新建项目应在根 `pom.xml` 中统一管理 Flink 版本，避免不同模块声明不同版本导致运行时冲突。Apache Flink 下载页当前提供的 Maven 示例版本为 `2.2.0`，但实际项目必须以目标运行集群版本为准。([flink.apache.org](https://flink.apache.org/downloads/?utm_source=chatgpt.com))

文件位置：`pom.xml`

```xml
<properties>
    <!-- Java 编译版本，Flink 2.x 推荐 Java 17 -->
    <maven.compiler.release>17</maven.compiler.release>

    <!-- 项目编码，避免中文日志、配置文件、注释乱码 -->
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

    <!-- Flink 版本必须与目标运行集群保持一致 -->
    <flink.version>2.2.0</flink.version>

    <!-- Hutool 工具类版本 -->
    <hutool.version>5.8.36</hutool.version>

    <!-- Lombok 版本 -->
    <lombok.version>1.18.36</lombok.version>

    <!-- JUnit 5 版本 -->
    <junit.version>5.11.4</junit.version>
</properties>
```

Flink 2.x 项目建议使用 Java 17，Flink 官方 Java 兼容性文档说明，Flink 2.0.0 起默认使用 Java 17，并将 Java 17 作为推荐运行版本；Java 21 仍属于实验性支持，生产项目不应直接默认采用。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-2.2/docs/deployment/java_compatibility/?utm_source=chatgpt.com))

核心依赖示例：

```xml
<dependencies>
    <!-- Flink Java API，提供基础 Java 开发能力 -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-java</artifactId>
        <version>${flink.version}</version>
        <scope>provided</scope>
    </dependency>

    <!-- Flink 客户端依赖，本地运行 main 方法或提交作业时使用 -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-clients</artifactId>
        <version>${flink.version}</version>
    </dependency>

    <!-- Hutool 工具类，常用于参数、字符串、集合、JSON、日期处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok，简化日志对象、Getter、Setter、Builder 等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

`flink-java` 通常使用 `provided`，因为生产 Flink 集群已经包含 Flink Runtime 和基础 API。`flink-clients` 可以用于本地开发和提交作业，但生产 Fat Jar 是否包含它，需要根据部署方式决定。若提交到已有 Flink 集群，通常不需要将 Flink Runtime 相关依赖打进业务 Jar。

### Flink Streaming 依赖

Flink Streaming 依赖是开发实时流处理作业的基础，主要提供 `StreamExecutionEnvironment`、`DataStream`、窗口、状态、时间语义、Checkpoint 和常见流式转换能力。Flink 官方项目配置文档说明，使用 DataStream API 时需要添加 `flink-streaming-java` 依赖。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/configuration/overview/?utm_source=chatgpt.com))

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Flink DataStream API，实时流处理作业核心依赖 -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-streaming-java</artifactId>
        <version>${flink.version}</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

Streaming 依赖适用于以下开发场景：

| 场景           | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| Kafka 实时消费 | 持续消费 Kafka Topic 并处理无界流                            |
| 实时清洗       | 对日志、埋点、交易事件进行解析、过滤和字段补全               |
| 状态计算       | 使用 ValueState、MapState、ListState 实现去重、规则匹配、累计统计 |
| 窗口聚合       | 使用滚动窗口、滑动窗口、会话窗口计算实时指标                 |
| 迟到数据处理   | 配合 Event Time、Watermark、侧输出流处理乱序和迟到数据       |
| 实时 Sink      | 输出到 Kafka、Redis、JDBC、Doris、Elasticsearch 等系统       |

生产项目中，绝大多数实时作业都应以 `flink-streaming-java` 为基础依赖。即使处理有界数据，也可以通过 DataStream API 的批执行模式完成批处理逻辑。Flink 的 RuntimeExecutionMode 支持 `STREAMING`、`BATCH` 和 `AUTOMATIC`，其中 `BATCH` 用于有界输入的批式执行语义。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/api/java/org/apache/flink/api/common/RuntimeExecutionMode.html?utm_source=chatgpt.com))

### Flink Table API 依赖

Table API 和 SQL 适合声明式数据处理、动态表、指标计算、维表 Join、SQL 化 ETL 和批流统一查询。Flink 官方文档说明，Table API 和 SQL 是用于统一流批处理的关系型 API，Table API 与 SQL 可以无缝集成，也可以和 DataStream API 互相转换。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/table/overview/?utm_source=chatgpt.com))

使用 Table API 时，推荐依赖如下：

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Flink Table API Java 依赖，用于 Table API 和 SQL 开发 -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-table-api-java</artifactId>
        <version>${flink.version}</version>
        <scope>provided</scope>
    </dependency>

    <!-- Table API 与 DataStream API 桥接依赖，用于 StreamTableEnvironment -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-table-api-java-bridge</artifactId>
        <version>${flink.version}</version>
        <scope>provided</scope>
    </dependency>

    <!-- Table Runtime，本地直接运行 Table/SQL 作业时通常需要 -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-table-runtime</artifactId>
        <version>${flink.version}</version>
    </dependency>

    <!-- Table Planner Loader，本地 IDE 运行 SQL 作业时通常需要 -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-table-planner-loader</artifactId>
        <version>${flink.version}</version>
    </dependency>
</dependencies>
```

依赖使用建议如下：

| 依赖                          | 作用                                                |
| ----------------------------- | --------------------------------------------------- |
| `flink-table-api-java`        | 使用 Table API 和 SQL 的基础依赖                    |
| `flink-table-api-java-bridge` | 用于 DataStream 与 Table 之间转换                   |
| `flink-table-runtime`         | 本地运行 Table 程序时需要的运行时能力               |
| `flink-table-planner-loader`  | 本地运行 SQL 或 Table 查询时需要的 Planner 加载能力 |

如果作业提交到已有 Flink 集群运行，`flink-table-runtime` 和 `flink-table-planner-loader` 是否打包需要结合集群实际依赖、部署模式和官方打包建议判断。Flink 官方项目配置文档说明，直接执行 main class 时需要 `flink-clients`，Table API 程序还需要 `flink-table-runtime` 和 `flink-table-planner-loader`；提交到已有集群的作业 Jar 则应避免把 Flink 自身运行时模块重复打入。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/configuration/overview/?utm_source=chatgpt.com))

### Connector 依赖

Connector 依赖用于接入外部系统，例如 Kafka、JDBC、文件系统、Elasticsearch、Hive、HBase、Doris、StarRocks、Iceberg 和 Hudi。Flink 官方 Connector 文档说明，DataStream Connector 用于和第三方系统交互，官方项目 Connector 包括 Kafka、Cassandra 等系统。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/connectors/datastream/overview/?utm_source=chatgpt.com))

Connector 依赖必须按 Flink 版本、Connector 版本和目标系统版本共同确认，不能只按 Artifact 名称盲目引入。尤其是 Kafka、CDC、Doris、StarRocks、Iceberg 这类独立演进较快的 Connector，应优先查官方兼容矩阵。

通用依赖声明示例：

```xml
<properties>
    <!-- Kafka Connector 版本需要按 Flink 官方 Connector 文档和实际集群版本确认 -->
    <flink.kafka.connector.version>待确认</flink.kafka.connector.version>

    <!-- JDBC Connector 版本需要按 Flink 官方 Connector 文档确认 -->
    <flink.jdbc.connector.version>待确认</flink.jdbc.connector.version>
</properties>

<dependencies>
    <!-- Kafka Connector，用于 Kafka Source 和 Kafka Sink -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-connector-kafka</artifactId>
        <version>${flink.kafka.connector.version}</version>
    </dependency>

    <!-- JDBC Connector，用于 MySQL、PostgreSQL 等数据库 Sink 或 Lookup 场景 -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-connector-jdbc</artifactId>
        <version>${flink.jdbc.connector.version}</version>
    </dependency>
</dependencies>
```

需要特别注意：当前 Flink 2.2 Kafka DataStream Connector 文档页面明确提示 Flink 2.2 暂无对应 Kafka Connector。因此，如果项目强依赖 Kafka Connector，不应只因为 Flink Core 已有 2.2.0 就直接升级生产作业，而应先确认 Connector 是否已有可用版本，或继续使用 Connector 已明确支持的 Flink 版本线。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-2.2/docs/connectors/datastream/kafka/?utm_source=chatgpt.com))

Connector 管理原则如下：

| 原则            | 说明                                                     |
| --------------- | -------------------------------------------------------- |
| 版本显式声明    | Connector 不应使用隐式传递版本                           |
| 不混用大版本    | 避免 Flink 1.x Connector 与 Flink 2.x Runtime 混用       |
| 优先官方文档    | Kafka、JDBC、CDC、Hive、Iceberg 等依赖以官方兼容说明为准 |
| Driver 单独管理 | MySQL、PostgreSQL、Oracle 等 JDBC Driver 应单独声明      |
| Sink 能力验证   | 事务、幂等、批量写入、失败重试必须专项验证               |
| 打包前检查      | Connector 和第三方依赖通常需要进入作业 Fat Jar           |

### 日志依赖

Flink 使用 SLF4J 作为日志接口，默认使用 Log4j 2 作为底层日志框架。Flink 官方日志文档说明，所有 Flink 进程都会创建日志文件，日志可通过 JobManager 或 TaskManager 的 Web UI 查看，Flink 使用 SLF4J 接口，默认底层实现是 Log4j 2。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/deployment/advanced/logging/?utm_source=chatgpt.com))

Flink 作业代码建议只直接依赖 SLF4J API，不主动引入多个日志实现。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- SLF4J 日志接口，业务代码通过 LoggerFactory 或 Lombok @Slf4j 打日志 -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <scope>provided</scope>
    </dependency>

    <!-- Lombok 提供 @Slf4j 注解，简化日志对象声明 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

日志依赖注意事项：

1. 不要同时打入多个 SLF4J Binding，例如 Logback、Log4j2、slf4j-simple。
2. 提交到 Flink 集群运行时，优先使用集群侧 Log4j2 配置。
3. 本地 IDE 运行可以通过 `-Dlog4j.configurationFile=...` 指定本地日志配置。
4. 高频算子中不要逐条打印 INFO 日志。
5. 异常日志要包含作业名、算子名、数据摘要和异常摘要，避免只打印“处理失败”。

### 序列化依赖

序列化依赖用于处理输入、输出和中间状态的数据格式。Flink Java 项目中常见格式包括 JSON、Avro、Protobuf、CSV 和自定义二进制格式。选择序列化方式时，应综合考虑数据规范、Schema 演进、性能、跨语言兼容和下游系统支持。

常用依赖示例：

```xml
<dependencies>
    <!-- Jackson Databind，用于 JSON 与 Java Bean 之间转换 -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.18.2</version>
    </dependency>

    <!-- Jackson JSR310 模块，用于 LocalDateTime、LocalDate、Instant 等 Java 8 时间类型 -->
    <dependency>
        <groupId>com.fasterxml.jackson.datatype</groupId>
        <artifactId>jackson-datatype-jsr310</artifactId>
        <version>2.18.2</version>
    </dependency>

    <!-- Avro 序列化，适合强 Schema 和 Schema 演进场景 -->
    <dependency>
        <groupId>org.apache.avro</groupId>
        <artifactId>avro</artifactId>
        <version>1.12.0</version>
    </dependency>

    <!-- Protobuf Java 运行时，适合高性能、跨语言、强 Schema 场景 -->
    <dependency>
        <groupId>com.google.protobuf</groupId>
        <artifactId>protobuf-java</artifactId>
        <version>4.29.3</version>
    </dependency>

    <!-- Hutool，轻量处理 JSON、日期、字符串、集合和类型转换 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>
</dependencies>
```

序列化依赖选择建议如下：

| 格式     | 适用场景                                          |
| -------- | ------------------------------------------------- |
| JSON     | 日志、埋点、简单消息、调试友好场景                |
| Avro     | Kafka、Schema Registry、数据湖、Schema 演进场景   |
| Protobuf | 高性能传输、跨语言服务、强约束消息协议            |
| CSV      | 简单文件导入、离线补数、人工构造测试数据          |
| POJO     | Flink 内部 Java Bean 模型，适合类型明确的业务处理 |

生产项目中，不建议核心链路长期使用无约束 JSON 字符串贯穿全流程。推荐在 Source 入口处尽快转换为 Java Bean 或强 Schema 对象，并在脏数据处理逻辑中记录解析失败原因。

### 测试依赖

测试依赖用于单元测试、算子测试、MiniCluster 测试、Table API 测试和端到端集成测试。Flink 官方测试依赖文档说明，DataStream API 测试可以引入 `flink-test-utils`，其中包含可在 JUnit 测试中运行作业的轻量级 `MiniCluster`；Table API 和 SQL 本地测试还可以额外引入 `flink-table-test-utils`。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/configuration/testing/?utm_source=chatgpt.com))

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- JUnit 5 测试框架 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>${junit.version}</version>
        <scope>test</scope>
    </dependency>

    <!-- AssertJ 断言库，提高测试断言可读性 -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <version>3.27.3</version>
        <scope>test</scope>
    </dependency>

    <!-- Flink DataStream 测试工具，包含 MiniCluster 等能力 -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-test-utils</artifactId>
        <version>${flink.version}</version>
        <scope>test</scope>
    </dependency>

    <!-- Flink Table API 测试工具，用于 Table/SQL 本地测试 -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-table-test-utils</artifactId>
        <version>${flink.version}</version>
        <scope>test</scope>
    </dependency>

    <!-- Mockito，用于 Mock 外部依赖或回调逻辑 -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>5.15.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

测试依赖使用建议：

| 测试目标                | 推荐依赖                                 |
| ----------------------- | ---------------------------------------- |
| 工具类测试              | JUnit 5、AssertJ                         |
| 参数解析测试            | JUnit 5、Hutool                          |
| Map/FlatMap/Filter 测试 | JUnit 5                                  |
| 有状态算子测试          | Flink Test Utils                         |
| MiniCluster 集成测试    | `flink-test-utils`                       |
| Table/SQL 测试          | `flink-table-test-utils`                 |
| 外部系统集成测试        | Testcontainers、嵌入式 Kafka、测试数据库 |

测试依赖必须使用 `test` 作用域，避免进入生产作业包。

### 依赖冲突处理

Flink 项目最常见的依赖冲突来自 Jackson、Guava、Netty、Kryo、Protobuf、Kafka Client、Hadoop、Hive、HBase、Log4j 和 SLF4J Binding。冲突处理的目标不是“强行让 Maven 不报错”，而是确保本地运行、集群运行、序列化、Connector 和日志行为一致。

常用排查命令如下：

```bash
# 查看完整依赖树
mvn dependency:tree

# 查看指定依赖来源，例如 Jackson
mvn dependency:tree -Dincludes=com.fasterxml.jackson.core

# 查看 Kafka Client 来源
mvn dependency:tree -Dincludes=org.apache.kafka

# 查看日志相关依赖，排查多个 SLF4J Binding
mvn dependency:tree -Dincludes=org.slf4j,org.apache.logging.log4j,ch.qos.logback

# 打包前跳过测试，确认依赖解析和 Shade 插件是否正常
mvn clean package -DskipTests
```

命令说明：`dependency:tree` 用于确认依赖从哪个直接依赖传递进来，`-Dincludes` 可以按 groupId 或 artifactId 过滤重点依赖。排查日志冲突时，应重点检查是否同时存在 Logback、Log4j2、slf4j-simple 等多个绑定。

生产作业常用 Shade 插件示例：

文件位置：`pom.xml`

```xml
<build>
    <plugins>
        <!-- Maven Shade 插件，用于构建 Flink 作业 Fat Jar -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.6.0</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                    <configuration>
                        <!-- 避免生成 dependency-reduced-pom.xml 影响多模块构建 -->
                        <createDependencyReducedPom>false</createDependencyReducedPom>

                        <!-- 过滤签名文件，避免打包后出现安全校验异常 -->
                        <filters>
                            <filter>
                                <artifact>*:*</artifact>
                                <excludes>
                                    <exclude>META-INF/*.SF</exclude>
                                    <exclude>META-INF/*.DSA</exclude>
                                    <exclude>META-INF/*.RSA</exclude>
                                </excludes>
                            </filter>
                        </filters>

                        <transformers>
                            <!-- 合并 SPI 文件，避免 Connector、Format、Factory 无法加载 -->
                            <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>

                            <!-- 指定作业入口类 -->
                            <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                <mainClass>io.github.atengk.flink.job.UserBehaviorJob</mainClass>
                            </transformer>
                        </transformers>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

依赖冲突处理原则如下：

1. Flink Runtime 已提供的依赖不要重复打入，除非明确知道原因。
2. Connector、Format、自定义 UDF、第三方业务 SDK 通常需要打入作业 Jar。
3. 日志实现只保留一种，业务代码只面向 SLF4J。
4. Hadoop、Hive、HBase 相关依赖冲突风险高，应单独验证。
5. 使用 Table/SQL Connector 时必须保留 `META-INF/services`，否则可能出现 Factory 找不到的问题。
6. 出现 `ClassNotFoundException`、`NoSuchMethodError`、`NoClassDefFoundError` 时，优先检查依赖版本和打包内容。
7. 每次新增 Connector 后，都要执行本地启动、集群提交和端到端读写验证。

## Flink 程序入口设计

本章节用于说明 Flink Java 作业的标准入口设计。一个合格的 Flink 程序入口应负责参数解析、配置加载、执行环境初始化、Checkpoint 配置、Source 构建、业务流程编排、Sink 构建和作业提交。入口类不应堆积大量业务逻辑，复杂转换应拆分到 Function、Service、SourceBuilder、SinkBuilder 或独立工具类中。

### StreamExecutionEnvironment 初始化

`StreamExecutionEnvironment` 是 DataStream 作业的核心入口，负责创建数据流、设置并行度、配置运行模式、开启 Checkpoint、设置重启策略并最终提交作业。Flink API 文档说明，`StreamExecutionEnvironment` 是流程序执行上下文，提供控制作业执行、并行度、容错和外部数据访问的方法。([JavaDoc](https://javadoc.io/static/org.apache.flink/flink-streaming-java/1.18.0/org/apache/flink/streaming/api/environment/StreamExecutionEnvironment.html?utm_source=chatgpt.com))

推荐初始化逻辑如下：

```java
package io.github.atengk.flink.job;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.time.Duration;

/**
 * 用户行为实时处理作业
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserBehaviorJob {

    /**
     * 作业入口方法
     *
     * @param args 启动参数
     * @throws Exception 作业提交异常
     */
    public static void main(String[] args) throws Exception {
        ParameterTool parameterTool = ParameterTool.fromArgs(args);
        JobRuntimeConfig config = JobRuntimeConfig.from(parameterTool);

        StreamExecutionEnvironment env = createStreamEnv(config);

        log.info("Flink作业开始初始化，作业名称：{}，环境：{}，并行度：{}",
                config.jobName(), config.env(), config.parallelism());

        // 示例 Source：实际项目中应替换为 Kafka、CDC、文件或自定义 Source
        env.fromElements("user_1001", "user_1002", "user_1003")
                .name("mock-source")
                .uid("mock-source")
                .filter(StrUtil::isNotBlank)
                .name("filter-empty-data")
                .uid("filter-empty-data")
                .print()
                .name("print-sink")
                .uid("print-sink");

        env.execute(config.jobName());
    }

    /**
     * 创建流式执行环境
     *
     * @param config 作业运行配置
     * @return StreamExecutionEnvironment
     */
    private static StreamExecutionEnvironment createStreamEnv(JobRuntimeConfig config) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.setParallelism(config.parallelism());
        env.setRuntimeMode(config.runtimeMode());

        if (config.checkpointEnabled()) {
            env.enableCheckpointing(config.checkpointIntervalMs(), CheckpointingMode.EXACTLY_ONCE);
            env.getCheckpointConfig().setCheckpointTimeout(config.checkpointTimeoutMs());
            env.getCheckpointConfig().setMinPauseBetweenCheckpoints(config.checkpointMinPauseMs());
            env.getCheckpointConfig().setExternalizedCheckpointCleanup(
                    CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION
            );
        }

        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(
                config.restartAttempts(),
                Duration.ofSeconds(config.restartDelaySeconds())
        ));

        return env;
    }

    /**
     * 作业运行配置
     *
     * @author Ateng
     * @since 2026-05-11
     */
    public record JobRuntimeConfig(
            String env,
            String jobName,
            int parallelism,
            RuntimeExecutionMode runtimeMode,
            boolean checkpointEnabled,
            long checkpointIntervalMs,
            long checkpointTimeoutMs,
            long checkpointMinPauseMs,
            int restartAttempts,
            long restartDelaySeconds
    ) {

        /**
         * 从启动参数构建运行配置
         *
         * @param parameterTool Flink 参数工具
         * @return 作业运行配置
         */
        public static JobRuntimeConfig from(ParameterTool parameterTool) {
            String env = parameterTool.get("env", "local");
            String jobName = parameterTool.get("jobName", "user-behavior-job");
            int parallelism = Convert.toInt(parameterTool.get("parallelism"), 1);

            RuntimeExecutionMode runtimeMode = RuntimeExecutionMode.valueOf(
                    parameterTool.get("runtimeMode", "STREAMING").toUpperCase()
            );

            boolean checkpointEnabled = Convert.toBool(parameterTool.get("checkpointEnabled"), true);
            long checkpointIntervalMs = Convert.toLong(parameterTool.get("checkpointIntervalMs"), 60_000L);
            long checkpointTimeoutMs = Convert.toLong(parameterTool.get("checkpointTimeoutMs"), 300_000L);
            long checkpointMinPauseMs = Convert.toLong(parameterTool.get("checkpointMinPauseMs"), 30_000L);

            int restartAttempts = Convert.toInt(parameterTool.get("restartAttempts"), 3);
            long restartDelaySeconds = Convert.toLong(parameterTool.get("restartDelaySeconds"), 10L);

            if (StrUtil.isBlank(jobName)) {
                throw new IllegalArgumentException("作业名称不能为空");
            }

            return new JobRuntimeConfig(
                    env,
                    jobName,
                    parallelism,
                    runtimeMode,
                    checkpointEnabled,
                    checkpointIntervalMs,
                    checkpointTimeoutMs,
                    checkpointMinPauseMs,
                    restartAttempts,
                    restartDelaySeconds
            );
        }
    }
}
```

这段代码展示了一个可直接扩展的作业入口：先解析参数，再初始化 `StreamExecutionEnvironment`，然后配置并行度、运行模式、Checkpoint 和重启策略，最后构建数据流并执行。实际项目中应将 Source、Transformation 和 Sink 拆到独立类中，入口类只保留编排逻辑。

启动示例：

```bash
mvn clean package -DskipTests

flink run \
  -c io.github.atengk.flink.job.UserBehaviorJob \
  target/flink-job-user-behavior.jar \
  --env local \
  --jobName user-behavior-job \
  --parallelism 2 \
  --runtimeMode STREAMING \
  --checkpointEnabled true
```

### ExecutionEnvironment 初始化

`ExecutionEnvironment` 需要区分版本语境。早期 Flink 中，`org.apache.flink.api.java.ExecutionEnvironment` 通常用于 DataSet 批处理 API；在新的批流统一实践中，更推荐使用 DataStream API 并通过 `RuntimeExecutionMode.BATCH` 处理有界数据。Flink 关于批执行模式的资料说明，DataStream API 支持 `STREAMING`、`BATCH` 和 `AUTOMATIC` 三种运行模式，其中 `BATCH` 适用于有界输入，官方也建议执行模式优先通过提交参数配置，而不是硬编码在程序中。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.13/docs/dev/datastream/execution_mode/index.html?utm_source=chatgpt.com))

因此，新项目推荐使用以下方式表达批处理作业：

```java
package io.github.atengk.flink.job;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * 有界数据批处理作业示例
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class BoundedBatchJob {

    /**
     * 批处理作业入口
     *
     * @param args 启动参数
     * @throws Exception 作业提交异常
     */
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 有界数据批处理建议使用 BATCH 模式
        env.setRuntimeMode(RuntimeExecutionMode.BATCH);
        env.setParallelism(2);

        log.info("有界批处理作业开始启动，运行模式：{}", RuntimeExecutionMode.BATCH);

        env.fromElements("order_1001", "order_1002", "order_1003")
                .name("mock-bounded-source")
                .uid("mock-bounded-source")
                .print()
                .name("print-sink")
                .uid("print-sink");

        env.execute("bounded-batch-job");
    }
}
```

如果是维护旧项目，并且旧项目仍使用 DataSet API，则可以保留旧式 `ExecutionEnvironment` 初始化。但对于新项目，建议统一基于 DataStream API 设计实时与批处理入口，这样有利于复用 Source、Sink、模型、参数和部署逻辑。

### TableEnvironment 初始化

`TableEnvironment` 用于 Table API 和 SQL 作业。若需要和 DataStream API 混用，推荐使用 `StreamTableEnvironment`。Flink Table API 文档说明，Table API 和 SQL 可以统一处理流和批，并且可以与 DataStream API 集成。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/table/overview/?utm_source=chatgpt.com))

基础初始化示例：

```java
package io.github.atengk.flink.job;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

/**
 * Flink SQL 作业入口示例
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserBehaviorSqlJob {

    /**
     * SQL 作业入口
     *
     * @param args 启动参数
     * @throws Exception 作业提交异常
     */
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(2);
        env.setRuntimeMode(RuntimeExecutionMode.STREAMING);

        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        log.info("Flink SQL作业开始初始化，运行模式：{}", RuntimeExecutionMode.STREAMING);

        tableEnv.executeSql("""
                CREATE TEMPORARY TABLE user_behavior_source (
                    user_id STRING,
                    event_type STRING,
                    event_time TIMESTAMP(3),
                    WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND
                ) WITH (
                    'connector' = 'datagen',
                    'rows-per-second' = '10',
                    'fields.user_id.length' = '8',
                    'fields.event_type.length' = '6'
                )
                """);

        tableEnv.executeSql("""
                CREATE TEMPORARY TABLE user_behavior_sink (
                    user_id STRING,
                    event_type STRING,
                    event_time TIMESTAMP(3)
                ) WITH (
                    'connector' = 'print'
                )
                """);

        tableEnv.executeSql("""
                INSERT INTO user_behavior_sink
                SELECT user_id, event_type, event_time
                FROM user_behavior_source
                WHERE user_id IS NOT NULL
                """);

        log.info("Flink SQL作业已提交");
    }
}
```

这个示例使用 `datagen` 和 `print` Connector 便于本地验证 SQL 作业入口。生产项目中应替换为 Kafka、JDBC、Doris、StarRocks、Iceberg 或 Hive 等实际 Source/Sink，并按照 Connector 文档引入对应依赖。

### 参数解析

参数解析用于将提交命令中的 `--env`、`--jobName`、`--parallelism`、`--config`、`--runtimeMode`、`--checkpointIntervalMs` 等参数转换为作业配置。Flink 自带 `ParameterTool`，适合解析命令行参数；Hutool 可以辅助完成字符串校验、类型转换和默认值处理。

推荐参数规范如下：

| 参数                     | 示例                           | 说明                |
| ------------------------ | ------------------------------ | ------------------- |
| `--env`                  | `local`、`test`、`prod`        | 运行环境            |
| `--jobName`              | `user-behavior-job`            | 作业名称            |
| `--config`               | `config/prod/application.yml`  | 配置文件路径        |
| `--parallelism`          | `4`                            | 作业默认并行度      |
| `--runtimeMode`          | `STREAMING`、`BATCH`           | 运行模式            |
| `--checkpointEnabled`    | `true`                         | 是否启用 Checkpoint |
| `--checkpointIntervalMs` | `60000`                        | Checkpoint 间隔     |
| `--fromSavepoint`        | `hdfs:///flink/savepoints/...` | Savepoint 恢复路径  |

参数解析工具示例：

```java
package io.github.atengk.flink.common.param;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.java.utils.ParameterTool;

/**
 * Flink 作业参数解析工具
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class FlinkJobParamParser {

    /**
     * 解析作业参数
     *
     * @param args 命令行参数
     * @return 作业参数对象
     */
    public static FlinkJobParam parse(String[] args) {
        ParameterTool parameterTool = ParameterTool.fromArgs(args);

        String env = parameterTool.get("env", "local");
        String jobName = parameterTool.get("jobName");
        String config = parameterTool.get("config", StrUtil.format("config/{}/application.yml", env));

        if (StrUtil.isBlank(jobName)) {
            throw new IllegalArgumentException("启动参数 jobName 不能为空");
        }

        int parallelism = Convert.toInt(parameterTool.get("parallelism"), 1);
        if (parallelism <= 0) {
            throw new IllegalArgumentException("启动参数 parallelism 必须大于 0");
        }

        RuntimeExecutionMode runtimeMode = RuntimeExecutionMode.valueOf(
                parameterTool.get("runtimeMode", "STREAMING").toUpperCase()
        );

        return new FlinkJobParam(env, jobName, config, parallelism, runtimeMode);
    }

    /**
     * Flink 作业参数
     *
     * @author Ateng
     * @since 2026-05-11
     */
    public record FlinkJobParam(
            String env,
            String jobName,
            String config,
            int parallelism,
            RuntimeExecutionMode runtimeMode
    ) {
    }
}
```

参数解析原则如下：

1. 必填参数必须显式校验。
2. 数字参数必须校验范围，例如并行度必须大于 0。
3. 枚举参数必须限制取值，例如 `runtimeMode` 只能使用 Flink 支持的运行模式。
4. 敏感参数不建议通过命令行传递，应使用 Secret、环境变量或配置中心。
5. 作业启动时可以打印参数摘要，但不能打印密码、Token、密钥。

### 配置加载

配置加载用于读取不同环境下的 Kafka、数据库、Redis、Checkpoint、Sink、告警和业务规则参数。配置文件不应散落在代码中，应通过 `--config` 参数显式指定，或按 `--env` 推导默认路径。

推荐配置文件结构：

文件位置：`config/local/application.yml`

```yaml
# 作业基础配置
job:
  name: user-behavior-job
  env: local
  parallelism: 2

# Checkpoint 配置
checkpoint:
  enabled: true
  interval-ms: 60000
  timeout-ms: 300000
  min-pause-ms: 30000
  storage: file:///tmp/flink-checkpoints/user-behavior-job

# Kafka 配置
kafka:
  bootstrap-servers: localhost:9092
  source-topic: user_behavior_log
  sink-topic: user_behavior_clean
  group-id: user-behavior-job-local

# 脏数据配置
dirty:
  enabled: true
  output-path: file:///tmp/flink-dirty/user-behavior-job
```

配置加载工具示例：

```java
package io.github.atengk.flink.common.config;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.setting.yaml.YamlUtil;

import java.util.Map;

/**
 * Flink 作业配置加载工具
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class FlinkJobConfigLoader {

    /**
     * 加载 YAML 配置
     *
     * @param configPath 配置文件路径
     * @return 配置 Map
     */
    public static Map<String, Object> loadYaml(String configPath) {
        if (StrUtil.isBlank(configPath)) {
            throw new IllegalArgumentException("配置文件路径不能为空");
        }
        if (!FileUtil.exist(configPath)) {
            throw new IllegalArgumentException(StrUtil.format("配置文件不存在：{}", configPath));
        }
        return YamlUtil.loadByPath(configPath);
    }

    /**
     * 获取字符串配置
     *
     * @param config 配置 Map
     * @param key 配置项名称
     * @param defaultValue 默认值
     * @return 配置值
     */
    public static String getString(Map<String, Object> config, String key, String defaultValue) {
        Object value = config.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }
}
```

上面的示例适合简单配置读取。生产项目中，如果配置层级较深，建议封装为强类型配置对象，例如 `KafkaConfig`、`CheckpointConfig`、`SinkConfig`，不要在业务代码中反复使用字符串 Key。

配置加载原则如下：

1. 作业启动时先加载配置，再初始化 Source 和 Sink。
2. 配置项应有默认值、范围校验和异常提示。
3. 生产密码、密钥、Token 不得明文提交到 Git。
4. 本地、测试、生产配置必须物理隔离。
5. 配置加载失败应直接终止作业，不应使用错误默认值继续运行。

### 作业提交入口

作业提交入口是生产运行的标准入口，通常由 `main()` 方法、Maven 打包、`flink run` 命令、YARN Application、Kubernetes Application 或 Flink Kubernetes Operator 共同组成。入口类应保持稳定，部署脚本通过参数控制环境、并行度、配置路径和恢复方式。

标准提交命令示例：

```bash
# 构建作业 Jar
mvn clean package -DskipTests

# 提交作业到 Flink 集群
flink run \
  -c io.github.atengk.flink.job.UserBehaviorJob \
  target/flink-job-user-behavior.jar \
  --env prod \
  --jobName user-behavior-job \
  --config /data/flink/config/prod/application.yml \
  --parallelism 4 \
  --runtimeMode STREAMING \
  --checkpointEnabled true \
  --checkpointIntervalMs 60000
```

命令说明：`-c` 指定入口类，Jar 后面的参数会传递给用户程序。`--config` 用于指定生产配置文件，`--parallelism` 用于控制作业默认并行度，`--runtimeMode` 用于控制流式或批式运行模式。Flink 官方运行模式资料说明，执行模式也可以通过提交参数设置，例如 `-Dexecution.runtime-mode=BATCH`。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.13/docs/dev/datastream/execution_mode/index.html?utm_source=chatgpt.com))

从 Savepoint 恢复提交示例：

```bash
flink run \
  -s hdfs:///flink/savepoints/user-behavior-job/savepoint-20260511 \
  -c io.github.atengk.flink.job.UserBehaviorJob \
  target/flink-job-user-behavior.jar \
  --env prod \
  --jobName user-behavior-job \
  --config /data/flink/config/prod/application.yml \
  --parallelism 4
```

作业提交入口设计原则如下：

1. 入口类名称一旦进入生产，应尽量保持稳定。
2. `uid()` 必须为关键算子显式设置，便于状态恢复。
3. 作业名称应具有业务含义，并与监控、日志、Checkpoint 路径保持一致。
4. 生产参数应通过脚本、平台或 Operator 管理，不应人工手写长命令。
5. 从 Savepoint 恢复时，代码变更必须验证状态兼容性。

### 本地调试入口

本地调试入口用于快速验证作业逻辑、配置加载、日志输出和简单数据流。它不应替代生产入口，而应作为开发阶段的辅助入口。生产作业入口保持干净，本地调试入口可以使用 Mock Source、`fromElements`、本地文件或 datagen 生成测试数据。

本地调试入口示例：

```java
package io.github.atengk.flink.local;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * 用户行为作业本地调试入口
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserBehaviorLocalDebug {

    /**
     * 本地调试入口
     *
     * @param args 启动参数
     * @throws Exception 作业执行异常
     */
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.setParallelism(1);
        env.setRuntimeMode(RuntimeExecutionMode.STREAMING);

        log.info("本地调试作业开始启动");

        env.fromElements(
                        "{\"userId\":\"1001\",\"eventType\":\"click\",\"eventTime\":\"2026-05-11 10:00:00\"}",
                        "{\"userId\":\"1002\",\"eventType\":\"pay\",\"eventTime\":\"2026-05-11 10:00:03\"}",
                        "{\"userId\":\"\",\"eventType\":\"click\",\"eventTime\":\"2026-05-11 10:00:05\"}"
                )
                .name("local-mock-source")
                .uid("local-mock-source")
                .filter(value -> value.contains("userId"))
                .name("local-filter")
                .uid("local-filter")
                .print()
                .name("local-print-sink")
                .uid("local-print-sink");

        env.execute("user-behavior-local-debug");
    }
}
```

本地调试运行命令：

```bash
mvn clean package -DskipTests

java \
  -Dfile.encoding=UTF-8 \
  -Dlog4j.configurationFile=src/main/resources/log4j2-local.properties \
  -cp target/flink-job-user-behavior.jar \
  io.github.atengk.flink.local.UserBehaviorLocalDebug
```

本地调试原则如下：

1. 本地入口可以使用 Mock 数据，但生产入口必须接入真实 Source。
2. 本地调试不应写入生产 Kafka、Redis、数据库或 OLAP 表。
3. 本地调试日志可以更详细，但不要影响生产日志配置。
4. 涉及 Checkpoint、状态恢复、反压和资源调度的问题，必须在 MiniCluster 或测试集群验证。
5. 本地入口可以保留在 `local` 包或 `test` 源目录中，不建议混入生产作业主流程。


## 配置管理

本章节用于统一 Flink 作业的配置管理方式。Flink 作业通常需要同时管理运行环境、并行度、Checkpoint、Source、Sink、状态后端、日志、告警和业务参数。如果配置设计不清晰，后续很容易出现本地能运行、测试环境异常、生产环境参数不可追踪的问题。

Flink 官方应用参数文档也强调，几乎所有 Flink 应用都依赖外部配置参数，例如输入输出路径、系统参数、并行度和业务参数，并提供了 `ParameterTool` 用于处理基础参数读取。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.15/docs/dev/datastream/application_parameters/?utm_source=chatgpt.com))

### 本地配置

本地配置用于开发人员在本机快速启动 Flink 作业，主要目标是方便调试、降低环境依赖和避免误连生产资源。本地配置通常连接本地 Kafka、MySQL、Redis、文件目录或 Mock 数据源。

推荐本地配置目录如下：

```text
config/
└── local/
    ├── application.yml
    ├── log4j2.properties
    └── mock-data/
        ├── user_behavior.json
        └── order_event.json
```

本地配置文件示例：

文件位置：`config/local/application.yml`

```yaml
# 作业基础配置
job:
  name: user-behavior-job
  env: local
  parallelism: 2
  runtime-mode: STREAMING

# Checkpoint 本地配置
checkpoint:
  enabled: true
  interval-ms: 60000
  timeout-ms: 300000
  min-pause-ms: 30000
  storage: file:///tmp/flink-checkpoints/user-behavior-job

# Kafka 本地配置
kafka:
  bootstrap-servers: localhost:9092
  group-id: user-behavior-job-local
  source-topic: user_behavior_log
  sink-topic: user_behavior_clean
  startup-mode: earliest

# MySQL CDC 本地配置
mysql-cdc:
  hostname: localhost
  port: 3306
  username: flink
  password: flink123456
  database-list: ateng_flink
  table-list: ateng_flink.user_behavior
  server-time-zone: Asia/Shanghai

# 文件数据源配置
file-source:
  path: file:///tmp/flink-data/user_behavior.json
  monitor-continuously: false
  discovery-interval-seconds: 10

# Socket 数据源配置，仅用于本地调试
socket:
  hostname: localhost
  port: 9999

# 脏数据输出配置
dirty:
  enabled: true
  output-path: file:///tmp/flink-dirty/user-behavior-job
```

本地配置应遵循以下原则：

| 配置项          | 建议                                            |
| --------------- | ----------------------------------------------- |
| 中间件地址      | 使用 `localhost`、Docker Compose 或测试环境地址 |
| Checkpoint 路径 | 使用本地 `file:///tmp/...`                      |
| 并行度          | 设置为 1 或 2，便于调试                         |
| Source          | 优先支持 Mock、文件、Socket 或本地 Kafka        |
| Sink            | 优先输出到 Print、文件或本地 Kafka              |
| 敏感信息        | 本地可使用测试账号，但禁止复用生产账号          |

本地配置不追求高可用和高吞吐，重点是让开发人员能快速复现数据解析、转换、过滤、窗口、状态和 Sink 写入逻辑。

### 测试环境配置

测试环境配置用于验证作业在接近生产的中间件和部署方式下是否稳定运行。它应接入测试 Kafka、测试 MySQL、测试 Redis、测试 OLAP 存储和测试告警通道，禁止直接连接生产环境。

推荐测试配置目录如下：

```text
config/
└── test/
    ├── application.yml
    ├── log4j2.properties
    └── README.md
```

测试配置文件示例：

文件位置：`config/test/application.yml`

```yaml
# 作业基础配置
job:
  name: user-behavior-job
  env: test
  parallelism: 4
  runtime-mode: STREAMING

# 测试环境 Checkpoint 配置
checkpoint:
  enabled: true
  interval-ms: 60000
  timeout-ms: 300000
  min-pause-ms: 30000
  storage: hdfs:///flink/checkpoints/test/user-behavior-job

# Kafka 测试环境配置
kafka:
  bootstrap-servers: kafka-test-01:9092,kafka-test-02:9092,kafka-test-03:9092
  group-id: user-behavior-job-test
  source-topic: user_behavior_log_test
  sink-topic: user_behavior_clean_test
  startup-mode: latest

# MySQL CDC 测试环境配置
mysql-cdc:
  hostname: mysql-test
  port: 3306
  username: flink_test
  password: ${MYSQL_CDC_PASSWORD}
  database-list: ateng_flink_test
  table-list: ateng_flink_test.user_behavior
  server-time-zone: Asia/Shanghai
  server-id: 5400-5499

# 脏数据输出配置
dirty:
  enabled: true
  output-path: hdfs:///flink/dirty/test/user-behavior-job
```

测试环境应重点验证以下内容：

| 验证项        | 说明                                                 |
| ------------- | ---------------------------------------------------- |
| 参数加载      | 确认 `--env test`、`--config`、`--parallelism` 生效  |
| Source 连通性 | Kafka、CDC、文件系统是否能正常读取                   |
| Sink 连通性   | Kafka、JDBC、Doris、Redis 等输出端是否能写入         |
| Checkpoint    | 是否稳定成功，是否存在超时或状态过大                 |
| 数据正确性    | 输入样本与输出结果是否符合预期                       |
| 失败恢复      | Kill TaskManager、重启作业后是否能恢复               |
| 日志与监控    | 是否能在 Web UI、日志平台、Prometheus 中看到关键指标 |

测试环境配置应尽量模拟生产参数，但可以适当降低并行度、数据量和资源配置，避免测试环境资源浪费。

### 生产环境配置

生产环境配置用于正式作业运行，要求稳定、安全、可审计、可回滚。生产配置不能只关注“作业能启动”，还要覆盖资源、状态、容错、安全、告警和恢复路径。

生产配置目录建议如下：

```text
config/
└── prod/
    ├── application.yml
    ├── log4j2.properties
    └── application.example.yml
```

生产配置文件示例：

文件位置：`config/prod/application.yml`

```yaml
# 作业基础配置
job:
  name: user-behavior-job
  env: prod
  parallelism: 8
  runtime-mode: STREAMING

# 生产 Checkpoint 配置
checkpoint:
  enabled: true
  interval-ms: 60000
  timeout-ms: 600000
  min-pause-ms: 30000
  max-concurrent-checkpoints: 1
  storage: hdfs:///flink/checkpoints/prod/user-behavior-job
  externalized-retention: RETAIN_ON_CANCELLATION

# Kafka 生产配置
kafka:
  bootstrap-servers: kafka-prod-01:9092,kafka-prod-02:9092,kafka-prod-03:9092
  group-id: user-behavior-job-prod
  source-topic: user_behavior_log
  sink-topic: user_behavior_clean
  startup-mode: latest
  properties:
    security.protocol: SASL_PLAINTEXT
    sasl.mechanism: SCRAM-SHA-512

# MySQL CDC 生产配置
mysql-cdc:
  hostname: mysql-prod-vip
  port: 3306
  username: flink_cdc
  password: ${MYSQL_CDC_PASSWORD}
  database-list: ateng_biz
  table-list: ateng_biz.user_behavior
  server-time-zone: Asia/Shanghai
  server-id: 5600-5799

# 脏数据配置
dirty:
  enabled: true
  output-path: hdfs:///flink/dirty/prod/user-behavior-job

# 告警配置
alarm:
  enabled: true
  channel: webhook
  webhook-url: ${ALARM_WEBHOOK_URL}
```

Flink 的外部化 Checkpoint 支持取消作业时保留状态，`RETAIN_ON_CANCELLATION` 表示作业取消或失败后仍保留外部化 Checkpoint，但这也意味着需要人工或平台负责清理历史状态目录。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/deployment/config/?utm_source=chatgpt.com))

生产配置原则如下：

1. 生产配置必须通过评审，不允许开发人员临时修改后直接提交。
2. 密码、Token、Webhook、AK/SK 等敏感信息不得明文写入 Git。
3. Checkpoint 路径、Savepoint 路径、脏数据路径必须按作业隔离。
4. Kafka `group-id` 必须唯一，避免不同作业抢占消费位点。
5. CDC `server-id` 范围必须按作业隔离，避免多个 CDC Reader 冲突。
6. Sink 必须明确幂等、事务或去重策略。
7. 生产参数变更必须有发布记录和回滚方案。

### 参数化配置

参数化配置用于通过启动参数覆盖配置文件中的默认值。它适合控制环境、作业名称、并行度、配置文件路径、运行模式、消费起点、Savepoint 恢复路径等运行时差异。

推荐标准参数如下：

| 参数                  | 示例                           | 说明                |
| --------------------- | ------------------------------ | ------------------- |
| `--env`               | `local`、`test`、`prod`        | 运行环境            |
| `--jobName`           | `user-behavior-job`            | 作业名称            |
| `--config`            | `config/prod/application.yml`  | 配置文件路径        |
| `--parallelism`       | `8`                            | 默认并行度          |
| `--runtimeMode`       | `STREAMING`、`BATCH`           | 运行模式            |
| `--checkpointEnabled` | `true`                         | 是否启用 Checkpoint |
| `--startupMode`       | `earliest`、`latest`           | Source 启动位置     |
| `--fromSavepoint`     | `hdfs:///flink/savepoints/...` | Savepoint 恢复路径  |

启动命令示例：

```bash
flink run \
  -c io.github.atengk.flink.job.UserBehaviorJob \
  target/flink-job-user-behavior.jar \
  --env prod \
  --jobName user-behavior-job \
  --config /data/flink/config/prod/application.yml \
  --parallelism 8 \
  --runtimeMode STREAMING \
  --checkpointEnabled true
```

参数解析类示例：

下面的代码用于统一解析 Flink 作业启动参数，适合放在公共模块中复用。

文件位置：`flink-common/src/main/java/io/github/atengk/flink/common/config/FlinkJobArgs.java`

```java
package io.github.atengk.flink.common.config;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.java.utils.ParameterTool;

/**
 * Flink 作业启动参数
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public record FlinkJobArgs(
        String env,
        String jobName,
        String config,
        int parallelism,
        RuntimeExecutionMode runtimeMode,
        boolean checkpointEnabled,
        String fromSavepoint
) {

    /**
     * 解析命令行参数
     *
     * @param args 命令行参数
     * @return Flink 作业启动参数
     */
    public static FlinkJobArgs parse(String[] args) {
        ParameterTool parameterTool = ParameterTool.fromArgs(args);

        String env = parameterTool.get("env", "local");
        String jobName = parameterTool.get("jobName");
        String config = parameterTool.get("config", StrUtil.format("config/{}/application.yml", env));
        int parallelism = Convert.toInt(parameterTool.get("parallelism"), 1);
        boolean checkpointEnabled = Convert.toBool(parameterTool.get("checkpointEnabled"), true);
        String fromSavepoint = parameterTool.get("fromSavepoint", "");

        RuntimeExecutionMode runtimeMode = RuntimeExecutionMode.valueOf(
                parameterTool.get("runtimeMode", "STREAMING").toUpperCase()
        );

        if (StrUtil.isBlank(jobName)) {
            throw new IllegalArgumentException("启动参数 jobName 不能为空");
        }
        if (parallelism <= 0) {
            throw new IllegalArgumentException("启动参数 parallelism 必须大于 0");
        }

        log.info("解析作业启动参数完成，环境：{}，作业名称：{}，配置文件：{}，并行度：{}，运行模式：{}",
                env, jobName, config, parallelism, runtimeMode);

        return new FlinkJobArgs(env, jobName, config, parallelism, runtimeMode, checkpointEnabled, fromSavepoint);
    }
}
```

参数化配置原则如下：

1. 配置文件提供默认值，启动参数提供覆盖能力。
2. 生产作业参数必须由脚本、调度平台或 Operator 管理。
3. 不建议通过启动参数传递密码。
4. 启动参数需要打印摘要，便于排查，但敏感字段必须脱敏。
5. 参数优先级应固定，例如：启动参数 > 环境变量 > 配置文件默认值。

### 配置中心集成

配置中心用于统一管理多个 Flink 作业的公共配置、业务规则、维表参数、告警参数和环境差异。常见配置中心包括 Nacos、Apollo、Consul、Spring Cloud Config 或企业内部配置平台。

Flink 作业接入配置中心时，需要谨慎处理“动态变更”。Flink 是长期运行的分布式作业，不是每次请求都重新读取配置的 Web 服务。配置中心更适合用于作业启动阶段拉取配置，或通过广播流、规则流等方式实现业务参数动态更新。

配置中心适用场景如下：

| 场景                  | 建议方式                               |
| --------------------- | -------------------------------------- |
| 启动配置              | 作业启动时从配置中心读取一次           |
| Kafka、数据库连接配置 | 启动时读取，不建议运行中直接变更       |
| 业务规则              | 使用广播流或规则流动态更新             |
| 告警阈值              | 可通过配置中心拉取，也可通过广播流更新 |
| 敏感配置              | 配置中心应结合 Secret 或 KMS           |
| 高风险配置            | 变更后通过 Savepoint 重启作业          |

配置中心集成流程建议如下：

```text
启动参数 --configCenter nacos
        |
        v
读取配置中心地址、命名空间、DataId、Group
        |
        v
拉取作业配置
        |
        v
合并启动参数与配置中心参数
        |
        v
初始化 Flink 环境、Source、Sink、Checkpoint
```

配置中心使用原则：

1. 启动关键配置可以从配置中心读取，但读取失败应直接终止作业。
2. Kafka Topic、数据库表、Checkpoint 路径等高风险配置不建议运行中自动变更。
3. 业务规则动态更新建议使用 Broadcast State，而不是在每条数据处理中访问配置中心。
4. 配置中心连接失败要有清晰日志。
5. 配置中心变更必须有审计记录。
6. 生产环境应保留配置快照，便于故障回滚。

### 敏感配置管理

敏感配置包括数据库密码、Kafka 认证信息、Webhook、Token、AK/SK、证书路径、Kerberos Keytab 等。敏感配置不能明文提交到 Git，也不应完整打印到日志中。

推荐敏感配置来源如下：

| 方式              | 适用场景                  |
| ----------------- | ------------------------- |
| 环境变量          | 本地、容器、CI/CD 注入    |
| Kubernetes Secret | Kubernetes 部署           |
| YARN 本地化文件   | YARN 集群提交             |
| 配置中心加密字段  | 企业配置中心统一管理      |
| KMS               | 高安全要求场景            |
| 挂载证书文件      | SSL、Kerberos、Kafka 认证 |

生产配置中建议使用占位符：

```yaml
mysql-cdc:
  hostname: mysql-prod-vip
  port: 3306
  username: flink_cdc
  password: ${MYSQL_CDC_PASSWORD}

alarm:
  webhook-url: ${ALARM_WEBHOOK_URL}
```

敏感字段脱敏工具示例：

下面的代码用于打印配置摘要时对密码、Token、Secret 等字段进行脱敏。

文件位置：`flink-common/src/main/java/io/github/atengk/flink/common/util/SensitiveMaskUtil.java`

```java
package io.github.atengk.flink.common.util;

import cn.hutool.core.util.StrUtil;

import java.util.Set;

/**
 * 敏感字段脱敏工具
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class SensitiveMaskUtil {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password",
            "passwd",
            "secret",
            "token",
            "access-key",
            "secret-key",
            "webhook",
            "keytab"
    );

    /**
     * 判断是否为敏感字段
     *
     * @param key 字段名称
     * @return 是否敏感
     */
    public static boolean isSensitiveKey(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }
        String lowerKey = key.toLowerCase();
        return SENSITIVE_KEYS.stream().anyMatch(lowerKey::contains);
    }

    /**
     * 脱敏配置值
     *
     * @param key 配置项名称
     * @param value 配置值
     * @return 脱敏后的配置值
     */
    public static String mask(String key, String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        if (!isSensitiveKey(key)) {
            return value;
        }
        return value.length() <= 4 ? "****" : StrUtil.format("{}****{}", value.charAt(0), value.charAt(value.length() - 1));
    }
}
```

敏感配置管理原则：

1. 密码、Token、AK/SK、Webhook 不得进入代码仓库。
2. 作业日志不得打印完整敏感字段。
3. 本地开发账号、测试账号、生产账号必须隔离。
4. 生产密钥应定期轮换。
5. 作业启动失败时，异常信息也不能暴露敏感字段。
6. 配置中心或 Secret 平台需要开启权限控制和审计日志。

### 动态参数传递

动态参数传递用于在作业运行时调整业务逻辑，例如规则配置、维表版本、告警阈值、黑白名单、活动配置等。需要注意，动态参数传递不等同于随意修改 Flink 作业运行参数。资源、并行度、Checkpoint 路径、Source Topic 等基础参数通常应通过重启作业生效。

常见动态参数方式如下：

| 方式             | 适用场景             | 说明                       |
| ---------------- | -------------------- | -------------------------- |
| 启动参数         | 作业启动前确定的参数 | 环境、并行度、配置文件     |
| 配置中心启动拉取 | 作业启动时读取配置   | 中间件地址、路径、阈值     |
| Broadcast State  | 规则动态更新         | 风控规则、告警阈值、黑名单 |
| Kafka 配置流     | 配置变更事件         | 主流和配置流联动           |
| Redis 查询       | 低频维表参数         | 需要缓存和超时控制         |
| 数据库查询       | 管理后台配置         | 不建议逐条数据同步查询     |

动态参数设计原则：

1. 高频业务规则优先使用广播流。
2. 不要在每条数据处理中访问配置中心。
3. 动态参数要有版本号、生效时间和更新时间。
4. 规则变更要记录日志和监控指标。
5. 错误规则不能导致主流作业崩溃，应进入降级逻辑或告警。
6. 动态参数如果影响状态结构，必须通过 Savepoint 和版本升级流程处理。

## 数据源开发

本章节用于说明 Flink Java 项目中常见 Source 的开发方式，包括 Kafka、MySQL CDC、文件、Socket 和自定义 Source。Flink DataStream Connector 文档说明，Flink 内置了基础 Source，例如文件、目录、Socket、集合和迭代器；同时 Flink 项目也提供 Kafka、FileSystem、JDBC、MongoDB、Pulsar 等外部系统 Connector，但很多 Connector 需要用户在应用中额外引入依赖。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/connectors/datastream/overview/?utm_source=chatgpt.com))

### Kafka 数据源

Kafka 是 Flink 实时作业中最常见的数据源，适合处理日志、埋点、交易事件、行为事件、设备数据和上游系统消息。Flink Kafka Connector 支持从 Kafka Topic 读取数据，也支持写入 Kafka，并且在满足 Checkpoint 和事务配置条件时可以支持 Exactly Once 语义。需要注意的是，Flink 2.2 官方 Kafka Connector 文档当前提示尚无对应 Flink 2.2 的 Kafka Connector，因此生产选型时必须先确认 Flink 主版本和 Kafka Connector 版本是否兼容。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-2.2/docs/connectors/datastream/kafka/?utm_source=chatgpt.com))

Kafka Source 配置示例：

```yaml
kafka:
  bootstrap-servers: kafka-prod-01:9092,kafka-prod-02:9092,kafka-prod-03:9092
  group-id: user-behavior-job-prod
  source-topic: user_behavior_log
  startup-mode: latest
  properties:
    security.protocol: SASL_PLAINTEXT
    sasl.mechanism: SCRAM-SHA-512
```

Kafka Source 工厂示例：

下面的代码用于根据配置创建 Kafka 字符串数据源，适合放在 `flink-connector` 模块中复用。

文件位置：`flink-connector/src/main/java/io/github/atengk/flink/connector/kafka/KafkaSourceFactory.java`

```java
package io.github.atengk.flink.connector.kafka;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;

import java.util.Properties;

/**
 * Kafka Source 工厂
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class KafkaSourceFactory {

    /**
     * 创建字符串 Kafka Source
     *
     * @param config Kafka Source 配置
     * @return KafkaSource
     */
    public static KafkaSource<String> createStringSource(KafkaSourceConfig config) {
        validate(config);

        Properties properties = new Properties();
        if (config.securityProtocol() != null) {
            properties.setProperty("security.protocol", config.securityProtocol());
        }
        if (config.saslMechanism() != null) {
            properties.setProperty("sasl.mechanism", config.saslMechanism());
        }

        log.info("创建Kafka数据源，Topic：{}，GroupId：{}，启动模式：{}",
                config.topic(), config.groupId(), config.startupMode());

        return KafkaSource.<String>builder()
                .setBootstrapServers(config.bootstrapServers())
                .setTopics(config.topic())
                .setGroupId(config.groupId())
                .setStartingOffsets(resolveOffsetsInitializer(config.startupMode()))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .setProperties(properties)
                .build();
    }

    /**
     * 解析 Kafka 启动位点
     *
     * @param startupMode 启动模式
     * @return OffsetsInitializer
     */
    private static OffsetsInitializer resolveOffsetsInitializer(String startupMode) {
        String mode = StrUtil.blankToDefault(startupMode, "latest").toLowerCase();
        return switch (mode) {
            case "earliest" -> OffsetsInitializer.earliest();
            case "latest" -> OffsetsInitializer.latest();
            default -> throw new IllegalArgumentException(StrUtil.format("不支持的Kafka启动模式：{}", startupMode));
        };
    }

    /**
     * 校验 Kafka Source 配置
     *
     * @param config Kafka Source 配置
     */
    private static void validate(KafkaSourceConfig config) {
        if (StrUtil.isBlank(config.bootstrapServers())) {
            throw new IllegalArgumentException("Kafka bootstrapServers 不能为空");
        }
        if (StrUtil.isBlank(config.topic())) {
            throw new IllegalArgumentException("Kafka topic 不能为空");
        }
        if (StrUtil.isBlank(config.groupId())) {
            throw new IllegalArgumentException("Kafka groupId 不能为空");
        }
    }

    /**
     * Kafka Source 配置
     *
     * @author Ateng
     * @since 2026-05-11
     */
    public record KafkaSourceConfig(
            String bootstrapServers,
            String topic,
            String groupId,
            String startupMode,
            String securityProtocol,
            String saslMechanism
    ) {
    }
}
```

作业中使用 Kafka Source 示例：

```java
KafkaSource<String> kafkaSource = KafkaSourceFactory.createStringSource(
        new KafkaSourceFactory.KafkaSourceConfig(
                "localhost:9092",
                "user_behavior_log",
                "user-behavior-job-local",
                "earliest",
                null,
                null
        )
);

env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "kafka-user-behavior-source")
        .name("kafka-user-behavior-source")
        .uid("kafka-user-behavior-source")
        .setParallelism(2);
```

Kafka Source 开发注意事项：

1. `group-id` 必须按作业唯一规划。
2. 启动模式要明确，测试环境常用 `earliest`，生产环境常用 `latest` 或从 Checkpoint 恢复。
3. 生产恢复应以 Checkpoint 或 Savepoint 为准，不应随意改启动位点。
4. Kafka 分区数会影响 Source 最大有效并行度。
5. 数据解析失败不能导致作业频繁重启，应进入脏数据处理链路。
6. 使用认证时，SASL、SSL、Kerberos 配置应从安全配置中读取。

### MySQL CDC 数据源

MySQL CDC 数据源用于读取 MySQL 表的快照数据和增量 Binlog 数据，适合实时同步、实时宽表、维表变更流、订单状态流和业务数据库变更捕获。Flink CDC 3.6 文档说明，MySQL CDC Connector 可以读取 MySQL 快照数据和增量数据，支持 MySQL 5.7、8.0.x、8.4+，也支持部分云数据库和 MariaDB，并提供 Maven 依赖 `flink-connector-mysql-cdc:3.6.0`。([Apache Nightlies](https://nightlies.apache.org/flink/flink-cdc-docs-release-3.6/docs/connectors/flink-sources/mysql-cdc/?utm_source=chatgpt.com))

Maven 依赖示例：

```xml
<!-- Flink CDC MySQL Connector，用于读取 MySQL 快照和 Binlog 增量数据 -->
<dependency>
    <groupId>org.apache.flink</groupId>
    <artifactId>flink-connector-mysql-cdc</artifactId>
    <version>3.6.0</version>
</dependency>

<!-- MySQL JDBC Driver，Flink CDC 文档说明 MySQL Connector 需要手动配置 MySQL 驱动 -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.27</version>
</dependency>
```

MySQL CDC 配置示例：

```yaml
mysql-cdc:
  hostname: mysql-prod-vip
  port: 3306
  username: flink_cdc
  password: ${MYSQL_CDC_PASSWORD}
  database-list: ateng_biz
  table-list: ateng_biz.user_behavior
  server-time-zone: Asia/Shanghai
  server-id: 5600-5799
```

MySQL CDC Source 工厂示例：

下面的代码用于创建 MySQL CDC Source，将 Debezium 变更事件转换为 JSON 字符串。

文件位置：`flink-connector/src/main/java/io/github/atengk/flink/connector/cdc/MySqlCdcSourceFactory.java`

```java
package io.github.atengk.flink.connector.cdc;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.cdc.connectors.mysql.source.MySqlSource;
import org.apache.flink.cdc.connectors.mysql.source.MySqlSourceBuilder;
import org.apache.flink.cdc.debezium.JsonDebeziumDeserializationSchema;

/**
 * MySQL CDC Source 工厂
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class MySqlCdcSourceFactory {

    /**
     * 创建 MySQL CDC JSON Source
     *
     * @param config MySQL CDC 配置
     * @return MySQL CDC Source
     */
    public static MySqlSource<String> createJsonSource(MySqlCdcSourceConfig config) {
        validate(config);

        MySqlSourceBuilder<String> builder = MySqlSource.<String>builder()
                .hostname(config.hostname())
                .port(config.port())
                .databaseList(split(config.databaseList()))
                .tableList(split(config.tableList()))
                .username(config.username())
                .password(config.password())
                .serverTimeZone(StrUtil.blankToDefault(config.serverTimeZone(), "Asia/Shanghai"))
                .deserializer(new JsonDebeziumDeserializationSchema());

        if (StrUtil.isNotBlank(config.serverId())) {
            builder.serverId(config.serverId());
        }

        log.info("创建MySQL CDC数据源，数据库：{}，表：{}，ServerId：{}",
                config.databaseList(), config.tableList(), config.serverId());

        return builder.build();
    }

    /**
     * 拆分逗号分隔配置
     *
     * @param value 配置值
     * @return 字符串数组
     */
    private static String[] split(String value) {
        return StrUtil.splitToArray(value, ',');
    }

    /**
     * 校验 MySQL CDC 配置
     *
     * @param config MySQL CDC 配置
     */
    private static void validate(MySqlCdcSourceConfig config) {
        if (StrUtil.isBlank(config.hostname())) {
            throw new IllegalArgumentException("MySQL CDC hostname 不能为空");
        }
        if (config.port() <= 0) {
            throw new IllegalArgumentException("MySQL CDC port 必须大于 0");
        }
        if (StrUtil.isBlank(config.username())) {
            throw new IllegalArgumentException("MySQL CDC username 不能为空");
        }
        if (StrUtil.isBlank(config.password())) {
            throw new IllegalArgumentException("MySQL CDC password 不能为空");
        }
        if (StrUtil.isBlank(config.databaseList())) {
            throw new IllegalArgumentException("MySQL CDC databaseList 不能为空");
        }
        if (StrUtil.isBlank(config.tableList())) {
            throw new IllegalArgumentException("MySQL CDC tableList 不能为空");
        }
    }

    /**
     * MySQL CDC Source 配置
     *
     * @author Ateng
     * @since 2026-05-11
     */
    public record MySqlCdcSourceConfig(
            String hostname,
            int port,
            String username,
            String password,
            String databaseList,
            String tableList,
            String serverTimeZone,
            String serverId
    ) {
    }
}
```

作业中使用 MySQL CDC Source 示例：

```java
MySqlSource<String> mySqlSource = MySqlCdcSourceFactory.createJsonSource(
        new MySqlCdcSourceFactory.MySqlCdcSourceConfig(
                "localhost",
                3306,
                "flink",
                "flink123456",
                "ateng_flink",
                "ateng_flink.user_behavior",
                "Asia/Shanghai",
                "5400-5499"
        )
);

env.fromSource(mySqlSource, WatermarkStrategy.noWatermarks(), "mysql-cdc-user-behavior-source")
        .name("mysql-cdc-user-behavior-source")
        .uid("mysql-cdc-user-behavior-source")
        .setParallelism(4);
```

MySQL CDC 开发注意事项：

1. MySQL 用户需要具备读取表结构、读取数据和读取 Binlog 的权限。
2. 每个 CDC Reader 需要唯一 `server-id`，并行读取时应配置足够范围。
3. MySQL 表最好有主键，便于增量快照切分和下游 Upsert。
4. 初次启动通常读取快照后继续读取 Binlog。
5. 生产恢复应从 Checkpoint 或 Savepoint 恢复，不应随意切换启动模式。
6. 表结构变更需要验证 Debezium、Flink CDC 和下游 Schema 兼容性。
7. CDC Source 的快照阶段和 Binlog 阶段会影响 Checkpoint 行为、状态大小和读取延迟。

Flink CDC 文档说明，MySQL CDC 支持增量快照读取，并且在增量快照读取中可以按 chunk 粒度执行 Checkpoint；如果 Source 需要并行读取，每个并行 Reader 应有唯一 server id，且 server-id 范围应大于并行度。([Apache Nightlies](https://nightlies.apache.org/flink/flink-cdc-docs-release-3.6/docs/connectors/flink-sources/mysql-cdc/?utm_source=chatgpt.com))

### 文件数据源

文件数据源适合离线补数、历史回放、本地调试、批处理和从 HDFS/S3/OSS 等文件系统读取数据。Flink FileSystem Connector 提供统一的 Source 和 Sink，支持批处理和流处理，可以从 POSIX、S3、HDFS 等 Flink 支持的文件系统读取或写入文件，并支持 Avro、CSV、Parquet 等格式。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/connectors/datastream/filesystem/?utm_source=chatgpt.com))

文件 Source 配置示例：

```yaml
file-source:
  path: file:///tmp/flink-data/user_behavior.json
  monitor-continuously: false
  discovery-interval-seconds: 10
```

文件 Source 工厂示例：

下面的代码用于创建按行读取文本文件的 FileSource，可按配置选择一次性读取或持续监听目录。

文件位置：`flink-connector/src/main/java/io/github/atengk/flink/connector/file/FileSourceFactory.java`

```java
package io.github.atengk.flink.connector.file;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.connector.file.src.FileSource;
import org.apache.flink.connector.file.src.reader.TextLineInputFormat;
import org.apache.flink.core.fs.Path;

import java.time.Duration;

/**
 * 文件 Source 工厂
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class FileSourceFactory {

    /**
     * 创建文本行文件 Source
     *
     * @param config 文件 Source 配置
     * @return FileSource
     */
    public static FileSource<String> createTextLineSource(FileSourceConfig config) {
        validate(config);

        FileSource.FileSourceBuilder<String> builder = FileSource
                .forRecordStreamFormat(new TextLineInputFormat(), new Path(config.path()));

        if (config.monitorContinuously()) {
            builder.monitorContinuously(Duration.ofSeconds(config.discoveryIntervalSeconds()));
        }

        log.info("创建文件数据源，路径：{}，是否持续监听：{}，发现间隔秒数：{}",
                config.path(), config.monitorContinuously(), config.discoveryIntervalSeconds());

        return builder.build();
    }

    /**
     * 校验文件 Source 配置
     *
     * @param config 文件 Source 配置
     */
    private static void validate(FileSourceConfig config) {
        if (StrUtil.isBlank(config.path())) {
            throw new IllegalArgumentException("文件数据源路径不能为空");
        }
        if (config.monitorContinuously() && config.discoveryIntervalSeconds() <= 0) {
            throw new IllegalArgumentException("文件持续监听间隔必须大于 0");
        }
    }

    /**
     * 文件 Source 配置
     *
     * @author Ateng
     * @since 2026-05-11
     */
    public record FileSourceConfig(
            String path,
            boolean monitorContinuously,
            long discoveryIntervalSeconds
    ) {
    }
}
```

作业中使用文件 Source 示例：

```java
FileSource<String> fileSource = FileSourceFactory.createTextLineSource(
        new FileSourceFactory.FileSourceConfig(
                "file:///tmp/flink-data/user_behavior.json",
                false,
                10
        )
);

env.fromSource(fileSource, WatermarkStrategy.noWatermarks(), "file-user-behavior-source")
        .name("file-user-behavior-source")
        .uid("file-user-behavior-source")
        .setParallelism(1);
```

文件数据源开发注意事项：

1. 本地路径使用 `file:///`，生产环境通常使用 `hdfs:///`、`s3://` 或对象存储路径。
2. 批处理补数建议使用一次性读取。
3. 持续监听目录适合准实时文件采集，但要考虑文件落地完整性。
4. 文件名、分区目录、时间范围应作为作业参数。
5. 大文件读取需要合理设置并行度和文件切分策略。
6. 文件内容解析失败应进入脏数据输出，不应直接丢弃。

Flink File Source 基于新的 Source API，包含 `SplitEnumerator` 和 `SourceReader` 两部分：前者负责发现文件和分配 Split，后者负责读取具体文件内容。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/connectors/datastream/filesystem/?utm_source=chatgpt.com))

### Socket 数据源

Socket 数据源通常用于本地调试、教学演示和验证简单转换逻辑，不适合生产环境。Flink 预定义 Source 包括从 Socket 中读取数据，因此可以直接通过 `socketTextStream` 快速构造输入流。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/zh/docs/connectors/datastream/overview/?utm_source=chatgpt.com))

Socket 配置示例：

```yaml
socket:
  hostname: localhost
  port: 9999
```

Socket Source 使用示例：

```java
DataStreamSource<String> socketSource = env.socketTextStream("localhost", 9999)
        .name("socket-debug-source")
        .uid("socket-debug-source")
        .setParallelism(1);
```

本地启动 Socket 输入：

```bash
# 启动一个本地 Socket 服务
nc -lk 9999

# 输入测试数据
{"userId":"1001","eventType":"click","eventTime":"2026-05-11 10:00:00"}
{"userId":"1002","eventType":"pay","eventTime":"2026-05-11 10:00:03"}
```

Socket 数据源注意事项：

1. Socket Source 不具备生产级容错能力。
2. Socket 数据通常无法可靠重放，不能用于 Exactly Once 链路。
3. Socket 适合调试 `map`、`filter`、`flatMap`、简单窗口和解析逻辑。
4. 不建议在生产代码中保留 Socket Source 作为正式入口。
5. 本地调试完成后，应替换为 Kafka、CDC、文件或正式自定义 Source。

### 自定义 Source

自定义 Source 用于接入官方 Connector 未覆盖的外部系统，例如内部 HTTP 接口、私有 MQ、专有设备协议、遗留系统或特殊文件格式。Flink 官方 Connector 文档也说明，使用 Connector 不是接入外部系统的唯一方式，常见模式还包括在算子中查询外部数据库或 Web 服务，并可通过异步 I/O 提升稳定性和效率。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/connectors/datastream/overview/?utm_source=chatgpt.com))

生产级自定义 Source 建议优先基于新的 Source API 设计；如果只是小规模内部系统、调试工具或非关键链路，可以使用简单的 `RichParallelSourceFunction` 实现。下面示例用于说明基础开发模式。

自定义 Source 示例：

下面的代码模拟从外部系统轮询数据，并支持作业取消时停止读取。

文件位置：`flink-connector/src/main/java/io/github/atengk/flink/connector/custom/PollingMockSource.java`

```java
package io.github.atengk.flink.connector.custom;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;

/**
 * 轮询模拟 Source
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class PollingMockSource extends RichParallelSourceFunction<String> {

    private final long intervalMs;
    private volatile boolean running = true;

    /**
     * 创建轮询模拟 Source
     *
     * @param intervalMs 轮询间隔毫秒数
     */
    public PollingMockSource(long intervalMs) {
        if (intervalMs <= 0) {
            throw new IllegalArgumentException("轮询间隔必须大于 0");
        }
        this.intervalMs = intervalMs;
    }

    /**
     * 运行 Source
     *
     * @param ctx Source 上下文
     * @throws Exception Source 运行异常
     */
    @Override
    public void run(SourceContext<String> ctx) throws Exception {
        int subtaskIndex = getRuntimeContext().getIndexOfThisSubtask();
        log.info("自定义Source开始运行，Subtask：{}，轮询间隔：{}ms", subtaskIndex, intervalMs);

        while (running) {
            String event = """
                    {"id":"%s","subtask":%d,"eventTime":"%s","eventType":"mock"}
                    """.formatted(IdUtil.fastSimpleUUID(), subtaskIndex, DateUtil.now());

            synchronized (ctx.getCheckpointLock()) {
                ctx.collect(event);
            }

            Thread.sleep(intervalMs);
        }

        log.info("自定义Source已停止，Subtask：{}", subtaskIndex);
    }

    /**
     * 取消 Source
     */
    @Override
    public void cancel() {
        running = false;
        log.info("收到自定义Source取消信号");
    }
}
```

作业中使用自定义 Source 示例：

```java
env.addSource(new PollingMockSource(1000))
        .name("polling-mock-source")
        .uid("polling-mock-source")
        .setParallelism(2)
        .print()
        .name("print-sink")
        .uid("print-sink");
```

自定义 Source 开发注意事项：

1. 必须支持 `cancel()`，否则作业停止可能卡住。
2. 长连接、线程池、HTTP 客户端等资源应在 `open()` 和 `close()` 中管理。
3. 如果需要 Exactly Once，必须实现可持久化位点和 Checkpoint 状态。
4. 外部系统读取失败要有重试、退避和告警。
5. 不要在 Source 中无限制缓存数据。
6. 对外部接口轮询时要控制频率，避免压垮上游系统。
7. 生产级 Source 应优先评估 FLIP-27 Source API。

### Source 并行度配置

Source 并行度决定数据读取能力，也会影响上游压力、下游分区、状态大小和 Checkpoint 时间。并行度不是越高越好，必须结合数据源能力、分区数、连接数限制和业务吞吐目标设置。

常见 Source 并行度建议如下：

| Source 类型      | 并行度建议                                            |
| ---------------- | ----------------------------------------------------- |
| Kafka Source     | 不超过 Kafka Topic 分区数，超过分区数通常没有实际收益 |
| MySQL CDC Source | 快照阶段可并行读取，但需要配置足够 server-id 范围     |
| 文件 Source      | 与文件数量、文件大小、切分能力相关                    |
| Socket Source    | 通常固定为 1                                          |
| 自定义 Source    | 取决于外部系统是否支持并发读取                        |
| 配置流 Source    | 通常固定为 1，再通过 Broadcast 分发                   |

Source 并行度示例：

```java
env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "kafka-source")
        .name("kafka-source")
        .uid("kafka-source")
        .setParallelism(4);

env.fromSource(mySqlSource, WatermarkStrategy.noWatermarks(), "mysql-cdc-source")
        .name("mysql-cdc-source")
        .uid("mysql-cdc-source")
        .setParallelism(4);

env.socketTextStream("localhost", 9999)
        .name("socket-source")
        .uid("socket-source")
        .setParallelism(1);
```

Source 并行度配置原则：

1. Kafka Source 并行度应结合 Topic 分区数。
2. CDC Source 并行度要结合表主键分布、server-id 范围和 MySQL 压力。
3. 文件 Source 并行度要结合文件切分和小文件数量。
4. 配置流、规则流、Socket Source 通常使用并行度 1。
5. Source 并行度变更可能影响状态恢复，生产变更前应验证 Savepoint。
6. 不同 Source 可以单独设置并行度，不必完全等于作业默认并行度。

### Source 容错处理

Source 容错处理用于保证作业失败后能够从正确位置恢复。Flink 的状态快照会包含数据源位置，例如文件偏移或 Kafka 分区 Offset，以及有状态算子的状态；Checkpoint 用于故障恢复，Savepoint 更适合作业升级、重部署和扩缩容等运维操作。([apache.googlesource.com](https://apache.googlesource.com/flink/%2B/master/docs/content/docs/learn-flink/fault_tolerance.md?utm_source=chatgpt.com))

不同 Source 的容错能力如下：

| Source           | 容错能力   | 说明                                            |
| ---------------- | ---------- | ----------------------------------------------- |
| Kafka Source     | 强         | Offset 可纳入 Checkpoint，失败后可重放          |
| MySQL CDC Source | 强         | 快照和 Binlog 位点可恢复，需正确配置 Checkpoint |
| 文件 Source      | 中到强     | 取决于文件系统、读取模式和文件是否稳定          |
| Socket Source    | 弱         | 通常不可重放，不适合生产一致性要求              |
| 自定义 Source    | 取决于实现 | 必须自行管理位点、状态和恢复逻辑                |

Source 容错配置示例：

```java
env.enableCheckpointing(60_000L, CheckpointingMode.EXACTLY_ONCE);

env.getCheckpointConfig().setCheckpointTimeout(300_000L);
env.getCheckpointConfig().setMinPauseBetweenCheckpoints(30_000L);
env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
env.getCheckpointConfig().setExternalizedCheckpointCleanup(
        CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION
);
```

Source 容错设计原则：

1. 生产实时作业必须开启 Checkpoint。
2. 可重放 Source 才能支持可靠恢复，例如 Kafka、CDC、稳定文件系统。
3. 不可重放 Source 不应承诺 Exactly Once。
4. Source、状态、Sink 必须一起评估端到端一致性。
5. 作业失败恢复后，要验证是否重复读取、漏读或乱序扩大。
6. 关键 Source 必须监控读取延迟、消费积压、读取错误和 Checkpoint 对齐耗时。
7. 生产作业必须设置稳定的算子 `uid()`，避免升级后状态无法恢复。

Source 容错排查建议：

| 问题               | 排查方向                                                     |
| ------------------ | ------------------------------------------------------------ |
| Kafka 重复消费     | 检查 Checkpoint 是否成功、group-id 是否变更、启动位点是否被覆盖 |
| Kafka 数据延迟     | 检查 Topic 分区数、Source 并行度、下游反压                   |
| CDC 快照慢         | 检查表大小、主键分布、并行度、MySQL 压力                     |
| CDC 位点异常       | 检查 server-id、Binlog 保留时间、权限、Checkpoint 日志       |
| 文件重复读取       | 检查文件监听模式、文件命名、落盘原子性                       |
| Socket 丢数据      | Socket 本身不可可靠重放，应替换为 Kafka 或文件回放           |
| 自定义 Source 重复 | 检查是否实现位点状态和 Checkpoint 恢复逻辑                   |


## 数据解析与模型设计

本章节用于规范 Flink 作业中原始数据、解析逻辑、业务模型、Schema 演进和脏数据处理方式。Flink 的 DataStream API 可以处理任意可序列化的数据类型，常见高效类型包括基础类型、Tuple 和 POJO；其他复杂类型通常会退化到通用序列化机制，因此模型设计会直接影响作业性能、状态兼容性和后续维护成本。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/learn-flink/datastream_api/?utm_source=chatgpt.com))

### 原始数据格式

原始数据格式是 Source 端读入 Flink 后的第一层数据形态，通常来自 Kafka、CDC、文件、Socket 或自定义 Source。原始数据不应直接贯穿整个计算链路，应尽早转换为结构化 Java Bean、Avro 对象、Protobuf 对象或 Table Row。

常见原始数据格式如下：

| 格式            | 示例来源                   | 适用场景               | 注意事项                        |
| --------------- | -------------------------- | ---------------------- | ------------------------------- |
| JSON 字符串     | Kafka、日志文件、HTTP 采集 | 埋点、日志、简单事件   | 易调试，但 Schema 约束弱        |
| Avro 二进制     | Kafka、文件、数据湖        | 强 Schema、Schema 演进 | 需要维护 Avro Schema            |
| Protobuf 二进制 | Kafka、服务消息            | 高性能、跨语言协议     | 需要维护 `.proto` 文件和生成类  |
| Debezium JSON   | MySQL CDC、Kafka CDC       | 数据库变更捕获         | 需要处理 before、after、op 字段 |
| CSV 文本        | 文件、离线补数             | 简单批量导入           | 字段顺序、转义、空值要严格约定  |
| Java 对象       | 自定义 Source、测试数据    | 内部程序流转           | 需要满足 Flink 类型和序列化要求 |

推荐统一封装原始事件模型，避免 Source 直接输出业务模型。这样可以保留原始消息、来源、接收时间、Topic、分区、Offset 等排查信息。

下面的模型用于封装 Source 读入后的原始事件，适合放在 `model` 模块中。

文件位置：`flink-model/src/main/java/io/github/atengk/flink/model/RawEvent.java`

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 原始事件模型
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RawEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据来源，例如 kafka、mysql-cdc、file
     */
    private String source;

    /**
     * 原始数据内容
     */
    private String payload;

    /**
     * 接收时间戳，单位毫秒
     */
    private Long receiveTime;
}
```

原始数据设计原则：

1. 保留原始内容，便于异常排查和数据回放。
2. Source 信息、Topic、分区、Offset、文件路径等元信息尽量保留。
3. 不要在 Source 层写复杂业务逻辑，Source 层只负责读取和基础封装。
4. 进入核心计算前必须完成格式解析、字段校验和脏数据分流。
5. 原始数据格式必须形成文档，明确字段含义、类型、是否必填和示例数据。

### JSON 数据解析

JSON 是实时日志和埋点系统中最常见的数据格式。Flink Table/SQL 也提供 JSON Format，JSON Format 可以基于表结构读写 JSON 数据，但普通 DataStream 项目中通常会使用 Jackson、Hutool 或 Fastjson2 将 JSON 字符串解析为 Java Bean。Flink 文档说明，JSON Format 基于 JSON Schema 读写数据，且在 Table 生态中 Schema 通常由表结构推导。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/connectors/table/formats/json/?utm_source=chatgpt.com))

JSON 原始数据示例：

```json
{
  "userId": "1001",
  "itemId": "sku_8899",
  "categoryId": "cat_10",
  "eventType": "click",
  "eventTime": 1778464800000
}
```

业务事件模型如下。

文件位置：`flink-model/src/main/java/io/github/atengk/flink/model/UserBehaviorEvent.java`

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户行为事件模型
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBehaviorEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 商品ID
     */
    private String itemId;

    /**
     * 类目ID
     */
    private String categoryId;

    /**
     * 行为类型，例如 click、cart、pay
     */
    private String eventType;

    /**
     * 事件时间戳，单位毫秒
     */
    private Long eventTime;
}
```

JSON 解析函数如下，解析失败或字段不合法时输出到脏数据侧输出流。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/function/UserBehaviorParseFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.function;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.flink.model.DirtyData;
import io.github.atengk.flink.model.UserBehaviorEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

/**
 * 用户行为 JSON 解析函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserBehaviorParseFunction extends ProcessFunction<String, UserBehaviorEvent> {

    public static final OutputTag<DirtyData> DIRTY_OUTPUT_TAG = new OutputTag<DirtyData>("dirty-json-data") {
    };

    /**
     * 处理单条 JSON 数据
     *
     * @param value 原始 JSON 字符串
     * @param ctx 处理上下文
     * @param out 主流输出
     */
    @Override
    public void processElement(String value, Context ctx, Collector<UserBehaviorEvent> out) {
        try {
            if (StrUtil.isBlank(value)) {
                outputDirty(ctx, value, "EMPTY_DATA", "原始数据为空");
                return;
            }

            UserBehaviorEvent event = JSONUtil.toBean(value, UserBehaviorEvent.class);
            String validateMessage = validate(event);
            if (StrUtil.isNotBlank(validateMessage)) {
                outputDirty(ctx, value, "INVALID_FIELD", validateMessage);
                return;
            }

            out.collect(event);
        } catch (Exception e) {
            outputDirty(ctx, value, "JSON_PARSE_ERROR", ExceptionUtil.getSimpleMessage(e));
            log.warn("用户行为数据解析失败，原因：{}", ExceptionUtil.getSimpleMessage(e));
        }
    }

    /**
     * 校验用户行为事件
     *
     * @param event 用户行为事件
     * @return 校验失败原因
     */
    private String validate(UserBehaviorEvent event) {
        if (event == null) {
            return "事件对象为空";
        }
        if (StrUtil.isBlank(event.getUserId())) {
            return "userId不能为空";
        }
        if (StrUtil.isBlank(event.getEventType())) {
            return "eventType不能为空";
        }
        if (event.getEventTime() == null || event.getEventTime() <= 0) {
            return "eventTime不合法";
        }
        return "";
    }

    /**
     * 输出脏数据
     *
     * @param ctx 处理上下文
     * @param payload 原始内容
     * @param reason 脏数据原因
     * @param message 错误信息
     */
    private void outputDirty(Context ctx, String payload, String reason, String message) {
        DirtyData dirtyData = new DirtyData(
                "user-behavior-json",
                payload,
                reason,
                message,
                System.currentTimeMillis()
        );
        ctx.output(DIRTY_OUTPUT_TAG, dirtyData);
    }
}
```

JSON 解析建议：

| 项目     | 建议                                         |
| -------- | -------------------------------------------- |
| 字段校验 | 必填字段必须校验，不能依赖下游报错           |
| 时间字段 | 推荐统一为毫秒时间戳或 ISO-8601 字符串       |
| 枚举字段 | 行为类型、事件类型、状态码应限制取值范围     |
| 解析失败 | 输出到侧输出流，不建议直接抛异常导致作业重启 |
| 原文保留 | 脏数据中保留原始 JSON，便于修复和回放        |
| 高频日志 | 不要逐条打印完整脏数据内容，只打印摘要和计数 |

### Avro 数据解析

Avro 适合强 Schema、跨系统传输、Schema 演进和数据湖场景。Flink 对 Apache Avro 有内置支持，可以基于 Avro Schema 读写 Avro 数据，Flink 序列化框架也可以处理 Avro Schema 生成的类；使用 Avro 时需要引入 `flink-avro` 依赖。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/connectors/datastream/formats/avro/?utm_source=chatgpt.com))

Maven 依赖示例：

```xml
<!-- Flink Avro 格式支持，用于 Avro 文件、Avro Schema 生成类和 Avro 序列化 -->
<dependency>
    <groupId>org.apache.flink</groupId>
    <artifactId>flink-avro</artifactId>
    <version>${flink.version}</version>
</dependency>

<!-- Apache Avro 核心依赖，用于 Schema、GenericRecord、SpecificRecord 等能力 -->
<dependency>
    <groupId>org.apache.avro</groupId>
    <artifactId>avro</artifactId>
    <version>1.12.0</version>
</dependency>
```

Avro Schema 示例：

文件位置：`src/main/avro/user_behavior.avsc`

```json
{
  "type": "record",
  "name": "UserBehaviorAvro",
  "namespace": "io.github.atengk.flink.avro",
  "fields": [
    {
      "name": "userId",
      "type": "string"
    },
    {
      "name": "itemId",
      "type": ["null", "string"],
      "default": null
    },
    {
      "name": "categoryId",
      "type": ["null", "string"],
      "default": null
    },
    {
      "name": "eventType",
      "type": "string"
    },
    {
      "name": "eventTime",
      "type": "long"
    }
  ]
}
```

Avro 使用建议：

1. 优先使用 Avro 生成类，不建议在核心链路长期使用 `GenericRecord`。
2. Schema 文件应纳入版本管理。
3. 字段新增应优先设置默认值。
4. 字段删除、类型变更、语义变更必须经过兼容性评估。
5. Kafka 中使用 Avro 时，建议配合 Schema Registry 或企业内部 Schema 管理平台。
6. 状态中保存 Avro 对象时，升级前必须验证状态恢复兼容性。

在 Table/SQL 场景中，Avro Format 也可用于 Kafka、Upsert Kafka、Kinesis 和 Filesystem 等 Connector，官方文档说明 Table Avro Format 的 Schema 通常由表结构推导。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/connectors/table/formats/avro/?utm_source=chatgpt.com))

### Protobuf 数据解析

Protobuf 适合高性能、强类型、跨语言通信场景，常用于服务间消息、设备数据、交易事件和高吞吐 Kafka 消息。Flink 的 Table Protobuf Format 支持基于 Protobuf 生成类读写数据，并要求指定 Protobuf 生成类的完整类名；该格式当前主要用于 Table/SQL 连接器场景。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/connectors/table/formats/protobuf/?utm_source=chatgpt.com))

Protobuf 文件示例：

文件位置：`src/main/proto/user_behavior.proto`

```proto
syntax = "proto3";

package io.github.atengk.flink.proto;

option java_package = "io.github.atengk.flink.proto";
option java_outer_classname = "UserBehaviorProto";

message UserBehavior {
  string user_id = 1;
  string item_id = 2;
  string category_id = 3;
  string event_type = 4;
  int64 event_time = 5;
}
```

Protobuf 依赖示例：

```xml
<!-- Protobuf Java 运行时，用于解析和构建 Protobuf 消息 -->
<dependency>
    <groupId>com.google.protobuf</groupId>
    <artifactId>protobuf-java</artifactId>
    <version>4.29.3</version>
</dependency>
```

Protobuf 解析示例：

下面的代码用于将 Protobuf 二进制消息转换为业务 Java Bean。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/function/UserBehaviorProtobufMapFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.function;

import io.github.atengk.flink.model.UserBehaviorEvent;
import io.github.atengk.flink.proto.UserBehaviorProto;
import org.apache.flink.api.common.functions.MapFunction;

/**
 * 用户行为 Protobuf 解析函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class UserBehaviorProtobufMapFunction implements MapFunction<byte[], UserBehaviorEvent> {

    /**
     * 将 Protobuf 字节数组解析为用户行为事件
     *
     * @param value Protobuf 字节数组
     * @return 用户行为事件
     * @throws Exception 解析异常
     */
    @Override
    public UserBehaviorEvent map(byte[] value) throws Exception {
        UserBehaviorProto.UserBehavior message = UserBehaviorProto.UserBehavior.parseFrom(value);
        return new UserBehaviorEvent(
                message.getUserId(),
                message.getItemId(),
                message.getCategoryId(),
                message.getEventType(),
                message.getEventTime()
        );
    }
}
```

Protobuf 使用建议：

1. `.proto` 文件必须版本化管理。
2. 字段编号不能复用。
3. 删除字段时应保留编号，避免后续误用。
4. 生产链路中不要依赖字段顺序，应依赖字段编号。
5. Protobuf 解析异常应输出到脏数据侧输出流。
6. 如果使用 Table/SQL Protobuf Format，应确认 Connector、Format 和 Protobuf 版本兼容性。

Flink Table Format 总览显示，Protobuf Format 支持 Kafka Connector，而 JSON、Avro、Debezium CDC、Canal CDC 等格式在不同 Connector 中也有对应支持范围。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/connectors/table/formats/overview/?utm_source=chatgpt.com))

### Java Bean 模型设计

Java Bean 是 Flink Java 项目中最常用的业务模型。模型应清晰表达业务语义，不要把原始 JSON 字符串、解析中间状态、错误信息和最终输出结果混在同一个类中。

推荐按职责拆分模型：

| 模型类型   | 示例                       | 说明                         |
| ---------- | -------------------------- | ---------------------------- |
| 原始模型   | `RawEvent`                 | 保存 Source 原始数据和元信息 |
| 输入模型   | `UserBehaviorEvent`        | 保存解析后的业务事件         |
| 中间模型   | `UserEventAggregate`       | 保存聚合、Join、窗口中间结果 |
| 输出模型   | `UserBehaviorResult`       | 保存 Sink 写出的最终结构     |
| 脏数据模型 | `DirtyData`                | 保存异常数据和失败原因       |
| 配置模型   | `JobConfig`、`KafkaConfig` | 保存作业配置                 |

脏数据模型如下。

文件位置：`flink-model/src/main/java/io/github/atengk/flink/model/DirtyData.java`

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 脏数据模型
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DirtyData implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据来源
     */
    private String source;

    /**
     * 原始数据
     */
    private String payload;

    /**
     * 脏数据原因
     */
    private String reason;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间戳，单位毫秒
     */
    private Long createTime;
}
```

聚合结果模型如下。

文件位置：`flink-model/src/main/java/io/github/atengk/flink/model/EventCount.java`

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 事件统计结果模型
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventCount implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 统计数量
     */
    private Long count;
}
```

模型设计原则：

1. 每个模型只表达一种数据阶段。
2. 字段命名使用业务语义，不使用 `field1`、`value1` 这类弱语义名称。
3. 时间字段统一单位，例如毫秒时间戳。
4. 状态中保存的模型必须谨慎变更字段。
5. 输出模型应与 Sink 表结构或消息 Schema 对齐。
6. 模型类建议实现 `Serializable`。
7. 不建议在模型类中写复杂业务逻辑。

### POJO 类型约束

Flink 对 POJO 类型有明确识别规则。官方 Java API 文档说明，一个 Flink POJO 应是 `public` 独立类，不能是非静态内部类；需要有 public 无参构造器；所有非静态、非 transient 字段要么是 public 且非 final，要么有符合 Java Bean 规范的 getter 和 setter。Flink POJO 是固定长度、可感知 null 的复合类型，每个字段可以独立为 null。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/api/java/org/apache/flink/api/common/typeinfo/Types.html?utm_source=chatgpt.com))

推荐 POJO 写法：

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 标准 Flink POJO 示例
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StandardFlinkPojo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String name;

    private Long eventTime;
}
```

不推荐写法：

```java
// 不推荐：没有无参构造器，字段 final，Flink 可能无法按 POJO 高效识别
public class BadPojo {

    private final String id;

    public BadPojo(String id) {
        this.id = id;
    }
}
```

POJO 约束建议：

| 规则             | 说明                                            |
| ---------------- | ----------------------------------------------- |
| public 类        | 模型类应声明为 public                           |
| 无参构造器       | 使用 Lombok `@NoArgsConstructor` 或手写无参构造 |
| getter/setter    | 使用 Lombok `@Data` 或手写 Java Bean 方法       |
| 避免 final 字段  | final 字段不适合普通 Flink POJO                 |
| 避免非静态内部类 | 模型类应放在独立文件中                          |
| 避免 Object 字段 | Object 会削弱类型推断和序列化效率               |
| 明确泛型         | List、Map 等字段要明确泛型类型                  |

Flink 类型系统在无法提取有效 POJO 类型信息时可能抛出类型异常；对于 Map、List 等集合类型，官方 API 也建议显式提供类型信息，因为默认情况下集合可能被当作泛型类型处理。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/api/java/org/apache/flink/api/common/typeinfo/Types.html?utm_source=chatgpt.com))

### Schema 管理

Schema 管理用于保证 Source、Flink 模型、状态、Sink 和下游系统对字段结构的理解一致。实时作业中的 Schema 变更风险高，特别是字段删除、类型变更、嵌套结构变更和状态对象变更。

Schema 管理对象包括：

| 类型            | 管理对象                                 |
| --------------- | ---------------------------------------- |
| JSON Schema     | Kafka JSON、日志事件、埋点事件           |
| Avro Schema     | `.avsc` 文件、Schema Registry            |
| Protobuf Schema | `.proto` 文件、生成类                    |
| CDC Schema      | 数据库表结构、Debezium 变更事件          |
| Java Bean       | Flink 内部模型和状态模型                 |
| Sink Schema     | Kafka 消息、Doris 表、数据库表、数据湖表 |

Schema 变更建议如下：

| 变更类型     | 风险 | 建议                          |
| ------------ | ---- | ----------------------------- |
| 新增可选字段 | 低   | 设置默认值，模型字段允许 null |
| 新增必填字段 | 中   | 需要灰度，旧数据可能解析失败  |
| 删除字段     | 中高 | 下游确认无依赖后再删除        |
| 字段改名     | 高   | 等同于删除旧字段并新增新字段  |
| 字段类型变更 | 高   | 可能导致解析失败、状态不兼容  |
| 嵌套结构变更 | 高   | 需要端到端回归测试            |
| 状态模型变更 | 高   | 需要 Savepoint 恢复验证       |

Schema 管理原则：

1. Schema 文件必须进入版本控制。
2. 生产消息字段不能随意删除或改类型。
3. JSON 事件也应有文档化 Schema，不应只依赖样例数据。
4. Avro 和 Protobuf 应通过 Schema 文件生成代码。
5. State 中使用的模型变更必须单独评估。
6. Sink 表结构变更要与作业发布顺序配合。
7. 每次 Schema 变更都要提供兼容性说明、回滚方案和样例数据。

### 脏数据处理

脏数据处理用于隔离解析失败、字段缺失、枚举非法、时间异常、Schema 不兼容和业务校验失败的数据。脏数据不应直接丢弃，也不应频繁导致作业重启。Flink Side Output 可以从主流计算结果之外产生任意数量的侧输出流，侧输出流类型可以不同于主流类型，适合用于脏数据、迟到数据和异常数据分流。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/side_output/?utm_source=chatgpt.com))

脏数据分类建议：

| 类型          | 示例                                    |
| ------------- | --------------------------------------- |
| 空数据        | 空字符串、null 消息                     |
| JSON 解析失败 | JSON 格式错误、字段类型不匹配           |
| 必填字段缺失  | `userId`、`eventType`、`eventTime` 缺失 |
| 枚举非法      | `eventType=unknown`                     |
| 时间异常      | 事件时间为空、过早、过晚                |
| Schema 不兼容 | 新旧版本字段结构不一致                  |
| 业务校验失败  | 金额为负、状态流转非法                  |

脏数据侧输出使用示例：

```java
SingleOutputStreamOperator<UserBehaviorEvent> parsedStream = sourceStream
        .process(new UserBehaviorParseFunction())
        .name("parse-user-behavior-json")
        .uid("parse-user-behavior-json");

DataStream<DirtyData> dirtyStream = parsedStream
        .getSideOutput(UserBehaviorParseFunction.DIRTY_OUTPUT_TAG)
        .name("dirty-data-stream");

dirtyStream
        .map(JSONUtil::toJsonStr)
        .name("dirty-data-to-json")
        .uid("dirty-data-to-json")
        .print()
        .name("dirty-data-print-sink")
        .uid("dirty-data-print-sink");
```

脏数据处理原则：

1. 解析失败、校验失败和业务异常应分类记录。
2. 脏数据应包含原始数据、失败原因、异常摘要和处理时间。
3. 高频脏数据不要逐条打印完整日志。
4. 脏数据应输出到 Kafka、文件、对象存储或专门表。
5. 脏数据量需要监控和告警。
6. 可修复脏数据应支持回放补偿。
7. 脏数据处理链路不能影响主流稳定性。

## DataStream API 开发

本章节用于说明 Flink DataStream API 的常用转换算子，包括 `map`、`flatMap`、`filter`、`keyBy`、`reduce`、`process`、`union`、`connect` 和 `side output`。Flink 官方算子文档说明，算子可以将一个或多个 DataStream 转换成新的 DataStream，多个转换可以组合成复杂的数据流拓扑。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/zh/docs/dev/datastream/operators/overview/?utm_source=chatgpt.com))

### map 转换

`map` 用于一进一出转换，即输入一条数据，输出一条数据。它适合字段映射、类型转换、补充默认值、简单计算和格式转换。Flink 官方算子文档将 `map` 定义为输入一个元素并输出一个元素。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/zh/docs/dev/datastream/operators/overview/?utm_source=chatgpt.com))

示例：将用户行为事件转换为事件统计对象。

```java
DataStream<EventCount> eventCountStream = eventStream
        .map(event -> new EventCount(event.getEventType(), 1L))
        .name("map-event-to-count")
        .uid("map-event-to-count");
```

如果转换逻辑较复杂，建议使用独立 `MapFunction`。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/function/EventCountMapFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.function;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.flink.model.EventCount;
import io.github.atengk.flink.model.UserBehaviorEvent;
import org.apache.flink.api.common.functions.MapFunction;

/**
 * 事件统计映射函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class EventCountMapFunction implements MapFunction<UserBehaviorEvent, EventCount> {

    /**
     * 将用户行为事件转换为事件统计对象
     *
     * @param value 用户行为事件
     * @return 事件统计对象
     */
    @Override
    public EventCount map(UserBehaviorEvent value) {
        String eventType = StrUtil.blankToDefault(value.getEventType(), "unknown");
        return new EventCount(eventType, 1L);
    }
}
```

`map` 使用建议：

1. 只做一进一出的轻量转换。
2. 不要在 `map` 中访问慢速外部系统。
3. 不要在 `map` 中吞掉异常后返回 null。
4. 复杂解析建议使用 `process` 或 `flatMap` 配合脏数据输出。
5. 为关键算子设置 `name()` 和 `uid()`，便于监控和状态恢复。

### flatMap 转换

`flatMap` 用于一进零出、一进一出或一进多出转换。它适合拆分数组、展开嵌套字段、过滤异常数据、解析一条消息中的多条业务记录。Flink 官方算子文档将 `flatMap` 定义为输入一个元素并输出零个、一个或多个元素。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/zh/docs/dev/datastream/operators/overview/?utm_source=chatgpt.com))

示例：将一条包含多个事件的 JSON 数组展开为多条事件。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/function/UserBehaviorArrayFlatMapFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.function;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.flink.model.UserBehaviorEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.util.Collector;

import java.util.List;

/**
 * 用户行为数组展开函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserBehaviorArrayFlatMapFunction implements FlatMapFunction<String, UserBehaviorEvent> {

    /**
     * 展开 JSON 数组数据
     *
     * @param value JSON 数组字符串
     * @param out 输出收集器
     */
    @Override
    public void flatMap(String value, Collector<UserBehaviorEvent> out) {
        try {
            List<UserBehaviorEvent> events = JSONUtil.toList(value, UserBehaviorEvent.class);
            if (CollUtil.isEmpty(events)) {
                return;
            }
            for (UserBehaviorEvent event : events) {
                out.collect(event);
            }
        } catch (Exception e) {
            log.warn("用户行为数组展开失败，原因：{}", e.getMessage());
        }
    }
}
```

使用示例：

```java
DataStream<UserBehaviorEvent> eventStream = jsonArrayStream
        .flatMap(new UserBehaviorArrayFlatMapFunction())
        .name("flatmap-user-behavior-array")
        .uid("flatmap-user-behavior-array");
```

`flatMap` 使用建议：

1. 适合一条输入拆成多条输出。
2. 如果需要输出脏数据，优先使用 `ProcessFunction`。
3. 不要在 `flatMap` 中静默丢弃关键业务异常。
4. 输出数据量可能放大，需评估下游吞吐。
5. 数组展开、文本切词、订单明细展开等场景适合使用。

### filter 过滤

`filter` 用于保留满足条件的数据。Flink 官方算子文档说明，`filter` 会对每个元素执行布尔函数，只保留返回 true 的元素。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/zh/docs/dev/datastream/operators/overview/?utm_source=chatgpt.com))

示例：只保留支付事件。

```java
DataStream<UserBehaviorEvent> payEventStream = eventStream
        .filter(event -> StrUtil.equals(event.getEventType(), "pay"))
        .name("filter-pay-event")
        .uid("filter-pay-event");
```

复杂过滤建议封装为独立函数。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/function/ValidUserBehaviorFilterFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.function;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.flink.model.UserBehaviorEvent;
import org.apache.flink.api.common.functions.FilterFunction;

/**
 * 有效用户行为过滤函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class ValidUserBehaviorFilterFunction implements FilterFunction<UserBehaviorEvent> {

    /**
     * 判断用户行为事件是否有效
     *
     * @param value 用户行为事件
     * @return 是否保留
     */
    @Override
    public boolean filter(UserBehaviorEvent value) {
        return value != null
                && StrUtil.isNotBlank(value.getUserId())
                && StrUtil.isNotBlank(value.getEventType())
                && value.getEventTime() != null
                && value.getEventTime() > 0;
    }
}
```

`filter` 使用建议：

1. 只用于保留或丢弃数据，不用于转换字段。
2. 关键业务过滤条件需要有注释或配置说明。
3. 被过滤的数据如果需要审计，应使用侧输出流，而不是直接过滤。
4. 高频过滤逻辑应保持轻量，避免访问外部系统。
5. 对于异常数据，建议先进行脏数据分流，再过滤主流。

### keyBy 分组

`keyBy` 用于按照 Key 对流进行逻辑分区。相同 Key 的记录会进入同一个分区，后续可以执行 keyed state、reduce、窗口聚合、定时器等操作。Flink 官方算子文档说明，`keyBy()` 内部通过哈希分区实现；文档也提醒，未重写 `hashCode()` 的 POJO 类和任意数组不能作为 Key。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/zh/docs/dev/datastream/operators/overview/?utm_source=chatgpt.com))

示例：按事件类型分组。

```java
KeyedStream<EventCount, String> keyedStream = eventCountStream
        .keyBy(EventCount::getEventType);
```

常见分组 Key 设计：

| Key 类型 | 示例                       | 适用场景                   |
| -------- | -------------------------- | -------------------------- |
| 用户 ID  | `userId`                   | 用户维度统计、用户状态维护 |
| 订单 ID  | `orderId`                  | 订单去重、订单状态流转     |
| 店铺 ID  | `shopId`                   | 店铺指标聚合               |
| 事件类型 | `eventType`                | 行为类型统计               |
| 组合 Key | `shopId + "_" + eventType` | 多维度聚合                 |

`keyBy` 使用建议：

1. Key 字段必须稳定，不应使用随机值或处理时间。
2. Key 的基数要合理，基数过低容易数据倾斜。
3. 自定义 Key 类型必须实现稳定的 `hashCode()` 和 `equals()`。
4. 数组不适合作为 Key。
5. Keyed State 必须先 `keyBy` 后使用。
6. 分组后并行度变更会导致状态重新分布，生产变更前应验证 Savepoint。

### reduce 聚合

`reduce` 用于对相同 Key 的数据进行滚动聚合。Flink 官方算子文档说明，`reduce` 会在相同 Key 的数据流上滚动执行，将当前元素与上一次 reduce 结果组合并输出新结果。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/zh/docs/dev/datastream/operators/overview/?utm_source=chatgpt.com))

示例：按事件类型累计数量。

```java
DataStream<EventCount> reducedStream = eventCountStream
        .keyBy(EventCount::getEventType)
        .reduce((left, right) -> new EventCount(left.getEventType(), left.getCount() + right.getCount()))
        .name("reduce-event-count")
        .uid("reduce-event-count");
```

独立 ReduceFunction 示例：

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/function/EventCountReduceFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.function;

import io.github.atengk.flink.model.EventCount;
import org.apache.flink.api.common.functions.ReduceFunction;

/**
 * 事件计数聚合函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class EventCountReduceFunction implements ReduceFunction<EventCount> {

    /**
     * 合并两条事件统计数据
     *
     * @param value1 第一条统计数据
     * @param value2 第二条统计数据
     * @return 合并后的统计数据
     */
    @Override
    public EventCount reduce(EventCount value1, EventCount value2) {
        return new EventCount(value1.getEventType(), value1.getCount() + value2.getCount());
    }
}
```

`reduce` 使用建议：

1. 适合相同类型输入、相同类型输出的增量聚合。
2. 聚合对象字段要语义清晰，避免误把维度字段覆盖。
3. 无窗口 `reduce` 会持续维护滚动状态，状态可能无限增长。
4. 长期运行作业应结合窗口、TTL 或状态清理策略。
5. 如果输入类型和输出类型不同，应使用 `aggregate`、`process` 或窗口函数。

### process 处理

`process` 是 DataStream API 中更底层、更灵活的处理算子，适合复杂逻辑、侧输出流、定时器、状态访问、事件时间处理和异常分流。Side Output 文档说明，`ProcessFunction`、`KeyedProcessFunction`、`CoProcessFunction` 等函数都可以通过 Context 输出侧输出流。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/side_output/?utm_source=chatgpt.com))

示例：识别迟到或异常时间数据，并通过侧输出流输出异常数据。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/function/EventTimeValidateProcessFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.function;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.flink.model.DirtyData;
import io.github.atengk.flink.model.UserBehaviorEvent;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

/**
 * 事件时间校验处理函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class EventTimeValidateProcessFunction extends ProcessFunction<UserBehaviorEvent, UserBehaviorEvent> {

    public static final OutputTag<DirtyData> INVALID_TIME_TAG = new OutputTag<DirtyData>("invalid-event-time") {
    };

    private static final long MAX_FUTURE_TIME_MS = 10 * 60 * 1000L;

    /**
     * 校验事件时间
     *
     * @param value 用户行为事件
     * @param ctx 处理上下文
     * @param out 主流输出
     */
    @Override
    public void processElement(UserBehaviorEvent value, Context ctx, Collector<UserBehaviorEvent> out) {
        long now = System.currentTimeMillis();
        Long eventTime = value.getEventTime();

        if (eventTime == null || eventTime <= 0) {
            outputDirty(ctx, value, "事件时间为空或不合法");
            return;
        }

        if (eventTime > now + MAX_FUTURE_TIME_MS) {
            outputDirty(ctx, value, "事件时间超过允许的未来时间范围");
            return;
        }

        out.collect(value);
    }

    /**
     * 输出异常时间数据
     *
     * @param ctx 处理上下文
     * @param value 用户行为事件
     * @param message 异常原因
     */
    private void outputDirty(Context ctx, UserBehaviorEvent value, String message) {
        DirtyData dirtyData = new DirtyData(
                "event-time-validate",
                String.valueOf(value),
                "INVALID_EVENT_TIME",
                StrUtil.blankToDefault(message, "事件时间异常"),
                System.currentTimeMillis()
        );
        ctx.output(INVALID_TIME_TAG, dirtyData);
    }
}
```

`process` 使用建议：

1. 需要状态、定时器、侧输出流时优先使用。
2. 简单字段转换不要滥用 `process`。
3. `KeyedProcessFunction` 必须在 `keyBy` 后使用。
4. 定时器注册要注意事件时间和处理时间区别。
5. 在 `process` 中访问外部系统需要谨慎，优先使用异步 IO。
6. 异常数据、迟到数据、分流数据适合用侧输出流处理。

### union 合并

`union` 用于合并两个或多个相同类型的 DataStream，输出一个包含所有输入流数据的新 DataStream。Flink 官方算子文档说明，`union` 可以合并两个或多个流；如果一个流和自身 union，结果中每条数据会出现两次。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/zh/docs/dev/datastream/operators/overview/?utm_source=chatgpt.com))

示例：合并 App 行为流和 Web 行为流。

```java
DataStream<UserBehaviorEvent> appEventStream = appSourceStream
        .process(new UserBehaviorParseFunction())
        .name("parse-app-user-behavior")
        .uid("parse-app-user-behavior");

DataStream<UserBehaviorEvent> webEventStream = webSourceStream
        .process(new UserBehaviorParseFunction())
        .name("parse-web-user-behavior")
        .uid("parse-web-user-behavior");

DataStream<UserBehaviorEvent> allEventStream = appEventStream
        .union(webEventStream)
        .name("union-app-web-user-behavior");
```

`union` 使用建议：

1. 只能合并相同类型的流。
2. 合并前应统一字段含义、时间单位和事件类型枚举。
3. 多来源流合并后建议保留 `source` 字段。
4. 不要将同一个流和自身 union，除非明确需要重复数据。
5. 合并后如果需要区分来源，应在合并前补充来源字段。

### connect 双流处理

`connect` 用于连接两个类型可以不同的流，并生成 `ConnectedStreams`。Flink 官方算子文档说明，`connect` 会保留两个流各自的类型，并允许两个流的处理逻辑之间共享状态。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/zh/docs/dev/datastream/operators/overview/?utm_source=chatgpt.com))

典型场景包括：

| 场景                | 说明                         |
| ------------------- | ---------------------------- |
| 主流 + 配置流       | 主流处理数据，配置流更新规则 |
| 订单流 + 支付流     | 双流状态匹配                 |
| 事件流 + 黑名单流   | 动态过滤                     |
| 数据流 + 阈值流     | 动态告警                     |
| 明细流 + 维度变更流 | 维表更新                     |

示例：主流连接规则流。

文件位置：`flink-model/src/main/java/io/github/atengk/flink/model/EventRule.java`

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 事件规则模型
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 是否启用
     */
    private Boolean enabled;
}
```

处理函数示例：

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/function/EventRuleCoProcessFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.function;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.flink.model.EventRule;
import io.github.atengk.flink.model.UserBehaviorEvent;
import org.apache.flink.streaming.api.functions.co.CoProcessFunction;
import org.apache.flink.util.Collector;

import java.util.HashMap;
import java.util.Map;

/**
 * 事件规则双流处理函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class EventRuleCoProcessFunction extends CoProcessFunction<UserBehaviorEvent, EventRule, UserBehaviorEvent> {

    private final Map<String, EventRule> ruleMap = new HashMap<>();

    /**
     * 处理用户行为主流
     *
     * @param value 用户行为事件
     * @param ctx 上下文
     * @param out 输出收集器
     */
    @Override
    public void processElement1(UserBehaviorEvent value, Context ctx, Collector<UserBehaviorEvent> out) {
        EventRule rule = ruleMap.get(value.getEventType());
        if (rule == null || BooleanUtil.isTrue(rule.getEnabled())) {
            out.collect(value);
        }
    }

    /**
     * 处理规则流
     *
     * @param value 事件规则
     * @param ctx 上下文
     * @param out 输出收集器
     */
    @Override
    public void processElement2(EventRule value, Context ctx, Collector<UserBehaviorEvent> out) {
        if (value == null || StrUtil.isBlank(value.getEventType())) {
            return;
        }
        ruleMap.put(value.getEventType(), value);
    }
}
```

使用示例：

```java
DataStream<UserBehaviorEvent> filteredStream = eventStream
        .connect(ruleStream)
        .process(new EventRuleCoProcessFunction())
        .name("connect-event-rule-process")
        .uid("connect-event-rule-process");
```

`connect` 使用建议：

1. 两个流类型可以不同。
2. 如果规则流需要广播到所有并行实例，应优先使用 Broadcast State。
3. 普通 `CoProcessFunction` 中的本地 Map 不具备容错状态能力，生产规则状态应使用 Flink State。
4. 双流处理要明确主流、配置流和状态更新顺序。
5. 规则变更需要记录版本号和更新时间。

### side output 侧输出流

`side output` 用于从主流中额外输出其他类型的数据，适合脏数据、迟到数据、告警数据、审计数据和分流数据。Flink 官方文档说明，侧输出流通过 `OutputTag` 标识，并且 `OutputTag` 需要携带输出类型信息；数据可以从 `ProcessFunction`、`KeyedProcessFunction`、`CoProcessFunction`、`ProcessWindowFunction` 等函数中输出。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/side_output/?utm_source=chatgpt.com))

侧输出流完整使用流程如下：

```text
定义 OutputTag
    |
    v
在 ProcessFunction 中 ctx.output(...)
    |
    v
从主流 getSideOutput(...)
    |
    v
将侧输出流写入 Kafka、文件、数据库或告警系统
```

侧输出流使用示例：

```java
SingleOutputStreamOperator<UserBehaviorEvent> mainStream = sourceStream
        .process(new UserBehaviorParseFunction())
        .name("parse-user-behavior-with-dirty-output")
        .uid("parse-user-behavior-with-dirty-output");

DataStream<DirtyData> dirtyStream = mainStream
        .getSideOutput(UserBehaviorParseFunction.DIRTY_OUTPUT_TAG)
        .name("dirty-data-side-output");

mainStream
        .print("normal-data")
        .name("normal-data-print")
        .uid("normal-data-print");

dirtyStream
        .map(JSONUtil::toJsonStr)
        .name("dirty-data-json-format")
        .uid("dirty-data-json-format")
        .print("dirty-data")
        .name("dirty-data-print")
        .uid("dirty-data-print");
```

侧输出流使用建议：

1. 脏数据、迟到数据、告警数据优先使用侧输出流。
2. 侧输出流类型可以不同于主流类型。
3. `OutputTag` 应定义为静态常量，避免多个位置重复创建。
4. 侧输出流必须被消费，否则排查时容易误以为数据丢失。
5. 脏数据侧输出流应接入持久化 Sink，而不是只 `print()`。
6. 主流和侧输出流要分别设置 Sink、日志和监控指标。

DataStream API 开发整体建议：

1. 简单字段转换使用 `map`。
2. 一进多出或可丢弃输出使用 `flatMap`。
3. 条件保留使用 `filter`。
4. 有状态计算前必须先 `keyBy`。
5. 同类型流合并使用 `union`。
6. 不同类型流联动使用 `connect`。
7. 脏数据和迟到数据使用 `side output`。
8. 复杂状态、定时器和分流逻辑使用 `process`。
9. 关键算子必须设置稳定的 `uid()`。
10. 每个算子的职责应单一，避免把解析、过滤、聚合、写出全部写在一个函数中。


## Table API 与 SQL 开发

本章节用于说明 Flink Table API 与 Flink SQL 的开发方式。Table API 与 SQL 更适合声明式开发、动态表建模、维表 Join、窗口指标、UDF 扩展和批流统一计算；DataStream API 更适合复杂状态、复杂事件处理和精细控制逻辑。实际项目中，两者可以组合使用，`StreamTableEnvironment` 是 Table/SQL 与 DataStream API 集成时的核心入口，它负责 DataStream 与 Table 的转换、外部系统连接、Catalog 元数据管理和 SQL 执行。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/api/java/org/apache/flink/table/api/bridge/java/StreamTableEnvironment.html?utm_source=chatgpt.com))

### TableEnvironment 初始化

`TableEnvironment` 是 Table API 和 SQL 程序的执行上下文。如果项目只使用 SQL 或 Table API，可以使用 `TableEnvironment`；如果需要和 DataStream API 互相转换，推荐使用 `StreamTableEnvironment`。Flink 官方文档说明，`StreamTableEnvironment` 面向 Java DataStream API 集成，并统一支持有界和无界数据处理。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/api/java/org/apache/flink/table/api/bridge/java/StreamTableEnvironment.html?utm_source=chatgpt.com))

Maven 依赖示例：

```xml
<dependencies>
    <!-- Flink Table API Java 依赖，用于 Table API 与 SQL 开发 -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-table-api-java</artifactId>
        <version>${flink.version}</version>
        <scope>provided</scope>
    </dependency>

    <!-- DataStream 与 Table API 桥接依赖，用于 StreamTableEnvironment -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-table-api-java-bridge</artifactId>
        <version>${flink.version}</version>
        <scope>provided</scope>
    </dependency>

    <!-- 本地运行 Table/SQL 作业时常用，提交到集群时需按集群依赖情况处理 -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-table-runtime</artifactId>
        <version>${flink.version}</version>
    </dependency>

    <!-- 本地运行 SQL Planner 时常用，生产打包需结合集群依赖确认 -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-table-planner-loader</artifactId>
        <version>${flink.version}</version>
    </dependency>
</dependencies>
```

下面的代码用于初始化 `StreamExecutionEnvironment` 和 `StreamTableEnvironment`，适合作为 SQL 作业入口基础模板。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/UserBehaviorSqlJob.java`

```java
package io.github.atengk.flink.job.userbehavior;

import cn.hutool.core.convert.Convert;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

/**
 * 用户行为 SQL 作业入口
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserBehaviorSqlJob {

    /**
     * SQL 作业入口方法
     *
     * @param args 启动参数
     * @throws Exception 作业提交异常
     */
    public static void main(String[] args) throws Exception {
        ParameterTool parameterTool = ParameterTool.fromArgs(args);

        String jobName = parameterTool.get("jobName", "user-behavior-sql-job");
        int parallelism = Convert.toInt(parameterTool.get("parallelism"), 2);

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(parallelism);
        env.setRuntimeMode(RuntimeExecutionMode.STREAMING);

        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        log.info("Flink SQL作业开始初始化，作业名称：{}，并行度：{}", jobName, parallelism);

        tableEnv.executeSql("""
                CREATE TEMPORARY TABLE user_behavior_source (
                    user_id STRING,
                    item_id STRING,
                    category_id STRING,
                    event_type STRING,
                    event_time AS PROCTIME()
                ) WITH (
                    'connector' = 'datagen',
                    'rows-per-second' = '10',
                    'fields.user_id.length' = '8',
                    'fields.item_id.length' = '8',
                    'fields.category_id.length' = '6',
                    'fields.event_type.length' = '6'
                )
                """);

        tableEnv.executeSql("""
                CREATE TEMPORARY TABLE user_behavior_sink (
                    event_type STRING,
                    event_count BIGINT
                ) WITH (
                    'connector' = 'print'
                )
                """);

        tableEnv.executeSql("""
                INSERT INTO user_behavior_sink
                SELECT event_type, COUNT(*) AS event_count
                FROM user_behavior_source
                GROUP BY event_type
                """);

        log.info("Flink SQL作业已提交，作业名称：{}", jobName);
    }
}
```

这个入口示例使用 `datagen` 和 `print` Connector，适合本地验证 SQL 语法、表定义、聚合逻辑和作业提交流程。生产环境应替换为 Kafka、JDBC、Doris、StarRocks、Iceberg、Hive 等正式 Connector，并按实际 Flink 版本确认 Connector 兼容性。

### 动态表概念

动态表是 Flink Table API 与 SQL 的核心概念。Flink 文档说明，动态表用于以统一方式处理有界和无界数据；动态表只是逻辑概念，实际数据仍存储在外部系统中，例如数据库、键值存储、消息队列或文件系统。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/table/sourcessinks/?utm_source=chatgpt.com))

可以将动态表理解为“不断变化的数据库表”：

```text
Kafka / CDC / File / DataStream
        |
        v
动态 Source 表
        |
        v
SQL 连续查询
        |
        v
动态结果表
        |
        v
Sink 表 / Changelog Stream / DataStream
```

动态表和传统数据库表的主要区别如下：

| 对比项   | 传统数据库表             | Flink 动态表                                |
| -------- | ------------------------ | ------------------------------------------- |
| 数据变化 | 由数据库事务写入         | 由流式 Source 持续驱动                      |
| 查询方式 | 查询当前快照             | 连续查询，结果持续更新                      |
| 结果类型 | 固定结果集               | Append、Update、Delete Changelog            |
| 时间语义 | 通常依赖字段或数据库时间 | 支持 Processing Time、Event Time、Watermark |
| 典型输入 | MySQL 表                 | Kafka Topic、CDC、文件、DataStream          |
| 典型输出 | SQL 查询结果             | Kafka、数据库、OLAP、数据湖、DataStream     |

在项目中，动态表适合表达以下场景：

1. Kafka Topic 映射为 Source 表。
2. MySQL CDC 表映射为动态变更表。
3. 聚合 SQL 生成持续更新的指标结果表。
4. 维表 Join 查询外部维度数据。
5. SQL 窗口计算输出分钟级、小时级指标。
6. DataStream 转换为临时视图后继续使用 SQL 处理。

### Source 表定义

Source 表定义用于将外部系统注册为 SQL 可查询的动态表。Flink `CREATE TABLE` 可以定义物理列、元数据列、计算列、Watermark、主键约束和 Connector 参数；`WITH` 选项用于声明连接到外部系统所需的 Connector 与格式参数。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/table/sql/create/?utm_source=chatgpt.com))

本地调试 Source 表可以使用 `datagen`：

```sql
CREATE TEMPORARY TABLE user_behavior_source (
    user_id STRING,
    item_id STRING,
    category_id STRING,
    event_type STRING,
    event_time AS PROCTIME()
) WITH (
    'connector' = 'datagen',
    'rows-per-second' = '10',
    'fields.user_id.length' = '8',
    'fields.item_id.length' = '8',
    'fields.category_id.length' = '6',
    'fields.event_type.length' = '6'
);
```

Kafka JSON Source 表模板如下，适合生产实时日志和埋点数据接入。具体 Connector 参数要按当前 Flink 版本和 Kafka Connector 文档确认。

```sql
CREATE TEMPORARY TABLE user_behavior_kafka_source (
    user_id STRING,
    item_id STRING,
    category_id STRING,
    event_type STRING,
    event_ts BIGINT,
    event_time AS TO_TIMESTAMP_LTZ(event_ts, 3),
    WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND
) WITH (
    'connector' = 'kafka',
    'topic' = 'user_behavior_log',
    'properties.bootstrap.servers' = 'kafka-prod-01:9092,kafka-prod-02:9092,kafka-prod-03:9092',
    'properties.group.id' = 'user-behavior-sql-job',
    'scan.startup.mode' = 'latest-offset',
    'format' = 'json',
    'json.ignore-parse-errors' = 'true',
    'json.fail-on-missing-field' = 'false'
);
```

Source 表定义建议：

1. 字段类型要与原始数据 Schema 对齐。
2. JSON 字段缺失和解析失败策略要明确。
3. 事件时间字段应转换为 `TIMESTAMP(3)` 或 `TIMESTAMP_LTZ(3)`。
4. Event Time 场景必须声明 `WATERMARK`。
5. Kafka Source 应明确 `topic`、`group.id` 和 `scan.startup.mode`。
6. CDC Source 应明确主键、库表、server-id 和时区。
7. 生产表定义应纳入版本管理，不建议散落在 Java 字符串中。

### Sink 表定义

Sink 表定义用于声明 SQL 结果写入的目标系统。Flink Table Connector 文档说明，表 Sink 会将动态表结果输出到外部存储系统，不同 Source 和 Sink 支持不同格式，例如 CSV、Avro、Parquet、ORC 等。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/connectors/table/overview/?utm_source=chatgpt.com))

本地调试 Sink 表可以使用 `print`：

```sql
CREATE TEMPORARY TABLE user_behavior_print_sink (
    event_type STRING,
    event_count BIGINT
) WITH (
    'connector' = 'print'
);
```

Kafka Sink 表模板如下：

```sql
CREATE TEMPORARY TABLE user_behavior_kafka_sink (
    user_id STRING,
    item_id STRING,
    category_id STRING,
    event_type STRING,
    event_time TIMESTAMP_LTZ(3)
) WITH (
    'connector' = 'kafka',
    'topic' = 'user_behavior_clean',
    'properties.bootstrap.servers' = 'kafka-prod-01:9092,kafka-prod-02:9092,kafka-prod-03:9092',
    'format' = 'json',
    'json.timestamp-format.standard' = 'ISO-8601'
);
```

Upsert Sink 通常需要主键，用于表达更新语义。例如写入支持主键更新的系统时，应显式声明主键：

```sql
CREATE TEMPORARY TABLE user_behavior_metric_sink (
    event_type STRING,
    window_start TIMESTAMP(3),
    window_end TIMESTAMP(3),
    event_count BIGINT,
    PRIMARY KEY (event_type, window_start, window_end) NOT ENFORCED
) WITH (
    'connector' = 'upsert-kafka',
    'topic' = 'user_behavior_metric',
    'properties.bootstrap.servers' = 'kafka-prod-01:9092,kafka-prod-02:9092,kafka-prod-03:9092',
    'key.format' = 'json',
    'value.format' = 'json'
);
```

Sink 表定义建议：

1. Append-only 结果可以写普通 Kafka、文件或明细表。
2. 聚合结果通常是 Update/Upsert 语义，需要支持主键或 Changelog 的 Sink。
3. 主键约束在 Flink SQL 中通常声明为 `PRIMARY KEY (...) NOT ENFORCED`。
4. Sink 表字段顺序和类型必须与 `INSERT INTO SELECT` 结果一致。
5. 生产 Sink 必须明确幂等、事务、重试和失败处理策略。
6. 本地调试优先使用 `print`，不要误写生产 Sink。

### SQL 查询开发

SQL 查询开发适合表达过滤、投影、聚合、窗口、Join、TopN、去重和维表关联等逻辑。Flink SQL 的 `CREATE` 语句可以通过 `TableEnvironment.executeSql()` 执行；官方文档说明，`executeSql()` 成功执行 `CREATE` 操作时返回 OK，否则抛出异常。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/table/sql/create/?utm_source=chatgpt.com))

基础过滤与投影示例：

```sql
INSERT INTO user_behavior_kafka_sink
SELECT
    user_id,
    item_id,
    category_id,
    event_type,
    event_time
FROM user_behavior_kafka_source
WHERE user_id IS NOT NULL
  AND event_type IS NOT NULL;
```

按事件类型聚合示例：

```sql
INSERT INTO user_behavior_print_sink
SELECT
    event_type,
    COUNT(*) AS event_count
FROM user_behavior_source
GROUP BY event_type;
```

滚动窗口聚合示例：

```sql
INSERT INTO user_behavior_metric_sink
SELECT
    event_type,
    window_start,
    window_end,
    COUNT(*) AS event_count
FROM TABLE(
    TUMBLE(TABLE user_behavior_kafka_source, DESCRIPTOR(event_time), INTERVAL '1' MINUTE)
)
GROUP BY event_type, window_start, window_end;
```

Java 中执行 SQL 建议将 DDL 和 DML 拆分管理：

```java
tableEnv.executeSql(sourceDdl);
tableEnv.executeSql(sinkDdl);
tableEnv.executeSql(insertSql);
```

SQL 查询开发原则：

1. DDL、DML 和 UDF 注册逻辑应分层管理。
2. SQL 字符串建议放到资源文件、配置中心或 SQL 管理模块中。
3. 复杂 SQL 需要提供字段血缘、输入表、输出表和时间语义说明。
4. 聚合 SQL 要明确输出是 Append、Update 还是 Upsert。
5. 窗口 SQL 必须确认时间字段和 Watermark 是否正确。
6. 生产 SQL 变更应经过样例数据验证和回归测试。

### UDF 自定义函数

UDF 通常指 Scalar Function，用于将一行中的一个或多个标量值转换为一个新的标量值。Flink 官方文档说明，用户自定义函数可用于查询中调用常用逻辑或 SQL 无法直接表达的自定义逻辑，JVM 语言实现的函数需要继承对应基类，例如 `ScalarFunction`。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/table/functions/udfs/?utm_source=chatgpt.com))

下面的函数用于标准化事件类型。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/udf/EventTypeNormalizeFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.udf;

import cn.hutool.core.util.StrUtil;
import org.apache.flink.table.functions.ScalarFunction;

/**
 * 事件类型标准化函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class EventTypeNormalizeFunction extends ScalarFunction {

    /**
     * 标准化事件类型
     *
     * @param eventType 原始事件类型
     * @return 标准化后的事件类型
     */
    public String eval(String eventType) {
        if (StrUtil.isBlank(eventType)) {
            return "unknown";
        }
        return StrUtil.trim(eventType).toLowerCase();
    }
}
```

注册和使用示例：

```java
tableEnv.createTemporarySystemFunction("normalize_event_type", EventTypeNormalizeFunction.class);

tableEnv.executeSql("""
        INSERT INTO user_behavior_print_sink
        SELECT normalize_event_type(event_type), COUNT(*) AS event_count
        FROM user_behavior_source
        GROUP BY normalize_event_type(event_type)
        """);
```

UDF 开发建议：

1. 函数类必须是 `public`，不应使用非静态内部类或匿名类。
2. 核心逻辑写在 `eval(...)` 方法中。
3. UDF 应保持无副作用，不建议访问外部系统。
4. 对空值、异常值要有明确处理策略。
5. 高频调用 UDF 要避免复杂对象创建和外部 IO。
6. 可复用 UDF 应放到公共 SQL 函数模块。

### UDAF 自定义聚合函数

UDAF 用于将多行标量值聚合为一个标量值。Flink 官方文档说明，Aggregate Function 的行为围绕 accumulator 展开，运行时先调用 `createAccumulator()` 创建累加器，再通过 `accumulate(...)` 更新累加器，最终通过 `getValue(...)` 计算结果。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/table/functions/udfs/?utm_source=chatgpt.com))

下面的聚合函数用于统计有效事件数量。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/udaf/ValidEventCountFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.udaf;

import cn.hutool.core.util.StrUtil;
import org.apache.flink.table.functions.AggregateFunction;

import java.io.Serializable;

/**
 * 有效事件计数聚合函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class ValidEventCountFunction extends AggregateFunction<Long, ValidEventCountFunction.CountAccumulator> {

    /**
     * 创建累加器
     *
     * @return 计数累加器
     */
    @Override
    public CountAccumulator createAccumulator() {
        return new CountAccumulator();
    }

    /**
     * 获取聚合结果
     *
     * @param accumulator 计数累加器
     * @return 有效事件数量
     */
    @Override
    public Long getValue(CountAccumulator accumulator) {
        return accumulator.count;
    }

    /**
     * 累加有效事件
     *
     * @param accumulator 计数累加器
     * @param userId 用户ID
     * @param eventType 事件类型
     */
    public void accumulate(CountAccumulator accumulator, String userId, String eventType) {
        if (StrUtil.isNotBlank(userId) && StrUtil.isNotBlank(eventType)) {
            accumulator.count++;
        }
    }

    /**
     * 撤回有效事件，适用于需要 retract 的聚合场景
     *
     * @param accumulator 计数累加器
     * @param userId 用户ID
     * @param eventType 事件类型
     */
    public void retract(CountAccumulator accumulator, String userId, String eventType) {
        if (StrUtil.isNotBlank(userId) && StrUtil.isNotBlank(eventType)) {
            accumulator.count--;
        }
    }

    /**
     * 计数累加器
     *
     * @author Ateng
     * @since 2026-05-11
     */
    public static class CountAccumulator implements Serializable {

        private static final long serialVersionUID = 1L;

        public long count = 0L;
    }
}
```

注册和使用示例：

```java
tableEnv.createTemporarySystemFunction("valid_event_count", ValidEventCountFunction.class);

tableEnv.executeSql("""
        INSERT INTO user_behavior_print_sink
        SELECT event_type, valid_event_count(user_id, event_type) AS event_count
        FROM user_behavior_source
        GROUP BY event_type
        """);
```

UDAF 开发建议：

1. 累加器应是简单、可序列化、可恢复的结构。
2. `accumulate(...)` 方法用于更新累加器。
3. 对于 Update/Retraction 场景，建议实现 `retract(...)`。
4. 聚合状态不能无限膨胀。
5. 聚合逻辑应可用样例数据单元测试验证。
6. UDAF 适合 SQL 中反复使用的业务聚合口径。

### UDTF 自定义表函数

UDTF 用于一行输入产生零行、一行或多行输出。Flink 官方文档说明，Table Function 会将标量值映射为新的行，输出记录可以包含一个或多个字段，函数通过 `collect(T)` 输出零条或多条记录。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/table/functions/udfs/?utm_source=chatgpt.com))

下面的表函数用于将字符串按分隔符拆分为多行。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/udtf/SplitWordsFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.udtf;

import cn.hutool.core.util.StrUtil;
import org.apache.flink.table.annotation.DataTypeHint;
import org.apache.flink.table.annotation.FunctionHint;
import org.apache.flink.table.functions.TableFunction;
import org.apache.flink.types.Row;

/**
 * 字符串拆分表函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
@FunctionHint(output = @DataTypeHint("ROW<word STRING>"))
public class SplitWordsFunction extends TableFunction<Row> {

    /**
     * 按分隔符拆分字符串
     *
     * @param value 原始字符串
     * @param delimiter 分隔符
     */
    public void eval(String value, String delimiter) {
        if (StrUtil.isBlank(value)) {
            return;
        }

        String actualDelimiter = StrUtil.blankToDefault(delimiter, ",");
        for (String word : StrUtil.split(value, actualDelimiter)) {
            if (StrUtil.isNotBlank(word)) {
                collect(Row.of(StrUtil.trim(word)));
            }
        }
    }
}
```

注册和使用示例：

```java
tableEnv.createTemporarySystemFunction("split_words", SplitWordsFunction.class);

tableEnv.executeSql("""
        SELECT user_id, word
        FROM user_behavior_source,
        LATERAL TABLE(split_words(event_type, ',')) AS T(word)
        """).print();
```

UDTF 开发建议：

1. 一进多出场景优先考虑 UDTF。
2. 输出 Row 结构应通过注解或类型提示明确。
3. 空输入不要抛异常，直接不输出即可。
4. UDTF 不适合访问慢速外部系统。
5. 高频 UDTF 要控制输出膨胀倍数。
6. 需要复杂状态和定时器时，应评估 Process Table Function 或回到 DataStream API。

### DataStream 与 Table 转换

DataStream 与 Table 转换用于在两套 API 之间复用能力。Flink 官方文档说明，`StreamTableEnvironment` 提供 `fromDataStream`、`createTemporaryView`、`toDataStream` 等方法；默认情况下，从 DataStream 转 Table 不会自动传播事件时间和 Watermark，除非通过 `Schema` 显式声明。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/table/data_stream_api/?utm_source=chatgpt.com))

下面的代码用于将 DataStream 转为临时视图，并在 SQL 中使用事件时间字段。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/DataStreamTableConvertJob.java`

```java
package io.github.atengk.flink.job.userbehavior;

import io.github.atengk.flink.model.UserBehaviorEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Schema;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

/**
 * DataStream 与 Table 转换示例作业
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class DataStreamTableConvertJob {

    /**
     * 作业入口
     *
     * @param args 启动参数
     * @throws Exception 作业执行异常
     */
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        DataStream<UserBehaviorEvent> eventStream = env.fromElements(
                new UserBehaviorEvent("1001", "sku_1", "cat_1", "click", 1778464800000L),
                new UserBehaviorEvent("1002", "sku_2", "cat_2", "pay", 1778464805000L)
        );

        tableEnv.createTemporaryView(
                "user_behavior_view",
                eventStream,
                Schema.newBuilder()
                        .column("userId", "STRING")
                        .column("itemId", "STRING")
                        .column("categoryId", "STRING")
                        .column("eventType", "STRING")
                        .column("eventTime", "BIGINT")
                        .columnByExpression("rowtime", "TO_TIMESTAMP_LTZ(eventTime, 3)")
                        .watermark("rowtime", "rowtime - INTERVAL '5' SECOND")
                        .build()
        );

        tableEnv.executeSql("""
                SELECT eventType, COUNT(*) AS event_count
                FROM user_behavior_view
                GROUP BY eventType
                """).print();

        log.info("DataStream转Table示例执行完成");
    }
}
```

转换建议：

1. DataStream 转 Table 时显式声明字段名、类型、时间字段和 Watermark。
2. Table 转 DataStream 时要区分 Append-only、Update、Retract 和 Changelog。
3. 普通 `toDataStream(Table)` 适合只追加结果。
4. 聚合更新结果通常需要使用 Changelog 语义，不应直接当作普通追加流处理。
5. 复杂状态逻辑先用 DataStream 处理，再注册为临时视图供 SQL 查询。
6. 标准指标计算可优先使用 SQL，异常分流和复杂规则可使用 DataStream。

## 时间语义

本章节用于说明 Flink 中 Processing Time、Event Time、Ingestion Time、时间字段提取、Watermark 生成、延迟策略、乱序数据和空闲分区处理。时间语义直接影响窗口结果、Join 结果、迟到数据处理和作业可重放性。

Flink Table API 时间属性文档说明，Processing Time 是执行相应操作的机器系统时间，Event Time 是记录自身携带的时间戳；Event Time 需要 Flink 知道每行时间戳，并通过 Watermark 表示事件时间推进程度。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/concepts/sql-table-concepts/time_attributes/?utm_source=chatgpt.com))

### Processing Time

Processing Time 使用算子所在机器的系统时间作为时间依据。它实现简单，不需要从数据中提取时间戳，也不需要生成 Watermark，但结果受机器时间、数据到达速度、反压和网络传输影响，通常不具备严格可重放性。Flink Table 文档也说明，Processing Time 最简单，但会产生非确定性结果，且不需要时间戳提取或 Watermark。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/concepts/sql-table-concepts/time_attributes/?utm_source=chatgpt.com))

SQL 中可以通过 `PROCTIME()` 定义处理时间字段：

```sql
CREATE TEMPORARY TABLE user_behavior_source (
    user_id STRING,
    event_type STRING,
    proc_time AS PROCTIME()
) WITH (
    'connector' = 'datagen',
    'rows-per-second' = '10',
    'fields.user_id.length' = '8',
    'fields.event_type.length' = '6'
);
```

Processing Time 窗口统计示例：

```sql
SELECT
    window_start,
    window_end,
    event_type,
    COUNT(*) AS event_count
FROM TABLE(
    TUMBLE(TABLE user_behavior_source, DESCRIPTOR(proc_time), INTERVAL '1' MINUTE)
)
GROUP BY window_start, window_end, event_type;
```

Processing Time 适用场景：

| 场景           | 说明                                  |
| -------------- | ------------------------------------- |
| 本地调试       | 不关心严格事件时间，只验证 SQL 和流程 |
| 近实时粗略统计 | 对乱序和可重放一致性要求不高          |
| 低延迟告警     | 以系统处理时间触发，允许一定误差      |
| 无事件时间字段 | Source 数据没有可靠业务时间           |

不适合使用 Processing Time 的场景包括订单金额结算、严格事件时间窗口、历史回放补数、需要重复计算得到一致结果的指标。

### Event Time

Event Time 使用事件自身携带的业务发生时间作为时间依据。它适合乱序数据、延迟到达数据、Kafka 回放、历史补数和需要可重放一致性的指标计算。Flink 文档说明，Event Time 基于每条记录附带的时间戳处理数据，可以在乱序或迟到事件存在时仍产生一致结果，并支持从持久化存储回放时获得可重复结果。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/concepts/sql-table-concepts/time_attributes/?utm_source=chatgpt.com))

Event Time Source 表定义示例：

```sql
CREATE TEMPORARY TABLE user_behavior_kafka_source (
    user_id STRING,
    item_id STRING,
    event_type STRING,
    event_ts BIGINT,
    event_time AS TO_TIMESTAMP_LTZ(event_ts, 3),
    WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND
) WITH (
    'connector' = 'kafka',
    'topic' = 'user_behavior_log',
    'properties.bootstrap.servers' = 'kafka-prod-01:9092,kafka-prod-02:9092,kafka-prod-03:9092',
    'properties.group.id' = 'user-behavior-event-time-job',
    'scan.startup.mode' = 'latest-offset',
    'format' = 'json'
);
```

Event Time 窗口统计示例：

```sql
SELECT
    window_start,
    window_end,
    event_type,
    COUNT(*) AS event_count
FROM TABLE(
    TUMBLE(TABLE user_behavior_kafka_source, DESCRIPTOR(event_time), INTERVAL '1' MINUTE)
)
GROUP BY window_start, window_end, event_type;
```

Event Time 使用建议：

1. 优先使用业务事件发生时间，例如订单创建时间、支付时间、行为发生时间。
2. Event Time 字段应统一时区和单位。
3. 必须声明 Watermark，否则事件时间窗口无法正确推进。
4. 乱序延迟要通过 Watermark 延迟策略控制。
5. 历史回放和生产实时链路应尽量使用同一套时间字段。
6. 不要使用数据进入 Flink 的时间冒充业务发生时间。

### Ingestion Time

Ingestion Time 表示事件进入 Flink Source 时的时间，语义介于 Processing Time 和 Event Time 之间。旧版本 Flink 文档曾将 Ingestion Time 作为独立时间特征描述：它在 Source 处给记录分配当前时间，窗口使用该时间；相比 Processing Time 更稳定，但不能处理乱序和迟到数据。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.10/dev/event_time.html?utm_source=chatgpt.com))

在现代 Flink 版本中，不建议再依赖旧的 `TimeCharacteristic.IngestionTime` 写法。Flink API 文档显示 `TimeCharacteristic` 已被标记为 Deprecated，并提示如果使用 Ingestion Time，应手动设置合适的 `WatermarkStrategy`。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.18/api/java/org/apache/flink/streaming/api/TimeCharacteristic.html?utm_source=chatgpt.com))

在新项目中，可以用以下方式替代 Ingestion Time：

```java
WatermarkStrategy<UserBehaviorEvent> ingestionLikeWatermark = WatermarkStrategy
        .<UserBehaviorEvent>forMonotonousTimestamps()
        .withTimestampAssigner((event, timestamp) -> System.currentTimeMillis());
```

更推荐的方式是在 Source 或解析阶段增加 `receiveTime` 字段，然后明确把它当作接入时间使用：

```java
RawEvent rawEvent = new RawEvent(
        "kafka",
        payload,
        System.currentTimeMillis()
);
```

Ingestion Time 使用建议：

1. 新项目不建议使用旧的 `setStreamTimeCharacteristic(TimeCharacteristic.IngestionTime)`。
2. 如果确实需要接入时间，应显式维护 `receiveTime` 字段。
3. 接入时间不能替代业务事件时间。
4. 对乱序、迟到、历史回放一致性有要求时，应使用 Event Time。
5. 接入时间适合监控采集延迟、链路延迟和数据到达时间。

### 时间字段提取

时间字段提取用于从数据中获取事件时间戳。Flink Watermark 文档说明，使用 Event Time 时，Flink 需要知道事件时间戳，通常通过 `TimestampAssigner` 从元素字段中提取；`WatermarkStrategy` 同时包含时间戳提取器和 Watermark 生成器。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/event-time/generating_watermarks/?utm_source=chatgpt.com))

DataStream 中的时间字段提取示例：

```java
WatermarkStrategy<UserBehaviorEvent> watermarkStrategy = WatermarkStrategy
        .<UserBehaviorEvent>forBoundedOutOfOrderness(Duration.ofSeconds(5))
        .withTimestampAssigner((event, recordTimestamp) -> event.getEventTime());

DataStream<UserBehaviorEvent> eventTimeStream = eventStream
        .assignTimestampsAndWatermarks(watermarkStrategy)
        .name("assign-event-time-watermark")
        .uid("assign-event-time-watermark");
```

如果 Source 支持在 Source 内部设置 Watermark，优先在 Source 上直接声明：

```java
env.fromSource(kafkaSource, watermarkStrategy, "kafka-user-behavior-source")
        .name("kafka-user-behavior-source")
        .uid("kafka-user-behavior-source")
        .setParallelism(4);
```

Flink 官方 Watermark 文档也建议优先在 Source 上直接指定 `WatermarkStrategy`，因为 Source 可以利用分区、分片或 Split 信息生成更准确的 Watermark；只有无法在 Source 设置时，才在非 Source 算子后设置。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/dev/datastream/event-time/generating_watermarks/?utm_source=chatgpt.com))

时间字段提取建议：

1. 时间戳单位必须统一，推荐毫秒。
2. 字段为空、为 0、过早、过晚时应进入脏数据链路。
3. Kafka 消息时间和业务事件时间要明确区分。
4. SQL 中可以使用计算列将 BIGINT 转换为 `TIMESTAMP_LTZ`。
5. Java 中应通过 `WatermarkStrategy.withTimestampAssigner(...)` 明确提取逻辑。
6. 事件时间字段变更属于高风险 Schema 变更。

### Watermark 生成

Watermark 用于表示事件时间推进程度。Flink Watermark 文档说明，时间戳分配和 Watermark 生成是配套的；`WatermarkStrategy` 包含 `TimestampAssigner` 和 `WatermarkGenerator`，Flink 提供了常见策略，也允许用户自定义。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/event-time/generating_watermarks/?utm_source=chatgpt.com))

常见 Watermark 策略如下：

| 策略         | 代码                                 | 适用场景         |
| ------------ | ------------------------------------ | ---------------- |
| 无 Watermark | `WatermarkStrategy.noWatermarks()`   | 不使用事件时间   |
| 严格递增     | `forMonotonousTimestamps()`          | 事件时间严格递增 |
| 有界乱序     | `forBoundedOutOfOrderness(Duration)` | 常见乱序流       |
| 自定义       | 自定义 `WatermarkGenerator`          | 特殊时间推进规则 |

有界乱序 Watermark 示例：

```java
WatermarkStrategy<UserBehaviorEvent> watermarkStrategy = WatermarkStrategy
        .<UserBehaviorEvent>forBoundedOutOfOrderness(Duration.ofSeconds(10))
        .withTimestampAssigner((event, timestamp) -> event.getEventTime());
```

SQL 中使用 Watermark 示例：

```sql
CREATE TEMPORARY TABLE user_behavior_source (
    user_id STRING,
    event_type STRING,
    event_ts BIGINT,
    event_time AS TO_TIMESTAMP_LTZ(event_ts, 3),
    WATERMARK FOR event_time AS event_time - INTERVAL '10' SECOND
) WITH (
    'connector' = 'kafka',
    'topic' = 'user_behavior_log',
    'properties.bootstrap.servers' = 'kafka-prod-01:9092',
    'properties.group.id' = 'user-behavior-job',
    'format' = 'json'
);
```

Flink `CREATE TABLE` 文档说明，`WATERMARK FOR rowtime_column AS watermark_strategy_expression` 用于声明事件时间属性，Watermark 表达式返回类型必须为 `TIMESTAMP(3)`，常见有严格递增、升序和有界乱序策略。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/table/sql/create/?utm_source=chatgpt.com))

Watermark 生成建议：

1. 实时业务通常使用有界乱序策略。
2. 延迟时间不能随意设置，必须基于数据真实乱序情况。
3. Watermark 延迟越大，窗口结果越晚输出。
4. Watermark 延迟越小，迟到数据越多。
5. Kafka 多分区数据应优先在 Source 内部生成 Watermark。
6. 生产环境应监控 Watermark 延迟、窗口触发时间和迟到数据量。

### Watermark 延迟策略

Watermark 延迟策略用于平衡结果及时性和乱序容忍度。对于 `event_time - INTERVAL '5' SECOND`，表示当前 Watermark 大约滞后于已观察到的最大事件时间 5 秒。Flink 文档中的有界乱序策略示例也使用 `WATERMARK FOR order_time AS order_time - INTERVAL '5' SECOND` 表达 5 秒延迟。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/table/sql/create/?utm_source=chatgpt.com))

延迟策略选择参考：

| 数据类型       | 建议延迟        | 说明                     |
| -------------- | --------------- | ------------------------ |
| 本地测试数据   | 0 到 1 秒       | 便于快速触发窗口         |
| Kafka 业务日志 | 5 到 30 秒      | 常见乱序范围             |
| 移动端埋点     | 30 秒到数分钟   | 客户端离线、网络波动明显 |
| IoT 设备数据   | 数分钟或更长    | 设备缓存、弱网、批量上报 |
| 金融交易事件   | 按业务 SLA 评估 | 需要兼顾准确性和延迟     |
| 历史回放       | 可设置较小延迟  | 数据通常可按时间排序回放 |

DataStream 延迟策略示例：

```java
WatermarkStrategy<UserBehaviorEvent> strategy = WatermarkStrategy
        .<UserBehaviorEvent>forBoundedOutOfOrderness(Duration.ofSeconds(30))
        .withTimestampAssigner((event, timestamp) -> event.getEventTime());
```

SQL 延迟策略示例：

```sql
WATERMARK FOR event_time AS event_time - INTERVAL '30' SECOND
```

Watermark 延迟策略原则：

1. 根据真实数据分布设置，不要凭经验随意设置。
2. 指标类作业优先保证准确性，再优化延迟。
3. 告警类作业优先保证时效性，但要明确误差范围。
4. 延迟时间变更会影响窗口触发和迟到数据比例，需要测试验证。
5. 延迟数据超过 Watermark 容忍范围后，应进入迟到数据处理或修复链路。

### 乱序数据处理

乱序数据是实时系统中的常态，尤其是 Kafka 多分区、移动端埋点、CDC 合并流、设备批量上报和跨系统链路。Event Time 配合 Watermark 可以让 Flink 在一定程度上等待乱序数据，从而保证窗口计算结果更接近业务真实发生时间。

乱序数据处理流程如下：

```text
事件到达 Flink
    |
    v
提取事件时间 eventTime
    |
    v
根据最大事件时间和延迟策略生成 Watermark
    |
    v
窗口等待 Watermark 推进
    |
    v
Watermark 超过窗口结束时间后触发计算
    |
    v
超过允许范围的数据进入迟到处理或修复链路
```

乱序数据处理建议：

| 问题                 | 处理方式                                      |
| -------------------- | --------------------------------------------- |
| 小范围乱序           | 设置合理 Watermark 延迟                       |
| 明显迟到数据         | 使用侧输出流记录迟到数据                      |
| 大量迟到数据         | 排查 Source、网络、客户端缓存、Kafka 积压     |
| 多 Source 乱序差异大 | 分别设置 Watermark 或使用 Watermark Alignment |
| 历史回放乱序         | 尽量按时间分区或按 Offset 有序回放            |
| 结果必须最终准确     | 建立补数、修复和校验机制                      |

在 DataStream API 中，窗口迟到数据处理通常会结合 `allowedLateness` 和侧输出流；在 SQL 场景中，则需要通过 Watermark 策略、Changelog Sink、补偿任务和离线校验共同保障结果质量。

### 空闲分区处理

空闲分区是 Kafka 分区、文件 Split、Source Split 或某个并行 Source 长时间没有新数据导致 Watermark 不推进的问题。Flink Watermark 文档说明，如果某个输入分区一段时间没有事件，WatermarkGenerator 无法产生新的 Watermark；下游算子的 Watermark 通常取所有上游 Watermark 的最小值，因此空闲输入会拖住整体事件时间推进。Flink 提供 `withIdleness(...)` 用于标记空闲输入。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/dev/datastream/event-time/generating_watermarks/?utm_source=chatgpt.com))

DataStream 空闲分区处理示例：

```java
WatermarkStrategy<UserBehaviorEvent> watermarkStrategy = WatermarkStrategy
        .<UserBehaviorEvent>forBoundedOutOfOrderness(Duration.ofSeconds(10))
        .withTimestampAssigner((event, timestamp) -> event.getEventTime())
        .withIdleness(Duration.ofMinutes(1));
```

SQL Source 表空闲分区配置示例：

```sql
CREATE TEMPORARY TABLE user_behavior_source (
    user_id STRING,
    event_type STRING,
    event_ts BIGINT,
    event_time AS TO_TIMESTAMP_LTZ(event_ts, 3),
    WATERMARK FOR event_time AS event_time - INTERVAL '10' SECOND
) WITH (
    'connector' = 'kafka',
    'topic' = 'user_behavior_log',
    'properties.bootstrap.servers' = 'kafka-prod-01:9092',
    'properties.group.id' = 'user-behavior-job',
    'format' = 'json',
    'scan.watermark.idle-timeout' = '1min'
);
```

Flink Table 时间属性文档说明，SQL 可以通过全局参数 `table.exec.source.idle-timeout` 设置 Source 空闲超时，也可以在 Source 表中使用 `scan.watermark.idle-timeout`；当两者同时配置时，Source 表级别参数优先。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/concepts/sql-table-concepts/time_attributes/?utm_source=chatgpt.com))

空闲分区处理建议：

1. Kafka Topic 分区数据不均衡时必须关注 Watermark 是否停滞。
2. 低频 Topic 或规则流应配置空闲超时。
3. 空闲超时不能过短，否则可能误判慢分区为空闲。
4. 多 Source Join 时尤其需要关注 Watermark 推进。
5. 空闲分区问题可以通过 Web UI、Metrics 和窗口迟迟不触发来定位。
6. 生产环境应监控 Watermark Lag、Source Idle 状态和窗口输出延迟。


## 窗口计算

本章节用于说明 Flink DataStream API 中窗口计算的开发方式。窗口用于将无界流切分为有限数据集合，再对每个窗口中的数据进行聚合、统计、关联或复杂处理。Flink 官方窗口文档说明，窗口通常由 Window Assigner、Trigger 和 Window Function 共同组成；窗口函数可以是 `ReduceFunction`、`AggregateFunction` 或 `ProcessWindowFunction`。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/operators/windows/?utm_source=chatgpt.com))

### 滚动窗口

滚动窗口是一种固定长度、窗口之间不重叠的窗口类型。每条数据只会归属于一个滚动窗口，适合按固定周期统计指标，例如每 1 分钟 UV/PV、每 5 分钟订单数、每小时支付金额等。

滚动窗口示意：

```text
窗口大小：1 分钟

[10:00:00, 10:01:00)
[10:01:00, 10:02:00)
[10:02:00, 10:03:00)
```

适用场景如下：

| 场景         | 示例                       |
| ------------ | -------------------------- |
| 固定周期指标 | 每分钟点击数、每小时订单数 |
| 监控统计     | 每 5 分钟错误日志数量      |
| 周期汇总     | 每日活跃用户数、每小时 GMV |
| 数据分桶     | 按固定时间段生成统计结果   |

下面的代码按事件类型统计每 1 分钟窗口内的事件数量。

```java
DataStream<EventCount> resultStream = eventStream
        .keyBy(UserBehaviorEvent::getEventType)
        .window(TumblingEventTimeWindows.of(Time.minutes(1)))
        .aggregate(new EventCountAggregateFunction(), new EventCountWindowFunction())
        .name("tumbling-event-count-window")
        .uid("tumbling-event-count-window");
```

滚动窗口使用建议：

1. 指标口径为“每 N 秒 / 每 N 分钟 / 每 N 小时”时优先使用。
2. Event Time 窗口必须先设置 Watermark。
3. 窗口大小要结合业务 SLA、数据延迟和输出频率设计。
4. 窗口结果写入 Sink 时建议携带 `windowStart` 和 `windowEnd`。
5. 聚合类窗口优先使用增量聚合，避免窗口内缓存大量原始数据。

### 滑动窗口

滑动窗口是一种固定长度、按固定滑动步长向前移动的窗口。窗口之间可以重叠，因此同一条数据可能进入多个窗口。它适合计算最近 N 分钟内的滚动指标，例如最近 5 分钟每 1 分钟输出一次访问量。

滑动窗口示意：

```text
窗口大小：5 分钟
滑动步长：1 分钟

[10:00:00, 10:05:00)
[10:01:00, 10:06:00)
[10:02:00, 10:07:00)
```

适用场景如下：

| 场景     | 示例                                 |
| -------- | ------------------------------------ |
| 滚动监控 | 最近 5 分钟错误数，每 1 分钟刷新一次 |
| 风控统计 | 最近 10 分钟同一用户下单次数         |
| 趋势计算 | 最近 30 分钟访问量滑动趋势           |
| 告警判断 | 最近 5 分钟失败率超过阈值            |

下面的代码按事件类型统计最近 5 分钟数据，并每 1 分钟输出一次。

```java
DataStream<EventCount> resultStream = eventStream
        .keyBy(UserBehaviorEvent::getEventType)
        .window(SlidingEventTimeWindows.of(Time.minutes(5), Time.minutes(1)))
        .aggregate(new EventCountAggregateFunction(), new EventCountWindowFunction())
        .name("sliding-event-count-window")
        .uid("sliding-event-count-window");
```

滑动窗口使用建议：

1. 滑动窗口会让一条数据进入多个窗口，计算成本高于滚动窗口。
2. 窗口大小越大、滑动步长越小，状态压力越大。
3. 适合趋势、告警、风控等需要“最近一段时间”视角的业务。
4. 如果只需要固定周期汇总，不应使用滑动窗口。
5. 生产环境需要关注窗口状态大小和 Checkpoint 耗时。

### 会话窗口

会话窗口根据数据之间的时间间隔动态划分窗口。如果同一个 Key 的两条数据间隔超过指定 Session Gap，则认为前一个会话结束。会话窗口适合用户访问会话、设备在线会话、操作链路分析等场景。

会话窗口示意：

```text
Session Gap：30 秒

用户 A：
10:00:01 访问
10:00:10 点击
10:00:25 加购
---------- 间隔超过 30 秒 ----------
10:01:20 支付

窗口 1：[10:00:01, 10:00:55)
窗口 2：[10:01:20, 10:01:50)
```

适用场景如下：

| 场景         | 示例                     |
| ------------ | ------------------------ |
| 用户访问会话 | 用户连续行为分析         |
| 设备在线状态 | 设备连续上报期间视为在线 |
| 操作链路分析 | 一段连续操作构成一次会话 |
| 风控行为链   | 短时间连续高频行为分析   |

下面的代码按用户 ID 构建 30 秒会话窗口，统计每个用户会话内的行为数量。

```java
DataStream<UserSessionCount> resultStream = eventStream
        .keyBy(UserBehaviorEvent::getUserId)
        .window(EventTimeSessionWindows.withGap(Time.seconds(30)))
        .aggregate(new UserSessionCountAggregateFunction(), new UserSessionWindowFunction())
        .name("user-session-count-window")
        .uid("user-session-count-window");
```

会话窗口使用建议：

1. Session Gap 应基于真实用户行为或设备上报间隔确定。
2. 会话窗口可能发生合并，状态管理比滚动窗口复杂。
3. 迟到数据可能导致会话窗口再次合并，输出结果可能更新。
4. 适合行为链路分析，不适合固定周期报表。
5. 如果业务需要严格固定时间段统计，应使用滚动或滑动窗口。

### 全局窗口

全局窗口会将所有相同 Key 的数据放入一个全局窗口中，默认不会因为时间自动触发计算，必须配合自定义 Trigger 才能输出结果。Flink 官方窗口文档说明，Global Windows 的结束时间为 `Long.MAX_VALUE`，因此数据通常不会被判定为迟到。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/operators/windows/?utm_source=chatgpt.com))

全局窗口适用场景较少，通常用于自定义触发逻辑：

| 场景           | 示例                            |
| -------------- | ------------------------------- |
| 按条数触发     | 每累计 1000 条输出一次          |
| 自定义规则触发 | 满足某个业务条件后输出          |
| 非时间窗口     | 不依赖事件时间或处理时间        |
| 特殊聚合       | 需要自定义 Trigger 控制生命周期 |

下面的代码使用全局窗口，并通过 CountTrigger 每 100 条触发一次计算。

```java
DataStream<EventCount> resultStream = eventStream
        .keyBy(UserBehaviorEvent::getEventType)
        .window(GlobalWindows.create())
        .trigger(CountTrigger.of(100))
        .aggregate(new EventCountAggregateFunction(), new GlobalEventCountWindowFunction())
        .name("global-count-trigger-window")
        .uid("global-count-trigger-window");
```

全局窗口使用建议：

1. 必须显式配置 Trigger，否则一般不会自动输出。
2. 要特别关注状态清理，否则状态可能长期增长。
3. 不适合常规时间指标统计。
4. 需要设计清晰的触发条件和清理策略。
5. 生产项目中应谨慎使用，优先考虑滚动、滑动或会话窗口。

### 窗口分配器

窗口分配器用于决定每条数据属于哪个窗口。Flink 常见窗口分配器包括滚动窗口、滑动窗口、会话窗口和全局窗口。窗口分配器本身只负责划分窗口，不负责计算逻辑；计算逻辑由窗口函数完成，触发时机由 Trigger 控制。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/zh/docs/dev/datastream/operators/windows/?utm_source=chatgpt.com))

常见窗口分配器如下：

| 窗口分配器                      | 说明                     | 示例                       |
| ------------------------------- | ------------------------ | -------------------------- |
| `TumblingEventTimeWindows`      | Event Time 滚动窗口      | 每 1 分钟统计              |
| `SlidingEventTimeWindows`       | Event Time 滑动窗口      | 最近 5 分钟，每 1 分钟输出 |
| `EventTimeSessionWindows`       | Event Time 会话窗口      | 用户连续会话               |
| `TumblingProcessingTimeWindows` | Processing Time 滚动窗口 | 按处理时间统计             |
| `SlidingProcessingTimeWindows`  | Processing Time 滑动窗口 | 处理时间滚动告警           |
| `GlobalWindows`                 | 全局窗口                 | 按条数或自定义规则触发     |

窗口分配器选择建议：

```text
固定周期汇总       -> 滚动窗口
最近 N 分钟滚动统计 -> 滑动窗口
用户连续行为分析   -> 会话窗口
自定义触发规则     -> 全局窗口
严格业务发生时间   -> Event Time 窗口
只关心系统处理时间 -> Processing Time 窗口
```

开发时应优先明确以下问题：

1. 窗口按业务时间还是处理时间划分。
2. 窗口是否重叠。
3. 窗口是否需要动态合并。
4. 是否允许迟到数据更新结果。
5. 输出结果是明细、累计值还是窗口最终值。

### 触发器

Trigger 用于决定窗口何时触发计算。Flink 官方窗口文档说明，每个 WindowAssigner 都有默认 Trigger；如果默认行为不符合需求，可以通过 `.trigger(...)` 指定自定义 Trigger。Trigger 可以响应元素到达、事件时间定时器、处理时间定时器、窗口合并和窗口清理，并返回 `CONTINUE`、`FIRE`、`PURGE` 或 `FIRE_AND_PURGE` 等结果。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/operators/windows/?utm_source=chatgpt.com))

常见触发方式如下：

| 触发方式             | 说明                             |
| -------------------- | -------------------------------- |
| Event Time 触发      | Watermark 超过窗口结束时间后触发 |
| Processing Time 触发 | 系统处理时间到达指定时间后触发   |
| Count 触发           | 累计条数达到阈值后触发           |
| 自定义触发           | 根据业务条件、状态或定时器触发   |

下面的代码实现一个简单的处理时间提前触发器：窗口内有数据到达后，每隔 10 秒提前触发一次窗口计算。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/window/EarlyProcessingTimeTrigger.java`

```java
package io.github.atengk.flink.job.userbehavior.window;

import org.apache.flink.streaming.api.windowing.triggers.Trigger;
import org.apache.flink.streaming.api.windowing.triggers.TriggerResult;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;

/**
 * 处理时间提前触发器
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class EarlyProcessingTimeTrigger<T> extends Trigger<T, TimeWindow> {

    private final long intervalMs;

    /**
     * 创建处理时间提前触发器
     *
     * @param intervalMs 触发间隔毫秒数
     */
    private EarlyProcessingTimeTrigger(long intervalMs) {
        this.intervalMs = intervalMs;
    }

    /**
     * 创建触发器实例
     *
     * @param intervalMs 触发间隔毫秒数
     * @param <T> 元素类型
     * @return 触发器
     */
    public static <T> EarlyProcessingTimeTrigger<T> of(long intervalMs) {
        return new EarlyProcessingTimeTrigger<>(intervalMs);
    }

    /**
     * 元素到达时注册下一次处理时间触发器
     *
     * @param element 元素
     * @param timestamp 元素时间戳
     * @param window 窗口
     * @param ctx 触发上下文
     * @return 触发结果
     */
    @Override
    public TriggerResult onElement(T element, long timestamp, TimeWindow window, TriggerContext ctx) {
        long now = ctx.getCurrentProcessingTime();
        long nextTriggerTime = now - now % intervalMs + intervalMs;
        if (nextTriggerTime < window.getEnd()) {
            ctx.registerProcessingTimeTimer(nextTriggerTime);
        }
        ctx.registerEventTimeTimer(window.maxTimestamp());
        return TriggerResult.CONTINUE;
    }

    /**
     * 事件时间触发，窗口最终输出
     *
     * @param time 事件时间
     * @param window 窗口
     * @param ctx 触发上下文
     * @return 触发结果
     */
    @Override
    public TriggerResult onEventTime(long time, TimeWindow window, TriggerContext ctx) {
        return time == window.maxTimestamp() ? TriggerResult.FIRE_AND_PURGE : TriggerResult.CONTINUE;
    }

    /**
     * 处理时间触发，提前输出但不清理窗口数据
     *
     * @param time 处理时间
     * @param window 窗口
     * @param ctx 触发上下文
     * @return 触发结果
     */
    @Override
    public TriggerResult onProcessingTime(long time, TimeWindow window, TriggerContext ctx) {
        long nextTriggerTime = time + intervalMs;
        if (nextTriggerTime < window.getEnd()) {
            ctx.registerProcessingTimeTimer(nextTriggerTime);
        }
        return TriggerResult.FIRE;
    }

    /**
     * 清理窗口定时器
     *
     * @param window 窗口
     * @param ctx 触发上下文
     */
    @Override
    public void clear(TimeWindow window, TriggerContext ctx) {
        ctx.deleteEventTimeTimer(window.maxTimestamp());
    }
}
```

使用示例：

```java
DataStream<EventCount> resultStream = eventStream
        .keyBy(UserBehaviorEvent::getEventType)
        .window(TumblingEventTimeWindows.of(Time.minutes(1)))
        .trigger(EarlyProcessingTimeTrigger.of(10_000L))
        .aggregate(new EventCountAggregateFunction(), new EventCountWindowFunction())
        .name("early-trigger-window")
        .uid("early-trigger-window");
```

触发器使用建议：

1. 默认触发器满足大多数窗口统计需求。
2. 提前触发会产生多次结果，下游 Sink 需要支持更新或幂等。
3. 自定义 Trigger 必须处理清理逻辑。
4. Session Window 自定义 Trigger 需要考虑窗口合并。
5. 触发频率过高会增加计算和 Sink 压力。

### 增量聚合

增量聚合是窗口计算中推荐优先使用的方式。Flink 官方窗口文档说明，`ReduceFunction` 和 `AggregateFunction` 可以在数据到达时逐步聚合，不需要缓存窗口内所有元素，因此通常比 `ProcessWindowFunction` 更高效。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/operators/windows/?utm_source=chatgpt.com))

下面的代码实现按事件类型统计数量的增量聚合函数。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/window/EventCountAggregateFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.window;

import io.github.atengk.flink.model.UserBehaviorEvent;
import org.apache.flink.api.common.functions.AggregateFunction;

/**
 * 事件数量增量聚合函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class EventCountAggregateFunction implements AggregateFunction<UserBehaviorEvent, Long, Long> {

    /**
     * 创建累加器
     *
     * @return 初始计数
     */
    @Override
    public Long createAccumulator() {
        return 0L;
    }

    /**
     * 累加事件数量
     *
     * @param value 用户行为事件
     * @param accumulator 当前计数
     * @return 新计数
     */
    @Override
    public Long add(UserBehaviorEvent value, Long accumulator) {
        return accumulator + 1;
    }

    /**
     * 获取聚合结果
     *
     * @param accumulator 当前计数
     * @return 事件数量
     */
    @Override
    public Long getResult(Long accumulator) {
        return accumulator;
    }

    /**
     * 合并累加器
     *
     * @param left 左累加器
     * @param right 右累加器
     * @return 合并后的计数
     */
    @Override
    public Long merge(Long left, Long right) {
        return left + right;
    }
}
```

增量聚合使用示例：

```java
DataStream<EventCount> resultStream = eventStream
        .keyBy(UserBehaviorEvent::getEventType)
        .window(TumblingEventTimeWindows.of(Time.minutes(1)))
        .aggregate(new EventCountAggregateFunction(), new EventCountWindowFunction())
        .name("aggregate-event-count-window")
        .uid("aggregate-event-count-window");
```

增量聚合建议：

1. 计数、求和、最值、简单均值优先使用增量聚合。
2. 聚合结果需要窗口开始和结束时间时，可结合 `ProcessWindowFunction`。
3. 增量聚合状态小，适合高吞吐实时指标。
4. 不能表达需要遍历全量窗口数据的逻辑。
5. 累加器类型要稳定，避免升级导致状态不兼容。

### 全量窗口函数

全量窗口函数会在窗口触发时拿到窗口内全部元素。Flink 官方文档说明，`ProcessWindowFunction` 可以访问窗口中的所有元素以及窗口上下文信息，但代价是 Flink 需要在窗口触发前缓存窗口内元素，因此性能和资源消耗高于增量聚合。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/operators/windows/?utm_source=chatgpt.com))

适用场景如下：

| 场景           | 说明                    |
| -------------- | ----------------------- |
| TopN           | 需要窗口内排序          |
| 去重明细       | 需要遍历窗口内所有数据  |
| 复杂规则       | 需要多条数据共同判断    |
| 输出窗口元信息 | 需要窗口开始、结束、Key |
| 小窗口小数据量 | 数据量可控的复杂处理    |

下面的代码输出窗口开始时间、结束时间和聚合结果。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/window/EventCountWindowFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.window;

import io.github.atengk.flink.model.EventCount;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

/**
 * 事件数量窗口结果函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class EventCountWindowFunction extends ProcessWindowFunction<Long, EventCount, String, TimeWindow> {

    /**
     * 输出窗口统计结果
     *
     * @param key 分组键
     * @param context 窗口上下文
     * @param elements 聚合结果集合
     * @param out 输出收集器
     */
    @Override
    public void process(String key, Context context, Iterable<Long> elements, Collector<EventCount> out) {
        Long count = elements.iterator().next();
        out.collect(new EventCount(key, count));
    }
}
```

如果需要输出窗口时间，建议扩展结果模型，例如 `WindowEventCount`：

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 窗口事件统计结果
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WindowEventCount implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventType;

    private Long windowStart;

    private Long windowEnd;

    private Long count;
}
```

全量窗口函数建议：

1. 能用增量聚合时不要使用全量窗口函数。
2. TopN、排序、复杂匹配等场景才考虑全量窗口。
3. 窗口大小要严格控制，避免 OOM。
4. 如果需要窗口元信息，可采用“增量聚合 + ProcessWindowFunction”的组合。
5. 生产作业应监控状态大小和窗口触发延迟。

### 窗口迟到数据处理

窗口迟到数据是指数据到达时，Watermark 已经超过该数据所属窗口的结束时间。Flink 官方窗口文档说明，默认情况下，Watermark 超过窗口结束时间后到达的迟到元素会被丢弃；可以通过 `allowedLateness(...)` 允许一定范围内的迟到数据继续更新窗口，也可以通过 `sideOutputLateData(...)` 将被丢弃的迟到数据输出到侧输出流。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/operators/windows/?utm_source=chatgpt.com))

迟到数据处理流程：

```text
事件到达
  |
  v
判断事件时间所属窗口
  |
  v
Watermark 是否已经超过窗口结束时间
  |
  |-- 否：进入窗口正常计算
  |
  |-- 是：判断是否在 allowedLateness 范围内
          |
          |-- 是：更新窗口并可能再次触发结果
          |
          |-- 否：进入迟到数据侧输出流或被丢弃
```

迟到数据侧输出示例：

```java
OutputTag<UserBehaviorEvent> lateOutputTag = new OutputTag<UserBehaviorEvent>("late-user-behavior") {
};

SingleOutputStreamOperator<EventCount> resultStream = eventStream
        .keyBy(UserBehaviorEvent::getEventType)
        .window(TumblingEventTimeWindows.of(Time.minutes(1)))
        .allowedLateness(Time.seconds(30))
        .sideOutputLateData(lateOutputTag)
        .aggregate(new EventCountAggregateFunction(), new EventCountWindowFunction())
        .name("window-with-late-data")
        .uid("window-with-late-data");

DataStream<UserBehaviorEvent> lateStream = resultStream
        .getSideOutput(lateOutputTag)
        .name("late-user-behavior-stream");
```

迟到数据处理建议：

1. 默认迟到数据会被丢弃，生产作业应显式设计迟到策略。
2. `allowedLateness` 会延长窗口状态保留时间，增加状态压力。
3. 迟到数据可能导致窗口再次输出结果，下游 Sink 必须支持更新或幂等。
4. 被丢弃的迟到数据建议进入侧输出流，便于后续补偿。
5. 延迟策略应结合真实数据乱序情况，而不是随意设置。
6. 对最终准确性要求高的指标，应设计离线校验和补数链路。

## 状态管理

本章节用于说明 Flink 的状态管理方式。状态是 Flink 支持复杂流处理的关键能力，窗口聚合、去重、规则匹配、双流 Join、维表缓存、定时器和 Exactly Once 语义都依赖状态。Flink 官方状态文档说明，如果要使用 Keyed State，必须先通过 `keyBy(...)` 将 DataStream 转换为 KeyedStream；Keyed State 只能在 KeyedStream 上使用，并且状态作用域限定在当前输入元素的 Key 上。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/fault-tolerance/state/?utm_source=chatgpt.com))

### Keyed State

Keyed State 是按 Key 分区的状态，只有在 `keyBy(...)` 之后的算子中才能使用。每个 Key 都有自己独立的状态空间，适合用户去重、订单状态维护、计数、最近一次事件、规则匹配、超时检测等场景。

适用场景如下：

| 场景     | Key         | 状态                   |
| -------- | ----------- | ---------------------- |
| 用户去重 | `userId`    | 是否已出现             |
| 订单超时 | `orderId`   | 订单创建时间、支付状态 |
| 风控规则 | `userId`    | 最近 N 次操作          |
| 实时计数 | `eventType` | 累计数量               |
| 设备状态 | `deviceId`  | 最近上报时间、状态     |

Keyed State 使用基本流程：

```text
DataStream
  |
  v
keyBy(...)
  |
  v
KeyedStream
  |
  v
RichFunction / KeyedProcessFunction
  |
  v
getRuntimeContext().getState(...)
```

Keyed State 使用建议：

1. 必须先 `keyBy`，否则不能使用 Keyed State。
2. Key 设计要稳定，不能使用随机值。
3. Key 基数过低会导致数据倾斜。
4. 状态模型变更需要评估 Savepoint 兼容性。
5. 长期状态必须配置 TTL 或清理逻辑。
6. 状态访问应尽量轻量，不要在每条数据中进行复杂对象转换。

### Operator State

Operator State 是绑定到算子并行实例上的状态，不按业务 Key 分区。它适合保存 Source 读取位点、分区分配、批量缓存、算子本地缓冲等。Flink 官方状态文档将状态分为 Keyed State 和 Operator State，并说明 Operator State 常通过 `CheckpointedFunction` 管理本地变量的容错。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/fault-tolerance/state/?utm_source=chatgpt.com))

适用场景如下：

| 场景               | 说明                           |
| ------------------ | ------------------------------ |
| 自定义 Source 位点 | 保存当前读取 Offset、文件位置  |
| 批量 Sink 缓冲     | 保存未提交批次数据             |
| 分区分配状态       | 保存当前算子实例负责的外部分片 |
| 本地缓存恢复       | 将本地变量纳入 Checkpoint      |
| 非 Keyed 算子状态  | 不依赖业务 Key 的状态          |

下面的代码演示使用 `CheckpointedFunction` 管理 Operator State。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/state/BufferedSinkOperator.java`

```java
package io.github.atengk.flink.job.userbehavior.state;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量缓冲 Operator State 示例
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class BufferedSinkOperator implements CheckpointedFunction {

    private final List<String> buffer = new ArrayList<>();

    private transient ListState<String> checkpointedState;

    /**
     * Checkpoint 时保存本地缓冲数据
     *
     * @param context 快照上下文
     * @throws Exception 状态保存异常
     */
    @Override
    public void snapshotState(FunctionSnapshotContext context) throws Exception {
        checkpointedState.clear();
        for (String value : buffer) {
            checkpointedState.add(value);
        }
        log.info("保存Operator State完成，缓冲数据条数：{}", buffer.size());
    }

    /**
     * 初始化或恢复 Operator State
     *
     * @param context 初始化上下文
     * @throws Exception 状态初始化异常
     */
    @Override
    public void initializeState(FunctionInitializationContext context) throws Exception {
        ListStateDescriptor<String> descriptor = new ListStateDescriptor<>("buffered-values", String.class);
        checkpointedState = context.getOperatorStateStore().getListState(descriptor);

        if (context.isRestored()) {
            for (String value : checkpointedState.get()) {
                buffer.add(value);
            }
            log.info("恢复Operator State完成，恢复数据条数：{}", buffer.size());
        }
    }

    /**
     * 添加缓冲数据
     *
     * @param value 数据
     */
    public void add(String value) {
        if (value != null) {
            buffer.add(value);
        }
    }

    /**
     * 判断缓冲是否为空
     *
     * @return 是否为空
     */
    public boolean isEmpty() {
        return CollUtil.isEmpty(buffer);
    }
}
```

Operator State 使用建议：

1. 自定义 Source 和批量 Sink 常用 Operator State。
2. Operator State 不按业务 Key 管理。
3. 并行度变化时需要考虑状态重新分配。
4. 状态内容应尽量小，不适合保存无限增长的数据。
5. 如果逻辑可以按 Key 建模，优先使用 Keyed State。

### ValueState

`ValueState<T>` 用于保存每个 Key 对应的单个值。Flink 官方状态文档说明，`ValueState<T>` 可以通过 `update(T)` 更新，通过 `value()` 获取当前 Key 的值。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/fault-tolerance/state/?utm_source=chatgpt.com))

适用场景如下：

| 场景         | 状态内容         |
| ------------ | ---------------- |
| 最近一次事件 | 最近一条用户行为 |
| 去重标记     | 是否已处理       |
| 累计数量     | 当前 Key 的计数  |
| 订单状态     | 当前订单状态     |
| 超时检测     | 事件创建时间     |

下面的代码使用 `ValueState` 统计每个事件类型的累计数量。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/state/EventTypeCountProcessFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.state;

import io.github.atengk.flink.model.EventCount;
import io.github.atengk.flink.model.UserBehaviorEvent;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

/**
 * 事件类型累计计数处理函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class EventTypeCountProcessFunction extends KeyedProcessFunction<String, UserBehaviorEvent, EventCount> {

    private transient ValueState<Long> countState;

    /**
     * 初始化状态
     *
     * @param parameters 配置参数
     */
    @Override
    public void open(org.apache.flink.configuration.Configuration parameters) {
        ValueStateDescriptor<Long> descriptor = new ValueStateDescriptor<>("event-type-count", Long.class);
        countState = getRuntimeContext().getState(descriptor);
    }

    /**
     * 处理用户行为事件
     *
     * @param value 用户行为事件
     * @param ctx 上下文
     * @param out 输出收集器
     * @throws Exception 状态访问异常
     */
    @Override
    public void processElement(UserBehaviorEvent value, Context ctx, Collector<EventCount> out) throws Exception {
        Long currentCount = countState.value();
        long newCount = currentCount == null ? 1L : currentCount + 1L;
        countState.update(newCount);
        out.collect(new EventCount(value.getEventType(), newCount));
    }
}
```

使用示例：

```java
DataStream<EventCount> countStream = eventStream
        .keyBy(UserBehaviorEvent::getEventType)
        .process(new EventTypeCountProcessFunction())
        .name("event-type-count-process")
        .uid("event-type-count-process");
```

ValueState 使用建议：

1. 保存单个值时优先使用。
2. 状态值可能为空，读取后必须判空。
3. 长期状态要配置 TTL。
4. 不要把大 List 或 Map 塞进 ValueState。
5. 状态名称一旦用于生产，应保持稳定。

### ListState

`ListState<T>` 用于保存每个 Key 对应的元素列表。Flink 官方状态文档说明，`ListState<T>` 可以通过 `add(T)`、`addAll(List<T>)` 增加元素，通过 `get()` 获取当前所有元素，也可以用 `update(List<T>)` 替换已有列表。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/fault-tolerance/state/?utm_source=chatgpt.com))

适用场景如下：

| 场景         | 示例               |
| ------------ | ------------------ |
| 最近行为列表 | 用户最近 N 次操作  |
| 批量缓冲     | 暂存一批待处理数据 |
| 简单轨迹     | 设备近期上报点     |
| 会话明细     | 一段会话内事件列表 |

下面的代码使用 `ListState` 保存每个用户最近行为，并限制最多保留 10 条。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/state/UserRecentEventsProcessFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.state;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.flink.model.UserBehaviorEvent;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 用户最近行为列表处理函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class UserRecentEventsProcessFunction extends KeyedProcessFunction<String, UserBehaviorEvent, List<UserBehaviorEvent>> {

    private static final int MAX_SIZE = 10;

    private transient ListState<UserBehaviorEvent> recentEventState;

    /**
     * 初始化状态
     *
     * @param parameters 配置参数
     */
    @Override
    public void open(org.apache.flink.configuration.Configuration parameters) {
        ListStateDescriptor<UserBehaviorEvent> descriptor = new ListStateDescriptor<>(
                "user-recent-events",
                UserBehaviorEvent.class
        );
        recentEventState = getRuntimeContext().getListState(descriptor);
    }

    /**
     * 处理用户行为事件
     *
     * @param value 用户行为事件
     * @param ctx 上下文
     * @param out 输出收集器
     * @throws Exception 状态访问异常
     */
    @Override
    public void processElement(UserBehaviorEvent value, Context ctx, Collector<List<UserBehaviorEvent>> out) throws Exception {
        List<UserBehaviorEvent> events = new ArrayList<>();
        for (UserBehaviorEvent event : recentEventState.get()) {
            events.add(event);
        }

        events.add(value);
        events.sort(Comparator.comparing(UserBehaviorEvent::getEventTime).reversed());

        if (events.size() > MAX_SIZE) {
            events = events.subList(0, MAX_SIZE);
        }

        recentEventState.update(events);

        if (CollUtil.isNotEmpty(events)) {
            out.collect(events);
        }
    }
}
```

ListState 使用建议：

1. 列表状态必须控制长度，避免无限增长。
2. 保存明细时要考虑状态大小和序列化成本。
3. 如果需要按子 Key 查询，优先使用 MapState。
4. ListState 适合小集合，不适合大规模明细缓存。
5. 配置 TTL 后，集合元素可以独立过期。

### MapState

`MapState<UK, UV>` 用于保存每个 Key 下的一组键值映射。Flink 官方状态文档说明，`MapState` 可以通过 `put(UK, UV)`、`putAll(Map<UK, UV>)` 写入，通过 `get(UK)` 获取指定子 Key 的值，也可以遍历 `entries()`、`keys()` 和 `values()`。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/fault-tolerance/state/?utm_source=chatgpt.com))

适用场景如下：

| 场景         | 示例                        |
| ------------ | --------------------------- |
| 用户商品去重 | `itemId -> 是否已点击`      |
| 订单状态明细 | `orderId -> 状态`           |
| 规则匹配     | `ruleId -> 规则命中次数`    |
| 维度缓存     | `dimensionId -> 维度值`     |
| 窗口内去重   | `uniqueKey -> 首次出现时间` |

下面的代码使用 `MapState` 实现用户维度的商品点击去重。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/state/UserItemDeduplicateFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.state;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.flink.model.UserBehaviorEvent;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

/**
 * 用户商品点击去重函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class UserItemDeduplicateFunction extends KeyedProcessFunction<String, UserBehaviorEvent, UserBehaviorEvent> {

    private transient MapState<String, Boolean> itemClickState;

    /**
     * 初始化状态
     *
     * @param parameters 配置参数
     */
    @Override
    public void open(org.apache.flink.configuration.Configuration parameters) {
        MapStateDescriptor<String, Boolean> descriptor = new MapStateDescriptor<>(
                "user-clicked-items",
                String.class,
                Boolean.class
        );
        itemClickState = getRuntimeContext().getMapState(descriptor);
    }

    /**
     * 处理用户行为事件
     *
     * @param value 用户行为事件
     * @param ctx 上下文
     * @param out 输出收集器
     * @throws Exception 状态访问异常
     */
    @Override
    public void processElement(UserBehaviorEvent value, Context ctx, Collector<UserBehaviorEvent> out) throws Exception {
        if (!StrUtil.equals(value.getEventType(), "click") || StrUtil.isBlank(value.getItemId())) {
            return;
        }

        Boolean clicked = itemClickState.get(value.getItemId());
        if (Boolean.TRUE.equals(clicked)) {
            return;
        }

        itemClickState.put(value.getItemId(), true);
        out.collect(value);
    }
}
```

MapState 使用建议：

1. 需要按子 Key 查询时优先使用。
2. MapState 容易增长，要配置 TTL 或清理逻辑。
3. 子 Key 应使用短且稳定的字段，例如 ID。
4. 不建议在每条数据中全量遍历大 Map。
5. 对于去重场景，要明确去重周期和状态清理策略。

### ReducingState

`ReducingState<T>` 用于保存一个通过 `ReduceFunction` 聚合后的状态值。Flink 官方状态文档说明，`ReducingState<T>` 与 `ListState` 接口类似，但每次 `add(T)` 都会通过指定的 `ReduceFunction` 合并成一个聚合值。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/fault-tolerance/state/?utm_source=chatgpt.com))

适用场景如下：

| 场景     | 示例                  |
| -------- | --------------------- |
| 累计计数 | 每个 Key 的累计事件数 |
| 累计金额 | 每个用户累计支付金额  |
| 最大值   | 每个设备最高温度      |
| 最小值   | 每个订单最低报价      |
| 合并对象 | 合并统计对象          |

下面的代码使用 `ReducingState` 统计每个事件类型的累计数量。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/state/EventCountReducingStateFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.state;

import io.github.atengk.flink.model.EventCount;
import io.github.atengk.flink.model.UserBehaviorEvent;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.state.ReducingState;
import org.apache.flink.api.common.state.ReducingStateDescriptor;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

/**
 * 事件数量 ReducingState 处理函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class EventCountReducingStateFunction extends KeyedProcessFunction<String, UserBehaviorEvent, EventCount> {

    private transient ReducingState<EventCount> countState;

    /**
     * 初始化状态
     *
     * @param parameters 配置参数
     */
    @Override
    public void open(org.apache.flink.configuration.Configuration parameters) {
        ReducingStateDescriptor<EventCount> descriptor = new ReducingStateDescriptor<>(
                "event-count-reducing-state",
                new EventCountReduceFunction(),
                EventCount.class
        );
        countState = getRuntimeContext().getReducingState(descriptor);
    }

    /**
     * 处理用户行为事件
     *
     * @param value 用户行为事件
     * @param ctx 上下文
     * @param out 输出收集器
     * @throws Exception 状态访问异常
     */
    @Override
    public void processElement(UserBehaviorEvent value, Context ctx, Collector<EventCount> out) throws Exception {
        countState.add(new EventCount(value.getEventType(), 1L));
        out.collect(countState.get());
    }

    /**
     * 事件数量合并函数
     *
     * @author Ateng
     * @since 2026-05-11
     */
    public static class EventCountReduceFunction implements ReduceFunction<EventCount> {

        /**
         * 合并事件数量
         *
         * @param left 左值
         * @param right 右值
         * @return 合并结果
         */
        @Override
        public EventCount reduce(EventCount left, EventCount right) {
            return new EventCount(left.getEventType(), left.getCount() + right.getCount());
        }
    }
}
```

ReducingState 使用建议：

1. 输入和状态输出类型相同时适合使用。
2. 聚合逻辑简单时比 ListState 更节省状态。
3. 复杂聚合或输入输出类型不一致时，使用 AggregatingState。
4. ReduceFunction 应保持确定性。
5. 状态清理和 TTL 仍然需要单独设计。

### AggregatingState

`AggregatingState<IN, OUT>` 用于输入类型和输出类型不同的状态聚合。Flink 官方状态文档说明，`AggregatingState` 保存一个聚合值，添加元素使用 `add(IN)`，聚合逻辑由 `AggregateFunction` 定义，输出类型可以不同于输入类型。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/fault-tolerance/state/?utm_source=chatgpt.com))

适用场景如下：

| 场景     | 输入          | 输出     |
| -------- | ------------- | -------- |
| 平均值   | 单条事件      | 平均值   |
| 计数     | 单条事件      | Long     |
| 金额统计 | 订单事件      | 统计对象 |
| 复杂指标 | 行为事件      | 指标结果 |
| 比率计算 | 成功/失败事件 | 成功率   |

下面的代码使用 `AggregatingState` 统计每个事件类型的累计数量。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/state/EventCountAggregatingStateFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.state;

import io.github.atengk.flink.model.EventCount;
import io.github.atengk.flink.model.UserBehaviorEvent;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.state.AggregatingState;
import org.apache.flink.api.common.state.AggregatingStateDescriptor;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

/**
 * 事件数量 AggregatingState 处理函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class EventCountAggregatingStateFunction extends KeyedProcessFunction<String, UserBehaviorEvent, EventCount> {

    private transient AggregatingState<UserBehaviorEvent, Long> countState;

    /**
     * 初始化状态
     *
     * @param parameters 配置参数
     */
    @Override
    public void open(org.apache.flink.configuration.Configuration parameters) {
        AggregatingStateDescriptor<UserBehaviorEvent, Long, Long> descriptor = new AggregatingStateDescriptor<>(
                "event-count-aggregating-state",
                new CountAggregateFunction(),
                Long.class
        );
        countState = getRuntimeContext().getAggregatingState(descriptor);
    }

    /**
     * 处理用户行为事件
     *
     * @param value 用户行为事件
     * @param ctx 上下文
     * @param out 输出收集器
     * @throws Exception 状态访问异常
     */
    @Override
    public void processElement(UserBehaviorEvent value, Context ctx, Collector<EventCount> out) throws Exception {
        countState.add(value);
        out.collect(new EventCount(value.getEventType(), countState.get()));
    }

    /**
     * 计数聚合函数
     *
     * @author Ateng
     * @since 2026-05-11
     */
    public static class CountAggregateFunction implements AggregateFunction<UserBehaviorEvent, Long, Long> {

        /**
         * 创建累加器
         *
         * @return 初始计数
         */
        @Override
        public Long createAccumulator() {
            return 0L;
        }

        /**
         * 累加数据
         *
         * @param value 用户行为事件
         * @param accumulator 累加器
         * @return 新累加器
         */
        @Override
        public Long add(UserBehaviorEvent value, Long accumulator) {
            return accumulator + 1;
        }

        /**
         * 获取结果
         *
         * @param accumulator 累加器
         * @return 计数结果
         */
        @Override
        public Long getResult(Long accumulator) {
            return accumulator;
        }

        /**
         * 合并累加器
         *
         * @param left 左累加器
         * @param right 右累加器
         * @return 合并结果
         */
        @Override
        public Long merge(Long left, Long right) {
            return left + right;
        }
    }
}
```

AggregatingState 使用建议：

1. 输入类型和输出类型不同的聚合优先使用。
2. 比 ListState 更节省状态空间。
3. 聚合函数必须具备确定性。
4. 累加器类型要保持兼容，避免影响 Savepoint 恢复。
5. 复杂指标计算需要补充单元测试验证。

### 状态 TTL

状态 TTL 用于控制状态的生命周期，避免长期运行作业状态无限增长。Flink 官方状态文档说明，可以为任意类型的 Keyed State 配置 TTL；如果状态值过期，Flink 会尽力清理；集合类型状态支持元素级 TTL，即 ListState 元素和 MapState 条目可以独立过期。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/fault-tolerance/state/?utm_source=chatgpt.com))

状态 TTL 配置示例：

```java
StateTtlConfig ttlConfig = StateTtlConfig
        .newBuilder(Duration.ofHours(24))
        .setUpdateType(StateTtlConfig.UpdateType.OnCreateAndWrite)
        .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired)
        .build();

ValueStateDescriptor<Long> descriptor = new ValueStateDescriptor<>("event-type-count", Long.class);
descriptor.enableTimeToLive(ttlConfig);
```

TTL 参数说明：

| 参数                          | 说明                         |
| ----------------------------- | ---------------------------- |
| `newBuilder(Duration)`        | 设置状态存活时间             |
| `OnCreateAndWrite`            | 创建和写入时刷新 TTL         |
| `OnReadAndWrite`              | 读取和写入时刷新 TTL         |
| `NeverReturnExpired`          | 过期状态不返回               |
| `ReturnExpiredIfNotCleanedUp` | 如果尚未清理，允许返回过期值 |

带 TTL 的 ValueState 示例：

```java
ValueStateDescriptor<Long> descriptor = new ValueStateDescriptor<>("user-last-event-time", Long.class);

StateTtlConfig ttlConfig = StateTtlConfig
        .newBuilder(Duration.ofDays(7))
        .setUpdateType(StateTtlConfig.UpdateType.OnCreateAndWrite)
        .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired)
        .build();

descriptor.enableTimeToLive(ttlConfig);

ValueState<Long> lastEventTimeState = getRuntimeContext().getState(descriptor);
```

状态 TTL 使用建议：

1. 去重、缓存、最近行为类状态必须配置 TTL。
2. TTL 时间应基于业务周期设计，例如 1 天、7 天、30 天。
3. `OnReadAndWrite` 会因读取刷新状态，可能让状态长期不清理。
4. `NeverReturnExpired` 更适合对准确性要求高的场景。
5. TTL 变更可能影响历史状态读取结果，生产变更前要测试。
6. TTL 不是精确到期立即删除，而是按 Flink 清理机制尽力清理。

### 状态后端选择

状态后端决定状态在运行时如何存储，以及 Checkpoint 时如何持久化。Flink 官方状态后端文档说明，DataStream 程序通常会以窗口、Key/Value 状态接口、`CheckpointedFunction` 等形式保存状态；开启 Checkpoint 后，状态会随 Checkpoint 持久化，用于防止数据丢失并保障恢复一致性。Flink 内置的主要状态后端包括 `HashMapStateBackend` 和 `EmbeddedRocksDBStateBackend`；如果未配置，默认使用 `HashMapStateBackend`。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/ops/state/state_backends/?utm_source=chatgpt.com))

常见状态后端对比：

| 状态后端                      | 存储位置                  | 适用场景                       |
| ----------------------------- | ------------------------- | ------------------------------ |
| `HashMapStateBackend`         | JVM 堆内对象              | 状态较小、低延迟、本地调试     |
| `EmbeddedRocksDBStateBackend` | TaskManager 本地 RocksDB  | 大状态、长窗口、大 Keyed State |
| Checkpoint Storage            | 远程文件系统或 JobManager | 决定状态快照保存位置           |

状态后端配置示例：

```yaml
# 使用 RocksDB 状态后端，适合大状态作业
state.backend: rocksdb

# 使用文件系统保存 Checkpoint
execution.checkpointing.storage: filesystem

# Checkpoint 目录，生产环境建议使用 HDFS、S3、OSS 等可靠存储
execution.checkpointing.dir: hdfs:///flink/checkpoints/user-behavior-job

# 开启增量 Checkpoint，RocksDB 常用配置
state.backend.incremental: true
```

Java 代码配置示例：

```java
Configuration configuration = new Configuration();

// 状态后端类型：hashmap 或 rocksdb
configuration.set(StateBackendOptions.STATE_BACKEND, "rocksdb");

// Checkpoint 存储类型
configuration.set(CheckpointingOptions.CHECKPOINT_STORAGE, "filesystem");

// Checkpoint 目录
configuration.set(CheckpointingOptions.CHECKPOINTS_DIRECTORY, "hdfs:///flink/checkpoints/user-behavior-job");

env.configure(configuration);
```

状态后端选择建议：

1. 本地调试和小状态作业可使用 `HashMapStateBackend`。
2. 大状态、长窗口、海量 Key、复杂去重优先使用 RocksDB。
3. RocksDB 状态读写需要序列化和反序列化，吞吐通常低于堆内状态。
4. RocksDB 支持增量 Checkpoint，适合降低大状态 Checkpoint 压力。
5. 生产 Checkpoint 目录应使用可靠分布式存储。
6. 状态后端变更前必须使用测试环境验证 Savepoint 恢复。

### 状态数据清理

状态数据清理用于防止长期运行作业状态无限膨胀。状态增长会导致 Checkpoint 变慢、恢复时间变长、RocksDB 磁盘占用升高、TaskManager 内存压力增加，最终可能引发反压或 OOM。

常见清理方式如下：

| 清理方式     | 适用场景                              |
| ------------ | ------------------------------------- |
| State TTL    | 去重、缓存、最近行为、维表状态        |
| 定时器清理   | 订单超时、会话过期、规则状态          |
| 窗口自动清理 | 时间窗口状态                          |
| 手动 clear   | 状态不再需要时立即清理                |
| 外部补偿清理 | 结合业务生命周期清理                  |
| 定期运维清理 | Checkpoint、Savepoint、脏数据目录清理 |

下面的代码使用定时器清理用户最近事件状态。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/state/UserEventStateCleanFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.state;

import io.github.atengk.flink.model.UserBehaviorEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

/**
 * 用户事件状态定时清理函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserEventStateCleanFunction extends KeyedProcessFunction<String, UserBehaviorEvent, UserBehaviorEvent> {

    private static final long CLEAN_DELAY_MS = 24 * 60 * 60 * 1000L;

    private transient ListState<UserBehaviorEvent> eventState;

    private transient ValueState<Long> cleanTimerState;

    /**
     * 初始化状态
     *
     * @param parameters 配置参数
     */
    @Override
    public void open(org.apache.flink.configuration.Configuration parameters) {
        eventState = getRuntimeContext().getListState(
                new ListStateDescriptor<>("user-event-list", UserBehaviorEvent.class)
        );
        cleanTimerState = getRuntimeContext().getState(
                new ValueStateDescriptor<>("user-event-clean-timer", Long.class)
        );
    }

    /**
     * 处理用户事件并注册清理定时器
     *
     * @param value 用户行为事件
     * @param ctx 上下文
     * @param out 输出收集器
     * @throws Exception 状态访问异常
     */
    @Override
    public void processElement(UserBehaviorEvent value, Context ctx, Collector<UserBehaviorEvent> out) throws Exception {
        eventState.add(value);

        Long currentTimer = cleanTimerState.value();
        if (currentTimer == null) {
            long cleanTime = ctx.timerService().currentProcessingTime() + CLEAN_DELAY_MS;
            ctx.timerService().registerProcessingTimeTimer(cleanTime);
            cleanTimerState.update(cleanTime);
        }

        out.collect(value);
    }

    /**
     * 定时清理状态
     *
     * @param timestamp 触发时间
     * @param ctx 定时器上下文
     * @param out 输出收集器
     * @throws Exception 状态清理异常
     */
    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<UserBehaviorEvent> out) throws Exception {
        eventState.clear();
        cleanTimerState.clear();
        log.info("用户事件状态已清理，Key：{}，清理时间：{}", ctx.getCurrentKey(), timestamp);
    }
}
```

状态清理建议：

1. 长期运行作业必须设计状态生命周期。
2. TTL 和定时器可以组合使用。
3. `clear()` 应在状态不再需要时主动调用。
4. 窗口允许迟到时间越长，窗口状态保留越久。
5. MapState 和 ListState 尤其要防止无限增长。
6. 监控状态大小、Checkpoint 时长、RocksDB 磁盘占用和恢复耗时。
7. 状态清理策略变更前应验证对业务结果的影响。


## 容错机制

本章节用于说明 Flink 作业在异常、节点故障、网络抖动、外部系统短暂不可用、Task 失败等场景下的恢复机制。Flink 的容错核心由 Checkpoint、状态后端、Checkpoint Storage、重启策略、Source 可重放能力和 Sink 一致性能力共同构成。Flink 官方文档说明，Checkpoint 会保存算子状态和数据源位置，使应用可以在故障后恢复到一致状态；端到端 Exactly Once 还要求 Source 可重放，并且 Sink 具备事务或幂等能力。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/learn-flink/fault_tolerance/?utm_source=chatgpt.com))

### Checkpoint 机制

Checkpoint 是 Flink 自动触发的状态快照机制，用于故障恢复。作业运行过程中，JobManager 中的 Checkpoint Coordinator 会周期性触发 Checkpoint，Source 记录当前读取位置并向数据流中注入 Checkpoint Barrier；Barrier 随数据流向下游传播，各个算子在收到 Barrier 后保存自身状态。Flink 官方文档说明，Checkpoint 使用异步 Barrier Snapshotting，快照会包含每个算子在消费 Barrier 之前数据后形成的状态。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/learn-flink/fault_tolerance/?utm_source=chatgpt.com))

Checkpoint 的核心流程如下：

```text
1. JobManager 周期性触发 Checkpoint
2. Source 保存 Kafka Offset、文件位置、CDC Binlog 位点等读取位置
3. Source 向下游发送 Checkpoint Barrier
4. 中间算子收到 Barrier 后保存状态快照
5. Sink 在支持 Checkpoint 的情况下保存或预提交写入状态
6. 所有算子确认完成后，Checkpoint 标记为成功
7. 作业失败时，从最近一次成功 Checkpoint 恢复
```

Checkpoint 适合自动故障恢复，不适合作为常规运维升级手段。作业升级、手动停止、分支实验、状态迁移等场景应优先使用 Savepoint。官方文档也区分了 Checkpoint 和 Savepoint：Checkpoint 面向自动恢复，Savepoint 面向手动触发的运维操作。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/learn-flink/fault_tolerance/?utm_source=chatgpt.com))

### Checkpoint 配置

Checkpoint 配置用于控制快照频率、超时时间、最小间隔、并发数量、一致性语义和外部化 Checkpoint 策略。Flink 官方文档说明，Checkpoint 默认关闭，需要通过 `enableCheckpointing(n)` 开启，其中 `n` 是毫秒级间隔；Checkpoint 模式可以选择 Exactly Once 或 At Least Once，生产中大多数场景优先选择 Exactly Once。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.19/docs/dev/datastream/fault-tolerance/checkpointing/?utm_source=chatgpt.com))

推荐配置如下：

```java
env.enableCheckpointing(60_000L, CheckpointingMode.EXACTLY_ONCE);

env.getCheckpointConfig().setCheckpointTimeout(300_000L);
env.getCheckpointConfig().setMinPauseBetweenCheckpoints(30_000L);
env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
env.getCheckpointConfig().setTolerableCheckpointFailureNumber(3);
env.getCheckpointConfig().setExternalizedCheckpointCleanup(
        CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION
);
```

完整配置工具类如下，适合放到公共模块中复用。

文件位置：`flink-common/src/main/java/io/github/atengk/flink/common/config/CheckpointConfigurer.java`

```java
package io.github.atengk.flink.common.config;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.Map;

/**
 * Flink Checkpoint 配置工具
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class CheckpointConfigurer {

    /**
     * 配置 Checkpoint
     *
     * @param env Flink 流执行环境
     * @param config 配置 Map
     */
    public static void configure(StreamExecutionEnvironment env, Map<String, Object> config) {
        boolean enabled = Convert.toBool(config.getOrDefault("checkpoint.enabled", true));
        if (!enabled) {
            log.warn("当前作业未开启Checkpoint，请确认是否为本地调试或一次性批处理任务");
            return;
        }

        long intervalMs = Convert.toLong(config.getOrDefault("checkpoint.interval-ms", 60_000L));
        long timeoutMs = Convert.toLong(config.getOrDefault("checkpoint.timeout-ms", 300_000L));
        long minPauseMs = Convert.toLong(config.getOrDefault("checkpoint.min-pause-ms", 30_000L));
        int tolerableFailureNumber = Convert.toInt(config.getOrDefault("checkpoint.tolerable-failure-number", 3));
        String mode = Convert.toStr(config.getOrDefault("checkpoint.mode", "EXACTLY_ONCE"));

        CheckpointingMode checkpointingMode = CheckpointingMode.valueOf(StrUtil.trim(mode).toUpperCase());

        env.enableCheckpointing(intervalMs, checkpointingMode);
        env.getCheckpointConfig().setCheckpointTimeout(timeoutMs);
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(minPauseMs);
        env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
        env.getCheckpointConfig().setTolerableCheckpointFailureNumber(tolerableFailureNumber);
        env.getCheckpointConfig().setExternalizedCheckpointCleanup(
                CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION
        );

        log.info("Checkpoint配置完成，模式：{}，间隔：{}ms，超时：{}ms，最小间隔：{}ms",
                checkpointingMode, intervalMs, timeoutMs, minPauseMs);
    }
}
```

Checkpoint 配置建议：

| 配置项                       | 建议                                          |
| ---------------------------- | --------------------------------------------- |
| `interval`                   | 常见 30 秒到 5 分钟，低延迟链路可更短         |
| `timeout`                    | 应大于正常 Checkpoint 耗时，常见 5 到 10 分钟 |
| `minPauseBetweenCheckpoints` | 防止 Checkpoint 过于密集                      |
| `maxConcurrentCheckpoints`   | 生产一般设置为 1，避免状态压力叠加            |
| `mode`                       | 默认使用 `EXACTLY_ONCE`                       |
| `externalized checkpoint`    | 生产建议保留，便于异常恢复                    |

Checkpoint 间隔不是越短越好。间隔过短会增加状态快照、网络传输和 Sink 提交压力；间隔过长会导致故障恢复时需要重放更多数据。需要结合状态大小、Kafka 积压、Sink 写入能力和业务恢复目标设置。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.19/docs/dev/datastream/fault-tolerance/checkpointing/?utm_source=chatgpt.com))

### Checkpoint 存储

Checkpoint Storage 用于决定 Checkpoint 快照保存到哪里。Flink 配置文档说明，`execution.checkpointing.storage` 支持 `jobmanager` 和 `filesystem`，其中 `filesystem` 需要配置 `execution.checkpointing.dir`，并且路径必须被所有 JobManager 和 TaskManager 访问到；生产环境建议使用高可用文件系统，例如 HDFS、S3、OSS、Ceph 等。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/deployment/config/?utm_source=chatgpt.com))

生产配置示例：

```yaml
# Checkpoint 存储类型，生产环境建议使用 filesystem
execution.checkpointing.storage: filesystem

# Checkpoint 持久化路径，所有 JobManager 和 TaskManager 必须可访问
execution.checkpointing.dir: hdfs:///flink/checkpoints/prod/user-behavior-job

# Savepoint 默认路径
execution.checkpointing.savepoint-dir: hdfs:///flink/savepoints/prod/user-behavior-job

# Checkpoint 模式
execution.checkpointing.mode: EXACTLY_ONCE

# 保留最近完成的 Checkpoint 数量
execution.checkpointing.num-retained: 3

# RocksDB 或支持增量快照的状态后端可开启增量 Checkpoint
execution.checkpointing.incremental: true
```

Java 配置示例：

```java
Configuration configuration = new Configuration();

// 使用文件系统保存 Checkpoint
configuration.set(CheckpointingOptions.CHECKPOINT_STORAGE, "filesystem");

// Checkpoint 目录，生产环境应使用可靠分布式存储
configuration.set(CheckpointingOptions.CHECKPOINTS_DIRECTORY, "hdfs:///flink/checkpoints/prod/user-behavior-job");

// Savepoint 默认目录
configuration.set(CheckpointingOptions.SAVEPOINT_DIRECTORY, "hdfs:///flink/savepoints/prod/user-behavior-job");

env.configure(configuration);
```

Checkpoint 存储建议：

1. 本地调试可以使用 `file:///tmp/...`。
2. 测试和生产环境应使用 HDFS、S3、OSS、Ceph 等可靠存储。
3. 不建议生产大状态作业使用 JobManager 内存保存 Checkpoint。
4. Checkpoint 路径必须按环境和作业隔离。
5. Checkpoint 目录需要定期清理，避免占满存储。
6. Checkpoint 与 Savepoint 目录应分开管理。

### Savepoint 机制

Savepoint 是用户手动触发的一致性状态快照，主要用于作业升级、停止后恢复、并行度调整、状态迁移、版本回滚和分支实验。Flink 官方 Savepoint 文档说明，Savepoint 是通过 Checkpoint 机制创建的流作业执行状态一致镜像，由稳定存储上的二进制状态文件和元数据文件组成，可用于停止恢复、Fork 或更新作业。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/ops/state/savepoints/?utm_source=chatgpt.com))

Savepoint 常用命令如下：

```bash
# 手动触发 Savepoint
flink savepoint <job-id> hdfs:///flink/savepoints/prod/user-behavior-job

# 停止作业并触发 Savepoint
flink stop --savepointPath hdfs:///flink/savepoints/prod/user-behavior-job <job-id>

# 从 Savepoint 恢复作业
flink run \
  -s hdfs:///flink/savepoints/prod/user-behavior-job/savepoint-xxxx \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  target/flink-job-user-behavior.jar \
  --env prod \
  --jobName user-behavior-job
```

命令说明：`flink savepoint` 用于对运行中的作业生成 Savepoint；`flink stop --savepointPath` 会先生成 Savepoint 再停止作业；`flink run -s` 用于从指定 Savepoint 恢复作业。

Savepoint 使用建议：

1. 作业升级前必须先创建 Savepoint。
2. 核心算子必须设置稳定 `uid()`，否则恢复状态时可能无法匹配。
3. 修改状态类型、状态名称、Key 类型前必须验证兼容性。
4. 并行度调整前应在测试环境从 Savepoint 恢复验证。
5. Savepoint 是运维资产，不应被自动清理脚本误删。
6. Savepoint 路径、版本、提交人、代码版本应记录在发布单中。

### 失败恢复策略

失败恢复策略用于决定某个 Task 失败后 Flink 如何恢复作业。常见故障包括用户代码异常、外部系统超时、网络抖动、TaskManager 退出、资源不足、Checkpoint 失败、Sink 写入失败等。Flink 的容错恢复依赖最近一次成功 Checkpoint，将状态和 Source 位点恢复到一致位置，然后重新执行后续数据。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/learn-flink/fault_tolerance/?utm_source=chatgpt.com))

故障恢复流程如下：

```text
Task 或外部调用失败
    |
    v
Flink 标记任务失败
    |
    v
根据重启策略决定是否重启
    |
    v
从最近一次成功 Checkpoint 恢复状态和 Source 位点
    |
    v
Source 从已保存位置重新读取
    |
    v
下游算子继续处理数据
```

常见故障与恢复关注点如下：

| 故障类型         | 恢复关注点                            |
| ---------------- | ------------------------------------- |
| Kafka 短暂不可用 | 重启策略、消费位点、积压恢复          |
| JDBC Sink 超时   | 重试、幂等写入、批量大小              |
| 用户代码 NPE     | 脏数据处理、字段校验、异常分流        |
| TaskManager OOM  | 状态大小、窗口大小、RocksDB、内存参数 |
| Checkpoint 超时  | 状态后端、存储性能、反压、Sink 提交   |
| 外部系统限流     | 重试间隔、降级、告警、背压传播        |

失败恢复策略建议：

1. 所有可预期数据异常应转为脏数据，不应让作业无限重启。
2. 外部系统短暂失败可以通过重启策略恢复。
3. 代码缺陷导致的确定性异常不会因为重启自动解决。
4. Sink 失败必须结合幂等、事务或补偿机制。
5. 故障恢复后应检查是否发生重复写入、消费积压和指标跳变。

### 重启策略

重启策略用于控制作业失败后的重试行为。Flink 官方任务恢复文档说明，固定延时重启策略会按指定次数重启作业，超过最大次数后作业失败；故障率重启策略会在指定时间间隔内限制最大失败次数，超过阈值后作业失败。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/zh/docs/ops/state/task_failure_recovery/?utm_source=chatgpt.com))

常见重启策略如下：

| 策略         | 说明                   | 适用场景             |
| ------------ | ---------------------- | -------------------- |
| fixed-delay  | 固定次数、固定间隔重启 | 外部系统短暂抖动     |
| failure-rate | 限制单位时间内失败次数 | 长期运行生产作业     |
| no-restart   | 失败后不重启           | 本地调试、一次性任务 |
| fallback     | 使用集群默认策略       | 平台统一管理         |

Java 配置示例：

```java
env.setRestartStrategy(RestartStrategies.fixedDelayRestart(
        3,
        Duration.ofSeconds(10)
));
```

`flink-conf.yaml` 配置示例：

```yaml
# 使用固定延时重启策略
restart-strategy.type: fixed-delay

# 最多重启 3 次
restart-strategy.fixed-delay.attempts: 3

# 每次重启间隔 10 秒
restart-strategy.fixed-delay.delay: 10 s
```

故障率重启策略示例：

```yaml
# 使用故障率重启策略
restart-strategy.type: failure-rate

# 每 10 分钟最多允许失败 5 次
restart-strategy.failure-rate.max-failures-per-interval: 5
restart-strategy.failure-rate.failure-rate-interval: 10 min

# 两次重启之间等待 30 秒
restart-strategy.failure-rate.delay: 30 s
```

重启策略建议：

1. 生产实时作业一般不使用 `no-restart`。
2. 外部系统偶发抖动适合 fixed-delay。
3. 长期运行作业适合 failure-rate，避免异常数据导致无限重启。
4. 重启次数不能掩盖代码缺陷，确定性异常应修复代码或数据。
5. 重启策略应与告警联动，不能只靠自动重启静默恢复。

### Exactly Once 实现

Exactly Once 分为 Flink 内部状态 Exactly Once 和端到端 Exactly Once。Flink 官方文档说明，Flink 的 Exactly Once 并不是每条事件物理上只处理一次，而是每条事件对 Flink 管理状态的影响恰好一次；端到端 Exactly Once 还要求 Source 可重放，并且 Sink 是事务型或幂等型。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/learn-flink/fault_tolerance/?utm_source=chatgpt.com))

端到端 Exactly Once 实现条件如下：

| 层次       | 要求                                                   |
| ---------- | ------------------------------------------------------ |
| Source     | 支持位点保存和重放，例如 Kafka Offset、CDC Binlog 位点 |
| Flink 状态 | 开启 Checkpoint，状态后端和 Checkpoint Storage 可靠    |
| Sink       | 支持事务提交、两阶段提交、幂等更新或唯一键去重         |
| 业务结果   | 有主键、版本号、幂等键或补偿校验机制                   |

Kafka Exactly Once Sink 示例：

```java
KafkaSink<String> kafkaSink = KafkaSink.<String>builder()
        .setBootstrapServers("kafka-prod-01:9092,kafka-prod-02:9092")
        .setRecordSerializer(
                KafkaRecordSerializationSchema.builder()
                        .setTopic("user_behavior_clean")
                        .setValueSerializationSchema(new SimpleStringSchema())
                        .build()
        )
        .setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
        .setTransactionalIdPrefix("user-behavior-job-")
        .build();

stream.sinkTo(kafkaSink)
        .name("kafka-exactly-once-sink")
        .uid("kafka-exactly-once-sink");
```

Flink 连接器保证文档列出 Kafka producer 可在事务生产者支持下实现 Exactly Once；文件 Sink 也可提供 Exactly Once，而 Elasticsearch、OpenSearch、Socket、标准输出等通常只提供 At Least Once 或更弱保证。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/connectors/datastream/guarantees/?utm_source=chatgpt.com))

Exactly Once 实践建议：

1. 开启 Checkpoint 是基础，但不是端到端 Exactly Once 的全部。
2. Kafka Sink 使用 Exactly Once 时必须配置事务 ID 前缀。
3. JDBC Sink 如果使用普通 Insert，失败恢复后可能重复写入。
4. 数据库 Sink 应优先使用 Upsert、唯一键、版本号或幂等 SQL。
5. Elasticsearch 应通过文档 ID 实现幂等覆盖，但交付语义通常仍按 At Least Once 评估。
6. Redis 写入适合最终一致性缓存场景，不应轻易承诺强 Exactly Once。

### At Least Once 场景

At Least Once 表示数据不会丢失，但可能重复处理或重复写入。Flink 官方文档说明，如果关闭 Exactly Once 所需的 Barrier Alignment，可以使用 At Least Once 模式降低部分延迟；一些 Sink 天然只能提供 At Least Once，需要通过幂等写入或业务去重降低重复影响。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/learn-flink/fault_tolerance/?utm_source=chatgpt.com))

At Least Once 常见场景如下：

| 场景               | 说明                                       |
| ------------------ | ------------------------------------------ |
| 普通 JDBC Insert   | 失败恢复后可能重复插入                     |
| Elasticsearch Sink | 官方保证通常按 At Least Once 评估          |
| Redis 缓存写入     | 可通过覆盖式写入实现最终一致               |
| Socket Sink        | 通常无法保证精确一次                       |
| 标准输出 Print     | 调试用，失败恢复可能重复输出               |
| 无事务外部 API     | 调用成功但状态未 Checkpoint 时可能重复调用 |

At Least Once 应对策略：

1. 使用业务唯一键去重。
2. 使用 Upsert 替代 Insert。
3. Sink 表设计主键或唯一约束。
4. 写入时携带事件 ID、版本号、窗口开始结束时间。
5. 对外部 API 调用增加请求幂等键。
6. 通过下游批处理或离线校验修复重复数据。

## 数据输出开发

本章节用于说明 Flink 作业常见 Sink 的开发方式，包括 Kafka、JDBC、Elasticsearch、Redis、文件和自定义 Sink。Flink DataStream Connector 文档说明，Flink 内置了一些基础 Sink，例如文件、标准输出、Socket；Kafka、Cassandra 等第三方系统通过 Connector 连接，其他系统也可通过自定义 Sink 或异步 I/O 接入。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/connectors/datastream/overview/?utm_source=chatgpt.com))

### Kafka Sink

Kafka Sink 适合将清洗结果、指标结果、脏数据、规则命中结果或中间宽表写回 Kafka，作为下游实时计算、数据同步或消息分发的输入。Flink 的连接器保证文档说明，Kafka producer 支持 At Least Once 或 Exactly Once；Exactly Once 依赖 Kafka 0.11+ 事务生产者。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/connectors/datastream/guarantees/?utm_source=chatgpt.com))

Kafka Sink 工厂示例：

下面的代码用于创建字符串 Kafka Sink，支持 At Least Once 和 Exactly Once 两种交付语义。

文件位置：`flink-connector/src/main/java/io/github/atengk/flink/connector/kafka/KafkaSinkFactory.java`

```java
package io.github.atengk.flink.connector.kafka;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;

/**
 * Kafka Sink 工厂
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class KafkaSinkFactory {

    /**
     * 创建字符串 Kafka Sink
     *
     * @param config Kafka Sink 配置
     * @return Kafka Sink
     */
    public static KafkaSink<String> createStringSink(KafkaSinkConfig config) {
        validate(config);

        DeliveryGuarantee deliveryGuarantee = DeliveryGuarantee.valueOf(
                StrUtil.blankToDefault(config.deliveryGuarantee(), "AT_LEAST_ONCE").toUpperCase()
        );

        KafkaSink.Builder<String> builder = KafkaSink.<String>builder()
                .setBootstrapServers(config.bootstrapServers())
                .setRecordSerializer(
                        KafkaRecordSerializationSchema.builder()
                                .setTopic(config.topic())
                                .setValueSerializationSchema(new SimpleStringSchema())
                                .build()
                )
                .setDeliveryGuarantee(deliveryGuarantee);

        if (deliveryGuarantee == DeliveryGuarantee.EXACTLY_ONCE) {
            builder.setTransactionalIdPrefix(StrUtil.blankToDefault(config.transactionalIdPrefix(), "flink-kafka-sink-"));
        }

        log.info("创建Kafka Sink，Topic：{}，交付语义：{}", config.topic(), deliveryGuarantee);

        return builder.build();
    }

    /**
     * 校验配置
     *
     * @param config Kafka Sink 配置
     */
    private static void validate(KafkaSinkConfig config) {
        if (StrUtil.isBlank(config.bootstrapServers())) {
            throw new IllegalArgumentException("Kafka bootstrapServers 不能为空");
        }
        if (StrUtil.isBlank(config.topic())) {
            throw new IllegalArgumentException("Kafka topic 不能为空");
        }
    }

    /**
     * Kafka Sink 配置
     *
     * @author Ateng
     * @since 2026-05-11
     */
    public record KafkaSinkConfig(
            String bootstrapServers,
            String topic,
            String deliveryGuarantee,
            String transactionalIdPrefix
    ) {
    }
}
```

使用示例：

```java
KafkaSink<String> kafkaSink = KafkaSinkFactory.createStringSink(
        new KafkaSinkFactory.KafkaSinkConfig(
                "kafka-prod-01:9092,kafka-prod-02:9092",
                "user_behavior_clean",
                "EXACTLY_ONCE",
                "user-behavior-job-"
        )
);

jsonStream.sinkTo(kafkaSink)
        .name("user-behavior-kafka-sink")
        .uid("user-behavior-kafka-sink");
```

Kafka Sink 建议：

1. 明细结果可写普通 Kafka Topic。
2. 聚合结果如果会更新，建议使用 Upsert Kafka 或携带业务主键。
3. Exactly Once 需要开启 Checkpoint。
4. 事务 ID 前缀必须按作业唯一规划。
5. Kafka Topic 分区数会影响下游消费并行度。
6. Sink 写入失败时要关注 Kafka Broker、权限、认证、序列化和事务超时。

### JDBC Sink

JDBC Sink 用于写入 MySQL、PostgreSQL、Oracle 等关系型数据库。Flink JDBC 文档说明，`JdbcSink.sink` 提供 At Least Once 保证；通过设计 Upsert SQL 或幂等更新，可以在效果上实现接近 Exactly Once 的结果。Flink JDBC 还提供 `exactlyOnceSink`，但需要数据库和 XA 事务等条件配合。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/connectors/datastream/jdbc/?utm_source=chatgpt.com))

Maven 依赖示例：

```xml
<!-- Flink JDBC Connector，用于写入关系型数据库 -->
<dependency>
    <groupId>org.apache.flink</groupId>
    <artifactId>flink-connector-jdbc</artifactId>
    <version>${flink.jdbc.connector.version}</version>
</dependency>

<!-- MySQL JDBC Driver，按目标数据库替换 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.4.0</version>
</dependency>
```

JDBC Sink 工厂示例：

下面的代码使用 MySQL Upsert SQL 写入事件统计结果，依赖唯一键保证幂等更新。

文件位置：`flink-connector/src/main/java/io/github/atengk/flink/connector/jdbc/EventCountJdbcSinkFactory.java`

```java
package io.github.atengk.flink.connector.jdbc;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.flink.model.EventCount;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

/**
 * 事件统计 JDBC Sink 工厂
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class EventCountJdbcSinkFactory {

    /**
     * 创建 MySQL Upsert Sink
     *
     * @param config JDBC 配置
     * @return SinkFunction
     */
    public static SinkFunction<EventCount> createMysqlUpsertSink(JdbcSinkConfig config) {
        validate(config);

        String sql = """
                INSERT INTO user_event_count(event_type, event_count, update_time)
                VALUES (?, ?, NOW())
                ON DUPLICATE KEY UPDATE
                    event_count = VALUES(event_count),
                    update_time = NOW()
                """;

        log.info("创建JDBC Sink，URL：{}，批量大小：{}", maskJdbcUrl(config.url()), config.batchSize());

        return JdbcSink.sink(
                sql,
                (statement, value) -> {
                    statement.setString(1, value.getEventType());
                    statement.setLong(2, value.getCount());
                },
                JdbcExecutionOptions.builder()
                        .withBatchSize(config.batchSize())
                        .withBatchIntervalMs(config.batchIntervalMs())
                        .withMaxRetries(config.maxRetries())
                        .build(),
                new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                        .withUrl(config.url())
                        .withDriverName(config.driverName())
                        .withUsername(config.username())
                        .withPassword(config.password())
                        .build()
        );
    }

    /**
     * 校验 JDBC 配置
     *
     * @param config JDBC 配置
     */
    private static void validate(JdbcSinkConfig config) {
        if (StrUtil.isBlank(config.url())) {
            throw new IllegalArgumentException("JDBC url 不能为空");
        }
        if (StrUtil.isBlank(config.driverName())) {
            throw new IllegalArgumentException("JDBC driverName 不能为空");
        }
        if (StrUtil.isBlank(config.username())) {
            throw new IllegalArgumentException("JDBC username 不能为空");
        }
    }

    /**
     * 脱敏 JDBC 地址
     *
     * @param url JDBC URL
     * @return 脱敏后地址
     */
    private static String maskJdbcUrl(String url) {
        return StrUtil.blankToDefault(url, "").replaceAll("password=[^&]+", "password=****");
    }

    /**
     * JDBC Sink 配置
     *
     * @author Ateng
     * @since 2026-05-11
     */
    public record JdbcSinkConfig(
            String url,
            String driverName,
            String username,
            String password,
            int batchSize,
            long batchIntervalMs,
            int maxRetries
    ) {
    }
}
```

建表示例：

```sql
CREATE TABLE user_event_count (
    event_type VARCHAR(64) NOT NULL COMMENT '事件类型',
    event_count BIGINT NOT NULL COMMENT '事件数量',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (event_type)
) COMMENT='用户行为事件统计表';
```

JDBC Sink 建议：

1. 普通 Insert 在失败恢复后可能重复写入。
2. 生产优先使用 Upsert、唯一键、版本号或幂等更新。
3. 批量大小不能过大，避免数据库压力和事务时间过长。
4. `maxRetries` 不能掩盖数据质量问题。
5. 数据库连接池、慢 SQL、锁等待和主键冲突需要监控。
6. 高吞吐明细写入不建议直接写 MySQL，应优先写 Kafka、Doris、StarRocks 或数据湖。

### Elasticsearch Sink

Elasticsearch Sink 适合写入检索型数据，例如日志检索、明细查询、用户行为检索、风控命中记录等。Flink Elasticsearch 文档说明，Elasticsearch Sink 用于向 Elasticsearch Index 发送文档操作；在开启 Checkpoint 时，Elasticsearch Sink 通过等待 Checkpoint 时刻的 BulkProcessor 待处理请求完成，提供 At Least Once 交付保证。需要注意，Flink 2.2 文档当前提示 Elasticsearch DataStream Connector 暂无对应 2.2 版本连接器，因此使用前必须确认 Flink 与 Connector 版本兼容。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-2.0/docs/connectors/datastream/elasticsearch/?utm_source=chatgpt.com))

Elasticsearch Sink 示例：

```java
ElasticsearchSink<String> elasticsearchSink = new Elasticsearch7SinkBuilder<String>()
        .setHosts(new HttpHost("elasticsearch-prod-01", 9200, "http"))
        .setEmitter((value, runtimeContext, requestIndexer) -> {
            IndexRequest request = Requests.indexRequest()
                    .index("user-behavior-log")
                    .id(IdUtil.fastSimpleUUID())
                    .source(JSONUtil.parseObj(value));
            requestIndexer.add(request);
        })
        .setBulkFlushMaxActions(1000)
        .setBulkFlushInterval(5000)
        .build();

jsonStream.sinkTo(elasticsearchSink)
        .name("elasticsearch-user-behavior-sink")
        .uid("elasticsearch-user-behavior-sink");
```

如果业务要求重复写入不产生重复文档，应使用业务主键生成 Elasticsearch 文档 ID：

```java
String documentId = StrUtil.format("{}_{}", event.getUserId(), event.getEventTime());
```

Elasticsearch Sink 建议：

1. 使用业务唯一键作为文档 ID，避免失败恢复后重复文档。
2. Bulk 参数要结合 ES 集群写入能力配置。
3. 写入失败要记录失败原因和数据摘要。
4. Elasticsearch 更适合检索，不适合强事务一致性结果表。
5. 聚合更新场景需要确认 Upsert 语义和主键设计。
6. 使用前必须确认 Connector 对当前 Flink 版本和 ES 版本的支持情况。

### Redis Sink

Redis Sink 常用于缓存实时指标、维表结果、风控命中状态、排行榜、去重标记等。当前 Flink 稳定版 DataStream Connector 总览没有将 Redis 列为 Flink Project Connector；历史版本曾提供 Redis Connector，Apache Bahir 和 StreamPark 也有封装方案。Apache StreamPark 文档明确说明，Apache Flink 官方未提供写入 Redis 数据的连接器，StreamPark 基于 Flink Connector Redis 做了封装。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/connectors/datastream/overview/?utm_source=chatgpt.com))

生产项目中，Redis 写入通常建议使用自定义 Sink，并通过幂等覆盖降低重复写入影响。下面示例使用 Jedis 写入 String 结果。

Maven 依赖示例：

```xml
<!-- Jedis Redis 客户端，用于自定义 Redis Sink -->
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>5.2.0</version>
</dependency>
```

Redis Sink 示例：

文件位置：`flink-connector/src/main/java/io/github/atengk/flink/connector/redis/EventCountRedisSink.java`

```java
package io.github.atengk.flink.connector.redis;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.flink.model.EventCount;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import redis.clients.jedis.JedisPooled;

/**
 * 事件统计 Redis Sink
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class EventCountRedisSink extends RichSinkFunction<EventCount> {

    private final RedisSinkConfig config;

    private transient JedisPooled jedis;

    /**
     * 创建 Redis Sink
     *
     * @param config Redis 配置
     */
    public EventCountRedisSink(RedisSinkConfig config) {
        this.config = config;
    }

    /**
     * 初始化 Redis 客户端
     *
     * @param parameters Flink 配置
     */
    @Override
    public void open(Configuration parameters) {
        if (StrUtil.isBlank(config.password())) {
            this.jedis = new JedisPooled(config.host(), config.port());
        } else {
            this.jedis = new JedisPooled(config.host(), config.port(), config.username(), config.password());
        }
        log.info("Redis Sink初始化完成，地址：{}:{}", config.host(), config.port());
    }

    /**
     * 写入 Redis
     *
     * @param value 事件统计结果
     */
    @Override
    public void invoke(EventCount value, Context context) {
        String key = StrUtil.format("flink:event-count:{}", value.getEventType());
        jedis.set(key, String.valueOf(value.getCount()));
        jedis.expire(key, config.ttlSeconds());
    }

    /**
     * 关闭 Redis 客户端
     */
    @Override
    public void close() {
        if (jedis != null) {
            jedis.close();
            log.info("Redis Sink已关闭");
        }
    }

    /**
     * Redis Sink 配置
     *
     * @author Ateng
     * @since 2026-05-11
     */
    public record RedisSinkConfig(
            String host,
            int port,
            String username,
            String password,
            long ttlSeconds
    ) {
    }
}
```

使用示例：

```java
eventCountStream
        .addSink(new EventCountRedisSink(
                new EventCountRedisSink.RedisSinkConfig(
                        "redis-prod-vip",
                        6379,
                        "",
                        "",
                        3600
                )
        ))
        .name("redis-event-count-sink")
        .uid("redis-event-count-sink");
```

Redis Sink 建议：

1. Redis 写入适合缓存和最终一致性结果。
2. 使用 `SET`、`HSET` 这类覆盖式写入可以降低重复写入影响。
3. 不建议将 Redis 作为强一致事务结果库。
4. 批量写入建议使用 Pipeline，但要控制批次大小。
5. Redis 密码不能打印到日志。
6. Redis 写入失败应区分网络异常、认证异常、超时和限流。

### 文件 Sink

文件 Sink 适合写入历史明细、脏数据、离线补数结果、审计日志和数据湖落盘。Flink 连接器保证文档说明，File Sink 可以提供 Exactly Once；FileSystem Connector 支持批和流场景，并可写入 Flink 支持的文件系统。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/connectors/datastream/guarantees/?utm_source=chatgpt.com))

文件 Sink 示例：

```java
FileSink<String> fileSink = FileSink
        .forRowFormat(
                new Path("hdfs:///flink/output/user-behavior-clean"),
                new SimpleStringEncoder<String>("UTF-8")
        )
        .withRollingPolicy(
                DefaultRollingPolicy.builder()
                        .withRolloverInterval(Duration.ofMinutes(15))
                        .withInactivityInterval(Duration.ofMinutes(5))
                        .withMaxPartSize(MemorySize.ofMebiBytes(128))
                        .build()
        )
        .build();

jsonStream.sinkTo(fileSink)
        .name("file-user-behavior-sink")
        .uid("file-user-behavior-sink");
```

文件 Sink 建议：

1. 脏数据、审计日志和历史回放结果适合写文件。
2. 流式写文件必须配置合理 RollingPolicy。
3. 小文件过多会影响 HDFS、对象存储和下游 Hive 查询。
4. 输出路径应按环境、作业、日期和业务分区隔离。
5. 文件 Sink Exactly Once 依赖 Checkpoint 提交机制。
6. 生产输出路径不能与测试环境共用。

### 自定义 Sink

自定义 Sink 用于接入官方 Connector 不支持的系统，例如内部 HTTP API、企业消息平台、专有存储、老系统接口、第三方 SaaS 等。Flink Connector 总览也说明，除了内置 Connector，还可以通过其他方式连接外部系统，包括异步 I/O 和自定义逻辑。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/connectors/datastream/overview/?utm_source=chatgpt.com))

下面示例实现一个简单 HTTP Sink，适合低吞吐告警、通知、审计类场景。高吞吐外部调用建议使用 Async I/O 或批量缓冲 Sink。

Maven 依赖示例：

```xml
<!-- Hutool HTTP 工具，用于简单 HTTP Sink 调用 -->
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>${hutool.version}</version>
</dependency>
```

自定义 HTTP Sink 示例：

文件位置：`flink-connector/src/main/java/io/github/atengk/flink/connector/http/HttpJsonSink.java`

```java
package io.github.atengk.flink.connector.http;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

/**
 * HTTP JSON Sink
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class HttpJsonSink extends RichSinkFunction<String> {

    private final String url;
    private final int timeoutMs;

    /**
     * 创建 HTTP JSON Sink
     *
     * @param url 请求地址
     * @param timeoutMs 超时时间毫秒数
     */
    public HttpJsonSink(String url, int timeoutMs) {
        if (StrUtil.isBlank(url)) {
            throw new IllegalArgumentException("HTTP Sink URL不能为空");
        }
        this.url = url;
        this.timeoutMs = timeoutMs;
    }

    /**
     * 发送 JSON 数据
     *
     * @param value JSON 字符串
     */
    @Override
    public void invoke(String value, Context context) {
        try {
            String response = HttpRequest.post(url)
                    .timeout(timeoutMs)
                    .contentType("application/json")
                    .body(value)
                    .execute()
                    .body();

            log.debug("HTTP Sink写入完成，响应摘要：{}", StrUtil.sub(response, 0, 128));
        } catch (Exception e) {
            log.error("HTTP Sink写入失败，原因：{}", ExceptionUtil.getSimpleMessage(e), e);
            throw e;
        }
    }
}
```

自定义 Sink 建议：

1. 必须明确失败时是否抛异常触发 Flink 重启。
2. 外部调用应设置超时，禁止无限等待。
3. 高吞吐场景避免逐条同步 HTTP 请求。
4. 批量写入要结合 Checkpoint 处理未提交数据。
5. 如果需要 Exactly Once，应实现事务、幂等或两阶段提交。
6. 自定义 Sink 必须有压测和异常恢复测试。

### 幂等写入

幂等写入是 At Least Once 场景下最常用的数据一致性手段。它的目标是同一条业务数据重复写入多次，最终结果仍然正确。Flink 官方容错文档说明，端到端 Exactly Once 可以通过事务型或幂等型 Sink 实现；JDBC 文档也说明，普通 JDBC Sink 是 At Least Once，但通过 Upsert SQL 或幂等更新可以在效果上达到 Exactly Once。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/learn-flink/fault_tolerance/?utm_source=chatgpt.com))

常见幂等设计如下：

| Sink            | 幂等方式                                           |
| --------------- | -------------------------------------------------- |
| MySQL           | 主键、唯一键、`INSERT ... ON DUPLICATE KEY UPDATE` |
| PostgreSQL      | `INSERT ... ON CONFLICT DO UPDATE`                 |
| Kafka           | Keyed Message、Upsert Kafka、业务事件 ID           |
| Elasticsearch   | 使用业务 ID 作为文档 ID                            |
| Redis           | `SET`、`HSET` 覆盖式写入                           |
| Doris/StarRocks | Unique Key、Primary Key、Merge-on-Write            |
| 文件            | 按批次、窗口、分区写入并避免重复提交               |

幂等键设计建议：

```text
明细数据：eventId
订单数据：orderId
用户窗口指标：userId + windowStart + windowEnd
事件类型指标：eventType + windowStart + windowEnd
同步宽表：业务主键 + 版本号
```

MySQL 幂等写入示例：

```sql
INSERT INTO user_event_count(event_type, window_start, window_end, event_count, update_time)
VALUES (?, ?, ?, ?, NOW())
ON DUPLICATE KEY UPDATE
    event_count = VALUES(event_count),
    update_time = NOW();
```

幂等写入建议：

1. 每个 Sink 结果都应明确幂等键。
2. 窗口结果应包含窗口开始时间和结束时间。
3. 事件明细应优先使用全局唯一事件 ID。
4. 重复写入后不应导致累计字段被重复累加。
5. 幂等逻辑必须通过故障恢复测试验证。
6. 幂等不是无成本的，需要考虑索引、锁、写放大和下游性能。

### 两阶段提交

两阶段提交用于实现事务型 Sink 的 Exactly Once。基本思想是：Flink 在处理数据时先将结果写入预提交事务，Checkpoint 成功后再正式提交事务；如果作业失败，则恢复后根据 Checkpoint 状态提交或回滚事务。Flink 官方容错文档说明，端到端 Exactly Once 要求 Sink 事务型或幂等型；Kafka Exactly Once 就依赖事务生产者。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/learn-flink/fault_tolerance/?utm_source=chatgpt.com))

两阶段提交流程如下：

```text
开始事务
   |
   v
写入数据到事务缓冲区或临时区域
   |
   v
Checkpoint 触发
   |
   v
预提交事务
   |
   v
Checkpoint 全局完成
   |
   v
正式提交事务
   |
   v
失败恢复时提交或回滚未完成事务
```

适合两阶段提交的 Sink：

| Sink             | 说明                                          |
| ---------------- | --------------------------------------------- |
| Kafka            | 事务生产者支持 Exactly Once                   |
| 支持 XA 的数据库 | 可通过 XA 事务实现，但复杂度高                |
| 文件系统         | 通过临时文件和提交文件实现一致性              |
| 自定义事务系统   | 需要外部系统支持 begin/preCommit/commit/abort |
| 数据湖表         | 依赖表格式提交协议                            |

两阶段提交建议：

1. 优先使用官方 Connector 已实现的事务能力。
2. 自定义两阶段提交 Sink 要谨慎，测试成本高。
3. 事务超时时间必须大于 Checkpoint 间隔和恢复时间。
4. 预提交数据不能被下游提前读取。
5. 事务 ID 必须唯一且可恢复。
6. 对于不支持事务的系统，优先采用幂等写入。

### 输出失败处理

输出失败处理用于保障 Sink 写入异常时作业行为可控。Sink 失败可能来自网络异常、认证失败、权限不足、下游限流、数据格式错误、主键冲突、事务超时、批量过大、下游磁盘满等。不同失败类型应采用不同策略，不能简单地全部重试或全部丢弃。

常见失败类型如下：

| 失败类型     | 处理建议                                |
| ------------ | --------------------------------------- |
| 网络超时     | 重试、退避、告警                        |
| 下游限流     | 降低并行度、减小批量、增加缓冲          |
| 权限错误     | 立即失败并告警                          |
| 数据格式错误 | 输出脏数据，不应反复重启                |
| 主键冲突     | 检查幂等 SQL 或业务主键设计             |
| 事务超时     | 调整事务超时、Checkpoint 间隔和恢复策略 |
| 下游不可用   | 触发重启策略或降级策略                  |
| 批量过大     | 减小 batch size 和 flush interval       |

输出失败处理示例：

```java
try {
    // 执行外部系统写入
    writeToExternalSystem(value);
} catch (DataFormatException e) {
    // 数据问题应进入脏数据链路，不建议无限重启
    log.warn("Sink数据格式异常，数据摘要：{}，原因：{}", value.summary(), e.getMessage());
    outputDirty(value, e);
} catch (TransientExternalException e) {
    // 外部系统短暂异常可以抛出，由Flink重启恢复
    log.error("外部系统短暂异常，触发作业恢复，原因：{}", e.getMessage(), e);
    throw e;
} catch (Exception e) {
    // 未知异常应保守处理，避免静默丢数据
    log.error("Sink写入发生未知异常，触发作业恢复，原因：{}", e.getMessage(), e);
    throw e;
}
```

输出失败处理建议：

1. 数据错误和系统错误要区分处理。
2. 数据错误进入脏数据，不应导致作业无限重启。
3. 系统错误可以抛出异常，让 Flink 基于 Checkpoint 恢复。
4. Sink 重试要有最大次数、间隔和告警。
5. 下游不可用时应保留失败上下文。
6. 对外部系统写入要设置超时。
7. 输出失败后必须验证恢复是否重复写入或漏写。


## Connector 集成

本章节用于规范 Flink 项目中常见 Connector 的选型、依赖管理、DDL 编写、DataStream 使用方式和生产注意事项。Connector 是 Flink 与外部系统交互的边界层，常见问题包括版本不兼容、依赖冲突、运行包缺失、序列化格式不一致、认证配置错误、Sink 语义误判和下游写入压力过大。

Flink 官方文档说明，Connector 和 Format 通常需要额外添加依赖；社区发布的连接器通常有两类制品：`flink-connector-<NAME>` 是较薄的 Jar，通常不包含第三方依赖；`flink-sql-connector-<NAME>` 是适合 SQL Client 使用的 Uber Jar，通常包含所需第三方依赖。实际项目中，DataStream 作业一般通过 Maven 打包 Connector，SQL Client 或集群级 SQL 作业通常将 SQL Connector Jar 放入 `$FLINK_HOME/lib`。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/configuration/connector/?utm_source=chatgpt.com))

Connector 统一治理建议如下：

| 事项       | 建议                                                         |
| ---------- | ------------------------------------------------------------ |
| 版本管理   | 所有 Connector 版本集中放在根 `pom.xml` 的 `properties` 或 `dependencyManagement` 中 |
| 兼容性     | 先确认 Flink 版本、Connector 版本、外部系统版本、JDK 版本是否匹配 |
| 打包方式   | DataStream 作业通常打入业务 Fat Jar；SQL Client 通常放入 `$FLINK_HOME/lib` |
| 一致性语义 | 每个 Source/Sink 必须明确 At Least Once、Exactly Once 或幂等语义 |
| 认证配置   | Kafka、Hive、HBase、Doris、StarRocks、Iceberg 等都需要独立安全配置 |
| 运行验证   | 每新增一个 Connector，都要做本地、测试环境、Checkpoint、失败恢复验证 |

### Kafka Connector

Kafka Connector 用于从 Kafka Topic 读取实时数据，或将清洗结果、指标结果、宽表结果、脏数据和告警事件写回 Kafka。Kafka 是 Flink 实时计算中最常见的 Source/Sink 之一，适合日志、埋点、订单事件、设备事件和跨系统消息传递。Flink Kafka Connector 文档说明，Flink 提供 Kafka Connector 读写 Kafka Topic，并支持 Exactly Once 语义；但对于 Flink 2.2，官方文档当前提示尚无对应 Kafka Connector，因此使用 Flink 2.2 时必须先确认 Connector 是否已发布或选择兼容的 Flink 版本线。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-2.2/docs/connectors/datastream/kafka/?utm_source=chatgpt.com))

Maven 依赖示例需要按实际 Flink 版本确认。以下以 Flink 1.19 版本线为例，官方文档中 Kafka Connector 示例版本为 `3.3.0-1.19`。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.19/docs/connectors/datastream/kafka/?utm_source=chatgpt.com))

```xml
<!-- Kafka Connector，用于 Kafka Source 和 Kafka Sink，版本必须匹配 Flink 主版本 -->
<dependency>
    <groupId>org.apache.flink</groupId>
    <artifactId>flink-connector-kafka</artifactId>
    <version>${flink.kafka.connector.version}</version>
</dependency>
```

Kafka Source DDL 示例：

```sql
CREATE TEMPORARY TABLE user_behavior_kafka_source (
    user_id STRING,
    item_id STRING,
    category_id STRING,
    event_type STRING,
    event_ts BIGINT,
    event_time AS TO_TIMESTAMP_LTZ(event_ts, 3),
    WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND
) WITH (
    'connector' = 'kafka',
    'topic' = 'user_behavior_log',
    'properties.bootstrap.servers' = 'kafka-prod-01:9092,kafka-prod-02:9092,kafka-prod-03:9092',
    'properties.group.id' = 'user-behavior-sql-job',
    'scan.startup.mode' = 'latest-offset',
    'format' = 'json',
    'json.ignore-parse-errors' = 'true',
    'json.fail-on-missing-field' = 'false'
);
```

Kafka Sink DDL 示例：

```sql
CREATE TEMPORARY TABLE user_behavior_kafka_sink (
    user_id STRING,
    item_id STRING,
    category_id STRING,
    event_type STRING,
    event_time TIMESTAMP_LTZ(3)
) WITH (
    'connector' = 'kafka',
    'topic' = 'user_behavior_clean',
    'properties.bootstrap.servers' = 'kafka-prod-01:9092,kafka-prod-02:9092,kafka-prod-03:9092',
    'format' = 'json',
    'json.timestamp-format.standard' = 'ISO-8601'
);
```

DataStream 中使用 Kafka Source/Sink 时，建议使用新 Source/Sink API，并显式设置 `WatermarkStrategy`、`DeliveryGuarantee` 和算子 `uid()`：

```java
KafkaSource<String> kafkaSource = KafkaSource.<String>builder()
        .setBootstrapServers("kafka-prod-01:9092,kafka-prod-02:9092")
        .setTopics("user_behavior_log")
        .setGroupId("user-behavior-job")
        .setStartingOffsets(OffsetsInitializer.latest())
        .setValueOnlyDeserializer(new SimpleStringSchema())
        .build();

KafkaSink<String> kafkaSink = KafkaSink.<String>builder()
        .setBootstrapServers("kafka-prod-01:9092,kafka-prod-02:9092")
        .setRecordSerializer(
                KafkaRecordSerializationSchema.builder()
                        .setTopic("user_behavior_clean")
                        .setValueSerializationSchema(new SimpleStringSchema())
                        .build()
        )
        .setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
        .setTransactionalIdPrefix("user-behavior-job-")
        .build();
```

Kafka Connector 使用建议：

1. Kafka Source 并行度不应盲目超过 Topic 分区数。
2. 生产恢复应依赖 Checkpoint 或 Savepoint，不应随意修改 `scan.startup.mode`。
3. Exactly Once Kafka Sink 必须开启 Checkpoint，并设置稳定、唯一的事务 ID 前缀。
4. Kafka 消息应有明确 Key，聚合或 Upsert 结果尤其需要业务主键。
5. Kafka 认证配置、SASL、SSL、Kerberos 参数不得硬编码。
6. Flink 2.x 项目必须先确认 Kafka Connector 是否已支持目标 Flink 版本。

### JDBC Connector

JDBC Connector 用于写入关系型数据库，例如 MySQL、PostgreSQL、Oracle、SQL Server 等。Flink 官方 JDBC 文档说明，JDBC Connector 提供写入 JDBC 数据库的 Sink；`JdbcSink.sink` 默认提供 At Least Once 保证，但通过 Upsert SQL 或幂等更新可以在结果层面实现接近 Exactly Once 的效果。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/connectors/datastream/jdbc/?utm_source=chatgpt.com))

Maven 依赖示例：

```xml
<!-- Flink JDBC Connector，用于写入 JDBC 数据库 -->
<dependency>
    <groupId>org.apache.flink</groupId>
    <artifactId>flink-connector-jdbc</artifactId>
    <version>${flink.jdbc.connector.version}</version>
</dependency>

<!-- MySQL Driver，按实际数据库替换为 PostgreSQL、Oracle 或 SQL Server Driver -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>${mysql.driver.version}</version>
</dependency>
```

JDBC Sink DDL 示例：

```sql
CREATE TEMPORARY TABLE user_event_count_jdbc_sink (
    event_type STRING,
    window_start TIMESTAMP(3),
    window_end TIMESTAMP(3),
    event_count BIGINT,
    PRIMARY KEY (event_type, window_start, window_end) NOT ENFORCED
) WITH (
    'connector' = 'jdbc',
    'url' = 'jdbc:mysql://mysql-prod-vip:3306/ateng_report?useSSL=false&serverTimezone=Asia/Shanghai',
    'table-name' = 'user_event_count',
    'username' = 'flink_writer',
    'password' = '${MYSQL_WRITER_PASSWORD}',
    'sink.buffer-flush.max-rows' = '1000',
    'sink.buffer-flush.interval' = '5s',
    'sink.max-retries' = '3'
);
```

JDBC 表结构示例：

```sql
CREATE TABLE user_event_count (
    event_type VARCHAR(64) NOT NULL COMMENT '事件类型',
    window_start DATETIME NOT NULL COMMENT '窗口开始时间',
    window_end DATETIME NOT NULL COMMENT '窗口结束时间',
    event_count BIGINT NOT NULL COMMENT '事件数量',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (event_type, window_start, window_end)
) COMMENT='用户行为事件窗口统计表';
```

JDBC Connector 使用建议：

1. 普通 Insert 不具备天然 Exactly Once，失败恢复后可能重复写入。
2. 生产环境优先使用主键、唯一键、Upsert SQL 或版本号字段实现幂等。
3. 不建议将高吞吐明细流直接写入 MySQL，应优先写 Kafka、Doris、StarRocks、Iceberg 或 Hudi。
4. `sink.buffer-flush.max-rows` 和 `sink.buffer-flush.interval` 需要结合数据库承载能力压测。
5. 数据库密码应通过 Secret、环境变量或配置中心注入。
6. 需要关注慢 SQL、死锁、连接数、主键冲突和数据库限流。

### Flink CDC Connector

Flink CDC Connector 用于读取数据库变更数据，包括初始快照和增量日志。它适合实时同步、实时宽表、数据库变更流、维表变更流和湖仓入湖场景。Flink CDC 3.6 文档说明，MySQL CDC Connector 可以读取 MySQL 快照数据和增量 Binlog，支持 MySQL 5.7、8.0.x、8.4+，以及 RDS MySQL、PolarDB MySQL、Aurora MySQL、MariaDB 等数据库；文档还说明 MySQL CDC Connector 在快照阶段和 Binlog 阶段都支持 Exactly Once 读取。([Apache Nightlies](https://nightlies.apache.org/flink/flink-cdc-docs-release-3.6/docs/connectors/flink-sources/mysql-cdc/?utm_source=chatgpt.com))

Maven 依赖示例：

```xml
<!-- Flink CDC MySQL Connector，用于读取 MySQL 快照和 Binlog 增量数据 -->
<dependency>
    <groupId>org.apache.flink</groupId>
    <artifactId>flink-connector-mysql-cdc</artifactId>
    <version>3.6.0</version>
</dependency>

<!-- MySQL Driver，Flink CDC 文档说明需要手动提供 MySQL JDBC Driver -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.27</version>
</dependency>
```

MySQL CDC Source DDL 示例：

```sql
CREATE TEMPORARY TABLE user_behavior_mysql_cdc_source (
    id BIGINT,
    user_id STRING,
    item_id STRING,
    category_id STRING,
    event_type STRING,
    event_time TIMESTAMP(3),
    PRIMARY KEY (id) NOT ENFORCED
) WITH (
    'connector' = 'mysql-cdc',
    'hostname' = 'mysql-prod-vip',
    'port' = '3306',
    'username' = 'flink_cdc',
    'password' = '${MYSQL_CDC_PASSWORD}',
    'database-name' = 'ateng_biz',
    'table-name' = 'user_behavior',
    'server-time-zone' = 'Asia/Shanghai',
    'server-id' = '5600-5799',
    'scan.startup.mode' = 'initial'
);
```

CDC Connector 使用建议：

1. MySQL 用户需要 `SELECT`、`SHOW DATABASES`、`REPLICATION SLAVE`、`REPLICATION CLIENT` 等权限，具体以 CDC 文档和数据库安全策略为准。([Apache Nightlies](https://nightlies.apache.org/flink/flink-cdc-docs-release-3.6/docs/connectors/flink-sources/mysql-cdc/?utm_source=chatgpt.com))
2. 每个 CDC 作业必须规划唯一 `server-id`，并行读取时需要足够范围。
3. 源表最好有主键，便于快照切分和下游 Upsert。
4. 初次启动常用 `initial`，生产恢复应依赖 Checkpoint 或 Savepoint。
5. Binlog 保留时间必须覆盖作业最大停机时间，避免恢复时位点已被清理。
6. DDL 变更要评估 CDC、Flink Schema、Sink 表结构和下游兼容性。

### Elasticsearch Connector

Elasticsearch Connector 用于将数据写入 Elasticsearch 索引，适合日志检索、风控命中明细、用户行为检索、审计查询和简单明细检索。Flink Elasticsearch SQL Connector 文档说明，它支持 Batch Sink 和 Streaming Sink，可以按 Append 或 Upsert 模式写入；如果 DDL 中定义了主键，Connector 可以用 Upsert 模式处理 UPDATE/DELETE 消息，否则只能按 Append 模式处理 INSERT 消息。当前 Flink 2.2 文档提示 Elasticsearch 连接器尚无对应 2.2 版本，因此使用前必须确认目标 Flink 版本是否已有可用 Connector。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/connectors/table/elasticsearch/?utm_source=chatgpt.com))

Elasticsearch Sink DDL 示例：

```sql
CREATE TEMPORARY TABLE user_behavior_es_sink (
    event_id STRING,
    user_id STRING,
    item_id STRING,
    event_type STRING,
    event_time TIMESTAMP_LTZ(3),
    PRIMARY KEY (event_id) NOT ENFORCED
) WITH (
    'connector' = 'elasticsearch-7',
    'hosts' = 'http://es-prod-01:9200;http://es-prod-02:9200',
    'index' = 'user-behavior-${event_type}',
    'sink.bulk-flush.max-actions' = '1000',
    'sink.bulk-flush.interval' = '5s',
    'sink.delivery-guarantee' = 'AT_LEAST_ONCE'
);
```

DataStream 写入示例：

```java
ElasticsearchSink<String> elasticsearchSink = new Elasticsearch7SinkBuilder<String>()
        .setHosts(new HttpHost("es-prod-01", 9200, "http"))
        .setEmitter((value, runtimeContext, requestIndexer) -> {
            String documentId = IdUtil.fastSimpleUUID();
            IndexRequest request = Requests.indexRequest()
                    .index("user-behavior-log")
                    .id(documentId)
                    .source(JSONUtil.parseObj(value));
            requestIndexer.add(request);
        })
        .setBulkFlushMaxActions(1000)
        .setBulkFlushInterval(5000)
        .build();
```

Elasticsearch Connector 使用建议：

1. 有更新语义的结果必须声明主键，并使用业务唯一 ID 作为文档 ID。
2. 明细日志索引建议按日期或业务类型分区，例如 `user-behavior-2026.05.11`。
3. Bulk 参数要结合 ES 集群写入能力设置。
4. Elasticsearch 更适合检索，不适合作为强事务指标结果库。
5. 生产环境需要监控 Bulk 失败次数、拒绝写入、索引膨胀和字段映射冲突。
6. 使用 Flink 2.x 时要先确认 Elasticsearch Connector 版本是否可用。

### Hive Connector

Hive Connector 用于集成 Hive Metastore 和 Hive 表。Flink 官方 Hive 文档说明，Flink 与 Hive 有两类集成方式：一是通过 `HiveCatalog` 使用 Hive Metastore 作为持久化 Catalog，保存 Flink 元数据并复用表定义；二是将 Flink 作为读写 Hive 表的执行引擎。Flink Hive 集成被设计为与已有 Hive 安装兼容，不需要修改 Hive Metastore 或表的数据位置与分区方式。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/connectors/table/hive/overview/?utm_source=chatgpt.com))

HiveCatalog 初始化示例：

```java
String catalogName = "hive_catalog";
String defaultDatabase = "default";
String hiveConfDir = "/etc/hive/conf";

HiveCatalog hiveCatalog = new HiveCatalog(catalogName, defaultDatabase, hiveConfDir);

tableEnv.registerCatalog(catalogName, hiveCatalog);
tableEnv.useCatalog(catalogName);
tableEnv.useDatabase(defaultDatabase);
```

Hive 表写入 SQL 示例：

```sql
CREATE TABLE IF NOT EXISTS hive_catalog.dwd.user_behavior_clean (
    user_id STRING COMMENT '用户ID',
    item_id STRING COMMENT '商品ID',
    event_type STRING COMMENT '事件类型',
    event_time TIMESTAMP(3) COMMENT '事件时间'
) PARTITIONED BY (
    dt STRING COMMENT '日期分区'
) STORED AS PARQUET;

INSERT INTO hive_catalog.dwd.user_behavior_clean
SELECT
    user_id,
    item_id,
    event_type,
    event_time,
    DATE_FORMAT(event_time, 'yyyy-MM-dd') AS dt
FROM user_behavior_source;
```

Hive Connector 使用建议：

1. Hive Connector 适合离线补数、湖仓落地、批流一体和历史数据查询。
2. HiveCatalog 依赖 Hive Metastore，必须保证 Flink 节点能访问 Metastore。
3. Hive 版本、Hadoop 版本、Flink 版本和依赖包需要严格匹配。
4. Hive 生产写入建议使用 Parquet 或 ORC，避免大量小文件。
5. 分区字段要统一，例如 `dt`、`hour`、`biz_type`。
6. Kerberos 环境需要配置 keytab、principal、krb5.conf 和 Hadoop/Hive 配置目录。

### HBase Connector

HBase Connector 常用于维表查询、宽表写入、设备状态存储、用户画像读取和低延迟 Key-Value 查询。Flink HBase SQL Connector 文档说明，HBase Connector 允许读写 HBase 集群；HBase 始终按 Upsert 模式与外部系统交换 Changelog 消息，并要求主键定义在 HBase rowkey 字段上，如果未声明主键，则默认将 rowkey 作为主键。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.19/docs/connectors/table/hbase/?utm_source=chatgpt.com))

HBase DDL 示例：

```sql
CREATE TEMPORARY TABLE user_profile_hbase (
    rowkey STRING,
    info ROW<
        user_name STRING,
        city STRING,
        age INT
    >,
    stat ROW<
        user_level STRING,
        last_active_time BIGINT
    >,
    PRIMARY KEY (rowkey) NOT ENFORCED
) WITH (
    'connector' = 'hbase-2.2',
    'table-name' = 'profile:user_profile',
    'zookeeper.quorum' = 'zk-prod-01:2181,zk-prod-02:2181,zk-prod-03:2181',
    'zookeeper.znode.parent' = '/hbase',
    'sink.buffer-flush.max-size' = '2mb',
    'sink.buffer-flush.max-rows' = '1000',
    'sink.buffer-flush.interval' = '1s'
);
```

维表 Lookup Join 示例：

```sql
SELECT
    e.user_id,
    e.event_type,
    p.info.city,
    p.stat.user_level
FROM user_behavior_source AS e
LEFT JOIN user_profile_hbase FOR SYSTEM_TIME AS OF e.proc_time AS p
ON e.user_id = p.rowkey;
```

HBase Connector 使用建议：

1. RowKey 设计决定查询性能和热点风险，不能简单使用递增 ID。
2. HBase 写入默认是 Upsert 语义，适合状态类、画像类、宽表类数据。
3. 维表查询需要控制缓存、超时和并发，避免压垮 HBase。
4. 字段族和列名要稳定，不建议频繁变更。
5. HBase 存储为字节数组，类型映射和空值编码需要按文档确认。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.19/docs/connectors/table/hbase/?utm_source=chatgpt.com))
6. 官方新版 Flink 对 HBase Connector 的支持情况要按目标 Flink 版本确认，不要直接复用旧版本文档依赖。

### Doris Connector

Doris Connector 用于 Flink 与 Apache Doris 集成，适合实时明细写入、聚合指标写入、维表 Lookup Join、OLAP 分析查询和 CDC 入仓。Apache Doris 文档说明，Flink Doris Connector 可以从 Doris 读取数据、向 Doris 写入数据、执行 Lookup Join，并集成 Flink CDC 实现整库同步；写入 Doris 时，Connector 会在 Flink 中批量聚合数据，然后通过 Stream Load 批量导入 Doris。([Apache Doris](https://doris.apache.org/docs/3.x/ecosystem/flink-doris-connector/?utm_source=chatgpt.com))

Doris Sink DDL 示例：

```sql
CREATE TEMPORARY TABLE user_event_count_doris_sink (
    event_type STRING,
    window_start TIMESTAMP(3),
    window_end TIMESTAMP(3),
    event_count BIGINT
) WITH (
    'connector' = 'doris',
    'fenodes' = 'doris-fe-01:8030,doris-fe-02:8030',
    'table.identifier' = 'ateng_report.user_event_count',
    'username' = 'flink_writer',
    'password' = '${DORIS_PASSWORD}',
    'sink.label-prefix' = 'user-event-count',
    'sink.properties.format' = 'json',
    'sink.properties.read_json_by_line' = 'true',
    'sink.buffer-count' = '3',
    'sink.buffer-size' = '1048576',
    'sink.enable-2pc' = 'true'
);
```

Doris Connector 使用建议：

1. Doris 适合高吞吐实时写入和 OLAP 查询，不建议用普通 JDBC 写大规模明细。
2. Unique Key 或 Aggregate Key 表需要结合业务主键和更新语义设计。
3. `sink.label-prefix` 必须按作业唯一规划，避免事务或导入 Label 冲突。
4. Stream Load 失败要检查 FE/BE 网络、权限、字段映射、JSON 格式和导入错误行。
5. Lookup Join 场景需要评估 Doris 查询压力和缓存策略。
6. 使用 CDC 整库同步时，需要额外评估 DDL 同步、Schema 演进和目标表自动创建策略。

### StarRocks Connector

StarRocks Connector 用于 Flink 与 StarRocks 集成，适合实时数据写入、OLAP 分析、维表读取和 CDC 入仓。StarRocks 文档说明，StarRocks 提供自研 Flink Connector，支持 DataStream API、Table API & SQL、Python API，写入时通过 Stream Load 将 Flink 中累积的数据批量导入 StarRocks；文档还说明该 Connector 相比 Flink JDBC Connector 有更高、更稳定的写入性能。([StarRocks 文档](https://docs.starrocks.io/docs/loading/Flink-connector-starrocks/?utm_source=chatgpt.com))

StarRocks Sink DDL 示例：

```sql
CREATE TEMPORARY TABLE user_event_count_starrocks_sink (
    event_type STRING,
    window_start TIMESTAMP(3),
    window_end TIMESTAMP(3),
    event_count BIGINT,
    PRIMARY KEY (event_type, window_start, window_end) NOT ENFORCED
) WITH (
    'connector' = 'starrocks',
    'jdbc-url' = 'jdbc:mysql://starrocks-fe-01:9030,starrocks-fe-02:9030',
    'load-url' = 'starrocks-fe-01:8030;starrocks-fe-02:8030',
    'database-name' = 'ateng_report',
    'table-name' = 'user_event_count',
    'username' = 'flink_writer',
    'password' = '${STARROCKS_PASSWORD}',
    'sink.properties.format' = 'json',
    'sink.properties.strip_outer_array' = 'true',
    'sink.buffer-flush.max-rows' = '100000',
    'sink.buffer-flush.interval-ms' = '5000'
);
```

StarRocks 读取 DDL 示例：

```sql
CREATE TEMPORARY TABLE user_profile_starrocks_source (
    id BIGINT,
    user_name STRING,
    city STRING,
    user_level STRING
) WITH (
    'connector' = 'starrocks',
    'scan-url' = 'starrocks-fe-01:8030,starrocks-fe-02:8030',
    'jdbc-url' = 'jdbc:mysql://starrocks-fe-01:9030',
    'username' = 'flink_reader',
    'password' = '${STARROCKS_PASSWORD}',
    'database-name' = 'dim',
    'table-name' = 'user_profile'
);
```

StarRocks 文档列出的当前 1.2.14 Connector 支持 Flink 1.16 到 1.20、StarRocks 2.1 及以上、Java 8；官方也提醒通常最新 Connector 只维护与最近三个 Flink 版本的兼容性，因此 Flink 2.x 项目使用前必须确认 StarRocks Connector 是否支持目标版本。([StarRocks 文档](https://docs.starrocks.io/releasenotes/flink_connector/?utm_source=chatgpt.com))

StarRocks Connector 使用建议：

1. Flink 写入 StarRocks 通常优先使用专用 Connector，不建议用 JDBC 承担高吞吐写入。
2. 需要 Exactly Once 时，应确认 StarRocks 版本和 Connector 版本满足事务 Stream Load 要求；StarRocks 文档建议 Exactly Once 场景使用 StarRocks 2.5+ 和 Flink Connector 1.2.4+。([StarRocks 文档](https://docs.starrocks.io/docs/loading/Flink-connector-starrocks/?utm_source=chatgpt.com))
3. Primary Key 表写入必须声明主键。
4. `jdbc-url` 用于 FE MySQL 协议端口，`load-url` 用于 FE HTTP 端口，网络必须打通。
5. 批量参数需要结合数据量、延迟要求和 StarRocks BE 写入能力压测。
6. 关注 `totalFlushRows`、`totalFlushFailedTimes`、过滤行数等写入指标。([StarRocks 文档](https://docs.starrocks.io/docs/loading/Flink-connector-starrocks/?utm_source=chatgpt.com))

### Iceberg Connector

Iceberg Connector 用于将 Flink 与 Apache Iceberg 表格式集成，适合湖仓表、流批一体、历史回放、增量读取、快照管理和多引擎共享。Iceberg 官方文档说明，Flink 可以在 SQL 中通过 `'connector'='iceberg'` 创建 Iceberg 表映射，并支持 Hive Catalog、Hadoop Catalog 和自定义 Catalog 等方式管理 Iceberg 表。([iceberg.apache.org](https://iceberg.apache.org/docs/latest/flink-connector/?utm_source=chatgpt.com))

Iceberg Hive Catalog DDL 示例：

```sql
CREATE CATALOG iceberg_hive WITH (
    'type' = 'iceberg',
    'catalog-type' = 'hive',
    'uri' = 'thrift://hive-metastore-prod:9083',
    'warehouse' = 'hdfs:///warehouse/iceberg'
);

USE CATALOG iceberg_hive;

CREATE DATABASE IF NOT EXISTS dwd;

CREATE TABLE IF NOT EXISTS dwd.user_behavior_clean (
    user_id STRING,
    item_id STRING,
    category_id STRING,
    event_type STRING,
    event_time TIMESTAMP_LTZ(3),
    dt STRING
) PARTITIONED BY (dt);
```

Iceberg Sink SQL 示例：

```sql
INSERT INTO iceberg_hive.dwd.user_behavior_clean
SELECT
    user_id,
    item_id,
    category_id,
    event_type,
    event_time,
    DATE_FORMAT(event_time, 'yyyy-MM-dd') AS dt
FROM user_behavior_kafka_source;
```

通过 `connector='iceberg'` 映射已有表的示例：

```sql
CREATE TEMPORARY TABLE user_behavior_iceberg_sink (
    user_id STRING,
    item_id STRING,
    event_type STRING,
    event_time TIMESTAMP_LTZ(3),
    dt STRING
) WITH (
    'connector' = 'iceberg',
    'catalog-name' = 'iceberg_hive',
    'catalog-type' = 'hive',
    'uri' = 'thrift://hive-metastore-prod:9083',
    'warehouse' = 'hdfs:///warehouse/iceberg',
    'catalog-database' = 'dwd',
    'catalog-table' = 'user_behavior_clean'
);
```

Iceberg Connector 使用建议：

1. Iceberg 适合湖仓明细、增量读取、快照回滚和多引擎共享。
2. Catalog 类型必须统一规划，常见为 Hive Catalog 或 Hadoop Catalog。
3. 分区字段不要过细，避免小文件和元数据膨胀。
4. 流式写入需要关注 Checkpoint、文件提交、小文件合并和 Compaction。
5. 表 Schema 变更要通过 Iceberg 元数据管理，不建议绕过 Catalog 直接改底层文件。
6. 与 Hive、Spark、Trino、Flink 多引擎共用时，要统一版本和表属性。

### Hudi Connector

Hudi Connector 用于 Flink 与 Apache Hudi 表格式集成，适合湖仓写入、CDC 入湖、近实时明细、增量查询、Upsert、Changelog、索引和压缩清理。Hudi 官方 Flink Quick Start 说明，Flink-Hudi 集成展示了 Flink 的流式能力；Hudi 支持 Flink 写入模式，包括 CDC Ingestion、Bulk Insert、Index Bootstrap、Changelog Mode 和 Append Mode，也支持 Streaming Query 和 Incremental Query。([hudi.apache.org](https://hudi.apache.org/docs/flink-quick-start-guide?utm_source=chatgpt.com))

Hudi Flink 支持矩阵需要严格确认。Hudi 1.1.x 文档显示，其支持 Flink 1.17.x、1.18.x、1.19.x、1.20.x 以及 2.0.x；Hudi 1.0.x 支持 Flink 1.14.x 到 1.20.x 等版本线。([hudi.apache.org](https://hudi.apache.org/docs/flink-quick-start-guide?utm_source=chatgpt.com))

Hudi Sink DDL 示例：

```sql
CREATE TEMPORARY TABLE user_behavior_hudi_sink (
    user_id STRING,
    item_id STRING,
    category_id STRING,
    event_type STRING,
    event_time TIMESTAMP(3),
    dt STRING,
    PRIMARY KEY (user_id, item_id, event_time) NOT ENFORCED
) WITH (
    'connector' = 'hudi',
    'path' = 'hdfs:///warehouse/hudi/dwd/user_behavior_clean',
    'table.type' = 'MERGE_ON_READ',
    'hoodie.datasource.write.recordkey.field' = 'user_id,item_id,event_time',
    'hoodie.datasource.write.partitionpath.field' = 'dt',
    'write.precombine.field' = 'event_time',
    'write.operation' = 'upsert',
    'compaction.async.enabled' = 'true'
);
```

Hudi 写入 SQL 示例：

```sql
INSERT INTO user_behavior_hudi_sink
SELECT
    user_id,
    item_id,
    category_id,
    event_type,
    CAST(event_time AS TIMESTAMP(3)) AS event_time,
    DATE_FORMAT(event_time, 'yyyy-MM-dd') AS dt
FROM user_behavior_kafka_source;
```

Hudi Connector 使用建议：

1. Hudi 适合 Upsert、CDC 入湖和增量消费场景。
2. `recordkey`、`partitionpath`、`precombine` 是核心配置，必须稳定。
3. `COPY_ON_WRITE` 适合读性能优先，`MERGE_ON_READ` 适合写入频繁和近实时场景。
4. 流式写入要关注 Checkpoint、Compaction、Clean、元数据表和小文件。
5. 主键设计错误会导致重复记录、更新失败或文件膨胀。
6. Hudi 与 Hive、Spark、Trino、Flink 共用时要统一表类型、Catalog 和版本兼容。

Connector 集成总体建议：

1. Kafka 用于实时消息入口和中间结果分发。
2. JDBC 用于低中吞吐维表、配置表和小规模结果表。
3. Flink CDC 用于数据库变更捕获和实时同步。
4. Elasticsearch 用于检索型明细，不用于强一致指标存储。
5. Hive 用于离线数仓和 Metastore Catalog。
6. HBase 用于低延迟 Key-Value 维表和状态类数据。
7. Doris、StarRocks 用于高吞吐实时写入和 OLAP 查询。
8. Iceberg、Hudi 用于湖仓表、流批一体和历史数据管理。
9. 每个 Connector 上线前必须验证版本兼容、依赖打包、认证、Checkpoint、失败恢复和 Sink 语义。


## 业务处理设计

本章节用于说明 Flink 作业在业务层面的常见处理模式，包括实时清洗、实时聚合、实时宽表、实时风控、实时指标、实时告警、实时同步和离线补数。业务处理设计的核心目标是把技术算子组合成稳定、可解释、可恢复、可验证的业务链路，而不是只完成单个算子的编码。

### 实时清洗

实时清洗用于将原始数据转换为标准化、结构化、可消费的数据。常见输入包括 Kafka 日志、埋点数据、CDC 数据、文件数据和设备上报数据；常见输出包括标准化 Kafka Topic、ODS/DWD 明细表、脏数据表和告警流。

实时清洗通常包含以下步骤：

```text
原始数据 Source
  -> 格式解析
  -> 字段校验
  -> 字段标准化
  -> 枚举转换
  -> 时间字段处理
  -> 脏数据侧输出
  -> 标准化 Sink
```

实时清洗处理内容如下：

| 处理项     | 示例                                       |
| ---------- | ------------------------------------------ |
| 格式解析   | JSON、Avro、Protobuf、Debezium JSON        |
| 字段校验   | 必填字段、时间字段、金额字段、枚举字段     |
| 字段标准化 | 字段命名统一、大小写统一、空值默认值       |
| 类型转换   | 字符串转数字、时间戳转时间对象             |
| 数据脱敏   | 手机号、身份证、邮箱、地址脱敏             |
| 脏数据分流 | 解析失败、字段缺失、非法枚举、异常时间     |
| 数据补全   | 来源系统、接收时间、TraceId、Topic、Offset |

实时清洗主流程示例：

```java
SingleOutputStreamOperator<UserBehaviorEvent> cleanStream = rawStream
        .process(new UserBehaviorParseFunction())
        .name("parse-user-behavior")
        .uid("parse-user-behavior");

DataStream<DirtyData> dirtyStream = cleanStream
        .getSideOutput(UserBehaviorParseFunction.DIRTY_OUTPUT_TAG)
        .name("dirty-user-behavior");

cleanStream
        .map(JSONUtil::toJsonStr)
        .name("clean-user-behavior-to-json")
        .uid("clean-user-behavior-to-json")
        .sinkTo(cleanKafkaSink)
        .name("clean-user-behavior-kafka-sink")
        .uid("clean-user-behavior-kafka-sink");

dirtyStream
        .map(JSONUtil::toJsonStr)
        .name("dirty-user-behavior-to-json")
        .uid("dirty-user-behavior-to-json")
        .sinkTo(dirtyKafkaSink)
        .name("dirty-user-behavior-kafka-sink")
        .uid("dirty-user-behavior-kafka-sink");
```

实时清洗设计建议：

1. 清洗作业应尽量保持轻量，避免同时承载复杂聚合和外部维表查询。
2. 脏数据必须持久化，不能只打印日志。
3. 清洗后的标准 Topic 应有明确 Schema 文档。
4. 清洗作业输出字段应稳定，避免下游频繁适配。
5. 清洗失败率、脏数据量、解析异常类型需要监控。
6. 高频异常数据不要逐条打印完整日志，只打印摘要并输出到脏数据 Sink。

### 实时聚合

实时聚合用于对流式数据按 Key、窗口或状态进行累计统计。常见场景包括按分钟统计 PV、UV、订单数、支付金额、失败率、设备上报次数和风控命中次数。

实时聚合通常包含以下步骤：

```text
标准化事件流
  -> 提取事件时间
  -> 分配 Watermark
  -> keyBy 分组
  -> 窗口聚合 / 状态聚合
  -> 输出指标结果
```

常见聚合方式如下：

| 聚合方式        | 适用场景                     |
| --------------- | ---------------------------- |
| 无窗口滚动状态  | 累计计数、累计金额、最近状态 |
| 滚动窗口        | 每分钟、每小时、每天指标     |
| 滑动窗口        | 最近 N 分钟滚动指标          |
| 会话窗口        | 用户连续访问会话统计         |
| SQL 聚合        | 指标口径清晰、适合声明式表达 |
| DataStream 聚合 | 逻辑复杂、需要状态和定时器   |

实时聚合示例：

```java
DataStream<WindowEventCount> metricStream = eventStream
        .assignTimestampsAndWatermarks(watermarkStrategy)
        .keyBy(UserBehaviorEvent::getEventType)
        .window(TumblingEventTimeWindows.of(Time.minutes(1)))
        .aggregate(new EventCountAggregateFunction(), new WindowEventCountFunction())
        .name("one-minute-event-count")
        .uid("one-minute-event-count");
```

实时聚合设计建议：

1. 聚合结果必须携带维度字段和窗口时间。
2. 窗口指标写入下游时应使用幂等键，例如 `eventType + windowStart + windowEnd`。
3. 迟到数据可能导致窗口再次输出，下游 Sink 需要支持更新或幂等。
4. 高基数 Key 可能导致状态膨胀，需要监控状态大小。
5. 低基数 Key 可能导致数据倾斜，需要考虑二阶段聚合。
6. 指标口径必须文档化，明确时间语义、去重口径和延迟策略。

### 实时宽表

实时宽表用于将主流事件与维表、配置流、CDC 变更流或其他业务流进行关联，生成面向分析和服务查询的宽表数据。常见场景包括订单宽表、用户行为宽表、设备状态宽表和风控特征宽表。

实时宽表常见输入如下：

| 输入类型 | 示例                             |
| -------- | -------------------------------- |
| 主流     | 订单事件、支付事件、用户行为事件 |
| 维表     | 用户表、商品表、店铺表、地区表   |
| 配置流   | 活动配置、规则配置、黑白名单     |
| 变更流   | MySQL CDC、维表 CDC、状态变更流  |
| 外部查询 | Redis、HBase、JDBC、Doris Lookup |

实时宽表处理流程：

```text
主流事件
  -> 清洗和标准化
  -> 维表 Join / 异步 IO / Broadcast State
  -> 字段补全
  -> 宽表模型构建
  -> 写入 Kafka / Doris / StarRocks / Iceberg / Hudi
```

宽表模型示例：

文件位置：`flink-model/src/main/java/io/github/atengk/flink/model/UserBehaviorWideEvent.java`

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户行为宽表事件
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBehaviorWideEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;

    private String userName;

    private String city;

    private String itemId;

    private String itemName;

    private String categoryId;

    private String categoryName;

    private String eventType;

    private Long eventTime;
}
```

实时宽表设计建议：

1. 主流和维表的时间语义要明确，是关联最新维度还是历史版本维度。
2. 维表数据量小、变更低频时，可以使用 Broadcast State。
3. 维表数据量大、查询频繁时，可以使用 Redis、HBase 或异步 IO。
4. SQL Lookup Join 适合外部维表查询，Flink SQL 中 Lookup Join 通常通过 `FOR SYSTEM_TIME AS OF` 语法表达，并要求右表由支持 lookup 的连接器提供。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/sql/reference/queries/joins/?utm_source=chatgpt.com))
5. 宽表输出应有主键或事件 ID，避免重复写入。
6. 维表缺失时要明确降级策略，例如保留主流、输出默认值或进入异常流。

### 实时风控

实时风控用于对交易、登录、支付、注册、设备、行为路径等事件进行实时规则判断、特征计算和风险输出。风控作业通常是高状态、高规则、高时效要求的 Flink 作业。

实时风控常见模式如下：

| 模式         | 示例                               |
| ------------ | ---------------------------------- |
| 单事件规则   | 金额超过阈值、异常 IP、黑名单命中  |
| 窗口频次规则 | 10 分钟内失败登录超过 5 次         |
| 状态规则     | 同一用户短时间切换多个设备         |
| 双流规则     | 订单创建后 15 分钟未支付           |
| 动态规则     | 规则流实时更新风控策略             |
| 特征查询     | 查询用户画像、历史风险分、设备指纹 |

实时风控处理流程：

```text
业务事件流
  -> 清洗标准化
  -> keyBy 用户 / 设备 / 订单
  -> 状态更新
  -> 规则匹配
  -> 风险评分
  -> 命中结果输出
  -> 告警 / 拦截 / 审计
```

风控命中结果模型示例：

文件位置：`flink-model/src/main/java/io/github/atengk/flink/model/RiskHitResult.java`

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 风控命中结果
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskHitResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;

    private String userId;

    private String ruleId;

    private String ruleName;

    private String riskLevel;

    private String hitReason;

    private Long eventTime;

    private Long hitTime;
}
```

实时风控设计建议：

1. 规则变更频繁时，应使用 Broadcast State 或规则流，而不是重启作业。
2. 高频状态必须配置 TTL，避免状态无限增长。
3. 风控结果必须携带规则 ID、规则版本、命中原因和事件 ID。
4. 外部画像查询应使用异步 IO，避免同步调用造成反压。
5. 黑白名单、阈值、策略开关应支持动态更新。
6. 风控误杀和漏判都需要支持回放验证。

### 实时指标计算

实时指标计算用于将业务事件转换为可展示、可告警、可分析的指标数据。指标计算与实时聚合相似，但更强调指标口径、维度、窗口、结果存储和监控展示。

常见指标类型如下：

| 指标类型 | 示例                       |
| -------- | -------------------------- |
| 计数指标 | PV、UV、订单数、支付数     |
| 金额指标 | GMV、退款金额、支付金额    |
| 比率指标 | 成功率、失败率、转化率     |
| 延迟指标 | 订单支付耗时、链路处理延迟 |
| 状态指标 | 在线设备数、活跃用户数     |
| 异常指标 | 错误数、拒绝数、风控命中数 |

实时指标输出模型示例：

文件位置：`flink-model/src/main/java/io/github/atengk/flink/model/RealtimeMetric.java`

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 实时指标结果
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeMetric implements Serializable {

    private static final long serialVersionUID = 1L;

    private String metricName;

    private String metricKey;

    private String metricValue;

    private Long windowStart;

    private Long windowEnd;

    private Long updateTime;
}
```

实时指标设计建议：

1. 指标必须有唯一标识，例如 `metricName + metricKey + windowStart + windowEnd`。
2. 指标口径要文档化，包含过滤条件、时间字段、窗口大小和更新方式。
3. UV 等去重指标需要明确去重周期和状态 TTL。
4. 比率指标要处理分母为 0 的情况。
5. 输出到 Doris、StarRocks、Redis 或 Kafka 时都要设计幂等键。
6. 指标结果需要与离线数仓对账，避免实时口径长期偏差。

### 实时告警

实时告警用于在异常指标、异常事件、风控命中、任务失败或业务规则触发时向告警系统发送通知。告警可以基于单条事件，也可以基于窗口聚合结果或状态判断结果。

实时告警处理流程：

```text
事件流 / 指标流
  -> 规则判断
  -> 告警去重
  -> 告警抑制
  -> 告警等级计算
  -> 告警消息构建
  -> 写入告警 Sink
```

告警消息模型示例：

文件位置：`flink-model/src/main/java/io/github/atengk/flink/model/AlarmMessage.java`

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 告警消息
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlarmMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String alarmId;

    private String alarmName;

    private String alarmLevel;

    private String alarmContent;

    private String bizKey;

    private Long eventTime;

    private Long alarmTime;
}
```

实时告警设计建议：

1. 告警必须去重，避免同一异常持续刷屏。
2. 告警应支持抑制窗口，例如 5 分钟内同类告警只发一次。
3. 告警等级、接收人、通知渠道应配置化。
4. 告警内容要包含作业名、业务 Key、规则 ID、异常摘要和排查入口。
5. 告警 Sink 不应阻塞主链路，高风险场景建议写 Kafka 后由专门告警服务发送。
6. 告警失败需要记录日志和补偿，不应静默丢失。

### 实时数据同步

实时数据同步用于将源系统数据变更同步到下游系统，例如 MySQL CDC 到 Kafka、Doris、StarRocks、Iceberg、Hudi，或 Kafka 明细流同步到 OLAP 表。同步链路重点关注一致性、幂等、Schema 变更和失败恢复。

常见同步链路如下：

| 链路                   | 场景                  |
| ---------------------- | --------------------- |
| MySQL CDC -> Kafka     | 数据库变更事件分发    |
| MySQL CDC -> Doris     | 实时入仓、明细查询    |
| MySQL CDC -> StarRocks | 实时 OLAP 分析        |
| Kafka -> Iceberg       | 明细入湖              |
| Kafka -> Hudi          | Upsert 入湖、增量查询 |
| Kafka -> Elasticsearch | 检索型明细            |

实时同步处理流程：

```text
Source 变更数据
  -> Schema 解析
  -> 主键提取
  -> 操作类型识别
  -> 字段映射
  -> 幂等写入 / Upsert
  -> 对账校验
```

实时同步设计建议：

1. CDC 同步必须明确主键、操作类型、DDL 变更策略。
2. 下游支持 Upsert 时，应优先使用主键更新。
3. 写入 OLAP 或湖仓时，要考虑分区、小文件、批量提交和 Compaction。
4. 删除事件是否同步必须明确，不同 Sink 对 DELETE 支持不同。
5. 同步作业需要监控延迟、Binlog 位点、写入失败、脏数据和对账差异。
6. 同步链路上线前必须做全量快照、增量变更、失败恢复和重复写入测试。

### 离线批处理补数

离线批处理补数用于修复历史数据、重算指标、补齐漏写结果或处理延迟到达的大批量数据。Flink 可以通过有界流或批执行模式处理历史数据，补数作业应与实时主链路隔离，避免影响在线实时计算。

补数常见场景如下：

| 场景            | 示例                              |
| --------------- | --------------------------------- |
| 历史指标重算    | 重算某天 UV、PV、GMV              |
| 漏数修复        | 修复 Kafka 消费异常期间丢失的输出 |
| Schema 变更重刷 | 新增字段后重刷历史宽表            |
| 脏数据修复      | 修复并回放脏数据                  |
| 下游故障补写    | Sink 故障期间重放数据             |
| 离线对账修正    | 按离线结果修正实时指标            |

补数作业处理流程：

```text
指定补数时间范围
  -> 读取历史数据 / Kafka 回放 / 文件输入
  -> 复用清洗和计算逻辑
  -> 写入临时表或正式表
  -> 数据校验
  -> 切换或覆盖结果
```

补数设计建议：

1. 补数作业必须有明确时间范围，禁止无边界扫描。
2. 补数输出要支持幂等或覆盖。
3. 补数作业与实时作业使用不同消费组、输出路径或临时表。
4. 补数前要评估下游写入压力，避免影响生产查询。
5. 补数结果必须校验，例如行数、金额、唯一键、窗口指标。
6. 补数脚本、参数、数据范围和结果要记录到发布或运维文档中。

## 多流处理

本章节用于说明 Flink 中多个数据流之间的关联处理方式，包括双流 Join、Interval Join、Window Join、Broadcast State、维表 Join、异步 IO、规则流处理和主流与配置流联动。多流处理的关键问题不是“能否 Join”，而是要明确时间语义、状态生命周期、迟到数据、维表版本、Join 结果语义和状态大小。

Flink DataStream 官方 Join 文档说明，Window Join 会连接相同 Key 且落在同一个窗口内的两个流；Interval Join 会连接相同 Key 且时间戳位于指定时间区间内的两个流。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/operators/joining/?utm_source=chatgpt.com))

### 双流 Join

双流 Join 指两个业务流之间的关联处理，例如订单流与支付流、曝光流与点击流、登录流与风控结果流。双流 Join 可以通过 Window Join、Interval Join、CoProcessFunction、自定义状态和 SQL Join 实现。

常见双流 Join 场景如下：

| 场景         | 左流       | 右流       | Join Key   |
| ------------ | ---------- | ---------- | ---------- |
| 订单支付关联 | 订单创建流 | 支付成功流 | `orderId`  |
| 曝光点击关联 | 曝光流     | 点击流     | `traceId`  |
| 注册登录关联 | 注册流     | 登录流     | `userId`   |
| 交易风控关联 | 交易流     | 风控结果流 | `eventId`  |
| 设备状态关联 | 设备上报流 | 设备配置流 | `deviceId` |

双流 Join 设计需要先明确以下问题：

1. 两个流是否都有事件时间。
2. Join 是否要求同一窗口内关联。
3. Join 是否允许一侧数据延迟到达。
4. 未匹配数据是否需要输出。
5. 状态保留多久。
6. 结果是 Inner Join、Left Join 还是自定义语义。

普通双流自定义状态处理流程如下：

```text
左流事件到达
  -> 写入左侧状态
  -> 查询右侧状态
  -> 匹配成功输出
  -> 注册清理定时器

右流事件到达
  -> 写入右侧状态
  -> 查询左侧状态
  -> 匹配成功输出
  -> 注册清理定时器
```

双流 Join 设计建议：

1. 如果 Join 关系有明确时间边界，优先使用 Interval Join 或 Window Join。
2. 如果需要输出未匹配数据，应使用 CoProcessFunction 自定义状态和定时器。
3. 两个流都应设置 Watermark，避免事件时间 Join 长时间不触发。
4. Join Key 基数过低会造成数据倾斜。
5. Join 状态必须有 TTL 或定时清理。
6. 结果 Sink 要设计幂等键，避免恢复后重复输出。

### Interval Join

Interval Join 用于关联两个 KeyedStream 中相同 Key 且时间戳满足相对时间范围的数据。它适合订单与支付、曝光与点击、请求与响应等具有明确先后时间关系的场景。Flink 官方 DataStream Join 文档说明，Interval Join 会把一个流的元素与另一个流中相同 Key 且时间戳位于指定区间内的元素进行 Join，例如 `b.timestamp ∈ [a.timestamp + lowerBound, a.timestamp + upperBound]`。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/operators/joining/?utm_source=chatgpt.com))

示例场景：订单创建后 15 分钟内支付成功，则认为订单支付匹配。

订单事件模型：

文件位置：`flink-model/src/main/java/io/github/atengk/flink/model/OrderEvent.java`

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单事件
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderId;

    private String userId;

    private Long orderAmount;

    private Long eventTime;
}
```

支付事件模型：

文件位置：`flink-model/src/main/java/io/github/atengk/flink/model/PayEvent.java`

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 支付事件
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderId;

    private String payChannel;

    private Long payAmount;

    private Long eventTime;
}
```

Join 结果模型：

文件位置：`flink-model/src/main/java/io/github/atengk/flink/model/OrderPayResult.java`

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单支付关联结果
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPayResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderId;

    private String userId;

    private Long orderAmount;

    private String payChannel;

    private Long payAmount;

    private Long orderTime;

    private Long payTime;
}
```

Interval Join 示例：

```java
DataStream<OrderPayResult> orderPayResultStream = orderStream
        .keyBy(OrderEvent::getOrderId)
        .intervalJoin(payStream.keyBy(PayEvent::getOrderId))
        .between(Time.minutes(0), Time.minutes(15))
        .process(new ProcessJoinFunction<OrderEvent, PayEvent, OrderPayResult>() {

            @Override
            public void processElement(OrderEvent order, PayEvent pay, Context ctx, Collector<OrderPayResult> out) {
                out.collect(new OrderPayResult(
                        order.getOrderId(),
                        order.getUserId(),
                        order.getOrderAmount(),
                        pay.getPayChannel(),
                        pay.getPayAmount(),
                        order.getEventTime(),
                        pay.getEventTime()
                ));
            }
        })
        .name("order-pay-interval-join")
        .uid("order-pay-interval-join");
```

Interval Join 使用建议：

1. 两个输入流必须先 `keyBy`。
2. 两个流都应有事件时间和 Watermark。
3. 时间区间必须结合业务 SLA 设计，例如支付有效期、点击归因窗口。
4. Interval Join 语义类似 Inner Join，未匹配事件不会自动输出。
5. 如果需要处理未支付订单或未匹配事件，应使用 `KeyedCoProcessFunction` 自定义定时器。
6. Join 时间范围越大，状态保留越久，Checkpoint 压力越大。

### Window Join

Window Join 用于将两个流中相同 Key 且落在同一个窗口内的数据进行关联。Flink 官方 Join 文档说明，Window Join 会基于窗口分配器定义的窗口，将两个流中相同 Key 且在同一窗口内的元素传入用户自定义的 `JoinFunction` 或 `FlatJoinFunction`；语义上类似 Inner Join，一侧没有匹配元素则不会输出。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/operators/joining/?utm_source=chatgpt.com))

Window Join 适合以下场景：

| 场景             | 示例                         |
| ---------------- | ---------------------------- |
| 同窗口数据对齐   | 每分钟广告曝光和点击关联     |
| 固定周期汇总关联 | 每小时订单指标和支付指标关联 |
| 会话内关联       | 同一用户会话内行为和结果关联 |
| 批流统一窗口关联 | 历史数据按固定窗口 Join      |

Window Join 示例：

```java
DataStream<OrderPayResult> resultStream = orderStream
        .join(payStream)
        .where(OrderEvent::getOrderId)
        .equalTo(PayEvent::getOrderId)
        .window(TumblingEventTimeWindows.of(Time.minutes(1)))
        .apply((order, pay) -> new OrderPayResult(
                order.getOrderId(),
                order.getUserId(),
                order.getOrderAmount(),
                pay.getPayChannel(),
                pay.getPayAmount(),
                order.getEventTime(),
                pay.getEventTime()
        ))
        .name("order-pay-window-join")
        .uid("order-pay-window-join");
```

Window Join 与 Interval Join 选择建议：

| 场景                       | 推荐                                           |
| -------------------------- | ---------------------------------------------- |
| 两个事件有明确相对时间关系 | Interval Join                                  |
| 两个流按同一统计周期关联   | Window Join                                    |
| 需要未匹配数据输出         | 自定义 CoProcessFunction                       |
| Join 条件复杂              | 自定义状态处理                                 |
| SQL 表关联                 | Flink SQL Join、Interval Join 或 Temporal Join |

Window Join 使用建议：

1. 适合固定窗口内的数据关联。
2. 窗口大小过大会增加状态压力。
3. 未匹配数据不会输出，如需输出需自定义实现。
4. 迟到数据策略要与窗口 Watermark 和 allowed lateness 配合。
5. Join 结果要携带窗口时间，便于下游幂等写入。

### Broadcast State

Broadcast State 用于将低吞吐配置流、规则流、字典流广播到所有并行实例，并在每个实例本地保存一份状态，再与高吞吐主流进行联动处理。Flink 官方 Broadcast State 文档说明，Broadcast State 适合某个流的数据需要被广播到下游所有并行任务并作为状态保存，用于处理另一个非广播流；官方提供 `BroadcastProcessFunction` 和 `KeyedBroadcastProcessFunction` 两种 API。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/dev/datastream/fault-tolerance/broadcast_state/?utm_source=chatgpt.com))

典型场景如下：

| 主流       | 广播流         | 应用         |
| ---------- | -------------- | ------------ |
| 用户行为流 | 规则配置流     | 动态规则匹配 |
| 交易流     | 风控规则流     | 实时风控     |
| 设备数据流 | 设备阈值配置流 | 动态告警     |
| 订单流     | 活动配置流     | 活动归因     |
| 日志流     | 过滤规则流     | 动态清洗     |

规则模型示例：

文件位置：`flink-model/src/main/java/io/github/atengk/flink/model/EventRule.java`

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 事件规则
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventRule implements Serializable {

    private static final long serialVersionUID = 1L;

    private String ruleId;

    private String eventType;

    private Boolean enabled;

    private Long updateTime;
}
```

Broadcast State 处理函数示例：

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/function/EventRuleBroadcastFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.function;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.flink.model.EventRule;
import io.github.atengk.flink.model.UserBehaviorEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;

/**
 * 事件规则广播处理函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class EventRuleBroadcastFunction extends BroadcastProcessFunction<UserBehaviorEvent, EventRule, UserBehaviorEvent> {

    public static final MapStateDescriptor<String, EventRule> RULE_STATE_DESCRIPTOR =
            new MapStateDescriptor<>("event-rule-broadcast-state", String.class, EventRule.class);

    /**
     * 处理主流事件
     *
     * @param value 用户行为事件
     * @param ctx 只读广播状态上下文
     * @param out 输出收集器
     * @throws Exception 状态访问异常
     */
    @Override
    public void processElement(UserBehaviorEvent value, ReadOnlyContext ctx, Collector<UserBehaviorEvent> out) throws Exception {
        EventRule rule = ctx.getBroadcastState(RULE_STATE_DESCRIPTOR).get(value.getEventType());

        if (rule == null || BooleanUtil.isTrue(rule.getEnabled())) {
            out.collect(value);
        }
    }

    /**
     * 处理广播规则流
     *
     * @param value 事件规则
     * @param ctx 广播状态上下文
     * @param out 输出收集器
     * @throws Exception 状态访问异常
     */
    @Override
    public void processBroadcastElement(EventRule value, Context ctx, Collector<UserBehaviorEvent> out) throws Exception {
        if (value == null || StrUtil.isBlank(value.getEventType())) {
            log.warn("收到无效规则配置，规则内容：{}", value);
            return;
        }

        ctx.getBroadcastState(RULE_STATE_DESCRIPTOR).put(value.getEventType(), value);
        log.info("事件规则已更新，规则ID：{}，事件类型：{}，启用状态：{}",
                value.getRuleId(), value.getEventType(), value.getEnabled());
    }
}
```

使用示例：

```java
BroadcastStream<EventRule> ruleBroadcastStream = ruleStream
        .broadcast(EventRuleBroadcastFunction.RULE_STATE_DESCRIPTOR);

DataStream<UserBehaviorEvent> filteredStream = eventStream
        .connect(ruleBroadcastStream)
        .process(new EventRuleBroadcastFunction())
        .name("event-rule-broadcast-process")
        .uid("event-rule-broadcast-process");
```

Broadcast State 使用建议：

1. 适合低吞吐、小规模、需要广播到所有并行实例的配置数据。
2. 广播状态会在每个并行实例保存一份，不适合超大维表。
3. `MapStateDescriptor` 在广播、读取、写入时必须一致。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/dev/datastream/fault-tolerance/broadcast_state/?utm_source=chatgpt.com))
4. 规则需要版本号、更新时间、生效状态和审计记录。
5. 广播流异常不能影响主流稳定性，应校验规则合法性。
6. 如果主流已经 `keyBy`，并且需要访问 Keyed State，应使用 `KeyedBroadcastProcessFunction`。

### 维表 Join

维表 Join 用于将事实流补充维度信息。常见维表包括用户表、商品表、店铺表、地区表、设备表、活动表和规则表。维表 Join 既可以用 SQL Lookup Join，也可以用 DataStream 异步 IO、Broadcast State、本地缓存或 HBase/Redis 查询实现。Flink SQL 文档说明，Lookup Join 通常用于用外部系统中的数据增强表，需要左表有处理时间属性，右表由支持 lookup 的连接器提供。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/sql/reference/queries/joins/?utm_source=chatgpt.com))

维表 Join 方案对比：

| 方案            | 适用场景                      | 优点             | 风险         |
| --------------- | ----------------------------- | ---------------- | ------------ |
| SQL Lookup Join | JDBC、HBase、Doris 等外部维表 | SQL 简洁         | 外部查询压力 |
| Broadcast State | 小维表、规则表、配置表        | 低延迟、本地状态 | 不适合大表   |
| Async I/O       | Redis、HBase、HTTP、数据库    | 高并发、低阻塞   | 实现复杂     |
| 本地缓存        | 低频变更维表                  | 性能好           | 一致性差     |
| CDC 维表流      | 维表变更需要实时感知          | 版本可控         | 状态管理复杂 |

SQL Lookup Join 示例：

```sql
CREATE TEMPORARY TABLE user_profile_dim (
    user_id STRING,
    user_name STRING,
    city STRING,
    user_level STRING
) WITH (
    'connector' = 'jdbc',
    'url' = 'jdbc:mysql://mysql-prod-vip:3306/dim',
    'table-name' = 'user_profile',
    'username' = 'flink_reader',
    'password' = '${MYSQL_PASSWORD}'
);

SELECT
    e.user_id,
    p.user_name,
    p.city,
    p.user_level,
    e.event_type,
    e.event_time
FROM user_behavior_source AS e
LEFT JOIN user_profile_dim FOR SYSTEM_TIME AS OF e.proc_time AS p
ON e.user_id = p.user_id;
```

维表 Join 设计建议：

1. 小维表优先 Broadcast State，大维表优先外部存储 Lookup 或 Async I/O。
2. 维表查询必须设置超时、缓存和降级策略。
3. 维表缺失时要明确输出默认值还是输出异常流。
4. 维表更新频率高时，不建议使用长时间本地缓存。
5. 如果需要按历史版本 Join，应使用 Event Time Temporal Join 或 CDC 维表版本化处理。
6. 维表 Join 要监控命中率、查询耗时、超时次数和外部系统 QPS。

### 异步 IO

异步 IO 用于在 Flink 流处理中访问外部系统而不阻塞主处理线程，适合高并发查询 Redis、HBase、HTTP 服务、画像服务、规则服务或数据库。Flink 官方 Async I/O 文档说明，异步 I/O 用于访问外部数据存储，并提供 API、超时处理、结果顺序、事件时间、容错保证、重试支持和实现建议。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/dev/datastream/operators/asyncio/?utm_source=chatgpt.com))

异步 IO 适用场景：

| 场景         | 示例                |
| ------------ | ------------------- |
| 维表查询     | Redis、HBase、MySQL |
| 画像查询     | 用户画像、设备画像  |
| 外部规则服务 | 风控评分服务        |
| HTTP 查询    | 地理位置、标签服务  |
| 外部特征查询 | 历史特征、实时特征  |

异步维表查询结果模型：

文件位置：`flink-model/src/main/java/io/github/atengk/flink/model/UserProfile.java`

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户画像维度
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;

    private String userName;

    private String city;

    private String userLevel;
}
```

异步 IO 函数示例：

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/function/UserProfileAsyncFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.function;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.flink.model.UserBehaviorEvent;
import io.github.atengk.flink.model.UserBehaviorWideEvent;
import io.github.atengk.flink.model.UserProfile;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * 用户画像异步查询函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserProfileAsyncFunction extends RichAsyncFunction<UserBehaviorEvent, UserBehaviorWideEvent> {

    /**
     * 异步查询用户画像并构建宽表事件
     *
     * @param input 用户行为事件
     * @param resultFuture 异步结果
     */
    @Override
    public void asyncInvoke(UserBehaviorEvent input, ResultFuture<UserBehaviorWideEvent> resultFuture) {
        CompletableFuture
                .supplyAsync(() -> queryUserProfile(input.getUserId()))
                .thenAccept(profile -> {
                    UserBehaviorWideEvent wideEvent = buildWideEvent(input, profile);
                    resultFuture.complete(Collections.singletonList(wideEvent));
                })
                .exceptionally(e -> {
                    log.warn("用户画像异步查询失败，userId：{}，原因：{}", input.getUserId(), e.getMessage());
                    resultFuture.complete(Collections.singletonList(buildWideEvent(input, null)));
                    return null;
                });
    }

    /**
     * 查询用户画像
     *
     * @param userId 用户ID
     * @return 用户画像
     */
    private UserProfile queryUserProfile(String userId) {
        if (StrUtil.isBlank(userId)) {
            return null;
        }

        // 示例代码：生产环境应替换为 Redis、HBase、HTTP 或数据库异步客户端
        return new UserProfile(userId, "用户-" + userId, "杭州", "A");
    }

    /**
     * 构建宽表事件
     *
     * @param event 用户行为事件
     * @param profile 用户画像
     * @return 用户行为宽表事件
     */
    private UserBehaviorWideEvent buildWideEvent(UserBehaviorEvent event, UserProfile profile) {
        return new UserBehaviorWideEvent(
                event.getUserId(),
                profile == null ? "" : profile.getUserName(),
                profile == null ? "" : profile.getCity(),
                event.getItemId(),
                "",
                event.getCategoryId(),
                "",
                event.getEventType(),
                event.getEventTime()
        );
    }
}
```

使用示例：

```java
DataStream<UserBehaviorWideEvent> wideStream = AsyncDataStream
        .unorderedWait(
                eventStream,
                new UserProfileAsyncFunction(),
                3,
                TimeUnit.SECONDS,
                100
        )
        .name("async-user-profile-lookup")
        .uid("async-user-profile-lookup");
```

异步 IO 使用建议：

1. 外部查询耗时较高时，应优先使用异步 IO，避免同步调用阻塞算子。
2. `unorderedWait` 吞吐较高，但结果顺序不保证；`orderedWait` 保证顺序但吞吐较低。Flink 官方 Async I/O 文档包含结果顺序相关语义说明。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/operators/asyncio?utm_source=chatgpt.com))
3. 必须设置超时时间和最大并发容量。
4. 异步查询失败要有默认值、降级或侧输出策略。
5. 不建议使用默认公共线程池承载高并发生产查询。
6. 外部系统必须有 QPS 保护、限流、缓存和告警。

### 规则流处理

规则流处理用于将业务规则、风控规则、过滤规则、告警阈值等作为独立数据流输入 Flink 作业，实现运行时动态更新。规则流通常来自 Kafka、CDC、配置中心变更事件、管理后台或文件。

规则模型通常需要包含以下字段：

| 字段                  | 说明        |
| --------------------- | ----------- |
| `ruleId`              | 规则唯一 ID |
| `ruleName`            | 规则名称    |
| `ruleType`            | 规则类型    |
| `conditionExpression` | 条件表达式  |
| `enabled`             | 是否启用    |
| `version`             | 规则版本    |
| `effectiveTime`       | 生效时间    |
| `updateTime`          | 更新时间    |

规则模型示例：

文件位置：`flink-model/src/main/java/io/github/atengk/flink/model/RiskRule.java`

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 风控规则
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskRule implements Serializable {

    private static final long serialVersionUID = 1L;

    private String ruleId;

    private String ruleName;

    private String ruleType;

    private String conditionExpression;

    private Boolean enabled;

    private Long version;

    private Long effectiveTime;

    private Long updateTime;
}
```

规则流处理流程：

```text
规则变更事件
  -> 解析规则
  -> 校验规则合法性
  -> 比较规则版本
  -> 更新 Broadcast State
  -> 主流按最新规则执行
```

规则流处理建议：

1. 规则必须有版本号，避免乱序更新覆盖新规则。
2. 规则变更必须记录日志和审计。
3. 规则解析失败不能影响主流处理。
4. 规则更新应支持启用、禁用、删除和版本回滚。
5. 复杂表达式规则需要进行预编译或缓存，避免每条数据重复解析表达式。
6. 规则流数据量通常较小，适合 Broadcast State；规则量巨大时应考虑外部规则服务或分层规则匹配。

### 主流与配置流联动

主流与配置流联动是 Flink 动态业务处理的典型模式。主流通常是高吞吐业务事件，配置流通常是低吞吐规则、阈值、黑白名单、活动配置或开关配置。Broadcast State 是该模式的核心实现之一。Flink 官方 Broadcast State 指南说明，Broadcast State 很适合低吞吐流和高吞吐流联合处理，或需要动态更新处理逻辑的应用。([Apache Flink](https://flink.apache.org/2019/06/26/a-practical-guide-to-broadcast-state-in-apache-flink/?utm_source=chatgpt.com))

典型结构如下：

```text
主流：用户行为 / 订单 / 支付 / 设备数据
        |
        v
connect
        ^
        |
配置流：规则 / 阈值 / 活动配置 / 黑白名单
        |
        v
broadcast
```

主流与配置流联动示例：

```java
BroadcastStream<EventRule> configBroadcastStream = configStream
        .broadcast(EventRuleBroadcastFunction.RULE_STATE_DESCRIPTOR);

DataStream<UserBehaviorEvent> resultStream = mainStream
        .connect(configBroadcastStream)
        .process(new EventRuleBroadcastFunction())
        .name("main-config-broadcast-process")
        .uid("main-config-broadcast-process");
```

如果主流需要先按用户或订单分组，并且规则处理还需要访问 Keyed State，可以使用 `KeyedBroadcastProcessFunction`。Flink 官方文档说明，`BroadcastProcessFunction` 和 `KeyedBroadcastProcessFunction` 的上下文能力不同；广播侧可以访问广播状态，非广播侧可读取广播状态，`KeyedBroadcastProcessFunction` 的非广播侧还可以使用定时器能力。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/dev/datastream/fault-tolerance/broadcast_state/?utm_source=chatgpt.com))

主流与配置流联动设计建议：

1. 配置流必须低频、可广播、状态量可控。
2. 配置必须有主键、版本号、更新时间和启用状态。
3. 配置更新要有幂等逻辑，避免重复更新产生副作用。
4. 主流处理时只能读取广播状态，广播流处理时可以更新广播状态。
5. 配置变更错误时要保留上一版本可用配置。
6. 主流与配置流联动应有配置命中率、配置版本、规则更新时间和异常规则数等监控指标。


## 维表处理

本章节用于说明 Flink 作业中维表数据的读取、缓存、异步查询、广播分发和一致性设计。维表通常用于补充事实流中的业务属性，例如用户画像、商品信息、店铺信息、地区信息、设备信息、规则配置和黑白名单。维表处理的核心不是“查到数据”即可，而是要综合考虑查询延迟、外部系统压力、缓存命中率、维表更新频率、数据一致性和失败降级策略。

Flink 官方 DataStream Connector 文档说明，连接外部系统不只有 Connector 一种方式，还可以通过异步 I/O 进行数据增强；该模式适合访问外部数据库或 Web 服务，避免同步调用阻塞主处理链路。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/connectors/datastream/overview/?utm_source=chatgpt.com))

### JDBC 维表

JDBC 维表通常指 MySQL、PostgreSQL、Oracle 等关系型数据库中的维度表，适合小规模、低频查询或通过缓存降低访问压力的场景。JDBC 维表常用于用户表、商品表、门店表、规则配置表和地区表。

JDBC 维表适用场景如下：

| 场景            | 说明                            |
| --------------- | ------------------------------- |
| 小规模维表      | 数据量不大，查询频率可控        |
| 管理后台配置    | 规则、阈值、字典、活动配置      |
| 低频维表查询    | 主流 QPS 不高，数据库可以承载   |
| SQL Lookup Join | 使用 Table API / SQL 做维表关联 |
| 本地缓存维表    | 数据变更频率低，可设置缓存 TTL  |

JDBC 维表查询要避免每条数据都同步访问数据库。同步访问会阻塞算子线程，外部数据库慢查询或连接池耗尽会直接造成 Flink 反压。生产环境中，JDBC 维表一般需要配合本地缓存、异步 IO 或 SQL Lookup Join 缓存参数使用。

下面的代码演示通过 JDBC 查询用户维表，并使用 Hutool 做参数校验。该示例适合低吞吐场景；高吞吐场景应改造为异步 IO 或缓存方案。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/dim/JdbcUserProfileLookupFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.dim;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.flink.model.UserBehaviorEvent;
import io.github.atengk.flink.model.UserBehaviorWideEvent;
import io.github.atengk.flink.model.UserProfile;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.RichMapFunction;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * JDBC 用户画像维表查询函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class JdbcUserProfileLookupFunction extends RichMapFunction<UserBehaviorEvent, UserBehaviorWideEvent> {

    private final JdbcDimConfig config;

    private transient Connection connection;

    private transient PreparedStatement statement;

    /**
     * 创建 JDBC 维表查询函数
     *
     * @param config JDBC 维表配置
     */
    public JdbcUserProfileLookupFunction(JdbcDimConfig config) {
        this.config = config;
    }

    /**
     * 初始化 JDBC 连接
     *
     * @param parameters Flink 配置
     * @throws Exception 初始化异常
     */
    @Override
    public void open(Configuration parameters) throws Exception {
        validate(config);

        Class.forName(config.driverClassName());
        connection = DriverManager.getConnection(config.url(), config.username(), config.password());
        statement = connection.prepareStatement("""
                SELECT user_id, user_name, city, user_level
                FROM dim_user_profile
                WHERE user_id = ?
                """);

        log.info("JDBC维表连接初始化完成，URL：{}", maskUrl(config.url()));
    }

    /**
     * 查询用户画像并构建宽表
     *
     * @param event 用户行为事件
     * @return 用户行为宽表事件
     * @throws Exception 查询异常
     */
    @Override
    public UserBehaviorWideEvent map(UserBehaviorEvent event) throws Exception {
        UserProfile profile = null;

        if (event != null && StrUtil.isNotBlank(event.getUserId())) {
            statement.setString(1, event.getUserId());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    profile = new UserProfile(
                            resultSet.getString("user_id"),
                            resultSet.getString("user_name"),
                            resultSet.getString("city"),
                            resultSet.getString("user_level")
                    );
                }
            }
        }

        return new UserBehaviorWideEvent(
                event.getUserId(),
                profile == null ? "" : profile.getUserName(),
                profile == null ? "" : profile.getCity(),
                event.getItemId(),
                "",
                event.getCategoryId(),
                "",
                event.getEventType(),
                event.getEventTime()
        );
    }

    /**
     * 关闭 JDBC 资源
     *
     * @throws Exception 关闭异常
     */
    @Override
    public void close() throws Exception {
        if (statement != null) {
            statement.close();
        }
        if (connection != null) {
            connection.close();
        }
        log.info("JDBC维表连接已关闭");
    }

    /**
     * 校验 JDBC 配置
     *
     * @param config JDBC 配置
     */
    private void validate(JdbcDimConfig config) {
        if (StrUtil.isBlank(config.url())) {
            throw new IllegalArgumentException("JDBC维表url不能为空");
        }
        if (StrUtil.isBlank(config.username())) {
            throw new IllegalArgumentException("JDBC维表username不能为空");
        }
        if (StrUtil.isBlank(config.driverClassName())) {
            throw new IllegalArgumentException("JDBC维表driverClassName不能为空");
        }
    }

    /**
     * 脱敏 JDBC 地址
     *
     * @param url JDBC URL
     * @return 脱敏地址
     */
    private String maskUrl(String url) {
        return StrUtil.blankToDefault(url, "").replaceAll("password=[^&]+", "password=****");
    }

    /**
     * JDBC 维表配置
     *
     * @author Ateng
     * @since 2026-05-11
     */
    public record JdbcDimConfig(
            String url,
            String username,
            String password,
            String driverClassName
    ) {
    }
}
```

JDBC 维表使用建议：

1. 主流吞吐较高时，不建议同步 JDBC 查询。
2. 查询字段必须命中索引，例如 `user_id`、`item_id`、`shop_id`。
3. 连接、超时、慢查询、连接池耗尽都需要监控。
4. 维表查询失败时要有默认值、侧输出或降级策略。
5. 频繁访问的维表应加本地缓存或 Redis/HBase 中间层。
6. 管理后台维表变更频繁时，可以通过 CDC 或广播流同步到 Flink 状态。

### Redis 维表

Redis 维表适合低延迟、高 QPS 的 Key-Value 查询场景，例如用户标签、设备状态、黑白名单、实时配置、活动开关和短周期缓存结果。需要注意，Apache Flink 官方 DataStream Connector 总览并未将 Redis 列为 Flink Project Connector；Apache StreamPark 文档也明确说明 Apache Flink 官方没有提供写 Redis 数据的官方 Connector，StreamPark 基于 Flink Connector Redis 做了封装。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/connectors/datastream/overview/?utm_source=chatgpt.com))

Redis 维表适用场景如下：

| 场景        | 说明                         |
| ----------- | ---------------------------- |
| 黑白名单    | `userId -> true/false`       |
| 用户标签    | `userId -> 标签集合`         |
| 实时状态    | `deviceId -> 最近状态`       |
| 规则缓存    | `ruleId -> 规则内容`         |
| 短期缓存    | 查询结果缓存，降低数据库压力 |
| 高 QPS 维表 | 主流吞吐较高，需要毫秒级查询 |

Redis 维表查询函数示例：

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/dim/RedisUserProfileLookupFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.dim;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.flink.model.UserBehaviorEvent;
import io.github.atengk.flink.model.UserBehaviorWideEvent;
import io.github.atengk.flink.model.UserProfile;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.RichMapFunction;
import redis.clients.jedis.JedisPooled;

/**
 * Redis 用户画像维表查询函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class RedisUserProfileLookupFunction extends RichMapFunction<UserBehaviorEvent, UserBehaviorWideEvent> {

    private final RedisDimConfig config;

    private transient JedisPooled jedis;

    /**
     * 创建 Redis 维表查询函数
     *
     * @param config Redis 配置
     */
    public RedisUserProfileLookupFunction(RedisDimConfig config) {
        this.config = config;
    }

    /**
     * 初始化 Redis 客户端
     *
     * @param parameters Flink 配置
     */
    @Override
    public void open(Configuration parameters) {
        if (StrUtil.isBlank(config.password())) {
            jedis = new JedisPooled(config.host(), config.port());
        } else {
            jedis = new JedisPooled(config.host(), config.port(), config.username(), config.password());
        }
        log.info("Redis维表客户端初始化完成，地址：{}:{}", config.host(), config.port());
    }

    /**
     * 查询 Redis 用户画像
     *
     * @param event 用户行为事件
     * @return 用户行为宽表事件
     */
    @Override
    public UserBehaviorWideEvent map(UserBehaviorEvent event) {
        UserProfile profile = null;

        if (event != null && StrUtil.isNotBlank(event.getUserId())) {
            String key = StrUtil.format("dim:user_profile:{}", event.getUserId());
            String value = jedis.get(key);
            if (StrUtil.isNotBlank(value) && JSONUtil.isTypeJSON(value)) {
                profile = JSONUtil.toBean(value, UserProfile.class);
            }
        }

        return new UserBehaviorWideEvent(
                event.getUserId(),
                profile == null ? "" : profile.getUserName(),
                profile == null ? "" : profile.getCity(),
                event.getItemId(),
                "",
                event.getCategoryId(),
                "",
                event.getEventType(),
                event.getEventTime()
        );
    }

    /**
     * 关闭 Redis 客户端
     */
    @Override
    public void close() {
        if (jedis != null) {
            jedis.close();
            log.info("Redis维表客户端已关闭");
        }
    }

    /**
     * Redis 维表配置
     *
     * @author Ateng
     * @since 2026-05-11
     */
    public record RedisDimConfig(
            String host,
            int port,
            String username,
            String password
    ) {
    }
}
```

Redis 维表使用建议：

1. Redis 查询适合高频 Key-Value 访问，但仍需控制 QPS。
2. Key 命名应统一，例如 `dim:user_profile:{userId}`。
3. Redis 维表应设置 TTL 或由上游维表同步任务维护。
4. 维表值建议使用 JSON、Hash 或二进制协议，并形成 Schema 文档。
5. Redis 异常时要有默认值或降级策略，避免主链路持续失败。
6. Redis Cluster、Sentinel、单节点模式的客户端配置应单独封装。

### HBase 维表

HBase 维表适合大规模、低延迟、按 RowKey 查询的数据访问场景，例如用户画像、设备画像、商品特征、风控特征、宽表明细和历史状态。Flink HBase SQL Connector 文档说明，HBase Connector 支持读写 HBase 表，HBase 始终按 Upsert 模式交换 Changelog 消息，并要求主键定义在 rowkey 字段上；如果未声明主键，则默认 rowkey 为主键。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/connectors/datastream/overview/?utm_source=chatgpt.com))

HBase 维表 DDL 示例：

```sql
CREATE TEMPORARY TABLE user_profile_hbase_dim (
    rowkey STRING,
    info ROW<
        user_name STRING,
        city STRING,
        user_level STRING
    >,
    stat ROW<
        last_active_time BIGINT
    >,
    PRIMARY KEY (rowkey) NOT ENFORCED
) WITH (
    'connector' = 'hbase-2.2',
    'table-name' = 'profile:user_profile',
    'zookeeper.quorum' = 'zk-prod-01:2181,zk-prod-02:2181,zk-prod-03:2181',
    'zookeeper.znode.parent' = '/hbase',
    'lookup.cache' = 'PARTIAL',
    'lookup.partial-cache.max-rows' = '100000',
    'lookup.partial-cache.expire-after-write' = '10min'
);
```

Lookup Join 示例：

```sql
SELECT
    e.user_id,
    p.info.user_name,
    p.info.city,
    p.info.user_level,
    e.event_type,
    e.event_time
FROM user_behavior_source AS e
LEFT JOIN user_profile_hbase_dim FOR SYSTEM_TIME AS OF e.proc_time AS p
ON e.user_id = p.rowkey;
```

HBase 维表设计建议：

1. RowKey 设计是核心，必须避免热点，例如纯递增 ID 容易造成 Region 热点。
2. 列族不宜过多，字段命名要稳定。
3. HBase Lookup 查询需要设置缓存、超时和重试。
4. 大规模维表不适合 Broadcast State，优先使用 HBase、Redis 或异步 IO。
5. HBase 表变更要考虑字段族、列名、编码方式和历史数据兼容。
6. 维表查询失败时应输出默认值或异常流，不应无限阻塞主流。

### 广播维表

广播维表适合数据量小、变更低频、需要被所有并行实例快速访问的维表或配置表，例如地区字典、活动配置、事件规则、黑白名单、状态码映射和小型商品分类表。Flink Broadcast State 官方文档说明，Broadcast State 适合把一个流广播到下游所有并行任务，并作为状态保存，用于和另一个非广播流共同处理；Flink 提供 `BroadcastProcessFunction` 和 `KeyedBroadcastProcessFunction`。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/fault-tolerance/broadcast_state/?utm_source=chatgpt.com))

广播维表处理流程：

```text
维表变更流
  -> 解析维表记录
  -> broadcast(...)
  -> 更新 Broadcast State
  -> 主流读取 Broadcast State
  -> 补充维表字段
```

广播维表模型示例：

文件位置：`flink-model/src/main/java/io/github/atengk/flink/model/CategoryDim.java`

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 商品类目维表
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDim implements Serializable {

    private static final long serialVersionUID = 1L;

    private String categoryId;

    private String categoryName;

    private Boolean enabled;

    private Long updateTime;
}
```

广播维表处理函数示例：

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/dim/CategoryBroadcastDimFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.dim;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.flink.model.CategoryDim;
import io.github.atengk.flink.model.UserBehaviorEvent;
import io.github.atengk.flink.model.UserBehaviorWideEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;

/**
 * 商品类目广播维表处理函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class CategoryBroadcastDimFunction extends BroadcastProcessFunction<UserBehaviorEvent, CategoryDim, UserBehaviorWideEvent> {

    public static final MapStateDescriptor<String, CategoryDim> CATEGORY_DIM_STATE =
            new MapStateDescriptor<>("category-dim-broadcast-state", String.class, CategoryDim.class);

    /**
     * 处理主流用户行为事件
     *
     * @param event 用户行为事件
     * @param ctx 只读上下文
     * @param out 输出收集器
     * @throws Exception 状态访问异常
     */
    @Override
    public void processElement(UserBehaviorEvent event, ReadOnlyContext ctx, Collector<UserBehaviorWideEvent> out) throws Exception {
        CategoryDim dim = ctx.getBroadcastState(CATEGORY_DIM_STATE).get(event.getCategoryId());

        out.collect(new UserBehaviorWideEvent(
                event.getUserId(),
                "",
                "",
                event.getItemId(),
                "",
                event.getCategoryId(),
                dim == null ? "" : dim.getCategoryName(),
                event.getEventType(),
                event.getEventTime()
        ));
    }

    /**
     * 处理广播维表变更
     *
     * @param dim 类目维表
     * @param ctx 广播上下文
     * @param out 输出收集器
     * @throws Exception 状态访问异常
     */
    @Override
    public void processBroadcastElement(CategoryDim dim, Context ctx, Collector<UserBehaviorWideEvent> out) throws Exception {
        if (dim == null || StrUtil.isBlank(dim.getCategoryId())) {
            log.warn("收到无效类目维表数据：{}", dim);
            return;
        }

        if (Boolean.FALSE.equals(dim.getEnabled())) {
            ctx.getBroadcastState(CATEGORY_DIM_STATE).remove(dim.getCategoryId());
            log.info("类目维表已删除，categoryId：{}", dim.getCategoryId());
            return;
        }

        ctx.getBroadcastState(CATEGORY_DIM_STATE).put(dim.getCategoryId(), dim);
        log.info("类目维表已更新，categoryId：{}，categoryName：{}", dim.getCategoryId(), dim.getCategoryName());
    }
}
```

使用示例：

```java
BroadcastStream<CategoryDim> categoryBroadcastStream = categoryDimStream
        .broadcast(CategoryBroadcastDimFunction.CATEGORY_DIM_STATE);

DataStream<UserBehaviorWideEvent> wideStream = eventStream
        .connect(categoryBroadcastStream)
        .process(new CategoryBroadcastDimFunction())
        .name("category-broadcast-dim-join")
        .uid("category-broadcast-dim-join");
```

广播维表使用建议：

1. 广播维表适合小表，不适合百万级大维表。
2. 广播状态会在每个并行实例保存一份，状态量会随并行度线性放大。
3. 维表记录要有主键、版本号、更新时间和启用状态。
4. 广播维表更新应支持新增、修改、删除。
5. 错误维表数据不能污染状态，应先校验再写入。
6. 如果主流需要 Keyed State，应使用 `KeyedBroadcastProcessFunction`。

### 缓存设计

缓存设计用于降低外部维表查询压力，提升维表关联吞吐和稳定性。缓存可以存在于 Flink 算子本地、Redis、Caffeine、Guava Cache、HBase BlockCache、数据库连接池或 Connector 内置 Lookup Cache 中。缓存设计要平衡命中率、一致性、内存占用和更新延迟。

常见缓存方式如下：

| 缓存位置         | 适用场景            | 注意事项                      |
| ---------------- | ------------------- | ----------------------------- |
| 本地 Map         | 小维表、本地调试    | 无 TTL 容易内存增长           |
| Caffeine         | 高性能本地缓存      | 需要设置容量和过期策略        |
| Redis            | 多作业共享缓存      | 需要网络访问和 Redis 稳定性   |
| HBase            | 大维表查询          | RowKey 设计和缓存命中影响性能 |
| SQL Lookup Cache | Table/SQL 维表 Join | 依赖 Connector 支持           |
| Broadcast State  | 小维表动态广播      | 状态大小随并行度放大          |

本地缓存维表函数示例：

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/dim/CachedUserProfileLookupFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.dim;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.flink.model.UserBehaviorEvent;
import io.github.atengk.flink.model.UserBehaviorWideEvent;
import io.github.atengk.flink.model.UserProfile;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.functions.RichMapFunction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地缓存用户画像维表查询函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class CachedUserProfileLookupFunction extends RichMapFunction<UserBehaviorEvent, UserBehaviorWideEvent> {

    private final Map<String, UserProfile> localCache = new ConcurrentHashMap<>();

    /**
     * 查询缓存并补充用户画像
     *
     * @param event 用户行为事件
     * @return 用户行为宽表事件
     */
    @Override
    public UserBehaviorWideEvent map(UserBehaviorEvent event) {
        UserProfile profile = null;

        if (event != null && StrUtil.isNotBlank(event.getUserId())) {
            profile = localCache.computeIfAbsent(event.getUserId(), this::queryUserProfile);
        }

        return new UserBehaviorWideEvent(
                event.getUserId(),
                profile == null ? "" : profile.getUserName(),
                profile == null ? "" : profile.getCity(),
                event.getItemId(),
                "",
                event.getCategoryId(),
                "",
                event.getEventType(),
                event.getEventTime()
        );
    }

    /**
     * 查询用户画像
     *
     * @param userId 用户ID
     * @return 用户画像
     */
    private UserProfile queryUserProfile(String userId) {
        log.debug("缓存未命中，查询用户画像，userId：{}", userId);

        // 示例：生产环境应替换为 Redis、HBase、JDBC 或 HTTP 查询
        return new UserProfile(userId, "用户-" + userId, "杭州", "A");
    }
}
```

缓存设计建议：

1. 缓存必须设置最大容量，避免内存无限增长。
2. 缓存必须设置 TTL 或失效策略。
3. 缓存 Key 应稳定且短，例如 `userId`、`itemId`、`ruleId`。
4. 缓存值应尽量小，不要缓存大对象或完整历史明细。
5. 高一致性场景不应依赖长 TTL 本地缓存。
6. 缓存命中率、加载耗时、过期次数、异常次数都应监控。

### 缓存失效策略

缓存失效策略用于控制维表数据更新后多久能在 Flink 作业中生效。缓存失效策略必须结合维表更新频率、业务一致性要求、查询压力和容错恢复方式设计。

常见失效策略如下：

| 策略         | 说明                               | 适用场景         |
| ------------ | ---------------------------------- | ---------------- |
| 固定 TTL     | 写入缓存后固定时间过期             | 大多数维表查询   |
| 写后过期     | 按写入时间过期                     | 低频变更维表     |
| 读后过期     | 每次读取刷新过期时间               | 热点维表         |
| 主动失效     | 收到变更事件后删除缓存             | 高一致性要求     |
| 版本更新     | 维表记录带版本号，高版本覆盖低版本 | CDC 维表、规则流 |
| 定期全量刷新 | 周期加载完整维表                   | 小表、字典表     |

缓存失效设计建议：

1. 配置类维表建议使用主动失效或广播更新。
2. 用户画像类维表可以使用固定 TTL。
3. 黑名单、风控规则等高风险维表不建议长时间缓存。
4. 缓存击穿要有保护，例如空值缓存、限流、异步加载。
5. 缓存雪崩要避免大量 Key 同时过期，可增加随机过期时间。
6. 维表变更频繁时，应优先使用 CDC 或配置流驱动更新。

空值缓存示例逻辑：

```java
if (profile == null) {
    // 对不存在的用户也缓存一个短 TTL 空值，避免重复打到外部系统
    cache.put(userId, EMPTY_PROFILE);
}
```

### 异步查询优化

异步查询优化用于提升维表查询吞吐，避免同步外部查询阻塞 Flink 算子线程。Flink 官方 Async I/O 文档说明，异步 I/O 用于访问外部数据存储，并提供超时、结果顺序、事件时间、容错保证和重试支持等能力。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/connectors/datastream/overview/?utm_source=chatgpt.com))

异步查询优化方向如下：

| 优化点      | 说明                                         |
| ----------- | -------------------------------------------- |
| 最大并发    | 控制同时请求外部系统的数量                   |
| 超时时间    | 防止外部系统卡住主链路                       |
| 有序 / 无序 | `orderedWait` 保序，`unorderedWait` 吞吐更高 |
| 缓存        | 查询前先查本地缓存或 Redis                   |
| 批量查询    | 将多个 Key 聚合后批量查询                    |
| 降级        | 查询失败时使用默认值或侧输出                 |
| 重试        | 短暂异常可重试，数据错误不重试               |

异步 IO 使用示例：

```java
DataStream<UserBehaviorWideEvent> wideStream = AsyncDataStream
        .unorderedWait(
                eventStream,
                new UserProfileAsyncFunction(),
                3,
                TimeUnit.SECONDS,
                100
        )
        .name("async-user-profile-dim-lookup")
        .uid("async-user-profile-dim-lookup");
```

异步查询函数中建议处理超时：

```java
@Override
public void timeout(UserBehaviorEvent input, ResultFuture<UserBehaviorWideEvent> resultFuture) {
    log.warn("用户画像异步查询超时，userId：{}", input.getUserId());
    resultFuture.complete(Collections.singletonList(buildWideEvent(input, null)));
}
```

异步查询优化建议：

1. 高吞吐维表 Join 优先使用异步 IO。
2. `unorderedWait` 适合不要求输出顺序的场景。
3. `orderedWait` 会降低吞吐，但适合强顺序要求场景。
4. 最大并发不宜盲目调大，需要结合外部系统 QPS 和延迟压测。
5. 查询失败不能静默丢数据，应输出默认值、侧输出或告警。
6. 异步 IO 仍然需要 Checkpoint 和超时策略配合，避免恢复后重复或长时间阻塞。

### 维表数据一致性

维表数据一致性用于描述事实流与维表数据在时间上的匹配关系。实时宽表中最常见的问题是：事件发生时的维表值与处理时查询到的最新维表值不一致。不同业务对一致性要求不同，必须在设计阶段明确。

常见维表一致性语义如下：

| 语义         | 说明                             | 示例                   |
| ------------ | -------------------------------- | ---------------------- |
| 最新维表     | 使用处理时刻能查到的最新维表值   | 用户当前等级           |
| 事件时刻维表 | 使用事件发生时刻对应的历史维表值 | 订单发生时商品价格     |
| 快照维表     | 使用某个批次或版本的维表快照     | 补数任务               |
| 最终一致     | 短时间允许不一致，后续修复       | 缓存型标签             |
| 强一致       | 必须精确匹配业务时刻             | 金额、合同、计费类数据 |

维表一致性设计建议：

1. 实时宽表必须说明使用“最新维表”还是“事件时刻维表”。
2. 最新维表适合用户画像、地区、非强交易属性。
3. 事件时刻维表适合价格、合同、费率、计费规则等强一致场景。
4. 事件时刻维表通常需要维表变更流、版本字段、生效时间和历史表。
5. 缓存 TTL 会引入可见延迟，必须纳入一致性说明。
6. 维表缺失、查询失败、字段为空要有明确默认值或异常处理策略。
7. 宽表结果应保留维表版本号或更新时间，便于追溯。

## 自定义函数开发

本章节用于说明 Flink Java 项目中常见自定义函数的开发方式，包括 `MapFunction`、`FlatMapFunction`、`ProcessFunction`、`KeyedProcessFunction`、`WindowFunction`、`SinkFunction`、`SourceFunction` 和自定义序列化器。Flink 官方用户自定义函数文档说明，大多数 DataStream 操作都需要用户自定义函数，可以通过实现接口、匿名类、Lambda 或 Rich Function 的方式定义；Rich Function 额外提供 `open`、`close`、`getRuntimeContext` 等生命周期和运行时访问能力。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/user_defined_functions/?utm_source=chatgpt.com))

### 自定义 MapFunction

`MapFunction` 用于一进一出转换，适合字段映射、类型转换、简单补全和轻量计算。复杂逻辑、状态访问和侧输出流不应放在普通 `MapFunction` 中。

下面的代码将用户行为事件转换为窗口指标基础对象。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/function/UserBehaviorToMetricMapFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.function;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.flink.model.RealtimeMetric;
import io.github.atengk.flink.model.UserBehaviorEvent;
import org.apache.flink.api.common.functions.MapFunction;

/**
 * 用户行为转指标映射函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class UserBehaviorToMetricMapFunction implements MapFunction<UserBehaviorEvent, RealtimeMetric> {

    /**
     * 将用户行为事件映射为实时指标
     *
     * @param value 用户行为事件
     * @return 实时指标
     */
    @Override
    public RealtimeMetric map(UserBehaviorEvent value) {
        String eventType = StrUtil.blankToDefault(value.getEventType(), "unknown");
        String metricName = StrUtil.format("user_behavior_{}_count", eventType);

        return new RealtimeMetric(
                metricName,
                eventType,
                "1",
                null,
                null,
                System.currentTimeMillis()
        );
    }
}
```

使用示例：

```java
DataStream<RealtimeMetric> metricStream = eventStream
        .map(new UserBehaviorToMetricMapFunction())
        .name("map-user-behavior-to-metric")
        .uid("map-user-behavior-to-metric");
```

MapFunction 开发建议：

1. 只处理一进一出轻量逻辑。
2. 不要返回 null。
3. 不要访问慢速外部系统。
4. 复杂资源初始化应使用 `RichMapFunction`。
5. 函数逻辑应可单元测试。

### 自定义 FlatMapFunction

`FlatMapFunction` 用于一进零出、一进一出或一进多出转换，适合展开数组、拆分字符串、过滤无效记录和从一条消息中解析多条业务记录。

下面的代码将一条 JSON 数组消息展开为多条用户行为事件。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/function/UserBehaviorArrayFlatMapFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.function;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.flink.model.UserBehaviorEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.util.Collector;

import java.util.List;

/**
 * 用户行为数组展开函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserBehaviorArrayFlatMapFunction implements FlatMapFunction<String, UserBehaviorEvent> {

    /**
     * 展开用户行为 JSON 数组
     *
     * @param value JSON 数组字符串
     * @param out 输出收集器
     */
    @Override
    public void flatMap(String value, Collector<UserBehaviorEvent> out) {
        if (StrUtil.isBlank(value)) {
            return;
        }

        try {
            List<UserBehaviorEvent> events = JSONUtil.toList(value, UserBehaviorEvent.class);
            if (CollUtil.isEmpty(events)) {
                return;
            }

            for (UserBehaviorEvent event : events) {
                if (event != null && StrUtil.isNotBlank(event.getUserId())) {
                    out.collect(event);
                }
            }
        } catch (Exception e) {
            log.warn("用户行为数组展开失败，原因：{}", e.getMessage());
        }
    }
}
```

FlatMapFunction 开发建议：

1. 适合拆分、展开和过滤。
2. 解析失败的数据如果需要保留，应使用 `ProcessFunction` 侧输出流。
3. 不应在高频函数中输出大量日志。
4. 一进多出会放大数据量，需要评估下游吞吐。
5. 输出前应进行基础字段校验。

### 自定义 ProcessFunction

`ProcessFunction` 是更底层的流处理函数，可以访问事件时间戳、`TimerService`、侧输出流和运行时上下文。Flink 官方 ProcessFunction 文档说明，`ProcessFunction` 可以看作带有 Keyed State 和 Timer 能力的 `FlatMapFunction`，可访问事件、状态和定时器等流处理基础组件；其中状态和定时器通常在 Keyed Stream 上使用。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.20/docs/dev/datastream/operators/process_function/?utm_source=chatgpt.com))

下面的代码用于解析 JSON，并将失败数据输出到侧输出流。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/function/UserBehaviorJsonProcessFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.function;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.flink.model.DirtyData;
import io.github.atengk.flink.model.UserBehaviorEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

/**
 * 用户行为 JSON 处理函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserBehaviorJsonProcessFunction extends ProcessFunction<String, UserBehaviorEvent> {

    public static final OutputTag<DirtyData> DIRTY_TAG = new OutputTag<DirtyData>("dirty-user-behavior") {
    };

    /**
     * 处理 JSON 数据
     *
     * @param value 原始 JSON
     * @param ctx 上下文
     * @param out 主流输出
     */
    @Override
    public void processElement(String value, Context ctx, Collector<UserBehaviorEvent> out) {
        try {
            if (StrUtil.isBlank(value)) {
                outputDirty(ctx, value, "EMPTY_DATA", "原始数据为空");
                return;
            }

            UserBehaviorEvent event = JSONUtil.toBean(value, UserBehaviorEvent.class);
            if (event == null || StrUtil.isBlank(event.getUserId())) {
                outputDirty(ctx, value, "INVALID_USER_ID", "userId为空");
                return;
            }

            out.collect(event);
        } catch (Exception e) {
            outputDirty(ctx, value, "JSON_PARSE_ERROR", ExceptionUtil.getSimpleMessage(e));
            log.warn("JSON解析失败，原因：{}", ExceptionUtil.getSimpleMessage(e));
        }
    }

    /**
     * 输出脏数据
     *
     * @param ctx 上下文
     * @param payload 原始数据
     * @param reason 失败原因
     * @param message 错误信息
     */
    private void outputDirty(Context ctx, String payload, String reason, String message) {
        ctx.output(DIRTY_TAG, new DirtyData(
                "user-behavior-json",
                payload,
                reason,
                message,
                System.currentTimeMillis()
        ));
    }
}
```

ProcessFunction 开发建议：

1. 需要侧输出流时优先使用。
2. 需要定时器和状态时，应在 `keyBy` 后使用 `KeyedProcessFunction`。
3. 数据异常应分类输出，不要全部抛出导致作业重启。
4. 高频异常日志要控制输出量。
5. 处理逻辑要拆分清楚，避免一个函数承担解析、聚合、Join 和 Sink。

### 自定义 KeyedProcessFunction

`KeyedProcessFunction` 用于 Keyed Stream 上的复杂状态处理，支持 Keyed State 和定时器。它适合订单超时检测、用户行为状态机、去重、风控规则、连续事件识别和定时清理。Flink ProcessFunction 文档说明，定时器允许程序对处理时间或事件时间变化做出响应，`TimerService` 可注册未来的事件时间或处理时间回调。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.20/docs/dev/datastream/operators/process_function/?utm_source=chatgpt.com))

下面的代码用于检测订单创建后 15 分钟内是否支付，未支付则输出告警。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/order/function/OrderTimeoutProcessFunction.java`

```java
package io.github.atengk.flink.job.order.function;

import io.github.atengk.flink.model.AlarmMessage;
import io.github.atengk.flink.model.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

/**
 * 订单超时处理函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class OrderTimeoutProcessFunction extends KeyedProcessFunction<String, OrderEvent, AlarmMessage> {

    private static final long TIMEOUT_MS = 15 * 60 * 1000L;

    private transient ValueState<OrderEvent> orderState;

    /**
     * 初始化状态
     *
     * @param parameters Flink 配置
     */
    @Override
    public void open(org.apache.flink.configuration.Configuration parameters) {
        orderState = getRuntimeContext().getState(
                new ValueStateDescriptor<>("order-create-state", OrderEvent.class)
        );
    }

    /**
     * 处理订单事件
     *
     * @param value 订单事件
     * @param ctx 上下文
     * @param out 输出收集器
     * @throws Exception 状态访问异常
     */
    @Override
    public void processElement(OrderEvent value, Context ctx, Collector<AlarmMessage> out) throws Exception {
        orderState.update(value);
        long timerTime = value.getEventTime() + TIMEOUT_MS;
        ctx.timerService().registerEventTimeTimer(timerTime);
        log.debug("订单超时定时器已注册，orderId：{}，触发时间：{}", value.getOrderId(), timerTime);
    }

    /**
     * 订单超时回调
     *
     * @param timestamp 触发时间
     * @param ctx 定时器上下文
     * @param out 输出收集器
     * @throws Exception 状态访问异常
     */
    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<AlarmMessage> out) throws Exception {
        OrderEvent order = orderState.value();
        if (order == null) {
            return;
        }

        out.collect(new AlarmMessage(
                "order-timeout-" + order.getOrderId(),
                "订单支付超时",
                "WARN",
                "订单超过15分钟未支付，orderId：" + order.getOrderId(),
                order.getOrderId(),
                order.getEventTime(),
                System.currentTimeMillis()
        ));

        orderState.clear();
        log.info("订单支付超时告警已输出，orderId：{}", order.getOrderId());
    }
}
```

使用示例：

```java
DataStream<AlarmMessage> timeoutAlarmStream = orderStream
        .keyBy(OrderEvent::getOrderId)
        .process(new OrderTimeoutProcessFunction())
        .name("order-timeout-process")
        .uid("order-timeout-process");
```

KeyedProcessFunction 开发建议：

1. 必须在 `keyBy` 后使用。
2. 状态名称和算子 `uid()` 进入生产后应保持稳定。
3. 定时器需要明确事件时间还是处理时间。
4. 状态必须有清理策略，避免无限增长。
5. 适合复杂状态和定时逻辑，不适合简单字段转换。

### 自定义 WindowFunction

自定义 WindowFunction 用于在窗口触发时处理窗口内数据或窗口聚合结果。常见方式是使用 `AggregateFunction` 进行增量聚合，再配合 `ProcessWindowFunction` 输出窗口元信息，例如窗口开始时间、结束时间和 Key。

下面的代码用于输出事件类型、窗口时间和事件数量。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/window/WindowEventCountFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.window;

import io.github.atengk.flink.model.WindowEventCount;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

/**
 * 窗口事件数量输出函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class WindowEventCountFunction extends ProcessWindowFunction<Long, WindowEventCount, String, TimeWindow> {

    /**
     * 输出窗口事件统计结果
     *
     * @param key 分组键
     * @param context 窗口上下文
     * @param elements 聚合结果
     * @param out 输出收集器
     */
    @Override
    public void process(String key, Context context, Iterable<Long> elements, Collector<WindowEventCount> out) {
        Long count = elements.iterator().next();

        out.collect(new WindowEventCount(
                key,
                context.window().getStart(),
                context.window().getEnd(),
                count
        ));
    }
}
```

窗口结果模型：

文件位置：`flink-model/src/main/java/io/github/atengk/flink/model/WindowEventCount.java`

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 窗口事件统计结果
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WindowEventCount implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventType;

    private Long windowStart;

    private Long windowEnd;

    private Long count;
}
```

使用示例：

```java
DataStream<WindowEventCount> resultStream = eventStream
        .keyBy(UserBehaviorEvent::getEventType)
        .window(TumblingEventTimeWindows.of(Time.minutes(1)))
        .aggregate(new EventCountAggregateFunction(), new WindowEventCountFunction())
        .name("window-event-count")
        .uid("window-event-count");
```

WindowFunction 开发建议：

1. 能用增量聚合时，不要直接全量缓存窗口数据。
2. 窗口结果必须携带窗口时间，便于下游幂等。
3. 窗口函数中不要访问慢速外部系统。
4. 输出结果要明确 Append、Update 还是最终窗口结果。
5. 窗口迟到数据可能触发多次输出，下游 Sink 要能处理更新。

### 自定义 SinkFunction

自定义 SinkFunction 用于将数据写入官方 Connector 不支持的外部系统，例如内部 HTTP 服务、企业告警平台、私有存储、专有消息队列等。Flink DataStream Connector 文档说明，Flink 内置了文件、标准输出、Socket 等基础 Sink，同时也支持通过其他方式连接外部系统。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/connectors/datastream/overview/?utm_source=chatgpt.com))

下面的代码实现一个 HTTP JSON Sink。它适合低吞吐告警和通知场景；高吞吐写入应使用批量、异步或专用 Connector。

文件位置：`flink-connector/src/main/java/io/github/atengk/flink/connector/http/HttpAlarmSinkFunction.java`

```java
package io.github.atengk.flink.connector.http;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import io.github.atengk.flink.model.AlarmMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

/**
 * HTTP 告警 Sink 函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class HttpAlarmSinkFunction extends RichSinkFunction<AlarmMessage> {

    private final String webhookUrl;

    private final int timeoutMs;

    /**
     * 创建 HTTP 告警 Sink
     *
     * @param webhookUrl 告警地址
     * @param timeoutMs 超时时间
     */
    public HttpAlarmSinkFunction(String webhookUrl, int timeoutMs) {
        this.webhookUrl = webhookUrl;
        this.timeoutMs = timeoutMs;
    }

    /**
     * 发送告警消息
     *
     * @param value 告警消息
     */
    @Override
    public void invoke(AlarmMessage value, Context context) {
        try {
            String body = JSONUtil.toJsonStr(value);

            String response = HttpRequest.post(webhookUrl)
                    .timeout(timeoutMs)
                    .contentType("application/json")
                    .body(body)
                    .execute()
                    .body();

            log.info("告警消息发送完成，alarmId：{}，响应摘要：{}", value.getAlarmId(), response);
        } catch (Exception e) {
            log.error("告警消息发送失败，alarmId：{}，原因：{}",
                    value == null ? "" : value.getAlarmId(),
                    ExceptionUtil.getSimpleMessage(e),
                    e);
            throw e;
        }
    }
}
```

SinkFunction 开发建议：

1. 外部调用必须设置超时。
2. 数据错误和系统错误要区分处理。
3. 写入失败是否抛异常要与容错策略一致。
4. 高吞吐写入不要逐条同步调用。
5. 如果要支持 Exactly Once，需要实现幂等、事务或两阶段提交。
6. Sink 中不要打印敏感信息，例如 Token、密码、完整 Webhook。

### 自定义 SourceFunction

自定义 SourceFunction 用于接入官方 Connector 未覆盖的数据源，例如内部 HTTP 拉取、私有 MQ、设备协议、遗留系统和测试数据生成器。需要注意，新版本 Flink 更推荐使用新的 Source API；`SourceFunction` 或 `RichParallelSourceFunction` 更适合简单兼容场景、内部工具和教学示例。Flink Connector 总览说明，Flink 内置了文件、目录、Socket、集合和迭代器等预定义 Source，同时也可以通过其他方式连接外部系统。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/connectors/datastream/overview/?utm_source=chatgpt.com))

下面的代码实现一个简单轮询 Source，每隔固定时间生成一条模拟事件。

文件位置：`flink-connector/src/main/java/io/github/atengk/flink/connector/source/PollingMockSourceFunction.java`

```java
package io.github.atengk.flink.connector.source;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;

/**
 * 轮询模拟 Source 函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class PollingMockSourceFunction extends RichParallelSourceFunction<String> {

    private final long intervalMs;

    private volatile boolean running = true;

    /**
     * 创建轮询模拟 Source
     *
     * @param intervalMs 生成间隔毫秒数
     */
    public PollingMockSourceFunction(long intervalMs) {
        if (intervalMs <= 0) {
            throw new IllegalArgumentException("生成间隔必须大于0");
        }
        this.intervalMs = intervalMs;
    }

    /**
     * 运行 Source
     *
     * @param ctx Source 上下文
     * @throws Exception 运行异常
     */
    @Override
    public void run(SourceContext<String> ctx) throws Exception {
        int subtask = getRuntimeContext().getIndexOfThisSubtask();
        log.info("轮询模拟Source启动，Subtask：{}，间隔：{}ms", subtask, intervalMs);

        while (running) {
            String payload = """
                    {"eventId":"%s","subtask":%d,"eventType":"mock","eventTime":"%s"}
                    """.formatted(IdUtil.fastSimpleUUID(), subtask, DateUtil.now());

            synchronized (ctx.getCheckpointLock()) {
                ctx.collect(payload);
            }

            Thread.sleep(intervalMs);
        }

        log.info("轮询模拟Source结束，Subtask：{}", subtask);
    }

    /**
     * 取消 Source
     */
    @Override
    public void cancel() {
        running = false;
        log.info("收到轮询模拟Source取消信号");
    }
}
```

使用示例：

```java
DataStream<String> mockStream = env
        .addSource(new PollingMockSourceFunction(1000L))
        .name("polling-mock-source")
        .uid("polling-mock-source")
        .setParallelism(2);
```

SourceFunction 开发建议：

1. 必须实现 `cancel()`，否则作业停止可能卡住。
2. 长连接、线程池、客户端资源应在 `open()` 和 `close()` 中管理。
3. 需要容错时，必须保存读取位点并参与 Checkpoint。
4. 使用 `ctx.getCheckpointLock()` 保证发射数据和状态更新一致。
5. 生产级 Source 优先评估新 Source API，而不是简单 SourceFunction。
6. Source 不应无限制缓存数据，外部系统异常要有退避和告警。

### 自定义序列化器

自定义序列化器用于控制数据在 Source/Sink、Kafka、状态、网络传输或外部系统中的编码方式。常见场景包括 Kafka 自定义消息格式、Protobuf、Avro、压缩 JSON、自定义二进制协议和状态对象序列化。对于 Kafka 场景，常见做法是实现 Flink 的 `SerializationSchema<T>` 或 `DeserializationSchema<T>`。

下面的代码实现一个用户行为事件 JSON 序列化器，用于 Kafka Sink。

文件位置：`flink-connector/src/main/java/io/github/atengk/flink/connector/serde/UserBehaviorJsonSerializationSchema.java`

```java
package io.github.atengk.flink.connector.serde;

import cn.hutool.json.JSONUtil;
import io.github.atengk.flink.model.UserBehaviorEvent;
import org.apache.flink.api.common.serialization.SerializationSchema;

import java.nio.charset.StandardCharsets;

/**
 * 用户行为 JSON 序列化器
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class UserBehaviorJsonSerializationSchema implements SerializationSchema<UserBehaviorEvent> {

    /**
     * 序列化用户行为事件
     *
     * @param element 用户行为事件
     * @return 字节数组
     */
    @Override
    public byte[] serialize(UserBehaviorEvent element) {
        return JSONUtil.toJsonStr(element).getBytes(StandardCharsets.UTF_8);
    }
}
```

下面的代码实现一个 JSON 反序列化器，用于 Kafka Source。

文件位置：`flink-connector/src/main/java/io/github/atengk/flink/connector/serde/UserBehaviorJsonDeserializationSchema.java`

```java
package io.github.atengk.flink.connector.serde;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.flink.model.UserBehaviorEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 用户行为 JSON 反序列化器
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserBehaviorJsonDeserializationSchema implements DeserializationSchema<UserBehaviorEvent> {

    /**
     * 反序列化用户行为事件
     *
     * @param message 字节数组
     * @return 用户行为事件
     * @throws IOException 反序列化异常
     */
    @Override
    public UserBehaviorEvent deserialize(byte[] message) throws IOException {
        String json = new String(message, StandardCharsets.UTF_8);
        if (StrUtil.isBlank(json)) {
            return null;
        }

        try {
            return JSONUtil.toBean(json, UserBehaviorEvent.class);
        } catch (Exception e) {
            log.warn("用户行为JSON反序列化失败，原因：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 判断流是否结束
     *
     * @param nextElement 下一个元素
     * @return 是否结束
     */
    @Override
    public boolean isEndOfStream(UserBehaviorEvent nextElement) {
        return false;
    }

    /**
     * 获取反序列化类型信息
     *
     * @return 类型信息
     */
    @Override
    public TypeInformation<UserBehaviorEvent> getProducedType() {
        return TypeInformation.of(UserBehaviorEvent.class);
    }
}
```

Kafka Sink 使用自定义序列化器示例：

```java
KafkaSink<UserBehaviorEvent> kafkaSink = KafkaSink.<UserBehaviorEvent>builder()
        .setBootstrapServers("kafka-prod-01:9092,kafka-prod-02:9092")
        .setRecordSerializer(
                KafkaRecordSerializationSchema.builder()
                        .setTopic("user_behavior_clean")
                        .setValueSerializationSchema(new UserBehaviorJsonSerializationSchema())
                        .build()
        )
        .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
        .build();
```

自定义序列化器开发建议：

1. 序列化格式必须有版本管理。
2. JSON 序列化要明确时间格式、空字段和字段命名策略。
3. 反序列化失败不应静默丢失，建议进入脏数据链路。
4. 状态对象序列化变更会影响 Savepoint 兼容性。
5. Protobuf 和 Avro 应优先基于 Schema 文件生成类。
6. Kafka Key 和 Value 的序列化策略应分开设计，聚合或 Upsert 场景必须重视 Key。


## 序列化与反序列化

本章节用于说明 Flink 作业中的类型系统、序列化机制、Schema 演进和性能优化。序列化不仅影响 Kafka、文件、Avro、Protobuf 等外部数据格式，也直接影响 Flink 网络传输、状态存储、Checkpoint、Savepoint 恢复和作业升级兼容性。Flink 官方文档说明，Flink 有自己的类型描述、类型提取和类型序列化框架，用于分析 DataStream 中元素类型并选择高效的执行与序列化策略。([apache.googlesource.com](https://apache.googlesource.com/flink/%2B/master/docs/content/docs/dev/datastream/fault-tolerance/serialization/types_serialization.md?utm_source=chatgpt.com))

### Flink 类型系统

Flink 类型系统用于识别 DataStream 中每个元素的类型，并为其生成对应的 `TypeInformation` 和序列化器。Flink 会根据类型选择更高效的序列化方式，例如基础类型、Tuple、POJO、集合类型、Hadoop Writable 或通用类型。官方类型文档列出了 Java Tuple、Java POJO、Primitive Types、Common Collection Types、Regular Classes、Values、Hadoop Writables 和 Special Types 等类型分类。([apache.googlesource.com](https://apache.googlesource.com/flink/%2B/master/docs/content/docs/dev/datastream/fault-tolerance/serialization/types_serialization.md?utm_source=chatgpt.com))

常见类型选择建议如下：

| 类型         | 示例                        | 建议                                 |
| ------------ | --------------------------- | ------------------------------------ |
| 基础类型     | `String`、`Long`、`Integer` | 适合简单字段和测试数据               |
| Tuple        | `Tuple2<String, Long>`      | 适合临时转换，不建议复杂业务长期使用 |
| POJO         | `UserBehaviorEvent`         | 推荐业务模型使用                     |
| Avro 类      | Avro 生成类                 | 适合强 Schema 和 Schema 演进         |
| Protobuf 类  | Protobuf 生成类             | 适合跨语言和高性能消息               |
| Row          | `Row`、`RowData`            | 常见于 Table API / SQL               |
| Generic Type | 普通复杂类                  | 可能退化到 Kryo，需谨慎              |

推荐在 DataStream 中使用语义明确的 Java Bean 或 Avro / Protobuf 生成类，不建议长期使用 `Map<String, Object>`、`JSONObject` 或弱类型结构贯穿主链路。弱类型结构会削弱类型检查、增加序列化开销，并提高状态升级风险。

查看类型信息示例：

```java
DataStream<UserBehaviorEvent> eventStream = sourceStream
        .process(new UserBehaviorJsonProcessFunction())
        .name("parse-user-behavior")
        .uid("parse-user-behavior");

System.out.println(eventStream.getType());
```

如果 Lambda、泛型或复杂集合导致 Flink 无法正确推断类型，可以显式指定返回类型：

```java
DataStream<UserBehaviorEvent> eventStream = jsonStream
        .map(JSONUtil::toJsonStr)
        .returns(String.class)
        .map(json -> JSONUtil.toBean(json, UserBehaviorEvent.class))
        .returns(UserBehaviorEvent.class)
        .name("json-to-user-behavior")
        .uid("json-to-user-behavior");
```

类型系统使用建议：

1. 业务主模型优先使用标准 POJO。
2. 状态中的类型要稳定，避免频繁变更字段结构。
3. 泛型、集合、匿名类和 Lambda 返回复杂对象时，必要时使用 `.returns(...)` 显式声明类型。
4. Key 类型必须稳定，不要使用可变对象、数组或字段会变更的对象作为 Key。
5. Flink 作业升级前应检查状态类型和序列化器兼容性。
6. 对于长期运行作业，类型设计要优先考虑 Savepoint 恢复，而不是只考虑当前代码能运行。

### Kryo 序列化

Kryo 是 Flink 处理无法识别为高效类型时的通用序列化回退机制。Flink 旧版本类型文档说明，POJO 中标准类型会由 Flink 内置序列化器处理，其他类型可能回退到 Kryo；也可以通过配置强制使用 Kryo，或为某些类型注册自定义 Kryo Serializer。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.13/docs/dev/serialization/types_serialization/?utm_source=chatgpt.com))

Kryo 适合兼容一些普通 Java 类，但不适合作为状态 Schema 演进的长期方案。Flink 状态 Schema 演进文档明确说明，状态 Schema 演进当前推荐使用 POJO 或 Avro；Kryo 不能用于 Schema 演进，因为框架无法验证是否发生了不兼容变更。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/fault-tolerance/serialization/schema_evolution/?utm_source=chatgpt.com))

Kryo 注册示例：

```java
StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

// 注册需要 Kryo 序列化的业务类型，减少运行期动态注册成本
env.getConfig().registerKryoType(UserBehaviorWideEvent.class);

// 为特定类型添加默认 Kryo Serializer，适合确实需要自定义序列化的场景
// env.getConfig().addDefaultKryoSerializer(MyClass.class, MyKryoSerializer.class);
```

强制 Kryo 示例：

```java
StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

// 不推荐在生产作业中随意开启，除非已经完成状态兼容性和性能验证
env.getConfig().enableForceKryo();
```

Kryo 使用建议：

1. 不要把 Kryo 作为生产状态类型的默认方案。
2. 如果对象会进入 Flink State，优先使用 POJO 或 Avro。
3. 需要 Kryo 时，尽量显式注册类型，减少运行时动态注册开销。
4. 使用 Kryo 的状态类型不适合做 Schema 演进。
5. Kryo 序列化问题经常在作业运行、Checkpoint 或 Savepoint 恢复时暴露，不能只靠本地单次运行验证。
6. 发现 `Class cannot be serialized`、`KryoException`、`NoClassDefFoundError` 时，应优先检查类型定义、依赖打包和序列化器注册。

### Avro 序列化

Avro 适合强 Schema、跨系统传输、Schema 演进和数据湖场景。Flink 官方 Avro Format 文档说明，Apache Avro Format 可以基于 Avro Schema 读写 Avro 数据；在 Table 生态中，Avro Schema 通常由表 Schema 推导。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/connectors/table/formats/avro/?utm_source=chatgpt.com))

Maven 依赖示例：

```xml
<!-- Flink Avro 支持，用于 Avro 格式、Avro Schema 和 Avro 序列化 -->
<dependency>
    <groupId>org.apache.flink</groupId>
    <artifactId>flink-avro</artifactId>
    <version>${flink.version}</version>
</dependency>

<!-- Apache Avro 核心依赖，用于 Schema、GenericRecord 和 SpecificRecord -->
<dependency>
    <groupId>org.apache.avro</groupId>
    <artifactId>avro</artifactId>
    <version>1.12.0</version>
</dependency>
```

Avro Schema 示例：

文件位置：`src/main/avro/user_behavior.avsc`

```json
{
  "type": "record",
  "name": "UserBehaviorAvro",
  "namespace": "io.github.atengk.flink.avro",
  "fields": [
    {
      "name": "userId",
      "type": "string"
    },
    {
      "name": "itemId",
      "type": ["null", "string"],
      "default": null
    },
    {
      "name": "categoryId",
      "type": ["null", "string"],
      "default": null
    },
    {
      "name": "eventType",
      "type": "string"
    },
    {
      "name": "eventTime",
      "type": "long"
    }
  ]
}
```

Table API / SQL 中使用 Avro Format 示例：

```sql
CREATE TEMPORARY TABLE user_behavior_avro_source (
    userId STRING,
    itemId STRING,
    categoryId STRING,
    eventType STRING,
    eventTime BIGINT
) WITH (
    'connector' = 'kafka',
    'topic' = 'user_behavior_avro',
    'properties.bootstrap.servers' = 'kafka-prod-01:9092',
    'properties.group.id' = 'user-behavior-avro-job',
    'format' = 'avro'
);
```

Avro 使用建议：

1. Avro Schema 文件必须纳入版本管理。
2. 新增字段应设置默认值，避免旧数据读取失败。
3. 删除字段、字段改名、类型变更必须做兼容性评估。
4. 状态中使用 Avro 类型时，恢复前要验证 Savepoint 兼容性。
5. Kafka Avro 场景建议配合 Schema Registry 或企业内部 Schema 管理平台。
6. 对强 Schema、数据湖、跨团队数据契约场景，Avro 通常比普通 JSON 更稳健。

### Protobuf 序列化

Protobuf 适合高性能、强类型、跨语言通信和二进制消息传输场景。Flink Protobuf Format 文档说明，Protobuf Format 可以基于 Protobuf 生成类读写数据，使用时需要配置 `format='protobuf'` 和 Protobuf 生成类完整类名；它还支持解析失败忽略等配置选项。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/connectors/table/formats/protobuf/?utm_source=chatgpt.com))

Protobuf 文件示例：

文件位置：`src/main/proto/user_behavior.proto`

```proto
syntax = "proto3";

package io.github.atengk.flink.proto;

option java_package = "io.github.atengk.flink.proto";
option java_outer_classname = "UserBehaviorProto";

message UserBehavior {
  string user_id = 1;
  string item_id = 2;
  string category_id = 3;
  string event_type = 4;
  int64 event_time = 5;
}
```

Maven 依赖示例：

```xml
<!-- Protobuf Java 运行时，用于 Protobuf 消息解析和构建 -->
<dependency>
    <groupId>com.google.protobuf</groupId>
    <artifactId>protobuf-java</artifactId>
    <version>4.29.3</version>
</dependency>
```

DataStream 反序列化示例：

文件位置：`flink-connector/src/main/java/io/github/atengk/flink/connector/serde/UserBehaviorProtobufDeserializationSchema.java`

```java
package io.github.atengk.flink.connector.serde;

import io.github.atengk.flink.model.UserBehaviorEvent;
import io.github.atengk.flink.proto.UserBehaviorProto;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.io.IOException;

/**
 * 用户行为 Protobuf 反序列化器
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserBehaviorProtobufDeserializationSchema implements DeserializationSchema<UserBehaviorEvent> {

    /**
     * 反序列化 Protobuf 数据
     *
     * @param message Protobuf 字节数组
     * @return 用户行为事件
     * @throws IOException 反序列化异常
     */
    @Override
    public UserBehaviorEvent deserialize(byte[] message) throws IOException {
        try {
            UserBehaviorProto.UserBehavior value = UserBehaviorProto.UserBehavior.parseFrom(message);
            return new UserBehaviorEvent(
                    value.getUserId(),
                    value.getItemId(),
                    value.getCategoryId(),
                    value.getEventType(),
                    value.getEventTime()
            );
        } catch (Exception e) {
            log.warn("Protobuf反序列化失败，原因：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 判断是否结束流
     *
     * @param nextElement 下一个元素
     * @return 是否结束
     */
    @Override
    public boolean isEndOfStream(UserBehaviorEvent nextElement) {
        return false;
    }

    /**
     * 获取输出类型
     *
     * @return 类型信息
     */
    @Override
    public TypeInformation<UserBehaviorEvent> getProducedType() {
        return TypeInformation.of(UserBehaviorEvent.class);
    }
}
```

SQL Protobuf Format 示例：

```sql
CREATE TEMPORARY TABLE user_behavior_proto_source (
    user_id STRING,
    item_id STRING,
    category_id STRING,
    event_type STRING,
    event_time BIGINT
) WITH (
    'connector' = 'kafka',
    'topic' = 'user_behavior_proto',
    'properties.bootstrap.servers' = 'kafka-prod-01:9092',
    'properties.group.id' = 'user-behavior-proto-job',
    'format' = 'protobuf',
    'protobuf.message-class-name' = 'io.github.atengk.flink.proto.UserBehaviorProto$UserBehavior',
    'protobuf.ignore-parse-errors' = 'true'
);
```

Protobuf 使用建议：

1. `.proto` 文件必须版本化管理。
2. 字段编号不能复用，删除字段后应保留编号。
3. 新增字段要考虑默认值和旧客户端兼容。
4. Protobuf 适合高吞吐链路，但调试成本高于 JSON。
5. Table/SQL 使用 Protobuf 时必须配置正确的生成类完整类名。
6. 解析失败不能静默丢弃，应结合脏数据链路处理。

### POJO 序列化

POJO 是 Flink Java 项目中最常用的业务模型类型。Flink 会识别符合条件的 Java 类为 POJO，并为其生成更高效的序列化器。Flink 类型系统资料说明，POJO 需要是 public 独立类，不能是非静态内部类；需要 public 无参构造器；字段要么是 public，要么有符合 Java Bean 规范的 getter 和 setter。([Apache 维基](https://cwiki.apache.org/confluence/display/FLINK/Type%2BSystem%2C%2BType%2BExtraction%2C%2BSerialization?utm_source=chatgpt.com))

标准 POJO 示例：

文件位置：`flink-model/src/main/java/io/github/atengk/flink/model/StandardPojoEvent.java`

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 标准 POJO 事件模型
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StandardPojoEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;

    private String eventType;

    private Long eventTime;
}
```

不推荐写法：

```java
// 不推荐：字段 final 且没有无参构造器，Flink 可能无法按 POJO 识别
public class BadEvent {

    private final String eventId;

    public BadEvent(String eventId) {
        this.eventId = eventId;
    }
}
```

POJO 序列化建议：

1. 模型类使用 `public` 顶级类。
2. 提供无参构造器。
3. 字段提供 getter / setter。
4. 不建议在状态模型中使用 `Object`、原始 Map、复杂泛型和匿名内部类。
5. 状态 POJO 的包名、类名和字段类型不要随意变更。
6. 需要长期升级兼容的状态类型，优先选择 POJO 或 Avro。

### Schema 演进

Schema 演进用于处理长期运行作业中的模型变更。Flink 状态 Schema 演进文档说明，当前状态 Schema 演进主要支持 POJO 和 Avro 类型；POJO 支持删除字段、新增字段，但字段类型不能变更，POJO 类名和命名空间不能变更；Avro 状态类型只要符合 Avro Schema 解析兼容规则，就支持 Schema 演进。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/fault-tolerance/serialization/schema_evolution/?utm_source=chatgpt.com))

常见 Schema 变更风险如下：

| 变更类型     | 风险 | 建议                         |
| ------------ | ---- | ---------------------------- |
| 新增可选字段 | 低   | 设置默认值，模型允许 null    |
| 新增必填字段 | 中   | 需要灰度和默认值策略         |
| 删除字段     | 中   | 确认下游和状态不再依赖       |
| 字段改名     | 高   | 等同于删除旧字段并新增新字段 |
| 字段类型变更 | 高   | 状态和下游都可能不兼容       |
| 类名变更     | 高   | POJO 状态恢复可能失败        |
| 包名变更     | 高   | 状态类型恢复可能失败         |
| Key 类型变更 | 极高 | 不支持直接演进               |

Flink 文档明确指出，Key 的 Schema 演进不受支持，因为 Key 结构变化可能导致非确定性行为；同时 Kryo 不能用于 Schema 演进，因为框架无法验证不兼容变更。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/fault-tolerance/serialization/schema_evolution/?utm_source=chatgpt.com))

Schema 演进流程建议：

```text
提出 Schema 变更
  -> 判断是否影响 Source / State / Sink
  -> 检查 POJO 或 Avro 兼容性
  -> 测试环境从 Savepoint 恢复
  -> 验证新旧数据解析
  -> 灰度发布
  -> 保留回滚 Savepoint
```

Schema 演进建议：

1. 进入状态的模型变更必须单独评审。
2. Key 类型和 Key 字段不应变更。
3. POJO 字段类型不应变更。
4. Avro Schema 变更应遵循 Avro 兼容规则。
5. 修改包名、类名、状态名、算子 `uid()` 都可能影响恢复。
6. 上线前必须用生产等价 Savepoint 做恢复验证。

### 序列化性能优化

序列化性能会影响网络 Shuffle、状态读写、Checkpoint 耗时、RocksDB 负载和 Sink 写入吞吐。性能优化应优先从类型设计和数据结构简化入手，而不是直接更换序列化框架。

常见优化方向如下：

| 优化项   | 建议                                                       |
| -------- | ---------------------------------------------------------- |
| 类型选择 | 优先使用 POJO、Avro、Protobuf 等明确类型                   |
| 字段设计 | 减少无用字段，避免大对象进入状态                           |
| 集合状态 | 大集合使用 MapState/ListState，不要整个集合放入 ValueState |
| Kryo     | 避免无意识退化到 Kryo                                      |
| Protobuf | 高吞吐跨语言链路可使用                                     |
| Avro     | 强 Schema 和演进场景可使用                                 |
| JSON     | 调试友好，但高吞吐链路要评估 CPU 开销                      |
| 状态对象 | 保持稳定、轻量、可演进                                     |

序列化性能排查方向：

```bash
# 查看 Maven 依赖冲突，重点关注 Kryo、Avro、Protobuf、Jackson
mvn dependency:tree

# 查看日志中是否存在 Kryo、序列化器、ClassNotFound 等异常
grep -E "Kryo|Serialization|Serializer|ClassNotFound|NoClassDefFoundError" logs/*.log
```

命令说明：`mvn dependency:tree` 用于排查序列化相关依赖冲突；`grep` 用于从日志中快速定位序列化异常、类缺失和运行时兼容问题。

性能优化建议：

1. 高频流中避免反复 JSON 字符串与对象互转。
2. 进入状态前删除不必要字段。
3. 窗口和状态中不要保存完整原始消息，除非业务必须。
4. 使用 RocksDB 时要关注序列化对象大小。
5. 对复杂类型显式声明 `.returns(...)`，减少类型推断问题。
6. 修改序列化相关依赖后，要执行 Checkpoint 和 Savepoint 恢复测试。

## 迟到数据与脏数据处理

本章节用于说明实时作业中迟到数据和脏数据的识别、分流、落盘、告警和修复策略。迟到数据通常由事件时间乱序、客户端离线、Kafka 积压、网络延迟或历史回放导致；脏数据通常由格式错误、字段缺失、枚举非法、时间异常、Schema 不兼容或业务校验失败导致。两者都不应简单丢弃，必须形成可观测、可追溯、可修复的处理链路。

Flink 窗口文档说明，默认情况下，Watermark 通过窗口结束时间后到达的元素会被丢弃；可以使用 `allowedLateness(...)` 允许一定范围内的迟到数据继续参与窗口计算，也可以使用 `sideOutputLateData(...)` 将被丢弃的迟到数据输出到侧输出流。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.19/docs/dev/datastream/operators/windows/?utm_source=chatgpt.com))

### 迟到数据识别

迟到数据识别依赖 Event Time 和 Watermark。当一条数据到达时，如果它所属窗口已经被 Watermark 推进到结束之后，则该数据属于迟到数据。对于窗口计算，迟到数据是否还能参与计算取决于 `allowedLateness` 配置；超过允许迟到范围的数据通常会被丢弃或输出到侧输出流。

迟到数据判断逻辑：

```text
事件时间 eventTime
  |
  v
计算所属窗口 [windowStart, windowEnd)
  |
  v
当前 Watermark 是否 >= windowEnd
  |
  |-- 否：正常数据
  |
  |-- 是：迟到数据
            |
            v
       是否在 allowedLateness 范围内
            |
            |-- 是：允许迟到，更新窗口结果
            |-- 否：超迟到，进入侧输出流或丢弃
```

迟到数据常见原因：

| 原因           | 说明                            |
| -------------- | ------------------------------- |
| 客户端离线缓存 | 移动端或设备端离线后批量上报    |
| Kafka 积压     | 消费落后导致旧数据晚到          |
| 多分区乱序     | 不同 Kafka 分区数据到达顺序不同 |
| 网络延迟       | 跨地域、弱网、重试造成延迟      |
| 历史回放       | 补数或重放旧数据                |
| 时间字段错误   | 事件时间生成异常或单位错误      |

DataStream 中设置 Event Time 和 Watermark 示例：

```java
WatermarkStrategy<UserBehaviorEvent> watermarkStrategy = WatermarkStrategy
        .<UserBehaviorEvent>forBoundedOutOfOrderness(Duration.ofSeconds(10))
        .withTimestampAssigner((event, timestamp) -> event.getEventTime());

DataStream<UserBehaviorEvent> eventTimeStream = eventStream
        .assignTimestampsAndWatermarks(watermarkStrategy)
        .name("assign-watermark")
        .uid("assign-watermark");
```

迟到数据识别建议：

1. 迟到判断必须基于事件时间，不应基于处理时间。
2. 事件时间字段必须校验，避免单位秒和毫秒混用。
3. Watermark 延迟策略要基于真实数据乱序分布设置。
4. Kafka 多分区、低频分区和空闲分区要配置空闲检测。
5. 迟到数据量需要监控，异常升高通常意味着上游延迟或时间字段异常。

### 允许迟到配置

允许迟到配置用于让窗口在 Watermark 通过窗口结束时间后继续保留一段时间，以接收一定范围内的迟到数据。Flink 窗口文档说明，当 `allowedLateness` 大于 0 时，窗口内容会在 Watermark 通过窗口结束后继续保留；如果迟到但未被丢弃的数据到达，窗口可能再次触发输出，这类输出称为 late firing。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.19/docs/dev/datastream/operators/windows/?utm_source=chatgpt.com))

允许迟到配置示例：

```java
OutputTag<UserBehaviorEvent> lateOutputTag = new OutputTag<UserBehaviorEvent>("late-user-behavior") {
};

SingleOutputStreamOperator<WindowEventCount> resultStream = eventStream
        .keyBy(UserBehaviorEvent::getEventType)
        .window(TumblingEventTimeWindows.of(Time.minutes(1)))
        .allowedLateness(Time.seconds(30))
        .sideOutputLateData(lateOutputTag)
        .aggregate(new EventCountAggregateFunction(), new WindowEventCountFunction())
        .name("event-count-window-with-lateness")
        .uid("event-count-window-with-lateness");

DataStream<UserBehaviorEvent> lateStream = resultStream
        .getSideOutput(lateOutputTag)
        .name("late-user-behavior-stream");
```

允许迟到配置影响如下：

| 配置                  | 影响                             |
| --------------------- | -------------------------------- |
| `allowedLateness = 0` | Watermark 过后迟到数据默认被丢弃 |
| `allowedLateness > 0` | 窗口状态继续保留，可接收迟到数据 |
| 允许迟到越长          | 准确性更高，但状态保留更久       |
| 允许迟到越短          | 输出更及时，但迟到数据更多       |
| late firing           | 同一窗口可能多次输出更新结果     |

允许迟到配置建议：

1. 指标类作业可以适当设置允许迟到，例如 30 秒、1 分钟或 5 分钟。
2. 告警类作业通常更重视时效性，允许迟到不宜过长。
3. 允许迟到会增加状态保留时间，需要关注 Checkpoint 和状态大小。
4. late firing 会导致同一窗口多次输出，下游必须支持幂等更新。
5. 超过允许迟到范围的数据应输出到侧输出流，供修复或审计。

### 侧输出流处理

侧输出流用于处理迟到数据、脏数据、异常数据、告警数据和分流数据。Flink 官方 Side Output 文档说明，侧输出流通过 `OutputTag` 标识，可以从 `ProcessFunction`、`KeyedProcessFunction`、`CoProcessFunction`、`ProcessWindowFunction` 等函数中输出，并且侧输出流类型可以不同于主流类型。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.19/docs/dev/datastream/operators/windows/?utm_source=chatgpt.com))

迟到数据侧输出示例：

```java
OutputTag<UserBehaviorEvent> lateOutputTag = new OutputTag<UserBehaviorEvent>("late-user-behavior") {
};

SingleOutputStreamOperator<WindowEventCount> resultStream = eventStream
        .keyBy(UserBehaviorEvent::getEventType)
        .window(TumblingEventTimeWindows.of(Time.minutes(1)))
        .allowedLateness(Time.seconds(30))
        .sideOutputLateData(lateOutputTag)
        .aggregate(new EventCountAggregateFunction(), new WindowEventCountFunction())
        .name("window-with-late-side-output")
        .uid("window-with-late-side-output");

DataStream<UserBehaviorEvent> lateStream = resultStream.getSideOutput(lateOutputTag);
```

脏数据侧输出示例：

```java
SingleOutputStreamOperator<UserBehaviorEvent> cleanStream = rawStream
        .process(new UserBehaviorJsonProcessFunction())
        .name("parse-json-with-dirty-output")
        .uid("parse-json-with-dirty-output");

DataStream<DirtyData> dirtyStream = cleanStream
        .getSideOutput(UserBehaviorJsonProcessFunction.DIRTY_TAG)
        .name("dirty-data-stream");
```

侧输出流处理建议：

1. 迟到数据和脏数据应使用不同的 `OutputTag`。
2. `OutputTag` 建议定义为 `public static final`，避免多个地方重复创建。
3. 侧输出流必须接入 Sink，否则数据只存在于流图中，无法追溯。
4. 侧输出流的 Sink 应与主流 Sink 隔离，避免脏数据影响主链路。
5. 对侧输出流也要设置监控，例如数量、比例、原因分布和输出失败次数。
6. 生产环境不要只使用 `print()`，应写入 Kafka、文件、数据湖或专门表。

### 脏数据分类

脏数据分类用于把异常数据按原因分组，便于监控、告警、修复和责任定位。所有脏数据都只记录为“解析失败”会降低可观测性，不利于定位上游系统、客户端或 Schema 问题。

推荐分类如下：

| 分类编码                 | 说明              | 示例                   |
| ------------------------ | ----------------- | ---------------------- |
| `EMPTY_DATA`             | 空数据            | null、空字符串         |
| `JSON_PARSE_ERROR`       | JSON 解析失败     | 缺少括号、字段类型错误 |
| `AVRO_PARSE_ERROR`       | Avro 解析失败     | Schema 不兼容          |
| `PROTOBUF_PARSE_ERROR`   | Protobuf 解析失败 | 二进制消息损坏         |
| `MISSING_REQUIRED_FIELD` | 必填字段缺失      | `userId` 为空          |
| `INVALID_ENUM`           | 枚举非法          | `eventType=xxx`        |
| `INVALID_TIME`           | 时间字段异常      | 时间为 0、未来时间过大 |
| `SCHEMA_MISMATCH`        | Schema 不匹配     | 字段类型变更           |
| `BUSINESS_INVALID`       | 业务校验失败      | 金额为负、状态非法     |
| `SINK_FORMAT_ERROR`      | Sink 数据格式异常 | 目标字段无法转换       |

脏数据枚举示例：

文件位置：`flink-model/src/main/java/io/github/atengk/flink/model/DirtyReason.java`

```java
package io.github.atengk.flink.model;

/**
 * 脏数据原因
 *
 * @author Ateng
 * @since 2026-05-11
 */
public enum DirtyReason {

    EMPTY_DATA,

    JSON_PARSE_ERROR,

    AVRO_PARSE_ERROR,

    PROTOBUF_PARSE_ERROR,

    MISSING_REQUIRED_FIELD,

    INVALID_ENUM,

    INVALID_TIME,

    SCHEMA_MISMATCH,

    BUSINESS_INVALID,

    SINK_FORMAT_ERROR
}
```

脏数据模型建议包含以下字段：

| 字段           | 说明            |
| -------------- | --------------- |
| `jobName`      | 作业名称        |
| `source`       | 数据来源        |
| `reason`       | 脏数据分类      |
| `errorMessage` | 错误摘要        |
| `payload`      | 原始数据        |
| `eventTime`    | 事件时间        |
| `createTime`   | 处理时间        |
| `traceId`      | 链路追踪 ID     |
| `topic`        | Kafka Topic     |
| `partition`    | Kafka Partition |
| `offset`       | Kafka Offset    |

脏数据分类建议：

1. 分类编码要稳定，便于监控统计。
2. 异常信息要简洁，不要包含大段堆栈。
3. 原始数据可保留，但敏感字段应脱敏。
4. 数据来源信息越完整，后续修复越容易。
5. 脏数据原因需要与上游团队对齐，方便推动修复。

### 脏数据落盘

脏数据落盘用于保留无法进入主链路的数据，便于后续排查、修复、回放和审计。脏数据不应只打印日志，因为日志可能滚动删除、格式不稳定且难以批量回放。

常见落盘方式如下：

| 方式                 | 适用场景                   |
| -------------------- | -------------------------- |
| Kafka Topic          | 实时查看、后续修复作业消费 |
| HDFS / S3 / OSS 文件 | 大量脏数据长期保存         |
| Iceberg / Hudi 表    | 可查询、可追溯、可批量修复 |
| Doris / StarRocks    | 实时分析脏数据原因         |
| Elasticsearch        | 按 traceId、原因、原文搜索 |
| MySQL                | 小规模脏数据管理后台查看   |

脏数据写入 Kafka 示例：

```java
KafkaSink<String> dirtyKafkaSink = KafkaSink.<String>builder()
        .setBootstrapServers("kafka-prod-01:9092,kafka-prod-02:9092")
        .setRecordSerializer(
                KafkaRecordSerializationSchema.builder()
                        .setTopic("dirty_user_behavior")
                        .setValueSerializationSchema(new SimpleStringSchema())
                        .build()
        )
        .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
        .build();

dirtyStream
        .map(JSONUtil::toJsonStr)
        .name("dirty-data-to-json")
        .uid("dirty-data-to-json")
        .sinkTo(dirtyKafkaSink)
        .name("dirty-data-kafka-sink")
        .uid("dirty-data-kafka-sink");
```

脏数据写入文件示例：

```java
FileSink<String> dirtyFileSink = FileSink
        .forRowFormat(
                new Path("hdfs:///flink/dirty/user-behavior-job"),
                new SimpleStringEncoder<String>("UTF-8")
        )
        .withRollingPolicy(
                DefaultRollingPolicy.builder()
                        .withRolloverInterval(Duration.ofMinutes(15))
                        .withInactivityInterval(Duration.ofMinutes(5))
                        .withMaxPartSize(MemorySize.ofMebiBytes(128))
                        .build()
        )
        .build();

dirtyStream
        .map(JSONUtil::toJsonStr)
        .name("dirty-data-json-format")
        .uid("dirty-data-json-format")
        .sinkTo(dirtyFileSink)
        .name("dirty-data-file-sink")
        .uid("dirty-data-file-sink");
```

脏数据落盘建议：

1. 生产环境脏数据必须落盘。
2. 落盘格式建议使用 JSON Lines，便于回放。
3. 路径或 Topic 应按环境、作业、日期隔离。
4. 原始数据中的敏感字段要脱敏。
5. 脏数据落盘 Sink 失败时要告警。
6. 脏数据保存周期应结合合规、存储成本和修复周期设置。

### 脏数据告警

脏数据告警用于在异常数据量或异常比例超过阈值时通知开发和运维人员。告警不应逐条发送，而应按窗口聚合后判断阈值，避免上游异常时告警系统被刷爆。

告警维度建议：

| 维度       | 示例                      |
| ---------- | ------------------------- |
| 作业名称   | `user-behavior-job`       |
| 数据来源   | `kafka:user_behavior_log` |
| 脏数据原因 | `JSON_PARSE_ERROR`        |
| 时间窗口   | 1 分钟、5 分钟            |
| 脏数据数量 | 窗口内异常条数            |
| 脏数据比例 | 脏数据 / 总输入           |
| 阈值       | 超过 100 条或超过 1%      |

脏数据窗口统计示例：

```java
DataStream<WindowEventCount> dirtyCountStream = dirtyStream
        .map(dirty -> new UserBehaviorEvent(
                "",
                "",
                "",
                dirty.getReason(),
                dirty.getCreateTime()
        ))
        .assignTimestampsAndWatermarks(
                WatermarkStrategy
                        .<UserBehaviorEvent>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                        .withTimestampAssigner((event, timestamp) -> event.getEventTime())
        )
        .keyBy(UserBehaviorEvent::getEventType)
        .window(TumblingEventTimeWindows.of(Time.minutes(1)))
        .aggregate(new EventCountAggregateFunction(), new WindowEventCountFunction())
        .name("dirty-data-count-window")
        .uid("dirty-data-count-window");
```

脏数据告警建议：

1. 告警应按窗口聚合，不要逐条告警。
2. 告警条件应支持绝对数量和比例两种阈值。
3. 不同原因可设置不同告警等级。
4. 告警内容要包含作业名、原因、数量、样例和排查路径。
5. 告警需要抑制和去重，避免重复轰炸。
6. 脏数据异常升高时，应优先检查上游发布、Schema 变更和时间字段变更。

### 数据修复策略

数据修复策略用于将脏数据、迟到数据、漏写数据或错误结果重新处理并补偿到下游。修复策略通常包括脏数据回放、Kafka Offset 回放、文件补数、离线重算、手工修正和下游覆盖写。

常见修复方式如下：

| 修复方式          | 适用场景                             |
| ----------------- | ------------------------------------ |
| 脏数据修复回放    | 原始数据保留，修复解析逻辑后重新消费 |
| Kafka Offset 回放 | 源 Topic 仍保留历史数据              |
| 文件补数          | 已落盘数据重新读取                   |
| 离线重算          | 指标或宽表按时间范围重新计算         |
| Sink 覆盖写       | 下游支持主键或分区覆盖               |
| 人工修正          | 小批量异常数据管理后台处理           |

修复作业流程：

```text
确定修复范围
  -> 获取原始数据
  -> 修复解析或业务逻辑
  -> 使用补数作业重跑
  -> 写入临时表或补偿 Topic
  -> 校验结果
  -> 覆盖或合并到正式结果
```

脏数据回放命令示例：

```bash
# 按指定时间范围从脏数据文件目录启动补数作业
flink run \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorRepairJob \
  target/flink-job-user-behavior.jar \
  --env prod \
  --jobName user-behavior-dirty-repair-job \
  --input hdfs:///flink/dirty/user-behavior-job/dt=2026-05-11 \
  --outputTopic user_behavior_clean_repair \
  --startTime 2026-05-11T00:00:00 \
  --endTime 2026-05-11T23:59:59
```

命令说明：`--input` 指定脏数据落盘路径，`--outputTopic` 指定修复后的补偿 Topic，`--startTime` 和 `--endTime` 限定修复范围。生产修复时建议先写入临时 Topic 或临时表，校验通过后再合并到正式结果。

数据修复建议：

1. 修复必须有明确时间范围和数据范围。
2. 修复输出应先进入临时表或补偿 Topic。
3. 下游合并必须依赖幂等键或主键。
4. 修复前后要做行数、金额、唯一键和指标口径校验。
5. 修复任务和实时任务应隔离消费组、输出路径和资源队列。
6. 修复过程要记录执行人、执行时间、参数、输入范围和校验结果。


## 性能优化

本章节用于说明 Flink 作业在吞吐、延迟、状态、Checkpoint、Kafka 消费、反压、数据倾斜和 JVM GC 等方面的优化方法。Flink 性能优化不能只靠调大资源，应该先定位瓶颈来源，再分别从并行度、算子链、状态后端、网络、外部系统、数据分布和 JVM 参数等层面处理。

性能优化的基本流程如下：

```text
确认性能目标
  -> 查看 Flink Web UI 指标
  -> 判断瓶颈算子
  -> 分析 Source、Transformation、State、Sink
  -> 调整并行度和资源
  -> 优化状态、Checkpoint、外部系统
  -> 压测验证
  -> 固化配置和监控
```

### 并行度优化

并行度决定同一个算子会被拆分为多少个并行子任务。并行度过低会导致单个 Subtask 压力过大，吞吐不足；并行度过高会增加资源消耗、网络 Shuffle、状态分片、Checkpoint 和调度开销。Flink 架构文档说明，TaskManager 可以执行多个 Subtask，算子子任务会被调度到 Task Slot 中执行。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/concepts/flink-architecture/?utm_source=chatgpt.com))

并行度设置层级如下：

| 层级           | 示例                                      | 说明               |
| -------------- | ----------------------------------------- | ------------------ |
| 集群默认并行度 | `parallelism.default`                     | 未显式设置时使用   |
| 作业默认并行度 | `env.setParallelism(4)`                   | 当前作业全局默认值 |
| 算子并行度     | `.setParallelism(8)`                      | 覆盖单个算子       |
| 提交参数       | `-p 8`                                    | 提交作业时指定     |
| SQL 配置       | `table.exec.resource.default-parallelism` | Table/SQL 作业使用 |

Java 配置示例：

```java
StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

// 作业默认并行度
env.setParallelism(4);

// Kafka Source 单独设置并行度
DataStream<String> sourceStream = env
        .fromSource(kafkaSource, watermarkStrategy, "kafka-source")
        .name("kafka-source")
        .uid("kafka-source")
        .setParallelism(8);

// Sink 单独设置并行度
sourceStream
        .sinkTo(kafkaSink)
        .name("kafka-sink")
        .uid("kafka-sink")
        .setParallelism(4);
```

提交时指定并行度：

```bash
flink run \
  -p 8 \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  target/flink-job-user-behavior.jar \
  --env prod \
  --jobName user-behavior-job
```

并行度优化建议：

1. Kafka Source 并行度通常不应超过 Topic 分区数，超过后部分 Subtask 可能无数据可读。
2. `keyBy` 后的算子并行度要结合 Key 基数和数据分布设置。
3. Sink 并行度受下游系统限制，例如数据库连接数、Kafka 分区数、Doris/StarRocks 写入能力。
4. 窗口、状态、Join 类算子提高并行度会改变状态分布，生产调整前应验证 Savepoint 恢复。
5. 对 CPU 密集型算子，提高并行度通常有效；对外部系统瓶颈，盲目提高并行度可能压垮下游。
6. 优化顺序建议是先找瓶颈算子，再单独调整该算子并行度，而不是整体调大。

### Slot 配置优化

Task Slot 是 TaskManager 提供的资源调度单位。Flink 架构文档说明，每个 TaskManager 是一个 JVM 进程，可以在不同线程中执行多个 Subtask；Task Slot 表示 TaskManager 资源的固定子集，例如一个 TaskManager 有 3 个 Slot，则每个 Slot 约占 1/3 的 Managed Memory。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/concepts/flink-architecture/?utm_source=chatgpt.com))

Slot 配置示例：

```yaml
# 每个 TaskManager 提供的 Slot 数量
taskmanager.numberOfTaskSlots: 4

# TaskManager 总进程内存
taskmanager.memory.process.size: 4096m
```

Slot 与并行度关系：

```text
总可用 Slot 数 >= 作业最大并行度
```

需要注意，这只是简化理解。实际调度还会受到 Slot Sharing Group、Operator Chain、资源配置、调度策略和部署模式影响。

Slot 配置建议：

| 场景                        | 建议                                          |
| --------------------------- | --------------------------------------------- |
| CPU 密集型作业              | Slot 数接近 CPU Core 数，避免过度线程竞争     |
| IO 密集型作业               | Slot 数可适当高于 CPU Core，但要压测验证      |
| 大状态 RocksDB 作业         | 每个 Slot 需要足够 Managed Memory 和本地磁盘  |
| 多作业共享集群              | 使用 Slot Sharing 和资源队列隔离              |
| Kubernetes Application 模式 | 每个作业独立资源，Slot 数与作业并行度协同设计 |
| 本地调试                    | Slot 不宜过多，优先保证可观测和可调试         |

生产中不要只按“并行度 = Slot 数”机械配置。更合理的方式是按算子瓶颈、状态大小、CPU、内存、网络、下游吞吐和作业隔离要求综合规划。

### Operator Chain 优化

Operator Chain 是 Flink 将多个上下游算子 Subtask 链接成一个 Task 的优化方式。Flink 架构文档说明，算子链可以减少线程之间的数据交接和缓冲开销，从而提升吞吐并降低延迟；每个链化后的 Task 由一个线程执行。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/concepts/flink-architecture/?utm_source=chatgpt.com))

默认情况下，Flink 会自动对可链化算子进行链化。例如 `map -> filter -> map` 这类窄依赖算子通常会被链在一起。

禁用全局算子链：

```java
StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

// 不推荐生产默认关闭，仅用于排查和特殊场景
env.disableOperatorChaining();
```

从某个算子开始新链：

```java
DataStream<UserBehaviorEvent> parsedStream = sourceStream
        .process(new UserBehaviorJsonProcessFunction())
        .name("parse-user-behavior")
        .uid("parse-user-behavior")
        .startNewChain();

DataStream<UserBehaviorEvent> filteredStream = parsedStream
        .filter(event -> StrUtil.isNotBlank(event.getUserId()))
        .name("filter-valid-user")
        .uid("filter-valid-user");
```

禁止当前算子链化：

```java
DataStream<UserBehaviorWideEvent> wideStream = eventStream
        .map(new HeavyDimJoinMapFunction())
        .name("heavy-dim-join")
        .uid("heavy-dim-join")
        .disableChaining();
```

Operator Chain 优化建议：

1. 默认开启算子链，通常性能更好。
2. 计算重、耗时长、可能阻塞的算子可以考虑单独拆链，便于定位反压和指标。
3. Source、异步 IO、复杂 Join、大状态处理和 Sink 可以根据排查需要拆链。
4. 过度拆链会增加线程切换、网络缓冲和序列化开销。
5. 拆链后要对比吞吐、延迟、反压和 CPU 使用率，不要只凭感觉调整。
6. 关键算子仍需设置稳定的 `name()` 和 `uid()`。

### State Backend 优化

State Backend 决定 Flink 状态的运行时存储方式和 Checkpoint 快照方式。Flink 大状态调优文档说明，RocksDB State Backend 是许多大规模状态流应用的核心组件，能很好地扩展到超过内存容量的大状态；对于极大状态，还可以评估分离式状态存储 ForStStateBackend。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/ops/state/large_state_tuning/?utm_source=chatgpt.com))

常见状态后端选择：

| 状态后端                      | 适用场景                       | 注意事项                                    |
| ----------------------------- | ------------------------------ | ------------------------------------------- |
| `HashMapStateBackend`         | 小状态、低延迟、本地调试       | 状态主要在 JVM 堆上，状态过大容易 GC 或 OOM |
| `EmbeddedRocksDBStateBackend` | 大状态、海量 Key、长窗口、去重 | 读写有序列化和本地磁盘开销                  |
| `ForStStateBackend`           | 超大状态、状态超过本地磁盘能力 | 需要评估版本、稳定性和存储系统能力          |

RocksDB 状态后端配置示例：

```yaml
# 使用 RocksDB 状态后端，适合大状态作业
state.backend: rocksdb

# 使用文件系统保存 Checkpoint
execution.checkpointing.storage: filesystem

# Checkpoint 目录
execution.checkpointing.dir: hdfs:///flink/checkpoints/prod/user-behavior-job

# 开启增量 Checkpoint
execution.checkpointing.incremental: true
```

State Backend 优化建议：

1. 小状态和低延迟场景可优先使用 HashMapStateBackend。
2. 大状态、海量 Key、长时间去重、窗口和 Join 场景优先使用 RocksDB。
3. RocksDB 需要足够本地磁盘、Managed Memory 和 I/O 能力。
4. 状态对象要尽量小，避免保存完整原始消息。
5. `MapState` 比把整个 `Map` 放入 `ValueState` 更适合大 Map 场景。
6. 状态后端变更需要测试 Savepoint 恢复兼容性。

### Checkpoint 优化

Checkpoint 优化用于降低快照耗时、减少失败率、缩短恢复时间和降低对主链路吞吐的影响。Flink 大状态调优文档说明，当 Checkpoint 耗时经常超过配置间隔时，下一个 Checkpoint 会在前一个完成后立即触发，可能导致作业持续处于 Checkpoint 压力下；可以通过设置最小 Checkpoint 间隔避免这种情况。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/ops/state/large_state_tuning/?utm_source=chatgpt.com))

Checkpoint 常见优化参数：

```java
env.enableCheckpointing(60_000L, CheckpointingMode.EXACTLY_ONCE);

env.getCheckpointConfig().setCheckpointTimeout(600_000L);
env.getCheckpointConfig().setMinPauseBetweenCheckpoints(30_000L);
env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
env.getCheckpointConfig().setTolerableCheckpointFailureNumber(3);
env.getCheckpointConfig().setExternalizedCheckpointCleanup(
        CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION
);
```

配置文件示例：

```yaml
# Checkpoint 间隔
execution.checkpointing.interval: 60s

# Checkpoint 超时时间
execution.checkpointing.timeout: 10min

# 两次 Checkpoint 之间的最小间隔
execution.checkpointing.min-pause: 30s

# 最大并发 Checkpoint 数，大状态作业通常设置为 1
execution.checkpointing.max-concurrent-checkpoints: 1

# Checkpoint 失败容忍次数
execution.checkpointing.tolerable-failed-checkpoints: 3

# 开启非对齐 Checkpoint，可用于反压严重场景，但需要结合版本和场景验证
execution.checkpointing.unaligned: true
```

Checkpoint 优化建议：

1. Checkpoint 间隔不要小于正常 Checkpoint 平均耗时。
2. 大状态作业设置 `min-pause`，避免连续触发 Checkpoint。
3. 大状态 RocksDB 作业优先开启增量 Checkpoint。Flink 文档说明，增量 Checkpoint 只记录相对上一次完成 Checkpoint 的变化，可以显著降低大状态 Checkpoint 时间。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/ops/state/large_state_tuning/?utm_source=chatgpt.com))
4. Checkpoint 存储必须使用可靠且性能稳定的 HDFS、S3、OSS 或 Ceph。
5. Sink 提交慢也会拖慢 Checkpoint，需要同时排查 Sink。
6. 监控 `checkpoint duration`、`checkpoint size`、`alignment time`、`start delay` 和失败原因。

### RocksDB 优化

RocksDB 适合大状态作业，但需要关注内存、磁盘、序列化、Compaction 和 Checkpoint。Flink 大状态调优文档说明，RocksDB 性能高度依赖可用内存，默认情况下 RocksDB 会使用 Flink Managed Memory 作为 buffer 和 cache 预算。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/ops/state/large_state_tuning/?utm_source=chatgpt.com))

RocksDB 常用配置示例：

```yaml
# 使用 RocksDB 状态后端
state.backend: rocksdb

# RocksDB 使用 Flink Managed Memory 管理内存
state.backend.rocksdb.memory.managed: true

# 开启增量 Checkpoint
execution.checkpointing.incremental: true

# RocksDB 本地状态目录，生产中建议使用高速本地盘
state.backend.rocksdb.localdir: /data1/flink/rocksdb,/data2/flink/rocksdb
```

RocksDB 调优方向：

| 方向            | 说明                                |
| --------------- | ----------------------------------- |
| Managed Memory  | 给 RocksDB 足够缓存和写缓冲区       |
| 本地磁盘        | 使用 SSD/NVMe，避免慢盘导致读写抖动 |
| 增量 Checkpoint | 降低大状态快照耗时                  |
| 状态结构        | 避免单个 ValueState 保存大对象      |
| TTL             | 清理过期状态，降低 RocksDB 数据量   |
| Compaction      | 关注写放大和磁盘抖动                |
| 序列化          | 减少状态对象大小和复杂类型          |

RocksDB 优化建议：

1. 大状态作业优先使用 RocksDB + 增量 Checkpoint。
2. RocksDB 本地目录应放在高速、独立、容量充足的磁盘。
3. 避免多个重状态作业共享同一块慢磁盘。
4. 增大 Managed Memory 通常能改善 RocksDB 读写性能。
5. 状态 TTL 和清理策略必须设计好，否则 RocksDB 数据会持续膨胀。
6. 监控 RocksDB 本地磁盘、Block Cache、Checkpoint 大小和恢复耗时。

### Kafka 消费优化

Kafka 消费优化主要围绕分区数、Source 并行度、消费位点、反序列化、Watermark、下游处理能力和 Checkpoint 进行。Kafka Source 的有效并行度通常受 Topic 分区数限制；如果 Source 并行度大于分区数，多出来的 Source Subtask 可能没有分区可读。

Kafka 消费优化项：

| 优化项        | 说明                                         |
| ------------- | -------------------------------------------- |
| Topic 分区数  | 决定 Kafka Source 最大有效读取并行度         |
| Source 并行度 | 通常不超过分区数                             |
| 消费组        | 每个作业使用独立 group-id                    |
| 反序列化      | 高吞吐链路避免复杂 JSON 反复转换             |
| Watermark     | 优先在 Source 上设置 WatermarkStrategy       |
| 空闲分区      | 低频分区配置 `withIdleness`                  |
| 下游能力      | Kafka Source 快不等于全链路快，Sink 可能反压 |
| Checkpoint    | Offset 随 Checkpoint 提交和恢复              |

Kafka Source 配置示例：

```java
WatermarkStrategy<UserBehaviorEvent> watermarkStrategy = WatermarkStrategy
        .<UserBehaviorEvent>forBoundedOutOfOrderness(Duration.ofSeconds(10))
        .withTimestampAssigner((event, timestamp) -> event.getEventTime())
        .withIdleness(Duration.ofMinutes(1));

DataStream<UserBehaviorEvent> eventStream = env
        .fromSource(kafkaSource, watermarkStrategy, "kafka-user-behavior-source")
        .name("kafka-user-behavior-source")
        .uid("kafka-user-behavior-source")
        .setParallelism(8);
```

Kafka 消费优化建议：

1. Topic 分区数应与目标吞吐和 Flink 并行度匹配。
2. 单个分区数据热点会导致某个 Source Subtask 繁忙。
3. 消费积压不一定是 Kafka Source 慢，常见原因是下游反压。
4. 反序列化逻辑要轻量，复杂解析和校验可以拆到后续算子。
5. Kafka 消费延迟、每分区 Lag、Source Busy/Idle、反压和 Checkpoint 耗时要一起观察。
6. 大量小消息可考虑上游批量、压缩或优化序列化格式。

### 数据倾斜处理

数据倾斜是指某些 Key 的数据量远高于其他 Key，导致部分 Subtask 负载过高，出现反压、Checkpoint 变慢、状态过大和延迟上升。倾斜通常出现在 `keyBy`、窗口聚合、Join、维表查询和 Sink 写入阶段。

常见倾斜原因：

| 原因         | 示例                                  |
| ------------ | ------------------------------------- |
| 热点 Key     | `unknown`、空用户、默认地区、热门商品 |
| Key 基数过低 | 按事件类型分组只有几个 Key            |
| 上游分区不均 | Kafka 分区数据量差异大                |
| Join 热点    | 某个商品或用户关联大量记录            |
| Sink 热点    | 单主键或单分区写入过多                |
| 脏数据聚集   | 异常数据统一落到同一个 Key            |

二阶段聚合示例：

```java
DataStream<EventCount> localAggStream = eventStream
        .map(event -> {
            String randomPrefix = String.valueOf(ThreadLocalRandom.current().nextInt(16));
            String localKey = randomPrefix + "_" + event.getEventType();
            return new EventCount(localKey, 1L);
        })
        .returns(EventCount.class)
        .keyBy(EventCount::getEventType)
        .reduce((left, right) -> new EventCount(left.getEventType(), left.getCount() + right.getCount()))
        .name("local-random-prefix-agg")
        .uid("local-random-prefix-agg");

DataStream<EventCount> finalAggStream = localAggStream
        .map(value -> {
            String originalKey = StrUtil.subAfter(value.getEventType(), "_", false);
            return new EventCount(originalKey, value.getCount());
        })
        .returns(EventCount.class)
        .keyBy(EventCount::getEventType)
        .reduce((left, right) -> new EventCount(left.getEventType(), left.getCount() + right.getCount()))
        .name("final-agg")
        .uid("final-agg");
```

数据倾斜处理建议：

1. 先通过 Web UI 查看各 Subtask 的 Records In/Out、Busy、Backpressure 和状态大小。
2. 对热点 Key 进行单独处理，例如拆分热点 Key、随机前缀、二阶段聚合。
3. 空值、默认值、异常值不要直接作为 Key 聚集到同一个分区。
4. Join 热点需要考虑广播小表、预聚合或拆分热点。
5. Sink 热点需要调整主键、分区键或写入策略。
6. 数据倾斜优化后必须验证结果一致性，尤其是二阶段聚合。

### 反压分析

反压表示下游处理速度低于上游生产速度，导致压力沿数据流向上游传播。Flink 反压监控文档说明，如果某个任务显示 Back Pressure 为 High，意味着它生产数据的速度快于下游算子消费速度；在 `Source -> Sink` 这样的简单作业中，如果 Source 显示反压，通常说明 Sink 消费慢并向上游施压。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.17/docs/ops/monitoring/back_pressure/?utm_source=chatgpt.com))

反压排查流程：

```text
Web UI 发现反压
  -> 找到最下游高 Busy 或高 Backpressure 算子
  -> 查看 Sink、状态、窗口、Join、外部 IO
  -> 对比 Subtask 指标是否倾斜
  -> 查看 Checkpoint 是否变慢
  -> 优化瓶颈算子或下游系统
```

关键指标：

| 指标                          | 说明                   |
| ----------------------------- | ---------------------- |
| `backPressureTimeMsPerSecond` | 每秒处于反压状态的时间 |
| `busyTimeMsPerSecond`         | 每秒忙于处理数据的时间 |
| `idleTimeMsPerSecond`         | 每秒空闲等待时间       |
| Records In/Out                | 输入输出记录数         |
| Checkpoint Duration           | Checkpoint 持续时间    |
| Alignment Time                | Barrier 对齐时间       |
| Kafka Lag                     | Kafka 消费积压         |
| Sink Flush Time               | Sink 批量写入耗时      |

反压原因与处理：

| 原因            | 处理建议                                  |
| --------------- | ----------------------------------------- |
| Sink 慢         | 增加 Sink 并行度、批量写入、优化下游      |
| 外部 IO 慢      | 使用异步 IO、缓存、限流、超时             |
| 状态读写慢      | 优化状态结构、增加 RocksDB 内存、清理状态 |
| 数据倾斜        | 拆分热点 Key、二阶段聚合                  |
| Checkpoint 过重 | 增量 Checkpoint、延长间隔、优化状态       |
| 网络不足        | 调整 Network Memory、检查跨节点传输       |
| GC 频繁         | 降低对象创建、调整内存和 GC 参数          |

反压分析建议：

1. 不要只看 Source 反压，真正瓶颈通常在下游。
2. 先定位最后一个高 Busy 或高反压的算子。
3. 如果只有部分 Subtask 慢，优先怀疑数据倾斜。
4. 如果 Sink 算子慢，优先查看下游系统写入耗时和错误。
5. 如果 Checkpoint 同时变慢，重点排查状态和 Barrier 对齐。
6. 每次优化后都要对比反压指标，而不是只看主观延迟。

### GC 优化

GC 优化用于降低 JVM 垃圾回收对作业延迟和吞吐的影响。GC 问题通常来自对象创建过多、大对象进入状态、堆内状态过大、JSON 频繁转换、缓存无界增长、批量缓冲过大或日志过多。

GC 问题表现：

| 表现            | 可能原因                  |
| --------------- | ------------------------- |
| 延迟周期性抖动  | Full GC 或长时间 Young GC |
| TaskManager OOM | 堆内状态、缓存或对象积压  |
| Checkpoint 变慢 | GC 停顿影响 Barrier 处理  |
| 反压升高        | GC 导致算子处理暂停       |
| CPU 高但吞吐低  | 对象创建和 GC 开销过大    |

JVM 参数示例：

```yaml
# TaskManager JVM 参数
env.java.opts.taskmanager: >-
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=/data/flink/heapdump
  -XX:+ExitOnOutOfMemoryError

# JobManager JVM 参数
env.java.opts.jobmanager: >-
  -XX:+UseG1GC
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=/data/flink/heapdump
  -XX:+ExitOnOutOfMemoryError
```

GC 优化建议：

1. 大状态作业优先使用 RocksDB，减少 JVM 堆内状态压力。
2. 高频路径减少临时对象创建，例如避免反复 `JSONUtil.parseObj`。
3. 缓存必须设置最大容量和 TTL。
4. 批量 Sink 缓冲不要无限增长。
5. 日志不要逐条打印大对象或完整原始消息。
6. 生产开启 OOM HeapDump，并将路径挂载到持久化磁盘。
7. Java 17 项目常用 G1GC，极低延迟场景需专项评估 ZGC，但必须压测验证。

## 内存与资源配置

本章节用于说明 Flink 作业的 TaskManager、JobManager、Managed Memory、Network Memory、JVM 参数、CPU、容器资源和资源隔离策略。Flink 内存配置比较复杂，生产中建议优先配置总进程内存，再按瓶颈微调内部组件。Flink 官方内存配置文档说明，TaskManager 和 JobManager 的总进程内存都可以通过 `taskmanager.memory.process.size` 和 `jobmanager.memory.process.size` 配置；在容器化部署中，这通常对应容器内存大小。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/deployment/memory/mem_setup/?utm_source=chatgpt.com))

### TaskManager 内存模型

TaskManager 是执行用户代码的核心进程，内存包括 JVM Heap、Off-Heap、Managed Memory、Network Memory、JVM Metaspace 和 JVM Overhead 等部分。Flink 内存文档说明，TaskManager 总进程内存由 Flink 应用使用的 Total Flink Memory 和 JVM 自身运行开销组成；Total Flink Memory 又包含 JVM Heap、Managed Memory 和其他 Direct/Native Memory 等部分。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.18/docs/deployment/memory/mem_setup_tm/?utm_source=chatgpt.com))

常用配置方式：

```yaml
# 推荐容器化环境使用，总进程内存等于容器内存预算
taskmanager.memory.process.size: 4096m

# 每个 TaskManager 的 Slot 数量
taskmanager.numberOfTaskSlots: 4

# Managed Memory 占 Total Flink Memory 的比例
taskmanager.memory.managed.fraction: 0.4

# Network Memory 占 Total Flink Memory 的比例
taskmanager.memory.network.fraction: 0.1
```

TaskManager 内存组成说明：

| 组件               | 说明                                           |
| ------------------ | ---------------------------------------------- |
| Framework Heap     | Flink 框架自身使用的堆内存                     |
| Task Heap          | 用户代码、算子对象、堆内状态和普通对象使用     |
| Framework Off-Heap | Flink 框架使用的堆外内存                       |
| Task Off-Heap      | 用户代码声明的堆外内存                         |
| Managed Memory     | Flink 管理的内存，用于 RocksDB、排序、哈希表等 |
| Network Memory     | 网络 Shuffle、缓冲区、数据交换使用             |
| JVM Metaspace      | 类元数据                                       |
| JVM Overhead       | 线程栈、JIT、GC、Native 等 JVM 开销            |

TaskManager 内存配置建议：

1. 容器化部署优先配置 `taskmanager.memory.process.size`。
2. Standalone 部署可根据实际情况配置 `taskmanager.memory.flink.size`。
3. 大状态 RocksDB 作业需要增加 Managed Memory。
4. 高 Shuffle、宽依赖、Join、窗口和大并行度作业需要关注 Network Memory。
5. 堆内状态和大量对象处理需要关注 Task Heap。
6. 内存配置变更要结合 GC、反压、Checkpoint 和 RocksDB 指标验证。

### JobManager 内存配置

JobManager 负责任务调度、Checkpoint 协调、作业元数据、资源管理和故障恢复。JobManager 通常不处理业务数据流，但大规模作业、超多算子、超多并行度、频繁 Checkpoint、大量作业同时提交时，JobManager 也可能出现内存压力。

配置示例：

```yaml
# JobManager 总进程内存，容器化部署中通常对应容器内存
jobmanager.memory.process.size: 2048m

# 高可用和大规模作业场景可适当增加
jobmanager.memory.heap.size: 1536m

# JobManager JVM Metaspace
jobmanager.memory.jvm-metaspace.size: 256m
```

JobManager 内存优化建议：

1. 单作业 Application 模式通常 1g 到 2g 可满足普通作业，复杂作业需压测。
2. Session 集群承载多个作业时，JobManager 内存要按作业数量和复杂度增加。
3. 大量 Checkpoint 元数据、大量 JobGraph 节点和高并行度会增加 JobManager 压力。
4. JobManager OOM 可能导致作业失败或无法恢复。
5. Web UI 展示大量作业或历史记录时也会增加内存消耗。
6. JobManager 内存调优要结合日志、GC、Checkpoint Coordinator 指标和作业规模。

### Managed Memory

Managed Memory 是 Flink 管理的堆外内存，常用于 RocksDB State Backend、排序、哈希表、批处理算子和 Python UDF 等。Flink 配置文档说明，`taskmanager.memory.managed.size` 是 TaskExecutor 的 Managed Memory 大小；如果未显式配置，则会按 `taskmanager.memory.managed.fraction` 从 Total Flink Memory 中推导，默认比例在配置文档中为 0.4。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/deployment/config/?utm_source=chatgpt.com))

配置示例：

```yaml
# 方式一：按比例配置 Managed Memory
taskmanager.memory.managed.fraction: 0.4

# 方式二：显式配置 Managed Memory 大小
taskmanager.memory.managed.size: 1536m
```

RocksDB 作业通常依赖 Managed Memory。Flink 大状态调优文档说明，默认情况下 RocksDB State Backend 使用 Flink Managed Memory 预算来管理 RocksDB buffer 和 cache。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/ops/state/large_state_tuning/?utm_source=chatgpt.com))

Managed Memory 优化建议：

1. RocksDB 大状态作业应保证 Managed Memory 充足。
2. 如果 RocksDB 读写慢、Block Cache 命中低、磁盘 IO 高，可以考虑增加 Managed Memory。
3. 批处理排序、Join 和聚合也会消耗 Managed Memory。
4. Managed Memory 不是 JVM Heap，不要用它解决普通 Java 对象 OOM。
5. 同一个 Slot 内多个 Managed Memory 消费者会共享预算，需要综合评估。
6. 配置过大可能挤压 Task Heap、Network Memory 或 JVM Overhead。

### Network Memory

Network Memory 是 TaskManager 中用于网络 Shuffle 和数据交换的堆外内存。Flink 配置文档说明，Network Memory 由 `taskmanager.memory.network.fraction`、`taskmanager.memory.network.min` 和 `taskmanager.memory.network.max` 控制；它用于 ShuffleEnvironment，例如网络缓冲区。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/deployment/config/?utm_source=chatgpt.com))

配置示例：

```yaml
# Network Memory 占 Total Flink Memory 的比例
taskmanager.memory.network.fraction: 0.1

# Network Memory 最小值
taskmanager.memory.network.min: 128m

# Network Memory 最大值
taskmanager.memory.network.max: 1g
```

需要增加 Network Memory 的场景：

| 场景             | 说明                                |
| ---------------- | ----------------------------------- |
| 高并行度 Shuffle | `keyBy`、rebalance、Join 等宽依赖多 |
| 大流量作业       | Source 到下游吞吐高                 |
| 多输入算子       | Join、CoProcess、Union 后处理       |
| 批处理 Shuffle   | 大规模批 Join、聚合、排序           |
| 网络缓冲不足     | 出现 buffer 不足或反压异常          |

Network Memory 优化建议：

1. 只有存在网络缓冲瓶颈时才调整，不要盲目增大。
2. 高并行度作业要关注 Input Gate 和 Result Partition 数量。
3. 反压不一定是 Network Memory 不足，更多时候是下游慢。
4. 调整 Network Memory 后要观察反压、吞吐和 GC。
5. Batch 作业、大 Shuffle 作业可能需要比流式清洗作业更多网络内存。
6. 容器内存固定时，增加 Network Memory 会压缩其他内存组件。

### JVM 参数配置

JVM 参数配置用于控制 GC、HeapDump、OOM 行为、编码、模块访问和诊断日志。Flink 官方内存配置文档说明，Flink 会根据配置的内存组件自动添加 `-Xmx`、`-Xms`、`-XX:MaxDirectMemorySize`、`-XX:MaxMetaspaceSize` 等 JVM 参数，因此不建议手工覆盖这些内存参数，除非非常明确。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/deployment/memory/mem_setup/?utm_source=chatgpt.com))

推荐 JVM 参数：

```yaml
# TaskManager JVM 参数
env.java.opts.taskmanager: >-
  -Dfile.encoding=UTF-8
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=/data/flink/heapdump
  -XX:+ExitOnOutOfMemoryError

# JobManager JVM 参数
env.java.opts.jobmanager: >-
  -Dfile.encoding=UTF-8
  -XX:+UseG1GC
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=/data/flink/heapdump
  -XX:+ExitOnOutOfMemoryError
```

Java 17 模块开放参数示例：

```yaml
env.java.opts.all: >-
  --add-opens=java.base/java.lang=ALL-UNNAMED
  --add-opens=java.base/java.util=ALL-UNNAMED
```

JVM 参数配置建议：

1. 不要手工设置 `-Xmx`、`-Xms` 覆盖 Flink 内存模型，优先用 Flink 内存配置项。
2. 生产建议开启 OOM HeapDump，并确保目录有空间和权限。
3. 中文日志和配置读取统一设置 `-Dfile.encoding=UTF-8`。
4. Java 17 反射访问问题需要按具体异常补充 `--add-opens`。
5. GC 参数调整必须基于 GC 日志和延迟指标。
6. OOM 后建议使用 `-XX:+ExitOnOutOfMemoryError` 让容器或资源管理器拉起新进程，避免进程处于不确定状态。

### CPU 资源配置

CPU 资源决定算子计算、序列化、反序列化、RocksDB Compaction、网络处理、GC 和外部系统客户端回调的执行能力。CPU 不足时，常见表现是 Busy Time 高、吞吐上不去、延迟升高、GC 变慢和 Checkpoint 处理变慢。

CPU 配置思路：

| 场景           | 建议                             |
| -------------- | -------------------------------- |
| CPU 密集型解析 | 增加并行度和 CPU Core            |
| JSON 大量解析  | 优化序列化格式或增加 CPU         |
| RocksDB 大状态 | 预留 CPU 给序列化和 Compaction   |
| Sink 批量写入  | CPU 通常不是唯一瓶颈，还要看下游 |
| 异步 IO        | CPU 与线程池、回调处理相关       |
| 多 Slot TM     | 注意多个 Slot 共享同一 JVM CPU   |

Kubernetes CPU 示例：

```yaml
resources:
  requests:
    cpu: "2"
    memory: "4Gi"
  limits:
    cpu: "4"
    memory: "4Gi"
```

CPU 优化建议：

1. CPU 密集型算子可以通过提高并行度扩展。
2. Slot 数不要远大于 CPU Core，否则线程竞争可能严重。
3. JSON、正则、加密、压缩、复杂表达式和 UDF 都可能成为 CPU 瓶颈。
4. RocksDB 大状态作业需要给 Compaction 和序列化留出 CPU 空间。
5. Kubernetes 中 CPU limit 过低可能导致 throttling，影响延迟稳定性。
6. 观察 CPU 使用率时要结合 Busy Time、反压、GC 和下游延迟。

### 容器资源配置

容器资源配置用于 Kubernetes、YARN 或其他资源管理器中的 Flink 部署。Flink 内存配置文档说明，在容器化部署中，`taskmanager.memory.process.size` 和 `jobmanager.memory.process.size` 通常对应请求的容器内存大小。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/deployment/memory/mem_setup/?utm_source=chatgpt.com))

Kubernetes TaskManager 示例：

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: flink-taskmanager
spec:
  containers:
    - name: taskmanager
      image: flink:2.2-java17
      resources:
        requests:
          cpu: "2"
          memory: "4Gi"
        limits:
          cpu: "4"
          memory: "4Gi"
      env:
        - name: FLINK_PROPERTIES
          value: |
            taskmanager.memory.process.size: 4096m
            taskmanager.numberOfTaskSlots: 4
            taskmanager.memory.managed.fraction: 0.4
            execution.checkpointing.interval: 60s
```

YARN 提交示例：

```bash
flink run \
  -t yarn-application \
  -Djobmanager.memory.process.size=2048m \
  -Dtaskmanager.memory.process.size=4096m \
  -Dtaskmanager.numberOfTaskSlots=4 \
  -Dparallelism.default=8 \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  target/flink-job-user-behavior.jar \
  --env prod \
  --jobName user-behavior-job
```

容器资源配置建议：

1. 容器内存 limit 应与 Flink process memory 保持一致或留出明确余量。
2. 容器 OOM 通常说明 Total Process Memory、JVM Overhead、Direct Memory 或 Native Memory 配置不合理。
3. RocksDB 本地目录要挂载高速持久或临时本地盘，并监控磁盘空间。
4. HeapDump 目录不要写在容器只读层，应挂载外部路径。
5. CPU request 决定调度保障，limit 决定最高可用 CPU。
6. Application 模式下每个作业独立容器资源，资源隔离更清晰。

### 资源隔离策略

资源隔离策略用于避免多个作业相互影响。实时计算作业常见资源竞争包括 CPU、内存、Slot、网络、RocksDB 本地磁盘、Checkpoint 存储、Kafka 消费、Sink 写入和外部维表查询。

常见隔离方式如下：

| 隔离方式             | 说明                                   |
| -------------------- | -------------------------------------- |
| Application 模式     | 每个作业独立 JobManager 和 TaskManager |
| YARN 队列            | 按业务线或优先级分配资源               |
| Kubernetes Namespace | 按环境或业务隔离                       |
| Slot Sharing Group   | 作业内控制算子资源共享                 |
| 独立 Kafka Group     | 避免消费位点相互影响                   |
| 独立 Checkpoint 路径 | 避免状态文件混用                       |
| 独立 Sink 账号       | 控制下游写入权限和限流                 |
| 独立 RocksDB 目录    | 避免本地磁盘争用                       |

Slot Sharing Group 示例：

```java
DataStream<String> sourceStream = env
        .fromSource(kafkaSource, watermarkStrategy, "kafka-source")
        .name("kafka-source")
        .uid("kafka-source")
        .slotSharingGroup("source-group");

DataStream<UserBehaviorEvent> parsedStream = sourceStream
        .process(new UserBehaviorJsonProcessFunction())
        .name("parse-user-behavior")
        .uid("parse-user-behavior")
        .slotSharingGroup("compute-group");

parsedStream
        .sinkTo(kafkaSink)
        .name("kafka-sink")
        .uid("kafka-sink")
        .slotSharingGroup("sink-group");
```

资源隔离建议：

1. 核心生产作业优先使用 Application 模式独立部署。
2. 重状态作业和普通清洗作业应分开资源池。
3. 实时主链路和离线补数作业必须隔离队列、消费组和 Sink 资源。
4. Checkpoint 存储路径按环境和作业隔离。
5. 外部系统账号按作业或业务域隔离，便于限流和审计。
6. 资源隔离不是只隔离 Flink，也要隔离 Kafka、数据库、OLAP、缓存、对象存储和告警通道。


## 日志与监控

本章节用于说明 Flink 作业的日志配置、运行日志规范、异常日志规范、Web UI 使用、Metrics 指标、Prometheus/Grafana 集成、业务指标监控和告警规则配置。Flink 官方文档说明，所有 Flink 进程都会创建日志文件，日志可通过 JobManager 或 TaskManager 的 Web UI 页面访问；Flink 使用 SLF4J 日志接口，默认底层日志框架是 Log4j 2。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/deployment/advanced/logging/?utm_source=chatgpt.com))

### 日志框架配置

Flink 作业建议统一使用 SLF4J 作为日志接口，底层使用 Flink 默认的 Log4j 2。业务代码中不要直接依赖 Log4j 2 API，也不要混用 Logback、slf4j-simple 等多个日志实现，避免运行时出现多个 SLF4J Binding 或日志输出混乱。

推荐依赖如下：

```xml
<dependencies>
    <!-- SLF4J 日志接口，业务代码统一面向该接口记录日志 -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <scope>provided</scope>
    </dependency>

    <!-- Lombok 提供 @Slf4j 注解，减少样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

本地开发环境可以单独准备 `log4j2-local.properties`，用于 IDE 或本地命令行调试。

文件位置：`src/main/resources/log4j2-local.properties`

```properties
# Log4j2 配置热加载间隔，单位秒
monitorInterval=30

# 根日志级别
rootLogger.level=INFO
rootLogger.appenderRefs=console, file
rootLogger.appenderRef.console.ref=ConsoleAppender
rootLogger.appenderRef.file.ref=FileAppender

# 控制台日志
appender.console.name=ConsoleAppender
appender.console.type=CONSOLE
appender.console.layout.type=PatternLayout
appender.console.layout.pattern=%d{yyyy-MM-dd HH:mm:ss.SSS} %-5p [%t] %c{36} - %m%n

# 本地文件日志
appender.file.name=FileAppender
appender.file.type=RollingFile
appender.file.fileName=logs/flink-local.log
appender.file.filePattern=logs/flink-local-%d{yyyy-MM-dd}-%i.log.gz
appender.file.layout.type=PatternLayout
appender.file.layout.pattern=%d{yyyy-MM-dd HH:mm:ss.SSS} %-5p [%t] %c{36} - %m%n
appender.file.policies.type=Policies
appender.file.policies.size.type=SizeBasedTriggeringPolicy
appender.file.policies.size.size=100MB
appender.file.strategy.type=DefaultRolloverStrategy
appender.file.strategy.max=10

# Flink 框架日志
logger.flink.name=org.apache.flink
logger.flink.level=INFO

# 项目业务日志
logger.biz.name=io.github.atengk.flink
logger.biz.level=INFO
```

本地运行时指定日志配置：

```bash
java \
  -Dfile.encoding=UTF-8 \
  -Dlog4j.configurationFile=src/main/resources/log4j2-local.properties \
  -cp target/flink-job-user-behavior.jar \
  io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  --env local \
  --jobName user-behavior-job
```

日志框架配置建议：

1. 业务代码统一使用 SLF4J。
2. Flink 集群运行时优先使用集群侧 Log4j 2 配置。
3. 本地日志和生产日志配置分离。
4. 不要把日志实现包重复打进业务 Fat Jar。
5. 敏感信息不得进入日志，包括密码、Token、Webhook、AK/SK。
6. 高频算子中不要逐条打印 INFO 日志。

### 作业运行日志

作业运行日志用于记录作业启动、配置加载、Source/Sink 初始化、Checkpoint 配置、关键业务分支和关闭流程。运行日志应帮助开发人员在不进入 Debug 的情况下判断作业是否按预期启动和运行。

推荐记录内容如下：

| 阶段            | 日志内容                                     |
| --------------- | -------------------------------------------- |
| 作业启动        | 作业名、环境、并行度、运行模式、配置文件路径 |
| 配置加载        | 配置项摘要、敏感字段脱敏                     |
| Source 初始化   | Source 类型、Topic、表名、路径、消费组       |
| Sink 初始化     | Sink 类型、目标表、Topic、批量参数           |
| Checkpoint 配置 | 间隔、超时、模式、存储路径                   |
| 规则加载        | 规则数量、版本、生效时间                     |
| 异常分流        | 脏数据数量、原因分类                         |
| 作业结束        | 关闭资源、停止原因                           |

下面的工具类用于打印作业启动摘要，并对敏感配置做脱敏。

文件位置：`flink-common/src/main/java/io/github/atengk/flink/common/log/JobStartupLogPrinter.java`

```java
package io.github.atengk.flink.common.log;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 作业启动日志打印工具
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class JobStartupLogPrinter {

    /**
     * 打印作业启动摘要
     *
     * @param jobName 作业名称
     * @param env 运行环境
     * @param parallelism 并行度
     * @param configPath 配置路径
     * @param config 配置项
     */
    public static void print(String jobName, String env, int parallelism, String configPath, Map<String, Object> config) {
        log.info("Flink作业启动，作业名称：{}，环境：{}，并行度：{}，配置文件：{}",
                jobName, env, parallelism, configPath);

        if (MapUtil.isEmpty(config)) {
            log.warn("作业配置为空，请检查配置文件或配置中心");
            return;
        }

        config.forEach((key, value) -> log.info("作业配置摘要，key：{}，value：{}",
                key, maskIfSensitive(key, String.valueOf(value))));
    }

    /**
     * 敏感字段脱敏
     *
     * @param key 配置项
     * @param value 配置值
     * @return 脱敏结果
     */
    private static String maskIfSensitive(String key, String value) {
        if (StrUtil.isBlank(key)) {
            return value;
        }

        String lowerKey = key.toLowerCase();
        boolean sensitive = lowerKey.contains("password")
                || lowerKey.contains("token")
                || lowerKey.contains("secret")
                || lowerKey.contains("webhook")
                || lowerKey.contains("access-key")
                || lowerKey.contains("secret-key");

        if (!sensitive) {
            return value;
        }

        return StrUtil.isBlank(value) ? value : "****";
    }
}
```

作业运行日志建议：

1. 启动参数必须打印摘要，但敏感字段要脱敏。
2. Source 和 Sink 初始化成功应记录目标地址摘要。
3. Checkpoint、状态后端、并行度、运行模式应记录。
4. 高频数据处理路径只记录 DEBUG 或采样日志。
5. 日志内容要能定位作业、算子、业务 Key 和异常原因。
6. 不要在日志中打印完整大对象、完整 Kafka 消息或大量原始数据。

### 异常日志规范

异常日志用于定位数据异常、外部系统异常、状态异常、序列化异常和 Sink 写入异常。异常日志应清楚区分“数据问题”和“系统问题”。数据问题应进入脏数据链路；系统问题可抛出异常触发 Flink 重启和 Checkpoint 恢复。

推荐异常日志字段如下：

| 字段           | 说明                           |
| -------------- | ------------------------------ |
| `jobName`      | 作业名称                       |
| `operatorName` | 算子名称                       |
| `bizKey`       | 业务主键，例如 userId、orderId |
| `source`       | 数据来源                       |
| `reason`       | 异常分类                       |
| `message`      | 异常摘要                       |
| `traceId`      | 链路追踪 ID                    |
| `eventTime`    | 事件时间                       |
| `processTime`  | 处理时间                       |

异常处理示例：

```java
try {
    // 执行业务处理逻辑
    processBusinessData(value);
} catch (IllegalArgumentException e) {
    log.warn("数据校验失败，作业：{}，算子：{}，业务Key：{}，原因：{}",
            jobName, operatorName, bizKey, e.getMessage());
    outputDirty(value, "BUSINESS_INVALID", e.getMessage());
} catch (Exception e) {
    log.error("系统处理异常，作业：{}，算子：{}，业务Key：{}，原因：{}",
            jobName, operatorName, bizKey, e.getMessage(), e);
    throw e;
}
```

异常日志规范建议：

1. 可预期的数据异常使用 `warn`，并输出脏数据。
2. 系统异常使用 `error`，必要时抛出异常触发恢复。
3. 日志中必须包含作业名、算子名、业务 Key 和异常原因。
4. 不要只写“处理失败”，必须说明失败类型。
5. 不要在高频异常中打印完整堆栈和完整原始消息。
6. 同类异常高频出现时，应通过指标和告警处理，而不是依赖日志刷屏。

### Flink Web UI

Flink Web UI 是作业排查的第一入口。可以查看作业拓扑、Task/Subtask 状态、并行度、吞吐、反压、Checkpoint、异常、日志、TaskManager、JobManager 和指标。Flink 官方日志文档说明，JobManager 和 TaskManager 日志可以通过 Web UI 页面访问。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/deployment/advanced/logging/?utm_source=chatgpt.com))

Web UI 常用页面如下：

| 页面          | 用途                                         |
| ------------- | -------------------------------------------- |
| Overview      | 查看集群资源、TaskManager、Slot、运行中作业  |
| Jobs          | 查看作业状态、运行时间、失败信息             |
| Job Graph     | 查看算子拓扑、并行度、吞吐和反压颜色         |
| Checkpoints   | 查看 Checkpoint 成功率、耗时、大小、失败原因 |
| Exceptions    | 查看异常堆栈和失败 Task                      |
| Back Pressure | 查看反压状态                                 |
| Task Managers | 查看 TM 日志、线程、内存和指标               |
| Metrics       | 查看作业和算子指标                           |
| Logs          | 查看 JobManager 和 TaskManager 日志          |

反压页面尤其重要。Flink 官方反压文档说明，如果某个 Task 显示 `High`，表示它生产数据的速度快于下游消费速度；反压会沿数据流反方向向上游传播。文档还说明每个 Subtask 暴露 `backPressureTimeMsPerSecond`、`idleTimeMsPerSecond` 和 `busyTimeMsPerSecond` 三类指标。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.14/docs/ops/monitoring/back_pressure/?utm_source=chatgpt.com))

Web UI 使用建议：

1. 先看作业是否 Running，再看异常和 Checkpoint。
2. 延迟升高时先看反压，不要直接调资源。
3. Checkpoint 失败时查看失败原因、耗时、状态大小和对齐时间。
4. 单个 Subtask 明显慢时，优先怀疑数据倾斜或外部系统热点。
5. Source 反压通常说明下游慢，不一定是 Source 本身问题。
6. Web UI 只能作为在线排查入口，长期监控应接入 Prometheus 和 Grafana。

### Metrics 指标

Flink 提供内置 Metrics 系统，也支持用户自定义指标。官方 Metrics 文档说明，用户可以在继承 `RichFunction` 的函数中通过 `getRuntimeContext().getMetricGroup()` 注册指标；Flink 支持 Counter、Gauge、Histogram 和 Meter 四种指标类型。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/ops/metrics/?utm_source=chatgpt.com))

常见 Flink 指标如下：

| 类型       | 指标                                              | 说明                  |
| ---------- | ------------------------------------------------- | --------------------- |
| 吞吐       | `numRecordsIn`、`numRecordsOut`                   | 输入输出记录数        |
| 反压       | `backPressureTimeMsPerSecond`                     | 反压时间              |
| 繁忙       | `busyTimeMsPerSecond`                             | 忙碌时间              |
| 空闲       | `idleTimeMsPerSecond`                             | 空闲时间              |
| Checkpoint | `lastCheckpointDuration`、`lastCheckpointSize`    | Checkpoint 耗时和大小 |
| Watermark  | `currentInputWatermark`、`currentOutputWatermark` | 事件时间推进          |
| Kafka      | Lag、消费速率、分区状态                           | Kafka Source 相关     |
| JVM        | CPU、Heap、NonHeap、GC                            | 运行时资源            |
| RocksDB    | Block Cache、Compaction、State Size               | 大状态作业重点关注    |

下面的函数用于注册业务指标，统计输入数量、脏数据数量和处理速率。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/metric/UserBehaviorMetricMapFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.metric;

import io.github.atengk.flink.model.UserBehaviorEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.Meter;
import org.apache.flink.metrics.MeterView;
import org.apache.flink.streaming.api.functions.RichMapFunction;

/**
 * 用户行为业务指标函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserBehaviorMetricMapFunction extends RichMapFunction<UserBehaviorEvent, UserBehaviorEvent> {

    private transient Counter inputCounter;

    private transient Meter inputMeter;

    /**
     * 初始化业务指标
     *
     * @param parameters Flink 配置
     */
    @Override
    public void open(Configuration parameters) {
        this.inputCounter = getRuntimeContext()
                .getMetricGroup()
                .addGroup("biz")
                .counter("user_behavior_input_total");

        this.inputMeter = getRuntimeContext()
                .getMetricGroup()
                .addGroup("biz")
                .meter("user_behavior_input_tps", new MeterView(inputCounter, 60));

        log.info("用户行为业务指标初始化完成");
    }

    /**
     * 统计输入指标
     *
     * @param value 用户行为事件
     * @return 用户行为事件
     */
    @Override
    public UserBehaviorEvent map(UserBehaviorEvent value) {
        inputCounter.inc();
        inputMeter.markEvent();
        return value;
    }
}
```

业务指标使用示例：

```java
DataStream<UserBehaviorEvent> monitoredStream = eventStream
        .map(new UserBehaviorMetricMapFunction())
        .name("user-behavior-biz-metrics")
        .uid("user-behavior-biz-metrics");
```

Metrics 指标建议：

1. 系统指标用于判断 Flink 作业运行状态。
2. 业务指标用于判断业务结果是否正常。
3. 指标命名应稳定，避免频繁变更导致 Grafana 看板失效。
4. 指标标签不要包含高基数字段，例如 userId、orderId。
5. Counter 适合计数，Gauge 适合当前值，Meter 适合速率，Histogram 适合延迟分布。
6. 关键指标要接入告警，而不是只展示在看板上。

### Prometheus 集成

Flink 支持通过 Metrics Reporter 将指标暴露给外部系统。官方 Metric Reporters 文档说明，可以在 Flink 配置文件中配置一个或多个 Reporter；Prometheus Reporter 是 pull 模式，默认端口是 `9249`，在同一台机器部署多个 JobManager/TaskManager 时建议使用端口范围，例如 `9250-9260`。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/deployment/metric_reporters/?utm_source=chatgpt.com))

Prometheus Reporter 配置示例：

文件位置：`conf/flink-conf.yaml`

```yaml
# 启用 Prometheus Reporter
metrics.reporter.prom.factory.class: org.apache.flink.metrics.prometheus.PrometheusReporterFactory

# Prometheus 拉取端口，建议使用端口范围
metrics.reporter.prom.port: 9250-9260

# 指标作用域，控制指标标签和层级
metrics.scope.jm: <host>.jobmanager
metrics.scope.tm: <host>.taskmanager.<tm_id>
metrics.scope.task: <host>.taskmanager.<tm_id>.<job_name>.<task_name>.<subtask_index>
metrics.scope.operator: <host>.taskmanager.<tm_id>.<job_name>.<operator_name>.<subtask_index>
```

Prometheus 抓取配置示例：

文件位置：`prometheus.yml`

```yaml
scrape_configs:
  - job_name: 'flink-jobmanager'
    static_configs:
      - targets:
          - 'flink-jobmanager:9250'

  - job_name: 'flink-taskmanager'
    static_configs:
      - targets:
          - 'flink-taskmanager-1:9250'
          - 'flink-taskmanager-2:9250'
```

Kubernetes 环境通常使用 ServiceMonitor 或 PodMonitor 由 Prometheus Operator 自动发现。核心思想仍然是暴露 Prometheus Reporter 端口，然后让 Prometheus 定期拉取。

Prometheus 集成建议：

1. 每个 JobManager 和 TaskManager 都需要暴露 Metrics 端口。
2. 使用端口范围避免同机多进程端口冲突。
3. 指标标签要控制基数，避免 Prometheus 压力过大。
4. 自定义业务指标命名要稳定。
5. Prometheus 只负责采集和存储，告警规则应通过 Alertmanager 或监控平台管理。
6. 不同环境的指标应通过 `env`、`cluster`、`job` 等标签区分。

### Grafana 看板

Grafana 用于展示 Flink 作业和业务指标。看板应分为系统层、作业层、算子层、业务层和外部依赖层，避免所有指标堆在一个页面中。

推荐看板结构如下：

| 看板           | 主要指标                                                |
| -------------- | ------------------------------------------------------- |
| Flink 集群总览 | JobManager、TaskManager、Slot、运行作业数               |
| 作业运行总览   | 作业状态、运行时长、重启次数、异常数                    |
| 吞吐与延迟     | Records In/Out、TPS、处理延迟、Watermark Lag            |
| Checkpoint     | 成功率、耗时、大小、失败次数、对齐时间                  |
| 反压分析       | Busy、Idle、BackPressure                                |
| 状态与 RocksDB | State Size、RocksDB Memory、Compaction、Checkpoint Size |
| Kafka Source   | Lag、消费速率、分区延迟                                 |
| Sink 写入      | 写入速率、失败次数、批量耗时                            |
| 业务指标       | PV、UV、订单数、金额、脏数据量、告警数                  |

常用 PromQL 示例：

```promql
# 作业每秒速率，实际指标名需要按 Prometheus 暴露结果调整
rate(flink_taskmanager_job_task_operator_numRecordsIn[1m])

# 反压时间
flink_taskmanager_job_task_backPressuredTimeMsPerSecond

# 忙碌时间
flink_taskmanager_job_task_busyTimeMsPerSecond

# 当前输出 Watermark
flink_taskmanager_job_task_operator_currentOutputWatermark

# 最近一次 Checkpoint 耗时
flink_jobmanager_job_lastCheckpointDuration
```

Grafana 看板建议：

1. 系统指标和业务指标分开。
2. 每个作业至少有吞吐、反压、Checkpoint、异常、业务量五类面板。
3. 关键面板应支持按作业名、算子名、Subtask 过滤。
4. 单个 Subtask 指标对排查数据倾斜非常重要。
5. 看板中的指标名要以实际 Prometheus 暴露结果为准。
6. 看板变更应纳入版本管理，避免人工修改后不可追踪。

### 业务指标监控

业务指标监控用于判断作业输出结果是否符合业务预期。系统指标只能说明 Flink 是否运行正常，不能说明业务数据是否正确。业务指标包括输入量、输出量、脏数据量、迟到数据量、维表命中率、规则命中数、Sink 写入失败数、指标结果波动等。

推荐业务指标如下：

| 指标                 | 说明              |
| -------------------- | ----------------- |
| `input_total`        | 输入总量          |
| `output_total`       | 输出总量          |
| `dirty_total`        | 脏数据总量        |
| `late_total`         | 迟到数据总量      |
| `parse_error_total`  | 解析失败总量      |
| `dim_hit_total`      | 维表命中数量      |
| `dim_miss_total`     | 维表未命中数量    |
| `sink_success_total` | Sink 写入成功数量 |
| `sink_failure_total` | Sink 写入失败数量 |
| `rule_hit_total`     | 规则命中数量      |

下面的函数用于统计维表命中和未命中指标。

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/metric/DimLookupMetricFunction.java`

```java
package io.github.atengk.flink.job.userbehavior.metric;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.flink.model.UserBehaviorWideEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Counter;
import org.apache.flink.streaming.api.functions.RichMapFunction;

/**
 * 维表查询业务指标函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class DimLookupMetricFunction extends RichMapFunction<UserBehaviorWideEvent, UserBehaviorWideEvent> {

    private transient Counter dimHitCounter;

    private transient Counter dimMissCounter;

    /**
     * 初始化维表指标
     *
     * @param parameters Flink 配置
     */
    @Override
    public void open(Configuration parameters) {
        this.dimHitCounter = getRuntimeContext()
                .getMetricGroup()
                .addGroup("biz")
                .counter("dim_lookup_hit_total");

        this.dimMissCounter = getRuntimeContext()
                .getMetricGroup()
                .addGroup("biz")
                .counter("dim_lookup_miss_total");

        log.info("维表查询业务指标初始化完成");
    }

    /**
     * 统计维表命中情况
     *
     * @param value 用户行为宽表
     * @return 用户行为宽表
     */
    @Override
    public UserBehaviorWideEvent map(UserBehaviorWideEvent value) {
        if (StrUtil.isNotBlank(value.getUserName()) || StrUtil.isNotBlank(value.getCategoryName())) {
            dimHitCounter.inc();
        } else {
            dimMissCounter.inc();
        }
        return value;
    }
}
```

业务指标监控建议：

1. 输入量和输出量应能对账。
2. 脏数据比例、迟到数据比例必须监控。
3. 维表命中率突然下降通常说明维表服务或缓存异常。
4. Sink 失败次数必须告警。
5. 业务指标应带作业名、环境、业务域等低基数标签。
6. 关键业务指标要与离线结果定期对账。

### 告警规则配置

告警规则用于在系统异常或业务异常时及时通知。告警不应只关注作业失败，也要关注延迟、反压、Checkpoint 失败、脏数据激增、Kafka Lag 增长、Sink 写入失败和业务指标异常。

推荐告警规则如下：

| 告警项          | 条件示例                      | 等级     |
| --------------- | ----------------------------- | -------- |
| 作业失败        | 作业状态非 Running            | Critical |
| 频繁重启        | 10 分钟内重启超过 3 次        | Critical |
| Checkpoint 失败 | 连续失败超过 3 次             | Critical |
| Checkpoint 过慢 | 耗时超过间隔 80%              | Warning  |
| 反压严重        | Backpressure High 持续 5 分钟 | Warning  |
| Kafka Lag 增长  | Lag 持续上升 10 分钟          | Warning  |
| 脏数据激增      | 脏数据比例超过 1%             | Warning  |
| Sink 失败       | 写入失败数大于 0              | Critical |
| Watermark 停滞  | 10 分钟无推进                 | Warning  |
| 维表命中率下降  | 命中率低于 95%                | Warning  |

Prometheus 告警规则示例：

```yaml
groups:
  - name: flink-job-alerts
    rules:
      - alert: FlinkCheckpointFailure
        expr: increase(flink_jobmanager_job_numberOfFailedCheckpoints[5m]) > 3
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Flink Checkpoint 连续失败"
          description: "作业 {{ $labels.job_name }} 在最近 5 分钟内 Checkpoint 失败超过 3 次"

      - alert: FlinkHighBackPressure
        expr: flink_taskmanager_job_task_backPressuredTimeMsPerSecond > 500
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Flink 作业出现严重反压"
          description: "作业 {{ $labels.job_name }} 算子 {{ $labels.task_name }} 反压持续超过阈值"

      - alert: FlinkDirtyDataHigh
        expr: rate(flink_taskmanager_job_task_operator_biz_dirty_total[5m]) > 100
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Flink 脏数据量异常"
          description: "作业 {{ $labels.job_name }} 最近 5 分钟脏数据速率超过阈值"
```

告警规则建议：

1. 告警要区分 Critical、Warning、Info。
2. 告警必须包含作业名、环境、算子名、指标值和排查入口。
3. 高频告警要做抑制和聚合。
4. 业务告警和系统告警要分组管理。
5. 告警阈值应基于历史基线，而不是拍脑袋设置。
6. 告警恢复也应通知，便于闭环。

## 测试体系

本章节用于说明 Flink Java 项目的测试体系，包括单元测试、算子测试、集成测试、MiniCluster 测试、Kafka 测试环境、Testcontainers 集成、数据回放测试和性能压测。Flink 官方测试文档说明，Flink 提供 `flink-test-utils`，其中包含 `MiniClusterWithClientResource`，可用于在 JUnit 测试中启动本地嵌入式 MiniCluster 来测试完整作业。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/testing/?utm_source=chatgpt.com))

### 单元测试

单元测试用于验证不依赖 Flink Runtime 的普通 Java 逻辑，例如参数解析、配置加载、JSON 解析、模型转换、校验规则、工具类和业务函数。单元测试应尽量快速、稳定、可重复，不依赖外部 Kafka、MySQL、Redis 等系统。

Maven 测试依赖：

```xml
<dependencies>
    <!-- JUnit 5 测试框架 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>${junit.version}</version>
        <scope>test</scope>
    </dependency>

    <!-- AssertJ 断言库，提升断言可读性 -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <version>3.27.3</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

下面的测试用于验证用户行为 JSON 解析。

文件位置：`flink-job-user-behavior/src/test/java/io/github/atengk/flink/job/userbehavior/UserBehaviorJsonParserTest.java`

```java
package io.github.atengk.flink.job.userbehavior;

import cn.hutool.json.JSONUtil;
import io.github.atengk.flink.model.UserBehaviorEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户行为 JSON 解析测试
 *
 * @author Ateng
 * @since 2026-05-11
 */
class UserBehaviorJsonParserTest {

    /**
     * 测试正常 JSON 解析
     */
    @Test
    void shouldParseUserBehaviorJson() {
        String json = """
                {
                  "userId": "1001",
                  "itemId": "sku_1001",
                  "categoryId": "cat_10",
                  "eventType": "click",
                  "eventTime": 1778464800000
                }
                """;

        UserBehaviorEvent event = JSONUtil.toBean(json, UserBehaviorEvent.class);

        assertThat(event).isNotNull();
        assertThat(event.getUserId()).isEqualTo("1001");
        assertThat(event.getEventType()).isEqualTo("click");
        assertThat(event.getEventTime()).isEqualTo(1778464800000L);
    }
}
```

单元测试建议：

1. 工具类、配置类、解析类必须有单元测试。
2. 正常数据、空数据、非法数据、边界值都要覆盖。
3. 单元测试不连接外部系统。
4. 测试数据放到 `src/test/resources`。
5. 测试名称应表达业务语义。
6. 单元测试应在 CI 中每次提交自动执行。

### 算子测试

算子测试用于验证 Flink Function 的行为，例如 `MapFunction`、`FlatMapFunction`、`ProcessFunction`、`KeyedProcessFunction`、窗口函数和自定义 Sink 的局部逻辑。简单函数可以直接实例化测试；涉及状态和定时器的函数需要使用 Flink 测试工具或 MiniCluster。

下面的测试用于验证 `MapFunction`。

文件位置：`flink-job-user-behavior/src/test/java/io/github/atengk/flink/job/userbehavior/function/UserBehaviorToMetricMapFunctionTest.java`

```java
package io.github.atengk.flink.job.userbehavior.function;

import io.github.atengk.flink.model.RealtimeMetric;
import io.github.atengk.flink.model.UserBehaviorEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户行为指标映射函数测试
 *
 * @author Ateng
 * @since 2026-05-11
 */
class UserBehaviorToMetricMapFunctionTest {

    /**
     * 测试用户行为转换为指标
     *
     * @throws Exception 转换异常
     */
    @Test
    void shouldMapUserBehaviorToMetric() throws Exception {
        UserBehaviorToMetricMapFunction function = new UserBehaviorToMetricMapFunction();

        UserBehaviorEvent event = new UserBehaviorEvent(
                "1001",
                "sku_1001",
                "cat_10",
                "click",
                1778464800000L
        );

        RealtimeMetric metric = function.map(event);

        assertThat(metric).isNotNull();
        assertThat(metric.getMetricName()).isEqualTo("user_behavior_click_count");
        assertThat(metric.getMetricKey()).isEqualTo("click");
        assertThat(metric.getMetricValue()).isEqualTo("1");
    }
}
```

算子测试建议：

1. 纯函数算子直接单元测试。
2. 解析函数要覆盖脏数据、空字段和非法枚举。
3. 有状态函数要验证状态更新、状态清理和定时器。
4. 窗口函数要验证窗口开始时间、结束时间和聚合结果。
5. Sink 函数要 Mock 外部写入或使用 Testcontainers。
6. 不要只靠本地启动作业观察日志来替代算子测试。

### 集成测试

集成测试用于验证多个组件组合后的行为，例如 Source -> Parse -> Window -> Sink，或者 Kafka -> Flink -> JDBC。集成测试可以使用本地文件、嵌入式 MiniCluster、Testcontainers 或测试环境中间件。

集成测试关注点如下：

| 关注点      | 说明                            |
| ----------- | ------------------------------- |
| 配置加载    | 环境参数、配置文件、默认值      |
| Source 读取 | Kafka、文件、CDC 是否能读取     |
| 转换逻辑    | 清洗、过滤、聚合、Join          |
| Sink 写入   | Kafka、数据库、文件是否写入正确 |
| Checkpoint  | 状态快照和恢复是否正常          |
| 异常处理    | 脏数据、迟到数据、外部系统失败  |
| 幂等性      | 重复处理是否导致重复结果        |

集成测试建议：

1. 测试输入和预期输出要固定。
2. 外部系统优先使用容器化临时环境。
3. 测试数据量不需要大，但要覆盖关键路径。
4. 集成测试可以在 CI 的独立阶段运行。
5. 涉及 Kafka、数据库、Redis 的测试需要自动清理数据。
6. 集成测试失败时要输出足够日志，便于定位。

### MiniCluster 测试

MiniCluster 测试用于在本地 JVM 中启动嵌入式 Flink 集群，验证完整作业拓扑、并行度、状态、Checkpoint 和算子行为。Flink 官方测试文档说明，`MiniClusterWithClientResource` 可用于针对本地嵌入式 MiniCluster 测试完整作业，并需要引入 `flink-test-utils` 测试依赖。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/testing/?utm_source=chatgpt.com))

Maven 依赖：

```xml
<dependency>
    <groupId>org.apache.flink</groupId>
    <artifactId>flink-test-utils</artifactId>
    <version>${flink.version}</version>
    <scope>test</scope>
</dependency>
```

MiniCluster 测试示例：

文件位置：`flink-job-user-behavior/src/test/java/io/github/atengk/flink/job/userbehavior/UserBehaviorMiniClusterTest.java`

```java
package io.github.atengk.flink.job.userbehavior;

import io.github.atengk.flink.model.UserBehaviorEvent;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.runtime.testutils.MiniClusterResourceConfiguration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.junit5.MiniClusterExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户行为 MiniCluster 测试
 *
 * @author Ateng
 * @since 2026-05-11
 */
class UserBehaviorMiniClusterTest {

    @RegisterExtension
    static final MiniClusterExtension MINI_CLUSTER = new MiniClusterExtension(
            new MiniClusterResourceConfiguration.Builder()
                    .setNumberTaskManagers(1)
                    .setNumberSlotsPerTaskManager(2)
                    .build()
    );

    /**
     * 测试本地 MiniCluster 执行用户行为作业片段
     *
     * @throws Exception 执行异常
     */
    @Test
    void shouldRunUserBehaviorPipelineInMiniCluster() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(2);
        env.setRuntimeMode(RuntimeExecutionMode.STREAMING);

        List<UserBehaviorEvent> result = new ArrayList<>();

        env.fromElements(
                        new UserBehaviorEvent("1001", "sku_1", "cat_1", "click", 1778464800000L),
                        new UserBehaviorEvent("1002", "sku_2", "cat_2", "pay", 1778464801000L)
                )
                .filter(event -> event.getUserId() != null)
                .executeAndCollect()
                .forEachRemaining(result::add);

        assertThat(result).hasSize(2);
    }
}
```

MiniCluster 测试建议：

1. 适合验证完整 Flink 拓扑，不适合替代所有单元测试。
2. 测试并行度应大于 1，便于发现并行执行问题。
3. 有状态逻辑、窗口逻辑、Checkpoint 逻辑建议使用 MiniCluster。
4. MiniCluster 测试运行较慢，应与普通单元测试分层执行。
5. 测试中不要依赖生产配置或生产外部系统。
6. 如果使用新版本 Flink，测试 API 包名可能随版本变化，需要以当前版本依赖为准。

### Kafka 测试环境

Kafka 测试环境用于验证 Kafka Source、Kafka Sink、序列化、消费位点、Topic 分区、Checkpoint 恢复和 Exactly Once/At Least Once 语义。测试环境可以使用本地 Docker Compose、Testcontainers Kafka 或公司测试 Kafka 集群。

Docker Compose 示例：

文件位置：`deploy/test/docker-compose-kafka.yml`

```yaml
services:
  kafka:
    image: apache/kafka:3.8.0
    container_name: kafka-test
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093
      KAFKA_LOG_DIRS: /tmp/kafka-logs
```

创建测试 Topic：

```bash
docker compose -f deploy/test/docker-compose-kafka.yml up -d

docker exec -it kafka-test /opt/kafka/bin/kafka-topics.sh \
  --create \
  --topic user_behavior_test \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1
```

发送测试数据：

```bash
docker exec -it kafka-test /opt/kafka/bin/kafka-console-producer.sh \
  --topic user_behavior_test \
  --bootstrap-server localhost:9092
```

Kafka 测试建议：

1. 测试 Topic 和生产 Topic 必须隔离。
2. 每个测试用例使用独立消费组，避免位点互相影响。
3. 测试完成后清理 Topic 或使用唯一 Topic 名。
4. Kafka 分区数应覆盖并行消费场景。
5. Exactly Once Sink 测试需要开启 Checkpoint。
6. 消费位点恢复测试要模拟失败和重启。

### Testcontainers 集成

Testcontainers 用于在测试中自动启动 Kafka、MySQL、PostgreSQL、Redis、Elasticsearch 等依赖环境，避免依赖共享测试环境。它适合集成测试和端到端测试，尤其适合 CI 中运行。

Maven 依赖示例：

```xml
<dependencies>
    <!-- Testcontainers JUnit 5 集成 -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>1.20.4</version>
        <scope>test</scope>
    </dependency>

    <!-- Testcontainers Kafka 模块 -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>kafka</artifactId>
        <version>1.20.4</version>
        <scope>test</scope>
    </dependency>

    <!-- Testcontainers MySQL 模块 -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>mysql</artifactId>
        <version>1.20.4</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

MySQL Testcontainers 示例：

文件位置：`flink-job-user-behavior/src/test/java/io/github/atengk/flink/job/userbehavior/container/MySqlContainerTest.java`

```java
package io.github.atengk.flink.job.userbehavior.container;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MySQL Testcontainers 测试
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Testcontainers
class MySqlContainerTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("ateng_test")
            .withUsername("flink")
            .withPassword("flink123456");

    /**
     * 测试 MySQL 容器可用性
     *
     * @throws Exception SQL 异常
     */
    @Test
    void shouldStartMysqlContainer() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(),
                MYSQL.getUsername(),
                MYSQL.getPassword()
        );
             Statement statement = connection.createStatement()) {

            ResultSet resultSet = statement.executeQuery("SELECT 1");
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt(1)).isEqualTo(1);
        }
    }
}
```

Testcontainers 使用建议：

1. 外部依赖集成测试优先使用 Testcontainers。
2. 每个测试容器要有初始化脚本和清理逻辑。
3. CI 环境需要支持 Docker。
4. 容器启动较慢，应将测试分层，不要每个单元测试都启动容器。
5. Kafka、MySQL、Redis 等容器版本要接近生产版本。
6. 测试中使用动态端口，不要写死 `localhost:9092` 这类固定地址。

### 数据回放测试

数据回放测试用于验证历史数据、脏数据、迟到数据或生产样本在新版本作业中的处理结果。它适合 Schema 变更、规则变更、指标口径变更、Bug 修复和上线前回归。

数据回放流程：

```text
准备历史样本数据
  -> 指定时间范围
  -> 使用本地文件或测试 Kafka 回放
  -> 执行 Flink 测试作业
  -> 输出到临时 Topic / 临时表
  -> 对比预期结果
  -> 生成回放报告
```

文件回放命令示例：

```bash
flink run \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorReplayJob \
  target/flink-job-user-behavior.jar \
  --env test \
  --jobName user-behavior-replay-job \
  --input file:///data/replay/user_behavior_2026-05-11.json \
  --outputTopic user_behavior_replay_result \
  --parallelism 2
```

数据回放测试建议：

1. 样本数据要覆盖正常、异常、迟到、乱序、边界值。
2. 回放结果写入临时 Topic 或临时表。
3. 回放测试必须有预期结果或对账规则。
4. Schema 变更前后都要跑回放测试。
5. 风控规则和指标口径变更必须使用历史数据验证。
6. 回放数据如果来自生产，需要做脱敏处理。

### 性能压测

性能压测用于验证作业在目标吞吐、延迟、状态规模和外部系统压力下是否稳定。压测不能只看作业是否 Running，还要看反压、Checkpoint、GC、Kafka Lag、Sink 写入失败、RocksDB 状态和业务结果正确性。

压测指标如下：

| 指标              | 目标                     |
| ----------------- | ------------------------ |
| 输入 TPS          | 是否达到目标吞吐         |
| 端到端延迟        | 是否满足业务 SLA         |
| Kafka Lag         | 是否稳定或下降           |
| Checkpoint 成功率 | 是否稳定成功             |
| Checkpoint 耗时   | 是否低于间隔             |
| 反压              | 是否持续 High            |
| CPU / 内存        | 是否有余量               |
| GC                | 是否频繁 Full GC         |
| Sink 成功率       | 是否无持续失败           |
| 状态大小          | 是否符合预期增长         |
| 业务结果          | 是否准确、无重复、无漏写 |

压测执行流程：

```text
确定目标 TPS 和延迟 SLA
  -> 准备压测数据
  -> 启动独立测试环境
  -> 逐步提升输入速率
  -> 观察系统指标和业务指标
  -> 找到瓶颈算子
  -> 调优并复测
  -> 输出压测报告
```

压测数据生成示例：

```bash
# 使用 Kafka producer 发送压测数据，实际压测建议使用专门压测工具或自研数据生成器
seq 1 1000000 | awk '{print "{\"userId\":\"user_"$1"\",\"itemId\":\"sku_1\",\"categoryId\":\"cat_1\",\"eventType\":\"click\",\"eventTime\":1778464800000}"}' \
  | kafka-console-producer.sh \
      --bootstrap-server kafka-test-01:9092 \
      --topic user_behavior_perf_test
```

性能压测建议：

1. 压测环境资源要尽量接近生产。
2. 压测数据分布要接近真实数据，包括热点 Key 和异常数据。
3. 逐步升压，不要直接打满。
4. 每次调参只改一个关键变量，便于比较。
5. 压测必须覆盖 Checkpoint 和失败恢复。
6. 压测报告应记录版本、参数、资源、吞吐、延迟、瓶颈和优化建议。


## 本地开发与调试

本章节用于说明 Flink Java 作业在本地环境中的启动、数据模拟、断点调试、日志排查、Checkpoint 验证和 SQL 验证方式。本地调试的目标不是完全模拟生产集群，而是快速验证作业入口、参数解析、配置加载、Source/Sink 初始化、算子逻辑、SQL 语法和异常分流。Flink 支持在单机甚至单个 JVM 中运行程序，便于通过 IDE 启动作业并设置断点调试；本地运行时通常需要 `flink-clients` 等运行相关依赖。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.16/docs/dev/dataset/local_execution/?utm_source=chatgpt.com))

### 本地启动作业

本地启动作业通常有两种方式：一种是在 IDE 中直接运行 `main()` 方法，另一种是打包后使用 `java -cp` 或本地 Flink 命令运行。IDE 启动适合断点调试和快速验证；本地 Flink 命令更接近集群提交方式。

推荐本地启动参数如下：

| 参数                  | 示例                           | 说明                |
| --------------------- | ------------------------------ | ------------------- |
| `--env`               | `local`                        | 本地环境标识        |
| `--jobName`           | `user-behavior-local-job`      | 作业名称            |
| `--config`            | `config/local/application.yml` | 本地配置文件        |
| `--parallelism`       | `1` 或 `2`                     | 本地并行度          |
| `--runtimeMode`       | `STREAMING`                    | 运行模式            |
| `--checkpointEnabled` | `true`                         | 是否开启 Checkpoint |

IDE 运行参数示例：

```text
--env local
--jobName user-behavior-local-job
--config config/local/application.yml
--parallelism 1
--runtimeMode STREAMING
--checkpointEnabled true
```

本地启动入口示例：

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/UserBehaviorLocalJob.java`

下面的代码用于在本地快速启动用户行为作业，使用内存模拟数据源和 Print Sink 验证主流程。

```java
package io.github.atengk.flink.job.userbehavior;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.flink.model.UserBehaviorEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * 用户行为本地调试作业
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserBehaviorLocalJob {

    /**
     * 本地作业入口
     *
     * @param args 启动参数
     * @throws Exception 作业执行异常
     */
    public static void main(String[] args) throws Exception {
        ParameterTool parameterTool = ParameterTool.fromArgs(args);

        String env = parameterTool.get("env", "local");
        String jobName = parameterTool.get("jobName", "user-behavior-local-job");
        int parallelism = Convert.toInt(parameterTool.get("parallelism"), 1);

        if (!StrUtil.equals(env, "local")) {
            throw new IllegalArgumentException("本地调试入口只允许使用 local 环境");
        }

        StreamExecutionEnvironment executionEnv = StreamExecutionEnvironment.getExecutionEnvironment();
        executionEnv.setParallelism(parallelism);
        executionEnv.setRuntimeMode(RuntimeExecutionMode.STREAMING);

        log.info("本地Flink作业启动，作业名称：{}，环境：{}，并行度：{}", jobName, env, parallelism);

        executionEnv.fromElements(
                        new UserBehaviorEvent("1001", "sku_1001", "cat_10", "click", 1778464800000L),
                        new UserBehaviorEvent("1002", "sku_1002", "cat_20", "pay", 1778464805000L),
                        new UserBehaviorEvent("1003", "sku_1003", "cat_10", "cart", 1778464810000L)
                )
                .name("local-user-behavior-source")
                .uid("local-user-behavior-source")
                .filter(event -> StrUtil.isNotBlank(event.getUserId()))
                .name("filter-valid-user")
                .uid("filter-valid-user")
                .print()
                .name("local-print-sink")
                .uid("local-print-sink");

        executionEnv.execute(jobName);
    }
}
```

本地命令运行示例：

```bash
mvn clean package -DskipTests

java \
  -Dfile.encoding=UTF-8 \
  -Dlog4j.configurationFile=src/main/resources/log4j2-local.properties \
  -cp target/flink-job-user-behavior.jar \
  io.github.atengk.flink.job.userbehavior.UserBehaviorLocalJob \
  --env local \
  --jobName user-behavior-local-job \
  --parallelism 1
```

命令说明：`mvn clean package -DskipTests` 用于构建本地作业包；`-Dlog4j.configurationFile` 指定本地日志配置；`--env local` 用于防止本地调试误连测试或生产资源。

本地启动建议：

1. 本地调试入口应与生产入口隔离。
2. 本地环境默认使用 Mock Source、文件 Source、datagen 或本地 Kafka。
3. 本地 Sink 优先使用 Print、文件或本地 Kafka，不连接生产 Sink。
4. 本地并行度建议为 1 或 2，便于断点和日志排查。
5. 本地入口可以保留更多日志，但不要污染生产入口。
6. IDE 运行时如果缺少 provided 依赖，需要在运行配置中包含 provided scope 依赖；Flink Maven 文档也提醒核心 API 依赖通常应为 provided，IDE 本地运行需要确保运行时类路径可用。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.13/docs/dev/datastream/project-configuration/?utm_source=chatgpt.com))

### 模拟数据源

模拟数据源用于在没有 Kafka、CDC、HBase、Redis 等外部依赖时验证作业逻辑。常见模拟方式包括 `fromElements`、文件数据源、Socket Source、自定义 Source、SQL `datagen` 表和测试 Kafka Topic。Flink 的 DataGen SQL Connector 是内置连接器，不需要额外依赖，适合本地开发 SQL 查询而不访问外部系统。([apache.googlesource.com](https://apache.googlesource.com/flink/%2B/master/docs/content/docs/connectors/table/datagen.md?utm_source=chatgpt.com))

常见模拟数据源如下：

| 方式           | 适用场景                             |
| -------------- | ------------------------------------ |
| `fromElements` | 少量固定样本，适合单元级本地调试     |
| 文件 Source    | 大批量样本、历史回放、边界数据       |
| Socket Source  | 手工输入，适合演示和简单验证         |
| 自定义 Source  | 模拟持续流、随机数据、压测样本       |
| SQL datagen    | 本地 SQL 查询、窗口、聚合验证        |
| 本地 Kafka     | 验证 Kafka Source/Sink、序列化和位点 |

文件模拟数据示例：

文件位置：`data/local/user_behavior.jsonl`

```json
{"userId":"1001","itemId":"sku_1001","categoryId":"cat_10","eventType":"click","eventTime":1778464800000}
{"userId":"1002","itemId":"sku_1002","categoryId":"cat_20","eventType":"pay","eventTime":1778464805000}
{"userId":"","itemId":"sku_1003","categoryId":"cat_10","eventType":"cart","eventTime":1778464810000}
```

文件 Source 本地读取示例：

```java
DataStream<String> fileStream = executionEnv
        .readTextFile("data/local/user_behavior.jsonl")
        .name("local-file-source")
        .uid("local-file-source");

fileStream
        .process(new UserBehaviorJsonProcessFunction())
        .name("parse-local-json")
        .uid("parse-local-json")
        .print()
        .name("print-local-result")
        .uid("print-local-result");
```

SQL datagen 模拟 Source 示例：

```sql
CREATE TEMPORARY TABLE user_behavior_datagen (
    user_id STRING,
    item_id STRING,
    category_id STRING,
    event_type STRING,
    event_time AS PROCTIME()
) WITH (
    'connector' = 'datagen',
    'rows-per-second' = '10',
    'fields.user_id.length' = '8',
    'fields.item_id.length' = '8',
    'fields.category_id.length' = '6',
    'fields.event_type.length' = '6'
);
```

模拟数据源建议：

1. Mock 数据要覆盖正常、空值、非法 JSON、字段缺失、迟到数据和乱序数据。
2. 文件数据建议使用 JSON Lines，一行一条，便于回放。
3. Socket Source 只用于演示，不用于可靠性验证。
4. SQL datagen 适合验证 SQL 语法和窗口逻辑。
5. 本地 Kafka 适合验证序列化、消费组和 Checkpoint 位点。
6. 模拟数据要纳入 `src/test/resources` 或 `data/local`，不要混入生产配置目录。

### 断点调试

断点调试用于定位函数内部字段转换、状态更新、定时器注册、维表查询和异常分流逻辑。Flink 本地运行时可以像普通 Java 程序一样在 `map()`、`flatMap()`、`processElement()`、`reduce()` 等方法中设置断点；Flink 本地执行文档也说明，本地运行程序时可以通过 IDE 调试器设置断点。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.16/docs/dev/dataset/local_execution/?utm_source=chatgpt.com))

断点调试建议断点位置如下：

| 位置               | 目的                             |
| ------------------ | -------------------------------- |
| `main()`           | 检查启动参数和配置加载           |
| Source 创建处      | 检查 Topic、GroupId、路径、表名  |
| 解析函数           | 检查 JSON/Avro/Protobuf 解析结果 |
| 校验函数           | 检查字段校验和脏数据输出         |
| `keyBy` 前         | 检查 Key 是否为空或倾斜          |
| `processElement()` | 检查状态、定时器和侧输出流       |
| Sink 创建处        | 检查连接参数和写入配置           |
| 异常捕获处         | 检查异常分类是否合理             |

断点调试入口建议固定并行度为 1：

```java
StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

// 断点调试建议使用并行度 1，避免多个 Subtask 同时执行导致调试混乱
env.setParallelism(1);
```

断点调试注意事项：

1. 并行度大于 1 时，同一断点可能被多个 Subtask 同时命中。
2. 不要在生产集群上用断点调试。
3. 断点停留时间过长可能导致外部连接超时，例如 Kafka、JDBC、Redis。
4. 调试带 Checkpoint 的作业时，长时间暂停可能导致 Checkpoint 超时。
5. 异步 IO 调试要关注线程池和回调线程，不一定在主线程命中断点。
6. 定时器调试要区分事件时间和处理时间。

### 日志排查

日志排查用于定位本地启动失败、依赖缺失、配置错误、序列化异常、SQL 语法错误、Connector 初始化失败和 Sink 写入异常。Flink 使用 SLF4J 作为日志接口，默认底层为 Log4j 2；Flink 进程日志可以在本地日志目录或 Web UI 中查看。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.16/docs/dev/dataset/local_execution/?utm_source=chatgpt.com))

常见排查命令：

```bash
# 查看本地日志
tail -f logs/flink-local.log

# 按异常关键字过滤
grep -E "Exception|ERROR|Caused by|ClassNotFound|NoClassDefFound|NoSuchMethod" logs/flink-local.log

# 查看依赖树，排查依赖冲突
mvn dependency:tree

# 只查看 Flink、Kafka、Jackson、Log4j 相关依赖
mvn dependency:tree -Dincludes=org.apache.flink,org.apache.kafka,com.fasterxml.jackson.core,org.apache.logging.log4j
```

命令说明：`tail -f` 用于实时查看本地日志；`grep -E` 用于快速定位异常关键字；`mvn dependency:tree` 用于排查依赖冲突、重复依赖和错误版本。

常见日志问题与处理：

| 问题                     | 可能原因                | 处理方式                     |
| ------------------------ | ----------------------- | ---------------------------- |
| `ClassNotFoundException` | 依赖未打包或 scope 错误 | 检查 Maven 依赖和 Shade 配置 |
| `NoSuchMethodError`      | 依赖版本冲突            | 查看依赖树，统一版本         |
| `NoClassDefFoundError`   | 运行时缺少类            | 检查 Fat Jar 和集群 lib      |
| 多个 SLF4J Binding       | 日志实现冲突            | 排除多余日志实现             |
| SQL Connector not found  | Connector Jar 缺失      | 添加依赖或放入 Flink lib     |
| JSON 解析异常            | 原始数据格式错误        | 输出脏数据并保留原文         |
| Checkpoint 超时          | 状态大、Sink 慢、反压   | 查看 Checkpoint 页面和反压   |

日志排查建议：

1. 先看第一段 `Caused by`，不要只看最后一行异常。
2. 依赖问题优先看 `dependency:tree`。
3. SQL Connector 问题优先检查 Connector Jar 是否在作业包或 Flink lib 中。
4. 本地调试不要把日志级别全局改成 DEBUG，容易刷屏。
5. 高频数据日志要采样或只输出摘要。
6. 敏感信息必须脱敏，尤其是 JDBC URL、密码、Token、Webhook。

### Checkpoint 本地验证

Checkpoint 本地验证用于确认作业状态、Source 位点、窗口状态、Keyed State、Sink 提交逻辑是否能正确快照和恢复。生产中 Checkpoint 应存储在高可用文件系统；本地调试可以使用 `file:///tmp/...` 目录。Flink Checkpoint 文档说明，Checkpoint 用于保存状态和流位置以支持故障恢复；`FileSystemCheckpointStorage` 可以写入文件系统路径，例如 HDFS 路径或本地 `file:///...` 路径。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-2.0-preview1/docs/ops/state/checkpoints/?utm_source=chatgpt.com))

本地 Checkpoint 配置示例：

文件位置：`config/local/application.yml`

```yaml
checkpoint:
  # 本地调试也建议开启，验证状态和恢复逻辑
  enabled: true

  # 本地间隔可以设置短一些，便于快速观察
  interval-ms: 10000

  # 本地超时时间
  timeout-ms: 60000

  # 防止频繁 Checkpoint
  min-pause-ms: 5000

  # 本地文件系统路径
  storage: file:///tmp/flink-checkpoints/user-behavior-local-job
```

Java 配置示例：

```java
StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

env.enableCheckpointing(10_000L, CheckpointingMode.EXACTLY_ONCE);
env.getCheckpointConfig().setCheckpointTimeout(60_000L);
env.getCheckpointConfig().setMinPauseBetweenCheckpoints(5_000L);
env.getCheckpointConfig().setExternalizedCheckpointCleanup(
        CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION
);

env.getCheckpointConfig().setCheckpointStorage("file:///tmp/flink-checkpoints/user-behavior-local-job");
```

本地验证命令：

```bash
# 清理历史本地 Checkpoint
rm -rf /tmp/flink-checkpoints/user-behavior-local-job

# 启动作业后观察本地 Checkpoint 文件
find /tmp/flink-checkpoints/user-behavior-local-job -maxdepth 3 -type f | head

# 查看目录大小
du -sh /tmp/flink-checkpoints/user-behavior-local-job
```

命令说明：`rm -rf` 清理历史调试状态，避免旧状态影响本次验证；`find` 查看 Checkpoint 文件是否生成；`du -sh` 查看状态大小是否符合预期。

Checkpoint 本地验证建议：

1. 有状态函数、窗口函数、Kafka Source、事务 Sink 都需要验证 Checkpoint。
2. 本地文件路径必须使用 `file:///` 前缀。
3. 断点调试时可能导致 Checkpoint 超时，必要时临时调大超时。
4. 本地 Checkpoint 成功不代表生产稳定，还需要测试 HDFS/S3/OSS 等远程存储。
5. 从 Checkpoint 恢复时，算子 `uid()` 必须稳定。
6. 本地验证后要清理 `/tmp/flink-checkpoints`，避免磁盘占满。

### SQL 本地验证

SQL 本地验证用于验证 DDL、DML、UDF、窗口、聚合、Join 和 Connector 参数。推荐优先使用 `datagen` Source 和 `print` Sink，不依赖外部 Kafka 或数据库。Flink Print SQL Connector 是内置 Sink，适合流作业测试和生产调试；输出会写到运行任务日志中，而不是一定出现在提交命令的控制台。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/connectors/table/print/?utm_source=chatgpt.com))

SQL 本地验证入口示例：

文件位置：`flink-job-user-behavior/src/main/java/io/github/atengk/flink/job/userbehavior/UserBehaviorSqlLocalJob.java`

下面的代码用于本地验证 Table API/SQL，使用 datagen 生成数据并通过 print 输出聚合结果。

```java
package io.github.atengk.flink.job.userbehavior;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

/**
 * 用户行为 SQL 本地验证作业
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserBehaviorSqlLocalJob {

    /**
     * SQL 本地验证入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.setRuntimeMode(RuntimeExecutionMode.STREAMING);

        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        log.info("用户行为SQL本地验证作业开始启动");

        tableEnv.executeSql("""
                CREATE TEMPORARY TABLE user_behavior_source (
                    user_id STRING,
                    item_id STRING,
                    event_type STRING,
                    proc_time AS PROCTIME()
                ) WITH (
                    'connector' = 'datagen',
                    'rows-per-second' = '5',
                    'fields.user_id.length' = '8',
                    'fields.item_id.length' = '8',
                    'fields.event_type.length' = '6'
                )
                """);

        tableEnv.executeSql("""
                CREATE TEMPORARY TABLE user_behavior_sink (
                    event_type STRING,
                    event_count BIGINT
                ) WITH (
                    'connector' = 'print',
                    'print-identifier' = 'user_behavior_metric'
                )
                """);

        tableEnv.executeSql("""
                INSERT INTO user_behavior_sink
                SELECT event_type, COUNT(*) AS event_count
                FROM user_behavior_source
                GROUP BY event_type
                """);

        log.info("用户行为SQL本地验证作业已提交");
    }
}
```

SQL 验证建议：

1. 先用 datagen + print 验证 SQL 语法和逻辑。
2. 再替换为 Kafka、JDBC、Doris、StarRocks 等真实 Connector。
3. Print Sink 输出在 Task 日志中，排查时要看 TaskManager 日志。
4. 聚合 SQL 可能输出 Update/Changelog 结果，要确认 Sink 是否支持。
5. Event Time SQL 必须验证 Watermark 是否推进。
6. UDF/UDAF/UDTF 先用小样本验证，再接入生产流。

### 常见调试问题

本地调试常见问题主要集中在依赖、日志、配置、Connector、端口、Checkpoint、并行度和 SQL Planner。调试时应先判断问题属于“启动前依赖问题”“运行时数据问题”“外部系统问题”还是“Flink 状态/时间语义问题”。

常见问题如下：

| 问题                    | 原因                             | 处理                                                  |
| ----------------------- | -------------------------------- | ----------------------------------------------------- |
| IDE 启动找不到 Flink 类 | provided 依赖未加入运行类路径    | IDE 运行配置包含 provided 依赖或添加本地 runtime 依赖 |
| Connector not found     | Connector 未打包或未放入 lib     | 检查 Maven 依赖、Shade 或 `$FLINK_HOME/lib`           |
| Print Sink 看不到输出   | 输出在 Task 日志                 | 查看 TaskManager 日志                                 |
| Checkpoint 不生成       | 未开启 Checkpoint 或作业太快结束 | 开启 Checkpoint，使用持续 Source                      |
| Checkpoint 超时         | 断点暂停、状态大、Sink 阻塞      | 调大超时或关闭断点验证                                |
| 本地 Kafka 连不上       | advertised.listeners 配置错误    | 检查 Docker Kafka 监听地址                            |
| JSON 解析失败           | 样本格式错误或字段类型不匹配     | 输出脏数据并保存原文                                  |
| SQL 聚合无法写 Kafka    | 普通 Kafka 不支持更新流          | 使用 Upsert Kafka 或支持 Changelog 的 Sink            |
| 状态恢复失败            | 算子 uid、状态名或类型变化       | 保持 uid 和状态类型稳定                               |
| 多个日志实现冲突        | 依赖引入 Logback/slf4j-simple    | 排除多余日志实现                                      |

调试建议：

1. 优先缩小输入数据量，用 3 到 10 条样本复现问题。
2. 本地并行度设为 1，便于观察顺序和断点。
3. 依赖问题先看 Maven 依赖树。
4. SQL 问题先用 datagen + print 验证。
5. 状态问题先固定 `uid()`，再验证 Checkpoint 和恢复。
6. 外部系统问题要先用命令行工具验证连通性，再接入 Flink。

## 作业打包

本章节用于说明 Flink Java 作业的 Maven 打包方式、Shade 插件配置、依赖排除、Fat Jar 构建、配置文件打包、多环境包管理和常见打包问题。Flink 官方 Maven 文档说明，如果作业使用了 Flink 发行版未内置的外部依赖，可以将依赖加入集群 classpath，也可以通过 Maven Shade Plugin 打入 Uber/Fat 应用 Jar；同时，Flink 核心 API 依赖通常应使用 `provided`，避免把 Flink Runtime 重复打入作业包导致 Jar 过大或依赖冲突。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/dev/configuration/maven/?utm_source=chatgpt.com))

### Maven 打包配置

Maven 打包配置应统一管理 Java 版本、Flink 版本、Connector 版本、编码、编译插件和打包插件。Flink 应用依赖通常分为 Flink Core Dependencies 和 User Application Dependencies；Flink Runtime 和 API 已由集群提供，用户作业 Jar 主要包含业务代码、Connector、Format 和第三方业务依赖。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.13/docs/dev/datastream/project-configuration/?utm_source=chatgpt.com))

根 `pom.xml` 基础配置示例：

文件位置：`pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>io.github.atengk</groupId>
    <artifactId>flink-java-demo</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <modules>
        <module>flink-model</module>
        <module>flink-common</module>
        <module>flink-connector</module>
        <module>flink-job-user-behavior</module>
    </modules>

    <properties>
        <!-- Java 编译版本，按实际 Flink 版本兼容性选择 -->
        <maven.compiler.release>17</maven.compiler.release>

        <!-- 项目统一编码 -->
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <!-- Flink 版本必须与目标集群保持一致 -->
        <flink.version>2.2.0</flink.version>

        <!-- Hutool 工具类版本 -->
        <hutool.version>5.8.36</hutool.version>

        <!-- Lombok 版本 -->
        <lombok.version>1.18.36</lombok.version>

        <!-- Maven Shade 插件版本 -->
        <maven.shade.plugin.version>3.6.2</maven.shade.plugin.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Flink Streaming API，生产集群通常已提供，业务模块中按 provided 使用 -->
            <dependency>
                <groupId>org.apache.flink</groupId>
                <artifactId>flink-streaming-java</artifactId>
                <version>${flink.version}</version>
            </dependency>

            <!-- Flink Java API，生产集群通常已提供 -->
            <dependency>
                <groupId>org.apache.flink</groupId>
                <artifactId>flink-java</artifactId>
                <version>${flink.version}</version>
            </dependency>

            <!-- Flink Client，本地运行 main 方法和提交作业时常用 -->
            <dependency>
                <groupId>org.apache.flink</groupId>
                <artifactId>flink-clients</artifactId>
                <version>${flink.version}</version>
            </dependency>

            <!-- Hutool 工具集合 -->
            <dependency>
                <groupId>cn.hutool</groupId>
                <artifactId>hutool-all</artifactId>
                <version>${hutool.version}</version>
            </dependency>

            <!-- Lombok 注解工具 -->
            <dependency>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

作业模块 `pom.xml` 示例：

文件位置：`flink-job-user-behavior/pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.github.atengk</groupId>
        <artifactId>flink-java-demo</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>flink-job-user-behavior</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <!-- 项目模型模块 -->
        <dependency>
            <groupId>io.github.atengk</groupId>
            <artifactId>flink-model</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- 项目公共模块 -->
        <dependency>
            <groupId>io.github.atengk</groupId>
            <artifactId>flink-common</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- 项目连接器模块 -->
        <dependency>
            <groupId>io.github.atengk</groupId>
            <artifactId>flink-connector</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- Flink Streaming API，集群已提供，避免打入业务 Jar -->
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-streaming-java</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Flink Java API，集群已提供，避免打入业务 Jar -->
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-java</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Flink Client，本地运行 main 方法需要；是否打包按部署方式决定 -->
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-clients</artifactId>
        </dependency>

        <!-- Hutool 工具类 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>

        <!-- Lombok 编译期注解 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

Maven 配置建议：

1. 根工程统一版本，子模块不要重复写不同版本。
2. Flink API 和 Runtime 依赖通常使用 `provided`。
3. Connector、Format、业务 SDK 通常需要进入作业包或集群 lib。
4. 本地运行依赖和生产打包依赖要区分。
5. 多模块项目建议只在作业模块构建最终可提交 Jar。
6. 每次新增依赖后执行 `mvn dependency:tree` 检查冲突。

### Shade 插件配置

Maven Shade Plugin 用于构建包含依赖的 Uber/Fat Jar。Apache Maven Shade Plugin 官方文档说明，该插件可以将项目制品及其依赖打包为 Uber Jar，并支持包重命名、资源转换、过滤和主类配置；`shade:shade` 目标默认绑定到 `package` 阶段。([maven.apache.org](https://maven.apache.org/plugins/maven-shade-plugin/index.html?utm_source=chatgpt.com))

Shade 插件配置示例：

文件位置：`flink-job-user-behavior/pom.xml`

```xml
<build>
    <finalName>flink-job-user-behavior</finalName>

    <plugins>
        <!-- Maven 编译插件，统一 Java 版本 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
            <configuration>
                <release>${maven.compiler.release}</release>
                <encoding>${project.build.sourceEncoding}</encoding>
            </configuration>
        </plugin>

        <!-- Maven Shade 插件，用于构建 Flink 作业 Fat Jar -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>${maven.shade.plugin.version}</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                    <configuration>
                        <!-- 不生成 dependency-reduced-pom.xml，避免影响多模块工程 -->
                        <createDependencyReducedPom>false</createDependencyReducedPom>

                        <!-- 过滤签名文件，避免打包后出现安全校验异常 -->
                        <filters>
                            <filter>
                                <artifact>*:*</artifact>
                                <excludes>
                                    <exclude>META-INF/*.SF</exclude>
                                    <exclude>META-INF/*.DSA</exclude>
                                    <exclude>META-INF/*.RSA</exclude>
                                </excludes>
                            </filter>
                        </filters>

                        <transformers>
                            <!-- 合并 SPI 文件，避免 Connector、Format、Factory 无法加载 -->
                            <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>

                            <!-- 指定 Flink 作业入口类 -->
                            <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                <mainClass>io.github.atengk.flink.job.userbehavior.UserBehaviorJob</mainClass>
                            </transformer>
                        </transformers>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

Shade 插件配置建议：

1. 必须合并 `META-INF/services`，否则 Table Connector、Format、Factory 可能无法发现。
2. 签名文件通常需要排除，避免 Jar 合并后安全校验异常。
3. `createDependencyReducedPom` 在多模块项目中建议设为 `false`。
4. Flink Runtime 依赖不要重复打入，避免 Jar 过大和依赖冲突。
5. Connector 和第三方业务依赖是否打入，要结合部署模式决定。
6. Maven Shade Plugin 的 `artifactSet`、`filters`、`transformers` 可以精细控制最终 Jar 内容。([maven.apache.org](https://maven.apache.org/plugins/maven-shade-plugin/shade-mojo.html?utm_source=chatgpt.com))

### 依赖排除

依赖排除用于避免 Flink Runtime、日志实现、旧版本 Jackson、Guava、Netty、Kafka Client、Hadoop、Hive 等依赖冲突。Flink 官方项目配置文档明确区分 Flink Core Dependencies 和 User Application Dependencies；核心依赖通常由 Flink 集群提供，用户应用 Jar 应主要包含业务依赖、Connector 和 Format。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.13/docs/dev/datastream/project-configuration/?utm_source=chatgpt.com))

常见排除对象：

| 依赖          | 排除原因                           |
| ------------- | ---------------------------------- |
| Flink Runtime | 集群已提供，避免冲突               |
| Logback       | 避免与 Flink 默认 Log4j2 冲突      |
| slf4j-simple  | 避免多个日志绑定                   |
| 旧 Jackson    | 避免与 Flink 或 Connector 版本冲突 |
| 旧 Guava      | 避免 `NoSuchMethodError`           |
| Netty         | Kafka、Hadoop、Flink 都可能传递    |
| Hadoop/Hive   | 版本强相关，需由集群统一管理       |
| 签名文件      | Fat Jar 合并后可能校验失败         |

依赖排除示例：

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>third-party-sdk</artifactId>
    <version>1.0.0</version>
    <exclusions>
        <!-- 排除 Logback，避免与 Flink 默认 Log4j2 冲突 -->
        <exclusion>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
        </exclusion>

        <!-- 排除 slf4j-simple，避免多个 SLF4J Binding -->
        <exclusion>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
        </exclusion>

        <!-- 排除旧版 Jackson，统一由项目 dependencyManagement 管理 -->
        <exclusion>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

依赖排查命令：

```bash
# 查看完整依赖树
mvn dependency:tree

# 排查日志依赖
mvn dependency:tree -Dincludes=org.slf4j,ch.qos.logback,org.apache.logging.log4j

# 排查 Jackson 依赖
mvn dependency:tree -Dincludes=com.fasterxml.jackson.core

# 排查 Guava 依赖
mvn dependency:tree -Dincludes=com.google.guava

# 排查 Kafka 依赖
mvn dependency:tree -Dincludes=org.apache.kafka
```

命令说明：`dependency:tree` 用于确认依赖来源；`-Dincludes` 用于按 groupId 或 artifactId 过滤关键依赖，适合定位冲突版本。

依赖排除建议：

1. 不要盲目排除依赖，先确认依赖来源和冲突原因。
2. 日志实现只能保留一种。
3. Flink 核心依赖通常使用 `provided`。
4. Connector 依赖缺失会导致运行时找不到 Factory 或 Class。
5. Hadoop、Hive、HBase 相关依赖必须与集群版本匹配。
6. 排除依赖后要重新执行本地运行和集群提交验证。

### Fat Jar 构建

Fat Jar 是包含业务代码及必要依赖的可提交作业包。Flink 官方 Maven 文档说明，如果应用使用 Flink 发行版未内置的外部依赖，可以将依赖添加到 Flink 发行版 classpath，也可以打入 Uber/Fat 应用 Jar；使用 Fat Jar 后，可以通过 `flink run -c` 提交到本地或远程集群。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/dev/configuration/maven/?utm_source=chatgpt.com))

构建命令：

```bash
# 在项目根目录构建全部模块
mvn clean package -DskipTests

# 只构建指定作业模块，同时构建它依赖的模块
mvn clean package -pl flink-job-user-behavior -am -DskipTests

# 查看打包结果
ls -lh flink-job-user-behavior/target/

# 查看 Jar 内容
jar tf flink-job-user-behavior/target/flink-job-user-behavior.jar | head -n 50
```

命令说明：`-pl` 指定构建模块，`-am` 表示同时构建依赖模块，`jar tf` 用于查看 Jar 内容是否包含业务类、配置文件和必要依赖。

提交命令：

```bash
flink run \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  flink-job-user-behavior/target/flink-job-user-behavior.jar \
  --env prod \
  --jobName user-behavior-job \
  --config /data/flink/config/prod/application.yml \
  --parallelism 8
```

Fat Jar 验证建议：

1. 使用 `jar tf` 检查业务入口类是否存在。
2. 使用 `jar tf` 检查 `META-INF/services` 是否存在。
3. 使用 `mvn dependency:tree` 检查是否误打入 Flink Runtime。
4. 本地先运行 `java -cp` 或本地 Flink 命令验证。
5. 提交测试集群后验证 Source、Sink、Checkpoint、日志和指标。
6. Jar 文件名建议包含作业名和版本号，便于发布追溯。

### 配置文件打包

配置文件是否打入 Jar 取决于部署方式。本地调试可以将 `application-local.yml` 放入 `src/main/resources`；生产环境更推荐使用外部配置文件、配置中心、Kubernetes ConfigMap、YARN 本地化文件或平台参数注入。生产密码、Token、Webhook、AK/SK 不应打入 Jar。

推荐配置分层：

```text
src/main/resources/
├── application-default.yml
├── log4j2-local.properties
└── sql/
    ├── user_behavior_source.sql
    ├── user_behavior_sink.sql
    └── user_behavior_insert.sql

config/
├── local/
│   └── application.yml
├── test/
│   └── application.yml
└── prod/
    └── application.yml
```

资源文件打包配置示例：

```xml
<build>
    <resources>
        <!-- 打包默认资源，不包含生产敏感配置 -->
        <resource>
            <directory>src/main/resources</directory>
            <filtering>false</filtering>
            <includes>
                <include>application-default.yml</include>
                <include>log4j2-local.properties</include>
                <include>sql/**</include>
            </includes>
        </resource>
    </resources>
</build>
```

外部配置读取示例：

```java
ParameterTool parameterTool = ParameterTool.fromArgs(args);
String configPath = parameterTool.get("config", "config/local/application.yml");

if (StrUtil.isBlank(configPath)) {
    throw new IllegalArgumentException("配置文件路径不能为空");
}

log.info("加载外部配置文件，路径：{}", configPath);
```

配置文件打包建议：

1. 默认配置和 SQL 模板可以打入 Jar。
2. 生产环境配置建议外置。
3. 敏感配置禁止打入 Jar。
4. 配置文件路径通过 `--config` 参数传入。
5. SQL 文件可以放在 `src/main/resources/sql`，便于版本管理。
6. 外部配置优先级应高于 Jar 内默认配置。

### 多环境包管理

多环境包管理用于区分 local、test、prod 等环境。推荐做法是“一份代码、一份 Jar、多套外部配置”，避免为每个环境构建不同 Jar。这样可以保证测试环境验证过的 Jar 与生产发布 Jar 完全一致。

推荐发布结构：

```text
release/
├── flink-job-user-behavior-1.0.0.jar
├── config/
│   ├── local/
│   │   └── application.yml
│   ├── test/
│   │   └── application.yml
│   └── prod/
│       └── application.yml
└── scripts/
    ├── run-local.sh
    ├── run-test.sh
    └── run-prod.sh
```

生产提交脚本示例：

文件位置：`scripts/run-prod.sh`

```bash
#!/usr/bin/env bash

set -euo pipefail

JOB_NAME="user-behavior-job"
MAIN_CLASS="io.github.atengk.flink.job.userbehavior.UserBehaviorJob"
JAR_PATH="release/flink-job-user-behavior-1.0.0.jar"
CONFIG_PATH="/data/flink/config/prod/application.yml"
PARALLELISM="8"

flink run \
  -c "${MAIN_CLASS}" \
  -p "${PARALLELISM}" \
  "${JAR_PATH}" \
  --env prod \
  --jobName "${JOB_NAME}" \
  --config "${CONFIG_PATH}" \
  --parallelism "${PARALLELISM}"
```

执行脚本：

```bash
chmod +x scripts/run-prod.sh
./scripts/run-prod.sh
```

命令说明：`chmod +x` 为脚本增加执行权限；脚本中通过 `--env` 和 `--config` 控制运行环境，不为生产单独重新编译代码。

多环境包管理建议：

1. 同一个版本的 Jar 应在 test 和 prod 复用。
2. 环境差异通过外部配置和启动参数控制。
3. 生产配置与测试配置物理隔离。
4. Jar 文件名包含版本号，例如 `flink-job-user-behavior-1.0.0.jar`。
5. 发布记录应包含 Git Commit、Jar 校验值、配置版本和 Savepoint 路径。
6. 补数作业与实时作业使用不同配置和脚本，避免误操作。

### 打包常见问题

打包常见问题通常出现在依赖冲突、Connector 缺失、日志冲突、Flink Runtime 重复打包、SPI 文件未合并和主类配置错误。打包问题经常表现为本地 IDE 能运行，但提交到集群后失败。

常见问题如下：

| 问题                     | 可能原因                                    | 处理                                  |
| ------------------------ | ------------------------------------------- | ------------------------------------- |
| `ClassNotFoundException` | 依赖未进入 Jar 或集群 lib                   | 检查 Shade 和依赖 scope               |
| `NoSuchMethodError`      | 依赖版本冲突                                | 统一依赖版本，排除旧版本              |
| `Factory not found`      | `META-INF/services` 未合并或 Connector 缺失 | 添加 `ServicesResourceTransformer`    |
| Jar 过大                 | Flink Runtime 被打入                        | Flink API/Runtime 设置 `provided`     |
| 多个日志绑定             | Logback、slf4j-simple 被打入                | 排除多余日志实现                      |
| 找不到 Main Class        | Manifest 或 `flink run -c` 错误             | 检查主类包名和 Jar 内容               |
| SQL Connector 不可用     | SQL Connector Jar 缺失                      | 打入 Job Jar 或放入 `$FLINK_HOME/lib` |
| 签名文件异常             | 合并 Jar 后签名无效                         | 排除 `META-INF/*.SF` 等文件           |
| 本地正常集群失败         | 集群 Flink 版本不同                         | 保持编译版本与集群版本一致            |

排查命令：

```bash
# 查看 Jar 中是否包含入口类
jar tf target/flink-job-user-behavior.jar | grep UserBehaviorJob

# 查看 Jar 中是否包含 SPI 文件
jar tf target/flink-job-user-behavior.jar | grep META-INF/services

# 查看 Jar 大小
ls -lh target/flink-job-user-behavior.jar

# 查看依赖冲突
mvn dependency:tree

# 查看打包过程详细日志
mvn clean package -DskipTests -X
```

命令说明：`jar tf` 用于查看 Jar 内容；`grep UserBehaviorJob` 检查入口类是否存在；`grep META-INF/services` 检查 SPI 文件是否存在；`-X` 输出 Maven 调试日志，适合排查插件执行和依赖解析问题。

打包问题处理建议：

1. 先确认 Flink 集群版本，再确认项目依赖版本。
2. Connector 找不到时，优先检查是否打入 Jar 或放入 Flink lib。
3. Table/SQL Connector 依赖特别依赖 SPI 文件合并。
4. Flink Runtime 被重复打入会导致 Jar 大、类冲突和运行异常。
5. 不要同时使用多个日志实现。
6. 每次打包后至少做一次测试集群提交验证。


## 作业部署

本章节用于说明 Flink Java 作业在 Standalone、YARN、Kubernetes、Application Mode、Session Mode、Per-Job Mode、SQL Gateway 以及参数提交方面的部署方式。部署方式会直接影响资源隔离、作业生命周期、依赖分发、故障恢复、运维复杂度和发布流程。

Flink 部署架构中，客户端会将用户代码转换为 JobGraph 并提交给 JobManager，JobManager 再将任务调度到 TaskManager 执行；Flink 可以运行在 Standalone、YARN、Kubernetes 等资源环境中。当前 Flink 2.2 部署文档将部署模式归纳为 Application Mode 和 Session Mode，两者主要差异在于集群生命周期、资源隔离以及 `main()` 方法是在客户端还是集群侧执行。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-2.2/docs/deployment/overview/?utm_source=chatgpt.com))

### Standalone 部署

Standalone 是最基础的 Flink 部署方式，JobManager 和 TaskManager 以普通 JVM 进程运行在操作系统上。它适合本地开发、测试环境、小规模独立集群、无 YARN/Kubernetes 资源平台的场景。官方文档也将 Standalone 描述为最基础的部署方式，Flink 服务直接作为操作系统进程启动。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/deployment/resource-providers/standalone/overview/?utm_source=chatgpt.com))

Standalone 部署适用场景如下：

| 场景       | 说明                                     |
| ---------- | ---------------------------------------- |
| 本地测试   | 单机启动 JobManager 和 TaskManager       |
| 小规模集群 | 几台机器固定部署 Flink 进程              |
| 独立环境   | 没有 YARN 或 Kubernetes 的传统服务器环境 |
| 调试验证   | 快速验证 Jar、依赖、配置、Checkpoint     |
| 教学演示   | 启动成本低，结构清晰                     |

Standalone 目录结构示例：

```text
flink-2.2.0/
├── bin/
│   ├── start-cluster.sh
│   ├── stop-cluster.sh
│   └── flink
├── conf/
│   ├── flink-conf.yaml
│   ├── masters
│   └── workers
├── lib/
├── logs/
└── usrlib/
```

Standalone 基础配置示例：

文件位置：`$FLINK_HOME/conf/flink-conf.yaml`

```yaml
# JobManager RPC 地址，单机可配置为 localhost，分布式部署配置为 JobManager 主机名
jobmanager.rpc.address: flink-jobmanager

# JobManager RPC 端口
jobmanager.rpc.port: 6123

# Web UI 端口
rest.port: 8081

# 每个 TaskManager 提供的 Slot 数量
taskmanager.numberOfTaskSlots: 4

# JobManager 进程内存
jobmanager.memory.process.size: 2048m

# TaskManager 进程内存
taskmanager.memory.process.size: 4096m

# Checkpoint 存储路径，生产环境建议使用 HDFS、S3、OSS 等可靠存储
execution.checkpointing.storage: filesystem
execution.checkpointing.dir: hdfs:///flink/checkpoints/user-behavior-job

# Savepoint 默认路径
execution.checkpointing.savepoint-dir: hdfs:///flink/savepoints/user-behavior-job
```

Standalone 启停命令如下：

```bash
# 进入 Flink 安装目录
cd /opt/flink/flink-2.2.0

# 启动 Standalone 集群
./bin/start-cluster.sh

# 查看进程
jps

# 查看 Web UI
# 浏览器访问：http://flink-jobmanager:8081

# 停止 Standalone 集群
./bin/stop-cluster.sh
```

命令说明：`start-cluster.sh` 会根据 `conf/masters` 和 `conf/workers` 启动 JobManager 和 TaskManager；`jps` 用于检查 JVM 进程；`stop-cluster.sh` 用于停止集群。

提交作业示例：

```bash
./bin/flink run \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  -p 4 \
  /data/flink/jobs/flink-job-user-behavior-1.0.0.jar \
  --env prod \
  --jobName user-behavior-job \
  --config /data/flink/config/prod/application.yml \
  --parallelism 4
```

Standalone 部署建议：

1. 适合简单、稳定、独立的 Flink 集群。
2. 生产环境必须配置高可用，否则 JobManager 故障会影响作业。
3. Checkpoint 和 Savepoint 目录必须使用可靠共享存储。
4. 所有节点的 Flink 版本、JDK 版本、配置文件和依赖 Jar 要保持一致。
5. Connector Jar 如果放入 `$FLINK_HOME/lib`，所有 JobManager 和 TaskManager 节点都要同步。
6. 日志目录、RocksDB 本地目录、临时目录需要定期清理和监控。

### YARN 部署

YARN 部署适合 Hadoop 生态环境，Flink 的 JobManager 和 TaskManager 会作为 YARN Container 运行。YARN 适合已经具备 Hadoop 集群、HDFS、队列资源管理、Kerberos 和多租户资源隔离的企业环境。Flink on YARN 文档说明，YARN 是常见资源提供方，Flink 服务会提交到 YARN ResourceManager，并由 NodeManager 启动容器；官方也建议生产使用 Application Mode，因为它在应用之间提供更好的隔离。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/deployment/resource-providers/yarn/?utm_source=chatgpt.com))

YARN 部署前置条件如下：

| 条件              | 说明                                   |
| ----------------- | -------------------------------------- |
| Hadoop 客户端     | 提交节点需要可访问 YARN 和 HDFS        |
| `HADOOP_CONF_DIR` | 指向 Hadoop 配置目录                   |
| `YARN_CONF_DIR`   | 指向 YARN 配置目录                     |
| HDFS 权限         | 能读写 Checkpoint、Savepoint、Jar 路径 |
| 队列权限          | 具备目标 YARN 队列提交权限             |
| Kerberos          | 安全集群需要 keytab 和 principal       |

YARN Application Mode 提交示例：

```bash
./bin/flink run \
  -t yarn-application \
  -Djobmanager.memory.process.size=2048m \
  -Dtaskmanager.memory.process.size=4096m \
  -Dtaskmanager.numberOfTaskSlots=4 \
  -Dyarn.application.name=user-behavior-job \
  -Dyarn.application.queue=realtime \
  -Dexecution.checkpointing.dir=hdfs:///flink/checkpoints/prod/user-behavior-job \
  -Dexecution.checkpointing.savepoint-dir=hdfs:///flink/savepoints/prod/user-behavior-job \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  hdfs:///flink/jars/flink-job-user-behavior-1.0.0.jar \
  --env prod \
  --jobName user-behavior-job \
  --config hdfs:///flink/config/prod/user-behavior/application.yml \
  --parallelism 8
```

命令说明：`-t yarn-application` 表示使用 YARN Application Mode；`-D` 用于传递 Flink 和 YARN 配置；`-c` 指定作业入口类；Jar 和配置可以放在 HDFS 上，减少客户端上传压力。

YARN Session Mode 启动示例：

```bash
# 启动一个 YARN Session 集群
./bin/yarn-session.sh \
  -Djobmanager.memory.process.size=2048m \
  -Dtaskmanager.memory.process.size=4096m \
  -Dtaskmanager.numberOfTaskSlots=4 \
  -Dyarn.application.name=flink-session-realtime \
  -Dyarn.application.queue=realtime \
  -d

# 向已存在的 Session 集群提交作业
./bin/flink run \
  -t yarn-session \
  -Dyarn.application.id=application_1710000000000_0001 \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  /data/flink/jobs/flink-job-user-behavior-1.0.0.jar \
  --env test \
  --jobName user-behavior-job \
  --config /data/flink/config/test/application.yml
```

YARN 作业管理命令：

```bash
# 查看 YARN 应用
yarn application -list

# 查看 YARN 应用日志
yarn logs -applicationId application_1710000000000_0001

# 停止 YARN Application
yarn application -kill application_1710000000000_0001

# 查看 Flink 作业列表
./bin/flink list \
  -t yarn-application \
  -Dyarn.application.id=application_1710000000000_0001

# 取消 Flink 作业
./bin/flink cancel \
  -t yarn-application \
  -Dyarn.application.id=application_1710000000000_0001 \
  <jobId>
```

YARN 部署建议：

1. 长期运行生产作业优先使用 Application Mode。
2. 多个短任务或临时 SQL 可使用 Session Mode，但资源隔离较弱。
3. Jar、配置文件、Checkpoint、Savepoint 建议放在 HDFS。
4. YARN 队列要按业务域隔离，例如 `realtime`、`batch`、`test`。
5. Kerberos 环境需要单独配置认证文件和续期策略。
6. Application Mode 下取消作业通常会停止对应应用集群，运维操作前要确认影响范围。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/deployment/resource-providers/yarn/?utm_source=chatgpt.com))

### Kubernetes 部署

Kubernetes 部署适合云原生环境，Flink 组件以 Pod、Deployment、Service、ConfigMap、Secret 等 Kubernetes 资源运行。Flink 支持 Kubernetes 部署，官方文档也提到 Flink Kubernetes Operator 可以管理 Flink 集群并简化部署、配置和生命周期管理。对于新项目，如果平台已经具备 Kubernetes 和 Operator，推荐优先使用 Operator 或 Native Kubernetes 方式。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-2.0/docs/deployment/resource-providers/native_kubernetes/?utm_source=chatgpt.com))

Kubernetes 部署适用场景如下：

| 场景       | 说明                                        |
| ---------- | ------------------------------------------- |
| 云原生平台 | 统一使用 Kubernetes 调度资源                |
| 多环境隔离 | 使用 Namespace 区分 dev/test/prod           |
| 弹性资源   | 按作业申请 CPU、内存、Pod                   |
| 镜像交付   | 将 Jar、依赖、配置模板构建进镜像            |
| 平台化运维 | 结合 Flink Kubernetes Operator 管理生命周期 |

Kubernetes Application Mode 镜像示例：

文件位置：`deploy/kubernetes/Dockerfile`

```dockerfile
# 基于官方 Flink 镜像构建业务作业镜像
FROM flink:2.2.0-scala_2.12-java17

# 创建用户 Jar 目录
RUN mkdir -p /opt/flink/usrlib

# 拷贝业务作业 Jar
COPY flink-job-user-behavior-1.0.0.jar /opt/flink/usrlib/flink-job-user-behavior-1.0.0.jar

# 拷贝默认配置，生产敏感配置建议使用 ConfigMap 或 Secret 注入
COPY application-default.yml /opt/flink/usrlib/config/application-default.yml
```

构建镜像命令：

```bash
docker build \
  -t registry.example.com/flink/flink-job-user-behavior:1.0.0 \
  -f deploy/kubernetes/Dockerfile \
  .

docker push registry.example.com/flink/flink-job-user-behavior:1.0.0
```

命令说明：`docker build` 构建包含业务 Jar 的 Flink 镜像；`docker push` 推送到镜像仓库，供 Kubernetes 拉取。

Native Kubernetes Application Mode 提交示例：

```bash
./bin/flink run-application \
  -t kubernetes-application \
  -Dkubernetes.cluster-id=user-behavior-job \
  -Dkubernetes.namespace=flink-prod \
  -Dkubernetes.container.image=registry.example.com/flink/flink-job-user-behavior:1.0.0 \
  -Djobmanager.memory.process.size=2048m \
  -Dtaskmanager.memory.process.size=4096m \
  -Dtaskmanager.numberOfTaskSlots=4 \
  -Dparallelism.default=8 \
  -Dexecution.checkpointing.dir=s3://flink-prod/checkpoints/user-behavior-job \
  -Dexecution.checkpointing.savepoint-dir=s3://flink-prod/savepoints/user-behavior-job \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  local:///opt/flink/usrlib/flink-job-user-behavior-1.0.0.jar \
  --env prod \
  --jobName user-behavior-job \
  --config /opt/flink/usrlib/config/application-default.yml \
  --parallelism 8
```

Kubernetes Session Mode 示例：

```bash
# 启动 Kubernetes Session 集群
./bin/kubernetes-session.sh \
  -Dkubernetes.cluster-id=flink-session-realtime \
  -Dkubernetes.namespace=flink-test \
  -Dkubernetes.container.image=flink:2.2.0-scala_2.12-java17 \
  -Djobmanager.memory.process.size=2048m \
  -Dtaskmanager.memory.process.size=4096m \
  -Dtaskmanager.numberOfTaskSlots=4

# 向 Session 集群提交作业
./bin/flink run \
  -t kubernetes-session \
  -Dkubernetes.cluster-id=flink-session-realtime \
  -Dkubernetes.namespace=flink-test \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  /data/flink/jobs/flink-job-user-behavior-1.0.0.jar \
  --env test \
  --jobName user-behavior-job \
  --config /data/flink/config/test/application.yml
```

Kubernetes 运维命令：

```bash
# 查看 Pod
kubectl get pods -n flink-prod

# 查看 Service
kubectl get svc -n flink-prod

# 查看 JobManager 日志
kubectl logs -n flink-prod deploy/user-behavior-job -c flink-main-container

# 端口转发访问 Web UI
kubectl port-forward -n flink-prod svc/user-behavior-job-rest 8081:8081

# 删除应用相关资源
kubectl delete deployment,service,configmap,secret -n flink-prod -l app=user-behavior-job
```

Kubernetes 部署建议：

1. 生产作业优先使用 Application Mode 或 Flink Kubernetes Operator。
2. Jar、配置、Connector 依赖建议通过镜像、挂载卷、Artifact 管理或对象存储统一分发。
3. 密码、Token、AK/SK 必须使用 Secret 注入，不要写入镜像。
4. Checkpoint 和 Savepoint 使用远程可靠存储，例如 S3、OSS、HDFS。
5. RocksDB 本地目录要挂载合适的本地盘或 EmptyDir，并监控磁盘空间。
6. 使用 Namespace、ResourceQuota、ServiceAccount、RBAC 做环境和权限隔离。
7. Native Kubernetes 模式下 Application Mode 要保证作业 Jar 在集群侧可用，因为 `main()` 在集群侧执行。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-2.0/docs/deployment/resource-providers/native_kubernetes/?utm_source=chatgpt.com))

### Application Mode

Application Mode 为每个应用创建独立 Flink 集群，应用的 `main()` 方法在 JobManager 上执行。它适合长期运行的生产作业，资源隔离更清晰，客户端压力更小。Flink 文档说明，Application Mode 会为提交的应用创建专用集群，并在 JobManager 上执行应用 `main()` 方法；YARN 文档也建议生产使用 Application Mode，以获得更好的应用隔离。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-2.2/docs/deployment/overview/?utm_source=chatgpt.com))

Application Mode 特点如下：

| 项目              | 说明                              |
| ----------------- | --------------------------------- |
| 集群生命周期      | 与应用生命周期绑定                |
| `main()` 执行位置 | JobManager                        |
| 资源隔离          | 较好，一个应用独立集群            |
| 适用场景          | 长期运行生产作业、核心实时链路    |
| 客户端压力        | 较低                              |
| 依赖要求          | 作业 Jar 和依赖需要在集群侧可访问 |

YARN Application Mode 示例：

```bash
./bin/flink run \
  -t yarn-application \
  -Djobmanager.memory.process.size=2048m \
  -Dtaskmanager.memory.process.size=4096m \
  -Dtaskmanager.numberOfTaskSlots=4 \
  -Dyarn.application.name=user-behavior-job \
  -Dyarn.application.queue=realtime \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  hdfs:///flink/jars/flink-job-user-behavior-1.0.0.jar \
  --env prod \
  --jobName user-behavior-job \
  --config hdfs:///flink/config/prod/application.yml
```

Kubernetes Application Mode 示例：

```bash
./bin/flink run-application \
  -t kubernetes-application \
  -Dkubernetes.cluster-id=user-behavior-job \
  -Dkubernetes.namespace=flink-prod \
  -Dkubernetes.container.image=registry.example.com/flink/flink-job-user-behavior:1.0.0 \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  local:///opt/flink/usrlib/flink-job-user-behavior-1.0.0.jar \
  --env prod \
  --jobName user-behavior-job
```

Application Mode 使用建议：

1. 生产长期运行作业优先使用 Application Mode。
2. 每个核心作业独立资源，避免多个作业相互影响。
3. 作业取消、失败、完成时，应用集群生命周期会随之变化，需要明确运维流程。
4. Jar 和依赖应在集群侧可访问，YARN 可使用 HDFS，Kubernetes 可使用镜像或远程 Artifact。
5. 发布前必须记录 Jar 版本、配置版本、Savepoint 路径和提交参数。
6. 多个 `execute()` 的复杂应用要谨慎，生产中建议一个应用入口对应一个主要实时作业。

### Session Mode

Session Mode 先启动一个长期运行的 Flink 集群，再向该集群提交多个作业。它适合开发测试、短作业、临时查询、共享测试环境和交互式使用。Flink 文档说明，Session Mode 使用已经运行的集群，多个作业共享同一组 TaskManager 资源。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-2.2/docs/deployment/overview/?utm_source=chatgpt.com))

Session Mode 特点如下：

| 项目              | 说明                             |
| ----------------- | -------------------------------- |
| 集群生命周期      | 独立于作业生命周期               |
| `main()` 执行位置 | 客户端                           |
| 资源隔离          | 较弱，多个作业共享集群           |
| 适用场景          | 测试、临时作业、交互式查询       |
| 启动作业速度      | 较快                             |
| 运维风险          | 一个异常作业可能影响共享集群资源 |

Standalone Session Mode 示例：

```bash
# 启动 Standalone Session 集群
./bin/start-cluster.sh

# 提交作业
./bin/flink run \
  -m flink-jobmanager:8081 \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  /data/flink/jobs/flink-job-user-behavior-1.0.0.jar \
  --env test \
  --jobName user-behavior-job \
  --config /data/flink/config/test/application.yml
```

YARN Session Mode 示例：

```bash
# 启动 YARN Session
./bin/yarn-session.sh \
  -Dyarn.application.name=flink-session-test \
  -Dyarn.application.queue=test \
  -Djobmanager.memory.process.size=2048m \
  -Dtaskmanager.memory.process.size=4096m \
  -Dtaskmanager.numberOfTaskSlots=4 \
  -d

# 提交作业到 YARN Session
./bin/flink run \
  -t yarn-session \
  -Dyarn.application.id=application_1710000000000_0001 \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  /data/flink/jobs/flink-job-user-behavior-1.0.0.jar \
  --env test \
  --jobName user-behavior-job
```

Session Mode 使用建议：

1. 适合开发、测试、临时查询和短任务。
2. 不建议多个核心生产作业共享同一个 Session 集群。
3. 多作业共享资源时，要关注 Slot、内存、Checkpoint 存储和外部 Sink 压力。
4. 一个作业的反压、OOM 或异常可能影响共享集群上的其他作业。
5. Session 集群升级会影响所有运行在该集群上的作业。
6. Session Mode 下作业提交客户端需要承担 `main()` 执行和依赖上传压力。

### Per-Job Mode

Per-Job Mode 是历史部署模式，表示为每个作业创建一个独立集群，作业结束后集群释放。它曾常用于 YARN 上的长期作业隔离，但在较新的 Flink 版本中已经不再作为推荐模式。Flink 1.18 部署文档明确说明 Per-Job Mode 已在 Flink 1.15 中废弃，并建议在 YARN 上使用 Application Mode 来为每个作业启动专用集群。当前 Flink 2.2 部署概览只列出 Application Mode 和 Session Mode。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.18/docs/deployment/overview/?utm_source=chatgpt.com))

Per-Job Mode 历史特点如下：

| 项目              | 说明             |
| ----------------- | ---------------- |
| 集群生命周期      | 与单个 Job 绑定  |
| `main()` 执行位置 | 客户端           |
| 资源隔离          | 较好             |
| 当前建议          | 新项目不要使用   |
| 替代方案          | Application Mode |
| 主要历史环境      | YARN             |

旧版本 YARN Per-Job 提交形式通常类似：

```bash
# 历史模式示例，新项目不推荐使用
./bin/flink run \
  -m yarn-cluster \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  /data/flink/jobs/flink-job-user-behavior-1.0.0.jar \
  --env prod \
  --jobName user-behavior-job
```

Per-Job Mode 使用建议：

1. 新项目不要选择 Per-Job Mode。
2. 如果旧平台仍使用 Per-Job Mode，应规划迁移到 Application Mode。
3. 迁移前要验证提交命令、依赖分发、日志位置、Savepoint 恢复和资源配置。
4. 旧版本升级到 Flink 2.x 时，必须检查部署脚本是否仍包含 Per-Job 相关参数。
5. 运维文档中应将 Per-Job 标记为历史兼容模式。
6. 长期生产作业按当前版本优先使用 Application Mode。

### Flink SQL Gateway 部署

Flink SQL Gateway 是一个服务进程，允许多个远程客户端并发执行 SQL、查看元数据和在线分析数据。官方文档说明，SQL Gateway 包含可插拔 Endpoint 和 `SqlGatewayService`，常规 Flink 发行版已包含 SQL Gateway，可直接运行；它默认包含 REST Endpoint，也支持通过配置启动 HiveServer2 Endpoint。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-2.2/docs/dev/table/sql-gateway/overview/?utm_source=chatgpt.com))

SQL Gateway 适用场景如下：

| 场景             | 说明                              |
| ---------------- | --------------------------------- |
| SQL 作业提交     | 通过 REST 或客户端提交 SQL        |
| 交互式查询       | 多个客户端连接执行 SQL            |
| 元数据查看       | 查询 Catalog、Database、Table     |
| 平台集成         | 数据开发平台对接 Gateway          |
| HiveServer2 兼容 | 使用兼容 HiveServer2 协议的客户端 |

SQL Gateway 基础配置示例：

文件位置：`$FLINK_HOME/conf/flink-conf.yaml`

```yaml
# SQL Gateway REST Endpoint 地址
sql-gateway.endpoint.rest.address: 0.0.0.0

# SQL Gateway REST Endpoint 端口
sql-gateway.endpoint.rest.port: 8083

# 默认 Endpoint 类型，默认 REST；需要 HiveServer2 时可改为 hiveserver2
sql-gateway.endpoint.type: rest

# 默认执行目标，可按部署环境调整
execution.target: yarn-application

# 默认并行度
parallelism.default: 4
```

启动 SQL Gateway：

```bash
# 启动默认 REST Endpoint
./bin/sql-gateway.sh start

# 查看 SQL Gateway 进程
jps

# 停止 SQL Gateway
./bin/sql-gateway.sh stop
```

启动 HiveServer2 Endpoint 示例：

```bash
./bin/sql-gateway.sh start \
  -Dsql-gateway.endpoint.type=hiveserver2
```

官方文档说明，如果 CLI 命令和 Flink 配置文件同时指定 `sql-gateway.endpoint.type`，命令行配置优先级更高。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-2.2/docs/dev/table/sql-gateway/overview/?utm_source=chatgpt.com))

通过 SQL Client 连接示例：

```bash
./bin/sql-client.sh \
  gateway \
  --endpoint http://flink-sql-gateway:8083
```

SQL Gateway 部署建议：

1. SQL Gateway 本身不是作业执行集群，仍需要可用的 Flink 集群或资源平台。
2. SQL Gateway 的 Connector Jar、Format Jar、Catalog 依赖要与执行集群保持一致。
3. 生产环境需要配置认证、网络访问控制、审计日志和资源队列。
4. SQL Gateway 适合平台化 SQL 提交，不适合替代 Java 作业发布流程。
5. Hive、Iceberg、Hudi、Doris、StarRocks 等 Catalog 或 Connector 依赖要统一管理。
6. 多租户环境要按用户、队列、Catalog、数据库权限做隔离。

### 参数提交方式

参数提交方式用于控制作业运行环境、配置路径、并行度、Source/Sink 地址、Checkpoint 参数、业务开关和补数范围。Flink 提交参数通常分为三类：Flink 配置参数、作业程序参数、环境变量或外部配置。Flink 的 YARN 和 Kubernetes 文档都说明，可以通过 `-Dkey=value` 形式覆盖 Flink 配置文件中的参数。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/deployment/resource-providers/yarn/?utm_source=chatgpt.com))

参数类型如下：

| 类型           | 示例                       | 说明                   |
| -------------- | -------------------------- | ---------------------- |
| Flink 配置参数 | `-Dparallelism.default=8`  | 影响 Flink Runtime     |
| 提交参数       | `-p 8`、`-c MainClass`     | 影响提交行为           |
| 作业程序参数   | `--env prod`               | 传给用户 `main()` 方法 |
| 外部配置文件   | `--config application.yml` | 作业内部读取           |
| 环境变量       | `MYSQL_PASSWORD`           | Secret 或容器环境注入  |
| Savepoint 参数 | `-s hdfs:///...`           | 从状态恢复启动         |

推荐提交命令结构：

```bash
./bin/flink run \
  -t yarn-application \
  -Djobmanager.memory.process.size=2048m \
  -Dtaskmanager.memory.process.size=4096m \
  -Dtaskmanager.numberOfTaskSlots=4 \
  -Dparallelism.default=8 \
  -Dexecution.checkpointing.interval=60s \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  hdfs:///flink/jars/flink-job-user-behavior-1.0.0.jar \
  --env prod \
  --jobName user-behavior-job \
  --config hdfs:///flink/config/prod/application.yml \
  --sourceTopic user_behavior_log \
  --sinkTopic user_behavior_clean \
  --parallelism 8
```

从 Savepoint 恢复提交示例：

```bash
./bin/flink run \
  -t yarn-application \
  -s hdfs:///flink/savepoints/prod/user-behavior-job/savepoint-20260511-001 \
  -Djobmanager.memory.process.size=2048m \
  -Dtaskmanager.memory.process.size=4096m \
  -Dparallelism.default=8 \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  hdfs:///flink/jars/flink-job-user-behavior-1.0.1.jar \
  --env prod \
  --jobName user-behavior-job \
  --config hdfs:///flink/config/prod/application.yml
```

下面的代码用于统一解析作业启动参数，适合放在公共模块中复用。

文件位置：`flink-common/src/main/java/io/github/atengk/flink/common/param/FlinkJobParameter.java`

```java
package io.github.atengk.flink.common.param;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;

/**
 * Flink 作业启动参数
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Getter
@ToString
@Slf4j
public class FlinkJobParameter {

    private final String env;

    private final String jobName;

    private final String config;

    private final int parallelism;

    private final boolean checkpointEnabled;

    /**
     * 创建作业参数
     *
     * @param env 运行环境
     * @param jobName 作业名称
     * @param config 配置路径
     * @param parallelism 并行度
     * @param checkpointEnabled 是否开启 Checkpoint
     */
    private FlinkJobParameter(String env, String jobName, String config, int parallelism, boolean checkpointEnabled) {
        this.env = env;
        this.jobName = jobName;
        this.config = config;
        this.parallelism = parallelism;
        this.checkpointEnabled = checkpointEnabled;
    }

    /**
     * 从启动参数解析作业参数
     *
     * @param args 启动参数
     * @return 作业参数
     */
    public static FlinkJobParameter fromArgs(String[] args) {
        ParameterTool parameterTool = ParameterTool.fromArgs(args);

        String env = parameterTool.get("env", "local");
        String jobName = parameterTool.get("jobName", "flink-job");
        String config = parameterTool.get("config", "");
        int parallelism = Convert.toInt(parameterTool.get("parallelism"), 1);
        boolean checkpointEnabled = Convert.toBool(parameterTool.get("checkpointEnabled"), true);

        if (StrUtil.isBlank(jobName)) {
            throw new IllegalArgumentException("作业名称不能为空");
        }
        if (parallelism <= 0) {
            throw new IllegalArgumentException("并行度必须大于0");
        }
        if (!StrUtil.equalsAny(env, "local", "test", "prod")) {
            throw new IllegalArgumentException("运行环境只允许为 local、test、prod");
        }

        FlinkJobParameter parameter = new FlinkJobParameter(env, jobName, config, parallelism, checkpointEnabled);

        log.info("Flink作业参数解析完成，env：{}，jobName：{}，config：{}，parallelism：{}，checkpointEnabled：{}",
                parameter.getEnv(),
                parameter.getJobName(),
                parameter.getConfig(),
                parameter.getParallelism(),
                parameter.isCheckpointEnabled());

        return parameter;
    }
}
```

入口类中使用参数解析：

```java
FlinkJobParameter parameter = FlinkJobParameter.fromArgs(args);

StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
env.setParallelism(parameter.getParallelism());

if (parameter.isCheckpointEnabled()) {
    env.enableCheckpointing(60_000L);
}

env.execute(parameter.getJobName());
```

参数提交建议：

1. `-D` 参数用于 Flink Runtime 配置，`--xxx` 参数用于业务程序配置。
2. 敏感参数不要直接出现在命令行，优先通过 Secret、环境变量或配置中心注入。
3. 发布脚本中要固定 `jobName`、`mainClass`、Jar 路径、配置路径和并行度。
4. 从 Savepoint 恢复时必须记录 Savepoint 路径和代码版本。
5. 同一个作业不同环境只改配置，不重新构建不同 Jar。
6. 提交参数要进入发布记录，便于故障回滚和问题复现。


## Kubernetes 部署实践

本章节用于说明 Flink 作业在 Kubernetes 环境中的生产部署实践，包括 Flink Kubernetes Operator、作业配置文件、镜像构建、ConfigMap、Secret、资源限制、滚动升级和 Savepoint 升级。Kubernetes 部署的核心目标是将 Flink 作业纳入云原生交付体系，实现声明式部署、资源隔离、版本回滚、配置管理和自动化运维。

Apache Flink Kubernetes Operator 当前是 Flink 在 Kubernetes 上生产化部署的重要方式。官方文档说明，Operator 的核心用户侧 API 是 `FlinkDeployment` 和 `FlinkSessionJob` 两类 Custom Resource，其中 `FlinkDeployment` 可用于 Application 或 Session 集群部署，`FlinkSessionJob` 用于向 Session 集群提交作业。Operator 会持续监听这些 CR 的新增和变更，并执行相应的部署、升级、恢复等操作。([Apache Nightlies](https://nightlies.apache.org/flink/flink-kubernetes-operator-docs-release-1.14/docs/custom-resource/overview/?utm_source=chatgpt.com))

### Flink Kubernetes Operator

Flink Kubernetes Operator 用于在 Kubernetes 中声明式管理 Flink 作业生命周期。相比手工使用 `flink run-application -t kubernetes-application`，Operator 更适合生产环境，因为它可以通过 CRD 描述作业、资源、镜像、配置、Checkpoint、Savepoint 和升级策略，并由控制器持续调谐实际状态。

Operator 管理的主要对象如下：

| 对象                 | 说明                                      |
| -------------------- | ----------------------------------------- |
| `FlinkDeployment`    | 管理 Flink Application 或 Session 集群    |
| `FlinkSessionJob`    | 向 Flink Session 集群提交作业             |
| `FlinkStateSnapshot` | 管理 Savepoint 或 Checkpoint 快照         |
| `ConfigMap`          | 管理非敏感配置，例如 Flink 配置、业务配置 |
| `Secret`             | 管理密码、Token、AK/SK、数据库凭据        |
| `Service`            | 暴露 REST、Web UI、Metrics 等访问入口     |

Operator 支持作业生命周期管理，包括运行、挂起、删除、状态化升级、无状态升级、触发 Savepoint、错误处理和失败升级回滚等。官方 Job Management 文档说明，状态化升级由 `job.upgradeMode` 控制，支持 `stateless`、`savepoint` 和 `last-state` 三种模式。([Apache Nightlies](https://nightlies.apache.org/flink/flink-kubernetes-operator-docs-release-1.10/docs/custom-resource/job-management/?utm_source=chatgpt.com))

安装 Operator 的 Helm 命令示例：

```bash
# 添加 Flink Kubernetes Operator Helm 仓库
helm repo add flink-operator-repo https://downloads.apache.org/flink/flink-kubernetes-operator-1.14.0/

# 更新 Helm 仓库索引
helm repo update

# 安装 Operator 到 flink-system 命名空间
helm install flink-kubernetes-operator \
  flink-operator-repo/flink-kubernetes-operator \
  --namespace flink-system \
  --create-namespace
```

命令说明：`helm repo add` 添加 Operator Chart 仓库，`helm install` 安装 Operator 控制器。Operator 官方 Helm 文档说明，可以通过 Helm Chart 安装，也可以通过 `--namespace` 和 `--create-namespace` 安装到指定命名空间。([Apache Nightlies](https://nightlies.apache.org/flink/flink-kubernetes-operator-docs-release-1.12/docs/operations/helm/?utm_source=chatgpt.com))

查看 Operator 状态：

```bash
# 查看 Operator Pod
kubectl get pods -n flink-system

# 查看 Operator 日志
kubectl logs -n flink-system deploy/flink-kubernetes-operator

# 查看 CRD
kubectl get crd | grep flink.apache.org
```

Flink Kubernetes Operator 使用建议：

1. 生产环境优先使用稳定版本 Operator，不建议直接使用 snapshot 文档或未发布版本。
2. Operator、Flink Runtime、业务 Jar、Connector 版本需要统一规划。
3. 一个生产作业建议对应一个 `FlinkDeployment`，便于资源隔离和独立升级。
4. 核心作业使用 Application 模式，临时或交互式 SQL 可以使用 Session 模式。
5. Checkpoint 和 Savepoint 目录必须使用远程可靠存储，例如 HDFS、S3、OSS。
6. Operator 升级前要先确认 CRD 兼容性、Java client 兼容性和已有作业升级策略。

### Job 配置文件

在 Operator 模式下，Flink 作业通常通过 YAML 文件声明。`FlinkDeployment` YAML 中应包含镜像、Flink 版本、Flink 配置、JobManager 资源、TaskManager 资源、作业 Jar、入口类、启动参数、并行度和升级模式。

Application 作业配置示例：

文件位置：`deploy/kubernetes/user-behavior-flinkdeployment.yaml`

```yaml
apiVersion: flink.apache.org/v1beta1
kind: FlinkDeployment
metadata:
  name: user-behavior-job
  namespace: flink-prod
  labels:
    app: user-behavior-job
    env: prod
spec:
  image: registry.example.com/flink/flink-job-user-behavior:1.0.0
  imagePullPolicy: IfNotPresent
  flinkVersion: v2_2
  serviceAccount: flink

  flinkConfiguration:
    taskmanager.numberOfTaskSlots: "4"
    parallelism.default: "8"

    execution.checkpointing.interval: "60s"
    execution.checkpointing.mode: "EXACTLY_ONCE"
    execution.checkpointing.timeout: "10min"
    execution.checkpointing.min-pause: "30s"
    execution.checkpointing.dir: "s3://flink-prod/checkpoints/user-behavior-job"
    execution.checkpointing.savepoint-dir: "s3://flink-prod/savepoints/user-behavior-job"

    state.backend: "rocksdb"
    state.backend.rocksdb.memory.managed: "true"
    execution.checkpointing.incremental: "true"

    metrics.reporter.prom.factory.class: "org.apache.flink.metrics.prometheus.PrometheusReporterFactory"
    metrics.reporter.prom.port: "9250-9260"

  jobManager:
    resource:
      memory: "2048m"
      cpu: 1

  taskManager:
    resource:
      memory: "4096m"
      cpu: 2

  podTemplate:
    spec:
      containers:
        - name: flink-main-container
          env:
            - name: ENV
              value: "prod"
            - name: MYSQL_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: user-behavior-secret
                  key: mysql-password
          volumeMounts:
            - name: app-config
              mountPath: /opt/flink/usrlib/config
            - name: rocksdb-local
              mountPath: /data/flink/rocksdb
      volumes:
        - name: app-config
          configMap:
            name: user-behavior-config
        - name: rocksdb-local
          emptyDir: {}

  job:
    jarURI: local:///opt/flink/usrlib/flink-job-user-behavior-1.0.0.jar
    entryClass: io.github.atengk.flink.job.userbehavior.UserBehaviorJob
    parallelism: 8
    upgradeMode: savepoint
    args:
      - "--env"
      - "prod"
      - "--jobName"
      - "user-behavior-job"
      - "--config"
      - "/opt/flink/usrlib/config/application.yml"
      - "--parallelism"
      - "8"
```

提交作业配置：

```bash
# 创建命名空间
kubectl create namespace flink-prod

# 提交 FlinkDeployment
kubectl apply -f deploy/kubernetes/user-behavior-flinkdeployment.yaml

# 查看部署状态
kubectl get flinkdeployment -n flink-prod

# 查看 Flink Deployment 详情
kubectl describe flinkdeployment user-behavior-job -n flink-prod
```

Job 配置文件建议：

1. `metadata.name` 应与作业名一致，便于监控和日志检索。
2. `image` 必须使用不可变版本号，不建议生产使用 `latest`。
3. `jarURI` 使用 `local://` 时，Jar 必须已经在镜像内。
4. `upgradeMode` 生产建议使用 `savepoint` 或 `last-state`，不要随意使用 `stateless`。
5. `args` 中不要直接传递密码，应使用 Secret 或环境变量。
6. `flinkConfiguration` 中的 Checkpoint、状态后端、Metrics 等配置应纳入版本管理。

### 镜像构建

Kubernetes 部署通常使用镜像交付业务 Jar。镜像应包含 Flink Runtime、业务 Jar、必要 Connector、默认配置模板和启动依赖。生产镜像应尽量稳定、可追溯、可重复构建。

镜像目录建议：

```text
deploy/kubernetes/image/
├── Dockerfile
├── flink-job-user-behavior-1.0.0.jar
├── connectors/
│   ├── flink-connector-kafka.jar
│   └── flink-connector-jdbc.jar
└── config/
    └── application-default.yml
```

Dockerfile 示例：

文件位置：`deploy/kubernetes/image/Dockerfile`

```dockerfile
FROM flink:2.2.0-scala_2.12-java17

USER root

RUN mkdir -p /opt/flink/usrlib \
    && mkdir -p /opt/flink/usrlib/config \
    && mkdir -p /data/flink/rocksdb \
    && mkdir -p /data/flink/heapdump \
    && chown -R flink:flink /opt/flink/usrlib /data/flink

COPY --chown=flink:flink flink-job-user-behavior-1.0.0.jar /opt/flink/usrlib/flink-job-user-behavior-1.0.0.jar
COPY --chown=flink:flink config/application-default.yml /opt/flink/usrlib/config/application-default.yml

USER flink
```

构建和推送镜像：

```bash
IMAGE="registry.example.com/flink/flink-job-user-behavior:1.0.0"

docker build \
  -t "${IMAGE}" \
  -f deploy/kubernetes/image/Dockerfile \
  deploy/kubernetes/image

docker push "${IMAGE}"
```

镜像构建建议：

1. 镜像 Tag 使用明确版本号，例如 `1.0.0`、`1.0.1`，不要使用 `latest`。
2. 业务 Jar 和配置模板应放在 `/opt/flink/usrlib`。
3. Connector Jar 可以打入业务 Fat Jar，也可以放入 Flink lib，但要统一策略。
4. 敏感配置不要写入镜像。
5. 生产镜像构建完成后记录 Git Commit、Jar SHA256、镜像 Digest。
6. Flink Runtime 镜像版本必须与编译依赖和 Operator 配置兼容。

### ConfigMap 管理

ConfigMap 用于管理非敏感配置，例如业务配置、SQL 文件、日志配置、阈值参数和环境开关。ConfigMap 适合频繁变更但不涉及敏感信息的配置。生产中建议将 ConfigMap YAML 纳入 Git 管理，通过 GitOps 或发布系统变更。

ConfigMap 示例：

文件位置：`deploy/kubernetes/configmap-user-behavior.yaml`

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: user-behavior-config
  namespace: flink-prod
data:
  application.yml: |
    job:
      name: user-behavior-job
      env: prod

    kafka:
      bootstrap-servers: kafka-prod-01:9092,kafka-prod-02:9092
      source-topic: user_behavior_log
      sink-topic: user_behavior_clean
      group-id: user-behavior-job

    checkpoint:
      enabled: true
      interval-ms: 60000
      timeout-ms: 600000
      min-pause-ms: 30000

    dirty:
      sink-topic: dirty_user_behavior

  user_behavior_insert.sql: |
    INSERT INTO user_behavior_sink
    SELECT user_id, item_id, category_id, event_type, event_time
    FROM user_behavior_source
```

创建或更新 ConfigMap：

```bash
kubectl apply -f deploy/kubernetes/configmap-user-behavior.yaml

kubectl get configmap user-behavior-config -n flink-prod -o yaml
```

ConfigMap 管理建议：

1. 非敏感配置使用 ConfigMap。
2. 敏感配置不要放入 ConfigMap。
3. ConfigMap 变更后，已运行 Pod 不一定自动重启，需通过作业升级或重新部署生效。
4. 配置文件名称和挂载路径应固定。
5. SQL 文件可以随 ConfigMap 挂载，便于平台化管理。
6. ConfigMap 变更应有版本记录和发布审批。

### Secret 管理

Secret 用于管理密码、Token、证书、Webhook、AK/SK、数据库凭据和 Kafka 认证信息。Secret 可以通过环境变量或文件挂载注入到 Pod。生产中 Secret 应由 Kubernetes Secret、云厂商 Secret Manager、Vault 或平台安全模块管理。

Secret 示例：

文件位置：`deploy/kubernetes/secret-user-behavior.yaml`

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: user-behavior-secret
  namespace: flink-prod
type: Opaque
stringData:
  mysql-password: "change-me"
  redis-password: "change-me"
  kafka-jaas-config: "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"flink\" password=\"change-me\";"
```

创建 Secret：

```bash
kubectl apply -f deploy/kubernetes/secret-user-behavior.yaml

kubectl get secret user-behavior-secret -n flink-prod
```

通过环境变量使用 Secret：

```yaml
env:
  - name: MYSQL_PASSWORD
    valueFrom:
      secretKeyRef:
        name: user-behavior-secret
        key: mysql-password
  - name: REDIS_PASSWORD
    valueFrom:
      secretKeyRef:
        name: user-behavior-secret
        key: redis-password
```

通过文件挂载使用 Secret：

```yaml
volumes:
  - name: kafka-secret
    secret:
      secretName: user-behavior-secret

volumeMounts:
  - name: kafka-secret
    mountPath: /opt/flink/secrets
    readOnly: true
```

Secret 管理建议：

1. 密码、Token、AK/SK、Webhook 必须使用 Secret，不进入 Jar、镜像、ConfigMap 或日志。
2. 生产 Secret 不建议直接提交到 Git 仓库。
3. Secret 变更后需要规划作业重启或升级。
4. 日志打印配置时必须脱敏。
5. 不同环境使用不同 Secret，不要复用测试和生产凭据。
6. 对接云存储、Kafka SASL、数据库和告警系统时，都要统一 Secret 注入方式。

### 资源限制

资源限制用于控制 JobManager 和 TaskManager 的 CPU、内存、Slot、本地磁盘和容器调度。Operator 的 `jobManager.resource` 和 `taskManager.resource` 可以配置内存和 CPU，底层会映射到 Kubernetes Pod 资源。资源配置必须与 Flink 内存模型、Task Slot、状态后端和作业并行度一起设计。

资源配置示例：

```yaml
spec:
  jobManager:
    resource:
      memory: "2048m"
      cpu: 1

  taskManager:
    resource:
      memory: "4096m"
      cpu: 2

  flinkConfiguration:
    taskmanager.numberOfTaskSlots: "4"
    taskmanager.memory.process.size: "4096m"
    jobmanager.memory.process.size: "2048m"
    taskmanager.memory.managed.fraction: "0.4"
    taskmanager.memory.network.fraction: "0.1"
```

资源配置参考：

| 作业类型          | TaskManager 内存 | CPU    | Slot   | 状态后端           |
| ----------------- | ---------------- | ------ | ------ | ------------------ |
| 简单清洗          | 2g 到 4g         | 1 到 2 | 2 到 4 | HashMap 或 RocksDB |
| Kafka 清洗 + Sink | 4g 到 8g         | 2 到 4 | 2 到 4 | HashMap 或 RocksDB |
| 大窗口聚合        | 8g 到 16g        | 4 到 8 | 2 到 4 | RocksDB            |
| 双流 Join         | 8g 到 16g        | 4 到 8 | 2 到 4 | RocksDB            |
| 大状态去重        | 16g 以上         | 4 以上 | 1 到 4 | RocksDB            |

资源限制建议：

1. 容器内存应与 `taskmanager.memory.process.size`、`jobmanager.memory.process.size` 保持一致或留出明确余量。
2. RocksDB 大状态作业要预留本地磁盘和 Managed Memory。
3. CPU limit 过低可能导致 throttling，引起延迟抖动。
4. Slot 数不要远大于 CPU Core 数，避免线程竞争。
5. 补数作业和实时作业应使用不同 Namespace、队列或资源池。
6. 资源变更后要观察反压、Checkpoint、GC、RocksDB 和 Kafka Lag。

### 滚动升级

滚动升级通常指更新镜像、配置、资源参数或业务逻辑后，让 Operator 自动停止旧作业并启动新作业。在 Flink 流式作业中，“滚动升级”不能只理解为 Kubernetes Deployment 的无状态滚动替换，因为 Flink 作业有状态，需要明确状态迁移方式。

Operator 中作业升级由 `job.upgradeMode` 控制。官方参考文档说明，`savepoint` 表示先对运行中的作业触发 Savepoint，再关闭并从 Savepoint 恢复；`last-state` 表示使用最新可用 Checkpoint 或 Savepoint；`stateless` 表示空状态启动。([Apache Nightlies](https://nightlies.apache.org/flink/flink-kubernetes-operator-docs-release-1.14/docs/custom-resource/reference/?utm_source=chatgpt.com))

滚动升级操作示例：

```bash
# 修改镜像版本
kubectl patch flinkdeployment user-behavior-job \
  -n flink-prod \
  --type=merge \
  -p '{"spec":{"image":"registry.example.com/flink/flink-job-user-behavior:1.0.1"}}'

# 查看状态变化
kubectl get flinkdeployment user-behavior-job -n flink-prod -w

# 查看 Operator 日志
kubectl logs -n flink-system deploy/flink-kubernetes-operator -f
```

YAML 中升级策略示例：

```yaml
spec:
  job:
    jarURI: local:///opt/flink/usrlib/flink-job-user-behavior-1.0.1.jar
    entryClass: io.github.atengk.flink.job.userbehavior.UserBehaviorJob
    parallelism: 8
    upgradeMode: savepoint
```

滚动升级建议：

1. 有状态生产作业优先使用 `savepoint` 或 `last-state`。
2. 只有明确不需要历史状态时才使用 `stateless`。
3. 升级前确认 Checkpoint 正常、Savepoint 目录可写。
4. 镜像、Jar、配置、Connector 版本要一起记录。
5. 升级后检查作业状态、Checkpoint、Kafka Lag、Sink 写入和业务指标。
6. 如果升级失败，优先根据 Savepoint 或最近可用状态恢复旧版本。

### Savepoint 升级

Savepoint 升级是生产 Flink 作业最稳妥的升级方式之一。它适合修改业务逻辑、调整并行度、升级 Connector、修复 Bug、修改状态兼容字段和版本回滚。Operator 的 Job Management 文档说明，`savepoint` 模式会使用 Savepoint 停止和恢复应用，安全性最高，并可作为备份或分叉点。([Apache Nightlies](https://nightlies.apache.org/flink/flink-kubernetes-operator-docs-release-1.10/docs/custom-resource/job-management/?utm_source=chatgpt.com))

Savepoint 升级流程：

```text
确认当前作业健康
  -> 确认 Checkpoint 正常
  -> 修改镜像 / Jar / 配置
  -> upgradeMode 使用 savepoint
  -> Operator 触发 Savepoint
  -> 停止旧作业
  -> 从 Savepoint 启动新作业
  -> 验证指标和状态恢复
```

手动挂起并触发 Savepoint：

```bash
kubectl patch flinkdeployment user-behavior-job \
  -n flink-prod \
  --type=merge \
  -p '{"spec":{"job":{"state":"suspended","upgradeMode":"savepoint"}}}'
```

恢复运行：

```bash
kubectl patch flinkdeployment user-behavior-job \
  -n flink-prod \
  --type=merge \
  -p '{"spec":{"job":{"state":"running","upgradeMode":"savepoint"}}}'
```

从指定 Savepoint 重新部署示例：

```yaml
spec:
  job:
    jarURI: local:///opt/flink/usrlib/flink-job-user-behavior-1.0.1.jar
    entryClass: io.github.atengk.flink.job.userbehavior.UserBehaviorJob
    parallelism: 8
    upgradeMode: savepoint
    initialSavepointPath: s3://flink-prod/savepoints/user-behavior-job/savepoint-20260511-001
    savepointRedeployNonce: 1
```

官方文档说明，可以通过 `initialSavepointPath` 和递增 `savepointRedeployNonce` 从目标 Savepoint 重新部署作业；`savepointRedeployNonce` 变化时，Operator 会从 `initialSavepointPath` 指定的 Savepoint 重启作业。([Apache Nightlies](https://nightlies.apache.org/flink/flink-kubernetes-operator-docs-release-1.10/docs/custom-resource/job-management/?utm_source=chatgpt.com))

Savepoint 升级建议：

1. 生产升级前必须确认 Savepoint 目录可写且容量充足。
2. 核心算子必须设置稳定 `uid()`。
3. 状态名称、状态类型、Key 类型不要随意变更。
4. 从 Savepoint 恢复后要验证状态数据、窗口结果、去重状态和 Kafka 位点。
5. Savepoint 路径要进入发布记录。
6. Savepoint 文件不要被自动清理脚本误删。

## YARN 部署实践

本章节用于说明 Flink 作业在 YARN 环境中的部署实践，包括 YARN Session、YARN Per-Job、队列配置、资源参数、日志查看、作业停止、Savepoint 管理和常见问题排查。YARN 适合 Hadoop 生态环境，通常与 HDFS、Hive、Kerberos、队列资源管理和多租户隔离一起使用。

Flink 官方 YARN 文档说明，生产使用推荐 Application Mode，因为它在应用之间提供更好的隔离；Application Mode 会在 YARN 上启动 Flink 集群，并在 YARN 中的 JobManager 上执行应用 `main()` 方法。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/deployment/resource-providers/yarn/?utm_source=chatgpt.com))

### YARN Session

YARN Session 是先启动一个长期运行的 Flink 集群，再向该 Session 提交多个作业。Session 模式适合测试环境、临时任务、开发调试和共享资源池，但不适合多个核心生产作业长期混跑，因为作业之间资源隔离较弱。

启动 YARN Session：

```bash
export HADOOP_CONF_DIR=/etc/hadoop/conf
export YARN_CONF_DIR=/etc/hadoop/conf

./bin/yarn-session.sh \
  -Djobmanager.memory.process.size=2048m \
  -Dtaskmanager.memory.process.size=4096m \
  -Dtaskmanager.numberOfTaskSlots=4 \
  -Dyarn.application.name=flink-session-realtime \
  -Dyarn.application.queue=realtime \
  -d
```

提交作业到 YARN Session：

```bash
./bin/flink run \
  -t yarn-session \
  -Dyarn.application.id=application_1710000000000_0001 \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  /data/flink/jobs/flink-job-user-behavior-1.0.0.jar \
  --env test \
  --jobName user-behavior-job \
  --config /data/flink/config/test/application.yml \
  --parallelism 4
```

重新连接 Session：

```bash
./bin/yarn-session.sh -id application_1710000000000_0001
```

官方 YARN 文档说明，可以使用 `yarn-session.sh -id application_XXXX_YY` 重新连接到已有 YARN Session；也可以通过 `-Dkey=value` 在提交时覆盖配置。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.17/docs/deployment/resource-providers/yarn/?utm_source=chatgpt.com))

YARN Session 使用建议：

1. 适合测试、开发、临时作业和共享计算环境。
2. 不建议承载多个核心生产实时作业。
3. Session 集群重启会影响所有运行中的作业。
4. 多作业共享同一 Session 时，要特别关注 Slot、内存、Checkpoint 和外部 Sink 压力。
5. Session 模式下客户端执行 `main()`，依赖上传和 JobGraph 生成会消耗客户端资源。
6. 生产长期作业优先使用 YARN Application Mode。

### YARN Per-Job

YARN Per-Job 是历史部署模式，会为每个作业启动一个独立 Flink 集群。它曾用于提高作业隔离，但已经被 Application Mode 替代。Flink 1.18 部署文档明确说明，Per-Job Mode 只由 YARN 支持，并从 Flink 1.15 起废弃，建议使用 Application Mode 在 YARN 上为每个作业启动专用集群。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.18/docs/deployment/overview/?utm_source=chatgpt.com))

旧版本 Per-Job 提交示例：

```bash
# 历史兼容示例，新项目不推荐
./bin/flink run \
  -t yarn-per-job \
  --detached \
  -Dyarn.application.name=user-behavior-job \
  -Dyarn.application.queue=realtime \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  /data/flink/jobs/flink-job-user-behavior-1.0.0.jar \
  --env prod \
  --jobName user-behavior-job \
  --config /data/flink/config/prod/application.yml
```

Per-Job 交互命令：

```bash
# 查看 Per-Job 集群中的作业
./bin/flink list \
  -t yarn-per-job \
  -Dyarn.application.id=application_1710000000000_0001

# 取消作业
./bin/flink cancel \
  -t yarn-per-job \
  -Dyarn.application.id=application_1710000000000_0001 \
  <jobId>
```

Flink 旧版 YARN 文档说明，取消 Per-Job 集群上的作业会停止对应集群。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-1.17/docs/deployment/resource-providers/yarn/?utm_source=chatgpt.com))

YARN Per-Job 使用建议：

1. 新项目不要使用 Per-Job。
2. 旧作业应规划迁移到 Application Mode。
3. 迁移前验证提交脚本、Savepoint 恢复、依赖分发和日志路径。
4. Per-Job 相关参数在 Flink 2.x 环境中可能不再可用，应以当前集群版本文档为准。
5. 运维文档中应把 Per-Job 标记为历史兼容模式。
6. 如果平台仍保留 Per-Job，需要制定下线时间表。

### 队列配置

YARN 队列用于多租户资源隔离。实时作业、补数作业、测试作业和离线作业应进入不同队列，避免资源争抢。Flink 提交 YARN 作业时，可以通过 `-Dyarn.application.queue=<queue>` 指定队列。

提交到指定队列：

```bash
./bin/flink run \
  -t yarn-application \
  -Dyarn.application.name=user-behavior-job \
  -Dyarn.application.queue=realtime \
  -Djobmanager.memory.process.size=2048m \
  -Dtaskmanager.memory.process.size=4096m \
  -Dtaskmanager.numberOfTaskSlots=4 \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  hdfs:///flink/jars/flink-job-user-behavior-1.0.0.jar \
  --env prod \
  --jobName user-behavior-job \
  --config hdfs:///flink/config/prod/application.yml
```

推荐队列规划：

| 队列            | 用途                 |
| --------------- | -------------------- |
| `realtime`      | 核心生产实时作业     |
| `realtime-test` | 实时测试作业         |
| `batch-repair`  | 补数、回放、修复任务 |
| `ad-hoc`        | 临时查询、实验任务   |
| `platform`      | 平台服务类作业       |

队列配置建议：

1. 核心实时作业和补数作业必须分队列。
2. 测试作业不要提交到生产实时队列。
3. 队列资源要设置容量、最大容量和用户限制。
4. Kerberos 环境中还要确认用户是否有目标队列提交权限。
5. 发布脚本中显式写队列，不依赖默认队列。
6. 作业排查时先确认 YARN Application 是否进入了正确队列。

### 资源参数

YARN 部署时需要配置 JobManager、TaskManager、Slot、默认并行度、状态后端、Checkpoint、队列和 Application 名称。资源参数必须与作业吞吐、状态大小、并行度、Kafka 分区数和 Sink 能力匹配。

YARN Application Mode 资源参数示例：

```bash
./bin/flink run \
  -t yarn-application \
  -Djobmanager.memory.process.size=2048m \
  -Dtaskmanager.memory.process.size=8192m \
  -Dtaskmanager.numberOfTaskSlots=4 \
  -Dparallelism.default=8 \
  -Dstate.backend=rocksdb \
  -Dstate.backend.rocksdb.memory.managed=true \
  -Dexecution.checkpointing.incremental=true \
  -Dexecution.checkpointing.interval=60s \
  -Dexecution.checkpointing.timeout=10min \
  -Dexecution.checkpointing.dir=hdfs:///flink/checkpoints/prod/user-behavior-job \
  -Dexecution.checkpointing.savepoint-dir=hdfs:///flink/savepoints/prod/user-behavior-job \
  -Dyarn.application.name=user-behavior-job \
  -Dyarn.application.queue=realtime \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  hdfs:///flink/jars/flink-job-user-behavior-1.0.0.jar \
  --env prod \
  --jobName user-behavior-job \
  --config hdfs:///flink/config/prod/application.yml \
  --parallelism 8
```

资源参数建议：

| 参数                              | 建议                                     |
| --------------------------------- | ---------------------------------------- |
| `jobmanager.memory.process.size`  | 普通作业 1g 到 2g，复杂作业适当增加      |
| `taskmanager.memory.process.size` | 根据状态、吞吐、Slot 数设置              |
| `taskmanager.numberOfTaskSlots`   | 通常与 CPU Core、算子负载协同设置        |
| `parallelism.default`             | 与 Kafka 分区数、状态分布、Sink 能力匹配 |
| `state.backend`                   | 大状态作业使用 RocksDB                   |
| `execution.checkpointing.dir`     | 使用 HDFS 等可靠共享存储                 |
| `yarn.application.queue`          | 显式指定队列                             |

资源参数使用建议：

1. 不要只调大并行度，必须同步检查 Slot 和 TaskManager 数量。
2. 大状态作业优先增加 TaskManager 内存和 RocksDB 本地磁盘。
3. `taskmanager.numberOfTaskSlots` 过大可能导致单 JVM 内资源争抢。
4. Kafka Source 并行度不应盲目超过 Topic 分区数。
5. Sink 慢时，增加 Flink 资源未必有效，可能需要优化下游。
6. 资源参数变更应通过压测或测试环境验证。

### 日志查看

YARN 环境下日志通常可以通过 YARN ResourceManager UI、Flink Web UI、`yarn logs` 命令和集群日志目录查看。Application Mode 下，JobManager 和 TaskManager 作为 YARN Container 运行，日志由 YARN 聚合。

查看 YARN 应用：

```bash
# 查看运行中的 YARN 应用
yarn application -list

# 查看全部应用并过滤作业名
yarn application -list -appStates ALL | grep user-behavior-job
```

查看应用日志：

```bash
# 查看指定 Application 的全部日志
yarn logs -applicationId application_1710000000000_0001

# 过滤错误日志
yarn logs -applicationId application_1710000000000_0001 | grep -E "ERROR|Exception|Caused by"

# 将日志保存到本地文件
yarn logs -applicationId application_1710000000000_0001 > user-behavior-job.log
```

查看 Flink Web UI：

```bash
# 方式一：通过 YARN ResourceManager UI 进入 Flink Web UI

# 方式二：如果平台提供代理地址，使用代理访问 JobManager Web UI
```

日志查看建议：

1. 先从 YARN Application ID 定位作业。
2. 再通过 Flink Web UI 查看 Exceptions、Checkpoints、Back Pressure。
3. 依赖问题通常在 JobManager 启动日志中暴露。
4. 算子运行异常通常在 TaskManager 日志中暴露。
5. Connector 初始化失败要看完整 `Caused by`。
6. 日志聚合未开启时，作业结束后可能无法通过 `yarn logs` 查看完整日志，需要平台侧配置日志聚合。

### 作业停止

YARN 作业停止方式包括 Flink cancel、stop with savepoint、YARN kill。不同方式语义不同：`cancel` 偏向直接取消作业；`stop --savepointPath` 会先触发 Savepoint 再停止，更适合有状态生产作业；`yarn application -kill` 是资源层强制停止，通常只用于异常场景。

查看作业 ID：

```bash
./bin/flink list \
  -t yarn-application \
  -Dyarn.application.id=application_1710000000000_0001
```

取消作业：

```bash
./bin/flink cancel \
  -t yarn-application \
  -Dyarn.application.id=application_1710000000000_0001 \
  <jobId>
```

停止并触发 Savepoint：

```bash
./bin/flink stop \
  -t yarn-application \
  -Dyarn.application.id=application_1710000000000_0001 \
  --savepointPath hdfs:///flink/savepoints/prod/user-behavior-job \
  <jobId>
```

强制停止 YARN Application：

```bash
yarn application -kill application_1710000000000_0001
```

作业停止建议：

1. 生产有状态作业优先使用 `flink stop --savepointPath`。
2. 不要直接用 `yarn application -kill` 停止正常生产作业。
3. 停止前确认 Checkpoint 是否正常。
4. 停止后记录 Savepoint 路径、Job ID、Application ID 和停止原因。
5. 停止后检查下游是否还有写入残留。
6. Application Mode 中作业停止通常会结束对应应用集群，需要确认影响范围。

### Savepoint 管理

YARN 环境中 Savepoint 通常存储在 HDFS。Savepoint 用于升级、回滚、并行度调整、状态迁移和故障恢复。生产作业应有统一 Savepoint 目录规范、命名规范和清理策略。

推荐目录规范：

```text
hdfs:///flink/savepoints/
└── prod/
    └── user-behavior-job/
        ├── savepoint-20260511-001/
        ├── savepoint-20260511-002/
        └── rollback/
```

触发 Savepoint：

```bash
./bin/flink savepoint \
  -t yarn-application \
  -Dyarn.application.id=application_1710000000000_0001 \
  <jobId> \
  hdfs:///flink/savepoints/prod/user-behavior-job
```

从 Savepoint 恢复：

```bash
./bin/flink run \
  -t yarn-application \
  -s hdfs:///flink/savepoints/prod/user-behavior-job/savepoint-20260511-001 \
  -Djobmanager.memory.process.size=2048m \
  -Dtaskmanager.memory.process.size=8192m \
  -Dtaskmanager.numberOfTaskSlots=4 \
  -Dyarn.application.name=user-behavior-job \
  -Dyarn.application.queue=realtime \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  hdfs:///flink/jars/flink-job-user-behavior-1.0.1.jar \
  --env prod \
  --jobName user-behavior-job \
  --config hdfs:///flink/config/prod/application.yml
```

Savepoint 管理建议：

1. Savepoint 目录按环境和作业隔离。
2. Savepoint 路径必须记录到发布单。
3. 升级前创建 Savepoint，升级后保留回滚 Savepoint。
4. 算子 `uid()` 必须稳定，否则状态可能无法映射。
5. Savepoint 不应与 Checkpoint 混用目录。
6. 清理 Savepoint 前必须确认没有发布、回滚或补数依赖。

### 常见问题排查

YARN 部署常见问题主要集中在 Hadoop 配置、队列权限、资源不足、依赖缺失、Kerberos、HDFS 路径、日志聚合、Application 启动失败和 Savepoint 恢复失败。

常见问题表：

| 问题                      | 可能原因                              | 处理                              |
| ------------------------- | ------------------------------------- | --------------------------------- |
| 作业提交失败              | `HADOOP_CONF_DIR` 未配置              | 设置 Hadoop/YARN 配置目录         |
| Application 一直 ACCEPTED | 队列资源不足                          | 检查队列容量和用户资源限制        |
| Container 启动失败        | 内存超限、依赖缺失、权限问题          | 查看 YARN Container 日志          |
| 找不到主类                | Jar 中未包含入口类或 `-c` 写错        | 使用 `jar tf` 检查                |
| Connector not found       | Connector Jar 缺失或 SPI 未合并       | 检查 Fat Jar 和 `$FLINK_HOME/lib` |
| HDFS 写入失败             | 路径权限不足                          | 检查 HDFS ACL 和用户权限          |
| Checkpoint 失败           | 存储不可写、状态过大、Sink 慢         | 查看 Checkpoint 页面和日志        |
| Savepoint 恢复失败        | 算子 uid、状态类型、并行度不兼容      | 检查升级变更和状态兼容            |
| Kafka 消费异常            | 认证、网络、Topic 权限或 groupId 错误 | 验证 Kafka 客户端配置             |
| Kerberos 认证失败         | keytab、principal、krb5.conf 错误     | 检查安全配置和票据续期            |

排查命令集合：

```bash
# 查看 YARN 应用
yarn application -list -appStates ALL | grep user-behavior-job

# 查看 YARN 应用状态
yarn application -status application_1710000000000_0001

# 查看 YARN 日志
yarn logs -applicationId application_1710000000000_0001 | grep -E "ERROR|Exception|Caused by"

# 查看 HDFS 路径权限
hdfs dfs -ls hdfs:///flink/checkpoints/prod
hdfs dfs -ls hdfs:///flink/savepoints/prod

# 查看 Jar 入口类
jar tf flink-job-user-behavior-1.0.0.jar | grep UserBehaviorJob

# 查看 Jar SPI 文件
jar tf flink-job-user-behavior-1.0.0.jar | grep META-INF/services

# 查看依赖树
mvn dependency:tree
```

YARN 问题排查建议：

1. 作业无法启动时，优先看 YARN Application 日志。
2. 作业启动后失败，优先看 Flink Web UI 的 Exceptions 和 TaskManager 日志。
3. 资源不足时，先看队列状态，再看 TaskManager 内存和 Slot。
4. 依赖问题用 `jar tf` 和 `mvn dependency:tree` 排查。
5. Savepoint 恢复失败时，检查算子 `uid()`、状态名称、状态类型和 Flink 版本。
6. 如果生产作业频繁重启，不要只调重启策略，应定位数据异常、外部系统异常或状态问题。

以下继续展开 `CI/CD 流程` 与 `数据一致性设计` 两部分。

## CI/CD 流程

本章节用于说明 Flink Java 项目的持续集成、持续交付和发布管理流程。Flink 作业通常是长期运行的有状态应用，CI/CD 不仅要完成代码编译和 Jar 构建，还要覆盖单元测试、依赖检查、镜像构建、配置校验、制品归档、Savepoint 发布、回滚策略和发布审批。对于生产实时作业，发布流程必须比普通无状态服务更谨慎，因为作业状态、Kafka 位点、Checkpoint、Savepoint 和 Sink 写入语义都会影响数据结果。

推荐整体流程如下：

```text
开发分支提交代码
  -> 代码扫描
  -> 单元测试
  -> 打包构建
  -> 依赖检查
  -> 集成测试
  -> 构建 Fat Jar
  -> 构建镜像
  -> 推送制品仓库
  -> 部署测试环境
  -> 数据回放验证
  -> 发布审批
  -> 生产 Savepoint
  -> 生产升级
  -> 指标验证
  -> 发布完成或回滚
```

### 代码分支管理

代码分支管理用于规范 Flink 作业的开发、测试、发布和修复流程。实时作业通常包含核心业务逻辑、状态模型、Connector 配置、SQL 文件、部署脚本和监控配置，任何变更都可能影响生产数据结果，因此必须通过分支、合并请求和审批机制进行管理。

推荐分支模型如下：

| 分支        | 用途                                     |
| ----------- | ---------------------------------------- |
| `main`      | 生产稳定分支，只允许发布完成后的代码合入 |
| `develop`   | 日常集成分支，用于测试环境部署           |
| `feature/*` | 新功能开发分支                           |
| `bugfix/*`  | 普通缺陷修复分支                         |
| `hotfix/*`  | 生产紧急修复分支                         |
| `release/*` | 发布准备分支，用于冻结版本、修复发布问题 |
| `tag`       | 生产发布版本，例如 `v1.0.0`、`v1.0.1`    |

推荐合并流程：

```text
feature/*
  -> develop
  -> release/*
  -> main
  -> tag
```

生产紧急修复流程：

```text
main
  -> hotfix/*
  -> main
  -> tag
  -> develop
```

分支管理建议：

1. `main` 分支必须受保护，禁止直接提交。
2. 所有变更必须通过 Merge Request 或 Pull Request。
3. 涉及状态模型、Key 类型、状态名称、算子 `uid()` 的变更必须单独标注。
4. 涉及 Connector、Sink、Checkpoint、Savepoint 的变更必须附带测试记录。
5. SQL 文件、部署 YAML、告警规则和 Grafana 看板也应纳入版本管理。
6. 生产发布必须创建 Git Tag，并记录 Jar 版本、镜像 Digest、配置版本和 Savepoint 路径。

### 构建流水线

构建流水线用于将代码编译为可运行的 Flink 作业制品。构建流程应保证 Java 版本、Maven 版本、Flink 版本、Connector 版本和依赖锁定一致，避免“本地可运行，流水线不可运行”或“测试环境可运行，生产环境依赖冲突”。

构建流水线主要步骤：

| 步骤       | 说明                                 |
| ---------- | ------------------------------------ |
| 拉取代码   | 指定分支、Commit 或 Tag              |
| 设置 JDK   | 与 Flink 运行环境兼容                |
| Maven 缓存 | 加速依赖下载                         |
| 编译       | 执行 `mvn clean compile`             |
| 单元测试   | 执行 `mvn test`                      |
| 打包       | 执行 `mvn package`                   |
| 依赖检查   | 执行 `mvn dependency:tree`           |
| 制品归档   | 保存 Jar、依赖树、测试报告           |
| 镜像构建   | 构建 Kubernetes 部署镜像             |
| 推送制品   | 推送到 Nexus、Artifactory、Harbor 等 |

GitLab CI 示例：

```yaml
stages:
  - validate
  - test
  - package
  - image
  - deploy

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository"
  MAVEN_CLI_OPTS: "-B -U -Dfile.encoding=UTF-8"
  IMAGE_NAME: "registry.example.com/flink/flink-job-user-behavior"

cache:
  key: "${CI_PROJECT_NAME}"
  paths:
    - .m2/repository

validate:
  stage: validate
  image: maven:3.9.9-eclipse-temurin-17
  script:
    - mvn ${MAVEN_CLI_OPTS} validate
    - mvn ${MAVEN_CLI_OPTS} dependency:tree -DoutputFile=target/dependency-tree.txt
  artifacts:
    paths:
      - target/dependency-tree.txt
    expire_in: 7 days

unit-test:
  stage: test
  image: maven:3.9.9-eclipse-temurin-17
  script:
    - mvn ${MAVEN_CLI_OPTS} test
  artifacts:
    when: always
    reports:
      junit:
        - "**/target/surefire-reports/TEST-*.xml"
    expire_in: 7 days

package-job:
  stage: package
  image: maven:3.9.9-eclipse-temurin-17
  script:
    - mvn ${MAVEN_CLI_OPTS} clean package -DskipTests
    - ls -lh flink-job-user-behavior/target/
  artifacts:
    paths:
      - flink-job-user-behavior/target/flink-job-user-behavior.jar
    expire_in: 30 days

build-image:
  stage: image
  image: docker:27
  services:
    - docker:27-dind
  script:
    - docker build -t ${IMAGE_NAME}:${CI_COMMIT_SHORT_SHA} -f deploy/kubernetes/image/Dockerfile .
    - docker push ${IMAGE_NAME}:${CI_COMMIT_SHORT_SHA}
  only:
    - develop
    - main
    - /^release\/.*$/
    - /^hotfix\/.*$/
```

构建流水线建议：

1. 流水线必须固定 JDK 版本。
2. Maven 构建参数要统一，避免不同环境使用不同 Profile。
3. 每次构建都应输出依赖树，便于排查依赖冲突。
4. Fat Jar、测试报告、依赖树、镜像 Digest 都应作为流水线制品保存。
5. 生产发布使用 Git Tag 触发，不建议直接使用分支最新提交发布。
6. 构建失败必须阻断后续部署，不允许手工绕过测试和打包阶段。

### 单元测试流水线

单元测试流水线用于在代码合并前自动验证普通 Java 逻辑、Flink 算子逻辑、配置解析、序列化、SQL 拼接和工具类。对于 Flink 项目，单元测试不仅要验证函数输出，还要验证状态、定时器、侧输出流、脏数据分类和幂等键生成。

推荐测试分层如下：

| 测试类型            | 执行阶段       | 说明                                     |
| ------------------- | -------------- | ---------------------------------------- |
| 单元测试            | 每次提交       | 测试普通 Java 逻辑                       |
| 算子测试            | 每次提交       | 测试 `MapFunction`、`ProcessFunction` 等 |
| MiniCluster 测试    | 合并前或夜间   | 测试完整 Flink 拓扑                      |
| Testcontainers 测试 | 合并前或发布前 | 测试 Kafka、MySQL、Redis 等外部依赖      |
| 数据回放测试        | 发布前         | 使用历史样本验证结果                     |
| 性能压测            | 大版本发布前   | 验证吞吐、延迟、Checkpoint 和状态        |

Maven Surefire 配置示例：

```xml
<build>
    <plugins>
        <!-- 单元测试插件，用于执行 JUnit 5 测试 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.5.2</version>
            <configuration>
                <useModulePath>false</useModulePath>
                <includes>
                    <include>**/*Test.java</include>
                </includes>
                <systemPropertyVariables>
                    <file.encoding>UTF-8</file.encoding>
                </systemPropertyVariables>
            </configuration>
        </plugin>

        <!-- 集成测试插件，用于执行较慢的 IT 测试 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-failsafe-plugin</artifactId>
            <version>3.5.2</version>
            <configuration>
                <includes>
                    <include>**/*IT.java</include>
                </includes>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>integration-test</goal>
                        <goal>verify</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

测试命令：

```bash
# 执行普通单元测试
mvn clean test

# 执行集成测试
mvn clean verify

# 跳过集成测试，仅打包
mvn clean package -DskipITs

# 跳过所有测试，仅用于临时本地构建，不建议发布使用
mvn clean package -DskipTests
```

单元测试流水线建议：

1. Merge Request 必须执行单元测试。
2. 算子函数、解析函数、幂等键生成函数必须有测试。
3. 涉及状态的函数要验证状态更新和清理逻辑。
4. 涉及序列化的代码要测试空值、非法数据和兼容字段。
5. 集成测试可以较慢，但发布前必须执行。
6. 测试失败不能进入部署阶段。

### 镜像构建

镜像构建用于将 Flink Runtime、业务 Jar、必要依赖和默认配置模板打包成可在 Kubernetes 中运行的镜像。镜像构建应保证可复现、可追溯和可回滚。镜像 Tag 不应使用 `latest`，应使用 Git Commit、版本号或发布号。

推荐镜像标签：

```text
registry.example.com/flink/flink-job-user-behavior:1.0.0
registry.example.com/flink/flink-job-user-behavior:1.0.1
registry.example.com/flink/flink-job-user-behavior:20260511-001
registry.example.com/flink/flink-job-user-behavior:8f3a2c1
```

Dockerfile 示例：

```dockerfile
FROM flink:2.2.0-scala_2.12-java17

USER root

RUN mkdir -p /opt/flink/usrlib \
    && mkdir -p /opt/flink/usrlib/config \
    && mkdir -p /data/flink/rocksdb \
    && mkdir -p /data/flink/heapdump \
    && chown -R flink:flink /opt/flink/usrlib /data/flink

COPY --chown=flink:flink flink-job-user-behavior/target/flink-job-user-behavior.jar \
    /opt/flink/usrlib/flink-job-user-behavior.jar

COPY --chown=flink:flink deploy/kubernetes/config/application-default.yml \
    /opt/flink/usrlib/config/application-default.yml

USER flink
```

镜像构建流水线示例：

```yaml
build-image:
  stage: image
  image: docker:27
  services:
    - docker:27-dind
  variables:
    IMAGE_TAG: "${CI_COMMIT_SHORT_SHA}"
  script:
    - docker build -t ${IMAGE_NAME}:${IMAGE_TAG} -f deploy/kubernetes/image/Dockerfile .
    - docker push ${IMAGE_NAME}:${IMAGE_TAG}
    - docker inspect --format='{{index .RepoDigests 0}}' ${IMAGE_NAME}:${IMAGE_TAG} > image-digest.txt
  artifacts:
    paths:
      - image-digest.txt
    expire_in: 30 days
```

镜像构建建议：

1. 生产镜像必须使用固定版本 Tag。
2. 镜像中不要包含生产密码、Token、AK/SK。
3. 镜像中 Jar 名称可以固定，版本通过镜像 Tag 管理。
4. 镜像 Digest 要进入发布记录。
5. Flink Runtime 镜像版本必须与编译版本和 Operator 配置匹配。
6. 镜像构建完成后应做漏洞扫描和基础启动验证。

### 制品管理

制品管理用于保存 Jar、镜像、配置、依赖树、测试报告、SQL 文件、部署 YAML 和发布记录。Flink 作业发布不能只记录“发布成功”，还必须能在问题发生时准确定位使用了哪个 Jar、哪个镜像、哪个配置、哪个 Savepoint 和哪个提交参数。

推荐制品清单如下：

| 制品           | 说明                        |
| -------------- | --------------------------- |
| Fat Jar        | Flink 作业 Jar              |
| Docker 镜像    | Kubernetes 部署镜像         |
| 镜像 Digest    | 不可变镜像标识              |
| Maven 依赖树   | 排查依赖冲突                |
| 测试报告       | 单元测试和集成测试结果      |
| 部署 YAML      | Kubernetes Operator 配置    |
| 提交脚本       | YARN 或 Standalone 提交脚本 |
| SQL 文件       | DDL、DML、UDF 注册脚本      |
| 配置文件       | 环境配置，不含敏感值        |
| Savepoint 路径 | 升级和回滚状态点            |
| 发布记录       | 版本、审批人、发布时间      |

推荐制品目录：

```text
artifacts/
└── user-behavior-job/
    └── 1.0.0/
        ├── flink-job-user-behavior.jar
        ├── dependency-tree.txt
        ├── test-report/
        ├── kubernetes/
        │   ├── flinkdeployment.yaml
        │   ├── configmap.yaml
        │   └── secret-template.yaml
        ├── scripts/
        │   ├── run-yarn-prod.sh
        │   └── rollback-yarn-prod.sh
        └── release.md
```

发布记录模板：

```markdown
# user-behavior-job 发布记录

## 基本信息

- 作业名称：user-behavior-job
- 版本号：1.0.0
- Git Commit：8f3a2c1
- Jar：flink-job-user-behavior.jar
- 镜像：registry.example.com/flink/flink-job-user-behavior:1.0.0
- 镜像 Digest：sha256:xxxx
- 发布时间：2026-05-11 20:00:00
- 发布人：Ateng

## 变更内容

- 增加用户行为清洗逻辑
- 增加 Kafka Sink 幂等键
- 调整 Checkpoint 间隔为 60 秒

## 状态信息

- 发布前 Savepoint：s3://flink-prod/savepoints/user-behavior-job/savepoint-20260511-001
- Checkpoint 目录：s3://flink-prod/checkpoints/user-behavior-job
- 并行度：8
- TaskManager：4g / 2 CPU / 4 Slot

## 验证结果

- 单元测试：通过
- 集成测试：通过
- 数据回放：通过
- Checkpoint：正常
- Kafka Lag：正常
- Sink 写入：正常

## 回滚方案

- 使用上一版本镜像：registry.example.com/flink/flink-job-user-behavior:0.9.9
- 从发布前 Savepoint 恢复
```

制品管理建议：

1. Jar、镜像、部署配置和发布记录必须一一对应。
2. 制品不可覆盖，使用版本号或 Commit 标识。
3. 生产 Secret 不应作为制品保存。
4. 发布记录必须包含 Savepoint 路径。
5. 依赖树和测试报告要保存，便于事后追踪。
6. 制品仓库应设置保留周期，但生产回滚版本不能过早清理。

### 自动部署

自动部署用于将构建好的制品发布到测试环境或生产环境。Flink 作业自动部署必须区别于普通无状态服务部署，因为它涉及 Savepoint、状态恢复、并行度、Checkpoint、Kafka 位点和 Sink 写入语义。

Kubernetes Operator 自动部署通常通过 `kubectl apply` 更新 `FlinkDeployment`：

```yaml
deploy-test:
  stage: deploy
  image: bitnami/kubectl:1.31
  script:
    - sed -i "s#IMAGE_PLACEHOLDER#${IMAGE_NAME}:${CI_COMMIT_SHORT_SHA}#g" deploy/kubernetes/user-behavior-flinkdeployment.yaml
    - kubectl apply -f deploy/kubernetes/configmap-user-behavior-test.yaml
    - kubectl apply -f deploy/kubernetes/user-behavior-flinkdeployment.yaml
    - kubectl rollout status deployment/user-behavior-job -n flink-test --timeout=300s
  only:
    - develop
```

YARN 自动部署脚本示例：

```bash
#!/usr/bin/env bash

set -euo pipefail

JOB_NAME="user-behavior-job"
MAIN_CLASS="io.github.atengk.flink.job.userbehavior.UserBehaviorJob"
JAR_PATH="hdfs:///flink/jars/user-behavior-job/flink-job-user-behavior-1.0.0.jar"
CONFIG_PATH="hdfs:///flink/config/prod/user-behavior/application.yml"
QUEUE="realtime"
PARALLELISM="8"

flink run \
  -t yarn-application \
  -Djobmanager.memory.process.size=2048m \
  -Dtaskmanager.memory.process.size=4096m \
  -Dtaskmanager.numberOfTaskSlots=4 \
  -Dparallelism.default="${PARALLELISM}" \
  -Dyarn.application.name="${JOB_NAME}" \
  -Dyarn.application.queue="${QUEUE}" \
  -c "${MAIN_CLASS}" \
  "${JAR_PATH}" \
  --env prod \
  --jobName "${JOB_NAME}" \
  --config "${CONFIG_PATH}" \
  --parallelism "${PARALLELISM}"
```

自动部署建议：

1. 测试环境可以自动部署，生产环境必须有审批或人工确认。
2. 生产自动部署前必须确认当前作业 Checkpoint 正常。
3. 有状态升级必须使用 Savepoint 或 Last-State 策略。
4. 部署后要自动检查作业状态、Checkpoint、Kafka Lag、Sink 错误和业务指标。
5. 部署失败必须停止流水线，并保留日志和制品。
6. 自动部署脚本禁止硬编码密码，必须使用 Secret 或配置中心。

### 回滚策略

回滚策略用于在发布失败、数据异常、延迟异常、Checkpoint 失败或 Sink 写入异常时快速恢复到上一个稳定版本。Flink 作业回滚必须同时考虑代码版本、配置版本和状态版本。

常见回滚场景如下：

| 场景               | 回滚方式                                   |
| ------------------ | ------------------------------------------ |
| 代码逻辑异常       | 使用旧 Jar 或旧镜像从发布前 Savepoint 恢复 |
| 配置错误           | 回退 ConfigMap 或配置文件，重新部署        |
| Connector 版本异常 | 回退旧依赖包或旧镜像                       |
| 状态兼容失败       | 使用发布前 Savepoint + 旧版本代码          |
| Sink 重复写入      | 暂停作业，修复下游数据，再从正确状态恢复   |
| Kafka Lag 激增     | 回退后观察消费恢复情况                     |

Kubernetes Operator 回滚示例：

```bash
# 回退镜像版本
kubectl patch flinkdeployment user-behavior-job \
  -n flink-prod \
  --type=merge \
  -p '{"spec":{"image":"registry.example.com/flink/flink-job-user-behavior:0.9.9","job":{"upgradeMode":"savepoint"}}}'
```

从指定 Savepoint 回滚示例：

```yaml
spec:
  image: registry.example.com/flink/flink-job-user-behavior:0.9.9
  job:
    jarURI: local:///opt/flink/usrlib/flink-job-user-behavior.jar
    entryClass: io.github.atengk.flink.job.userbehavior.UserBehaviorJob
    parallelism: 8
    upgradeMode: savepoint
    initialSavepointPath: s3://flink-prod/savepoints/user-behavior-job/savepoint-before-release-1.0.0
    savepointRedeployNonce: 2
```

YARN 回滚示例：

```bash
flink run \
  -t yarn-application \
  -s hdfs:///flink/savepoints/prod/user-behavior-job/savepoint-before-release-1.0.0 \
  -Djobmanager.memory.process.size=2048m \
  -Dtaskmanager.memory.process.size=4096m \
  -Dtaskmanager.numberOfTaskSlots=4 \
  -Dyarn.application.name=user-behavior-job-rollback \
  -Dyarn.application.queue=realtime \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  hdfs:///flink/jars/user-behavior-job/flink-job-user-behavior-0.9.9.jar \
  --env prod \
  --jobName user-behavior-job \
  --config hdfs:///flink/config/prod/user-behavior/application-0.9.9.yml
```

回滚策略建议：

1. 发布前必须保留 Savepoint。
2. 回滚使用旧代码、旧配置和发布前 Savepoint 三者配套。
3. 不能用新版本产生的不兼容状态强行恢复旧版本。
4. 回滚后必须验证业务数据是否重复、漏写或窗口回退。
5. 如果 Sink 已产生错误数据，需要先制定下游修复策略。
6. 回滚操作必须进入发布记录。

### 发布审批

发布审批用于控制生产作业变更风险。实时作业的生产发布必须经过代码、测试、数据、运维和业务层面的检查。审批不是形式化流程，而是为了确认状态兼容、数据一致性、回滚方案和监控告警已经就绪。

推荐审批项：

| 审批项   | 检查内容                                        |
| -------- | ----------------------------------------------- |
| 代码变更 | 是否影响状态、Key、算子 `uid()`、Sink 语义      |
| 测试结果 | 单元测试、集成测试、回放测试是否通过            |
| 依赖变更 | Connector、Flink、Kafka、JDBC 版本是否变化      |
| 配置变更 | Checkpoint、并行度、资源、Topic、表名是否变化   |
| 数据影响 | 是否影响指标口径、维表 Join、幂等键             |
| 发布方案 | 是否有 Savepoint、发布步骤和验证步骤            |
| 回滚方案 | 是否能从发布前 Savepoint 恢复                   |
| 监控告警 | 是否有作业、Checkpoint、Lag、Sink、业务指标告警 |

发布审批清单示例：

```markdown
# Flink 作业发布审批清单

## 基本信息

- 作业名称：
- 发布版本：
- Git Commit：
- 发布环境：
- 发布窗口：
- 发布人：

## 变更评估

- [ ] 是否变更状态模型
- [ ] 是否变更 Key 类型或 Key 字段
- [ ] 是否变更算子 uid
- [ ] 是否变更 Sink 幂等键
- [ ] 是否变更 Checkpoint 配置
- [ ] 是否变更 Connector 版本
- [ ] 是否变更业务指标口径

## 测试结果

- [ ] 单元测试通过
- [ ] 算子测试通过
- [ ] 集成测试通过
- [ ] 数据回放通过
- [ ] 测试环境运行稳定

## 发布准备

- [ ] Jar 已归档
- [ ] 镜像已推送
- [ ] 配置已确认
- [ ] 发布前 Savepoint 路径已生成
- [ ] 回滚版本已确认
- [ ] 监控告警已确认

## 发布后验证

- [ ] 作业状态 Running
- [ ] Checkpoint 正常
- [ ] Kafka Lag 正常
- [ ] Sink 写入正常
- [ ] 业务指标正常
```

发布审批建议：

1. 核心实时作业必须人工审批。
2. 热修复也不能跳过 Savepoint 和回滚方案。
3. 状态变更必须经过技术负责人审批。
4. 指标口径变更必须经过业务方确认。
5. 发布后必须有观察窗口。
6. 发布记录必须可追溯。

## 数据一致性设计

本章节用于说明 Flink 作业从 Source、Flink 内部状态、Sink 到端到端链路的数据一致性设计。数据一致性不是只依赖 Checkpoint。完整链路需要 Source 可重放、Flink 状态可恢复、Sink 支持事务或幂等，以及业务层面的去重、补偿和校验机制。

一致性设计整体结构如下：

```text
Source 可重放
  + Flink Checkpoint
  + 状态后端可靠存储
  + Sink 幂等或事务
  + 业务去重
  + 数据校验
  + 补偿修复
= 可恢复、可追溯、可验证的数据链路
```

### Source 端一致性

Source 端一致性用于保证作业失败恢复后能够从正确位置继续读取数据。不同 Source 的一致性能力不同，Kafka、CDC、文件、Socket、自定义 Source 的恢复能力差异较大。

常见 Source 一致性能力如下：

| Source        | 一致性基础                    | 风险                         |
| ------------- | ----------------------------- | ---------------------------- |
| Kafka Source  | Offset 随 Checkpoint 保存     | Topic 数据过期会导致无法回放 |
| MySQL CDC     | Binlog 位点随 Checkpoint 保存 | Binlog 被清理会导致恢复失败  |
| 文件 Source   | 文件 Split 和读取位置         | 文件被修改或删除会影响恢复   |
| Socket Source | 不可重放                      | 通常不适合生产一致性链路     |
| 自定义 Source | 需要自行保存位点              | 实现不当会重复或丢失         |
| Datagen       | 测试用                        | 不具备真实业务一致性意义     |

Kafka Source 一致性设计示例：

```java
KafkaSource<String> kafkaSource = KafkaSource.<String>builder()
        .setBootstrapServers("kafka-prod-01:9092,kafka-prod-02:9092")
        .setTopics("user_behavior_log")
        .setGroupId("user-behavior-job")
        .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.LATEST))
        .setValueOnlyDeserializer(new SimpleStringSchema())
        .build();
```

Source 端一致性建议：

1. 生产 Source 必须具备可重放能力。
2. Kafka Topic 保留时间必须大于最大停机恢复时间。
3. CDC Binlog 保留时间必须覆盖作业停机、升级和故障恢复时间。
4. 作业恢复应依赖 Checkpoint 或 Savepoint，不应随意修改启动 Offset。
5. Source 并行度调整前要验证状态恢复。
6. 自定义 Source 必须实现位点状态保存和恢复。

### Flink 内部一致性

Flink 内部一致性主要依赖 Checkpoint、状态后端、Barrier、状态快照和恢复机制。Flink 内部 Exactly Once 指的是每条事件对 Flink 管理状态的影响恰好一次，而不是事件在物理上绝对只被处理一次。

Flink 内部一致性涉及以下组件：

| 组件               | 作用                             |
| ------------------ | -------------------------------- |
| Checkpoint         | 周期性保存状态快照和 Source 位点 |
| Barrier            | 标记 Checkpoint 边界             |
| State Backend      | 管理运行时状态                   |
| Checkpoint Storage | 持久化状态快照                   |
| Restart Strategy   | 失败后决定是否恢复               |
| Operator UID       | 恢复时匹配状态                   |
| Savepoint          | 手动触发的一致性状态快照         |

Checkpoint 配置示例：

```java
StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

env.enableCheckpointing(60_000L, CheckpointingMode.EXACTLY_ONCE);
env.getCheckpointConfig().setCheckpointTimeout(600_000L);
env.getCheckpointConfig().setMinPauseBetweenCheckpoints(30_000L);
env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
env.getCheckpointConfig().setExternalizedCheckpointCleanup(
        CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION
);

env.getCheckpointConfig().setCheckpointStorage("hdfs:///flink/checkpoints/prod/user-behavior-job");
```

内部一致性设计建议：

1. 生产实时作业必须开启 Checkpoint。
2. Checkpoint Storage 必须使用可靠共享存储。
3. 核心算子必须设置稳定 `uid()`。
4. 状态名称和状态类型进入生产后不要随意变更。
5. 大状态作业使用 RocksDB 和增量 Checkpoint。
6. Checkpoint 连续失败应视为生产故障，而不是普通告警。

### Sink 端一致性

Sink 端一致性决定最终结果是否会重复、丢失或不一致。即使 Source 和 Flink 内部都是 Exactly Once，如果 Sink 是普通 Insert 或外部 API 调用，端到端仍可能出现重复写入。

常见 Sink 一致性能力如下：

| Sink               | 常见语义                     | 设计建议                           |
| ------------------ | ---------------------------- | ---------------------------------- |
| Kafka Sink         | At Least Once / Exactly Once | Exactly Once 使用事务和 Checkpoint |
| JDBC Sink          | 通常 At Least Once           | 使用主键和 Upsert 实现幂等         |
| Redis Sink         | 通常 At Least Once           | 使用覆盖式写入、TTL 和幂等 Key     |
| Elasticsearch Sink | 通常 At Least Once           | 使用业务 ID 作为文档 ID            |
| File Sink          | 可实现 Exactly Once          | 依赖 Checkpoint 提交文件           |
| Doris / StarRocks  | 依赖 Connector 和事务能力    | 使用主键、Label、2PC 或幂等写入    |
| 外部 HTTP API      | 通常 At Least Once           | 必须设计请求幂等键                 |

JDBC 幂等写入示例：

```sql
INSERT INTO user_event_count (
    event_type,
    window_start,
    window_end,
    event_count,
    update_time
)
VALUES (?, ?, ?, ?, NOW())
ON DUPLICATE KEY UPDATE
    event_count = VALUES(event_count),
    update_time = NOW();
```

Kafka Exactly Once Sink 示例：

```java
KafkaSink<String> kafkaSink = KafkaSink.<String>builder()
        .setBootstrapServers("kafka-prod-01:9092,kafka-prod-02:9092")
        .setRecordSerializer(
                KafkaRecordSerializationSchema.builder()
                        .setTopic("user_behavior_clean")
                        .setValueSerializationSchema(new SimpleStringSchema())
                        .build()
        )
        .setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
        .setTransactionalIdPrefix("user-behavior-job-")
        .build();
```

Sink 端一致性建议：

1. 每个 Sink 都必须明确交付语义。
2. 普通 Insert Sink 不应承诺 Exactly Once。
3. 窗口结果必须使用主键或幂等键。
4. 外部 API 调用必须携带请求幂等 ID。
5. Sink 写入失败后要确认恢复是否会重复写入。
6. 端到端一致性以最弱 Sink 为准。

### 端到端 Exactly Once

端到端 Exactly Once 表示从 Source 到 Sink 的最终结果在故障恢复后仍然等价于每条业务事件只生效一次。它不是单独打开 Checkpoint 就能保证的，而是 Source、Flink 状态和 Sink 三端共同满足条件。

端到端 Exactly Once 条件如下：

| 层次          | 条件                                        |
| ------------- | ------------------------------------------- |
| Source        | 可重放，位点可被 Checkpoint 管理            |
| Flink         | 开启 Exactly Once Checkpoint                |
| State Backend | 状态可可靠持久化和恢复                      |
| Sink          | 支持事务提交、两阶段提交或幂等写入          |
| 业务模型      | 有稳定唯一键、版本号或窗口主键              |
| 运维流程      | 发布、回滚、补偿都基于 Savepoint 或幂等机制 |

端到端 Exactly Once 组合示例：

```text
Kafka Source
  -> Checkpoint 保存 Offset
  -> Flink 状态 Exactly Once
  -> Kafka Transactional Sink
= 较完整的端到端 Exactly Once
Kafka Source
  -> Checkpoint 保存 Offset
  -> Flink 状态 Exactly Once
  -> MySQL Upsert 主键覆盖
= 结果层面的幂等一致性
Kafka Source
  -> Checkpoint 保存 Offset
  -> Flink 状态 Exactly Once
  -> 普通 HTTP API
= 通常只能达到 At Least Once，除非 API 支持幂等键
```

端到端 Exactly Once 设计建议：

1. 明确区分 Flink 内部 Exactly Once 和端到端 Exactly Once。
2. Kafka 到 Kafka 链路优先使用 Kafka 事务 Sink。
3. 数据库结果表优先使用主键 Upsert。
4. OLAP 和数据湖 Sink 要确认 Connector 的事务或提交语义。
5. 外部 HTTP、Redis、Elasticsearch 等系统通常需要业务幂等辅助。
6. 发布、重启、回滚都要验证是否重复写入或漏写。

### 幂等写入设计

幂等写入是数据一致性设计中最常用、最实用的手段。幂等写入表示同一条业务数据重复写入多次，最终结果仍然一致。对于大多数 JDBC、Redis、Elasticsearch、OLAP 写入场景，幂等设计比强事务更容易落地。

幂等键设计示例：

| 数据类型     | 幂等键                                          |
| ------------ | ----------------------------------------------- |
| 明细事件     | `eventId`                                       |
| 订单事件     | `orderId + eventType`                           |
| 用户窗口指标 | `userId + metricName + windowStart + windowEnd` |
| 事件类型指标 | `eventType + windowStart + windowEnd`           |
| 宽表数据     | `businessId + version`                          |
| 告警数据     | `alarmName + bizKey + windowStart`              |
| 补偿数据     | `repairBatchId + businessKey`                   |

幂等键生成工具示例：

下面的代码用于统一生成窗口指标幂等键，避免各个作业自行拼接导致格式不一致。

```java
package io.github.atengk.flink.common.id;

import cn.hutool.core.util.StrUtil;

/**
 * Flink 幂等键生成工具
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class IdempotentKeyUtil {

    /**
     * 生成窗口指标幂等键
     *
     * @param metricName 指标名称
     * @param dimensionKey 维度 Key
     * @param windowStart 窗口开始时间
     * @param windowEnd 窗口结束时间
     * @return 幂等键
     */
    public static String windowMetricKey(String metricName, String dimensionKey, Long windowStart, Long windowEnd) {
        if (StrUtil.hasBlank(metricName, dimensionKey)) {
            throw new IllegalArgumentException("指标名称和维度Key不能为空");
        }
        if (windowStart == null || windowEnd == null) {
            throw new IllegalArgumentException("窗口时间不能为空");
        }

        return StrUtil.format("{}:{}:{}:{}", metricName, dimensionKey, windowStart, windowEnd);
    }

    /**
     * 生成明细事件幂等键
     *
     * @param source 来源系统
     * @param eventId 事件ID
     * @return 幂等键
     */
    public static String eventKey(String source, String eventId) {
        if (StrUtil.hasBlank(source, eventId)) {
            throw new IllegalArgumentException("来源系统和事件ID不能为空");
        }

        return StrUtil.format("{}:{}", source, eventId);
    }
}
```

MySQL 幂等表设计示例：

```sql
CREATE TABLE user_event_count (
    idempotent_key VARCHAR(256) NOT NULL COMMENT '幂等键',
    metric_name VARCHAR(128) NOT NULL COMMENT '指标名称',
    dimension_key VARCHAR(128) NOT NULL COMMENT '维度Key',
    window_start BIGINT NOT NULL COMMENT '窗口开始时间',
    window_end BIGINT NOT NULL COMMENT '窗口结束时间',
    metric_value BIGINT NOT NULL COMMENT '指标值',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (idempotent_key)
) COMMENT='用户行为窗口指标表';
```

幂等写入建议：

1. 所有 Sink 结果都应设计幂等键。
2. 幂等键必须稳定，不能包含随机数或处理时间。
3. 窗口指标幂等键必须包含窗口开始和结束时间。
4. 明细事件优先使用全局唯一事件 ID。
5. 累加类更新要避免重复累加，优先覆盖最终结果。
6. 幂等键设计应写入数据契约文档。

### 去重设计

去重设计用于处理 Source 重放、上游重复发送、故障恢复重复处理、CDC 重复变更和业务重复事件。去重不应只依赖 Flink Checkpoint，因为很多重复数据来自上游业务系统或 Sink 恢复。

常见去重方式如下：

| 去重方式       | 适用场景                       |
| -------------- | ------------------------------ |
| Source 端去重  | 上游能保证 eventId 唯一        |
| Flink 状态去重 | 短周期事件去重                 |
| Sink 主键去重  | 数据库、OLAP、ES 等结果去重    |
| Kafka Key 去重 | 下游按 Key 保留最新值          |
| 离线对账去重   | 事后修复和校验                 |
| 布隆过滤器     | 大规模短周期去重，需要接受误判 |

基于 `MapState` 的短周期去重示例：

下面的代码用于按事件 ID 去重，重复事件会被丢弃。生产使用时需要配置状态 TTL，防止状态无限增长。

```java
package io.github.atengk.flink.job.userbehavior.dedup;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.flink.model.UserBehaviorEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

/**
 * 用户行为事件去重函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserBehaviorDeduplicateFunction extends KeyedProcessFunction<String, UserBehaviorEvent, UserBehaviorEvent> {

    private transient MapState<String, Boolean> eventIdState;

    /**
     * 初始化去重状态
     *
     * @param parameters Flink 配置
     */
    @Override
    public void open(org.apache.flink.configuration.Configuration parameters) {
        MapStateDescriptor<String, Boolean> descriptor = new MapStateDescriptor<>(
                "user-behavior-event-id-state",
                String.class,
                Boolean.class
        );
        eventIdState = getRuntimeContext().getMapState(descriptor);
    }

    /**
     * 处理事件并按 eventId 去重
     *
     * @param value 用户行为事件
     * @param ctx 上下文
     * @param out 输出收集器
     * @throws Exception 状态访问异常
     */
    @Override
    public void processElement(UserBehaviorEvent value, Context ctx, Collector<UserBehaviorEvent> out) throws Exception {
        String eventId = buildEventId(value);
        if (StrUtil.isBlank(eventId)) {
            log.warn("事件ID为空，丢弃异常事件，userId：{}，eventType：{}", value.getUserId(), value.getEventType());
            return;
        }

        Boolean exists = eventIdState.get(eventId);
        if (Boolean.TRUE.equals(exists)) {
            log.debug("检测到重复事件，eventId：{}", eventId);
            return;
        }

        eventIdState.put(eventId, true);
        out.collect(value);
    }

    /**
     * 构建事件ID
     *
     * @param value 用户行为事件
     * @return 事件ID
     */
    private String buildEventId(UserBehaviorEvent value) {
        if (value == null || StrUtil.hasBlank(value.getUserId(), value.getItemId(), value.getEventType())) {
            return "";
        }

        return StrUtil.format("{}:{}:{}:{}",
                value.getUserId(),
                value.getItemId(),
                value.getEventType(),
                value.getEventTime());
    }
}
```

去重使用示例：

```java
DataStream<UserBehaviorEvent> deduplicatedStream = eventStream
        .keyBy(UserBehaviorEvent::getUserId)
        .process(new UserBehaviorDeduplicateFunction())
        .name("user-behavior-deduplicate")
        .uid("user-behavior-deduplicate");
```

去重设计建议：

1. 先确认重复来源，再选择去重位置。
2. 去重 Key 必须稳定且足够唯一。
3. Flink 状态去重必须配置 TTL。
4. 大规模去重要评估状态大小和 Checkpoint 成本。
5. Sink 端仍然建议有主键或唯一约束。
6. 去重逻辑要保留重复数量指标，便于发现上游异常。

### 补偿机制

补偿机制用于处理迟到数据、脏数据修复、Sink 写入失败、下游故障、历史回放和数据对账差异。补偿机制不是失败后手工改库，而应是设计好的数据修复链路。

补偿机制类型如下：

| 补偿类型      | 说明                             |
| ------------- | -------------------------------- |
| 迟到数据补偿  | 超过窗口允许迟到范围后进入补偿流 |
| 脏数据修复    | 修复解析逻辑后回放脏数据         |
| Sink 失败补偿 | 失败数据进入重试 Topic 或失败表  |
| 离线重算      | 按时间范围重新计算指标           |
| 下游覆盖      | 使用幂等键覆盖错误结果           |
| 差异修正      | 对账发现差异后生成修正记录       |

补偿事件模型示例：

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 数据补偿事件
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompensationEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String compensationId;

    private String sourceJob;

    private String bizKey;

    private String compensationType;

    private String payload;

    private String reason;

    private Long eventTime;

    private Long createTime;
}
```

补偿流程示例：

```text
发现异常数据
  -> 写入补偿 Topic / 补偿表
  -> 补偿作业读取
  -> 修复解析或重新计算
  -> 写入临时结果
  -> 对账验证
  -> 覆盖正式结果
  -> 记录补偿批次
```

补偿作业启动命令示例：

```bash
flink run \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorCompensationJob \
  hdfs:///flink/jars/flink-job-user-behavior-1.0.1.jar \
  --env prod \
  --jobName user-behavior-compensation-job \
  --inputTopic user_behavior_compensation \
  --outputTopic user_behavior_clean_repair \
  --repairBatchId repair-20260511-001 \
  --startTime 2026-05-11T00:00:00 \
  --endTime 2026-05-11T23:59:59
```

补偿机制建议：

1. 补偿数据必须可追溯，有补偿批次号。
2. 补偿输出先写临时 Topic 或临时表。
3. 补偿合并必须依赖幂等键。
4. 补偿前后要做数据校验。
5. 补偿作业和实时作业必须隔离资源和消费组。
6. 补偿记录应进入发布或运维文档。

### 数据校验机制

数据校验机制用于确认实时链路输出是否正确。它包括输入输出量校验、窗口指标校验、主键唯一性校验、金额校验、维表命中率校验、实时离线对账和 Sink 写入校验。没有数据校验机制的实时链路，即使作业 Running，也不能说明数据正确。

常见校验类型如下：

| 校验类型   | 示例                                   |
| ---------- | -------------------------------------- |
| 数量校验   | Kafka 输入量与清洗输出量、脏数据量对账 |
| 唯一性校验 | `eventId`、`orderId` 是否重复          |
| 金额校验   | 实时 GMV 与离线 GMV 差异               |
| 窗口校验   | 每分钟指标是否在合理范围               |
| 维表校验   | 维表命中率是否低于阈值                 |
| 延迟校验   | 端到端延迟是否超过 SLA                 |
| Sink 校验  | 写入成功数与输出数是否匹配             |
| 采样校验   | 抽样对比原始事件和宽表字段             |

数据校验结果模型示例：

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 数据校验结果
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataCheckResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String checkId;

    private String jobName;

    private String checkName;

    private String checkType;

    private String status;

    private String message;

    private Long expectedValue;

    private Long actualValue;

    private Long checkTime;
}
```

输入输出数量校验示例：

```sql
-- 示例：按分钟对清洗输入、输出、脏数据进行校验
SELECT
    window_start,
    window_end,
    input_count,
    clean_count,
    dirty_count,
    input_count - clean_count - dirty_count AS diff_count
FROM realtime_data_check
WHERE dt = '2026-05-11'
  AND ABS(input_count - clean_count - dirty_count) > 0;
```

实时与离线指标对账示例：

```sql
-- 示例：对比实时和离线每小时事件数
SELECT
    r.stat_hour,
    r.event_type,
    r.event_count AS realtime_count,
    o.event_count AS offline_count,
    ABS(r.event_count - o.event_count) AS diff_count
FROM realtime_event_count_hour r
JOIN offline_event_count_hour o
  ON r.stat_hour = o.stat_hour
 AND r.event_type = o.event_type
WHERE ABS(r.event_count - o.event_count) > 100;
```

数据校验机制建议：

1. 每个核心作业都要有输入、输出、脏数据三方数量校验。
2. 指标类作业要与离线数仓定期对账。
3. 明细类作业要校验主键唯一性。
4. 宽表类作业要校验维表命中率和关键字段空值率。
5. 校验结果应写入独立校验表，并接入告警。
6. 数据校验发现问题后，应能触发补偿或修复流程。

以下继续展开 `安全设计` 与 `版本升级与兼容` 两部分。

## 安全设计

本章节用于说明 Flink Java 作业在 Kafka、数据库、Kerberos、SSL、敏感字段、权限、审计和密钥管理方面的安全设计。Flink 作业通常会访问 Kafka、HDFS、HBase、Hive、MySQL、Redis、Doris、StarRocks、对象存储和告警服务，因此安全设计不能只依赖网络隔离，还需要认证、授权、加密、脱敏、审计和密钥生命周期管理。

Flink 官方 Kerberos 文档说明，Flink 的 Kerberos 安全基础设施主要用于让作业安全访问 Kafka、HDFS、HBase、ZooKeeper 等系统；Flink SSL 文档也说明，Flink 可以分别为内部通信和外部 REST/Web UI 通信启用 SSL/TLS。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/deployment/security/security-kerberos/?utm_source=chatgpt.com))

### Kafka 认证

Kafka 认证用于保护 Flink 作业访问 Kafka Broker 的身份安全。生产 Kafka 集群常见认证方式包括 SASL/SCRAM、SASL/GSSAPI、SASL/PLAIN、SASL/OAUTHBEARER 和 SSL 双向认证。Kafka 官方安全文档说明，Kafka 支持客户端到 Broker 的 SSL 或 SASL 认证，同时也支持读写授权和传输加密。([Apache Kafka](https://kafka.apache.org/42/security/security-overview/?utm_source=chatgpt.com))

Kafka SASL/SCRAM 配置示例：

```yaml
kafka:
  # Kafka Broker 地址
  bootstrap-servers: kafka-prod-01:9093,kafka-prod-02:9093,kafka-prod-03:9093

  # Kafka Source Topic
  source-topic: user_behavior_log

  # Kafka Sink Topic
  sink-topic: user_behavior_clean

  # 消费组 ID
  group-id: user-behavior-job

  security:
    # Kafka 安全协议，SASL_SSL 表示 SASL 认证 + SSL 加密
    protocol: SASL_SSL

    # SASL 机制，常见为 SCRAM-SHA-256 或 SCRAM-SHA-512
    mechanism: SCRAM-SHA-512

    # JAAS 配置中的密码应通过 Secret 或环境变量注入，不应写死
    jaas-config: org.apache.kafka.common.security.scram.ScramLoginModule required username="flink_user" password="${KAFKA_PASSWORD}";
```

DataStream Kafka Source 设置认证参数示例：

```java
Properties kafkaProperties = new Properties();
kafkaProperties.setProperty("security.protocol", "SASL_SSL");
kafkaProperties.setProperty("sasl.mechanism", "SCRAM-SHA-512");
kafkaProperties.setProperty(
        "sasl.jaas.config",
        "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"flink_user\" password=\"" + System.getenv("KAFKA_PASSWORD") + "\";"
);

KafkaSource<String> kafkaSource = KafkaSource.<String>builder()
        .setBootstrapServers("kafka-prod-01:9093,kafka-prod-02:9093")
        .setTopics("user_behavior_log")
        .setGroupId("user-behavior-job")
        .setProperties(kafkaProperties)
        .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.LATEST))
        .setValueOnlyDeserializer(new SimpleStringSchema())
        .build();
```

Kafka ACL 设计建议：

| 资源              | 权限    | 说明                               |
| ----------------- | ------- | ---------------------------------- |
| Source Topic      | `READ`  | 作业读取原始数据                   |
| Source Group      | `READ`  | 作业使用指定消费组                 |
| Sink Topic        | `WRITE` | 作业写入清洗或指标结果             |
| Transactional ID  | `WRITE` | Kafka Exactly Once Sink 需要       |
| Dead Letter Topic | `WRITE` | 写入脏数据或失败数据               |
| Admin 权限        | 禁止    | Flink 作业通常不应拥有集群管理权限 |

Kafka 认证建议：

1. 生产 Kafka 禁止明文匿名访问。
2. 作业账号按作业或业务域独立创建。
3. Source、Sink、事务 ID、消费组分别配置最小权限。
4. 密码通过 Kubernetes Secret、YARN 本地化安全文件、环境变量或配置中心注入。
5. 日志中禁止打印 `sasl.jaas.config` 原文。
6. Kafka 认证变更后必须验证 Source 消费、Sink 写入和 Exactly Once 事务提交。

### 数据库认证

数据库认证用于保护 Flink 作业访问 MySQL、PostgreSQL、Oracle、Doris、StarRocks 等数据库系统的安全。Flink 作业访问数据库时应使用独立账号，按读写权限拆分，并限制访问库表范围。不要使用 DBA、root 或共享账号运行生产作业。

数据库账号建议如下：

| 账号类型         | 权限               | 使用场景                    |
| ---------------- | ------------------ | --------------------------- |
| `flink_reader`   | `SELECT`           | 维表查询、Lookup Join       |
| `flink_writer`   | `INSERT`、`UPDATE` | 写入结果表                  |
| `flink_cdc`      | CDC 所需权限       | MySQL CDC 读取快照和 Binlog |
| `flink_admin`    | 谨慎使用           | 仅平台初始化或自动建表场景  |
| `readonly_audit` | `SELECT`           | 数据校验和审计查询          |

MySQL 账号授权示例：

```sql
-- 创建 Flink 维表只读账号
CREATE USER 'flink_reader'@'%' IDENTIFIED BY 'change-me';

-- 授予维表查询权限
GRANT SELECT ON dim_user_profile.* TO 'flink_reader'@'%';

-- 创建 Flink 结果写入账号
CREATE USER 'flink_writer'@'%' IDENTIFIED BY 'change-me';

-- 授予结果表写入权限
GRANT SELECT, INSERT, UPDATE ON realtime_report.* TO 'flink_writer'@'%';

-- 刷新权限
FLUSH PRIVILEGES;
```

JDBC 配置示例：

```yaml
jdbc:
  # JDBC 地址，不建议在 URL 中拼接密码
  url: jdbc:mysql://mysql-prod-vip:3306/realtime_report?useSSL=true&serverTimezone=Asia/Shanghai

  # 数据库驱动
  driver-class-name: com.mysql.cj.jdbc.Driver

  # 用户名可以写配置，密码通过 Secret 注入
  username: flink_writer

  # 密码从环境变量或 Secret 读取
  password-env: MYSQL_WRITER_PASSWORD

  # 连接超时，避免外部系统异常时无限等待
  connection-timeout-ms: 5000
```

数据库认证建议：

1. 读写账号分离。
2. 每个作业或业务域使用独立数据库账号。
3. 生产作业禁止使用 root、DBA 或个人账号。
4. 密码必须通过 Secret 或配置中心管理。
5. JDBC URL、账号和密码日志必须脱敏。
6. 数据库账号应配置来源 IP、库表权限和密码轮换周期。

### Kerberos 集成

Kerberos 集成用于 Hadoop 安全集群环境，常见于 YARN、HDFS、HBase、Hive、ZooKeeper 和 Kafka GSSAPI 场景。Flink 官方 Kerberos 文档说明，Flink 支持通过 Hadoop Security Module、JAAS Security Module 和 ZooKeeper Security Module 在不同部署模式下使用 Kerberos，并明确支持 Kafka、HDFS、HBase、ZooKeeper 等组件。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/deployment/security/security-kerberos/?utm_source=chatgpt.com))

Flink Kerberos 配置示例：

```yaml
# Kerberos keytab 文件路径，所有 JobManager 和 TaskManager 需要可访问
security.kerberos.login.keytab: /etc/security/keytabs/flink.keytab

# Kerberos principal
security.kerberos.login.principal: flink@EXAMPLE.COM

# Kerberos krb5.conf 路径
security.kerberos.krb5-conf.path: /etc/krb5.conf

# 启用 Hadoop 安全上下文，用于 HDFS、HBase、YARN 等
security.kerberos.login.contexts: Client,KafkaClient
```

Kafka GSSAPI 配置示例：

```properties
# Kafka 使用 SASL_PLAINTEXT 或 SASL_SSL，生产建议 SASL_SSL
security.protocol=SASL_SSL

# Kerberos 机制
sasl.mechanism=GSSAPI

# Kafka 服务名，通常为 kafka
sasl.kerberos.service.name=kafka
```

Kubernetes 中挂载 keytab 和 krb5.conf 示例：

```yaml
volumes:
  - name: kerberos-secret
    secret:
      secretName: flink-kerberos-secret

volumeMounts:
  - name: kerberos-secret
    mountPath: /etc/security/keytabs
    readOnly: true
```

Kerberos 集成建议：

1. keytab 必须通过 Secret 或安全文件分发，禁止打入 Jar 或镜像。
2. principal 应按作业或平台账号规划，不建议多人共用个人 principal。
3. YARN 模式下要确认提交用户、HDFS 目录权限和队列权限。
4. Kafka GSSAPI 场景需要同时配置 JAAS、服务名和安全协议。
5. Kerberos 票据续期、keytab 权限和时钟同步必须检查。
6. 作业启动失败时优先查看 `LoginException`、`GSSException`、`SaslException` 和 HDFS 权限错误。

### SSL 配置

SSL/TLS 用于加密 Flink 内部通信、REST/Web UI 通信以及访问外部系统时的数据传输。Flink SSL 文档说明，Flink 区分内部连接和外部 REST 连接；内部连接包括 JobManager、TaskManager、ResourceManager 之间的 RPC、Shuffle 数据传输和 Blob 服务，外部连接包括 Web UI、REST API 和 CLI 到集群的通信。SSL 可以分别通过 `security.ssl.internal.enabled` 和 `security.ssl.rest.enabled` 开启。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-master/docs/deployment/security/security-ssl/?utm_source=chatgpt.com))

Flink SSL 配置示例：

```yaml
# 启用 Flink 内部通信 SSL
security.ssl.internal.enabled: true

# 启用 Flink REST / Web UI SSL
security.ssl.rest.enabled: true

# 内部通信 keystore
security.ssl.internal.keystore: /etc/flink/certs/internal.keystore
security.ssl.internal.keystore-password: ${FLINK_INTERNAL_KEYSTORE_PASSWORD}
security.ssl.internal.key-password: ${FLINK_INTERNAL_KEY_PASSWORD}

# 内部通信 truststore
security.ssl.internal.truststore: /etc/flink/certs/internal.truststore
security.ssl.internal.truststore-password: ${FLINK_INTERNAL_TRUSTSTORE_PASSWORD}

# REST 端 keystore
security.ssl.rest.keystore: /etc/flink/certs/rest.keystore
security.ssl.rest.keystore-password: ${FLINK_REST_KEYSTORE_PASSWORD}
security.ssl.rest.key-password: ${FLINK_REST_KEY_PASSWORD}

# REST 端 truststore
security.ssl.rest.truststore: /etc/flink/certs/rest.truststore
security.ssl.rest.truststore-password: ${FLINK_REST_TRUSTSTORE_PASSWORD}

# 主机名校验，生产建议保持 true
security.ssl.verify-hostname: true
```

Kafka SSL 配置示例：

```properties
security.protocol=SASL_SSL
ssl.truststore.location=/etc/flink/certs/kafka.client.truststore.jks
ssl.truststore.password=${KAFKA_TRUSTSTORE_PASSWORD}
ssl.keystore.location=/etc/flink/certs/kafka.client.keystore.jks
ssl.keystore.password=${KAFKA_KEYSTORE_PASSWORD}
ssl.key.password=${KAFKA_KEY_PASSWORD}
```

SSL 配置建议：

1. Flink 内部 SSL 和 REST SSL 分开配置，便于差异化管理。
2. Web UI 和 REST API 暴露到非可信网络时必须启用 SSL。
3. 证书、keystore、truststore 通过 Secret 或安全挂载分发。
4. 生产环境不要关闭主机名校验，除非有明确的证书策略和风险接受。
5. SSL 会增加 CPU 开销，需要在高吞吐链路中压测验证。
6. 证书到期时间必须纳入监控和运维日历。

### 敏感字段脱敏

敏感字段脱敏用于防止日志、脏数据、审计数据、监控指标和下游宽表泄露个人隐私或安全凭据。常见敏感字段包括手机号、身份证、邮箱、银行卡、地址、Token、密码、AK/SK、Cookie、设备号、IP 地址和精确地理位置。

常见脱敏规则如下：

| 字段   | 脱敏方式                         |
| ------ | -------------------------------- |
| 手机号 | 保留前三后四，例如 `138****5678` |
| 身份证 | 保留前六后四                     |
| 邮箱   | 保留用户名首尾和域名             |
| 银行卡 | 保留后四位                       |
| 地址   | 保留省市，隐藏详细地址           |
| Token  | 全部隐藏                         |
| 密码   | 全部隐藏                         |
| IP     | IPv4 最后一段隐藏                |
| 设备号 | 保留前四后四                     |

脱敏工具类示例：

下面的代码用于统一处理日志和脏数据中的敏感字段脱敏。

```java
package io.github.atengk.flink.common.security;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;

import java.util.Set;

/**
 * 敏感字段脱敏工具
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class SensitiveMaskUtil {

    private static final Set<String> PASSWORD_KEYS = Set.of("password", "token", "secret", "accessKey", "secretKey");

    /**
     * 按字段名称脱敏
     *
     * @param fieldName 字段名称
     * @param value 字段值
     * @return 脱敏后的字段值
     */
    public static String mask(String fieldName, String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }

        String normalizedName = StrUtil.blankToDefault(fieldName, "").toLowerCase();

        if (PASSWORD_KEYS.stream().anyMatch(normalizedName::contains)) {
            return "****";
        }
        if (normalizedName.contains("phone") || normalizedName.contains("mobile")) {
            return DesensitizedUtil.mobilePhone(value);
        }
        if (normalizedName.contains("idcard") || normalizedName.contains("identity")) {
            return DesensitizedUtil.idCardNum(value, 6, 4);
        }
        if (normalizedName.contains("email")) {
            return DesensitizedUtil.email(value);
        }
        if (normalizedName.contains("bankcard")) {
            return DesensitizedUtil.bankCard(value);
        }

        return value;
    }

    /**
     * 判断是否为敏感字段
     *
     * @param fieldName 字段名称
     * @return 是否敏感
     */
    public static boolean isSensitiveField(String fieldName) {
        if (StrUtil.isBlank(fieldName)) {
            return false;
        }

        String normalizedName = fieldName.toLowerCase();
        return normalizedName.contains("password")
                || normalizedName.contains("token")
                || normalizedName.contains("secret")
                || normalizedName.contains("phone")
                || normalizedName.contains("mobile")
                || normalizedName.contains("idcard")
                || normalizedName.contains("identity")
                || normalizedName.contains("email")
                || normalizedName.contains("bankcard");
    }
}
```

脱敏使用示例：

```java
String maskedPhone = SensitiveMaskUtil.mask("phone", "13812345678");
log.info("用户手机号脱敏结果：{}", maskedPhone);
```

敏感字段脱敏建议：

1. 日志、脏数据、审计数据都要执行脱敏。
2. 脱敏规则应统一封装，不要每个作业单独实现。
3. 密码、Token、AK/SK 不允许部分保留，直接全隐藏。
4. 脏数据保留原文时要考虑合规要求，必要时只保留摘要或加密存储。
5. 数据宽表中是否保留敏感字段必须经过权限和合规评审。
6. 脱敏前后要保留可排查能力，例如保留 hash 值用于关联排查。

### 权限控制

权限控制用于保证 Flink 作业、开发人员、运维人员和平台服务只能访问必要资源。权限控制应覆盖代码仓库、制品仓库、Flink Web UI、Kubernetes/YARN、Kafka、数据库、HDFS、对象存储、监控系统和告警系统。

权限控制对象如下：

| 对象         | 控制方式                               |
| ------------ | -------------------------------------- |
| 代码仓库     | 分支保护、MR 审批                      |
| 制品仓库     | 只读、发布、删除权限分离               |
| Kubernetes   | Namespace、ServiceAccount、RBAC        |
| YARN         | 队列权限、提交用户                     |
| Kafka        | ACL 控制 Topic、Group、TransactionalId |
| 数据库       | 最小化账号权限                         |
| HDFS         | 路径 ACL、Kerberos                     |
| Flink Web UI | 入口认证、网络隔离、反向代理           |
| Secret       | 只允许运行时读取                       |
| 监控告警     | 查看、编辑、静默权限分离               |

Kubernetes RBAC 示例：

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: flink
  namespace: flink-prod

---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: flink-job-role
  namespace: flink-prod
rules:
  # 允许 Flink Operator 管理作业相关资源
  - apiGroups: [""]
    resources: ["pods", "services", "configmaps", "secrets"]
    verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]

  # 允许访问 Flink 自定义资源
  - apiGroups: ["flink.apache.org"]
    resources: ["flinkdeployments", "flinksessionjobs"]
    verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]

---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: flink-job-rolebinding
  namespace: flink-prod
subjects:
  - kind: ServiceAccount
    name: flink
    namespace: flink-prod
roleRef:
  kind: Role
  name: flink-job-role
  apiGroup: rbac.authorization.k8s.io
```

权限控制建议：

1. 默认按最小权限分配。
2. 测试环境和生产环境权限必须隔离。
3. Flink Web UI 不应直接暴露到公网。
4. 发布人员不应直接拥有生产数据库写权限。
5. Secret 查看权限应严格限制。
6. 所有高权限操作必须有审批和审计记录。

### 审计日志

审计日志用于记录与安全和数据结果相关的重要操作，包括作业提交、停止、升级、Savepoint、配置变更、权限变更、密钥读取、脏数据修复、补数任务和人工干预。审计日志不同于普通运行日志，它需要可追溯、不可随意删除、字段稳定，并能支持问题复盘。

审计事件建议如下：

| 审计事件                  | 说明              |
| ------------------------- | ----------------- |
| `JOB_SUBMIT`              | 作业提交          |
| `JOB_CANCEL`              | 作业取消          |
| `JOB_STOP_WITH_SAVEPOINT` | 带 Savepoint 停止 |
| `JOB_UPGRADE`             | 作业升级          |
| `JOB_ROLLBACK`            | 作业回滚          |
| `CONFIG_CHANGE`           | 配置变更          |
| `SECRET_CHANGE`           | 密钥变更          |
| `SAVEPOINT_CREATE`        | Savepoint 创建    |
| `SAVEPOINT_RESTORE`       | 从 Savepoint 恢复 |
| `DATA_REPAIR`             | 数据修复          |
| `PERMISSION_CHANGE`       | 权限变更          |

审计日志模型示例：

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 作业审计日志
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobAuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private String auditId;

    private String jobName;

    private String action;

    private String operator;

    private String env;

    private String target;

    private String detail;

    private Long operateTime;
}
```

审计日志建议：

1. 发布、停止、回滚、补数必须记录审计日志。
2. 审计日志应进入独立存储，不依赖 TaskManager 普通日志。
3. 审计日志中的敏感字段必须脱敏。
4. 审计日志要包含操作者、时间、环境、对象和操作结果。
5. 对 Secret、权限和生产配置的变更应重点审计。
6. 审计数据保存周期应符合公司合规要求。

### 密钥管理

密钥管理用于管理数据库密码、Kafka JAAS、SSL 证书、对象存储 AK/SK、Webhook、Token、keytab 和应用私钥。密钥管理应覆盖生成、分发、使用、轮换、吊销和审计全生命周期。

常见密钥类型如下：

| 密钥            | 建议管理方式              |
| --------------- | ------------------------- |
| 数据库密码      | Secret / Vault / 配置中心 |
| Kafka JAAS      | Secret 文件或环境变量     |
| SSL keystore    | Secret 文件挂载           |
| SSL truststore  | Secret 文件挂载           |
| 对象存储 AK/SK  | Secret / IAM Role         |
| Kerberos keytab | Secret 文件挂载           |
| Webhook Token   | Secret                    |
| API Token       | Secret / Vault            |

Kubernetes Secret 注入示例：

```yaml
env:
  - name: MYSQL_PASSWORD
    valueFrom:
      secretKeyRef:
        name: user-behavior-secret
        key: mysql-password

  - name: KAFKA_PASSWORD
    valueFrom:
      secretKeyRef:
        name: user-behavior-secret
        key: kafka-password
```

Java 读取环境变量示例：

```java
package io.github.atengk.flink.common.security;

import cn.hutool.core.util.StrUtil;

/**
 * 密钥读取工具
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class SecretEnvUtil {

    /**
     * 从环境变量读取密钥
     *
     * @param envName 环境变量名称
     * @return 密钥值
     */
    public static String requireSecret(String envName) {
        String value = System.getenv(envName);
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException("缺少必需密钥环境变量：" + envName);
        }
        return value;
    }
}
```

密钥管理建议：

1. 密钥不进入代码仓库、Jar、镜像、ConfigMap 和日志。
2. 生产密钥通过 Secret、Vault、云厂商 Secret Manager 或配置中心管理。
3. 密钥必须支持轮换。
4. 密钥变更要有审批和审计。
5. 不同环境使用不同密钥。
6. 密钥读取失败应让作业启动失败，而不是使用默认空密码继续运行。

## 版本升级与兼容

本章节用于说明 Flink 作业在 Flink Runtime、Connector、状态、Savepoint、API、依赖和回滚方面的升级兼容设计。Flink 作业是有状态长期运行程序，升级风险主要来自状态序列化、算子拓扑、Connector 状态、依赖冲突、API 行为变化和 Sink 语义变化。升级前必须先在测试环境使用生产等价 Savepoint 或样本状态进行恢复验证。

Flink Savepoint 文档说明，Savepoint 是通过 Checkpoint 机制创建的一致性作业状态镜像，可用于停止恢复、分叉或更新作业；官方同时强调，为了支持程序和 Flink 版本升级，理解并设置算子 ID 很重要。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/ops/state/savepoints/?utm_source=chatgpt.com))

### Flink 版本升级

Flink 版本升级包括小版本升级、跨大版本升级和运行环境升级。升级时需要同时检查 Java 版本、Scala 版本、Connector 版本、状态兼容、配置项变化、部署脚本和运行时依赖。跨大版本升级尤其要谨慎，因为状态序列化、API、Connector、SQL Planner 和默认配置都可能变化。

升级前检查项如下：

| 检查项        | 内容                                                  |
| ------------- | ----------------------------------------------------- |
| Flink Runtime | 目标集群版本、发行版、镜像版本                        |
| Java 版本     | 编译 JDK 与运行 JDK 是否兼容                          |
| Connector     | Kafka、JDBC、CDC、Doris、StarRocks 等是否支持目标版本 |
| 状态          | State 类型、Serializer、TTL、RocksDB 状态             |
| Savepoint     | 是否能从旧版本 Savepoint 恢复                         |
| API           | 废弃 API、新 Source/Sink API、配置项变化              |
| 部署          | YARN/Kubernetes/Operator 提交方式变化                 |
| 依赖          | Jackson、Guava、Netty、Kryo、Avro、Protobuf 冲突      |

Flink 升级流程：

```text
确认目标版本
  -> 阅读 Release Notes 和 Migration Guide
  -> 升级依赖和插件
  -> 编译通过
  -> 单元测试
  -> 集成测试
  -> 使用旧版本 Savepoint 测试恢复
  -> 数据回放验证
  -> 测试环境长稳运行
  -> 生产发布前 Savepoint
  -> 灰度升级
  -> 验证指标
```

Maven 版本统一示例：

```xml
<properties>
    <!-- Flink 版本必须与目标集群保持一致 -->
    <flink.version>2.2.0</flink.version>

    <!-- Java 版本按目标 Flink 运行环境选择 -->
    <maven.compiler.release>17</maven.compiler.release>
</properties>
```

Flink 版本升级建议：

1. 不要直接在生产从旧版本跳到新版本。
2. 先用测试环境从旧 Savepoint 恢复验证。
3. 升级时保留发布前 Savepoint。
4. 核心作业建议逐个升级，不要批量升级所有作业。
5. SQL 作业要额外验证 Changelog、时间语义和 Connector DDL。
6. 升级后重点观察 Checkpoint、状态大小、Kafka Lag、Sink 写入和业务指标。

### Connector 版本兼容

Connector 版本兼容用于保证 Flink 作业能够正确访问外部系统。Flink 官方 Connector 文档说明，Connector 和 Format 需要让 Flink 访问对应制品；社区通常发布薄 Jar `flink-connector-<NAME>` 和适合 SQL Client 使用的 Uber Jar `flink-sql-connector-<NAME>`，使用方式可以是 Shade 到作业 Jar，也可以放到 Flink 发行版 `/lib` 目录。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/configuration/connector/?utm_source=chatgpt.com))

Connector 兼容检查表：

| Connector     | 检查内容                                     |
| ------------- | -------------------------------------------- |
| Kafka         | Connector 版本、Kafka Broker 版本、事务语义  |
| JDBC          | Connector 版本、数据库驱动版本、SQL 方言     |
| Flink CDC     | Flink CDC 版本、数据库版本、Binlog 格式      |
| Elasticsearch | ES 主版本、Connector 支持版本                |
| Hive          | Hive 版本、Hadoop 版本、Metastore 兼容       |
| HBase         | HBase 版本、Hadoop 版本、Kerberos            |
| Doris         | Doris 版本、Connector 版本、Stream Load 参数 |
| StarRocks     | StarRocks 版本、Connector 版本、事务能力     |
| Iceberg       | Iceberg Runtime、Catalog、Flink 版本         |
| Hudi          | Hudi Bundle、Flink 版本、表类型              |

依赖管理示例：

```xml
<dependencyManagement>
    <dependencies>
        <!-- Kafka Connector 版本应与 Flink 主版本兼容 -->
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-connector-kafka</artifactId>
            <version>${flink.kafka.connector.version}</version>
        </dependency>

        <!-- JDBC Connector 版本应与 Flink 主版本兼容 -->
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-connector-jdbc</artifactId>
            <version>${flink.jdbc.connector.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Connector 版本兼容建议：

1. Connector 版本不要跟随业务随意升级。
2. 升级 Connector 前先阅读 Connector Release Notes。
3. SQL Connector 要检查 Factory 名称、DDL 参数和 Changelog 支持。
4. DataStream Connector 要检查 Source/Sink API 是否变化。
5. Exactly Once、事务、分区发现、认证参数必须重新验证。
6. Connector Jar 进入作业 Fat Jar 还是集群 `/lib` 必须统一规范。

### State 兼容性

State 兼容性用于保证作业升级后能够读取旧版本状态。状态兼容风险主要来自状态类型变化、序列化器变化、Key 类型变化、状态名称变化、算子 `uid()` 变化和 Flink 版本升级。Flink 状态 Schema 演进文档说明，当前状态 Schema 演进主要支持 POJO 和 Avro；POJO 支持新增字段、删除字段，但不支持字段类型变化、类名和命名空间变化；Key 的 Schema 演进不受支持；Kryo 不能用于 Schema 演进。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/fault-tolerance/serialization/schema_evolution/?utm_source=chatgpt.com))

State 变更风险表：

| 变更             | 风险 | 建议                 |
| ---------------- | ---- | -------------------- |
| 新增 POJO 字段   | 低   | 使用默认值，测试恢复 |
| 删除 POJO 字段   | 中   | 确认旧字段不再使用   |
| 修改字段类型     | 高   | 不建议直接修改       |
| 修改包名或类名   | 高   | 不建议修改           |
| 修改状态名称     | 高   | 会导致状态无法匹配   |
| 修改 Key 类型    | 极高 | 不支持直接演进       |
| 修改算子 `uid()` | 高   | 状态可能无法恢复     |
| 使用 Kryo 状态   | 高   | 不适合 Schema 演进   |

状态声明示例：

```java
ValueStateDescriptor<UserBehaviorState> descriptor = new ValueStateDescriptor<>(
        "user-behavior-state",
        UserBehaviorState.class
);

ValueState<UserBehaviorState> state = getRuntimeContext().getState(descriptor);
```

状态模型示例：

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户行为状态模型
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBehaviorState implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;

    private Long lastEventTime;

    private Long eventCount;
}
```

State 兼容性建议：

1. 进入 Flink State 的模型必须稳定。
2. 状态名称进入生产后不要修改。
3. Key 类型和 Key 字段不要修改。
4. POJO 状态可以新增字段，但要验证 Savepoint 恢复。
5. 状态中避免使用复杂泛型、匿名类、第三方对象和 Kryo 回退类型。
6. 重大状态变更应考虑使用 State Processor API 或重建状态。

### Savepoint 兼容性

Savepoint 兼容性用于保证作业能够从旧 Savepoint 正确恢复。Savepoint 恢复依赖算子 ID、状态名称、状态序列化器、并行度、状态后端和作业拓扑。Flink Savepoint 文档说明，Savepoint 由稳定存储上的状态文件和元数据文件组成，可用于停止恢复、分叉或更新作业；为了支持升级，需要理解并分配算子 ID。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/ops/state/savepoints/?utm_source=chatgpt.com))

Savepoint 恢复风险如下：

| 变更              | 风险                       |
| ----------------- | -------------------------- |
| 删除有状态算子    | 旧状态无法匹配             |
| 新增有状态算子    | 新状态为空，需要业务可接受 |
| 调整无状态算子    | 通常风险较低               |
| 重排有状态算子    | 如果 UID 稳定，风险较低    |
| 修改 UID          | 高风险                     |
| 修改并行度        | 通常支持，但需测试         |
| 修改状态类型      | 高风险                     |
| 修改 Flink 大版本 | 高风险                     |

Savepoint 触发命令：

```bash
flink savepoint \
  <jobId> \
  hdfs:///flink/savepoints/prod/user-behavior-job
```

从 Savepoint 恢复命令：

```bash
flink run \
  -s hdfs:///flink/savepoints/prod/user-behavior-job/savepoint-20260511-001 \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  target/flink-job-user-behavior.jar \
  --env prod \
  --jobName user-behavior-job
```

Savepoint 兼容性建议：

1. 所有关键算子必须显式设置 `uid()`。
2. 生产升级必须先创建 Savepoint。
3. 从 Savepoint 恢复前要确认 Jar、Flink 版本、Connector 版本和配置版本。
4. 调整并行度前必须在测试环境验证状态重分布。
5. 删除有状态算子前要评估旧状态如何处理。
6. Savepoint 恢复失败时不要直接删状态，应先分析状态映射和序列化异常。

### API 变更适配

API 变更适配用于处理 Flink 版本升级后 Source、Sink、Table API、SQL、窗口、状态、配置项和部署命令的变化。Flink 版本升级经常伴随 API 废弃、参数改名、默认行为调整或 Connector 独立版本发布。

常见 API 变更类型：

| 类型            | 示例                                           |
| --------------- | ---------------------------------------------- |
| Source API      | 从旧 `FlinkKafkaConsumer` 迁移到 `KafkaSource` |
| Sink API        | 从旧 `addSink` 迁移到新 `sinkTo`               |
| Checkpoint 配置 | 配置项名称变化                                 |
| SQL Connector   | DDL 参数变化                                   |
| Table API       | Planner 行为变化                               |
| 部署命令        | Per-Job 被 Application Mode 替代               |
| 状态 API        | TTL、Serializer 行为变化                       |
| Java 版本       | Java 17 模块访问限制                           |

旧 Kafka Consumer 迁移示例：

```java
// 新 Source API 示例
KafkaSource<String> kafkaSource = KafkaSource.<String>builder()
        .setBootstrapServers("kafka-prod-01:9092,kafka-prod-02:9092")
        .setTopics("user_behavior_log")
        .setGroupId("user-behavior-job")
        .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.LATEST))
        .setValueOnlyDeserializer(new SimpleStringSchema())
        .build();

DataStream<String> sourceStream = env.fromSource(
        kafkaSource,
        WatermarkStrategy.noWatermarks(),
        "kafka-user-behavior-source"
);
```

API 变更适配建议：

1. 编译错误只是第一层，运行行为变化也必须测试。
2. Source/Sink API 迁移后要验证 Checkpoint 和恢复语义。
3. SQL DDL 参数升级后要验证 Factory 是否能加载。
4. 部署命令升级后要验证参数传递和 Jar 分发位置。
5. Java 版本升级后要验证反射、序列化和第三方 SDK。
6. 适配过程要保留旧版本回滚分支或 Tag。

### 依赖升级风险

依赖升级风险包括 Flink Core、Connector、Kafka Client、数据库 Driver、Jackson、Guava、Netty、Kryo、Avro、Protobuf、Hadoop、Hive、HBase 和日志框架的冲突。依赖升级最常见的问题是本地编译成功，但集群运行时报 `NoSuchMethodError`、`ClassNotFoundException`、`NoClassDefFoundError` 或 Connector Factory 找不到。

依赖升级检查命令：

```bash
# 查看完整依赖树
mvn dependency:tree

# 检查 Flink 依赖
mvn dependency:tree -Dincludes=org.apache.flink

# 检查 Kafka 依赖
mvn dependency:tree -Dincludes=org.apache.kafka

# 检查 Jackson 依赖
mvn dependency:tree -Dincludes=com.fasterxml.jackson.core

# 检查 Guava 依赖
mvn dependency:tree -Dincludes=com.google.guava

# 检查日志依赖
mvn dependency:tree -Dincludes=org.slf4j,ch.qos.logback,org.apache.logging.log4j
```

依赖升级风险表：

| 依赖                 | 风险                         |
| -------------------- | ---------------------------- |
| Flink Runtime        | 与集群版本冲突               |
| Kafka Client         | Broker 协议、认证、事务兼容  |
| Jackson              | JSON 解析行为、方法缺失      |
| Guava                | 常见 `NoSuchMethodError`     |
| Netty                | 与 Flink、Kafka、Hadoop 冲突 |
| Kryo                 | 状态序列化兼容风险           |
| Avro                 | Schema 解析和生成类兼容      |
| Protobuf             | 生成类和运行时版本兼容       |
| Hadoop/Hive          | 集群依赖强绑定               |
| Logback/slf4j-simple | 与 Flink Log4j2 冲突         |

依赖升级建议：

1. 一次升级尽量只升级少量关键依赖。
2. Flink Core 依赖版本必须与集群一致。
3. Hadoop、Hive、HBase 依赖优先由集群统一提供。
4. Connector 依赖升级必须做端到端测试。
5. Shade 后检查 `META-INF/services` 是否保留。
6. 依赖升级后必须执行 Savepoint 恢复验证。

### 回滚方案

回滚方案用于在升级失败后恢复到上一稳定版本。Flink 作业回滚必须同时考虑代码版本、运行时版本、Connector 版本、配置版本和状态版本。不能只把 Jar 换回旧版本，因为新版本可能已经改变了状态、写入了下游结果或升级了 Connector 状态。

推荐回滚流程：

```text
发现升级异常
  -> 停止新版本作业
  -> 确认下游是否产生错误数据
  -> 使用旧版本 Jar / 镜像
  -> 使用发布前 Savepoint
  -> 使用旧版本配置
  -> 启动作业
  -> 验证状态、Lag、Checkpoint、Sink 和业务指标
  -> 必要时执行数据补偿
```

YARN 回滚命令示例：

```bash
flink run \
  -t yarn-application \
  -s hdfs:///flink/savepoints/prod/user-behavior-job/savepoint-before-upgrade-20260511 \
  -Djobmanager.memory.process.size=2048m \
  -Dtaskmanager.memory.process.size=4096m \
  -Dtaskmanager.numberOfTaskSlots=4 \
  -Dyarn.application.name=user-behavior-job-rollback \
  -Dyarn.application.queue=realtime \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  hdfs:///flink/jars/user-behavior-job/flink-job-user-behavior-1.0.0.jar \
  --env prod \
  --jobName user-behavior-job \
  --config hdfs:///flink/config/prod/user-behavior/application-1.0.0.yml
```

Kubernetes Operator 回滚配置示例：

```yaml
spec:
  image: registry.example.com/flink/flink-job-user-behavior:1.0.0
  job:
    jarURI: local:///opt/flink/usrlib/flink-job-user-behavior.jar
    entryClass: io.github.atengk.flink.job.userbehavior.UserBehaviorJob
    parallelism: 8
    upgradeMode: savepoint
    initialSavepointPath: s3://flink-prod/savepoints/user-behavior-job/savepoint-before-upgrade-20260511
    savepointRedeployNonce: 3
```

Operator 也提供应用升级回滚能力，但官方文档将其标记为实验性能力，并说明它基于稳定部署规格工作；启用时需要配置 `kubernetes.operator.deployment.rollback.enabled: true`，且回滚只影响 `FlinkDeployment` / `FlinkSession` CRD，本身不能替代完整的 Jar、配置、Secret 和外部资源回滚设计。([Apache Nightlies](https://nightlies.apache.org/flink/flink-kubernetes-operator-docs-main/docs/custom-resource/job-management/?utm_source=chatgpt.com))

回滚方案建议：

1. 发布前 Savepoint 是回滚基础。
2. 回滚必须使用旧 Jar、旧配置、旧 Connector 和发布前 Savepoint。
3. 如果新版本已经写入错误数据，需要先制定下游修复方案。
4. 回滚后必须检查 Kafka Lag、Checkpoint、Sink、业务指标和脏数据。
5. 重大版本升级建议保留旧集群或旧镜像一段时间。
6. 回滚流程要在测试环境演练，而不是生产故障时首次执行。

以下继续展开 `运维管理` 与 `故障排查` 两部分。

## 运维管理

本章节用于说明 Flink 作业上线后的日常运维流程，包括作业启动、停止、重启、Savepoint 创建与恢复、Checkpoint 清理、日志归档和异常处理。Flink 运维的核心原则是：有状态作业优先基于 Savepoint 操作，生产变更必须保留可回滚状态点，任何清理动作都要先确认不会影响恢复。

Flink CLI 提供 `run`、`list`、`savepoint`、`checkpoint`、`cancel`、`stop` 等动作，用于提交作业、查看作业、触发状态快照和终止作业；其中 `stop` 会结合停止与 Savepoint 行为，更适合有状态流作业的优雅停止。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/zh/docs/deployment/cli/?utm_source=chatgpt.com))

### 作业启动

作业启动用于将 Flink Jar 提交到 Standalone、YARN、Kubernetes 或 Session 集群中运行。生产作业启动前必须确认 Jar 版本、配置文件、运行参数、Checkpoint 路径、Savepoint 路径、Source/Sink 权限和资源参数。

启动前检查项如下：

| 检查项          | 说明                                            |
| --------------- | ----------------------------------------------- |
| Jar 版本        | 是否为审批通过的发布版本                        |
| Git Commit      | 是否与制品记录一致                              |
| 主类            | `-c` 指定入口类是否正确                         |
| 配置文件        | 是否为目标环境配置                              |
| 并行度          | 是否与 Kafka 分区数、Slot、Sink 能力匹配        |
| Checkpoint 路径 | 是否存在、可写、环境隔离                        |
| Savepoint 路径  | 是否存在、可写、不会误删                        |
| Source 权限     | Kafka Topic、CDC、文件路径是否可读              |
| Sink 权限       | Kafka、JDBC、Doris、StarRocks、文件路径是否可写 |
| 监控告警        | Prometheus、Grafana、告警规则是否已配置         |

YARN Application Mode 启动作业示例：

```bash
flink run \
  -t yarn-application \
  -Djobmanager.memory.process.size=2048m \
  -Dtaskmanager.memory.process.size=4096m \
  -Dtaskmanager.numberOfTaskSlots=4 \
  -Dparallelism.default=8 \
  -Dyarn.application.name=user-behavior-job \
  -Dyarn.application.queue=realtime \
  -Dexecution.checkpointing.dir=hdfs:///flink/checkpoints/prod/user-behavior-job \
  -Dexecution.checkpointing.savepoint-dir=hdfs:///flink/savepoints/prod/user-behavior-job \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  hdfs:///flink/jars/user-behavior-job/flink-job-user-behavior-1.0.0.jar \
  --env prod \
  --jobName user-behavior-job \
  --config hdfs:///flink/config/prod/user-behavior/application.yml \
  --parallelism 8
```

Kubernetes Operator 启动作业示例：

```bash
kubectl apply -f deploy/kubernetes/configmap-user-behavior.yaml
kubectl apply -f deploy/kubernetes/secret-user-behavior.yaml
kubectl apply -f deploy/kubernetes/user-behavior-flinkdeployment.yaml

kubectl get flinkdeployment user-behavior-job -n flink-prod
kubectl describe flinkdeployment user-behavior-job -n flink-prod
```

命令说明：YARN 模式通过 `flink run -t yarn-application` 启动作业，`-D` 用于传递 Flink/YARN 运行参数，`--env`、`--config`、`--parallelism` 是业务程序参数。Kubernetes Operator 模式通过声明式 YAML 创建或更新 `FlinkDeployment`。

作业启动建议：

1. 生产启动必须使用发布审批通过的 Jar 或镜像。
2. 作业参数必须进入发布记录。
3. 启动后先确认作业状态为 Running。
4. 启动后立即检查 Checkpoint 是否成功。
5. Kafka Source 作业要观察 Lag 是否稳定下降或保持合理水平。
6. Sink 作业要检查写入成功率、失败数和下游负载。

### 作业停止

作业停止用于安全结束运行中的 Flink 作业。对于无状态测试作业，可以直接 `cancel`；对于有状态生产作业，建议使用 `stop --savepointPath`，先生成 Savepoint，再停止作业。Savepoint 是流作业执行状态的一致性镜像，由稳定存储上的二进制状态文件和元数据文件组成，可用于停止恢复、分叉和升级作业。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/ops/state/savepoints/?utm_source=chatgpt.com))

查看作业列表：

```bash
flink list \
  -t yarn-application \
  -Dyarn.application.id=application_1710000000000_0001
```

优雅停止并创建 Savepoint：

```bash
flink stop \
  -t yarn-application \
  -Dyarn.application.id=application_1710000000000_0001 \
  --savepointPath hdfs:///flink/savepoints/prod/user-behavior-job \
  <jobId>
```

直接取消作业：

```bash
flink cancel \
  -t yarn-application \
  -Dyarn.application.id=application_1710000000000_0001 \
  <jobId>
```

Kubernetes Operator 挂起作业并触发 Savepoint：

```bash
kubectl patch flinkdeployment user-behavior-job \
  -n flink-prod \
  --type=merge \
  -p '{"spec":{"job":{"state":"suspended","upgradeMode":"savepoint"}}}'
```

作业停止建议：

1. 生产有状态作业优先使用 `stop --savepointPath`。
2. 停止前确认最近 Checkpoint 正常。
3. 停止后记录 Savepoint 路径、Job ID、Application ID、停止人和停止原因。
4. 不建议直接通过 `yarn application -kill` 或 `kubectl delete pod` 停止正常作业。
5. 停止后检查下游 Sink 是否还存在未提交事务或缓冲数据。
6. 停止后的 Savepoint 不要自动清理，直到确认不再需要回滚。

### 作业重启

作业重启用于恢复异常作业、加载新配置、切换版本或从 Savepoint 恢复。重启方式分为无状态重启、从最近 Checkpoint 自动恢复、从指定 Savepoint 手动恢复。生产重启前必须明确是否保留状态。

常见重启方式如下：

| 重启方式           | 适用场景                       | 风险                         |
| ------------------ | ------------------------------ | ---------------------------- |
| 自动重启           | Task 失败、外部系统短暂异常    | 依赖重启策略                 |
| 无状态重启         | 本地测试、临时作业、状态可丢弃 | 可能重复消费或丢失状态       |
| 从 Checkpoint 恢复 | 故障自动恢复                   | 依赖 Checkpoint 可用         |
| 从 Savepoint 恢复  | 升级、回滚、并行度调整         | 依赖状态兼容                 |
| 删除后重新提交     | 无状态或可回放作业             | 需要明确 Offset 和 Sink 幂等 |

从 Savepoint 重启 YARN 作业：

```bash
flink run \
  -t yarn-application \
  -s hdfs:///flink/savepoints/prod/user-behavior-job/savepoint-20260511-001 \
  -Djobmanager.memory.process.size=2048m \
  -Dtaskmanager.memory.process.size=4096m \
  -Dtaskmanager.numberOfTaskSlots=4 \
  -Dparallelism.default=8 \
  -Dyarn.application.name=user-behavior-job \
  -Dyarn.application.queue=realtime \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  hdfs:///flink/jars/user-behavior-job/flink-job-user-behavior-1.0.1.jar \
  --env prod \
  --jobName user-behavior-job \
  --config hdfs:///flink/config/prod/user-behavior/application.yml
```

Kubernetes Operator 恢复运行：

```bash
kubectl patch flinkdeployment user-behavior-job \
  -n flink-prod \
  --type=merge \
  -p '{"spec":{"job":{"state":"running","upgradeMode":"savepoint"}}}'
```

作业重启建议：

1. 先判断是否需要状态恢复。
2. 生产作业优先从 Savepoint 或最近可用状态恢复。
3. 重启前检查 Source 数据保留时间，例如 Kafka Topic 保留时间、CDC Binlog 保留时间。
4. 重启后检查 Kafka Lag、Checkpoint、Sink 写入和业务指标。
5. 如果重启原因是代码异常，不要只依赖自动重启掩盖问题。
6. 如果重启后数据重复，优先检查 Sink 幂等键、Kafka 位点和 Savepoint 路径。

### Savepoint 创建

Savepoint 创建用于在升级、停止、回滚、迁移和并行度调整前保存作业一致性状态。Savepoint 不是普通备份文件，它与作业拓扑、算子 UID、状态名称、状态序列化器和 Flink 版本相关。Flink CLI 支持通过 `savepoint` 动作创建 Savepoint；如果未在配置中指定默认 Savepoint 目录，需要在命令中显式指定目录。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/zh/docs/deployment/cli/?utm_source=chatgpt.com))

创建 Savepoint：

```bash
flink savepoint \
  -t yarn-application \
  -Dyarn.application.id=application_1710000000000_0001 \
  <jobId> \
  hdfs:///flink/savepoints/prod/user-behavior-job
```

Kubernetes Operator 触发 Savepoint 的常见方式是通过挂起或升级策略触发：

```bash
kubectl patch flinkdeployment user-behavior-job \
  -n flink-prod \
  --type=merge \
  -p '{"spec":{"job":{"state":"suspended","upgradeMode":"savepoint"}}}'
```

Savepoint 命名建议：

```text
hdfs:///flink/savepoints/prod/user-behavior-job/
├── savepoint-before-release-1.0.1/
├── savepoint-before-parallelism-change-20260511/
└── savepoint-before-rollback-20260511/
```

Savepoint 创建建议：

1. 每次生产升级前必须创建 Savepoint。
2. 并行度调整前必须创建 Savepoint。
3. Savepoint 路径必须记录到发布单。
4. Savepoint 创建成功后，再进行停止、升级或回滚。
5. Savepoint 目录与 Checkpoint 目录分开。
6. Savepoint 文件不要由自动清理任务直接删除。

### Savepoint 恢复

Savepoint 恢复用于从指定状态点启动作业。常见场景包括版本升级、版本回滚、作业迁移、并行度调整、环境迁移和补偿任务分叉。恢复前需要确认新旧作业的算子 UID、状态名称、状态类型和 Key 类型兼容。

从 Savepoint 恢复作业：

```bash
flink run \
  -s hdfs:///flink/savepoints/prod/user-behavior-job/savepoint-before-release-1.0.1 \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  hdfs:///flink/jars/user-behavior-job/flink-job-user-behavior-1.0.1.jar \
  --env prod \
  --jobName user-behavior-job \
  --config hdfs:///flink/config/prod/user-behavior/application.yml
```

允许跳过无法恢复的状态：

```bash
flink run \
  -s hdfs:///flink/savepoints/prod/user-behavior-job/savepoint-before-release-1.0.1 \
  --allowNonRestoredState \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  hdfs:///flink/jars/user-behavior-job/flink-job-user-behavior-1.0.1.jar \
  --env prod \
  --jobName user-behavior-job
```

`--allowNonRestoredState` 只应在明确删除某些有状态算子且业务允许丢弃这些状态时使用。默认情况下，如果 Savepoint 中存在无法映射到新作业拓扑的状态，恢复应失败，这可以防止状态被静默丢弃。

Savepoint 恢复建议：

1. 恢复前确认作业 Jar 和 Savepoint 属于同一业务链路。
2. 核心算子必须设置稳定 `uid()`。
3. 不要随意修改 Key 类型、状态名称和状态模型。
4. 使用 `--allowNonRestoredState` 必须经过评审。
5. 恢复后检查状态是否生效，例如去重、窗口、定时器、CDC 位点。
6. 恢复后必须观察 Checkpoint 是否继续成功。

### Checkpoint 清理

Checkpoint 清理用于释放历史状态快照占用的存储空间。Checkpoint 是 Flink 自动容错机制的一部分，不能像普通临时文件一样随意删除。生产环境应通过 Flink 配置控制保留数量，并通过定期巡检处理孤儿目录或废弃作业目录。

Checkpoint 常见目录结构：

```text
hdfs:///flink/checkpoints/prod/
└── user-behavior-job/
    ├── chk-1024/
    ├── chk-1025/
    └── shared/
```

配置保留外部化 Checkpoint：

```yaml
# 取消作业后保留外部化 Checkpoint，便于异常恢复
execution.checkpointing.externalized-checkpoint-retention: RETAIN_ON_CANCELLATION

# 保留最近完成的 Checkpoint 数量
execution.checkpointing.num-retained: 3
```

清理前检查命令：

```bash
# 查看 Checkpoint 目录大小
hdfs dfs -du -h hdfs:///flink/checkpoints/prod/user-behavior-job

# 查看目录修改时间
hdfs dfs -ls hdfs:///flink/checkpoints/prod/user-behavior-job

# 查看当前运行作业，确认目录是否仍在使用
flink list -a
```

清理废弃 Checkpoint 示例：

```bash
# 确认作业已下线且不再依赖该目录后再删除
hdfs dfs -rm -r hdfs:///flink/checkpoints/prod/deprecated-user-behavior-job
```

Checkpoint 清理建议：

1. 正在运行作业的 Checkpoint 目录禁止手工删除。
2. Checkpoint 和 Savepoint 分开清理。
3. 删除前确认作业已经下线、无恢复需求、无发布回滚依赖。
4. 推荐通过保留数量和生命周期策略控制存储增长。
5. 对象存储场景要设置生命周期规则，但排除 Savepoint 目录。
6. 清理操作必须记录审计日志。

### 日志归档

日志归档用于保存 JobManager、TaskManager、YARN Container、Kubernetes Pod、业务审计和错误样本。Flink 进程会生成日志文件，日志可通过 JobManager/TaskManager 的 Web UI 页面访问，也可由 YARN、Kubernetes 或日志采集系统提供额外访问方式；Flink 使用 SLF4J，默认底层日志框架是 Log4j 2。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/deployment/advanced/logging/?utm_source=chatgpt.com))

日志归档范围如下：

| 日志类型         | 来源                          | 用途                                     |
| ---------------- | ----------------------------- | ---------------------------------------- |
| JobManager 日志  | Flink JM                      | 调度、提交、Checkpoint Coordinator、异常 |
| TaskManager 日志 | Flink TM                      | 算子异常、Sink 失败、序列化异常          |
| YARN 日志        | ResourceManager / NodeManager | Container 启动和退出原因                 |
| Kubernetes 日志  | Pod stdout/stderr             | 容器运行和重启原因                       |
| 业务审计日志     | 自定义审计表或 Topic          | 发布、回滚、补数、配置变更               |
| 脏数据样本       | Kafka/File/Table              | 数据修复和上游定位                       |
| GC 日志          | JVM                           | OOM、Full GC、延迟抖动分析               |

YARN 日志归档命令：

```bash
# 保存 YARN 应用日志到归档目录
yarn logs -applicationId application_1710000000000_0001 \
  > /data/flink/log-archive/user-behavior-job/application_1710000000000_0001.log

# 压缩归档
gzip /data/flink/log-archive/user-behavior-job/application_1710000000000_0001.log
```

Kubernetes 日志归档命令：

```bash
# 归档 JobManager 日志
kubectl logs -n flink-prod deploy/user-behavior-job \
  > /data/flink/log-archive/user-behavior-job/jobmanager-$(date +%Y%m%d%H%M%S).log

# 归档指定 TaskManager Pod 日志
kubectl logs -n flink-prod pod/user-behavior-job-taskmanager-0 \
  > /data/flink/log-archive/user-behavior-job/taskmanager-0-$(date +%Y%m%d%H%M%S).log
```

日志归档建议：

1. 生产作业日志应接入统一日志系统。
2. 异常发布、回滚、补数和故障必须归档相关日志。
3. 日志归档要保留作业名、环境、版本、Application ID、Pod 名称和时间。
4. 日志中的密码、Token、手机号、身份证等敏感字段必须脱敏。
5. GC 日志和 HeapDump 需要单独存储和权限控制。
6. 日志保留周期应符合公司审计和合规要求。

### 异常处理流程

异常处理流程用于规范生产作业出现故障时的响应、定位、止血、恢复和复盘。Flink 异常通常涉及多个系统，不能只看 Flink 日志；需要同时查看 Source、Flink、状态存储、网络、Sink、监控和业务结果。

推荐异常处理流程：

```text
发现告警
  -> 判断影响范围
  -> 确认作业状态
  -> 查看最近异常日志
  -> 查看 Checkpoint 和反压
  -> 判断 Source / Flink / Sink / 外部系统
  -> 采取止血措施
  -> 恢复作业
  -> 验证数据
  -> 记录故障和复盘
```

异常分级建议：

| 等级 | 场景                                                  | 响应                     |
| ---- | ----------------------------------------------------- | ------------------------ |
| P0   | 核心链路停止、数据大量丢失、Sink 大规模失败           | 立即响应，必要时回滚     |
| P1   | Checkpoint 持续失败、Kafka Lag 快速上涨、业务指标异常 | 快速定位，准备降级或扩容 |
| P2   | 局部脏数据升高、维表命中率下降、部分 Sink 超时        | 排查并修复               |
| P3   | 单次重启、短暂外部抖动、非核心指标波动                | 观察和记录               |

异常处理建议：

1. 先止血，再深入优化。
2. 不要在故障中直接清理 Checkpoint 或 Savepoint。
3. 如果 Sink 可能重复写入，先暂停写入链路，再评估恢复策略。
4. 如果 Source 数据保留时间不足，优先延长保留或停止上游清理。
5. 重大故障必须保留日志、指标截图、Savepoint 路径和修复记录。
6. 故障后必须复盘，包括根因、影响、处理过程、补偿结果和预防措施。

## 故障排查

本章节用于说明 Flink 作业常见故障的定位方法，包括启动失败、依赖冲突、Kafka 消费异常、Checkpoint 失败、反压、OOM、数据延迟、数据重复、数据丢失和 Sink 写入失败。排查时应先确定故障发生在哪一层：提交层、资源层、Source 层、Flink 计算层、状态层、Sink 层或业务数据层。

通用排查路径如下：

```text
确认作业状态
  -> 查看 Flink Web UI Exceptions
  -> 查看 JobManager 日志
  -> 查看 TaskManager 日志
  -> 查看 Checkpoint 页面
  -> 查看 Back Pressure 页面
  -> 查看 Source Lag 和 Sink 指标
  -> 查看业务指标和脏数据
  -> 定位具体算子和 Subtask
```

### 作业启动失败

作业启动失败通常发生在提交阶段、JobManager 初始化阶段或 TaskManager 启动阶段。常见原因包括主类错误、Jar 缺失、依赖冲突、配置文件缺失、权限不足、资源不足、Connector 找不到、Kerberos 认证失败和 Checkpoint 路径不可写。

常见现象如下：

| 现象                                | 可能原因                                      |
| ----------------------------------- | --------------------------------------------- |
| `Could not find or load main class` | 主类错误或 Jar 不包含入口类                   |
| `ClassNotFoundException`            | 依赖未打包或集群 lib 缺失                     |
| `Factory not found`                 | SQL Connector SPI 未合并或 Connector Jar 缺失 |
| Application 一直 ACCEPTED           | YARN 队列资源不足                             |
| Pod 一直 Pending                    | Kubernetes 资源不足或调度限制                 |
| Pod CrashLoopBackOff                | 启动参数、依赖、权限或配置错误                |
| Checkpoint 路径报错                 | HDFS/S3/OSS 权限或路径错误                    |
| Kerberos LoginException             | keytab、principal、krb5 配置错误              |

排查命令：

```bash
# 查看 Jar 是否包含入口类
jar tf flink-job-user-behavior.jar | grep UserBehaviorJob

# 查看 Jar 是否包含 SPI 文件
jar tf flink-job-user-behavior.jar | grep META-INF/services

# 查看 YARN 应用状态
yarn application -status application_1710000000000_0001

# 查看 YARN 日志
yarn logs -applicationId application_1710000000000_0001 | grep -E "ERROR|Exception|Caused by"

# 查看 Kubernetes Pod 状态
kubectl get pods -n flink-prod

# 查看 Kubernetes 事件
kubectl describe pod <pod-name> -n flink-prod

# 查看 Operator 日志
kubectl logs -n flink-system deploy/flink-kubernetes-operator
```

作业启动失败处理建议：

1. 先确认 Jar、主类、配置路径是否正确。
2. 再检查资源平台状态，例如 YARN 队列、Kubernetes Pod 调度。
3. Connector 找不到时检查 Fat Jar、`META-INF/services` 和 `$FLINK_HOME/lib`。
4. 权限错误优先检查 HDFS/S3/Kafka/数据库账号。
5. Kerberos 环境优先检查 keytab、principal、krb5.conf 和时钟同步。
6. 修复后重新提交前，保留失败日志用于复盘。

### 依赖冲突

依赖冲突是 Flink 作业最常见的问题之一，通常表现为 `ClassNotFoundException`、`NoClassDefFoundError`、`NoSuchMethodError`、`ClassCastException`、`X cannot be cast to X`、Connector Factory 找不到等。Flink Classloading 文档说明，Flink 运行时涉及 Java Classpath、插件组件和动态用户代码；依赖冲突可以通过 Maven Shade Plugin 对用户依赖进行 relocation 来规避。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-release-2.1/docs/ops/debugging/debugging_classloading/?utm_source=chatgpt.com))

常见冲突来源：

| 依赖                 | 问题                           |
| -------------------- | ------------------------------ |
| Flink Runtime        | 与集群自带版本冲突             |
| Guava                | `NoSuchMethodError`            |
| Jackson              | JSON 解析或方法缺失            |
| Netty                | Kafka/Hadoop/Flink 冲突        |
| Kafka Client         | Broker 或 Connector 版本不兼容 |
| Hadoop/Hive          | 集群版本强绑定                 |
| Logback/slf4j-simple | 多日志绑定冲突                 |
| Connector            | SQL Factory 找不到或版本不匹配 |

依赖排查命令：

```bash
# 查看完整依赖树
mvn dependency:tree

# 查看 Flink 依赖
mvn dependency:tree -Dincludes=org.apache.flink

# 查看日志依赖
mvn dependency:tree -Dincludes=org.slf4j,ch.qos.logback,org.apache.logging.log4j

# 查看 Jackson 依赖
mvn dependency:tree -Dincludes=com.fasterxml.jackson.core

# 查看 Guava 依赖
mvn dependency:tree -Dincludes=com.google.guava

# 查看 Kafka 依赖
mvn dependency:tree -Dincludes=org.apache.kafka
```

Shade relocation 示例：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>${maven.shade.plugin.version}</version>
    <configuration>
        <relocations>
            <!-- 将第三方 SDK 中的 Guava 重定位，避免与 Flink 或集群依赖冲突 -->
            <relocation>
                <pattern>com.google.common</pattern>
                <shadedPattern>io.github.atengk.shaded.com.google.common</shadedPattern>
            </relocation>
        </relocations>
        <transformers>
            <!-- 合并 SPI 文件，避免 Connector Factory 无法发现 -->
            <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
        </transformers>
    </configuration>
</plugin>
```

依赖冲突处理建议：

1. Flink 核心依赖通常使用 `provided`。
2. Connector 依赖要与 Flink 版本兼容。
3. Hadoop、Hive、HBase 依赖优先由集群统一提供。
4. 第三方 SDK 冲突时可以使用 Shade relocation。
5. 日志实现只能保留一种，避免多个 SLF4J Binding。
6. 每次依赖变更后都要做测试集群提交验证。

### Kafka 消费异常

Kafka 消费异常通常表现为消费不到数据、Lag 持续上涨、认证失败、分区分配异常、反序列化失败、Offset 恢复异常或 Source 反压。排查时要同时查看 Kafka Topic、Consumer Group、Flink Source 指标、反压和下游 Sink。

常见问题如下：

| 现象           | 可能原因                                |
| -------------- | --------------------------------------- |
| 无数据消费     | Topic 错误、Group 错误、Offset 起点错误 |
| Lag 持续上涨   | 下游反压、并行度不足、Sink 慢           |
| 认证失败       | SASL/SSL/Kerberos 配置错误              |
| 部分分区无消费 | Source 并行度、分区分配或权限问题       |
| 反序列化失败   | Schema 不兼容、消息格式错误             |
| 重启后重复消费 | Checkpoint 未成功或 Sink 不幂等         |
| 重启后跳过数据 | Offset 起始策略误配置                   |

Kafka 排查命令：

```bash
# 查看 Topic 分区
kafka-topics.sh \
  --bootstrap-server kafka-prod-01:9092 \
  --describe \
  --topic user_behavior_log

# 查看消费组 Lag
kafka-consumer-groups.sh \
  --bootstrap-server kafka-prod-01:9092 \
  --describe \
  --group user-behavior-job

# 从 Topic 抽样消费
kafka-console-consumer.sh \
  --bootstrap-server kafka-prod-01:9092 \
  --topic user_behavior_log \
  --from-beginning \
  --max-messages 10
```

Kafka 消费异常处理建议：

1. Lag 上涨时不要只调 Source 并行度，先看下游是否反压。
2. Source 并行度通常不应超过 Topic 分区数太多。
3. 反序列化失败要进入脏数据链路，而不是导致作业无限重启。
4. Kafka 认证错误要检查 JAAS、SSL 证书、ACL、Topic 和 Group 权限。
5. 恢复时不要随意重置消费组 Offset，优先依赖 Checkpoint 或 Savepoint。
6. 如果 Topic 保留时间过短，作业长时间停机后可能无法从旧位点恢复。

### Checkpoint 失败

Checkpoint 失败会影响 Flink 作业的容错能力。常见原因包括状态过大、Checkpoint 存储慢、HDFS/S3/OSS 权限不足、Sink 提交阻塞、反压严重、Barrier 对齐耗时长、RocksDB 磁盘慢、网络异常和 Task 频繁失败。Flink 大状态调优文档指出，当 Checkpoint 耗时经常超过间隔时，系统可能持续处于 Checkpoint 压力下；对大状态作业，设置最小 Checkpoint 间隔和使用增量 Checkpoint 是常见优化方向。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/ops/state/large_state_tuning/?utm_source=chatgpt.com))

常见 Checkpoint 指标：

| 指标                | 说明             |
| ------------------- | ---------------- |
| Checkpoint Duration | 总耗时           |
| Checkpoint Size     | 状态大小         |
| Alignment Time      | Barrier 对齐耗时 |
| Start Delay         | Barrier 到达延迟 |
| Sync Duration       | 同步阶段耗时     |
| Async Duration      | 异步上传阶段耗时 |
| Failed Checkpoints  | 失败次数         |
| End-to-End Duration | 端到端耗时       |

Checkpoint 配置优化示例：

```yaml
# Checkpoint 间隔
execution.checkpointing.interval: 60s

# Checkpoint 超时时间
execution.checkpointing.timeout: 10min

# 避免 Checkpoint 完成后立即触发下一次
execution.checkpointing.min-pause: 30s

# 大状态作业通常设置为 1
execution.checkpointing.max-concurrent-checkpoints: 1

# RocksDB 大状态作业建议开启增量 Checkpoint
execution.checkpointing.incremental: true

# 反压严重场景可评估非对齐 Checkpoint
execution.checkpointing.unaligned: true
```

Checkpoint 失败处理建议：

1. 先看 Flink Web UI 的 Checkpoints 页面。
2. 如果 `Start Delay` 或 `Alignment Time` 高，优先排查反压。
3. 如果 `Async Duration` 高，优先排查远程存储和状态大小。
4. 如果状态持续增大，检查状态 TTL、窗口大小和去重状态。
5. 如果 Sink 参与 Checkpoint 提交，检查 Sink 批量写入和事务提交耗时。
6. 不要通过无限增大超时时间掩盖根因。

### 反压问题

反压表示下游消费速度低于上游生产速度，压力会沿数据流反向传播。Flink 在严重反压下，Checkpoint Barrier 的传递也会变慢，导致 Checkpoint 的 Start Delay 和 Alignment Time 上升；官方文档建议通过消除反压源、减少缓冲中的飞行数据或启用非对齐 Checkpoint 来处理这类问题。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/zh/docs/ops/state/checkpointing_under_backpressure/?utm_source=chatgpt.com))

反压排查路径：

```text
Web UI 发现反压
  -> 找到最下游高 Busy 算子
  -> 查看 Sink 耗时和失败
  -> 查看状态读写耗时
  -> 查看数据倾斜
  -> 查看外部系统延迟
  -> 调整并行度、资源或下游能力
```

常见反压原因：

| 原因                | 表现                        | 处理                                 |
| ------------------- | --------------------------- | ------------------------------------ |
| Sink 慢             | Sink Busy 高、Source 也反压 | 优化批量、提高 Sink 并行度、扩容下游 |
| 数据倾斜            | 部分 Subtask 忙             | 热点 Key 拆分、二阶段聚合            |
| 状态过大            | RocksDB 慢、Checkpoint 慢   | 优化状态结构、TTL、Managed Memory    |
| 外部 IO 慢          | 维表查询慢                  | 异步 IO、缓存、限流                  |
| CPU 不足            | Busy 高、CPU 高             | 增加并行度或 CPU                     |
| Network Memory 不足 | Shuffle 压力高              | 调整网络内存                         |
| Checkpoint 压力     | 周期性延迟                  | 调整间隔、增量 Checkpoint            |

反压处理建议：

1. 不要只看 Source 反压，瓶颈通常在下游。
2. 先找最下游高 Busy 或高反压的算子。
3. 单个 Subtask 反压通常是数据倾斜。
4. 全部 Subtask 反压通常是下游能力不足或资源不足。
5. 反压导致 Checkpoint 失败时，可评估 Buffer Debloating 或非对齐 Checkpoint。
6. 每次优化后要对比 Records、Busy、Backpressure、Checkpoint 和业务延迟。

### OOM 问题

OOM 表示 JVM 或容器内存不足。Flink 内存由 Total Process Memory、Total Flink Memory、JVM Heap、Off-Heap、Managed Memory、Network Memory、JVM Metaspace 和 JVM Overhead 等组成；官方文档建议优先通过 `taskmanager.memory.process.size` 和 `jobmanager.memory.process.size` 配置总进程内存，再按组件细调。([Apache Nightlies](https://nightlies.apache.org/flink/flink-docs-stable/docs/deployment/memory/mem_setup/?utm_source=chatgpt.com))

常见 OOM 类型：

| 类型                | 可能原因                                  |
| ------------------- | ----------------------------------------- |
| Java Heap OOM       | 堆内状态、大对象、缓存无界、JSON 对象过多 |
| Direct Memory OOM   | 网络缓冲、堆外内存、外部客户端            |
| Metaspace OOM       | 类加载过多、依赖冲突、动态加载泄漏        |
| Container OOMKilled | 容器 limit 小于实际进程内存               |
| RocksDB Native OOM  | Managed Memory 不足或 RocksDB 配置不合理  |
| GC Overhead Limit   | GC 频繁且回收效果差                       |

JVM 诊断配置示例：

```yaml
env.java.opts.taskmanager: >-
  -Dfile.encoding=UTF-8
  -XX:+UseG1GC
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=/data/flink/heapdump
  -XX:+ExitOnOutOfMemoryError
```

OOM 排查命令：

```bash
# Kubernetes 查看 OOMKilled
kubectl describe pod <taskmanager-pod> -n flink-prod | grep -i oom -A 5

# 查看容器日志
kubectl logs <taskmanager-pod> -n flink-prod

# YARN 查看 OOM 日志
yarn logs -applicationId application_1710000000000_0001 | grep -i "OutOfMemory\|GC overhead\|Killed"

# 查看 HeapDump 文件
ls -lh /data/flink/heapdump
```

OOM 处理建议：

1. 先确认是 JVM OOM 还是容器 OOMKilled。
2. 堆内 OOM 检查缓存、状态对象、窗口数据和大对象日志。
3. RocksDB 大状态作业增加 Managed Memory，并检查本地磁盘。
4. 容器 OOMKilled 检查 `taskmanager.memory.process.size` 与容器 limit 是否匹配。
5. 缓存必须设置最大容量和 TTL。
6. 不要只调大内存，必须定位状态增长或对象增长来源。

### 数据延迟

数据延迟表示事件从 Source 到 Sink 的端到端处理时间超过预期。延迟可能来自 Kafka 积压、Source 读取慢、反压、窗口等待、Watermark 停滞、外部维表慢、Sink 慢、Checkpoint 压力或资源不足。

延迟来源排查表：

| 来源            | 现象                 | 排查                       |
| --------------- | -------------------- | -------------------------- |
| Kafka 积压      | Lag 增长             | Consumer Group Lag         |
| Source 慢       | Source Records In 低 | Kafka 分区、认证、反序列化 |
| 反压            | Backpressure High    | Web UI Back Pressure       |
| Watermark 停滞  | 窗口不触发           | currentInputWatermark      |
| 维表查询慢      | Async IO 超时        | 外部系统 QPS、缓存命中率   |
| Sink 慢         | Sink Busy 高         | 下游写入耗时               |
| Checkpoint 压力 | 周期性延迟           | Checkpoint Duration        |
| 数据倾斜        | 部分 Subtask 延迟高  | Subtask Records/Busy       |

Watermark 空闲分区处理示例：

```java
WatermarkStrategy<UserBehaviorEvent> watermarkStrategy = WatermarkStrategy
        .<UserBehaviorEvent>forBoundedOutOfOrderness(Duration.ofSeconds(10))
        .withTimestampAssigner((event, timestamp) -> event.getEventTime())
        .withIdleness(Duration.ofMinutes(1));
```

数据延迟处理建议：

1. 先区分 Kafka Lag 延迟和 Flink 内部处理延迟。
2. 如果窗口迟迟不输出，检查 Watermark 是否推进。
3. 如果只某几个 Subtask 延迟高，优先排查数据倾斜。
4. 如果延迟与 Checkpoint 周期一致，排查状态和 Sink 提交。
5. 外部维表慢时优先使用异步 IO、缓存和限流。
6. 端到端延迟应作为业务指标持续监控。

### 数据重复

数据重复可能来自 Source 重放、Checkpoint 恢复、Sink 不幂等、上游重复发送、CDC 重复变更、窗口迟到数据再次输出或补数重复写入。Flink 内部 Exactly Once 不等于端到端没有重复，最终结果还取决于 Sink 事务或幂等设计。

常见重复来源：

| 来源               | 示例                             |
| ------------------ | -------------------------------- |
| 上游重复           | 业务系统重复发送相同 eventId     |
| Kafka 重放         | 从旧 Offset 或 Savepoint 恢复    |
| Checkpoint 恢复    | 最近成功 Checkpoint 后的数据重放 |
| Sink 不幂等        | 普通 Insert 重复写入             |
| Window late firing | 迟到数据导致窗口再次输出         |
| 补数重复           | 补数范围和实时数据重叠           |
| CDC 重复           | Update/Delete 处理不正确         |

重复排查 SQL 示例：

```sql
-- 检查明细表 event_id 是否重复
SELECT
    event_id,
    COUNT(*) AS cnt
FROM user_behavior_detail
WHERE dt = '2026-05-11'
GROUP BY event_id
HAVING COUNT(*) > 1;
```

窗口指标重复排查示例：

```sql
-- 检查窗口指标幂等键是否重复
SELECT
    metric_name,
    dimension_key,
    window_start,
    window_end,
    COUNT(*) AS cnt
FROM user_event_count
WHERE dt = '2026-05-11'
GROUP BY metric_name, dimension_key, window_start, window_end
HAVING COUNT(*) > 1;
```

数据重复处理建议：

1. 明细数据必须有 eventId 或业务唯一键。
2. Sink 端优先使用 Upsert、主键覆盖或事务写入。
3. 窗口结果幂等键必须包含窗口开始和结束时间。
4. 补数作业与实时作业输出要隔离，确认后再合并。
5. late firing 场景下游必须支持更新或覆盖。
6. 重复数据修复要先确认重复范围和重复来源。

### 数据丢失

数据丢失通常比数据重复更严重。常见原因包括 Source 不可重放、Kafka Topic 过期、CDC Binlog 被清理、Checkpoint 长期失败、Sink 静默丢弃、异常数据未落盘、过滤逻辑错误、补数范围错误和手工重置 Offset。

常见丢失来源：

| 来源             | 示例                        |
| ---------------- | --------------------------- |
| Source 不可重放  | Socket、HTTP 推送无重放机制 |
| Kafka 数据过期   | 作业停机超过 Topic 保留时间 |
| CDC Binlog 清理  | Binlog 位点不可恢复         |
| Checkpoint 失败  | 故障后恢复点过旧或不可用    |
| Sink 静默失败    | catch 异常后不抛出、不落盘  |
| 过滤条件错误     | 误过滤合法数据              |
| 脏数据未落盘     | 解析失败后只打印日志        |
| 手工 Offset 错误 | reset 到错误位置            |

输入输出对账示例：

```sql
-- 输入、清洗输出、脏数据数量对账
SELECT
    stat_minute,
    input_count,
    clean_count,
    dirty_count,
    input_count - clean_count - dirty_count AS diff_count
FROM realtime_input_output_check
WHERE dt = '2026-05-11'
  AND input_count <> clean_count + dirty_count;
```

数据丢失处理建议：

1. 所有脏数据必须落盘。
2. Sink 写入异常不能静默吞掉。
3. Source 数据保留时间必须覆盖最长停机恢复时间。
4. Kafka Offset 不要手工重置，除非经过审批和备份。
5. 对核心链路做输入、输出、脏数据三方对账。
6. 如果已发生丢失，优先从原始 Kafka、脏数据表、历史文件或 CDC 快照补偿。

### Sink 写入失败

Sink 写入失败是生产 Flink 作业最常见的故障之一。原因可能是下游不可用、认证失败、网络超时、批量过大、主键冲突、字段类型不匹配、事务超时、限流、磁盘满、索引冲突、连接池耗尽或下游 Schema 变更。

常见 Sink 失败类型：

| Sink                 | 常见问题                                       |
| -------------------- | ---------------------------------------------- |
| Kafka Sink           | Topic 不存在、ACL、事务超时、Broker 不可用     |
| JDBC Sink            | 连接失败、慢 SQL、死锁、主键冲突、字段类型错误 |
| Redis Sink           | 连接超时、认证失败、集群重定向、内存满         |
| Elasticsearch Sink   | Bulk 失败、Mapping 冲突、索引只读、拒绝写入    |
| Doris/StarRocks Sink | Stream Load 失败、Label 冲突、字段映射错误     |
| File Sink            | 路径权限、对象存储异常、小文件过多             |
| HTTP Sink            | 超时、限流、非 2xx 状态、接口幂等失败          |

Sink 异常处理示例：

```java
try {
    // 写入外部系统
    writeToSink(value);
} catch (IllegalArgumentException e) {
    log.warn("Sink数据格式异常，业务Key：{}，原因：{}", value.getBizKey(), e.getMessage());
    outputDirty(value, "SINK_FORMAT_ERROR", e.getMessage());
} catch (Exception e) {
    log.error("Sink写入失败，业务Key：{}，原因：{}", value.getBizKey(), e.getMessage(), e);
    throw e;
}
```

Sink 写入失败处理建议：

1. 数据格式错误进入脏数据链路。
2. 外部系统异常应抛出异常，让 Flink 基于 Checkpoint 恢复。
3. 不要在 Sink 中无限重试。
4. 批量参数要结合下游能力压测。
5. Sink 失败后要确认是否产生部分成功写入。
6. 强一致结果优先使用事务 Sink 或幂等写入。

Sink 写入排查流程：

```text
确认 Sink 类型
  -> 查看异常信息
  -> 判断数据错误还是系统错误
  -> 检查下游可用性和权限
  -> 检查批量参数和超时
  -> 检查幂等键和主键冲突
  -> 恢复作业或执行补偿
```

生产处理建议：

1. Kafka Sink 先检查 Topic、ACL、事务 ID 和 Broker 状态。
2. JDBC Sink 先检查连接数、慢 SQL、死锁和主键冲突。
3. Elasticsearch Sink 先检查 Mapping、Bulk 响应和索引状态。
4. Doris/StarRocks Sink 先检查 Stream Load 返回、Label 和错误行。
5. 文件 Sink 先检查路径权限、对象存储可用性和小文件数量。
6. HTTP Sink 先检查接口状态码、超时、限流和幂等键。

以下继续展开 `代码规范` 与 `项目实战案例` 两部分。

## 代码规范

本章节用于统一 Flink Java 项目的包结构、类命名、方法命名、配置命名、日志、异常、注释和工具类规范。Flink 作业通常包含 Source、Transformation、State、Window、Sink、Connector、配置、模型和部署脚本，如果缺少统一规范，后续排查问题、版本升级、状态恢复和多人协作都会变得困难。

代码规范的目标不是追求形式统一，而是保证作业具备以下特征：

```text
结构清晰
  -> 命名稳定
  -> 配置可控
  -> 日志可查
  -> 异常可分类
  -> 状态可恢复
  -> 代码可测试
  -> 运维可定位
```

### 包结构规范

包结构应按工程职责划分，而不是按个人习惯随意创建。推荐使用多模块结构，将模型、公共工具、连接器封装和具体作业分离。

推荐工程结构：

```text
flink-java-demo/
├── flink-model/
│   └── src/main/java/io/github/atengk/flink/model/
├── flink-common/
│   └── src/main/java/io/github/atengk/flink/common/
├── flink-connector/
│   └── src/main/java/io/github/atengk/flink/connector/
├── flink-job-user-behavior/
│   └── src/main/java/io/github/atengk/flink/job/userbehavior/
├── flink-job-order/
│   └── src/main/java/io/github/atengk/flink/job/order/
└── deploy/
    ├── yarn/
    └── kubernetes/
```

推荐包结构：

```text
io.github.atengk.flink
├── model                 # 公共数据模型
├── common                # 公共工具、参数、配置、异常、日志
│   ├── config
│   ├── exception
│   ├── id
│   ├── log
│   ├── param
│   └── security
├── connector             # 自定义 Source、Sink、序列化器
│   ├── kafka
│   ├── jdbc
│   ├── http
│   └── serde
└── job
    ├── userbehavior      # 用户行为作业
    │   ├── function
    │   ├── source
    │   ├── sink
    │   ├── window
    │   ├── metric
    │   ├── dim
    │   └── UserBehaviorJob.java
    └── order             # 订单相关作业
```

包结构建议：

1. `model` 只放数据模型，不放业务处理逻辑。
2. `common` 只放跨作业复用能力，例如参数解析、配置加载、异常定义、脱敏工具。
3. `connector` 放自定义 Source、Sink、序列化器、外部系统客户端封装。
4. `job` 按业务域划分，每个作业有独立入口类。
5. `function` 放 Flink 算子函数，例如 `MapFunction`、`ProcessFunction`、`AggregateFunction`。
6. `dim` 放维表查询、广播维表、异步维表相关代码。
7. 不建议把所有类都放到 `service`、`util` 或 `handler` 这类模糊包中。

### 类命名规范

类命名应体现 Flink 作业中的职责。名称应包含业务对象、处理动作和 Flink 类型，避免出现 `DataHandler`、`CommonFunction`、`TestJob`、`MySink` 这类不可维护名称。

推荐命名规则：

| 类别              | 命名格式                             | 示例                                    |
| ----------------- | ------------------------------------ | --------------------------------------- |
| 作业入口          | `XxxJob`                             | `UserBehaviorCleanJob`                  |
| 本地调试入口      | `XxxLocalJob`                        | `UserBehaviorLocalJob`                  |
| 参数类            | `XxxParameter`                       | `FlinkJobParameter`                     |
| 配置类            | `XxxConfig`                          | `KafkaSourceConfig`                     |
| 数据模型          | `XxxEvent`、`XxxResult`、`XxxMetric` | `UserBehaviorEvent`                     |
| Map 函数          | `XxxMapFunction`                     | `UserBehaviorToMetricMapFunction`       |
| FlatMap 函数      | `XxxFlatMapFunction`                 | `UserBehaviorArrayFlatMapFunction`      |
| Process 函数      | `XxxProcessFunction`                 | `UserBehaviorJsonProcessFunction`       |
| KeyedProcess 函数 | `XxxKeyedProcessFunction`            | `OrderTimeoutProcessFunction`           |
| 窗口函数          | `XxxWindowFunction`                  | `WindowEventCountFunction`              |
| 聚合函数          | `XxxAggregateFunction`               | `EventCountAggregateFunction`           |
| Sink 函数         | `XxxSinkFunction`                    | `HttpAlarmSinkFunction`                 |
| Source 函数       | `XxxSourceFunction`                  | `PollingMockSourceFunction`             |
| 序列化器          | `XxxSerializationSchema`             | `UserBehaviorJsonSerializationSchema`   |
| 反序列化器        | `XxxDeserializationSchema`           | `UserBehaviorJsonDeserializationSchema` |
| 工具类            | `XxxUtil`                            | `IdempotentKeyUtil`                     |
| 异常类            | `XxxException`                       | `FlinkJobConfigException`               |

类命名建议：

1. 类名必须体现业务含义和技术角色。
2. 作业入口类必须以 `Job` 结尾。
3. Flink Function 类必须保留 Function 类型后缀。
4. 配置类以 `Config` 结尾，启动参数类以 `Parameter` 结尾。
5. 工具类以 `Util` 结尾，并提供私有构造器。
6. 不使用 `Manager`、`Processor`、`Handler` 作为万能后缀，除非职责非常明确。

### 方法命名规范

方法命名应使用动词开头，清楚表达动作和返回结果。Flink 作业中的方法通常可以分为环境初始化、Source 构建、转换逻辑、Sink 构建、配置加载、参数校验和资源释放。

推荐方法命名：

| 场景         | 命名示例                           |
| ------------ | ---------------------------------- |
| 创建执行环境 | `createStreamExecutionEnvironment` |
| 创建表环境   | `createStreamTableEnvironment`     |
| 构建 Source  | `buildKafkaSource`                 |
| 构建 Sink    | `buildKafkaSink`                   |
| 加载配置     | `loadJobConfig`                    |
| 校验参数     | `validateParameter`                |
| 解析 JSON    | `parseUserBehaviorEvent`           |
| 构建宽表     | `buildWideEvent`                   |
| 构建告警     | `buildAlarmMessage`                |
| 注册指标     | `registerMetrics`                  |
| 初始化状态   | `initState`                        |
| 输出脏数据   | `outputDirtyData`                  |
| 关闭资源     | `closeClient`                      |

方法命名建议：

1. 返回布尔值的方法使用 `is`、`has`、`can`、`should` 开头。
2. 构造对象使用 `buildXxx`。
3. 创建外部客户端使用 `createXxxClient`。
4. 加载配置使用 `loadXxxConfig`。
5. 校验方法使用 `validateXxx`。
6. 不使用 `doSomething`、`handleData`、`processData` 这类泛化名称。

### 配置命名规范

配置命名应按环境、组件、功能分层，避免平铺大量配置项。配置项名称应稳定、语义清晰，并且敏感配置应通过 Secret、环境变量或配置中心注入。

推荐 YAML 配置结构：

```yaml
job:
  name: user-behavior-job
  env: prod
  parallelism: 8

checkpoint:
  enabled: true
  interval-ms: 60000
  timeout-ms: 600000
  min-pause-ms: 30000
  storage: hdfs:///flink/checkpoints/prod/user-behavior-job
  savepoint-dir: hdfs:///flink/savepoints/prod/user-behavior-job

kafka:
  bootstrap-servers: kafka-prod-01:9092,kafka-prod-02:9092
  group-id: user-behavior-job
  source-topic: user_behavior_log
  clean-topic: user_behavior_clean
  dirty-topic: dirty_user_behavior

sink:
  jdbc:
    url: jdbc:mysql://mysql-prod-vip:3306/realtime_report?useSSL=true&serverTimezone=Asia/Shanghai
    username: flink_writer
    password-env: MYSQL_WRITER_PASSWORD
    batch-size: 1000
    flush-interval-ms: 5000

dim:
  redis:
    host: redis-prod-vip
    port: 6379
    password-env: REDIS_PASSWORD
    cache-ttl-seconds: 300
```

配置命名建议：

1. 配置项使用小写短横线，例如 `bootstrap-servers`、`group-id`。
2. 时间单位写入配置名，例如 `interval-ms`、`timeout-ms`。
3. Topic、表名、路径、账号等不要写死在代码中。
4. 密码类配置只保留环境变量名称，例如 `password-env`。
5. 不同环境使用不同配置文件，不重新编译不同 Jar。
6. 默认值只能用于本地环境，生产配置必须显式声明。

### 日志规范

日志用于运行观测和故障定位。Flink 作业日志必须兼顾可读性、性能和安全性。高频数据处理路径不要逐条输出 INFO 日志；异常日志必须包含作业名、算子名、业务 Key 和失败原因；敏感字段必须脱敏。

推荐日志级别：

| 级别    | 使用场景                                         |
| ------- | ------------------------------------------------ |
| `DEBUG` | 本地调试、采样日志、状态细节                     |
| `INFO`  | 作业启动、配置摘要、Source/Sink 初始化、规则更新 |
| `WARN`  | 可恢复异常、脏数据、维表未命中、外部系统短暂失败 |
| `ERROR` | 系统异常、Sink 持续失败、作业需要重启的问题      |

日志示例：

```java
log.info("用户行为作业启动，作业名称：{}，环境：{}，并行度：{}", jobName, env, parallelism);

log.warn("用户行为数据校验失败，userId：{}，原因：{}", event.getUserId(), reason);

log.error("Kafka Sink写入失败，topic：{}，业务Key：{}，原因：{}",
        topic, bizKey, e.getMessage(), e);
```

日志规范建议：

1. 日志使用中文，表达清楚、简洁。
2. 日志必须使用占位符，不使用字符串拼接。
3. 高频算子中禁止逐条 INFO 输出。
4. 异常日志要包含业务 Key，便于追踪。
5. 密码、Token、手机号、身份证、Webhook 不得明文输出。
6. `catch` 后不能只打印日志不处理，要么侧输出，要么抛出异常。

### 异常处理规范

异常处理应区分数据异常和系统异常。数据异常通常进入脏数据链路；系统异常通常抛出，让 Flink 根据 Checkpoint 和重启策略恢复。不要在算子中无条件吞掉异常，否则会造成静默丢数。

异常分类：

| 类型          | 示例                    | 处理方式                   |
| ------------- | ----------------------- | -------------------------- |
| 数据格式异常  | JSON 解析失败、字段缺失 | 输出脏数据                 |
| 业务校验异常  | 金额为负、状态非法      | 输出脏数据或异常流         |
| 维表查询异常  | Redis 超时、HBase 超时  | 降级、默认值、侧输出或抛出 |
| Sink 数据异常 | 字段类型不匹配          | 输出失败数据               |
| Sink 系统异常 | 下游不可用、认证失败    | 抛出异常触发恢复           |
| 状态异常      | 状态序列化失败          | 抛出异常                   |
| 配置异常      | 缺少必需配置            | 启动失败                   |

自定义异常类示例：

下面的代码定义 Flink 作业配置异常，用于启动阶段配置校验失败。

```java
package io.github.atengk.flink.common.exception;

/**
 * Flink 作业配置异常
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class FlinkJobConfigException extends RuntimeException {

    public FlinkJobConfigException(String message) {
        super(message);
    }

    public FlinkJobConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

异常处理示例：

```java
try {
    UserBehaviorEvent event = parseUserBehaviorEvent(value);
    out.collect(event);
} catch (IllegalArgumentException e) {
    log.warn("用户行为数据校验失败，原因：{}", e.getMessage());
    outputDirtyData(ctx, value, "BUSINESS_INVALID", e.getMessage());
} catch (Exception e) {
    log.error("用户行为数据处理异常，原因：{}", e.getMessage(), e);
    throw e;
}
```

异常处理建议：

1. 数据异常不要导致作业无限重启。
2. 系统异常不要静默吞掉。
3. Sink 写入失败必须明确是否重试、抛出或补偿。
4. 配置缺失应在作业启动阶段失败。
5. 异常分类编码要稳定，便于监控和统计。
6. 异常链路必须能追溯原始数据或业务 Key。

### 注释规范

注释应解释业务目的、设计约束和关键风险，而不是重复代码表面含义。模型类、公共工具类、核心作业入口类、自定义 Source/Sink、状态处理函数必须有类注释。方法注释可以按需要添加，不要求所有简单方法都写。

类注释规范：

```java
/**
 * 用户行为实时清洗作业
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class UserBehaviorCleanJob {
}
```

推荐注释位置：

| 位置               | 是否建议 |
| ------------------ | -------- |
| 作业入口类         | 必须     |
| 公共模型类         | 必须     |
| 自定义 Source/Sink | 必须     |
| 状态处理函数       | 必须     |
| 复杂窗口逻辑       | 必须     |
| 幂等键生成         | 建议     |
| 简单 getter/setter | 不需要   |
| 一眼能看懂的代码   | 不需要   |

注释建议：

1. 类注释必须包含 `@author Ateng` 和 `@since yyyy-MM-dd`。
2. 注释说明业务含义，不重复代码。
3. 状态、定时器、幂等、补偿等关键逻辑必须解释设计原因。
4. TODO 必须包含原因和后续处理方向。
5. 不要保留无效注释、废弃代码注释和错误注释。
6. 配置文件注释要说明单位、默认值和生产注意事项。

### 工具类规范

工具类用于封装跨作业复用的通用逻辑，例如幂等键生成、脱敏、配置读取、时间转换、JSON 处理、参数校验和日志摘要。工具类必须无状态，方法应尽量纯函数化，不应隐藏访问外部系统。

工具类示例：

下面的代码用于生成 Flink 作业中的常见业务 Key，适合在多个作业中复用。

```java
package io.github.atengk.flink.common.id;

import cn.hutool.core.util.StrUtil;

/**
 * 业务 Key 生成工具
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class BizKeyUtil {

    private BizKeyUtil() {
    }

    public static String windowMetricKey(String metricName, String dimensionKey, Long windowStart, Long windowEnd) {
        if (StrUtil.hasBlank(metricName, dimensionKey)) {
            throw new IllegalArgumentException("指标名称和维度Key不能为空");
        }
        if (windowStart == null || windowEnd == null) {
            throw new IllegalArgumentException("窗口开始时间和结束时间不能为空");
        }

        return StrUtil.format("{}:{}:{}:{}", metricName, dimensionKey, windowStart, windowEnd);
    }

    public static String eventKey(String source, String eventId) {
        if (StrUtil.hasBlank(source, eventId)) {
            throw new IllegalArgumentException("来源系统和事件ID不能为空");
        }

        return StrUtil.format("{}:{}", source, eventId);
    }
}
```

工具类规范建议：

1. 工具类必须有私有构造器。
2. 工具类不持有可变全局状态。
3. 工具类不访问外部系统。
4. 通用工具放 `flink-common`，不要散落在各个作业模块。
5. 工具方法要有单元测试。
6. 和业务强绑定的逻辑不要伪装成通用工具。

## 项目实战案例

本章节提供几个典型 Flink Java 项目实战场景，包括 Kafka 实时消费清洗、MySQL CDC 实时同步、实时 UV/PV 统计、实时订单宽表、实时风控规则处理、实时告警系统和离线批处理补数。这些案例可以作为项目模板，也可以拆分组合成生产作业。

### Kafka 实时消费清洗

Kafka 实时消费清洗用于从 Kafka 读取原始 JSON 数据，完成解析、校验、脱敏、脏数据分流，并将标准化结果写入新的 Kafka Topic。该类作业通常位于实时数仓 ODS 到 DWD 层之间。

处理流程：

```text
Kafka Source
  -> JSON 解析
  -> 字段校验
  -> 敏感字段脱敏
  -> 脏数据侧输出
  -> 清洗结果 Kafka Sink
  -> 脏数据 Kafka Sink
```

核心模型示例：

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户行为事件
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBehaviorEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;

    private String userId;

    private String itemId;

    private String categoryId;

    private String eventType;

    private Long eventTime;
}
```

解析函数示例：

下面的代码从原始 JSON 中解析用户行为事件，并将解析失败或必填字段缺失的数据输出到侧输出流。

```java
package io.github.atengk.flink.job.userbehavior.function;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.flink.model.DirtyData;
import io.github.atengk.flink.model.UserBehaviorEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

/**
 * 用户行为 JSON 清洗函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserBehaviorCleanProcessFunction extends ProcessFunction<String, UserBehaviorEvent> {

    public static final OutputTag<DirtyData> DIRTY_TAG = new OutputTag<DirtyData>("dirty-user-behavior") {
    };

    @Override
    public void processElement(String value, Context ctx, Collector<UserBehaviorEvent> out) {
        try {
            if (StrUtil.isBlank(value)) {
                outputDirty(ctx, value, "EMPTY_DATA", "原始数据为空");
                return;
            }

            UserBehaviorEvent event = JSONUtil.toBean(value, UserBehaviorEvent.class);
            if (event == null || StrUtil.hasBlank(event.getEventId(), event.getUserId(), event.getEventType())) {
                outputDirty(ctx, value, "MISSING_REQUIRED_FIELD", "eventId、userId或eventType为空");
                return;
            }

            out.collect(event);
        } catch (Exception e) {
            outputDirty(ctx, value, "JSON_PARSE_ERROR", ExceptionUtil.getSimpleMessage(e));
            log.warn("用户行为JSON解析失败，原因：{}", ExceptionUtil.getSimpleMessage(e));
        }
    }

    private void outputDirty(Context ctx, String payload, String reason, String message) {
        ctx.output(DIRTY_TAG, new DirtyData(
                "user-behavior-clean-job",
                payload,
                reason,
                message,
                System.currentTimeMillis()
        ));
    }
}
```

作业主流程示例：

下面的代码展示 Kafka 清洗作业的主流程，包含清洗主流和脏数据侧输出流。

```java
SingleOutputStreamOperator<UserBehaviorEvent> cleanStream = rawStream
        .process(new UserBehaviorCleanProcessFunction())
        .name("clean-user-behavior-json")
        .uid("clean-user-behavior-json");

DataStream<DirtyData> dirtyStream = cleanStream
        .getSideOutput(UserBehaviorCleanProcessFunction.DIRTY_TAG)
        .name("dirty-user-behavior-stream");

cleanStream
        .map(JSONUtil::toJsonStr)
        .name("clean-event-to-json")
        .uid("clean-event-to-json")
        .sinkTo(cleanKafkaSink)
        .name("clean-user-behavior-kafka-sink")
        .uid("clean-user-behavior-kafka-sink");

dirtyStream
        .map(JSONUtil::toJsonStr)
        .name("dirty-data-to-json")
        .uid("dirty-data-to-json")
        .sinkTo(dirtyKafkaSink)
        .name("dirty-user-behavior-kafka-sink")
        .uid("dirty-user-behavior-kafka-sink");
```

设计要点：

1. 清洗结果和脏数据必须分开 Topic。
2. 原始数据解析失败不能导致作业重启。
3. 输出数据必须带 `eventId`，便于下游幂等和排查。
4. 清洗逻辑应轻量，不建议在清洗作业中做复杂维表 Join。
5. 脏数据 Topic 要设置合理保留时间，便于修复回放。
6. 作业指标至少包含输入量、输出量、脏数据量和解析失败量。

### MySQL CDC 实时同步

MySQL CDC 实时同步用于读取 MySQL 表的全量快照和增量 Binlog，并同步到 Kafka、Doris、StarRocks、Iceberg 或 Hudi。该场景适合实时入仓、缓存刷新、维表同步和数据库变更分发。

处理流程：

```text
MySQL CDC Source
  -> 快照读取
  -> Binlog 增量读取
  -> 操作类型识别
  -> 字段映射
  -> Upsert Sink
```

Flink SQL CDC DDL 示例：

```sql
CREATE TEMPORARY TABLE user_profile_mysql_cdc (
    id BIGINT,
    user_id STRING,
    user_name STRING,
    city STRING,
    user_level STRING,
    update_time TIMESTAMP(3),
    PRIMARY KEY (id) NOT ENFORCED
) WITH (
    'connector' = 'mysql-cdc',
    'hostname' = 'mysql-prod-vip',
    'port' = '3306',
    'username' = 'flink_cdc',
    'password' = '${MYSQL_CDC_PASSWORD}',
    'database-name' = 'dim',
    'table-name' = 'user_profile',
    'server-time-zone' = 'Asia/Shanghai',
    'server-id' = '5600-5699',
    'scan.startup.mode' = 'initial'
);
```

同步到 Kafka DDL 示例：

```sql
CREATE TEMPORARY TABLE user_profile_kafka_sink (
    user_id STRING,
    user_name STRING,
    city STRING,
    user_level STRING,
    update_time TIMESTAMP(3),
    PRIMARY KEY (user_id) NOT ENFORCED
) WITH (
    'connector' = 'upsert-kafka',
    'topic' = 'dim_user_profile',
    'properties.bootstrap.servers' = 'kafka-prod-01:9092,kafka-prod-02:9092',
    'key.format' = 'json',
    'value.format' = 'json'
);
```

同步 SQL：

```sql
INSERT INTO user_profile_kafka_sink
SELECT
    user_id,
    user_name,
    city,
    user_level,
    update_time
FROM user_profile_mysql_cdc;
```

设计要点：

1. CDC 用户必须有读取快照和 Binlog 的权限。
2. 每个 CDC 作业的 `server-id` 必须唯一。
3. 源表最好有主键，便于快照切分和下游 Upsert。
4. Binlog 保留时间必须覆盖作业最大停机时间。
5. 下游 Sink 必须支持 Upsert 或幂等写入。
6. DDL 变更必须有兼容策略，不能随意修改字段类型和主键。

### 实时 UV PV 统计

实时 UV/PV 统计用于按窗口统计页面访问量和独立访客数。PV 可以直接计数，UV 需要按用户去重。该场景常用于实时看板、运营大盘、活动效果和流量监控。

处理流程：

```text
用户行为事件
  -> 过滤 page_view
  -> 分配 Event Time 和 Watermark
  -> 按页面或活动分组
  -> 窗口聚合
  -> PV 计数
  -> UV 去重
  -> 写入指标表
```

指标模型示例：

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * UV PV 统计结果
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UvPvMetric implements Serializable {

    private static final long serialVersionUID = 1L;

    private String pageId;

    private Long windowStart;

    private Long windowEnd;

    private Long pv;

    private Long uv;

    private Long updateTime;
}
```

窗口统计示例：

下面的代码展示按页面统计 1 分钟窗口内的 PV 和 UV。示例使用窗口内 Set 去重，适合说明逻辑；生产高基数场景应考虑状态优化、布隆过滤器或外部去重方案。

```java
DataStream<UvPvMetric> metricStream = eventStream
        .filter(event -> StrUtil.equals(event.getEventType(), "page_view"))
        .assignTimestampsAndWatermarks(
                WatermarkStrategy
                        .<UserBehaviorEvent>forBoundedOutOfOrderness(Duration.ofSeconds(10))
                        .withTimestampAssigner((event, timestamp) -> event.getEventTime())
                        .withIdleness(Duration.ofMinutes(1))
        )
        .keyBy(UserBehaviorEvent::getItemId)
        .window(TumblingEventTimeWindows.of(Time.minutes(1)))
        .process(new UvPvWindowFunction())
        .name("uv-pv-one-minute-window")
        .uid("uv-pv-one-minute-window");
```

窗口函数示例：

```java
package io.github.atengk.flink.job.userbehavior.window;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.flink.model.UserBehaviorEvent;
import io.github.atengk.flink.model.UvPvMetric;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.util.HashSet;
import java.util.Set;

/**
 * UV PV 窗口统计函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class UvPvWindowFunction extends ProcessWindowFunction<UserBehaviorEvent, UvPvMetric, String, TimeWindow> {

    @Override
    public void process(String pageId, Context context, Iterable<UserBehaviorEvent> elements, Collector<UvPvMetric> out) {
        long pv = 0L;
        Set<String> userSet = new HashSet<>();

        for (UserBehaviorEvent event : elements) {
            pv++;
            if (event != null && event.getUserId() != null) {
                userSet.add(event.getUserId());
            }
        }

        out.collect(new UvPvMetric(
                pageId,
                context.window().getStart(),
                context.window().getEnd(),
                pv,
                CollUtil.size(userSet).longValue(),
                System.currentTimeMillis()
        ));
    }
}
```

设计要点：

1. PV 是计数指标，UV 是去重指标，二者状态压力不同。
2. UV 高基数场景不能无限使用 Set，需要评估状态大小。
3. 输出结果必须包含窗口开始和结束时间。
4. Sink 表建议使用 `pageId + windowStart + windowEnd` 作为主键。
5. 允许迟到会导致窗口重复输出，下游必须支持幂等更新。
6. 实时指标需要与离线指标定期对账。

### 实时订单宽表

实时订单宽表用于将订单主流与用户、商品、门店、支付、优惠、地区等维度或业务流关联，形成面向分析和查询的宽表。常见输出目标包括 Kafka、Doris、StarRocks、Iceberg、Hudi 和 Elasticsearch。

处理流程：

```text
订单流
  -> 订单清洗
  -> 支付流 Interval Join
  -> 用户维表 Join
  -> 商品维表 Join
  -> 构建订单宽表
  -> 写入 OLAP / Kafka / 湖仓
```

订单宽表模型示例：

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单宽表事件
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderWideEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderId;

    private String userId;

    private String userName;

    private String itemId;

    private String itemName;

    private Long orderAmount;

    private Long payAmount;

    private String payChannel;

    private Long orderTime;

    private Long payTime;

    private Long updateTime;
}
```

订单和支付 Interval Join 示例：

下面的代码用于将订单创建事件与 15 分钟内的支付成功事件关联，生成订单支付基础结果。

```java
DataStream<OrderPayResult> orderPayStream = orderStream
        .keyBy(OrderEvent::getOrderId)
        .intervalJoin(payStream.keyBy(PayEvent::getOrderId))
        .between(Time.minutes(0), Time.minutes(15))
        .process(new ProcessJoinFunction<OrderEvent, PayEvent, OrderPayResult>() {
            @Override
            public void processElement(OrderEvent order, PayEvent pay, Context ctx, Collector<OrderPayResult> out) {
                out.collect(new OrderPayResult(
                        order.getOrderId(),
                        order.getUserId(),
                        order.getOrderAmount(),
                        pay.getPayChannel(),
                        pay.getPayAmount(),
                        order.getEventTime(),
                        pay.getEventTime()
                ));
            }
        })
        .name("order-pay-interval-join")
        .uid("order-pay-interval-join");
```

订单宽表设计要点：

1. 先确定宽表使用最新维度还是事件时刻维度。
2. 支付关联适合使用 Interval Join。
3. 用户、商品等维表可以使用异步 IO 或 Lookup Join。
4. 宽表结果必须有主键，例如 `orderId`。
5. 下游写入建议使用 Upsert 或主键覆盖。
6. 对订单取消、退款、支付失败等变更事件要设计更新语义。

### 实时风控规则处理

实时风控规则处理用于根据动态规则判断用户、订单、支付、登录、设备等事件是否存在风险。规则通常来自 Kafka、CDC 或配置中心，并通过 Broadcast State 分发到所有并行实例。

处理流程：

```text
业务主流
  -> keyBy 用户 / 订单 / 设备
  -> 读取广播规则
  -> 读取历史状态
  -> 规则匹配
  -> 输出风险结果
  -> 更新状态
```

风控规则模型示例：

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 风控规则
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskRule implements Serializable {

    private static final long serialVersionUID = 1L;

    private String ruleId;

    private String ruleName;

    private String ruleType;

    private String eventType;

    private Long threshold;

    private Boolean enabled;

    private Long version;

    private Long updateTime;
}
```

风险结果模型示例：

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 风控命中结果
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskHitResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;

    private String userId;

    private String ruleId;

    private String ruleName;

    private String riskLevel;

    private String hitReason;

    private Long eventTime;

    private Long hitTime;
}
```

规则广播处理示例：

下面的代码使用 Broadcast State 动态更新风控规则，并在主流事件中读取最新规则进行匹配。

```java
package io.github.atengk.flink.job.risk.function;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.flink.model.RiskHitResult;
import io.github.atengk.flink.model.RiskRule;
import io.github.atengk.flink.model.UserBehaviorEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;

/**
 * 风控规则广播处理函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class RiskRuleBroadcastProcessFunction extends BroadcastProcessFunction<UserBehaviorEvent, RiskRule, RiskHitResult> {

    public static final MapStateDescriptor<String, RiskRule> RISK_RULE_STATE =
            new MapStateDescriptor<>("risk-rule-broadcast-state", String.class, RiskRule.class);

    @Override
    public void processElement(UserBehaviorEvent event, ReadOnlyContext ctx, Collector<RiskHitResult> out) throws Exception {
        RiskRule rule = ctx.getBroadcastState(RISK_RULE_STATE).get(event.getEventType());
        if (rule == null || BooleanUtil.isFalse(rule.getEnabled())) {
            return;
        }

        out.collect(new RiskHitResult(
                event.getEventId(),
                event.getUserId(),
                rule.getRuleId(),
                rule.getRuleName(),
                "WARN",
                StrUtil.format("事件类型命中风控规则：{}", rule.getEventType()),
                event.getEventTime(),
                System.currentTimeMillis()
        ));
    }

    @Override
    public void processBroadcastElement(RiskRule rule, Context ctx, Collector<RiskHitResult> out) throws Exception {
        if (rule == null || StrUtil.hasBlank(rule.getRuleId(), rule.getEventType())) {
            log.warn("无效风控规则，规则内容：{}", rule);
            return;
        }

        ctx.getBroadcastState(RISK_RULE_STATE).put(rule.getEventType(), rule);
        log.info("风控规则已更新，ruleId：{}，ruleName：{}，version：{}",
                rule.getRuleId(), rule.getRuleName(), rule.getVersion());
    }
}
```

设计要点：

1. 规则必须有版本号，防止乱序更新覆盖新规则。
2. 规则流异常不能影响主流处理。
3. 风控结果必须携带规则 ID、规则版本和命中原因。
4. 高频规则匹配要避免每条数据解析复杂表达式。
5. 大规模规则不适合全部广播，需要分层匹配或外部规则服务。
6. 风控命中结果必须可审计、可回放、可解释。

### 实时告警系统

实时告警系统用于根据业务指标、异常事件、风控结果或系统状态触发告警。告警系统必须支持去重、抑制、分级、路由和失败重试，避免异常时刷屏或丢失关键告警。

处理流程：

```text
指标流 / 异常流 / 风控流
  -> 告警规则匹配
  -> 告警去重
  -> 告警抑制
  -> 告警消息构建
  -> 告警 Sink
```

告警消息模型示例：

```java
package io.github.atengk.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 告警消息
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlarmMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String alarmId;

    private String alarmName;

    private String alarmLevel;

    private String alarmContent;

    private String bizKey;

    private Long eventTime;

    private Long alarmTime;
}
```

告警抑制函数示例：

下面的代码基于 Keyed State 对相同告警做时间窗口抑制，避免短时间重复发送。

```java
package io.github.atengk.flink.job.alarm.function;

import io.github.atengk.flink.model.AlarmMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

/**
 * 告警抑制处理函数
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class AlarmSuppressProcessFunction extends KeyedProcessFunction<String, AlarmMessage, AlarmMessage> {

    private static final long SUPPRESS_INTERVAL_MS = 5 * 60 * 1000L;

    private transient ValueState<Long> lastAlarmTimeState;

    @Override
    public void open(org.apache.flink.configuration.Configuration parameters) {
        lastAlarmTimeState = getRuntimeContext().getState(
                new ValueStateDescriptor<>("last-alarm-time-state", Long.class)
        );
    }

    @Override
    public void processElement(AlarmMessage value, Context ctx, Collector<AlarmMessage> out) throws Exception {
        Long lastAlarmTime = lastAlarmTimeState.value();
        long currentTime = System.currentTimeMillis();

        if (lastAlarmTime != null && currentTime - lastAlarmTime < SUPPRESS_INTERVAL_MS) {
            log.debug("告警被抑制，alarmName：{}，bizKey：{}", value.getAlarmName(), value.getBizKey());
            return;
        }

        lastAlarmTimeState.update(currentTime);
        out.collect(value);
    }
}
```

告警 Sink 示例：

```java
alarmStream
        .keyBy(alarm -> alarm.getAlarmName() + ":" + alarm.getBizKey())
        .process(new AlarmSuppressProcessFunction())
        .name("alarm-suppress")
        .uid("alarm-suppress")
        .sinkTo(alarmKafkaSink)
        .name("alarm-kafka-sink")
        .uid("alarm-kafka-sink");
```

设计要点：

1. 告警必须分级，例如 `CRITICAL`、`WARN`、`INFO`。
2. 告警必须去重和抑制。
3. 告警内容要包含作业名、业务 Key、规则、原因和排查入口。
4. 告警发送失败不能阻塞主链路，建议先写 Kafka。
5. 告警规则应配置化，支持动态调整。
6. 告警系统本身也要监控发送成功率和积压。

### 离线批处理补数

离线批处理补数用于修复历史数据、重算指标、补齐漏写结果、处理脏数据回放和修复 Sink 故障期间的数据。Flink 可以使用有界流或批执行模式处理历史数据。补数作业必须与实时作业隔离消费组、资源队列和输出路径。

处理流程：

```text
确定补数范围
  -> 读取历史 Kafka / 文件 / 湖仓数据
  -> 复用清洗和计算逻辑
  -> 写入临时表或补偿 Topic
  -> 数据校验
  -> 合并正式结果
```

补数参数模型示例：

```java
package io.github.atengk.flink.common.param;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import org.apache.flink.api.java.utils.ParameterTool;

/**
 * 补数作业参数
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Getter
public class RepairJobParameter {

    private final String env;

    private final String jobName;

    private final String input;

    private final String output;

    private final String repairBatchId;

    private final String startTime;

    private final String endTime;

    private RepairJobParameter(String env, String jobName, String input, String output,
                               String repairBatchId, String startTime, String endTime) {
        this.env = env;
        this.jobName = jobName;
        this.input = input;
        this.output = output;
        this.repairBatchId = repairBatchId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static RepairJobParameter fromArgs(String[] args) {
        ParameterTool parameterTool = ParameterTool.fromArgs(args);

        RepairJobParameter parameter = new RepairJobParameter(
                parameterTool.get("env", "test"),
                parameterTool.getRequired("jobName"),
                parameterTool.getRequired("input"),
                parameterTool.getRequired("output"),
                parameterTool.getRequired("repairBatchId"),
                parameterTool.getRequired("startTime"),
                parameterTool.getRequired("endTime")
        );

        if (StrUtil.hasBlank(parameter.getInput(), parameter.getOutput(), parameter.getRepairBatchId())) {
            throw new IllegalArgumentException("补数输入、输出和批次号不能为空");
        }

        return parameter;
    }
}
```

补数作业入口示例：

下面的代码展示一个文件补数作业入口，用于读取历史 JSON Lines 文件，复用清洗逻辑，并写入补偿 Topic。

```java
package io.github.atengk.flink.job.userbehavior;

import cn.hutool.json.JSONUtil;
import io.github.atengk.flink.common.param.RepairJobParameter;
import io.github.atengk.flink.job.userbehavior.function.UserBehaviorCleanProcessFunction;
import io.github.atengk.flink.model.UserBehaviorEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * 用户行为离线补数作业
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserBehaviorRepairJob {

    public static void main(String[] args) throws Exception {
        RepairJobParameter parameter = RepairJobParameter.fromArgs(args);

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setRuntimeMode(RuntimeExecutionMode.BATCH);
        env.setParallelism(4);

        log.info("用户行为补数作业启动，作业名称：{}，补数批次：{}，输入：{}，输出：{}",
                parameter.getJobName(), parameter.getRepairBatchId(), parameter.getInput(), parameter.getOutput());

        SingleOutputStreamOperator<UserBehaviorEvent> cleanStream = env
                .readTextFile(parameter.getInput())
                .name("repair-file-source")
                .uid("repair-file-source")
                .process(new UserBehaviorCleanProcessFunction())
                .name("repair-clean-process")
                .uid("repair-clean-process");

        cleanStream
                .map(JSONUtil::toJsonStr)
                .name("repair-event-to-json")
                .uid("repair-event-to-json")
                .print()
                .name("repair-print-sink")
                .uid("repair-print-sink");

        env.execute(parameter.getJobName());
    }
}
```

补数启动命令示例：

```bash
flink run \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorRepairJob \
  hdfs:///flink/jars/user-behavior-job/flink-job-user-behavior-1.0.1.jar \
  --env prod \
  --jobName user-behavior-repair-job \
  --input hdfs:///flink/dirty/user-behavior-job/dt=2026-05-11 \
  --output user_behavior_clean_repair \
  --repairBatchId repair-20260511-001 \
  --startTime 2026-05-11T00:00:00 \
  --endTime 2026-05-11T23:59:59
```

设计要点：

1. 补数必须有明确时间范围和批次号。
2. 补数输出先进入临时表或补偿 Topic。
3. 补数结果必须校验后再合并正式结果。
4. 补数作业和实时作业使用不同消费组、队列和资源。
5. 补数写入正式表必须依赖幂等键。
6. 补数过程要记录执行参数、输入范围、输出结果和校验报告。

以下继续展开 `生产最佳实践` 与 `项目交付文档` 两部分。

## 生产最佳实践

本章节用于总结 Flink Java 作业在生产环境中的关键实践，包括作业参数标准化、状态大小控制、Checkpoint 稳定性、数据倾斜预防、Connector 稳定性、资源容量规划、灰度发布和应急回滚。生产最佳实践的目标不是让作业“能跑”，而是让作业在高吞吐、故障恢复、版本升级、数据修复和长期运维中保持稳定。

生产环境 Flink 作业应满足以下基本要求：

```text
参数标准化
  -> 状态可控
  -> Checkpoint 稳定
  -> 数据分布均衡
  -> Connector 可降级
  -> 资源有余量
  -> 发布可灰度
  -> 故障可回滚
```

### 作业参数标准化

作业参数标准化用于统一不同 Flink 作业的启动参数、配置路径、运行环境、并行度、Checkpoint 开关、补数范围和运行模式。参数标准化可以降低发布脚本复杂度，也方便 CI/CD、运维平台和审计系统统一管理。

推荐所有作业统一支持以下参数：

| 参数                  | 示例                                | 说明                                   |
| --------------------- | ----------------------------------- | -------------------------------------- |
| `--env`               | `prod`                              | 运行环境，支持 `local`、`test`、`prod` |
| `--jobName`           | `user-behavior-job`                 | 作业名称                               |
| `--config`            | `/opt/flink/config/application.yml` | 外部配置文件路径                       |
| `--parallelism`       | `8`                                 | 作业默认并行度                         |
| `--checkpointEnabled` | `true`                              | 是否开启 Checkpoint                    |
| `--runtimeMode`       | `STREAMING`                         | 运行模式                               |
| `--startTime`         | `2026-05-11T00:00:00`               | 补数开始时间                           |
| `--endTime`           | `2026-05-11T23:59:59`               | 补数结束时间                           |
| `--repairBatchId`     | `repair-20260511-001`               | 补数批次号                             |

推荐配置结构：

```yaml
job:
  name: user-behavior-job
  env: prod
  runtime-mode: STREAMING
  parallelism: 8

checkpoint:
  enabled: true
  interval-ms: 60000
  timeout-ms: 600000
  min-pause-ms: 30000
  storage: hdfs:///flink/checkpoints/prod/user-behavior-job
  savepoint-dir: hdfs:///flink/savepoints/prod/user-behavior-job

source:
  kafka:
    bootstrap-servers: kafka-prod-01:9092,kafka-prod-02:9092
    topic: user_behavior_log
    group-id: user-behavior-job

sink:
  kafka:
    bootstrap-servers: kafka-prod-01:9092,kafka-prod-02:9092
    topic: user_behavior_clean

dirty:
  enabled: true
  topic: dirty_user_behavior
```

下面的代码用于统一解析 Flink 作业标准参数，适合放在 `flink-common` 模块中供所有作业复用。

```java
package io.github.atengk.flink.common.param;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.java.utils.ParameterTool;

/**
 * Flink 标准作业参数
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Getter
@ToString
@Slf4j
public class StandardJobParameter {

    private final String env;

    private final String jobName;

    private final String config;

    private final int parallelism;

    private final boolean checkpointEnabled;

    private final RuntimeExecutionMode runtimeMode;

    private StandardJobParameter(String env, String jobName, String config, int parallelism,
                                 boolean checkpointEnabled, RuntimeExecutionMode runtimeMode) {
        this.env = env;
        this.jobName = jobName;
        this.config = config;
        this.parallelism = parallelism;
        this.checkpointEnabled = checkpointEnabled;
        this.runtimeMode = runtimeMode;
    }

    public static StandardJobParameter fromArgs(String[] args) {
        ParameterTool parameterTool = ParameterTool.fromArgs(args);

        String env = parameterTool.get("env", "local");
        String jobName = parameterTool.get("jobName", "flink-job");
        String config = parameterTool.get("config", "");
        int parallelism = Convert.toInt(parameterTool.get("parallelism"), 1);
        boolean checkpointEnabled = Convert.toBool(parameterTool.get("checkpointEnabled"), true);
        String runtimeModeText = parameterTool.get("runtimeMode", "STREAMING");

        if (!StrUtil.equalsAny(env, "local", "test", "prod")) {
            throw new IllegalArgumentException("运行环境只允许为 local、test、prod");
        }
        if (StrUtil.isBlank(jobName)) {
            throw new IllegalArgumentException("作业名称不能为空");
        }
        if (parallelism <= 0) {
            throw new IllegalArgumentException("并行度必须大于0");
        }

        RuntimeExecutionMode runtimeMode = RuntimeExecutionMode.valueOf(runtimeModeText);

        StandardJobParameter parameter = new StandardJobParameter(
                env,
                jobName,
                config,
                parallelism,
                checkpointEnabled,
                runtimeMode
        );

        log.info("Flink标准作业参数解析完成，env：{}，jobName：{}，config：{}，parallelism：{}，checkpointEnabled：{}，runtimeMode：{}",
                parameter.getEnv(),
                parameter.getJobName(),
                parameter.getConfig(),
                parameter.getParallelism(),
                parameter.isCheckpointEnabled(),
                parameter.getRuntimeMode());

        return parameter;
    }
}
```

作业参数标准化建议：

1. 所有作业入口统一使用相同参数名称。
2. 生产环境禁止使用代码默认值连接外部系统。
3. 参数解析失败应直接启动失败，不要继续运行。
4. 敏感参数不通过命令行传递，使用 Secret、环境变量或配置中心。
5. 补数作业必须包含时间范围和补数批次号。
6. 提交参数必须进入发布记录，便于问题复现和回滚。

### 状态大小控制

状态大小控制用于防止 Flink 状态无限增长，避免 Checkpoint 变慢、恢复时间变长、RocksDB 磁盘膨胀、TaskManager OOM 和作业延迟升高。状态越大，作业越难升级、越难恢复、越难排查。

状态增长常见来源如下：

| 来源            | 示例                        |
| --------------- | --------------------------- |
| 去重状态        | 保存大量 `eventId`          |
| 窗口状态        | 大窗口、长允许迟到          |
| Join 状态       | 双流 Join 时间范围过大      |
| Broadcast State | 广播规则或维表过大          |
| MapState        | Key 下保存大量明细          |
| 缓存状态        | 本地缓存无 TTL              |
| 低质量 Key      | 空值、默认值、热点 Key 聚集 |

状态 TTL 配置示例：

```java
StateTtlConfig ttlConfig = StateTtlConfig
        .newBuilder(org.apache.flink.api.common.time.Time.hours(24))
        .setUpdateType(StateTtlConfig.UpdateType.OnCreateAndWrite)
        .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired)
        .cleanupInRocksdbCompactFilter(1000)
        .build();

ValueStateDescriptor<Long> descriptor = new ValueStateDescriptor<>(
        "user-last-event-time-state",
        Long.class
);

descriptor.enableTimeToLive(ttlConfig);

ValueState<Long> lastEventTimeState = getRuntimeContext().getState(descriptor);
```

状态结构优化建议：

| 场景           | 不推荐                 | 推荐                    |
| -------------- | ---------------------- | ----------------------- |
| 大量 Key-Value | `ValueState<Map<K,V>>` | `MapState<K,V>`         |
| 事件明细缓存   | 保存完整 JSON          | 保存必要字段            |
| 去重状态       | 永久保存 eventId       | TTL + 短周期去重        |
| 窗口聚合       | 全量窗口缓存           | 增量聚合                |
| 维表数据       | 大表 Broadcast         | Redis/HBase/Lookup Join |
| 历史明细       | 全部放 State           | 写外部存储              |

状态大小控制建议：

1. 所有长期状态必须配置 TTL。
2. 状态对象只保存业务必须字段。
3. 大 Map 使用 `MapState`，不要整体放入 `ValueState`。
4. 大窗口要评估窗口大小、允许迟到和状态保留时间。
5. Broadcast State 只适合小配置、小规则、小维表。
6. 定期监控状态大小、Checkpoint 大小和恢复耗时。
7. 状态模型变更必须经过评审和 Savepoint 恢复测试。

### Checkpoint 稳定性保障

Checkpoint 是 Flink 容错恢复的核心。生产作业必须保障 Checkpoint 稳定成功，否则故障恢复、版本升级、Savepoint、Exactly Once 和数据一致性都无法保证。Checkpoint 稳定性受状态大小、反压、Sink 提交、远程存储、RocksDB、本地磁盘和网络影响。

推荐 Checkpoint 配置：

```yaml
execution.checkpointing.interval: 60s
execution.checkpointing.mode: EXACTLY_ONCE
execution.checkpointing.timeout: 10min
execution.checkpointing.min-pause: 30s
execution.checkpointing.max-concurrent-checkpoints: 1
execution.checkpointing.tolerable-failed-checkpoints: 3
execution.checkpointing.externalized-checkpoint-retention: RETAIN_ON_CANCELLATION
execution.checkpointing.incremental: true

state.backend: rocksdb
state.backend.rocksdb.memory.managed: true

execution.checkpointing.dir: hdfs:///flink/checkpoints/prod/user-behavior-job
execution.checkpointing.savepoint-dir: hdfs:///flink/savepoints/prod/user-behavior-job
```

Checkpoint 稳定性检查项：

| 检查项         | 正常表现                     |
| -------------- | ---------------------------- |
| 成功率         | 连续成功，无长期失败         |
| 耗时           | 明显小于 Checkpoint 间隔     |
| 大小           | 增长可解释，无异常膨胀       |
| Start Delay    | 不持续升高                   |
| Alignment Time | 不持续升高                   |
| Async Duration | 不长期接近超时               |
| Sink Commit    | 无持续阻塞                   |
| 存储系统       | HDFS/S3/OSS 无抖动和权限错误 |

Checkpoint 稳定性建议：

1. Checkpoint 间隔不要小于平均完成耗时。
2. 大状态作业开启增量 Checkpoint。
3. 大状态作业设置 `min-pause`，避免连续快照。
4. Checkpoint 存储必须使用可靠共享存储。
5. Sink 提交参与 Checkpoint 时，要重点监控 Sink 事务提交耗时。
6. Checkpoint 连续失败必须告警并处理，不能长期忽略。
7. 反压严重时要先处理反压，再调 Checkpoint 参数。

### 数据倾斜预防

数据倾斜是 Flink 生产作业常见性能问题。倾斜会导致部分 Subtask 高负载、局部状态过大、Checkpoint 变慢、延迟升高和反压传播。数据倾斜应在设计阶段预防，而不是等上线后再被动处理。

常见倾斜场景：

| 场景              | 示例                       |
| ----------------- | -------------------------- |
| 空值 Key          | `userId=""`、`itemId=null` |
| 默认 Key          | `unknown`、`other`         |
| 热点用户          | 大客户、机器人用户         |
| 热点商品          | 爆款商品、活动商品         |
| 低基数维度        | 只按 `eventType` 分组      |
| 单分区 Source     | Kafka 分区数太少           |
| Sink 分区键不合理 | 所有数据写入同一分区       |

预防策略：

| 策略         | 说明                        |
| ------------ | --------------------------- |
| Key 校验     | 空 Key 进入异常流或特殊处理 |
| 分区规划     | Kafka 分区数与并行度匹配    |
| 二阶段聚合   | 热点聚合先局部再全局        |
| 热点拆分     | 给热点 Key 增加随机前缀     |
| 独立处理热点 | 热点 Key 单独分流           |
| 广播小表     | 避免大流 Join 小表造成热点  |
| Sink 分桶    | 下游按合理分区键写入        |

二阶段聚合示意：

```text
原始流
  -> 添加随机前缀
  -> keyBy(randomPrefix + bizKey)
  -> 局部聚合
  -> 去掉随机前缀
  -> keyBy(bizKey)
  -> 全局聚合
```

数据倾斜预防建议：

1. 所有 `keyBy` 前都要校验 Key 是否为空。
2. 低基数 Key 不适合直接做高吞吐聚合。
3. 热点 Key 要在压测数据中模拟。
4. Kafka 分区数不要小于目标 Source 有效并行度。
5. 指标聚合类作业要提前设计二阶段聚合方案。
6. 上线后持续观察 Subtask 级别 Records、Busy、Backpressure 和 State Size。
7. 发现倾斜后不要只加资源，应优先调整 Key 设计和聚合方式。

### Connector 稳定性保障

Connector 是 Flink 作业与外部系统的边界。生产故障中，很多问题不是 Flink 算子本身，而是 Kafka、JDBC、Redis、HBase、Elasticsearch、Doris、StarRocks、对象存储或 HTTP 服务异常造成。Connector 稳定性保障的核心是超时、重试、限流、幂等、降级、监控和隔离。

Connector 稳定性设计项：

| 设计项 | 说明                                 |
| ------ | ------------------------------------ |
| 超时   | 所有外部调用必须设置超时             |
| 重试   | 短暂异常可有限重试                   |
| 限流   | 防止压垮外部系统                     |
| 批量   | Sink 写入优先批量提交                |
| 幂等   | 失败恢复后重复写入结果一致           |
| 降级   | 维表失败可默认值或侧输出             |
| 熔断   | 外部系统持续失败时快速失败           |
| 监控   | 成功数、失败数、耗时、超时、重试数   |
| 隔离   | 补数作业和实时作业使用不同账号或资源 |

Connector 参数建议：

| Connector         | 重点参数                                                  |
| ----------------- | --------------------------------------------------------- |
| Kafka Source      | 分区数、groupId、offset、认证、反序列化                   |
| Kafka Sink        | delivery guarantee、transactional id、batch、acks         |
| JDBC Sink         | batch size、flush interval、max retry、connection timeout |
| Redis             | connection timeout、so timeout、pool size、TTL            |
| HBase             | RPC timeout、scan cache、retries、RowKey                  |
| Elasticsearch     | bulk size、flush interval、retry、mapping                 |
| Doris / StarRocks | label、batch size、flush interval、stream load timeout    |
| File Sink         | rolling policy、bucket、checkpoint、文件大小              |

Connector 稳定性建议：

1. 所有外部系统调用必须有超时。
2. Sink 写入必须明确 At Least Once、Exactly Once 或幂等语义。
3. 高吞吐 Sink 不要逐条同步写入。
4. 外部维表查询优先异步 IO、缓存和限流。
5. Connector 失败指标必须接入告警。
6. Connector 升级必须做端到端测试。
7. 补数作业和实时作业不要共享同一个下游写入配额。

### 资源容量规划

资源容量规划用于在作业上线前评估 CPU、内存、Slot、Kafka 分区、状态大小、Checkpoint 存储、网络、磁盘和 Sink 能力。容量规划不能只看当前流量，还要考虑峰值流量、业务增长、补数压力、故障恢复和灰度发布。

容量规划输入项：

| 输入项          | 示例           |
| --------------- | -------------- |
| 峰值 TPS        | 每秒 10 万条   |
| 平均消息大小    | 1 KB           |
| Kafka 分区数    | 48             |
| 作业并行度      | 24             |
| 状态保留时间    | 24 小时        |
| Checkpoint 间隔 | 60 秒          |
| 窗口大小        | 1 分钟、5 分钟 |
| 维表查询 QPS    | 每秒 2 万次    |
| Sink 写入能力   | 每秒 5 万行    |
| 故障恢复目标    | 10 分钟内恢复  |

容量规划参考表：

| 资源             | 规划依据                           |
| ---------------- | ---------------------------------- |
| Source 并行度    | Kafka 分区数、反序列化 CPU         |
| 计算并行度       | CPU 消耗、Key 分布、窗口复杂度     |
| Sink 并行度      | 下游写入能力、批量大小             |
| Slot 数          | 最大并行度、Slot Sharing、资源隔离 |
| TaskManager 内存 | 状态大小、缓存、RocksDB、网络      |
| Managed Memory   | RocksDB、排序、批处理算子          |
| Network Memory   | Shuffle、Join、高并行度            |
| 本地磁盘         | RocksDB 状态、日志、HeapDump       |
| Checkpoint 存储  | 状态大小、保留数量、增长速度       |

容量规划建议：

1. 生产资源按峰值流量规划，不按平均流量规划。
2. 至少保留 30% 到 50% 资源余量，应对流量波动和恢复压力。
3. 大状态作业优先规划 RocksDB 本地磁盘和 Managed Memory。
4. Kafka 分区数要支撑 Source 并行读取。
5. Sink 下游能力必须压测，不要只看 Flink 资源。
6. 补数作业使用独立资源，不能挤占实时主链路。
7. 每次容量变更都要记录在发布或运维文档中。

### 灰度发布

灰度发布用于降低生产变更风险。Flink 作业灰度比普通服务复杂，因为作业通常有状态、消费位点、Sink 写入和业务结果。灰度发布应根据作业类型选择不同策略。

常见灰度方式：

| 方式            | 适用场景       | 说明                            |
| --------------- | -------------- | ------------------------------- |
| 测试 Topic 灰度 | Kafka 链路     | 新版本消费测试 Topic            |
| 影子消费        | 不影响正式输出 | 新版本消费同源数据，写临时 Sink |
| 双跑对比        | 指标口径变更   | 新旧作业同时跑，对比结果        |
| 小流量分流      | 上游可分流     | 只处理部分业务 Key              |
| 临时表输出      | 宽表或指标     | 新版本写临时表                  |
| Savepoint 灰度  | 有状态升级     | 从 Savepoint 启动新版本验证     |
| 单作业灰度      | 多作业体系     | 先升级低风险作业                |

双跑灰度流程：

```text
旧版本正式作业
  -> 写正式结果表

新版本灰度作业
  -> 使用独立 groupId 消费同源数据
  -> 写灰度结果表
  -> 与正式结果对账
  -> 通过后切换正式版本
```

灰度发布建议：

1. 核心指标口径变更必须双跑对比。
2. 灰度作业使用独立消费组。
3. 灰度输出写临时 Topic 或临时表。
4. 灰度期间不要让新旧作业同时写同一个非幂等 Sink。
5. 灰度验证至少包括数据量、主键、金额、延迟、脏数据和 Sink 错误。
6. 灰度通过后再执行正式 Savepoint 升级。
7. 灰度失败必须保留差异样本，便于定位问题。

### 应急回滚

应急回滚用于在生产发布失败、业务指标异常、数据错误、Checkpoint 失败、Sink 写入异常或依赖升级失败时快速恢复。应急回滚的核心是发布前 Savepoint、旧版本 Jar 或镜像、旧版本配置和明确的恢复命令。

回滚前判断：

| 判断项                 | 说明                       |
| ---------------------- | -------------------------- |
| 是否有发布前 Savepoint | 没有 Savepoint 回滚风险高  |
| 新版本是否写入错误数据 | 需要先评估下游修复         |
| 状态是否兼容旧版本     | 新状态不能保证被旧代码读取 |
| Kafka 数据是否仍可回放 | Topic 保留时间是否足够     |
| Sink 是否幂等          | 重复写入是否会造成问题     |
| 配置是否可回退         | 旧配置是否仍可用           |
| Connector 是否可回退   | 旧依赖是否保留             |

YARN 回滚命令模板：

```bash
flink run \
  -t yarn-application \
  -s hdfs:///flink/savepoints/prod/user-behavior-job/savepoint-before-release-1.0.1 \
  -Djobmanager.memory.process.size=2048m \
  -Dtaskmanager.memory.process.size=4096m \
  -Dtaskmanager.numberOfTaskSlots=4 \
  -Dparallelism.default=8 \
  -Dyarn.application.name=user-behavior-job-rollback \
  -Dyarn.application.queue=realtime \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  hdfs:///flink/jars/user-behavior-job/flink-job-user-behavior-1.0.0.jar \
  --env prod \
  --jobName user-behavior-job \
  --config hdfs:///flink/config/prod/user-behavior/application-1.0.0.yml \
  --parallelism 8
```

Kubernetes Operator 回滚配置：

```yaml
spec:
  image: registry.example.com/flink/flink-job-user-behavior:1.0.0
  job:
    jarURI: local:///opt/flink/usrlib/flink-job-user-behavior.jar
    entryClass: io.github.atengk.flink.job.userbehavior.UserBehaviorJob
    parallelism: 8
    upgradeMode: savepoint
    initialSavepointPath: s3://flink-prod/savepoints/user-behavior-job/savepoint-before-release-1.0.1
    savepointRedeployNonce: 4
```

应急回滚建议：

1. 发布前必须创建 Savepoint。
2. 回滚使用旧 Jar、旧配置、旧镜像和发布前 Savepoint。
3. 回滚后检查 Checkpoint、Kafka Lag、Sink 写入和业务指标。
4. 如果新版本已经污染下游结果，需要执行数据修复。
5. 回滚过程必须记录操作人、时间、版本、Savepoint 和原因。
6. 核心作业应定期演练回滚流程。

## 项目交付文档

本章节用于说明 Flink 项目交付时应包含的文档，包括架构设计文档、接口说明文档、部署文档、运维手册、监控告警文档、故障处理文档和发布记录。Flink 作业不是只交付代码和 Jar，还必须交付可运行、可维护、可监控、可回滚、可审计的一整套资料。

推荐交付目录：

```text
docs/
├── 01-architecture-design.md
├── 02-interface-specification.md
├── 03-deployment-guide.md
├── 04-operations-manual.md
├── 05-monitoring-alerting.md
├── 06-troubleshooting-guide.md
└── 07-release-records/
    ├── release-1.0.0.md
    └── release-1.0.1.md
```

### 架构设计文档

架构设计文档用于说明作业的业务背景、技术架构、数据流、状态设计、时间语义、容错机制、部署模式和数据一致性。该文档应在开发前完成初版，在上线前补齐最终设计。

推荐结构：

```markdown
# Flink 作业架构设计文档

## 1. 项目背景

## 2. 业务目标

## 3. 总体架构

## 4. 数据流设计

## 5. Source 设计

## 6. Transformation 设计

## 7. State 设计

## 8. 时间语义与 Watermark

## 9. Sink 设计

## 10. 数据一致性

## 11. 容错与恢复

## 12. 部署架构

## 13. 资源规划

## 14. 安全设计

## 15. 风险与限制
```

架构设计文档应包含：

| 内容       | 说明                              |
| ---------- | --------------------------------- |
| 作业名称   | 统一作业标识                      |
| 业务链路   | 上游、Flink、下游                 |
| 数据流图   | Source 到 Sink 的拓扑             |
| 时间语义   | Processing Time / Event Time      |
| Watermark  | 延迟策略、空闲分区                |
| 状态设计   | 状态类型、TTL、状态大小预估       |
| Checkpoint | 间隔、超时、存储位置              |
| Sink 语义  | At Least Once、Exactly Once、幂等 |
| 异常处理   | 脏数据、迟到数据、失败数据        |
| 安全       | 认证、权限、脱敏、密钥            |
| 风险       | 数据倾斜、外部系统压力、状态膨胀  |

架构设计文档建议：

1. 必须包含数据流图或文字版流程图。
2. 必须说明 Source、State、Sink 的一致性语义。
3. 必须说明状态大小预估和 TTL 策略。
4. 必须说明迟到数据和脏数据处理方式。
5. 必须说明上线风险和回滚方案。
6. 架构变更后要同步更新文档。

### 接口说明文档

接口说明文档用于描述 Source、Sink、Kafka Topic、数据库表、字段 Schema、数据格式、主键、幂等键、错误码和数据契约。它是上下游团队协作的基础，也是排查数据问题和做 Schema 演进的依据。

推荐结构：

```markdown
# Flink 作业接口说明文档

## 1. Source 接口

### 1.1 Kafka Source

### 1.2 CDC Source

### 1.3 文件 Source

## 2. Sink 接口

### 2.1 Kafka Sink

### 2.2 JDBC Sink

### 2.3 Doris / StarRocks Sink

## 3. 字段 Schema

## 4. 主键与幂等键

## 5. 数据示例

## 6. 脏数据格式

## 7. 错误码说明

## 8. Schema 变更规则
```

Kafka Source 说明模板：

```markdown
## Kafka Source：user_behavior_log

| 项目 | 内容 |
| --- | --- |
| Topic | `user_behavior_log` |
| 消费组 | `user-behavior-job` |
| 数据格式 | JSON |
| Key | `eventId` |
| Value | 用户行为 JSON |
| 语义 | Kafka Offset 随 Checkpoint 管理 |
| 保留时间 | 7 天 |
| 负责人 | 数据采集团队 |

### 字段说明

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| eventId | STRING | 是 | 事件唯一 ID |
| userId | STRING | 是 | 用户 ID |
| itemId | STRING | 否 | 商品或页面 ID |
| categoryId | STRING | 否 | 类目 ID |
| eventType | STRING | 是 | 事件类型 |
| eventTime | BIGINT | 是 | 事件时间，毫秒时间戳 |
```

Sink 表说明模板：

```markdown
## Sink 表：user_event_count

| 项目 | 内容 |
| --- | --- |
| 存储 | MySQL |
| 数据库 | realtime_report |
| 表名 | user_event_count |
| 写入方式 | Upsert |
| 主键 | idempotent_key |
| 写入语义 | 幂等写入 |
| 更新频率 | 每分钟 |

### 字段说明

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| idempotent_key | VARCHAR(256) | 幂等键 |
| metric_name | VARCHAR(128) | 指标名称 |
| dimension_key | VARCHAR(128) | 维度 Key |
| window_start | BIGINT | 窗口开始时间 |
| window_end | BIGINT | 窗口结束时间 |
| metric_value | BIGINT | 指标值 |
| update_time | DATETIME | 更新时间 |
```

接口说明文档建议：

1. Source 和 Sink 都必须有字段 Schema。
2. 必须明确主键、幂等键和写入语义。
3. Kafka Topic 必须说明数据格式、Key、Value 和保留时间。
4. 数据库表必须说明主键、索引、写入模式和更新语义。
5. Schema 变更必须更新接口文档。
6. 脏数据格式和错误码必须文档化。

### 部署文档

部署文档用于说明作业如何打包、配置、提交、升级、停止和恢复。部署文档应能够让运维人员不阅读代码也能完成标准发布。

推荐结构：

```markdown
# Flink 作业部署文档

## 1. 部署环境

## 2. 制品信息

## 3. 配置文件

## 4. 依赖说明

## 5. YARN 部署

## 6. Kubernetes 部署

## 7. 参数说明

## 8. Savepoint 升级

## 9. 回滚操作

## 10. 部署验证
```

YARN 部署模板：

```bash
flink run \
  -t yarn-application \
  -Djobmanager.memory.process.size=2048m \
  -Dtaskmanager.memory.process.size=4096m \
  -Dtaskmanager.numberOfTaskSlots=4 \
  -Dparallelism.default=8 \
  -Dyarn.application.name=user-behavior-job \
  -Dyarn.application.queue=realtime \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  hdfs:///flink/jars/user-behavior-job/flink-job-user-behavior-1.0.0.jar \
  --env prod \
  --jobName user-behavior-job \
  --config hdfs:///flink/config/prod/user-behavior/application.yml \
  --parallelism 8
```

Kubernetes Operator 部署模板：

```bash
kubectl apply -f deploy/kubernetes/configmap-user-behavior.yaml
kubectl apply -f deploy/kubernetes/secret-user-behavior.yaml
kubectl apply -f deploy/kubernetes/user-behavior-flinkdeployment.yaml

kubectl get flinkdeployment user-behavior-job -n flink-prod
kubectl describe flinkdeployment user-behavior-job -n flink-prod
```

部署文档建议：

1. 所有命令必须可复制执行。
2. 明确 Jar 路径、主类、配置路径和参数。
3. 明确 YARN 队列或 Kubernetes Namespace。
4. 明确 Checkpoint 和 Savepoint 路径。
5. 明确发布后验证项。
6. 回滚命令必须写入部署文档。

### 运维手册

运维手册用于指导日常启动、停止、重启、Savepoint、Checkpoint 清理、日志查看、状态查看、资源调整和数据修复。运维手册应更偏操作步骤，而不是设计说明。

推荐结构：

```markdown
# Flink 作业运维手册

## 1. 作业信息

## 2. 日常启动

## 3. 日常停止

## 4. 作业重启

## 5. Savepoint 创建

## 6. Savepoint 恢复

## 7. Checkpoint 清理

## 8. 日志查看

## 9. 指标查看

## 10. 数据修复

## 11. 常用命令
```

常用命令清单：

```bash
# 查看作业列表
flink list -a

# 触发 Savepoint
flink savepoint <jobId> hdfs:///flink/savepoints/prod/user-behavior-job

# 停止并创建 Savepoint
flink stop --savepointPath hdfs:///flink/savepoints/prod/user-behavior-job <jobId>

# 从 Savepoint 恢复
flink run \
  -s hdfs:///flink/savepoints/prod/user-behavior-job/savepoint-xxx \
  -c io.github.atengk.flink.job.userbehavior.UserBehaviorJob \
  hdfs:///flink/jars/user-behavior-job/flink-job-user-behavior.jar \
  --env prod \
  --jobName user-behavior-job

# 查看 YARN 日志
yarn logs -applicationId application_1710000000000_0001

# 查看 Kubernetes Pod
kubectl get pods -n flink-prod
```

运维手册建议：

1. 使用操作步骤和命令，不写过多理论。
2. 区分 YARN、Kubernetes、Standalone 的操作方式。
3. 明确哪些操作需要审批。
4. 明确哪些目录不能清理。
5. 明确异常时联系谁。
6. 运维手册应随部署方式变化同步更新。

### 监控告警文档

监控告警文档用于说明作业需要监控哪些系统指标、业务指标、告警规则、阈值、通知方式和处理人。它是生产可观测性的核心资料。

推荐结构：

```markdown
# Flink 作业监控告警文档

## 1. 监控目标

## 2. Flink 系统指标

## 3. Kafka 指标

## 4. Checkpoint 指标

## 5. State / RocksDB 指标

## 6. Sink 指标

## 7. 业务指标

## 8. 告警规则

## 9. 告警处理人

## 10. Grafana 看板
```

推荐告警规则：

| 告警项              | 条件                          | 等级     |
| ------------------- | ----------------------------- | -------- |
| 作业停止            | 作业状态非 Running            | Critical |
| Checkpoint 连续失败 | 5 分钟内失败超过 3 次         | Critical |
| Checkpoint 耗时过长 | 耗时超过间隔 80%              | Warning  |
| Kafka Lag 持续增长  | 10 分钟持续增长               | Warning  |
| 反压严重            | Backpressure High 超过 5 分钟 | Warning  |
| Sink 失败           | 写入失败数大于 0              | Critical |
| 脏数据激增          | 脏数据比例超过 1%             | Warning  |
| Watermark 停滞      | 10 分钟无推进                 | Warning  |
| 维表命中率下降      | 低于 95%                      | Warning  |

业务指标说明模板：

```markdown
## 业务指标：dirty_data_total

| 项目 | 内容 |
| --- | --- |
| 指标名称 | dirty_data_total |
| 指标类型 | Counter |
| 说明 | 脏数据总量 |
| 标签 | jobName、env、reason |
| 告警条件 | 5 分钟增长量超过 1000 |
| 处理方式 | 查看脏数据 Topic，确认 reason 分布 |
```

监控告警文档建议：

1. 系统指标和业务指标都要覆盖。
2. 告警必须有处理人和升级路径。
3. 告警阈值要基于历史基线。
4. 告警内容要包含作业名、环境、指标值和排查入口。
5. Grafana 看板地址要写入文档。
6. 告警规则变更要有记录。

### 故障处理文档

故障处理文档用于记录常见故障的现象、原因、排查步骤、处理命令和预防措施。它应服务于一线运维和开发排障，避免每次故障都从零开始分析。

推荐结构：

```markdown
# Flink 作业故障处理文档

## 1. 作业启动失败

## 2. 依赖冲突

## 3. Kafka 消费异常

## 4. Checkpoint 失败

## 5. 反压问题

## 6. OOM 问题

## 7. 数据延迟

## 8. 数据重复

## 9. 数据丢失

## 10. Sink 写入失败

## 11. 故障复盘模板
```

故障处理模板：

```markdown
## 故障：Checkpoint 连续失败

### 现象

- Flink Web UI 中 Checkpoint 连续失败
- 告警：FlinkCheckpointFailure
- Kafka Lag 开始上涨

### 可能原因

- 状态过大
- 远程存储不可写
- Sink 事务提交阻塞
- 反压严重
- RocksDB 本地磁盘异常

### 排查步骤

1. 查看 Flink Web UI Checkpoints 页面
2. 查看失败原因和异常堆栈
3. 查看 Back Pressure 页面
4. 查看 TaskManager 日志
5. 查看 HDFS/S3/OSS 写入权限和耗时
6. 查看 Sink 写入耗时

### 处理方式

- 临时降低输入流量
- 修复远程存储权限
- 优化 Sink 批量和事务提交
- 调整 Checkpoint 间隔和 min-pause
- 必要时从最近 Savepoint 恢复

### 预防措施

- 监控 Checkpoint 成功率和耗时
- 控制状态大小
- 大状态作业开启增量 Checkpoint
```

故障处理文档建议：

1. 每类故障都要包含现象、原因、排查、处理、预防。
2. 命令必须可复制。
3. 故障文档要根据真实事故持续补充。
4. 不要只写“重启作业”，必须说明重启前后检查项。
5. 数据重复和数据丢失必须有补偿方案。
6. 故障复盘结果要沉淀到文档中。

### 发布记录

发布记录用于记录每次生产变更的版本、制品、配置、Savepoint、发布人、审批人、验证结果和回滚方案。发布记录是审计、回滚和故障复盘的依据。

推荐发布记录模板：

```markdown
# Flink 作业发布记录

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| 作业名称 | user-behavior-job |
| 发布版本 | 1.0.0 |
| Git Commit | 8f3a2c1 |
| Jar 路径 | hdfs:///flink/jars/user-behavior-job/flink-job-user-behavior-1.0.0.jar |
| 镜像 | registry.example.com/flink/flink-job-user-behavior:1.0.0 |
| 配置版本 | config-20260511-001 |
| 发布环境 | prod |
| 发布人 | Ateng |
| 审批人 | xxx |
| 发布时间 | 2026-05-11 20:00:00 |

## 2. 变更内容

- 新增用户行为清洗逻辑
- 增加脏数据侧输出
- 调整 Checkpoint 间隔为 60 秒

## 3. 发布前状态

| 项目 | 内容 |
| --- | --- |
| 作业状态 | Running |
| Checkpoint | 正常 |
| Kafka Lag | 正常 |
| Sink 写入 | 正常 |
| 发布前 Savepoint | hdfs:///flink/savepoints/prod/user-behavior-job/savepoint-before-release-1.0.0 |

## 4. 发布步骤

1. 创建 Savepoint
2. 停止旧版本作业
3. 提交新版本作业
4. 验证作业状态
5. 验证 Checkpoint
6. 验证业务指标

## 5. 验证结果

| 检查项 | 结果 |
| --- | --- |
| 作业状态 | 通过 |
| Checkpoint | 通过 |
| Kafka Lag | 通过 |
| Sink 写入 | 通过 |
| 脏数据量 | 通过 |
| 业务指标 | 通过 |

## 6. 回滚方案

- 回滚版本：0.9.9
- 回滚 Jar：hdfs:///flink/jars/user-behavior-job/flink-job-user-behavior-0.9.9.jar
- 回滚配置：application-0.9.9.yml
- 回滚 Savepoint：hdfs:///flink/savepoints/prod/user-behavior-job/savepoint-before-release-1.0.0

## 7. 备注

- 无
```

发布记录建议：

1. 每次生产发布必须有记录。
2. 记录中必须包含 Savepoint 路径。
3. 记录中必须包含制品版本和配置版本。
4. 发布验证结果不能只写“正常”，应写具体检查项。
5. 回滚方案必须在发布前确认。
6. 发布记录应长期保存，便于审计和故障复盘。

## 总结

本章节对 Flink Java 开发文档进行收束，梳理从需求分析、工程搭建、作业开发、测试验证、部署上线到生产运维的完整流程，并总结关键技术点、常见风险和后续优化方向。Flink 项目的交付目标不是单纯完成一个实时作业，而是形成一套可开发、可测试、可部署、可监控、可恢复、可升级的数据处理体系。

### 核心开发流程

Flink Java 项目的核心开发流程可以分为需求分析、工程初始化、数据接入、业务处理、状态与时间语义设计、结果输出、测试验证、部署上线和运维治理几个阶段。每个阶段都应有明确输入、输出和检查标准。

整体流程如下：

```text
需求分析
  -> 架构设计
  -> 工程初始化
  -> Source 开发
  -> 数据解析与模型设计
  -> DataStream / Table API 业务开发
  -> 时间语义与 Watermark 设计
  -> 状态与窗口设计
  -> Sink 输出设计
  -> 容错与一致性配置
  -> 单元测试与集成测试
  -> 打包构建
  -> 测试环境部署
  -> 数据回放验证
  -> 生产发布
  -> 监控告警
  -> 运维优化
```

推荐开发步骤：

| 阶段     | 重点工作                                     | 交付物             |
| -------- | -------------------------------------------- | ------------------ |
| 需求分析 | 明确实时指标、数据来源、延迟要求、一致性要求 | 需求说明、指标口径 |
| 架构设计 | 确定 Source、State、Sink、部署模式、容错策略 | 架构设计文档       |
| 工程搭建 | 创建 Maven 多模块、依赖管理、配置目录        | 项目骨架           |
| 数据接入 | Kafka、CDC、文件、自定义 Source              | Source 模块        |
| 数据解析 | JSON、Avro、Protobuf、POJO、脏数据           | 数据模型和解析函数 |
| 业务处理 | 清洗、聚合、Join、维表、规则、告警           | Flink 作业逻辑     |
| 状态设计 | Keyed State、Operator State、TTL、RocksDB    | 状态设计说明       |
| 时间语义 | Event Time、Watermark、迟到数据              | 时间语义配置       |
| 结果输出 | Kafka、JDBC、文件、OLAP、湖仓                | Sink 模块          |
| 容错设计 | Checkpoint、Savepoint、重启策略              | 容错配置           |
| 测试验证 | 单测、MiniCluster、Testcontainers、回放      | 测试报告           |
| 打包部署 | Fat Jar、镜像、YARN、Kubernetes              | 部署制品           |
| 监控运维 | Metrics、Prometheus、Grafana、告警           | 运维手册           |
| 升级回滚 | Savepoint 升级、灰度、回滚                   | 发布记录           |

标准作业入口应统一完成以下动作：

```text
解析启动参数
  -> 加载环境配置
  -> 初始化 StreamExecutionEnvironment
  -> 配置 Checkpoint
  -> 创建 Source
  -> 解析与清洗数据
  -> 执行业务转换
  -> 写入 Sink
  -> 注册监控指标
  -> execute(jobName)
```

作业入口参考结构：

```java
package io.github.atengk.flink.job.userbehavior;

import io.github.atengk.flink.common.param.StandardJobParameter;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * 用户行为实时处理作业
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserBehaviorJob {

    public static void main(String[] args) throws Exception {
        StandardJobParameter parameter = StandardJobParameter.fromArgs(args);

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setRuntimeMode(RuntimeExecutionMode.STREAMING);
        env.setParallelism(parameter.getParallelism());

        if (parameter.isCheckpointEnabled()) {
            env.enableCheckpointing(60_000L);
            log.info("Checkpoint已开启，作业名称：{}", parameter.getJobName());
        }

        log.info("Flink作业开始构建，作业名称：{}，环境：{}",
                parameter.getJobName(), parameter.getEnv());

        // 1. 创建 Source
        // 2. 数据解析与清洗
        // 3. 业务处理
        // 4. 维表关联
        // 5. 结果输出
        // 6. 脏数据输出
        // 7. 指标注册

        env.execute(parameter.getJobName());
    }
}
```

核心开发流程建议：

1. 先设计数据流和一致性语义，再写代码。
2. 所有作业入口统一参数解析和配置加载方式。
3. 业务模型、状态模型和 Sink 模型必须稳定。
4. 作业必须有脏数据链路、迟到数据链路和监控指标。
5. 生产发布前必须完成数据回放和 Savepoint 恢复验证。
6. 上线后持续关注 Checkpoint、反压、Lag、Sink 错误和业务指标。

### 关键技术点

Flink Java 开发的关键技术点集中在流处理模型、时间语义、状态管理、容错机制、数据一致性、Connector 集成、性能优化和运维治理几个方面。掌握这些技术点，是构建生产级实时作业的基础。

核心技术点汇总如下：

| 技术点          | 说明                                                         |
| --------------- | ------------------------------------------------------------ |
| DataStream API  | 实现实时清洗、转换、聚合、Join、侧输出流                     |
| Table API / SQL | 实现声明式查询、Lookup Join、窗口聚合和数据同步              |
| Event Time      | 基于事件发生时间计算，适合真实业务时间语义                   |
| Watermark       | 推进事件时间，处理乱序和迟到数据                             |
| Window          | 滚动窗口、滑动窗口、会话窗口、全局窗口                       |
| State           | Keyed State、Operator State、Broadcast State                 |
| State TTL       | 控制状态生命周期，防止状态无限增长                           |
| Checkpoint      | Flink 容错恢复的核心机制                                     |
| Savepoint       | 升级、回滚、迁移和状态恢复的关键机制                         |
| Exactly Once    | Source、Flink State、Sink 共同保障的一致性语义               |
| Connector       | Kafka、JDBC、CDC、Elasticsearch、Doris、StarRocks、Iceberg、Hudi |
| Async IO        | 维表查询和外部服务访问的高吞吐方案                           |
| Broadcast State | 动态规则、配置流、小维表广播                                 |
| Side Output     | 脏数据、迟到数据、异常数据分流                               |
| RocksDB         | 大状态作业常用状态后端                                       |
| Metrics         | 系统指标和业务指标监控                                       |
| CI/CD           | 构建、测试、部署、灰度和回滚自动化                           |

技术选型建议：

| 场景           | 推荐方案                                      |
| -------------- | --------------------------------------------- |
| Kafka 实时清洗 | DataStream API + Kafka Source/Sink            |
| MySQL 实时同步 | Flink CDC + Upsert Sink                       |
| 实时指标统计   | Event Time + Watermark + Window + Upsert Sink |
| 实时宽表       | Kafka 主流 + Async IO / Lookup Join           |
| 动态规则       | Broadcast State                               |
| 高吞吐维表     | Redis / HBase / Async IO                      |
| 小维表配置     | Broadcast State                               |
| 大状态去重     | Keyed State + TTL + RocksDB                   |
| 精确结果输出   | 幂等 Sink / 事务 Sink                         |
| 数据修复       | 有界流补数 + 幂等覆盖                         |

关键配置示例：

```yaml
job:
  name: user-behavior-job
  env: prod
  parallelism: 8

checkpoint:
  enabled: true
  interval-ms: 60000
  timeout-ms: 600000
  min-pause-ms: 30000
  mode: EXACTLY_ONCE
  storage: hdfs:///flink/checkpoints/prod/user-behavior-job
  savepoint-dir: hdfs:///flink/savepoints/prod/user-behavior-job

state:
  backend: rocksdb
  incremental-checkpoint: true
  ttl-hours: 24

kafka:
  bootstrap-servers: kafka-prod-01:9092,kafka-prod-02:9092
  source-topic: user_behavior_log
  sink-topic: user_behavior_clean
  group-id: user-behavior-job
```

关键技术点建议：

1. 时间语义优先使用 Event Time，而不是 Processing Time。
2. Watermark 延迟要基于真实乱序分布设置。
3. 状态必须控制大小，长期状态必须配置 TTL。
4. Checkpoint 必须稳定成功，连续失败应视为生产故障。
5. Sink 必须明确写入语义，不能模糊承诺 Exactly Once。
6. 端到端一致性依赖 Source、Flink、Sink 和业务幂等共同设计。
7. 生产作业必须有 Savepoint 升级和回滚能力。

### 常见风险

Flink 作业常见风险主要来自数据质量、时间语义、状态膨胀、Checkpoint 不稳定、外部系统异常、依赖冲突、版本升级、资源不足和运维误操作。很多风险不是代码编译阶段能发现的，必须通过测试、压测、监控和发布流程进行控制。

常见风险清单：

| 风险                        | 表现                                          | 影响           |
| --------------------------- | --------------------------------------------- | -------------- |
| Kafka Lag 持续上涨          | 消费速度低于生产速度                          | 数据延迟、积压 |
| Watermark 不推进            | 窗口不触发                                    | 指标不输出     |
| 状态无限增长                | Checkpoint 变慢、OOM                          | 作业不稳定     |
| Checkpoint 连续失败         | 无法可靠恢复                                  | 容错能力失效   |
| Sink 不幂等                 | 故障恢复后重复写入                            | 数据重复       |
| Source 不可重放             | 故障后无法补偿                                | 数据丢失       |
| 数据倾斜                    | 部分 Subtask 高负载                           | 反压、延迟     |
| 维表查询慢                  | Async 超时、反压                              | 宽表延迟       |
| Connector 版本不兼容        | 启动失败或运行异常                            | 发布失败       |
| 依赖冲突                    | `NoSuchMethodError`、`ClassNotFoundException` | 作业失败       |
| 算子 UID 变化               | Savepoint 恢复失败                            | 无法升级回滚   |
| Key 类型变化                | 状态无法兼容                                  | 恢复失败       |
| 脏数据未落盘                | 异常数据丢失                                  | 无法修复       |
| 补数无幂等                  | 重复覆盖或重复累加                            | 数据错误       |
| 资源不足                    | OOM、反压、延迟                               | 作业不稳定     |
| 密钥泄露                    | 日志或配置暴露密码                            | 安全风险       |
| 误删 Checkpoint / Savepoint | 无法恢复                                      | 运维事故       |

高风险变更清单：

| 变更类型            | 风险等级 | 处理建议                             |
| ------------------- | -------- | ------------------------------------ |
| 修改 Key 类型       | 极高     | 避免直接修改，必须重新设计状态迁移   |
| 修改状态字段类型    | 高       | 测试 Savepoint 恢复                  |
| 修改算子 `uid()`    | 高       | 禁止随意修改                         |
| 删除有状态算子      | 高       | 明确是否使用 `allowNonRestoredState` |
| Connector 升级      | 高       | 做端到端测试                         |
| Flink 大版本升级    | 高       | 做完整回归和 Savepoint 恢复          |
| Sink 语义调整       | 高       | 验证重复、丢失和幂等                 |
| Checkpoint 参数调整 | 中       | 测试环境压测                         |
| 并行度调整          | 中       | 验证状态重分布                       |
| 新增字段            | 中       | 验证 Schema 兼容                     |
| 日志级别调整        | 低       | 避免高频 DEBUG                       |

风险控制建议：

1. 生产作业必须保留发布前 Savepoint。
2. 所有有状态算子必须显式设置 `uid()`。
3. Sink 必须设计幂等键或事务语义。
4. 脏数据、迟到数据和失败数据必须可追溯。
5. 版本升级前必须执行 Savepoint 恢复测试。
6. 数据修复和补数必须先写临时表或补偿 Topic。
7. 高风险变更必须有审批、灰度和回滚方案。

### 后续优化方向

Flink 项目上线后仍需要持续优化。优化方向可以分为工程规范、性能稳定、数据质量、平台化能力、成本治理、安全合规和智能运维几个方面。后续优化不应只在故障后被动进行，而应通过指标和复盘持续推进。

推荐优化方向如下：

| 方向           | 目标                                             |
| -------------- | ------------------------------------------------ |
| 工程模板化     | 统一作业骨架、参数解析、配置加载、日志、指标     |
| Connector 封装 | 统一 Kafka、JDBC、Redis、HTTP、OLAP 写入规范     |
| 数据质量平台化 | 脏数据、迟到数据、校验结果统一管理               |
| 状态治理       | 状态大小、TTL、Savepoint 兼容性定期检查          |
| 监控体系完善   | 系统指标、业务指标、告警规则、看板标准化         |
| 自动化发布     | CI/CD、灰度、Savepoint、回滚流程自动化           |
| 补数平台化     | 标准补数参数、批次、校验和合并机制               |
| 成本优化       | 资源利用率、并行度、Slot、存储和下游写入成本优化 |
| 安全治理       | 认证、权限、脱敏、密钥、审计统一管理             |
| 数据血缘       | Source、作业、Sink、指标、表之间建立血缘关系     |
| 版本治理       | Flink、Connector、JDK、依赖版本统一升级管理      |

工程层优化：

```text
抽取公共 Job 模板
  -> 标准参数解析
  -> 标准 Checkpoint 配置
  -> 标准 Kafka Source/Sink 构建
  -> 标准脏数据处理
  -> 标准 Metrics 注册
  -> 标准发布脚本
```

性能层优化：

```text
分析 Flink Web UI
  -> 定位瓶颈算子
  -> 观察 Subtask 差异
  -> 优化 Key 分布
  -> 优化状态结构
  -> 优化 Sink 批量
  -> 调整资源和并行度
  -> 压测验证
```

数据质量层优化：

```text
输入输出对账
  -> 脏数据原因统计
  -> 迟到数据统计
  -> 维表命中率统计
  -> Sink 成功率统计
  -> 实时离线对账
  -> 自动生成校验报告
```

后续优化建议：

1. 建立统一 Flink 作业脚手架，减少重复代码。
2. 将参数解析、配置加载、Checkpoint、日志、指标封装成公共模块。
3. 建立统一脏数据 Topic、脏数据表和修复流程。
4. 建立标准 Grafana 看板和告警模板。
5. 将 Savepoint 发布、升级、回滚纳入自动化流程。
6. 对核心作业定期做数据回放、压测和故障演练。
7. 对资源利用率进行周期性评估，降低长期运行成本。
8. 对 Flink、Connector、JDK 和依赖版本建立统一升级计划。

至此，Flink Java 开发文档从基础知识、工程结构、依赖管理、作业开发、状态与容错、Connector 集成、性能优化、部署运维、安全升级、故障排查、代码规范、项目案例到生产交付已经形成完整闭环。生产级 Flink 项目的核心能力可以概括为四点：稳定处理实时数据，准确维护状态结果，可靠完成故障恢复，持续支撑版本演进。