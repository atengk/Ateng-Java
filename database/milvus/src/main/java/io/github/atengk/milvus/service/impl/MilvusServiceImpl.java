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
