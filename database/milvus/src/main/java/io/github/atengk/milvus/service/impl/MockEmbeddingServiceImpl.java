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

