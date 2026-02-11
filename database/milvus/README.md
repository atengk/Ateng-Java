# Milvus

Milvus 是一款开源的向量数据库，专为大规模相似度搜索与 AI 应用设计，支持高维向量高效存储、索引与检索。常用于推荐系统、语义搜索、图像/视频检索与 RAG 场景，具备高性能、可扩展、云原生等特点。

- 官网：[https://milvus.io](https://milvus.io)

- Milvus服务安装文档：[链接](https://atengk.github.io/ops/#/work/docker/service/milvus/)



## 基础配置

**添加依赖**

```xml
<properties>
    <milvus.version>2.5.8</milvus.version>
</properties>

<dependencies>
    <!-- Milvus 向量数据库依赖 -->
    <dependency>
        <groupId>io.milvus</groupId>
        <artifactId>milvus-sdk-java</artifactId>
        <version>${milvus.version}</version>
    </dependency>
</dependencies>
```

**添加配置**

```yaml
---
# Milvus 配置
milvus:
  host: 175.178.193.128
  port: 20016
  database: default
  username: root
  password: Milvus
```

**创建配置属性类**

```java
package io.github.atengk.milvus.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "milvus")
@Data
public class MilvusProperties {

    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
}
```

**创建配置类**

```java
package io.github.atengk.milvus.config;

import io.milvus.client.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MilvusConfig {

    @Bean(destroyMethod = "close")
    public MilvusClient milvusClient(MilvusProperties props) {
        return new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost(props.getHost())
                        .withPort(props.getPort())
                        .withDatabaseName(props.getDatabase())
                        .withAuthorization(
                                props.getUsername(),
                                props.getPassword()
                        )
                        .build()
        );
    }

}

```



## 创建实体类

### VectorDocument

```java
package io.github.atengk.milvus.entity;

import com.google.gson.JsonObject;
import lombok.Data;

import java.util.List;

@Data
public class VectorDocument {

    private String id;

    private String content;

    private List<Float> embedding;

    private JsonObject metadata;

}


```

### SimilaritySearchRequest

```java
package io.github.atengk.milvus.entity;

import lombok.Data;

import java.util.List;

@Data
public class SimilaritySearchRequest {

    /**
     * collection 名称
     */
    private String collectionName;

    /**
     * 查询向量（embedding）
     */
    private List<Float> embedding;

    /**
     * 返回的相似结果数量
     */
    private int topK = 5;

    /**
     * Milvus expr
     */
    private String expr;

    /**
     * 是否返回向量本身（默认不返回）
     */
    private boolean includeEmbedding = false;

}
```

### SimilaritySearchResult

```java
package io.github.atengk.milvus.entity;

import lombok.Data;

@Data
public class SimilaritySearchResult {
    private VectorDocument document;
    private float score;
}
```

### QueryRequest

```java
package io.github.atengk.milvus.entity;

import lombok.Data;

import java.util.List;

@Data
public class QueryRequest {

    private String collectionName;

    private String expr;

    private List<String> outputFields;

}


```

### CollectionSpec

```java
package io.github.atengk.milvus.entity;

import lombok.Data;

@Data
public class CollectionSpec {

    private String collectionName;

    private int dimension;

    private boolean autoId;

}
```



## 创建EmbeddingService

Embedding 就是把文本、图片等“人能理解的内容”，通过模型转换成固定维度的数字向量，让机器可以用数学距离来判断语义相似度；Milvus 只负责存和算距离，真正“懂语义”的只有 Embedding 模型，写入和查询必须用同一个模型生成向量，否则检索就是假的。

### 创建接口

```java
package io.github.atengk.milvus.service;

import java.util.List;

public interface EmbeddingService {

    /**
     * 单条文本 embedding
     */
    List<Float> embed(String text);

    /**
     * 批量 embedding（强烈推荐）
     */
    List<List<Float>> embedBatch(List<String> texts);

    /**
     * 返回 embedding 维度
     */
    int dimension();
}


```

### 实现接口

```java
package io.github.atengk.milvus.service.impl;

import io.github.atengk.milvus.service.EmbeddingService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class MockEmbeddingServiceImpl implements EmbeddingService {

    private static final int DIMENSION = 1536;

    @Override
    public List<Float> embed(String text) {
        Random random = new Random(text.hashCode());
        List<Float> vector = new ArrayList<>(DIMENSION);
        for (int i = 0; i < DIMENSION; i++) {
            vector.add(random.nextFloat());
        }
        return vector;
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        return texts.stream()
                .map(this::embed)
                .toList();
    }

    @Override
    public int dimension() {
        return DIMENSION;
    }
}


```



## 创建MilvusService

### 创建接口

```java
package io.github.atengk.milvus.service;

import io.github.atengk.milvus.entity.*;
import jakarta.servlet.Filter;

import java.util.List;
import java.util.Optional;

public interface MilvusService {

    /* ========================= health ========================= */

    /**
     * Milvus 是否可用（健康检查）
     */
    boolean isHealthy();


    /* ========================= database ========================= */

    /**
     * 创建 database（幂等）
     */
    void createDatabaseIfNotExists(String databaseName);

    /**
     * 删除 database
     */
    void dropDatabase(String databaseName);


    /* ========================= collection ========================= */

    /**
     * 创建 collection（包含 schema + 默认向量索引）
     */
    void createCollection(CollectionSpec spec);

    /**
     * collection 是否存在
     */
    boolean collectionExists(String collectionName);

    /**
     * 加载 collection（运行态）
     */
    void loadCollection(String collectionName);

    /**
     * 释放 collection（资源管理）
     */
    void releaseCollection(String collectionName);

    /**
     * 获取 collection 真实 schema 信息
     */
    Optional<CollectionSpec> getCollection(String collectionName);

    /**
     * 删除 collection
     */
    void dropCollection(String collectionName);


    /* ========================= document ========================= */

    /**
     * 新增文档（不允许覆盖）
     */
    void add(String collectionName, List<VectorDocument> documents);

    /**
     * 根据 ID 查询
     */
    List<VectorDocument> getByIds(String collectionName, List<String> ids);

    /**
     * 根据 ID 删除
     */
    void deleteByIds(String collectionName, List<String> ids);

    /**
     * 根据条件表达式删除数据
     *
     * @param collectionName collection 名称
     * @param expr Milvus 表达式，例如：id in ["1","2"]
     */
    void deleteByExpr(String collectionName, String expr);

    /* ========================= query by expr ========================= */

    /**
     * 根据条件表达式查询文档列表
     *
     * @param collectionName collection 名称
     * @param expr Milvus 表达式，例如：metadata.documentId == "xxx"
     * @param limit 最大返回数量
     * @return 文档列表
     */
    List<VectorDocument> listByExpr(
            String collectionName,
            String expr,
            long limit
    );

    /**
     * 根据条件表达式判断是否存在数据
     *
     * @param collectionName collection 名称
     * @param expr Milvus 表达式
     * @return 是否存在
     */
    boolean existsByExpr(
            String collectionName,
            String expr
    );

    /* ========================= vector search ========================= */

    /**
     * 相似度检索（默认返回 score）
     */
    List<SimilaritySearchResult> similaritySearch(SimilaritySearchRequest request);


    /* ========================= admin / maintenance ========================= */

    /**
     * 强制持久化（一般不需要手动调用）
     */
    void flush(String collectionName);
}

```

### 实现接口

```java
package io.github.atengk.milvus.service.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.protobuf.ByteString;
import io.github.atengk.milvus.entity.CollectionSpec;
import io.github.atengk.milvus.entity.SimilaritySearchRequest;
import io.github.atengk.milvus.entity.SimilaritySearchResult;
import io.github.atengk.milvus.entity.VectorDocument;
import io.github.atengk.milvus.service.MilvusService;
import io.milvus.client.MilvusClient;
import io.milvus.grpc.*;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.*;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.QueryParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.highlevel.collection.ListCollectionsParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MilvusServiceImpl implements MilvusService {

    private static final String ID_FIELD = "id";
    private static final String CONTENT_FIELD = "content";
    private static final String METADATA_FIELD = "metadata";
    private static final String VECTOR_FIELD = "embedding";

    private static final MetricType METRIC_TYPE = MetricType.COSINE;

    private final MilvusClient milvusClient;

    public MilvusServiceImpl(MilvusClient milvusClient) {
        this.milvusClient = milvusClient;
    }

    /* ========================= health ========================= */

    @Override
    public boolean isHealthy() {
        try {
            milvusClient.listCollections(
                    ListCollectionsParam.newBuilder().build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /* ========================= database ========================= */

    @Override
    public void createDatabaseIfNotExists(String databaseName) {

        R<ListDatabasesResponse> response = milvusClient.listDatabases();

        if (response.getStatus() != R.Status.Success.getCode()) {
            throw new IllegalStateException("List databases failed: " + response.getMessage());
        }

        if (!response.getData().getDbNamesList().contains(databaseName)) {
            milvusClient.createDatabase(
                    CreateDatabaseParam.newBuilder()
                            .withDatabaseName(databaseName)
                            .build()
            );
        }
    }

    @Override
    public void dropDatabase(String databaseName) {
        milvusClient.dropDatabase(
                DropDatabaseParam.newBuilder()
                        .withDatabaseName(databaseName)
                        .build()
        );
    }

    /* ========================= collection ========================= */

    @Override
    public void createCollection(CollectionSpec spec) {

        if (collectionExists(spec.getCollectionName())) {
            return;
        }

        List<FieldType> fields = new ArrayList<>();

        fields.add(FieldType.newBuilder()
                .withName(ID_FIELD)
                .withDataType(DataType.VarChar)
                .withMaxLength(128)
                .withPrimaryKey(true)
                .withAutoID(spec.isAutoId())
                .build());

        fields.add(FieldType.newBuilder()
                .withName(CONTENT_FIELD)
                .withDataType(DataType.VarChar)
                .withMaxLength(65535)
                .build());

        fields.add(FieldType.newBuilder()
                .withName(METADATA_FIELD)
                .withDataType(DataType.JSON)
                .build());

        fields.add(FieldType.newBuilder()
                .withName(VECTOR_FIELD)
                .withDataType(DataType.FloatVector)
                .withDimension(spec.getDimension())
                .build());

        milvusClient.createCollection(
                CreateCollectionParam.newBuilder()
                        .withCollectionName(spec.getCollectionName())
                        .withFieldTypes(fields)
                        .withShardsNum(1)
                        .withReplicaNumber(1)
                        .build()
        );

        // 自动创建向量索引
        milvusClient.createIndex(
                CreateIndexParam.newBuilder()
                        .withCollectionName(spec.getCollectionName())
                        .withFieldName(VECTOR_FIELD)
                        .withIndexType(IndexType.IVF_FLAT)
                        .withMetricType(MetricType.COSINE)
                        .withExtraParam("{\"nlist\":1024}")
                        .build()
        );

        // 加载集合
        loadCollection(spec.getCollectionName());
    }

    @Override
    public boolean collectionExists(String collectionName) {
        return milvusClient.hasCollection(
                HasCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .build()
        ).getData();
    }

    @Override
    public void loadCollection(String collectionName) {

        milvusClient.loadCollection(
                LoadCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .build()
        );

    }

    @Override
    public void releaseCollection(String collectionName) {

        milvusClient.releaseCollection(
                ReleaseCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .build()
        );
    }

    @Override
    public Optional<CollectionSpec> getCollection(String collectionName) {

        if (!collectionExists(collectionName)) {
            return Optional.empty();
        }

        DescribeCollectionResponse response =
                milvusClient.describeCollection(
                        DescribeCollectionParam.newBuilder()
                                .withCollectionName(collectionName)
                                .build()
                ).getData();

        CollectionSpec spec = new CollectionSpec();
        spec.setCollectionName(collectionName);

        for (FieldSchema field : response.getSchema().getFieldsList()) {

            if (field.getIsPrimaryKey()) {
                spec.setAutoId(field.getAutoID());
            }

            if (field.getDataType() == DataType.FloatVector
                    && VECTOR_FIELD.equals(field.getName())) {

                for (KeyValuePair kv : field.getTypeParamsList()) {
                    if ("dim".equals(kv.getKey())) {
                        spec.setDimension(Integer.parseInt(kv.getValue()));
                        break;
                    }
                }
            }
        }

        return Optional.of(spec);
    }

    @Override
    public void dropCollection(String collectionName) {
        milvusClient.dropCollection(
                DropCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .build()
        );
    }

    /* ========================= document ========================= */

    @Override
    public void add(String collectionName, List<VectorDocument> documents) {
        insertInternal(collectionName, documents);
    }

    private void insertInternal(String collectionName, List<VectorDocument> documents) {

        List<InsertParam.Field> fields = new ArrayList<>();

        fields.add(new InsertParam.Field(
                ID_FIELD,
                documents.stream().map(VectorDocument::getId).collect(Collectors.toList())
        ));

        fields.add(new InsertParam.Field(
                CONTENT_FIELD,
                documents.stream().map(VectorDocument::getContent).collect(Collectors.toList())
        ));

        fields.add(new InsertParam.Field(
                METADATA_FIELD,
                documents.stream()
                        .map(VectorDocument::getMetadata)
                        .collect(Collectors.toList())
        ));

        fields.add(new InsertParam.Field(
                VECTOR_FIELD,
                documents.stream()
                        .map(VectorDocument::getEmbedding)
                        .collect(Collectors.toList())
        ));

        milvusClient.insert(
                InsertParam.newBuilder()
                        .withCollectionName(collectionName)
                        .withFields(fields)
                        .build()
        );
    }

    @Override
    public List<VectorDocument> getByIds(String collectionName, List<String> ids) {

        String expr = ID_FIELD + " in " + ids.stream()
                .map(id -> "\"" + id + "\"")
                .collect(Collectors.joining(",", "[", "]"));

        QueryResults results = milvusClient.query(
                QueryParam.newBuilder()
                        .withCollectionName(collectionName)
                        .withExpr(expr)
                        .withOutFields(Arrays.asList(
                                ID_FIELD, CONTENT_FIELD, METADATA_FIELD
                        ))
                        .build()
        ).getData();

        return mapQueryResults(results);
    }

    @Override
    public void deleteByIds(String collectionName, List<String> ids) {

        String expr = ID_FIELD + " in " + ids.stream()
                .map(id -> "\"" + id + "\"")
                .collect(Collectors.joining(",", "[", "]"));

        milvusClient.delete(
                DeleteParam.newBuilder()
                        .withCollectionName(collectionName)
                        .withExpr(expr)
                        .build()
        );
    }

    @Override
    public void deleteByExpr(String collectionName, String expr) {
        milvusClient.delete(
                DeleteParam.newBuilder()
                        .withCollectionName(collectionName)
                        .withExpr(expr)
                        .build()
        );
    }

    @Override
    public List<VectorDocument> listByExpr(String collectionName, String expr, long limit) {

        if (limit <= 0) {
            return List.of();
        }

        QueryResults results = milvusClient.query(
                QueryParam.newBuilder()
                        .withCollectionName(collectionName)
                        .withExpr(expr)
                        .withOutFields(Arrays.asList(
                                ID_FIELD, CONTENT_FIELD, METADATA_FIELD
                        ))
                        .withLimit(limit)
                        .build()
        ).getData();

        return mapQueryResults(results);
    }

    @Override
    public boolean existsByExpr(String collectionName, String expr) {
        return !listByExpr(collectionName, expr, 1).isEmpty();
    }

    /* ========================= vector search ========================= */

    @Override
    public List<SimilaritySearchResult> similaritySearch(SimilaritySearchRequest request) {

        List<String> outFields = new ArrayList<>(
                Arrays.asList(ID_FIELD, CONTENT_FIELD, METADATA_FIELD)
        );

        if (request.isIncludeEmbedding()) {
            outFields.add(VECTOR_FIELD);
        }

        SearchParam param = SearchParam.newBuilder()
                .withCollectionName(request.getCollectionName())
                .withVectorFieldName(VECTOR_FIELD)
                .withVectors(Collections.singletonList(request.getEmbedding()))
                .withTopK(request.getTopK())
                .withMetricType(METRIC_TYPE)
                .withParams("{\"nprobe\":10}")
                .withOutFields(outFields)
                .withExpr(request.getExpr())
                .build();

        SearchResultsWrapper wrapper =
                new SearchResultsWrapper(
                        milvusClient.search(param)
                                .getData()
                                .getResults()
                );

        List<QueryResultsWrapper.RowRecord> rows = wrapper.getRowRecords(0);
        List<SearchResultsWrapper.IDScore> scores = wrapper.getIDScore(0);

        List<SimilaritySearchResult> results = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            QueryResultsWrapper.RowRecord row = rows.get(i);

            VectorDocument doc = new VectorDocument();
            doc.setId((String) row.get(ID_FIELD));
            doc.setContent((String) row.get(CONTENT_FIELD));
            doc.setMetadata((JsonObject) row.get(METADATA_FIELD));

            if (request.isIncludeEmbedding()) {
                doc.setEmbedding((List<Float>) row.get(VECTOR_FIELD));
            }

            SimilaritySearchResult result = new SimilaritySearchResult();
            result.setDocument(doc);
            result.setScore(scores.get(i).getScore());

            results.add(result);
        }

        return results;
    }


    /* ========================= admin ========================= */

    @Override
    public void flush(String collectionName) {
        milvusClient.flush(
                FlushParam.newBuilder()
                        .withCollectionNames(Collections.singletonList(collectionName))
                        .build()
        );
    }

    /* ========================= helper ========================= */

    private List<VectorDocument> mapQueryResults(QueryResults results) {

        if (results == null || results.getFieldsDataCount() == 0) {
            return Collections.emptyList();
        }

        List<FieldData> fields = results.getFieldsDataList();

        int rowCount = fields.get(0)
                .getScalars()
                .getStringData()
                .getDataCount();

        List<VectorDocument> documents = new ArrayList<>(rowCount);

        for (int i = 0; i < rowCount; i++) {

            Map<String, Object> row = new HashMap<>();

            for (FieldData field : fields) {

                String fieldName = field.getFieldName();

                if (field.getType() == DataType.VarChar) {
                    row.put(fieldName,
                            field.getScalars()
                                    .getStringData()
                                    .getData(i));
                }

                else if (field.getType() == DataType.JSON) {
                    row.put(fieldName, field.getScalars()
                            .getJsonData()
                            .getData(i));
                }
            }

            VectorDocument doc = new VectorDocument();
            doc.setId((String) row.get(ID_FIELD));
            doc.setContent((String) row.get(CONTENT_FIELD));

            Object rawMetadata = row.get(METADATA_FIELD);
            JsonObject metadata = parseMetadata(rawMetadata);
            doc.setMetadata(metadata);

            documents.add(doc);
        }

        return documents;
    }

    private JsonObject parseMetadata(Object raw) {
        if (raw == null) {
            return null;
        }

        String jsonText;

        if (raw instanceof ByteString bs) {
            jsonText = bs.toStringUtf8();
        } else {
            jsonText = raw.toString();
        }

        try {
            // 先尝试严格 JSON
            return JsonParser.parseString(jsonText).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            // fallback：Milvus JSON 经常不是严格格式
            JsonReader reader = new JsonReader(new StringReader(jsonText));
            reader.setStrictness(Strictness.LENIENT);
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

}

```



### 使用方法

```java
package io.github.atengk.milvus;

import com.google.gson.JsonObject;
import io.github.atengk.milvus.entity.CollectionSpec;
import io.github.atengk.milvus.entity.SimilaritySearchRequest;
import io.github.atengk.milvus.entity.SimilaritySearchResult;
import io.github.atengk.milvus.entity.VectorDocument;
import io.github.atengk.milvus.service.EmbeddingService;
import io.github.atengk.milvus.service.MilvusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@SpringBootTest
public class MilvusServiceTest {

    @Autowired
    private MilvusService milvusService;
    @Autowired
    private EmbeddingService embeddingService;

    private static final String DATABASE = "test_db";
    private static final String COLLECTION = "test_collection";

    /* ========================= context ========================= */

    @Test
    void testIsHealthy() {
        boolean healthy = milvusService.isHealthy();
        System.out.println("Milvus healthy = " + healthy);
    }

    /* ========================= database ========================= */

    @Test
    void testCreateDatabase() {
        milvusService.createDatabaseIfNotExists(DATABASE);
    }

    @Test
    void testDropDatabase() {
        milvusService.dropDatabase(DATABASE);
    }

    /* ========================= collection ========================= */

    @Test
    void testCreateCollection() {
        CollectionSpec spec = new CollectionSpec();
        spec.setCollectionName(COLLECTION);
        spec.setDimension(1536);
        spec.setAutoId(false);

        milvusService.createCollection(spec);
    }

    @Test
    void testGetCollection() {
        Optional<CollectionSpec> optional = milvusService.getCollection(COLLECTION);
        System.out.println(optional.get());
    }

    @Test
    void testDropCollection() {
        milvusService.dropCollection(COLLECTION);
    }

    /* ========================= data ========================= */

    @Test
    void testAddDocuments() {
        VectorDocument doc = new VectorDocument();
        doc.setId("doc-1");
        String content = "Hello Milvus";
        doc.setContent(content);
        doc.setEmbedding(embeddingService.embed(content));

        JsonObject metadata = new JsonObject();
        metadata.addProperty("source", "test");
        doc.setMetadata(metadata);

        milvusService.add(COLLECTION, Collections.singletonList(doc));
    }

    @Test
    void testDeleteByIds() {
        milvusService.deleteByIds(
                COLLECTION,
                Arrays.asList("doc-1", "doc-2")
        );
    }

    @Test
    void testDeleteByExpr() {
        milvusService.deleteByExpr(
                COLLECTION,
                "metadata[\"source\"] == \"test\""
        );
    }

    @Test
    void testGetByIds() {
        List<VectorDocument> documentList = milvusService.getByIds(
                COLLECTION,
                Arrays.asList("doc-1", "doc-2")
        );
        System.out.println(documentList);
    }

    @Test
    void testSimilaritySearch() {
        String queryText = "Hello Milvus";
        List<Float> queryEmbedding = embeddingService.embed(queryText);

        SimilaritySearchRequest request = new SimilaritySearchRequest();
        request.setCollectionName(COLLECTION);
        request.setEmbedding(queryEmbedding);
        request.setTopK(5);
        request.setExpr("metadata[\"source\"] == \"test\"");
        request.setIncludeEmbedding(false);

        List<SimilaritySearchResult> results =
                milvusService.similaritySearch(request);

        results.forEach(System.out::println);
    }

    @Test
    void testSimilaritySearchAll() {

        List<Float> queryEmbedding =
                embeddingService.embed("这份文档主要讲了什么？");

        SimilaritySearchRequest request = new SimilaritySearchRequest();
        request.setCollectionName(COLLECTION);
        request.setEmbedding(queryEmbedding);
        request.setTopK(5);
        // 设置 expr = "" 查全部
        request.setExpr("");

        List<SimilaritySearchResult> results =
                milvusService.similaritySearch(request);

        results.forEach(System.out::println);
    }

    @Test
    void testSimilaritySearchByAuthor() {

        // 查询文本 → embedding
        List<Float> queryEmbedding =
                embeddingService.embed("阿腾在文档中写了什么？");

        // 构造搜索请求
        SimilaritySearchRequest request = new SimilaritySearchRequest();
        request.setCollectionName(COLLECTION);
        request.setEmbedding(queryEmbedding);
        request.setTopK(5);
        request.setExpr("metadata[\"author\"] == \"阿腾\"");
        request.setIncludeEmbedding(false);

        // 搜索
        List<SimilaritySearchResult> results =
                milvusService.similaritySearch(request);

        // 使用结果
        for (SimilaritySearchResult r : results) {
            VectorDocument doc = r.getDocument();
            JsonObject meta = doc.getMetadata();

            System.out.println("score = " + r.getScore());
            System.out.println("file = " + meta.get("fileName").getAsString());
            System.out.println("chunkIndex = " + meta.get("chunkIndex").getAsInt());
            System.out.println("content = " + doc.getContent());
            System.out.println("--------------");
        }
    }

    /* ========================= maintenance ========================= */

    @Test
    void testFlush() {
        milvusService.flush(COLLECTION);
    }
}


```



## 构建知识库

### 配置 Apache Tika

#### 添加依赖

```xml
<properties>
    <tika.version>3.2.3</tika.version>
</properties>
<dependencies>
    <!-- Apache Tika 检测库 -->
    <dependency>
        <groupId>org.apache.tika</groupId>
        <artifactId>tika-core</artifactId>
        <version>${tika.version}</version>
    </dependency>
    <!-- Apache Tika 解析内容库 -->
    <dependency>
        <groupId>org.apache.tika</groupId>
        <artifactId>tika-parsers-standard-package</artifactId>
        <version>${tika.version}</version>
    </dependency>
</dependencies>
```

#### 创建 TikaUtil

```java
package io.github.atengk.milvus.util;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Apache Tika 工具类
 * <p>
 * 提供文件类型检测、文本内容提取、元数据解析等能力
 *
 * @author Ateng
 * @since 2026-02-09
 */
public final class TikaUtil {

    private static final Logger log = LoggerFactory.getLogger(TikaUtil.class);

    /**
     * 默认最大文本提取长度
     */
    private static final int DEFAULT_MAX_CONTENT_LENGTH = 100_000;

    /**
     * 线程安全的 Tika 实例
     */
    private static final Tika TIKA = new Tika();

    /**
     * 自动检测解析器
     */
    private static final AutoDetectParser PARSER = new AutoDetectParser();

    private TikaUtil() {
    }

    /* ========================= type ========================= */

    /**
     * 检测文件 MIME 类型
     *
     * @param file 文件对象
     * @return MIME 类型，失败返回 null
     */
    public static String detect(File file) {
        if (file == null) {
            return null;
        }
        try {
            return TIKA.detect(file);
        } catch (Exception e) {
            log.warn("Detect file type failed: {}", file.getAbsolutePath(), e);
            return null;
        }
    }

    /**
     * 检测字节数据 MIME 类型
     *
     * @param data 文件字节数据
     * @return MIME 类型，失败返回 null
     */
    public static String detect(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        try {
            return TIKA.detect(data);
        } catch (Exception e) {
            log.warn("Detect byte[] type failed", e);
            return null;
        }
    }

    /**
     * 检测输入流 MIME 类型
     *
     * @param inputStream 输入流
     * @return MIME 类型，失败返回 null
     */
    public static String detect(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        try {
            return TIKA.detect(inputStream);
        } catch (Exception e) {
            log.warn("Detect InputStream type failed", e);
            return null;
        }
    }

    /**
     * 是否为图片类型
     *
     * @param mimeType MIME 类型
     * @return 是否为图片
     */
    public static boolean isImage(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    /**
     * 是否需要 OCR 处理
     *
     * @param mimeType MIME 类型
     * @return 是否需要 OCR
     */
    public static boolean needOcr(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return isImage(mimeType);
    }

    /**
     * 是否为音频类型
     *
     * @param mimeType MIME 类型
     * @return 是否为音频
     */
    public static boolean isAudio(String mimeType) {
        return mimeType != null && mimeType.startsWith("audio/");
    }

    /**
     * 是否为视频类型
     *
     * @param mimeType MIME 类型
     * @return 是否为视频
     */
    public static boolean isVideo(String mimeType) {
        return mimeType != null && mimeType.startsWith("video/");
    }

    /**
     * 是否为 PDF
     *
     * @param mimeType MIME 类型
     * @return 是否为 PDF
     */
    public static boolean isPdf(String mimeType) {
        return "application/pdf".equals(mimeType);
    }

    /**
     * 是否为 Office 文档
     *
     * @param mimeType MIME 类型
     * @return 是否为 Office 文档
     */
    public static boolean isOffice(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return mimeType.startsWith("application/msword")
                || mimeType.startsWith("application/vnd.ms-")
                || mimeType.startsWith("application/vnd.openxmlformats-officedocument");
    }

    /**
     * 是否为可解析文本类型
     *
     * @param mimeType MIME 类型
     * @return 是否可能包含正文文本
     */
    public static boolean isTextual(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return mimeType.startsWith("text/")
                || isPdf(mimeType)
                || isOffice(mimeType);
    }

    /**
     * 校验 MIME 类型是否在白名单中
     *
     * @param mimeType MIME 类型
     * @param allowed  允许的 MIME 类型集合
     * @return 是否允许
     */
    public static boolean isAllowed(String mimeType, Set<String> allowed) {
        if (mimeType == null || allowed == null || allowed.isEmpty()) {
            return false;
        }
        return allowed.contains(mimeType);
    }

    /**
     * 校验文件扩展名与 MIME 是否匹配
     *
     * @param file     文件
     * @param mimeType MIME 类型
     * @return 是否匹配
     */
    public static boolean isExtensionMatch(File file, String mimeType) {
        if (file == null || mimeType == null) {
            return false;
        }
        String name = file.getName().toLowerCase();

        if (name.endsWith(".pdf")) {
            return isPdf(mimeType);
        }
        if (name.endsWith(".docx") || name.endsWith(".doc")) {
            return isOffice(mimeType);
        }
        if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return isImage(mimeType);
        }
        return true;
    }

    /**
     * 是否为可安全解析文件
     *
     * @param file        文件
     * @param allowedMime 允许的 MIME 类型
     * @param maxBytes    最大文件大小
     * @return 是否可解析
     */
    public static boolean canParse(File file, Set<String> allowedMime, long maxBytes) {
        if (isEmpty(file) || isTooLarge(file, maxBytes)) {
            return false;
        }
        String mimeType = detect(file);
        return isAllowed(mimeType, allowedMime) && isExtensionMatch(file, mimeType);
    }


    /* ========================= text ========================= */

    /**
     * 提取文件文本内容
     *
     * @param file 文件对象
     * @return 文本内容，失败返回空字符串
     */
    public static String parseText(File file) {
        if (file == null) {
            return "";
        }
        try (InputStream inputStream = new FileInputStream(file)) {
            return parseText(inputStream, DEFAULT_MAX_CONTENT_LENGTH);
        } catch (Exception e) {
            log.warn("Parse text from file failed: {}", file.getAbsolutePath(), e);
            return "";
        }
    }

    /**
     * 提取字节数据文本内容
     *
     * @param data 文件字节数据
     * @return 文本内容，失败返回空字符串
     */
    public static String parseText(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        try (InputStream inputStream = new ByteArrayInputStream(data)) {
            return parseText(inputStream, DEFAULT_MAX_CONTENT_LENGTH);
        } catch (Exception e) {
            log.warn("Parse text from byte[] failed", e);
            return "";
        }
    }

    /**
     * 提取输入流文本内容
     *
     * @param inputStream      输入流
     * @param maxContentLength 最大提取字符数，< 0 表示不限制
     * @return 文本内容，失败返回空字符串
     */
    public static String parseText(InputStream inputStream, int maxContentLength) {
        if (inputStream == null) {
            return "";
        }

        try {
            int limit = maxContentLength < 0 ? -1 : maxContentLength;
            BodyContentHandler handler = new BodyContentHandler(limit);
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            PARSER.parse(inputStream, handler, metadata, context);

            return handler.toString();
        } catch (Exception e) {
            log.warn("Parse text from InputStream failed", e);
            return "";
        }
    }

    /* ========================= metadata ========================= */

    /**
     * 解析文件元数据
     *
     * @param file 文件对象
     * @return 元数据 Map，失败返回空 Map
     */
    public static Map<String, String> parseMetadata(File file) {
        if (file == null) {
            return Collections.emptyMap();
        }
        try (InputStream inputStream = new FileInputStream(file)) {
            return parseMetadata(inputStream);
        } catch (Exception e) {
            log.warn("Parse metadata from file failed: {}", file.getAbsolutePath(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * 获取指定元数据值
     *
     * @param file 文件
     * @param key  元数据 key
     * @return 元数据值，不存在返回 null
     */
    public static String getMetadata(File file, String key) {
        if (file == null || key == null) {
            return null;
        }
        Map<String, String> metadata = parseMetadata(file);
        return metadata.get(key);
    }

    /**
     * 解析字节数据元数据
     *
     * @param data 文件字节数据
     * @return 元数据 Map，失败返回空 Map
     */
    public static Map<String, String> parseMetadata(byte[] data) {
        if (data == null || data.length == 0) {
            return Collections.emptyMap();
        }
        try (InputStream inputStream = new ByteArrayInputStream(data)) {
            return parseMetadata(inputStream);
        } catch (Exception e) {
            log.warn("Parse metadata from byte[] failed", e);
            return Collections.emptyMap();
        }
    }

    /**
     * 解析输入流元数据
     *
     * @param inputStream 输入流
     * @return 元数据 Map，失败返回空 Map
     */
    public static Map<String, String> parseMetadata(InputStream inputStream) {
        if (inputStream == null) {
            return Collections.emptyMap();
        }
        try {
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            PARSER.parse(inputStream, handler, metadata, context);

            return toMap(metadata);
        } catch (Exception e) {
            log.warn("Parse metadata from InputStream failed", e);
            return Collections.emptyMap();
        }
    }

    /* ========================= full ========================= */

    /**
     * 同时解析文本内容和元数据
     *
     * @param file 文件对象
     * @return 解析结果，失败返回 null
     */
    public static TikaResult parseAll(File file) {
        if (file == null) {
            return null;
        }
        try (InputStream inputStream = new FileInputStream(file)) {
            return parseAll(inputStream, DEFAULT_MAX_CONTENT_LENGTH);
        } catch (Exception e) {
            log.warn("Parse all from file failed: {}", file.getAbsolutePath(), e);
            return null;
        }
    }

    /**
     * 同时解析文本内容和元数据
     *
     * @param inputStream      输入流
     * @param maxContentLength 最大提取字符数
     * @return 解析结果，失败返回 null
     */
    public static TikaResult parseAll(InputStream inputStream, int maxContentLength) {
        if (inputStream == null) {
            return null;
        }

        try {
            int limit = maxContentLength < 0 ? -1 : maxContentLength;
            BodyContentHandler handler = new BodyContentHandler(limit);
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            PARSER.parse(inputStream, handler, metadata, context);

            return new TikaResult(handler.toString(), toMap(metadata));
        } catch (Exception e) {
            log.warn("Parse all from InputStream failed", e);
            return null;
        }
    }

    /* ========================= helper ========================= */

    private static Map<String, String> toMap(Metadata metadata) {
        if (metadata == null || metadata.size() == 0) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new HashMap<>(metadata.size());
        for (String name : metadata.names()) {
            map.put(name, metadata.get(name));
        }
        return map;
    }

    /* ========================= result ========================= */

    /**
     * Tika 解析结果封装
     */
    public static final class TikaResult {

        private final String content;
        private final Map<String, String> metadata;

        public TikaResult(String content, Map<String, String> metadata) {
            this.content = content;
            this.metadata = metadata;
        }

        public String getContent() {
            return content;
        }

        public Map<String, String> getMetadata() {
            return metadata;
        }
    }

    /* ========================= size ========================= */

    /**
     * 是否为空文件
     *
     * @param file 文件
     * @return 是否为空
     */
    public static boolean isEmpty(File file) {
        return file == null || !file.exists() || file.length() == 0;
    }

    /**
     * 是否超过最大文件大小
     *
     * @param file     文件
     * @param maxBytes 最大字节数
     * @return 是否超限
     */
    public static boolean isTooLarge(File file, long maxBytes) {
        if (file == null || maxBytes <= 0) {
            return false;
        }
        return file.length() > maxBytes;
    }

}
```



### 创建FileVectorService接口

```java
package io.github.atengk.milvus.service;

import java.io.InputStream;
import java.util.Map;

public interface FileVectorService {

    /**
     * 将文件写入 Milvus（自动解析、切割、embedding）
     *
     * @param collectionName Milvus collection
     * @param fileName 文件名（用于 metadata）
     * @param inputStream 文件流
     * @param metadata 额外元数据（如 source、bizId）
     */
    void ingest(
            String collectionName,
            String fileName,
            InputStream inputStream,
            Map<String, Object> metadata
    );
}

```

### FileVectorService接口实现

```java
package io.github.atengk.milvus.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.google.gson.JsonObject;
import io.github.atengk.milvus.entity.VectorDocument;
import io.github.atengk.milvus.service.EmbeddingService;
import io.github.atengk.milvus.service.FileVectorService;
import io.github.atengk.milvus.service.MilvusService;
import io.github.atengk.milvus.util.TextSplitter;
import io.github.atengk.milvus.util.TikaUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class FileVectorServiceImpl implements FileVectorService {

    private static final int CHUNK_SIZE = 800;
    private static final int CHUNK_OVERLAP = 150;

    private final MilvusService milvusService;
    private final EmbeddingService embeddingService;

    public FileVectorServiceImpl(
            MilvusService milvusService,
            EmbeddingService embeddingService
    ) {
        this.milvusService = milvusService;
        this.embeddingService = embeddingService;
    }

    @Override
    public void ingest(
            String collectionName,
            String fileName,
            InputStream inputStream,
            Map<String, Object> externalMetadata
    ) {
        try {
            // 读取文件字节 + 计算内容指纹
            byte[] fileBytes = inputStream.readAllBytes();
            String documentId = DigestUtil.sha256Hex(fileBytes);

            // 文件防重复
            String expr = String.format(
                    "metadata[\"documentId\"] == \"%s\"",
                    documentId
            );

            if (milvusService.existsByExpr(collectionName, expr)) {
                log.warn("文件重复，跳过写入: fileName={}, SHA-256={}", fileName, documentId);
                return;
            }

            // Tika 解析文本
            InputStream tikaInputStream = new ByteArrayInputStream(fileBytes);
            TikaUtil.TikaResult tikaResult = TikaUtil.parseAll(tikaInputStream, -1);

            String content = tikaResult.getContent();
            if (StrUtil.isBlank(content)) {
                log.warn("文件内容为空，跳过写入: fileName={}", fileName);
                return;
            }

            // 文本切割
            List<String> chunks = TextSplitter.split(
                    content,
                    CHUNK_SIZE,
                    CHUNK_OVERLAP
            );

            if (chunks.isEmpty()) {
                log.warn("文本切割结果为空，跳过写入: fileName={}", fileName);
                return;
            }

            int chunkTotal = chunks.size();
            List<VectorDocument> documents = new ArrayList<>(chunkTotal);

            int offsetCursor = 0;

            // 构建向量文档
            for (int i = 0; i < chunkTotal; i++) {

                String chunk = chunks.get(i);
                if (StrUtil.isBlank(chunk)) {
                    continue;
                }

                String chunkId = UUID.randomUUID().toString();

                int startOffset = offsetCursor;
                int endOffset = startOffset + chunk.length();
                offsetCursor = Math.max(endOffset - CHUNK_OVERLAP, startOffset);

                VectorDocument document = new VectorDocument();
                document.setId(chunkId);
                document.setContent(chunk);
                document.setEmbedding(embeddingService.embed(chunk));

                JsonObject metadata = new JsonObject();

                // 文档级
                metadata.addProperty("documentId", documentId);
                metadata.addProperty("fileHash", documentId);
                metadata.addProperty("fileName", fileName);

                // chunk 级
                metadata.addProperty("chunkId", chunkId);
                metadata.addProperty("chunkIndex", i);
                metadata.addProperty("chunkTotal", chunkTotal);
                metadata.addProperty("startOffset", startOffset);
                metadata.addProperty("endOffset", endOffset);
                metadata.addProperty("chunkSize", CHUNK_SIZE);
                metadata.addProperty("chunkOverlap", CHUNK_OVERLAP);

                // Tika 元数据
                if (tikaResult.getMetadata() != null) {
                    tikaResult.getMetadata().forEach(metadata::addProperty);
                }

                // 外部透传 metadata
                if (externalMetadata != null) {
                    externalMetadata.forEach(
                            (k, v) -> metadata.addProperty(k, String.valueOf(v))
                    );
                }

                document.setMetadata(metadata);
                documents.add(document);
            }

            // 写入 Milvus
            if (!documents.isEmpty()) {
                milvusService.add(collectionName, documents);
            }

            log.info(
                    "文件写入 Milvus 完成: fileName={}, documentId={}, chunks={}",
                    fileName,
                    documentId,
                    documents.size()
            );

        } catch (Exception e) {
            log.error("写入 Milvus 失败: fileName={}", fileName, e);
            throw new RuntimeException("写入 Milvus 失败", e);
        }
    }

}

```

### 创建文本切割工具

```java
package io.github.atengk.milvus.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 文本切割工具类
 *
 * <p>
 * 用于将长文本按指定最大长度切割为多个 chunk，
 * 并在相邻 chunk 之间保留一定的字符重叠，
 * 以减少语义在切割边界处的丢失。
 * </p>
 *
 * <p>
 * 常用于：
 * <ul>
 *     <li>RAG 文档切块</li>
 *     <li>Embedding 前文本预处理</li>
 * </ul>
 * </p>
 */
public class TextSplitter {

    private TextSplitter() {
    }

    /**
     * 按最大字符数切割文本（支持重叠）
     *
     * @param text 原始文本
     * @param chunkSize 每个 chunk 的最大字符数，必须 &gt; 0
     * @param overlap 相邻 chunk 之间的重叠字符数，必须 &gt;= 0 且 &lt; chunkSize
     * @return 切割后的文本块列表，按原文顺序排列
     */
    public static List<String> split(
            String text,
            int chunkSize,
            int overlap
    ) {
        /* ---------- 基础校验 ---------- */

        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be greater than 0");
        }

        if (overlap < 0) {
            throw new IllegalArgumentException("overlap must be greater than or equal to 0");
        }

        if (overlap >= chunkSize) {
            throw new IllegalArgumentException(
                    "overlap must be smaller than chunkSize"
            );
        }

        /* ---------- 切割逻辑 ---------- */

        List<String> chunks = new ArrayList<>();

        int textLength = text.length();
        int start = 0;

        int step = chunkSize - overlap;
        if (step <= 0) {
            throw new IllegalStateException(
                    "Invalid step size, possible infinite loop: step=" + step
            );
        }

        while (start < textLength) {
            int end = Math.min(start + chunkSize, textLength);
            chunks.add(text.substring(start, end));
            start += step;
        }

        return chunks;
    }
}
```

### 使用方法

```java
package io.github.atengk.milvus;

import cn.hutool.core.io.file.PathUtil;
import io.github.atengk.milvus.service.FileVectorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

@SpringBootTest
public class FileVectorTests {

    @Autowired
    private FileVectorService fileVectorService;

    private static final String COLLECTION = "test_collection";

    @Test
    void test1() {
        Path filepath = Paths.get("d:/Temp/pdf", "demo_more.pdf");

        HashMap<String, Object> metadata = new HashMap<>();
        metadata.put("author", "阿腾");
        metadata.put("date", "20260208");
        fileVectorService.ingest(
                COLLECTION,
                filepath.getFileName().toString(),
                PathUtil.getInputStream(filepath),
                metadata
        );
    }

}

```



## 使用 OpenAI 的 EmbeddingModel

### 基础配置

**添加依赖**

```xml
        <!-- Spring AI - OpenAI 依赖 -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-openai</artifactId>
            <version>1.1.2</version>
        </dependency>
```

**创建配置**

```yaml
---
# Spring AI 配置
spring:
  ai:
    openai:
      base-url: https://api.chatanywhere.tech
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o-mini
```



### FileVectorService接口实现

FileVectorService接口实现OpenAiEmbeddingService

```java
package io.github.atengk.milvus.service.impl;

import io.github.atengk.milvus.service.EmbeddingService;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 基于 OpenAI 的 Embedding 服务实现
 *
 * <p>
 * 说明：
 * <ul>
 *     <li>内部通过 Spring AI 的 EmbeddingModel 调用 OpenAI Embeddings API</li>
 *     <li>embed(text) 会自动委托到批量接口</li>
 *     <li>dimension 在首次调用后缓存</li>
 * </ul>
 */
@Service
public class OpenAiEmbeddingService implements EmbeddingService {

    /**
     * Spring AI 抽象的 EmbeddingModel
     */
    private final EmbeddingModel embeddingModel;

    /**
     * embedding 维度缓存
     */
    private volatile Integer dimensionCache;

    public OpenAiEmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * 单条文本 embedding
     */
    @Override
    public List<Float> embed(String text) {
        if (ObjectUtils.isEmpty(text)) {
            return Collections.emptyList();
        }

        List<List<Float>> result = embedBatch(Collections.singletonList(text));
        return result.isEmpty() ? Collections.emptyList() : result.get(0);
    }

    /**
     * 批量文本 embedding
     */
    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        if (ObjectUtils.isEmpty(texts)) {
            return Collections.emptyList();
        }

        EmbeddingRequest request = new EmbeddingRequest(texts, null);

        EmbeddingResponse response = embeddingModel.call(request);

        List<Embedding> embeddings = response.getResults();
        if (ObjectUtils.isEmpty(embeddings)) {
            return Collections.emptyList();
        }

        List<List<Float>> vectors = embeddings.stream()
                .map(Embedding::getOutput)
                .filter(Objects::nonNull)
                .map(this::toFloatList)
                .collect(Collectors.toList());

        cacheDimensionIfNecessary(vectors);

        return vectors;
    }

    /**
     * 将 float[] 转换为 List<Float>
     */
    private List<Float> toFloatList(float[] vector) {
        List<Float> result = new ArrayList<>(vector.length);
        for (float v : vector) {
            result.add(v);
        }
        return result;
    }

    /**
     * 返回 embedding 向量维度
     */
    @Override
    public int dimension() {
        if (dimensionCache != null) {
            return dimensionCache;
        }

        List<Float> vector = embed("dimension_probe");
        if (ObjectUtils.isEmpty(vector)) {
            throw new IllegalStateException("Failed to determine embedding dimension");
        }

        dimensionCache = vector.size();
        return dimensionCache;
    }

    /**
     * 缓存 embedding 维度
     */
    private void cacheDimensionIfNecessary(List<List<Float>> vectors) {
        if (dimensionCache != null) {
            return;
        }

        if (ObjectUtils.isEmpty(vectors)) {
            return;
        }

        List<Float> first = vectors.get(0);
        if (!ObjectUtils.isEmpty(first)) {
            dimensionCache = first.size();
        }
    }
}

```

### 修改 FileVectorServiceImpl

主要是修改了批量获取embedding，节省调用AI模型的token

```java
package io.github.atengk.milvus.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.google.gson.JsonObject;
import io.github.atengk.milvus.entity.VectorDocument;
import io.github.atengk.milvus.service.EmbeddingService;
import io.github.atengk.milvus.service.FileVectorService;
import io.github.atengk.milvus.service.MilvusService;
import io.github.atengk.milvus.util.TextSplitter;
import io.github.atengk.milvus.util.TikaUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class FileVectorServiceImpl implements FileVectorService {

    private static final int CHUNK_SIZE = 800;
    private static final int CHUNK_OVERLAP = 150;

    private final MilvusService milvusService;
    private final EmbeddingService embeddingService;

    public FileVectorServiceImpl(
            MilvusService milvusService,
            @Qualifier("openAiEmbeddingService") EmbeddingService embeddingService
    ) {
        this.milvusService = milvusService;
        this.embeddingService = embeddingService;
    }

    @Override
    public void ingest(
            String collectionName,
            String fileName,
            InputStream inputStream,
            Map<String, Object> externalMetadata
    ) {
        try {
            /* ========================= file fingerprint ========================= */

            byte[] fileBytes = inputStream.readAllBytes();
            String documentId = DigestUtil.sha256Hex(fileBytes);

            String expr = String.format(
                    "metadata[\"documentId\"] == \"%s\"",
                    documentId
            );

            if (milvusService.existsByExpr(collectionName, expr)) {
                log.warn(
                        "文件重复，跳过写入: fileName={}, documentId={}",
                        fileName,
                        documentId
                );
                return;
            }

            /* ========================= tika parse ========================= */

            InputStream tikaInputStream = new ByteArrayInputStream(fileBytes);
            TikaUtil.TikaResult tikaResult = TikaUtil.parseAll(tikaInputStream, -1);

            String content = tikaResult.getContent();
            if (StrUtil.isBlank(content)) {
                log.warn("文件内容为空，跳过写入: fileName={}", fileName);
                return;
            }

            /* ========================= text split ========================= */

            List<String> chunks = TextSplitter.split(
                    content,
                    CHUNK_SIZE,
                    CHUNK_OVERLAP
            );

            if (chunks.isEmpty()) {
                log.warn("文本切割结果为空，跳过写入: fileName={}", fileName);
                return;
            }

            /* ========================= batch embedding ========================= */

            List<String> chunkTexts = new ArrayList<>(chunks.size());
            for (String chunk : chunks) {
                if (StrUtil.isNotBlank(chunk)) {
                    chunkTexts.add(chunk);
                }
            }

            List<List<Float>> embeddings =
                    embeddingService.embedBatch(chunkTexts);

            if (embeddings.size() != chunkTexts.size()) {
                throw new IllegalStateException(
                        "Embedding 结果数量不匹配: texts="
                                + chunkTexts.size()
                                + ", embeddings="
                                + embeddings.size()
                );
            }

            /* ========================= build vector documents ========================= */

            int chunkTotal = chunkTexts.size();
            List<VectorDocument> documents = new ArrayList<>(chunkTotal);

            int offsetCursor = 0;

            for (int i = 0; i < chunkTotal; i++) {

                String chunk = chunkTexts.get(i);
                List<Float> vector = embeddings.get(i);

                String chunkId = UUID.randomUUID().toString();

                int startOffset = offsetCursor;
                int endOffset = startOffset + chunk.length();
                offsetCursor = Math.max(endOffset - CHUNK_OVERLAP, startOffset);

                VectorDocument document = new VectorDocument();
                document.setId(chunkId);
                document.setContent(chunk);
                document.setEmbedding(vector);

                JsonObject metadata = new JsonObject();

                /* ---------- document level ---------- */
                metadata.addProperty("documentId", documentId);
                metadata.addProperty("fileHash", documentId);
                metadata.addProperty("fileName", fileName);

                /* ---------- chunk level ---------- */
                metadata.addProperty("chunkId", chunkId);
                metadata.addProperty("chunkIndex", i);
                metadata.addProperty("chunkTotal", chunkTotal);
                metadata.addProperty("startOffset", startOffset);
                metadata.addProperty("endOffset", endOffset);
                metadata.addProperty("chunkSize", CHUNK_SIZE);
                metadata.addProperty("chunkOverlap", CHUNK_OVERLAP);

                /* ---------- tika metadata ---------- */
                if (tikaResult.getMetadata() != null) {
                    tikaResult.getMetadata().forEach(metadata::addProperty);
                }

                /* ---------- external metadata ---------- */
                if (externalMetadata != null) {
                    externalMetadata.forEach(
                            (k, v) -> metadata.addProperty(k, String.valueOf(v))
                    );
                }

                document.setMetadata(metadata);
                documents.add(document);
            }

            /* ========================= write to milvus ========================= */

            if (!documents.isEmpty()) {
                milvusService.add(collectionName, documents);
            }

            log.info(
                    "文件写入 Milvus 完成: fileName={}, documentId={}, chunks={}",
                    fileName,
                    documentId,
                    documents.size()
            );

        } catch (Exception e) {
            log.error("写入 Milvus 失败: fileName={}", fileName, e);
            throw new RuntimeException("写入 Milvus 失败", e);
        }
    }
}
```

### 写入数据到 Milvus

基于 OpenAI EmbeddingModel（1536）写入数据到 Milvus

```java
package io.github.atengk.milvus;

import cn.hutool.core.io.file.PathUtil;
import io.github.atengk.milvus.service.FileVectorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

@SpringBootTest
public class FileVectorTests {

    @Autowired
    private FileVectorService fileVectorService;

    private static final String COLLECTION = "test_collection";

    @Test
    void test1() {
        Path filepath = Paths.get("D:\\temp", "demo.docx");

        HashMap<String, Object> metadata = new HashMap<>();
        metadata.put("author", "阿腾");
        metadata.put("date", "20260208");
        fileVectorService.ingest(
                COLLECTION,
                filepath.getFileName().toString(),
                PathUtil.getInputStream(filepath),
                metadata
        );
    }

}
```

