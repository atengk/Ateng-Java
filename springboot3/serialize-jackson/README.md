# Jackson

[Jackson](https://github.com/FasterXML/jackson) 是一个 Java 的 JSON 处理库，广泛用于对象与 JSON 之间的转换（序列化和反序列化）。Spring Boot 默认集成了 Jackson，并将其作为 `spring-boot-starter-web` 依赖的一部分来处理 JSON 数据。

以下是序列化和反序列化的应用场景

| **应用场景**                       | **序列化**                  | **反序列化**              |
| ---------------------------------- | --------------------------- | ------------------------- |
| **Spring Boot API** 返回 JSON 响应 | Java 对象 → JSON            | 前端请求 JSON → Java 对象 |
| **数据库存储 JSON**                | Java 对象 → JSON 存储       | 读取 JSON → Java 对象     |
| **Redis 缓存**                     | Java 对象 → JSON 存入 Redis | 取出 JSON → Java 对象     |
| **消息队列（MQ）**                 | Java 对象 → JSON 发送       | 监听 JSON → Java 对象     |



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



## Spring Web MVC序列化和反序列化

在 Spring Boot 中，定义 `@Bean ObjectMapper` 会 **覆盖默认的 JSON 配置**，影响 **`@RestController` 返回值**、`@RequestBody` 解析、以及 `@Autowired ObjectMapper` 注入。这样可以 **统一全局 JSON 格式**（如时间格式、属性命名）并 **修改 Jackson 默认行为**，确保应用中的 JSON 处理符合需求。如果不定义，Spring Boot 会使用默认 `ObjectMapper`，但无法定制其行为。

### 配置

```java
package local.ateng.java.serialize.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Jackson 使用 ObjectMapper 序列化和反序列化配置
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-03-05
 */
@Configuration
public class JacksonConfig {

    // 日期与时间格式化
    public static String DEFAULT_TIME_ZONE = "Asia/Shanghai";
    public static String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 自定义 Jackson 时间日期的序列化和反序列化规则
     *
     * @param objectMapper Jackson 的 ObjectMapper 实例
     */
    public static void customizeJsonDateTime(ObjectMapper objectMapper, String timeZone,String dateFormat, String dateTimeFormat) {
        // 设置全局时区，确保 Date 类型数据使用此时区
        objectMapper.setTimeZone(TimeZone.getTimeZone(timeZone));

        // 关闭默认时间戳序列化，改为标准格式
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 避免与 JavaTimeModule 冲突
        objectMapper.setDateFormat(new SimpleDateFormat(dateTimeFormat));

        // Java 8 时间模块
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // LocalDateTime 序列化 & 反序列化
        javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(dateTimeFormat)));
        javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(dateTimeFormat)));

        // LocalDate 序列化 & 反序列化
        javaTimeModule.addSerializer(LocalDate.class,
                new LocalDateSerializer(DateTimeFormatter.ofPattern(dateFormat)));
        javaTimeModule.addDeserializer(LocalDate.class,
                new LocalDateDeserializer(DateTimeFormatter.ofPattern(dateFormat)));

        // 注册 JavaTimeModule
        objectMapper.registerModule(javaTimeModule);
    }

    /**
     * 自定义 Jackson 序列化规则
     *
     * @param objectMapper Jackson 的 ObjectMapper 实例
     */
    public static void customizeJsonSerialization(ObjectMapper objectMapper) {
        // 关闭 JSON 美化输出（生产环境建议关闭，提高性能）
        objectMapper.disable(SerializationFeature.INDENT_OUTPUT);

        // 避免 "No serializer found for class" 异常
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // 过滤 null 值，减少 JSON 体积
        //objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 过滤空集合、空字符串等无效数据，进一步精简 JSON
        //objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        // 不过滤 null、空集合、空字符串等无效数据值，保持数据的原始状态
        objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);

        // 枚举类型：使用 `toString()` 方式序列化，而不是默认的 `name()`
        objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);

        // BigDecimal 序列化时不使用科学计数法，确保数据精确
        objectMapper.enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);

        // 排序字段名，保证 JSON 输出的键顺序固定（有助于缓存和数据比对）
        objectMapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

        // 将 Long 和 BigInteger 序列化为字符串，防止 JavaScript 丢失精度
        SimpleModule simpleModule = new SimpleModule();
        ToStringSerializer stringSerializer = ToStringSerializer.instance;
        simpleModule.addSerializer(BigDecimal.class, stringSerializer);
        simpleModule.addSerializer(BigInteger.class, stringSerializer);
        simpleModule.addSerializer(Long.class, stringSerializer);
        simpleModule.addSerializer(Long.TYPE, stringSerializer);
        objectMapper.registerModule(simpleModule);
    }

    /**
     * 自定义 Jackson 反序列化规则
     *
     * @param objectMapper Jackson 的 ObjectMapper 实例
     */
    public static void customizeJsonDeserialization(ObjectMapper objectMapper) {
        // 允许单个值转数组（例如 1 -> [1]）
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        // 忽略未知字段（避免因缺少字段报错，提升兼容性）
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // 处理 原始类型（如 int, long, boolean 等）在反序列化时如果遇到 null 值将其替换为默认值，而不是抛出异常
        objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);

        // 使用 BigDecimal 反序列化浮点数，避免精度丢失
        objectMapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

        // 使用枚举的 `toString()` 方法进行反序列化，而不是默认的 `name()`
        objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);

        // 允许特殊字符转义
        objectMapper.enable(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS);
        objectMapper.enable(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER);

        // 错误时提供类型检查，增强反序列化稳定性
        objectMapper.enable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
    }

    /**
     * 自定义 Jackson JSON 解析设置
     *
     * @param objectMapper Jackson 的 ObjectMapper 实例
     */
    public static void customizeJsonParsing(ObjectMapper objectMapper) {
        // 允许 JSON 中带注释，方便开发阶段使用
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);

        // 允许字段名不带引号（可处理某些特殊格式的 JSON）
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

        // 允许单引号作为 JSON 字符串的定界符（适用于某些特殊格式）
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

        // 允许控制字符的转义（例如，`\n` 或 `\t`）
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);

        // 允许反斜杠转义任何字符（如：`\\`）
        objectMapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);

        // 允许无效的 UTF-8 字符（如果 JSON 编码不完全符合标准）
        objectMapper.configure(JsonParser.Feature.IGNORE_UNDEFINED, true);

        // 允许 JSON 中无序字段（通常是为了性能优化）
        objectMapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
    }

    /**
     * 配置并返回一个定制化的 ObjectMapper 实例。
     * 该方法配置了日期时间格式、JSON 序列化、反序列化和解析等相关设置。
     * 通过调用不同的定制方法，确保 ObjectMapper 在各种场景下都能正确处理数据。
     *
     * @return 配置完成的 ObjectMapper 实例
     */
    @Bean
    public ObjectMapper objectMapper() {
        // 创建 ObjectMapper 实例
        ObjectMapper objectMapper = new ObjectMapper();
        // 配置日期和时间的序列化与反序列化
        customizeJsonDateTime(objectMapper, DEFAULT_TIME_ZONE,DEFAULT_DATE_FORMAT,DEFAULT_DATE_TIME_FORMAT);
        // 配置 JSON 序列化相关设置
        customizeJsonSerialization(objectMapper);
        // 配置 JSON 反序列化相关设置
        customizeJsonDeserialization(objectMapper);
        // 配置 JSON 解析相关设置
        customizeJsonParsing(objectMapper);
        // 返回配置完成的 ObjectMapper 实例
        return objectMapper;
    }

}
```

### 使用

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
{"id":"1","name":"ateng","age":25,"phoneNumber":"1762306666","email":"kongyu2385569970@gmail.com","score":"88.911","ratio":0.7147,"birthday":"2000-01-01","province":"重庆市","city":"重庆市","createTime":"2025-03-06 08:28:19","createTime2":null,"createTime3":null,"num":0,"list":null}
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
MyUser(id=1, name=ateng, age=25, phoneNumber=1762306666, email=kongyu2385569970@gmail.com, score=88.911, ratio=0.7147, birthday=2000-01-01, province=Chongqing, city=Chongqing, createTime=2025-03-05T14:30, createTime2=null, createTime3=null, num=0, list=null)
```



## Spring Data Redis序列化和反序列化

在 **Spring Data Redis** 中，Jackson 主要用于将 Java 对象序列化为 JSON 存入 Redis，并在读取时反序列化回 Java 对象。由于 Redis 只能存储字符串或二进制数据，因此 `RedisTemplate` 需要配置合适的序列化器，如 `Jackson2JsonRedisSerializer`，以确保对象能正确存储和恢复。

### 配置

```java
package local.ateng.java.serialize.config;


import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * RedisTemplate 配置类
 * <p>
 * 该类负责配置 RedisTemplate，允许对象进行序列化和反序列化。
 * 在这里，我们使用了 StringRedisSerializer 来序列化和反序列化 Redis 键，
 * 使用 Jackson2JsonRedisSerializer 来序列化和反序列化 Redis 值，确保 Redis 能够存储 Java 对象。
 * 另外，ObjectMapper 的配置确保 JSON 的格式和解析行为符合预期。
 * </p>
 */
@Configuration
public class RedisTemplateConfig {
    // 日期与时间格式化
    public static String DEFAULT_TIME_ZONE = "Asia/Shanghai";
    public static String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSSSS";

    /**
     * 自定义 ObjectMapper 配置以启用默认类型标记。
     * 该方法的作用是在 JSON 序列化和反序列化时包含类类型信息，
     * 以便在反序列化时能够正确地识别对象的具体类型。
     *
     * @param objectMapper 要配置的 ObjectMapper 实例
     */
    public static void customizeJsonClassType(ObjectMapper objectMapper) {
        // 启用默认类型标记，使 JSON 中包含对象的类信息
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance, // 允许所有子类型的验证器（最宽松）
                ObjectMapper.DefaultTyping.NON_FINAL,  // 仅对非 final 类启用类型信息
                JsonTypeInfo.As.PROPERTY                // 以 JSON 属性的形式存储类型信息
        );
    }

    /**
     * 配置 RedisTemplate
     * <p>
     * 创建 RedisTemplate，并指定如何序列化和反序列化 Redis 中的键值。
     * 该配置支持使用 Jackson2JsonRedisSerializer 序列化值，并使用 StringRedisSerializer 序列化键。
     * </p>
     *
     * @param redisConnectionFactory Redis 连接工厂
     * @return 配置好的 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        // 创建 RedisTemplate 实例
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);  // 设置连接工厂

        // 使用 StringRedisSerializer 来序列化和反序列化 Redis 键
        // Redis 键将被序列化为字符串类型
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);  // 设置键的序列化器
        template.setHashKeySerializer(stringRedisSerializer);  // 设置哈希键的序列化器

        // 创建 ObjectMapper 实例，用于配置 Jackson 的序列化和反序列化行为
        ObjectMapper objectMapper = new ObjectMapper();
        JacksonConfig.customizeJsonDateTime(objectMapper, DEFAULT_TIME_ZONE, DEFAULT_DATE_FORMAT, DEFAULT_DATE_TIME_FORMAT);
        JacksonConfig.customizeJsonSerialization(objectMapper);
        JacksonConfig.customizeJsonDeserialization(objectMapper);
        JacksonConfig.customizeJsonParsing(objectMapper);
        customizeJsonClassType(objectMapper);

        // 创建 Jackson2JsonRedisSerializer，用于序列化和反序列化值
        // 该序列化器使用配置好的 ObjectMapper
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        // 设置 RedisTemplate 的值的序列化器
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);  // 设置哈希值的序列化器

        // 返回配置好的 RedisTemplate
        template.afterPropertiesSet();
        return template;
    }

}
```

### 使用

```java
package local.ateng.java.serialize.controller;

import local.ateng.java.serialize.entity.MyUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/redis")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisController {
    private final RedisTemplate<String, Object> redisTemplate;

    // 序列化
    @GetMapping("/serialize")
    public String serialize() {
        MyUser myUser = MyUser.builder()
                .id(1L)
                .name("ateng")
                .age(null)
                .phoneNumber("1762306666")
                .email("kongyu2385569970@gmail.com")
                .score(new BigDecimal("88.911"))
                .ratio(0.7147)
                .birthday(LocalDate.parse("2000-01-01"))
                .province(null)
                .city("重庆市")
                .createTime(LocalDateTime.now())
                .createTime2(new Date())
                .list(List.of())
                .build();
        redisTemplate.opsForValue().set("myUser", myUser);
        return "ok";
    }

    // 反序列化
    @GetMapping("/deserialize")
    public String deserialize() {
        MyUser myUser = (MyUser) redisTemplate.opsForValue().get("myUser");
        System.out.println(myUser);
        System.out.println(myUser.getCreateTime());
        return "ok";
    }

}
```

序列化到Redis

```json
{
    "@class": "local.ateng.java.serialize.entity.MyUser",
    "id": "1",
    "name": "ateng",
    "age": null,
    "phoneNumber": "1762306666",
    "email": "kongyu2385569970@gmail.com",
    "score": [
        "java.math.BigDecimal",
        "88.911"
    ],
    "ratio": 0.7147,
    "birthday": "2000-01-01",
    "province": null,
    "city": "重庆市",
    "createTime": "2025-03-06 08:46:55.760579",
    "createTime2": [
        "java.util.Date",
        "2025-03-06 08:46:55.000760"
    ],
    "createTime3": null,
    "num": 0,
    "list": [
        "java.util.ImmutableCollections$ListN",
        []
    ]
}
```

反序列化输出

```
MyUser(id=1, name=ateng, age=null, phoneNumber=1762306666, email=kongyu2385569970@gmail.com, score=88.911, ratio=0.7147, birthday=2000-01-01, province=null, city=重庆市, createTime=2025-03-06T08:46:55.760579, createTime2=Thu Mar 06 08:46:55 CST 2025, createTime3=null, num=0, list=[])
```

