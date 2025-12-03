package local.ateng.java.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
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

    @Test
    public void test02() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        // 序列化
        UserEnumEntity user = new UserEnumEntity();
        user.setId(1L);
        user.setUserEnum(UserEnum.ENABLE);

        String str = mapper.writeValueAsString(user);
        System.out.println(str);
        // {"id":1,"userEnum":"开启"}

        // 反序列化
        String str2 = "{\"id\":1,\"userEnum\":1}";
        UserEnumEntity user2 = mapper.readValue(str2, UserEnumEntity.class);
        System.out.println(user2);
        // UserEnumEntity(id=1, userEnum=ENABLE)
    }

    @Data
    public static class UserEnumEntity {
        private Long id;
        private UserEnum userEnum;
    }

    // 序列化和反序列化枚举
    public enum UserEnum {

        ENABLE(1, "开启"),
        DISABLE(2, "关闭");

        private final Integer code;
        @JsonValue  // 序列化时输出 name 字段
        private final String name;

        UserEnum(int code, String name) {
            this.code = code;
            this.name = name;
        }

        public Integer getCode() {
            return code;
        }

        public String getName() {
            return this.name;
        }

        @JsonCreator  // 反序列化根据 code 解析枚举
        public static UserEnum fromCode(Integer code) {
            if (code == null) {
                return null;
            }
            for (UserEnum e : values()) {
                if (e.code.equals(code)) {
                    return e;
                }
            }
            return null;
        }
    }

}
