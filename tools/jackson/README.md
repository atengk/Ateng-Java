# Jackson

Jackson是一个高效的Java JSON处理库，广泛用于将Java对象与JSON数据进行相互转换。它提供了强大的数据绑定功能，可以将JSON解析为Java对象，或将Java对象序列化为JSON格式。

- [官网链接](https://github.com/FasterXML/jackson)



## ObjectNode 用法

`ObjectNode` 代表一个 JSON 对象，可以用来存储键值对，并允许进行动态操作。

### 创建 `ObjectNode`

普通用法

```java
ObjectMapper mapper = new ObjectMapper();
ObjectNode objectNode = mapper.createObjectNode();
```

Spring用法

```java
@Bean
public ObjectMapper objectMapper() {
    return new ObjectMapper();
}
@Autowired
private ObjectMapper objectMapper;
```

### 向 `ObjectNode` 中添加属性

可以使用 `put()` 方法添加基本类型的字段（例如：字符串、整数等）：

```java
objectNode.put("name", "John");
objectNode.put("age", 30);
objectNode.put("isStudent", true);
```

对于其他类型（如 `Date` 或对象），可以使用 `putPOJO()` 方法：

```java
objectNode.putPOJO("birthdate", new Date());
```

### 向 `ObjectNode` 中嵌套另一个 `ObjectNode`
你可以通过 `set()` 方法将一个 `ObjectNode` 放入另一个 `ObjectNode` 中，从而实现嵌套结构。

```java
ObjectNode addressNode = mapper.createObjectNode();
addressNode.put("street", "123 Main St");
addressNode.put("city", "New York");
objectNode.set("address", addressNode);
```

### 修改 `ObjectNode` 中的字段
直接调用 `put()` 方法更新字段的值：

```java
objectNode.put("name", "Jane");  // 修改 name 为 Jane
```

### 删除 `ObjectNode` 中的字段
使用 `remove()` 方法删除字段：

```java
objectNode.remove("isStudent");  // 删除 isStudent 字段
```

### 输出 `ObjectNode` 的 JSON 字符串
```java
String jsonString = objectNode.toString();
System.out.println(jsonString);  // 输出 JSON 字符串
```

### 完整示例：创建、修改和嵌套 `ObjectNode`
```java
ObjectMapper mapper = new ObjectMapper();
ObjectNode objectNode = mapper.createObjectNode();

// 添加字段
objectNode.put("name", "John");
objectNode.put("age", 30);

// 创建嵌套 ObjectNode
ObjectNode addressNode = mapper.createObjectNode();
addressNode.put("street", "123 Main St");
addressNode.put("city", "New York");

// 将嵌套对象添加到主对象
objectNode.set("address", addressNode);

// 修改字段
objectNode.put("name", "Jane");

// 删除字段
objectNode.remove("age");

// 输出结果
System.out.println(objectNode.toString());
```

## ArrayNode 用法

`ArrayNode` 代表一个 JSON 数组，你可以向其中添加任何类型的元素，包括基本数据类型和对象。

### 创建 `ArrayNode`
```java
ObjectMapper mapper = new ObjectMapper();
ArrayNode arrayNode = mapper.createArrayNode();
```

### 向 `ArrayNode` 中添加元素
可以使用 `add()` 方法向数组中添加元素，可以是基本类型、对象或其他 `ArrayNode`：

```java
arrayNode.add("apple");      // 添加字符串
arrayNode.add(123);          // 添加整数
arrayNode.add(45.67);        // 添加浮点数
arrayNode.add(true);         // 添加布尔值
```

你还可以添加一个 `ObjectNode` 或 `ArrayNode` 作为元素：

```java
ObjectNode personNode = mapper.createObjectNode();
personNode.put("name", "John");
personNode.put("age", 30);

arrayNode.add(personNode);   // 添加一个 ObjectNode
```

### 修改 `ArrayNode` 中的元素
`ArrayNode` 支持通过索引修改元素，使用 `set()` 方法：

```java
arrayNode.set(0, new TextNode("orange"));  // 修改索引 0 的元素
```

### 删除 `ArrayNode` 中的元素
使用 `remove()` 方法根据索引删除元素：

```java
arrayNode.remove(1);  // 删除索引为 1 的元素
```

### 输出 `ArrayNode` 的 JSON 字符串
```java
String jsonArray = arrayNode.toString();
System.out.println(jsonArray);  // 输出 JSON 数组字符串
```

### 完整示例：创建和修改 `ArrayNode`
```java
ObjectMapper mapper = new ObjectMapper();
ArrayNode arrayNode = mapper.createArrayNode();

// 添加元素
arrayNode.add("apple");
arrayNode.add(123);
arrayNode.add(45.67);
arrayNode.add(true);

// 添加嵌套 ObjectNode
ObjectNode personNode = mapper.createObjectNode();
personNode.put("name", "John");
personNode.put("age", 30);
arrayNode.add(personNode);

// 修改元素
arrayNode.set(1, new TextNode("banana"));  // 修改第 1 个元素

// 删除元素
arrayNode.remove(2);  // 删除第 2 个元素

// 输出结果
System.out.println(arrayNode.toString());
```





## 常用注解及使用方法

### 常用注解

Jackson 提供了一系列注解用于控制 JSON 序列化和反序列化行为，下面是常见的 Jackson 注解及其示例。

---

#### 1. `@JsonProperty` - 指定 JSON 关键字名称

**作用**：可以用于字段、getter 或 setter 方法，指定 JSON 中的属性名称。

**示例**：

```java
import com.fasterxml.jackson.annotation.JsonProperty;

public class User {
    private int id;

    @JsonProperty("full_name")
    private String name;

    // 省略构造方法、getter 和 setter
}
```

**序列化结果**：

```json
{
    "id": 1,
    "full_name": "Tom"
}
```

---

#### 2. `@JsonIgnore` - 忽略字段

**作用**：用于标记不需要被序列化或反序列化的字段。

**示例**：

```java
import com.fasterxml.jackson.annotation.JsonIgnore;

public class User {
    private int id;
    private String name;

    @JsonIgnore
    private String password;
}
```

**序列化结果**：

```json
{
    "id": 1,
    "name": "Tom"
}
```

**注意**：`password` 字段不会出现在 JSON 结果中。

---

#### 3. `@JsonIgnoreProperties` - 忽略多个字段

**作用**：用于类级别，忽略 JSON 解析时的多个字段。

**示例**：

```java
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"password", "email"})
public class User {
    private int id;
    private String name;
    private String password;
    private String email;
}
```

**序列化结果**：

```json
{
    "id": 1,
    "name": "Tom"
}
```

**注意**：`password` 和 `email` 不会出现在 JSON 中。

---

#### 4. `@JsonInclude` - 仅包含非空字段

**作用**：用于控制 JSON 仅包含特定的非空字段。

**示例**：

```java
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class User {
    private int id;
    private String name;
    private String email; // 可能为 null
}
```

**当 `email` 为 `null` 时，序列化结果**：

```json
{
    "id": 1,
    "name": "Tom"
}
```

**注意**：`email` 字段不会被序列化。



#### **5. `@JsonFormat` - Jackson 格式化注解**  

`@JsonFormat` 主要用于控制 Java 对象在 **序列化（对象 → JSON）** 和 **反序列化（JSON → 对象）** 时的格式，常用于 **日期、Long 类型、枚举类型等字段**。

---

##### **📌 用法 1：格式化日期时间**（适用于 `Date` / `LocalDateTime` / `LocalDate`）  

**作用**：  

- 指定日期格式（`pattern` 参数）  
- 指定时区（`timezone` 参数）  

**示例**：

```java
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.Date;

public class User {
    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @JsonFormat(pattern = "yyyy/MM/dd HH:mm")
    private LocalDateTime lastLogin;
}
```

**序列化结果**：

```json
{
    "name": "Tom",
    "createTime": "2024-03-05 14:30:00",
    "lastLogin": "2024/03/05 14:30"
}
```

📌 **说明**：

- `pattern = "yyyy-MM-dd HH:mm:ss"` **格式化日期输出**
- `timezone = "GMT+8"` **解决时区问题**
- `LocalDateTime` 也可以直接使用 `@JsonFormat`

---

##### **📌 用法 2：将 `Long` 类型转换为 `String`**（避免前端精度丢失）  

**作用**：

- **防止 `Long` 精度丢失**（JavaScript 处理大数时可能会出现误差）  

**示例**：

```java
import com.fasterxml.jackson.annotation.JsonFormat;

public class Order {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long orderId;
}
```

**序列化结果**：

```json
{
    "orderId": "1234567890123456789"
}
```

📌 **说明**：

- `shape = JsonFormat.Shape.STRING` **将 Long 类型转换为 String**
- 避免前端（如 JavaScript）处理大数时的精度问题

---

##### **📌 用法 3：格式化 `Enum` 枚举类型**（可序列化为 `String`）  

**作用**：

- 指定 **枚举值** 的序列化方式  
- 可以让枚举以 `name()` 或 `ordinal()` 输出  

**示例**：

```java
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum Status {
    SUCCESS(200, "成功"),
    ERROR(500, "失败");

    private final int code;
    private final String message;
}
```

**序列化结果**：

```json
{
    "code": 200,
    "message": "成功"
}
```

📌 **说明**：

- `JsonFormat.Shape.OBJECT` **让枚举类作为对象输出**
- 可以用于 **返回枚举的多个属性**（如 `code` 和 `message`）

**🚀 另一种方式：格式化枚举为 `String`**

```java
@JsonFormat(shape = JsonFormat.Shape.STRING)
private Status status;
```

**序列化后**：

```json
{
    "status": "SUCCESS"
}
```

📌 **说明**：

- `JsonFormat.Shape.STRING` **让枚举类以 `name()` 的形式输出**
- 适用于 **只想输出枚举名** 而不是数值

---

##### **📌 用法 4：忽略空值（控制 JSON 输出）**

**作用**：

- 通过 `JsonFormat.Shape.STRING` 处理空值  
- 适用于 `null` 值的处理  

**示例**：

```java
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

public class User {
    private String name;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;
}
```

**序列化结果**（当 `id` 为 `null`）：

```json
{
    "name": "Tom"
}
```

📌 **说明**：

- `JsonInclude.Include.NON_NULL` **当值为 `null` 时不输出**
- `@JsonFormat(shape = JsonFormat.Shape.STRING)` **可配合 `Long` 类型使用**

---

##### **📌 用法 5：格式化布尔类型**

**作用**：

- 让 `Boolean` 类型以 `"true"` / `"false"` 字符串输出  

**示例**：

```java
import com.fasterxml.jackson.annotation.JsonFormat;

public class User {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Boolean active;
}
```

**序列化结果**：

```json
{
    "active": "true"
}
```

📌 **说明**：

- `shape = JsonFormat.Shape.STRING` **让 `Boolean` 以 `String` 形式输出**
- 适用于某些需要 `Boolean` 作为 `"true"` / `"false"` 处理的 API

---

#### 6. `@JsonCreator` - 反序列化时指定构造方法

**作用**：Jackson 默认使用无参构造方法进行反序列化，`@JsonCreator` 可以让 Jackson 使用特定的构造方法。

**示例**：

```java
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class User {
    private int id;
    private String name;

    @JsonCreator
    public User(@JsonProperty("id") int id, @JsonProperty("name") String name) {
        this.id = id;
        this.name = name;
    }
}
```

**反序列化 JSON**：

```json
{
    "id": 1,
    "name": "Tom"
}
```

**解析后生成的对象**：

```java
User user = objectMapper.readValue(json, User.class);
```

---

#### 7. `@JsonAnySetter` - 动态处理未知属性

**作用**：可以在反序列化时接收 JSON 中额外的字段。

**示例**：

```java
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.HashMap;
import java.util.Map;

public class User {
    private int id;
    private String name;
    private Map<String, Object> additionalProperties = new HashMap<>();

    @JsonAnySetter
    public void setAdditionalProperty(String key, Object value) {
        additionalProperties.put(key, value);
    }
}
```

**反序列化 JSON**：

```json
{
    "id": 1,
    "name": "Tom",
    "age": 25,
    "gender": "male"
}
```

**解析后**：

```java
user.getAdditionalProperties(); // { "age": 25, "gender": "male" }
```

**注意**：`age` 和 `gender` 不在 `User` 类中，但仍然能被存储并使用。

---

#### 8. `@JsonAnyGetter` - 动态序列化额外字段

**作用**：用于将 `Map` 类型的额外字段动态序列化为 JSON。

**示例**：

```java
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import java.util.HashMap;
import java.util.Map;

public class User {
    private int id;
    private String name;
    private Map<String, Object> additionalProperties = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }
}
```

**序列化 JSON**：

```json
{
    "id": 1,
    "name": "Tom",
    "age": 25,
    "gender": "male"
}
```

---

#### 9. `@JsonPropertyOrder` - 指定 JSON 字段顺序

**作用**：指定 JSON 输出时的字段顺序。

**示例**：

```java
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"id", "name", "email"})
public class User {
    private int id;
    private String name;
    private String email;
}
```

**序列化 JSON**：

```json
{
    "id": 1,
    "name": "Tom",
    "email": "tom@example.com"
}
```

---

#### **10. `@JsonSerialize`（自定义序列化）**

**作用**

- `@JsonSerialize(using = CustomSerializer.class)` **用于自定义对象如何转换为 JSON**。
- 适用于 **字段级别** 或 **类级别** 的序列化控制。

**示例：自定义 BigDecimal 序列化（去除多余小数位数）**

```java
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.math.BigDecimal;

// 自定义序列化器：限制 BigDecimal 只保留 2 位小数
public class BigDecimalSerializer extends JsonSerializer<BigDecimal> {
    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.setScale(2, BigDecimal.ROUND_HALF_UP).toString());
    }
}
```

**应用 `@JsonSerialize`**

```java
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class Product {
    private String name;

    @JsonSerialize(using = BigDecimalSerializer.class)
    private BigDecimal price;
}
```

**测试序列化**

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

public class JsonSerializeTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testBigDecimalSerialization() throws Exception {
        Product product = Product.builder().name("Laptop").price(new BigDecimal("1234.56789")).build();
        String json = objectMapper.writeValueAsString(product);
        System.out.println(json);
        assertTrue(json.contains("\"price\":\"1234.57\"")); // 验证是否保留 2 位小数
    }
}
```

**输出 JSON**

```json
{
    "name": "Laptop",
    "price": "1234.57"
}
```

📌 **`price` 只保留了 2 位小数，而不是 `1234.56789`，成功自定义了序列化！**

---

#### **11. `@JsonDeserialize`（自定义反序列化）**

**作用**

- `@JsonDeserialize(using = CustomDeserializer.class)` **用于自定义 JSON 反序列化到 Java 对象的方式**。
- 适用于 **字段级别** 或 **类级别** 的反序列化控制。

**示例：自定义日期反序列化**

默认情况下，Jackson 不能直接解析 `"2024-03-05 12:30:00"` 格式的字符串为 `LocalDateTime`，需要自定义 `JsonDeserializer`。

```java
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// 自定义反序列化器
public class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return LocalDateTime.parse(p.getText(), FORMATTER);
    }
}
```

**应用 `@JsonDeserialize`**

```java
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class Event {
    private String title;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime startTime;
}
```

**测试反序列化**

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class JsonDeserializeTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testLocalDateTimeDeserialization() throws Exception {
        String json = "{\"title\":\"Meeting\",\"startTime\":\"2024-03-05 12:30:00\"}";
        Event event = objectMapper.readValue(json, Event.class);
        System.out.println(event);

        assertEquals(2024, event.getStartTime().getYear());
        assertEquals(3, event.getStartTime().getMonthValue());
        assertEquals(5, event.getStartTime().getDayOfMonth());
        assertEquals(12, event.getStartTime().getHour());
        assertEquals(30, event.getStartTime().getMinute());
    }
}
```

