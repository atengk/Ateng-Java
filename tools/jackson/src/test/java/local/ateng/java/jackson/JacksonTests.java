package local.ateng.java.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class JacksonTests {

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void test() throws IOException {
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("name", "Alice");  // 字符串
        objectNode.put("age", 25);        // 数字
        objectNode.put("isStudent", true);  // 布尔值
// 创建另一个 ObjectNode
        ObjectNode addressNode = objectMapper.createObjectNode();
        addressNode.put("city", "New York");
        addressNode.put("zipcode", "10001");

// 将 addressNode 添加为字段
        objectNode.set("address", addressNode);

// 创建 ArrayNode
        ArrayNode phoneNumbers = objectMapper.createArrayNode();
        phoneNumbers.add("123-456-7890");
        phoneNumbers.add("987-654-3210");

// 将 ArrayNode 添加为字段
        objectNode.set("phoneNumbers", phoneNumbers);

        ArrayNode arrayNode = objectMapper.createArrayNode();
        System.out.println(objectNode);
        System.out.println(arrayNode);
    }


}
