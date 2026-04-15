# PostgreSQL + pgvector

安装 pgvector 参考：[链接](https://atengk.github.io/Ateng-Linux/work/docker/service/postgresql/pgvector/README)

## 1. 简介

pgvector 是 PostgreSQL 的向量扩展，用于在数据库中存储向量并执行相似度搜索（ANN/精确检索）。

适用场景：

* RAG（检索增强生成）
* 语义搜索
* 推荐系统
* 相似内容匹配

核心能力：

* 向量存储（embedding）
* 多种距离计算（L2 / Cosine / Inner Product 等）
* 高性能近似索引（HNSW / IVFFlat）
* 与 SQL、事务、过滤条件天然结合

---

## 2. 环境准备

### 2.1 安装扩展

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

说明：

* 每个数据库都需要单独执行
* PostgreSQL >= 13

---

## 3. 数据建模

### 3.1 基础表结构（推荐模板）

```sql
CREATE TABLE rag_documents (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    doc_id BIGINT NOT NULL,
    chunk_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1536) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, doc_id, chunk_id)
);
```

设计说明：

* `tenant_id`：多租户隔离
* `doc_id`：文档ID
* `chunk_id`：分片ID（RAG常用）
* `embedding`：向量字段（维度必须固定）

---

## 4. 基本操作

### 4.1 插入数据

```sql
INSERT INTO rag_documents (tenant_id, doc_id, chunk_id, content, embedding)
VALUES (1, 1001, 1, '示例文本', '[0.1,0.2,0.3,...]');
```

### 4.2 更新数据

```sql
UPDATE rag_documents
SET embedding = '[...]'
WHERE id = 1;
```

### 4.3 删除数据

```sql
DELETE FROM rag_documents WHERE id = 1;
```

---

## 5. 向量检索

### 5.1 距离计算符号

| 操作符 | 含义         |
| ------ | ------------ |
| `<->`  | L2 距离      |
| `<=>`  | Cosine 距离  |
| `<#>`  | 内积（负值） |
| `<+>`  | L1 距离      |

---

### 5.2 基础查询

```sql
SELECT *
FROM rag_documents
ORDER BY embedding <=> '[...]'
LIMIT 10;
```

---

### 5.3 带业务过滤

```sql
SELECT *
FROM rag_documents
WHERE tenant_id = 1
ORDER BY embedding <=> '[...]'
LIMIT 10;
```

说明：

* 过滤条件优先执行
* 再进行向量排序

---

## 6. 索引（核心）

### 6.1 HNSW（推荐）

```sql
CREATE INDEX CONCURRENTLY idx_rag_embedding_hnsw
ON rag_documents
USING hnsw (embedding vector_cosine_ops);
```

特点：

* 查询速度快
* 精度高
* 内存占用较大
* 无需训练

---

### 6.2 IVFFlat

```sql
CREATE INDEX idx_rag_embedding_ivf
ON rag_documents
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);
```

特点：

* 构建快
* 内存占用低
* 需要训练（数据需提前存在）

---

## 7. 查询调优

### 7.1 HNSW 参数

```sql
SET hnsw.ef_search = 100;
```

说明：

* 越大 → 召回率越高
* 越小 → 查询更快

---

### 7.2 IVFFlat 参数

```sql
SET ivfflat.probes = 10;
```

说明：

* probes 越高 → 更准确
* probes 越低 → 更快

---

## 8. 高级能力

### 8.1 向量函数

```sql
SELECT
    vector_dims(embedding),
    l2_norm(embedding)
FROM rag_documents;
```

常用函数：

* `vector_dims`：维度
* `l2_norm`：向量长度
* `subvector`：子向量
* `avg`：聚合

---

### 8.2 子向量索引（降维优化）

```sql
CREATE INDEX idx_subvector
ON rag_documents
USING hnsw ((subvector(embedding, 1, 256)::vector(256)) vector_cosine_ops);
```

适用：

* 超高维向量（>1536）
* 需要快速粗排

---

### 8.3 二值化索引（极致性能）

```sql
CREATE INDEX idx_binary
ON rag_documents
USING hnsw ((binary_quantize(embedding)::bit(1536)) bit_hamming_ops);
```

适用：

* 大规模检索（千万级以上）

---

## 9. 性能优化建议

### 9.1 数据导入

```sql
COPY rag_documents FROM 'data.csv' WITH (FORMAT csv);
```

建议：

* 批量导入用 COPY
* 导入后再建索引

---

### 9.2 执行计划分析

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT ...
```

---

### 9.3 索引策略

| 场景       | 建议            |
| ---------- | --------------- |
| 小数据量   | 不建索引        |
| 中等数据   | HNSW            |
| 超大规模   | HNSW + 压缩     |
| 多过滤条件 | 分区 + 普通索引 |

---

## 10. 典型架构（RAG）

查询流程：

```
用户问题
   ↓
Embedding模型
   ↓
pgvector向量检索
   ↓
TopK结果
   ↓
LLM生成答案
```

---

## 11. 常见问题（坑位）

### 11.1 维度不一致

错误：

```
dimension mismatch
```

原因：

* 插入向量维度不一致

解决：

* 固定模型（如 1536）
* 严格校验

---

### 11.2 `<#>` 是负值

```sql
ORDER BY embedding <#> '[...]'
```

说明：

* 返回的是“负内积”
* 越小越相似

---

### 11.3 过滤后结果不足

原因：

* ANN索引先召回再过滤

解决：

```sql
SET hnsw.ef_search = 200;
```

---

### 11.4 索引阻塞

错误用法：

```sql
CREATE INDEX ...
```

正确：

```sql
CREATE INDEX CONCURRENTLY ...
```

---

## 12. 推荐最佳实践

1. 固定 embedding 维度（如 1536）
2. 优先使用 HNSW
3. 查询必须带 LIMIT
4. 向量 + 业务字段联合设计
5. 大数据量使用分区表
6. ANN + 精排（应用层二次排序）

---

## 13. 项目落地最小示例

### 建表 + 索引

```sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE rag_documents (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    content TEXT,
    embedding vector(1536)
);

CREATE INDEX CONCURRENTLY idx_embedding
ON rag_documents
USING hnsw (embedding vector_cosine_ops);
```

### 查询

```sql
SELECT id, content
FROM rag_documents
WHERE tenant_id = 1
ORDER BY embedding <=> :embedding
LIMIT 5;
```

---