**JSON 反序列化后**

```json
Event(title=Meeting, startTime=2024-03-05T12:30)
```

📌 **成功将 `"2024-03-05 12:30:00"` 转换成 `LocalDateTime`！**

---

#### 12. `@JsonTypeInfo` 和 `@JsonSubTypes` - 处理多态对象

**作用**：在序列化/反序列化时包含类的类型信息，常用于继承结构。

**示例**：

```java
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Student.class, name = "student"),
    @JsonSubTypes.Type(value = Teacher.class, name = "teacher")
})
public abstract class Person {
    public String name;
}

public class Student extends Person {
    public int grade;
}

public class Teacher extends Person {
    public String subject;
}
```

**序列化 JSON**：

```json
{
    "type": "student",
    "name": "Tom",
    "grade": 10
}
```

### 使用方法

#### 创建实体类

```java
package local.ateng.java.serialize.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import local.ateng.java.serialize.deserializer.LocalDateTimeDeserializer;
import local.ateng.java.serialize.serializer.BigDecimalSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 名称
     */
    @JsonProperty("full_name")
    private String name;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 手机号码
     */
    private String phoneNumber;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 分数
     */
    @JsonSerialize(using = BigDecimalSerializer.class)
    private BigDecimal score;

    /**
     * 比例
     */
    private Double ratio;

    /**
     * 生日
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    /**
     * 所在省份
     */
    private String province;

    /**
     * 所在城市
     */
    private String city;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createTime;

}
```



