package io.github.atengk;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

public class JSONPointerTests {

    private final JsonMapper mapper = JsonMapper.builder().build();

    /**
     * 访问数组元素：$[0]
     */
    @Test
    void testArrayIndex() {
        JsonNode root = mapper.valueToTree(List.of("A", "B", "C"));

        JsonNode node = root.at("/0");

        System.out.println(node.asString()); // A
    }

    /**
     * 访问对象字段：$.user.name
     */
    @Test
    void testObjectField() {
        String json = """
                {
                  "user": {
                    "id": 1,
                    "name": "Tom"
                  }
                }
                """;

        JsonNode root = mapper.readTree(json);

        JsonNode nameNode = root.at("/user/name");

        System.out.println(nameNode.asString()); // Tom
    }

    /**
     * 对象 + 数组混合：$.users[1].name
     */
    @Test
    void testObjectAndArray() {
        String json = """
                {
                  "users": [
                    { "id": 1, "name": "Tom" },
                    { "id": 2, "name": "Jerry" }
                  ]
                }
                """;

        JsonNode root = mapper.readTree(json);

        JsonNode nameNode = root.at("/users/1/name");

        System.out.println(nameNode.asString()); // Jerry
    }

    /**
     * 多层嵌套访问：$.data.list[0].value
     */
    @Test
    void testDeepNested() {
        String json = """
                {
                  "data": {
                    "list": [
                      { "value": 100 },
                      { "value": 200 }
                    ]
                  }
                }
                """;

        JsonNode root = mapper.readTree(json);

        JsonNode valueNode = root.at("/data/list/0/value");

        System.out.println(valueNode.asInt()); // 100
    }

    /**
     * 不存在路径（不会 NPE）
     */
    @Test
    void testMissingPath() {
        JsonNode root = mapper.valueToTree(List.of("A", "B"));

        JsonNode node = root.at("/10");

        System.out.println(node.isMissingNode()); // true
        System.out.println(node.asString("DEFAULT")); // DEFAULT
    }

    /**
     * 对比 get()：安全访问（推荐）
     */
    @Test
    void testAtVsGet() {
        String json = """
                {
                  "user": {}
                }
                """;

        JsonNode root = mapper.readTree(json);

        // 不存在字段
        JsonNode byAt = root.at("/user/name");
        JsonNode byGet = root.get("user").get("name");
        String byGetStr = root.get("user").get("name").asString(); // 这里会 NPE

        System.out.println(byAt.isMissingNode()); // true
        System.out.println(byGet); // null
    }

}
