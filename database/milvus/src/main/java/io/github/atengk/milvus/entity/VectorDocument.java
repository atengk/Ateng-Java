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