#### 使用JSON

使用测试类来进行演示

```java
package local.ateng.java.serialize;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import local.ateng.java.serialize.entity.MyUser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class JacksonAnnotationTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public JacksonAnnotationTests () {
        /**
         * 注册 Java 8 日期时间
         * Jackson 默认不支持 java.time.LocalDate 和 java.time.LocalDateTime，需要手动注册 JSR-310（Java 8 日期时间）模块 才能正常序列化/反序列化 LocalDate 和 LocalDateTime。
         */
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    public void serialization() throws JsonProcessingException {
        // 创建对象
        MyUser myUser = MyUser.builder()
                .id(1L)
                .name("ateng")
                .age(25)
                .phoneNumber("1762306666")
                .email("kongyu2385569970@gmail.com")
                .score(new BigDecimal("88.911"))
                .ratio(0.7147)
                .birthday(LocalDate.parse("2000-01-01"))
                .province("重庆市")
                .city("重庆市")
                .createTime(LocalDateTime.now())
                .build();
        // 进行序列化
        String json = objectMapper.writeValueAsString(myUser);
        System.out.println(json);
    }

    @Test
    public void deserialization() throws JsonProcessingException {
        // 创建数据
        String json = "{\"id\":\"1\",\"age\":25,\"phoneNumber\":\"1762306666\",\"email\":\"kongyu2385569970@gmail.com\",\"score\":\"88.91\",\"ratio\":0.7147,\"birthday\":\"2000-01-01\",\"province\":\"重庆市\",\"city\":\"重庆市\",\"createTime\":\"2025-03-05 11:02:56\",\"full_name\":\"ateng\"}";
        // 进行反序列化
        MyUser myUser = objectMapper.readValue(json, MyUser.class);
        System.out.println(myUser);
    }

}
```

