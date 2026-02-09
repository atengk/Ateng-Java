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
