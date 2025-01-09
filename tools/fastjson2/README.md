# FastJson2

https://github.com/alibaba/fastjson2/wiki/fastjson2_intro_cn

FASTJSON v2是FASTJSON项目的重要升级，目标是为下一个十年提供一个高性能的JSON库。通过同一套API，

支持JSON/JSONB两种协议，JSONPath是一等公民。
支持全量解析和部分解析。
支持Java服务端、客户端Android、大数据场景。
支持Kotlin
支持 JSON Schema https://github.com/alibaba/fastjson2/wiki/json_schema_cn
支持Android (2.0.10.android)
支持Graal Native-Image (2.0.10.graal)



## SpringBoot集成Fastjson2

### 添加依赖

**编辑pom.xml添加依赖**

```xml
        <!-- 高性能的JSON库 -->
        <!-- https://github.com/alibaba/fastjson2/wiki/fastjson2_intro_cn#0-fastjson-20%E4%BB%8B%E7%BB%8D -->
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2</artifactId>
            <version>${fastjson2.version}</version>
        </dependency>
        <!-- 在 Spring 中集成 Fastjson2 -->
        <!-- https://github.com/alibaba/fastjson2/blob/main/docs/spring_support_cn.md -->
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2-extension-spring5</artifactId>
            <version>${fastjson2.version}</version>
        </dependency>
```

### 配置转换器

**配置WebMvcConfigurer**

```java
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.support.config.FastJsonConfig;
import com.alibaba.fastjson2.support.spring.http.converter.FastJsonHttpMessageConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * 在 Spring Web MVC 中集成 Fastjson2
 * https://github.com/alibaba/fastjson2/blob/main/docs/spring_support_cn.md#2-%E5%9C%A8-spring-web-mvc-%E4%B8%AD%E9%9B%86%E6%88%90-fastjson2
 *
 * @author 孔余
 * @since 2024-02-05 15:06
 */
@Configuration
public class MyWebMvcConfigurer implements WebMvcConfigurer {
    /**
     * Fastjson2转换器配置
     *
     * @return
     */
    private static FastJsonHttpMessageConverter getFastJsonHttpMessageConverter() {
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        FastJsonConfig config = new FastJsonConfig();
        config.setCharset(Charset.forName("UTF-8"));
        config.setDateFormat("yyyy-MM-dd HH:mm:ss");
        config.setWriterFeatures(
                //JSONWriter.Feature.WriteNulls, // 将String类型字段的空值序列化输出为空字符串""
                //JSONWriter.Feature.FieldBased, // 基于字段序列化，如果不配置，会默认基于public的field和getter方法序列化。配置后，会基于非static的field（包括private）做序列化。
                //JSONWriter.Feature.NullAsDefaultValue, // 将空置输出为缺省值，Number类型的null都输出为0，String类型的null输出为""，数组和Collection类型的输出为[]
                JSONWriter.Feature.BrowserCompatible, // 在大范围超过JavaScript支持的整数，输出为字符串格式
                JSONWriter.Feature.WriteMapNullValue,
                JSONWriter.Feature.BrowserSecure // 浏览器安全，将会'<' '>' '(' ')'字符做转义输出
        );
        config.setReaderFeatures(
                //JSONReader.Feature.FieldBased, // 基于字段反序列化，如果不配置，会默认基于public的field和getter方法序列化。配置后，会基于非static的field（包括private）做反序列化。在fieldbase配置下会更安全
                //JSONReader.Feature.InitStringFieldAsEmpty, // 初始化String字段为空字符串""
                JSONReader.Feature.SupportArrayToBean, // 支持数据映射的方式
                JSONReader.Feature.UseBigDecimalForDoubles // 默认配置会使用BigDecimal来parse小数，打开后会使用Double
        );
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

**配置全局JSON**

```java
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * 全局配置fastjson2
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @date 2024-06-21 11:38:11
 */
@Configuration
public class FastJsonConfig {