序列化serialization结果：

```json
{"id":"1","age":25,"phoneNumber":"1762306666","email":"kongyu2385569970@gmail.com","score":"88.91","ratio":0.7147,"birthday":"2000-01-01","province":"重庆市","city":"重庆市","createTime":"2025-03-05 11:24:51.002","full_name":"ateng"}
```

反序列化deserialization结果

```
MyUser(id=1, name=ateng, age=25, phoneNumber=1762306666, email=kongyu2385569970@gmail.com, score=88.91, ratio=0.7147, birthday=2000-01-01, province=重庆市, city=重庆市, createTime=2025-03-05T11:02:56)
```



#### 使用Controller

在 **Spring Web MVC** 中，Jackson 主要用于处理 HTTP 请求和响应的 JSON 序列化与反序列化。当 Controller 返回 Java 对象时，Spring MVC 通过 `MappingJackson2HttpMessageConverter` 将其转换为 JSON 响应给前端，反之，当前端发送 JSON 数据时，Spring MVC 会自动解析，并使用 Jackson 将其转换为 Java 对象。在实际应用中，`@RestController` 或 `@ResponseBody` 注解可以让 Spring 自动调用 Jackson 进行序列化，而 `@RequestBody` 注解则让 Jackson 负责反序列化。

```java
package local.ateng.java.serialize.controller;

import local.ateng.java.serialize.entity.MyUser;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/jackson")
public class JacksonController {

    // 序列化
    @GetMapping("/serialize")
    public MyUser serialize() {
        return MyUser.builder()
                .id(1L)
                .name("ateng")
                .age(25)
                .phoneNumber("1762306666")
                .email("kongyu2385569970@gmail.com")
                .score(new BigDecimal("88.911"))
                .ratio(0.7147)
                .birthday(LocalDate.parse("2000-01-01"))
                .province("重庆市")
                .city("重庆市")
                .createTime(LocalDateTime.now())
                .build();
    }

    // 反序列化
    @PostMapping("/deserialize")
    public String deserialize(@RequestBody MyUser myUser) {
        System.out.println(myUser);
        return "ok";
    }

}
```

