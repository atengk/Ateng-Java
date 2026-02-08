package io.github.atengk.milvus;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.atengk.milvus.entity.*;
import io.github.atengk.milvus.service.EmbeddingService;
import io.github.atengk.milvus.service.MilvusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

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

    /* ========================= maintenance ========================= */

    @Test
    void testFlush() {
        milvusService.flush(COLLECTION);
    }
}

