# Jackson 3

## 版本信息

| 组件       | 版本   |
| ---------- | ------ |
| JDK        | 25     |
| Maven      | 3.9.12 |
| SpringBoot | 4.0.2  |
| Jackson    | 3.0.4  |



## 基础配置

**添加依赖**

```xml
<properties>
    <jackson.version>3.0.4</jackson.version>
</properties>

<!-- 项目依赖 -->
<dependencies>
    <!-- Jackson 依赖 -->
    <dependency>
        <groupId>tools.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>${jackson.version}</version>
    </dependency>
</dependencies>
```



## JSON 对象

**测试类**

```java
public class JSONTests {

    private final JsonMapper mapper = JsonMapper.builder().build();

}
```

**对象转JsonNode**

```java
    /**
     * POJO → JsonNode
     */
    @Test
    void testPojoToJsonNode() {
        MyUser user = InitData.getDataList(1).getFirst();

        JsonNode node = mapper.valueToTree(user);

        System.out.println(node);
    }
```

**JSON String转JsonNode**

```java
    /**
     * JSON String → JsonNode
     */
    @Test
    void testJsonToJsonNode() {
        String json = mapper.writeValueAsString(InitData.getDataList(1).getFirst());

        JsonNode node = mapper.readTree(json);

        System.out.println(node);
    }
```

**对象转JSON字符串**

```java
    /**
     * 对象 → JSON 字符串
     */
    @Test
    void testObjectToJson() {

        MyUser myUser = InitData.getDataList().getFirst();

        String json = mapper.writeValueAsString(myUser);
        System.out.println(json);
    }
```

**JSON字符串转对象**

```java
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
```

**Map转JSON字符串**

```java
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
```

**JSON字符串转Map**

```java
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
```

**JSON 是否合法**

```java
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
```



## JSON 列表

**测试类**

```java
public class JSONArrayTests {

    private final JsonMapper mapper = JsonMapper.builder().build();

}
```

**对象列表转JsonNode**

```java
    /**
     * POJO → JsonNode
     */
    @Test
    void testPojoToJsonNode() {
        List<MyUser> userList = InitData.getDataList(3);

        JsonNode node = mapper.valueToTree(userList);

        System.out.println(node);
    }
```

**JSON字符串转JsonNode**

```java
    /**
     * JSON String → JsonNode
     */
    @Test
    void testJsonToJsonNode() {
        String json = mapper.writeValueAsString(InitData.getDataList(3));

        JsonNode node = mapper.readTree(json);

        System.out.println(node);
    }
```

**对象列表转JSON字符串**

```java
    /**
     * List → JSON
     */
    @Test
    void testListToJson() {
        List<MyUser> list = InitData.getDataList(3);

        String json = mapper.writeValueAsString(list);
        System.out.println(json);
    }
```

**JSON字符串转对象列表**

```java
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
```



## JSON Pointer

**JSON Pointer** 是一个 **RFC 6901 标准**，
 用来**精确定位 JSON 中某一个节点**的路径语法。

**测试类**

```java
public class JSONPointerTests {

    private final JsonMapper mapper = JsonMapper.builder().build();

}
```

**访问数组元素**

```java
    /**
     * 访问数组元素：$[0]
     */
    @Test
    void testArrayIndex() {
        JsonNode root = mapper.valueToTree(List.of("A", "B", "C"));

        JsonNode node = root.at("/0");

        System.out.println(node.asString()); // A
    }
```

**访问对象字段**

```java
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
```

**对象 + 数组混合**

```java
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
```

**多层嵌套访问**

```java
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
```

**不存在路径**

```java
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
```

**对比 get()：安全访问（推荐）**

```java
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
```



