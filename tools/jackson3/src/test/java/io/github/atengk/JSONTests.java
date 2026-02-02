package io.github.atengk;

import io.github.atengk.entity.MyUser;
import io.github.atengk.init.InitData;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

public class JSONTests {

    private final JsonMapper mapper = JsonMapper.builder().build();

    /**
     * POJO → JsonNode
     */
    @Test
    void testPojoToJsonNode() {
        MyUser user = InitData.getDataList(1).getFirst();

        JsonNode node = mapper.valueToTree(user);

        System.out.println(node);
    }

    /**
     * JSON String → JsonNode
     */
    @Test
    void testJsonToJsonNode() {
        String json = mapper.writeValueAsString(InitData.getDataList(1).getFirst());

        JsonNode node = mapper.readTree(json);

        System.out.println(node);
    }

    /**
     * 对象 → JSON 字符串
     */
    @Test
    void testObjectToJson() {

        MyUser myUser = InitData.getDataList().getFirst();

        String json = mapper.writeValueAsString(myUser);
        System.out.println(json);
    }

    /**
     * JSON 字符串 → 对象
     */
    @Test
    void testJsonToObject() {
        String json = """
                {
                  "id": 1,
                  "name": "Ateng",
                  "age": 18
                }
                """;

        MyUser user = mapper.readValue(json, MyUser.class);
        System.out.println(user);
    }

    /**
     * Map → JSON
     */
    @Test
    void testMapToJson() {
        Map<String, Object> map = Map.of(
                "id", 1,
                "name", "Ateng",
                "active", true
        );

        String json = mapper.writeValueAsString(map);
        System.out.println(json);
    }

    /**
     * JSON → Map<String, Object>
     */
    @Test
    void testJsonToMap() {
        String json = """
                {
                  "id": 1,
                  "name": "Ateng",
                  "active": true
                }
                """;

        Map<String, Object> map = mapper.readValue(
                json,
                new TypeReference<Map<String, Object>>() {
                }
        );

        System.out.println(map);
    }

    /**
     * JSON 是否合法
     */
    @Test
    void testIsValidJson() {
        String json = "{ \"id\": 1, \"name\": \"Ateng\" }";

        boolean valid;
        try {
            mapper.readTree(json);
            valid = true;
        } catch (Exception e) {
            valid = false;
        }

        System.out.println("valid = " + valid);
    }

}
