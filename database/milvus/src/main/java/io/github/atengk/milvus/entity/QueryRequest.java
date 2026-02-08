package io.github.atengk.milvus.entity;

import lombok.Data;

import java.util.List;

@Data
public class QueryRequest {

    private String collectionName;

    private String expr;

    private List<String> outputFields;

}

