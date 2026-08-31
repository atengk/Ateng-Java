# Apache Fory JSON

Apache Fory JSON 是 Apache Fory 提供的标准 JSON 序列化模块，可以把 Java 对象转换成 JSON 字符串或 UTF-8 字节数组，也可以从 JSON 还原成 Java 对象。官方当前文档显示，Fory JSON 支持 Java 8+，并提供 `fory-json` Maven 依赖。([Fory][1])

## 1. Maven 依赖

当前官方 Java Setup 文档已经使用 `1.6.1`，建议直接统一使用当前版本。([Fory][1])

```xml
<dependency>
    <groupId>org.apache.fory</groupId>
    <artifactId>fory-json</artifactId>
    <version>1.6.1</version>
</dependency>
```

Gradle：

```gradle
implementation("org.apache.fory:fory-json:1.6.1")
```

如果项目同时使用 `fory-core`、`fory-json` 等模块，版本建议保持一致。([Fory][1])

## 2. 最简单的使用

最核心的 API 就是：

```java
toJson()
fromJson()
toJsonBytes()
```

一个最基本的示例：

```java
import org.apache.fory.json.ForyJson;

/**
 * Apache Fory JSON 基础使用示例
 *
 * @author Ateng
 * @since 2026-08-31
 */
public class ForyJsonDemo {

    private static final ForyJson JSON = ForyJson.builder().build();

    public static void main(String[] args) {
        User user = new User(1L, "Ateng", 20);

        // Java 对象 -> JSON 字符串
        String json = JSON.toJson(user);
        System.out.println(json);

        // JSON 字符串 -> Java 对象
        User result = JSON.fromJson(json, User.class);
        System.out.println(result);

        // Java 对象 -> UTF-8 字节数组
        byte[] bytes = JSON.toJsonBytes(user);

        // UTF-8 字节数组 -> Java 对象
        User result2 = JSON.fromJson(bytes, User.class);
        System.out.println(result2);
    }

    public static class User {

        private Long id;
        private String name;
        private Integer age;

        public User() {
        }

        public User(Long id, String name, Integer age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }

        // getter/setter
    }
}
```

官方文档同样推荐创建一个 `ForyJson` 后重复复用，而不是每次序列化都重新 `builder()`。构建完成的 `ForyJson` 是线程安全的。([Fory][2])

## 3. 推荐封装成 JSON 工具类

实际 Spring Boot 项目里，可以直接封装成工具类。

```java
import cn.hutool.core.util.StrUtil;
import org.apache.fory.json.ForyJson;

/**
 * Apache Fory JSON 工具类
 *
 * @author Ateng
 * @since 2026-08-31
 */
public final class ForyJsonUtil {

    private static final ForyJson JSON = ForyJson.builder().build();

    private ForyJsonUtil() {
    }

    /**
     * 对象转换为 JSON 字符串
     *
     * @param object 对象
     * @return JSON 字符串
     */
    public static String toJson(Object object) {
        return JSON.toJson(object);
    }

    /**
     * JSON 字符串转换为对象
     *
     * @param json JSON 字符串
     * @param clazz 类型
     * @param <T> 类型
     * @return 对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        return JSON.fromJson(json, clazz);
    }

    /**
     * 对象转换为 UTF-8 JSON 字节数组
     *
     * @param object 对象
     * @return JSON 字节数组
     */
    public static byte[] toJsonBytes(Object object) {
        return JSON.toJsonBytes(object);
    }

    /**
     * UTF-8 JSON 字节数组转换为对象
     *
     * @param bytes JSON 字节数组
     * @param clazz 类型
     * @param <T> 类型
     * @return 对象
     */
    public static <T> T fromJson(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return JSON.fromJson(bytes, clazz);
    }
}
```

日常代码就可以直接：

```java
String json = ForyJsonUtil.toJson(user);

User user = ForyJsonUtil.fromJson(json, User.class);
```

## 4. List 泛型对象

这是实际项目中比较常见的场景。

例如：

```java
List<User> users = Arrays.asList(
        new User(1L, "张三", 20),
        new User(2L, "李四", 21)
);
```

JSON：

```json
[
  {
    "id": 1,
    "name": "张三",
    "age": 20
  },
  {
    "id": 2,
    "name": "李四",
    "age": 21
  }
]
```

Fory JSON 支持通过 `TypeRef` 保留泛型类型信息。官方文档明确提供了 `TypeRef<List<User>>` 的用法。([Fory][3])

