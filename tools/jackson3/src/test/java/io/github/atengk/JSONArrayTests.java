package io.github.atengk;

import io.github.atengk.entity.MyUser;
import io.github.atengk.init.InitData;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

public class JSONArrayTests {

    private final JsonMapper mapper = JsonMapper.builder().build();

    /**
     * POJO → JsonNode
     */
    @Test
    void testPojoToJsonNode() {
        List<MyUser> userList = InitData.getDataList(3);

        JsonNode node = mapper.valueToTree(userList);

        System.out.println(node);
    }

    /**
     * JSON String → JsonNode
     */
    @Test
    void testJsonToJsonNode() {
        String json = mapper.writeValueAsString(InitData.getDataList(3));

        JsonNode node = mapper.readTree(json);

        System.out.println(node);
    }

    /**
     * List → JSON
     */
    @Test
    void testListToJson() {
        List<MyUser> list = InitData.getDataList(3);

        String json = mapper.writeValueAsString(list);
        System.out.println(json);
    }

    /**
     * JSON → List<MyUser>
     */
    @Test
    void testJsonToList() {
        String json = mapper.writeValueAsString(InitData.getDataList(3));

        List<MyUser> list = mapper.readValue(
                json,
                new TypeReference<List<MyUser>>() {}
        );

        list.forEach(System.out::println);
    }

}