**访问序列化接口**

```
curl -X GET http://localhost:12014/jackson/serialize
```

示例输出：

```json
{"id":"1","age":25,"phoneNumber":"1762306666","email":"kongyu2385569970@gmail.com","score":"88.91","ratio":0.7147,"birthday":"2000-01-01","province":"重庆市","city":"重庆市","createTime":"2025-03-05 11:32:34.043","full_name":"ateng"}
```

**访问反序列化接口**

```
curl -X POST http://192.168.100.2:12014/jackson/deserialize \
     -H "Content-Type: application/json" \
     -d '{
           "id": 1,
           "name": "ateng",
           "age": 25,
           "phoneNumber": "1762306666",
           "email": "kongyu2385569970@gmail.com",
           "score": 88.911,
           "ratio": 0.7147,
           "birthday": "2000-01-01",
           "province": "Chongqing",
           "city": "Chongqing",
           "createTime": "2025-03-05 14:30:00"
         }'
```

控制台打印

```
MyUser(id=1, name=null, age=25, phoneNumber=1762306666, email=kongyu2385569970@gmail.com, score=88.911, ratio=0.7147, birthday=2000-01-01, province=Chongqing, city=Chongqing, createTime=2025-03-05T14:30)
```



## 自定义序列化和反序列化

### **1. `JsonSerializer` - 自定义序列化**

