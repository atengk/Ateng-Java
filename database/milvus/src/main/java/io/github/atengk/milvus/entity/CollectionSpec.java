package io.github.atengk.milvus.entity;

import lombok.Data;

@Data
public class CollectionSpec {

    private String collectionName;

    private int dimension;

    private boolean autoId;

}