    @EventListener
    public void run(ApplicationReadyEvent event) {
        JSON.config(
                //JSONWriter.Feature.WriteNulls, // 将String类型字段的空值序列化输出为空字符串""
                //JSONWriter.Feature.FieldBased, // 基于字段序列化，如果不配置，会默认基于public的field和getter方法序列化。配置后，会基于非static的field（包括private）做序列化。
                //JSONWriter.Feature.NullAsDefaultValue, // 将空置输出为缺省值，Number类型的null都输出为0，String类型的null输出为""，数组和Collection类型的输出为[]
                JSONWriter.Feature.BrowserCompatible, // 在大范围超过JavaScript支持的整数，输出为字符串格式
                JSONWriter.Feature.WriteMapNullValue,
                JSONWriter.Feature.BrowserSecure // 浏览器安全，将会'<' '>' '(' ')'字符做转义输出
        );

        JSON.config(
                //JSONReader.Feature.FieldBased, // 基于字段反序列化，如果不配置，会默认基于public的field和getter方法序列化。配置后，会基于非static的field（包括private）做反序列化。在fieldbase配置下会更安全
                //JSONReader.Feature.InitStringFieldAsEmpty, // 初始化String字段为空字符串""
                JSONReader.Feature.SupportArrayToBean, // 支持数据映射的方式
                JSONReader.Feature.UseBigDecimalForDoubles // 默认配置会使用BigDecimal来parse小数
        );
    }
}
```

### 创建实体类

**UserInfoEntity**

```java
package local.ateng.java.fastjson2.entity;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 用户信息实体类
 * 用于表示系统中的用户信息。
 *
 * @author 孔余
 * @since 2024-01-10 15:51
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoEntity {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户姓名
     */
    private String name;

    /**
     * 用户年龄
     * 注意：这里使用Integer类型，表示年龄是一个整数值。
     */
    private Integer age;

    /**
     * 分数
     */
    private Double score;

    /**
     * 用户生日
     * 注意：这里使用Date类型，表示用户的生日。
     */
    private Date birthday;

    /**
     * 用户所在省份
     */
    private String province;

    /**
     * 用户所在城市
     */
    private String city;

    /**
     * 创建时间
     * 自定义时间格式
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime createAt;
}
```

**ClassNameEntity**

```java
@Data
public class ClassNameEntity {
    private String name;
    private List<UserInfoEntity> users;
}
```

### 配置接口

**ClassNameController**

```java
@RestController
@RequestMapping("/class")
public class ClassNameController {

    @PostMapping("/add")
    public ClassNameEntity add(@RequestBody ClassNameEntity className) {
        return className;
    }

    @PostMapping("/user")
    public UserInfoEntity add(@RequestBody UserInfoEntity userInfoEntity) {
        return userInfoEntity;
    }

}
```



## 使用Fastjson2

### 测试数据准备

**添加依赖**

```xml
        <!-- JavaFaker: 用于生成虚假数据的Java库 -->
        <dependency>
            <groupId>com.github.javafaker</groupId>
            <artifactId>javafaker</artifactId>
            <version>1.0.2</version>
        </dependency>
```

**创建数据类**

```java
package local.ateng.java.fastjson2.init;

import com.github.javafaker.Faker;
import local.ateng.java.fastjson2.entity.UserInfoEntity;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 初始化数据
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-09
 */
@Getter
public class InitData {
    List<UserInfoEntity> list;
    List<UserInfoEntity> list2;

    public InitData() {
        //生成测试数据
        // 创建一个Java Faker实例，指定Locale为中文
        Faker faker = new Faker(new Locale("zh-CN"));
        // 创建一个包含不少于100条JSON数据的列表
        List<UserInfoEntity> userList = new ArrayList();
        for (int i = 1; i <= 3000; i++) {
            UserInfoEntity user = new UserInfoEntity();
            user.setId((long) i);
            user.setName(faker.name().fullName());
            user.setBirthday(faker.date().birthday());
            user.setAge(faker.number().numberBetween(0, 100));
            user.setProvince(faker.address().state());
            user.setCity(faker.address().cityName());
            user.setScore(faker.number().randomDouble(3, 1, 100));
            userList.add(user);
        }
        list = userList;
        for (int i = 1; i <= 3000; i++) {
            UserInfoEntity user = new UserInfoEntity();
            user.setId((long) i);
            user.setName(faker.name().fullName());
            user.setBirthday(faker.date().birthday());
            user.setAge(faker.number().numberBetween(0, 100));
            user.setProvince(faker.address().state());
            user.setCity(faker.address().cityName());
            user.setScore(faker.number().randomDouble(3, 1, 100));
            userList.add(user);
        }
        list2 = userList;
    }

}
```

**创建测试类**

在 `test` 包下创建测试类

```java
package local.ateng.java.fastjson2;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import local.ateng.java.fastjson2.entity.UserInfoEntity;
import local.ateng.java.fastjson2.init.InitData;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public class JSONObjectTests {
    List<UserInfoEntity> list;
    List<UserInfoEntity> list2;

    public JSONObjectTests() {
        InitData initData = new InitData();
        list = initData.getList();
        list2 = initData.getList2();
    }
    
}
```



### 使用JSONObject

#### 字符串转换为JSONObject

```java
    // 字符串转换为JSONObject
    @Test
    void test01() {
        String str = "{\"id\":12345,\"name\":\"John Doe\",\"age\":25,\"score\":85.5,\"birthday\":\"1997-03-15\",\"province\":\"Example Province\",\"city\":\"Example City\"}";
        JSONObject jsonObject = JSONObject.parse(str);
        System.out.println(jsonObject);
    }
```

#### 字符串转换为java对象

```java
    // 字符串转换为java对象
    @Test
    void test02() {
        String str = "{\"id\":null,\"name\":\"John Doe\",\"age\":25,\"score\":0.0,\"birthday\":\"2024-01-18 15:05:10.102\",\"province\":\"\",\"city\":\"Example City\"}";
        UserInfoEntity user = JSONObject.parseObject(str, UserInfoEntity.class);
        System.out.println(user);
    }
```

#### JSONObject转换为java对象

```java
    // JSONObject转换为java对象
    @Test
    void test02_2() {
        JSONObject jsonObject = new JSONObject() {{
            put("name", "John Doe");
            put("age", 25);
            put("birthday", "2024-01-18 15:05:10.102");
            put("city", "Example City");
            put("province", "Example province");
            put("score", 85.5);
        }};
        UserInfoEntity user = jsonObject.toJavaObject(UserInfoEntity.class);
        System.out.println(user);
    }
```

#### 从URL链接获取结果并转JSONObject

```java
    // 从URL链接获取结果并转JSONObject
    @Test
    void test03() throws MalformedURLException {
        JSONObject jsonObject = JSON.parseObject(new URL("https://api.apiopen.top/api/sentences"));
        System.out.println(jsonObject);
    }
```

#### java对象转String

```java
    // java对象转String
    @Test
    void test04() {
        UserInfoEntity user = UserInfoEntity.builder()
                .name("John Doe")
                .age(25)
                .score(85.5)
                .birthday(new Date())
                .province("")
                .city("Example City")
                .build();
        // JSONWriter.Feature介绍
        // https://github.com/alibaba/fastjson2/blob/main/docs/features_cn.md
        // JSONWriter.Feature.WriteNulls -> 序列化输出空值字段
        String str1 = JSONObject.toJSONString(user, JSONWriter.Feature.WriteNulls);
        System.out.println(str1);

        // JSONWriter.Feature.PrettyFormat -> 格式化输出
        String str2 = JSONObject.toJSONString(user, JSONWriter.Feature.PrettyFormat);
        System.out.println(str2);
    }
```

#### java对象转JSONObject

```java
    // java对象转JSONObject
    @Test
    void test05() {
        UserInfoEntity user = UserInfoEntity.builder()
                .name("John Doe")
                .age(25)
                .score(85.5)
                .birthday(new Date())
                .province("")
                .city("Example City")
                .build();
        String str = JSONObject.toJSONString(user);
        JSONObject jsonObject = JSONObject.parse(str);
        System.out.println(jsonObject);
    }
```

#### JSONObject.of创建JSONObject对象

```java
    // JSONObject.of创建JSONObject对象
    @Test
    void test06() {
        JSONObject object = JSONObject.of(
                "id", 1001,
                "name", "DataWorks",
                "date", "2017-07-14",
                "createAt1", LocalDateTime.now(),
                "createAt2", new Date()
        );
        System.out.println(object);
    }
```



### 使用JSONArray

#### 将字符串转换为JSONArray

```java
    // 将字符串转换为JSONArray
    @Test
    void test01() {
        String str = "[{\"age\":87,\" \":\"1979-04-15 03:44:31.797\",\"city\":\"东莞\",\"id\":994,\"name\":\"贺熠彤\",\"province\":\"四川省\",\"score\":65.833},{\"age\":90,\"birthday\":\"1999-12-07 12:45:15.226\",\"city\":\"东莞\",\"id\":1023,\"name\":\"沈伟宸\",\"province\":\"山西省\",\"score\":57.183},{\"age\":57,\"birthday\":\"1987-05-08 04:42:58.07\",\"city\":\"东莞\",\"id\":1213,\"name\":\"戴弘文\",\"province\":\"四川省\",\"score\":53.744},{\"age\":87,\"birthday\":\"1982-12-20 23:35:45.202\",\"city\":\"东莞\",\"id\":1221,\"name\":\"钱振家\",\"province\":\"宁夏\",\"score\":67.445},{\"age\":68,\"birthday\":\"2000-03-17 11:49:20.09\",\"city\":\"东莞\",\"id\":1828,\"name\":\"顾智辉\",\"province\":\"湖南省\",\"score\":44.626},{\"age\":66,\"birthday\":\"1997-10-19 06:26:17.117\",\"city\":\"东莞\",\"id\":2035,\"name\":\"董擎宇\",\"province\":\"河南省\",\"score\":27.493},{\"age\":90,\"birthday\":\"1967-02-21 04:33:24.239\",\"city\":\"东莞\",\"id\":170,\"name\":\"谭思聪\",\"province\":\"澳门\",\"score\":27.498},{\"age\":92,\"birthday\":\"1988-09-02 06:51:55.229\",\"city\":\"东莞\",\"id\":822,\"name\":\"蒋聪健\",\"province\":\"宁夏\",\"score\":38.001},{\"age\":65,\"birthday\":\"2005-10-20 09:59:55.274\",\"city\":\"东莞\",\"id\":1816,\"name\":\"丁天磊\",\"province\":\"宁夏\",\"score\":47.401},{\"age\":58,\"birthday\":\"1990-08-08 13:11:58.805\",\"city\":\"东莞\",\"id\":1845,\"name\":\"钟志泽\",\"province\":\"黑龙江省\",\"score\":16.65},{\"age\":75,\"birthday\":\"1996-09-04 15:02:12.485\",\"city\":\"东莞\",\"id\":2283,\"name\":\"韦鹏煊\",\"province\":\"上海市\",\"score\":75.307},{\"age\":66,\"birthday\":\"1978-01-25 22:22:42.441\",\"city\":\"东莞\",\"id\":2563,\"name\":\"曹明杰\",\"province\":\"广东省\",\"score\":89.582}]";
        JSONArray jsonArray = JSONArray.parse(str);
        System.out.println(jsonArray);
    }
```

#### 将字符串转换为java对象列表

```java
    // 将字符串转换为java对象列表
    @Test
    void test02() {
        String str = "[{\"age\":87,\"birthday\":\"1979-04-15 03:44:31.797\",\"city\":\"东莞\",\"id\":994,\"name\":\"贺熠彤\",\"province\":\"四川省\",\"score\":65.833},{\"age\":90,\"birthday\":\"1999-12-07 12:45:15.226\",\"city\":\"东莞\",\"id\":1023,\"name\":\"沈伟宸\",\"province\":\"山西省\",\"score\":57.183},{\"age\":57,\"birthday\":\"1987-05-08 04:42:58.07\",\"city\":\"东莞\",\"id\":1213,\"name\":\"戴弘文\",\"province\":\"四川省\",\"score\":53.744},{\"age\":87,\"birthday\":\"1982-12-20 23:35:45.202\",\"city\":\"东莞\",\"id\":1221,\"name\":\"钱振家\",\"province\":\"宁夏\",\"score\":67.445},{\"age\":68,\"birthday\":\"2000-03-17 11:49:20.09\",\"city\":\"东莞\",\"id\":1828,\"name\":\"顾智辉\",\"province\":\"湖南省\",\"score\":44.626},{\"age\":66,\"birthday\":\"1997-10-19 06:26:17.117\",\"city\":\"东莞\",\"id\":2035,\"name\":\"董擎宇\",\"province\":\"河南省\",\"score\":27.493},{\"age\":90,\"birthday\":\"1967-02-21 04:33:24.239\",\"city\":\"东莞\",\"id\":170,\"name\":\"谭思聪\",\"province\":\"澳门\",\"score\":27.498},{\"age\":92,\"birthday\":\"1988-09-02 06:51:55.229\",\"city\":\"东莞\",\"id\":822,\"name\":\"蒋聪健\",\"province\":\"宁夏\",\"score\":38.001},{\"age\":65,\"birthday\":\"2005-10-20 09:59:55.274\",\"city\":\"东莞\",\"id\":1816,\"name\":\"丁天磊\",\"province\":\"宁夏\",\"score\":47.401},{\"age\":58,\"birthday\":\"1990-08-08 13:11:58.805\",\"city\":\"东莞\",\"id\":1845,\"name\":\"钟志泽\",\"province\":\"黑龙江省\",\"score\":16.65},{\"age\":75,\"birthday\":\"1996-09-04 15:02:12.485\",\"city\":\"东莞\",\"id\":2283,\"name\":\"韦鹏煊\",\"province\":\"上海市\",\"score\":75.307},{\"age\":66,\"birthday\":\"1978-01-25 22:22:42.441\",\"city\":\"东莞\",\"id\":2563,\"name\":\"曹明杰\",\"province\":\"广东省\",\"score\":89.582}]";
        List<UserInfoEntity> userList = JSONArray.parseArray(str, UserInfoEntity.class);
        System.out.println(userList);
    }
```

#### 将java对象列表转换为字符串

```java
    // 将java对象列表转换为字符串
    @Test
    void test03() {
        String str = JSONArray.toJSONString(list);
        System.out.println(str);
    }
```

#### 循环 增强 for 循环

```java
    // 循环 增强 for 循环
    @Test
    void test04() {
        String str = "[{\"age\":87,\"birthday\":\"1979-04-15 03:44:31.797\",\"city\":\"东莞\",\"id\":994,\"name\":\"贺熠彤\",\"province\":\"四川省\",\"score\":65.833},{\"age\":90,\"birthday\":\"1999-12-07 12:45:15.226\",\"city\":\"东莞\",\"id\":1023,\"name\":\"沈伟宸\",\"province\":\"山西省\",\"score\":57.183},{\"age\":57,\"birthday\":\"1987-05-08 04:42:58.07\",\"city\":\"东莞\",\"id\":1213,\"name\":\"戴弘文\",\"province\":\"四川省\",\"score\":53.744},{\"age\":87,\"birthday\":\"1982-12-20 23:35:45.202\",\"city\":\"东莞\",\"id\":1221,\"name\":\"钱振家\",\"province\":\"宁夏\",\"score\":67.445},{\"age\":68,\"birthday\":\"2000-03-17 11:49:20.09\",\"city\":\"东莞\",\"id\":1828,\"name\":\"顾智辉\",\"province\":\"湖南省\",\"score\":44.626},{\"age\":66,\"birthday\":\"1997-10-19 06:26:17.117\",\"city\":\"东莞\",\"id\":2035,\"name\":\"董擎宇\",\"province\":\"河南省\",\"score\":27.493},{\"age\":90,\"birthday\":\"1967-02-21 04:33:24.239\",\"city\":\"东莞\",\"id\":170,\"name\":\"谭思聪\",\"province\":\"澳门\",\"score\":27.498},{\"age\":92,\"birthday\":\"1988-09-02 06:51:55.229\",\"city\":\"东莞\",\"id\":822,\"name\":\"蒋聪健\",\"province\":\"宁夏\",\"score\":38.001},{\"age\":65,\"birthday\":\"2005-10-20 09:59:55.274\",\"city\":\"东莞\",\"id\":1816,\"name\":\"丁天磊\",\"province\":\"宁夏\",\"score\":47.401},{\"age\":58,\"birthday\":\"1990-08-08 13:11:58.805\",\"city\":\"东莞\",\"id\":1845,\"name\":\"钟志泽\",\"province\":\"黑龙江省\",\"score\":16.65},{\"age\":75,\"birthday\":\"1996-09-04 15:02:12.485\",\"city\":\"东莞\",\"id\":2283,\"name\":\"韦鹏煊\",\"province\":\"上海市\",\"score\":75.307},{\"age\":66,\"birthday\":\"1978-01-25 22:22:42.441\",\"city\":\"东莞\",\"id\":2563,\"name\":\"曹明杰\",\"province\":\"广东省\",\"score\":89.582}]";
        JSONArray jsonArray = JSONArray.parse(str);
        for (Object obj : jsonArray) {
            JSONObject jsonObject = (JSONObject) obj;
            System.out.println("Name: " + jsonObject.getString("name") + ", Age: " + jsonObject.getIntValue("age"));
        }
    }
```

#### 循环 Java 8 的 Stream API

```java
    // 循环 Java 8 的 Stream API
    @Test
    void test05() {
        String str = "[{\"age\":87,\"birthday\":\"1979-04-15 03:44:31.797\",\"city\":\"东莞\",\"id\":994,\"name\":\"贺熠彤\",\"province\":\"四川省\",\"score\":65.833},{\"age\":90,\"birthday\":\"1999-12-07 12:45:15.226\",\"city\":\"东莞\",\"id\":1023,\"name\":\"沈伟宸\",\"province\":\"山西省\",\"score\":57.183},{\"age\":57,\"birthday\":\"1987-05-08 04:42:58.07\",\"city\":\"东莞\",\"id\":1213,\"name\":\"戴弘文\",\"province\":\"四川省\",\"score\":53.744},{\"age\":87,\"birthday\":\"1982-12-20 23:35:45.202\",\"city\":\"东莞\",\"id\":1221,\"name\":\"钱振家\",\"province\":\"宁夏\",\"score\":67.445},{\"age\":68,\"birthday\":\"2000-03-17 11:49:20.09\",\"city\":\"东莞\",\"id\":1828,\"name\":\"顾智辉\",\"province\":\"湖南省\",\"score\":44.626},{\"age\":66,\"birthday\":\"1997-10-19 06:26:17.117\",\"city\":\"东莞\",\"id\":2035,\"name\":\"董擎宇\",\"province\":\"河南省\",\"score\":27.493},{\"age\":90,\"birthday\":\"1967-02-21 04:33:24.239\",\"city\":\"东莞\",\"id\":170,\"name\":\"谭思聪\",\"province\":\"澳门\",\"score\":27.498},{\"age\":92,\"birthday\":\"1988-09-02 06:51:55.229\",\"city\":\"东莞\",\"id\":822,\"name\":\"蒋聪健\",\"province\":\"宁夏\",\"score\":38.001},{\"age\":65,\"birthday\":\"2005-10-20 09:59:55.274\",\"city\":\"东莞\",\"id\":1816,\"name\":\"丁天磊\",\"province\":\"宁夏\",\"score\":47.401},{\"age\":58,\"birthday\":\"1990-08-08 13:11:58.805\",\"city\":\"东莞\",\"id\":1845,\"name\":\"钟志泽\",\"province\":\"黑龙江省\",\"score\":16.65},{\"age\":75,\"birthday\":\"1996-09-04 15:02:12.485\",\"city\":\"东莞\",\"id\":2283,\"name\":\"韦鹏煊\",\"province\":\"上海市\",\"score\":75.307},{\"age\":66,\"birthday\":\"1978-01-25 22:22:42.441\",\"city\":\"东莞\",\"id\":2563,\"name\":\"曹明杰\",\"province\":\"广东省\",\"score\":89.582}]";
        JSONArray jsonArray = JSONArray.parse(str);
        IntStream.range(0, jsonArray.size())
                .mapToObj(jsonArray::getJSONObject)
                .forEach(jsonObject ->
                        System.out.println("Name: " + jsonObject.getString("name") + ", Age: " + jsonObject.getIntValue("age"))
                );
    }
```



### 使用JSONPath

#### 读取集合多个元素的某个属性

```java
    // 读取集合多个元素的某个属性
    @Test
    void test01() {
        List<String> stringList = (List<String>) JSONPath.eval(JSON.toJSONString(list), "$[*].name");
        System.out.println(stringList);
    }
```

#### 读取集合多个元素的某个属性

```java
    // 读取集合多个元素的某个属性
    @Test
    void test01_2() {
        String json = "{\"students\":[{\"name\":\"John\"},{\"name\":\"Alice\"}]}";
        List<String> names = (List<String>) JSONPath.eval(json, "$.students[*].name");
        System.out.println(names);
    }
    @Test
    void test01_2_1() {
        String json = "{\"张三\":[{\"name\":\"John\"},{\"name\":\"Alice\"}]}";
        List<String> names = (List<String>) JSONPath.eval(json, "$.张三[*].name");
        System.out.println(names);
    }
    @Test
    void test01_3() {
        // 递归查询
        String json = "{\"students\":[{\"name\":\"John\"},{\"name\":\"Alice\"}]}";
        List<String> names = (List<String>) JSONPath.eval(json, "$.students..name");
        System.out.println(names);
    }
    @Test
    void test01_04() {
        String jsonString = "{\"students\":[{\"name\":\"John\",\"age\":25,\"gender\":\"Male\"},{\"name\":\"Alice\",\"age\":30,\"gender\":\"Female\"},{\"name\":\"Bob\",\"age\":28,\"gender\":\"Male\"}]}";
        // 使用 JSONPath.eval 获取多条件筛选的结果
        List<Object> result = (List<Object>) JSONPath.eval(jsonString, "$.students[?(@.age >= 25 && @.age <= 30 && @.gender == 'Male')].name");
        System.out.println(result);
    }
    @Test
    void test01_05() {
        String jsonString = "{\"students\":[{\"name\":\"John\",\"age\":25,\"gender\":\"Male\"},{\"name\":\"Alice\",\"age\":30,\"gender\":\"Female\"},{\"name\":\"Bob\",\"age\":28,\"gender\":\"Male\"}]}";
        // 使用 JSONPath.eval 获取1条件筛选的结果
        Object result = JSONPath.eval(jsonString, "$.students[?(@.age >= 25 && @.age <= 30 && @.gender == 'Male')][0]");
        System.out.println(result);
    }
    @Test
    void test01_06() {
        String jsonString = "[{\"name\":\"高新区管委会\",\"congestionIndex\":1.2,\"realSpeed\":\"60.918\",\"yoy\":0.0,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"保税区-B区\",\"congestionIndex\":1.2,\"realSpeed\":\"39.3355\",\"yoy\":-0.1,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"康居西城\",\"congestionIndex\":1.3,\"realSpeed\":\"29.8503\",\"yoy\":0.0,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"白市驿\",\"congestionIndex\":1.1,\"realSpeed\":\"45.5646\",\"yoy\":-0.1,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"金凤\",\"congestionIndex\":1.2,\"realSpeed\":\"44.2227\",\"yoy\":0.0,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"保税区-A区\",\"congestionIndex\":1.4,\"realSpeed\":\"30.8192\",\"yoy\":0.0,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"高新天街\",\"congestionIndex\":1.4,\"realSpeed\":\"19.2326\",\"yoy\":0.1,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"熙街\",\"congestionIndex\":1.6,\"realSpeed\":\"23.2695\",\"yoy\":0.0,\"mom\":0.0,\"status\":\"缓行\"}]";
        // 使用 JSONPath.eval 获取多条件筛选的结果
        List<BigDecimal> congestionIndexList1 = (List<BigDecimal>) JSONPath.eval(jsonString, "$[?(@.name == '高新区管委会')].congestionIndex");
        double congestionIndex1 = ObjectUtils.isEmpty(congestionIndexList1) ? 1.0 : congestionIndexList1.get(0).doubleValue();
        System.out.println(congestionIndex1);

    }
```

#### 返回集合中多个元素

```java
    // 返回集合中多个元素
    @Test
    void test02() {
        Object objectList = JSONPath.eval(JSON.toJSONString(list), "[1,5]"); // 返回下标为1和5的元素
        List<UserInfoEntity> userList = JSONArray.parse(objectList.toString()).toJavaList(UserInfoEntity.class);
        System.out.println(userList);
    }
```

#### 按范围返回集合的子集

```java
    // 按范围返回集合的子集
    @Test
    void test03() {
        Object objectList = JSONPath.eval(JSON.toJSONString(list), "[1:5]"); // 返回下标从1到5的元素
        List<UserInfoEntity> userList = JSONArray.parse(objectList.toString()).toJavaList(UserInfoEntity.class);
        System.out.println(userList);
    }
```

#### 通过条件过滤，返回集合的子集

```java
    // 通过条件过滤，返回集合的子集
    @Test
    void test04() {
        Object objectList = JSONPath.eval(JSON.toJSONString(list), "$[?(@.id in (88,99))]"); // 返回下标从1到5的元素
        List<UserInfoEntity> userList = JSONArray.parse(objectList.toString()).toJavaList(UserInfoEntity.class);
        System.out.println(userList);
    }
    // 通过条件过滤，返回集合的子集
    @Test
    void test04_2() {
        Object objectList = JSONPath.eval(JSON.toJSONString(list), "$[?(@.age = 88)]"); // 返回列表对象的age=88的数据
        List<UserInfoEntity> userList = JSONArray.parse(objectList.toString()).toJavaList(UserInfoEntity.class);
        System.out.println(userList);
    }
```

#### 多条件筛选，返回集合的子集

```java
    // 多条件筛选，返回集合的子集
    @Test
    void test04_3() {
        Object objectList = JSONPath.eval(JSON.toJSONString(list), "$[?(@.age == 88 || @.age == 95)]"); // 返回列表对象的age=88或者=95的数据
        List<UserInfoEntity> userList = JSONArray.parse(objectList.toString()).toJavaList(UserInfoEntity.class);
        System.out.println(userList);
    }
    // 多条件筛选，返回集合的子集
    @Test
    void test04_4() {
        Object objectList = JSONPath.eval(JSON.toJSONString(list), "$[?(@.age > 50 && @.age < 95 && @.city='东莞')]");
        List<UserInfoEntity> userList = JSONArray.parse(objectList.toString()).toJavaList(UserInfoEntity.class);
        System.out.println(userList);
    }
```

#### 通过条件过滤，获取数组长度

```java
    // 通过条件过滤，获取数组长度
    @Test
    void test04_5() {
        int length = (int) JSONPath.eval(JSON.toJSONString(list), "$[?(@.age = 88)].length()"); // 返回列表对象的age=88的数据的长度
        System.out.println(length);
    }
```

