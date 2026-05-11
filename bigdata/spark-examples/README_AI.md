# Spark

Spark 是面向大规模数据处理的分布式计算引擎，适用于离线批处理、交互式分析、机器学习特征加工、实时流处理以及湖仓数据加工等场景。在 Java 项目中使用 Spark 时，通常以 Spark SQL、DataFrame API、Structured Streaming 和外部数据源集成为主要开发方式，RDD 更多用于底层控制能力较强的特殊场景。

## 项目概述

本章节用于说明 Spark 项目的建设背景、目标范围、适用场景、技术路线以及项目边界，帮助开发、测试、运维和数据使用方统一对项目的理解。项目概述不展开具体代码实现，重点说明“为什么建设、解决什么问题、做到什么程度、不做什么”。

### 项目背景

随着业务数据规模持续增长，传统单机程序、普通数据库 SQL 或定时脚本在处理大批量数据时容易出现性能瓶颈、资源不可控、任务失败难恢复、数据链路难追踪等问题。尤其在日志分析、指标计算、宽表构建、离线同步、实时消费和数据清洗等场景中，单机计算模式已经难以满足稳定性、扩展性和时效性要求。

Spark 作为统一的大数据计算引擎，能够基于分布式计算能力对 HDFS、Hive、Kafka、JDBC、对象存储等多种数据源进行统一处理。相比传统 MapReduce，Spark 通过内存计算、DAG 调度和丰富的高级 API，能够显著提升批处理和交互式计算效率。

本项目以 Java 技术栈为主，围绕 Spark Core、Spark SQL、Structured Streaming 以及常见数据源集成能力，构建一套可开发、可调试、可提交、可监控、可运维的 Spark 开发工程规范，用于支撑企业级数据处理任务。

### 建设目标

本项目的核心目标是建立一套标准化的 Spark Java 开发体系，使开发人员能够按照统一的工程结构、配置方式、参数规范、日志规范和任务提交规范完成 Spark 任务开发。

项目建设目标包括以下几个方面：

| 目标           | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| 统一工程结构   | 规范 Maven 或 Gradle 工程目录、配置目录、脚本目录、日志目录和测试目录 |
| 降低开发成本   | 提供通用 SparkSession 初始化、参数解析、数据读取、数据写出和异常处理方式 |
| 提升任务稳定性 | 通过合理的资源配置、日志记录、异常处理、数据校验和任务重试机制降低线上故障 |
| 支持多数据源   | 支持 Hive、HDFS、Kafka、JDBC、Parquet、ORC、JSON、CSV 等常见数据源 |
| 适配多运行环境 | 支持本地开发、测试环境、生产环境，以及 Standalone、YARN、Kubernetes 等运行模式 |
| 便于运维排查   | 通过标准日志、Spark UI、History Server、YARN UI 和任务指标辅助问题定位 |
| 支持批流一体   | 既支持离线批处理任务，也支持 Structured Streaming 实时任务开发 |

最终目标不是简单完成单个 Spark 任务，而是沉淀一套可复用的 Spark 开发基础能力，使后续任务能够快速接入并保持一致的开发质量。

### 适用场景

本项目适用于需要进行大规模数据处理、数据清洗、指标计算、数据同步和实时计算的业务场景。对于数据量较小、逻辑简单、单机程序即可稳定完成的任务，不建议过度引入 Spark。

常见适用场景如下：

| 场景          | 说明                                                         |
| ------------- | ------------------------------------------------------------ |
| 离线批处理    | 按天、按小时或按批次处理历史数据，例如日志清洗、订单汇总、用户行为分析 |
| 数据清洗      | 对原始数据进行空值处理、异常值过滤、字段标准化、格式转换和重复数据去重 |
| 宽表构建      | 将事实表、维度表、明细表进行 Join 和聚合，形成面向分析的宽表 |
| 指标计算      | 计算 UV、PV、GMV、转化率、留存率、活跃用户数等业务指标       |
| Hive 数据加工 | 读取 Hive 表并写入分区表，支撑数仓 ODS、DWD、DWS、ADS 等分层建设 |
| Kafka 流处理  | 消费 Kafka 实时数据，进行实时清洗、聚合、告警或写入下游系统  |
| JDBC 数据同步 | 从 MySQL、PostgreSQL、Oracle、SQL Server 等数据库读取或写入数据 |
| 文件格式转换  | 在 CSV、JSON、Parquet、ORC 等格式之间进行批量转换            |
| 数据质量校验  | 对数据完整性、唯一性、准确性、一致性和时效性进行校验         |
| 补数和重跑    | 对历史任务失败、数据延迟或业务口径变更后的数据进行重新计算   |

不适合使用 Spark 的场景包括：毫秒级低延迟在线接口、事务型业务系统、数据量极小的简单定时任务、强一致事务处理、复杂 OLTP 查询等。

### 技术选型

本项目以 Spark 作为核心计算引擎，以 Java 作为主要开发语言，并结合 Hadoop、Hive、Kafka、JDBC 等组件完成数据读取、转换和写出。

推荐技术选型如下：

| 技术                 | 作用                                                         |
| -------------------- | ------------------------------------------------------------ |
| Java                 | 项目主要开发语言，适合企业后端团队统一技术栈                 |
| Spark Core           | 提供底层分布式计算能力，支持 RDD、任务调度、缓存和分区控制   |
| Spark SQL            | 提供 DataFrame、Dataset、SQL 查询、Catalog、函数和执行计划优化能力 |
| Structured Streaming | 提供流式计算能力，适合 Kafka 等实时数据源处理                |
| Hadoop Client        | 支持 HDFS 文件读取、写入和集群资源访问                       |
| Hive Support         | 支持 Hive Metastore、Hive 表读取、分区表写入和 Hive SQL 兼容 |
| Kafka Connector      | 支持 Kafka 流式读取和写入                                    |
| JDBC Connector       | 支持关系型数据库读取和写入                                   |
| Maven 或 Gradle      | 用于项目依赖管理、构建和打包                                 |
| Logback 或 Log4j2    | 用于应用日志输出和问题排查                                   |
| YARN 或 Kubernetes   | 用于生产环境资源调度和任务运行                               |
| Spark History Server | 用于查看历史任务执行情况、Stage、SQL、Executor 和 Shuffle 信息 |

在 Java 开发中，优先使用 SparkSession、Dataset、DataFrame API 和 Spark SQL 完成业务开发。RDD API 保留用于需要精细控制分区、底层转换或非结构化数据处理的场景。

### 项目边界

本项目聚焦 Spark 数据处理任务的工程化开发，不覆盖完整数据平台、调度平台、数据治理平台和实时 OLAP 系统的全部能力。

项目包含以下边界内能力：

| 范围                      | 说明                                                         |
| ------------------------- | ------------------------------------------------------------ |
| Spark 批处理开发          | 支持基于文件、Hive、JDBC 等数据源的离线任务开发              |
| Spark SQL 开发            | 支持 SQL 查询、临时视图、函数、Join、聚合和结果写出          |
| Structured Streaming 开发 | 支持 Kafka 等流式数据源的实时任务开发                        |
| 数据源集成                | 支持 HDFS、Hive、Kafka、JDBC、对象存储等常见数据源           |
| 工程规范                  | 支持统一目录、依赖、配置、参数、日志、异常和测试规范         |
| 打包提交                  | 支持 Fat Jar 构建、依赖排除、spark-submit 参数和运行模式配置 |
| 运维排查                  | 支持日志、Spark UI、History Server、YARN UI 和常见问题定位   |

项目不包含以下边界外能力：

| 不包含范围       | 说明                                                         |
| ---------------- | ------------------------------------------------------------ |
| 业务系统在线接口 | 不负责提供高并发低延迟的 Web API 服务                        |
| 数据仓库完整建模 | 不覆盖 ODS、DWD、DWS、ADS 的完整建模方法论，只提供 Spark 实现能力 |
| 调度平台建设     | 不实现 DolphinScheduler、Airflow、Azkaban 等调度系统本身     |
| 集群安装部署     | 不负责 Hadoop、Hive、Spark、Kafka 集群的底层安装和维护       |
| 数据权限平台     | 不实现统一权限中心、数据目录、血缘系统和审计平台             |
| 实时 OLAP 查询   | 不替代 ClickHouse、Doris、StarRocks、Elasticsearch 等查询引擎 |

## Spark 基础认知

本章节用于建立 Spark 开发所需的基础概念，包括核心模型、运行架构、Driver、Executor、RDD、DataFrame、Dataset、Transformation、Action、Spark SQL 以及 Spark 流处理能力。掌握这些内容后，开发人员可以更准确地理解任务执行过程、性能瓶颈来源和常见异常原因。

### Spark 核心概念

Spark 的核心是将大规模数据处理任务拆分为多个可并行执行的计算单元，并调度到集群中的多个节点上执行。开发人员编写的是逻辑计算流程，Spark 负责将流程转换为 DAG，然后拆分为 Stage 和 Task，在集群中并行执行。

核心概念包括：

| 概念         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| Application  | 一个 Spark 应用程序，对应一次 Spark 任务提交                 |
| Driver       | 运行 main 方法的进程，负责创建 SparkContext、生成执行计划和调度任务 |
| Executor     | 运行在 Worker 节点上的执行进程，负责执行 Task、缓存数据和返回结果 |
| SparkContext | Spark Core 的入口对象，负责连接集群和调度底层任务            |
| SparkSession | Spark SQL 的统一入口，封装 SparkContext、SQLContext 和 HiveContext 能力 |
| Job          | 每触发一次 Action，通常会生成一个 Job                        |
| Stage        | Job 按照 Shuffle 边界拆分后的执行阶段                        |
| Task         | Stage 中最小的执行单元，通常对应一个分区上的计算             |
| Partition    | 数据分区，是 Spark 并行计算的基本数据单位                    |
| DAG          | 有向无环图，用于描述任务之间的依赖关系                       |
| Shuffle      | 跨分区、跨节点的数据重分布过程，常见于 Join、GroupBy、Distinct 等操作 |

开发 Spark 程序时，需要重点关注分区数量、Shuffle 次数、Executor 资源、Driver 内存、数据倾斜和缓存使用，这些因素直接影响任务性能和稳定性。

### Spark 运行架构

Spark 运行架构由 Driver、Cluster Manager、Worker Node 和 Executor 组成。Driver 负责应用程序控制和任务调度，Cluster Manager 负责资源申请和分配，Executor 负责具体计算。

整体执行流程如下：

1. 用户通过 `spark-submit` 提交 Spark 应用。
2. Spark 启动 Driver 进程，执行应用程序入口逻辑。
3. Driver 创建 SparkContext 或 SparkSession，并向集群资源管理器申请资源。
4. Cluster Manager 在集群节点上启动 Executor。
5. Driver 根据代码中的 Transformation 和 Action 生成 DAG。
6. DAG Scheduler 将 Job 拆分为多个 Stage。
7. Task Scheduler 将 Stage 拆分为多个 Task，并分发给 Executor 执行。
8. Executor 执行 Task，读取数据、计算数据、缓存数据或写出结果。
9. Driver 汇总任务状态，并在任务完成后释放资源。

Spark 支持多种运行模式：

| 运行模式     | 说明                                               |
| ------------ | -------------------------------------------------- |
| Local        | 本地模式，适合开发调试和小规模验证                 |
| Standalone   | Spark 自带集群模式，部署简单                       |
| YARN Client  | Driver 运行在提交任务的客户端，适合调试            |
| YARN Cluster | Driver 运行在 YARN 集群中，适合生产任务            |
| Kubernetes   | Driver 和 Executor 以 Pod 形式运行，适合云原生环境 |

生产环境通常优先使用 YARN Cluster 或 Kubernetes 模式，避免客户端断开导致任务中断。

### Driver 与 Executor

Driver 和 Executor 是 Spark 应用运行时最重要的两个角色。理解二者职责边界，有助于排查 Driver 内存溢出、Executor 内存溢出、Task 失败、序列化异常和网络通信异常等问题。

Driver 的主要职责包括：

| 职责         | 说明                                           |
| ------------ | ---------------------------------------------- |
| 执行主程序   | 运行 main 方法，解析参数，创建 SparkSession    |
| 生成执行计划 | 根据 Transformation 和 Action 构建 DAG         |
| 任务调度     | 将 Job 拆分为 Stage 和 Task，并分发给 Executor |
| 状态管理     | 跟踪 Task 执行状态、失败重试和 Stage 进度      |
| 结果收集     | 接收 Executor 返回的部分结果或元信息           |

Executor 的主要职责包括：

| 职责         | 说明                                  |
| ------------ | ------------------------------------- |
| 执行 Task    | 在具体分区上执行计算逻辑              |
| 数据缓存     | 缓存 RDD、DataFrame 或中间计算结果    |
| Shuffle 读写 | 处理 Shuffle 中间文件和网络传输       |
| 返回结果     | 将任务执行状态和部分结果返回给 Driver |
| 资源隔离     | 使用独立 JVM 进程承载计算任务         |

常见注意事项：

| 问题                         | 说明                                                         |
| ---------------------------- | ------------------------------------------------------------ |
| 避免 collect 大数据          | `collect()` 会将数据拉回 Driver，数据量大时容易导致 Driver OOM |
| 避免在 Driver 中处理大量数据 | 大规模数据处理应交给 Executor 并行执行                       |
| 控制 Executor 内存           | Join、GroupBy、Sort、Cache 等操作容易造成 Executor 内存压力  |
| 注意闭包序列化               | 在算子中引用外部对象时，外部对象需要可序列化                 |
| 合理配置并行度               | Task 数量过少会导致资源利用不足，过多会增加调度开销          |

在 Java 开发中，应尽量将业务计算逻辑放在 Spark 算子、SQL 或 DataFrame 转换中执行，而不是先将数据收集到 Driver 后再使用普通 Java 集合处理。

### RDD、DataFrame 与 Dataset

Spark 提供 RDD、DataFrame 和 Dataset 三种主要数据抽象。三者都可以表达分布式数据集，但使用方式、优化能力和类型安全程度不同。

| 数据抽象  | 特点                                             | 适用场景                                         |
| --------- | ------------------------------------------------ | ------------------------------------------------ |
| RDD       | 最底层的分布式数据抽象，强调函数式转换和分区控制 | 非结构化数据、底层转换、精细控制分区             |
| DataFrame | 带 Schema 的分布式表结构，本质是 `Dataset<Row>`  | SQL 查询、结构化数据处理、Hive/JDBC/Parquet 加工 |
| Dataset   | 强类型分布式数据集，兼具类型安全和优化能力       | Scala 场景更常用，Java 中使用相对繁琐            |

RDD 的优势是控制能力强，可以直接操作分区、缓存、Pair RDD 和自定义转换逻辑；缺点是缺少 Catalyst 优化器支持，开发复杂度较高。

DataFrame 的优势是结构清晰、API 简洁、支持 Spark SQL 优化、适合处理结构化数据；缺点是字段通过字符串或 Row 访问，编译期类型检查较弱。

Dataset 在 Scala 中体验较好，在 Java 中需要定义 Bean Encoder，代码相对繁琐。Java 项目中通常以 `Dataset<Row>` 作为主要开发对象，也就是常说的 DataFrame。

推荐使用策略如下：

| 场景                   | 推荐方式                              |
| ---------------------- | ------------------------------------- |
| Hive 表处理            | DataFrame / Spark SQL                 |
| Parquet / ORC 文件处理 | DataFrame                             |
| CSV / JSON 清洗        | DataFrame                             |
| Kafka 流处理           | Structured Streaming DataFrame        |
| 复杂 SQL 指标计算      | Spark SQL                             |
| 非结构化文本处理       | RDD 或 DataFrame                      |
| 需要精确控制分区       | RDD 或 DataFrame repartition/coalesce |
| Java 企业项目常规开发  | DataFrame + Spark SQL                 |

### Transformation 与 Action

Spark 计算操作分为 Transformation 和 Action。Transformation 用于描述数据转换逻辑，Action 用于触发真正执行。Spark 采用惰性计算机制，只有遇到 Action 时，前面的 Transformation 才会被统一转换为执行计划并提交执行。

Transformation 常见操作包括：

| 操作        | 说明                         |
| ----------- | ---------------------------- |
| map         | 对每条数据进行一对一转换     |
| flatMap     | 一条数据转换为多条数据       |
| filter      | 按条件过滤数据               |
| select      | 选择 DataFrame 中的指定字段  |
| withColumn  | 新增或替换字段               |
| groupBy     | 按字段分组                   |
| join        | 多数据集关联                 |
| distinct    | 去重                         |
| repartition | 重新分区，通常会产生 Shuffle |
| coalesce    | 减少分区，通常用于小文件合并 |
| orderBy     | 全局排序，通常会产生 Shuffle |

Action 常见操作包括：

| 操作           | 说明                                       |
| -------------- | ------------------------------------------ |
| count          | 统计数据条数                               |
| show           | 打印部分数据，常用于调试                   |
| collect        | 将数据拉取到 Driver，需要谨慎使用          |
| take           | 获取前 N 条数据                            |
| first          | 获取第一条数据                             |
| foreach        | 对每条数据执行外部操作                     |
| write          | 将结果写出到文件、Hive、JDBC、Kafka 等目标 |
| saveAsTextFile | RDD 写出文本文件                           |

惰性计算的优势是 Spark 可以在真正执行前对整体逻辑进行优化，例如谓词下推、列裁剪、Join 策略选择和物理执行计划优化。

开发时需要注意以下问题：

| 注意点                      | 说明                                                 |
| --------------------------- | ---------------------------------------------------- |
| Transformation 不会立即执行 | 只定义计算逻辑，不触发任务                           |
| Action 会触发 Job           | 每个 Action 都可能产生一次完整任务执行               |
| 避免重复 Action             | 多次 `count()`、`show()`、`write()` 可能导致重复计算 |
| 合理使用 cache              | 多次复用的数据集可以缓存，避免重复计算               |
| 谨慎使用 collect            | 大数据量 collect 容易造成 Driver 内存溢出            |

### Spark SQL

Spark SQL 是 Spark 处理结构化数据的核心模块，支持 SQL 查询、DataFrame API、Dataset API、临时视图、全局临时视图、Hive 表访问和多种数据源读写。对于 Java Spark 项目，Spark SQL 通常是最主要的开发方式。

Spark SQL 的核心能力包括：

| 能力              | 说明                                                    |
| ----------------- | ------------------------------------------------------- |
| SQL 查询          | 支持使用 SQL 处理结构化数据                             |
| DataFrame API     | 支持通过 Java API 完成字段选择、过滤、聚合、Join 等操作 |
| Catalyst 优化器   | 对逻辑计划和物理计划进行优化                            |
| Tungsten 执行引擎 | 优化内存管理和二进制执行效率                            |
| Hive 集成         | 支持 Hive Metastore、Hive 表和部分 Hive SQL 语法        |
| 数据源统一访问    | 支持 Parquet、ORC、JSON、CSV、JDBC、Kafka 等数据源      |
| UDF / UDAF        | 支持自定义函数和自定义聚合函数                          |
| 执行计划分析      | 支持通过 `explain()` 查看逻辑计划和物理计划             |

Spark SQL 常见开发流程如下：

1. 创建 SparkSession。
2. 读取文件、Hive 表、JDBC 表或 Kafka 数据。
3. 注册临时视图或直接使用 DataFrame API。
4. 编写 SQL 或链式转换逻辑。
5. 对结果进行字段处理、Join、聚合、排序或分区。
6. 将结果写出到 Hive、HDFS、对象存储、JDBC 或 Kafka。
7. 通过日志、数据量和执行计划验证任务结果。

Spark SQL 适合表达结构化计算逻辑，尤其适合数据清洗、维表关联、指标统计、宽表加工和分区写入等场景。开发中应优先使用 Spark SQL 和 DataFrame API，只有在 SQL 难以表达或需要底层控制时，再考虑使用 RDD。

### Spark Streaming 与 Structured Streaming

Spark 提供两类流处理能力：早期的 Spark Streaming 和较新的 Structured Streaming。当前项目开发中应优先使用 Structured Streaming。

Spark Streaming 基于 DStream 模型，将实时数据按照固定时间间隔切分成一批批小数据集，本质是微批处理。它适合早期 Spark 版本中的实时处理场景，但 API 和 Spark SQL 生态结合较弱。

Structured Streaming 基于 Spark SQL 引擎，将实时数据流抽象为不断增长的无界表。开发人员可以像处理静态 DataFrame 一样处理流式 DataFrame，并通过输出模式、Checkpoint、Watermark 和状态管理完成实时计算。

两者对比如下：

| 对比项     | Spark Streaming                      | Structured Streaming               |
| ---------- | ------------------------------------ | ---------------------------------- |
| 核心抽象   | DStream                              | Streaming DataFrame / Dataset      |
| 编程模型   | RDD 风格微批                         | SQL / DataFrame 风格               |
| 优化能力   | 相对有限                             | 可使用 Catalyst 优化器             |
| Kafka 集成 | 支持，但开发方式较旧                 | 推荐方式，集成更自然               |
| 状态管理   | 需要基于 updateStateByKey 等方式处理 | 支持状态算子、Watermark 和窗口聚合 |
| 使用建议   | 维护旧任务                           | 新任务优先使用                     |

Structured Streaming 常见概念包括：

| 概念         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| Source       | 流式数据源，例如 Kafka、文件目录、Rate Source                |
| Sink         | 输出目标，例如 Kafka、Console、Memory、File、ForeachBatch    |
| Output Mode  | 输出模式，包括 Append、Update、Complete                      |
| Trigger      | 触发策略，控制流任务执行频率                                 |
| Checkpoint   | 保存 Offset、状态和进度，用于故障恢复                        |
| Watermark    | 处理乱序和迟到数据的时间边界                                 |
| State Store  | 保存有状态计算的中间状态                                     |
| ForeachBatch | 将每个微批次当作普通 DataFrame 处理，常用于写入 JDBC 或 Hive |

Structured Streaming 适合 Kafka 实时消费、实时清洗、实时指标聚合、实时宽表更新、实时告警和流批一体处理。生产环境中必须配置稳定的 Checkpoint 路径，并关注 Kafka Offset、状态数据大小、迟到数据、任务重启和下游写入幂等性。

## 开发环境准备

本章节用于说明 Spark Java 项目在本地开发、测试和集群提交前需要准备的基础环境。环境准备的重点不是单独安装某一个组件，而是保证 JDK、构建工具、Spark、Hadoop、Hive、IDE 和本地调试参数之间版本匹配，避免开发阶段就出现依赖冲突、类加载失败和运行模式不一致的问题。

### JDK 环境

Spark Java 项目必须先准备 JDK 环境。JDK 版本需要与 Spark 版本、Hadoop 版本以及项目依赖保持兼容。生产环境中应优先以集群已安装的 JDK 版本为准，本地开发环境应尽量与生产环境一致。

推荐使用 JDK 8、JDK 11 或 JDK 17，具体版本由 Spark 集群版本决定。不要只在本地使用较新的 JDK，而生产集群仍使用较旧 JDK，否则容易出现编译通过但集群运行失败的问题。

Linux 环境中可以通过以下命令检查 JDK：

```bash
# 查看 Java 运行版本
java -version

# 查看 Java 编译器版本
javac -version

# 查看 JAVA_HOME
echo $JAVA_HOME
```

如果需要手动配置 JDK 环境变量，可以在 `/etc/profile` 或用户目录下的 `~/.bash_profile` 中增加以下配置：

```bash
# JDK 安装目录，根据实际路径调整
export JAVA_HOME=/opt/module/jdk

# 将 Java 命令加入 PATH
export PATH=$JAVA_HOME/bin:$PATH
```

配置完成后执行：

```bash
# 重新加载环境变量
source /etc/profile

# 验证 Java 环境
java -version
javac -version
```

本地开发时需要重点确认三点：`JAVA_HOME` 指向正确目录，IDE 使用的 Project SDK 与命令行 JDK 一致，Maven 或 Gradle 编译版本与运行环境一致。

Maven 项目中可以显式指定编译版本：

```xml
<!-- Java 编译版本配置，需与 Spark 集群运行 JDK 保持兼容 -->
<properties>
    <maven.compiler.source>8</maven.compiler.source>
    <maven.compiler.target>8</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

Gradle 项目中可以这样配置：

```groovy
// Java 编译版本配置，需与集群运行环境保持一致
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
```

### Scala 环境

虽然本项目以 Java 开发为主，但 Spark 本身由 Scala 编写，并且 Spark 依赖包通常带有 Scala 二进制版本后缀，例如 `spark-sql_2.12`。因此，Java 项目也需要关注 Scala 版本匹配问题。

Java 开发者通常不需要单独编写 Scala 代码，但必须保证 Spark 依赖中的 Scala 后缀与集群 Spark 版本一致。常见依赖格式如下：

```xml
<!-- Spark SQL 依赖，artifactId 中的 _2.12 表示 Scala 二进制版本 -->
<dependency>
    <groupId>org.apache.spark</groupId>
    <artifactId>spark-sql_2.12</artifactId>
    <version>${spark.version}</version>
    <scope>provided</scope>
</dependency>
```

这里的 `_2.12` 不能随意修改。如果集群 Spark 使用 Scala 2.12 编译，则项目依赖也应使用 `_2.12`；如果集群使用 Scala 2.13，则应使用 `_2.13`。Scala 二进制版本不一致时，常见异常包括 `NoSuchMethodError`、`ClassNotFoundException`、`NoClassDefFoundError` 和隐式依赖冲突。

可以通过以下方式查看 Spark 对应的 Scala 版本：

```bash
# 查看 Spark Shell 启动信息，通常会显示 Scala 版本
spark-shell --version

# 查看 Spark Submit 版本信息
spark-submit --version
```

如果项目只使用 Java API，不建议额外引入 Scala 标准库版本，优先由 Spark 相关依赖传递管理。确实需要显式声明时，应与 Spark 依赖保持一致。

### Maven 或 Gradle 环境

Spark Java 项目可以使用 Maven 或 Gradle 管理依赖和构建产物。企业 Java 项目中 Maven 使用更普遍，Gradle 在多模块、构建性能和灵活配置方面更有优势。项目应二选一作为主构建工具，不建议同时维护两套构建逻辑。

Maven 环境检查命令如下：

```bash
# 查看 Maven 版本
mvn -version

# 查看本地仓库路径
mvn help:evaluate -Dexpression=settings.localRepository -q -DforceStdout
```

Maven 推荐配置基础属性：

```xml
<!-- 项目基础版本属性，统一管理 Spark、Hadoop、Hive 等依赖版本 -->
<properties>
    <java.version>8</java.version>
    <spark.version>3.5.0</spark.version>
    <hadoop.version>3.3.6</hadoop.version>
    <hive.version>3.1.3</hive.version>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

Gradle 环境检查命令如下：

```bash
# 查看 Gradle 版本
gradle -version

# 如果项目使用 Gradle Wrapper，优先使用 wrapper
./gradlew -version
```

Gradle 推荐使用 Wrapper 固定构建版本：

```bash
# 生成 Gradle Wrapper，版本根据团队规范调整
gradle wrapper --gradle-version 8.5
```

Maven 和 Gradle 的选择建议如下：

| 构建工具             | 适用场景                                   | 建议                         |
| -------------------- | ------------------------------------------ | ---------------------------- |
| Maven                | 团队 Java 项目标准化程度高，依赖管理稳定   | 推荐作为默认选择             |
| Gradle               | 多模块复杂、构建脚本灵活、需要更快构建速度 | 适合有 Gradle 使用经验的团队 |
| Maven Shade Plugin   | 需要构建 Fat Jar 并处理依赖冲突            | Spark 项目常用               |
| Gradle Shadow Plugin | Gradle 项目构建 Fat Jar                    | Gradle 项目常用              |

Spark 项目中依赖作用域非常重要。集群已提供的 Spark、Hadoop、Hive 相关依赖通常应设置为 `provided`，避免打包进 Fat Jar 后与集群运行时依赖冲突。

### Spark 本地环境

Spark 本地环境用于本机调试、样例数据验证、SQL 逻辑验证和小规模任务运行。本地环境不用于替代生产集群，只用于提高开发效率。

本地安装 Spark 后，需要配置 `SPARK_HOME`：

```bash
# Spark 安装目录，根据实际路径调整
export SPARK_HOME=/opt/module/spark

# 将 Spark 命令加入 PATH
export PATH=$SPARK_HOME/bin:$SPARK_HOME/sbin:$PATH
```

验证 Spark 环境：

```bash
# 查看 Spark Submit 版本
spark-submit --version

# 启动 Spark SQL 客户端
spark-sql --version

# 启动 Spark Shell 验证本地环境
spark-shell --master local[*]
```

本地模式常用配置如下：

```bash
# 使用所有本地 CPU 核心运行
--master local[*]

# 使用 2 个本地线程运行，适合复现并行度问题
--master local[2]

# 使用 1 个本地线程运行，适合排查顺序执行逻辑
--master local[1]
```

本地 Spark 目录中常见文件如下：

```bash
$SPARK_HOME
├── bin                 # spark-submit、spark-shell、spark-sql 等命令
├── conf                # spark-defaults.conf、log4j2.properties 等配置
├── jars                # Spark 运行依赖包
├── sbin                # Spark 服务启动脚本
├── examples            # 官方示例
└── work                # 本地运行产生的工作目录
```

本地开发时可以在项目中配置 `local[*]`，但生产提交时必须通过参数切换为 YARN、Standalone 或 Kubernetes 模式，避免将本地调试配置误提交到生产环境。

### Hadoop 环境

Spark 读取 HDFS、提交 YARN 任务或访问 Hadoop 生态组件时，需要 Hadoop 客户端环境。即使 Spark 程序本身不直接调用 Hadoop API，只要涉及 HDFS、YARN、Hive 或 Kerberos，通常都需要正确配置 Hadoop 环境。

基础环境变量如下：

```bash
# Hadoop 安装目录，根据实际路径调整
export HADOOP_HOME=/opt/module/hadoop

# Hadoop 配置文件目录，生产环境应使用集群提供的配置
export HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop

# 将 Hadoop 命令加入 PATH
export PATH=$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$PATH
```

常见 Hadoop 配置文件包括：

```bash
$HADOOP_CONF_DIR
├── core-site.xml       # HDFS 地址、默认文件系统、安全认证等配置
├── hdfs-site.xml       # HDFS 副本数、NameNode、DataNode 等配置
├── yarn-site.xml       # YARN ResourceManager、队列、调度等配置
├── mapred-site.xml     # MapReduce 和部分历史兼容配置
└── capacity-scheduler.xml # YARN 队列调度配置，部分环境存在
```

验证 Hadoop 环境：

```bash
# 查看 HDFS 根目录
hdfs dfs -ls /

# 查看当前用户 HDFS 目录
hdfs dfs -ls /user/$(whoami)

# 查看 YARN 应用列表
yarn application -list
```

如果在 Windows 本地开发 Spark 并访问 HDFS，需要额外处理 Hadoop native 库和 `winutils.exe`，但生产部署不应依赖 Windows 本地环境。建议本地开发优先使用 Linux、macOS 或容器环境，减少环境差异。

### Hive 环境

Spark 集成 Hive 时，需要 Hive Metastore、Hive 配置文件以及相关依赖支持。项目中如果需要读取 Hive 表、写入 Hive 分区表或使用 Hive Catalog，应在 SparkSession 中开启 Hive 支持。

Hive 环境通常依赖以下配置文件：

```bash
$HIVE_CONF_DIR
├── hive-site.xml       # Hive Metastore、Warehouse、权限、安全等配置
```

常见环境变量如下：

```bash
# Hive 安装目录，根据实际路径调整
export HIVE_HOME=/opt/module/hive

# Hive 配置文件目录
export HIVE_CONF_DIR=$HIVE_HOME/conf

# 将 Hive 命令加入 PATH
export PATH=$HIVE_HOME/bin:$PATH
```

验证 Hive 环境：

```bash
# 查看 Hive 版本
hive --version

# 连接 Hive 并查看数据库
hive -e "show databases;"

# 使用 Spark SQL 查看数据库，验证 Spark 与 Hive 集成
spark-sql -e "show databases;"
```

Spark 代码中开启 Hive 支持时，应确保 `hive-site.xml` 位于 classpath 或提交参数指定的配置目录中：

```java
SparkSession spark = SparkSession.builder()
        .appName("spark-hive-job")
        .enableHiveSupport()
        .getOrCreate();
```

如果没有正确配置 Hive，常见问题包括找不到 Hive 表、连接不到 Metastore、无法写入分区、权限不足、Warehouse 路径异常等。

### IDE 配置

Spark Java 项目推荐使用 IntelliJ IDEA 进行开发。IDE 配置的重点是统一 JDK、Maven 或 Gradle、编码格式、运行参数和资源目录，保证 IDE 中运行结果与命令行运行结果一致。

推荐配置项如下：

| 配置项                | 建议                                                  |
| --------------------- | ----------------------------------------------------- |
| Project SDK           | 与集群 JDK 保持一致                                   |
| Language Level        | 与 Maven 或 Gradle 编译版本一致                       |
| Maven Home            | 使用本机统一 Maven 或项目 Wrapper                     |
| Gradle JVM            | 与项目 JDK 一致                                       |
| File Encoding         | 统一使用 UTF-8                                        |
| Annotation Processing | 如果使用 Lombok，需要开启                             |
| Resource Directory    | 确认 `src/main/resources` 被标记为 Resources Root     |
| Test Directory        | 确认 `src/test/java` 和 `src/test/resources` 配置正确 |

IDE 中建议创建本地运行配置，指定以下内容：

```text
Main class: io.github.atengk.spark.Application
VM options: -Dspark.master=local[*] -Dlogback.configurationFile=src/main/resources/logback.xml
Program arguments: --env local --bizDate 2026-05-11
Working directory: 项目根目录
Use classpath of module: spark-job-app
```

如果项目依赖 Hive 或 Hadoop 配置，可以在 VM options 中指定配置目录：

```text
-DHADOOP_CONF_DIR=/opt/module/hadoop/etc/hadoop
-DHIVE_CONF_DIR=/opt/module/hive/conf
```

IDE 中不要直接使用生产路径、生产队列和生产数据库连接信息。敏感配置应通过本地配置文件、环境变量或启动参数传入。

### 本地调试配置

本地调试用于验证代码逻辑、SQL 语法、字段转换、数据写出和异常处理。为了保证调试过程可控，本地调试应使用小样例数据、临时输出目录和本地运行模式。

推荐在 `src/main/resources/application-local.yml` 中维护本地配置：

```yaml
# 本地开发环境配置，只用于 IDE 或 local 模式调试
spark:
  app-name: spark-local-debug
  master: local[*]
  shuffle-partitions: 4

data:
  # 本地输入路径，使用小样例数据
  input-path: data/input/user.json
  # 本地输出路径，避免覆盖生产数据
  output-path: data/output/user_summary

runtime:
  # 业务日期，调试时可手动指定
  biz-date: 2026-05-11
  env: local
```

推荐准备本地样例数据目录：

```bash
project-root
├── data
│   ├── input
│   │   ├── user.json
│   │   ├── order.csv
│   │   └── event.json
│   └── output
└── src
```

本地调试时可以使用以下命令运行 Fat Jar：

```bash
# 在本地模式运行 Spark 任务
spark-submit \
  --class io.github.atengk.spark.Application \
  --master local[*] \
  --conf spark.sql.shuffle.partitions=4 \
  target/spark-job.jar \
  --env local \
  --bizDate 2026-05-11
```

参数说明：

| 参数                           | 说明                                       |
| ------------------------------ | ------------------------------------------ |
| `--class`                      | 指定 Spark 程序入口类                      |
| `--master local[*]`            | 使用本地模式运行，`*` 表示使用本机全部核心 |
| `spark.sql.shuffle.partitions` | 控制 Shuffle 分区数，本地调试不宜过大      |
| `--env local`                  | 指定当前运行环境                           |
| `--bizDate`                    | 指定业务处理日期                           |

本地调试时建议遵守以下规则：

| 规则              | 说明                                               |
| ----------------- | -------------------------------------------------- |
| 使用小数据集      | 避免本地调试读取大量生产数据                       |
| 输出到临时目录    | 避免覆盖正式目录                                   |
| 降低 Shuffle 分区 | 本地调试设置为 2 到 8 即可                         |
| 控制日志级别      | 开发阶段可使用 INFO，排查问题时临时使用 DEBUG      |
| 不使用生产密钥    | 本地配置不得包含生产密码、Keytab、Token 等敏感信息 |
| 验证后再提交集群  | 本地通过后再使用 YARN 或 Kubernetes 模式验证       |

## 项目工程结构

本章节用于规范 Spark Java 项目的目录组织方式。良好的工程结构可以降低多人协作成本，使任务入口、配置文件、脚本、日志、测试和通用组件保持清晰边界。Spark 项目通常不是单纯的 Java 应用，而是包含任务代码、SQL 文件、配置文件、提交脚本和运维脚本的综合工程。

### Maven 项目结构

Maven 是 Spark Java 项目最常用的构建方式。单模块项目适合任务较少、结构简单的场景；多模块项目适合多个任务共享公共组件、统一配置和通用工具的场景。

推荐 Maven 单模块结构如下：

```bash
spark-job
├── pom.xml
├── README.md
├── data
│   ├── input                 # 本地调试输入样例
│   └── output                # 本地调试输出目录
├── scripts
│   ├── submit-local.sh       # 本地提交脚本
│   ├── submit-yarn.sh        # YARN 提交脚本
│   └── stop-job.sh           # 停止任务脚本
├── sql
│   ├── dwd_user_detail.sql   # 业务 SQL 文件
│   └── ads_user_summary.sql
├── logs                      # 本地运行日志目录
└── src
    ├── main
    │   ├── java
    │   │   └── io/github/atengk/spark
    │   │       ├── Application.java
    │   │       ├── config
    │   │       ├── job
    │   │       ├── reader
    │   │       ├── transformer
    │   │       ├── writer
    │   │       ├── service
    │   │       ├── exception
    │   │       └── util
    │   └── resources
    │       ├── application.yml
    │       ├── application-local.yml
    │       ├── application-test.yml
    │       ├── application-prod.yml
    │       └── logback.xml
    └── test
        ├── java
        └── resources
```

基础 `pom.xml` 可按以下方式组织：

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- Spark Java 项目基础坐标 -->
    <groupId>io.github.atengk</groupId>
    <artifactId>spark-job</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <!-- Java 编译版本需与集群 JDK 保持一致 -->
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <!-- 依赖版本需与集群组件版本匹配 -->
        <spark.version>3.5.0</spark.version>
        <hutool.version>5.8.26</hutool.version>
        <lombok.version>1.18.30</lombok.version>
    </properties>

    <dependencies>
        <!-- Spark SQL：提供 DataFrame、Dataset、Spark SQL 能力，集群通常已提供，使用 provided -->
        <dependency>
            <groupId>org.apache.spark</groupId>
            <artifactId>spark-sql_2.12</artifactId>
            <version>${spark.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- Hutool：用于参数处理、日期处理、字符串处理、文件处理等通用工具能力 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok：减少 Getter、Setter、构造方法等样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- 单元测试依赖 -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.1</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

Maven 项目中应将 Spark、Hadoop、Hive 等集群已存在依赖设置为 `provided`，将业务工具类、数据库驱动、JSON 工具等运行时确实需要的依赖打入 Jar。

### Gradle 项目结构

Gradle 项目结构与 Maven 类似，区别主要在构建脚本和依赖声明方式。Gradle 更适合复杂多模块工程，但团队需要统一 Gradle 版本和插件版本。

推荐 Gradle 单模块结构如下：

```bash
spark-job
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
├── gradle
│   └── wrapper
├── scripts
├── sql
├── data
├── logs
└── src
    ├── main
    │   ├── java
    │   └── resources
    └── test
        ├── java
        └── resources
```

基础 `build.gradle` 可按以下方式组织：

```groovy
plugins {
    // Java 项目插件
    id 'java'

    // 构建 Fat Jar，可按需启用
    id 'com.github.johnrengelman.shadow' version '8.1.1'
}

group = 'io.github.atengk'
version = '1.0.0'

java {
    // Java 编译版本需与集群运行环境一致
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    // 优先使用公司私服；没有私服时使用 Maven Central
    mavenCentral()
}

ext {
    // 组件版本需与集群环境保持一致
    sparkVersion = '3.5.0'
    hutoolVersion = '5.8.26'
    lombokVersion = '1.18.30'
}

dependencies {
    // Spark SQL：集群运行时通常已提供，使用 compileOnly 避免打入 Jar
    compileOnly "org.apache.spark:spark-sql_2.12:${sparkVersion}"

    // Hutool：通用工具类依赖，随应用一起打包
    implementation "cn.hutool:hutool-all:${hutoolVersion}"

    // Lombok：编译期生成代码，不需要进入运行包
    compileOnly "org.projectlombok:lombok:${lombokVersion}"
    annotationProcessor "org.projectlombok:lombok:${lombokVersion}"

    // 测试依赖
    testImplementation "org.junit.jupiter:junit-jupiter:5.10.1"
}

test {
    useJUnitPlatform()
}

shadowJar {
    // 生成 Spark 任务提交使用的 Fat Jar
    archiveClassifier.set('all')
}
```

Gradle 项目建议统一使用 `./gradlew` 执行构建，避免不同开发人员本地 Gradle 版本不一致导致构建结果不同。

### 多模块工程划分

当 Spark 任务较多，或者多个任务需要共享参数解析、SparkSession 初始化、日志、异常、数据源读写和通用转换逻辑时，建议使用多模块工程。

推荐 Maven 多模块结构如下：

```bash
spark-platform
├── pom.xml
├── spark-common              # 通用工具、异常、参数、日志、常量
├── spark-core                # SparkSession、Reader、Writer、Transformer 抽象
├── spark-batch-jobs          # 离线批处理任务
├── spark-streaming-jobs      # Structured Streaming 任务
├── spark-sql-jobs            # SQL 文件驱动型任务
├── spark-test-support        # 测试基类、样例数据、断言工具
├── scripts
├── sql
└── docs
```

各模块职责建议如下：

| 模块                   | 职责                                                      |
| ---------------------- | --------------------------------------------------------- |
| `spark-common`         | 通用常量、异常、参数解析、日期工具、日志工具              |
| `spark-core`           | SparkSession 创建、数据读取、数据写出、配置封装、抽象 Job |
| `spark-batch-jobs`     | 离线批处理任务入口和业务逻辑                              |
| `spark-streaming-jobs` | Kafka 流处理、Checkpoint、Watermark、ForeachBatch 逻辑    |
| `spark-sql-jobs`       | SQL 文件加载、参数替换、SQL 执行和结果写出                |
| `spark-test-support`   | 本地测试 SparkSession、测试样例数据、公共断言方法         |
| `scripts`              | 提交、停止、补数、重跑和部署脚本                          |
| `sql`                  | 可维护的 SQL 文件，按业务域或数据层级划分                 |

多模块项目中，父工程只负责依赖版本管理和模块聚合，不应写具体业务代码。公共模块不应反向依赖业务模块，避免依赖关系混乱。

父工程依赖版本管理示例：

```xml
<!-- 父工程 pom.xml 中统一管理依赖版本 -->
<dependencyManagement>
    <dependencies>
        <!-- Spark SQL 版本统一管理，子模块按需引入 -->
        <dependency>
            <groupId>org.apache.spark</groupId>
            <artifactId>spark-sql_2.12</artifactId>
            <version>${spark.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- Hutool 工具类版本统一管理 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

多模块划分不宜过度。任务数量较少时，单模块更简单；任务数量多、团队协作多、公共能力复用明显时，再拆分多模块。

### 配置文件目录

配置文件用于管理运行环境、数据源、Spark 参数、Hive 参数、Kafka 参数、JDBC 参数和日志参数。配置文件应与代码解耦，支持不同环境使用不同配置。

推荐配置目录如下：

```bash
src/main/resources
├── application.yml           # 通用配置
├── application-local.yml     # 本地环境配置
├── application-test.yml      # 测试环境配置
├── application-prod.yml      # 生产环境配置
├── logback.xml               # 日志配置
├── spark-defaults.conf       # Spark 默认参数，可选
├── hive-site.xml             # Hive 配置，可选，通常由集群提供
└── sql
    ├── user
    │   └── ads_user_summary.sql
    └── order
        └── dwd_order_detail.sql
```

通用配置示例：

```yaml
# 通用配置，所有环境共享
spark:
  app-name: spark-data-job
  sql:
    # 默认 Shuffle 分区数，生产环境可通过提交参数覆盖
    shuffle-partitions: 200
  hive:
    # 是否开启 Hive 支持
    enabled: true

job:
  # 默认业务日期参数名
  biz-date-param: bizDate
  # 默认运行环境
  env: local

log:
  # 是否打印任务参数
  print-args: true
```

本地环境配置示例：

```yaml
# 本地环境配置，只用于开发调试
spark:
  master: local[*]
  sql:
    shuffle-partitions: 4

data:
  input-root: data/input
  output-root: data/output

job:
  env: local
```

生产环境配置示例：

```yaml
# 生产环境配置，敏感信息不建议直接写入文件
spark:
  master: yarn
  deploy-mode: cluster
  sql:
    shuffle-partitions: 400

data:
  input-root: hdfs:///warehouse/ods
  output-root: hdfs:///warehouse/ads

job:
  env: prod
```

配置文件中不应直接保存数据库密码、Kerberos Keytab、云存储密钥等敏感信息。敏感配置应通过环境变量、密钥管理系统或调度平台参数注入。

### 脚本目录

脚本目录用于保存任务提交、停止、补数、重跑、部署和环境检查脚本。脚本需要做到参数清晰、路径明确、日志可追踪，并且不要将生产敏感信息写死在脚本中。

推荐脚本目录如下：

```bash
scripts
├── env.sh                    # 公共环境变量
├── submit-local.sh           # 本地提交脚本
├── submit-yarn-client.sh     # YARN Client 模式提交
├── submit-yarn-cluster.sh    # YARN Cluster 模式提交
├── submit-k8s.sh             # Kubernetes 模式提交，可选
├── stop-job.sh               # 停止任务
├── rerun-job.sh              # 重跑任务
├── backfill-job.sh           # 补数任务
└── check-env.sh              # 环境检查脚本
```

公共环境变量脚本示例：

```bash
#!/usr/bin/env bash

# scripts/env.sh
# 公共环境变量，提交脚本统一引用

export JAVA_HOME=/opt/module/jdk
export SPARK_HOME=/opt/module/spark
export HADOOP_CONF_DIR=/opt/module/hadoop/etc/hadoop
export HIVE_CONF_DIR=/opt/module/hive/conf

export PATH=$JAVA_HOME/bin:$SPARK_HOME/bin:$PATH

# 默认应用名称
export APP_NAME=spark-data-job

# 默认 Jar 路径，发布时根据实际路径调整
export APP_JAR=/opt/app/spark-job/spark-job.jar
```

YARN Cluster 提交脚本示例：

```bash
#!/usr/bin/env bash

# scripts/submit-yarn-cluster.sh
# 提交 Spark 任务到 YARN Cluster 模式

set -e

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
source "${SCRIPT_DIR}/env.sh"

BIZ_DATE=$1
ENV=${2:-prod}

if [ -z "${BIZ_DATE}" ]; then
  echo "缺少业务日期参数，例如：./submit-yarn-cluster.sh 2026-05-11 prod"
  exit 1
fi

spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --name "${APP_NAME}-${BIZ_DATE}" \
  --queue default \
  --driver-memory 2g \
  --executor-memory 4g \
  --executor-cores 2 \
  --num-executors 4 \
  --conf spark.sql.shuffle.partitions=400 \
  --conf spark.yarn.maxAppAttempts=1 \
  "${APP_JAR}" \
  --env "${ENV}" \
  --bizDate "${BIZ_DATE}"
```

执行脚本前需要授权：

```bash
# 给脚本增加执行权限
chmod +x scripts/*.sh

# 提交生产任务
./scripts/submit-yarn-cluster.sh 2026-05-11 prod
```

脚本中应避免写死复杂业务参数。通用参数可以放在 `env.sh`，动态参数通过命令行传入，敏感参数通过调度平台或环境变量注入。

### 日志目录

日志目录用于保存本地调试日志、任务提交日志、运行日志和异常日志。生产环境中，Spark Executor 日志通常由 YARN、Kubernetes 或日志采集系统管理；项目本地仍应保留统一日志目录，便于开发调试。

推荐日志目录如下：

```bash
logs
├── app                         # 应用运行日志
│   └── spark-data-job.log
├── submit                      # 提交脚本日志
│   └── submit-2026-05-11.log
├── error                       # 错误日志
│   └── error.log
└── archive                     # 归档日志
```

Logback 配置示例：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 日志目录，可通过 -DLOG_HOME 覆盖 -->
    <property name="LOG_HOME" value="${LOG_HOME:-logs/app}"/>

    <!-- 控制台日志，适合本地开发和任务提交观察 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 文件日志，按天滚动 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_HOME}/spark-data-job.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- 日志按天归档，保留 30 天 -->
            <fileNamePattern>${LOG_HOME}/archive/spark-data-job.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>

    <!-- Spark 自身日志级别，避免输出过多调度细节 -->
    <logger name="org.apache.spark" level="WARN"/>
    <logger name="org.apache.hadoop" level="WARN"/>
    <logger name="org.apache.kafka" level="WARN"/>

    <!-- 项目包日志级别 -->
    <logger name="io.github.atengk" level="INFO"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

日志中建议记录以下信息：

| 日志内容     | 说明                                   |
| ------------ | -------------------------------------- |
| 任务启动信息 | 应用名称、运行环境、业务日期、主类     |
| 参数信息     | 输入路径、输出路径、数据源、运行模式   |
| 数据量信息   | 输入条数、输出条数、过滤条数、异常条数 |
| 耗时信息     | 读取耗时、转换耗时、写出耗时、总耗时   |
| 异常信息     | 异常类型、异常消息、关键参数和堆栈     |
| 任务结束信息 | 成功、失败、输出位置和处理结果         |

不要在日志中打印密码、Token、Keytab、身份证号、手机号、银行卡号等敏感信息。

### 测试目录

测试目录用于保存单元测试、集成测试、SQL 测试、样例数据和测试配置。Spark 项目测试的核心目标是验证数据处理逻辑，而不只是验证方法能否调用成功。

推荐测试目录如下：

```bash
src/test
├── java
│   └── io/github/atengk/spark
│       ├── SparkBaseTest.java
│       ├── job
│       ├── transformer
│       ├── reader
│       └── writer
└── resources
    ├── application-test.yml
    ├── data
    │   ├── input
    │   │   ├── user.json
    │   │   └── order.csv
    │   └── expected
    │       └── user_summary.json
    └── sql
        └── test_user_summary.sql
```

测试数据应尽量小而完整，覆盖正常数据、空值、重复数据、异常数据、边界值和无数据场景。

测试配置示例：

```yaml
# 测试环境配置，用于本地单元测试和集成测试
spark:
  master: local[2]
  app-name: spark-test
  sql:
    shuffle-partitions: 2

data:
  input-root: src/test/resources/data/input
  expected-root: src/test/resources/data/expected
  output-root: target/test-output
```

测试类型建议如下：

| 测试类型     | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 单元测试     | 验证参数解析、字段转换、工具类、SQL 参数替换等小逻辑         |
| 本地集成测试 | 使用 local 模式创建 SparkSession，验证完整 DataFrame 转换链路 |
| SQL 结果测试 | 对输入样例执行 SQL，比较实际结果和期望结果                   |
| 边界值测试   | 验证空数据、空字段、异常格式、重复数据等情况                 |
| 写出测试     | 写入本地临时目录，验证文件格式、分区目录和数据内容           |
| 回归测试     | 修改核心逻辑后，确保历史样例数据处理结果不变                 |

测试输出目录建议使用 `target/test-output` 或 `build/test-output`，并加入 `.gitignore`，避免测试结果文件提交到代码仓库。

```gitignore
# Spark 本地调试输出
data/output/

# Maven 构建目录
target/

# Gradle 构建目录
build/

# 本地日志目录
logs/

# IDE 文件
.idea/
*.iml
```

Spark 项目的测试不应只依赖人工执行 `spark-submit`。核心转换逻辑、SQL 文件和参数处理应具备可重复执行的自动化测试能力，确保后续任务扩展和逻辑调整时能够快速发现问题。

## 依赖管理

本章节用于规范 Spark Java 项目的依赖声明、版本管理、作用域控制和冲突处理方式。Spark 项目依赖通常涉及 Spark、Hadoop、Hive、Kafka、JDBC、日志框架和工具类，如果版本不统一或打包范围不合理，容易在本地运行正常但集群提交失败。依赖管理应以集群运行时版本为基准，再结合项目实际使用的数据源进行最小化引入。

### Spark Core 依赖

Spark Core 是 Spark 的底层计算模块，提供 RDD、任务调度、分区、缓存、广播变量、累加器和 Shuffle 等基础能力。即使项目主要使用 Spark SQL，Spark Core 仍然是 Spark 应用运行的基础依赖。

在 Maven 项目中可以按以下方式声明：

```xml
<!-- Spark Core：提供 RDD、任务调度、分区、缓存、广播变量等基础能力 -->
<dependency>
    <groupId>org.apache.spark</groupId>
    <artifactId>spark-core_2.12</artifactId>
    <version>${spark.version}</version>
    <scope>provided</scope>
</dependency>
```

在 Gradle 项目中可以按以下方式声明：

```groovy
// Spark Core：集群运行时通常已提供，避免打入 Fat Jar
compileOnly "org.apache.spark:spark-core_2.12:${sparkVersion}"
```

`artifactId` 中的 `_2.12` 表示 Scala 二进制版本，需要与 Spark 集群版本保持一致。生产环境中，Spark Core 通常由集群自带，因此建议使用 `provided` 或 `compileOnly`，避免将 Spark 运行时依赖打入任务 Jar。

Spark Core 依赖适用于以下场景：

| 场景     | 说明                                               |
| -------- | -------------------------------------------------- |
| RDD 开发 | 创建 RDD、执行 map、filter、reduceByKey 等底层算子 |
| 广播变量 | 将小维表或配置数据广播到 Executor                  |
| 累加器   | 统计脏数据数量、异常记录数量等运行指标             |
| 分区控制 | 自定义分区、调整并行度、控制数据分布               |
| 缓存控制 | 对 RDD 或 DataFrame 进行 cache、persist 操作       |

如果项目只使用 `SparkSession` 和 `Dataset<Row>`，一般可以只显式引入 Spark SQL，因为 Spark SQL 会传递依赖 Spark Core。但在文档和依赖管理中仍应明确 Spark Core 的版本来源。

### Spark SQL 依赖

Spark SQL 是 Java Spark 项目中最常用的依赖，提供 SparkSession、DataFrame、Dataset、SQL 查询、函数、执行计划优化和多数据源读写能力。常规离线开发、Hive 加工、Parquet 处理、JDBC 读写都应优先使用 Spark SQL。

Maven 依赖如下：

```xml
<!-- Spark SQL：提供 SparkSession、DataFrame、Dataset、SQL 查询和结构化数据处理能力 -->
<dependency>
    <groupId>org.apache.spark</groupId>
    <artifactId>spark-sql_2.12</artifactId>
    <version>${spark.version}</version>
    <scope>provided</scope>
</dependency>
```

Gradle 依赖如下：

```groovy
// Spark SQL：结构化数据处理核心依赖，生产集群通常已提供
compileOnly "org.apache.spark:spark-sql_2.12:${sparkVersion}"
```

Spark SQL 常用于以下功能：

| 功能              | 说明                                    |
| ----------------- | --------------------------------------- |
| 创建 SparkSession | Java Spark 应用的统一入口               |
| 读取结构化文件    | 支持 CSV、JSON、Parquet、ORC 等格式     |
| 注册临时视图      | 将 DataFrame 注册为 SQL 可查询对象      |
| 执行 SQL          | 使用 SQL 完成清洗、Join、聚合和指标计算 |
| 写出结果          | 写入 Hive、HDFS、对象存储、JDBC 等目标  |
| 执行计划分析      | 使用 `explain()` 查看逻辑计划和物理计划 |

生产环境建议将 Spark SQL 设置为 `provided`。如果使用本地模式运行单元测试，可以在测试范围内引入 Spark SQL，或者通过 IDE 和本地 Spark 环境提供依赖。

### Spark Streaming 依赖

Spark Streaming 包括传统 DStream 模型和 Structured Streaming。新项目应优先使用 Structured Streaming，尤其是 Kafka 实时消费、流式清洗、实时聚合和流批一体处理场景。

如果使用传统 DStream API，需要引入 `spark-streaming`：

```xml
<!-- Spark Streaming：传统 DStream 流处理依赖，新项目优先使用 Structured Streaming -->
<dependency>
    <groupId>org.apache.spark</groupId>
    <artifactId>spark-streaming_2.12</artifactId>
    <version>${spark.version}</version>
    <scope>provided</scope>
</dependency>
```

如果使用 Structured Streaming 读取 Kafka，需要引入 Kafka SQL Connector：

```xml
<!-- Spark Kafka SQL Connector：Structured Streaming 读取和写入 Kafka 的核心依赖 -->
<dependency>
    <groupId>org.apache.spark</groupId>
    <artifactId>spark-sql-kafka-0-10_2.12</artifactId>
    <version>${spark.version}</version>
</dependency>
```

Gradle 依赖如下：

```groovy
// Spark Streaming：传统 DStream API，只有维护旧任务时才建议引入
compileOnly "org.apache.spark:spark-streaming_2.12:${sparkVersion}"

// Kafka Connector：Structured Streaming 读取和写入 Kafka 使用
implementation "org.apache.spark:spark-sql-kafka-0-10_2.12:${sparkVersion}"
```

需要注意，`spark-sql-kafka-0-10_2.12` 是否由集群提供取决于集群安装方式。很多环境不会默认提供该 Connector，因此通常需要随应用一起打包，或者通过 `--packages`、`--jars` 方式提交。

Structured Streaming 常见依赖选择如下：

| 场景                 | 依赖                         |
| -------------------- | ---------------------------- |
| Kafka 流式读取       | `spark-sql-kafka-0-10_2.12`  |
| Kafka 流式写入       | `spark-sql-kafka-0-10_2.12`  |
| 文件流读取           | `spark-sql_2.12`             |
| ForeachBatch 写 JDBC | `spark-sql_2.12` + JDBC 驱动 |
| 旧 DStream 任务      | `spark-streaming_2.12`       |

### Hadoop 相关依赖

Spark 访问 HDFS、YARN 和 Hadoop 生态组件时，需要 Hadoop 相关依赖。生产环境中，Hadoop 依赖通常由 Spark 集群或 Hadoop 客户端提供，项目中不应随意打包完整 Hadoop 依赖，否则容易与集群版本冲突。

Maven 依赖示例：

```xml
<!-- Hadoop Client：访问 HDFS、YARN 等 Hadoop 生态能力，生产环境通常由集群提供 -->
<dependency>
    <groupId>org.apache.hadoop</groupId>
    <artifactId>hadoop-client</artifactId>
    <version>${hadoop.version}</version>
    <scope>provided</scope>
</dependency>
```

如果项目需要显式访问 HDFS API，可以按需引入更细粒度依赖：

```xml
<!-- Hadoop Common：提供 Hadoop 基础配置、文件系统抽象等能力 -->
<dependency>
    <groupId>org.apache.hadoop</groupId>
    <artifactId>hadoop-common</artifactId>
    <version>${hadoop.version}</version>
    <scope>provided</scope>
</dependency>

<!-- Hadoop HDFS Client：提供 HDFS 文件系统访问能力 -->
<dependency>
    <groupId>org.apache.hadoop</groupId>
    <artifactId>hadoop-hdfs-client</artifactId>
    <version>${hadoop.version}</version>
    <scope>provided</scope>
</dependency>
```

Hadoop 依赖常见注意事项：

| 注意点                  | 说明                                                       |
| ----------------------- | ---------------------------------------------------------- |
| 版本必须匹配集群        | Hadoop 客户端版本与集群差异过大时，容易出现协议兼容问题    |
| 优先使用 provided       | 避免任务 Jar 中打入 Hadoop 依赖与集群冲突                  |
| 配置文件比依赖更重要    | `core-site.xml`、`hdfs-site.xml`、`yarn-site.xml` 必须正确 |
| Kerberos 环境需额外配置 | 需要 keytab、principal、krb5.conf 等安全配置               |
| 不建议随意升级          | Hadoop 依赖升级应跟随集群升级，而不是业务项目单独升级      |

### Hive 相关依赖

Spark 集成 Hive 时，需要 Spark Hive 模块和 Hive Metastore 配置。项目如果需要读取 Hive 表、写入 Hive 分区表、访问 Hive Metastore 或启用 `enableHiveSupport()`，应引入 Spark Hive 依赖。

Maven 依赖如下：

```xml
<!-- Spark Hive：支持 Hive Metastore、Hive 表读写、enableHiveSupport 等能力 -->
<dependency>
    <groupId>org.apache.spark</groupId>
    <artifactId>spark-hive_2.12</artifactId>
    <version>${spark.version}</version>
    <scope>provided</scope>
</dependency>
```

Gradle 依赖如下：

```groovy
// Spark Hive：生产集群通常已提供，避免重复打包导致 Hive 依赖冲突
compileOnly "org.apache.spark:spark-hive_2.12:${sparkVersion}"
```

Hive 依赖常见使用方式如下：

```java
SparkSession spark = SparkSession.builder()
        .appName("spark-hive-job")
        .enableHiveSupport()
        .getOrCreate();
```

Hive 集成重点不只在依赖，还包括以下配置：

| 配置                              | 说明                                        |
| --------------------------------- | ------------------------------------------- |
| `hive-site.xml`                   | Hive Metastore、Warehouse、权限、安全等配置 |
| `spark.sql.catalogImplementation` | 使用 Hive Catalog 时通常为 `hive`           |
| `spark.sql.warehouse.dir`         | Spark SQL 默认 Warehouse 目录               |
| Metastore 地址                    | 通常通过 `hive.metastore.uris` 指定         |
| HDFS 权限                         | 写 Hive 表时需要目标库表路径权限            |

如果只读取 Parquet 或 ORC 文件，不访问 Hive Metastore，可以不引入 `spark-hive_2.12`。如果需要 `spark.table("db.table")` 读取 Hive 表，则必须保证 Spark Hive 依赖和 Hive 配置完整。

### 数据源连接依赖

Spark 项目经常需要连接 Kafka、MySQL、PostgreSQL、Oracle、SQL Server、对象存储和其他外部系统。数据源依赖应按需引入，不要一次性引入所有驱动。

常见数据源依赖如下：

```xml
<!-- Kafka Connector：Structured Streaming 读取和写入 Kafka -->
<dependency>
    <groupId>org.apache.spark</groupId>
    <artifactId>spark-sql-kafka-0-10_2.12</artifactId>
    <version>${spark.version}</version>
</dependency>

<!-- MySQL 驱动：Spark JDBC 读取和写入 MySQL -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>${mysql.version}</version>
</dependency>

<!-- PostgreSQL 驱动：Spark JDBC 读取和写入 PostgreSQL -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>${postgresql.version}</version>
</dependency>

<!-- SQL Server 驱动：Spark JDBC 读取和写入 SQL Server -->
<dependency>
    <groupId>com.microsoft.sqlserver</groupId>
    <artifactId>mssql-jdbc</artifactId>
    <version>${sqlserver.version}</version>
</dependency>
```

Oracle 驱动依赖需要根据公司私服或授权方式处理：

```xml
<!-- Oracle 驱动：通常需要从公司 Maven 私服获取，版本按数据库环境确定 -->
<dependency>
    <groupId>com.oracle.database.jdbc</groupId>
    <artifactId>ojdbc8</artifactId>
    <version>${oracle.version}</version>
</dependency>
```

如果访问 S3、OSS、COS、MinIO 等对象存储，通常需要 Hadoop 对象存储相关依赖和配置。例如 S3A 依赖：

```xml
<!-- Hadoop AWS：通过 s3a:// 协议访问 S3 或兼容 S3 的对象存储 -->
<dependency>
    <groupId>org.apache.hadoop</groupId>
    <artifactId>hadoop-aws</artifactId>
    <version>${hadoop.version}</version>
</dependency>

<!-- AWS SDK Bundle：Hadoop AWS 依赖的 SDK 包，版本需要与 Hadoop 版本兼容 -->
<dependency>
    <groupId>com.amazonaws</groupId>
    <artifactId>aws-java-sdk-bundle</artifactId>
    <version>${aws.sdk.version}</version>
</dependency>
```

数据源依赖选择建议如下：

| 数据源     | 推荐依赖                    | 打包建议               |
| ---------- | --------------------------- | ---------------------- |
| Kafka      | `spark-sql-kafka-0-10_2.12` | 集群未提供时打包       |
| MySQL      | `mysql-connector-j`         | 通常随应用打包         |
| PostgreSQL | `postgresql`                | 通常随应用打包         |
| Oracle     | `ojdbc8` 或对应版本         | 按公司私服和授权管理   |
| SQL Server | `mssql-jdbc`                | 通常随应用打包         |
| S3 / MinIO | `hadoop-aws` + SDK          | 与 Hadoop 版本严格匹配 |
| Hive       | `spark-hive_2.12`           | 通常 provided          |

### 日志依赖

Spark 本身依赖日志框架，项目侧需要统一日志输出规范。常见选择包括 Logback 和 Log4j2。具体使用哪一种，需要结合 Spark 版本、公司日志规范和现有依赖体系。

如果项目使用 Logback，可以声明：

```xml
<!-- SLF4J API：日志门面，业务代码统一使用 Logger 输出日志 -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>${slf4j.version}</version>
</dependency>

<!-- Logback Classic：SLF4J 的 Logback 实现，用于本地和应用日志输出 -->
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>${logback.version}</version>
</dependency>
```

如果 Spark 集群使用 Log4j2，则可以按集群规范使用 Log4j2 配置：

```xml
<!-- Log4j2 SLF4J 绑定：将 SLF4J 日志转到 Log4j2 -->
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-slf4j2-impl</artifactId>
    <version>${log4j2.version}</version>
</dependency>

<!-- Log4j2 Core：Log4j2 核心实现 -->
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>${log4j2.version}</version>
</dependency>
```

日志依赖管理原则如下：

| 原则                  | 说明                                                      |
| --------------------- | --------------------------------------------------------- |
| 业务代码使用 SLF4J    | 避免直接绑定具体日志实现                                  |
| 不混用多个绑定        | 避免同时存在 logback-classic、log4j-slf4j-impl 等多个实现 |
| 与 Spark 日志体系兼容 | 根据 Spark 集群使用 Log4j 还是 Log4j2 选择配置            |
| 控制依赖传递          | 排除多余日志桥接包，避免循环转发                          |
| 生产日志级别保守      | Spark、Hadoop、Kafka 日志通常设置为 WARN                  |

常见日志冲突包括多个 SLF4J Binding、Log4j 1.x 与 Log4j2 混用、桥接包循环依赖等。出现日志异常时，应优先通过依赖树排查。

### 工具类依赖

工具类依赖用于简化参数解析、字符串处理、日期处理、集合处理、JSON 处理、文件处理和配置读取。Java Spark 项目中建议引入 Hutool，减少重复工具代码。

Maven 依赖如下：

```xml
<!-- Hutool：提供字符串、日期、集合、JSON、文件、参数校验等常用工具能力 -->
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>${hutool.version}</version>
</dependency>

<!-- Lombok：减少日志对象、Getter、Setter、构造方法等样板代码 -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>${lombok.version}</version>
    <scope>provided</scope>
</dependency>
```

常用工具类建议如下：

| 工具类       | 用途                           |
| ------------ | ------------------------------ |
| `StrUtil`    | 字符串判空、格式化、分割、替换 |
| `ObjectUtil` | 对象判空、默认值处理           |
| `CollUtil`   | 集合判空、集合构造、集合转换   |
| `MapUtil`    | Map 判空、参数 Map 构造        |
| `DateUtil`   | 日期解析、格式化、偏移计算     |
| `JSONUtil`   | JSON 字符串解析和对象转换      |
| `FileUtil`   | 本地文件读取、写入、路径处理   |
| `ReUtil`     | 正则校验和提取                 |

在 Spark 项目中，Hutool 适合用于 Driver 侧参数处理、配置校验、SQL 模板替换和本地文件读取。不要在大规模 Executor 计算逻辑中过度使用复杂工具方法，避免不必要的序列化和性能开销。

### 依赖冲突处理

Spark 项目依赖冲突非常常见，尤其集中在 Scala、Guava、Jackson、Netty、SLF4J、Log4j、Hadoop、Hive、Kafka Client 等组件。依赖冲突处理的原则是先确认集群运行时版本，再调整项目依赖，而不是盲目升级或强制覆盖。

Maven 查看依赖树：

```bash
# 查看完整依赖树
mvn dependency:tree

# 查看指定依赖来源，例如排查 Jackson 冲突
mvn dependency:tree -Dincludes=com.fasterxml.jackson.core

# 查看 Spark 相关依赖
mvn dependency:tree -Dincludes=org.apache.spark
```

Gradle 查看依赖：

```bash
# 查看运行时依赖
./gradlew dependencies --configuration runtimeClasspath

# 查看指定依赖的来源
./gradlew dependencyInsight --dependency jackson-databind --configuration runtimeClasspath
```

Maven 排除传递依赖示例：

```xml
<!-- 示例：引入某个外部依赖时排除冲突的日志实现 -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>example-client</artifactId>
    <version>${example.version}</version>
    <exclusions>
        <!-- 排除多余 SLF4J 绑定，避免多个日志实现冲突 -->
        <exclusion>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-log4j12</artifactId>
        </exclusion>
        <!-- 排除旧版 Log4j，避免与集群日志体系冲突 -->
        <exclusion>
            <groupId>log4j</groupId>
            <artifactId>log4j</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

Maven Shade Plugin 中可以排除集群已提供依赖：

```xml
<!-- Shade 打包配置：构建业务 Fat Jar，同时避免打入 Spark、Hadoop 等集群依赖 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.5.1</version>
    <configuration>
        <artifactSet>
            <excludes>
                <!-- Spark 运行时由集群提供 -->
                <exclude>org.apache.spark:*</exclude>
                <!-- Hadoop 运行时由集群提供 -->
                <exclude>org.apache.hadoop:*</exclude>
                <!-- Scala 标准库由 Spark 集群依赖提供 -->
                <exclude>org.scala-lang:*</exclude>
            </excludes>
        </artifactSet>
    </configuration>
</plugin>
```

常见依赖冲突与处理方式如下：

| 异常                      | 常见原因                            | 处理方式                            |
| ------------------------- | ----------------------------------- | ----------------------------------- |
| `ClassNotFoundException`  | 依赖未打包或集群未提供              | 将依赖打入 Jar 或通过 `--jars` 提交 |
| `NoSuchMethodError`       | 依赖版本不一致                      | 对齐集群版本，排除低版本传递依赖    |
| `NoClassDefFoundError`    | 编译期存在，运行期缺失              | 检查 scope、Fat Jar 和提交参数      |
| `Multiple SLF4J bindings` | 多个日志实现同时存在                | 保留一个日志实现，排除其他绑定      |
| `Guava` 冲突              | Hadoop、Hive、第三方 SDK 版本不一致 | 避免强制升级，优先按集群版本处理    |
| `Jackson` 冲突            | Spark、Kafka、业务 SDK 版本不一致   | 使用依赖树确认来源，统一版本或排除  |
| Scala 版本冲突            | `_2.11`、`_2.12`、`_2.13` 混用      | 所有 Spark 依赖使用同一 Scala 后缀  |

依赖冲突处理顺序建议为：先查看集群组件版本，再查看项目依赖树，然后定位冲突来源，最后通过 `provided`、`exclusion`、版本锁定或调整打包策略解决。

## 配置管理

本章节用于规范 Spark 项目的配置来源、配置结构、参数传入、多环境隔离和运行时覆盖方式。Spark 项目配置通常包括应用基础信息、Spark 参数、运行环境、数据源、Hive Metastore、日志和业务参数。配置管理的目标是让同一套代码可以在本地、测试和生产环境中通过配置切换稳定运行。

### 应用基础配置

应用基础配置用于描述任务名称、运行环境、业务日期、输入输出路径、是否开启 Hive、是否打印参数等通用信息。这类配置应放在项目配置文件中，并允许通过命令行参数或调度平台参数覆盖。

推荐配置结构如下：

```yaml
# src/main/resources/application.yml
# 应用通用配置，所有环境共享
app:
  # 应用名称，用于 Spark UI、YARN UI 和日志标识
  name: spark-data-job
  # 默认运行环境，可被命令行参数覆盖
  env: local
  # 是否打印启动参数，生产环境建议开启但需要脱敏
  print-args: true

job:
  # 默认业务日期，实际生产通常由调度平台传入
  biz-date: 2026-05-11
  # 任务批次号，可选
  batch-id: default
  # 是否允许空数据继续执行
  allow-empty: false

data:
  # 默认输入根路径
  input-root: data/input
  # 默认输出根路径
  output-root: data/output
```

应用基础配置建议遵守以下规则：

| 规则             | 说明                                              |
| ---------------- | ------------------------------------------------- |
| 配置键命名清晰   | 使用 `app`、`job`、`data`、`spark`、`hive` 等分组 |
| 默认值保守       | 本地默认值可以简单，生产默认值必须安全            |
| 业务参数可覆盖   | 业务日期、输入路径、输出路径应支持命令行覆盖      |
| 敏感信息不落文件 | 密码、Token、Keytab、Secret 不直接写入配置文件    |
| 配置变更可追踪   | 生产配置变更应进入发布或变更流程                  |

### SparkConf 配置

SparkConf 用于设置 Spark 应用运行参数，包括应用名称、Master、序列化方式、Shuffle 分区数、Executor 资源、动态资源分配、SQL 参数等。SparkConf 可以通过代码、配置文件、`spark-submit --conf` 或 `spark-defaults.conf` 设置。

常见 SparkConf 配置如下：

```yaml
# Spark 运行配置
spark:
  # 应用名称
  app-name: spark-data-job
  # 本地开发使用 local[*]，生产环境由 spark-submit 指定 yarn 或 k8s
  master: local[*]
  conf:
    # SQL Shuffle 默认分区数，本地调试可降低，生产按数据规模调整
    spark.sql.shuffle.partitions: 200
    # 开启自适应执行，适合 Spark 3.x
    spark.sql.adaptive.enabled: true
    # 自动合并 Shuffle 分区
    spark.sql.adaptive.coalescePartitions.enabled: true
    # 开启广播 Join 阈值，按实际内存调整
    spark.sql.autoBroadcastJoinThreshold: 10485760
    # 使用 Kryo 序列化提升性能
    spark.serializer: org.apache.spark.serializer.KryoSerializer
```

SparkConf Java 构建示例：

```java
package io.github.atengk.spark.config;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.SparkConf;

import java.util.Map;

/**
 * SparkConf 配置构建工具。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class SparkConfFactory {

    /**
     * 创建 SparkConf。
     *
     * @param appName 应用名称
     * @param master 运行模式
     * @param confMap Spark 配置项
     * @return SparkConf
     */
    public static SparkConf create(String appName, String master, Map<String, String> confMap) {
        SparkConf sparkConf = new SparkConf();

        if (StrUtil.isNotBlank(appName)) {
            sparkConf.setAppName(appName);
        }

        if (StrUtil.isNotBlank(master)) {
            sparkConf.setMaster(master);
        }

        if (MapUtil.isNotEmpty(confMap)) {
            confMap.forEach((key, value) -> {
                if (StrUtil.isNotBlank(key) && StrUtil.isNotBlank(value)) {
                    sparkConf.set(key, value);
                    log.info("加载 Spark 配置：{}={}", key, value);
                }
            });
        }

        return sparkConf;
    }
}
```

SparkConf 配置优先级通常遵循运行时覆盖原则。生产环境中更推荐通过 `spark-submit --conf` 或调度平台传入资源类参数，而不是写死在代码中。

### 运行环境配置

运行环境配置用于区分 local、test、prod 等不同环境。不同环境的数据路径、Shuffle 分区数、日志级别、资源配置、Hive Metastore 和数据源地址通常不同。

推荐环境划分如下：

| 环境    | 说明                                       |
| ------- | ------------------------------------------ |
| `local` | 本地开发调试，使用小样例数据和本地输出目录 |
| `test`  | 测试环境，连接测试集群和测试数据源         |
| `prod`  | 生产环境，连接生产集群和正式数据路径       |

本地环境配置：

```yaml
# src/main/resources/application-local.yml
# 本地调试环境
app:
  env: local

spark:
  master: local[*]
  conf:
    spark.sql.shuffle.partitions: 4
    spark.sql.adaptive.enabled: true

data:
  input-root: data/input
  output-root: data/output
```

测试环境配置：

```yaml
# src/main/resources/application-test.yml
# 测试环境，使用测试集群和测试数据源
app:
  env: test

spark:
  master: yarn
  conf:
    spark.sql.shuffle.partitions: 100
    spark.sql.adaptive.enabled: true

data:
  input-root: hdfs:///warehouse/test/ods
  output-root: hdfs:///warehouse/test/ads
```

生产环境配置：

```yaml
# src/main/resources/application-prod.yml
# 生产环境，资源和路径需要经过发布审核
app:
  env: prod

spark:
  master: yarn
  conf:
    spark.sql.shuffle.partitions: 400
    spark.sql.adaptive.enabled: true
    spark.sql.adaptive.skewJoin.enabled: true

data:
  input-root: hdfs:///warehouse/prod/ods
  output-root: hdfs:///warehouse/prod/ads
```

运行环境应通过启动参数指定，例如：

```bash
# 本地环境
--env local

# 测试环境
--env test

# 生产环境
--env prod
```

代码中不应通过硬编码判断机器名、IP 或路径来决定环境，应统一由参数或配置文件控制。

### 数据源配置

数据源配置用于管理文件、Hive、Kafka、JDBC 和对象存储等外部系统连接信息。配置应按数据源类型分组，避免所有配置平铺在同一级导致难以维护。

JDBC 数据源配置示例：

```yaml
# JDBC 数据源配置
datasource:
  mysql:
    # JDBC 连接地址
    url: jdbc:mysql://127.0.0.1:3306/demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    # 数据库用户名，生产环境建议通过环境变量注入
    username: ${MYSQL_USERNAME:root}
    # 数据库密码，生产环境建议通过环境变量或密钥系统注入
    password: ${MYSQL_PASSWORD:123456}
    # JDBC 驱动类
    driver: com.mysql.cj.jdbc.Driver
    # 读取分区字段，适合大表并行读取
    partition-column: id
    lower-bound: 1
    upper-bound: 1000000
    num-partitions: 8
```

Kafka 数据源配置示例：

```yaml
# Kafka 数据源配置
kafka:
  bootstrap-servers: localhost:9092
  source:
    topic: ods_user_event
    starting-offsets: earliest
  sink:
    topic: dwd_user_event
  options:
    # 消费失败时是否失败退出，生产环境建议 true
    failOnDataLoss: false
    # 每个触发批次最大读取量，避免瞬时数据过大
    maxOffsetsPerTrigger: 100000
```

文件数据源配置示例：

```yaml
# 文件数据源配置
file:
  input:
    user-path: hdfs:///warehouse/ods/user/dt=${bizDate}
    order-path: hdfs:///warehouse/ods/order/dt=${bizDate}
  output:
    user-summary-path: hdfs:///warehouse/ads/user_summary/dt=${bizDate}
  format:
    # 默认使用 Parquet，适合分析型场景
    default: parquet
```

数据源配置管理建议如下：

| 配置类型    | 建议                                     |
| ----------- | ---------------------------------------- |
| JDBC URL    | 按环境区分，不写死在代码中               |
| 用户名密码  | 使用环境变量、调度平台参数或密钥系统注入 |
| Kafka Topic | 按环境命名或通过配置切换                 |
| HDFS 路径   | 使用模板变量拼接业务日期                 |
| 读取并行度  | 大表 JDBC 读取必须配置分区字段           |
| 输出路径    | 生产写出路径必须明确分区和覆盖策略       |

### Hive Metastore 配置

Hive Metastore 配置用于让 Spark 访问 Hive 元数据，包括数据库、表、字段、分区、存储路径和 SerDe 信息。Spark 读取 Hive 表时，必须能够访问 Metastore，并具备相应 HDFS 权限。

`hive-site.xml` 示例：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Hive Metastore 服务地址，按实际环境调整 -->
    <property>
        <name>hive.metastore.uris</name>
        <value>thrift://hive-metastore:9083</value>
    </property>

    <!-- Hive Warehouse 默认目录 -->
    <property>
        <name>hive.metastore.warehouse.dir</name>
        <value>/user/hive/warehouse</value>
    </property>

    <!-- 是否启用 Metastore 客户端自动重试 -->
    <property>
        <name>hive.metastore.client.connect.retry.delay</name>
        <value>5s</value>
    </property>
</configuration>
```

Spark 配置中可以补充 Hive 相关参数：

```yaml
# Hive 集成配置
hive:
  enabled: true
  database: default
  warehouse-dir: hdfs:///user/hive/warehouse
  metastore-uris: thrift://hive-metastore:9083

spark:
  conf:
    # 使用 Hive Catalog
    spark.sql.catalogImplementation: hive
    # 指定 Spark SQL Warehouse 目录
    spark.sql.warehouse.dir: hdfs:///user/hive/warehouse
```

SparkSession 开启 Hive 支持：

```java
package io.github.atengk.spark.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;

/**
 * SparkSession 创建工厂。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class SparkSessionFactory {

    /**
     * 创建支持 Hive 的 SparkSession。
     *
     * @param sparkConf Spark 配置
     * @param hiveEnabled 是否开启 Hive 支持
     * @return SparkSession
     */
    public static SparkSession create(SparkConf sparkConf, boolean hiveEnabled) {
        SparkSession.Builder builder = SparkSession.builder().config(sparkConf);

        if (hiveEnabled) {
            builder.enableHiveSupport();
            log.info("已开启 Spark Hive 支持");
        } else {
            log.info("未开启 Spark Hive 支持");
        }

        return builder.getOrCreate();
    }
}
```

Hive Metastore 配置注意事项：

| 注意点                              | 说明                               |
| ----------------------------------- | ---------------------------------- |
| `hive-site.xml` 必须在 classpath 中 | 否则 Spark 无法读取 Metastore 配置 |
| Metastore 地址区分环境              | 测试和生产不能混用                 |
| Warehouse 路径权限正确              | 写表和动态分区需要 HDFS 写权限     |
| 分区修复谨慎使用                    | `MSCK REPAIR TABLE` 对大表可能较慢 |
| 不在代码中写死库名                  | 库名应通过环境配置或参数传入       |

### 日志配置

日志配置用于控制应用日志格式、日志级别、输出位置和归档策略。Spark 项目日志需要兼顾 Driver 日志、Executor 日志、业务日志和提交脚本日志。

Logback 配置示例：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 日志目录，可通过 -DLOG_HOME=xxx 覆盖 -->
    <property name="LOG_HOME" value="${LOG_HOME:-logs/app}"/>

    <!-- 控制台日志，适合本地开发和任务提交观察 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 文件日志，按天滚动 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_HOME}/spark-data-job.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- 日志保留 30 天 -->
            <fileNamePattern>${LOG_HOME}/archive/spark-data-job.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>

    <!-- 降低 Spark、Hadoop、Kafka 框架日志噪声 -->
    <logger name="org.apache.spark" level="WARN"/>
    <logger name="org.apache.hadoop" level="WARN"/>
    <logger name="org.apache.kafka" level="WARN"/>

    <!-- 项目业务日志 -->
    <logger name="io.github.atengk" level="INFO"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

提交时可以通过 JVM 参数指定日志配置：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --conf "spark.driver.extraJavaOptions=-Dlogback.configurationFile=logback.xml -DLOG_HOME=/data/logs/spark-data-job" \
  --conf "spark.executor.extraJavaOptions=-Dlogback.configurationFile=logback.xml" \
  spark-job.jar \
  --env prod \
  --bizDate 2026-05-11
```

日志配置建议如下：

| 项目           | 建议                             |
| -------------- | -------------------------------- |
| 业务日志级别   | 默认 INFO                        |
| Spark 框架日志 | 默认 WARN                        |
| 参数日志       | 打印非敏感参数                   |
| 数据量日志     | 记录输入、输出、过滤、异常数据量 |
| 异常日志       | 记录异常类型、关键参数和堆栈     |
| 敏感信息       | 必须脱敏或不打印                 |

### 参数动态传入

Spark 任务通常由调度平台触发，需要动态传入业务日期、环境、输入路径、输出路径、队列、资源参数和任务开关。参数动态传入可以通过应用参数、Spark 参数、环境变量和配置文件共同实现。

常见提交命令如下：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --name spark-data-job-2026-05-11 \
  --queue default \
  --conf spark.sql.shuffle.partitions=400 \
  spark-job.jar \
  --env prod \
  --bizDate 2026-05-11 \
  --inputPath hdfs:///warehouse/ods/user/dt=2026-05-11 \
  --outputPath hdfs:///warehouse/ads/user_summary/dt=2026-05-11
```

应用参数解析示例：

```java
package io.github.atengk.spark.config;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Spark 任务启动参数解析器。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class JobArgsParser {

    /**
     * 将命令行参数解析为 Map。
     *
     * @param args 命令行参数
     * @return 参数 Map
     */
    public static Map<String, String> parse(String[] args) {
        Map<String, String> argMap = new HashMap<>();

        if (args == null || args.length == 0) {
            log.warn("未传入任务启动参数");
            return argMap;
        }

        for (int i = 0; i < args.length; i++) {
            String key = args[i];
            if (StrUtil.startWith(key, "--") && i + 1 < args.length) {
                String value = args[i + 1];
                if (!StrUtil.startWith(value, "--")) {
                    argMap.put(StrUtil.removePrefix(key, "--"), value);
                    i++;
                }
            }
        }

        log.info("任务启动参数解析完成，参数个数：{}", argMap.size());
        return argMap;
    }

    /**
     * 获取必填参数。
     *
     * @param argMap 参数 Map
     * @param key 参数键
     * @return 参数值
     */
    public static String getRequired(Map<String, String> argMap, String key) {
        String value = MapUtil.getStr(argMap, key);
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException(StrUtil.format("缺少必填参数：{}", key));
        }
        return value;
    }

    /**
     * 获取可选参数。
     *
     * @param argMap 参数 Map
     * @param key 参数键
     * @param defaultValue 默认值
     * @return 参数值
     */
    public static String getOptional(Map<String, String> argMap, String key, String defaultValue) {
        return StrUtil.blankToDefault(MapUtil.getStr(argMap, key), defaultValue);
    }
}
```

参数类型建议如下：

| 参数             | 示例                        | 说明                   |
| ---------------- | --------------------------- | ---------------------- |
| `env`            | `prod`                      | 运行环境               |
| `bizDate`        | `2026-05-11`                | 业务日期               |
| `batchId`        | `202605110001`              | 批次号                 |
| `inputPath`      | `hdfs:///warehouse/ods/...` | 输入路径               |
| `outputPath`     | `hdfs:///warehouse/ads/...` | 输出路径               |
| `overwrite`      | `true`                      | 是否覆盖输出           |
| `checkpointPath` | `hdfs:///checkpoint/...`    | 流任务 Checkpoint 路径 |

动态参数应在任务启动阶段统一校验。业务日期、输入路径、输出路径、环境标识等关键参数不应等到任务执行一半才发现为空。

### 多环境配置管理

多环境配置管理用于保证同一套代码在本地、测试和生产环境中通过不同配置运行。推荐采用“通用配置 + 环境配置 + 启动参数覆盖”的方式。

推荐目录如下：

```bash
src/main/resources
├── application.yml           # 通用配置
├── application-local.yml     # 本地环境配置
├── application-test.yml      # 测试环境配置
├── application-prod.yml      # 生产环境配置
└── logback.xml
```

配置加载优先级建议如下：

| 优先级 | 来源         | 说明                                   |
| ------ | ------------ | -------------------------------------- |
| 1      | 命令行参数   | 最高优先级，适合业务日期、输入输出路径 |
| 2      | 环境变量     | 适合密码、Token、临时开关              |
| 3      | 环境配置文件 | 适合环境差异配置                       |
| 4      | 通用配置文件 | 适合默认值和公共配置                   |
| 5      | 代码默认值   | 最低优先级，只用于兜底                 |

多环境配置示例：

```yaml
# application.yml
# 通用配置
app:
  name: spark-data-job
  print-args: true

job:
  allow-empty: false

spark:
  conf:
    spark.sql.adaptive.enabled: true
# application-local.yml
# 本地环境覆盖项
app:
  env: local

spark:
  master: local[*]
  conf:
    spark.sql.shuffle.partitions: 4

data:
  input-root: data/input
  output-root: data/output
# application-prod.yml
# 生产环境覆盖项
app:
  env: prod

spark:
  master: yarn
  conf:
    spark.sql.shuffle.partitions: 400
    spark.sql.adaptive.skewJoin.enabled: true

data:
  input-root: hdfs:///warehouse/prod/ods
  output-root: hdfs:///warehouse/prod/ads
```

生产环境配置管理建议如下：

| 建议             | 说明                                                         |
| ---------------- | ------------------------------------------------------------ |
| 配置与代码分离   | 生产配置尽量由发布系统、调度平台或外部配置文件管理           |
| 敏感配置外置     | 密码、密钥、Keytab、Token 不提交到 Git                       |
| 变更可审计       | 生产参数变更需要记录变更人、时间和原因                       |
| 参数可回滚       | 配置变更失败时应能快速回退                                   |
| 本地不可误连生产 | 本地默认配置不能指向生产数据库和生产输出路径                 |
| 任务参数标准化   | 不同任务尽量使用统一参数名称，例如 `env`、`bizDate`、`batchId` |

多环境配置的核心原则是：代码不感知具体部署环境，环境差异全部由配置和参数控制。这样可以降低发布风险，也便于任务在本地、测试和生产之间迁移。

## SparkSession 与 SparkContext 初始化

本章节用于规范 Spark 应用入口对象的创建方式。Spark Java 项目中应优先使用 `SparkSession` 作为统一入口，通过 `SparkSession` 获取 `SparkContext` 或 `JavaSparkContext`，避免在不同模块中重复创建上下文对象。SparkSession 初始化应集中封装，统一处理应用名称、运行模式、SparkConf、Hive 支持、参数校验和资源释放。

### SparkSession 创建

`SparkSession` 是 Spark SQL、DataFrame、Dataset 和 Structured Streaming 的统一入口。Java 项目中建议所有任务通过统一工厂类创建 `SparkSession`，不要在每个 Job 中直接调用 `SparkSession.builder()`，否则容易出现配置分散、Hive 支持不一致、资源释放遗漏等问题。

推荐基础配置对象如下。

文件位置：`src/main/java/io/github/atengk/spark/config/SparkRuntimeConfig.java`

该配置类用于封装 Spark 应用名称、运行模式、Hive 开关和 SparkConf 参数。

```java
package io.github.atengk.spark.config;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Spark 运行时配置。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
public class SparkRuntimeConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Spark 应用名称。
     */
    private String appName;

    /**
     * Spark 运行模式，例如 local[*]、yarn、spark://host:7077。
     */
    private String master;

    /**
     * 是否开启 Hive 支持。
     */
    private boolean hiveEnabled;

    /**
     * Spark 扩展配置。
     */
    private Map<String, String> sparkConf = new HashMap<>();
}
```

文件位置：`src/main/java/io/github/atengk/spark/config/SparkSessionFactory.java`

该工厂类用于统一创建 SparkSession，并按配置注入 SparkConf 和 Hive 支持。

```java
package io.github.atengk.spark.config;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;

import java.util.Map;

/**
 * SparkSession 工厂类。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class SparkSessionFactory {

    private SparkSessionFactory() {
    }

    /**
     * 创建 SparkSession。
     *
     * @param runtimeConfig Spark 运行时配置
     * @return SparkSession
     */
    public static SparkSession create(SparkRuntimeConfig runtimeConfig) {
        validate(runtimeConfig);

        SparkConf sparkConf = new SparkConf();

        if (StrUtil.isNotBlank(runtimeConfig.getAppName())) {
            sparkConf.setAppName(runtimeConfig.getAppName());
        }

        if (StrUtil.isNotBlank(runtimeConfig.getMaster())) {
            sparkConf.setMaster(runtimeConfig.getMaster());
            log.info("设置 Spark 运行模式：{}", runtimeConfig.getMaster());
        }

        Map<String, String> confMap = runtimeConfig.getSparkConf();
        if (MapUtil.isNotEmpty(confMap)) {
            confMap.forEach((key, value) -> {
                if (StrUtil.isNotBlank(key) && StrUtil.isNotBlank(value)) {
                    sparkConf.set(key, value);
                    log.info("加载 Spark 配置：{}={}", key, value);
                }
            });
        }

        SparkSession.Builder builder = SparkSession.builder().config(sparkConf);

        if (runtimeConfig.isHiveEnabled()) {
            builder.enableHiveSupport();
            log.info("已开启 Spark Hive 支持");
        }

        SparkSession sparkSession = builder.getOrCreate();
        log.info("SparkSession 创建完成，应用名称：{}", sparkSession.sparkContext().appName());
        return sparkSession;
    }

    /**
     * 校验 Spark 运行时配置。
     *
     * @param runtimeConfig Spark 运行时配置
     */
    private static void validate(SparkRuntimeConfig runtimeConfig) {
        if (runtimeConfig == null) {
            throw new IllegalArgumentException("Spark 运行时配置不能为空");
        }
        if (StrUtil.isBlank(runtimeConfig.getAppName())) {
            throw new IllegalArgumentException("Spark 应用名称不能为空");
        }
    }
}
```

使用示例：

```java
SparkRuntimeConfig config = new SparkRuntimeConfig();
config.setAppName("spark-user-summary-job");
config.setMaster("local[*]");
config.setHiveEnabled(true);
config.getSparkConf().put("spark.sql.shuffle.partitions", "4");
config.getSparkConf().put("spark.sql.adaptive.enabled", "true");

SparkSession spark = SparkSessionFactory.create(config);
```

生产环境中不建议在代码中固定 `master`，通常由 `spark-submit --master` 指定。代码中设置 `master` 更适合本地调试和单元测试。

### SparkContext 获取

`SparkContext` 是 Spark Core 的入口对象，负责连接集群、申请资源、调度任务和管理底层执行上下文。使用 `SparkSession` 后，可以直接通过 `spark.sparkContext()` 获取底层 `SparkContext`，也可以通过 `JavaSparkContext.fromSparkContext()` 获取 Java API 风格的上下文对象。

获取方式如下：

```java
// 获取 Scala 风格 SparkContext
SparkContext sparkContext = spark.sparkContext();

// 获取 Java 风格 JavaSparkContext，适合 RDD API 开发
JavaSparkContext javaSparkContext = JavaSparkContext.fromSparkContext(spark.sparkContext());
```

完整使用示例：

```java
package io.github.atengk.spark.demo;

import lombok.extern.slf4j.Slf4j;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;

/**
 * SparkContext 获取示例。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class SparkContextDemo {

    /**
     * 演示从 SparkSession 获取 SparkContext。
     *
     * @param spark SparkSession
     */
    public void showContextInfo(SparkSession spark) {
        SparkContext sparkContext = spark.sparkContext();
        JavaSparkContext javaSparkContext = JavaSparkContext.fromSparkContext(sparkContext);

        log.info("Spark 应用名称：{}", sparkContext.appName());
        log.info("Spark Master：{}", sparkContext.master());
        log.info("默认并行度：{}", javaSparkContext.defaultParallelism());
    }
}
```

实际开发中，只有在使用 RDD、广播变量、累加器、Hadoop API 或底层分区控制时，才需要直接使用 `SparkContext` 或 `JavaSparkContext`。常规结构化数据处理应优先使用 `SparkSession`。

### 本地模式配置

本地模式主要用于开发调试、单元测试、SQL 验证和小样例数据处理。常用 Master 配置包括 `local[1]`、`local[2]` 和 `local[*]`。

本地调试配置示例：

```yaml
# src/main/resources/application-local.yml
# 本地模式配置，只用于开发调试
spark:
  app-name: spark-local-debug
  master: local[*]
  hive-enabled: false
  conf:
    # 本地调试不需要过多 Shuffle 分区
    spark.sql.shuffle.partitions: 4
    # 开启自适应执行，便于观察执行计划优化
    spark.sql.adaptive.enabled: true
    # 本地调试时降低日志噪声
    spark.ui.enabled: true
```

本地模式说明：

| 配置                             | 说明                                   |
| -------------------------------- | -------------------------------------- |
| `local[1]`                       | 单线程运行，适合排查执行顺序和基础逻辑 |
| `local[2]`                       | 双线程运行，适合测试简单并行逻辑       |
| `local[*]`                       | 使用本机全部核心，适合本地性能粗略验证 |
| `spark.sql.shuffle.partitions=4` | 本地调试分区数不宜过大                 |
| `spark.ui.enabled=true`          | 本地运行时可以打开 Spark UI 查看任务   |

本地启动示例：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master local[*] \
  --conf spark.sql.shuffle.partitions=4 \
  target/spark-job.jar \
  --env local \
  --bizDate 2026-05-11
```

本地模式不能完全代表集群运行结果。涉及 Hive 权限、HDFS 路径、YARN 队列、Kafka 消费、Executor 内存和 Shuffle 网络传输的问题，仍需要在测试集群中验证。

### 集群模式配置

集群模式用于测试环境和生产环境运行，常见模式包括 Standalone、YARN Client、YARN Cluster 和 Kubernetes。企业生产环境中较常见的是 YARN Cluster 和 Kubernetes。

YARN Cluster 提交示例：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --name spark-user-summary-job \
  --queue default \
  --driver-memory 2g \
  --executor-memory 4g \
  --executor-cores 2 \
  --num-executors 4 \
  --conf spark.sql.shuffle.partitions=400 \
  --conf spark.sql.adaptive.enabled=true \
  --conf spark.yarn.maxAppAttempts=1 \
  target/spark-job.jar \
  --env prod \
  --bizDate 2026-05-11
```

常用集群参数说明：

| 参数                           | 说明                              |
| ------------------------------ | --------------------------------- |
| `--master yarn`                | 使用 YARN 作为资源管理器          |
| `--deploy-mode cluster`        | Driver 运行在集群中，适合生产任务 |
| `--queue`                      | 指定 YARN 队列                    |
| `--driver-memory`              | Driver 内存                       |
| `--executor-memory`            | Executor 内存                     |
| `--executor-cores`             | 每个 Executor 使用的 CPU 核心数   |
| `--num-executors`              | Executor 数量                     |
| `spark.sql.shuffle.partitions` | SQL Shuffle 分区数                |
| `spark.sql.adaptive.enabled`   | 是否开启自适应执行                |

集群模式中建议将资源参数放在提交脚本或调度平台配置中，而不是写死在 Java 代码里。业务代码只关心任务逻辑，资源分配由运行环境控制。

### Hive 支持开启

如果任务需要读取 Hive 表、写入 Hive 分区表或访问 Hive Metastore，需要开启 Hive 支持。开启方式是在创建 `SparkSession` 时调用 `enableHiveSupport()`。

Hive 支持需要满足以下条件：

| 条件                   | 说明                                 |
| ---------------------- | ------------------------------------ |
| 引入 `spark-hive_2.12` | 项目或集群中需要存在 Spark Hive 依赖 |
| 配置 `hive-site.xml`   | 需要能访问 Hive Metastore            |
| 配置 HDFS 权限         | 需要具备 Hive 表路径读写权限         |
| 设置 Warehouse 路径    | 必要时配置 `spark.sql.warehouse.dir` |
| 集群支持 Hive          | Spark 集群需要具备 Hive 集成能力     |

配置示例：

```yaml
spark:
  app-name: spark-hive-job
  master: yarn
  hive-enabled: true
  conf:
    # 使用 Hive Catalog
    spark.sql.catalogImplementation: hive
    # Spark SQL Warehouse 目录
    spark.sql.warehouse.dir: hdfs:///user/hive/warehouse
```

代码示例：

```java
SparkRuntimeConfig config = new SparkRuntimeConfig();
config.setAppName("spark-hive-job");
config.setHiveEnabled(true);
config.getSparkConf().put("spark.sql.catalogImplementation", "hive");
config.getSparkConf().put("spark.sql.warehouse.dir", "hdfs:///user/hive/warehouse");

SparkSession spark = SparkSessionFactory.create(config);
```

验证 Hive 支持是否生效：

```java
spark.sql("SHOW DATABASES").show(false);
spark.sql("SHOW TABLES IN default").show(false);
```

如果未正确开启 Hive 支持，常见表现是只能访问 Spark 内置 Catalog，无法读取 Hive Metastore 中已有的库表。

### 参数封装

Spark 任务通常需要接收运行环境、业务日期、输入路径、输出路径、是否覆盖、数据源配置等参数。建议在应用启动阶段统一解析并封装参数，避免在业务代码中散落读取 `args`。

文件位置：`src/main/java/io/github/atengk/spark/config/JobRuntimeArgs.java`

该类用于保存任务运行参数。

```java
package io.github.atengk.spark.config;

import lombok.Data;

import java.io.Serializable;

/**
 * Spark 任务运行参数。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
public class JobRuntimeArgs implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 运行环境，例如 local、test、prod。
     */
    private String env;

    /**
     * 业务日期，格式 yyyy-MM-dd。
     */
    private String bizDate;

    /**
     * 输入路径。
     */
    private String inputPath;

    /**
     * 输出路径。
     */
    private String outputPath;

    /**
     * 是否覆盖输出。
     */
    private boolean overwrite;
}
```

文件位置：`src/main/java/io/github/atengk/spark/config/JobArgsParser.java`

该解析器用于将 `spark-submit` 传入的命令行参数转换为配置对象。

```java
package io.github.atengk.spark.config;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Spark 任务参数解析器。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class JobArgsParser {

    private JobArgsParser() {
    }

    /**
     * 解析任务参数。
     *
     * @param args 命令行参数
     * @return 任务运行参数
     */
    public static JobRuntimeArgs parse(String[] args) {
        Map<String, String> argMap = parseToMap(args);

        JobRuntimeArgs runtimeArgs = new JobRuntimeArgs();
        runtimeArgs.setEnv(StrUtil.blankToDefault(MapUtil.getStr(argMap, "env"), "local"));
        runtimeArgs.setBizDate(required(argMap, "bizDate"));
        runtimeArgs.setInputPath(MapUtil.getStr(argMap, "inputPath"));
        runtimeArgs.setOutputPath(MapUtil.getStr(argMap, "outputPath"));
        runtimeArgs.setOverwrite(Boolean.parseBoolean(StrUtil.blankToDefault(MapUtil.getStr(argMap, "overwrite"), "false")));

        log.info("任务参数解析完成，env={}，bizDate={}，overwrite={}",
                runtimeArgs.getEnv(), runtimeArgs.getBizDate(), runtimeArgs.isOverwrite());
        return runtimeArgs;
    }

    /**
     * 将命令行参数解析为 Map。
     *
     * @param args 命令行参数
     * @return 参数 Map
     */
    private static Map<String, String> parseToMap(String[] args) {
        Map<String, String> argMap = new HashMap<>();

        if (args == null || args.length == 0) {
            return argMap;
        }

        for (int i = 0; i < args.length; i++) {
            String key = args[i];
            if (StrUtil.startWith(key, "--") && i + 1 < args.length) {
                String value = args[i + 1];
                if (!StrUtil.startWith(value, "--")) {
                    argMap.put(StrUtil.removePrefix(key, "--"), value);
                    i++;
                }
            }
        }

        return argMap;
    }

    /**
     * 获取必填参数。
     *
     * @param argMap 参数 Map
     * @param key 参数名
     * @return 参数值
     */
    private static String required(Map<String, String> argMap, String key) {
        String value = MapUtil.getStr(argMap, key);
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException(StrUtil.format("缺少必填参数：{}", key));
        }
        return value;
    }
}
```

参数传入示例：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master local[*] \
  target/spark-job.jar \
  --env local \
  --bizDate 2026-05-11 \
  --inputPath data/input/user.json \
  --outputPath data/output/user_summary \
  --overwrite true
```

### 资源释放

Spark 任务结束后应主动释放 `SparkSession`，避免本地调试、单元测试或长生命周期进程中出现资源泄漏。生产 Spark 任务完成后，集群通常会回收资源，但代码中仍应使用 `try-finally` 保证释放逻辑。

推荐入口写法：

```java
package io.github.atengk.spark;

import io.github.atengk.spark.config.JobArgsParser;
import io.github.atengk.spark.config.JobRuntimeArgs;
import io.github.atengk.spark.config.SparkRuntimeConfig;
import io.github.atengk.spark.config.SparkSessionFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.SparkSession;

/**
 * Spark 应用程序入口。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class Application {

    /**
     * Spark 任务主入口。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SparkSession spark = null;

        try {
            JobRuntimeArgs runtimeArgs = JobArgsParser.parse(args);

            SparkRuntimeConfig sparkConfig = new SparkRuntimeConfig();
            sparkConfig.setAppName("spark-data-job-" + runtimeArgs.getBizDate());
            sparkConfig.setMaster("local".equals(runtimeArgs.getEnv()) ? "local[*]" : null);
            sparkConfig.setHiveEnabled(!"local".equals(runtimeArgs.getEnv()));
            sparkConfig.getSparkConf().put("spark.sql.adaptive.enabled", "true");
            sparkConfig.getSparkConf().put("spark.sql.shuffle.partitions", "local".equals(runtimeArgs.getEnv()) ? "4" : "400");

            spark = SparkSessionFactory.create(sparkConfig);

            log.info("Spark 任务开始执行，业务日期：{}", runtimeArgs.getBizDate());

            // TODO 在此调用具体 Job，例如 new UserSummaryJob().run(spark, runtimeArgs);

            log.info("Spark 任务执行完成，业务日期：{}", runtimeArgs.getBizDate());
        } catch (Exception e) {
            log.error("Spark 任务执行失败", e);
            throw e;
        } finally {
            if (spark != null) {
                spark.stop();
                log.info("SparkSession 已释放");
            }
        }
    }
}
```

资源释放注意事项：

| 注意点                        | 说明                                                         |
| ----------------------------- | ------------------------------------------------------------ |
| 使用 `finally` 释放           | 确保异常场景也能执行 `spark.stop()`                          |
| 不重复创建 SparkSession       | 一个任务进程内通常只创建一个 SparkSession                    |
| 不在工具类中关闭 SparkSession | 由应用入口或任务调度层统一释放                               |
| 流任务谨慎释放                | Structured Streaming 应等待 `awaitTermination()` 后再释放    |
| 测试用例及时释放              | 单元测试应在 `@AfterEach` 或 `@AfterAll` 中停止 SparkSession |

## 数据读取

本章节用于说明 Spark Java 项目读取不同数据源的常见方式。Spark 支持文本、CSV、JSON、Parquet、ORC、Hive、JDBC、Kafka 和自定义数据源。数据读取应统一封装，保证路径、格式、Schema、参数、异常和日志处理方式一致。

### 读取文本文件

文本文件读取适用于日志文件、简单分隔文件、原始文本数据和非结构化数据。Spark 可以使用 `spark.read().text()` 读取为 DataFrame，也可以使用 `JavaSparkContext.textFile()` 读取为 RDD。

DataFrame 方式读取文本文件：

```java
Dataset<Row> textDf = spark.read()
        .text("data/input/access.log");

textDf.show(false);
```

读取结果默认只有一个字段 `value`，每一行文本对应一条记录。

RDD 方式读取文本文件：

```java
JavaSparkContext javaSparkContext = JavaSparkContext.fromSparkContext(spark.sparkContext());

JavaRDD<String> lines = javaSparkContext.textFile("data/input/access.log");

long count = lines
        .filter(line -> line.contains("ERROR"))
        .count();
```

推荐使用场景：

| 方式                          | 场景                              |
| ----------------------------- | --------------------------------- |
| `spark.read().text()`         | 后续希望转为 DataFrame 处理       |
| `javaSparkContext.textFile()` | 需要使用 RDD API 进行底层文本处理 |
| `wholeTextFiles()`            | 一个文件整体作为一条记录处理      |

文本文件读取后通常需要配合 `split`、正则解析、JSON 解析或 UDF 进行结构化处理。

### 读取 CSV 文件

CSV 是常见的结构化文本格式。读取 CSV 时应明确分隔符、表头、编码、Schema、空值处理和异常记录策略。生产环境不建议完全依赖 Spark 自动推断 Schema，因为自动推断会增加一次扫描成本，也可能导致字段类型不稳定。

基础读取示例：

```java
Dataset<Row> csvDf = spark.read()
        .option("header", "true")
        .option("sep", ",")
        .option("encoding", "UTF-8")
        .option("mode", "PERMISSIVE")
        .csv("data/input/user.csv");

csvDf.show(false);
```

指定 Schema 读取示例：

```java
StructType schema = new StructType()
        .add("user_id", DataTypes.StringType, false)
        .add("user_name", DataTypes.StringType, true)
        .add("age", DataTypes.IntegerType, true)
        .add("create_time", DataTypes.StringType, true);

Dataset<Row> userDf = spark.read()
        .option("header", "true")
        .option("sep", ",")
        .schema(schema)
        .csv("data/input/user.csv");
```

常用 CSV 参数：

| 参数          | 说明                 |
| ------------- | -------------------- |
| `header`      | 是否包含表头         |
| `sep`         | 字段分隔符           |
| `encoding`    | 文件编码             |
| `quote`       | 引号字符             |
| `escape`      | 转义字符             |
| `multiLine`   | 是否支持多行字段     |
| `mode`        | 异常记录处理模式     |
| `inferSchema` | 是否自动推断字段类型 |

`mode` 常见取值：

| 取值            | 说明                             |
| --------------- | -------------------------------- |
| `PERMISSIVE`    | 容错读取，异常内容放入坏记录字段 |
| `DROPMALFORMED` | 丢弃格式异常记录                 |
| `FAILFAST`      | 遇到异常记录立即失败             |

生产任务建议显式指定 Schema，并对异常记录数量进行统计或落盘，避免脏数据被静默忽略。

### 读取 JSON 文件

JSON 文件适用于半结构化数据，例如埋点日志、接口日志、消息队列落盘数据等。Spark 支持读取一行一个 JSON 对象的文件，也支持配置 `multiLine` 读取多行 JSON。

基础读取示例：

```java
Dataset<Row> jsonDf = spark.read()
        .option("mode", "PERMISSIVE")
        .json("data/input/user.json");

jsonDf.printSchema();
jsonDf.show(false);
```

指定 Schema 读取示例：

```java
StructType schema = new StructType()
        .add("user_id", DataTypes.StringType, false)
        .add("event_type", DataTypes.StringType, true)
        .add("event_time", DataTypes.StringType, true)
        .add("properties", DataTypes.StringType, true);

Dataset<Row> eventDf = spark.read()
        .schema(schema)
        .option("mode", "PERMISSIVE")
        .json("data/input/event.json");
```

多行 JSON 读取示例：

```java
Dataset<Row> multiLineJsonDf = spark.read()
        .option("multiLine", "true")
        .json("data/input/config.json");
```

JSON 读取注意事项：

| 注意点              | 说明                                      |
| ------------------- | ----------------------------------------- |
| 推荐一行一个 JSON   | 更适合分布式读取                          |
| 避免复杂深层嵌套    | 深层嵌套会增加解析和字段处理复杂度        |
| 生产建议指定 Schema | 保证字段类型稳定                          |
| 注意字段缺失        | JSON 字段缺失时 Spark 会填充 null         |
| 关注坏数据          | 使用 `_corrupt_record` 或异常数据处理策略 |

### 读取 Parquet 文件

Parquet 是 Spark 中非常常用的列式存储格式，适合离线数仓、指标计算、宽表加工和大规模分析查询。Parquet 支持列裁剪和谓词下推，通常比 CSV 和 JSON 读取效率更高。

基础读取示例：

```java
Dataset<Row> parquetDf = spark.read()
        .parquet("hdfs:///warehouse/dwd/user_detail/dt=2026-05-11");

parquetDf.select("user_id", "user_name", "age").show(false);
```

读取多个路径：

```java
Dataset<Row> parquetDf = spark.read()
        .parquet(
                "hdfs:///warehouse/dwd/user_detail/dt=2026-05-10",
                "hdfs:///warehouse/dwd/user_detail/dt=2026-05-11"
        );
```

通过过滤条件触发谓词下推：

```java
Dataset<Row> resultDf = parquetDf
        .filter("age >= 18")
        .select("user_id", "age");
```

Parquet 读取建议：

| 建议                   | 说明                                 |
| ---------------------- | ------------------------------------ |
| 优先用于中间层和明细层 | 比 CSV、JSON 更适合分析型数据        |
| 读取时选择必要字段     | 减少 IO 和内存占用                   |
| 使用分区路径           | 例如 `dt=2026-05-11`，减少扫描范围   |
| 控制小文件数量         | 小文件过多会增加任务调度和元数据开销 |
| 注意 Schema 演进       | 字段新增、类型变更需要统一规范       |

### 读取 ORC 文件

ORC 也是常见列式存储格式，在 Hive 生态中使用较多。Spark 可以直接读取 ORC 文件，也可以通过 Hive 表间接读取 ORC 格式数据。

基础读取示例：

```java
Dataset<Row> orcDf = spark.read()
        .orc("hdfs:///warehouse/dwd/order_detail/dt=2026-05-11");

orcDf.show(false);
```

使用通用格式读取：

```java
Dataset<Row> orcDf = spark.read()
        .format("orc")
        .load("hdfs:///warehouse/dwd/order_detail/dt=2026-05-11");
```

ORC 读取适用场景：

| 场景                  | 说明                          |
| --------------------- | ----------------------------- |
| Hive 表底层格式为 ORC | 可直接读取 Hive 表或 ORC 路径 |
| 需要列式存储能力      | 支持列裁剪和压缩              |
| 离线批处理            | 适合大规模明细数据和宽表数据  |
| 与 Hive 兼容          | 在 Hive 生态中兼容性较好      |

Parquet 和 ORC 都适合离线分析。具体选择应以公司数仓规范、Hive 表格式和上下游工具兼容性为准。

### 读取 Hive 表

读取 Hive 表需要开启 Hive 支持，并确保 Spark 能访问 Hive Metastore。Hive 表读取适用于数仓分层加工、分区表处理、离线指标计算和宽表构建。

通过 `spark.table()` 读取：

```java
Dataset<Row> userDf = spark.table("dwd.dwd_user_detail")
        .filter("dt = '2026-05-11'");

userDf.show(false);
```

通过 SQL 读取：

```java
Dataset<Row> userDf = spark.sql(
        "SELECT user_id, user_name, age " +
        "FROM dwd.dwd_user_detail " +
        "WHERE dt = '2026-05-11'"
);
```

推荐使用参数拼接时先进行参数校验，不要直接拼接未校验的外部输入。示例：

```java
String bizDate = "2026-05-11";

if (!bizDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
    throw new IllegalArgumentException("业务日期格式不正确：" + bizDate);
}

Dataset<Row> userDf = spark.sql(String.format(
        "SELECT user_id, user_name, age FROM dwd.dwd_user_detail WHERE dt = '%s'",
        bizDate
));
```

Hive 表读取建议：

| 建议            | 说明                                       |
| --------------- | ------------------------------------------ |
| 必须带分区条件  | 大分区表不带分区过滤容易全表扫描           |
| 只选择必要字段  | 避免读取无关列                             |
| 避免 `SELECT *` | 生产 SQL 中不建议使用                      |
| 确认库表环境    | test 和 prod Hive Metastore 不要混用       |
| 注意权限        | 读取和写入 Hive 表都依赖 HDFS 和 Hive 权限 |

### 读取 JDBC 数据

Spark 可以通过 JDBC 读取 MySQL、PostgreSQL、Oracle、SQL Server 等关系型数据库。JDBC 读取适合小表维度数据、业务系统快照数据和数据同步任务。大表读取必须配置分区字段，否则单连接读取容易成为瓶颈。

基础读取示例：

```java
Dataset<Row> mysqlDf = spark.read()
        .format("jdbc")
        .option("url", "jdbc:mysql://127.0.0.1:3306/demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai")
        .option("driver", "com.mysql.cj.jdbc.Driver")
        .option("user", "root")
        .option("password", "123456")
        .option("dbtable", "user_info")
        .load();

mysqlDf.show(false);
```

使用 SQL 子查询读取：

```java
Dataset<Row> userDf = spark.read()
        .format("jdbc")
        .option("url", "jdbc:mysql://127.0.0.1:3306/demo")
        .option("driver", "com.mysql.cj.jdbc.Driver")
        .option("user", "root")
        .option("password", "123456")
        .option("dbtable", "(SELECT id, user_name, age FROM user_info WHERE status = 1) t")
        .load();
```

大表分区读取示例：

```java
Dataset<Row> userDf = spark.read()
        .format("jdbc")
        .option("url", "jdbc:mysql://127.0.0.1:3306/demo")
        .option("driver", "com.mysql.cj.jdbc.Driver")
        .option("user", "root")
        .option("password", "123456")
        .option("dbtable", "user_info")
        .option("partitionColumn", "id")
        .option("lowerBound", "1")
        .option("upperBound", "1000000")
        .option("numPartitions", "8")
        .load();
```

JDBC 读取注意事项：

| 注意点             | 说明                                                         |
| ------------------ | ------------------------------------------------------------ |
| 小表可直接读取     | 例如维表、配置表                                             |
| 大表必须分区读取   | 使用 `partitionColumn`、`lowerBound`、`upperBound`、`numPartitions` |
| 避免压垮业务库     | 控制并发连接数和读取时间窗口                                 |
| 使用只读账号       | 避免 Spark 任务误操作业务库                                  |
| 密码不写死         | 使用环境变量、调度参数或密钥系统                             |
| SQL 子查询需加别名 | `dbtable` 使用子查询时必须加别名                             |

生产环境读取业务数据库前，需要评估数据库压力。对于大规模数据同步，更推荐从数据库备库、CDC、数据湖或离线同步链路读取。

### 读取 Kafka 数据

Kafka 数据读取通常用于 Structured Streaming 实时任务，也可以使用批模式读取指定 Offset 范围内的数据。Spark 读取 Kafka 后，默认字段包括 `key`、`value`、`topic`、`partition`、`offset`、`timestamp` 等，其中 `key` 和 `value` 是二进制类型，通常需要转换为字符串或进一步解析 JSON。

Structured Streaming 读取 Kafka 示例：

```java
Dataset<Row> kafkaDf = spark.readStream()
        .format("kafka")
        .option("kafka.bootstrap.servers", "localhost:9092")
        .option("subscribe", "ods_user_event")
        .option("startingOffsets", "latest")
        .option("failOnDataLoss", "false")
        .load();

Dataset<Row> valueDf = kafkaDf.selectExpr(
        "CAST(key AS STRING) AS message_key",
        "CAST(value AS STRING) AS message_value",
        "topic",
        "partition",
        "offset",
        "timestamp"
);
```

解析 Kafka JSON 消息示例：

```java
StructType schema = new StructType()
        .add("user_id", DataTypes.StringType)
        .add("event_type", DataTypes.StringType)
        .add("event_time", DataTypes.StringType);

Dataset<Row> parsedDf = valueDf
        .select(functions.from_json(functions.col("message_value"), schema).alias("json_data"))
        .select("json_data.*");
```

批模式读取 Kafka 示例：

```java
Dataset<Row> kafkaBatchDf = spark.read()
        .format("kafka")
        .option("kafka.bootstrap.servers", "localhost:9092")
        .option("subscribe", "ods_user_event")
        .option("startingOffsets", "{\"ods_user_event\":{\"0\":100}}")
        .option("endingOffsets", "{\"ods_user_event\":{\"0\":200}}")
        .load();
```

Kafka 读取常用参数：

| 参数                      | 说明                                           |
| ------------------------- | ---------------------------------------------- |
| `kafka.bootstrap.servers` | Kafka Broker 地址                              |
| `subscribe`               | 订阅 Topic                                     |
| `assign`                  | 指定 Topic 和分区                              |
| `subscribePattern`        | 按正则订阅 Topic                               |
| `startingOffsets`         | 起始 Offset，支持 `earliest`、`latest` 或 JSON |
| `endingOffsets`           | 批读取结束 Offset                              |
| `failOnDataLoss`          | 数据丢失时是否失败                             |
| `maxOffsetsPerTrigger`    | 每个触发批次最大消费数量                       |

Kafka 流任务必须配置 Checkpoint，否则任务重启后无法稳定恢复 Offset 和状态。

### 自定义数据源读取

自定义数据源读取适用于统一封装公司内部数据源、特殊文件格式、接口数据落地结果、加密文件、复杂参数读取等场景。常见做法是在项目中定义统一 Reader 接口，然后按数据源类型提供不同实现。

文件位置：`src/main/java/io/github/atengk/spark/reader/SparkDataReader.java`

该接口用于定义统一的数据读取入口。

```java
package io.github.atengk.spark.reader;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.util.Map;

/**
 * Spark 数据读取接口。
 *
 * @author Ateng
 * @since 2026-05-11
 */
public interface SparkDataReader {

    /**
     * 读取数据。
     *
     * @param spark SparkSession
     * @param options 读取参数
     * @return Dataset<Row>
     */
    Dataset<Row> read(SparkSession spark, Map<String, String> options);
}
```

文件位置：`src/main/java/io/github/atengk/spark/reader/ParquetDataReader.java`

该实现类用于封装 Parquet 数据读取逻辑。

```java
package io.github.atengk.spark.reader;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.util.Map;

/**
 * Parquet 数据读取器。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class ParquetDataReader implements SparkDataReader {

    /**
     * 读取 Parquet 数据。
     *
     * @param spark SparkSession
     * @param options 读取参数
     * @return Dataset<Row>
     */
    @Override
    public Dataset<Row> read(SparkSession spark, Map<String, String> options) {
        String path = MapUtil.getStr(options, "path");
        if (StrUtil.isBlank(path)) {
            throw new IllegalArgumentException("Parquet 读取路径不能为空");
        }

        log.info("开始读取 Parquet 数据，路径：{}", path);
        Dataset<Row> dataset = spark.read().parquet(path);
        log.info("Parquet 数据读取完成，路径：{}", path);
        return dataset;
    }
}
```

文件位置：`src/main/java/io/github/atengk/spark/reader/JdbcDataReader.java`

该实现类用于封装 JDBC 数据读取逻辑。

```java
package io.github.atengk.spark.reader;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.util.Map;
import java.util.Properties;

/**
 * JDBC 数据读取器。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class JdbcDataReader implements SparkDataReader {

    /**
     * 读取 JDBC 数据。
     *
     * @param spark SparkSession
     * @param options 读取参数
     * @return Dataset<Row>
     */
    @Override
    public Dataset<Row> read(SparkSession spark, Map<String, String> options) {
        String url = required(options, "url");
        String dbtable = required(options, "dbtable");
        String user = required(options, "user");
        String password = required(options, "password");
        String driver = required(options, "driver");

        Properties properties = new Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", password);
        properties.setProperty("driver", driver);

        log.info("开始读取 JDBC 数据，表或查询：{}", dbtable);
        Dataset<Row> dataset = spark.read().jdbc(url, dbtable, properties);
        log.info("JDBC 数据读取完成，表或查询：{}", dbtable);
        return dataset;
    }

    /**
     * 获取必填参数。
     *
     * @param options 参数 Map
     * @param key 参数名
     * @return 参数值
     */
    private String required(Map<String, String> options, String key) {
        String value = MapUtil.getStr(options, key);
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException(StrUtil.format("缺少 JDBC 读取参数：{}", key));
        }
        return value;
    }
}
```

自定义 Reader 使用示例：

```java
Map<String, String> options = new HashMap<>();
options.put("path", "hdfs:///warehouse/dwd/user_detail/dt=2026-05-11");

SparkDataReader reader = new ParquetDataReader();
Dataset<Row> userDf = reader.read(spark, options);
```

自定义数据源读取建议：

| 建议                   | 说明                                     |
| ---------------------- | ---------------------------------------- |
| 定义统一接口           | Reader 对外暴露统一 `read` 方法          |
| 参数集中校验           | 在读取前校验路径、表名、连接地址等必填项 |
| 日志记录路径和表名     | 便于排查数据来源                         |
| 不在日志中打印密码     | JDBC、Kafka、对象存储密钥必须脱敏        |
| 按格式拆分实现类       | Parquet、JDBC、Kafka、Hive 分别实现      |
| 业务层不直接拼读取细节 | 业务 Job 只关心读取结果和转换逻辑        |

## 数据清洗

本章节用于说明 Spark 任务中常见的数据清洗方式。数据清洗的目标是将原始数据转换为结构稳定、字段规范、质量可控的中间数据，为后续 Join、聚合、指标计算和数据写出提供可靠基础。该部分对应你上传的大纲中“数据清洗”和“数据转换”章节。

### 空值处理

空值处理用于解决字段缺失、字段为空字符串、字段为非法占位值等问题。Spark 中常见空值包括 `null`、空字符串、字符串 `"null"`、字符串 `"NULL"`、`"N/A"`、`"-"` 等。处理方式应根据字段业务含义决定，不能简单地对所有字段统一填充。

常见处理策略如下：

| 策略           | 适用场景                                    |
| -------------- | ------------------------------------------- |
| 删除空值记录   | 主键、业务日期、核心关联字段为空时          |
| 默认值填充     | 状态、数值、布尔标识等字段为空时            |
| 置为标准空值   | 原始值为 `""`、`"null"`、`"N/A"` 等占位值时 |
| 单独落脏数据   | 核心字段为空但需要追踪来源时                |
| 按业务规则推导 | 可以通过其他字段推导缺失字段时              |

基础示例：

```java
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.lit;
import static org.apache.spark.sql.functions.when;

Dataset<Row> cleanedDf = sourceDf
        // 用户 ID 为空的数据不可用于后续计算，直接过滤
        .filter(col("user_id").isNotNull())
        // 年龄为空时填充为 0
        .withColumn("age", when(col("age").isNull(), lit(0)).otherwise(col("age")))
        // 状态为空时填充为 UNKNOWN
        .withColumn("status", when(col("status").isNull(), lit("UNKNOWN")).otherwise(col("status")));
```

如果只是简单填充固定默认值，可以使用 `na().fill()`：

```java
Map<String, Object> fillMap = new HashMap<>();
fillMap.put("age", 0);
fillMap.put("status", "UNKNOWN");
fillMap.put("city", "未知");

Dataset<Row> cleanedDf = sourceDf.na().fill(fillMap);
```

空值处理建议如下：

| 字段类型 | 推荐处理                                     |
| -------- | -------------------------------------------- |
| 主键字段 | 为空时过滤或进入脏数据表                     |
| 维度字段 | 可填充为 `UNKNOWN`、`其他`、`未知`           |
| 数值字段 | 按业务含义填充为 `0` 或保留 null             |
| 时间字段 | 不建议随意填充当前时间，应进入异常数据处理   |
| 分区字段 | 必须保证非空，否则写出分区会异常或产生脏分区 |

### 重复数据处理

重复数据处理用于解决数据重复采集、重复同步、Kafka 重复消费、接口重复推送、上游补偿重发等问题。去重前必须明确去重口径，是按主键去重、按业务唯一键去重，还是按事件 ID 去重。

常见去重方式如下：

| 去重方式     | 说明                               |
| ------------ | ---------------------------------- |
| 全字段去重   | 所有字段完全相同才认为重复         |
| 指定字段去重 | 按主键、订单号、事件 ID 等字段去重 |
| 窗口去重     | 同一业务键下保留最新或最早一条     |
| 增量去重     | 与历史结果表比对，只保留新增数据   |

全字段去重：

```java
Dataset<Row> distinctDf = sourceDf.distinct();
```

指定字段去重：

```java
Dataset<Row> deduplicateDf = sourceDf.dropDuplicates("user_id", "event_id");
```

按业务键保留最新记录：

```java
import org.apache.spark.sql.expressions.Window;
import org.apache.spark.sql.expressions.WindowSpec;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.row_number;

WindowSpec windowSpec = Window
        .partitionBy(col("user_id"), col("event_id"))
        .orderBy(col("event_time").desc());

Dataset<Row> latestDf = sourceDf
        .withColumn("rn", row_number().over(windowSpec))
        .filter(col("rn").equalTo(1))
        .drop("rn");
```

去重注意事项：

| 注意点                 | 说明                                              |
| ---------------------- | ------------------------------------------------- |
| 明确去重键             | 不要在没有业务口径的情况下直接 `distinct()`       |
| 注意 Shuffle 成本      | `distinct` 和 `dropDuplicates` 通常会产生 Shuffle |
| 保留最新记录需排序     | 需要使用窗口函数明确保留规则                      |
| 流任务去重需 Watermark | Structured Streaming 中去重必须控制状态大小       |
| 去重前统一字段格式     | 大小写、空格、时间格式不统一会影响去重结果        |

### 异常数据过滤

异常数据过滤用于剔除不符合业务规则、字段格式错误、数值越界、状态非法、时间非法的数据。异常数据不建议直接丢弃，生产任务通常需要将异常数据单独输出，便于追踪上游质量问题。

常见异常规则如下：

| 规则     | 示例                                        |
| -------- | ------------------------------------------- |
| 主键非法 | `user_id` 为空、长度不符合要求              |
| 数值越界 | 年龄小于 0 或大于 150                       |
| 枚举非法 | 状态不在 `ACTIVE`、`DISABLED`、`DELETED` 中 |
| 时间非法 | 事件时间为空或晚于当前处理时间              |
| 金额非法 | 金额为负数或精度异常                        |
| 格式非法 | 手机号、邮箱、证件号格式不符合要求          |

正常数据过滤示例：

```java
import static org.apache.spark.sql.functions.col;

Dataset<Row> validDf = sourceDf
        .filter(col("user_id").isNotNull())
        .filter(col("age").geq(0).and(col("age").leq(150)))
        .filter(col("status").isin("ACTIVE", "DISABLED", "DELETED"));
```

异常数据单独输出示例：

```java
Dataset<Row> invalidDf = sourceDf
        .filter(
                col("user_id").isNull()
                        .or(col("age").lt(0))
                        .or(col("age").gt(150))
                        .or(not(col("status").isin("ACTIVE", "DISABLED", "DELETED")))
        );

invalidDf.write()
        .mode("append")
        .parquet("hdfs:///warehouse/error/user_invalid/dt=2026-05-11");
```

如果需要给异常数据增加原因字段，可以使用 `when`：

```java
import static org.apache.spark.sql.functions.lit;
import static org.apache.spark.sql.functions.when;

Dataset<Row> invalidReasonDf = sourceDf.withColumn(
        "error_reason",
        when(col("user_id").isNull(), lit("用户ID为空"))
                .when(col("age").lt(0).or(col("age").gt(150)), lit("年龄范围非法"))
                .when(not(col("status").isin("ACTIVE", "DISABLED", "DELETED")), lit("状态非法"))
                .otherwise(lit(null))
);
```

### 字段类型转换

字段类型转换用于将原始字符串、数值、时间等字段转换为标准类型。CSV、JSON、Kafka 等数据源读取后，字段经常是字符串类型，后续计算前需要转换为 `IntegerType`、`LongType`、`DecimalType`、`TimestampType`、`DateType` 等。

基础类型转换示例：

```java
import static org.apache.spark.sql.functions.col;

Dataset<Row> typedDf = sourceDf
        .withColumn("age", col("age").cast("int"))
        .withColumn("amount", col("amount").cast("decimal(18,2)"))
        .withColumn("create_time", col("create_time").cast("timestamp"))
        .withColumn("is_active", col("is_active").cast("boolean"));
```

时间字符串转换示例：

```java
import static org.apache.spark.sql.functions.to_date;
import static org.apache.spark.sql.functions.to_timestamp;

Dataset<Row> timeDf = sourceDf
        .withColumn("event_timestamp", to_timestamp(col("event_time"), "yyyy-MM-dd HH:mm:ss"))
        .withColumn("event_date", to_date(col("event_time"), "yyyy-MM-dd HH:mm:ss"));
```

类型转换建议如下：

| 原始类型   | 目标类型             | 说明                              |
| ---------- | -------------------- | --------------------------------- |
| 字符串金额 | `decimal(18,2)`      | 避免使用 double 造成精度问题      |
| 字符串时间 | `timestamp` / `date` | 必须明确时间格式                  |
| 字符串标识 | `boolean` / `int`    | 根据业务枚举统一映射              |
| ID 字段    | `string` 或 `long`   | 超长 ID 建议保留字符串            |
| 分区日期   | `string`             | Hive 分区字段通常使用字符串更稳定 |

生产任务中不建议直接转换后忽略失败记录。类型转换失败会产生 null，应配合异常数据校验处理。

### 字段裁剪

字段裁剪用于减少无关字段传递，降低内存、网络和磁盘 IO 成本。Spark SQL 对 Parquet、ORC 等列式存储支持列裁剪，越早选择必要字段，后续处理成本越低。

字段裁剪示例：

```java
Dataset<Row> selectedDf = sourceDf.select(
        col("user_id"),
        col("user_name"),
        col("age"),
        col("event_time")
);
```

基于 SQL 的字段裁剪：

```java
sourceDf.createOrReplaceTempView("ods_user_event");

Dataset<Row> selectedDf = spark.sql(
        "SELECT user_id, user_name, age, event_time " +
        "FROM ods_user_event " +
        "WHERE dt = '2026-05-11'"
);
```

字段裁剪建议如下：

| 建议               | 说明                                             |
| ------------------ | ------------------------------------------------ |
| 尽早裁剪字段       | 读取后尽快保留必要字段                           |
| 避免 `SELECT *`    | 生产 SQL 中不建议全字段查询                      |
| 保留审计字段       | 如 `dt`、`source_system`、`etl_time`、`batch_id` |
| 写出前统一字段顺序 | 便于下游消费和表结构管理                         |
| 大宽表分阶段裁剪   | Join 前后分别控制字段规模                        |

### 字段重命名

字段重命名用于统一字段命名规范，解决上游字段名称不一致、大小写混用、业务字段含义不清晰等问题。字段命名建议使用小写字母加下划线，例如 `user_id`、`event_time`、`order_amount`。

单字段重命名：

```java
Dataset<Row> renamedDf = sourceDf
        .withColumnRenamed("userId", "user_id")
        .withColumnRenamed("userName", "user_name")
        .withColumnRenamed("createTime", "create_time");
```

使用 `selectExpr` 重命名：

```java
Dataset<Row> renamedDf = sourceDf.selectExpr(
        "userId AS user_id",
        "userName AS user_name",
        "createTime AS create_time"
);
```

字段重命名建议如下：

| 场景         | 建议                                            |
| ------------ | ----------------------------------------------- |
| 驼峰字段     | 转换为下划线字段                                |
| 上游缩写字段 | 转换为业务含义明确的字段                        |
| 多表 Join 前 | 先处理同名字段，避免字段冲突                    |
| 写 Hive 表前 | 字段名与目标表结构严格一致                      |
| 维表字段     | 增加业务前缀，例如 `city_name`、`category_name` |

### 时间字段处理

时间字段处理用于统一事件时间、创建时间、更新时间、分区日期和处理时间。时间字段是离线分区、实时窗口、增量计算和数据质量校验的关键字段，必须保持格式稳定。

常见时间处理如下：

```java
import static org.apache.spark.sql.functions.current_timestamp;
import static org.apache.spark.sql.functions.date_format;
import static org.apache.spark.sql.functions.to_date;
import static org.apache.spark.sql.functions.to_timestamp;

Dataset<Row> timeDf = sourceDf
        // 字符串转时间戳
        .withColumn("event_time", to_timestamp(col("event_time"), "yyyy-MM-dd HH:mm:ss"))
        // 提取事件日期
        .withColumn("event_date", to_date(col("event_time")))
        // 生成 Hive 分区日期
        .withColumn("dt", date_format(col("event_time"), "yyyy-MM-dd"))
        // 增加 ETL 处理时间
        .withColumn("etl_time", current_timestamp());
```

时间字段处理建议如下：

| 字段           | 说明               |
| -------------- | ------------------ |
| `event_time`   | 业务事件发生时间   |
| `create_time`  | 业务数据创建时间   |
| `update_time`  | 业务数据更新时间   |
| `etl_time`     | Spark 任务处理时间 |
| `dt`           | 离线分区日期       |
| `window_start` | 流式窗口开始时间   |
| `window_end`   | 流式窗口结束时间   |

注意事项：

| 注意点                       | 说明                                       |
| ---------------------------- | ------------------------------------------ |
| 明确时区                     | 跨地区业务必须统一时区                     |
| 不随意用当前时间替代事件时间 | 否则会影响分区和指标口径                   |
| 时间格式统一                 | 推荐 `yyyy-MM-dd HH:mm:ss` 和 `yyyy-MM-dd` |
| 分区日期单独生成             | 不建议直接依赖原始字符串字段               |
| 迟到数据单独处理             | 流任务需要配合 Watermark                   |

### 数据标准化

数据标准化用于统一字段格式、大小写、空格、枚举值、编码、手机号、邮箱、地区名称等内容。标准化处理应在数据进入明细层或宽表层前完成，避免下游重复处理。

常见标准化示例：

```java
import static org.apache.spark.sql.functions.lower;
import static org.apache.spark.sql.functions.regexp_replace;
import static org.apache.spark.sql.functions.trim;
import static org.apache.spark.sql.functions.upper;

Dataset<Row> standardDf = sourceDf
        // 去除用户名称首尾空格
        .withColumn("user_name", trim(col("user_name")))
        // 邮箱统一小写
        .withColumn("email", lower(trim(col("email"))))
        // 状态统一大写
        .withColumn("status", upper(trim(col("status"))))
        // 手机号去除空格和横线
        .withColumn("mobile", regexp_replace(col("mobile"), "[\\s-]", ""));
```

枚举值标准化示例：

```java
Dataset<Row> statusDf = sourceDf.withColumn(
        "status",
        when(col("status").isin("1", "ACTIVE", "active", "启用"), lit("ACTIVE"))
                .when(col("status").isin("0", "DISABLED", "disabled", "禁用"), lit("DISABLED"))
                .otherwise(lit("UNKNOWN"))
);
```

数据标准化建议如下：

| 类型   | 建议                        |
| ------ | --------------------------- |
| 字符串 | 统一 trim，必要时统一大小写 |
| 手机号 | 去除空格、横线，校验长度    |
| 邮箱   | 统一小写并校验格式          |
| 枚举   | 映射为标准枚举值            |
| 地区   | 使用维表进行标准化映射      |
| 金额   | 统一精度和币种              |
| 时间   | 统一时区和格式              |

### 数据质量校验

数据质量校验用于在数据清洗后检查数据是否满足业务和技术要求。质量校验不应只在任务失败后人工排查，而应作为 Spark 任务中的固定步骤。

常见校验维度如下：

| 校验维度   | 说明                         |
| ---------- | ---------------------------- |
| 完整性     | 必填字段是否为空             |
| 唯一性     | 主键或业务唯一键是否重复     |
| 准确性     | 字段值是否符合业务范围       |
| 一致性     | 多字段之间是否符合约束       |
| 时效性     | 数据日期是否符合任务处理日期 |
| 波动性     | 数据量是否较历史异常增减     |
| 参照完整性 | 事实表字段是否能关联到维表   |

简单质量校验示例：

```java
long totalCount = cleanedDf.count();

long nullUserCount = cleanedDf
        .filter(col("user_id").isNull())
        .count();

long duplicateCount = cleanedDf.count()
        - cleanedDf.dropDuplicates("user_id", "event_id").count();

if (totalCount == 0) {
    throw new IllegalStateException("清洗后数据为空");
}

if (nullUserCount > 0) {
    throw new IllegalStateException("存在用户ID为空的数据，数量：" + nullUserCount);
}

if (duplicateCount > 0) {
    throw new IllegalStateException("存在重复数据，数量：" + duplicateCount);
}
```

封装质量校验类时，可以集中记录日志并抛出异常。

文件位置：`src/main/java/io/github/atengk/spark/quality/DataQualityChecker.java`

该类用于封装数据质量校验逻辑，适合在每个 Job 的关键节点调用。

```java
package io.github.atengk.spark.quality;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.util.List;

import static org.apache.spark.sql.functions.col;

/**
 * Spark 数据质量校验工具。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class DataQualityChecker {

    private DataQualityChecker() {
    }

    /**
     * 校验数据集不能为空。
     *
     * @param dataset 数据集
     * @param datasetName 数据集名称
     */
    public static void checkNotEmpty(Dataset<Row> dataset, String datasetName) {
        long count = dataset.count();
        log.info("数据质量校验：{} 数据量为 {}", datasetName, count);
        if (count == 0) {
            throw new IllegalStateException(StrUtil.format("{} 数据为空", datasetName));
        }
    }

    /**
     * 校验必填字段不能为空。
     *
     * @param dataset 数据集
     * @param datasetName 数据集名称
     * @param requiredColumns 必填字段
     */
    public static void checkRequiredColumns(Dataset<Row> dataset, String datasetName, List<String> requiredColumns) {
        if (CollUtil.isEmpty(requiredColumns)) {
            log.warn("数据质量校验：{} 未配置必填字段", datasetName);
            return;
        }

        for (String column : requiredColumns) {
            long nullCount = dataset.filter(col(column).isNull()).count();
            log.info("数据质量校验：{} 字段 {} 空值数量为 {}", datasetName, column, nullCount);
            if (nullCount > 0) {
                throw new IllegalStateException(StrUtil.format("{} 字段 {} 存在空值，数量：{}", datasetName, column, nullCount));
            }
        }
    }

    /**
     * 校验指定字段组合唯一。
     *
     * @param dataset 数据集
     * @param datasetName 数据集名称
     * @param uniqueColumns 唯一键字段
     */
    public static void checkUnique(Dataset<Row> dataset, String datasetName, String... uniqueColumns) {
        long totalCount = dataset.count();
        long uniqueCount = dataset.dropDuplicates(uniqueColumns).count();
        long duplicateCount = totalCount - uniqueCount;

        log.info("数据质量校验：{} 总数 {}，唯一数 {}，重复数 {}",
                datasetName, totalCount, uniqueCount, duplicateCount);

        if (duplicateCount > 0) {
            throw new IllegalStateException(StrUtil.format("{} 存在重复数据，重复数量：{}", datasetName, duplicateCount));
        }
    }
}
```

使用示例：

```java
DataQualityChecker.checkNotEmpty(cleanedDf, "用户清洗结果");
DataQualityChecker.checkRequiredColumns(cleanedDf, "用户清洗结果", Arrays.asList("user_id", "event_time", "dt"));
DataQualityChecker.checkUnique(cleanedDf, "用户清洗结果", "user_id", "event_id");
```

## 数据转换

本章节用于说明 Spark 中常用的数据转换方式，包括 DataFrame、Dataset、SQL、UDF、UDAF、窗口函数、Join、聚合、排序和分区操作。数据转换是 Spark 任务的核心部分，应优先选择可读性高、优化器支持好的方式实现业务逻辑。

### DataFrame 常用转换

DataFrame 是 Java Spark 项目中最常用的数据抽象，适合处理结构化数据。常见转换包括字段选择、过滤、新增字段、字段删除、Join、聚合和排序。

基础转换示例：

```java
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.current_timestamp;
import static org.apache.spark.sql.functions.lit;
import static org.apache.spark.sql.functions.when;

Dataset<Row> resultDf = sourceDf
        // 选择必要字段
        .select("user_id", "user_name", "age", "status", "event_time")
        // 过滤有效用户
        .filter(col("user_id").isNotNull())
        // 新增成年人标识
        .withColumn("adult_flag", when(col("age").geq(18), lit(1)).otherwise(lit(0)))
        // 增加处理时间
        .withColumn("etl_time", current_timestamp())
        // 删除不需要字段
        .drop("status");
```

DataFrame 常用 API：

| API                 | 说明               |
| ------------------- | ------------------ |
| `select`            | 选择字段           |
| `selectExpr`        | 使用表达式选择字段 |
| `filter` / `where`  | 过滤数据           |
| `withColumn`        | 新增或替换字段     |
| `withColumnRenamed` | 字段重命名         |
| `drop`              | 删除字段           |
| `dropDuplicates`    | 去重               |
| `join`              | 数据关联           |
| `groupBy`           | 分组聚合           |
| `orderBy`           | 排序               |
| `repartition`       | 重新分区           |
| `coalesce`          | 减少分区           |

推荐将复杂转换封装到 Transformer 类中。

文件位置：`src/main/java/io/github/atengk/spark/transformer/UserEventTransformer.java`

该类用于封装用户事件数据的清洗和转换逻辑。

```java
package io.github.atengk.spark.transformer;

import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.current_timestamp;
import static org.apache.spark.sql.functions.lit;
import static org.apache.spark.sql.functions.to_timestamp;
import static org.apache.spark.sql.functions.when;

/**
 * 用户事件数据转换器。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserEventTransformer {

    /**
     * 清洗并转换用户事件数据。
     *
     * @param sourceDf 原始用户事件数据
     * @return 转换后的用户事件数据
     */
    public Dataset<Row> transform(Dataset<Row> sourceDf) {
        log.info("开始转换用户事件数据");

        Dataset<Row> resultDf = sourceDf
                .filter(col("user_id").isNotNull())
                .withColumn("event_time", to_timestamp(col("event_time"), "yyyy-MM-dd HH:mm:ss"))
                .withColumn("event_valid_flag", when(col("event_time").isNotNull(), lit(1)).otherwise(lit(0)))
                .withColumn("etl_time", current_timestamp())
                .select(
                        col("user_id"),
                        col("event_type"),
                        col("event_time"),
                        col("event_valid_flag"),
                        col("etl_time")
                );

        log.info("用户事件数据转换完成");
        return resultDf;
    }
}
```

### Dataset 常用转换

Dataset 是强类型数据集，Java 中可以通过 Bean 和 Encoder 使用。相比 `Dataset<Row>`，强类型 Dataset 在字段访问上更清晰，但 Java 写法相对繁琐。常规 Spark Java 项目中，Dataset 更多用于小范围强类型处理。

先定义 Bean 对象。

文件位置：`src/main/java/io/github/atengk/spark/model/UserEvent.java`

该类用于承载用户事件强类型数据。

```java
package io.github.atengk.spark.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户事件数据模型。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
public class UserEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID。
     */
    private String userId;

    /**
     * 事件类型。
     */
    private String eventType;

    /**
     * 事件时间。
     */
    private String eventTime;
}
```

Dataset 转换示例：

```java
import io.github.atengk.spark.model.UserEvent;
import org.apache.spark.api.java.function.FilterFunction;
import org.apache.spark.sql.Encoders;

Dataset<UserEvent> userEventDs = sourceDf
        .selectExpr(
                "user_id AS userId",
                "event_type AS eventType",
                "event_time AS eventTime"
        )
        .as(Encoders.bean(UserEvent.class));

Dataset<UserEvent> validDs = userEventDs.filter(
        (FilterFunction<UserEvent>) event -> event.getUserId() != null && event.getEventType() != null
);
```

Dataset 使用建议：

| 场景                 | 建议                      |
| -------------------- | ------------------------- |
| 常规结构化加工       | 优先使用 `Dataset<Row>`   |
| 强类型业务处理       | 可以使用 `Dataset<Bean>`  |
| 复杂 SQL 聚合        | 优先使用 Spark SQL        |
| 大规模性能敏感任务   | 注意 Java Bean 编解码成本 |
| 与 Java 业务对象集成 | Dataset Bean 可提升可读性 |

### SQL 查询转换

SQL 查询转换适合表达复杂字段计算、Join、聚合、窗口函数和指标统计。对于数据开发团队，SQL 可读性通常高于链式 DataFrame API。Spark Java 项目可以将 SQL 文件外置，减少 Java 代码中的长 SQL 字符串。

临时视图示例：

```java
userDf.createOrReplaceTempView("dwd_user");
orderDf.createOrReplaceTempView("dwd_order");

Dataset<Row> resultDf = spark.sql(
        "SELECT " +
        "  u.user_id, " +
        "  u.user_name, " +
        "  COUNT(o.order_id) AS order_count, " +
        "  SUM(o.order_amount) AS order_amount " +
        "FROM dwd_user u " +
        "LEFT JOIN dwd_order o ON u.user_id = o.user_id " +
        "GROUP BY u.user_id, u.user_name"
);
```

外部 SQL 文件示例：

```sql
-- src/main/resources/sql/ads_user_order_summary.sql
-- 用户订单汇总指标
SELECT
    u.user_id,
    u.user_name,
    COUNT(o.order_id) AS order_count,
    COALESCE(SUM(o.order_amount), 0) AS order_amount,
    '${bizDate}' AS dt
FROM dwd_user u
LEFT JOIN dwd_order o
    ON u.user_id = o.user_id
WHERE u.dt = '${bizDate}'
GROUP BY u.user_id, u.user_name
```

使用 Hutool 读取和替换 SQL 模板：

```java
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;

String sqlTemplate = ResourceUtil.readUtf8Str("sql/ads_user_order_summary.sql");

String sql = StrUtil.format(
        sqlTemplate.replace("${bizDate}", "{}"),
        "2026-05-11"
);

Dataset<Row> resultDf = spark.sql(sql);
```

SQL 查询转换建议如下：

| 建议                    | 说明                             |
| ----------------------- | -------------------------------- |
| 复杂逻辑优先 SQL        | 指标计算、Join、窗口函数更易读   |
| SQL 文件外置            | 避免 Java 代码中拼接大段 SQL     |
| 参数统一替换            | 业务日期、库名、表名通过模板注入 |
| 禁止未校验参数拼接      | 防止 SQL 逻辑错误和路径污染      |
| 执行前打印 SQL          | 生产日志中可打印脱敏后的 SQL     |
| 使用 `explain` 分析计划 | 排查 Join、Shuffle、扫描范围问题 |

### UDF 函数

UDF 用于处理 Spark 内置函数无法覆盖的自定义字段逻辑，例如手机号脱敏、复杂枚举映射、特殊字符串解析、业务规则判断等。UDF 应谨慎使用，因为它可能降低 Catalyst 优化能力。能使用 Spark 内置函数实现的逻辑，优先使用内置函数。

文件位置：`src/main/java/io/github/atengk/spark/udf/UserUdfRegister.java`

该类用于注册用户相关 UDF。

```java
package io.github.atengk.spark.udf;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.api.java.UDF1;
import org.apache.spark.sql.types.DataTypes;

/**
 * 用户相关 UDF 注册器。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserUdfRegister {

    private UserUdfRegister() {
    }

    /**
     * 注册用户相关 UDF。
     *
     * @param spark SparkSession
     */
    public static void register(SparkSession spark) {
        spark.udf().register("mobile_mask", mobileMask(), DataTypes.StringType);
        spark.udf().register("status_name", statusName(), DataTypes.StringType);
        log.info("用户相关 UDF 注册完成");
    }

    /**
     * 手机号脱敏 UDF。
     *
     * @return UDF1
     */
    private static UDF1<String, String> mobileMask() {
        return mobile -> {
            if (StrUtil.isBlank(mobile)) {
                return null;
            }
            return DesensitizedUtil.mobilePhone(mobile);
        };
    }

    /**
     * 状态名称转换 UDF。
     *
     * @return UDF1
     */
    private static UDF1<String, String> statusName() {
        return status -> {
            if (StrUtil.isBlank(status)) {
                return "未知";
            }
            switch (status) {
                case "ACTIVE":
                    return "启用";
                case "DISABLED":
                    return "禁用";
                case "DELETED":
                    return "删除";
                default:
                    return "未知";
            }
        };
    }
}
```

UDF 使用示例：

```java
UserUdfRegister.register(spark);

sourceDf.createOrReplaceTempView("dwd_user");

Dataset<Row> resultDf = spark.sql(
        "SELECT " +
        "  user_id, " +
        "  mobile_mask(mobile) AS mobile_mask, " +
        "  status_name(status) AS status_name " +
        "FROM dwd_user"
);
```

UDF 使用建议：

| 建议             | 说明                                  |
| ---------------- | ------------------------------------- |
| 优先使用内置函数 | 内置函数可被优化器更好优化            |
| UDF 保持简单     | 不要在 UDF 中访问数据库、调用远程接口 |
| 注意空值处理     | UDF 必须处理 null                     |
| 注意序列化       | UDF 中引用的对象必须可序列化          |
| 控制注册范围     | 按业务模块集中注册，避免名称冲突      |

### UDAF 函数

UDAF 用于自定义聚合逻辑，例如自定义去重统计、复杂评分聚合、特殊金额汇总、业务状态归并等。Spark Java 中实现 UDAF 相对复杂，常规聚合应优先使用内置聚合函数，例如 `count`、`sum`、`avg`、`max`、`min`、`countDistinct`。

内置聚合示例：

```java
import static org.apache.spark.sql.functions.avg;
import static org.apache.spark.sql.functions.count;
import static org.apache.spark.sql.functions.countDistinct;
import static org.apache.spark.sql.functions.sum;

Dataset<Row> aggDf = orderDf
        .groupBy("user_id")
        .agg(
                count("order_id").alias("order_count"),
                countDistinct("product_id").alias("product_count"),
                sum("order_amount").alias("total_amount"),
                avg("order_amount").alias("avg_amount")
        );
```

如果确实需要 UDAF，可以基于 `UserDefinedAggregateFunction` 实现。下面示例用于计算非空字符串数量。

文件位置：`src/main/java/io/github/atengk/spark/udaf/NonEmptyCountUdaf.java`

该类用于实现非空字符串数量统计 UDAF。

```java
package io.github.atengk.spark.udaf;

import cn.hutool.core.util.StrUtil;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.expressions.MutableAggregationBuffer;
import org.apache.spark.sql.expressions.UserDefinedAggregateFunction;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

/**
 * 非空字符串数量统计 UDAF。
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class NonEmptyCountUdaf extends UserDefinedAggregateFunction {

    /**
     * 输入字段结构。
     *
     * @return 输入 Schema
     */
    @Override
    public StructType inputSchema() {
        return DataTypes.createStructType(new StructField[]{
                DataTypes.createStructField("value", DataTypes.StringType, true)
        });
    }

    /**
     * 聚合缓冲区结构。
     *
     * @return 缓冲区 Schema
     */
    @Override
    public StructType bufferSchema() {
        return DataTypes.createStructType(new StructField[]{
                DataTypes.createStructField("count", DataTypes.LongType, false)
        });
    }

    /**
     * 返回值类型。
     *
     * @return 返回值类型
     */
    @Override
    public DataType dataType() {
        return DataTypes.LongType;
    }

    /**
     * 是否确定性函数。
     *
     * @return true 表示相同输入返回相同结果
     */
    @Override
    public boolean deterministic() {
        return true;
    }

    /**
     * 初始化缓冲区。
     *
     * @param buffer 聚合缓冲区
     */
    @Override
    public void initialize(MutableAggregationBuffer buffer) {
        buffer.update(0, 0L);
    }

    /**
     * 更新当前分区聚合缓冲区。
     *
     * @param buffer 聚合缓冲区
     * @param input 输入行
     */
    @Override
    public void update(MutableAggregationBuffer buffer, Row input) {
        String value = input.isNullAt(0) ? null : input.getString(0);
        if (StrUtil.isNotBlank(value)) {
            buffer.update(0, buffer.getLong(0) + 1L);
        }
    }

    /**
     * 合并不同分区的聚合结果。
     *
     * @param buffer1 聚合缓冲区 1
     * @param buffer2 聚合缓冲区 2
     */
    @Override
    public void merge(MutableAggregationBuffer buffer1, Row buffer2) {
        buffer1.update(0, buffer1.getLong(0) + buffer2.getLong(0));
    }

    /**
     * 输出最终聚合结果。
     *
     * @param buffer 聚合缓冲区
     * @return 聚合结果
     */
    @Override
    public Object evaluate(Row buffer) {
        return buffer.getLong(0);
    }
}
```

注册和使用：

```java
spark.udf().register("non_empty_count", new NonEmptyCountUdaf());

sourceDf.createOrReplaceTempView("user_event");

Dataset<Row> resultDf = spark.sql(
        "SELECT event_type, non_empty_count(user_id) AS valid_user_count " +
        "FROM user_event " +
        "GROUP BY event_type"
);
```

UDAF 使用建议：

| 建议             | 说明                       |
| ---------------- | -------------------------- |
| 优先使用内置聚合 | 内置函数性能和可维护性更好 |
| 聚合状态尽量小   | 避免缓冲区保存大量对象     |
| 注意空值和类型   | 明确输入 Schema 和返回类型 |
| 做好单元测试     | UDAF 逻辑错误不容易排查    |
| 避免复杂外部依赖 | 不要在 UDAF 中访问远程系统 |

### 窗口函数

窗口函数用于在分组范围内进行排序、排名、累计、取前后记录、去重保留最新记录等操作。常见函数包括 `row_number`、`rank`、`dense_rank`、`lag`、`lead`、`sum over` 等。

按用户保留最新事件：

```java
import org.apache.spark.sql.expressions.Window;
import org.apache.spark.sql.expressions.WindowSpec;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.row_number;

WindowSpec latestWindow = Window
        .partitionBy(col("user_id"))
        .orderBy(col("event_time").desc());

Dataset<Row> latestEventDf = eventDf
        .withColumn("rn", row_number().over(latestWindow))
        .filter(col("rn").equalTo(1))
        .drop("rn");
```

按用户计算订单累计金额：

```java
import static org.apache.spark.sql.functions.sum;

WindowSpec amountWindow = Window
        .partitionBy(col("user_id"))
        .orderBy(col("order_time").asc())
        .rowsBetween(Window.unboundedPreceding(), Window.currentRow());

Dataset<Row> amountDf = orderDf
        .withColumn("cum_amount", sum(col("order_amount")).over(amountWindow));
```

SQL 窗口函数示例：

```sql
SELECT
    user_id,
    order_id,
    order_time,
    order_amount,
    ROW_NUMBER() OVER(PARTITION BY user_id ORDER BY order_time DESC) AS rn,
    SUM(order_amount) OVER(
        PARTITION BY user_id
        ORDER BY order_time
        ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
    ) AS cum_amount
FROM dwd_order
WHERE dt = '${bizDate}'
```

窗口函数注意事项：

| 注意点                     | 说明                                 |
| -------------------------- | ------------------------------------ |
| `partitionBy` 控制分组范围 | 分组字段选择不当会导致数据倾斜       |
| `orderBy` 控制窗口顺序     | 排序字段必须符合业务规则             |
| 窗口函数通常会 Shuffle     | 大数据量下需要关注性能               |
| 去重推荐 `row_number`      | 可以明确保留最新或最早记录           |
| 累计计算要明确窗口范围     | 使用 `rowsBetween` 或 `rangeBetween` |

### Join 操作

Join 用于关联事实表、维度表、明细表和指标表，是 Spark 任务中最常见也最容易产生性能问题的操作之一。Join 前应明确关联键、关联类型、字段裁剪、数据量大小和是否存在数据倾斜。

常见 Join 类型：

| Join 类型   | 说明                                    |
| ----------- | --------------------------------------- |
| `inner`     | 只保留两边都匹配的数据                  |
| `left`      | 保留左表全部数据，右表匹配不到则为 null |
| `right`     | 保留右表全部数据                        |
| `full`      | 保留两边全部数据                        |
| `left_semi` | 只保留左表中能匹配右表的数据            |
| `left_anti` | 只保留左表中不能匹配右表的数据          |

基础 Join 示例：

```java
Dataset<Row> resultDf = userDf
        .join(orderDf, userDf.col("user_id").equalTo(orderDf.col("user_id")), "left")
        .select(
                userDf.col("user_id"),
                userDf.col("user_name"),
                orderDf.col("order_id"),
                orderDf.col("order_amount")
        );
```

小维表广播 Join 示例：

```java
import static org.apache.spark.sql.functions.broadcast;

Dataset<Row> resultDf = factDf
        .join(broadcast(dimCityDf), factDf.col("city_id").equalTo(dimCityDf.col("city_id")), "left")
        .select(
                factDf.col("user_id"),
                factDf.col("city_id"),
                dimCityDf.col("city_name")
        );
```

Join 前建议进行字段裁剪：

```java
Dataset<Row> factSelectedDf = factDf.select("user_id", "city_id", "order_amount");
Dataset<Row> citySelectedDf = dimCityDf.select("city_id", "city_name");

Dataset<Row> resultDf = factSelectedDf
        .join(broadcast(citySelectedDf), "city_id");
```

Join 操作建议如下：

| 建议                    | 说明                         |
| ----------------------- | ---------------------------- |
| Join 前裁剪字段         | 减少 Shuffle 数据量          |
| 小表使用广播 Join       | 减少大表 Shuffle             |
| 关联键提前标准化        | 避免空格、大小写、类型不一致 |
| 避免重复字段冲突        | Join 后显式 select 字段      |
| 大表 Join 关注倾斜      | 热点 key 需要特殊处理        |
| 使用 `left_anti` 查差异 | 适合找未匹配数据             |

### 聚合操作

聚合操作用于统计数量、金额、均值、最大值、最小值、去重数和复杂指标。聚合通常会产生 Shuffle，应合理选择分组字段和分区数。

DataFrame 聚合示例：

```java
import static org.apache.spark.sql.functions.avg;
import static org.apache.spark.sql.functions.count;
import static org.apache.spark.sql.functions.countDistinct;
import static org.apache.spark.sql.functions.max;
import static org.apache.spark.sql.functions.min;
import static org.apache.spark.sql.functions.sum;

Dataset<Row> summaryDf = orderDf
        .groupBy("dt", "user_id")
        .agg(
                count("order_id").alias("order_count"),
                countDistinct("product_id").alias("product_count"),
                sum("order_amount").alias("total_amount"),
                avg("order_amount").alias("avg_amount"),
                max("order_amount").alias("max_amount"),
                min("order_amount").alias("min_amount")
        );
```

SQL 聚合示例：

```sql
SELECT
    dt,
    user_id,
    COUNT(order_id) AS order_count,
    COUNT(DISTINCT product_id) AS product_count,
    SUM(order_amount) AS total_amount,
    AVG(order_amount) AS avg_amount,
    MAX(order_amount) AS max_amount,
    MIN(order_amount) AS min_amount
FROM dwd_order
WHERE dt = '${bizDate}'
GROUP BY dt, user_id
```

聚合操作建议如下：

| 建议                   | 说明                        |
| ---------------------- | --------------------------- |
| 分组字段不宜过多       | 过多会导致结果膨胀          |
| 避免高基数字段随意分组 | 如订单号、请求 ID           |
| 大规模去重谨慎使用     | `countDistinct` 成本较高    |
| 聚合前先过滤           | 减少参与聚合的数据量        |
| 聚合后校验数据量       | 防止分组口径错误            |
| 关注数据倾斜           | 单个 key 数据过大时需要优化 |

### 排序操作

排序操作用于生成 TopN、排名、全局排序、分区内排序等结果。全局排序通常成本较高，因为需要 Shuffle 和全局比较。生产任务中应尽量避免对大规模数据进行不必要的全局排序。

全局排序示例：

```java
Dataset<Row> sortedDf = orderDf.orderBy(col("order_amount").desc());
```

分区内排序示例：

```java
Dataset<Row> sortedWithinDf = orderDf
        .repartition(col("dt"))
        .sortWithinPartitions(col("order_time").asc());
```

TopN 示例：

```java
Dataset<Row> top10Df = orderDf
        .orderBy(col("order_amount").desc())
        .limit(10);
```

分组 TopN 推荐使用窗口函数：

```java
WindowSpec windowSpec = Window
        .partitionBy(col("dt"))
        .orderBy(col("order_amount").desc());

Dataset<Row> topNByDateDf = orderDf
        .withColumn("rn", row_number().over(windowSpec))
        .filter(col("rn").leq(10))
        .drop("rn");
```

排序操作建议如下：

| 场景         | 建议                                   |
| ------------ | -------------------------------------- |
| 全局 TopN    | 使用 `orderBy + limit`，但控制数据规模 |
| 分组 TopN    | 使用窗口函数                           |
| 写出前排序   | 优先使用 `sortWithinPartitions`        |
| 大表全局排序 | 谨慎使用，成本很高                     |
| 排序字段为空 | 先处理 null 排序规则                   |
| 多字段排序   | 明确主排序和次排序字段                 |

### 分区操作

分区操作用于控制并行度、Shuffle 后数据分布、输出文件数量和任务性能。Spark 中常见分区操作包括 `repartition`、`coalesce`、按字段分区写出和读取分区表。

增加或重排分区：

```java
Dataset<Row> repartitionDf = sourceDf.repartition(200);
```

按字段重新分区：

```java
Dataset<Row> repartitionByDateDf = sourceDf.repartition(col("dt"));
```

减少分区：

```java
Dataset<Row> coalesceDf = sourceDf.coalesce(20);
```

分区写出示例：

```java
sourceDf.write()
        .mode("overwrite")
        .partitionBy("dt")
        .parquet("hdfs:///warehouse/ads/user_summary");
```

分区操作区别：

| 操作                   | 说明                                   |
| ---------------------- | -------------------------------------- |
| `repartition`          | 可增加或减少分区，通常会产生 Shuffle   |
| `coalesce`             | 通常用于减少分区，默认尽量避免 Shuffle |
| `partitionBy`          | 写出时按字段生成目录分区               |
| `sortWithinPartitions` | 分区内排序，不保证全局有序             |

分区配置建议如下：

| 场景             | 建议                                         |
| ---------------- | -------------------------------------------- |
| 小数据写出       | 使用 `coalesce` 减少小文件                   |
| 大数据聚合       | 合理提高 Shuffle 分区数                      |
| 按日期写 Hive 表 | 使用 `partitionBy("dt")` 或动态分区          |
| Join 前优化      | 按 Join Key 重新分区可减少部分 Shuffle 问题  |
| 数据倾斜         | 单纯增加分区不一定有效，需要识别热点 key     |
| 本地调试         | `spark.sql.shuffle.partitions` 设置为 2 到 8 |
| 生产任务         | 根据数据量设置为 200、400、800 或更高        |

分区不是越多越好。分区过少会导致单个 Task 数据量过大，分区过多会导致调度开销增加和小文件变多。生产环境应结合数据量、Executor 数量、文件大小和任务耗时持续调整。



## 业务计算开发

本章节用于说明 Spark 项目中常见业务计算任务的开发方式，包括离线批处理、增量计算、实时流处理、宽表构建、指标计算、明细加工、维度关联和数据分层处理。业务计算开发应围绕“读取数据、清洗转换、业务计算、质量校验、结果写出、日志审计”这条主线展开，避免将所有逻辑堆叠在 `main` 方法中。

### 离线批处理任务

离线批处理任务通常按天、按小时或按批次执行，适合处理历史数据、日志清洗、指标汇总、宽表加工和 Hive 分区写入等场景。离线任务的核心特征是输入范围明确、输出结果可重跑、任务链路可追踪。

典型处理流程如下：

| 步骤     | 说明                                                       |
| -------- | ---------------------------------------------------------- |
| 参数解析 | 获取运行环境、业务日期、输入路径、输出路径、是否覆盖等参数 |
| 数据读取 | 从 Hive、HDFS、Parquet、ORC、JDBC 等数据源读取数据         |
| 数据清洗 | 处理空值、重复值、异常值、字段类型和时间字段               |
| 业务计算 | 执行 Join、聚合、窗口函数、指标计算等逻辑                  |
| 数据校验 | 校验数据量、主键唯一性、必填字段和分区日期                 |
| 数据写出 | 写入 Hive 表、HDFS 路径、对象存储或 JDBC 目标表            |
| 日志审计 | 记录输入量、输出量、耗时、参数和异常信息                   |

文件位置：`src/main/java/io/github/atengk/spark/job/BatchJob.java`

下面的接口用于统一批处理任务的开发规范。

```java
package io.github.atengk.spark.job;

import io.github.atengk.spark.config.JobRuntimeArgs;
import org.apache.spark.sql.SparkSession;

/**
 * Spark 离线批处理任务接口。
 *
 * @author Ateng
 * @since 2026-05-11
 */
public interface BatchJob {

    /**
     * 执行批处理任务。
     *
     * @param spark SparkSession
     * @param args 任务运行参数
     */
    void run(SparkSession spark, JobRuntimeArgs args);
}
```

文件位置：`src/main/java/io/github/atengk/spark/job/UserSummaryBatchJob.java`

下面的任务示例按业务日期读取用户明细和订单明细，计算用户订单汇总结果。

```java
package io.github.atengk.spark.job;

import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.spark.config.JobRuntimeArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.count;
import static org.apache.spark.sql.functions.current_timestamp;
import static org.apache.spark.sql.functions.sum;

/**
 * 用户订单汇总离线批处理任务。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserSummaryBatchJob implements BatchJob {

    /**
     * 执行用户订单汇总任务。
     *
     * @param spark SparkSession
     * @param args 任务运行参数
     */
    @Override
    public void run(SparkSession spark, JobRuntimeArgs args) {
        TimeInterval timer = new TimeInterval();
        String bizDate = args.getBizDate();

        if (StrUtil.isBlank(bizDate)) {
            throw new IllegalArgumentException("业务日期不能为空");
        }

        log.info("开始执行用户订单汇总任务，业务日期：{}", bizDate);

        Dataset<Row> userDf = spark.table("dwd.dwd_user_detail")
                .filter(col("dt").equalTo(bizDate))
                .select("user_id", "user_name", "user_level", "dt");

        Dataset<Row> orderDf = spark.table("dwd.dwd_order_detail")
                .filter(col("dt").equalTo(bizDate))
                .select("user_id", "order_id", "order_amount");

        Dataset<Row> summaryDf = orderDf
                .groupBy("user_id")
                .agg(
                        count("order_id").alias("order_count"),
                        sum("order_amount").alias("order_amount")
                );

        Dataset<Row> resultDf = userDf
                .join(summaryDf, "user_id")
                .withColumn("etl_time", current_timestamp())
                .select(
                        col("user_id"),
                        col("user_name"),
                        col("user_level"),
                        col("order_count"),
                        col("order_amount"),
                        col("etl_time"),
                        col("dt")
                );

        long outputCount = resultDf.count();
        log.info("用户订单汇总结果数量：{}", outputCount);

        resultDf.write()
                .mode(SaveMode.Overwrite)
                .format("parquet")
                .insertInto("ads.ads_user_order_summary");

        log.info("用户订单汇总任务执行完成，业务日期：{}，耗时：{} ms", bizDate, timer.interval());
    }
}
```

离线批处理任务开发建议：

| 建议                 | 说明                                       |
| -------------------- | ------------------------------------------ |
| 任务必须支持重跑     | 输出分区应可覆盖或可清理后重算             |
| 业务日期必须显式传入 | 不建议在代码中直接使用当前日期作为业务日期 |
| 读取分区必须明确     | 避免误扫全表                               |
| 写出结果必须可校验   | 记录输出条数、分区、路径和目标表           |
| 异常应快速失败       | 核心数据缺失或质量异常时不应继续写出       |

### 增量计算任务

增量计算任务只处理新增或变更的数据，适合订单增量同步、用户状态变更、日志增量加工、CDC 数据处理和按时间窗口补充计算等场景。增量任务的关键是明确增量边界，例如业务日期、更新时间、水位线、Offset 或批次号。

常见增量口径如下：

| 增量方式             | 说明                                       |
| -------------------- | ------------------------------------------ |
| 按分区日期增量       | 处理指定 `dt` 分区数据                     |
| 按更新时间增量       | 处理 `update_time > last_watermark` 的数据 |
| 按自增 ID 增量       | 处理 `id > last_max_id` 的数据             |
| 按 Kafka Offset 增量 | 处理指定 Offset 范围内的消息               |
| 按批次号增量         | 处理指定 `batch_id` 的数据                 |

文件位置：`src/main/java/io/github/atengk/spark/job/UserIncrementJob.java`

下面的任务示例基于更新时间读取 JDBC 增量数据，并写入 Hive 分区表。

```java
package io.github.atengk.spark.job;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.spark.config.JobRuntimeArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;

import java.util.Properties;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.current_timestamp;
import static org.apache.spark.sql.functions.lit;

/**
 * 用户增量数据同步任务。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserIncrementJob implements BatchJob {

    /**
     * 执行用户增量同步任务。
     *
     * @param spark SparkSession
     * @param args 任务运行参数
     */
    @Override
    public void run(SparkSession spark, JobRuntimeArgs args) {
        String bizDate = args.getBizDate();
        String startTime = args.getStartTime();
        String endTime = args.getEndTime();

        if (StrUtil.hasBlank(bizDate, startTime, endTime)) {
            throw new IllegalArgumentException("业务日期、增量开始时间和增量结束时间不能为空");
        }

        log.info("开始执行用户增量同步任务，业务日期：{}，开始时间：{}，结束时间：{}", bizDate, startTime, endTime);

        String query = StrUtil.format(
                "(SELECT id, user_name, mobile, status, update_time FROM user_info " +
                        "WHERE update_time >= '{}' AND update_time < '{}') t",
                startTime,
                endTime
        );

        Properties properties = new Properties();
        properties.setProperty("user", System.getenv("MYSQL_USERNAME"));
        properties.setProperty("password", System.getenv("MYSQL_PASSWORD"));
        properties.setProperty("driver", "com.mysql.cj.jdbc.Driver");

        Dataset<Row> incrementDf = spark.read()
                .jdbc("jdbc:mysql://mysql-host:3306/demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai",
                        query,
                        properties)
                .withColumn("dt", lit(bizDate))
                .withColumn("etl_time", current_timestamp())
                .select(
                        col("id").alias("user_id"),
                        col("user_name"),
                        col("mobile"),
                        col("status"),
                        col("update_time"),
                        col("etl_time"),
                        col("dt")
                );

        long count = incrementDf.count();
        log.info("用户增量数据读取完成，数据量：{}", count);

        if (count == 0) {
            log.warn("用户增量数据为空，本次任务不写出结果，业务日期：{}", bizDate);
            return;
        }

        incrementDf.write()
                .mode(SaveMode.Append)
                .format("parquet")
                .insertInto("ods.ods_user_increment");

        log.info("用户增量同步任务执行完成，业务日期：{}", bizDate);
    }
}
```

增量计算注意事项：

| 注意点               | 说明                                              |
| -------------------- | ------------------------------------------------- |
| 增量边界必须稳定     | 起止时间建议左闭右开，例如 `[startTime, endTime)` |
| 需要考虑迟到数据     | 上游延迟写入时，应预留回溯窗口或做补偿            |
| 写出需要幂等         | 重跑同一批次不应产生重复结果                      |
| 水位线应可追踪       | 增量任务应记录当前处理到的位置                    |
| 重要任务建议落审计表 | 记录批次号、输入量、输出量、开始时间、结束时间    |

### 实时流处理任务

实时流处理任务用于消费 Kafka、文件流或其他流式数据源，进行实时清洗、实时聚合、实时告警和实时写出。新项目应优先使用 Structured Streaming，而不是旧版 Spark Streaming。

Structured Streaming 的核心流程如下：

1. 通过 `readStream` 读取 Kafka 或文件流。
2. 解析消息内容，例如 JSON 反序列化。
3. 进行字段清洗、过滤、转换、聚合。
4. 配置输出模式、Checkpoint 和 Trigger。
5. 启动流式查询并等待任务运行。

文件位置：`src/main/java/io/github/atengk/spark/job/UserEventStreamJob.java`

下面的实时任务从 Kafka 读取用户事件，解析 JSON 后写入 Parquet 路径。

```java
package io.github.atengk.spark.job;

import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.OutputMode;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.current_timestamp;
import static org.apache.spark.sql.functions.from_json;

/**
 * 用户事件实时流处理任务。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserEventStreamJob {

    /**
     * 执行用户事件流处理任务。
     *
     * @param spark SparkSession
     * @throws Exception 流任务异常
     */
    public void run(SparkSession spark) throws Exception {
        log.info("开始启动用户事件实时流处理任务");

        StructType schema = new StructType()
                .add("user_id", DataTypes.StringType)
                .add("event_type", DataTypes.StringType)
                .add("event_time", DataTypes.StringType)
                .add("source", DataTypes.StringType);

        Dataset<Row> kafkaDf = spark.readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", "kafka-host:9092")
                .option("subscribe", "ods_user_event")
                .option("startingOffsets", "latest")
                .option("failOnDataLoss", "false")
                .option("maxOffsetsPerTrigger", "100000")
                .load();

        Dataset<Row> eventDf = kafkaDf
                .selectExpr("CAST(value AS STRING) AS message_value", "topic", "partition", "offset", "timestamp")
                .withColumn("json_data", from_json(col("message_value"), schema))
                .select(
                        col("json_data.user_id").alias("user_id"),
                        col("json_data.event_type").alias("event_type"),
                        col("json_data.event_time").alias("event_time"),
                        col("json_data.source").alias("source"),
                        col("topic"),
                        col("partition"),
                        col("offset"),
                        col("timestamp").alias("kafka_time")
                )
                .filter(col("user_id").isNotNull())
                .withColumn("etl_time", current_timestamp());

        StreamingQuery query = eventDf.writeStream()
                .format("parquet")
                .option("path", "hdfs:///warehouse/dwd/user_event_stream")
                .option("checkpointLocation", "hdfs:///checkpoint/user_event_stream")
                .outputMode(OutputMode.Append())
                .start();

        log.info("用户事件实时流处理任务启动完成");
        query.awaitTermination();
    }
}
```

实时流任务开发建议：

| 建议                     | 说明                                         |
| ------------------------ | -------------------------------------------- |
| 必须配置 Checkpoint      | 用于保存 Offset、状态和恢复信息              |
| 控制每批消费量           | 使用 `maxOffsetsPerTrigger` 防止瞬时数据过大 |
| 写出必须幂等             | 尤其是写 JDBC、Hive 或外部接口时             |
| 监控延迟和积压           | 关注 Kafka Lag、批次耗时、输入速率           |
| 谨慎使用 Complete 模式   | 状态数据较大时成本较高                       |
| 状态计算必须配 Watermark | 避免状态无限增长                             |

### 宽表构建

宽表构建是将事实表、维度表、明细表和指标表进行关联，形成面向查询、分析或下游应用的数据表。宽表通常包含较多字段，便于下游减少 Join，但构建时需要重点关注字段口径、维度快照、Join 性能和数据膨胀。

宽表构建常见流程如下：

| 步骤       | 说明                             |
| ---------- | -------------------------------- |
| 读取事实表 | 例如订单、支付、访问、事件明细   |
| 读取维度表 | 例如用户、商品、地区、门店、类目 |
| 字段裁剪   | Join 前只保留必要字段            |
| 维度去重   | 保证维度关联键唯一               |
| 关联维度   | 使用 left join 或广播 join       |
| 补充指标   | 计算明细级派生字段               |
| 写入宽表   | 通常按 `dt` 分区写入 Hive        |

宽表构建 SQL 示例：

```sql
-- sql/dws/dws_order_wide.sql
-- 订单宽表构建 SQL
SELECT
    o.order_id,
    o.user_id,
    u.user_name,
    u.user_level,
    o.product_id,
    p.product_name,
    p.category_id,
    c.category_name,
    o.order_amount,
    o.order_time,
    o.pay_status,
    '${bizDate}' AS dt
FROM dwd_order o
LEFT JOIN dim_user u
    ON o.user_id = u.user_id
LEFT JOIN dim_product p
    ON o.product_id = p.product_id
LEFT JOIN dim_category c
    ON p.category_id = c.category_id
WHERE o.dt = '${bizDate}'
```

DataFrame 构建示例：

```java
Dataset<Row> orderDf = spark.table("dwd.dwd_order")
        .filter(col("dt").equalTo(bizDate))
        .select("order_id", "user_id", "product_id", "order_amount", "order_time", "pay_status", "dt");

Dataset<Row> userDf = spark.table("dim.dim_user")
        .select("user_id", "user_name", "user_level")
        .dropDuplicates("user_id");

Dataset<Row> productDf = spark.table("dim.dim_product")
        .select("product_id", "product_name", "category_id")
        .dropDuplicates("product_id");

Dataset<Row> wideDf = orderDf
        .join(userDf, "user_id")
        .join(productDf, "product_id");
```

宽表构建注意事项：

| 注意点               | 说明                         |
| -------------------- | ---------------------------- |
| 维表必须去重         | 否则 Join 后事实表会被放大   |
| Join 字段类型一致    | 避免字符串和数值类型混用     |
| 小维表可广播         | 提升 Join 性能               |
| 字段命名要清晰       | 避免多个表同名字段混淆       |
| 维度口径要明确       | 使用当前维度还是历史快照维度 |
| 宽表字段不宜无限扩张 | 字段过多会影响存储和维护     |

### 指标计算

指标计算用于生成业务统计结果，例如用户数、订单数、支付金额、转化率、留存率、活跃率等。指标计算的重点是口径清晰、粒度明确、时间范围准确、结果可校验。

指标计算常见要素：

| 要素     | 说明                                 |
| -------- | ------------------------------------ |
| 指标名称 | 例如订单数、支付金额、活跃用户数     |
| 统计粒度 | 例如按天、按小时、按渠道、按地区     |
| 统计范围 | 例如指定业务日期或时间窗口           |
| 过滤条件 | 例如只统计支付成功订单               |
| 聚合逻辑 | 例如 count、sum、avg、count distinct |
| 输出位置 | 例如 ADS 指标表或报表表              |

SQL 指标计算示例：

```sql
-- sql/ads/ads_order_daily_summary.sql
-- 每日订单指标汇总
SELECT
    '${bizDate}' AS dt,
    channel,
    COUNT(order_id) AS order_count,
    COUNT(DISTINCT user_id) AS pay_user_count,
    SUM(order_amount) AS pay_amount,
    AVG(order_amount) AS avg_order_amount
FROM dwd_order
WHERE dt = '${bizDate}'
  AND pay_status = 'PAID'
GROUP BY channel
```

DataFrame 指标计算示例：

```java
import static org.apache.spark.sql.functions.avg;
import static org.apache.spark.sql.functions.count;
import static org.apache.spark.sql.functions.countDistinct;
import static org.apache.spark.sql.functions.sum;

Dataset<Row> metricDf = orderDf
        .filter(col("pay_status").equalTo("PAID"))
        .groupBy("dt", "channel")
        .agg(
                count("order_id").alias("order_count"),
                countDistinct("user_id").alias("pay_user_count"),
                sum("order_amount").alias("pay_amount"),
                avg("order_amount").alias("avg_order_amount")
        );
```

指标计算建议：

| 建议               | 说明                                 |
| ------------------ | ------------------------------------ |
| 指标口径写入文档   | 避免同名指标多套口径                 |
| 统计粒度必须固定   | 输出表主键应与统计粒度一致           |
| 金额使用 Decimal   | 避免 double 精度问题                 |
| 去重指标谨慎计算   | `countDistinct` 在大数据量下成本较高 |
| 结果需要波动校验   | 对比历史数据，发现异常增减           |
| 指标表建议宽而稳定 | ADS 层字段应面向报表和应用使用       |

### 明细数据加工

明细数据加工用于将原始数据清洗成结构化、标准化、可复用的 DWD 明细表。明细层一般尽量保留业务过程信息，不做过度聚合，为后续宽表、指标和查询提供基础。

明细加工常见内容：

| 加工项     | 说明                                         |
| ---------- | -------------------------------------------- |
| 字段标准化 | 统一字段命名、类型、格式                     |
| 数据清洗   | 过滤异常数据、处理空值、去重                 |
| 时间处理   | 生成事件时间、处理时间和分区日期             |
| 枚举转换   | 将原始状态码转换为标准状态                   |
| 脏数据输出 | 异常记录进入错误表或错误路径                 |
| 审计字段   | 增加 `etl_time`、`source_system`、`batch_id` |

明细加工示例：

```java
Dataset<Row> detailDf = sourceDf
        .filter(col("order_id").isNotNull())
        .withColumn("order_amount", col("order_amount").cast("decimal(18,2)"))
        .withColumn("order_time", functions.to_timestamp(col("order_time"), "yyyy-MM-dd HH:mm:ss"))
        .withColumn("pay_status", functions.upper(functions.trim(col("pay_status"))))
        .withColumn("etl_time", functions.current_timestamp())
        .select(
                col("order_id"),
                col("user_id"),
                col("product_id"),
                col("order_amount"),
                col("order_time"),
                col("pay_status"),
                col("etl_time"),
                col("dt")
        );
```

明细数据加工建议：

| 建议             | 说明                               |
| ---------------- | ---------------------------------- |
| 不过早聚合       | DWD 明细层应保留业务过程粒度       |
| 字段类型稳定     | 下游依赖明细字段，类型变更需谨慎   |
| 保留来源信息     | 便于排查数据来源和追溯             |
| 异常数据单独落地 | 不建议静默丢弃                     |
| 分区字段必须规范 | 通常使用 `dt`，格式为 `yyyy-MM-dd` |

### 维度数据关联

维度数据关联用于补充事实数据的业务属性，例如用户等级、商品类目、地区名称、门店信息等。维度关联的质量直接影响宽表和指标的准确性。

常见维度类型：

| 维度类型     | 说明                               |
| ------------ | ---------------------------------- |
| 静态维度     | 变化很少，例如地区、行业分类       |
| 缓慢变化维度 | 例如用户等级、商品类目、组织架构   |
| 快照维度     | 按日期保存维度状态                 |
| 拉链维度     | 使用生效时间和失效时间描述历史变化 |

当前维度关联示例：

```java
Dataset<Row> factDf = spark.table("dwd.dwd_order")
        .filter(col("dt").equalTo(bizDate));

Dataset<Row> userDimDf = spark.table("dim.dim_user_current")
        .select("user_id", "user_level", "city_id")
        .dropDuplicates("user_id");

Dataset<Row> resultDf = factDf.join(functions.broadcast(userDimDf), "user_id");
```

快照维度关联示例：

```sql
SELECT
    o.order_id,
    o.user_id,
    u.user_level,
    u.city_id,
    o.order_amount,
    o.dt
FROM dwd.dwd_order o
LEFT JOIN dim.dim_user_snapshot u
    ON o.user_id = u.user_id
   AND o.dt = u.dt
WHERE o.dt = '${bizDate}'
```

拉链维度关联示例：

```sql
SELECT
    o.order_id,
    o.user_id,
    u.user_level,
    u.city_id,
    o.order_time,
    o.dt
FROM dwd.dwd_order o
LEFT JOIN dim.dim_user_zipper u
    ON o.user_id = u.user_id
   AND o.order_time >= u.start_time
   AND o.order_time < u.end_time
WHERE o.dt = '${bizDate}'
```

维度关联建议：

| 建议                | 说明                                       |
| ------------------- | ------------------------------------------ |
| 维度表先去重        | 避免事实数据被放大                         |
| 小维表使用广播 Join | 降低 Shuffle 成本                          |
| 明确维度版本        | 当前维度、快照维度、拉链维度口径不同       |
| 关联失败要统计      | 统计未匹配维度的数据量                     |
| 重要维度缺失需告警  | 如商品、地区、组织维度缺失可能影响核心指标 |

### 数据分层处理

数据分层用于规范数据从原始层到应用层的加工链路，提升数据复用性、可维护性和口径一致性。Spark 项目通常服务于数仓分层建设，不同层级的数据加工目标不同。

常见分层如下：

| 层级 | 说明                                         |
| ---- | -------------------------------------------- |
| ODS  | 原始数据层，尽量保留源系统原貌               |
| DWD  | 明细数据层，完成清洗、标准化和结构化         |
| DWS  | 汇总数据层，按主题或业务过程进行轻度汇总     |
| ADS  | 应用数据层，面向报表、接口、看板和应用场景   |
| DIM  | 维度层，保存用户、商品、地区、组织等维度信息 |

分层处理示例：

```text
ODS 原始订单数据
        ↓ 清洗、类型转换、异常过滤
DWD 订单明细表
        ↓ 关联用户、商品、地区维度
DWS 订单主题宽表 / 用户订单汇总表
        ↓ 按报表口径聚合
ADS 每日订单指标表
```

分层处理建议：

| 建议                 | 说明                                   |
| -------------------- | -------------------------------------- |
| ODS 尽量少加工       | 保留原始字段和原始记录                 |
| DWD 注重清洗标准化   | 提供可复用明细数据                     |
| DWS 注重主题汇总     | 面向业务过程组织数据                   |
| ADS 注重应用口径     | 面向具体报表和接口                     |
| DIM 注重一致性       | 维度主键、名称、层级和历史变化要稳定   |
| 层级之间禁止混乱依赖 | ADS 不应直接大量依赖 ODS，除非特殊场景 |

## Spark SQL 开发

本章节用于说明 Spark SQL 的开发方式，包括临时视图、全局临时视图、SQL 文件管理、动态 SQL 拼接、参数绑定、结果处理和性能优化。Spark SQL 是 Java Spark 项目中最常用的业务表达方式，适合处理结构化数据、复杂 Join、窗口函数和指标计算。

### 临时视图

临时视图用于在当前 SparkSession 中注册 DataFrame，使其可以通过 SQL 查询。临时视图生命周期只在当前 SparkSession 内有效，适合一个任务内部不同计算步骤之间共享中间结果。

创建临时视图：

```java
Dataset<Row> userDf = spark.table("dwd.dwd_user_detail")
        .filter(col("dt").equalTo(bizDate));

userDf.createOrReplaceTempView("tmp_user_detail");
```

使用临时视图查询：

```java
Dataset<Row> resultDf = spark.sql(
        "SELECT user_id, user_name, user_level " +
        "FROM tmp_user_detail " +
        "WHERE user_level IS NOT NULL"
);
```

临时视图使用建议：

| 建议                     | 说明                             |
| ------------------------ | -------------------------------- |
| 适合任务内部使用         | 临时视图不会跨 SparkSession 保留 |
| 命名加 `tmp_` 前缀       | 便于区分临时视图和 Hive 表       |
| 复杂链路分段注册         | 提高 SQL 可读性和调试便利性      |
| 避免视图名冲突           | 同一任务中视图名应唯一           |
| 调试时可 `show` 少量数据 | 生产不要频繁 `show` 大量数据     |

### 全局临时视图

全局临时视图可以在同一个 Spark Application 的不同 SparkSession 之间共享，存储在系统数据库 `global_temp` 下。它的生命周期与 Spark Application 一致，而不是某个单独 SparkSession。

创建全局临时视图：

```java
userDf.createOrReplaceGlobalTempView("user_detail");
```

查询全局临时视图：

```java
Dataset<Row> resultDf = spark.sql(
        "SELECT user_id, user_name FROM global_temp.user_detail"
);
```

全局临时视图使用场景较少，通常适合以下情况：

| 场景                         | 说明                     |
| ---------------------------- | ------------------------ |
| 多 SparkSession 共享中间结果 | 同一应用内部需要共享数据 |
| 测试或调试                   | 验证多个会话间数据可见性 |
| 临时公共数据                 | 应用生命周期内临时复用   |

使用建议：

| 建议                     | 说明                     |
| ------------------------ | ------------------------ |
| 常规任务优先使用临时视图 | 全局临时视图不应滥用     |
| 查询必须带 `global_temp` | 否则找不到视图           |
| 不替代 Hive 表           | 全局临时视图不是持久化表 |
| 任务结束后自动失效       | 不适合跨任务共享数据     |

### SQL 文件管理

SQL 文件管理用于将复杂 SQL 从 Java 代码中剥离，提升可读性、可维护性和数据开发协作效率。复杂指标、宽表构建、分层加工等 SQL 应优先放在资源目录或独立 SQL 目录中。

推荐 SQL 目录结构：

```bash
src/main/resources/sql
├── ods
│   └── ods_user_clean.sql
├── dwd
│   ├── dwd_order_detail.sql
│   └── dwd_user_event.sql
├── dws
│   └── dws_order_wide.sql
└── ads
    ├── ads_order_daily_summary.sql
    └── ads_user_order_summary.sql
```

SQL 文件示例：

```sql
-- src/main/resources/sql/ads/ads_user_order_summary.sql
-- 用户订单汇总指标
SELECT
    u.user_id,
    u.user_name,
    COUNT(o.order_id) AS order_count,
    COALESCE(SUM(o.order_amount), 0) AS order_amount,
    '${bizDate}' AS dt
FROM dwd.dwd_user_detail u
LEFT JOIN dwd.dwd_order_detail o
    ON u.user_id = o.user_id
   AND o.dt = '${bizDate}'
WHERE u.dt = '${bizDate}'
GROUP BY u.user_id, u.user_name
```

文件位置：`src/main/java/io/github/atengk/spark/sql/SqlFileLoader.java`

下面的工具类用于从 `resources/sql` 目录读取 SQL 文件。

```java
package io.github.atengk.spark.sql;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * SQL 文件加载工具。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class SqlFileLoader {

    private SqlFileLoader() {
    }

    /**
     * 从 classpath 读取 SQL 文件。
     *
     * @param sqlPath SQL 文件路径
     * @return SQL 内容
     */
    public static String loadFromClasspath(String sqlPath) {
        if (StrUtil.isBlank(sqlPath)) {
            throw new IllegalArgumentException("SQL 文件路径不能为空");
        }

        String sql = ResourceUtil.readUtf8Str(sqlPath);
        if (StrUtil.isBlank(sql)) {
            throw new IllegalArgumentException(StrUtil.format("SQL 文件内容为空：{}", sqlPath));
        }

        log.info("SQL 文件读取完成：{}", sqlPath);
        return sql;
    }
}
```

SQL 文件管理建议：

| 建议                   | 说明                                            |
| ---------------------- | ----------------------------------------------- |
| SQL 按数据层级分类     | ODS、DWD、DWS、ADS 分目录维护                   |
| SQL 文件名体现目标表   | 便于定位任务逻辑                                |
| 文件顶部写明用途       | 使用 SQL 注释描述业务口径                       |
| 避免 Java 拼接大 SQL   | 复杂 SQL 应外置                                 |
| SQL 参数使用统一占位符 | 例如 `${bizDate}`、`${sourceDb}`、`${targetDb}` |
| 上线前执行语法校验     | 避免运行时才发现 SQL 错误                       |

### 动态 SQL 拼接

动态 SQL 拼接用于根据运行环境、业务日期、库名、表名、分区、过滤条件等参数生成最终 SQL。动态 SQL 应优先通过模板替换完成，不建议在业务代码中使用大量字符串拼接。

常见动态参数如下：

| 参数            | 示例               | 说明       |
| --------------- | ------------------ | ---------- |
| `${bizDate}`    | `2026-05-11`       | 业务日期   |
| `${sourceDb}`   | `dwd`              | 来源数据库 |
| `${targetDb}`   | `ads`              | 目标数据库 |
| `${userTable}`  | `dwd_user_detail`  | 来源表     |
| `${orderTable}` | `dwd_order_detail` | 来源表     |
| `${channel}`    | `APP`              | 过滤条件   |

文件位置：`src/main/java/io/github/atengk/spark/sql/SqlTemplateRenderer.java`

下面的工具类使用 Hutool 完成简单 SQL 模板变量替换，并对未替换变量做校验。

```java
package io.github.atengk.spark.sql;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SQL 模板渲染工具。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class SqlTemplateRenderer {

    private static final String PLACEHOLDER_PATTERN = "\\$\\{[a-zA-Z0-9_]+}";

    private SqlTemplateRenderer() {
    }

    /**
     * 渲染 SQL 模板。
     *
     * @param sqlTemplate SQL 模板
     * @param params 参数 Map
     * @return 渲染后的 SQL
     */
    public static String render(String sqlTemplate, Map<String, String> params) {
        if (StrUtil.isBlank(sqlTemplate)) {
            throw new IllegalArgumentException("SQL 模板不能为空");
        }
        if (MapUtil.isEmpty(params)) {
            throw new IllegalArgumentException("SQL 模板参数不能为空");
        }

        String renderedSql = sqlTemplate;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String placeholder = StrUtil.format("${{{}}}", entry.getKey());
            renderedSql = StrUtil.replace(renderedSql, placeholder, entry.getValue());
        }

        Set<String> notReplaced = ReUtil.findAll(PLACEHOLDER_PATTERN, renderedSql, 0)
                .stream()
                .collect(Collectors.toSet());

        if (!notReplaced.isEmpty()) {
            throw new IllegalArgumentException(StrUtil.format("SQL 存在未替换参数：{}", notReplaced));
        }

        log.info("SQL 模板渲染完成，参数个数：{}", params.size());
        return renderedSql;
    }
}
```

使用示例：

```java
String sqlTemplate = SqlFileLoader.loadFromClasspath("sql/ads/ads_user_order_summary.sql");

Map<String, String> params = new HashMap<>();
params.put("bizDate", "2026-05-11");

String sql = SqlTemplateRenderer.render(sqlTemplate, params);

Dataset<Row> resultDf = spark.sql(sql);
```

动态 SQL 拼接建议：

| 建议                   | 说明                              |
| ---------------------- | --------------------------------- |
| 使用模板替换           | 避免大段 Java 字符串拼接          |
| 参数必须校验           | 日期、库名、表名、枚举值都要校验  |
| 禁止拼接未校验外部输入 | 防止 SQL 错误、路径污染和逻辑异常 |
| SQL 执行前可打印       | 生产日志中打印非敏感 SQL          |
| 保留 SQL 文件版本      | SQL 变更应进入代码评审            |

### SQL 参数绑定

Spark SQL 不像传统 JDBC PreparedStatement 那样提供标准的参数绑定机制。实际开发中通常通过模板变量替换实现参数注入。因此，SQL 参数绑定的重点是参数校验、类型规范、占位符统一和安全替换。

推荐参数校验规则：

| 参数类型 | 校验方式                           |
| -------- | ---------------------------------- |
| 业务日期 | 正则校验 `yyyy-MM-dd`              |
| 数据库名 | 只允许字母、数字、下划线           |
| 表名     | 只允许字母、数字、下划线           |
| 分区值   | 按分区格式校验                     |
| 枚举参数 | 使用白名单                         |
| 路径参数 | 限制前缀，例如 `hdfs://`、`s3a://` |

文件位置：`src/main/java/io/github/atengk/spark/sql/SqlParamValidator.java`

下面的工具类用于校验 SQL 模板参数。

```java
package io.github.atengk.spark.sql;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;

import java.util.Collection;

/**
 * SQL 参数校验工具。
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class SqlParamValidator {

    private static final String DATE_PATTERN = "\\d{4}-\\d{2}-\\d{2}";
    private static final String IDENTIFIER_PATTERN = "[a-zA-Z_][a-zA-Z0-9_]*";

    private SqlParamValidator() {
    }

    /**
     * 校验业务日期。
     *
     * @param bizDate 业务日期
     */
    public static void validateBizDate(String bizDate) {
        if (StrUtil.isBlank(bizDate) || !ReUtil.isMatch(DATE_PATTERN, bizDate)) {
            throw new IllegalArgumentException(StrUtil.format("业务日期格式不正确：{}", bizDate));
        }
    }

    /**
     * 校验 SQL 标识符。
     *
     * @param identifier SQL 标识符
     * @param name 参数名称
     */
    public static void validateIdentifier(String identifier, String name) {
        if (StrUtil.isBlank(identifier) || !ReUtil.isMatch(IDENTIFIER_PATTERN, identifier)) {
            throw new IllegalArgumentException(StrUtil.format("{} 格式不正确：{}", name, identifier));
        }
    }

    /**
     * 校验参数必须在白名单中。
     *
     * @param value 参数值
     * @param allowValues 白名单
     * @param name 参数名称
     */
    public static void validateIn(String value, Collection<String> allowValues, String name) {
        if (StrUtil.isBlank(value) || CollUtil.isEmpty(allowValues) || !allowValues.contains(value)) {
            throw new IllegalArgumentException(StrUtil.format("{} 不在允许范围内：{}", name, value));
        }
    }
}
```

使用示例：

```java
String bizDate = "2026-05-11";
String sourceDb = "dwd";
String targetDb = "ads";

SqlParamValidator.validateBizDate(bizDate);
SqlParamValidator.validateIdentifier(sourceDb, "来源库名");
SqlParamValidator.validateIdentifier(targetDb, "目标库名");

Map<String, String> params = new HashMap<>();
params.put("bizDate", bizDate);
params.put("sourceDb", sourceDb);
params.put("targetDb", targetDb);
```

SQL 参数绑定建议：

| 建议               | 说明                          |
| ------------------ | ----------------------------- |
| 占位符格式统一     | 推荐 `${paramName}`           |
| 参数先校验再替换   | 不要直接替换用户输入          |
| 表名库名走白名单   | 防止误读误写其他库表          |
| 日期格式强校验     | 避免分区路径错误              |
| 敏感参数不进入 SQL | 密码、Token 不应出现在 SQL 中 |

### SQL 执行结果处理

SQL 执行结果通常是 `Dataset<Row>`，后续可以继续转换、注册视图、质量校验、写出 Hive 表或写出文件。执行 SQL 后不建议立即 `collect()`，除非确认结果非常小。

常见结果处理方式如下：

| 处理方式                  | 说明                      |
| ------------------------- | ------------------------- |
| `show`                    | 调试查看少量结果          |
| `count`                   | 统计结果数据量            |
| `createOrReplaceTempView` | 注册为后续 SQL 的临时视图 |
| `write`                   | 写出到文件、Hive、JDBC    |
| `cache`                   | 多次复用结果时缓存        |
| `explain`                 | 查看执行计划              |

SQL 执行示例：

```java
Dataset<Row> resultDf = spark.sql(sql);

long resultCount = resultDf.count();
log.info("SQL 执行完成，结果数据量：{}", resultCount);

if (resultCount == 0) {
    throw new IllegalStateException("SQL 执行结果为空");
}

resultDf.write()
        .mode(SaveMode.Overwrite)
        .format("parquet")
        .insertInto("ads.ads_user_order_summary");
```

小结果收集示例：

```java
Dataset<Row> configDf = spark.sql(
        "SELECT config_key, config_value FROM dim.dim_job_config WHERE job_name = 'user_summary'"
);

List<Row> rows = configDf.limit(100).collectAsList();
for (Row row : rows) {
    log.info("配置项：{}={}", row.getAs("config_key"), row.getAs("config_value"));
}
```

SQL 结果处理建议：

| 建议                 | 说明                               |
| -------------------- | ---------------------------------- |
| 大结果不要 collect   | 避免 Driver 内存溢出               |
| 写出前做质量校验     | 校验空结果、主键、分区和重复数据   |
| 多次复用才 cache     | 不要无意义缓存                     |
| 重要 SQL 记录数据量  | 便于审计和排查                     |
| 写 Hive 表前字段对齐 | 字段名称、类型、顺序应与目标表一致 |
| 调试使用 limit       | 避免 `show` 或 `collect` 大量数据  |

### SQL 性能优化

SQL 性能优化的目标是减少扫描数据量、降低 Shuffle 成本、提升 Join 效率、控制分区数量并避免数据倾斜。Spark SQL 优化应先看执行计划和任务指标，再有针对性地调整。

常见优化方向如下：

| 优化方向     | 说明                                          |
| ------------ | --------------------------------------------- |
| 分区裁剪     | 查询分区表时必须带分区条件                    |
| 列裁剪       | 只查询必要字段                                |
| 谓词下推     | 过滤条件尽量靠近数据读取                      |
| 广播 Join    | 小维表使用广播 Join                           |
| AQE          | 开启自适应查询执行                            |
| Shuffle 分区 | 根据数据量调整 `spark.sql.shuffle.partitions` |
| 缓存复用     | 多次复用中间结果时缓存                        |
| 数据倾斜处理 | 对热点 Key 做特殊处理                         |
| 小文件优化   | 合并小文件，控制输出文件数量                  |

查看执行计划：

```java
Dataset<Row> resultDf = spark.sql(sql);

// 查看格式化执行计划
resultDf.explain("formatted");
```

常用优化配置：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --conf spark.sql.adaptive.enabled=true \
  --conf spark.sql.adaptive.coalescePartitions.enabled=true \
  --conf spark.sql.adaptive.skewJoin.enabled=true \
  --conf spark.sql.shuffle.partitions=400 \
  --conf spark.sql.autoBroadcastJoinThreshold=10485760 \
  target/spark-job.jar \
  --env prod \
  --bizDate 2026-05-11
```

SQL 优化示例，避免全表扫描：

```sql
-- 不推荐：没有分区条件，容易扫描全表
SELECT user_id, order_amount
FROM dwd.dwd_order_detail;

-- 推荐：带业务日期分区条件，并只选择必要字段
SELECT user_id, order_amount
FROM dwd.dwd_order_detail
WHERE dt = '${bizDate}';
```

SQL 优化示例，Join 前裁剪字段：

```sql
WITH order_base AS (
    SELECT
        user_id,
        product_id,
        order_amount
    FROM dwd.dwd_order_detail
    WHERE dt = '${bizDate}'
),
user_base AS (
    SELECT
        user_id,
        user_level
    FROM dim.dim_user_current
)
SELECT
    o.user_id,
    u.user_level,
    o.product_id,
    o.order_amount
FROM order_base o
LEFT JOIN user_base u
    ON o.user_id = u.user_id;
```

SQL 性能优化建议：

| 问题           | 处理方式                                           |
| -------------- | -------------------------------------------------- |
| 扫描数据量过大 | 增加分区过滤和字段裁剪                             |
| Join 很慢      | 检查大小表关系，尝试广播小表                       |
| Shuffle 过多   | 合并计算步骤，减少重复聚合和重复 Join              |
| 单个 Task 很慢 | 检查数据倾斜和热点 Key                             |
| 输出小文件过多 | 写出前 `coalesce` 或调整分区策略                   |
| Driver OOM     | 避免 `collect` 大结果，减少 Driver 侧处理          |
| Executor OOM   | 降低单 Task 数据量，调整内存和分区                 |
| SQL 结果异常   | 检查 Join 是否放大、维表是否重复、过滤条件是否缺失 |

生产优化顺序建议为：先确认 SQL 逻辑和数据范围，再查看 `explain("formatted")`，然后分析 Spark UI 中的 Stage、Shuffle Read、Shuffle Write、Task Duration 和 Skew 情况，最后再调整配置。不要在没有执行计划和任务指标的情况下盲目增加 Executor 或内存。

## RDD 开发

本章节用于说明 Spark RDD 的开发方式。RDD 是 Spark 最底层的数据抽象，提供较强的分区控制能力和函数式转换能力，但缺少 Spark SQL 的 Catalyst 优化能力。在 Java Spark 项目中，常规结构化数据处理应优先使用 DataFrame 和 Spark SQL，只有在非结构化数据处理、底层分区控制、自定义转换逻辑等场景下再使用 RDD。

### RDD 创建

RDD 可以从本地集合、外部文件、已有 DataFrame 或其他 RDD 转换而来。Java 项目中通常通过 `JavaSparkContext` 创建和操作 RDD。

从集合创建 RDD：

```java
JavaSparkContext javaSparkContext = JavaSparkContext.fromSparkContext(spark.sparkContext());

List<String> dataList = Arrays.asList("java", "spark", "hadoop", "hive");

JavaRDD<String> rdd = javaSparkContext.parallelize(dataList, 2);
```

从文本文件创建 RDD：

```java
JavaSparkContext javaSparkContext = JavaSparkContext.fromSparkContext(spark.sparkContext());

JavaRDD<String> lineRdd = javaSparkContext.textFile("hdfs:///warehouse/ods/access_log/dt=2026-05-11");
```

从 DataFrame 转换为 RDD：

```java
Dataset<Row> userDf = spark.table("dwd.dwd_user_detail")
        .filter("dt = '2026-05-11'");

JavaRDD<Row> rowRdd = userDf.javaRDD();
```

RDD 创建方式建议如下：

| 创建方式         | 适用场景                             |
| ---------------- | ------------------------------------ |
| `parallelize`    | 本地小集合测试、单元测试             |
| `textFile`       | 读取普通文本、日志文件               |
| `wholeTextFiles` | 每个文件整体作为一条记录读取         |
| `javaRDD`        | 从 DataFrame 转为 RDD 做底层处理     |
| `mapToPair`      | 构建键值对 RDD，用于分组、聚合、Join |

在生产任务中，不建议通过 `parallelize` 创建大规模数据，因为数据需要先集中在 Driver，再分发给 Executor，容易导致 Driver 内存压力。

### RDD Transformation

Transformation 用于描述 RDD 的转换逻辑，具有惰性计算特征。只有遇到 Action 时，Transformation 才会真正执行。

常见 Transformation 包括：

| 操作           | 说明                   |
| -------------- | ---------------------- |
| `map`          | 一条数据转换为一条数据 |
| `flatMap`      | 一条数据转换为多条数据 |
| `filter`       | 过滤数据               |
| `distinct`     | 去重                   |
| `union`        | 合并两个 RDD           |
| `intersection` | 求交集                 |
| `subtract`     | 求差集                 |
| `sample`       | 抽样                   |
| `repartition`  | 重新分区               |
| `coalesce`     | 减少分区               |

基础转换示例：

```java
JavaRDD<String> lineRdd = javaSparkContext.textFile("data/input/access.log");

JavaRDD<String> errorRdd = lineRdd
        .filter(line -> line.contains("ERROR"))
        .map(String::trim);

JavaRDD<String> wordRdd = lineRdd
        .flatMap(line -> Arrays.asList(line.split("\\s+")).iterator());
```

封装 RDD 文本清洗逻辑示例。

文件位置：`src/main/java/io/github/atengk/spark/rdd/AccessLogRddTransformer.java`

```java
package io.github.atengk.spark.rdd;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.api.java.JavaRDD;

/**
 * 访问日志 RDD 转换器。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class AccessLogRddTransformer {

    /**
     * 清洗访问日志。
     *
     * @param lineRdd 原始日志 RDD
     * @return 清洗后的日志 RDD
     */
    public JavaRDD<String> clean(JavaRDD<String> lineRdd) {
        log.info("开始清洗访问日志 RDD");

        JavaRDD<String> cleanedRdd = lineRdd
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .filter(line -> !StrUtil.startWith(line, "#"));

        log.info("访问日志 RDD 清洗逻辑构建完成");
        return cleanedRdd;
    }
}
```

Transformation 使用建议：

| 建议                     | 说明                                     |
| ------------------------ | ---------------------------------------- |
| 避免在算子中访问外部系统 | 不要在 `map` 中频繁访问数据库、HTTP 接口 |
| 注意闭包序列化           | Lambda 中引用的对象需要可序列化          |
| 避免过长链路不可读       | 复杂逻辑应拆分为方法或转换器类           |
| Shuffle 类操作谨慎使用   | `distinct`、`groupByKey` 等操作成本较高  |
| 优先使用 Spark SQL       | 结构化数据转换优先使用 DataFrame API     |

### RDD Action

Action 用于触发 RDD 计算并返回结果、写出结果或执行外部操作。每次 Action 都可能触发一个 Job，因此需要避免无意义的重复 Action。

常见 Action 包括：

| 操作             | 说明                    |
| ---------------- | ----------------------- |
| `count`          | 统计数据条数            |
| `collect`        | 将所有数据拉取到 Driver |
| `take`           | 获取前 N 条             |
| `first`          | 获取第一条              |
| `reduce`         | 聚合所有元素            |
| `foreach`        | 对每条数据执行操作      |
| `saveAsTextFile` | 保存为文本文件          |

Action 示例：

```java
long totalCount = lineRdd.count();

List<String> sampleList = lineRdd.take(10);

lineRdd.saveAsTextFile("hdfs:///warehouse/tmp/access_log_clean/dt=2026-05-11");
```

谨慎使用 `collect`：

```java
// 只适用于确认数据量很小的场景
List<String> smallResult = lineRdd.take(100);
```

RDD Action 使用建议：

| 建议                       | 说明                       |
| -------------------------- | -------------------------- |
| 大数据量禁止直接 `collect` | 容易导致 Driver OOM        |
| 调试时使用 `take`          | 获取少量样例数据即可       |
| 避免重复 `count`           | 多次 Action 会重复触发计算 |
| 写出前确认分区数           | 避免输出小文件过多         |
| 多次复用时先缓存           | 避免每次 Action 都重新计算 |

### Pair RDD 操作

Pair RDD 是键值对形式的 RDD，类型通常为 `JavaPairRDD<K, V>`，适用于按 Key 分组、聚合、Join、排序和自定义分区等场景。

创建 Pair RDD：

```java
JavaPairRDD<String, Integer> wordPairRdd = wordRdd
        .mapToPair(word -> new Tuple2<>(word, 1));
```

词频统计示例：

```java
JavaPairRDD<String, Integer> wordCountRdd = wordPairRdd
        .reduceByKey(Integer::sum);
```

常见 Pair RDD 操作如下：

| 操作             | 说明                          |
| ---------------- | ----------------------------- |
| `reduceByKey`    | 按 Key 聚合，推荐用于聚合场景 |
| `groupByKey`     | 按 Key 分组，容易造成内存压力 |
| `aggregateByKey` | 按 Key 自定义聚合             |
| `combineByKey`   | 更底层的按 Key 聚合           |
| `join`           | 两个 Pair RDD 按 Key 关联     |
| `leftOuterJoin`  | 左外关联                      |
| `sortByKey`      | 按 Key 排序                   |
| `partitionBy`    | 按指定分区器重新分区          |

推荐使用 `reduceByKey`，避免直接使用 `groupByKey`：

```java
JavaPairRDD<String, Integer> resultRdd = wordRdd
        .mapToPair(word -> new Tuple2<>(word, 1))
        .reduceByKey(Integer::sum);
```

Join 示例：

```java
JavaPairRDD<String, String> userRdd = javaSparkContext.parallelizePairs(Arrays.asList(
        new Tuple2<>("1001", "张三"),
        new Tuple2<>("1002", "李四")
));

JavaPairRDD<String, BigDecimal> amountRdd = javaSparkContext.parallelizePairs(Arrays.asList(
        new Tuple2<>("1001", new BigDecimal("99.90")),
        new Tuple2<>("1002", new BigDecimal("199.00"))
));

JavaPairRDD<String, Tuple2<String, BigDecimal>> joinedRdd = userRdd.join(amountRdd);
```

Pair RDD 使用建议：

| 建议                         | 说明                                   |
| ---------------------------- | -------------------------------------- |
| 聚合优先 `reduceByKey`       | 会先在 map 端聚合，减少 Shuffle 数据量 |
| 谨慎使用 `groupByKey`        | 同一个 Key 的数据会集中到一个 Executor |
| Join 前控制数据规模          | 大 RDD Join 容易产生较大 Shuffle       |
| 处理热点 Key                 | 对倾斜 Key 做单独处理或加盐            |
| 需要结构化输出时转 DataFrame | 便于写 Hive、Parquet、ORC              |

### RDD 缓存

RDD 缓存用于避免同一个 RDD 被多次重复计算。常见缓存方法包括 `cache()` 和 `persist()`。`cache()` 默认使用内存缓存，`persist()` 可以指定存储级别。

缓存示例：

```java
JavaRDD<String> cleanedRdd = lineRdd
        .filter(StrUtil::isNotBlank)
        .map(String::trim)
        .cache();

long totalCount = cleanedRdd.count();
long errorCount = cleanedRdd.filter(line -> line.contains("ERROR")).count();
```

使用 `persist` 指定存储级别：

```java
JavaRDD<String> cleanedRdd = lineRdd
        .filter(StrUtil::isNotBlank)
        .persist(StorageLevel.MEMORY_AND_DISK());

long totalCount = cleanedRdd.count();
```

释放缓存：

```java
cleanedRdd.unpersist();
```

缓存策略建议：

| 场景                   | 建议                       |
| ---------------------- | -------------------------- |
| RDD 只使用一次         | 不需要缓存                 |
| RDD 被多个 Action 复用 | 可以缓存                   |
| 数据量较小             | 使用 `MEMORY_ONLY`         |
| 数据量较大             | 使用 `MEMORY_AND_DISK`     |
| 缓存后不再使用         | 及时 `unpersist`           |
| Executor 内存紧张      | 谨慎缓存，避免挤占计算内存 |

缓存不是性能优化的默认答案。只有中间结果确实被复用，并且重新计算成本较高时，缓存才有价值。

### RDD 分区

RDD 分区决定了并行计算的基本粒度。分区数过少会导致资源利用不足，分区数过多会增加调度开销和小文件数量。

查看分区数：

```java
int partitionCount = lineRdd.getNumPartitions();
```

重新分区：

```java
JavaRDD<String> repartitionRdd = lineRdd.repartition(200);
```

减少分区：

```java
JavaRDD<String> coalesceRdd = lineRdd.coalesce(20);
```

Pair RDD 使用 HashPartitioner：

```java
JavaPairRDD<String, Integer> partitionedRdd = wordPairRdd
        .partitionBy(new HashPartitioner(100));
```

分区操作区别：

| 操作               | 说明                                   |
| ------------------ | -------------------------------------- |
| `repartition`      | 增加或减少分区，通常会产生 Shuffle     |
| `coalesce`         | 通常用于减少分区，默认尽量避免 Shuffle |
| `partitionBy`      | Pair RDD 按指定分区器分区              |
| `getNumPartitions` | 查看当前分区数                         |

RDD 分区建议：

| 建议                  | 说明                           |
| --------------------- | ------------------------------ |
| 分区数与资源匹配      | 通常应大于 Executor 总核数     |
| 避免单分区大数据      | 容易造成单 Task 过慢或 OOM     |
| 输出前适当减少分区    | 控制小文件数量                 |
| Join 前考虑统一分区器 | 减少部分 Shuffle 成本          |
| 处理倾斜不能只加分区  | 热点 Key 仍可能集中到单个 Task |

### RDD 使用场景

在 Java Spark 项目中，RDD 不是首选开发模型。对于结构化数据、Hive 表、Parquet、ORC、JDBC、Kafka 等场景，应优先使用 DataFrame、Dataset 和 Spark SQL。

适合使用 RDD 的场景：

| 场景              | 说明                                 |
| ----------------- | ------------------------------------ |
| 非结构化文本处理  | 原始日志、自由格式文本、复杂行解析   |
| 自定义底层转换    | DataFrame API 难以表达的转换逻辑     |
| 精细分区控制      | 自定义 Partitioner、特殊数据分布策略 |
| 需要 Pair RDD API | `reduceByKey`、`aggregateByKey` 等   |
| 旧 Spark 任务维护 | 历史任务基于 RDD 实现                |
| 小规模特殊计算    | 图计算、规则匹配、复杂状态对象处理   |

不建议使用 RDD 的场景：

| 场景               | 原因                           |
| ------------------ | ------------------------------ |
| Hive 表加工        | Spark SQL 更自然，优化能力更强 |
| 指标聚合           | SQL 和 DataFrame 可读性更好    |
| 宽表构建           | SQL Join 更易维护              |
| Parquet / ORC 处理 | DataFrame 支持列裁剪和谓词下推 |
| 结构化清洗         | DataFrame API 更简洁           |
| 流式 Kafka 处理    | Structured Streaming 更合适    |

推荐原则是：能用 Spark SQL 清晰表达的逻辑，不要优先使用 RDD；只有确实需要底层控制时，再使用 RDD。

## Structured Streaming 开发

本章节用于说明 Spark Structured Streaming 的开发方式。Structured Streaming 将实时数据流抽象为持续增长的无界表，开发方式接近 DataFrame 和 Spark SQL。它适合 Kafka 实时消费、实时清洗、实时聚合、窗口统计、实时写入数据湖和流批一体处理等场景。

### 流式数据源

Structured Streaming 支持多种流式数据源，常见包括 Kafka、文件目录、Rate Source 和 Socket Source。生产环境中 Kafka 最常见，文件流适合目录监听，Rate Source 和 Socket Source 更多用于测试。

常见流式数据源如下：

| 数据源        | 说明                                   |
| ------------- | -------------------------------------- |
| Kafka         | 最常用的生产流式数据源                 |
| File Source   | 监听目录中新文件，适合文件增量到达场景 |
| Rate Source   | 按固定速率生成测试数据                 |
| Socket Source | 简单调试使用，不适合生产               |
| 自定义 Source | 特殊系统需要自行扩展                   |

读取 Kafka：

```java
Dataset<Row> kafkaDf = spark.readStream()
        .format("kafka")
        .option("kafka.bootstrap.servers", "kafka-host:9092")
        .option("subscribe", "ods_user_event")
        .option("startingOffsets", "latest")
        .option("failOnDataLoss", "false")
        .load();
```

读取文件流：

```java
StructType schema = new StructType()
        .add("user_id", DataTypes.StringType)
        .add("event_type", DataTypes.StringType)
        .add("event_time", DataTypes.StringType);

Dataset<Row> fileStreamDf = spark.readStream()
        .schema(schema)
        .json("hdfs:///warehouse/stream/input/user_event");
```

读取 Rate Source 测试数据：

```java
Dataset<Row> rateDf = spark.readStream()
        .format("rate")
        .option("rowsPerSecond", "100")
        .load();
```

流式数据源使用建议：

| 建议                  | 说明                                             |
| --------------------- | ------------------------------------------------ |
| 生产优先 Kafka        | 支持 Offset、分区、消费组和高吞吐                |
| 文件流必须指定 Schema | 流式读取不能依赖自动推断 Schema                  |
| 输入目录只追加新文件  | 不建议修改已存在文件                             |
| 控制 Kafka 每批数据量 | 使用 `maxOffsetsPerTrigger`                      |
| 记录源端元信息        | Kafka 的 topic、partition、offset 应保留用于排查 |

### 流式查询

流式查询由 `readStream`、中间转换和 `writeStream` 组成。调用 `start()` 后，Spark 会启动持续运行的查询任务。流任务通常不会自动结束，需要通过 `awaitTermination()` 阻塞等待。

基础流式查询示例：

```java
Dataset<Row> valueDf = kafkaDf.selectExpr(
        "CAST(key AS STRING) AS message_key",
        "CAST(value AS STRING) AS message_value",
        "topic",
        "partition",
        "offset",
        "timestamp"
);

StreamingQuery query = valueDf.writeStream()
        .format("console")
        .option("truncate", "false")
        .outputMode(OutputMode.Append())
        .start();

query.awaitTermination();
```

完整流任务入口示例。

文件位置：`src/main/java/io/github/atengk/spark/stream/UserEventStreamApplication.java`

```java
package io.github.atengk.spark.stream;

import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.OutputMode;
import org.apache.spark.sql.streaming.StreamingQuery;

/**
 * 用户事件流处理任务入口。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserEventStreamApplication {

    /**
     * 启动流处理任务。
     *
     * @param spark SparkSession
     * @throws Exception 流任务异常
     */
    public void run(SparkSession spark) throws Exception {
        log.info("开始启动用户事件流式查询");

        Dataset<Row> kafkaDf = spark.readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", "kafka-host:9092")
                .option("subscribe", "ods_user_event")
                .option("startingOffsets", "latest")
                .option("maxOffsetsPerTrigger", "100000")
                .load();

        Dataset<Row> valueDf = kafkaDf.selectExpr(
                "CAST(value AS STRING) AS message_value",
                "topic",
                "partition",
                "offset",
                "timestamp"
        );

        StreamingQuery query = valueDf.writeStream()
                .format("parquet")
                .option("path", "hdfs:///warehouse/dwd/user_event_stream")
                .option("checkpointLocation", "hdfs:///checkpoint/user_event_stream")
                .outputMode(OutputMode.Append())
                .start();

        log.info("用户事件流式查询启动完成，queryId={}", query.id());
        query.awaitTermination();
    }
}
```

流式查询常用方法：

| 方法                 | 说明                 |
| -------------------- | -------------------- |
| `start()`            | 启动流式查询         |
| `awaitTermination()` | 等待流任务持续运行   |
| `stop()`             | 停止查询             |
| `isActive()`         | 判断查询是否仍在运行 |
| `lastProgress()`     | 查看最近批次进度     |
| `recentProgress()`   | 查看近期批次进度     |

### 输出模式

Structured Streaming 支持 Append、Update 和 Complete 三种输出模式。输出模式必须与查询类型和 Sink 能力匹配。

| 输出模式 | 说明             | 适用场景                        |
| -------- | ---------------- | ------------------------------- |
| Append   | 只输出新增结果   | 明细流、带 Watermark 的窗口聚合 |
| Update   | 输出被更新的结果 | 聚合结果持续更新                |
| Complete | 输出完整结果表   | 小规模全量聚合结果              |

Append 模式示例：

```java
StreamingQuery query = eventDf.writeStream()
        .format("parquet")
        .option("path", "hdfs:///warehouse/dwd/user_event_stream")
        .option("checkpointLocation", "hdfs:///checkpoint/user_event_stream")
        .outputMode(OutputMode.Append())
        .start();
```

Update 模式示例：

```java
Dataset<Row> aggDf = eventDf
        .groupBy("event_type")
        .count();

StreamingQuery query = aggDf.writeStream()
        .format("console")
        .outputMode(OutputMode.Update())
        .start();
```

Complete 模式示例：

```java
Dataset<Row> totalDf = eventDf
        .groupBy("event_type")
        .count();

StreamingQuery query = totalDf.writeStream()
        .format("console")
        .outputMode(OutputMode.Complete())
        .start();
```

输出模式选择建议：

| 场景                    | 推荐模式          |
| ----------------------- | ----------------- |
| Kafka 明细清洗写入 HDFS | Append            |
| 实时指标持续刷新        | Update            |
| 小规模全量聚合展示      | Complete          |
| 带 Watermark 的窗口结果 | Append 或 Update  |
| 状态很大的聚合          | 谨慎使用 Complete |

生产环境中，Complete 模式需要输出完整结果，状态较大时成本很高，应谨慎使用。

### Watermark 机制

Watermark 用于处理乱序数据和迟到数据，主要用于窗口聚合、去重和状态清理。没有 Watermark 的有状态流任务可能导致状态无限增长，最终造成内存或磁盘压力。

事件时间字段转换：

```java
Dataset<Row> eventDf = parsedDf
        .withColumn("event_time", functions.to_timestamp(col("event_time"), "yyyy-MM-dd HH:mm:ss"));
```

窗口聚合与 Watermark 示例：

```java
Dataset<Row> windowAggDf = eventDf
        .withWatermark("event_time", "10 minutes")
        .groupBy(
                functions.window(col("event_time"), "5 minutes"),
                col("event_type")
        )
        .count();
```

写出窗口结果：

```java
StreamingQuery query = windowAggDf.writeStream()
        .format("console")
        .outputMode(OutputMode.Append())
        .option("truncate", "false")
        .start();
```

Watermark 参数含义：

| 配置         | 说明                           |
| ------------ | ------------------------------ |
| `event_time` | 事件时间字段                   |
| `10 minutes` | 允许数据迟到的最大时间         |
| `window`     | 按事件时间划分窗口             |
| Append 模式  | 只有窗口确定不会再更新时才输出 |

Watermark 使用建议：

| 建议                 | 说明                             |
| -------------------- | -------------------------------- |
| 使用事件时间         | 不要用处理时间替代业务事件时间   |
| 延迟时间按业务确定   | 例如 5 分钟、10 分钟、1 小时     |
| 延迟越大状态越大     | Watermark 时间过长会增加状态存储 |
| 迟到过久数据会被丢弃 | 需要明确业务是否接受             |
| 与窗口聚合配合使用   | 实时统计中非常常见               |

### 状态管理

状态管理用于支持跨批次计算，例如窗口聚合、流式去重、累计统计、Session 聚合等。状态会保存在 State Store 中，并通过 Checkpoint 进行恢复。

常见有状态操作：

| 操作               | 说明                             |
| ------------------ | -------------------------------- |
| 窗口聚合           | 按时间窗口持续聚合               |
| 流式去重           | 使用 `dropDuplicates` 跨批次去重 |
| 聚合统计           | 按 Key 持续维护聚合结果          |
| Session Window     | 按用户行为间隔聚合会话           |
| mapGroupsWithState | 自定义状态计算，Java 中使用较少  |

流式去重示例：

```java
Dataset<Row> deduplicateDf = eventDf
        .withWatermark("event_time", "30 minutes")
        .dropDuplicates("event_id");
```

窗口状态聚合示例：

```java
Dataset<Row> userEventCountDf = eventDf
        .withWatermark("event_time", "10 minutes")
        .groupBy(
                functions.window(col("event_time"), "5 minutes"),
                col("user_id")
        )
        .count();
```

状态管理配置示例：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --conf spark.sql.streaming.stateStore.providerClass=org.apache.spark.sql.execution.streaming.state.HDFSBackedStateStoreProvider \
  --conf spark.sql.shuffle.partitions=200 \
  target/spark-job.jar
```

状态管理建议：

| 建议                   | 说明                                |
| ---------------------- | ----------------------------------- |
| 必须配置 Checkpoint    | 状态恢复依赖 Checkpoint             |
| 必须设置合理 Watermark | 避免状态无限增长                    |
| 控制 Key 基数          | 高基数 Key 会导致状态数据膨胀       |
| 监控状态大小           | 关注 State Store 行数和内存占用     |
| 避免无界全局聚合       | 没有窗口或 Watermark 的聚合风险较高 |

### Checkpoint 配置

Checkpoint 是 Structured Streaming 的核心配置，用于保存 Offset、查询元信息、状态数据和提交日志。生产流任务必须配置稳定的 Checkpoint 路径，否则任务重启后无法准确恢复。

Checkpoint 示例：

```java
StreamingQuery query = eventDf.writeStream()
        .format("parquet")
        .option("path", "hdfs:///warehouse/dwd/user_event_stream")
        .option("checkpointLocation", "hdfs:///checkpoint/user_event_stream")
        .outputMode(OutputMode.Append())
        .start();
```

推荐 Checkpoint 路径规范：

```text
hdfs:///checkpoint/{env}/{appName}/{topic}/{jobName}
```

示例：

```text
hdfs:///checkpoint/prod/spark-user-event/ods_user_event/user_event_clean
```

Checkpoint 目录中通常包含：

| 内容       | 说明                 |
| ---------- | -------------------- |
| `metadata` | 查询元信息           |
| `offsets`  | 每批次读取的 Offset  |
| `commits`  | 已提交批次信息       |
| `sources`  | Source 相关元数据    |
| `state`    | 有状态计算的状态数据 |

Checkpoint 使用建议：

| 建议               | 说明                                   |
| ------------------ | -------------------------------------- |
| 生产必须配置       | 不配置无法稳定恢复                     |
| 路径必须持久化     | 使用 HDFS、S3、OSS 等可靠存储          |
| 不同任务不能共用   | 每个流任务独立 Checkpoint              |
| 不随意删除         | 删除后任务可能重复消费或状态丢失       |
| 变更查询逻辑需谨慎 | 部分逻辑变更后旧 Checkpoint 可能不兼容 |
| 测试和生产路径隔离 | 避免互相污染状态                       |

如果修改了流任务的 Source、状态算子或输出结构，旧 Checkpoint 可能无法继续使用，需要评估是否清理并重新启动。

### Kafka 集成

Kafka 是 Structured Streaming 最常见的数据源。Spark 读取 Kafka 时会获取消息的 `key`、`value`、`topic`、`partition`、`offset`、`timestamp` 等字段，业务数据通常在 `value` 中。

Kafka 读取并解析 JSON 示例：

```java
StructType schema = new StructType()
        .add("event_id", DataTypes.StringType)
        .add("user_id", DataTypes.StringType)
        .add("event_type", DataTypes.StringType)
        .add("event_time", DataTypes.StringType);

Dataset<Row> kafkaDf = spark.readStream()
        .format("kafka")
        .option("kafka.bootstrap.servers", "kafka-host:9092")
        .option("subscribe", "ods_user_event")
        .option("startingOffsets", "latest")
        .option("failOnDataLoss", "false")
        .option("maxOffsetsPerTrigger", "100000")
        .load();

Dataset<Row> eventDf = kafkaDf
        .selectExpr(
                "CAST(key AS STRING) AS message_key",
                "CAST(value AS STRING) AS message_value",
                "topic",
                "partition",
                "offset",
                "timestamp AS kafka_time"
        )
        .withColumn("json_data", functions.from_json(col("message_value"), schema))
        .select(
                col("json_data.event_id").alias("event_id"),
                col("json_data.user_id").alias("user_id"),
                col("json_data.event_type").alias("event_type"),
                functions.to_timestamp(col("json_data.event_time"), "yyyy-MM-dd HH:mm:ss").alias("event_time"),
                col("topic"),
                col("partition"),
                col("offset"),
                col("kafka_time")
        );
```

写入 Kafka 示例：

```java
Dataset<Row> kafkaOutputDf = resultDf.selectExpr(
        "CAST(user_id AS STRING) AS key",
        "to_json(struct(*)) AS value"
);

StreamingQuery query = kafkaOutputDf.writeStream()
        .format("kafka")
        .option("kafka.bootstrap.servers", "kafka-host:9092")
        .option("topic", "dwd_user_event")
        .option("checkpointLocation", "hdfs:///checkpoint/dwd_user_event")
        .outputMode(OutputMode.Append())
        .start();
```

Kafka 常用参数：

| 参数                      | 说明                 |
| ------------------------- | -------------------- |
| `kafka.bootstrap.servers` | Kafka Broker 地址    |
| `subscribe`               | 订阅一个或多个 Topic |
| `assign`                  | 指定 Topic 分区      |
| `subscribePattern`        | 正则订阅 Topic       |
| `startingOffsets`         | 起始 Offset          |
| `maxOffsetsPerTrigger`    | 每批最大消费消息数   |
| `failOnDataLoss`          | 数据丢失时是否失败   |
| `kafka.security.protocol` | 安全协议             |
| `kafka.sasl.mechanism`    | SASL 认证机制        |

Kafka 集成建议：

| 建议                                   | 说明                                |
| -------------------------------------- | ----------------------------------- |
| 保留 Offset 信息                       | 便于排查重复、延迟和丢失            |
| 控制每批消费量                         | 防止下游处理不过来                  |
| 生产使用稳定 Checkpoint                | Offset 恢复依赖 Checkpoint          |
| 解析失败数据单独输出                   | 避免脏消息影响主链路                |
| 写 Kafka 时保证 value 为字符串或二进制 | Kafka Sink 需要 `key`、`value` 字段 |
| 关注 Exactly Once 语义                 | 写外部系统时通常需要幂等设计配合    |

### 流批一体处理

流批一体处理是 Structured Streaming 的重要优势。通过 `foreachBatch`，可以将每个微批次的数据当作普通 DataFrame 处理，从而复用批处理逻辑，写入 Hive、JDBC、HDFS 或执行复杂 SQL。

`foreachBatch` 写 Hive 示例：

```java
StreamingQuery query = eventDf.writeStream()
        .foreachBatch((batchDf, batchId) -> {
            if (batchDf.isEmpty()) {
                log.warn("当前微批次为空，batchId={}", batchId);
                return;
            }

            log.info("开始处理微批次，batchId={}", batchId);

            Dataset<Row> resultDf = batchDf
                    .withColumn("dt", functions.date_format(col("event_time"), "yyyy-MM-dd"))
                    .select("event_id", "user_id", "event_type", "event_time", "dt");

            resultDf.write()
                    .mode(SaveMode.Append)
                    .format("parquet")
                    .insertInto("dwd.dwd_user_event");

            log.info("微批次处理完成，batchId={}", batchId);
        })
        .option("checkpointLocation", "hdfs:///checkpoint/user_event_foreach_batch")
        .start();
```

封装批处理逻辑示例。

文件位置：`src/main/java/io/github/atengk/spark/stream/UserEventBatchProcessor.java`

```java
package io.github.atengk.spark.stream;

import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.date_format;

/**
 * 用户事件微批处理器。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserEventBatchProcessor {

    /**
     * 处理单个微批次数据。
     *
     * @param batchDf 微批次数据
     * @param batchId 微批次 ID
     */
    public void process(Dataset<Row> batchDf, long batchId) {
        if (batchDf.isEmpty()) {
            log.warn("用户事件微批次为空，batchId={}", batchId);
            return;
        }

        log.info("开始处理用户事件微批次，batchId={}", batchId);

        Dataset<Row> resultDf = batchDf
                .withColumn("dt", date_format(col("event_time"), "yyyy-MM-dd"))
                .select(
                        col("event_id"),
                        col("user_id"),
                        col("event_type"),
                        col("event_time"),
                        col("topic"),
                        col("partition"),
                        col("offset"),
                        col("dt")
                );

        resultDf.write()
                .mode(SaveMode.Append)
                .format("parquet")
                .insertInto("dwd.dwd_user_event");

        log.info("用户事件微批次处理完成，batchId={}", batchId);
    }
}
```

调用方式：

```java
UserEventBatchProcessor processor = new UserEventBatchProcessor();

StreamingQuery query = eventDf.writeStream()
        .foreachBatch(processor::process)
        .option("checkpointLocation", "hdfs:///checkpoint/user_event_batch_processor")
        .start();
```

流批一体处理建议：

| 建议                   | 说明                                          |
| ---------------------- | --------------------------------------------- |
| 复用批处理逻辑         | 微批次 DataFrame 可以复用清洗、校验、写出方法 |
| 写外部系统要幂等       | `foreachBatch` 失败重试可能导致重复写入       |
| 使用 batchId 辅助去重  | 可记录已处理批次                              |
| 不在微批次中做过慢操作 | 避免批次积压                                  |
| 每批记录输入输出量     | 便于排查延迟和数据异常                        |

### 流任务故障恢复

流任务故障恢复依赖 Checkpoint、Source Offset、状态数据和下游幂等写入。故障恢复的目标是任务重启后能够从上次进度继续处理，并尽量避免数据丢失和重复。

常见故障类型：

| 故障             | 说明                            |
| ---------------- | ------------------------------- |
| Driver 失败      | 应用主进程异常退出              |
| Executor 失败    | 部分 Task 失败，可由 Spark 重试 |
| Kafka 短暂不可用 | Source 暂时无法读取             |
| HDFS 写入失败    | Sink 或 Checkpoint 写入失败     |
| 状态过大         | State Store 读写变慢或失败      |
| 代码变更不兼容   | 新逻辑无法读取旧 Checkpoint     |
| 下游写入重复     | 任务重试导致重复写入外部系统    |

故障恢复关键配置：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --conf spark.yarn.maxAppAttempts=2 \
  --conf spark.sql.streaming.stopGracefullyOnShutdown=true \
  --conf spark.sql.shuffle.partitions=200 \
  target/spark-job.jar
```

优雅停止配置说明：

| 配置                                                | 说明                         |
| --------------------------------------------------- | ---------------------------- |
| `spark.sql.streaming.stopGracefullyOnShutdown=true` | JVM 关闭时尽量优雅停止流查询 |
| `spark.yarn.maxAppAttempts`                         | YARN 应用失败重试次数        |
| `checkpointLocation`                                | 恢复 Offset 和状态的关键路径 |
| `maxOffsetsPerTrigger`                              | 控制恢复后每批处理压力       |

流任务恢复建议：

| 建议                  | 说明                                              |
| --------------------- | ------------------------------------------------- |
| 保持 Checkpoint 不变  | 正常重启应使用原 Checkpoint                       |
| 不随意删除 Checkpoint | 删除后可能重复消费或状态丢失                      |
| 下游写入设计幂等      | 使用主键、批次号、Offset 或分区覆盖               |
| 重要流任务配置监控    | 监控输入速率、处理耗时、Kafka Lag、状态大小       |
| 代码升级先评估兼容性  | Source、Sink、状态算子变更可能不兼容旧 Checkpoint |
| 恢复后检查数据连续性  | 对比 Kafka Offset、输出分区和数据量               |

故障恢复排查顺序建议如下：

```text
查看应用是否重启
        ↓
检查 Checkpoint 是否存在且可读写
        ↓
检查 Kafka Offset 是否连续
        ↓
检查最近批次 progress 信息
        ↓
检查 Sink 是否重复写入或写入失败
        ↓
检查状态目录是否过大
        ↓
根据异常决定继续使用旧 Checkpoint 或重新初始化任务
```

生产流任务的可靠性不只依赖 Spark 本身，还依赖 Kafka 保留策略、Checkpoint 存储可靠性、下游写入幂等性和调度平台重启策略。对于核心实时链路，应将这些内容作为上线检查项。


## 数据写出

本章节用于说明 Spark 任务将计算结果写出到文件系统、Hive、JDBC、Kafka 等目标端的常见方式。数据写出不仅是保存结果，还涉及文件格式、分区策略、写入模式、幂等性、小文件控制、权限校验和失败恢复等问题。生产任务中，写出逻辑应统一封装，避免不同 Job 使用不同写法导致结果不可控。

### 写出文本文件

文本文件写出适用于简单日志、调试结果、单字段结果、临时中间结果和对外导出的纯文本数据。Spark 写出文本文件时，DataFrame 只能包含一个字符串类型字段，通常命名为 `value`。

将单字段写出为文本文件：

```java
import static org.apache.spark.sql.functions.col;

Dataset<Row> textDf = sourceDf
        .select(col("user_id").cast("string").alias("value"));

textDf.write()
        .mode(SaveMode.Overwrite)
        .text("hdfs:///warehouse/export/user_id_text/dt=2026-05-11");
```

多字段拼接后写出文本文件：

```java
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.concat_ws;

Dataset<Row> textDf = sourceDf
        .select(concat_ws("\t",
                col("user_id"),
                col("user_name"),
                col("user_level")
        ).alias("value"));

textDf.write()
        .mode(SaveMode.Overwrite)
        .text("hdfs:///warehouse/export/user_text/dt=2026-05-11");
```

文本文件写出建议如下：

| 建议                       | 说明                                            |
| -------------------------- | ----------------------------------------------- |
| 只适合简单结果             | 文本文件缺少 Schema，不适合作为标准数仓存储格式 |
| 多字段需要手动拼接         | 推荐使用制表符或明确分隔符                      |
| 控制输出分区数             | 避免生成大量小文件                              |
| 不适合复杂结构             | 嵌套字段、数组、Map 不建议直接写文本            |
| 生产结果优先 Parquet / ORC | 文本更适合导出和临时排查                        |

### 写出 CSV 文件

CSV 写出适用于报表导出、外部系统交换、人工查看和简单结构化数据传输。CSV 可读性较好，但类型信息缺失，不适合作为长期数仓存储格式。

基础写出示例：

```java
sourceDf.write()
        .mode(SaveMode.Overwrite)
        .option("header", "true")
        .option("sep", ",")
        .option("encoding", "UTF-8")
        .csv("hdfs:///warehouse/export/user_csv/dt=2026-05-11");
```

写出前控制字段顺序：

```java
Dataset<Row> exportDf = sourceDf.select(
        "user_id",
        "user_name",
        "user_level",
        "order_count",
        "order_amount",
        "dt"
);

exportDf.write()
        .mode(SaveMode.Overwrite)
        .option("header", "true")
        .option("sep", ",")
        .csv("hdfs:///warehouse/export/user_summary_csv/dt=2026-05-11");
```

如果需要导出单个 CSV 文件，可以在小数据量场景下使用 `coalesce(1)`：

```java
exportDf.coalesce(1)
        .write()
        .mode(SaveMode.Overwrite)
        .option("header", "true")
        .csv("hdfs:///warehouse/export/user_summary_single/dt=2026-05-11");
```

CSV 写出注意事项：

| 注意点                     | 说明                                               |
| -------------------------- | -------------------------------------------------- |
| `coalesce(1)` 只适合小数据 | 大数据量强制单文件会导致单 Task 过慢或 OOM         |
| 写出后文件名由 Spark 生成  | 通常是 `part-xxxxx.csv`                            |
| 字段类型会丢失             | 下游读取时需要重新指定 Schema                      |
| 注意分隔符冲突             | 字段中包含逗号、换行、引号时需配置 quote 和 escape |
| 不建议作为核心数仓格式     | 长期存储优先使用 Parquet 或 ORC                    |

### 写出 JSON 文件

JSON 写出适用于半结构化数据导出、接口数据落地、消息格式归档和调试场景。Spark 默认写出为一行一个 JSON 对象，适合后续分布式读取。

基础写出示例：

```java
sourceDf.write()
        .mode(SaveMode.Overwrite)
        .json("hdfs:///warehouse/export/user_json/dt=2026-05-11");
```

写出前选择字段：

```java
Dataset<Row> jsonDf = sourceDf.select(
        "user_id",
        "user_name",
        "user_level",
        "event_time",
        "dt"
);

jsonDf.write()
        .mode(SaveMode.Append)
        .json("hdfs:///warehouse/export/user_event_json");
```

JSON 写出建议如下：

| 建议              | 说明                                    |
| ----------------- | --------------------------------------- |
| 适合半结构化数据  | 嵌套字段、数组字段可用 JSON 表达        |
| 不适合高性能分析  | 相比 Parquet / ORC，JSON 读取成本更高   |
| 推荐一行一个 JSON | 便于 Spark 后续并行读取                 |
| 控制字段类型      | 时间、金额、ID 等字段写出前应标准化     |
| 注意文件体积      | JSON 冗余字段名较多，压缩后再存储更合适 |

### 写出 Parquet 文件

Parquet 是 Spark 离线数仓和数据湖中最常用的列式存储格式之一，支持列裁剪、谓词下推、压缩和高效读取。对于 DWD、DWS、ADS 等分层数据，优先推荐使用 Parquet 或 ORC。

基础写出示例：

```java
resultDf.write()
        .mode(SaveMode.Overwrite)
        .parquet("hdfs:///warehouse/dwd/user_detail/dt=2026-05-11");
```

带压缩配置写出：

```java
resultDf.write()
        .mode(SaveMode.Overwrite)
        .option("compression", "snappy")
        .parquet("hdfs:///warehouse/dwd/user_detail/dt=2026-05-11");
```

按分区字段写出：

```java
resultDf.write()
        .mode(SaveMode.Overwrite)
        .partitionBy("dt")
        .parquet("hdfs:///warehouse/dwd/user_detail");
```

Parquet 写出建议如下：

| 建议                   | 说明                                   |
| ---------------------- | -------------------------------------- |
| 推荐用于离线明细和宽表 | 适合大规模分析型数据                   |
| 使用 Snappy 压缩       | 压缩率和性能较均衡                     |
| 写出前控制分区数       | 避免小文件过多                         |
| 字段类型保持稳定       | Schema 变化需评估下游兼容性            |
| 按查询条件设计分区     | 常用 `dt`、`hour`、`region` 等分区字段 |

### 写出 ORC 文件

ORC 是 Hive 生态中常见的列式存储格式，适合 Hive 表、离线数仓和大规模分析任务。若公司 Hive 数仓默认使用 ORC，则 Spark 写出也应遵循统一格式。

基础写出示例：

```java
resultDf.write()
        .mode(SaveMode.Overwrite)
        .orc("hdfs:///warehouse/dwd/order_detail/dt=2026-05-11");
```

使用通用格式写出：

```java
resultDf.write()
        .format("orc")
        .mode(SaveMode.Append)
        .save("hdfs:///warehouse/dwd/order_detail");
```

按分区字段写出：

```java
resultDf.write()
        .mode(SaveMode.Overwrite)
        .partitionBy("dt")
        .orc("hdfs:///warehouse/dwd/order_detail");
```

ORC 写出建议如下：

| 建议                 | 说明                                  |
| -------------------- | ------------------------------------- |
| 适合 Hive 生态       | 与 Hive 表兼容性较好                  |
| 适合列式分析         | 支持列裁剪和压缩                      |
| 与公司表格式保持一致 | 不建议同一层级混用多种格式            |
| 注意 Schema 变化     | 字段新增、删除、类型变更需评估        |
| 分区写出要规范       | 保持 `dt=yyyy-MM-dd` 这类稳定目录结构 |

### 写入 Hive 表

写入 Hive 表是 Spark 数仓任务中最常见的结果输出方式。写入 Hive 前必须开启 Hive 支持，并保证目标表存在、字段结构匹配、分区字段正确、HDFS 权限具备。

通过 `insertInto` 写入 Hive 表：

```java
resultDf.write()
        .mode(SaveMode.Append)
        .insertInto("ads.ads_user_order_summary");
```

通过 SQL 写入 Hive 表：

```java
resultDf.createOrReplaceTempView("tmp_user_order_summary");

spark.sql(
        "INSERT OVERWRITE TABLE ads.ads_user_order_summary PARTITION (dt = '2026-05-11') " +
        "SELECT user_id, user_name, order_count, order_amount, etl_time " +
        "FROM tmp_user_order_summary"
);
```

通过 `saveAsTable` 创建或写入表：

```java
resultDf.write()
        .mode(SaveMode.Overwrite)
        .format("parquet")
        .saveAsTable("ads.ads_user_order_summary");
```

Hive 表写入注意事项：

| 注意点             | 说明                                      |
| ------------------ | ----------------------------------------- |
| 字段顺序必须匹配   | `insertInto` 按字段位置写入，不是按字段名 |
| 分区字段要处理正确 | 静态分区和动态分区写法不同                |
| 写入前校验数据量   | 避免空数据覆盖正式分区                    |
| 覆盖写入需谨慎     | 生产环境覆盖前应明确分区范围              |
| 目标表格式统一     | Parquet、ORC 等格式应符合数仓规范         |

推荐在写入 Hive 前显式调整字段顺序：

```java
Dataset<Row> hiveDf = resultDf.select(
        "user_id",
        "user_name",
        "order_count",
        "order_amount",
        "etl_time",
        "dt"
);

hiveDf.write()
        .mode(SaveMode.Append)
        .insertInto("ads.ads_user_order_summary");
```

### 写入 JDBC

JDBC 写入适用于将 Spark 计算结果写入 MySQL、PostgreSQL、Oracle、SQL Server 等关系型数据库。常见场景包括报表结果落库、指标结果同步、任务审计记录和小规模维表更新。

基础写入示例：

```java
Properties properties = new Properties();
properties.setProperty("user", System.getenv("MYSQL_USERNAME"));
properties.setProperty("password", System.getenv("MYSQL_PASSWORD"));
properties.setProperty("driver", "com.mysql.cj.jdbc.Driver");

resultDf.write()
        .mode(SaveMode.Append)
        .jdbc(
                "jdbc:mysql://mysql-host:3306/report?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai",
                "ads_user_order_summary",
                properties
        );
```

使用批量写入参数：

```java
resultDf.write()
        .mode(SaveMode.Append)
        .format("jdbc")
        .option("url", "jdbc:mysql://mysql-host:3306/report?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai")
        .option("driver", "com.mysql.cj.jdbc.Driver")
        .option("dbtable", "ads_user_order_summary")
        .option("user", System.getenv("MYSQL_USERNAME"))
        .option("password", System.getenv("MYSQL_PASSWORD"))
        .option("batchsize", "5000")
        .option("isolationLevel", "READ_COMMITTED")
        .save();
```

JDBC 写入建议如下：

| 建议           | 说明                               |
| -------------- | ---------------------------------- |
| 控制写入数据量 | JDBC 不适合超大规模明细写入        |
| 控制分区数     | 分区数过多会创建过多数据库连接     |
| 设置 batchsize | 提高批量写入效率                   |
| 写入前去重     | 避免目标表主键冲突                 |
| 使用临时表中转 | 复杂更新、覆盖、合并建议先写临时表 |
| 生产密码外置   | 不要将数据库密码写入代码或配置文件 |

写入关系型数据库前，建议控制分区数：

```java
resultDf.coalesce(8)
        .write()
        .mode(SaveMode.Append)
        .format("jdbc")
        .option("url", "jdbc:mysql://mysql-host:3306/report")
        .option("driver", "com.mysql.cj.jdbc.Driver")
        .option("dbtable", "ads_user_order_summary")
        .option("user", System.getenv("MYSQL_USERNAME"))
        .option("password", System.getenv("MYSQL_PASSWORD"))
        .option("batchsize", "5000")
        .save();
```

### 写入 Kafka

写入 Kafka 常用于实时链路下游分发、实时清洗后消息转发、告警消息推送和指标变更通知。Spark 写入 Kafka 时，DataFrame 必须包含 `value` 字段，`key` 字段可选，二者通常需要是字符串或二进制类型。

Structured Streaming 写入 Kafka 示例：

```java
Dataset<Row> kafkaOutputDf = resultDf.selectExpr(
        "CAST(user_id AS STRING) AS key",
        "to_json(struct(*)) AS value"
);

StreamingQuery query = kafkaOutputDf.writeStream()
        .format("kafka")
        .option("kafka.bootstrap.servers", "kafka-host:9092")
        .option("topic", "dwd_user_event")
        .option("checkpointLocation", "hdfs:///checkpoint/dwd_user_event_to_kafka")
        .outputMode(OutputMode.Append())
        .start();
```

批处理写入 Kafka 示例：

```java
Dataset<Row> kafkaOutputDf = resultDf.selectExpr(
        "CAST(user_id AS STRING) AS key",
        "to_json(struct(*)) AS value"
);

kafkaOutputDf.write()
        .format("kafka")
        .option("kafka.bootstrap.servers", "kafka-host:9092")
        .option("topic", "ads_user_summary")
        .save();
```

Kafka 写入建议如下：

| 建议                        | 说明                              |
| --------------------------- | --------------------------------- |
| 必须包含 `value` 字段       | Kafka Sink 的必要字段             |
| `key` 用于分区路由          | 可使用 user_id、order_id 等业务键 |
| value 推荐 JSON 字符串      | 便于下游解析                      |
| 流式写入必须配置 Checkpoint | 保障故障恢复                      |
| 下游需要处理重复消息        | Spark 重试可能导致至少一次写入    |
| 控制消息大小                | 不建议向 Kafka 写入超大 JSON      |

### 分区写入

分区写入用于将数据按业务日期、小时、地区、渠道等字段拆分为目录或 Hive 分区。合理的分区设计可以提升查询效率，降低扫描范围，但分区过细会造成小文件和元数据压力。

按日期分区写出文件：

```java
resultDf.write()
        .mode(SaveMode.Append)
        .partitionBy("dt")
        .parquet("hdfs:///warehouse/dwd/user_event");
```

按日期和小时分区写出：

```java
resultDf.write()
        .mode(SaveMode.Append)
        .partitionBy("dt", "hour")
        .parquet("hdfs:///warehouse/dwd/user_event");
```

写出前调整分区数：

```java
resultDf.repartition(col("dt"))
        .write()
        .mode(SaveMode.Append)
        .partitionBy("dt")
        .parquet("hdfs:///warehouse/dwd/user_event");
```

分区写入建议如下：

| 建议                     | 说明                                   |
| ------------------------ | -------------------------------------- |
| 常用查询条件适合作为分区 | 例如 `dt`、`hour`、`region`            |
| 分区字段基数不宜过高     | 用户 ID、订单 ID 不适合作为分区字段    |
| 控制输出文件数量         | 写出前合理 `repartition` 或 `coalesce` |
| 分区字段值要规范         | 避免 `null`、空字符串、非法日期        |
| Hive 表分区需同步元数据  | 非 Hive 写入路径时可能需要修复分区     |

常见分区目录示例：

```text
hdfs:///warehouse/dwd/user_event
├── dt=2026-05-10
│   ├── part-00000.snappy.parquet
│   └── part-00001.snappy.parquet
└── dt=2026-05-11
    ├── part-00000.snappy.parquet
    └── part-00001.snappy.parquet
```

### 覆盖写入与追加写入

Spark 常见写入模式包括 `Overwrite`、`Append`、`ErrorIfExists` 和 `Ignore`。生产任务中最常用的是覆盖写入和追加写入。选择写入模式时，需要结合任务是否支持重跑、目标表是否分区、是否存在幂等要求来决定。

写入模式说明：

| 模式            | 说明                 |
| --------------- | -------------------- |
| `Overwrite`     | 覆盖目标路径或表数据 |
| `Append`        | 追加写入目标路径或表 |
| `ErrorIfExists` | 目标存在时直接失败   |
| `Ignore`        | 目标存在时忽略写入   |

覆盖写入示例：

```java
resultDf.write()
        .mode(SaveMode.Overwrite)
        .parquet("hdfs:///warehouse/ads/user_summary/dt=2026-05-11");
```

追加写入示例：

```java
resultDf.write()
        .mode(SaveMode.Append)
        .insertInto("ads.ads_user_order_summary");
```

覆盖动态分区时，建议显式配置分区覆盖模式：

```java
spark.conf().set("spark.sql.sources.partitionOverwriteMode", "dynamic");

resultDf.write()
        .mode(SaveMode.Overwrite)
        .insertInto("ads.ads_user_order_summary");
```

覆盖与追加选择建议：

| 场景             | 推荐模式             |
| ---------------- | -------------------- |
| 离线按天重算分区 | `Overwrite`          |
| 明细增量不断追加 | `Append`             |
| 实时流式写入     | 通常 `Append`        |
| 补数任务         | 指定分区 `Overwrite` |
| 审计日志表       | `Append`             |
| 全量维表快照     | `Overwrite`          |

生产环境中，覆盖写入必须明确覆盖范围。不要在未指定分区条件的情况下覆盖整张 Hive 表或整个数据目录。

## Hive 集成

本章节用于说明 Spark 与 Hive 的集成方式，包括 Hive Metastore 配置、Hive 表读写、分区表处理、动态分区写入、内部表与外部表选择、Hive SQL 兼容性以及权限安全。Spark 集成 Hive 后，可以通过 Spark SQL 直接访问 Hive 元数据和表数据，是离线数仓开发的核心能力之一。

### Hive Metastore 配置

Hive Metastore 保存 Hive 库、表、字段、分区、存储路径、文件格式等元数据信息。Spark 访问 Hive 表时，需要通过 Hive Metastore 获取表结构和存储位置。

基础 `hive-site.xml` 示例：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Hive Metastore Thrift 服务地址 -->
    <property>
        <name>hive.metastore.uris</name>
        <value>thrift://hive-metastore:9083</value>
    </property>

    <!-- Hive Warehouse 默认目录 -->
    <property>
        <name>hive.metastore.warehouse.dir</name>
        <value>/user/hive/warehouse</value>
    </property>

    <!-- Metastore 连接重试次数 -->
    <property>
        <name>hive.metastore.connect.retries</name>
        <value>3</value>
    </property>

    <!-- Metastore 连接重试间隔 -->
    <property>
        <name>hive.metastore.client.connect.retry.delay</name>
        <value>5s</value>
    </property>
</configuration>
```

SparkSession 开启 Hive 支持：

```java
SparkSession spark = SparkSession.builder()
        .appName("spark-hive-job")
        .enableHiveSupport()
        .getOrCreate();
```

提交任务时确保 Hive 配置可见：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --files /opt/module/hive/conf/hive-site.xml \
  target/spark-job.jar \
  --env prod \
  --bizDate 2026-05-11
```

Hive Metastore 配置建议如下：

| 建议                       | 说明                                     |
| -------------------------- | ---------------------------------------- |
| 测试和生产 Metastore 隔离  | 避免误读误写生产表                       |
| `hive-site.xml` 随任务提交 | 确保 Driver 和 Executor 能读取配置       |
| Warehouse 路径统一         | 不同环境使用独立路径                     |
| 开启 Hive 支持             | 使用 Hive 表必须 `enableHiveSupport()`   |
| 配置变更需谨慎             | Metastore 地址变更会影响所有 Hive 表访问 |

### Hive 表读取

Spark 可以通过 `spark.table()` 或 `spark.sql()` 读取 Hive 表。读取分区表时必须带分区条件，避免全表扫描。

通过 `spark.table()` 读取：

```java
Dataset<Row> userDf = spark.table("dwd.dwd_user_detail")
        .filter(col("dt").equalTo("2026-05-11"))
        .select("user_id", "user_name", "user_level", "dt");
```

通过 SQL 读取：

```java
Dataset<Row> orderDf = spark.sql(
        "SELECT order_id, user_id, order_amount, order_time, dt " +
        "FROM dwd.dwd_order_detail " +
        "WHERE dt = '2026-05-11'"
);
```

读取前查看库表：

```java
spark.sql("SHOW DATABASES").show(false);
spark.sql("SHOW TABLES IN dwd").show(false);
spark.sql("DESCRIBE FORMATTED dwd.dwd_user_detail").show(false);
```

Hive 表读取建议如下：

| 建议                      | 说明                                    |
| ------------------------- | --------------------------------------- |
| 分区表必须带分区条件      | 防止扫描全表                            |
| 只读取必要字段            | 利用列裁剪减少 IO                       |
| 不建议生产使用 `SELECT *` | 字段变化可能影响下游                    |
| 读取前确认环境            | 防止 test/prod 库表混用                 |
| 大表 Join 前先裁剪字段    | 降低 Shuffle 成本                       |
| 读取失败先查权限和元数据  | 常见问题是 HDFS 权限或 Metastore 不可达 |

### Hive 表写入

Hive 表写入可以使用 DataFrameWriter，也可以使用 Spark SQL 的 `INSERT INTO` 或 `INSERT OVERWRITE`。生产环境中推荐对目标表结构、字段顺序和分区字段进行显式控制。

追加写入 Hive 表：

```java
Dataset<Row> hiveDf = resultDf.select(
        "user_id",
        "user_name",
        "order_count",
        "order_amount",
        "etl_time",
        "dt"
);

hiveDf.write()
        .mode(SaveMode.Append)
        .insertInto("ads.ads_user_order_summary");
```

覆盖指定分区：

```java
resultDf.createOrReplaceTempView("tmp_user_summary");

spark.sql(
        "INSERT OVERWRITE TABLE ads.ads_user_order_summary PARTITION (dt = '2026-05-11') " +
        "SELECT user_id, user_name, order_count, order_amount, etl_time " +
        "FROM tmp_user_summary"
);
```

创建 Hive 表后写入：

```sql
CREATE TABLE IF NOT EXISTS ads.ads_user_order_summary (
    user_id STRING COMMENT '用户ID',
    user_name STRING COMMENT '用户名称',
    order_count BIGINT COMMENT '订单数量',
    order_amount DECIMAL(18,2) COMMENT '订单金额',
    etl_time TIMESTAMP COMMENT 'ETL处理时间'
)
COMMENT '用户订单汇总表'
PARTITIONED BY (dt STRING COMMENT '分区日期')
STORED AS PARQUET;
```

Hive 写入建议如下：

| 建议                   | 说明                           |
| ---------------------- | ------------------------------ |
| 写入前调整字段顺序     | `insertInto` 对字段顺序敏感    |
| 分区字段放在最后       | 与 Hive 表结构保持一致         |
| 写入前做质量校验       | 避免空数据覆盖有效分区         |
| 覆盖写入只覆盖目标分区 | 不要误覆盖整表                 |
| 生产写入记录审计日志   | 记录目标表、分区、写入量、耗时 |

### 分区表处理

Hive 分区表通过分区字段将数据映射到不同目录，常见分区字段为 `dt`、`hour`、`region` 等。合理使用分区可以显著减少查询扫描范围。

创建分区表示例：

```sql
CREATE TABLE IF NOT EXISTS dwd.dwd_user_event (
    event_id STRING COMMENT '事件ID',
    user_id STRING COMMENT '用户ID',
    event_type STRING COMMENT '事件类型',
    event_time TIMESTAMP COMMENT '事件时间',
    etl_time TIMESTAMP COMMENT 'ETL处理时间'
)
COMMENT '用户事件明细表'
PARTITIONED BY (
    dt STRING COMMENT '分区日期'
)
STORED AS PARQUET;
```

查看分区：

```sql
SHOW PARTITIONS dwd.dwd_user_event;
```

添加分区：

```sql
ALTER TABLE dwd.dwd_user_event
ADD IF NOT EXISTS PARTITION (dt = '2026-05-11')
LOCATION 'hdfs:///warehouse/dwd/dwd_user_event/dt=2026-05-11';
```

删除分区：

```sql
ALTER TABLE dwd.dwd_user_event
DROP IF EXISTS PARTITION (dt = '2026-05-11');
```

修复分区：

```sql
MSCK REPAIR TABLE dwd.dwd_user_event;
```

分区表处理建议如下：

| 建议                   | 说明                              |
| ---------------------- | --------------------------------- |
| 查询必须带分区条件     | 提升性能并降低资源消耗            |
| 分区字段不宜过多       | 分区过细会造成元数据压力          |
| 分区字段基数不宜过高   | 不要用 user_id、order_id 作为分区 |
| 分区值格式统一         | 日期推荐 `yyyy-MM-dd`             |
| 谨慎使用 `MSCK REPAIR` | 大表分区很多时执行较慢            |
| 删除分区前确认数据     | 防止误删生产数据                  |

### 动态分区写入

动态分区写入是指 Spark 根据 DataFrame 中的分区字段值自动写入不同 Hive 分区。适合一次处理多个日期、多个小时或多个区域的数据。

开启动态分区配置：

```java
spark.sql("SET hive.exec.dynamic.partition=true");
spark.sql("SET hive.exec.dynamic.partition.mode=nonstrict");
spark.sql("SET spark.sql.sources.partitionOverwriteMode=dynamic");
```

动态分区写入示例：

```java
Dataset<Row> hiveDf = resultDf.select(
        "event_id",
        "user_id",
        "event_type",
        "event_time",
        "etl_time",
        "dt"
);

hiveDf.write()
        .mode(SaveMode.Append)
        .insertInto("dwd.dwd_user_event");
```

使用 SQL 动态分区写入：

```sql
INSERT INTO TABLE dwd.dwd_user_event PARTITION (dt)
SELECT
    event_id,
    user_id,
    event_type,
    event_time,
    etl_time,
    dt
FROM tmp_user_event;
```

动态覆盖分区：

```java
spark.conf().set("spark.sql.sources.partitionOverwriteMode", "dynamic");

hiveDf.write()
        .mode(SaveMode.Overwrite)
        .insertInto("dwd.dwd_user_event");
```

动态分区写入注意事项：

| 注意点                     | 说明                             |
| -------------------------- | -------------------------------- |
| DataFrame 必须包含分区字段 | 例如 `dt` 字段必须存在           |
| 字段顺序必须匹配 Hive 表   | 分区字段通常放最后               |
| 控制分区数量               | 一次写入过多分区会造成元数据压力 |
| 空分区值要提前处理         | 避免生成 `dt=null` 或异常分区    |
| 覆盖模式必须谨慎           | 动态覆盖前确认 Spark 配置生效    |

### 外部表与内部表

Hive 表分为内部表和外部表。内部表由 Hive 管理数据生命周期，删除表时通常会删除数据；外部表只管理元数据，删除表时通常不删除底层数据。数仓和数据湖场景中，外部表更常见。

内部表示例：

```sql
CREATE TABLE IF NOT EXISTS ads.ads_user_order_summary (
    user_id STRING COMMENT '用户ID',
    user_name STRING COMMENT '用户名称',
    order_count BIGINT COMMENT '订单数量',
    order_amount DECIMAL(18,2) COMMENT '订单金额'
)
PARTITIONED BY (dt STRING COMMENT '分区日期')
STORED AS PARQUET;
```

外部表示例：

```sql
CREATE EXTERNAL TABLE IF NOT EXISTS dwd.dwd_user_event (
    event_id STRING COMMENT '事件ID',
    user_id STRING COMMENT '用户ID',
    event_type STRING COMMENT '事件类型',
    event_time TIMESTAMP COMMENT '事件时间'
)
PARTITIONED BY (dt STRING COMMENT '分区日期')
STORED AS PARQUET
LOCATION 'hdfs:///warehouse/dwd/dwd_user_event';
```

内部表与外部表对比：

| 类型   | 数据生命周期                            | 适用场景                             |
| ------ | --------------------------------------- | ------------------------------------ |
| 内部表 | Hive 管理数据，删除表可能删除数据       | 临时表、测试表、完全由 Hive 管理的表 |
| 外部表 | Hive 只管理元数据，删除表通常不删除数据 | 数据湖、数仓分层、跨引擎共享数据     |

选择建议：

| 场景             | 推荐             |
| ---------------- | ---------------- |
| 生产数仓表       | 外部表           |
| 多计算引擎共享   | 外部表           |
| 临时计算结果     | 内部表或临时目录 |
| 测试验证表       | 内部表           |
| 原始落地数据     | 外部表           |
| 需要手动管理路径 | 外部表           |

生产环境中，建议核心 ODS、DWD、DWS、ADS 表使用外部表，并由统一的数据目录规范管理 HDFS 或对象存储路径。

### Hive SQL 兼容性

Spark SQL 支持大量 Hive SQL 语法，但并不完全等同于 Hive 引擎。迁移 Hive SQL 到 Spark 执行时，需要关注函数兼容性、数据类型、动态分区、SerDe、执行配置和 SQL 语义差异。

常见兼容点如下：

| 类型        | 说明                                           |
| ----------- | ---------------------------------------------- |
| 基础 SQL    | `SELECT`、`WHERE`、`GROUP BY`、`JOIN` 基本兼容 |
| Hive 表读取 | 开启 Hive 支持后可读取 Hive Metastore 表       |
| 分区写入    | 支持静态分区和动态分区                         |
| 内置函数    | 大部分常见函数兼容                             |
| UDF         | Hive UDF 不一定可直接在 Spark 中使用           |
| SerDe 表    | 特殊 SerDe 表可能存在兼容问题                  |

常见差异如下：

| 差异               | 说明                                        |
| ------------------ | ------------------------------------------- |
| 函数行为差异       | 个别 Hive 函数与 Spark SQL 函数结果可能不同 |
| 类型转换差异       | 字符串、日期、Decimal 转换需重点验证        |
| 动态分区配置不同   | Spark 和 Hive 参数都可能影响写入            |
| 小文件处理方式不同 | Spark 写出文件数量由分区数决定              |
| 执行计划不同       | Spark 使用 Catalyst 和 Tungsten 优化        |
| Hive UDF 兼容问题  | 旧 Hive UDF 可能依赖 Hive 运行时环境        |

迁移 Hive SQL 时建议先执行验证：

```java
Dataset<Row> resultDf = spark.sql(sql);

// 查看执行计划，确认是否分区裁剪、广播 Join、谓词下推
resultDf.explain("formatted");

// 先查看少量样例
resultDf.show(20, false);
```

Hive SQL 兼容性建议：

| 建议                      | 说明                                      |
| ------------------------- | ----------------------------------------- |
| 复杂 SQL 先在测试环境验证 | 不要直接替换生产 Hive 任务                |
| 重点验证日期函数          | 时间格式和时区容易出现差异                |
| 重点验证 Decimal          | 金额类指标需检查精度                      |
| 重点验证 UDF              | Hive UDF 可能不能直接复用                 |
| 对比历史结果              | 使用相同业务日期对比 Hive 与 Spark 输出   |
| 查看执行计划              | 发现全表扫描、Shuffle 过多、Join 策略异常 |

### Hive 权限与安全

Spark 访问 Hive 表不仅需要 Hive Metastore 权限，还需要底层 HDFS 或对象存储路径权限。在开启 Kerberos、Ranger、Sentry 或其他权限系统的集群中，还需要配置认证和授权信息。

常见权限类型如下：

| 权限                 | 说明                             |
| -------------------- | -------------------------------- |
| Hive 库表权限        | 是否允许查询、写入、建表、删表   |
| HDFS 路径权限        | 是否允许读取和写入表目录         |
| Metastore 访问权限   | 是否允许访问 Hive 元数据服务     |
| YARN 队列权限        | 是否允许提交到指定队列           |
| Kerberos 认证        | 是否具备合法 principal 和 keytab |
| Ranger / Sentry 权限 | 是否通过统一权限系统授权         |

Kerberos 提交示例：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --principal spark-user@EXAMPLE.COM \
  --keytab /etc/security/keytabs/spark-user.keytab \
  --files /opt/module/hive/conf/hive-site.xml,/etc/krb5.conf \
  target/spark-job.jar \
  --env prod \
  --bizDate 2026-05-11
```

HDFS 权限检查命令：

```bash
# 查看表目录权限
hdfs dfs -ls /warehouse/dwd/dwd_user_event

# 查看当前用户是否能写入目标目录
hdfs dfs -touchz /warehouse/dwd/dwd_user_event/_permission_test

# 删除测试文件
hdfs dfs -rm /warehouse/dwd/dwd_user_event/_permission_test
```

Hive 权限与安全建议如下：

| 建议             | 说明                                     |
| ---------------- | ---------------------------------------- |
| 使用任务专用账号 | 不建议使用个人账号长期跑生产任务         |
| 最小权限授权     | 只授予任务需要的库表和路径权限           |
| Keytab 安全管理  | 不提交到 Git，不写入普通配置文件         |
| 敏感参数脱敏     | 日志中不打印密码、Token、Keytab 路径细节 |
| 测试生产权限隔离 | 避免测试任务误写生产表                   |
| 失败先查权限     | 写 Hive 失败常见原因是 HDFS 路径无权限   |
| 审计任务写入     | 记录任务账号、目标表、分区、写入量和时间 |

生产环境中，Hive 写入任务上线前应至少检查：目标库表是否存在、字段结构是否匹配、分区策略是否正确、任务账号是否有 Hive 和 HDFS 权限、覆盖范围是否可控、失败后是否可以安全重跑。


## Kafka 集成

本章节用于说明 Spark 与 Kafka 的集成方式，包括 Kafka 数据读取、数据写入、Topic 配置、Offset 管理、Consumer Group 管理、消息序列化与反序列化，以及 Exactly Once 相关处理。Kafka 通常用于实时数据采集、日志传输、事件流处理和实时指标计算，在 Spark 项目中主要通过 Structured Streaming 进行集成。

### Kafka 数据读取

Spark 读取 Kafka 数据时，通常使用 `spark-sql-kafka-0-10` Connector。读取后的 DataFrame 默认包含 `key`、`value`、`topic`、`partition`、`offset`、`timestamp`、`timestampType` 等字段，其中 `key` 和 `value` 是二进制类型，业务开发中通常需要转换为字符串或 JSON 结构。

Structured Streaming 读取 Kafka 示例：

```java
Dataset<Row> kafkaDf = spark.readStream()
        .format("kafka")
        .option("kafka.bootstrap.servers", "kafka-host:9092")
        .option("subscribe", "ods_user_event")
        .option("startingOffsets", "latest")
        .option("failOnDataLoss", "false")
        .option("maxOffsetsPerTrigger", "100000")
        .load();

Dataset<Row> messageDf = kafkaDf.selectExpr(
        "CAST(key AS STRING) AS message_key",
        "CAST(value AS STRING) AS message_value",
        "topic",
        "partition",
        "offset",
        "timestamp AS kafka_time"
);
```

批模式读取 Kafka 指定 Offset 范围：

```java
Dataset<Row> kafkaBatchDf = spark.read()
        .format("kafka")
        .option("kafka.bootstrap.servers", "kafka-host:9092")
        .option("subscribe", "ods_user_event")
        .option("startingOffsets", "{\"ods_user_event\":{\"0\":100,\"1\":200}}")
        .option("endingOffsets", "{\"ods_user_event\":{\"0\":1000,\"1\":1200}}")
        .load();
```

Kafka 读取常用参数如下：

| 参数                      | 说明                                           |
| ------------------------- | ---------------------------------------------- |
| `kafka.bootstrap.servers` | Kafka Broker 地址                              |
| `subscribe`               | 订阅一个或多个 Topic                           |
| `assign`                  | 指定 Topic 和分区                              |
| `subscribePattern`        | 使用正则订阅 Topic                             |
| `startingOffsets`         | 起始 Offset，支持 `earliest`、`latest` 或 JSON |
| `endingOffsets`           | 批读取结束 Offset                              |
| `failOnDataLoss`          | 数据丢失时是否失败                             |
| `maxOffsetsPerTrigger`    | 每个微批次最大读取消息数量                     |
| `includeHeaders`          | 是否读取 Kafka Headers                         |

读取 Kafka 时建议保留 `topic`、`partition`、`offset` 和 `kafka_time` 字段，便于后续排查重复消费、数据延迟、消息丢失和重放问题。

### Kafka 数据写入

Spark 写入 Kafka 时，DataFrame 必须包含 `value` 字段，`key` 字段可选。`key` 和 `value` 通常需要转换为字符串或二进制类型。流式写入 Kafka 必须配置 Checkpoint，用于记录进度和支持任务恢复。

流式写入 Kafka 示例：

```java
Dataset<Row> kafkaOutputDf = resultDf.selectExpr(
        "CAST(user_id AS STRING) AS key",
        "to_json(struct(*)) AS value"
);

StreamingQuery query = kafkaOutputDf.writeStream()
        .format("kafka")
        .option("kafka.bootstrap.servers", "kafka-host:9092")
        .option("topic", "dwd_user_event")
        .option("checkpointLocation", "hdfs:///checkpoint/prod/dwd_user_event_to_kafka")
        .outputMode(OutputMode.Append())
        .start();

query.awaitTermination();
```

批处理写入 Kafka 示例：

```java
Dataset<Row> kafkaOutputDf = resultDf.selectExpr(
        "CAST(user_id AS STRING) AS key",
        "to_json(struct(*)) AS value"
);

kafkaOutputDf.write()
        .format("kafka")
        .option("kafka.bootstrap.servers", "kafka-host:9092")
        .option("topic", "ads_user_summary")
        .save();
```

Kafka 写入建议如下：

| 建议                        | 说明                                     |
| --------------------------- | ---------------------------------------- |
| 必须提供 `value` 字段       | Kafka Sink 要求存在 `value`              |
| 推荐提供 `key` 字段         | 便于按业务键分区，例如 user_id、order_id |
| value 推荐 JSON             | 下游系统解析更通用                       |
| 控制消息大小                | 避免单条消息过大影响 Broker 和 Consumer  |
| 流式写入必须配置 Checkpoint | 用于故障恢复                             |
| 下游必须具备幂等能力        | Spark 失败重试可能导致重复写入           |

### Topic 配置

Topic 是 Kafka 消息的逻辑分类。Spark 任务读取或写入 Kafka 前，需要明确 Topic 名称、分区数、副本数、数据保留时间、压缩策略、权限配置和命名规范。

Topic 命名建议如下：

```text
{layer}_{domain}_{event}
```

示例：

```text
ods_user_event
dwd_user_event
ads_user_summary
metric_order_realtime
```

常见 Topic 配置项如下：

| 配置                 | 说明                                   |
| -------------------- | -------------------------------------- |
| `partitions`         | Topic 分区数量，影响并行度             |
| `replication.factor` | 副本数，影响高可用能力                 |
| `retention.ms`       | 消息保留时间                           |
| `cleanup.policy`     | 清理策略，例如 `delete` 或 `compact`   |
| `compression.type`   | 压缩类型，例如 `snappy`、`lz4`、`zstd` |
| `max.message.bytes`  | 单条消息最大大小                       |

创建 Topic 示例：

```bash
kafka-topics.sh \
  --bootstrap-server kafka-host:9092 \
  --create \
  --topic ods_user_event \
  --partitions 12 \
  --replication-factor 3
```

查看 Topic 配置：

```bash
kafka-topics.sh \
  --bootstrap-server kafka-host:9092 \
  --describe \
  --topic ods_user_event
```

Topic 配置建议如下：

| 建议                     | 说明                                    |
| ------------------------ | --------------------------------------- |
| 分区数与吞吐匹配         | 分区数过少会限制 Spark 并行消费能力     |
| 副本数生产至少为 3       | 提高 Kafka 高可用能力                   |
| 保留时间覆盖故障恢复窗口 | 防止 Spark 停机后 Offset 对应消息被清理 |
| Topic 命名体现数据层级   | 便于区分 ODS、DWD、ADS 等链路           |
| 不随意扩分区             | 扩分区可能影响 Key 分区顺序             |

### Offset 管理

Offset 表示 Kafka 分区中消息的位置。Structured Streaming 使用 Checkpoint 管理 Offset，任务重启后会从 Checkpoint 记录的位置继续消费。生产流任务不应依赖手动记录 Offset，除非是特殊审计或批读取场景。

Structured Streaming 的 Offset 恢复依赖以下配置：

```java
StreamingQuery query = messageDf.writeStream()
        .format("parquet")
        .option("path", "hdfs:///warehouse/dwd/user_event")
        .option("checkpointLocation", "hdfs:///checkpoint/prod/user_event_clean")
        .outputMode(OutputMode.Append())
        .start();
```

常见 Offset 策略：

| 策略              | 说明                                   |
| ----------------- | -------------------------------------- |
| `earliest`        | 从最早可用消息开始消费                 |
| `latest`          | 从最新消息开始消费                     |
| JSON Offset       | 指定 Topic、分区和 Offset              |
| Checkpoint Offset | Structured Streaming 自动恢复的 Offset |

批任务指定 Offset 示例：

```java
Dataset<Row> batchDf = spark.read()
        .format("kafka")
        .option("kafka.bootstrap.servers", "kafka-host:9092")
        .option("subscribe", "ods_user_event")
        .option("startingOffsets", "{\"ods_user_event\":{\"0\":1000}}")
        .option("endingOffsets", "{\"ods_user_event\":{\"0\":2000}}")
        .load();
```

Offset 管理建议如下：

| 建议                      | 说明                                                         |
| ------------------------- | ------------------------------------------------------------ |
| 流任务必须配置 Checkpoint | Offset 由 Checkpoint 管理                                    |
| 不随意删除 Checkpoint     | 删除后可能重复消费或跳过数据                                 |
| Kafka 保留时间要足够长    | 保证任务故障恢复时 Offset 仍可读取                           |
| 保留 Offset 元信息        | 写出数据中可保留 topic、partition、offset                    |
| 重新初始化任务需明确起点  | 删除 Checkpoint 后必须明确使用 earliest、latest 或指定 Offset |
| 关键链路记录消费进度      | 可将每批 Offset 写入审计表                                   |

### Consumer Group 管理

在 Spark Structured Streaming 中，Kafka Consumer Group 的管理方式与普通 Kafka Consumer 不完全相同。Spark 通常通过 Kafka Source 管理消费进度，并将 Offset 保存在 Checkpoint 中。实际开发中，更多关注的是 Checkpoint 路径和查询 ID，而不是手动提交 Kafka Consumer Group Offset。

读取 Kafka 时可以设置 `kafka.group.id`，但生产中需要谨慎，因为多个查询共用同一个 Group ID 可能造成消费干扰：

```java
Dataset<Row> kafkaDf = spark.readStream()
        .format("kafka")
        .option("kafka.bootstrap.servers", "kafka-host:9092")
        .option("subscribe", "ods_user_event")
        .option("kafka.group.id", "spark-user-event-clean")
        .option("startingOffsets", "latest")
        .load();
```

Consumer Group 使用建议如下：

| 建议                             | 说明                                              |
| -------------------------------- | ------------------------------------------------- |
| 一般不强制指定 `kafka.group.id`  | 交由 Spark 管理更稳妥                             |
| 不同流任务使用独立 Checkpoint    | Checkpoint 才是恢复关键                           |
| 不同业务任务不要共用 Group ID    | 避免消费状态互相影响                              |
| Kafka Lag 监控需结合 Spark 进度  | 普通 Consumer Group Lag 不一定完全反映 Spark 进度 |
| 重启任务优先保持 Checkpoint 不变 | 保证从上次进度继续处理                            |

生产环境中，建议以 Spark Streaming Query 的 Checkpoint 路径作为任务消费状态标识，并通过 Spark UI、Streaming Query Progress、Kafka Lag 监控共同判断消费状态。

### 数据反序列化

Kafka 中的消息通常以字符串、JSON、Avro、Protobuf 或自定义二进制格式存储。Spark 读取 Kafka 后，`value` 字段是二进制类型，需要根据消息格式进行反序列化。

JSON 反序列化示例：

```java
StructType schema = new StructType()
        .add("event_id", DataTypes.StringType)
        .add("user_id", DataTypes.StringType)
        .add("event_type", DataTypes.StringType)
        .add("event_time", DataTypes.StringType);

Dataset<Row> parsedDf = kafkaDf
        .selectExpr("CAST(value AS STRING) AS message_value", "topic", "partition", "offset", "timestamp")
        .withColumn("json_data", functions.from_json(functions.col("message_value"), schema))
        .select(
                functions.col("json_data.event_id").alias("event_id"),
                functions.col("json_data.user_id").alias("user_id"),
                functions.col("json_data.event_type").alias("event_type"),
                functions.to_timestamp(functions.col("json_data.event_time"), "yyyy-MM-dd HH:mm:ss").alias("event_time"),
                functions.col("topic"),
                functions.col("partition"),
                functions.col("offset"),
                functions.col("timestamp").alias("kafka_time")
        );
```

解析失败数据单独输出示例：

```java
Dataset<Row> rawDf = kafkaDf.selectExpr(
        "CAST(value AS STRING) AS message_value",
        "topic",
        "partition",
        "offset",
        "timestamp"
);

Dataset<Row> parsedDf = rawDf.withColumn("json_data", functions.from_json(functions.col("message_value"), schema));

Dataset<Row> invalidDf = parsedDf
        .filter(functions.col("json_data").isNull())
        .select("message_value", "topic", "partition", "offset", "timestamp");

Dataset<Row> validDf = parsedDf
        .filter(functions.col("json_data").isNotNull())
        .select("json_data.*", "topic", "partition", "offset", "timestamp");
```

反序列化建议如下：

| 建议                 | 说明                                 |
| -------------------- | ------------------------------------ |
| 明确消息 Schema      | 不建议生产环境完全依赖自动推断       |
| 解析失败数据单独落地 | 便于追踪脏消息                       |
| 保留 Kafka 元信息    | topic、partition、offset 是排查依据  |
| 时间字段统一转换     | 将字符串时间转换为 timestamp         |
| Schema 变更需兼容    | 新增字段可以兼容，字段类型变更需谨慎 |

### 数据序列化

写入 Kafka 前，需要将 Spark DataFrame 转换为 Kafka Sink 要求的格式。最常见方式是使用 `to_json(struct(*))` 将整行转换为 JSON 字符串。

JSON 序列化示例：

```java
Dataset<Row> kafkaOutputDf = resultDf.selectExpr(
        "CAST(user_id AS STRING) AS key",
        "to_json(struct(user_id, event_type, event_time, dt)) AS value"
);
```

保留全部字段写入：

```java
Dataset<Row> kafkaOutputDf = resultDf.selectExpr(
        "CAST(user_id AS STRING) AS key",
        "to_json(struct(*)) AS value"
);
```

构建嵌套 JSON 示例：

```java
Dataset<Row> kafkaOutputDf = resultDf.selectExpr(
        "CAST(user_id AS STRING) AS key",
        "to_json(named_struct(" +
                "'userId', user_id, " +
                "'eventType', event_type, " +
                "'eventTime', cast(event_time as string), " +
                "'properties', named_struct('source', source, 'dt', dt)" +
                ")) AS value"
);
```

序列化建议如下：

| 建议                   | 说明                                |
| ---------------------- | ----------------------------------- |
| value 使用 JSON 字符串 | 通用性较好                          |
| key 使用业务主键       | 有助于 Kafka 分区稳定               |
| 控制字段数量           | 不要把无关字段写入消息              |
| 时间字段转字符串       | 避免下游解析 timestamp 差异         |
| 金额字段保持精度       | Decimal 转 JSON 前确认格式          |
| 消息结构版本化         | 重要链路建议增加 schemaVersion 字段 |

### Exactly Once 处理

Spark Structured Streaming 对 Kafka Source 读取可以做到基于 Checkpoint 的精确 Offset 跟踪，但端到端 Exactly Once 取决于 Source、计算逻辑、Sink 和下游系统的幂等能力。实际生产中，更多采用“可恢复读取 + 幂等写入 + 去重校验”的方式实现业务上的准 Exactly Once。

常见语义说明：

| 范围         | 说明                                       |
| ------------ | ------------------------------------------ |
| Kafka Source | 通过 Checkpoint 管理 Offset，支持故障恢复  |
| Spark 计算   | 同一批次失败可重试                         |
| File Sink    | 配合 Checkpoint 通常能较好避免重复提交     |
| Kafka Sink   | 失败重试下下游仍需处理重复                 |
| JDBC Sink    | 必须依赖主键、唯一键、批次号或 upsert 设计 |
| foreachBatch | 需要业务侧保证批次幂等                     |

使用 `foreachBatch` 实现幂等写入思路：

```java
StreamingQuery query = resultDf.writeStream()
        .foreachBatch((batchDf, batchId) -> {
            if (batchDf.isEmpty()) {
                log.warn("当前批次为空，batchId={}", batchId);
                return;
            }

            log.info("开始处理 Kafka 微批次，batchId={}", batchId);

            Dataset<Row> outputDf = batchDf
                    .dropDuplicates("event_id")
                    .withColumn("batch_id", functions.lit(batchId));

            outputDf.write()
                    .mode(SaveMode.Append)
                    .format("parquet")
                    .insertInto("dwd.dwd_user_event");

            log.info("Kafka 微批次处理完成，batchId={}", batchId);
        })
        .option("checkpointLocation", "hdfs:///checkpoint/prod/user_event_exactly_once")
        .start();
```

Exactly Once 处理建议如下：

| 建议                   | 说明                                       |
| ---------------------- | ------------------------------------------ |
| 保持 Checkpoint 稳定   | 任务重启必须复用原 Checkpoint              |
| 下游写入必须幂等       | 使用主键、唯一键、批次号或分区覆盖         |
| 保留 Kafka Offset      | 用于排查重复和丢失                         |
| 业务主键去重           | 例如 event_id、order_id、request_id        |
| 避免外部副作用不可回滚 | 不建议在 foreachBatch 中直接调用非幂等接口 |
| 写入审计表             | 记录 batchId、输入量、输出量、Offset 范围  |

严格端到端 Exactly Once 需要系统整体设计支持，不能只依赖 Spark 配置实现。

## JDBC 数据源集成

本章节用于说明 Spark 通过 JDBC 集成 MySQL、PostgreSQL、Oracle、SQL Server 等关系型数据库的方式。JDBC 常用于读取小维表、同步业务系统快照、写入报表结果和记录任务审计信息。对于超大规模数据同步，应优先评估专用同步工具、CDC 链路或数据湖落地方案，不建议直接用 Spark JDBC 长时间压业务库。

### MySQL 集成

MySQL 是 Spark JDBC 集成中最常见的数据源之一。集成前需要引入 MySQL JDBC 驱动，并准备只读或写入账号。

Maven 依赖：

```xml
<!-- MySQL JDBC 驱动：用于 Spark 读取和写入 MySQL -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>${mysql.version}</version>
</dependency>
```

读取 MySQL 示例：

```java
Properties properties = new Properties();
properties.setProperty("user", System.getenv("MYSQL_USERNAME"));
properties.setProperty("password", System.getenv("MYSQL_PASSWORD"));
properties.setProperty("driver", "com.mysql.cj.jdbc.Driver");

Dataset<Row> userDf = spark.read()
        .jdbc(
                "jdbc:mysql://mysql-host:3306/demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai",
                "user_info",
                properties
        );
```

写入 MySQL 示例：

```java
resultDf.coalesce(8)
        .write()
        .mode(SaveMode.Append)
        .format("jdbc")
        .option("url", "jdbc:mysql://mysql-host:3306/report?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai")
        .option("driver", "com.mysql.cj.jdbc.Driver")
        .option("dbtable", "ads_user_order_summary")
        .option("user", System.getenv("MYSQL_USERNAME"))
        .option("password", System.getenv("MYSQL_PASSWORD"))
        .option("batchsize", "5000")
        .save();
```

MySQL 集成建议：

| 建议                   | 说明                         |
| ---------------------- | ---------------------------- |
| 使用只读账号读取业务库 | 避免误操作生产业务表         |
| 大表读取必须分区       | 防止单连接读取过慢           |
| 写入前控制分区数       | 避免过多并发连接压垮 MySQL   |
| 使用环境变量传密码     | 不在代码和配置文件中明文保存 |
| 设置时区参数           | 避免时间字段偏移             |
| 写入报表库优先         | 不建议直接写业务核心库       |

### PostgreSQL 集成

PostgreSQL 集成方式与 MySQL 类似，需要引入 PostgreSQL JDBC 驱动。PostgreSQL 在分析型业务、报表库和数据服务中使用较多。

Maven 依赖：

```xml
<!-- PostgreSQL JDBC 驱动：用于 Spark 读取和写入 PostgreSQL -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>${postgresql.version}</version>
</dependency>
```

读取 PostgreSQL 示例：

```java
Dataset<Row> orderDf = spark.read()
        .format("jdbc")
        .option("url", "jdbc:postgresql://postgres-host:5432/report")
        .option("driver", "org.postgresql.Driver")
        .option("dbtable", "public.order_summary")
        .option("user", System.getenv("POSTGRES_USERNAME"))
        .option("password", System.getenv("POSTGRES_PASSWORD"))
        .load();
```

使用子查询读取：

```java
Dataset<Row> paidOrderDf = spark.read()
        .format("jdbc")
        .option("url", "jdbc:postgresql://postgres-host:5432/report")
        .option("driver", "org.postgresql.Driver")
        .option("dbtable", "(SELECT order_id, user_id, amount, pay_time FROM public.order_info WHERE status = 'PAID') t")
        .option("user", System.getenv("POSTGRES_USERNAME"))
        .option("password", System.getenv("POSTGRES_PASSWORD"))
        .load();
```

PostgreSQL 集成建议：

| 建议                 | 说明                                  |
| -------------------- | ------------------------------------- |
| 子查询必须加别名     | `dbtable` 中使用子查询时必须有别名    |
| 注意 schema 名称     | 例如 `public.table_name`              |
| 时间字段注意时区     | PostgreSQL 时间类型较多，需要统一处理 |
| 大表读取使用分区字段 | 使用数值或时间范围拆分                |
| 写入前确认字段大小写 | PostgreSQL 对大小写和引号较敏感       |

### Oracle 集成

Oracle 集成需要使用 Oracle JDBC 驱动。由于 Oracle 驱动可能涉及授权和公司 Maven 私服管理，实际项目中应以公司依赖仓库提供的版本为准。

Maven 依赖：

```xml
<!-- Oracle JDBC 驱动：版本以公司私服和数据库版本为准 -->
<dependency>
    <groupId>com.oracle.database.jdbc</groupId>
    <artifactId>ojdbc8</artifactId>
    <version>${oracle.version}</version>
</dependency>
```

读取 Oracle 示例：

```java
Dataset<Row> customerDf = spark.read()
        .format("jdbc")
        .option("url", "jdbc:oracle:thin:@//oracle-host:1521/ORCLPDB1")
        .option("driver", "oracle.jdbc.OracleDriver")
        .option("dbtable", "BI.CUSTOMER_INFO")
        .option("user", System.getenv("ORACLE_USERNAME"))
        .option("password", System.getenv("ORACLE_PASSWORD"))
        .load();
```

使用子查询读取 Oracle：

```java
Dataset<Row> customerDf = spark.read()
        .format("jdbc")
        .option("url", "jdbc:oracle:thin:@//oracle-host:1521/ORCLPDB1")
        .option("driver", "oracle.jdbc.OracleDriver")
        .option("dbtable", "(SELECT ID, NAME, STATUS, UPDATE_TIME FROM BI.CUSTOMER_INFO WHERE STATUS = 'ACTIVE') T")
        .option("user", System.getenv("ORACLE_USERNAME"))
        .option("password", System.getenv("ORACLE_PASSWORD"))
        .load();
```

Oracle 集成建议：

| 建议                 | 说明                             |
| -------------------- | -------------------------------- |
| 驱动版本与数据库匹配 | 避免协议或类型兼容问题           |
| 表名通常带 Schema    | 例如 `BI.CUSTOMER_INFO`          |
| 注意字段大小写       | Oracle 默认大写标识符            |
| 分区读取谨慎设计     | 优先使用数值主键或时间范围字段   |
| 大字段谨慎读取       | CLOB、BLOB 字段会影响性能        |
| 避免直接压生产库     | 大批量读取建议走只读库或同步链路 |

### SQL Server 集成

SQL Server 集成需要引入 Microsoft JDBC 驱动。常见于企业业务系统、报表库和历史系统数据同步场景。

Maven 依赖：

```xml
<!-- SQL Server JDBC 驱动：用于 Spark 读取和写入 SQL Server -->
<dependency>
    <groupId>com.microsoft.sqlserver</groupId>
    <artifactId>mssql-jdbc</artifactId>
    <version>${sqlserver.version}</version>
</dependency>
```

读取 SQL Server 示例：

```java
Dataset<Row> salesDf = spark.read()
        .format("jdbc")
        .option("url", "jdbc:sqlserver://sqlserver-host:1433;databaseName=report;encrypt=false")
        .option("driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver")
        .option("dbtable", "dbo.sales_order")
        .option("user", System.getenv("SQLSERVER_USERNAME"))
        .option("password", System.getenv("SQLSERVER_PASSWORD"))
        .load();
```

写入 SQL Server 示例：

```java
resultDf.coalesce(6)
        .write()
        .mode(SaveMode.Append)
        .format("jdbc")
        .option("url", "jdbc:sqlserver://sqlserver-host:1433;databaseName=report;encrypt=false")
        .option("driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver")
        .option("dbtable", "dbo.ads_order_summary")
        .option("user", System.getenv("SQLSERVER_USERNAME"))
        .option("password", System.getenv("SQLSERVER_PASSWORD"))
        .option("batchsize", "3000")
        .save();
```

SQL Server 集成建议：

| 建议                 | 说明                                 |
| -------------------- | ------------------------------------ |
| URL 参数明确加密策略 | 新版本驱动可能默认启用加密           |
| 表名建议带 schema    | 例如 `dbo.table_name`                |
| 写入前控制连接数     | 使用 `coalesce` 降低并发写入压力     |
| 注意日期时间类型     | `datetime`、`datetime2` 需要统一转换 |
| 大表读取使用分区     | 避免单连接全表扫描                   |

### 分区读取

JDBC 分区读取用于提升大表读取速度。Spark 会根据 `partitionColumn`、`lowerBound`、`upperBound` 和 `numPartitions` 将读取任务拆分为多个并行 JDBC 查询。

基础分区读取示例：

```java
Dataset<Row> userDf = spark.read()
        .format("jdbc")
        .option("url", "jdbc:mysql://mysql-host:3306/demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai")
        .option("driver", "com.mysql.cj.jdbc.Driver")
        .option("dbtable", "user_info")
        .option("user", System.getenv("MYSQL_USERNAME"))
        .option("password", System.getenv("MYSQL_PASSWORD"))
        .option("partitionColumn", "id")
        .option("lowerBound", "1")
        .option("upperBound", "1000000")
        .option("numPartitions", "8")
        .load();
```

基于时间范围的子查询读取：

```java
String query = "(SELECT id, user_name, status, update_time FROM user_info " +
        "WHERE update_time >= '2026-05-11 00:00:00' " +
        "AND update_time < '2026-05-12 00:00:00') t";

Dataset<Row> userDf = spark.read()
        .format("jdbc")
        .option("url", "jdbc:mysql://mysql-host:3306/demo")
        .option("driver", "com.mysql.cj.jdbc.Driver")
        .option("dbtable", query)
        .option("user", System.getenv("MYSQL_USERNAME"))
        .option("password", System.getenv("MYSQL_PASSWORD"))
        .option("partitionColumn", "id")
        .option("lowerBound", "1")
        .option("upperBound", "1000000")
        .option("numPartitions", "8")
        .load();
```

分区读取参数说明：

| 参数              | 说明                     |
| ----------------- | ------------------------ |
| `partitionColumn` | 用于拆分读取范围的字段   |
| `lowerBound`      | 分区字段下界             |
| `upperBound`      | 分区字段上界             |
| `numPartitions`   | 并行读取分区数           |
| `fetchsize`       | 每次从数据库拉取的记录数 |

分区读取建议：

| 建议                 | 说明                                 |
| -------------------- | ------------------------------------ |
| 分区字段选数值字段   | 自增 ID、序列号等更适合              |
| 分区字段分布要均匀   | 避免某个分区数据过多                 |
| 控制 `numPartitions` | 避免并发连接过多压垮数据库           |
| 配置 `fetchsize`     | 提高读取效率                         |
| 读取业务库需限流     | 优先读取从库、报表库或同步库         |
| 读取前估算数据量     | 根据数据量和数据库承载能力设置并行度 |

### 批量写入

批量写入用于提升 JDBC 写入效率。Spark 写 JDBC 时，每个分区通常对应一个数据库连接，分区内按 batch 写入。写入性能取决于分区数、batchsize、数据库索引、事务日志和目标表约束。

批量写入示例：

```java
resultDf.coalesce(8)
        .write()
        .mode(SaveMode.Append)
        .format("jdbc")
        .option("url", "jdbc:mysql://mysql-host:3306/report?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai")
        .option("driver", "com.mysql.cj.jdbc.Driver")
        .option("dbtable", "ads_user_order_summary")
        .option("user", System.getenv("MYSQL_USERNAME"))
        .option("password", System.getenv("MYSQL_PASSWORD"))
        .option("batchsize", "5000")
        .option("isolationLevel", "READ_COMMITTED")
        .save();
```

覆盖写入示例：

```java
resultDf.coalesce(4)
        .write()
        .mode(SaveMode.Overwrite)
        .format("jdbc")
        .option("url", "jdbc:postgresql://postgres-host:5432/report")
        .option("driver", "org.postgresql.Driver")
        .option("dbtable", "public.ads_user_order_summary")
        .option("user", System.getenv("POSTGRES_USERNAME"))
        .option("password", System.getenv("POSTGRES_PASSWORD"))
        .option("batchsize", "5000")
        .save();
```

批量写入建议如下：

| 建议                   | 说明                           |
| ---------------------- | ------------------------------ |
| 写入前控制分区数       | 分区数决定并发连接数量         |
| 合理设置 batchsize     | 常见为 1000、3000、5000、10000 |
| 大结果优先写数据湖     | JDBC 不适合写超大规模明细      |
| 避免目标表索引过多     | 索引过多会降低写入速度         |
| 写入前去重             | 避免主键冲突                   |
| 重要结果使用临时表中转 | 便于校验后再切换或合并         |

### 事务处理

Spark JDBC 写入不是传统单进程事务模型。由于 Spark 是分布式写入，每个分区可能使用独立连接和事务，不能简单认为整个 DataFrame 写入具备全局原子性。生产中需要通过临时表、批次号、幂等键和数据库侧合并逻辑保障一致性。

常见事务处理策略：

| 策略                  | 说明                                  |
| --------------------- | ------------------------------------- |
| 临时表中转            | 先写临时表，校验后再合并到正式表      |
| 批次号控制            | 每批数据带 `batch_id`，重复写入可识别 |
| 主键幂等              | 使用唯一键避免重复数据                |
| 分区删除再写          | 写入前删除指定业务日期数据            |
| 数据库 Merge / Upsert | 使用数据库原生语法合并                |
| 审计表记录            | 记录每批写入状态和数据量              |

临时表中转流程：

```text
Spark 写入临时表
        ↓
校验临时表数据量和主键
        ↓
数据库执行 delete + insert 或 merge
        ↓
记录批次审计状态
        ↓
清理临时表
```

Spark 写临时表示例：

```java
Dataset<Row> outputDf = resultDf
        .withColumn("batch_id", functions.lit("202605110001"));

outputDf.coalesce(4)
        .write()
        .mode(SaveMode.Append)
        .format("jdbc")
        .option("url", "jdbc:mysql://mysql-host:3306/report")
        .option("driver", "com.mysql.cj.jdbc.Driver")
        .option("dbtable", "tmp_ads_user_order_summary")
        .option("user", System.getenv("MYSQL_USERNAME"))
        .option("password", System.getenv("MYSQL_PASSWORD"))
        .option("batchsize", "5000")
        .save();
```

事务处理建议：

| 建议                  | 说明                                 |
| --------------------- | ------------------------------------ |
| 不依赖 Spark 全局事务 | 分布式写入无法保证整批全局原子性     |
| 重要结果先写临时表    | 校验通过后再合并正式表               |
| 每批数据带 batch_id   | 便于追踪、重跑和清理                 |
| 正式表设置唯一键      | 防止重复写入                         |
| 数据库侧执行合并      | 使用存储过程、SQL 脚本或调度任务完成 |
| 写入失败要可重跑      | 重跑同一批次不应造成重复结果         |

### 连接池配置

Spark JDBC 读取和写入通常不直接使用连接池。Spark 的 JDBC Reader 和 Writer 会在 Executor 端按分区创建连接，连接生命周期由 Spark 管理。传统 Web 应用中的 HikariCP、Druid 等连接池不适合直接套在 Spark DataFrame JDBC 读写上。

如果只是在 Driver 端执行少量元数据查询、审计表写入或任务状态更新，可以使用连接池。但大规模数据读写仍应使用 Spark JDBC 原生能力。

Driver 端使用 HikariCP 记录审计信息的示例。

文件位置：`src/main/java/io/github/atengk/spark/jdbc/JdbcAuditService.java`

该类用于在 Driver 端记录任务审计状态，不用于大规模数据写入。

```java
package io.github.atengk.spark.jdbc;

import cn.hutool.core.util.StrUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * JDBC 任务审计服务。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class JdbcAuditService implements AutoCloseable {

    private final HikariDataSource dataSource;

    /**
     * 创建 JDBC 审计服务。
     *
     * @param jdbcUrl JDBC 地址
     * @param username 用户名
     * @param password 密码
     */
    public JdbcAuditService(String jdbcUrl, String username, String password) {
        if (StrUtil.hasBlank(jdbcUrl, username, password)) {
            throw new IllegalArgumentException("JDBC 审计连接参数不能为空");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10000);
        config.setIdleTimeout(60000);
        config.setMaxLifetime(300000);

        this.dataSource = new HikariDataSource(config);
        log.info("JDBC 审计服务初始化完成");
    }

    /**
     * 记录任务状态。
     *
     * @param jobName 任务名称
     * @param batchId 批次号
     * @param status 任务状态
     */
    public void recordStatus(String jobName, String batchId, String status) {
        String sql = "INSERT INTO spark_job_audit(job_name, batch_id, status, create_time) VALUES (?, ?, ?, NOW())";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, jobName);
            statement.setString(2, batchId);
            statement.setString(3, status);
            statement.executeUpdate();
            log.info("任务审计状态写入完成，jobName={}，batchId={}，status={}", jobName, batchId, status);
        } catch (SQLException e) {
            log.error("任务审计状态写入失败，jobName={}，batchId={}，status={}", jobName, batchId, status, e);
            throw new IllegalStateException("任务审计状态写入失败", e);
        }
    }

    /**
     * 关闭连接池。
     */
    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
            log.info("JDBC 审计服务已关闭");
        }
    }
}
```

HikariCP 依赖：

```xml
<!-- HikariCP：仅用于 Driver 端少量审计或元数据操作，不用于 Spark 大规模分布式写入 -->
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>${hikari.version}</version>
</dependency>
```

连接池使用建议：

| 场景                             | 是否建议使用连接池 | 说明                          |
| -------------------------------- | ------------------ | ----------------------------- |
| Spark DataFrame 大规模 JDBC 读取 | 不建议             | 使用 Spark 原生 JDBC 分区读取 |
| Spark DataFrame 大规模 JDBC 写入 | 不建议             | 使用 Spark 原生 JDBC Writer   |
| Driver 端写审计表                | 可以               | 数据量小、连接稳定            |
| Driver 端读取任务配置            | 可以               | 小查询适合连接池              |
| Executor 中每条数据查库          | 禁止               | 会造成大量连接和严重性能问题  |

Spark 项目中最重要的不是给 JDBC 加连接池，而是控制 JDBC 读取并行度、写入分区数、batchsize、数据库负载和任务幂等性。


## HDFS 与对象存储集成

本章节用于说明 Spark 项目与 HDFS、S3、MinIO、OSS、COS 等文件存储系统的集成方式。Spark 本身通过 Hadoop FileSystem API 访问不同存储系统，因此只要配置对应的协议、依赖、认证信息和路径规范，就可以使用统一的 DataFrame API 读写数据。

### HDFS 文件操作

HDFS 是 Hadoop 生态中最常见的分布式文件系统，Spark 读取 Hive 表、Parquet、ORC、JSON、CSV 文件时，底层通常都依赖 HDFS。Spark 任务中既可以通过 `spark.read()` 和 `write()` 进行数据读写，也可以通过 Hadoop FileSystem API 进行文件存在性检查、目录删除、目录创建等操作。

常见 HDFS 路径格式如下：

```text
hdfs:///warehouse/dwd/user_event/dt=2026-05-11
hdfs://nameservice1/warehouse/dwd/user_event/dt=2026-05-11
/user/hive/warehouse/dwd.db/user_event/dt=2026-05-11
```

使用 Spark 读取 HDFS 上的 Parquet 文件：

```java
Dataset<Row> userEventDf = spark.read()
        .parquet("hdfs:///warehouse/dwd/user_event/dt=2026-05-11");
```

写出数据到 HDFS：

```java
resultDf.write()
        .mode(SaveMode.Overwrite)
        .parquet("hdfs:///warehouse/ads/user_summary/dt=2026-05-11");
```

如果需要在写出前检查目录或清理目录，可以封装 HDFS 工具类。

文件位置：`src/main/java/io/github/atengk/spark/util/HdfsFileUtil.java`

该工具类用于通过 Hadoop FileSystem API 操作 HDFS 路径。

```java
package io.github.atengk.spark.util;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.IOException;

/**
 * HDFS 文件操作工具类。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class HdfsFileUtil {

    private HdfsFileUtil() {
    }

    /**
     * 判断路径是否存在。
     *
     * @param configuration Hadoop 配置
     * @param path 文件路径
     * @return true 表示存在
     */
    public static boolean exists(Configuration configuration, String path) {
        checkPath(path);

        try {
            FileSystem fileSystem = FileSystem.get(configuration);
            return fileSystem.exists(new Path(path));
        } catch (IOException e) {
            log.error("检查 HDFS 路径是否存在失败，路径：{}", path, e);
            throw new IllegalStateException("检查 HDFS 路径失败", e);
        }
    }

    /**
     * 创建目录。
     *
     * @param configuration Hadoop 配置
     * @param path 目录路径
     */
    public static void mkdirs(Configuration configuration, String path) {
        checkPath(path);

        try {
            FileSystem fileSystem = FileSystem.get(configuration);
            Path hdfsPath = new Path(path);
            if (!fileSystem.exists(hdfsPath)) {
                fileSystem.mkdirs(hdfsPath);
                log.info("HDFS 目录创建完成，路径：{}", path);
            }
        } catch (IOException e) {
            log.error("创建 HDFS 目录失败，路径：{}", path, e);
            throw new IllegalStateException("创建 HDFS 目录失败", e);
        }
    }

    /**
     * 删除路径。
     *
     * @param configuration Hadoop 配置
     * @param path 文件或目录路径
     * @param recursive 是否递归删除
     */
    public static void delete(Configuration configuration, String path, boolean recursive) {
        checkPath(path);

        try {
            FileSystem fileSystem = FileSystem.get(configuration);
            Path hdfsPath = new Path(path);
            if (fileSystem.exists(hdfsPath)) {
                fileSystem.delete(hdfsPath, recursive);
                log.info("HDFS 路径删除完成，路径：{}，递归：{}", path, recursive);
            }
        } catch (IOException e) {
            log.error("删除 HDFS 路径失败，路径：{}", path, e);
            throw new IllegalStateException("删除 HDFS 路径失败", e);
        }
    }

    /**
     * 校验路径。
     *
     * @param path 文件路径
     */
    private static void checkPath(String path) {
        if (StrUtil.isBlank(path)) {
            throw new IllegalArgumentException("HDFS 路径不能为空");
        }
    }
}
```

使用示例：

```java
Configuration hadoopConf = spark.sparkContext().hadoopConfiguration();

String outputPath = "hdfs:///warehouse/ads/user_summary/dt=2026-05-11";

if (HdfsFileUtil.exists(hadoopConf, outputPath)) {
    HdfsFileUtil.delete(hadoopConf, outputPath, true);
}

resultDf.write()
        .mode(SaveMode.Overwrite)
        .parquet(outputPath);
```

HDFS 文件操作建议如下：

| 建议                        | 说明                                       |
| --------------------------- | ------------------------------------------ |
| 优先使用 Spark API 读写数据 | 常规数据读写不需要手动调用 FileSystem      |
| 文件管理操作集中封装        | 删除、创建、检查路径统一放入工具类         |
| 覆盖写入前确认路径          | 防止误删上层目录                           |
| 生产环境禁止拼错根路径      | 删除前必须校验路径前缀                     |
| 路径中包含业务日期          | 便于重跑、补数和排查                       |
| 不直接操作 Hive 表目录      | Hive 表数据优先通过 Hive SQL 或表 API 写入 |

### S3 集成

S3 是 AWS 提供的对象存储，Spark 通常通过 Hadoop S3A Connector 访问。路径协议一般使用 `s3a://`。使用 S3 前需要配置 `hadoop-aws` 依赖、AWS SDK、访问密钥、Endpoint、凭证提供器和文件系统实现类。

Maven 依赖示例：

```xml
<!-- Hadoop AWS：支持 Spark 通过 s3a:// 协议访问 S3 或兼容 S3 的对象存储 -->
<dependency>
    <groupId>org.apache.hadoop</groupId>
    <artifactId>hadoop-aws</artifactId>
    <version>${hadoop.version}</version>
</dependency>

<!-- AWS SDK Bundle：Hadoop AWS 依赖的 AWS SDK 包，版本需要与 Hadoop 版本兼容 -->
<dependency>
    <groupId>com.amazonaws</groupId>
    <artifactId>aws-java-sdk-bundle</artifactId>
    <version>${aws.sdk.version}</version>
</dependency>
```

Spark 提交时配置 S3：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --conf spark.hadoop.fs.s3a.impl=org.apache.hadoop.fs.s3a.S3AFileSystem \
  --conf spark.hadoop.fs.s3a.access.key="${AWS_ACCESS_KEY_ID}" \
  --conf spark.hadoop.fs.s3a.secret.key="${AWS_SECRET_ACCESS_KEY}" \
  --conf spark.hadoop.fs.s3a.endpoint=s3.amazonaws.com \
  --conf spark.hadoop.fs.s3a.path.style.access=false \
  target/spark-job.jar \
  --env prod \
  --bizDate 2026-05-11
```

读取 S3 数据：

```java
Dataset<Row> sourceDf = spark.read()
        .parquet("s3a://data-bucket/warehouse/dwd/user_event/dt=2026-05-11");
```

写入 S3 数据：

```java
resultDf.write()
        .mode(SaveMode.Overwrite)
        .parquet("s3a://data-bucket/warehouse/ads/user_summary/dt=2026-05-11");
```

S3 集成建议如下：

| 建议                   | 说明                                         |
| ---------------------- | -------------------------------------------- |
| 使用 `s3a://` 协议     | 不建议使用旧的 `s3n://`                      |
| 依赖版本与 Hadoop 匹配 | `hadoop-aws` 与 Hadoop 版本不一致容易报错    |
| 密钥不要写入代码       | 使用环境变量、IAM Role 或密钥管理系统        |
| 控制提交协议           | 大规模写入对象存储时需要关注 commit protocol |
| 避免频繁 rename        | 对象存储 rename 成本高，与 HDFS 语义不同     |
| 小文件问题更明显       | 对象存储上小文件会影响 list 和读取性能       |

### MinIO 集成

MinIO 是兼容 S3 协议的对象存储，Spark 通常也通过 `s3a://` 协议访问。与 AWS S3 不同，MinIO 通常需要配置自定义 Endpoint，并启用 path-style access。

MinIO 配置示例：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --conf spark.hadoop.fs.s3a.impl=org.apache.hadoop.fs.s3a.S3AFileSystem \
  --conf spark.hadoop.fs.s3a.endpoint=http://minio-host:9000 \
  --conf spark.hadoop.fs.s3a.access.key="${MINIO_ACCESS_KEY}" \
  --conf spark.hadoop.fs.s3a.secret.key="${MINIO_SECRET_KEY}" \
  --conf spark.hadoop.fs.s3a.path.style.access=true \
  --conf spark.hadoop.fs.s3a.connection.ssl.enabled=false \
  target/spark-job.jar \
  --env prod \
  --bizDate 2026-05-11
```

读取 MinIO 数据：

```java
Dataset<Row> sourceDf = spark.read()
        .json("s3a://data-lake/ods/user_event/dt=2026-05-11");
```

写入 MinIO 数据：

```java
resultDf.write()
        .mode(SaveMode.Append)
        .partitionBy("dt")
        .parquet("s3a://data-lake/dwd/user_event");
```

MinIO 集成注意事项：

| 注意点                  | 说明                                      |
| ----------------------- | ----------------------------------------- |
| 通常必须开启 path-style | `fs.s3a.path.style.access=true`           |
| Endpoint 使用内网地址   | 生产环境优先使用集群可访问的内网地址      |
| SSL 按实际环境配置      | HTTP 环境需要关闭 SSL                     |
| Bucket 需要提前创建     | Spark 不负责自动创建 Bucket               |
| 权限最小化              | 只给任务需要的 Bucket 和路径权限          |
| 关注对象存储一致性      | 写后读、列表延迟需结合 MinIO 部署模式验证 |

### OSS 集成

OSS 通常指阿里云对象存储。Spark 集成 OSS 可以通过 Hadoop OSS Connector 或兼容 S3 的方式接入，具体取决于集群发行版和公司基础设施配置。路径协议常见为 `oss://` 或通过 `s3a://` 兼容方式访问。

使用 OSS Connector 时，常见配置如下：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --conf spark.hadoop.fs.oss.endpoint=oss-cn-hangzhou.aliyuncs.com \
  --conf spark.hadoop.fs.oss.accessKeyId="${OSS_ACCESS_KEY_ID}" \
  --conf spark.hadoop.fs.oss.accessKeySecret="${OSS_ACCESS_KEY_SECRET}" \
  target/spark-job.jar \
  --env prod \
  --bizDate 2026-05-11
```

读取 OSS 数据：

```java
Dataset<Row> sourceDf = spark.read()
        .parquet("oss://data-bucket/warehouse/dwd/user_event/dt=2026-05-11");
```

写入 OSS 数据：

```java
resultDf.write()
        .mode(SaveMode.Overwrite)
        .parquet("oss://data-bucket/warehouse/ads/user_summary/dt=2026-05-11");
```

OSS 集成建议如下：

| 建议                         | 说明                                       |
| ---------------------------- | ------------------------------------------ |
| 以集群内置 Connector 为准    | 不同 Hadoop 发行版的 OSS 支持方式不同      |
| Endpoint 必须匹配地域        | 地域错误会导致访问失败或跨地域流量         |
| 密钥外置                     | 使用环境变量、RAM Role 或密钥管理系统      |
| 控制写出文件数量             | 对象存储小文件会明显影响查询效率           |
| 注意提交协议                 | 大规模写入时需验证任务失败后的临时文件清理 |
| 权限按 Bucket 和 Prefix 授权 | 不建议使用过大权限范围                     |

### COS 集成

COS 通常指腾讯云对象存储。Spark 集成 COS 可以通过 Hadoop COS Connector 或 S3 兼容协议实现。具体依赖和配置应以集群提供的 Connector 为准。

COS 配置示例：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --conf spark.hadoop.fs.cosn.impl=org.apache.hadoop.fs.CosFileSystem \
  --conf spark.hadoop.fs.cosn.userinfo.secretId="${COS_SECRET_ID}" \
  --conf spark.hadoop.fs.cosn.userinfo.secretKey="${COS_SECRET_KEY}" \
  --conf spark.hadoop.fs.cosn.bucket.region=ap-guangzhou \
  target/spark-job.jar \
  --env prod \
  --bizDate 2026-05-11
```

读取 COS 数据：

```java
Dataset<Row> sourceDf = spark.read()
        .orc("cosn://data-bucket/warehouse/dwd/order_detail/dt=2026-05-11");
```

写入 COS 数据：

```java
resultDf.write()
        .mode(SaveMode.Append)
        .partitionBy("dt")
        .orc("cosn://data-bucket/warehouse/dwd/order_detail");
```

COS 集成建议如下：

| 建议                 | 说明                                                    |
| -------------------- | ------------------------------------------------------- |
| 使用集群支持的协议   | 常见为 `cosn://` 或 S3 兼容方式                         |
| 地域配置必须正确     | Bucket 所在地域要与配置一致                             |
| 密钥通过安全渠道注入 | 不写入 Git 和普通配置文件                               |
| 写入后校验文件数量   | 防止任务失败留下异常文件                                |
| 控制并发和小文件     | 对象存储访问吞吐与请求数有关                            |
| 与 Hive 表集成需验证 | 外部表 LOCATION 指向 COS 时需确认 Hive/Spark 都支持协议 |

### 文件路径规范

文件路径规范用于保证数据目录清晰、环境隔离、层级明确、分区稳定，并支持重跑、补数和排查。路径命名应尽量包含环境、数据层级、主题域、表名和分区字段。

推荐 HDFS 路径规范：

```text
/warehouse/{env}/{layer}/{domain}/{table_name}/dt={yyyy-MM-dd}
```

示例：

```text
/warehouse/prod/ods/user/ods_user_event/dt=2026-05-11
/warehouse/prod/dwd/user/dwd_user_event/dt=2026-05-11
/warehouse/prod/dws/order/dws_order_wide/dt=2026-05-11
/warehouse/prod/ads/order/ads_order_daily_summary/dt=2026-05-11
```

推荐对象存储路径规范：

```text
s3a://{bucket}/warehouse/{env}/{layer}/{domain}/{table_name}/dt={yyyy-MM-dd}
oss://{bucket}/warehouse/{env}/{layer}/{domain}/{table_name}/dt={yyyy-MM-dd}
cosn://{bucket}/warehouse/{env}/{layer}/{domain}/{table_name}/dt={yyyy-MM-dd}
```

临时路径规范：

```text
/tmp/spark/{env}/{app_name}/{biz_date}/{batch_id}
/checkpoint/{env}/{app_name}/{job_name}
/error/{env}/{app_name}/{table_name}/dt={yyyy-MM-dd}
```

路径规范建议如下：

| 建议                | 说明                                         |
| ------------------- | -------------------------------------------- |
| 环境必须隔离        | local、test、prod 使用不同根路径             |
| 路径体现数据层级    | ODS、DWD、DWS、ADS、DIM 清晰区分             |
| 分区字段格式统一    | 推荐 `dt=yyyy-MM-dd`，小时分区使用 `hour=HH` |
| 临时目录单独规划    | 不与正式数据目录混用                         |
| Checkpoint 独立规划 | 流任务 Checkpoint 不要放在数据目录下         |
| 不使用中文路径      | 避免脚本、权限和跨系统兼容问题               |
| 路径不要包含空格    | 避免 Shell 和 Hadoop 命令解析问题            |

### 权限配置

权限配置用于控制 Spark 任务对 HDFS 和对象存储路径的访问范围。生产任务应使用专用账号运行，避免使用个人账号长期执行调度任务。

HDFS 权限检查命令：

```bash
# 查看目录权限
hdfs dfs -ls /warehouse/prod/dwd/user

# 查看当前用户身份
whoami

# 测试写权限
hdfs dfs -touchz /warehouse/prod/dwd/user/_permission_test

# 删除测试文件
hdfs dfs -rm /warehouse/prod/dwd/user/_permission_test
```

HDFS 授权示例：

```bash
# 修改目录属主
hdfs dfs -chown -R spark:spark /warehouse/prod/dwd/user

# 修改目录权限
hdfs dfs -chmod -R 750 /warehouse/prod/dwd/user

# 给指定用户授权 ACL
hdfs dfs -setfacl -m user:spark_user:rwx /warehouse/prod/dwd/user
```

对象存储权限建议通过 Bucket Policy、IAM、RAM、CAM 等方式控制。推荐按 Bucket 和 Prefix 授权，例如只允许任务访问：

```text
s3a://data-bucket/warehouse/prod/dwd/user/*
oss://data-bucket/warehouse/prod/dwd/user/*
cosn://data-bucket/warehouse/prod/dwd/user/*
```

权限配置建议如下：

| 建议             | 说明                                  |
| ---------------- | ------------------------------------- |
| 使用任务专用账号 | 生产调度不使用个人账号                |
| 最小权限原则     | 只授权任务需要的路径                  |
| 测试生产权限隔离 | 防止测试任务误写生产路径              |
| 密钥不写入代码   | 使用环境变量、Keytab、Role 或密钥系统 |
| 写入前校验权限   | 上线前检查目标路径读写权限            |
| 删除权限谨慎授予 | 删除权限应控制在必要范围内            |
| 权限变更要审计   | 记录授权对象、路径、时间和原因        |

### 小文件处理

小文件是 Spark 与 HDFS、Hive、对象存储集成中常见问题。小文件过多会导致 NameNode 元数据压力、对象存储 list 请求变慢、Spark 任务调度开销增加、查询性能下降。

小文件产生原因包括：

| 原因                 | 说明                         |
| -------------------- | ---------------------------- |
| 分区数过多           | 每个分区通常至少输出一个文件 |
| 分区字段过细         | 例如按用户 ID 分区           |
| 流任务频繁小批次写入 | 每个微批次写出少量文件       |
| 追加写入频繁         | 多次 append 产生多个小文件   |
| 上游文件本身过小     | 读取阶段已经存在大量小文件   |

写出前减少分区：

```java
resultDf.coalesce(20)
        .write()
        .mode(SaveMode.Overwrite)
        .parquet("hdfs:///warehouse/ads/user_summary/dt=2026-05-11");
```

按分区字段重分区后写出：

```java
resultDf.repartition(100, functions.col("dt"))
        .write()
        .mode(SaveMode.Append)
        .partitionBy("dt")
        .parquet("hdfs:///warehouse/dwd/user_event");
```

通过 Spark 参数控制文件大小：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --conf spark.sql.files.maxRecordsPerFile=1000000 \
  --conf spark.sql.shuffle.partitions=400 \
  target/spark-job.jar \
  --env prod \
  --bizDate 2026-05-11
```

小文件处理建议如下：

| 建议                             | 说明                             |
| -------------------------------- | -------------------------------- |
| 写出前控制分区数                 | 使用 `coalesce` 或 `repartition` |
| 合理设置 `maxRecordsPerFile`     | 控制单文件记录数                 |
| 避免过细分区                     | 不使用高基数字段作为分区         |
| 定期合并历史小文件               | 对长期追加表执行 compaction      |
| 流任务使用 foreachBatch 合并写出 | 控制每批写出文件数               |
| 查询前优化输入文件               | 小文件过多会拖慢读取和调度       |

小文件合并示例：

```java
Dataset<Row> sourceDf = spark.read()
        .parquet("hdfs:///warehouse/dwd/user_event/dt=2026-05-11");

sourceDf.coalesce(20)
        .write()
        .mode(SaveMode.Overwrite)
        .parquet("hdfs:///warehouse/dwd/user_event_compact/dt=2026-05-11");
```

## 项目代码设计

本章节用于说明 Spark Java 项目的代码组织方式。良好的代码设计应将任务入口、Job 抽象、数据读取、数据转换、数据写出、业务服务、配置、工具类和异常处理分层管理，避免所有逻辑堆叠在一个 main 方法中。代码设计的目标是让任务可复用、可测试、可扩展、可排查。

### 主程序入口

主程序入口负责解析参数、初始化 SparkSession、创建 Job、执行任务、记录日志和释放资源。入口类不应承载具体业务计算逻辑，只负责调度。

文件位置：`src/main/java/io/github/atengk/spark/Application.java`

该入口类用于统一启动 Spark 任务。

```java
package io.github.atengk.spark;

import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.spark.config.JobRuntimeArgs;
import io.github.atengk.spark.config.JobArgsParser;
import io.github.atengk.spark.config.SparkRuntimeConfig;
import io.github.atengk.spark.config.SparkSessionFactory;
import io.github.atengk.spark.exception.SparkJobException;
import io.github.atengk.spark.job.SparkJob;
import io.github.atengk.spark.job.UserSummaryJob;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.SparkSession;

/**
 * Spark 应用程序入口。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class Application {

    /**
     * Spark 任务主入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        TimeInterval timer = new TimeInterval();
        SparkSession spark = null;

        try {
            JobRuntimeArgs runtimeArgs = JobArgsParser.parse(args);
            log.info("Spark 任务启动，任务名称：{}，运行环境：{}，业务日期：{}",
                    runtimeArgs.getJobName(), runtimeArgs.getEnv(), runtimeArgs.getBizDate());

            SparkRuntimeConfig sparkConfig = buildSparkConfig(runtimeArgs);
            spark = SparkSessionFactory.create(sparkConfig);

            SparkJob job = createJob(runtimeArgs.getJobName());
            job.execute(spark, runtimeArgs);

            log.info("Spark 任务执行成功，任务名称：{}，耗时：{} ms",
                    runtimeArgs.getJobName(), timer.interval());
        } catch (Exception e) {
            log.error("Spark 任务执行失败", e);
            throw new SparkJobException("Spark 任务执行失败", e);
        } finally {
            if (spark != null) {
                spark.stop();
                log.info("SparkSession 已释放");
            }
        }
    }

    /**
     * 构建 Spark 运行配置。
     *
     * @param runtimeArgs 任务运行参数
     * @return Spark 运行配置
     */
    private static SparkRuntimeConfig buildSparkConfig(JobRuntimeArgs runtimeArgs) {
        SparkRuntimeConfig config = new SparkRuntimeConfig();
        config.setAppName(StrUtil.format("{}-{}", runtimeArgs.getJobName(), runtimeArgs.getBizDate()));
        config.setMaster("local".equals(runtimeArgs.getEnv()) ? "local[*]" : null);
        config.setHiveEnabled(!"local".equals(runtimeArgs.getEnv()));
        config.getSparkConf().put("spark.sql.adaptive.enabled", "true");
        config.getSparkConf().put("spark.sql.shuffle.partitions", "local".equals(runtimeArgs.getEnv()) ? "4" : "400");
        return config;
    }

    /**
     * 根据任务名称创建 Job。
     *
     * @param jobName 任务名称
     * @return Spark Job
     */
    private static SparkJob createJob(String jobName) {
        if ("user-summary".equals(jobName)) {
            return new UserSummaryJob();
        }
        throw new IllegalArgumentException(StrUtil.format("不支持的任务名称：{}", jobName));
    }
}
```

主程序入口设计建议如下：

| 建议                     | 说明                         |
| ------------------------ | ---------------------------- |
| 入口只负责调度           | 不写具体业务 SQL 和转换逻辑  |
| 参数启动即校验           | 缺少关键参数直接失败         |
| SparkSession 统一创建    | 避免多个模块重复初始化       |
| try-finally 释放资源     | 异常场景也要 `spark.stop()`  |
| 记录任务耗时             | 便于审计和性能分析           |
| Job 创建可替换为工厂模式 | 任务多时使用 JobFactory 管理 |

### Job 抽象设计

Job 抽象用于定义所有 Spark 任务的统一执行协议。不同任务只需要实现 `execute` 方法，内部完成读取、转换、写出和校验逻辑。

文件位置：`src/main/java/io/github/atengk/spark/job/SparkJob.java`

该接口定义 Spark 任务的统一入口。

```java
package io.github.atengk.spark.job;

import io.github.atengk.spark.config.JobRuntimeArgs;
import org.apache.spark.sql.SparkSession;

/**
 * Spark 任务接口。
 *
 * @author Ateng
 * @since 2026-05-11
 */
public interface SparkJob {

    /**
     * 执行 Spark 任务。
     *
     * @param spark SparkSession
     * @param args 任务运行参数
     */
    void execute(SparkSession spark, JobRuntimeArgs args);
}
```

文件位置：`src/main/java/io/github/atengk/spark/job/AbstractSparkJob.java`

该抽象类提供通用执行模板，子类只需要实现具体处理逻辑。

```java
package io.github.atengk.spark.job;

import cn.hutool.core.date.TimeInterval;
import io.github.atengk.spark.config.JobRuntimeArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.SparkSession;

/**
 * Spark 任务抽象模板。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public abstract class AbstractSparkJob implements SparkJob {

    /**
     * 执行任务模板。
     *
     * @param spark SparkSession
     * @param args 任务运行参数
     */
    @Override
    public final void execute(SparkSession spark, JobRuntimeArgs args) {
        TimeInterval timer = new TimeInterval();
        String jobName = getJobName();

        log.info("开始执行 Spark Job，jobName={}，bizDate={}", jobName, args.getBizDate());

        validate(args);
        doExecute(spark, args);

        log.info("Spark Job 执行完成，jobName={}，耗时={} ms", jobName, timer.interval());
    }

    /**
     * 获取任务名称。
     *
     * @return 任务名称
     */
    protected abstract String getJobName();

    /**
     * 执行具体任务逻辑。
     *
     * @param spark SparkSession
     * @param args 任务运行参数
     */
    protected abstract void doExecute(SparkSession spark, JobRuntimeArgs args);

    /**
     * 校验任务参数。
     *
     * @param args 任务运行参数
     */
    protected void validate(JobRuntimeArgs args) {
        if (args == null) {
            throw new IllegalArgumentException("任务运行参数不能为空");
        }
    }
}
```

Job 抽象设计建议如下：

| 建议                 | 说明                             |
| -------------------- | -------------------------------- |
| 使用接口定义统一协议 | 所有任务都实现同一执行方法       |
| 抽象类封装通用流程   | 参数校验、日志、耗时统计统一处理 |
| 子类只写业务逻辑     | 降低重复代码                     |
| Job 名称全局唯一     | 便于调度、日志和审计             |
| 复杂任务拆分 Service | Job 不要承担所有细节             |

### Reader 组件设计

Reader 组件负责数据读取，包括 Hive、HDFS、Parquet、ORC、CSV、JSON、JDBC、Kafka 等数据源。Reader 的目标是屏蔽读取细节，让 Job 只关心业务数据集。

文件位置：`src/main/java/io/github/atengk/spark/reader/DataReader.java`

该接口定义统一数据读取方法。

```java
package io.github.atengk.spark.reader;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.util.Map;

/**
 * Spark 数据读取接口。
 *
 * @author Ateng
 * @since 2026-05-11
 */
public interface DataReader {

    /**
     * 读取数据。
     *
     * @param spark SparkSession
     * @param options 读取参数
     * @return Dataset<Row>
     */
    Dataset<Row> read(SparkSession spark, Map<String, String> options);
}
```

文件位置：`src/main/java/io/github/atengk/spark/reader/HiveTableReader.java`

该 Reader 用于读取 Hive 表。

```java
package io.github.atengk.spark.reader;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.util.Map;

import static org.apache.spark.sql.functions.col;

/**
 * Hive 表数据读取器。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class HiveTableReader implements DataReader {

    /**
     * 读取 Hive 表数据。
     *
     * @param spark SparkSession
     * @param options 读取参数
     * @return Dataset<Row>
     */
    @Override
    public Dataset<Row> read(SparkSession spark, Map<String, String> options) {
        String tableName = MapUtil.getStr(options, "tableName");
        String bizDate = MapUtil.getStr(options, "bizDate");

        if (StrUtil.hasBlank(tableName, bizDate)) {
            throw new IllegalArgumentException("Hive 表名和业务日期不能为空");
        }

        log.info("开始读取 Hive 表，tableName={}，bizDate={}", tableName, bizDate);

        Dataset<Row> dataset = spark.table(tableName)
                .filter(col("dt").equalTo(bizDate));

        log.info("Hive 表读取逻辑构建完成，tableName={}", tableName);
        return dataset;
    }
}
```

Reader 组件设计建议如下：

| 建议                     | 说明                                |
| ------------------------ | ----------------------------------- |
| 每类数据源一个 Reader    | Hive、JDBC、Kafka、Parquet 分开实现 |
| 参数统一使用 options     | 避免方法参数过多                    |
| 读取前校验必填参数       | 路径、表名、Topic、URL 等必须校验   |
| 日志记录数据来源         | 记录表名、路径、Topic，不打印密码   |
| 不在 Reader 中写业务逻辑 | Reader 只负责读取和基础字段选择     |
| 复杂数据源可增加配置对象 | JDBC、Kafka 可使用专门配置类        |

### Transformer 组件设计

Transformer 组件负责数据清洗和转换，包括字段裁剪、字段重命名、类型转换、空值处理、维度关联、指标聚合等。Transformer 应尽量保持纯函数风格，输入 DataFrame，输出 DataFrame，不直接写出数据。

文件位置：`src/main/java/io/github/atengk/spark/transformer/DataTransformer.java`

该接口定义统一数据转换方法。

```java
package io.github.atengk.spark.transformer;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

/**
 * Spark 数据转换接口。
 *
 * @author Ateng
 * @since 2026-05-11
 */
public interface DataTransformer {

    /**
     * 转换数据。
     *
     * @param sourceDf 原始数据
     * @return 转换后的数据
     */
    Dataset<Row> transform(Dataset<Row> sourceDf);
}
```

文件位置：`src/main/java/io/github/atengk/spark/transformer/UserSummaryTransformer.java`

该 Transformer 用于用户汇总数据转换。

```java
package io.github.atengk.spark.transformer;

import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.current_timestamp;
import static org.apache.spark.sql.functions.lit;
import static org.apache.spark.sql.functions.when;

/**
 * 用户汇总数据转换器。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserSummaryTransformer implements DataTransformer {

    /**
     * 转换用户汇总数据。
     *
     * @param sourceDf 原始数据
     * @return 转换后的数据
     */
    @Override
    public Dataset<Row> transform(Dataset<Row> sourceDf) {
        log.info("开始转换用户汇总数据");

        Dataset<Row> resultDf = sourceDf
                .filter(col("user_id").isNotNull())
                .withColumn("user_level", when(col("user_level").isNull(), lit("UNKNOWN")).otherwise(col("user_level")))
                .withColumn("etl_time", current_timestamp())
                .select(
                        col("user_id"),
                        col("user_name"),
                        col("user_level"),
                        col("order_count"),
                        col("order_amount"),
                        col("etl_time"),
                        col("dt")
                );

        log.info("用户汇总数据转换完成");
        return resultDf;
    }
}
```

Transformer 组件设计建议如下：

| 建议                         | 说明                       |
| ---------------------------- | -------------------------- |
| 输入输出都是 Dataset         | 方便链式组合               |
| 不直接读取和写出             | 保持职责单一               |
| 复杂转换拆分多个 Transformer | 清洗、关联、聚合分别处理   |
| 字段选择显式声明             | 避免 `select("*")`         |
| 转换前后记录日志             | 关键步骤记录业务含义       |
| 可配合单元测试               | 使用小样例数据验证转换结果 |

### Writer 组件设计

Writer 组件负责将结果写出到 Hive、HDFS、JDBC、Kafka 等目标端。Writer 应统一处理写入模式、路径、表名、分区字段、输出数据量和异常日志。

文件位置：`src/main/java/io/github/atengk/spark/writer/DataWriter.java`

该接口定义统一数据写出方法。

```java
package io.github.atengk.spark.writer;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.util.Map;

/**
 * Spark 数据写出接口。
 *
 * @author Ateng
 * @since 2026-05-11
 */
public interface DataWriter {

    /**
     * 写出数据。
     *
     * @param dataset 数据集
     * @param options 写出参数
     */
    void write(Dataset<Row> dataset, Map<String, String> options);
}
```

文件位置：`src/main/java/io/github/atengk/spark/writer/HiveTableWriter.java`

该 Writer 用于写入 Hive 表。

```java
package io.github.atengk.spark.writer;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;

import java.util.Map;

/**
 * Hive 表数据写出器。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class HiveTableWriter implements DataWriter {

    /**
     * 写出数据到 Hive 表。
     *
     * @param dataset 数据集
     * @param options 写出参数
     */
    @Override
    public void write(Dataset<Row> dataset, Map<String, String> options) {
        String tableName = MapUtil.getStr(options, "tableName");
        String mode = StrUtil.blankToDefault(MapUtil.getStr(options, "mode"), "append");

        if (StrUtil.isBlank(tableName)) {
            throw new IllegalArgumentException("Hive 写出表名不能为空");
        }

        SaveMode saveMode = "overwrite".equalsIgnoreCase(mode) ? SaveMode.Overwrite : SaveMode.Append;

        long count = dataset.count();
        log.info("开始写入 Hive 表，tableName={}，mode={}，数据量={}", tableName, mode, count);

        if (count == 0) {
            log.warn("写入数据为空，跳过 Hive 写入，tableName={}", tableName);
            return;
        }

        dataset.write()
                .mode(saveMode)
                .insertInto(tableName);

        log.info("Hive 表写入完成，tableName={}，数据量={}", tableName, count);
    }
}
```

Writer 组件设计建议如下：

| 建议                 | 说明                                         |
| -------------------- | -------------------------------------------- |
| 每类目标一个 Writer  | Hive、Parquet、JDBC、Kafka 分开实现          |
| 写出前校验数据量     | 防止空数据覆盖正式分区                       |
| 写出参数集中管理     | 表名、路径、模式、分区字段不散落在业务代码中 |
| 日志记录目标端       | 表名、路径、写入模式、数据量必须记录         |
| 覆盖写入要谨慎       | 需要明确覆盖范围                             |
| 写 JDBC 前控制分区数 | 防止数据库连接过多                           |

### Service 层设计

Service 层用于编排一个业务任务中的读取、转换、校验和写出逻辑。Job 负责调用 Service，Service 负责组织业务流程，Reader、Transformer、Writer 负责具体技术细节。

文件位置：`src/main/java/io/github/atengk/spark/service/UserSummaryService.java`

该 Service 用于编排用户汇总任务。

```java
package io.github.atengk.spark.service;

import cn.hutool.core.map.MapUtil;
import io.github.atengk.spark.config.JobRuntimeArgs;
import io.github.atengk.spark.quality.DataQualityChecker;
import io.github.atengk.spark.reader.HiveTableReader;
import io.github.atengk.spark.transformer.UserSummaryTransformer;
import io.github.atengk.spark.writer.HiveTableWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.util.Arrays;
import java.util.Map;

/**
 * 用户汇总业务服务。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserSummaryService {

    private final HiveTableReader reader = new HiveTableReader();
    private final UserSummaryTransformer transformer = new UserSummaryTransformer();
    private final HiveTableWriter writer = new HiveTableWriter();

    /**
     * 执行用户汇总处理。
     *
     * @param spark SparkSession
     * @param args 任务运行参数
     */
    public void process(SparkSession spark, JobRuntimeArgs args) {
        log.info("开始执行用户汇总业务处理，bizDate={}", args.getBizDate());

        Map<String, String> readOptions = MapUtil.builder("tableName", "dws.dws_user_order")
                .put("bizDate", args.getBizDate())
                .build();

        Dataset<Row> sourceDf = reader.read(spark, readOptions);
        Dataset<Row> resultDf = transformer.transform(sourceDf);

        DataQualityChecker.checkNotEmpty(resultDf, "用户汇总结果");
        DataQualityChecker.checkRequiredColumns(resultDf, "用户汇总结果", Arrays.asList("user_id", "dt"));

        Map<String, String> writeOptions = MapUtil.builder("tableName", "ads.ads_user_summary")
                .put("mode", "append")
                .build();

        writer.write(resultDf, writeOptions);

        log.info("用户汇总业务处理完成，bizDate={}", args.getBizDate());
    }
}
```

文件位置：`src/main/java/io/github/atengk/spark/job/UserSummaryJob.java`

Job 只负责调用 Service，不直接堆叠业务细节。

```java
package io.github.atengk.spark.job;

import io.github.atengk.spark.config.JobRuntimeArgs;
import io.github.atengk.spark.service.UserSummaryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.SparkSession;

/**
 * 用户汇总 Spark Job。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserSummaryJob extends AbstractSparkJob {

    private final UserSummaryService userSummaryService = new UserSummaryService();

    /**
     * 获取任务名称。
     *
     * @return 任务名称
     */
    @Override
    protected String getJobName() {
        return "user-summary";
    }

    /**
     * 执行用户汇总任务。
     *
     * @param spark SparkSession
     * @param args 任务运行参数
     */
    @Override
    protected void doExecute(SparkSession spark, JobRuntimeArgs args) {
        userSummaryService.process(spark, args);
    }
}
```

Service 层设计建议如下：

| 建议                   | 说明                                      |
| ---------------------- | ----------------------------------------- |
| Service 负责编排流程   | 读取、转换、校验、写出按顺序组织          |
| Job 保持轻量           | 不在 Job 中写复杂业务逻辑                 |
| 复杂业务拆多个 Service | 按主题域或处理阶段拆分                    |
| Service 可以复用组件   | Reader、Transformer、Writer 可组合        |
| 日志记录业务阶段       | 便于定位是读取、转换还是写出失败          |
| 核心流程可测试         | 使用 local SparkSession 验证 Service 逻辑 |

### Config 层设计

Config 层用于封装任务参数、Spark 配置、数据源配置和运行环境配置。配置对象应集中管理，避免在业务代码中散落读取命令行参数、环境变量和配置文件。

文件位置：`src/main/java/io/github/atengk/spark/config/JobRuntimeArgs.java`

该配置类用于保存任务启动参数。

```java
package io.github.atengk.spark.config;

import lombok.Data;

import java.io.Serializable;

/**
 * Spark 任务运行参数。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
public class JobRuntimeArgs implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务名称。
     */
    private String jobName;

    /**
     * 运行环境。
     */
    private String env;

    /**
     * 业务日期。
     */
    private String bizDate;

    /**
     * 批次号。
     */
    private String batchId;

    /**
     * 输入路径。
     */
    private String inputPath;

    /**
     * 输出路径。
     */
    private String outputPath;

    /**
     * 是否覆盖写入。
     */
    private boolean overwrite;
}
```

文件位置：`src/main/java/io/github/atengk/spark/config/JobArgsParser.java`

该解析器用于解析命令行参数。

```java
package io.github.atengk.spark.config;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Spark 任务参数解析器。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class JobArgsParser {

    private JobArgsParser() {
    }

    /**
     * 解析任务参数。
     *
     * @param args 命令行参数
     * @return 任务运行参数
     */
    public static JobRuntimeArgs parse(String[] args) {
        Map<String, String> argMap = parseToMap(args);

        JobRuntimeArgs runtimeArgs = new JobRuntimeArgs();
        runtimeArgs.setJobName(required(argMap, "jobName"));
        runtimeArgs.setEnv(StrUtil.blankToDefault(MapUtil.getStr(argMap, "env"), "local"));
        runtimeArgs.setBizDate(required(argMap, "bizDate"));
        runtimeArgs.setBatchId(StrUtil.blankToDefault(MapUtil.getStr(argMap, "batchId"), runtimeArgs.getBizDate()));
        runtimeArgs.setInputPath(MapUtil.getStr(argMap, "inputPath"));
        runtimeArgs.setOutputPath(MapUtil.getStr(argMap, "outputPath"));
        runtimeArgs.setOverwrite(Boolean.parseBoolean(StrUtil.blankToDefault(MapUtil.getStr(argMap, "overwrite"), "false")));

        log.info("任务参数解析完成，jobName={}，env={}，bizDate={}，batchId={}，overwrite={}",
                runtimeArgs.getJobName(),
                runtimeArgs.getEnv(),
                runtimeArgs.getBizDate(),
                runtimeArgs.getBatchId(),
                runtimeArgs.isOverwrite());

        return runtimeArgs;
    }

    /**
     * 将命令行参数解析为 Map。
     *
     * @param args 命令行参数
     * @return 参数 Map
     */
    private static Map<String, String> parseToMap(String[] args) {
        Map<String, String> argMap = new HashMap<>();

        if (args == null || args.length == 0) {
            return argMap;
        }

        for (int i = 0; i < args.length; i++) {
            String key = args[i];
            if (StrUtil.startWith(key, "--") && i + 1 < args.length) {
                String value = args[i + 1];
                if (!StrUtil.startWith(value, "--")) {
                    argMap.put(StrUtil.removePrefix(key, "--"), value);
                    i++;
                }
            }
        }

        return argMap;
    }

    /**
     * 获取必填参数。
     *
     * @param argMap 参数 Map
     * @param key 参数名
     * @return 参数值
     */
    private static String required(Map<String, String> argMap, String key) {
        String value = MapUtil.getStr(argMap, key);
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException(StrUtil.format("缺少必填参数：{}", key));
        }
        return value;
    }
}
```

Config 层设计建议如下：

| 建议               | 说明                             |
| ------------------ | -------------------------------- |
| 参数对象集中定义   | 不在业务代码中直接解析 args      |
| 关键参数必填校验   | jobName、bizDate、env 等必须明确 |
| 敏感配置不入日志   | 密码、Token、Keytab 不打印       |
| 多环境配置隔离     | local、test、prod 配置分开       |
| SparkConf 独立封装 | 不在 Job 中散落设置 Spark 参数   |
| 支持命令行覆盖     | 调度平台传入参数优先级最高       |

### Utility 工具类设计

Utility 工具类用于封装通用逻辑，例如日期处理、路径构建、SQL 加载、参数校验、HDFS 文件操作、数据质量校验等。工具类应保持无状态，方法职责明确，避免变成大而全的杂项类。

文件位置：`src/main/java/io/github/atengk/spark/util/PathBuilder.java`

该工具类用于统一构建数据路径。

```java
package io.github.atengk.spark.util;

import cn.hutool.core.util.StrUtil;

/**
 * 数据路径构建工具。
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class PathBuilder {

    private PathBuilder() {
    }

    /**
     * 构建分区路径。
     *
     * @param rootPath 根路径
     * @param tableName 表名或目录名
     * @param bizDate 业务日期
     * @return 分区路径
     */
    public static String buildDtPath(String rootPath, String tableName, String bizDate) {
        if (StrUtil.hasBlank(rootPath, tableName, bizDate)) {
            throw new IllegalArgumentException("根路径、表名和业务日期不能为空");
        }

        return StrUtil.format("{}/{}/dt={}",
                StrUtil.removeSuffix(rootPath, "/"),
                tableName,
                bizDate);
    }

    /**
     * 构建 Checkpoint 路径。
     *
     * @param rootPath 根路径
     * @param env 运行环境
     * @param jobName 任务名称
     * @return Checkpoint 路径
     */
    public static String buildCheckpointPath(String rootPath, String env, String jobName) {
        if (StrUtil.hasBlank(rootPath, env, jobName)) {
            throw new IllegalArgumentException("Checkpoint 根路径、环境和任务名称不能为空");
        }

        return StrUtil.format("{}/{}/{}",
                StrUtil.removeSuffix(rootPath, "/"),
                env,
                jobName);
    }
}
```

文件位置：`src/main/java/io/github/atengk/spark/util/BizDateUtil.java`

该工具类用于校验和处理业务日期。

```java
package io.github.atengk.spark.util;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;

import java.util.Date;

/**
 * 业务日期工具类。
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class BizDateUtil {

    private static final String BIZ_DATE_PATTERN = "\\d{4}-\\d{2}-\\d{2}";

    private BizDateUtil() {
    }

    /**
     * 校验业务日期。
     *
     * @param bizDate 业务日期
     */
    public static void validate(String bizDate) {
        if (StrUtil.isBlank(bizDate) || !ReUtil.isMatch(BIZ_DATE_PATTERN, bizDate)) {
            throw new IllegalArgumentException(StrUtil.format("业务日期格式不正确：{}", bizDate));
        }
    }

    /**
     * 获取上一天业务日期。
     *
     * @param bizDate 业务日期
     * @return 上一天日期
     */
    public static String previousDay(String bizDate) {
        validate(bizDate);
        Date date = DateUtil.parse(bizDate, DatePattern.NORM_DATE_PATTERN);
        return DateUtil.format(DateUtil.offsetDay(date, -1), DatePattern.NORM_DATE_PATTERN);
    }

    /**
     * 获取下一天业务日期。
     *
     * @param bizDate 业务日期
     * @return 下一天日期
     */
    public static String nextDay(String bizDate) {
        validate(bizDate);
        Date date = DateUtil.parse(bizDate, DatePattern.NORM_DATE_PATTERN);
        return DateUtil.format(DateUtil.offsetDay(date, 1), DatePattern.NORM_DATE_PATTERN);
    }
}
```

Utility 工具类设计建议如下：

| 建议             | 说明                             |
| ---------------- | -------------------------------- |
| 工具类无状态     | 使用 private 构造方法禁止实例化  |
| 方法命名明确     | 不使用含糊的 `handle`、`process` |
| 按领域拆分工具类 | 日期、路径、SQL、HDFS、校验分开  |
| 优先使用 Hutool  | 减少重复工具代码                 |
| 不写业务流程     | 工具类只提供通用能力             |
| 异常信息清晰     | 便于调用方定位问题               |

### Exception 异常设计

Exception 层用于统一定义 Spark 项目中的异常类型。合理的异常设计可以帮助区分参数错误、读取失败、转换失败、写出失败、数据质量失败和系统异常。

推荐异常类型如下：

| 异常类型                 | 说明             |
| ------------------------ | ---------------- |
| `SparkJobException`      | 任务执行通用异常 |
| `JobParamException`      | 参数异常         |
| `DataReadException`      | 数据读取异常     |
| `DataTransformException` | 数据转换异常     |
| `DataWriteException`     | 数据写出异常     |
| `DataQualityException`   | 数据质量异常     |

文件位置：`src/main/java/io/github/atengk/spark/exception/SparkJobException.java`

该异常用于表示 Spark 任务执行过程中的通用异常。

```java
package io.github.atengk.spark.exception;

/**
 * Spark 任务通用异常。
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class SparkJobException extends RuntimeException {

    /**
     * 创建异常。
     *
     * @param message 异常消息
     */
    public SparkJobException(String message) {
        super(message);
    }

    /**
     * 创建异常。
     *
     * @param message 异常消息
     * @param cause 原始异常
     */
    public SparkJobException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

文件位置：`src/main/java/io/github/atengk/spark/exception/DataQualityException.java`

该异常用于表示数据质量校验失败。

```java
package io.github.atengk.spark.exception;

/**
 * 数据质量异常。
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class DataQualityException extends RuntimeException {

    /**
     * 创建数据质量异常。
     *
     * @param message 异常消息
     */
    public DataQualityException(String message) {
        super(message);
    }

    /**
     * 创建数据质量异常。
     *
     * @param message 异常消息
     * @param cause 原始异常
     */
    public DataQualityException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

文件位置：`src/main/java/io/github/atengk/spark/exception/DataWriteException.java`

该异常用于表示数据写出失败。

```java
package io.github.atengk.spark.exception;

/**
 * 数据写出异常。
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class DataWriteException extends RuntimeException {

    /**
     * 创建数据写出异常。
     *
     * @param message 异常消息
     */
    public DataWriteException(String message) {
        super(message);
    }

    /**
     * 创建数据写出异常。
     *
     * @param message 异常消息
     * @param cause 原始异常
     */
    public DataWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

异常使用示例：

```java
try {
    writer.write(resultDf, writeOptions);
} catch (Exception e) {
    log.error("用户汇总结果写出失败，tableName={}，bizDate={}",
            writeOptions.get("tableName"), args.getBizDate(), e);
    throw new DataWriteException("用户汇总结果写出失败", e);
}
```

异常设计建议如下：

| 建议                   | 说明                                 |
| ---------------------- | ------------------------------------ |
| 异常类型按场景划分     | 参数、读取、转换、写出、质量分别处理 |
| 保留原始异常           | 包装异常时传入 cause                 |
| 异常消息包含关键上下文 | 任务名、表名、路径、业务日期         |
| 日志中记录堆栈         | 抛出前使用 `log.error` 记录          |
| 不吞异常               | 核心链路失败应直接失败任务           |
| 数据质量异常独立处理   | 便于调度平台识别质量失败             |


## 参数设计

本章节用于规范 Spark 任务的参数体系。Spark 项目通常由调度平台、Shell 脚本或人工命令触发，参数设计是否清晰会直接影响任务可重跑、可补数、可排查和可迁移能力。参数应覆盖任务标识、运行环境、业务时间、数据源、输出位置、资源配置和任务开关等内容。

### 任务参数

任务参数用于标识当前执行的是哪个 Spark 任务，以及本次任务对应的业务批次。所有 Spark 任务都应至少包含任务名称、业务日期、批次号和运行环境。

推荐任务参数如下：

| 参数         | 示例           | 是否必填 | 说明                             |
| ------------ | -------------- | -------- | -------------------------------- |
| `jobName`    | `user-summary` | 是       | 任务名称，用于选择具体 Job       |
| `bizDate`    | `2026-05-11`   | 是       | 业务日期，通常对应数据分区       |
| `batchId`    | `202605110001` | 否       | 批次号，用于审计和幂等控制       |
| `env`        | `prod`         | 是       | 运行环境，例如 local、test、prod |
| `overwrite`  | `true`         | 否       | 是否覆盖写入                     |
| `dryRun`     | `false`        | 否       | 是否只校验不写出                 |
| `allowEmpty` | `false`        | 否       | 是否允许结果为空                 |

提交示例：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  target/spark-job.jar \
  --jobName user-summary \
  --env prod \
  --bizDate 2026-05-11 \
  --batchId 202605110001 \
  --overwrite true \
  --allowEmpty false
```

任务参数设计建议如下：

| 建议               | 说明                                       |
| ------------------ | ------------------------------------------ |
| `jobName` 全局唯一 | 便于调度、日志、审计和任务路由             |
| `bizDate` 显式传入 | 不建议在代码中默认使用当前日期             |
| `batchId` 支持幂等 | 写 JDBC、Kafka、审计表时非常有用           |
| 布尔参数默认保守   | 覆盖、跳过校验、允许空数据等默认应为 false |
| 参数命名统一       | 不同任务尽量使用同一套参数名称             |

### 环境参数

环境参数用于控制任务运行在哪个环境，以及读取哪套配置、连接哪个集群和写入哪个路径。常见环境包括 `local`、`test`、`prod`。

推荐环境参数如下：

| 参数      | 示例            | 说明             |
| --------- | --------------- | ---------------- |
| `env`     | `prod`          | 运行环境         |
| `profile` | `prod`          | 配置文件标识     |
| `cluster` | `yarn-prod`     | 集群标识         |
| `queue`   | `root.prod.etl` | YARN 队列        |
| `region`  | `cn-hangzhou`   | 云资源地域       |
| `tenant`  | `default`       | 租户或业务线标识 |

环境配置示例：

```yaml
# application-prod.yml
# 生产环境配置
app:
  env: prod
  profile: prod

cluster:
  name: yarn-prod
  queue: root.prod.etl

warehouse:
  root-path: hdfs:///warehouse/prod
  checkpoint-root: hdfs:///checkpoint/prod
  error-root: hdfs:///error/prod
```

环境参数使用建议如下：

| 建议                 | 说明                                   |
| -------------------- | -------------------------------------- |
| 环境参数必须显式传入 | 生产任务不依赖本地默认环境             |
| local 不连接生产资源 | 防止本地调试误写生产库表               |
| test/prod 路径隔离   | 数据目录、Checkpoint、错误目录必须分离 |
| 队列参数由提交侧控制 | 资源调度参数不建议写死在 Java 代码中   |
| 敏感配置外置         | 密码、Keytab、Token 不放入普通配置文件 |

### 时间参数

时间参数用于控制任务处理的数据范围。离线任务通常使用业务日期，增量任务通常使用开始时间和结束时间，实时任务通常使用 Checkpoint 和 Offset 管理进度。

推荐时间参数如下：

| 参数        | 示例                  | 说明                       |
| ----------- | --------------------- | -------------------------- |
| `bizDate`   | `2026-05-11`          | 业务日期，常用于 Hive 分区 |
| `startDate` | `2026-05-01`          | 补数开始日期               |
| `endDate`   | `2026-05-11`          | 补数结束日期               |
| `startTime` | `2026-05-11 00:00:00` | 增量开始时间               |
| `endTime`   | `2026-05-12 00:00:00` | 增量结束时间               |
| `hour`      | `13`                  | 小时分区                   |
| `timezone`  | `Asia/Shanghai`       | 业务时区                   |

时间参数设计建议如下：

| 建议                   | 说明                           |
| ---------------------- | ------------------------------ |
| 日期格式统一           | 推荐 `yyyy-MM-dd`              |
| 时间格式统一           | 推荐 `yyyy-MM-dd HH:mm:ss`     |
| 增量区间左闭右开       | 推荐 `[startTime, endTime)`    |
| 补数支持日期范围       | 避免重复手动提交多次           |
| 时区明确               | 跨地区任务必须统一时区         |
| 不直接使用系统当前时间 | 调度延迟时容易造成处理日期错误 |

时间参数校验和处理工具如下。

文件位置：`src/main/java/io/github/atengk/spark/param/TimeParamUtil.java`

该工具类用于校验业务日期、时间范围和补数日期范围。

```java
package io.github.atengk.spark.param;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;

import java.util.Date;

/**
 * 时间参数工具类。
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class TimeParamUtil {

    private static final String DATE_PATTERN = "\\d{4}-\\d{2}-\\d{2}";
    private static final String DATETIME_PATTERN = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";

    private TimeParamUtil() {
    }

    /**
     * 校验业务日期。
     *
     * @param bizDate 业务日期
     */
    public static void validateBizDate(String bizDate) {
        if (StrUtil.isBlank(bizDate) || !ReUtil.isMatch(DATE_PATTERN, bizDate)) {
            throw new IllegalArgumentException(StrUtil.format("业务日期格式不正确：{}", bizDate));
        }
    }

    /**
     * 校验时间范围。
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     */
    public static void validateTimeRange(String startTime, String endTime) {
        if (!ReUtil.isMatch(DATETIME_PATTERN, startTime) || !ReUtil.isMatch(DATETIME_PATTERN, endTime)) {
            throw new IllegalArgumentException("开始时间或结束时间格式不正确，格式应为 yyyy-MM-dd HH:mm:ss");
        }

        Date start = DateUtil.parse(startTime, DatePattern.NORM_DATETIME_PATTERN);
        Date end = DateUtil.parse(endTime, DatePattern.NORM_DATETIME_PATTERN);

        if (!start.before(end)) {
            throw new IllegalArgumentException(StrUtil.format("开始时间必须早于结束时间，startTime={}，endTime={}", startTime, endTime));
        }
    }

    /**
     * 获取业务日期的上一天。
     *
     * @param bizDate 业务日期
     * @return 上一天日期
     */
    public static String previousDay(String bizDate) {
        validateBizDate(bizDate);
        Date date = DateUtil.parse(bizDate, DatePattern.NORM_DATE_PATTERN);
        return DateUtil.format(DateUtil.offsetDay(date, -1), DatePattern.NORM_DATE_PATTERN);
    }
}
```

### 数据源参数

数据源参数用于描述任务读取的数据来源，包括 Hive 表、HDFS 路径、Kafka Topic、JDBC 连接、对象存储路径等。数据源参数应按类型分组，避免在命令行中传入大量无结构参数。

常见数据源参数如下：

| 数据源   | 参数                             | 示例                                       |
| -------- | -------------------------------- | ------------------------------------------ |
| Hive     | `sourceDb`、`sourceTable`        | `dwd`、`dwd_user_detail`                   |
| HDFS     | `inputPath`                      | `hdfs:///warehouse/dwd/user/dt=2026-05-11` |
| Kafka    | `bootstrapServers`、`topic`      | `kafka-host:9092`、`ods_user_event`        |
| JDBC     | `jdbcUrl`、`dbtable`、`username` | `jdbc:mysql://...`、`user_info`            |
| 对象存储 | `bucket`、`endpoint`、`path`     | `data-bucket`、`oss-cn-hangzhou...`        |

推荐配置方式：

```yaml
# 数据源配置
datasource:
  hive:
    source-db: dwd
    user-table: dwd_user_detail
    order-table: dwd_order_detail

  kafka:
    bootstrap-servers: kafka-host:9092
    source-topic: ods_user_event
    sink-topic: dwd_user_event

  jdbc:
    mysql:
      url: jdbc:mysql://mysql-host:3306/demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
      username: ${MYSQL_USERNAME}
      password: ${MYSQL_PASSWORD}
      driver: com.mysql.cj.jdbc.Driver
```

数据源参数设计建议如下：

| 建议                   | 说明                               |
| ---------------------- | ---------------------------------- |
| 表名和路径不要硬编码   | 通过配置或参数传入                 |
| 密码使用环境变量       | 不在命令行日志和配置文件中明文输出 |
| Kafka Topic 按环境隔离 | test/prod 不共用 Topic             |
| JDBC 读取使用只读账号  | 降低误操作风险                     |
| 对象存储密钥外置       | 使用 Role、环境变量或密钥系统      |
| 关键参数启动即校验     | 不等到读取阶段才失败               |

### 输出路径参数

输出路径参数用于控制结果写入位置。Spark 任务输出通常包括正式数据路径、错误数据路径、临时路径、Checkpoint 路径和审计结果路径。

推荐输出参数如下：

| 参数             | 示例                                               | 说明              |
| ---------------- | -------------------------------------------------- | ----------------- |
| `outputPath`     | `hdfs:///warehouse/ads/user_summary/dt=2026-05-11` | 正式输出路径      |
| `errorPath`      | `hdfs:///error/user_summary/dt=2026-05-11`         | 异常数据路径      |
| `tempPath`       | `hdfs:///tmp/spark/user_summary/202605110001`      | 临时路径          |
| `checkpointPath` | `hdfs:///checkpoint/user_summary`                  | 流任务 Checkpoint |
| `targetTable`    | `ads.ads_user_summary`                             | Hive 目标表       |
| `writeMode`      | `overwrite`                                        | 写入模式          |

路径构建示例：

```java
String outputPath = PathBuilder.buildDtPath(
        "hdfs:///warehouse/prod/ads/user",
        "ads_user_summary",
        "2026-05-11"
);
```

输出路径设计建议如下：

| 建议                       | 说明                     |
| -------------------------- | ------------------------ |
| 正式路径和临时路径分离     | 防止临时数据污染正式目录 |
| 错误数据单独落地           | 便于排查数据质量问题     |
| Checkpoint 独立规划        | 不要放在业务数据目录下   |
| 路径包含环境信息           | 防止 test/prod 混写      |
| 覆盖写入必须限定分区       | 不要覆盖上层目录         |
| 输出路径统一通过工具类生成 | 避免手写路径不一致       |

### 资源参数

资源参数用于控制 Spark 任务的 Driver、Executor、CPU、内存、并行度、队列和动态资源配置。资源参数通常通过 `spark-submit`、调度平台或配置文件传入，不建议写死在业务代码中。

常见资源参数如下：

| 参数                              | 示例            | 说明                   |
| --------------------------------- | --------------- | ---------------------- |
| `--driver-memory`                 | `2g`            | Driver 内存            |
| `--executor-memory`               | `4g`            | Executor 内存          |
| `--executor-cores`                | `2`             | 每个 Executor CPU 核数 |
| `--num-executors`                 | `8`             | Executor 数量          |
| `--queue`                         | `root.prod.etl` | YARN 队列              |
| `spark.sql.shuffle.partitions`    | `400`           | SQL Shuffle 分区数     |
| `spark.dynamicAllocation.enabled` | `true`          | 是否开启动态资源分配   |

提交示例：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --queue root.prod.etl \
  --driver-memory 2g \
  --executor-memory 4g \
  --executor-cores 2 \
  --num-executors 8 \
  --conf spark.sql.shuffle.partitions=400 \
  --conf spark.sql.adaptive.enabled=true \
  target/spark-job.jar \
  --jobName user-summary \
  --env prod \
  --bizDate 2026-05-11
```

资源参数设计建议如下：

| 建议                 | 说明                                           |
| -------------------- | ---------------------------------------------- |
| 资源配置由提交侧控制 | 不写死在 Java 业务代码中                       |
| 本地调试降低并行度   | `spark.sql.shuffle.partitions` 可设置为 2 到 8 |
| 生产按数据量调整     | 根据输入量、Shuffle 量和任务耗时调参           |
| 开启 AQE             | Spark 3.x 推荐开启自适应执行                   |
| 不盲目增加内存       | 应先分析执行计划和 Spark UI                    |
| 资源参数版本化       | 重要任务资源调整应记录变更原因                 |

### 参数校验

参数校验用于在任务启动阶段发现问题，避免任务运行到中后段才失败。参数校验应覆盖必填参数、日期格式、路径前缀、写入模式、环境值、表名格式和布尔参数等。

推荐校验规则如下：

| 参数类型 | 校验规则                         |
| -------- | -------------------------------- |
| 必填参数 | 不能为空                         |
| 日期参数 | 符合 `yyyy-MM-dd`                |
| 时间参数 | 符合 `yyyy-MM-dd HH:mm:ss`       |
| 环境参数 | 只能是 `local`、`test`、`prod`   |
| 写入模式 | 只能是 `append`、`overwrite`     |
| 表名     | 只能包含字母、数字、下划线和点号 |
| 路径     | 必须以允许的协议开头             |
| 布尔参数 | 只能是 `true` 或 `false`         |

文件位置：`src/main/java/io/github/atengk/spark/param/JobParamValidator.java`

该校验器用于集中校验 Spark 任务启动参数。

```java
package io.github.atengk.spark.param;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.spark.config.JobRuntimeArgs;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * Spark 任务参数校验器。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class JobParamValidator {

    private static final List<String> ALLOW_ENV_LIST = Arrays.asList("local", "test", "prod");
    private static final List<String> ALLOW_WRITE_MODE_LIST = Arrays.asList("append", "overwrite");
    private static final List<String> ALLOW_PATH_PREFIX_LIST = Arrays.asList("hdfs://", "s3a://", "oss://", "cosn://", "data/", "/tmp/");
    private static final String TABLE_PATTERN = "[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)?";

    private JobParamValidator() {
    }

    /**
     * 校验任务运行参数。
     *
     * @param args 任务运行参数
     */
    public static void validate(JobRuntimeArgs args) {
        if (args == null) {
            throw new IllegalArgumentException("任务运行参数不能为空");
        }

        checkRequired(args.getJobName(), "jobName");
        checkRequired(args.getEnv(), "env");
        checkRequired(args.getBizDate(), "bizDate");

        if (!ALLOW_ENV_LIST.contains(args.getEnv())) {
            throw new IllegalArgumentException(StrUtil.format("运行环境不合法：{}", args.getEnv()));
        }

        TimeParamUtil.validateBizDate(args.getBizDate());

        if (StrUtil.isNotBlank(args.getTargetTable()) && !ReUtil.isMatch(TABLE_PATTERN, args.getTargetTable())) {
            throw new IllegalArgumentException(StrUtil.format("目标表名格式不正确：{}", args.getTargetTable()));
        }

        if (StrUtil.isNotBlank(args.getWriteMode()) && !ALLOW_WRITE_MODE_LIST.contains(args.getWriteMode())) {
            throw new IllegalArgumentException(StrUtil.format("写入模式不合法：{}", args.getWriteMode()));
        }

        if (StrUtil.isNotBlank(args.getOutputPath())) {
            validatePath(args.getOutputPath(), "outputPath");
        }

        log.info("任务参数校验通过，jobName={}，env={}，bizDate={}", args.getJobName(), args.getEnv(), args.getBizDate());
    }

    /**
     * 校验必填参数。
     *
     * @param value 参数值
     * @param name 参数名
     */
    private static void checkRequired(String value, String name) {
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException(StrUtil.format("缺少必填参数：{}", name));
        }
    }

    /**
     * 校验路径参数。
     *
     * @param path 路径
     * @param name 参数名
     */
    private static void validatePath(String path, String name) {
        boolean matched = CollUtil.emptyIfNull(ALLOW_PATH_PREFIX_LIST)
                .stream()
                .anyMatch(path::startsWith);

        if (!matched) {
            throw new IllegalArgumentException(StrUtil.format("{} 路径前缀不合法：{}", name, path));
        }
    }
}
```

### 参数默认值

参数默认值用于降低本地调试成本，但生产任务默认值必须谨慎。对于可能造成误写、误删、误覆盖的参数，不应设置激进默认值。

推荐默认值如下：

| 参数                | 默认值                      | 说明             |
| ------------------- | --------------------------- | ---------------- |
| `env`               | `local`                     | 仅适合本地开发   |
| `batchId`           | 等于 `bizDate`              | 简化普通离线任务 |
| `overwrite`         | `false`                     | 防止误覆盖       |
| `allowEmpty`        | `false`                     | 防止空结果写出   |
| `dryRun`            | `false`                     | 正常执行         |
| `writeMode`         | `append`                    | 默认追加更安全   |
| `shufflePartitions` | local 为 `4`，prod 为 `400` | 按环境区分       |

参数默认值填充示例：

```java
runtimeArgs.setEnv(StrUtil.blankToDefault(MapUtil.getStr(argMap, "env"), "local"));
runtimeArgs.setBatchId(StrUtil.blankToDefault(MapUtil.getStr(argMap, "batchId"), runtimeArgs.getBizDate()));
runtimeArgs.setWriteMode(StrUtil.blankToDefault(MapUtil.getStr(argMap, "writeMode"), "append"));
runtimeArgs.setOverwrite(Boolean.parseBoolean(StrUtil.blankToDefault(MapUtil.getStr(argMap, "overwrite"), "false")));
runtimeArgs.setAllowEmpty(Boolean.parseBoolean(StrUtil.blankToDefault(MapUtil.getStr(argMap, "allowEmpty"), "false")));
```

默认值设计建议如下：

| 建议                       | 说明                              |
| -------------------------- | --------------------------------- |
| 默认值只做兜底             | 关键生产参数仍应显式传入          |
| 覆盖类参数默认 false       | 避免误删误覆盖                    |
| local 默认路径不能指向生产 | 本地默认只使用 `data/` 或 `/tmp/` |
| 默认值写入日志             | 便于确认实际运行参数              |
| 不给敏感参数设置明文默认值 | 密码、Token 必须外部注入          |
| 参数解析后统一打印脱敏结果 | 便于排查调度参数问题              |

## 日志设计

本章节用于规范 Spark 项目的日志框架、日志格式、启动日志、参数日志、数据量日志、异常日志、耗时日志和审计日志。Spark 任务通常运行在集群中，日志是排查失败、分析性能、确认数据量和审计任务行为的核心依据。

### 日志框架选择

Spark 项目建议业务代码统一使用 SLF4J 作为日志门面，具体实现根据 Spark 版本和集群规范选择 Logback 或 Log4j2。不要在业务代码中直接依赖具体日志实现类。

常见选择如下：

| 方案               | 说明                                         |
| ------------------ | -------------------------------------------- |
| SLF4J + Logback    | Java 项目常见组合，本地开发友好              |
| SLF4J + Log4j2     | Spark 3.x 集群中较常见                       |
| Spark 内置日志配置 | 集群统一管理日志时使用                       |
| 不建议 System.out  | 生产代码不使用 `System.out.println` 作为日志 |

Maven 依赖示例：

```xml
<!-- SLF4J API：日志门面，业务代码统一使用 Logger 输出日志 -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>${slf4j.version}</version>
</dependency>

<!-- Logback：本地开发和普通 Java 项目常用日志实现 -->
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>${logback.version}</version>
</dependency>
```

日志框架选择建议如下：

| 建议                 | 说明                               |
| -------------------- | ---------------------------------- |
| 业务代码只使用 SLF4J | 使用 `@Slf4j` 或 `LoggerFactory`   |
| 避免多个日志绑定     | 不同时引入多个 SLF4J 实现          |
| 与集群日志体系一致   | Spark 集群使用 Log4j2 时要避免冲突 |
| 依赖树检查日志冲突   | 使用 `mvn dependency:tree` 排查    |
| 生产日志级别保守     | Spark、Hadoop、Kafka 默认 WARN     |

### 日志格式规范

日志格式应包含时间、日志级别、线程、Logger 名称、任务标识、业务日期和日志消息。Spark 任务日志需要便于在 YARN UI、History Server、日志采集平台中检索。

推荐 Logback 配置如下。

文件位置：`src/main/resources/logback.xml`

该配置用于控制业务日志格式、日志级别和文件滚动策略。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 日志目录，可通过 -DLOG_HOME 覆盖 -->
    <property name="LOG_HOME" value="${LOG_HOME:-logs/app}"/>

    <!-- 控制台日志，适合本地开发和集群日志采集 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 文件日志，按天滚动 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_HOME}/spark-job.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- 日志保留 30 天 -->
            <fileNamePattern>${LOG_HOME}/archive/spark-job.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>

    <!-- 降低框架日志噪声 -->
    <logger name="org.apache.spark" level="WARN"/>
    <logger name="org.apache.hadoop" level="WARN"/>
    <logger name="org.apache.kafka" level="WARN"/>

    <!-- 项目业务日志 -->
    <logger name="io.github.atengk" level="INFO"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

日志格式建议如下：

| 字段    | 说明                       |
| ------- | -------------------------- |
| 时间    | 精确到毫秒                 |
| 级别    | INFO、WARN、ERROR          |
| 线程    | 便于区分 Driver 和异步线程 |
| Logger  | 定位具体类                 |
| jobName | 任务名称                   |
| bizDate | 业务日期                   |
| batchId | 批次号                     |
| message | 清晰描述事件               |

### 任务启动日志

任务启动日志用于记录任务开始执行时的上下文，包括任务名称、运行环境、业务日期、批次号、Spark 应用名称、Master、队列和提交参数等。

启动日志示例：

```java
log.info("Spark 任务启动，jobName={}，env={}，bizDate={}，batchId={}",
        runtimeArgs.getJobName(),
        runtimeArgs.getEnv(),
        runtimeArgs.getBizDate(),
        runtimeArgs.getBatchId());

log.info("Spark 应用信息，appName={}，master={}，applicationId={}",
        spark.sparkContext().appName(),
        spark.sparkContext().master(),
        spark.sparkContext().applicationId());
```

建议记录的启动信息如下：

| 信息            | 说明                 |
| --------------- | -------------------- |
| `jobName`       | 当前任务名称         |
| `env`           | 运行环境             |
| `bizDate`       | 业务日期             |
| `batchId`       | 批次号               |
| `appName`       | Spark 应用名         |
| `applicationId` | Spark Application ID |
| `master`        | 运行模式             |
| `queue`         | YARN 队列            |
| `startTime`     | 任务开始时间         |

任务启动日志建议如下：

| 建议                        | 说明                         |
| --------------------------- | ---------------------------- |
| 启动日志必须完整            | 失败后可快速识别任务上下文   |
| 使用结构化字段              | 便于日志平台检索             |
| 启动阶段打印 Spark 配置摘要 | 只打印关键配置               |
| 不打印敏感信息              | 密码、Token、Keytab 不输出   |
| 任务入口统一打印            | 不在各个组件重复打印同类信息 |

### 参数日志

参数日志用于确认调度平台或提交脚本传入的实际参数。参数日志必须脱敏，不能直接打印数据库密码、访问密钥、Token、Keytab 等敏感信息。

文件位置：`src/main/java/io/github/atengk/spark/log/SensitiveLogUtil.java`

该工具类用于对参数 Map 进行脱敏后输出。

```java
package io.github.atengk.spark.log;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 敏感日志脱敏工具。
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class SensitiveLogUtil {

    private SensitiveLogUtil() {
    }

    /**
     * 脱敏参数 Map。
     *
     * @param paramMap 原始参数
     * @return 脱敏后的参数
     */
    public static Map<String, String> maskParams(Map<String, String> paramMap) {
        Map<String, String> resultMap = new HashMap<>();

        if (MapUtil.isEmpty(paramMap)) {
            return resultMap;
        }

        paramMap.forEach((key, value) -> {
            if (isSensitiveKey(key)) {
                resultMap.put(key, "******");
            } else {
                resultMap.put(key, value);
            }
        });

        return resultMap;
    }

    /**
     * 判断是否为敏感参数。
     *
     * @param key 参数名
     * @return true 表示敏感参数
     */
    private static boolean isSensitiveKey(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }

        String lowerKey = StrUtil.lowerFirst(key).toLowerCase();
        return StrUtil.containsAny(lowerKey, "password", "secret", "token", "keytab", "accesskey", "secretkey");
    }
}
```

参数日志示例：

```java
Map<String, String> rawParams = JobArgsParser.parseToMap(args);
Map<String, String> safeParams = SensitiveLogUtil.maskParams(rawParams);
log.info("任务启动参数：{}", safeParams);
```

参数日志建议如下：

| 建议               | 说明                                   |
| ------------------ | -------------------------------------- |
| 参数解析后立即打印 | 便于排查调度参数问题                   |
| 敏感参数必须脱敏   | 密码、Token、Secret、Keytab 不直接输出 |
| 默认值也要打印     | 便于确认实际生效配置                   |
| 参数异常要明确提示 | 指出缺少哪个参数或格式哪里错误         |
| 不打印完整超长 SQL | 可打印 SQL 文件路径和参数摘要          |

### 数据量日志

数据量日志用于记录任务读取、清洗、过滤、聚合和写出的数据量。数据量日志是数据质量排查和任务审计的重要依据。

常见数据量指标如下：

| 指标             | 说明             |
| ---------------- | ---------------- |
| `inputCount`     | 输入数据量       |
| `validCount`     | 有效数据量       |
| `invalidCount`   | 异常数据量       |
| `duplicateCount` | 重复数据量       |
| `outputCount`    | 输出数据量       |
| `joinMissCount`  | 维度关联失败数量 |
| `partitionCount` | 输出分区数量     |

数据量日志示例：

```java
long inputCount = sourceDf.count();
log.info("数据读取完成，dataset={}，inputCount={}", "用户明细", inputCount);

Dataset<Row> validDf = sourceDf.filter(col("user_id").isNotNull());
long validCount = validDf.count();
long invalidCount = inputCount - validCount;

log.info("数据清洗完成，dataset={}，inputCount={}，validCount={}，invalidCount={}",
        "用户明细", inputCount, validCount, invalidCount);
```

封装数据量日志工具。

文件位置：`src/main/java/io/github/atengk/spark/log/DataMetricLogger.java`

```java
package io.github.atengk.spark.log;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

/**
 * 数据量日志工具。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class DataMetricLogger {

    private DataMetricLogger() {
    }

    /**
     * 记录数据集数量。
     *
     * @param datasetName 数据集名称
     * @param dataset 数据集
     * @return 数据量
     */
    public static long logCount(String datasetName, Dataset<Row> dataset) {
        if (StrUtil.isBlank(datasetName)) {
            throw new IllegalArgumentException("数据集名称不能为空");
        }
        if (dataset == null) {
            throw new IllegalArgumentException("数据集不能为空");
        }

        long count = dataset.count();
        log.info("数据量统计，dataset={}，count={}", datasetName, count);
        return count;
    }

    /**
     * 记录输入输出数量。
     *
     * @param stage 阶段名称
     * @param inputCount 输入数量
     * @param outputCount 输出数量
     */
    public static void logInputOutput(String stage, long inputCount, long outputCount) {
        log.info("阶段数据量，stage={}，inputCount={}，outputCount={}，diffCount={}",
                stage, inputCount, outputCount, inputCount - outputCount);
    }
}
```

数据量日志建议如下：

| 建议               | 说明                         |
| ------------------ | ---------------------------- |
| 关键阶段记录 count | 读取、清洗、写出阶段必须记录 |
| 不要过度 count     | 每次 count 都会触发 Action   |
| 复用结果前可 cache | 避免多次 count 重复计算      |
| 异常数据单独统计   | 便于追踪上游质量问题         |
| 输出数量必须记录   | 用于校验本次任务是否正常产出 |
| 重要指标写入审计表 | 便于后续查询和告警           |

### 异常日志

异常日志用于记录任务失败原因、关键参数、异常堆栈和失败阶段。异常日志必须包含足够上下文，不能只打印 `e.getMessage()`。

异常日志示例：

```java
try {
    writer.write(resultDf, writeOptions);
} catch (Exception e) {
    log.error("数据写出失败，jobName={}，bizDate={}，targetTable={}，writeMode={}",
            args.getJobName(),
            args.getBizDate(),
            writeOptions.get("tableName"),
            writeOptions.get("mode"),
            e);
    throw new DataWriteException("数据写出失败", e);
}
```

异常日志建议如下：

| 建议                 | 说明                                 |
| -------------------- | ------------------------------------ |
| 必须打印堆栈         | 使用 `log.error("xxx", e)`           |
| 包含失败阶段         | 读取、转换、校验、写出               |
| 包含关键上下文       | 任务名、业务日期、表名、路径         |
| 不吞异常             | 核心任务失败应抛出异常让调度平台感知 |
| 敏感信息脱敏         | JDBC URL 可打印，密码不可打印        |
| 数据质量异常单独区分 | 便于识别是数据问题还是系统问题       |

常见异常分类：

| 异常类型 | 说明                                    |
| -------- | --------------------------------------- |
| 参数异常 | 缺少参数、格式错误、环境不合法          |
| 读取异常 | Hive 表不存在、路径不存在、Kafka 不可用 |
| 转换异常 | 字段不存在、类型转换失败、SQL 语法错误  |
| 质量异常 | 空数据、重复数据、必填字段为空          |
| 写出异常 | 权限不足、表结构不匹配、路径覆盖失败    |
| 资源异常 | Executor OOM、Driver OOM、Shuffle 失败  |

### 性能耗时日志

性能耗时日志用于记录任务整体耗时和关键阶段耗时，包括数据读取、转换、Join、聚合、写出等。耗时日志可以辅助定位性能瓶颈。

使用 Hutool `TimeInterval` 记录耗时：

```java
TimeInterval timer = new TimeInterval();

Dataset<Row> sourceDf = reader.read(spark, readOptions);
long inputCount = sourceDf.count();
log.info("数据读取完成，inputCount={}，耗时={} ms", inputCount, timer.intervalRestart());

Dataset<Row> resultDf = transformer.transform(sourceDf);
long outputCount = resultDf.count();
log.info("数据转换完成，outputCount={}，耗时={} ms", outputCount, timer.intervalRestart());

writer.write(resultDf, writeOptions);
log.info("数据写出完成，耗时={} ms", timer.intervalRestart());
```

封装阶段耗时日志工具。

文件位置：`src/main/java/io/github/atengk/spark/log/StageTimer.java`

```java
package io.github.atengk.spark.log;

import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 阶段耗时记录器。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class StageTimer {

    private final TimeInterval timer = new TimeInterval();

    /**
     * 记录阶段耗时并重置计时器。
     *
     * @param stage 阶段名称
     */
    public void logAndRestart(String stage) {
        if (StrUtil.isBlank(stage)) {
            throw new IllegalArgumentException("阶段名称不能为空");
        }

        long cost = timer.intervalRestart();
        log.info("阶段耗时，stage={}，costMs={}", stage, cost);
    }

    /**
     * 记录总耗时。
     *
     * @param jobName 任务名称
     */
    public void logTotal(String jobName) {
        long cost = timer.interval();
        log.info("任务总耗时，jobName={}，costMs={}", jobName, cost);
    }
}
```

性能耗时日志建议如下：

| 建议               | 说明                                     |
| ------------------ | ---------------------------------------- |
| 记录总耗时         | 每个 Job 必须记录                        |
| 记录关键阶段耗时   | 读取、转换、Join、聚合、写出             |
| 结合 Spark UI 分析 | 日志只能粗略定位，详细性能看 Spark UI    |
| 避免无意义 Action  | 为记录耗时额外触发过多 count 会影响性能  |
| 慢任务增加上下文   | 记录输入量、输出量、分区数、Shuffle 参数 |
| 长期任务沉淀趋势   | 审计表中保存耗时用于趋势分析             |

### 审计日志

审计日志用于记录任务执行结果，包括任务名称、运行环境、业务日期、批次号、输入量、输出量、状态、开始时间、结束时间、耗时和异常信息。审计日志通常写入数据库、Hive 审计表或日志平台。

推荐审计字段如下：

| 字段             | 说明                 |
| ---------------- | -------------------- |
| `job_name`       | 任务名称             |
| `env`            | 运行环境             |
| `biz_date`       | 业务日期             |
| `batch_id`       | 批次号               |
| `application_id` | Spark Application ID |
| `input_count`    | 输入数据量           |
| `output_count`   | 输出数据量           |
| `status`         | SUCCESS、FAILED      |
| `error_message`  | 异常摘要             |
| `start_time`     | 开始时间             |
| `end_time`       | 结束时间             |
| `cost_ms`        | 执行耗时             |

Hive 审计表示例：

```sql
CREATE TABLE IF NOT EXISTS audit.spark_job_audit (
    job_name STRING COMMENT '任务名称',
    env STRING COMMENT '运行环境',
    biz_date STRING COMMENT '业务日期',
    batch_id STRING COMMENT '批次号',
    application_id STRING COMMENT 'Spark Application ID',
    input_count BIGINT COMMENT '输入数据量',
    output_count BIGINT COMMENT '输出数据量',
    status STRING COMMENT '任务状态',
    error_message STRING COMMENT '异常信息',
    start_time TIMESTAMP COMMENT '开始时间',
    end_time TIMESTAMP COMMENT '结束时间',
    cost_ms BIGINT COMMENT '耗时毫秒'
)
COMMENT 'Spark任务审计表'
PARTITIONED BY (dt STRING COMMENT '审计分区日期')
STORED AS PARQUET;
```

文件位置：`src/main/java/io/github/atengk/spark/audit/SparkJobAudit.java`

该实体类用于封装 Spark 任务审计信息。

```java
package io.github.atengk.spark.audit;

import lombok.Data;

import java.io.Serializable;

/**
 * Spark 任务审计信息。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
public class SparkJobAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务名称。
     */
    private String jobName;

    /**
     * 运行环境。
     */
    private String env;

    /**
     * 业务日期。
     */
    private String bizDate;

    /**
     * 批次号。
     */
    private String batchId;

    /**
     * Spark Application ID。
     */
    private String applicationId;

    /**
     * 输入数量。
     */
    private Long inputCount;

    /**
     * 输出数量。
     */
    private Long outputCount;

    /**
     * 任务状态。
     */
    private String status;

    /**
     * 异常信息。
     */
    private String errorMessage;

    /**
     * 耗时毫秒。
     */
    private Long costMs;
}
```

文件位置：`src/main/java/io/github/atengk/spark/audit/SparkJobAuditLogger.java`

该工具类用于输出审计日志，实际项目中可扩展为写入 Hive、MySQL 或日志平台。

```java
package io.github.atengk.spark.audit;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Spark 任务审计日志记录器。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class SparkJobAuditLogger {

    private SparkJobAuditLogger() {
    }

    /**
     * 记录任务成功审计日志。
     *
     * @param audit 审计信息
     */
    public static void success(SparkJobAudit audit) {
        checkAudit(audit);
        audit.setStatus("SUCCESS");

        log.info("任务审计日志，jobName={}，env={}，bizDate={}，batchId={}，applicationId={}，inputCount={}，outputCount={}，status={}，costMs={}",
                audit.getJobName(),
                audit.getEnv(),
                audit.getBizDate(),
                audit.getBatchId(),
                audit.getApplicationId(),
                audit.getInputCount(),
                audit.getOutputCount(),
                audit.getStatus(),
                audit.getCostMs());
    }

    /**
     * 记录任务失败审计日志。
     *
     * @param audit 审计信息
     * @param e 异常
     */
    public static void failed(SparkJobAudit audit, Exception e) {
        checkAudit(audit);
        audit.setStatus("FAILED");
        audit.setErrorMessage(e == null ? null : StrUtil.subPre(e.getMessage(), 1000));

        log.error("任务审计日志，jobName={}，env={}，bizDate={}，batchId={}，applicationId={}，inputCount={}，outputCount={}，status={}，errorMessage={}，costMs={}",
                audit.getJobName(),
                audit.getEnv(),
                audit.getBizDate(),
                audit.getBatchId(),
                audit.getApplicationId(),
                audit.getInputCount(),
                audit.getOutputCount(),
                audit.getStatus(),
                audit.getErrorMessage(),
                audit.getCostMs(),
                e);
    }

    /**
     * 校验审计信息。
     *
     * @param audit 审计信息
     */
    private static void checkAudit(SparkJobAudit audit) {
        if (audit == null) {
            throw new IllegalArgumentException("任务审计信息不能为空");
        }
    }
}
```

审计日志设计建议如下：

| 建议                     | 说明                                |
| ------------------------ | ----------------------------------- |
| 每次任务必须生成审计记录 | 成功和失败都要记录                  |
| 审计字段结构化           | 便于查询、统计和告警                |
| 记录 Application ID      | 便于跳转 Spark UI 或 History Server |
| 记录输入输出量           | 用于数据波动分析                    |
| 失败记录异常摘要         | 便于快速定位问题                    |
| 审计数据长期保存         | 支撑任务稳定性分析和问题追溯        |


## 异常处理

本章节用于规范 Spark 项目中的异常分类、捕获、日志记录、失败退出和恢复策略。Spark 任务运行在分布式环境中，异常可能来自参数、数据源、SQL、网络、权限、依赖、资源和下游系统。异常处理的目标不是简单捕获所有异常，而是让任务失败原因清晰、日志可定位、状态可审计、任务可安全重跑。

### 参数异常

参数异常通常发生在任务启动阶段，例如缺少 `jobName`、`bizDate`、`env`，日期格式错误，写入模式不合法，输入路径为空，目标表名格式错误等。参数异常应在 SparkSession 初始化或业务处理前尽早发现，避免任务申请集群资源后才失败。

常见参数异常如下：

| 异常场景     | 示例                                                  |
| ------------ | ----------------------------------------------------- |
| 必填参数缺失 | 未传 `--jobName`、`--bizDate`                         |
| 日期格式错误 | `20260511`、`2026/05/11`                              |
| 环境值错误   | `prd`、`production` 未在白名单内                      |
| 写入模式错误 | `replace`、`delete`                                   |
| 路径前缀非法 | 输出路径不是 `hdfs://`、`s3a://`、`oss://` 等允许协议 |
| 表名格式错误 | 表名包含空格、特殊字符                                |

文件位置：`src/main/java/io/github/atengk/spark/exception/JobParamException.java`

该异常用于表示任务启动参数错误。

```java
package io.github.atengk.spark.exception;

/**
 * 任务参数异常。
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class JobParamException extends RuntimeException {

    /**
     * 创建任务参数异常。
     *
     * @param message 异常消息
     */
    public JobParamException(String message) {
        super(message);
    }

    /**
     * 创建任务参数异常。
     *
     * @param message 异常消息
     * @param cause 原始异常
     */
    public JobParamException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

文件位置：`src/main/java/io/github/atengk/spark/param/JobParamChecker.java`

该校验器用于在任务启动阶段统一校验关键参数。

```java
package io.github.atengk.spark.param;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.spark.config.JobRuntimeArgs;
import io.github.atengk.spark.exception.JobParamException;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * Spark 任务参数检查器。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class JobParamChecker {

    private static final List<String> ALLOW_ENV_LIST = Arrays.asList("local", "test", "prod");
    private static final List<String> ALLOW_WRITE_MODE_LIST = Arrays.asList("append", "overwrite");
    private static final String DATE_PATTERN = "\\d{4}-\\d{2}-\\d{2}";
    private static final String TABLE_PATTERN = "[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)?";

    private JobParamChecker() {
    }

    /**
     * 校验任务参数。
     *
     * @param args 任务参数
     */
    public static void check(JobRuntimeArgs args) {
        if (args == null) {
            throw new JobParamException("任务参数不能为空");
        }

        checkRequired(args.getJobName(), "jobName");
        checkRequired(args.getBizDate(), "bizDate");
        checkRequired(args.getEnv(), "env");

        if (!ALLOW_ENV_LIST.contains(args.getEnv())) {
            throw new JobParamException(StrUtil.format("运行环境不合法：{}", args.getEnv()));
        }

        if (!ReUtil.isMatch(DATE_PATTERN, args.getBizDate())) {
            throw new JobParamException(StrUtil.format("业务日期格式不正确：{}", args.getBizDate()));
        }

        if (StrUtil.isNotBlank(args.getWriteMode()) && !ALLOW_WRITE_MODE_LIST.contains(args.getWriteMode())) {
            throw new JobParamException(StrUtil.format("写入模式不合法：{}", args.getWriteMode()));
        }

        if (StrUtil.isNotBlank(args.getTargetTable()) && !ReUtil.isMatch(TABLE_PATTERN, args.getTargetTable())) {
            throw new JobParamException(StrUtil.format("目标表名格式不正确：{}", args.getTargetTable()));
        }

        log.info("任务参数校验通过，jobName={}，env={}，bizDate={}",
                args.getJobName(), args.getEnv(), args.getBizDate());
    }

    /**
     * 校验必填参数。
     *
     * @param value 参数值
     * @param name 参数名称
     */
    private static void checkRequired(String value, String name) {
        if (StrUtil.isBlank(value)) {
            throw new JobParamException(StrUtil.format("缺少必填参数：{}", name));
        }
    }
}
```

参数异常处理建议如下：

| 建议                       | 说明                                 |
| -------------------------- | ------------------------------------ |
| 启动阶段立即校验           | 不等到业务执行阶段才发现参数错误     |
| 异常消息明确参数名         | 直接说明缺少或错误的参数             |
| 环境、模式、枚举使用白名单 | 避免随意传入非法值                   |
| 参数日志脱敏               | 密码、Token、Secret 不打印           |
| 参数异常不重试             | 参数错误重试通常没有意义，应直接失败 |

### 数据读取异常

数据读取异常通常发生在读取 Hive、HDFS、Kafka、JDBC、对象存储时。常见原因包括路径不存在、表不存在、分区不存在、权限不足、Schema 不匹配、数据库连接失败、Kafka Topic 不存在等。

常见读取异常如下：

| 数据源   | 常见异常                                 |
| -------- | ---------------------------------------- |
| Hive     | 表不存在、分区不存在、Metastore 不可达   |
| HDFS     | 路径不存在、无读取权限、NameNode 不可达  |
| Kafka    | Topic 不存在、Broker 不可达、Offset 过期 |
| JDBC     | 连接失败、账号密码错误、SQL 语法错误     |
| 对象存储 | Endpoint 错误、密钥错误、Bucket 不存在   |

文件位置：`src/main/java/io/github/atengk/spark/exception/DataReadException.java`

该异常用于表示数据读取失败。

```java
package io.github.atengk.spark.exception;

/**
 * 数据读取异常。
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class DataReadException extends RuntimeException {

    /**
     * 创建数据读取异常。
     *
     * @param message 异常消息
     */
    public DataReadException(String message) {
        super(message);
    }

    /**
     * 创建数据读取异常。
     *
     * @param message 异常消息
     * @param cause 原始异常
     */
    public DataReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

读取 Hive 表时建议包装异常并记录上下文。

```java
try {
    Dataset<Row> userDf = spark.table("dwd.dwd_user_detail")
            .filter(col("dt").equalTo(bizDate));
    log.info("Hive 表读取逻辑构建完成，tableName={}，bizDate={}", "dwd.dwd_user_detail", bizDate);
    return userDf;
} catch (Exception e) {
    log.error("读取 Hive 表失败，tableName={}，bizDate={}", "dwd.dwd_user_detail", bizDate, e);
    throw new DataReadException("读取 Hive 表失败", e);
}
```

读取路径前可以先检查路径是否存在。

```java
Configuration hadoopConf = spark.sparkContext().hadoopConfiguration();
String inputPath = "hdfs:///warehouse/dwd/user_event/dt=2026-05-11";

if (!HdfsFileUtil.exists(hadoopConf, inputPath)) {
    throw new DataReadException("输入路径不存在：" + inputPath);
}

Dataset<Row> sourceDf = spark.read().parquet(inputPath);
```

数据读取异常处理建议如下：

| 建议                        | 说明                                     |
| --------------------------- | ---------------------------------------- |
| 日志记录数据源上下文        | 表名、路径、Topic、分区、业务日期        |
| 读取失败保留原始异常        | 包装异常时传入 cause                     |
| 路径类输入可提前检查        | 避免 Spark 执行阶段才失败                |
| Kafka Offset 异常需单独处理 | 判断是否 Offset 被清理或 Checkpoint 损坏 |
| 读取异常一般不吞掉          | 核心输入读取失败应直接失败任务           |

### 数据转换异常

数据转换异常通常发生在字段不存在、类型转换失败、SQL 语法错误、UDF 执行失败、Join 字段类型不一致、窗口函数字段错误等场景。转换异常应明确指出失败阶段和关键字段。

常见转换异常如下：

| 异常场景         | 示例                                |
| ---------------- | ----------------------------------- |
| 字段不存在       | SQL 或 DataFrame 中引用了不存在字段 |
| 类型转换失败     | 字符串转 Decimal、Timestamp 失败    |
| SQL 语法错误     | 动态 SQL 拼接后语法不合法           |
| UDF 异常         | UDF 未处理 null 或非法输入          |
| Join 失败        | Join Key 类型不一致或字段重名       |
| 数据倾斜导致失败 | 单个 Task 数据过大导致 OOM          |

文件位置：`src/main/java/io/github/atengk/spark/exception/DataTransformException.java`

该异常用于表示数据转换失败。

```java
package io.github.atengk.spark.exception;

/**
 * 数据转换异常。
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class DataTransformException extends RuntimeException {

    /**
     * 创建数据转换异常。
     *
     * @param message 异常消息
     */
    public DataTransformException(String message) {
        super(message);
    }

    /**
     * 创建数据转换异常。
     *
     * @param message 异常消息
     * @param cause 原始异常
     */
    public DataTransformException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

转换逻辑包装示例：

```java
try {
    Dataset<Row> resultDf = sourceDf
            .filter(col("user_id").isNotNull())
            .withColumn("event_time", to_timestamp(col("event_time"), "yyyy-MM-dd HH:mm:ss"))
            .withColumn("etl_time", current_timestamp())
            .select("user_id", "event_type", "event_time", "etl_time", "dt");

    log.info("用户事件数据转换完成，bizDate={}", bizDate);
    return resultDf;
} catch (Exception e) {
    log.error("用户事件数据转换失败，bizDate={}", bizDate, e);
    throw new DataTransformException("用户事件数据转换失败", e);
}
```

SQL 转换前建议打印 SQL 文件和参数摘要。

```java
try {
    String sqlTemplate = SqlFileLoader.loadFromClasspath("sql/ads/ads_user_summary.sql");
    String sql = SqlTemplateRenderer.render(sqlTemplate, params);

    log.info("开始执行 Spark SQL，sqlFile={}，bizDate={}", "sql/ads/ads_user_summary.sql", params.get("bizDate"));

    Dataset<Row> resultDf = spark.sql(sql);
    resultDf.explain("formatted");
    return resultDf;
} catch (Exception e) {
    log.error("Spark SQL 执行失败，sqlFile={}，params={}", "sql/ads/ads_user_summary.sql", params, e);
    throw new DataTransformException("Spark SQL 执行失败", e);
}
```

数据转换异常处理建议如下：

| 建议               | 说明                                    |
| ------------------ | --------------------------------------- |
| 转换阶段日志要明确 | 说明是清洗、Join、聚合还是 SQL 执行失败 |
| SQL 文件路径要记录 | 便于定位具体 SQL                        |
| UDF 必须处理 null  | 避免 Executor 中大量 UDF 异常           |
| 类型转换后做校验   | 转换失败产生 null 时应进入质量校验      |
| 字段选择显式声明   | 避免字段变化导致隐性错误                |

### 数据写出异常

数据写出异常通常发生在写 Hive、HDFS、JDBC、Kafka、对象存储时。常见原因包括目标路径无权限、目标表不存在、字段顺序不匹配、分区字段错误、数据库主键冲突、Kafka Broker 不可用、对象存储提交失败等。

文件位置：`src/main/java/io/github/atengk/spark/exception/DataWriteException.java`

该异常用于表示数据写出失败。

```java
package io.github.atengk.spark.exception;

/**
 * 数据写出异常。
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class DataWriteException extends RuntimeException {

    /**
     * 创建数据写出异常。
     *
     * @param message 异常消息
     */
    public DataWriteException(String message) {
        super(message);
    }

    /**
     * 创建数据写出异常。
     *
     * @param message 异常消息
     * @param cause 原始异常
     */
    public DataWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

Hive 写出异常处理示例：

```java
try {
    long outputCount = resultDf.count();
    log.info("准备写入 Hive 表，targetTable={}，bizDate={}，outputCount={}",
            targetTable, bizDate, outputCount);

    if (outputCount == 0 && !allowEmpty) {
        throw new DataWriteException("写出数据为空，禁止写入目标表：" + targetTable);
    }

    resultDf.write()
            .mode(SaveMode.Append)
            .insertInto(targetTable);

    log.info("Hive 表写入完成，targetTable={}，bizDate={}，outputCount={}",
            targetTable, bizDate, outputCount);
} catch (DataWriteException e) {
    throw e;
} catch (Exception e) {
    log.error("Hive 表写入失败，targetTable={}，bizDate={}", targetTable, bizDate, e);
    throw new DataWriteException("Hive 表写入失败", e);
}
```

JDBC 写出异常处理建议先控制分区数，并在异常日志中记录目标表和批次号。

```java
try {
    resultDf.coalesce(8)
            .write()
            .mode(SaveMode.Append)
            .format("jdbc")
            .option("url", jdbcUrl)
            .option("driver", "com.mysql.cj.jdbc.Driver")
            .option("dbtable", targetTable)
            .option("user", username)
            .option("password", password)
            .option("batchsize", "5000")
            .save();

    log.info("JDBC 写入完成，targetTable={}，batchId={}", targetTable, batchId);
} catch (Exception e) {
    log.error("JDBC 写入失败，targetTable={}，batchId={}", targetTable, batchId, e);
    throw new DataWriteException("JDBC 写入失败", e);
}
```

数据写出异常处理建议如下：

| 建议                         | 说明                             |
| ---------------------------- | -------------------------------- |
| 写出前校验数据量             | 防止空数据覆盖正常分区           |
| 覆盖写入必须限定范围         | 只覆盖指定分区或路径             |
| 写 Hive 前校验字段顺序       | `insertInto` 对字段位置敏感      |
| 写 JDBC 前控制分区数         | 防止数据库连接过多               |
| 写 Kafka 必须配置 Checkpoint | 流式写入需要恢复能力             |
| 写出失败应可重跑             | 输出设计必须支持幂等或清理后重跑 |

### 网络异常

网络异常通常发生在 Spark 访问 Kafka、JDBC、Hive Metastore、HDFS NameNode、对象存储、YARN ResourceManager 或其他外部服务时。网络异常可能是短暂抖动，也可能是配置错误、DNS 问题、防火墙策略或服务不可用。

常见网络异常如下：

| 场景           | 示例                                  |
| -------------- | ------------------------------------- |
| Kafka          | Broker 连接超时、Topic 元数据获取失败 |
| JDBC           | 数据库连接超时、连接被重置            |
| Hive Metastore | Thrift 服务不可达                     |
| HDFS           | NameNode RPC 超时                     |
| 对象存储       | Endpoint 无法访问、请求超时           |
| YARN           | ResourceManager 不可达                |

网络重试工具示例。

文件位置：`src/main/java/io/github/atengk/spark/util/RetryUtil.java`

该工具类用于对 Driver 端少量外部操作进行重试，不适合包裹大规模 Spark Action。

```java
package io.github.atengk.spark.util;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * 重试工具类。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class RetryUtil {

    private RetryUtil() {
    }

    /**
     * 执行带重试的操作。
     *
     * @param name 操作名称
     * @param maxRetry 最大重试次数
     * @param sleepMillis 重试间隔毫秒
     * @param supplier 操作函数
     * @param <T> 返回类型
     * @return 执行结果
     */
    public static <T> T retry(String name, int maxRetry, long sleepMillis, Supplier<T> supplier) {
        if (StrUtil.isBlank(name)) {
            throw new IllegalArgumentException("重试操作名称不能为空");
        }
        if (maxRetry < 1) {
            throw new IllegalArgumentException("最大重试次数必须大于 0");
        }

        Exception lastException = null;

        for (int i = 1; i <= maxRetry; i++) {
            try {
                log.info("开始执行操作，name={}，attempt={}/{}", name, i, maxRetry);
                return supplier.get();
            } catch (Exception e) {
                lastException = e;
                log.warn("操作执行失败，name={}，attempt={}/{}", name, i, maxRetry, e);
                if (i < maxRetry) {
                    ThreadUtil.sleep(sleepMillis);
                }
            }
        }

        throw new IllegalStateException(StrUtil.format("操作重试失败，name={}", name), lastException);
    }
}
```

使用示例：

```java
Dataset<Row> configDf = RetryUtil.retry(
        "读取任务配置表",
        3,
        3000,
        () -> spark.read()
                .format("jdbc")
                .option("url", jdbcUrl)
                .option("driver", "com.mysql.cj.jdbc.Driver")
                .option("dbtable", "spark_job_config")
                .option("user", username)
                .option("password", password)
                .load()
);
```

网络异常处理建议如下：

| 建议                           | 说明                                          |
| ------------------------------ | --------------------------------------------- |
| 短暂网络异常可重试             | 适合 Driver 端元数据查询、审计写入            |
| 大规模 Spark Action 不盲目重试 | 可能导致重复计算或重复写入                    |
| 重试次数有限                   | 防止任务长时间卡住                            |
| 日志记录连接目标               | 记录服务类型、地址、表名或 Topic              |
| 区分配置错误和网络抖动         | 账号密码错误、Endpoint 错误不应反复重试       |
| 依赖服务需监控                 | Kafka、Metastore、数据库、HDFS 都应有监控告警 |

### 权限异常

权限异常通常发生在读取或写入 Hive 表、HDFS 路径、对象存储、Kafka Topic、JDBC 表、YARN 队列时。权限异常一般不能通过任务重试解决，需要修复账号授权、路径权限、队列权限或安全认证配置。

常见权限异常如下：

| 类型          | 示例                                  |
| ------------- | ------------------------------------- |
| HDFS 权限     | 无法读取输入目录或写入输出目录        |
| Hive 权限     | 无权查询库表、写入分区、创建表        |
| Kafka 权限    | 无权消费或生产指定 Topic              |
| JDBC 权限     | 数据库账号无查询、写入或建表权限      |
| YARN 权限     | 无权提交到指定队列                    |
| Kerberos 权限 | principal、keytab、krb5.conf 配置错误 |

权限检查命令示例：

```bash
# 检查 HDFS 路径权限
hdfs dfs -ls /warehouse/prod/dwd/user_event

# 测试写权限
hdfs dfs -touchz /warehouse/prod/dwd/user_event/_permission_test
hdfs dfs -rm /warehouse/prod/dwd/user_event/_permission_test

# 查看当前 Kerberos 票据
klist

# 使用 keytab 认证
kinit -kt /etc/security/keytabs/spark-user.keytab spark-user@EXAMPLE.COM
```

权限异常处理建议如下：

| 建议                   | 说明                                 |
| ---------------------- | ------------------------------------ |
| 权限异常不做无意义重试 | 应直接失败并提示缺少权限             |
| 日志记录访问对象       | 表名、路径、Topic、队列              |
| 使用任务专用账号       | 生产任务不使用个人账号               |
| 最小权限授权           | 只授权需要的库表和路径               |
| Keytab 不提交 Git      | 通过安全渠道部署                     |
| 上线前做权限检查       | 读路径、写路径、目标表、队列都要验证 |

### 依赖异常

依赖异常是 Spark 项目中非常常见的问题，通常表现为 `ClassNotFoundException`、`NoClassDefFoundError`、`NoSuchMethodError`、`ClassCastException`、多个 SLF4J Binding、Jackson 版本冲突、Guava 版本冲突等。

常见依赖异常如下：

| 异常                      | 常见原因                                   |
| ------------------------- | ------------------------------------------ |
| `ClassNotFoundException`  | 运行时缺少依赖                             |
| `NoClassDefFoundError`    | 编译期存在，运行期缺失                     |
| `NoSuchMethodError`       | 依赖版本不一致                             |
| `ClassCastException`      | 多版本类加载冲突                           |
| `Multiple SLF4J bindings` | 多个日志实现同时存在                       |
| Scala 版本冲突            | `_2.11`、`_2.12`、`_2.13` 混用             |
| Jackson / Guava 冲突      | Spark、Hadoop、Hive、第三方 SDK 版本不一致 |

排查 Maven 依赖树：

```bash
# 查看完整依赖树
mvn dependency:tree

# 排查 Spark 依赖
mvn dependency:tree -Dincludes=org.apache.spark

# 排查 Jackson 依赖
mvn dependency:tree -Dincludes=com.fasterxml.jackson.core

# 排查 Guava 依赖
mvn dependency:tree -Dincludes=com.google.guava
```

依赖排除示例：

```xml
<!-- 引入外部 SDK 时排除冲突的日志依赖 -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>example-client</artifactId>
    <version>${example.version}</version>
    <exclusions>
        <!-- 排除旧版 SLF4J 绑定 -->
        <exclusion>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-log4j12</artifactId>
        </exclusion>
        <!-- 排除旧版 Log4j -->
        <exclusion>
            <groupId>log4j</groupId>
            <artifactId>log4j</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

依赖异常处理建议如下：

| 建议                           | 说明                                       |
| ------------------------------ | ------------------------------------------ |
| 先确认集群版本                 | Spark、Scala、Hadoop、Hive 版本必须匹配    |
| Spark/Hadoop 依赖通常 provided | 避免与集群运行时冲突                       |
| Kafka/JDBC Connector 按需打包  | 集群未提供时必须随 Jar 提交                |
| 不混用 Scala 后缀              | 所有 Spark 依赖使用相同 `_2.12` 或 `_2.13` |
| 使用依赖树定位冲突来源         | 不盲目升级                                 |
| 打包后验证 Jar 内容            | 确认必要依赖是否进入 Fat Jar               |

### 任务失败恢复

任务失败恢复用于保证 Spark 任务失败后可以安全重跑、补数或从断点恢复。不同任务类型恢复方式不同：离线批处理通常通过覆盖指定分区重跑；增量任务需要依赖批次号或水位线；流任务依赖 Checkpoint 和下游幂等写入。

常见恢复策略如下：

| 任务类型      | 恢复方式                          |
| ------------- | --------------------------------- |
| 离线批处理    | 清理或覆盖指定分区后重跑          |
| 增量任务      | 使用相同时间范围或批次号重跑      |
| JDBC 写入任务 | 临时表中转，正式表按 batchId 合并 |
| Kafka 流任务  | 保留 Checkpoint 后重启            |
| 状态流任务    | 使用原 Checkpoint 恢复状态        |
| 补数任务      | 按日期范围逐日重跑                |

离线任务重跑建议流程：

```text
确认失败原因
        ↓
确认目标分区是否已部分写入
        ↓
清理目标分区或使用 INSERT OVERWRITE
        ↓
使用相同 bizDate 和 batchId 重跑
        ↓
校验输入量、输出量和目标分区
        ↓
记录审计状态
```

流任务恢复建议流程：

```text
确认 Checkpoint 是否存在
        ↓
确认 Kafka Offset 是否仍在保留期内
        ↓
使用原 Checkpoint 重启任务
        ↓
观察 recentProgress 和 Kafka Lag
        ↓
确认下游是否重复写入
        ↓
必要时按业务主键去重
```

任务失败恢复建议如下：

| 建议                        | 说明                                 |
| --------------------------- | ------------------------------------ |
| 任务必须可重跑              | 离线任务输出按分区覆盖更容易恢复     |
| 输出写入要幂等              | 使用分区覆盖、主键去重、batchId 控制 |
| 失败后先查目标端            | 判断是否部分写入                     |
| 流任务不随意删除 Checkpoint | 删除后可能重复消费或丢失状态         |
| 审计记录失败原因            | 便于后续追踪和告警                   |
| 补数任务与日常任务隔离      | 防止补数影响正常调度                 |

## 测试体系

本章节用于规范 Spark Java 项目的测试体系。Spark 项目测试不应只依赖手工提交集群任务，而应通过单元测试、本地集成测试、样例数据测试、SQL 结果测试、边界值测试、异常测试、性能测试和回归测试，尽早发现逻辑错误、字段错误、SQL 错误和数据质量问题。

### 单元测试

单元测试用于验证不依赖 Spark 集群的基础逻辑，例如参数解析、日期处理、路径构建、SQL 模板渲染、参数校验、脱敏工具、异常类型等。单元测试应执行快、依赖少、结果稳定。

Maven 测试依赖示例：

```xml
<!-- JUnit 5：Java 单元测试框架 -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.1</version>
    <scope>test</scope>
</dependency>

<!-- AssertJ：提供可读性更强的断言能力 -->
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <version>3.25.1</version>
    <scope>test</scope>
</dependency>
```

文件位置：`src/test/java/io/github/atengk/spark/param/TimeParamUtilTest.java`

该测试类用于验证业务日期参数处理逻辑。

```java
package io.github.atengk.spark.param;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 时间参数工具测试。
 *
 * @author Ateng
 * @since 2026-05-11
 */
class TimeParamUtilTest {

    /**
     * 测试合法业务日期。
     */
    @Test
    void shouldValidateBizDateSuccess() {
        TimeParamUtil.validateBizDate("2026-05-11");
    }

    /**
     * 测试非法业务日期。
     */
    @Test
    void shouldThrowExceptionWhenBizDateInvalid() {
        assertThrows(IllegalArgumentException.class, () -> TimeParamUtil.validateBizDate("20260511"));
    }

    /**
     * 测试获取上一天。
     */
    @Test
    void shouldReturnPreviousDay() {
        String previousDay = TimeParamUtil.previousDay("2026-05-11");
        assertThat(previousDay).isEqualTo("2026-05-10");
    }
}
```

单元测试建议如下：

| 建议                 | 说明                              |
| -------------------- | --------------------------------- |
| 工具类必须有单元测试 | 日期、路径、SQL、参数校验优先覆盖 |
| 测试名称表达预期     | 使用 should... 风格清晰描述       |
| 不依赖外部服务       | 单元测试不访问 Hive、Kafka、JDBC  |
| 覆盖异常分支         | 不只测试成功场景                  |
| 测试执行要快         | 适合在 CI 中每次提交执行          |

### 本地集成测试

本地集成测试用于在 `local` 模式下创建 SparkSession，验证 DataFrame 转换、SQL 执行、Reader/Transformer/Writer 组合等逻辑。它不依赖真实集群，但可以验证 Spark API 和业务转换是否正确。

文件位置：`src/test/java/io/github/atengk/spark/test/SparkLocalTestBase.java`

该基类用于创建本地 SparkSession。

```java
package io.github.atengk.spark.test;

import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * Spark 本地测试基类。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public abstract class SparkLocalTestBase {

    protected static SparkSession spark;

    /**
     * 初始化本地 SparkSession。
     */
    @BeforeAll
    static void beforeAll() {
        spark = SparkSession.builder()
                .appName("spark-local-test")
                .master("local[2]")
                .config("spark.ui.enabled", "false")
                .config("spark.sql.shuffle.partitions", "2")
                .getOrCreate();

        log.info("本地 SparkSession 初始化完成");
    }

    /**
     * 释放本地 SparkSession。
     */
    @AfterAll
    static void afterAll() {
        if (spark != null) {
            spark.stop();
            log.info("本地 SparkSession 已释放");
        }
    }
}
```

Transformer 集成测试示例：

```java
package io.github.atengk.spark.transformer;

import io.github.atengk.spark.test.SparkLocalTestBase;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户汇总转换器测试。
 *
 * @author Ateng
 * @since 2026-05-11
 */
class UserSummaryTransformerTest extends SparkLocalTestBase {

    /**
     * 测试用户汇总数据转换。
     */
    @Test
    void shouldTransformUserSummary() {
        Dataset<Row> sourceDf = spark.createDataFrame(
                Arrays.asList(
                        new UserSummaryTestRow("1001", "张三", null, 2L, "99.90", "2026-05-11"),
                        new UserSummaryTestRow("1002", "李四", "VIP", 1L, "199.00", "2026-05-11")
                ),
                UserSummaryTestRow.class
        );

        UserSummaryTransformer transformer = new UserSummaryTransformer();
        Dataset<Row> resultDf = transformer.transform(sourceDf);

        assertThat(resultDf.count()).isEqualTo(2);
        assertThat(Arrays.asList(resultDf.columns())).contains("user_id", "user_level", "etl_time", "dt");
    }
}
```

本地集成测试建议如下：

| 建议                    | 说明                                               |
| ----------------------- | -------------------------------------------------- |
| 使用 `local[2]`         | 能发现部分并行执行问题                             |
| 降低 Shuffle 分区       | 测试中设置为 2 即可                                |
| 关闭 Spark UI           | 避免测试环境端口冲突                               |
| 测试数据量要小          | 聚焦逻辑正确性                                     |
| 测试后释放 SparkSession | 避免资源泄漏                                       |
| 不连接生产资源          | 本地测试只使用内存数据或 `src/test/resources` 数据 |

### 数据样例测试

数据样例测试用于通过固定输入样例验证清洗、转换和写出结果。样例数据应覆盖正常数据、空值、重复数据、异常数据、边界数据和多分区数据。

推荐测试目录结构：

```text
src/test/resources
├── data
│   ├── input
│   │   ├── user_event.json
│   │   ├── order_detail.csv
│   │   └── user_dim.json
│   ├── expected
│   │   └── user_summary.json
│   └── invalid
│       └── user_event_invalid.json
└── sql
    └── ads_user_summary.sql
```

样例数据示例：

```json
{"user_id":"1001","event_type":"click","event_time":"2026-05-11 10:00:00","dt":"2026-05-11"}
{"user_id":"1002","event_type":"pay","event_time":"2026-05-11 10:05:00","dt":"2026-05-11"}
{"user_id":null,"event_type":"click","event_time":"2026-05-11 10:10:00","dt":"2026-05-11"}
```

样例测试示例：

```java
package io.github.atengk.spark.sample;

import io.github.atengk.spark.test.SparkLocalTestBase;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.apache.spark.sql.functions.col;

/**
 * 用户事件样例数据测试。
 *
 * @author Ateng
 * @since 2026-05-11
 */
class UserEventSampleTest extends SparkLocalTestBase {

    /**
     * 测试用户事件样例清洗。
     */
    @Test
    void shouldCleanUserEventSample() {
        Dataset<Row> sourceDf = spark.read()
                .json("src/test/resources/data/input/user_event.json");

        Dataset<Row> cleanedDf = sourceDf
                .filter(col("user_id").isNotNull())
                .select("user_id", "event_type", "event_time", "dt");

        assertThat(cleanedDf.count()).isEqualTo(2);
    }
}
```

数据样例测试建议如下：

| 建议                       | 说明                           |
| -------------------------- | ------------------------------ |
| 样例数据小而完整           | 不追求大，追求覆盖关键场景     |
| 输入和期望结果分开         | 便于自动比对                   |
| 覆盖脏数据                 | 空值、非法格式、重复数据都要有 |
| 样例文件提交 Git           | 保证回归测试稳定               |
| 样例字段贴近生产           | 不使用过度简化字段             |
| 修改逻辑时同步更新期望结果 | 防止测试失真                   |

### SQL 结果测试

SQL 结果测试用于验证外部 SQL 文件执行结果是否符合预期。适合指标计算、宽表构建、窗口函数、复杂 Join 和动态 SQL 模板。

SQL 文件示例：

```sql
-- src/test/resources/sql/ads_user_summary.sql
SELECT
    user_id,
    COUNT(event_type) AS event_count,
    '${bizDate}' AS dt
FROM tmp_user_event
WHERE dt = '${bizDate}'
GROUP BY user_id
```

SQL 测试示例：

```java
package io.github.atengk.spark.sql;

import io.github.atengk.spark.test.SparkLocalTestBase;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户汇总 SQL 测试。
 *
 * @author Ateng
 * @since 2026-05-11
 */
class UserSummarySqlTest extends SparkLocalTestBase {

    /**
     * 测试用户汇总 SQL 结果。
     */
    @Test
    void shouldExecuteUserSummarySql() {
        Dataset<Row> sourceDf = spark.read()
                .json("src/test/resources/data/input/user_event.json");

        sourceDf.createOrReplaceTempView("tmp_user_event");

        String sqlTemplate = SqlFileLoader.loadFromClasspath("sql/ads_user_summary.sql");

        Map<String, String> params = new HashMap<>();
        params.put("bizDate", "2026-05-11");

        String sql = SqlTemplateRenderer.render(sqlTemplate, params);
        Dataset<Row> resultDf = spark.sql(sql);

        assertThat(resultDf.count()).isEqualTo(2);
        assertThat(Arrays.asList(resultDf.columns())).contains("user_id", "event_count", "dt");
    }
}
```

SQL 结果测试建议如下：

| 建议                | 说明                            |
| ------------------- | ------------------------------- |
| SQL 文件外置测试    | 不只测试 Java 拼接结果          |
| 动态参数必须覆盖    | bizDate、库名、表名等参数要测试 |
| 验证字段和值        | 不只验证 count                  |
| 复杂 SQL 使用样例表 | 注册临时视图模拟 Hive 表        |
| 执行计划可辅助排查  | 失败时可打印 `explain`          |
| 指标 SQL 要验证口径 | count、sum、distinct 结果要断言 |

### 边界值测试

边界值测试用于验证特殊输入条件下任务是否稳定，例如空数据、单条数据、全部重复数据、字段全为空、极大金额、极早或极晚时间、跨天数据、多分区数据等。

常见边界场景如下：

| 场景              | 说明                         |
| ----------------- | ---------------------------- |
| 空数据集          | 输入为空时任务是否失败或跳过 |
| 单条数据          | 聚合、Join、写出是否正常     |
| 全部重复          | 去重结果是否正确             |
| 必填字段全为空    | 是否进入异常处理             |
| 金额为 0 或超大值 | Decimal 精度是否正确         |
| 时间跨天          | 分区日期是否生成正确         |
| 维表缺失          | Join 后是否统计未匹配数据    |
| 多分区输入        | 是否只处理目标业务日期       |

空数据测试示例：

```java
package io.github.atengk.spark.boundary;

import io.github.atengk.spark.quality.DataQualityChecker;
import io.github.atengk.spark.test.SparkLocalTestBase;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 数据边界值测试。
 *
 * @author Ateng
 * @since 2026-05-11
 */
class DataBoundaryTest extends SparkLocalTestBase {

    /**
     * 测试空数据集校验失败。
     */
    @Test
    void shouldThrowExceptionWhenDatasetEmpty() {
        StructType schema = new StructType()
                .add("user_id", DataTypes.StringType)
                .add("dt", DataTypes.StringType);

        Dataset<Row> emptyDf = spark.createDataFrame(Collections.emptyList(), schema);

        assertThrows(IllegalStateException.class,
                () -> DataQualityChecker.checkNotEmpty(emptyDf, "空用户数据"));
    }
}
```

边界值测试建议如下：

| 建议                 | 说明                     |
| -------------------- | ------------------------ |
| 空数据必须覆盖       | 决定任务失败还是跳过     |
| 主键重复必须覆盖     | 验证唯一性校验           |
| 时间边界必须覆盖     | 跨天、月底、年底、闰年   |
| 金额精度必须覆盖     | Decimal 精度和舍入规则   |
| 分区字段异常必须覆盖 | null、空字符串、非法日期 |
| Join 缺失必须覆盖    | 维表缺失数据是否符合预期 |

### 异常场景测试

异常场景测试用于验证任务在参数错误、数据质量失败、SQL 参数缺失、写入数据为空、路径非法等情况下能否抛出明确异常。异常测试的目标是让失败可预期，而不是出现模糊的底层异常。

参数异常测试示例：

```java
package io.github.atengk.spark.exception;

import io.github.atengk.spark.config.JobRuntimeArgs;
import io.github.atengk.spark.param.JobParamChecker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 任务异常场景测试。
 *
 * @author Ateng
 * @since 2026-05-11
 */
class JobExceptionTest {

    /**
     * 测试缺少任务名称时抛出异常。
     */
    @Test
    void shouldThrowExceptionWhenJobNameMissing() {
        JobRuntimeArgs args = new JobRuntimeArgs();
        args.setEnv("prod");
        args.setBizDate("2026-05-11");

        assertThrows(JobParamException.class, () -> JobParamChecker.check(args));
    }

    /**
     * 测试业务日期格式错误时抛出异常。
     */
    @Test
    void shouldThrowExceptionWhenBizDateInvalid() {
        JobRuntimeArgs args = new JobRuntimeArgs();
        args.setJobName("user-summary");
        args.setEnv("prod");
        args.setBizDate("20260511");

        assertThrows(JobParamException.class, () -> JobParamChecker.check(args));
    }
}
```

SQL 参数缺失测试示例：

```java
@Test
void shouldThrowExceptionWhenSqlParamMissing() {
    String sqlTemplate = "SELECT * FROM dwd_user WHERE dt = '${bizDate}'";

    Map<String, String> params = new HashMap<>();

    assertThrows(IllegalArgumentException.class,
            () -> SqlTemplateRenderer.render(sqlTemplate, params));
}
```

异常场景测试建议如下：

| 建议                 | 说明                       |
| -------------------- | -------------------------- |
| 参数异常必须测试     | 缺失、格式错误、非法枚举   |
| SQL 模板异常必须测试 | 参数缺失、非法表名         |
| 数据质量异常必须测试 | 空数据、重复主键、必填为空 |
| 写出保护必须测试     | 空结果禁止覆盖             |
| 异常类型要明确       | 不只断言 RuntimeException  |
| 异常消息要可读       | 便于调度平台和日志排查     |

### 性能测试

性能测试用于验证 Spark 任务在一定数据规模下的耗时、资源消耗、Shuffle 情况、输出文件数量和稳定性。性能测试不一定在单元测试阶段执行，通常在测试集群或压测环境中通过固定数据集执行。

性能测试关注指标如下：

| 指标                 | 说明                       |
| -------------------- | -------------------------- |
| 输入数据量           | 读取记录数、文件大小       |
| 输出数据量           | 写出记录数、文件大小       |
| 总耗时               | 任务从启动到结束的时间     |
| Stage 耗时           | 关键 Stage 耗时            |
| Shuffle Read / Write | Shuffle 数据规模           |
| Executor OOM         | 是否出现内存溢出           |
| 数据倾斜             | 是否存在个别 Task 明显过慢 |
| 小文件数量           | 输出文件是否过多           |

性能测试提交示例：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --queue root.test.etl \
  --driver-memory 2g \
  --executor-memory 4g \
  --executor-cores 2 \
  --num-executors 6 \
  --conf spark.sql.shuffle.partitions=300 \
  --conf spark.sql.adaptive.enabled=true \
  target/spark-job.jar \
  --jobName user-summary \
  --env test \
  --bizDate 2026-05-11 \
  --batchId perf-202605110001
```

性能测试建议如下：

| 建议                   | 说明                              |
| ---------------------- | --------------------------------- |
| 使用接近生产的数据规模 | 小样例无法反映真实 Shuffle 和倾斜 |
| 记录资源参数           | Executor 数量、内存、核数、分区数 |
| 保留 Spark UI 结果     | 用于对比优化前后差异              |
| 关注最慢 Stage         | 优化应从瓶颈 Stage 开始           |
| 分析数据倾斜           | 查看 Task Duration 是否差异过大   |
| 输出文件数量要检查     | 防止性能通过但产生大量小文件      |
| 性能结论要可复现       | 固定数据日期、参数和代码版本      |

性能测试结果记录建议：

```text
任务名称：user-summary
业务日期：2026-05-11
输入数据量：1,200,000,000
输出数据量：8,000,000
Executor：6
Executor Memory：4g
Executor Cores：2
Shuffle Partitions：300
总耗时：18 分 35 秒
最大 Stage 耗时：7 分 10 秒
主要瓶颈：订单明细与用户维表 Join
优化建议：广播用户维表，Join 前裁剪字段
```

### 回归测试

回归测试用于保证代码修改后，历史核心逻辑和历史样例结果不被破坏。Spark 项目中，回归测试尤其适合 SQL 指标、清洗规则、字段映射、宽表构建和数据质量校验。

回归测试范围如下：

| 范围     | 说明                         |
| -------- | ---------------------------- |
| 参数解析 | 新增参数不影响旧参数         |
| 清洗规则 | 空值、去重、异常过滤结果稳定 |
| SQL 指标 | 指标值与期望结果一致         |
| 字段结构 | 输出字段名称、类型、顺序不变 |
| 分区写入 | 分区字段和路径不变           |
| 异常处理 | 错误输入仍能抛出明确异常     |
| 数据质量 | 核心质量校验仍生效           |

回归测试建议使用固定输入和固定期望输出。

```java
package io.github.atengk.spark.regression;

import io.github.atengk.spark.test.SparkLocalTestBase;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户汇总回归测试。
 *
 * @author Ateng
 * @since 2026-05-11
 */
class UserSummaryRegressionTest extends SparkLocalTestBase {

    /**
     * 测试用户汇总结果与期望结果一致。
     */
    @Test
    void shouldMatchExpectedUserSummary() {
        Dataset<Row> actualDf = spark.read()
                .json("src/test/resources/data/actual/user_summary.json")
                .select("user_id", "event_count", "dt")
                .orderBy("user_id");

        Dataset<Row> expectedDf = spark.read()
                .json("src/test/resources/data/expected/user_summary.json")
                .select("user_id", "event_count", "dt")
                .orderBy("user_id");

        List<String> actualList = actualDf.toJSON().collectAsList()
                .stream()
                .collect(Collectors.toList());

        List<String> expectedList = expectedDf.toJSON().collectAsList()
                .stream()
                .collect(Collectors.toList());

        assertThat(actualList).containsExactlyElementsOf(expectedList);
    }
}
```

回归测试建议如下：

| 建议                   | 说明                                    |
| ---------------------- | --------------------------------------- |
| 核心 SQL 必须回归      | 指标 SQL、宽表 SQL、清洗 SQL 优先覆盖   |
| 期望结果固定保存       | 放在 `src/test/resources/data/expected` |
| 字段结构也要断言       | 防止字段名和顺序被误改                  |
| 修改口径需同步更新测试 | 并在提交说明中写清楚原因                |
| CI 中自动执行          | 每次合并前执行核心回归测试              |
| 大规模回归放测试集群   | 本地只做小样例回归                      |

推荐测试分层如下：

| 测试类型     | 执行位置        | 执行频率         |
| ------------ | --------------- | ---------------- |
| 单元测试     | 本地 / CI       | 每次提交         |
| 本地集成测试 | 本地 / CI       | 每次提交或合并   |
| SQL 结果测试 | 本地 / CI       | 每次提交         |
| 异常场景测试 | 本地 / CI       | 每次提交         |
| 性能测试     | 测试集群        | 重要变更或上线前 |
| 回归测试     | 本地 + 测试集群 | 合并和发布前     |


## 打包与构建

本章节用于说明 Spark Java 项目的构建、打包、依赖排除、Fat Jar 生成、配置文件处理、构建脚本和版本号管理。Spark 项目打包的核心问题不是简单生成 Jar，而是要明确哪些依赖由集群提供、哪些依赖需要随应用发布、哪些配置文件应该进入包内、哪些配置应该由运行环境外部注入。

### Maven 打包

Maven 是 Spark Java 项目中最常用的构建工具。Maven 打包通常通过 `mvn clean package` 完成，生成的 Jar 位于 `target/` 目录下。普通 Jar 只包含当前项目编译后的 class 和 resources，不包含第三方依赖；如果需要提交到集群运行，通常还需要使用 Shade 插件构建 Fat Jar。

基础 Maven 打包命令如下：

```bash
# 清理历史构建产物并重新打包
mvn clean package

# 跳过测试打包，适合本地快速验证，不建议生产发布直接使用
mvn clean package -DskipTests

# 执行指定环境 profile 打包
mvn clean package -Pprod
```

基础 `pom.xml` 打包配置如下：

```xml
<project>
    <properties>
        <!-- Java 编译版本需与 Spark 集群运行 JDK 保持一致 -->
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <!-- 统一组件版本 -->
        <spark.version>3.5.0</spark.version>
        <hadoop.version>3.3.6</hadoop.version>
        <hutool.version>5.8.26</hutool.version>
    </properties>

    <build>
        <finalName>spark-job</finalName>

        <plugins>
            <!-- Maven 编译插件：控制源码和目标字节码版本 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>${maven.compiler.source}</source>
                    <target>${maven.compiler.target}</target>
                    <encoding>${project.build.sourceEncoding}</encoding>
                </configuration>
            </plugin>

            <!-- Maven 测试插件：执行 JUnit 5 测试 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
            </plugin>
        </plugins>
    </build>
</project>
```

Maven 打包建议如下：

| 建议                | 说明                                        |
| ------------------- | ------------------------------------------- |
| 发布前必须执行测试  | 不建议生产发布时跳过测试                    |
| 使用统一 Maven 版本 | 团队内可通过 Maven Wrapper 固定版本         |
| 依赖版本集中管理    | Spark、Hadoop、Hive、Kafka 版本不要散落声明 |
| 构建产物名称固定    | 例如 `spark-job.jar`，便于脚本提交          |
| 使用公司 Maven 私服 | 保证依赖下载稳定和版本可控                  |

### Gradle 打包

Gradle 适合多模块项目、复杂构建脚本和需要更快构建速度的项目。Spark 项目使用 Gradle 时，建议通过 Gradle Wrapper 固定 Gradle 版本，避免不同开发人员本地版本不一致。

Gradle 常用构建命令如下：

```bash
# 使用 Gradle Wrapper 打包
./gradlew clean build

# 跳过测试打包
./gradlew clean build -x test

# 构建 Shadow Fat Jar
./gradlew clean shadowJar
```

基础 `build.gradle` 配置如下：

```groovy
plugins {
    // Java 项目插件
    id 'java'

    // 构建 Fat Jar 的 Shadow 插件
    id 'com.github.johnrengelman.shadow' version '8.1.1'
}

group = 'io.github.atengk'
version = '1.0.0'

java {
    // Java 版本需与集群运行环境一致
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    // 生产项目建议优先配置公司 Maven 私服
    mavenCentral()
}

ext {
    sparkVersion = '3.5.0'
    hutoolVersion = '5.8.26'
    lombokVersion = '1.18.30'
}

dependencies {
    // Spark SQL：集群运行时通常已提供，避免打入应用 Jar
    compileOnly "org.apache.spark:spark-sql_2.12:${sparkVersion}"

    // Hutool：通用工具类，需要随应用一起打包
    implementation "cn.hutool:hutool-all:${hutoolVersion}"

    // Lombok：编译期使用，不进入运行包
    compileOnly "org.projectlombok:lombok:${lombokVersion}"
    annotationProcessor "org.projectlombok:lombok:${lombokVersion}"

    // JUnit 5：单元测试框架
    testImplementation "org.junit.jupiter:junit-jupiter:5.10.1"
}

test {
    useJUnitPlatform()
}

shadowJar {
    // 生成 spark-job-all.jar
    archiveBaseName.set('spark-job')
    archiveClassifier.set('all')
    archiveVersion.set(project.version.toString())
}
```

Gradle 打包建议如下：

| 建议                          | 说明                         |
| ----------------------------- | ---------------------------- |
| 优先使用 `./gradlew`          | 保证构建版本一致             |
| Spark 依赖使用 `compileOnly`  | 避免与集群 Spark 依赖冲突    |
| 业务依赖使用 `implementation` | 需要进入运行包的依赖明确声明 |
| Fat Jar 使用 Shadow 插件      | 统一生成可提交 Jar           |
| CI 中执行 `clean build`       | 保证测试和编译都通过         |

### Shade 打包

Shade 打包用于将项目 class 和第三方依赖合并到一个 Fat Jar 中，便于通过 `spark-submit` 提交。Spark 项目中最常用的是 Maven Shade Plugin。Shade 打包时必须谨慎排除 Spark、Hadoop、Hive、Scala 等集群已提供依赖，避免运行时冲突。

Maven Shade Plugin 配置如下：

```xml
<build>
    <finalName>spark-job</finalName>

    <plugins>
        <!-- Shade 插件：构建 Spark 提交使用的 Fat Jar -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.5.1</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                    <configuration>
                        <!-- 生成 dependency-reduced-pom.xml 可能影响多模块构建，通常关闭 -->
                        <createDependencyReducedPom>false</createDependencyReducedPom>

                        <!-- 排除签名文件，避免 Jar 校验失败 -->
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

                        <!-- 指定主类，部分场景可用于 java -jar，本项目主要通过 spark-submit --class 指定 -->
                        <transformers>
                            <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                <mainClass>io.github.atengk.spark.Application</mainClass>
                            </transformer>
                        </transformers>

                        <!-- 排除集群已提供依赖，避免运行时冲突 -->
                        <artifactSet>
                            <excludes>
                                <exclude>org.apache.spark:*</exclude>
                                <exclude>org.apache.hadoop:*</exclude>
                                <exclude>org.scala-lang:*</exclude>
                            </excludes>
                        </artifactSet>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

打包命令：

```bash
# 构建 Fat Jar
mvn clean package

# 查看构建产物
ls -lh target/

# 查看 Jar 中是否包含指定依赖
jar tf target/spark-job.jar | grep hutool
```

Shade 打包建议如下：

| 建议                           | 说明                                         |
| ------------------------------ | -------------------------------------------- |
| Spark 依赖不要打入 Fat Jar     | 集群运行时已提供                             |
| Hadoop 依赖通常不打入 Fat Jar  | 避免与集群 Hadoop 版本冲突                   |
| JDBC 驱动通常需要打入 Fat Jar  | 除非通过 `--jars` 外部提交                   |
| Kafka Connector 按集群情况决定 | 集群未提供时需要打入或通过 `--packages` 提交 |
| 排除签名文件                   | 避免合并 Jar 后安全签名失效                  |
| 打包后检查 Jar 内容            | 确认必要依赖存在、集群依赖未误打入           |

### 依赖排除

依赖排除用于解决 Spark 项目中的依赖冲突问题。常见需要排除的依赖包括 Spark、Hadoop、Scala、旧版 Log4j、重复 SLF4J Binding、Guava、Jackson 等。

查看依赖树：

```bash
# 查看完整依赖树
mvn dependency:tree

# 查看 Spark 依赖来源
mvn dependency:tree -Dincludes=org.apache.spark

# 查看日志依赖来源
mvn dependency:tree -Dincludes=org.slf4j

# 查看 Jackson 依赖来源
mvn dependency:tree -Dincludes=com.fasterxml.jackson.core
```

排除外部 SDK 中的冲突依赖：

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>example-client</artifactId>
    <version>${example.version}</version>
    <exclusions>
        <!-- 排除旧版 SLF4J 绑定，避免多个日志实现冲突 -->
        <exclusion>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-log4j12</artifactId>
        </exclusion>

        <!-- 排除旧版 Log4j，避免与集群日志体系冲突 -->
        <exclusion>
            <groupId>log4j</groupId>
            <artifactId>log4j</artifactId>
        </exclusion>

        <!-- 排除冲突 Guava，实际是否排除需结合依赖树判断 -->
        <exclusion>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

Spark 相关依赖作用域建议：

```xml
<!-- Spark SQL：由集群提供，使用 provided -->
<dependency>
    <groupId>org.apache.spark</groupId>
    <artifactId>spark-sql_2.12</artifactId>
    <version>${spark.version}</version>
    <scope>provided</scope>
</dependency>

<!-- Spark Hive：由集群提供，使用 provided -->
<dependency>
    <groupId>org.apache.spark</groupId>
    <artifactId>spark-hive_2.12</artifactId>
    <version>${spark.version}</version>
    <scope>provided</scope>
</dependency>

<!-- Hutool：业务工具依赖，需要进入 Fat Jar -->
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>${hutool.version}</version>
</dependency>
```

依赖排除建议如下：

| 建议                    | 说明                                         |
| ----------------------- | -------------------------------------------- |
| 先看依赖树再排除        | 不要凭经验盲目排除                           |
| 集群依赖使用 `provided` | Spark、Hadoop、Hive、Scala 通常由集群提供    |
| 业务依赖进入 Fat Jar    | Hutool、JDBC 驱动、业务 SDK 等需要随应用发布 |
| 日志实现只保留一个      | 避免多个 SLF4J Binding                       |
| Scala 后缀必须统一      | Spark 依赖统一使用 `_2.12` 或 `_2.13`        |
| 排除后做集群验证        | 本地通过不代表集群运行通过                   |

### Fat Jar 构建

Fat Jar 是包含项目代码和必要第三方依赖的可运行 Jar。Spark 任务提交时，Fat Jar 能减少依赖分发复杂度，但不能无脑把所有依赖都打进去。

推荐 Fat Jar 包含内容：

| 类型              | 是否包含 | 说明                    |
| ----------------- | -------- | ----------------------- |
| 项目业务代码      | 是       | 当前项目 class          |
| 项目 resources    | 是       | SQL、默认配置、日志配置 |
| Hutool 等工具依赖 | 是       | 业务运行需要            |
| JDBC 驱动         | 通常是   | 除非通过 `--jars` 提交  |
| Kafka Connector   | 视情况   | 集群无该依赖时包含      |
| Spark 依赖        | 否       | 集群提供                |
| Hadoop 依赖       | 通常否   | 集群提供                |
| Hive 依赖         | 通常否   | 集群提供                |
| Scala 标准库      | 否       | Spark 集群提供          |

构建 Fat Jar：

```bash
# Maven 构建
mvn clean package

# 查看 Jar 大小
ls -lh target/spark-job.jar

# 查看 Jar 内容
jar tf target/spark-job.jar | head

# 检查是否误打入 Spark 依赖
jar tf target/spark-job.jar | grep 'org/apache/spark' | head
```

Fat Jar 构建建议如下：

| 建议                   | 说明                            |
| ---------------------- | ------------------------------- |
| Jar 不宜过大           | 过大会增加上传和分发耗时        |
| 必要依赖必须包含       | 缺少 JDBC 驱动会导致运行时报错  |
| 集群依赖不要包含       | 避免版本冲突                    |
| 发布前检查 Jar 内容    | 确认依赖范围正确                |
| 构建产物统一命名       | 例如 `spark-job-${version}.jar` |
| Fat Jar 和源码版本绑定 | 便于回滚和问题追踪              |

### 配置文件打包

配置文件打包需要区分默认配置、环境配置、敏感配置和运行时外部配置。项目可以将通用配置、SQL 文件、日志配置打入 Jar，但生产敏感配置不应进入 Jar。

推荐打入 Jar 的资源：

| 文件                    | 是否建议打包 | 说明                        |
| ----------------------- | ------------ | --------------------------- |
| `application.yml`       | 是           | 通用默认配置                |
| `application-local.yml` | 可选         | 本地调试配置                |
| `logback.xml`           | 是           | 默认日志配置                |
| `sql/*.sql`             | 是           | SQL 文件随代码版本发布      |
| `hive-site.xml`         | 视情况       | 通常由集群或 `--files` 提供 |
| 生产密码配置            | 否           | 必须外部注入                |
| Keytab                  | 否           | 禁止打入 Jar                |

Maven resources 配置示例：

```xml
<build>
    <resources>
        <resource>
            <!-- 打包 src/main/resources 下的配置和 SQL 文件 -->
            <directory>src/main/resources</directory>
            <includes>
                <include>application.yml</include>
                <include>application-*.yml</include>
                <include>logback.xml</include>
                <include>sql/**/*.sql</include>
            </includes>
            <filtering>false</filtering>
        </resource>
    </resources>
</build>
```

提交时外部传入配置文件：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --files /opt/app/spark-job/conf/application-prod.yml,/opt/app/spark-job/conf/logback.xml,/opt/module/hive/conf/hive-site.xml \
  --conf "spark.driver.extraJavaOptions=-Dlogback.configurationFile=logback.xml" \
  target/spark-job.jar \
  --jobName user-summary \
  --env prod \
  --bizDate 2026-05-11
```

配置文件打包建议如下：

| 建议                | 说明                         |
| ------------------- | ---------------------------- |
| SQL 文件随 Jar 发布 | 保证 SQL 与代码版本一致      |
| 通用配置可打入 Jar  | 作为默认值                   |
| 生产配置优先外部化  | 便于运维调整和安全管理       |
| 敏感配置禁止打包    | 密码、Token、Keytab 不进 Jar |
| 外部配置优先级更高  | 运行时参数覆盖默认配置       |
| 发布包记录配置版本  | 方便回滚时恢复配置           |

### 构建脚本

构建脚本用于统一本地、测试和生产构建流程，减少人工命令差异。脚本应包含清理、测试、打包、产物检查、版本输出等步骤。

文件位置：`scripts/build.sh`

该脚本用于执行 Maven 构建并检查构建产物。

```bash
#!/usr/bin/env bash

# scripts/build.sh
# Spark 项目 Maven 构建脚本

set -e

PROJECT_DIR=$(cd "$(dirname "$0")/.." && pwd)
cd "${PROJECT_DIR}"

VERSION=${1:-"1.0.0"}
PROFILE=${2:-"prod"}

echo "开始构建 Spark 项目"
echo "项目目录：${PROJECT_DIR}"
echo "构建版本：${VERSION}"
echo "构建环境：${PROFILE}"

mvn clean package -P"${PROFILE}" -Drevision="${VERSION}"

JAR_FILE="${PROJECT_DIR}/target/spark-job.jar"

if [ ! -f "${JAR_FILE}" ]; then
  echo "构建失败，未找到 Jar：${JAR_FILE}"
  exit 1
fi

echo "构建完成：${JAR_FILE}"
ls -lh "${JAR_FILE}"

echo "检查 Jar 内容"
jar tf "${JAR_FILE}" | grep "io/github/atengk/spark/Application.class" >/dev/null

echo "构建产物校验通过"
```

脚本授权和执行：

```bash
# 增加执行权限
chmod +x scripts/build.sh

# 构建指定版本
./scripts/build.sh 1.0.0 prod
```

构建脚本建议如下：

| 建议                 | 说明                         |
| -------------------- | ---------------------------- |
| 脚本放入版本库       | 保证团队构建方式一致         |
| 使用 `set -e`        | 任意步骤失败立即退出         |
| 检查 Jar 是否生成    | 避免发布空产物               |
| 打印版本和环境       | 便于构建日志追踪             |
| CI 使用同一脚本      | 本地和流水线构建一致         |
| 不在脚本中写敏感信息 | 构建脚本不包含生产密码和密钥 |

### 版本号管理

版本号用于标识 Spark 任务发布包、代码版本、SQL 版本和配置版本。生产环境必须能够根据运行日志或审计记录追溯到具体 Jar 版本。

推荐版本号格式：

```text
主版本.次版本.修订号
```

示例：

```text
1.0.0
1.1.0
1.1.1
2.0.0
```

版本含义建议：

| 类型   | 说明                                         |
| ------ | -------------------------------------------- |
| 主版本 | 不兼容变更，例如任务结构、输出表结构重大调整 |
| 次版本 | 新增功能或新增任务，保持兼容                 |
| 修订号 | Bug 修复、SQL 小调整、性能优化               |

Maven 版本配置示例：

```xml
<groupId>io.github.atengk</groupId>
<artifactId>spark-job</artifactId>
<version>${revision}</version>

<properties>
    <!-- 默认版本，可由构建命令 -Drevision 覆盖 -->
    <revision>1.0.0</revision>
</properties>
```

打包时指定版本：

```bash
mvn clean package -Drevision=1.0.1
```

在启动日志中打印版本：

```java
String version = Application.class.getPackage().getImplementationVersion();
log.info("Spark 应用版本：{}", version);
```

版本号管理建议如下：

| 建议                     | 说明                       |
| ------------------------ | -------------------------- |
| 每次生产发布必须有版本号 | 不使用无版本 Jar 覆盖      |
| Jar 文件名包含版本       | 例如 `spark-job-1.0.1.jar` |
| 审计日志记录版本         | 便于回溯问题               |
| SQL 修改也要升级版本     | SQL 是业务逻辑的一部分     |
| 配置变更记录版本         | 生产配置变化要可追踪       |
| 保留历史发布包           | 支持快速回滚               |

## 任务提交

本章节用于说明 Spark 任务通过 `spark-submit` 提交到不同运行模式的方法，包括本地模式、Standalone、YARN Client、YARN Cluster 和 Kubernetes。任务提交需要明确主类、运行模式、资源参数、依赖文件、配置参数和业务参数。

### spark-submit 基础参数

`spark-submit` 是 Spark 官方提供的任务提交命令。无论使用本地模式、YARN、Standalone 还是 Kubernetes，核心参数都包括主类、Master、部署模式、资源配置、Spark 配置、依赖文件和应用参数。

基础命令格式：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --name spark-user-summary \
  --conf spark.sql.shuffle.partitions=400 \
  target/spark-job.jar \
  --jobName user-summary \
  --env prod \
  --bizDate 2026-05-11
```

常用基础参数如下：

| 参数                | 说明                                                         |
| ------------------- | ------------------------------------------------------------ |
| `--class`           | 指定应用主类                                                 |
| `--master`          | 指定运行模式，例如 `local[*]`、`yarn`、`spark://...`、`k8s://...` |
| `--deploy-mode`     | 部署模式，常见为 `client` 或 `cluster`                       |
| `--name`            | Spark 应用名称                                               |
| `--conf`            | 设置 Spark 配置                                              |
| `--jars`            | 附加外部 Jar                                                 |
| `--files`           | 分发配置文件                                                 |
| `--packages`        | 自动下载 Maven 依赖                                          |
| `--driver-memory`   | Driver 内存                                                  |
| `--executor-memory` | Executor 内存                                                |
| `--executor-cores`  | 每个 Executor 核数                                           |
| `--num-executors`   | Executor 数量，YARN 常用                                     |
| 应用 Jar            | Spark 项目构建产物                                           |
| 应用参数            | Jar 后面的参数，由业务程序解析                               |

`spark-submit` 参数分为 Spark 参数和业务参数。Jar 前面的参数由 Spark 解析，Jar 后面的参数由应用程序 `main(String[] args)` 接收。

### 本地模式提交

本地模式适合开发调试、小样例验证、SQL 逻辑验证和本地集成测试。它不需要集群资源，Driver 和 Executor 都运行在本地 JVM 中。

本地提交示例：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master local[*] \
  --name spark-user-summary-local \
  --conf spark.sql.shuffle.partitions=4 \
  target/spark-job.jar \
  --jobName user-summary \
  --env local \
  --bizDate 2026-05-11 \
  --inputPath data/input/user_event.json \
  --outputPath data/output/user_summary \
  --overwrite true
```

本地模式常用 Master：

| Master                 | 说明                           |
| ---------------------- | ------------------------------ |
| `local[1]`             | 单线程运行，适合排查执行顺序   |
| `local[2]`             | 两个线程运行，适合基础并行测试 |
| `local[*]`             | 使用本机全部 CPU 核心          |
| `local-cluster[n,c,m]` | 模拟本地集群，使用较少         |

本地模式建议如下：

| 建议                   | 说明                                     |
| ---------------------- | ---------------------------------------- |
| 使用小样例数据         | 不在本地读取大规模生产数据               |
| 降低 Shuffle 分区      | 设置为 2 到 8 即可                       |
| 输出到临时目录         | 不要写生产路径                           |
| 本地不启用生产 Hive    | 避免误读误写                             |
| 用于逻辑验证           | 不代表真实集群性能                       |
| 提交前仍需测试集群验证 | 集群权限、依赖、资源问题本地无法完全覆盖 |

### Standalone 模式提交

Standalone 是 Spark 自带的集群模式，适合独立部署 Spark 集群的场景。Standalone 模式使用 Spark Master 和 Worker 管理资源，不依赖 YARN 或 Kubernetes。

Standalone Client 模式提交：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master spark://spark-master:7077 \
  --deploy-mode client \
  --name spark-user-summary-standalone \
  --driver-memory 2g \
  --executor-memory 4g \
  --executor-cores 2 \
  --conf spark.sql.shuffle.partitions=200 \
  target/spark-job.jar \
  --jobName user-summary \
  --env test \
  --bizDate 2026-05-11
```

Standalone Cluster 模式提交：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master spark://spark-master:7077 \
  --deploy-mode cluster \
  --name spark-user-summary-standalone \
  --driver-memory 2g \
  --executor-memory 4g \
  --executor-cores 2 \
  --conf spark.sql.shuffle.partitions=200 \
  target/spark-job.jar \
  --jobName user-summary \
  --env test \
  --bizDate 2026-05-11
```

Standalone 参数建议如下：

| 参数                        | 说明                       |
| --------------------------- | -------------------------- |
| `spark://spark-master:7077` | Spark Master 地址          |
| `--deploy-mode client`      | Driver 在提交机器运行      |
| `--deploy-mode cluster`     | Driver 在 Worker 节点运行  |
| `--executor-memory`         | Executor 内存              |
| `--executor-cores`          | 每个 Executor 核数         |
| `spark.cores.max`           | 当前应用可使用的最大总核数 |

示例：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master spark://spark-master:7077 \
  --deploy-mode cluster \
  --conf spark.cores.max=8 \
  --executor-memory 4g \
  --executor-cores 2 \
  target/spark-job.jar \
  --jobName user-summary \
  --env test \
  --bizDate 2026-05-11
```

Standalone 模式建议如下：

| 建议                  | 说明                                |
| --------------------- | ----------------------------------- |
| 适合独立 Spark 集群   | 不依赖 YARN 或 K8s                  |
| 生产优先 Cluster 模式 | 避免提交客户端断开影响任务          |
| 控制总核数            | 使用 `spark.cores.max` 限制资源     |
| 配置 History Server   | 便于查看历史任务                    |
| 依赖和配置文件要分发  | 使用 `--jars`、`--files` 或共享目录 |

### YARN Client 模式提交

YARN Client 模式下，Driver 运行在提交任务的客户端机器上，Executor 运行在 YARN NodeManager 上。该模式适合开发调试和测试环境观察 Driver 日志，但不适合生产长期任务，因为客户端断开可能影响任务运行。

YARN Client 提交示例：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode client \
  --name spark-user-summary-client \
  --queue root.test.etl \
  --driver-memory 2g \
  --executor-memory 4g \
  --executor-cores 2 \
  --num-executors 4 \
  --conf spark.sql.shuffle.partitions=200 \
  target/spark-job.jar \
  --jobName user-summary \
  --env test \
  --bizDate 2026-05-11
```

YARN Client 特点如下：

| 特点                  | 说明                     |
| --------------------- | ------------------------ |
| Driver 在提交机器     | 方便查看日志和调试       |
| Executor 在 YARN 集群 | 计算仍在集群执行         |
| 客户端依赖较强        | 提交机器异常可能影响任务 |
| 适合测试调试          | 生产任务不建议长期使用   |

YARN Client 使用建议如下：

| 建议                 | 说明                        |
| -------------------- | --------------------------- |
| 用于测试和调试       | 方便直接查看 Driver 日志    |
| 不用于生产调度       | 生产优先 YARN Cluster       |
| 提交机器配置要稳定   | Driver 运行在本机           |
| 本地配置文件要可访问 | Driver 读取本机配置         |
| 注意客户端网络       | Driver 与 Executor 需要通信 |

### YARN Cluster 模式提交

YARN Cluster 模式下，Driver 运行在 YARN 集群中，提交客户端只负责提交应用。该模式适合生产环境，因为任务不依赖提交机器持续在线。

YARN Cluster 提交示例：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --name spark-user-summary-prod \
  --queue root.prod.etl \
  --driver-memory 2g \
  --executor-memory 4g \
  --executor-cores 2 \
  --num-executors 8 \
  --conf spark.sql.shuffle.partitions=400 \
  --conf spark.sql.adaptive.enabled=true \
  --conf spark.sql.adaptive.skewJoin.enabled=true \
  --conf spark.yarn.maxAppAttempts=1 \
  --files /opt/app/spark-job/conf/application-prod.yml,/opt/module/hive/conf/hive-site.xml \
  target/spark-job.jar \
  --jobName user-summary \
  --env prod \
  --bizDate 2026-05-11 \
  --batchId 202605110001 \
  --overwrite true
```

YARN Cluster 常用参数：

| 参数                        | 说明                                       |
| --------------------------- | ------------------------------------------ |
| `--master yarn`             | 使用 YARN 资源管理                         |
| `--deploy-mode cluster`     | Driver 在 YARN 集群中运行                  |
| `--queue`                   | 指定 YARN 队列                             |
| `--num-executors`           | Executor 数量                              |
| `--executor-cores`          | 每个 Executor 核数                         |
| `--executor-memory`         | 每个 Executor 内存                         |
| `--driver-memory`           | Driver 内存                                |
| `spark.yarn.maxAppAttempts` | YARN 应用最大尝试次数                      |
| `--files`                   | 分发配置文件到 Driver 和 Executor 工作目录 |

查看 YARN 应用：

```bash
# 查看当前运行中的应用
yarn application -list

# 查看指定应用状态
yarn application -status application_1710000000000_0001

# 停止指定应用
yarn application -kill application_1710000000000_0001
```

YARN Cluster 使用建议如下：

| 建议                        | 说明                              |
| --------------------------- | --------------------------------- |
| 生产任务优先使用            | 不依赖提交客户端                  |
| 配置文件通过 `--files` 分发 | Driver 在集群中，不能依赖本机路径 |
| 资源参数由调度平台管理      | 便于不同任务单独调优              |
| Application ID 写入审计日志 | 便于关联 YARN UI 和 Spark UI      |
| 失败后查看 YARN 日志        | Driver 日志在 YARN 容器中         |
| 控制最大重试次数            | 防止非幂等任务重复写入            |

### Kubernetes 模式提交

Kubernetes 模式下，Spark Driver 和 Executor 以 Pod 形式运行。该模式适合云原生部署环境，便于资源隔离、镜像管理、弹性调度和容器化运维。

Kubernetes 提交示例：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master k8s://https://kubernetes.default.svc \
  --deploy-mode cluster \
  --name spark-user-summary-k8s \
  --conf spark.kubernetes.namespace=spark \
  --conf spark.kubernetes.container.image=registry.example.com/bigdata/spark-job:1.0.0 \
  --conf spark.kubernetes.authenticate.driver.serviceAccountName=spark \
  --conf spark.executor.instances=4 \
  --conf spark.executor.cores=2 \
  --conf spark.executor.memory=4g \
  --conf spark.driver.memory=2g \
  local:///opt/spark/app/spark-job.jar \
  --jobName user-summary \
  --env prod \
  --bizDate 2026-05-11
```

Kubernetes 模式常用配置：

| 配置                                                      | 说明                         |
| --------------------------------------------------------- | ---------------------------- |
| `spark.kubernetes.namespace`                              | Pod 所在命名空间             |
| `spark.kubernetes.container.image`                        | Spark 应用镜像               |
| `spark.kubernetes.authenticate.driver.serviceAccountName` | Driver 使用的 ServiceAccount |
| `spark.executor.instances`                                | Executor Pod 数量            |
| `spark.executor.cores`                                    | Executor CPU 核数            |
| `spark.executor.memory`                                   | Executor 内存                |
| `spark.driver.memory`                                     | Driver 内存                  |
| `local:///path/app.jar`                                   | 镜像内 Jar 路径              |

镜像中建议包含：

```text
/opt/spark/app/spark-job.jar
/opt/spark/conf/application-prod.yml
/opt/spark/conf/logback.xml
/opt/spark/conf/hive-site.xml
```

Kubernetes 模式建议如下：

| 建议                              | 说明                           |
| --------------------------------- | ------------------------------ |
| 使用固定镜像版本                  | 镜像版本与应用版本绑定         |
| 配置通过 ConfigMap 或 Secret 注入 | 生产配置和密钥不要写死在镜像中 |
| 使用 ServiceAccount 控制权限      | 访问 K8s 资源和对象存储需授权  |
| Driver 和 Executor 资源明确配置   | 避免 Pod 被频繁驱逐            |
| 日志接入平台采集                  | 通过 stdout 或日志采集 Sidecar |
| 对象存储替代 HDFS 时注意提交协议  | 避免写入失败留下脏数据         |

### 资源参数配置

资源参数直接影响 Spark 任务的运行效率和稳定性。资源配置需要结合数据量、Shuffle 量、Join 复杂度、Executor 数量、队列资源和历史运行情况调整。

常用资源参数如下：

| 参数                              | 示例   | 说明                   |
| --------------------------------- | ------ | ---------------------- |
| `--driver-memory`                 | `2g`   | Driver 内存            |
| `--executor-memory`               | `4g`   | Executor 内存          |
| `--executor-cores`                | `2`    | 每个 Executor CPU 核数 |
| `--num-executors`                 | `8`    | Executor 数量          |
| `spark.executor.memoryOverhead`   | `1g`   | Executor 堆外内存      |
| `spark.driver.memoryOverhead`     | `512m` | Driver 堆外内存        |
| `spark.sql.shuffle.partitions`    | `400`  | SQL Shuffle 分区数     |
| `spark.default.parallelism`       | `400`  | RDD 默认并行度         |
| `spark.dynamicAllocation.enabled` | `true` | 动态资源分配           |

资源配置示例：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --queue root.prod.etl \
  --driver-memory 2g \
  --executor-memory 4g \
  --executor-cores 2 \
  --num-executors 8 \
  --conf spark.executor.memoryOverhead=1g \
  --conf spark.sql.shuffle.partitions=400 \
  --conf spark.sql.adaptive.enabled=true \
  target/spark-job.jar \
  --jobName user-summary \
  --env prod \
  --bizDate 2026-05-11
```

资源参数配置建议如下：

| 场景         | 建议                                            |
| ------------ | ----------------------------------------------- |
| 小数据任务   | Executor 数量少，Shuffle 分区适当降低           |
| 大数据 Join  | 增加 Executor 数量和 Shuffle 分区               |
| Driver OOM   | 检查是否 collect 大数据，再考虑增加 Driver 内存 |
| Executor OOM | 调整分区数、内存、Join 策略和缓存策略           |
| Shuffle 失败 | 检查磁盘、网络、分区数和数据倾斜                |
| 小文件过多   | 写出前降低分区或设置文件大小                    |
| 本地调试     | `spark.sql.shuffle.partitions=4`                |

资源调优原则如下：

| 原则                    | 说明                               |
| ----------------------- | ---------------------------------- |
| 先分析 Spark UI         | 不盲目加资源                       |
| 关注最慢 Stage          | 优化瓶颈阶段最有效                 |
| Executor cores 不宜过大 | 常见设置为 2 到 5                  |
| 单 Task 数据量要合理    | 分区过少容易 OOM                   |
| AQE 建议开启            | Spark 3.x 下可改善分区和 Join 策略 |
| 资源配置要版本化        | 生产任务调参需要记录变更           |

### 运行参数传递

运行参数是传给业务程序的参数，位于应用 Jar 后面，由 `main(String[] args)` 接收。运行参数通常包括任务名称、环境、业务日期、批次号、输入路径、输出路径、写入模式等。

运行参数示例：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  target/spark-job.jar \
  --jobName user-summary \
  --env prod \
  --bizDate 2026-05-11 \
  --batchId 202605110001 \
  --sourceTable dwd.dwd_user_detail \
  --targetTable ads.ads_user_summary \
  --writeMode overwrite \
  --allowEmpty false
```

业务参数解析示例：

```java
package io.github.atengk.spark.config;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Spark 运行参数解析器。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class RuntimeArgParser {

    private RuntimeArgParser() {
    }

    /**
     * 将启动参数解析为 Map。
     *
     * @param args 启动参数
     * @return 参数 Map
     */
    public static Map<String, String> parseToMap(String[] args) {
        Map<String, String> argMap = new HashMap<>();

        if (args == null || args.length == 0) {
            log.warn("未传入运行参数");
            return argMap;
        }

        for (int i = 0; i < args.length; i++) {
            String key = args[i];
            if (StrUtil.startWith(key, "--") && i + 1 < args.length) {
                String value = args[i + 1];
                if (!StrUtil.startWith(value, "--")) {
                    argMap.put(StrUtil.removePrefix(key, "--"), value);
                    i++;
                }
            }
        }

        log.info("运行参数解析完成，参数数量：{}", argMap.size());
        return argMap;
    }

    /**
     * 获取必填参数。
     *
     * @param argMap 参数 Map
     * @param key 参数名
     * @return 参数值
     */
    public static String required(Map<String, String> argMap, String key) {
        String value = MapUtil.getStr(argMap, key);
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException(StrUtil.format("缺少必填运行参数：{}", key));
        }
        return value;
    }

    /**
     * 获取可选参数。
     *
     * @param argMap 参数 Map
     * @param key 参数名
     * @param defaultValue 默认值
     * @return 参数值
     */
    public static String optional(Map<String, String> argMap, String key, String defaultValue) {
        return StrUtil.blankToDefault(MapUtil.getStr(argMap, key), defaultValue);
    }
}
```

运行参数传递建议如下：

| 建议                      | 说明                                                |
| ------------------------- | --------------------------------------------------- |
| 使用 `--key value` 格式   | 解析简单，可读性好                                  |
| 参数名称统一              | 所有任务使用 `jobName`、`env`、`bizDate` 等通用名称 |
| 必填参数启动即校验        | 缺少参数直接失败                                    |
| 参数日志需要脱敏          | 密码、Token、Secret 不打印                          |
| 业务参数与 Spark 参数分清 | Jar 前是 Spark 参数，Jar 后是业务参数               |
| 调度平台模板化            | 将业务日期、批次号、环境变量统一由调度平台注入      |


## 调度集成

本章节用于说明 Spark 任务与 Shell、Crontab、DolphinScheduler、Airflow、Azkaban、Oozie 等调度方式的集成方案。调度集成的核心目标是让 Spark 任务可以稳定、可重复、可监控地运行，并支持任务依赖、失败重试、补数、参数传递和日志追踪。

### Shell 脚本调度

Shell 脚本是最基础的 Spark 任务调度方式，适合本地调试、临时执行、简单生产任务和被其他调度平台调用。Shell 脚本应统一封装环境变量、Jar 路径、资源参数、业务参数和日志输出，不建议每次手工拼接 `spark-submit` 命令。

推荐脚本目录结构：

```text
scripts
├── env.sh
├── submit-user-summary.sh
├── submit-stream-user-event.sh
├── stop-job.sh
├── rerun-job.sh
└── backfill-job.sh
```

文件位置：`scripts/env.sh`

该脚本用于维护 Spark 任务公共环境变量。

```bash
#!/usr/bin/env bash

# scripts/env.sh
# Spark 任务公共环境变量

export JAVA_HOME=/opt/module/jdk
export SPARK_HOME=/opt/module/spark
export HADOOP_CONF_DIR=/opt/module/hadoop/etc/hadoop
export HIVE_CONF_DIR=/opt/module/hive/conf

export PATH=$JAVA_HOME/bin:$SPARK_HOME/bin:$PATH

# 应用 Jar 路径
export APP_JAR=/opt/app/spark-job/spark-job.jar

# 默认队列
export YARN_QUEUE=root.prod.etl

# 默认配置文件
export APP_CONF=/opt/app/spark-job/conf/application-prod.yml
export LOG_CONF=/opt/app/spark-job/conf/logback.xml
```

文件位置：`scripts/submit-user-summary.sh`

该脚本用于提交用户汇总离线任务。

```bash
#!/usr/bin/env bash

# scripts/submit-user-summary.sh
# 提交用户汇总 Spark 离线任务

set -e

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
source "${SCRIPT_DIR}/env.sh"

BIZ_DATE=$1
BATCH_ID=${2:-"${BIZ_DATE}"}
ENV=${3:-prod}

if [ -z "${BIZ_DATE}" ]; then
  echo "缺少业务日期参数，例如：./submit-user-summary.sh 2026-05-11"
  exit 1
fi

spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --name "spark-user-summary-${BIZ_DATE}" \
  --queue "${YARN_QUEUE}" \
  --driver-memory 2g \
  --executor-memory 4g \
  --executor-cores 2 \
  --num-executors 8 \
  --conf spark.sql.shuffle.partitions=400 \
  --conf spark.sql.adaptive.enabled=true \
  --files "${APP_CONF},${LOG_CONF},${HIVE_CONF_DIR}/hive-site.xml" \
  "${APP_JAR}" \
  --jobName user-summary \
  --env "${ENV}" \
  --bizDate "${BIZ_DATE}" \
  --batchId "${BATCH_ID}" \
  --writeMode overwrite \
  --allowEmpty false
```

Shell 脚本调度建议如下：

| 建议                    | 说明                                  |
| ----------------------- | ------------------------------------- |
| 统一封装 `spark-submit` | 不建议人工直接提交复杂命令            |
| 参数必须校验            | 业务日期、环境、批次号不能为空        |
| 环境变量集中维护        | Jar 路径、队列、配置文件放入 `env.sh` |
| 日志输出可追踪          | 脚本执行日志和 Spark 应用日志都要保留 |
| 敏感信息不写死          | 密码、Token、Keytab 通过安全方式注入  |
| 脚本进入版本管理        | 任务提交方式需要可追溯                |

### Crontab 调度

Crontab 适合简单定时任务，例如每天固定时间执行一个离线 Spark 任务。它配置简单，但缺少复杂依赖管理、可视化、失败告警、补数编排和任务实例管理能力，因此只适合轻量场景。

Crontab 示例：

```bash
# 编辑当前用户定时任务
crontab -e
```

每天凌晨 2 点执行用户汇总任务：

```bash
# 每天 02:00 执行前一天用户汇总任务
0 2 * * * /opt/app/spark-job/scripts/submit-user-summary.sh $(date -d "yesterday" +\%F) >> /opt/app/spark-job/logs/cron-user-summary.log 2>&1
```

每小时执行小时任务：

```bash
# 每小时第 10 分钟执行上一小时任务
10 * * * * /opt/app/spark-job/scripts/submit-hour-job.sh $(date -d "1 hour ago" +\%F) $(date -d "1 hour ago" +\%H) >> /opt/app/spark-job/logs/cron-hour-job.log 2>&1
```

Crontab 调度建议如下：

| 建议               | 说明                                   |
| ------------------ | -------------------------------------- |
| 只用于简单任务     | 复杂依赖任务建议使用专业调度平台       |
| 日志必须重定向     | 否则排查困难                           |
| 时间参数显式生成   | 使用业务日期，不依赖程序内部当前时间   |
| 注意百分号转义     | Crontab 中 `date +%F` 需要写成 `+\%F`  |
| 配置环境变量       | Crontab 环境较少，脚本中应 source 环境 |
| 失败告警需额外实现 | Crontab 本身不提供完善告警能力         |

### DolphinScheduler 集成

DolphinScheduler 是常见的数据调度平台，适合编排 Spark、Shell、SQL、Flink、Python 等多类型任务。Spark 项目中可以直接使用 DolphinScheduler 的 Spark 任务类型，也可以通过 Shell 节点调用标准提交脚本。

推荐集成方式：

| 方式       | 说明                                      |
| ---------- | ----------------------------------------- |
| Spark 节点 | 平台直接配置主类、Jar、资源参数和业务参数 |
| Shell 节点 | 调用项目中的 `submit-xxx.sh` 脚本         |
| 子工作流   | 将多个 Spark 任务按依赖编排               |
| 参数传递   | 使用调度日期、业务日期、环境变量注入      |

Shell 节点调用示例：

```bash
/opt/app/spark-job/scripts/submit-user-summary.sh ${bizDate} ${batchId} prod
```

DolphinScheduler 参数示例：

```text
bizDate=${system.biz.date}
batchId=${system.biz.date}_user_summary
env=prod
```

DolphinScheduler 集成建议如下：

| 建议                        | 说明                                 |
| --------------------------- | ------------------------------------ |
| 推荐通过 Shell 脚本封装提交 | 平台只负责传参，提交细节由项目维护   |
| 调度参数标准化              | 统一使用 `bizDate`、`batchId`、`env` |
| 任务失败自动重试需谨慎      | 非幂等任务不应盲目重试               |
| 工作流配置任务依赖          | 下游任务依赖上游成功完成             |
| 补数使用参数化工作流        | 支持指定日期范围补数                 |
| 告警接入平台能力            | 失败、超时、延迟都应告警             |

### Airflow 集成

Airflow 适合使用 Python DAG 管理复杂任务编排。Spark 任务可以通过 `BashOperator` 调用 Shell 脚本，也可以通过 Spark Provider 的 `SparkSubmitOperator` 直接提交任务。

使用 `BashOperator` 调度示例：

```python
from datetime import datetime, timedelta

from airflow import DAG
from airflow.operators.bash import BashOperator

default_args = {
    "owner": "data-team",
    "depends_on_past": False,
    "retries": 1,
    "retry_delay": timedelta(minutes=10),
}

with DAG(
    dag_id="spark_user_summary_daily",
    default_args=default_args,
    start_date=datetime(2026, 5, 1),
    schedule_interval="0 2 * * *",
    catchup=False,
    tags=["spark", "user", "daily"],
) as dag:

    user_summary = BashOperator(
        task_id="user_summary",
        bash_command=(
            "/opt/app/spark-job/scripts/submit-user-summary.sh "
            "{{ ds }} "
            "{{ ds_nodash }}_user_summary "
            "prod"
        ),
    )
```

使用 `SparkSubmitOperator` 示例：

```python
from datetime import datetime, timedelta

from airflow import DAG
from airflow.providers.apache.spark.operators.spark_submit import SparkSubmitOperator

with DAG(
    dag_id="spark_user_summary_submit",
    start_date=datetime(2026, 5, 1),
    schedule_interval="0 2 * * *",
    catchup=False,
) as dag:

    user_summary = SparkSubmitOperator(
        task_id="user_summary",
        application="/opt/app/spark-job/spark-job.jar",
        java_class="io.github.atengk.spark.Application",
        conn_id="spark_yarn",
        name="spark-user-summary-{{ ds }}",
        application_args=[
            "--jobName", "user-summary",
            "--env", "prod",
            "--bizDate", "{{ ds }}",
            "--batchId", "{{ ds_nodash }}_user_summary",
            "--writeMode", "overwrite",
        ],
        conf={
            "spark.sql.shuffle.partitions": "400",
            "spark.sql.adaptive.enabled": "true",
        },
        driver_memory="2g",
        executor_memory="4g",
        executor_cores=2,
        num_executors=8,
    )
```

Airflow 集成建议如下：

| 建议                                    | 说明                     |
| --------------------------------------- | ------------------------ |
| 简单场景使用 BashOperator               | 复用已有提交脚本         |
| 标准 Spark 场景可用 SparkSubmitOperator | 参数更结构化             |
| 使用 `{{ ds }}` 作为业务日期            | 保证调度日期明确         |
| 谨慎开启 catchup                        | 大量历史补跑可能压垮集群 |
| 重试策略与幂等配合                      | 写出必须支持重复执行     |
| DAG 中记录任务标签                      | 便于运维检索             |

### Azkaban 集成

Azkaban 是较早期常用的批处理任务调度系统，通常通过 `.job` 文件定义任务和依赖关系。Spark 任务可以通过 `command` 调用 Shell 提交脚本。

Azkaban 任务文件示例：

```properties
# user_summary.job
# 用户汇总 Spark 任务

type=command
command=/opt/app/spark-job/scripts/submit-user-summary.sh ${bizDate} ${batchId} prod
```

依赖任务示例：

```properties
# order_summary.job
# 订单汇总任务依赖用户汇总任务完成

type=command
dependencies=user_summary
command=/opt/app/spark-job/scripts/submit-order-summary.sh ${bizDate} ${batchId} prod
```

Azkaban 参数可以在 Flow 参数中配置：

```properties
bizDate=2026-05-11
batchId=202605110001
```

Azkaban 集成建议如下：

| 建议                       | 说明                   |
| -------------------------- | ---------------------- |
| 通过 Shell 脚本提交 Spark  | 保持提交逻辑统一       |
| `.job` 文件进入版本管理    | 调度定义也属于项目资产 |
| 使用 Flow 参数传业务日期   | 避免硬编码日期         |
| 使用 dependencies 管理依赖 | 上游成功后再执行下游   |
| 失败重试需控制             | 非幂等任务谨慎自动重试 |
| 补数使用独立 Flow          | 避免影响日常调度       |

### Oozie 集成

Oozie 是 Hadoop 生态中的工作流调度系统，适合传统 Hadoop 集群中的 MapReduce、Hive、Sqoop、Spark 等任务编排。Spark 任务可通过 Oozie Spark Action 或 Shell Action 提交。

Oozie Spark Action 示例：

```xml
<workflow-app name="spark-user-summary" xmlns="uri:oozie:workflow:0.5">
    <start to="spark-user-summary-node"/>

    <action name="spark-user-summary-node">
        <spark xmlns="uri:oozie:spark-action:0.2">
            <job-tracker>${jobTracker}</job-tracker>
            <name-node>${nameNode}</name-node>
            <master>yarn</master>
            <mode>cluster</mode>
            <name>spark-user-summary-${bizDate}</name>
            <class>io.github.atengk.spark.Application</class>
            <jar>${appPath}/spark-job.jar</jar>
            <spark-opts>
                --driver-memory 2g
                --executor-memory 4g
                --executor-cores 2
                --num-executors 8
                --conf spark.sql.shuffle.partitions=400
                --conf spark.sql.adaptive.enabled=true
            </spark-opts>
            <arg>--jobName</arg>
            <arg>user-summary</arg>
            <arg>--env</arg>
            <arg>prod</arg>
            <arg>--bizDate</arg>
            <arg>${bizDate}</arg>
            <arg>--batchId</arg>
            <arg>${batchId}</arg>
        </spark>
        <ok to="end"/>
        <error to="fail"/>
    </action>

    <kill name="fail">
        <message>Spark 用户汇总任务失败，错误信息：${wf:errorMessage(wf:lastErrorNode())}</message>
    </kill>

    <end name="end"/>
</workflow-app>
```

Oozie 集成建议如下：

| 建议                       | 说明                                       |
| -------------------------- | ------------------------------------------ |
| 传统 Hadoop 环境适用       | 新项目更多使用 DolphinScheduler 或 Airflow |
| 参数使用 properties 管理   | `bizDate`、`batchId`、路径等外部传入       |
| Jar 上传到 HDFS            | Oozie 通常从 HDFS 分发资源                 |
| 失败节点要清晰             | kill 节点输出错误信息                      |
| 权限和 Kerberos 需提前配置 | Oozie 与 Spark、Hive、HDFS 权限链路较长    |
| 工作流 XML 进入版本管理    | 保证调度定义可追踪                         |

### 任务依赖配置

任务依赖用于控制多个 Spark 任务之间的执行顺序。常见依赖包括数据层级依赖、主题域依赖、维表依赖、实时到离线依赖和外部系统数据到达依赖。

常见依赖类型如下：

| 依赖类型 | 说明                                 |
| -------- | ------------------------------------ |
| 时间依赖 | 每日任务依赖指定业务日期             |
| 数据依赖 | 下游依赖上游表分区产出               |
| 任务依赖 | 下游任务依赖上游任务成功             |
| 维表依赖 | 宽表构建依赖维表刷新完成             |
| 外部依赖 | 依赖业务库同步、Kafka 落盘、文件到达 |
| 资源依赖 | 依赖队列资源或集群可用性             |

典型数仓任务依赖：

```text
ODS 原始数据同步
        ↓
DWD 明细清洗
        ↓
DWS 宽表构建
        ↓
ADS 指标汇总
        ↓
报表刷新 / 数据服务同步
```

依赖检查脚本示例：

```bash
#!/usr/bin/env bash

# scripts/check-hive-partition.sh
# 检查 Hive 表分区是否存在

set -e

TABLE_NAME=$1
BIZ_DATE=$2

if [ -z "${TABLE_NAME}" ] || [ -z "${BIZ_DATE}" ]; then
  echo "用法：./check-hive-partition.sh dwd.dwd_user_event 2026-05-11"
  exit 1
fi

PARTITION_COUNT=$(spark-sql -S -e "SHOW PARTITIONS ${TABLE_NAME} PARTITION(dt='${BIZ_DATE}')" | wc -l)

if [ "${PARTITION_COUNT}" -eq 0 ]; then
  echo "依赖分区不存在，table=${TABLE_NAME}, dt=${BIZ_DATE}"
  exit 1
fi

echo "依赖分区存在，table=${TABLE_NAME}, dt=${BIZ_DATE}"
```

任务依赖配置建议如下：

| 建议                           | 说明                         |
| ------------------------------ | ---------------------------- |
| 按数据层级配置依赖             | ODS → DWD → DWS → ADS        |
| 依赖具体分区而非只依赖任务成功 | 上游成功但未产出数据时应失败 |
| 维表刷新要前置                 | 宽表任务依赖维表可用         |
| 依赖检查失败不执行下游         | 避免错误数据扩散             |
| 依赖关系文档化                 | 便于排查链路问题             |
| 调度平台中配置可视化依赖       | 避免脚本隐式依赖过多         |

### 补数任务配置

补数任务用于处理历史日期数据重算、任务失败后重跑、业务口径变更后回刷和上游数据延迟后的修复。补数任务必须与日常任务隔离，避免补数消耗过多资源影响正常调度。

补数脚本示例：

```bash
#!/usr/bin/env bash

# scripts/backfill-user-summary.sh
# 按日期范围补跑用户汇总任务

set -e

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)

START_DATE=$1
END_DATE=$2
ENV=${3:-prod}

if [ -z "${START_DATE}" ] || [ -z "${END_DATE}" ]; then
  echo "用法：./backfill-user-summary.sh 2026-05-01 2026-05-11 prod"
  exit 1
fi

CURRENT_DATE="${START_DATE}"

while [ "${CURRENT_DATE}" != "$(date -d "${END_DATE} +1 day" +%F)" ]; do
  BATCH_ID="$(date -d "${CURRENT_DATE}" +%Y%m%d)_backfill_user_summary"

  echo "开始补数，bizDate=${CURRENT_DATE}, batchId=${BATCH_ID}"

  "${SCRIPT_DIR}/submit-user-summary.sh" "${CURRENT_DATE}" "${BATCH_ID}" "${ENV}"

  CURRENT_DATE=$(date -d "${CURRENT_DATE} +1 day" +%F)
done

echo "补数完成，startDate=${START_DATE}, endDate=${END_DATE}"
```

补数任务提交示例：

```bash
chmod +x scripts/backfill-user-summary.sh

./scripts/backfill-user-summary.sh 2026-05-01 2026-05-11 prod
```

补数任务配置建议如下：

| 建议                   | 说明                   |
| ---------------------- | ---------------------- |
| 补数使用独立 batchId   | 与日常调度区分         |
| 支持日期范围           | 不手动重复提交多个日期 |
| 补数默认覆盖目标分区   | 保证结果幂等           |
| 控制并发数量           | 防止补数压垮集群       |
| 避开业务高峰           | 大规模补数建议低峰执行 |
| 补数前确认上游数据完整 | 避免重算仍然错误       |
| 补数后做数据校验       | 对比数据量和核心指标   |

## 性能优化

本章节用于说明 Spark 任务性能优化的常见方向，包括分区、Shuffle、Join、缓存、广播变量、数据倾斜、小文件、内存、并行度和 SQL 执行计划分析。性能优化应以 Spark UI、执行计划、数据量、Shuffle 指标和任务日志为依据，不应盲目增加资源。

### 分区优化

分区是 Spark 并行计算的基本单位。分区数过少会导致资源利用不足，单个 Task 处理数据过大；分区数过多会增加调度开销，并可能产生大量小文件。

查看分区数：

```java
int partitionCount = dataset.rdd().getNumPartitions();
log.info("当前 DataFrame 分区数：{}", partitionCount);
```

重新分区：

```java
Dataset<Row> repartitionDf = sourceDf.repartition(400);
```

按字段重新分区：

```java
Dataset<Row> repartitionByDateDf = sourceDf.repartition(functions.col("dt"));
```

减少分区：

```java
Dataset<Row> coalesceDf = sourceDf.coalesce(20);
```

分区优化建议如下：

| 场景           | 建议                               |
| -------------- | ---------------------------------- |
| 输入数据量大   | 增加分区数，提高并行度             |
| 单个 Task 很慢 | 检查分区过大或数据倾斜             |
| 输出小文件过多 | 写出前使用 `coalesce` 减少分区     |
| Join 或聚合前  | 根据 Key 适当 repartition          |
| 本地调试       | Shuffle 分区设置为 2 到 8          |
| 生产任务       | 结合数据量和 Executor 核数设置分区 |

分区数经验值：

```text
建议分区数 ≈ Executor 总核数的 2 到 4 倍
```

但该经验值只适合作为初始值，最终应根据 Spark UI 中 Task 耗时、Shuffle 数据量和输出文件大小调整。

### Shuffle 优化

Shuffle 是 Spark 中最消耗资源的操作之一，常见于 `groupBy`、`distinct`、`join`、`orderBy`、窗口函数和重分区操作。Shuffle 会产生磁盘 IO、网络传输和序列化开销，是性能优化重点。

常见产生 Shuffle 的操作：

| 操作             | 说明                             |
| ---------------- | -------------------------------- |
| `groupBy`        | 按 Key 聚合                      |
| `join`           | 大表关联                         |
| `distinct`       | 全局去重                         |
| `dropDuplicates` | 按字段去重                       |
| `orderBy`        | 全局排序                         |
| `repartition`    | 重新分区                         |
| 窗口函数         | 通常需要按分区和排序字段 Shuffle |

优化前：

```java
Dataset<Row> resultDf = sourceDf
        .groupBy("user_id")
        .count()
        .orderBy(functions.col("count").desc());
```

优化方向：

```java
Dataset<Row> selectedDf = sourceDf
        .filter(functions.col("dt").equalTo("2026-05-11"))
        .select("user_id");

Dataset<Row> resultDf = selectedDf
        .groupBy("user_id")
        .count();
```

常用 Shuffle 配置：

```bash
--conf spark.sql.shuffle.partitions=400
--conf spark.sql.adaptive.enabled=true
--conf spark.sql.adaptive.coalescePartitions.enabled=true
--conf spark.sql.adaptive.skewJoin.enabled=true
```

Shuffle 优化建议如下：

| 建议                      | 说明                             |
| ------------------------- | -------------------------------- |
| 先过滤再 Shuffle          | 减少参与 Shuffle 的数据量        |
| 先裁剪字段再 Shuffle      | 降低 Shuffle 数据宽度            |
| 避免重复聚合              | 多个指标尽量一次 groupBy 完成    |
| 避免不必要 distinct       | 明确去重键，不滥用全字段去重     |
| 开启 AQE                  | Spark 3.x 推荐开启自适应执行     |
| 调整 Shuffle 分区数       | 分区过少会 OOM，过多会调度开销大 |
| 分析 Shuffle Read / Write | 通过 Spark UI 定位瓶颈 Stage     |

### Join 优化

Join 是 Spark 性能问题高发点。Join 优化需要关注大小表关系、Join Key 类型、字段裁剪、广播 Join、数据倾斜和 Join 顺序。

Join 前字段裁剪：

```java
Dataset<Row> orderDf = spark.table("dwd.dwd_order_detail")
        .filter(functions.col("dt").equalTo(bizDate))
        .select("user_id", "product_id", "order_amount");

Dataset<Row> userDf = spark.table("dim.dim_user_current")
        .select("user_id", "user_level")
        .dropDuplicates("user_id");

Dataset<Row> resultDf = orderDf.join(userDf, "user_id");
```

广播小表 Join：

```java
Dataset<Row> resultDf = orderDf.join(
        functions.broadcast(userDf),
        orderDf.col("user_id").equalTo(userDf.col("user_id")),
        "left"
);
```

SQL 广播 Hint：

```sql
SELECT /*+ BROADCAST(u) */
    o.order_id,
    o.user_id,
    u.user_level,
    o.order_amount
FROM dwd_order o
LEFT JOIN dim_user u
    ON o.user_id = u.user_id
WHERE o.dt = '${bizDate}'
```

Join 优化建议如下：

| 建议               | 说明                       |
| ------------------ | -------------------------- |
| Join 前裁剪字段    | 减少网络传输和内存占用     |
| 小表使用广播 Join  | 避免大表 Shuffle           |
| 维表先去重         | 防止 Join 后数据膨胀       |
| Join Key 类型一致  | 避免隐式类型转换影响性能   |
| 过滤条件尽量前置   | 减少参与 Join 的数据       |
| 多表 Join 注意顺序 | 优先关联过滤效果明显的小表 |
| 倾斜 Key 特殊处理  | 热点 Key 不靠普通调参解决  |

广播阈值配置：

```bash
--conf spark.sql.autoBroadcastJoinThreshold=10485760
```

如果小表略大但 Executor 内存充足，可以适当调大该值；如果广播导致 Executor OOM，则需要降低阈值或取消广播。

### 缓存优化

缓存用于避免中间结果被重复计算。缓存适合多次复用且计算成本较高的数据集，不适合一次性使用的数据集。错误使用缓存会占用 Executor 内存，反而降低性能。

DataFrame 缓存示例：

```java
Dataset<Row> baseDf = spark.table("dwd.dwd_user_event")
        .filter(functions.col("dt").equalTo(bizDate))
        .select("user_id", "event_type", "event_time")
        .cache();

long clickCount = baseDf.filter(functions.col("event_type").equalTo("click")).count();
long payCount = baseDf.filter(functions.col("event_type").equalTo("pay")).count();

baseDf.unpersist();
```

指定存储级别：

```java
Dataset<Row> cachedDf = baseDf.persist(StorageLevel.MEMORY_AND_DISK());

cachedDf.count();

cachedDf.unpersist();
```

缓存优化建议如下：

| 建议                       | 说明                                 |
| -------------------------- | ------------------------------------ |
| 多次复用才缓存             | 只使用一次的数据不缓存               |
| 缓存后触发 Action          | `cache()` 是惰性的，需要 Action 生效 |
| 数据量大用 MEMORY_AND_DISK | 避免内存不足导致缓存失败             |
| 用完及时 unpersist         | 释放 Executor 内存                   |
| 不缓存原始大宽表           | 优先裁剪字段、过滤后再缓存           |
| 监控 Storage 页签          | Spark UI 查看缓存是否生效            |

### 广播变量

广播变量用于将较小的只读数据分发到各个 Executor，避免每个 Task 重复传输。常见场景包括小维表、规则配置、字典映射、黑名单、枚举映射等。

广播变量适合 RDD 或自定义函数场景：

```java
JavaSparkContext javaSparkContext = JavaSparkContext.fromSparkContext(spark.sparkContext());

Map<String, String> statusMap = new HashMap<>();
statusMap.put("ACTIVE", "启用");
statusMap.put("DISABLED", "禁用");

Broadcast<Map<String, String>> broadcastStatusMap = javaSparkContext.broadcast(statusMap);

JavaRDD<String> resultRdd = sourceRdd.map(status -> {
    Map<String, String> map = broadcastStatusMap.value();
    return map.getOrDefault(status, "未知");
});
```

DataFrame Join 中更常见的是广播 Join：

```java
Dataset<Row> resultDf = factDf.join(functions.broadcast(dimDf), "dim_id");
```

广播变量使用建议如下：

| 建议                        | 说明                            |
| --------------------------- | ------------------------------- |
| 广播数据必须足够小          | 大对象广播会占用 Executor 内存  |
| 广播对象只读                | 不应在 Executor 中修改          |
| 使用后销毁                  | 长任务中可调用 `destroy()` 释放 |
| DataFrame 维表优先广播 Join | 比手动广播 Map 更易维护         |
| 不广播大维表                | 大维表应正常 Join 或预处理      |
| 注意序列化                  | 广播对象必须可序列化            |

### 数据倾斜处理

数据倾斜是指少数 Key 对应的数据量远大于其他 Key，导致部分 Task 执行时间特别长甚至 OOM。数据倾斜常见于 Join、GroupBy、Distinct、窗口函数等操作。

数据倾斜表现：

| 表现                | 说明                                |
| ------------------- | ----------------------------------- |
| 少数 Task 极慢      | Spark UI 中 Task Duration 差异明显  |
| Shuffle Read 差异大 | 个别 Task 读取数据远大于其他 Task   |
| Executor OOM        | 热点 Key 集中到单个 Executor        |
| Join 长时间卡住     | 大表 Join 热点 Key 导致单 Task 拖慢 |
| 输出文件大小不均    | 某些分区文件特别大                  |

识别热点 Key：

```java
Dataset<Row> keyCountDf = sourceDf
        .groupBy("user_id")
        .count()
        .orderBy(functions.col("count").desc());

keyCountDf.show(20, false);
```

过滤无效热点 Key：

```java
Dataset<Row> filteredDf = sourceDf
        .filter(functions.col("user_id").isNotNull())
        .filter(functions.not(functions.col("user_id").equalTo("UNKNOWN")));
```

加盐处理 Join 倾斜示例：

```java
Dataset<Row> factSaltDf = factDf.withColumn(
        "salt",
        functions.floor(functions.rand().multiply(10))
);

Dataset<Row> dimSaltDf = dimDf
        .withColumn("salt_array", functions.array(
                functions.lit(0), functions.lit(1), functions.lit(2), functions.lit(3), functions.lit(4),
                functions.lit(5), functions.lit(6), functions.lit(7), functions.lit(8), functions.lit(9)
        ))
        .withColumn("salt", functions.explode(functions.col("salt_array")))
        .drop("salt_array");

Dataset<Row> resultDf = factSaltDf.join(
        dimSaltDf,
        factSaltDf.col("user_id").equalTo(dimSaltDf.col("user_id"))
                .and(factSaltDf.col("salt").equalTo(dimSaltDf.col("salt"))),
        "left"
);
```

数据倾斜处理建议如下：

| 方式          | 适用场景                        |
| ------------- | ------------------------------- |
| 过滤无效 Key  | null、UNKNOWN、空字符串造成倾斜 |
| 广播小表      | 小维表 Join 大事实表            |
| 加盐 Join     | 大表 Join 热点 Key              |
| 拆分热点 Key  | 热点 Key 单独处理后 union       |
| AQE skew join | Spark 3.x 自动处理部分倾斜      |
| 调整业务口径  | 某些热点 Key 可能没有业务意义   |

开启 AQE 倾斜 Join：

```bash
--conf spark.sql.adaptive.enabled=true
--conf spark.sql.adaptive.skewJoin.enabled=true
```

数据倾斜不能只靠增加 Executor 或提高内存解决。必须先识别热点 Key，再选择过滤、广播、加盐、拆分或 AQE 等方案。

### 小文件优化

小文件过多会增加 HDFS NameNode 压力、对象存储 list 成本、Spark 任务调度开销和查询延迟。Spark 写出时，每个分区通常至少生成一个文件，因此输出分区数和分区字段设计直接影响小文件数量。

写出前减少分区：

```java
resultDf.coalesce(20)
        .write()
        .mode(SaveMode.Overwrite)
        .parquet("hdfs:///warehouse/ads/user_summary/dt=2026-05-11");
```

按分区字段重分区后写出：

```java
resultDf.repartition(100, functions.col("dt"))
        .write()
        .mode(SaveMode.Append)
        .partitionBy("dt")
        .parquet("hdfs:///warehouse/dwd/user_event");
```

控制单文件记录数：

```bash
--conf spark.sql.files.maxRecordsPerFile=1000000
```

小文件合并任务示例：

```java
Dataset<Row> sourceDf = spark.read()
        .parquet("hdfs:///warehouse/dwd/user_event/dt=2026-05-11");

sourceDf.coalesce(30)
        .write()
        .mode(SaveMode.Overwrite)
        .parquet("hdfs:///warehouse/dwd/user_event_compact/dt=2026-05-11");
```

小文件优化建议如下：

| 建议                   | 说明                                     |
| ---------------------- | ---------------------------------------- |
| 写出前控制分区数       | 使用 `coalesce` 或合理 `repartition`     |
| 避免高基数字段分区     | 不按 user_id、order_id 分区              |
| 设置单文件记录数       | 使用 `spark.sql.files.maxRecordsPerFile` |
| 流任务定期 compaction  | 实时小批写入容易产生小文件               |
| 按数据量设计文件大小   | 常见目标为 128MB 到 512MB                |
| 对象存储更要控制小文件 | 小文件会显著增加请求成本和查询延迟       |

### 内存优化

Spark 内存问题通常表现为 Driver OOM、Executor OOM、GC 时间过长、Shuffle 溢写严重、缓存失败等。内存优化应先判断问题发生在 Driver 还是 Executor，再有针对性处理。

常见内存问题：

| 问题             | 常见原因                                          |
| ---------------- | ------------------------------------------------- |
| Driver OOM       | `collect` 大结果、广播超大对象、任务元数据过多    |
| Executor OOM     | 单分区数据过大、Join 倾斜、缓存过多、聚合状态过大 |
| GC 过长          | 对象创建过多、缓存不合理、内存不足                |
| Shuffle 溢写严重 | Shuffle 数据量大，内存不足                        |
| Container killed | YARN 内存超限，memoryOverhead 不足                |

避免 Driver OOM：

```java
// 不推荐：大数据量会拉回 Driver
List<Row> rows = resultDf.collectAsList();

// 推荐：只获取少量样例
List<Row> sampleRows = resultDf.limit(100).collectAsList();
```

Executor 内存相关配置：

```bash
--executor-memory 4g
--conf spark.executor.memoryOverhead=1g
--conf spark.memory.fraction=0.6
--conf spark.memory.storageFraction=0.5
```

内存优化建议如下：

| 建议                    | 说明                                 |
| ----------------------- | ------------------------------------ |
| 避免大规模 collect      | 大数据处理留在 Executor              |
| 增加分区数              | 降低单 Task 数据量                   |
| Join 前裁剪字段         | 减少内存占用                         |
| 缓存前过滤和裁剪        | 不缓存大宽表                         |
| 用完及时 unpersist      | 释放缓存内存                         |
| 检查数据倾斜            | 单个热点 Key 会导致 Executor OOM     |
| 合理设置 memoryOverhead | Kafka、PySpark、对象存储场景尤其重要 |

### 并行度优化

并行度决定同一时间可执行的 Task 数量和数据处理粒度。并行度过低会造成资源闲置，过高会增加调度开销。Spark SQL 中常见并行度由输入文件分片、Shuffle 分区数、repartition 和 Executor 总核数共同决定。

查看默认并行度：

```java
int defaultParallelism = JavaSparkContext.fromSparkContext(spark.sparkContext()).defaultParallelism();
log.info("Spark 默认并行度：{}", defaultParallelism);
```

设置 Shuffle 并行度：

```bash
--conf spark.sql.shuffle.partitions=400
```

RDD 默认并行度：

```bash
--conf spark.default.parallelism=400
```

调整 DataFrame 分区：

```java
Dataset<Row> repartitionDf = sourceDf.repartition(400);
```

并行度优化建议如下：

| 建议                     | 说明                                |
| ------------------------ | ----------------------------------- |
| 并行度大于总核数         | 通常为总核数的 2 到 4 倍            |
| Shuffle 分区按数据量调整 | 大数据增大，小数据降低              |
| 本地调试降低分区数       | 避免生成大量无意义 Task             |
| 避免过度 repartition     | 每次 repartition 都可能产生 Shuffle |
| 输出前降低并行度         | 控制输出文件数量                    |
| 结合 Spark UI 调整       | 看 Task 耗时和资源利用率            |

并行度估算示例：

```text
Executor 数量：8
每个 Executor 核数：2
总核数：16
建议初始 Shuffle 分区数：16 × 3 = 48

如果数据量较大，可设置为 200、400 或更高，并结合 Spark UI 调整。
```

### SQL 执行计划分析

SQL 执行计划分析是 Spark 性能优化的重要手段。通过 `explain` 可以查看逻辑计划、优化后的逻辑计划和物理执行计划，判断是否发生分区裁剪、列裁剪、广播 Join、SortMergeJoin、Shuffle、Filter 下推等。

查看执行计划：

```java
Dataset<Row> resultDf = spark.sql(sql);

resultDf.explain("formatted");
```

常见执行计划关键字：

| 关键字              | 说明           |
| ------------------- | -------------- |
| `FileScan`          | 文件扫描       |
| `PushedFilters`     | 谓词下推       |
| `PartitionFilters`  | 分区裁剪       |
| `BroadcastHashJoin` | 广播 Hash Join |
| `SortMergeJoin`     | 排序合并 Join  |
| `Exchange`          | Shuffle        |
| `HashAggregate`     | Hash 聚合      |
| `SortAggregate`     | 排序聚合       |
| `Window`            | 窗口函数       |
| `AdaptiveSparkPlan` | AQE 自适应计划 |

示例：检查分区裁剪和谓词下推：

```sql
SELECT
    user_id,
    event_type
FROM dwd.dwd_user_event
WHERE dt = '${bizDate}'
  AND event_type = 'pay'
```

执行计划中应重点观察：

```text
PartitionFilters: [isnotnull(dt), (dt = 2026-05-11)]
PushedFilters: [IsNotNull(event_type), EqualTo(event_type,pay)]
```

SQL 执行计划分析建议如下：

| 建议                        | 说明                                 |
| --------------------------- | ------------------------------------ |
| 大 SQL 上线前必须看执行计划 | 防止全表扫描和异常 Join              |
| 关注 `Exchange` 数量        | Exchange 表示 Shuffle                |
| 关注 Join 策略              | 小表是否广播，大表是否 SortMergeJoin |
| 关注 PartitionFilters       | 分区表是否命中分区裁剪               |
| 关注 PushedFilters          | 过滤条件是否下推                     |
| 关注 AdaptiveSparkPlan      | 确认 AQE 是否生效                    |
| 结合 Spark UI 验证          | 执行计划和实际任务指标一起分析       |

常见 SQL 优化前后对比：

```sql
-- 不推荐：字段过多，且未显式裁剪
SELECT *
FROM dwd.dwd_order_detail o
LEFT JOIN dim.dim_user_current u
    ON o.user_id = u.user_id
WHERE o.dt = '${bizDate}';

-- 推荐：先裁剪字段，再 Join
WITH order_base AS (
    SELECT
        order_id,
        user_id,
        order_amount,
        dt
    FROM dwd.dwd_order_detail
    WHERE dt = '${bizDate}'
),
user_base AS (
    SELECT
        user_id,
        user_level
    FROM dim.dim_user_current
)
SELECT
    o.order_id,
    o.user_id,
    u.user_level,
    o.order_amount,
    o.dt
FROM order_base o
LEFT JOIN user_base u
    ON o.user_id = u.user_id;
```

性能优化的基本顺序是：先确认数据范围和业务逻辑，再看执行计划，然后分析 Spark UI，最后调整 SQL、分区、Join 策略、缓存和资源参数。不要在没有定位瓶颈的情况下直接增加 Executor、内存或队列资源。

## 资源管理

本章节用于说明 Spark 任务运行时的 Driver、Executor、CPU、内存、动态资源、队列、隔离和监控配置。资源管理的目标不是单纯增加资源，而是让任务在合理资源范围内稳定运行，并能通过监控数据持续优化。

### Driver 资源配置

Driver 负责 Spark 应用的调度、任务切分、执行计划生成、元数据管理和结果汇总。Driver 不负责大规模数据计算，但如果代码中存在大量 `collect()`、广播大对象、超大执行计划或大量小文件元数据，也会导致 Driver 内存压力过大。

Driver 常用配置如下：

| 参数                          | 示例   | 说明                                 |
| ----------------------------- | ------ | ------------------------------------ |
| `--driver-memory`             | `2g`   | Driver JVM 堆内存                    |
| `spark.driver.memoryOverhead` | `512m` | Driver 堆外内存                      |
| `spark.driver.cores`          | `1`    | Driver 使用 CPU 核数，K8s 模式更常用 |
| `spark.driver.maxResultSize`  | `1g`   | Driver 允许接收的最大结果大小        |

YARN Cluster 模式示例：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --name spark-user-summary-prod \
  --driver-memory 2g \
  --conf spark.driver.memoryOverhead=512m \
  --conf spark.driver.maxResultSize=1g \
  target/spark-job.jar \
  --jobName user-summary \
  --env prod \
  --bizDate 2026-05-11
```

Driver 资源配置建议如下：

| 建议                   | 说明                                 |
| ---------------------- | ------------------------------------ |
| 避免大规模 `collect`   | 大结果拉回 Driver 容易 OOM           |
| 小结果使用 `limit`     | 调试和配置读取只取少量数据           |
| 审计信息在 Driver 处理 | 只处理轻量级状态和计数               |
| 大广播对象谨慎使用     | 广播变量过大也会占用 Driver 内存     |
| SQL 过长需拆分         | 超复杂 SQL 可能导致执行计划庞大      |
| 小文件过多会压 Driver  | 文件元数据过多会增加 Driver 调度压力 |

不推荐写法：

```java
// 不推荐：大数据量 collect 会把所有数据拉到 Driver，容易导致 Driver OOM
List<Row> rows = resultDf.collectAsList();
```

推荐写法：

```java
// 推荐：只拉取少量样例数据用于调试
List<Row> sampleRows = resultDf.limit(100).collectAsList();
```

### Executor 资源配置

Executor 负责实际计算任务，包括读取数据、执行转换、Join、聚合、Shuffle、缓存和写出。Executor 的资源配置直接影响任务吞吐、稳定性和并行度。

Executor 常用配置如下：

| 参数                            | 示例 | 说明                                   |
| ------------------------------- | ---- | -------------------------------------- |
| `--num-executors`               | `8`  | Executor 数量，YARN 常用               |
| `--executor-memory`             | `4g` | 每个 Executor JVM 堆内存               |
| `--executor-cores`              | `2`  | 每个 Executor 使用 CPU 核数            |
| `spark.executor.memoryOverhead` | `1g` | Executor 堆外内存                      |
| `spark.executor.instances`      | `8`  | Executor 实例数，K8s / Standalone 常用 |

YARN 提交示例：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --name spark-user-summary-prod \
  --queue root.prod.etl \
  --driver-memory 2g \
  --executor-memory 4g \
  --executor-cores 2 \
  --num-executors 8 \
  --conf spark.executor.memoryOverhead=1g \
  --conf spark.sql.shuffle.partitions=400 \
  target/spark-job.jar \
  --jobName user-summary \
  --env prod \
  --bizDate 2026-05-11
```

Executor 配置建议如下：

| 建议                         | 说明                                     |
| ---------------------------- | ---------------------------------------- |
| `executor-cores` 不宜过大    | 常见设置为 2 到 5                        |
| 单 Executor 内存不宜盲目加大 | 先分析是否分区过少或数据倾斜             |
| Executor 数量与队列资源匹配  | 避免申请资源长期等待                     |
| 堆外内存适当预留             | Shuffle、Netty、对象存储、Kafka 场景需要 |
| 任务稳定后固化资源配置       | 生产任务资源参数应版本化                 |
| 不同任务独立配置             | 大宽表、小指标、流任务资源模型不同       |

### CPU 配置

CPU 配置主要由 Executor 数量、每个 Executor 核数和总并行度决定。Spark 中一个 Task 通常占用一个 CPU Core。CPU 配置过低会导致任务并行度不足；配置过高但分区数不足，则会造成资源闲置。

CPU 相关参数如下：

| 参数                           | 示例  | 说明                   |
| ------------------------------ | ----- | ---------------------- |
| `--executor-cores`             | `2`   | 每个 Executor 使用核数 |
| `--num-executors`              | `8`   | Executor 数量          |
| `spark.task.cpus`              | `1`   | 每个 Task 占用 CPU 数  |
| `spark.default.parallelism`    | `400` | RDD 默认并行度         |
| `spark.sql.shuffle.partitions` | `400` | SQL Shuffle 并行度     |

总核数计算方式：

```text
总 CPU 核数 = num-executors × executor-cores
```

示例：

```text
num-executors = 8
executor-cores = 2
总 CPU 核数 = 16
```

CPU 配置建议如下：

| 场景              | 建议                                        |
| ----------------- | ------------------------------------------- |
| 普通离线 SQL 任务 | `executor-cores` 设置为 2 到 4              |
| 大 Shuffle 任务   | 增加 Executor 数量，并合理提高 Shuffle 分区 |
| CPU 密集型 UDF    | 控制每个 Executor 核数，避免 GC 和 CPU 抢占 |
| IO 密集型任务     | 适当增加并行度，提高读取吞吐                |
| 本地调试          | 使用 `local[2]` 或 `local[*]`               |
| 生产任务          | 总 Task 数应明显大于总核数                  |

CPU 配置不应孤立调整。增加 CPU 后，如果分区数不足，任务仍然无法充分并行；如果单个 Task 数据倾斜，增加 CPU 也无法解决热点 Task 过慢问题。

### 内存配置

内存配置用于控制 Driver 和 Executor 的堆内存、堆外内存、Shuffle 内存、缓存内存等。Spark 内存问题通常表现为 Executor OOM、Driver OOM、Container killed、GC 时间过长、Shuffle Spill 严重等。

常用内存配置如下：

| 参数                            | 示例   | 说明                              |
| ------------------------------- | ------ | --------------------------------- |
| `--driver-memory`               | `2g`   | Driver 堆内存                     |
| `--executor-memory`             | `4g`   | Executor 堆内存                   |
| `spark.driver.memoryOverhead`   | `512m` | Driver 堆外内存                   |
| `spark.executor.memoryOverhead` | `1g`   | Executor 堆外内存                 |
| `spark.memory.fraction`         | `0.6`  | Spark 执行和存储内存比例          |
| `spark.memory.storageFraction`  | `0.5`  | 存储内存在 Spark 内存中的初始占比 |

配置示例：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --driver-memory 2g \
  --executor-memory 4g \
  --executor-cores 2 \
  --num-executors 8 \
  --conf spark.executor.memoryOverhead=1g \
  --conf spark.driver.memoryOverhead=512m \
  target/spark-job.jar \
  --jobName user-summary \
  --env prod \
  --bizDate 2026-05-11
```

内存问题处理建议如下：

| 问题             | 处理方式                                       |
| ---------------- | ---------------------------------------------- |
| Driver OOM       | 检查 `collect`、广播对象、执行计划、小文件数量 |
| Executor OOM     | 增加分区、裁剪字段、处理倾斜、减少缓存         |
| Container killed | 增加 memoryOverhead，检查堆外内存使用          |
| GC 时间长        | 减少缓存、降低单 Task 数据量、调整分区         |
| Shuffle Spill 多 | 增加内存、优化 Shuffle、减少数据宽度           |
| 缓存失败         | 改用 `MEMORY_AND_DISK` 或减少缓存数据量        |

内存优化优先级建议为：先减少单 Task 数据量和无关字段，再处理数据倾斜和缓存策略，最后再增加 Executor 内存。

### 动态资源分配

动态资源分配用于根据任务负载自动申请和释放 Executor。它适合负载波动较大的任务，可以提升集群资源利用率。但对实时流任务、资源隔离要求严格的任务和启动延迟敏感任务，需要谨慎使用。

常用配置如下：

```bash
--conf spark.dynamicAllocation.enabled=true \
--conf spark.dynamicAllocation.minExecutors=2 \
--conf spark.dynamicAllocation.initialExecutors=4 \
--conf spark.dynamicAllocation.maxExecutors=20 \
--conf spark.shuffle.service.enabled=true
```

参数说明：

| 参数                                          | 说明                              |
| --------------------------------------------- | --------------------------------- |
| `spark.dynamicAllocation.enabled`             | 是否开启动态资源分配              |
| `spark.dynamicAllocation.minExecutors`        | 最小 Executor 数                  |
| `spark.dynamicAllocation.initialExecutors`    | 初始 Executor 数                  |
| `spark.dynamicAllocation.maxExecutors`        | 最大 Executor 数                  |
| `spark.shuffle.service.enabled`               | 是否开启 External Shuffle Service |
| `spark.dynamicAllocation.executorIdleTimeout` | Executor 空闲释放时间             |

动态资源配置示例：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --queue root.prod.etl \
  --driver-memory 2g \
  --executor-memory 4g \
  --executor-cores 2 \
  --conf spark.dynamicAllocation.enabled=true \
  --conf spark.dynamicAllocation.minExecutors=2 \
  --conf spark.dynamicAllocation.initialExecutors=4 \
  --conf spark.dynamicAllocation.maxExecutors=20 \
  --conf spark.shuffle.service.enabled=true \
  target/spark-job.jar \
  --jobName user-summary \
  --env prod \
  --bizDate 2026-05-11
```

动态资源分配建议如下：

| 建议                         | 说明                                  |
| ---------------------------- | ------------------------------------- |
| 批处理任务可以使用           | 对阶段性资源波动较友好                |
| 流任务谨慎使用               | Executor 频繁变化可能影响稳定性       |
| 设置合理上下限               | 防止资源申请过多或过少                |
| 需要集群支持 Shuffle Service | YARN 环境通常需要外部 Shuffle 服务    |
| 与队列资源配额匹配           | 最大 Executor 不应超过队列承载能力    |
| 观察实际资源变化             | 通过 Spark UI 和 YARN UI 确认是否生效 |

### 队列配置

队列配置用于控制 Spark 任务提交到哪个资源队列。企业集群通常会按团队、业务线、任务类型或优先级划分 YARN 队列。队列配置不应写死在代码中，应由提交脚本或调度平台控制。

YARN 队列提交示例：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --queue root.prod.etl \
  --name spark-user-summary-prod \
  --driver-memory 2g \
  --executor-memory 4g \
  --executor-cores 2 \
  --num-executors 8 \
  target/spark-job.jar \
  --jobName user-summary \
  --env prod \
  --bizDate 2026-05-11
```

常见队列划分方式如下：

| 队列                 | 说明               |
| -------------------- | ------------------ |
| `root.prod.etl`      | 生产离线 ETL 任务  |
| `root.prod.realtime` | 生产实时任务       |
| `root.test.etl`      | 测试离线任务       |
| `root.adhoc`         | 临时查询和开发任务 |
| `root.backfill`      | 补数任务           |

队列配置建议如下：

| 建议                 | 说明                           |
| -------------------- | ------------------------------ |
| 生产和测试分队列     | 防止测试任务占用生产资源       |
| 日常任务和补数分队列 | 防止大规模补数影响日常调度     |
| 实时任务使用独立队列 | 保证实时链路资源稳定           |
| 临时任务限制资源     | 防止临时查询抢占核心任务资源   |
| 队列权限提前申请     | 任务账号必须有目标队列提交权限 |
| 队列使用写入审计日志 | 便于排查资源等待问题           |

### 资源隔离

资源隔离用于避免不同环境、不同业务线、不同优先级任务之间互相影响。资源隔离可以通过 YARN 队列、Kubernetes Namespace、任务账号、HDFS 路径、Hive 库、Kafka Topic 和对象存储 Bucket 等方式实现。

常见资源隔离维度如下：

| 维度       | 隔离方式                                      |
| ---------- | --------------------------------------------- |
| 计算资源   | YARN 队列、K8s Namespace                      |
| 存储资源   | HDFS 根目录、对象存储 Bucket / Prefix         |
| 元数据     | Hive Database、Catalog                        |
| 消息队列   | Kafka Topic、Consumer Group                   |
| 权限账号   | Kerberos Principal、数据库账号、对象存储 Role |
| 配置文件   | local/test/prod 分环境配置                    |
| Checkpoint | 按环境和任务独立路径                          |

资源隔离路径示例：

```text
hdfs:///warehouse/prod/dwd/user/dwd_user_event
hdfs:///warehouse/test/dwd/user/dwd_user_event

hdfs:///checkpoint/prod/user-event-clean
hdfs:///checkpoint/test/user-event-clean

s3a://data-prod/warehouse/dwd/user/dwd_user_event
s3a://data-test/warehouse/dwd/user/dwd_user_event
```

资源隔离建议如下：

| 建议               | 说明                                |
| ------------------ | ----------------------------------- |
| 环境必须隔离       | local、test、prod 不共用数据目录    |
| 生产账号独立       | 不使用个人账号运行生产任务          |
| Checkpoint 独立    | 测试和生产流任务不能共用 Checkpoint |
| Kafka Topic 分环境 | 防止测试消费或写入生产消息          |
| 补数资源单独规划   | 防止补数任务冲击日常任务            |
| 权限最小化         | 每个任务账号只授权必要资源          |

### 资源监控

资源监控用于观察 Spark 任务的资源使用、任务耗时、失败原因和性能瓶颈。常见监控入口包括 Spark UI、Spark History Server、YARN UI、Kubernetes Dashboard、日志平台和调度平台。

常见监控指标如下：

| 指标                 | 说明                               |
| -------------------- | ---------------------------------- |
| Application 状态     | RUNNING、SUCCEEDED、FAILED、KILLED |
| Executor 数量        | 当前 Executor 数和动态变化         |
| CPU 使用             | Task 并行度和资源利用率            |
| 内存使用             | Executor 内存、缓存、GC            |
| Shuffle Read / Write | Shuffle 数据规模                   |
| Spill                | 内存溢写和磁盘溢写                 |
| Task Duration        | Task 耗时分布                      |
| Failed Tasks         | 失败 Task 数量                     |
| Input / Output       | 读取和写出数据量                   |
| Streaming Lag        | 流任务输入速率、处理速率、延迟     |

查看 YARN 应用：

```bash
# 查看运行中的应用
yarn application -list

# 查看应用状态
yarn application -status application_1710000000000_0001

# 查看应用日志
yarn logs -applicationId application_1710000000000_0001
```

代码中记录 Spark 应用信息：

```java
log.info("Spark 应用信息，appName={}，applicationId={}，master={}",
        spark.sparkContext().appName(),
        spark.sparkContext().applicationId(),
        spark.sparkContext().master());
```

资源监控建议如下：

| 建议                        | 说明                                                      |
| --------------------------- | --------------------------------------------------------- |
| 每个任务记录 Application ID | 便于关联 Spark UI 和 YARN 日志                            |
| 关注最慢 Stage              | 性能优化从瓶颈 Stage 开始                                 |
| 关注 Task 倾斜              | 个别 Task 极慢通常是数据倾斜                              |
| 关注 Shuffle 数据量         | Shuffle 是性能瓶颈高发点                                  |
| 关注 Executor OOM           | 判断是资源不足还是数据问题                                |
| 流任务监控延迟              | inputRowsPerSecond、processedRowsPerSecond、batchDuration |
| 审计表记录资源参数          | 便于对比不同版本性能                                      |

## 数据质量

本章节用于规范 Spark 任务中的数据质量校验。数据质量不应只依赖下游发现问题，而应在任务处理过程中主动校验完整性、唯一性、准确性、一致性、时效性、波动性，并将脏数据和校验结果单独输出。

### 数据完整性校验

数据完整性校验用于检查关键字段是否为空、关键分区是否存在、输入数据是否为空、输出数据是否为空。完整性问题通常会直接影响下游 Join、指标计算和报表展示。

常见完整性规则如下：

| 校验项       | 示例                             |
| ------------ | -------------------------------- |
| 数据集非空   | 输入表指定分区必须有数据         |
| 主键非空     | `user_id`、`order_id` 不允许为空 |
| 分区字段非空 | `dt` 不允许为空                  |
| 事件时间非空 | `event_time` 不允许为空          |
| 金额字段非空 | 支付成功订单金额不允许为空       |

文件位置：`src/main/java/io/github/atengk/spark/quality/DataCompletenessChecker.java`

该工具类用于校验数据完整性。

```java
package io.github.atengk.spark.quality;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.util.List;

import static org.apache.spark.sql.functions.col;

/**
 * 数据完整性校验工具。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class DataCompletenessChecker {

    private DataCompletenessChecker() {
    }

    /**
     * 校验数据集不能为空。
     *
     * @param dataset 数据集
     * @param datasetName 数据集名称
     */
    public static void checkNotEmpty(Dataset<Row> dataset, String datasetName) {
        long count = dataset.count();
        log.info("完整性校验：{} 数据量为 {}", datasetName, count);

        if (count == 0) {
            throw new IllegalStateException(StrUtil.format("{} 数据为空", datasetName));
        }
    }

    /**
     * 校验必填字段不能为空。
     *
     * @param dataset 数据集
     * @param datasetName 数据集名称
     * @param requiredColumns 必填字段
     */
    public static void checkRequiredColumns(Dataset<Row> dataset, String datasetName, List<String> requiredColumns) {
        if (CollUtil.isEmpty(requiredColumns)) {
            log.warn("完整性校验：{} 未配置必填字段", datasetName);
            return;
        }

        for (String column : requiredColumns) {
            long nullCount = dataset.filter(col(column).isNull()).count();
            log.info("完整性校验：{} 字段 {} 空值数量为 {}", datasetName, column, nullCount);

            if (nullCount > 0) {
                throw new IllegalStateException(StrUtil.format("{} 字段 {} 存在空值，数量：{}", datasetName, column, nullCount));
            }
        }
    }
}
```

使用示例：

```java
DataCompletenessChecker.checkNotEmpty(resultDf, "用户汇总结果");
DataCompletenessChecker.checkRequiredColumns(resultDf, "用户汇总结果", Arrays.asList("user_id", "dt"));
```

完整性校验建议如下：

| 建议                 | 说明                             |
| -------------------- | -------------------------------- |
| 输入和输出都要校验   | 防止上游为空或处理后为空         |
| 主键字段必须非空     | 主键为空会影响 Join 和去重       |
| 分区字段必须非空     | 避免写出 `dt=null` 分区          |
| 关键字段空值应失败   | 不建议静默填充核心字段           |
| 非核心维度可默认填充 | 例如城市、渠道可以填充 `UNKNOWN` |

### 数据唯一性校验

数据唯一性校验用于检查主键、业务唯一键、事件 ID、订单 ID 等字段组合是否重复。唯一性问题会造成指标重复计算、宽表膨胀、JDBC 主键冲突和下游数据不一致。

常见唯一性规则如下：

| 数据       | 唯一键                                         |
| ---------- | ---------------------------------------------- |
| 用户表     | `user_id`                                      |
| 订单表     | `order_id`                                     |
| 事件表     | `event_id`                                     |
| 用户日汇总 | `user_id + dt`                                 |
| 渠道日指标 | `channel + dt`                                 |
| Kafka 消息 | `topic + partition + offset` 或业务 `event_id` |

文件位置：`src/main/java/io/github/atengk/spark/quality/DataUniqueChecker.java`

该工具类用于校验字段组合唯一性。

```java
package io.github.atengk.spark.quality;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

/**
 * 数据唯一性校验工具。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class DataUniqueChecker {

    private DataUniqueChecker() {
    }

    /**
     * 校验字段组合唯一。
     *
     * @param dataset 数据集
     * @param datasetName 数据集名称
     * @param uniqueColumns 唯一键字段
     */
    public static void checkUnique(Dataset<Row> dataset, String datasetName, String... uniqueColumns) {
        if (ArrayUtil.isEmpty(uniqueColumns)) {
            throw new IllegalArgumentException("唯一性校验字段不能为空");
        }

        long totalCount = dataset.count();
        long uniqueCount = dataset.dropDuplicates(uniqueColumns).count();
        long duplicateCount = totalCount - uniqueCount;

        log.info("唯一性校验：{} 总数={}，唯一数={}，重复数={}，唯一键={}",
                datasetName, totalCount, uniqueCount, duplicateCount, String.join(",", uniqueColumns));

        if (duplicateCount > 0) {
            throw new IllegalStateException(StrUtil.format("{} 存在重复数据，重复数量：{}", datasetName, duplicateCount));
        }
    }
}
```

查出重复数据示例：

```java
Dataset<Row> duplicateDf = resultDf
        .groupBy("user_id", "dt")
        .count()
        .filter(col("count").gt(1));

duplicateDf.show(20, false);
```

唯一性校验建议如下：

| 建议                   | 说明                       |
| ---------------------- | -------------------------- |
| 输出表必须明确唯一键   | 指标表和宽表尤其重要       |
| 写 JDBC 前先校验唯一性 | 防止主键冲突               |
| 去重规则要业务明确     | 保留最新、最早还是任意一条 |
| 重复数据可单独输出     | 便于追踪上游重复问题       |
| 不滥用全字段 distinct  | 应按业务唯一键处理         |

### 数据准确性校验

数据准确性校验用于检查字段值是否符合业务规则，例如金额不能为负、年龄范围合法、状态枚举合法、时间格式正确、数量不能小于 0 等。

常见准确性规则如下：

| 字段           | 校验规则          |
| -------------- | ----------------- |
| `age`          | 0 到 150          |
| `order_amount` | 大于等于 0        |
| `pay_status`   | 必须在枚举范围内  |
| `event_time`   | 必须能转换为时间  |
| `mobile`       | 符合手机号格式    |
| `email`        | 符合邮箱格式      |
| `dt`           | 符合 `yyyy-MM-dd` |

准确性校验示例：

```java
Dataset<Row> invalidDf = sourceDf
        .filter(
                col("age").lt(0)
                        .or(col("age").gt(150))
                        .or(col("order_amount").lt(0))
                        .or(not(col("pay_status").isin("PAID", "UNPAID", "REFUND")))
        );

long invalidCount = invalidDf.count();
log.info("准确性校验完成，异常数据量：{}", invalidCount);

if (invalidCount > 0) {
    throw new IllegalStateException("存在准确性异常数据，数量：" + invalidCount);
}
```

封装枚举校验示例：

```java
Dataset<Row> statusInvalidDf = orderDf
        .filter(not(col("pay_status").isin("PAID", "UNPAID", "REFUND")));

long statusInvalidCount = statusInvalidDf.count();
log.info("订单状态准确性校验，非法状态数量：{}", statusInvalidCount);
```

准确性校验建议如下：

| 建议                    | 说明                               |
| ----------------------- | ---------------------------------- |
| 枚举字段使用白名单      | 状态、类型、渠道等字段必须控制范围 |
| 金额使用 Decimal        | 避免 double 精度问题               |
| 时间字段转换后校验 null | 转换失败通常产生 null              |
| 异常数据单独落地        | 不建议只打印日志                   |
| 核心指标字段异常应失败  | 例如支付金额为负数应阻断任务       |

### 数据一致性校验

数据一致性校验用于检查同一条记录中多个字段之间、不同表之间或上下游之间是否符合约束。例如支付状态为已支付时支付时间不能为空，订单明细金额之和应等于订单主表金额，事实表用户 ID 应能关联到用户维表。

常见一致性规则如下：

| 规则           | 说明                           |
| -------------- | ------------------------------ |
| 状态与时间一致 | 已支付订单必须有支付时间       |
| 主从表金额一致 | 订单主表金额等于明细金额之和   |
| 事实与维度一致 | 事实表用户 ID 能关联到用户维表 |
| 分区与时间一致 | `dt` 应等于事件时间日期        |
| 上下游数量一致 | DWD 输入输出数量在合理范围内   |

状态与时间一致性校验：

```java
Dataset<Row> invalidPayDf = orderDf
        .filter(col("pay_status").equalTo("PAID").and(col("pay_time").isNull()));

long invalidPayCount = invalidPayDf.count();
log.info("一致性校验：已支付但支付时间为空数量={}", invalidPayCount);

if (invalidPayCount > 0) {
    throw new IllegalStateException("存在已支付但支付时间为空的订单，数量：" + invalidPayCount);
}
```

事实表与维表关联一致性校验：

```java
Dataset<Row> missUserDf = orderDf
        .join(userDimDf, orderDf.col("user_id").equalTo(userDimDf.col("user_id")), "left_anti");

long missUserCount = missUserDf.count();
log.info("一致性校验：订单无法关联用户维度数量={}", missUserCount);
```

分区与事件时间一致性校验：

```java
Dataset<Row> invalidDtDf = eventDf
        .withColumn("event_dt", date_format(col("event_time"), "yyyy-MM-dd"))
        .filter(not(col("dt").equalTo(col("event_dt"))));

long invalidDtCount = invalidDtDf.count();
log.info("一致性校验：事件日期与分区日期不一致数量={}", invalidDtCount);
```

一致性校验建议如下：

| 建议                   | 说明                             |
| ---------------------- | -------------------------------- |
| 关键业务约束必须校验   | 状态、时间、金额、分区等         |
| 维度关联失败要统计     | 不一定全部失败，但必须可见       |
| 金额类一致性要考虑精度 | 使用 Decimal 并明确小数位        |
| 分区日期与事件时间一致 | 防止数据写错分区                 |
| 一致性阈值可配置       | 维度缺失可设置告警阈值或失败阈值 |

### 数据时效性校验

数据时效性校验用于检查数据是否按预期时间到达、事件时间是否在合理范围内、处理延迟是否超过阈值。离线任务关注分区是否及时产出，实时任务关注事件延迟、处理延迟和 Kafka Lag。

常见时效性规则如下：

| 场景       | 校验项                         |
| ---------- | ------------------------------ |
| 离线任务   | 指定 `bizDate` 分区是否存在    |
| 增量任务   | `update_time` 是否落在处理窗口 |
| 实时任务   | 事件时间与处理时间延迟         |
| Kafka 任务 | 消费 Lag 是否过大              |
| 宽表任务   | 上游维表是否已刷新             |
| 报表任务   | ADS 表是否在 SLA 前产出        |

离线分区时效性校验：

```java
Dataset<Row> sourceDf = spark.table("dwd.dwd_user_event")
        .filter(col("dt").equalTo(bizDate));

long inputCount = sourceDf.count();
log.info("时效性校验：table={}，bizDate={}，inputCount={}",
        "dwd.dwd_user_event", bizDate, inputCount);

if (inputCount == 0) {
    throw new IllegalStateException("上游分区未产出数据，bizDate=" + bizDate);
}
```

实时延迟计算示例：

```java
Dataset<Row> delayDf = eventDf
        .withColumn("process_time", current_timestamp())
        .withColumn("delay_seconds", unix_timestamp(col("process_time")).minus(unix_timestamp(col("event_time"))));

Dataset<Row> timeoutDf = delayDf.filter(col("delay_seconds").gt(600));

long timeoutCount = timeoutDf.count();
log.info("时效性校验：延迟超过 10 分钟的数据量={}", timeoutCount);
```

时效性校验建议如下：

| 建议                 | 说明                         |
| -------------------- | ---------------------------- |
| 离线任务校验上游分区 | 上游未到齐不应继续下游计算   |
| 实时任务监控处理延迟 | 关注事件时间与处理时间差     |
| 增量任务校验时间窗口 | 防止漏读或重复读取           |
| SLA 指标写入审计     | 记录任务开始、结束和产出时间 |
| 延迟阈值可配置       | 不同业务链路时效要求不同     |

### 数据波动监控

数据波动监控用于检查当前批次数据量、金额、用户数、订单数等指标是否相对历史发生异常变化。波动不一定是错误，但应记录、告警或阻断，具体取决于业务重要性。

常见波动指标如下：

| 指标       | 说明             |
| ---------- | ---------------- |
| 输入数据量 | 当前分区输入条数 |
| 输出数据量 | 当前分区输出条数 |
| 支付金额   | 当前日支付总额   |
| 活跃用户数 | 当前日活跃用户数 |
| 订单数     | 当前日订单数量   |
| 异常数据量 | 脏数据数量       |
| 维度缺失率 | Join 未匹配比例  |

波动率计算公式：

```text
波动率 = abs(当前值 - 历史均值) / 历史均值
```

波动监控示例：

```java
long currentCount = resultDf.count();
long historyAvgCount = 1000000L;

double fluctuationRate = Math.abs(currentCount - historyAvgCount) * 1.0 / historyAvgCount;

log.info("数据波动监控：currentCount={}，historyAvgCount={}，fluctuationRate={}",
        currentCount, historyAvgCount, fluctuationRate);

if (fluctuationRate > 0.5) {
    throw new IllegalStateException("数据量波动超过阈值，当前波动率：" + fluctuationRate);
}
```

实际项目中，历史均值应来自审计表或指标监控表，而不是写死在代码中。

审计表查询历史均值思路：

```sql
SELECT
    AVG(output_count) AS avg_output_count
FROM audit.spark_job_audit
WHERE job_name = 'user-summary'
  AND status = 'SUCCESS'
  AND biz_date >= date_sub('${bizDate}', 7)
  AND biz_date < '${bizDate}'
```

数据波动监控建议如下：

| 建议                     | 说明                    |
| ------------------------ | ----------------------- |
| 使用历史窗口对比         | 常见为近 7 天、近 14 天 |
| 区分工作日和节假日       | 节假日波动可能正常      |
| 设置告警阈值和失败阈值   | 小波动告警，大波动阻断  |
| 核心指标必须监控         | 金额、订单数、用户数等  |
| 波动结果写入审计         | 便于长期趋势分析        |
| 首次运行允许跳过历史对比 | 没有历史数据时不能误判  |

### 脏数据处理

脏数据处理用于将不符合规则的数据从主链路中剥离，并输出到错误路径、错误表或审计系统。脏数据不建议直接丢弃，否则后续无法追踪上游问题。

常见脏数据类型如下：

| 类型     | 示例                        |
| -------- | --------------------------- |
| 字段缺失 | 主键为空、事件时间为空      |
| 格式错误 | 时间格式错误、JSON 解析失败 |
| 枚举非法 | 状态值不在白名单内          |
| 数值异常 | 金额为负、年龄越界          |
| 重复数据 | 主键或事件 ID 重复          |
| 关联失败 | 维度表无法匹配              |
| 分区异常 | 分区日期为空或格式错误      |

脏数据标记示例：

```java
Dataset<Row> checkedDf = sourceDf.withColumn(
        "error_reason",
        when(col("user_id").isNull(), lit("用户ID为空"))
                .when(col("event_time").isNull(), lit("事件时间为空"))
                .when(not(col("event_type").isin("click", "view", "pay")), lit("事件类型非法"))
                .otherwise(lit(null))
);

Dataset<Row> validDf = checkedDf.filter(col("error_reason").isNull()).drop("error_reason");

Dataset<Row> dirtyDf = checkedDf.filter(col("error_reason").isNotNull());
```

脏数据写出示例：

```java
dirtyDf.write()
        .mode(SaveMode.Append)
        .partitionBy("dt")
        .parquet("hdfs:///error/prod/user_event_dirty");
```

脏数据处理建议如下：

| 建议                     | 说明                   |
| ------------------------ | ---------------------- |
| 脏数据单独落地           | 不直接丢弃             |
| 增加错误原因字段         | 便于上游修复           |
| 保留原始字段             | 脏数据表应保留原始记录 |
| 按日期分区               | 方便按批次排查         |
| 设置脏数据阈值           | 超过阈值应失败或告警   |
| 脏数据路径与正式路径隔离 | 避免被下游误读         |

### 校验结果输出

校验结果输出用于将每次任务的数据质量结果结构化保存，便于审计、监控、告警和趋势分析。校验结果可以写入 Hive 审计表、MySQL 审计表、日志平台或监控系统。

推荐校验结果字段如下：

| 字段            | 说明                     |
| --------------- | ------------------------ |
| `job_name`      | 任务名称                 |
| `biz_date`      | 业务日期                 |
| `batch_id`      | 批次号                   |
| `dataset_name`  | 数据集名称               |
| `rule_name`     | 校验规则名称             |
| `rule_type`     | 完整性、唯一性、准确性等 |
| `check_status`  | PASS、WARN、FAILED       |
| `total_count`   | 总数据量                 |
| `invalid_count` | 异常数据量               |
| `threshold`     | 阈值                     |
| `error_message` | 错误说明                 |
| `check_time`    | 校验时间                 |

Hive 校验结果表示例：

```sql
CREATE TABLE IF NOT EXISTS audit.data_quality_check_result (
    job_name STRING COMMENT '任务名称',
    biz_date STRING COMMENT '业务日期',
    batch_id STRING COMMENT '批次号',
    dataset_name STRING COMMENT '数据集名称',
    rule_name STRING COMMENT '规则名称',
    rule_type STRING COMMENT '规则类型',
    check_status STRING COMMENT '校验状态',
    total_count BIGINT COMMENT '总数据量',
    invalid_count BIGINT COMMENT '异常数据量',
    threshold STRING COMMENT '阈值',
    error_message STRING COMMENT '错误信息',
    check_time TIMESTAMP COMMENT '校验时间'
)
COMMENT '数据质量校验结果表'
PARTITIONED BY (dt STRING COMMENT '分区日期')
STORED AS PARQUET;
```

文件位置：`src/main/java/io/github/atengk/spark/quality/DataQualityResult.java`

该实体类用于封装数据质量校验结果。

```java
package io.github.atengk.spark.quality;

import lombok.Data;

import java.io.Serializable;

/**
 * 数据质量校验结果。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
public class DataQualityResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务名称。
     */
    private String jobName;

    /**
     * 业务日期。
     */
    private String bizDate;

    /**
     * 批次号。
     */
    private String batchId;

    /**
     * 数据集名称。
     */
    private String datasetName;

    /**
     * 规则名称。
     */
    private String ruleName;

    /**
     * 规则类型。
     */
    private String ruleType;

    /**
     * 校验状态。
     */
    private String checkStatus;

    /**
     * 总数据量。
     */
    private Long totalCount;

    /**
     * 异常数据量。
     */
    private Long invalidCount;

    /**
     * 阈值。
     */
    private String threshold;

    /**
     * 错误信息。
     */
    private String errorMessage;
}
```

文件位置：`src/main/java/io/github/atengk/spark/quality/DataQualityResultLogger.java`

该工具类用于输出数据质量校验结果日志，实际项目中可扩展为写入 Hive 或 MySQL。

```java
package io.github.atengk.spark.quality;

import lombok.extern.slf4j.Slf4j;

/**
 * 数据质量校验结果记录器。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class DataQualityResultLogger {

    private DataQualityResultLogger() {
    }

    /**
     * 记录校验结果。
     *
     * @param result 校验结果
     */
    public static void logResult(DataQualityResult result) {
        if (result == null) {
            throw new IllegalArgumentException("数据质量校验结果不能为空");
        }

        log.info("数据质量校验结果，jobName={}，bizDate={}，batchId={}，datasetName={}，ruleName={}，ruleType={}，status={}，totalCount={}，invalidCount={}，threshold={}，errorMessage={}",
                result.getJobName(),
                result.getBizDate(),
                result.getBatchId(),
                result.getDatasetName(),
                result.getRuleName(),
                result.getRuleType(),
                result.getCheckStatus(),
                result.getTotalCount(),
                result.getInvalidCount(),
                result.getThreshold(),
                result.getErrorMessage());
    }
}
```

使用示例：

```java
DataQualityResult result = new DataQualityResult();
result.setJobName("user-summary");
result.setBizDate("2026-05-11");
result.setBatchId("202605110001");
result.setDatasetName("用户汇总结果");
result.setRuleName("用户ID非空校验");
result.setRuleType("完整性");
result.setCheckStatus("PASS");
result.setTotalCount(100000L);
result.setInvalidCount(0L);
result.setThreshold("0");

DataQualityResultLogger.logResult(result);
```

校验结果输出建议如下：

| 建议                   | 说明                                |
| ---------------------- | ----------------------------------- |
| 每条规则都有结果       | 成功、告警、失败都要记录            |
| 结果结构化保存         | 便于查询和告警                      |
| 失败信息要可读         | 明确是哪条规则失败                  |
| 质量结果关联任务审计   | 使用 jobName、bizDate、batchId 关联 |
| 历史结果用于波动监控   | 近 7 天均值、异常率趋势等           |
| 重要规则失败应阻断任务 | 例如主键为空、输出为空、金额为负    |

## 监控与告警

本章节用于说明 Spark 任务运行过程中的监控与告警设计，包括 Spark UI、YARN UI、History Server、日志采集、指标采集、任务状态监控、数据量监控、失败告警和延迟告警。监控与告警的目标是及时发现任务失败、资源异常、性能退化、数据波动和实时链路延迟问题。

### Spark UI

Spark UI 是分析 Spark 任务运行状态和性能问题的核心入口。任务运行中可以通过 Spark UI 查看 Job、Stage、Task、SQL、Storage、Environment、Executors 等信息。

Spark UI 常用页面如下：

| 页面            | 说明                                  |
| --------------- | ------------------------------------- |
| Jobs            | 查看 Job 执行状态、耗时和触发 Action  |
| Stages          | 查看 Stage 详情、Shuffle、Task 分布   |
| SQL / DataFrame | 查看 SQL 执行计划和 SQL 耗时          |
| Storage         | 查看缓存数据集占用情况                |
| Environment     | 查看 SparkConf、JVM、Classpath        |
| Executors       | 查看 Executor 内存、GC、Task、Shuffle |
| Streaming       | 查看流任务批次、输入速率、处理速率    |

本地模式开启 Spark UI：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master local[*] \
  --conf spark.ui.enabled=true \
  target/spark-job.jar \
  --jobName user-summary \
  --env local \
  --bizDate 2026-05-11
```

Spark UI 排查建议如下：

| 问题          | 重点查看                                       |
| ------------- | ---------------------------------------------- |
| 任务整体慢    | Jobs、Stages、SQL                              |
| 某个 Stage 慢 | Stage 详情页 Task Duration                     |
| 数据倾斜      | Stage 中 Task 耗时和 Shuffle Read 是否极不均匀 |
| Executor OOM  | Executors 页内存、GC、失败日志                 |
| Shuffle 过大  | Stage 页 Shuffle Read / Write                  |
| 缓存未生效    | Storage 页是否存在缓存数据                     |
| SQL 未优化    | SQL 页物理计划是否存在过多 Exchange            |

Spark UI 中如果发现少量 Task 明显慢于其他 Task，优先判断数据倾斜；如果所有 Task 都慢，优先判断输入数据量、Shuffle 数据量、资源不足或外部存储吞吐问题。

### YARN UI

YARN UI 用于查看 Spark 应用在 YARN 集群中的资源申请、运行状态、队列、Container、ApplicationMaster 和日志信息。生产环境中，Spark on YARN 的任务排查通常需要结合 Spark UI 和 YARN UI。

YARN 常用命令如下：

```bash
# 查看当前运行中的 YARN 应用
yarn application -list

# 查看指定应用状态
yarn application -status application_1710000000000_0001

# 查看指定应用日志
yarn logs -applicationId application_1710000000000_0001

# 停止指定应用
yarn application -kill application_1710000000000_0001
```

YARN UI 常见关注点如下：

| 关注项             | 说明                              |
| ------------------ | --------------------------------- |
| Application 状态   | RUNNING、FINISHED、FAILED、KILLED |
| Queue              | 应用提交到哪个队列                |
| Allocated VCores   | 已分配 CPU 核数                   |
| Allocated Memory   | 已分配内存                        |
| Running Containers | 当前运行的 Container 数           |
| Diagnostics        | 失败诊断信息                      |
| Logs               | Driver 和 Executor 容器日志       |

YARN UI 排查建议如下：

| 问题                | 排查方向                         |
| ------------------- | -------------------------------- |
| 应用长时间 ACCEPTED | 队列资源不足或权限问题           |
| Container killed    | 内存超限、节点异常或队列策略限制 |
| Application FAILED  | 查看 Diagnostics 和 Driver 日志  |
| Executor 频繁丢失   | 节点不稳定、内存不足或网络问题   |
| 资源申请失败        | 检查队列容量、账号权限和资源参数 |

### History Server

History Server 用于查看已经结束的 Spark 应用历史信息。生产任务结束后，如果需要分析性能、失败原因或资源使用情况，应通过 Spark History Server 查看历史 Spark UI。

开启事件日志配置示例：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --conf spark.eventLog.enabled=true \
  --conf spark.eventLog.dir=hdfs:///spark-history \
  --conf spark.history.fs.logDirectory=hdfs:///spark-history \
  target/spark-job.jar \
  --jobName user-summary \
  --env prod \
  --bizDate 2026-05-11
```

常用配置说明：

| 配置                                 | 说明                            |
| ------------------------------------ | ------------------------------- |
| `spark.eventLog.enabled`             | 是否开启事件日志                |
| `spark.eventLog.dir`                 | Spark 应用事件日志写入目录      |
| `spark.history.fs.logDirectory`      | History Server 读取事件日志目录 |
| `spark.eventLog.compress`            | 是否压缩事件日志                |
| `spark.history.retainedApplications` | History Server 保留应用数量     |

History Server 使用建议如下：

| 建议                      | 说明                                     |
| ------------------------- | ---------------------------------------- |
| 生产任务必须开启事件日志  | 否则任务结束后无法查看 Spark UI          |
| 事件日志目录放 HDFS       | 保证 Driver 结束后日志仍可访问           |
| 定期清理历史日志          | 防止 HDFS 占用过大                       |
| 审计表记录 Application ID | 便于快速定位历史任务                     |
| 性能优化前后对比 History  | 用同一业务日期对比 Stage 和 Shuffle 指标 |

### 日志采集

日志采集用于集中保存 Driver、Executor、提交脚本、调度平台和审计日志。生产环境不应只依赖临时控制台输出，任务日志应接入日志平台，支持检索、过滤、告警和长期留存。

日志来源如下：

| 来源           | 说明                              |
| -------------- | --------------------------------- |
| Driver 日志    | 主程序、SQL、参数、异常、审计日志 |
| Executor 日志  | Task 执行异常、UDF 异常、写出异常 |
| Shell 脚本日志 | spark-submit 提交日志             |
| YARN 日志      | Container 标准输出和错误输出      |
| 调度平台日志   | 调度实例、依赖、重试和告警日志    |
| 业务审计日志   | 输入量、输出量、耗时、状态        |

推荐日志字段如下：

| 字段            | 说明                 |
| --------------- | -------------------- |
| `jobName`       | 任务名称             |
| `env`           | 运行环境             |
| `bizDate`       | 业务日期             |
| `batchId`       | 批次号               |
| `applicationId` | Spark Application ID |
| `stage`         | 当前处理阶段         |
| `tableName`     | 表名                 |
| `path`          | 路径                 |
| `status`        | 任务状态             |
| `costMs`        | 耗时                 |

日志采集建议如下：

| 建议                            | 说明                                      |
| ------------------------------- | ----------------------------------------- |
| 日志必须结构化                  | 关键字段固定，便于检索                    |
| Driver 和 Executor 日志都要采集 | 仅 Driver 日志无法覆盖 Task 异常          |
| 异常日志必须打印堆栈            | 使用 `log.error("xxx", e)`                |
| 敏感信息脱敏                    | 密码、Token、Secret、Keytab 不打印        |
| 日志保留周期明确                | 按任务重要性设置 7 天、30 天或更长        |
| 日志与审计表关联                | 使用 jobName、batchId、applicationId 关联 |

### 指标采集

指标采集用于将任务状态、数据量、耗时、资源使用、失败次数和流任务延迟等指标写入监控系统或审计表。相比日志，指标更适合做趋势分析、看板展示和自动告警。

常见指标如下：

| 指标                       | 说明           |
| -------------------------- | -------------- |
| `input_count`              | 输入数据量     |
| `output_count`             | 输出数据量     |
| `invalid_count`            | 脏数据量       |
| `duplicate_count`          | 重复数据量     |
| `cost_ms`                  | 任务耗时       |
| `shuffle_read_bytes`       | Shuffle 读取量 |
| `shuffle_write_bytes`      | Shuffle 写出量 |
| `failed_task_count`        | 失败 Task 数   |
| `streaming_input_rate`     | 流任务输入速率 |
| `streaming_process_rate`   | 流任务处理速率 |
| `streaming_batch_duration` | 流任务批次耗时 |

文件位置：`src/main/java/io/github/atengk/spark/monitor/SparkMetricRecord.java`

该类用于封装 Spark 任务指标记录。

```java
package io.github.atengk.spark.monitor;

import lombok.Data;

import java.io.Serializable;

/**
 * Spark 任务指标记录。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
public class SparkMetricRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务名称。
     */
    private String jobName;

    /**
     * 运行环境。
     */
    private String env;

    /**
     * 业务日期。
     */
    private String bizDate;

    /**
     * 批次号。
     */
    private String batchId;

    /**
     * Spark Application ID。
     */
    private String applicationId;

    /**
     * 指标名称。
     */
    private String metricName;

    /**
     * 指标值。
     */
    private Double metricValue;

    /**
     * 指标标签。
     */
    private String tags;
}
```

文件位置：`src/main/java/io/github/atengk/spark/monitor/SparkMetricLogger.java`

该工具类用于统一输出任务指标日志，后续可扩展为写入 Prometheus Pushgateway、MySQL 或 Hive。

```java
package io.github.atengk.spark.monitor;

import lombok.extern.slf4j.Slf4j;

/**
 * Spark 指标日志记录器。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class SparkMetricLogger {

    private SparkMetricLogger() {
    }

    /**
     * 记录指标。
     *
     * @param record 指标记录
     */
    public static void logMetric(SparkMetricRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("指标记录不能为空");
        }

        log.info("Spark 指标，jobName={}，env={}，bizDate={}，batchId={}，applicationId={}，metricName={}，metricValue={}，tags={}",
                record.getJobName(),
                record.getEnv(),
                record.getBizDate(),
                record.getBatchId(),
                record.getApplicationId(),
                record.getMetricName(),
                record.getMetricValue(),
                record.getTags());
    }
}
```

使用示例：

```java
SparkMetricRecord record = new SparkMetricRecord();
record.setJobName("user-summary");
record.setEnv("prod");
record.setBizDate("2026-05-11");
record.setBatchId("202605110001");
record.setApplicationId(spark.sparkContext().applicationId());
record.setMetricName("output_count");
record.setMetricValue((double) resultDf.count());
record.setTags("table=ads.ads_user_summary");

SparkMetricLogger.logMetric(record);
```

指标采集建议如下：

| 建议                         | 说明                              |
| ---------------------------- | --------------------------------- |
| 核心任务必须采集输入和输出量 | 便于判断任务是否正常产出          |
| 指标带上 jobName 和 bizDate  | 便于按任务和日期查询              |
| 流任务采集批次指标           | 输入速率、处理速率、批次耗时      |
| 数据质量指标单独采集         | invalid_count、duplicate_count 等 |
| 指标写入长期存储             | 支持趋势分析和容量规划            |
| 指标采集失败不应影响主任务   | 除非是强审计要求                  |

### 任务状态监控

任务状态监控用于判断 Spark 任务是否成功、失败、运行中、超时或被杀死。状态监控通常由调度平台、YARN、Spark History Server 和审计表共同完成。

推荐任务状态如下：

| 状态        | 说明       |
| ----------- | ---------- |
| `SUBMITTED` | 已提交     |
| `RUNNING`   | 运行中     |
| `SUCCESS`   | 执行成功   |
| `FAILED`    | 执行失败   |
| `KILLED`    | 被手动终止 |
| `TIMEOUT`   | 执行超时   |
| `SKIPPED`   | 被跳过     |

任务入口中记录状态示例：

```java
try {
    log.info("任务状态变更，jobName={}，bizDate={}，status=RUNNING", jobName, bizDate);

    job.execute(spark, runtimeArgs);

    log.info("任务状态变更，jobName={}，bizDate={}，status=SUCCESS", jobName, bizDate);
} catch (Exception e) {
    log.error("任务状态变更，jobName={}，bizDate={}，status=FAILED", jobName, bizDate, e);
    throw e;
}
```

任务状态监控建议如下：

| 建议                   | 说明                                |
| ---------------------- | ----------------------------------- |
| 状态写入审计表         | 不只依赖调度平台状态                |
| 失败状态保留异常摘要   | 便于快速定位原因                    |
| 记录 Application ID    | 便于查看 Spark UI 和 YARN 日志      |
| 调度平台配置超时       | 防止任务长时间挂起                  |
| 流任务监控存活状态     | 长期 RUNNING 也要监控是否有数据处理 |
| 任务重试要关联 batchId | 防止多次重试状态混乱                |

### 数据量监控

数据量监控用于发现上游数据缺失、重复、暴增、暴跌和清洗规则异常。离线任务应监控输入量和输出量，实时任务应监控每批输入量和处理量。

数据量监控示例：

```java
long inputCount = sourceDf.count();
long outputCount = resultDf.count();

log.info("数据量监控，jobName={}，bizDate={}，inputCount={}，outputCount={}，diffCount={}",
        args.getJobName(),
        args.getBizDate(),
        inputCount,
        outputCount,
        inputCount - outputCount);
```

波动率监控示例：

```java
long currentCount = outputCount;
long historyAvgCount = 1000000L;

double fluctuationRate = Math.abs(currentCount - historyAvgCount) * 1.0 / historyAvgCount;

log.info("数据量波动监控，jobName={}，bizDate={}，currentCount={}，historyAvgCount={}，fluctuationRate={}",
        args.getJobName(),
        args.getBizDate(),
        currentCount,
        historyAvgCount,
        fluctuationRate);
```

数据量监控建议如下：

| 建议                  | 说明                          |
| --------------------- | ----------------------------- |
| 输入量为 0 应重点告警 | 可能是上游未产出              |
| 输出量为 0 默认失败   | 除非任务允许空结果            |
| 监控输入输出比例      | 清洗后骤降可能是规则异常      |
| 与历史均值对比        | 近 7 天、近 14 天趋势更有意义 |
| 节假日单独处理        | 避免正常业务波动误报          |
| 监控结果写审计表      | 支持后续趋势查询              |

### 失败告警

失败告警用于在 Spark 任务失败时通知开发、运维或数据值班人员。告警内容必须包含任务名称、运行环境、业务日期、批次号、Application ID、失败阶段和异常摘要。

推荐告警内容如下：

| 字段           | 说明                               |
| -------------- | ---------------------------------- |
| 任务名称       | `user-summary`                     |
| 运行环境       | `prod`                             |
| 业务日期       | `2026-05-11`                       |
| 批次号         | `202605110001`                     |
| Application ID | `application_1710000000000_0001`   |
| 失败阶段       | 读取、转换、写出、质量校验         |
| 异常摘要       | 截取前 1000 字符                   |
| 日志链接       | YARN 或日志平台地址                |
| 处理建议       | 查看目标表、路径、权限、上游依赖等 |

文件位置：`src/main/java/io/github/atengk/spark/alert/AlertMessage.java`

该类用于封装告警消息。

```java
package io.github.atengk.spark.alert;

import lombok.Data;

import java.io.Serializable;

/**
 * 告警消息。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
public class AlertMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 告警标题。
     */
    private String title;

    /**
     * 任务名称。
     */
    private String jobName;

    /**
     * 运行环境。
     */
    private String env;

    /**
     * 业务日期。
     */
    private String bizDate;

    /**
     * 批次号。
     */
    private String batchId;

    /**
     * Spark Application ID。
     */
    private String applicationId;

    /**
     * 告警级别。
     */
    private String level;

    /**
     * 告警内容。
     */
    private String content;
}
```

文件位置：`src/main/java/io/github/atengk/spark/alert/AlertLogger.java`

该工具类用于输出告警日志，实际项目中可以扩展为企业微信、钉钉、邮件或监控平台通知。

```java
package io.github.atengk.spark.alert;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 告警日志发送器。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class AlertLogger {

    private AlertLogger() {
    }

    /**
     * 发送失败告警。
     *
     * @param message 告警消息
     */
    public static void send(AlertMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("告警消息不能为空");
        }

        log.error("任务告警，title={}，level={}，jobName={}，env={}，bizDate={}，batchId={}，applicationId={}，content={}",
                message.getTitle(),
                StrUtil.blankToDefault(message.getLevel(), "ERROR"),
                message.getJobName(),
                message.getEnv(),
                message.getBizDate(),
                message.getBatchId(),
                message.getApplicationId(),
                message.getContent());
    }
}
```

失败告警建议如下：

| 建议                 | 说明                                 |
| -------------------- | ------------------------------------ |
| 生产任务失败必须告警 | 不只依赖人工看调度平台               |
| 告警内容必须可定位   | 包含 jobName、bizDate、applicationId |
| 异常摘要限制长度     | 避免告警消息过长                     |
| 告警分级             | 核心链路 ERROR，普通波动 WARN        |
| 防止告警风暴         | 同一任务重复失败应合并或限流         |
| 告警后保留审计记录   | 便于复盘和统计失败率                 |

### 延迟告警

延迟告警用于发现离线任务未按 SLA 完成、实时任务处理延迟过高、Kafka 消费积压、上游分区迟迟未产出等问题。

常见延迟类型如下：

| 类型           | 说明                      |
| -------------- | ------------------------- |
| 离线产出延迟   | 任务超过 SLA 时间仍未成功 |
| 上游到达延迟   | 依赖表分区未及时产出      |
| 实时处理延迟   | 批次处理耗时超过阈值      |
| Kafka 消费延迟 | Lag 持续升高              |
| 数据事件延迟   | 事件时间与处理时间差过大  |

Structured Streaming 延迟监听示例：

```java
package io.github.atengk.spark.monitor;

import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.streaming.StreamingQueryListener;

/**
 * Structured Streaming 延迟监听器。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class StreamingDelayListener extends StreamingQueryListener {

    /**
     * 查询启动事件。
     *
     * @param event 启动事件
     */
    @Override
    public void onQueryStarted(QueryStartedEvent event) {
        log.info("流式查询启动，id={}，name={}", event.id(), event.name());
    }

    /**
     * 查询进度事件。
     *
     * @param event 进度事件
     */
    @Override
    public void onQueryProgress(QueryProgressEvent event) {
        long batchId = event.progress().batchId();
        long inputRows = event.progress().numInputRows();
        double inputRate = event.progress().inputRowsPerSecond();
        double processRate = event.progress().processedRowsPerSecond();

        log.info("流式查询进度，batchId={}，inputRows={}，inputRate={}，processRate={}",
                batchId, inputRows, inputRate, processRate);

        if (inputRate > 0 && processRate > 0 && processRate < inputRate * 0.5) {
            log.warn("流式处理速率低于输入速率，batchId={}，inputRate={}，processRate={}",
                    batchId, inputRate, processRate);
        }
    }

    /**
     * 查询结束事件。
     *
     * @param event 结束事件
     */
    @Override
    public void onQueryTerminated(QueryTerminatedEvent event) {
        log.error("流式查询终止，id={}，exception={}", event.id(), event.exception().orElse(null));
    }
}
```

注册监听器：

```java
spark.streams().addListener(new StreamingDelayListener());
```

延迟告警建议如下：

| 建议                     | 说明                           |
| ------------------------ | ------------------------------ |
| 离线任务配置 SLA         | 超过预期完成时间告警           |
| 流任务监控输入和处理速率 | 处理速率长期低于输入速率会积压 |
| Kafka Lag 持续升高要告警 | 单次波动可观察，持续升高需处理 |
| 延迟阈值按业务配置       | 核心实时链路和普通链路阈值不同 |
| 告警包含当前速率和批次号 | 便于判断积压程度               |
| 延迟告警与失败告警区分   | 延迟不一定失败，但需要关注     |

## 安全与权限

本章节用于说明 Spark 项目中的认证、授权、权限控制、数据脱敏、敏感配置管理、密钥管理和审计追踪。安全设计应覆盖计算资源、存储资源、元数据、消息队列、数据库、配置文件和日志输出。

### Kerberos 认证

Kerberos 是 Hadoop 生态中常见的统一认证机制。启用 Kerberos 后，Spark 访问 HDFS、Hive、YARN 等资源时需要有效的 Principal 和 Keytab。

使用 Keytab 提交 Spark 任务：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --principal spark-user@EXAMPLE.COM \
  --keytab /etc/security/keytabs/spark-user.keytab \
  --files /etc/krb5.conf,/opt/module/hive/conf/hive-site.xml \
  --conf spark.yarn.principal=spark-user@EXAMPLE.COM \
  --conf spark.yarn.keytab=/etc/security/keytabs/spark-user.keytab \
  target/spark-job.jar \
  --jobName user-summary \
  --env prod \
  --bizDate 2026-05-11
```

本地验证 Kerberos 认证：

```bash
# 使用 keytab 获取票据
kinit -kt /etc/security/keytabs/spark-user.keytab spark-user@EXAMPLE.COM

# 查看当前票据
klist

# 验证 HDFS 访问
hdfs dfs -ls /warehouse/prod
```

Kerberos 配置建议如下：

| 建议                       | 说明                     |
| -------------------------- | ------------------------ |
| 生产任务使用专用 Principal | 不使用个人账号长期运行   |
| Keytab 权限严格控制        | 仅任务运行账号可读       |
| Keytab 不进入 Git          | 通过安全渠道部署         |
| 票据过期要监控             | 长时间运行任务尤其关注   |
| 测试和生产 Principal 隔离  | 防止测试任务访问生产资源 |
| 日志不打印 Keytab 内容     | 路径也应谨慎输出         |

### HDFS 权限

HDFS 权限用于控制 Spark 任务对文件目录的读写删除能力。生产任务应按最小权限原则授权，只允许访问任务需要的输入目录、输出目录、临时目录、错误目录和 Checkpoint 目录。

查看权限：

```bash
# 查看目录权限
hdfs dfs -ls /warehouse/prod/dwd/user_event

# 查看 ACL
hdfs dfs -getfacl /warehouse/prod/dwd/user_event
```

设置权限：

```bash
# 修改目录属主
hdfs dfs -chown -R spark:spark /warehouse/prod/dwd/user_event

# 修改目录权限
hdfs dfs -chmod -R 750 /warehouse/prod/dwd/user_event

# 设置 ACL
hdfs dfs -setfacl -m user:spark_user:rwx /warehouse/prod/dwd/user_event
```

写权限验证：

```bash
# 测试写入权限
hdfs dfs -touchz /warehouse/prod/dwd/user_event/_permission_test

# 删除测试文件
hdfs dfs -rm /warehouse/prod/dwd/user_event/_permission_test
```

HDFS 权限建议如下：

| 建议                    | 说明                           |
| ----------------------- | ------------------------------ |
| 输入目录只授予读权限    | 防止误删源数据                 |
| 输出目录授予写权限      | 控制在目标表或目标路径范围内   |
| 删除权限谨慎授予        | 覆盖写入任务才需要             |
| Checkpoint 目录独立授权 | 流任务必须稳定读写             |
| 错误目录单独规划        | 脏数据路径与正式路径隔离       |
| 权限变更记录审计        | 记录授权对象、路径、原因和时间 |

### Hive 权限

Hive 权限用于控制 Spark SQL 访问 Hive 库表、分区和元数据的能力。开启 Ranger、Sentry 或其他权限系统后，Spark 访问 Hive 表需要同时满足 Hive 元数据权限和底层 HDFS 路径权限。

常见 Hive 权限如下：

| 权限   | 说明               |
| ------ | ------------------ |
| SELECT | 查询表数据         |
| INSERT | 写入表或分区       |
| CREATE | 创建库表           |
| DROP   | 删除库表           |
| ALTER  | 修改表结构或分区   |
| ALL    | 全部权限，生产慎用 |

Hive 权限检查示例：

```sql
-- 查看当前用户
SELECT current_user();

-- 验证表读取权限
SELECT COUNT(1)
FROM dwd.dwd_user_event
WHERE dt = '2026-05-11';

-- 验证目标表写入权限
INSERT INTO TABLE audit.permission_test
SELECT 'spark_user', current_timestamp();
```

Hive 权限建议如下：

| 建议                       | 说明                             |
| -------------------------- | -------------------------------- |
| 生产任务使用任务账号       | 不使用个人账号                   |
| 按库表最小授权             | 只授权任务需要的表               |
| 写权限只给目标表           | 不给无关库表写权限               |
| DROP 权限严格控制          | 防止误删表                       |
| 表权限和 HDFS 权限都要检查 | 只具备 Hive 权限不一定能读写文件 |
| 权限申请保留记录           | 便于审计和问题追溯               |

### Kafka 权限

Kafka 权限用于控制 Spark 任务对 Topic 的消费、生产、查看元数据和 Consumer Group 使用权限。开启 ACL、SASL、SSL 后，需要配置认证信息和访问权限。

Kafka SASL 配置示例：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --conf spark.kafka.bootstrap.servers=kafka-host:9092 \
  --conf spark.executor.extraJavaOptions=-Djava.security.auth.login.config=kafka_client_jaas.conf \
  --conf spark.driver.extraJavaOptions=-Djava.security.auth.login.config=kafka_client_jaas.conf \
  --files /opt/app/spark-job/conf/kafka_client_jaas.conf \
  target/spark-job.jar \
  --jobName user-event-stream \
  --env prod \
  --bizDate 2026-05-11
```

Kafka 读取安全参数示例：

```java
Dataset<Row> kafkaDf = spark.readStream()
        .format("kafka")
        .option("kafka.bootstrap.servers", "kafka-host:9092")
        .option("subscribe", "ods_user_event")
        .option("kafka.security.protocol", "SASL_PLAINTEXT")
        .option("kafka.sasl.mechanism", "PLAIN")
        .option("startingOffsets", "latest")
        .load();
```

Kafka 权限建议如下：

| 建议                    | 说明                                  |
| ----------------------- | ------------------------------------- |
| 读写权限分开授权        | Consumer 和 Producer 权限不要混用     |
| Topic 按环境隔离        | test/prod 不共用 Topic                |
| Consumer Group 独立命名 | 防止消费状态互相影响                  |
| JAAS 配置外部注入       | 不打入 Jar，不提交 Git                |
| 密码和 Token 脱敏       | 不输出到日志                          |
| Kafka ACL 变更需审计    | 记录授权对象、Topic、Group 和操作权限 |

### 数据脱敏

数据脱敏用于保护手机号、身份证号、邮箱、银行卡号、姓名、地址等敏感信息。脱敏应根据数据使用场景决定：开发测试环境通常需要静态脱敏，生产查询和日志输出需要动态脱敏。

常见敏感字段如下：

| 字段         | 脱敏方式               |
| ------------ | ---------------------- |
| 手机号       | 保留前三后四           |
| 身份证号     | 保留前六后四           |
| 邮箱         | 用户名前几位保留       |
| 银行卡号     | 保留后四位             |
| 姓名         | 保留姓氏               |
| 地址         | 保留省市，隐藏详细地址 |
| Token / 密码 | 全部隐藏               |

文件位置：`src/main/java/io/github/atengk/spark/security/DataMaskingUtil.java`

该工具类用于对常见敏感字段进行脱敏。

```java
package io.github.atengk.spark.security;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 数据脱敏工具类。
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class DataMaskingUtil {

    private DataMaskingUtil() {
    }

    /**
     * 手机号脱敏。
     *
     * @param mobile 手机号
     * @return 脱敏后的手机号
     */
    public static String maskMobile(String mobile) {
        if (StrUtil.isBlank(mobile)) {
            return null;
        }
        return DesensitizedUtil.mobilePhone(mobile);
    }

    /**
     * 身份证号脱敏。
     *
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    public static String maskIdCard(String idCard) {
        if (StrUtil.isBlank(idCard)) {
            return null;
        }
        return DesensitizedUtil.idCardNum(idCard, 6, 4);
    }

    /**
     * 邮箱脱敏。
     *
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String email) {
        if (StrUtil.isBlank(email)) {
            return null;
        }
        return DesensitizedUtil.email(email);
    }

    /**
     * 密钥脱敏。
     *
     * @param secret 密钥
     * @return 脱敏后的密钥
     */
    public static String maskSecret(String secret) {
        if (StrUtil.isBlank(secret)) {
            return null;
        }
        return "******";
    }
}
```

注册 Spark UDF 示例：

```java
spark.udf().register("mask_mobile", DataMaskingUtil::maskMobile, DataTypes.StringType);
spark.udf().register("mask_email", DataMaskingUtil::maskEmail, DataTypes.StringType);

Dataset<Row> maskedDf = spark.sql(
        "SELECT user_id, mask_mobile(mobile) AS mobile, mask_email(email) AS email " +
        "FROM dwd.dwd_user_detail " +
        "WHERE dt = '2026-05-11'"
);
```

数据脱敏建议如下：

| 建议                     | 说明                             |
| ------------------------ | -------------------------------- |
| 日志中不打印原始敏感字段 | 手机号、身份证号、Token 都要脱敏 |
| 测试环境使用脱敏数据     | 不直接复制生产明文数据           |
| 脱敏规则统一封装         | 不在各个 SQL 中随意实现          |
| 明确可逆与不可逆脱敏     | 普通分析优先不可逆脱敏           |
| 敏感字段清单文档化       | 便于开发和审计                   |
| 输出给外部系统前再次检查 | 防止敏感字段泄露                 |

### 敏感配置管理

敏感配置包括数据库密码、Kafka 密码、对象存储密钥、Keytab、Token、API Key、证书等。敏感配置不应写入 Git、不应打入 Jar、不应出现在日志中。

常见敏感配置如下：

| 类型            | 示例                           |
| --------------- | ------------------------------ |
| JDBC 密码       | MySQL、PostgreSQL、Oracle 密码 |
| Kafka 凭据      | JAAS 用户名密码、SSL 证书      |
| 对象存储密钥    | AK、SK、SecretId、SecretKey    |
| Kerberos Keytab | `.keytab` 文件                 |
| API Token       | 外部接口访问 Token             |
| 私钥证书        | SSL Key、Truststore            |

推荐通过环境变量读取：

```java
String username = System.getenv("MYSQL_USERNAME");
String password = System.getenv("MYSQL_PASSWORD");

if (StrUtil.hasBlank(username, password)) {
    throw new IllegalArgumentException("MySQL 用户名或密码环境变量不能为空");
}
```

提交任务时注入环境变量：

```bash
export MYSQL_USERNAME=spark_user
export MYSQL_PASSWORD='******'

spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  target/spark-job.jar \
  --jobName user-summary \
  --env prod \
  --bizDate 2026-05-11
```

敏感配置管理建议如下：

| 建议           | 说明                               |
| -------------- | ---------------------------------- |
| 不提交 Git     | 密码、Keytab、证书不能进入代码仓库 |
| 不打入 Jar     | 生产密钥必须外部化                 |
| 不打印日志     | 参数日志必须脱敏                   |
| 按环境隔离     | test/prod 使用不同账号和密钥       |
| 定期轮换       | 长期密钥存在泄露风险               |
| 最小权限       | 密钥只授予必要资源访问权限         |
| 配置变更有审计 | 记录变更人、时间和用途             |

### 密钥管理

密钥管理用于安全保存、分发、轮换和吊销各类访问凭据。常见方案包括环境变量、配置中心、Kubernetes Secret、云厂商 KMS、HashiCorp Vault、密钥文件挂载等。

常见密钥管理方式如下：

| 方式              | 适用场景                       |
| ----------------- | ------------------------------ |
| 环境变量          | 简单任务、脚本提交             |
| 配置中心          | 企业内部统一配置管理           |
| Kubernetes Secret | K8s 模式 Spark 任务            |
| 云厂商 KMS        | 云上对象存储、数据库、接口密钥 |
| Vault             | 多环境密钥集中管理             |
| Keytab 文件       | Kerberos 认证                  |

Kubernetes Secret 注入示例：

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: spark-job-secret
  namespace: spark
type: Opaque
stringData:
  MYSQL_USERNAME: spark_user
  MYSQL_PASSWORD: change_me
```

Spark on Kubernetes 使用 Secret：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master k8s://https://kubernetes.default.svc \
  --deploy-mode cluster \
  --conf spark.kubernetes.namespace=spark \
  --conf spark.kubernetes.container.image=registry.example.com/bigdata/spark-job:1.0.0 \
  --conf spark.kubernetes.driver.secretKeyRef.MYSQL_USERNAME=spark-job-secret:MYSQL_USERNAME \
  --conf spark.kubernetes.driver.secretKeyRef.MYSQL_PASSWORD=spark-job-secret:MYSQL_PASSWORD \
  --conf spark.kubernetes.executor.secretKeyRef.MYSQL_USERNAME=spark-job-secret:MYSQL_USERNAME \
  --conf spark.kubernetes.executor.secretKeyRef.MYSQL_PASSWORD=spark-job-secret:MYSQL_PASSWORD \
  local:///opt/spark/app/spark-job.jar \
  --jobName user-summary \
  --env prod \
  --bizDate 2026-05-11
```

密钥管理建议如下：

| 建议             | 说明                       |
| ---------------- | -------------------------- |
| 使用专用密钥系统 | 生产环境不依赖手工散落文件 |
| 密钥定期轮换     | 降低长期泄露风险           |
| 密钥权限最小化   | 不使用全局管理员密钥       |
| 密钥访问有审计   | 记录访问者、时间和用途     |
| 密钥吊销流程明确 | 人员离职、泄露时可快速停用 |
| 测试生产密钥隔离 | 不允许测试任务使用生产密钥 |

### 审计追踪

审计追踪用于记录谁在什么时间通过什么任务访问了哪些数据、写入了哪些目标、使用了哪些资源、产生了什么结果。审计追踪是数据安全、合规、故障复盘和成本分析的重要基础。

推荐审计内容如下：

| 审计项          | 说明                                               |
| --------------- | -------------------------------------------------- |
| 任务名称        | `jobName`                                          |
| 运行账号        | Kerberos Principal、Linux 用户、K8s ServiceAccount |
| 运行环境        | local、test、prod                                  |
| 业务日期        | `bizDate`                                          |
| 批次号          | `batchId`                                          |
| Application ID  | Spark 应用 ID                                      |
| 输入表 / 路径   | 读取的数据来源                                     |
| 输出表 / 路径   | 写入的数据目标                                     |
| 输入量 / 输出量 | 数据处理规模                                       |
| 任务状态        | SUCCESS、FAILED                                    |
| 异常摘要        | 失败原因                                           |
| 开始 / 结束时间 | 任务时间线                                         |
| 资源队列        | YARN 队列或 K8s Namespace                          |

Hive 审计表示例：

```sql
CREATE TABLE IF NOT EXISTS audit.spark_access_audit (
    job_name STRING COMMENT '任务名称',
    env STRING COMMENT '运行环境',
    batch_id STRING COMMENT '批次号',
    biz_date STRING COMMENT '业务日期',
    application_id STRING COMMENT 'Spark Application ID',
    run_user STRING COMMENT '运行用户',
    source_objects STRING COMMENT '输入表或路径',
    target_objects STRING COMMENT '输出表或路径',
    input_count BIGINT COMMENT '输入数据量',
    output_count BIGINT COMMENT '输出数据量',
    status STRING COMMENT '任务状态',
    error_message STRING COMMENT '异常信息',
    start_time TIMESTAMP COMMENT '开始时间',
    end_time TIMESTAMP COMMENT '结束时间'
)
COMMENT 'Spark 数据访问审计表'
PARTITIONED BY (dt STRING COMMENT '审计分区日期')
STORED AS PARQUET;
```

获取运行用户示例：

```java
String runUser = System.getProperty("user.name");
String applicationId = spark.sparkContext().applicationId();

log.info("审计信息，jobName={}，runUser={}，applicationId={}",
        args.getJobName(),
        runUser,
        applicationId);
```

审计追踪建议如下：

| 建议                   | 说明                            |
| ---------------------- | ------------------------------- |
| 生产任务必须记录审计   | 成功和失败都要记录              |
| 输入输出对象必须可追踪 | 表名、路径、Topic 都要记录      |
| 记录运行账号           | 便于权限和责任追踪              |
| 关联数据质量结果       | 审计表和质量表通过 batchId 关联 |
| 审计数据长期保存       | 支持安全检查和历史复盘          |
| 审计日志不可随意修改   | 重要场景应写入受控审计库        |

## 部署方案

本章节用于说明 Spark 项目的部署方式，包括本地部署、测试环境部署、生产环境部署、配置分离、启动脚本、停止脚本、回滚方案和发布检查。Spark 项目部署的核心是保证代码包、配置文件、依赖文件、提交脚本、权限账号和运行参数在不同环境中保持一致且可追踪。

### 本地部署

本地部署主要用于开发调试、样例数据验证、SQL 逻辑验证和单元测试。 本地部署不应连接生产 Hive、生产 Kafka、生产 JDBC 和生产对象存储，避免误读误写。

推荐本地目录结构：

```text
spark-job
├── target
│   └── spark-job.jar
├── conf
│   ├── application-local.yml
│   └── logback.xml
├── data
│   ├── input
│   └── output
├── logs
└── scripts
    └── start-local.sh
```

本地启动脚本示例：

```bash
#!/usr/bin/env bash

# scripts/start-local.sh
# 本地启动 Spark 任务

set -e

PROJECT_DIR=$(cd "$(dirname "$0")/.." && pwd)
APP_JAR="${PROJECT_DIR}/target/spark-job.jar"

if [ ! -f "${APP_JAR}" ]; then
  echo "未找到应用 Jar，请先执行：mvn clean package"
  exit 1
fi

spark-submit \
  --class io.github.atengk.spark.Application \
  --master local[*] \
  --name spark-user-summary-local \
  --conf spark.sql.shuffle.partitions=4 \
  --conf spark.ui.enabled=true \
  "${APP_JAR}" \
  --jobName user-summary \
  --env local \
  --bizDate 2026-05-11 \
  --inputPath "${PROJECT_DIR}/data/input/user_event.json" \
  --outputPath "${PROJECT_DIR}/data/output/user_summary" \
  --writeMode overwrite
```

本地部署建议如下：

| 建议                     | 说明                                         |
| ------------------------ | -------------------------------------------- |
| 使用 local 模式          | 推荐 `local[2]` 或 `local[*]`                |
| 使用样例数据             | 不直接读取生产数据                           |
| 输出到本地目录           | 避免写入 HDFS 生产路径                       |
| Shuffle 分区调小         | 本地设置为 2 到 8 即可                       |
| 配置文件使用 local       | 不加载生产配置                               |
| 本地通过后再提交测试环境 | 本地只能验证逻辑，不能覆盖集群权限和依赖问题 |

### 测试环境部署

测试环境用于验证 Spark 任务在真实集群中的运行效果，包括依赖、权限、Hive Metastore、HDFS 路径、Kafka Topic、JDBC 连接、资源参数和调度平台集成。

推荐测试环境目录结构：

```text
/opt/app/spark-job
├── bin
│   ├── start-user-summary.sh
│   ├── stop-job.sh
│   └── backfill-user-summary.sh
├── conf
│   ├── application-test.yml
│   ├── logback.xml
│   └── hive-site.xml
├── lib
│   └── spark-job-1.0.0.jar
├── logs
└── releases
    └── spark-job-1.0.0.jar
```

测试环境提交示例：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --name spark-user-summary-test \
  --queue root.test.etl \
  --driver-memory 1g \
  --executor-memory 2g \
  --executor-cores 2 \
  --num-executors 2 \
  --conf spark.sql.shuffle.partitions=100 \
  --conf spark.sql.adaptive.enabled=true \
  --files /opt/app/spark-job/conf/application-test.yml,/opt/app/spark-job/conf/logback.xml,/opt/app/spark-job/conf/hive-site.xml \
  /opt/app/spark-job/lib/spark-job-1.0.0.jar \
  --jobName user-summary \
  --env test \
  --bizDate 2026-05-11 \
  --batchId test-202605110001 \
  --writeMode overwrite
```

测试环境部署建议如下：

| 建议                 | 说明                     |
| -------------------- | ------------------------ |
| 使用独立测试队列     | 不占用生产资源           |
| 使用测试 Hive 库     | 防止误写生产表           |
| 使用测试 HDFS 路径   | `/warehouse/test/...`    |
| 使用测试 Kafka Topic | test/prod Topic 必须隔离 |
| 配置测试账号权限     | 提前验证读写权限         |
| 保留测试运行日志     | 用于上线前排查问题       |

### 生产环境部署

生产环境部署用于正式调度运行 Spark 任务。生产环境必须保证版本可追踪、配置可审计、权限最小化、任务可重跑、失败可告警、结果可校验。

推荐生产目录结构：

```text
/opt/app/spark-job
├── bin
│   ├── env.sh
│   ├── start-user-summary.sh
│   ├── stop-job.sh
│   ├── rollback.sh
│   └── backfill-user-summary.sh
├── conf
│   ├── application-prod.yml
│   ├── logback.xml
│   ├── hive-site.xml
│   └── kafka_client_jaas.conf
├── lib
│   └── spark-job.jar -> /opt/app/spark-job/releases/spark-job-1.0.0.jar
├── logs
├── releases
│   ├── spark-job-1.0.0.jar
│   └── spark-job-1.0.1.jar
└── backup
```

生产提交示例：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --name spark-user-summary-prod \
  --queue root.prod.etl \
  --driver-memory 2g \
  --executor-memory 4g \
  --executor-cores 2 \
  --num-executors 8 \
  --conf spark.sql.shuffle.partitions=400 \
  --conf spark.sql.adaptive.enabled=true \
  --conf spark.sql.adaptive.skewJoin.enabled=true \
  --conf spark.eventLog.enabled=true \
  --conf spark.eventLog.dir=hdfs:///spark-history \
  --files /opt/app/spark-job/conf/application-prod.yml,/opt/app/spark-job/conf/logback.xml,/opt/app/spark-job/conf/hive-site.xml \
  /opt/app/spark-job/lib/spark-job.jar \
  --jobName user-summary \
  --env prod \
  --bizDate 2026-05-11 \
  --batchId 202605110001 \
  --writeMode overwrite \
  --allowEmpty false
```

生产环境部署建议如下：

| 建议               | 说明                                 |
| ------------------ | ------------------------------------ |
| 使用版本化 Jar     | 例如 `spark-job-1.0.1.jar`           |
| 当前版本使用软链接 | `lib/spark-job.jar` 指向当前发布版本 |
| 生产配置外部化     | 不将生产密码、Keytab 打入 Jar        |
| 使用专用任务账号   | 不使用个人账号运行生产任务           |
| 生产开启事件日志   | 便于 History Server 查看历史任务     |
| 发布前完成检查清单 | 权限、配置、依赖、调度、回滚都要确认 |

### 配置分离

配置分离用于保证代码包与环境配置独立。Jar 包应只包含通用默认配置、SQL 文件和基础日志配置，生产差异化配置应通过外部配置文件、环境变量、调度参数或密钥系统注入。

推荐配置分类如下：

| 配置类型 | 示例                   | 管理方式           |
| -------- | ---------------------- | ------------------ |
| 通用配置 | SQL 文件、默认参数     | 打入 Jar           |
| 环境配置 | local、test、prod 路径 | 外部配置文件       |
| 资源配置 | Executor、内存、队列   | 提交脚本或调度平台 |
| 敏感配置 | 密码、Token、Keytab    | 环境变量或密钥系统 |
| 业务参数 | bizDate、batchId       | 调度平台传入       |
| 日志配置 | logback.xml            | 可随包或外部分发   |

配置文件示例：

```yaml
# conf/application-prod.yml
# 生产环境 Spark 任务配置

app:
  env: prod
  name: spark-job

warehouse:
  root-path: hdfs:///warehouse/prod
  error-path: hdfs:///error/prod
  checkpoint-path: hdfs:///checkpoint/prod

hive:
  source-db: dwd
  target-db: ads

audit:
  table: audit.spark_job_audit
```

配置分离建议如下：

| 建议               | 说明                       |
| ------------------ | -------------------------- |
| 代码和配置分离发布 | 配置变更不必重新打包       |
| 敏感配置不进入 Jar | 密码、密钥、Keytab 外部化  |
| 配置按环境隔离     | local/test/prod 分文件     |
| 运行参数优先级最高 | 调度平台参数可覆盖默认配置 |
| 配置变更需要审计   | 记录变更人、时间、原因     |
| 发布时记录配置版本 | 便于回滚时恢复一致配置     |

### 启动脚本

启动脚本用于封装生产环境 `spark-submit` 命令，统一资源参数、配置文件、Jar 路径和业务参数。调度平台应优先调用启动脚本，而不是直接维护复杂提交命令。

文件位置：`bin/env.sh`

```bash
#!/usr/bin/env bash

# bin/env.sh
# Spark 生产环境公共变量

export JAVA_HOME=/opt/module/jdk
export SPARK_HOME=/opt/module/spark
export HADOOP_CONF_DIR=/opt/module/hadoop/etc/hadoop
export HIVE_CONF_DIR=/opt/module/hive/conf

export APP_HOME=/opt/app/spark-job
export APP_JAR=${APP_HOME}/lib/spark-job.jar
export APP_CONF=${APP_HOME}/conf/application-prod.yml
export LOG_CONF=${APP_HOME}/conf/logback.xml
export HIVE_CONF=${APP_HOME}/conf/hive-site.xml

export YARN_QUEUE=root.prod.etl

export PATH=${JAVA_HOME}/bin:${SPARK_HOME}/bin:${PATH}
```

文件位置：`bin/start-user-summary.sh`

```bash
#!/usr/bin/env bash

# bin/start-user-summary.sh
# 启动用户汇总 Spark 任务

set -e

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
source "${SCRIPT_DIR}/env.sh"

BIZ_DATE=$1
BATCH_ID=${2:-"$(date -d "${BIZ_DATE}" +%Y%m%d)_user_summary"}

if [ -z "${BIZ_DATE}" ]; then
  echo "缺少业务日期参数，例如：./start-user-summary.sh 2026-05-11"
  exit 1
fi

if [ ! -f "${APP_JAR}" ]; then
  echo "应用 Jar 不存在：${APP_JAR}"
  exit 1
fi

spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --name "spark-user-summary-${BIZ_DATE}" \
  --queue "${YARN_QUEUE}" \
  --driver-memory 2g \
  --executor-memory 4g \
  --executor-cores 2 \
  --num-executors 8 \
  --conf spark.sql.shuffle.partitions=400 \
  --conf spark.sql.adaptive.enabled=true \
  --conf spark.sql.adaptive.skewJoin.enabled=true \
  --conf spark.eventLog.enabled=true \
  --conf spark.eventLog.dir=hdfs:///spark-history \
  --files "${APP_CONF},${LOG_CONF},${HIVE_CONF}" \
  "${APP_JAR}" \
  --jobName user-summary \
  --env prod \
  --bizDate "${BIZ_DATE}" \
  --batchId "${BATCH_ID}" \
  --writeMode overwrite \
  --allowEmpty false
```

启动脚本建议如下：

| 建议               | 说明                           |
| ------------------ | ------------------------------ |
| 参数必须校验       | 缺少业务日期直接失败           |
| 资源参数统一维护   | 不在调度平台散落配置           |
| 记录任务名称和日期 | Spark 应用名包含业务日期       |
| 使用软链接 Jar     | 便于版本切换和回滚             |
| 脚本放入版本管理   | 提交逻辑可追踪                 |
| 不在脚本中写密码   | 密码通过环境变量或密钥系统注入 |

### 停止脚本

停止脚本用于终止正在运行的 Spark 任务。离线任务一般通过 YARN Application ID 停止；实时流任务更需要标准停止方式，避免重复消费、Checkpoint 状态异常或下游写入中断。

文件位置：`bin/stop-job.sh`

```bash
#!/usr/bin/env bash

# bin/stop-job.sh
# 停止指定 YARN Application

set -e

APP_ID=$1

if [ -z "${APP_ID}" ]; then
  echo "缺少 Application ID，例如：./stop-job.sh application_1710000000000_0001"
  exit 1
fi

echo "准备停止 Spark 应用：${APP_ID}"

yarn application -status "${APP_ID}" || true

read -r -p "确认停止该应用？输入 yes 继续：" CONFIRM

if [ "${CONFIRM}" != "yes" ]; then
  echo "已取消停止操作"
  exit 0
fi

yarn application -kill "${APP_ID}"

echo "停止命令已提交：${APP_ID}"
```

按应用名称查找任务：

```bash
yarn application -list | grep spark-user-summary
```

停止脚本建议如下：

| 建议                      | 说明                                                     |
| ------------------------- | -------------------------------------------------------- |
| 停止前确认 Application ID | 防止误杀任务                                             |
| 生产停止需要二次确认      | 脚本中要求输入 yes                                       |
| 流任务优先优雅停止        | 配置 `spark.sql.streaming.stopGracefullyOnShutdown=true` |
| 停止后检查状态            | 确认应用已 KILLED 或 FINISHED                            |
| 停止操作记录审计          | 记录操作者、时间和原因                                   |
| 不直接删除 Checkpoint     | 停止流任务不等于清理状态                                 |

### 回滚方案

回滚方案用于在新版本发布后出现任务失败、结果异常、性能严重下降或依赖冲突时，快速恢复到上一稳定版本。回滚应包括 Jar 回滚、配置回滚、脚本回滚和调度参数回滚。

推荐版本目录：

```text
/opt/app/spark-job
├── lib
│   └── spark-job.jar -> /opt/app/spark-job/releases/spark-job-1.0.1.jar
└── releases
    ├── spark-job-1.0.0.jar
    └── spark-job-1.0.1.jar
```

文件位置：`bin/rollback.sh`

```bash
#!/usr/bin/env bash

# bin/rollback.sh
# 回滚 Spark 应用 Jar 到指定版本

set -e

APP_HOME=/opt/app/spark-job
TARGET_VERSION=$1

if [ -z "${TARGET_VERSION}" ]; then
  echo "缺少目标版本，例如：./rollback.sh 1.0.0"
  exit 1
fi

TARGET_JAR="${APP_HOME}/releases/spark-job-${TARGET_VERSION}.jar"

if [ ! -f "${TARGET_JAR}" ]; then
  echo "目标版本 Jar 不存在：${TARGET_JAR}"
  exit 1
fi

CURRENT_LINK="${APP_HOME}/lib/spark-job.jar"

echo "当前版本："
ls -l "${CURRENT_LINK}" || true

echo "准备回滚到：${TARGET_JAR}"

read -r -p "确认回滚？输入 yes 继续：" CONFIRM

if [ "${CONFIRM}" != "yes" ]; then
  echo "已取消回滚"
  exit 0
fi

ln -sfn "${TARGET_JAR}" "${CURRENT_LINK}"

echo "回滚完成："
ls -l "${CURRENT_LINK}"
```

回滚流程建议：

```text
确认新版本异常
        ↓
暂停相关调度
        ↓
检查是否已有异常输出分区
        ↓
切换 Jar 软链接到上一稳定版本
        ↓
恢复配置文件到对应版本
        ↓
使用相同 bizDate 和 batchId 重跑
        ↓
校验输出数据和审计结果
        ↓
恢复调度
```

回滚方案建议如下：

| 建议                 | 说明                       |
| -------------------- | -------------------------- |
| 每次发布保留历史 Jar | 至少保留最近 3 到 5 个版本 |
| 使用软链接切换版本   | 回滚速度快                 |
| 配置也要可回滚       | 只回滚 Jar 不一定解决问题  |
| SQL 变更视为代码变更 | SQL 文件随版本发布         |
| 回滚后必须重跑校验   | 确认结果恢复正常           |
| 回滚操作记录审计     | 记录回滚版本、原因和操作者 |

### 发布检查

发布检查用于在生产上线前确认代码、依赖、配置、权限、资源、调度、监控和回滚方案都已经准备完成。发布检查应形成固定清单，避免靠个人经验。

发布前检查清单：

| 检查项           | 说明                             |
| ---------------- | -------------------------------- |
| 代码编译通过     | `mvn clean package` 成功         |
| 单元测试通过     | JUnit 测试通过                   |
| 集成测试通过     | local Spark 测试通过             |
| 测试环境验证通过 | 测试集群运行成功                 |
| Jar 版本正确     | 文件名和审计版本一致             |
| 依赖无冲突       | Spark、Hadoop、Scala 版本匹配    |
| 配置文件正确     | 使用 prod 配置                   |
| 权限验证通过     | Hive、HDFS、Kafka、JDBC 权限正常 |
| 目标表存在       | 表结构、分区字段、存储格式正确   |
| 调度参数正确     | bizDate、batchId、env、writeMode |
| 资源参数合理     | Executor、内存、队列确认         |
| 监控告警配置完成 | 失败、延迟、数据量告警           |
| 回滚版本可用     | 上一版本 Jar 和配置存在          |

发布检查脚本示例：

```bash
#!/usr/bin/env bash

# bin/release-check.sh
# Spark 任务发布前检查

set -e

APP_HOME=/opt/app/spark-job
APP_JAR=${APP_HOME}/lib/spark-job.jar
APP_CONF=${APP_HOME}/conf/application-prod.yml

echo "开始发布检查"

if [ ! -f "${APP_JAR}" ]; then
  echo "应用 Jar 不存在：${APP_JAR}"
  exit 1
fi

if [ ! -f "${APP_CONF}" ]; then
  echo "生产配置不存在：${APP_CONF}"
  exit 1
fi

echo "检查 Jar：${APP_JAR}"
ls -lh "${APP_JAR}"

echo "检查配置：${APP_CONF}"
ls -lh "${APP_CONF}"

echo "检查 HDFS 生产目录"
hdfs dfs -ls /warehouse/prod >/dev/null

echo "检查 Spark History 目录"
hdfs dfs -ls /spark-history >/dev/null

echo "发布检查通过"
```

发布检查建议如下：

| 建议                   | 说明                              |
| ---------------------- | --------------------------------- |
| 发布前必须测试环境跑通 | 不直接上生产首跑                  |
| 检查目标表结构         | 字段顺序、类型、分区字段          |
| 检查覆盖范围           | 防止误覆盖整表                    |
| 检查调度依赖           | 上游任务和分区必须存在            |
| 检查告警接收人         | 失败后能及时通知                  |
| 发布后首批重点观察     | 查看 Spark UI、日志、审计和数据量 |

## 运维管理

本章节用于说明 Spark 任务上线后的日常运维方式，包括任务重跑、任务补数、任务暂停、任务下线、日志排查、失败定位、资源排查和数据修复。运维管理的目标是让任务运行状态可控，问题处理有流程，数据修复可追溯。

### 任务重跑

任务重跑用于处理单次任务失败、上游数据延迟、临时资源问题、权限修复后重新执行等场景。重跑必须保证输出幂等，避免重复写入或结果叠加。

离线任务重跑命令：

```bash
/opt/app/spark-job/bin/start-user-summary.sh 2026-05-11 202605110001_rerun
```

重跑前检查目标分区：

```sql
SHOW PARTITIONS ads.ads_user_summary PARTITION(dt='2026-05-11');

SELECT COUNT(1)
FROM ads.ads_user_summary
WHERE dt = '2026-05-11';
```

重跑流程：

```text
确认失败原因
        ↓
确认上游数据是否完整
        ↓
检查目标分区是否部分写入
        ↓
清理或覆盖目标分区
        ↓
使用相同业务日期重跑
        ↓
校验输出数据量和核心指标
        ↓
记录重跑审计
```

任务重跑建议如下：

| 建议                   | 说明                           |
| ---------------------- | ------------------------------ |
| 离线任务使用覆盖写入   | 指定分区覆盖最容易保证幂等     |
| 重跑使用独立 batchId   | 便于区分正常调度和重跑         |
| 重跑前确认目标数据状态 | 防止重复追加                   |
| 重跑后校验数据量       | 与历史或预期结果对比           |
| 重跑操作记录审计       | 记录原因、执行人和时间         |
| 流任务不要随意重跑     | 流任务通常通过 Checkpoint 恢复 |

### 任务补数

任务补数用于处理历史数据回刷、业务口径变更、上游延迟修复、历史分区缺失等情况。补数通常涉及多个业务日期，应限制并发，避免冲击集群。

补数脚本示例：

```bash
#!/usr/bin/env bash

# bin/backfill-user-summary.sh
# 用户汇总任务补数脚本

set -e

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)

START_DATE=$1
END_DATE=$2

if [ -z "${START_DATE}" ] || [ -z "${END_DATE}" ]; then
  echo "用法：./backfill-user-summary.sh 2026-05-01 2026-05-11"
  exit 1
fi

CURRENT_DATE="${START_DATE}"

while [ "${CURRENT_DATE}" != "$(date -d "${END_DATE} +1 day" +%F)" ]; do
  BATCH_ID="$(date -d "${CURRENT_DATE}" +%Y%m%d)_backfill_user_summary"

  echo "开始补数：bizDate=${CURRENT_DATE}, batchId=${BATCH_ID}"

  "${SCRIPT_DIR}/start-user-summary.sh" "${CURRENT_DATE}" "${BATCH_ID}"

  echo "完成补数：bizDate=${CURRENT_DATE}"

  CURRENT_DATE=$(date -d "${CURRENT_DATE} +1 day" +%F)
done

echo "补数完成：startDate=${START_DATE}, endDate=${END_DATE}"
```

补数执行示例：

```bash
/opt/app/spark-job/bin/backfill-user-summary.sh 2026-05-01 2026-05-11
```

补数建议如下：

| 建议               | 说明                         |
| ------------------ | ---------------------------- |
| 补数前确认上游完整 | 上游未修复时补数无意义       |
| 使用独立补数队列   | 避免影响日常生产任务         |
| 控制补数并发       | 大范围补数不要一次性并发过多 |
| 使用覆盖写入       | 历史分区重算应覆盖目标分区   |
| 补数后做汇总校验   | 对比历史趋势和核心指标       |
| 补数记录单独审计   | 与日常调度区分               |

### 任务暂停

任务暂停用于临时停止调度，例如上游数据异常、目标表维护、业务口径调整、资源紧张、发布窗口冻结等。暂停任务时应明确影响范围和恢复条件。

任务暂停方式：

| 方式                   | 说明                       |
| ---------------------- | -------------------------- |
| 调度平台禁用任务       | 推荐方式，保留任务定义     |
| Crontab 注释任务       | 简单定时任务适用           |
| 停止流任务 Application | 实时任务需要停止运行中应用 |
| 设置任务开关           | 代码读取配置后跳过执行     |
| 队列资源限制           | 运维侧临时限制资源         |

Crontab 暂停示例：

```bash
# 原任务
# 0 2 * * * /opt/app/spark-job/bin/start-user-summary.sh $(date -d "yesterday" +\%F)

# 暂停后注释
# 0 2 * * * /opt/app/spark-job/bin/start-user-summary.sh $(date -d "yesterday" +\%F)
```

任务开关示例：

```yaml
# application-prod.yml
job:
  user-summary:
    enabled: false
```

任务暂停建议如下：

| 建议               | 说明                             |
| ------------------ | -------------------------------- |
| 暂停前评估下游影响 | 下游报表、接口、宽表可能受影响   |
| 记录暂停原因       | 便于恢复和审计                   |
| 设置恢复条件       | 例如上游修复、发布完成、资源恢复 |
| 暂停期间监控积压   | 实时链路尤其需要关注 Kafka Lag   |
| 恢复后考虑补数     | 暂停期间缺失的分区需要补跑       |
| 不直接删除任务定义 | 临时暂停应可恢复                 |

### 任务下线

任务下线用于永久停止不再使用的 Spark 任务。下线不仅是停止调度，还需要清理任务脚本、配置、权限、监控、告警、文档和数据依赖。

任务下线流程：

```text
确认业务方不再使用
        ↓
确认无下游依赖
        ↓
停止调度任务
        ↓
观察一段时间
        ↓
移除脚本和配置
        ↓
回收权限和资源
        ↓
关闭监控告警
        ↓
归档代码和文档
```

下线检查清单：

| 检查项   | 说明                                        |
| -------- | ------------------------------------------- |
| 业务确认 | 业务方确认任务不再需要                      |
| 下游确认 | 没有报表、接口、任务依赖                    |
| 调度关闭 | DolphinScheduler / Airflow / Crontab 已禁用 |
| 数据保留 | 历史数据按保留策略处理                      |
| 权限回收 | HDFS、Hive、Kafka、JDBC 权限回收            |
| 告警关闭 | 避免无效告警                                |
| 文档更新 | 标记任务已下线                              |
| 代码归档 | 保留历史版本和下线记录                      |

任务下线建议如下：

| 建议               | 说明                         |
| ------------------ | ---------------------------- |
| 不直接删除历史数据 | 先确认数据保留策略           |
| 下线前保留观察期   | 停止调度后观察是否有业务反馈 |
| 权限及时回收       | 减少安全风险                 |
| 调度定义归档       | 便于历史追溯                 |
| 审计表保留下线记录 | 记录下线时间和原因           |
| 下游依赖必须确认   | 防止误下线核心链路           |

### 日志排查

日志排查用于分析任务失败、数据异常、性能问题和外部依赖问题。Spark 日志通常分散在调度平台、提交脚本、YARN、Driver、Executor 和业务审计表中。

常用日志查看命令：

```bash
# 查看 YARN 应用日志
yarn logs -applicationId application_1710000000000_0001

# 查看最近提交日志
tail -n 200 /opt/app/spark-job/logs/submit-user-summary.log

# 按关键字过滤异常
yarn logs -applicationId application_1710000000000_0001 | grep -i "exception"

# 查看任务失败关键字
yarn logs -applicationId application_1710000000000_0001 | grep -E "ERROR|FAILED|Exception|Caused by"
```

常见日志关键字：

| 关键字                    | 可能含义            |
| ------------------------- | ------------------- |
| `ClassNotFoundException`  | 缺少依赖            |
| `NoSuchMethodError`       | 依赖版本冲突        |
| `OutOfMemoryError`        | 内存不足            |
| `Permission denied`       | 权限不足            |
| `Table or view not found` | Hive 表或视图不存在 |
| `Path does not exist`     | 输入路径不存在      |
| `TimeoutException`        | 网络或外部服务超时  |
| `Task not serializable`   | 闭包对象不可序列化  |

日志排查建议如下：

| 建议                  | 说明                             |
| --------------------- | -------------------------------- |
| 先定位 Application ID | 所有集群日志都依赖它             |
| 先看 Driver 日志      | 主流程异常通常在 Driver          |
| 再看 Executor 日志    | UDF、Task、写出异常常在 Executor |
| 搜索 `Caused by`      | Java 异常根因通常在最后几段      |
| 结合 Spark UI         | 日志说明原因，UI 说明发生位置    |
| 保留关键日志片段      | 便于复盘和问题单记录             |

### 失败定位

失败定位用于快速判断任务失败属于参数问题、数据问题、权限问题、依赖问题、资源问题还是外部系统问题。定位时应先分类，再处理，避免盲目重跑。

失败分类表：

| 类型     | 常见表现                    | 处理方式             |
| -------- | --------------------------- | -------------------- |
| 参数异常 | 缺少参数、格式错误          | 修复调度参数后重跑   |
| 数据异常 | 空数据、重复、字段缺失      | 检查上游和质量规则   |
| 权限异常 | Permission denied           | 修复账号和路径权限   |
| 依赖异常 | ClassNotFound、NoSuchMethod | 调整依赖和打包       |
| 资源异常 | OOM、Container killed       | 调整资源、分区、倾斜 |
| 网络异常 | Timeout、Connection refused | 检查外部服务和网络   |
| SQL 异常 | 字段不存在、语法错误        | 修复 SQL 或表结构    |

失败定位流程：

```text
查看调度平台状态
        ↓
获取 Application ID
        ↓
查看 Driver 日志
        ↓
搜索 ERROR / Caused by
        ↓
结合 Spark UI 查看失败 Stage
        ↓
判断异常类型
        ↓
修复后重跑或回滚
```

失败定位建议如下：

| 建议                       | 说明                          |
| -------------------------- | ----------------------------- |
| 不要先盲目加资源           | 先确认失败类型                |
| 参数和权限问题无需多次重试 | 修复后再跑                    |
| 数据质量失败要查上游       | 不应简单跳过                  |
| 依赖问题要看构建产物       | 检查 Fat Jar 和 provided 配置 |
| 资源问题要看 Spark UI      | 确认 OOM、倾斜还是分区不足    |
| 失败结论写入问题记录       | 便于后续复盘                  |

### 资源排查

资源排查用于分析任务等待资源、执行慢、Executor 丢失、OOM、Shuffle 失败、GC 时间过长等问题。资源排查需要结合 YARN UI、Spark UI 和任务日志。

常见资源问题：

| 问题                | 可能原因                             |
| ------------------- | ------------------------------------ |
| 应用长时间 ACCEPTED | 队列资源不足或无权限                 |
| Executor OOM        | 单 Task 数据过大、倾斜、缓存过多     |
| Driver OOM          | collect 大结果、广播过大、小文件过多 |
| Container killed    | 超出 YARN 内存限制                   |
| Task 很慢           | 数据倾斜或外部存储慢                 |
| Shuffle 失败        | 磁盘、网络、Executor 丢失            |
| GC 时间过长         | 内存不足或对象创建过多               |

资源排查命令：

```bash
# 查看应用状态
yarn application -status application_1710000000000_0001

# 查看队列资源
yarn queue -status root.prod.etl

# 查看应用日志
yarn logs -applicationId application_1710000000000_0001
```

Spark UI 资源排查重点：

| 页面        | 查看内容                            |
| ----------- | ----------------------------------- |
| Executors   | Executor 内存、GC、失败数量         |
| Stages      | Task 耗时分布、Shuffle Read / Write |
| SQL         | 执行计划、Exchange、Join 策略       |
| Storage     | 缓存是否占用过多                    |
| Environment | 实际生效 Spark 参数                 |

资源排查建议如下：

| 建议                            | 说明                         |
| ------------------------------- | ---------------------------- |
| 先看队列是否有资源              | 应用 ACCEPTED 多为资源问题   |
| OOM 先看是否数据倾斜            | 不一定是内存配置太小         |
| Driver OOM 检查 collect         | 大结果不能拉回 Driver        |
| Executor OOM 检查分区大小       | 增加分区可降低单 Task 数据量 |
| Shuffle 失败检查磁盘和 Executor | 可能是节点异常或数据量过大   |
| 调参后记录对比结果              | 资源参数变更需要可追踪       |

### 数据修复

数据修复用于处理已产出的错误数据、缺失分区、重复写入、脏数据过多、业务口径变更等问题。数据修复必须有明确范围、修复脚本、校验步骤和审计记录。

常见数据修复场景：

| 场景             | 修复方式                 |
| ---------------- | ------------------------ |
| 目标分区缺失     | 指定 bizDate 重跑        |
| 目标分区数据错误 | 删除或覆盖指定分区后重算 |
| 追加写入重复     | 按主键去重后重写         |
| 上游数据延迟     | 上游补齐后补跑下游       |
| 维表错误         | 修复维表后重建宽表和指标 |
| 业务口径变更     | 按历史日期范围回刷       |
| 脏数据误入主表   | 过滤后重写分区           |

Hive 分区修复示例：

```sql
-- 删除错误分区
ALTER TABLE ads.ads_user_summary DROP IF EXISTS PARTITION (dt = '2026-05-11');

-- 重新补跑任务后检查结果
SELECT COUNT(1)
FROM ads.ads_user_summary
WHERE dt = '2026-05-11';
```

HDFS 路径修复示例：

```bash
# 备份错误分区
hdfs dfs -mv \
  /warehouse/prod/ads/user_summary/dt=2026-05-11 \
  /warehouse/prod/backup/user_summary/dt=2026-05-11_$(date +%Y%m%d%H%M%S)

# 重新执行任务
/opt/app/spark-job/bin/start-user-summary.sh 2026-05-11 202605110001_repair
```

重复数据修复 SQL 示例：

```sql
INSERT OVERWRITE TABLE ads.ads_user_summary PARTITION (dt = '2026-05-11')
SELECT
    user_id,
    user_name,
    order_count,
    order_amount,
    etl_time
FROM (
    SELECT
        user_id,
        user_name,
        order_count,
        order_amount,
        etl_time,
        ROW_NUMBER() OVER(PARTITION BY user_id ORDER BY etl_time DESC) AS rn
    FROM ads.ads_user_summary
    WHERE dt = '2026-05-11'
) t
WHERE rn = 1;
```

数据修复建议如下：

| 建议               | 说明                                         |
| ------------------ | -------------------------------------------- |
| 修复前先备份       | 防止二次修复困难                             |
| 修复范围必须明确   | 指定表、分区、日期和字段                     |
| 不直接全表覆盖     | 除非确认影响范围                             |
| 修复后做质量校验   | 数据量、唯一性、核心指标必须校验             |
| 修复记录写审计     | 记录修复原因、操作者、时间和结果             |
| 下游影响要同步修复 | 上游分区修复后，下游宽表和指标也可能需要重跑 |


## 常见问题

本章节用于整理 Spark Java 项目开发、打包、提交、运行和运维过程中常见的问题。排查 Spark 问题时，应优先获取 `Application ID`，再结合 Driver 日志、Executor 日志、Spark UI、YARN UI、History Server 和任务审计信息定位原因。

### ClassNotFoundException

`ClassNotFoundException` 表示运行时找不到指定类。该问题通常发生在 Spark 集群运行阶段，而不是本地编译阶段。

常见表现如下：

```text
java.lang.ClassNotFoundException: com.mysql.cj.jdbc.Driver
java.lang.ClassNotFoundException: org.apache.spark.sql.kafka010.KafkaSourceProvider
java.lang.ClassNotFoundException: io.github.atengk.spark.Application
```

常见原因：

| 原因                | 说明                                  |
| ------------------- | ------------------------------------- |
| Fat Jar 未包含依赖  | 例如 JDBC 驱动、业务 SDK 未打入 Jar   |
| `--jars` 未提交依赖 | 外部依赖没有分发到 Driver 和 Executor |
| 主类配置错误        | `--class` 指定的类名不正确            |
| Jar 路径错误        | 提交的不是最新 Jar 或 Jar 不存在      |
| 依赖 scope 配错     | 需要运行时依赖却配置成 `provided`     |
| 集群缺少 Connector  | Kafka、S3、OSS 等 Connector 未提供    |

排查命令：

```bash
# 查看 Jar 中是否包含主类
jar tf target/spark-job.jar | grep "io/github/atengk/spark/Application.class"

# 查看 Jar 中是否包含 MySQL 驱动
jar tf target/spark-job.jar | grep "com/mysql/cj/jdbc/Driver.class"

# 查看 Maven 依赖树
mvn dependency:tree

# 查看指定依赖是否存在
mvn dependency:tree -Dincludes=com.mysql
```

处理方式：

| 场景                   | 处理方式                                       |
| ---------------------- | ---------------------------------------------- |
| 主类找不到             | 检查 `--class` 和包名是否一致                  |
| JDBC 驱动找不到        | 将 JDBC 驱动打入 Fat Jar 或通过 `--jars` 提交  |
| Kafka Connector 找不到 | 添加 `spark-sql-kafka-0-10` 依赖               |
| 业务类找不到           | 确认提交的是最新构建产物                       |
| 集群依赖找不到         | 通过 `--packages`、`--jars` 或集群公共依赖解决 |

Kafka Connector 依赖示例：

```xml
<!-- Spark Kafka Connector：用于 Structured Streaming 读取和写入 Kafka -->
<dependency>
    <groupId>org.apache.spark</groupId>
    <artifactId>spark-sql-kafka-0-10_2.12</artifactId>
    <version>${spark.version}</version>
</dependency>
```

### NoSuchMethodError

`NoSuchMethodError` 表示运行时加载到的类版本中不存在调用的方法。该问题通常不是代码写错，而是依赖版本冲突。

常见表现如下：

```text
java.lang.NoSuchMethodError: com.fasterxml.jackson.databind.ObjectMapper.readerFor
java.lang.NoSuchMethodError: com.google.common.base.Preconditions.checkArgument
java.lang.NoSuchMethodError: org.apache.hadoop.fs.FileSystem.get
```

常见原因：

| 原因                                 | 说明                                  |
| ------------------------------------ | ------------------------------------- |
| 本地依赖和集群依赖版本不一致         | 编译期和运行期加载的类不同            |
| Fat Jar 打入了 Spark/Hadoop 自带依赖 | 覆盖了集群运行时依赖                  |
| 第三方 SDK 引入旧版本依赖            | 常见于 Jackson、Guava、Netty          |
| Scala 后缀不一致                     | `_2.11`、`_2.12` 混用                 |
| Spark 版本不匹配                     | Connector 版本和 Spark 集群版本不一致 |

排查命令：

```bash
# 查看 Jackson 依赖来源
mvn dependency:tree -Dincludes=com.fasterxml.jackson.core

# 查看 Guava 依赖来源
mvn dependency:tree -Dincludes=com.google.guava

# 查看 Spark 依赖
mvn dependency:tree -Dincludes=org.apache.spark

# 检查 Jar 中是否误打入 Hadoop 类
jar tf target/spark-job.jar | grep "org/apache/hadoop" | head
```

处理方式：

| 场景               | 处理方式                                  |
| ------------------ | ----------------------------------------- |
| Spark/Hadoop 冲突  | Spark、Hadoop、Hive 依赖设置为 `provided` |
| Jackson 冲突       | 统一 Jackson 版本或排除旧依赖             |
| Guava 冲突         | 排除第三方 SDK 中冲突版本                 |
| Scala 冲突         | 所有 Spark 依赖保持相同 Scala 后缀        |
| Connector 版本冲突 | 使用与 Spark 集群版本匹配的 Connector     |

依赖排除示例：

```xml
<!-- 外部 SDK 可能引入旧版本 Jackson，需要根据 dependency:tree 排查后排除 -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>example-client</artifactId>
    <version>${example.version}</version>
    <exclusions>
        <exclusion>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </exclusion>
        <exclusion>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

### 依赖冲突

依赖冲突是 Spark 项目最常见问题之一，表现可能是类找不到、方法不存在、日志异常、序列化失败、运行时行为不一致等。

常见冲突依赖：

| 依赖          | 常见问题                            |
| ------------- | ----------------------------------- |
| Spark         | 与集群 Spark 版本不一致             |
| Hadoop        | 与集群 Hadoop 版本不一致            |
| Hive          | Metastore、SerDe、Hive UDF 兼容问题 |
| Scala         | `_2.11`、`_2.12` 混用               |
| Jackson       | Spark、Hadoop、Kafka、SDK 版本冲突  |
| Guava         | Hadoop 与第三方 SDK 冲突            |
| SLF4J / Log4j | 多个日志绑定或日志实现冲突          |
| Netty         | Kafka、Hadoop、对象存储 SDK 冲突    |

推荐依赖原则：

| 依赖类型                | 推荐 scope                  |
| ----------------------- | --------------------------- |
| Spark Core / SQL / Hive | `provided`                  |
| Hadoop Client           | `provided`                  |
| Scala Library           | `provided`                  |
| Hutool                  | 默认打包                    |
| Lombok                  | `provided` 或 `compileOnly` |
| JDBC 驱动               | 默认打包或通过 `--jars`     |
| 业务 SDK                | 默认打包                    |
| Kafka Connector         | 根据集群情况决定            |

依赖树检查：

```bash
# 查看完整依赖树
mvn dependency:tree

# 输出到文件便于检索
mvn dependency:tree > dependency-tree.txt

# 检查重复日志实现
mvn dependency:tree | grep -E "slf4j|log4j|logback"
```

依赖冲突处理建议如下：

| 建议                        | 说明                                   |
| --------------------------- | -------------------------------------- |
| 先看依赖树                  | 不凭经验盲目排除                       |
| 集群已有依赖使用 provided   | 避免 Fat Jar 覆盖集群依赖              |
| 业务依赖进入 Fat Jar        | 避免运行时找不到类                     |
| 日志实现只保留一个          | 避免 SLF4J 多绑定                      |
| Connector 版本跟 Spark 对齐 | Kafka、Hive、Delta、Iceberg 等都要匹配 |
| 打包后检查 Jar 内容         | 确认没有误打入大范围集群依赖           |

### Executor 内存溢出

Executor 内存溢出通常发生在实际计算阶段，常见于大 Join、大聚合、窗口函数、缓存大表、单分区数据过大、数据倾斜等场景。

常见表现：

```text
java.lang.OutOfMemoryError: Java heap space
Container killed by YARN for exceeding memory limits
ExecutorLostFailure
GC overhead limit exceeded
```

常见原因：

| 原因                | 说明                              |
| ------------------- | --------------------------------- |
| 单个分区数据过大    | 分区数过少，单 Task 压力过大      |
| 数据倾斜            | 热点 Key 集中到少数 Task          |
| Join 数据过大       | 大表 Join 未优化                  |
| 缓存过多            | Executor 内存被缓存占满           |
| 字段过宽            | Shuffle 或缓存携带大量无关字段    |
| memoryOverhead 不足 | 堆外内存不足导致 Container killed |

处理方式：

| 问题       | 处理方式                                        |
| ---------- | ----------------------------------------------- |
| 单分区过大 | 增加分区数，调整 `spark.sql.shuffle.partitions` |
| 数据倾斜   | 过滤热点 Key、加盐、拆分热点 Key、开启 AQE      |
| Join 过大  | Join 前裁剪字段，小表广播                       |
| 缓存过多   | 取消无用缓存，使用 `MEMORY_AND_DISK`            |
| 字段过宽   | 先 `select` 必要字段                            |
| 堆外不足   | 增加 `spark.executor.memoryOverhead`            |

配置示例：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --executor-memory 6g \
  --executor-cores 2 \
  --num-executors 10 \
  --conf spark.executor.memoryOverhead=2g \
  --conf spark.sql.shuffle.partitions=600 \
  --conf spark.sql.adaptive.enabled=true \
  --conf spark.sql.adaptive.skewJoin.enabled=true \
  target/spark-job.jar \
  --jobName user-summary \
  --env prod \
  --bizDate 2026-05-11
```

### Driver 内存溢出

Driver 内存溢出通常是因为将大量数据、元数据或执行计划集中到 Driver。Driver 不应承担大规模数据处理。

常见表现：

```text
java.lang.OutOfMemoryError: Java heap space
Total size of serialized results is bigger than spark.driver.maxResultSize
Driver exited with code 1
```

常见原因：

| 原因                        | 说明                      |
| --------------------------- | ------------------------- |
| 使用 `collect()` 拉取大结果 | 大量数据回到 Driver       |
| 使用 `toPandas()`           | PySpark 场景常见          |
| 广播对象过大                | Driver 需要先构建广播数据 |
| 小文件过多                  | Driver 维护大量文件元数据 |
| SQL 过度复杂                | 执行计划过大              |
| `show` 大量数据             | 调试时误输出过多行        |

不推荐写法：

```java
// 不推荐：大数据量会全部拉回 Driver
List<Row> rows = resultDf.collectAsList();
```

推荐写法：

```java
// 推荐：只获取少量样例
List<Row> rows = resultDf.limit(100).collectAsList();
```

处理建议：

| 建议                     | 说明                     |
| ------------------------ | ------------------------ |
| 禁止大规模 `collect`     | 大数据留在 Executor 处理 |
| 调试使用 `limit`         | 只取少量样例             |
| 增大 Driver 内存只是兜底 | 根因通常是代码写法问题   |
| 合并小文件               | 降低文件元数据压力       |
| 拆分复杂 SQL             | 降低执行计划复杂度       |
| 控制广播数据大小         | 大维表不要强制广播       |

### Shuffle 失败

Shuffle 失败通常发生在 Join、聚合、去重、排序、窗口函数和 repartition 操作中。Shuffle 涉及网络、磁盘、内存和 Executor 稳定性，问题来源较多。

常见表现：

```text
org.apache.spark.shuffle.FetchFailedException
MetadataFetchFailedException
ExecutorLostFailure
No space left on device
Connection reset by peer
```

常见原因：

| 原因               | 说明                      |
| ------------------ | ------------------------- |
| Executor 丢失      | 导致 Shuffle 文件不可获取 |
| 磁盘空间不足       | Shuffle 写临时文件失败    |
| Shuffle 数据量过大 | 网络和磁盘压力过大        |
| 分区数过少         | 单个 Shuffle 分区过大     |
| 数据倾斜           | 少数 Reduce Task 数据过大 |
| 网络不稳定         | Fetch Shuffle 文件失败    |

处理方式：

| 问题             | 处理方式                            |
| ---------------- | ----------------------------------- |
| 分区过少         | 增加 `spark.sql.shuffle.partitions` |
| Shuffle 数据过宽 | Join / 聚合前裁剪字段               |
| 数据倾斜         | 识别热点 Key 并加盐或拆分           |
| Executor 丢失    | 检查 YARN 日志、节点状态和内存      |
| 磁盘不足         | 清理节点磁盘或调整本地目录          |
| 网络抖动         | 检查集群网络和节点稳定性            |

Shuffle 优化示例：

```java
Dataset<Row> selectedDf = sourceDf
        .filter(col("dt").equalTo(bizDate))
        .select("user_id", "order_id", "order_amount");

Dataset<Row> resultDf = selectedDf
        .groupBy("user_id")
        .agg(
                count("order_id").alias("order_count"),
                sum("order_amount").alias("order_amount")
        );
```

### 数据倾斜

数据倾斜是指少数 Key 的数据量远大于其他 Key，导致少数 Task 特别慢或 OOM。数据倾斜是 Spark 性能问题中最常见也最容易被误判的问题之一。

常见表现：

| 表现                  | 说明                                     |
| --------------------- | ---------------------------------------- |
| 个别 Task 极慢        | Spark UI 中少数 Task 耗时远高于其他 Task |
| Shuffle Read 极不均匀 | 少数 Task 读取数据量巨大                 |
| Executor OOM          | 热点 Key 集中到单个 Executor             |
| Join 长时间卡住       | 最后几个 Task 长时间不结束               |
| 输出文件大小不均      | 某些 part 文件异常大                     |

识别热点 Key：

```java
Dataset<Row> hotKeyDf = sourceDf
        .groupBy("user_id")
        .count()
        .orderBy(col("count").desc());

hotKeyDf.show(20, false);
```

常见处理方式：

| 方式          | 适用场景                         |
| ------------- | -------------------------------- |
| 过滤无效 Key  | null、空字符串、UNKNOWN 造成倾斜 |
| 小表广播      | 大表 Join 小维表                 |
| 加盐 Join     | 大表 Join 中存在热点 Key         |
| 拆分热点 Key  | 少数热点 Key 可单独处理          |
| AQE Skew Join | Spark 3.x 自动优化部分倾斜       |
| 业务口径调整  | 某些热点 Key 无业务意义          |

加盐 Join 示例：

```java
Dataset<Row> factSaltDf = factDf.withColumn(
        "salt",
        floor(rand().multiply(10))
);

Dataset<Row> dimSaltDf = dimDf
        .withColumn("salt_array", array(
                lit(0), lit(1), lit(2), lit(3), lit(4),
                lit(5), lit(6), lit(7), lit(8), lit(9)
        ))
        .withColumn("salt", explode(col("salt_array")))
        .drop("salt_array");

Dataset<Row> resultDf = factSaltDf.join(
        dimSaltDf,
        factSaltDf.col("user_id").equalTo(dimSaltDf.col("user_id"))
                .and(factSaltDf.col("salt").equalTo(dimSaltDf.col("salt"))),
        "left"
);
```

### 小文件过多

小文件过多会影响 HDFS NameNode、对象存储 List 性能、Spark 读取效率、Hive 查询性能和调度开销。

常见原因：

| 原因           | 说明                                |
| -------------- | ----------------------------------- |
| 写出分区数过多 | 每个 Spark 分区通常至少输出一个文件 |
| Hive 分区过细  | 按高基数字段分区                    |
| 流任务频繁写入 | 每个微批次产生小文件                |
| 多次 append    | 同一分区多批次追加                  |
| 上游源文件过小 | 读取阶段已有大量小文件              |

查看文件数量：

```bash
hdfs dfs -count /warehouse/prod/dwd/user_event/dt=2026-05-11

hdfs dfs -ls /warehouse/prod/dwd/user_event/dt=2026-05-11 | wc -l
```

写出前控制文件数：

```java
resultDf.coalesce(20)
        .write()
        .mode(SaveMode.Overwrite)
        .parquet("hdfs:///warehouse/ads/user_summary/dt=2026-05-11");
```

按分区字段重分区：

```java
resultDf.repartition(100, col("dt"))
        .write()
        .mode(SaveMode.Append)
        .partitionBy("dt")
        .parquet("hdfs:///warehouse/dwd/user_event");
```

处理建议：

| 建议                     | 说明                                  |
| ------------------------ | ------------------------------------- |
| 写出前控制分区数         | 使用 `coalesce` 或合理 `repartition`  |
| 避免高基数字段分区       | 不使用 user_id、order_id 作为分区字段 |
| 定期 Compaction          | 对流式追加表定期合并                  |
| 控制 `maxRecordsPerFile` | 避免单文件过大或过小                  |
| 目标文件大小合理         | 常见目标 128MB 到 512MB               |
| 对象存储更要控制小文件   | 小文件会放大请求成本和查询延迟        |

### Kafka Offset 异常

Kafka Offset 异常通常发生在 Structured Streaming 任务重启、Checkpoint 丢失、Kafka 消息过期、Topic 分区变化或 `failOnDataLoss=true` 的情况下。

常见表现：

```text
OffsetOutOfRangeException
Set() are gone. Some data may have been missed
Cannot fetch offset
Kafka topic partition offset was changed
```

常见原因：

| 原因                       | 说明                        |
| -------------------------- | --------------------------- |
| Kafka 消息被清理           | Offset 已不在保留范围内     |
| Checkpoint 被删除          | Spark 无法从历史进度恢复    |
| Topic 被重建               | Offset 语义发生变化         |
| 分区数量变化               | 消费进度和 Topic 元数据变化 |
| `startingOffsets` 设置不当 | 新任务启动点不符合预期      |
| 多任务共用 Checkpoint      | 状态互相污染                |

常用配置：

```java
Dataset<Row> kafkaDf = spark.readStream()
        .format("kafka")
        .option("kafka.bootstrap.servers", "kafka-host:9092")
        .option("subscribe", "ods_user_event")
        .option("startingOffsets", "latest")
        .option("failOnDataLoss", "false")
        .option("maxOffsetsPerTrigger", "100000")
        .load();
```

处理建议：

| 场景            | 处理方式                                   |
| --------------- | ------------------------------------------ |
| Offset 被清理   | 评估是否允许跳过历史数据，必要时重新初始化 |
| Checkpoint 损坏 | 备份旧 Checkpoint 后重建任务               |
| Topic 重建      | 明确新 Topic 起始 Offset                   |
| 数据不能丢      | 需要从备份、落盘数据或重放链路恢复         |
| 消费积压严重    | 增加资源、调大每批处理能力                 |
| 重复消费        | 下游按业务主键或 Offset 去重               |

Kafka Offset 管理建议：

| 建议                          | 说明                       |
| ----------------------------- | -------------------------- |
| 生产流任务必须配置 Checkpoint | Offset 恢复依赖 Checkpoint |
| 不随意删除 Checkpoint         | 删除后可能重复或跳过数据   |
| Kafka 保留时间覆盖故障窗口    | 防止任务停机后 Offset 过期 |
| 保留 topic、partition、offset | 便于排查重复和丢失         |
| 下游写入要幂等                | 重启和重试可能导致重复处理 |

### Hive 分区异常

Hive 分区异常通常发生在分区字段为空、分区未修复、动态分区配置错误、写出字段顺序不匹配、分区路径与元数据不一致等场景。

常见表现：

```text
Partition not found
Dynamic partition strict mode requires at least one static partition column
Cannot insert into table because the number of columns are different
Path does not exist
```

常见原因：

| 原因           | 说明                        |
| -------------- | --------------------------- |
| 分区字段为空   | 写出 `dt=null` 或异常分区   |
| 字段顺序不一致 | `insertInto` 按字段位置写入 |
| 动态分区未开启 | Hive 动态分区配置不足       |
| 元数据未同步   | HDFS 有目录但 Hive 无分区   |
| 分区路径被删除 | Hive 有分区但底层路径不存在 |
| 覆盖范围错误   | 误覆盖整表或错误分区        |

查看分区：

```sql
SHOW PARTITIONS dwd.dwd_user_event;

SHOW PARTITIONS dwd.dwd_user_event PARTITION(dt='2026-05-11');
```

修复分区：

```sql
MSCK REPAIR TABLE dwd.dwd_user_event;
```

动态分区配置：

```java
spark.sql("SET hive.exec.dynamic.partition=true");
spark.sql("SET hive.exec.dynamic.partition.mode=nonstrict");
spark.conf().set("spark.sql.sources.partitionOverwriteMode", "dynamic");
```

写入前显式调整字段顺序：

```java
Dataset<Row> hiveDf = resultDf.select(
        "event_id",
        "user_id",
        "event_type",
        "event_time",
        "etl_time",
        "dt"
);

hiveDf.write()
        .mode(SaveMode.Append)
        .insertInto("dwd.dwd_user_event");
```

Hive 分区异常处理建议：

| 建议                   | 说明                          |
| ---------------------- | ----------------------------- |
| 写入前校验分区字段     | `dt` 不允许为空或格式错误     |
| 字段顺序与 Hive 表一致 | 特别是 `insertInto`           |
| 动态分区配置明确       | 不依赖默认配置                |
| 不随意执行全表覆盖     | 覆盖必须限定分区              |
| 元数据和路径都要检查   | Hive 分区与 HDFS 路径必须一致 |
| 修复前先确认影响范围   | `MSCK REPAIR` 大表可能较慢    |

## 项目规范

本章节用于规范 Spark Java 项目的代码、包结构、配置、SQL、日志、异常、Git、提交信息和版本发布。规范的目标是提升代码可读性、任务可维护性、问题可排查性和发布可追踪性。

### 代码命名规范

Java 代码命名应遵循清晰、稳定、语义明确的原则。Spark 项目中类名应体现组件职责，例如 Job、Reader、Transformer、Writer、Service、Config、Util、Exception。

命名建议如下：

| 类型           | 命名示例               | 说明               |
| -------------- | ---------------------- | ------------------ |
| Job 类         | `UserSummaryJob`       | 表示一个可调度任务 |
| Service 类     | `UserSummaryService`   | 表示业务编排服务   |
| Reader 类      | `HiveUserReader`       | 表示读取组件       |
| Transformer 类 | `UserEventTransformer` | 表示转换组件       |
| Writer 类      | `HiveTableWriter`      | 表示写出组件       |
| Config 类      | `SparkRuntimeConfig`   | 表示配置对象       |
| Util 类        | `BizDateUtil`          | 表示无状态工具类   |
| Exception 类   | `DataReadException`    | 表示异常类型       |
| 常量类         | `SparkConstants`       | 表示常量集合       |

变量命名建议：

| 变量         | 示例                            |
| ------------ | ------------------------------- |
| SparkSession | `spark`                         |
| DataFrame    | `userDf`、`orderDf`、`resultDf` |
| 业务日期     | `bizDate`                       |
| 批次号       | `batchId`                       |
| 输入路径     | `inputPath`                     |
| 输出路径     | `outputPath`                    |
| 目标表       | `targetTable`                   |
| 配置参数     | `options`、`params`             |

类注释规范示例：

```java
package io.github.atengk.spark.job;

/**
 * 用户汇总 Spark 任务。
 *
 * @author Ateng
 * @since 2026-05-11
 */
public class UserSummaryJob {
}
```

代码命名建议如下：

| 建议                        | 说明                                     |
| --------------------------- | ---------------------------------------- |
| 类名体现职责                | 不使用 `MainJob`、`TestJob` 这类模糊名称 |
| 方法名使用动词开头          | `read`、`transform`、`write`、`validate` |
| 变量名表达业务含义          | 不使用 `df1`、`df2`、`tmp`               |
| DataFrame 命名带层级或业务  | `odsUserDf`、`dwdOrderDf`                |
| 工具类使用 `Util` 后缀      | 保持无状态                               |
| 异常类使用 `Exception` 后缀 | 便于识别异常类型                         |

### 包结构规范

包结构应按职责分层，而不是按单个任务随意堆叠。推荐将入口、配置、任务、读取、转换、写出、服务、质量、日志、监控、异常和工具类分开管理。

推荐包结构：

```text
src/main/java/io/github/atengk/spark
├── Application.java
├── audit
├── config
├── constant
├── exception
├── job
├── log
├── monitor
├── param
├── quality
├── reader
├── security
├── service
├── sql
├── stream
├── transformer
├── util
└── writer
```

各包职责说明：

| 包名          | 职责                     |
| ------------- | ------------------------ |
| `job`         | Spark 任务入口和任务抽象 |
| `service`     | 业务流程编排             |
| `reader`      | 数据读取组件             |
| `transformer` | 数据清洗、转换、聚合     |
| `writer`      | 数据写出组件             |
| `config`      | 配置对象和配置加载       |
| `param`       | 参数解析和校验           |
| `quality`     | 数据质量校验             |
| `sql`         | SQL 文件加载、模板渲染   |
| `exception`   | 自定义异常               |
| `util`        | 通用工具类               |
| `monitor`     | 指标、监控、监听器       |
| `security`    | 脱敏、权限、安全相关     |
| `audit`       | 审计实体和审计输出       |

包结构规范建议：

| 建议                         | 说明                           |
| ---------------------------- | ------------------------------ |
| 按职责分包                   | 不按临时功能随意建包           |
| Job 保持轻量                 | 复杂逻辑下沉到 Service         |
| Reader / Writer 不写业务逻辑 | 保持组件职责单一               |
| Transformer 不做写出         | 输入 DataFrame，输出 DataFrame |
| Util 不依赖业务状态          | 保持静态、无状态、可测试       |
| 包名小写                     | 符合 Java 包命名规范           |

### 配置命名规范

配置命名应稳定、可读、分层清晰。建议使用小写字母和中划线命名 YAML 配置，Java 字段使用驼峰命名。

配置文件命名：

| 文件                     | 说明           |
| ------------------------ | -------------- |
| `application.yml`        | 通用默认配置   |
| `application-local.yml`  | 本地配置       |
| `application-test.yml`   | 测试环境配置   |
| `application-prod.yml`   | 生产环境配置   |
| `logback.xml`            | 日志配置       |
| `hive-site.xml`          | Hive 配置      |
| `kafka_client_jaas.conf` | Kafka 安全配置 |

配置项示例：

```yaml
app:
  name: spark-job
  env: prod

warehouse:
  root-path: hdfs:///warehouse/prod
  error-path: hdfs:///error/prod
  checkpoint-path: hdfs:///checkpoint/prod

spark:
  sql:
    shuffle-partitions: 400
    adaptive-enabled: true

hive:
  source-db: dwd
  target-db: ads

kafka:
  bootstrap-servers: kafka-host:9092
  source-topic: ods_user_event
  sink-topic: dwd_user_event
```

配置命名建议：

| 建议                        | 说明                                     |
| --------------------------- | ---------------------------------------- |
| 按模块分层                  | app、spark、hive、kafka、jdbc、warehouse |
| 环境配置分文件              | local/test/prod 分离                     |
| 布尔值使用明确名称          | `enabled`、`allow-empty`、`overwrite`    |
| 路径配置使用 `path` 后缀    | `root-path`、`error-path`                |
| Topic 配置使用 `topic` 后缀 | `source-topic`、`sink-topic`             |
| 敏感配置不写入普通文件      | 密码、Secret 使用环境变量或密钥系统      |

### SQL 编写规范

Spark SQL 应保持可读、可维护、可优化。复杂 SQL 应外置到 `src/main/resources/sql` 目录中，不建议在 Java 代码中拼接大段 SQL。

SQL 文件目录建议：

```text
src/main/resources/sql
├── ods
├── dwd
├── dws
├── ads
└── dim
```

SQL 编写示例：

```sql
-- sql/ads/ads_user_order_summary.sql
-- 用户订单日汇总指标
SELECT
    u.user_id,
    u.user_name,
    COUNT(o.order_id) AS order_count,
    COALESCE(SUM(o.order_amount), 0) AS order_amount,
    '${bizDate}' AS dt
FROM dwd.dwd_user_detail u
LEFT JOIN dwd.dwd_order_detail o
    ON u.user_id = o.user_id
   AND o.dt = '${bizDate}'
WHERE u.dt = '${bizDate}'
GROUP BY
    u.user_id,
    u.user_name
```

SQL 编写规范：

| 规范                    | 说明                              |
| ----------------------- | --------------------------------- |
| 禁止生产使用 `SELECT *` | 必须显式列出字段                  |
| 分区表必须带分区条件    | 防止全表扫描                      |
| Join 前裁剪字段         | 减少 Shuffle 数据量               |
| 字段别名明确            | 输出字段使用标准命名              |
| SQL 文件顶部写注释      | 说明用途和目标表                  |
| 关键参数使用占位符      | 例如 `${bizDate}`                 |
| 聚合字段口径明确        | count、sum、distinct 需有业务说明 |
| 复杂 SQL 使用 CTE       | 提高可读性                        |

不推荐写法：

```sql
SELECT *
FROM dwd.dwd_order_detail o
LEFT JOIN dim.dim_user_current u
    ON o.user_id = u.user_id;
```

推荐写法：

```sql
WITH order_base AS (
    SELECT
        order_id,
        user_id,
        order_amount,
        dt
    FROM dwd.dwd_order_detail
    WHERE dt = '${bizDate}'
),
user_base AS (
    SELECT
        user_id,
        user_level
    FROM dim.dim_user_current
)
SELECT
    o.order_id,
    o.user_id,
    u.user_level,
    o.order_amount,
    o.dt
FROM order_base o
LEFT JOIN user_base u
    ON o.user_id = u.user_id;
```

### 日志编写规范

日志应服务于排查问题和审计追踪。Spark 任务日志必须包含任务名称、业务日期、批次号、阶段、数据量、目标表或路径、异常堆栈等关键信息。

推荐日志格式：

```text
时间 级别 线程 Logger - message，jobName=xxx，bizDate=xxx，batchId=xxx
```

日志示例：

```java
log.info("开始读取 Hive 表，jobName={}，bizDate={}，tableName={}",
        args.getJobName(), args.getBizDate(), "dwd.dwd_user_detail");

log.info("数据转换完成，jobName={}，bizDate={}，inputCount={}，outputCount={}",
        args.getJobName(), args.getBizDate(), inputCount, outputCount);

log.error("数据写出失败，jobName={}，bizDate={}，targetTable={}",
        args.getJobName(), args.getBizDate(), targetTable, e);
```

日志级别使用建议：

| 级别    | 使用场景                                     |
| ------- | -------------------------------------------- |
| `INFO`  | 任务启动、参数、数据量、阶段完成、写出成功   |
| `WARN`  | 非阻断异常、数据波动、空批次、维度少量缺失   |
| `ERROR` | 任务失败、写出失败、质量阻断、外部依赖不可用 |
| `DEBUG` | 本地调试，不建议生产大量开启                 |

日志规范建议：

| 建议                        | 说明                            |
| --------------------------- | ------------------------------- |
| 不使用 `System.out.println` | 统一使用 SLF4J                  |
| 异常必须打印堆栈            | `log.error("xxx", e)`           |
| 日志字段结构化              | 便于检索                        |
| 敏感信息脱敏                | 密码、Token、手机号等不输出明文 |
| 关键阶段记录数据量          | 输入、输出、异常、重复数据量    |
| 不打印大 SQL 全文           | 可打印 SQL 文件路径和参数摘要   |

### 异常处理规范

异常处理应分类明确、日志充分、保留原始异常、失败可感知。不要吞掉异常，也不要用过大的 `catch Exception` 后只打印日志不抛出。

推荐异常分类：

| 异常                     | 说明         |
| ------------------------ | ------------ |
| `JobParamException`      | 参数错误     |
| `DataReadException`      | 数据读取失败 |
| `DataTransformException` | 数据转换失败 |
| `DataWriteException`     | 数据写出失败 |
| `DataQualityException`   | 数据质量失败 |
| `SparkJobException`      | 任务通用异常 |

异常处理示例：

```java
try {
    writer.write(resultDf, writeOptions);
} catch (Exception e) {
    log.error("写出用户汇总结果失败，jobName={}，bizDate={}，targetTable={}",
            args.getJobName(), args.getBizDate(), targetTable, e);
    throw new DataWriteException("写出用户汇总结果失败", e);
}
```

异常处理规范建议：

| 建议                 | 说明                              |
| -------------------- | --------------------------------- |
| 按异常类型分类       | 参数、读取、转换、写出、质量分开  |
| 保留原始异常         | 包装异常时传入 cause              |
| 异常消息包含上下文   | jobName、bizDate、tableName、path |
| 核心失败必须抛出     | 让调度平台感知失败                |
| 不吞异常             | 不允许只打印日志后继续执行        |
| 数据质量异常单独处理 | 便于区分系统问题和数据问题        |

### Git 分支规范

Git 分支规范用于保证多人协作、版本发布、缺陷修复和回滚可控。Spark 项目通常可以采用 `main`、`develop`、`feature`、`release`、`hotfix` 分支模型。

推荐分支：

| 分支        | 说明             |
| ----------- | ---------------- |
| `main`      | 生产稳定分支     |
| `develop`   | 日常开发集成分支 |
| `feature/*` | 新功能开发分支   |
| `bugfix/*`  | 普通缺陷修复分支 |
| `hotfix/*`  | 生产紧急修复分支 |
| `release/*` | 发布准备分支     |

分支命名示例：

```text
feature/user-summary
feature/kafka-stream-clean
bugfix/hive-partition-null
hotfix/prod-jdbc-write-error
release/1.1.0
```

Git 分支规范建议：

| 建议                    | 说明                            |
| ----------------------- | ------------------------------- |
| main 只保留生产稳定代码 | 不直接在 main 开发              |
| 新功能从 develop 拉分支 | 完成后合并回 develop            |
| 生产紧急修复使用 hotfix | 修复后同步 main 和 develop      |
| 发布前创建 release 分支 | 用于测试和版本冻结              |
| 删除已合并临时分支      | 保持仓库清晰                    |
| 合并前必须 Code Review  | SQL、资源参数、写出逻辑重点审查 |

### 提交信息规范

提交信息应清晰表达本次修改的类型、范围和内容。推荐使用类似 Conventional Commits 的格式。

提交格式：

```text
<type>(<scope>): <subject>
```

常见 type：

| type       | 说明                 |
| ---------- | -------------------- |
| `feat`     | 新功能               |
| `fix`      | Bug 修复             |
| `docs`     | 文档修改             |
| `style`    | 格式调整，不影响逻辑 |
| `refactor` | 重构                 |
| `perf`     | 性能优化             |
| `test`     | 测试相关             |
| `build`    | 构建和依赖           |
| `ci`       | CI 配置              |
| `chore`    | 其他杂项             |

提交示例：

```text
feat(user-summary): add daily user order summary job

fix(hive): handle null dt before partition write

perf(join): broadcast user dimension table

docs(spark): update kafka offset troubleshooting guide

build(maven): add shade plugin configuration
```

提交信息建议：

| 建议                           | 说明                          |
| ------------------------------ | ----------------------------- |
| 一次提交只做一类修改           | 避免大杂烩提交                |
| scope 使用模块名               | 如 hive、kafka、jdbc、quality |
| subject 使用英文或团队统一语言 | 保持一致                      |
| 修复类提交关联问题单           | 便于追踪                      |
| SQL 口径变更写清楚             | 提交信息或 MR 描述中说明影响  |
| 资源参数调整写原因             | 便于后续回溯性能变化          |

### 版本发布规范

版本发布规范用于保证 Spark 任务发布可追踪、可回滚、可审计。每次发布都应有明确版本号、发布说明、构建产物、配置版本、上线时间和回滚方案。

推荐版本格式：

```text
主版本.次版本.修订号
```

示例：

```text
1.0.0
1.1.0
1.1.1
2.0.0
```

版本号含义：

| 类型   | 说明                               |
| ------ | ---------------------------------- |
| 主版本 | 不兼容变更，例如输出表结构重大调整 |
| 次版本 | 新增功能，例如新增 Job 或数据源    |
| 修订号 | Bug 修复、SQL 小调整、性能优化     |

发布包命名：

```text
spark-job-1.0.0.jar
spark-job-1.1.0.jar
spark-job-1.1.1.jar
```

发布流程：

```text
完成开发
        ↓
通过单元测试和集成测试
        ↓
合并 release 分支
        ↓
构建版本化 Jar
        ↓
部署测试环境验证
        ↓
生成发布说明
        ↓
部署生产环境
        ↓
观察首批任务结果
        ↓
记录发布审计
```

发布说明模板：

```text
版本号：1.1.0
发布时间：2026-05-11
发布内容：
- 新增 user-summary 离线汇总任务
- 优化订单宽表 Join 策略
- 修复 Hive 动态分区字段为空问题

影响范围：
- ads.ads_user_summary
- dws.dws_order_wide

上线检查：
- 单元测试通过
- 测试环境验证通过
- 生产权限确认
- 回滚版本：1.0.0

回滚方式：
- 执行 bin/rollback.sh 1.0.0
- 使用相同 bizDate 重跑失败分区
```

版本发布建议：

| 建议                     | 说明                           |
| ------------------------ | ------------------------------ |
| 每次生产发布必须有版本号 | 不使用无版本 Jar               |
| SQL 变更也要升级版本     | SQL 是任务逻辑的一部分         |
| 配置变更记录版本         | 生产配置修改要可追踪           |
| 发布前完成检查清单       | 依赖、权限、表结构、调度、告警 |
| 保留历史发布包           | 支持快速回滚                   |
| 发布后观察首批任务       | 检查日志、数据量、指标和告警   |

## 示例项目

本章节提供一个完整的 Spark Java 示例项目，用于串联批处理、Spark SQL、Hive 读写、Kafka 流处理、JDBC 读写、数据清洗、指标计算和任务提交。示例以“用户订单汇总”和“用户事件实时清洗”为业务场景，展示一个 Spark 项目从代码到提交运行的基本开发方式。

示例项目推荐结构如下：

```text
spark-example
├── pom.xml
├── scripts
│   ├── submit-user-order-summary.sh
│   └── submit-user-event-stream.sh
└── src
    ├── main
    │   ├── java
    │   │   └── io/github/atengk/spark/example
    │   │       ├── Application.java
    │   │       ├── batch
    │   │       │   └── UserOrderSummaryBatchJob.java
    │   │       ├── config
    │   │       │   └── RuntimeArgs.java
    │   │       ├── jdbc
    │   │       │   └── JdbcUserReader.java
    │   │       ├── kafka
    │   │       │   └── UserEventStreamJob.java
    │   │       ├── quality
    │   │       │   └── ExampleQualityChecker.java
    │   │       └── sql
    │   │           └── SqlTemplateUtil.java
    │   └── resources
    │       └── sql
    │           └── ads_user_order_summary.sql
    └── test
```

### 批处理示例

批处理示例以用户订单汇总为场景，按业务日期读取用户明细表和订单明细表，完成订单数、支付金额、平均订单金额等指标计算，并写入 ADS 汇总表。该示例适合每日离线任务、补数任务和定时报表任务。

文件位置：`src/main/java/io/github/atengk/spark/example/batch/UserOrderSummaryBatchJob.java`

下面的批处理任务完成 Hive 数据读取、业务聚合、质量校验和 Hive 写出。

```java
package io.github.atengk.spark.example.batch;

import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.spark.example.config.RuntimeArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;

import static org.apache.spark.sql.functions.avg;
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.count;
import static org.apache.spark.sql.functions.countDistinct;
import static org.apache.spark.sql.functions.current_timestamp;
import static org.apache.spark.sql.functions.sum;

/**
 * 用户订单汇总批处理任务。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserOrderSummaryBatchJob {

    /**
     * 执行用户订单汇总批处理任务。
     *
     * @param spark SparkSession
     * @param args 运行参数
     */
    public void run(SparkSession spark, RuntimeArgs args) {
        TimeInterval timer = new TimeInterval();
        String bizDate = args.getBizDate();

        if (StrUtil.isBlank(bizDate)) {
            throw new IllegalArgumentException("业务日期不能为空");
        }

        log.info("开始执行用户订单汇总批处理任务，bizDate={}", bizDate);

        Dataset<Row> userDf = spark.table("dwd.dwd_user_detail")
                .filter(col("dt").equalTo(bizDate))
                .select(
                        col("user_id"),
                        col("user_name"),
                        col("user_level"),
                        col("dt")
                );

        Dataset<Row> orderDf = spark.table("dwd.dwd_order_detail")
                .filter(col("dt").equalTo(bizDate))
                .filter(col("pay_status").equalTo("PAID"))
                .select(
                        col("order_id"),
                        col("user_id"),
                        col("order_amount"),
                        col("dt")
                );

        long userCount = userDf.count();
        long orderCount = orderDf.count();
        log.info("源数据读取完成，bizDate={}，userCount={}，orderCount={}", bizDate, userCount, orderCount);

        Dataset<Row> orderSummaryDf = orderDf
                .groupBy("user_id", "dt")
                .agg(
                        count("order_id").alias("order_count"),
                        countDistinct("order_id").alias("distinct_order_count"),
                        sum("order_amount").alias("pay_amount"),
                        avg("order_amount").alias("avg_order_amount")
                );

        Dataset<Row> resultDf = userDf
                .join(orderSummaryDf, new String[]{"user_id", "dt"}, "left")
                .na().fill(0, new String[]{"order_count", "distinct_order_count", "pay_amount", "avg_order_amount"})
                .withColumn("etl_time", current_timestamp())
                .select(
                        col("user_id"),
                        col("user_name"),
                        col("user_level"),
                        col("order_count"),
                        col("distinct_order_count"),
                        col("pay_amount"),
                        col("avg_order_amount"),
                        col("etl_time"),
                        col("dt")
                );

        long outputCount = resultDf.count();
        log.info("用户订单汇总计算完成，bizDate={}，outputCount={}", bizDate, outputCount);

        if (outputCount == 0) {
            throw new IllegalStateException("用户订单汇总结果为空，禁止写入目标表");
        }

        resultDf.write()
                .mode(SaveMode.Overwrite)
                .insertInto("ads.ads_user_order_summary");

        log.info("用户订单汇总批处理任务完成，bizDate={}，耗时={} ms", bizDate, timer.interval());
    }
}
```

批处理任务开发要点：

| 要点                        | 说明                   |
| --------------------------- | ---------------------- |
| 使用 `bizDate` 控制处理范围 | 避免扫描全表           |
| 读取后记录输入量            | 便于排查上游数据缺失   |
| 写出前校验输出量            | 防止空数据覆盖         |
| 输出字段顺序显式指定        | 避免 Hive 写入字段错位 |
| 使用 `Overwrite` 支持重跑   | 按分区重算更安全       |

### Spark SQL 示例

Spark SQL 示例适合复杂指标、宽表构建和业务口径较重的计算。建议将 SQL 文件放在 `src/main/resources/sql` 目录中，通过模板参数替换方式传入业务日期、库名和表名。

文件位置：`src/main/resources/sql/ads_user_order_summary.sql`

下面的 SQL 用于按用户汇总每日订单指标。

```sql
-- 用户订单日汇总指标
SELECT
    u.user_id,
    u.user_name,
    u.user_level,
    COUNT(o.order_id) AS order_count,
    COUNT(DISTINCT o.order_id) AS distinct_order_count,
    COALESCE(SUM(o.order_amount), 0) AS pay_amount,
    COALESCE(AVG(o.order_amount), 0) AS avg_order_amount,
    CURRENT_TIMESTAMP() AS etl_time,
    '${bizDate}' AS dt
FROM dwd.dwd_user_detail u
LEFT JOIN dwd.dwd_order_detail o
    ON u.user_id = o.user_id
   AND o.dt = '${bizDate}'
   AND o.pay_status = 'PAID'
WHERE u.dt = '${bizDate}'
GROUP BY
    u.user_id,
    u.user_name,
    u.user_level
```

文件位置：`src/main/java/io/github/atengk/spark/example/sql/SqlTemplateUtil.java`

下面的工具类用于读取 classpath 中的 SQL 文件，并替换 `${param}` 形式的参数。

```java
package io.github.atengk.spark.example.sql;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SQL 模板工具类。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class SqlTemplateUtil {

    private static final String PLACEHOLDER_PATTERN = "\\$\\{[a-zA-Z0-9_]+}";

    private SqlTemplateUtil() {
    }

    /**
     * 读取并渲染 SQL 模板。
     *
     * @param sqlPath SQL 文件路径
     * @param params 参数
     * @return 渲染后的 SQL
     */
    public static String loadAndRender(String sqlPath, Map<String, String> params) {
        if (StrUtil.isBlank(sqlPath)) {
            throw new IllegalArgumentException("SQL 文件路径不能为空");
        }
        if (MapUtil.isEmpty(params)) {
            throw new IllegalArgumentException("SQL 参数不能为空");
        }

        String sql = ResourceUtil.readUtf8Str(sqlPath);
        if (StrUtil.isBlank(sql)) {
            throw new IllegalArgumentException(StrUtil.format("SQL 文件内容为空：{}", sqlPath));
        }

        String renderedSql = sql;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            renderedSql = StrUtil.replace(renderedSql, StrUtil.format("${{{}}}", entry.getKey()), entry.getValue());
        }

        Set<String> notReplaced = ReUtil.findAll(PLACEHOLDER_PATTERN, renderedSql, 0)
                .stream()
                .collect(Collectors.toSet());

        if (!notReplaced.isEmpty()) {
            throw new IllegalArgumentException(StrUtil.format("SQL 存在未替换参数：{}", notReplaced));
        }

        log.info("SQL 模板渲染完成，sqlPath={}，paramSize={}", sqlPath, params.size());
        return renderedSql;
    }
}
```

Spark SQL 执行示例：

```java
Map<String, String> params = new HashMap<>();
params.put("bizDate", args.getBizDate());

String sql = SqlTemplateUtil.loadAndRender("sql/ads_user_order_summary.sql", params);

Dataset<Row> resultDf = spark.sql(sql);
resultDf.explain("formatted");
```

Spark SQL 示例建议：

| 建议                     | 说明                              |
| ------------------------ | --------------------------------- |
| 复杂 SQL 外置            | 不在 Java 中拼接大段 SQL          |
| 参数替换后检查占位符     | 防止运行时 SQL 错误               |
| SQL 执行前可打印文件路径 | 不建议生产打印超长 SQL 全文       |
| 上线前查看执行计划       | 检查分区裁剪、Join 策略和 Shuffle |
| 输出字段显式声明         | 保证目标表写入稳定                |

### Hive 读写示例

Hive 读写是 Spark 离线数仓任务的核心能力。示例中使用 `enableHiveSupport()` 开启 Hive 支持，通过 `spark.table()` 读取 Hive 表，通过 `insertInto()` 写入 Hive 表。

文件位置：`src/main/java/io/github/atengk/spark/example/Application.java`

下面的入口类根据 `jobName` 路由到不同示例任务，并统一创建 SparkSession。

```java
package io.github.atengk.spark.example;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.spark.example.batch.UserOrderSummaryBatchJob;
import io.github.atengk.spark.example.config.RuntimeArgs;
import io.github.atengk.spark.example.kafka.UserEventStreamJob;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.SparkSession;

/**
 * Spark 示例项目启动入口。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class Application {

    /**
     * 应用主入口。
     *
     * @param args 启动参数
     * @throws Exception 任务异常
     */
    public static void main(String[] args) throws Exception {
        RuntimeArgs runtimeArgs = RuntimeArgs.parse(args);

        SparkSession.Builder builder = SparkSession.builder()
                .appName(StrUtil.format("{}-{}", runtimeArgs.getJobName(), runtimeArgs.getBizDate()))
                .config("spark.sql.adaptive.enabled", "true");

        if ("local".equals(runtimeArgs.getEnv())) {
            builder.master("local[*]");
            builder.config("spark.sql.shuffle.partitions", "4");
        } else {
            builder.enableHiveSupport();
        }

        SparkSession spark = builder.getOrCreate();

        try {
            log.info("Spark 示例任务启动，jobName={}，env={}，bizDate={}",
                    runtimeArgs.getJobName(), runtimeArgs.getEnv(), runtimeArgs.getBizDate());

            if ("user-order-summary".equals(runtimeArgs.getJobName())) {
                new UserOrderSummaryBatchJob().run(spark, runtimeArgs);
                return;
            }

            if ("user-event-stream".equals(runtimeArgs.getJobName())) {
                new UserEventStreamJob().run(spark, runtimeArgs);
                return;
            }

            throw new IllegalArgumentException(StrUtil.format("不支持的任务名称：{}", runtimeArgs.getJobName()));
        } finally {
            spark.stop();
            log.info("SparkSession 已关闭");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/spark/example/config/RuntimeArgs.java`

下面的参数类用于解析 `spark-submit` 传入的业务参数。

```java
package io.github.atengk.spark.example.config;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Spark 示例运行参数。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Slf4j
public class RuntimeArgs implements Serializable {

    private static final long serialVersionUID = 1L;

    private String jobName;
    private String env;
    private String bizDate;
    private String batchId;
    private String inputPath;
    private String outputPath;
    private String writeMode;

    /**
     * 解析启动参数。
     *
     * @param args 启动参数
     * @return 运行参数
     */
    public static RuntimeArgs parse(String[] args) {
        Map<String, String> argMap = parseToMap(args);

        RuntimeArgs runtimeArgs = new RuntimeArgs();
        runtimeArgs.setJobName(required(argMap, "jobName"));
        runtimeArgs.setEnv(StrUtil.blankToDefault(MapUtil.getStr(argMap, "env"), "local"));
        runtimeArgs.setBizDate(required(argMap, "bizDate"));
        runtimeArgs.setBatchId(StrUtil.blankToDefault(MapUtil.getStr(argMap, "batchId"), runtimeArgs.getBizDate()));
        runtimeArgs.setInputPath(MapUtil.getStr(argMap, "inputPath"));
        runtimeArgs.setOutputPath(MapUtil.getStr(argMap, "outputPath"));
        runtimeArgs.setWriteMode(StrUtil.blankToDefault(MapUtil.getStr(argMap, "writeMode"), "append"));

        log.info("运行参数解析完成，jobName={}，env={}，bizDate={}，batchId={}，writeMode={}",
                runtimeArgs.getJobName(),
                runtimeArgs.getEnv(),
                runtimeArgs.getBizDate(),
                runtimeArgs.getBatchId(),
                runtimeArgs.getWriteMode());

        return runtimeArgs;
    }

    /**
     * 解析为 Map。
     *
     * @param args 启动参数
     * @return 参数 Map
     */
    private static Map<String, String> parseToMap(String[] args) {
        Map<String, String> argMap = new HashMap<>();
        if (args == null) {
            return argMap;
        }

        for (int i = 0; i < args.length; i++) {
            String key = args[i];
            if (StrUtil.startWith(key, "--") && i + 1 < args.length) {
                String value = args[i + 1];
                if (!StrUtil.startWith(value, "--")) {
                    argMap.put(StrUtil.removePrefix(key, "--"), value);
                    i++;
                }
            }
        }

        return argMap;
    }

    /**
     * 获取必填参数。
     *
     * @param argMap 参数 Map
     * @param key 参数名
     * @return 参数值
     */
    private static String required(Map<String, String> argMap, String key) {
        String value = MapUtil.getStr(argMap, key);
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException(StrUtil.format("缺少必填参数：{}", key));
        }
        return value;
    }
}
```

Hive 读写关键点：

| 操作           | 示例                                                    |
| -------------- | ------------------------------------------------------- |
| 开启 Hive 支持 | `enableHiveSupport()`                                   |
| 读取表         | `spark.table("dwd.dwd_user_detail")`                    |
| SQL 查询       | `spark.sql("SELECT ...")`                               |
| 写入表         | `dataset.write().insertInto("ads.table")`               |
| 覆盖分区       | `INSERT OVERWRITE TABLE ... PARTITION`                  |
| 动态分区       | 设置 `spark.sql.sources.partitionOverwriteMode=dynamic` |

### Kafka 流处理示例

Kafka 流处理示例以用户行为事件为场景，从 Kafka Topic 读取 JSON 消息，解析后进行基础清洗，并写入 HDFS Parquet 路径。生产环境中建议保留 `topic`、`partition`、`offset` 字段，便于排查重复消费和数据丢失。

文件位置：`src/main/java/io/github/atengk/spark/example/kafka/UserEventStreamJob.java`

下面的流处理任务读取 Kafka、解析 JSON、过滤非法数据，并写入 HDFS。

```java
package io.github.atengk.spark.example.kafka;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.spark.example.config.RuntimeArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.OutputMode;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.current_timestamp;
import static org.apache.spark.sql.functions.from_json;
import static org.apache.spark.sql.functions.to_timestamp;

/**
 * 用户事件 Kafka 流处理任务。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class UserEventStreamJob {

    /**
     * 执行用户事件流处理任务。
     *
     * @param spark SparkSession
     * @param args 运行参数
     * @throws Exception 流任务异常
     */
    public void run(SparkSession spark, RuntimeArgs args) throws Exception {
        String outputPath = StrUtil.blankToDefault(
                args.getOutputPath(),
                "hdfs:///warehouse/dwd/user_event_stream"
        );

        String checkpointPath = StrUtil.format("hdfs:///checkpoint/{}/user-event-stream", args.getEnv());

        log.info("开始启动用户事件流处理任务，outputPath={}，checkpointPath={}", outputPath, checkpointPath);

        StructType schema = new StructType()
                .add("event_id", DataTypes.StringType)
                .add("user_id", DataTypes.StringType)
                .add("event_type", DataTypes.StringType)
                .add("event_time", DataTypes.StringType)
                .add("source", DataTypes.StringType);

        Dataset<Row> kafkaDf = spark.readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", "kafka-host:9092")
                .option("subscribe", "ods_user_event")
                .option("startingOffsets", "latest")
                .option("failOnDataLoss", "false")
                .option("maxOffsetsPerTrigger", "100000")
                .load();

        Dataset<Row> eventDf = kafkaDf
                .selectExpr(
                        "CAST(key AS STRING) AS message_key",
                        "CAST(value AS STRING) AS message_value",
                        "topic",
                        "partition",
                        "offset",
                        "timestamp AS kafka_time"
                )
                .withColumn("json_data", from_json(col("message_value"), schema))
                .select(
                        col("json_data.event_id").alias("event_id"),
                        col("json_data.user_id").alias("user_id"),
                        col("json_data.event_type").alias("event_type"),
                        to_timestamp(col("json_data.event_time"), "yyyy-MM-dd HH:mm:ss").alias("event_time"),
                        col("json_data.source").alias("source"),
                        col("topic"),
                        col("partition"),
                        col("offset"),
                        col("kafka_time")
                )
                .filter(col("event_id").isNotNull())
                .filter(col("user_id").isNotNull())
                .filter(col("event_time").isNotNull())
                .withColumn("etl_time", current_timestamp());

        StreamingQuery query = eventDf.writeStream()
                .format("parquet")
                .option("path", outputPath)
                .option("checkpointLocation", checkpointPath)
                .outputMode(OutputMode.Append())
                .start();

        log.info("用户事件流处理任务启动完成，queryId={}", query.id());
        query.awaitTermination();
    }
}
```

Kafka 流处理建议：

| 建议                 | 说明                           |
| -------------------- | ------------------------------ |
| 必须配置 Checkpoint  | 用于 Offset 和状态恢复         |
| 保留 Kafka 元信息    | `topic`、`partition`、`offset` |
| 解析失败数据单独处理 | 避免脏消息影响主链路           |
| 控制每批消费量       | 使用 `maxOffsetsPerTrigger`    |
| 下游写入保持幂等     | 重启或重试可能重复处理         |
| 监控处理延迟         | 关注输入速率和处理速率         |

### JDBC 读写示例

JDBC 示例用于读取 MySQL 用户维表，并将 Spark 计算结果写入 MySQL 报表表。JDBC 适合小维表读取、报表结果写入、任务审计写入，不适合大规模明细数据长期写入业务库。

文件位置：`src/main/java/io/github/atengk/spark/example/jdbc/JdbcUserReader.java`

下面的组件用于从 MySQL 读取用户维表。

```java
package io.github.atengk.spark.example.jdbc;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

/**
 * JDBC 用户维表读取器。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class JdbcUserReader {

    /**
     * 读取 MySQL 用户维表。
     *
     * @param spark SparkSession
     * @return 用户维表
     */
    public Dataset<Row> readUserDim(SparkSession spark) {
        String username = System.getenv("MYSQL_USERNAME");
        String password = System.getenv("MYSQL_PASSWORD");

        if (StrUtil.hasBlank(username, password)) {
            throw new IllegalArgumentException("MySQL 用户名或密码环境变量不能为空");
        }

        log.info("开始读取 MySQL 用户维表");

        Dataset<Row> userDf = spark.read()
                .format("jdbc")
                .option("url", "jdbc:mysql://mysql-host:3306/demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai")
                .option("driver", "com.mysql.cj.jdbc.Driver")
                .option("dbtable", "(SELECT user_id, user_name, user_level FROM dim_user WHERE status = 'ACTIVE') t")
                .option("user", username)
                .option("password", password)
                .option("fetchsize", "5000")
                .load();

        log.info("MySQL 用户维表读取逻辑构建完成");
        return userDf;
    }
}
```

JDBC 写入示例：

```java
String username = System.getenv("MYSQL_USERNAME");
String password = System.getenv("MYSQL_PASSWORD");

resultDf.coalesce(4)
        .write()
        .mode(SaveMode.Append)
        .format("jdbc")
        .option("url", "jdbc:mysql://mysql-host:3306/report?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai")
        .option("driver", "com.mysql.cj.jdbc.Driver")
        .option("dbtable", "ads_user_order_summary")
        .option("user", username)
        .option("password", password)
        .option("batchsize", "5000")
        .save();
```

JDBC 读写建议：

| 建议                 | 说明                   |
| -------------------- | ---------------------- |
| 密码使用环境变量     | 不写入代码和日志       |
| 大表读取使用分区读取 | 配置 `partitionColumn` |
| 写入前控制分区数     | 避免过多数据库连接     |
| 写入使用 batchsize   | 提高批量写入效率       |
| 重要写入使用临时表   | 校验后再合并正式表     |
| 不直接压业务核心库   | 优先读从库或报表库     |

### 数据清洗示例

数据清洗示例以用户事件数据为场景，处理空值、非法枚举、时间转换、字段标准化和脏数据拆分。清洗逻辑建议封装为独立组件，输入 DataFrame，输出清洗后的 DataFrame 或合法/非法数据集。

文件位置：`src/main/java/io/github/atengk/spark/example/quality/ExampleQualityChecker.java`

下面的工具类用于完成基础数据质量校验。

```java
package io.github.atengk.spark.example.quality;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import static org.apache.spark.sql.functions.col;

/**
 * 示例数据质量校验器。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
public class ExampleQualityChecker {

    private ExampleQualityChecker() {
    }

    /**
     * 校验数据集不能为空。
     *
     * @param dataset 数据集
     * @param datasetName 数据集名称
     */
    public static void checkNotEmpty(Dataset<Row> dataset, String datasetName) {
        if (StrUtil.isBlank(datasetName)) {
            throw new IllegalArgumentException("数据集名称不能为空");
        }

        long count = dataset.count();
        log.info("数据质量校验：{} 数据量为 {}", datasetName, count);

        if (count == 0) {
            throw new IllegalStateException(StrUtil.format("{} 数据为空", datasetName));
        }
    }

    /**
     * 校验字段不能为空。
     *
     * @param dataset 数据集
     * @param datasetName 数据集名称
     * @param columnName 字段名
     */
    public static void checkColumnNotNull(Dataset<Row> dataset, String datasetName, String columnName) {
        long nullCount = dataset.filter(col(columnName).isNull()).count();
        log.info("数据质量校验：{} 字段 {} 空值数量为 {}", datasetName, columnName, nullCount);

        if (nullCount > 0) {
            throw new IllegalStateException(StrUtil.format("{} 字段 {} 存在空值，数量：{}", datasetName, columnName, nullCount));
        }
    }
}
```

数据清洗示例：

```java
Dataset<Row> checkedDf = sourceDf
        .withColumn("event_time", to_timestamp(col("event_time"), "yyyy-MM-dd HH:mm:ss"))
        .withColumn("event_type", upper(trim(col("event_type"))))
        .withColumn(
                "error_reason",
                when(col("event_id").isNull(), lit("事件ID为空"))
                        .when(col("user_id").isNull(), lit("用户ID为空"))
                        .when(col("event_time").isNull(), lit("事件时间非法"))
                        .when(not(col("event_type").isin("CLICK", "VIEW", "PAY")), lit("事件类型非法"))
                        .otherwise(lit(null))
        );

Dataset<Row> validDf = checkedDf
        .filter(col("error_reason").isNull())
        .drop("error_reason");

Dataset<Row> dirtyDf = checkedDf
        .filter(col("error_reason").isNotNull());

dirtyDf.write()
        .mode(SaveMode.Append)
        .partitionBy("dt")
        .parquet("hdfs:///error/prod/user_event_dirty");
```

数据清洗建议：

| 建议                 | 说明                             |
| -------------------- | -------------------------------- |
| 清洗规则显式表达     | 空值、枚举、时间、数值范围       |
| 脏数据单独输出       | 不直接丢弃                       |
| 增加错误原因字段     | 便于排查上游问题                 |
| 主键字段严格校验     | 主键为空通常应阻断               |
| 非核心字段可默认填充 | 例如渠道为空填充 `UNKNOWN`       |
| 清洗后记录数据量     | 输入量、合法量、脏数据量都要记录 |

### 指标计算示例

指标计算示例以订单日指标为场景，计算订单数、支付用户数、支付金额、平均订单金额等指标。指标计算必须明确统计范围、统计粒度、过滤条件和输出表主键。

DataFrame 指标计算示例：

```java
Dataset<Row> orderMetricDf = orderDf
        .filter(col("pay_status").equalTo("PAID"))
        .groupBy("dt", "channel")
        .agg(
                count("order_id").alias("order_count"),
                countDistinct("user_id").alias("pay_user_count"),
                sum("order_amount").alias("pay_amount"),
                avg("order_amount").alias("avg_order_amount")
        )
        .withColumn("etl_time", current_timestamp())
        .select(
                col("dt"),
                col("channel"),
                col("order_count"),
                col("pay_user_count"),
                col("pay_amount"),
                col("avg_order_amount"),
                col("etl_time")
        );
```

SQL 指标计算示例：

```sql
SELECT
    dt,
    channel,
    COUNT(order_id) AS order_count,
    COUNT(DISTINCT user_id) AS pay_user_count,
    SUM(order_amount) AS pay_amount,
    AVG(order_amount) AS avg_order_amount,
    CURRENT_TIMESTAMP() AS etl_time
FROM dwd.dwd_order_detail
WHERE dt = '${bizDate}'
  AND pay_status = 'PAID'
GROUP BY
    dt,
    channel
```

指标写入示例：

```java
long metricCount = orderMetricDf.count();
log.info("订单日指标计算完成，bizDate={}，metricCount={}", args.getBizDate(), metricCount);

if (metricCount == 0) {
    throw new IllegalStateException("订单日指标结果为空，禁止写入");
}

orderMetricDf.write()
        .mode(SaveMode.Overwrite)
        .insertInto("ads.ads_order_daily_metric");
```

指标计算建议：

| 建议                  | 说明                     |
| --------------------- | ------------------------ |
| 指标口径必须稳定      | 过滤条件和统计粒度要明确 |
| 输出表主键明确        | 例如 `dt + channel`      |
| 金额使用 Decimal      | 避免精度问题             |
| 结果写入前校验非空    | 防止空指标覆盖           |
| 与历史数据做波动对比  | 发现异常暴增或暴跌       |
| 指标 SQL 纳入版本管理 | SQL 变更视为业务逻辑变更 |

### 任务提交示例

任务提交示例包括离线批处理任务和 Kafka 流处理任务。生产环境建议通过 Shell 脚本封装 `spark-submit`，由调度平台调用脚本并传入业务日期、批次号和运行环境。

文件位置：`scripts/submit-user-order-summary.sh`

下面的脚本用于提交用户订单汇总离线任务。

```bash
#!/usr/bin/env bash

# scripts/submit-user-order-summary.sh
# 提交用户订单汇总批处理任务

set -e

APP_HOME=/opt/app/spark-example
APP_JAR=${APP_HOME}/lib/spark-example.jar
BIZ_DATE=$1
BATCH_ID=${2:-"$(date -d "${BIZ_DATE}" +%Y%m%d)_user_order_summary"}

if [ -z "${BIZ_DATE}" ]; then
  echo "缺少业务日期参数，例如：./submit-user-order-summary.sh 2026-05-11"
  exit 1
fi

spark-submit \
  --class io.github.atengk.spark.example.Application \
  --master yarn \
  --deploy-mode cluster \
  --name "spark-user-order-summary-${BIZ_DATE}" \
  --queue root.prod.etl \
  --driver-memory 2g \
  --executor-memory 4g \
  --executor-cores 2 \
  --num-executors 8 \
  --conf spark.sql.shuffle.partitions=400 \
  --conf spark.sql.adaptive.enabled=true \
  --conf spark.sql.adaptive.skewJoin.enabled=true \
  --conf spark.eventLog.enabled=true \
  --conf spark.eventLog.dir=hdfs:///spark-history \
  --files ${APP_HOME}/conf/application-prod.yml,${APP_HOME}/conf/hive-site.xml,${APP_HOME}/conf/logback.xml \
  "${APP_JAR}" \
  --jobName user-order-summary \
  --env prod \
  --bizDate "${BIZ_DATE}" \
  --batchId "${BATCH_ID}" \
  --writeMode overwrite
```

文件位置：`scripts/submit-user-event-stream.sh`

下面的脚本用于提交用户事件 Kafka 流处理任务。

```bash
#!/usr/bin/env bash

# scripts/submit-user-event-stream.sh
# 提交用户事件 Kafka 流处理任务

set -e

APP_HOME=/opt/app/spark-example
APP_JAR=${APP_HOME}/lib/spark-example.jar

spark-submit \
  --class io.github.atengk.spark.example.Application \
  --master yarn \
  --deploy-mode cluster \
  --name spark-user-event-stream \
  --queue root.prod.realtime \
  --driver-memory 2g \
  --executor-memory 4g \
  --executor-cores 2 \
  --num-executors 4 \
  --conf spark.sql.shuffle.partitions=200 \
  --conf spark.sql.streaming.stopGracefullyOnShutdown=true \
  --conf spark.eventLog.enabled=true \
  --conf spark.eventLog.dir=hdfs:///spark-history \
  --files ${APP_HOME}/conf/application-prod.yml,${APP_HOME}/conf/hive-site.xml,${APP_HOME}/conf/logback.xml \
  "${APP_JAR}" \
  --jobName user-event-stream \
  --env prod \
  --bizDate "$(date +%F)" \
  --outputPath hdfs:///warehouse/dwd/user_event_stream
```

本地模式提交示例：

```bash
spark-submit \
  --class io.github.atengk.spark.example.Application \
  --master local[*] \
  --name spark-example-local \
  --conf spark.sql.shuffle.partitions=4 \
  target/spark-example.jar \
  --jobName user-order-summary \
  --env local \
  --bizDate 2026-05-11 \
  --writeMode overwrite
```

任务提交建议：

| 建议                         | 说明                                           |
| ---------------------------- | ---------------------------------------------- |
| Jar 前是 Spark 参数          | 例如 `--master`、`--conf`、`--executor-memory` |
| Jar 后是业务参数             | 例如 `--jobName`、`--bizDate`                  |
| 生产使用 Cluster 模式        | 避免提交客户端影响任务                         |
| 脚本中校验必填参数           | 缺少业务日期直接失败                           |
| 资源参数按任务类型配置       | 离线任务和流任务资源模型不同                   |
| Application 名称包含业务信息 | 便于在 YARN 和 History Server 中检索           |

## 项目验收

本章节用于说明 Spark Java 项目的验收标准。项目验收不应只检查任务是否能运行，还需要从功能、数据、性能、稳定性、安全、运维和文档多个维度确认项目是否满足上线要求。

### 功能验收

功能验收用于确认 Spark 项目是否实现了预期业务能力，包括数据读取、清洗、转换、计算、写出、调度和参数化运行等功能。

功能验收清单如下：

| 验收项     | 验收标准                                               |
| ---------- | ------------------------------------------------------ |
| 任务入口   | 可以通过统一入口类启动指定 Job                         |
| 参数解析   | 支持 `jobName`、`env`、`bizDate`、`batchId` 等基础参数 |
| 数据读取   | 能正确读取 Hive、HDFS、Kafka、JDBC 等指定数据源        |
| 数据清洗   | 能处理空值、重复、异常格式、非法枚举等问题             |
| 数据转换   | 能完成字段转换、Join、聚合、窗口计算等逻辑             |
| 数据写出   | 能正确写入 Hive、HDFS、JDBC、Kafka 等目标端            |
| 多环境运行 | 支持 local、test、prod 环境切换                        |
| 重跑支持   | 指定相同业务日期可以安全重跑                           |
| 补数支持   | 支持指定历史日期或日期范围补数                         |
| 异常处理   | 失败时能抛出明确异常并记录日志                         |

功能验收建议如下：

| 建议                 | 说明                                 |
| -------------------- | ------------------------------------ |
| 使用固定样例数据验收 | 保证结果可重复验证                   |
| 覆盖正常和异常流程   | 不只验证成功路径                     |
| 按 Job 分别验收      | 每个任务都要有独立验收记录           |
| 验证参数优先级       | 命令行参数、配置文件、默认值关系明确 |
| 验证重跑幂等性       | 重复执行不应产生重复结果             |

功能验收记录示例：

```text
任务名称：user-order-summary
业务日期：2026-05-11
运行环境：test
验收内容：
- Hive 源表读取成功
- 用户订单指标计算正确
- ADS 目标表写入成功
- 指定分区重跑结果一致
- 空结果禁止写入逻辑生效

验收结论：通过
```

### 数据验收

数据验收用于确认 Spark 任务产出的数据在数量、字段、口径、质量和分区方面符合预期。数据验收是数仓类 Spark 项目最关键的验收环节。

数据验收清单如下：

| 验收项       | 验收标准                             |
| ------------ | ------------------------------------ |
| 输入数据量   | 与上游表或文件分区数据量一致         |
| 输出数据量   | 与业务预期、历史均值或测试用例一致   |
| 字段完整性   | 目标表字段齐全，字段名和类型正确     |
| 主键唯一性   | 目标表业务主键不重复                 |
| 必填字段     | 主键、分区字段、核心指标字段不能为空 |
| 指标准确性   | 订单数、金额、用户数等核心指标正确   |
| 分区正确性   | 数据写入指定 `dt` 分区               |
| 脏数据处理   | 脏数据输出到错误目录或错误表         |
| 数据波动     | 当前批次与历史数据波动在合理范围内   |
| 上下游一致性 | 上游输入、下游输出和审计记录可对齐   |

常用数据验收 SQL 如下：

```sql
-- 校验目标分区数据量
SELECT COUNT(1) AS output_count
FROM ads.ads_user_order_summary
WHERE dt = '2026-05-11';

-- 校验主键唯一性
SELECT
    user_id,
    dt,
    COUNT(1) AS duplicate_count
FROM ads.ads_user_order_summary
WHERE dt = '2026-05-11'
GROUP BY user_id, dt
HAVING COUNT(1) > 1;

-- 校验核心字段空值
SELECT
    SUM(CASE WHEN user_id IS NULL THEN 1 ELSE 0 END) AS user_id_null_count,
    SUM(CASE WHEN dt IS NULL THEN 1 ELSE 0 END) AS dt_null_count,
    SUM(CASE WHEN order_count IS NULL THEN 1 ELSE 0 END) AS order_count_null_count
FROM ads.ads_user_order_summary
WHERE dt = '2026-05-11';

-- 校验金额指标
SELECT
    SUM(order_count) AS order_count,
    SUM(pay_amount) AS pay_amount
FROM ads.ads_user_order_summary
WHERE dt = '2026-05-11';
```

数据验收建议如下：

| 建议                           | 说明                             |
| ------------------------------ | -------------------------------- |
| 验收核心指标而不是只看任务成功 | 任务成功不代表数据正确           |
| 输出字段逐项检查               | 字段名、类型、顺序、注释都要确认 |
| 分区验收必须执行               | 确认没有写入错误分区             |
| 关键指标与旧链路对比           | 迁移类项目必须对比历史任务结果   |
| 异常数据要可追溯               | 脏数据表应包含错误原因和原始字段 |
| 验收结果写入记录               | 便于上线审批和问题追踪           |

### 性能验收

性能验收用于确认 Spark 任务在预期数据规模下可以在 SLA 时间内完成，并且资源使用合理，不会对集群造成明显冲击。

性能验收清单如下：

| 验收项         | 验收标准                          |
| -------------- | --------------------------------- |
| 总耗时         | 不超过任务 SLA                    |
| Stage 耗时     | 无异常长尾 Stage                  |
| Task 分布      | Task 耗时分布相对均匀             |
| Shuffle 数据量 | Shuffle Read / Write 在可接受范围 |
| Executor 内存  | 无频繁 OOM 和严重 GC              |
| Driver 内存    | 无 Driver OOM 和超大结果拉取      |
| 输出文件数     | 文件数量和大小合理                |
| 队列资源       | 不长期等待资源                    |
| 并行度         | Executor 核心利用率合理           |
| 历史对比       | 相比旧版本无明显性能退化          |

性能验收记录示例：

```text
任务名称：user-order-summary
业务日期：2026-05-11
输入数据量：1,200,000,000
输出数据量：8,000,000
Executor 数量：8
Executor Memory：4g
Executor Cores：2
Shuffle Partitions：400
总耗时：18 分 20 秒
最大 Stage 耗时：6 分 45 秒
Shuffle Write：320 GB
Shuffle Read：318 GB
输出文件数：96
验收结论：通过
```

性能验收建议如下：

| 建议                          | 说明                           |
| ----------------------------- | ------------------------------ |
| 使用接近生产的数据量          | 小样例无法验证性能             |
| 保留 Spark UI 或 History 记录 | 便于后续对比                   |
| 输出文件数必须检查            | 防止小文件问题上线后暴露       |
| 关注长尾 Task                 | 长尾通常意味着数据倾斜         |
| 资源配置写入验收记录          | 后续调优需要对比基线           |
| 性能异常先分析再加资源        | 不盲目提高 Executor 数量和内存 |

### 稳定性验收

稳定性验收用于确认 Spark 任务在重复执行、失败重试、资源波动、上游延迟和外部系统短暂异常时仍能保持可控状态。

稳定性验收清单如下：

| 验收项       | 验收标准                           |
| ------------ | ---------------------------------- |
| 重复执行     | 同一 `bizDate` 重跑结果一致        |
| 失败恢复     | 失败后可以安全重跑                 |
| 空数据保护   | 关键任务空数据不覆盖目标分区       |
| 部分写入处理 | 写出失败后有修复方案               |
| 流任务恢复   | 使用原 Checkpoint 可以恢复         |
| 外部依赖异常 | Kafka、JDBC、Hive 异常时日志明确   |
| 资源波动     | 队列资源不足时任务不会产生错误数据 |
| 幂等性       | 重试不会导致重复写入               |
| 长时间运行   | 流任务能长期稳定运行               |
| 告警触发     | 失败和延迟能触发告警               |

稳定性验收建议如下：

| 建议                  | 说明                               |
| --------------------- | ---------------------------------- |
| 至少连续运行多个批次  | 不只验收单次运行                   |
| 模拟失败重跑          | 验证恢复流程                       |
| 流任务验证 Checkpoint | 停止后重启确认 Offset 连续         |
| 验证下游幂等          | JDBC、Kafka、foreachBatch 尤其重要 |
| 验证异常日志          | 失败原因应可定位                   |
| 验证调度重试策略      | 非幂等任务不应盲目自动重试         |

### 安全验收

安全验收用于确认 Spark 项目的认证、授权、脱敏、密钥管理和审计追踪符合安全要求。

安全验收清单如下：

| 验收项        | 验收标准                              |
| ------------- | ------------------------------------- |
| Kerberos 认证 | 任务账号可以正常认证                  |
| HDFS 权限     | 只具备必要路径读写权限                |
| Hive 权限     | 只具备必要库表权限                    |
| Kafka 权限    | Topic 读写权限按需授权                |
| JDBC 权限     | 使用专用账号，权限最小化              |
| 敏感配置      | 密码、Token、Keytab 不进入 Git 和 Jar |
| 日志脱敏      | 日志不输出密码、密钥和敏感字段明文    |
| 数据脱敏      | 测试环境和对外数据已脱敏              |
| 审计记录      | 任务访问、写入和异常有审计            |
| 权限回收      | 下线任务有权限回收方案                |

安全验收命令示例：

```bash
# 检查 Kerberos 票据
klist

# 验证 HDFS 读权限
hdfs dfs -ls /warehouse/prod/dwd/user_event

# 验证 HDFS 写权限
hdfs dfs -touchz /warehouse/prod/ads/user_summary/_permission_test
hdfs dfs -rm /warehouse/prod/ads/user_summary/_permission_test

# 检查发布包中是否包含敏感文件
jar tf spark-job.jar | grep -E "keytab|secret|password|jaas"
```

安全验收建议如下：

| 建议               | 说明                             |
| ------------------ | -------------------------------- |
| 权限按最小范围授权 | 不使用全库、全路径权限           |
| 敏感配置必须外置   | 使用环境变量、密钥系统或安全挂载 |
| 发布包扫描敏感文件 | 防止 Keytab、密码文件误打包      |
| 日志抽样检查       | 确认无敏感信息明文               |
| 任务账号专用化     | 生产任务不使用个人账号           |
| 审计记录长期保存   | 支持安全追踪和合规检查           |

### 运维验收

运维验收用于确认任务上线后可以被调度、监控、告警、重跑、补数、暂停、下线和回滚。

运维验收清单如下：

| 验收项   | 验收标准                         |
| -------- | -------------------------------- |
| 启动脚本 | 可以通过脚本提交任务             |
| 停止脚本 | 可以停止指定 Application         |
| 调度配置 | 调度平台配置正确                 |
| 任务依赖 | 上下游依赖关系正确               |
| 补数脚本 | 支持指定日期范围补数             |
| 重跑流程 | 指定分区可安全重跑               |
| 回滚方案 | 可以切换到上一版本               |
| 监控配置 | 状态、耗时、数据量可监控         |
| 告警配置 | 失败和延迟可告警                 |
| 日志查询 | 可以通过 Application ID 查询日志 |

运维验收建议如下：

| 建议                 | 说明                                |
| -------------------- | ----------------------------------- |
| 脚本必须可执行       | 检查权限和环境变量                  |
| 调度参数必须可模板化 | `bizDate`、`batchId` 由调度平台传入 |
| 告警接收人明确       | 失败后有人处理                      |
| 补数流程演练         | 上线前至少执行一次测试补数          |
| 回滚流程演练         | 确认历史版本可用                    |
| 运维手册同步发布     | 便于值班人员处理问题                |

### 文档验收

文档验收用于确认项目相关文档完整、准确、可执行。Spark 项目文档不仅包括开发说明，还应包括部署、调度、运维、数据口径、故障排查和发布记录。

文档验收清单如下：

| 文档     | 验收标准                             |
| -------- | ------------------------------------ |
| 项目说明 | 项目背景、目标、边界清晰             |
| 开发文档 | 工程结构、依赖、代码设计完整         |
| 数据说明 | 输入表、输出表、字段、指标口径明确   |
| 部署文档 | 部署目录、配置文件、脚本说明完整     |
| 调度文档 | 调度时间、依赖、参数、补数方式明确   |
| 运维文档 | 重跑、补数、暂停、下线、回滚流程完整 |
| 排查文档 | 常见异常和处理方式完整               |
| 安全文档 | 权限、密钥、脱敏、审计说明完整       |
| 发布记录 | 每次版本变更有记录                   |
| 验收记录 | 功能、数据、性能、安全验收有结论     |

文档验收建议如下：

| 建议                 | 说明                       |
| -------------------- | -------------------------- |
| 文档与代码同版本管理 | 文档变更跟随代码提交       |
| SQL 口径必须说明     | 指标计算不能只看 SQL       |
| 运维步骤可复制执行   | 命令、路径、参数要完整     |
| 表结构和字段要同步   | 输出表字段变化必须更新文档 |
| 发布记录保留历史     | 支持问题追踪和回滚         |
| 文档验收作为上线门禁 | 文档不完整不建议发布       |

## 附录

本章节整理 Spark Java 项目中常用配置、提交参数、Shell 脚本、SQL 模板、排查命令、目录结构和依赖版本，便于开发、测试、部署和运维时快速查阅。

### Spark 常用配置

Spark SQL 常用配置：

| 配置                                            | 示例            | 说明                      |
| ----------------------------------------------- | --------------- | ------------------------- |
| `spark.sql.shuffle.partitions`                  | `400`           | SQL Shuffle 分区数        |
| `spark.sql.adaptive.enabled`                    | `true`          | 开启 AQE 自适应执行       |
| `spark.sql.adaptive.coalescePartitions.enabled` | `true`          | AQE 自动合并 Shuffle 分区 |
| `spark.sql.adaptive.skewJoin.enabled`           | `true`          | AQE 处理部分 Join 倾斜    |
| `spark.sql.autoBroadcastJoinThreshold`          | `10485760`      | 自动广播 Join 阈值        |
| `spark.sql.files.maxRecordsPerFile`             | `1000000`       | 控制单文件最大记录数      |
| `spark.sql.sources.partitionOverwriteMode`      | `dynamic`       | 动态分区覆盖模式          |
| `spark.sql.session.timeZone`                    | `Asia/Shanghai` | SQL 会话时区              |

资源常用配置：

| 配置                            | 示例   | 说明               |
| ------------------------------- | ------ | ------------------ |
| `--driver-memory`               | `2g`   | Driver 堆内存      |
| `--executor-memory`             | `4g`   | Executor 堆内存    |
| `--executor-cores`              | `2`    | 每个 Executor 核数 |
| `--num-executors`               | `8`    | Executor 数量      |
| `spark.executor.memoryOverhead` | `1g`   | Executor 堆外内存  |
| `spark.driver.memoryOverhead`   | `512m` | Driver 堆外内存    |
| `spark.default.parallelism`     | `400`  | RDD 默认并行度     |

Structured Streaming 常用配置：

| 配置                                           | 示例                     | 说明                     |
| ---------------------------------------------- | ------------------------ | ------------------------ |
| `checkpointLocation`                           | `hdfs:///checkpoint/job` | 流任务恢复目录           |
| `maxOffsetsPerTrigger`                         | `100000`                 | Kafka 每批最大读取量     |
| `startingOffsets`                              | `latest`                 | Kafka 起始 Offset        |
| `failOnDataLoss`                               | `false`                  | Kafka 数据缺失时是否失败 |
| `spark.sql.streaming.stopGracefullyOnShutdown` | `true`                   | JVM 关闭时优雅停止流查询 |

YARN 常用配置：

| 配置                        | 示例                    | 说明                  |
| --------------------------- | ----------------------- | --------------------- |
| `--queue`                   | `root.prod.etl`         | YARN 队列             |
| `spark.yarn.maxAppAttempts` | `1`                     | YARN 应用最大尝试次数 |
| `spark.eventLog.enabled`    | `true`                  | 开启事件日志          |
| `spark.eventLog.dir`        | `hdfs:///spark-history` | 事件日志目录          |

### spark-submit 参数说明

`spark-submit` 参数分为 Spark 参数和业务参数。应用 Jar 前面的参数由 Spark 解析，应用 Jar 后面的参数由业务程序解析。

基础格式：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  [Spark 参数] \
  spark-job.jar \
  [业务参数]
```

常用 Spark 参数：

| 参数                | 说明                                                    |
| ------------------- | ------------------------------------------------------- |
| `--class`           | 应用主类                                                |
| `--master`          | 运行模式，例如 `local[*]`、`yarn`、`spark://`、`k8s://` |
| `--deploy-mode`     | 部署模式，`client` 或 `cluster`                         |
| `--name`            | Spark 应用名称                                          |
| `--queue`           | YARN 队列                                               |
| `--driver-memory`   | Driver 内存                                             |
| `--executor-memory` | Executor 内存                                           |
| `--executor-cores`  | Executor 核数                                           |
| `--num-executors`   | Executor 数量                                           |
| `--conf`            | Spark 配置                                              |
| `--files`           | 分发配置文件                                            |
| `--jars`            | 分发外部 Jar                                            |
| `--packages`        | 自动解析 Maven 依赖                                     |
| `--principal`       | Kerberos Principal                                      |
| `--keytab`          | Kerberos Keytab                                         |

常用业务参数：

| 参数            | 说明           |
| --------------- | -------------- |
| `--jobName`     | 任务名称       |
| `--env`         | 运行环境       |
| `--bizDate`     | 业务日期       |
| `--batchId`     | 批次号         |
| `--sourceTable` | 源表           |
| `--targetTable` | 目标表         |
| `--inputPath`   | 输入路径       |
| `--outputPath`  | 输出路径       |
| `--writeMode`   | 写入模式       |
| `--allowEmpty`  | 是否允许空结果 |

生产提交模板：

```bash
spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --name "spark-${JOB_NAME}-${BIZ_DATE}" \
  --queue root.prod.etl \
  --driver-memory 2g \
  --executor-memory 4g \
  --executor-cores 2 \
  --num-executors 8 \
  --conf spark.sql.shuffle.partitions=400 \
  --conf spark.sql.adaptive.enabled=true \
  --conf spark.sql.adaptive.skewJoin.enabled=true \
  --conf spark.eventLog.enabled=true \
  --conf spark.eventLog.dir=hdfs:///spark-history \
  --files /opt/app/spark-job/conf/application-prod.yml,/opt/app/spark-job/conf/hive-site.xml,/opt/app/spark-job/conf/logback.xml \
  /opt/app/spark-job/lib/spark-job.jar \
  --jobName "${JOB_NAME}" \
  --env prod \
  --bizDate "${BIZ_DATE}" \
  --batchId "${BATCH_ID}" \
  --writeMode overwrite \
  --allowEmpty false
```

### 常用 Shell 脚本

公共环境脚本：

```bash
#!/usr/bin/env bash

# bin/env.sh
# Spark 项目公共环境变量

export JAVA_HOME=/opt/module/jdk
export SPARK_HOME=/opt/module/spark
export HADOOP_CONF_DIR=/opt/module/hadoop/etc/hadoop
export HIVE_CONF_DIR=/opt/module/hive/conf

export APP_HOME=/opt/app/spark-job
export APP_JAR=${APP_HOME}/lib/spark-job.jar
export APP_CONF=${APP_HOME}/conf/application-prod.yml
export LOG_CONF=${APP_HOME}/conf/logback.xml
export HIVE_CONF=${APP_HOME}/conf/hive-site.xml

export YARN_QUEUE=root.prod.etl

export PATH=${JAVA_HOME}/bin:${SPARK_HOME}/bin:${PATH}
```

离线任务启动脚本：

```bash
#!/usr/bin/env bash

# bin/start-job.sh
# 通用 Spark 离线任务启动脚本

set -e

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
source "${SCRIPT_DIR}/env.sh"

JOB_NAME=$1
BIZ_DATE=$2
BATCH_ID=${3:-"$(date -d "${BIZ_DATE}" +%Y%m%d)_${JOB_NAME}"}

if [ -z "${JOB_NAME}" ] || [ -z "${BIZ_DATE}" ]; then
  echo "用法：./start-job.sh user-summary 2026-05-11"
  exit 1
fi

spark-submit \
  --class io.github.atengk.spark.Application \
  --master yarn \
  --deploy-mode cluster \
  --name "spark-${JOB_NAME}-${BIZ_DATE}" \
  --queue "${YARN_QUEUE}" \
  --driver-memory 2g \
  --executor-memory 4g \
  --executor-cores 2 \
  --num-executors 8 \
  --conf spark.sql.shuffle.partitions=400 \
  --conf spark.sql.adaptive.enabled=true \
  --files "${APP_CONF},${LOG_CONF},${HIVE_CONF}" \
  "${APP_JAR}" \
  --jobName "${JOB_NAME}" \
  --env prod \
  --bizDate "${BIZ_DATE}" \
  --batchId "${BATCH_ID}" \
  --writeMode overwrite \
  --allowEmpty false
```

补数脚本：

```bash
#!/usr/bin/env bash

# bin/backfill-job.sh
# 按日期范围补跑 Spark 任务

set -e

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)

JOB_NAME=$1
START_DATE=$2
END_DATE=$3

if [ -z "${JOB_NAME}" ] || [ -z "${START_DATE}" ] || [ -z "${END_DATE}" ]; then
  echo "用法：./backfill-job.sh user-summary 2026-05-01 2026-05-11"
  exit 1
fi

CURRENT_DATE="${START_DATE}"

while [ "${CURRENT_DATE}" != "$(date -d "${END_DATE} +1 day" +%F)" ]; do
  BATCH_ID="$(date -d "${CURRENT_DATE}" +%Y%m%d)_backfill_${JOB_NAME}"

  echo "开始补数，jobName=${JOB_NAME}, bizDate=${CURRENT_DATE}, batchId=${BATCH_ID}"

  "${SCRIPT_DIR}/start-job.sh" "${JOB_NAME}" "${CURRENT_DATE}" "${BATCH_ID}"

  CURRENT_DATE=$(date -d "${CURRENT_DATE} +1 day" +%F)
done

echo "补数完成，jobName=${JOB_NAME}, startDate=${START_DATE}, endDate=${END_DATE}"
```

停止任务脚本：

```bash
#!/usr/bin/env bash

# bin/stop-job.sh
# 停止指定 YARN Application

set -e

APP_ID=$1

if [ -z "${APP_ID}" ]; then
  echo "用法：./stop-job.sh application_1710000000000_0001"
  exit 1
fi

yarn application -status "${APP_ID}" || true

read -r -p "确认停止应用 ${APP_ID}？输入 yes 继续：" CONFIRM

if [ "${CONFIRM}" != "yes" ]; then
  echo "已取消停止"
  exit 0
fi

yarn application -kill "${APP_ID}"
echo "停止命令已提交：${APP_ID}"
```

### 常用 SQL 模板

Hive 分区检查模板：

```sql
SHOW PARTITIONS ${tableName} PARTITION(dt='${bizDate}');
```

目标分区数据量检查模板：

```sql
SELECT
    COUNT(1) AS total_count
FROM ${tableName}
WHERE dt = '${bizDate}';
```

主键重复检查模板：

```sql
SELECT
    ${uniqueColumns},
    COUNT(1) AS duplicate_count
FROM ${tableName}
WHERE dt = '${bizDate}'
GROUP BY ${uniqueColumns}
HAVING COUNT(1) > 1;
```

必填字段空值检查模板：

```sql
SELECT
    SUM(CASE WHEN ${columnName} IS NULL THEN 1 ELSE 0 END) AS null_count
FROM ${tableName}
WHERE dt = '${bizDate}';
```

动态分区写入模板：

```sql
INSERT OVERWRITE TABLE ${targetTable} PARTITION (dt)
SELECT
    ${selectColumns},
    dt
FROM ${sourceTable}
WHERE dt = '${bizDate}';
```

按日期补数查询模板：

```sql
SELECT
    *
FROM ${sourceTable}
WHERE dt >= '${startDate}'
  AND dt <= '${endDate}';
```

指标汇总模板：

```sql
SELECT
    dt,
    ${groupColumns},
    COUNT(1) AS total_count,
    COUNT(DISTINCT ${distinctColumn}) AS distinct_count,
    SUM(${amountColumn}) AS amount_sum,
    CURRENT_TIMESTAMP() AS etl_time
FROM ${sourceTable}
WHERE dt = '${bizDate}'
GROUP BY
    dt,
    ${groupColumns};
```

### 常用排查命令

YARN 应用排查：

```bash
# 查看运行中的应用
yarn application -list

# 查看指定应用状态
yarn application -status application_1710000000000_0001

# 查看指定应用日志
yarn logs -applicationId application_1710000000000_0001

# 杀掉指定应用
yarn application -kill application_1710000000000_0001
```

HDFS 文件排查：

```bash
# 查看目录
hdfs dfs -ls /warehouse/prod/ads/user_summary

# 查看目录统计
hdfs dfs -count /warehouse/prod/ads/user_summary/dt=2026-05-11

# 查看文件大小
hdfs dfs -du -h /warehouse/prod/ads/user_summary/dt=2026-05-11

# 测试写权限
hdfs dfs -touchz /warehouse/prod/ads/user_summary/_permission_test
hdfs dfs -rm /warehouse/prod/ads/user_summary/_permission_test
```

Hive 排查：

```sql
-- 查看库
SHOW DATABASES;

-- 查看表
SHOW TABLES IN ads;

-- 查看表结构
DESCRIBE FORMATTED ads.ads_user_summary;

-- 查看分区
SHOW PARTITIONS ads.ads_user_summary;

-- 查看指定分区数据量
SELECT COUNT(1)
FROM ads.ads_user_summary
WHERE dt = '2026-05-11';
```

Kafka 排查：

```bash
# 查看 Topic
kafka-topics.sh \
  --bootstrap-server kafka-host:9092 \
  --list

# 查看 Topic 详情
kafka-topics.sh \
  --bootstrap-server kafka-host:9092 \
  --describe \
  --topic ods_user_event

# 查看 Consumer Group
kafka-consumer-groups.sh \
  --bootstrap-server kafka-host:9092 \
  --list

# 查看消费进度
kafka-consumer-groups.sh \
  --bootstrap-server kafka-host:9092 \
  --describe \
  --group spark-user-event
```

Maven 依赖排查：

```bash
# 查看完整依赖树
mvn dependency:tree

# 检查 Spark 依赖
mvn dependency:tree -Dincludes=org.apache.spark

# 检查 Jackson 依赖
mvn dependency:tree -Dincludes=com.fasterxml.jackson.core

# 检查 Guava 依赖
mvn dependency:tree -Dincludes=com.google.guava

# 检查日志依赖
mvn dependency:tree | grep -E "slf4j|log4j|logback"
```

Jar 内容排查：

```bash
# 查看主类是否存在
jar tf spark-job.jar | grep "Application.class"

# 查看是否包含 MySQL 驱动
jar tf spark-job.jar | grep "com/mysql/cj/jdbc/Driver.class"

# 检查是否误打入 Spark 类
jar tf spark-job.jar | grep "org/apache/spark" | head
```

日志排查：

```bash
# 搜索异常
yarn logs -applicationId application_1710000000000_0001 | grep -i "exception"

# 搜索根因
yarn logs -applicationId application_1710000000000_0001 | grep -i "caused by"

# 搜索 ERROR
yarn logs -applicationId application_1710000000000_0001 | grep "ERROR"

# 搜索 OOM
yarn logs -applicationId application_1710000000000_0001 | grep -E "OutOfMemory|GC overhead|Container killed"
```

### 推荐目录结构

单模块项目推荐结构：

```text
spark-job
├── pom.xml
├── README.md
├── docs
│   ├── deployment.md
│   ├── operation.md
│   └── troubleshooting.md
├── scripts
│   ├── build.sh
│   ├── start-job.sh
│   ├── stop-job.sh
│   ├── backfill-job.sh
│   └── rollback.sh
├── src
│   ├── main
│   │   ├── java
│   │   │   └── io/github/atengk/spark
│   │   │       ├── Application.java
│   │   │       ├── audit
│   │   │       ├── config
│   │   │       ├── exception
│   │   │       ├── job
│   │   │       ├── monitor
│   │   │       ├── quality
│   │   │       ├── reader
│   │   │       ├── service
│   │   │       ├── transformer
│   │   │       ├── util
│   │   │       └── writer
│   │   └── resources
│   │       ├── application.yml
│   │       ├── logback.xml
│   │       └── sql
│   │           ├── dwd
│   │           ├── dws
│   │           └── ads
│   └── test
│       ├── java
│       └── resources
│           └── data
└── target
```

多模块项目推荐结构：

```text
spark-platform
├── pom.xml
├── spark-common
│   ├── src/main/java
│   └── src/main/resources
├── spark-core-job
│   ├── src/main/java
│   └── src/main/resources
├── spark-stream-job
│   ├── src/main/java
│   └── src/main/resources
├── spark-quality
│   ├── src/main/java
│   └── src/main/resources
├── spark-submit
│   └── scripts
└── docs
```

目录结构建议如下：

| 目录            | 说明                       |
| --------------- | -------------------------- |
| `job`           | Job 任务入口               |
| `service`       | 业务流程编排               |
| `reader`        | 数据读取组件               |
| `transformer`   | 数据转换组件               |
| `writer`        | 数据写出组件               |
| `quality`       | 数据质量校验               |
| `monitor`       | 指标和监听器               |
| `audit`         | 审计记录                   |
| `config`        | 配置和参数                 |
| `exception`     | 自定义异常                 |
| `util`          | 通用工具                   |
| `resources/sql` | SQL 文件                   |
| `scripts`       | 构建、提交、补数、停止脚本 |
| `docs`          | 项目文档                   |

### 推荐依赖版本

依赖版本必须以实际 Spark 集群版本为准。Spark、Scala、Hadoop、Hive、Kafka Connector 等组件版本不匹配时，容易出现运行时依赖冲突。以下版本仅作为示例基线，生产项目应根据集群发行版统一调整。

推荐版本基线示例：

| 组件              | 示例版本    | 说明                           |
| ----------------- | ----------- | ------------------------------ |
| JDK               | 8 或 11     | 以 Spark 集群支持版本为准      |
| Spark             | 3.5.x       | 与集群 Spark 版本一致          |
| Scala             | 2.12.x      | 与 Spark 构建后缀一致          |
| Hadoop            | 3.3.x       | 与集群 Hadoop 版本一致         |
| Hive              | 2.3.x / 3.x | 以集群 Hive Metastore 版本为准 |
| Hutool            | 5.8.x       | Java 工具类                    |
| Lombok            | 1.18.x      | 编译期注解                     |
| JUnit             | 5.10.x      | 单元测试                       |
| AssertJ           | 3.25.x      | 测试断言                       |
| MySQL Driver      | 8.x         | MySQL JDBC 驱动                |
| PostgreSQL Driver | 42.x        | PostgreSQL JDBC 驱动           |
| HikariCP          | 5.x         | Driver 端少量 JDBC 操作        |

Maven 版本属性示例：

```xml
<properties>
    <!-- Java 编译版本需与集群 JDK 兼容 -->
    <maven.compiler.source>8</maven.compiler.source>
    <maven.compiler.target>8</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

    <!-- Spark / Hadoop / Scala 版本必须与集群保持一致 -->
    <spark.version>3.5.0</spark.version>
    <scala.binary.version>2.12</scala.binary.version>
    <hadoop.version>3.3.6</hadoop.version>

    <!-- 常用业务依赖版本 -->
    <hutool.version>5.8.26</hutool.version>
    <lombok.version>1.18.30</lombok.version>
    <mysql.version>8.3.0</mysql.version>
    <postgresql.version>42.7.1</postgresql.version>
    <junit.version>5.10.1</junit.version>
    <assertj.version>3.25.1</assertj.version>
</properties>
```

核心依赖示例：

```xml
<!-- Spark SQL：由集群提供，避免打入 Fat Jar -->
<dependency>
    <groupId>org.apache.spark</groupId>
    <artifactId>spark-sql_${scala.binary.version}</artifactId>
    <version>${spark.version}</version>
    <scope>provided</scope>
</dependency>

<!-- Spark Hive：用于 Spark 访问 Hive Metastore 和 Hive 表 -->
<dependency>
    <groupId>org.apache.spark</groupId>
    <artifactId>spark-hive_${scala.binary.version}</artifactId>
    <version>${spark.version}</version>
    <scope>provided</scope>
</dependency>

<!-- Spark Kafka Connector：用于 Structured Streaming 读取和写入 Kafka -->
<dependency>
    <groupId>org.apache.spark</groupId>
    <artifactId>spark-sql-kafka-0-10_${scala.binary.version}</artifactId>
    <version>${spark.version}</version>
</dependency>

<!-- Hutool：常用工具类库 -->
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>${hutool.version}</version>
</dependency>

<!-- Lombok：简化 Java 样板代码，编译期使用 -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>${lombok.version}</version>
    <scope>provided</scope>
</dependency>

<!-- MySQL JDBC 驱动：用于 Spark JDBC 读取和写入 MySQL -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>${mysql.version}</version>
</dependency>

<!-- JUnit 5：单元测试框架 -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>${junit.version}</version>
    <scope>test</scope>
</dependency>

<!-- AssertJ：测试断言工具 -->
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <version>${assertj.version}</version>
    <scope>test</scope>
</dependency>
```

依赖版本管理建议如下：

| 建议                         | 说明                                   |
| ---------------------------- | -------------------------------------- |
| Spark 版本以集群为准         | 应用依赖不能脱离集群版本               |
| Scala 后缀必须一致           | 例如统一使用 `_2.12`                   |
| Spark / Hadoop 使用 provided | 避免与集群运行时冲突                   |
| JDBC 驱动按需打包            | 集群没有驱动时打入 Fat Jar             |
| Connector 版本对齐 Spark     | Kafka、Hive、Iceberg、Delta 等都要匹配 |
| 发布前检查依赖树             | 使用 `mvn dependency:tree` 排查冲突    |
| 版本升级必须测试             | 依赖升级可能引发运行时问题             |