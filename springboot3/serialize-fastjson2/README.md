# Fastjson2 序列化和反序列化

[Fastjson2](https://github.com/alibaba/fastjson2/wiki/fastjson2_intro_cn) 是一个高性能的 JSON 处理库，用于序列化和反序列化 Java 对象。在 Spring Boot 中集成 Fastjson2，通过自定义 `HttpMessageConverter` 来定制序列化配置，使其替代默认的 Jackson 进行 JSON 处理。这样，Spring Boot 会使用 Fastjson2 来处理 HTTP 请求和响应的 JSON 数据。

以下是序列化和反序列化的应用场景

| **应用场景**                       | **序列化**                  | **反序列化**              |
| ---------------------------------- | --------------------------- | ------------------------- |
| **Spring Boot API** 返回 JSON 响应 | Java 对象 → JSON            | 前端请求 JSON → Java 对象 |
| **数据库存储 JSON**                | Java 对象 → JSON 存储       | 读取 JSON → Java 对象     |
| **Redis 缓存**                     | Java 对象 → JSON 存入 Redis | 取出 JSON → Java 对象     |
| **消息队列（MQ）**                 | Java 对象 → JSON 发送       | 监听 JSON → Java 对象     |

- [Fastjson2使用文档](/tools/fastjson2/README.md)



## 基础配置

### 添加依赖

```xml
<properties>
    <fastjson2.version>2.0.53</fastjson2.version>
</properties>
<!-- 在 Spring 中集成 Fastjson2 -->
<dependencies>
    <!-- 在 Spring 中集成 Fastjson2 -->
    <dependency>
        <groupId>com.alibaba.fastjson2</groupId>
        <artifactId>fastjson2-extension-spring6</artifactId>
        <version>${fastjson2.version}</version>
    </dependency>
</dependencies>
```



## Spring Web MVC序列化和反序列化

在Spring Boot Web应用中，JSON的序列化和反序列化操作通常通过`HttpMessageConverter`来实现。默认情况下，Spring Boot使用Jackson来处理JSON数据，但如果想使用Fastjson2作为JSON处理工具，可以通过自定义配置来替换Spring Boot的默认`ObjectMapper`。Fastjson2在处理大数据量时具备更高的性能，同时提供了更丰富的序列化特性，如格式化输出、嵌套对象处理和日期格式化等。通过配置`Fastjson2HttpMessageConverter`，Spring Boot可以高效地序列化Java对象为JSON格式，也能将JSON字符串反序列化为Java对象。这种配置对于性能要求较高的应用非常有利，尤其是在处理大量数据交互时。

### 配置

```java
package local.ateng.java.serialize.config;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.support.config.FastJsonConfig;
import com.alibaba.fastjson2.support.spring6.http.converter.FastJsonHttpMessageConverter;
import local.ateng.java.serialize.serializer.DefaultValueFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * 在 Spring Web MVC 中集成 Fastjson2
 * https://github.com/alibaba/fastjson2/blob/main/docs/spring_support_cn.md#2-%E5%9C%A8-spring-web-mvc-%E4%B8%AD%E9%9B%86%E6%88%90-fastjson2
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-03-06
 */
@Configuration
public class FastJsonWebMvcConfig implements WebMvcConfigurer {

    /**
     * Fastjson2转换器配置
     *
     * @return
     */
    private static FastJsonHttpMessageConverter getFastJsonHttpMessageConverter() {
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        FastJsonConfig config = new FastJsonConfig();
        config.setWriterFeatures(
                // 序列化输出空值字段
                JSONWriter.Feature.WriteNulls,
                // 基于字段反序列化
                JSONWriter.Feature.FieldBased,
                // 把 Long 类型转为字符串，避免前端精度丢失
                JSONWriter.Feature.WriteLongAsString,
                // 序列化BigDecimal使用toPlainString，避免科学计数法
                JSONWriter.Feature.WriteBigDecimalAsPlain
        );
        config.setReaderFeatures(
                // 默认下是camel case精确匹配，打开这个后，能够智能识别camel/upper/pascal/snake/Kebab五中case
                JSONReader.Feature.SupportSmartMatch,
                // 允许字段名不带引号
                JSONReader.Feature.AllowUnQuotedFieldNames,
                // 忽略无法序列化的字段
                JSONReader.Feature.IgnoreNoneSerializable,
                // 防止类型不匹配时报错（更安全）
                JSONReader.Feature.IgnoreAutoTypeNotMatch
        );
        config.setWriterFilters(new DefaultValueFilter());
        converter.setFastJsonConfig(config);
        converter.setDefaultCharset(StandardCharsets.UTF_8);
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_JSON));
        return converter;
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        FastJsonHttpMessageConverter converter = getFastJsonHttpMessageConverter();
        converters.add(0, converter);
    }

}


```

### 使用

```java
package local.ateng.java.serialize.controller;

import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.serialize.entity.MyUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/fastjson2")
@RequiredArgsConstructor
public class Fastjson2Controller {

    // 序列化
    @GetMapping("/serialize")
    public MyUser serialize() {
        Map<String, Object> map = Map.of("name", "ateng", "age", 26L);
        return MyUser.builder()
                .id(1L)
                .name("ateng")
                .age(25)
                .phoneNumber("1762306666")
                .email("kongyu2385569970@gmail.com")
                .score(new BigDecimal("1E+20"))
                .ratio(0.7147)
                .birthday(LocalDate.parse("2000-01-01"))
                .province(null)
                .city("重庆市")
                .createTime(LocalDateTime.now())
                .createTime2(new Date())
                .list(List.of("1", "2"))
                .set(Set.of("1", "2", "3"))
                .map(new HashMap<>(map))
                .build();
    }

    // 反序列化
    @PostMapping("/deserialize")
    public String deserialize(@RequestBody MyUser myUser) {
        System.out.println(myUser);
        return "ok";
    }

    // 反序列化
    @PostMapping("/deserialize2")
    public String deserialize2(@RequestBody JSONObject myUser) {
        System.out.println(myUser);
        return "ok";
    }

    // 反序列化
    @PostMapping("/deserialize3")
    public String deserialize3(@RequestBody List<String> list) {
        System.out.println(list);
        return "ok";
    }

    // 反序列化
    @PostMapping("/test")
    public String test(@RequestBody String str) {
        System.out.println(str);
        JSONObject jsonObject = JSONObject.parseObject(str);
        System.out.println(jsonObject);
        System.out.println(jsonObject.getDouble("score"));
        System.out.println(JSONObject.parseObject(str, MyUser.class));
        return "ok";
    }

}

```

**访问序列化接口**

```
curl -X GET http://localhost:12013/fastjson2/serialize
```

示例输出：

```json
{"age":25,"birthday":"2000-01-01 00:00:00","city":"重庆市","createTime":"2026-04-11 20:16:23","createTime2":"2026-04-11 20:16:23","createTime3":null,"email":"kongyu2385569970@gmail.com","id":"1","list":["1","2"],"map":{"name":"ateng","age":"26"},"name":"ateng","num":0,"phoneNumber":"1762306666","province":null,"ratio":0.7147,"score":100000000000000000000.0,"set":["3","2","1"]}
```

**访问反序列化接口**

```
curl -X POST http://192.168.100.2:12013/fastjson2/deserialize \
     -H "Content-Type: application/json" \
     -d '{"age":25,"birthday":"2000-01-01 00:00:00","city":"重庆市","createTime":"2026-04-11 20:16:23","createTime2":"2026-04-11 20:16:23","createTime3":null,"email":"kongyu2385569970@gmail.com","id":"1","list":["1","2"],"map":{"name":"ateng","age":"26"},"name":"ateng","num":0,"phoneNumber":"1762306666","province":null,"ratio":0.7147,"score":100000000000000000000.0,"set":["3","2","1"]}'
```

控制台打印

```
MyUser(id=1, name=ateng, age=25, phoneNumber=1762306666, email=kongyu2385569970@gmail.com, score=100000000000000000000.0, ratio=0.7147, birthday=2000-01-01, province=null, city=重庆市, createTime=2026-04-11T20:16:23, createTime2=Sat Apr 11 20:16:23 CST 2026, createTime3=null, num=0, list=[1, 2], set=[1, 2, 3], map={name=ateng, age=26})
```



## Spring Data Redis序列化和反序列化

在Spring Boot集成Redis时，数据的序列化与反序列化是至关重要的，通常通过`RedisTemplate`来完成。默认情况下，Spring Boot使用JDK的原生序列化方式或Jackson来序列化对象。如果需要使用Fastjson2，可以自定义Redis的序列化机制，采用Fastjson2进行高效的对象与JSON的转换。通过配置`RedisTemplate`的序列化器，开发者可以利用Fastjson2对Redis中存储的对象进行序列化和反序列化。Fastjson2在性能上相较于其他序列化库表现更优，尤其是在大规模数据访问时，可以显著提升应用性能，确保在分布式缓存系统中对数据的高效处理和快速响应。

### 配置（默认）

使用 `GenericFastJsonRedisSerializer` 这个有点小问题，一些类型会带有后缀，有些不便于开发修改数据，建议还是 自定义序列化器 配置。

```java
public GenericFastJsonRedisSerializer() {
    config.setReaderFeatures(JSONReader.Feature.SupportAutoType);
    config.setWriterFeatures(JSONWriter.Feature.WriteClassName);
}
```

![image-20250306140136614](./assets/image-20250306140136614.png)

```java
package local.ateng.java.serialize.config;

import com.alibaba.fastjson2.support.spring6.data.redis.GenericFastJsonRedisSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * RedisTemplate 配置类
 *
 * <p>
 * 该类负责配置 RedisTemplate，允许对象进行序列化和反序列化。
 * 在这里，我们使用了 StringRedisSerializer 来序列化和反序列化 Redis 键，
 * 使用 FastJsonRedisSerializer 来序列化和反序列化 Redis 值，确保 Redis 能够存储 Java 对象。
 * </p>
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-03-06
 */
@Configuration
public class RedisTemplateConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        /**
         * 使用StringRedisSerializer来序列化和反序列化redis的key值
         */
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer);
        /**
         * 使用Fastjson2默认的Serializer来序列化和反序列化redis的value值
         */
        GenericFastJsonRedisSerializer fastJsonRedisSerializer = new GenericFastJsonRedisSerializer();
        redisTemplate.setValueSerializer(fastJsonRedisSerializer);
        redisTemplate.setHashValueSerializer(fastJsonRedisSerializer);
        redisTemplate.setStringSerializer(fastJsonRedisSerializer);

        // 返回redisTemplate
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

}
```

### 配置（自定义）

#### 配置序列化器

注意修改为自己的包名：`config.setReaderFilters(JSONReader.autoTypeFilter("local.ateng.java."));`

```java
package local.ateng.java.serialize.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.filter.Filter;
import com.alibaba.fastjson2.support.config.FastJsonConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * FastJson2 Redis序列化器
 * <p>
 * 功能：
 * 1. 支持JSON与JSONB序列化
 * 2. 支持自动类型（白名单控制）
 * 3. 提供日志输出便于问题排查
 *
 * @param <T> 序列化类型
 * @author Ateng
 * @since 2026-04-11
 */
public class FastJson2RedisSerializer<T> implements RedisSerializer<T> {

    private static final Logger log = LoggerFactory.getLogger(FastJson2RedisSerializer.class);

    /**
     * 空字节数组常量
     */
    private static final byte[] EMPTY_BYTES = new byte[0];

    /**
     * 目标类型
     */
    private final Class<T> type;

    /**
     * FastJson配置
     */
    private final FastJsonConfig config;

    /**
     * 是否使用JSONB
     */
    private final boolean jsonb;

    /**
     * 构造方法（默认JSON模式）
     */
    public FastJson2RedisSerializer(Class<T> type) {
        this(type, buildDefaultConfig(), false);
    }

    /**
     * 构造方法（自定义配置）
     */
    public FastJson2RedisSerializer(Class<T> type, FastJsonConfig config, boolean jsonb) {
        this.type = Objects.requireNonNull(type, "type不能为空");
        this.config = Objects.requireNonNull(config, "config不能为空");
        this.jsonb = jsonb;
    }

    /**
     * 构建默认配置
     */
    private static FastJsonConfig buildDefaultConfig() {
        FastJsonConfig config = new FastJsonConfig();

        // 基础配置
        config.setCharset(StandardCharsets.UTF_8);

        // 序列化特性
        config.setWriterFeatures(
                // 序列化时输出类型信息
                JSONWriter.Feature.WriteClassName,
                // 不输出数字类型的类名
                JSONWriter.Feature.NotWriteNumberClassName,
                // 不输出 Set 类型的类名
                JSONWriter.Feature.NotWriteSetClassName,
                // 不输出 Map 类型的类名
                JSONWriter.Feature.NotWriteHashMapArrayListClassName,
                // 序列化输出空值字段
                JSONWriter.Feature.WriteNulls,
                // 基于字段反序列化
                JSONWriter.Feature.FieldBased
        );

        // 反序列化特性
        config.setReaderFeatures(
                // 默认下是camel case精确匹配，打开这个后，能够智能识别camel/upper/pascal/snake/Kebab五中case
                JSONReader.Feature.SupportSmartMatch,
                // 允许字段名不带引号
                JSONReader.Feature.AllowUnQuotedFieldNames,
                // 忽略无法序列化的字段
                JSONReader.Feature.IgnoreNoneSerializable
        );

        // 自动类型白名单（建议尽量收敛）
        config.setReaderFilters(
                JSONReader.autoTypeFilter(
                        "local.ateng.",
                        "io.github.ateng."
                )
        );

        return config;
    }

    /**
     * 序列化
     */
    @Override
    public byte[] serialize(T value) throws SerializationException {
        if (value == null) {
            return EMPTY_BYTES;
        }

        try {
            byte[] result = jsonb
                    ? JSONB.toBytes(value, config.getSymbolTable(), getWriterFilters(), config.getWriterFeatures())
                    : JSON.toJSONBytes(value, config.getDateFormat(), getWriterFilters(), config.getWriterFeatures());

            if (log.isDebugEnabled()) {
                log.debug("Redis序列化成功，类型：{}，字节大小：{}", type.getName(), result.length);
            }

            return result;
        } catch (Exception e) {
            log.error("Redis序列化失败，类型：{}，对象：{}", type.getName(), value, e);
            throw new SerializationException(buildSerializeError(value), e);
        }
    }

    /**
     * 反序列化
     */
    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try {
            T result = jsonb
                    ? JSONB.parseObject(bytes, type, config.getSymbolTable(), config.getReaderFilters(), config.getReaderFeatures())
                    : JSON.parseObject(bytes, type, config.getDateFormat(), config.getReaderFilters(), config.getReaderFeatures());

            if (log.isDebugEnabled()) {
                log.debug("Redis反序列化成功，类型：{}，字节大小：{}", type.getName(), bytes.length);
            }

            return result;
        } catch (Exception e) {
            log.error("Redis反序列化失败，类型：{}，字节长度：{}", type.getName(), bytes.length, e);
            throw new SerializationException(buildDeserializeError(bytes), e);
        }
    }

    /**
     * 获取WriterFilters，避免空指针
     */
    private Filter[] getWriterFilters() {
        Filter[] filters = config.getWriterFilters();
        return filters == null ? new Filter[0] : filters;
    }

    /**
     * 构建序列化异常信息
     */
    private String buildSerializeError(T value) {
        return "FastJson2序列化失败，type=" + type.getName()
                + ", valueClass=" + (value == null ? "null" : value.getClass().getName());
    }

    /**
     * 构建反序列化异常信息
     */
    private String buildDeserializeError(byte[] bytes) {
        return "FastJson2反序列化失败，type=" + type.getName()
                + ", bytesLength=" + (bytes == null ? 0 : bytes.length);
    }
}
```

#### 配置序列化和反序列化

```java
package local.ateng.java.serialize.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * RedisTemplate 配置类
 *
 * <p>
 * 该类负责配置 RedisTemplate，允许对象进行序列化和反序列化。
 * 在这里，我们使用了 StringRedisSerializer 来序列化和反序列化 Redis 键，
 * 使用 FastJsonRedisSerializer 来序列化和反序列化 Redis 值，确保 Redis 能够存储 Java 对象。
 * </p>
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-03-06
 */
@Configuration
public class RedisTemplateConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        /**
         * 使用StringRedisSerializer来序列化和反序列化redis的key值
         */
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer);

        /**
         * 使用自定义的Fastjson2的Serializer来序列化和反序列化redis的value值
         */
        FastJson2RedisSerializer fastJson2RedisSerializer = new FastJson2RedisSerializer(Object.class);
        redisTemplate.setValueSerializer(fastJson2RedisSerializer);
        redisTemplate.setHashValueSerializer(fastJson2RedisSerializer);

        // 返回redisTemplate
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

}
```



### 使用

```java
package local.ateng.java.serialize.controller;

import local.ateng.java.serialize.entity.MyUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/redis")
@RequiredArgsConstructor
public class RedisController {
    private final RedisTemplate<String, Object> redisTemplate;

    // 序列化
    @GetMapping("/serialize")
    public String serialize() {
        Map<String, Object> map = Map.of("name", "ateng", "age", 26L);
        MyUser myUser = MyUser.builder()
                .id(1L)
                .name("ateng")
                .age(null)
                .phoneNumber("1762306666")
                .email("kongyu2385569970@gmail.com")
                .score(new BigDecimal("1E+20"))
                .ratio(0.7147)
                .birthday(LocalDate.parse("2000-01-01"))
                .province(null)
                .city("重庆市")
                .createTime(LocalDateTime.now())
                .createTime2(new Date())
                .list(List.of("1", "2"))
                .set(Set.of("1", "2", "3"))
                .map(new HashMap<>(map))
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
    "@type": "local.ateng.java.serialize.entity.MyUser",
    "age": null,
    "birthday": "2000-01-01 00:00:00",
    "city": "重庆市",
    "createTime": "2026-04-11 20:17:44",
    "createTime2": "2026-04-11 20:17:44",
    "createTime3": null,
    "email": "kongyu2385569970@gmail.com",
    "id": "1",
    "list": [
        "1",
        "2"
    ],
    "map": {
        "name": "ateng",
        "age": "26"
    },
    "name": "ateng",
    "num": 0,
    "phoneNumber": "1762306666",
    "province": null,
    "ratio": 0.7147,
    "score": 100000000000000000000,
    "set": [
        "3",
        "2",
        "1"
    ]
}
```

反序列化输出

```
MyUser(id=1, name=ateng, age=null, phoneNumber=1762306666, email=kongyu2385569970@gmail.com, score=100000000000000000000, ratio=0.7147, birthday=2000-01-01, province=null, city=重庆市, createTime=2026-04-11T20:17:44, createTime2=Sat Apr 11 20:17:44 CST 2026, createTime3=null, num=0, list=[1, 2], set=[1, 2, 3], map={name=ateng, age=26})
2026-04-11T20:17:44
```



## 默认值

### 默认注解参数

当该字段为null时，反序列化后的值就是字符串的斜杠

注意Fastjson2是反序列化生效这个默认值，Fastjson1是序列化是生效

```java
@JSONField(defaultValue = "/")
private String province;
```

### 自定义注解+Filters

#### 创建注解

```java
package local.ateng.java.serialize.serializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultNullValue {
    String value() default "/";
}

```

#### 创建过滤器

```java
package local.ateng.java.serialize.serializer;


import com.alibaba.fastjson2.filter.BeanContext;
import com.alibaba.fastjson2.filter.ContextValueFilter;

public class DefaultValueFilter implements ContextValueFilter {

    @Override
    public Object process(BeanContext context, Object object, String name, Object value) {
        if (value != null) {
            return value;
        }

        // 如果字段上有注解，就用注解里的值
        DefaultNullValue ann = context.getField().getAnnotation(DefaultNullValue.class);
        if (ann != null) {
            return ann.value();
        }

        // 没有注解就返回原来的 null
        return null;
    }
}

```

#### 配置Converter

给 FastJsonConfig 配置上序列化过滤器

```java
config.setWriterFilters(new DefaultValueFilter());
```

完整代码如下

```java
package local.ateng.java.serialize.config;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.support.config.FastJsonConfig;
import com.alibaba.fastjson2.support.spring6.http.converter.FastJsonHttpMessageConverter;
import local.ateng.java.serialize.serializer.DefaultValueFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * 在 Spring Web MVC 中集成 Fastjson2
 * https://github.com/alibaba/fastjson2/blob/main/docs/spring_support_cn.md#2-%E5%9C%A8-spring-web-mvc-%E4%B8%AD%E9%9B%86%E6%88%90-fastjson2
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-03-06
 */
@Configuration
public class FastJsonWebMvcConfig implements WebMvcConfigurer {

    /**
     * Fastjson2转换器配置
     *
     * @return
     */
    private static FastJsonHttpMessageConverter getFastJsonHttpMessageConverter() {
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        FastJsonConfig config = new FastJsonConfig();
        config.setDateFormat("yyyy-MM-dd HH:mm:ss");
        config.setWriterFeatures(
                // 序列化输出空值字段
                JSONWriter.Feature.WriteNulls,
                // 在大范围超过JavaScript支持的整数，输出为字符串格式
                JSONWriter.Feature.BrowserCompatible,
                // 序列化BigDecimal使用toPlainString，避免科学计数法
                JSONWriter.Feature.WriteBigDecimalAsPlain
        );
        config.setReaderFeatures(
                // 默认下是camel case精确匹配，打开这个后，能够智能识别camel/upper/pascal/snake/Kebab五中case
                JSONReader.Feature.SupportSmartMatch
        );
        config.setWriterFilters(new DefaultValueFilter());
        converter.setFastJsonConfig(config);
        converter.setDefaultCharset(StandardCharsets.UTF_8);
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_JSON));
        return converter;
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        FastJsonHttpMessageConverter converter = getFastJsonHttpMessageConverter();
        converters.add(0, converter);
    }

}


```

#### 配置默认值

在字段上配置默认值即可

```java
@DefaultNullValue("/")
private Date createTime3;
```



## 自定义序列化

### 配置自定义序列化器

```java
package local.ateng.java.serialize.serializer;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;

import java.lang.reflect.Type;

public class CustomSerializer implements ObjectWriter {

    @Override
    public void write(JSONWriter writer, Object object, Object fieldName, Type fieldType, long features) {
        writer.writeString(object + "~");
    }

}

```

### 使用

使用 serializeUsing 指定自定义的序列化器，最终序列化后就可以实现自定义，但是注意字段为null就不会走该序列化器（不会生效）

```java
@JSONField(serializeUsing = CustomSerializer.class)
private String province;
```



## 全局序列化和反序列化配置

配置后再使用 JSON、JSONObject、JSONArray等命令就默认有这些序列化和反序列化配置

```java
package local.ateng.java.serialize.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 * 全局配置fastjson2属性
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-03-06
 */
@Configuration
public class FastJsonGlobalConfig {

    @PostConstruct
    public void run() {
        // 设置全局默认的 JSON 序列化特性
        JSON.config(
                // 序列化输出空值字段
                JSONWriter.Feature.WriteNulls,
                // 基于字段反序列化
                JSONWriter.Feature.FieldBased,
                // 把 Long 类型转为字符串，避免前端精度丢失
                JSONWriter.Feature.WriteLongAsString,
                // 序列化BigDecimal使用toPlainString，避免科学计数法
                JSONWriter.Feature.WriteBigDecimalAsPlain
        );
        JSON.config(
                // 默认下是camel case精确匹配，打开这个后，能够智能识别camel/upper/pascal/snake/Kebab五中case
                JSONReader.Feature.SupportSmartMatch,
                // 允许字段名不带引号
                JSONReader.Feature.AllowUnQuotedFieldNames,
                // 忽略无法序列化的字段
                JSONReader.Feature.IgnoreNoneSerializable,
                // 防止类型不匹配时报错（更安全）
                JSONReader.Feature.IgnoreAutoTypeNotMatch
        );
    }
}

```