例如：

```java
import org.apache.fory.json.ForyJson;
import org.apache.fory.reflect.TypeRef;

import java.util.List;

/**
 * Fory JSON 泛型集合示例
 *
 * @author Ateng
 * @since 2026-08-31
 */
public class ForyJsonListDemo {

    private static final ForyJson JSON = ForyJson.builder().build();

    public static void main(String[] args) {
        String json = "[{\"id\":1,\"name\":\"张三\"},{\"id\":2,\"name\":\"李四\"}]";

        List<User> users = JSON.fromJson(
                json,
                new TypeRef<List<User>>() {
                }
        );

        System.out.println(users);
    }

    public static class User {

        private Long id;
        private String name;

        public User() {
        }

        // getter/setter
    }
}
```

## 5. Map 泛型对象

例如：

```java
Map<String, User>
```

可以同样通过 `TypeRef`：

```java
Map<String, User> userMap = JSON.fromJson(
        json,
        new TypeRef<Map<String, User>>() {
        }
);
```

整体思路和 Jackson 的 `TypeReference` 很类似。

## 6. Java 对象 ↔ JSON

最常用的 API 可以简单记成下面这几个：

| 方法                          | 作用               |
| --------------------------- | ---------------- |
| `toJson(Object)`            | 对象 → JSON 字符串    |
| `toJsonBytes(Object)`       | 对象 → UTF-8 字节数组  |
| `fromJson(String, Class)`   | JSON 字符串 → 对象    |
| `fromJson(byte[], Class)`   | JSON 字节数组 → 对象   |
| `fromJson(String, TypeRef)` | JSON → 泛型对象      |
| `fromJson(byte[], TypeRef)` | JSON 字节数组 → 泛型对象 |

对于 Web 接口、Redis、Kafka、MQ、缓存等场景，`toJsonBytes()` 会比较方便，因为可以直接处理 UTF-8 字节数据。官方也明确提供 String 和 byte[] 两套 API。([Fory][3])

## 7. Spring Boot 中使用

项目中通常不需要每次创建 `ForyJson`。

直接注册成 Bean：

```java
import org.apache.fory.json.ForyJson;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Apache Fory JSON 配置
 *
 * @author Ateng
 * @since 2026-08-31
 */
@Configuration
public class ForyJsonConfig {

    @Bean
    public ForyJson foryJson() {
        return ForyJson.builder().build();
    }
}
```

业务代码注入：

```java
import lombok.RequiredArgsConstructor;
import org.apache.fory.json.ForyJson;
import org.springframework.stereotype.Service;

/**
 * 用户 JSON 服务
 *
 * @author Ateng
 * @since 2026-08-31
 */
@Service
@RequiredArgsConstructor
public class UserJsonService {

    private final ForyJson foryJson;

    public String serialize(User user) {
        return foryJson.toJson(user);
    }

    public User deserialize(String json) {
        return foryJson.fromJson(json, User.class);
    }
}
```

Fory 官方建议复用单个 `ForyJson` 实例，因为它是线程安全的。([Fory][2])

## 8. 和 Jackson 的简单对比

如果你本身大量使用 Jackson，可以简单理解成：

```java
// Jackson
objectMapper.writeValueAsString(object);
objectMapper.readValue(json, User.class);

// Fory JSON
foryJson.toJson(object);
foryJson.fromJson(json, User.class);
```

泛型：

```java
// Jackson
objectMapper.readValue(
        json,
        new TypeReference<List<User>>() {}
);

// Fory JSON
foryJson.fromJson(
        json,
        new TypeRef<List<User>>() {}
);
```

所以迁移成本并不高。

## 9. 实际项目推荐写法

如果你现在主要是 Spring Boot + Java 后端，我建议先这样用即可：

```java
private static final ForyJson JSON = ForyJson.builder().build();

// 对象 -> JSON
String json = JSON.toJson(object);

// JSON -> 对象
User user = JSON.fromJson(json, User.class);

// 对象 -> byte[]
byte[] bytes = JSON.toJsonBytes(object);

// byte[] -> 对象
User user = JSON.fromJson(bytes, User.class);

// 泛型
List<User> list = JSON.fromJson(
        json,
        new TypeRef<List<User>>() {
        }
);
```

也就是说，日常开发真正需要记住的其实就是 `ForyJson.builder()`、`toJson`、`fromJson`、`toJsonBytes` 和 `TypeRef`。