#### **1.1 `JsonSerializer` - 自定义日期格式**

**作用**：格式化 `LocalDateTime` 为 **`yyyy-MM-dd HH:mm:ss`**。

```java
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.format(formatter));
    }
}
```

------

#### **1.2 `JsonSerializer` - 枚举转换为字符串**

**作用**：序列化枚举时返回枚举 `name()`，而不是默认的索引值。

```java
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

public class EnumToStringSerializer extends JsonSerializer<Enum<?>> {
    @Override
    public void serialize(Enum<?> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.name());
    }
}
```

------

#### **1.3 `JsonSerializer` - 敏感信息脱敏（如手机号）**

**作用**：只显示手机号后 4 位，其余部分用 `*` 号代替。

```java
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

public class MaskedPhoneSerializer extends JsonSerializer<String> {
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value != null && value.length() >= 4) {
            gen.writeString("****" + value.substring(value.length() - 4));
        } else {
            gen.writeString(value);
        }
    }
}
```

------

#### **1.4 `JsonSerializer` - Long 转 String**

**作用**：防止前端 JavaScript 解析 `Long` 精度丢失。

```java
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

public class LongToStringSerializer extends JsonSerializer<Long> {
    @Override
    public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.toString());
    }
}
```

------

#### **1.5 `JsonSerializer` - BigDecimal 保留两位小数**

**作用**：格式化 `BigDecimal`，保留两位小数。

```java
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;

public class BigDecimalSerializer extends JsonSerializer<BigDecimal> {
    private static final DecimalFormat df = new DecimalFormat("0.00");

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(df.format(value));
    }
}
```

------

### **2. `JsonDeserializer` - 自定义反序列化**

#### **2.1 `JsonDeserializer` - 解析日期格式**

**作用**：解析 `"yyyy-MM-dd HH:mm:ss"` 格式的字符串为 `LocalDateTime`。

```java
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return LocalDateTime.parse(p.getText(), formatter);
    }
}
```

------

#### **2.2 `JsonDeserializer` - 解析布尔值**

**作用**：支持 `1/0`、`yes/no` 解析为 `Boolean`。

```java
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

public class CustomBooleanDeserializer extends JsonDeserializer<Boolean> {
    @Override
    public Boolean deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText().toLowerCase();
        return "yes".equals(text) || "1".equals(text);
    }
}
```

------

#### **2.3 `JsonDeserializer` - 忽略空字符串并返回 `null`**

**作用**：如果 JSON 中某个字段是 `""`，则转换为 `null`。

```java
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

public class EmptyStringToNullDeserializer extends JsonDeserializer<String> {
    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        return (value == null || value.trim().isEmpty()) ? null : value;
    }
}
```

------

#### **2.4 `JsonDeserializer` - 解析枚举（忽略大小写）**

**作用**：支持大小写不敏感的字符串映射到 `Enum`。

```java
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

public class CaseInsensitiveEnumDeserializer<T extends Enum<T>> extends JsonDeserializer<T> {
    private final Class<T> enumClass;

    public CaseInsensitiveEnumDeserializer(Class<T> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText().toUpperCase();
        return Enum.valueOf(enumClass, value);
    }
}
```

**应用方式**：

```java
public enum Status {
    ACTIVE, INACTIVE
}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class User {
    private String name;

    @JsonDeserialize(using = CaseInsensitiveEnumDeserializer.class)
    private Status status;

    // 省略构造方法、getter 和 setter
}
```

------

#### **2.5 `JsonDeserializer` - 解析数字为 `String`**

**作用**：防止前端 `Long` 精度丢失。

```java
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

public class NumberToStringDeserializer extends JsonDeserializer<String> {
    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return p.getText();
    }
}
```



