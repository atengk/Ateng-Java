# pgvector



## 项目概述

本项目基于 PostgreSQL 的 pgvector 扩展实现向量数据存储与相似度检索能力，主要用于语义搜索、知识库问答、RAG 检索增强、相似内容推荐等场景。项目目标是在保留 PostgreSQL 关系型数据库能力的基础上，引入向量检索能力，降低系统架构复杂度，并方便与现有 Spring Boot 业务系统集成。

### 背景与目标

随着大模型、Embedding 模型和智能检索场景的普及，传统基于关键词的检索方式已经难以满足语义理解类需求。例如，用户搜索“忘记密码怎么办”时，系统不仅需要匹配包含“密码”的数据，还需要识别“无法登录”“重置账号密码”“找回登录凭证”等语义相近的内容。

pgvector 是 PostgreSQL 的向量扩展，可以在 PostgreSQL 中直接存储向量数据，并基于向量距离进行相似度检索。通过 pgvector，业务系统可以将文本、图片、商品、文档片段、用户行为等数据转换为向量后存入数据库，然后通过 SQL 查询完成相似内容召回。

本项目的主要目标如下：

1. 在 PostgreSQL 中启用 pgvector 扩展，支持向量字段存储和查询。
2. 设计适合业务场景的向量表结构和索引结构。
3. 实现文本、文档片段或业务数据的向量化入库流程。
4. 支持基于向量距离的 Top K 相似度检索。
5. 支持向量检索与普通业务字段过滤条件组合查询。
6. 在 Spring Boot 项目中封装向量写入、相似度查询和混合检索接口。
7. 为知识库问答、语义搜索、推荐系统等 AI 应用提供基础检索能力。

### 使用场景

pgvector 适合用于业务数据已经存储在 PostgreSQL 中，同时又需要增加语义检索能力的项目。相比单独引入向量数据库，pgvector 可以减少数据同步、权限管理、事务一致性和运维部署方面的复杂度。

常见使用场景如下：

| 使用场景        | 场景说明                                         | 示例                                               |
| --------------- | ------------------------------------------------ | -------------------------------------------------- |
| 语义搜索        | 根据用户输入的自然语言查询语义相近的数据         | 搜索“如何修改密码”，匹配“忘记密码后如何重置”       |
| 知识库问答      | 将知识文档切分成片段并向量化，提问时召回相关片段 | 企业内部知识库、客服 FAQ、技术文档问答             |
| RAG 检索增强    | 在调用大模型前，先通过向量检索获取相关上下文     | 根据用户问题召回相关文档片段，再交给大模型生成回答 |
| 相似内容推荐    | 根据当前内容查找语义相似的数据                   | 相似文章、相似商品、相似问题、相似工单             |
| 内容去重        | 判断两段文本或两条业务数据是否高度相似           | 重复问题识别、重复文档检测、相似评论归并           |
| 多条件检索      | 在向量相似度检索基础上叠加业务过滤条件           | 查询某个分类、租户或状态下最相似的 Top 10 数据     |
| AI 应用原型验证 | 快速验证向量检索和语义召回能力                   | 小型知识库、内部智能助手、低成本 AI 检索服务       |

pgvector 更适合中小规模向量检索、业务强依赖 PostgreSQL、需要 SQL 条件过滤、需要快速集成的系统。如果数据规模非常大、并发要求极高、需要复杂分布式向量检索能力，则需要结合实际场景评估是否使用专用向量数据库。

### 技术选型

本项目采用 PostgreSQL + pgvector + Spring Boot 的技术组合。PostgreSQL 负责业务数据和向量数据的统一存储，pgvector 负责向量类型、距离计算和向量索引能力，Spring Boot 负责业务接口封装和应用集成。

| 技术类别   | 技术选型                    | 说明                                                         |
| ---------- | --------------------------- | ------------------------------------------------------------ |
| 数据库     | PostgreSQL                  | 作为主数据库，存储业务数据、向量字段、索引和查询结果         |
| 向量扩展   | pgvector                    | 提供向量字段类型、相似度计算和向量索引能力                   |
| 后端框架   | Spring Boot 3               | 提供向量写入、语义检索、混合查询等接口                       |
| 数据访问   | MyBatis-Plus / MyBatis XML  | 普通 CRUD 使用 MyBatis-Plus，复杂向量 SQL 建议使用 XML 或原生 SQL |
| 向量模型   | Embedding 模型              | 将文本、文档或业务内容转换为固定维度向量                     |
| 相似度算法 | Cosine / L2 / Inner Product | 根据模型类型和业务目标选择距离计算方式                       |
| 向量索引   | HNSW / IVFFlat              | 根据数据量、查询性能和召回率要求选择索引类型                 |
| 工具类     | Hutool                      | 用于字符串、集合、JSON、对象校验等通用处理                   |
| 接口协议   | REST API                    | 对外提供向量写入、向量检索、文本检索和混合检索接口           |
| 部署方式   | Docker / Linux 原生部署     | 支持本地开发、测试环境和生产环境部署                         |

推荐的技术组合如下：

| 项目阶段             | 推荐方案                               | 说明                                               |
| -------------------- | -------------------------------------- | -------------------------------------------------- |
| 原型验证阶段         | PostgreSQL + pgvector + 精确检索       | 优先验证数据结构、向量写入和检索结果是否正确       |
| 小中型数据阶段       | PostgreSQL + pgvector + HNSW           | 在保证较好召回效果的同时提升查询性能               |
| 批量导入阶段         | 先写入数据，再创建向量索引             | 避免导入过程中频繁维护索引导致写入变慢             |
| Spring Boot 集成阶段 | MyBatis-Plus + XML 原生 SQL            | 普通业务操作走 MyBatis-Plus，向量检索 SQL 单独维护 |
| 生产优化阶段         | 普通字段索引 + 向量索引 + 查询参数调优 | 同时优化业务过滤条件和向量相似度排序               |

在实际项目中，Embedding 模型输出的向量维度必须与数据库中 pgvector 字段定义的维度保持一致。例如，模型输出 1536 维向量时，数据库字段应定义为 `vector(1536)`。如果维度不一致，向量写入或查询时会出现异常。

对于文本语义检索场景，通常优先选择余弦距离；对于数值特征相似度计算，可以根据模型训练方式选择 L2 距离或内积。索引方面，项目初期可以先不创建近似索引，使用精确检索验证结果；当数据量增长后，再根据查询延迟和召回率要求选择 HNSW 或 IVFFlat。



## 环境准备

本节用于说明项目运行 pgvector 所需的基础环境，包括 PostgreSQL 版本、pgvector 扩展安装方式以及 Spring Boot 项目依赖配置。环境准备完成后，后续才能进行向量字段建模、数据写入和相似度检索。

### PostgreSQL 版本要求

