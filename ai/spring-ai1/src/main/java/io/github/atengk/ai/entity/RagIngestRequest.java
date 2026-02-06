package io.github.atengk.ai.entity;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RagIngestRequest {

    private List<String> texts;

    private Map<String, Object> metadata;

}