# Milvus

Milvus 是一款开源的向量数据库，专为大规模相似度搜索与 AI 应用设计，支持高维向量高效存储、索引与检索。常用于推荐系统、语义搜索、图像/视频检索与 RAG 场景，具备高性能、可扩展、云原生等特点。
官网：[https://milvus.io](https://milvus.io)



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

### 接口实现PdfFileVectorService

```java
package io.github.atengk.milvus.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.milvus.entity.VectorDocument;
import io.github.atengk.milvus.service.EmbeddingService;
import io.github.atengk.milvus.service.FileVectorService;
import io.github.atengk.milvus.service.MilvusService;
import io.github.atengk.milvus.util.TextSplitter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
public class PdfFileVectorService implements FileVectorService {

    private static final int CHUNK_SIZE = 800;
    private static final int CHUNK_OVERLAP = 150;

    private final MilvusService milvusService;
    private final EmbeddingService embeddingService;

    public PdfFileVectorService(
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
            Map<String, Object> metadata
    ) {
        try (PDDocument document = PDDocument.load(inputStream)) {

            PDFTextStripper stripper = new PDFTextStripper();
            String fullText = stripper.getText(document);

            if (fullText == null || fullText.isBlank()) {
                log.warn("PDF 内容为空: {}", fileName);
                return;
            }

            // 文本切割
            List<String> chunks = TextSplitter.split(
                    fullText,
                    CHUNK_SIZE,
                    CHUNK_OVERLAP
            );

            List<VectorDocument> docs = new ArrayList<>();

            // 文档级唯一 ID
            String docId = UUID.randomUUID().toString();
            int totalChunks = chunks.size();

            // 用于计算 offset
            int cursor = 0;

            for (int i = 0; i < chunks.size(); i++) {

                String chunk = chunks.get(i);
                if (StrUtil.isBlank(chunk)) {
                    continue;
                }

                int startOffset = cursor;
                int endOffset = cursor + chunk.length();
                cursor = endOffset - CHUNK_OVERLAP;

                VectorDocument doc = new VectorDocument();
                doc.setId(UUID.randomUUID().toString());
                doc.setContent(chunk);
                doc.setEmbedding(embeddingService.embed(chunk));

                // metadata
                com.google.gson.JsonObject meta = new com.google.gson.JsonObject();

                meta.addProperty("fileName", fileName);
                meta.addProperty("chunkIndex", i);

                meta.addProperty("docId", docId);
                meta.addProperty("chunkTotal", totalChunks);
                meta.addProperty("fileType", "pdf");
                meta.addProperty("startOffset", startOffset);
                meta.addProperty("endOffset", endOffset);
                meta.addProperty("chunkSize", CHUNK_SIZE);
                meta.addProperty("chunkOverlap", CHUNK_OVERLAP);

                // 外部 metadata 透传
                if (metadata != null) {
                    metadata.forEach((k, v) ->
                            meta.addProperty(k, String.valueOf(v))
                    );
                }

                doc.setMetadata(meta);
                docs.add(doc);
            }

            milvusService.add(collectionName, docs);

            log.info(
                    "PDF 写入完成: file={}, docId={}, chunks={}",
                    fileName,
                    docId,
                    docs.size()
            );

        } catch (Exception e) {
            throw new RuntimeException("PDF 写入 Milvus 失败", e);
        }
    }

}

```

### 创建文本切割工具

```java
package io.github.atengk.milvus.util;

import java.util.ArrayList;
import java.util.List;

public class TextSplitter {

    /**
     * 按最大字符数切割，带重叠
     */
    public static List<String> split(
            String text,
            int chunkSize,
            int overlap
    ) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be > 0");
        }
        if (overlap < 0) {
            throw new IllegalArgumentException("overlap must be >= 0");
        }
        if (overlap >= chunkSize) {
            throw new IllegalArgumentException(
                    "overlap must be smaller than chunkSize"
            );
        }

        List<String> chunks = new ArrayList<>();

        int start = 0;
        int textLength = text.length();

        while (start < textLength) {
            int end = Math.min(start + chunkSize, textLength);
            chunks.add(text.substring(start, end));

            start += (chunkSize - overlap);
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