pgvector 是 PostgreSQL 的扩展，因此项目需要先准备可安装扩展的 PostgreSQL 环境。根据 pgvector 当前官方说明，pgvector v0.8.2 支持 PostgreSQL 13 及以上版本，并且官方 Docker 镜像提供了 PostgreSQL 13 到 PostgreSQL 18 的 pgvector 镜像标签。([GitHub](https://github.com/pgvector/pgvector?utm_source=chatgpt.com))

项目建议版本如下：

| 环境          | 推荐版本             | 说明                                           |
| ------------- | -------------------- | ---------------------------------------------- |
| 本地开发环境  | PostgreSQL 16 或以上 | 便于使用较新的 PostgreSQL 能力和 pgvector 镜像 |
| 测试环境      | 与生产环境保持一致   | 避免扩展版本、索引行为和 SQL 执行计划不一致    |
| 生产环境      | PostgreSQL 16 或以上 | 推荐使用稳定版本，并统一扩展安装方式           |
| pgvector 扩展 | 0.8.x 或当前稳定版本 | 需要和 PostgreSQL 主版本匹配安装               |

使用前需要确认数据库是否支持安装扩展。可以通过以下 SQL 查看 PostgreSQL 版本：

```sql
-- 查看当前 PostgreSQL 版本
SELECT version();
```

如果使用 Docker，可以优先选择已经内置 pgvector 的镜像，减少手动编译和安装扩展的成本：

```bash
# 拉取带 pgvector 扩展的 PostgreSQL 镜像
docker pull pgvector/pgvector:pg16
```

如果使用 Linux 原生安装方式，需要保证服务器上存在 PostgreSQL 服务端开发包，否则编译 pgvector 时可能出现 `postgres.h` 找不到的问题。pgvector 官方安装说明中也明确提到，如果缺少 PostgreSQL 开发文件，需要安装对应版本的 `postgresql-server-dev-*` 包。([GitHub](https://github.com/pgvector/pgvector?utm_source=chatgpt.com))

### pgvector 扩展安装

pgvector 可以通过 Docker、源码编译、APT、Yum、Homebrew、PGXN 等方式安装。官方文档中列出的安装方式包括 Docker、Homebrew、PGXN、APT、Yum、pkg、APK 和 conda-forge。([GitHub](https://github.com/pgvector/pgvector?utm_source=chatgpt.com))

在项目开发和测试阶段，推荐使用 Docker 方式启动 PostgreSQL + pgvector，环境一致性更好，也更方便快速初始化。

以下命令用于启动一个带 pgvector 扩展的 PostgreSQL 容器。

```bash
# 创建并启动 PostgreSQL + pgvector 容器
docker run -d \
  --name postgres-pgvector \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=123456 \
  -e POSTGRES_DB=vector_db \
  -p 5432:5432 \
  -v pgvector-data:/var/lib/postgresql/data \
  pgvector/pgvector:pg16
```

参数说明：

| 参数                                        | 说明                                      |
| ------------------------------------------- | ----------------------------------------- |
| `--name postgres-pgvector`                  | 指定容器名称                              |
| `POSTGRES_USER`                             | 初始化数据库用户                          |
| `POSTGRES_PASSWORD`                         | 初始化数据库密码                          |
| `POSTGRES_DB`                               | 初始化数据库名称                          |
| `-p 5432:5432`                              | 将容器 PostgreSQL 端口映射到宿主机        |
| `-v pgvector-data:/var/lib/postgresql/data` | 使用 Docker Volume 持久化数据库数据       |
| `pgvector/pgvector:pg16`                    | 使用带 pgvector 扩展的 PostgreSQL 16 镜像 |

容器启动后，需要在目标数据库中启用 `vector` 扩展。pgvector 官方说明中要求在每个需要使用 pgvector 的数据库中执行一次 `CREATE EXTENSION vector;`。([GitHub](https://github.com/pgvector/pgvector?utm_source=chatgpt.com))

```sql
-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 检查扩展是否安装成功
SELECT extname, extversion
FROM pg_extension
WHERE extname = 'vector';
```

如果返回了 `vector` 扩展名称和版本号，说明扩展已经启用成功。

也可以通过创建一个简单向量表进行验证：

```sql
-- 创建测试表，用于验证 vector 类型是否可用
CREATE TABLE IF NOT EXISTS vector_test (
    id BIGSERIAL PRIMARY KEY,
    embedding vector(3)
);

-- 插入测试向量
INSERT INTO vector_test (embedding)
VALUES ('[1,2,3]'), ('[4,5,6]');

-- 使用 L2 距离查询最相似向量
SELECT id, embedding
FROM vector_test
ORDER BY embedding <-> '[1,2,3]'
LIMIT 1;
```

pgvector 支持通过 `<->` 计算 L2 距离，通过 `<#>` 计算内积，通过 `<=>` 计算余弦距离，通过 `<+>` 计算 L1 距离。([GitHub](https://github.com/pgvector/pgvector?utm_source=chatgpt.com))

### 项目依赖配置

Spring Boot 项目需要引入 PostgreSQL 驱动、MyBatis-Plus、Lombok、Hutool 等依赖。普通 CRUD 可以使用 MyBatis-Plus，向量相似度检索建议使用 XML Mapper 或原生 SQL，因为 pgvector 的距离操作符更适合直接写 SQL。

以下依赖适用于 Maven 项目。

```xml
<!-- PostgreSQL JDBC 驱动，用于连接 PostgreSQL 数据库 -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>

<!-- MyBatis-Plus，负责基础 CRUD 和 Mapper 能力 -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.7</version>
</dependency>

<!-- Hutool 工具类，处理字符串、集合、JSON、对象校验等通用逻辑 -->
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.32</version>
</dependency>

<!-- Lombok，减少实体类、DTO、VO 的样板代码 -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- Spring Boot Web，提供 REST API 能力 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- 参数校验，用于接口入参合法性校验 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

`application.yml` 中配置 PostgreSQL 数据源和 MyBatis-Plus Mapper 路径：

```yaml
spring:
  datasource:
    # PostgreSQL 数据库连接地址
    url: jdbc:postgresql://127.0.0.1:5432/vector_db
    # 数据库用户名
    username: postgres
    # 数据库密码
    password: 123456
    # PostgreSQL JDBC 驱动
    driver-class-name: org.postgresql.Driver

mybatis-plus:
  # Mapper XML 文件位置，用于编写 pgvector 原生 SQL
  mapper-locations: classpath*:/mapper/**/*.xml
  configuration:
    # SQL 日志输出，开发环境建议开启，生产环境按需关闭
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      # 主键策略，数据库使用 bigserial 或 identity 时可保持默认
      id-type: auto
```

如果项目中需要将 Java 的 `List<Double>` 或 `float[]` 写入 PostgreSQL 的 `vector` 字段，建议在 Service 层统一转换成 pgvector 可识别的字符串格式，例如：

```text
[0.0123,0.2345,0.3456]
```

后续在 Mapper XML 中可以通过 `#{embedding}::vector` 写入数据库，避免 JDBC 类型映射不明确的问题。

## 数据模型设计

本节用于说明 pgvector 项目中的核心数据结构设计。数据模型设计需要同时考虑向量维度、业务字段、检索条件、索引类型和后续扩展能力，避免只设计一个孤立的向量字段，导致后续无法结合业务条件进行过滤和排序。

### 向量字段设计

pgvector 提供多种向量相关类型，包括 `vector`、`halfvec`、`bit`、`sparsevec` 等。官方说明中，HNSW 索引支持的 `vector` 最大维度为 2000，`halfvec` 最大维度为 4000，`bit` 最大维度为 64000，`sparsevec` 最大支持 1000 个非零元素；IVFFlat 也支持 `vector`、`halfvec`、`bit` 等类型。([GitHub](https://github.com/pgvector/pgvector?utm_source=chatgpt.com))

普通文本语义检索场景建议优先使用 `vector(n)` 类型，其中 `n` 必须与 Embedding 模型输出维度一致。

示例：

```sql
-- 768 维向量，常见于部分开源文本向量模型
embedding vector(768)

-- 1024 维向量，常见于部分中文或多语言 Embedding 模型
embedding vector(1024)

-- 1536 维向量，常见于部分商业 Embedding 模型
embedding vector(1536)
```

向量字段设计建议如下：

| 设计项       | 建议                               | 说明                               |
| ------------ | ---------------------------------- | ---------------------------------- |
| 字段类型     | `vector(n)`                        | 最常用的稠密向量类型               |
| 维度定义     | 固定维度                           | 必须与 Embedding 模型输出维度一致  |
| 字段名称     | `embedding`                        | 表示向量数据，命名清晰             |
| 是否允许为空 | 初期可允许为空，生产建议按流程约束 | 原始数据可能先入库，后异步生成向量 |
| 写入格式     | `'[0.1,0.2,0.3]'`                  | pgvector 支持字符串形式写入向量    |
| 查询方式     | `ORDER BY embedding <=> ?`         | 文本语义检索通常使用余弦距离       |
| 维度变更     | 不建议直接修改原字段               | 模型升级时建议新增字段或新建版本表 |

如果项目后续可能切换 Embedding 模型，不建议只在原字段上直接覆盖向量。更稳妥的方式是增加模型版本字段，或者单独设计向量表来保存不同模型生成的向量。

### 业务表结构设计

业务表结构需要同时保存原始文本、业务归属信息、向量字段和状态字段。对于知识库、文档问答、RAG 场景，推荐以“文档片段”为最小检索单元，而不是直接对整篇文档生成一个向量。

推荐表结构如下：

```sql
-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 知识片段向量表，用于保存文本片段及其向量表示
CREATE TABLE IF NOT EXISTS knowledge_chunk (
    -- 主键 ID
    id BIGSERIAL PRIMARY KEY,

    -- 租户 ID，多租户系统中用于数据隔离
    tenant_id BIGINT NOT NULL,

    -- 知识库 ID，用于区分不同知识库
    knowledge_base_id BIGINT NOT NULL,

    -- 原始文档 ID，用于关联文档主表
    document_id BIGINT NOT NULL,

    -- 文档片段序号，用于保持原始文档中的顺序
    chunk_index INTEGER NOT NULL,

    -- 片段标题，可用于展示和辅助检索
    title VARCHAR(255),

    -- 片段正文内容
    content TEXT NOT NULL,

    -- Embedding 模型名称
    embedding_model VARCHAR(100) NOT NULL,

    -- Embedding 模型版本
    embedding_version VARCHAR(50) NOT NULL DEFAULT 'v1',

    -- 文本向量，维度需要与模型输出维度一致
    embedding vector(1536),

    -- 数据状态：1 启用，0 禁用
    status SMALLINT NOT NULL DEFAULT 1,

    -- 创建时间
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 更新时间
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

如果需要保存原始文档信息，可以单独设计文档主表：

```sql
-- 文档主表，用于保存文档级元数据
CREATE TABLE IF NOT EXISTS knowledge_document (
    -- 主键 ID
    id BIGSERIAL PRIMARY KEY,

    -- 租户 ID
    tenant_id BIGINT NOT NULL,

    -- 知识库 ID
    knowledge_base_id BIGINT NOT NULL,

    -- 文档标题
    title VARCHAR(255) NOT NULL,

    -- 文档来源，例如 upload、url、api、manual
    source_type VARCHAR(50) NOT NULL,

    -- 文档来源地址或外部业务 ID
    source_value VARCHAR(500),

    -- 文档处理状态：0 待处理，1 已完成，2 失败
    process_status SMALLINT NOT NULL DEFAULT 0,

    -- 创建时间
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 更新时间
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

常用业务字段说明如下：

| 字段                | 作用         | 设计说明                               |
| ------------------- | ------------ | -------------------------------------- |
| `tenant_id`         | 租户隔离     | 多租户系统必须保留，查询时作为过滤条件 |
| `knowledge_base_id` | 知识库隔离   | 支持多个知识库独立检索                 |
| `document_id`       | 关联原始文档 | 方便追溯向量片段来源                   |
| `chunk_index`       | 片段顺序     | 用于拼接上下文或展示原文位置           |
| `content`           | 片段文本     | 用于展示、重新生成向量、关键字检索     |
| `embedding_model`   | 模型名称     | 用于区分不同 Embedding 模型            |
| `embedding_version` | 模型版本     | 模型升级时便于灰度和回滚               |
| `embedding`         | 向量字段     | 相似度检索的核心字段                   |
| `status`            | 数据状态     | 过滤禁用、删除或未发布数据             |

对于生产环境，不建议只保留向量而不保留原始文本。向量适合检索，原始文本适合展示、重建索引、重新生成 Embedding 和问题排查。

### 索引类型选择

pgvector 默认执行精确最近邻搜索，召回结果准确，但在数据量较大时查询性能可能下降。官方说明中，pgvector 支持 HNSW 和 IVFFlat 两类近似索引，使用近似索引可以提升查询速度，但会在召回率和性能之间做权衡。([GitHub](https://github.com/pgvector/pgvector?utm_source=chatgpt.com))

索引选择建议如下：

| 索引类型         | 适用场景                           | 优点                             | 注意事项                                         |
| ---------------- | ---------------------------------- | -------------------------------- | ------------------------------------------------ |
| 无向量索引       | 原型验证、小数据量、结果准确性验证 | 结果准确，便于调试               | 数据量增大后查询会变慢                           |
| HNSW             | 中小型到较大规模的在线检索         | 查询性能和召回率表现较好         | 构建较慢，占用内存较高                           |
| IVFFlat          | 批量数据较稳定、希望控制索引大小   | 构建较快，内存占用较低           | 需要先有数据再建索引，召回率依赖 lists 和 probes |
| 普通 B-tree 索引 | 业务字段过滤                       | 适合租户、知识库、状态等过滤条件 | 不能替代向量索引                                 |
| 组合业务索引     | 多字段过滤                         | 提升带条件检索性能               | 需要结合实际 WHERE 条件设计                      |

常用业务字段索引如下：

```sql
-- 按租户、知识库、状态过滤数据
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_tenant_kb_status
ON knowledge_chunk (tenant_id, knowledge_base_id, status);

-- 按文档 ID 查询文档片段
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_document_id
ON knowledge_chunk (document_id);

-- 按创建时间查询数据
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_create_time
ON knowledge_chunk (create_time);
```

如果文本语义检索主要使用余弦距离，可以创建 HNSW 向量索引：

```sql
-- 使用 HNSW 创建余弦距离向量索引
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_embedding_hnsw
ON knowledge_chunk
USING hnsw (embedding vector_cosine_ops);
```

如果使用 L2 距离，可以创建如下索引：

```sql
-- 使用 HNSW 创建 L2 距离向量索引
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_embedding_l2_hnsw
ON knowledge_chunk
USING hnsw (embedding vector_l2_ops);
```

如果使用 IVFFlat，需要在表中已经存在一定数据后再创建索引。pgvector 官方说明中，IVFFlat 的召回效果依赖三个关键点：建索引前表中已有数据、选择合适的 `lists` 数量、查询时设置合适的 `probes`。官方建议在 100 万行以内可以从 `rows / 1000` 作为 `lists` 起点，超过 100 万行可以从 `sqrt(rows)` 作为起点；`probes` 可以从 `sqrt(lists)` 开始。([GitHub](https://github.com/pgvector/pgvector?utm_source=chatgpt.com))

```sql
-- 使用 IVFFlat 创建余弦距离向量索引，适合数据相对稳定后创建
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_embedding_ivfflat
ON knowledge_chunk
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);
```

查询时可以设置 `probes` 控制召回率和查询速度：

```sql
-- 提高 probes 可以提升召回率，但会增加查询耗时
SET ivfflat.probes = 10;
```

生产环境中，索引设计建议遵循以下原则：

1. 项目初期先使用精确检索，验证向量维度、距离算法和召回结果。
2. 数据量增长后优先考虑 HNSW，尤其是在线查询要求较高的语义搜索场景。
3. 使用 IVFFlat 时，先导入数据再创建索引，不建议空表时创建 IVFFlat。
4. 向量索引只能优化相似度排序，业务过滤字段仍然需要普通索引。
5. 多租户或多知识库场景下，必须为 `tenant_id`、`knowledge_base_id`、`status` 等过滤字段建立索引。
6. 查询性能问题需要结合 `EXPLAIN (ANALYZE, BUFFERS)` 分析执行计划，而不是只依赖索引数量判断。



## 核心功能实现

本节用于说明 pgvector 在项目中的核心使用方式，包括向量数据写入、相似度检索、业务条件过滤和 Top K 查询。核心实现主要围绕 PostgreSQL SQL 语句展开，后续可以在 Spring Boot 的 Mapper、Repository 或 Service 层中进行封装。

### 向量数据写入

向量数据写入是指将文本、文档片段或业务数据经过 Embedding 模型转换后，将生成的向量存入 PostgreSQL 的 `vector` 字段中。写入时需要保证向量维度与表结构中定义的维度一致，例如字段定义为 `vector(1536)`，则写入的向量必须是 1536 维。

通常写入流程如下：

1. 获取业务原始数据，例如文档片段、商品描述、问题内容。
2. 调用 Embedding 模型生成向量。
3. 将向量转换为 pgvector 支持的字符串格式。
4. 写入业务字段和向量字段。
5. 记录模型名称、模型版本、处理状态等元数据。

向量写入格式示例：

```text
[0.0123,0.2345,0.3456]
```

插入知识片段数据时，可以使用如下 SQL：

```sql
-- 写入知识片段及其向量数据
INSERT INTO knowledge_chunk (
    tenant_id,
    knowledge_base_id,
    document_id,
    chunk_index,
    title,
    content,
    embedding_model,
    embedding_version,
    embedding,
    status
) VALUES (
    1,
    1001,
    2001,
    0,
    'pgvector 简介',
    'pgvector 是 PostgreSQL 的向量扩展，可用于向量存储和相似度检索。',
    'text-embedding-model',
    'v1',
    '[0.0123,0.2345,0.3456]'::vector,
    1
);
```

在实际项目中，向量一般不会手动写入，而是由后端服务完成转换。Service 层可以将模型返回的 `List<Double>`、`List<Float>` 或数组转换为字符串格式，再交给 Mapper 写入。

批量写入时，建议采用批处理方式，避免每条数据单独提交事务：

```sql
-- 批量写入多个知识片段向量
INSERT INTO knowledge_chunk (
    tenant_id,
    knowledge_base_id,
    document_id,
    chunk_index,
    title,
    content,
    embedding_model,
    embedding_version,
    embedding,
    status
) VALUES
(
    1,
    1001,
    2001,
    0,
    '账号登录问题',
    '用户无法登录系统时，可以先检查账号状态和密码是否正确。',
    'text-embedding-model',
    'v1',
    '[0.11,0.22,0.33]'::vector,
    1
),
(
    1,
    1001,
    2001,
    1,
    '密码重置说明',
    '用户忘记密码后，可以通过手机号验证码或邮箱链接重置密码。',
    'text-embedding-model',
    'v1',
    '[0.12,0.21,0.31]'::vector,
    1
);
```

如果原始业务数据已经存在，也可以先插入文本内容，再异步更新向量字段：

```sql
-- 先写入文本内容，向量字段暂时为空
INSERT INTO knowledge_chunk (
    tenant_id,
    knowledge_base_id,
    document_id,
    chunk_index,
    title,
    content,
    embedding_model,
    embedding_version,
    status
) VALUES (
    1,
    1001,
    2002,
    0,
    '订单退款规则',
    '订单支付成功后，在满足退款条件时可以发起退款申请。',
    'text-embedding-model',
    'v1',
    1
);

-- 后续异步生成向量后再更新 embedding 字段
UPDATE knowledge_chunk
SET
    embedding = '[0.15,0.25,0.35]'::vector,
    update_time = CURRENT_TIMESTAMP
WHERE id = 1;
```

这种方式适合文档导入、批量解析、异步 Embedding 生成等场景。生产环境中建议增加处理状态字段，例如 `process_status`、`embedding_status` 或 `vector_status`，用于标识向量是否已经生成成功。

### 相似度检索

相似度检索是 pgvector 的核心能力。查询时需要先将用户输入内容转换为查询向量，然后使用 pgvector 的距离操作符对数据库中的向量字段进行排序，距离越小表示越相似。

常见距离计算方式如下：

| 操作符 | 距离类型 | 常见用途                     |
| ------ | -------- | ---------------------------- |
| `<->`  | L2 距离  | 适合欧氏距离相似度计算       |
| `<#>`  | 内积     | 适合部分向量模型的相似度计算 |
| `<=>`  | 余弦距离 | 常用于文本语义检索           |
| `<+>`  | L1 距离  | 适合部分数值特征相似度计算   |

文本语义检索通常使用余弦距离，即 `<=>`。查询结果中可以同时返回距离值，方便调试和排序展示：

```sql
-- 使用余弦距离查询最相似的知识片段
SELECT
    id,
    tenant_id,
    knowledge_base_id,
    document_id,
    chunk_index,
    title,
    content,
    embedding <=> '[0.10,0.20,0.30]'::vector AS distance
FROM knowledge_chunk
WHERE embedding IS NOT NULL
  AND status = 1
ORDER BY embedding <=> '[0.10,0.20,0.30]'::vector
LIMIT 10;
```

如果需要将余弦距离转换为相似度分数，可以使用 `1 - distance` 的方式计算：

```sql
-- 查询相似内容，并返回余弦相似度分数
SELECT
    id,
    title,
    content,
    embedding <=> '[0.10,0.20,0.30]'::vector AS distance,
    1 - (embedding <=> '[0.10,0.20,0.30]'::vector) AS similarity
FROM knowledge_chunk
WHERE embedding IS NOT NULL
  AND status = 1
ORDER BY embedding <=> '[0.10,0.20,0.30]'::vector
LIMIT 10;
```

返回字段说明如下：

| 字段          | 说明                           |
| ------------- | ------------------------------ |
| `distance`    | 向量距离，数值越小越相似       |
| `similarity`  | 相似度分数，通常数值越大越相似 |
| `content`     | 被召回的文本片段               |
| `document_id` | 片段所属文档 ID                |
| `chunk_index` | 片段在原始文档中的顺序         |

在 RAG 场景中，相似度检索结果通常不会直接返回给用户，而是作为上下文材料传递给大模型。此时需要重点关注召回内容的相关性、片段数量、上下文长度和排序稳定性。

### 条件过滤查询

实际业务中，向量检索通常不会在全量数据中直接执行，而是需要结合租户、知识库、文档类型、状态、分类、权限等条件进行过滤。条件过滤查询可以减少无关数据参与相似度排序，同时保证数据隔离和业务正确性。

典型过滤条件包括：

| 过滤条件            | 说明                   |
| ------------------- | ---------------------- |
| `tenant_id`         | 多租户数据隔离         |
| `knowledge_base_id` | 限定知识库范围         |
| `document_id`       | 限定某个文档范围       |
| `status`            | 只查询启用或已发布数据 |
| `embedding_model`   | 限定指定模型生成的向量 |
| `embedding_version` | 限定指定模型版本       |
| `create_time`       | 限定数据创建时间范围   |

按租户和知识库过滤后执行相似度检索：

```sql
-- 在指定租户和知识库范围内进行向量检索
SELECT
    id,
    title,
    content,
    document_id,
    chunk_index,
    embedding <=> '[0.10,0.20,0.30]'::vector AS distance
FROM knowledge_chunk
WHERE tenant_id = 1
  AND knowledge_base_id = 1001
  AND status = 1
  AND embedding IS NOT NULL
ORDER BY embedding <=> '[0.10,0.20,0.30]'::vector
LIMIT 10;
```

按模型版本过滤，适合模型升级或灰度发布场景：

```sql
-- 查询指定 Embedding 模型和版本下的相似数据
SELECT
    id,
    title,
    content,
    embedding_model,
    embedding_version,
    embedding <=> '[0.10,0.20,0.30]'::vector AS distance
FROM knowledge_chunk
WHERE tenant_id = 1
  AND knowledge_base_id = 1001
  AND embedding_model = 'text-embedding-model'
  AND embedding_version = 'v1'
  AND status = 1
  AND embedding IS NOT NULL
ORDER BY embedding <=> '[0.10,0.20,0.30]'::vector
LIMIT 10;
```

如果需要叠加关键词过滤，可以使用 `ILIKE` 进行简单模糊匹配：

```sql
-- 向量检索叠加关键词过滤
SELECT
    id,
    title,
    content,
    embedding <=> '[0.10,0.20,0.30]'::vector AS distance
FROM knowledge_chunk
WHERE tenant_id = 1
  AND knowledge_base_id = 1001
  AND status = 1
  AND embedding IS NOT NULL
  AND content ILIKE '%' || '密码' || '%'
ORDER BY embedding <=> '[0.10,0.20,0.30]'::vector
LIMIT 10;
```

需要注意的是，关键词过滤和向量检索的组合方式会影响召回结果。如果先用关键词过滤，可能会丢失语义相关但不包含关键词的数据；如果先做向量召回再做关键词过滤，则可能需要子查询或应用层二次处理。

可以使用子查询先扩大向量召回范围，再在外层进行业务处理：

```sql
-- 先召回更多相似片段，再在外层做二次过滤和限制
SELECT
    t.id,
    t.title,
    t.content,
    t.distance
FROM (
    SELECT
        id,
        title,
        content,
        embedding <=> '[0.10,0.20,0.30]'::vector AS distance
    FROM knowledge_chunk
    WHERE tenant_id = 1
      AND knowledge_base_id = 1001
      AND status = 1
      AND embedding IS NOT NULL
    ORDER BY embedding <=> '[0.10,0.20,0.30]'::vector
    LIMIT 50
) t
WHERE t.content ILIKE '%' || '密码' || '%'
ORDER BY t.distance
LIMIT 10;
```

这种方式适合需要兼顾语义召回和关键词约束的混合查询场景。

### Top K 查询

Top K 查询是指根据输入向量返回最相似的前 K 条数据。K 值通常由业务场景决定，例如语义搜索页面可能返回前 10 条，RAG 上下文召回可能返回前 3 到 8 条，相似推荐可能返回前 5 条。

基础 Top K 查询如下：

```sql
-- 查询最相似的前 K 条数据
SELECT
    id,
    title,
    content,
    embedding <=> '[0.10,0.20,0.30]'::vector AS distance
FROM knowledge_chunk
WHERE tenant_id = 1
  AND knowledge_base_id = 1001
  AND status = 1
  AND embedding IS NOT NULL
ORDER BY embedding <=> '[0.10,0.20,0.30]'::vector
LIMIT 5;
```

如果需要设置相似度阈值，可以在外层对子查询结果进行过滤。由于 `distance` 是计算字段，推荐使用子查询提高可读性：

```sql
-- Top K 查询叠加距离阈值过滤
SELECT
    t.id,
    t.title,
    t.content,
    t.distance,
    1 - t.distance AS similarity
FROM (
    SELECT
        id,
        title,
        content,
        embedding <=> '[0.10,0.20,0.30]'::vector AS distance
    FROM knowledge_chunk
    WHERE tenant_id = 1
      AND knowledge_base_id = 1001
      AND status = 1
      AND embedding IS NOT NULL
    ORDER BY embedding <=> '[0.10,0.20,0.30]'::vector
    LIMIT 20
) t
WHERE t.distance <= 0.35
ORDER BY t.distance
LIMIT 5;
```

不同业务场景的 K 值建议如下：

| 场景           | 推荐 K 值 | 说明                             |
| -------------- | --------- | -------------------------------- |
| 搜索结果页     | 10 到 20  | 需要给用户展示多个候选结果       |
| RAG 上下文召回 | 3 到 8    | 避免上下文过长影响大模型回答质量 |
| 相似内容推荐   | 5 到 10   | 返回少量高相关内容即可           |
| 去重判断       | 1 到 3    | 重点关注最相似的数据             |
| 后台审核辅助   | 10 到 50  | 可以扩大候选范围，供人工判断     |

如果需要排除当前数据本身，例如查询某篇文章的相似文章，需要在条件中排除当前 ID：

```sql
-- 查询相似内容时排除当前数据
SELECT
    id,
    title,
    content,
    embedding <=> (
        SELECT embedding
        FROM knowledge_chunk
        WHERE id = 100
    ) AS distance
FROM knowledge_chunk
WHERE tenant_id = 1
  AND knowledge_base_id = 1001
  AND status = 1
  AND embedding IS NOT NULL
  AND id <> 100
ORDER BY embedding <=> (
    SELECT embedding
    FROM knowledge_chunk
    WHERE id = 100
)
LIMIT 5;
```

为了避免重复子查询，也可以使用公共表表达式：

```sql
-- 使用 CTE 查询当前片段的相似内容
WITH current_chunk AS (
    SELECT embedding
    FROM knowledge_chunk
    WHERE id = 100
      AND embedding IS NOT NULL
)
SELECT
    kc.id,
    kc.title,
    kc.content,
    kc.embedding <=> cc.embedding AS distance
FROM knowledge_chunk kc
CROSS JOIN current_chunk cc
WHERE kc.tenant_id = 1
  AND kc.knowledge_base_id = 1001
  AND kc.status = 1
  AND kc.embedding IS NOT NULL
  AND kc.id <> 100
ORDER BY kc.embedding <=> cc.embedding
LIMIT 5;
```

Top K 查询在生产环境中需要重点关注以下事项：

1. K 值不宜设置过大，否则会增加排序和网络传输成本。
2. RAG 场景中不应只看 K 值，还要控制召回文本总长度。
3. 如果使用余弦距离，通常可以结合 `1 - distance` 作为相似度分数展示。
4. 如果查询结果相关性不稳定，应优先检查 Embedding 模型、文本切片策略和距离算法。
5. 如果查询延迟较高，需要结合向量索引、业务字段索引和 `EXPLAIN ANALYZE` 分析执行计划。



## Spring Boot 集成

本节用于说明如何在 Spring Boot 项目中集成 PostgreSQL pgvector，包括数据库连接配置、实体类字段映射、Mapper SQL 实现以及 Service 层封装。项目采用 Spring Boot 3 + PostgreSQL + MyBatis-Plus + XML 原生 SQL 的方式实现，普通业务字段可以使用 MyBatis-Plus，向量写入和相似度检索建议使用 XML SQL 显式处理。

pgvector 官方提供了 Java 支持库 `com.pgvector:pgvector`，支持 JDBC、Spring JDBC、Hibernate、R2DBC 等方式，并且可以通过 `PGvector` 对象写入和查询向量数据；在 MyBatis XML 场景中，也可以将向量转换为 pgvector 字符串格式，再通过 `::vector` 显式转换写入 PostgreSQL。([GitHub](https://github.com/pgvector/pgvector-java?utm_source=chatgpt.com))

### 数据库连接配置

Spring Boot 连接 pgvector 本质上仍然是连接 PostgreSQL。项目需要引入 PostgreSQL JDBC 驱动，并在 `application.yml` 中配置数据库连接信息。pgvector 扩展本身在数据库层启用，应用启动后只需要访问包含 `vector` 扩展的数据库即可。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供 REST 接口 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- PostgreSQL JDBC 驱动，用于连接 PostgreSQL 数据库 -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>

    <!-- MyBatis-Plus Spring Boot 3 启动器，用于基础 Mapper 能力 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.7</version>
    </dependency>

    <!-- pgvector Java 支持库，可用于 JDBC 场景下处理 PGvector 对象 -->
    <dependency>
        <groupId>com.pgvector</groupId>
        <artifactId>pgvector</artifactId>
        <version>0.1.6</version>
    </dependency>

    <!-- Hutool 工具类，用于字符串、集合、数字、JSON 等通用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.32</version>
    </dependency>

    <!-- Lombok，用于减少实体类、DTO、VO 的样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 参数校验，用于接口入参合法性校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
</dependencies>
```

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 应用服务端口
  port: 8080

spring:
  application:
    # 应用名称
    name: pgvector-demo

  datasource:
    # PostgreSQL 数据库连接地址
    url: jdbc:postgresql://127.0.0.1:5432/vector_db
    # 数据库用户名
    username: postgres
    # 数据库密码
    password: 123456
    # PostgreSQL JDBC 驱动类
    driver-class-name: org.postgresql.Driver

mybatis-plus:
  # Mapper XML 文件扫描路径
  mapper-locations: classpath*:/mapper/**/*.xml
  configuration:
    # 开发环境输出 SQL 日志，生产环境建议关闭或改为日志框架输出
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    # 开启下划线字段到驼峰属性映射
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      # 数据库主键使用 bigserial 时，Java 侧使用自增策略
      id-type: auto

logging:
  level:
    # 项目包日志级别
    io.github.atengk: info
```

应用启动前，需要确认目标数据库已经启用 pgvector 扩展：

```sql
-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 检查扩展版本
SELECT extname, extversion
FROM pg_extension
WHERE extname = 'vector';
```

pgvector 官方 Java 示例中，JDBC 方式可以通过 `CREATE EXTENSION IF NOT EXISTS vector` 启用扩展，并通过 `PGvector.registerTypes(conn)` 注册向量类型；如果项目采用 MyBatis XML 并使用 `::vector` 显式转换，通常不需要在业务代码中手动注册类型。([GitHub](https://github.com/pgvector/pgvector-java?utm_source=chatgpt.com))

### 实体类与字段映射

实体类用于映射 `knowledge_chunk` 表中的业务字段。由于 `vector` 是 PostgreSQL 扩展类型，不建议直接依赖 MyBatis-Plus 的通用 CRUD 自动写入向量字段。更稳妥的方式是将 `embedding` 在 Java 层保存为 pgvector 字符串格式，例如 `[0.1,0.2,0.3]`，然后在 XML SQL 中使用 `#{embedding}::vector` 写入数据库。

推荐文件结构如下：

```text
src/main/java/io/github/atengk/vector/
├── entity/
│   └── KnowledgeChunkEntity.java
├── dto/
│   ├── KnowledgeChunkCreateDTO.java
│   └── VectorSearchDTO.java
├── vo/
│   └── KnowledgeChunkSearchVO.java
├── mapper/
│   └── KnowledgeChunkMapper.java
├── service/
│   ├── KnowledgeChunkService.java
│   └── impl/KnowledgeChunkServiceImpl.java
└── util/
    └── VectorConvertUtil.java

src/main/resources/mapper/
└── KnowledgeChunkMapper.xml
```

文件位置：`src/main/java/io/github/atengk/vector/entity/KnowledgeChunkEntity.java`

```java
package io.github.atengk.vector.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识片段向量实体
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@TableName("knowledge_chunk")
public class KnowledgeChunkEntity {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户 ID
     */
    private Long tenantId;

    /**
     * 知识库 ID
     */
    private Long knowledgeBaseId;

    /**
     * 原始文档 ID
     */
    private Long documentId;

    /**
     * 文档片段序号
     */
    private Integer chunkIndex;

    /**
     * 片段标题
     */
    private String title;

    /**
     * 片段正文内容
     */
    private String content;

    /**
     * Embedding 模型名称
     */
    private String embeddingModel;

    /**
     * Embedding 模型版本
     */
    private String embeddingVersion;

    /**
     * 向量字符串，格式示例：[0.1,0.2,0.3]
     */
    private String embedding;

    /**
     * 数据状态：1 启用，0 禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
```

文件位置：`src/main/java/io/github/atengk/vector/dto/KnowledgeChunkCreateDTO.java`

```java
package io.github.atengk.vector.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 知识片段创建参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class KnowledgeChunkCreateDTO {

    /**
     * 租户 ID
     */
    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    /**
     * 知识库 ID
     */
    @NotNull(message = "知识库ID不能为空")
    private Long knowledgeBaseId;

    /**
     * 原始文档 ID
     */
    @NotNull(message = "文档ID不能为空")
    private Long documentId;

    /**
     * 文档片段序号
     */
    @NotNull(message = "片段序号不能为空")
    private Integer chunkIndex;

    /**
     * 片段标题
     */
    private String title;

    /**
     * 片段正文内容
     */
    @NotBlank(message = "片段内容不能为空")
    private String content;

    /**
     * Embedding 模型名称
     */
    @NotBlank(message = "向量模型名称不能为空")
    private String embeddingModel;

    /**
     * Embedding 模型版本
     */
    @NotBlank(message = "向量模型版本不能为空")
    private String embeddingVersion;

    /**
     * 向量数组
     */
    @NotEmpty(message = "向量数据不能为空")
    private List<Float> embedding;
}
```

文件位置：`src/main/java/io/github/atengk/vector/dto/VectorSearchDTO.java`

```java
package io.github.atengk.vector.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 向量检索参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class VectorSearchDTO {

    /**
     * 租户 ID
     */
    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    /**
     * 知识库 ID
     */
    @NotNull(message = "知识库ID不能为空")
    private Long knowledgeBaseId;

    /**
     * 查询向量
     */
    @NotEmpty(message = "查询向量不能为空")
    private List<Float> queryEmbedding;

    /**
     * 返回数量
     */
    @NotNull(message = "返回数量不能为空")
    @Min(value = 1, message = "返回数量不能小于1")
    @Max(value = 50, message = "返回数量不能大于50")
    private Integer topK;

    /**
     * 最小相似度，可为空
     */
    private Double minSimilarity;
}
```

文件位置：`src/main/java/io/github/atengk/vector/vo/KnowledgeChunkSearchVO.java`

```java
package io.github.atengk.vector.vo;

import lombok.Data;

/**
 * 知识片段检索结果
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class KnowledgeChunkSearchVO {

    /**
     * 主键 ID
     */
    private Long id;

    /**
     * 文档 ID
     */
    private Long documentId;

    /**
     * 片段序号
     */
    private Integer chunkIndex;

    /**
     * 片段标题
     */
    private String title;

    /**
     * 片段内容
     */
    private String content;

    /**
     * 向量距离，数值越小越相似
     */
    private Double distance;

    /**
     * 相似度分数，数值越大越相似
     */
    private Double similarity;
}
```

以下工具类用于将 `List<Float>` 转换为 pgvector 支持的字符串格式。

文件位置：`src/main/java/io/github/atengk/vector/util/VectorConvertUtil.java`

```java
package io.github.atengk.vector.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.StrJoiner;

import java.util.List;

/**
 * 向量格式转换工具
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class VectorConvertUtil {

    private VectorConvertUtil() {
    }

    /**
     * 将 Float 集合转换为 pgvector 字符串格式
     *
     * @param vector 向量集合
     * @return pgvector 字符串，格式：[0.1,0.2,0.3]
     */
    public static String toPgVectorString(List<Float> vector) {
        if (CollUtil.isEmpty(vector)) {
            throw new IllegalArgumentException("向量数据不能为空");
        }

        StrJoiner joiner = StrJoiner.of(",");
        for (Float item : vector) {
            if (item == null) {
                throw new IllegalArgumentException("向量元素不能为 null");
            }
            joiner.append(item.toString());
        }
        return "[" + joiner + "]";
    }
}
```

### Repository 或 Mapper 实现

pgvector 的核心查询依赖 PostgreSQL 的向量距离操作符，例如 `<=>` 表示余弦距离，`<->` 表示 L2 距离，`<#>` 表示内积。由于这些 SQL 不属于普通 CRUD，建议使用 Mapper XML 单独维护，便于控制 `ORDER BY embedding <=> #{queryEmbedding}::vector`、`LIMIT`、相似度阈值等逻辑。pgvector 官方 Java 示例也展示了通过 SQL `ORDER BY embedding <-> ? LIMIT 5` 获取最近邻结果的方式。([GitHub](https://github.com/pgvector/pgvector-java?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/vector/mapper/KnowledgeChunkMapper.java`

```java
package io.github.atengk.vector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.vector.entity.KnowledgeChunkEntity;
import io.github.atengk.vector.vo.KnowledgeChunkSearchVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识片段向量 Mapper
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Mapper
public interface KnowledgeChunkMapper extends BaseMapper<KnowledgeChunkEntity> {

    /**
     * 写入知识片段向量
     *
     * @param entity 知识片段实体
     * @return 影响行数
     */
    int insertChunk(KnowledgeChunkEntity entity);

    /**
     * 根据查询向量检索相似知识片段
     *
     * @param tenantId        租户 ID
     * @param knowledgeBaseId 知识库 ID
     * @param queryEmbedding  查询向量字符串
     * @param topK            返回数量
     * @return 相似知识片段列表
     */
    List<KnowledgeChunkSearchVO> searchByVector(@Param("tenantId") Long tenantId,
                                                @Param("knowledgeBaseId") Long knowledgeBaseId,
                                                @Param("queryEmbedding") String queryEmbedding,
                                                @Param("topK") Integer topK);

    /**
     * 根据查询向量和相似度阈值检索知识片段
     *
     * @param tenantId        租户 ID
     * @param knowledgeBaseId 知识库 ID
     * @param queryEmbedding  查询向量字符串
     * @param minSimilarity   最小相似度
     * @param topK            返回数量
     * @return 相似知识片段列表
     */
    List<KnowledgeChunkSearchVO> searchByVectorWithThreshold(@Param("tenantId") Long tenantId,
                                                             @Param("knowledgeBaseId") Long knowledgeBaseId,
                                                             @Param("queryEmbedding") String queryEmbedding,
                                                             @Param("minSimilarity") Double minSimilarity,
                                                             @Param("topK") Integer topK);
}
```

文件位置：`src/main/resources/mapper/KnowledgeChunkMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="io.github.atengk.vector.mapper.KnowledgeChunkMapper">

    <!-- 知识片段检索结果映射 -->
    <resultMap id="KnowledgeChunkSearchMap" type="io.github.atengk.vector.vo.KnowledgeChunkSearchVO">
        <id column="id" property="id"/>
        <result column="document_id" property="documentId"/>
        <result column="chunk_index" property="chunkIndex"/>
        <result column="title" property="title"/>
        <result column="content" property="content"/>
        <result column="distance" property="distance"/>
        <result column="similarity" property="similarity"/>
    </resultMap>

    <!-- 写入知识片段向量，embedding 使用 ::vector 显式转换 -->
    <insert id="insertChunk" parameterType="io.github.atengk.vector.entity.KnowledgeChunkEntity"
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO knowledge_chunk (
            tenant_id,
            knowledge_base_id,
            document_id,
            chunk_index,
            title,
            content,
            embedding_model,
            embedding_version,
            embedding,
            status,
            create_time,
            update_time
        ) VALUES (
            #{tenantId},
            #{knowledgeBaseId},
            #{documentId},
            #{chunkIndex},
            #{title},
            #{content},
            #{embeddingModel},
            #{embeddingVersion},
            #{embedding}::vector,
            #{status},
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        )
    </insert>

    <!-- 使用余弦距离检索 Top K 相似知识片段 -->
    <select id="searchByVector" resultMap="KnowledgeChunkSearchMap">
        SELECT
            id,
            document_id,
            chunk_index,
            title,
            content,
            embedding &lt;=&gt; #{queryEmbedding}::vector AS distance,
            1 - (embedding &lt;=&gt; #{queryEmbedding}::vector) AS similarity
        FROM knowledge_chunk
        WHERE tenant_id = #{tenantId}
          AND knowledge_base_id = #{knowledgeBaseId}
          AND status = 1
          AND embedding IS NOT NULL
        ORDER BY embedding &lt;=&gt; #{queryEmbedding}::vector
        LIMIT #{topK}
    </select>

    <!-- 使用余弦距离检索，并按最小相似度过滤 -->
    <select id="searchByVectorWithThreshold" resultMap="KnowledgeChunkSearchMap">
        SELECT
            t.id,
            t.document_id,
            t.chunk_index,
            t.title,
            t.content,
            t.distance,
            t.similarity
        FROM (
            SELECT
                id,
                document_id,
                chunk_index,
                title,
                content,
                embedding &lt;=&gt; #{queryEmbedding}::vector AS distance,
                1 - (embedding &lt;=&gt; #{queryEmbedding}::vector) AS similarity
            FROM knowledge_chunk
            WHERE tenant_id = #{tenantId}
              AND knowledge_base_id = #{knowledgeBaseId}
              AND status = 1
              AND embedding IS NOT NULL
            ORDER BY embedding &lt;=&gt; #{queryEmbedding}::vector
            LIMIT #{topK}
        ) t
        WHERE t.similarity &gt;= #{minSimilarity}
        ORDER BY t.distance
    </select>

</mapper>
```

这里需要注意 XML 中的 `<=>` 必须转义为 `<=>`，否则 MyBatis XML 会把 `<` 识别为标签开始符号，导致 XML 解析失败。

### Service 封装

Service 层负责屏蔽向量格式转换、参数校验、默认值处理和 Mapper 调用细节。Controller 或其他业务模块只需要传入 `List<Float>`，不需要关心 pgvector 的字符串格式和 SQL 转换规则。

文件位置：`src/main/java/io/github/atengk/vector/service/KnowledgeChunkService.java`

```java
package io.github.atengk.vector.service;

import io.github.atengk.vector.dto.KnowledgeChunkCreateDTO;
import io.github.atengk.vector.dto.VectorSearchDTO;
import io.github.atengk.vector.vo.KnowledgeChunkSearchVO;

import java.util.List;

/**
 * 知识片段向量服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface KnowledgeChunkService {

    /**
     * 创建知识片段向量
     *
     * @param createDTO 创建参数
     * @return 知识片段 ID
     */
    Long createChunk(KnowledgeChunkCreateDTO createDTO);

    /**
     * 向量相似度检索
     *
     * @param searchDTO 检索参数
     * @return 相似知识片段列表
     */
    List<KnowledgeChunkSearchVO> search(VectorSearchDTO searchDTO);
}
```

文件位置：`src/main/java/io/github/atengk/vector/service/impl/KnowledgeChunkServiceImpl.java`

```java
package io.github.atengk.vector.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.vector.dto.KnowledgeChunkCreateDTO;
import io.github.atengk.vector.dto.VectorSearchDTO;
import io.github.atengk.vector.entity.KnowledgeChunkEntity;
import io.github.atengk.vector.mapper.KnowledgeChunkMapper;
import io.github.atengk.vector.service.KnowledgeChunkService;
import io.github.atengk.vector.util.VectorConvertUtil;
import io.github.atengk.vector.vo.KnowledgeChunkSearchVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 知识片段向量服务实现
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeChunkServiceImpl implements KnowledgeChunkService {

    private final KnowledgeChunkMapper knowledgeChunkMapper;

    /**
     * 创建知识片段向量
     *
     * @param createDTO 创建参数
     * @return 知识片段 ID
     */
    @Override
    public Long createChunk(KnowledgeChunkCreateDTO createDTO) {
        String embedding = VectorConvertUtil.toPgVectorString(createDTO.getEmbedding());

        KnowledgeChunkEntity entity = new KnowledgeChunkEntity();
        entity.setTenantId(createDTO.getTenantId());
        entity.setKnowledgeBaseId(createDTO.getKnowledgeBaseId());
        entity.setDocumentId(createDTO.getDocumentId());
        entity.setChunkIndex(createDTO.getChunkIndex());
        entity.setTitle(createDTO.getTitle());
        entity.setContent(createDTO.getContent());
        entity.setEmbeddingModel(createDTO.getEmbeddingModel());
        entity.setEmbeddingVersion(createDTO.getEmbeddingVersion());
        entity.setEmbedding(embedding);
        entity.setStatus(1);

        int rows = knowledgeChunkMapper.insertChunk(entity);
        if (rows <= 0 || ObjectUtil.isNull(entity.getId())) {
            log.error("知识片段向量写入失败，tenantId={}，knowledgeBaseId={}，documentId={}",
                    createDTO.getTenantId(), createDTO.getKnowledgeBaseId(), createDTO.getDocumentId());
            throw new IllegalStateException("知识片段向量写入失败");
        }

        log.info("知识片段向量写入成功，id={}，tenantId={}，knowledgeBaseId={}",
                entity.getId(), entity.getTenantId(), entity.getKnowledgeBaseId());
        return entity.getId();
    }

    /**
     * 向量相似度检索
     *
     * @param searchDTO 检索参数
     * @return 相似知识片段列表
     */
    @Override
    public List<KnowledgeChunkSearchVO> search(VectorSearchDTO searchDTO) {
        String queryEmbedding = VectorConvertUtil.toPgVectorString(searchDTO.getQueryEmbedding());

        List<KnowledgeChunkSearchVO> resultList;
        if (ObjectUtil.isNotNull(searchDTO.getMinSimilarity())) {
            resultList = knowledgeChunkMapper.searchByVectorWithThreshold(
                    searchDTO.getTenantId(),
                    searchDTO.getKnowledgeBaseId(),
                    queryEmbedding,
                    searchDTO.getMinSimilarity(),
                    searchDTO.getTopK()
            );
        } else {
            resultList = knowledgeChunkMapper.searchByVector(
                    searchDTO.getTenantId(),
                    searchDTO.getKnowledgeBaseId(),
                    queryEmbedding,
                    searchDTO.getTopK()
            );
        }

        if (CollUtil.isEmpty(resultList)) {
            log.info("向量检索未命中数据，tenantId={}，knowledgeBaseId={}，topK={}",
                    searchDTO.getTenantId(), searchDTO.getKnowledgeBaseId(), searchDTO.getTopK());
            return List.of();
        }

        log.info("向量检索完成，tenantId={}，knowledgeBaseId={}，topK={}，命中数量={}",
                searchDTO.getTenantId(), searchDTO.getKnowledgeBaseId(), searchDTO.getTopK(), resultList.size());
        return resultList;
    }
}
```

如果需要提供接口层，可以增加 Controller 对外暴露写入和检索能力：

文件位置：`src/main/java/io/github/atengk/vector/controller/KnowledgeChunkController.java`

```java
package io.github.atengk.vector.controller;

import io.github.atengk.vector.dto.KnowledgeChunkCreateDTO;
import io.github.atengk.vector.dto.VectorSearchDTO;
import io.github.atengk.vector.service.KnowledgeChunkService;
import io.github.atengk.vector.vo.KnowledgeChunkSearchVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识片段向量接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vector/chunks")
public class KnowledgeChunkController {

    private final KnowledgeChunkService knowledgeChunkService;

    /**
     * 写入知识片段向量
     *
     * @param createDTO 创建参数
     * @return 知识片段 ID
     */
    @PostMapping
    public Long createChunk(@Valid @RequestBody KnowledgeChunkCreateDTO createDTO) {
        return knowledgeChunkService.createChunk(createDTO);
    }

    /**
     * 向量相似度检索
     *
     * @param searchDTO 检索参数
     * @return 相似知识片段列表
     */
    @PostMapping("/search")
    public List<KnowledgeChunkSearchVO> search(@Valid @RequestBody VectorSearchDTO searchDTO) {
        return knowledgeChunkService.search(searchDTO);
    }
}
```

接口调用示例：

```bash
# 写入知识片段向量
curl -X POST 'http://127.0.0.1:8080/api/vector/chunks' \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": 1,
    "knowledgeBaseId": 1001,
    "documentId": 2001,
    "chunkIndex": 0,
    "title": "密码重置说明",
    "content": "用户忘记密码后，可以通过手机号验证码或邮箱链接重置密码。",
    "embeddingModel": "text-embedding-model",
    "embeddingVersion": "v1",
    "embedding": [0.12, 0.21, 0.31]
  }'

# 根据查询向量检索相似知识片段
curl -X POST 'http://127.0.0.1:8080/api/vector/chunks/search' \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": 1,
    "knowledgeBaseId": 1001,
    "queryEmbedding": [0.10, 0.20, 0.30],
    "topK": 5,
    "minSimilarity": 0.7
  }'
```

返回示例：

```json
[
  {
    "id": 1,
    "documentId": 2001,
    "chunkIndex": 0,
    "title": "密码重置说明",
    "content": "用户忘记密码后，可以通过手机号验证码或邮箱链接重置密码。",
    "distance": 0.12,
    "similarity": 0.88
  }
]
```

实际项目中，Controller 接收到的一般不是手写向量，而是用户输入的文本。生产流程通常是：用户提交文本，Service 调用 Embedding 模型生成向量，再调用 `KnowledgeChunkService.search()` 完成检索。当前示例为了聚焦 Spring Boot 与 pgvector 集成，直接使用向量数组作为入参。



## 向量生成与存储流程

本节用于说明文本数据从原始内容到向量入库的完整流程。向量生成不是单纯调用 Embedding 模型，而是包含文本清洗、文本切片、模型调用、向量格式转换、数据库写入、状态更新和异常处理等步骤。

整体流程如下：

```text
原始文本
  ↓
文本预处理
  ↓
文本切片
  ↓
调用 Embedding 模型
  ↓
生成向量数组
  ↓
转换为 pgvector 字符串格式
  ↓
写入 knowledge_chunk 表
  ↓
用于相似度检索
```

### 文本预处理

文本预处理用于将原始文档、网页内容、用户输入或业务文本转换为适合生成 Embedding 的标准文本。预处理质量会直接影响向量检索效果，尤其是在知识库问答和 RAG 场景中，如果文本中存在大量噪声、空白字符、无效符号或过长片段，会降低召回质量。

常见预处理内容如下：

| 处理项       | 说明                                     |
| ------------ | ---------------------------------------- |
| 去除多余空白 | 清理连续空格、制表符、换行符             |
| 去除无效字符 | 清理不可见字符、控制字符、异常编码       |
| 保留语义结构 | 保留标题、段落、编号、代码块等有意义结构 |
| 文本切片     | 将长文档切分为多个较短片段               |
| 长度控制     | 避免单个片段超过 Embedding 模型限制      |
| 元数据保留   | 保留文档 ID、标题、片段序号、来源等信息  |

文本切片通常不建议按固定字符数硬切，而是优先按标题、段落、句子进行切分。对于知识库问答场景，每个片段需要尽量保持语义完整，避免一个问题描述被切成多个不完整片段。

推荐切片策略如下：

| 文本类型      | 推荐切片方式                     |
| ------------- | -------------------------------- |
| FAQ           | 一个问题和答案作为一个片段       |
| Markdown 文档 | 按标题层级和段落切分             |
| 普通文章      | 按段落或句子切分                 |
| 技术文档      | 保留标题、说明和相关代码块       |
| 表格内容      | 转换为可读文本后按行或业务块切分 |

以下工具类用于完成基础文本清洗和简单切片，适合项目初期使用。后续可以根据 Markdown、HTML、PDF 等不同来源扩展专门解析器。

文件位置：`src/main/java/io/github/atengk/vector/util/TextPreprocessUtil.java`

```java
package io.github.atengk.vector.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本预处理工具
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class TextPreprocessUtil {

    private static final int DEFAULT_CHUNK_SIZE = 800;

    private static final int DEFAULT_OVERLAP_SIZE = 100;

    private TextPreprocessUtil() {
    }

    public static String cleanText(String text) {
        if (StrUtil.isBlank(text)) {
            return StrUtil.EMPTY;
        }

        String cleanText = text;
        cleanText = ReUtil.replaceAll(cleanText, "[\\u0000-\\u001F\\u007F]", StrPool.SPACE);
        cleanText = ReUtil.replaceAll(cleanText, "\\s+", StrPool.SPACE);
        return StrUtil.trim(cleanText);
    }

    public static List<String> splitText(String text) {
        return splitText(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP_SIZE);
    }

    public static List<String> splitText(String text, int chunkSize, int overlapSize) {
        String cleanText = cleanText(text);
        if (StrUtil.isBlank(cleanText)) {
            return List.of();
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("文本切片长度必须大于0");
        }
        if (overlapSize < 0 || overlapSize >= chunkSize) {
            throw new IllegalArgumentException("文本重叠长度必须大于等于0且小于切片长度");
        }

        List<String> chunkList = new ArrayList<>();
        int start = 0;
        while (start < cleanText.length()) {
            int end = Math.min(start + chunkSize, cleanText.length());
            String chunk = StrUtil.sub(cleanText, start, end);
            if (StrUtil.isNotBlank(chunk)) {
                chunkList.add(chunk);
            }

            if (end >= cleanText.length()) {
                break;
            }
            start = end - overlapSize;
        }

        return CollUtil.unmodifiable(chunkList);
    }
}
```

该工具类只提供基础切片能力。对于正式知识库项目，建议根据文档类型增强切片规则，例如 Markdown 按标题层级切分、HTML 先提取正文、PDF 先按页码和段落解析，再生成片段。

### Embedding 模型调用

Embedding 模型调用用于将文本转换为固定维度的向量数组。项目中不建议在业务代码中直接绑定某一个模型厂商，而是先定义统一的 Embedding 客户端接口，再根据实际情况实现 OpenAI、本地模型、企业模型网关或其他模型服务。

抽象接口的好处如下：

1. 业务代码不依赖具体模型厂商。
2. 模型切换时只需要替换实现类。
3. 可以统一处理日志、异常、重试和耗时统计。
4. 可以在测试环境中使用 Mock 实现。
5. 可以统一校验向量维度。

推荐定义统一 Embedding 接口。

文件位置：`src/main/java/io/github/atengk/vector/client/EmbeddingClient.java`

```java
package io.github.atengk.vector.client;

import java.util.List;

/**
 * Embedding 模型客户端
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface EmbeddingClient {

    List<Float> embed(String text);

    List<List<Float>> embedBatch(List<String> textList);

    String modelName();

    String modelVersion();

    int dimension();
}
```

以下示例提供一个 Mock 实现，用于本地开发和接口联调。真实项目中需要替换为实际 Embedding 模型调用逻辑。

文件位置：`src/main/java/io/github/atengk/vector/client/impl/MockEmbeddingClient.java`

```java
package io.github.atengk.vector.client.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.HashUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.vector.client.EmbeddingClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock Embedding 模型客户端
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class MockEmbeddingClient implements EmbeddingClient {

    private static final int MOCK_DIMENSION = 3;

    @Override
    public List<Float> embed(String text) {
        if (StrUtil.isBlank(text)) {
            throw new IllegalArgumentException("生成向量的文本不能为空");
        }

        int hash = HashUtil.fnvHash(text);
        List<Float> vector = new ArrayList<>(MOCK_DIMENSION);
        vector.add((hash % 100) / 100.0F);
        vector.add((hash % 1000) / 1000.0F);
        vector.add((hash % 10000) / 10000.0F);

        log.info("Mock 向量生成完成，文本长度={}，向量维度={}", text.length(), vector.size());
        return vector;
    }

    @Override
    public List<List<Float>> embedBatch(List<String> textList) {
        if (CollUtil.isEmpty(textList)) {
            return List.of();
        }

        List<List<Float>> resultList = new ArrayList<>(textList.size());
        for (String text : textList) {
            resultList.add(embed(text));
        }

        log.info("Mock 批量向量生成完成，文本数量={}", textList.size());
        return resultList;
    }

    @Override
    public String modelName() {
        return "mock-embedding-model";
    }

    @Override
    public String modelVersion() {
        return "v1";
    }

    @Override
    public int dimension() {
        return MOCK_DIMENSION;
    }
}
```

真实模型调用时需要重点关注以下问题：

| 问题     | 处理建议                                            |
| -------- | --------------------------------------------------- |
| 向量维度 | 必须与数据库 `vector(n)` 定义一致                   |
| 超长文本 | 调用模型前先切片或截断                              |
| 空文本   | 不调用模型，直接标记为处理失败                      |
| 调用失败 | 记录失败原因，支持后续重试                          |
| 批量调用 | 优先使用批量接口，减少网络开销                      |
| 模型版本 | 入库时记录 `embedding_model` 和 `embedding_version` |
| 结果校验 | 校验返回向量不为空且维度正确                        |

如果生产模型返回 1536 维向量，则数据库字段需要定义为：

```sql
-- 向量维度必须和 Embedding 模型输出维度一致
embedding vector(1536)
```

如果本地 Mock 模型只返回 3 维向量，则只能用于开发验证，对应测试表字段应为 `vector(3)`。正式环境不能混用不同维度的向量。

### 向量入库流程

向量入库流程负责将预处理后的文本片段、模型生成的向量和业务元数据写入 `knowledge_chunk` 表。建议以文档为单位处理：先保存文档主表，再切分为多个片段，逐个生成向量并写入片段表。

推荐流程如下：

1. 接收文档标题、正文、租户 ID、知识库 ID 等参数。
2. 清洗正文内容。
3. 将正文切分为多个片段。
4. 批量调用 Embedding 模型生成向量。
5. 校验向量数量和片段数量是否一致。
6. 将每个片段转换为 `KnowledgeChunkCreateDTO`。
7. 调用 `KnowledgeChunkService.createChunk()` 写入数据库。
8. 记录处理日志和异常信息。

以下 DTO 用于接收文本入库请求。

文件位置：`src/main/java/io/github/atengk/vector/dto/TextIngestDTO.java`

```java
package io.github.atengk.vector.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文本向量入库参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class TextIngestDTO {

    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    @NotNull(message = "知识库ID不能为空")
    private Long knowledgeBaseId;

    @NotNull(message = "文档ID不能为空")
    private Long documentId;

    @NotBlank(message = "文档标题不能为空")
    private String title;

    @NotBlank(message = "文档内容不能为空")
    private String content;
}
```

以下 Service 用于封装“文本清洗、切片、向量生成、向量入库”的完整流程。

文件位置：`src/main/java/io/github/atengk/vector/service/TextVectorIngestService.java`

```java
package io.github.atengk.vector.service;

import io.github.atengk.vector.dto.TextIngestDTO;

import java.util.List;

/**
 * 文本向量入库服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface TextVectorIngestService {

    List<Long> ingest(TextIngestDTO ingestDTO);
}
```

文件位置：`src/main/java/io/github/atengk/vector/service/impl/TextVectorIngestServiceImpl.java`

```java
package io.github.atengk.vector.service.impl;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.vector.client.EmbeddingClient;
import io.github.atengk.vector.dto.KnowledgeChunkCreateDTO;
import io.github.atengk.vector.dto.TextIngestDTO;
import io.github.atengk.vector.service.KnowledgeChunkService;
import io.github.atengk.vector.service.TextVectorIngestService;
import io.github.atengk.vector.util.TextPreprocessUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本向量入库服务实现
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TextVectorIngestServiceImpl implements TextVectorIngestService {

    private final EmbeddingClient embeddingClient;

    private final KnowledgeChunkService knowledgeChunkService;

    @Override
    public List<Long> ingest(TextIngestDTO ingestDTO) {
        List<String> chunkList = TextPreprocessUtil.splitText(ingestDTO.getContent());
        if (CollUtil.isEmpty(chunkList)) {
            log.warn("文本切片为空，tenantId={}，knowledgeBaseId={}，documentId={}",
                    ingestDTO.getTenantId(), ingestDTO.getKnowledgeBaseId(), ingestDTO.getDocumentId());
            return List.of();
        }

        List<List<Float>> embeddingList = embeddingClient.embedBatch(chunkList);
        if (embeddingList.size() != chunkList.size()) {
            log.error("向量生成数量与文本片段数量不一致，chunkSize={}，embeddingSize={}",
                    chunkList.size(), embeddingList.size());
            throw new IllegalStateException("向量生成数量与文本片段数量不一致");
        }

        List<Long> idList = new ArrayList<>(chunkList.size());
        for (int i = 0; i < chunkList.size(); i++) {
            List<Float> embedding = embeddingList.get(i);
            checkDimension(embedding);

            KnowledgeChunkCreateDTO createDTO = new KnowledgeChunkCreateDTO();
            createDTO.setTenantId(ingestDTO.getTenantId());
            createDTO.setKnowledgeBaseId(ingestDTO.getKnowledgeBaseId());
            createDTO.setDocumentId(ingestDTO.getDocumentId());
            createDTO.setChunkIndex(i);
            createDTO.setTitle(ingestDTO.getTitle());
            createDTO.setContent(chunkList.get(i));
            createDTO.setEmbeddingModel(embeddingClient.modelName());
            createDTO.setEmbeddingVersion(embeddingClient.modelVersion());
            createDTO.setEmbedding(embedding);

            Long chunkId = knowledgeChunkService.createChunk(createDTO);
            idList.add(chunkId);
        }

        log.info("文本向量入库完成，tenantId={}，knowledgeBaseId={}，documentId={}，片段数量={}",
                ingestDTO.getTenantId(), ingestDTO.getKnowledgeBaseId(), ingestDTO.getDocumentId(), idList.size());
        return idList;
    }

    private void checkDimension(List<Float> embedding) {
        if (CollUtil.isEmpty(embedding)) {
            throw new IllegalArgumentException("向量数据不能为空");
        }
        if (embedding.size() != embeddingClient.dimension()) {
            throw new IllegalArgumentException("向量维度不正确，期望维度="
                    + embeddingClient.dimension() + "，实际维度=" + embedding.size());
        }
    }
}
```

入库流程需要注意：

1. 文本切片后，每个片段都需要保留 `document_id` 和 `chunk_index`。
2. 向量入库时必须记录模型名称和模型版本。
3. 如果模型升级，不建议覆盖旧向量，建议新建版本或重新生成全部向量。
4. 如果文档重新导入，需要考虑先删除旧片段，再写入新片段。
5. 批量处理大文档时，建议异步执行，并记录处理状态。
6. 向量维度必须在入库前校验，不能等数据库报错后再处理。

## 查询接口设计

本节用于说明 pgvector 项目对外提供的查询接口。查询接口通常分为三类：向量检索接口、文本检索接口和混合查询接口。向量检索接口适合内部调用和调试；文本检索接口适合业务使用；混合查询接口适合结合关键词、分类、权限、状态等条件的复杂检索场景。

推荐接口设计如下：

| 接口类型     | 接口路径                         | 说明                               |
| ------------ | -------------------------------- | ---------------------------------- |
| 向量检索接口 | `POST /api/vector/search`        | 调用方直接传入查询向量             |
| 文本检索接口 | `POST /api/vector/search/text`   | 调用方传入文本，后端生成向量并检索 |
| 混合查询接口 | `POST /api/vector/search/hybrid` | 同时支持文本、关键词和业务过滤条件 |

### 向量检索接口

向量检索接口用于接收查询向量，然后直接执行 pgvector 相似度检索。该接口适合内部系统、测试工具、调试页面或已经生成向量的上游服务调用。

接口说明如下：

| 项目     | 内容                                       |
| -------- | ------------------------------------------ |
| 请求方式 | `POST`                                     |
| 接口路径 | `/api/vector/search`                       |
| 请求类型 | `application/json`                         |
| 查询方式 | 直接传入查询向量                           |
| 适用场景 | 内部调试、模型服务联调、上游系统已生成向量 |

请求参数：

| 参数              | 类型          | 必填 | 说明       |
| ----------------- | ------------- | ---- | ---------- |
| `tenantId`        | `Long`        | 是   | 租户 ID    |
| `knowledgeBaseId` | `Long`        | 是   | 知识库 ID  |
| `queryEmbedding`  | `List<Float>` | 是   | 查询向量   |
| `topK`            | `Integer`     | 是   | 返回数量   |
| `minSimilarity`   | `Double`      | 否   | 最小相似度 |

请求示例：

```json
{
  "tenantId": 1,
  "knowledgeBaseId": 1001,
  "queryEmbedding": [0.10, 0.20, 0.30],
  "topK": 5,
  "minSimilarity": 0.7
}
```

返回示例：

```json
[
  {
    "id": 1,
    "documentId": 2001,
    "chunkIndex": 0,
    "title": "密码重置说明",
    "content": "用户忘记密码后，可以通过手机号验证码或邮箱链接重置密码。",
    "distance": 0.12,
    "similarity": 0.88
  }
]
```

Controller 示例代码如下。

文件位置：`src/main/java/io/github/atengk/vector/controller/VectorSearchController.java`

```java
package io.github.atengk.vector.controller;

import io.github.atengk.vector.dto.HybridSearchDTO;
import io.github.atengk.vector.dto.TextSearchDTO;
import io.github.atengk.vector.dto.VectorSearchDTO;
import io.github.atengk.vector.service.VectorQueryService;
import io.github.atengk.vector.vo.KnowledgeChunkSearchVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 向量查询接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vector/search")
public class VectorSearchController {

    private final VectorQueryService vectorQueryService;

    @PostMapping
    public List<KnowledgeChunkSearchVO> searchByVector(@Valid @RequestBody VectorSearchDTO searchDTO) {
        return vectorQueryService.searchByVector(searchDTO);
    }

    @PostMapping("/text")
    public List<KnowledgeChunkSearchVO> searchByText(@Valid @RequestBody TextSearchDTO searchDTO) {
        return vectorQueryService.searchByText(searchDTO);
    }

    @PostMapping("/hybrid")
    public List<KnowledgeChunkSearchVO> hybridSearch(@Valid @RequestBody HybridSearchDTO searchDTO) {
        return vectorQueryService.hybridSearch(searchDTO);
    }
}
```

### 文本检索接口

文本检索接口是业务系统最常用的接口。调用方只需要传入用户搜索文本，后端负责调用 Embedding 模型生成查询向量，再基于 pgvector 查询相似片段。

接口说明如下：

| 项目     | 内容                           |
| -------- | ------------------------------ |
| 请求方式 | `POST`                         |
| 接口路径 | `/api/vector/search/text`      |
| 请求类型 | `application/json`             |
| 查询方式 | 传入文本，由后端生成查询向量   |
| 适用场景 | 语义搜索、知识库问答、RAG 召回 |

请求 DTO 如下。

文件位置：`src/main/java/io/github/atengk/vector/dto/TextSearchDTO.java`

```java
package io.github.atengk.vector.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文本语义检索参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class TextSearchDTO {

    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    @NotNull(message = "知识库ID不能为空")
    private Long knowledgeBaseId;

    @NotBlank(message = "查询文本不能为空")
    private String queryText;

    @NotNull(message = "返回数量不能为空")
    @Min(value = 1, message = "返回数量不能小于1")
    @Max(value = 50, message = "返回数量不能大于50")
    private Integer topK;

    private Double minSimilarity;
}
```

请求示例：

```json
{
  "tenantId": 1,
  "knowledgeBaseId": 1001,
  "queryText": "忘记密码后怎么重置",
  "topK": 5,
  "minSimilarity": 0.7
}
```

curl 调用示例：

```bash
curl -X POST 'http://127.0.0.1:8080/api/vector/search/text' \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": 1,
    "knowledgeBaseId": 1001,
    "queryText": "忘记密码后怎么重置",
    "topK": 5,
    "minSimilarity": 0.7
  }'
```

文本检索的核心 Service 如下。

文件位置：`src/main/java/io/github/atengk/vector/service/VectorQueryService.java`

```java
package io.github.atengk.vector.service;

import io.github.atengk.vector.dto.HybridSearchDTO;
import io.github.atengk.vector.dto.TextSearchDTO;
import io.github.atengk.vector.dto.VectorSearchDTO;
import io.github.atengk.vector.vo.KnowledgeChunkSearchVO;

import java.util.List;

/**
 * 向量查询服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface VectorQueryService {

    List<KnowledgeChunkSearchVO> searchByVector(VectorSearchDTO searchDTO);

    List<KnowledgeChunkSearchVO> searchByText(TextSearchDTO searchDTO);

    List<KnowledgeChunkSearchVO> hybridSearch(HybridSearchDTO searchDTO);
}
```

文件位置：`src/main/java/io/github/atengk/vector/service/impl/VectorQueryServiceImpl.java`

```java
package io.github.atengk.vector.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.vector.client.EmbeddingClient;
import io.github.atengk.vector.dto.HybridSearchDTO;
import io.github.atengk.vector.dto.TextSearchDTO;
import io.github.atengk.vector.dto.VectorSearchDTO;
import io.github.atengk.vector.mapper.KnowledgeChunkMapper;
import io.github.atengk.vector.service.KnowledgeChunkService;
import io.github.atengk.vector.service.VectorQueryService;
import io.github.atengk.vector.util.VectorConvertUtil;
import io.github.atengk.vector.vo.KnowledgeChunkSearchVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 向量查询服务实现
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorQueryServiceImpl implements VectorQueryService {

    private final EmbeddingClient embeddingClient;

    private final KnowledgeChunkService knowledgeChunkService;

    private final KnowledgeChunkMapper knowledgeChunkMapper;

    @Override
    public List<KnowledgeChunkSearchVO> searchByVector(VectorSearchDTO searchDTO) {
        return knowledgeChunkService.search(searchDTO);
    }

    @Override
    public List<KnowledgeChunkSearchVO> searchByText(TextSearchDTO searchDTO) {
        String queryText = StrUtil.trim(searchDTO.getQueryText());
        List<Float> queryEmbedding = embeddingClient.embed(queryText);

        VectorSearchDTO vectorSearchDTO = new VectorSearchDTO();
        vectorSearchDTO.setTenantId(searchDTO.getTenantId());
        vectorSearchDTO.setKnowledgeBaseId(searchDTO.getKnowledgeBaseId());
        vectorSearchDTO.setQueryEmbedding(queryEmbedding);
        vectorSearchDTO.setTopK(searchDTO.getTopK());
        vectorSearchDTO.setMinSimilarity(searchDTO.getMinSimilarity());

        List<KnowledgeChunkSearchVO> resultList = knowledgeChunkService.search(vectorSearchDTO);
        log.info("文本语义检索完成，tenantId={}，knowledgeBaseId={}，queryText={}，命中数量={}",
                searchDTO.getTenantId(), searchDTO.getKnowledgeBaseId(), queryText, resultList.size());
        return resultList;
    }

    @Override
    public List<KnowledgeChunkSearchVO> hybridSearch(HybridSearchDTO searchDTO) {
        String queryText = StrUtil.trim(searchDTO.getQueryText());
        List<Float> queryEmbedding = embeddingClient.embed(queryText);
        String queryEmbeddingText = VectorConvertUtil.toPgVectorString(queryEmbedding);

        List<KnowledgeChunkSearchVO> resultList = knowledgeChunkMapper.hybridSearch(
                searchDTO.getTenantId(),
                searchDTO.getKnowledgeBaseId(),
                queryEmbeddingText,
                searchDTO.getKeyword(),
                searchDTO.getTopK(),
                searchDTO.getMinSimilarity()
        );

        if (CollUtil.isEmpty(resultList)) {
            log.info("混合查询未命中数据，tenantId={}，knowledgeBaseId={}，queryText={}，keyword={}",
                    searchDTO.getTenantId(), searchDTO.getKnowledgeBaseId(), queryText, searchDTO.getKeyword());
            return List.of();
        }

        log.info("混合查询完成，tenantId={}，knowledgeBaseId={}，命中数量={}",
                searchDTO.getTenantId(), searchDTO.getKnowledgeBaseId(), resultList.size());
        return resultList;
    }
}
```

### 混合查询接口

混合查询接口用于同时支持语义检索和业务条件过滤。它通常用于生产检索场景，例如在指定租户、指定知识库、指定关键词或指定状态下查询语义最相近的内容。

接口说明如下：

| 项目     | 内容                                           |
| -------- | ---------------------------------------------- |
| 请求方式 | `POST`                                         |
| 接口路径 | `/api/vector/search/hybrid`                    |
| 请求类型 | `application/json`                             |
| 查询方式 | 文本生成向量，再叠加关键词或业务条件过滤       |
| 适用场景 | 知识库搜索、后台检索、RAG 召回、多条件语义查询 |

请求 DTO 如下。

文件位置：`src/main/java/io/github/atengk/vector/dto/HybridSearchDTO.java`

```java
package io.github.atengk.vector.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 混合查询参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class HybridSearchDTO {

    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    @NotNull(message = "知识库ID不能为空")
    private Long knowledgeBaseId;

    @NotBlank(message = "查询文本不能为空")
    private String queryText;

    private String keyword;

    @NotNull(message = "返回数量不能为空")
    @Min(value = 1, message = "返回数量不能小于1")
    @Max(value = 50, message = "返回数量不能大于50")
    private Integer topK;

    private Double minSimilarity;
}
```

需要在 `KnowledgeChunkMapper` 中增加混合查询方法。

文件位置：`src/main/java/io/github/atengk/vector/mapper/KnowledgeChunkMapper.java`

```java
package io.github.atengk.vector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.vector.entity.KnowledgeChunkEntity;
import io.github.atengk.vector.vo.KnowledgeChunkSearchVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识片段向量 Mapper
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Mapper
public interface KnowledgeChunkMapper extends BaseMapper<KnowledgeChunkEntity> {

    int insertChunk(KnowledgeChunkEntity entity);

    List<KnowledgeChunkSearchVO> searchByVector(@Param("tenantId") Long tenantId,
                                                @Param("knowledgeBaseId") Long knowledgeBaseId,
                                                @Param("queryEmbedding") String queryEmbedding,
                                                @Param("topK") Integer topK);

    List<KnowledgeChunkSearchVO> searchByVectorWithThreshold(@Param("tenantId") Long tenantId,
                                                             @Param("knowledgeBaseId") Long knowledgeBaseId,
                                                             @Param("queryEmbedding") String queryEmbedding,
                                                             @Param("minSimilarity") Double minSimilarity,
                                                             @Param("topK") Integer topK);

    List<KnowledgeChunkSearchVO> hybridSearch(@Param("tenantId") Long tenantId,
                                              @Param("knowledgeBaseId") Long knowledgeBaseId,
                                              @Param("queryEmbedding") String queryEmbedding,
                                              @Param("keyword") String keyword,
                                              @Param("topK") Integer topK,
                                              @Param("minSimilarity") Double minSimilarity);
}
```

需要在 `KnowledgeChunkMapper.xml` 中增加混合查询 SQL。

文件位置：`src/main/resources/mapper/KnowledgeChunkMapper.xml`

```xml
<!-- 混合查询：语义向量检索 + 关键词过滤 + 相似度阈值 -->
<select id="hybridSearch" resultMap="KnowledgeChunkSearchMap">
    SELECT
        t.id,
        t.document_id,
        t.chunk_index,
        t.title,
        t.content,
        t.distance,
        t.similarity
    FROM (
        SELECT
            id,
            document_id,
            chunk_index,
            title,
            content,
            embedding &lt;=&gt; #{queryEmbedding}::vector AS distance,
            1 - (embedding &lt;=&gt; #{queryEmbedding}::vector) AS similarity
        FROM knowledge_chunk
        WHERE tenant_id = #{tenantId}
          AND knowledge_base_id = #{knowledgeBaseId}
          AND status = 1
          AND embedding IS NOT NULL
          <if test="keyword != null and keyword != ''">
              AND (
                  title ILIKE CONCAT('%', #{keyword}, '%')
                  OR content ILIKE CONCAT('%', #{keyword}, '%')
              )
          </if>
        ORDER BY embedding &lt;=&gt; #{queryEmbedding}::vector
        LIMIT #{topK}
    ) t
    WHERE 1 = 1
    <if test="minSimilarity != null">
        AND t.similarity &gt;= #{minSimilarity}
    </if>
    ORDER BY t.distance
</select>
```

请求示例：

```json
{
  "tenantId": 1,
  "knowledgeBaseId": 1001,
  "queryText": "用户无法登录系统",
  "keyword": "密码",
  "topK": 10,
  "minSimilarity": 0.65
}
```

curl 调用示例：

```bash
curl -X POST 'http://127.0.0.1:8080/api/vector/search/hybrid' \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": 1,
    "knowledgeBaseId": 1001,
    "queryText": "用户无法登录系统",
    "keyword": "密码",
    "topK": 10,
    "minSimilarity": 0.65
  }'
```

混合查询接口需要注意：

1. `queryText` 用于生成查询向量，影响语义召回结果。
2. `keyword` 用于普通字段过滤，适合限定标题或正文中必须出现的关键词。
3. `minSimilarity` 用于过滤低相关结果，阈值需要通过测试数据调优。
4. 如果 `keyword` 过滤过强，可能导致语义相关但不包含关键词的数据无法返回。
5. 如果用于 RAG 场景，建议接口返回结果后再按 token 长度进行上下文截断。
6. 生产环境中可以继续扩展 `documentType`、`categoryId`、`sourceType`、`createTimeRange` 等过滤条件。



## 性能优化

本节用于说明 pgvector 在生产项目中的性能优化方式。优化重点包括向量索引选择、查询参数调整、业务过滤条件优化、批量写入策略以及数据量增长后的表结构和运维处理。pgvector 默认执行精确最近邻搜索，召回结果最准确；添加 HNSW 或 IVFFlat 近似索引后，可以提升查询速度，但会在召回率和性能之间做权衡。([GitHub](https://github.com/pgvector/pgvector?utm_source=chatgpt.com))

### 向量索引优化

向量索引优化的核心是根据数据规模、查询延迟、召回率要求和写入频率选择合适的索引类型。pgvector 支持 HNSW 和 IVFFlat 两类近似索引，其中 HNSW 通常有更好的查询性能和速度-召回权衡，但构建更慢、内存占用更高；IVFFlat 构建更快、内存占用更低，但查询性能和召回表现通常弱于 HNSW。([GitHub](https://github.com/pgvector/pgvector?utm_source=chatgpt.com))

索引选择建议如下：

| 场景               | 推荐方式                  | 说明                                           |
| ------------------ | ------------------------- | ---------------------------------------------- |
| 原型验证           | 不创建向量索引            | 使用精确检索，便于验证召回结果是否正确         |
| 小数据量           | 不创建向量索引或使用 HNSW | 数据量较小时，精确检索也可以满足性能要求       |
| 中等数据量在线检索 | HNSW                      | 查询性能较好，适合语义搜索、RAG 召回           |
| 批量导入后检索     | IVFFlat 或 HNSW           | 数据稳定后再创建索引，减少导入期间索引维护成本 |
| 高写入频率         | 谨慎使用复杂索引          | 写入、更新、删除都需要维护索引                 |
| 多业务条件过滤     | 普通索引 + 向量索引       | 业务字段过滤和向量排序需要分别优化             |

如果文本语义检索使用余弦距离，可以创建 HNSW 索引：

```sql
-- 使用 HNSW 创建余弦距离索引
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_knowledge_chunk_embedding_hnsw
ON knowledge_chunk
USING hnsw (embedding vector_cosine_ops);
```

如果使用 L2 距离，可以创建 L2 对应的索引：

```sql
-- 使用 HNSW 创建 L2 距离索引
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_knowledge_chunk_embedding_l2_hnsw
ON knowledge_chunk
USING hnsw (embedding vector_l2_ops);
```

如果选择 IVFFlat，需要在表中已有一定数据后再创建索引。pgvector 官方建议 IVFFlat 的 `lists` 参数在 100 万行以内可以从 `rows / 1000` 开始，超过 100 万行可以从 `sqrt(rows)` 开始；查询时 `probes` 可以从 `sqrt(lists)` 开始调优。([GitHub](https://github.com/pgvector/pgvector?utm_source=chatgpt.com))

```sql
-- 使用 IVFFlat 创建余弦距离索引，适合数据稳定后创建
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_knowledge_chunk_embedding_ivfflat
ON knowledge_chunk
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);
```

业务过滤字段仍然需要普通索引。向量索引用于优化相似度排序，不能替代 `tenant_id`、`knowledge_base_id`、`status` 等字段的过滤索引。pgvector 官方也建议对于带 `WHERE` 条件的近邻查询，可以先从过滤字段索引开始优化。([GitHub](https://github.com/pgvector/pgvector?utm_source=chatgpt.com))

```sql
-- 多租户和知识库过滤索引
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_tenant_kb_status
ON knowledge_chunk (tenant_id, knowledge_base_id, status);

-- 文档片段查询索引
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_document_id
ON knowledge_chunk (document_id);

-- 模型版本过滤索引
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_model_version
ON knowledge_chunk (embedding_model, embedding_version);

-- 时间范围查询索引
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_create_time
ON knowledge_chunk (create_time);
```

生产环境中建议使用 `CREATE INDEX CONCURRENTLY` 创建索引，避免长时间阻塞写入。pgvector 官方性能建议中也提到，生产环境创建索引时可以使用并发创建方式。([GitHub](https://github.com/pgvector/pgvector?utm_source=chatgpt.com))

### 查询参数调优

查询参数调优主要用于平衡查询速度和召回质量。不同索引类型有不同的查询参数：HNSW 主要关注 `hnsw.ef_search`，IVFFlat 主要关注 `ivfflat.probes`。参数越大，通常召回率越高，但查询耗时也会增加。pgvector 官方说明中提到，HNSW 查询结果会受到动态候选列表 `hnsw.ef_search` 的限制，其默认值为 40；IVFFlat 查询结果会受到 `ivfflat.probes` 的影响。([GitHub](https://github.com/pgvector/pgvector?utm_source=chatgpt.com))

HNSW 查询参数示例：

```sql
-- 当前会话设置 HNSW 查询候选集大小
SET hnsw.ef_search = 100;

-- 执行向量检索
SELECT
    id,
    title,
    content,
    embedding <=> '[0.10,0.20,0.30]'::vector AS distance
FROM knowledge_chunk
WHERE tenant_id = 1
  AND knowledge_base_id = 1001
  AND status = 1
  AND embedding IS NOT NULL
ORDER BY embedding <=> '[0.10,0.20,0.30]'::vector
LIMIT 10;
```

IVFFlat 查询参数示例：

```sql
-- 当前会话设置 IVFFlat probes 数量
SET ivfflat.probes = 10;

-- 执行向量检索
SELECT
    id,
    title,
    content,
    embedding <=> '[0.10,0.20,0.30]'::vector AS distance
FROM knowledge_chunk
WHERE tenant_id = 1
  AND knowledge_base_id = 1001
  AND status = 1
  AND embedding IS NOT NULL
ORDER BY embedding <=> '[0.10,0.20,0.30]'::vector
LIMIT 10;
```

查询性能分析建议使用 `EXPLAIN (ANALYZE, BUFFERS)` 查看真实执行计划、耗时和缓冲区命中情况。pgvector 官方也建议使用该命令调试查询性能。([GitHub](https://github.com/pgvector/pgvector?utm_source=chatgpt.com))

```sql
-- 分析向量查询执行计划和真实耗时
EXPLAIN (ANALYZE, BUFFERS)
SELECT
    id,
    title,
    content,
    embedding <=> '[0.10,0.20,0.30]'::vector AS distance
FROM knowledge_chunk
WHERE tenant_id = 1
  AND knowledge_base_id = 1001
  AND status = 1
  AND embedding IS NOT NULL
ORDER BY embedding <=> '[0.10,0.20,0.30]'::vector
LIMIT 10;
```

常见调优方向如下：

| 问题             | 可能原因                 | 优化方式                                                 |
| ---------------- | ------------------------ | -------------------------------------------------------- |
| 查询慢           | 未创建向量索引           | 创建 HNSW 或 IVFFlat 索引                                |
| 查询慢           | 业务过滤字段无索引       | 给 `tenant_id`、`knowledge_base_id`、`status` 建普通索引 |
| 召回数量少       | `hnsw.ef_search` 过小    | 适当增大 `hnsw.ef_search`                                |
| IVFFlat 召回差   | `probes` 过小            | 适当增大 `ivfflat.probes`                                |
| IVFFlat 效果差   | 空表或少量数据时创建索引 | 删除索引，等数据量稳定后重建                             |
| 排序耗时高       | `LIMIT` 过大             | 限制 `topK`，避免一次返回过多结果                        |
| 结果不相关       | 切片或模型问题           | 优化文本切片、Embedding 模型和相似度算法                 |
| 条件过滤后结果少 | 过滤条件过强             | 扩大召回范围或调整关键词过滤策略                         |

对于 RAG 场景，`topK` 不宜盲目设置过大。更大的 `topK` 会增加数据库查询成本、网络传输成本和后续大模型上下文成本。一般建议先从 3 到 8 开始调试，再结合回答质量和上下文长度调整。

### 数据量增长处理

当数据量从几万增长到几十万、几百万甚至更多时，需要从写入、索引、存储、归档和分区等方面处理。pgvector 官方性能建议中提到，批量加载时可以使用 `COPY`，并且初始数据加载完成后再创建索引通常性能更好。([GitHub](https://github.com/pgvector/pgvector?utm_source=chatgpt.com))

批量导入建议流程如下：

```text
准备原始数据
  ↓
清洗文本
  ↓
批量生成 Embedding
  ↓
使用 COPY 或批量 INSERT 写入数据库
  ↓
写入完成后创建向量索引
  ↓
执行 ANALYZE 更新统计信息
  ↓
验证查询性能
```

批量导入后执行统计信息更新：

```sql
-- 更新表统计信息，帮助优化器选择更合适的执行计划
ANALYZE knowledge_chunk;
```

如果初始数据量较大，建议先导入数据，再创建索引：

```sql
-- 先完成数据导入，再创建业务字段索引
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_tenant_kb_status
ON knowledge_chunk (tenant_id, knowledge_base_id, status);

-- 再创建向量索引
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_knowledge_chunk_embedding_hnsw
ON knowledge_chunk
USING hnsw (embedding vector_cosine_ops);

-- 最后更新统计信息
ANALYZE knowledge_chunk;
```

数据量增长后的处理建议如下：

| 方向     | 建议                                       |
| -------- | ------------------------------------------ |
| 数据写入 | 使用批量写入，避免逐条提交事务             |
| 初始导入 | 先导入数据，再创建向量索引                 |
| 查询范围 | 必须带租户、知识库、状态等过滤条件         |
| 分区策略 | 大规模多租户或按时间管理的数据可考虑分区表 |
| 模型升级 | 不建议直接覆盖原向量，建议按模型版本重建   |
| 数据归档 | 对禁用、过期、历史版本数据做归档或物理清理 |
| 索引维护 | 大量删除或更新后执行 `VACUUM`、`REINDEX`   |
| 性能分析 | 定期使用 `EXPLAIN ANALYZE` 分析核心查询    |

对于 HNSW 索引，pgvector 官方 FAQ 中提到，Vacuum HNSW 索引可能需要较长时间，可以先并发重建索引，再执行 Vacuum。([GitHub](https://github.com/pgvector/pgvector?utm_source=chatgpt.com))

```sql
-- 大量更新或删除后，可以先重建索引再清理表
REINDEX INDEX CONCURRENTLY idx_knowledge_chunk_embedding_hnsw;

-- 清理表中的无效行版本
VACUUM ANALYZE knowledge_chunk;
```

如果数据按租户或知识库强隔离，并且单表数据规模持续增长，可以考虑 PostgreSQL 分区表。例如按 `knowledge_base_id` 或时间分区，但是否分区需要结合查询模式、写入模式和运维复杂度综合评估。

## 功能验证

本节用于说明如何验证 pgvector 项目是否可以正常运行。验证过程包括初始化测试数据、执行 SQL 相似度查询、调用 Spring Boot 接口以及检查返回结果。功能验证建议先使用低维向量，例如 `vector(3)`，确认流程正确后再切换到正式 Embedding 模型维度。

### 初始化测试数据

初始化测试数据用于验证 pgvector 扩展、表结构、向量写入和基础检索是否正常。为了便于手工测试，以下示例使用 3 维向量。如果正式环境使用 768、1024 或 1536 维向量，需要将表字段和测试向量统一调整为对应维度。

```sql
-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 删除旧测试表，避免重复执行脚本时报错
DROP TABLE IF EXISTS knowledge_chunk;

-- 创建知识片段测试表
CREATE TABLE knowledge_chunk (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    knowledge_base_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    chunk_index INTEGER NOT NULL,
    title VARCHAR(255),
    content TEXT NOT NULL,
    embedding_model VARCHAR(100) NOT NULL,
    embedding_version VARCHAR(50) NOT NULL DEFAULT 'v1',
    embedding vector(3),
    status SMALLINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 写入测试数据
INSERT INTO knowledge_chunk (
    tenant_id,
    knowledge_base_id,
    document_id,
    chunk_index,
    title,
    content,
    embedding_model,
    embedding_version,
    embedding,
    status
) VALUES
(
    1,
    1001,
    2001,
    0,
    '密码重置',
    '用户忘记密码后，可以通过手机号验证码或邮箱链接重置密码。',
    'mock-embedding-model',
    'v1',
    '[0.10,0.20,0.30]'::vector,
    1
),
(
    1,
    1001,
    2002,
    0,
    '账号登录',
    '用户无法登录系统时，需要检查账号状态、密码和验证码。',
    'mock-embedding-model',
    'v1',
    '[0.11,0.21,0.29]'::vector,
    1
),
(
    1,
    1001,
    2003,
    0,
    '订单退款',
    '订单支付成功后，在满足退款条件时可以发起退款申请。',
    'mock-embedding-model',
    'v1',
    '[0.80,0.70,0.60]'::vector,
    1
),
(
    1,
    1002,
    2004,
    0,
    '发票申请',
    '用户可以在订单完成后申请电子发票。',
    'mock-embedding-model',
    'v1',
    '[0.75,0.65,0.55]'::vector,
    1
);

-- 创建业务过滤索引
CREATE INDEX idx_knowledge_chunk_tenant_kb_status
ON knowledge_chunk (tenant_id, knowledge_base_id, status);
```

如果需要验证 HNSW 索引，可以继续执行：

```sql
-- 创建 HNSW 余弦距离索引
CREATE INDEX idx_knowledge_chunk_embedding_hnsw
ON knowledge_chunk
USING hnsw (embedding vector_cosine_ops);

-- 更新统计信息
ANALYZE knowledge_chunk;
```

初始化完成后，确认数据是否写入成功：

```sql
-- 查看测试数据
SELECT
    id,
    title,
    content,
    embedding
FROM knowledge_chunk
ORDER BY id;
```

### 相似度查询验证

相似度查询验证用于确认 pgvector 距离计算是否符合预期。以下示例使用余弦距离 `<=>` 查询与 `[0.10,0.20,0.30]` 最相似的片段。pgvector 支持多种距离操作符，包括 L2 距离 `<->`、内积 `<#>`、余弦距离 `<=>` 和 L1 距离 `<+>`。([GitHub](https://github.com/pgvector/pgvector?utm_source=chatgpt.com))

```sql
-- 使用余弦距离查询最相似的 Top 3 数据
SELECT
    id,
    title,
    content,
    embedding <=> '[0.10,0.20,0.30]'::vector AS distance,
    1 - (embedding <=> '[0.10,0.20,0.30]'::vector) AS similarity
FROM knowledge_chunk
WHERE tenant_id = 1
  AND knowledge_base_id = 1001
  AND status = 1
  AND embedding IS NOT NULL
ORDER BY embedding <=> '[0.10,0.20,0.30]'::vector
LIMIT 3;
```

预期结果中，`密码重置` 和 `账号登录` 应该排在靠前位置，因为它们的测试向量更接近查询向量。

如果需要验证业务过滤是否生效，可以切换 `knowledge_base_id`：

```sql
-- 验证知识库过滤条件是否生效
SELECT
    id,
    knowledge_base_id,
    title,
    embedding <=> '[0.10,0.20,0.30]'::vector AS distance
FROM knowledge_chunk
WHERE tenant_id = 1
  AND knowledge_base_id = 1002
  AND status = 1
  AND embedding IS NOT NULL
ORDER BY embedding <=> '[0.10,0.20,0.30]'::vector
LIMIT 3;
```

验证执行计划：

```sql
-- 查看查询执行计划和耗时
EXPLAIN (ANALYZE, BUFFERS)
SELECT
    id,
    title,
    content,
    embedding <=> '[0.10,0.20,0.30]'::vector AS distance
FROM knowledge_chunk
WHERE tenant_id = 1
  AND knowledge_base_id = 1001
  AND status = 1
  AND embedding IS NOT NULL
ORDER BY embedding <=> '[0.10,0.20,0.30]'::vector
LIMIT 3;
```

验证时重点关注以下内容：

| 验证项             | 判断方式                           |
| ------------------ | ---------------------------------- |
| 扩展是否可用       | `CREATE EXTENSION vector` 执行成功 |
| 向量是否写入       | `embedding` 字段有值               |
| 维度是否一致       | 插入数据时没有维度错误             |
| 排序是否正确       | 相似向量排在前面                   |
| 过滤是否生效       | 不同租户或知识库结果隔离           |
| 索引是否使用       | `EXPLAIN` 中查看执行计划           |
| 查询耗时是否可接受 | 查看 `Execution Time`              |

### 接口调用验证

接口调用验证用于确认 Spring Boot 应用能正常连接 PostgreSQL，并完成向量写入、文本入库、向量检索和文本检索。验证前需要先启动数据库，再启动 Spring Boot 应用。

启动应用：

```bash
# 在项目根目录执行
mvn spring-boot:run
```

验证应用健康状态。如果项目已引入 Actuator，可以使用健康检查接口；如果没有引入 Actuator，可以直接调用业务接口验证。

```bash
# 验证服务端口是否可访问
curl -i 'http://127.0.0.1:8080/api/vector/search'
```

写入知识片段向量：

```bash
# 写入一条知识片段向量
curl -X POST 'http://127.0.0.1:8080/api/vector/chunks' \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": 1,
    "knowledgeBaseId": 1001,
    "documentId": 3001,
    "chunkIndex": 0,
    "title": "登录失败处理",
    "content": "用户登录失败时，需要检查账号状态、密码是否正确以及验证码是否过期。",
    "embeddingModel": "mock-embedding-model",
    "embeddingVersion": "v1",
    "embedding": [0.12, 0.20, 0.28]
  }'
```

调用向量检索接口：

```bash
# 直接传入查询向量进行检索
curl -X POST 'http://127.0.0.1:8080/api/vector/search' \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": 1,
    "knowledgeBaseId": 1001,
    "queryEmbedding": [0.10, 0.20, 0.30],
    "topK": 5,
    "minSimilarity": 0.6
  }'
```

调用文本检索接口：

```bash
# 传入查询文本，由后端生成向量后检索
curl -X POST 'http://127.0.0.1:8080/api/vector/search/text' \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": 1,
    "knowledgeBaseId": 1001,
    "queryText": "用户忘记密码怎么处理",
    "topK": 5,
    "minSimilarity": 0.6
  }'
```

调用混合查询接口：

```bash
# 文本语义检索叠加关键词过滤
curl -X POST 'http://127.0.0.1:8080/api/vector/search/hybrid' \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": 1,
    "knowledgeBaseId": 1001,
    "queryText": "用户无法登录系统",
    "keyword": "密码",
    "topK": 10,
    "minSimilarity": 0.6
  }'
```

预期返回格式如下：

```json
[
  {
    "id": 1,
    "documentId": 2001,
    "chunkIndex": 0,
    "title": "密码重置",
    "content": "用户忘记密码后，可以通过手机号验证码或邮箱链接重置密码。",
    "distance": 0.05,
    "similarity": 0.95
  }
]
```

接口验证时重点检查：

| 验证项     | 说明                                       |
| ---------- | ------------------------------------------ |
| 数据库连接 | 应用启动时无连接异常                       |
| Mapper XML | 应用启动时无 XML 解析异常                  |
| 向量写入   | `embedding` 能正常写入数据库               |
| XML 转义   | `<=>` 在 XML 中必须写成 `<=>`              |
| 参数校验   | 空文本、空向量、非法 `topK` 应返回校验错误 |
| 查询结果   | 返回内容与测试数据语义或向量距离一致       |
| 日志输出   | 能看到写入、检索、未命中、异常等关键日志   |

## 项目部署

本节用于说明 pgvector 项目的部署过程，包括数据库初始化脚本、扩展安装检查和应用启动验证。部署时需要保证 PostgreSQL 数据库、pgvector 扩展、表结构、索引、应用配置和模型服务配置一致。

### 数据库初始化脚本

数据库初始化脚本用于创建扩展、业务表、索引和基础测试数据。生产环境中建议将初始化脚本纳入 Flyway、Liquibase 或公司内部发布流程，避免手工执行导致环境不一致。

文件位置：`sql/init-pgvector.sql`

```sql
-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 创建文档主表
CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    knowledge_base_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_value VARCHAR(500),
    process_status SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建知识片段向量表
CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    knowledge_base_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    chunk_index INTEGER NOT NULL,
    title VARCHAR(255),
    content TEXT NOT NULL,
    embedding_model VARCHAR(100) NOT NULL,
    embedding_version VARCHAR(50) NOT NULL DEFAULT 'v1',
    embedding vector(1536),
    status SMALLINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建业务过滤索引
CREATE INDEX IF NOT EXISTS idx_knowledge_document_tenant_kb
ON knowledge_document (tenant_id, knowledge_base_id);

-- 创建业务过滤索引
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_tenant_kb_status
ON knowledge_chunk (tenant_id, knowledge_base_id, status);

-- 创建文档片段关联索引
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_document_id
ON knowledge_chunk (document_id);

-- 创建模型版本索引
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_model_version
ON knowledge_chunk (embedding_model, embedding_version);

-- 创建 HNSW 向量索引
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_embedding_hnsw
ON knowledge_chunk
USING hnsw (embedding vector_cosine_ops);

-- 更新统计信息
ANALYZE knowledge_document;
ANALYZE knowledge_chunk;
```

如果本地开发使用 Mock 3 维向量，需要将正式脚本中的 `embedding vector(1536)` 改为：

```sql
-- 本地 Mock 测试使用 3 维向量
embedding vector(3)
```

使用 Docker 部署 PostgreSQL + pgvector：

```bash
# 启动 PostgreSQL + pgvector 容器
docker run -d \
  --name postgres-pgvector \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=123456 \
  -e POSTGRES_DB=vector_db \
  -p 5432:5432 \
  -v pgvector-data:/var/lib/postgresql/data \
  pgvector/pgvector:pg16
```

执行初始化脚本：

```bash
# 将初始化脚本复制到容器
docker cp sql/init-pgvector.sql postgres-pgvector:/tmp/init-pgvector.sql

# 在容器内执行初始化脚本
docker exec -it postgres-pgvector \
  psql -U postgres -d vector_db -f /tmp/init-pgvector.sql
```

命令说明：

| 命令                        | 说明                            |
| --------------------------- | ------------------------------- |
| `docker run`                | 启动 PostgreSQL + pgvector 容器 |
| `docker cp`                 | 将本地 SQL 文件复制到容器中     |
| `docker exec`               | 在容器中执行命令                |
| `psql -U postgres`          | 使用 `postgres` 用户连接数据库  |
| `-d vector_db`              | 指定目标数据库                  |
| `-f /tmp/init-pgvector.sql` | 执行初始化 SQL 文件             |

### 扩展安装检查

扩展安装检查用于确认当前数据库是否已经启用 pgvector。如果扩展未启用，`vector` 类型、距离操作符和向量索引都无法使用。PostgreSQL 的扩展需要在目标数据库中执行 `CREATE EXTENSION` 后才能使用；pgvector 官方使用方式也是在数据库中执行 `CREATE EXTENSION vector` 启用扩展。([GitHub](https://github.com/pgvector/pgvector?utm_source=chatgpt.com))

检查扩展是否存在：

```sql
-- 查看 vector 扩展是否已启用
SELECT
    extname,
    extversion
FROM pg_extension
WHERE extname = 'vector';
```

检查 `vector` 类型是否可用：

```sql
-- 检查 vector 类型是否可用
SELECT '[1,2,3]'::vector;
```

检查距离操作符是否可用：

```sql
-- 检查 L2 距离
SELECT '[1,2,3]'::vector <-> '[1,2,4]'::vector AS l2_distance;

-- 检查余弦距离
SELECT '[1,2,3]'::vector <=> '[1,2,4]'::vector AS cosine_distance;
```

检查索引是否创建成功：

```sql
-- 查看 knowledge_chunk 表相关索引
SELECT
    indexname,
    indexdef
FROM pg_indexes
WHERE tablename = 'knowledge_chunk'
ORDER BY indexname;
```

检查表结构是否正确：

```sql
-- 查看 knowledge_chunk 表字段
SELECT
    column_name,
    data_type,
    udt_name,
    is_nullable
FROM information_schema.columns
WHERE table_name = 'knowledge_chunk'
ORDER BY ordinal_position;
```

检查当前数据库连接信息：

```sql
-- 查看当前连接的数据库和用户
SELECT
    current_database() AS database_name,
    current_user AS user_name,
    inet_server_addr() AS server_addr,
    inet_server_port() AS server_port;
```

如果扩展检查失败，可以按以下方向排查：

| 问题                                  | 排查方式                                   |
| ------------------------------------- | ------------------------------------------ |
| `type "vector" does not exist`        | 当前数据库未执行 `CREATE EXTENSION vector` |
| `extension "vector" is not available` | PostgreSQL 环境未安装 pgvector 扩展文件    |
| SQL 在一个库执行，应用连另一个库      | 检查 `current_database()` 和应用 `jdbcUrl` |
| Docker 容器重建后数据丢失             | 检查是否挂载了数据卷                       |
| 索引创建失败                          | 检查向量维度、操作符类型和表字段类型       |

### 应用启动验证

应用启动验证用于确认 Spring Boot 项目可以正常读取配置、连接数据库、加载 Mapper XML，并完成一次基础查询。部署时建议先验证数据库，再启动应用，最后调用接口。

推荐启动前检查项：

| 检查项        | 说明                                       |
| ------------- | ------------------------------------------ |
| 数据库地址    | `spring.datasource.url` 是否正确           |
| 数据库账号    | 用户名和密码是否正确                       |
| pgvector 扩展 | 目标数据库是否启用 `vector`                |
| 表结构        | `knowledge_chunk` 是否存在                 |
| 向量维度      | 表字段维度是否和模型输出一致               |
| Mapper XML    | `mapper-locations` 是否能扫描到 XML        |
| 模型配置      | Embedding 模型服务地址、密钥、维度是否正确 |
| 日志级别      | 生产环境避免输出过多 SQL 明细              |

打包应用：

```bash
# 清理并打包 Spring Boot 项目
mvn clean package -DskipTests
```

启动应用：

```bash
# 启动应用，使用默认配置
java -jar target/pgvector-demo.jar
```

如果需要指定环境配置：

```bash
# 启动应用，并指定 prod 环境
java -jar target/pgvector-demo.jar \
  --spring.profiles.active=prod
```

如果需要覆盖数据库连接配置：

```bash
# 启动应用时覆盖数据库连接参数
java -jar target/pgvector-demo.jar \
  --spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/vector_db \
  --spring.datasource.username=postgres \
  --spring.datasource.password=123456
```

启动后检查日志中是否存在以下信息：

```text
Started PgvectorDemoApplication
```

如果启动失败，可以按以下问题排查：

| 异常现象                                    | 可能原因                   | 处理方式                                |
| ------------------------------------------- | -------------------------- | --------------------------------------- |
| 数据库连接失败                              | 地址、端口、账号或密码错误 | 检查 `application.yml` 和数据库服务状态 |
| `relation "knowledge_chunk" does not exist` | 表未初始化                 | 执行初始化 SQL                          |
| `type "vector" does not exist`              | 扩展未启用                 | 执行 `CREATE EXTENSION vector`          |
| Mapper XML 解析失败                         | `<=>` 未转义               | XML 中写成 `<=>`                        |
| 向量写入失败                                | 向量维度不一致             | 检查 Embedding 模型维度和 `vector(n)`   |
| 查询结果为空                                | 数据未写入或过滤条件不匹配 | 检查租户、知识库、状态和向量字段        |
| 查询慢                                      | 缺少索引或参数不合理       | 检查业务索引、向量索引和执行计划        |

应用启动后，执行一次完整接口验证：

```bash
# 1. 写入向量数据
curl -X POST 'http://127.0.0.1:8080/api/vector/chunks' \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": 1,
    "knowledgeBaseId": 1001,
    "documentId": 9001,
    "chunkIndex": 0,
    "title": "部署验证数据",
    "content": "这是一条用于验证 pgvector 应用启动和向量检索功能的测试数据。",
    "embeddingModel": "mock-embedding-model",
    "embeddingVersion": "v1",
    "embedding": [0.10, 0.20, 0.30]
  }'

# 2. 查询相似数据
curl -X POST 'http://127.0.0.1:8080/api/vector/search' \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": 1,
    "knowledgeBaseId": 1001,
    "queryEmbedding": [0.10, 0.20, 0.30],
    "topK": 5,
    "minSimilarity": 0.6
  }'
```

如果接口返回写入数据，并且 `similarity` 分数符合预期，说明数据库扩展、表结构、Mapper、Service 和接口链路已经打通。生产环境上线前，还需要使用真实 Embedding 模型、真实向量维度和真实业务数据执行一次完整验证，避免 Mock 低维向量验证通过但正式模型维度不匹配。
